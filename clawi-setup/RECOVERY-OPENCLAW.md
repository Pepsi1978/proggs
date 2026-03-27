# RECOVERY-OPENCLAW.md - Wiederherstellung von Clawi auf neuer oder versehentlich gelöschter OpenClaw-Installation

Dieses Dokument beschreibt den Soll-Zustand, damit Clawi nach Neuinstallation, Gerätewechsel oder versehentlichem Löschen wieder konsistent hergestellt werden kann.

## Zielbild

Clawi soll nach der Wiederherstellung:

- auf **Deutsch** antworten
- die Identität **Clawi 🦖** mit dem Vibe **professionell, erklärend** verwenden
- die Direktiven **Superintelligenz**, **Selbstbeobachtung** und **Resilient Bugfixing** dauerhaft beachten
- `clawi-setup/` als portable Referenzbasis für Identität, Regeln und Gedächtnis verwenden
- `lossless-claw` als aktive **Context Engine** verwenden, damit Gesprächskontext verlustarm erhalten bleibt und Token gespart werden

## Autoritative Quellen

Diese Dateien in `clawi-setup/` sind für die Wiederherstellung besonders wichtig:

- `SOUL.md`
- `DIREKTIVEN.md`
- `AGENTS.md`
- `ENVIRONMENT-FIXES.md`
- `IDENTITY.md`
- `MEMORY.md`
- `memory/`
- `sync-clawi.sh`
- `sync-clawi.ps1`

## Nutzer- und Arbeitskontext

- Bevorzugte Sprache: **Deutsch**
- Zeitzone: **Europe/Berlin**
- Standard-Arbeitsrepo für Code-Arbeit: `/Users/frank/clawi/proggs`
- Zugehöriges GitHub-Repo: `https://github.com/Pepsi1978/proggs`

## OpenClaw-Wiederherstellung

### 1. Workspace-Dateien in `~/.openclaw/workspace` übernehmen

Mindestens diese Dateien aus `clawi-setup/` in den OpenClaw-Workspace synchronisieren:

- `SOUL.md`
- `DIREKTIVEN.md`
- `AGENTS.md`
- `ENVIRONMENT-FIXES.md`
- `IDENTITY.md`
- `MEMORY.md`
- `memory/`
- optional zusätzlich `README.md`, `HEARTBEAT.md`, Hook-Dateien und Sync-Skripte

### 2. `lossless-claw` installieren

Empfohlene Installation:

```bash
openclaw plugins install @martian-engineering/lossless-claw
```

Die gewünschte installierte Fassung war zuletzt:

- Paket: `@martian-engineering/lossless-claw`
- Version: `0.5.2`

Wenn möglich, die Version explizit pinnen:

```bash
openclaw plugins install @martian-engineering/lossless-claw@0.5.2
```

### 3. OpenClaw-Konfiguration prüfen

`lossless-claw` soll in der OpenClaw-Konfiguration aktiv und explizit gesetzt sein.
Der gewünschte Zustand in `~/.openclaw/openclaw.json` ist sinngemäß:

```json
{
  "plugins": {
    "entries": {
      "lossless-claw": {
        "enabled": true,
        "config": {
          "freshTailCount": 48,
          "contextThreshold": 0.75,
          "incrementalMaxDepth": -1,
          "summaryModel": "openai-codex/gpt-5.4",
          "expansionModel": "openai-codex/gpt-5.4"
        }
      }
    },
    "installs": {
      "lossless-claw": {
        "spec": "@martian-engineering/lossless-claw@0.5.2"
      }
    },
    "allow": ["lossless-claw"],
    "slots": {
      "contextEngine": "lossless-claw"
    }
  }
}
```

### 4. Erwartetes Verhalten von `lossless-claw`

- Gesprächskontext wird lokal in `~/.openclaw/lcm.db` gespeichert
- Konversationen können erhalten bleiben, auch wenn der unmittelbare Chat-Kontext kompakter wird
- Summaries entstehen erst dann, wenn genug Verlauf für Kompaktierung vorhanden ist
- Das Plugin ist **ergänzendes Gedächtnis**, aber **kein Ersatz** für `SOUL.md`, `DIREKTIVEN.md`, `AGENTS.md` oder `MEMORY.md`

## Verhältnis zu den Direktiven

`lossless-claw` muss mit den drei obersten Direktiven harmonieren:

1. **Superintelligenz**
   - mehr rekonstruierbarer Kontext
   - weniger unnötiger Informationsverlust
   - bessere Kontinuität über Sessions hinweg

2. **Selbstbeobachtung**
   - frühere Fehler, Entscheidungen und Beobachtungen bleiben besser nachvollziehbar
   - Erkenntnisse können leichter in Regeln und Fixes überführt werden

3. **Resilient Bugfixing**
   - Root-Cause-Ketten gehen seltener verloren
   - technische Verbesserungen bleiben langfristig auffindbar
   - Wiederholungsfehler werden unwahrscheinlicher

## Wichtige Abgrenzung

Das Plugin verweist technisch auf sein Ursprungsprojekt `Martian-Engineering/lossless-claw`, weil der Code von dort kommt.
Das bedeutet **nicht**, dass es auf fremde Nutzerdaten oder fremde Repos "zeigt".
Es läuft lokal für Clawi mit:

- lokaler OpenClaw-Instanz
- lokalem Workspace
- lokaler LCM-Datenbank
- Clawis Direktiven, Regeln und Gedächtnis

## Wiederherstellungs-Checkliste

Nach einer Neuinstallation prüfen:

- [ ] `SOUL.md` vorhanden
- [ ] `DIREKTIVEN.md` vorhanden
- [ ] `AGENTS.md` vorhanden
- [ ] `ENVIRONMENT-FIXES.md` vorhanden
- [ ] `IDENTITY.md` vorhanden
- [ ] `MEMORY.md` vorhanden
- [ ] `memory/` synchronisiert
- [ ] `lossless-claw` installiert
- [ ] `plugins.allow` enthält `lossless-claw`
- [ ] `plugins.slots.contextEngine` ist `lossless-claw`
- [ ] OpenClaw startet ohne Pluginfehler
- [ ] `~/.openclaw/lcm.db` wird angelegt oder genutzt

## Langfristige Regel

Wenn Clawi-relevante Grundlagen geändert werden, sollen die wiederherstellungsrelevanten Informationen in `clawi-setup/` mit Commit und Push gesichert werden.

So bleibt Clawi auch nach Totalverlust des lokalen Setups rekonstruktierbar.
