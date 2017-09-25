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

#_(defn key-comb [k f & args]
  {k (fn []
       ;; (.log js/console "key-comb" "editor" editor)
       #_(.log js/console (name k))
       (apply f args))})

(defmacro key-comb [k fun]
  `(let [xk# ~k
         xfun# ~fun]
     (.log js/console (name (quote ~k)))
     {(quote ~k) (quote ~fun)}))

(defn keymap
  "CodeMirror only keymap. Global shortcuts must be configured elsewhere"
  [file open-files]
  (key-comb :Cmd-D (fn [editor] (fs/exec ["ls" "-la"])))
  (->>
   (dbg {:Cmd-D (fn [editor] (fs/exec ["ls" "-la"]))})
   #_(conj
    {}
    (key-comb :Cmd-Ctrl-Up (fn [editor] (rf/dispatch [:tabs-pos-change css/tabs-on-top])))
    #_(key-comb :Ctrl-W (fn [editor]))
    #_(key-comb :Mod (fn [editor])) ;; single key: <S>
    #_(key-comb :F11 (fn [editor] (.log js/console "Full screen: Can be stolen")))
    (key-comb :Cmd-Ctrl-Down (fn [editor] (rf/dispatch [:tabs-pos-change css/no-tabs])))
    (key-comb :Cmd-Ctrl-Left (fn [editor] (rf/dispatch [:tabs-pos-change css/left-to-right])))
    (key-comb :Cmd-Ctrl-Right (fn [editor] (rf/dispatch [:tabs-pos-change css/right-to-left])))
    (key-comb :Cmd-F (fn [editor] ;; assigning file to file causes file-reload
                       (rf/dispatch [:active-file-change file])
                       (fs/read file editor open-files)))
     #_(key-comb :Ctrl-R (fn [editor] (.log js/console "Can be stolen")))
     #_(key-comb :Shift-Ctrl-I (fn [editor] (.log js/console "Can be stolen")))
     (key-comb :Cmd-Tab (fn [editor] (alternate-active editor open-files)))
     (key-comb :Cmd-S (fn [editor] (fs/save file (.getValue (.-doc editor)))))
     (key-comb :Shift-Ctrl-S (fn [editor] (fs/save-ide-settings)))
     (key-comb :Cmd-Q (fn [editor] (next-active editor open-files)))
     (key-comb :Cmd-Left (fn [editor] (p/backward-sexp editor)))
     (key-comb :Cmd-Right (fn [editor] (p/forward-sexp editor)))
     (key-comb :Cmd-Up (fn [editor] (p/forward-sexp editor)))
     #_(key-comb :Cmd-Ctrl-Alt-P (fn [editor] (p/forward-sexp editor)))
     (key-comb :Cmd-Ctrl-Alt-K (fn [editor] (fs/exec ["pkill" "--full" "boot"])))
     (key-comb :Cmd-Ctrl-Alt-L (fn [editor] (fs/exec ["pgrep" "--full" "boot"])))
     (key-comb :Shift-Cmd-D (fn [editor] (fs/exec ["ls" "-la"])))
     (key-comb :Cmd-Ctrl-Alt-B (fn [editor] (fs/exec ["boot" "watch" "dev-build"]))))
   clj->js))
