(ns edo.interceptors
  (:require
   [re-frame.core :as rf]
   [re-frame.interceptor :as rfi]))

(defn after-dx [store f]
  (rf/->interceptor
   :id :after-dx
   :after (fn [context]
            (println :after-dx)
            (tap> [:context context])
            (let [db (rfi/get-coeffect context store)]
              (tap> [:db db])
              (f store db)
              context))))
