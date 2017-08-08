(ns app.renderer
  (:require [demos.filesystem :as fs]))

(defn init []
  (js/console.log "Starting Application")
  (fs/renderer))
