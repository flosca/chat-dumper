(ns chat-dumper.core
  (:require
   [chat-dumper.edn     :as edn]
   [chat-dumper.api     :as api]
   [chat-dumper.auth    :as auth]
   [chat-dumper.parsers :as p]
   [clojure.string      :as s]
   [clj-time.coerce     :as c]
   [clj-time.format     :as f]
   [clojure.java.io     :as io]))

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
  (let [total-count (-> (get-history-from-user 0 user-id) :response :count)]
    (println "Total count of messages with user:" total-count)
    (loop [offset 0
           result []]
      (let [messages (->> (get-history-from-user offset user-id)
                          :response
                          :items
                          (map p/get-message))
            message-count (count messages)]
        (if (zero? total-count)
          (throw (Exception. "There's nothing to write!\nYour logs with this user are empty. :("))
          (if (>= (+ offset message-count) total-count)
            (do
              (println "All" total-count "messages are ready! Logs are almost stored in.")
              (into result messages))
            (do
              (println (+ message-count offset) "messages are ready")
              (recur (+ offset max-count) (into result messages)))))))))


(defn write-user-log-to-file
  [user-id]
  (let [take-name (comp p/parse-name p/get-name)
        names [(take-name user-id)
               (take-name p/my-id)]
        output-file (str "/home/flosca/" user-id ".txt")
        final-output (if (.exists (io/as-file output-file))
                       (do
                         (io/delete-file output-file)
                         (str "/home/flosca/" user-id ".txt"))
                       output-file)
        logs (dump-history-from-user user-id)]
    (letfn
      [(parse-message
        [[out user-id date body geo fwd attachments]]
        (str (nth names out) "  " (p/convert-datetime date)
             body
             (if (nil? geo) "" (p/parse-geo geo))
          ;   (if (nil? fwd) "" )
             (if (nil? attachments) "" (apply str (mapcat p/parse-att attachments)))
             "\n\n"))]
      (loop [i logs]
        (if (empty? i) (println "Operation is completed. Logs are stored in" final-output)
          (do
            (spit final-output
                  (parse-message (first i)) :append true)
            (recur (rest i))))))))



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
    (println "Total count of messages in chat:" total-count)
    (loop [offset 0
           result []]
      (let [messages (->> (get-history-from-chat offset chat-id)
                          :response
                          :items
                          (map p/get-message))
            message-count (count messages)]
        (if (zero? total-count)
          (throw (Exception. "There's nothing to write!\nYour logs in this chat are empty. :("))
          (if (>= (+ offset message-count) total-count)
            (do
              (println "All" total-count "messages are ready! Logs are almost stored in.")
              (into result messages))
            (do
              (println (+ message-count offset) "messages are ready")
              (recur (+ offset max-count) (into result messages)))))))))

(defn write-chat-log-to-file
  [chat-id]
  (let [logs (dump-history-from-chat chat-id)
        ids-from-logs (set (map second logs))
        output-file (str "/home/flosca/chat_" chat-id ".txt")
        final-output (if (.exists (io/as-file output-file))
                       (do
                         (io/delete-file output-file)
                         (str "/home/flosca/chat_" chat-id ".txt"))
                       output-file)]
    (letfn
      [(make-list-of-ids
        [chat-id]
        (map identity ids-from-logs))
       (make-list-of-names
        [chat-id]
        (map (comp p/parse-name p/get-name) ids-from-logs))]
      (let [ids (make-list-of-ids chat-id)
            names (make-list-of-names chat-id)]
        (letfn
          [(convert-name
            [user-id]
            (let [id chat-id
                  index (.indexOf ids user-id)]
              (nth names index)))
           (parse-message
            [[out user-id date body geo fwd attachments]]
            (str (convert-name user-id) "  " (p/convert-datetime date)
                 body
                 (if (nil? geo) "" (p/parse-geo geo))
                 (if (nil? attachments) "" (apply str (mapcat p/parse-att attachments)))
                 "\n\n"))]
          (loop [i logs]
            (if (empty? i) (println "Operation is completed. Logs are stored in" final-output)
              (do
                (spit final-output
                      (parse-message (first i)) :append true)
                (recur (rest i))))))))))
