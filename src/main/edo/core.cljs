(ns edo.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as rd]
   [re-frame.core :as rf]
   [taoensso.encore :as enc]
   [taoensso.timbre :as timbre]
   [edo.init :as init]
   [edo.ui :as ui]
   [edo.ipc :as ipc]))

(defn mount-components []
  (timbre/info :mount-components)
  (rd/render [#'ui/view] (.getElementById js/document "app"))
  )

(defn ^:export init []
  (timbre/info :init)
  (timbre/info :after)
  (timbre/set-level! :debug)
  (ipc/listen!)
  (rf/dispatch-sync [::init/boot])
  (mount-components)

  )
