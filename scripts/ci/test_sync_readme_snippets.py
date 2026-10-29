"""Unit tests for sync-readme-snippets.py (stdlib unittest only; fixtures in a temp dir) and a check of the README."""
import importlib.util
import os
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("sync_readme_snippets", HERE / "sync-readme-snippets.py")
sync = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sync)

SCRIPT = HERE / "sync-readme-snippets.py"
EXAMPLE_PATH = "example-x/src/test/java/Demo.java"
# The repository this test file belongs to, derived from its location and never from the working directory.
REPO_ROOT = HERE.parents[1]

EXAMPLE = textwrap.dedent("""\
    class Demo {
        void run() {
            // tag::basic[]
            assertThat(ADA, isCustomer().withName("Ada"));
            // end::basic[]
        }
    }
    """)


class Repo:
    """A throw-away repository tree with an example file and a README."""

    def __init__(self, example=EXAMPLE, readme=None):
        self.dir = tempfile.TemporaryDirectory()
        self.root = Path(self.dir.name)
        self.write("example-x/src/test/java/Demo.java", example)
        self.write("README.md", readme if readme is not None else self.block("example-x/src/test/java/Demo.java", "basic"))

    def write(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def read(self, name):
        return (self.root / name).read_text(encoding="utf-8")

    def close(self):
        self.dir.cleanup()

    @staticmethod
    def block(path, tag, body="assertThat(ADA, isCustomer().withName(\"Ada\"));\n"):
        """A README directive with a snippet block (the body is given as it reads in the README)."""
        return (f"<!-- snippet: {path}#{tag} -->\n"
                "```java\n"
                f"{body}"
                "```\n"
                "<!-- end snippet -->\n")


class SnippetTestCase(unittest.TestCase):
    def setUp(self):
        self.repo = None

    def tearDown(self):
        if self.repo is not None:
            self.repo.close()

    def make_repo(self, **kwargs):
        self.repo = Repo(**kwargs)
        return self.repo


class ExtractRegionTest(SnippetTestCase):
    def test_returns_the_lines_between_the_tags_without_them(self):
        text = "x\n  // tag::a[]\n  one();\n  two();\n  // end::a[]\ny\n"

        self.assertEqual(["one();", "two();"], sync.extract_region(text, "a"))

    def test_dedents_by_the_least_indented_line(self):
        text = "    // tag::a[]\n        deep();\n    shallow();\n    // end::a[]\n"

        self.assertEqual(["    deep();", "shallow();"], sync.extract_region(text, "a"))

    def test_tabs_are_expanded_before_the_dedent(self):
        text = "// tag::a[]\n\t\tx();\n    y();\n// end::a[]\n"

        self.assertEqual(["    x();", "y();"], sync.extract_region(text, "a"))

    def test_a_tag_inside_an_xml_comment_is_a_tag(self):
        text = "<!-- tag::a[] -->\n<dependency/>\n<!-- end::a[] -->\n"

        self.assertEqual(["<dependency/>"], sync.extract_region(text, "a"))

    def test_a_missing_region_is_an_error(self):
        with self.assertRaises(sync.SnippetError) as raised:
            sync.extract_region("// tag::other[]\n// end::other[]\n", "a")

        self.assertIn("no region a", str(raised.exception))

    def test_a_region_given_twice_is_an_error(self):
        text = "// tag::a[]\nx\n// end::a[]\n// tag::a[]\ny\n// end::a[]\n"

        with self.assertRaises(sync.SnippetError) as raised:
            sync.extract_region(text, "a")

        self.assertIn("more than one region a", str(raised.exception))


class LanguageTest(unittest.TestCase):
    def test_the_fence_language_follows_the_file_extension(self):
        self.assertEqual("xml", sync.fence_language("pom.xml"))
        self.assertEqual("java", sync.fence_language("Demo.java"))
        self.assertEqual("kotlin", sync.fence_language("Demo.kt"))
        self.assertEqual("kotlin", sync.fence_language("build.gradle.kts"))

    def test_an_unknown_extension_is_an_error(self):
        with self.assertRaises(sync.SnippetError):
            sync.fence_language("notes.txt")


class CheckModeTest(SnippetTestCase):
    def test_an_in_sync_readme_passes(self):
        repo = self.make_repo()

        self.assertEqual([], sync.problems(repo.root))

    def test_a_changed_example_region_is_out_of_sync_with_its_line(self):
        repo = self.make_repo(example=EXAMPLE.replace('withName("Ada")', 'withName("Bob")'))

        found = sync.problems(repo.root)

        self.assertEqual(1, len(found))
        self.assertTrue(found[0].startswith("README.md:1: "), found[0])
        self.assertIn("example-x/src/test/java/Demo.java#basic", found[0])
        self.assertIn("out of sync", found[0])

    def test_an_edited_readme_block_is_out_of_sync(self):
        repo = self.make_repo(readme=Repo.block("example-x/src/test/java/Demo.java", "basic", body="hand written\n"))

        found = sync.problems(repo.root)

        self.assertEqual(["README.md:1: snippet example-x/src/test/java/Demo.java#basic is out of sync with its "
                          "example region; run scripts/ci/sync-readme-snippets.py"], found)

    def test_a_directive_without_its_end_marker_is_an_error_with_its_line(self):
        repo = self.make_repo(readme="intro\n<!-- snippet: example-x/src/test/java/Demo.java#basic -->\n```java\n")

        found = sync.problems(repo.root)

        self.assertEqual(["README.md:2: snippet directive has no <!-- end snippet --> line"], found)

    def test_a_directive_naming_a_missing_region_is_an_error_with_its_line(self):
        repo = self.make_repo(readme=Repo.block("example-x/src/test/java/Demo.java", "nope"))

        found = sync.problems(repo.root)

        self.assertEqual(1, len(found))
        self.assertTrue(found[0].startswith("README.md:1: "), found[0])
        self.assertIn("no region nope", found[0])

    def test_text_outside_the_directives_is_not_checked(self):
        repo = self.make_repo(readme="# Title\n\nAny prose, any `code`.\n\n" + Repo.block("example-x/src/test/java/Demo.java", "basic"))

        self.assertEqual([], sync.problems(repo.root))


class WriteModeTest(SnippetTestCase):
    def test_write_rewrites_a_stale_block_to_the_region(self):
        repo = self.make_repo(example=EXAMPLE.replace('withName("Ada")', 'withName("Bob")'))

        sync.write(repo.root)

        self.assertEqual(Repo.block("example-x/src/test/java/Demo.java", "basic",
                                    body='assertThat(ADA, isCustomer().withName("Bob"));\n'),
                         repo.read("README.md"))
        self.assertEqual([], sync.problems(repo.root))

    def test_write_keeps_the_text_around_the_block(self):
        repo = self.make_repo(readme="# Title\n\n" + Repo.block("example-x/src/test/java/Demo.java", "basic", body="old\n")
                              + "\nTail\n")

        sync.write(repo.root)

        self.assertEqual("# Title\n\n" + Repo.block("example-x/src/test/java/Demo.java", "basic")
                         + "\nTail\n", repo.read("README.md"))

    def test_write_does_not_touch_a_readme_that_is_in_sync(self):
        repo = self.make_repo()
        before = repo.read("README.md")

        sync.write(repo.root)

        self.assertEqual(before, repo.read("README.md"))


def run_script(*args):
    return subprocess.run([sys.executable, str(SCRIPT), *args], capture_output=True, text=True)


class CommandLineTest(SnippetTestCase):

    def test_check_exits_1_and_prints_the_drift(self):
        repo = self.make_repo(example=EXAMPLE.replace('withName("Ada")', 'withName("Bob")'))

        result = run_script("--check", str(repo.root))

        self.assertEqual(1, result.returncode)
        self.assertIn("README.md:1: snippet", result.stdout)

    def test_check_passes_on_an_in_sync_readme(self):
        repo = self.make_repo()

        result = run_script("--check", str(repo.root))

        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_without_check_the_readme_is_rewritten(self):
        repo = self.make_repo(readme=Repo.block("example-x/src/test/java/Demo.java", "basic", body="old\n"))

        result = run_script(str(repo.root))

        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual([], sync.problems(repo.root))


class LineEndingTest(SnippetTestCase):
    def test_a_crlf_readme_is_checked_like_an_lf_one(self):
        readme = Repo.block(EXAMPLE_PATH, "basic", body="hand written\n").replace("\n", "\r\n")
        repo = self.make_repo(readme=readme)

        found = sync.problems(repo.root)

        self.assertEqual(1, len(found))
        self.assertTrue(found[0].startswith("README.md:1: snippet "), found[0])

    def test_write_keeps_crlf_line_endings(self):
        repo = self.make_repo(readme=Repo.block(EXAMPLE_PATH, "basic", body="old\n").replace("\n", "\r\n"))

        sync.write(repo.root)

        self.assertEqual(Repo.block(EXAMPLE_PATH, "basic").replace("\n", "\r\n").encode("utf-8"),
                         (repo.root / "README.md").read_bytes())


class NoDirectiveTest(SnippetTestCase):
    def test_a_readme_without_any_directive_fails_the_check(self):
        repo = self.make_repo(readme="# Title\n\nNo snippets here.\n")

        self.assertEqual(["README.md: no snippet directive found"], sync.problems(repo.root))


class ReadErrorTest(SnippetTestCase):
    def test_a_missing_readme_is_one_line_naming_it_and_exit_1(self):
        repo = self.make_repo()
        (repo.root / "README.md").unlink()

        result = run_script("--check", str(repo.root))

        self.assertEqual(1, result.returncode)
        self.assertEqual(1, len(result.stderr.strip().splitlines()), result.stderr)
        self.assertTrue(result.stderr.startswith("README.md: cannot be read"), result.stderr)

    def test_an_undecodable_readme_is_one_line_naming_it_and_exit_1(self):
        repo = self.make_repo()
        (repo.root / "README.md").write_bytes(b"\xff\xfe# Title\n")

        result = run_script("--check", str(repo.root))

        self.assertEqual(1, result.returncode)
        self.assertEqual(1, len(result.stderr.strip().splitlines()), result.stderr)
        self.assertTrue(result.stderr.startswith("README.md: not UTF-8 text"), result.stderr)

    def test_a_missing_example_file_is_reported_with_its_name_and_line(self):
        repo = self.make_repo()
        (repo.root / EXAMPLE_PATH).unlink()

        self.assertEqual([f"README.md:1: {EXAMPLE_PATH}: cannot be read (No such file or directory)"],
                         sync.problems(repo.root))

    def test_an_undecodable_example_file_is_reported_with_its_name_and_line(self):
        repo = self.make_repo()
        (repo.root / EXAMPLE_PATH).write_bytes(b"\xff\xfe")

        self.assertEqual([f"README.md:1: {EXAMPLE_PATH}: not UTF-8 text"], sync.problems(repo.root))


class PathTest(SnippetTestCase):
    def test_a_relative_path_above_the_repository_is_rejected(self):
        with tempfile.TemporaryDirectory() as outer:
            root = Path(outer) / "repo"
            root.mkdir()
            (Path(outer) / "Demo.java").write_text(EXAMPLE, encoding="utf-8")
            (root / "README.md").write_text(Repo.block("../Demo.java", "basic"), encoding="utf-8")

            self.assertEqual(["README.md:1: ../Demo.java must stay inside the repository"], sync.problems(root))

    def test_an_absolute_path_is_rejected(self):
        with tempfile.TemporaryDirectory() as outer:
            root = Path(outer) / "repo"
            root.mkdir()
            demo = Path(outer) / "Demo.java"
            demo.write_text(EXAMPLE, encoding="utf-8")
            (root / "README.md").write_text(Repo.block(str(demo), "basic"), encoding="utf-8")

            self.assertEqual([f"README.md:1: {demo} must stay inside the repository"], sync.problems(root))


class ResultTest(SnippetTestCase):
    def test_problems_list_both_the_stale_blocks_and_the_broken_directives(self):
        repo = self.make_repo(readme=Repo.block(EXAMPLE_PATH, "basic", body="hand\n") + Repo.block(EXAMPLE_PATH, "nope"))

        found = sync.problems(repo.root)

        self.assertEqual(2, len(found), found)
        self.assertIn("out of sync", found[0])
        self.assertIn("no region nope", found[1])


class RepositoryReadmeTest(unittest.TestCase):
    def test_the_repository_readme_is_in_sync_with_the_example_regions(self):
        self.assertEqual([], sync.problems(REPO_ROOT))

    def test_the_repository_readme_carries_the_snippets(self):
        expected = sorted([
            ("example-maven-java-hamcrest/src/test/java/example/hamcrest/CustomerMatcherTest.java", "nested-property"),
            ("example-maven-java-assertj/src/test/java/example/assertj/CustomerAssertTest.java", "basic-assertion"),
            ("example-maven-kotlin/src/test/kotlin/example/kotlin/CustomerDslTest.kt", "basic-assertion"),
            ("example-maven-java-hamcrest/src/main/java/example/hamcrest/configuration/HamcrestConfiguration.java",
             "configuration"),
            ("example-maven-java-hamcrest/pom.xml", "maven-dependencies"),
            ("example-maven-java-hamcrest/pom.xml", "maven-processor"),
            ("example-maven-java-assertj/pom.xml", "maven-dependencies"),
            ("example-maven-java-assertj/pom.xml", "maven-processor"),
            ("example-gradle-java/build.gradle.kts", "gradle-version"),
            ("example-gradle-java/build.gradle.kts", "gradle-dependencies"),
            ("example-gradle-java/build.gradle.kts", "gradle-processor"),
        ])
        self.assertEqual(expected, sorted(sync.directive_pairs(REPO_ROOT)))


if __name__ == "__main__":
    unittest.main()
