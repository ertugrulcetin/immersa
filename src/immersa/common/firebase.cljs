(ns immersa.common.firebase
  (:refer-clojure :exclude [list])
  (:require
    ["/immersa/vendor/utils" :as utils]
    ["@firebase/firestore" :refer [getFirestore runTransaction collection getDoc doc setDoc getDocs query where]]
    ["firebase/app" :refer [initializeApp]]
    ["firebase/storage" :refer [getStorage uploadString ref getDownloadURL uploadBytes uploadBytesResumable listAll list]]
    [applied-science.js-interop :as j]
    [clojure.string :as str]))

(defonce app nil)
(defonce storage nil)
(defonce db nil)

(defn get-download-url [path]
  (-> (getDownloadURL (ref storage path))
      (j/call :then (fn [url]
                      (println "Url: " url)))
      (j/call :catch (fn [e]
                       (println "Firebase download URL error")
                       (js/console.error e)))))

(defn init-app []
  (set! app (initializeApp #js {:apiKey "AIzaSyDVBneQ2EVdElUSdSU1xBQYsk5AaOZo2Uc"
                                :authDomain "immersa-6d29f.firebaseapp.com"
                                :projectId "immersa-6d29f"
                                :storageBucket "immersa-6d29f.appspot.com"
                                :messagingSenderId "673288914536"
                                :appId "1:673288914536:web:d8fab9505ffc63b0a65ddd"
                                :measurementId "G-T62SN00GF8"}))
  (set! storage (getStorage))
  (set! db (getFirestore app)))

(defn upload-file [{:keys [user-id
                           file
                           type
                           task-state
                           on-progress
                           on-error
                           on-complete]}]
  (let [type-prefix (case type
                      :image "images/user/"
                      :model "models/user/")
        storage-ref (ref storage (str type-prefix user-id "/" (j/get file :name)) #js{:timestamp (-> (js/Date.)
                                                                                                     (j/call :toISOString))})
        task (uploadBytesResumable storage-ref file)]
    (reset! task-state task)
    (.on task "state_changed"
         (fn [snapshot]
           (let [percentage (* (/ (j/get snapshot :bytesTransferred) (j/get snapshot :totalBytes)) 100)
                 percentage (int percentage)]
             (when on-progress (on-progress percentage))
             (println "Upload is " percentage "% done")))
         (fn [err]
           (when on-error (on-error err))
           (println "Upload error:" err)
           (js/console.error err))
         (fn []
           (println "Upload complete!")
           (-> (getDownloadURL storage-ref)
               (j/call :then (fn [url]
                               (when on-complete (on-complete url))
                               (println "File available at" url)))
               (j/call :catch (fn [e]
                                (when on-error (on-error e))
                                (println "Firebase download URL error")
                                (js/console.error e))))))))

(defn get-last-uploaded-files [{:keys [type user-id on-complete]}]
  (let [type-prefix (case type
                      :image "images/user/"
                      :model "models/user/")
        storage-ref (ref storage (str type-prefix user-id "/"))
        images (list storage-ref #js {:maxResults 20})]
    (j/call images :then (fn [result]
                           (let [items (j/get result :items)]
                             (doseq [item items]
                               (-> (getDownloadURL item)
                                   (j/call :then (fn [url]
                                                   (when on-complete
                                                     (on-complete {:name (j/get item :name)
                                                                   :url url}))))
                                   (j/call :catch (fn [e]
                                                    (println "Firebase download URL error")
                                                    (js/console.error e))))))))))

(defn upload-presentation [{:keys [user-id
                                   presentation-id
                                   presentation-data]}]
  (let [type-prefix "presentations/user/"
        path (str type-prefix user-id "/" presentation-id "/" presentation-id ".edn")
        storage-ref (ref storage path #js{:timestamp (-> (js/Date.)
                                                         (j/call :toISOString))})]
    (uploadString storage-ref (pr-str presentation-data))))

(defn upload-thumbnail [{:keys [user-id
                                slide-id
                                presentation-id
                                thumbnail]}]
  (let [type-prefix "presentations/user/"
        path (str type-prefix user-id "/" presentation-id "/thumbnails/" slide-id ".webp")
        storage-ref (ref storage path #js{:timestamp (-> (js/Date.)
                                                         (j/call :toISOString))})]
    (uploadBytes storage-ref (utils/base64ToBlob thumbnail "webp"))))

(defn get-presentation [id user-id]
  (getDownloadURL (ref storage (str "presentations/user/" user-id "/" id "/" id ".edn"))))

(defn get-thumbnails [{:keys [presentation-id user-id on-complete]}]
  (let [type-prefix "presentations/user/"
        storage-ref (ref storage (str type-prefix user-id "/" presentation-id "/thumbnails/"))
        images (list storage-ref)]
    (j/call images :then (fn [result]
                           (let [items (j/get result :items)]
                             (doseq [item items]
                               (-> (getDownloadURL item)
                                   (j/call :then (fn [url]
                                                   (let [[slide-id] (str/split (j/get item :name) #"\.webp")]
                                                     (when on-complete (on-complete slide-id url))
                                                     (println "Thumbnail URL: " url))))
                                   (j/call :catch (fn [e]
                                                    (println "Firebase download URL error")
                                                    (js/console.error e))))))))))

(defn get-user [user-id]
  (-> (doc db "users" user-id) getDoc))

(defn get-presentation-info [user-id]
  (getDocs (query (collection db "presentations") (where "user_id" "==" user-id))))

(defn set-user [user]
  (let [ref (doc db "users" (:id user))]
    (setDoc ref (clj->js user) #js{:merge true})))

(defn set-presentation [data]
  (let [ref (doc db "presentations" (:id data))]
    (setDoc ref (clj->js data) #js{:merge true})))

(defn init-user-and-presentation [{:keys [user presentation]}]
  (runTransaction
    db
    (fn [tx]
      (let [user-ref (doc db "users" (:id user))
            presentation-ref (doc db "presentations" (:id presentation))]
        (j/call tx :set user-ref (clj->js user))
        (j/call tx :set presentation-ref (clj->js (assoc presentation :user_id (:id user))))
        (js/Promise.resolve)))))

(defn update-presentation-title [{:keys [user-id presentation-id title]}]
  (set-presentation {:id presentation-id
                     :title title
                     :user_id user-id}))

(comment
  (j/call (get-user "!23")
          :then (fn []
                  (println "Success!"))
          :catch (fn []
                   (println "Something is off")))
  (init-app)
  (get-download-url "images/schaltbau/logo.png")
  (get-last-uploaded-files "user_2c20lPttd6jIbGObJJhbKwN3tLh")
  )
