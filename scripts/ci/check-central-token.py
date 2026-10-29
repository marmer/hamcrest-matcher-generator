#!/usr/bin/env python3
"""Fail the credential reminder when the Maven Central Portal token is missing or rejected.

The token is the username/password pair of the Central Portal user token, sent as HTTP Basic-style
`Bearer base64(username:password)`. The Portal answers 401 or 403 for a bad token. The probe asks the
status endpoint for a deployment id that does not exist: a valid token gets a client error other than
401/403, a 500 or success, so only the authentication answer decides. Other server errors (502, 503, 504)
and network errors mean the token could not be checked. Nothing is published or changed.

Usage (step of .github/workflows/credential-reminder.yml):
  CENTRAL_USERNAME=... CENTRAL_PASSWORD=... scripts/ci/check-central-token.py
"""
import base64
import os
import sys
import urllib.error
import urllib.request

PROBE_URL = "https://central.sonatype.com/api/v1/publisher/status?id=00000000-0000-0000-0000-000000000000"
RUNBOOK = "https://github.com/marmer/hamcrest-matcher-generator/blob/main/MAINTAINING.md"
MISSING_OR_INVALID = "CENTRAL_TOKEN missing or invalid"


def report_invalid_token(err):
    print(f"::error::{MISSING_OR_INVALID}: the Central Portal rejects it. Runbook: {RUNBOOK}", file=err)
    return 1


def main(environ=os.environ, opener=urllib.request.urlopen, out=sys.stdout, err=sys.stderr):
    username = environ.get("CENTRAL_USERNAME", "").strip()
    password = environ.get("CENTRAL_PASSWORD", "").strip()
    if not username or not password:
        print(f"::error::{MISSING_OR_INVALID}: the environment secrets are not set. Runbook: {RUNBOOK}", file=err)
        return 1
    bearer = base64.b64encode(f"{username}:{password}".encode("utf-8")).decode("ascii")
    request = urllib.request.Request(PROBE_URL, headers={"Authorization": f"Bearer {bearer}"})
    try:
        with opener(request, timeout=30) as response:
            response.read()
    except urllib.error.HTTPError as error:
        if error.code in (401, 403):
            return report_invalid_token(err)
        # The Portal checks the login first and answers 500 for the unknown probe id (observed with a valid token
        # on 2026-10-29, where a rejected token got 401), so 500 counts as accepted; other 5xx stay "unknown".
        if error.code > 500:
            print(f"::error::could not validate CENTRAL_TOKEN: the Central Portal answered HTTP {error.code}", file=err)
            return 1
    except (OSError, ValueError) as error:
        print(f"::error::could not validate CENTRAL_TOKEN: {type(error).__name__} while calling the Central Portal",
              file=err)
        return 1
    print("CENTRAL_TOKEN accepted by the Central Portal", file=out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
