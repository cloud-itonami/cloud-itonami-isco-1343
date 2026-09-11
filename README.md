# cloud-itonami-isco-1343

Open Occupation Blueprint for **ISCO-08 1343**: Aged Care Services Managers.

This repository designs a forkable OSS business for an aged care facility administrator: a document-handling and scheduling robot performs administrative tasks (staff scheduling, resident record administration, family correspondence, incident logging) under a governor-gated actor, so the facility keeps its own administrative and compliance records instead of renting a closed aged-care SaaS.

## CRITICAL SCOPE BOUNDARY: Administrative Support Only

**This actor supports ADMINISTRATIVE management of aged-care facilities ONLY.** It does NOT and WILL NOT:
- Make clinical care decisions
- Order medications or treatments
- Create or modify care plans
- Make any determination about medical diagnosis or treatment
- Recommend clinical interventions

**These remain EXCLUSIVELY with licensed care staff.** A hard, permanent block in the AgedCareGovernor rejects any proposal containing clinical decision-making language.

Administrative operations supported:
- Staff scheduling and roster management
- Resident enrollment and non-clinical record administration
- Family correspondence and communication
- Incident logging and flagging for human review

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document-handling and scheduling robot performs
staff scheduling, resident enrollment, family outreach, and incident logging under
an actor that proposes actions and an independent **Aged Care Governor** that gates them.
The governor never dispatches hardware itself; incident flagging always requires human review.

A live sample of the operator console (robotics safety console, shared template) is rendered
in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML
output of `kotoba.robotics.ui`.

## Core Contract

```text
facility enrollment + resident registry + staffing schedules
        |
        v
Aged Care Advisor -> Aged Care Governor -> administrative actions
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence. All clinical determinations remain with licensed care staff.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `1343`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section, alongside `cloud-itonami-isco-2411`, `-6130`, `-8160`, `-2166`, `-2641`,
`-2651`, `-2652`, `-2654`, `-1219`, `-1223`, `-1330`, `-1341`, `-1349`,
`-1412`, `-1439`, `-2144` and `-2320`): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/aged_care/store.kotoba` — `Store` protocol + `MemStore`:
  registered facilities, registered residents, committed administrative records,
  an append-only audit ledger. Note: clinical data (care plans, medications,
  diagnoses) is NOT stored here — that remains in clinical systems.
- `src/aged_care/advisor.kotoba` — `Advisor` protocol; `mock-advisor`
  (deterministic, default) proposes an administrative operation from a
  request; `llm-advisor` wraps a `langchain.model/ChatModel` — either
  way the advisor only ever produces a `:propose`-effect proposal,
  never a committed record, and LLM parse failures always yield
  `confidence 0.0` (forces escalation, never fabricated confidence).
- `src/aged_care/governor.kotoba` — `AgedCareGovernor/check`: a pure
  function, wired as its own `:govern` node. Hard invariants
  (unregistered facility, unregistered resident, a proposal whose `:effect` isn't `:propose`,
  or any proposal containing clinical decision-making) always route to `:hold`.
  Escalation invariants (`:flag-incident`, or low advisor confidence) always route to
  `:request-approval` — an `interrupt-before` node that the graph
  checkpoints and only resumes on explicit human approval
  (`actor/approve!`), matching the README's administrative-boundary premise
  that incident flagging always requires human validation.
- `src/aged_care/actor.kotoba` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
kbb -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
