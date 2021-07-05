(ns edo.store
  (:require
   [meander.epsilon :as m]))



(defmacro reg-event
  [id argsv & body]
  `(defmethod event ~id
     ~argsv
     ~@body))
