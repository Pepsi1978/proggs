# UpdateZentrale

Ein Windows-Programm, das alle wichtigen Werkzeuge an einer Stelle prüft und aktualisiert:
LM Studio (inklusive Engines und Runtimes), Claude Code CLI,
Codex CLI, Kimi Code CLI, Antigravity CLI, OpenCode, GitHub CLI, PowerShell 7, die Kommandozeilen-Werkzeuge (ripgrep, fd,
bat, fzf, jq, delta, lazygit, ShellCheck, shfmt, Trivy, FFmpeg, Rclone, scrcpy, uv), Stream Deck, VPN (WireGuard,
Windscribe), die komplette Programmierumgebung (.NET 8 und 10, Node.js, Deno, Java, Gradle, Kotlin, Rust, Go,
Python, Visual Studio, VS Code, Android Studio und SDK, Git, Docker, Windows SDK, npm-Werkzeuge …), winget selbst
sowie die eigenen Werkzeuge OpenLauncher, TVO, CVO und die UpdateZentrale.

Start über die Verknüpfung **UpdateZentrale** auf dem Desktop oder im Startmenü (`create_shortcut.ps1` legt beide an).

---

## Was das Programm macht

| Bereich | Verhalten |
|---|---|
| **Prüfen** | Alle Einträge werden gleichzeitig geprüft (nur winget läuft in einer eigenen Reihe); die Fußzeile zeigt „x von n fertig". Zeigt installierte Version → verfügbare Version. Das Ergebnis wird gemerkt: Beim nächsten Öffnen steht es sofort da, neu geprüft wird nur, was älter als 60 Minuten ist – die eigenen Werkzeuge immer. |
| **Repo-Abgleich** | Vor der Prüfung der eigenen Werkzeuge (OpenLauncher, TVO, CVO, UpdateZentrale) wird `~/proggs` einmal mit GitHub abgeglichen: `git fetch` und Vorspulen (`--ff-only`), nie ein Merge. Erst dadurch ist bekannt, welche Version im Repo wartet. Lässt sich das Repo nicht vorspulen (lokale Änderungen im Weg), steht das in der Fußzeile, und die Karten zählen weiter die wartenden Commits. |
| **Prüfung beim Windows-Start** | Schalter in der Fußzeile, standardmäßig an. 45 Sekunden nach der Anmeldung startet die UpdateZentrale unsichtbar (`--hintergrund`), gleicht das Repo ab, prüft alles und beendet sich wieder. Wird sie währenddessen von Hand geöffnet, erscheint das Fenster mit dem laufenden Fortschritt. Eingerichtet wird das als geplante Aufgabe „UpdateZentrale - Hintergrund" (dafür muss die App einmal mit Administratorrechten laufen), ohne den Schalter „Immer als Administrator" als normaler Autostart-Eintrag. |
| **Aktualisieren** | Läuft für jedes Programm still durch, ohne dass eine fremde Oberfläche aufgeht. Die Schaltfläche lädt nur dann zum Klick ein, wenn wirklich eine neuere Version vorliegt; ist alles aktuell, steht dort „Aktuell" und sie ist abgeschaltet. |
| **Sammelknöpfe** | Zwei unabhängige Knöpfe: „⚠ CLI-kritische Updates installieren“ und „Updates installieren · nicht CLI-kritisch“. Je eine Rückfrage mit der geplanten Reihenfolge, dann in Phasen (siehe „Zwei Sammelknöpfe“). Unabhängige Selbst-Updater laufen parallel neben der seriellen Bahn. |
| **Dauer und Sortierung** | Jede Karte zeigt, wie lange ihr Update zuletzt gedauert hat (Median der letzten drei echten Updates) oder eine Schätzung. Der Schalter „⏱ nach Dauer" sortiert innerhalb jeder Gruppe die kürzesten nach oben; die Gruppen bleiben zusammen. |
| **Laufende Programme** | Wird pfadgenau erkannt (inklusive Helferprogramme). Muss ein Programm für das Update beendet werden, fragt die App vorher und startet es danach wieder, wenn es vorher lief. |
| **Autostart** | Wird pro Programm angezeigt und lässt sich auf eine geplante Aufgabe mit Administratorrechten umstellen. |
| **Als Administrator** | Schiebeschalter pro Programm sowie für die UpdateZentrale selbst. |
| **Hell und Dunkel** | Umschalter oben rechts; die Wahl wird gemerkt, die Fenstertitelleiste zieht mit. |
| **Protokoll** | Rechts oben steht die vollständige Ausgabe des jeweiligen Update-Werkzeugs. |
| **Terminal** | Rechts unten eine eingebaute PowerShell-Zeile mit den Rechten der UpdateZentrale, dazu eine Schaltfläche für ein echtes Terminalfenster. |

