(ns edo.file-storage.util
  (:require
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [cuerdas.core :as str]
   [ribelo.doxa :as dx]
   [edo.electron.core :refer [app]]
   [edo.transit :refer [write-transit read-transit]]))

(def fs   (js/require "fs"))
(def path (js/require "path"))

(def ^:dynamic *data-path* (enc/path (.getPath ^js app "appData") (.getName ^js app) "_data"))

(defn- keyword->string [nk]
  (let [ns (namespace nk)
        k  (name nk)]
    (str ns "_" k)))

(defn- string->keyword [s]
  (apply keyword (str/split s #"_")))

(defn- store->file-name [store]
  (enc/cond
    (keyword? store) (enc/format "%s/%s.transit" *data-path* (keyword->string store))
    (vector?  store) (enc/format "%s/%s.transit" *data-path*
                                 (enc/str-join-once "__" (mapv keyword->string store)))))

(defn- file-name->store [file-name]
  (enc/cond
    :let [file-name (.basename path file-name ".transit")]

    (pos? (.indexOf file-name "__"))
    (->> (str/split file-name "__") (mapv #(apply keyword (filter seq (str/split % #"_")))))

    :else
    (apply keyword (filter seq (str/split file-name #"_")))))

(defn -write-to-file [file-name data]
  (when-not (.existsSync fs *data-path*) (.mkdirSync fs *data-path* #js {:recursive true}))
  (.writeFile fs file-name (write-transit data)
              (fn [err]
                (if err
                  (timbre/error err)
                  (timbre/infof "successful write %s" file-name)))))


(defn -read-file [file-name]
  (if (.existsSync fs file-name)
    (read-transit (.readFileSync fs file-name))
    (timbre/errorf "file %s does not exist" file-name)))

(defn -read-store-from-file [store]
  (-read-file (store->file-name store)))

(defn freeze-dx [k db]
  (if db
    (let [file-name (store->file-name k)]
      (-write-to-file file-name db))
    (timbre/warnf "can't freeze dx: %s, db is empty" k)))

(defn freeze-store [& ks]
  (doseq [k ks]
    (freeze-dx k (some-> (dx/get-dx k) deref))))

(defn thaw-store [& ks]
  (->> (for [k ks]
         (if-let [data (-read-store-from-file k)]
           (dx/with-dx! [db_ k]
             (swap! db_ (fn [] data)))
           (timbre/errorf "can't thaw %s" k)))
       (filter some?)
       seq))
