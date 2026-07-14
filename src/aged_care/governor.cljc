(ns aged-care.governor
  "AgedCareGovernor — the independent safety/traceability layer for the
  ISCO-08 1343 aged care services manager actor. Wired as its own `:govern`
  node in `aged-care.actor`'s StateGraph, downstream of `:advise` — the
  Advisor has no notion of facility/resident provenance or clinical-decision
  risk, so this MUST be a separate system able to reject a proposal (itonami
  actor pattern, per ADR-2607011000 / CLAUDE.md Actors section).

  CRITICAL SCOPE BOUNDARY: This actor supports ADMINISTRATIVE management only
  — staff scheduling, resident record administration, family correspondence,
  incident logging. It NEVER itself makes a clinical care decision, medication
  order, or treatment determination. Those remain exclusively with licensed
  care staff. A proposal containing ANY clinical decision-making is a HARD
  block (permanent :hold).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. facility provenance   — the request's facility must be registered.
    2. resident provenance   — if resident-id is present, resident must be
                               registered and match the facility.
    3. no-actuation          — proposal :effect must be :propose.
    4. SCOPE BOUNDARY        — proposal must NOT contain a clinical decision,
                               medication order, or treatment determination.
                               These are hard blocks; this actor is administrative
                               support only.

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README administrative-boundary premise: :flag-incident and low confidence
  always require human review):
    5. :op :flag-incident — incident flagging always requires human validation.
    6. low confidence (< `confidence-floor`)."
  (:require [aged-care.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:flag-incident})
(def ^:private clinical-keywords
  #{:medication :treatment :clinical-decision :diagnosis :prescription
    :care-plan :pain-management :palliative})

(defn- has-clinical-decision?
  "Check if a proposal contains any clinical decision-making keywords or
  references. Returns true if clinical content is detected."
  [proposal]
  (let [proposal-str (str proposal)
        lower-str (clojure.string/lower-case proposal-str)]
    (boolean (some #(clojure.string/includes? lower-str (name %))
                   clinical-keywords))))

(defn- hard-violations [{:keys [proposal request]} facility-record resident-record]
  (cond-> []
    (nil? facility-record)
    (conj {:rule :no-facility :detail "未登録 facility"})

    (and (:resident-id request)
         (nil? resident-record))
    (conj {:rule :no-resident :detail "未登録 resident"})

    (and (:resident-id request)
         resident-record
         (not= (:facility-id request) (:facility-id resident-record)))
    (conj {:rule :resident-mismatch :detail "resident is not in this facility"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

    (has-clinical-decision? proposal)
    (conj {:rule :clinical-decision-boundary :detail
           "This actor does NOT make clinical decisions. Clinical decisions, medications,
            treatment determinations must remain with licensed care staff."})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `aged-care.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [facility-record (store/facility store (:facility-id request))
        resident-record (when (:resident-id request)
                          (store/resident store (:resident-id request)))
        hard (hard-violations {:proposal proposal :request request}
                              facility-record
                              resident-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
