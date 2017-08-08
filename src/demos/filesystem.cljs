(ns demos.filesystem)

(defn renderer []
  (let [fs (js/require "fs")
        path (js/require "path")
        current-dir (.resolve path ".")
        fname (str current-dir "/boot.properties")]

    (.readFile fs fname "utf8"
               (fn [err data]
                 (js/CodeMirror (.-body js/document)
                                #js {:value #_text data
                                     :mode "text" #_"javascript"
                                     :lineNumbers true})))
    (let [div (.-body js/document)]
      (set! (. div -innerHTML) "<b>Bold!</b>"))))
