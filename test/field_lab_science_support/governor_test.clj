(ns field-lab-science-support.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [field-lab-science-support.store :as store]
            [field-lab-science-support.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-project! st {:project-id "project-1" :name "Watershed Survey"})
    st))

(deftest ok-on-clean-sample
  (let [st (fresh-store)
        proposal {:op :sample :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:project-id "project-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-project
  (let [st (fresh-store)
        proposal {:op :sample :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:project-id "no-such-project"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-project (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :sample :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:project-id "project-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-near-research-subjects-operation
  (let [st (fresh-store)
        proposal {:op :operate-near-research-subjects :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:project-id "project-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-near-protected-site-operation
  (let [st (fresh-store)
        proposal {:op :operate-near-protected-site :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:project-id "project-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :sample :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:project-id "project-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:project-id "project-1" :op :analyze})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "project-1"))))
    (is (= 1 (count (store/ledger st))))))
