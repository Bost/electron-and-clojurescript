(ns app.panel
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [re-frame.core :as rf]
   [clojure.string :as s]))

(def numPanels 0)
(def panels {})
#_(def editor)

(defn makePanel [where]
  (let [id (inc numPanels)]
    [:div {:id (str "panel-" id)
           :class (str "panel " where)}
     [:a {:title "Remove me!" :class "remove-panel"
          :on-click (fn [] (println "on-click"))} "✖"]
     [:span (str "I'm panel n° " id)]]))

(defn addPanel [editor where]
  (.addPanel editor
             (r/render (makePanel where) (.getInputField editor))
             (clj->js {:position where :stable true})))


