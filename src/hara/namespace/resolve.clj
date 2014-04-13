(ns hara.namespace.resolve
  (:require [hara.common.error :refer [error suppress]]))

(defn resolve-ns [sym]
  (if-let [nsp (.getNamespace sym)]
    (suppress (require (symbol nsp))
              (fn [e] (error e  "Could not load namespace: " nsp)))))
