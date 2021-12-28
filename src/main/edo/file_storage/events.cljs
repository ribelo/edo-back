(ns edo.file-storage.events
  (:require
   [taoensso.timbre :as timbre]
   [ribelo.metaxy :as mx]
   [missionary.core :as mi]
   [edo.file-storage.util :as u]
   [edo.store :as st]
   [edo.transit :as t]))

(defn throttle [ms >f]
  (mi/ap
   (let [x (mi/?> (mi/relieve {} >f))]
     (mi/amb> x (do (mi/? (mi/sleep ms)) (mi/amb>))))))

(defmethod mx/event ::freeze-node
  [_ k]
  (mx/reify ::freeze-node
    mx/CustomEvent
    (mx/props [_] k)
    (mx/process [_ dag _]
      (mi/ap
       (timbre/debug ::freeze-node k)
       (when-let [node (dag k)]
         (u/freeze-data k (mi/? @node)))))))

(mx/add-watch!
 (mx/reify ::watch-freeze-node
   mx/EffectEvent
   (mx/effect [_ dag >stream]
     (mi/ap
      (let [[_ >x] (mi/?= (->> >stream
                               (mi/eduction (filter (fn [e] (and (mx/custom-event? e) (keyword-identical? ::freeze-node (mx/id e))))))
                               (mi/group-by mx/props)))]
        (let [e (mi/?> (throttle 5000 >x))]
          (mi/?> (mx/process e dag >stream))))))))

(mx/defupdate ::replace-node
  [e dag k data]
  {k data})

(mx/defwatch ::thaw-node
  [e dag stream k]
  (mi/ap
   (when-let [data (u/thaw-data k)]
     (mx/event ::replace-node k data))))
