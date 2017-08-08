(ns demos.badge)

(defn main [app electron]
  (let [ipc (.-ipcMain electron)]
    (.on ipc "bounce-dock" (fn [event arg]
                             #_(js/console.log "bounce-dock" #_event #_arg)
                             (js/console.log "bounce-dock" arg)
                             #_(.. app -dock bounce)))
    (.on ipc "set-badge" (fn [event arg]
                           #_(js/console.log "set-badge" #_event #_arg)
                           (js/console.log "set-badge" arg)
                           #_(.. app -dock (setBadge arg))))))


(defn renderer [electron]
  (let [ipc (.-ipcRenderer electron)]
    (.send ipc "bounce-dock")
    (.send ipc "set-badge" "122")))


