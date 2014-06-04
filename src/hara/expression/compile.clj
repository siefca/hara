(ns hara.expression.compile)

(defmacro const
  "converts an expression into a constant at compile time

  (const (+ 1 2)) => 3
  
  (macroexpand '(const (+ 1 2))) => 3"
  {:added "2.1"}
  [body]
  (eval body))

(defmacro applym
  "Allow macros to be applied to arguments just like functions

  (applym const '((+ 1 2))) => 3
  
  (macroexpand '(applym const '((+ 1 2))))
  => 3"
  {:added "2.1"}
  [macro & args]
  (cons macro (#'clojure.core/spread (map eval args))))