# Zentrale Secrets in $HOME/SK/ (KRITISCH)

Diese Regel gilt AUTOMATISCH in JEDER Session und in ALLEN Projekten.
Alle API-Keys, Signing-Keys, Tokens und vertraulichen Zugangsdaten liegen
zentral in `$HOME/SK/` und NIEMALS im jeweiligen Git-Repo.

## Pfade

| Plattform | Pfad |
|-----------|------|
| Windows | `C:\Users\barwa\SK\` |
| macOS | `/Users/barwa/SK/` |
| Plattformuebergreifend | `$HOME/SK/` |

## Struktur

```text
$HOME/SK/
  README.md
  BestJournalAndroid/
  BestJournalFrank/
  VoiceOverlays/
  <neue Projekte>/
```

`$HOME/SK/README.md` ist die autoritative Dokumentation. Vor jeder
Secrets-bezogenen Aenderung MUSS diese Datei gelesen werden.

## Grundregel

NIEMALS Secrets ins Repo committen:

- API-Keys
- Signing-Keys
- Tokens
- `.env`
- `google-services.json`
- `google-services-*.json`
- `credentials.json`
- Keystores (`*.keystore`, `*.jks`, `*.p12`, `*.pem`)
- `keystore.properties`

Wenn solche Dateien im Repo, im Index oder als untracked Eintrag sichtbar werden,
MUSS Codex das sofort melden und darf sie nicht stagen.

## Keine .gitignore-Ausnahmen fuer Secrets

NIEMALS `.gitignore`-Ausnahmen erstellen oder belassen, die Secret-Dateien wieder
trackbar machen. Verboten sind insbesondere Muster wie:

```gitignore
!app/src/debug/google-services.json
!app/src/release/google-services.json
!*.keystore
!keystore.properties
!.env
```

Solche Ausnahmen umgehen die Schutzregel und waren die Ursache frueherer Leaks.
Wenn Codex sie sieht, muss Codex sie entfernen oder dem Benutzer sofort melden.

## Wie Projekte Secrets lesen

Wenn ein Projekt einen Key braucht, liest es aus `$HOME/SK/<projekt-name>/`.

| Stack | Pflichtmuster |
|-------|---------------|
| Android / Gradle | `syncSecretsFromSk`-Task kopiert beim Build aus SK an die erwarteten Pfade |
| C# / .NET | `Config.cs` sucht `.env`; SK ist erste Prioritaet im `searchPaths`-Array |
| Swift / macOS | `Config.swift` sucht `.env`; SK ist erste Prioritaet |
| Python / Node | `$HOME/SK/<projekt>/.env` ist erste Prioritaet |

Ins Repo gehoeren nur Template-Dateien mit redaktierten Werten, z.B.
`.env.example`, `google-services.json.template` oder `keystore.properties.template`.

## Neue Projekte

Wenn ein neues Projekt Keys braucht:

1. Unterordner `$HOME/SK/<projekt-name>/` erstellen.
2. Keys nur dort ablegen.
3. Build-Task oder Start-Code bauen, der aus SK liest.
4. Arbeitskopien im Projekt strikt ignorieren.
5. Nur `.example`- oder `.template`-Dateien mit `REDACTED`-Werten committen.

## Release-Keystores

Release-Keystores sind unwiederbringbar. Besonders kritisch:

```text
$HOME/SK/BestJournalAndroid/release.keystore
```

Diese Datei muss extra gesichert werden, z.B. auf externer Platte oder in
verschluesseltem Cloud-Speicher. Sie darf niemals ins Repo.

## Git-Pflicht vor Commit und Push

- NIEMALS `git add -A` oder `git add .` verwenden.
- Nur eigene Dateien namentlich stagen.
- Vor jedem `git push`: `git status --short`.
- Jede Status-Zeile bewusst klassifizieren:
  - gehoert zu meiner Aufgabe
  - gehoert zu fremder paralleler Session
  - ist lokaler Muell
- Secret-Dateien gehoeren nie zu einer commitbaren Aufgabe.

## Begruendung

GitHub Secret Scanning hat am 24.04.2026 einen Firebase-API-Key im Repo gefunden.
Ursache war eine `.gitignore`-Ausnahme plus ein zu grobkoerniges `git add`.

Die SK-Struktur ist Poka-Yoke Stufe 3 (Eliminierung): Dateien die nicht im
Projekt-Ordner liegen, koennen konzeptionell nicht committed werden.
