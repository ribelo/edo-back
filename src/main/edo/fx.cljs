(ns edo.fx
  (:require
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [re-frame.core :as rf]
   [re-frame.router :as router]))

(let [timeouts (atom {})]
  (rf/reg-fx
   :dispatch-later
   (fn [data]
     (if (zero? (mod (count data) 3))
       (let [it (iter data)]
         (while (.hasNext it)
           (let [ms    (.next it)
                 id    (.next it)
                 evts  (.next it)]
             (let [t (enc/cond!
                       :do            (some-> (@timeouts id) enc/tf-cancel!)
                       :let           [evt (nth evts 0)]
                       (vector?  evt) (enc/after-timeout ms (doseq [evt evts] (router/dispatch evt)))
                       (keyword? evt) (enc/after-timeout ms (router/dispatch evts)))]
               (swap! timeouts assoc id t)))))
       (timbre/error "matterhorn: \":dispatch-debounce\" number of elements should be multiple of 3")))))
