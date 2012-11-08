(ns fourclojure.client
  (:require [clojure.string :as string]
            [clj-http.client :as http]
            [cheshire.core :as json]))

(def ^:dynamic *problem-id* nil)
(def ^:dynamic *current-problem* nil)
(def ^:dynamic *cookie* nil)

;; TODO does not support "See the solutions that the users you follow have submitted"
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

;; TODO check result
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

(defn get-problem [id]
  (json/parse-string
   (:body
    (http/get
      (str "http://www.4clojure.com/api/problem/" id)
              {:accept :json}))))

(defn run-and-submit
  [problem-id
   {:keys [tests cookie]
    :or {tests []}}
   forms]
  {:pre [problem-id]}
  (let [body (string/join
               " " (map pr-str forms))]
    (if-not (run-tests body tests)
      (str "/o\\ Fail /o\\\n") ;; TODO which test failed?
      (let [response (remote-check body)]
        (println (str "body " body))
        (if (string/blank? (:message response))
         (str "/o\\ Error /o\\\n"
              (:error response))
         (str "\\o/ Success \\o/\n"
              (clean (:message response)))))
      )))

(defn run-and-submit-current [forms]
  (run-and-submit *problem-id*
                  {:tests (:tests *current-problem*)
                   :cookie *cookie*}
                  forms))

;;
;; Commands
;;

(defn set-cookie! [c]
  (def ^:dynamic *cookie* c))

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

(defn select! [id]
  (def ^:dynamic *problem-id* id)
  (def ^:dynamic *current-problem*
       (parse-problem (get-problem id) id))
  (view))

(defmacro check
  "check your solution of a 4clojure problem
   parameters
    - problem-id: the id of the problem to solve
    - body: your solution, exactly like you would enter it in the code box at 4clojure.com, plain code (not quoted, not as string)
    - config: map of optional config options,
      {:tests quoted seq of the tests from 4clojure.com, to be run locally before submitting. If any of the tests fail, they will not be submitted
       :cookie value of the \"ring-session\"-cookie (extract it from your browser when logged in), needed for persisting your results}"
  [& body]
  `(run-and-submit-current '~body))
