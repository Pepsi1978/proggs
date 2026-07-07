# Toolchain-Update-Management (macOS-Dev-Umgebung) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## Kurzcheck (vor jedem Toolchain-Update lesen)

| Signal / Situation | Sofort-Regel |
|--------------------|--------------|
| "Alle Tools aktualisieren" auf macOS | brew-Formulae gefahrlos in einem Rutsch; Session überlebt (Binaries werden ersetzt, laufende Prozesse behalten alte Version) |
| node/deno/powershell sollen "zuletzt" bleiben | Geht via Liste NICHT zuverlässig — `brew upgrade` zieht sie als Dependency trotzdem mit. Auf macOS unkritisch. |
| Echte Terminal-Killer (vorher fragen!) | NUR `brew upgrade --cask iterm2` + `claude update`. Sonst nichts. |
| Cask-Update meldet "Upgraded", aber Version stimmt nicht | Cask brauchte `sudo` (kein TTY) → echte Version mit dem Tool selbst prüfen (`dotnet --version`, `--list-sdks`) |
| Major-Bump eines globalen CLI-Tools (TS 5→6, Kotlin 2.3→2.4) | NICHT blind global. Konservativ stabile Version global, Major projekt-lokal + test-getrieben |
| Nach Update | PATH verifizieren (`~/.claude/hooks/path-verify.sh --fix`) + Kern-Versionen real prüfen |
