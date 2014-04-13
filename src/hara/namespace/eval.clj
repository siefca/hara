(ns hara.namespace.eval
  (:require [hara.common.error :refer [error suppress]]))

(defmacro with-ns [ns & forms]
  `(binding [*ns* (the-ns ~ns)]
     ~@(map (fn [form] `(eval '~form)) forms)))

(defmacro with-tmp-ns [& forms]
  `(try
     (create-ns 'sym#)
     (let [res# (with-ns 'sym#
                            (clojure.core/refer-clojure)
                            ~@forms)]
       res#)
     (finally (remove-ns 'sym#))))