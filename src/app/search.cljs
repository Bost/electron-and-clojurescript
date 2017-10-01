(ns app.search)

(defn ofu []
  "<pre>ofu</pre>"
  #_[:pre "ofu"])

(defn search [editor]
  (.openNotification editor (ofu))
  [:span {:class "CodeMirror-search-label"} "Search:"]
  [:input {:type "text" :style {:width "10em"} :class "CodeMirror-search-field"}]
  [:span {:style {:color "#888"} :class "CodeMirror-search-hint"} "(Use /re/ syntax for regexp search)"]
  )
