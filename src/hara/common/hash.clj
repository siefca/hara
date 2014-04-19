(ns hara.common.hash
  (:require [clojure.string :as string]))

(defn hash-label
  "Returns a keyword repesentation of the hash-code. For use in 
   generating internally unique keys

   (hash-label 1) => \"__1__\"
   (hash-label \"a\" \"b\" \"c\") => \"__97_98_99__\"
   (hash-label \"abc\") => \"__96354__\""
  {:added "2.0"}
  ([obj] (str "__" (.hashCode obj) "__"))
  ([obj & more]
     (->> (map #(.hashCode %) (cons obj more))
          (string/join "_")
          (format "__%s__"))))