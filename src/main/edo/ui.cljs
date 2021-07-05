(ns edo.ui
  (:require
   ["react-transition-group" :refer [Transition CSSTransition TransitionGroup]]
   ["react-feather" :refer [ChevronRight DollarSign ShoppingCart X Plus]]
   ["react-virtualized/dist/commonjs/AutoSizer" :default auto-sizer]
   ["react-virtualized/dist/commonjs/List" :default virtual-list]
   ["react-virtualized/dist/commonjs/Masonry" :default masonry]
   [taoensso.encore :as enc]
   [cuerdas.core :as str]
   [meander.epsilon :as m]
   [edo.init :as init]
   [edo.subs :as sub]
   [edo.events :as evt]
   [edo.ui.subs :as ui.sub]
   [edo.ui.events :as ui.evt]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.ratom :as ra]))


(defn notification []
  (let [msgs @(rf/subscribe [::ui.sub/notifications])]
    [:div {:class "fixed flex flex-row items-center top-0 right-0 mr-4 mt-12"}
     [:div
      [:> TransitionGroup
       (for [[i {:keys [title content type]}]  (mapv vector (range) msgs)]
         ^{:key i}
         [:> CSSTransition {:in              true
                            :timeout         500
                            :unmount-on-exit true
                            :class-names     "notification"}
          [:div {:class ["min-h-24 w-64 bg-nord-3 z-50 shadow my-4"
                         (case type
                           :error   ["border" "border-nord-11"]
                           :success ["border" "border-nord-14"]
                           nil)]}
           (when title
             [:div {:class "px-4 py-2 flex-1 font-medium tracking-wider text-nord-4"}
              title])
           (when content
             [:div {:class "px-4 py-2 text-nord-4 text-xs break-words"}
              content])]])]]]))

(defn modal
  [_params _content]
  (fn [{:keys [show? on-close title bg class] :as params} content]
    [:> CSSTransition {:in              show?
                       :timeout         200
                       :unmount-on-exit true
                       :class-names     "modal"}
     (fn [_]
       (r/as-element
        ^{:key :modal}
        [:div {:class "fixed flex inset-0 w-full h-full z-50"}
         [:div {:class    "absolute inset-0 w-full h-full bg-nord-0 opacity-25"
                :on-click on-close}]
         [:div {:class ["min-h-24 m-auto z-40 shadow" (if bg bg "bg-nord-3")]}
          [:div {:class "border-b border-nord-0 bg-nord-3"}
           (when title
             [:div {:class "px-4 py-2 flex items-center"}
              [:div {:class "flex-1 text-xl font-medium tracking-wider text-nord-4"}
               title]
              [:> X {:class    "text-nord-4 cursor-pointer hover:text-nord-11
                                   transition duration-150 easy-in-out"
                     :on-click on-close}]])]
          [:div {:class "px-4 py-2"}
           content]]]))]))

(defn logo []
  (fn []
    [:div {:class "text-xl text-nord-4 mx-4 w-48 hover:text-nord-7 transition duration-150 easy-in-out"}
     "edo"]))

(defn loader []
  [:div {:class "fixed flex inset-0 w-full h-full"}
   [:div {:class "absolute inset-0 w-full h-full bg-nord-3 opacity-50"}]
   [:div {:class "rounded-full h-64 w-64 border-8 border-t-8 m-auto z-50 shadow spinner"
          :style {:border-top-color "#5E81AC"}}]])

