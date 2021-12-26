(ns edo.core
  (:require
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [edo.init :as init]
   [edo.ui :as ui]
   [rumext.alpha :as mf]
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]
   [edo.store :as st]
   ))

(defn mount-graph []
  (mx/run!))

(defn mount-components []
  (timbre/info :mount-components)
  (mx/run!)
  (mf/mount (mf/element ui/view) (.getElementById js/document "app"))
  )

(defn ^:export init []
  (timbre/info :init)
  (timbre/info :after)
  (timbre/set-level! :debug)

  ;; (ipc/listen!)
  ;; (rf/dispatch-sync [::init/boot])
  (mount-components)
  (mx/dispatch ::init/boot!)

  )

(comment
  (mx/dispatch [:commit :edo/app [:dx/update [:db/id 1] :a (fnil inc 0)]])



  )
