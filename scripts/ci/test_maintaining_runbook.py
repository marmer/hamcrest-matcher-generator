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
    spec = importlib.util.spec_from_file_location(filename.replace("-", "_")[:-3], HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class MaintainingRunbookTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(RUNBOOK_FILE.is_file(), "MAINTAINING.md is missing at the repository root")
        self.text = RUNBOOK_FILE.read_text(encoding="utf-8")

    def test_required_headings_are_present(self):
        for heading in REQUIRED_HEADINGS:
            self.assertIn(heading, self.text)

    def test_credentials_table_names_every_credential_and_its_storage(self):
        for name in CREDENTIALS:
            self.assertIn(f"`{name}`", self.text)
        self.assertIn("| Credential |", self.text)

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
