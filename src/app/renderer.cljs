(ns app.renderer
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as s]
   #_[clojure.pprint :refer [pprint]]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   [app.regs]
   [app.styles :as css]
   [app.keymap :as k]
   [app.fs :as fs]))

(def default-css-fn css/left-to-right)

(defn init []
  (.log js/console "Starting Application" (js/Date.))
  (fs/read-ide-settings))

;; A detailed walk-through of this source code is provied in the docs:
;; https://github.com/Day8/re-frame/blob/master/docs/CodeWalkthrough.md

(defn dispatch-timer-event []
  (let [active @(rf/subscribe [:active-file])
        editor @(rf/subscribe [:ide-file-editor active])]
    (.setOption editor "styleActiveLine" true))
  #_(let [now (js/Date.)] (rf/dispatch [:timer now])))

;; Call the dispatching function every second.
;; `defonce` is like `def` but it ensures only one instance is ever
;; created in the face of figwheel hot-reloading of this file.
#_(defonce do-timer (js/setInterval dispatch-timer-event 500))

(defn clock []
  [:div.example-clock
   {:style {:color @(rf/subscribe [:time-color])}}
   (-> @(rf/subscribe [:time])
       .toTimeString
       (s/split " ")
       first)])

(defn color-input []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @(rf/subscribe [:time-color])
            :on-change #(rf/dispatch
                         [:time-color-change (-> % .-target .-value)])}]])

