(ns edo.sidebar.subs
  (:require
   [ribelo.metaxy :as mx]
   [edo.store :as st]))

(mx/defnode ::sidebar-expanded?
  [_ {:edo/keys [app]}]
  (some-> app :app/ui :ui/main :sidebar-expanded?))
