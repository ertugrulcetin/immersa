(ns immersa.ui.editor.views
  (:require
    ["react-color" :refer [SketchPicker]]
    [applied-science.js-interop :as j]
    [goog.functions :as functions]
    [immersa.scene.core :as scene.core]
    [immersa.ui.editor.components.button :refer [button]]
    [immersa.ui.editor.components.dropdown :refer [dropdown dropdown-item dropdown-separator]]
    [immersa.ui.editor.components.input :refer [input-number]]
    [immersa.ui.editor.components.scroll-area :refer [scroll-area]]
    [immersa.ui.editor.components.separator :refer [separator]]
    [immersa.ui.editor.components.slider :refer [slider]]
    [immersa.ui.editor.components.switch :refer [switch]]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.editor.components.textarea :refer [textarea]]
    [immersa.ui.editor.components.tooltip :refer [tooltip]]
    [immersa.ui.editor.events :as events]
    [immersa.ui.editor.options-panel.views :refer [options-panel]]
    [immersa.ui.editor.styles :as styles]
    [immersa.ui.editor.subs :as subs]
    [immersa.ui.icons :as icon]
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(defn- canvas [state]
  (r/create-class
    {:component-did-mount #(scene.core/start-scene (js/document.getElementById "renderCanvas")
                                                   {:mode :editor
                                                    :slides @(subscribe [::subs/slides-all])
                                                    :thumbnails @(subscribe [::subs/slides-thumbnails])})
     :reagent-render (fn []
                       [:canvas
                        {:id "renderCanvas"
                         :on-blur #(reset! state :blur)
                         :on-focus #(reset! state :focus)
                         :class (styles/canvas)}])}))

(defn- canvas-container []
  (let [state (r/atom :blur)]
    (fn []
      (let [{:keys [width height]} @(subscribe [::subs/calculated-canvas-wrapper-dimensions])]
        (when (and width height (> width 0) (> height 0))
          [:div
           {:id "canvas-container"
            :style {:width (str width "px")
                    :height (str height "px")}
            :class (styles/canvas-container @state)}
           [canvas state]])))))

(defn- canvas-wrapper []
  (let [ref (r/atom nil)
        ob (atom nil)
        on-resize (functions/debounce
                    (fn [entries]
                      (doseq [e entries]
                        (dispatch [::events/set-canvas-wrapper-dimensions
                                   (j/get-in e [:contentRect :width])
                                   (j/get-in e [:contentRect :height])]))
                      (js/setTimeout #(dispatch [::events/resize-scene]) 200))
                    200)]
    (r/create-class
      {:component-did-mount (fn []
                              (println "canvas-wrapper did mount")
                              (when @ref
                                (let [observer (js/ResizeObserver. on-resize)]
                                  (reset! ob observer)
                                  (j/call observer :observe @ref))))
       :component-will-unmount #(some-> @ob (j/call :disconnect))
       :reagent-render (fn []
                         [:div
                          {:id "canvas-wrapper"
                           :ref #(reset! ref %)
                           :class (styles/canvas-wrapper)}
                          (when @ref
                            [canvas-container])])})))

(defn- header-left-panel []
  [:div (styles/title-bar)
   [:div (styles/menubar-list-icon)
    [icon/list-menu {:size 24
                     :color colors/text-primary}]]
   [:div (styles/title-bar-full-width)
    [:div (styles/title-container)
     [:span (styles/title-label) "My 3D Presentation"]
     [:div (styles/private-badge)
      [icon/lock {:size 12}]
      [tooltip
       {:trigger [text {:size :s
                        :weight :light} "Beta"]
        :content "Immersa is currently in beta"}]]]]])

(defn presentation-component [{:keys [icon class disabled? color text-weight on-click]
                               :or {disabled? false}
                               :as opts}]
  [:div {:class [(styles/presentation-component disabled?) class]
         :on-click on-click
         :disabled disabled?}
   [icon {:size 24
          :color (or color colors/text-primary)}]
   [text {:size :s
          :color (or color colors/text-primary)
          :weight (or text-weight :light)} (:text opts)]])

(defn- header-center-panel []
  [:div (styles/header-center-panel)

   [tooltip
    {:trigger [presentation-component {:icon icon/text
                                       :text "Text"
                                       :on-click #(dispatch [::events/add-text-mesh])}]
     :content "Add text"
     :shortcuts "T"}]
   #_[dropdown
      [presentation-component {:icon icon/text
                               :text "Text"}]
      [:<>
       [dropdown-item
        [text {:size :xl} "Normal text"]]
       [dropdown-separator]
       [dropdown-item
        [text {:size :xl} "Greased line text"]]
       [dropdown-separator]
       [dropdown-item
        [text {:size :xl} "Info text"]]]]
   [presentation-component {:icon icon/image
                            :text "Image"
                            :disabled? true}]
   [presentation-component {:icon icon/cube
                            :text "3D Model"
                            :class (styles/presentation-component-cube)
                            :disabled? true}]
   [presentation-component {:icon icon/camera
                            :text "Camera"
                            :disabled? true}]
   [presentation-component {:icon icon/student
                            :text "Tutorial"
                            :disabled? true}]
   [presentation-component {:icon icon/chats-circle
                            :text "Feedback"
                            :text-weight :regular
                            :class (styles/presentation-component-cube)
                            :color colors/button-outline-text
                            :disabled? true}]
   #_[presentation-component {:icon icon/books
                              :text "Library"
                              :disabled? true}]
   #_[presentation-component {:icon icon/light
                              :text "Light"
                              :disabled? true}]])

