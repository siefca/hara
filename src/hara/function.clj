(ns hara.function
  (:require [hara.common.error :refer [suppress]]
            [hara.common.checks :refer [hash-map? hash-set?]]))

;; ## Calling Conventions
;;
;; Adds more flexibility to how functions can be called.
;; `call` adds a level of indirection and allows the function
;; to not be present, returning nil instead. `msg` mimicks the way
;; that object-orientated languages access their functions.
;;

(defn call
  "Executes `(f v1 ... vn)` if `f` is not nil

    (call nil 1 2 3) ;=> nil

    (call + 1 2 3) ;=> 6
  "
  ([f] (if-not (nil? f) (f)) )
  ([f v] (if-not (nil? f) (f v)))
  ([f v1 v2] (if-not (nil? f) (f v1 v2)))
  ([f v1 v2 v3] (if-not (nil? f) (f v1 v2 v3)))
  ([f v1 v2 v3 v4 ] (if-not (nil? f) (f v1 v2 v3 v4)))
  ([f v1 v2 v3 v4 & vs] (if-not (nil? f) (apply f v1 v2 v3 v4 vs))))

(defn msg
  "Message dispatch for object orientated type calling convention.

    (def obj {:a 10
              :b 20
              :get-sum (fn [this]
                        (+ (:b this) (:a this)))})

    (msg obj :get-sum) ;=> 30
  "
  ([obj kw] (call (obj kw) obj))
  ([obj kw v] (call (obj kw) obj v))
  ([obj kw v1 v2] (call (obj kw) obj v1 v2))
  ([obj kw v1 v2 v3] (call (obj kw) obj v1 v2 v3))
  ([obj kw v1 v2 v3 v4] (call (obj kw) obj v1 v2 v3 v4))
  ([obj kw v1 v2 v3 v4 & vs] (apply call (obj kw) obj v1 v2 v3 v4 vs)))

(defn T [& args] true)

(defn F [& args] false)

(defn vargs?
  (if (some #(= "getRequiredArity" (.getName %))
           (.getDeclaredMethods (class f))
    true
    false)))

(defn varg-count [f]
  (if (some #(= "getRequiredArity" (.getName %))
           (.getDeclaredMethods (class f)))
     (.getRequiredArity f)))

(defn arg-count [f]
  (let [ms (filter #(= "invoke" (.getName %))
                   (.getDeclaredMethods (class f)))
        ps (map (fn [m] (.getParameterTypes m)) ms)]
    (map alength ps)))


(comment
  (defn- op-max-args
   ([counts cargs] (op-max-args counts cargs nil))
   ([counts cargs res]
      (if-let [c (first counts)]
        (if (= c cargs) c
            (recur (next counts) cargs
                   (if (and res (> res c))
                     res c)))
        res)))

  (defn op [f & args]
   (let [vc    (varg-count f)
         cargs (count args)]
     (if (and vc (> cargs vc))
       (apply f args)
       (let [cs (arg-counts f)
             amax (op-max-args cs cargs)]
         (if amax
           (apply f (take amax args)))))))
)