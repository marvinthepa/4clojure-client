# 4clojure.client

A Clojure library designed to check and submit your solutions for http://www.4clojure.com problems right from your editor of choice (it was written in Vim, of course).

It is a quick hack and far from finished, error handling is non-existent and the output is very crude.

You have been warned.

## Usage

```clojure
(use 'fourclojure.client)

;; select problem 83
(select! 1)

;; check your solution
(check false)

;; review the problem description and tests
(view)

;; try again
(check true)

;; heureka!

;; set cookie to record your progress
(set-cookie! "24b081f1-4d52-45db-7be8-4429b18ee6f7")
(check true)
```

## License

Copyright © 2012 Martin Sander, Maximilian Karasz

Distributed under the Eclipse Public License, the same as Clojure.
