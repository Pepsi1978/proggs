# Bekannte Bugs & Fallen: App-Icon-Building — Windows · Android · macOS

> **PFLICHT-LESEN vor JEDER echten Arbeit am Erstellen/Einspielen von App-Icons**
> (`.ico`, `.icns`, Adaptive Icons, App-Icon-Assets, Verknuepfungs-Icons). Trivialer
> Kleinkram (ein bestehendes Icon 1:1 kopieren) ausgenommen. Loesungen sind
> **funktionserhaltend** — nie "Icon weglassen".
>
> **Stand:** recherchiert am **2026-06-07**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax)
> fuer **Windows 11 Build 26200 (Explorer 26100), .NET 10.0.204 / WPF, Pillow
> 12.1.1, ImageMagick 7, Android 16 / API 36 / AGP 9.x, macOS 15 Sequoia + macOS 26 Tahoe**.
> Viele Punkte sind "per Design" und gelten versionsunabhaengig.
> **Re-Recherche 2026-07-02:** Anker weitgehend bestaetigt (Windows-Icon-Cache = wie Win10, Play-512px-Regeln,
> iconutil/.iconset unveraendert). Neu ergaenzt: **Android 16 QPR 2 Auto-Theming** (§6.4) und der ausfuehrliche
> macOS-26-`.icon`-Workflow inkl. `actool`→`Assets.car` + Xcode-26.1-Falle (§7.3).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Schwarze/eckige Ecken | Quelle als PNG mit echtem Alpha (RGBA), Ecken Alpha=0 | §2.1 |
| 2 | OS maskiert (Android/iOS/macOS-Tahoe) | Volles Quadrat liefern, nie selbst runden | §2.2 |
| 3 | Windows-`.ico`/klassisches `.icns` | Form/Padding als Alpha einbacken (einmal) | §2.2 |
| 4 | Icon wechselt nicht (Windows) | Cache-DBs löschen + Explorer neu, nicht nur `ie4uinit` | §4 |
| 5 | Verknüpfung zeigt altes Icon | Neuen IconLocation-Schlüssel: `<App>.exe,0` | §4.3 |
| 6 | Konverter macht Ecken schwarz/weiss | Kein `-flatten`/`-background`/`-border`; Pillow `convert("RGBA")` + `sizes` | §3, §8 |
| 7 | WPF Icon fehlt/single-file bricht | `ApplicationIcon` + `Window.Icon`, Datei als `Resource` | §5.1, §5.2 |
| 8 | Android Motiv abgeschnitten | Kernmotiv in 66dp-Safe-Zone, Background opak ohne Rundung | §6.1, §6.2 |

---

## 1. ⭐ Referenz-Vorfall: VoiceAgent — schwarze Ecken + Icon-Cache (2026-06-07)

**Symptom:** Desktop-Verknuepfung zeigte das Icon als Quadrat mit schwarzen Ecken statt
abgerundet/transparent. Nach Korrektur des `.ico` blieb das alte eckige Icon hartnaeckig.
**Ursache (zwei Schichten):**
1. Die PNG-Quelle (KI-generiert) hatte in den Ecken **reines Schwarz mit Alpha=255** statt
   Transparenz. Bei der Konvertierung zu `.ico` blieb das schwarze Viereck.
2. Nach dem Fix blieb das alte Icon, weil `ie4uinit -show`/`-ClearIconCache` + .lnk-Neuerstellung
   den Cache **nicht** leeren und die Verknuepfung ueber den **gleichen IconLocation-Pfad**
   (`…\voiceagent.ico`) cacht — gleicher Cache-Schluessel = altes Icon.
**FIX (verlaesslich, funktionserhaltend):**
- Schwarze Ecken transparent machen (Pillow): nahe-schwarze Pixel `(r<28,g<28,b<28)` → Alpha 0,
  ODER abgerundete/Squircle-Alpha-Maske `putalpha(mask)`. Sanity-Check: nur ~5 % (die Ecken)
  duerfen sich aendern; Alpha-min muss 0 sein.
