# goRalph stop kinds

The goRalph control file (`control.json`) names three stop kinds, set by `stop` (a stop request) and `limit_stop` (what a reached limit acts as):

- `after-iteration`: the stop takes effect when the current iteration ends.
- `after-step` (the default, also for a stop request without a kind): the stop waits for the current work step to end.
- `now`: the child process group is terminated at once; the state is the last pushed work step and uncommitted changes are lost.

Source: `docs/agents/ralph-loop.md` (Terms and Built).
