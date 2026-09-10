(ns aged-care.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [aged-care.actor :as actor]
            [aged-care.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-facility! st {:facility-id "fac-1" :name "Sundale Manor"})
    (store/register-resident! st {:resident-id "res-1" :name "Alice Smith" :facility-id "fac-1"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:facility-id "fac-1" :op :schedule-staffing :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "fac-1"))))))

(deftest holds-on-unregistered-facility-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:facility-id "no-such-facility" :op :schedule-staffing :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-facility")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; incident flagging always escalates (governor invariant)
        request {:facility-id "fac-1" :op :flag-incident :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "fac-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "fac-1")))))))