- Cache verlaesslich neu: IconLocation auf **`<App>.exe,0`** (eingebettetes Icon = frischer
  Schluessel) + Explorer beenden + `iconcache_*.db`/`thumbcache_*.db`/`IconCache.db` loeschen +
  Explorer neu (§4).
**Versionen:** Windows 11 26200, .NET 10/WPF, Pillow 12.1.1.
**Quelle:** eigener Vorfall + Researcher-Belege unten.

---

## 2. Schwarze / eckige Ecken — das WARUM (plattformuebergreifend)

### 2.1 Opake Quelle (Alpha=255) oder abgeflachtes Bild  ⭐ HAEUFIG
**Symptom:** Schwarzer Hintergrund + harte eckige Ecken statt transparent-abgerundet.
**Ursache:** PNG ohne Alpha / als JPEG gespeichert / iOS-Quelle (iOS verbietet Alpha und
**rendert Transparenz als Schwarz**). KI-Generatoren (DALL-E/Midjourney/Nano Banana) liefern
nur RGB, **kein Alpha** — auch bei Prompt "transparent background" kommt eine Vollfarbe.
**FIX:** Master als **32-bit PNG mit echtem (straight/unpremultiplied) Alpha**. KI-Output
nachbearbeiten: Hintergrund entfernen (Select Subject / Transparify / Differenz zweier Renders
auf Weiss+Schwarz). JPEG fuer Icons NIE. Pruefen: Schachbrett-Hintergrund / Alpha-Kanal-Palette.
**Versionen:** per Design (iOS verbietet Alpha; Android/Windows/macOS erlauben/erwarten es).
**Quelle:** https://transparify.app/blog/ai-image-transparent-background · https://developer.apple.com/forums/thread/96003

### 2.2 Eigene Rundung trifft OS-Maske → Doppelrundung
**Symptom:** Icon wirkt "doppelt gerundet" / abgehackte Ecken / schwarze Zwickel.
**Ursache:** Das OS legt seine eigene Maske ueber ein bereits gerundetes Bild.
**FIX:** Wo das OS maskiert (Android, iOS, macOS-Tahoe) **volles Quadrat ohne eigene Rundung**
liefern. Wo es NICHT maskiert (Windows-`.ico`, klassisches macOS-`.icns`) die Form als
**Alpha-Maske einbacken** — aber nur EINMAL (keine vorgerundete Quelle zusaetzlich maskieren,
sonst transparente Luecken).
**Eckenradius:** iOS/Squircle ≈ 22,37 % Superellipse (kein Kreisbogen). Einfaches `rounded_rectangle`
(~18-22 %) reicht fuer Windows meist; echtes Squircle nur wenn iOS-Optik gewuenscht.
**Quelle:** https://liamrosenfeld.com/posts/apple_icon_quest/ · https://www.one4studio.com/glossary/squircle-icons

### 2.3 Anti-Aliasing-Halo (dunkler Saum an der Kante)
**Symptom:** Duenner dunkler Rand entlang abgerundeter Kanten.
**Ursache:** Harte (aliased) Maskenkante ueber dunklen opaken Pixeln; ODER premultiplied Alpha
mit schwarzem Matte; ODER vor dem Maskieren geblurrt.
**FIX:** Maske per **Supersampling** (5× zeichnen → LANCZOS-Downscale) als weiche Alpha-Kante;
PNG **straight/unpremultiplied** speichern; **erst maskieren, dann** evtl. blurren.
**Quelle:** Pillow #5577, #307 · https://limnu.com/premultiplied-alpha-primer-artists/

---

## 3. Windows: `.ico` korrekt erstellen (Pillow / ImageMagick)

### 3.1 Pillow: Alpha verloren / nur 16×16 / 256-Roundtrip
**Symptom:** Transparenz weg, oder ICO enthaelt nur eine winzige verpixelte Groesse, oder 256er wird zu 255.
**Ursache:** Nicht-RGBA-Modus; fehlender `sizes`-Parameter; Re-Save aus geladenem ICO (off-by-one).
**FIX:**
```python
img = Image.open(src).convert("RGBA")           # RGBA-Pflicht
# ... Alpha-Maske setzen (Ecken transparent) ...
img.save("out.ico", format="ICO",
         sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
```
Quelle ≥256px (groesser wird ignoriert — 256 ist Hardlimit). 256er ist Pillow-Default PNG-komprimiert (richtig so).
**Versionen:** Pillow alle inkl. 12.1.1. **Quelle:** Pillow #2405, #2264; pillow.readthedocs.io/handbook/image-file-formats

