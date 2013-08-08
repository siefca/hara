(ns hara.test-conditional
  (:use midje.sweet
        hara.common
        hara.conditional)
  (:require [hara.conditional :as s]))

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
  => (throws Exception "RAISE_UNHANDLED: the label :one has not been implemented")

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
  => (throws Exception "RAISE_UNHANDLED: Wrong number of arguments to option key :custom"))

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
