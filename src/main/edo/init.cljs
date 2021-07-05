(ns edo.init
  (:require
   [reagent.ratom :as ra :refer [reaction]]
   [re-frame.core :as rf]
   [day8.re-frame.async-flow-fx]
   [cljs-bean.core :as bean :refer [->js ->clj]]
   [shadow.resource :as rc]
   [taoensso.timbre :as timbre]
   [ribelo.doxa :as dx]
   [edo.fx]
   [edo.cofx]
   [edo.subs]
   [edo.events]
   [edo.axios.fx]
   [edo.file-storage.fx]
   [edo.file-storage.events :as fs.evt]
   [edo.db.core]
   [edo.db.fx]
   [edo.db.events :as db.evt]

   ))

(rf/reg-event-fx
 ::set-boot-successful
 (fn [_ _]
   (timbre/info ::set-boot-successful)
   {:fx [[:commit [:app [:dx/put [:app/id :settings] :boot-successful? true]]]]}))

(rf/reg-sub-raw
 ::boot-successful?
 (fn [_ _]
   (dx/with-dx! [dx_ :app]
     (reaction
      (get-in @dx_ [:app/id :settings :boot-successful?])))))

(rf/reg-event-fx
 ::boot
 (fn [{:keys [db]} _]
   {:fx [[:dispatch [::db.evt/init-db]]
         [:thaw-store {:store :edo/settings}]
         [:dispatch [::set-boot-successful]]
         ]}))

(comment
  (rf/dispatch [::boot])
  (meta @re-frame.db/app-db))

