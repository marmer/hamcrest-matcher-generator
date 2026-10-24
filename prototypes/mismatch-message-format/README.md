# PROTOTYPE: Mismatch message format

Throwaway, never merged. Answers [Mismatch message format](https://github.com/marmer/hamcrest-matcher-generator/issues/66) on wayfinder map #52.

Hand-written stand-ins for what the 6.0.0 generator would emit for a flat POJO (`Person`), a nested
POJO graph (`Order` → `Customer` → `Address`) and a record (`Address`), run against real Hamcrest 2.2
and AssertJ 3.27.3. `./run.sh` prints every scenario:

- **5.x today**: the real 5.x runtime (`legacy/BeanPropertyMatcher.java`, verbatim copy).
- **Hamcrest A / B / C**: path list, inline expected-vs-actual, expected/actual blocks
  (`hamcrest/PropertyMatcher.java`, switch `PropertyMatcher.format`).
- **AssertJ J1 / J2**: native `Expecting …` wording with the property name only, or with the
  property path from the root (`assertj/PropertyAssert.java`, switch `PropertyAssert.withPath`).
- **Reference object**: `isOrderLike` (equalTo per property) vs `isOrderLikeDeep` (recursing into
  nested generated matchers).
- **Nested lists** (`./run.sh` second part, `Demo lists`): `Cart` with `List<OrderLine>`; generated
  matchers inside Hamcrest's `contains`/`hasItem` with multi-line vs one-line rendering
  (`PropertyMatcher.oneLineInsideForeignMatchers`), a runtime `itemsInOrder(..)` with indexed paths,
  shallow vs deep reference object, and the AssertJ path into lists (`PropertyAssert.pathIntoCollections`).
