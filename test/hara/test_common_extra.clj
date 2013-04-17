(ns hara.test-common-extra
  (:use midje.sweet)
  (:require [hara.common :as h]))
#_(facts "watch-for-change produces another function that can be used in watching
        the nested values of maps contained in atoms and iotams.

          @usage
          I have an atom containing a large array of nested keys:

             (def a (atom {:k0
                           {:k1
                            ....
                            {:kn value}}})

          And I only wish to run <fn> when the value changes, then this function can be used
          to generate a function for watch.

              (add-watch a (watch-for-change [:k0 :k1 ... :kn] <fn>))
       "
  (let [itm      (atom {:a {:b {:c {:d nil}}}})
        out1     (atom nil)
        to-out1-fn (fn [k r p v] (reset! out1 v))]
    (add-watch itm  :test (f/watch-for-change [:a :b :c :d] to-out1-fn))
    (fact "assert that adding watch does not change any of the
           values of the atoms: the nested value within itm and out1 are nil"
      @itm => {:a {:b {:c {:d nil}}}}
      @out1 => nil)

    (reset! itm {:a {:b {:c {:d 1}}}})
    (fact "assert that itm is updated and out1 has also been manipulated through the watch"
      @itm => {:a {:b {:c {:d 1}}}}
      @out1 => 1)

    (swap! itm update-in [:a :b :c :d] inc)
    (fact "assert that itm is updated  and out has been manipulated"
      @itm => {:a {:b {:c {:d 2}}}}
      @out1 => 2)))
	  )