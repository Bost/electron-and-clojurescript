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
                   (js/console.log "cm" cm)
                   #_(.setValue doc "ufo"))))
    #_(set! (. doc -innerHTML) "something in the container")))