### Zwei Sammelknöpfe und ihre Reihenfolge

Oben stehen zwei voneinander unabhängige Knöpfe; welcher zuerst läuft, entscheidest du:

* **⚠ CLI-kritische Updates installieren** – nur die Einträge mit `cliRisiko`. Die Rückfrage davor ist zugleich die
  Aufforderung, Claude Code, Codex, OpenCode und alle Terminalfenster zu schließen; danach fragt der Lauf nicht noch einmal.
  Eingeordnet sind (Stand 1.3.36): PowerShell 7 und Git (die Hooks und Werkzeuge der Sitzungen laufen über `pwsh` und Git Bash),
  Node.js (MCP-Server und npm-CLIs), Windows-Terminal, WSL, Go, Bun, JDK 21 (Gradle-Daemons, Android-Builds), Android SDK
  (adb-Server), LM Studio und seine Runtimes (Sitzungen mit lokalem Modell), VS Code (integriertes Terminal), Windscribe
  (VPN-Abbruch). Nicht kritisch sind die CLI-Selbst-Updates (Claude Code, Codex, Kimi, Antigravity: laufende Sitzungen
  behalten ihre Version) und die kleinen Kommandozeilen-Werkzeuge.
* **Updates installieren · nicht CLI-kritisch** – alles andere, in dieser Reihenfolge:

1. **Repo-Abgleich** (`git fetch` und Vorspulen) schon bei der Prüfung – erst danach ist bekannt, welche eigenen Werkzeuge neu gebaut werden müssen.
2. **UpdateZentrale zuerst und allein.** Ihr Skript baut neu und startet die App neu. Der Rest des Sammellaufs wird vorher in
   `settings.json` (`sammelFortsetzen`) gesichert; die neue Fassung setzt ihn nach ihrer Startprüfung ohne zweite Rückfrage fort
   (nur innerhalb von 30 Minuten, nie beim versteckten Start bei der Anmeldung).
3. **Phase 1 – Eigene Werkzeuge** (OpenLauncher, TVO, CVO): Sie fragen Ja/Nein, und direkt nach dem Klick sitzt man noch davor.
4. **Phase 2 – CLI-kritisch** gehört zum anderen Knopf. Enthält ein Plan sie doch einmal (etwa nach einer Fortsetzung), hält
   der Lauf davor an und fragt: CLIs schließen und „Ja“, oder „Nein“ – dann bleiben sie offen und der Rest läuft weiter.
5. **Phase 3 – der Rest.** Seriell: winget/MSI/Store (ein Installer zur Zeit), npm (ein gemeinsamer Baum), die Hilfsskripte
   (sie schreiben den PATH), sdkmanager (braucht das JDK) und die LM-Studio-Runtimes (LM Studio wird daneben ersetzt).
   Parallel daneben (höchstens drei): nur Einträge mit `parallel` – Claude Code, Codex, Antigravity, rustup und der Start
   der OpenCode-Sitzung.
6. **Zum Schluss** alles mit `zuletzt` (App Installer).

Die Fußzeile zeigt den Gesamtstand vollständig und darunter je laufendem Update eine Zeile mit Position, Phase, Startzeit und
erwarteter Dauer.

Innerhalb jeder Phase geht das kürzeste erwartete Update zuerst, die großen Installer (Visual Studio, Android Studio, Docker)
also ans Ende. `zuletzt` (App Installer = winget selbst) steht immer ganz hinten; `hauptversionNurPerKlick` (TypeScript, npm,
FFmpeg) bleibt bei einem Sprung der Hauptversion ganz draußen und läuft nur per Klick auf der Karte.

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

Das rote Band bleibt stehen, bis es über das **✕** rechts oben weggeklickt wird – es verschwindet
also nicht von allein und geht beim Schließen der App nicht verloren. Weggeklickt wird nur die
Anzeige: im Tagesprotokoll und in `verlauf.jsonl` steht der Lauf unverändert, und die graue
Zeile „Zuletzt: …" nennt ihn weiter. Tritt später ein **neuer** Fehler auf, erscheint das Band
wieder, weil sein Zeitstempel jenseits der quittierten Marke liegt.

