(ns app.renderer
  (:require
   ;; [cljsjs.codemirror :as cm]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [utils.core :refer [in? dbg next-cyclic]]
   [app.regs]
   #_cljsjs.parinfer
   cljsjs.codemirror
   [parinfer-codemirror.editor :as editor]
   ))

(enable-console-print!)

(def parinfer (js/require "parinfer"))

(defn- convert-changed-line [e]
  {:line-no (aget e "lineNo")
   :line (aget e "line")})

(defn- convert-error [e]
  (when e
    {:name (aget e "name")
     :message (aget e "message")
     :line-no (aget e "lineNo")
     :x (aget e "x")}))

(defn- convert-result [result]
  {:text (aget result "text")
   :success? (aget result "success")
   :changed-lines (mapv convert-changed-line (aget result "changedLines"))
   :error (convert-error (aget result "error"))})

(defn- convert-options [option]
  #js {:cursorX (:cursor-x option)
       :cursorLine (:cursor-line option)
       :cursorDx (:cursor-dx option)})

#_(def parinfer (js/require "parinfer"))

;; (def indent-mode* (partial .indentMode parinfer))
;; (def paren-mode* (partial .parenMode parinfer))

(defn indent-mode
  ([text] (convert-result (.indentMode (js/require "parinfer") text)))
  ([text options] (convert-result (.indentMode (js/require "parinfer") text (convert-options options)))))

(defn paren-mode
  ([text] (convert-result (.parenMode (js/require "parinfer") text)))
  ([text options] (convert-result (.parenMode (js/require "parinfer") text (convert-options options)))))

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


(defonce state (r/atom {:code "(defn hello [] \"world\")"}))

(defn edit [file]
  (let [fs (js/require "fs")
        state (r/atom {})] ;; you can include state
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
                              })
              open-files @(rf/subscribe [:open-files])
              parinfer-mode #_:paren-mode :indent-mode
              ]
          (read fs file editor open-files)
          (.on editor "change" (partial editor/on-change parinfer-mode))
          (.on editor "beforeChange" editor/before-change)
          (.on editor "cursorActivity" (partial editor/on-cursor-activity parinfer-mode))
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

(defn active-file [file]
  (let [
        af @(rf/subscribe [:active-file])
        ]
    [:div
     [:div (if (= file af) "*" "") file]
     (if (= file af)
       [edit file
        {:on-before-change editor/before-change
         :on-cursor-activity (partial editor/on-cursor-activity :indent-mode)
         :on-change (fn [cm change]
                      (editor/on-change :indent-mode cm change)
                      (if (not= "setState" (.-origin change))
                        (swap! state assoc :code (.getValue cm))))
         :codemirror-opts (merge editor/default-opts
                                 {})}])
     [:div "stats: " file]]))

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

(defn ui
  []
  (let [
        path (js/require "path")
        cur-dir (.resolve path ".")
        ide-files
        {(str cur-dir "/src/app/s1.cljs") {}
         (str cur-dir "/src/app/s2.cljs") {}}
        #_{(str cur-dir "/src/app/renderer.cljs") {}
         (str cur-dir "/src/app/main.cljs") {}}
        files (->> ide-files keys vec)
        ]
    (rf/dispatch [:ide-files-change ide-files])
    (rf/dispatch [:open-files-change files])
    (rf/dispatch [:active-file-change (first files)])
    [:div
     [context-menu]
     (map-indexed
      (fn [i file]
        [:div {:key i
               :on-click (fn [] (rf/dispatch [:active-file-change file]))}
         [active-file file]])
      files)
     ]))

(defn ^:export run
  []
  ;; puts a value into application state
  (rf/dispatch-sync [:initialize])
  ;; mount the application's ui into '<div id="app" />'
  (r/render [ui] (js/document.getElementById "app")))

