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
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.editor.events :as events]
    [immersa.ui.editor.styles :as styles]
    [immersa.ui.editor.subs :as subs]
    [immersa.ui.icons :as icon]
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(def ^:private color-picker* (r/adapt-react-class SketchPicker))

(defn- canvas [state]
  (r/create-class
    {:component-did-mount #(scene.core/start-scene (js/document.getElementById "renderCanvas") :mode :editor)
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
    [icon/list {:size 24
                :color colors/text-primary}]]
   [:div (styles/title-bar-full-width)
    [:div (styles/title-container)
     [:span (styles/title-label) "My 3D Presentation"]
     [:div (styles/private-badge)
      [icon/lock {:size 12}]
      [text {:size :s
             :weight :light} "Private"]]]]])

(defn presentation-component [{:keys [icon class disabled?]
                               :or {disabled? false}
                               :as opts}]
  [:div {:class [(styles/presentation-component disabled?) class]
         :disabled disabled?}
   [icon {:size 24
          :color colors/text-primary}]
   [text {:size :s
          :weight :light} (:text opts)]])

(defn- header-center-panel []
  [:div (styles/header-center-panel)
   [dropdown
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
                            :text "Image"}]
   [presentation-component {:icon icon/cube
                            :text "3D Model"
                            :class (styles/presentation-component-cube)}]
   [presentation-component {:icon icon/books
                            :text "Library"
                            :disabled? true}]
   [presentation-component {:icon icon/light
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

(defn pos-rot-scale-comp [{:keys [label type value event]}]
  (let [[x y z] value]
    [:div (styles/pos-rot-scale-comp-container)
     [text {:class (styles/pos-rot-scale-comp-label)} label]
     [input-number {:label "X"
                    :value x
                    :on-change #(dispatch [event type 0 %])}]
     [input-number {:label "Y"
                    :value y
                    :on-change #(dispatch [event type 1 %])}]
     [input-number {:label "Z"
                    :value z
                    :on-change #(dispatch [event type 2 %])}]]))

(defn color-picker []
  (r/with-let [open? (r/atom false)]
    [:div (styles/color-picker-container)
     [:div (styles/color-picker-button-container)
      [text "Background color"]
      [:button {:class (styles/color-picker-button)
                :style {:background @(subscribe [::subs/scene-background-color])}
                :on-click #(swap! open? not)}]]
     (when @open?
       [:div (styles/color-picker-component-container)
        [button {:class (styles/color-picker-close-button)
                 :on-click #(reset! open? false)
                 :icon-right [icon/x {:size 12
                                      :weight "bold"
                                      :color colors/text-primary}]}]
        [:div (styles/color-picker-component-wrapper)
         [color-picker* {:disable-alpha true
                         :color @(subscribe [::subs/scene-background-color])
                         :on-change #(let [{:keys [r g b]} (j/lookup (j/get % :rgb))]
                                       (dispatch [::events/update-scene-background-color [r g b]]))}]]])]))

(defn editor-panel []
  [:div (styles/editor-container)
   [header]
   [:div (styles/content-container)
    [:div (styles/side-bar)
     [:div
      {:style {:display "flex"
               :align-items "center"
               :padding "8px 16px 0 16px"}}
      [button {:text "Add slide"
               :class (styles/add-slide-button)
               :icon-left [icon/plus {:size 18
                                      :color colors/text-primary}]}]]

     [scroll-area
      {:class (styles/slides-scroll-area)
       :children [:div {:style {:padding-top "8px"}}
                  (for [i (range 1 15)]
                    ^{:key i}
                    [:div {:style {:display "flex"
                                   :align-items "flex-start"
                                   :padding-left "8px"
                                   :padding-bottom "8px"}}
                     [:span {:style {:width "22px"
                                     :color (if (= i 1)
                                              colors/button-outline-border
                                              colors/text-primary)
                                     :font-size typography/s
                                     :font-weight (if (= i 1)
                                                    typography/medium
                                                    typography/regular)}} i]
                     [:div
                      {:style {:width "123px"
                               :height "70px"
                               :border-radius "5px"
                               :border (if (= i 1)
                                         (str "2px solid " colors/button-outline-border)
                                         (str "1px solid " colors/border2))}}]])]}]]
    [canvas-wrapper]
    [:div (styles/options-bar)
     [:div
      {:style {:display "flex"
               :justify-content "center"
               :padding-top "8px"}}
      (if @(subscribe [::subs/selected-mesh])
        [:div {:style {:display "flex"
                       :flex-direction "column"
                       :gap "12px"
                       :padding "16px"}}
         [text {:size :xxl
                :weight :semi-bold} "3D Model"]
         [separator]
         [pos-rot-scale-comp {:label "Position"
                              :type :position
                              :event ::events/update-selected-mesh
                              :value @(subscribe [::subs/selected-mesh-position])}]
         [pos-rot-scale-comp {:label "Rotation"
                              :type :rotation
                              :event ::events/update-selected-mesh
                              :value @(subscribe [::subs/selected-mesh-rotation])}]
         [pos-rot-scale-comp {:label "Scale"
                              :type :scaling
                              :event ::events/update-selected-mesh
                              :value @(subscribe [::subs/selected-mesh-scaling])}]]
        [:div {:style {:user-select "none"}}
         [:div {:style {:display "flex"
                        :flex-direction "column"
                        :gap "12px"
                        :padding "16px"}}
          [text {:size :xxl
                 :weight :semi-bold} "Scene"]
          [separator]
          [color-picker]]
         [:div {:style {:display "flex"
                        :flex-direction "column"
                        :gap "12px"
                        :padding "16px"}}
          [text {:size :xxl
                 :weight :semi-bold} "Camera"]
          [separator]
          [pos-rot-scale-comp {:label "Position"
                               :type :position
                               :event ::events/update-camera
                               :value @(subscribe [::subs/camera-position])}]
          [pos-rot-scale-comp {:label "Rotation"
                               :type :rotation
                               :event ::events/update-camera
                               :value @(subscribe [::subs/camera-rotation])}]]])]]]])
