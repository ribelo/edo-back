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
  (let [msgs (mx/subscribe ::ui.sub/notifications)]
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
  (let [qname_    (mf/use-state (or query ""))
        size_     (mx/subscribe ::sub/query-size {:query query})
        disabled? (empty? @qname_)]
    [:& modal {:title    "zapytanie"
               :on-close #(mx/dispatch ::evt/toggle-query-modal nil)}
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
                                  (do (mx/dispatch ::evt/add-new-query {:query @qname_ :size @size_})
                                      (mx/dispatch ::evt/toggle-query-modal nil))
                                  (= "Escape" (.-key e))
                                  (mx/dispatch ::evt/toggle-query-modal nil)))
                 :on-change   (fn [^js e] (reset! qname_ (-> e .-target .-value str/lower)))}]]]
      [:button {:class    ["flex self-center mt-2 px-4 py-1 bg-nord-0 font-medium"
                           (if-not disabled?
                             "text-nord-4 hover:text-nord-6 hover:bg-nord-7 ring-nord-7"
                             "text-nord-4 hover:text-nord-6 hover:bg-nord-11 ring-nord-11")
                           "outline-none focus:ring"
                           "rounded shadow cursor-pointer border border-gray-900"
                           "transition duration-150 w-24"]
                :disabled disabled?
                :on-click (fn []
                            (mx/dispatch ::evt/add-new-query {:query @qname_ :a 1})
                            ;; (mx/dispatch ::evt/toggle-query-modal nil)
                            )}
       [:div {:class "w-full text-center"}
        "dodaj"]]]]))

(mf/defc query-sidebar []
  (let [queries           @(mx/subscribe ::sub/queries)
        selected-query    @(mx/subscribe ::sub/selected-query)
        edit-query        @(mx/subscribe ::sub/edit-query)
        show-query-modal? @(mx/subscribe ::sub/show-query-modal?)
        ]
    [:* {}
     [:div {:class "flex flex-col text-nord-5"}
      (for [{:keys [query]} queries]
        (do
          [:div {:class    ["flex justify-between cursor-pointer hover:bg-nord-2"
                            (when (= query selected-query) "bg-nord-3 hover:bg-nord-1")]
                 :on-click #(mx/dispatch ::evt/select-query {:query query})}
           [:div {:class "flex"}
            query]
           [:div {:class "flex gap-2 items-center"}
            [:div {:class    "hover:text-nord-10"
                   :on-click (fn [^js e]
                               (.stopPropagation e)
                               (mx/dispatch ::evt/edit-query {:query query}))}
             [:> Edit2 {:size 18}]]
            [:div {:class    "hover:text-nord-13"
                   :on-click (fn [^js e]
                               (.stopPropagation e)
                               (mx/dispatch ::evt/cleanup-cache {:query query}))}
             [:> Trash {:size 18}]]
            [:div {:class    "hover:text-nord-11"
                   :on-click (fn [^js e]
                               (.stopPropagation e)
                               (mx/dispatch ::evt/remove-query {:query query}))}
             [:> X {:size 18}]]]
           ]))
      [:div {:class    "flex justify-center mt-4 cursor-pointer"
             :on-click #(mx/dispatch ::evt/toggle-query-modal nil)}
       [:> Plus {:size 18}]]]
     (when show-query-modal? [:& query-modal {:query edit-query}])
     ]))

(mf/defc sidebar [{:keys [children]}]
  [:div {:class "flex flex-none"}
   [:div {:class "flex flex-none flex-col w-64 p-4 bg-nord-1 overflow-y-auto shadow-xl transition-all duration-150 ease-in-out"}
    [:div {:class "text-nord-4 mb-4"}
     "edo 0.2.3"]
    children]])

(mf/defc image-modal [{:keys [img]}]
  (let [pos (mf/use-state [])]
    [:div {:class "fixed flex inset-0 w-full h-full z-50 bg-nord-0 bg-opacity-50 justify-center items-center"}
     [:div {:class "pl-64 z-100"}
      [:img {:class "flex w-[66vw] h-[66vh] cursor-pointer object-contain"
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
                 (mx/dispatch ::evt/hover-tile {:mode :leave :img img})
                 ))
             :src   img}]]]))

