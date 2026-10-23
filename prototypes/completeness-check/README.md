# PROTOTYPE: Shape of the completeness check

Throwaway. Answers [Shape of the completeness check](https://github.com/marmer/hamcrest-matcher-generator/issues/65)
on the map [Wayfinder: 6.0.0, the final feature release](https://github.com/marmer/hamcrest-matcher-generator/issues/52).
Never merged; the decision lives in the ticket's resolution comment.

Run: `./run.sh` (JDK 11+). It compiles hand-written stand-ins for generated code for a
`Person(name, age, email, phone)` and prints what a test author sees in each scenario.
`phone` plays the property added after the tests were written.

| Candidate | Backend | Where it bites when `phone` is added |
|---|---|---|
| H1 runtime opt-in: `isPerson().checkingAllProperties()…ignoringEmail()` | Hamcrest | test run: `neither checked nor ignored: "phone"` |
| H2 staged builder: `isPersonWithAll().name(…).age(…).email(…).phone(…)` | Hamcrest | compile: `PhoneStep cannot be converted to Matcher` |
| A1a terminal call: `…ignoringEmail().hasAllPropertiesChecked()` | AssertJ | test run, but only if the terminal call is not forgotten |
| A1b scope: `satisfiesCheckingAllProperties(p -> p.hasName(…)…)` | AssertJ | test run |

Files: `src/proto/hamcrest/PersonMatcher.java` (H1, plus reference-object `isPersonLike`),
`src/proto/staged/PersonMatchers.java` (H2), `src/proto/assertj/PersonAssert.java` (A1a/A1b),
`src/proto/Demo.java` (scenarios), `before-phone/` (an old test that must stop compiling under H2).
