(ns app.renderer
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as s]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   [app.regs]
   [app.styles :as css]
   [app.keymap :as k]
   [app.fs :as fs]))

;; TODO Ctrl-Cmd-Alt-<Key> jump-to-char
;; TODO vim keybindings
;; TODO drag-line / drag-text
;; TODO Kill only the own boot-process; display a list of found processes
;; TODO full-screen style w/o stats and cmd-line
;; TODO context-menu: 'Search for <hlited-text>'
;; TODO context-menu: paste from clipboard
;; TODO integrate paredit-cm
;; TODO integrate utils dependency
;; TODO multiple-buffer, split-view: horizontal / vertical (active/inactive editor- bg-color)
;; TODO multiple-buffer, split-view: http://codemirror.net/demo/buffers.html
;; TODO dired
;; TODO stack of active files 1. *A*, 2. *P*, 3. <order-by edit-time?>
;; TODO search and replace: http://codemirror.net/doc/manual.html#addon_search

(def default-css-fn css/left-to-right)

(defn save-ide-settings []
  #_(fs/save fs "~/.eac/settings.edn" "{:open-files []}"))

(defn init []
  (.log js/console "Starting Application" (js/Date.))
  (fs/read-ide-settings))

;; A detailed walk-through of this source code is provied in the docs:
;; https://github.com/Day8/re-frame/blob/master/docs/CodeWalkthrough.md

(defn dispatch-timer-event
  []
  (let [
        active @(rf/subscribe [:active-file])
        editor @(rf/subscribe [:ide-file-editor active])
        ;; content @(rf/subscribe [:ide-file-content active])
        ]
    (if editor ;; TODO editor must be always defined!
      (do
        (.log js/console "active" active "old styleActiveLine"
              (.getOption editor "styleActiveLine" true))
        (.setOption editor "styleActiveLine" true))))
  #_(let [now (js/Date.)] (rf/dispatch [:timer now])))

;; Call the dispatching function every second.
;; `defonce` is like `def` but it ensures only one instance is ever
;; created in the face of figwheel hot-reloading of this file.
(defonce do-timer (js/setInterval dispatch-timer-event 2000))

(defn clock
  []
  [:div.example-clock
   {:style {:color @(rf/subscribe [:time-color])}}
   (-> @(rf/subscribe [:time])
       .toTimeString
       (s/split " ")
       first)])

(defn color-input
  []
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
                   ;; :cursorBlinkRate 0 ;; 0 - no blinking
                   :highlightSelectionMatches #js {:showToken true
                                                   :annotateScrollbar true}
                   ;; see https://github.com/Bost/paredit-cm.git
                   ;; :keyMap "paredit_cm"
                   :autoCloseBrackets true}
                  (conj ((keyword (.extname (js/require "path") file))
                         {:.cljs {:mode "clojure"}
                          :.html {:mode "xml" :htmlMode true}}))
                  clj->js))
            open-files @(rf/subscribe [:open-files])]
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

(def fancy-indexes ["➊" "➋" "➌" "➍" "➎" "➏" "➐" "➑" "➒"])

(defn file-tab [key-name i file]
  [:div {:key (str key-name i)
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
             (nth fancy-indexes i)
             (.basename (js/require "path") file)]))])

(defn editors [key-name files active count-tabs]
  (map-indexed (fn [i file]
                 [:div {:key (str key-name (+ count-tabs i))
                        :class (sjoin [#_css/box css/editor])}
                  (if (= active file) [edit file])])
               files))

(defn cmd-line [cnt-files]
  [:div {:key (inc cnt-files)
         :class (sjoin [css/box css/cmd-line
                        css/codemirror-theme css/codemirror-theme-mode])}
   "cmd-line, messages"])

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
        (doall
         (if (= css-fn css/no-tabs)
           []
           (map-indexed (fn [i file] (file-tab css/tabs i file))
                        files)))]
       [:div {:class "r-wrapper"}
        (editors css/editor files active 0) ;; (= 0 count-tabs)
        [active-stats cnt-files files]
        [cmd-line cnt-files]]
       [context-menu]]
      [:div {:class "wrapper"}
       [(if css-fn css-fn default-css-fn) files]
       ;; Can't use (defn active-file [...] ...) because of the react warning:
       ;; Each child in an array or iterator should have a unique "key" prop
       (let [tabs (if (= css-fn css/no-tabs)
                    []
                    (map-indexed (fn [i file] (file-tab "" i file))
                                 files))]
         (into (editors "" files active (count tabs))
               tabs))
       [active-stats cnt-files files]
       [cmd-line cnt-files]
       [context-menu]])))

(defn ui []
  (let [ide-files @(rf/subscribe [:ide-files])
        files (->> ide-files keys vec)
        active (first files)]
    (rf/dispatch [:tabs-pos-change default-css-fn])
    (rf/dispatch [:open-files-change files])
    (rf/dispatch [:active-file-change active])
    (rf/dispatch [:prev-file-change active])
    [uix files]))

(defn ^:export run
  []
  ;; puts a value into application state
  (rf/dispatch-sync [:initialize])
  ;; mount the application's ui into '<div id="app" />'
  (r/render [ui] (js/document.getElementById "app")))
