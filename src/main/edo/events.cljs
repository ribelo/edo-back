(ns edo.events
  (:require
   [clojure.set :as set]
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [meander.epsilon :as m]
   [cljs-bean.core :refer [->clj]]
   [ribelo.doxa :as dx]
   [ribelo.danzig :refer [=>>]]
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]
   [edo.store :as st]
   [edo.file-storage.events :as fs]
   [edo.ui.events :as ui.events]
   [edo.axios.events :as axios.events]
   [edo.electron.core :refer [shell]]))

(declare fetch-query fetch-query-success fetch-query-failure)

(mx/defevent toggle-query-modal [{:keys [edit?]}]
  mx/WatchEvent
  (mx/watch [_ _ _]
    (mi/ap
      (mi/amb>
        (st/commit :edo/app [:dx/update [:app/id :app/ui] :show-query-modal? not])
        (when-not edit?
          (st/commit :edo/app [:dx/delete [:app/id :app/ui] :edit-query]))
        ))))

(mx/defevent edit-query [{:keys [query]}]
  mx/WatchEvent
  (mx/watch [_ _ _]
    (mi/ap
      (mi/amb>
        (st/commit :edo/app [:dx/put [:app/id :app/ui] :edit-query query])
        (toggle-query-modal {:edit? true})))))

(mx/defevent add-new-query [{:keys [query size]}]
  mx/WatchEvent
  (mx/watch [e _ _]
    (timbre/debug (mx/-id e) query)
    (mi/ap
      (mi/amb>
        (st/commit :edo/settings [:dx/merge [:query/id query] {:query query :size size}])
        (fs/freeze-node :edo/settings)))))

(mx/defevent remove-query [{:keys [query]}]
  mx/WatchEvent
  (mx/watch [e _ _]
    (timbre/debug (mx/-id e) query)
    (mi/ap
      (mi/amb>
        (st/commit :edo/settings [:dx/delete [:query/id query]])
        (fs/freeze-node :edo/settings)))))

(mx/defevent select-query [{:keys [query]}]
  mx/WatchEvent
  (mx/watch [e _ _]
    (timbre/debug (mx/-id e) query)
    (mi/ap
      (mi/amb>
        (st/commit :edo/app [:dx/put [:app/id :app/ui] :selected-query query])
        (fetch-query {:query query})))))

(mx/defevent toggle-favourite [{:keys [id query img price favourite?]}]
  mx/WatchEvent
  (mx/watch [e _ _]
    (timbre/debug (mx/-id e) id query)
    (mi/ap
      (mi/amb>
        (if-not favourite?
          (st/commit :edo/settings
            [:dx/update [:query/id query] :favourites (fnil conj [])
             {:id id :img img :price price :favourite? true}])
          (st/commit :edo/settings
            [:dx/update [:query/id query] :favourites
             (fn [elems] (into [] (remove (fn [m] (= id (m :id)))) elems))]))
        ;; (fs/freeze-node :edo/settings)
        ))))

(mx/defevent cleanup-cache [{:keys [query]}]
  mx/WatchEvent
  (watch [e _ _]
    (timbre/debug (mx/-id e) query)
    (mi/ap
      (mi/amb>
        (st/commit :edo/settings [[:dx/delete [:query/id query] :cache]
                                  [:dx/delete [:query/id query] :favourites]])
        (st/commit :edo/app [[:dx/delete [:app/id query] :data]
                        [:dx/put    [:app/id query] :end? false]
                        [:dx/put    [:app/id query] :last-page 1]])))))

(defn parse-price [s]
  (second (re-find #"data-jpy='(.*?)'" s)))

(comment
  (get-in @(st/dag :edo/app) [:app/id "armor" :last-page]))

(mx/defevent fetch-query [{:keys [query page prev counter]}]
  mx/WatchEvent
  (mx/watch [e {:edo/keys [settings app]} _]
    (timbre/debug (mx/-id e) query page)
    (mi/ap
      (let [page    (or (get-in (mi/? app) [:app/id query :last-page]) page 1)
            counter (or counter (get-in (mi/? settings) [:query/id query :size]) 1)]
        (when (and name query)
          (let [end? (get-in (mi/? app) [:app/id query :end?])]
            (when-not end?
              (mi/amb>
                (ui.events/set-data-loading true)
                (axios.events/request {:method     :post
                                       :url        (enc/format "https://zenmarket.jp/en/yahoo.aspx/getProducts?q=%s" (enc/url-encode query))
                                       :body       {:page page}
                                       :on-success (fn [resp]
                                                     (fetch-query-success {:query   query
                                                                           :page    page
                                                                           :prev    prev
                                                                           :counter counter
                                                                           :resp    resp}))
                                       :on-failure (fn [resp]
                                                     (fetch-query-failure {:query   query
                                                                           :page    page
                                                                           :prev    prev
                                                                           :counter counter
                                                                           :resp    resp}))})))))))))

(mx/defevent fetch-query-success [{:keys [query page prev cnt resp counter] :or {counter 0}}]
  mx/WatchEvent
  (mx/watch [e {:edo/keys [settings app]} _]
    (timbre/debug (mx/-id e) query page)
    (mi/ap
      (let [favourites   (into #{} (map :id) (get-in (mi/? settings) [:query/id query :favourites]))
            cached       (get-in (mi/? settings) [:query/id query :cache] #{})
            old-data-ids (into #{} (map :id) (get-in (mi/? app) [:app/id query :data]))
            to-remove    (into old-data-ids (set/difference cached favourites))
            page         (or (get-in (mi/? app) [:app/id query :last-page]) page 1)
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
        (mi/amb>
          (st/commit :edo/app [:dx/put [:app/id query] :last-page (inc page)])
          (fs/freeze-node :edo/settings)
          (cond
            (seq new-data)
            (mi/amb>
              (st/commit :edo/settings [:dx/update [:query/id query] :cache (fnil into #{}) ids])
              (st/commit :edo/app [[:dx/update [:app/id query] :data (fnil into []) new-data]
                                   [:dx/put [:app/id :ui/main] :show-spinner? false]]))

            (or (nil? data) (= prev data) (zero? counter))
            (mi/amb>
              (st/commit :edo/app [[:dx/put [:app/id :ui/main] :show-spinner? false]
                                   (when (nil? data) [:dx/put [:app/id query] :end? true])])))
          (when (or (pos? counter) (or (nil? data) (= prev data)))
            (fetch-query {:query query :page (inc page) :prev data :counter (dec counter)})))))))

(mx/defevent fetch-query-failure [{:keys [query page]}]
  mx/WatchEvent
  (mx/watch [e _ _]
    (timbre/error (mx/-id e) query page)
    (st/commit :edo/app [:dx/put [:app/id :ui/main] :show-spinner? false])))

(mx/defevent open-browser [{:keys [url]}]
  mx/EffectEvent
  (mx/effect [_ _ _]
    (mi/ap (.openExternal ^js shell url))))

(comment
  ((mi/sp @(st/dag :edo.subs/hovered-tile)) tap> prn)
  )

(mx/defevent hover-tile [{:keys [mode img]}]
  mx/WatchEvent
  (mx/watch [e _ _]
    (timbre/debug (mx/-id e) mode img)
    (mi/ap
      (case mode
        :enter
        (st/commit :edo/app [:dx/put [:app/id :ui/main] :tile-hovered img])
        :leave
        (st/commit :edo/app [:dx/delete [:app/id :ui/main] :tile-hovered])))))
