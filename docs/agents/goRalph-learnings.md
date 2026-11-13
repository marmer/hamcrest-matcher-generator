# goRalph learnings

Every goRalph iteration reads this file before it starts working (`iteration-rules.md` points here). It holds lessons for the loop only; lessons for every implementation belong in `implementer-brief.md`.

Limit: 40 lines (`LearningsFileTest` in `scripts/agents/test_go_ralph_loop.py` pins it). When the file is full, the retro condenses it instead of appending: general lessons move to `implementer-brief.md`, obsolete ones are deleted, and if it still does not fit, a retro ticket proposes raising the limit or restructuring. Only the retro iteration edits this file, through its own reviewed PR.

## Lessons

- You start with an empty context: the issue tracker, Git, the ticket's handover note `notes/ticket-<n>.md` and the run directory are the only state. Read the note first, and update it after every work step.
- A ticket is done when its PR is squash-merged, not when the implementer reports. Open the PR with squash auto-merge, wait for CI once, then end; the script's work check picks up what is left.
- Never ask the owner interactively. Comment on the ticket with the question, the options and your recommendation, set `needs-info`, and go on with other tickets.
- Record every escalation as a ticket comment (from which model to which, and why); the retro reads these comments.
- This sandbox refuses shell commands that mix the literal text "github" with pipes, loops, substitutions or heredocs: put such a command in a script file in the scratchpad and run the file.
- Tests of scripts: build the fake PATH from the needed tools only, wait with poll-and-deadline helpers (never fixed sleeps), and give every failure line an assertion message, never a bare timeout.
- Never `git checkout` an uncommitted file, and never force-push or push to `main`: the permission settings deny both.
