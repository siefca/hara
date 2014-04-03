(ns hara.test-string
  (:require [hara.string :as h]
            [midje.sweet :refer :all]))

(fact "replace-all"
  (h/replace-all "hello there, hello again" "hello" "bye")
  => "bye there, bye again")

(fact "starts-with"
  (h/starts-with? "prefix" "pre") => true
  (h/starts-with? "prefix" "suf") => false)