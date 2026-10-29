#!/usr/bin/env python3
"""Fail when a module pom's own or parent version differs from the version in .release-please-manifest.json.

release-please raises the manifest version with every release PR; everything that must follow it is raised in the same
PR (`extra-files` of release-please-config.json) or derived from `${project.version}`. This check keeps both true:
  - every pom whose parent is the project (and the root pom) carries the manifest version as its own and parent version,
    unless the build filters the value (`@project.version@`, the frozen 5.x suite),
  - such a pom is an xml entry of `extra-files`, so the next release raises it, and every entry names an existing pom,
  - a dependency on a project artifact has no literal version (a property such as `${project.version}`, or managed),
  - a line marked `x-release-please-version` in a plain `extra-files` entry carries the manifest version.

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


def extra_files(root):
    config = json.loads((root / "release-please-config.json").read_text(encoding="utf-8"))
    return config["packages"]["."].get("extra-files", [])


def covered_poms(entries):
    """Paths of the xml entries of extra-files that raise the own and the parent version of a pom."""
    return {entry["path"] for entry in entries
            if isinstance(entry, dict) and entry.get("type") == "xml" and entry.get("xpath") == POM_VERSIONS_XPATH}


def pom_problems(path, project, manifest):
    """Problems of one pom, or None when it is not a pom of this project (no parent of the group, not the root)."""
    parent = project.find(NS + "parent")
    parent_version = child_text(parent, "version") if parent is not None else None
    in_project = path == "pom.xml" or (parent is not None and child_text(parent, "groupId") == GROUP)
    if not in_project:
        return None
    problems = []
    own = child_text(project, "version")
    for label, version in (("<version>", own), ("<parent><version>", parent_version)):
        if version is not None and version not in (manifest, PLACEHOLDER):
            problems.append(f"{path}: {label} is {version}, the manifest says {manifest}")
    for dependency in project.iter(NS + "dependency"):
        version = child_text(dependency, "version")
        if child_text(dependency, "groupId") == GROUP and version is not None and not version.startswith("${"):
            problems.append(f"{path}: dependency {child_text(dependency, 'artifactId')} has the literal version "
                            f"{version}, use ${{project.version}}")
    return problems, {own, parent_version} - {None} == {PLACEHOLDER}


def marker_problems(root, entries, manifest):
    problems = []
    for entry in entries:
        if not isinstance(entry, str):
            continue
        for number, line in enumerate((root / entry).read_text(encoding="utf-8").splitlines(), 1):
            if MARKER not in line:
                continue
            versions = re.findall(r"\d+\.\d+\.\d+(?:-[0-9A-Za-z.]+)?", line)
            if versions != [manifest]:
                problems.append(f"{entry}:{number}: marked {MARKER}, but carries "
                                f"{', '.join(versions) or 'no version'}, the manifest says {manifest}")
    return problems


def problems(root):
    manifest = json.loads((root / ".release-please-manifest.json").read_text(encoding="utf-8"))["."]
    entries = extra_files(root)
    covered = covered_poms(entries)
    found = poms(root)
    result = []
    for path in found:
        checked = pom_problems(path, ET.parse(root / path).getroot(), manifest)
        if checked is None:
            continue
        pom_result, filtered = checked
        result.extend(pom_result)
        if not filtered and path not in covered:
            result.append(f"{path}: not an xml entry of extra-files in release-please-config.json, "
                          "release-please would not raise its version")
    for path in sorted(covered - set(found)):
        result.append(f"{path}: listed in extra-files of release-please-config.json, but no such pom")
    return result + marker_problems(root, entries, manifest)


def main():
    if len(sys.argv) > 2:
        sys.exit(__doc__)
    root = Path(sys.argv[1]).resolve() if len(sys.argv) == 2 else ROOT
    found = problems(root)
    for problem in found:
        print(problem)
    if found:
        sys.exit(1)
    print("every module pom follows the version of .release-please-manifest.json")


if __name__ == "__main__":
    main()
