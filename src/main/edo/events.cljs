(ns edo.events
  (:require
   [clojure.set :as set]
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [re-frame.core :as rf]
   [meander.epsilon :as m]
   [cljs-bean.core :refer [->clj]]
   [ribelo.doxa :as dx]
   [ribelo.danzig :refer [=>>]]
   [edo.electron.core :refer [shell]]))

(rf/reg-event-fx
 ::toggle-query-modal
 (fn [_ _]
   {:fx [[:commit [:app [:dx/update [:app/id :app/ui] :show-query-modal? not]]]]}))

(rf/reg-event-fx
 ::add-new-query
 (fn [_ [_eid name query]]
   (timbre/debug _eid name query)
   {:fx [[:commit [:edo/settings [:dx/put [:query/id [:query/name name]] {:name  name
                                                                          :query query}]]]
         [:freeze-store :edo/settings]]}))

(rf/reg-event-fx
 ::remove-query
 (fn [_ [_eid name]]
   (timbre/debug _eid name)
   {:fx [[:commit [:edo/settings [:dx/delete [:query/id [:query/name name]]]]]
         [:freeze-store :edo/settings]]}))

(rf/reg-event-fx
 ::select-query
 (fn [_ [_eid name query]]
   (timbre/debug _eid name)
   {:fx [[:commit [:app [:dx/put [:app/id :app/ui] :selected-query {:name name :query query}]]]
         [:dispatch [::fetch-query name query]]]}))

(rf/reg-event-fx
 ::add-to-favourites
 (fn [_ [_eid id]]
   (println :id id)
   {:fx [[:commit [:edo/settings [:dx/update [:db/id :cache] :favourites (fnil conj #{}) id]]]]}))

(rf/reg-event-fx
 ::fetch-query
 (fn [_ [_eid name query page]]
   (let [page  (or page 1)]
     (when (and name query)
       {:fx [[:dispatch [:edo.ui.events/data-loading true]]
             [:axios {:method     :post
                      :url        (enc/format "https://zenmarket.jp/en/yahoo.aspx/getProducts?q=%s" (enc/url-encode query))
                      :body       {:page page}
                      :on-success [::fetch-query-success name query page]
                      :on-failure [::fetch-query-failure name query page]}]]}))))

(rf/reg-event-fx
 ::fetch-query-success
 [(rf/inject-cofx ::dx/with-dx! [:settings :edo/settings])]
 (fn [{:keys [settings db]} [_eid name query page resp]]
   (enc/when-let [favourites   (get-in settings [:db/id :cache :favourites] #{})
                  cached       (get-in settings [:db/id :cache :ids] #{})
                  old-data-ids (mapv :id (get-in db [:app/id [:query/name name] :data]))
                  to-remove    (into (set old-data-ids) (set/difference cached favourites))
                  page         (or (get-in db [:app/id [:query/name name] :last-page]) page 1)
                  data         (not-empty (->clj (js/JSON.parse (.-d (.-data resp)))))
                  parsed       (m/rewrite data
                                 [{:AuctionID  !ids
                                   :Title      !titles
                                   :AuctionURL !urls
                                   :Thumbnail  !imgs} ...]
                                 [{:id !ids :title !titles :url !urls :img !imgs} ...])
                  ids          (mapv :id parsed)
                  new-data     (=>> parsed (remove (fn [{:keys [id]}] (to-remove id))))]
     {:fx [[:commit [:app [:dx/put [:app/id [:query/name name]] :last-page (inc page)]]]
           [:freeze-store :edo/settings]
           (enc/cond
             (seq new-data)
             [:commit [:edo/settings [:dx/update [:db/id :cache] :ids (fnil into #{}) ids]
                       :app          [[:dx/update [:app/id [:query/name name]] :data (fnil into [])  new-data]
                                      [:dx/put [:app/id :ui/main] :show-spinner? false]]]]
             (some? data)
             [:dispatch [::fetch-query name query (inc page)]])
           (when (< page 5)
             [:dispatch [::fetch-query name query (inc page)]])]})))

(rf/reg-event-fx
 ::fetch-query-failure
 (fn [_ [_eid name query page]]
   (timbre/error _eid name query page)
   {:fx [[:commit [:dx/put [:app/id :ui/main] :show-spinner? false]]]}))

(rf/reg-event-fx
 ::open-browser
 (fn [_ [_eid url]]
   (enc/do-nil
    (.openExternal ^js shell url))))

(comment
  (rf/dispatch [::fetch-query "test" "test"])
  (dx/commit {} [:dx/update [:app/id :a] :data (fnil into []) [1 2 3]])

  (dx/with-dx! [settings_ :edo/settings
                app_ :app]
    (tap> @settings_)
    (tap> @app_))

  )
