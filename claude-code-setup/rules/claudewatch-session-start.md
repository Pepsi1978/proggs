# claudewatch — Session Start Protocol

At the start of EVERY session (new or resumed), execute this protocol
BEFORE responding to the user's request:

1. Call `get_project_health` (claudewatch MCP) immediately.
   You cannot calibrate to friction patterns, historical blockers, or agent
   failure modes without this baseline. Proceeding without it means repeating
   known failures.

2. Read and internalize the friction metrics returned. If friction
   rate > 30%, you are operating in a high-error environment. If specific agent
   types show 0% success rate, DO NOT spawn them. If top friction type is
   `buggy_code` or `retry:Bash`, verify every command before execution.

3. When resuming work: If the user says "continue", "resume", "keep
   working on", or references previous work, call `get_task_history(query: "<topic>")`
   BEFORE attempting implementation. If you find a matching task with status
   "abandoned" or "in_progress", read its blockers/solution. Do not repeat
   failed approaches.
