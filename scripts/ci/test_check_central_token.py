"""Unit tests for check-central-token.py (stdlib unittest only, the HTTP call is replaced by a fake opener)."""
import base64
import importlib.util
import io
import json
import os
import unittest
import urllib.error
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("check_central_token", HERE / "check-central-token.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

USERNAME = "central-token-user"
PASSWORD = "central-token-password-value"
MISSING_OR_INVALID = "CENTRAL_TOKEN missing or invalid"


class Answer:
    """Fake urlopen: records the request and answers with a JSON body, an HTTP status or a network error."""

    def __init__(self, body=None, error=None):
        self.body = body if body is not None else {}
        self.error = error
        self.requests = []

    def __call__(self, request, timeout=None):
        self.requests.append(request)
        if self.error is not None:
            raise self.error
        return io.BytesIO(json.dumps(self.body).encode("utf-8"))


def http_error(code):
    return urllib.error.HTTPError("https://central.sonatype.com", code, "answer", None, None)


def run(environ, opener):
    out, err = io.StringIO(), io.StringIO()
    code = check.main(environ, opener, out, err)
    return code, out.getvalue(), err.getvalue()


class CentralTokenCheckTest(unittest.TestCase):
    def test_missing_username_fails_with_explicit_message(self):
        code, _, err = run({"CENTRAL_USERNAME": "", "CENTRAL_PASSWORD": PASSWORD}, Answer())

        self.assertEqual(1, code)
        self.assertIn(MISSING_OR_INVALID, err)

    def test_missing_password_fails_with_explicit_message(self):
        code, _, err = run({"CENTRAL_USERNAME": USERNAME}, Answer())

        self.assertEqual(1, code)
        self.assertIn(MISSING_OR_INVALID, err)

    def test_sends_bearer_token_made_of_username_and_password(self):
        answer = Answer()

        run({"CENTRAL_USERNAME": USERNAME, "CENTRAL_PASSWORD": PASSWORD}, answer)

        expected = base64.b64encode(f"{USERNAME}:{PASSWORD}".encode("utf-8")).decode("ascii")
        self.assertEqual(f"Bearer {expected}", answer.requests[0].get_header("Authorization"))

    def test_never_prints_the_password(self):
        _, out, err = run({"CENTRAL_USERNAME": USERNAME, "CENTRAL_PASSWORD": PASSWORD},
                          Answer(error=http_error(401)))

        self.assertNotIn(PASSWORD, out + err)
        self.assertNotIn(USERNAME, out + err)

    def test_unauthorized_is_reported_as_invalid_with_runbook(self):
        code, _, err = run({"CENTRAL_USERNAME": USERNAME, "CENTRAL_PASSWORD": PASSWORD},
                           Answer(error=http_error(401)))

        self.assertEqual(1, code)
        self.assertIn(MISSING_OR_INVALID, err)
        self.assertIn(check.RUNBOOK, err)

    def test_forbidden_is_reported_as_invalid(self):
        code, _, err = run({"CENTRAL_USERNAME": USERNAME, "CENTRAL_PASSWORD": PASSWORD},
                           Answer(error=http_error(403)))

        self.assertEqual(1, code)
        self.assertIn(MISSING_OR_INVALID, err)

    def test_accepted_token_passes_when_the_probe_id_is_unknown(self):
        # The probe deployment id does not exist: a valid token gets a client error other than 401/403.
        code, out, err = run({"CENTRAL_USERNAME": USERNAME, "CENTRAL_PASSWORD": PASSWORD},
                             Answer(error=http_error(404)))

        self.assertEqual(0, code)
        self.assertIn("CENTRAL_TOKEN accepted by the Central Portal", out)
        self.assertEqual("", err)

    def test_successful_answer_passes(self):
        code, out, _ = run({"CENTRAL_USERNAME": USERNAME, "CENTRAL_PASSWORD": PASSWORD}, Answer())

        self.assertEqual(0, code)
        self.assertIn("accepted", out)

    def test_server_error_is_could_not_validate_not_invalid(self):
        code, _, err = run({"CENTRAL_USERNAME": USERNAME, "CENTRAL_PASSWORD": PASSWORD},
                           Answer(error=http_error(503)))

        self.assertEqual(1, code)
        self.assertIn("could not validate CENTRAL_TOKEN", err)
        self.assertNotIn(MISSING_OR_INVALID, err)

    def test_network_error_is_could_not_validate_not_invalid(self):
        code, _, err = run({"CENTRAL_USERNAME": USERNAME, "CENTRAL_PASSWORD": PASSWORD},
                           Answer(error=OSError("no route")))

        self.assertEqual(1, code)
        self.assertIn("could not validate CENTRAL_TOKEN", err)
        self.assertNotIn(MISSING_OR_INVALID, err)


if __name__ == "__main__":
    unittest.main()
