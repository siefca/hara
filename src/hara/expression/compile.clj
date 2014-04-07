(ns hara.expression.compile)

(defmacro const [body]
  "Allow static compilation of forms"
  (eval body))

(defmacro applym
  "Allow macros to be applied, just like functions"
  [macro & args]
  (cons macro (#'clojure.core/spread (map eval args))))