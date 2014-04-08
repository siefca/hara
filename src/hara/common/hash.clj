(ns hara.common.hash
  (:require [clojure.string :as string]))

(defn hash-label
  "Returns a keyword repesentation of the hash-code.
   For use in generating internally unique keys

    (h/hash-label 1)
    ;=> \"__1__\"
  "
  ([obj] (str "__" (.hashCode obj) "__"))
  ([obj & more]
     (->> (map #(.hashCode %) (cons obj more))
          (string/join "_")
          (format "__%s__"))))