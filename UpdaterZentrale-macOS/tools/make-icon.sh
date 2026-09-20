#!/bin/bash
# Erzeugt UpdaterZentrale/Resources/AppIcon.icns.
#
# Gegenstueck zu UpdateZentrale/make_icon.ps1 (Windows, erzeugt app.ico). Beide Fassungen tragen
# dasselbe Bild: ein Aufwaertspfeil in einem offenen Kreis auf violett-blauem Verlauf.
#
# Warum gezeichnet statt aus app.ico umgewandelt: Windows-Symbole sind hoechstens 256 px gross und
# fuellen ihre Flaeche randlos. macOS erwartet 1024 px und ein Motiv, das MIT Rand in einem
# abgerundeten Quadrat sitzt - eine blosse Umwandlung saehe im Dock neben jeder anderen App falsch
# aus. Deshalb wird das Motiv hier in der richtigen Geometrie neu gezeichnet.
set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RES_DIR="$PROJECT_DIR/UpdaterZentrale/Resources"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

mkdir -p "$RES_DIR"

if [ -x "/Library/Developer/CommandLineTools/usr/bin/swift" ]; then
    export DEVELOPER_DIR="/Library/Developer/CommandLineTools"
fi

cat > "$WORK_DIR/zeichne.swift" <<'SWIFT'
import AppKit

// Zeichnet das Symbol in 1024 x 1024 und legt es als PNG ab.
let kante: CGFloat = 1024
let bild = NSImage(size: NSSize(width: kante, height: kante))
bild.lockFocus()

guard let ctx = NSGraphicsContext.current?.cgContext else { exit(1) }
ctx.setShouldAntialias(true)

// --- Grundform ---
// macOS-Symbole fuellen ihre Leinwand nicht randlos aus: das Motiv sitzt mit Luft in einem
// abgerundeten Quadrat. Die Werte entsprechen Apples Vorgabe fuer App-Symbole.
let rand = kante * 0.098
let flaeche = CGRect(x: rand, y: rand, width: kante - rand * 2, height: kante - rand * 2)
let radius = flaeche.width * 0.2237
let form = CGPath(roundedRect: flaeche, cornerWidth: radius, cornerHeight: radius, transform: nil)

// Weicher Schlagschatten, damit das Symbol im Dock aufliegt.
ctx.saveGState()
ctx.setShadow(offset: CGSize(width: 0, height: -kante * 0.012), blur: kante * 0.028,
              color: NSColor.black.withAlphaComponent(0.28).cgColor)
ctx.addPath(form)
ctx.setFillColor(NSColor.white.cgColor)
ctx.fillPath()
ctx.restoreGState()

// --- Verlauf: dieselben Farben wie der Akzentverlauf der App (#7C5CFF -> #4DA3FF) ---
ctx.saveGState()
ctx.addPath(form)
ctx.clip()

let farbraum = CGColorSpaceCreateDeviceRGB()
let farben = [
    NSColor(srgbRed: 0.639, green: 0.408, blue: 1.0, alpha: 1).cgColor,   // #A368FF, heller Einstieg
    NSColor(srgbRed: 0.486, green: 0.361, blue: 1.0, alpha: 1).cgColor,   // #7C5CFF
    NSColor(srgbRed: 0.302, green: 0.639, blue: 1.0, alpha: 1).cgColor    // #4DA3FF
] as CFArray
if let verlauf = CGGradient(colorsSpace: farbraum, colors: farben, locations: [0.0, 0.42, 1.0]) {
    ctx.drawLinearGradient(verlauf,
                           start: CGPoint(x: flaeche.minX, y: flaeche.maxY),
                           end: CGPoint(x: flaeche.maxX, y: flaeche.minY),
                           options: [])
}

// Sanfter Lichtschein oben links - dieselbe Idee wie der Schein hinter den Karten im Fenster.
if let schein = CGGradient(colorsSpace: farbraum,
                           colors: [NSColor.white.withAlphaComponent(0.26).cgColor,
                                    NSColor.white.withAlphaComponent(0.0).cgColor] as CFArray,
                           locations: [0.0, 1.0]) {
    ctx.drawRadialGradient(schein,
                           startCenter: CGPoint(x: flaeche.minX + flaeche.width * 0.24,
                                                y: flaeche.maxY - flaeche.height * 0.14),
                           startRadius: 0,
                           endCenter: CGPoint(x: flaeche.minX + flaeche.width * 0.24,
                                              y: flaeche.maxY - flaeche.height * 0.14),
                           endRadius: flaeche.width * 0.72,
                           options: [])
}
ctx.restoreGState()

// --- Motiv ---
let mitte = CGPoint(x: flaeche.midX, y: flaeche.midY)
let weiss = NSColor(srgbRed: 1, green: 1, blue: 1, alpha: 0.97)

// Offener Kreis: oben ausgespart, damit der Pfeil hindurchstoesst.
// Gezeichnet mit NSBezierPath und Gradangaben - CGMutablePath.addArc rechnet im Bogenmass und in
// der gespiegelten Achse des lockFocus-Kontexts, was die Luecke an die falsche Stelle legt.
ctx.saveGState()
ctx.setShadow(offset: CGSize(width: 0, height: -kante * 0.006), blur: kante * 0.018,
              color: NSColor(srgbRed: 0.15, green: 0.10, blue: 0.35, alpha: 0.32).cgColor)
