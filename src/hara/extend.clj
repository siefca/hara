(ns hara.extend
  (:require [hara.namespace.import :as ns]
            [hara.extend.all]
            [hara.extend.abstract]))
            
(ns/import 
  hara.extend.all        [extend-all]
  hara.extend.abstract   [extend-abstract extend-implementations])