(ns edo.ui
  (:require
   ["react-transition-group" :refer [Transition CSSTransition TransitionGroup]]
   ["react-feather" :refer [ChevronRight DollarSign ShoppingCart X Plus Trash Heart Eye ArrowDown Edit2]]
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
                       :unmount-on-exit true}
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
  (let [qname_    (r/atom nil)
        size_     (r/atom nil)
        disabled? (ra/reaction (empty? @qname_))
        show?_    (rf/subscribe [::sub/show-query-modal?])
        edit_     (rf/subscribe [::sub/edit-query])]
    (fn []
      (println :qname @qname_ (type @qname_) @edit_)
      [modal {:show?    @show?_
              :title    "zapytanie"
              :on-close (fn []
                          (rf/dispatch [::evt/toggle-query-modal])
                          (reset! qname_ nil)
                          (reset! size_  nil))}
       [:div {:class "flex flex-col"}
        [:div {:class "flex gap-2"}
         [:div {:class "flex flex-col"}
          [:div {:class "text-xs text-nord-5"}
           "kwerenda"]
          [:input {:class       "rounded"
                   :type        :text
                   :auto-focus  true
                   :value       (or @qname_ @(rf/subscribe [::sub/query-text {:query @edit_}]))
                   :on-key-down (fn [^js e]
                                  (enc/cond
                                    (= "Enter" (.-key e))
                                    (do (rf/dispatch [::evt/add-new-query {:query @qname_ :size @size_}])
                                        (rf/dispatch [::evt/toggle-query-modal])
                                        (reset! qname_ nil)
                                        (reset! size_  nil))
                                    (= "Escape" (.-key e))
                                    (rf/dispatch [::evt/toggle-query-modal])))
                   :on-change   (fn [^js e] (reset! qname_ (-> e .-target .-value str/lower)))}]]
         [:div {:class "flex flex-col"}
          [:div {:class "text-xs text-nord-5"}
           "ilość jednorazowo pobieranych stron"]
          [:input {:class       "rounded"
                   :type        :number
                   :placeholder "ilość stron"
                   :value       (or @size_ @(rf/subscribe [::sub/query-size {:query @edit_}]))
                   :on-change (fn [^js e]
                                (reset! size_ (-> e .-target .-value))
                                (rf/dispatch [::evt/add-new-query {:query @qname_ :size @size_}]))}]]]
        [:button {:class    ["flex self-center mt-2 px-4 py-1 bg-nord-0 font-medium"
                             (if-not @disabled?
                               "text-nord-4 hover:text-nord-6 hover:bg-nord-7 ring-nord-7"
                               "text-nord-4 hover:text-nord-6 hover:bg-nord-11 ring-nord-11")
                             "outline-none focus:ring"
                             "rounded shadow cursor-pointer border border-gray-900"
                             "transition duration-150 w-24"]
                  :disabled @disabled?
                  :on-click (fn []
                              (rf/dispatch [::evt/add-new-query {:query @qname_ :size @size_}])
                              (rf/dispatch [::evt/toggle-query-modal])
                              (reset! qname_ nil)
                              (reset! size_  nil))}
         [:div {:class "w-full text-center"}
          "dodaj"]]]])))

