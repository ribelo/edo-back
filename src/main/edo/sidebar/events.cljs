(ns edo.sidebar.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::toggle-sidebar
 (fn [_ _]
   {:fx [[:commit [:app [:dx/update [:app/ui :ui/main] :sidebar-expanded? not]]]]}))