### 3.2 ImageMagick: `-flatten`/`-border`/`-background` machen Ecken schwarz/weiss  ⭐
**Symptom:** Transparente Ecken werden weiss oder schwarz; weiche Kanten werden 1-bit-zackig.
**Ursache:** `-flatten` komponiert gegen `-background`; `-colors` zwingt Palettenfarbe in transparente
Bereiche; ICO-Encoder reduziert Alpha auf 1-bit.
**FIX:** Keine `-flatten`/`-border`/aggressiven `-colors`. Stattdessen:
`magick in.png -define icon:auto-resize=256,128,64,48,32,24,16 out.ico` (auto-resize erhaelt Alpha).
Quelle als `PNG32:in.png` halten; Alpha schuetzen mit `-alpha off [op] -alpha on`.
**Quelle:** ImageMagick #6361, #6985 · jqmagick Forum t=26252/t=28214

### 3.3 Pflicht-Groessen fuer ein gutes Windows-`.ico`
16, 24, 32, 48, 64, 128, 256 (Minimum 16/24/32/48/256). 256er PNG-komprimiert. Quelle ≥512px
(besser 1024), damit Windows nur herunter-, nie hochskaliert.
**Quelle:** learn.microsoft.com/windows/apps/design/iconography/app-icon-construction

---

## 4. ⭐ Windows-Icon-Cache: warum altes Icon bleibt + verlaesslicher Reset

### 4.1 `ie4uinit -show` / `-ClearIconCache` reichen NICHT
**Symptom:** Befehl laeuft, altes Icon bleibt.
**Ursache:** Beide zwingen nur ein **Neuzeichnen**; sie loeschen die `iconcache_*.db`-Dateien nicht.
In neueren Win11-Builds laeuft `ie4uinit` teils gar nicht zuverlaessig durch.
**FIX:** Die DBs selbst loeschen (4.2).
**Quelle:** woshub.com/how-to-rebuild-corrupted-icon-cache-in-windows

### 4.2 Der verlaessliche Reset (Win10/11 inkl. 26200)
**Pfade (Win10/11):** `%LOCALAPPDATA%\Microsoft\Windows\Explorer\iconcache_*.db` (+ `iconcache_idx.db`),
`…\Explorer\thumbcache_*.db`, plus alt `%LOCALAPPDATA%\IconCache.db` (versteckt). **Nur `IconCache.db`
loeschen reicht seit Win10 NICHT** — die ganze `iconcache_*.db`-Serie muss weg. Explorer haelt die
DBs gesperrt → erst beenden.
```powershell
Stop-Process -Name explorer -Force
Start-Sleep 2
Remove-Item "$env:LOCALAPPDATA\Microsoft\Windows\Explorer\iconcache_*.db" -Force -ErrorAction SilentlyContinue
Remove-Item "$env:LOCALAPPDATA\Microsoft\Windows\Explorer\thumbcache_*.db" -Force -ErrorAction SilentlyContinue
Remove-Item "$env:LOCALAPPDATA\IconCache.db" -Force -ErrorAction SilentlyContinue
Start-Process explorer.exe
```
Greift es immer noch nicht: einmal ab-/anmelden oder neu starten.
**Quelle:** woshub.com · ghacks.net (2024) · winhelponline.com · thewindowsclub.com

