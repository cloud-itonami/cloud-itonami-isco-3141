(ns field-lab-science-support.store
  "SSoT for the ISCO-08 3141 independent field-and-lab-science-support
  sole-proprietor actor. Store is a protocol injected into the
  `field-lab-science-support.actor` StateGraph — `MemStore` is the
  default, deterministic, zero-dep backend; a Datomic/kotoba-server-
  backed implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    project  — a registered research project (:project-id, :name)
    record   — a committed operating record under a project (sample
               collection, analysis, near-research-subjects operation,
               near-protected-site operation) — written ONLY via
               commit-record!, never mutated in place
    ledger   — an append-only audit trail of every proposal/verdict/
               disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (project [s project-id])
  (records-of [s project-id])
  (ledger [s])
  (register-project! [s project])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (project [_ project-id] (get-in @a [:projects project-id]))
  (records-of [_ project-id] (filter #(= project-id (:project-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-project! [s project]
    (swap! a assoc-in [:projects (:project-id project)] project) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:projects {} :records [] :ledger []} seed)))))
