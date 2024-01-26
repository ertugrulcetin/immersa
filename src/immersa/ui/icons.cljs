(ns immersa.ui.icons
  (:require
    ["@phosphor-icons/react/dist/csr/ArrowFatLeft" :refer [ArrowFatLeft]]
    ["@phosphor-icons/react/dist/csr/ArrowFatRight" :refer [ArrowFatRight]]
    ["@phosphor-icons/react/dist/csr/CaretLeft" :refer [CaretLeft]]
    ["@phosphor-icons/react/dist/csr/CaretRight" :refer [CaretRight]]
    ["@phosphor-icons/react/dist/csr/ChatCenteredText" :refer [ChatCenteredText]]
    ["@phosphor-icons/react/dist/csr/Command" :refer [Command]]
    ["@phosphor-icons/react/dist/csr/Cube" :refer [Cube]]
    ["@phosphor-icons/react/dist/csr/DotsThreeVertical" :refer [DotsThreeVertical]]
    ["@phosphor-icons/react/dist/csr/Image" :refer [Image]]
    ["@phosphor-icons/react/dist/csr/Lightbulb" :refer [Lightbulb]]
    ["@phosphor-icons/react/dist/csr/List" :refer [List]]
    ["@phosphor-icons/react/dist/csr/LockSimple" :refer [LockSimple]]
    ["@phosphor-icons/react/dist/csr/Play" :refer [Play]]
    ["@phosphor-icons/react/dist/csr/PlusCircle" :refer [PlusCircle]]
    ["@phosphor-icons/react/dist/csr/ShareNetwork" :refer [ShareNetwork]]
    ["@phosphor-icons/react/dist/csr/SmileySticker" :refer [SmileySticker]]
    ["@phosphor-icons/react/dist/csr/TextT" :refer [TextT]]
    [reagent.core :as r]))

(def prev (r/adapt-react-class CaretLeft))
(def next (r/adapt-react-class CaretRight))
(def dots (r/adapt-react-class DotsThreeVertical))
(def chat (r/adapt-react-class ChatCenteredText))
(def smiley (r/adapt-react-class SmileySticker))
(def control (r/adapt-react-class Command))
(def arrow-left (r/adapt-react-class ArrowFatLeft))
(def arrow-right (r/adapt-react-class ArrowFatRight))
(def list (r/adapt-react-class List))
(def text (r/adapt-react-class TextT))
(def image (r/adapt-react-class Image))
(def cube (r/adapt-react-class Cube))
(def light (r/adapt-react-class Lightbulb))
(def share (r/adapt-react-class ShareNetwork))
(def play (r/adapt-react-class Play))
(def lock (r/adapt-react-class LockSimple))
(def plus (r/adapt-react-class PlusCircle))
