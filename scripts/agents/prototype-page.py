#!/usr/bin/env python3
"""Builds the overview page (an Artifact) for a wayfinder prototype ticket from the prototype's run output.

Usage: scripts/agents/prototype-page.py OUTPUT.txt SPEC.json > page.html

OUTPUT.txt is what the prototype's run.sh prints, in this shape:

    ##### <GROUP> <ID> <scenario title>
    === <variant name>
    <real output, or PASSED>

GROUP is one word (e.g. HAMCREST, ASSERTJ), ID one word (e.g. S1, L1-before). Header lines with fewer
than three words are separators and are skipped; Java stack-trace lines are dropped from the output.

SPEC.json holds the page text, all optional except "title":

    {
      "title": "Mismatch Message Format",
      "intro_html": "<p>What does a failing check print? ...</p>",
      "decisions_html": "<div class=\\"decision\\"><span class=\\"q\\">Q1</span><h3>... <span class=\\"pick\\">recommended</span></h3>...</div>",
      "sections": [{"heading": "Nested lists", "match": "^[A-Z]+ L", "intro_html": "<p>...</p>"},
                   {"heading": "Hamcrest scenarios", "match": "^HAMCREST "}],
      "labels": {"B_INLINE": "B · inline expected/actual"},
      "picks": ["B_INLINE"],
      "code": {"HAMCREST S1": "isPerson().withName(\\"Bob\\")"}
    }

Each scenario goes to the first section whose "match" regex matches "<GROUP> <ID>"; without sections,
one section per GROUP. "code" shows the test code above a scenario; "picks" marks variants as recommended.
"""
import html
import json
import re
import sys

CSS = """
:root {
  --bg: #f6f7f5; --surface: #ffffff; --fg: #1d2420; --muted: #5d6a62; --line: #d9ded9;
  --accent: #2f6b4f; --console: #eef1ee; --pick: #8a5a00; --pick-bg: #fff1d6;
  --display: "Fraunces", Georgia, serif; --body: "IBM Plex Sans", system-ui, sans-serif; --mono: "IBM Plex Mono", ui-monospace, Menlo, monospace;
}
@media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) {
  --bg: #141816; --surface: #1b201d; --fg: #e3e8e4; --muted: #9aa69f; --line: #2e3532;
  --accent: #7cc4a0; --console: #101311; --pick: #ffc561; --pick-bg: #3a2c10; color-scheme: dark } }
:root[data-theme="dark"] {
  --bg: #141816; --surface: #1b201d; --fg: #e3e8e4; --muted: #9aa69f; --line: #2e3532;
  --accent: #7cc4a0; --console: #101311; --pick: #ffc561; --pick-bg: #3a2c10; color-scheme: dark }
body { background: var(--bg); color: var(--fg); font: 15px/1.55 var(--body); }
main { max-width: 980px; margin: 0 auto; padding-block: 32px 64px; padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)); display: grid; gap: 28px; }
h1 { font: 600 2rem/1.15 var(--display); margin: 0; text-wrap: balance; }
h2 { font: 600 1.35rem/1.2 var(--display); margin: 8px 0 0; text-wrap: balance; }
h3 { font-size: 1rem; margin: 0; font-weight: 600; text-wrap: balance; }
p, li { max-width: 68ch; } p { margin: 0; }
a { color: var(--accent); }
code { font: .88em var(--mono); background: var(--console); padding: 1px 4px; border-radius: 3px; }
.eyebrow { font: 500 .75rem var(--mono); letter-spacing: .08em; text-transform: uppercase; color: var(--muted); }
.intro, .decisions, .group { display: grid; gap: 12px; }
.decision, .scenario { background: var(--surface); border: 1px solid var(--line); border-radius: 6px; padding: 14px 16px; display: grid; gap: 10px; min-width: 0; }
.decision h3 { display: flex; gap: 8px; align-items: baseline; flex-wrap: wrap; }
.decision .q { font: 500 .8rem var(--mono); color: var(--muted); }
.decision ul { margin: 0; padding-left: 18px; }
.pick { font: 500 .7rem var(--mono); letter-spacing: .04em; text-transform: uppercase; color: var(--pick); background: var(--pick-bg); padding: 1px 6px; border-radius: 3px; white-space: nowrap; }
.scenario header { display: flex; gap: 10px; align-items: baseline; flex-wrap: wrap; }
.lib { font: 500 .7rem var(--mono); text-transform: uppercase; letter-spacing: .06em; padding: 1px 6px; border-radius: 3px; border: 1px solid currentColor; color: var(--accent); }
pre { margin: 0; font: 12.5px/1.5 var(--mono); overflow-x: auto; white-space: pre; }
pre.code { color: var(--muted); border-left: 2px solid var(--line); padding-left: 10px; }
.variants { display: grid; gap: 10px; min-width: 0; }
.variant { min-width: 0; }
.variant pre { background: var(--console); padding: 10px 12px; border-radius: 4px; }
.vname { font: 600 .8rem var(--body); margin-bottom: 4px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
nav { display: flex; gap: 8px 16px; flex-wrap: wrap; font-size: .9rem; }
"""

