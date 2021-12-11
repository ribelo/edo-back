(ns edo.ui.events
  (:require
   [taoensso.encore :as enc]
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]
   [edo.store :as st]))

(defn add-notification [id notification]
  (mx/reify :add-notification
    mx/WatchEvent
    (mx/watch [_ _ _]
      (let [pred-fn #(not (enc/rsome (partial = notification) %))]
        (mi/ap
          (mi/amb>
            (st/commit :edo/app
              [[:dx/match [:app/id :ui/main] :notifications pred-fn]
               [:dx/conj [:app/id :ui/main] :notifications notification]])
            (st/commit-later (enc/ms :secs 3) [id notification] :edo/app
              [[:dx/match [:app/id :ui/main] :notifications pred-fn]
               [:dx/conj [:app/id :ui/main] :notifications notification]])))))))

(defn set-data-loading
  ([v]
   (mx/reify ::set-data-loading
     mx/WatchEvent
     (mx/watch [id _ _]
       (cond
         (boolean? v)
         (mi/ap
           (st/commit :edo/app
             [:dx/put [:app/id :ui/main] :show-spinner? v]))

         (number? v)
         (mi/ap
           (mi/amb>
             (st/commit :edo/app
               [:dx/put [:app/id :ui/main] :show-spinner? true])
             (st/commit-later v id :edo/app
               [:dx/put [:app/id :ui/main] :show-spinner? false])))

         :else
         mi/none)))))