(defn query-modal []
  (let [qname     (r/atom nil)
        qtext     (r/atom nil)
        disabled? (ra/reaction (or (empty? @qname) (empty? @qtext)))
        show?_    (rf/subscribe [::sub/show-query-modal?])]
    (fn []
      [modal {:show?    @show?_
              :title    "zapytanie"
              :on-close #(rf/dispatch [::evt/toggle-query-modal])}
       [:div {:class "flex flex-col"}
        [:div {:class "flex gap-2"}
         [:input {:class       "rounded"
                  :type        :text
                  :auto-focus  true
                  :placeholder "nazwa"
                  :value       @qname
                  :on-key-down (fn [^js e]
                                 (enc/cond
                                   (= "Enter" (.-key e))
                                   (do (rf/dispatch [::evt/add-new-query @qname @qtext])
                                       (rf/dispatch [::evt/toggle-query-modal]))
                                   (= "Escape" (.-key e))
                                   (rf/dispatch [::evt/toggle-query-modal])))
                  :on-change   (fn [^js e] (reset! qname (-> e .-target .-value str/lower)))}]
         [:input {:class       "rounded"
                  :type        :text
                  :placeholder "kwerenda"
                  :value       @qtext
                  :on-key-down (fn [^js e]
                                 (enc/cond
                                   (= "Enter" (.-key e))
                                   (do (rf/dispatch [::evt/add-new-query @qname @qtext])
                                       (rf/dispatch [::evt/toggle-query-modal]))
                                   (= "Escape" (.-key e))
                                   (rf/dispatch [::evt/toggle-query-modal])))
                  :on-change   (fn [^js e] (reset! qtext (-> e .-target .-value)))}]]
        [:button {:class    ["flex self-center mt-2 px-4 py-1 bg-nord-0 font-medium"
                          (if-not @disabled?
                            "text-nord-4 hover:text-nord-6 hover:bg-nord-7 ring-nord-7"
                            "text-nord-4 hover:text-nord-6 hover:bg-nord-11 ring-nord-11")
                          "outline-none focus:ring"
                          "rounded shadow cursor-pointer border border-gray-900"
                          "transition duration-150 w-24"]
                  :disabled @disabled?
                  :on-click (fn []
                              (rf/dispatch [::evt/add-new-query @qname @qtext])
                              (rf/dispatch [::evt/toggle-query-modal]))}
         [:div {:class "w-full text-center"}
          "dodaj"]]]])))

(defn query-sidebar []
  (let [queries        @(rf/subscribe [::sub/queries])
        selected-query @(rf/subscribe [::sub/selected-query])]
    [:<>
     [:div {:class "flex flex-col text-nord-5"}
      (doall
       (for [{:keys [name query]} queries]
         ^{:key name}
         [:div {:class    ["flex justify-between cursor-pointer hover:bg-nord-2"
                           (when (= name (:name selected-query)) "bg-nord-3 hover:bg-nord-1")]
                :on-click #(rf/dispatch [::evt/select-query name query])}
          [:div {:class "flex"}
           name]
          [:div {:on-click (fn [^js e]
                             (.stopPropagation e)
                             (rf/dispatch [::evt/remove-query name]))}
           [:> X]]]))
      [:div {:class    "flex justify-center mt-4 cursor-pointer"
             :on-click #(rf/dispatch [::evt/toggle-query-modal])}
       [:> Plus]]]
     [query-modal]]))

(defn sidebar [content]
  [:div {:class "flex flex-none"}
   [:div {:class "flex flex-none flex-col w-64 p-4 bg-nord-1 overflow-y-auto shadow-xl transition-all duration-150 ease-in-out"}
    content]])

(defn table-row [row]
  [:div {:class ["flex h-full w-full gap-2 transition duration-150 ease-in-out"]}
   (for [{:keys [id url img]} row]
     ^{:key id}
     [:div {:class "flex-1 self-center truncate"}
      [:div {:class "cursor-pointer"
             :on-click (fn []
                         (rf/dispatch [::evt/add-to-favourites id])
                         (rf/dispatch [::evt/open-browser url]))}
       [:img {:src img
              :style {:max-height "180px"
                      :max-width  "180px"
                      :height     :auto
                      :width      :auto}}]]])])

(defn table [{:keys [name query]}]
  (let [data @(rf/subscribe [::sub/query-data name])
        partitioned (partition 6 data)]
    [:div {:class "flex flex-col flex-1 px-4 pt-4 pb-1"}
     [:div {:class ["flex-1 border-b border-nord-3"]}
      [:> auto-sizer
       (fn [^js sizer]
         (r/as-element
          [:> virtual-list
           {:class       ["focus:outline-none !overflow-y-scroll"]
            :height      (.-height sizer)
            :width       (.-width sizer)
            :rowHeight   200
            :rowCount    (count partitioned)
            :onScroll    (fn [^js m]
                           (when (>= (+ (.-scrollTop m) (.-clientHeight m) 96)
                                     (.-scrollHeight m))
                             (rf/dispatch [::evt/fetch-query name query])))
            :rowRenderer (fn [^js m]
                           (r/as-element
                            ^{:key (.-key m)}
                            [:div {:style (.-style m)}
                             [table-row (nth partitioned (.-index m))]]))}]))]]]))

(defn main-view []
  (let [query @(rf/subscribe [::sub/selected-query])]
    [:div {:class "flex flex-col h-screen min-h-screen bg-nord-4 text-nord-1"}
     [:div {:class "flex flex-1 overflow-y-hidden overflow-x-hidden"}
      [sidebar [query-sidebar]]
      [table query]]]))

(defn view []
  (let [boot-successful? @(rf/subscribe [::init/boot-successful?])
        app-version      @(rf/subscribe [:edo/version])
        show-spinner?    @(rf/subscribe [::ui.sub/show-spinner?])]
    [:div
     (enc/cond
       boot-successful?
       [main-view]

       :else
       [:div {:class "flex flex-col justify-center items-center w-screen h-screen inset-0 bg-nord-3 overflow-hidden"}
        [:div {:class "relative flex py-4 text-nord-4 text-6xl hover:text-nord-7 transition duration-150 easy-in-out"}
         [:span
          "edo"]
         [:> CSSTransition {:in true
                            :timeout 1000
                            :appear true
                            :class-names {:appear        "transition-opacity duration-1000 opacity-0"
                                          :appear-active "transition-opacity duration-1000 opacity-100"}}
          [:span {:class "absolute text-nord-4 text-sm right-0"}
           app-version]]]])
     [notification]
     (when show-spinner? [loader])]

    ))
