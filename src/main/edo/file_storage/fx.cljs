(ns edo.file-storage.fx
  (:require
   [re-frame.core :as rf]
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [ribelo.doxa :as dx]
   [edo.electron.core :refer [app]]
   [edo.transit :refer [write-transit read-transit]]
   [edo.file-storage.util :as u]))

(rf/reg-fx
 :freeze-dx
 (fn [store db]
   (timbre/debug ::freeze-dx store)
   (u/freeze-dx store db)))

;; TODO on-success/failure
(rf/reg-fx
 :freeze-store
 (fn [store]
   (timbre/debug ::freeze-store store)
   (u/freeze-store store)
   ;; (some-> on-success rf/dispatch)
   ;; (some-> on-failure rf/dispatch)
   ))

(rf/reg-fx
 :thaw-store
 (fn [{:keys [store on-success on-failure]}]
   (timbre/debug ::thaw-store store)
   (if (u/thaw-store store)
     (some-> on-success rf/dispatch)
     (some-> on-failure rf/dispatch))))

(rf/reg-fx
 :sync-store
 (fn [store]
   (timbre/debug ::sync-store store)
   (dx/with-dx! [dx_ store]
     (dx/listen! dx_ (enc/merge-keywords [:sync/store store])
                 (fn [db] (when db (u/freeze-dx store db)))))))
