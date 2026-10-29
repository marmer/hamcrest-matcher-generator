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


class CommandLineTest(SnippetTestCase):
    def run_script(self, *args):
        return subprocess.run([sys.executable, str(SCRIPT), *args], capture_output=True, text=True)

    def test_check_exits_1_and_prints_the_drift(self):
        repo = self.make_repo(example=EXAMPLE.replace('withName("Ada")', 'withName("Bob")'))

        result = self.run_script("--check", str(repo.root))

        self.assertEqual(1, result.returncode)
        self.assertIn("README.md:1: snippet", result.stdout)

    def test_check_passes_on_an_in_sync_readme(self):
        repo = self.make_repo()

        result = self.run_script("--check", str(repo.root))

        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_without_check_the_readme_is_rewritten(self):
        repo = self.make_repo(readme=Repo.block("example-x/src/test/java/Demo.java", "basic", body="old\n"))

        result = self.run_script(str(repo.root))

        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual([], sync.problems(repo.root))


class RepositoryReadmeTest(unittest.TestCase):
    def test_the_repository_readme_is_in_sync_with_the_example_regions(self):
        self.assertEqual([], sync.problems(REPO_ROOT))

    def test_the_repository_readme_carries_the_snippets(self):
        self.assertGreaterEqual(sync.directive_count(REPO_ROOT), 8)


if __name__ == "__main__":
    unittest.main()
