(ns immersa.ui.editor.views
  (:require
    ["react-color" :refer [SketchPicker]]
    [applied-science.js-interop :as j]
    [goog.functions :as functions]
    [immersa.scene.core :as scene.core]
    [immersa.ui.editor.components.button :refer [button]]
    [immersa.ui.editor.components.context-menu :refer [context-menu]]
    [immersa.ui.editor.components.dropdown :refer [dropdown dropdown-item dropdown-separator dropdown-context-menu]]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.editor.components.tooltip :refer [tooltip]]
    [immersa.ui.editor.events :as events]
    [immersa.ui.editor.options-panel.views :refer [options-panel]]
    [immersa.ui.editor.slide-panel.views :refer [slides-panel]]
    [immersa.ui.editor.styles :as styles]
    [immersa.ui.editor.subs :as subs]
    [immersa.ui.icons :as icon]
    [immersa.ui.theme.colors :as colors]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(defn- canvas [state]
  (r/create-class
    {:component-did-mount #(scene.core/start-scene (js/document.getElementById "renderCanvas")
                                                   {:mode :editor
                                                    :slides @(subscribe [::subs/slides-all])
                                                    :thumbnails @(subscribe [::subs/slides-thumbnails])})
     :reagent-render (fn []
                       [:canvas
                        {:id "renderCanvas"
                         :on-blur #(do
                                     (reset! state :blur)
                                     (dispatch [::events/update-thumbnail]))
                         :on-focus #(reset! state :focus)
                         :class (styles/canvas)}])}))

(defn- canvas-container []
  (let [state (r/atom :blur)]
    (fn []
      (let [{:keys [width height]} @(subscribe [::subs/calculated-canvas-wrapper-dimensions])
            camera-unlocked? (not @(subscribe [::subs/camera-locked?]))]
        (when (and width height (> width 0) (> height 0))
          [:div
           {:id "canvas-container"
            :style {:width (str width "px")
                    :height (str height "px")}
            :class (styles/canvas-container @state camera-unlocked?)}
           [canvas state]])))))

(defn- canvas-wrapper []
  (let [ref (r/atom nil)
        ob (atom nil)
        on-resize (functions/debounce
                    (fn [entries]
                      (doseq [e entries]
                        (dispatch [::events/set-canvas-wrapper-dimensions
                                   (j/get-in e [:contentRect :width])
                                   (j/get-in e [:contentRect :height])]))
                      (js/setTimeout #(dispatch [::events/resize-scene]) 200))
                    200)]
    (r/create-class
      {:component-did-mount (fn []
                              (println "canvas-wrapper did mount")
                              (when @ref
                                (let [observer (js/ResizeObserver. on-resize)]
                                  (reset! ob observer)
                                  (j/call observer :observe @ref))))
       :component-will-unmount #(some-> @ob (j/call :disconnect))
       :reagent-render (fn []
                         [:div
                          {:id "canvas-wrapper"
                           :ref #(reset! ref %)
                           :class (styles/canvas-wrapper)}
                          (when @ref
                            [canvas-container])])})))

(defn- header-left-panel []
  [:div (styles/title-bar)
   [:div (styles/menubar-list-icon)
    [icon/list-menu {:size 24
                     :color colors/text-primary}]]
   [:div (styles/title-bar-full-width)
    [:div (styles/title-container)
     [:span (styles/title-label) "My 3D Presentation"]
     [:div (styles/private-badge)
      [icon/lock {:size 12}]
      [tooltip
       {:trigger [text {:size :s
                        :weight :light} "Beta"]
        :content "Immersa is currently in beta"}]]]]])

(defn presentation-component [{:keys [icon class disabled? color text-weight on-click]
                               :or {disabled? false}
                               :as opts}]
  [:div {:class [(styles/presentation-component disabled?) class]
         :on-click on-click
         :disabled disabled?}
   [icon {:size 24
          :color (or color colors/text-primary)}]
   [text {:size :s
          :color (or color colors/text-primary)
          :weight (or text-weight :light)} (:text opts)]])

(defn- header-center-panel []
  [:div (styles/header-center-panel)

   [tooltip
    {:trigger [presentation-component {:icon icon/text
                                       :text "Text"
                                       :on-click #(dispatch [::events/add-text-mesh])}]
     :content "Add text"
     :shortcuts "T"}]
   #_[dropdown
      [presentation-component {:icon icon/text
                               :text "Text"}]
      [:<>
       [dropdown-item
        [text {:size :xl} "Normal text"]]
       [dropdown-separator]
       [dropdown-item
        [text {:size :xl} "Greased line text"]]
       [dropdown-separator]
       [dropdown-item
        [text {:size :xl} "Info text"]]]]
   [presentation-component {:icon icon/image
                            :text "Image"
                            :disabled? true}]
   [presentation-component {:icon icon/cube
                            :text "3D Model"
                            :class (styles/presentation-component-cube)
                            :disabled? true}]
   #_[presentation-component {:icon icon/camera
                              :text "Camera"
                              :disabled? true}]
   [presentation-component {:icon icon/student
                            :text "Tutorial"
                            :disabled? true}]
   [presentation-component {:icon icon/chats-circle
                            :text "Feedback"
                            :text-weight :regular
                            :class (styles/presentation-component-cube)
                            :color colors/button-outline-text
                            :disabled? true}]
   #_[presentation-component {:icon icon/books
                              :text "Library"
                              :disabled? true}]
   #_[presentation-component {:icon icon/light
                              :text "Light"
                              :disabled? true}]])

(defn- header-right-panel []
  [:div (styles/header-right)
   [:div (styles/header-right-container)
    [button {:text "Present"
             :type :outline
             :class (styles/present-share-width)
             :icon-left [icon/play {:size 18
                                    :weight "fill"
                                    :color colors/button-outline-text}]}]
    [button {:text "Share"
             :type :regular
             :class (styles/present-share-width)
             :icon-right [icon/share {:size 18
                                      :weight "fill"
                                      :color colors/button-text}]}]]])

(defn- header []
  [:div (styles/header-container)
   [header-left-panel]
   [header-center-panel]
   [header-right-panel]])

(defn editor-panel []
  [:div (styles/editor-container)
   [header]
   [:div (styles/content-container)
    [slides-panel]
    [canvas-wrapper]
    [options-panel]
    [context-menu]]])

(comment
  @(subscribe [::subs/editor]))
