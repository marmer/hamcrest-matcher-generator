"""Unit tests for check-workflow-hardening.py (stdlib unittest only, no API calls) and a check of the repository's workflows."""
import importlib.util
import os
import tempfile
import textwrap
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("check_workflow_hardening", HERE / "check-workflow-hardening.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

SHA = "11d5960a326750d5838078e36cf38b85af677262"
REQUIRED_CHECK_MESSAGE = ["required-check workflows must run on `pull_request` and on `push` to main"]


def workflow(text):
    return textwrap.dedent(text).lstrip("\n")


class UnpinnedActionsTest(unittest.TestCase):

    def test_tag_reference_is_reported_with_its_line(self):
        text = workflow("""
            jobs:
              build:
                steps:
                  - uses: actions/checkout@v4
        """)
        self.assertEqual([(4, "actions/checkout@v4")], check.unpinned_actions(text))

    def test_full_commit_sha_with_version_comment_passes(self):
        text = workflow(f"""
            jobs:
              build:
                steps:
                  - uses: actions/checkout@{SHA} # v4.4.0
        """)
        self.assertEqual([], check.unpinned_actions(text))

    def test_short_sha_is_reported(self):
        text = "      - uses: actions/checkout@11d5960\n"
        self.assertEqual([(1, "actions/checkout@11d5960")], check.unpinned_actions(text))

    def test_branch_reference_is_reported(self):
        text = "        uses: ossf/scorecard-action@main\n"
        self.assertEqual([(1, "ossf/scorecard-action@main")], check.unpinned_actions(text))

    def test_action_without_ref_is_reported(self):
        text = "        uses: actions/checkout\n"
        self.assertEqual([(1, "actions/checkout")], check.unpinned_actions(text))

    def test_local_action_is_exempt(self):
        text = "      - uses: ./.github/actions/setup\n"
        self.assertEqual([], check.unpinned_actions(text))


class PermissionsTest(unittest.TestCase):

    def test_missing_top_level_permissions_is_reported(self):
        text = workflow("""
            name: CI-Build
            jobs:
              build:
                permissions:
                  contents: read
        """)
        self.assertEqual(["no top-level permissions block"], check.permissions_problems(text))

    def test_top_level_contents_read_passes(self):
        text = workflow("""
            name: Docs
            permissions:
              contents: read
            jobs:
              build:
                runs-on: ubuntu-latest
        """)
        self.assertEqual([], check.permissions_problems(text))

    def test_write_at_top_level_is_reported(self):
        text = workflow("""
            permissions:
              contents: read
              issues: write
            jobs:
              build:
                runs-on: ubuntu-latest
        """)
        self.assertEqual(["top-level permissions must be exactly `contents: read`, found {'contents': 'read', 'issues': 'write'}"],
                         check.permissions_problems(text))

    def test_read_all_shorthand_at_top_level_is_reported(self):
        text = "permissions: read-all\njobs: {}\n"
        self.assertEqual(["top-level permissions must be exactly `contents: read`, found 'read-all'"],
                         check.permissions_problems(text))

    def test_job_level_write_does_not_count_as_top_level(self):
        text = workflow("""
            permissions:
              contents: read
            jobs:
              publish:
                permissions:
                  id-token: write
        """)
        self.assertEqual([], check.permissions_problems(text))


class JavaCacheTest(unittest.TestCase):

    def test_setup_java_without_maven_cache_is_reported(self):
        text = workflow(f"""
            jobs:
              build:
                steps:
                  - uses: actions/setup-java@{SHA} # v4.9.1
                    with:
                      java-version: 21
                      distribution: temurin
        """)
        self.assertEqual(["line 4: setup-java without `cache: maven`"], check.java_cache_problems(text))

    def test_setup_java_with_maven_cache_passes(self):
        text = workflow(f"""
            jobs:
              build:
                steps:
                  - name: Set up JDK 21
                    uses: actions/setup-java@{SHA} # v4.9.1
                    with:
                      java-version: 21
                      distribution: temurin
                      cache: maven
                  - name: Build
                    run: ./mvnw -B verify
        """)
        self.assertEqual([], check.java_cache_problems(text))

    def test_setup_java_with_another_cache_is_reported(self):
        text = workflow(f"""
            jobs:
              build:
                steps:
                  - uses: actions/setup-java@{SHA} # v4.9.1
                    with:
                      java-version: 21
                      cache: gradle
        """)
        self.assertEqual(["line 4: setup-java without `cache: maven`"], check.java_cache_problems(text))

    def test_a_setup_java_step_without_maven_cache_is_reported_in_a_multi_step_job(self):
        text = workflow(f"""
            jobs:
              publish:
                steps:
                  - uses: actions/checkout@{SHA} # v4.4.0
                  - name: Set up JDK 21
                    uses: actions/setup-java@{SHA} # v4.9.1
                    with:
                      java-version: 21
                      distribution: temurin
                      cache: maven
                  - name: Set up JDK 25
                    uses: actions/setup-java@{SHA} # v4.9.1
                    with:
                      java-version: 25
                      distribution: temurin
                  - name: Deploy
                    run: ./mvnw -B -P release deploy
        """)
        self.assertEqual(["line 12: setup-java without `cache: maven`"], check.java_cache_problems(text))

    def test_manual_m2_cache_is_reported(self):
        text = workflow(f"""
            jobs:
              build:
                steps:
                  - name: Cache Maven packages
                    uses: actions/cache@{SHA} # v4.3.0
                    with:
                      path: ~/.m2
                      key: x
        """)
        self.assertEqual(["line 5: manual Maven cache (actions/cache on ~/.m2), use setup-java `cache: maven`"],
                         check.java_cache_problems(text))

    def test_sonar_cache_and_pit_history_cache_are_not_maven_caches(self):
        text = workflow(f"""
            jobs:
              sonar:
                steps:
                  - uses: actions/cache@{SHA} # v4.3.0
                    with:
                      path: ~/.sonar/cache
                      key: x
                  - uses: actions/cache/restore@{SHA} # v4.3.0
                    with:
                      path: target/pit-history
                      key: y
        """)
        self.assertEqual([], check.java_cache_problems(text))


class CiTriggerTest(unittest.TestCase):

    def test_pull_request_and_push_to_main_pass(self):
        text = workflow("""
            name: CI-Build
            on:
              push:
                branches: [ main ]
              pull_request:
            jobs:
              build:
                runs-on: ubuntu-latest
        """)
        self.assertEqual([], check.ci_trigger_problems(text))

    def test_missing_pull_request_trigger_is_reported(self):
        text = workflow("""
            on:
              push:
                branches: [ main ]
            jobs:
              build:
                runs-on: ubuntu-latest
        """)
        self.assertEqual(REQUIRED_CHECK_MESSAGE, check.ci_trigger_problems(text))

    def test_bare_push_on_every_branch_is_reported(self):
        text = workflow("""
            on:
              push:
              pull_request:
            jobs:
              build:
                runs-on: ubuntu-latest
        """)
        self.assertEqual(REQUIRED_CHECK_MESSAGE, check.ci_trigger_problems(text))

    def test_required_check_workflows_with_bare_push_are_reported_in_a_root(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            workflows = root / ".github" / "workflows"
            workflows.mkdir(parents=True)
            pinned = f"    steps:\n      - uses: actions/checkout@{SHA} # v4.4.0\n"
            permissions = "permissions:\n  contents: read\n\n"
            (workflows / "domain-docs-sync.yml").write_text(
                "on:\n  push:\n  pull_request:\n\n" + permissions + "jobs:\n  j:\n    runs-on: x\n" + pinned,
                encoding="utf-8")
            (workflows / "docs.yml").write_text(
                "on:\n  push:\n  pull_request:\n\n" + permissions + "jobs:\n  j:\n    runs-on: x\n" + pinned,
                encoding="utf-8")
            problems = check.check_workflows(root)
        self.assertEqual(
            [f".github/workflows/domain-docs-sync.yml: {REQUIRED_CHECK_MESSAGE[0]}"],
            problems)

    def test_domain_docs_sync_runs_on_pull_requests_and_push_to_main_only(self):
        text = (HERE.parents[1] / ".github" / "workflows" / "domain-docs-sync.yml").read_text(encoding="utf-8")
        self.assertIn("name: \"Domain docs sync\"", text)
        self.assertIn("name: Domain docs in sync", text)
        self.assertEqual([], check.ci_trigger_problems(text))


class RepositoryWorkflowsTest(unittest.TestCase):

    def test_repository_workflows_are_pinned_and_minimal(self):
        self.assertEqual([], check.check_workflows(HERE.parents[1]))

    def test_scorecard_is_weekly_publishes_results_and_grants_only_its_job_permissions(self):
        text = (HERE.parents[1] / ".github" / "workflows" / "scorecard.yml").read_text(encoding="utf-8")
        self.assertIn("publish_results: true", text)
        self.assertIn("push:\n    branches: [ main ]", text)
        self.assertRegex(text, r"schedule:\n\s+- cron: \"[0-9*]+ [0-9*]+ \* \* [0-7]\"")
        self.assertIn("    permissions:\n      security-events: write\n      id-token: write", text)
        self.assertIn("continue-on-error: true", text)

    def test_dependency_review_blocks_any_severity_and_disallowed_licenses(self):
        text = (HERE.parents[1] / ".github" / "workflows" / "dependency-review.yml").read_text(encoding="utf-8")
        self.assertIn("name: Dependency review", text)
        self.assertIn("fail-on-severity: low", text)
        self.assertIn("allow-licenses:", text)
        self.assertNotIn("continue-on-error", text)

    def test_scanoss_runs_on_pull_requests_and_weekly_and_is_non_blocking(self):
        text = (HERE.parents[1] / ".github" / "workflows" / "scanoss.yml").read_text(encoding="utf-8")
        self.assertIn("pull_request:", text)
        self.assertIn("schedule:", text)
        self.assertIn("continue-on-error: true", text)
        self.assertIn("GITHUB_STEP_SUMMARY", text)

    def test_scanoss_skips_build_output_and_agent_tooling_and_scans_the_absolute_workspace(self):
        text = (HERE.parents[1] / ".github" / "workflows" / "scanoss.yml").read_text(encoding="utf-8")
        for folder in ("target", ".agents", ".claude", ".git"):
            self.assertIn(f"--skip-folder {folder}", text)
        self.assertIn('scan --output scanoss-results.json --skip-folder target', text)
        self.assertIn('"$GITHUB_WORKSPACE"', text)

    def test_dependency_review_runs_the_test_scope_step_even_when_the_first_failed_and_still_blocks(self):
        text = (HERE.parents[1] / ".github" / "workflows" / "dependency-review.yml").read_text(encoding="utf-8")
        test_step = text.index("name: Test scope")
        self.assertIn("if: success() || failure()", text[test_step:])
        self.assertNotIn("continue-on-error", text)

    def test_dependency_review_allows_exactly_the_two_junit_purls_in_the_test_scope_only(self):
        text = (HERE.parents[1] / ".github" / "workflows" / "dependency-review.yml").read_text(encoding="utf-8")
        test_step = text.index("name: Test scope")
        allow_lines = [line.strip() for line in text.splitlines() if line.strip().startswith("allow-dependencies-licenses:")]
        self.assertEqual(allow_lines, [
            "allow-dependencies-licenses: pkg:maven/org.junit.jupiter/junit-jupiter, pkg:maven/org.junit.platform/junit-platform-launcher",
        ])
        self.assertGreater(text.index(allow_lines[0]), test_step)


if __name__ == "__main__":
    unittest.main()
