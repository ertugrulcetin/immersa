(ns immersa.ui.editor.components.tutorial
  (:require
    ["@radix-ui/react-dialog" :as Dialog]
    [immersa.ui.editor.components.button :refer [button]]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.icons :as icon]
    [immersa.ui.theme.colors :as colors]
    [reagent.core :as r]
    [spade.core :refer [defclass defattrs]]))

(defclass tutorial-root []
  {:all :unset})

(defclass tutorial-overlay []
  {:background-color colors/black-a9
   :position :fixed
   :inset 0
   :z-index 6000
   :animation "overlayShow 150ms cubic-bezier(0.16, 1, 0.3, 1)"})

(defclass tutorial-content []
  {:background-color :white
   :border-radius "6px"
   :box-shadow "hsl(206 22% 7% / 35%) 0px 10px 38px -10px, hsl(206 22% 7% / 20%) 0px 10px 20px -15px"
   :position :fixed
   :top "50%"
   :left "50%"
   :transform "translate(-50%, -50%)"
   :width "90vw"
   :height "400px"
   :max-width "1000px"
   :max-height "85vh"
   :padding "15px"
   :z-index 7000
   :box-sizing :border-box
   :animation "contentShow 150ms cubic-bezier(0.16, 1, 0.3, 1)"}
  [:&:focus
   {:outline :none}])

(defclass tutorial-title []
  {:margin 0
   :color colors/text-primary
   :font-size "17px"
   :font-weight 500})

(defclass tutorial-description []
  {:margin-bottom "20px"
   :color colors/text-primary
   :font-size "15px"
   :line-height 1.5})

(defclass close-button-styles []
  {:border-radius "100%"
   :width "32px"
   :height "32px"}
  [:&:hover
   {:background-color "#e1dfdf"}])

(defn- close-button []
  [button {:class (close-button-styles)
           :icon-left [icon/x {:size 20
                               :weight "bold"
                               :color colors/text-primary}]}])

(def tutorials
  (let [ul-style {:style {:padding-inline-start "20px"
                          :padding-inline-end "15px"}}
        li-style {:style {:margin-bottom "15px"}}
        text-style {:size :xxl
                    :weight :light}]
    [{:title "Camera controls"
      :content [:div
                [:ul ul-style
                 [:li li-style
                  [text text-style "Make sure camera is unlocked."]]
                 [:li li-style
                  [text text-style "Click and drag to rotate."]]
                 [:li li-style
                  [text text-style "Scroll to zoom."]]
                 [:li li-style
                  [text text-style "Right-click (or Ctrl / ⌃) and drag to pan."]]
                 [:li li-style
                  [text text-style "WASD to free move. Q/E to move up/down."]]]]
      :video "video/mouse-drag.mp4"}
     {:title "Importing 3D models"
      :content [:div
                [:ul ul-style
                 [:li li-style
                  [text text-style "Use the 3D Model button in the toolbar to import your models."]]
                 [:li li-style
                  [text text-style "You can only import .glb files. (for now)"]]]]
      :video "video/import-model.mp4"}
     {:title "Move, rotate & scale objects"
      :content [:div
                [:ul ul-style
                 [:li li-style
                  [text text-style "Moving object: enable position helper and drag the arrows."]]
                 [:li li-style
                  [text text-style "Rotating object: enable rotation helper and drag the circles."]]
                 [:li li-style
                  [text text-style "Scaling object: enable scale helper and drag the squares."]]]]
      :video "video/gizmo.mp4"}
     {:title "Adding slides"
      :content [:div
                [:ul ul-style
                 [:li li-style
                  [text text-style "Use the Add slide button in the left panel."]]
                 [:li li-style
                  [text text-style "Click Duplicate or Blank to add a new slide."]]
                 [:li li-style
                  [text text-style "Your objects will automatically animate, changing based on the position, rotation, and scale between slides."]]]]
      :video "video/add-slide.mp4"}
     {:title "Sharing slides"
      :content [:div
                [:ul ul-style
                 [:li li-style
                  [text text-style "Use the Share button in the top right corner."]]
                 [:li li-style
                  [text text-style "Click Copy link to copy the link to your clipboard."]]]]
      :video "video/share.mp4"}]))

(defn tutorial [_]
  (let [current-index (r/atom 0)]
    (fn [{:keys [trigger]}]
      (let [tutorial-count (count tutorials)
            current-tutorial (get tutorials @current-index)
            {:keys [title content video]} current-tutorial]
        [:> Dialog/Root {:class (tutorial-root)}
         [:> Dialog/Trigger {:as-child true}
          (if (-> trigger first fn?)
            (apply (first trigger) (rest trigger))
            trigger)]
         [:> Dialog/Portal
          [:> Dialog/Overlay {:class (tutorial-overlay)}]
          [:> Dialog/Content {:class (tutorial-content)}
           [:div {:style {:display "flex"
                          :flex-direction "row"
                          :width "100%"
                          :height "100%"}}
            [:div
             {:style {:display "flex"
                      :flex-direction "column"
                      :justify-content "flex-end"
                      :width "100%"
                      :height "100%"
                      :box-sizing "border-box"}}
             [:div
              {:style {:display "flex"
                       :flex-direction "column"
                       :width "100%"
                       :height "100%"
                       :box-sizing "border-box"}}
              [text {:size :xxxl
                     :weight :medium} title]
              content]
             [:div
              {:style {:display "flex"
                       :flex-direction "row"
                       :justify-content "space-between"
                       :align-items "center"
                       :width "calc(100% - 15px)"}}
              [:div
               [text {:size :xxl} (str (inc @current-index) " of " tutorial-count)]]
              [:div {:style {:display "flex"
                             :flex-direction "row"
                             :gap "5px"}}
               (when (not= @current-index 0)
                 [button {:text "Previous"
                          :on-click #(reset! current-index (dec @current-index))
                          :type :outline
                          :style {:font-size "16px"}}])
               (when (not= @current-index (dec tutorial-count))
                 [button {:text "Next"
                          :on-click #(reset! current-index (inc @current-index))
                          :type :regular
                          :style {:font-size "16px"}}])]]]
            [:div
             {:style {:width "100%"
                      :height "100%"
                      :box-sizing "border-box"}}
             [:> Dialog/Close {:as-child true}
              [:div
               {:style {:position "absolute"
                        :top "-30px"
                        :right "-30px"
                        :z-index 2}}
               [close-button]]]
             [:video {:style {:width "100%"
                              :height "100%"
                              :border-radius "6px"
                              :object-fit "cover"}
                      :src video
                      :loop true
                      :autoPlay true
                      :controls false}]]]]]]))))
