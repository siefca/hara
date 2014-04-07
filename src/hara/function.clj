(ns hara.function
  (:require [hara.import :as im]
            [hara.function.args]
            [hara.function.dispatch]))
            
(im/import 
  hara.function.args      :all
  hara.function.dispatch  :all)