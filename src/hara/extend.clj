(ns hara.extend
  (:require [hara.namespace.import :as im]
            [hara.extend.all]
            [hara.extend.abstract]))
            
(im/import 
  hara.extend.all        [extend-all]
  hara.class.abstract    [extend-abstract extend-implementations])