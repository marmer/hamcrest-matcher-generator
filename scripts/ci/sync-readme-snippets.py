#!/usr/bin/env python3
"""Copy the snippets of the README from the tagged regions of the example modules, or check that they are current.

A snippet is a directive in README.md followed by the code block the script writes, closed by an end line:

    <!-- snippet: example-maven-java-hamcrest/pom.xml#maven-dependencies -->
    ```xml
    ...the region, as written below...
    ```
    <!-- end snippet -->

The region is the text between `tag::NAME[]` and `end::NAME[]` (either marker in any comment syntax), without the two
marker lines, dedented by its least indented line. The fence language follows the extension of the file (xml, java,
kotlin for .kt and .kts). Text outside the directives is never touched.

Modes:
  scripts/ci/sync-readme-snippets.py [<root-dir>]           rewrite the stale blocks of README.md
  scripts/ci/sync-readme-snippets.py --check [<root-dir>]   print one line per stale block or broken directive, exit 1

Exits 1 when a directive names a missing file or region, a directive has no end line, or (with --check) a block is
out of sync. Stdlib only, like the other scripts of scripts/ci.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
README = "README.md"
DIRECTIVE = re.compile(r"^<!-- snippet: (\S+)#(\S+) -->$")
END = "<!-- end snippet -->"
LANGUAGES = {".xml": "xml", ".java": "java", ".kt": "kotlin", ".kts": "kotlin"}


class SnippetError(Exception):
    """A directive or region that cannot be used; the message names the file or the region."""


def fence_language(path):
    for suffix, language in LANGUAGES.items():
        if path.endswith(suffix):
            return language
    raise SnippetError(f"{path}: no fence language for this extension")


def extract_region(text, tag):
    """The lines between the tag and end markers of region `tag`, dedented; a missing or doubled region is an error."""
    opening = re.compile(rf"tag::{re.escape(tag)}\[\]")
    closing = re.compile(rf"end::{re.escape(tag)}\[\]")
    lines = text.split("\n")
    starts = [i for i, line in enumerate(lines) if opening.search(line)]
    if not starts:
        raise SnippetError(f"no region {tag}")
    if len(starts) > 1:
        raise SnippetError(f"more than one region {tag}")
    ends = [i for i in range(starts[0] + 1, len(lines)) if closing.search(lines[i])]
    if not ends:
        raise SnippetError(f"region {tag} has no end::{tag}[] marker")
    body = lines[starts[0] + 1:ends[0]]
    indents = [len(line) - len(line.lstrip()) for line in body if line.strip()]
    least = min(indents) if indents else 0
    body = [line[least:].rstrip() for line in body]
    while body and not body[0]:
        body.pop(0)
    while body and not body[-1]:
        body.pop()
    return body


def snippet_lines(root, path, tag):
    """The lines the directive must carry: the fence, the region and the closing fence."""
    try:
        text = (root / path).read_text(encoding="utf-8")
    except OSError as error:
        raise SnippetError(f"{path} cannot be read ({error.strerror})") from error
    return [f"```{fence_language(path)}"] + extract_region(text, tag) + ["```"]


def process(root):
    """Return (the README text with every block regenerated, drift problems, other problems) as lines of text."""
    text = (root / README).read_text(encoding="utf-8")
    lines = text.split("\n")
    out, drift, errors = [], [], []
    i = 0
    while i < len(lines):
        match = DIRECTIVE.match(lines[i])
        if not match:
            out.append(lines[i])
            i += 1
            continue
        path, tag = match.groups()
        end = None
        for j in range(i + 1, len(lines)):
            if lines[j] == END:
                end = j
                break
            if DIRECTIVE.match(lines[j]):
                break
        where = f"{README}:{i + 1}: "
        if end is None:
            errors.append(where + "snippet directive has no <!-- end snippet --> line")
            out.extend(lines[i:])
            break
        try:
            expected = [lines[i]] + snippet_lines(root, path, tag) + [END]
        except SnippetError as error:
            errors.append(where + str(error))
            out.extend(lines[i:end + 1])
            i = end + 1
            continue
        if lines[i:end + 1] != expected:
            drift.append(where + f"snippet {path}#{tag} is out of sync with its example region; "
                                 "run scripts/ci/sync-readme-snippets.py")
        out.extend(expected)
        i = end + 1
    return "\n".join(out), drift, errors


def problems(root):
    """Every line that makes the check fail; empty when the README is in sync."""
    _, drift, errors = process(root)
    return drift + errors


def write(root):
    """Rewrite README.md with the regenerated blocks; returns the problems that a rewrite cannot repair."""
    new_text, _, errors = process(root)
    (root / README).write_text(new_text, encoding="utf-8")
    return errors


def directive_count(root):
    return sum(1 for line in (root / README).read_text(encoding="utf-8").split("\n") if DIRECTIVE.match(line))


def main(argv):
    check_only = "--check" in argv
    arguments = [argument for argument in argv if argument != "--check"]
    if len(arguments) > 1:
        sys.exit(__doc__)
    root = Path(arguments[0]).resolve() if arguments else ROOT
    if check_only:
        found = problems(root)
        for line in found:
            print(line)
        if found:
            sys.exit(1)
        print("the README snippets are in sync with the example regions")
        return
    errors = write(root)
    for line in errors:
        print(line)
    if errors:
        sys.exit(1)
    print("the README snippets are written from the example regions")


if __name__ == "__main__":
    main(sys.argv[1:])
