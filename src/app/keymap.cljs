(ns app.keymap
  (:require
   [clojure.string :as s]
   [re-frame.core :as rf]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   [app.regs]
   [app.fs :as fs]
   [app.styles :as css]
   [app.paredit-cm :as p]
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

(defn key-comb [k f & args]
  {k
   (fn [editor]
     (.log js/console (name k))
     (apply f args))})

(defn keymap
  "CodeMirror only keymap. Global shortcuts must be configured elsewhere"
  [file open-files]
  (->>
   (conj
    {}
    (key-comb :Cmd-Ctrl-Up (fn [editor] (rf/dispatch [:tabs-pos-change css/tabs-on-top])))
    {
     ;; :Ctrl-W (fn [editor] (.log js/console "Ctrl-W"))

     ;; single key: <S>
     ;; :Mod (fn [editor] (.log js/console "Mod"))

     ;; :F11
     ;; (fn [editor]
     ;;   (.log js/console "F11")
     ;;   (.log js/console "Full screen: Can be stolen"))
     #_:Cmd-Ctrl-Up
     #_(fn [editor]
         (.log js/console "Cmd-Ctrl-Up / <s-C-up>")
         (rf/dispatch [:tabs-pos-change css/tabs-on-top]))
     :Cmd-Ctrl-Down
     (fn [editor]
       (.log js/console "Cmd-Ctrl-Down / <s-C-down>")
       (rf/dispatch [:tabs-pos-change css/no-tabs])
       )
     :Cmd-Ctrl-Left
     (fn [editor]
       (.log js/console "Cmd-Ctrl-Left / <s-C-left>")
       (rf/dispatch [:tabs-pos-change css/left-to-right]))
     :Cmd-Ctrl-Right
     (fn [editor]
       (.log js/console "Cmd-Ctrl-Right / <s-C-right>")
       (rf/dispatch [:tabs-pos-change css/right-to-left]))
     :Cmd-F
     (fn [editor]
       (.log js/console "Cmd-F / <S-f>")
       ;; assigning file to file causes file-reload
       (rf/dispatch [:active-file-change file])
       (fs/read file editor open-files))
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
       (fs/save file (.getValue (.-doc editor))))
     :Shift-Ctrl-S
     (fn [editor]
       (.log js/console "Shift-Ctrl-S")
       (fs/save-ide-settings))
     :Cmd-Q
     (fn [editor]
       (.log js/console "Cmd-Q / <S-q>")
       (next-active editor open-files))
     :Cmd-Left
     (fn [editor]
       (.log js/console "Cmd-Left")
       (p/backward-sexp editor))
     :Cmd-Right
     (fn [editor]
       (.log js/console "Cmd-Right")
       (p/forward-sexp editor))
     :Cmd-Up
     (fn [editor]
       (.log js/console "Cmd-Up")
       (p/forward-sexp editor))
     :Cmd-Ctrl-Alt-P
     (fn [editor]
       (.log js/console "Cmd-Ctrl-Alt-P")
       #_(p/forward-sexp editor))
     :Cmd-Ctrl-Alt-K
     (fn [editor]
       (.log js/console "Cmd-Ctrl-Alt-K")
       (fs/exec ["pkill" "--full" "boot"]))
     :Cmd-Ctrl-Alt-L
     (fn [editor]
       (.log js/console "Cmd-Ctrl-Alt-L")
       (fs/exec ["pgrep" "--full" "boot"]))
     :Shift-Cmd-D
     (fn [editor]
       (.log js/console "Shift-Cmd-D")
       (fs/exec ["ls" "-la"]))
     :Cmd-Ctrl-Alt-B
     (fn [editor]
       (.log js/console "Cmd-Ctrl-Alt-B")
       (fs/exec ["boot" "watch" "dev-build"]))
     })
   clj->js))
