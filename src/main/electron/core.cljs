(ns electron.core
  (:require
   [cljs.nodejs :refer [require]]
   [taoensso.timbre :as timbre]
   [taoensso.encore :as enc]
   [edo.transit :refer [write-transit read-transit]]))

(def electron (js/require "electron"))
(def app (.-app electron))
(def ipcMain (.-ipcMain electron))
(def browser-window (.-BrowserWindow electron))
(def crash-reporter (.-crashReporter electron))
(def electron-updater (js/require "electron-updater"))
(def auto-updater (.-autoUpdater electron-updater))

(def main-window (atom nil))

(declare listen-updates-available! check-for-updates-and-notify! send!)

(defn init-browser! []
  (reset! main-window (browser-window.
                       #js {:width           800
                            :height          600
                            :minWidth        800
                            :minHeight       600
                            :autoHideMenuBar true
                            :useContentSize  true
                            :fullscreen      false
                            :webPreferences  #js{:enableRemoteModule      true
                                                 :contextIsolation        false
                                                 :nodeIntegration         true
                                                 :nodeIntegrationInWorker true}}))
  (.maximize ^js/electron.BrowserWindow @main-window)
                                        ;(.setMenu @main-window nil)
                                        ; Path is relative to the compiled js file (main.js in our case)
                                        ;(.log js/console (str "file://" js/__dirname "/public/index.html"))
                                        ;(.loadURL @main-window "http://localhost:3449/index.html")
  (.loadURL ^js/electron.BrowserWindow @main-window (str "file://" js/__dirname "/public/index.html"))
  #_(.loadURL ^js/electron.BrowserWindow @main-window (str "http://localhost:8020/public/index.html"))
  (.on ^js/electron.BrowserWindow @main-window "closed" #(reset! main-window nil))
  ;; (.on ^js/electron.BrowserWindow @main-window "ready-to-show"
  ;;      (fn []
  ;;        (listen-updates-available!)
  ;;        (check-for-updates-and-notify!)))
  )

(defmulti <-renderer first)

(defmethod <-renderer :timbre/debug
  [[_ msg]]
  (timbre/debug msg))

(defmethod <-renderer :timbre/info
  [[_ msg]]
  (timbre/info msg))

(defmethod <-renderer :timbre/warn
  [[_ msg]]
  (timbre/warn msg))

(defmethod <-renderer :timbre/error
  [[_ msg]]
  (timbre/error msg))

(defmethod <-renderer :app/version
  [[_ args]]
  [:app/version (.getVersion ^js app)])

(defn send! [v]
  (.send ^js (.-webContents ^js @main-window) "message" (write-transit v)))

(defn check-for-updates-and-notify! []
  (-> (.checkForUpdatesAndNotify auto-updater)
      (.then  (fn [_e tmp]
                (send! [:timbre/info  "successful check for updates and notify"])))
      (.catch (fn [err]
                (send! [:timbre/error (str "failure check for updates and notify " err)])))))

(defn listen-updates-available! []
  (.on auto-updater "update-downloaded"
       (fn [_]
         (send! [:timbre/info "successful download update"])
         (send! [:re-frame/dispatch [:edo.ui.events/add-notification
                                     ::successful-download-update
                                     {:content "pobrano najnowszą wersję aplikacji"}]])
         (dotimes [n 10]
           (let [msg (enc/format "going to quit and install app in - %ss"
                                 (- 10 n))]
             (js/setTimeout
                 (fn []
                   (send! [:timbre/warn (enc/format "going to quit and install app in - %ss"
                                                    (- 9 n))])
                   (send! [:re-frame/dispatch [:edo.ui.events/add-notification
                                               [::going-to-install n]
                                               {:content msg}]]))
                 (enc/ms :secs n))))
         (js/setTimeout (fn [] (.quitAndInstall auto-updater)) (enc/ms :secs 10)))))

(defn main []
  (.on app "ready" init-browser!)
  (.on app "window-all-closed"
       #(when-not (= js/process.platform "darwin") (.quit app)))
  (.on ipcMain "message"
       (fn [^js e x]
         (let [event (read-transit x)
               v     (<-renderer event)]
           (.reply e "reply" (write-transit v))))))
