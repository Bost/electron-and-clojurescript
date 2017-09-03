(ns demos.filesystem
  (:require
   [re-frame.core :as rf]
   ))

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
                   (js/console.log (count data) "bytes loaded"))))))

(defn edit [fname]
  (let [doc (.-body js/document)]
    #_(js/Notification. "Hello ClojuTRE!" (clj->js {:body "It's great to be here."}))
    (let [cm (js/CodeMirror doc
                            #js {:mode #_"text" "javascript"
                                 :lineNumbers true})
          fs (js/require "fs")]
      (read fs fname cm)
      (rf/dispatch [:fname-change fname])
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
                                  (read fs new-fname cm)))
                       :Cmd-S (fn [cm]
                                (js/console.log "Cmd-S / <S-s>")
                                (save fs fname (.getValue (.-doc cm))))
                       })
      cm)))

(defn renderer [fname]
  (let [cm (edit fname)]
    #_(set! (. doc -innerHTML) "something in the container")))
