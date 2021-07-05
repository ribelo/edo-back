(ns edo.subs
  (:require
   [taoensso.encore :as enc]
   [reagent.ratom :as ra]
   [re-frame.core :as rf]
   [ribelo.doxa :as dx]
   [edo.ipc :as ipc]))

(rf/reg-sub-raw
 :pull
 (fn [_ [_ store q eid]]
   (ra/reaction
    (dx/with-dx! [db_ store]
      (dx/pull @db_ q eid)))))

(rf/reg-sub-raw
 :pull-one
 (fn [_ [_ store q eid]]
   (ra/reaction
    (dx/with-dx! [db_ store]
      (dx/pull-one @db_ q eid)))))

(rf/reg-sub-raw
 :edo/version
 (fn [_ _]
   (ipc/send! [:app/version])
   (dx/with-dx! [dx_ :app]
     (ra/reaction
      (get-in @dx_ [:app/id :app/info :app/version])))))

(rf/reg-sub-raw
 ::show-query-modal?
 (fn [_ _]
   (dx/with-dx! [dx_ :app]
     (ra/reaction
      (get-in @dx_ [:app/id :app/ui :show-query-modal?])))))

(rf/reg-sub-raw
 ::queries
 (fn [_ _]
   (dx/with-dx! [settings_ :edo/settings]
     (ra/reaction (vals (@settings_ :query/id))))))

(rf/reg-sub-raw
 ::selected-query
 (fn [_ _]
   (dx/with-dx! [app_ :app]
     (ra/reaction (get-in @app_ [:app/id :app/ui :selected-query])))))

(rf/reg-sub-raw
 ::query-data
 (fn [_ [_ name]]
   (dx/with-dx! [app_ :app]
     (ra/reaction (get-in @app_ [:app/id [:query/name name] :data])))))

(rf/reg-sub-raw
 ::selected-query-data
 (fn [_ _]
   (ra/reaction
    (enc/when-let [selected-query @(rf/subscribe [::selected-query])
                   data           @(rf/subscribe [::query-data selected-query])]
      data))))

(comment
  (rf/clear-subscription-cache!)
  (count @(rf/subscribe [::selected-query-data]))
  (dx/with-dx! [settings_ :edo/settings]
    settings_))
