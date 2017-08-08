(ns demos.filesystem)

(defn renderer []
  (let [body (.-body js/document)
        myCodeMirror (js/CodeMirror body
                                    {:value "function myScript(){return 200;}\n"
                                     :mode "javascript"
                                     :lineNumbers true})]
    myCodeMirror)
  #_(let [fs (js/require "fs")
          path (js/require "path")
          current-dir (.resolve path ".")
          fname (str current-dir "/written-from-cljs.txt")
          data "I didn't expect it to be so warm in Finland."]
      (.writeFile fs fname data (fn [] (js/console.log fname "saved")))))
