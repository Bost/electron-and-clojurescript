(ns app.styles
  (:require
   [garden.core :as g]
   [garden.units :as un]
   [garden.selectors :as se]
   [garden.stylesheet :as stylesheet]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   ))

(def col-width "170px")

(defn common []
  [[:body {:font-family "monospace"}]
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
    ]])

(defn tabs-on-top [files]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cols (count files)]
           [
            [:.wrapper
             {:display "grid"
              ;; :grid-gap "10px"
              :grid-template-columns (str "repeat(" cols ", [col] auto)")
              :grid-template-rows (str "repeat(" cols ", [row] auto)")
              :background-color "#fff"
              :color "#444"}]
            (map-indexed
             (fn [i f] [(keyword (str ".a" (inc i)))
                       {:grid-column (str "col " i)
                        :grid-row "row 1"}]) files)
            [:.e {:grid-column (str "col 1 / span " cols) :grid-row "row 2"}]
            [:.s {:grid-column (str "col 1 / span " cols) :grid-row "row 4"}]
            ]))
        (apply g/css))])

(defn no-tabs [files]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cols (count files)]
           [
            [:.wrapper
             {:display "grid"
              ;; :grid-gap "10px"
              :grid-template-columns 1
              :grid-template-rows 2
              :background-color "#fff"
              :color "#444"}]
            (map-indexed
             (fn [i f] [(keyword (str ".a" (inc i)))
                        {:grid-column (str "col " i)
                         :grid-row "row 1"}]) files)
            [:.e {:grid-column 1 :grid-row 1}]
            [:.s {:grid-column 1 :grid-row 2}]
            ]))
        (apply g/css))])

(defn tabs-on-left [files]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cols (count files)]
           [
            [:.wrapper
             {:display "grid"
              ;; :grid-gap "10px"
              :grid-template-columns
              #_(str "repeat(" cols ", [col] auto)")
              (sjoin [col-width "auto"])
              ;; :grid-template-rows (str "repeat(" cols ", [row] auto)")
              :background-color "#fff"
              :color "#444"}]
            (map-indexed
             (fn [i f] [(keyword (str ".a" (inc i)))
                       {:grid-column 1
                        :grid-row (inc i)}]) files)
            [:.e {:grid-column 2 :grid-row (str "1 / " cols)}]
            [:.s {:grid-column 2 :grid-row cols}]]))
        (apply g/css))])

(defn tabs-on-right [files]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cols (count files)]
           [
            [:.wrapper
             {:display "grid"
              ;; :grid-gap "10px"
              :grid-template-columns
              #_(str "repeat(" cols ", [col] auto)")
              (sjoin ["auto" col-width])
              ;; :grid-template-rows (str "repeat(" cols ", [row] auto)")
              :background-color "#fff"
              :color "#444"}]
            (map-indexed
             (fn [i f] [(keyword (str ".a" (inc i)))
                       {:grid-column 2
                        :grid-row (inc i)}]) files)
            [:.e {:grid-column 1 :grid-row (str "1 / " cols)}]
            [:.s {:grid-column 1 :grid-row cols}]
            ]))
        (apply g/css))])
