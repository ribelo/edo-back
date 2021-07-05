(ns edo.electron.core
  (:refer-clojure :exclude [require]))

(defonce electron (js/require "electron"))
(defonce shell    (.-shell electron))
(defonce remote (.-remote ^js electron))
(defonce app (.-app ^js remote ))
(defonce browser-window (.-BrowserWindow ^js remote))
(defonce current-window (.getCurrentWindow ^js remote))
(defonce ipc-renderer (.-ipcRenderer ^js electron))
(defonce dialog (.-dialog ^js remote))


(comment
  (.getPath app "appData"))
