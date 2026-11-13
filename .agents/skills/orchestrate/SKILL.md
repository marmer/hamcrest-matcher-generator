---
name: orchestrate
description: Coordinate for the rest of the session: delegate the work to subagents, verify and integrate their results.
disable-model-invocation: true
---

For the rest of this session you are the **coordinator**: subagents do the work. Decompose, delegate, verify and integrate are your steps; talking to the user, decisions and findings stay with you. When another skill's process is active (e.g. `/implement-spec`), that process wins where it conflicts with this one.

## Decompose

Split the task into subtasks, each with its own checkable **done criterion**. Independent subtasks with disjoint areas (files, modules) run in parallel; overlapping ones run in sequence.

Name what stays in the main session: questions and ongoing alignment with the user, one-line fixes (1-2 lines; see Verify), and pure commands that check (`git status`, the project's lint or build command). Every part of the task ends up as a subtask or as main-session work.

## Delegate

Subagents start with fresh context: they know neither the session nor its decisions. Write one self-contained **handoff** per subtask:

- goal and scope
- the project rules that apply
- exact paths
- the done criterion
- the exclusion zone (what they must leave alone)

Read-only exploration goes to `Explore`; changes and multi-step work go to `general-purpose`. Launch independent subtasks in one message, so they run in parallel.

### Models

Start every subagent on **Haiku with effort `high`** (`model: "haiku"`, `effort: "high"`). Where the harness is Copilot, use its equivalents: **GPT Luna** in place of Haiku, **GPT Sol** in place of Sonnet.

## Verify

A result counts only after you verified it: read the diff or the changed files, check scope, rules and style, and run the matching check yourself.

A result that misses the bar goes back as a targeted follow-up instruction to the same subagent (`SendMessage`), naming what is wrong. One-line fixes (1-2 lines) you make yourself. Every fix, yours included, is checked by a fresh agent, and a verification ends with a check, never with a fix.

### Escalation

Count **attempts** per subtask as "Attempts" in `docs/agents/implementation.md` says: only after a report that missed the quality bar; review rounds and CI fixes do not count.

1. Haiku gets **3 attempts**.
2. After the third miss, discard the work and restart the subtask on **Sonnet** with a fresh handoff that adds what Haiku got wrong. Sonnet gets **2 attempts**.
3. After the fifth miss, stop and put **options with your recommendation** to the user: e.g. split the subtask finer, change the approach, give a stronger model or take it over yourself, drop it. Act only on the answer.

Escalate strictly by count: a fundamentally wrong direction uses up attempts like any other miss. Jump to a stronger model only when the user instructs it.

## Integrate and report

Integrate the verified results. Report the state between delegations, and bring findings and decisions to the user condensed.

## Done

The work is done when every subtask is verified and integrated, everything kept in the main session is finished, and the user knows the results, the checks you ran and any deviations.
