(ns hara.class
  (:require [hara.namespace.import :as im]
            [hara.class.checks]
            [hara.class.inheritance]
            [hara.class.multi]))
            
(im/import 
  hara.class.checks      :all
  hara.class.inheritance :all
  hara.class.multi       :all)