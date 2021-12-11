(ns edo.ui
  (:require
   ["react-transition-group" :refer [Transition CSSTransition TransitionGroup]]
   ["react-feather" :refer [ChevronRight DollarSign ShoppingCart X Plus Trash Heart Eye ArrowDown Edit2]]
   ["react-virtuoso" :refer [Virtuoso]]
   [rumext.alpha :as mf]
   [taoensso.encore :as enc]
   [cuerdas.core :as str]
   [meander.epsilon :as m]
   [edo.init :as init]
   [edo.subs :as sub]
   [edo.events :as evt]
   [edo.ui.subs :as ui.sub]
   [edo.ui.events :as ui.evt]
   [missionary.core :as mi]
   [ribelo.metaxy :as mx]
   [edo.store :as st]
   ))


(mf/defc notification []
  (let [msgs (st/subscribe! ::ui.sub/notifications)]
    [:div {:class "fixed flex flex-row items-center top-0 right-0 mr-4 mt-12"}
     [:div
      [:> TransitionGroup
       (for [[i {:keys [title content type]}]  (mapv vector (range) msgs)]
         ^{:key i}
         [:> CSSTransition {:in              true
                            :timeout         500
                            :unmount-on-exit true
                            :class-names     "notification"}
          (mf/fnc []
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
                content])])])]]]))

(mf/defc modal [{:keys [on-close title bg class children] :as params}]
  [:> CSSTransition {:in              true
                     :timeout         200
                     :unmount-on-exit true}
   (fn []
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
        children]]])])

(mf/defc logo []
  [:div {:class "text-xl text-nord-4 mx-4 w-48 hover:text-nord-7 transition duration-150 easy-in-out"}
   "edo"])

(mf/defc loader []
  [:div {:class "fixed flex inset-0 w-full h-full"}
   [:div {:class "absolute inset-0 w-full h-full bg-nord-3 opacity-50"}]
   [:div {:class "rounded-full h-64 w-64 border-8 border-t-8 m-auto z-50 shadow spinner"
          :style {:border-top-color "#5E81AC"}}]])

(mf/defc query-modal [{:keys [query]}]
  (println :render :modal query :q)
  (let [qname_    (mf/use-state (or query ""))
        size_     (st/subscribe! ::sub/query-size {:query query})
        disabled? (empty? @qname_)]
    [:& modal {:title    "zapytanie"
               :on-close #(st/emit! (evt/toggle-query-modal nil))}
     [:div {:class "flex flex-col"}
      [:div {:class "flex gap-2"}
       [:div {:class "flex flex-col"}
        [:div {:class "text-xs text-nord-5"}
         "kwerenda"]
        [:input {:class       "rounded"
                 :type        :text
                 :auto-focus  true
                 :value       @qname_
                 :on-key-down (fn [^js e]
                                (enc/cond
                                  (= "Enter" (.-key e))
                                  (do (st/emit! (evt/add-new-query {:query @qname_ :size @size_}))
                                      (st/emit! (evt/toggle-query-modal nil)))
                                  (= "Escape" (.-key e))
                                  (st/emit! (evt/toggle-query-modal nil))))
                 :on-change   (fn [^js e] (reset! qname_ (-> e .-target .-value str/lower)))}]]
       [:div {:class "flex flex-col"}
        [:div {:class "text-xs text-nord-5"}
         "ilość jednorazowo pobieranych stron"]
        [:input {:class       "rounded"
                 :type        :number
                 :placeholder "ilość stron"
                 :value       (or @size_ 0)
                 :on-change   (fn [^js e] (reset! size_ (-> e .-target .-value)))}]]]
      [:button {:class    ["flex self-center mt-2 px-4 py-1 bg-nord-0 font-medium"
                           (if-not disabled?
                             "text-nord-4 hover:text-nord-6 hover:bg-nord-7 ring-nord-7"
                             "text-nord-4 hover:text-nord-6 hover:bg-nord-11 ring-nord-11")
                           "outline-none focus:ring"
                           "rounded shadow cursor-pointer border border-gray-900"
                           "transition duration-150 w-24"]
                :disabled disabled?
                :on-click (fn []
                            (st/emit! (evt/add-new-query {:query @qname_ :size @size_}))
                            (st/emit! (evt/toggle-query-modal nil)))}
       [:div {:class "w-full text-center"}
        "dodaj"]]]]))

(comment
  ((mi/sp (mi/? (mx/-value (st/dag ::sub/selected-query)))) prn prn)
  (tap> @st/dag))

(mf/defc query-sidebar []
  (let [queries           @(st/subscribe! ::sub/queries)
        selected-query    @(st/subscribe! ::sub/selected-query)
        edit-query        @(st/subscribe! ::sub/edit-query)
        show-query-modal? @(st/subscribe! ::sub/show-query-modal?)
        ]
    [:* {}
     [:div {:class "flex flex-col text-nord-5"}
      (for [{:keys [query]} queries]
        (do
          [:div {:class    ["flex justify-between cursor-pointer hover:bg-nord-2"
                            (when (= query selected-query) "bg-nord-3 hover:bg-nord-1")]
                 :on-click #(st/emit! (evt/select-query {:query query}))}
           [:div {:class "flex"}
            query]
           [:div {:class "flex gap-2 items-center"}
            [:div {:class    "hover:text-nord-10"
                   :on-click (fn [^js e]
                               (.stopPropagation e)
                               (st/emit! (evt/edit-query {:query query})))}
             [:> Edit2 {:size 18}]]
            [:div {:class    "hover:text-nord-13"
                   :on-click (fn [^js e]
                               (.stopPropagation e)
                               (st/emit! (evt/cleanup-cache {:query query})))}
             [:> Trash {:size 18}]]
            [:div {:class    "hover:text-nord-11"
                   :on-click (fn [^js e]
                               (.stopPropagation e)
                               (st/emit! (evt/remove-query {:query query})))}
             [:> X {:size 18}]]]
           ]))
      [:div {:class    "flex justify-center mt-4 cursor-pointer"
             :on-click #(st/emit! (evt/toggle-query-modal nil))}
       [:> Plus {:size 18}]]]
     (when show-query-modal? [:& query-modal {:query edit-query}])
     ]))

(mf/defc sidebar [{:keys [children]}]
  [:div {:class "flex flex-none"}
   [:div {:class "flex flex-none flex-col w-64 p-4 bg-nord-1 overflow-y-auto shadow-xl transition-all duration-150 ease-in-out"}
    children]])

(mf/defc image-modal [{:keys [img]}]
  (let [pos (mf/use-state [])]
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
                 (st/emit! (evt/hover-tile {:mode :leave :img img}))
                 ))
             :src   img}]]]))

