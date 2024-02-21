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

(defattrs shortcut-button-style [symbol?]
  {:box-sizing :border-box
   :display :inline-flex
   :align-items :center
   :justify-content :center
   :flex-shrink 0
   :font-weight 400
   :font-family typography/font
   :vertical-align :text-top
   :white-space :nowrap
   :user-select :none
   :cursor :default
   :position :relative
   :top "-0.03em"
   :font-size (if symbol? ".65em" ".62em")
   :min-width "1.75em"
   :line-height "1.7em"
   :padding-left "0.5em"
   :padding-right "0.5em"
   :padding-bottom "0.05em"
   :word-spacing "-0.1em"
   :border-radius "0.35em"
   :letter-spacing "0em"
   :color "color(display-p3 0.113 0.125 0.14)"
   :background-color "color(display-p3 0.988 0.988 0.992)"
   :box-shadow "color(display-p3 0.024 0.024 0.349 / 0.024) 0px -0.525px 5.25px 0px inset, color(display-p3 1 1 1 / 0.95) 0px 0.525px 0px 0px inset, color(display-p3 0.024 0.024 0.349 / 0.024) 0px 2.625px 5.25px 0px inset, color(display-p3 0.008 0.008 0.165 / 0.15) 0px -0.525px 0px 0px inset, color(display-p3 0.004 0.039 0.2 / 0.122) 0px 0px 0px 0.525px, color(display-p3 0.008 0.027 0.184 / 0.197) 0px 0.84px 1.785px 0px"})

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

(defn shortcut-button [key]
  (let [symbol? (not (re-matches #"[A-Za-z0-9]+" key))]
    [:kbd (shortcut-button-style symbol?)
     key]))
