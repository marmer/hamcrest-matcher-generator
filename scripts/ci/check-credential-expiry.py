#!/usr/bin/env python3
"""Open or update the renewal issue when a release credential expires within 30 days.

Expiry dates:
- the Sonar token: `expirationDate` of the user's tokens (SonarQube Cloud API, the earliest one counts);
- the GPG signing key: the expiry of its primary and subkeys, read from `MAVEN_GPG_KEY` in a throwaway GNUPGHOME;
- the Central Portal token and the GitHub App private key: no API exposes them, so the repository variables
  `CENTRAL_TOKEN_EXPIRES_ON` and `RELEASE_APP_KEY_EXPIRES_ON` hold their expiry dates (YYYY-MM-DD), set by the
  owner after each renewal. An unset variable is a warning, a malformed one fails.

A credential due within 30 days (or already expired) puts one open issue, titled `ISSUE_TITLE`, assigned to and
@-mentioning the owner, with renewal steps. Later runs update that issue instead of opening another.
Secrets are read from the environment and never printed or written to the issue.

Stdlib only. The tokens themselves are validated by check-sonar-token.py and check-central-token.py.

Usage (step of .github/workflows/credential-reminder.yml):
  SONAR_TOKEN=... MAVEN_GPG_KEY=... CENTRAL_TOKEN_EXPIRES_ON=... RELEASE_APP_KEY_EXPIRES_ON=... GH_TOKEN=... \
    scripts/ci/check-credential-expiry.py
"""
import json
import os
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
from datetime import date, datetime, timezone

SONAR_TOKENS_URL = "https://sonarcloud.io/api/user_tokens/search"
REMINDER_DAYS = 30
ISSUE_TITLE = "Credential renewal due"
OWNER = "marmer"
# The setup checklist (#147) until MAINTAINING.md (#137) exists; then point this at that file.
RUNBOOK = "https://github.com/marmer/hamcrest-matcher-generator/issues/147"
# Field index 6 of a `gpg --with-colons` pub or sub record is the expiry as epoch seconds (empty: no expiry).
GPG_EXPIRY_FIELD = 6
# Field 2 (validity) values of keys that need no renewal: revoked, invalid, disabled.
UNUSABLE_VALIDITY = ("r", "i", "d")

RENEWAL_STEPS = {
    "Sonar token": (
        "Create a new user token in SonarQube Cloud (My Account, Security) with a one-year expiry, "
        "and replace the `SONAR_TOKEN` repository secret."
    ),
    "GPG signing key": (
        "Generate a new signing key with a three-year expiry, publish it to the keyservers, and replace "
        "`MAVEN_GPG_KEY` and `MAVEN_GPG_PASSPHRASE` in the `release` environment."
    ),
    "Central Portal token": (
        "Generate a new user token in the Central Portal (Account, Generate User Token), replace "
        "`CENTRAL_USERNAME` and `CENTRAL_PASSWORD` in the `release` environment, then set the repository "
        "variable `CENTRAL_TOKEN_EXPIRES_ON` to the new expiry date."
    ),
    "GitHub App private key": (
        "Generate a new private key in the App settings, replace `RELEASE_APP_PRIVATE_KEY`, then set the "
        "repository variable `RELEASE_APP_KEY_EXPIRES_ON` to the new expiry date."
    ),
}


class ToolError(Exception):
    """A command-line tool (gh, gpg) failed. Its output is not repeated: it may echo the key it was given."""


class ReadError(Exception):
    """An expiry date could not be read; the message is printed as the error annotation."""


def parse_date(text):
    """A plain ISO date (YYYY-MM-DD); anything else raises ValueError."""
    return date.fromisoformat(text.strip())


def gpg_expiry(listing):
    """The earliest expiry among the usable pub and sub records of a `gpg --with-colons --list-keys` listing.

    Revoked (r), invalid (i) and disabled (d) keys and subkeys are skipped: renewing them is not needed."""
    records = [fields for fields in (line.split(":") for line in listing.splitlines())
               if fields[0] in ("pub", "sub") and (len(fields) < 2 or fields[1] not in UNUSABLE_VALIDITY)]
    if not records:
        raise ValueError("gpg listed no key")
    expiries = []
    for fields in records:
        if len(fields) <= GPG_EXPIRY_FIELD:
            raise ValueError("unexpected gpg listing")
        if fields[GPG_EXPIRY_FIELD]:
            expiries.append(datetime.fromtimestamp(int(fields[GPG_EXPIRY_FIELD]), tz=timezone.utc).date())
    return min(expiries) if expiries else None


def sonar_expiry(body):
    """The earliest `expirationDate` of the user's Sonar tokens, or None when no token expires."""
    if not isinstance(body, dict) or not isinstance(body.get("userTokens"), list):
        raise ValueError("unexpected SonarQube Cloud answer")
    expiries = [parse_date(token["expirationDate"][:10]) for token in body["userTokens"]
                if token.get("expirationDate")]
    return min(expiries) if expiries else None


def days_left(expiry, today):
    return (expiry - today).days


