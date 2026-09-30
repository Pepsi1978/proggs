---
name: modul-bibliothek
description: Modul-Bibliothek unter ~/proggs/Module/ — Kopie statt Verdrahtung; zwei Schichten Modulkopie und Anbindung
metadata: 
  node_type: memory
  type: project
  originSessionId: 3810f5b5-113e-43a7-bb89-82032a8f4c9a
  modified: 2026-09-12T11:41:56.849Z
---

Am 12.09.2026 aufgesetzt: `~/proggs/Module/` mit Nummernkreisen M1 Android,
M2 Windows, M3 macOS, M4 iOS, plus Skill `modul-erstellen`.

**Warum Kopie statt lebendem Link:** Frank hat sich bewusst gegen `srcDir`
entschieden (wie bei [[claude-profile-architektur-launcher]] KompassKern) —
jeder App-Ordner muss allein baubar bleiben, auch ohne Netz und ohne den
Modulordner. Der Preis, Fixes mehrfach verteilen zu müssen, ist akzeptiert.

**How to apply:** Zwei Skills bilden das Paar — `modul-erstellen` (App-Code zu Modul UND Modul aendern/Version anheben — die Bibliothek gehoert diesem Skill)
und `modul-einbauen` (Modul in App, plus Nachziehen an alle
Konsumenten). Beim Einbau gilt die Zweischichtung: Modulkopie byte-identisch
und unantastbar, daneben eine app-eigene `Anbindung.<ext>` fuer Aussehen,
Texte und Schnittstellen — nur so gehen Funktionen 1:1 und angepasstes
Aussehen zusammen.

**Stand 12.09.2026:** M1.1 Sicherung steht bei v3 mit vier Konsumenten. Die
drei Kompass-Apps (geteilter Pfad `KompassKern/`) haengen auf v2 und warten auf
"zieh M1.1 nach"; GenialeIdeen steht auf v3. Beide Skills sind damit an einem
echten Fall erprobt — inklusive Abloesung eines Eigenbaus mit Datenpruefung
Fall C: Das Kopf-Feld hiess dort `schemaVersion` statt `schema`, weshalb das
Modul erst um `SicherungsInhalt.kopfAliase` erweitert wurde, statt die
Altdateien aufzugeben. Merksatz daraus: Bei einer Abloesung entscheidet ein
einziger Feldname darueber, ob alle bisherigen Sicherungen wertlos werden —
zuerst das Modul erweitern, dann einbauen.
