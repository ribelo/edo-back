(ns edo.file-storage.events
  (:require
   [taoensso.timbre :as timbre]
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]
   [edo.file-storage.util :as u]
   [edo.store :as st]
   [edo.transit :as t]))

(defn freeze-node [k]
  (mx/reify ::freeze-node
    mx/EffectEvent
    (mx/effect [e dag _]
      (timbre/debug (mx/-id e) k)
      (mi/ap
        (when-let [node (dag k)]
          (u/freeze-data k (mi/? node)))))))

;; (mx/add-watch! st/dag
;;   (mx/reify ::watch-freeze-node
;;     mx/WatchEvent
;;     (mx/watch [_ _ >f]
;;       (mi/eduction
;;         (comp
;;           (filter (fn [xs] (keyword-identical? :freeze-node (nth xs 0))))
;;           (map (fn [_ k] (freeze-node k))))
;;         >f))))

(defn replace-node [k data]
  (mx/reify ::replace-node
    mx/UpdateEvent
    (mx/update [_ dag]
      (if (dag k)
        {k data}
        (timbre/errorf "can't thaw, can't replace node %s that dosen't exist!" k)))))

(defn thaw-node [k]
  (mx/reify ::thaw-node
    mx/WatchEvent
    (mx/watch [_ dag _]
      (mi/ap
        (when-let [data (u/thaw-data k)]
          (replace-node k data))))))
