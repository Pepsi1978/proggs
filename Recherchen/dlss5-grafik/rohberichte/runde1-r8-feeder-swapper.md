# Web-Researcher 8 — DLSS5-Feeder & DLSS5-Swapper (Original-Repos im Detail)

Stand der Recherche: 21.09.2026. Quellen: README (raw), Releases-Seiten, Wurzel-Dateiliste (GitHub API), Issues, ein Wiki-Check je Repo. Nichts heruntergeladen/installiert.

## 1. DLSS5-Feeder (jlrouzies-fr) — https://github.com/jlrouzies-fr/DLSS5-Feeder

### Technische Funktionsweise
- Feeder baut aus ReShade-Depth-Buffer + geschätzten Motion Vectors einen synthetischen DLSS-DLAA-"Contract", ruft damit ein echtes DLSS-Evaluate auf, lässt das DLSS-5-Neural-Rendering-Add-on (Deep Fried Chicken oder RenoDX) darin einhaken und kopiert das neurale Ergebnis zurück ins Bild — für Spiele, die selbst nie DLSS-Aufrufe machen. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder (Repo-Beschreibung, per WebSearch-Snippet bestätigt) und https://github.com/jlrouzies-fr/DLSS5-Feeder/blob/main/README.md
- Dateien im Spielordner (64-Bit): `dlss5-feed.addon64`, `DLSS5_Feed.fx` (unter `reshade-shaders\Shaders\`), der Neural-Consumer (`deep-fried-chicken.addon64` + `deep-fried-chicken-nvngx.dll` + `.cfg` ODER `renodx-dlss5.addon64`), sowie `nvngx_dlssnr.dll` + `nvngx_dlss.dll`. Für 32-Bit-Spiele zusätzlich ein `host64\`-Ordner mit `dlss5-feed-host64.exe`, einer 64-Bit-ReShade-`dxgi.dll` und denselben Neural-/nvngx-Dateien. Quelle: README (raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/README.md).

### Voraussetzungen
- NVIDIA-GPU mit NGX-Unterstützung (keine explizite RTX-50-only-Beschränkung im README dokumentiert). Quelle: README.
- Treiber: laut Kompatibilitätstabelle ab **616.56**, mit dokumentierten Einschränkungen bei Treiber **616.64+** in Kombination mit `renodx-dlss5` v4.6+. Quelle: README.
- APIs: D3D10, D3D11, D3D12, Vulkan, OpenGL sowie D3D9 über dgVoodoo2-Umweg — **nicht** nur DX12. Quelle: README.
- ReShade **6.8+ mit Add-on-Support** muss vorher installiert sein. Quelle: README.

### Kompatibilitätsliste
- Das README enthält NUR eine kleine, selbst getestete/Community-verifizierte Tabelle, keine große Spieleliste: Metro 2033 Redux (64-bit D3D11), Subnautica (64-bit D3D11), The Lord of the Rings: War in the North (64-bit D3D12), Splinter Cell: Blacklist (32-bit D3D11), BioShock Remastered (32-bit D3D11-Wrapper), Fable Anniversary (32-bit D3D9 via dgVoodoo2), DOOM 2016 (64-bit Vulkan), Worms Ultimate Mayhem (32-bit OpenGL), dazu Community-Meldungen zu Guild Wars/WoW 3.3.5a (DXVK), Star Wars: KOTOR (OpenGL), MX Bikes (OpenGL) und ein bekanntes Problem mit Detroit: Become Human Demo (Vulkan, "Smooth Motion"-Artefakte). Quelle: README.
- **Keine der vom Auftrag genannten Titel** (Cyberpunk 2077, Starfield, Assassin's Creed Shadows, Manor Lords, Star Trek Voyager Across the Unknown, Civilization VII, DREADZONE, Delta Force, Battlefield 6, Call of Duty) taucht in dieser Tabelle auf — auch nicht in den Issues, die ich einsehen konnte. Quelle: README + Issues-Übersicht https://github.com/jlrouzies-fr/DLSS5-Feeder/issues. Ein WebSearch-Treffer (thepcenthusiast.com) behauptet allgemein "Community-Demos" in Cyberpunk 2077/Starfield/AC-Reihe, das bezieht sich aber auf DLSS-5-Mods im Allgemeinen, nicht nachweisbar auf eine offizielle Feeder-Kompatibilitätsangabe — siehe OFFEN/UNSICHER.
- Kein eigenes Wiki vorhanden; Doku ist komplett im README + `docs`-Ordner. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder/wiki (leer/nicht vorhanden).

### Installation
- Automatisiert: `powershell.exe -ExecutionPolicy Bypass -File .\Install-DLSS5Feeder.ps1` (Skript liegt unter `tools/Install-DLSS5Feeder.ps1`). Quelle: README und https://github.com/jlrouzies-fr/DLSS5-Feeder/blob/main/tools/Install-DLSS5Feeder.ps1
- Manuell (64-Bit): 1) ReShade-Installer, Ziel-API "Direct3D 10/11/12" wählen, "Enable loading of add-ons" aktivieren → 2) `dlss5-feed.addon64` + `DLSS5_Feed.fx` aus den GitHub-Releases laden und in den Spielordner/Shader-Ordner kopieren → 3) LumeniteFX Kernel installieren (Motion-Vector-Provider) → 4) genau EINEN Neural-Consumer installieren (Deep Fried Chicken ODER RenoDX) → 5) `nvngx_dlssnr.dll` + `nvngx_dlss.dll` selbst beschaffen und ablegen → 6) im Spiel Home-Taste, `DLSS5_Feed.fx` aktivieren, `DLSS5_MV_PROVIDER = 3` setzen, Techniken in der richtigen Reihenfolge aktivieren. Für 32-Bit zusätzlich der `host64\`-Ordner mit Host-EXE. Quelle: README.

### Herkunft der DLSS-5-DLL / rechtliche Hinweise
- README-Zitat sinngemäß: **"You supply `nvngx_dlssnr.dll` and `nvngx_dlss.dll` yourself, as always."** Das Tool lädt die DLLs selbst NICHT automatisch herunter und bündelt sie nicht. `nvngx_dlssnr.dll` soll laut README aus dem RenoDX-Discord stammen ("Chicken does not bundle it"), `nvngx_dlss.dll` "from any DLSS game, or DLSS Swapper". Quelle: README.
- Eigene rechtliche Einordnung liegt in `AI-DECLARATION.md` im Repo-Root (6,2 KB) — Inhalt im Detail nicht vollständig extrahierbar, siehe OFFEN/UNSICHER. Quelle: Datei-Existenz via https://api.github.com/repos/jlrouzies-fr/DLSS5-Feeder/contents

### Deinstallation
- Keine dedizierte Deinstallations-Sektion im README gefunden; implizit: alle oben genannten kopierten Dateien (`dlss5-feed.addon64`/`.addon32`, `DLSS5_Feed.fx`, Neural-Consumer-Dateien, `nvngx_*`, `host64\`-Ordner, `dlss5-feed.cfg`) wieder aus dem Spielordner entfernen. Quelle: README (kein expliziter Deinstall-Abschnitt vorhanden — als OFFEN vermerkt).

### Anti-Cheat-Warnung
- Laut WebSearch-Auswertung von github.com/jlrouzies-fr/DLSS5-Feeder erkennt/warnt das Tool bei Spielen mit Easy Anti-Cheat, BattlEye, Vanguard, EA Javelin, HoYoverse, GameGuard, XIGNCODE3, Denuvo Anti-Cheat, PunkBuster, FACEIT, Ricochet oder ACE und verlangt eine Bestätigung; Formulierung laut Suchergebnis: "ReShade add-ons and anti-cheat do not coexist, and any ban risk falls on the person who chooses to install anyway" — bei Easy Anti-Cheat/BattlEye/GameGuard-Dateien im Installationsziel verweigert das Tool laut derselben Quelle die Installation ganz. Quelle: WebSearch-Zusammenfassung mit Referenz auf https://github.com/jlrouzies-fr/DLSS5-Feeder — **Wortlaut nicht per Direktzitat aus dem README verifiziert, daher als mittel-sicher einzustufen (siehe OFFEN/UNSICHER)**.

### Warnung vor Namensklonen (README-Datei `CAREFUL_FAKE_MALICIOUS_FEEDER.txt`)
- Repo enthält eine eigene Datei `CAREFUL_FAKE_MALICIOUS_FEEDER.txt` im Root. Laut Zusammenfassung: warnt vor gefälschten Kopien auf Drittseiten/GitHub-Mirrors, die sich als installierbare EXE ausgeben; empfiehlt SHA-256-Prüfung per PowerShell und Virenscan; offizielle Releases seien auf VirusTotal geprüft; bei False-Positives soll man das bei Microsoft melden. Quelle: https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/CAREFUL_FAKE_MALICIOUS_FEEDER.txt
- README selbst warnt zusätzlich (laut Extraktion): *"We got information that some malicious websites were making users download ZIP using similar name"* (Beispiel genannt: `DLSS5-Feeder-v0.7.0.zip`), und dass geklonte GitHub-Repos denselben Quellcode zeigen, aber das README durch einen "Download"-Button zu einer ZIP auf einer privaten `github.io`-Seite ersetzt haben. Offizielle Quelle laut README nur: https://github.com/jlrouzies-fr/DLSS5-Feeder/releases . Quelle: README.
- **Bestätigt gefunden per WebSearch:** mindestens diese Namens-/Ableger-Repos existieren real: `efdfdfsdfds/DLSS5-Feeder` (https://github.com/efdfdfsdfds/DLSS5-Feeder), sowie mehrere "DLSS5-Autopilot"-Forks (`bhardwajRahul/DLSS5-Autopilot`, `TTTT-T/DLSS5-Autopilot`, `ikunkk02-afk/DLSS5-Autopilot-Chinese-version`, `ther3ptil31987-prog/DLSS5-Autopilot`) sowie `faisalkindi/DLSS5oneclick` — das deckt sich mit der Vorrecherche-Aussage "mind. 4 Klon-Repos". Quelle: WebSearch-Trefferliste.

### Lizenz
- MIT License, Copyright (c) 2026 Jean-Laurent Rouzies, mit Hinweis "Portions derived from dlss5-dx11-bridge, Copyright (c) 2026 NIGos (MIT)". Quelle: https://raw.githubusercontent.com/jlrouzies-fr/DLSS5-Feeder/main/LICENSE

### Neuestes Release
- **v1.16.0-beta.6**, veröffentlicht 20.09.2026, Asset `DLSS5-Feeder-1.16.0-beta.6.zip` mit angegebenem SHA-256 (Hash aus der Zusammenfassung mit ungewöhnlicher Zeichenlänge übernommen — **nicht als exakt verifiziert werten, siehe OFFEN/UNSICHER**). Vorgänger-Releases laut Liste u. a. 1.16.0-beta.5 (19.09.), 1.16.0-beta.4/-beta.3 (16.09.), 1.16.0-beta.2 (14.09.), 1.16.0-beta.1 (10.09.), 0.15.1/0.15.0 (09.09.), 0.14.0-beta.5 (07.09.), 0.14.0-beta.4 (06.09.) — Projekt ist also innerhalb von rund zwei Wochen von 0.14.x auf 1.16.x gesprungen, weiterhin als Beta markiert. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder/releases

### Häufige Fehler/Abstürze aus den Issues
- #47: NGX-Init-Fehler `NVSDK_NGX_D3D12_Init -> 0xBAD00001` auf mehreren Maschinen mit Treiber 616.56 — Autor berichtet, dieselben Dateien funktionierten im `host64`-Prozess, aber nicht "in-process". Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder/issues (Issue #47)
- #15: Kompatibilitätsproblem beim 32-Bit-DXVK-Hook. Quelle: Issue #15.
- #44: Absturz bei Bayonetta, mit Crash-Dump. Quelle: Issue #44.
- #57: Batman: Arkham Knight — Feeder stoppt nach mehreren Minuten Spielzeit. Quelle: Issue #57.
- #63: FiveM — D3D12-Gerätefehler `0x887A0006 / DEVICE_HUNG`. Quelle: Issue #63.
- Geschlossen: #1 Konflikt mit NVIDIA Smooth Motion (Bildfehler), #33 NFS Most Wanted 2012 (`CreateTexture2D`-Fehler), #13/#8 Vulkan-Flackern/Freezes. Quelle: Issues-Übersicht.
- Insgesamt rund 30 offene Issues zum Zeitpunkt der Recherche. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder/issues

---

## 2. DLSS5-Swapper (rakanki911) — https://github.com/rakanki911/DLSS5-Swapper

### Technische Funktionsweise
- GUI-Installer/Manager, der DLSS-5-Neural-Rendering in Spielen und Emulatoren installiert, verwaltet und wiederherstellt — für native DLSS-Spiele direkt, für Nicht-DLSS-Spiele über Integration von **DLSS5-Feeder**, außerdem eigene Routen über **RenoDX** und **OptiScaler** sowie eine "Multipass"-Route (neurale Passage bis zu 10x pro Frame, DX12/DX11/64-bit-DX9, auch ohne natives DLSS). Quelle: README (raw.githubusercontent.com/rakanki911/DLSS5-Swapper/main/README.md).
- Enthält Bibliotheksverwaltung (Steam/Epic/GOG/Game-Pass/manuell), Backup/Restore-Funktion, Diagnose-Log-Export, In-Game-Overlay (F8) zum Verstellen der DLSS-Neural-Rendering-Regler — Overlay funktioniert nur über die Routen **DLSS5-Feeder und RenoDX v4.7**, nicht über OptiScaler. Quelle: README.

### Voraussetzungen
- Windows 10/11 x64. Quelle: README.
- ReShade/Feeder-Route: RTX 20/30/40/50 — laut README ist Support für ältere Serien "reported by the bundled modified runtime's author", also nicht offiziell von Swapper selbst getestet/garantiert. Quelle: README.
- OptiScaler-Route: das gebündelte Neural-Modell läuft nativ auf **Blackwell** (RTX 50 / RTX PRO Blackwell); für ältere Karten braucht man eine selbst besorgte, gemoddete `nvngx_dlssnr.dll`, die vom Tool nie überschrieben wird. Empfohlener Treiber: **616.56**. Quelle: README.
- Nicht DX12-only: DX12 nativ/Feeder/OptiScaler, DX11 über Feeder bzw. geeignete OptiScaler-Spiele, DX9 (32/64-bit) und DX8 (32-bit über dgVoodoo2→DX11→Feeder), Vulkan/OpenGL über ReShade/Feeder, Vulkan zusätzlich über OptiScaler bei geeigneten Spielen. DX10 wird von Feeder nicht direkt unterstützt (Empfehlung: DX11 wählen, falls verfügbar). Quelle: README.
- Feeder-Route benötigt zusätzlich Visual-C++-Runtimes (x64, plus x86 für 32-Bit-Spiele); manche Komponenten werden erst beim ersten Gebrauch nachgeladen. Quelle: README ("Before installing").

### Kompatibilitätsliste
- README enthält eine **API/Kategorie-Tabelle** ("Compatibility"), aber **keine Liste einzelner AAA-Spieltitel**. Die vom Auftrag genannten Titel (Cyberpunk 2077, Starfield, Assassin's Creed Shadows, Manor Lords, Star Trek Voyager Across the Unknown, Civilization VII, DREADZONE, Delta Force, Battlefield 6, Call of Duty) werden im README **nicht namentlich genannt**. Quelle: README.
- Stattdessen verweist das Tool auf eine **eingebaute "Community"-Funktion (BETA)**: Nutzer können pro Spiel und pro eigener GPU Erfahrungsberichte lesen/schreiben ("read what worked for other people, narrowed to the games on your PC and the graphics card in it"); das ist explizit KEINE offizielle/kuratierte Kompatibilitätsliste, sondern nutzergenerierte, ungeprüfte Berichte. Quelle: README.
- Release-Notes zu v2.2.7 nennen konkrete Titel im Kontext von Bugfixes, nicht als Kompatibilitätsfreigabe: Prey, Titanfall 2, Call of Duty 2 und Max Payne wurden vorher fälschlich als "No 3D executable" erkannt und sind seit 2.2.7 als installierbar erkennbar (Issues #259, #249); Portal wurde wegen gemeinsamer `hl2.exe` fälschlich unter Half-Life 2 einsortiert (Issue #274). **Das ist der einzige Bezug zu "Call of Duty" im Repo — es ist Call of Duty 2, nicht das im Auftrag gemeinte Battlefield 6/aktuelle CoD.** Quelle: https://github.com/rakanki911/DLSS5-Swapper (README-Abschnitt "New in 2.2.7") und https://github.com/rakanki911/DLSS5-Swapper/releases/tag/v2.2.7
- Kein eigenes Wiki vorhanden (nicht verlinkt/nicht existent). Quelle: https://github.com/rakanki911/DLSS5-Swapper/wiki
- Ein WebSearch-Treffer (thepcenthusiast.com, allgemeiner Artikel über DLSS-5-Mods) behauptet "Community demonstrations" in u. a. Cyberpunk 2077, Starfield, Assassin's-Creed-Reihe — das ist aber ein externer Artikel, keine Aussage aus dem Swapper-Repo selbst. Siehe OFFEN/UNSICHER.

### Installation
- Nur GUI-Installer, **keine dokumentierte Kommandozeilen-/Silent-Installation** im README gefunden. Download: "Windows Installer" (`DLSS5-Swapper-Setup-<Version>.exe`) oder "Portable" (`DLSS5-Swapper-<Version>-portable.exe`), beide über die Releases-Seite, mit `SHA256SUMS.txt` daneben. Quelle: README + https://github.com/rakanki911/DLSS5-Swapper/releases/latest
- Ablauf laut Features-Beschreibung: Spiel in der Bibliothek auswählen (automatisch gescannt aus Steam/Epic/GOG/Game-Pass-Ordnern oder manuell hinzugefügt), Installationsroute wählen (nativ/Feeder/RenoDX/OptiScaler/Multipass), Install-Button klicken; Rendering-API-Override ist pro Spiel optional, Default "Automatic". Quelle: README.
- Für Emulatoren: Emulator-Ordner + aktiven Renderer auswählen, dann "ReShade/Feeder"-Route nutzen; unterstützt u. a. DuckStation, PCSX2, RPCS3, Dolphin, PPSSPP, Xenia, Cemu, Ryujinx, yuzu/suyu/Eden/Citron/Sudachi, shadPS4, Azahar/Citra/Lime3DS, melonDS, Flycast, xemu, Vita3K, RetroArch, mGBA, Snes9x, Play! — Kompatibilität variiert je Renderer/Spiel, Xenia-HUD-Korrektur "experimental". Quelle: README.

### Herkunft der DLSS-5-DLL / rechtliche Hinweise
- README-Abschnitt "Before installing" sagt nur allgemein: "Some components download on first use" — **keine explizite Nennung einer Download-URL für die DLSS-5-DLL selbst und keine gesonderte rechtliche Einordnung** im extrahierten README-Text gefunden. Für OptiScaler wird ein "bundled neural model" erwähnt, das nativ auf Blackwell läuft; woher genau dieses Modell/die DLL stammt, wird im README nicht spezifiziert. Quelle: README — **als OFFEN/UNSICHER vermerkt**, da nicht abschließend im README verifizierbar; ggf. steht mehr Detail in `THIRD_PARTY_NOTICES.md`, die im README verlinkt, aber nicht separat abgerufen wurde.
- Es gibt zumindest ein offenes Sicherheits-Issue **#363** "Warning about malicious third-party DLSS 5 / ReShade downloads": Autor stellt klar "DLSS 5 itself is not the malware", warnt aber vor kompromittierten **inoffiziellen** Repacks/Installern/BAT-Skripten, die u. a. versteckte ScreenConnect-Fernwartung, Krypto-Miner (srbminer), getarnte Scheduled Tasks ("Windows System Health") und Cookie-Grabber enthielten. Issue war zum Recherchezeitpunkt offen. Quelle: https://github.com/rakanki911/DLSS5-Swapper/issues/363

### Deinstallation
- README-Fix-Eintrag zu v2.2.7 stellt klar: **"Uninstalling never touches game folders. The uninstaller now says so and points to Restore originals first"** (Bezug: Issue #266) — d. h. die reguläre Deinstallation des Swapper-Programms entfernt NICHT automatisch die in Spielordner kopierten ReShade-/Feeder-/OptiScaler-Dateien; dafür muss vorher explizit die "Restore originals"-Funktion je Spiel genutzt werden. Quelle: README-Abschnitt "Fixed" zu v2.2.7, https://github.com/rakanki911/DLSS5-Swapper/issues/266

### Anti-Cheat-Warnung
- README, Abschnitt "Before installing": **"Anti-cheat: red warning and optional confirmation, not a blanket block. Injection can cause crashes or account bans; the app never bypasses anti-cheat."** Quelle: README.

### Häufige Fehler/Abstürze (aus Release-Notes v2.2.7 und Issues)
- DirectDraw-Spiele (inkl. Gens/andere Emulatoren) installierten nie, weil dgVoodoo nur für DX8/DX9 heruntergeladen wurde → Fehler `errDgVoodooMissing` (Issues #292, #279, #150). Quelle: README/Release-Notes.
- Prey, Titanfall 2, Call of Duty 2, Max Payne wurden als "No 3D executable" erkannt (Renderer liegt als DLL neben der EXE) (Issues #259, #249). Quelle: README/Release-Notes.
- Falsche Zuordnung: Portal landete wegen gemeinsamer `hl2.exe` unter Half-Life-2-Berichten (Issue #274). Quelle: README/Release-Notes.
- Fälschlich als "read-only" markierte `ReShade.ini` kehrte nach 2.2.5 zurück (Issue #155). Quelle: README/Release-Notes.
- Spiele unter "Program Files" scheiterten mit `EPERM`, da Windows den Ordner schützt — Fix zeigt jetzt Hinweis "als Administrator ausführen oder Spiel verschieben" (Issue #301). Quelle: README/Release-Notes.
- Treiber-Warnhinweis wurde als zu pauschal/"wall of text" empfunden, Formulierung entschärft (Issues #300, #278). Quelle: README/Release-Notes.
- Overlay zeigte "DLSS was installed normally" bereits VOR Installationsende, auch wenn die Installation dann fehlschlug (Issue #275). Quelle: README/Release-Notes.
- Uninstall-Verhalten bzgl. Spieleordner (siehe Deinstallation oben, Issue #266). Quelle: README/Release-Notes.
- Windows Defender markierte das Tool früher als verdächtig (Issue #145, laut Web-Suchtreffer — Primärtext nicht einzeln verifiziert). Quelle: WebFetch-Zusammenfassung der Issues-Seite.
- Aktuell (Stand Recherche) einziges hervorstechendes offenes Issue: #363 (s. o., Warnung vor manipulierten Drittanbieter-Downloads). Quelle: https://github.com/rakanki911/DLSS5-Swapper/issues

### Lizenz, Stars, Sprachen
- MIT-lizenziert, Autor "Rakan Alkhaldi", Verweis auf `THIRD_PARTY_NOTICES.md` für Drittanbieter-Lizenzen. Quelle: README.
- 38 unterstützte Sprachen inkl. Deutsch; Arabisch/Persisch/Urdu mit RTL-Layout. Quelle: README.
- Sternezahl laut vorherigem WebFetch ca. 6,2k (deckt sich mit Vorrecherche-Angabe ca. 6,2k Sterne) — **nicht separat neu gegengeprüft in dieser Runde, aus erster WebFetch-Antwort übernommen**, siehe OFFEN/UNSICHER. Quelle: https://github.com/rakanki911/DLSS5-Swapper

### Neuestes Release
- **v2.2.7**, veröffentlicht 12.09.2026 (laut Extraktion 23:47 Uhr — Uhrzeit nicht doppelt verifiziert). Assets: `DLSS5-Swapper-Setup-2.2.7.exe`, `DLSS5-Swapper-2.2.7-portable.exe`, SHA256SUMS.txt. Vorgänger u. a. v2.2.6 (10.09.), v2.2.5 (09.09.), v2.2.4 (08.09.), v2.2.3 (07.09.), v2.2.2 (06.09.), v2.2.1 (04.09.), v2.2.0 (02.09.), v2.1.1/v2.1.0 (jeweils 01.–02.09.) — auch dieses Projekt entwickelt sich sehr schnell (fast täglich neue Version). Quelle: https://github.com/rakanki911/DLSS5-Swapper/releases

---

## BEST-PRACTICES-KANDIDATEN:
- Vor Installation beider Tools zuerst `CAREFUL_FAKE_MALICIOUS_FEEDER.txt` bzw. den Abschnitt "Community and privacy"/"Before installing" im jeweiligen README lesen und NUR von den offiziellen Releases-URLs laden (https://github.com/jlrouzies-fr/DLSS5-Feeder/releases, https://github.com/rakanki911/DLSS5-Swapper/releases/latest), SHA-256 gegen die dort angegebene Prüfsumme/`SHA256SUMS.txt` verifizieren, da mehrfach bestätigte Namensklone/Ableger-Repos existieren (z. B. https://github.com/efdfdfsdfds/DLSS5-Feeder, mehrere "DLSS5-Autopilot"-Forks, https://github.com/faisalkindi/DLSS5oneclick). Quelle: README jlrouzies-fr/DLSS5-Feeder, CAREFUL_FAKE_MALICIOUS_FEEDER.txt, Issue rakanki911/DLSS5-Swapper#363.
- Bei Swapper vor dem Deinstallieren des Programms zuerst je installiertem Spiel "Restore originals" ausführen — die App-Deinstallation entfernt keine in Spielordner kopierten Dateien. Quelle: README rakanki911/DLSS5-Swapper (Fix-Eintrag zu Issue #266).
- Für Spiele mit Anti-Cheat (Easy Anti-Cheat, BattlEye, Vanguard, GameGuard, Denuvo Anti-Cheat, Ricochet, ACE u. a.) grundsätzlich NICHT installieren bzw. das Bann-/Absturzrisiko bewusst selbst tragen — beide Tools weisen ausdrücklich darauf hin, dass sie Anti-Cheat nicht umgehen. Quelle: README rakanki911/DLSS5-Swapper ("Before installing"), WebSearch-Zusammenfassung zu jlrouzies-fr/DLSS5-Feeder.
- Treiberversion vor Installation prüfen: beide Tools nennen 616.56 als Referenzwert; für Feeder gibt es dokumentierte Einschränkungen mit Treiber 616.64+ in Kombination mit renodx-dlss5 v4.6+. Der Nutzer-Treiber 616.92 ist neuer als beide genannten Referenzpunkte und wird in keinem der beiden READMEs explizit erwähnt — vor Nutzung im Zweifel die jeweils aktuelle Release-Beschreibung/Issues nach 616.9x durchsuchen. Quelle: README jlrouzies-fr/DLSS5-Feeder, README rakanki911/DLSS5-Swapper.

## BUG-KANDIDATEN:
- jlrouzies-fr/DLSS5-Feeder, Version 1.16.0-beta.6 (Stand 20.09.2026): Issue #47 — NGX-Init schlägt in-process mit `NVSDK_NGX_D3D12_Init -> 0xBAD00001` fehl, obwohl dieselben Dateien im `host64`-Modus funktionieren; betrifft mehrere Systeme mit Treiber 616.56. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder/issues (Issue #47).
- jlrouzies-fr/DLSS5-Feeder: Issue #63 — FiveM stürzt mit D3D12-Gerätefehler `0x887A0006 / DEVICE_HUNG` ab. Quelle: Issue #63.
- jlrouzies-fr/DLSS5-Feeder: Issue #57 — Batman: Arkham Knight, Feeder stoppt nach mehreren Minuten Spielzeit. Quelle: Issue #57.
- jlrouzies-fr/DLSS5-Feeder: Issue #44 — Absturz bei Bayonetta (mit Crash-Dump). Quelle: Issue #44.
- rakanki911/DLSS5-Swapper, bis Version 2.2.7 (12.09.2026) bereits gefixt, aber relevant für ältere installierte Versionen: DirectDraw-Installation schlägt fehl (`errDgVoodooMissing`) vor 2.2.7 (Issues #292, #279, #150); Spiele unter "Program Files" scheitern mit `EPERM` vor 2.2.7 (Issue #301); Overlay meldete Erfolg vor Installationsende (Issue #275). Quelle: README rakanki911/DLSS5-Swapper, Release-Notes v2.2.7.
- rakanki911/DLSS5-Swapper: offenes Issue #363 (Stand Recherche) — Warnung vor mit Malware verseuchten inoffiziellen DLSS-5/ReShade-Repacks (ScreenConnect, Krypto-Miner srbminer, getarnte Scheduled Tasks, Cookie-Grabber); betrifft nicht den offiziellen Installer selbst, aber Nutzer, die auf Drittquellen ausweichen. Quelle: https://github.com/rakanki911/DLSS5-Swapper/issues/363

## OFFEN/UNSICHER:
- Exakter Wortlaut der Anti-Cheat-Passage im jlrouzies-fr/DLSS5-Feeder-README konnte nicht per Direktzitat verifiziert werden (README-Volltext-Reproduktion wurde vom Fetch-Tool zweimal verweigert); Aussage stammt aus einer WebSearch-Zusammenfassung mit Verweis auf das Repo, nicht aus einem selbst gelesenen Primärzitat.
- Der für Release 1.16.0-beta.6 genannte SHA-256-Hash hat in der extrahierten Form eine unplausible Zeichenlänge (könnte ein Übertragungsfehler des Zusammenfassungs-Tools sein) — vor echtem Gebrauch den Hash direkt auf der Releases-Seite nachschlagen, nicht aus diesem Dokument übernehmen.
- Inhalt von `AI-DECLARATION.md` (jlrouzies-fr/DLSS5-Feeder) und `THIRD_PARTY_NOTICES.md` (rakanki911/DLSS5-Swapper) wurden nicht im Detail gelesen — dort könnten weitere rechtliche Hinweise zur DLSS-5-DLL-Herkunft stehen.
- Herkunft/Downloadquelle der DLSS-5-DLL bzw. des "bundled neural model" bei der OptiScaler-Route von DLSS5-Swapper wird im README nicht konkret benannt (nur "some components download on first use") — nicht abschließend geklärt, von wo genau.
- Keiner der beiden Original-Repos dokumentiert offiziell Kompatibilität mit den vom Auftrag genannten AAA-Titeln (Cyberpunk 2077, Starfield, Assassin's Creed Shadows, Manor Lords, Star Trek Voyager Across the Unknown, Civilization VII, DREADZONE, Delta Force, Battlefield 6, aktuelles Call of Duty). Externe Artikel (z. B. thepcenthusiast.com) behaupten allgemeine Community-Demos dieser Titel mit "DLSS-5-Mods", das ließ sich aber nicht auf eine offizielle Aussage der beiden Repos zurückführen — falls das für die Kaufentscheidung/Nutzung relevant ist, empfiehlt sich eine gezielte Nachrecherche in den Community-Reports innerhalb der Swapper-App selbst oder in den Issues beider Repos nach diesen konkreten Spieltiteln.
- Ob Treiber 616.92 (Nutzer-System) mit beiden Tools tatsächlich problemlos läuft, ist in keinem der beiden READMEs explizit erwähnt (beide referenzieren nur 616.56 als Richtwert und 616.64+ als teils problematisch bei Feeder) — vor Nutzung ggf. gezielt in den aktuellen Issues nach "616.9" suchen.
- Stern-/Fork-Zahl von DLSS5-Swapper (~6,2k Sterne, 323 Forks) stammt aus einer einzelnen WebFetch-Zusammenfassung und wurde nicht durch eine zweite unabhängige Quelle gegengeprüft.
