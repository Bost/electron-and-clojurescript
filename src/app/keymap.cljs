(ns app.keymap
  (:require
   [clojure.string :as s]
   [re-frame.core :as rf]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   [app.regs]
   [app.fs :as fs]
   [app.styles :as css]
   ))

(defn next-active [editor open-files]
  (let [active @(rf/subscribe [:active-file])
        idx (.indexOf open-files active)]
    (rf/dispatch [:prev-file-change active])
    (rf/dispatch [:active-file-change (next-cyclic idx open-files)])))

(defn alternate-active [editor open-files]
 (let [prev @(rf/subscribe [:prev-file])
       active @(rf/subscribe [:active-file])]
   (if (= prev active)
     (next-active editor open-files)
     (do
       (rf/dispatch [:prev-file-change active])
       (rf/dispatch [:active-file-change prev])))))

(defn keymap [fs file open-files]
  #js
  {
   ;; :Ctrl-W (fn [editor] (.log js/console "Ctrl-W"))

   ;; single key: <S>
   ;; :Mod (fn [editor] (.log js/console "Mod"))

   :Cmd-Up
   (fn [editor]
     (.log js/console "Cmd-Up / <s-up>")
     (rf/dispatch [:tabs-pos-change css/up]))
   :Cmd-Down
   (fn [editor]
     (.log js/console "Cmd-Down / <s-down>")
     (rf/dispatch [:tabs-pos-change css/down]))
   :Cmd-Left
   (fn [editor]
     (.log js/console "Cmd-Left / <s-left>")
     (rf/dispatch [:tabs-pos-change css/left]))
   :Cmd-Right
   (fn [editor]
     (.log js/console "Cmd-Right / <s-right>")
     (rf/dispatch [:tabs-pos-change css/right]))
   :Cmd-F
   (fn [editor]
     (.log js/console "Cmd-F / <S-f>")
     ;; assigning file to file causes file-reload
     (rf/dispatch [:active-file-change file])
     (fs/read fs file editor open-files))
   ;; :Ctrl-R
   ;; (fn [editor]
   ;;   (.log js/console "Ctrl-R / <C-r>")
   ;;   (.log js/console "Can be stolen"))
   ;; :Shift-Ctrl-I
   ;; (fn [editor]
   ;;   (.log js/console "Shif-Ctrl-I / <C-S-i>")
   ;;   (.log js/console "Can be stolen"))
   :Cmd-Tab
   (fn [editor]
     (.log js/console "Cmd-Tab / <s-tab>")
     (alternate-active editor open-files))
   :Cmd-S
   (fn [editor]
     (.log js/console "Cmd-S / <S-s>")
     (fs/save fs file (.getValue (.-doc editor))))
   :Cmd-Q
   (fn [editor]
     (.log js/console "Cmd-Q / <S-q>")
     (next-active editor open-files))
   })


