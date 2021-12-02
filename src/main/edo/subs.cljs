(ns edo.subs
  (:require
   [taoensso.encore :as enc]
   [reagent.ratom :as ra]
   [re-frame.core :as rf]
   [ribelo.doxa :as dx]
   [ribelo.danzig :as dz :refer [=>>]]
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
 ::edit-query
 (fn [_ _]
   (dx/with-dx! [dx_ :app]
     (ra/reaction
      (get-in @dx_ [:app/id :app/ui :edit-query])))))

(rf/reg-sub-raw
 ::query-text
 (fn [_ [_ {:keys [query]}]]
   (dx/with-dx! [dx_ :edo/settings]
     (ra/reaction
      (get-in @dx_ [:query/id query :query])))))

(rf/reg-sub-raw
 ::query-size
 (fn [_ [_ {:keys [query]}]]
   (dx/with-dx! [dx_ :edo/settings]
     (ra/reaction
      (get-in @dx_ [:query/id query :size] 1)))))

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
     (ra/reaction (sort-by :query (vals (@settings_ :query/id)))))))

(rf/reg-sub-raw
 ::selected-query
 (fn [_ _]
   (dx/with-dx! [app_ :app]
     (ra/reaction (get-in @app_ [:app/id :app/ui :selected-query])))))

(rf/reg-sub-raw
 ::query-data
 (fn [_ [_ query]]
   (dx/with-dx! [settings_ :edo/settings
                 app_      :app]
     (ra/reaction
      (let [favourites (get-in @settings_ [:query/id query :favourites])
            data (get-in @app_ [:app/id query :data])]
        (=>> (enc/into-all [] favourites data)
             (enc/xdistinct :id)))))))

(comment
  (tap> @(rf/subscribe [::query-data "zet"]))
  )

(rf/reg-sub-raw
 ::selected-query-data
 (fn [_ _]
   (ra/reaction
    (enc/when-let [selected-query @(rf/subscribe [::selected-query])
                   data           @(rf/subscribe [::query-data selected-query])]
      data))))

(rf/reg-sub-raw
 ::hovered-tile
 (fn [_ _]
   (dx/with-dx! [app_ :app]
     (ra/reaction
      (get-in @app_ [:app/id :ui/main :tile-hovered])))))

(comment
  (rf/clear-subscription-cache!)
  (count @(rf/subscribe [::query-data "armor"]))
  (dx/with-dx! [settings_ :edo/settings]
    settings_))
