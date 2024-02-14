(ns immersa.scene.ui-notifier
  (:require
    [applied-science.js-interop :as j]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.core :as api.core]
    [immersa.scene.utils :as utils]
    [immersa.ui.editor.events :as events]
    [immersa.ui.editor.events :as editor.events]
    [re-frame.core :refer [dispatch]]))

(defn- update-ui-by-selected-mesh-type [name type mesh]
  (let [pos-rot-scale-name (assoc (utils/v3->v-data mesh [:position :rotation :scaling]) :name name)]
    (case type
      "text3D" (dispatch [::events/set-selected-text3D-data
                          (merge
                            pos-rot-scale-name
                            {:type "text3D"
                             :text (api.core/get-node-attr mesh :text)
                             :depth (api.core/get-node-attr mesh :depth)
                             :size (api.core/get-node-attr mesh :size)
                             :face-to-screen? (api.core/get-node-attr mesh :face-to-screen?)
                             :opacity (j/get mesh :visibility)
                             :color (-> (j/get-in mesh [:material :albedoColor]) api.core/color->v)
                             :emissive-color (some-> (j/get-in mesh [:material :emissiveColor]) api.core/color->v)
                             :emissive-intensity (j/get-in mesh [:material :emissiveIntensity])
                             :alpha (j/get-in mesh [:material :alpha])
                             :metallic (j/get-in mesh [:material :metallic])
                             :roughness (j/get-in mesh [:material :roughness])})])
      "glb" (dispatch [::events/set-selected-glb-data (merge pos-rot-scale-name {:type "glb"})])
      "image" (dispatch [::events/set-selected-image-data
                         (merge pos-rot-scale-name
                                {:type "image"
                                 :face-to-screen? (api.core/get-node-attr mesh :face-to-screen?)})]))))

(defn notify-ui-selected-mesh [mesh]
  (let [name (j/get mesh :immersa-id)
        type (api.core/get-object-type-by-name name)]
    (update-ui-by-selected-mesh-type name type mesh)))

(defn notify-ui-selected-mesh-rotation-axis [axis value]
  (dispatch [::events/update-selected-mesh-rotation-axis axis (-> value
                                                                  api.core/to-deg
                                                                  common.utils/number->fixed
                                                                  str)]))

(defn sync-slides-info [current-index slides]
  (dispatch [::editor.events/sync-slides-info {:current-index current-index
                                               :slides slides}]))

(defn notify-gizmo-state [type enabled?]
  (dispatch [::editor.events/notify-gizmo-state type enabled?]))
