(ns edo.db.core
  (:require
   [re-frame.db]
   [ribelo.doxa :as dx]
   [reagent.core :as r]))

(def default-db (dx/create-dx))

(dx/reg-dx! :app re-frame.db/app-db)
(dx/reg-dx! :edo/settings (r/atom (hash-map)))
(dx/reg-dx! :edo/cache  (r/atom (hash-map)))
