(set-env!
 :source-paths    #{"src"}
 :resource-paths  #{"resources"}
 :dependencies
 '[
   ;; When you use :scope "test" dependencies won't end up in your uberjar

   [org.clojure/clojurescript "1.10.520"]
   [cider/piggieback "0.4.1" :scope "test"]
   [nrepl "0.7.0-alpha1" :scope "test"]
   [utils "0.0.0-22-0x5cef"]

   ;; ClojureScript browser REPL using WebSockets
   [weasel "0.7.0" :scope "test"]

   [reagent  "0.8.1"]
   [re-frame "0.10.8"]

   ;; Boot task to compile ClojureScript applications
   [adzerk/boot-cljs "2.1.5" :scope "test"]

   ;; Boot task providing a ClojureScript browser REPL via Weasel and Piggieback.
   [adzerk/boot-cljs-repl "0.4.0" :scope "test"]

   ;; Boot task to automatically reload resources in the browser when files in the project change.
   ;; Communication with the client is via websockets.
   [adzerk/boot-reload    "0.6.0"  :scope "test"]
   [garden "1.3.9"] ; render css
   #_[org.webjars.bower/bootstrap "3.3.6"]
   ;; use `boot show --updates` instead of boot-deps
   ;; [boot-deps "0.1.9"] ;; boot --dependencies boot-deps ancient
   ]
 )

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 )

(deftask prod-build []
  (comp (cljs :ids #{"main"}
              :optimizations :simple)
        (cljs :ids #{"renderer"}
              :optimizations :advanced)))

(deftask dev-build []
  (comp ;; Audio feedback about warnings etc. =======================
        (speak)
        #_(sift :add-jar {'cljsjs/codemirror
                        #"cljsjs/codemirror/development/codemirror.css"})
        #_(sift :move {#"cljsjs/codemirror/development/codemirror.css"
                     "vendor/codemirror/codemirror.css"})

        #_(deraen.boot-less/less) ;; TODO http://lesscss.org/
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
