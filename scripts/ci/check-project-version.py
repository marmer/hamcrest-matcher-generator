#!/usr/bin/env python3
"""Fail when a module pom's own or parent version differs from the version in .release-please-manifest.json.

release-please raises the manifest version with every release PR; everything that must follow it is raised in the same
PR (`extra-files` of release-please-config.json) or derived from `${project.version}`. This check keeps both true:
  - every pom whose parent is the project (groupId io.github.marmer.testutils) and the root pom carries the manifest version as its own and parent version,
    unless the build filters the value (`@project.version@`, the frozen 5.x suite),
  - such a pom is an xml entry of `extra-files`, so the next release raises it, and every entry names an existing pom,
  - a dependency on a project artifact has no literal version (a property such as `${project.version}`, or managed),
  - a line marked `x-release-please-version` in a plain `extra-files` entry carries the manifest version.

A pom whose parent is not of that group (the consumer poms below `src/it`, which are filtered and built apart from the
reactor) is skipped by design. Poms below target/, .git/, .claude/ and node_modules/ are not looked at.
An unreadable manifest, config or pom is reported as one line naming the file (exit 1), never as a traceback.

Usage: scripts/ci/check-project-version.py [<root-dir>]
"""
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GROUP = "io.github.marmer.testutils"
NS = "{http://maven.apache.org/POM/4.0.0}"
PLACEHOLDER = "@project.version@"
POM_VERSIONS_XPATH = ('/*[local-name()="project"]/*[local-name()="version"] | '
                      '/*[local-name()="project"]/*[local-name()="parent"]/*[local-name()="version"]')
MARKER = "x-release-please-version"
SKIPPED_DIRS = {"target", ".git", ".claude", "node_modules"}


class CheckError(Exception):
    """A file the check needs cannot be used; the message starts with the file's path."""


def child_text(element, name):
    child = element.find(NS + name)
    return child.text.strip() if child is not None and child.text else None


def poms(root):
    """Relative posix paths of all pom.xml files below root, outside build output and tooling directories."""
    found = []
    for directory, subdirectories, files in os.walk(root):
        subdirectories[:] = [d for d in subdirectories if d not in SKIPPED_DIRS]
        if "pom.xml" in files:
            found.append((Path(directory) / "pom.xml").relative_to(root).as_posix())
    return sorted(found)


def read_json(root, name):
    try:
        return json.loads((root / name).read_text(encoding="utf-8"))
    except OSError as error:
        raise CheckError(f"{name}: cannot be read ({error.strerror})") from error
    except ValueError as error:
        raise CheckError(f"{name}: not valid JSON ({error})") from error


def manifest_version(root):
    name = ".release-please-manifest.json"
    manifest = read_json(root, name)
    if not isinstance(manifest, dict) or not isinstance(manifest.get("."), str):
        raise CheckError(f"{name}: no version for the root package \".\"")
    return manifest["."]


def extra_files(root):
    name = "release-please-config.json"
    config = read_json(root, name)
    package = config.get("packages", {}).get(".") if isinstance(config, dict) else None
    if not isinstance(package, dict):
        raise CheckError(f"{name}: no root package \".\" under packages")
    return package.get("extra-files", [])


def parse_pom(root, path):
    try:
        return ET.parse(root / path).getroot()
    except ET.ParseError as error:
        raise CheckError(f"{path}: not well-formed XML ({error})") from error
    except OSError as error:
        raise CheckError(f"{path}: cannot be read ({error.strerror})") from error


def covered_poms(entries):
    """Paths of the xml entries of extra-files that raise the own and the parent version of a pom."""
    return {entry["path"] for entry in entries
            if isinstance(entry, dict) and entry.get("type") == "xml" and entry.get("xpath") == POM_VERSIONS_XPATH}


def is_project_pom(path, project):
    """The root pom, or a pom whose parent is of the project's group; any other pom is skipped by design."""
    parent = project.find(NS + "parent")
    return path == "pom.xml" or (parent is not None and child_text(parent, "groupId") == GROUP)


def is_filtered(project):
    """True when the build fills in the version (`@project.version@`), as in the frozen 5.x suite."""
    parent = project.find(NS + "parent")
    versions = {child_text(project, "version"), child_text(parent, "version") if parent is not None else None}
    return versions - {None} == {PLACEHOLDER}


def pom_problems(path, project, manifest):
    """Problems of one project pom: stale own or parent version, literal version of a project dependency."""
    parent = project.find(NS + "parent")
    parent_version = child_text(parent, "version") if parent is not None else None
    problems = []
    for label, version in (("<version>", child_text(project, "version")), ("<parent><version>", parent_version)):
        if version is not None and version not in (manifest, PLACEHOLDER):
            problems.append(f"{path}: {label} is {version}, the manifest says {manifest}")
    for dependency in project.iter(NS + "dependency"):
        version = child_text(dependency, "version")
        if child_text(dependency, "groupId") == GROUP and version is not None and not version.startswith("${"):
            problems.append(f"{path}: dependency {child_text(dependency, 'artifactId')} has the literal version "
                            f"{version}, use ${{project.version}}")
    return problems


def read_text(root, name):
    try:
        return (root / name).read_text(encoding="utf-8")
    except OSError as error:
        raise CheckError(f"{name}: cannot be read ({error.strerror})") from error


def marker_problems(root, entries, manifest):
    problems = []
    for entry in entries:
        if not isinstance(entry, str):
            continue
        for number, line in enumerate(read_text(root, entry).splitlines(), 1):
            if MARKER not in line:
                continue
            versions = re.findall(r"\d+\.\d+\.\d+(?:-[0-9A-Za-z.]+)?", line)
            if versions != [manifest]:
                problems.append(f"{entry}:{number}: marked {MARKER}, but carries "
                                f"{', '.join(versions) or 'no version'}, the manifest says {manifest}")
    return problems


def problems(root):
    manifest = manifest_version(root)
    entries = extra_files(root)
    covered = covered_poms(entries)
    found = poms(root)
    result = []
    for path in found:
        project = parse_pom(root, path)
        if project.tag != NS + "project":
            result.append(f"{path}: root element is not <project> in the namespace {NS[1:-1]}, "
                          "so it cannot be checked")
            continue
        if not is_project_pom(path, project):
            continue
        result.extend(pom_problems(path, project, manifest))
        if not is_filtered(project) and path not in covered:
            result.append(f"{path}: not an xml entry of extra-files in release-please-config.json, "
                          "release-please would not raise its version")
    for path in sorted(covered - set(found)):
        result.append(f"{path}: listed in extra-files of release-please-config.json, but no such pom")
    return result + marker_problems(root, entries, manifest)


def main():
    if len(sys.argv) > 2:
        sys.exit(__doc__)
    root = Path(sys.argv[1]).resolve() if len(sys.argv) == 2 else ROOT
    try:
        found = problems(root)
    except CheckError as error:
        sys.exit(str(error))
    for problem in found:
        print(problem)
    if found:
        sys.exit(1)
    print("every module pom follows the version of .release-please-manifest.json")


if __name__ == "__main__":
    main()
