(ns app.fs
  "File system relevant stuff"
  (:require
   [cljs.reader :refer [read-string]]
   [cljs.pprint :refer [pprint]]
   [re-frame.core :as rf]
   [clojure.string :as s]
   [utils.core :refer [in? sjoin next-cyclic]]
   [app.regs]))

(def home-dir (.homedir (js/require "os")))
;; (def current-dir (.resolve js-path "."))
(def config-file (str home-dir "/.eac/config.edn"))
(def encoding "utf8")
(def fwriter (js/require "writefile"))
(def current-dir (.resolve (js/require "path") "."))

(defn cur-dir [f] (str current-dir f))

(defn read-file [file cont-fn]
  (.readFile (js/require "fs") file (clj->js {:encoding encoding}) cont-fn))

(defn read [file editor open-files]
  (let [content @(rf/subscribe [:ide-file-content file])]
    (if content
      (.setValue (.-doc editor) content)
      (read-file
       file
       (fn [err data]
         (if err
           (throw (js/Error. err))
           (do
             (println file (count data) "bytes loaded")
             (.setValue (.-doc editor) data)
             (rf/dispatch [:ide-file-content-change [file data]]))))))))

(defn save [file data]
  ((js/require "writefile")
   file data
   (fn [err _]
     (if err
       (throw (js/Error. err))
       (println file (count data) "bytes saved")))))

(defn exec [cmd-line]
  (let [cmd (first cmd-line)
        prms (clj->js (rest cmd-line))]
    (println "$" (sjoin cmd-line))
    (let [spawn (.-spawn (js/require "child_process"))
          prc (spawn cmd prms)]
      (js/prc.stdout.setEncoding encoding)
      (js/prc.stdout.on
       "data" (fn [data] (println #_"STDOUT" (str data))))
      ;; boot process output gets displayed only on the STDERR
      #_(js/prc.stderr.on
         "data" (fn [data] (.error js/console #_"STDERR" (str data))))
      (js/prc.stdout.on
       "message" (fn [msg] (println "CHILD got message" msg)))
      (js/prc.stdout.on
       "close" (fn [code] #_(println "Process close code" code)))
      (js/prc.stdout.on
       "exit" (fn [code] (println "Process exit code" code))))))

(defn save-ide-settings []
  (save config-file
        (-> {:tabs-pos @(rf/subscribe [:tabs-pos])
             :ide-files (->> @(rf/subscribe [:ide-files])
                             (map (fn [[k v]]
                                    {k (dissoc v :editor :content)}))
                             (into {}))
             :active-file @(rf/subscribe [:active-file])}
          pprint
          with-out-str)))

(def default-tabs-pos :css/tabs-left)

(def default-ide-files
  {(cur-dir "/src/app/fs.cljs") {}
   (cur-dir "/src/app/keymap.cljs") {}
   (cur-dir "/src/app/renderer.cljs") {}
   (cur-dir "/src/app/styles.cljs") {}
   (cur-dir "/src/app/regs.cljs") {}
   (cur-dir "/src/app/main.cljs") {}
   (cur-dir "/resources/index.html") {}})

;; TODO handle situation with too many opened files
(defn read-ide-settings []
  (read-file config-file
             (fn [err data]
               (rf/dispatch [:tabs-pos-change
                             (if err (do (.error js/console err)
                                         default-tabs-pos)
                                 (->> data
                                      cljs.reader/read-string
                                      :tabs-pos))])
               (rf/dispatch [:ide-files-change
                             (if err (do (.error js/console err)
                                         default-ide-files)
                                 (->> data
                                      cljs.reader/read-string
                                      :ide-files))])
               #_(rf/dispatch [:active-file-change
                             (if err (do (.error js/console err)
                                         default-ide-files)
                                 (->> data
                                      cljs.reader/read-string
                                      :active-file))]))))