(defn query-sidebar []
  (let [queries        @(rf/subscribe [::sub/queries])
        selected-query @(rf/subscribe [::sub/selected-query])
        edit-query     @(rf/subscribe [::sub/edit-query])]
    [:<>
     [:div {:class "flex flex-col text-nord-5"}
      (doall
       (for [{:keys [query]} queries]
         ^{:key query}
         [:div {:class    ["flex justify-between cursor-pointer hover:bg-nord-2"
                           (when (= query selected-query) "bg-nord-3 hover:bg-nord-1")]
                :on-click #(rf/dispatch [::evt/select-query query])}
          [:div {:class "flex"}
           query]
          [:div {:class "flex items-center"}
           [:div {:class    "hover:text-nord-11"
                  :on-click (fn [^js e]
                              (.stopPropagation e)
                              (rf/dispatch [::evt/edit-query {:query query}]))}
            [:> Edit2
             {:size 18}]]
           [:div {:class    "hover:text-nord-11"
                  :on-click (fn [^js e]
                              (.stopPropagation e)
                              (rf/dispatch [::evt/cleanup-cache query]))}
            [:> Trash
             {:size 18}]]
           [:div {:class    "hover:text-nord-11"
                  :on-click (fn [^js e]
                              (.stopPropagation e)
                              (rf/dispatch [::evt/remove-query query]))}
            [:> X
             {:size 18}]]]]))
      [:div {:class    "flex justify-center mt-4 cursor-pointer"
             :on-click #(rf/dispatch [::evt/toggle-query-modal])}
       [:> Plus]]]
     [query-modal {:query edit-query}]]))

(defn sidebar [content]
  [:div {:class "flex flex-none"}
   [:div {:class "flex flex-none flex-col w-64 p-4 bg-nord-1 overflow-y-auto shadow-xl transition-all duration-150 ease-in-out"}
    content]])

(defn image-modal [img]
  (let [pos (r/atom [])]
    (fn [img]
      [:div {:class "fixed flex inset-0 w-full h-full z-50 bg-nord-0 bg-opacity-50 justify-center items-center"}
       [:div {:class "pl-64 z-100"}
        [:img {:class "flex w-[66vw] h-[66vw] cursor-pointer object-contain"
               :on-mouse-move
               (fn [^js e]
                 (enc/cond
                   :let [[x' y'] @pos
                         x       (.-clientX e)
                         y       (.-clientY e)
                         w       (.-clientWidth js/document.body)
                         h       (.-clientHeight js/document.body)]
                   (or (not x') (not y'))
                   (reset! pos [(.-clientX e) (.-clientY e)])

                   (or (< 0.05 (/ (js/Math.abs (- (js/Math.abs x) (js/Math.abs x'))) w))
                       (< 0.05 (/ (js/Math.abs (- (js/Math.abs y) (js/Math.abs y'))) h)))
                   (rf/dispatch [::evt/hover-tile :leave img])))
               :src   img}]]])))

(defn table-row [query row]
  (let [hovered-tile @(rf/subscribe [::sub/hovered-tile])]
    (into [:div {:class ["flex h-full w-full gap-2 transition duration-150 ease-in-out"]}]
          (map (fn [{:keys [id img price favourite?]}]
                 [:div {:class "flex flex-col my-5 flex-1 h-[212px] self-center truncate"}
                  [:div {:class "flex flex-col h-full"}
                   [:img {:class "flex-1 cursor-pointer object-scale-down"
                          :src   img
                          :style {:max-height "180px"
                                  :width      :auto}}]
                   [:div {:class "flex flex-none justify-between px-1"}
                    [:div {:class "cursor-pointer"
                           :on-click (fn []
                                       (rf/dispatch [::evt/open-browser (enc/format "https://zenmarket.jp/en/auction.aspx?itemCode=%s" id)]))}
                     price]
                    [:div {:on-mouse-enter (fn [_e]
                                             (rf/dispatch [::evt/hover-tile :enter img]))
                           :on-mouse-leave (fn [_e]
                                             (when-not hovered-tile
                                               (rf/dispatch [::evt/hover-tile :leave img])))}
                     [:> Eye]]
                    [:div {:class    ["cursor-pointer" (when favourite? "text-nord-11")]
                           :on-click (fn [^js e]
                                       (.stopPropagation e)
                                       (rf/dispatch [::evt/toggle-favourite id query img price favourite?]))}
                     [:> Heart]]]]]))
          row)))

(defn table [query]
  (let [data @(rf/subscribe [::sub/query-data query])
        partitioned (partition-all 6 data)]
    [:div {:class "flex flex-col flex-1"}
     [:div {:class ["flex-1 pt-4 pb-1 border-b border-nord-3"]}
      [:> auto-sizer
       (fn [^js sizer]
         (r/as-element
          [:> virtual-list
           {:class       ["focus:outline-none px-4 !overflow-y-scroll"]
            :height      (.-height sizer)
            :width       (.-width sizer)
            :rowHeight   220
            :rowCount    (count partitioned)
            :rowRenderer (fn [^js m]
                           (r/as-element
                            ^{:key (.-key m)}
                            [:div {:style (.-style m)}
                             [table-row query (nth partitioned (.-index m))]]))}]))]]]))

(defn main-view []
  (let [query @(rf/subscribe [::sub/selected-query])
        cb    (fn [^js x]
                (when (= "Escape" (.-key x))
                  (rf/dispatch [::evt/hover-tile :leave nil])))
        _     (.addEventListener js/document "keydown" cb)]
    [:div {:class "flex flex-col h-screen min-h-screen bg-nord-4 text-nord-1"}
     [:div {:class "flex flex-1 overflow-y-hidden overflow-x-hidden"}
      [sidebar [query-sidebar]]
      [:div {:class "flex flex-1 flex-col"}
       [table query]
       [:div {:class "flex justify-center my-4 cursor-pointer"}
        [:> ArrowDown
         {:on-click #(rf/dispatch [::evt/fetch-query {:query query}])}]]]]]))

(defn view []
  (let [boot-successful? @(rf/subscribe [::init/boot-successful?])
        app-version      @(rf/subscribe [:edo/version])
        show-spinner?    @(rf/subscribe [::ui.sub/show-spinner?])
        hovered-tile     @(rf/subscribe [::sub/hovered-tile])]
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
     (when hovered-tile [image-modal hovered-tile])
     [notification]
     (when show-spinner? [loader])]

    ))
