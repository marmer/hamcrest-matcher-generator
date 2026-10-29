#!/usr/bin/env python3
"""Copy the snippets of the README from the tagged regions of the example modules, or check that they are current.

A snippet is a directive in README.md followed by the code block the script writes, closed by an end line:

    <!-- snippet: example-maven-java-hamcrest/pom.xml#maven-dependencies -->
    ```xml
    ...the region, as written below...
    ```
    <!-- end snippet -->

The region is the text between `tag::NAME[]` and `end::NAME[]` (either marker in any comment syntax), without the two
marker lines. Tabs are expanded to 4 columns, then the region is dedented by its least indented line. The fence language
follows the extension of the file (xml, java, kotlin for .kt and .kts). Text outside the directives is never touched.
A directive path must stay inside the repository. README.md keeps its line endings (LF or CRLF).

Markdown has no include of tagged regions as Asciidoctor has, so the region rule is repeated here; that duplication is
unavoidable for the README.

Modes:
  scripts/ci/sync-readme-snippets.py [<root-dir>]           rewrite the stale blocks of README.md
  scripts/ci/sync-readme-snippets.py --check [<root-dir>]   print one line per stale block or broken directive, exit 1

Exits 1 with one line when README.md or a directive cannot be used (a missing or unreadable file, no directive at all,
a path outside the repository). Stdlib only, like the other scripts of scripts/ci.
"""
import re
import sys
from pathlib import Path, PurePosixPath
from typing import NamedTuple

ROOT = Path(__file__).resolve().parents[2]
README = "README.md"
DIRECTIVE = re.compile(r"^<!-- snippet: (\S+)#(\S+) -->$")
END = "<!-- end snippet -->"
LANGUAGES = {".xml": "xml", ".java": "java", ".kt": "kotlin", ".kts": "kotlin"}
TAB_WIDTH = 4


class CheckError(Exception):
    """A file the script needs cannot be used; the message starts with the file's name."""


class SnippetError(Exception):
    """A directive or region that cannot be used; the message names the file or the region."""


class Sync(NamedTuple):
    """The result of processing README.md: its text with the blocks regenerated, and the problems found."""
    text: str
    drift: list
    errors: list

    @property
    def problems(self):
        return self.drift + self.errors


def read_raw(root, name):
    """The text of a file as written (CRLF kept), or a CheckError that names the file."""
    try:
        raw = (root / name).read_bytes()
    except OSError as error:
        raise CheckError(f"{name}: cannot be read ({error.strerror})") from error
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError as error:
        raise CheckError(f"{name}: not UTF-8 text") from error


def read_text(root, name):
    """The text of a file with LF line endings."""
    return read_raw(root, name).replace("\r\n", "\n")


def fence_language(path):
    for suffix, language in LANGUAGES.items():
        if path.endswith(suffix):
            return language
    raise SnippetError(f"{path}: no fence language for this extension")


def extract_region(text, tag):
    """The lines between the tag and end markers of region `tag`, tabs expanded and dedented; a missing or doubled
    region is an error."""
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
    body = [line.expandtabs(TAB_WIDTH) for line in lines[starts[0] + 1:ends[0]]]
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
    pure = PurePosixPath(path)
    if pure.is_absolute() or ".." in pure.parts:
        raise SnippetError(f"{path} must stay inside the repository")
    try:
        text = read_text(root, path)
    except CheckError as error:
        raise SnippetError(str(error)) from error
    return [f"```{fence_language(path)}"] + extract_region(text, tag) + ["```"]


def directive_pairs(root):
    """The (example file, tag) pairs of the directives in README.md, in document order."""
    return [match.groups() for match in (DIRECTIVE.match(line) for line in read_text(root, README).split("\n")) if match]


def process(root):
    """Process README.md: its blocks regenerated with its own line endings, the out-of-sync lines and the errors."""
    raw = read_raw(root, README)
    newline = "\r\n" if "\r\n" in raw else "\n"
    lines = raw.replace("\r\n", "\n").split("\n")
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
    if not any(DIRECTIVE.match(line) for line in lines):
        errors.append(f"{README}: no snippet directive found")
    return Sync("\n".join(out).replace("\n", newline), drift, errors)


def problems(root):
    """Every line that makes the check fail; empty when the README is in sync."""
    return process(root).problems


def write(root):
    """Rewrite README.md with the regenerated blocks; returns the errors that a rewrite cannot repair."""
    result = process(root)
    (root / README).write_bytes(result.text.encode("utf-8"))
    return result.errors


def main(argv):
    check_only = "--check" in argv
    arguments = [argument for argument in argv if argument != "--check"]
    if len(arguments) > 1:
        sys.exit(__doc__)
    root = Path(arguments[0]).resolve() if arguments else ROOT
    try:
        if check_only:
            found = problems(root)
            for line in found:
                print(line)
            if found:
                sys.exit(1)
            print("the README snippets are in sync with the example regions")
            return
        errors = write(root)
    except CheckError as error:
        sys.exit(str(error))
    for line in errors:
        print(line)
    if errors:
        sys.exit(1)
    print("the README snippets are written from the example regions")


if __name__ == "__main__":
    main(sys.argv[1:])
