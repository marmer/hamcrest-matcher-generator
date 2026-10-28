"""Unit tests for scripts/agents/verify.sh, release step (stdlib unittest only).

The script is copied into a scratch repository with a fake ./mvnw and a fake scripts/ci/check-release-artifacts.sh,
so no Maven build runs.
"""
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
REPO = HERE.parents[1]

FAKE_MVNW = """#!/usr/bin/env bash
echo "args: $*"
echo "[INFO] BUILD ${MVNW_EXIT_NOTE:-SUCCESS}"
exit "${MVNW_EXIT:-0}"
"""

FAKE_CHECK = """#!/usr/bin/env bash
echo "$*" >> "$(dirname "$0")/../../check-calls.txt"
exit 0
"""


def write_executable(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    path.chmod(0o755)


class ReleaseStepTest(unittest.TestCase):

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        (self.root / "scripts" / "agents").mkdir(parents=True)
        shutil.copy2(REPO / "scripts" / "agents" / "verify.sh", self.root / "scripts" / "agents" / "verify.sh")
        write_executable(self.root / "mvnw", FAKE_MVNW)
        write_executable(self.root / "scripts" / "ci" / "check-release-artifacts.sh", FAKE_CHECK)

    def tearDown(self):
        self.directory.cleanup()

    def run_release(self, mvnw_exit):
        env = dict(os.environ, VERIFY_LOG_DIR=str(self.root / "logs"), MVNW_EXIT=str(mvnw_exit))
        return subprocess.run(
            ["bash", str(self.root / "scripts" / "agents" / "verify.sh"), "--only", "release"],
            cwd=self.root, env=env, capture_output=True, text=True)

    def test_failed_dry_run_fails_the_step_and_skips_the_artifact_check(self):
        result = self.run_release(mvnw_exit=1)

        self.assertNotEqual(0, result.returncode, result.stdout)
        self.assertIn("FAILED release", result.stdout)
        self.assertFalse((self.root / "check-calls.txt").exists(),
                         "the artifact check ran although the dry run failed")

    def test_dry_run_is_teed_into_the_release_log_and_the_check_reads_that_log(self):
        result = self.run_release(mvnw_exit=0)

        self.assertEqual(0, result.returncode, result.stdout)
        self.assertIn("ok release", result.stdout)
        log = (self.root / "release-dry-run.log").read_text(encoding="utf-8")
        self.assertIn("args: -B -P release verify -DskipTests -Dgpg.skip=true", log)
        self.assertIn("[INFO] BUILD SUCCESS", log)
        self.assertEqual("release-dry-run.log\n", (self.root / "check-calls.txt").read_text(encoding="utf-8"))


FRESH_MVNW = """#!/usr/bin/env bash
echo "args: $*"
for a in "$@"; do
  case "$a" in -Dmaven.repo.local=*) repo="${a#-Dmaven.repo.local=}" ;; esac
done
echo "$repo" >> "$MVNW_RECORD"
(cd "$repo" && find . -type f | sort) >> "$MVNW_RECORD"
case " $* " in *" -o "*) if [ -n "${MVNW_OFFLINE_FAIL:-}" ]; then echo "in offline mode"; exit 1; fi ;; esac
exit "${MVNW_EXIT:-0}"
"""


class FreshRepoStepTest(unittest.TestCase):

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        (self.root / "scripts" / "agents").mkdir(parents=True)
        shutil.copy2(REPO / "scripts" / "agents" / "verify.sh", self.root / "scripts" / "agents" / "verify.sh")
        write_executable(self.root / "mvnw", FRESH_MVNW)
        self.seed = self.root / "seed"
        for name in ("org/example/lib/1/lib-1.jar", "io/github/marmer/testutils/x/x-1.jar"):
            (self.seed / name).parent.mkdir(parents=True)
            (self.seed / name).write_text("jar", encoding="utf-8")
        self.record = self.root / "record.txt"

    def tearDown(self):
        self.directory.cleanup()

    def run_fresh(self, **extra):
        env = dict(os.environ, VERIFY_LOG_DIR=str(self.root / "logs"), VERIFY_SEED_REPO=str(self.seed),
                   MVNW_RECORD=str(self.record), **extra)
        return subprocess.run(
            ["bash", str(self.root / "scripts" / "agents" / "verify.sh"), "--only", "fresh-repo"],
            cwd=self.root, env=env, capture_output=True, text=True)

    def test_verify_runs_on_a_seeded_copy_without_the_project_artifacts_and_cleans_up(self):
        result = self.run_fresh()

        self.assertEqual(0, result.returncode, result.stdout)
        self.assertIn("ok fresh-repo", result.stdout)
        lines = self.record.read_text(encoding="utf-8").splitlines()
        repo = Path(lines[0])
        self.assertNotEqual(self.seed, repo)
        self.assertIn("./org/example/lib/1/lib-1.jar", lines)
        self.assertFalse(any("testutils" in line for line in lines), lines)
        self.assertFalse(repo.exists(), "the throw-away repository was not removed")
        self.assertTrue((self.seed / "io/github/marmer/testutils/x/x-1.jar").exists(), "the seed was touched")
        log = (self.root / "logs" / "fresh-repo.log").read_text(encoding="utf-8")
        self.assertIn("args: -o -B clean verify -Dmaven.repo.local=", log)

    def test_failed_build_fails_the_step_and_still_cleans_up(self):
        result = self.run_fresh(MVNW_EXIT="1")

        self.assertNotEqual(0, result.returncode, result.stdout)
        self.assertIn("FAILED fresh-repo", result.stdout)
        self.assertFalse(Path(self.record.read_text(encoding="utf-8").splitlines()[0]).exists())

    def test_offline_gap_falls_back_to_an_online_build(self):
        result = self.run_fresh(MVNW_OFFLINE_FAIL="1")

        self.assertEqual(0, result.returncode, result.stdout)
        log = (self.root / "logs" / "fresh-repo.log").read_text(encoding="utf-8")
        self.assertIn("args: -B clean verify -Dmaven.repo.local=", log)


if __name__ == "__main__":
    unittest.main()
