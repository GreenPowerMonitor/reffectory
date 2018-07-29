(ns greenpowermonitor.common-fixtures
  (:require
   [greenpowermonitor.reffectory :as reffectory]))

(defn- after-test-fn [initial-handlers-value]
  (reset! reffectory/verbose true)
  (reset! reffectory/handlers initial-handlers-value))

#?(:clj  (defn reset-handlers! [f]
           (let [initial-value @reffectory/handlers]
             (reset! reffectory/verbose false)
             (f)
             (after-test-fn initial-value)))

   :cljs (def reset-handlers!
           (let [initial-value @reffectory/handlers]
             {:before #(reset! reffectory/verbose false)
              :after  #(after-test-fn initial-value)})))
