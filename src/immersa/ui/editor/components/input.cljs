(ns immersa.ui.editor.components.input
  (:require
    [applied-science.js-interop :as j]
    [clojure.string :as str]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [reagent.core :as r]
    [spade.core :refer [defclass defattrs]]))

(defattrs input-wrapper [disabled?]
  {:position :relative}
  [:&:hover
   (if disabled?
     [:label {:cursor :not-allowed}]
     [:label {:visibility :hidden}])])

(defclass input-number-style [disabled?]
  {:font-size typography/s
   :font-weight typography/medium
   :color colors/text-primary
   :border (str "1px solid " colors/border2)
   :border-radius "5px"
   :cursor (if disabled? :not-allowed :auto)
   :padding "0 0.4rem"
   :outline :none}
  [:&:hover
   {:box-shadow (str colors/button-box-shadow " 0px 1px 2px")
    :border (str "1px solid " colors/border3)}]
  [:&:focus
   {:box-shadow (str colors/button-box-shadow " 0px 1px 2px")
    :border (str "1px solid " colors/button-outline-border)}]
  [:&.invalid
   {:border (str "1px solid " colors/error)}]
  ["&[type=\"number\"]::-webkit-outer-spin-button"
   "&[type=\"number\"]::-webkit-inner-spin-button"
   {:height "25px"
    :z-index 2
    :position :absolute
    :top "0"
    :right "0"
    :bottom "0"}])

(defattrs label-style []
  {:width :min-content
   :height "calc(100% - 2px)"
   :padding "0 0.4rem"
   :z-index 1
   :top "1px"
   :right "1px"
   :position :absolute
   :display :flex
   :justify-content :center
   :align-items :center
   :border-top-right-radius "1rem"
   :border-bottom-right-radius "1rem"})

(defn input-number [_]
  (let [error? (r/atom false)]
    (fn [{:keys [min max step class style label on-change value disabled?]
          :or {min "-Infinity"
               max "Infinity"
               step "0.1"}}]
      [:div (input-wrapper disabled?)
       [:input {:class [(input-number-style disabled?) class (when @error? "invalid")]
                :style (merge {:width "56px"
                               :height "24px"} style)
                :value value
                :disabled (or (nil? value) disabled?)
                :type "number"
                :min min
                :max max
                :step step
                :on-wheel (fn [e]
                            (-> e .-target .focus))
                :on-change (fn [e]
                             (let [v (-> e .-target .-value)
                                   v (if-not (str/blank? v)
                                       (cond
                                         (< (parse-double v)
                                            (parse-double min))
                                         min

                                         (> (parse-double v)
                                            (parse-double max))
                                         max

                                         :else v)
                                       v)]
                               (when on-change (on-change v))
                               (reset! error? (str/blank? v))))
                :on-key-down (fn [e]
                               (when (= (j/get e :key) ".")
                                 (.preventDefault e)))}]
       (when label
         [:label (label-style)
          [text {:size :s} label]])])))
