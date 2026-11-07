---
name: modul-bibliothek
description: Modul-Bibliothek unter ~/proggs/Module/ — Kopie statt Verdrahtung; zwei Schichten Modulkopie und Anbindung
metadata:
  node_type: memory
  type: project
  originSessionId: 3810f5b5-113e-43a7-bb89-82032a8f4c9a
  modified: 2026-11-07T13:54:15.544Z
---

Seit 12.09.2026: `~/proggs/Module/` mit Nummernkreisen M1 Android, M2 Windows, M3 macOS, M4 iOS, plus Skill `modul-erstellen`.

**Why (Kopie statt lebendem Link):** Frank hat sich bewusst gegen `srcDir` entschieden (wie bei [[claude-profile-architektur-launcher]] KompassKern): jeder App-Ordner muss allein baubar bleiben, auch ohne Netz und ohne Modulordner. Fixes mehrfach verteilen zu müssen ist akzeptiert.

**How to apply:**
- Skill-Paar: `modul-erstellen` (App-Code zu Modul UND Modul ändern/Version anheben; die Bibliothek gehört diesem Skill) und `modul-einbauen` (Modul in App, plus Nachziehen an alle Konsumenten).
- Zweischichtung beim Einbau: Modulkopie byte-identisch und unantastbar, daneben app-eigene `Anbindung.<ext>` für Aussehen, Texte und Schnittstellen; nur so gehen Funktionen 1:1 und angepasstes Aussehen zusammen.
- Bei Ablösung eines Eigenbaus entscheidet ein einziger Feldname, ob alle bisherigen Sicherungen wertlos werden: zuerst das Modul erweitern, dann einbauen.

**Stand 12.09.2026:** M1.1 Sicherung bei v3 mit vier Konsumenten. Die drei Kompass-Apps (geteilter Pfad `KompassKern/`) hängen auf v2 und warten auf "zieh M1.1 nach"; GenialeIdeen auf v3. Beide Skills an echtem Fall erprobt, inkl. Ablösung eines Eigenbaus mit Datenprüfung Fall C: Kopf-Feld hieß dort `schemaVersion` statt `schema`, daher Modul erst um `SicherungsInhalt.kopfAliase` erweitert, statt die Altdateien aufzugeben.
