# Review gate for outside contributors only (ticket #283, spec #104)

Research done for ticket #283 (part of spec #104). Researched 2026-10-31 by a subagent against the GitHub Docs pages on rulesets, bypass actors, auto-merge, code owners and required reviews (docs.github.com), the OpenSSF Scorecard docs and source pinned to v5.6.0, commit `61e84743356275f6ffca23b1b2f2cb945e19eb96` (the version the public Scorecard API reports for this repository), and read-only REST reads of this repository dated 2026-10-31. Nothing was changed in the repository settings and no experiment was run; points a session could not verify are marked (A) for assumption or "not verifiable from a session", each with the experiment the owner can run.

**Context:** single-maintainer, user-owned repository; PRs are merged by auto-merge under the ruleset `Development (Trunk)`; the owner's sessions author PRs under his identity, Renovate and the release-please App author the rest. The owner's wish of 2026-10-31: accept the Scorecard finding for his own PRs, but require a review from outside contributors.

Question: can the `main` ruleset require a human review for pull requests from other people while the owner's own PRs and those of the GitHub Apps (Renovate, the release-please App) keep merging through auto-merge?

## Verdict in five lines

1. A ruleset cannot scope a rule by PR author. Bypass is granted per actor (role, team, App), not per author. This is the answer to "can a required review be exempted per author at all": not natively.
2. Whether auto-merge honours a bypass actor is not documented and not verifiable from a session. It decides whether any "require approvals plus bypass" setup keeps the owner's auto-merge working. Experiment E1 below settles it.
3. Scorecard v5.6.0 treats any bypass actor as "not enforced for admins", which only affects the last and smallest scoring tier (1 point). It does not block the points for required approvals.
4. `Code-Review` stays about 0/10 under every option: it counts PRs the owner authored and merged himself, and those stay unreviewed unless someone else approves or merges them.
5. Recommendation: change nothing yet; the owner runs E1; apply configuration A's settings only if E1 passes, otherwise accept the finding (see "Recommendation").

## Facts, with sources

Scorecard is pinned to the version and commit the public API reports for this repository (`https://api.securityscorecards.dev/projects/github.com/marmer/hamcrest-matcher-generator`, fetched 2026-10-31): `v5.6.0`, commit `61e84743356275f6ffca23b1b2f2cb945e19eb96`. The API score on that day was 7.7, not 7.4 as in the ticket. Raw URLs below are `https://raw.githubusercontent.com/ossf/scorecard/61e84743356275f6ffca23b1b2f2cb945e19eb96/<path>`.

### Repository state (read through the REST API)

- One ruleset, `Development (Trunk)`, id 25820378, target branch, enforcement active, condition `ref_name` include `~DEFAULT_BRANCH` and nothing else. Rules: `deletion`, `non_fast_forward`, `required_status_checks` (ten checks, `strict_required_status_checks_policy: false`), `pull_request` (`required_approving_review_count: 0`, `require_code_owner_review: false`, `require_last_push_approval: false`, `required_review_thread_resolution: true`, squash only), `required_linear_history`. Bypass actors: `{actor_id: 5, actor_type: RepositoryRole, bypass_mode: pull_request}` (role 5 is admin). `GET repos/marmer/hamcrest-matcher-generator/rulesets/25820378`, 2026-10-31.
- Two fields not discussed elsewhere: `required_status_checks.do_not_enforce_on_create: true` (checks are not enforced when a branch is created together with its PR; irrelevant to approvals and to this question) and `pull_request.require_extra_approval_for_unattributed_changes: true` (the "additional approval for unattributed Copilot pull requests" option; the docs say it "has no effect if the ruleset requires zero approvals", so today it does nothing, and with 1 required approval it would only matter for PRs Copilot opens under its own App identity, which this repository does not use). Neither changes a conclusion; configuration A leaves both as they are.
- The repository is user-owned (`owner.type: User`), `allow_auto_merge: true`. There is no `CODEOWNERS` file (`ls .github`).
- Last 30 closed PRs (`GET pulls?state=closed`, 2026-10-31): 21 authored by `marmer`, 9 by `renovate[bot]`.
- Observation, sample of 9 merged PRs read individually (`GET pulls/<n>`): the owner's PRs 289, 288, 276, 274 have author, `merged_by` and `auto_merge.enabled_by` all `marmer`; Renovate's PRs 267, 256, 254, 246, 245 have all three equal to `renovate[bot]`. 9 of 9 match. Inference (not shown for other PRs): with auto-merge the "merger" Scorecard sees is whoever enabled auto-merge.
- The release-please PRs are created with a GitHub App token (`.github/workflows/release.yml` lines 39-49, `RELEASE_APP_ID` variable); that id is not readable from a session, the owner supplies it.
- Renovate's App id 2740 is read from the avatar URL `https://avatars.githubusercontent.com/in/2740` on the `user` object of PR 267 (not from an `auto_merge.enabled_by` payload). That a ruleset bypass entry of type `Integration` with this id is accepted is unverified (A). Renovate itself enables auto-merge (`renovate.json`: `platformAutomerge: true`).
- Collaborators: `GET repos/marmer/hamcrest-matcher-generator/collaborators` was refused by the session proxy (HTTP 403, 2026-10-31), so who holds write access is not verified from the API. The statements below that "only the owner has write" are an assumption (A) from the ticket's single-owner premise (the ticket speaks of "the owner's own PRs" and "other people"); the owner can check Settings > Collaborators.