### 4.3 ⭐ Verknuepfungen (.lnk) cachen ueber den IconLocation-PFAD (nicht mtime)
**Symptom:** Neue `.ico` am gleichen Pfad → Verknuepfung zeigt weiter das alte Icon, selbst nach DB-Reset.
**Ursache:** Cache-Schluessel = `IconLocation-Pfad + Index` (z.B. `C:\App\icon.ico,0`), unabhaengig
von der Datei-Aenderungszeit. Gleicher Pfad = gleicher Schluessel = alter Eintrag.
**FIX (in Reihenfolge):** (1) IconLocation auf einen **neuen Schluessel** zeigen — am robustesten auf
die **`<App>.exe,0`** (eingebettetes Icon, ein Paket, kein externer Pfad der bricht); alternativ die
`.ico` unter neuem Dateinamen (`icon_v2.ico`). (2) DB-Reset (4.2). (3) Explorer neu.
**Schnelltest:** Korrigiert der reine Pfad-/Schluessel-Wechsel das Icon sofort → es war der
IconLocation-Cache, nicht DB-Korruption.
**Quelle:** winaero.com · learn.microsoft.com/troubleshoot/.../application-shortcuts-show-blank-icons

---

## 5. Windows .NET / WPF: Icon einbinden

### 5.1 `ApplicationIcon` (.exe) vs `Window.Icon` (Titelleiste) — beide setzen
**Symptom:** Entweder die .exe hat ein Icon, aber das Fenster zeigt das generische — oder umgekehrt.
**Ursache:** Zwei unabhaengige Mechanismen: `<ApplicationIcon>` (csproj) → in die .exe eingebettet
(Datei-Icon, Fallback); `Window.Icon` → Titelleiste/Taskleiste/ALT+TAB.
**FIX:** Beide setzen: `<ApplicationIcon>assets\app.ico</ApplicationIcon>` UND `Icon="/assets/app.ico"` am `<Window>`.
**Quelle:** learn.microsoft.com/dotnet/api/system.windows.window.icon

### 5.2 ⭐ `Window.Icon`-Datei muss `Resource` sein, NICHT `Content` (sonst bricht single-file)
**Symptom:** "Cannot locate resource" / IOException / leeres Fenster-Icon — besonders nach
`PublishSingleFile=true`.
**Ursache:** Content-Dateien sind im single-file-Bundle loose und per Pack-URI nicht auffindbar;
Resource-eingebettete liegen im Assembly-Manifest (kommt ins Bundle). `<ApplicationIcon>` (Win32-Resource)
ueberlebt single-file ohnehin.
**FIX:** Icon-Datei als `<Resource Include="assets\app.ico" />`; `Icon="/assets/app.ico"` (fuehrender
Slash = Assembly-Root). Defensiv: Pack-URI-Laden in try/catch mit Fallback (kein leeres catch).
**Versionen:** .NET 5+; mit Resource stabil in .NET 6-10. **Quelle:** dotnet/runtime #38636; dotnet/sdk #10670; Microsoft Q&A

### 5.3 Unscharfes Taskleisten-Icon
**Symptom:** Icon pixelig/verwaschen.
**Ursache:** `.ico` enthaelt zu wenige Groessen → Windows skaliert (16 fuer Titelleiste, 32 fuer ALT+TAB).
**FIX:** Multi-Res 16/24/32/48/256 (umfassend bis 256) aus ≥512px-Quelle.
**Quelle:** learn.microsoft.com/dotnet/api/system.windows.window.icon

---

## 6. Android: Adaptive Icons

### 6.1 Motiv abgeschnitten (Safe-Zone)  ⭐
**Symptom:** Logo/Text an den Raendern abgeschnitten, je Launcher unterschiedlich.
**Ursache:** Canvas 108×108dp, aber nur die inneren **66×66dp** sind auf JEDER Maskenform sicher sichtbar.
**FIX:** Kernmotiv in die 66dp-Safe-Zone (Logo ~48-66dp, zentriert). Dekoratives darf bis 108dp.
**Versionen:** API 26+. **Quelle:** developer.android.com/develop/ui/compose/system/icon_design_adaptive

### 6.2 Schwarze/eckige Ecken am Background-Layer
**Symptom:** Schwarze/eckige Ecken hinter dem maskierten Icon.
**Ursache:** Background-Layer hat eigene Rundung/Transparenz; das **System maskiert selbst**.
**FIX:** Background **vollflaechig opak, keine eigene Rundung, keine Transparenz, kein Schatten**.
**Quelle:** medium.com/androiddevelopers/implementing-adaptive-icons-1e4d1795470e

