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
    [immersa.scene.ui-listener :as ui-listener]))

(defn- mac? []
  device/isMacOs)

(defn- key-down? [info]
  (= (j/get info :type) api.const/keyboard-type-key-down))

(defn- cmd? [info]
  (or (j/get-in info [:event :metaKey])
      (= (j/get-in info [:event :key]) "Control")))

(defn- shift? [info]
  (j/get-in info [:event :shiftKey]))

(defn- delete? [info]
  (or (= (j/get-in info [:event :key]) "Backspace")
      (= (j/get-in info [:event :key]) "Delete")))

(defn- get-selected-object-data []
  (-> (api.core/selected-mesh)
      api.core/get-object-name
      (#(vector % (get-in @slide/all-slides [@slide/current-slide-index :data %])))))

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
                    (and (= key "backspace") (api.core/selected-mesh)))
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
                       (and (cmd? info) (= key "d") (api.core/selected-mesh)))
               :action (fn []
                         (try
                           (api.core/attach-to-mesh (slide/duplicate-slide-data (get-selected-object-data)))
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
                                               (js/console.warn "Clipboard data is not in EDN format.")
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
                           :pred (fn [_ key]
                                   (and (= key "1") (api.core/selected-mesh)))
                           :action #(api.gizmo/toggle-gizmo :position)}
   :toggle-rotation-gizmo {:label "Toggle rotation gizmo"
                           :shortcut ["2"]
                           :pred (fn [_ key]
                                   (and (= key "2") (api.core/selected-mesh)))
                           :action #(api.gizmo/toggle-gizmo :rotation)}
   :toggle-scale-gizmo {:label "Toggle scale gizmo"
                        :shortcut ["3"]
                        :pred (fn [_ key]
                                (and (= key "3")
                                     (api.core/selected-mesh)
                                     (not= (api.core/selected-mesh-type) "text3D")))
                        :action #(api.gizmo/toggle-gizmo :scale)}
   :add-slide {:label "Add slide"
               :shortcut ["n"]
               :pred (fn [_ key]
                       (= key "n"))
               :action #(ui-listener/handle-ui-update {:type :add-slide})}
   :delete-slide {:label "Delete slide"
                  :shortcut ["⌘" "⌫"]
                  :pred (fn [info _]
                          (and (cmd? info) (delete? info)))
                  :action #(slide/delete-slide)}
   :camera-reset-to-initials {:label "Reset camera to initials"
                              :shortcut ["shift" "r"]
                              :pred (fn [info key]
                                      (and (shift? info) (= "r" key)))
                              :action #(fn [])}
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

(defn process [info key]
  (doseq [[_ {:keys [pred action prevent-default?]}] shortcuts]
    (when (and (key-down? info) (pred info key))
      (when prevent-default?
        (j/call-in info [:event :preventDefault]))
      (action info))))
