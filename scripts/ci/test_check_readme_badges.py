"""Unit tests for check-readme-badges.py (stdlib unittest only; the HTTP call is replaced by a fake opener)."""
import importlib.util
import io
import os
import tempfile
import unittest
import urllib.error
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("check_readme_badges", HERE / "check-readme-badges.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

REPO_README = HERE.parents[1] / "README.md"
CI_BADGE = "https://github.com/marmer/hamcrest-matcher-generator/actions/workflows/ci-build.yml/badge.svg"
SONAR_WRONG = ("https://sonarcloud.io/api/project_badges/measure"
               "?project=io.github.marmer.testutils:hamcrest-matcher-generator&metric=alert_status")
SONAR_RIGHT = ("https://sonarcloud.io/api/project_badges/measure"
               "?project=io.github.marmer.testutils:matcher-generator-parent&metric=alert_status")
SVG_PASSED = b'<svg xmlns="http://www.w3.org/2000/svg"><text>passed</text></svg>'
SVG_PROJECT_NOT_FOUND = (b'<svg xmlns="http://www.w3.org/2000/svg"><!-- ERROR -->'
                         b'<text x="4" y="14">Project not found</text></svg>')


class FakeResponse:
    """Stands in for the response of a successful urlopen call."""

    def __init__(self, status, body):
        self.status = status
        self._body = body

    def __enter__(self):
        return self

    def __exit__(self, *exc_info):
        return False

    def read(self):
        return self._body


class Answers:
    """Fake urlopen: answers each URL from a table with (status, body), or raises the exception given for it.

    A URL missing from the table gets 200 with a passing SVG, so a test lists only the badges it wants to break.
    """

    def __init__(self, table=None):
        self.table = dict(table or {})
        self.requests = []
        self.timeouts = []

    def __call__(self, request, timeout=None):
        self.requests.append(request)
        self.timeouts.append(timeout)
        answer = self.table.get(request.full_url, (200, SVG_PASSED))
        if isinstance(answer, BaseException):
            raise answer
        status, body = answer
        if status >= 400:
            raise urllib.error.HTTPError(request.full_url, status, "error", None, io.BytesIO(body))
        return FakeResponse(status, body)


def run_main(readme_text, answers):
    with tempfile.TemporaryDirectory() as directory:
        readme = Path(directory) / "README.md"
        readme.write_text(readme_text, encoding="utf-8")
        out = io.StringIO()
        code = check.main(readme=readme, opener=answers, out=out)
    return code, out.getvalue().splitlines()


class BadgeUrlsTest(unittest.TestCase):
    def test_image_urls_of_linked_badges_are_extracted_and_plain_links_are_not(self):
        text = (f"[![CI]({CI_BADGE})](https://github.com/marmer/hamcrest-matcher-generator/actions)\n"
                "[plain link](https://example.org/not-a-badge)\n"
                "![Logo](https://example.org/logo.png)\n")

        self.assertEqual([CI_BADGE, "https://example.org/logo.png"], check.badge_urls(text))

    def test_the_repository_readme_has_badges_and_all_of_them_are_https(self):
        urls = check.badge_urls(REPO_README.read_text(encoding="utf-8"))

        self.assertTrue(urls)
        for url in urls:
            with self.subTest(url=url):
                self.assertTrue(url.startswith("https://"))



class BadgeProblemTest(unittest.TestCase):
    def test_a_passing_svg_is_fine(self):
        self.assertIsNone(check.badge_problem(200, SVG_PASSED))

    def test_sonarcloud_project_not_found_is_a_problem_even_with_http_200(self):
        problem = check.badge_problem(200, SVG_PROJECT_NOT_FOUND)

        self.assertIsNotNone(problem)
        self.assertIn("not found", problem)

    def test_not_found_is_matched_case_insensitively(self):
        self.assertIsNotNone(check.badge_problem(200, b"<text>NOT FOUND</text>"))

    def test_an_http_error_status_is_a_problem(self):
        self.assertIn("404", check.badge_problem(404, b"<svg/>"))

    def test_a_server_error_status_is_a_problem_even_with_a_passing_body(self):
        self.assertIn("500", check.badge_problem(500, SVG_PASSED))


class CheckBadgeTest(unittest.TestCase):
    def test_the_request_identifies_the_checker_and_waits_at_most_twenty_seconds(self):
        answers = Answers()

        self.assertIsNone(check.check_badge(CI_BADGE, answers))

        self.assertEqual([20], answers.timeouts)
        self.assertTrue(answers.requests[0].get_header("User-agent"))

    def test_a_connection_failure_is_reported_as_a_problem(self):
        answers = Answers({CI_BADGE: urllib.error.URLError("name or service not known")})

        problem = check.check_badge(CI_BADGE, answers)

        self.assertIsNotNone(problem)
        self.assertIn("could not fetch", problem)

    def test_a_wrong_sonar_key_answered_with_http_200_fails(self):
        answers = Answers({SONAR_WRONG: (200, SVG_PROJECT_NOT_FOUND)})

        self.assertIsNotNone(check.check_badge(SONAR_WRONG, answers))


class MainTest(unittest.TestCase):
    def test_every_badge_passing_prints_one_line_per_badge_and_exits_zero(self):
        readme = f"[![CI]({CI_BADGE})](https://example.org)\n[![Sonar]({SONAR_RIGHT})](https://example.org)\n"

        code, lines = run_main(readme, Answers())

        self.assertEqual(0, code)
        self.assertEqual([f"ok {CI_BADGE}", f"ok {SONAR_RIGHT}"], lines)

    def test_a_broken_badge_fails_with_an_annotation_naming_its_url(self):
        readme = f"[![CI]({CI_BADGE})](https://example.org)\n[![Sonar]({SONAR_WRONG})](https://example.org)\n"
        answers = Answers({SONAR_WRONG: (200, SVG_PROJECT_NOT_FOUND)})

        code, lines = run_main(readme, answers)

        self.assertEqual(1, code)
        self.assertEqual(f"ok {CI_BADGE}", lines[0])
        self.assertTrue(lines[1].startswith("::error::"))
        self.assertIn(SONAR_WRONG, lines[1])

    def test_a_missing_badge_status_fails_the_run(self):
        code, lines = run_main("![CI](https://example.org/ci.svg)\n", Answers({
            "https://example.org/ci.svg": (404, b"<svg/>"),
        }))

        self.assertEqual(1, code)
        self.assertIn("404", lines[0])

    def test_a_readme_without_badges_fails_instead_of_passing_silently(self):
        code, lines = run_main("Matcher Generator\n=================\n", Answers())

        self.assertEqual(1, code)
        self.assertEqual(1, len(lines))
        self.assertIn("no badge", lines[0])

    def test_the_repository_readme_passes_with_stubbed_answers(self):
        out = io.StringIO()

        code = check.main(readme=REPO_README, opener=Answers(), out=out)

        self.assertEqual(0, code)
        self.assertEqual(len(check.badge_urls(REPO_README.read_text(encoding="utf-8"))),
                         len(out.getvalue().splitlines()))


if __name__ == "__main__":
    unittest.main()
