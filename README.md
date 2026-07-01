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

## License

AGPL-3.0-or-later.
