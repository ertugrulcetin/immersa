(ns immersa.scene.slide
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go go-loop <!]]
    [clojure.set :as set]
    [clojure.walk :as walk]
    [com.rpl.specter :as sp]
    [goog.functions :as functions]
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
    [immersa.ui.editor.events :as editor.events]
    [immersa.ui.present.events :as events]
    [re-frame.core :refer [dispatch dispatch-sync]]))

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
  (let [end-visibility (:visibility object-slide-info)
        start-visibility (j/get (api.core/get-object-by-name object-name) :visibility)]
    (when (and end-visibility (not= start-visibility end-visibility))
      [object-name (api.animation/create-visibility-animation {:start start-visibility
                                                               :end end-visibility
                                                               :delay (:delay object-slide-info)})])))

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

(defn- parse-slides [slides]
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
  (let [slides (parse-slides slides)
        slides-vec (vec (map-indexed #(assoc %2 :index %1) slides))
        #_#_props-to-copy [:type :position :rotation :visibility]
        #_#_clone-if-exists (fn [data]
                              (let [position (:position data)
                                    rotation (:rotation data)]
                                (cond-> data
                                        (vector? position)
                                        (assoc :position (mapv api.core/clone (:position data)))

                                        (and position (not (vector? position)))
                                        (assoc :position (api.core/clone (:position data)))

                                        (vector? rotation)
                                        (assoc :rotation (mapv api.core/clone (:rotation data)))

                                        (and rotation (not (vector? rotation)))
                                        (assoc :rotation (api.core/clone (:rotation data))))))]
    ;; TODO this should be removed
    #_(reduce
        (fn [slides-vec slide]
          (let [prev-slide-data (get-in slides-vec [(dec (:index slide)) :data])
                slide-data (:data slide)]
            (conj slides-vec
                  (assoc slide :data
                               (reduce-kv
                                 (fn [acc name objet-slide-data]
                                   (if-let [prev-slide-data (get prev-slide-data name)]
                                     (assoc acc name (merge (clone-if-exists (select-keys prev-slide-data props-to-copy)) objet-slide-data))
                                     (assoc acc name objet-slide-data)))
                                 {}
                                 slide-data)))))
        [(first slides-vec)]
        (rest slides-vec))
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
  (disable-mesh-component-via-visibility name))

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
  (defn process-next-prev-command [type ch slide-in-progress? current-running-anims]
    (if (and @slide-in-progress? (not @circuit-breaker-running?))
      (do
        (reset! circuit-breaker-running? true)
        (println "Circuit breaker tripped")
        (doseq [{:keys [force-finish-fn force-finish-fn-atom]} @current-running-anims]
          (if-let [force-finish-fn (some-> force-finish-fn-atom deref)]
            (force-finish-fn)
            (force-finish-fn)))
        (reset! current-running-anims [])
        (reset! circuit-breaker-running? false))
      (a/put! ch type))))

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

(defonce thumbnails (atom {:thumbnails {}
                           :last-time-slide-updated (js/Date.now)
                           :last-time-thumbnail-updated (js/Date.now)}))

(defn- init-slide-show-state []
  (set! command-ch (a/chan (a/dropping-buffer 1)))
  (reset! current-running-anims [])
  (reset! slide-in-progress? false)
  (reset! prev-slide nil)
  (reset! current-slide-index 0))

(defn go-to-slide [index]
  (process-next-prev-command index command-ch slide-in-progress? current-running-anims))

(comment
  (a/put! command-ch 11)
  )

(defn duplicate-slide-data [[id params]]
  (let [mesh (api.core/get-object-by-name id)
        index @current-slide-index
        exists? (boolean (get-in @all-slides [index :data id]))
        id (if exists? (str (random-uuid)) id)]
    (when (and mesh (not exists?) (> (:visibility params) 0))
      (j/assoc! mesh :visibility (:visibility params))
      (api.core/set-enabled mesh true))
    (when exists?
      (let [params (->> params
                        (parse-pos-rot-scale :position)
                        (parse-pos-rot-scale :rotation)
                        (parse-pos-rot-scale :scale)
                        (parse-colors :color))]
        (case (:type params)
          :text3D (api.mesh/text id params)
          :image (api.component/image id params)
          :glb (api.mesh/glb->mesh id params))))
    (sp/setval [sp/ATOM index :data id] params all-slides)
    (sp/setval [sp/ATOM id] params prev-slide)))

(defn vec-insert [lst elem index]
  (let [[l r] (split-at index lst)]
    (vec (concat l [elem] r))))

(defn add-slide []
  (let [index @current-slide-index
        uuid (str (random-uuid))]
    (api.core/clear-selected-mesh)
    (swap! all-slides (fn [slides]
                        (vec-insert slides (assoc (get slides index) :id uuid) (inc index))))
    (swap! current-slide-index inc)
    (dispatch [::editor.events/sync-slides-info {:current-index @current-slide-index
                                                 :slides @all-slides}])))

