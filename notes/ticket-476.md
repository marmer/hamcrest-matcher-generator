# Ticket 476 handover

- New package-private `TypeExpansion` (core): `typesToPlan` (names -> types to plan, exclusion applied),
  `unmatchedExclusions` (public member type walk), `isUserType` (Kotlin file facade check).
- `MatcherGenerationProcessorWorker` keeps planning, generation and the without-properties report.
- Seam left by candidate 1 (`ConfiguredNames`) was as assumed; no adjustment of the cut.
- `TypeExpansionTest` stubs only `Elements` and elements, no `ProcessingEnvironment`.
