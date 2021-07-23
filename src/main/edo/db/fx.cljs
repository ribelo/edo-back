(ns edo.db.fx
  (:require
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ribelo.doxa :as dx]))

(rf/reg-fx
 :commit
 (fn [data]
   (binding [dx/*atom-fn* r/atom]
     (if (even? (count data))
       (let [it (iter data)]
         (while (.hasNext it)
           (let [store (.next it)
                 txs   (.next it)]
             (dx/with-dx! [db store]
               (enc/cond
                 :let          [tx (nth txs 0)]
                 (vector?  tx) (dx/commit! db txs)
                 (keyword? tx) (dx/commit! db [txs]))))))
       (timbre/error "matterhorn: \":commit\" number of elements should be even")))))

(let [timeouts (atom {})]
  (rf/reg-fx
   :commit-later
   (fn [data]
     (binding [dx/*atom-fn* r/atom]
       (if (or (zero? (mod (count data) 4))
               (= 2 (count data)))
         (let [it (iter data)]
           (while (.hasNext it)
             (let [ms    (.next it)
                   id    (.next it)
                   store (.next it)
                   txs   (.next it)]
               (some-> (@timeouts id) enc/tf-cancel!)
               (when store
                 (dx/with-dx! [db store]
                   (let [t (enc/cond!
                             :let          [tx (nth txs 0)]
                             (nil?     ms) nil
                             (vector?  tx) (enc/after-timeout ms (dx/commit! db txs))
                             (keyword? tx) (enc/after-timeout ms (dx/commit! db [txs])))]
                     (swap! timeouts assoc id t)))))))
         (timbre/error "matterhorn: \":commit\" number of elements should be a multiple of 4"))))))
