"""Unit tests for sonar-issues.py (stdlib unittest only, no API calls)."""
import importlib.util
import os
import unittest
import urllib.parse

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("sonar_issues", os.path.join(HERE, "sonar-issues.py"))
sonar = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sonar)

PROJECT = "io.github.marmer.testutils:matcher-generator-parent"


def issue(n):
    return {"rule": "java:S6204", "severity": "MINOR", "component": f"{PROJECT}:core/src/A{n}.java",
            "line": n, "message": f"message {n}"}


def query(url):
    return urllib.parse.parse_qs(urllib.parse.urlparse(url).query)


class UrlTest(unittest.TestCase):

    def test_pull_request_url_asks_for_open_issues_of_that_pull_request(self):
        q = query(sonar.issues_url(pull_request="12", branch=None, page=1))

        self.assertEqual([PROJECT], q["componentKeys"])
        self.assertEqual(["12"], q["pullRequest"])
        self.assertEqual(["false"], q["resolved"])
        self.assertEqual(["500"], q["ps"])
        self.assertEqual(["1"], q["p"])
        self.assertNotIn("branch", q)

    def test_branch_url_names_the_branch(self):
        q = query(sonar.issues_url(pull_request=None, branch="feature", page=2))

        self.assertEqual(["feature"], q["branch"])
        self.assertEqual(["2"], q["p"])
        self.assertNotIn("pullRequest", q)

    def test_default_branch_url_names_neither(self):
        q = query(sonar.issues_url(pull_request=None, branch=None, page=1))

        self.assertNotIn("branch", q)
        self.assertNotIn("pullRequest", q)


class FetchAllTest(unittest.TestCase):

    def test_pages_until_the_reported_total_is_read(self):
        pages = {1: {"total": 3, "issues": [issue(1), issue(2)]}, 2: {"total": 3, "issues": [issue(3)]}}
        requested = []

        def fetch(url):
            page = int(query(url)["p"][0])
            requested.append(page)
            return pages[page]

        result = sonar.fetch_all(fetch, pull_request="1", branch=None)

        self.assertEqual([1, 2, 3], [i["line"] for i in result])
        self.assertEqual([1, 2], requested)

    def test_no_issues_is_one_request(self):
        result = sonar.fetch_all(lambda url: {"total": 0, "issues": []}, pull_request="1", branch=None)

        self.assertEqual([], result)


class FormatTest(unittest.TestCase):

    def test_line_names_rule_severity_file_line_and_message_without_project_key(self):
        self.assertEqual("java:S6204 MINOR core/src/A7.java:7 message 7", sonar.format_issue(issue(7)))

    def test_file_level_issue_without_line_prints_only_the_file(self):
        without_line = {k: v for k, v in issue(7).items() if k != "line"}

        self.assertEqual("java:S6204 MINOR core/src/A7.java message 7", sonar.format_issue(without_line))

    def test_report_ends_with_the_total(self):
        self.assertEqual(["java:S6204 MINOR core/src/A1.java:1 message 1", "1 open issue(s)"],
                         sonar.report([issue(1)]))

    def test_report_of_nothing_is_the_zero_total(self):
        self.assertEqual(["0 open issue(s)"], sonar.report([]))


class ArgumentsTest(unittest.TestCase):

    def test_pr_and_branch_exclude_each_other(self):
        with self.assertRaises(SystemExit):
            sonar.parse_args(["--pr", "1", "--branch", "x"])


if __name__ == "__main__":
    unittest.main()
