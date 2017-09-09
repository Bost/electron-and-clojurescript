(ns app.regs
  (:require
   ;; [cljsjs.codemirror :as cm]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [utils.core :refer [in? dbg next-cyclic]]
   ))

(enable-console-print!)

(rf/reg-event-db :initialize (fn [_ _] {:time (js/Date.) :time-color "#f88"}))

(rf/reg-event-db :time-color-change
                 (fn [db [_ new]] (assoc db :time-color new)))
(rf/reg-event-db :timer (fn [db [_ new]] (assoc db :time new)))
(rf/reg-sub :time (fn [db _] (:time db)))
(rf/reg-sub :time-color (fn [db _] (:time-color db)))

(rf/reg-event-db
 :open-files-change (fn [db [_ new]] (assoc db :open-files new)))
(rf/reg-sub
 :open-files (fn [db _] (:open-files db)))

;; (rf/reg-event-db
;;  :keymap-change (fn [db [_ new]] (assoc db :keymap new)))
;; (rf/reg-sub
;;  :keymap (fn [db _] (:keymap db)))

(rf/reg-event-db
 :active-file-change (fn [db [_ new]] (assoc db :active-file new)))
(rf/reg-sub
 :active-file (fn [db _] (:active-file db)))

(rf/reg-event-db
 :ide-files-change (fn [db [_ new]] (assoc db :ide-files new)))
(rf/reg-sub
 :ide-files (fn [db _] (:ide-files db)))

(rf/reg-event-db
 :ide-file-content-change
 (fn [db [_ [file new]]]
   (assoc-in db [:ide-files file :content] new)))

(rf/reg-sub
 :ide-file-content
 (fn [db [_ file]]
   (get-in db [:ide-files file :content])))
