(ns immersa.scene.slide
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go go-loop <!]]
    [clojure.set :as set]
    [clojure.walk :as walk]
    [com.rpl.specter :as sp]
    [goog.functions :as functions]
    [immersa.common.firebase :as firebase]
    [immersa.common.utils :as common.utils]
    [immersa.presentations.intro-immersa :refer [immersa-intro-slides]]
    [immersa.scene.api.animation :as api.animation]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.component :as api.component]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v2 v3 v4]]
    [immersa.scene.api.gui :as api.gui]
    [immersa.scene.api.mesh :as api.mesh]
    [immersa.scene.api.particle :as api.particle]
    [immersa.scene.materials-in-sphere :as mat.spheres]
    [immersa.scene.ui-notifier :as ui.notifier]
    [immersa.scene.utils :as utils]
    [immersa.ui.editor.events :as editor.events]
    [immersa.ui.events :as main.events]
    [immersa.ui.present.events :as events]
    [re-frame.core :refer [dispatch dispatch-sync]]))

(def brightness-scale-factor 11.8)

(defn calculate-brightness-factor [brightness]
  (* brightness brightness-scale-factor))

(defn- get-position-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-position (j/get object :position)
        end-position (:position object-slide-info)
        camera? (= object-name :camera)]
    (let [end-position (cond
                         (and start-position
                              end-position
                              (or (and
                                    (not (vector? end-position))
                                    (not (api.core/equals? start-position end-position)))
                                  (and (vector? end-position)
                                       (> (count end-position) 1))))
                         end-position

                         (and camera?
                              start-position
                              (not end-position)
                              (not (api.core/equals? start-position (j/get object :init-position))))
                         (api.core/clone (j/get object :init-position)))]
      (when end-position
        [object-name (api.animation/create-position-animation (assoc object-slide-info
                                                                     :start start-position
                                                                     :end end-position))]))))

(defn- get-rotation-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-rotation (j/get object :rotation)
        end-rotation (:rotation object-slide-info)
        camera? (= object-name :camera)]
    (let [end-rotation (cond
                         (and end-rotation
                              (or (and
                                    (not (vector? end-rotation))
                                    (not (api.core/equals? start-rotation end-rotation)))
                                  (and (vector? end-rotation)
                                       (> (count end-rotation) 1))))
                         end-rotation

                         (and camera?
                              (not end-rotation)
                              (not (api.core/equals? start-rotation (j/get object :init-rotation))))
                         (api.core/clone (j/get object :init-rotation)))]
      (when end-rotation
        [object-name (api.animation/create-rotation-animation (assoc object-slide-info
                                                                     :start start-rotation
                                                                     :end end-rotation))]))))

(defn- get-scale-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-scale (j/get object :scaling)
        end-scale (:scale object-slide-info)
        camera? (= object-name :camera)]
    (when-not camera?
      (let [end-scale (when
                        (and start-scale
                             end-scale
                             (or (and
                                   (not (vector? end-scale))
                                   (not (api.core/equals? start-scale end-scale)))
                                 (and (vector? end-scale)
                                      (> (count end-scale) 1))))
                        end-scale)]
        (when end-scale
          [object-name (api.animation/create-scale-animation (assoc object-slide-info
                                                                    :start start-scale
                                                                    :end end-scale))])))))

(defn- get-visibility-anim [object-slide-info object-name]
  (if (= :image (:type object-slide-info))
    (let [end-visibility (:visibility object-slide-info)
          start-visibility (j/get-in (api.core/get-object-by-name object-name) [:material :alpha])]
      (when (and end-visibility (not= start-visibility end-visibility))
        [object-name (api.animation/create-image-visibility-animation {:start start-visibility
                                                                       :end end-visibility
                                                                       :delay (:delay object-slide-info)})]))
    (let [end-visibility (:visibility object-slide-info)
          start-visibility (j/get (api.core/get-object-by-name object-name) :visibility)]
      (when (and end-visibility (not= start-visibility end-visibility))
        [object-name (api.animation/create-visibility-animation {:start start-visibility
                                                                 :end end-visibility
                                                                 :delay (:delay object-slide-info)})]))))

(defn- get-alpha-anim [object-slide-info object-name]
  (let [end-alpha (:alpha object-slide-info)
        start-alpha (j/get (api.core/get-object-by-name object-name) :alpha)]
    (when (and end-alpha (not= start-alpha end-alpha))
      [object-name (api.animation/create-alpha-animation {:start start-alpha
                                                          :end end-alpha
                                                          :delay (:delay object-slide-info)})])))