### 6.3 mipmap statt drawable + alle Dichten; PNG-Fallback fuer Legacy
**Symptom:** Unscharfes/fehlendes Icon auf High-DPI / alten Geraeten.
**Ursache:** PNG in `drawable/` (wird beim Install ggf. entfernt) statt `mipmap/`; fehlende Dichte-Buckets.
**FIX:** Adaptive-Layer als Vektor-Drawable; PNG-Fallback `ic_launcher.png` in mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi
(48/72/96/144/192) fuer < API 26. XML nach `res/mipmap-anydpi-v26/ic_launcher.xml`.
**Quelle:** developer.android.com/studio/write/create-app-icons

### 6.4 Themed Icons (Material You) fehlen
**Symptom:** Icon bleibt bei "Themed Icons" bunt/inkonsistent.
**Ursache:** Kein `<monochrome>`-Layer.
**FIX:** `<monochrome android:drawable="@drawable/ic_launcher_mono"/>` — flaches einfarbiges Drawable
(nur ueber Alpha modelliert, kein Schatten/Maske). API 33+.
**Neu (Re-Recherche 2026-07-02):** Ab **Android 16 QPR 2** themt Android App-Icons **automatisch**, auch wenn
die App KEINEN monochrome-Layer liefert (System leitet ihn ab). Der `<monochrome>`-Layer bleibt aber
**empfohlen** — nur er garantiert ein korrektes themed Icon auf **Android 13–16 (vor QPR 2)** und die
Kontrolle ueber das Ergebnis (die Auto-Ableitung kann bei detailreichen Foregrounds schlechter aussehen).
Also: monochrome-Layer weiter mitliefern, nicht auf die Auto-Ableitung verlassen.
**Quelle:** proandroiddev.com/android-13-implementing-themed-icons, developer.android.com (Adaptive Icons, Stand 2026-06-16)

### 6.5 Play-Store-Icon 512px
**Symptom:** Upload abgelehnt / falsche Rundung.
**Ursache:** Falsches Format, oder Entwickler rundet selbst (Play rundet dynamisch).
**FIX:** Exakt **512×512, 32-bit PNG mit Alpha, sRGB, <1024 KB**, volles Quadrat. **Ab 31.03.2026**
rendert Play mit 30 % Corner-Radius → wichtige Elemente in 15-18 % Innen-Padding.
**Quelle:** theapplaunchpad.com/blog/google-play-app-icon-guidelines

---

## 7. macOS: `.icns` (+ Zaesur macOS 26 Tahoe)

> **Wichtig:** Ab **macOS 26 Tahoe** (Herbst 2025) neues System: `.icon` (Icon Composer) → `Assets.car`,
> System maskiert selbst (Liquid-Glass-Squircle). Davor (macOS 15 und aelter): klassisches `.icns`,
> System maskiert NICHT automatisch → Form + Padding **einbacken**.

### 7.1 ⭐ macOS rundet (pre-Tahoe) NICHT automatisch — anders als iOS
**Symptom:** Icon erscheint als volles Quadrat mit harten/schwarzen Ecken statt Squircle.
**Ursache:** Klassisches macOS legt keine Maske an; die HIG-Formulierung ist von iOS kopiert und irrefuehrend.
**FIX:** Squircle + Padding selbst ins PNG einbacken (NICHT die iOS-Vollbild-Quelle nehmen).
Maße bei 1024er Canvas: Body **824×824, Radius ~185, je 100px Gutter**. Aussen transparent.
**Quelle:** developer.apple.com/forums/thread/670578 · liamrosenfeld.com

### 7.2 iconutil: fehlende/falsch benannte Groessen
**FIX:** Ordner `Name.iconset` mit genau 10 PNGs (`icon_16x16.png` … `icon_512x512@2x.png`, Prefix
`icon_` Pflicht, `@2x` = doppelte Pixel), dann `iconutil -c icns Name.iconset`. Robuster: Xcode
Asset-Catalog (`AppIcon.appiconset`) generiert `.icns`/`Assets.car` + Plist-Keys selbst.
**Quelle:** developer.apple.com/library/archive/.../HighResolutionOSX

