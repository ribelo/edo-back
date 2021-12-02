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
 (fn [_ [_ {:keys [edit?]}]]
   {:fx [[:commit [:app [[:dx/update [:app/id :app/ui] :show-query-modal? not]
                         (when-not edit?
                           [:dx/delete [:app/id :app/ui] :edit-query])]]]]}))

(rf/reg-event-fx
 ::edit-query
 (fn [_ [_ {:keys [query]}]]
   {:fx [[:commit   [:app [:dx/put [:app/id :app/ui] :edit-query query]]]
         [:dispatch [::toggle-query-modal {:edit? true}]]]}))

(rf/reg-event-fx
 ::add-new-query
 (fn [_ [_eid {:keys [query size]}]]
   (timbre/debug _eid query)
   {:fx [[:commit [:edo/settings [:dx/merge [:query/id query] {:query query :size size}]]]
         [:freeze-store :edo/settings]]}))

(rf/reg-event-fx
 ::remove-query
 (fn [_ [_eid query]]
   (timbre/debug _eid query)
   {:fx [[:commit [:edo/settings [:dx/delete [:query/id query]]]]
         [:freeze-store :edo/settings]]}))

(rf/reg-event-fx
 ::select-query
 (fn [_ [_eid query]]
   (timbre/debug _eid query)
   {:fx [[:commit [:app [:dx/put [:app/id :app/ui] :selected-query query]]]
         [:dispatch [::fetch-query {:query query}]]]}))

(rf/reg-event-fx
 ::toggle-favourite
 (fn [_ [_eid id query img price favourite?]]
   (timbre/debug _eid id query)
   {:fx [(if-not favourite?
           [:commit [:edo/settings [:dx/update [:query/id query] :favourites (fnil conj [])
                                    {:id id :img img :price price :favourite? true}]]]
           [:commit [:edo/settings [:dx/update [:query/id query] :favourites
                                    (fn [elems] (into [] (remove (fn [m] (= id (m :id)))) elems))]]])
         [:freeze-store :edo/settings]]}))

(rf/reg-event-fx
 ::cleanup-cache
 (fn [_ [_eid query]]
   (timbre/debug _eid query)
   {:fx [[:commit [:edo/settings [[:dx/delete [:query/id query] :cache]
                                  [:dx/delete [:query/id query] :favourites]]
                   :app          [[:dx/delete [:app/id query] :data]
                                  [:dx/put    [:app/id query] :end? false]
                                  [:dx/put    [:app/id query] :last-page 1]]]]]}))

(rf/reg-event-fx
 ::fetch-query
 [(rf/inject-cofx ::dx/with-dx! [:settings :edo/settings])]
 (fn [{:keys [db settings]} [_eid {:keys [query page prev counter]}]]
   (timbre/debug _eid query page)
   (let [page (or page 1)
         counter  (or counter (get-in settings [:query/id query :size]) 1)]
     (when (and name query)
       (let [end? (get-in db [:app/id query :end?])]
         (when-not end?
           {:fx [[:dispatch [:edo.ui.events/data-loading true]]
                 [:axios {:method     :post
                          :url        (enc/format "https://zenmarket.jp/en/yahoo.aspx/getProducts?q=%s" (enc/url-encode query))
                          :body       {:page page}
                          :on-success [::fetch-query-success {:query query
                                                              :page  page
                                                              :prev  prev
                                                              :counter counter}]
                          :on-failure [::fetch-query-failure {:query query
                                                              :page  page
                                                              :prev  prev
                                                              :counter counter}]}]]}))))))

(defn parse-price [s]
  (second (re-find #"data-jpy='(.*?)'" s)))

(rf/reg-event-fx
 ::fetch-query-success
 [(rf/inject-cofx ::dx/with-dx! [:settings :edo/settings])]
 (fn [{:keys [settings db]} [_eid {:keys [query page prev cnt resp counter]
                                  :or    {counter 0}}]]
   (timbre/debug _eid query page)
   (let [favourites   (into #{} (map :id) (get-in settings [:query/id query :favourites]))
         cached       (get-in settings [:query/id query :cache] #{})
         old-data-ids (into #{} (map :id) (get-in db [:app/id query :data]))
         to-remove    (into old-data-ids (set/difference cached favourites))
         page         (or (get-in db [:app/id query :last-page]) page 1)
         data         (not-empty (->clj (js/JSON.parse (.-d (.-data resp)))))
         parsed       (m/rewrite data
                        [{:AuctionID        !ids
                          :Title            !titles
                          :AuctionURL       !urls
                          :Thumbnail        !imgs
                          :PriceTextControl (m/app parse-price !price)
                          :EndTime          !end-time} ...]
                        [{:id !ids :title !titles :url !urls :img !imgs :price !price :end-time !end-time} ...])
         ids          (mapv :id parsed)
         new-data     (=>> parsed (remove (fn [{:keys [id]}] (to-remove id))))]
     {:fx [[:commit [:app [:dx/put [:app/id query] :last-page (inc page)]]]
           [:freeze-store :edo/settings]
           (enc/cond

             (seq new-data)
             [:commit [:edo/settings [:dx/update [:query/id query] :cache (fnil into #{}) ids]
                       :app          [[:dx/update [:app/id query] :data (fnil into []) new-data]
                                      [:dx/put [:app/id :ui/main] :show-spinner? false]]]]

             (or (nil? data) (= prev data))
             [:commit [:app [[:dx/put [:app/id :ui/main] :show-spinner? false]
                             [:dx/put [:app/id query] :end? true]]]])
           (when (or (pos? counter) (or (nil? data) (= prev data)))
             [:dispatch [::fetch-query {:query query :page (inc page) :prev data :counter (dec counter)}]])]})))

(rf/reg-event-fx
 ::fetch-query-failure
 (fn [_ [_eid {:keys [query page]}]]
   (timbre/error _eid query page)
   {:fx [[:commit [:app [:dx/put [:app/id :ui/main] :show-spinner? false]]]]}))

(rf/reg-event-fx
 ::open-browser
 (fn [_ [_eid url]]
   (enc/do-nil
    (.openExternal ^js shell url))))

(rf/reg-event-fx
 ::hover-tile
 (fn [_ [_eid mode img]]
   #_(timbre/debug _eid mode img)
   (case mode
     :enter
     {:fx [[:commit [:app [:dx/put [:app/id :ui/main] :tile-hovered img]]]]}
     :leave
     {:fx [[:commit-later [nil  [_eid img]]]
           [:commit [:app [:dx/delete [:app/id :ui/main] :tile-hovered]]]]})))
