(ns hara.common.test-error
  (:require [hara.common.error :as h]
            [midje.sweet :refer :all]))
            
(fact "error"
  (h/error "something") => (throws Exception))

(fact "suppress"
  (h/suppress 2) => 2
  (h/suppress (h/error "e")) => nil
  (h/suppress (h/error "e") :error) => :error
  (h/suppress (h/error "e") h/error-message) => "e")