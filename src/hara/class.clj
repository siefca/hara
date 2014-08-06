(ns hara.class
  (:require [hara.namespace.import :as ns]
            [hara.class.checks]
            [hara.class.inheritance]))
            
(ns/import 
  hara.class.checks      :all
  hara.class.inheritance :all)