(ns immersa.scene.api.core
  (:refer-clojure :exclude [clone])
  (:require
    ["@babylonjs/core/Actions/actionManager" :refer [ActionManager]]
    ["@babylonjs/core/Actions/directActions" :refer [ExecuteCodeAction]]
    ["@babylonjs/core/Buffers/buffer" :refer [VertexBuffer]]
    ["@babylonjs/core/Debug/debugLayer"]
    ["@babylonjs/core/Engines/engine" :refer [Engine]]
    ["@babylonjs/core/Helpers/sceneHelpers"]
    ["@babylonjs/core/Layers/glowLayer" :refer [GlowLayer]]
    ["@babylonjs/core/Layers/highlightLayer" :refer [HighlightLayer]]
    ["@babylonjs/core/Loading/loadingScreen"]
    ["@babylonjs/core/Loading/sceneLoader" :refer [SceneLoader]]
    ["@babylonjs/core/Materials/Node/Blocks"]
    ["@babylonjs/core/Materials/Textures/Loaders/envTextureLoader"]
    ["@babylonjs/core/Materials/Textures/cubeTexture" :refer [CubeTexture]]
    ["@babylonjs/core/Materials/Textures/dynamicTexture" :refer [DynamicTexture]]
    ["@babylonjs/core/Materials/Textures/texture" :refer [Texture]]
    ["@babylonjs/core/Maths/math" :refer [Vector2 Vector3 Vector4]]
    ["@babylonjs/core/Maths/math.color" :refer [Color3 Color4]]
    ["@babylonjs/core/Maths/math.scalar" :refer [Scalar]]
    ["@babylonjs/core/Meshes/mesh" :refer [Mesh]]
    ["@babylonjs/core/Meshes/transformNode" :refer [TransformNode]]
    ["@babylonjs/core/Misc/assetsManager" :refer [AssetsManager]]
    ["@babylonjs/core/Misc/tools" :refer [Tools]]
    ["@babylonjs/core/Particles/pointsCloudSystem" :refer [PointsCloudSystem]]
    ["@babylonjs/core/scene" :refer [Scene]]
    ["@babylonjs/loaders"]
    ;; ["@babylonjs/inspector"]
    ["p5" :as p5]
    [applied-science.js-interop :as j]
    [cljs.core.async :as a]
    [clojure.walk :as walk]
    [immersa.scene.api.assets :refer [assets]])
  (:require-macros
    [immersa.scene.macros :as m]))

