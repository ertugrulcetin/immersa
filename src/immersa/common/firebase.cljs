(ns immersa.common.firebase
  (:refer-clojure :exclude [list])
  (:require
    ["firebase/app" :refer [initializeApp]]
    ["firebase/storage" :refer [getStorage ref getDownloadURL uploadBytesResumable listAll list]]
    [applied-science.js-interop :as j]))

(defonce app nil)
(defonce storage nil)

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
  (set! storage (getStorage)))

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

(comment
  (init-app)
  (get-download-url "images/schaltbau/logo.png")
  (get-last-uploaded-files "user_2c20lPttd6jIbGObJJhbKwN3tLh")
  )