(defn- header-right-panel []
  [:div (styles/header-right)
   [:div (styles/header-right-container)
    [button {:text "Present"
             :type :outline
             :class (styles/present-share-width)
             :icon-left [icon/play {:size 18
                                    :weight "fill"
                                    :color colors/button-outline-text}]}]
    [button {:text "Share"
             :type :regular
             :class (styles/present-share-width)
             :icon-right [icon/share {:size 18
                                      :weight "fill"
                                      :color colors/button-text}]}]]])

(defn- header []
  [:div (styles/header-container)
   [header-left-panel]
   [header-center-panel]
   [header-right-panel]])

(defn- slide [index]
  (let [current-index @(subscribe [::subs/slides-current-index])
        thumbnail @(subscribe [::subs/slide-thumbnail index])
        selected? (= index current-index)]
    [:div {:style {:display "flex"
                   :align-items "flex-start"
                   :padding-left "8px"
                   :padding-bottom "8px"
                   :user-select "none"}
           :on-click #(dispatch [::events/go-to-slide index])}
     [:span {:style {:width "22px"
                     :color (if selected?
                              colors/button-outline-border
                              colors/text-primary)
                     :font-size typography/s
                     :font-weight (if selected?
                                    typography/medium
                                    typography/regular)}} (inc index)]
     [:div
      {:style {:width "123px"
               :height "70px"
               :border-radius "5px"
               :border (if selected?
                         (str "2px solid " colors/button-outline-border)
                         (str "2px solid " colors/border2))}}
      [:img {:src thumbnail
             :style {:width "100%"
                     :height "100%"
                     :box-sizing "border-box"
                     :border "2px solid transparent"
                     :border-radius "3px"}}]]]))

(defn editor-panel []
  [:div (styles/editor-container)
   [header]
   [:div (styles/content-container)
    [:div (styles/side-bar)
     [:div
      {:style {:display "flex"
               :align-items "center"
               :padding "8px 16px 0 16px"}}
      [tooltip
       {:trigger [button {:text "Add slide"
                          :on-click #(dispatch [::events/add-slide])
                          :class (styles/add-slide-button)
                          :icon-left [icon/plus {:size 18
                                                 :color colors/text-primary}]}]
        :content "Add a new slide"
        :shortcuts "N"}]]

     [scroll-area
      {:class (styles/slides-scroll-area)
       :children [:div {:tabIndex "0"
                        :on-key-down (fn [e]
                                       (when-not (j/get e :repeat)
                                         (when (or (= "ArrowDown" (j/get e :code))
                                                   (= "ArrowRight" (j/get e :code)))
                                           (dispatch [::events/go-to-slide :next]))
                                         (when (or (= "ArrowUp" (j/get e :code))
                                                   (= "ArrowLeft" (j/get e :code)))
                                           (dispatch [::events/go-to-slide :prev]))))
                        :style {:padding-top "8px"
                                :outline "none"}}
                  (for [{:keys [id index]} (map-indexed #(assoc %2 :index %1) @(subscribe [::subs/slides-all]))]
                    ^{:key id}
                    [slide index])]}]]
    [canvas-wrapper]
    [options-panel]]])

(comment
  @(subscribe [::subs/selected-mesh]))
