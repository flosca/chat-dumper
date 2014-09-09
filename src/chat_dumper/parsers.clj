(ns chat-dumper.parsers
  (:require
   [chat-dumper.edn    :as edn]
   [chat-dumper.api    :as api]
   [clojure.string     :as s]
   [clj-time.coerce    :as c]
   [clj-time.format    :as f]))


(def my-id (:id (edn/config)))


(defn get-message
  [message]
  ((juxt :out
         :user_id
         :date
         :body
         :geo
         :fwd_messages
         :attachments) message))

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

(defn convert-datetime
  [datetime]
  (str (f/unparse
        (f/formatter "(dd.MM.yyyy hh:mm:ss)")
        (c/from-long (* 1000 datetime))) "\n"))

(defn parse-geo
  [geo]
  (str (->> geo :place :title)
       " (" (:coordinates geo) ")\n"))

(defn photo?
  [att]
  (= (:type att) "photo"))


(defn video?
  [att]
  (= (:type att) "video"))


(defn audio?
  [att]
  (= (:type att) "audio"))


(defn doc?
  [att]
  (= (:type att) "doc"))


(defn choose-largest-photo
  [att]
  (let [tt (->> att
                :photo
                keys
                (map str)
                (filter #(.startsWith % ":photo_"))
                sort
                reverse
                first)]
    (keyword (subs tt 1))))

(defn parse-photo
  [att]
  (let [keyword (choose-largest-photo att)]
    (str "Прикрепленная фотография:\n"
         (-> att :photo keyword) "\n")))

(defn parse-audio
  [att]
  (str "Прикрепленная аудиозапись:\n"
       (-> att :audio :artist) " – " (-> att :audio :title) "\n"
       "(" (-> att :audio :url) ")\n"))

(defn parse-video
  [att]
  (str "Прикрепленная видеозапись:\n"
       (-> att :video :title) "\n"))

(defn parse-doc
  [att]
  (str "Прикрепленный документ:\n"
       (-> att :doc :title)
       "(" (-> att :doc :url) ")\n"))

#_(defn parse-fwd
  [messages]
  (str (if (= (count messages) 1)
         "Прикрепленное сообщение:\n"
         "Прикрепленные сообщения:\n")
       (apply str (mapcat get-message messages)) "\n"))


(defn parse-att
  [attachment]
  (cond
   (photo? attachment) (parse-photo attachment)
   (video? attachment) (parse-video attachment)
   (audio? attachment) (parse-audio attachment)
   (doc? attachment)   (parse-doc attachment)
   :else "\n"))




















