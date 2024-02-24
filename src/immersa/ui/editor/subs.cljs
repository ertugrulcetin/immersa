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
  ::editor
  (fn [db]
    (:editor db)))

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

(defn has-initial? [db type]
  (let [slides (-> db :editor :slides :all)
        current-index (-> db :editor :slides :current-index)
        selected-mesh-name (-> db :editor :selected-mesh :name)]
    (boolean (get-in slides [current-index :data selected-mesh-name type]))))

(reg-sub
  ::selected-mesh-initial-position?
  (fn [db]
    (has-initial? db :initial-position)))

(reg-sub
  ::selected-mesh-initial-rotation?
  (fn [db]
    (has-initial? db :initial-rotation)))

(reg-sub
  ::selected-mesh-initial-scale?
  (fn [db]
    (has-initial? db :initial-scale)))

(reg-sub
  ::selected-mesh-linked-type
  (fn [db]
    (-> db :editor :selected-mesh :linked-type)))

(reg-sub
  ::camera
  (fn [db]
    (-> db :editor :camera)))

(reg-sub
  ::camera-position
  (fn [db]
    (-> db :editor :camera :position)))

(reg-sub
  ::camera-rotation
  (fn [db]
    (-> db :editor :camera :rotation)))

(defn- color->rgb-str [color]
  (let [[r g b] (or color [255 255 255])]
    (str "rgb(" r "," g "," b ")")))

(reg-sub
  ::scene-background-color
  (fn [db]
    (let [current-index (-> db :editor :slides :current-index)
          slides (-> db :editor :slides :all)]
      (-> (nth slides current-index nil) :data :skybox :background :color color->rgb-str))))

(reg-sub
  ::scene-background-brightness
  (fn [db]
    (let [current-index (-> db :editor :slides :current-index)
          slides (-> db :editor :slides :all)]
      (-> (nth slides current-index nil) :data :skybox :background :brightness (* 100) int))))

(reg-sub
  ::selected-mesh-color
  (fn [db]
    (-> db :editor :selected-mesh :color color->rgb-str)))

(reg-sub
  ::selected-mesh-emissive-intensity
  (fn [db]
    (-> db :editor :selected-mesh :emissive-intensity (* 100) int)))

(reg-sub
  ::selected-mesh-metallic
  (fn [db]
    (-> db :editor :selected-mesh :metallic (or 0) (* 100) int)))

(reg-sub
  ::selected-mesh-roughness
  (fn [db]
    (-> db :editor :selected-mesh :roughness (or 0) (* 100) int)))

(reg-sub
  ::selected-mesh-opacity
  (fn [db]
    (-> db :editor :selected-mesh :opacity (* 100) int)))

(reg-sub
  ::selected-mesh-text-content
  (fn [db]
    (-> db :editor :selected-mesh :text)))

(reg-sub
  ::selected-mesh-text-size
  (fn [db]
    (-> db :editor :selected-mesh :scaling first)))

(reg-sub
  ::selected-mesh-text-depth
  (fn [db]
    (-> db :editor :selected-mesh :scaling last)))

(reg-sub
  ::selected-mesh-type
  (fn [db]
    (-> db :editor :selected-mesh :type)))

(reg-sub
  ::selected-mesh-face-to-screen?
  (fn [db]
    (-> db :editor :selected-mesh :face-to-screen?)))

(reg-sub
  ::slides-current-index
  (fn [db]
    (-> db :editor :slides :current-index)))

(reg-sub
  ::slides-all
  (fn [db]
    (-> db :editor :slides :all)))

(reg-sub
  ::slides-thumbnails
  (fn [db]
    (-> db :editor :slides :thumbnails)))

(reg-sub
  ::slide-thumbnail
  (fn [db [_ index]]
    (let [thumbnails (-> db :editor :slides :thumbnails)]
      (get thumbnails (-> db :editor :slides :all (get-in [index :id]))))))

(reg-sub
  ::gizmo-visible?
  (fn [db [_ type]]
    (-> db :editor :gizmo type)))

(reg-sub
  ::camera-locked?
  (fn [db]
    (-> db :editor :camera :locked?)))

(reg-sub
  ::context-menu-position
  (fn [db]
    (-> db :editor :context-menu :position)))
