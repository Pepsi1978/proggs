"""Erzeugt die deutsche Offline-Referenz aus offiziellen Quellen und redaktionellen Texten.

Aufruf: python tools/baue_assets.py --quellen <Ordner mit commands.md und changelog.html>
Ohne --quellen werden ausschließlich die unten genannten OpenAI-Seiten abgerufen.
"""
import argparse
import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
URL = "https://learn.chatgpt.com/docs/developer-commands.md?surface=cli"
CHANGELOG = "https://learn.chatgpt.com/docs/changelog"
CONFIG = "https://learn.chatgpt.com/docs/config-file/config-basic"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--quellen", type=Path)
    args = parser.parse_args()

    def source(filename, url):
        if args.quellen:
            return (args.quellen / filename).read_text(encoding="utf-8")
        with urllib.request.urlopen(url, timeout=60) as response:
            return response.read().decode("utf-8")

    markdown = source("commands.md", URL)
    changelog = source("changelog.html", CHANGELOG)
    version = re.search(r"Codex CLI\s*(?:<[^>]+>\s*)*([0-9]+\.[0-9]+\.[0-9]+)", changelog).group(1)
    texte = {}
    for line in (ROOT / "tools/erklaerungen.txt").read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        name, category, short, explanation = line.split("|", 3)
        texte[name] = (category, short, explanation.replace(r"\n\n", "\n\n"))

    entries = {}
    table = markdown.split("## Built-in slash commands", 1)[1].split("\n## ", 1)[0]
    for line in table.splitlines():
        if not line.startswith("|"):
            continue
        cells = re.split(r"(?<!\\)\|", line.strip("|"))
        names = re.findall(r"`(/[a-z][a-z0-9-]*)`", cells[0])
        for index, name in enumerate(names):
            entries[name] = (names[0], cells[1].strip(), "Eingebaut" if index == 0 else f"Alias von {names[0]}")
    if "`/clean`" in markdown:
        entries["/clean"] = ("/stop", entries["/stop"][1], "Alias von /stop")
    supplemental = {
        "/cd": "Change the working directory in the TUI session.",
        "/pwd": "Show the working directory in the TUI session.",
        "/cwd": "Manage the working directory in the TUI session.",
        "/export": "Export the conversation as Markdown to the clipboard or a file.",
        "/recap": "Request a manual recap of the current conversation.",
    }
    for name, english in supplemental.items():
        if re.search(r"<code[^>]*>" + re.escape(name) + r"(?:\s|<)", changelog):
            entries.setdefault(name, (name, english, "Eingebaut"))

    def entry(name, category, short, explanation, english="", kind="Artikel", since=""):
        return dict(name=name, kategorie=category, art=kind, kurz=short,
                    englisch=english, erklaerung=explanation, seit=since,
                    sortierName=name.removeprefix("/").lower(), entfernt=False)

    commands = []
    for name, (canonical, english, kind) in sorted(entries.items()):
        if canonical not in texte:
            raise SystemExit(f"Deutsche Erklärung fehlt: {canonical}. Bestand nicht überschrieben.")
        category, short, explanation = texte[canonical]
        if canonical != name:
            short = f"Alternative Schreibweise für {canonical}."
            explanation = f"{name} ist eine alternative Schreibweise für {canonical}. Beide rufen dieselbe Funktion auf.\n\n" + explanation
        since = "0.149.0" if name in ("/cd", "/pwd", "/cwd") else ""
        commands.append(entry(name, category, short, explanation, english, kind, since))

    config = [entry("Konfiguration in Codex CLI", "Orientierung",
        "Codex nutzt config.toml und einzelne Slash-Befehle für Einstellungen.",
        "Ein allgemeiner Slash-Befehl /config ist in der offiziellen CLI-Referenz nicht aufgeführt. Deine persönlichen Vorgaben stehen in ~/.codex/config.toml. Für vertrauenswürdige Projekte gibt es zusätzlich .codex/config.toml im Projekt.\n\n"
        "Einige Einstellungen erreichst du direkt, etwa über /model, /permissions, /theme und /statusline. Mit /debug-config erkennst du, welche Konfigurationsebenen wirksam sind. Für einzelne Programmstarts gibt es außerdem -c beziehungsweise --config.\n\n"
        "Diese erste Fassung von Codex Kompass enthält den Slash-Befehlskatalog. Ein vollständiges Nachschlagewerk sämtlicher config.toml-Schlüssel ist noch nicht enthalten. Die Einstellungen dieser Android-App mit Stimmen, Anmeldung und Darstellung findest du weiterhin über das Einstellungsmenü.")]

    praxis_text = [
        ("Aufgaben verständlich formulieren", "Beschreibe Ergebnis und Grenzen der Aufgabe.", "Nenne das konkrete Ergebnis, das du brauchst, und welche Dateien oder Funktionen betroffen sind. Ein Auftrag wie „Erkläre die Anmeldung in dieser Datei“ ist eindeutiger als „Schau mal rein“.\n\nFüge wichtige Randbedingungen gleich hinzu, beispielsweise die gewünschte Sprache und die unterstützten Plattformen. Mit /mention kannst du die passende Datei einbeziehen."),
        ("Befehle und normale Fragen unterscheiden", "Slash-Befehle steuern die CLI; normale Texte beschreiben deine Aufgabe.", "Ein Slash-Befehl öffnet eine bestimmte Funktion der Codex CLI. Eine normale Nachricht beschreibt dagegen, was Codex erledigen soll. Schreibe /, um die gerade verfügbaren Befehle zu sehen.\n\nDie Befehlsliste hängt von Version, Betriebssystem und aktivierten Funktionen ab. Die hier dokumentierten CLI-Befehle sind nicht automatisch Befehle der ChatGPT-Weboberfläche."),
        ("Berechtigungen passend wählen", "Lass die erlaubten Aktionen zur Aufgabe passen.", "Für eine reine Erklärung kann ein lesender Zugriff ausreichen. Wenn Codex Dateien ändern soll, benötigt es passende Berechtigungen. Mit /permissions siehst du die angebotenen Profile.\n\nEin zusätzlicher Ordner und eine Erlaubnis zum Schreiben sind nicht dasselbe. Mit /status kannst du die aktive Arbeitsumgebung nachvollziehen."),
        ("Ergebnisse nachvollziehen", "Sieh dir Änderungen und Begründungen an.", "Mit /diff kannst du die tatsächlich geänderten Stellen im Projekt ansehen. /review bietet eine zusätzliche inhaltliche Beurteilung. Beide Funktionen ergänzen sich.\n\nLass dir außerdem sagen, welche Builds oder Prüfungen tatsächlich gelaufen sind. Eine überzeugende Erklärung beweist allein noch nicht, dass ein Programm funktioniert."),
        ("Gespräche wiederfinden", "Gib längeren Aufgaben aussagekräftige Namen.", "Mit /rename benennst du das aktuelle Gespräch so, dass du es später erkennen kannst. Mit /resume öffnest du gespeicherte Sitzungen wieder.\n\nFür erledigte Arbeit, deren Verlauf du behalten möchtest, eignet sich /archive. /delete löscht dagegen dauerhaft und ist keine bloße Aufräumfunktion."),
        ("Kontext bewusst verwalten", "Halte dauerhafte Anforderungen auch in Dateien fest.", "Lange Gespräche benötigen viel Kontext. /compact kann frühere Nachrichten zusammenfassen und damit Platz schaffen. Speichere präzise Anforderungen zusätzlich in einer Projektdatei.\n\nFür ein unabhängiges Thema eignet sich /new. Für eine alternative Richtung aus dem bisherigen Verlauf gibt es /fork. Das trennt Gespräche, aber nicht automatisch die bearbeiteten Dateien."),
        ("Längere Arbeit planen", "Trenne die Planung vom Beginn der Umsetzung.", "Mit /plan kannst du zuerst ein Vorgehen ausarbeiten lassen. Das ist hilfreich, wenn mehrere Teile eines Projekts zusammenhängen oder wichtige Entscheidungen offen sind.\n\nEin dauerhaftes Ziel kannst du anschließend über /goal verwalten. Formuliere, woran sich erkennen lässt, dass die Aufgabe erreicht ist. Sehr ausführliche Anforderungen gehören in eine referenzierte Datei."),
        ("Projektregeln festhalten", "Nutze AGENTS.md für die dauerhaft geltenden Hinweise.", "Mit /init kannst du einen Entwurf für AGENTS.md vorbereiten lassen. Überarbeite ihn anhand der tatsächlichen Projektstruktur und der benötigten Arbeitsabläufe.\n\nHalte die Regeln konkret: Wo liegen wichtige Dateien, wie wird gebaut und welche Besonderheiten müssen beachtet werden? Zugangsdaten gehören nicht in diese Anleitung."),
        ("Verfügbarkeit richtig einordnen", "Ein fehlender Slash-Befehl ist nicht immer ein Fehler.", "Einige Befehle benötigen eine bestimmte Plattform oder Funktion. /setup-default-sandbox erscheint beispielsweise nur in einer bestimmten Windows-Situation. /fast hängt vom Modellangebot ab.\n\nAndere Befehle sind während laufender Arbeit vorübergehend gesperrt. Vergleiche die lokale Codex-Version und die Hinweise beim Eintrag, bevor du von einem Fehler ausgehst."),
        ("Werkzeuge gezielt einbeziehen", "Wähle die passende App, den Skill oder die MCP-Verbindung.", "Mit /apps wählst du verbundene Apps aus. /skills zeigt aufgabenspezifische Anleitungen und /mcp die konfigurierten externen Werkzeuge. /plugins verwaltet zusätzliche Erweiterungen.\n\nBeschreibe nach der Auswahl weiterhin dein Ziel. Die bloße Auswahl einer Verbindung führt deine fachliche Aufgabe noch nicht aus."),
    ]
    praxis = [entry(name, "Arbeitsweise", short, explanation) for name, short, explanation in praxis_text]
    assets = ROOT / "app/src/main/assets"
    assets.mkdir(parents=True, exist_ok=True)
    for filename, area, content in [("slash_befehle.json", "slash", commands),
                                    ("config_einstellungen.json", "config", config),
                                    ("best_practices.json", "praxis", praxis)]:
        payload = dict(bereich=area, standVersion=version, quellen=[URL, CHANGELOG, CONFIG], eintraege=content)
        (assets / filename).write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Codex CLI {version}: {len(commands)} Slash-Einträge, {len(praxis)} Praxisartikel.")


if __name__ == "__main__":
    main()
