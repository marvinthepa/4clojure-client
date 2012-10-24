# 4clojure.client

A Clojure library designed to check and submit your solutions for http://www.4clojure.com problems right from your editor of choice (it was written in Vim, of course).

It is a quick hack and far from finished, error handling is non-existent and the output is very crude.

You have been warned.

## Usage

    (use 'fourclojure.client)

    ;; just submit to the homepage without login
    (check 1
           {}
           true)

    ;; run tests offline before submitting (just copy the tests from the website, and wrap them in a quoted seq)
    (check 5
           {:tests '[(= __ (conj '(2 3 4) 1))
                     (= __ (conj '(3 4) 2 1))]}
           [1 2 3 4])

    ;; submit to the homepage with "login" (copy the value of the "ring-session" cookie from your browser when logged in
    (check 1
           {:cookie "d0217053-6447-47c2-ab8d-8e9f436e18d9"}
           true)

## License

Copyright © 2012 Martin Sander

Distributed under the Eclipse Public License, the same as Clojure.
