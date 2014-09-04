(ns chat-dumper.auth
  (:require
   [chat-dumper.edn     :as edn]
   [clojure.java.browse :as browse]))


(def app-id
  ((edn/config) :app_id))


;; Authorizing in app via OAuth:

(defn oauth-url
  [app-id scope redirect-uri display version]
  (format
   "https://oauth.vk.com/authorize?client_id=%s&scope=%s&redirect_uri=&s&display=&s&v=%s&response_type=token&revoke=1"
   app-id
   scope
   redirect-uri
   display
   version))


(def permissions
  "friends,photos,audio,video,docs,messages")

(def standalone-redirect-url "https://oauth.vk.com/blank.html")

(defn make-auth []
  (browse/browse-url
   (oauth-url app-id permissions standalone-redirect-url "popup" "5.24")))

