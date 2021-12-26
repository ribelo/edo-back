(ns edo.subs
  (:require
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [ribelo.doxa :as dx]
   [ribelo.danzig :as dz :refer [=>>]]
   [missionary.core :as mi]
   [ribelo.metaxy :as mx]
   [edo.store :as st]))

(mx/defnode ::edit-query
  [_ {:edo/keys [app]}]
  (some-> app :app/id :app/ui :edit-query))

(mx/defnode ::query-text
  [_ {:edo/keys [settings]} {:keys [query]}]
  (get-in settings [:query/id query :query] 1))

(mx/defnode ::query-size
  [_ {:edo/keys [settings]} {:keys [query]}]
  (get-in settings [:query/id query :size] 1))

(mx/defnode ::show-query-modal?
  [_ {:edo/keys [app]}]
  (some-> app :app/id :app/ui :show-query-modal?))

(mx/defnode ::queries
  [_ {:edo/keys [settings]}]
  (sort-by :query (vals (settings :query/id))))

(mx/defnode ::selected-query
  [_ {:edo/keys [app]}]
  (some-> app :app/id :app/ui :selected-query))

(mx/defnode ::query-data
  [_ {:edo/keys [settings app]} {:keys [query]}]
  (let [favourites (get-in settings [:query/id query :favourites])
        data (get-in app [:app/id query :data])]
    (=>> (enc/into-all [] favourites data)
         (enc/xdistinct :id))))

;; (mx/listen! st/dag
;;   ::query-data
;;   {:query "armor"}
;;   (fn [_id r]
;;     (println :listen _id :start)
;;     (println :listen _id :r r)))

(mx/defnode ::selected-query-data
  [_ {::keys [selected-query query-data]}]
  (query-data {:query selected-query}))

(mx/defnode ::hovered-tile
  [id {:edo/keys [app]}]
  (some-> app :app/id :ui/main :tile-hovered))

(mx/defnode ::parsed-page
  [_ {:edo/keys [app]} item-id]
  (get-in app [:app/id item-id :page]))

(mx/defnode ::auction-seller
  [_ {::keys [parsed-page]} {:keys [item-id]}]
  (some-> (parsed-page item-id) (.querySelector "#seller") .-textContent))
