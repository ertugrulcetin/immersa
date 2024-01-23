(ns immersa.ui.present.subs
  (:require
    [breaking-point.core :as bp]
    [re-frame.core :refer [reg-sub]]))

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
    {:current-slide-index (-> db :present :current-slide-index (or "-"))
     :slide-count (-> db :present :slide-count (or "-"))}))

(reg-sub
  ::show-arrow-keys-text?
  (fn [db]
    (-> db :present :show-arrow-keys-text?)))

(reg-sub
  ::show-pre-warm-text?
  (fn [db]
    (-> db :present :show-pre-warm-text?)))

(reg-sub
  ::background-color
  (fn [db]
    (str "radial-gradient(" (-> db :present :background-color) ", rgb(0,0,0))")))
