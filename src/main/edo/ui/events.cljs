(ns edo.ui.events
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [taoensso.encore :as enc]
   [meander.epsilon :as m]
   [ribelo.doxa :as dx]))

(rf/reg-event-fx
 ::add-notification
 (fn [_ [_ id notification]]
   (enc/have! notification)
   (let [pred-fn #(not (enc/rsome (partial = notification) %))]
     {:fx [[:commit       [:app [[:dx/match  [:app/id :ui/main] :notifications pred-fn]
                                 [:dx/conj   [:app/id :ui/main] :notifications notification]]]]
           [:commit-later [(enc/ms :secs 3)  [id notification]
                           :app [:dx/delete  [:app/id :ui/main] :notifications notification]]]]})))

(rf/reg-event-fx
 ::data-loading
 (fn [_ [_ v]]
   {:fx [[:commit [:app [:dx/put [:app/id :ui/main] :show-spinner? v]]]]}))

(rf/reg-event-fx
 ::data-loading-ms
 (fn [_ [_eid id ms]]
   {:fx [[:commit       [      :app [:dx/put    [:app/id :ui/main] :show-spinner? true]]]
         [:commit-later [ms id :app [:dx/delete [:app/id :ui/main] :show-spinner?]]]]}))

(comment
  (dx/with-dx! [app_ :app]
    (tap> @app_))
  )
