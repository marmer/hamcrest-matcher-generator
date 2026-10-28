#!/usr/bin/env python3
"""Fail the Sonar job with an explicit message when SONAR_TOKEN is missing or not accepted by SonarQube Cloud.

The token is read from the environment only and never printed. SonarQube Cloud answers 200 with
`{"valid": false}` for a bad token, so the body is checked, not the status.

Usage (first step of the Sonar job in .github/workflows/ci-build.yml):
  SONAR_TOKEN=... scripts/ci/check-sonar-token.py
"""
import json
import os
import sys
import urllib.error
import urllib.request

VALIDATE_URL = "https://sonarcloud.io/api/authentication/validate"
# The setup checklist (#147) until MAINTAINING.md (#137) exists; then point this at that file.
RUNBOOK = "https://github.com/marmer/hamcrest-matcher-generator/issues/147"
MISSING_OR_INVALID = "SONAR_TOKEN missing or invalid"


def report_invalid_token(err):
    print(f"::error::{MISSING_OR_INVALID}: SonarQube Cloud rejects it. Runbook: {RUNBOOK}", file=err)
    return 1


def main(environ=os.environ, opener=urllib.request.urlopen, out=sys.stdout, err=sys.stderr):
    token = environ.get("SONAR_TOKEN", "").strip()
    if not token:
        print(f"::error::{MISSING_OR_INVALID}: the repository secret is not set. Runbook: {RUNBOOK}", file=err)
        return 1
    request = urllib.request.Request(VALIDATE_URL, headers={"Authorization": f"Bearer {token}"})
    try:
        with opener(request, timeout=30) as response:
            body = json.load(response)
    except urllib.error.HTTPError as error:
        if error.code in (401, 403):
            return report_invalid_token(err)
        print(f"::error::could not validate SONAR_TOKEN: SonarQube Cloud answered HTTP {error.code}", file=err)
        return 1
    except (OSError, ValueError) as error:
        print(f"::error::could not validate SONAR_TOKEN: {type(error).__name__} while calling SonarQube Cloud", file=err)
        return 1
    if not isinstance(body, dict) or body.get("valid") is not True:
        return report_invalid_token(err)
    print("SONAR_TOKEN accepted by SonarQube Cloud", file=out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
