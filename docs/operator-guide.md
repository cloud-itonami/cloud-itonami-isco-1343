# Operator Guide for cloud-itonami-isco-1343

## Starting the Actor

```clojure
(require '[aged-care.actor :as actor]
         '[aged-care.store :as store])

;; Create a fresh store
(def st (store/mem-store))

;; Register a facility
(store/register-facility! st {:facility-id "fac-1" :name "Sundale Manor"})

;; Register residents
(store/register-resident! st {:resident-id "res-1" :name "Alice Smith" :facility-id "fac-1"})

;; Build the actor graph
(def graph (actor/build-graph {:store st}))
```

## Running a Request

```clojure
;; Submit an administrative request
(def request {:facility-id "fac-1" :op :schedule-staffing :stake :low})
(def result (actor/run-request! graph request {} "thread-1"))

;; :status can be :done (automatically approved) or :interrupted (human review needed)
(:status result)  ; => :done or :interrupted
```

## Handling Escalations

Some operations (like `:flag-incident`) always require human review and result
in `:interrupted` status:

```clojure
;; This always escalates to human review
(def incident-request {:facility-id "fac-1" :op :flag-incident :stake :high})
(def incident-result (actor/run-request! graph incident-request {} "thread-2"))

;; :status => :interrupted

;; After human review and sign-off:
(def approved-result (actor/approve! graph "thread-2"))
;; :status => :done (approval is the act of resuming)
```

## Checking the Audit Ledger

```clojure
;; All proposals, verdicts, and dispositions are logged
(store/ledger st)

;; All records committed for a facility
(store/records-of st "fac-1")
```

## Supported Operations

- `:schedule-staffing` — Staff roster scheduling
- `:log-resident-record` — Administrative resident record updates
- `:draft-family-correspondence` — Family communications
- `:flag-incident` — Escalate incident for human review (always requires approval)

## Scope Boundaries

This actor does NOT:
- Make clinical decisions
- Order medications
- Create care plans
- Recommend treatments
- Make any medical determination

Clinical functions remain with licensed care staff. The governor has a HARD block
on any proposal containing clinical decision-making language (`:clinical-decision-boundary`
violation). Attempts to use this actor for clinical decision-making will fail with `:hold`.

## Testing

Run the full test suite:

```bash
kbb -M:test
```

## Checkpointing and Resume

The actor uses langgraph checkpointing to save state at each graph node. This enables:
- Resuming after human approval without re-running the entire flow
- Thread-scoped checkpoints so multiple flows can run in parallel
- Deterministic replay of any request

Thread IDs must be unique within a session. Use a UUID or task identifier.
