#!/usr/bin/env bash
set -euo pipefail

assert() {
  if ! "$@"; then
    printf 'ASSERTION FAILED: %s\n' "$*" >&2
    exit 1
  fi
}

root="$(mktemp -d "${TMPDIR:-/tmp}/session-opencode-test.XXXXXX")"
trap 'rm -rf "$root"' EXIT

fake_home="$root/home"
repo="$fake_home/proggs"
remote="$root/remote.git"
remote_writer="$root/remote-writer"
second_pc="$root/second-pc"
local_backup="$fake_home/.claude/session-opencode-backup.md"
repo_backup="$repo/.claude/session-opencode-backup.md"
local_diff="$fake_home/.claude/session-opencode-backup.diff"
repo_diff="$repo/.claude/session-opencode-backup.diff"

export GIT_AUTHOR_NAME='Session OpenCode Test'
export GIT_AUTHOR_EMAIL='session-opencode@example.invalid'
export GIT_COMMITTER_NAME="$GIT_AUTHOR_NAME"
export GIT_COMMITTER_EMAIL="$GIT_AUTHOR_EMAIL"

git init -q -b main "$repo"
git init -q --bare -b main "$remote"
mkdir -p "$repo/.claude"
: > "$repo_backup"
printf 'base\n' > "$repo/foreign.txt"
printf 'base\n' > "$repo/work.txt"
git -C "$repo" add -- .claude/session-opencode-backup.md foreign.txt work.txt
git -C "$repo" commit -q -m baseline
git -C "$repo" remote add origin "$remote"
git -C "$repo" push -q -u origin main

git clone -q "$remote" "$remote_writer"
printf '# Session Handoff (OpenCode) - alter Remote-Stand\n' > "$remote_writer/.claude/session-opencode-backup.md"
git -C "$remote_writer" commit --only -q -m 'remote advance' -- .claude/session-opencode-backup.md
git -C "$remote_writer" push -q origin HEAD:main

mkdir -p "$(dirname "$local_backup")" "$(dirname "$repo_backup")"
cat > "$local_backup" <<'SESSION_OPENCODE_EOF'
# Session Handoff (OpenCode) — Test

Umlaute: äöü
SESSION_OPENCODE_EOF
git -C "$repo" fetch -q origin
git -C "$repo" merge --ff-only origin/main >/dev/null
cp "$local_backup" "$repo_backup"
printf 'diff payload\n' > "$local_diff"
cp "$local_diff" "$repo_diff"
assert cmp -s "$local_backup" "$repo_backup"
assert grep -q 'Session Handoff' "$local_backup"

printf 'fremde staged Aenderung\n' > "$repo/foreign.txt"
git -C "$repo" add -- foreign.txt
printf 'halbfertige Arbeit\n' > "$repo/work.txt"
git -C "$repo" fetch -q origin
git -C "$repo" merge-base --is-ancestor origin/main HEAD
git -C "$repo" add -N -- .claude/session-opencode-backup.diff
git -C "$repo" commit --only -q -m 'session-opencode: handoff snapshot' -- .claude/session-opencode-backup.md .claude/session-opencode-backup.diff
committed="$(git -C "$repo" show --pretty='' --name-only HEAD | sed '/^$/d')"
assert grep -qx '.claude/session-opencode-backup.md' <<< "$committed"
assert grep -qx '.claude/session-opencode-backup.diff' <<< "$committed"
assert test "$(printf '%s\n' "$committed" | wc -l | tr -d ' ')" -eq 2
assert grep -qx 'foreign.txt' < <(git -C "$repo" diff --cached --name-only)
assert grep -qx 'work.txt' < <(git -C "$repo" diff --name-only)
git -C "$repo" push -q origin HEAD:main

git clone -q "$remote" "$second_pc"
assert grep -q 'Session Handoff' "$second_pc/.claude/session-opencode-backup.md"
assert test -f "$second_pc/.claude/session-opencode-backup.diff"

: > "$local_backup"
: > "$repo_backup"
rm -f "$local_diff" "$repo_diff"
git -C "$repo" commit --only -q -m 'session-opencode: clear restored handoff' -- .claude/session-opencode-backup.md .claude/session-opencode-backup.diff
git -C "$repo" push -q origin HEAD:main
git -C "$second_pc" pull -q --ff-only
assert test ! -s "$second_pc/.claude/session-opencode-backup.md"
assert test ! -e "$second_pc/.claude/session-opencode-backup.diff"

printf 'PASS: Bash Backup, isolierter Commit, Zweit-PC-Restore und Cleanup\n'