Abgelegt wird alles in `%LOCALAPPDATA%\UpdateZentrale\logs\`:

* `updates-JJJJ-MM-TT.log` – ein Tagesprotokoll. Pro Lauf ein Kopf mit Zeit, Programm, Art,
  genauem Befehl, ob erhöht gelaufen wurde und dem Stand vorher; danach die rohe Ausgabe des
  Werkzeugs; am Ende Ergebnis, Begründung, Stand nachher und Exit-Code. Auch Programmausnahmen
  landen hier.
* `verlauf.jsonl` – eine Zeile je Lauf, maschinenlesbar. Daraus lädt die App beim Start, was
  zuletzt passiert ist. Seit 1.3.31 mit `DauerSekunden` (ganzer Klick, Vorprüfung bis Urteil) sowie `SammelId`,
  `SammelPhase` und `SammelPosition`, wenn der Lauf Teil von „Alle Updates installieren" war.
* `sammellaeufe.jsonl` – je Sammellauf eine Zeile `plan` (Reihenfolge, Phase, parallel ja/nein, erwartete Dauer,
  gemessen oder geschätzt) und eine Abschlusszeile mit Beginn, Ende, Dauer und Ergebnis je Programm sowie der
  Gesamtdauer. Damit lassen sich Reihenfolge und Schätzungen später gegen die echten Zeiten prüfen.
* Im Tagesprotokoll stehen im Fuß jedes Laufs `Dauer:` und `Sammellauf:`. Die strukturierte Diagnose misst zusätzlich
  jede Prüfung und jedes Update als Vorgang mit Dauer.
* winget-Prüfungen nutzen eine gemeinsame Abfrage `winget list --source winget` (90 s gültig, nach jedem Installer
  verworfen); das Tagesprotokoll vermerkt sie unter `[winget]`.

Die Schaltfläche **Protokolle** in der Fußzeile öffnet diesen Ordner.

### Administratorrechte

Zum **Prüfen** braucht die UpdateZentrale keine Administratorrechte, ebenso wenig für die
Kommandozeilen-Werkzeuge und die eigenen Werkzeuge. Gebraucht werden sie nur zum **Installieren**
von Paket-Apps (Claude Desktop, Codex Desktop, PowerShell 7). Schlägt eine Prüfung fehl, liegt es
also nicht an den Rechten – siehe `bugs/desktop/dotnet-httpclient-ipv6-haenger.md`.

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

### Die Update-Arten

| `art` | Wofür | Pflichtfelder | Prüfung | Update |
|---|---|---|---|---|
| `winget` | Alles, was winget kennt | `wingetId` | `winget list --id …` (bzw. `--name …`) | `winget upgrade --id … --silent` (bzw. `--name …`) |
| `cli` | Werkzeuge, die sich selbst aktualisieren | `exePfad`, `updateArgumente` | `pruefArgumente` (Trockenlauf) **oder** `npmPaket` **oder** `versionsUrl` | `exePfad updateArgumente` |
| `store` | Store-signierte MSIX-Pakete | `appxName`, `packageFamilyName`, `storeProduktId` | `Get-AppxPackage` + `winget upgrade`-Liste | `winget upgrade --id <ProduktId> --source msstore --silent` |
| `reposkript` | Eigene Werkzeuge in `~/proggs` | `skript`, `statusPraefix` | `git rev-list HEAD..origin/main` + csproj-Version gegen gebaute Exe | ruft das vorhandene PowerShell-Skript auf |
| `opencodesitzung` | OpenCode mit Windows-Fix | `npmPaket` | npm-Version gegen `current.json` | startet eine OpenLauncher-Sitzung (`--jarvis-auftrag`: Opus 5.5, minimal, Schnellmodus, high), die die neue Version mit dem Fix verschmilzt und baut; die Karte steht auf „Sitzung baut", bis eine Prüfung die neue Version sieht |

Weitere optionale Felder:

| Feld | Bedeutung |
|---|---|
| `wingetQuelle` | Nur `winget`: die Quelle des Pakets, Standard `winget`. Mit Quelle dauert eine Abfrage 1 s, ohne fragt winget zusätzlich den Store ab (45–95 s). Leer = alle Quellen. |
| `exePfadAlternativen` | Weitere Orte für dieselbe Programmdatei; der erste Pfad, den es auf diesem Rechner wirklich gibt, gewinnt. So läuft derselbe Katalog auf mehreren Rechnern (LM Studio unter `Program Files` statt `%LOCALAPPDATA%`, CLIs als npm-Shim statt nativem Installer). |
| `ausblendenWennFehlt` | Blendet die Karte aus, wenn auf diesem Rechner keiner der Pfadkandidaten existiert – für Programme, die nur auf einem Teil der Geräte benutzt werden. |
| `versionsArgumente` | Argumente, die die installierte Version ausgeben (z. B. `--version`) |
| `pruefArgumente` | Trockenlauf-Befehl; Zeilen mit `→` bzw. `->` gelten als geplante Updates |
| `npmPaket` | Vergleichsquelle für die neueste Version, wenn das Werkzeug auf npm liegt |
| `versionsUrl` | Vergleichsquelle als Klartext-Adresse, deren Antwort die neueste Version nennt (z. B. Kimi Code: `https://code.kimi.ai/kimi-code/latest`) |
| `storeProduktId` | Store-Produkt-ID für das stille Update über die msstore-Quelle |
| `appxAnwendungsId` | Anwendungs-ID im MSIX-Paket (fast immer `App`) |
| `startArgumente` | Argumente beim Start über die Schaltfläche „Starten" und in der geplanten Aufgabe |
| `zeitlimitMinuten` | Zeitlimit des Update-Laufs (Standard 20) |
| `dialogWartezeitSekunden` | Nur `reposkript`: wie lange auf den Ja/Nein-Klick gewartet wird |
| `hinweis` | Kursive Zeile auf der Karte |
| `cliRisiko` | Grund, warum dieses Update laufende CLI-Sitzungen (Claude Code, Codex, Terminalfenster) beenden kann. Die Karte zeigt ein Warndreieck ⚠ neben dem Namen (Grund im Tooltip), der Einzelknopf fragt nach, und die Updates laufen nur über den eigenen Knopf „⚠ CLI-kritische Updates installieren“, nie mit den unkritischen. |
| `pfeilZaehlen` | Dokumentiert, dass die Prüfausgabe ihre Updates als `alt → neu` auflistet |
| `wingetName` | Nur `winget`: Paket per Anzeigename statt per Kennung auswählen (`--name` statt `--id … --exact`), bei Prüfung **und** Update. Nötig für Programme, die nicht über winget installiert wurden: winget ordnet sie nur über den Namen zu (PowerShell als MSI meldet bei `--id` „kein installiertes Paket“). Die Tabellenzeile wird weiter über `wingetId` gefunden, ein loser Name greift also nie ein fremdes Paket. |
| `wingetUpdatePerInstall` | Nur `winget`: Update mit `winget install --id … --exact` statt `upgrade`. `upgrade` findet nicht zugeordnete Pakete auch mit `--name` nicht (PowerShell-MSI); das neuere MSI ersetzt die alte Fassung beim Installieren selbst. |
| `wingetInstallerTyp` | Nur `winget`: Installer-Typ des Updates festlegen (`wix` = MSI). Ohne ihn nimmt winget den bevorzugten Typ des Manifests – bei PowerShell das MSIX-Paket, das sich **neben** die MSI-Installation legt statt sie zu ersetzen. |
| `pruefExitCodes` | Nur `cli`: Exit-Codes der Prüfung, die eine gültige Antwort sind (Standard `[0]`). `rustup check` endet mit 100, sobald Updates anstehen. |
| `pruefAbschnitt` | Nur `cli`: erst Zeilen nach der ersten Zeile mit diesem Text zählen (sdkmanager: `Available Updates`). Fehlt der Abschnitt, ist nichts offen. |
| `pruefZeilenMuster` | Nur `cli`: regulärer Ausdruck für eine geplante Update-Zeile statt des Pfeils – für Werkzeuge mit Tabellenausgabe. |
| `uebernahmeText` | Text des Bandes „wird übernommen" statt des Standards „nach dem Neustart des Programms" (OpenCode-Sitzung). |
| `hauptversionNurPerKlick` | Ein Sprung der ersten Versionsstelle bleibt aus „Alle Updates installieren" draußen und läuft nur per Klick. |
| `dauerSchaetzungSekunden` | Erwartete Update-Dauer, bis echte Messungen vorliegen (sonst ein Viertel von `zeitlimitMinuten`). |
| `zuletzt` | Immer das letzte Update eines Sammellaufs, nach allen Phasen (App Installer: ersetzt winget). |
| `parallel` | Darf im Sammellauf in der parallelen Bahn laufen. Nur für Selbst-Updater, die nichts mit einem anderen Eintrag teilen (Claude Code, Codex, Antigravity, rustup, OpenCode-Sitzung); bewusst kein Raten aus der Befehlszeile. |
| `nach` | Ids, die im selben Lauf vorher drankommen müssen (Android SDK und Gradle/Kotlin nach dem JDK, die LM-Studio-Runtimes nach LM Studio, die npm-Werkzeuge nach Node.js). Steht die Voraussetzung im anderen Knopf noch offen, nennt die Rückfrage das. |
| `versionsMuster` / `versionsUrlMuster` | Regulärer Ausdruck, dessen Gruppe 1 die Version in der Versionsausgabe bzw. in der Antwort von `versionsUrl` ist (Gradle „9.7“ hat nur zwei Stellen, `gradle --version` nennt auch Kotlin). |
| `updateExePfad` | Nur `cli`: anderes Programm für den Update-Lauf als für die Versionsabfrage (npm-Pakete: Version über `firebase.cmd --version`, Update über `npm.cmd install -g …`). Läuft im eigenen Ordner, damit npm seine Module findet. |

