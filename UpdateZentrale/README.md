# UpdateZentrale

Ein Windows-Programm, das alle wichtigen Werkzeuge an einer Stelle prüft und aktualisiert:
LM Studio (inklusive Engines und Runtimes), Claude Desktop, Codex Desktop, Claude Code CLI,
Codex CLI, Stream Deck sowie die eigenen Werkzeuge OpenLauncher, TVO und CVO.

Start über die Desktop-Verknüpfung **UpdateZentrale** (`create_shortcut.ps1` legt sie an).

---

## Was das Programm macht

| Bereich | Verhalten |
|---|---|
| **Prüfen** | Läuft beim Start automatisch für alle Einträge, danach jederzeit einzeln oder gesammelt. Zeigt installierte Version → verfügbare Version. |
| **Aktualisieren** | Läuft für jedes Programm still durch, ohne dass eine fremde Oberfläche aufgeht. Die Schaltfläche lädt nur dann zum Klick ein, wenn wirklich eine neuere Version vorliegt; ist alles aktuell, steht dort „Aktuell" und sie ist abgeschaltet. |
| **Laufende Programme** | Wird pfadgenau erkannt (inklusive Helferprogramme). Muss ein Programm für das Update beendet werden, fragt die App vorher und startet es danach wieder, wenn es vorher lief. |
| **Autostart** | Wird pro Programm angezeigt und lässt sich auf eine geplante Aufgabe mit Administratorrechten umstellen. |
| **Als Administrator** | Schiebeschalter pro Programm sowie für die UpdateZentrale selbst. |
| **Hell und Dunkel** | Umschalter oben rechts; die Wahl wird gemerkt, die Fenstertitelleiste zieht mit. |
| **Protokoll** | Rechts oben steht die vollständige Ausgabe des jeweiligen Update-Werkzeugs. |
| **Terminal** | Rechts unten eine eingebaute PowerShell-Zeile mit den Rechten der UpdateZentrale, dazu eine Schaltfläche für ein echtes Terminalfenster. |

### Protokolle und Verifikation

Jeder Update-Lauf wird **überprüft und dauerhaft mitgeschrieben** – ein Programm gilt erst dann
als aktualisiert, wenn sich nachweislich etwas geändert hat, nicht schon weil ein Installer
„fertig" gemeldet hat.

Dafür liest die App vor und nach dem Lauf einen *Fingerabdruck* und vergleicht beide. Was das
ist, hängt von der Update-Art ab:

| Art | Fingerabdruck |
|---|---|
| `winget`, `store` | die installierte Version (bei Paket-Apps aus `Get-AppxPackage`) |
| `cli` | die gemeldete Version, bei den LM-Studio-Runtimes stattdessen der Trockenlauf-Plan – der muss danach leer sein |
| `reposkript` | Version **und** Schreibzeit der gebauten Programmdatei (die csproj-Version allein ändert sich beim Neubau nicht) |

Daraus wird eines von fünf Ergebnissen:

| Ergebnis | Bedeutung |
|---|---|
| **Erfolgreich** | Der Fingerabdruck hat sich geändert – das Update ist nachweislich angekommen. |
| **Ausstehend** | Der Installer hat das Update abgelegt, es wird beim nächsten Start des Programms aktiv. Beim nächsten Start der UpdateZentrale wird das rückwirkend bestätigt; kommt es binnen sieben Tagen nicht an, wird es als Fehler ausgewiesen. |
| **Abgebrochen** | Es war nichts offen, oder du hast im Rückfragefenster abgelehnt. |
| **Fehlgeschlagen** | Das Werkzeug selbst hat einen Fehler gemeldet – mit Grund, etwa fehlende Administratorrechte. |
| **Nicht verifiziert** | Der heikelste Fall: Erfolg gemeldet, aber nichts hat sich geändert. Wird rot ausgewiesen statt stillschweigend als Erfolg verbucht. |

Auf der Karte steht danach eine Zeile „Zuletzt: … – Ergebnis", bei Problemen zusätzlich ein rotes
Band mit dem Grund und einer Schaltfläche **Protokoll öffnen**.

