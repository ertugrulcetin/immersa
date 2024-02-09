(ns immersa.scene.api.constant
  (:require
    ["@babylonjs/core/Events/keyboardEvents" :refer [KeyboardEventTypes]]
    ["@babylonjs/core/Events/pointerEvents" :refer [PointerEventTypes]]
    ["@babylonjs/core/Materials/GreasedLine/greasedLineMaterialInterfaces" :refer [GreasedLineMeshColorMode
                                                                                   GreasedLineMeshMaterialType]]
    ["@babylonjs/core/Materials/material" :refer [Material]]
    ["@babylonjs/core/Maths/math" :refer [Vector2 Vector3 Vector4]]
    ["@babylonjs/core/Maths/math.color" :refer [Color3]]
    ["@babylonjs/core/Meshes/mesh" :refer [Mesh]]
    ["@babylonjs/core/Particles/particleSystem" :refer [ParticleSystem]]
    [applied-science.js-interop :as j]))

(def v3-up (j/call Vector3 :Up))
(def v3-forward (j/call Vector3 :Forward))
(def v3-left (j/call Vector3 :Left))
(def v3-right (j/call Vector3 :Right))

(def animation-type-v3 :ANIMATIONTYPE_VECTOR3)
(def animation-type-float :ANIMATIONTYPE_FLOAT)
(def animation-loop-cons :ANIMATIONLOOPMODE_CONSTANT)
(def animation-loop-cycle :ANIMATIONLOOPMODE_CYCLE)

(def easing-ease-in :EASINGMODE_EASEIN)
(def easing-ease-out :EASINGMODE_EASEOUT)
(def easing-ease-in-out :EASINGMODE_EASEINOUT)

(def gui-horizontal-align-left :HORIZONTAL_ALIGNMENT_LEFT)
(def gui-horizontal-align-center :HORIZONTAL_ALIGNMENT_CENTER)
(def gui-vertical-align-center :VERTICAL_ALIGNMENT_CENTER)
(def gui-vertical-align-top :VERTICAL_ALIGNMENT_TOP)

(def gui-text-wrapping-word-wrap :WordWrap)

(def mesh-billboard-mode-all (j/get Mesh :BILLBOARDMODE_ALL))

(defn color-white [] (j/call Color3 :White))
(defn color-black [] (j/call Color3 :Black))
(defn color-yellow [] (j/call Color3 :Yellow))
(defn color-red [] (j/call Color3 :Red))
(defn color-teal [] (j/call Color3 :Teal))
(defn color-gray [] (j/call Color3 :Gray))

(def coordinates-mode-skybox :SKYBOX_MODE)

(def mesh-default-side (j/get Mesh :DEFAULTSIDE))
(def mesh-double-side (j/get Mesh :DOUBLESIDE))

(def mat-alpha-blend (j/get Material :MATERIAL_ALPHABLEND))
(def mat-alpha-test-and-blend (j/get Material :ATERIAL_ALPHATESTANDBLEND))

(def pointer-type-down (j/get PointerEventTypes :POINTERDOWN))
(def pointer-type-up (j/get PointerEventTypes :POINTERUP))

(def keyboard-type-key-down (j/get KeyboardEventTypes :KEYDOWN))
(def keyboard-type-key-up (j/get KeyboardEventTypes :KEYUP))

(def particle-blend-mode-standard (j/get ParticleSystem :BLENDMODE_STANDARD))

(def greased-line-material-pbr (j/get GreasedLineMeshMaterialType :MATERIAL_TYPE_PBR))
(def greased-line-color-mode-multi (j/get GreasedLineMeshColorMode :COLOR_MODE_MULTIPLY))
