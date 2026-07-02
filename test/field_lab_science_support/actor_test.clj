(ns field-lab-science-support.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [field-lab-science-support.actor :as actor]
            [field-lab-science-support.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-project! st {:project-id "project-1" :name "Watershed Survey"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:project-id "project-1" :op :sample :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "project-1"))))))

(deftest holds-on-unregistered-project-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:project-id "no-such-project" :op :sample :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-project")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; near-research-subjects operation always escalates (governor invariant)
        request {:project-id "project-1" :op :operate-near-research-subjects :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "project-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "project-1")))))))
