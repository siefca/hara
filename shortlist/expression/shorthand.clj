(ns hara.expression.shorthand
  (:require [hara.common.checks :refer [hash-set? hash-map?]]
            [hara.common.error :refer [error suppress]]
            [hara.namespace.resolve :refer [resolve-ns]]
            [hara.function.dispatch :refer [call]]))

(defn shorthand-form
  "Makes an expression using `sym`

    (make-exp 'y '(str)) ;=> '(str y)

    (make-exp 'x '((inc) (- 2) (+ 2)))
    ;=> '(+ (- (inc x) 2) 2))
  "
  [sym [ff & more]]
  (cond (nil? ff)     sym
        (list? ff)    (recur (shorthand-form sym ff) more)
        (vector? ff)  (recur (list 'get-in sym ff) more)
        (keyword? ff) (recur (list 'get sym ff) more)
        (symbol? ff)  (apply list ff sym more)
        :else         (recur (list 'get sym ff) more)))

(defn shorthand-fn-expr
  "Makes a function expression out of the form

    (make-fn-exp '(+ 2))
    ;=> '(fn [?%] (+ ?% 2))
  "
  [form]
  (apply list 'fn ['?%]
         (list (shorthand-form '?% form))))

(defn fn->
  "Constructs a function from a form representation.

    ((fn-> '(+ 10)) 10) ;=> 20
  "
  [form]
  (eval (shorthand-fn-expr form)))

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
        (symbol? ff)  (if-let [f (do
                                   (resolve-ns ff)
                                   (suppress (resolve ff)))]
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
        (symbol? sel) (if-let [f (do
                                   (resolve-ns sel)
                                   (suppress (resolve sel)))]
                        (call f obj)
                        (get obj sel))
        (ifn? sel)    (sel obj)
        :else         (get obj sel)))

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


(defn check
  "Returns `true` if `obj` or `(sel obj)` satisfies `chk`

    (check 2 2) => true

    (check 2 even?) => true

    (check 2 '(< 1)) => true

    (check {:a {:b 1}} '(? [:a :b] (= 1))) => true

    (check {:a {:b 1}} :a hash-map?) => true

    (check {:a {:b 1}} [:a :b] 1) => true
  "
  ([obj chk]
   (or (= obj chk)
       (-> (get-> obj chk) not not)))
  ([obj sel chk]
    (check (get-> obj sel) chk)))

(defn check-all
  "Returns `true` if `obj` satisfies all pairs of sel and chk

    (check-all {:a {:b 1}}
      :a       hash-map?
      [:a :b]  1)
    => true
  "
  [obj & pairs]
  (every? (fn [[sel chk]]
            (check obj sel chk))
          (partition 2 pairs)))

(defn check->
  "Shorthand ways of checking where `m` fits `prchk`

    (check-> {:a 1} :a) ;=> truthy

    (check-> {:a 1 :val 1} [:val 1]) ;=> true

    (check-> {:a {:b 1}} [[:a :b] odd?]) ;=> true
  "
  [obj pchk]
  (cond (vector? pchk)
        (apply check-all obj pchk)

        (hash-set? pchk)
        (or (some true? (map #(check-> obj %) pchk))
            false)

        :else
        (check obj pchk)))

(defn check?->
  "Tests obj using prchk and returns `obj` or `res` if true

    (check?-> :3 even?) => nil

    (check?-> 3 even?) => nil

    (check?-> 2 even?) => true
  "
  ([obj prchk] (check?-> obj prchk true))
  ([obj prchk res]
     (suppress (if (check-> obj prchk) res))))
