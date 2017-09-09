(ns app.renderer
  (:require
   ;; [cljsjs.codemirror :as cm]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [utils.core :refer [in? dbg next-cyclic]]
   ))

(enable-console-print!)

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

(rf/reg-event-db :initialize (fn [_ _] {:time (js/Date.) :time-color "#f88"}))

(rf/reg-event-db :time-color-change
                 (fn [db [_ new]] (assoc db :time-color new)))
(rf/reg-event-db :timer (fn [db [_ new]] (assoc db :time new)))
(rf/reg-sub :time (fn [db _] (:time db)))
(rf/reg-sub :time-color (fn [db _] (:time-color db)))

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

(rf/reg-event-db
 :open-files-change (fn [db [_ new]] (assoc db :open-files new)))
(rf/reg-sub
 :open-files (fn [db _] (:open-files db)))

(rf/reg-event-db
 :active-file-change (fn [db [_ new]] (assoc db :active-file new)))
(rf/reg-sub
 :active-file (fn [db _] (:active-file db)))

(rf/reg-event-db
 :ide-files-change (fn [db [_ new]] (assoc db :ide-files new)))
(rf/reg-sub
 :ide-files (fn [db _] (:ide-files db)))

(rf/reg-event-db
 :ide-file-content-change
 (fn [db [_ [file new]]]
   (assoc-in db [:ide-files file :content] new)))
(rf/reg-sub
 :ide-file-content
 (fn [db [_ file]]
   (get-in db [:ide-files file :content])))

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
  (let [fs (js/require "fs")
        state (reagent/atom {})] ;; you can include state
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        #_(println "did-mount this" this)
        (let [editor
              (js/CodeMirror (reagent/dom-node this)
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
              ]
          (read fs file editor open-files)
          (.focus editor)
          ;; editor.setCursor({line: 1, ch: 5})
          (.setOption
           editor "extraKeys"
           #js {
                ;; :Ctrl-W (fn [editor] (println "Ctrl-W"))

                ;; single key: <S>
                ;; :Mod (fn [editor] (println "Mod"))

                :Cmd-F (fn [editor]
                         (println "Cmd-F / <S-f>")
                         (rf/dispatch [:time-color-change "green"])
                         (let [new-file
                               ;; assigning file to new-file causes file-reload
                               file]
                           (rf/dispatch [:active-file-change new-file])
                           (read fs new-file editor open-files)
                           ))
                :Cmd-S (fn [editor]
                         (println "Cmd-S / <S-s>")
                         (save fs file (.getValue (.-doc editor))))
                :Cmd-Q (fn [editor]
                         (println "Cmd-Q / <S-q>")
                         (let [active-file @(rf/subscribe [:active-file])
                               idx (.indexOf open-files active-file)]
                           (rf/dispatch [:active-file-change (next-cyclic idx open-files)])))
                })))

      ;; ... other methods go here
      ;; see https://facebook.github.io/react/docs/react-component.html#the-component-lifecycle
      ;; for a complete list

      ;; name your component for inclusion in error messages
      ;; :display-name "edit"

      ;; :component-did-update
      ;; (fn [this old-argv] (println "did-update this" this))

      ;; note the keyword for this method
      :reagent-render
      (fn []
        #_(println "reagent-render")
        [:div])})))

(defn active-file [file]
  (let [af @(rf/subscribe [:active-file])]
    [:div
     [:div (if (= file af) "*" "") file]
     (if (= file af)
       [edit file])
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
        ide-files {(str cur-dir "/src/app/renderer.cljs") {}
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
  (reagent/render [ui] (js/document.getElementById "app")))

