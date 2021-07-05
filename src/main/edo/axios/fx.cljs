(ns edo.axios.fx
  (:require
   [cljs.core.async :as a :refer [<! go go-loop]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [re-frame.core :as rf]
   [cljs-bean.core :refer [->js]]))

(def axios (js/require "axios"))

(defn <http
  [{:keys [method url body settings]}]
  (go (case method
        :get  (<p! (.get  axios url (->js settings)))
        :post (<p! (.post axios url (->js body) (->js settings))))))

(comment
  (go
    (let [resp (<p! (.post axios (enc/format "https://zenmarket.jp/en/yahoo.aspx/getProducts?q=%s"
                                             (enc/url-encode "test"))
                           (->js {:page 9999
                                  :limit 100})))]
      (tap> [:resp resp])))
  )

(rf/reg-fx
 :axios
 (let [limiter (enc/limiter {:3s [5 3000]})]
   (fn [{:keys [method url body settings on-success on-failure] :as m}]
     (go-loop []
       (enc/cond
         (limiter)
         (do (<! (a/timeout 1000)) (recur))

         :let [v    (<! (<http m))
               code (.-status v)]

         (== 200 code)
         (enc/cond
           (fn? on-success)                 (on-success v)
           (vector? on-success)             (rf/dispatch (conj on-success v))
           (enc/revery? vector? on-success) (doseq [v on-success]
                                              (rf/dispatch (conj on-success v)))
           v)

         :else
         (do (timbre/warnf "request: %s code: %s" url code)
             (enc/cond
               (fn? on-failure)                 (on-failure v)
               (vector? on-failure)             (rf/dispatch (conj on-failure v))
               (enc/revery? vector? on-failure) (doseq [v on-failure]
                                                  (rf/dispatch (conj on-failure v)))
               v)))))))
