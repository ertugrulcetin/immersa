(ns immersa.ui.editor.components.dropdown
  (:require
    ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
    [immersa.ui.theme.colors :as colors]
    [spade.core :refer [defclass]]))

(defclass content-style []
  {:min-width "200px"
   :z-index "5000"
   :background-color "white"
   :border-radius "6px"
   :border "1px solid rgba(22, 23, 24, 0.15)"
   :padding "5px"
   :box-shadow "0px 10px 38px -10px rgba(22, 23, 24, 0.35), 0px 10px 20px -15px rgba(22, 23, 24, 0.2)"
   :animation-duration "400ms"
   :animation-timing-function "cubic-bezier(0.16, 1, 0.3, 1)"
   :will-change "transform, opacity"})

(defclass menu-item []
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
    :border-radius "5px"}])

(defclass seprator-style []
  {:height "1px"
   :background-color colors/panel-border
   :margin "5px"})

(defn dropdown-separator []
  [:> DropdownMenu/Separator {:class (seprator-style)}])

(defn dropdown-item [{:keys [item on-click]}]
  [:> DropdownMenu/Item
   {:class (menu-item)
    :on-click on-click}
   item])

(defn dropdown [{:keys [trigger children style]}]
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger {:as-child true}
    (if (-> trigger first fn?)
      (apply (first trigger) (rest trigger))
      trigger)]
   [:> DropdownMenu/Portal
    [:> DropdownMenu/Content {:style style
                              :class (content-style)}
     children]]])

(defclass context-menu-content-style []
  {:width "220px"
   :z-index "5000"
   :background-color "white"
   :border-radius "6px"
   :border "1px solid rgba(22, 23, 24, 0.15)"
   :padding "5px"
   :box-shadow "0px 10px 38px -10px rgba(22, 23, 24, 0.35), 0px 10px 20px -15px rgba(22, 23, 24, 0.2)"
   :animation-duration "400ms"
   :animation-timing-function "cubic-bezier(0.16, 1, 0.3, 1)"
   :will-change "transform, opacity"})

(defn dropdown-context-menu [{:keys [trigger children on-open-change style]}]
  [:> DropdownMenu/Root {:default-open true
                         :on-open-change on-open-change}
   [:> DropdownMenu/Trigger {:as-child true}
    (if (-> trigger first fn?)
      (apply (first trigger) (rest trigger))
      trigger)]
   [:> DropdownMenu/Portal
    [:> DropdownMenu/Content {:style style
                              :class (context-menu-content-style)}
     children]]])
