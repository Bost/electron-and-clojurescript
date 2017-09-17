(ns app.fs
  (:require
   [re-frame.core :as rf]
   [clojure.string :as s]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   [app.regs]
   ))

(defn read [fs file editor open-files]
  (let [content @(rf/subscribe [:ide-file-content file])]
    (if content
      (.setValue (.-doc editor) content)
      (.readFile fs file "utf8"
                 (fn [err data]
                   (if err
                     (.log js/console err)
                     (do
                       (.log js/console (count data) "bytes loaded")
                       (.setValue (.-doc editor) data)
                       (rf/dispatch [:ide-file-content-change [file data]])
                       )))))))

(defn save [fs fname data]
  (.writeFile fs fname data
              (fn [err _]
                (if err
                  (.log js/console err)
                  (.log js/console (count data) "bytes saved")))))

