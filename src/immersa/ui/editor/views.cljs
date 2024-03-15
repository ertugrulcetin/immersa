(ns immersa.ui.editor.views
  (:require
    ["@clerk/clerk-react" :refer [useUser useAuth useClerk]]
    ["firebase/auth" :refer [getAuth signInWithCustomToken]]
    ["react" :as react]
    ["react-color" :refer [SketchPicker]]
    [applied-science.js-interop :as j]
    [clojure.string :as str]
    [goog.functions :as functions]
    [immersa.common.firebase :as firebase]
    [immersa.common.mixpanel :as mixpanel]
    [immersa.common.shortcut :as shortcut]
    [immersa.common.utils :as common.utils]
    [immersa.presentations.init :refer [slides thumbnails]]
    [immersa.scene.core :as scene.core]
    [immersa.ui.crisp-chat :as crisp-chat]
    [immersa.ui.editor.components.alert-dialog :refer [alert-dialog]]
    [immersa.ui.editor.components.button :refer [button]]
    [immersa.ui.editor.components.canvas-context-menu :refer [canvas-context-menu undo-redo-options]]
    [immersa.ui.editor.components.dropdown :refer [dropdown
                                                   dropdown-item
                                                   dropdown-separator
                                                   dropdown-context-menu
                                                   option-text]]
    [immersa.ui.editor.components.popup :refer [popup]]
    [immersa.ui.editor.components.progress :refer [progress]]
    [immersa.ui.editor.components.text :refer [text]]
    [immersa.ui.editor.components.tooltip :refer [tooltip]]
    [immersa.ui.editor.components.tutorial :refer [tutorial]]
    [immersa.ui.editor.events :as events]
    [immersa.ui.editor.options-panel.views :refer [options-panel]]
    [immersa.ui.editor.slide-panel.views :refer [slides-panel]]
    [immersa.ui.editor.styles :as styles]
    [immersa.ui.editor.subs :as subs]
    [immersa.ui.icons :as icon]
    [immersa.ui.loading-screen :refer [loading-screen]]
    [immersa.ui.present.events :as present.events]
    [immersa.ui.present.views :as present.views]
    [immersa.ui.subs :as main.subs]
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.styles :as main.styles]
    [nano-id.core :refer [custom]]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r])
  (:require-macros
    [immersa.common.macros :as m]))

(defonce present? (r/atom false))
(defonce canvas-started? (atom false))

(def nano-id
  (custom (apply str (concat (map (fn [i] (char i)) (range 65 91))
                             (map (fn [i] (char i)) (range 97 123))
                             (map str (range 0 10))))
          10))

