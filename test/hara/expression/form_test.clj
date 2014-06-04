(ns hara.expression.form-test
  (:use midje.sweet)
  (:require [hara.expression.form :refer :all]))

^{:refer hara.expression.form/form-require :added "2.1"}
(fact "Makes sure that the namespace is loaded for a particular symbol"

  (form-require 'cons) => 'cons
  
  (form-require 'clojure.core/cons) => 'clojure.core/cons)

^{:refer hara.expression.form/form-prep :added "2.1"}
(fact "Prepares the form into a function form"
  ^:hidden
  (let [my-inc (form-prep '(+ 1 %))]
    
    ((eval my-inc) 1) => 2
    
    (meta my-inc) => {:source "#(+ 1 %)\n"}))

^{:refer hara.expression.form/form-fn :added "2.1"}
(fact "Creates a function out of a list"

  (let [my-inc (form-fn '(+ 1 %))]
  
    (my-inc 1) => 2
  
    (meta my-inc) => {:source "#(+ 1 %)\n"}))

^{:refer hara.expression.form/form-eval :added "2.1"}
(fact "Evaluates a list as a functions and to a set of arguments."

  (form-eval '(+ 1 %1 %2) 2 3) => 6)

^{:refer hara.expression.form/form-apply :added "2.1"}
(fact "Applies a list as a function to an argument vector"

  (form-apply '(+ 1 %1 %2) [2 3]) => 6)