(ns immersa.scene.ui-listener
  (:require
    [applied-science.js-interop :as j]
    [immersa.common.communication :refer [event-bus-pub]]
    [immersa.scene.api.core :as api.core])
  (:require-macros
    [immersa.common.macros :refer [go-loop-sub]]))

(defmulti handle-ui-update :type)

(defmethod handle-ui-update :update-selected-mesh [{{:keys [update value]} :data}]
  (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
    (case update
      :position (j/assoc! mesh :position (api.core/v->v3 value))
      :rotation (j/assoc! mesh :rotation (api.core/v->v3 (mapv api.core/to-rad value)))
      :scaling (j/assoc! mesh :scaling (api.core/v->v3 value)))))

(defn init-ui-update-listener []
  (go-loop-sub event-bus-pub :get-ui-update [_ data]
    (handle-ui-update data)))