Nicht über winget installierte Programme, deren Datei unter `exePfad` existiert, erscheinen nie mehr als
„Nicht installiert“: Findet winget sie nicht, steht dort „Unbekannt“ mit dem Hinweis auf `wingetName`.
Bei mehreren installierten Fassungen desselben Pakets (.NET SDK 10.0.2xx neben 10.0.4xx) zählt die
neueste. winget-Versionen wie `< 3.14.7` oder `> 17.14.41` werden als eine Angabe gelesen. Endet ein
winget-Update mit „Neustart nötig“ (Dateien in Benutzung, MSI 3010), gilt es als ausstehend, nicht als Fehler.

### Entwicklungswerkzeuge

Die Gruppen „Entwicklung: …“ halten die Programmierumgebung aktuell: .NET SDK, Node.js, JDK 21, Rust
(rustup), Go, Python, Visual Studio, Build Tools, VS Code, Android Studio, Android SDK (sdkmanager),
Git, GitHub CLI, CMake, Bun, WSL, Windows-Terminal sowie globale npm-Pakete (Firebase CLI, TypeScript,
tsx, Biome, npm, OpenCode CLI). Hauptversionssprünge (z. B. TypeScript 6 → 7, npm 11 → 12)
zeigt die Karte an, ausgelöst werden sie nur bewusst per Klick. Das Windows-Terminal-Update schließt
alle Terminalfenster samt laufender CLI-Sitzungen.

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
* **UpdateZentrale selbst** – nach „Ja“ baut `update-zentrale.ps1` in einen Zwischenordner und startet
  `werkzeuge/selbstupdate-helfer.ps1`. Kam das Update aus der App, übergibt sie ihre Prozessnummer (`-AppPid`): sie schreibt
  Protokoll und Verlauf fertig und beendet sich danach selbst (etwa eine Sekunde nach dem Build); der Helfer wartet nur auf
  das Ende genau dieses Prozesses (höchstens 60 s), kopiert die neue Fassung (`robocopy /R:5 /W:1`) und startet sie. Kam es
  von außen, schließt er die App nach einer Sekunde. Schlägt das Kopieren fehl, startet er die bisherige Fassung wieder und
  sagt das. Jeder Schritt steht in `%LOCALAPPDATA%\UpdateZentrale\logs\selbstupdate.log`. Wer die App vorher von Hand
  schließt, schadet nichts: der Helfer kopiert und startet trotzdem.
