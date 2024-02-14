(ns immersa.ui.icons
  (:require
    ["@phosphor-icons/react/dist/csr/ArrowFatLeft" :refer [ArrowFatLeft]]
    ["@phosphor-icons/react/dist/csr/ArrowFatRight" :refer [ArrowFatRight]]
    ["@phosphor-icons/react/dist/csr/Books" :refer [Books]]
    ["@phosphor-icons/react/dist/csr/Camera" :refer [Camera]]
    ["@phosphor-icons/react/dist/csr/CaretLeft" :refer [CaretLeft]]
    ["@phosphor-icons/react/dist/csr/CaretRight" :refer [CaretRight]]
    ["@phosphor-icons/react/dist/csr/ChatCenteredText" :refer [ChatCenteredText]]
    ["@phosphor-icons/react/dist/csr/ChatsCircle" :refer [ChatsCircle]]
    ["@phosphor-icons/react/dist/csr/Command" :refer [Command]]
    ["@phosphor-icons/react/dist/csr/Cube" :refer [Cube]]
    ["@phosphor-icons/react/dist/csr/DotsThreeVertical" :refer [DotsThreeVertical]]
    ["@phosphor-icons/react/dist/csr/Image" :refer [Image]]
    ["@phosphor-icons/react/dist/csr/Info" :refer [Info]]
    ["@phosphor-icons/react/dist/csr/Lightbulb" :refer [Lightbulb]]
    ["@phosphor-icons/react/dist/csr/List" :refer [List]]
    ["@phosphor-icons/react/dist/csr/LockSimple" :refer [LockSimple]]
    ["@phosphor-icons/react/dist/csr/Play" :refer [Play]]
    ["@phosphor-icons/react/dist/csr/PlusCircle" :refer [PlusCircle]]
    ["@phosphor-icons/react/dist/csr/ShareNetwork" :refer [ShareNetwork]]
    ["@phosphor-icons/react/dist/csr/SmileySticker" :refer [SmileySticker]]
    ["@phosphor-icons/react/dist/csr/Student" :refer [Student]]
    ["@phosphor-icons/react/dist/csr/TextT" :refer [TextT]]
    ["@phosphor-icons/react/dist/csr/X" :refer [X]]
    [reagent.core :as r]))

(def prev (r/adapt-react-class CaretLeft))
(def next (r/adapt-react-class CaretRight))
(def dots (r/adapt-react-class DotsThreeVertical))
(def chat (r/adapt-react-class ChatCenteredText))
(def smiley (r/adapt-react-class SmileySticker))
(def control (r/adapt-react-class Command))
(def arrow-left (r/adapt-react-class ArrowFatLeft))
(def arrow-right (r/adapt-react-class ArrowFatRight))
(def list-menu (r/adapt-react-class List))
(def text (r/adapt-react-class TextT))
(def image (r/adapt-react-class Image))
(def cube (r/adapt-react-class Cube))
(def light (r/adapt-react-class Lightbulb))
(def share (r/adapt-react-class ShareNetwork))
(def play (r/adapt-react-class Play))
(def lock (r/adapt-react-class LockSimple))
(def plus (r/adapt-react-class PlusCircle))
(def books (r/adapt-react-class Books))
(def x (r/adapt-react-class X))
(def chats-circle (r/adapt-react-class ChatsCircle))
(def student (r/adapt-react-class Student))
(def camera (r/adapt-react-class Camera))
(def info (r/adapt-react-class Info))
