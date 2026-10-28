"""Unit tests for check-sonar-token.py (stdlib unittest only, the HTTP call is replaced by a fake opener)."""
import importlib.util
import io
import json
import os
import unittest
import urllib.error
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("check_sonar_token", HERE / "check-sonar-token.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

TOKEN = "squ_secret-token-value-0123456789"


class Answer:
    """Fake urlopen: records the request and answers with a JSON body, an HTTP status or a network error."""

    def __init__(self, body=None, error=None):
        self.body = body
        self.error = error
        self.requests = []

    def __call__(self, request, timeout=None):
        self.requests.append(request)
        if self.error is not None:
            raise self.error
        return io.BytesIO(json.dumps(self.body).encode("utf-8"))


def run(environ, opener):
    out, err = io.StringIO(), io.StringIO()
    code = check.main(environ, opener, out, err)
    return code, out.getvalue(), err.getvalue()


class SonarTokenCheckTest(unittest.TestCase):

    def test_missing_token_fails_without_calling_sonar(self):
        answer = Answer({"valid": True})
        code, out, err = run({}, answer)
        self.assertEqual(1, code)
        self.assertIn("SONAR_TOKEN missing or invalid", err)
        self.assertIn(check.RUNBOOK, err)
        self.assertEqual([], answer.requests)

    def test_blank_token_counts_as_missing(self):
        answer = Answer({"valid": True})
        code, _, err = run({"SONAR_TOKEN": "   "}, answer)
        self.assertEqual(1, code)
        self.assertIn("SONAR_TOKEN missing or invalid", err)
        self.assertEqual([], answer.requests)

    def test_valid_token_passes_and_is_sent_as_bearer(self):
        answer = Answer({"valid": True})
        code, out, err = run({"SONAR_TOKEN": TOKEN}, answer)
        self.assertEqual(0, code)
        self.assertEqual("", err)
        self.assertEqual(1, len(answer.requests))
        request = answer.requests[0]
        self.assertEqual(check.VALIDATE_URL, request.full_url)
        self.assertEqual("Bearer " + TOKEN, request.get_header("Authorization"))

    def test_token_rejected_by_sonar_fails_with_the_same_message(self):
        code, out, err = run({"SONAR_TOKEN": TOKEN}, Answer({"valid": False}))
        self.assertEqual(1, code)
        self.assertIn("SONAR_TOKEN missing or invalid", err)
        self.assertIn(check.RUNBOOK, err)

    def test_http_401_counts_as_invalid(self):
        error = urllib.error.HTTPError(check.VALIDATE_URL, 401, "Unauthorized", None, None)
        code, _, err = run({"SONAR_TOKEN": TOKEN}, Answer(error=error))
        self.assertEqual(1, code)
        self.assertIn("SONAR_TOKEN missing or invalid", err)

    def test_http_403_counts_as_invalid(self):
        error = urllib.error.HTTPError(check.VALIDATE_URL, 403, "Forbidden", None, None)
        code, _, err = run({"SONAR_TOKEN": TOKEN}, Answer(error=error))
        self.assertEqual(1, code)
        self.assertIn("SONAR_TOKEN missing or invalid", err)

    def test_other_http_error_is_not_blamed_on_the_token(self):
        error = urllib.error.HTTPError(check.VALIDATE_URL, 500, "Server Error", None, None)
        code, _, err = run({"SONAR_TOKEN": TOKEN}, Answer(error=error))
        self.assertEqual(1, code)
        self.assertNotIn("SONAR_TOKEN missing or invalid", err)
        self.assertIn("could not validate SONAR_TOKEN", err)

    def test_unreachable_sonar_fails_without_blaming_the_token(self):
        error = urllib.error.URLError("name resolution failed")
        code, _, err = run({"SONAR_TOKEN": TOKEN}, Answer(error=error))
        self.assertEqual(1, code)
        self.assertNotIn("SONAR_TOKEN missing or invalid", err)
        self.assertIn("could not validate SONAR_TOKEN", err)

    def test_answer_without_valid_flag_fails(self):
        code, _, err = run({"SONAR_TOKEN": TOKEN}, Answer({"unexpected": 1}))
        self.assertEqual(1, code)
        self.assertIn("SONAR_TOKEN missing or invalid", err)

    def test_token_is_never_printed(self):
        for answer in (Answer({"valid": True}), Answer({"valid": False}),
                       Answer(error=urllib.error.URLError("down")),
                       Answer(error=urllib.error.HTTPError(check.VALIDATE_URL, 500, "Server Error", None, None))):
            _, out, err = run({"SONAR_TOKEN": TOKEN}, answer)
            self.assertNotIn(TOKEN, out)
            self.assertNotIn(TOKEN, err)


if __name__ == "__main__":
    unittest.main()
