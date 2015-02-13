(ns hara.function.args)

(defn vargs?
  "checks that function contain variable arguments

  (vargs? (fn [x])) => false

  (vargs? (fn [x & xs])) => true"
  {:added "2.1"}
  [^clojure.lang.Fn f]
  (if (some (fn [^java.lang.reflect.Method mthd]
              (= "getRequiredArity" (.getName mthd)))
            (.getDeclaredMethods (class f)))
    true
    false))

(defn varg-count
  "counts the number of arguments types before variable arguments

  (varg-count (fn [x y & xs])) => 2

  (varg-count (fn [x])) => nil"
  {:added "2.1"}
  [f]
  (if (some (fn [^java.lang.reflect.Method mthd]
              (= "getRequiredArity" (.getName mthd)))
            (.getDeclaredMethods (class f)))
    (.getRequiredArity ^clojure.lang.RestFn f)))

(defn arg-count
  "counts the number of non-varidic argument types

  (arg-count (fn [x])) => [1]

  (arg-count (fn [x & xs])) => []

  (arg-count (fn ([x]) ([x y]))) => [1 2]"
  {:added "2.1"}
  [f]
  (let [ms (filter (fn [^java.lang.reflect.Method mthd]
                     (= "invoke" (.getName mthd)))
                   (.getDeclaredMethods (class f)))
        ps (map (fn [^java.lang.reflect.Method m]
                  (.getParameterTypes m)) ms)]
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

(defn op
  "loose version of apply. Will adjust the arguments to put into a function

  (op + 1 2 3 4 5 6) => 21

  (op (fn [x] x) 1 2 3) => 1

  (op (fn [_ y] y) 1 2 3) => 2
  
  (op (fn [_] nil)) => (throws Exception)"
  {:added "2.1"}
  [f & args]
  (let [nargs (count args)
        vargs (varg-count f)]
    (if (and vargs (>= nargs vargs))
      (apply f args)
      (let [fargs (arg-count f)
            candidates (filter #(<= % nargs) fargs)]
        (if (empty? candidates)
          (throw (Exception. (str "arguments have to be of at least length " (apply min fargs))))
          (let [cnt (apply max candidates)]
            (apply f (take cnt args))))))))