def due_reminders(expiries, today):
    """(credential, expiry, days left) for every credential expiring within REMINDER_DAYS, expired ones included."""
    due = []
    for name, expiry in expiries.items():
        if expiry is not None and days_left(expiry, today) <= REMINDER_DAYS:
            due.append((name, expiry, days_left(expiry, today)))
    return due


def issue_body(due):
    lines = [
        f"@{OWNER}, these credentials expire within {REMINDER_DAYS} days or have expired. "
        "The weekly credential reminder updates this issue until the dates move.",
        "",
        "| Credential | Expires | Days left |",
        "|---|---|---|",
    ]
    for name, expiry, days in due:
        lines.append(f"| {name} | {expiry.isoformat()} | {days} |")
    lines += ["", "## Renewal steps", ""]
    for name, _, _ in due:
        lines.append(f"- **{name}**: {RENEWAL_STEPS[name]}")
    lines += ["", "After the renewal, run the Credential reminder workflow with `workflow_dispatch` to check.",
              f"Runbook: {RUNBOOK}", ""]
    return "\n".join(lines)


def default_tool(args, input_text=None, env=None):
    try:
        completed = subprocess.run(args, input=input_text, env=env, capture_output=True, text=True, check=True)
    except (OSError, subprocess.CalledProcessError) as error:
        raise ToolError(args[0]) from error
    return completed.stdout


def read_sonar_expiry(environ, opener):
    token = environ.get("SONAR_TOKEN", "").strip()
    if not token:
        raise ReadError("could not read the SONAR_TOKEN expiry: the repository secret is not set")
    request = urllib.request.Request(SONAR_TOKENS_URL, headers={"Authorization": f"Bearer {token}"})
    try:
        with opener(request, timeout=30) as response:
            return sonar_expiry(json.load(response))
    except urllib.error.HTTPError as error:
        raise ReadError(f"could not read the SONAR_TOKEN expiry: SonarQube Cloud answered HTTP {error.code}")
    except (OSError, ValueError, TypeError, AttributeError) as error:
        raise ReadError(f"could not read the SONAR_TOKEN expiry: {type(error).__name__}")


def read_gpg_expiry(environ, tool):
    key = environ.get("MAVEN_GPG_KEY", "")
    if not key.strip():
        raise ReadError("MAVEN_GPG_KEY is not set, so the GPG key expiry cannot be read")
    with tempfile.TemporaryDirectory() as home:
        gpg_env = dict(environ, GNUPGHOME=home)
        try:
            tool(["gpg", "--batch", "--import"], input_text=key, env=gpg_env)
            return gpg_expiry(tool(["gpg", "--batch", "--with-colons", "--list-keys"], env=gpg_env))
        except (ToolError, ValueError) as error:
            raise ReadError(f"could not read the GPG key expiry: {type(error).__name__}")


def read_reminder_date(environ, name, err):
    """The repository variable `name` as a date. Unset is a warning and gives None; malformed raises ReadError."""
    text = environ.get(name, "").strip()
    if not text:
        print(f"::warning::{name} is not set, so its expiry is not checked. Set it to the expiry date (YYYY-MM-DD).",
              file=err)
        return None
    try:
        return parse_date(text)
    except ValueError:
        raise ReadError(f"{name} must be an expiry date (YYYY-MM-DD), found {text!r}")


def open_or_update_issue(environ, tool, body, out):
    listed = json.loads(tool(["gh", "issue", "list", "--state", "open", "--json", "number,title", "--limit", "100"],
                             env=environ) or "[]")
    existing = [issue["number"] for issue in listed if issue.get("title") == ISSUE_TITLE]
    if existing:
        tool(["gh", "issue", "edit", str(existing[0]), "--body-file", "-", "--add-assignee", OWNER],
             input_text=body, env=environ)
        print(f"updated renewal issue #{existing[0]}", file=out)
    else:
        tool(["gh", "issue", "create", "--title", ISSUE_TITLE, "--body-file", "-", "--assignee", OWNER],
             input_text=body, env=environ)
        print("opened renewal issue", file=out)


def main(environ=os.environ, opener=urllib.request.urlopen, tool=default_tool, today=None,
         out=sys.stdout, err=sys.stderr):
    today = today or datetime.now(timezone.utc).date()
    try:
        expiries = {
            "Sonar token": read_sonar_expiry(environ, opener),
            "GPG signing key": read_gpg_expiry(environ, tool),
            "Central Portal token": read_reminder_date(environ, "CENTRAL_TOKEN_EXPIRES_ON", err),
            "GitHub App private key": read_reminder_date(environ, "RELEASE_APP_KEY_EXPIRES_ON", err),
        }
    except ReadError as error:
        print(f"::error::{error}", file=err)
        return 1

    due = due_reminders(expiries, today)
    if not due:
        print(f"no credential expires within {REMINDER_DAYS} days", file=out)
        return 0
    try:
        open_or_update_issue(environ, tool, issue_body(due), out)
    except (ToolError, ValueError) as error:
        print(f"::error::could not open or update the renewal issue: {type(error).__name__}", file=err)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
