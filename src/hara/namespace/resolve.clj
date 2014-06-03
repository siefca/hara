(ns hara.namespace.resolve
  (:require [hara.common.error :refer [error suppress]]))

(defn resolve-ns
  "resolves the namespace or else returns nil if it does not exist

  (resolve-ns 'clojure.core) => 'clojure.core

  (resolve-ns 'clojure.core/some) => 'clojure.core

  (resolve-ns 'clojure.hello) => nil"
  {:added "2.1"}
  [sym]
  (let [nsp  (.getNamespace sym)
        nsym (or  (and nsp
                       (symbol nsp))
                  sym)]
    (if nsym
      (suppress (do (require nsym) nsym)))))

(defn ns-vars
  "lists the vars in a particular namespace
  
  (ns-vars 'hara.namespace.resolve) => '[ns-vars resolve-ns]"
  {:added "2.1"} 
  [ns]
  (vec (sort (keys (ns-publics ns)))))