(ns immersa.ui.editor.views
  (:require
    ["@radix-ui/react-scroll-area" :as ScrollArea]
    [applied-science.js-interop :as j]
    [goog.functions :as functions]
    [immersa.scene.core :as scene.core]
    [immersa.ui.editor.components.button :refer [button]]
    [immersa.ui.editor.events :as event]
    [immersa.ui.editor.styles :as styles]
    [immersa.ui.editor.subs :as subs]
    [immersa.ui.icons :as icon]
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(defn- canvas []
  (r/create-class
    {:component-did-mount #(scene.core/start-scene
                             (js/document.getElementById "renderCanvas")
                             :start-slide-show? false)
     :reagent-render (fn []
                       [:canvas
                        {:id "renderCanvas"
                         :class (styles/canvas)}])}))

(defn- canvas-container []
  (let [{:keys [width height]} @(subscribe [::subs/calculated-canvas-wrapper-dimensions])]
    (when (and width height (> width 0) (> height 0))
      [:div
       {:id "canvas-container"
        :style {:width (str width "px")
                :height (str height "px")}
        :class (styles/canvas-container)}
       [canvas]])))

(defn- canvas-wrapper []
  (let [ref (r/atom nil)
        on-resize (functions/debounce
                    (fn [entries]
                      ;; TODO resize the BabylonJS
                      (doseq [e entries]
                        (dispatch [::event/set-canvas-wrapper-dimensions
                                   (j/get-in e [:contentRect :width])
                                   (j/get-in e [:contentRect :height])])))
                    200)]
    (r/create-class
      {:component-did-mount (fn []
                              (when @ref
                                (let [observer (js/ResizeObserver. on-resize)]
                                  (j/call observer :observe @ref))))
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
    [icon/list {:size 24
                :color colors/text-primary}]]
   [:div (styles/title-bar-full-width)
    [:div (styles/title-container)
     [:span (styles/title-label) "My 3D Presentation"]
     [:div (styles/private-badge)
      [icon/lock {:size 12}]
      [:span (styles/private-badge-label) "Private"]]]]])

(defn presentation-component [{:keys [icon class text]}]
  [:div {:class [(styles/presentation-component) class]}
   [icon {:size 24
          :color colors/text-primary}]
   [:span (styles/presentation-component-label) text]])

(defn- header-center-panel []
  [:div (styles/header-center-panel)
   [presentation-component {:icon icon/text
                            :text "Text"}]
   [presentation-component {:icon icon/image
                            :text "Image"}]
   [presentation-component {:icon icon/cube
                            :text "3D Model"
                            :class (styles/presentation-component-cube)}]
   [presentation-component {:icon icon/light
                            :text "Light"}]])

(defn- header-right-panel []
  [:div (styles/header-right)
   [:div (styles/header-right-container)
    [button {:text "Present"
             :type :outline
             :icon-left [icon/play {:size 18
                                    :weight "fill"
                                    :color colors/button-outline-text}]}]
    [button {:text "Share"
             :type :regular
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
    [:div (styles/side-bar)
     [:div
      {:style {:display "flex"
               :align-items "center"
               :padding "8px 16px 0 16px"}}
      [button {:text "Add slide"
               :class (styles/add-slide-button)
               :icon-left [icon/plus {:size 18
                                      :color colors/text-primary}]}]]
     [:> ScrollArea/Root
      {:style {:width "170px"
               :height "100%"
               :border-radius "4px"
               :overflow "hidden"
               ;; :background-color "white"
               :--scrollbar-size "10px"}}
      [:> ScrollArea/Viewport
       {:style {:width "100%"
                :height "100%"
                :border-radius "inherit"}}
       [:div {:style {:padding-top "8px"}}
        (for [i (range 1 15)]
          [:div {:style {:display "flex"
                         :align-items "flex-start"
                         :padding-left "8px"
                         :padding-bottom "8px"}}
           [:span {:style {:width "22px"
                           :color (if (= i 1)
                                    colors/button-outline-border
                                    colors/text-primary)
                           :font-size typography/s
                           :font-weight (if (= i 1)
                                          typography/medium
                                          typography/regular)}} i]
           [:div
            {:style {:width "123px"
                     :height "70px"
                     :border-radius "5px"
                     :border (if (= i 1)
                               (str "2px solid " colors/button-outline-border)
                               (str "1px solid " colors/border2))}}]])]]
      [:> ScrollArea/Scrollbar
       {:orientation "vertical"}
       [:> ScrollArea/Thumb]]
      [:> ScrollArea/Corner]]
     #_[:div {:style {:display "flex"
                      :flex-direction "column"
                      :gap "16px"
                      :padding-top "16px"
                      :padding-bottom "16px"
                      :overflow-y "auto"}}
        (for [i (range 1 15)]
          [:div {:style {:display "flex"
                         :align-items "flex-start"}}
           [:span {:style {:padding "0px 8px 0px 8px"
                           :color (if (= i 1)
                                    colors/button-outline-border
                                    colors/text-primary)
                           :font-size typography/s
                           :font-weight (if (= i 1)
                                          typography/medium
                                          typography/regular)}} i]
           [:div
            {:style {:width "123px"
                     :height "70px"
                     :border-radius "5px"
                     :border (if (= i 1)
                               (str "2px solid " colors/button-outline-border)
                               (str "1px solid " colors/border2))}}]])]]
    [canvas-wrapper]
    [:div (styles/options-bar)]]])
