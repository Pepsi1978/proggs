# Auftrag an den Designer — StackLabor

Stand: 14.08.2026 · Erzeugt von: `spec-schmiede` (Stufe 1) · Herkunft: `Specs/StackLabor/v1/`

## Vorgesehene Zielplattform

**Android.** Das ist die Absicht aus Stufe 1. Beim Herunterladen fragt Werft Studio das
Zielsystem noch einmal ausdrücklich ab und übersetzt die Specs in dessen Sprache. Weichen beide
ab, gilt die Wahl beim Herunterladen.

**Leitgröße ist das zugeklappte Cover-Display des Galaxy Z Fold 8:**
1248 × 1972 px @ 420 dpi ≈ **297 × 469 dp**. Nutzbar nach Status- und Gestenleiste: **421 dp**.
Zusätzlich ist ein zweispaltiges Layout für das aufgeklappte Innendisplay
(1848 × 2448 px ≈ **440 × 583 dp**, 120 Hz) vorgesehen — siehe Teil B §10.

Die Systemschrift des Benutzers steht auf **90 %**. Alle sp-Werte im Spec sind darauf gerechnet.

## Der Auftrag

**Baue jeden Bildschirm aus Teil B §6 in jeder Erscheinung auf.** Das sind 15 Bildschirme
(B-01 bis B-15) in zwei vollständigen Erscheinungen: **Hell ist der Standard**, Dunkel ist
gleichwertig und wird über einen Umschalter im Kopf von B-01 erreicht. Keine der beiden
Erscheinungen ist eine Nebensache — es darf keinen Bildschirm geben, der nur in einer gebaut ist.

Gestalterisch ist ausdrücklich viel gewünscht: Der Benutzer hat „sehr viele verschiedene
Spezialeffekte in jeder Hinsicht" verlangt. Teil B §5 und Teil C nennen die Effekt-Familien mit
ihrem Ort. **Eine einzige Einschränkung:** Weichzeichner (Glasflächen) nur auf **festen**
Flächen — Kopfleiste, Sockel, Blätter — **niemals** über einer scrollenden Liste. Das ist der
einzige Effekt, der auf 120 Hz messbar ins Stocken führt.

## Die drei Dinge, die nicht verhandelbar sind

1. **Die Ampelfarben sind die lautesten Elemente im Bild.** Kein Schmuck, kein Verlauf und
   keine Akzentfarbe darf mit ihnen um Aufmerksamkeit konkurrieren oder ihnen ähneln.
   Grün, Gelb, Rot und Grau bedeuten in dieser App jeweils genau eine Sache.
2. **Der Mittel-Eintrag ist zweizeilig.** Das ist keine Geschmacksfrage: Einzeilig bleibt für
   den Namen rechnerisch 1 dp übrig (die Rechnung steht in Teil B §6a). Wer ihn einzeilig
   entwirft, entwirft etwas Unbaubares.
3. **Der Ziel-Bereich in B-02 verdrängt die Mittel-Liste nicht**, sondern legt sich als
   Überlagerung darüber. Verdrängt er, bleiben 77 dp und damit 1,3 Einträge übrig.

## Regeln für Ergänzungen

- **Jedes neue Bedienelement braucht eine Aufgabe.** Wer einen Knopf hinzufügt, beschreibt in
  einem Satz, was er tun soll — sonst entsteht beim Bauen ein toter Knopf, und beim Rückimport
  muss nachgefragt werden.
- **Kennungen bleiben erhalten.** Ein Bildschirm, der hier `B-03` heißt, heißt auch im Rücklauf
  `B-03`. Dasselbe gilt für `F-`, `M-` und `A-`.
- **Neues bekommt die nächste freie Nummer** und wird als `NEU` gekennzeichnet.
- **Weggelassenes** wird als `ENTFALLEN` gekennzeichnet, mit einer kurzen Begründung — statt
  einfach zu fehlen.

## Was ausdrücklich nicht ins Design gehört

- Kein Erststart-Ablauf, keine Einführung, keine Tour. Die App ist ausschließlich für ihren
  einen Benutzer; er kennt sie.
- Keine Datenschutz-, Zustimmungs- oder Rechtstexte.
- Keine Anmeldung außer der Codex-Geräteanmeldung (B-11).
- Keine Fotos und keine Illustrationen. Die einzigen Bildelemente sind Ampeln,
  Löslichkeitspunkte und Symbole (Material Symbols Rounded).
- Kein Kalender und keine Einnahme-Historie. StackLabor ist ein Komponier-Werkzeug, kein
  Tracker — das leistet eine andere App.
- Keine Werbe-, Abo- oder Bezahlflächen.

## Ablage des Rücklaufs

`~/proggs/Designs/Outbox/StackLabor-SPEC-v2.zip`

Danach übernimmt Stufe 2 (`spec-rueckimport`): Sie gleicht den Rücklauf gegen diese Fassung ab,
fragt zu allem Neuen nach und schreibt daraus das Bau-Spec-Paket `Specs/StackLabor/v2/`.
