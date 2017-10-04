(ns app.main)

(def app           (.-app (js/require "electron")))
(def BrowserWindow (.-BrowserWindow (js/require "electron")))
(def local-shortcut (js/require "electron-localshortcut"))

(goog-define dev? false)

(defn load-page
  "When compiling with `:none` the compiled JS that calls .loadURL is
  in a different place than it would be when compiling with optimizations
  that produce a single artifact (`:whitespace, :simple, :advanced`).

  Because of this we need to dispatch the loading based on the used
  optimizations, for this we defined `dev?` above that we can override
  at compile time using the `:clojure-defines` compiler option."
  [window]
  (if dev?
      (.loadURL window (str "file://" js/__dirname "/../../index.html"))
      (.loadURL window (str "file://" js/__dirname "/index.html"))))

(def main-window (atom nil))

(defn mk-window [w h frame? show?]
  (BrowserWindow. #js {:width w :height h :frame frame? :show show?}))

(defn register [key-chord callback]

  #_(let [
        ;; electron (js/require "electron")
        ;; app (.-app electron)
        local-shortcut (js/require "electron-localshortcut")]
    (.register local-shortcut
               (.-BrowserWindow (js/require "electron"))
               key-chord callback))

  #_(let [remote (->> (js/require "electron") .-remote)]
    (.log js/console "remote" remote)
    #_(if (.isRegistered global-shortcut key-chord)
      #_(.unregisterAll global-shortcut)
      (.unregister global-shortcut key-chord))
    #_(if-not (.register global-shortcut key-chord callback)
      (.error js/console key-chord "registration failed")))

  ;; (js/window.addEventListener "keyup" callback true)

  #_(let [remote (.-remote (js/require "electron"))
        hotkey (.require remote "electron-hotkey")]
    #_(js/hotkey.register "global" key-chord  "event-1")
    #_(js/hotkey.register "myWindow" key-chord "event-3")
    (.register hotkey "local" key-chord callback)
    #_(let [app (.-app (js/require "electron"))]
      (.on app "shortcut-pressed" callback))

    ))

(defn init-browser []
  (reset! main-window (mk-window 800 600 true true))
  (load-page @main-window)
  (if dev? (.openDevTools @main-window))
  (.log js/console "local-shortcut:" local-shortcut)
  (register "Ctrl+O" (fn []
                       (.log js/console "Ctrl+O")
                       #_(let [active @(rf/subscribe [:active-file])
                             editor @(rf/subscribe [:ide-file-editor active])]
                         (.log js/console "Ctrl+O" editor))))
  #_(register "Super+Q" (fn [editor]
                        (let [active @(rf/subscribe [:active-file])
                              editor @(rf/subscribe [:ide-file-editor active])
                              open-files @(rf/subscribe [:open-files])]
                          (k/next-active editor open-files))))

  (.on @main-window "closed" #(reset! main-window nil)))

(defn will-quit []
  (.log js/console "will-quit"))

(defn init []
  (.on app "window-all-closed"
       #(when-not (= js/process.platform "darwin") (.quit app)))
  (.on app "ready" init-browser)
  (.on app "will-quit" will-quit)
  (set! *main-cli-fn* (fn [] nil)))