Abgelegt wird alles in `%LOCALAPPDATA%\UpdateZentrale\logs\`:

* `updates-JJJJ-MM-TT.log` – ein Tagesprotokoll. Pro Lauf ein Kopf mit Zeit, Programm, Art,
  genauem Befehl, ob erhöht gelaufen wurde und dem Stand vorher; danach die rohe Ausgabe des
  Werkzeugs; am Ende Ergebnis, Begründung, Stand nachher und Exit-Code. Auch Programmausnahmen
  landen hier.
* `verlauf.jsonl` – eine Zeile je Lauf, maschinenlesbar. Daraus lädt die App beim Start, was
  zuletzt passiert ist.

Die Schaltfläche **Protokolle** in der Fußzeile öffnet diesen Ordner.

### Administratorrechte

* Die UpdateZentrale markiert sich beim ersten Start selbst mit dem Windows-Kompatibilitätsschalter
  **RUNASADMIN**. Dadurch startet sie erhöht – egal ob über die Desktop-Verknüpfung, das Startmenü
  oder einen Doppelklick auf die Programmdatei. Der Schalter in der Fußzeile schaltet das wieder ab.
* **Ausnahme Autostart:** Programme, die Administratorrechte brauchen, überspringt Windows im
  normalen Autostart (`HKCU\...\Run`) still – das ist eine Vorgabe der Benutzerkontensteuerung.
  Deshalb bietet jede betroffene Karte die Umstellung auf eine **geplante Aufgabe** an
  (Anmelde-Trigger, höchste Rechte, keine Rückfrage). Der bisherige Autostart-Eintrag wird dabei
  gesichert und beim Zurückstellen wiederhergestellt. Das Anlegen der Aufgabe setzt voraus, dass die
  UpdateZentrale gerade selbst erhöht läuft.
* Das Programmmanifest bleibt bewusst auf `asInvoker`: ein fest erzwungenes
  `requireAdministrator` würde auch jeden Installer erhöht starten, und benutzerbezogene
  Installationen (winget, Squirrel) landen dann im falschen Profil.

---

## Neue Programme hinzufügen (ohne Neubau)

Alles steht in **`programs.json`** im Projektordner. Die App liest die Datei bei jedem Start und
über die Schaltfläche **Neu laden**. Ein neues Programm ist also ein weiteres Objekt in
`programme` – kein Code, kein Build.

```jsonc
{
  "id": "beispiel",                    // eindeutig, dient als Schlüssel für Einstellungen und Aufgaben
  "name": "Beispielprogramm",
  "gruppe": "KI-Anwendungen",          // Überschrift in der Liste; freie Wahl
  "beschreibung": "Kurztext auf der Karte.",
  "art": "winget",                     // siehe Tabelle unten
  "wingetId": "Hersteller.Paket",
  "exePfad": "%LOCALAPPDATA%/Programs/Beispiel/Beispiel.exe",
  "prozesse": ["Beispiel"],            // Hauptprozess(e), ohne .exe
  "helferProzesse": ["BeispielTray"],  // Helfer, die den Installer sonst blockieren
  "beendenVorUpdate": true,
  "neuStartenNachUpdate": true,
  "akzent": "#3FA9F5"                  // Farbe des Kennzeichens
}
```

Pfade werden mit **Schrägstrichen** geschrieben (kein Escaping nötig); `%VARIABLEN%` werden
aufgelöst und Windows bekommt intern die gewohnte Schreibweise.

Prozessnamen werden immer gegen den Ordner aus `exePfad` geprüft. Kollidieren zwei Programme im
Namen und lässt sich das nicht über den Pfad trennen (etwa `claude` als CLI und als Store-App),
bleibt `prozesse` besser leer – lieber kein Merkmal als ein falsches.

### Die vier Update-Arten

| `art` | Wofür | Pflichtfelder | Prüfung | Update |
|---|---|---|---|---|
| `winget` | Alles, was winget kennt | `wingetId` | `winget list --id …` | `winget upgrade --id … --silent` |
| `cli` | Werkzeuge, die sich selbst aktualisieren | `exePfad`, `updateArgumente` | `pruefArgumente` (Trockenlauf) **oder** `npmPaket` | `exePfad updateArgumente` |
| `store` | Store-signierte MSIX-Pakete | `appxName`, `packageFamilyName`, `storeProduktId` | `Get-AppxPackage` + `winget upgrade`-Liste | `winget upgrade --id <ProduktId> --source msstore --silent` |
| `reposkript` | Eigene Werkzeuge in `~/proggs` | `skript`, `statusPraefix` | `git rev-list HEAD..origin/main` + csproj-Version gegen gebaute Exe | ruft das vorhandene PowerShell-Skript auf |

Weitere optionale Felder:

| Feld | Bedeutung |
|---|---|
| `exePfadAlternativen` | Weitere Orte für dieselbe Programmdatei; der erste Pfad, den es auf diesem Rechner wirklich gibt, gewinnt. So läuft derselbe Katalog auf mehreren Rechnern (LM Studio unter `Program Files` statt `%LOCALAPPDATA%`, CLIs als npm-Shim statt nativem Installer). |
| `ausblendenWennFehlt` | Blendet die Karte aus, wenn auf diesem Rechner keiner der Pfadkandidaten existiert – für Programme, die nur auf einem Teil der Geräte benutzt werden. |
| `versionsArgumente` | Argumente, die die installierte Version ausgeben (z. B. `--version`) |
| `pruefArgumente` | Trockenlauf-Befehl; Zeilen mit `→` bzw. `->` gelten als geplante Updates |
| `npmPaket` | Vergleichsquelle für die neueste Version, wenn das Werkzeug auf npm liegt |
| `storeProduktId` | Store-Produkt-ID für das stille Update über die msstore-Quelle |
| `appxAnwendungsId` | Anwendungs-ID im MSIX-Paket (fast immer `App`) |
| `startArgumente` | Argumente beim Start über die Schaltfläche „Starten" und in der geplanten Aufgabe |
| `zeitlimitMinuten` | Zeitlimit des Update-Laufs (Standard 20) |
| `dialogWartezeitSekunden` | Nur `reposkript`: wie lange auf den Ja/Nein-Klick gewartet wird |
| `hinweis` | Kursive Zeile auf der Karte |
| `pfeilZaehlen` | Dokumentiert, dass die Prüfausgabe ihre Updates als `alt → neu` auflistet |

---

## Besonderheiten der eingetragenen Programme

* **LM Studio Engines & Runtimes** – eigener Eintrag. Geprüft wird mit
  `lms runtime update --all --dry-run`, aktualisiert mit `lms runtime update --all --yes`. Deckt
  llama.cpp für CPU/AVX2, NVIDIA CUDA, CUDA12 und Vulkan ab. Lädt teils mehrere Gigabyte;
  LM Studio darf dabei laufen.
* **Claude Desktop** – winget führt das Paket als `Anthropic.Claude` mit dem offiziellen
  Installer von `downloads.claude.ai`. Das Update läuft still und vollautomatisch. Der Installer
  **legt die neue Fassung aber nur bereit** und schaltet sie erst beim nächsten Start von Claude
  Desktop um. Bis dahin meldet jede Prüfung weiter die alte Version – früher sah das aus, als sei
  das Update gar nicht angekommen. Die Karte weist das jetzt als **Ausstehend** aus, bietet
  „Jetzt starten und übernehmen" an und bestätigt es später rückwirkend.
* **Codex Desktop** – das installierte Paket ist Store-signiert und im Microsoft Store als
  „ChatGPT" gelistet (Produkt-ID `9PLM9XGG6VKS`, Publisher-Seite openai.com/codex). Das Update
  läuft still über die Store-Quelle von winget; der Weg über die App war nur die sichtbare
  Variante desselben Mechanismus.
* **Paket-Apps und Administratorrechte** – Windows lässt MSIX-Pakete grundsätzlich nicht erhöht
  laufen; für sie gibt es keinen Kompatibilitätsschalter. Deshalb zeigen diese Karten statt des
  Schalters eine Erklärung. Das Update selbst braucht sehr wohl Adminrechte – die bringt die
  UpdateZentrale mit.
* **Codex Desktop vs. Codex CLI** – zwei getrennte Installationen: das MSIX-Paket und der
  Standalone-Build unter `~/.codex/packages`. Deshalb zwei Karten mit unterschiedlichen Wegen.
* **Claude Code CLI** – laufende Sessions werden nie beendet; das Update greift beim nächsten Start.
* **Stream Deck** – die Helferdienste werden beim Update mitbeendet, sonst hängt der Installer.
* **OpenLauncher / TVO / CVO** – die App ruft ausschließlich die vorhandenen Skripte auf
  (`update-launcher.ps1`, `rebuild-overlay.ps1`), **nie mit `-Force`**, und beendet deren Prozesse
  niemals selbst. Jedes Skript zeigt sein eigenes Ja/Nein-Fenster; die Antwort (`started`,
  `already-current`, `cancelled`, `no-answer`) wird ausgelesen und im Klartext angezeigt.

---

## Dateien

| Datei | Zweck |
|---|---|
| `programs.json` | Der Katalog. Hier werden Programme hinzugefügt. |
| `Models/` | Katalog-Eintrag und Prüfergebnis |
| `Services/` | Pfade, Prozesse, Registry, Aufgabenplanung, Rechte, Terminal, Kommandozeile, Einstellungen, Darstellung, Konverter |
| `Providers/` | Je eine Implementierung pro Update-Art |
| `ViewModels/` | Karten- und Fensterlogik |
| `HauptFenster.xaml` | Oberfläche |
| `Theme.xaml` | Formen und Typografie (ohne Farben) |
| `Themes/Dunkel.xaml`, `Themes/Hell.xaml` | Die beiden Farbsätze; gleiche Schlüssel, zur Laufzeit tauschbar |
| `create_shortcut.ps1` | Desktop-Verknüpfung |
| `make_icon.ps1` | Erzeugt `app.ico` |

Benutzereinstellungen (Administrator-Schalter, Hell/Dunkel, gesicherte Autostart-Einträge) liegen
außerhalb des Repos in `%LOCALAPPDATA%\UpdateZentrale\settings.json`, damit `programs.json`
sauber bleibt.

## Bauen

```powershell
cd ~\proggs\UpdateZentrale
dotnet build -c Release
pwsh -File .\create_shortcut.ps1
```
