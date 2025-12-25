(ns immersa.common.local-storage
  "Local storage module for offline presentations management.
   Replaces Firebase with IndexedDB for local-first functionality."
  (:require
    [applied-science.js-interop :as j]
    [cljs.reader :as reader]
    [clojure.string :as str]))

;; IndexedDB database name and version
(def db-name "immersa-db")
(def db-version 1)

;; Store names
(def presentations-store "presentations")
(def thumbnails-store "thumbnails")
(def files-store "files")
(def settings-store "settings")

(defonce db-instance (atom nil))

;; ============ IndexedDB Core Functions ============

(defn- get-db []
  @db-instance)

(defn init-db
  "Initialize IndexedDB database with all required object stores."
  []
  (js/Promise.
    (fn [resolve reject]
      (let [request (j/call js/indexedDB :open db-name db-version)]
        (j/assoc! request :onerror
                  (fn [event]
                    (js/console.error "IndexedDB error:" event)
                    (reject event)))

        (j/assoc! request :onupgradeneeded
                  (fn [event]
                    (let [db (j/get-in event [:target :result])]
                      ;; Presentations store
                      (when-not (j/call-in db [:objectStoreNames :contains] presentations-store)
                        (let [store (j/call db :createObjectStore presentations-store #js {:keyPath "id"})]
                          (j/call store :createIndex "title" "title" #js {:unique false})
                          (j/call store :createIndex "updated_at" "updated_at" #js {:unique false})))

                      ;; Thumbnails store
                      (when-not (j/call-in db [:objectStoreNames :contains] thumbnails-store)
                        (j/call db :createObjectStore thumbnails-store #js {:keyPath "id"}))

                      ;; Files store (for images and models)
                      (when-not (j/call-in db [:objectStoreNames :contains] files-store)
                        (let [store (j/call db :createObjectStore files-store #js {:keyPath "id"})]
                          (j/call store :createIndex "type" "type" #js {:unique false})
                          (j/call store :createIndex "name" "name" #js {:unique false})))

                      ;; Settings store
                      (when-not (j/call-in db [:objectStoreNames :contains] settings-store)
                        (j/call db :createObjectStore settings-store #js {:keyPath "key"})))))

        (j/assoc! request :onsuccess
                  (fn [event]
                    (let [db (j/get-in event [:target :result])]
                      (reset! db-instance db)
                      (js/console.log "IndexedDB initialized successfully")
                      (resolve db))))))))

;; ============ Presentations ============

(defn save-presentation
  "Save presentation data to IndexedDB."
  [{:keys [id title data]}]
  (js/Promise.
    (fn [resolve reject]
      (when-let [db (get-db)]
        (let [tx (j/call db :transaction #js [presentations-store] "readwrite")
              store (j/call tx :objectStore presentations-store)
              record #js {:id id
                          :title title
                          :data (pr-str data)
                          :updated_at (-> (js/Date.) (j/call :toISOString))}
              request (j/call store :put record)]
          (j/assoc! request :onsuccess #(resolve true))
          (j/assoc! request :onerror #(reject %)))))))

(defn get-presentation
  "Get presentation by ID from IndexedDB."
  [id]
  (js/Promise.
    (fn [resolve reject]
      (when-let [db (get-db)]
        (let [tx (j/call db :transaction #js [presentations-store] "readonly")
              store (j/call tx :objectStore presentations-store)
              request (j/call store :get id)]
          (j/assoc! request :onsuccess
                    (fn [event]
                      (let [result (j/get-in event [:target :result])]
                        (if result
                          (resolve {:id (j/get result :id)
                                    :title (j/get result :title)
                                    :data (reader/read-string (j/get result :data))
                                    :updated_at (j/get result :updated_at)})
                          (resolve nil)))))
          (j/assoc! request :onerror #(reject %)))))))

(defn get-all-presentations
  "Get all presentations from IndexedDB."
  []
  (js/Promise.
    (fn [resolve reject]
      (when-let [db (get-db)]
        (let [tx (j/call db :transaction #js [presentations-store] "readonly")
              store (j/call tx :objectStore presentations-store)
              request (j/call store :getAll)]
          (j/assoc! request :onsuccess
                    (fn [event]
                      (let [results (j/get-in event [:target :result])]
                        (resolve (mapv (fn [r]
                                         {:id (j/get r :id)
                                          :title (j/get r :title)
                                          :updated_at (j/get r :updated_at)})
                                       results)))))
          (j/assoc! request :onerror #(reject %)))))))

(defn delete-presentation
  "Delete presentation by ID."
  [id]
  (js/Promise.
    (fn [resolve reject]
      (when-let [db (get-db)]
        (let [tx (j/call db :transaction #js [presentations-store] "readwrite")
              store (j/call tx :objectStore presentations-store)
              request (j/call store :delete id)]
          (j/assoc! request :onsuccess #(resolve true))
          (j/assoc! request :onerror #(reject %)))))))

;; ============ Thumbnails ============

(defn save-thumbnail
  "Save thumbnail for a slide."
  [{:keys [presentation-id slide-id thumbnail]}]
  (js/Promise.
    (fn [resolve reject]
      (when-let [db (get-db)]
        (let [tx (j/call db :transaction #js [thumbnails-store] "readwrite")
              store (j/call tx :objectStore thumbnails-store)
              id (str presentation-id "-" slide-id)
              record #js {:id id
                          :presentation_id presentation-id
                          :slide_id slide-id
                          :data thumbnail}
              request (j/call store :put record)]
          (j/assoc! request :onsuccess #(resolve true))
          (j/assoc! request :onerror #(reject %)))))))

(defn get-thumbnails
  "Get all thumbnails for a presentation."
  [presentation-id on-complete]
  (when-let [db (get-db)]
    (let [tx (j/call db :transaction #js [thumbnails-store] "readonly")
          store (j/call tx :objectStore thumbnails-store)
          request (j/call store :getAll)]
      (j/assoc! request :onsuccess
                (fn [event]
                  (let [results (j/get-in event [:target :result])]
                    (doseq [r results]
                      (when (= (j/get r :presentation_id) presentation-id)
                        (when on-complete
                          (on-complete (j/get r :slide_id) (j/get r :data)))))))))))

;; ============ Files (Images & Models) ============

(defn save-file
  "Save a file (image or model) to IndexedDB.
   Returns a stable local-file:// URL that can be resolved later."
  [{:keys [file type on-progress on-complete on-error]}]
  (let [file-id (str (random-uuid))
        file-name (j/get file :name)
        mime-type (j/get file :type)]
    (when on-progress (on-progress 10))
    ;; Read file as ArrayBuffer for proper blob handling
    (let [reader (js/FileReader.)]
      (j/assoc! reader :onload
                (fn [event]
                  (let [array-buffer (j/get-in event [:target :result])
                        blob (js/Blob. #js [array-buffer] #js {:type mime-type})]
                    (when on-progress (on-progress 50))
                    ;; Also store in IndexedDB for persistence (as base64 for storage)
                    (let [base64-reader (js/FileReader.)]
                      (j/assoc! base64-reader :onload
                                (fn [e]
                                  (let [data-url (j/get-in e [:target :result])]
                                    (when-let [db (get-db)]
                                      (let [tx (j/call db :transaction #js [files-store] "readwrite")
                                            store (j/call tx :objectStore files-store)
                                            record #js {:id file-id
                                                        :name file-name
                                                        :type (name type)
                                                        :mime_type mime-type
                                                        :data data-url
                                                        :created_at (-> (js/Date.) (j/call :toISOString))}
                                            request (j/call store :put record)]
                                        (j/assoc! request :onsuccess
                                                  (fn [_]
                                                    (when on-progress (on-progress 100))
                                                    ;; Return a stable URL for persistence
                                                    (when on-complete
                                                      (on-complete (str "local-file://" file-id)))))
                                        (j/assoc! request :onerror
                                                  (fn [e]
                                                    (when on-error (on-error e)))))))))
                      (j/call base64-reader :readAsDataURL blob)))))
      (j/assoc! reader :onerror #(when on-error (on-error %)))
      (j/call reader :readAsArrayBuffer file))))

(defn get-file
  "Get file data by ID."
  [file-id]
  (js/Promise.
    (fn [resolve reject]
      (when-let [db (get-db)]
        (let [tx (j/call db :transaction #js [files-store] "readonly")
              store (j/call tx :objectStore files-store)
              request (j/call store :get file-id)]
          (j/assoc! request :onsuccess
                    (fn [event]
                      (let [result (j/get-in event [:target :result])]
                        (resolve (when result
                                   {:id (j/get result :id)
                                    :name (j/get result :name)
                                    :type (j/get result :type)
                                    :mime_type (j/get result :mime_type)
                                    :data (j/get result :data)})))))
          (j/assoc! request :onerror #(reject %)))))))

(defn get-files-by-type
  "Get all files of a specific type (image or model)."
  [type on-complete]
  (when-let [db (get-db)]
    (let [tx (j/call db :transaction #js [files-store] "readonly")
          store (j/call tx :objectStore files-store)
          index (j/call store :index "type")
          request (j/call index :getAll (name type))]
      (j/assoc! request :onsuccess
                (fn [event]
                  (let [results (j/get-in event [:target :result])]
                    (doseq [r results]
                      (when on-complete
                        (on-complete {:name (j/get r :name)
                                      :url (str "local-file://" (j/get r :id))})))))))))

(defn resolve-local-file-url
  "Resolve a local-file:// URL to actual data.
   If it's not a local-file URL, returns the original URL."
  [url]
  (if (and url (str/starts-with? url "local-file://"))
    (let [file-id (subs url 13)]
      (js/Promise.
        (fn [resolve _]
          (-> (get-file file-id)
              (j/call :then (fn [file]
                              (resolve (or (:data file) url))))
              (j/call :catch (fn [_] (resolve url)))))))
    (js/Promise.resolve url)))

(defn resolve-local-file->file
  "Resolve a local-file:// URL to a js/File (preserves original filename/extension).
   If not local-file://, resolves to nil."
  [url]
  (if (and url (str/starts-with? url "local-file://"))
    (let [file-id (subs url 13)]
      (-> (get-file file-id)
          (j/call :then
                  (fn [{:keys [data name mime_type]}]
                    (if (and data name)
                      (-> (js/fetch data)
                          (j/call :then (fn [resp] (j/call resp :blob)))
                          (j/call :then (fn [blob]
                                          (js/File. #js [blob] name #js {:type (or mime_type (j/get blob :type))}))))
                      nil)))
          (j/call :catch (fn [_] nil))))
    (js/Promise.resolve nil)))

;; ============ Settings ============

(defn save-setting
  "Save a setting to IndexedDB."
  [key value]
  (js/Promise.
    (fn [resolve reject]
      (when-let [db (get-db)]
        (let [tx (j/call db :transaction #js [settings-store] "readwrite")
              store (j/call tx :objectStore settings-store)
              record #js {:key key :value (pr-str value)}
              request (j/call store :put record)]
          (j/assoc! request :onsuccess #(resolve true))
          (j/assoc! request :onerror #(reject %)))))))

(defn get-setting
  "Get a setting from IndexedDB."
  [key]
  (js/Promise.
    (fn [resolve reject]
      (when-let [db (get-db)]
        (let [tx (j/call db :transaction #js [settings-store] "readonly")
              store (j/call tx :objectStore settings-store)
              request (j/call store :get key)]
          (j/assoc! request :onsuccess
                    (fn [event]
                      (let [result (j/get-in event [:target :result])]
                        (resolve (when result
                                   (reader/read-string (j/get result :value)))))))
          (j/assoc! request :onerror #(reject %)))))))

(defn get-current-presentation-id
  "Get the ID of the last used presentation."
  []
  (get-setting "current-presentation-id"))

(defn set-current-presentation-id
  "Set the ID of the current presentation."
  [id]
  (save-setting "current-presentation-id" id))

;; ============ Initialization ============

(defn init
  "Initialize the local storage system."
  []
  (init-db))

(comment
  ;; Test initialization
  (init)

  ;; Test saving presentation
  (save-presentation {:id "test-123"
                      :title "Test Presentation"
                      :data [{:id "slide-1" :data {:camera {:position [0 5 -10]}}}]})

  ;; Test getting presentation
  (-> (get-presentation "test-123")
      (.then #(js/console.log "Got presentation:" %)))

  ;; Test getting all presentations
  (-> (get-all-presentations)
      (.then #(js/console.log "All presentations:" (clj->js %))))
  )
