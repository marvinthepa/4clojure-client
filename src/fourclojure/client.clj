(ns fourclojure.client
  (:require [clojure.string :as string]
            [clj-http.client :as http] 
            [cheshire.core :as json]))

;; TODO parse the test, and output the expected vs the real result (like common test libraries do)
(defn run-test [body t]
  (->
    (pr-str t)
    (string/replace "__" body)
    read-string
    eval))

(defn run-tests [body tests]
  (every? identity
          (map
            (partial run-test body)
            tests)))

;; TODO check result
(defn submit [cookie problem-id body]
  (http/post
    (str "http://www.4clojure.com/rest/problem/" problem-id)
    {:headers
     (when cookie
       {"Cookie" (str "ring-session=" cookie)})
     :accept :json
     :form-params {:id problem-id :code body}}))

(defn get-problem [id]
  (json/parse-string
   (:body
    (http/get (str "http://www.4clojure.com/api/problem/" id)
              {:accept :json}))))

(defn run-and-submit
  [problem-id
   {:keys [tests cookie]
    :or {tests []}}
   forms]
  {:pre [problem-id]}
  (let [body (string/join
               " " (map pr-str forms))]
    (and (run-tests body tests)
         (json/parse-string
           (:body (submit cookie problem-id body))))))

(defmacro check
  "check your solution of a 4clojure problem
   parameters
    - problem-id: the id of the problem to solve
    - body: your solution, exactly like you would enter it in the code box at 4clojure.com, plain code (not quoted, not as string)
    - config: map of optional config options,
      {:tests quoted seq of the tests from 4clojure.com, to be run locally before submitting. If any of the tests fail, they will not be submitted
       :cookie value of the \"ring-session\"-cookie (extract it from your browser when logged in), needed for persisting your results}"
  [problem-id config & body]
  `(run-and-submit
     ~problem-id ~config '~body))
