(ns chat-dumper.core
  (:require
   [chat-dumper.edn    :as edn]
   [chat-dumper.api    :as api]
   [chat-dumper.auth   :as auth]
   [clojure.string     :as s]
   [clj-time.coerce    :as c]))

(def max-count 200)
#_(def token ((edn/config) :token))



(defn get-history-from-user
  [offset user-id]
  (Thread/sleep 350)
  (:response (api/call-api "messages.getHistory"
                           {:offset offset
                            :count max-count
                            :user_id user-id
                            :rev "1"})))


(defn total-count
  [offset user-id]
  (:count (get-history-from-user offset user-id)))

(defn items
  [offset user-id]
  (:items (get-history-from-user offset user-id)))



(defn dump-history
  [user-id]
  (loop [offset 0
         result []]
    (let [messages (map (juxt :user_id :date :body) (items offset user-id))
          total-count (total-count offset user-id)]
      (if (>= (+ offset (count messages)) total-count)
        (into result messages)
        (do
          (println offset "done")
          (recur (+ offset max-count) (into result messages)))))))

(defn get-name
  [user-id]
  (->> (api/call-api "users.get" {:user_id user-id
                                  :fields "first_name,last_name"})
       :response
       (map (juxt :first_name :last_name))
       first
       (s/join " ")))

(defn parse-time
  [datetime]
  (.format
   (java.text.SimpleDateFormat. "(hh:mm:ss dd.MM.yyyy)")
    datetime))

(defn output-data
  [logs]
  (doseq [xs logs]
    (let [[user datetime body] xs]
      (println (str (get-name user) "  " (parse-time datetime)))
      (println body)
      (println ""))))

(defn write-data
  [data output-file]
  (binding [*out* (java.io.PrintWriter. output-file)]
    (output-data data)))

