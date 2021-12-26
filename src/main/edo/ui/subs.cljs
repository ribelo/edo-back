(ns edo.ui.subs
  (:require
   [ribelo.metaxy :as mx]
   [edo.store :as st]))

(mx/defnode ::notifications
  [_ {:edo/keys [app]}]
  (get-in app [:app/id :ui/main :notifications]))

(mx/defnode ::view
  [_ {:edo/keys [app]}]
  (get-in app [:app/id :ui/main :view] :yahoo))

(mx/defnode ::show-spinner?
  [_ {:edo/keys [app]}]
  (get-in app [:app/id :ui/main :show-spinner?]))
