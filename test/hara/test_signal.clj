(ns hara.test-signal
  (:use midje.sweet
        hara.common
        hara.signal)
  (:require [hara.signal :as s]))

(fact "parse-contents"
 (parse-contents {:error true}) => {:error true}
 (parse-contents :error) => {:error true}
 (parse-contents [:error]) => {:error true}
 (parse-contents [:error :error1]) => {:error true :error1 true})

(fact "raise-loop - unmanaged"
  (s/raise-loop {:contents {:error true}} [] {})
  => (throws clojure.lang.ExceptionInfo " :unmanaged - {:error true}")

  (s/raise-loop {:id :issue :contents {:error true} :default [:one]}
                [] {})
  => (throws Exception "UNWRAP_ISSUE: the label :one has not been implemented")

  (s/raise-loop {:id :issue :contents {:error true} :default [:one]}
                [] {:one :other})
  => (throws clojure.lang.ExceptionInfo " :default - {:error true}")

  (s/raise-loop {:id :issue :contents {:error true}
                 :options {:one (fn [] 1)} :default [:one]}
                [] {:one :issue})
  => 1

  (s/raise-loop {:id :issue :contents {:error true}
                 :options {:custom (fn [n] n)} :default [:custom 100]}
                [] {:custom :issue})
  => 100

  (s/raise-loop {:id :issue :contents {:error true}
                 :options {:custom (fn [n] n)} :default [:custom]}
                [] {:custom :issue})
  => (throws Exception "UNWRAP_ISSUE: Wrong number of arguments to option key :custom"))

(fact "raise-loop - continue"
  (s/raise-loop {:id :issue
                 :contents {:error true :data 1}}
                [{:id :h1
                  :handlers [{:checker :error
                              :type :continue
                              :fn (fn [{:keys [data]}]
                                    data)}]}]
                {})
  => 1)

(fact "raise-loop - escalate"
  (s/raise-loop {:id :issue
                 :contents {:error true :data 1}}
                [{:id :h1
                  :handlers [{:checker :error
                              :type :escalate
                              :contents-fn (fn [{:keys [data]}]
                                             {:data (* 2 data)})
                              :options-fn (fn [{:keys []}]{})
                              :default-fn (fn [{:keys []}] nil)}]}]
                {})
  => (throws clojure.lang.ExceptionInfo " :unmanaged - {:data 2, :error true}"))

(fact "raise-loop - catch"
  (s/raise-loop {:id :issue
                 :contents {:error true :data 1}}
                [{:id :h1
                  :handlers [{:checker :error
                              :type :catch
                              :fn (fn [{:keys [data]}]
                                    {:data data})}]}]
                {})
  => (throws clojure.lang.ExceptionInfo " :catch - {:data 1, :error true}"))


(fact "raise-loop - choose"
  (s/raise-loop {:id :issue
                 :contents {:error true :data 1}
                 :options {:add (fn [x y z] (+ x y z))}}
                [{:id :h1
                  :handlers [{:checker :error
                              :type :choose
                              :label :add
                              :args-fn (fn [{:keys [data]}]
                                         (list data data data))}]}]
                {:add :issue})
  => 3

  (s/raise-loop {:id :issue
                 :contents {:error true :data 1}}
                [{:id :h1
                  :handlers [{:checker :error
                              :type :choose
                              :label :add
                              :args-fn (fn [{:keys [data]}]
                                         (list data data data))}]
                  :options {:add (fn [x y z] (+ x y z))}}]
                {:add :h1})
  => (throws clojure.lang.ExceptionInfo " :choose - {:data 1, :error true}"))


(fact "raise"
  (let [data 1]
    (raise :error
           (option :one [] data)
           (option :two [] 2)
           (option :custom [n] (+ n data))
           (default :custom data)))
  => 2)


(fact "manage")


(defmacro hello
  ([] (hello 1))
  ([n]
      (let [i `(list 1 2 3 ~n)]
        i)))

#_(hello)

(try
  (raise :error)
  (catch Throwable t
    nil))

(defn value-func []
  (raise [:error {:d 10000}]
         (option :one [] 1)
         (option :custom [n] n)))


(defn med-func []
  (manage
   (value-func)
   (on :error []
       (escalate :big-error))))

(manage
 (med-func)
 (on :big-error [d]
     (choose :custom d)))

(fact
  (manage
   (manage
    (raise :error)

    (on :error []
        (escalate
         {:data }
         (default :one))))

   (on :error [data]
       (continue data))
   (option :one [] 1)) => :data)


(defn int-check [i]
  (manage
   (cond (integer? i)
         (cond (zero? (mod i 2)) i

               :else
               (raise [:is-odd {:i i}]
                      (option :approx [] (dec i))
                      (default :approx)))

         (string? i)
         (raise [:is-string {:i i}]
                (option :scream []
                        (error "ARRGH!!"))
                (default :scream)))
   (option :zero [] 0)
   (option :custom [n] n)))

(manage
 (int-check 9)
 (on :is-odd []
     (continue 1000)))


(defn int-halve [i]
  (manage
   (let [i (int-check i)]
     (quot i 2))
   (option :nan [] :nan)))

(int-halve 2)
(int-halve 3)

(defn array-halve [arr]
  (manage
   (mapv int-halve arr)
   (option :empty-array [] [])))

(manage
 (array-halve [0 1 2 3 4 5])
 (on :is-odd []
     ;;(continue 1000)
     (choose :nan)
     ))

(manage
 (array-halve [0 1 2 3 4 5])
 (on :is-odd []
     ;;(continue 1000)
     (choose :empty-array)
     ))
(defn array-halve-nan [arr]
  (manage
   (mapv int-halve arr)
   (on :is-odd []
       (choose :approx))
   (on :is-string []
       (choose :nan))))

(array-halve [0 1 2 3 4 5])

(fact
  (array-halve-nan [0 1 2 3 4 "5"]))