let ringRadius = flaeche.width * 0.330
let ringStaerke = flaeche.width * 0.092
let ring = NSBezierPath()
// Von 124 Grad mit STEIGENDEM Winkel ueber links, unten und rechts bis 56 Grad: das laesst die
// Luecke oben, genau dort, wo die Pfeilspitze durchstoesst. `clockwise: false` heisst in der
// nicht gespiegelten Achse des lockFocus-Kontexts "Winkel nimmt zu".
ring.appendArc(withCenter: NSPoint(x: mitte.x, y: mitte.y), radius: ringRadius,
               startAngle: 124, endAngle: 56, clockwise: false)
ring.lineWidth = ringStaerke
ring.lineCapStyle = .round
weiss.setStroke()
ring.stroke()
ctx.restoreGState()

// Aufwaertspfeil - sitzt IM Ring, die Spitze stoesst durch die Luecke.
ctx.saveGState()
ctx.setShadow(offset: CGSize(width: 0, height: -kante * 0.006), blur: kante * 0.016,
              color: NSColor(srgbRed: 0.15, green: 0.10, blue: 0.35, alpha: 0.30).cgColor)

let schaftBreite = flaeche.width * 0.105
let spitzeBreite = flaeche.width * 0.268
let spitzeHoehe = flaeche.height * 0.205
let obenY = mitte.y + flaeche.height * 0.372
let untenY = mitte.y - flaeche.height * 0.230

let pfeil = NSBezierPath()
pfeil.move(to: NSPoint(x: mitte.x, y: obenY))                                       // Spitze
pfeil.line(to: NSPoint(x: mitte.x + spitzeBreite / 2, y: obenY - spitzeHoehe))
pfeil.line(to: NSPoint(x: mitte.x + schaftBreite / 2, y: obenY - spitzeHoehe))
pfeil.line(to: NSPoint(x: mitte.x + schaftBreite / 2, y: untenY))
pfeil.line(to: NSPoint(x: mitte.x - schaftBreite / 2, y: untenY))
pfeil.line(to: NSPoint(x: mitte.x - schaftBreite / 2, y: obenY - spitzeHoehe))
pfeil.line(to: NSPoint(x: mitte.x - spitzeBreite / 2, y: obenY - spitzeHoehe))
pfeil.close()
weiss.setFill()
pfeil.fill()
ctx.restoreGState()

// Kein Sockel unter dem Pfeil: der Ring laeuft an dieser Stelle dicht darunter vorbei, eine
// zusaetzliche Standlinie verschmilzt dort mit ihm zu einem Fleck. Die Windows-Fassung hat unten
// mehr Luft und kann sich den Sockel leisten.

bild.unlockFocus()

guard let tiff = bild.tiffRepresentation,
      let bitmap = NSBitmapImageRep(data: tiff),
      let png = bitmap.representation(using: .png, properties: [:]) else { exit(1) }

let ziel = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "icon.png"
try! png.write(to: URL(fileURLWithPath: ziel))
SWIFT

echo "  -> zeichne Symbol (1024 x 1024)"
swift "$WORK_DIR/zeichne.swift" "$WORK_DIR/icon-1024.png"

# Alle von macOS erwarteten Groessen in ein .iconset legen; iconutil macht daraus das .icns.
ICONSET="$WORK_DIR/AppIcon.iconset"
mkdir -p "$ICONSET"
for groesse in 16 32 64 128 256 512 1024; do
    sips -z "$groesse" "$groesse" "$WORK_DIR/icon-1024.png" \
         --out "$ICONSET/icon_${groesse}x${groesse}.png" >/dev/null 2>&1
done

# Die @2x-Varianten sind Kopien der jeweils doppelten Kantenlaenge.
mv "$ICONSET/icon_32x32.png"     "$ICONSET/icon_16x16@2x.png.tmp"
mv "$ICONSET/icon_64x64.png"     "$ICONSET/icon_32x32@2x.png"
mv "$ICONSET/icon_256x256.png"   "$ICONSET/icon_128x128@2x.png.tmp"
mv "$ICONSET/icon_1024x1024.png" "$ICONSET/icon_512x512@2x.png"
cp "$ICONSET/icon_16x16@2x.png.tmp" "$ICONSET/icon_32x32.png"
mv "$ICONSET/icon_16x16@2x.png.tmp" "$ICONSET/icon_16x16@2x.png"
cp "$ICONSET/icon_128x128@2x.png.tmp" "$ICONSET/icon_256x256.png"
mv "$ICONSET/icon_128x128@2x.png.tmp" "$ICONSET/icon_128x128@2x.png"
sips -z 512 512 "$WORK_DIR/icon-1024.png" --out "$ICONSET/icon_256x256@2x.png" >/dev/null 2>&1
cp "$ICONSET/icon_256x256@2x.png" "$ICONSET/icon_512x512.png"

iconutil --convert icns "$ICONSET" --output "$RES_DIR/AppIcon.icns"

# Vorschau neben dem Symbol ablegen - wie icon-vorschau.png in der Windows-Fassung.
cp "$WORK_DIR/icon-1024.png" "$PROJECT_DIR/icon-vorschau.png"

echo "  -> $RES_DIR/AppIcon.icns"
