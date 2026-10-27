"""Job display names of the GitHub workflows, shared by check-domain-docs-sync.py and scripts/ci/check-ruleset-job-names.py."""
import re

WORKFLOW_DIR = ".github/workflows"
JOB_NAME = re.compile(r"^    name:\s*(.+?)\s*$")
MATRIX_ROLE = re.compile(r"^\s*-?\s*role:\s*[\"']?([^\"'\s]+)[\"']?\s*$")


def workflow_job_names(root):
    """Job display names of every workflow, with `${{ matrix.role }}` expanded per matrix role."""
    names = set()
    workflow_dir = root / WORKFLOW_DIR
    if not workflow_dir.is_dir():
        return names
    for workflow in sorted(workflow_dir.glob("*.y*ml")):
        jobs = []  # (name, roles) per job, in file order
        for line in workflow.read_text(encoding="utf-8").splitlines():
            if m := JOB_NAME.match(line):
                jobs.append((m.group(1).strip("\"'"), []))
            elif jobs and (m := MATRIX_ROLE.match(line)):
                jobs[-1][1].append(m.group(1))
        for name, roles in jobs:
            if "${{ matrix.role }}" in name:
                names.update(name.replace("${{ matrix.role }}", role) for role in roles)
            else:
                names.add(name)
    return names
