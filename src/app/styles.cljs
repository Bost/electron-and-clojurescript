(ns app.styles
  (:require
   [garden.core :as g]
   [garden.units :as un]
   [garden.selectors :as se]
   [garden.stylesheet :as stylesheet]
   [utils.core :refer [in? dbg sjoin next-cyclic]]
   ))

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

(defn window-height []
  (let [
        ;; (->> (js/require "electron")
        ;;        .-screen
        ;;        .getPrimaryDisplay
        ;;        .-workAreaSize .-height)
        ;; opts (js/require "electron-browser-window-options")
        ;; bw (->> electron .-remote)
        ;; wc (->> electron .-remote .-webContents .getFocusedWebContents)
        ]
    (- (->> js/document .-documentElement .-clientHeight) 95)))

(defn window-width []
  (- (->> js/document .-documentElement .-clientWidth) 0))
;; TODO use (window-width) for col-width calculation
(def col-width "170px")

(defn common []
  [[:body {:font-family "monospace" :margin 0}]
   [(class box)
    (conj
     #_{:border-radius "4px" :padding "0px" :margin "0px" :font-size "100%"})
    ]])

(defn row-height [cnt-files]
  (/ (window-height) cnt-files))

(defn tabs-on-left [files]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cnt-files (count files)]
           [
            [:.wrapper
             {:display "grid"
              :grid-template-columns (sjoin [col-width "auto"])
              }]
            (map-indexed
             (fn [i _]
               (let [idx (inc i)]
                 [(class (str tabs idx))
                  {:grid-column 1 :grid-row idx}])) files)
            [(class editor)
             {:grid-column 2 :grid-row 1}]
            [(class stats)
             {:grid-column 2 :grid-row 2}]
            [(class cmd-line)
             {:grid-column 2 :grid-row 3}]
            ]))
        (apply g/css))])

(defn tabs-on-top [files]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cnt-files (count files)]
           [
            [:.wrapper
             (conj
              {:display "grid"
               :grid-template-columns (str "repeat(" cnt-files ", [col] auto)")
               :grid-template-rows 3})
             ]
            (map-indexed
             (fn [i _]
               (let [idx (inc i)]
                 [(class (str tabs idx))
                  {:grid-column (str "col " idx) :grid-row "row 1"}])) files)
            [(class editor)
             {:grid-column (str "col 1 / span " cnt-files) :grid-row "row 2"}]
            [(class stats)
             {:grid-column (str "col 1 / span " cnt-files) :grid-row "row 3"}]
            [(class cmd-line)
             {:grid-column (str "col 1 / span " cnt-files) :grid-row "row 4"}]
            ]))
        (apply g/css))])

(defn no-tabs [files]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cnt-files (count files)]
           [
            [:.wrapper
             (conj
              {:display "grid"
               :grid-template-columns 1
               :grid-template-rows 2
               })]
            (map-indexed
             (fn [i _]
               (let [idx (inc i)]
                 [(class (str tabs idx))
                  {:grid-column (str "col " idx) :grid-row "row 1"}])) files)
            [(class editor)
             {:grid-column 1 :grid-row 1}]
            [(class stats)
             {:grid-column 1 :grid-row 2}]
            ]))
        (apply g/css))])

(defn tabs-on-right [files]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cnt-files (count files)]
           [
            [:.wrapper
             {:display "grid"
              :grid-template-columns (sjoin ["auto" col-width])
              ;; :grid-template-rows (str "repeat(" cols ", [row] auto)")
              }
             ]
            (map-indexed
             (fn [i _]
               [(class (str tabs (inc i)))
                {:grid-column 2 :grid-row (inc i)}]) files)
            [(class editor)
             {:grid-column 1 :grid-row 1}]
            [(class stats)
             {:grid-column 1 :grid-row 2}]
            [(class cmd-line)
             {:grid-column 1 :grid-row 3}]
            ]))
        (apply g/css))])
