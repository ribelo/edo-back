(ns edo.ui.subs
  (:require
   [ribelo.metaxy :as mx]
   [edo.store :as st]))

(mx/add-node! st/dag ::notifications
  [:edo/app]
  (fn [_ {:edo/keys [app]}]
    (get-in app [:app/id :ui/main :notifications])))

(mx/add-node! st/dag ::view
  [:edo/app]
  (fn [_ {:edo/keys [app]}]
    (get-in app [:app/id :ui/main :view] :yahoo)))

(mx/add-node! st/dag ::show-spinner?
  [:edo/app]
  (fn [_ {:edo/keys [app]}]
    (get-in app [:app/id :ui/main :show-spinner?])))