### 7.3 Tahoe: altes .icns sieht kaputt aus; Cross-Version fragil
**Ursache:** Neues `.icon`-Format (Icon Composer, max 4 Layer) via `actool`; Rueckwaerts-Kompat zwischen
Tahoe und aelter ab Xcode 26.1 "by design" gebrochen.
**FIX:** Mit Icon Composer `.icon` fuer Tahoe; klassisches Squircle-`.icns` als Basis fuers Min-Target.
`CFBundleIconName` (Asset-Catalog) bzw. `CFBundleIconFile` (klassisch) in Info.plist setzen.
**Workflow im Detail (Re-Recherche 2026-07-02):** `.icon` ist ein **Ordner** (Vektoren + JSON, Finder blendet
die Endung aus). (1) Mit **Icon Composer** erzeugen (max 4 Layer). (2) Mit **`actool`** (braucht Xcode-Toolchain)
zu **`Assets.car`** kompilieren, z. B. `xcrun actool app.icon --compile <out> --app-icon Icon --include-all-app-icons
--minimum-deployment-target 26.0 --platform macosx --output-partial-info-plist temp.plist`. (3) **BEIDE** —
`Assets.car` UND die alte `.icns` — in `Contents/Resources` legen, **vor** dem Signieren (Tahoe zeigt Liquid-Glass,
aeltere macOS das `.icns`). (4) `CFBundleIconName` (→ Icon-Name) UND `CFBundleIconFile` (→ `AppIcon.icns`) in die
Info.plist. **Fallen:** die „gleicher Name fuer `.icon` + `.appiconset`"-Technik ist ab **Xcode 26.1** weg (nutze
getrennte Namen / `--include-all-app-icons`, sonst App-Store-Reject `ITMS-90236`). Erscheinungsbild-Varianten des
`.icon` (Default/Dark/Clear-Light/Clear-Dark/Tinted) betreffen nur Tahoe. Fuer die kommenden Jahre BEIDE Formate ausliefern.
**Quelle:** successfulsoftware.net/2025/09/26/... · hendrik-erz.de/... · mjtsai.com/blog/2025/08/08/... (Updates bis 2026-02)

### 7.4 macOS-Icon-Cache
**FIX:** `sudo rm -rf /Library/Caches/com.apple.iconservices.store` + Dock-iconcache loeschen →
`killall Dock; killall Finder` (vorher Backup, `rm` ist destruktiv).
**Quelle:** developer.apple.com/forums/thread/676723 · osxdaily.com/2022/05/23/clear-icon-cache-mac

---

## 8. Build-Tools — die uebergreifende Falle: Alpha → Schwarz/Weiss

| Tool | Falle | FIX |
|------|-------|-----|
| **ImageMagick** | `-flatten`/`-border`/`-background <farbe>` flacht Alpha ab | nur `-define icon:auto-resize=...`, kein flatten; `-alpha off [op] -alpha on` |
| **Pillow** | fehlendes `convert("RGBA")` / fehlende `sizes` | beides explizit; Groessen nie doppeln |
| **Tauri** (`cargo tauri icon`) | Quelle muss quadratisch 1024 RGBA; iOS verbietet Alpha; macOS kein Padding | Quelle 1024² RGBA; fuer iOS Alpha-freie Extra-PNGs; macOS-Quelle mit ~10 % Rand |
| **electron-builder** | Default-Icon / APPX-Akzentfarbe hinter Icon | `build.icon` setzen; transparentes PNG + APPX-Assets |
| **@capacitor/assets** | round-Variante bekommt Hintergrund; 18dp Safe-Margin | transparente Quelle; Motiv in zentrale 66 % |
| **Online-Konverter** | stilles Re-Encoding, 1-bit-Alpha, Datenschutz | lokal (Pillow/ImageMagick/Tauri) bevorzugen; Ergebnis auf Groessen+Alpha pruefen |

**Goldene Regel:** Niemals `-flatten`/`-background <farbe>`/`-border` bei transparenten Icons;
Pillow immer `convert("RGBA")` + explizite `sizes`.
**Quelle:** ImageMagick #6361; Pillow #2405/#6121; v2.tauri.app/develop/icons; electron.build/icons

---

