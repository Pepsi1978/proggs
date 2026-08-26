# macOS · Strikt-Profil

Wie das Standard-Profil, zusätzlich mit der **vollständigen Hook-Kette**: die `settings.json`
trägt alle bash-Hooks aus `claude-code-setup/hooks-macos.json` (SessionStart, UserPromptSubmit,
PreToolUse, PostToolUse, Stop, SubagentStop, SessionEnd u. a.).

Alle übrigen Einstellungen sind 1:1 aus `Profiles/ClaudeCode/strict/settings.json` (Windows)
übernommen. Der Profiltext steht in `../sources/strict.md`.
