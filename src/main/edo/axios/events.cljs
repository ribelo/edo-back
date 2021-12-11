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

(defn limiter [n ms >f]
  (mi/ap
    (let [[i x] (mi/?> (mi/eduction (map-indexed vector) >f))]
      (if (zero? (rem i n)) (mi/? (mi/sleep ms x)) x))))

(mx/defevent request [m]
  mx/WatchEvent
  (mx/watch [_ _ _]
    (mi/ap
      (let [resp (try (mi/? (<http m)) (catch :default e ((m :on-failure) nil)))
            code (some-> resp .-status)]
        (if (== 200 code)
          ((m :on-success) resp)
          ((m :on-failure) resp))))))

(mx/add-watch! st/dag
  (mx/reify :watch-axios
    mx/WatchEvent
    (mx/watch [_ _ >f]
      (mi/ap
        (let [[_ m] (mi/?> (mi/eduction (filter (fn [xs] (and (vector? xs) (keyword-identical? :axios (nth xs 0)) (= 2 (count xs)))))))]
          (request m))))))
