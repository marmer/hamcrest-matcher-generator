"""Unit tests for rule 6 of check-domain-docs-sync.py (stdlib unittest only; fixtures in a temp dir)."""
import importlib.util
import os
import tempfile
import unittest
from pathlib import Path

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("check_domain_docs_sync", os.path.join(HERE, "check-domain-docs-sync.py"))
check = importlib.util.module_from_spec(spec)
import sys  # noqa: E402

sys.path.insert(0, HERE)
spec.loader.exec_module(check)


class SpiTypesNamedTest(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.root = Path(self._tmp.name)
        self.spi = self.root / check.CORE_MAIN_SOURCES / "a" / "b" / "spi"
        self.spi.mkdir(parents=True)

    def chapter(self, text):
        path = self.root / check.BUILDING_BLOCKS_ADOC
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def violations(self):
        found = []
        check.check_spi_types_named(self.root, found)
        return found

    def test_named_type_passes(self):
        (self.spi / "Backend.java").write_text("public interface Backend {}\n")
        self.chapter("The `Backend` interface.")
        self.assertEqual([], self.violations())

    def test_unnamed_type_fails_with_message(self):
        (self.spi / "Backend.java").write_text("public interface Backend {}\n")
        self.chapter("Nothing here.")
        self.assertEqual(
            [f"{check.BUILDING_BLOCKS_ADOC}: SPI type 'Backend' from "
             f"{check.CORE_MAIN_SOURCES}/a/b/spi/Backend.java is not named"],
            self.violations())

    def test_type_in_nested_sub_package_is_found(self):
        nested = self.spi / "deep"
        nested.mkdir()
        (nested / "Inner.java").write_text("public record Inner(int x) {}\n")
        self.chapter("Nothing here.")
        self.assertEqual(1, len(self.violations()))

    def test_non_sealed_class_is_found(self):
        (self.spi / "Open.java").write_text("public non-sealed class Open extends Base {}\n")
        self.chapter("Nothing here.")
        self.assertEqual(1, len(self.violations()))


if __name__ == "__main__":
    unittest.main()
