(ns immersa.common.locals
  "Local storage adapter - replaces Firebase with local IndexedDB storage.
   Maintains the same API for backwards compatibility."
  (:refer-clojure :exclude [list])
  (:require
    [applied-science.js-interop :as j]
    [immersa.common.local-storage :as ls]))

;; No-op initialization (previously initialized Firebase)
(defn init-app []
  (-> (ls/init)
      (j/call :then #(js/console.log "Local storage initialized"))
      (j/call :catch #(js/console.error "Failed to initialize local storage:" %))))

;; ============ File Upload (now local storage) ============

(defn upload-file [{:keys [file
                           type
                           on-progress
                           on-error
                           on-complete]}]
  (ls/save-file {:file file
                 :type type
                 :on-progress on-progress
                 :on-error on-error
                 :on-complete on-complete}))

(defn get-last-uploaded-files [{:keys [type on-complete]}]
  (ls/get-files-by-type type on-complete))

;; ============ Presentation Storage ============

(defn upload-presentation [{:keys [presentation-id
                                   presentation-data]}]
  (ls/save-presentation {:id presentation-id
                         :title (or (get-in presentation-data [0 :title]) "Untitled")
                         :data presentation-data}))

(defn upload-thumbnail [{:keys [slide-id
                                presentation-id
                                thumbnail]}]
  (ls/save-thumbnail {:presentation-id presentation-id
                      :slide-id slide-id
                      :thumbnail thumbnail}))

(defn get-presentation [id _user-id]
  "Get presentation - returns a promise that resolves to the presentation data URL."
  (js/Promise.
    (fn [resolve reject]
      (-> (ls/get-presentation id)
          (j/call :then (fn [result]
                          (if result
                            ;; Return a data URL with the presentation data
                            (let [data-str (pr-str (:data result))
                                  blob (js/Blob. #js [data-str] #js {:type "text/plain"})
                                  url (js/URL.createObjectURL blob)]
                              (resolve url))
                            (reject "Presentation not found"))))
          (j/call :catch reject)))))

(defn get-thumbnails [{:keys [presentation-id on-complete]}]
  (ls/get-thumbnails presentation-id on-complete))

;; ============ User Functions (now no-op for offline mode) ============

(defn get-user [_user-id]
  "Returns a mock document that 'exists' for offline mode."
  (js/Promise.resolve
    #js {:exists (fn [] true)
         :data (fn [] #js {:id "local-user"})}))

(defn get-presentation-info [_user-id]
  "Get presentation info - returns all local presentations."
  (js/Promise.
    (fn [resolve _reject]
      (-> (ls/get-all-presentations)
          (j/call :then (fn [presentations]
                          (if (seq presentations)
                            (let [first-pres (first presentations)]
                              (resolve #js {:docs #js [#js {:data (fn []
                                                                    #js {:id (:id first-pres)
                                                                         :title (:title first-pres)
                                                                         :user_id "local-user"})}]}))
                            ;; No presentations exist yet
                            (resolve #js {:docs #js []}))))
          (j/call :catch (fn [_]
                           (resolve #js {:docs #js []})))))))

(defn get-presentation-info-by-id [id]
  "Get presentation info by ID."
  (js/Promise.
    (fn [resolve _reject]
      (-> (ls/get-presentation id)
          (j/call :then (fn [result]
                          (if result
                            (resolve #js {:docs #js [#js {:data (fn []
                                                                  #js {:id (:id result)
                                                                       :title (:title result)
                                                                       :user_id "local-user"})}]})
                            (resolve #js {:docs #js []}))))
          (j/call :catch (fn [_]
                           (resolve #js {:docs #js []})))))))

(defn set-user [_user]
  "No-op for offline mode."
  (js/Promise.resolve true))

(defn set-presentation-info [data]
  "Update presentation info in local storage."
  (-> (ls/get-presentation (:id data))
      (j/call :then (fn [existing]
                      (when existing
                        (ls/save-presentation {:id (:id data)
                                               :title (or (:title data) (:title existing))
                                               :data (:data existing)}))))))

(defn init-user-and-presentation [{:keys [presentation]}]
  "Initialize a new presentation in local storage."
  (ls/save-presentation {:id (:id presentation)
                         :title (:title presentation)
                         :data []}))

(defn update-presentation-title [{:keys [presentation-id title]}]
  "Update presentation title."
  (-> (ls/get-presentation presentation-id)
      (j/call :then (fn [existing]
                      (when existing
                        (ls/save-presentation {:id presentation-id
                                               :title title
                                               :data (:data existing)}))))))

(comment
  (init-app)
  )
