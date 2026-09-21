# Unterthema: Anti-Cheat und Ban-Risiko von ReShade / „DLSS 5 Mod" (Web-Researcher 4/7)

Stand der Recherche: 21.09.2026. Alle Aussagen mit Quelle. Aggregator-/SEO-Seiten (dlssmod.com, heldgames.com, backgrind.com, switchbladegaming.com, cheating.live, gameslearningsociety.org) sind erkennbar KI-generierte Ratgeberseiten ohne Autorenangabe/Primärquelle — als solche gekennzeichnet und nur ergänzend verwendet, nie als Beleg für offizielle Policy.

## Zentraler technischer Befund: Der „DLSS 5 Mod" braucht die NICHT anti-cheat-sichere ReShade-Variante

ReShade existiert offiziell in zwei Bauformen:
- **Standard-Version**: hookt nur einen sehr eingeschränkten Satz an Grafik-API-Funktionen, erlaubt KEINE Add-ons zu laden. Erkennt ReShade ein Multiplayer-Spiel, wird ein Teil der Funktionalität (Tiefenpuffer-Zugriff) automatisch abgeschaltet, sobald Netzwerkverkehr erkannt wird. Seit Version 4.3.0 ist diese Standard-Version von Easy Anti-Cheat (EAC) whitelisted. Quelle: ReShade-Forum-Diskussion, referiert crosire (Entwickler) — https://reshade.me/forum/general-discussion/6119-about-a-list-of-white-of-easy-anti-cheat
- **Version „with full add-on support"**: schaltet die frei programmierbare Add-on-API frei. Diese Variante ist NICHT von Anti-Cheat-Anbietern whitelisted und laut Community-Zusammenfassung offizieller ReShade-Aussagen ausdrücklich nur für Singleplayer gedacht („may cause problems with anti-cheat systems"). Quelle (Forenzusammenfassung mit Bezug auf offizielle ReShade-Doku): https://reshade.me/forum/general-discussion/8487-reshade-vs-reshade-with-addon-support-difference

