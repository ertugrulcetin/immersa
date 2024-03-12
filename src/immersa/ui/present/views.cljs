(ns immersa.ui.present.views
  (:require
    ["progressbar.js" :as ProgressBar]
    ["react" :as react]
    [applied-science.js-interop :as j]
    [clojure.string :as str]
    [immersa.common.firebase :as firebase]
    [immersa.scene.core :as scene.core]
    [immersa.ui.editor.components.button :refer [button]]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.icons :as icon]
    [immersa.ui.loading-screen :refer [loading-screen]]
    [immersa.ui.present.events :as events]
    [immersa.ui.present.styles :as styles]
    [immersa.ui.present.subs :as subs]
    [immersa.ui.subs :as main.subs]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r])
  (:require-macros
    [immersa.common.macros :as m]))

(defn- canvas-editor [mode]
  (r/create-class
    {:component-did-mount (fn []
                            (when (= mode :present)
                              (let [canvas (js/document.getElementById "renderCanvas")]
                                (j/assoc! canvas :className (styles/canvas))
                                (->> canvas
                                     (j/call (js/document.getElementById "canvas-present-origin") :append)))))
     :reagent-render (fn []
                       [:div {:id "canvas-present-origin"
                              :style {:width "100%"
                                      :height "100%"}}])}))

(defn- canvas-container [mode]
  (let [{:keys [width height]} (if (= mode :editor)
                                 @(subscribe [::subs/calculated-present-mode-canvas-dimensions])
                                 @(subscribe [::subs/calculated-canvas-dimensions]))]
    (when (and (> width 0) (> height 0))
      [:div
       {:id "canvas-container"
        :style {:width (str width "px")
                :height (str height "px")}
        :class (styles/canvas-container)}
       [canvas-editor mode]
       (when @(subscribe [::main.subs/loading-screen?])
         [loading-screen (if (= mode :editor)
                           (str height "px")
                           "100%")])])))

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

(defn- immersa-home-page-button []
  [:button
   {:on-click #(js/window.open "https://immersa.app" "_blank")
    :class [(styles/immersa-button)
            (styles/immersa-button-glow)
            (styles/immersa-button-gradient-border)]}
   [:img {:src "img/logo_white.png"
          :style {:width "70px"
                  :padding "3px"}}]])

(defn- title-bar [title present-state]
  [:div
   {:style {:width "100%"
            :height "55px"
            :display "flex"
            :justify-content "space-between"
            :align-items "center"
            :background "#2a2c37"}}
   [:div {:style {:display "flex"
                  :flex-direction "row"
                  :gap "5px"
                  :padding "0 16px"}}
    [icon/presentation {:size 18
                        :weight "bold"
                        :color "#fff"}]
    [text {:size :xl
           :weight :medium
           :color "#fff"} title]]
   [button {:text "Exit present mode"
            :on-click #(reset! present-state false)
            :type :outline
            :style {:color "#fff"
                    :display "flex"
                    :align-items "flex-end"
                    :font-size "15px"
                    :cursor "pointer"
                    :border-color "#fff"
                    :margin-right "16px"
                    :background "transparent"}
            :icon-left [icon/x {:size 16
                                :weight "bold"
                                :color "#fff"}]}]])

(defn init-app [{:keys [title slides presentation-id]}]
  (dispatch [::events/init-presentation
             {:id presentation-id
              :title title
              :slides slides}]))

(defn present-panel [& {:keys [mode title present-state]}]
  (let [_ (when (= mode :present)
            (react/useEffect
              (fn []
                (set! events/start-scene scene.core/start-scene)
                (let [_ (firebase/init-app)
                      path (j/get js/location :pathname)
                      slide-id (last (str/split path #"-+"))]
                  (m/js-await [q (firebase/get-presentation-info-by-id slide-id)]
                    (let [{:keys [id user_id title]} (j/lookup (j/call-in q [:docs 0 :data]))]
                      (m/js-await [presentation-url (firebase/get-presentation id user_id)]
                        (m/js-await [response (js/fetch presentation-url)]
                          (m/js-await [presentation (j/call response :text)]
                            (init-app {:title title
                                       :slides (cljs.reader/read-string presentation)
                                       :user-id user_id
                                       :presentation-id id})))))
                    (catch e
                           (js/console.log e)))))
              #js[]))
        present-loading? (and (= mode :present) (not @(subscribe [::subs/scene-ready?])))]
    [:<>
     (when (= mode :editor)
       [title-bar title present-state])
     [:div
      {:id "content-container"
       :class (styles/content-container (= mode :editor) present-loading?)
       :style {:background @(subscribe [::subs/background-color])}}

      [canvas-container mode]

      [:div
       {:id "progress-bar"
        :class (styles/progress-bar present-loading?)}
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
         [immersa-home-page-button]
         #_[icon/control {:size 24
                          :color "white"
                          :cursor "pointer"}]
         #_[icon/chat {:size 24
                       :color "white"
                       :cursor "pointer"}]
         #_[icon/dots {:size 24
                       :color "white"
                       :weight "bold"
                       :cursor "pointer"}]]]]]]))
