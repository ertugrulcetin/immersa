(ns immersa.ui.editor.components.canvas-context-menu
  (:require
    [immersa.common.shortcut :as shortcut]
    [immersa.ui.editor.components.dropdown :refer [dropdown
                                                   dropdown-item
                                                   dropdown-separator
                                                   dropdown-context-menu
                                                   option-text]]
    [immersa.ui.editor.events :as events]
    [immersa.ui.editor.subs :as subs]
    [immersa.ui.icons :as icon]
    [immersa.ui.theme.colors :as colors]
    [re-frame.core :refer [dispatch subscribe]]))

(def gap "2px")

(defn- main-options []
  [:div {:style {:display "flex"
                 :flex-direction "column"
                 :gap gap}}
   [dropdown-item
    {:item [option-text {:label "Duplicate slide"
                         :icon [icon/copy {:size 16
                                           :color colors/text-primary}]
                         :shortcut (shortcut/get-shortcut-key-labels :add-slide)}]
     :on-select #(shortcut/call-shortcut-action :add-slide)}]
   [dropdown-item
    {:item [option-text {:label "Paste"
                         :icon [icon/clipboard {:size 16
                                                :color colors/text-primary}]
                         :shortcut (shortcut/get-shortcut-key-labels :paste)}]
     :on-select #(shortcut/call-shortcut-action :paste)}]])

(defn- selected-object-options []
  [:div {:style {:display "flex"
                 :flex-direction "column"
                 :gap gap}}
   [dropdown-item
    {:item [option-text {:label "Focus"
                         :icon [icon/focus {:size 16
                                            :color colors/text-primary}]
                         :shortcut (shortcut/get-shortcut-key-labels :focus)}]
     :on-select #(shortcut/call-shortcut-action :focus)}]
   [dropdown-item
    {:item [option-text {:label "Reset to initials"
                         :icon [icon/reset-init {:size 16
                                                 :color colors/text-primary}]
                         :shortcut (shortcut/get-shortcut-key-labels :reset-initials)}]
     :on-select #(shortcut/call-shortcut-action :reset-initials)
     :disabled? (not @(subscribe [::subs/selected-mesh-initial-position?]))}]
   [dropdown-item
    {:item [option-text {:label "Duplicate"
                         :icon [icon/copy {:size 16
                                           :color colors/text-primary}]
                         :shortcut (shortcut/get-shortcut-key-labels :duplicate)}]
     :on-select #(shortcut/call-shortcut-action :duplicate)}]
   [dropdown-item
    {:item [option-text {:label "Copy"
                         :icon [icon/copy-simple {:size 16
                                                  :color colors/text-primary}]
                         :shortcut (shortcut/get-shortcut-key-labels :copy)}]
     :on-select #(shortcut/call-shortcut-action :copy)}]
   [dropdown-item
    {:item [option-text {:label "Paste"
                         :icon [icon/clipboard {:size 16
                                                :color colors/text-primary}]
                         :shortcut (shortcut/get-shortcut-key-labels :paste)}]
     :on-select #(shortcut/call-shortcut-action :paste)}]
   [dropdown-item
    {:item [option-text {:label "Delete"
                         :icon [icon/trash {:size 16
                                            :color colors/warning}]
                         :color colors/warning
                         :shortcut (shortcut/get-shortcut-key-labels :delete)}]
     :on-select #(shortcut/call-shortcut-action :delete)}]
   [dropdown-item
    {:item [option-text {:label "Reset position"
                         :icon [icon/reset-pos {:size 16
                                                :color colors/text-primary}]
                         :shortcut (shortcut/get-shortcut-key-labels :reset-position)}]
     :on-select #(shortcut/call-shortcut-action :reset-position)
     :disabled? (not @(subscribe [::subs/selected-mesh-initial-position?]))}]
   [dropdown-item
    {:item [option-text {:label "Reset rotation"
                         :icon [icon/reset-rot {:size 16
                                                :color colors/text-primary}]
                         :shortcut (shortcut/get-shortcut-key-labels :reset-rotation)}]
     :on-select #(shortcut/call-shortcut-action :reset-rotation)
     :disabled? (not @(subscribe [::subs/selected-mesh-initial-rotation?]))}]
   (when-not (= "text3D" @(subscribe [::subs/selected-mesh-type]))
     [dropdown-item
      {:item [option-text {:label "Reset scale"
                           :icon [icon/reset-sca {:size 16
                                                  :color colors/text-primary}]
                           :shortcut (shortcut/get-shortcut-key-labels :reset-scale)}]
       :on-select #(shortcut/call-shortcut-action :reset-scale)
       :disabled? (not @(subscribe [::subs/selected-mesh-initial-scale?]))}])])

(defn- camera-options []
  (let [camera-locked? @(subscribe [::subs/camera-locked?])
        lock-text (if camera-locked? "Unlock" "Lock")]
    [:div {:style {:display "flex"
                   :flex-direction "column"
                   :gap gap}}
     [dropdown-item
      {:item [option-text {:label "Reset camera to initials"
                           :icon [icon/reset-cam {:size 16
                                                  :color colors/text-primary}]
                           :shortcut (shortcut/get-shortcut-key-labels :camera-reset-to-initials)}]
       :on-select #(shortcut/call-shortcut-action :camera-reset-to-initials)}]
     [dropdown-item
      {:item [option-text {:label (str lock-text " camera")
                           :icon (if camera-locked?
                                   [icon/unlock {:size 16
                                                 :color colors/text-primary}]
                                   [icon/lock {:size 16
                                               :color colors/text-primary}])
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
        :children [:<>
                   (if @(subscribe [::subs/selected-mesh])
                     [selected-object-options]
                     [main-options])
                   [dropdown-separator]
                   [camera-options]]}])))
