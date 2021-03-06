(ns chat-dumper.api
  (:require
   [chat-dumper.edn     :as edn]
   [clojure.data.json   :as json]
   [org.httpkit.client  :as http]
   [clojure.string      :as s]))


;; Access to API methods:

(def token
  ((edn/config) :token))

(defn api-call-url
  [method-name]
  (format "https://api.vk.com/method/%s" method-name))

(def default-opts
  {:access_token token
   :v "5.24"})

(defn call-async-api [method opts]
  (future
    (when-let
      [resp  @(http/get (api-call-url method)
                {:query-params (merge default-opts opts)})]
       (json/read-str (:body resp) :key-fn keyword))))

(defn call-api [method opts]
  @(call-async-api method opts))


;; API methods (just for test):

#_(defn send-message-to-chat [chat-id message]
  (call-api "messages.send" {:chat_id chat-id
                             :message message}))

#_(defn send-message-to-user [user-ids message]
  (call-api "messages.send" {:user_ids user-ids
                             :message  message}))
