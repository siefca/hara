(ns hara.common.test-constructor
  (:require [hara.common.types :as h :refer [bytes?]]
            [hara.common.constructor :as s]
            [midje.sweet :refer :all]))

(fact "queue"
  (s/queue 1 2 3 4) => [1 2 3 4]
  (pop (s/queue 1 2 3 4)) => [2 3 4])

(fact "uuid"
  (s/uuid) => h/uuid?
  (s/uuid "00000000-0000-0000-0000-000000000000") => h/uuid?
  (s/uuid 0 0) => h/uuid?)

(fact "uri"
  (s/uri "http://www.google.com") => h/uri?
  (s/uri "ssh://github.com") => h/uri?)

(fact "instant"
  (s/instant) => h/instant?
  (s/instant 0) => h/instant?)