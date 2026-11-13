# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual label strings used in this repo's issue tracker.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs-triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

A ticket whose first step needs data an agent cannot read (e.g. the Dependabot alert list, HTTP 403 in cloud sessions) carries `needs-info`, not `ready-for-agent`, until the owner supplies the data.

A ticket labelled `needs-info` or `ready-for-human` is not left for the owner to pick up alone: the agent walks the owner through it, does every sub-step it can do itself (gathering data, preparing options with a recommendation, running commands, writing the result back to the ticket) and hands the owner only what needs the owner's decision or action.

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label string from this table.

Edit the "Label in our tracker" column to match whatever vocabulary you actually use.
