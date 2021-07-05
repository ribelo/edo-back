(ns edo.file-storage.events
  (:require
   [taoensso.timbre :as timbre]
   [re-frame.core :as rf]
   [edo.file-storage.fx :as fx]))

(rf/reg-event-fx
 ::freeze-store
 (fn [_ [_ store]]
   {:freeze-store store}))

(rf/reg-event-fx
 ::sync-store
 (fn [_ [_ store]]
   {:sync-store store}))

(rf/reg-event-fx
 ::thaw-store
 (fn [_ [_ store]]
   {:thaw-store {:store store
                     :on-success [::thaw-store-success store]
                     :on-failure [::thaw-store-success store]}}))

(rf/reg-event-fx
 ::thaw-store-success
 (fn [_ [_eid store]]
   (timbre/debug _eid store)))

(rf/reg-event-fx
 ::thaw-store-failure
 (fn [_ [_eid store]]
   (timbre/debug _eid store)))
