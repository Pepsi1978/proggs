# Codex-Statuszeile: unterstützte Anzeige mit aktivem Limit verwechselt

Festgehalten am 04.09.2026, 21:09 Uhr. CLI 0.153.2.

Falle: Aus dem vorhandenen Schlüssel `five-hour-limit` wurde ohne Prüfung des Kontos zugesagt, dass ein Fünf-Stunden-Kontingent angezeigt werde.

Ursache: Technische Unterstützung einer Anzeige wurde mit einer aktuell geltenden Kontoregel gleichgesetzt.

Vorgehen: Aktuelle offizielle Dokumentation und `/status` beziehungsweise das Nutzungsdashboard getrennt prüfen. Kontospezifische Geltung ohne Kontodaten offenlassen. Vorübergehende Aufhebung nicht als dauerhafte Abschaffung ausgeben.

Belege und Stand: [Recherche zu Nutzungslimits](../../best-practices/agents/codex-nutzungslimits.md).

Kein Programmfehler nachgewiesen; keine Codeänderung erforderlich.
