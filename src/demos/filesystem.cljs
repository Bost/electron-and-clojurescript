(ns demos.filesystem)

(defn renderer []
  (let [fs (js/require "fs")
        path (js/require "path")
        current-dir (.resolve path ".")
        fname (str current-dir "/boot.properties")
        doc (.-body js/document)]
    (.readFile fs fname "utf8"
               (fn [err data]
                 (let [cm (js/CodeMirror doc
                                         #js {:value data
                                              :mode "text" #_"javascript"
                                              :lineNumbers true})
                       doc (.-doc cm)]
                   #_(js/Notification. "Hello ClojuTRE!" (clj->js {:body "It's great to be here."}))
                   (.setOption cm "extraKeys"
                               #js {
                                    :Ctrl-W (fn [cm] (js/console.log "Ctrl-W"))
                                    :Ctrl-S (fn [cm] (js/console.log "Ctrl-s"))
                                    :Mod (fn [cm] (js/console.log "Mod"))     ; single key: <S>
                                    :Cmd-S (fn [cm] (js/console.log "Cmd-s")) ; combination: <S-s>
                                    })
                   #_(.setValue doc "ufo"))))
    #_(set! (. doc -innerHTML) "something in the container")))
