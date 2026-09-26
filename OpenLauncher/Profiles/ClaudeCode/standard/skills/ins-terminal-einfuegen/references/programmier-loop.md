# Zielorientierter Programmierloop

Gemeinsame Referenz für Windows und macOS. Laden, sobald eine Programmierbegleitung mit
Claude als Implementierer beauftragt ist (unter Windows ebenso mit OpenCode oder Codex CLI;
„Claude“ steht dann für die beauftragte CLI). Sie ergänzt, ersetzt aber keine Transport-,
Stopp- und Abschlussregeln der Plattformreferenzen. Kein perfekter Loop: Sie legt fest,
wann ein Schritt belegt ist und wann nicht.

## Zielvertrag

Codex hält den Zielvertrag `ziel.json` außerhalb von Repo und Memory im selben
nutzerprivaten Ordner wie die Steuerdatei und ersetzt ihn atomar. Felder:

- `ziel_rev`: Revision der Nutzervorgaben
- `ziel`: angestrebte Nutzerwirkung
- `nichtziele`: Grenzen
- `profil`: aktives Arbeitsprofil `schnell|normal|gründlich` gemäß der Referenz
  „Arbeitsprofile“ der Plattform; beim Anlegen mit dem dort abgeleiteten Startwert
  gespeichert. Fehlt es in einem älteren Vertrag, den Startwert dieser Referenz verwenden
- `kriterien`: priorisiert, je `id`, `text`, `status` (`offen|erfuellt|blockiert|zurueckgestellt`), `beleg`
- `annahmen`

`ziel_rev` steigt nur, wenn der Nutzer Ziel, Grenzen, Kriterien oder das Arbeitsprofil
verbindlich ändert.
Rundenkennungen (`R27`), interne Korrekturen und Fragen erhöhen sie nicht.
`arbeitsstand.json` behält seine fünf Schlüssel. `phase` und `offen` verweisen bei Bedarf
auf Kriterien-IDs. Die Kriterien selbst stehen nur im Zielvertrag.

Zielvertrag und Steuerdatei werden einzeln atomar ersetzt, nicht als gemeinsame
Transaktion. Ist ein Zielvertrag vereinbart, muss seine `ziel_rev` zur aktuellen Steuerung
passen. Weichen sie ab oder fehlt der Zielvertrag, vor Commit, Push und Deployment klären;
kein stiller Rückfall auf einen alten Stand. `processed_ziel_rev` belegt Einarbeitung,
nicht die Erfüllung der Kriterien. Nach einer Kontextverdichtung `ziel.json` einmal lesen,
bevor der nächste Umsetzungsauftrag Ziel, Kriterien oder Profil nennt; das Profil kommt
aus dem Zielvertrag, nicht aus der Erinnerung.

Kriterien nie absenken, damit Tests grün werden. Neue Features sind kein Fortschritt,
solange sie kein Kriterium erfüllen.

## Nutzerbeiträge einordnen

- **Frage:** beantworten, lokal aus Bericht, Dateien oder Wissen. Braucht die Antwort
  Claude-Kontext, am nächsten freien Prompt mitgeben, nicht unterbrechen.
- **Unverbindliche Idee** („vielleicht wäre … nett“): als Option mit Folgen kurz
  einordnen und vormerken. Kein Scope, keine `ziel_rev`, keine Steuerdatei.
- **Auftrag oder Korrektur:** `ziel_rev` erhöhen, Zielvertrag und Steuerdatei atomar ersetzen.
  Ein eindeutiger Profilwechsel gehört dazu: `profil` setzen, wirksam ab der nächsten
  sicheren Runde.
- **Stopp/Pause:** sofort nach den bestehenden Regeln, beim Abschluss zusätzlich `status=stopp`.

Nicht jede Frage mit einer Rückfrage beantworten. Nur wenn Mehrdeutigkeit die nächste
folgenreiche Handlung betrifft, kurz klären und unabhängige Arbeit fortsetzen.
Routineentscheidungen innerhalb des autorisierten Ziels trifft der Loop selbst; echte
Produkt- oder Scope-Konflikte gehen an den Nutzer. Nutzersteuerung hat Vorrang.

## Rollen als Funktionen

- **Nutzer:** setzt Ziele und Prioritäten.
- **Codex:** bildet Konsens, plant, nimmt risikobasiert ab.
- **Claude:** exploriert breit, implementiert, testet und schließt ab.
- **Research:** bei relevanter Wissenslücke über den bestehenden `research`-Skill und dessen Ablauf.
- **Fable:** berät gemäß den Auslösern in [Kontingent sparsam nutzen](kontingent-sparen.md). Kein Ersatzmodell.

Keine feste Agentenmannschaft. Keine Behauptung besonderer Modellstärken ohne Messung.

## Optionale Subagenten

Nur bei einer wirklich unabhängigen Parallelaufgabe oder einer nötigen unabhängigen Prüfung
und konkreter vorhandener Autorisierung. Nie automatisch, nur weil sie im Gespräch
erwähnt wurden. Codeintensive Arbeit bevorzugt auf Claude-Seite. Eine Codex-Zusatzprüfung
nur bei ungeklärtem begründetem Risiko mit echtem Mehrwert. Die Dateizahl allein ist
kein Grund dafür oder dagegen.

Kein Modell-, Effort- oder Moduswechsel und keine Agentic Workflows; nur gewöhnliche
Subagenten der bestehenden Sitzung und Konfiguration.

