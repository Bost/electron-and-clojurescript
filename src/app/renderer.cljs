(ns app.renderer
  (:require [demos.filesystem :as fs]
            #_[reagent.core :as reagent]
            #_[re-frame.core :as re-frame]))

(defn init []
  (js/console.log "Starting Application")
  (fs/renderer))
