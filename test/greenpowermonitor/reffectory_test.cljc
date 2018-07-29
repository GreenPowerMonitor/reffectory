(ns greenpowermonitor.reffectory-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures #?(:cljs async :clj assert-any)]]
   #?(:clj [clojure.core.async :refer [<!! >!! timeout]])
   [greenpowermonitor.common-fixtures :refer [reset-handlers!]]
   [greenpowermonitor.reffectory :as sut]))

(use-fixtures :each reset-handlers!)

(defn- make-cofxs-checker [expected-payload expected-cofxs]
  (fn [cofx payload]
    (is (= payload expected-payload))
    (doseq [[kw expected-val] expected-cofxs]
      (is (= expected-val (kw cofx))))
    {}))

(deftest calling-an-event-handler
  (let [passed-payload :some-payload
        calls-counter (atom 0)]

    (sut/register-event-handler!
     ::an-event-handler-is-called
     (fn [cofx payload]
       (swap! calls-counter inc)
       (is (= [passed-payload] payload))
       (is (= (contains? cofx :db)))
       {}))

    (sut/dispatch! [::an-event-handler-is-called passed-payload])

    (is (= 1 @calls-counter))))

(deftest checking-cofxs-are-injected-when-registering-an-event
  (testing "cofxs are injected into the event handler along with the db coeffect"
    (let [expected-date-time :any-date
          passed-payload :some-payload]

      (sut/register-cofx!
       :date-time
       (fn [cofx]
         (assoc cofx :date-time expected-date-time)))

      (sut/register-event-handler!
       ::cofxs-are-injected
       [(sut/inject-cofx :date-time)]
       (make-cofxs-checker [passed-payload] {:date-time expected-date-time}))

      (sut/dispatch! [::cofxs-are-injected passed-payload]))))

(deftest submit-effect-handler
  (let [expected-payload [:arg1 :arg2]
        calls-count (atom 0)]

    (sut/register-event-handler!
     ::event-submitted-using-submit-effect
     (fn [_ payload]
       (swap! calls-count inc)
       (is (= expected-payload payload))
       {}))

    (sut/register-event-handler!
     ::event-producing-submit-fx
     (fn [_ _]
       {:dispatch [::event-submitted-using-submit-effect :arg1 :arg2]}))

    (sut/dispatch! [::event-producing-submit-fx])

    (is (= 1 @calls-count))))

(deftest submit-n-effect-handler
  (let [expected-payload-for-first-event [:args1-1 :args1-2]
        expected-payload-for-second-event [:args2-1 :args2-2]
        first-event-calls-count (atom 0)
        second-event-calls-count (atom 0)]

    (sut/register-event-handler!
     ::first-event-submitted-using-submit-n
     (fn [_ payload]
       (swap! first-event-calls-count inc)
       (is (= expected-payload-for-first-event payload))
       {}))

    (sut/register-event-handler!
     ::second-event-submitted-using-submit-n
     (fn [_ payload]
       (swap! second-event-calls-count inc)
       (is (= expected-payload-for-second-event payload))
       {}))

    (sut/register-event-handler!
     ::event-producing-submit-n-fx
     (fn [_ _]
       {:dispatch-n [[::first-event-submitted-using-submit-n :args1-1 :args1-2]
                     [::second-event-submitted-using-submit-n :args2-1 :args2-2]]}))

    (sut/dispatch! [::event-producing-submit-n-fx])

    (is (= 1 @first-event-calls-count))
    (is (= 1 @second-event-calls-count))))

#?(:clj (defn- now []
          (.getTime (java.util.Date.))))

(deftest submit-later-effect-handler
  #?(:cljs (async done
                  (let [delay-in-ms 10]

                    (sut/register-event-handler!
                     ::event-submitted-using-submit-later
                     (fn [_ [payload]]
                       (is (>= (- (js/Date.now) payload) delay-in-ms))
                       (done)
                       {}))

                    (sut/register-event-handler!
                     ::event-producing-submit-later-fx
                     (fn [_ _]
                       {:dispatch-later
                        {:ms    delay-in-ms
                         :event [::event-submitted-using-submit-later (js/Date.now)]}}))

                    (sut/dispatch! [::event-producing-submit-later-fx])))

     :clj  (let [delay-in-ms 10
                 channel (timeout (+ delay-in-ms 100))]

             (sut/register-event-handler!
              ::event-submitted-using-submit-later
              (fn [_ [channel]]
                (>!! channel (now))
                {}))

             (let [initial-time (now)]
               (sut/register-event-handler!
                ::event-producing-submit-later-fx
                (fn [_ _]
                  {:dispatch-later
                   {:ms    delay-in-ms
                    :event [::event-submitted-using-submit-later channel]}}))

               (sut/dispatch! [::event-producing-submit-later-fx])

               (let [time-when-event-submitted-using-submit-later-got-called (<!! channel)]
                 (if (some? time-when-event-submitted-using-submit-later-got-called)
                   (is (>= (- time-when-event-submitted-using-submit-later-got-called initial-time)
                           delay-in-ms))
                   (is false "event-submitted-using-submit-later handler was not called")))))))

(deftest delegating-events
  (let [payload-1 :payload-1
        payload-2 :payload-2
        calls-payloads (atom [])]

    (sut/register-event-handler!
     ::event-delegated-to
     (fn [_ payload]
       (swap! calls-payloads conj payload)
       {}))

    (sut/register-events-delegation!
     [:event-delegating-to-another-1
      :event-delegating-to-another-2]
     ::event-delegated-to)

    (sut/dispatch! [:event-delegating-to-another-1 payload-1])
    (sut/dispatch! [:event-delegating-to-another-2 payload-2])

    (is (= [[payload-1] [payload-2]] @calls-payloads))))
