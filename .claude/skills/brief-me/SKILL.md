---
name: brief-me
description: "Your to-dos: the owner's action list. Use when writing the final report or end-of-session summary of an implementation, wayfinder or retro session."
---

Write a section headed "Your to-dos" (in the language the owner wrote in) at the end of the final report, before the `/retro` request.

1. Re-check the live state before writing: default branch, open PRs, rulesets, secrets. Do not copy the previous report's list.
2. Collect every action only the owner can take: GitHub settings, branch renames, decisions, secrets, merges of red PRs, approvals.
3. Write each as a numbered item with three parts: what to do; why it is needed (what stays blocked until it is done); where (settings page, PR or issue link).
4. Order the items by what unblocks the most first.
5. Keep an item in every later report until its live-state check shows it done.
6. If nothing is left for the owner, write the single line "No to-dos for you."
7. Close the report after the list by asking the user to run `/retro`.

Done when every open owner action appears once, with what, why and where, and the list matches the live state.
