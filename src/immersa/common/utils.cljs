(ns immersa.common.utils
  (:require
    [applied-science.js-interop :as j]))

(defonce db (atom {}))

(defn register-event-listener [element type f]
  (j/call element :addEventListener type f)
  (swap! db update :event-listeners (fnil conj []) [element type f])
  f)

(defn remove-element-listeners []
  (doseq [[element type f] (:event-listeners @db)]
    (j/call element :removeEventListener type f))
  (swap! db assoc :event-listeners [])
  (js/console.log "All events listeners removed"))

(defn save-canvas-as-webp [canvas-id & {:keys [scale-factor quality]
                                        :or {scale-factor 0.5
                                             quality 0.75}}]
  (let [canvas (js/document.getElementById canvas-id)
        ctx (j/call canvas :getContext "2d")]
    (j/assoc! canvas :width (* (j/get canvas :width) scale-factor))
    (j/assoc! canvas :height (* (j/get canvas :height) scale-factor))
    (j/call ctx :drawImage canvas 0 0 (j/get canvas :width) (j/get canvas :height))
    (j/call canvas :toDataURL "image/webp" quality)))

(defn number->fixed [n]
  (j/call n :toFixed 2))
