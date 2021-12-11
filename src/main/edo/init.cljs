(ns edo.init
  (:require
   [cljs-bean.core :as bean :refer [->js ->clj]]
   [taoensso.timbre :as timbre]
   [missionary.core :as mi]
   [ribelo.metaxy :as mx]
   [edo.store :as st]
   [edo.events]
   [edo.subs]
   [edo.file-storage.events :as fs]
   [edo.ui.events]
   [edo.ui.subs]
   [edo.sidebar.events]
   [edo.sidebar.subs]
   ;; [edo.cofx]
   ;; [edo.subs]
   ;; [edo.events]
   ;; [edo.axios.fx]
   ;; [edo.file-storage.fx]
   ;; [edo.file-storage.events :as fs.evt]
   ;; [edo.db.core]
   ;; [edo.db.fx]
   ;; [edo.db.events :as db.evt]

   ))

(mx/defevent set-boot-successful []
  mx/WatchEvent
  (mx/watch [_ _ _]
    (timbre/info ::set-boot-successful)
    (mi/ap
      (st/commit :edo/app [:dx/put [:app/id :settings] :boot-successful? true]))))

(mx/add-node! st/dag ::boot-successful?
  [:edo/app]
  (fn [_ {:edo/keys [app]}]
    (get-in app [:app/id :settings :boot-successful?])))

(mx/defevent boot! []
  mx/WatchEvent
  (mx/watch [_ _ _]
    (mi/ap
      (mi/amb>
        (fs/thaw-node :edo/settings)
        (set-boot-successful)))))

;; (comment
;;   (rf/dispatch [::boot])
;;   (meta @re-frame.db/app-db))
