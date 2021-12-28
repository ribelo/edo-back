(ns edo.axios.events
  (:require
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [meander.epsilon :as m]
   [missionary.core :as mi]
   [cljs-bean.core :refer [->js]]
   [ribelo.metaxy :as mx]
   [edo.store :as st]))

(def axios (js/require "axios"))

(defn <http
  [{:keys [method url body settings]}]
  (let [v (mi/dfv)]
    (case method
      :get  (-> (.get  axios url (->js settings)) (.then #(v (fn [] %)) #(v (fn [] (throw %)))))
      :post (-> (.post axios url (->js body) (->js settings)) (.then #(v (fn [] %)) #(v (fn [] (throw %))))))
    (mi/absolve v)))

(def a (enc/limiter {:main [10 10000]}))

(defn limiter [n ms]
  (let [l (enc/limiter {:main [n ms]})]
    (fn [>f]
      (mi/ap
       (let [x (mi/?> (mi/relieve {} >f))]
         (if-let [[_ wait] (l)]
           (do (mi/? (mi/sleep wait)) (mi/amb>))
           x))))))

(defmethod mx/event ::request
  [_ m]
  (mx/reify ::request
    mx/CustomEvent
    (mx/process [_ _ _]
      (mi/ap
       (println ::request)
       (let [resp (try (mi/? (<http m)) (catch :default e ((m :on-failure) nil)))
             code (some-> resp .-status)]
         (if (== 200 code)
           ((m :on-success) resp)
           ((m :on-failure) resp)))))))

(mx/add-watch!
 (mx/reify ::watch-request
   mx/WatchEvent
   (mx/watch [_ dag >stream]
     (mi/ap
      (let [e (mi/?< (->> >stream
                               (mi/eduction (filter (fn [e] (and (mx/custom-event? e) (keyword-identical? ::request (mx/id e))))))
                               ((limiter 30 (enc/ms :secs 60)))))]
        (mi/?= (mx/process e dag >stream)))))))
