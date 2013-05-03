;; ## Common Paradigms
;;
;; `hara.common` provides methods, macros and utility
;; functions that complements `clojure.core` and makes programming
;; in clojure more "clojurish". Each function is not that useful
;; on its own but together, they span a number of paradigms and
;; adds flexibility to program structure and control. The main
;; functionality are:
;;
;;  - Exceptions
;;  - Calling Conventions
;;  - Strings
;;  - Predicate Checking
;;  - Nested Structures
;;  - Watch Callback
;;
(ns hara.common
  (:require [clojure.string :as st]
            [clojure.set :as set])
  (:refer-clojure :exclude [send]))


;; ## Classes

(defn construct [class & args]
  (let [class (cond (instance? java.lang.Class class) class
                    (string? class) (-> class symbol resolve)
                    (instance? java.lang.Object class)
                    (clojure.core/class class))
        types (map type args)
        ctor  (.getConstructor class (into-array java.lang.Class types))]
    (.newInstance ctor (object-array args))))


;; ## Threads
;;
;; Simple functions for thread that increase readability.
;;

(defmacro time-ms
  "Evaluates expr and outputs the time it took.  Returns the time in ms

    (time-ms (inc 1)) ;=> 0.008

    (time-ms (Thread/sleep 100)) ;=> 100.619
  "
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (/ (double (- (. System (nanoTime)) start#)) 1000000.0)))

(defn current-thread
  "Returns the currenly executing thread."
  []
  (Thread/currentThread))

(defn sleep
  "Shortcut for Thread/sleep.

    (sleep 100) ;=> <sleeps for 100ms>.
  "
  [ms]
  (Thread/sleep ms))

(defn yield
  "Yields control of the currently executing thread."
  []
  (Thread/yield))

(defn interrupt
  "Interrupts a `thd` or the current thread
   if no arguments are given.
  "
  ([] (interrupt (current-thread)))
  ([thd] (.interrupt thd)))


;; ## Exceptions
;;
;; If we place too much importance on exceptions, exception handling code
;; starts littering through the control code. Most internal code
;; do not require definition of exception types as exceptions are
;; meant for the programmer to look at and handle.
;;
;; Therefore, the exception mechanism should get out of the way
;; of the code. The noisy `try .... catch...` control structure
;; can be replaced by a `suppress` statement so that errors can be
;; handled seperately within another function or ignored completely.
;;

(defn error
  "Throws an exception when called.

    (error \"This is an error\")
    ;=> (throws Exception)
  "
  ([e] (throw (Exception. (str e))))
  ([e & more]
     (throw (Exception. (apply str e more)))))

(defn error-message
  "Returns the the error message associated with `e`.

    (error-message (Exception. \"error\")) => \"error\"
  "
  [e]
  (.getMessage e))

(defn error-stacktrace
  "Returns the the error message associated with `e`.
  "
  [e]
  (.getStackTrace e))

(defmacro suppress
  "Suppresses any errors thrown.

    (suppress (error \"Error\")) ;=> nil

    (suppress (error \"Error\") :error) ;=> :error
  "
  ([body]
     `(try ~body (catch Throwable ~'t)))
  ([body catch-val]
     `(try ~body (catch Throwable ~'t
                   (cond (fn? ~catch-val)
                         (~catch-val ~'t)
                         :else ~catch-val)))))

;; ## String
;;
;; Functions that should be in `clojure.string` but are not.
;;

(defn replace-all
  "Returns a string with all instances of `old` in `s` replaced with
   the value of `new`.

    (replace-all \"hello there, hello again\"
                   \"hello\" \"bye\")
    ;=> \"bye there, bye again\"
  "
  [s old new]
  (.replaceAll s old new))

(defn starts-with?
  "Returns `true` if `s` begins with `pre`.

    (starts-with? \"prefix\" \"pre\") ;=> true

    (starts-with? \"prefix\" \"suf\") ;=> false
  "
  [s pre]
  (.startsWith s pre))

(defn ends-with?
  "Returns `true` if `s` begins with `pre`.

    (ends-with? \"suffix\" \"fix\") ;=> true
  "
  [s suf]
  (.endsWith s suf))


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

;; ## Type Predicates
;;
;; Adds additional type predicates that are not in clojure.core

(defn boolean?
  "Returns `true` if `x` is of type `java.lang.Boolean`.

    (boolean? false) ;=> true
  "
  [x] (instance? java.lang.Boolean x))

(defn hash-map?
  "Returns `true` if `x` implements `clojure.lang.IPersistentMap`.

    (hash-map? {}) ;=> true
  "
  [x] (instance? clojure.lang.IPersistentMap x))

(defn hash-set?
  "Returns `true` if `x` implements `clojure.lang.IPersistentHashSet`.

    (hash-set? #{}) ;=> true
  "
  [x] (instance? clojure.lang.PersistentHashSet x))

(defn long?
  "Returns `true` if `x` is of type `java.lang.Long`.

    (h/long? 1) ;=> true

    (h/long? 1N) ;=> false
  "
  [x] (instance? java.lang.Long x))

(defn bigint?
  "Returns `true` if `x` is of type `clojure.lang.BigInt`.

    (h/bigint? 1N) ;=> true
  "
  [x] (instance? clojure.lang.BigInt x))

(defn bigdec?
  "Returns `true` if `x` is of type `java.math.BigDecimal`.

     (h/bigdec? 1M) ;=> true
  "
  [x] (instance? java.math.BigDecimal x))

(defn instant?
  "Returns `true` if `x` is of type `java.util.Date`.

    (instant? (instant 0)) => true
  "
  [x] (instance? java.util.Date x))

(defn uuid?
  "Returns `true` if `x` is of type `java.util.UUID`.

    (uuid? (uuid)) ;=> true
  "
  [x] (instance? java.util.UUID x))

(defn uri?
  "Returns `true` if `x` is of type `java.net.URI`.

    (uri? (uri \"http://www.google.com\"))
    ;=> true
  "
  [x] (instance? java.net.URI x))

(defn bytes?
  "Returns `true` if `x` is a primitive `byte` array.

    (bytes? (byte-array 8)) ;=> true

  "
  [x] (= (Class/forName "[B")
         (.getClass x)))

(defn atom?
  "Returns `true` if `x` is of type `clojure.lang.Atom`.

    (atom? (atom 0)) ;=> true
  "
  [obj] (instance? clojure.lang.Atom obj))

(defn aref?
  "Returns `true` if `x` is of type `clojure.lang.Ref`.

    (aref? (ref 0)) ;=> true
  "
  [obj]  (instance? clojure.lang.Ref obj))

(defn agent?
  "Returns `true` if `x` is of type `clojure.lang.Agent`.

    (agent? (agent 0)) ;=> true
  "
  [obj] (instance? clojure.lang.Agent obj))

(defn iref?
  "Returns `true` if `x` is of type `clojure.lang.IRef`.

    (iref? (atom 0)) ;=> true
  "
  [obj]  (instance? clojure.lang.IRef obj))

(defn ideref?
  "Returns `true` if `x` is of type `java.util.UUID`.

    (ideref? (promise)) ;=> true
  "
  [obj]  (instance? clojure.lang.IDeref obj))

(defn promise?
  "Returns `true` is `x` is a promise

    (promise? (future (inc 1))) ;=> true
  "
  [obj]
  (let [s (str (type obj))]
    (or (starts-with? s "class clojure.core$promise$")
        (starts-with? s "class clojure.core$future_call$"))))

(defn type-checker
  "Returns the checking function associated with `k`

    (type-checker :string)
    ;=> #'clojure.core/string?

    (type-checker :bytes)
    ;=> #'adi.utils/bytes?
   "
  [k]
  (resolve (symbol (str (name k) "?"))))

;; ## Function Representation
;;
;; Usually in clojure programs, the most common control structure that
;; is used is the `->` and `->>` macros. This is because a function can
;; be view as a series of smaller functional transforms
;;
;; A very important part of this pipeling style of programming can be seen
;; in how predicates are tested. They tend to be quite short, as in:
;;
;;  - `(< x 3)`
;;  - `(< (:a obj) 3)`
;;  - `(-> obj t1 t2 (< 3))`
;;
;; In general, they are written as:
;;
;;  - `(-> x t1 t2 pred)`
;;
;; It is worth keeping the predicates as data structures because
;; as they act as more than just functions. They can be used
;; for conditions, selections and filters when in the right
;; context. Although the form can only represent pipelines, it is enough to
;; cover most predicates and blurs the line between program and data.
;;

(defmacro ??
  "Constructs a list out of a function. Used for predicates

    (?? + 1 2 3) ;=> '(+ 1 2 3)

    (?? < 1) ;=> '(< 1)

    (?? (get-in [:a :b]) = 1)
    ;=> '((get-in [:a :b]) = 1)
  "
  [& args]
  (apply list 'list (map #(list 'quote %) args)))

(defn call->
  "Indirect call, takes `obj` and a list containing either a function,
   a symbol representing the function or the symbol `?` and any additional
   arguments. Used for calling functions that have been stored as symbols.

     (call-> 1 '(+ 2 3 4)) ;=> 10

     (call-> 1 '(< 2)) ;=> true

     (call-> 1 '(? < 2)) ;=> true

     (call-> {:a {:b 1}} '((get-in [:a :b]) = 1))
     ;=> true
   "
  [obj [ff & args]]
  (cond (nil? ff)     obj
        (list? ff)    (recur (call-> obj ff) args)
        (vector? ff)  (recur (get-in obj ff) args)
        (keyword? ff) (recur (get obj ff) args)
        (fn? ff)      (apply ff obj args)
        (symbol? ff)  (if-let [f (suppress (resolve ff))]
                        (apply call f obj args)
                        (recur (get obj ff) args))
        :else         (recur (get obj ff) args)))

(defn get->
  "Provides a shorthand way of getting a return value.
   `sel` can be a function, a vector, or a value.

    (get-> {:a {:b {:c 1}}} :a) => {:b {:c 1}}

    (get-> {:a {:b {:c 1}}} [:a :b]) => {:c 1}
  "
  [obj sel]
  (cond (nil? sel)    obj
        (list? sel)   (call-> obj sel)
        (vector? sel) (get-in obj sel)
        (symbol? sel) (if-let [f (suppress (resolve sel))]
                        (call f obj)
                        (get obj sel))
        (ifn? sel)    (sel obj)
        :else         (get obj sel)))

(defn make-exp
  "Makes an expression using `sym`

    (make-exp 'y (?? str)) ;=> '(str y)

    (make-exp 'x (?? (inc) (- 2) (+ 2)))
    ;=> '(+ (- (inc x) 2) 2))
  "
  [sym [ff & more]]
  (cond (nil? ff)     sym
        (list? ff)    (recur (make-exp sym ff) more)
        (vector? ff)  (recur (list 'get-in sym ff) more)
        (keyword? ff) (recur (list 'get sym ff) more)
        (fn? ff)      (apply list ff sym more)
        (symbol? ff)  (apply list ff sym more)
        :else         (recur (list 'get sym ff) more)))

(defn make-fn-exp
  "Makes a function expression out of the form

    (make-fn-exp '(+ 2))
    ;=> '(fn [?%] (+ ?% 2))
  "
  [form]
  (apply list 'fn ['?%]
         (list (make-exp '?% form))))

(defn fn->
  "Constructs a function from a form representation.

    ((fn-> '(+ 10)) 10) ;=> 20
  "
  [form]
  (eval (make-fn-exp form)))

(defmacro ?%
  "Constructs a function of one argument, Used for predicate

    ((?% < 4) 3) ;=> true

    ((?% > 2) 3) ;=> true
  "
  [& args]
  (make-fn-exp args))

;; ## Predicate Checking


(defn check
  "Returns `true` when `v` equals `chk`, or if `chk` is a function, `(chk v)`

    (check 2 2) ;=> true

    (check 2 even?) ;=> true

    (check 2 '(< 1)) ;=> true

    (check {:a {:b 1}} (?? ([:a :b]) = 1)) ;=> true
  "
  [obj chk]
  (or (= obj chk)
      (-> (get-> obj chk) not not)))

(defn check->
  "Returns `true` if `(sel obj)` satisfies `check`

    (check-> {:a {:b 1}} :a hash-map?) ;=> true

    (check-> {:a {:b 1}} [:a :b] 1) ;=> true
  "
  [obj sel chk]
  (check (get-> obj sel) chk))

(defn check-all->
  "Returns `true` if `obj` satisfies all pairs of sel and chk

    (check-all-> {:a {:b 1}}
                 [:a {:b 1} :a hash-map?])
    => true
  "
  [obj scv]
  (every? (fn [[sel chk]]
            (check-> obj sel chk))
          (partition 2 scv)))

(defn eq->
  "A shortcut to compare if two vals are equal.

      (eq-> {:id 1 :a 1} {:id 1 :a 2} :id)
      ;=> true

      (eq-> {:db {:id 1} :a 1}
            {:db {:id 1} :a 2} [:db :id])
      ;=> true
  "
  [obj1 obj2 sel]
  (= (get-> obj1 sel) (get-> obj2 sel)))

(defn pcheck->
  "Shorthand ways of checking where `m` fits `prchk`

    (pcheck-> {:a 1} :a) ;=> truthy

    (pcheck-> {:a 1 :val 1} [:val 1]) ;=> true

    (pcheck-> {:a {:b 1}} [[:a :b] odd?]) ;=> true
  "
  [obj pchk]
  (cond (vector? pchk)
        (check-all-> obj pchk)

        (hash-set? pchk)
        (some (map #(pcheck-> obj %) pchk))

        :else
        (check obj pchk)))

(defn suppress-pcheck
  "Tests obj using prchk and returns `obj` or `res` if true

    (suppress-pcheck :3 even?) => nil

    (suppress-pcheck 3 even?) => nil

    (suppress-pcheck 2 even?) => true
  "
  ([obj prchk] (suppress-pcheck obj prchk true))
  ([obj prchk res]
     (suppress (if (pcheck-> obj prchk) res))))

;; ## Constructors

(defn queue
  "Returns a `clojure.lang.PersistentQueue` object.

    (def a (queue 1 2 3 4))
    (seq (pop a) ;=> [2 3 4]
  "
  ([] (clojure.lang.PersistentQueue/EMPTY))
  ([x] (conj (queue) x))
  ([x & xs] (apply conj (queue) x xs)))

(defn uuid
  "Returns a `java.util.UUID` object

    (uuid) ;=> <random uuid>

    (uuid \"00000000-0000-0000-0000-000000000000\")
    ;=> #uuid \"00000000-0000-0000-0000-000000000000\"
  "
  ([] (java.util.UUID/randomUUID))
  ([id]
     (cond (string? id)
           (java.util.UUID/fromString id)
           (bytes? id)
           (java.util.UUID/nameUUIDFromBytes id)
           :else (error id " can only be a string or byte array")))
  ([^java.lang.Long msb ^java.lang.Long lsb]
     (java.util.UUID. msb lsb)))

(defn instant
  "Returns a `java.util.Date` object

    (instant) ;=> <current time>

    (instant 0) ;=> 1970-01-01T00:00:00.000-00:00
  "
  ([] (java.util.Date.))
  ([val] (java.util.Date. val)))

(defn uri
  "Returns a `java.net.URI` object

    (uri \"http://www.google.com\")
    ;=> #<URI http://www.google.com>
  "
  [path] (java.net.URI/create path))

 ;; ## Useful Methods

(defn func-map
  "Returns a hash-map `m`, with the the values of `m` being
   the items within the collection and keys of `m` constructed
   by mapping `f` to `coll`. This is used to turn a collection
   into a lookup for better search performance.

    (funcmap :id [{:id 1 :val 1} {:id 2 :val 2}])
    ;=> {1 {:id 1 :val 1} 2 {:id 2 :val 2}}
   "
  [f coll] (zipmap (map f coll) coll))

(defn remove-repeats
  "Returns a vector of the items in `coll` for which `(f item)` is unique
   for sequential `item`'s in `coll`.

    (remove-repeats [1 1 2 2 3 3 4 5 6])
    ;=> [1 2 3 4 5 6]

    (remove-repeats even? [2 4 6 1 3 5])
    ;=> [2 1]
   "
  ([coll] (remove-repeats identity coll))
  ([f coll] (remove-repeats f coll [] nil))
  ([f coll output last]
     (if-let [v (first coll)]
       (cond (and last (= (f last) (f v)))
             (recur f (next coll) output last)
             :else (recur f (next coll) (conj output v) v))
       output)))

(defn replace-walk
  "Replaces all values in coll with the replacements defined as a lookup

    (replace-walk {:a 1 :b {:c 1}} {1 2})
    ;=> {:a 2 :b {:c 2}}
  "
  [coll rep]
  (cond (vector? coll)   (mapv #(replace-walk % rep) coll)
        (list? coll)     (apply list (map #(replace-walk % rep) coll))
        (hash-set? coll) (set (map #(replace-walk % rep) coll))
        (hash-map? coll) (zipmap (keys coll)
                               (map #(replace-walk % rep) (vals coll)))
        (rep coll) (rep coll)
        :else coll))

(defn group-bys
  "Returns a map of the elements of coll keyed by the result of
  f on each element. The value at each key will be a set of the
  corresponding elements, in the order they appeared in coll.

    (group-bys even? [1 2 3 4 5])
    ;=> {false #{1 3 5}, true #{2 4}}
  "
  [f coll]
  (persistent!
   (reduce
    (fn [ret x]
      (let [k (f x)]
        (assoc! ret k (conj (get ret k #{}) x))))
    (transient {}) coll)))

(defn dissoc-in
  "Dissociates a key in a nested associative structure `m`, where `[k & ks]` is a
  sequence of keys. When `keep` is true, nested empty levels will not be removed.

    (dissoc-in {:a {:b {:c 3}}} [:a :b :c])
    ;=> {}

    (dissoc-in {:a {:b {:c 3}}} [:a :b :c] true)
    ;=> {:a {:b {}}}
  "
  ([m [k & ks]]
     (if-not ks
       (dissoc m k)
       (let [nm (dissoc-in (m k) ks)]
         (cond (empty? nm) (dissoc m k)
               :else (assoc m k nm)))))

  ([m [k & ks] keep]
     (if-not ks
       (dissoc m k)
       (assoc m k (dissoc-in (m k) ks keep)))))

(defn assoc-nil
  ([m k v]
     (if (get m k) m (assoc m k v)))
  ([m k v & kvs]
     (apply assoc-nil (assoc-nil m k v) kvs)))

(defn assoc-nil-in
  [m ks v]
  (if (get-in m ks) m (assoc-in m ks v)))

(defn merge-nil
  ([m] m)
  ([m1 m2]
     (loop [m1 m1 m2 m2]
       (if-let [[k v] (first m2)]
         (if (get m1 k)
           (recur m1 (next m2))
           (recur (assoc m1 k v) (next m2)))
         m1)))
  ([m1 m2 & more]
     (apply merge-nil (merge-nil m1 m2) more)))

(defn merge-nil-nested
  ([m] m)
  ([m1 m2]
     (if-let [[k v] (first m2)]
       (let [v1 (get m1 k)]
         (cond (nil? v1)
               (recur (assoc m1 k v) (next m2))

               (and (hash-map? v) (hash-map? v1))
               (recur (assoc m1 k (merge-nil-nested v1 v)) (next m2))

               :else
               (recur m1 (next m2))))
       m1))
  ([m1 m2 & more]
     (apply merge-nil-nested (merge-nil-nested m1 m2) more)))

(defn keys-nested
  "Return the set of all nested keys in `m`.

    (keys-nested {:a {:b 1 :c {:d 1}}})
    ;=> #{:a :b :c :d})"
  ([m] (keys-nested m #{}))
  ([m ks]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (set/union
              (keys-nested (next m) (conj ks k))
              (keys-nested v))

             :else (recur (next m) (conj ks k)))
       ks)))

(defn dissoc-nested
   "Returns `m` without all nested keys in `ks`.

    (dissoc-nested {:a {:b 1 :c {:b 1}}} [:b])
    ;=> {:a {:c {}}}"

  ([m ks] (dissoc-nested m (set ks) {}))
  ([m ks output]
    (if-let [[k v] (first m)]
      (cond (ks k)
            (recur (next m) ks output)

            (hash-map? v)
            (recur (next m) ks
              (assoc output k (dissoc-nested v ks)))
            :else
            (recur (next m) ks (assoc output k v)))
      output)))

(defn diff-nested
  "Returns any nested values in `m1` that are different to those in `m2`.

    (diff-nested {:a {:b 1}}
                 {:a {:b 1 :c 1}})
    ;=> {}

    (diff-nested {:a {:b 1 :c 1}}
                 {:a {:b 1}})
    ;=> {:a {:c 1}}"
  ([m1 m2] (diff-nested m1 m2 {}))
  ([m1 m2 output]
     (if-let [[k v] (first m1)]
       (cond (nil? (k m2))
             (recur (dissoc m1 k) m2 (assoc output k v))

             (and (hash-map? v) (hash-map? (k m2)))
             (let [sub (diff-nested v (k m2))]
               (if (empty? sub)
                 (recur (dissoc m1 k) m2 output)
                 (recur (dissoc m1 k) m2 (assoc output k sub))))

             (not= v (k m2))
             (recur (dissoc m1 k) m2 (assoc output k v))

             :else
             (recur (dissoc m1 k) m2 output))
       output)))

(defn merge-nested
  "Merges all nested values in `m1`, `m2` and `ms` if there are more.
   nested values of maps on the right will replace those on the left if
   the keys are the same.

    (merge-nested {:a {:b {:c 3}}} {:a {:b 3}})
    ;=> {:a {:b 3}}

    (merge-nested {:a {:b {:c 1 :d 2}}}
                  {:a {:b {:c 3}}})
    ;=> {:a {:b {:c 3 :d 2}}})
  "
  ([m] m)
  ([m1 m2]
     (if-let [[k v] (first m2)]
       (cond (nil? (k m1))
             (recur (assoc m1 k v) (dissoc m2 k))

             (and (hash-map? v) (hash-map? (k m1)))
             (recur (assoc m1 k (merge-nested (k m1) v)) (dissoc m2 k))

             (not= v (k m1))
             (recur (assoc m1 k v) (dissoc m2 k))

             :else
             (recur m1 (dissoc m2 k)))
       m1))
  ([m1 m2 m3 & ms]
     (apply merge-nested (merge-nested m1 m2) m3 ms)))


(defn remove-nested
  "Returns a associative with nils and empty hash-maps removed.

    (remove-nested {:a {:b {:c {}}}})
    ;=> {}

    (remove-nested {:a {:b {:c {} :d 1}}})
    ;=> {:a {:b {:d 1}}}
  "
  ([m] (remove-nested m (constantly false) {}))
  ([m prchk] (remove-nested m prchk {}))
  ([m prchk output]
     (if-let [[k v] (first m)]
       (cond (or (nil? v) (suppress (pcheck-> m prchk)))
             (recur (dissoc m k) prchk output)

             (hash-map? v)
             (let [rmm (remove-nested v prchk)]
               (if (empty? rmm)
                 (recur (dissoc m k) prchk output)
                 (recur (dissoc m k) prchk (assoc output k rmm))))

             :else
             (recur (dissoc m k) prchk (assoc output k v)))
       output)))

(defn manipulate
  "Higher order function for manipulating entire data
   trees. Clones refs and atoms. It is useful for type
   conversion and serialization/deserialization.
  "
 ([f x] (manipulate f x {}))
 ([f x cs]
    (let [m-fn    #(manipulate f % cs)
          pred-fn (fn [pd]
                    (cond (instance? Class pd) #(instance? pd %)
                          (fn? pd) pd
                          :else (constantly false)))
          custom? #((pred-fn (:pred %)) x)
          c (first (filter custom? cs))]
      (cond (not (nil? c))
            (let [ctor (or (:ctor c) identity)
                  dtor (or (:dtor c) identity)]
              (ctor (manipulate f (dtor x) cs)))

            :else
            (cond
              (instance? clojure.lang.Atom x)   (atom (m-fn @x))
              (instance? clojure.lang.Ref x)    (ref (m-fn @x))
              (instance? clojure.lang.Agent x)  (agent (m-fn @x))
              (list? x)                         (apply list (map m-fn x))
              (vector? x)                       (vec (map m-fn x))
              (instance? clojure.lang.IPersistentSet x)
              (set (map m-fn x))

              (instance? clojure.lang.IPersistentMap x)
              (zipmap (keys x) (map m-fn (vals x)))

              (instance? clojure.lang.ISeq x)
              (map m-fn x)

              :else (f x))))))

(defn deref-nested
  "Dereferences all nested refs within data-structures

    (deref-nested
         (atom {:a (atom {:b (atom :c)})}))
    => {:a {:b :c}}
 "
 ([x] (deref-nested identity x))
 ([f x] (deref-nested f x []))
 ([f x cs]
    (manipulate f
             x
             (conj cs {:pred clojure.lang.IDeref
                       :ctor identity
                       :dtor deref}))))

;; ## IRef Functions

(defn hash-code
  "Returns the hash-code of the object

    (hash-code 1) => 1

    (hash-code :1) => 1013907437

    (hash-code \"1\") => 49
  "
  [obj]
  (.hashCode obj))

(defn hash-keyword
  "Returns a keyword repesentation of the hash-code.
   For use in generating internally unique keys

    (h/hash-keyword 1)
    ;=> :__1__
  "
  [obj & ids]
  (keyword (str "__" (st/join "_" (concat (map str ids) [(hash-code obj)])) "__")))

(defn hash-pair
  "Combines the hash of two objects together.

    (hash-pair 1 :1)
    ;=> :__1_1013907437__
  "
  [v1 v2]
  (hash-keyword v2 (hash-code v1)))

(defn set-value!
  "Change the value contained within a ref or atom.

    @(set-value! (atom 0) 1)
    ;=> 1

    @(set-value! (ref 0) 1)
    ;=> 1
  "
  [rf obj]
  (cond (atom? rf) (reset! rf obj)
        (aref? rf) (dosync (ref-set rf obj)))
  rf)

(defn alter!
  "Updates the value contained within a ref or atom using `f`.

    @(alter! (atom 0) inc)
    ;=> 1

    @(alter! (ref 0) inc)
    ;=> 1
  "
  [rf f & args]
  (cond (atom? rf) (apply swap! rf f args)
        (aref? rf) (dosync (apply alter rf f args)))
  rf)

(defn dispatch!
  "Updates the value contained within a ref or atom using another thread.

    (dispatch! (atom 0)
                (fn [x] (Thread/sleep 1000)
                        (inc x)))
    ;=> <future_call>
  "
  [ref f & args]
  (future
    (apply alter! ref f args)))

(declare add-change-watch
         make-change-watch)

(defn add-change-watch
  "Adds a watch function that only triggers when there is change
   in `(sel <value>)`.

    (def subject (atom {:a 1 :b 2}))
    (def observer (atom nil)
    (add-change-watch subject :clone
        :b (fn [& _] (reset! observer @a)))

    (swap! subject assoc :a 0)
    @observer => nil

    (swap! subject assoc :b 1)
    @observer => {:a 0 :b 1}
  "
  ([rf k f] (add-change-watch rf k identity f))
  ([rf k sel f]
     (add-watch rf k (make-change-watch sel f))))

(defn make-change-watch
  [sel f]
  (fn [k rf p n]
    (let [pv (get-> p sel)
          nv (get-> n sel)]
      (if-not (or (= pv nv) (nil? nv))
        (f k rf pv nv)))))

;; ## Latching

(defn latch-transform-fn
  [rf f]
  (fn [_ _ _ v]
    (set-value! rf (f v))))

(defn latch
  "Latches two irefs together so that when `master`
   changes, the `slave` will also be updated

    (def master (atom 1))
    (def slave (atom nil))

    (latch master slave #(* 10 %)
    (swap! master inc)
    @master ;=> 2
    @slave ;=> 20
  "
  ([master slave] (latch master slave identity))
  ([master slave f]
     (add-watch master
                (hash-pair master slave)
                (latch-transform-fn slave f))))

(defn latch-changes
  "Same as latch but only changes in `(sel <val>)` will be propagated
    (def master (atom {:a 1))
    (def slave (atom nil))

    (latch-changes master slave :a #(* 10 %)
    (swap! master update-in [:a] inc)
    @master ;=> {:a 2}
    @slave ;=> 20
  "
  ([master slave] (latch-changes master slave identity identity))
  ([master slave sel] (latch-changes master slave sel identity))
  ([master slave sel f]
     (add-change-watch master (hash-pair master slave)
                       sel (latch-transform-fn slave f))))

(defn delatch
  "Removes the latch so that updates will not be propagated"
  [master slave]
  (remove-watch master (hash-pair master slave)))

;; ## Concurrency Watch
(defn run-notify
  "Adds a notifier to a long running function so that it returns
   a promise that is accessible when the function has finished.
   updating the iref.

    (let [res (run-notify
             #(do (sleep 200)
                  (alter! % inc))
                  (atom 1)
                  notify-on-all)]
    res ;=> promise?
    @res ;=> atom?
    @@res ;=> 2)
  "
  [mtf ref notify-fn]
  (let [p (promise)
        pk (hash-keyword p)]
    (add-watch ref pk (notify-fn p pk))
    (mtf ref)
    p))

(defn notify-on-all
  "Returns a watch-callback function that waits
   for the ref to be updated then removes itself
   and delivers the promise"
  [p pk]
  (fn [_ ref _ _]
    (remove-watch ref pk)
    (deliver p ref)))

(defn notify-on-change
  "Returns a watch-callback function that waits
   for the ref to be updated, checks if the `(sel <value>)`
   has been updated then removes itself and delivers the promise"
  ([] (notify-on-change identity))
  ([sel]
     (fn [p pk]
       (fn [k ref old new]
         (when-not (eq-> old new sel)
           (remove-watch ref pk)
           (deliver p ref))))))

(defn wait-deref
  "A nicer interface for `deref`"
  ([p] (wait-deref p nil nil))
  ([p ms] (wait-deref p ms nil))
  ([p ms ret]
     (cond (nil? ms) (deref p)
           :else (deref p ms ret))))

(defn wait-for
  "Waits for a long running multithreaded function to update the ref.
   Used for testing purposes

    (def atm (atom 1))
    ;; concurrent call
    (def f #(dispatch! % slow-inc))
    (def ret (wait-for f atm))

    @atm ;=> 2
    @ret ;=> 2
  "
  ([mtf ref] (wait-for mtf ref notify-on-all nil nil))
  ([mtf ref notifier ms] (wait-for mtf ref notifier ms nil))
  ([mtf ref notifier ms ret]
     (wait-deref (run-notify mtf ref notifier) ms ret)))

(defn wait-on
  "A redundant function. Used for testing purposes. The same as
   `(alter! ref f & args)` but the function is wired with the
   notification scheme.

    (def atm (wait-on slow-inc (atom 1)))
    (@atm => 2)
  "
  [f ref & args]
  (wait-for #(apply dispatch! % f args) ref))
