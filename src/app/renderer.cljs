(ns app.renderer
  (:require
   #_[cljsjs.codemirror :as cm]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [clojure.string :as str]
   ))

(defn init []
  (js/console.log "Starting Application"))

;; A detailed walk-through of this source code is provied in the docs:
;; https://github.com/Day8/re-frame/blob/master/docs/CodeWalkthrough.md

;; -- Domino 1 - Event Dispatch -----------------------------------------------

(defn dispatch-timer-event
  []
  (let [now (js/Date.)]
    (rf/dispatch [:timer now])))  ;; <-- dispatch used

;; Call the dispatching function every second.
;; `defonce` is like `def` but it ensures only one instance is ever
;; created in the face of figwheel hot-reloading of this file.
(defonce do-timer (js/setInterval dispatch-timer-event 1000))


;; -- Domino 2 - Event Handlers -----------------------------------------------

(rf/reg-event-db             ;; sets up initial application state
 :initialize                 ;; usage:  (dispatch [:initialize])
 (fn [_ _]                    ;; the two parameters are not important here, so use _
   {
    ;; :fname "ufo"
    :time (js/Date.)         ;; What it returns becomes the new application state
    :time-color "#f88"}))    ;; so the application state will initially be a map with two keys


(rf/reg-event-db
 :data-change
 (fn [db [_ new-data]] (assoc db :data new-data)))

(rf/reg-event-db
 :cm-change
 (fn [db [_ new-cm]] (assoc db :cm new-cm)))

(rf/reg-event-db
 :fname-change
 (fn [db [_ new-fname]] (assoc db :fname new-fname)))

(rf/reg-event-db              ;; usage:  (dispatch [:time-color-change 34562])
 :time-color-change           ;; dispatched when the user enters a new colour into the UI text field
 (fn [db [_ new-color-value]]  ;; -db event handlers given 2 parameters:  current application state and event (a vector)
   (assoc db :time-color new-color-value)))   ;; compute and return the new application state


(rf/reg-event-db                ;; usage:  (dispatch [:timer a-js-Date])
 :timer                         ;; every second an event of this kind will be dispatched
 (fn [db [_ new-time]]           ;; note how the 2nd parameter is destructured to obtain the data value
   (assoc db :time new-time)))  ;; compute and return the new application state


;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
 :data
 (fn [db _]      ;; db is current app state. 2nd unused param is query vector
   (:data db)))

(rf/reg-sub
 :cm
 (fn [db _]      ;; db is current app state. 2nd unused param is query vector
   (:cm db)))

(rf/reg-sub
 :fname
 (fn [db _]      ;; db is current app state. 2nd unused param is query vector
   (:fname db)))

(rf/reg-sub
 :time
 (fn [db _]      ;; db is current app state. 2nd unused param is query vector
   (:time db))) ;; return a query computation over the application state

(rf/reg-sub
 :time-color
 (fn [db _]
   (:time-color db)))


;; -- Domino 5 - View Functions ----------------------------------------------

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
            :on-change #(rf/dispatch [:time-color-change (-> % .-target .-value)])}]])  ;; <---

(defn save [fs fname data]
  (.writeFile fs fname data
              (fn [err _]
                (if err
                  (js/console.log err)
                  (js/console.log (count data) "bytes saved")))))

(defn read [fs fname cm]
  (.readFile fs fname "utf8"
             (fn [err data]
               (if err
                 (js/console.log err)
                 (do
                   (.setValue (.-doc cm) data)
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
        (let [cm (js/CodeMirror (reagent/dom-node this) #_(.-body js/document)
                                #js {
                                     :theme "xq-light"
                                     :mode "javascript"
                                     :lineNumbers true
                                     })]
          (read fs fname cm)
          (.setOption cm "extraKeys"
                      #js {
                           ;; :Ctrl-W (fn [cm] (js/console.log "Ctrl-W"))
                           ;; :Mod (fn [cm] (js/console.log "Mod"))     ; single key: <S>

                           :Cmd-F (fn [cm]
                                    (js/console.log "Cmd-F / <S-f>")
                                    (rf/dispatch [:time-color-change "green"])
                                    (let [new-fname
                                          #_"/home/bost/dev/eac/README.md"
                                          fname]
                                      (rf/dispatch [:fname-change new-fname])
                                      (read fs new-fname cm)
                                      ))
                           :Cmd-S (fn [cm]
                                    (js/console.log "Cmd-S / <S-s>")
                                    (save fs fname (.getValue (.-doc cm))))
                           })
          (js/console.log "did-mount cm" cm)))

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


;; -- Entry Point -------------------------------------------------------------

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])  ;; puts a value into application state
  (reagent/render [ui]              ;; mount the application's ui into '<div id="app" />'
                  (js/document.getElementById "app")))