(defn add-slide-data [obj params]
  (let [index @current-slide-index]
    (sp/setval [sp/ATOM index :data (api.core/get-object-name obj)] params all-slides)
    (sp/setval [sp/ATOM (api.core/get-object-name obj)] params prev-slide)))

(defn update-slide-data [obj k v]
  (let [index @current-slide-index
        object-id (if (keyword? obj) obj (api.core/get-object-name obj))
        path (if (vector? k)
               (apply sp/comp-paths k)
               k)]
    (sp/setval [sp/ATOM index :data object-id path] v all-slides)
    (sp/setval [sp/ATOM object-id path] v prev-slide)))

(defn update-thumbnail []
  (let [base64 (j/call-in api.core/db [:canvas :toDataURL] "image/webp" 0.2)
        index @current-slide-index
        id (get-in @all-slides [index :id])]
    (sp/setval [sp/ATOM :thumbnails id] base64 thumbnails)
    (dispatch [::editor.events/sync-thumbnails (:thumbnails @thumbnails)])))

(defn- capture-thumbnail-changes []
  (add-watch all-slides :slide-update
             (fn [_ _ old-val new-val]
               (when-not (= old-val new-val)
                 (swap! thumbnails assoc :last-time-slide-updated (js/Date.now)))))
  (go-loop []
    (<! (a/timeout 500))
    (let [{:keys [last-time-slide-updated last-time-thumbnail-updated]} @thumbnails]
      (when (> last-time-slide-updated last-time-thumbnail-updated)
        (update-thumbnail)
        (swap! thumbnails assoc :last-time-thumbnail-updated (js/Date.now))))
    (recur)))

(defn- add-listeners-for-present-mode [mode]
  (when (= mode :present)
    (api.camera/detach-control (api.camera/active-camera))
    (let [slide-controls (js/document.getElementById "slide-controls")
          prev-button (j/get-in slide-controls [:children 0])
          next-button (j/get-in slide-controls [:children 2])]
      (common.utils/register-event-listener prev-button "click"
        (fn [e]
          (when-not (j/get e :repeat)
            (process-next-prev-command :prev command-ch slide-in-progress? current-running-anims))))
      (common.utils/register-event-listener next-button "click"
        (fn [e]
          (when-not (j/get e :repeat)
            (process-next-prev-command :next command-ch slide-in-progress? current-running-anims))))
      (common.utils/register-event-listener js/window "keydown"
        (fn [e]
          (when-not (j/get e :repeat)
            (cond
              (or (= (.-keyCode e) 39)
                  (= (.-keyCode e) 40))
              (process-next-prev-command :next command-ch slide-in-progress? current-running-anims)
              (or (= (.-keyCode e) 37)
                  (= (.-keyCode e) 38))
              (process-next-prev-command :prev command-ch slide-in-progress? current-running-anims))))))))

(defn start-slide-show [{:keys [mode slides] :as opts}]
  (let [_ (init-slide-show-state)
        slides (reset! all-slides slides)
        slides (get-slides slides)
        _ (reset! thumbnails (:thumbnails opts))]
    (api.camera/reset-camera (-> slides first :data :camera :position)
                             (-> slides first :data :camera :rotation))
    (pre-warm-the-scene slides)
    (prepare-first-skybox slides)
    (capture-thumbnail-changes)
    (a/put! command-ch :next)
    (add-listeners-for-present-mode mode)
    (js/setTimeout #(api.core/hide-loading-ui) 500)
    (go-loop [index -1]
      (let [command (a/<! command-ch)
            current-index (case command
                            :next (inc index)
                            :prev (dec index)
                            command)
            slides (get-slides @all-slides)]
        (if (and (>= current-index 0) (< current-index (count slides)))
          (let [_ (reset! current-slide-index current-index)
                _ (when (= mode :present)
                    (notify-ui current-index (count slides)))
                _ (when (= mode :editor)
                    (dispatch-sync [::editor.events/set-current-slide-index current-index]))
                slide (slides current-index)
                objects-data (:data slide)
                object-names-from-slide-info (set (conj (keys (:data slide)) :camera))
                _ (when (object-names-from-slide-info :camera)
                    (api.camera/update-active-camera))
                objects-to-create (filter #(not (api.core/get-object-by-name %)) object-names-from-slide-info)
                current-slide-object-names (-> slide :data keys set)
                [prev-slide-object-names object-names-to-dispose] (when @prev-slide
                                                                    (let [prev-slide-object-names (-> @prev-slide keys set)]
                                                                      [prev-slide-object-names
                                                                       (set/difference prev-slide-object-names current-slide-object-names #{:camera :skybox})]))
                ;; TODO add anims to the current-running-anims and block here
                ;; so that the next slide is not processed until the current slide is done
                _ (doseq [name object-names-to-dispose]
                    (disable-component name))
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
            (reset! prev-slide objects-data)
            (reset! slide-in-progress? false)
            (recur current-index))
          (recur index))))))
