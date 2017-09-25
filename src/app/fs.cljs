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
(def fwriter (js/require "writefile"))

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
  (fwriter file data
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
      (js/prc.stdout.setEncoding encoding)
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
    #_(with-open [wr (clojure.core/writer config-file)]
        (.write wr (clojure.core/pr-str settings)))
    #_(.log js/console "writing" (-> {:ide-files settings} clojure.core/pr-str #_with-out-str))
    (save config-file
          #_(with-out-str (clojure.pprint/pprint settings))
          (-> {:ide-files settings} clojure.core/prn-str))))

#_(defn write-object
  "Serializes an object to disk so it can be opened again later.
   Careful: It will overwrite an existing file at file-path."
  []
  (let [settings @(rf/subscribe [:ide-files])]
    (with-open [wr (clojure.core/writer config-file)]
      (.write wr (clojure.core/pr-str settings)))))

#_{:ide-files
 {
  "/home/bost/dev/eac/src/app/keymap.cljs" {}
  "/home/bost/dev/eac/src/app/renderer.cljs" {}
  "/home/bost/dev/eac/src/app/styles.cljs" {}
  "/home/bost/dev/eac/resources/index.html" {}
  "/home/bost/dev/eac/src/app/regs.cljs" {}
  }}

(defn fn-load-ide-files [err data]
  (rf/dispatch
   [:ide-files-change
    (if err
      (do
        ;; (js/throw err)
        (let [path (js/require "path")
              cur-dir (.resolve path ".")]
          {
           (str cur-dir "/src/app/keymap.cljs") {}
           (str cur-dir "/src/app/renderer.cljs") {}
           (str cur-dir "/src/app/styles.cljs") {}
           (str cur-dir "/resources/index.html") {}
           (str cur-dir "/src/app/regs.cljs") {}
           }))
      (do
        #_(.log js/console "(:ide-files (cljs.reader/read-string data))" (:ide-files (cljs.reader/read-string data)))
        (:ide-files (cljs.reader/read-string data))))]))

(defn read-ide-settings []
  (read-file config-file fn-load-ide-files))
