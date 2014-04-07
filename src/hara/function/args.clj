(ns hara.function.args)

(defn vargs? [f]
  (if (some #(= "getRequiredArity" (.getName %))
           (.getDeclaredMethods (class f)))
    true
    false))

(defn varg-count [f]
  (if (some #(= "getRequiredArity" (.getName %))
           (.getDeclaredMethods (class f)))
     (.getRequiredArity f)))

(defn arg-count [f]
  (let [ms (filter #(= "invoke" (.getName %))
                   (.getDeclaredMethods (class f)))
        ps (map (fn [m] (.getParameterTypes m)) ms)]
    (map alength ps)))