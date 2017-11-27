(ns app.keymap
  (:require
   [clojure.string :as s]
   [re-frame.core :as rf]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   [app.regs]
   [app.fs :as fs]
   [app.styles :as css]
   [app.search :as sr]
   [app.paredit-cm :as p]
   [app.panel :as panel]
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

(defn insert-sexp [{:keys [editor sexp n-chars-back]}]
  (let [doc (.-doc editor)]
    (.replaceSelection doc sexp)
    (let [pos (.getCursor doc)]
      (.setCursor doc (clj->js {:line (.-line pos)
                                :ch (- (.-ch pos) n-chars-back)})))))

(defn row [editor] (->> editor .-doc .getCursor .-line))
(defn col [editor] (->> editor .-doc .getCursor .-ch))

(defn kill-buffer [editor]
  ;; (.log js/console "Cmd-K")
  (.log js/console "active-file" @(rf/subscribe [:active-file])))

(defn active-line-off [editor]
  (.setOption editor "styleActiveLine" false))

(defn up-key [editor]
  (active-line-off editor)
  (let [pos (->> editor .-doc .getCursor)]
    (rf/dispatch [:cursor-change {:r (.-line pos) :c (.-ch pos)}]))
  js/CodeMirror.Pass)

(defn down-key [editor]
  (active-line-off editor)
  (let [pos (->> editor .-doc .getCursor)]
    (rf/dispatch [:cursor-change {:r (.-line pos) :c (.-ch pos)}]))
  js/CodeMirror.Pass)

(defn left-key [editor]
  (let [pos (->> editor .-doc .getCursor)]
    (rf/dispatch [:cursor-change {:r (.-line pos) :c (.-ch pos)}]))
  js/CodeMirror.Pass)

(defn right-key [editor]
  (let [pos (->> editor .-doc .getCursor)]
    (rf/dispatch [:cursor-change {:r (.-line pos) :c (.-ch pos)}]))
  js/CodeMirror.Pass)

(defn keymap
  "CodeMirror only keymap. Global shortcuts must be configured elsewhere"
  [file open-files]
  (->>
   conj
   {}
   (->>
    (conj
     {
      :Up up-key
      :Down down-key
      ;; TODO PgUp, PgDown, Home, End keys; Mouse movements change cursor pos
      :Left left-key
      :Right right-key
      }
     {
      :Ctrl-B (fn [editor] (panel/addPanel editor "bottom"))
      (keyword "Cmd-;") (fn [editor] (.execCommand editor "toggleComment"))
      :Cmd-Ctrl-F (fn [editor] (sr/search editor))
      :Ctrl-Left (fn [editor] (.execCommand editor "goWordLeft"))
      :Ctrl-Right (fn [editor] (.execCommand editor "goWordRight"))
      :Cmd-K kill-buffer
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
      :Shift-Cmd-D (fn [editor]
                     (.log js/console editor (->> editor .-options .-styleActiveLine))
                     #_(fs/exec ["ls" "-la"]))
      :Cmd-Ctrl-Alt-B (fn [editor] (fs/exec ["boot" "watch" "dev-build"]))
      :Cmd-Ctrl-P (fn [editor] (insert-sexp {:editor editor
                                            :sexp "(.log js/console \"\")"
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

