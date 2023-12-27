(ns immersa.scene.api.gui
  (:require
    ["@babylonjs/gui/2D" :refer [AdvancedDynamicTexture Control TextWrapping]]
    ["@babylonjs/gui/2D/controls" :refer [Button Image Rectangle TextBlock]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core])
  (:require-macros
    [immersa.scene.macros :as m]))

(defn advanced-dynamic-texture []
  (let [advanced-texture (j/call-in AdvancedDynamicTexture [:CreateFullscreenUI] "UI")]
    (j/assoc! api.core/db :gui-advanced-texture advanced-texture)
    advanced-texture))

(defn image [name url]
  (Image. name url))

(defn button [name text]
  (j/call Button :CreateSimpleButton name text))

(defn add-control [container control]
  (j/call container :addControl control))

(defn rectangle [name & {:keys [corner-radius
                                background
                                height]
                         :as opts}]
  (let [rect (Rectangle. name)]
    (api.core/add-node-to-db name rect opts)
    (m/cond-doto rect
      height (j/assoc! :height height)
      corner-radius (j/assoc! :cornerRadius corner-radius)
      background (j/assoc! :background background))))

(defn create-for-mesh [mesh & {:keys [width height]}]
  (j/call AdvancedDynamicTexture :CreateForMesh mesh width height))

;; TODO fix resolution
;; function adjustTextSize(textBlock, baseScreenResolution) {
;;    let currentResolution = window.innerWidth; // or window.innerHeight based on your preference
;;    let scaleFactor = currentResolution / baseScreenResolution;
;;    textBlock.fontSizeInPixels = originalFontSize * scaleFactor;
;; }
;;
;; // Usage
;; const textBlock = new BABYLON.GUI.TextBlock();
;; textBlock.text = "Your Text Here";
;; let originalFontSize = 24; // Define your base font size
;; textBlock.fontSizeInPixels = originalFontSize;
;; adjustTextSize(textBlock, 1920); // Assuming 1920 is the base screen width you designed for
(defn text-block [name & {:keys [text
                                 alpha
                                 font-family
                                 font-size-in-pixels
                                 text-wrapping
                                 text-horizontal-alignment
                                 text-vertical-alignment
                                 padding-top
                                 padding-bottom
                                 padding-right
                                 padding-left
                                 font-size
                                 line-spacing
                                 color
                                 font-weight]
                          :as opts}]
  (let [text-block (TextBlock. name text)]
    (api.core/add-node-to-db name text-block (assoc opts :type :text))
    (m/cond-doto text-block
      font-size-in-pixels (j/assoc! :fontSizeInPixels font-size-in-pixels)
      text-wrapping (j/assoc! :textWrapping (j/get TextWrapping text-wrapping))
      text-horizontal-alignment (j/assoc! :textHorizontalAlignment (j/get Control text-horizontal-alignment))
      text-vertical-alignment (j/assoc! :textVerticalAlignment (j/get Control text-vertical-alignment))
      alpha (j/assoc! :alpha alpha)
      font-family (j/assoc! :fontFamily font-family)
      line-spacing (j/assoc! :lineSpacing line-spacing)
      padding-top (j/assoc! :paddingTop padding-top)
      padding-bottom (j/assoc! :paddingBottom padding-bottom)
      padding-right (j/assoc! :paddingRight padding-right)
      padding-left (j/assoc! :paddingLeft padding-left)
      font-size (j/assoc! :fontSize font-size)
      color (j/assoc! :color color)
      font-weight (j/assoc! :fontWeight font-weight))))
