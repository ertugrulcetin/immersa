(ns immersa.ui.editor.components.dropdown
  (:require
    ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
    [immersa.ui.editor.components.button :refer [shortcut-button]]
    [immersa.ui.editor.components.scroll-area :refer [scroll-area]]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.theme.colors :as colors]
    [spade.core :refer [defclass]]))

(def dropdown-content-width "200px")
(def dropdown-content-height "180px")

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

(defclass menu-item [disabled?]
  {:border-radius "3px"
   :display "flex"
   :align-items "center"
   :min-height "25px"
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

(defclass dropdown-content-scroll-area []
  {:width dropdown-content-width
   :height dropdown-content-height
   :overflow :hidden}
  ;; Fade out effect
  [:&:before
   {:content "''"
    :position "fixed"
    :width dropdown-content-height
    :height "8px"
    :background (str "linear-gradient(180deg," colors/app-background " 0%,rgba(252,252,253,0) 100%)")}])

(defclass seprator-style []
  {:height "1px"
   :background-color colors/panel-border
   :margin "5px"})

(defn dropdown-separator []
  [:> DropdownMenu/Separator {:class (seprator-style)}])

(defclass option-text-style [disabled?]
  {:display :flex
   :flex-direction :row
   :align-items :center
   :justify-content :space-between
   :width "100%"}
  (when disabled?
    {:cursor :not-allowed}))

(defn option-text [{:keys [label icon color shortcut disabled?]}]
  [:div {:class (option-text-style disabled?)}
   [:div {:style {:display "flex"
                  :align-items "center"
                  :gap "4px"}}
    icon
    [text {:weight :light
           :disabled? disabled?
           :color color} label]]
   (when (seq shortcut)
     [:div
      {:style {:display "flex"
               :gap "4px"}}
      (for [s shortcut]
        ^{:key s}
        [shortcut-button s])])])

(defn dropdown-item [{:keys [item on-select disabled?]}]
  [:> DropdownMenu/Item
   {:class (menu-item disabled?)
    :on-select on-select
    :disabled disabled?}
   item])

(defn dropdown [{:keys [trigger children style scroll?]}]
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger {:as-child true}
    (if (-> trigger first fn?)
      (apply (first trigger) (rest trigger))
      trigger)]
   [:> DropdownMenu/Portal
    [:> DropdownMenu/Content {:style style
                              :class (content-style)}
     (if scroll?
       [scroll-area
        {:class (dropdown-content-scroll-area)
         :children children}]
       children)]]])

(defclass context-menu-content-style []
  {:width "225px"
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
