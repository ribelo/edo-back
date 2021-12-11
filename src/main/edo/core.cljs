(ns edo.core
  (:require
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [edo.init :as init]
   ;; [edo.ui :as ui]
   ;; [edo.ipc :as ipc]
   [rumext.alpha :as mf]
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]
   [edo.store :as st]
   [edo.ui :as ui]))

(defn mount-graph []
  (when-not (mx/running? st/dag)
    (-> st/dag mx/build! mx/run!)))

(mx/add-node! st/dag ::show-b?
  [:edo/app]
  (fn [_ {:edo/keys [app]}]
    (some-> app :db/id :tmp :show-b?)))

(mx/add-node! st/dag ::show-c?
  [:edo/app]
  (fn [_ {:edo/keys [app]}]
    (some-> app :db/id :tmp :show-c?)))

(defn mount-components []
  (timbre/info :mount-components)
  (mf/mount (mf/element ui/view) (.getElementById js/document "app")))

(defn ^:export init []
  (timbre/info :init)
  (timbre/info :after)
  (timbre/set-level! :debug)

  ;; (ipc/listen!)
  ;; (rf/dispatch-sync [::init/boot])
  (mount-graph)
  (mount-components)
  (st/emit! (init/boot!))

  )

(comment
  (st/emit! [:commit :edo/app [:dx/update [:db/id 1] :a (fnil inc 0)]])



  )
