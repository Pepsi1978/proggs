# UpdateZentrale: Abgebrochenes Vorspulen blockiert das Repo und alle Repo-Updates (02.11.2026)

> Behoben in UpdateZentrale 1.3.72 und Updater-Zentrale macOS 1.0.44. Geschwisterfall: `2026-11-01-updatezentrale-npm-kill-zerstoert-cli.md`.

## Symptom

TVO und CVO scheiterten im Sammellauf mit: „git pull --ff-only ist fehlgeschlagen (lokale Änderungen oder abweichender Verlauf) – das Repo muss erst von Hand abgeglichen werden.“ Im Protokoll stand dagegen `Unable to create '.git/index.lock': File exists` und `fatal: Cannot autostash`. `git status` zeigte 3.715 Commits Rückstand und rund 3.800 scheinbar lokale Änderungen, darunter 2.425 gelöschte Dateien.

## Ursache

1. **Ein Vorspulen wurde mitten im Lauf beendet.** Um 20:16:03 entstanden `ORIG_HEAD` und die `index.lock`, bis 20:16:08 schrieb git den Arbeitsbaum. Der Lauf endete, bevor HEAD und Index nachgezogen waren. Der Rechner war um 20:12:56 gestartet.
2. **Git setzt HEAD zuletzt.** Der Vergleich über einen temporären Index zeigte: Der Arbeitsbaum entsprach `origin/main` bis auf 20 Pfade. Die vielen „Änderungen“ waren also keine, sondern nur der alte HEAD gegen den neuen Arbeitsbaum.
3. **Der Abbruch lag beim Holen der LFS-Dateien.** Von den neuen Dateien fehlten genau drei auf der Platte, und alle drei sind LFS-Zeiger. Dort lädt git über das Netz.
4. **Die Meldung riet falsch.** Der Text „lokale Änderungen oder abweichender Verlauf“ war fest verdrahtet und nannte die Sperre nicht.
5. **`git pull` stasht je nach Rechner.** Mit `pull.rebase=true` und `rebase.autostash=true` versuchte selbst `pull --ff-only` einen Autostash und scheiterte schon daran.

**Nicht belegt:** Welcher Prozess den Lauf um 20:16 beendet hat. OpenLauncher startete erst um 20:40, die UpdateZentrale um 20:46, und beide protokollieren für 20:16 nichts. Geplante Aufgaben und Autostarts enthalten keinen passenden Git-Aufruf.

## Vorbeugung

Die Reparatur gilt für die ganze Fehlerart und hängt nicht vom Verursacher ab. Alles liegt in `Services/RepoAbgleich` (Windows und macOS):

- **Verwaiste Sperre:** Vor dem Vorspulen wird `.git/index.lock` nur entfernt, wenn kein Git-Prozess läuft und sie mindestens 2 Minuten alt ist. Läuft git noch, wird bis zu 60 Sekunden gewartet. Danach meldet der Git-Aufruf die Sperre selbst.
- **Halbes Vorspulen:** Verweigert git das Vorspulen, vergleicht ein temporärer Index den Arbeitsbaum mit dem Upstream. Weicht er von HEAD um mindestens 50 Dateien ab und vom Upstream höchstens um ein Zehntel davon, vollendet `git reset --mixed @{u}` das Vorspulen. Der Arbeitsbaum bleibt unberührt, uncommittete Änderungen überleben. Nur noch nicht geschriebene Dateien kommen aus dem Upstream.
- **Zeitlimit:** Das Vorspulen hat 15 statt 2 bis 3 Minuten Zeit, damit ein großer Rückstand mit LFS-Dateien nicht selbst abgewürgt wird.
- **Ein Weg:** `AktualisierenAsync` nutzt `merge --ff-only --autostash` statt `pull --ff-only`, unabhängig von `pull.rebase`. Der Autostash bleibt, weil er bisher über die Git-Einstellung ohnehin griff. Die Prüfung stasht weiterhin nie.
- **Meldung:** `VorspulGrund` nennt die Sperre, wenn sie der Grund ist.
- **Bewusst nicht:** `GIT_LFS_SKIP_SMUDGE`. Es hätte das Hängen beim Laden verhindert, aber die Android-Builds fänden dann nur Zeiger statt der echten Bibliotheken.

## Vorgehen, wenn es wieder auftritt

```powershell
# Nur lesen: Arbeitsbaum gegen origin/main, ohne den echten Index anzufassen
$env:GIT_INDEX_FILE = "$env:TEMP\vergleich-index"; git read-tree origin/main
git status --porcelain --no-renames | Where-Object { $_[1] -ne ' ' }
Remove-Item Env:GIT_INDEX_FILE, "$env:TEMP\vergleich-index"
```

Sind das nur wenige Pfade: Prüfen, dass kein Git-Prozess läuft, `.git/index.lock` löschen und `git reset --mixed origin/main` ausführen. Nie `reset --hard` oder `pull --autostash`: Das erste verwirft lokale Arbeit, das zweite stasht tausende Scheinänderungen.

## Dahinter lagen zwei weitere Fehler

Beide kamen erst zum Vorschein, als das Repo wieder stand. Sie betrafen jedes Update von einem sehr alten Stand aus.

- **CVO: falsches SDK.** `ClaudeVoiceOverlay-Windows/publish.ps1` gab `~/.dotnet` fest den Vorrang. Dort liegt nur SDK 8.0.418, das Projekt braucht .NET 10: `NETSDK1045`. Jetzt gewinnt die Installation mit dem neuesten SDK, `~/.dotnet` nur bei Gleichstand.
- **TVO: alte Fassung galt als hängend.** Das laufende TVO vom Juni kennt die Endpunkte `/deployment/prepare` und `/recording/status` noch nicht und antwortet mit 404, `/autoenter/status` antwortet normal. `voice-overlay-deploy-guard.ps1` wertete jede Fehlantwort bei offenem Port als Hänger und brach ab. So blockierte die alte Fassung genau das Update, das sie ablösen sollte. Ein 404 zählt jetzt wie „kein Schutz angeboten“, in `.ps1` und `.sh`. Ein echter Hänger (keine Antwort) bricht weiter fail-closed ab.

## Offen

- `OpenLauncher/Services/RepoSync.cs` und das Mac-Gegenstück beenden `git pull` nach 20 Sekunden mit dem ganzen Prozessbaum. Bei großem Rückstand erzeugt das genau diesen Zustand. Für den Lauf vom 02.11. ist es nicht belegt und wurde in dieser Sitzung nicht geändert.
