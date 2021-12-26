(ns edo.sidebar.events
  (:require
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]))

(mx/defwatch ::toggle-sidebar [_ _ _]
  (mi/ap [:commit :edo/app [:dx/update [:app/ui :ui/main] :sidebar-expanded? not]]))
