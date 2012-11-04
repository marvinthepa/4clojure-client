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
      (string/replace #"<.?span.*?>" "")
      (string/replace #"<.?br\s?.?>" "\n")
      (string/replace #"\t\s" "")
      (string/replace #"<.?ul>" "")
      (string/replace #"<li>" "- ")
      (string/replace #"<\/li>" "")
      (string/replace #"<.?i>" "")
      (string/replace #"&mdash;" "-")
      (string/replace #"<.?strong>" "")
      (string/replace #"\n{2,}" "\n\n")))

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

(defn force-cols [cols s]
  (string/replace (apply str
                         (interpose "\n"
                                    (re-seq #".{0,79}[\s\n]" (str s "\n"))))
                  #"\n{2}" "\n"))

(defn select [id]
  (def ^:dynamic *current-problem*
       (parse-problem (client/get-problem id) id)))


;;
;; Commands
;;


(defn view
  ([]
     (if *current-problem*
       (let [{:keys [id title description tests
                     user difficulty restricted]} *current-problem*]
         (println
          (force-cols 80
                      (str
                       id ". *** " title " ***\n"
                       "author: " user "\n"
                       "level: " difficulty  "\n\n"
                       (clean description) "\n\n"
                       "Tests:" "\n"
                       (apply str (interpose "\n"
                                             (map clean tests)))
                       (when (seq restricted)
                         (str "\n\n" "Restrictions: " (pr-str restricted)))))))
       (println "No problem selected - try (view <id>).")))
  ([id]
     (do (select id)
         (view))))


(defmacro propose [& body]
  `(let [rsp# (remote-check ''~@body)]
     (println
      (str
       (if (string/blank? (:message rsp#))
         (str "/o\\ Error /o\\\n"
              (:error rsp#))
         (str "\\o/ Success \\o/\n"
              (clean (:message rsp#))))))))