(mf/defc table-cell [{:keys [query id img price favourite?]}]
  (let [seller @(mx/subscribe ::sub/auction-seller {:item-id id})]
    [:div {:class "flex flex-col h-[234px] my-5 flex-1 self-center truncate"}
     [:div {:class "flex flex-col h-full"}
      [:div {:class    "text-center min-h-[24px] cursor-pointer"
             :on-click (fn [_] (when seller (mx/dispatch ::evt/open-browser {:url (str "https://zenmarket.jp/en/yahoo.aspx?sellerID=" seller)})))}
       seller]
      [:img {:class          "flex-1 cursor-pointer object-scale-down"
             :src            img
             :style          {:max-height "180px"
                              :width      :auto}
             :on-mouse-enter #(mx/dispatch ::evt/parse-auction-page {:item-id id})}]
      [:div {:class "flex flex-none justify-between px-4"}
       [:div {:class    "cursor-pointer"
              :on-click (fn []
                          (mx/dispatch ::evt/open-browser {:url (enc/format "https://zenmarket.jp/en/auction.aspx?itemCode=%s" id)}))}
        price]
       [:div {:on-mouse-enter (fn [_e]
                                (mx/dispatch ::evt/hover-tile {:mode :enter :img img}))}
        [:> Eye {}]]
       [:div {:class    ["cursor-pointer" (when favourite? "text-nord-11")]
              :on-click (fn [^js e]
                          (.stopPropagation e)
                          (mx/dispatch ::evt/toggle-favourite {:id id :query query :img img :price price :favourite? favourite?}))}
        [:> Heart {}]]]]]))

(mf/defc table-row [{:keys [query row]}]
  (let [hovered-tile @(mx/subscribe ::sub/hovered-tile)]
    [:div {:class ["flex h-full w-full gap-2 transition duration-150 ease-in-out"]}
     (for [{:keys [id img price favourite?] :as cell} row]
       [:& table-cell (assoc cell :query query)])]
    ))

(mf/defc table [{:keys [query]}]
  (tap> [:render :table :query query])
  (let [data        @(mx/subscribe ::sub/query-data {:query query})
        partitioned (partition-all 6 data)]
    (println :rerender :table)
    [:div {:class "flex flex-col flex-1"}
     [:div {:class ["flex-1 pt-4 px-4 pb-1 border-b border-nord-3"]}
      [:> Virtuoso
       #js {:totalCount  (count partitioned)
            :itemContent (fn [i]
                           (mf/html [:& table-row {:query query :row (nth partitioned i)}]))}]]]))

(mf/defc main-view []
  (let [query @(mx/subscribe ::sub/selected-query)
        cb    (fn [^js x]
                (when (= "Escape" (.-key x))
                  (mx/dispatch ::evt/hover-tile {:mode :leave :img nil})))]
    (println :rerender :main-view)
    (.addEventListener js/document "keydown" cb)
    [:div {:class "flex flex-col h-screen min-h-screen bg-nord-4 text-nord-1"}
     [:div {:class "flex flex-1 overflow-y-hidden overflow-x-hidden"}
      [:& sidebar {}
       [:& query-sidebar]]
      [:div {:class "flex flex-1 flex-col"}
       [:& table {:query query}]
       [:div {:class "flex justify-center my-4 cursor-pointer"}
        [:> ArrowDown
         {:on-click #(mx/dispatch ::evt/fetch-query {:query query})}]]]]]))

(mf/defc view []
  (let [boot-successful? @(mx/subscribe ::init/boot-successful?)
        show-spinner?    @(mx/subscribe ::ui.sub/show-spinner?)
        hovered-tile     @(mx/subscribe ::sub/hovered-tile)]
    (println :rerender :view)
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
