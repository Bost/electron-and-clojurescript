(ns app.styles
  (:require
   [garden.core :as g]
   [garden.units :as u]
   [garden.selectors :as s]
   [garden.stylesheet :as stylesheet]
   ))

(defn styles [files]
  [:style {:type "text/css"}
   (let [cols (count files)
         tabs (map-indexed
             (fn [idx f]
               (let [i (inc idx)]
                 [(keyword (str ".a" i))
                  {:grid-column (str "col " idx)
                   :grid-row "row 1"}])) files)]
     #_(println "tabs" tabs)
     (g/css
      [:body {:font-family "monospace"}]
      [:.wrapper
       {:display "grid"
        ;; :grid-gap "10px"
        :grid-template-columns (str "repeat(" cols ", [col] auto)")
        :grid-template-rows (str "repeat(" cols ", [row] auto)")
        :background-color "#fff"
        :color "#444"}]

      [:.box
       {
        ;; .cm-s-solarized.CodeMirror ;; TODO use 'less'
        :background-color "#002b36"
        ;; :background-color "#444"
        :color "#fff"
        ;; :border-radius "4px"
        :padding "4px"
        ;; :margin "4px"
        :font-size "100%"}
       ]
      tabs
      [:.c
       {:grid-column (str "col 1 / span " cols)
        :grid-row "row 2"}]
      [:.d
       {:grid-column (str "col 1 / span " cols)
        :grid-row "row 3"}]
      [:.e
       {:grid-column (str"col 1 / span " cols)
        :grid-row "row 4"}]


      ))])
