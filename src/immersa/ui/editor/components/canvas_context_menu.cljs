(ns immersa.ui.editor.components.canvas-context-menu
  (:require
    [immersa.common.shortcut :as shortcut]
    [immersa.ui.editor.components.button :refer [shortcut-button]]
    [immersa.ui.editor.components.dropdown :refer [dropdown
                                                   dropdown-item
                                                   dropdown-separator
                                                   dropdown-context-menu]]
    [immersa.ui.editor.components.scroll-area :refer [scroll-area]]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.editor.events :as events]
    [immersa.ui.editor.subs :as subs]
    [re-frame.core :refer [dispatch subscribe]]
    [spade.core :refer [defclass defattrs]]))

(def context-menu-width "220px")
(def context-menu-height "220px")

(defclass context-menu-scroll-area []
  {:width context-menu-width
   :height context-menu-height
   :overflow :hidden}
  ;; Fade out effect
  [:&:before
   {:content "''"
    :position "fixed"
    :width context-menu-width
    :height "8px"
    :background "linear-gradient(180deg,#ffffff 0%,rgba(252,252,253,0) 100%)"}])

(defclass option-text-style [disabled?]
  {:display :flex
   :flex-direction :row
   :align-items :center
   :justify-content :space-between
   :width "100%"}
  (when disabled?
    {:cursor :not-allowed}))

(defn- option-text [{:keys [label shortcut disabled?]}]
  [:div {:class (option-text-style disabled?)}
   [text {:weight :light
          :size :xs
          :disabled? disabled?} label]
   (when (seq shortcut)
     [:div
      {:style {:display "flex"
               :gap "4px"}}
      (for [s shortcut]
        ^{:key s}
        [shortcut-button s])])])

(defn- main-options []
  [:<>
   [dropdown-item
    {:item [option-text {:label "Add new slide"
                         :shortcut (shortcut/get-shortcut-key-labels :add-slide)}]
     :on-select #(shortcut/call-shortcut-action :add-slide)}]
   [dropdown-item
    {:item [option-text {:label "Delete slide"
                         :shortcut (shortcut/get-shortcut-key-labels :delete-slide)}]
     :on-select #(shortcut/call-shortcut-action :delete-slide)}]
   [dropdown-item
    {:item [option-text {:label "Paste"
                         :shortcut (shortcut/get-shortcut-key-labels :paste)}]
     :on-select #(shortcut/call-shortcut-action :paste)}]])

(defn- selected-object-options []
  [:<>
   [dropdown-item
    {:item [option-text {:label "Focus"
                         :shortcut (shortcut/get-shortcut-key-labels :focus)}]
     :on-select #(shortcut/call-shortcut-action :focus)}]
   [dropdown-item
    {:item [option-text {:label "Reset to initials"
                         :shortcut (shortcut/get-shortcut-key-labels :reset-initials)}]
     :on-select #(shortcut/call-shortcut-action :reset-initials)
     :disabled? (not @(subscribe [::subs/selected-mesh-initial-position?]))}]
   [dropdown-item
    {:item [option-text {:label "Duplicate"
                         :shortcut (shortcut/get-shortcut-key-labels :duplicate)}]
     :on-select #(shortcut/call-shortcut-action :duplicate)}]
   [dropdown-item
    {:item [option-text {:label "Copy"
                         :shortcut (shortcut/get-shortcut-key-labels :copy)}]
     :on-select #(shortcut/call-shortcut-action :copy)}]
   [dropdown-item
    {:item [option-text {:label "Paste"
                         :shortcut (shortcut/get-shortcut-key-labels :paste)}]
     :on-select #(shortcut/call-shortcut-action :paste)}]
   [dropdown-item
    {:item [option-text {:label "Delete"
                         :shortcut (shortcut/get-shortcut-key-labels :delete)}]
     :on-select #(shortcut/call-shortcut-action :delete)}]
   [dropdown-item
    {:item [option-text {:label "Reset position"
                         :shortcut (shortcut/get-shortcut-key-labels :reset-position)}]
     :on-select #(shortcut/call-shortcut-action :reset-position)
     :disabled? (not @(subscribe [::subs/selected-mesh-initial-position?]))}]
   [dropdown-item
    {:item [option-text {:label "Reset rotation"
                         :shortcut (shortcut/get-shortcut-key-labels :reset-rotation)}]
     :on-select #(shortcut/call-shortcut-action :reset-rotation)
     :disabled? (not @(subscribe [::subs/selected-mesh-initial-rotation?]))}]
   (when-not (= "text3D" @(subscribe [::subs/selected-mesh-type]))
     [dropdown-item
      {:item [option-text {:label "Reset scale"
                           :shortcut (shortcut/get-shortcut-key-labels :reset-scale)}]
       :on-select #(shortcut/call-shortcut-action :reset-scale)
       :disabled? (not @(subscribe [::subs/selected-mesh-initial-scale?]))}])])

(defn- camera-options []
  (let [camera-locked? @(subscribe [::subs/camera-locked?])
        lock-text (if camera-locked? "Unlock" "Lock")]
    [:<>
     [dropdown-item
      {:item [option-text {:label "Reset camera to initials"
                           :shortcut (shortcut/get-shortcut-key-labels :camera-reset-to-initials)}]
       :on-select #(shortcut/call-shortcut-action :camera-reset-to-initials)}]
     [dropdown-item
      {:item [option-text {:label (str lock-text " camera")
                           :shortcut (shortcut/get-shortcut-key-labels :toggle-camera-lock)}]
       :on-select #(shortcut/call-shortcut-action :toggle-camera-lock)}]]))

(defn canvas-context-menu []
  (let [[x y] @(subscribe [::subs/context-menu-position])]
    (when x
      [dropdown-context-menu
       {:on-open-change (fn [open?]
                          (when-not open?
                            (dispatch [::events/clear-context-menu-position])
                            (some-> (js/document.getElementById "renderCanvas") .focus)))
        :trigger [:div
                  {:id "context-menu-trigger"
                   :style {:position "absolute"
                           :z-index 9999
                           :top (str (- y 55) "px")
                           :left (str (+ x 115) "px")}}]
        :children [scroll-area
                   {:class (context-menu-scroll-area)
                    :children
                    [:<>
                     (if @(subscribe [::subs/selected-mesh])
                       [selected-object-options]
                       [main-options])
                     [dropdown-separator]
                     [camera-options]]}]}])))
