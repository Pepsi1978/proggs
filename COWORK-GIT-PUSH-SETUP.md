# Cowork (Claude Desktop App): Dauerhaft pushen

> **Problem:** In Cowork kannst du committen, aber `git push` scheitert
> (`could not read Username for github.com`) — und Workarounds halten nur eine Session.
>
> **Recherche-Stand:** 2026-06-15, 7 parallele Researcher (offizielle Anthropic-/GitHub-Doku +
> GitHub-Issues + Reverse-Engineering-Analysen). Siehe auch `bugs/claude-tooling/cowork.md`
> (Volltext) und `best-practices/projekt-code/claude-tooling/best-practices-cowork.md`.

---

## Warum es so ist (gesichert, hohe Konfidenz)

- Cowork führt Git in einer **isolierten Linux-VM** aus. Diese VM startet bei **jeder Session
  frisch** — das VM-Heimverzeichnis (`~/.git-credentials`, `~/.ssh`, `~/.gitconfig`) wird
  jedes Mal zurückgesetzt. Das ist Absicht ("clean VM state", offiziell).
- Es gibt in Cowork **keinen** offiziellen Push-Weg: Der GitHub-Connector kann nur
  API-Aktionen (Dateien lesen, PRs/Issues), **nicht** `git push`. Den Push-Proxy gibt es nur
  bei "Claude Code on the web", nicht in der Cowork-Desktop-VM.
- Es gibt **keinen** Secret-Speicher und **keinen** Startup-Hook in Cowork.
- **Persistent ist nur, was im gemounteten Ordner liegt** — also `proggs` selbst (der echte
  Windows-Ordner). Die VM-Mount-Pfade wechseln zwischen Sessions → **relative** Pfade nutzen.

---

## Die Lösung: Zugangsdaten in `.git/`

Der `.git/`-Ordner deines Repos liegt im echten Windows-Ordner (**überlebt jede Session**) und
wird von Git **per Definition nie committet** (landet also nie auf GitHub). Genau dort legen
wir die Anmeldung ab — mit einem **relativen** Pfad und **ohne** die Remote-URL zu ändern.

> **Warum nicht SSH oder Token-in-der-Remote-URL?** Deine `.git/config` ist zwischen Cowork
> und deinem normalen Windows-Terminal **geteilt** (derselbe Ordner). Würde man dort das Remote
> auf SSH oder eine Token-URL umstellen, betrifft das auch dein funktionierendes Terminal-Setup.
> `credential.helper store` ist dagegen **additiv** und stört das Terminal nicht.

### Schritt 1 — Token (falls noch keins)
Fein-granulares Token auf `https://github.com/settings/personal-access-tokens/new`:
- **Only select repositories** → `Pepsi1978/proggs`
- **Repository permissions → Contents** → **Read and write**
  (Metadata: Read wird automatisch gesetzt — Pflicht, sonst `403` beim Push.)
- **Expiration:** ruhig das Maximum (bis 366 Tage). Danach einmal erneuern.

### Schritt 2 — In Cowork einrichten (einmalig, hält dann dauerhaft)
Gib dem Claude-in-Cowork **im Chat** (nicht in eine Datei) diesen Auftrag mit deinem **echten** Token:

```text
Richte git push für proggs DAUERHAFT ein (muss jede Session überleben).
Lege die Zugangsdaten in .git/ ab (überlebt Sessions, wird nie committet),
nutze einen relativen Pfad und ändere die Remote-URL NICHT. Führe im proggs-Repo aus:

git config credential.helper 'store --file=.git/credentials'
printf 'https://Pepsi1978:%s@github.com\n' 'DEIN_ECHTER_TOKEN' > .git/credentials
chmod 600 .git/credentials

Test ohne etwas zu verändern:
git ls-remote origin -h refs/heads/main
```

Erscheint eine Zeile mit `refs/heads/main`, ist der dauerhafte Push-Zugang eingerichtet.
Ab jetzt geht in Cowork:

```bash
git add <dateien>
git commit -m "#NNN - Beschreibung"
git fetch origin && git rebase origin/main
git push
```

---

## Wichtig

- **Dauerhaft, aber nicht ewig:** Das Token läuft nach maximal 366 Tagen ab. Dann ein neues
  erstellen (gleiche Einstellungen) und Schritt 2 einmal wiederholen. Ein SSH-Deploy-Key liefe
  nie ab, würde aber das geteilte Terminal-Setup stören — darum dieser Weg.
- **`proggs` ist privat** → GitHub sperrt ein geleaktes Token **nicht** automatisch (das tut es
  nur bei öffentlichen Repos). Darum: Token nur in Cowork eingeben, nirgends sonst; bei Verdacht
  auf `https://github.com/settings/personal-access-tokens` widerrufen (Revoke).
- **Falls der Test fehlschlägt:** Auf manchen Windows-Mounts sieht die Cowork-bash die
  `.git/`-Dateien nicht zuverlässig (bekannter Mount-Bug). Dann den `git config`-Schritt zu
  Session-Beginn erneut ausführen lassen — Token bleibt gültig.
- **Parallele Sessions:** immer `git fetch origin && git rebase origin/main` vor dem Push,
  nie `--force`.

---

## Praktische Realität auf Windows (live bestätigt 2026-06-15)

Die dauerhafte **Anmeldung** funktioniert (Schreibzugang real getestet: Test-Tag gepusht,
Rückgabecode 0). **Aber:** Auf dem gemounteten Windows-`.git` kann die Cowork-VM ihre eigenen
`.lock`-Hilfsdateien zwar anlegen, **aber nicht wieder löschen** ("Operation not permitted") —
eine Eigenschaft der Cowork-Windows-Einbindung, nicht deines Setups. Folge: Ein `git commit`
oder `git push` **aus der VM** kann an einer liegengebliebenen Lock-Datei hängen bleiben.

**Empfehlung für den Alltag:**
- **Committen/Pushen** zuverlässig aus deinem **normalen Windows-Terminal** in `C:\Users\barwa\proggs`
  (dort gibt es das Lock-Problem nicht; die gespeicherte Anmeldung gilt dank geteilter `.git/config`
  auch dort — du musst dich nie einloggen).
- **Cowork** zum Lesen/Bearbeiten nutzen. Push aus der VM geht zur Not auch, braucht dann aber ein
  Aufräumen vom Windows-Terminal aus: `rm .git/*.lock` (NIEMALS `.git/claude-multi-session.lock`
  löschen — das ist ein Hook-Lock, kein Git-Lock).

Damit ist dein eigentliches Ziel erreicht (dauerhafte Push-Anmeldung steht), und der zuverlässige
Weg fürs tägliche Pushen ist klar.

## Alternative ganz ohne Token
In Cowork committen, und das Pushen aus dem **normalen Claude Code im Terminal** (oder dem
Code-Tab der Desktop-App) erledigen — dort sind deine Zugangsdaten vorhanden.