### GitHub

- Bypass list contents: "Repository admins, organization owners, and enterprise owners; the maintain or write role ...; Teams ...; GitHub Apps; Dependabot." Bypass mode "For pull requests only": "The selected actor is now required to open a pull request ... The actor can then choose to bypass any branch protections and merge that pull request." (docs.github.com `.../managing-rulesets/creating-rulesets-for-a-repository`, "Granting bypass permissions for your branch or tag ruleset"). So a bypass is a choice the actor makes at merge time.
- The "Required reviewers" rule (team-based, path-scoped approvals) "is not available on user-owned repositories" (`.../managing-rulesets/available-rules-for-rulesets`, "Required reviewers"). Teams do not exist on this repository, so a team-based bypass or reviewer list is out. This is also the only path-scoped reviewer mechanism, and it is path-scoped, not author-scoped.
- Review options of "Require a pull request before merging": number of approvals (0 to 10), dismiss stale approvals, "Require review from Code Owners" (any one code owner of the changed files suffices), approval of the most recent reviewable push, conversation resolution, allowed merge methods (`.../available-rules-for-rulesets`). None filters by PR author. One more option: "Require an additional approval for unattributed Copilot pull requests", which "has no effect if the ruleset requires zero approvals".
- "Pull request authors cannot approve their own pull requests." (`.../reviewing-changes-in-pull-requests/approving-a-pull-request-with-required-reviews`). With the owner as the only person with write access (A, see Collaborators above), any approval requirement makes the owner's own PRs unmergeable except by bypass.
- Code owners must "have write permissions for the repository" (`.../customizing-your-repository/about-code-owners`). `CODEOWNERS` maps paths to owners, not authors, so it cannot exempt the owner's PRs either.
- Auto-merge: "Auto-merge merges a pull request automatically after all required reviews and status checks pass." People with write permission can enable it; "Auto-merge is disabled if someone without write permissions pushes new changes to the head branch" (`.../incorporating-changes-from-a-pull-request/automatically-merging-a-pull-request`). The page says nothing about bypass actors.
- Rulesets layer: all applicable rulesets are enforced together (`.../managing-rulesets/about-rulesets`, "About rulesets and protected branches"). The About-rulesets page states that all applicable rules are enforced; it does not state in these words that a second ruleset cannot relax the first, so "a second ruleset can only add rules" is an inference (A) from "all applicable rules are enforced". The condition schema I saw (`conditions.ref_name` only, REST read above) has no author condition to attach to either; I did not read the REST schema reference for other condition types, so that part is also (A).

### Scorecard

`docs/checks.md#code-review` and `checks/raw/code_review.go`, `probes/codeApproved/impl.go`, `checks/evaluation/code_review.go`:

