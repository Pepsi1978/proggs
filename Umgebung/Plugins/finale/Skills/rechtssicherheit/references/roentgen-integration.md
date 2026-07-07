# Roentgen-Integration — Schritt 1.5 Detail

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** In Schritt 1.5 des Skill-Ablaufs. Sie beschreibt wie der
> Rechtssicherheits-Skill mit dem `app-roentgen`-Skill zusammenarbeitet, um Token zu sparen
> und den UWG-Werbeaussagen-Check zu ermoeglichen.

## Ziel

Wenn der `app-roentgen`-Skill die App bereits durchleuchtet hat, MUSS der Rechtssicherheits-
Skill diese Ergebnisse zuerst lesen — er kennt dann alle Screens, Klick-Pfade, Paywall-Stufen,
Permissions-Mappings und Werbeaussagen-vs-Feature-Matrix **ohne erneuten Vollscan**. Das spart
Tokens, vermeidet Inkonsistenzen und macht den Werbeaussagen-Check nach UWG §5/§5a moeglich.

## 1.5.1 Pruefe auf vorhandene Roentgen-Outputs

Skript: `scripts/check-roentgen-output.sh [APP_DIR]`

Inline (falls Skript nicht verfuegbar):
```sh
# Standard-Suchorte fuer Roentgen-Outputs
ls -1 <APP_DIR>/app-roentgen-*.md 2>/dev/null
ls -1 <APP_DIR>/app-roentgen-export.json 2>/dev/null
ls -1 <APP_DIR>/../<APP_NAME>-app-roentgen-AUDIT-*.md 2>/dev/null
# Alternativ Workspace-Root
ls -1 <WORKSPACE_ROOT>/<APP_NAME>/app-roentgen-*.md 2>/dev/null
ls -1 <WORKSPACE_ROOT>/<APP_NAME>/app-roentgen-export.json 2>/dev/null
```

| Zustand | Aktion |
|---------|--------|
| **Roentgen-Output vorhanden** | Komplett einlesen. Schritt 4 (Vollscan) DARF entfallen, falls der Roentgen-Output das gleiche Verzeichnis und denselben Code-Stand abdeckt (per Datum/letztem Commit pruefen). Werbeaussagen-vs-Feature-Matrix uebernehmen. |
| **Roentgen-Output fehlt** | Dem Benutzer auf Deutsch melden: *"Es gibt noch keinen Roentgen-Output. Empfehlung: starte ZUERST den `app-roentgen`-Skill — der Rechtssicherheits-Skill wird dadurch wesentlich praeziser. Soll ich abbrechen und mit Roentgen beginnen?"* Wenn der Benutzer trotzdem fortfahren will: Schritt 4 (Vollscan) selbst ausfuehren. |
| **Roentgen-Output veraltet** (letzter Commit der App neuer als Roentgen-Output) | Hinweis ausgeben, Vollscan durchfuehren und Roentgen-Datum am Ende des Berichts markieren. |

## 1.5.2 Was aus dem Roentgen-Output uebernommen wird

| Roentgen-Block | Wird verwendet in |
|---|---|
| **Screens + Klick-Pfade** | Pflicht-Platzierungs-Pruefung (5e), Werbeaussagen-Check |
| **Paywall-Stufen** | Widerruf-Pruefung (5e), Pricing/Trial/Cancel-Hinweise, BGB §312k Pflicht-Button |
| **Permissions-Mapping** | 5b Manifest-Pruefung + Prominent Disclosure + Data Safety |
| **Werbeaussage-vs-Feature-Matrix** | Formulierungs-Check (UWG §5/§5a) — KEINE App-Werbeaussage darf eine Funktion versprechen, die der Roentgen-Scan nicht im Code findet |
| **Hidden Features / Tracking** | Code-vs-Text-vs-Play-Matrix, Privacy Policy |
| **Feature-Inventar** | Feature-Gates (Kinder/Health/AI/UGC/Ads/Abo/Standort/Barrierefreiheit) |

## 1.5.3 UWG-Werbeaussagen-Check (nur mit Roentgen-Output sinnvoll)

Wenn die Werbeaussage-vs-Feature-Matrix aus dem Roentgen-Output vorliegt:

- Fuer JEDE Werbeaussage (App-Beschreibung im Store, Marketing-Texte, Paywall-Slogans,
  Onboarding-Headlines) pruefen: gibt es im Roentgen-Output eine bestaetigte Funktion
  die diese Aussage stuetzt?
- Wenn nein: 🔴 BLOCKER nach UWG §5 (irrefuehrende geschaeftliche Handlung) bzw. §5a
  (Unterlassen wesentlicher Informationen) im Bericht.
- Wenn Aussage nur teilweise zutrifft: 🟠 HOCH mit Vorschlag zur Reformulierung.

## 1.5.4 Was NIEMALS passieren darf

- Roentgen-Output ignorieren wenn er existiert — das verschwendet Tokens und fuehrt zu
  inkonsistenten Befunden zwischen den beiden Skills
- Roentgen-Output als alleinige Wahrheit verwenden — er wird GEGEN Privacy Policy, Terms,
  Data Safety und Manifest abgeglichen, nicht ersetzt
- Werbeaussagen-Check ohne Roentgen-Output machen — dann fehlt die Feature-Basis; in diesem
  Fall den Werbeaussagen-Check als "nicht durchgefuehrt mangels Roentgen-Output" markieren,
  nicht raten

## Zusammenspiel mit anderen Skills

- **`app-roentgen` ist die ideale Vorstufe** — liefert das Feature-Inventar
- **`app-monetizer`** kann zusaetzlich genutzt werden um Paywall-Stufen detaillierter zu kennen
- **`string-extraktor`** falls Rechtstexte hardcodiert statt in strings.xml sind
