(ns immersa.ui.editor.slide-panel.views
  (:require
    ["@dnd-kit/core" :refer [DndContext
                             closestCenter
                             KeyboardSensor
                             PointerSensor
                             TouchSensor
                             useSensor
                             useSensors]]
    ["@dnd-kit/sortable" :refer [arrayMove
                                 SortableContext
                                 sortableKeyboardCoordinates
                                 verticalListSortingStrategy
                                 rectSortingStrategy
                                 verticalListSortingStrategy
                                 useSortable]]
    ["@dnd-kit/utilities" :refer [CSS]]
    ["react" :as react]
    [applied-science.js-interop :as j]
    [immersa.common.shortcut :as shortcut]
    [immersa.ui.editor.components.button :refer [button]]
    [immersa.ui.editor.components.context-menu :refer [context-menu context-menu-item]]
    [immersa.ui.editor.components.dropdown :refer [dropdown dropdown-item option-text]]
    [immersa.ui.editor.components.scroll-area :refer [scroll-area]]
    [immersa.ui.editor.components.tooltip :refer [tooltip]]
    [immersa.ui.editor.events :as events]
    [immersa.ui.editor.slide-panel.styles :as styles]
    [immersa.ui.editor.subs :as subs]
    [immersa.ui.icons :as icon]
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(def dnd-context (r/adapt-react-class DndContext))
(def sortable-context (r/adapt-react-class SortableContext))

(def dragging-slide-id (r/atom nil))

(defn- to-clj-map [hash-map]
  (js->clj hash-map :keywordize-keys true))

(defn- click-prev-slide [props index-state]
  (when-let [id (j/get (:items props) (dec @index-state))]
    (some-> (js/document.getElementById (str "slide-container-" id)) .click)))

(defn- click-next-slide [props index-state]
  (when-let [id (j/get (:items props) (inc @index-state))]
    (some-> (js/document.getElementById (str "slide-container-" id)) .click)))

(defn- slide [props]
  (let [{:keys [attributes
                listeners
                setNodeRef
                transform
                transition]} (to-clj-map (useSortable (clj->js {:id (:id props)})))
        index (.indexOf (:items props) (:id props))
        current-index @(subscribe [::subs/slides-current-index])
        index-state (atom current-index)
        thumbnail @(subscribe [::subs/slide-thumbnail index])
        camera-unlocked? (not @(subscribe [::subs/camera-locked?]))
        selected? (= index current-index)
        transform-js (clj->js transform)
        transform-js (when-not (nil? transform-js)
                       (j/assoc! transform-js :x 0))]
    [context-menu
     {:children [:<>
                 [context-menu-item {:label "Duplicate"
                                     :icon [icon/copy {:size 16
                                                       :color colors/text-primary}]
                                     :shortcut (shortcut/get-shortcut-key-labels :add-slide)
                                     :on-select #(shortcut/call-shortcut-action :add-slide)}]
                 [context-menu-item {:label "Delete"
                                     :icon [icon/trash {:size 16
                                                        :color colors/warning}]
                                     :color colors/warning
                                     :shortcut (shortcut/get-shortcut-key-labels :delete-slide)
                                     :on-select #(shortcut/call-shortcut-action :delete-slide)}]]
      :trigger [:div
                (merge {:id (:id props)
                        :ref setNodeRef
                        :style {:display "flex"
                                :align-items "flex-start"
                                :padding-left "8px"
                                :padding-bottom "8px"
                                :user-select "none"
                                :outline "none"
                                :z-index (if (= (:id props) @(:dragging-slide-id props)) 9 0)
                                :transform (j/call-in CSS [:Transform :toString] transform-js)
                                :transition transition}
                        :on-click #(dispatch [::events/go-to-slide index])}
                       attributes
                       listeners)
                [:div
                 {:style {:display "flex"
                          :flex-direction "column"
                          :justify-content "space-between"
                          :height "42px"}}
                 [:span {:style {:width "22px"
                                 :color (cond
                                          (and camera-unlocked? selected?)
                                          colors/unlocked-camera

                                          selected?
                                          colors/button-outline-border

                                          :else colors/text-primary)
                                 :font-size typography/s
                                 :font-weight (if selected?
                                                typography/medium
                                                typography/regular)}}
                  (if (= (:id props) @dragging-slide-id)
                    ""
                    (inc index))]
                 (when (and camera-unlocked? selected?)
                   [tooltip
                    {:trigger [icon/unlock {:size 12
                                            :color colors/unlocked-camera}]
                     :content "Camera unlocked"}])]
                [:div {:id (str "slide-container-" (:id props))
                       :tabIndex "0"
                       :class (styles/slide camera-unlocked? selected?)
                       :on-key-down (fn [e]
                                      (when-not (j/get e :repeat)
                                        (when (or (= "ArrowDown" (j/get e :code))
                                                  (= "ArrowRight" (j/get e :code)))
                                          (click-next-slide props index-state))

                                        (when (or (= "ArrowUp" (j/get e :code))
                                                  (= "ArrowLeft" (j/get e :code)))
                                          (click-prev-slide props index-state))

                                        (when (shortcut/call-shortcut-action-with-event :delete-slide e)
                                          (click-prev-slide props index-state))
                                        (shortcut/call-shortcut-action-with-event :add-slide e)))
                       :on-click #(some-> (js/document.getElementById (str "slide-container-" (:id props))) .focus)}
                 [:img {:src thumbnail
                        :class (styles/slide-img)}]]]}]))

(defn- sortable-slides-list [slides]
  (let [slide-ids (clj->js (mapv :id slides))
        slide-ids-length (count slide-ids)
        [items setItems] (react/useState slide-ids)
        _ (react/useEffect (fn [] (setItems slide-ids)) (clj->js [slide-ids-length]))
        sensors (useSensors
                  (useSensor PointerSensor (clj->js {:activationConstraint {:distance 0.01}}))
                  (useSensor KeyboardSensor (to-clj-map {:coordinateGetter sortableKeyboardCoordinates})))
        handle-drag-start (fn [event]
                            (reset! dragging-slide-id (j/get-in event [:active :id])))
        handle-drag-end (fn [event]
                          (reset! dragging-slide-id nil)
                          (let [{:keys [active over]} (to-clj-map event)]
                            (let [oldIndex (.indexOf items (:id active))
                                  newIndex (.indexOf items (:id over))]
                              (dispatch [::events/re-order-slides oldIndex newIndex])
                              (setItems (clj->js (arrayMove (clj->js items) oldIndex newIndex))))))]
    [:div
     [dnd-context {:sensors sensors
                   :collisionDetection closestCenter
                   :onDragStart handle-drag-start
                   :onDragEnd handle-drag-end}
      [sortable-context {:items items
                         :strategy rectSortingStrategy}
       [:div {:style {:display :flex
                      :flex-wrap :wrap}}
        (map
          (fn [id]
            ^{:key id}
            [:f> slide
             {:key id
              :id id
              :items items
              :dragging-slide-id dragging-slide-id}])
          items)]]]]))

(defn slides-panel []
  [:div (styles/side-bar)
   [:div
    {:style {:display "flex"
             :align-items "center"
             :padding "8px 16px 0 16px"}}
    [dropdown
     {:style {:height "100%"}
      :trigger [button {:text "Add slide"
                        :on-click #(dispatch [::events/add-slide])
                        :class (styles/add-slide-button)
                        :icon-left [icon/plus {:size 18
                                               :color colors/text-primary}]}]
      :children [:<>
                 [dropdown-item
                  {:item [option-text {:label "Duplicate"
                                       :icon [icon/copy {:size 16
                                                         :color colors/text-primary}]
                                       :shortcut (shortcut/get-shortcut-key-labels :add-slide)}]
                   :on-select #(shortcut/call-shortcut-action :add-slide)}]
                 [dropdown-item
                  {:item [option-text {:label "Blank slide"
                                       :icon [icon/file {:size 16
                                                         :color colors/text-primary}]
                                       :shortcut (shortcut/get-shortcut-key-labels :blank-slide)}]
                   :on-select #(shortcut/call-shortcut-action :blank-slide)}]]}]]
   [scroll-area
    {:class (styles/slides-scroll-area)
     :children [:div {:style {:padding-top "8px"
                              :outline "none"}}
                (let [slides (map-indexed #(assoc %2 :index %1) @(subscribe [::subs/slides-all]))]
                  [:f> sortable-slides-list slides])]}]])
