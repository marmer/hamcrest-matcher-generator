"""Unit tests for check-credential-expiry.py (stdlib unittest only; HTTP, gpg and gh are replaced by fakes)."""
import importlib.util
import io
import json
import os
import unittest
import urllib.error
from datetime import date
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("check_credential_expiry", HERE / "check-credential-expiry.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

TODAY = date(2026, 10, 28)
SONAR_TOKEN = "squ_secret-sonar-token-0123456789"
GPG_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----\nsecret-gpg-material\n-----END PGP PRIVATE KEY BLOCK-----"
ISSUE_TITLE = "Credential renewal due"

# gpg --with-colons --list-keys: field 7 (index 6) of pub and sub records is the expiry as epoch seconds.
# 1798761600 is 2027-01-01T00:00:00Z; an empty field means the key does not expire.
GPG_LISTING = "\n".join([
    "tru:t:1:1700000000:0:3:1:5",
    "pub:u:4096:1:AAAABBBBCCCCDDDD:1700000000:1798761600::u:::scSC:::::::",
    "fpr:::::::::0123456789ABCDEF0123456789ABCDEF01234567:",
    "uid:u::::1700000000::HASH::Release <release@example.org>::::::::::0:",
    "sub:u:4096:1:1111222233334444:1700000000:1798848000:::::e::::::::",
    "sub:u:4096:1:5555666677778888:1700000000::::::e::::::::",
])


class Answer:
    """Fake urlopen: records the request and answers with a JSON body or an HTTP error."""

    def __init__(self, body=None, error=None):
        self.body = body
        self.error = error
        self.requests = []

    def __call__(self, request, timeout=None):
        self.requests.append(request)
        if self.error is not None:
            raise self.error
        return io.BytesIO(json.dumps(self.body).encode("utf-8"))


class FakeTools:
    """Fake gpg and gh: records every call, answers gpg with a listing and gh with the open issues."""

    def __init__(self, open_issues=(), gh_fails=False):
        self.open_issues = list(open_issues)
        self.gh_fails = gh_fails
        self.calls = []

    def __call__(self, args, input_text=None, env=None):
        self.calls.append((list(args), input_text))
        if args[0] == "gpg":
            return GPG_LISTING if "--list-keys" in args else ""
        if self.gh_fails:
            raise check.ToolError(args[0])
        if args[:3] == ["gh", "issue", "list"]:
            return json.dumps(self.open_issues)
        return ""

    def gh_calls(self):
        return [(args, text) for args, text in self.calls if args[0] == "gh"]


def sonar_body(*expirations):
    tokens = [{"name": f"token-{index}", "type": "USER_TOKEN", "expirationDate": each}
              for index, each in enumerate(expirations)]
    return {"userTokens": tokens}


def environ(**overrides):
    values = {
        "SONAR_TOKEN": SONAR_TOKEN,
        "MAVEN_GPG_KEY": GPG_KEY,
        "CENTRAL_TOKEN_EXPIRES_ON": "2099-01-01",
        "RELEASE_APP_KEY_EXPIRES_ON": "2099-01-01",
        "GH_TOKEN": "gh-secret-value",
    }
    values.update(overrides)
    return values


def run(env, opener, tools, today=TODAY):
    out, err = io.StringIO(), io.StringIO()
    code = check.main(env, opener, tools, today, out, err)
    return code, out.getvalue(), err.getvalue()


class ParsingTest(unittest.TestCase):
    def test_gpg_expiry_is_the_earliest_expiry_of_the_primary_and_subkeys(self):
        self.assertEqual(date(2027, 1, 1), check.gpg_expiry(GPG_LISTING))

    def test_gpg_expiry_is_none_when_no_key_expires(self):
        listing = "pub:u:4096:1:AAAA:1700000000::::::\nsub:u:4096:1:BBBB:1700000000::::::"

        self.assertIsNone(check.gpg_expiry(listing))

    def test_gpg_expiry_ignores_revoked_invalid_and_expired_subkeys(self):
        listing = "\n".join([
            "pub:u:4096:1:AAAA:1700000000:1798761600::u:::scSC:::::::",
            "sub:r:4096:1:BBBB:1700000000:1600000000:::::e::::::::",
            "sub:i:4096:1:CCCC:1700000000:1600000001:::::e::::::::",
            "sub:d:4096:1:DDDD:1700000000:1600000002:::::e::::::::",
        ])

        self.assertEqual(date(2027, 1, 1), check.gpg_expiry(listing))

    def test_gpg_expiry_with_only_unusable_keys_is_an_error(self):
        with self.assertRaises(ValueError):
            check.gpg_expiry("pub:r:4096:1:AAAA:1700000000:1798761600::u:::scSC:::::::")

    def test_gpg_expiry_without_any_key_record_is_an_error(self):
        with self.assertRaises(ValueError):
            check.gpg_expiry("tru:t:1:1700000000:0:3:1:5")

    def test_sonar_expiry_is_the_earliest_token_expiry(self):
        body = sonar_body("2027-11-20T10:00:00+0000", "2027-03-01T08:30:00+0000")

        self.assertEqual(date(2027, 3, 1), check.sonar_expiry(body))

    def test_sonar_expiry_is_none_for_tokens_without_expiry(self):
        self.assertIsNone(check.sonar_expiry(sonar_body(None)))

    def test_sonar_expiry_without_user_tokens_is_an_error(self):
        with self.assertRaises(ValueError):
            check.sonar_expiry({"unexpected": 1})

    def test_iso_date_parses_plain_dates(self):
        self.assertEqual(date(2099, 1, 31), check.parse_date("2099-01-31"))

    def test_iso_date_rejects_other_text(self):
        with self.assertRaises(ValueError):
            check.parse_date("31.01.2099")


class ReminderWindowTest(unittest.TestCase):
    def test_thirty_days_before_expiry_is_due(self):
        due = check.due_reminders({"Central Portal token": date(2026, 11, 27)}, TODAY)

        self.assertEqual([("Central Portal token", date(2026, 11, 27), 30)], due)

    def test_thirty_one_days_before_expiry_is_not_due(self):
        self.assertEqual([], check.due_reminders({"Central Portal token": date(2026, 11, 28)}, TODAY))

    def test_expired_credentials_stay_due_with_negative_days(self):
        due = check.due_reminders({"GPG signing key": date(2026, 10, 1)}, TODAY)

        self.assertEqual([("GPG signing key", date(2026, 10, 1), -27)], due)

    def test_credentials_without_expiry_are_never_due(self):
        self.assertEqual([], check.due_reminders({"Sonar token": None}, TODAY))


class IssueBodyTest(unittest.TestCase):
    def test_body_mentions_the_owner_and_lists_each_due_credential(self):
        body = check.issue_body([("Sonar token", date(2026, 11, 20), 23)])

        self.assertIn("@marmer", body)
        self.assertIn("Sonar token", body)
        self.assertIn("2026-11-20", body)
        self.assertIn("23", body)
        self.assertIn(check.RUNBOOK, body)

    def test_body_gives_renewal_steps_for_every_credential_kind(self):
        body = check.issue_body([("GPG signing key", date(2026, 11, 20), 23)])

        self.assertIn("MAVEN_GPG_KEY", body)


class CredentialExpiryMainTest(unittest.TestCase):
    def test_nothing_due_passes_without_touching_issues(self):
        tools = FakeTools()

        code, out, err = run(environ(), Answer(sonar_body("2099-01-01T00:00:00+0000")), tools)

        self.assertEqual(0, code)
        self.assertEqual([], tools.gh_calls())
        self.assertIn("no credential expires within 30 days", out)
        self.assertEqual("", err)

    def test_due_credential_opens_an_issue_assigned_to_the_owner(self):
        tools = FakeTools()

        code, _, _ = run(environ(CENTRAL_TOKEN_EXPIRES_ON="2026-11-20"),
                         Answer(sonar_body("2099-01-01T00:00:00+0000")), tools)

        self.assertEqual(0, code)
        created = [(args, text) for args, text in tools.gh_calls() if args[:3] == ["gh", "issue", "create"]]
        self.assertEqual(1, len(created))
        args, body = created[0]
        self.assertIn(ISSUE_TITLE, args)
        self.assertEqual("marmer", args[args.index("--assignee") + 1])
        self.assertIn("Central Portal token", body)
        self.assertIn("@marmer", body)

    def test_due_credential_updates_the_open_issue_instead_of_opening_another(self):
        tools = FakeTools(open_issues=[{"number": 42, "title": ISSUE_TITLE}])

        code, _, _ = run(environ(RELEASE_APP_KEY_EXPIRES_ON="2026-11-20"),
                         Answer(sonar_body("2099-01-01T00:00:00+0000")), tools)

        self.assertEqual(0, code)
        names = [args[:3] for args, _ in tools.gh_calls()]
        self.assertIn(["gh", "issue", "edit"], names)
        self.assertNotIn(["gh", "issue", "create"], names)
        edit_args = [args for args, _ in tools.gh_calls() if args[:3] == ["gh", "issue", "edit"]][0]
        self.assertEqual("42", edit_args[3])

    def test_unrelated_open_issues_are_not_updated(self):
        tools = FakeTools(open_issues=[{"number": 9, "title": "Something else"}])

        run(environ(CENTRAL_TOKEN_EXPIRES_ON="2026-11-20"), Answer(sonar_body("2099-01-01T00:00:00+0000")), tools)

        names = [args[:3] for args, _ in tools.gh_calls()]
        self.assertIn(["gh", "issue", "create"], names)
        self.assertNotIn(["gh", "issue", "edit"], names)

    def test_sonar_token_expiry_within_thirty_days_is_reported(self):
        tools = FakeTools()

        code, _, _ = run(environ(), Answer(sonar_body("2026-11-20T00:00:00+0000")), tools)

        self.assertEqual(0, code)
        body = [text for args, text in tools.gh_calls() if args[:3] == ["gh", "issue", "create"]][0]
        self.assertIn("Sonar token", body)

    def test_unset_reminder_date_is_a_warning_not_a_failure(self):
        tools = FakeTools()

        code, _, err = run(environ(CENTRAL_TOKEN_EXPIRES_ON=""), Answer(sonar_body("2099-01-01T00:00:00+0000")),
                           tools)

        self.assertEqual(0, code)
        self.assertIn("::warning::", err)
        self.assertIn("CENTRAL_TOKEN_EXPIRES_ON", err)

    def test_malformed_reminder_date_fails(self):
        code, _, err = run(environ(RELEASE_APP_KEY_EXPIRES_ON="next year"),
                           Answer(sonar_body("2099-01-01T00:00:00+0000")), FakeTools())

        self.assertEqual(1, code)
        self.assertIn("RELEASE_APP_KEY_EXPIRES_ON", err)

    def test_rejected_sonar_token_fails_with_an_explicit_message(self):
        error = urllib.error.HTTPError("https://sonarcloud.io", 401, "Unauthorized", None, None)

        code, _, err = run(environ(), Answer(error=error), FakeTools())

        self.assertEqual(1, code)
        self.assertIn("SONAR_TOKEN", err)

    def test_missing_gpg_key_fails(self):
        code, _, err = run(environ(MAVEN_GPG_KEY=""), Answer(sonar_body("2099-01-01T00:00:00+0000")), FakeTools())

        self.assertEqual(1, code)
        self.assertIn("MAVEN_GPG_KEY", err)

    def test_failing_issue_call_fails_the_job(self):
        code, _, err = run(environ(CENTRAL_TOKEN_EXPIRES_ON="2026-11-20"),
                           Answer(sonar_body("2099-01-01T00:00:00+0000")), FakeTools(gh_fails=True))

        self.assertEqual(1, code)
        self.assertIn("could not", err)

    def test_secrets_are_never_printed_or_sent_to_the_issue(self):
        tools = FakeTools()

        _, out, err = run(environ(CENTRAL_TOKEN_EXPIRES_ON="2026-11-20"),
                          Answer(sonar_body("2099-01-01T00:00:00+0000")), tools)

        for secret in (SONAR_TOKEN, "secret-gpg-material", "gh-secret-value"):
            self.assertNotIn(secret, out + err)
        for args, text in tools.gh_calls():
            for secret in (SONAR_TOKEN, "secret-gpg-material", "gh-secret-value"):
                self.assertNotIn(secret, " ".join(args) + str(text))


if __name__ == "__main__":
    unittest.main()
