# ticket-489

loop.sh fetches origin main and refuses a start whose HEAD lacks it (before lock and run dir). LoopScriptTest.setUp now builds a git repo with a bare origin in the scratch dir; `advance_origin` adds commits to origin main. Slow full test file (several minutes): run with -k.
