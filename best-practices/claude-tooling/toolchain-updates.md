# Best Practices: Toolchain-Update-Management (macOS-Dev-Umgebung)

> Stand: 2026-06-14 · Plattform: macOS (Homebrew + rustup + npm-global + uv + Sprach-SDKs)
> Quelle der Erkenntnisse: self-improve-Lauf 2026-06-14 (2 Researcher) + realer Update-Vorgang.
> Gegenstück im Bug-Almanach: noch keine eigene Datei — Fallen unten direkt mit aufgeführt.

---

## Kurzcheck (vor jedem Toolchain-Update lesen)

| Signal / Situation | Sofort-Regel |
|--------------------|--------------|
| "Alle Tools aktualisieren" auf macOS | brew-Formulae gefahrlos in einem Rutsch; Session überlebt (Binaries werden ersetzt, laufende Prozesse behalten alte Version) |
| node/deno/powershell sollen "zuletzt" bleiben | Geht via Liste NICHT zuverlässig — `brew upgrade` zieht sie als Dependency trotzdem mit. Auf macOS unkritisch. |
| Echte Terminal-Killer (vorher fragen!) | NUR `brew upgrade --cask iterm2` + `claude update`. Sonst nichts. |
| Cask-Update meldet "Upgraded", aber Version stimmt nicht | Cask brauchte `sudo` (kein TTY) → echte Version mit dem Tool selbst prüfen (`dotnet --version`, `--list-sdks`) |
| Major-Bump eines globalen CLI-Tools (TS 5→6, Kotlin 2.3→2.4) | NICHT blind global. Konservativ stabile Version global, Major projekt-lokal + test-getrieben |
| Nach Update | PATH verifizieren (`~/.claude/hooks/path-verify.sh --fix`) + Kern-Versionen real prüfen |

---

## Volltext

### 1. macOS-brew killt die Session nicht
`brew upgrade` ersetzt nur Binaries auf der Platte. Die laufende Shell, Claude Code und MCP-Server
behalten ihre Version im Speicher und laufen ungestört weiter. Deshalb darf man auf macOS Formulae
(auch node/git/python/deno/powershell) im laufenden Betrieb upgraden. Der A3-Vorfall, der zur
"Shell-Updates zuletzt"-Regel führte, war Windows-PowerShell-spezifisch (schließt Windows-Terminals).

### 2. Selektiver Ausschluss per Befehlsliste funktioniert nicht
`brew upgrade <formula1 formula2 …>` zieht veraltete **Dependencies** der genannten Formulae
ebenfalls hoch. Wer node/deno/powershell "zurückstellen" will, erreicht das per Liste nicht.
Wer es wirklich einfrieren muss: `brew pin <formula>` (Stufe stärker), wieder lösen mit `brew unpin`.

### 3. Echte Terminal-Killer auf macOS
Nur zwei Aktionen unterbrechen die laufende Sitzung wirklich und gehören vorher abgestimmt:
- **iTerm2-Cask** (`brew upgrade --cask iterm2`) — ersetzt das Terminal-Programm.
- **`claude update`** — Self-Update der CLI, beendet die Session.

### 4. Cask + sudo-Falle (.NET, u.a.)
Casks, die nach Systempfaden wie `/usr/local/share/dotnet` installieren, brauchen `sudo`.
Im Hintergrund-/Non-TTY-Terminal scheitert die Passwortabfrage still; brew meldet dennoch
"Upgraded" (Manifest aktualisiert), aber die echten Dateien bleiben alt. **Immer mit dem Tool
selbst gegenprüfen** (`dotnet --version`, `dotnet --list-sdks`). Bei dotnet zusätzlich beachten:
brew-Formula `dotnet` und Cask `dotnet-sdk` können kollidieren — `/opt/homebrew/bin/dotnet` kann
auf den Cask gesymlinkt sein, sodass eine frisch gebaute Formula-Version inaktiv bleibt.

### 5. Update-Strategie (Researcher-Empfehlungen)
- **Kritische Tools `brew pin`** (z.B. eine bestimmte node-/python-Major), damit sie nicht
  ungewollt als Dependency springen.
- **`HOMEBREW_NO_AUTO_UPDATE=1`** für schnellere, vorhersehbare brew-Läufe (kein implizites Index-Update).
- **Manueller Drift-Report statt Auto-Upgrade**: regelmäßig `brew outdated` + `npm outdated -g`
  ansehen und bewusst entscheiden, statt automatisiert alles zu ziehen (Frank will sichtbar + bewusst).
- **Sprach-Runtimes via `mise`** (früher rtx/asdf) statt brew — erlaubt mehrere Versionen
  nebeneinander und projekt-spezifisches Pinning (`.mise.toml`).
- **Major-Bumps einzeln + test-getrieben**: für npm-Pakete `npx npm-check-updates --doctor`
  (führt Tests nach jedem Bump aus); Major (TS, Kotlin, .NET-Major) nie flächendeckend ohne Test.

### 6. Bekannte Breaking-/Vorsicht-Punkte (Stand Juni 2026)
| Tool | Sprung | Einstufung | Hinweis |
|------|--------|-----------|---------|
| TypeScript | 5.x → 6.0 | BREAKING | Nicht global setzen; projekt-lokal + Test |
| Kotlin | 2.3.x → 2.4.0 | VORSICHT | AGP ≥ 8.5.2, Compose-Compiler-Flags prüfen |
| Rust | 1.94 → 1.96 | leichte VORSICHT | nur bei `{self as name}`-Imports relevant |
| Gradle 9.5.1 / Go 1.26.4 / .NET 10.0.301 / Node 26.3 / Python 3.13.x | Patch/Minor | SAFE | — |

---

## Quellen
- Homebrew Docs: `brew pin`, `HOMEBREW_NO_AUTO_UPDATE` (docs.brew.sh)
- mise: mise.jdx.dev (Runtime-Manager, Nachfolger asdf/rtx)
- npm-check-updates `--doctor`: github.com/raineorshine/npm-check-updates
- TypeScript 6.0 / Kotlin 2.4.0 / Rust 1.96 Release Notes (offiziell)
