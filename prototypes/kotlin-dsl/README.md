# PROTOTYPE: Shape and delivery of the Kotlin DSL

Throwaway. Answers wayfinder ticket #67 (map #52); never merged.

- `java/`: hand-written stand-ins of the generated 6.0.0 Java code (from the "Mismatch message format" prototype),
  now JSpecify-annotated, plus the K2 `Consumer` overloads.
- `kotlin/proto/dsl/`: the two Kotlin layers, K3 (per-type wrapper) and K4 (generic, getter references).
- `kotlin/proto/demo/K0..K4.kt`: the same sample-set scenarios written in each candidate shape; `AssertJ.kt` the AssertJ backend as is.
- `compile/`: mistakes a test author makes; `run.sh` prints kotlinc's verdict per candidate.

Run: `./run.sh` (JDK 17+, Kotlin 2.2.21 by default; `KV=2.1.20 ./run.sh` shows K4 failing harder).
Page: `./page-code.py spec.json > /tmp/s.json && ../../scripts/agents/prototype-page.py run-output.txt /tmp/s.json > page.html`.
