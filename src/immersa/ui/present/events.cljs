(ns immersa.ui.present.events
  (:require
    [applied-science.js-interop :as j]
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]))

(def start-scene nil)

(reg-event-db
  ::add-progress-bar
  (fn [db [_ bar]]
    (assoc-in db [:present :progress-bar-element] bar)))

(reg-event-fx
  ::update-slide-info
  (fn [{:keys [db]} [_ index slide-count]]
    (let [current-slide-index (inc index)]
      {:db (-> db
               (assoc-in [:present :current-slide-index] current-slide-index)
               (assoc-in [:present :slide-count] slide-count))
       ::animate-progress-bar [(-> db :present :progress-bar-element) (/ current-slide-index slide-count)]})))

(reg-fx
  ::animate-progress-bar
  (fn [[progress-bar progress]]
    (when progress-bar
      (j/call progress-bar :animate progress))))

(reg-event-db
  ::set-show-arrow-keys-text?
  (fn [db [_ show-arrow-keys-text?]]
    (assoc-in db [:present :show-arrow-keys-text?] show-arrow-keys-text?)))

(reg-event-db
  ::set-show-pre-warm-text?
  (fn [db [_ show-pre-warm-text?]]
    (assoc-in db [:present :show-pre-warm-text?] show-pre-warm-text?)))

(reg-event-db
  ::set-background-color
  (fn [db [_ background-color]]
    (assoc-in db [:present :background-color] background-color)))

(reg-event-fx
  ::init-presentation
  (fn [{:keys [db]} [_ {:keys [id title slides] :as opts}]]
    {:db (-> db
             (assoc-in [:present :slides :id] id)
             (assoc-in [:present :slides :title] title)
             (assoc-in [:present :slides :all] slides))
     ::start-scene opts
     ::set-title title}))

(reg-fx
  ::start-scene
  (fn [{:keys [slides thumbnails present-state]}]
    (start-scene (js/document.getElementById "renderCanvas")
                 {:mode :present
                  :present-state present-state
                  :slides slides
                  :thumbnails thumbnails})))

(reg-fx
  ::set-title
  (fn [title]
    (j/assoc! js/document :title (str title " - Immersa"))))

(reg-event-db
  ::scene-ready
  (fn [db]
    (assoc-in db [:present :scene-ready?] true)))
