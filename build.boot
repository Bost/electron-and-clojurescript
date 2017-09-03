(set-env!
 :source-paths    #{"src"}
 :resource-paths  #{"resources"}
 :dependencies
 '[[org.clojure/clojurescript "1.9.908"]
   [org.clojure/tools.nrepl "0.2.12" :scope "test"]
   [com.cemerick/piggieback "0.2.2" :scope "test"]

   ;; ClojureScript browser REPL using WebSockets
   [weasel "0.7.0" :scope "test"]

   [reagent  "0.7.0"]
   [re-frame "0.10.1"]

   ;; Boot task to compile ClojureScript applications
   [adzerk/boot-cljs "2.1.2" :scope "test"]

   ;; Boot task providing a ClojureScript browser REPL via Weasel and Piggieback.
   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]

   ;; Boot task to automatically reload resources in the browser when files in the project change.
   ;; Communication with the client is via websockets.
   [adzerk/boot-reload    "0.5.2"  :scope "test"]
   #_[cljsjs/codemirror "5.24.0-1"]]
 )

;; (sift :add-jar {'cljsjs/codemirror #"cljsjs/codemirror/development/codemirror.css"})
;; (sift :move {#"cljsjs/codemirror/development/codemirror.css" "vendor/codemirror/codemirror.css"})))

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]])

(deftask prod-build []
  (comp (cljs :ids #{"main"}
              :optimizations :simple)
        (cljs :ids #{"renderer"}
              :optimizations :advanced)))

(deftask dev-build []
  (comp ;; Audio feedback about warnings etc. =======================
        (speak)
        ;; Inject REPL and reloading code into renderer build =======
        (cljs-repl :ids #{"renderer"}
                   :nrepl-opts {:bind "0.0.0.0" :port 36503})
        (reload    :ids #{"renderer"}
                   :ws-host #_"192.168.0.104" "localhost"
                   :on-jsload 'app.renderer/init
                   :target-path "target")
        ;; Compile renderer =========================================
        (cljs      :ids #{"renderer"})
        ;; Compile JS for main process ==============================
        ;; path.resolve(".") which is used in CLJS's node shim
        ;; returns the directory `electron` was invoked in and
        ;; not the directory our main.js file is in.
        ;; Because of this we need to override the compilers `:asset-path option`
        ;; See http://dev.clojure.org/jira/browse/CLJS-1444 for details.
        (cljs      :ids #{"main"}
                   :compiler-options {:asset-path "target/main.out"
                                      :closure-defines {'app.main/dev? true}})
        (target)))
