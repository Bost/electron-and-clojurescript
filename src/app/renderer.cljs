(ns app.renderer
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as s]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   [app.regs]
   [app.styles :as css]
   [app.keymap :as k]
   [app.fs :as fs]
   ))

(def default-css-fn css/left)

(defn init []
  (.log js/console "Starting Application"))

;; A detailed walk-through of this source code is provied in the docs:
;; https://github.com/Day8/re-frame/blob/master/docs/CodeWalkthrough.md

(defn dispatch-timer-event
  []
  (let [now (js/Date.)] (rf/dispatch [:timer now])))

;; Call the dispatching function every second.
;; `defonce` is like `def` but it ensures only one instance is ever
;; created in the face of figwheel hot-reloading of this file.
(defonce do-timer (js/setInterval dispatch-timer-event 1000))

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
             (->> {:theme "solarized dark" #_"xq-light"
                   :lineNumbers true
                   :vimMode true
                   ;; see https://github.com/Bost/paredit-cm.git
                   ;; :keyMap "paredit_cm"
                   :autoCloseBrackets true}
                  (conj (let [path (js/require "path")]
                          ((keyword (.extname path file))
                           {:.cljs {:mode "clojure"}
                            :.html {:mode "xml" :htmlMode true}})))
                  clj->js))
            open-files @(rf/subscribe [:open-files])
            ]
        (let [fs (js/require "fs")]
          (fs/read fs file editor open-files)
          (let [
                height
                (->> js/document .-documentElement .-clientHeight)
                #_(->> (js/require "electron")
                       .-screen
                       .getPrimaryDisplay
                       .-workAreaSize .-height)
                ;; opts (js/require "electron-browser-window-options")
                ;; bw (->> electron .-remote)
                ;; wc (->> electron .-remote .-webContents .getFocusedWebContents)
                ]
            #_(js/console.log "height" height)
            (.setSize editor nil (- height 90)))
          (.focus editor)
          ;; editor.setCursor({line: 1, ch: 5})
          (.setOption editor "extraKeys" (k/keymap fs file open-files)))))

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
                     #js {:label "MenuItem1"
                          :click (fn [] (.log js/console "item 1 clicked"))})
                    (menu-item-fn.
                     #js {:type "separator"})
                    (menu-item-fn.
                     #js {:label "MenuItem2"
                          :type "checkbox"
                          :checked true
                          :click (fn [] (.log js/console "item 2 clicked"))
                          })]
        ]
    (doseq [mi menu-items]
      (.append menu mi))
    (.addEventListener
     js/window "contextmenu"
     (fn [e]
       (.preventDefault e)
       (.popup menu (.getCurrentWindow remote)))
     false)
    [:div]
    ))

(defn active-stats [files]
  (let [active @(rf/subscribe [:active-file])
        orig-size @(rf/subscribe [:ide-file-content active])]
    [:div {:class "box s"}
     (map-indexed
      (fn [i file]
        (if (= file active) (sjoin [ file "orig-size" (count orig-size)])))
      files)]))

(defn prev? [file]
  (= file @(rf/subscribe [:prev-file])))
(defn active? [file]
  (= file @(rf/subscribe [:active-file])))

(defn uix [files]
  [:div {:class "wrapper"}
   (let [css-fn @(rf/subscribe [:tabs-pos])]
     [(if css-fn css-fn default-css-fn) files])
   [context-menu]
   ;; Can't use (defn active-file [...] ...) because of the react warning:
   ;; Each child in an array or iterator should have a unique "key" prop
   (let [active @(rf/subscribe [:active-file])
         prev @(rf/subscribe [:prev-file])
         path (js/require "path")
         tabs (map-indexed
                (fn [i file]
                  [:div {:key i
                         :class (str "box a" (inc i))
                         :on-click (fn [] (rf/dispatch [:active-file-change file]))}

                   (let [attr (->> [(if (active? file) "A") (if (prev? file) "P")]
                                   (remove nil?)
                                   s/join)]
                     (sjoin [(if-not (empty? attr)
                               (str "*" attr "*"))
                             (.basename path file)]))])
                files)
         cnt-files (count files)
         editor (map-indexed
               (fn [i file]
                 [:div {:key (+ cnt-files i) :class (sjoin [#_"box" "e"])}
                  (if (= active file) [edit file])]) files)]
     (into editor tabs))
   #_[:div {:class "box d"} "D"]
   [active-stats files]
   ])

(defn ui []
  (let [path (js/require "path")
        cur-dir (.resolve path ".")
        ide-files {
                   (str cur-dir "/src/app/renderer.cljs") {}
                   (str cur-dir "/src/app/styles.cljs") {}
                   (str cur-dir "/resources/index.html") {}
                   (str cur-dir "/src/app/main.cljs") {}
                   }
        files (->> ide-files keys vec)
        active (first files)]
    (rf/dispatch [:tabs-pos-change css/left])
    (rf/dispatch [:ide-files-change ide-files])
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