- The check looks at the last ~30 default-branch commits, groups them into changesets (one per merged PR) and treats a changeset as approved if some review has state `APPROVED` and `review.Author.Login != PR author`. `getGithubReviews` adds the merger as an `APPROVED` review, so a PR merged by someone other than its author counts as approved (the documented "implicit review"). A PR the owner authored and merged himself, directly or through auto-merge he enabled, has `merged_by == author` and counts as unreviewed. This is the cause of 0/29.
- Bot-authored changesets that are approved are skipped; unapproved ones count. Renovate PRs merged by Renovate itself are unapproved and count.
- Scoring: the docs describe a leveled deduction (3 for unreviewed bot changes, 7 or 10 for human ones). The code at this commit calls `CreateProportionalScoreResult(approved, total)`, which matches the API text "0/29 approved changesets -- score normalized to 0". Docs and code disagree at v5.6.0; the code is what ran.
- Reviews by bots do not count (docs). Outside-contributor PRs approved by the owner count as approved, but there are few of them.

`docs/checks.md#branch-protection`, `checks/evaluation/branch_protection.go`, `clients/githubrepo/branches.go`:

- Repository rules are read through GraphQL and translated to branch-protection fields. `applyRepoRules`: `translated.EnforceAdmins = asPtr(len(r.BypassActors.Nodes) == 0)`. The docs say the same: "calculated as `false` if any Bypass Actors are defined on any rule, regardless of if they are admins". The bypass mode is not looked at. Scorecard issue 3480 (linked in a code comment) tracks the over-simplification.
- Tiers (`computeFinalScore`): tier 1, delete and force-push blocked, 3 points; tier 2, 3 points shared by the approver probe (weight 2, true only when the required count is above 0) and the admin-review probes (PRs required, last-push approval, up-to-date branches); tier 3, required status checks, 2 points; tier 4, 1 point for at least two required reviewers and code-owner review (shared); tier 5, 1 point for admins not bypassing and stale-review dismissal. A tier is only reached when the previous one is full.
- Computed for today's ruleset: tier 1 full (3), tier 2 has 1 of 5 (PR required) so 3 + 3 x 1/5 = 3.6, truncated to 3. This matches the reported 3 and so confirms the reading of the code.

## Questions from the ticket

| Question | Answer |
|---|---|
| Does auto-merge honour a bypass actor? | Not documented (the docs say "after all required reviews and status checks pass"). Not verifiable from a session. Experiment E1. |
| Can a required review be exempted per author? | No. Rulesets have no author condition, the rule layering only adds (A, inference from the docs), CODEOWNERS is path based, required reviewers are path based and unavailable here. Only per-actor bypass exists, which exempts the merger, not the author. A custom required status check can implement an author rule (configuration C). |
| How does Scorecard score a ruleset with bypass actors? | Branch-Protection: any bypass actor makes "applies to administrators" false, which can only cost the tier-5 point. Code-Review: bypass actors are invisible to it; it counts approvals and merger identity. |

## Configurations compared

Assumptions are marked (A). "Owner PR" is a PR the owner or one of his sessions authored under his identity.

| | A. Approvals 1 plus bypass list | B. Code owner review plus bypass list | C. Review-gate status check, approvals stay 0 |
|---|---|---|---|
| Change | `required_approving_review_count: 1`; bypass list gets the Renovate App and the release App next to the admin role | `.github/CODEOWNERS` with `* @marmer`; `require_code_owner_review: true`; same bypass list as A | New workflow job `Review gate` as a required status check: passes at once when the PR author is the owner or one of the two Apps, otherwise fails until a write-role user has approved |
| External PR | Cannot merge without one approval from a write-role user; only the owner has write (A), so the owner approves or bypasses | Same, with the approval coming from the code owner | Check stays red until approved |
| Owner PR | Needs bypass: the author cannot approve his own PR | Same; whether the author-owner counts as the required code owner is undocumented (A: no) | Unaffected |
| Apps PR | Needs the Apps in the bypass list | Same | Unaffected |
| Auto-merge | Works only if auto-merge honours bypass (E1, E2). If not, every owner and Renovate PR stalls and needs a manual "Merge without waiting" click: the workflow in `docs/agents/wayfinding.md` breaks | Same risk as A | Unaffected (A: the check also has to run on the PR head sha, see E3) |
| Gate is real | Only for people without bypass; the owner can bypass on outside PRs too | Same | Yes, the owner cannot skip it either, unless he is the author |
| Scorecard Branch-Protection (A, from the scoring code above) | 3 -> 4 (tier 2 at 3 of 5). Full tier 2 (+ last-push approval and strict up-to-date) would give 3 + 3 + 2 = 8, but strict up-to-date means every auto-merge PR must be rebased first, and last-push approval cannot be met by the solo owner | 3 -> 4 if approvals are 1 as well; code-owner review only scores in tier 4, which is not reached while tier 2 is incomplete; with approvals 0 it adds nothing | 3 (no change) |
| Scorecard Code-Review (A) | About 0: the owner's PRs stay unapproved; only outside PRs gain | Same | Same |
| Effort and risk | One API call, easy to revert, large auto-merge risk | A file plus the settings, same risk | New workflow to maintain, a required check that must never hang |

