(ns app.renderer
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [utils.core :refer [in? dbg next-cyclic]]
   [app.regs]
   ))

(enable-console-print!)

(def default-codemirror-opts {})

(defn init []
  (println "Starting Application"))

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
       (str/split " ")
       first)])

(defn color-input
  []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @(rf/subscribe [:time-color])
            :on-change #(rf/dispatch
                         [:time-color-change (-> % .-target .-value)])}]])

(declare read)
(declare save)

(defn keymap [fs file open-files]
  #js
  {
   ;; :Ctrl-W (fn [editor] (println "Ctrl-W"))

   ;; single key: <S>
   ;; :Mod (fn [editor] (println "Mod"))

   :Cmd-F (fn [editor]
            (println "Cmd-F / <S-f>")
            ;; assigning file to file causes file-reload
            (rf/dispatch [:active-file-change file])
            (read fs file editor open-files)
            )
   :Cmd-S (fn [editor]
            (println "Cmd-S / <S-s>")
            (save fs file (.getValue (.-doc editor))))
   :Cmd-Q (fn [editor]
            (println "Cmd-Q / <S-q>")
            (let [active-file @(rf/subscribe [:active-file])
                  idx (.indexOf open-files active-file)]
              (rf/dispatch [:active-file-change (next-cyclic idx open-files)])))
   })

(defn read [fs file editor open-files]
  (let [content @(rf/subscribe [:ide-file-content file])]
    (if content
      (.setValue (.-doc editor) content)
      (.readFile fs file "utf8"
                 (fn [err data]
                   (if err
                     (println err)
                     (do
                       (println (count data) "bytes loaded")
                       (.setValue (.-doc editor) data)
                       (rf/dispatch [:ide-file-content-change [file data]])
                       )))))))

(defn save [fs fname data]
  (.writeFile fs fname data
              (fn [err _]
                (if err
                  (println err)
                  (println (count data) "bytes saved")))))

(defn edit [file]
  (let [fs (js/require "fs")]
    (r/create-class
     {:component-did-mount
      (fn [this]
        #_(js/console.log "did-mount this" this)
        (let [editor
              (js/CodeMirror (r/dom-node this)
                             #js
                             {
                              :theme
                              #_"xq-light"
                              "solarized dark"
                              :mode "clojure"
                              :lineNumbers true
                              ;; :vimMode true
                              :autoCloseBrackets true
                              ;; see https://github.com/Bost/paredit-cm.git
                              :keyMap "paredit_cm"
                              })
              open-files @(rf/subscribe [:open-files])
              parinfer-mode :paren-mode #_:indent-mode
              ]
          (read fs file editor open-files)
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
            (js/console.log "height" height)
            (.setSize editor nil (- height 90)))
          (.focus editor)
          ;; editor.setCursor({line: 1, ch: 5})
          (.setOption editor "extraKeys" (keymap fs file open-files))))

      ;; ... other methods go here
      ;; see https://facebook.github.io/react/docs/react-component.html#the-component-lifecycle
      ;; for a complete list

      ;; name your component for inclusion in error messages
      ;; :display-name "edit"

      #_:component-did-update
      #_(fn [this [_ prev-props]]
        #_(println "did-update this" this)
        ;; TODO: Handle codemirror-opts changes?

        (if-let [new-value (:value (r/props this))]
          ;; Not checked against (:value prev-props) as that causes problems with parinfer
          ;; not sure if any benefit in using that when not using parinfer?
          (when (not= (.getValue @cm) new-value)
            (.setValue @cm new-value))))

      ;; note the keyword for this method
      :reagent-render
      (fn []
        #_(println "reagent-render")
        [:div])})))

(defn context-menu
  "See https://github.com/electron/electron/blob/master/docs/api/menu.md"
  []
  (let [remote (.-remote (js/require "electron"))
        menu-fn (.-Menu remote)
        menu-item-fn (.-MenuItem remote)
        menu (menu-fn.)
        menu-items [(menu-item-fn.
                     #js {:label "MenuItem1"
                          :click (fn [] (println "item 1 clicked"))})
                    (menu-item-fn.
                     #js {:type "separator"})
                    (menu-item-fn.
                     #js {:label "MenuItem2"
                          :type "checkbox"
                          :checked true
                          :click (fn [] (println "item 2 clicked"))
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

(defn active-file [file]
  (let [
        af @(rf/subscribe [:active-file])
        ]
    [:div (if (= file af) [edit file])]))

(defn ui
  []
  (let [
        path (js/require "path")
        cur-dir (.resolve path ".")
        ide-files {(str cur-dir "/src/app/renderer.cljs") {}
                   (str cur-dir "/src/app/main.cljs") {}}
        files (->> ide-files keys vec)
        af (first files)
        ]
    (rf/dispatch [:ide-files-change ide-files])
    (rf/dispatch [:open-files-change files])
    (rf/dispatch [:active-file-change af])
    [:div
     [context-menu]
     (map-indexed
      (fn [i file]
        [:div {:key i}
         [:div {:on-click (fn [] (rf/dispatch [:active-file-change file]))}
          (if (= file af) "*" "") file]])
      files)
     (map-indexed (fn [i file] [:div {:key i} [active-file file]]) files)
     (map-indexed (fn [i file] [:div {:key i} [:div "stats: " file]])
      files)
     ]))

(defn ^:export run
  []
  ;; puts a value into application state
  (rf/dispatch-sync [:initialize])
  ;; mount the application's ui into '<div id="app" />'
  (r/render [ui] (js/document.getElementById "app")))

