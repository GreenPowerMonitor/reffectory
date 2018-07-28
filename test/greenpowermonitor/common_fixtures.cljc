(ns greenpowermonitor.common-fixtures
  (:require
   [greenpowermonitor.reffectory :as reffectory]))

#?(:clj  (defn reset-handlers! [f]
           (binding [reffectory/*verbose* false]
             (let [initial-value @reffectory/handlers]
               (f)
               (reset! reffectory/handlers initial-value))))

   :cljs (def reset-handlers!
           (binding [reffectory/*verbose* false]
             (let [initial-value @reffectory/handlers]
               {:after #(reset! reffectory/handlers initial-value)}))))
