(ns app.renderer
  (:require
   ;; [cljsjs.codemirror :as cm]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [clojure.string :as str]
   ))

(defn init []
  (js/console.log "Starting Application"))

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
(rf/reg-event-db :data-change (fn [db [_ new-data]] (assoc db :data new-data)))
(rf/reg-event-db :cm-change (fn [db [_ new-cm]] (assoc db :cm new-cm)))
(rf/reg-event-db :active-file-change
                 (fn [db [_ new-file]] (assoc db :active-file new-file)))
(rf/reg-event-db :open-files-change (fn [db [_ of]] (assoc db :open-files of)))
(rf/reg-event-db :time-color-change
                 (fn [db [_ new-color]] (assoc db :time-color new-color)))
(rf/reg-event-db :timer (fn [db [_ new-time]] (assoc db :time new-time)))

(rf/reg-sub :data (fn [db _] (:data db)))
(rf/reg-sub :cm (fn [db _] (:cm db)))
(rf/reg-sub :active-file (fn [db _] (:active-file db)))
(rf/reg-sub :open-files (fn [db _] (:open-files db)))
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

(defn save [fs fname data]
  (.writeFile fs fname data
              (fn [err _]
                (if err
                  (js/console.log err)
                  (js/console.log (count data) "bytes saved")))))

(defn read [fs fname editor]
  (.readFile fs fname "utf8"
             (fn [err data]
               (if err
                 (js/console.log err)
                 (do
                   (.setValue (.-doc editor) data)
                   (rf/dispatch [:data-change data])
                   (js/console.log (count data) "bytes loaded"))))))

(defn edit [file]
  (let [fs (js/require "fs")
        state (reagent/atom {})] ;; you can include state
    #_(js/console.log "edit" "file" file)
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        #_(js/console.log "did-mount this" this)
        (let [editor
              (js/CodeMirror (reagent/dom-node this)
                             #js
                             {
                              :theme "xq-light"
                              :mode "javascript"
                              :lineNumbers true
                              })
              #_(js/CodeMirror (reagent/dom-node this) #_(.-body js/document)
                               #js
                               {
                                :theme "xq-light"
                                :mode "javascript"
                                :lineNumbers true
                                }
                               )]
          (read fs file editor)
          (.setOption
           editor "extraKeys"
           #js {
                ;; :Ctrl-W (fn [editor] (js/console.log "Ctrl-W"))

                ;; single key: <S>
                ;; :Mod (fn [editor] (js/console.log "Mod"))

                :Cmd-F (fn [editor]
                         (js/console.log "Cmd-F / <S-f>")
                         (rf/dispatch [:time-color-change "green"])
                         (let [new-file
                               ;; assigning file to new-file causes file-reload
                               file]
                           (rf/dispatch [:active-file-change new-file])
                           (read fs new-file editor)
                           ))
                :Cmd-S (fn [editor]
                         (js/console.log "Cmd-S / <S-s>")
                         (save fs file (.getValue (.-doc editor))))
                })))

      ;; ... other methods go here
      ;; see https://facebook.github.io/react/docs/react-component.html#the-component-lifecycle
      ;; for a complete list

      ;; name your component for inclusion in error messages
      ;; :display-name "edit"

      ;; :component-did-update
      ;; (fn [this old-argv] (js/console.log "did-update this" this))

      ;; note the keyword for this method
      :reagent-render
      (fn []
        #_(js/console.log "reagent-render")
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
                          :click (fn [] (js/console.log "item 1 clicked"))})
                    (menu-item-fn.
                     #js {:type "separator"})
                    (menu-item-fn.
                     #js {:label "MenuItem2"
                          :type "checkbox"
                          :checked true
                          :click (fn [] (js/console.log "item 2 clicked"))
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
        fname1 (str cur-dir "/nodejs-code.js")
        fname2 (str cur-dir "/n2.js")
        files [fname1 fname2]
        ]
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