- **Übergabe:** Ziel, Kriterien, `ziel_rev`, Pfade mit Besitz (lesend oder schreibend),
  Grenzen, erwarteter Nachweis und Abbruchkriterium.
- **Rückgabe:** etwa 200–300 Wörter plus Belegpfade.
- **Schreibrecht:** ein Schreibender pro Datei. Kinder führen keine Git- oder
  Deploy-Mutationen aus. Der Elternagent prüft die Ergebnisse, führt sie zusammen und
  bleibt der einzige Git-Verantwortliche.
- **Steuerung:** Zieländerung oder Stopp an aktive Kinder weitergeben. Veraltete Ergebnisse
  nicht blind übernehmen.

Diese Grenzen sind Absprachen, keine technisch erzwungenen Sperren. Nicht als mechanische
Garantie beschreiben.

## Ablauf einer Runde

1. Zielvertrag, Steuerdatei und aktuellen Stand lesen.
2. Den Umfang nach dem aktiven Profil wählen: gründlich den kleinsten nutzbaren vertikalen
   Teil, normal einen zusammenhängenden Änderungssatz, schnell möglichst viele sinnvolle,
   vom Auftrag gedeckte Verbesserungen in einem Update.
3. Vorschlag mit begründetem Gegenargument oder Abwägung; Konsens oder entscheidender Test.
4. Claude setzt um und prüft selbst.
5. Claude meldet `Rn zwischenstand` am Review-Checkpoint.
6. Codex prüft risikobasiert und schickt Korrekturen oder einen gebundenen Abschlussauftrag.
7. Claude liefert aus und prüft die Auslieferung.
8. Kriterienbilanz ziehen, dann der nächste Teil.

## Review-Checkpoint und Bindung

Ein Checkpoint pro zusammenhängendem Codeblock, nicht pro Edit, auch bei `compact`.
Triviale Textübergaben und reine Fragen brauchen keinen. Er ist ein interner Halt zwischen
den Agenten, keine neue Nutzerfreigabe.

**Bericht am Checkpoint:** `ziel_rev`, effektiv angewandtes Profil samt Abweichung, Basis-HEAD, explizite eigene Pfade mit SHA-256
einschließlich neuer Dateien, Tests mit Ergebnis, kriterienbezogene Belege. Gelöschte
Dateien erhalten einen Löschmarker mit ihrem Blob-Hash im Basis-HEAD statt eines aktuellen
SHA-256.

**Freigabe:** Codex bindet sie an genau diesen Stand. Zulässig bleibt danach nur der
Versionsbump als begrenzte Metadatenänderung. Jede fachliche Änderung an den eigenen Pfaden
entwertet die Freigabe.

**Rebase:** nicht pauschal ignorieren. Bringt er fachliche Änderungen oder neue Abhängigkeits-
bzw. Basisrisiken für die betroffenen Teile, deren Abnahme und Tests wiederholen. Unveränderte
geprüfte Teile nicht vollständig neu lesen. Bei Konflikten nach Repo-Regel stoppen.

## Belege

| Beleg | belegt | belegt nicht |
|---|---|---|
| Build | Kompilierbarkeit | Funktion |
| Screenshot | eine Ansicht | Hintergrundverhalten oder Ablauf |
| Installation | Auslieferung | funktionierender Alarm oder Nutzerpfad |

Nutzerpfade und Funktionen gesondert prüfen, etwa per Test, Logcat/Dumpsys oder
Gerätebedienung. Automatisch messbare Bedienpfade prüft der Loop selbst. Persönliche
Zustimmung nur dort ausweisen, wo sie wirklich Teil der Abnahme ist, nicht pauschal für jede UX.

## Dissens, Stagnation, Sättigung

Nach zwei Diskussionen oder Fehlversuchen ohne neue Evidenz: konkreter Test, Fable oder
gezielte Recherche beziehungsweise Strategiewechsel. Kein blinder dritter Versuch. Blocker
offen benennen und unabhängige Teilziele weiterführen. Keine Modellmehrheit.

`saettigung` ist kein Zielerfolg, solange Musskriterien offen sind. Nur der Nutzer darf
sie zurückstellen. Einen fortlaufenden Verbesserungsauftrag bis zum Stopp respektieren;
keine kosmetischen Runden und kein Versprechen von Hintergrundarbeit.

## Abschluss

Das Gesamtergebnis gilt erst, wenn die vereinbarten Kriterien und die autorisierte
Abschlusskette belegt sind. Zurückgestellte Teile getrennt nennen.

Android:
- final versionierte APK bauen
- Commit, Rebase, Push
- nach Projektregel auf das WLAN-Gerät installieren
- Nutzerpfad prüfen

Ohne Nachweis nichts als vollendet melden.

## Startauftrag (Vorlage)

> Rn: Ziel und Kriterien laut `<ziel.json>` (ziel_rev N). Profil: [schnell|normal|gründlich] (gilt für diese Runde
> vor einem bestätigten Launcher-Startmodus; verbindliche Projektpflichten aus AGENTS.md und
> Projektregeln haben Vorrang). Prüfe `<steuerung.json>`
> vor Plan, Review-Übergabe, Commit, Push und Deployment. Umfang nach Profil: … Grenzen: …
> Halte am Review-Checkpoint als `Rn zwischenstand` mit Basis-HEAD, effektiv angewandtem
> Profil, Pfaden mit SHA-256, Tests und Kriterienbelegen; Abschluss erst nach gebundener Freigabe.
