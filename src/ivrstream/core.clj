(ns ivrstream.core
  (:require [org.httpkit.server :as http-kit]
            [cheshire.core :as json]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]))

;; Storage for WebSocket clients (voice bot)
(def clients (atom #{}))

;; Channel for receiving events from Cisco
(def cisco-event-chan (async/chan))

;; Cisco API URL from environment variable
(def cisco-api-url (System/getenv "CISCO_API_URL"))

;; Broadcasts a message to all connected clients
(defn broadcast-to-clients [msg]
  (doseq [client @clients]
    (http-kit/send! client (json/generate-string msg))))

;; Simulates Cisco Call Center events
(defn simulate-cisco-events []
  (async/go-loop []
    (let [event {:event "call_started"
                 :call_id (str "call-" (rand-int 10000))
                 :caller (str "+994" (rand-int 9999999))
                 :timestamp (System/currentTimeMillis)}]
      (log/info "Simulated Cisco event:" event)
      (async/>! cisco-event-chan event)
      (async/<! (async/timeout 5000)) ;; Event generated every 5 seconds
      (recur))))

;; Sends a command to Cisco IVR
(defn send-to-cisco-ivr [command]
  (log/info "Sending to Cisco IVR at" cisco-api-url ":" command)
  ;; Sends HTTP request to Cisco API
  (try
    (let [response (http-kit/post (str cisco-api-url "/ivr")
                                 {:body (json/generate-string command)})]
      (log/info "Cisco response:" response)
      {:status "success" :command command})
    (catch Exception e
      (log/error "Failed to send to Cisco:" (.getMessage e))
      {:status "error" :error (.getMessage e)})))

;; WebSocket handler for client connections
(defn ws-handler [request]
  (http-kit/with-channel request channel
    (swap! clients conj channel)
    (log/info "Voice-bot connected. Total clients:" (count @clients))

    ;; Handles incoming messages from the voice bot
    (http-kit/on-receive channel
      (fn [data]
        (let [msg (json/parse-string data true)]
          (log/info "Received from voice-bot:" msg)
          (cond
            ;; Audio event
            (= (:event msg) "audio")
            (do
              (log/info "Proxying audio to Cisco:" (:payload msg))
              (send-to-cisco-ivr {:type "audio" :payload (:payload msg)}))

            ;; TTS command
            (= (:command msg) "say")
            (do
              (log/info "Proxying TTS to Cisco:" (:text msg))
              (send-to-cisco-ivr {:type "say" :text (:text msg)}))

            :else
            (log/warn "Unknown message:" msg)))))

    ;; Handles client disconnection
    (http-kit/on-close channel
      (fn [status]
        (swap! clients disj channel)
        (log/info "Voice-bot disconnected. Status:" status)))))

;; Proxies Cisco events to the voice bot
(defn start-cisco-event-proxy []
  (async/go-loop []
    (when-let [event (async/<! cisco-event-chan)]
      (log/info "Broadcasting Cisco event to voice-bot:" event)
      (broadcast-to-clients event)
      (recur))))

;; Starts the WebSocket server
(defn start-server []
  (log/info "Starting IVRStream WebSocket server on ws://localhost:8080/ws")
  (simulate-cisco-events)
  (start-cisco-event-proxy)
  (http-kit/run-server ws-handler {:port 8080}))

;; Main entry point
(defn -main []
  (start-server))
