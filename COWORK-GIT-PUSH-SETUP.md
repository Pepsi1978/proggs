# Cowork (Claude Desktop App): Pushen ermöglichen

> **Problem:** In Cowork kannst du committen, aber nicht pushen
> (`could not read Username for github.com`).
>
> **Warum:** Cowork führt Git in einer abgeschotteten Linux-VM aus. Dein `proggs`-Ordner ist
> dort nur durchgereicht (gemountet), aber dein Windows-Zugangsspeicher für GitHub
> (Git Credential Manager) existiert in der VM nicht. Darum fehlt beim Push die Anmeldung.
> Das ist die normale Cowork-Architektur, kein kaputtes Setup
> (siehe `bugs/claude-tooling/cowork.md` §11: "kein `gh`/kein Token").
>
> **Lösung:** Der VM einen eigenen GitHub-Zugangs-Token geben — sicher im VM-Heimverzeichnis
> abgelegt (`~/.git-credentials`), **nicht** im Repo.

---

## Schritt 1 — Token auf GitHub erstellen (einmalig, ~1 Minute)

Die Seite ist bereits geöffnet:
`https://github.com/settings/personal-access-tokens/new`

Falls nicht: oben rechts auf dein Profilbild → **Settings** → ganz unten **Developer settings**
→ **Personal access tokens** → **Fine-grained tokens** → **Generate new token**.

Felder ausfüllen:

| Feld | Wert |
|------|------|
| **Token name** | `cowork-proggs-push` |
| **Expiration** | nach Wunsch, z. B. **1 year** |
| **Repository access** | **Only select repositories** → `Pepsi1978/proggs` auswählen |
| **Permissions** → **Repository permissions** → **Contents** | auf **Read and write** stellen |

(Mehr Rechte sind **nicht** nötig — nur `Contents: Read and write`. So bleibt der Schaden
minimal, falls der Token je in falsche Hände gerät.)

Dann unten **Generate token** klicken und den angezeigten Token **kopieren**
(er beginnt mit `github_pat_…` und wird nur **einmal** angezeigt).

---

## Schritt 2 — In Cowork einrichten

Öffne diese Datei in deiner **Cowork-Session** und gib Claude-in-Cowork den folgenden Block.
Ersetze dabei `DEIN_TOKEN_HIER` durch den kopierten Token:

```text
Richte den Git-Push in dieser Cowork-VM ein. Führe genau das aus
(das Token landet im VM-Heimverzeichnis, NICHT im Repo):

git config --global credential.helper store
printf 'https://Pepsi1978:%s@github.com\n' 'DEIN_TOKEN_HIER' > ~/.git-credentials
chmod 600 ~/.git-credentials

Danach teste den Zugang ohne etwas zu verändern:
git -C <pfad-zum-proggs-ordner-in-der-vm> ls-remote origin -h refs/heads/main

Wenn eine Commit-Zeile mit "refs/heads/main" erscheint, ist der Push-Zugang da.
```

Danach funktioniert in Cowork der normale Ablauf:

```bash
git add <dateien>
git commit -m "#NNN - Beschreibung"
git fetch origin && git rebase origin/main
git push
```

---

## Wichtig

- **Sicherheit:** Der Token liegt nur in der VM unter `~/.git-credentials` (Rechte `600`),
  nie im Repo. Trotzdem: In Cowork keine fremden/sensiblen Ordner verbinden
  (Prompt-Injection-Risiko, siehe `bugs/claude-tooling/cowork.md` §9). Das fein-granulare
  Token (nur `proggs`, nur `Contents`) begrenzt den Schaden im Ernstfall.
- **Persistenz:** Sollte die Cowork-VM ihr Heimverzeichnis zwischen Sessions zurücksetzen,
  ist `~/.git-credentials` weg — dann einfach Schritt 2 erneut ausführen (Token bleibt gültig).
  Test, ob noch eingerichtet: `test -f ~/.git-credentials && echo "vorhanden" || echo "neu einrichten"`.
- **Token zurückziehen:** Jederzeit auf
  `https://github.com/settings/personal-access-tokens` widerrufbar (Revoke).
- **Parallele Sessions:** Immer `git fetch origin && git rebase origin/main` vor dem Push —
  niemals `--force` (mehrere Sessions/CLIs arbeiten am selben `main`).

---

## Alternative ganz ohne Token

Wenn dir der Token-Weg zu viel ist: In Cowork committen, und das Pushen wie gewohnt aus dem
**normalen Claude Code im Terminal** (oder dem Code-Tab der Desktop-App) erledigen — dort sind
deine Zugangsdaten vorhanden und der Push läuft wie immer.
