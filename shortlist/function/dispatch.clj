(ns hara.function.dispatch)

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