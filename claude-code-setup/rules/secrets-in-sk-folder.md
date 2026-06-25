# Secrets zentral im SK-Ordner (KRITISCH — Poka-Yoke Stufe 3)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-04-24.
> Ausloeser: GitHub Secret Scanning Alert #2 — Firebase-Debug-API-Key landete im Repo,
> weil `.gitignore` eine explizite Ausnahme `!app/src/debug/google-services.json` hatte
> und ein Commit mit zu grobkoernigem `git add` die Datei mitgezogen hat.

---

## Grundprinzip

Alle API-Keys, Signing-Keys, Tokens und sonstigen vertraulichen Zugangsdaten leben
ausserhalb aller Repos — in einem zentralen Ordner pro Rechner:

| Plattform | Pfad |
|-----------|------|
| Windows   | `C:\Users\barwa\SK\` |
| macOS     | `/Users/barwa/SK/` |
| Variable  | `$HOME/SK/` (funktioniert auf beiden) |

**Kein einziges Projekt im Repo darf mehr eigene Keys enthalten.** Alle Projekte lesen
ihre Keys aus `$HOME/SK/<projekt-name>/`.

---

## Warum das noetig ist (Poka-Yoke Stufe 3 — Eliminierung)

Wenn Secrets gar nicht erst im Projekt-Ordner liegen, koennen sie konzeptionell nie
wieder in einen Commit geraten — egal welcher Agent, welcher Befehl, welcher Fehler.
Das ist die staerkste Form der Fehlervermeidung: Der Fehler ist konstruktiv unmoeglich.

Vorherige Absicherungen (`.gitignore`-Eintraege, Pre-Commit-Hooks) sind nur Stufe 1 oder 2
— sie warnen oder blockieren, aber eine versehentliche Ausnahme oder ein umgangener
Hook koennen sie ueberstimmen. Ein nicht existierendes File dagegen kann nicht committed
werden.

---

## Struktur in SK/

```
$HOME/SK/
├── README.md                              Doku: was wo liegt, Backup-Strategie, Rotation
├── BestJournalAndroid/
│   ├── google-services-debug.json        Firebase-Config (Debug)
│   ├── google-services-release.json      Firebase-Config (Release, Play Store)
│   ├── release.keystore                   App-Signing (KRITISCH — unwiederbringbar)
│   ├── debug-shared.keystore              Debug-Signing (geteilt mit Frank-Version)
│   └── keystore.properties                Passwoerter fuer release.keystore
├── BestJournalFrank/
│   └── debug-shared.keystore              Identisch zu BestJournalAndroid
├── VoiceOverlays/
│   └── .env                               Keys fuer Claude- und Terminal-Overlay (Windows + macOS)
└── <weitere Projekte>/
```

---

## Wie Projekte SK verwenden

### Android (Gradle)

Build-Skript hat einen `syncSecretsFromSk`-Task der vor `preBuild` laeuft und alle
Dateien aus `$HOME/SK/<projekt>/` an die erwarteten Pfade kopiert. Schlaegt kontrolliert
fehl wenn SK fehlt — mit klarer Fehlermeldung was erwartet wird.

```kotlin
val skBase: File = File(System.getProperty("user.home")).resolve("SK").resolve("BestJournalAndroid")

