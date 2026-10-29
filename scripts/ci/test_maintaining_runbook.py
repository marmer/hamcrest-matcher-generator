"""Guards for MAINTAINING.md (#137): the file exists and covers the required parts, the README links it,
and every credential check points its failure message at it (stdlib unittest only)."""
import importlib.util
import os
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
REPO = HERE.parents[1]
RUNBOOK_FILE = REPO / "MAINTAINING.md"
RUNBOOK_URL = "https://github.com/marmer/hamcrest-matcher-generator/blob/main/MAINTAINING.md"
CHECK_SCRIPTS = ("check-sonar-token.py", "check-central-token.py", "check-credential-expiry.py")
REQUIRED_HEADINGS = (
    "## Credentials",
    "## Playbooks",
    "### Red Renovate pull request",
    "### Red Release pull request",
    "### Failed publish",
    "### Expiring credential",
    "### Dependency that needs a newer Java",
    "### External service outage",
)
CREDENTIALS = ("SONAR_TOKEN", "MAVEN_GPG_KEY", "CENTRAL_USERNAME", "RELEASE_APP_PRIVATE_KEY", "RELEASE_APP_ID")


def load_check(filename):
    spec = importlib.util.spec_from_file_location(Path(filename).stem.replace("-", "_"), HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class MaintainingRunbookTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(RUNBOOK_FILE.is_file(), "MAINTAINING.md is missing at the repository root")
        self.text = RUNBOOK_FILE.read_text(encoding="utf-8")

    def test_required_headings_are_present(self):
        lines = self.text.splitlines()
        for heading in REQUIRED_HEADINGS:
            self.assertIn(heading, lines)

    def test_every_credential_has_a_table_row_with_a_storage_location(self):
        # Table columns after split("|"): [before, Credential, Used by, Storage location, Expiry, Renewal, after]
        rows = [line.split("|") for line in self.text.splitlines() if line.startswith("|")]
        self.assertIn("Credential", [cells[1].strip() for cells in rows if len(cells) > 6])
        for name in CREDENTIALS:
            with self.subTest(credential=name):
                matching = [cells for cells in rows if len(cells) > 6 and f"`{name}`" in cells[1]]
                self.assertTrue(matching, f"no credentials table row names `{name}`")
                self.assertTrue(any(cells[3].strip() for cells in matching),
                                f"the table row for `{name}` has no storage location")

    def test_readme_links_the_runbook(self):
        readme = (REPO / "README.md").read_text(encoding="utf-8")
        self.assertIn("(MAINTAINING.md)", readme)


class CredentialCheckRunbookLinkTest(unittest.TestCase):
    def test_credential_checks_link_the_runbook_file(self):
        for filename in CHECK_SCRIPTS:
            with self.subTest(script=filename):
                self.assertEqual(RUNBOOK_URL, load_check(filename).RUNBOOK)


if __name__ == "__main__":
    unittest.main()
