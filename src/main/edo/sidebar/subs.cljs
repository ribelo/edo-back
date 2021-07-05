(ns edo.sidebar.subs
  (:require
   [re-frame.core :as rf]
   [reagent.ratom :refer [reaction]]
   [ribelo.doxa :as dx]))

(rf/reg-sub-raw
 ::sidebar-expanded?
 (fn [_ _]
   (dx/with-dx! [dx_ :app]
     (reaction
      (get-in @dx_ [:app/ui :ui/main :sidebar-expanded?])))))
