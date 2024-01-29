(ns immersa.ui.editor.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::calculated-canvas-wrapper-dimensions
  (fn [db]
    (let [wrapper-width (-> db :editor :canvas-wrapper :width)
          wrapper-height (-> db :editor :canvas-wrapper :height)
          max-height wrapper-height
          max-width wrapper-width
          aspect-ratio (/ 16 9)
          height-based-width (* max-height aspect-ratio)
          width-based-height (/ max-width aspect-ratio)]
      (if (> height-based-width max-width)
        {:width max-width :height width-based-height}
        {:width height-based-width :height max-height}))))

(reg-sub
  ::selected-mesh
  (fn [db]
    (-> db :editor :selected-mesh)))

(reg-sub
  ::selected-mesh-position
  (fn [db]
    (-> db :editor :selected-mesh :position)))

(reg-sub
  ::selected-mesh-rotation
  (fn [db]
    (-> db :editor :selected-mesh :rotation)))

(reg-sub
  ::selected-mesh-scaling
  (fn [db]
    (-> db :editor :selected-mesh :scaling)))
