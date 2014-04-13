(ns hara.io
  (:require [clojure.string :as string]))

(defn printv [v & args]
  (println
   (apply format
          (string/join "\n" (flatten v))
          args)))