(defn get-target-anim [object-slide-info object-name]
  (when (and object-slide-info (= object-name :camera))
    (let [camera (api.camera/active-camera)
          start-target (j/call camera :getTarget)
          end-target (:target object-slide-info)
          end-target (and end-target
                          (or (and
                                (not (vector? end-target))
                                (not (api.core/equals? start-target end-target)))
                              (and (vector? end-target)
                                   (> (count end-target) 1))))]
      (when end-target
        [object-name (api.animation/create-camera-target-anim {:camera camera
                                                               :target end-target
                                                               :duration (:duration object-slide-info)
                                                               :delay (:delay object-slide-info)})]))))

(defn- get-animations-from-slide-info [acc object-slide-info object-name]
  (reduce
    (fn [acc anim-type]
      (let [[object-name anim :as anim-vec] (case anim-type
                                              :position (get-position-anim object-slide-info object-name)
                                              :rotation (get-rotation-anim object-slide-info object-name)
                                              :scale (get-scale-anim object-slide-info object-name)
                                              :visibility (get-visibility-anim object-slide-info object-name)
                                              :alpha (get-alpha-anim object-slide-info object-name)
                                              :focus (api.animation/create-focus-camera-anim object-slide-info)
                                              :target (get-target-anim object-slide-info object-name))]
        (cond-> acc
          (and (not= anim-type :focus) anim-vec)
          (conj anim-vec)

          (and (= anim-type :focus) anim-vec)
          (conj [object-name (first anim)] [object-name (second anim)]))))
    acc
    [:position :rotation :scale :visibility :alpha :focus :target]))

(defn- notify-ui [index slides-count]
  (dispatch [::events/update-slide-info index slides-count]))

(defn- parse-pos-rot-scale [type form]
  (cond
    (and (map? form)
         (type form)
         (number? (type form)))
    (assoc form type (v3 (type form)))

    (and (map? form)
         (type form)
         (vector? (type form))
         (number? (first (type form))))
    (assoc form type (apply v3 (type form)))

    (and (map? form)
         (type form)
         (vector? (type form))
         (vector? (first (type form))))
    (assoc form type (mapv (partial apply v3) (type form)))

    :else form))

(defn- parse-colors [type form]
  (let [color-kw->color (fn [kw]
                          (case kw
                            :color/white (api.const/color-white)
                            :color/black (api.const/color-black)
                            :color/red (api.const/color-red)
                            :color/yellow (api.const/color-yellow)
                            :color/teal (api.const/color-teal)
                            :color/gray (api.const/color-gray)))]
    (cond
      (and (map? form)
           (type form)
           (keyword? (type form)))
      (assoc form type (color-kw->color (type form)))

      (and (map? form)
           (type form)
           (vector? (type form)))
      (assoc form type (apply api.core/color-rgb (type form)))

      (and (map? form)
           (type form)
           (number? (type form)))
      (assoc form type (api.core/color-rgb (type form) (type form) (type form)))

      :else form)))

