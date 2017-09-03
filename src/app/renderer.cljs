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
(rf/reg-event-db :fname-change (fn [db [_ new-fname]] (assoc db :fname new-fname)))
(rf/reg-event-db :time-color-change (fn [db [_ new-color-value]] (assoc db :time-color new-color-value)))
(rf/reg-event-db :timer (fn [db [_ new-time]] (assoc db :time new-time)))

(rf/reg-sub :data (fn [db _] (:data db)))
(rf/reg-sub :cm (fn [db _] (:cm db)))
(rf/reg-sub :fname (fn [db _] (:fname db)))
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
            :on-change #(rf/dispatch [:time-color-change (-> % .-target .-value)])}]])

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

(defn complex-component [a b c]
  (let [
        path (js/require "path")
        current-dir (.resolve path ".")
        fname (str current-dir "/nodejs-code.js")
        fs (js/require "fs")
        state (reagent/atom {})] ;; you can include state
    #_(js/console.log "complex-component" "state" state)
    (reagent/create-class
     {:component-did-mount
      (fn [this]
        (js/console.log "did-mount this" this)
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
          (read fs fname editor)
          (.setOption editor "extraKeys"
                      #js {
                           ;; :Ctrl-W (fn [editor] (js/console.log "Ctrl-W"))
                           ;; :Mod (fn [editor] (js/console.log "Mod"))     ; single key: <S>

                           :Cmd-F (fn [editor]
                                    (js/console.log "Cmd-F / <S-f>")
                                    (rf/dispatch [:time-color-change "green"])
                                    (let [new-fname
                                          #_"/home/bost/dev/eac/README.md"
                                          fname]
                                      (rf/dispatch [:fname-change new-fname])
                                      (read fs new-fname editor)
                                      ))
                           :Cmd-S (fn [editor]
                                    (js/console.log "Cmd-S / <S-s>")
                                    (save fs fname (.getValue (.-doc editor))))
                           })
          (js/console.log "did-mount editor" editor)))

      ;; ... other methods go here
      ;; see https://facebook.github.io/react/docs/react-component.html#the-component-lifecycle
      ;; for a complete list

      ;; name your component for inclusion in error messages
      ;; :display-name "complex-component"

      ;; :component-did-update (fn [this old-argv] (js/console.log "did-update this" this))

      ;; note the keyword for this method
      :reagent-render
      (fn [a b c]
        (js/console.log "reagent-render" a b c)
        [:div (str a b c)])})))

(defn ui
  []
  [:div
   ;; [:div [clock] #_[color-input]]
   [:div @(rf/subscribe [:fname])]
   [:div "before"]
   [complex-component "" "" ""]
   [:div "after"]
   ])

(defn ^:export run
  []
  ;; puts a value into application state
  (rf/dispatch-sync [:initialize])
  ;; mount the application's ui into '<div id="app" />'
  (reagent/render [ui] (js/document.getElementById "app")))