(defn edit [file]
  (r/create-class
   {:component-did-mount
    (fn [this]
      #_(js/console.log "did-mount this" this)
      (let [editor
            (js/CodeMirror
             (r/dom-node this)
             (->> {:theme (sjoin [css/theme css/theme-mode #_"xq-light"])
                   :lineNumbers true
                   :vimMode true
                   :styleActiveLine true
                   :showCursorWhenSelecting true
                   :rulers (clj->js
                            [{:color "lightgray"
                              :column 80 :lineStyle "dashed"}])
                   ;; :cursorBlinkRate 0 ;; 0 - no blinking
                   :matchBrackets true
                   :showTrailingSpace true ;; TODO doesn't work
                   :highlightSelectionMatches (clj->js
                                               [{:showToken true
                                                 :annotateScrollbar true}])
                   ;; see https://github.com/Bost/paredit-cm.git
                   ;; :keyMap "paredit_cm" ;; see lib/keymap-paredit-cm.js
                   :autoCloseBrackets true}
                  (conj ((keyword (.extname (js/require "path") file))
                         {:.cljs {:mode "clojure"}
                          :.html {:mode "xml" :htmlMode true}}))
                  clj->js))
            open-files @(rf/subscribe [:open-files])]
        (rf/dispatch [:ide-file-editor-change [file editor]])
        (fs/read file editor open-files)
        (.setSize editor nil (css/window-height))
        (.focus editor)
        ;; editor.setCursor({line: 1, ch: 5})
        (.setOption editor "extraKeys" (k/keymap file open-files))))

    ;; ... other methods go here
    ;; see https://facebook.github.io/react/docs/react-component.html#the-component-lifecycle
    ;; for a complete list

    ;; name your component for inclusion in error messages
    ;; :display-name "edit"

    #_:component-did-update
    #_(fn [this [_ prev-props]]
        #_(.log js/console "did-update this" this)
        ;; TODO: Handle codemirror-opts changes?

        (if-let [new-value (:value (r/props this))]
          ;; Not checked against (:value prev-props) as that causes problems with parinfer
          ;; not sure if any benefit in using that when not using parinfer?
          (when (not= (.getValue @cm) new-value)
            (.setValue @cm new-value))))

    ;; note the keyword for this method
    :reagent-render
    (fn []
      #_(.log js/console "reagent-render")
      [:div])}))

(defn context-menu
  "See https://github.com/electron/electron/blob/master/docs/api/menu.md"
  []
  (let [remote (.-remote (js/require "electron"))
        menu-fn (.-Menu remote)
        menu-item-fn (.-MenuItem remote)
        menu (menu-fn.)
        menu-items [(menu-item-fn.
                     (clj->js {:label "MenuItem1"
                               :click
                               (fn [] (.log js/console "item 1 clicked"))}))
                    (menu-item-fn.
                     (clj->js {:type "separator"}))
                    (menu-item-fn.
                     (clj->js {:label "MenuItem2"
                               :type "checkbox"
                               :checked true
                               :click
                               (fn [] (.log js/console "item 2 clicked"))}))]]
    (doseq [mi menu-items]
      (.append menu mi))
    (.addEventListener
     js/window "contextmenu"
     (fn [e]
       (.preventDefault e)
       (.popup menu (.getCurrentWindow remote)))
     false)
    [:div]))

(defn active-stats [key files]
  (let [active @(rf/subscribe [:active-file])
        orig-size @(rf/subscribe [:ide-file-content active])]
    [:div {:key key
           :class (sjoin [css/box css/stats
                          css/codemirror-theme css/codemirror-theme-mode])}
     (map-indexed (fn [i file]
                    (if (= file active)
                      (sjoin [file "orig-size" (count orig-size) "bytes"])))
                  files)]))

(defn prev? [file]
  (= file @(rf/subscribe [:prev-file])))
(defn active? [file]
  (= file @(rf/subscribe [:active-file])))

(def fancy-indexes-active-or-prev ["➊" "➋" "➌" "➍" "➎" "➏" "➐" "➑" "➒"]
  #_["❶" "❷" "❸" "❹" "❺" "❻" "❼" "❽" "❾"])
(def fancy-indexes-default ["①" "②" "③" "④" "⑤" "⑥" "⑦" "⑧" "⑨"])

(defn file-tab [react-key i file]
  [:div {:key (str react-key i)
         :class (sjoin [css/box
                        (str css/tabs (inc i))
                        css/codemirror-theme
                        css/codemirror-theme-mode
                        (if (active? file) css/active)
                        (if (prev? file) css/prev)])
         :on-click (fn [] (rf/dispatch [:active-file-change file]))}
   (let [attr (->> [(if (active? file) "A") (if (prev? file) "P")]
                   (remove nil?)
                   s/join)]
     (sjoin [
             #_(if-not (empty? attr) (str "*" attr "*"))
             (let [fancy-indexes (if (or (active? file) (prev? file))
                                   fancy-indexes-active-or-prev
                                   fancy-indexes-default)]
               (nth fancy-indexes i))
             (.basename (js/require "path") file)]))])

(defn editors [{:keys [react-key files active count-tabs]}]
  (map-indexed (fn [i file]
                 [:div {:key (str react-key (+ count-tabs i))
                        :class (sjoin [#_css/box css/editor])}
                  (if (= active file) [edit file])])
               files))

(defn cmd-line [cnt-files]
  [:div {:key (inc cnt-files)
         :class (sjoin [css/box css/cmd-line
                        css/codemirror-theme css/codemirror-theme-mode])}
   "cmd-line, messages"])

(defn file-tab-key [{:keys [react-key css-fn files]}]
  (if-not (= css-fn css/no-tabs)
    (map-indexed (fn [i file] (file-tab react-key i file))
                 files)))

(defn uix [files]
  (let [cnt-files (count files)
        css-fn @(rf/subscribe [:tabs-pos])
        active @(rf/subscribe [:active-file])
        prev @(rf/subscribe [:prev-file])]
    (if (in? [css/left-to-right css/right-to-left] css-fn)
      [:div {:class "lr-wrapper"}
       [(if css-fn css-fn default-css-fn) files]
       ;; Can't use (defn active-file [...] ...) because of the react warning:
       ;; Each child in an array or iterator should have a unique "key" prop
       [:div {:class "l-wrapper"}
        (let [react-key css/tabs]
          (doall
           (file-tab-key {:react-key react-key :css-fn css-fn :files files})))]
       [:div {:class "r-wrapper"}
        (editors {:react-key css/editor :files files :active active :count-tabs 0})
        [active-stats cnt-files files]
        [cmd-line cnt-files]]
       [context-menu]]
      [:div {:class "wrapper"}
       [(if css-fn css-fn default-css-fn) files]
       ;; Can't use (defn active-file [...] ...) because of the react warning:
       ;; Each child in an array or iterator should have a unique "key" prop
       (let [prm {:react-key "" :files files :active active :css-fn css-fn}
             tabs (file-tab-key prm)]
         (conj (editors (assoc prm :count-tabs (count tabs)))
               tabs))
       [active-stats cnt-files files]
       [cmd-line cnt-files]
       [context-menu]])))

(defn init-vals [k default-val]
  [(keyword (str (name k) "-change"))
   (if-let [val @(rf/subscribe [k])]
     val default-val)])

(defn ui []
  (let [ide-files @(rf/subscribe [:ide-files])
        files (->> ide-files keys vec)]
    (->> [(init-vals :tabs-pos default-css-fn)
          [:open-files-change files]
          (init-vals :active-file (first files))
          (init-vals :prev-file (first files))]
         (map rf/dispatch)
         doall)
    [uix files]))

(defn ^:export run
  []
  ;; puts a value into application state
  (rf/dispatch-sync [:initialize])
  ;; mount the application's ui into '<div id="app" />'
  (r/render [ui] (js/document.getElementById "app")))
