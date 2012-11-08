(ns fourclojure.client
  (:require [clojure.string :as string]
            [clj-http.client :as http]
            [cheshire.core :as json]))

(def ^:dynamic *problem-id* nil)
(def ^:dynamic *current-problem* nil)
(def ^:dynamic *cookie* nil)
(def ^:dynamic *width* 80)

;; TODO does not support "See the solutions that the users you follow have submitted"
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


;; TODO parse the test, and output the expected vs the real result (like common test libraries do)
(defn run-test [body t]
  (let [code-string (string/replace t "__" body)
        code (read-string code-string)
        result (try (eval code)
                 (catch Exception e
                   (println "exception in test '" code-string "':")
                   (println (.getMessage e))
                   false))]
    (or result
        (do
          (println "you failed the test '" code-string "'")))))

(defn run-tests [body tests]
  (let [results (map
                  (partial run-test body)
                  tests)]
    (every? identity results)))

(defn submit [cookie problem-id body]
  (http/post
    (str "http://www.4clojure.com/rest/problem/" problem-id)
    {:headers
     (when cookie
       {"Cookie" (str "ring-session=" cookie)})
     :accept :json
     :form-params {:id problem-id :code body}}))

(defn remote-check [code]
  (parse-response
    (:body (submit *cookie*
                   (:id *current-problem*)
                   code))))

(defn force-cols [cols s]
  (let [pattern (re-pattern (format ".{0,%d}[\\s\\n]" (dec cols)))]
    (string/replace (apply str
                           (interpose "\n"
                                      (re-seq pattern (str s "\n"))))
                    #"\n{2}" "\n")))

(defn get-problem [id]
  (json/parse-string
   (:body
    (http/get
      (str "http://www.4clojure.com/api/problem/" id)
              {:accept :json}))))

(defn run-and-submit
  [problem-id
   {:keys [tests cookie] :or {tests []}}
   forms]
  {:pre [problem-id]}
  (let [body (string/join
               " " (map pr-str forms))]
    (if-not (run-tests body tests)
      (str "/o\\ Fail /o\\\n") ;; TODO which test failed?
      (let [response (remote-check body)]
        (if (string/blank? (:message response))
         (str "/o\\ Error /o\\\n"
              (:error response))
         (str "\\o/ Success \\o/\n"
              (clean (:message response))))))))

(defn run-and-submit-current [forms]
  (run-and-submit *problem-id*
                  {:tests (:tests *current-problem*)
                   :cookie *cookie*}
                  forms))

(defn select [id]
  (def ^:dynamic *problem-id* id)
  (def ^:dynamic *current-problem*
       (parse-problem (get-problem id) id))
  (view))

;;
;; Commands
;;

(defn set-cookie! [c]
  (def ^:dynamic *cookie* c))

(defn view
  ([]
     (if *current-problem*
       (let [{:keys [id title description tests
                     user difficulty restricted]} *current-problem*]
         (println
          (force-cols *width*
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

(defmacro check
  "check your solution of a 4clojure problem
   parameters
    - body: your solution, exactly like you would enter it in the code box at 4clojure.com, plain code (not quoted, not as string)"
  [& body]
  `(run-and-submit-current '~body))
