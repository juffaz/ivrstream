(defproject ivrstream "0.1.0"
  :description "Middleware for Cisco Call Center IVR voice-bot integration"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.async "1.6.673"]
                 [http-kit "2.7.0"]
                 [cheshire "5.12.0"]
                 [org.clojure/tools.logging "1.3.0"]]
  :main ivrstream.core
  :repl-options {:init-ns ivrstream.core})