## Recommendation

**Change nothing yet; the owner runs experiment E1; apply configuration A's settings only if E1 passes, otherwise accept the finding.**

Settings for A (ruleset `Development (Trunk)`, id 25820378), to be applied only after E1 passed:

- `pull_request.parameters.required_approving_review_count`: `0` -> `1`. Every other `pull_request` parameter stays as it is, including `require_extra_approval_for_unattributed_changes: true`.
- `bypass_actors`: keep `{actor_id: 5, actor_type: "RepositoryRole", bypass_mode: "pull_request"}`; add `{actor_id: 2740, actor_type: "Integration", bypass_mode: "pull_request"}` for Renovate (id from its avatar URL; that the ruleset accepts it is unverified (A)) and the same entry for the release-please App. The release-please App id is unreadable from a session; the owner supplies it (repository variable `RELEASE_APP_ID`).
- If experiment E2 shows `pull_request` mode does not apply to App-enabled auto-merge, use `bypass_mode: "always"` for the two Apps.
- Expected Scorecard effect (assumption, from the scoring code): Branch-Protection 3 -> 4, Code-Review unchanged.

If E1 fails (auto-merge waits for the approval despite the bypass), accept the finding. The owner's own PRs pass the full CI gate and the review step in `docs/agents/implementation.md`, and outside contributors cannot merge by themselves because merging needs write access (A, see Collaborators above), so the owner already sits between every outside PR and `main`; what is missing is only a recorded approval.

The finding for the owner's own PRs (Code-Review 0/10) is acceptable in both outcomes: no option makes a solo owner's self-merged PRs count as reviewed, and the check says so itself ("Requiring reviews for all changes is infeasible for some projects, such as those with only one active participant", `docs/checks.md#code-review`).

## Not verifiable from a session, with the experiment

- E1, auto-merge versus bypass for a human admin: create a scratch repository under the owner account, copy the ruleset with 1 required approval and the admin role in bypass mode `pull_request`, enable auto-merge in the repository settings, open a PR from a branch with a green required check, click "Enable auto-merge" (squash). Observe: does it merge by itself, or does it sit as "Auto-merge enabled, waiting for approval"? Repeat once with the bypass mode `always`. Reading `mergeStateStatus` and the PR timeline through the API gives the same answer.
- E2, auto-merge versus bypass for an App: in the same scratch repository install Renovate, add it as bypass actor, let it open and auto-merge a PR; same observation. Do the same with a GitHub App token used by `gh pr merge --auto`. This also shows whether the `Integration` bypass entry is accepted.
- E5, the Scorecard numbers (4 and 8 above) come from reading the scoring code, not from running Scorecard. Check: after a settings change, run `gh workflow run Scorecard` (the workflow has `workflow_dispatch`) or the public API again and compare the `details` list of Branch-Protection.

## Optional: only if E1 fails

Only relevant if the owner wants a recorded approval for outside PRs although E1 failed. Configuration C (the review-gate status check) is the fallback; configuration B is not recommended.

- E3, the review-gate check (configuration C): a workflow on `pull_request` and `pull_request_review` writes the status. For `pull_request_review` events `GITHUB_SHA` is the merge ref, not the PR head, so the check may not attach to the commit the ruleset waits on (A, from memory of the Actions event docs, not re-read for this report). Test with a scratch PR from a fork whether the required check turns green after the approval, and whether first-time contributors' workflows wait for manual approval of the run.
- E4, code owner as author (configuration B): whether a PR authored by the sole code owner can ever satisfy `require_code_owner_review` without bypass. The docs only say authors cannot approve their own PR.