Das vom Nutzer verlinkte Tool **DLSS5-Feeder** (Basis der „DLSS 5 Mod"-YouTube-Anleitungen) verlangt laut eigenem GitHub-README ausdrücklich **„ReShade 6.8+ with add-on support"** und die Installer-Option „Enable loading of add-ons" — also exakt die NICHT-whitelistete Variante. Anti-Cheat-Hinweise fehlen im README komplett. Quelle: https://github.com/jlrouzies-fr/DLSS5-Feeder
Auch die Installationsseite https://dlss5feeder.com/paths thematisiert Anti-Cheat/Ban-Risiko nicht.

**Konsequenz für den Nutzer:** Unabhängig vom Einzelspiel ist die vom „DLSS 5 Mod" benötigte ReShade-Variante technisch die Kategorie, die Anti-Cheat-Hersteller als riskant einstufen bzw. aktiv blockieren. Das ist unabhängig von der Frage „bannt Spiel X ReShade normal" zu sehen.

Community-Warnungen speziell zum DLSS-5-Mod (aggregierte Ratgeberseiten, keine Primärquelle, aber inhaltlich konsistent mit obigem technischen Befund):
- „do not use this mod with online games. It can trigger anti-cheat software. That can get your account banned." — https://dlssmod.com/is-it-safe/
- Regel „Singleplayer only. Always." und explizite Nennung von VAC/EAC/BattlEye als Risiko wegen DLL-Injection — https://heldgames.com/guides/dlss-5-mod-risks
- Auch das OptiScaler-Team (verwandtes Projekt) warnt laut Sekundärquelle direkt davor, den Mod in Online-Spielen zu nutzen — zitiert in: https://tech.sportskeeda.com/gaming-news/modders-built-dlss-5-mod-override-tool-use-it
- Zusatzhinweis: Die kursierende `nvngx_dlssnr.dll` (Kern der DLSS-5-Feeder-Funktion) stammt aus einem versehentlich mit NBA 2K27 ausgelieferten NVIDIA-Build und ist kein offiziell freigegebenes, verteiltes NVIDIA-Produkt — zusätzliches (Lizenz-)Risiko unabhängig vom Anti-Cheat-Thema. Quelle: https://tech.sportskeeda.com/gaming-news/modders-built-dlss-5-mod-override-tool-use-it

## Battlefield 6 — EA Javelin (kernel-level)

- EA-Forum-Nutzer melden: **Javelin blockiert ReShade aktiv** seit dem Anti-Cheat-Update; Thread-Titel „Can't use reshade with the new anti-cheat update" — https://forums.ea.com/discussions/battlefield-franchise-discussion-en/cant-use-reshade-with-the-new-anti-cheat-update/6826706 (Seiteninhalt selbst nicht per Fetch abrufbar, HTTP 403 — Titel/Fund über Suche bestätigt)
- Community-Frage „Does BF6 allow Reshade?" im offiziellen EA-Forum ohne öffentlich bestätigte offizielle Freigabe — https://forums.ea.com/discussions/battlefield-6-general-discussion-en/does-bf6-allow-reshade/12756806
- Steam-Diskussion „Will reshade get you banned?": Nutzer bestätigen, dass ReShade **injiziert werden muss**, um wirksam zu sein (reines Vorhandensein auf der Platte ist unkritisch), aktive Nutzung in BF6 wird aber als TOS-Verstoß mit echtem Bann-Risiko eingeschätzt. Keine dokumentierten Einzelfall-Bans mit Datum/Screenshot gefunden, nur Einschätzungen/Erfahrungsberichte. — https://steamcommunity.com/app/2807960/discussions/0/690871777622153886/
- Javelin ist ein kernel-level Treiber, der Software/Treiber/Speicherbereiche systemweit einsieht, während BF6 läuft; EA gibt an, dass er nur bei laufendem Spiel aktiv ist und über 2,39 Mio. Cheat-Versuche verhindert hat (offizieller EA-Blogpost, keine ReShade-spezifische Aussage) — https://www.ea.com/en/games/battlefield/battlefield-6/news/battlefield-6-anticheat-update-season-1
- Es existiert ein EA-Forum-Thread zu False-Positives: ReShade wird teils fälschlich als „untrusted system file" erkannt (Titel bestätigt per Suche, Volltext per Fetch nicht abrufbar/403) — https://forums.ea.com/discussions/-/-/5247361 — d.h. auch bei rein kosmetischer Nutzung drohen Fehlalarme/Blockaden unabhängig vom „echten" Cheat-Risiko.
- Es gibt einen ReShade-eigenen Forumswunsch „A ReShade version that bypass EA Anticheat... A dream" — impliziert, dass die Community EA Javelin aktuell als inkompatibel/blockierend wahrnimmt — https://reshade.me/forum/suggestions/9679-a-reshade-version-that-bypass-ea-anticheat-a-dream

**Fazit BF6:** ReShade wird von Javelin blockiert bzw. als riskant/TOS-widrig eingestuft; dokumentierte Einzelban-Fälle mit Beleg wurden nicht gefunden, aber sowohl EA-Forennutzer als auch Steam-Community gehen übereinstimmend von echtem Risiko aus. Offizielle Herstelleraussage „ReShade ist verboten" im engeren Sinn (Blogpost/FAQ-Zitat) wurde NICHT gefunden — nur Foren-Indizien.

## Delta Force — Tencent/Team Jade, Anti-Cheat Expert (ACE)

- ACE ist ein kernel-level Anti-Cheat, das von Reviewern als „invasiv"/mit Spyware verglichen beschrieben wird und laut Kritik auch nach Spielende/-deinstallation weiterlaufen soll — https://80.lv/articles/delta-force-installs-kernel-level-spyware-anti-cheat-on-your-pc
- Offizielle Delta-Force-Seite zu „G.T.I. Security" (Kooperation mit ACE) nennt explizit verbotene Software: „Cheats, Injektoren, Emulator-Skripte, illegale Plugins, gecrackte Versionen, Exploits" sowie Emulatoren — **ReShade wird dort NICHT namentlich erwähnt**, weder erlaubt noch verboten. — https://www.playdeltaforce.com/en/anti-cheat.html
- Zwei unabhängige Steam-Community-Threads (Alpha- und aktuelle Phase) berichten übereinstimmend: ReShade **funktioniert ohne Probleme**, steht laut Nutzern nicht auf einer Verbotsliste — https://steamcommunity.com/app/2507950/discussions/0/604141771964884199/
- Allgemeine Kritik an ACE: Bans werden u. a. für VPN-Nutzung, Hardware-Features wie Anti-Ghosting/Zero-Lag, Controller-Nutzung, Overlays und „Mitspielen mit erkannten Cheatern" gemeldet — ReShade wird in diesen Fällen nicht konkret genannt. — Suchergebnis-Zusammenfassung, Primärquellen Steam-Diskussionen app/2507950
- Delta-Force-Bans in großer Zahl bestätigt (1729 Cheater in einer Woche), aber ohne ReShade-Bezug — https://www.dualshockers.com/delta-force-bans-cheaters-backlash-invasive-anti-cheat/

**Fazit Delta Force:** Aktuell keine belastbare offizielle Aussage von Tencent/Team Jade zu ReShade gefunden; Community-Berichte deuten auf faktische Duldung der (vermutlich Standard-)Version hin. Kein dokumentierter ReShade-Ban gefunden. Trotzdem: kernel-level ACE gilt als besonders „touchy" bei DLL-Hooks laut Sekundärquelle (backgrind.com, nicht offiziell) — Risiko bleibt unklar/nicht abschließend geklärt.

## Call of Duty — Black Ops 7 / Warzone / Modern Warfare III — RICOCHET

- Offizielle Activision-Richtlinie „Call of Duty Security and Enforcement Policy" verbietet allgemein „any code and/or software not authorized by Activision" inkl. „unauthorized texture/graphics modifications" sowie „tutorials and services that offer ways to modify your camo or other game content" und Speicher-Manipulation — **ReShade wird nicht namentlich genannt**, dürfte aber unter „unauthorized software" fallen können. Sanktionen reichen bis zu permanenten Account- und Hardware-Bans. — https://support.activision.com/articles/call-of-duty-security-and-enforcement-policy
- CoD-Forum-Thread „Can I get banned if I use Reshade?" blieb unbeantwortet/geschlossen ohne offizielle Antwort — https://www.codforums.com/threads/can-i-get-banned-if-i-use-reshade.19522/
- RICOCHET ist ein kernel-level PC-Treiber, der laut offiziellem Statement nur aktiv ist, während ein geschütztes CoD-Spiel läuft, und beim Schließen des Spiels wieder deaktiviert wird — https://www.callofduty.com/warzone/ricochet
- Ob der Treiber auch in der reinen Offline-Kampagne (Singleplayer) lädt, wird in offiziellen Quellen nicht explizit differenziert; da der Treiber pro Spielprozess (nicht pro Modus) aktiviert wird, ist ein Laden auch in der Kampagne wahrscheinlich, aber nicht explizit belegt — als OFFEN markiert.
- Aktuelle Bann-Zahlen (ohne ReShade-Bezug): über 136.000 Accounts in BO6/Warzone seit Ranked-Play-Start gesperrt — https://www.oneesports.gg/call-of-duty/ricochet-bans-cheaters-bo6-wz/
- Laufend aktualisierte offizielle RICOCHET-Blogposts (Season 02–04) beschreiben erweiterte Erkennung, aber keine ReShade-spezifische Aussage — https://www.callofduty.com/blog/2026/04/call-of-duty-black-ops-7-ricochet-anti-cheat-season-03 , https://www.callofduty.com/blog/2026/06/call-of-duty-black-ops-7-warzone-ricochet-anti-cheat-season-04

**Fazit CoD:** Keine explizite offizielle Aussage zu ReShade gefunden; die allgemeine Policy deckt „unauthorized modifications" ab, was ReShade einschließen kann. Community-Konsens (unbelegt/anekdotisch) tendiert dazu, dass ReShade in CoD-Titeln geflaggt werden kann, ohne dass Einzelfälle mit Datum/Beleg auffindbar waren.

## PUBG — BattlEye

- Am klarsten dokumentierter Fall: BattlEye **blockiert ReShade aktiv** in PUBG. Mehrere Steam-Diskussionen bestätigen: Mit installiertem ReShade lässt sich das Spiel nicht mehr starten, bis ReShade deinstalliert wird — https://steamcommunity.com/app/578080/discussions/1/2765630416809556052/ , https://steamcommunity.com/app/578080/discussions/1/1458455461485869020
- Ausdrückliche Klarstellung aus der Community: **Kein Bann allein durchs Vorhandensein** von ReShade, sondern ein reiner Startblock; ein Bann drohe nur bei Versuch, den Block zu umgehen — https://steamcommunity.com/app/578080/discussions/1/2765630416809556052/ (Community-Zusammenfassung)
- Historischer Kontext: ReShade war in PUBG früher sogar von Spielentwickler Brendan „PlayerUnknown" Greene ausdrücklich befürwortet, wurde aber später wegen der frei programmierbaren Shader-Möglichkeiten (z. B. Zoom-Tricks) gesperrt — https://wccftech.com/playerunknowns-battlegrounds-anti-cheat-update-delayed/
- Offizielle Presse-Meldung zur damaligen Verzögerung des Anti-Cheat-Updates wegen ReShade-Blockade — https://www.altchar.com/game-news/reshade-blocked-for-pubg-ap6ky2Y7F2X4

**Fazit PUBG:** Eindeutigster Fall der Recherche — ReShade ist technisch blockiert (Spielstart verhindert), kein automatischer Account-Bann durch bloßes Vorhandensein, aber Umgehungsversuche sind bannbar.

## DREADZONE

- Auf der offiziellen Steam-Store-Seite ist **kein Anti-Cheat-System deklariert** (Steam verlangt seit Ende 2024 die Offenlegung von Kernel-Level-Anti-Cheat auf der Store-Seite; ein solcher Abschnitt fehlt bei DREADZONE) — https://store.steampowered.com/app/3484300/DREADZONE/
- Spiel ist Early Access (Eshed als Entwickler), hat Online-Koop und Singleplayer; öffentlich keine Angaben zu ReShade-Politik oder einem benannten Anti-Cheat-Hersteller gefunden.
- Sekundärquellen (Cheat-/Trainer-Seiten) warnen pauschal, Trainer nicht online zu nutzen „da Anti-Cheat es erkennen könnte", ohne das konkrete System zu benennen — Qualität dieser Quellen niedrig, nur als Randnotiz.

**Fazit DREADZONE:** Anti-Cheat-System bleibt unbekannt/nicht offengelegt; keine belastbare Aussage zu ReShade möglich. Da kein Kernel-Anti-Cheat auf der Store-Seite deklariert ist, ist das Risiko vermutlich geringer als bei den übrigen Titeln, aber nicht verifiziert.

## Erlaubte Alternativen mit ähnlichem Effekt (Anti-Cheat-sicherer)

- **NVIDIA App „Freestyle"-Filter / RTX HDR**: Wird von einem Teil der Community als „sicherste" visuelle Erweiterung angesehen, weil es kein DLL-Hook-Mod, sondern ein von NVIDIA selbst bereitgestelltes Treiber-Feature ist; EAC soll dafür nicht bannen laut Community-Einschätzung — https://www.nvidia.com/en-us/geforce/forums/geforce-experience/14/434226/can-freestyle-get-me-banned-from-unsupported-games/
- Einschränkend: Manche Spiele/Anti-Cheat-Systeme (u. a. EAC-Titel) blockieren Freestyle dennoch aktiv (Metadaten-Flag „FREESTYLE: false"), und es gibt Berichte über Bans in einzelnen Titeln — pauschale „100% sicher"-Aussage ist NICHT belegt — https://www.nvidia.com/en-us/geforce/forums/freestyle-filters/17/489476/easy-anti-cheat-eac-preventing-freestyle-from-work/
- Destiny 2 wird als Gegenbeispiel genannt: dort sollen Accounts mit Drittanbieter-Mods wie ReShade im Hintergrund gebannt werden können (Community-Aussage, nicht offiziell verifiziert) — Quelle innerhalb obiger Suchzusammenfassung, Primärlink nicht separat auffindbar.
- **RTX Dynamic Vibrance / VibranceGUI**: Gilt für VAC-gesicherte Spiele als etabliert sicher (keine VAC-Bans seit Februar 2014 laut Tool-eigener Aussage), jedoch mindestens ein gemeldeter Bann-Fall bei Riots Vanguard (VALORANT) — https://vibrancegui.com/ , https://www.answeroverflow.com/m/1226192933284347994
- Fazit: Treiberseitige Filter (NVIDIA App Freestyle, RTX HDR, RTX Dynamic Vibrance) gelten in der Community als deutlich risikoärmer als ReShade, weil sie nicht per DLL-Injection in den Spielprozess eingreifen, sind aber nicht in jedem Einzelfall/jedem Spiel offiziell freigegeben — Einzelfallprüfung bleibt nötig.

## Offline-/Singleplayer-Betrachtung

- Bei allen betrachteten kernel-level-Anti-Cheat-Systemen (Javelin, ACE, RICOCHET) wird der Treiber pro Spielprozess bzw. während der gesamten Laufzeit des Spiels geladen — nicht nur im Online-Modus. Für RICOCHET ist offiziell bestätigt: „turns on when you start [Spiel] and shuts down when you close the game" — https://www.callofduty.com/warzone/ricochet — das umfasst nach gängiger Auslegung auch die Kampagne, wurde aber in keiner Quelle explizit für den Kampagnen-Modus separat bestätigt (siehe OFFEN unten).
- Für BF6/Javelin gilt laut EA-Aussage ebenfalls: Javelin läuft nur, während Battlefield aktiv ist — unabhängig vom Modus — https://www.ea.com/en/games/battlefield/battlefield-6/news/battlefield-6-anticheat-update-season-1

## Tabelle: Spiel | Anti-Cheat | ReShade erlaubt? | Ban-Belege | Empfehlung

| Spiel | Anti-Cheat | ReShade erlaubt? | Ban-Belege | Empfehlung |
|---|---|---|---|---|
| Battlefield 6 | EA Javelin (kernel-level) | Nein / aktiv blockiert bzw. als TOS-Verstoß eingeschätzt; teils False-Positive-Erkennung als „untrusted system file" | Keine dokumentierten Einzelban-Belege mit Datum gefunden, aber breiter Community-Konsens über reales Risiko (EA-Forum, Steam) | Nicht nutzen. DLSS-5-Mod (Add-on-Version) erst recht nicht |
| Delta Force | Tencent/Team Jade ACE (kernel-level) | Offiziell nicht geregelt (nicht auf Verbotsliste laut Nutzern); faktisch von mehreren Spielern ohne Probleme genutzt | Keine ReShade-spezifischen Bans gefunden; allgemein hohe Bann-Rate für andere Gründe (VPN, Overlays, Anti-Ghosting) | Nur mit Vorsicht, Standard-Version, nicht Add-on-Version; Risiko unklar/unbestätigt |
| Call of Duty BO7/Warzone/MW3 | RICOCHET (kernel-level) | Keine explizite offizielle Aussage; allgemeine Policy verbietet „unauthorized modifications", was ReShade einschließen kann | Keine dokumentierten Einzelfälle gefunden; unbeantworteter Forenthread zeigt Unsicherheit in der Community | Nicht nutzen, insbesondere nicht Add-on-Version |
| PUBG | BattlEye | Nein — technisch blockiert (Spielstart verhindert) | Kein automatischer Bann durchs bloße Vorhandensein, aber Bann-Risiko bei Umgehungsversuchen | Deinstallieren bzw. nicht mit PUBG kombinieren |
| DREADZONE | unbekannt/nicht deklariert (kein Kernel-AC auf Steam-Store-Seite) | Unklar, keine öffentliche Policy gefunden | Keine Belege gefunden | Vorsicht mangels Information; Standardversion vermutlich risikoärmer, aber nicht verifiziert |
| Alle Spiele, „DLSS 5 Mod“ (DLSS5-Feeder) | betrifft alle oben genannten | Nein — Tool verlangt zwingend die ReShade-Add-on-Version, die von keinem der o. g. Anti-Cheat-Systeme offiziell whitelisted ist | Keine spielspezifischen Einzelfälle, aber technisch dieselbe Kategorie wie generell blockierte/riskante ReShade-Nutzung; Ratgeberseiten warnen einhellig vor Multiplayer-Einsatz | Nur in reinen Singleplayer-Spielen ohne Kernel-Anti-Cheat verwenden; in keinem der sechs installierten Online-Titel einsetzen |

## BEST-PRACTICES-KANDIDATEN:
KEINE (Thema betrifft Ban-Risiko-Einschätzung für Endnutzer-Software, kein Claude-Code-/Projekt-Best-Practice im Sinne des lokalen best-practices/-Ordners)

## BUG-KANDIDATEN:
KEINE (kein reproduzierbarer Software-Bug im eigenen Projektcode; es handelt sich um Drittanbieter-Anti-Cheat-Verhalten)

## OFFEN/UNSICHER:
- Keine einzige offizielle, wortwörtlich zitierbare Herstelleraussage (EA/DICE, Activision, Tencent/Team Jade, Krafton/BattlEye) explizit zu „ReShade" als Begriff gefunden — alle Einschätzungen stützen sich auf Forenindizien, technisches Blockierverhalten oder allgemeine „unauthorized software"-Klauseln.
- Keine mit Datum/Screenshot dokumentierten Einzelban-Fälle speziell wegen ReShade oder des „DLSS 5 Mods" in den fünf genannten Online-Spielen gefunden (außer dem generellen PUBG-Startblock, der kein Account-Bann ist).
- Ob RICOCHET (CoD) und Javelin (BF6) auch in der reinen Offline-Kampagne aktiv laufen und dort ReShade genauso erkennen würden wie im Multiplayer, ist nicht explizit durch offizielle Quellen belegt (nur indirekt über „läuft solange das Spiel läuft").
- DREADZONE: Anti-Cheat-Hersteller/-System konnte nicht ermittelt werden; ebenso keine Policy zu ReShade.
- Mehrere zitierte Ratgeberseiten (dlssmod.com, heldgames.com, backgrind.com, switchbladegaming.com, cheating.live, gameslearningsociety.org) sind erkennbar KI-/SEO-generierte Drittseiten ohne erkennbare Primärquellen-Verifikation; ihre Aussagen wurden nur als ergänzende, nicht als beweiskräftige Belege gewertet.
- EA-Forenlinks (forums.ea.com) waren per WebFetch mehrfach nicht abrufbar (HTTP 403); Inhalte stammen daher nur aus Google-Suchvorschau/Snippets, nicht aus vollständig gelesenen Threads.
