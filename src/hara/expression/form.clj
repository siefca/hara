(ns hara.expression.form
  (:require [hara.common.error :refer [error suppress]]
            [hara.namespace.resolve :refer [resolve-ns]]))

(defn form-require
  "Makes sure that the namespace is loaded for a particular symbol

  (form-require 'cons) => 'cons
  
  (form-require 'clojure.core/cons) => 'clojure.core/cons"
  {:added "2.1"}
  [x]
  (if (symbol? x)
    (do (if-let [nsp (.getNamespace x)]
          (require (symbol nsp)))
        x)
    x))

(defn form-prep
  "Prepares the form into a function form
  "
  {:added "2.1"}
  [form]
  (let [rform (clojure.walk/prewalk
               form-require
               form)
        sform (str "#" (with-out-str (prn rform)))]
    (with-meta (read-string sform) {:source sform})))

(defn form-fn
  "Creates a function out of a list

  (let [my-inc (form-fn '(+ 1 %))]
  
    (my-inc 1) => 2
  
    (meta my-inc) => {:source \"#(+ 1 %)\\n\"})"
  {:added "2.1"}
  [form]
  (try (let [fform (form-prep form)]
         (with-meta (eval fform) (meta fform)))
    (catch clojure.lang.Compiler$CompilerException e
      (error e (str "Cannot evaluate form: " form)))))

(defn form-eval
  "Evaluates a list as a functions and to a set of arguments.

  (form-eval '(+ 1 %1 %2) 2 3) => 6"
  {:added "2.1"}
  [form & args]
  (apply (form-fn form) args))

(defn form-apply
  "Applies a list as a function to an argument vector

  (form-apply '(+ 1 %1 %2) [2 3]) => 6"
  {:added "2.1"}
  [form args]
  (apply (form-fn form) args))
