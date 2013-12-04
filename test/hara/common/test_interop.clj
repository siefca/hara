(ns hara.common.test-interop
  (:require [hara.common.interop :as h]
            [midje.sweet :refer :all]))
            
(fact
  (h/into-object (java.util.Date.) {:time 0}) 
  => #inst "1970-01-01T00:00:00.000-00:00")