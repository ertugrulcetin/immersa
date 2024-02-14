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

(defclass shortcut-button-style [symbol?]
  {:min-width "20px"
   :height "20px"
   :padding-right "3px"
   :padding-left "3px"
   :font-size (if symbol? typography/s typography/xs)})

(defattrs button-text []
  {:user-select :none})

(defn button [{:keys [text
                      on-click
                      icon-left
                      icon-right
                      class
                      style
                      type]}]
  [:button
   {:on-click on-click
    :class [(case type
              :regular (button-regular)
              :outline (button-outline)
              (button-primary))
            class]
    :style style}
   (when icon-left icon-left)
   (when text [:span (button-text) text])
   (when icon-right icon-right)])

(defn shortcut-button [text]
  (let [symbol? (not (re-matches #"[A-Za-z0-9]+" text))]
    [button {:text text
             :class (shortcut-button-style symbol?)}]))
