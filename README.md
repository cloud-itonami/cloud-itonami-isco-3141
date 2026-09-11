# cloud-itonami-isco-3141

Open Occupation Blueprint for **ISCO-08 3141**: Life Science Technicians (excluding Medical).

This repository designs a forkable OSS business for an independent life-science field/lab technician: a field-sampling robot performs collection and basic on-site analysis under a governor-gated actor, so the practice keeps its own chain-of-custody and data records instead of renting a closed LIMS SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a field-sampling robot performs soil/water/crop sample collection and basic on-site analysis under an actor that proposes
actions and an independent **Field Lab Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near research subjects, protected sites or water sources) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
research protocol + sampling plan + chain-of-custody requirement
        |
        v
Sampling Advisor -> Field Lab Governor -> sample/analyze, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3141`). Required capabilities:

- :robotics
- :telemetry
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section, alongside `cloud-itonami-isco-6130`, `-8160`, `-2166`, `-2641`,
`-2651`, `-2652`, `-2654`, `-1219`, `-1223`, `-1330`, `-1341`, `-1349`,
`-1412`, `-1439`, `-2144`, `-2320`, `-2411`, `-2422`, `-2431`, `-2621`,
`-2634`, `-3122` and `-3123`): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/field_lab_science_support/store.kotoba` — `Store` protocol +
  `MemStore`: registered projects, committed records, an append-only
  audit ledger.
- `src/field_lab_science_support/advisor.kotoba` — `Advisor` protocol;
  `mock-advisor` (deterministic, default) proposes a sampling
  operation from a request; `llm-advisor` wraps a
  `langchain.model/ChatModel` — either way the advisor only ever
  produces a `:propose`-effect proposal, never a committed record, and
  LLM parse failures always yield `confidence 0.0` (forces escalation,
  never fabricated confidence).
- `src/field_lab_science_support/governor.kotoba` —
  `FieldLabGovernor/check`: a pure function, wired as its own
  `:govern` node. Hard invariants (unregistered project, a proposal
  whose `:effect` isn't `:propose`) always route to `:hold`. Escalation
  invariants (`:operate-near-research-subjects`,
  `:operate-near-protected-site`, or low advisor confidence) always
  route to `:request-approval` — an `interrupt-before` node that the
  graph checkpoints and only resumes on explicit human approval
  (`actor/approve!`), matching the README's robotics-premise statement
  that operating near research subjects, protected sites or water
  sources always require human sign-off.
- `src/field_lab_science_support/actor.kotoba` — `build-graph`,
  `run-request!`, `approve!`: the `langgraph.graph/state-graph` wiring
  itself.

```bash
kbb -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