(defn- parse-skybox-background-color [slides]
  (let [adjust-color (fn [m]
                       (assoc m :color (mapv #(* % (calculate-brightness-factor (:brightness m))) (:color m))))]
    (sp/transform [sp/ALL :data :skybox :background] adjust-color slides)))

(defn parse-slides [slides]
  (walk/prewalk
    (fn [form]
      (->> form
           (parse-pos-rot-scale :position)
           (parse-pos-rot-scale :rotation)
           (parse-pos-rot-scale :scale)
           (parse-colors :color)
           (parse-colors :hl-color)
           (parse-colors :albedo-color)
           (parse-colors :emissive-color)))
    slides))

(defn get-slides [slides]
  (let [slides (parse-skybox-background-color slides)
        slides (parse-slides slides)
        slides-vec (vec (map-indexed #(assoc %2 :index %1) slides))]
    slides-vec))

(defn- run-skybox-bg-image->bg-image-dissolve-anim [skybox]
  (let [skybox-path (-> skybox :background :image)
        skybox-shader (api.core/get-object-by-name "skybox-shader")
        current-skybox-path (j/get skybox-shader :skybox-path)
        default-skybox-path (j/get skybox-shader :default-skybox-path)
        new-skybox-path (cond
                          (and skybox-path (not= skybox-path current-skybox-path))
                          skybox-path

                          (and (nil? skybox-path) (not= default-skybox-path current-skybox-path))
                          default-skybox-path)]
    (when new-skybox-path
      (api.animation/create-skybox-bg-image->bg-image-dissolve-anim
        :skybox-path new-skybox-path
        :duration (:duration skybox)))))

(defmulti disable-component (comp keyword api.core/get-object-type-by-name))

(defn- disable-mesh-component-via-visibility [name]
  (let [mesh (api.core/get-object-by-name name)
        visibility (j/get mesh :visibility)
        duration 0.5
        to (* 30 1.0)]
    (api.animation/begin-direct-animation
      :target mesh
      :animations (api.animation/create-visibility-animation {:start visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end #(api.core/set-enabled mesh false))))

(defn- disable-image-mesh-component-via-visibility [name]
  (let [mesh (api.core/get-object-by-name name)
        visibility (j/get-in mesh [:material :alpha])
        duration 0.5
        to (* 30 1.0)]
    (api.animation/begin-direct-animation
      :target mesh
      :animations (api.animation/create-image-visibility-animation {:start visibility
                                                                    :end 0
                                                                    :duration duration})
      :to to
      :on-animation-end #(api.core/set-enabled mesh false))))

(defmethod disable-component :text [name]
  (let [text (api.core/get-object-by-name name)
        alpha (j/get text :alpha)
        duration 0.5
        to (* 30 1.0)]
    (api.animation/begin-direct-animation
      :target text
      :animations (api.animation/create-alpha-animation {:start alpha
                                                         :end 0
                                                         :duration duration})
      :to to)))

(defmethod disable-component :earth [name]
  (let [earth-sphere (api.core/get-object-by-name (str name "-earth-sphere"))
        cloud-sphere (api.core/get-object-by-name (str name "-cloud-sphere"))
        hl (j/get-in api.core/db [:nodes name :hl])
        earth-sphere-visibility (j/get earth-sphere :visibility)
        cloud-sphere-visibility (j/get cloud-sphere :visibility)
        duration 0.5
        to (* 30 duration)]
    (j/call hl :removeMesh cloud-sphere)
    (api.animation/begin-direct-animation
      :target earth-sphere
      :animations (api.animation/create-visibility-animation {:start earth-sphere-visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end #(api.core/set-enabled earth-sphere false))
    (api.animation/begin-direct-animation
      :target cloud-sphere
      :animations (api.animation/create-visibility-animation {:start cloud-sphere-visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end #(api.core/set-enabled cloud-sphere false))))

(defmethod disable-component :box [name]
  (disable-mesh-component-via-visibility name))

(defmethod disable-component :billboard [name]
  (disable-mesh-component-via-visibility name))

(defmethod disable-component :image [name]
  (disable-image-mesh-component-via-visibility name))

(defmethod disable-component :text3D [name]
  (disable-mesh-component-via-visibility name))

(defmethod disable-component :greased-line [name]
  (disable-mesh-component-via-visibility name))

(defmethod disable-component :glb [name]
  (api.core/set-enabled (api.core/get-object-by-name name) false))

(defmethod disable-component :sphere-mat [name]
  (api.core/set-enabled (api.core/get-object-by-name name) false))

(defmethod disable-component :pcs-text [name]
  (let [mesh (api.core/get-object-by-name name)
        pcs (j/get mesh :pcs)
        visibility (j/get mesh :visibility)
        duration 0.5
        to (* 30 duration)]
    (api.animation/begin-direct-animation
      :target mesh
      :animations (api.animation/create-visibility-animation {:start visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end (fn []
                          (api.core/dispose mesh)
                          (api.core/dispose pcs)))))

(defmethod disable-component :wave [name]
  (let [pcs (api.core/get-object-by-name name)
        mesh (j/get pcs :mesh)
        visibility (j/get mesh :visibility)
        duration 0.5
        to (* 30 duration)]
    (api.animation/begin-direct-animation
      :target mesh
      :animations (api.animation/create-visibility-animation {:start visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end (fn []
                          (api.core/dispose mesh)
                          (api.core/dispose name)))))

(defmethod disable-component :particle [name]
  (let [ps (api.core/get-object-by-name name)]
    (api.particle/stop ps)
    (api.particle/reset ps)))

(defmethod disable-component :default [name]
  (println "dispose-component Default: " name))

(defmulti enable-component (fn [name _]
                             (-> name api.core/get-object-type-by-name keyword)))

(defn- enable-mesh-component [name]
  (api.core/set-enabled (api.core/get-object-by-name name) true))

(defmethod enable-component :earth [name slide-info]
  (let [hl (j/get-in api.core/db [:nodes name :hl])
        earth-sphere (api.core/get-object-by-name (str name "-earth-sphere"))
        cloud-sphere (api.core/get-object-by-name (str name "-cloud-sphere"))
        duration 0.5
        to (* 30 duration)]
    (api.core/set-enabled earth-sphere true)
    (api.core/set-enabled cloud-sphere true)
    (j/call hl :addMesh cloud-sphere (api.core/color 0.3 0.74 0.94))
    (api.animation/begin-direct-animation
      :target earth-sphere
      :animations (api.animation/create-visibility-animation {:start (j/get earth-sphere :visibility)
                                                              :end (:visibility slide-info)
                                                              :duration duration})
      :to to)
    (api.animation/begin-direct-animation
      :target cloud-sphere
      :animations (api.animation/create-visibility-animation {:start (j/get cloud-sphere :visibility)
                                                              :end (:visibility slide-info)
                                                              :duration duration})
      :to to)))

(defmethod enable-component :box [name _]
  (enable-mesh-component name))

(defmethod enable-component :billboard [name _]
  (enable-mesh-component name))

(defmethod enable-component :image [name _]
  (enable-mesh-component name))

(defmethod enable-component :text3D [name _]
  (enable-mesh-component name))

(defmethod enable-component :greased-line [name _]
  (enable-mesh-component name))

(defmethod enable-component :glb [name _]
  (enable-mesh-component name))

(defmethod enable-component :sphere-mat [name _]
  (enable-mesh-component name))

(defmethod enable-component :wave [name _]
  (when-not (api.core/get-object-by-name name)
    (api.component/wave name)))

(defmethod enable-component :particle [name _]
  (api.particle/start (api.core/get-object-by-name name)))

(defmethod enable-component :default [name]
  (println "enable-component Default: " name))

(let [circuit-breaker-running? (atom false)]
  (defn process-next-prev-command [{:keys [type ch slide-in-progress? current-running-anims on-done] :as opts}]
    (if (and @slide-in-progress? (not @circuit-breaker-running?))
      (do
        (reset! circuit-breaker-running? true)
        (println "Circuit breaker tripped")
        (doseq [{:keys [force-finish-fn force-finish-fn-atom]} @current-running-anims]
          (if-let [force-finish-fn (some-> force-finish-fn-atom deref)]
            (force-finish-fn)
            (force-finish-fn)))
        ;; For fast navigation, re-trigger the command
        (when (number? type)
          (process-next-prev-command opts))
        (reset! current-running-anims [])
        (reset! circuit-breaker-running? false))
      (do
        (a/put! ch type)
        (when on-done (on-done))))))

(defn- create-objects [objects-to-create objects-data]
  (doseq [name objects-to-create]
    (let [params (get objects-data name)
          type (:type params)
          params (dissoc params :type)]
      (case type
        :glb (api.mesh/glb->mesh name params)
        ;; TODO wave mesh is async, so we need to wait for it to be created
        :wave (api.component/wave name params)
        :box (api.component/create-box-with-numbers name params)
        :earth (api.component/earth name params)
        :text3D (api.mesh/text name params)
        :text (api.gui/add-control
                (api.core/get-advanced-texture)
                (api.gui/text-block name params))
        :image (api.component/image name params)
        :particle (case (:particle-type params)
                    :sparkle (api.particle/sparkles name params)
                    :cloud (api.particle/clouds name params))
        :billboard (api.component/billboard name params)
        :sphere-mat (mat.spheres/get-sphere name params)
        :greased-line (api.mesh/greased-line name params)
        nil))))

(defn- pre-warm-the-scene [slides]
  (doseq [slide (map-indexed #(assoc %2 :index %1) slides)]
    (let [slide-data (:data slide)
          objects-to-create (set/difference (set (keys slide-data)) #{:camera :skybox})
          objects-to-create (filter #(not (api.core/get-object-by-name %)) objects-to-create)]
      (create-objects objects-to-create slide-data)
      (when-not (= (:index slide) 0)
        (doseq [obj objects-to-create]
          (disable-component obj)))))
  (dispatch [::events/set-show-pre-warm-text? false]))

(defn- prepare-first-skybox [slides]
  (let [{:keys [color image]} (-> slides first :data :skybox :background)
        skybox-shader-mat (api.core/get-object-by-name "skybox-shader")
        bg-skybox-material (j/get-in api.core/db [:environment-helper :skybox :material])
        bg-ground-material (j/get-in api.core/db [:environment-helper :ground :material])]
    (cond
      color (do
              (some-> skybox-shader-mat (j/call :setFloat "dissolve" 1.5))
              (some-> skybox-shader-mat (j/call :setFloat "transparency" 1.5))
              (some-> skybox-shader-mat (j/assoc! :alpha 0))
              (j/assoc! bg-skybox-material :primaryColor color)
              (j/assoc! bg-ground-material :primaryColor color))
      image (do
              (some-> skybox-shader-mat (j/call :setFloat "dissolve" 0))
              (some-> skybox-shader-mat (j/call :setFloat "transparency" 0))
              (some-> skybox-shader-mat (j/assoc! :alpha 1))
              (some-> (j/call skybox-shader-mat :setTexture "skybox1" (api.core/cube-texture :root-url image)))))))

(defn- add-skybox-anims [{:keys [prev-slide objects-data current-running-anims skybox]}]
  (when prev-slide
    (let [anim (cond
                 (and
                   (-> prev-slide :skybox :background :color)
                   (-> objects-data :skybox :background :color))
                 (api.animation/create-background->background-color-anim skybox)

                 (and (-> prev-slide :skybox :background :image)
                      (-> objects-data :skybox :background :image))
                 (run-skybox-bg-image->bg-image-dissolve-anim skybox)

                 (and (-> prev-slide :skybox :background :image)
                      (-> objects-data :skybox :background :color))
                 (api.animation/create-skybox->background-dissolve-anim skybox)

                 (and (-> prev-slide :skybox :background :color)
                      (-> objects-data :skybox :background :image))
                 (api.animation/create-background->skybox-dissolve-anim skybox))]
      (some->> anim (swap! current-running-anims conj)))))

(defonce all-slides (atom nil))
(defonce command-ch nil)
(defonce current-running-anims (atom []))
(defonce slide-in-progress? (atom false))
(defonce prev-slide (atom nil))
(defonce current-slide-index (atom 0))
(defonce disabled-component-anims (atom []))

(defonce thumbnails (atom {}))

(defn- init-slide-show-state []
  (set! command-ch (a/chan (a/dropping-buffer 1)))
  (reset! current-running-anims [])
  (reset! slide-in-progress? false)
  (reset! prev-slide nil)
  (reset! current-slide-index 0))

(defn go-to-slide [index]
  (process-next-prev-command {:type index
                              :ch command-ch
                              :slide-in-progress? slide-in-progress?
                              :current-running-anims current-running-anims}))

(defn get-slide-id-by-index [index]
  (get-in @all-slides [index :id]))

(defn get-slide-index-by-id [id]
  (->> @all-slides
       (sp/select-one [sp/INDEXED-VALS #(= id (:id (second %)))])
       first))

(defn duplicate-slide-data [[id params]]
  (let [mesh (api.core/get-object-by-name id)
        index @current-slide-index
        exists? (boolean (get-in @all-slides [index :data id]))
        id (if exists? (str (random-uuid)) id)
        _ (when (and mesh (not exists?))
            (when (> (:visibility params) 0)
              (j/assoc! mesh :visibility (:visibility params)))
            (api.core/set-enabled mesh true))
        mesh (if exists?
               (let [params (->> params
                                 (parse-pos-rot-scale :position)
                                 (parse-pos-rot-scale :rotation)
                                 (parse-pos-rot-scale :scale)
                                 (parse-colors :color))]
                 (case (:type params)
                   :text3D (api.mesh/text id params)
                   :image (api.component/image id params)
                   :glb (api.mesh/glb->mesh id params)))
               mesh)]
    (sp/setval [sp/ATOM index :data id] params all-slides)
    (sp/setval [sp/ATOM id] params prev-slide)
    mesh))

(defn update-thumbnail []
  (let [{:keys [last-time-slide-updated last-time-thumbnail-updated]} @thumbnails]
    (when (> last-time-slide-updated last-time-thumbnail-updated)
      (let [base64 (j/call-in api.core/db [:canvas :toDataURL] "image/webp" 0.2)
            index @current-slide-index
            id (get-in @all-slides [index :id])]
        (sp/setval [sp/ATOM :thumbnails id] base64 thumbnails)
        (dispatch [::editor.events/sync-thumbnails (:thumbnails @thumbnails)])
        (firebase/upload-thumbnail {:user-id (j/get-in api.core/db [:user :id])
                                    :slide-id id
                                    :presentation-id (j/get-in api.core/db [:presentation :id])
                                    :thumbnail base64}))
      (swap! thumbnails assoc :last-time-thumbnail-updated (js/Date.now)))))

(defn get-slide-data [obj k]
  (let [index @current-slide-index
        object-id (if (keyword? obj) obj (api.core/get-object-name obj))
        path [index :data object-id]
        path (if (vector? k)
               (into path k)
               (conj path k))]
    (get-in @all-slides path)))

(defn get-slide-data-by-index [index obj k]
  (let [object-id (if (keyword? obj) obj (api.core/get-object-name obj))
        path [index :data object-id]
        path (if (vector? k)
               (into path k)
               (conj path k))]
    (get-in @all-slides path)))

(defn add-slide [index*]
  (let [index (or index* @current-slide-index)
        same-slide? (nil? index*)
        uuid (str (random-uuid))
        position (get-slide-data :camera :position)
        rotation (get-slide-data :camera :rotation)
        selected-slide-id (get-slide-id-by-index @current-slide-index)]
    (api.core/clear-selected-mesh)
    (swap! all-slides (fn [slides]
                        (let [duplicated-slide (-> (get slides index)
                                                   (assoc-in [:data :camera :initial-position] position)
                                                   (assoc-in [:data :camera :initial-rotation] rotation))
                              types #{:glb :text3D :image}
                              duplicated-slide (walk/prewalk
                                                 (fn [form]
                                                   (if (and (map? form) (types (:type form)))
                                                     (-> form
                                                         (assoc :initial-position (:position form))
                                                         (assoc :initial-rotation (:rotation form))
                                                         (assoc :initial-scale (:scale form)))
                                                     form))
                                                 duplicated-slide)]
                          (utils/vec-insert slides (assoc duplicated-slide :id uuid) (inc index)))))
    (reset! current-slide-index (get-slide-index-by-id selected-slide-id))
    (when same-slide?
      (go-to-slide (inc @current-slide-index)))
    (ui.notifier/sync-slides-info @current-slide-index @all-slides)
    (js/setTimeout update-thumbnail 550)
    [(inc index) (get @all-slides (inc index))]))

(defn blank-slide []
  (let [index @current-slide-index
        uuid (str (random-uuid))
        position (get-slide-data :camera :position)
        rotation (get-slide-data :camera :rotation)]
    (api.core/clear-selected-mesh)
    (swap! all-slides (fn [slides]
                        (let [slide (get slides index)
                              slide (-> slide
                                        (assoc-in [:data :camera :initial-position] position)
                                        (assoc-in [:data :camera :initial-rotation] rotation))
                              slide {:id uuid
                                     :data {:camera (get-in slide [:data :camera])
                                            :skybox (get-in slide [:data :skybox])}}]
                          (utils/vec-insert slides slide (inc index)))))
    (ui.notifier/sync-slides-info @current-slide-index @all-slides)
    (go-to-slide (inc @current-slide-index))
    (js/setTimeout update-thumbnail 550)
    [(inc index) (get @all-slides (inc index))]))

(defn delete-slide [index]
  (when (> (count @all-slides) 1)
    (api.core/clear-selected-mesh)
    (if (and index (not= index @current-slide-index))
      (let [current-selected-slide-id (get-in @all-slides [@current-slide-index :id])
            _ (sp/setval [sp/ATOM index] sp/NONE all-slides)
            new-current-index (get-slide-index-by-id current-selected-slide-id)]
        (reset! current-slide-index new-current-index)
        (ui.notifier/sync-slides-info @current-slide-index @all-slides))
      (let [index @current-slide-index]
        (if (= index 0)
          (go-to-slide 0)
          (go-to-slide (dec @current-slide-index)))
        (sp/setval [sp/ATOM index] sp/NONE all-slides)
        (when-not (= index 0)
          (swap! current-slide-index dec))
        (ui.notifier/sync-slides-info @current-slide-index @all-slides)))))

(defn re-order-slides [old-index new-index]
  (let [current-selected-slide-id (get-in @all-slides [@current-slide-index :id])
        old-index-slide (get @all-slides old-index)
        _ (sp/setval [sp/ATOM old-index] sp/NONE all-slides)
        _ (sp/transform sp/ATOM #(utils/vec-insert % old-index-slide new-index) all-slides)
        current-index (get-slide-index-by-id current-selected-slide-id)]
    (reset! current-slide-index current-index)
    (ui.notifier/sync-slides-info @current-slide-index @all-slides)))

(defn add-slide-data
  ([obj params]
   (add-slide-data @current-slide-index obj params))
  ([index obj params]
   (sp/setval [sp/ATOM index :data (api.core/get-object-name obj)] params all-slides)
   (sp/setval [sp/ATOM (api.core/get-object-name obj)] params prev-slide)))

(defn delete-slide-data [obj-or-name]
  (let [id (if (string? obj-or-name)
             obj-or-name
             (api.core/get-object-name obj-or-name))
        current-index @current-slide-index]
    (api.core/clear-selected-mesh)
    (sp/setval [sp/ATOM current-index :data id] sp/NONE all-slides)
    (sp/setval [sp/ATOM id] sp/NONE prev-slide)))

(defn update-slide-data [obj k v]
  (let [index @current-slide-index
        object-id (if (keyword? obj) obj (api.core/get-object-name obj))
        path (if (vector? k)
               (apply sp/comp-paths k)
               k)]
    (sp/setval [sp/ATOM index :data object-id path] v all-slides)
    (sp/setval [sp/ATOM object-id path] v prev-slide)))

(defn update-text-mesh [data]
  (let [mesh (:mesh data)
        data (dissoc data :mesh)
        material (api.core/clone (j/get mesh :material))
        position (api.core/clone (j/get mesh :position))
        rotation (api.core/clone (j/get mesh :rotation))
        scale (api.core/clone (j/get mesh :scaling))
        depth (api.core/get-node-attr mesh :depth)
        size (api.core/get-node-attr mesh :size)
        text (api.core/get-node-attr mesh :text)
        name (j/get mesh :immersa-id)
        opts (merge
               {:mat material
                :depth depth
                :size size
                :position position
                :rotation rotation
                :scale scale
                :text text}
               data)
        opts (cond
               (<= (:depth data) 0)
               (assoc opts :depth 0.01)

               (<= (:size data) 0)
               (assoc opts :size 0.1)

               :else opts)
        mesh (cond
               (:depth data) (j/assoc-in! mesh [:scaling :z] (:depth data))
               (:size data) (-> mesh
                                (j/assoc-in! [:scaling :x] (:size data))
                                (j/assoc-in! [:scaling :y] (:size data)))
               (:text data) (do
                              (api.core/dispose name)
                              (api.mesh/text name opts)))]
    (when (:size data)
      (update-slide-data mesh :scale [(:size opts)
                                      (:size opts)
                                      (j/get-in mesh [:scaling :z])]))
    (when (:depth data)
      (update-slide-data mesh :scale [(j/get-in mesh [:scaling :x])
                                      (j/get-in mesh [:scaling :y])
                                      (:depth opts)]))
    (when (:text data)
      (update-slide-data mesh :text (:text opts))
      (j/call-in api.core/db [:gizmo :manager :attachToMesh] mesh))))

(def update-text-mesh-with-debounce (functions/debounce update-text-mesh 500))

(defn camera-locked? []
  (get-slide-data :camera :locked?))

(defn- capture-thumbnail-changes []
  (let [upload-presentation-with-debounce (functions/debounce firebase/upload-presentation 2000)]
    (add-watch all-slides :slide-update
               (fn [_ _ old-val new-val]
                 (when-not (= old-val new-val)
                   (let [user-id (j/get-in api.core/db [:user :id])
                         presentation-id (j/get-in api.core/db [:presentation :id])]
                     (when (and user-id presentation-id)
                       (upload-presentation-with-debounce {:user-id user-id
                                                           :presentation-id presentation-id
                                                           :presentation-data new-val})))
                   (swap! thumbnails assoc :last-time-slide-updated (js/Date.now)))))))

(defn- next-prev-slide-event-listener [e]
  (when-not (j/get e :repeat)
    (cond
      (or (= (.-keyCode e) 39)
          (= (.-keyCode e) 40))
      (process-next-prev-command
        {:type :next
         :ch command-ch
         :slide-in-progress? slide-in-progress?
         :current-running-anims current-running-anims})
      (or (= (.-keyCode e) 37)
          (= (.-keyCode e) 38))
      (process-next-prev-command
        {:type :prev
         :ch command-ch
         :slide-in-progress? slide-in-progress?
         :current-running-anims current-running-anims}))))

(defn add-listeners-for-present-mode []
  (let [slide-controls (js/document.getElementById "slide-controls")
        prev-button (j/get-in slide-controls [:children 0])
        next-button (j/get-in slide-controls [:children 2])]
    (common.utils/register-event-listener prev-button "click"
      (fn [e]
        (when-not (j/get e :repeat)
          (process-next-prev-command
            {:type :prev
             :ch command-ch
             :slide-in-progress? slide-in-progress?
             :current-running-anims current-running-anims}))))
    (common.utils/register-event-listener next-button "click"
      (fn [e]
        (when-not (j/get e :repeat)
          (process-next-prev-command
            {:type :next
             :ch command-ch
             :slide-in-progress? slide-in-progress?
             :current-running-anims current-running-anims}))))
    (common.utils/remove-element-listener js/window "keydown" next-prev-slide-event-listener)
    (common.utils/register-event-listener js/window "keydown" next-prev-slide-event-listener)))

(defn- hide-loading-screen [mode]
  (dispatch [::main.events/hide-loading-screen])
  (api.core/resize)
  (when (= mode :editor)
    (dispatch [::editor.events/scene-ready]))
  (when (= mode :present)
    (dispatch [::events/scene-ready])))

(defn start-slide-show [{:keys [mode slides] :as opts}]
  (let [_ (init-slide-show-state)
        slides (reset! all-slides slides)
        slides (get-slides slides)
        _ (reset! thumbnails {:thumbnails (:thumbnails opts)
                              :last-time-slide-updated (js/Date.now)
                              :last-time-thumbnail-updated (js/Date.now)})]
    (api.camera/reset-camera (-> slides first :data :camera :position)
                             (-> slides first :data :camera :rotation))
    (pre-warm-the-scene slides)
    (prepare-first-skybox slides)
    (capture-thumbnail-changes)
    (a/put! command-ch :next)
    (when (= mode :present)
      (api.camera/detach-control (api.camera/active-camera))
      (add-listeners-for-present-mode))
    (js/setTimeout #(hide-loading-screen mode) 500)
    (go-loop [index -1]
      (let [command (a/<! command-ch)
            current-index (case command
                            :next (inc index)
                            :prev (dec index)
                            command)
            slides (get-slides @all-slides)]
        (if (and (>= current-index 0) (< current-index (count slides)))
          (let [_ (reset! current-slide-index current-index)
                _ (notify-ui current-index (count slides))
                slide (slides current-index)
                objects-data (:data slide)
                ground (api.core/get-object-by-name "ground")
                _ (api.core/set-enabled ground (-> objects-data :ground :enabled? boolean))
                _ (when (= mode :editor)
                    (api.camera/toggle-camera-lock (camera-locked?))
                    (api.camera/switch-camera-if-needed (camera-locked?))
                    (ui.notifier/notify-camera-lock-state (camera-locked?))
                    (ui.notifier/notify-ground-state (-> objects-data :ground :enabled? boolean))
                    (dispatch-sync [::editor.events/set-current-slide-index current-index]))
                object-names-from-slide-info (set (conj (keys (:data slide)) :camera))
                _ (when (object-names-from-slide-info :camera)
                    (api.camera/update-active-camera))
                objects-to-create (filter #(not (api.core/get-object-by-name %)) object-names-from-slide-info)
                current-slide-object-names (-> slide :data keys set)
                [prev-slide-object-names object-names-to-dispose] (when @prev-slide
                                                                    (let [prev-slide-object-names (-> @prev-slide keys set)]
                                                                      [prev-slide-object-names
                                                                       (set/difference prev-slide-object-names current-slide-object-names #{:camera :skybox})]))
                _ (doseq [{:keys [force-finish-fn]} @disabled-component-anims]
                    (when force-finish-fn (force-finish-fn)))
                _ (reset! disabled-component-anims (doall
                                                     (for [name object-names-to-dispose]
                                                       (disable-component name))))
                _ (create-objects objects-to-create objects-data)
                animations (reduce
                             (fn [acc object-name]
                               (let [object-slide-info (get-in slide [:data object-name])]
                                 (get-animations-from-slide-info acc object-slide-info object-name)))
                             []
                             object-names-from-slide-info)
                animations-data (vals
                                  (reduce-kv
                                    (fn [acc name animations]
                                      (let [animations (mapv second animations)
                                            delay (first (keep (j/get :delay) animations))
                                            duration (first (keep (j/get :duration) animations))
                                            max-fps (apply max (map (j/get :framePerSecond) animations))
                                            target (api.core/get-object-by-name name)]
                                        (if target
                                          (assoc acc name {:target (api.core/get-object-by-name name)
                                                           :animations animations
                                                           :delay delay
                                                           :from 0
                                                           :to (* max-fps duration)})
                                          acc)))
                                    {}
                                    (group-by first animations)))
                _ (doseq [name (set/difference current-slide-object-names
                                               object-names-to-dispose
                                               (set prev-slide-object-names))]
                    (enable-component name (get-in slide [:data name])))
                anims (mapv #(api.animation/begin-direct-animation %) animations-data)
                _ (reset! slide-in-progress? true)
                _ (swap! current-running-anims #(vec (concat % anims)))
                pcs-animations (keep
                                 (fn [object-name]
                                   (let [object-slide-info (get-in slide [:data object-name])]
                                     (when (and (#{:pcs-text} (:type object-slide-info)))
                                       (api.animation/pcs-text-anim object-name object-slide-info))))
                                 object-names-from-slide-info)]
            (swap! current-running-anims #(vec (concat % pcs-animations)))
            (add-skybox-anims {:prev-slide @prev-slide
                               :objects-data objects-data
                               :current-running-anims current-running-anims
                               :skybox (:skybox objects-data)})
            (doseq [{:keys [ch]} @current-running-anims]
              (a/<! ch))
            (reset! current-running-anims [])
            (reset! prev-slide objects-data)
            (reset! slide-in-progress? false)
            (recur current-index))
          (recur index))))))
