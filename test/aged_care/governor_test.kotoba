(ns aged-care.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [aged-care.store :as store]
            [aged-care.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-facility! st {:facility-id "fac-1" :name "Sundale Manor"})
    (store/register-resident! st {:resident-id "res-1" :name "Alice Smith" :facility-id "fac-1"})
    st))

(deftest ok-on-clean-schedule-staffing
  (let [st (fresh-store)
        proposal {:op :schedule-staffing :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:facility-id "fac-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-facility
  (let [st (fresh-store)
        proposal {:op :schedule-staffing :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:facility-id "no-such-facility"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-facility (:rule %)) (:violations v)))))

(deftest hard-on-unregistered-resident
  (let [st (fresh-store)
        proposal {:op :log-resident-record :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:facility-id "fac-1" :resident-id "no-such-resident"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-resident (:rule %)) (:violations v)))))

(deftest hard-on-resident-mismatch
  (let [st (fresh-store)
        ;; register a resident for a different facility
        _ (store/register-resident! st {:resident-id "res-2" :name "Bob Jones" :facility-id "fac-2"})
        proposal {:op :log-resident-record :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:facility-id "fac-1" :resident-id "res-2"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :resident-mismatch (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :schedule-staffing :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:facility-id "fac-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-clinical-decision-boundary
  (let [st (fresh-store)
        proposal {:op :log-resident-record :effect :propose :confidence 0.9 :stake :low
                  :content "prescribe pain medication for resident Alice"}
        v (governor/check {:facility-id "fac-1" :resident-id "res-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :clinical-decision-boundary (:rule %)) (:violations v)))))

(deftest escalates-on-flag-incident
  (let [st (fresh-store)
        proposal {:op :flag-incident :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:facility-id "fac-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :draft-family-correspondence :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:facility-id "fac-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:facility-id "fac-1" :resident-id "res-1" :op :log-resident-record})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "fac-1"))))
    (is (= 1 (count (store/ledger st))))))
