(ns aged-care.store
  "SSoT for the ISCO-08 1343 aged care services manager actor. Store is
  a protocol injected into the `aged-care.actor` StateGraph — `MemStore`
  is the default, deterministic, zero-dep backend; a Datomic/kotoba-server-
  backed implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md Actors section).

  Domain:

    facility   — a registered aged care facility (:facility-id, :name)
    resident   — a registered resident of a facility (:resident-id, :name,
                 :facility-id) — never contains clinical decisions or
                 treatment plans (those remain with licensed care staff)
    record     — a committed administrative record under a facility
                 (staff schedule, resident enrollment, family correspondence,
                 incident log entry) — written ONLY via commit-record!,
                 never mutated in place
    ledger     — an append-only audit trail of every proposal/verdict/
                 disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (facility [s facility-id])
  (resident [s resident-id])
  (records-of [s facility-id])
  (ledger [s])
  (register-facility! [s facility])
  (register-resident! [s resident])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (facility [_ facility-id] (get-in @a [:facilities facility-id]))
  (resident [_ resident-id] (get-in @a [:residents resident-id]))
  (records-of [_ facility-id] (filter #(= facility-id (:facility-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-facility! [s facility]
    (swap! a assoc-in [:facilities (:facility-id facility)] facility) s)
  (register-resident! [s resident]
    (swap! a assoc-in [:residents (:resident-id resident)] resident) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:facilities {} :residents {} :records [] :ledger []} seed)))))
