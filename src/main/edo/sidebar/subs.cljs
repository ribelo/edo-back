(ns edo.sidebar.subs
  (:require
   [ribelo.metaxy :as mx]
   [edo.store :as st]))

(mx/add-node! st/dag ::sidebar-expanded?
  [:edo/app]
  (fn [_ {:keys [app]}]
    (some-> app :app/ui :ui/main :sidebar-expanded?)))
