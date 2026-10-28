---
name: limit-loop
description: "Schedule a recurring check that resumes work cut off by a usage limit."
disable-model-invocation: true
argument-hint: "An interval such as 5h (default), or nothing"
---

A **limit-loop** is a recurring tick that finds work stopped by a usage limit (HTTP 429, "hit your 5-hour limit") and resumes it, so a long session survives the reset without you.

1. **Interval**: the argument, else `5h`. Round it to one that divides its unit evenly (`*/5` hours leaves uneven gaps), upwards, so one tick is at least as long as the limit window (`5h` becomes `6h`). Tell the user the interval you chose.
2. **Schedule**: call the Skill tool with `loop`, args `<interval> <tick prompt>`. `loop` creates the recurring job and runs the first tick at once.
3. **Tick prompt**, written for the session's current task so it stands alone:
   - Resume work that a usage limit cut off: check `ListAgents`, the branches and worktrees of the task.
   - An agent that ended with a rate-limit error resumes through `SendMessage`, with the original task and its remaining steps.
   - When nothing was cut off, the tick does nothing.
4. **Done** when the job exists, the first tick ran, and the user has the job ID, the cadence, the 7-day expiry and `CronDelete` as the way to stop it.

## Resuming an agent

- A failed agent's saved context makes `SendMessage` cheaper than a fresh agent.
- An agent that died before its first commit has lost its worktree: tell it to start a fresh worktree on its ticket branch from `origin/main`.
