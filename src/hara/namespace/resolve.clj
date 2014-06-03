(ns hara.namespace.resolve
  (:require [hara.common.error :refer [error suppress]]))

(defn resolve-ns
  "resolves the namespace or else returns nil if it does not exist

  (resolve-ns 'clojure.core) => 'clojure.core

  (resolve-ns 'clojure.core/some) => 'clojure.core

  (resolve-ns 'clojure.hello) => nil"
  {:added "2.0"}
  [sym]
  (let [nsp  (.getNamespace sym)
        nsym (or  (and nsp
                       (symbol nsp))
                  sym)]
    (if nsym
      (suppress (do (require nsym) nsym)))))