(defonce db #js {})

(comment
  (require '["@babylonjs/inspector"])
  (show-debug))

(defn create-engine [canvas]
  (let [e (Engine. canvas true #js {:preserveDrawingBuffer true
                                    :stencil true
                                    :adaptToDeviceRatio true})]
    (j/assoc! db :engine e :canvas canvas)
    (j/assoc! js/window :engine e)
    e))

(defn get-engine []
  (j/get db :engine))

(defn create-scene [engine]
  (let [s (Scene. engine)]
    (j/assoc! db :scene s)
    s))

(defn canvas []
  (j/get db :canvas))

(defn get-scene []
  (j/get db :scene))

(defn get-elapsed-time []
  (j/get db :elapsed-time))

(defn v2
  ([]
   (Vector2.))
  ([n]
   (Vector2. n n))
  ([x z]
   (Vector2. x z)))

(defn v3
  ([]
   (Vector3.))
  ([n]
   (Vector3. n n n))
  ([x y z]
   (Vector3. x y z)))

(defn v4
  ([]
   (Vector4.))
  ([n]
   (Vector4. n n n n))
  ([x y z w]
   (Vector4. x y z w)))

(defn v3? [v]
  (instance? Vector3 v))

(defn clone [obj]
  (j/call obj :clone))

(defn set-v2 [v x y]
  (j/call v :set x y))

(defn set-v3 [v x y z]
  (j/call v :set x y z))

(defn v3->v [v3]
  [(j/get v3 :x) (j/get v3 :y) (j/get v3 :z)])

(defn v->v3 [[x y z :as v]]
  (v3 x y z))

(defn equals? [v1 v2]
  (j/call v1 :equals v2))

(defn get-delta-time []
  (/ (j/call-in db [:engine :getDeltaTime]) 1000))

(defn color
  ([c]
   (color c c c))
  ([r g b]
   (Color3. r g b))
  ([r g b a]
   (Color4. r g b a)))

(defn color-rgb
  ([c]
   (color-rgb c c c))
  ([r g b]
   (j/call Color3 :FromInts r g b))
  ([r g b a]
   (j/call Color4 :FromInts r g b a)))

(defn color->color-rgb [[r g b]]
  [(* 255.0 r) (* 255.0 g) (* 255.0 b)])

(defn color->v [c]
  (let [r (j/get c :r)
        g (j/get c :g)
        b (j/get c :b)]
    [(* 255.0 r) (* 255.0 g) (* 255.0 b)]))

(defn get-node-by-name [name]
  (j/get-in db [:nodes name]))

(defn get-node-attr [mesh-or-name & ks]
  (j/get-in db (concat [:nodes (if (string? mesh-or-name)
                                 mesh-or-name
                                 (j/get mesh-or-name :immersa-id))] ks)))

(defn get-object-name [mesh]
  (j/get mesh :immersa-id))

(defn get-object-by-name [name]
  (j/get-in db [:nodes name :obj]))

(defn get-object-type-by-name [name]
  (j/get-in db [:nodes name :type]))

(defn get-objects-by-type [type]
  (map (j/get :obj) (filter #(= (j/get % :type) type) (js/Object.values (j/get db :nodes)))))

(defn dispose [& name-or-obj]
  (doseq [o name-or-obj]
    (when-let [obj (if (string? o)
                     (get-object-by-name o)
                     o)]
      (let [obj-name (j/get obj :name)
            children (j/get-in db [:nodes obj-name :children])]
        (j/call obj :dispose)
        (js-delete (j/get db :nodes) obj-name)
        (doseq [c children]
          (dispose c))))))

(defn set-enabled [obj enabled?]
  (j/call obj :setEnabled enabled?))

(defn dispose-all [objs]
  (apply dispose objs))

(defn- remove-if-exists [name]
  (when-let [obj (j/get-in db [:nodes name :obj])]
    (dispose obj)))

(defn add-node-to-db [name obj opts]
  (remove-if-exists name)
  (j/assoc! obj :immersa-id name)
  (j/assoc-in! db [:nodes name] (clj->js (assoc opts :obj obj :name name)))
  obj)

(defn add-prop-to-db [name & ks]
  (doseq [[prop val] (partition 2 ks)]
    (j/assoc-in! db [:nodes name prop] (if (keyword? val)
                                         (cljs.core/name val)
                                         val))))

(defn get-pos [obj]
  (j/call obj :getAbsolutePosition))

(defn set-pos [obj v3]
  (j/call obj :setPosition v3))

(defn update-pos [obj v3]
  (j/assoc! obj :position v3))

(defn create-action-manager [obj]
  (let [am (ActionManager.)]
    (j/assoc! obj :actionManager am)
    am))

(defn register-action [action-manager params callback]
  (j/call action-manager :registerAction (ExecuteCodeAction.
                                           (if (keyword? params)
                                             (j/get ActionManager (name params))
                                             (clj->js (update params :trigger #(j/get ActionManager (name %)))))
                                           callback)))

(defn texture [path & {:keys [u-scale
                              v-scale
                              on-load
                              on-error]}]
  (let [tex (Texture. path)]
    (m/cond-doto tex
      u-scale (j/assoc! :uScale u-scale)
      v-scale (j/assoc! :vScale v-scale)
      on-load (j/call-in [:onLoadObservable :add] on-load)
      on-error (j/assoc! :onError on-error))))

(defn register-on-before-render [f]
  (j/call-in db [:scene :onBeforeRenderObservable :add] f))

(defn register-on-after-render [f]
  (j/call-in db [:scene :onAfterRenderObservable :add] f))

(defn get-advanced-texture []
  (j/get db :gui-advanced-texture))

(defn import-mesh [file f]
  (j/call SceneLoader :ImportMesh "" "models/" file (j/get db :scene) f))

(defn normalize [v]
  (j/call v :normalize))

(defn scale [v n]
  (j/call v :scale n))

(defn to-rad [angle]
  (j/call Tools :ToRadians angle))

(defn to-deg [angle]
  (j/call Tools :ToDegrees angle))

(defn dispose-engine []
  (j/call-in db [:engine :dispose])
  (set! db #js {}))

(defn show-debug []
  (let [p (j/call-in db [:scene :debugLayer :show] #js {:embedMode true})]
    (.then p #(j/assoc-in! (js/document.getElementById "embed-host") [:style :position] "absolute"))))

(defn hide-debug []
  (j/call-in db [:scene :debugLayer :hide]))

(defn dynamic-texture [name & {:keys [resolution
                                      width
                                      height
                                      alpha?
                                      generate-mipmaps?]
                               :or {resolution 1024
                                    generate-mipmaps? true}}]
  (let [[width height] (if resolution
                         [resolution (/ resolution 2)]
                         [width height])
        texture (DynamicTexture. name #js {:width width
                                           :height height
                                           :generateMipMaps generate-mipmaps?})]
    (m/cond-doto texture
      (some? alpha?) (j/assoc! :hasAlpha alpha?))))

(defn clear-scene-color [color]
  (j/assoc-in! db [:scene :clearColor] color))

(defn highlight-layer [name & {:keys [blur-horizontal-size
                                      blur-vertical-size
                                      main-texture-ratio
                                      stroke?
                                      inner-glow?
                                      outer-glow?]}]
  (let [opts (cond-> #js {}
               (some? stroke?) (j/assoc! :isStroke stroke?)
               main-texture-ratio (j/assoc! :mainTextureRatio main-texture-ratio))]
    (m/cond-doto (HighlightLayer. name (get-scene) opts)
      blur-horizontal-size (j/assoc! :blurHorizontalSize blur-horizontal-size)
      blur-vertical-size (j/assoc! :blurVerticalSize blur-vertical-size)
      (some? inner-glow?) (j/assoc! :innerGlow inner-glow?)
      (some? outer-glow?) (j/assoc! :outerGlow outer-glow?))))

(defn glow-layer [name & {:keys [main-texture-samples
                                 main-texture-fixed-size
                                 blur-kernel-size
                                 intensity]
                          :or {main-texture-samples 1
                               blur-kernel-size 32}}]
  (let [gl (GlowLayer. name (j/get db :scene) #js {:mainTextureSamples main-texture-samples
                                                   :mainTextureFixedSize main-texture-fixed-size
                                                   :blurKernelSize blur-kernel-size})]
    (m/cond-doto gl
      intensity (j/assoc! :intensity intensity))))

(defn transform-node [name & {:keys [position
                                     rotation
                                     scale]
                              :as opts}]
  (let [tn (TransformNode. name)]
    (add-node-to-db name tn opts)
    (m/cond-doto tn
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (j/assoc! :scaling scale))))

(defn mesh [name & {:keys [position
                           rotation
                           scale
                           visibility]
                    :as opts}]
  (let [tn (Mesh. name)]
    (add-node-to-db name tn opts)
    (m/cond-doto tn
      position (j/assoc! :position position)
      visibility (j/assoc! :visibility visibility)
      rotation (j/assoc! :rotation rotation)
      scale (j/assoc! :scaling scale))))

(defn add-children [parent & children]
  (add-prop-to-db (j/get parent :name) :children children)
  (doseq [c children]
    (j/assoc! c :parent parent)))

(defn point-cloud-system [name & {:keys [point-size
                                         point-count
                                         on-add-point
                                         on-build-done
                                         build-mesh?]
                                  :or {build-mesh? true
                                       point-size 1}
                                  :as opts}]
  (let [pcs (PointsCloudSystem. name point-size)]
    (add-node-to-db name pcs opts)
    (m/cond-doto pcs
      on-add-point (j/call :addPoints point-count on-add-point))
    (when build-mesh?
      (let [p (j/call pcs :buildMeshAsync)]
        (when on-build-done
          (j/call p :then on-build-done))))
    pcs))

(defn register-before-render-fn [name f]
  (j/assoc-in! db [:before-render-fns name] f))

(defn remove-before-render-fn [name]
  (js-delete (j/get db :before-render-fns) name))

(defn get-before-render-fns []
  (some-> (j/get db :before-render-fns) (js/Object.values)))

(defn cube-texture [& {:keys [root-url
                              extensions
                              no-mipmaps?
                              files
                              coordinates-mode
                              on-load
                              on-error]
                       :or {root-url ""}}]
  (let [cb (CubeTexture. root-url nil extensions no-mipmaps? (clj->js files))]
    (m/cond-doto cb
      coordinates-mode (j/assoc! :coordinatesMode (j/get Texture coordinates-mode))
      on-load (j/call-in [:onLoadObservable :add] on-load)
      on-error (j/assoc! :onError on-error))))

(defn create-assets-manager [& {:keys [on-finish]}]
  (let [am (AssetsManager.)]
    (j/assoc! db :assets-manager am)
    (j/assoc! am :autoHideLoadingUI false)
    (j/assoc! am :onFinish on-finish)))

(defn add-texture-task [name url]
  (j/call-in db [:assets-manager :addTextureTask] name url))

(defn add-cube-texture-task [name url]
  (j/call-in db [:assets-manager :addCubeTextureTask] name url))

(defn add-text-task [name url]
  (let [task (j/call-in db [:assets-manager :addTextFileTask] name url)]
    (j/assoc! task :onSuccess #(j/assoc-in! db [:assets-manager :texts url] (j/get % :text)))))

(defn add-mesh-task [name meshes-names url]
  (let [task (j/call-in db [:assets-manager :addMeshTask] name meshes-names url)]
    (j/assoc! task :onSuccess (fn [task]
                                (let [mesh (j/get-in task [:loadedMeshes 0])]
                                  (set-enabled mesh false)
                                  (j/assoc-in! db [:models url] mesh)))
              :onError (fn [task]
                         (let [meshes (j/get task :loadedMeshes)]
                           (when (and meshes (> (j/get meshes :length) 0))
                             (js/console.warn "Failed meshes: " meshes)
                             (j/call meshes :forEach dispose)))))))

(defn load-async [slides]
  (let [p (a/promise-chan)
        assets (atom [])]
    (walk/prewalk
      (fn [form]
        (if (and (map? form) (:asset-type form))
          (do
            (swap! assets conj (select-keys form [:asset-type :path]))
            form)
          form))
      slides)
    (doseq [[index {:keys [asset-type path]}] (map-indexed vector (distinct @assets))]
      (case asset-type
        :text (add-text-task (str "text-" index) path)
        :texture (add-texture-task (str "texture-" index) path)
        :cube-texture (add-cube-texture-task (str "cube-texture-" index) path)
        :model (add-mesh-task (str "mesh-" index) "" path)))
    (j/call (j/call-in db [:assets-manager :loadAsync]) :then #(a/put! p true))
    p))

(defn hide-loading-ui []
  (j/call (get-engine) :hideLoadingUI))

(defn pcs [name & {:keys [point-size]
                   :or {point-size 1}
                   :as opts}]
  (let [pcs (PointsCloudSystem. name point-size)]
    (add-node-to-db name pcs (assoc opts :type :pcs))))

(defn add-points [pcs n f]
  (j/call pcs :addPoints n f))

(defn build-mesh-async [pcs on-build-done]
  (let [p (j/call pcs :buildMeshAsync)]
    (some->> on-build-done (j/call p :then))))

(defn init-p5 []
  (new p5 (fn [p5]
            (j/assoc! p5 :setup
                      (fn []
                        (some-> (js/document.getElementsByTagName "main") (j/get 0) (j/call :remove))))
            (j/assoc-in! db [:p5 :obj] p5)
            (j/assoc-in! db [:p5 :font :big-caslon] (j/call p5 :loadFont "font/BigCaslon.otf")))))

(defn get-p5 []
  (j/get-in db [:p5 :obj]))

(defn get-p5-font [font]
  (j/get-in db [:p5 :font font]))

(defn update-vertices-data [mesh positions]
  (j/call mesh :updateVerticesData (j/get VertexBuffer :PositionKind) positions))

(defn lerp [start end amount]
  (j/call Scalar :Lerp start end amount))

(defn rand-range [start end]
  (j/call Scalar :RandomRange start end))

(defn color-lerp [start end amount]
  (j/call Color3 :Lerp start end amount))

(defn get-mat-by-name [name]
  (j/call-in db [:scene :getMaterialByName] name))

(defn create-from-prefiltered-data [path]
  (j/call CubeTexture :CreateFromPrefilteredData path (get-scene)))

(defn lerp-colors [{:keys [start-color
                           end-color
                           on-lerp
                           on-end
                           targets
                           duration]
                    :or {duration 0.5}}]
  (if (and start-color end-color (not (equals? start-color end-color)))
    (let [p (a/promise-chan)
          elapsed-time (atom 0)
          fn-name (gensym "lerp-colors-")
          _ (register-before-render-fn
              fn-name
              (fn []
                (let [elapsed-time (swap! elapsed-time + (get-delta-time))
                      amount (min (/ elapsed-time duration) 1)
                      new-color (color-lerp start-color end-color amount)
                      r (j/get new-color :r)
                      g (j/get new-color :g)
                      b (j/get new-color :b)]
                  (when on-lerp (on-lerp r g b))
                  (when (seq targets)
                    (doseq [[obj prop] targets]
                      (j/assoc! obj prop (color r g b))))
                  (when (>= elapsed-time duration)
                    (remove-before-render-fn fn-name)
                    (when on-end (on-end))
                    (a/put! p true)))))]
      {:ch p
       :force-finish-fn (fn []
                          (remove-before-render-fn fn-name)
                          (when (seq targets)
                            (doseq [[obj prop] targets]
                              (j/assoc! obj prop (clone end-color))))
                          (when on-end (on-end))
                          (a/put! p true))})
    (do
      (when on-end (on-end))
      nil)))

(defn selected-mesh []
  (j/get-in db [:gizmo :selected-mesh]))

(defn clear-selected-mesh []
  (j/call-in db [:gizmo :manager :attachToMesh] nil))

(defn attach-to-mesh [mesh]
  (j/call-in db [:gizmo :manager :attachToMesh] mesh))

(defn gizmo-manager []
  (j/get-in db [:gizmo :manager]))

(defn look-at [obj target]
  (j/call obj :lookAt target))

(comment
  (j/assoc! (get-object-by-name "33e4ee76-bb27-4904-9d30-360a40d8abc1") )
  (j/call-in db [:gizmo :manager :attachToMesh] (get-object-by-name "00f4ee76-bb27-4904-9d30-360a40d8abc1")))