* **Gradle, Kotlin, uv** – ohne winget-Paket und ohne Selbst-Update. `werkzeuge/zip-werkzeug.ps1 -Werkzeug …` lädt die
  neueste Fassung beim Hersteller, entpackt sie nach `%LOCALAPPDATA%\Programs\<Werkzeug>\<Version>` und lässt die Junction
  `current` darauf zeigen; der Benutzer-PATH zeigt danach nur noch auf `current` (beim ersten Lauf umgestellt). Die vorige
  Fassung bleibt als Rückweg liegen.
* **OpenCode CLI** – das Bauskript allein scheiterte, sobald der Windows-Fix nicht mehr auf die neue Version passte. Ein Update
  startet deshalb eine OpenLauncher-Sitzung mit dem Auftrag, die Version über `opencode-setup/build-install-windows-mousefix.ps1`
  zu bauen und den Fix bei Bedarf anzupassen. Eine zweite Sitzung für dieselbe Version startet frühestens nach drei Stunden.
* **TVO / CVO** – Updates ausschließlich nach dem Klick auf „Ja" in `rebuild-overlay.ps1`. `update.ps1` leitet dorthin weiter,
  `publish.ps1` und das Publish-Ziel im csproj verweigern ohne die Freigabe, die erst der Klick setzt. Statuszeilen werden nur
  am Zeilenanfang gelesen, die letzte zählt.
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
| `create_shortcut.ps1` | Verknüpfung auf dem Desktop und im Startmenü |
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
