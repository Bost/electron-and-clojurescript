(ns app.styles
  (:require
   [garden.core :as g]
   [garden.units :as un]
   [garden.selectors :as se]
   [garden.stylesheet :as stylesheet]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   ))

(def col-width "170px")

(def tabs "a")
(def editor "e")
(def stats "s")
(def cmd-line "c")
(def box "box")
(defn class-str [name] (str "." name))
(defn class [name] (keyword (class-str name)))

(def theme "solarized")
(def theme-mode "light" #_"dark")
(def codemirror-theme (str "cm-s-" theme))
(def codemirror-theme-mode (str "cm-s-" theme-mode))

(defn common []
  [[:body {:font-family "monospace"}]
   [(class box)
    {
     ;; :border-radius "4px"
     :padding "4px"
     ;; :margin "4px"
     :font-size "100%"}
    #_{
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
              :grid-template-rows 3}
             #_{:display "grid"
              ;; :grid-gap "10px"
              :grid-template-columns (str "repeat(" cols ", [col] auto)")
              :grid-template-rows 3
              :background-color "#fff"
              :color "#444"}]
            (map-indexed
             (fn [i f] [(class (str tabs (inc i)))
                       {:grid-column (str "col " i)
                        :grid-row "row 1"}]) files)
            [(class editor) {:grid-column (str "col 1 / span " cols) :grid-row "row 2"}]
            [(class stats) {:grid-column (str "col 1 / span " cols) :grid-row "row 3"}]
            [(class cmd-line) {:grid-column (str "col 1 / span " cols) :grid-row "row 4"}]
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
              }
             #_{:display "grid"
              ;; :grid-gap "10px"
              :grid-template-columns 1
              :grid-template-rows 2
              :background-color "#fff"
              :color "#444"}]
            (map-indexed
             (fn [i f] [(class (str tabs (inc i)))
                        {:grid-column (str "col " i)
                         :grid-row "row 1"}]) files)
            [(class editor) {:grid-column 1 :grid-row 1}]
            [(class stats) {:grid-column 1 :grid-row 2}]
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
              }
             #_{:display "grid"
              ;; :grid-gap "10px"
              :grid-template-columns
              #_(str "repeat(" cols ", [col] auto)")
              (sjoin [col-width "auto"])
              ;; :grid-template-rows (str "repeat(" cols ", [row] auto)")
              :background-color "#fff"
              :color "#444"}]
            (map-indexed
             (fn [i f] [(class (str tabs (inc i)))
                       {:grid-column 1
                        :grid-row (inc i)}]) files)
            [(class editor) {:grid-column 2 :grid-row 1}]
            [(class stats) {:grid-column 2 :grid-row 2}]
            [(class cmd-line) {:grid-column 2 :grid-row 3}]
            ]))
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
              }
             #_{:display "grid"
              ;; :grid-gap "10px"
              :grid-template-columns
              #_(str "repeat(" cols ", [col] auto)")
              (sjoin ["auto" col-width])
              ;; :grid-template-rows (str "repeat(" cols ", [row] auto)")
              :background-color "#fff"
              :color "#444"}]
            (map-indexed
             (fn [i f] [(class (str tabs (inc i)))
                       {:grid-column 2
                        :grid-row (inc i)}]) files)
            [(class editor) {:grid-column 1 :grid-row 1}]
            [(class stats) {:grid-column 1 :grid-row 2}]
            [(class cmd-line) {:grid-column 1 :grid-row 3}]
            ]))
        (apply g/css))])
