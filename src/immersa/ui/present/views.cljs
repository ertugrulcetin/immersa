(ns immersa.ui.present.views
  (:require
    ["progressbar.js" :as ProgressBar]
    [applied-science.js-interop :as j]
    [immersa.common.utils :as common.utils]
    [immersa.scene.core :as scene.core]
    [immersa.ui.editor.subs :as editor.subs]
    [immersa.ui.icons :as icon]
    [immersa.ui.present.events :as events]
    [immersa.ui.present.styles :as styles]
    [immersa.ui.present.subs :as subs]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(defn- canvas []
  (r/create-class
    {:component-did-mount #(scene.core/start-scene (js/document.getElementById "renderCanvas")
                                                   {:mode :present
                                                    :slides @(subscribe [::editor.subs/slides-all])})
     :reagent-render (fn []
                       [:canvas
                        {:id "renderCanvas"
                         :class (styles/canvas)}])}))

(defn- canvas-container []
  (let [{:keys [width height]} @(subscribe [::subs/calculated-canvas-dimensions])]
    (when (and (> width 0) (> height 0))
      [:div
       {:id "canvas-container"
        :style {:width (str width "px")
                :height (str height "px")}
        :class (styles/canvas-container)}
       (when @(subscribe [::subs/show-arrow-keys-text?])
         [:div (styles/arrow-keys-text)
          [:h1 "Use arrow keys to navigate"]
          [:div
           [icon/arrow-left {:size 32
                             :color "white"
                             :weight "bold"}]
           [icon/arrow-right {:size 32
                              :color "white"
                              :weight "bold"}]]])
       (when @(subscribe [::subs/show-pre-warm-text?])
         [:div (styles/arrow-keys-text)
          [:h1 "Pre-warming scene..."]])
       [canvas]])))

(defn- progress-bar-line []
  [:div
   {:id "progress-line"
    :class (styles/progress-line)
    :ref (fn [ref]
           (when ref
             (let [bar (ProgressBar/Line. ref
                                          #js {:strokeWidth 1
                                               :easing "easeInOut"
                                               :duration 500
                                               :color "white"
                                               :trailColor "rgb(63, 66, 80)"
                                               :trailWidth 1
                                               :svgStyle #js{:width "100%"
                                                             :height "100%"
                                                             :borderRadius "3px"}})]
               (dispatch [::events/add-progress-bar bar]))))}])

(defn- enable-wait-list [waitlist]
  (j/assoc-in! waitlist [:style :z-index] "1")
  (j/assoc-in! waitlist [:style :opacity] "1"))

(defn- disable-wait-list [waitlist]
  (j/assoc-in! waitlist [:style :z-index] "-1")
  (j/assoc-in! waitlist [:style :opacity] "0"))

(defn- wait-list-button []
  (let [init? (atom false)]
    (r/create-class
      {:component-did-mount
       (fn []
         (common.utils/register-event-listener
           js/window
           "keydown"
           (fn [e]
             (when (= (j/get e :keyCode) 27)
               (some-> (js/document.getElementById "getWaitlistContainer") disable-wait-list)))))
       :reagent-render
       (fn []
         [:button
          {:on-click (fn []
                       (js/window.open "https://immersa.app" "_blank"))
           :class [(styles/wait-list-button)
                   (styles/wait-list-button-glow)
                   (styles/wait-list-button-gradient-border)]}
          "Join Waitlist"])})))

(defn present-panel []
  [:div
   {:id "content-container"
    :class (styles/content-container)
    :style {:background @(subscribe [::subs/background-color])}}
   [canvas-container]
   [:div
    {:id "progress-bar"
     :class (styles/progress-bar)}
    [progress-bar-line]
    [:div
     {:id "progress-controls"
      :class (styles/progress-controls)}
     [:div
      {:id "slide-controls"
       :class (styles/slide-controls)}
      [icon/prev {:size 24
                  :color "white"
                  :weight "bold"
                  :cursor "pointer"}]
      [:span {:id "slide-indicator"
              :class (styles/current-slide-indicator)}
       (let [{:keys [current-slide-index slide-count]} @(subscribe [::subs/slide-info])]
         (str current-slide-index " / " slide-count))]
      [icon/next {:size 24
                  :color "white"
                  :weight "bold"
                  :cursor "pointer"}]]
     [:div {:id "right-controls"
            :class (styles/right-controls)}
      [wait-list-button]
      [icon/control {:size 24
                     :color "white"
                     :cursor "pointer"}]
      [icon/smiley {:size 24
                    :color "white"
                    :cursor "pointer"}]
      [icon/chat {:size 24
                  :color "white"
                  :cursor "pointer"}]
      [icon/dots {:size 24
                  :color "white"
                  :weight "bold"
                  :cursor "pointer"}]]]]])
