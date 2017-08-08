(ns app.renderer
  (:require [demos.badge :as ba]))

(def electron      (js/require "electron"))

(defn init []
  (js/console.log "Starting Application")
  (ba/renderer electron))
