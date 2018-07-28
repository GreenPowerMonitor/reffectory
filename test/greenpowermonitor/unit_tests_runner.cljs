(ns greenpowermonitor.unit-tests-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [greenpowermonitor.reffectory-test]))

(doo-tests
 'greenpowermonitor.reffectory-test)