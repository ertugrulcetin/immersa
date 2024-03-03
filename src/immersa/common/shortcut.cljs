(ns immersa.common.shortcut
  (:require
    ["react-device-detect" :as device]
    [applied-science.js-interop :as j]
    [cljs.reader :as reader]
    [clojure.string :as str]
    [com.rpl.specter :as sp]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.animation :as api.anim]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core]
    [immersa.scene.api.gizmo :as api.gizmo]
    [immersa.scene.slide :as slide]
    [immersa.scene.ui-listener :as ui-listener]
    [immersa.scene.ui-notifier :as ui.notifier]))

(defn- mac? []
  device/isMacOs)

(defn- key-down? [info]
  (= (j/get info :type) api.const/keyboard-type-key-down))

(defn- cmd? [info]
  (or (j/get-in info [:event :metaKey])
      (j/get-in info [:event :ctrlKey])))

(defn- shift? [info]
  (j/get-in info [:event :shiftKey]))

(defn- delete? [info]
  (or (= (j/get-in info [:event :key]) "Backspace")
      (= (j/get-in info [:event :key]) "Delete")))

(defn- get-selected-object-data []
  (-> (api.core/selected-mesh)
      api.core/get-object-name
      (#(vector % (get-in @slide/all-slides [@slide/current-slide-index :data %])))))

(defn- reset-position [mesh]
  (let [position (slide/get-slide-data mesh :initial-position)]
    (j/assoc! mesh :position (api.core/v->v3 position))
    (slide/update-slide-data mesh :position position)))

(defn- reset-rotation [mesh]
  (let [rotation (slide/get-slide-data mesh :initial-rotation)]
    (j/assoc! mesh :rotation (api.core/v->v3 rotation))
    (slide/update-slide-data mesh :rotation rotation)))

(defn- reset-scale [mesh]
  (let [scale (slide/get-slide-data mesh :initial-scale)]
    (j/assoc! mesh :scaling (api.core/v->v3 scale))
    (slide/update-slide-data mesh :scale scale)))

(def shortcuts
  {:focus {:label "Focus"
           :shortcut ["f"]
           :pred (fn [_ key]
                   (and (= key "f") (api.core/selected-mesh)))
           ;; TODO should not focus when camera is locked
           :action #(api.anim/run-camera-focus-anim (api.core/selected-mesh) (slide/camera-locked?))}
   :delete {:label "Delete"
            :shortcut ["⌫"]
            :pred (fn [_ key]
                    (or (and (= key "backspace") (api.core/selected-mesh))
                        (and (= key "delete") (api.core/selected-mesh))))
            :action (fn []
                      (let [obj (api.core/selected-mesh)
                            id (api.core/get-object-name obj)
                            current-index @slide/current-slide-index]
                        (api.core/clear-selected-mesh)
                        (sp/setval [sp/ATOM current-index :data id] sp/NONE slide/all-slides)
                        (sp/setval [sp/ATOM id] sp/NONE slide/prev-slide)
                        (api.core/set-enabled obj false)))}
   :duplicate {:label "Duplicate"
               :shortcut ["⌘" "d"]
               :prevent-default? true
               :pred (fn [info key]
                       (and (cmd? info) (= key "d")))
               :action (fn []
                         (try
                           (when (api.core/selected-mesh)
                             (api.core/attach-to-mesh (slide/duplicate-slide-data (get-selected-object-data))))
                           (catch js/Error e
                             (js/console.warn "Duplicate failed.")
                             (js/console.warn e))))}
   :copy {:label "Copy"
          :shortcut ["⌘" "c"]
          :pred (fn [info key]
                  (and (cmd? info) (= key "c") (api.core/selected-mesh)))
          :action #(common.utils/copy-to-clipboard (get-selected-object-data))}
   :paste {:label "Paste"
           :shortcut ["⌘" "v"]
           :pred (fn [info key]
                   (and (cmd? info) (= key "v")))
           :action (fn []
                     (-> (j/call-in js/navigator [:clipboard :readText])
                         (j/call :then (fn [text]
                                         (when-not (str/blank? text)
                                           (try
                                             (slide/duplicate-slide-data (reader/read-string text))
                                             (catch js/Error e
                                               (js/console.error "Clipboard data is not in EDN format.")
                                               (js/console.warn e))))))
                         (j/call :catch (fn []
                                          (js/console.error "Clipboard failed.")))))}
   :escape {:label "Escape"
            :shortcut ["escape"]
            :pred (fn [_ key]
                    (and (= key "escape") (api.core/selected-mesh)))
            :action #(api.core/clear-selected-mesh)}
   :add-text {:label "Add text"
              :shortcut ["t"]
              :pred (fn [_ key]
                      (= key "t"))
              :action #(ui-listener/handle-ui-update {:type :add-text-mesh})}
   :toggle-position-gizmo {:label "Toggle position gizmo"
                           :shortcut ["1"]
                           :pred (fn [info key]
                                   (and (= key "1") (not (cmd? info)) (api.core/selected-mesh)))
                           :action #(api.gizmo/toggle-gizmo :position)}
   :toggle-rotation-gizmo {:label "Toggle rotation gizmo"
                           :shortcut ["2"]
                           :pred (fn [info key]
                                   (and (= key "2")
                                        (not (cmd? info))
                                        (api.core/selected-mesh)
                                        (not (api.core/get-node-attr (api.core/selected-mesh) :face-to-screen?))))
                           :action #(api.gizmo/toggle-gizmo :rotation)}
   :toggle-scale-gizmo {:label "Toggle scale gizmo"
                        :shortcut ["3"]
                        :pred (fn [info key]
                                (and (= key "3")
                                     (not (cmd? info))
                                     (api.core/selected-mesh)
                                     (not= (api.core/selected-mesh-type) "text3D")))
                        :action #(api.gizmo/toggle-gizmo :scale)}
   :add-slide {:label "Duplicate slide"
               :shortcut ["⌘" "d"]
               :prevent-default? true
               :ui-only? true
               :pred (fn [info key]
                       (and (cmd? info) (= key "d")))
               :action #(ui-listener/handle-ui-update {:type :add-slide})}
   :blank-slide {:label "Blank slide"
                 :shortcut ["shift" "n"]
                 :prevent-default? true
                 :pred (fn [info key]
                         (and (shift? info) (= key "n")))
                 :action #(ui-listener/handle-ui-update {:type :blank-slide})}
   :delete-slide {:label "Delete slide"
                  :shortcut ["⌫"]
                  :ui-only? true
                  :pred (fn [info _]
                          (delete? info))
                  :action #(slide/delete-slide)}
   :camera-reset-to-initials {:label "Reset camera to initials"
                              :shortcut ["shift" "c"]
                              :pred (fn [info key]
                                      (and (shift? info) (= "c" key)))
                              :action (fn []
                                        (let [initial-position (slide/get-slide-data :camera :initial-position)
                                              initial-rotation (slide/get-slide-data :camera :initial-rotation)
                                              free-camera (api.core/get-object-by-name "free-camera")]
                                          (j/assoc! free-camera :position (api.core/v->v3 initial-position))
                                          (j/assoc! free-camera :rotation (api.core/v->v3 initial-rotation))
                                          (j/assoc! (api.core/get-scene) :activeCamera free-camera)))}
   :reset-position {:label "Reset position"
                    :shortcut ["shift" "p"]
                    :pred (fn [info key]
                            (and (shift? info) (= "p" key)))
                    :action (fn []
                              (let [mesh (api.core/selected-mesh)]
                                (reset-position mesh)
                                (ui.notifier/notify-ui-selected-mesh mesh)))}
   :reset-rotation {:label "Reset rotation"
                    :shortcut ["shift" "r"]
                    :pred (fn [info key]
                            (and (shift? info) (= "r" key)))
                    :action (fn []
                              (let [mesh (api.core/selected-mesh)]
                                (reset-rotation mesh)
                                (ui.notifier/notify-ui-selected-mesh mesh)))}
   :reset-scale {:label "Reset scale"
                 :shortcut ["shift" "s"]
                 :pred (fn [info key]
                         (and (shift? info) (= "s" key)))
                 :action (fn []
                           (let [mesh (api.core/selected-mesh)]
                             (reset-scale mesh)
                             (ui.notifier/notify-ui-selected-mesh mesh)))}
   :reset-initials {:label "Reset initials"
                    :shortcut ["shift" "i"]
                    :pred (fn [info key]
                            (and (shift? info) (= "i" key)))
                    :action (fn []
                              (let [mesh (api.core/selected-mesh)]
                                (reset-position mesh)
                                (reset-rotation mesh)
                                (when-not (= "text3D" (api.core/selected-mesh-type))
                                  (reset-scale mesh))
                                (ui.notifier/notify-ui-selected-mesh mesh)))}
   :toggle-camera-lock {:label "Toggle camera lock"
                        :shortcut ["shift" "l"]
                        :pred (fn [info key]
                                (and (shift? info) (= "l" key)))
                        :action #(ui-listener/handle-ui-update
                                   {:type :toggle-camera-lock
                                    :data {:value (not (slide/get-slide-data :camera :locked?))}})}})

(defn get-shortcut-key-labels [type]
  (mapv
    (fn [s]
      (str/capitalize
        (case s
          "⌘" (if (mac?) "⌘" "Ctrl")
          "⌫" (if (mac?) "⌫" "Delete")
          s)))
    (get-in shortcuts [type :shortcut])))

(defn call-shortcut-action [type]
  (when-let [f (get-in shortcuts [type :action])]
    (f)))

(defn call-shortcut-action-with-event [type event]
  (let [f (get-in shortcuts [type :action])
        pred (get-in shortcuts [type :pred])
        prevent-default? (get-in shortcuts [type :prevent-default?])]
    (when (pred #js {:event event} (some-> (j/get event :key) str/lower-case))
      (when prevent-default?
        (.preventDefault event))
      (f)
      true)))

(defn process [info key]
  (doseq [[_ {:keys [pred action prevent-default? ui-only?]}] shortcuts]
    (when (and (key-down? info) (pred info key) (not ui-only?))
      (when prevent-default?
        (j/call-in info [:event :preventDefault]))
      (action info))))
