# macOS · Minimal-Profil

Regelfreies Profil. Es trägt bewusst KEINE versionierten Regeln, Agents oder Commands.

Die Skills blendet der Launcher zur Laufzeit als **Symlink** auf `~/.claude/skills` ein
(`skills -> /Users/frank/.claude/skills`) — so sind im sonst isolierten Minimal-Profil alle Skills
verfügbar, ohne die übrige `~/.claude`-Umgebung (Rules/Hooks/Memory/Agents) hereinzuholen.
Der Symlink bleibt lokal: die `.gitignore` schließt `skills/` bewusst nicht wieder ein.

Der Profiltext steht in `../sources/minimal.md`, die `settings.json` daneben.
