(ns app.styles
  (:require
   [re-frame.core :as rf]
   [garden.core :as g]
   [garden.units :as un]
   [garden.selectors :as se]
   [garden.stylesheet :as stylesheet]
   [utils.core :refer [in? sjoin next-cyclic]]
   ))

(enable-console-print!)

(def tabs "tabs")
(def editor "editor")
(def editor-container "editor-container")

(def row-col-stats "row-col-stats")
(def row-col "row-col")
(def stats "stats")

(def cmd-line "cmd-line")
(def box "box")
(def active "active")
(def prev "prev")

(defn class-str [name] (str "." name))
(defn class [name] (keyword (class-str name)))

(def theme "solarized")
(def theme-mode "light" #_"dark")
(def codemirror-theme (str "cm-s-" theme))
(def codemirror-theme-mode (str "cm-s-" theme-mode))

(defn window-width []
  (- (->> js/document .-documentElement .-clientWidth) 0))
;; TODO use (window-width) for col-width calculation
(def col-width 170)
(def col-width-px (str col-width "px"))

(def row-col-width 150)
(def row-col-width-px (str row-col-width "px"))

(defn common []
  [[:body {:font-family "monospace" :margin 0}]
   [(class box)
    (conj
     #_{:border-radius "4px" :padding "0px" :margin "0px" :font-size "100%"})]
   [(class active)
    {:font-weight "bold" :text-decoration "underline"}]
   [(class prev)
    {:font-weight "bold"}]])

(def left "left")
(def right "right")

(defn repeat-row-auto [cnt] (str "repeat(" cnt ", [row] auto)"))

(defn left-right [{:keys [tabs-left files] :as prm}]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cnt-files (count files)]
           [
            [:.lr-wrapper
             {:display "grid"
              :grid-template-columns (sjoin (if tabs-left
                                              [col-width-px "auto"]
                                              ["auto" col-width-px]))
              }]
            [(class left)
             {:grid-column 1 :grid-row 1}]
            [(class right)
             {:grid-column 2 :grid-row 1}]

            [(if tabs-left :.l-wrapper :.r-wrapper)
             {:grid-column 1 :grid-row 1
              :display "grid"
              :grid-template-rows (repeat-row-auto cnt-files)
              }]
            (map-indexed
             (fn [i _]
               (let [idx (inc i)]
                 [(class (str tabs idx))
                  {:grid-column 1 :grid-row idx}])) files)

            [(if tabs-left :.r-wrapper :.l-wrapper)
             {:grid-column 2 :grid-row 1
              :display "grid"
              :grid-template-rows (repeat-row-auto 3)}]
            [(class editor)
             {:grid-column 1 :grid-row 1}]
            [(class row-col-stats)
             {:grid-column 1 :grid-row 2
              :display "grid"
              :grid-template-columns (sjoin [row-col-width-px "auto"])
              }]
            [(class row-col)
             {:grid-column 1 :grid-row 1}]
            [(class stats)
             {:grid-column 2 :grid-row 1}]
            [(class cmd-line)
             {:grid-column 1 :grid-row 3}]]))
        (apply g/css))
   ])

(def tabs-pos #{:tabs-left :tabs-right :tabs-on-top :no-tabs})

(defn left-to-right [{:keys [files] :as prm}] (left-right (assoc prm :tabs-left true)))
(defn right-to-left [{:keys [files] :as prm}] (left-right (assoc prm :tabs-left false)))

(defn tabs-on-top [{:keys [files]}]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cnt-files (count files)]
           [
            [:.wrapper
             {:display "grid"
              :grid-template-columns (str "repeat(" cnt-files ", [col] auto)")}]
            (map-indexed
             (fn [i _]
               (let [idx (inc i)]
                 [(class (str tabs idx))
                  {:grid-column idx :grid-row 1}])) files)
            [(class editor)
             {:grid-column (str "1 / span " cnt-files) :grid-row 2}]
            [(class row-col-stats)
             {:grid-column (str "1 / span " cnt-files) :grid-row 3
              :display "grid"
              :grid-template-columns (sjoin [row-col-width-px "auto"])
              }]
            [(class row-col)
             {:grid-column 1 :grid-row 1}]
            [(class stats)
             {:grid-column 2 :grid-row 1}]
            [(class cmd-line)
             {:grid-column (str " 1 / span " cnt-files) :grid-row 4}]
            ]))
        (apply g/css))])

(defn no-tabs [{:keys [files] :as prm}]
  [:style {:type "text/css"}
   (->> (common)
        (conj
         (let [cnt-files (count files)]
           [
            [:.wrapper {:display "grid"}]
            (map-indexed
             (fn [i _]
               (let [idx (inc i)]
                 [(class (str tabs idx))
                  {:grid-column idx :grid-row 1}])) files)
            [(class editor)
             {:grid-column 1 :grid-row 1}]
            [(class row-col-stats)
             {:grid-column 1 :grid-row 2
              :display "grid"
              :grid-template-columns (sjoin [row-col-width-px "auto"])}]
            [(class row-col)
             {:grid-column 1 :grid-row 1}]
            [(class stats)
             {:grid-column 2 :grid-row 1}]]))
        (apply g/css))])

(def default-window-height-offset 38)

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
    (let [tabs-pos @(rf/subscribe [:tabs-pos])]
      (- (->> js/document .-documentElement .-clientHeight)
         (if-let [tabs-pos @(rf/subscribe [:tabs-pos]) ]
           (or (tabs-pos {:css/tabs-on-top 56})
               default-window-height-offset)
           (do
             (println "WARN: window-height: undefined tabs-pos")
             default-window-height-offset))))))

(defn row-height [cnt-files] (/ (window-height) cnt-files))

(def default-tabs-pos
  "See also ~/.eac/config.edn"
  #_:css/tabs-left :no-tabs)
