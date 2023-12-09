(ns immersa.subs
  (:require
    [breaking-point.core :as bp]
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::name
  (fn [db]
    (:name db)))

(reg-sub
  ::calculated-canvas-dimensions
  :<- [::bp/screen-width]
  :<- [::bp/screen-height]
  (fn [[screen-width screen-height]]
    (let [max-height (- screen-height 64)
          aspect-ratio (/ 16 9)
          max-width screen-width
          height-based-width (* max-height aspect-ratio)
          width-based-height (/ max-width aspect-ratio)]
      (if (> height-based-width max-width)
        {:width max-width :height width-based-height}
        {:width height-based-width :height max-height}))))

(reg-sub
  ::slide-info
  (fn [db]
    {:current-slide-index (:current-slide-index db "-")
     :slide-count (:slide-count db "-")}))
