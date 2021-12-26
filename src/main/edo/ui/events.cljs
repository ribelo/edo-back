(ns edo.ui.events
  (:require
   [taoensso.encore :as enc]
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]
   [edo.store :as st]))

(mx/defwatch ::add-notification
  [_ _ _ id notification]
  (let [pred-fn #(not (enc/rsome (partial = notification) %))]
    (mi/ap
     (mi/amb>
      (mx/event :commit :edo/app
                [[:dx/match [:app/id :ui/main] :notifications pred-fn]
                 [:dx/conj [:app/id :ui/main] :notifications notification]])
      (mx/event :commit-later (enc/ms :secs 3) [id notification] :edo/app
                [[:dx/match [:app/id :ui/main] :notifications pred-fn]
                 [:dx/conj [:app/id :ui/main] :notifications notification]])))))

(mx/defwatch ::set-data-loading
  [_ _ _ v]
  (cond
    (boolean? v)
    (mi/ap
     (mx/event :commit :edo/app
               [:dx/put [:app/id :ui/main] :show-spinner? v]))

    (number? v)
    (mi/ap
     (mi/amb>
      (mx/event :commit :edo/app
                [:dx/put [:app/id :ui/main] :show-spinner? true])
      (mx/event :commit-later v id :edo/app
                [:dx/put [:app/id :ui/main] :show-spinner? false])))

    :else
    mi/none))
