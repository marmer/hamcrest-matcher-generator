# Ticket 488: page loop instead of gh --paginate

- `gh api --paginate` follows GitHub's Link header (`repositories/{id}/...`), which the cloud proxy refuses with 403. Lists are now read as `per_page=100&page=1, 2, ...` until a page has fewer than 100 items: `ghapi.gh_list` (Python) and `gh_pages` in `loop.sh` (array of pages, so the `jq` callers are unchanged; for check runs the items counted are those of `check_runs`).
- Page limit `RALPH_GH_MAX_PAGES` (default 100): a list still full there is an error naming the endpoint (never truncated). It is an env knob so the tests need only 3 pages.
- All list paths must carry a query with `per_page=100` (the loop appends `&page=N`).
- Test stubs of summary, interview and loop serve page N for `&page=N` and refuse `--paginate` like the proxy. A multi-page fixture needs full pages (100 items) before the last one, else the reader stops early.
- `scripts/agents/issue-status.py` reads `sub_issues?per_page=100` and `dependencies/blocked_by?per_page=100` as one page only (not changed, outside the ticket): a spec with more than 100 sub-issues would be cut silently.
- Stub pitfall: `per_page=100` contains `page=`; match `&page=`. In bash `case` patterns an unquoted `&` is a syntax error, quote it.
