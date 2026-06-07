# App-Icon-Building — Best Practices (Stand 2026-06-07)

**Versions-Anker:** Windows 11 Build 26200 / .NET 10 / Pillow 12.1.1 / ImageMagick 7,
Android 16 (API 36), macOS 15 Sequoia + macOS 26 Tahoe.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/assets/icon-building.md`](../../../bugs/assets/icon-building.md)):
> der Almanach sagt *was schiefgeht*, diese Datei sagt *wie man es von vornherein richtig macht*.

---

## TL;DR — die Defaults, die man einmal richtig setzt

1. **EIN Master-Asset:** 1024×1024 PNG mit **straight (unpremultiplied) Alpha**, quadratisch, Motiv in der Safe-Zone. Daraus alle Plattform-Formate ableiten — nie pro Plattform neu malen.
2. **Alpha ist Pflicht.** Quelle nie als JPEG/abgeflachtes PNG. KI-Output (DALL-E/Midjourney/Nano Banana) hat **kein** Alpha → Hintergrund vor Verwendung entfernen. Nach jeder Konvertierung Alpha prüfen (`Image.getchannel("A").getextrema()` → min == 0).
3. **Nie selbst runden, wo das OS maskiert** (Android, iOS, macOS Tahoe = volles Quadrat liefern). Nur Windows-`.ico` und klassisches macOS-`.icns` brauchen Form/Padding **eingebacken**.
4. **Konverter ohne Alpha-Flatten:** Pillow `img.convert("RGBA")` + explizite `sizes`; ImageMagick nur `-define icon:auto-resize=...`, NIEMALS `-flatten`/`-border`/`-background <farbe>`.
5. **Nach Icon-Änderung auf Windows den Cache verlässlich leeren** (DBs + Explorer-Neustart) und IconLocation auf einen frischen Schlüssel (`<App>.exe,0`) — `ie4uinit` allein reicht nicht.

---

## Pillow — fehlerfreier `.ico`-Workflow (Referenz)

```python
from PIL import Image, ImageDraw
img = Image.open(src).convert("RGBA")            # 1. RGBA-Pflicht
# 2. abgerundete/transparente Ecken via Supersampling (weiche Kante)
w, h = img.size
mask = Image.new("L", (w*5, h*5), 0)
ImageDraw.Draw(mask).rounded_rectangle([0,0,w*5-1,h*5-1], radius=int(w*5*0.22), fill=255)
mask = mask.resize((w, h), Image.LANCZOS)
img.putalpha(mask)
assert img.getchannel("A").getextrema()[0] == 0   # 3. Alpha prüfen (Ecken transparent)
img.save("out.ico", format="ICO",                  # 4. multi-res, 256 PNG-komprimiert (Default)
         sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
```
(Bei bereits abgerundetem Motiv mit schwarzen Ecken stattdessen nahe-schwarze Pixel → Alpha 0.)

## WPF (.NET) — Icon robust einbinden

- `.csproj`: `<ApplicationIcon>assets\app.ico</ApplicationIcon>` **und** `<Resource Include="assets\app.ico" />`.
- `<Window … Icon="/assets/app.ico">` (führender Slash = Assembly-Root). Datei-Build-Action = **Resource**, nie Content (sonst bricht single-file-Publish).
- Verknüpfung: IconLocation auf **`app.exe,0`** (eingebettetes Icon) statt externe `.ico` — robuster Cache-Schlüssel, kein externer Pfad der bricht.

## Android

- Background-Layer vollopak, **keine** eigene Rundung/Transparenz; Foreground-Motiv in der **66dp-Safe-Zone** (von 108dp). Adaptive-Layer als Vektor; PNG-Fallback in allen mipmap-Dichten (48/72/96/144/192). `<monochrome>`-Layer für Themed Icons. Play-Store: 512×512 PNG-Alpha, volles Quadrat, 15-18 % Innen-Padding (Play rundet ab 31.03.2026 mit 30 % Radius).

## macOS

- Klassisch (`.icns`): Squircle + ~100px Gutter (bei 1024) **einbacken**, `iconutil -c icns Name.iconset` mit allen 10 Größen; besser Xcode Asset-Catalog. Tahoe (macOS 26): Icon Composer → `.icon`, System maskiert selbst. Plist-Key `CFBundleIconName`/`CFBundleIconFile` setzen.

---

## Kopplung zum Bug-Almanach

| Best-Practice (hier) | verhindert Bug in `bugs/assets/icon-building.md` |
|----------------------|--------------------------------------------------|
| Master-Asset + Alpha-Pflicht | §2.1 schwarze Ecken |
| Nie selbst runden wo OS maskiert | §2.2 Doppelrundung, §6.2 Android, §7.1 macOS |
| Supersampling-Maske | §2.3 Anti-Aliasing-Halo |
| Pillow RGBA+sizes / IM kein flatten | §3.1, §3.2, §8 |
| WPF ApplicationIcon+Window.Icon als Resource | §5.1, §5.2 |
| Windows-Cache-Reset + .exe,0 | §4 |
