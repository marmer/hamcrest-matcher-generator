# Java 17 baseline for 6.0.0

6.0.0 raises the minimum Java version of the processor, the runtime artifacts and the generated code from 11 to 17 (`release=17`, Enforcer checks bytecode 17 of the shipped dependencies, Kotlin `jvmTarget` 17). The ecosystem the autopilot updates against has left Java 11 behind: JUnit 6 and AssertJ 4 need Java 17, Maven 4 needs it to run, and Structurizr and Error Prone already need 21. With a Java 11 baseline, each of these majors would arrive as a red Renovate pull request that waits for the owner, which defeats the autopilot of a final feature release. A major version is the one place where such a break is allowed.

## Considered Options

- **Keep Java 11 and cap each Java-17-only dependency in Renovate**: rejected, because the list of caps only grows, test-only tools would be frozen on old majors, and every new case still lands on the owner first.
- **Keep Java 11, run Maven on 17+ and verify Java 11 via toolchains** (compile with javac 11 incl. annotation processing, test on JDK 11, JUnit 5 pinned in those modules): rejected for the same reason in the shipped scope (AssertJ 4 would still need a cap), at the cost of a second, unusual CI setup.
- **Let each such major go red and decide case by case**: rejected, it makes the owner the gate for routine updates.

## Consequences

- The CI matrix is JDK 17, 21, 25 and `latest`; `Verify JDK 11` leaves the matrix and the `main` ruleset. The JDK 21+ profile (Error Prone, NullAway, Structurizr docs) stays, the 17 leg skips it.
- Records are always available: the records end-to-end module runs on every leg, and detection may use `ElementKind.RECORD` directly.
- The AssertJ backend follows AssertJ's current stable line (3.27.x today, 4.x once GA) without a cap; generated code avoids APIs removed in 4.x.
- 5.x users on JDK 17+ upgrade by a version bump alone; users on Java 11 stay on 5.x, which gets no further releases. The migration guide and the 6.0.0 CHANGELOG say so.
- The next wave (libraries requiring Java 21) is a recorded risk in arc42 chapter 11: it shows up as one red Renovate pull request, and the owner either caps that dependency with a comment or raises the baseline in a later major.
