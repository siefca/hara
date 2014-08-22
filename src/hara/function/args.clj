(ns hara.function.args)

(defn vargs?
  "checks that function contain variable arguments

  (vargs? (fn [x])) => false

  (vargs? (fn [x & xs])) => true"
  {:added "2.1"}
  [f]
  (if (some #(= "getRequiredArity" (.getName %))
           (.getDeclaredMethods (class f)))
    true
    false))

(defn varg-count
  "counts the number of arguments types before variable arguments

  (varg-count (fn [x y & xs])) => 2

  (varg-count (fn [x])) => nil"
  {:added "2.1"}
  [f]
  (if (some #(= "getRequiredArity" (.getName %))
           (.getDeclaredMethods (class f)))
     (.getRequiredArity f)))

(defn arg-count
  "counts the number of non-varidic argument types

  (arg-count (fn [x])) => [1]

  (arg-count (fn [x & xs])) => []

  (arg-count (fn ([x]) ([x y]))) => [1 2]"
  {:added "2.1"}
  [f]
  (let [ms (filter #(= "invoke" (.getName %))
                   (.getDeclaredMethods (class f)))
        ps (map (fn [m] (.getParameterTypes m)) ms)]
    (map alength ps)))

(defn arg-check
  "counts the number of non-varidic argument types

  (arg-check (fn [x]) 1) => true

  (arg-check (fn [x & xs]) 1) => true

  (arg-check (fn [x & xs]) 0)
  => (throws Exception \"Function must accomodate 0 arguments\")"
  {:added "2.1"}
  [f num]
  (or (if-let [vc (varg-count f)]
        (<= vc num))
      (some #(= num %) (arg-count f))
      (throw (Exception. (str "Function must accomodate " num " arguments")))))
