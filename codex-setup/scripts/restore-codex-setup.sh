#!/usr/bin/env bash
set -euo pipefail

workspace="${1:-$(pwd)}"
setup_root="$workspace/codex-setup"
codex_home="$HOME/.codex"

copy_directory_children() {
  local source_dir="$1"
  local target_dir="$2"

  [[ -d "$source_dir" ]] || {
    echo 0
    return
  }

  mkdir -p "$target_dir"
  local count
  count="$(find "$source_dir" -mindepth 1 -maxdepth 1 | wc -l | tr -d '[:space:]')"
  cp -R "$source_dir"/. "$target_dir"/
  echo "$count"
}

if [[ ! -d "$setup_root" ]]; then
  echo "Missing codex-setup directory: $setup_root" >&2
  exit 1
fi

mkdir -p "$codex_home"

git config --global pull.rebase true
git config --global rebase.autoStash true
git config --global rerere.enabled true

rules_copied="$(copy_directory_children "$setup_root/rules" "$codex_home/rules")"
skills_copied="$(copy_directory_children "$setup_root/skills" "$codex_home/skills")"
agents_copied="$(copy_directory_children "$setup_root/agents" "$codex_home/agents")"
hooks_copied="$(copy_directory_children "$setup_root/hooks" "$codex_home/hooks")"

if [[ -d "$codex_home/hooks" ]]; then
  find "$codex_home/hooks" -type f -name "*.sh" -exec chmod +x {} +
fi

echo "Codex setup restored to $codex_home"
echo "Rules entries copied: $rules_copied"
echo "Skill entries copied: $skills_copied"
echo "Agent entries copied: $agents_copied"
echo "Hook entries copied: $hooks_copied"
echo "Git config pull.rebase=$(git config --global --get pull.rebase)"
echo "Git config rebase.autoStash=$(git config --global --get rebase.autoStash)"
echo "Git config rerere.enabled=$(git config --global --get rerere.enabled)"
