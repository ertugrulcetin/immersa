(ns immersa.ui.editor.components.textarea
  (:require
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [spade.core :refer [defclass defattrs]]))

(defclass textarea-style []
  {:font-size typography/s
   :font-weight typography/regular
   :color colors/text-primary
   :border (str "1px solid " colors/border2)
   :border-radius "5px"
   :outline :none
   :width "297px"
   :height "70px"
   :resize :none
   :padding "8px"
   :box-sizing :border-box}
  [:&:hover
   {:box-shadow (str colors/button-box-shadow " 0px 1px 2px")
    :border (str "1px solid " colors/border3)}]
  [:&:focus
   {:box-shadow (str colors/button-box-shadow " 0px 1px 2px")
    :border (str "1px solid " colors/button-outline-border)}]
  [:&.invalid
   {:border (str "1px solid " colors/error)}])

(defn textarea [{:keys [value on-change]}]
  [:textarea {:class (textarea-style)
              :value value
              :on-change on-change}])
