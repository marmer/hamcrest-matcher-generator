# PROTOTYPE: GitHub Pages project site (throwaway, never merged)

Answers the question of [GitHub Pages project site](https://github.com/marmer/hamcrest-matcher-generator/issues/68): what the landing page, the navigation, the theme and the maintainer footer look like.
Three variants are rendered from the same AsciiDoc sources:

| Variant | Look |
|---|---|
| `a/` | Asciidoctor default stylesheet, left TOC, slim link bar, one-line footer |
| `b/` | Own stylesheet (`b/theme.css`), sticky top bar, chapter sidebar, "on this page" TOC, hero landing, maintainer card footer, dark mode |
| `c/` | Own stylesheet (`c/theme.css`), whole guide on one page with a sticky chapter rail, architecture separate, signature footer, dark mode |

`shared/` holds the content all variants include (stand-ins for the example-module tag regions, the Getting started body, an arc42 §1 excerpt) and the floating variant switcher.

## Run

```bash
gem install asciidoctor rouge   # rbenv: RBENV_VERSION=<ruby> ASCIIDOCTOR=/opt/rbenv/shims/asciidoctor ./build.sh
./build.sh
python3 -m http.server 8765 --directory out   # open http://localhost:8765/b/index.html
```

## Gotchas for the real docs module

- **`:docrole:` does not reach `<body>`** in Asciidoctor 2.0.26. Per-page layout classes come from a page attribute (`:page-kind: guide`) plus `<script>document.body.className += " {page-kind}"</script>` in `docinfo-header.html`; docinfo files get attribute substitution.
- **Relative links in shared docinfo**: set `:relroot: ../` per page depth and write `{relroot}guide/...` in docinfo files.
- **Attributes in source blocks** need `subs="attributes+"` (for example `{project-version}` in dependency snippets).
- **Rouge inline styles** (`rouge-css=style`) put token backgrounds on spans (`monokai.sublime`, `bw`). A custom theme with dark mode has to reset them (`.listingblock pre span { background: none !important }`), or use `rouge-css=class` with its own token colours.
- **Ligature fonts** (JetBrains Mono) turn the XML callout `<!--1-->` into arrows; set `font-variant-ligatures: none` on code.
- **`[tabs]` needs the asciidoctor-tabs extension**; the prototype uses titled blocks instead.
- **Custom stylesheet replaces the default completely** (`-a stylesheet=...`): admonitions, callouts, tables and the TOC have to be styled by hand.