STACK_LINE = re.compile(r"\n\s+at [^\n]*|\n\s+\.\.\.\([^\n]*")


def parse(output):
    scenarios = []
    for block in re.split(r"^##### ", output, flags=re.M)[1:]:
        head, _, body = block.partition("\n")
        words = head.split(" ", 2)
        if len(words) < 3:
            continue
        group, sid, title = words
        variants = []
        for v in re.split(r"^=== ", body, flags=re.M)[1:]:
            name, _, text = v.partition("\n")
            variants.append((name.strip(), STACK_LINE.sub("", text.strip())))
        scenarios.append({"group": group, "id": sid, "title": title.strip(), "variants": variants})
    return scenarios


def card(sc, spec):
    labels, picks, code = spec.get("labels", {}), set(spec.get("picks", [])), spec.get("code", {})
    key = f"{sc['group']} {sc['id']}"
    parts = [f'<article class="scenario" id="{html.escape(key.lower().replace(" ", "-"))}">',
             f'<header><span class="lib">{html.escape(sc["group"].title())}</span><h3>{html.escape(sc["title"])}</h3></header>']
    if key in code:
        parts.append(f'<pre class="code">{html.escape(code[key])}</pre>')
    parts.append('<div class="variants">')
    for name, text in sc["variants"]:
        pick = ' <span class="pick">recommended</span>' if name in picks else ""
        parts.append(f'<div class="variant"><div class="vname">{html.escape(labels.get(name, name))}{pick}</div>'
                     f'<pre>{html.escape(text)}</pre></div>')
    parts.append("</div></article>")
    return "\n".join(parts)


def main():
    output = open(sys.argv[1], encoding="utf-8").read()
    spec = json.load(open(sys.argv[2], encoding="utf-8"))
    scenarios = parse(output)
    sections = spec.get("sections") or [
        {"heading": f"{g.title()} scenarios", "match": f"^{re.escape(g)} "}
        for g in dict.fromkeys(s["group"] for s in scenarios)]
    buckets = [[] for _ in sections]
    for sc in scenarios:
        key = f"{sc['group']} {sc['id']}"
        for i, sec in enumerate(sections):
            if re.search(sec["match"], key):
                buckets[i].append(sc)
                break
        else:
            sys.exit(f"prototype-page: no section matches scenario '{key}'")
    nav = ['<a href="#decide">What to decide</a>'] if spec.get("decisions_html") else []
    body = []
    for i, (sec, items) in enumerate(zip(sections, buckets)):
        anchor = f"section-{i + 1}"
        nav.append(f'<a href="#{anchor}">{html.escape(sec["heading"])}</a>')
        body.append(f'<section class="group" id="{anchor}"><h2>{html.escape(sec["heading"])}</h2>'
                    f'{sec.get("intro_html", "")}\n' + "\n".join(card(s, spec) for s in items) + "</section>")
    if "title" not in spec:
        sys.exit("prototype-page: SPEC.json needs a \"title\"")
    title = html.escape(spec["title"])
    print(f"""<title>{title}</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;600&family=IBM+Plex+Mono:wght@400;500&family=Fraunces:opsz,wght@9..144,600&display=swap">
<style>{CSS}</style>
<main>
<section class="intro"><div class="eyebrow">Wayfinder · prototype</div><h1>{title}</h1>
{spec.get("intro_html", "")}
<nav>{"".join(nav)}</nav></section>
{f'<section class="decisions" id="decide"><h2>What to decide</h2>{spec["decisions_html"]}</section>' if spec.get("decisions_html") else ""}
{"".join(body)}
</main>""")


if __name__ == "__main__":
    main()
