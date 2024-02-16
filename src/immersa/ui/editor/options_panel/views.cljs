(ns immersa.ui.editor.options-panel.views
  (:require
    ["react-color" :refer [SketchPicker]]
    [applied-science.js-interop :as j]
    [immersa.ui.editor.components.button :refer [button shortcut-button]]
    [immersa.ui.editor.components.input :refer [input-number]]
    [immersa.ui.editor.components.scroll-area :refer [scroll-area]]
    [immersa.ui.editor.components.separator :refer [separator]]
    [immersa.ui.editor.components.slider :refer [slider]]
    [immersa.ui.editor.components.switch :refer [switch]]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.editor.components.textarea :refer [textarea]]
    [immersa.ui.editor.components.tooltip :refer [tooltip tooltip-text]]
    [immersa.ui.editor.events :as events]
    [immersa.ui.editor.options-panel.styles :as styles]
    [immersa.ui.editor.subs :as subs]
    [immersa.ui.icons :as icon]
    [immersa.ui.theme.colors :as colors]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(def ^:private color-picker* (r/adapt-react-class SketchPicker))

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

(defn pos-rot-scale-comp [{:keys [label type value event disabled?]}]
  (let [[x y z] value]
    [:div (styles/pos-rot-scale-comp-container disabled?)
     [text {:class (styles/pos-rot-scale-comp-label)} label]
     [input-number {:label "X"
                    :value x
                    :disabled? disabled?
                    :on-change #(dispatch [event type 0 %])}]
     [input-number {:label "Y"
                    :value y
                    :disabled? disabled?
                    :on-change #(dispatch [event type 1 %])}]
     [input-number {:label "Z"
                    :value z
                    :disabled? disabled?
                    :on-change #(dispatch [event type 2 %])}]]))

(defn- position [& {:keys [disabled?]}]
  [pos-rot-scale-comp {:label "Position"
                       :disabled? disabled?
                       :type :position
                       :event ::events/update-selected-mesh
                       :value @(subscribe [::subs/selected-mesh-position])}])

(defn- rotation [& {:keys [disabled?]}]
  [pos-rot-scale-comp {:label "Rotation"
                       :disabled? disabled?
                       :type :rotation
                       :event ::events/update-selected-mesh
                       :value @(subscribe [::subs/selected-mesh-rotation])}])

(defn- scale [& {:keys [disabled?]}]
  [pos-rot-scale-comp {:label "Scale"
                       :disabled? disabled?
                       :type :scaling
                       :event ::events/update-selected-mesh
                       :value @(subscribe [::subs/selected-mesh-scaling])}])

(defn- arrow-helper-position []
  (let [checked? @(subscribe [::subs/gizmo-visible? :position])
        trigger [switch {:checked? checked?
                         :on-change #(dispatch [::events/update-gizmo-visibility :position])}]]
    [:div
     {:style {:display "flex"
              :align-items "center"
              :gap "5px"}}
     [text "Position"]
     [tooltip
      {:trigger trigger
       :content (if checked?
                  "Hide position helper"
                  "Show position helper")
       :shortcuts "1"}]]))

(defn- arrow-helper-rotation []
  (let [checked? @(subscribe [::subs/gizmo-visible? :rotation])
        disabled? @(subscribe [::subs/selected-mesh-face-to-screen?])
        trigger [switch {:checked? checked?
                         :on-change #(dispatch [::events/update-gizmo-visibility :rotation])
                         :disabled? disabled?}]]
    [:div
     {:style {:display "flex"
              :align-items "center"
              :gap "5px"}}
     [text {:disabled? disabled?} "Rotation"]
     (if disabled?
       [tooltip
        {:trigger trigger
         :content "Disable face to screen to use rotation helper."}]
       [tooltip
        {:trigger trigger
         :content (if checked?
                    "Hide rotation helper"
                    "Show rotation helper")
         :shortcuts "2"}])]))

(defn- arrow-helper-scale []
  (let [checked? @(subscribe [::subs/gizmo-visible? :scale])
        trigger [switch {:checked? checked?
                         :on-change #(dispatch [::events/update-gizmo-visibility :scale])}]]
    [:div
     {:style {:display "flex"
              :align-items "center"
              :gap "5px"}}
     [text "Scale"]
     [tooltip
      {:trigger trigger
       :content (if checked?
                  "Hide scale helper"
                  "Show scale helper")
       :shortcuts "3"}]]))

(defn- arrow-helpers [type]
  [:div {:style {:display "flex"
                 :flex-direction "column"
                 :gap "15px"}}

   [:div {:style {:display "flex"
                  :flex-direction "row"
                  :align-items "center"
                  :gap "2px"}}
    [text "Arrow helpers"]
    [tooltip
     {:trigger [icon/info {:size 12
                           :weight :fill
                           :color colors/button-bg}]
      :content (str "Arrow helpers are used to update the " type " position, rotation, and scale visually.")}]]

   [:div
    {:style {:display "flex"
             :flex-direction "rows"
             :align-items "center"
             :justify-content "flex-start"
             :gap "16px"}}
    [arrow-helper-position]
    [arrow-helper-rotation]
    (when-not (= type "text")
      [arrow-helper-scale])]])

(defn- text-content []
  [:div {:style {:display "flex"
                 :justify-content "space-between"}}
   [:div {:style {:display "flex"
                  :flex-direction "column"
                  :gap "12px"}}
    [text "Content"]
    [textarea {:value @(subscribe [::subs/selected-mesh-text-content])
               :on-change #(dispatch [::events/update-selected-mesh-text-content
                                      (-> % .-target .-value)])}]]])

(defn- size-and-depth []
  [:div {:style {:display "flex"
                 :justify-content "space-between"}}
   [:div {:style {:display "flex"
                  :flex-direction "row"
                  :align-items "center"
                  :justify-content "center"
                  :gap "16px"}}
    [text "Size"]
    [input-number {:max "100"
                   :step "0.01"
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
                   :on-change #(dispatch [::events/update-selected-mesh-text-depth-or-size :depth %])}]]])

(defn- opacity []
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
            :on-change #(dispatch [::events/update-selected-mesh-slider-value :opacity %])}]])

(defn- material-options []
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
   [opacity]
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
             :on-change #(dispatch [::events/update-selected-mesh-slider-value :metallic %])}]]])

(defn- face-to-screen [type]
  [:div {:style {:display "flex"
                 :flex-direction "row"
                 :justify-content "space-between"
                 :gap "8px"}}
   [:div {:style {:display "flex"
                  :flex-direction "row"
                  :align-items "center"
                  :gap "2px"}}
    [text "Face to screen"]
    [tooltip
     {:trigger [icon/info {:size 12
                           :weight :fill
                           :color colors/button-bg}]
      :content (str "Enabling face to screen will make the " type " always face the screen.")}]]
   [switch {:checked? @(subscribe [::subs/selected-mesh-face-to-screen?])
            :on-change #(dispatch [::events/update-selected-mesh-face-to-screen?])}]])

(defn- text-3d-options []
  (let [type "text"]
    [:div {:style {:display "flex"
                   :flex-direction "column"
                   :gap "12px"
                   :padding "22px"
                   :position "relative"}}
     [text {:size :xxl
            :weight :semi-bold} "Text"]
     [separator]
     [position]
     [rotation {:disabled? @(subscribe [::subs/selected-mesh-face-to-screen?])}]
     [separator]
     [arrow-helpers type]
     [separator]
     [text-content]
     [separator]
     [size-and-depth]
     [separator]
     [material-options]
     [separator]
     [face-to-screen type]]))

(defn- image-options []
  (let [type "image"]
    [:div {:style {:display "flex"
                   :flex-direction "column"
                   :gap "12px"
                   :padding "22px"
                   :position "relative"}}
     [text {:size :xxl
            :weight :semi-bold} "Image"]
     [separator]
     [position]
     [rotation]
     [scale]
     [separator]
     [arrow-helpers type]
     [separator]
     [opacity]
     [separator]
     [face-to-screen type]]))

(defn- glb-options []
  [:div {:style {:display "flex"
                 :flex-direction "column"
                 :gap "12px"
                 :padding "22px"
                 :position "relative"}}
   [text {:size :xxl
          :weight :semi-bold} "3D Model"]
   [separator]
   [position]
   [rotation]
   [scale]
   [separator]
   [arrow-helpers "model"]])

(defn- selected-mesh-options []
  (case @(subscribe [::subs/selected-mesh-type])
    "text3D" [text-3d-options]
    "image" [image-options]
    "glb" [glb-options]))

(defn- camera-options []
  [:div {:style {:display "flex"
                 :flex-direction "column"
                 :gap "12px"
                 :padding "22px"}}
   [text {:size :xxl
          :weight :semi-bold} "Camera"]
   [separator]
   [pos-rot-scale-comp {:label "Position"
                        :type :position
                        :event ::events/update-camera
                        :disabled? @(subscribe [::subs/camera-locked?])
                        :value @(subscribe [::subs/camera-position])}]
   [pos-rot-scale-comp {:label "Rotation"
                        :type :rotation
                        :event ::events/update-camera
                        :disabled? @(subscribe [::subs/camera-locked?])
                        :value @(subscribe [::subs/camera-rotation])}]
   [:div
    {:style {:display "flex"
             :align-items "center"
             :justify-content "space-between"
             :gap "5px"}}
    [:div {:style {:display "flex"
                   :flex-direction "row"
                   :align-items "center"
                   :gap "2px"}}
     [text "Locked"]
     [tooltip
      {:trigger [icon/info {:size 12
                            :weight :fill
                            :color colors/button-bg}]
       :content (if @(subscribe [::subs/camera-locked?])
                  [:<>
                   [tooltip-text {:text "The view displayed on the canvas is from the camera's perspective."}]
                   [tooltip-text {:text "Locking the camera disables movement control."}]
                   [tooltip-text {:text "Recommended option for beginners."
                                  :weight :regular}]]
                  [:<>
                   [tooltip-text {:text "The view displayed on the canvas is from the camera's perspective."}]
                   [tooltip-text {:text "Unlocking the camera enables movement control."}]
                   [tooltip-text {:text "Recommended for advanced users."
                                  :weight :regular}]])}]]
    [switch {:checked? @(subscribe [::subs/camera-locked?])
             :on-change #(dispatch [::events/toggle-camera-lock])}]]])

(defn options-panel []
  [:div (styles/options-panel)
   [:div
    {:style {:display "flex"
             :justify-content "center"
             :padding-top "8px"}}
    [scroll-area
     {:class (styles/options-panel-scroll-area)
      :children (if @(subscribe [::subs/selected-mesh])
                  [selected-mesh-options]
                  [:div {:style {:user-select "none"}}
                   [:div {:style {:display "flex"
                                  :flex-direction "column"
                                  :gap "12px"
                                  :padding "22px"}}
                    [text {:size :xxl
                           :weight :semi-bold} "Scene"]
                    [separator]
                    [color-picker {:text "Background color"
                                   :sub-key ::subs/scene-background-color
                                   :event-key ::events/update-scene-background-color}]]
                   [camera-options]])}]]])
