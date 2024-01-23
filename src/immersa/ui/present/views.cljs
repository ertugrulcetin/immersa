(ns immersa.ui.present.views
  (:require
    ["@phosphor-icons/react" :refer [CaretLeft
                                     CaretRight
                                     DotsThreeVertical
                                     ChatCenteredText
                                     SmileySticker
                                     Command
                                     ArrowFatRight
                                     ArrowFatLeft]]
    ["progressbar.js" :as ProgressBar]
    [applied-science.js-interop :as j]
    [immersa.common.utils :as common.utils]
    [immersa.scene.core :as scene.core]
    [immersa.ui.present.events :as events]
    [immersa.ui.present.styles :as styles]
    [immersa.ui.present.subs :as subs]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(def icon-prev (r/adapt-react-class CaretLeft))
(def icon-next (r/adapt-react-class CaretRight))
(def icon-dots (r/adapt-react-class DotsThreeVertical))
(def icon-chat (r/adapt-react-class ChatCenteredText))
(def icon-smiley (r/adapt-react-class SmileySticker))
(def icon-control (r/adapt-react-class Command))
(def icon-arrow-left (r/adapt-react-class ArrowFatLeft))
(def icon-arrow-right (r/adapt-react-class ArrowFatRight))

(defn- canvas []
  (r/create-class
    {:component-did-mount #(scene.core/start-scene (js/document.getElementById "renderCanvas"))
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
           [icon-arrow-left {:size 32
                             :color "white"
                             :weight "bold"}]
           [icon-arrow-right {:size 32
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
                       (when-let [waitlist (js/document.getElementById "getWaitlistContainer")]
                         (when-not @init?
                           (reset! init? true)
                           (js/setTimeout
                             (fn []
                               (some-> (js/document.getElementById "primaryCTA")
                                       (common.utils/register-event-listener "click" #(disable-wait-list waitlist))))
                             1000))
                         (let [opacity (j/get-in waitlist [:style :opacity])]
                           (if (= opacity "0")
                             (enable-wait-list waitlist)
                             (disable-wait-list waitlist)))))
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
      [icon-prev {:size 24
                  :color "white"
                  :weight "bold"
                  :cursor "pointer"}]
      [:span {:id "slide-indicator"
              :class (styles/current-slide-indicator)}
       (let [{:keys [current-slide-index slide-count]} @(subscribe [::subs/slide-info])]
         (str current-slide-index " / " slide-count))]
      [icon-next {:size 24
                  :color "white"
                  :weight "bold"
                  :cursor "pointer"}]]
     [:div {:id "right-controls"
            :class (styles/right-controls)}
      [wait-list-button]
      [icon-control {:size 24
                     :color "white"
                     :cursor "pointer"}]
      [icon-smiley {:size 24
                    :color "white"
                    :cursor "pointer"}]
      [icon-chat {:size 24
                  :color "white"
                  :cursor "pointer"}]
      [icon-dots {:size 24
                  :color "white"
                  :weight "bold"
                  :cursor "pointer"}]]]]])
