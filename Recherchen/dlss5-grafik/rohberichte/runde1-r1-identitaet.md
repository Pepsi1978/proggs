# Web-Researcher 1 — Identität des „DLSS 5 Mod" / „DLSS5feeder" / „DLSS5 Swapper" / imod.gg

Stand der Recherche: 21.09.2026. Quellentreu, WebSearch/WebFetch. YouTube-Videobeschreibung (uAHkSDk9LRo) selbst konnte NICHT direkt gelesen werden — WebFetch lieferte nur Footer/Navigation von YouTube, keinen Videotext. Das ist unter OFFEN/UNSICHER vermerkt.

## 1. Herkunft der Technik: kein eigenständiges „DLSS 5", sondern ReShade + RenoDX + eine echte NVIDIA-DLL

- Der reale Auslöser ist laut heise.de: Die Datei `nvngx_dlsss.dll` aus der **Early-Access-Version von „NBA 2K27"** (dem offiziellen DLSS-5-Launchtitel) wurde extrahiert/„geleakt". Modder nutzen diese echte NVIDIA-DLL und hooken sie in andere Spiele hinein. https://www.heise.de/en/background/DLSS-5-mod-tried-out-Why-all-the-fuss-11434544.html
- Trägerwerkzeug ist **RenoDX** („Renovation Engine for DirectX Games") von GitHub-Nutzer **clshortfuse** (https://github.com/clshortfuse/renodx) — ein ReShade-Addon-Framework, das Shader ersetzt, Buffer injiziert, Swapchains/Texturen austauscht und Nutzer-Settings speichert. RenoDX selbst nutzt Reshades Addon-System.
- Laut heise stammt das konkrete „DLSS 5"-Addon/Preset für RenoDX von einem Modder namens **„Lecram"**. https://www.heise.de/en/background/DLSS-5-mod-tried-out-Why-all-the-fuss-11434544.html
- Technik ist also: **ReShade-Addon (C++/DLL-Injection) + echte NVIDIA-Neural-Rendering-DLL**, NICHT ein eigenes trainiertes KI-Modell und NICHT ein Nachbau per Stable Diffusion. Es ist auch kein reines Farb-/Schärfe-ReShade-Preset im klassischen Sinn, sondern DLL-Hooking, das Nvidias echten Inferenz-Pfad in Spiele einschleust, die ihn offiziell nicht unterstützen.

## 2. „DLSS5-Feeder" / „DLSS5feeder" (Hashtag im Video)

- Offizielles Projekt: **GitHub github.com/jlrouzies-fr/DLSS5-Feeder**, Autor-Account **jlrouzies-fr**. https://github.com/jlrouzies-fr/DLSS5-Feeder
- Zweck laut README (wörtlich zitiert durch WebFetch): *„DLSS 5 neural rendering in D3D11/D12/Vulkan games that ship without any DLSS — feeds a synthetic DLAA contract (ReShade depth + motion vectors) to the DLSS 5 add-on via a private D3D12 device."*
- Technik: Das Tool erzeugt selbst die DLSS-Aufrufe, die ein Spiel ohne native DLSS-Unterstützung nie tätigen würde (DLL-Hooking, Cross-Process-Kommunikation, GPU-Interop, deckt DX10/11/12, Vulkan, OpenGL ab).
- **Wichtiger Sicherheitsbefund direkt aus dem offiziellen README**: Das Projekt warnt explizit vor Fälschungen: *„malicious websites were making users download ZIP using similar name as this project"* sowie *„copies of this repository on GitHub itself: same source tree, but the README replaced by a Download button."* Die Empfehlung lautet, ausschließlich `github.com/jlrouzies-fr/DLSS5-Feeder/releases` mit SHA-256-Verifikation zu nutzen. https://github.com/jlrouzies-fr/DLSS5-Feeder/blob/main/README.md
- Beleg für die Fälschungs-Warnung in der Praxis: Bei der Suche tauchten mehrere **1:1-Namensklone** mit identischer Beschreibung, aber anderem Account auf: `efdfdfsdfds/DLSS5-Feeder`, `gitzec/DLSS5-Feeder`, `Aryoksini/DLSS5-Feeder`, `GorgotsRoman/DLSS5-Feeder` — exakt derselbe Repo-Text, andere Besitzer. Das deckt sich mit der README-Warnung des Originals. https://github.com/efdfdfsdfds/DLSS5-Feeder , https://github.com/gitzec/DLSS5-Feeder , https://github.com/Aryoksini/DLSS5-Feeder , https://github.com/GorgotsRoman/DLSS5-Feeder
- Es gibt zusätzlich eine bewusst separate Variante **`DLSS5_AIO_Feed.fx`** im Repo `kibblerz/DLSS5-Reshade-AIO` (Apache-2.0-Lizenz), die laut eigenem README „intentionally separate" vom Original-Feeder-Shader gehalten wird, um Koexistenz zu ermöglichen. https://github.com/kibblerz/DLSS5-Reshade-AIO
- Kein Hashtag „DLSS5feeder" (ohne Bindestrich) ließ sich direkt einer offiziellen Marke zuordnen — es handelt sich vermutlich um die Kurzschreibweise des Feeder-Projektnamens für Social-Media-Tags, konnte aber nicht direkt im Video verifiziert werden (YouTube-Fetch schlug fehl, s. o.).

## 3. „DLSS5 Swapper" (das vom Nutzer genannte Tool)

- Offizielles GitHub-Projekt: **github.com/rakanki911/DLSS5-Swapper**, Autor lt. WebFetch **Rakan Alkhaldi (@rakanki911)**. https://github.com/rakanki911/DLSS5-Swapper
- Funktion: GUI-Installer/-Manager, der mehrere „Routen" bündelt — natives RenoDX, DLSS5-Feeder (für Spiele ohne DLSS), OptiScaler sowie Multipass — für DX8/9/11/12, Vulkan, OpenGL, DirectDraw und Emulatoren. In-Game-Overlay per F8-Taste.
- Lizenz: **MIT**, kostenlos, mit optionalem „Buy me a coffee"-Spendenlink.
- Umfang lt. WebFetch: 6.200 Stars, 323 Forks, 37 Watcher, 112 Commits, Version 2.2.7 zum Zeitpunkt der Recherche — deutet auf ein aktiv gepflegtes Community-Projekt hin, nicht auf einen Ein-Mann-Wegwerf-Mod.
- Eigene Sicherheitswarnungen im README: *„Anti-cheat: red warning and optional confirmation, not a blanket block. Injection can cause crashes or account bans"* sowie *„Compatibility is not guaranteed. Keep backups; existing mods may conflict."*
- Weitere Verbreitungswege: SourceForge (https://sourceforge.net/projects/dlss5-swapper/), Nexus Mods unter „Modding Tools" (https://www.nexusmods.com/site/mods/2228 — Direktzugriff per WebFetch von Nexus mit HTTP 403 blockiert, daher Autor/Downloadzahlen dort nicht verifizierbar), TechSpot-Downloadseite (https://www.techspot.com/downloads/7902-dlss-5-swapper.html) sowie eine augenscheinlich offizielle Projektseite **dlss5swapper.org**. Presseberichte bei TweakTown und TechPowerUp bestätigen Existenz und Funktion unabhängig. https://www.tweaktown.com/news/113398/new-dlss-5-swapper-tool-can-mod-dlss-5-into-games-that-dont-support-dlss-at-all/index.html , https://www.techpowerup.com/352395/new-dlss-5-swapper-tool-brings-neural-rendering-to-games-nvidia-never-supported
- **Konkurrierendes / alternatives Projekt mit ähnlichem Zweck**: `faisalkindi/DLSS5oneclick` — „One-click setup of the leaked DLSS 5 neural-rendering build ... Rust, single exe." Auch hier ein offen dokumentiertes **Sicherheitsproblem**: Issue #102 im Repo lautet wörtlich *„Security: RenoDX HDR mod URL is taken from an openly editable wiki with no host allowlist or integrity check."* https://github.com/faisalkindi/DLSS5oneclick/issues/102 — das ist ein konkreter Beleg dafür, dass mindestens ein Tool im Ökosystem Downloadquellen ohne Integritätsprüfung aus einem offen editierbaren Wiki zieht.
- Fazit: „DLSS5 Swapper" und „DLSS5-Feeder" sind **zwei verschiedene, aber zusammenhängende Community-Projekte** desselben Ökosystems rund um RenoDX/ReShade — Swapper ist der komfortable Installer/Manager, Feeder ist der Baustein, der DLSS-5-Aufrufe künstlich erzeugt.

## 4. imod.gg — Einordnung

- Selbstbeschreibung der Startseite (nur Titel per WebFetch lesbar, Rest des Inhalts wurde nicht ausgeliefert): **„iMod.gg | The Next-Gen Game Modding & Asset Platform"**, beworben in Suchergebnis-Snippets als Plattform zum „discover, download, and share free game mods with built-in security scanning, creator profiles, and mod galleries". https://imod.gg/
- **Keine unabhängigen Bewertungen, Scam-Checks oder Reddit-Threads auffindbar.** Weder ScamAdviser/Scam-Detector-Einträge noch Community-Diskussionen zu imod.gg ließen sich finden (Suchen liefen ins Leere bzw. lieferten nur branchenfremde Treffer wie das wissenschaftliche „IMOD"-Softwarepaket der University of Colorado). https://www.scamadviser.com/check-website/imod.ai (verwandte, aber andere Domain „imod.ai")
- `site:imod.gg`-Suche brachte **keinen einzigen indexierten imod.gg-Treffer** — weder zu DLSS5feeder noch generell. Das spricht für eine sehr neue bzw. von Suchmaschinen kaum erfasste Seite.
- Der Versuch, eine „/about"-Unterseite zu lesen, lieferte ebenfalls nur den Seitentitel, keine Angaben zu Betreiber, Impressum, Gründungsdatum oder Geschäftsmodell.
- **Einordnung als Hoster**: imod.gg tritt als generische Mod-Hosting-Plattform auf (vergleichbar mit Nexus Mods, mod.io, GGMods), nicht als offizielle NVIDIA- oder RenoDX-Quelle. Da das offizielle DLSS5-Feeder-Projekt ausdrücklich vor **namensähnlichen Drittseiten mit eigenen Download-Buttons** warnt (siehe Abschnitt 2), lässt sich die Seriosität von imod.gg als konkreter Distributionskanal für „DLSS5feeder" **nicht bestätigen und nicht widerlegen** — es fehlen sowohl Warnsignale als auch Vertrauensbelege.

## 5. Namensverwirrung / Fake-Gefahr — Zusammenfassung

Mehrere Projekte mit nahezu identischem Namen und Zweck existieren parallel, was Verwechslung begünstigt:
- `clshortfuse/renodx` — Kern-Framework
- `jlrouzies-fr/DLSS5-Feeder` — Original „Feeder" (mit Klon-Warnung im eigenen README)
- mind. 4 identische GitHub-Klone von „DLSS5-Feeder" unter fremden Accounts (s. o.)
- `kibblerz/DLSS5-Reshade-AIO` — bewusst separate All-in-one-Variante
- `rakanki911/DLSS5-Swapper` — GUI-Installer/Manager (MIT, 6,2k Stars)
- `faisalkindi/DLSS5oneclick` — konkurrierender One-Click-Installer (Rust) mit offen dokumentiertem Integritätsproblem
- `dlss5mod.com` — laut Selbstbeschreibung reine **Guide-/Linksammlung**, hostet ausdrücklich **keine** eigenen DLLs/Installer, sondern verweist auf Originalquellen. https://dlss5mod.com/tools/
- `dlss5swapper.org` — wirkt wie die offizielle Projektseite zu `rakanki911/DLSS5-Swapper`, aber eigenständige Domain-Inhaber-Identität wurde nicht verifiziert
- `imod.gg` — allgemeine Mod-Plattform ohne erkennbaren offiziellen Bezug zu einem der oben genannten Original-Autoren

## BEST-PRACTICES-KANDIDATEN:
KEINE (Thema ist ein experimenteller Grafik-Mod, keine Entwicklungs-Best-Practice für die eigenen Projekte des Nutzers erkennbar).

## BUG-KANDIDATEN:
- **Fake-/Klon-Repos als Malware-Vektor**: Offizielles Projekt `jlrouzies-fr/DLSS5-Feeder` warnt selbst vor namensgleichen GitHub-Klonen mit manipuliertem Download-Button sowie vor externen Seiten mit ähnlichem ZIP-Namen. Belegt durch mind. 4 auffindbare 1:1-Klon-Repos (`efdfdfsdfds`, `gitzec`, `Aryoksini`, `GorgotsRoman` jeweils `/DLSS5-Feeder`). Quelle/Version: README Stand 21.09.2026, https://github.com/jlrouzies-fr/DLSS5-Feeder/blob/main/README.md
- **Fehlende Integritätsprüfung bei Download-URL**: `faisalkindi/DLSS5oneclick` Issue #102, wörtlich „RenoDX HDR mod URL is taken from an openly editable wiki with no host allowlist or integrity check" — offen zum Zeitpunkt der Recherche. https://github.com/faisalkindi/DLSS5oneclick/issues/102
- **Anti-Cheat-/Account-Risiko durch DLL-Injection**: `rakanki911/DLSS5-Swapper` README, Version 2.2.7, warnt selbst: „Injection can cause crashes or account bans" — relevant, falls der Nutzer den Mod in Online-Titeln statt nur offline (z. B. MW2 DMZ Offline) einsetzen würde. https://github.com/rakanki911/DLSS5-Swapper

## OFFEN/UNSICHER:
- Die tatsächliche Videobeschreibung von „TheGr08" (https://www.youtube.com/watch?v=uAHkSDk9LRo) mit dem genannten Download-Link zu imod.gg und den Tags #ReShade #DLSS5feeder konnte NICHT direkt gelesen werden (WebFetch lieferte nur YouTube-Navigations-/Footer-Text, keinen Videoinhalt). Die Zuordnung „DLSS5feeder" ↔ `jlrouzies-fr/DLSS5-Feeder` ist daher eine plausible Namens-Ableitung, keine direkt verifizierte 1:1-Bestätigung.
- Kein Kanal-Profil, Impressum oder Autoren-Identität von „TheGr08" auffindbar (weder über YouTube noch über Web-Suche) — Seriosität/Reichweite des Kanals bleibt ungeklärt.
- imod.gg: kein Betreiber-Impressum, kein Gründungsdatum, keine unabhängigen Bewertungen oder Sicherheits-Testberichte gefunden — weder positiv noch negativ belegt, echtes Erkenntnis-Vakuum.
- Ob der von TheGr08 verlinkte imod.gg-Download tatsächlich das offizielle `jlrouzies-fr`-Release ist oder eine der bekannten Namens-/Klon-Fallen, ließ sich mit den verfügbaren Mitteln nicht klären.
- Gamespot- und Tom's-Hardware-Artikel zum „Leaked DLSS 5 Mod" konnten wegen Content-Kürzung/403 nicht vollständig ausgewertet werden; die Kernaussage zur Herkunft der DLL (NBA-2K27-Early-Access-Leak) stützt sich auf die heise.de-Quelle.
