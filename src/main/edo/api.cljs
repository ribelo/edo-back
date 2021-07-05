(ns edo.api
  (:require
   [taoensso.timbre :as timbre]))

(defmulti event first)
