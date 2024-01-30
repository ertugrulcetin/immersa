(ns immersa.scene.utils
  (:require
    [applied-science.js-interop :as j]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.core :as api.core]))

(defn register-object-to-rotate [canvas object rotation-factor]
  (let [on-mousemove (fn [event]
                       (let [mouse-x (- (* (/ (j/get event :clientX) (j/get canvas :width)) 2) 1)
                             mouse-y (- (* (/ (j/get event :clientY) (j/get canvas :height)) 2) 1)]

                         (j/assoc-in! object [:rotation :y] (* mouse-x rotation-factor))
                         (j/assoc-in! object [:rotation :x] (* mouse-y rotation-factor))))]
    (common.utils/register-event-listener canvas "mousemove" on-mousemove)))

(defn v3->v-data [obj attrs]
  (reduce
    (fn [acc attr]
      (let [data (if (= attr :rotation)
                   (mapv (comp common.utils/number->fixed api.core/to-deg) (api.core/v3->v (j/get obj :rotation)))
                   (mapv common.utils/number->fixed (api.core/v3->v (j/get obj attr))))]
        (assoc acc attr data)))
    {}
    attrs))
