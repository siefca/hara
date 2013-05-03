(ns hara.test-nothing
  (:use midje.sweet
        hara.nothing))

(fact "booleans"
 (bool<- <true>) => true
 (bool<- <false>) => false)

(fact "!not"
  (bool<- (!not <true>)) => false
  (bool<- (!not <false>)) => true)

(fact "!and"
  (bool<- (!and <true>  <true>)) => true
  (bool<- (!and <true>  <false>)) => false
  (bool<- (!and <false> <true>)) => false
  (bool<- (!and <false> <false>)) => false)

(fact "!or"
  (bool<- (!or <true>  <true>)) => true
  (bool<- (!or <true>  <false>)) => true
  (bool<- (!or <false> <true>)) => true
  (bool<- (!or <false> <false>)) => false)

(fact "|bool|"
  (bool<- (|bool| true)) => true
  (bool<- (|bool| false)) => false)
