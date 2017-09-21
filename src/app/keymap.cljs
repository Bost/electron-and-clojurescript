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

(defn exec [cmd-line]
  (let [cmd (first cmd-line)
        prms (clj->js (rest cmd-line))]
    (.log js/console "$" (sjoin cmd-line))
    (let [spawn (->> (js/require "child_process") .-spawn)
          prc (spawn cmd prms)]
      (js/prc.stdout.setEncoding "utf8")
      (js/prc.stdout.on
       "data" (fn [data] (.log js/console #_"STDOUT" (str data))))
      ;; boot process output gets displayed only on the STDERR
      #_(js/prc.stderr.on
       "data" (fn [data] (.error js/console #_"STDERR" (str data))))
      (js/prc.stdout.on
       "message" (fn [msg] (.log js/console "CHILD got message" msg)))
      (js/prc.stdout.on
       "close" (fn [code] #_(.log js/console "Process close code" code)))
      (js/prc.stdout.on
       "exit" (fn [code] (.log js/console "Process exit code" code))))))

(defn keymap
  "CodeMirror only keymap. Global shortcuts must be configured elsewhere"
  [fs file open-files]
  #js
  {
   ;; :Ctrl-W (fn [editor] (.log js/console "Ctrl-W"))

   ;; single key: <S>
   ;; :Mod (fn [editor] (.log js/console "Mod"))

   ;; :F11
   ;; (fn [editor]
   ;;   (.log js/console "F11")
   ;;   (.log js/console "Full screen: Can be stolen"))
   :Cmd-Up
   (fn [editor]
     (.log js/console "Cmd-Up / <s-up>")
     (rf/dispatch [:tabs-pos-change css/tabs-on-top]))
   :Cmd-Down
   (fn [editor]
     (.log js/console "Cmd-Down / <s-down>")
     (rf/dispatch [:tabs-pos-change css/no-tabs])
     )
   :Cmd-Left
   (fn [editor]
     (.log js/console "Cmd-Left / <s-left>")
     (rf/dispatch [:tabs-pos-change css/left-to-right]))
   :Cmd-Right
   (fn [editor]
     (.log js/console "Cmd-Right / <s-right>")
     (rf/dispatch [:tabs-pos-change css/right-to-left]))
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
   :Cmd-Ctrl-Alt-P
   (fn [editor]
     (.log js/console "Cmd-Ctrl-Alt-P")
     )
   :Cmd-Ctrl-Alt-K
   (fn [editor]
     (.log js/console "Cmd-Ctrl-Alt-K")
     (exec ["pkill" "--full" "boot"]))
   :Cmd-Ctrl-Alt-L
   (fn [editor]
     (.log js/console "Cmd-Ctrl-Alt-L")
     (exec ["pgrep" "--full" "boot"]))
   :Shift-Cmd-D
   (fn [editor]
     (.log js/console "Shift-Shift-D")
     (exec ["ls" "-la"]))
   :Cmd-Ctrl-Alt-B
   (fn [editor]
     (.log js/console "Cmd-Ctrl-Alt-B")
     (exec ["boot" "watch" "dev-build"]))
   })
