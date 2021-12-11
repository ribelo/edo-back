(ns edo.subs
  (:require
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [ribelo.doxa :as dx]
   [ribelo.danzig :as dz :refer [=>>]]
   [missionary.core :as mi]
   [ribelo.metaxy :as mx]
   [edo.store :as st]))

(mx/add-node! st/dag ::edit-query
  [:edo/app]
  (fn [_ {:edo/keys [app]}]
    (some-> app :app/id :app/ui :edit-query)))

(mx/add-node! st/dag ::query-text
  [:edo/settings]
  (fn [_ {:edo/keys [settings]}]
    (fn [{:keys [query]}]
      (get-in settings [:query/id query :query] 1))))

(mx/add-node! st/dag ::query-size
  [:edo/settings]
  (fn [_ {:edo/keys [settings]}]
    (fn [{:keys [query]}]
      (get-in settings [:query/id query :size] 1))))

(mx/add-node! st/dag ::show-query-modal?
  [:edo/app]
  (fn [_ {:edo/keys [app]}]
    (some-> app :app/id :app/ui :show-query-modal?)))

(mx/add-node! st/dag ::queries
  [:edo/settings]
  (fn [_ {:edo/keys [settings]}]
    (sort-by :query (vals (settings :query/id)))))

(mx/add-node! st/dag ::selected-query
  [:edo/app]
  (fn [_ {:edo/keys [app]}]
    (some-> app :app/id :app/ui :selected-query)))

(mx/add-node! st/dag ::query-data
  [:edo/settings :edo/app]
  (fn [_ {:edo/keys [settings app]}]
    (fn [{:keys [query]}]
      (let [favourites (get-in settings [:query/id query :favourites])
            data (get-in app [:app/id query :data])]
        (=>> (enc/into-all [] favourites data)
             (enc/xdistinct :id))))))

;; (mx/listen! st/dag
;;   ::query-data
;;   {:query "armor"}
;;   (fn [_id r]
;;     (println :listen _id :start)
;;     (println :listen _id :r r)))

(mx/add-node! st/dag ::selected-query-data
  [::selected-query ::query-data]
  (fn [_ {::keys [selected-query query-data]}]
    (println :recalculate ::selected-query-data)
    (query-data {:query selected-query})))

(mx/add-node! st/dag ::hovered-tile
  [:edo/app]
  (fn [id {:edo/keys [app]}]
    (timbre/debug id)
    (some-> app :app/id :ui/main :tile-hovered)))
