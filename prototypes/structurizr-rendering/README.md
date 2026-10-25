# Prototype: Structurizr C4 rendering inside `mvn verify`

Throwaway proof for [Documentation and examples structure](https://github.com/marmer/hamcrest-matcher-generator/issues/64) and ADR 0003. Never merged.

Run (JDK 21+, from this directory):

```bash
mvn verify -Ddiagram.structurizrcli.home=$PWD/target/structurizr
```

It renders all five view types of `src/docs/asciidoc/workspace.dsl` (system context, container, component, dynamic, deployment) to SVG in `target/generated-docs/`, with no Docker, no Graphviz and no Structurizr CLI download. `rendered/` holds the output of the last run.

Gotchas the implementation must keep:

- asciidoctorj-diagram's `[structurizr, view=…]` block expects a Structurizr CLI home. A folder whose `lib/` holds the Maven jars works: `dependency:copy-dependencies` of `structurizr-dsl`, `-export`, `-import` and `-client` (transitives included). Without `structurizr-client` rendering fails with `NoClassDefFoundError: com/structurizr/http/HttpClient`; hand-listing jars misses transitives such as httpclient5.
- `diagram.structurizrcli.home` must reach the Maven JVM (CLI flag here; `.mvn/jvm.config` or properties-maven-plugin in the real build).
- Structurizr 6.x is compiled for Java 21: the docs module needs a JDK 21+ profile.
- PlantUML (bundled by `asciidoctorj-diagram-plantuml`) falls back to the Smetana layout when no `dot` binary exists.
- `logHandler failIf WARN` turns DSL, classpath and PlantUML errors into build failures.
