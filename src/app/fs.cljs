(ns app.fs
  (:require
   [re-frame.core :as rf]
   [clojure.string :as s]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   [app.regs]
   ))

(def fs (js/require "fs"))
(def home-dir (-> (js/require "os") .homedir))
(def config-file (str home-dir "/.eac/config.edn"))
(def encoding "utf8")
(def writefile (js/require "writefile"))

(defn read-file [file cont-fn]
  (.readFile fs file (clj->js {:encoding encoding}) cont-fn))

(defn read [file editor open-files]
  (let [content @(rf/subscribe [:ide-file-content file])]
    (if content
      (.setValue (.-doc editor) content)
      (read-file file
                 (fn [err data]
                   (if err
                     (js/throw err)
                     (do
                       (.log js/console file (count data) "bytes loaded")
                       (.setValue (.-doc editor) data)
                       (rf/dispatch [:ide-file-content-change [file data]])
                       )))))))

(defn save [file data]
  #_(.writeFile fs file data #_(clj->js {:encoding encoding :flags "w"})
                (fn [err _]
                  (if err
                    (.error js/console err)
                    (.log js/console file (count data) "bytes saved"))))
  #_(.log js/console "writefile" writefile)
  (writefile file data
   (fn [err _]
     (if err
       (js/throw err)
       (.log js/console file (count data) "bytes saved")))))

(defn exec [cmd-line]
  (let [cmd (first cmd-line)
        prms (clj->js (rest cmd-line))]
    (.log js/console "$" (sjoin cmd-line))
    (let [spawn (->> (js/require "child_process") .-spawn)
          prc (spawn cmd prms)]
      (js/prc.stdout.setEncoding "utf8")
      (js/prc.stdout.on
       "data" (fn [data] (.log js/console #_"STDOUT" (str data))))
      ;; boot process output gets displayed only on the STDERR
      #_(js/prc.stderr.on
         "data" (fn [data] (.error js/console #_"STDERR" (str data))))
      (js/prc.stdout.on
       "message" (fn [msg] (.log js/console "CHILD got message" msg)))
      (js/prc.stdout.on
       "close" (fn [code] #_(.log js/console "Process close code" code)))
      (js/prc.stdout.on
       "exit" (fn [code] (.log js/console "Process exit code" code))))))

(defn save-ide-settings []
  (let [settings @(rf/subscribe [:ide-files])]
    #_(println "settings" settings)
    (save config-file (-> settings println with-out-str))))

(defn read-ide-settings []
  (read-file config-file
             (fn [err data]
               (if err
                 (js/throw err)
                 (do
                   (.log js/console config-file (count data) "bytes loaded")
                   #_(.setValue (.-doc editor) data)
                   #_(rf/dispatch [:ide-file-content-change [file data]])
                   )))))

