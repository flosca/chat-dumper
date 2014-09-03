(ns chat-dumper.edn
  (:require
   [clojure.string :as string]
   [clojure.tools.reader.edn :as read-edn]))

(defn config []
  (read-edn/read-string (slurp "config.edn")))

