(ns edo.sidebar.events
  (:require
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]))

(defn toggle-sidebar []
  (mx/reify ::toggle-sidebar
    mx/WatchEvent
    (mx/watch [_ _ _]
      (mi/ap
        [:commit :edo/app [:dx/update [:app/ui :ui/main] :sidebar-expanded? not]]))))
