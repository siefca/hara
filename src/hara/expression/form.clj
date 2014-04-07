(ns hara.expression.form
  (:require [hara.common.error :refer [error suppress]]
            [hara.expression.resolve :refer [resolve-ns]]))

(defn func-prep [data]
  (fn [x]
    (if (= '?% x) data x)))

(defn func-rprep [data]
  (fn [x]
    (cond (= '?% x) data

          (symbol? x)
          (do (resolve-ns x)
              x)

          :else x)))

(defn func-form
  ([form] (func-form form func-rprep))
  ([form fp]
     (let [data (gensym)
           rform   (clojure.walk/prewalk
                    (fp data)
                    form)]
       `(fn [~data] ~rform))))

(defn func-fn
  ([form] (func-fn form func-rprep))
  ([form fp]
     (try
       (eval (func-form form fp))
       (catch clojure.lang.Compiler$CompilerException e
         (error e (str "Cannot evaluate form: " form))))))

(defn func-eval
  ([form data] (func-eval form data func-rprep))
  ([form data fp]
     ((func-fn form fp) data)))

(comment
  (func-fn 'oeuoe/oeuoeu)
  (>pst))
