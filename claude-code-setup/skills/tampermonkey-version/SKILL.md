---
name: tampermonkey-version
description: Use AUTOMATICALLY whenever ANY Tampermonkey userscript (.user.js) in ~/proggs/Tampermonkey/ is modified. Ensures version numbers are bumped before committing.
---

# Tampermonkey Versionsnummern-Pflicht

## Wann

Dieses Skill gilt IMMER wenn eine oder mehrere `.user.js` Dateien in `~/proggs/Tampermonkey/` geaendert werden - egal ob Code, Konfiguration, Styling oder Positionierung.

## Regeln

1. **Patch-Version erhoehen** (z.B. 1.3.4 -> 1.3.5) bei jeder Aenderung
2. **Minor-Version erhoehen** (z.B. 1.3.4 -> 1.4.0) bei neuen Features
3. **Beide Stellen aktualisieren**: Sowohl `@name` als auch `@version` im UserScript-Header muessen uebereinstimmen

### Beispiel

Vorher:
```
// @name         Claude V.1.3.0
// @version      1.3.0
```

Nachher:
```
// @name         Claude V.1.3.1
// @version      1.3.1
```

## Ablauf

1. Aenderungen am Skript-Code vornehmen
2. **VOR dem Commit**: Versionsnummer in `@name` und `@version` erhoehen
3. Erst dann committen

## Wichtig

- NIEMALS eine Tampermonkey-Aenderung ohne Versionsupdate committen
- Wenn mehrere Skripte gleichzeitig geaendert werden, JEDES einzelne Skript erhoehen
- Das ChatGPT-Skript (chatgpt.user.js) ist die Referenz fuer UI-Positionierung (rightPx: 16 + UI_SHIFT_LEFT_PX: 11.34 = ~27px)

## Shared Knowledge Hub Integration

**Whiteboard**: `.claude/agent-memory/shared/MEMORY.md` (EINZIGE zentrale Wissensdatei)

**Lesen**: Vor der Ausfuehrung das Whiteboard lesen fuer bekannte Probleme mit Tampermonkey-Skripten oder Versionsnummern.

**Schreiben bei Fehlern**: Wenn der Versions-Bump oder der Commit fehlschlaegt, in "Offene Fehler & Probleme" eintragen:
- Quelle: `tampermonkey-version`
- Symptom: [Was hat nicht funktioniert]
- Betroffene Datei: [Pfad zum .user.js]
- Fix-Vorschlag: [Manueller Schritt]
- Status: OFFEN