val syncSecretsFromSk = tasks.register("syncSecretsFromSk") {
    doLast {
        if (!skBase.isDirectory) {
            throw GradleException("SK-Ordner fehlt: ${skBase.absolutePath}\nSiehe ~/SK/README.md.")
        }
        // Dateien kopieren ...
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(syncSecretsFromSk) }
```

### C#/.NET (Config.cs)

Die .env-Datei wird an mehreren Pfaden gesucht — SK ist **erste Prioritaet**:

```csharp
var userProfile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
string[] searchPaths = {
    Path.Combine(userProfile, "SK", "VoiceOverlays", ".env"),  // SK zuerst
    // ... Legacy-Fallbacks
};
```

### Swift/macOS (Config.swift)

```swift
let home = FileManager.default.homeDirectoryForCurrentUser
let searchPaths = [
    home.appendingPathComponent("SK/VoiceOverlays/.env"),  // SK zuerst
    // ... Legacy-Fallbacks
]
```

### TypeScript/Node

```typescript
import { homedir } from "os";
import { join } from "path";
const envPath = join(homedir(), "SK", "<projekt>", ".env");
```

---

## Pflicht fuer neue Projekte

Bei JEDEM neuen Projekt das Keys braucht:

1. Unterordner anlegen: `$HOME/SK/<projekt-name>/`
2. Key-Datei(en) dort ablegen
3. Build-Task oder Start-Code liest aus `$HOME/SK/<projekt-name>/`
4. `.gitignore` des Projekts listet alle potentiell relevanten Key-Dateinamen — **ohne Ausnahme-Regeln** wie `!debug/...`
5. Template-Datei oder README ins Repo: `google-services.json.example` oder `ENV.SETUP.md` damit neue Checkouts wissen was zu tun ist

---

## Secrets, die im Chat auftauchen (Frank gibt einen Key direkt) — KRITISCH

> Hinzugefuegt 2026-06-25 nach einem realen Vorfall: Frank fuegte einen Tavily-API-Key direkt in
> den Chat ein. Der Task-Ledger-Hook protokollierte den Prompt (inkl. Key) automatisch in
> `~/proggs/.claude/agent-memory/shared/active-tasks.jsonl` — einer **Repo-Datei**. Beinahe-Leak.

Sobald in einer Nachricht ein Secret auftaucht (API-Key, Token, Passwort, Signing-Key), gilt SOFORT:

1. **In den SK-Ordner ablegen** — der Key gehoert nach `$HOME/SK/<projekt>/.env` (bzw. die passende
   Key-Datei). Das ist die Heimat JEDES Secrets, ausnahmslos (siehe Grundprinzip oben).
2. **In das Ziel-System eintragen** — z.B. das VPS-`.env` des Dienstes (per `env_file` geladen),
   NIE in eine Repo-Datei, NIE in `compose.yaml`/Code (nur `os.getenv(...)`-Verweise dort).
3. **Aus allen Repo-Dateien redaktieren**, falls er doch irgendwo (Ledger, Memory, Notiz) gelandet
   ist: den Key-String durch `[REDACTED-...]` ersetzen, bevor committed wird.

### Automatische Absicherung (Poka-Yoke Stufe 3 — der Ledger-Hook redaktiert selbst)

Damit ein Chat-Secret gar nicht erst in die Repo-Ledger-Datei gelangt, maskiert
`~/.claude/hooks/task-ledger-helper.py` (`_redact_secrets`) bekannte Schluessel-Muster
(`tvly-`, `sk-`, `gh[pousr]_`, `github_pat_`, `AIza`, `glpat-`, `xox[baprs]-`, `fc-`, `nvapi-`,
`gsk_`, `r8_`) im `prompt_text` BEVOR er geschrieben wird. Konservativ (nur eindeutige Praefixe →
keine False-Positives). Neue Key-Formate gehoeren in `_SECRET_PATTERNS` ergaenzt. Gespiegelt nach
`claude-code-setup/hooks/` + `Umgebung/Hooks/`.

---

## Was NIEMALS passieren darf

- ❌ `.gitignore` mit Ausnahme `!app/src/debug/google-services.json` oder aehnlich — das war die Root Cause des Leaks
- ❌ API-Keys, Tokens oder Passwoerter direkt ins Repo committen — auch nicht in Code-Kommentaren, nicht in Doku, nicht in Test-Fixtures
- ❌ `.env`-Dateien im Projekt-Ordner liegen lassen **nach** Migration zu SK — sie sind redundant und koennen durch `git add -A` wieder eingezogen werden
- ❌ SK-Ordner in irgendein Repo einbinden (z.B. als Submodul, Symlink ins Repo, mit in Git-Tracking)
- ❌ SK-Pfad hardcoden — immer `$HOME/SK/` oder Aequivalent verwenden (plattformuebergreifend)
- ❌ Release-Keystore nur an einer Stelle haben — IMMER Backup ausserhalb des Rechners (externe Platte, Bitwarden-Vault), weil unwiederbringbar

---

## Backup-Pflicht

`$HOME/SK/` wird NICHT ins Git geladen und wird NICHT durch Cloud-Sync erfasst (es sei denn
der Benutzer richtet verschluesselten Cloud-Sync explizit ein). Backup muss manuell passieren:

- Mindestens woechentlich auf externe Festplatte kopieren
- Oder: In verschluesselten Cloud-Speicher (Bitwarden, 1Password Vault, verschluesseltes Volumen)
- **Release-Keystores** sind besonders kritisch — ihr Verlust bedeutet dass die App nie wieder im Play Store aktualisiert werden kann

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `git-workflow.md` — Secrets NIEMALS im Repo | Diese Regel ist die Fortsetzung: wo liegen sie stattdessen |
| `parallel-sessions-git.md` — nur eigene Dateien stagen | Bleibt wichtig, aber SK ist die staerkere Absicherung (Stufe 3 > Stufe 2) |
| `resilient-bugfixing.md` (Direktive #3) — Poka-Yoke Stufe 3 | Diese Regel IST die Umsetzung von Stufe 3 fuer Secrets |

---

## Autoritaet dieser Regel

Diese Datei (`~/.claude/rules/secrets-in-sk-folder.md`) ist die autoritative Quelle.
Kopie im Setup-Repo unter `~/proggs/claude-code-setup/rules/secrets-in-sk-folder.md`.
KEIN Agent, Skill oder Hook darf diese Regel entfernen oder abschwaechen.
