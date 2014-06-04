(ns hara.expression.compile-test
  (:use midje.sweet)
  (:require [hara.expression.compile :refer :all]))

^{:refer hara.expression.compile/const :added "2.1"}
(fact "converts an expression into a constant at compile time"

  (const (+ 1 2)) => 3
  
  (macroexpand '(const (+ 1 2))) => 3)

^{:refer hara.expression.compile/applym :added "2.1"}
(fact "Allow macros to be applied to arguments just like functions"

  (applym const '((+ 1 2))) => 3
  
  (macroexpand '(applym const '((+ 1 2))))
  => 3)