## 9. Master-Asset-Strategie (ein Quell-Asset → alle Plattformen)

**Master:** ein **1024×1024 PNG mit straight (unpremultiplied) Alpha, quadratisch**. Daraus ableiten:
- **Windows `.ico`:** transparente (ggf. abgerundete) Ecken, 16/24/32/48/64/128/256.
- **Android:** 512² PNG-Alpha (Play), Adaptive-Layer volles Quadrat (Background opak, Foreground in 66 %-Safe-Zone).
- **macOS (klassisch):** Body 824² in 1024, 100px Gutter, Squircle eingebacken; (Tahoe: flaches Vollbild, System maskiert).
- **iOS:** 1024², Alpha **entfernen** (opak), volles Quadrat.
**Nie selbst runden, wo das OS maskiert. Nie Alpha verlieren.**
**Quelle:** medium.com/design-bootcamp/creating-app-icons-for-macos-11-and-up · iconikai.com/blog/app-icon-size-chart-2026

---

## Fix-Status (Ehrlichkeit)

Die meisten Punkte hier sind **"per Design" / versionsunabhaengig** (Alpha-Verhalten, OS-Maskierung,
Cache-Mechanik) — kein Versions-Fix erwartet. Aktiv versions-abhaengig:

| Thema | Aenderung ab | Bezug |
|-------|-------------|-------|
| Play-Store rendert 30 % Corner-Radius automatisch | **31.03.2026** | §6.5 — Padding 15-18 % einplanen |
| macOS Icon-System neu (`.icon`/Liquid Glass) | **macOS 26 Tahoe** (Herbst 2025) | §7.3 — Icon Composer |
| Cross-Version-Icon (Tahoe ↔ alt) gebrochen | **Xcode 26.1** | §7.3 — by design, Workaround fragil |

Nicht gefixt / dauerhaft: schwarze Ecken bei opaker Quelle (§2.1), Windows-Cache-Haertnaeckigkeit (§4),
Konverter-Alpha-Flatten (§8) — Workarounds bleiben aktiv.

---

## ✅ Pflicht-Checkliste vor "Icon fertig"

```
□ Quelle ist PNG mit echtem Alpha (RGBA), Ecken Alpha=0 — geprueft (Alpha-min == 0)?
□ Nicht selbst gerundet, wo das OS maskiert (Android/iOS/macOS-Tahoe = volles Quadrat)?
□ Windows: .ico mit 16/24/32/48/256 aus ≥512px-Quelle, kein -flatten/-background?
□ WPF: ApplicationIcon + Window.Icon gesetzt, Icon-Datei als Resource (nicht Content)?
□ Nach Icon-Aenderung auf Windows: Cache verlaesslich geleert (DBs + Explorer), neuer IconLocation-Schluessel (.exe,0)?
□ Android: Background opak ohne Rundung, Motiv in 66dp-Safe-Zone, mipmap alle Dichten, monochrome-Layer?
□ macOS: Squircle+Padding eingebacken (klassisch) bzw. Icon Composer (Tahoe), Plist-Key gesetzt?
□ Konverter-Ergebnis auf Groessen UND Alpha geprueft?
```

---

## 🔗 Bezug zur Best-Practices-Gegenseite

Bug-Almanach (diese Datei) ↔ Best-Practices [`best-practices/assets/icon-building.md`](../../best-practices/assets/icon-building.md). Die gespiegelte Tabelle steht auch dort. Links der *Bug*, rechts die *Best-Practice, die ihn verhindert*.

| Bug-Abschnitt (dieser Almanach) | Verhindert durch Best-Practice |
|---------------------------------|--------------------------------|
| §2.1 schwarze Ecken | Master-Asset + Alpha-Pflicht |
| §2.2 Doppelrundung, §6.2 Android, §7.1 macOS | Nie selbst runden wo OS maskiert |
| §2.3 Anti-Aliasing-Halo | Supersampling-Maske |
| §3.1, §3.2, §8 | Pillow RGBA+sizes / IM kein flatten |
| §5.1, §5.2 | WPF ApplicationIcon + Window.Icon als Resource |
| §4 | Windows-Cache-Reset + .exe,0 |
