#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TMP_MESSAGE="$(mktemp)"
TMP_LOG="$(mktemp)"
cleanup() {
  rm -f "$TMP_MESSAGE" "$TMP_LOG"
}
trap cleanup EXIT

codex mcp get openaiDeveloperDocs | rg -F "enabled: true" >/dev/null
codex mcp get openaiDeveloperDocs | rg -F "https://developers.openai.com/mcp" >/dev/null
curl -fsS -o /dev/null "https://developers.openai.com/api/docs/guides/tools-connectors-mcp"

PROMPT=$'Use only the openaiDeveloperDocs MCP server.\nDo not use web search or any fallback.\nIf openaiDeveloperDocs is available in this fresh Codex session, look up the OpenAI models page and reply with exactly AVAILABLE.\nIf the MCP server is unavailable, reply with exactly UNAVAILABLE.'

SMOKE_RESULT=""
for attempt in 1 2; do
  : >"$TMP_MESSAGE"
  : >"$TMP_LOG"

  if codex exec \
    --skip-git-repo-check \
    --dangerously-bypass-approvals-and-sandbox \
    -C "$REPO_ROOT" \
    -c 'model_reasoning_effort="low"' \
    -o "$TMP_MESSAGE" \
    "$PROMPT" >"$TMP_LOG" 2>&1; then
    SMOKE_RESULT="$(tr -d '\r' < "$TMP_MESSAGE" | tail -n 1 | sed 's/[[:space:]]*$//')"
    if [[ "$SMOKE_RESULT" == "AVAILABLE" ]]; then
      break
    fi
  fi

  if [[ "$attempt" -lt 2 ]]; then
    sleep 1
  fi
done

if [[ "$SMOKE_RESULT" != "AVAILABLE" ]]; then
  cat "$TMP_LOG" >&2
  echo "Fresh Codex exec did not confirm openaiDeveloperDocs availability after retry. Last message: $SMOKE_RESULT" >&2
  exit 1
fi

echo "openaiDeveloperDocs MCP configured, official docs reachable, and fresh Codex exec can use it"
