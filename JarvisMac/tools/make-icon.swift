import AppKit
// Zeichnet das Jarvis-Symbol (leuchtende Kugel auf dunklem, abgerundetem Quadrat) als 1024-px-PNG.
let groesse = 1024.0
let bild = NSImage(size: NSSize(width: groesse, height: groesse))
bild.lockFocus()
let rahmen = NSRect(x: 80, y: 80, width: groesse - 160, height: groesse - 160)
let form = NSBezierPath(roundedRect: rahmen, xRadius: 190, yRadius: 190)
NSGradient(colors: [NSColor(red: 0.05, green: 0.06, blue: 0.11, alpha: 1), NSColor(red: 0.02, green: 0.03, blue: 0.06, alpha: 1)])!.draw(in: form, angle: -90)
form.addClip()
let mitte = NSPoint(x: groesse / 2, y: groesse / 2)
NSGradient(colors: [NSColor(red: 0.22, green: 0.88, blue: 1, alpha: 0.55), .clear])!
    .draw(fromCenter: mitte, radius: 0, toCenter: mitte, radius: 420, options: [])
NSGradient(colors: [.white, NSColor(red: 0.22, green: 0.88, blue: 1, alpha: 1), NSColor(red: 0.55, green: 0.36, blue: 0.96, alpha: 1), NSColor(red: 0.55, green: 0.36, blue: 0.96, alpha: 0)])!
    .draw(fromCenter: NSPoint(x: mitte.x - 50, y: mitte.y + 60), radius: 0, toCenter: mitte, radius: 270, options: [])
let ring = NSBezierPath(ovalIn: NSRect(x: mitte.x - 330, y: mitte.y - 330, width: 660, height: 660))
ring.lineWidth = 10
NSColor(red: 0.22, green: 0.88, blue: 1, alpha: 0.45).setStroke()
ring.stroke()
bild.unlockFocus()
let rep = NSBitmapImageRep(data: bild.tiffRepresentation!)!
try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: CommandLine.arguments[1]))
