# R8: Call of Duty (Steam) öffnet Microsoft-Store-/Gaming-Services-Fenster

Stand: 17.11.2026. Quellentreu; "belegt" = Aussage steht in der genannten Quelle, "Hypothese" = Schluss aus mehreren Quellen. Viele Seiten (Tom's Hardware, TweakTown, Digital Citizen, Neowin, theclick.gg, elevenforum, support.xbox.com) lieferten per Abruf 403 oder nur den Titel; Aussagen dazu stammen aus Suchsnippets und sind markiert.

## Kurzfazit

1. Es gibt KEINE offizielle Quelle (Activision oder Microsoft), die genau dieses Fenster ("Verbindung zum Microsoft Store") bei Steam-CoD beschreibt. Die Ursache ist eine begründete Hypothese, kein belegter Fakt.
2. Wahrscheinlichste Ursache (Hypothese, stützt sich auf belegte Teilaussagen): Activisions "Advanced Shader Delivery" (ASD, vorher "Shader Preloading"). Activision empfiehlt, die Xbox-PC-App "no matter what your PC platform" (also auch Steam) zu installieren, damit vorkompilierte Shader beim Install und bei Live-Updates ankommen. Das passt zu den Beobachtungen: Ordner C:\XboxGames\Shaders existiert, gamingservices lädt mit 12 parallelen HTTP-Verbindungen von einem CDN (Fastly). Der Shader-Ordner enthält laut Tom's-Hardware-Test (Suchsnippet) SODB- und PSDB-Dateien.
3. Das Fenster selbst (Store-Verbindung) ist wahrscheinlich ein Hilfsfenster des Xbox-/Store-Stacks, das die Shader-Lieferung oder ein Gaming-Services-Update anstößt. Ob es nur kosmetisch ist, ist nicht offiziell belegt. Siehe Abschnitt "Was passiert beim Schließen".
4. Nebenspur (Hypothese, andere Fehlerklasse): Windows zeigt "Look for an app in the Microsoft Store"-Dialoge, wenn ein Protokoll-Handler (z. B. ms-gamingoverlay) nicht registriert ist (Xbox Game Bar entfernt). Bei Steam-Spielen ist das Schließen dort laut Don't-Starve-Thread folgenlos. Das Fenster des Nutzers sagt aber "Verbindung zum Store herstellen", das passt eher zu Gaming Services/Store-Update als zu einem fehlenden Handler. Prüfen: Fenstertitel/Text genau notieren (Screenshot).

## 1. Warum öffnet Steam-CoD ein Store-/Gaming-Services-Fenster?

### 1a. Advanced Shader Delivery braucht Xbox-App + Gaming Services
- Microsoft DirectX Blog (21.10.2026): ASD liefert Shader "at download of a title" aus; für Call of Duty: Modern Warfare 4 "install the XBOX PC app before downloading"; Steam und Epic: "will soon expand beyond the Microsoft Store and XBOX PC app to the Epic Games Store and Steam". Bei MW4 dauert die Shader-Kompilierung beim ersten Start "just a few seconds". Im Startfenster erscheint "Precompiled shaders installed". Nennt keinen Pfad und keine Opt-out-Einstellung. https://devblogs.microsoft.com/directx/advanced-shader-delivery-is-available-now-across-hardware
- Microsoft DirectX Blog (15.05.2026, Nachtrag 11.06.2026): Voraussetzungen Windows 11 24H2+, Xbox Gaming Services 37.113.11003.0 oder neuer (installiert beim Nutzer: 39.118.30001.0, also neu genug); GA in der Xbox-PC-App seit 11.06.2026; Leserkommentar: derzeit nur MS-Store/Xbox-App-Version. https://devblogs.microsoft.com/directx/advanced-shader-delivery-expands-public-preview-with-amd/
- Activision/callofduty.com Blog (Suchsnippet, per Abruf nicht lesbar): "For the best experience, download and install the XBOX PC app ahead of time no matter what your PC platform", damit ASD Shader bei Install und Live-Updates liefert; ein anderer Absatz: Xbox-App nur nötig, wenn man ASD nutzen will. https://www.callofduty.com/blog/2026/09/call-of-duty-modern-warfare-4-pc-trailer-tech-specs-features
- TweakTown (Suchsnippet): "Xbox PC app must be installed to download its precompiled shader database", auch für Spiel von anderem Store. https://www.tweaktown.com/news/114214/nvidia-introduces-advanced-shader-delivery-support-into-its-latest-geforce-driver/index.html
- NVIDIA-Treiber 621.01 (Game Ready) ergänzt ASD für MW4 (godisageek, Suchsnippet; Seite 403). https://godisageek.com/2026/10/nvidia-621-driver-rtx-advanced-shader/
- Resetera-Thread zitiert Microsoft: Fremdstore-Spiele: "allowing the required shaders to be pre-installed", Xbox-App Voraussetzung. Nutzervermutung (unbelegt): Steam-Spiel prüft, ob Xbox-App installiert ist, und stößt den Shader-Download darüber an. https://www.resetera.com/threads/advanced-shader-delivery-coming-to-all-gpus-later-this-month-you-need-xbox-app-even-for-games-outside-ms-store.1649947/
- Gegenstimme (Suchsnippet, "Steam hat noch keinen ASD-Support"): Ein Tester sah bei der Steam-Fassung von Gears of War: E-Day keinen Effekt. Für Steam-CoD gilt laut Activision-Blog (Snippet) aber die Xbox-App-Empfehlung. Widerspruch bleibt offen. Quelle: Suchergebnis zu https://www.theclick.gg/modern-warfare-4-advanced-shader-delivery/ (403)

Fazit: Dass bei Steam-CoD die Xbox-App/Gaming Services im Hintergrund Shader laden, ist durch Beobachtung des Nutzers (Ordner, 12 Fastly-Verbindungen) plus Activision-Empfehlung plausibel, aber der genaue Auslöser des Fensters ist nicht dokumentiert.

### 1b. Andere geprüfte Möglichkeiten
- Gaming-Services-Pflicht/Update: Microsoft Q&A beschreibt bei beschädigtem Gaming Services Installationsfehler (0x87E0000F) und Store-Fenster; Fix per Neuinstallation. Das gilt für Store/Game-Pass-Fassungen. https://learn.microsoft.com/en-us/answers/a/12135107
- Xbox-Konto-Verknüpfung: Keine Quelle gefunden, die ein Store-Fenster beim Start von Steam-CoD damit begründet. XboxIdentityProvider ist eine Systemkomponente der Xbox-Anmeldung (Repair Tool repariert sie mit). Kein Beleg für Auslöser.
- Anti-Cheat: Activision verlangt für MW4 TPM 2.0 und Secure Boot ("TPM 2.0 and Secure Boot are required to play"); kein Bezug zu Store-Fenstern. https://support.activision.com/modern-warfare-4/articles/pc-system-requirements-for-modern-warfare-4 (gelesen). Der Steam-Thread zu CoD HQ nennt Ricochet-Auslöser durch Dienste (Spybot), nichts zum Store. https://steamcommunity.com/app/1938090/discussions/0/4757577823500255211/
- "Search Microsoft Store to launch app": Fehlerfenster der MW4-Beta, wenn die Beta über CoD HQ statt Xbox-App installiert wurde (Fan-Wiki, sekundär). Betrifft Store/Xbox-Installationen, nicht Steam. https://cod-mw4.wiki/settings/cod-mw4-not-launching
- Doppelinstallation: Ein Thread zu Black Ops 7 sieht Abstürze, wenn das Spiel gleichzeitig über Xbox-App und Steam installiert ist (Suchsnippet, Microsoft Q&A). Prüfen, ob eine Xbox-App-Installation von CoD/MW4 neben Steam existiert.

## 2. Was passiert, wenn man das Fenster schließt?
- Keine offizielle Aussage gefunden. Hinweise: Bei einem ähnlichen Steam-Popup (ms-gamingoverlay) berichtet ein Nutzer, dass das Spiel nach Schließen normal läuft (Klei-Forum). https://forums.kleientertainment.com/klei-bug-tracker/dont-starve-together/microsoft-app-needed-on-launch-r38854/
- Beim Nutzer läuft CoD offenbar weiter. Ohne ASD fällt laut Microsoft nur der Komfort weg: Shader werden beim Start lokal kompiliert (Dauer "several minutes" statt "a few seconds", Microsoft DirectX Blog). Kein Spielstart-Block belegt.
- Hypothese: Schließen verhindert nur die Interaktion mit dem Store-Fenster, der Hintergrund-Download von gamingservices läuft unabhängig weiter (der Nutzer sah 12 Verbindungen).

## 3. Dauerhafte Behebung

Reihenfolge von harmlos zu invasiv. Quellen: Microsoft Q&A (Microsoft-Mitarbeiter/Community), keine reine Support-Seite mit exakten PowerShell-Schritten gefunden.

1. Store-Cache leeren: `wsreset.exe` (Standardwerkzeug; in der Q&A-Antwort nur als Ergänzung erwähnt, die Antwort selbst nennt Store-Reset über App-Einstellungen). https://learn.microsoft.com/en-us/answers/a/12135107
2. Microsoft Store zurücksetzen: Rechtsklick Microsoft Store, App-Einstellungen, Zurücksetzen (gleiche Quelle). Danach Store und Xbox-App aktualisieren (Store, Bibliothek, Updates abrufen).
3. Gaming Services Repair Tool (offiziell, Xbox-Support): in der Xbox-App Profilbild, Support, "Gaming Services Repair Tool", "Start Troubleshooting", danach Neustart. Repariert Xbox-App, Xbox Identity Provider, Game Bar, Gaming Services. Offizielle Seite: https://support.xbox.com/en-US/help/games-apps/troubleshooting/gaming-services-repair-tool (Inhalt per Abruf nicht lesbar; Beschreibung aus Suchsnippet/winget-Listing). Bekannte Fehler: 0x8000ffff, bleibt bei "Repairing Gaming Services" hängen.
4. Gaming Services neu installieren (PowerShell als Administrator), aus Microsoft Q&A:
   - `get-appxpackage Microsoft.GamingServices | remove-AppxPackage -allusers`
   - `start ms-windows-store://pdp/?productid=9MWPM2CQNLHN` und im Store installieren, dann PC neu starten.
   Warnung: Ein Nutzer berichtet, dass danach Installationen über die Xbox-App kaputt waren; Neuinstallation nur wenn 1-3 nichts helfen. Ein weiterer Hinweis: Nach dem Neustart kann das automatische Update dasselbe Problem erneut auslösen. https://learn.microsoft.com/en-us/answers/a/12135107
5. Letzter Ausweg: Store neu registrieren: `$manifest = (Get-AppxPackage Microsoft.WindowsStore).InstallLocation + '\AppxManifest.xml' ; Add-AppxPackage -DisableDevelopmentMode -Register $manifest` (gleiche Quelle).
6. Zusatz laut Neowin: Microsoft-Workaround bei Game-Pass-Installationsfehlern über PowerShell und Löschen von Registry-Schlüsseln (nur fortgeschritten): https://www.neowin.net/news/microsoft-details-workaround-for-xbox-game-pass-installation-errors/
7. Nur wenn der Titel "ms-gamingoverlay" lautet: Game Bar neu installieren oder Game Bar deaktivieren. https://appuals.com/how-to-fix-the-youll-need-a-new-app-to-open-this-ms-gamingoverlay-error-on-windows-10

Zu klären (offen): Ob das Fenster nach Update/Reparatur dauerhaft verschwindet, ist nicht belegt; das Verhalten könnte vorgesehenes ASD-Verhalten sein und sich nur durch das Abschalten der Shader-Auslieferung unterdrücken lassen (Abschnitt 4).

## 4. Hintergrund-Downloads von gamingservices: begrenzen oder abschalten

Belegt:
- Es gibt keine offizielle Einstellung, ASD global zu deaktivieren (Microsoft-Blogs und Xbox DevDocs enthalten keinen Opt-out; ein Leser fragt im Blog danach ohne Antwort). https://devdocs.xbox.com/build/gdk-and-engines/guides/advanced-shader-delivery.md
- Tom's-Hardware-Tester (Suchsnippet): Kein offizieller Weg zum Abschalten; er löschte pro Spiel die Dateien im Ordner "Shaders" und trennte danach die Internetverbindung. https://www.tomshardware.com/pc-components/gpus/we-tested-advanced-shader-delivery-on-the-rx-9070-xt
- Activision-Berichte (sekundär, Suchsnippet): Das Feature ist in den CoD-Einstellungen optional und lässt sich dort abschalten ("Shader Preloading" neu starten/abschalten). Nicht bei Activision selbst bestätigt. Prüfen in MW4: Einstellungen, Grafik, "Shader Preloading".
- Windows Delivery Optimization (offiziell): Store-Downloads laufen laut Microsoft über Delivery Optimization; Download-Limit: Einstellungen, Windows Update, Erweiterte Optionen, Übermittlungsoptimierung, Erweiterte Optionen, "Bandbreite für Downloads begrenzen" (Mbit/s oder Prozent, auch "im Vordergrund/Hintergrund getrennt"). Zeitgesteuert nur per Gruppenrichtlinie (Business Hours). Ob gamingservices (Xbox-Delivery) dieses Limit einhält, ist NICHT belegt. https://support.microsoft.com/de-DE/Windows/Deployment/Updates-Lifecycle/delivery-optimization-in-windows und https://learn.microsoft.com/en-us/windows/deployment/do/waas-delivery-optimization-reference
- Gemessene Verbindung als "getaktet" markieren (Metered): Windows und Store laden dann keine App-Updates automatisch; kritische Updates kommen weiter. Wirkung auf gamingservices-Shader nicht belegt. https://www.windowscentral.com/how-make-network-connections-metered-windows-11
- Store: Automatische App-Updates lassen sich in Windows 11 nur noch für 1 bis 5 Wochen pausieren, nicht dauerhaft abschalten. https://www.windowscentral.com/microsoft/windows-11/microsoft-quietly-removes-ability-to-permanently-stop-apps-from-automatically-updating-on-windows-11-just-like-system-updates
- Xbox-App hat eigene Update-Einstellung; ein Nutzer berichtet, dass Updates trotz abgeschalteter Optionen kamen. https://learn.microsoft.com/en-us/answers/questions/4145438/ms-store-xbox-app-automatic-update-setting-turned

Praktische Optionen (Einschätzung, nicht offiziell bestätigt):
- Zeitlich steuern: Windows-Firewall-Regel oder QoS-Richtlinie (gpedit, "Richtlinienbasierte QoS", Prozess gamingservices.exe, Drosselrate) mit Zeitplan; NICHT per Quelle belegt, hier nur als Idee. Vor dem Online-Spielen Delivery-Optimization-Limit setzen ist belegt (siehe oben).
- Xbox-App deinstallieren (Forumsangabe, Resetera: Nutzer deinstallierte sie) beendet ASD-Downloads, riskiert aber Fehler bei Gaming Services. Nicht empfohlen ohne Test.
- Hinweis zur Größe: Keine Quelle nennt Shader-Datenmengen. Der Nutzer kann sie vor Ort messen (Größe C:\XboxGames\Shaders).

## 5. Offizielle Supportseiten (Activision und Microsoft)
- Activision PC-Anforderungen MW4 (TPM 2.0, Secure Boot; erwähnt Xbox-App NICHT): https://support.activision.com/modern-warfare-4/articles/pc-system-requirements-for-modern-warfare-4
- Activision/Call of Duty Blog MW4 PC (ASD, Xbox-App-Empfehlung): https://www.callofduty.com/blog/2026/09/call-of-duty-modern-warfare-4-pc-trailer-tech-specs-features
- Activision-Support MW3 bekannte Probleme/Gerätelimit Game Pass: https://support.activision.com/de/modern-warfare-iii/articles/known-issues-in-modern-warfare-iii
- Microsoft DirectX Blog ASD: https://devblogs.microsoft.com/directx/advanced-shader-delivery-is-available-now-across-hardware
- Microsoft DirectX Blog ASD Preview/GA: https://devblogs.microsoft.com/directx/advanced-shader-delivery-expands-public-preview-with-amd/
- Xbox DevDocs ASD: https://devdocs.xbox.com/build/gdk-and-engines/guides/advanced-shader-delivery.md
- Xbox Support Gaming Services Repair Tool: https://support.xbox.com/en-US/help/games-apps/troubleshooting/gaming-services-repair-tool
- Microsoft Support Delivery Optimization: https://support.microsoft.com/de-DE/Windows/Deployment/Updates-Lifecycle/delivery-optimization-in-windows
- Microsoft Q&A Gaming Services Neuinstallation: https://learn.microsoft.com/en-us/answers/a/12135107
- Microsoft KB5004327 (Gaming Services Fehler 0x80073D26/0x8007139F, Windows 10): https://support.microsoft.com/en-US/servicing/os/windows-10/2021/06/kb5004327-error-0x80073d26-or-0x8007139f-occurs-when-you-install-or-start-gaming-services-on-a-windo

## 6. Empfohlene nächste Schritte für den Nutzer (Reihenfolge)
1. Fenster beim nächsten Start fotografieren (Titel und Text) und Prozess prüfen (Task-Manager, Details): welcher Prozess besitzt das Fenster (WinStore.App.exe, XboxPcApp.exe, GamingServices)? Das entscheidet zwischen ASD/Store-Update und Protokoll-Handler-Fall.
2. In MW4 Einstellungen, Grafik: Shader Preloading prüfen/abschalten (Sekundärquelle).
3. Gaming Services Repair Tool, wsreset, Store/Xbox-App-Updates. Erst danach Neuinstallation.
4. Delivery-Optimization-Limit setzen, Netz als getaktet markieren, falls die Downloads beim Online-Spielen stören.

## BEST-PRACTICES-KANDIDATEN:
- Gaming Services Reparaturreihenfolge (wsreset, Store-Reset, Repair Tool, Neuinstallation per PowerShell, Admin nötig): https://learn.microsoft.com/en-us/answers/a/12135107 und https://support.xbox.com/en-US/help/games-apps/troubleshooting/gaming-services-repair-tool
- Windows-11-Bandbreitenlimit für Store-/Xbox-Downloads über Delivery Optimization: https://support.microsoft.com/de-DE/Windows/Deployment/Updates-Lifecycle/delivery-optimization-in-windows
- ASD braucht Xbox-App + Gaming Services >= 37.113.11003.0, auch bei Steam-CoD empfohlen: https://devblogs.microsoft.com/directx/advanced-shader-delivery-is-available-now-across-hardware

## BUG-KANDIDATEN:
- Gaming Services Reparatur schlägt fehl (0x8000ffff, hängt bei "Repairing Gaming Services") und Neuinstallation kann Xbox-App-Installationen brechen: https://learn.microsoft.com/en-us/answers/a/12135107
- MW4-Beta "Search Microsoft Store to launch app" bei Installation über CoD HQ statt Xbox-App (sekundär): https://cod-mw4.wiki/settings/cod-mw4-not-launching
- ms-gamingoverlay-Popup bei Steam-Spielstart (Game Bar entfernt): https://appuals.com/how-to-fix-the-youll-need-a-new-app-to-open-this-ms-gaming-overlay-error-on-windows-10
- ASD: kein offizieller Opt-out, Abschalten nur per Löschen der Dateien in Shaders und Offline-Trennung (Tom's-Hardware-Test, Snippet): https://www.tomshardware.com/pc-components/gpus/we-tested-advanced-shader-delivery-on-the-rx-9070-xt
