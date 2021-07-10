(ns app.editor
  (:require ["@codemirror/closebrackets" :refer [closeBrackets]]
            ["@codemirror/fold" :as fold]
            ["@codemirror/gutter" :refer [lineNumbers]]
            ["@codemirror/highlight" :as highlight]
            ["@codemirror/history" :refer [history historyKeymap]]
            ["@codemirror/state" :refer [EditorState]]
            ["@codemirror/view" :as view :refer [EditorView]]
            ["lezer" :as lezer]
            ["lezer-generator" :as lg]
            ["lezer-tree" :as lz-tree]
            [applied-science.js-interop :as j]
            [clojure.string :as str]
            [nextjournal.clojure-mode :as cm-clj]
            [app.sci :as sci]
            [nextjournal.clojure-mode.extensions.close-brackets :as close-brackets]
            [nextjournal.clojure-mode.extensions.formatting :as format]
            [nextjournal.clojure-mode.extensions.selection-history :as sel-history]
            [nextjournal.clojure-mode.keymap :as keymap]
            [nextjournal.clojure-mode.live-grammar :as live-grammar]
            [nextjournal.clojure-mode.node :as n]
            [nextjournal.clojure-mode.selections :as sel]
            [nextjournal.clojure-mode.test-utils :as test-utils]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(def theme
  (.theme
   EditorView
   (j/lit {".cm-content" {:white-space "pre-wrap", :padding "10px 0"},
           "&.cm-focused" {:outline "none"},
           ".cm-line" {:padding "0 9px",
                       :line-height "1.6",
                       :font-size "16px",
                       :font-family "var(--code-font)"},
           ".cm-matchingBracket" {:border-bottom "1px solid var(--teal-color)",
                                  :color "inherit"},
           ".cm-gutters" {:background "transparent", :border "none"},
           ".cm-gutterElement" {:margin-left "5px"},
           ;; only show cursor when focused
           ".cm-cursor" {:visibility "hidden"},
           "&.cm-focused .cm-cursor" {:visibility "visible"}})))

(defonce extensions
  #js
  [theme
   (history)
   highlight/defaultHighlightStyle
   (view/drawSelection)
   (lineNumbers)
   (fold/foldGutter)
   (.. EditorState -allowMultipleSelections (of true))
   (if false
     ;; use live-reloading grammar
     #js [(cm-clj/syntax live-grammar/parser)
          (.slice cm-clj/default-extensions 1)]
     cm-clj/default-extensions)
   (.of view/keymap cm-clj/complete-keymap)
   (.of view/keymap historyKeymap)])


(defn editor
  [source !view {:keys [eval?]}]
  (r/with-let
    [last-result (when eval? (r/atom (sci/eval-string source)))
     mount! (fn [el]
              (when el
                (reset! !view (new EditorView
                                   (j/obj :state (test-utils/make-state
                                                  (cond-> #js [extensions]
                                                    eval? (.concat
                                                           #js
                                                           [(sci/extension
                                                             {:modifier "Alt",
                                                              :on-result
                                                              (partial reset! last-result)})]))
                                                  source)


                                          :parent el)))))]
    [:div
     [:div
      {:ref mount!,
       :style {:max-height 500 :background-color "#e3e3e3"}}]
     (when eval?
       [:div
        {:style {:white-space "pre-wrap"
                 :margin-top "0.5rem"
                 :color "#c3c3c3"
                 :font-family "var(--code-font)"}}
        [:span "user=> "]
        (prn-str @last-result)])
     ]
    (finally (j/call @!view :destroy))))