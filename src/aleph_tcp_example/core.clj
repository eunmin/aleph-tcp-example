(ns aleph-tcp-example.core
  (:require [aleph.tcp :as tcp]
            [aleph.netty :refer [wait-for-close]]
            [manifold.stream :as s]
            [clojure.string :refer [trim-newline]]
            [gloss.core :as gloss]
            [gloss.io :as io]))

(def protocol (gloss/string :utf-8 :delimiters ["\n"]))

(defn create-handler [f]
  (fn [s info]
    (s/connect-via (->> (io/decode-stream s protocol)
                        (s/map #(assoc info :body (trim-newline %)))
                        (s/map f))
                   #(let [r (s/put! s (io/encode protocol (:body %)))]
                      (s/close! s)
                      r)
                   s)))

(defn handler [{:keys [remote-addr ssl-session server-port server-name body]}]
  (println body)
  {:body "200 OK\n\n"})

(defn start-server [port]
  (tcp/start-server (create-handler #'handler) {:port port}))

(defn stop-server [^java.io.Closeable s]
  (.close s))

(defn -main [& [port]]
  (let [s (start-server (Integer/parseInt port))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(stop-server s)))
    (wait-for-close s)))
