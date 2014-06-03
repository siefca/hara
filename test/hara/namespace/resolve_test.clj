(ns hara.namespace.resolve-test
  (:use midje.sweet)
  (:require [hara.namespace.resolve :refer :all]))

^{:refer hara.namespace.resolve/resolve-ns :added "2.0"}
(fact "resolves the namespace or else returns nil if it does not exist"

  (resolve-ns 'clojure.core) => 'clojure.core

  (resolve-ns 'clojure.core/some) => 'clojure.core

  (resolve-ns 'clojure.hello) => nil)
