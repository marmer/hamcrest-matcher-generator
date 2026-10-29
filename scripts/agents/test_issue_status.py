"""Unit tests for the loose-ticket check in issue-status.py (stdlib unittest only, no API calls)."""
import importlib.util
import os
import unittest

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


def ready(number, labels=(), state="ready"):
    return {"number": number, "title": f"t{number}", "labels": list(labels), "status": state}


class FrontierLinesTest(unittest.TestCase):

    def test_owner_labelled_tickets_wait_on_a_separate_line(self):
        tickets = [ready(1), ready(2, ["needs-info"]), ready(3, ["ready-for-human"]), ready(4, state="blocked")]

        self.assertEqual(["#1 t1", "waiting for the owner: #2 t2 (needs-info), #3 t3 (ready-for-human)"],
                         status.frontier_lines(tickets))

    def test_no_waiting_line_without_owner_labelled_tickets(self):
        self.assertEqual(["#1 t1 (ready-for-agent)"], status.frontier_lines([ready(1, ["ready-for-agent"])]))


if __name__ == "__main__":
    unittest.main()
