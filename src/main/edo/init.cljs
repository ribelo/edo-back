(ns edo.init
  (:require
   [cljs-bean.core :as bean :refer [->js ->clj]]
   [taoensso.timbre :as timbre]
   [missionary.core :as mi]
   [ribelo.metaxy :as mx]
   [edo.store :as st]
   [edo.events :as evt]
   [edo.subs]
   [edo.file-storage.events :as fs]
   [edo.ui.events]
   [edo.ui.subs]
   [edo.sidebar.events]
   [edo.sidebar.subs]
   ))

(comment

  (mx/dispatch ::sex)
  ((mi/sp (println :mbx (mx/id (mi/? mx/mbx)))) prn prn))


(mx/defwatch ::set-boot-successful [_ _ _]
  (timbre/info ::set-boot-successful)
  (mi/ap
   (mx/event :commit :edo/app [:dx/put [:app/id :settings] :boot-successful? true])))

(mx/defnode ::boot-successful?
  [_ {:edo/keys [app]}]
  (get-in app [:app/id :settings :boot-successful?]))

(comment
  @(mx/value ::boot-successful?)
  )

(mx/defwatch ::boot! [_ _ _]
  (mi/ap
   (mi/amb>
    (mx/event ::fs/thaw-node :edo/settings)
    (mx/event ::set-boot-successful)
    (mi/? (mi/sleep 3000))
    (mx/event ::evt/run-auto-fetch))))

;; (comment
;;   (rf/dispatch [::boot])
;;   (meta @re-frame.db/app-db))
