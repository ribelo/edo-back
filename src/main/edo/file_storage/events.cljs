(ns edo.file-storage.events
  (:require
   [taoensso.timbre :as timbre]
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]
   [edo.file-storage.util :as u]
   [edo.store :as st]
   [edo.transit :as t]))

(mx/defeffect ::freeze-node
  [e dag _stream k]
  (timbre/debug (mx/id e) k)
  (mi/ap
   (when-let [node (dag k)]
     (u/freeze-data k (mi/? @node)))))

(mx/defupdate ::replace-node
  [e dag k data]
  (if (dag k)
    {k data}
    (timbre/errorf "can't thaw, can't replace node %s that dosen't exist!" k)))

(mx/defwatch ::thaw-node
  [e dag stream k]
  (mi/ap
   (when-let [data (u/thaw-data k)]
     (mx/event ::replace-node k data))))
