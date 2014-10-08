(ns hara.concurrent.workflow.register
  (:require [hara.protocol.state :refer [IStateGet IStateSet]]
            [hara.protocol.watch :refer [IWatch]]
            [hara.common.state :as state]
            [hara.common.watch :as watch]))

(defn new-register-state []
  (do {:tasks {}
       :running {}
       :active #{}
       :downstream {}
       :upstream {}}))

(deftype Register [state]
  IStateGet
  (-get-state [obj _])

  IStateSet
  (-update-state [obj _ f args])
  (-set-state [obj _ v])
  (-empty-state [obj _]))

(defn register []
  (Register. (atom (new-register-state))))
