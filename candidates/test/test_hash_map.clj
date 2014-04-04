(ns hara.test-hash-map
  (:require [hara.hash-map :as h]
            [midje.sweet :refer :all]))
            
(fact "dissoc-in"
  (h/dissoc-in {:a 2 :b 2} [:a]) => {:b 2}
  (h/dissoc-in {:a 2 :b 2} [:a] true) => {:b 2}

  (h/dissoc-in {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
  (h/dissoc-in {:a {:b 2 :c 3}} [:a :b] true) => {:a {:c 3}}

  (h/dissoc-in {:a {:c 3}} [:a :c]) => {}
  (h/dissoc-in {:a {:c 3}} [:a :c] true) => {:a {}}

  (h/dissoc-in {:a {:b {:c 3}}} [:a :b :c]) => {}
  (h/dissoc-in {:a {:b {:c 3}}} [:a :b :c] true) => {:a {:b {}}})

(fact "assoc-nil"
  (h/assoc-nil {} :a 1) => {:a 1}
  (h/assoc-nil {:a 1} :a 2) => {:a 1}
  (h/assoc-nil {:a 1} :a 2 :b 2) => {:a 1 :b 2})

(fact "assoc-nil-in"
  (h/assoc-nil-in {} [:a] 1) => {:a 1}
  (h/assoc-nil-in {} [:a :b] 1) => {:a {:b 1}}
  (h/assoc-nil-in {:a {:b 1}} [:a :b] 2) => {:a {:b 1}})

(fact "merge-nil"
  (h/merge-nil {} {:a 1}) => {:a 1}
  (h/merge-nil {:a 1} {:a 3}) => {:a 1}
  (h/merge-nil {:a 1 :b 2} {:a 3 :c 3}) => {:a 1 :b 2 :c 3})

(fact "merge-nil-nested"
  (h/merge-nil-nested {} {:a {:b 1}}) => {:a {:b 1}}
  (h/merge-nil-nested {:a {}} {:a {:b 1}}) => {:a {:b 1}}
  (h/merge-nil-nested {:a {:b 1}} {:a {:b 2}}) => {:a {:b 1}}
  (h/merge-nil-nested {:a 1 :b {:c 2}} {:a 3 :e 4 :b {:c 3 :d 3}})
  => {:a 1 :b {:c 2 :d 3} :e 4})

(fact "keys-nested will output all keys in a map"
  (h/keys-nested {:a {:b 1 :c 2}})
  => #{:a :b :c}
  (h/keys-nested {:a {:b 1 :c {:d 2 :e {:f 3}}}})
  => #{:a :b :c :d :e :f})

(fact "dissoc-nested will dissoc all nested keys in a map"
  (h/dissoc-nested {:a 1} [:a])
  => {}

  (h/dissoc-nested {:a 1 :b 1} [:a :b])
  => {}

  (h/dissoc-nested {:a {:b 1 :c {:b 1}}} [:b])
  => {:a {:c {}}}

  (h/dissoc-nested {:a {:b 1 :c {:b 1}}} [:a :b])
  => {})


(fact "diff-nested will take two maps and compare what in the first is different to that in the second"
  (h/diff-nested {} {}) => {}
  (h/diff-nested {:a 1} {}) => {:a 1}
  (h/diff-nested {:a {:b 1}} {})=> {:a {:b 1}}
  (h/diff-nested {:a {:b 1}} {:a {:b 1}}) => {}
  (h/diff-nested {:a {:b 1}} {:a {:b 1 :c 1}}) => {}
  (h/diff-nested {:a {:b 1 :c 1}} {:a {:b 1}}) => {:a {:c 1}}
  (h/diff-nested {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c {:d {:e 1}}}})
  => {}
  (h/diff-nested {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c 1}})
  => {:b {:c {:d {:e 1}}}})


(fact "merge-nested will take two maps and merge them recursively"
  (h/merge-nested {} {}) => {}
  (h/merge-nested {:a 1} {}) => {:a 1}
  (h/merge-nested {} {:a 1}) => {:a 1}
  (h/merge-nested {:a {:b 1}} {:a {:c 2}}) => {:a {:b 1 :c 2}}
  (h/merge-nested {:a {:b {:c 1}}} {:a {:b {:c 2}}}) => {:a {:b {:c 2}}}
  (h/merge-nested {:a {:b 3}} {:a {:b {:c 3}}}) => {:a {:b {:c 3}}}
  (h/merge-nested {:a {:b {:c 3}}} {:a {:b 3}}) => {:a {:b 3}}
  (h/merge-nested {:a {:b {:c 1 :d 2}}} {:a {:b {:c 3}}}) => {:a {:b {:c 3 :d 2}}})


(fact "remove-nested"
  (h/remove-nested {}) => {}
  (h/remove-nested {:a {}}) => {}
  (h/remove-nested {:a {} :b 1}) => {:b 1}
  (h/remove-nested {:a {:b {:c 1}}}) => {:a {:b {:c 1}}}
  (h/remove-nested {:a {:b {:c {}}}}) => {}
  (h/remove-nested {:a {:b {:c {} :d 1}}}) => {:a {:b {:d 1}}})
