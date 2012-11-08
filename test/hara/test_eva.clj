(ns hara.test-eva
  (:refer-clojure :exclude [swap! reset!])
  (:use midje.sweet
        [hara.fn :only [deref*]]
        [hara.data.evom :only [evom swap! reset! add-watches remove-watches]])
  (:require [hara.eva :as v]))
