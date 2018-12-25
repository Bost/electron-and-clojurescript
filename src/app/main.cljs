(ns app.main)

(enable-console-print!)

(def app            (-> "electron" js/require .-app ))
(def BrowserWindow  (-> "electron" js/require .-BrowserWindow))
(def local-shortcut (-> "electron-localshortcut" js/require))

(goog-define dev? false)

(defn load-page
  "When compiling with `:none` the compiled JS that calls .loadURL is
  in a different place than it would be when compiling with optimizations
  that produce a single artifact (`:whitespace, :simple, :advanced`).

  Because of this we need to dispatch the loading based on the used
  optimizations, for this we defined `dev?` above that we can override
  at compile time using the `:clojure-defines` compiler option."
  [window]
  (.loadURL window (str "file://" js/__dirname (if dev? "/../.." "") "/index.html")))

(def browser-window (atom nil))

(defn register [key-chord callback]
  (.register local-shortcut @browser-window key-chord callback))

(defn init-browser []
  (reset! browser-window
          (BrowserWindow. #js {:width 800 :height 600
                               :frame true :show true
                               ;; :useContentSize true
                               :webPreferences #js {:nodeIntegration true}}))
  (load-page @browser-window)
  (if dev? (.openDevTools @browser-window))
  (register "Ctrl+O"
            (fn []
              (println "Ctrl+O") ;; see OS console
              #_(let [active @(rf/subscribe [:active-file])
                      editor @(rf/subscribe [:ide-file-editor active])]
                  (println "Ctrl+O" editor))))
  #_(register "Super+Q" (fn [editor]
                        (let [active @(rf/subscribe [:active-file])
                              editor @(rf/subscribe [:ide-file-editor active])
                              open-files @(rf/subscribe [:open-files])]
                          (k/next-active editor open-files))))

  (.on @browser-window "resize"
       (fn [v] (println "resize browser-window" (.getBounds @browser-window))))
  (.on @browser-window "closed" #(reset! browser-window nil)))

(defn will-quit []
  (println "will-quit"))

(defn init []
  (.on app "window-all-closed"
       #(when-not (= js/process.platform "darwin") (.quit app)))
  (.on app "ready" init-browser)
  (.on app "will-quit" will-quit)
  (set! *main-cli-fn* (fn [] nil)))
