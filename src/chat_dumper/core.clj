(ns chat-dumper.core
  (:require
   [chat-dumper.edn    :as edn]
   [chat-dumper.api    :as api]
   [chat-dumper.auth   :as auth]
   [clojure.string     :as s]
   [clojure.java.io    :as io]
   [clj-time.coerce    :as c]))

(def max-count 200)



;; for users:

(defn get-history-from-user
  [offset user-id]
  (let [call
        (api/call-async-api "messages.getHistory"
                            {:offset offset
                             :count max-count
                             :user_id user-id
                             :rev "1"})]
    (do
      (Thread/sleep 350)
      (deref call))))


(defn dump-history-from-user
  [user-id]
  (let [total-count (-> (get-history-from-chat 0 chat-id) :response :count)]
    (loop [offset 0
           result []]
      (let [messages (map (juxt :user_id :body) (-> (get-history-from-user offset user-id)
                                                    :response
                                                    :items))
            message-count (count messages)]
        (if (>= (+ offset message-count) total-count)
          (into result messages)
          (do
            (println (+ message-count offset) "done")
            (recur (+ offset max-count) (into result messages))))))))


;; for chats:


(defn get-history-from-chat
  [offset chat-id]
  (let [call
        (api/call-async-api "messages.getHistory"
                            {:offset offset
                             :count max-count
                             :chat_id chat-id
                             :rev "1"})]
    (do
      (Thread/sleep 350)
      (deref call))))


(defn dump-history-from-chat
  [chat-id]
  (let [total-count (-> (get-history-from-chat 0 chat-id) :response :count)]
    (loop [offset 0
           result []]
      (let [messages (map (juxt :user_id :body) (-> (get-history-from-chat offset chat-id)
                                                    :response
                                                    :items))
            message-count (count messages)]
        (if (>= (+ offset message-count) total-count)
          (into result messages)
          (do
            (println (+ message-count offset) "done")
            (recur (+ offset max-count) (into result messages))))))))





(defn get-name
  [user-id]
  (let [call (api/call-async-api
              "users.get" {:user_id user-id
                           :fields "first_name,last_name"})]
    (do
      (Thread/sleep 350)
      (deref call))))

(defn parse-name
  [name]
  (->> name
       :response
       (map (juxt :first_name :last_name))
       first
       (s/join " ")))


(defn coerce-message-to-string
  "Coerces a message (Clojure data structure: [user-id body]) to a string"
  [message]
  (str ((comp parse-name get-name) (first message)) "\n"
       (second message) "\n\n"))


(defn write-log-to-file
  [logs output-file]
  (->> logs
       (mapcat coerce-message-to-string)
       (apply str)
       (spit output-file)))

(defn write-file [data]
  (with-open [w (clojure.java.io/writer  "f:/w.txt" :append true)]
    (.write w data)))


