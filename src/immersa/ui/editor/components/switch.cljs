(ns immersa.ui.editor.components.switch
  (:require
    ["@radix-ui/react-switch" :as Switch]
    [immersa.ui.theme.colors :as colors]
    [spade.core :refer [defclass defattrs]]))

(defclass switch-root []
  {:all :unset
   :width "36px"
   :height "18px"
   :border-radius "9999px"
   :-webkit-tap-highlight-color "rgba(0,0,0,0)"
   :position :relative
   :background-color colors/border3
   :box-shadow (str colors/button-box-shadow " 0px 1px 2px")}
  ["&[data-state='checked']"
   {:background-color colors/button-bg}])

(defclass switch-thumb []
  {:display :block
   :width "16px"
   :height "16px"
   :background-color :white
   :border-radius "9999px"
   :box-shadow (str colors/button-box-shadow " 0px 1px 2px")
   :transition "transform 100ms"
   :transform "translateX(2px)"
   :will-change "transform"}
  ["&[data-state='checked']"
   {:transform "translateX(19px)"}])

(defn switch [{:keys [checked? on-change]}]
  [:> Switch/Root
   {:class (switch-root)
    :checked checked?
    :on-checked-change on-change}
   [:> Switch/Thumb
    {:class (switch-thumb)}]])