(defn- canvas []
  (r/create-class
    {:component-did-mount (fn []
                            (println "Canvas editor mounted")
                            (let [canvas (js/document.getElementById "renderCanvas")
                                  app (js/document.getElementById "app")]
                              (->> canvas
                                   (j/call (js/document.getElementById "canvas-editor-origin") :append))
                              (j/assoc! canvas :className (styles/canvas :editor))
                              (j/assoc-in! app [:style :border] main.styles/app-border)
                              (j/assoc-in! canvas [:style :pointer-events] "auto")
                              (when-not @canvas-started?
                                (reset! canvas-started? true)
                                (j/call canvas :addEventListener "mouseenter" #(dispatch [::events/update-thumbnail]))
                                (j/call canvas :addEventListener "mouseleave" #(dispatch [::events/update-thumbnail])))
                              (when @canvas-started?
                                (js/setTimeout #(dispatch [::events/resize-scene]) 200))))
     :component-will-unmount (fn []
                               (->> (js/document.getElementById "renderCanvas")
                                    (j/call js/document.body :append)))
     :reagent-render (fn []
                       [:div {:id "canvas-editor-origin"
                              :style {:width "100%"
                                      :height "100%"}}])}))

(defn- canvas-container []
  (let [{:keys [width height]} @(subscribe [::subs/calculated-canvas-wrapper-dimensions])]
    (when (and width height (> width 0) (> height 0))
      [:div
       {:id "canvas-container"
        :style {:width (str width "px")
                :height (str height "px")}
        :class (styles/canvas-container)}
       [canvas]
       (when @(subscribe [::main.subs/loading-screen?])
         [loading-screen height])])))

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
                              (when @ref
                                (let [observer (js/ResizeObserver. on-resize)]
                                  (reset! ob observer)
                                  (j/call observer :observe @ref))))
       :component-will-unmount #(some-> @ob (j/call :disconnect))
       :reagent-render (fn []
                         [:div
                          {:id "canvas-wrapper"
                           :ref #(reset! ref %)
                           ;; :style {:flex 1}
                           :class (styles/canvas-wrapper)}
                          (when @ref
                            [canvas-container])])})))

(defn- sign-out []
  (let [{:keys [signOut]} (j/lookup (useClerk))]
    [dropdown-item
     {:item [option-text {:label "Sign out"
                          :size :l
                          :icon [icon/sign-out {:size 16
                                                :color colors/text-primary}]}]
      :on-select signOut}]))

(defn- feedback []
  [dropdown-item
   {:item [option-text {:label "Feedback & help"
                        :size :l
                        :icon [icon/chats-circle {:size 16
                                                  :color colors/text-primary}]}]
    :on-select #(dispatch [::events/open-crisp-chat])}])

(defn- header-left-panel []
  [:div (styles/title-bar)
   [:div (styles/menubar-list-icon)
    [dropdown
     {:style {:margin-left "28px"
              :margin-top "5px"}
      :trigger [icon/list-menu {:size 24
                                :color colors/text-primary}]
      :children [:<>
                 [undo-redo-options]
                 [dropdown-separator]
                 [feedback]
                 [:f> sign-out]]}]]
   [:div (styles/title-bar-full-width)
    [:div (styles/title-container)
     [:span {:class (styles/title-label)
             :content-editable true
             :suppressContentEditableWarning true
             :on-key-down (fn [e]
                            (when (= (j/get e :key) "Enter")
                              (.preventDefault e)
                              (.blur (-> e .-target))))
             :on-blur (fn [e]
                        (let [title (-> e .-target .-innerText)
                              title (if (str/blank? title)
                                      "Untitled"
                                      title)
                              title (if (> (count title) 50)
                                      (str (subs title 0 50) "...")
                                      title)
                              title (str/replace title #"\n" "")
                              title (str/trim title)]
                          (j/assoc-in! e [:target :innerText] title)
                          (dispatch [::events/update-presentation-title title])))}
      @(subscribe [::subs/slides-title])]
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

(defn- invisible-file-input [{:keys [type
                                     user-id
                                     max-size
                                     title
                                     on-complete]}]
  (let [open? (r/atom false)
        err? (r/atom false)
        limit-exceeded? (r/atom false)
        progress* (r/atom 0)
        task (atom nil)
        type-label (if (= type :image)
                     "image"
                     "model")]
    (fn []
      [:<>
       [alert-dialog
        {:open? @open?
         :title title
         :description (cond
                        @limit-exceeded? (str "The file size exceeds the limit of " max-size "MB. "
                                              "Please choose a smaller file.")
                        @err? (str "An error occurred while uploading your " type-label ". Please try again.")
                        :else (str "Please wait while we upload your " type-label "."))
         :content (when-not @limit-exceeded?
                    [progress {:value @progress*
                               :style {:width "100%"}}])
         :cancel-button [button {:text "Cancel"
                                 :on-click (fn []
                                             (when-let [task* @task]
                                               (j/call task* :cancel))
                                             (reset! open? false))}]}]
       [:input
        {:id (str type-label "-file-input")
         :type "file"
         :style {:display "none"}
         :accept (if (= type :image)
                   ".jpg, .jpeg, .png"
                   ".glb")
         :on-change (fn [this]
                      (when (-> this .-target .-value (not= ""))
                        (let [^js/File file (-> this .-target .-files (aget 0))]
                          (reset! open? true)
                          (reset! err? false)
                          (reset! limit-exceeded? false)
                          (reset! progress* 0)
                          (reset! task nil)
                          (if (< (j/get file :size) (* 1024 1024 max-size))
                            (firebase/upload-file
                              {:user-id user-id
                               :file file
                               :type type
                               :task-state task
                               :on-progress (fn [percentage]
                                              (reset! progress* percentage))
                               :on-error (fn [_]
                                           (reset! err? true))
                               :on-complete (fn [url]
                                              (reset! open? false)
                                              (on-complete file url))})
                            (reset! limit-exceeded? true))
                          (set! (-> this .-target .-value) ""))))}]])))

(defn- invisible-img-file-input []
  (when-let [user-id @(subscribe [::main.subs/user-id])]
    [invisible-file-input
     {:type :image
      :user-id user-id
      :max-size 5
      :title "Uploading image"
      :on-complete (fn [file url]
                     (dispatch [::events/add-image url])
                     (dispatch [::events/add-uploaded-image {:name (j/get file :name)
                                                             :url url}]))}]))

(defn- invisible-model-file-input []
  (when-let [user-id @(subscribe [::main.subs/user-id])]
    [invisible-file-input
     {:type :model
      :user-id user-id
      :max-size 10
      :title "Uploading 3D model"
      :on-complete (fn [file url]
                     (dispatch [::events/add-model url])
                     (dispatch [::events/add-uploaded-model {:name (j/get file :name)
                                                             :url url}]))}]))

(defn- image-component []
  (let [images @(subscribe [::subs/uploaded-images])]
    [dropdown
     {:scroll? true
      :style (when (<= (count images) 4)
               {:height (str (* 30 (inc (count images))) "px")})
      :trigger [presentation-component {:icon icon/image
                                        :text "Image"}]
      :children [:<>
                 [dropdown-item
                  {:item [:div {:style {:display "flex"
                                        :align-items "center"
                                        :justify-content "space-between"
                                        :width "100%"}}
                          [text {:size :l} "Upload image"]
                          [icon/upload {:size 16}]]
                   :on-select #(some-> (js/document.getElementById "image-file-input") .click)}]
                 [dropdown-separator]
                 (for [{:keys [name url]} images
                       :let [tr ^{:key url}
                             [dropdown-item
                              {:item [text {:size :l
                                            :weight :light
                                            :style {:white-space "nowrap"
                                                    :overflow "hidden"
                                                    :text-overflow "ellipsis"
                                                    :width "175px"}} name]
                               :on-select #(dispatch [::events/add-image url])}]]]
                   (if (> (count name) 21)
                     ^{:key url}
                     [tooltip
                      {:content name
                       :trigger tr}]
                     tr))]}]))

(defn- model-component []
  (let [models @(subscribe [::subs/uploaded-models])]
    [dropdown
     {:scroll? true
      :style (when (<= (count models) 4)
               {:height (str (* 30 (inc (count models))) "px")})
      :trigger [presentation-component {:icon icon/cube
                                        :text "3D Model"
                                        :class (styles/presentation-component-cube)}]
      :children [:<>
                 [dropdown-item
                  {:item [:div {:style {:display "flex"
                                        :align-items "center"
                                        :justify-content "space-between"
                                        :width "100%"}}
                          [text {:size :l} "Upload 3D model (.glb)"]
                          [icon/upload {:size 16}]]
                   :on-select #(some-> (js/document.getElementById "model-file-input") .click)}]
                 [dropdown-separator]
                 (for [{:keys [name url]} models
                       :let [tr ^{:key url}
                             [dropdown-item
                              {:item [text {:size :l
                                            :weight :light
                                            :style {:white-space "nowrap"
                                                    :overflow "hidden"
                                                    :text-overflow "ellipsis"
                                                    :width "175px"}} name]
                               :on-select #(dispatch [::events/add-model url])}]]]
                   (if (> (count name) 21)
                     ^{:key url}
                     [tooltip
                      {:content name
                       :trigger tr}]
                     tr))]}]))

(defn- header-center-panel []
  [:div (styles/header-center-panel)

   [tooltip
    {:trigger [presentation-component {:icon icon/text
                                       :text "Text"
                                       :on-click #(dispatch [::events/add-text-mesh])}]
     :content "Add text"
     :shortcuts "T"}]
   [invisible-img-file-input]
   [invisible-model-file-input]
   [image-component]
   [model-component]
   #_[presentation-component {:icon icon/camera
                              :text "Camera"
                              :disabled? true}]
   [tutorial {:trigger [presentation-component {:icon icon/student
                                                :text "Tutorial"}]}]
   [presentation-component {:icon icon/chats-circle
                            :text "Feedback"
                            :text-weight :regular
                            :class (styles/presentation-component-cube)
                            :color colors/button-outline-text
                            :on-click #(dispatch [::events/open-crisp-chat])}]
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
             :on-click #(reset! present? true)
             :disabled? (not @(subscribe [::subs/scene-ready?]))
             :type :outline
             :class (styles/present-share-width)
             :icon-left [icon/play {:size 18
                                    :weight "fill"
                                    :color colors/button-outline-text}]}]
    [popup {:trigger [button {:text "Share"
                              :disabled? (not @(subscribe [::subs/scene-ready?]))
                              :type :regular
                              :class (styles/present-share-width)
                              :icon-right [icon/share {:size 18
                                                       :weight "fill"
                                                       :color colors/button-text}]}]
            :content (let [url @(subscribe [::subs/share-link])]
                       [:div
                        {:style {:display "flex"
                                 :flex-direction "column"
                                 :gap "12px"}}
                        [:div {:style {:display "flex"
                                       :gap "4px"}}
                         [text {:size :l} "Share presentation"]
                         [icon/paper-plane {:size 16}]]
                        [:a {:href url
                             :target "_blank"
                             :style {:padding "5px"
                                     :font-size "14px"
                                     :user-select "auto"
                                     :color colors/button-outline-text
                                     :border (str "1px solid " colors/active-bg)
                                     :border-radius "5px"}}
                         url]
                        [:div {:style {:display "flex"
                                       :flex-direction "row"
                                       :justify-content "flex-end"
                                       :width "100%"
                                       :margin-top "10px"
                                       :gap "8px"}}
                         [button {:text "Copy link"
                                  :on-click #(common.utils/copy-to-clipboard url)
                                  :style {:font-weight 400}
                                  :icon-left [icon/link {:size 16
                                                         :color colors/text-primary}]}]]])}]]])

(defn- header []
  [:div (styles/header-container)
   [header-left-panel]
   [header-center-panel]
   [header-right-panel]])

(def blocked-fields #{"INPUT" "TEXTAREA" "CANVAS"})

(defn- register-global-events []
  (common.utils/register-event-listener
    js/document
    "keydown"
    (fn [e]
      (when (= (j/get e :key) "Escape")
        (reset! present? false))
      (when (and (or (j/get e :metaKey)
                     (j/get e :ctrlKey))
                 (= (some-> (j/get e :key) str/lower-case) "d"))
        (j/call e :preventDefault))
      (when (and (not (j/get e :repeat))
                 (not (blocked-fields (j/get-in js/document [:activeElement :tagName]))))
        (shortcut/call-shortcut-action-with-event :blank-slide e)
        (shortcut/call-shortcut-action-with-event :paste e)))))

(defn- present-panel []
  (r/create-class
    {:component-did-mount (fn []
                            (dispatch [::events/clear-selected-mesh])
                            (dispatch [::events/update-slide-info])
                            (dispatch [::events/add-listeners-for-present-mode])
                            (dispatch [::events/save-started-index])
                            (let [canvas (js/document.getElementById "renderCanvas")
                                  app (js/document.getElementById "app")]
                              (j/assoc! canvas :className (styles/canvas :present))
                              (j/assoc-in! app [:style :border] "0px")
                              (j/assoc-in! canvas [:style :pointer-events] "none")
                              (->> canvas
                                   (j/call (js/document.getElementById "canvas-present-origin") :append))
                              (js/setTimeout #(dispatch [::events/resize-scene]) 200)))
     :component-will-unmount (fn []
                               (dispatch [::events/create-go-to-slide-action])
                               (dispatch [::events/remove-listeners-for-present-mode])
                               (let [canvas (js/document.getElementById "renderCanvas")]
                                 (->> canvas
                                      (j/call js/document.body :append))))
     :reagent-render (fn []
                       [:f> present.views/present-panel {:mode :editor
                                                         :title @(subscribe [::subs/slides-title])
                                                         :present-state present?}])}))

(defn- init-crisp-chat
  ([email full-name]
   (init-crisp-chat email full-name 0))
  ([email full-name retry-count]
   (try
     (crisp-chat/set-user-email email)
     (when-not (str/blank? full-name)
       (crisp-chat/set-user-name full-name))
     (js/console.log "Crisp Chat initialized")
     (catch js/Error e
       (if (< retry-count 3)
         (do
           (js/console.log "Retrying to initialize Crisp Chat")
           (js/setTimeout #(init-crisp-chat email full-name (inc retry-count)) (* 1000 (inc retry-count))))
         (do
           (js/console.error e)
           (js/console.error "Failed to initialize Crisp Chat")))))))

(defn init-app [{:keys [title slides thumbnails user-id presentation-id email full-name]}]
  (firebase/get-last-uploaded-files
    {:type :image
     :user-id user-id
     :on-complete #(dispatch [::events/add-uploaded-image %])})
  (firebase/get-last-uploaded-files
    {:type :model
     :user-id user-id
     :on-complete #(dispatch [::events/add-uploaded-model %])})
  (dispatch [::events/init-user
             {:id user-id
              :full-name full-name
              :email email}])
  (dispatch [::events/init-presentation
             {:id presentation-id
              :title title
              :slides slides
              :thumbnails thumbnails
              :present-state present?}]))

(defn- upload-first-slide-and-thumbnail [{:keys [title user-id presentation-id email full-name]}]
  (m/js-await [_ (firebase/upload-presentation {:user-id user-id
                                                :presentation-id presentation-id
                                                :presentation-data slides})]
    (m/js-await [_ (firebase/upload-thumbnail {:user-id user-id
                                               :presentation-id presentation-id
                                               :slide-id "14e4ee76-bb27-4904-9d30-360a40d8abb7"
                                               :thumbnail (get thumbnails "14e4ee76-bb27-4904-9d30-360a40d8abb7")})]
      (init-app {:title title
                 :slides slides
                 :thumbnails thumbnails
                 :user-id user-id
                 :presentation-id presentation-id
                 :email email
                 :full-name full-name})
      (catch fail-upload-thumbnail))
    (catch fail-upload-presentation)))

(defn- init-thumbnails [{:keys [user-id presentation-id]}]
  (firebase/get-thumbnails {:user-id user-id
                            :presentation-id presentation-id
                            :on-complete (fn [slide-id url]
                                           (dispatch [::events/add-thumbnail slide-id url]))}))

(defn- get-presentation-and-start [{:keys [user-id email full-name]}]
  (m/js-await [q (firebase/get-presentation-info user-id)]
    (let [{:keys [id title]} (j/lookup (j/call-in q [:docs 0 :data]))]
      (m/js-await [presentation-url (firebase/get-presentation id user-id)]
        (m/js-await [response (js/fetch presentation-url)]
          (m/js-await [presentation (j/call response :text)]
            (init-app {:title title
                       :slides (cljs.reader/read-string presentation)
                       :thumbnails {}
                       :user-id user-id
                       :presentation-id id
                       :email email
                       :full-name full-name})
            (init-thumbnails {:user-id user-id
                              :presentation-id id})))))
    (catch e
           (js/console.log e))))

(defn editor-panel []
  (let [{:keys [user]} (j/lookup (useUser))
        {:keys [getToken]} (j/lookup (useAuth))
        _ (react/useEffect
            (fn []
              (-> (js/document.getElementById "app")
                  (j/assoc-in! [:style :border] main.styles/app-border))
              (set! events/start-scene scene.core/start-scene)
              (let [user-id (j/get user :id)
                    email (j/get-in user [:primaryEmailAddress :emailAddress])
                    full-name (j/get user :fullName)
                    _ (firebase/init-app)
                    auth (getAuth)]
                (mixpanel/init user-id email full-name)
                (m/js-await [token (getToken #js {:template "integration_firebase"})]
                  (m/js-await [userCredentials (signInWithCustomToken auth token)]
                    (init-crisp-chat email full-name)
                    (m/js-await [doc (firebase/get-user user-id)]
                      (if (j/call doc :exists)
                        (get-presentation-and-start {:user-id user-id
                                                     :email email
                                                     :full-name full-name})
                        (let [presentation-id (nano-id 10)]
                          (m/js-await [_ (firebase/init-user-and-presentation
                                           {:user {:id user-id
                                                   :email email
                                                   :full_name full-name}
                                            :presentation {:id presentation-id
                                                           :title "Untitled"
                                                           :created_at (-> (js/Date.)
                                                                           (j/call :toISOString))}})]
                            (upload-first-slide-and-thumbnail {:slides slides
                                                               :thumbnails thumbnails
                                                               :user-id user-id
                                                               :title "Untitled"
                                                               :presentation-id presentation-id
                                                               :email email
                                                               :full-name full-name})
                            (catch fail-init-user-data
                                   (println "Failed")))))
                      (catch fail-get-user
                             (println "Error!!!!..")))))
                (register-global-events)))
            #js[])]
    [:<>
     (if @present?
       [present-panel]
       [:<>
        (if-not (seq @(subscribe [::subs/slides-all]))
          [:div (styles/logo-container)
           [:img {:src "img/logo.png"
                  :class (styles/logo-loading)}]])
        [:div (styles/editor-container)
         [header]
         [:div (styles/content-container)
          [slides-panel]
          [canvas-wrapper]
          [options-panel]
          [canvas-context-menu]]]])]))

(comment
  @(subscribe [::subs/slides-thumbnails])

  (->> (js/document.getElementById "canvas-wrapper")
       (j/call (js/document.getElementById "temp") :append))

  @(subscribe [::subs/editor])
  @(subscribe [::main.subs/user])
  (count (nano-id 10))
  )
