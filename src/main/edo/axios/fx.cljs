(ns edo.axios.fx
  (:require
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [meander.epsilon :as m]
   [missionary.core :as mi]
   [re-frame.core :as rf]
   [cljs-bean.core :refer [->js]]))

(def axios (js/require "axios"))

(defn <http
  [{:keys [method url body settings]}]
  (fn [s f]
    (case method
      :get  (-> (.get  axios url (->js settings)) (.then s f))
      :post (-> (.post axios url (->js body) (->js settings)) (.then s f)))
    (fn [])))

(defn flatten-dispatch [x]
  (m/rewrite x
    (m/with [%a [(m/pred keyword?) . !xs ... :as !vs]
             %b (m/or [(m/or %a %b) ...] %a nil)]
            %b)
    [!vs ...]
    _
    ~(timbre/error :flatte-dispatch x)))

(rf/reg-fx
 :axios
 (let [limiter (enc/limiter {:5s [5 5000]})]
   (fn [{:keys [url on-success on-failure] :as m}]
     (let [t (mi/sp
              (loop []
                (if-let [ms (second (limiter))]
                  (do
                    (mi/? (mi/sleep (inc ms)))
                    (recur))
                  (let [resp (mi/? (<http m))
                        code (some-> resp .-status)]
                    (if (== 200 code)
                      resp
                      (throw (ex-info "request failed" {:code code :url url :resp resp})))))))]
       (t
        (fn [resp]
          (enc/cond!
            (fn? on-success)
            (on-success resp)

            (vector? on-success)
            (doseq [[k m] (flatten-dispatch on-success)]
              (rf/dispatch [k (assoc m :resp resp)]))))
        (fn [err]
          (let [resp (or (.-data err) (.-response err))
                code (or (:code resp) (.-status resp))
                url  (or (:url resp)  (-> ^js resp .-config .-url))]
            (timbre/warnf "request: %s code: %s" url code)
            (enc/cond!
              (fn? on-failure)
              (on-failure resp)

              (vector? on-failure)
              (doseq [[k m] (flatten-dispatch on-failure)]
                (rf/dispatch [k (assoc m :resp resp)]))))))))))
