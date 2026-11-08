"""Tests for scripts/agents/fast-forward-main.sh and the SessionStart hook that calls it (stdlib unittest only).

Every test builds a bare origin, a seed that pushes to it and a clone of it in a scratch directory, so no test
touches a real checkout or the network. The script and the hook are committed into each scratch repository.
"""
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
REPO = HERE.parents[1]
SCRIPT = REPO / "scripts" / "agents" / "fast-forward-main.sh"
HOOK = REPO / ".claude" / "hooks" / "session-start.sh"


def git(cwd, *args):
    return subprocess.run(["git", *args], cwd=cwd, check=True, capture_output=True, text=True).stdout.strip()


def commit(repo, name, text):
    (repo / name).write_text(text, encoding="utf-8")
    git(repo, "add", name)
    git(repo, "commit", "-q", "-m", f"add {name}")


class FastForwardMainTest(unittest.TestCase):

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        root = Path(self.directory.name)
        self.origin = root / "origin.git"
        self.seed = root / "seed"
        self.clone = root / "clone"
        self.root = root
        git(root, "init", "--bare", "-q", "-b", "main", str(self.origin))
        git(root, "init", "-q", "-b", "main", str(self.seed))
        git(self.seed, "config", "user.name", "Test")
        git(self.seed, "config", "user.email", "test@example.invalid")
        git(self.seed, "remote", "add", "origin", str(self.origin))
        for relative in ("scripts/agents/fast-forward-main.sh", ".claude/hooks/session-start.sh"):
            target = self.seed / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(REPO / relative, target)
            target.chmod(0o755)
            git(self.seed, "add", relative)
        commit(self.seed, "README.md", "first\n")
        git(self.seed, "push", "-q", "origin", "main")
        git(root, "clone", "-q", str(self.origin), str(self.clone))
        git(self.clone, "config", "user.name", "Test")
        git(self.clone, "config", "user.email", "test@example.invalid")

    def tearDown(self):
        self.directory.cleanup()

    def advance_origin(self):
        """Pushes one commit to origin/main from the seed, so the clone is one commit behind."""
        commit(self.seed, "upstream.txt", "upstream\n")
        git(self.seed, "push", "-q", "origin", "main")
        return git(self.seed, "rev-parse", "HEAD")

    def run_script(self, cwd=None):
        env = {k: v for k, v in os.environ.items() if k not in ("CLAUDE_CODE_REMOTE", "CLAUDE_PROJECT_DIR")}
        return subprocess.run(["bash", str(SCRIPT)], cwd=cwd or self.clone, capture_output=True, text=True,
                              env=env, timeout=120)

    def assert_one_status_line(self, result):
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stderr, "")
        lines = result.stdout.strip().splitlines()
        self.assertEqual(len(lines), 1, result.stdout)
        return lines[0]

    def test_behind_and_clean_main_is_fast_forwarded(self):
        new_sha = self.advance_origin()
        old_sha = git(self.clone, "rev-parse", "main")

        result = self.run_script()

        line = self.assert_one_status_line(result)
        self.assertIn("fast-forwarded", line)
        self.assertNotEqual(old_sha, new_sha)
        self.assertEqual(git(self.clone, "rev-parse", "main"), new_sha)
        self.assertEqual(git(self.clone, "symbolic-ref", "--short", "HEAD"), "main")
        self.assertEqual(git(self.clone, "status", "--porcelain"), "")

    def test_dirty_tracked_file_leaves_main_untouched(self):
        self.advance_origin()
        old_sha = git(self.clone, "rev-parse", "main")
        (self.clone / "README.md").write_text("local edit\n", encoding="utf-8")

        result = self.run_script()

        line = self.assert_one_status_line(result)
        self.assertIn("behind", line)
        self.assertIn("working tree dirty", line)
        self.assertEqual(git(self.clone, "rev-parse", "main"), old_sha)
        self.assertEqual((self.clone / "README.md").read_text(encoding="utf-8"), "local edit\n")

    def test_untracked_file_leaves_main_untouched(self):
        self.advance_origin()
        old_sha = git(self.clone, "rev-parse", "main")
        (self.clone / "notes.txt").write_text("scratch\n", encoding="utf-8")

        result = self.run_script()

        line = self.assert_one_status_line(result)
        self.assertIn("working tree dirty", line)
        self.assertEqual(git(self.clone, "rev-parse", "main"), old_sha)
        self.assertTrue((self.clone / "notes.txt").exists())

    def test_other_branch_leaves_main_and_the_branch_untouched(self):
        self.advance_origin()
        git(self.clone, "checkout", "-q", "-b", "feature")
        old_sha = git(self.clone, "rev-parse", "main")
        feature_sha = git(self.clone, "rev-parse", "HEAD")

        result = self.run_script()

        line = self.assert_one_status_line(result)
        self.assertIn("current branch is feature, not main", line)
        self.assertEqual(git(self.clone, "symbolic-ref", "--short", "HEAD"), "feature")
        self.assertEqual(git(self.clone, "rev-parse", "HEAD"), feature_sha)
        self.assertEqual(git(self.clone, "rev-parse", "main"), old_sha)

    def test_detached_head_leaves_main_untouched(self):
        self.advance_origin()
        git(self.clone, "checkout", "-q", "--detach")
        old_sha = git(self.clone, "rev-parse", "main")

        result = self.run_script()

        line = self.assert_one_status_line(result)
        self.assertIn("HEAD is detached", line)
        self.assertEqual(git(self.clone, "rev-parse", "main"), old_sha)

    def test_diverged_main_leaves_main_untouched(self):
        commit(self.clone, "local.txt", "local\n")
        local_sha = git(self.clone, "rev-parse", "main")
        self.advance_origin()

        result = self.run_script()

        line = self.assert_one_status_line(result)
        self.assertIn("diverged", line)
        self.assertEqual(git(self.clone, "rev-parse", "main"), local_sha)

    def test_main_ahead_of_origin_leaves_main_untouched(self):
        commit(self.clone, "local.txt", "local\n")
        local_sha = git(self.clone, "rev-parse", "main")

        result = self.run_script()

        line = self.assert_one_status_line(result)
        self.assertIn("nothing to fast-forward", line)
        self.assertEqual(git(self.clone, "rev-parse", "main"), local_sha)

    def test_up_to_date_main_says_so(self):
        old_sha = git(self.clone, "rev-parse", "main")

        result = self.run_script()

        line = self.assert_one_status_line(result)
        self.assertIn("up to date", line)
        self.assertEqual(git(self.clone, "rev-parse", "main"), old_sha)

    def test_unreachable_remote_exits_zero_and_leaves_main_untouched(self):
        old_sha = git(self.clone, "rev-parse", "main")
        git(self.clone, "remote", "set-url", "origin", str(self.root / "missing.git"))

        result = self.run_script()

        line = self.assert_one_status_line(result)
        self.assertIn("could not fetch origin", line)
        self.assertEqual(git(self.clone, "rev-parse", "main"), old_sha)

    def test_directory_that_is_not_a_checkout_exits_zero(self):
        plain = self.root / "plain"
        plain.mkdir()

        result = self.run_script(cwd=plain)

        line = self.assert_one_status_line(result)
        self.assertIn("not a git checkout", line)

    def test_session_start_hook_fast_forwards_in_a_local_session(self):
        new_sha = self.advance_origin()
        env = {k: v for k, v in os.environ.items() if k not in ("CLAUDE_CODE_REMOTE", "CLAUDE_PROJECT_DIR")}
        env["CLAUDE_PROJECT_DIR"] = str(self.clone)

        result = subprocess.run(["bash", str(self.clone / ".claude" / "hooks" / "session-start.sh")],
                                cwd=self.root, capture_output=True, text=True, env=env, timeout=120)

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("fast-forwarded", result.stdout)
        self.assertEqual(git(self.clone, "rev-parse", "main"), new_sha)


if __name__ == "__main__":
    unittest.main()
