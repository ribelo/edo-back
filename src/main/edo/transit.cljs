(ns edo.transit
  (:require
   [cognitect.transit :as t]
   [cljs-bean.transit :as bt]
   [meander.epsilon :as m]))

(def write-handlers
  (bt/writer-handlers))

(defn write-transit [s]
  (t/write (t/writer :json {:handlers write-handlers :transform t/write-meta}) s))

(defn read-transit [s]
  (t/read (t/reader :json) s))
