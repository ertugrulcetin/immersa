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
      [text {:size :s
             :weight :light} "Beta"]]]]])

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

   [presentation-component {:icon icon/text
                            :text "Text"
                            :on-click #(dispatch [::events/add-text-mesh])}]
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

(defn color-picker [_]
  (let [open? (r/atom false)]
    (fn [{:keys [sub-key event-key] :as opts}]
      [:div (styles/color-picker-container)
       [:div (styles/color-picker-button-container)
        [text (:text opts)]
        [:button {:class (styles/color-picker-button)
                  :style {:background @(subscribe [sub-key])}
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
                           :color @(subscribe [sub-key])
                           :on-change #(let [{:keys [r g b]} (j/lookup (j/get % :rgb))]
                                         (dispatch [event-key [r g b]]))}]]])])))

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

(defn- get-selected-mesh-type-name []
  (case @(subscribe [::subs/selected-mesh-type])
    "text3D" "Text"
    "glb" "3D Model"
    "image" "Image"))

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
               :on-click #(dispatch [::events/add-slide])
               :class (styles/add-slide-button)
               :icon-left [icon/plus {:size 18
                                      :color colors/text-primary}]}]]

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
    [:div (styles/options-bar)
     [:div
      {:style {:display "flex"
               :justify-content "center"
               :padding-top "8px"}}
      [scroll-area
       {:class (styles/options-scroll-area)
        :children (if @(subscribe [::subs/selected-mesh])
                    [:div {:style {:display "flex"
                                   :flex-direction "column"
                                   :gap "12px"
                                   :padding "22px"
                                   :position "relative"}}
                     [text {:size :xxl
                            :weight :semi-bold} (get-selected-mesh-type-name)]
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
                                          :value @(subscribe [::subs/selected-mesh-scaling])}]
                     [separator]
                     [:div {:style {:display "flex"
                                    :flex-direction "column"
                                    ;; :align-items "center"
                                    ;; :justify-content "center"
                                    :gap "15px"}}
                      [text "Arrow helpers"]
                      [:div
                       {:style {:display "flex"
                                :flex-direction "rows"
                                :align-items "center"
                                :justify-content "space-between"}}
                       [:div
                        {:style {:display "flex"
                                 :align-items "center"
                                 :gap "5px"}}
                        [text "Position"]
                        [switch {:checked? @(subscribe [::subs/gizmo-visible? :position])
                                 :on-change #(dispatch [::events/update-gizmo-visibility :position])}]]
                       [:div
                        {:style {:display "flex"
                                 :align-items "center"
                                 :gap "5px"}}
                        [text "Rotation"]
                        [switch {:checked? @(subscribe [::subs/gizmo-visible? :rotation])
                                 :on-change #(dispatch [::events/update-gizmo-visibility :rotation])}]]
                       [:div
                        {:style {:display "flex"
                                 :align-items "center"
                                 :gap "5px"}}
                        [text "Scale"]
                        [switch {:checked? @(subscribe [::subs/gizmo-visible? :scale])
                                 :on-change #(dispatch [::events/update-gizmo-visibility :scale])}]]]]
                     [separator]
                     [:div {:style {:display "flex"
                                    :justify-content "space-between"}}
                      [:div {:style {:display "flex"
                                     :flex-direction "column"
                                     ;; :align-items "center"
                                     ;; :justify-content "center"
                                     :gap "12px"}}
                       [text "Content"]
                       [textarea {:value @(subscribe [::subs/selected-mesh-text-content])
                                  :on-change #(dispatch [::events/update-selected-mesh-text-content
                                                         (-> % .-target .-value)])}]]]
                     [separator]
                     [:div {:style {:display "flex"
                                    :justify-content "space-between"}}
                      [:div {:style {:display "flex"
                                     :flex-direction "row"
                                     :align-items "center"
                                     :justify-content "center"
                                     :gap "16px"}}
                       [text "Size"]
                       [input-number {:max "100"
                                      :value @(subscribe [::subs/selected-mesh-text-size])
                                      :on-change #(dispatch [::events/update-selected-mesh-text-depth-or-size :size %])}]]
                      [separator {:orientation "vertical"
                                  :style {:height "25px"}}]
                      [:div {:style {:display "flex"
                                     :flex-direction "row"
                                     :align-items "center"
                                     :justify-content "center"
                                     :gap "16px"}}
                       [text "Depth"]
                       [input-number {:min "0.01"
                                      :max "100000"
                                      :value @(subscribe [::subs/selected-mesh-text-depth])
                                      :on-change #(dispatch [::events/update-selected-mesh-text-depth-or-size :depth %])}]]]
                     [separator]
                     [:div
                      {:style {:display "flex"
                               :flex-direction "column"
                               :gap "20px"}}
                      [color-picker {:text "Color"
                                     :sub-key ::subs/selected-mesh-color
                                     :event-key ::events/update-selected-mesh-main-color}]
                      [:div {:style {:display "flex"
                                     :flex-direction "column"
                                     :gap "8px"}}
                       [:div {:style {:display "flex"
                                      :flex-direction "row"
                                      :justify-content "space-between"}}
                        [text "Brightness"]
                        [text {:weight :light} (str @(subscribe [::subs/selected-mesh-emissive-intensity]) "%")]]
                       [slider {:value @(subscribe [::subs/selected-mesh-emissive-intensity])
                                :on-change #(dispatch [::events/update-selected-mesh-slider-value :emissive-intensity %])}]]
                      [:div {:style {:display "flex"
                                     :flex-direction "column"
                                     :gap "8px"}}
                       [:div {:style {:display "flex"
                                      :flex-direction "row"
                                      :justify-content "space-between"}}
                        [text "Opacity"]
                        [text {:weight :light} (str @(subscribe [::subs/selected-mesh-opacity]) "%")]]
                       [slider {:min 0.001
                                :value @(subscribe [::subs/selected-mesh-opacity])
                                :on-change #(dispatch [::events/update-selected-mesh-slider-value :opacity %])}]]
                      [:div {:style {:display "flex"
                                     :flex-direction "column"
                                     :gap "8px"}}
                       [:div {:style {:display "flex"
                                      :flex-direction "row"
                                      :justify-content "space-between"}}
                        [text "Roughness"]
                        [text {:weight :light} (str @(subscribe [::subs/selected-mesh-roughness]) "%")]]
                       [slider {:value @(subscribe [::subs/selected-mesh-roughness])
                                :on-change #(dispatch [::events/update-selected-mesh-slider-value :roughness %])}]]
                      [:div {:style {:display "flex"
                                     :flex-direction "column"
                                     :gap "8px"}}
                       [:div {:style {:display "flex"
                                      :flex-direction "row"
                                      :justify-content "space-between"}}
                        [text "Metalness"]
                        [text {:weight :light} (str @(subscribe [::subs/selected-mesh-metallic]) "%")]]
                       [slider {:value @(subscribe [::subs/selected-mesh-metallic])
                                :on-change #(dispatch [::events/update-selected-mesh-slider-value :metallic %])}]]]]
                    [:div {:style {:user-select "none"}}
                     [:div {:style {:display "flex"
                                    :flex-direction "column"
                                    :gap "12px"
                                    :padding "16px"}}
                      [text {:size :xxl
                             :weight :semi-bold} "Scene"]
                      [separator]
                      [color-picker {:text "Background color"
                                     :sub-key ::subs/scene-background-color
                                     :event-key ::events/update-scene-background-color}]]
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
                                           :value @(subscribe [::subs/camera-rotation])}]]])}]]]]])

(comment
  @(subscribe [::subs/selected-mesh]))
