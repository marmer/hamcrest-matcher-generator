"""Unit tests for the loose-ticket check in issue-status.py (stdlib unittest only, no API calls)."""
import contextlib
import importlib.util
import io
import os
import subprocess
import sys
import unittest
from unittest import mock

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("issue_status", os.path.join(HERE, "issue-status.py"))
status = importlib.util.module_from_spec(spec)
spec.loader.exec_module(status)


def ticket(number, blocked_by=(), closed=False):
    return {"number": number, "title": f"t{number}", "closed": closed, "blocked_by": list(blocked_by)}


class LooseTicketsTest(unittest.TestCase):

    def test_chain_into_one_final_ticket_has_no_loose_tickets(self):
        tickets = [ticket(1), ticket(2, [1]), ticket(3, [2])]

        self.assertEqual((3, []), status.loose_tickets(tickets))

    def test_open_ticket_that_blocks_nothing_is_loose(self):
        tickets = [ticket(1), ticket(2, [1]), ticket(3, [1]), ticket(4, [2])]

        final, loose = status.loose_tickets(tickets)

        self.assertEqual(4, final)
        self.assertEqual([3], loose)

    def test_final_ticket_is_the_sink_with_most_open_ancestors(self):
        tickets = [ticket(1), ticket(2), ticket(3, [1, 2]), ticket(4)]

        self.assertEqual((3, [4]), status.loose_tickets(tickets))

    def test_closed_tickets_are_ignored(self):
        tickets = [ticket(1, closed=True), ticket(2, [1]), ticket(3, [2]), ticket(4, closed=True)]

        self.assertEqual((3, []), status.loose_tickets(tickets))

    def test_no_open_ticket_or_a_single_one_has_nothing_loose(self):
        self.assertEqual((None, []), status.loose_tickets([ticket(1, closed=True)]))
        self.assertEqual((1, []), status.loose_tickets([ticket(1)]))

    def test_warning_names_final_and_loose_tickets(self):
        warning = status.loose_warning(3, [4, 5])

        self.assertIn("#4, #5", warning)
        self.assertIn("#3", warning)
        self.assertIn("issue.py block 3 --by", warning)


class LooseWarningForTest(unittest.TestCase):

    def test_warning_for_tickets_with_a_loose_one_and_none_otherwise(self):
        self.assertIn("#3", status.loose_warning_for([ticket(1), ticket(2, [1]), ticket(3)]))
        self.assertIsNone(status.loose_warning_for([ticket(1), ticket(2, [1])]))


def ready(number, labels=(), state="ready"):
    return {"number": number, "title": f"t{number}", "labels": list(labels), "status": state}


class FrontierLinesTest(unittest.TestCase):

    def test_owner_labelled_tickets_wait_on_a_separate_line(self):
        tickets = [ready(1), ready(2, ["needs-info"]), ready(3, ["ready-for-human"]), ready(4, state="blocked")]

        self.assertEqual(["#1 t1", "waiting for the owner: #2 t2 (needs-info), #3 t3 (ready-for-human)"],
                         status.frontier_lines(tickets))

    def test_no_waiting_line_without_owner_labelled_tickets(self):
        self.assertEqual(["#1 t1 (ready-for-agent)"], status.frontier_lines([ready(1, ["ready-for-agent"])]))


class CommandLineTest(unittest.TestCase):

    def run_script(self, *args):
        return subprocess.run([sys.executable, os.path.join(HERE, "issue-status.py"), *args],
                              capture_output=True, text=True, timeout=30)

    def test_help_prints_usage_naming_the_flags_and_exits_0(self):
        for flag in ("--help", "-h"):
            with self.subTest(flag=flag):
                result = self.run_script(flag)

                self.assertEqual(0, result.returncode, result.stderr)
                for name in ("--print", "--json", "--frontier"):
                    self.assertIn(name, result.stdout)

    def test_parent_number_with_frontier_still_prints_the_ready_tickets(self):
        tickets = [{"number": 7, "title": "t7", "closed": False, "blocked_by": [], "status": "ready",
                    "labels": ["ready-for-agent"]}]
        out = io.StringIO()

        with mock.patch.object(status, "collect", return_value=tickets) as collect, \
                mock.patch.object(status, "gh", side_effect=AssertionError("must not call the API")), \
                mock.patch.object(sys, "argv", ["issue-status.py", "12", "--frontier"]), \
                contextlib.redirect_stdout(out):
            status.main()

        collect.assert_called_once_with("12")
        self.assertEqual("#7 t7 (ready-for-agent)\n", out.getvalue())


if __name__ == "__main__":
    unittest.main()
