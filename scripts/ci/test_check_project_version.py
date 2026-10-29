"""Unit tests for check-project-version.py (stdlib unittest only) and a check of the repository's own poms."""
import importlib.util
import json
import os
import tempfile
import textwrap
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("check_project_version", HERE / "check-project-version.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

GROUP = "io.github.marmer.testutils"


def pom(own=None, parent=None, dependencies=()):
    """A minimal pom; `own` and `parent` are version strings or None."""
    own_xml = f"<version>{own}</version>" if own else ""
    parent_xml = (f"<parent><groupId>{GROUP}</groupId><artifactId>matcher-generator-parent</artifactId>"
                  f"<version>{parent}</version></parent>") if parent else ""
    deps = "".join(
        f"<dependency><groupId>{group}</groupId><artifactId>{artifact}</artifactId>"
        + (f"<version>{version}</version>" if version else "") + "</dependency>"
        for group, artifact, version in dependencies)
    return textwrap.dedent(f"""\
        <project xmlns="http://maven.apache.org/POM/4.0.0">
          <modelVersion>4.0.0</modelVersion>
          {parent_xml}
          <groupId>{GROUP}</groupId>
          <artifactId>module</artifactId>
          {own_xml}
          <dependencies>{deps}</dependencies>
        </project>
        """)


def extra_file(path):
    return {"type": "xml", "path": path, "xpath": check.POM_VERSIONS_XPATH}


class Repo:
    """A throw-away repository tree: a manifest, a release-please config and poms."""

    def __init__(self, manifest="6.0.0", poms=None, extra_files=None, other_files=None):
        self.dir = tempfile.TemporaryDirectory()
        self.root = Path(self.dir.name)
        poms = {"pom.xml": pom(own="6.0.0"), "a/pom.xml": pom(own="6.0.0", parent="6.0.0")} if poms is None else poms
        if extra_files is None:
            extra_files = [extra_file(path) for path in poms]
        self.write(".release-please-manifest.json", json.dumps({".": manifest}))
        self.write("release-please-config.json",
                   json.dumps({"packages": {".": {"extra-files": extra_files}}}))
        for path, text in {**poms, **(other_files or {})}.items():
            self.write(path, text)

    def write(self, path, text):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")

    def problems(self):
        return check.problems(self.root)

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.dir.cleanup()


class ProblemsTest(unittest.TestCase):

    def test_consistent_repository_has_no_problems(self):
        with Repo() as repo:
            self.assertEqual([], repo.problems())

    def test_stale_parent_version_is_reported(self):
        with Repo(poms={"pom.xml": pom(own="6.0.0"), "a/pom.xml": pom(own="6.0.0", parent="5.1.0")}) as repo:
            self.assertEqual(["a/pom.xml: <parent><version> is 5.1.0, the manifest says 6.0.0"], repo.problems())

    def test_stale_own_version_is_reported(self):
        with Repo(poms={"pom.xml": pom(own="5.1.0"), "a/pom.xml": pom(own="6.0.0", parent="6.0.0")}) as repo:
            self.assertEqual(["pom.xml: <version> is 5.1.0, the manifest says 6.0.0"], repo.problems())

    def test_module_without_own_version_checks_the_parent_only(self):
        with Repo(poms={"pom.xml": pom(own="6.0.0"), "a/pom.xml": pom(parent="6.0.0")}) as repo:
            self.assertEqual([], repo.problems())

    def test_manifest_ahead_of_the_poms_is_reported_for_every_pom(self):
        with Repo(manifest="6.0.1") as repo:
            self.assertEqual(["a/pom.xml: <version> is 6.0.0, the manifest says 6.0.1",
                              "a/pom.xml: <parent><version> is 6.0.0, the manifest says 6.0.1",
                              "pom.xml: <version> is 6.0.0, the manifest says 6.0.1"], repo.problems())

    def test_filtered_placeholder_version_is_not_a_drift(self):
        poms = {"pom.xml": pom(own="6.0.0"), "suite/pom.xml": pom(own="@project.version@", parent="@project.version@")}
        with Repo(poms=poms, extra_files=[extra_file("pom.xml")]) as repo:
            self.assertEqual([], repo.problems())

    def test_pom_without_project_parent_is_not_checked(self):
        poms = {"pom.xml": pom(own="6.0.0"), "src/it/pom.xml": pom(own="1")}
        with Repo(poms=poms, extra_files=[extra_file("pom.xml")]) as repo:
            self.assertEqual([], repo.problems())

    def test_pom_missing_from_the_release_please_config_is_reported(self):
        with Repo(extra_files=[extra_file("pom.xml")]) as repo:
            self.assertEqual(["a/pom.xml: not an xml entry of extra-files in release-please-config.json, "
                              "release-please would not raise its version"], repo.problems())

    def test_config_entry_of_a_pom_that_does_not_exist_is_reported(self):
        with Repo(extra_files=[extra_file("pom.xml"), extra_file("a/pom.xml"), extra_file("gone/pom.xml")]) as repo:
            self.assertEqual(["gone/pom.xml: listed in extra-files of release-please-config.json, but no such pom"],
                             repo.problems())

    def test_config_entry_with_another_xpath_is_reported(self):
        wrong = {"type": "xml", "path": "a/pom.xml", "xpath": "//project/version"}
        with Repo(extra_files=[extra_file("pom.xml"), wrong]) as repo:
            self.assertEqual(["a/pom.xml: not an xml entry of extra-files in release-please-config.json, "
                              "release-please would not raise its version"], repo.problems())

    def test_literal_version_of_a_project_dependency_is_reported(self):
        deps = [(GROUP, "matcher-generator-core", "6.0.0")]
        poms = {"pom.xml": pom(own="6.0.0"), "a/pom.xml": pom(own="6.0.0", parent="6.0.0", dependencies=deps)}
        with Repo(poms=poms) as repo:
            self.assertEqual(["a/pom.xml: dependency matcher-generator-core has the literal version 6.0.0, "
                              "use ${project.version}"], repo.problems())

    def test_derived_and_foreign_dependency_versions_are_fine(self):
        deps = [(GROUP, "matcher-generator-core", "${project.version}"), ("org.hamcrest", "hamcrest", "2.2"),
                (GROUP, "managed-by-parent", None),
                (GROUP, "via-property", "${core.version}")]
        poms = {"pom.xml": pom(own="6.0.0"), "a/pom.xml": pom(own="6.0.0", parent="6.0.0", dependencies=deps)}
        with Repo(poms=poms) as repo:
            self.assertEqual([], repo.problems())

    def test_marked_line_of_a_plain_extra_file_must_carry_the_manifest_version(self):
        extra = [extra_file("pom.xml"), extra_file("a/pom.xml"), "example/build.gradle.kts"]
        other = {"example/build.gradle.kts": 'val v = "5.1.0" // x-release-please-version\nval w = "1.0"\n'}
        with Repo(extra_files=extra, other_files=other) as repo:
            self.assertEqual(["example/build.gradle.kts:1: marked x-release-please-version, but carries 5.1.0, "
                              "the manifest says 6.0.0"], repo.problems())

    def test_marked_line_with_the_manifest_version_is_fine(self):
        extra = [extra_file("pom.xml"), extra_file("a/pom.xml"), "example/build.gradle.kts"]
        other = {"example/build.gradle.kts": 'val v = "6.0.0" // x-release-please-version\n'}
        with Repo(extra_files=extra, other_files=other) as repo:
            self.assertEqual([], repo.problems())

    def test_poms_below_build_output_are_ignored(self):
        with Repo(other_files={"a/target/pom.xml": pom(own="1.0", parent="1.0")}) as repo:
            self.assertEqual([], repo.problems())


class RepositoryTest(unittest.TestCase):

    def test_every_pom_of_this_repository_follows_the_manifest_version(self):
        self.assertEqual([], check.problems(HERE.parents[1]))


if __name__ == "__main__":
    unittest.main()
