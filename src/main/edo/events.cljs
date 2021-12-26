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

(def htmlparser  (js/require "node-html-parser"))

(declare fetch-query fetch-query-success fetch-query-failure)

(mx/defwatch ::toggle-query-modal
  [_ _ _ {:keys [edit?]}]
  (mi/ap
   (mi/amb>
    (mx/event :commit :edo/app [:dx/update [:app/id :app/ui] :show-query-modal? not])
    (when-not edit?
      (mx/event :commit :edo/app [:dx/delete [:app/id :app/ui] :edit-query])))))

(mx/defwatch ::edit-query
  [_ _ _ {:keys [query]}]
  (mi/ap
   (mi/amb>
    (mx/event :commit :edo/app [:dx/put [:app/id :app/ui] :edit-query query])
    (mx/event ::toggle-query-modal {:edit? true}))))

(mx/defwatch ::parse-auction-page
  [_ {:edo/keys [app]} _ {:keys [item-id]}]
  (mi/ap
   (when-not (get-in (mi/? app) [:app/id item-id :page])
     (let [resp (mi/? (axios.events/<http {:url    (str "https://zenmarket.jp/auction.aspx?itemCode=" item-id)
                                           :method :get}))
           data (.parse htmlparser (.-data resp))]
       (mx/event :commit :edo/app [:dx/put [:app/id item-id] :page data])))))

(mx/defwatch ::add-new-query
  [e _ _ {:keys [query a]}]
  (timbre/debug (mx/id e) query :a a)
  (mi/ap
   (mi/amb>
    (mx/event :commit :edo/settings [:dx/merge [:query/id query] {:query query}])
    (mx/event ::fs/freeze-node :edo/settings))))

(comment
  (mx/watch (mx/event ::add-new-query {:query :armor :a 1}) nil nil)
  )

(mx/defwatch ::remove-query
  [e _ _ {:keys [query]}]
  (timbre/debug (mx/id e) query)
  (mi/ap
   (mi/amb>
    (mx/event :commit :edo/settings [:dx/delete [:query/id query]])
    (mx/event ::fs/freeze-node :edo/settings))))

(mx/defwatch ::select-query
  [e _ _ {:keys [query]}]
  (timbre/debug (mx/id e) query)
  (mi/ap
   (mi/amb>
    (mx/event :commit :edo/app [:dx/put [:app/id :app/ui] :selected-query query])
    (mx/event ::fetch-query {:query query}))))

(mx/defwatch ::toggle-favourite
  [e _ _ {:keys [id query img price favourite?]}]
  (timbre/debug (mx/id e) id query)
  (mi/ap
   (mi/amb>
    (if-not favourite?
      (mx/event :commit :edo/settings
                [:dx/update [:query/id query] :favourites (fnil conj [])
                 {:id id :img img :price price :favourite? true}])
      (mx/event :commit :edo/settings
                [:dx/update [:query/id query] :favourites
                 (fn [elems] (into [] (remove (fn [m] (= id (m :id)))) elems))])))))

(mx/defwatch ::cleanup-cache [e _ _ {:keys [query]}]
  (timbre/debug (mx/id e) query)
  (mi/ap
   (mi/amb>
    (mx/event :commit :edo/settings [[:dx/delete [:query/id query] :cache]
                                     [:dx/delete [:query/id query] :favourites]])
    (mx/event :commit :edo/app [[:dx/delete [:app/id query] :data]
                                [:dx/put    [:app/id query] :end? false]
                                [:dx/put    [:app/id query] :last-page 1]]))))

(defn parse-price [s]
  (second (re-find #"data-jpy='(.*?)'" s)))

(comment
  (get-in @(st/dag :edo/app) [:app/id "armor" :last-page]))

(mx/defwatch ::fetch-query
  [e {:edo/keys [settings app]} _ {:keys [query page prev counter]}]
  mx/WatchEvent
  (timbre/debug (mx/id e) query page)
  (mi/ap
   (let [page    (or (get-in (mi/? app) [:app/id query :last-page]) page 1)
         counter (or counter 10)]
     (when (and name query)
       (let [end? (get-in (mi/? app) [:app/id query :end?])]
         (when-not end?
           (mi/amb>
            (mx/event ::ui.events/set-data-loading true)
            (mx/event ::axios.events/request
                      {:method     :post
                       :url        (enc/format "https://zenmarket.jp/en/yahoo.aspx/getProducts?q=%s" (enc/url-encode query))
                       :body       {:page page}
                       :on-success (fn [resp]
                                     (mx/event ::fetch-query-success
                                               {:query   query
                                                :page    page
                                                :prev    prev
                                                :counter counter
                                                :resp    resp}))
                       :on-failure (fn [resp]
                                     (mx/event ::fetch-query-failure
                                               {:query   query
                                                :page    page
                                                :prev    prev
                                                :counter counter
                                                :resp    resp}))}))))))))

(mx/defwatch ::fetch-query-success
  [e {:edo/keys [settings app]} _ {:keys [query page prev cnt resp counter] :or {counter 0}}]
  (timbre/debug (mx/id e) query page)
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
      (mx/event :commit :edo/app [:dx/put [:app/id query] :last-page (inc page)])
      (mx/event ::fs/freeze-node :edo/settings)
      (cond
        (seq new-data)
        (mi/amb>
         (mx/event :commit :edo/settings [:dx/update [:query/id query] :cache (fnil into #{}) ids])
         (mx/event :commit :edo/app [[:dx/update [:app/id query] :data (fnil into []) new-data]
                                     [:dx/put [:app/id :ui/main] :show-spinner? false]]))

        (or (nil? data) (= prev data) (zero? counter))
        (mi/amb>
         (mx/event :commit :edo/app [[:dx/put [:app/id :ui/main] :show-spinner? false]
                                     (when (nil? data) [:dx/put [:app/id query] :end? true])])))
      (when (or (pos? counter) (or (nil? data) (= prev data)))
        (mx/event ::fetch-query {:query query :page (inc page) :prev data :counter (dec counter)}))))))

(mx/defwatch ::fetch-query-failure
  [e _ _ {:keys [query page]}]
  (timbre/error (mx/id e) query page)
  (mx/event :commit :edo/app [:dx/put [:app/id :ui/main] :show-spinner? false]))

(mx/defeffect ::open-browser
  [_ _ _ {:keys [url]}]
  (mi/ap (.openExternal ^js shell url)))

(mx/defwatch ::hover-tile
  [e _ _ {:keys [mode img]}]
  (timbre/debug (mx/id e) mode img)
  (mi/ap
   (case mode
     :enter
     (mx/event :commit :edo/app [:dx/put [:app/id :ui/main] :tile-hovered img])
     :leave
     (mx/event :commit :edo/app [:dx/delete [:app/id :ui/main] :tile-hovered]))))
