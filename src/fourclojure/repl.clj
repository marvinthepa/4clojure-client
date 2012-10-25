(ns fourclojure.repl
  (:require [fourclojure.client :as client]
            [cheshire.core :as json]
            [clojure.string :as string]))

(def ^:dynamic *current-problem* nil)
(def ^:dynamic *cookie* nil)

(defn clean [s]
  (-> s
      (string/replace #"\r" "")
      (string/replace #"<.?p>" "")
      (string/replace #"<.?code>" "")
      (string/replace #"<a.*?href=\"(.*?)\".*?>(.*?)<\/a>"
                      "$2 [$1]")
      (string/replace #"<.?span.*?>" "")))

(defn keywordize
  ([source]
     (keywordize source {}))
  ([source seed]
     (into seed
           (map (fn [[k v]] [(keyword k) v])
                source))))

(defn parse-problem [problem-data id]
  (keywordize problem-data {:id id}))

(defn parse-response [response]
  (keywordize (json/parse-string response)))

(defn remote-check [code]
  (parse-response
    (:body (client/submit *cookie*
                          (:id *current-problem*)
                          code))))

;;
;; Commands
;;

(defn view []
  (if *current-problem*
    (let [{:keys [id title description tests]} *current-problem*]
      (println
       (str
        id ". "title "\n"
        (clean description) "\n\n"
        "Tests:" "\n"
        (apply str (interpose "\n"
                              (map clean tests))))))
    (println "No problem selected - use select first.")))


(defn select [id]
  (def ^:dynamic *current-problem*
       (parse-problem (client/get-problem id) id))
  (view))

(defmacro propose [body]
  `(let [rsp# (remote-check '~body)]
     (println
      (str
       (if (string/blank? (:message rsp#))
         (str "/o\\ Error /o\\\n"
              (:error rsp#))
         (str "\\o/ Success \\o/\n"
              (clean (:message rsp#))))))))