(mf/defc table-row [{:keys [query row]}]
  (let [hovered-tile @(st/subscribe! ::sub/hovered-tile)]
    [:div {:class ["flex h-full w-full gap-2 transition duration-150 ease-in-out"]}
     (for [{:keys [id img price favourite?]} row]
       [:div {:class "flex flex-col my-5 flex-1 h-[212px] self-center truncate"}
        [:div {:class "flex flex-col h-full"}
         [:img {:class "flex-1 cursor-pointer object-scale-down"
                :src   img
                :style {:max-height "180px"
                        :width      :auto}}]
         [:div {:class "flex flex-none justify-between px-4"}
          [:div {:class    "cursor-pointer"
                 :on-click (fn []
                             (st/emit! (evt/open-browser {:url (enc/format "https://zenmarket.jp/en/auction.aspx?itemCode=%s" id)})))}
           price]
          [:div {:on-mouse-enter (fn [_e]
                                   (st/emit! (evt/hover-tile {:mode :enter :img img})))}
           [:> Eye {}]]
          [:div {:class    ["cursor-pointer" (when favourite? "text-nord-11")]
                 :on-click (fn [^js e]
                             (.stopPropagation e)
                             (st/emit! (evt/toggle-favourite {:id id :query query :img img :price price :favourite? favourite?})))}
           [:> Heart {}]]]
         ]
        ])]
    ))

(mf/defc table [{:keys [query]}]
  (let [data        @(st/subscribe! ::sub/query-data {:query query})
        partitioned (partition-all 6 data)]
    [:div {:class "flex flex-col flex-1"}
     [:div {:class ["flex-1 pt-4 px-4 pb-1 border-b border-nord-3"]}
      [:> Virtuoso
       #js {:totalCount  (count partitioned)
            :itemContent (fn [i]
                           (mf/html [:& table-row {:query query :row (nth partitioned i)}]))}]]]))

(mf/defc main-view []
  (let [query @(st/subscribe! ::sub/selected-query)
        cb    (fn [^js x]
                (when (= "Escape" (.-key x))
                  (st/emit! (evt/hover-tile {:mode :leave :img nil}))))]
    (.addEventListener js/document "keydown" cb)
    [:div {:class "flex flex-col h-screen min-h-screen bg-nord-4 text-nord-1"}
     [:div {:class "flex flex-1 overflow-y-hidden overflow-x-hidden"}
      [:& sidebar {}
       [:& query-sidebar]]
      [:div {:class "flex flex-1 flex-col"}
       [:& table {:query query}]
       [:div {:class "flex justify-center my-4 cursor-pointer"}
        [:> ArrowDown
         {:on-click #(st/emit! (evt/fetch-query {:query query}))}]]]]]))

(mf/defc view []
  (let [boot-successful? @(st/subscribe! ::init/boot-successful?)
        show-spinner?    @(st/subscribe! ::ui.sub/show-spinner?)
        hovered-tile     @(st/subscribe! ::sub/hovered-tile)]
    [:div
     (enc/cond
       boot-successful?
       [:& main-view]

       :else
       [:div {:class "flex flex-col justify-center items-center w-screen h-screen inset-0 bg-nord-3 overflow-hidden"}
        [:div {:class "relative flex py-4 text-nord-4 text-6xl hover:text-nord-7 transition duration-150 easy-in-out"}
         [:span
          "edo"]]])
     (when hovered-tile [:& image-modal {:img hovered-tile}])
     ;; [:& notification]
     (when show-spinner? [:& loader])
     ]

    ))
