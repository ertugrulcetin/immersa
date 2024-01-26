(ns immersa.ui.editor.components.button
  (:require
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [spade.core :refer [defclass defattrs]]))

(def common
  {:display :flex
   :align-items :center
   :justify-content :center
   :outline :none
   :box-shadow (str colors/button-box-shadow " 0px 1px 2px")
   :gap "4px"
   :padding "5px 8px"
   :border-radius "5px"
   :font-size typography/m
   :font-weight typography/medium})

(defclass button-regular []
  (merge common
         {:color colors/button-text
          :background colors/button-bg
          :border (str "1px solid " colors/button-border)})
  [:&:hover {:background colors/button-bg-hover}]
  [:&:active {:background colors/button-bg-active}])

(defclass button-outline []
  (merge common
         {:color colors/button-outline-text
          :background colors/button-outline-bg
          :border (str "1px solid " colors/button-outline-border)})
  [:&:hover {:background colors/hover-bg}]
  [:&:active {:background colors/active-bg}])

(defclass button-primary []
  (merge common
         {:color colors/text-primary
          :background colors/button-outline-bg
          :border (str "1px solid " colors/border2)})
  [:&:hover {:background colors/hover-bg}]
  [:&:active {:background colors/active-bg}])

(defn button [{:keys [text
                      on-click
                      icon-left
                      icon-right
                      class
                      type]}]
  [:button
   {:on-click on-click
    :class [(case type
              :regular (button-regular)
              :outline (button-outline)
              (button-primary))
            class]}
   (when icon-left icon-left)
   [:span text]
   (when icon-right icon-right)])
