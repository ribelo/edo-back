(ns edo.util
  (:require
   [cljs.core.async :as a :refer [<! go]]
   [taoensso.encore :as enc]
   [cljs-bean.core :refer [bean ->clj ->js]]
   [cuerdas.core :as str]))

(defn <memoize
  ([f] (<memoize nil f))
  ([ms f]
   (let [cache_     (volatile! {})
         ttl-cache_ (volatile! {})]
     (fn [& xs]
       (enc/cond
         :let [v (@cache_ xs)
               t (@ttl-cache_ xs)
               c (a/chan)]
         (or (not v) (and (some? ms) (>= (enc/now-udt) t)))
         (go (let [v (<! (apply f xs))]
               (vswap! cache_ assoc xs v)
               (some->> ms (+ (enc/now-udt)) (vswap! ttl-cache_ assoc xs))
               v))
         ;;
         (and (some? v) (or (nil? ms) (< (enc/now-udt) t)))
         (do (some->> v (a/put! c)) c))))))
