(ns immersa.ui.editor.components.context-menu
  (:require
    ["@radix-ui/react-context-menu" :as ContextMenu]
    [immersa.ui.editor.components.button :refer [shortcut-button]]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.theme.colors :as colors]
    [spade.core :refer [defclass defattrs]]))

(defclass context-menu-content-styles []
  {:width "220px"
   :z-index "5000"
   :background-color :white
   :border-radius "6px"
   :border "1px solid rgba(22, 23, 24, 0.15)"
   :padding "5px"
   :box-shadow "0px 10px 38px -10px rgba(22, 23, 24, 0.35), 0px 10px 20px -15px rgba(22, 23, 24, 0.2)"
   :animation-duration "400ms"
   :animation-timing-function "cubic-bezier(0.16, 1, 0.3, 1)"
   :will-change "transform, opacity"})

(defclass context-menu-item-styles [disabled?]
  {:line-height "1"
   :border-radius "3px"
   :display "flex"
   :align-items "center"
   :height "25px"
   :padding "0 12px"
   :position "relative"
   :margin-bottom "5px"
   :user-select :none
   :outline :none}
  [:&:hover
   {:background colors/hover-bg
    :border-radius "5px"}]
  [:&:active
   {:background colors/active-bg
    :border-radius "5px"}]
  (when disabled?
    {:cursor :not-allowed
     :opacity 0.5}))

(defclass context-menu-separator-styles []
  {:height "1px"
   :background-color colors/panel-border
   :margin "5px"})

(defclass option-text-style [disabled?]
  {:display :flex
   :flex-direction :row
   :align-items :center
   :justify-content :space-between
   :width "100%"}
  (when disabled?
    {:cursor :not-allowed}))

(defn- option-text [{:keys [label shortcut disabled? color]}]
  [:div {:class (option-text-style disabled?)}
   [text {:weight :light
          :size :s
          :disabled? disabled?
          :color color} label]
   (when (seq shortcut)
     [:div
      {:style {:display "flex"
               :gap "4px"}}
      (for [s shortcut]
        ^{:key s}
        [shortcut-button s])])])

(defn context-menu-item [{:keys [label on-select disabled? shortcut color]}]
  [:> ContextMenu/Item
   {:on-select on-select
    :disabled disabled?
    :class (context-menu-item-styles disabled?)}
   [option-text {:label label
                 :disabled disabled?
                 :shortcut shortcut
                 :color color}]])

(defn context-menu-separator []
  [:> ContextMenu/Separator
   {:class (context-menu-separator-styles)}])

(defn context-menu [{:keys [trigger children]}]
  [:> ContextMenu/Root
   [:> ContextMenu/Trigger {:as-child true}
    (if (-> trigger first fn?)
      (apply (first trigger) (rest trigger))
      trigger)]
   [:> ContextMenu/Portal
    [:> ContextMenu/Content
     {:class (context-menu-content-styles)
      :side-offset 5}
     children]]])
