# Docs as code: Asciidoctor in the Maven reactor, Structurizr C4 model, embedded example modules

All documentation is built and checked by `mvn verify`. A `docs` module renders the user guide and the arc42 architecture docs with the `asciidoctor-maven-plugin`; documented code is included via tagged regions from example Maven modules that compile and run their tests in the same build, so a snippet can never drift from working code. The arc42 context, building block, runtime and deployment views are C4 views generated from one Structurizr DSL workspace (`workspace.dsl`) through asciidoctorj-diagram's `structurizr` block, C4-PlantUML and PlantUML's built-in Smetana layout, so the diagram levels stay consistent and need little explanatory prose. The build fails on any Asciidoctor warning, and tests check internal links, anchors and images in the rendered HTML, and that every workspace view is embedded and every embedded diagram comes from a view.

## Consequences

- Structurizr 6.x requires Java 21, so the `docs` module only builds in a JDK 21+ profile (like Error Prone and NullAway); the JDK 17 CI job skips it, the main JDK 21+ job always builds it. Structurizr versions are pinned and updated together, because driving asciidoctorj-diagram with the Maven jars instead of the Structurizr CLI is undocumented.
- The Docker-based docs build and the `gh-pages` branch go away; Pages is deployed with the official Pages actions on each release.

## Considered Options

- Docker or a downloaded Structurizr CLI: a second toolchain outside Maven that local builds would skip.
- Mermaid or d2 rendering: they need Node or extra binaries.
- Pinning Structurizr 5.x to build on JDK 17: it freezes an outdated version that Renovate would keep flagging.
- Hand-drawn diagrams: they drift between levels and need long captions.
