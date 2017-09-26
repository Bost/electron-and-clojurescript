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

(defn keymap
  "CodeMirror only keymap. Global shortcuts must be configured elsewhere"
  [file open-files]
  (->>
   (conj
    {}
    {
     ;; :Ctrl-W (fn [editor] (.log js/console "Ctrl-W"))

     ;; single key: <S>
     ;; :Mod (fn [editor] (.log js/console "Mod"))

     ;; :F11 (fn [editor] (.log js/console "Full screen: Can be stolen"))
     :Cmd-Ctrl-Up (fn [editor] (rf/dispatch [:tabs-pos-change css/tabs-on-top]))
     :Cmd-Ctrl-Down (fn [editor] (rf/dispatch [:tabs-pos-change css/no-tabs]))
     :Cmd-Ctrl-Left (fn [editor] (rf/dispatch [:tabs-pos-change css/left-to-right]))
     :Cmd-Ctrl-Right (fn [editor] (rf/dispatch [:tabs-pos-change css/right-to-left]))
     :Cmd-F (fn [editor]
              ;; assigning file to file causes file-reload
              (rf/dispatch [:active-file-change file])
              (fs/read file editor open-files))
     ;; :Ctrl-R (fn [editor] (.log js/console "Can be stolen"))
     ;; :Shift-Ctrl-I (fn [editor] (.log js/console "Can be stolen"))
     :Cmd-Tab (fn [editor] (alternate-active editor open-files))
     :Cmd-S (fn [editor] (fs/save file (.getValue (.-doc editor))))
     :Shift-Ctrl-S (fn [editor] (fs/save-ide-settings))
     :Cmd-Q (fn [editor] (next-active editor open-files))
     :Cmd-Left (fn [editor] (p/backward-sexp editor))
     :Cmd-Right (fn [editor] (p/forward-sexp editor))
     :Cmd-Up (fn [editor] (p/forward-sexp editor))
     :Cmd-Ctrl-Alt-P (fn [editor] #_(p/forward-sexp editor))
     :Cmd-Ctrl-Alt-K (fn [editor] (fs/exec ["pkill" "--full" "boot"]))
     :Cmd-Ctrl-Alt-L (fn [editor] (fs/exec ["pgrep" "--full" "boot"]))
     :Shift-Cmd-D (fn [editor] (fs/exec ["ls" "-la"]))
     :Cmd-Ctrl-Alt-B (fn [editor] (fs/exec ["boot" "watch" "dev-build"]))

     }
    (->>
     (map-indexed (fn [i file]
                    {(keyword (str "Cmd-" i))
                     (fn [editor] (rf/dispatch [:active-file-change file]))})
                  open-files)
     (into {})))
   (map (fn [[k v]]
          {k (fn [editor] (println "key-comb" (name k)) (v editor))}))
   (into {})
   clj->js))

