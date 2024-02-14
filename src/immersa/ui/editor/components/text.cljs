(ns immersa.ui.editor.components.text
  (:require
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]))

(defn text
  ([text*]
   (text {} text*))
  ([{:keys [size weight color class style]
     :or {color colors/text-primary}} text]
   (let [font-size (case size
                     :xxs typography/xxs
                     :xs typography/xs
                     :s typography/s
                     :m typography/m
                     :l typography/l
                     :xl typography/xl
                     :xxl typography/xxl
                     :xxxl typography/xxxl
                     typography/m)
         font-weight (case weight
                       :light typography/light
                       :regular typography/regular
                       :medium typography/medium
                       :semi-bold typography/semi-bold
                       :bold typography/bold
                       typography/medium)]
     [:span
      {:style (merge {:font-size font-size
                      :font-weight font-weight
                      :color color
                      :user-select "none"} style)
       :class [(when class class)]}
      text])))
