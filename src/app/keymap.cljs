(ns app.keymap
  (:require
   [clojure.string :as s]
   [re-frame.core :as rf]
   [utils.core :refer [in? sjoin next-cyclic]]
   [app.regs]
   [app.fs :as fs]
   [app.styles :as css]
   [app.search :as sr]
   [app.paredit-cm :as p]
   [app.panel :as panel]
   ))

(enable-console-print!)

(defn activate [{:keys [active editor prev] :as prm}]
  (rf/dispatch [:ide-file-editor-change [active editor]])
  (rf/dispatch [:prev-file-change active])
  (rf/dispatch [:active-file-change prev]))

(defn next-active [{:keys [active editor open-files] :as prm}]
  (let [prev (next-cyclic (.indexOf open-files active) open-files)]
    (activate (conj prm {:prev prev}))))

(defn alternate-active [{:keys [active editor open-files] :as prm}]
 (let [prev @(rf/subscribe [:prev-file])]
   (if (= prev active)
     (next-active prm)
     (activate (conj prm {:prev prev})))))

(defn insert-sexp [{:keys [editor sexp n-chars-back]}]
  (let [doc (.-doc editor)]
    (.replaceSelection doc sexp)
    (let [pos (.getCursor doc)]
      (.setCursor doc (clj->js {:line (.-line pos)
                                :ch (- (.-ch pos) n-chars-back)})))))

(defn kill-buffer [editor]
  ;; (println "Cmd-K")
  (println "active-file" @(rf/subscribe [:active-file])))

(defn move [editor codemirror-cmd]
  (.setOption editor "styleActiveLine" false)
  (.execCommand editor codemirror-cmd)
  ;; invoking js/CodeMirror.Pass makes scrolling much faster; but the cursor
  ;; moves twice
  #_js/CodeMirror.Pass
  )

(defn up       [editor] (move editor "goLineUp"))
(defn down     [editor] (move editor "goLineDown"))
(defn left     [editor] (move editor "goCharLeft"))
(defn right    [editor] (move editor "goCharRight"))
(defn home     [editor] (move editor "goLineStartSmart"))
(defn end      [editor] (move editor "goLineEnd"))
(defn pageup   [editor] (move editor "goPageUp"))
(defn pagedown [editor] (move editor "goPageDown"))
(defn file-end [editor] (move editor "goDocEnd"))
(defn file-beg [editor] (move editor "goDocStart"))

(defn keymap
  "CodeMirror only keymap. Global shortcuts must be configured elsewhere"
  [file open-files]
  (->>
   conj
   {}
   (->>
    (conj
     {
      :Up up
      :Down down
      ;; TODO Mouse movements change cursor pos
      :Left left
      :Right right
      :Ctrl-Left (fn [editor] (move editor "goWordLeft"))
      :Ctrl-Right (fn [editor] (move editor "goWordRight"))
      :PageUp pageup
      :PageDown pagedown
      :Home home
      :End end
      :Ctrl-Home file-beg
      :Ctrl-End file-end
      }
     {
      :Ctrl-B (fn [editor] (panel/addPanel editor "bottom"))
      (keyword "Cmd-;") (fn [editor] (.execCommand editor "toggleComment"))
      :Cmd-Ctrl-F (fn [editor] (sr/search editor))
      :Cmd-K kill-buffer
      ;; :Ctrl-W (fn [editor] (println "Ctrl-W"))

      ;; single key: <S>
      ;; :Mod (fn [editor] (println "Mod"))

      ;; :F11 (fn [editor] (println "Full screen: Can be stolen"))
      :Cmd-Ctrl-Up (fn [editor] (rf/dispatch [:tabs-pos-change :css/tabs-on-top]))
      :Cmd-Ctrl-Down (fn [editor] (rf/dispatch [:tabs-pos-change :css/no-tabs]))
      :Cmd-Ctrl-Left (fn [editor] (rf/dispatch [:tabs-pos-change :css/tabs-left]))
      :Cmd-Ctrl-Right (fn [editor] (rf/dispatch [:tabs-pos-change :css/tabs-right]))
      :Cmd-F (fn [editor]
               ;; assigning file to file causes file-reload
               (rf/dispatch [:active-file-change file])
               (fs/read file editor open-files))
      ;; :Ctrl-R (fn [editor] (println "Can be stolen"))
      ;; :Shift-Ctrl-I (fn [editor] (println "Can be stolen"))
      :Cmd-Tab (fn [editor] (alternate-active {:active @(rf/subscribe [:active-file]) :editor editor :open-files open-files}))
      :Cmd-S (fn [editor] (fs/save file (.getValue (.-doc editor))))
      :Shift-Ctrl-S (fn [editor] (fs/save-ide-settings))
      :Cmd-Q (fn [editor] (next-active {:active @(rf/subscribe [:active-file]) :editor editor :open-files open-files}))
      :Cmd-Left (fn [editor] (p/backward-sexp editor))
      :Cmd-Right (fn [editor] (p/forward-sexp editor))
      :Cmd-Up (fn [editor] (p/forward-sexp editor))
      :Cmd-Ctrl-Alt-P (fn [editor] #_(p/forward-sexp editor))
      :Cmd-Ctrl-Alt-K (fn [editor] (fs/exec ["pkill" "--full" "boot"]))
      :Cmd-Ctrl-Alt-L (fn [editor] (fs/exec ["pgrep" "--full" "boot"]))
      :Shift-Cmd-D (fn [editor]
                     (println editor (->> editor .-options .-styleActiveLine))
                     #_(fs/exec ["ls" "-la"]))
      :Cmd-Ctrl-Alt-B (fn [editor] (fs/exec ["boot" "watch" "dev-build"]))
      :Cmd-Ctrl-P (fn [editor] (insert-sexp {:editor editor
                                            :sexp "(println \"\")"
                                            :n-chars-back 2}))

      :Cmd-Ctrl-L (fn [editor] (insert-sexp {:editor editor
                                            :sexp "(let [])"
                                             :n-chars-back 2}))
      }
     (->>
      (map-indexed (fn [i file]
                     {(keyword (str "Cmd-" (inc i)))
                      (fn [editor] (rf/dispatch [:active-file-change file]))})
                   open-files)
      (into {})))
    #_(map (fn [[k v]]
           {k (fn [editor] (println "key-chord" (name k)) (v editor))}))
    (into {}))
   clj->js))
