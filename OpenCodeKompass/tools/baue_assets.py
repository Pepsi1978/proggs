"""Erzeugt die deutsche Offline-Referenz von OpenCode Kompass aus offiziellen Quellen.

Die Namen und die englischen Beschreibungen der Slash-Befehle werden deterministisch aus
`tui.mdx` gelesen — derselbe Abschnitt und dieselben Regeln wie im Aktualisieren-Knopf der App
(DokuParser.kt). Die deutschen Erklärungen kommen aus tools/erklaerungen.txt und werden nie
erfunden: Fehlt für einen Befehl der Text, bricht der Lauf ab, statt eine halbe Auslieferung
zu schreiben.

Aufruf:  python tools/baue_assets.py
         python tools/baue_assets.py --quellen <Ordner mit tui.mdx und config.mdx>
"""
import argparse
import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASIS = "https://raw.githubusercontent.com/anomalyco/opencode/dev/packages/web/src/content/docs"
URL_TUI = f"{BASIS}/tui.mdx"
URL_CONFIG = f"{BASIS}/config.mdx"
URL_RELEASES = "https://api.github.com/repos/anomalyco/opencode/releases?per_page=10"

# Unter dieser Zahl gelesener Befehle gilt die Doku als umgebaut und der Lauf bricht ab.
MINDEST_SLASH = 10

UEBERSCHRIFT = re.compile(r"^###\s+([a-z][a-z0-9-]*)\s*$")
# MULTILINE: Die Alias-Angabe steht mitten im Abschnitt, nicht an dessen Ende.
ALIAS = re.compile(r"_Alias(?:es)?_:\s*(.+)$", re.MULTILINE)
SLASH = re.compile(r"`(/[a-z][a-z0-9-]*)`")


def hole(url, timeout=60):
    anfrage = urllib.request.Request(url, headers={"User-Agent": "OpenCodeKompass-Generator"})
    with urllib.request.urlopen(anfrage, timeout=timeout) as antwort:
        return antwort.read().decode("utf-8")


def bereinige_beschreibung(roh):
    """Macht aus dem Abschnittstext den Satz, der als offizielle Beschreibung taugt.

    Genommen wird NUR der erste Absatz. Danach folgen Hinweiskaesten, die Tastenkuerzel-Zeile
    und Nachbemerkungen — sie beschreiben den Befehl nicht mehr und blaehten den Eintrag auf.
    """
    absaetze = [a.strip() for a in roh.split("\n\n")]
    erster = ""
    for absatz in absaetze:
        zeilen = [z.strip() for z in absatz.splitlines()
                  if z.strip() and not z.strip().startswith((":::", "---", "**Keybind:**"))]
        if not zeilen:
            continue
        erster = " ".join(zeilen)
        break
    text = ALIAS.sub("", erster)
    text = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", text)
    text = re.sub(r"`([^`]*)`", r"\1", text)
    text = text.replace("**", "")
    # Der Linktext bleibt sonst als nackter Satz "Learn more." stehen und sagt nichts.
    text = re.sub(r"\s*\bLearn more\b\s*\.?", " ", text)
    text = re.sub(r"\s+", " ", text).strip().rstrip(". ")
    return f"{text}." if text else ""


def lies_slash_befehle(markdown):
    """Liest ausschliesslich den Abschnitt 'Commands' — genau wie DokuParser.kt in der App."""
    abschnitt = markdown.split("\n## Commands", 1)
    if len(abschnitt) < 2:
        raise SystemExit("Der Abschnitt 'Commands' fehlt in tui.mdx. Bestand nicht überschrieben.")
    abschnitt = abschnitt[1].split("\n## ", 1)[0]

    ergebnis = {}
    name = ""
    text = []
    im_codeblock = False

    def uebernimm():
        nonlocal name, text
        if not name:
            return
        roh = "\n".join(text).strip()
        aliasse = []
        treffer = ALIAS.search(roh)
        if treffer:
            aliasse = SLASH.findall(treffer.group(1))
        beschreibung = bereinige_beschreibung(roh)
        if beschreibung:
            ergebnis.setdefault(name, (name, beschreibung, "Eingebaut"))
            for alias in aliasse:
                ergebnis.setdefault(alias, (name, beschreibung, f"Alias von {name}"))
        name = ""
        text = []

    for zeile in abschnitt.splitlines():
        if zeile.lstrip().startswith("```"):
            im_codeblock = not im_codeblock
            continue
        if im_codeblock:
            continue
        treffer = UEBERSCHRIFT.match(zeile.strip())
        if treffer:
            uebernimm()
            name = "/" + treffer.group(1)
            continue
        if name:
            text.append(zeile)
    uebernimm()
    return ergebnis


def eintrag(name, kategorie, kurz, erklaerung, englisch="", art="Artikel", seit=""):
    return dict(name=name, kategorie=kategorie, art=art, kurz=kurz, englisch=englisch,
                erklaerung=erklaerung, seit=seit, seitBeleg="",
                sortierName=name.removeprefix("/").lower(), entfernt=False,
                entferntIn="", ersatz="")


CONFIG_TEXTE = [
    ("Konfiguration in OpenCode", "Orientierung",
     "OpenCode wird über opencode.json eingestellt, nicht über einen Slash-Befehl.",
     "OpenCode kennt keinen eingebauten Befehl /config. Die Einstellungen stehen in einer JSON-Datei: global unter ~/.config/opencode/opencode.json, im Projekt unter opencode.json beziehungsweise .opencode/opencode.json.\n\nDie Projektdatei überschreibt die globale. Über die Umgebungsvariable OPENCODE_CONFIG lässt sich ein eigener Pfad angeben. Das Schema steht unter https://opencode.ai/config.json und wird von den gängigen Editoren zur Vervollständigung genutzt — trag es als $schema in die erste Zeile ein.\n\nDie Oberflächen-Einstellungen sind in eine eigene Datei tui.json ausgelagert. Die alten Schlüssel theme, keybinds und tui in opencode.json gelten als veraltet und werden, wo möglich, automatisch übernommen."),
    ("model", "Modelle",
     "Bestimmt das Modell, mit dem gearbeitet wird.",
     "Unter model steht das Modell in der Form anbieter/modell, zum Beispiel anthropic/claude-sonnet-4-5. Das ist der dauerhafte Standard; für die laufende Sitzung wechselst du stattdessen mit /models.\n\nDamit ein Modell zur Auswahl steht, muss sein Anbieter verbunden sein — dafür gibt es /connect."),
    ("small_model", "Modelle",
     "Ein günstigeres Modell für Nebenaufgaben.",
     "Manche Arbeiten sind Beiwerk, etwa das Benennen einer Sitzung. Für sie lässt sich unter small_model ein kleineres, günstigeres Modell eintragen.\n\nOhne eigene Angabe sucht OpenCode selbst nach einem günstigeren Modell desselben Anbieters und fällt sonst auf das Hauptmodell zurück."),
    ("provider", "Modelle",
     "Feineinstellungen je Anbieter, etwa Zeitgrenzen.",
     "Unter provider lassen sich je Anbieter Einstellungen setzen: timeout begrenzt die gesamte Anfrage in Millisekunden (Standard 300000), headerTimeout nur das Warten auf den Antwortkopf, chunkTimeout den Abstand zwischen zwei Teilstücken einer laufenden Antwort.\n\nJede dieser Grenzen lässt sich mit false abschalten. setCacheKey sorgt dafür, dass für den Anbieter immer ein Zwischenspeicher-Schlüssel gesetzt wird. Auch eigene, lokal laufende Modelle werden hier eingetragen."),
    ("tools", "Werkzeuge",
     "Schaltet einzelne Werkzeuge des Modells ab.",
     "Unter tools lässt sich jedes Werkzeug einzeln auf false setzen, etwa \"write\": false oder \"bash\": false. Damit kann das Modell zwar noch lesen und antworten, aber nichts mehr schreiben oder ausführen.\n\nDas ist der ruhigste Weg, ein Projekt nur erklären zu lassen. Für feinere Abstufungen — fragen statt verbieten — gibt es permissions."),
    ("permissions", "Berechtigungen",
     "Legt fest, was ohne Rückfrage geschehen darf.",
     "Während tools ein Werkzeug ganz abschaltet, regeln die permissions, ob eine Aktion ohne Nachfrage ausgeführt, vorher bestätigt oder abgelehnt wird.\n\nDas ist die passende Stufe für ein Projekt, in dem gearbeitet werden soll, aber nicht blind: Lesen ohne Rückfrage, Schreiben und Ausführen erst nach Bestätigung."),
    ("shell", "Arbeitsumgebung",
     "Bestimmt die Kommandozeile für Befehle.",
     "Unter shell steht, welche Kommandozeile OpenCode für das eingebaute Terminal und für die Befehle des Modells benutzt, etwa pwsh, /bin/zsh oder ein vollständiger Pfad.\n\nOhne eigene Angabe wählt OpenCode eine zum Betriebssystem passende Vorgabe — unter Windows pwsh oder cmd.exe, unter macOS und Linux zsh oder bash."),
    ("server", "Server",
     "Einstellungen für opencode serve und opencode web.",
     "Unter server stehen port und hostname des eingebauten Servers. Mit mdns meldet er sich im lokalen Netz an, sodass andere Geräte ihn finden; mdnsDomain vergibt dafür einen eigenen Namen (Standard opencode.local), was beim Betrieb mehrerer Instanzen im selben Netz hilft.\n\nUnter cors stehen zusätzliche Herkunftsadressen, die eine Browser-Oberfläche ansprechen darf — jeweils vollständig mit Schema, Rechnername und gegebenenfalls Port."),
    ("agents", "Agenten",
     "Eigene Agenten mit eigenem Auftrag und Modell.",
     "Unter agent lassen sich eigene Agenten festlegen: jeweils mit eigenem Auftragstext, eigenem Modell und eigenen Werkzeugen. Damit bekommt eine wiederkehrende Rolle — etwa das Durchsehen von Änderungen — feste Regeln.\n\nWelcher Agent voreingestellt ist, steht unter default_agent. Wie tief Agenten weitere Agenten starten dürfen, begrenzt subagent depth."),
    ("command", "Eigene Befehle",
     "Legt eigene Slash-Befehle an.",
     "Neben den eingebauten Befehlen lassen sich eigene anlegen: entweder unter command in der Konfiguration oder als Markdown-Datei im Ordner commands/, etwa .opencode/commands/test.md.\n\nDer Name der Datei wird zum Befehl, der Inhalt zur Vorlage, die an das Modell geht. Im Kopf der Datei stehen description, agent und model. Diese App führt nur die eingebauten Befehle — deine eigenen kennt nur deine Installation."),
    ("keybinds", "Bedienung",
     "Belegt die Tastenkürzel neu.",
     "Die Kürzel der Oberfläche laufen über eine Führungstaste, voreingestellt Strg+X: erst Strg+X, dann der Buchstabe des Befehls. Unter keybinds lässt sich diese Belegung ändern.\n\nDer Schlüssel gehört inzwischen in die eigene Datei tui.json; in opencode.json gilt er als veraltet."),
    ("mcp", "Erweiterungen",
     "Bindet externe Werkzeugserver ein.",
     "Unter mcp werden Server nach dem Model-Context-Protocol eingetragen — externe Dienste, die dem Modell zusätzliche Werkzeuge bereitstellen, etwa den Zugriff auf ein Ticketsystem oder eine Datenbank.\n\nJeder Server erweitert, was das Modell anfassen kann. Was ein Server darf, gehört deshalb genauso bedacht wie die eigenen permissions."),
    ("plugins", "Erweiterungen",
     "Lädt Erweiterungen von OpenCode selbst.",
     "Unter plugin stehen Erweiterungen, die in OpenCode selbst eingreifen, statt nur Werkzeuge anzubieten.\n\nSie laufen mit denselben Rechten wie OpenCode. Nimm nur Erweiterungen auf, deren Herkunft du kennst."),
    ("instructions", "Projektregeln",
     "Zusätzliche Regeldateien neben AGENTS.md.",
     "Unter instructions lassen sich weitere Dateien angeben, deren Inhalt bei jeder Anfrage mitgegeben wird — zusätzlich zu AGENTS.md.\n\nDas ist der Weg, geteilte Regeln aus einem anderen Ordner einzubinden, statt sie in jedes Projekt zu kopieren. Jede eingebundene Datei verbraucht Kontext, also kurz halten."),
    ("theme", "Darstellung",
     "Das Farbschema der Oberfläche.",
     "Das Farbschema wird über /themes umgeschaltet und dauerhaft in der Konfiguration festgelegt. Eigene Schemata sind möglich.\n\nDer Schlüssel gehört inzwischen in die eigene Datei tui.json; in opencode.json gilt er als veraltet."),
    ("tui.json", "Darstellung",
     "Die eigene Datei für alles, was die Oberfläche betrifft.",
     "Die Einstellungen der Terminaloberfläche stehen in tui.json beziehungsweise tui.jsonc, mit dem Schema https://opencode.ai/tui.json. Dazu gehören scroll_speed und scroll_acceleration, diff_style für die Anzeige von Änderungen, cursor mit style und blinking sowie mouse für die Mausbedienung.\n\nSteht cursor.style auf \"default\", gilt der Cursor des Terminals und cursor.blinking bleibt wirkungslos. Über die Umgebungsvariable OPENCODE_TUI_CONFIG lässt sich ein eigener Pfad angeben."),
    ("attention", "Darstellung",
     "Meldet sich, wenn OpenCode auf dich wartet.",
     "Unter attention in tui.json wird eingestellt, ob OpenCode auf sich aufmerksam macht, sobald es fertig ist oder eine Bestätigung braucht: enabled schaltet es ein, notifications gibt eine Meldung des Betriebssystems aus, sound spielt einen Ton, volume regelt dessen Lautstärke.\n\nBei langen Läufen erspart das die Warterei vor dem Fenster."),
    ("compaction", "Kontext",
     "Wann der Verlauf selbsttätig zusammengefasst wird.",
     "Unter compaction steht, ab wann OpenCode den Verlauf von sich aus zusammenfasst, statt an die Grenze des Kontextfensters zu laufen.\n\nVon Hand geht dasselbe jederzeit mit /compact. Beides kostet Genauigkeit: Was in der Zusammenfassung fehlt, ist für das Modell weg."),
    ("share", "Teilen",
     "Ob und wie Sitzungen geteilt werden dürfen.",
     "Unter share steht, ob das Teilen einer Sitzung erlaubt ist und ob es von allein geschieht. In einem Arbeitsumfeld mit vertraulichem Quelltext gehört es abgeschaltet.\n\nEine einzelne Sitzung teilst du sonst mit /share und nimmst es mit /unshare zurück."),
    ("snapshot", "Rückgängig",
     "Die Sicherungspunkte hinter /undo und /redo.",
     "Damit /undo und /redo auch Dateiänderungen zurücknehmen können, legt OpenCode Sicherungspunkte über Git an. Unter snapshot lässt sich dieses Verhalten steuern.\n\nDas ist der Grund, warum beide Befehle ein Git-Archiv voraussetzen. In einem Ordner ohne Git bleibt nur das Gespräch zurückzunehmen, nicht die Dateien."),
    ("autoupdate", "Wartung",
     "Ob OpenCode sich selbst aktualisiert.",
     "Unter autoupdate steht, ob OpenCode neue Fassungen von allein einspielt. Das Projekt veröffentlicht sehr häufig.\n\nAbschalten ist sinnvoll, wo die Fassung festgelegt sein soll — etwa auf einem Server oder in einem Team, das mit demselben Stand arbeitet."),
    ("formatters", "Werkzeuge",
     "Die Formatierer für geänderte Dateien.",
     "Unter formatter steht, womit geänderte Dateien nachformatiert werden. OpenCode erkennt die gängigen Werkzeuge selbst; eigene Einträge überschreiben oder ergänzen das.\n\nSo bleibt der Stil im Projekt einheitlich, egal ob ein Mensch oder das Modell geschrieben hat."),
    ("lsp", "Werkzeuge",
     "Die Sprachserver für Fehler und Verweise.",
     "Unter lsp stehen die Sprachserver, über die OpenCode Fehler, Typen und Verweise im Quelltext liest. Die gängigen werden selbst erkannt, weitere lassen sich eintragen.\n\nErst damit merkt OpenCode einen Übersetzungsfehler, ohne das Projekt vollständig zu bauen."),
    ("watcher", "Werkzeuge",
     "Die Überwachung von Dateiänderungen.",
     "Unter watcher steht, ob und wie OpenCode Änderungen an den Projektdateien mitliest — auch solche, die ausserhalb entstanden sind.\n\nIn sehr grossen Projekten kann sich das Abschalten lohnen, wenn die Überwachung spürbar Leistung kostet."),
    ("disabled_providers", "Modelle",
     "Blendet Anbieter aus der Auswahl aus.",
     "Unter disabled_provider stehen Anbieter, die nicht angeboten werden sollen, obwohl Zugangsdaten in der Umgebung liegen. Unter enabled_provider lässt sich umgekehrt auf eine Auswahl begrenzen.\n\nDas hält die Liste unter /models kurz und verhindert, dass versehentlich über einen unerwünschten Anbieter abgerechnet wird."),
    ("experimental.policies", "Berechtigungen",
     "Erlaubt oder verbietet Aktionen an Ressourcen.",
     "Unter experimental.policies lassen sich Regeln aufstellen, die eine Aktion an einer Ressource erlauben oder verbieten — je Eintrag mit effect, action und resource. Zurzeit lässt sich damit steuern, welche Anbieter benutzt werden dürfen.\n\nDer Bereich heisst ausdrücklich experimental: Sein Aufbau kann sich zwischen zwei Fassungen ändern."),
    ("Umgebungsvariablen", "Arbeitsumgebung",
     "Werte aus der Umgebung in der Konfiguration verwenden.",
     "In der Konfiguration lassen sich Werte aus Umgebungsvariablen und aus Dateien einsetzen, statt sie fest einzutragen.\n\nDas ist der richtige Weg für Schlüssel: Sie bleiben aus der Datei heraus, die im Projektarchiv landet. OPENCODE_CONFIG zeigt auf eine eigene Konfigurationsdatei, OPENCODE_TUI_CONFIG auf eine eigene Oberflächendatei."),
]

PRAXIS_TEXTE = [
    ("Anbieter frei wählen", "OpenCode ist an keinen Hersteller gebunden.",
     "OpenCode arbeitet mit Anbietern verschiedener Hersteller. Mit /connect hinterlegst du deren Schlüssel, mit /models wählst du für die laufende Sitzung ein Modell.\n\nDas ist der grösste Unterschied zu den herstellereigenen Kommandozeilen: Du kannst für eine knifflige Aufgabe ein starkes Modell nehmen und für Nebenarbeiten ein günstiges. Unter model und small_model wird beides dauerhaft festgelegt."),
    ("Aufgaben verständlich formulieren", "Beschreibe Ergebnis und Grenzen der Aufgabe.",
     "Nenne das konkrete Ergebnis, das du brauchst, und welche Dateien oder Funktionen betroffen sind. Ein Auftrag wie „Erkläre die Anmeldung in dieser Datei\" ist eindeutiger als „Schau mal rein\".\n\nMit @ verweist du direkt auf eine Datei; OpenCode sucht sie im Arbeitsverzeichnis und nimmt ihren Inhalt ins Gespräch auf. Das ist genauer als eine Beschreibung aus dem Gedächtnis."),
    ("Dateien und Befehle einbeziehen", "@ holt eine Datei herein, ! führt einen Befehl aus.",
     "Ein @ in der Nachricht startet eine unscharfe Dateisuche im Arbeitsverzeichnis; die gewählte Datei wird mit ihrem Inhalt ins Gespräch aufgenommen. Eingerichtete Verweise erscheinen dort ebenfalls: @kuerzel nimmt den ganzen Bereich auf, @kuerzel/ vervollständigt die Dateien darin.\n\nBeginnt eine Nachricht mit !, wird der Rest als Befehl in der Kommandozeile ausgeführt und die Ausgabe landet als Werkzeugergebnis im Gespräch. So belegst du einen Fehler mit der echten Ausgabe statt mit einer Beschreibung."),
    ("Rückgängig machen statt neu anfangen", "/undo nimmt auch die Dateiänderungen zurück.",
     "Wenn eine Antwort in die falsche Richtung ging, nimmt /undo die letzte Nachricht, alle Antworten darauf und die entstandenen Dateiänderungen zurück. /redo stellt sie wieder her.\n\nBeides setzt ein Git-Archiv voraus — ohne Git kann OpenCode die Dateien nicht zurücksetzen. Ein Projekt unter Git zu stellen, bevor das Modell daran arbeitet, ist deshalb die billigste Versicherung, die es gibt."),
    ("Kontext bewusst verwalten", "Für ein neues Thema /new statt /compact.",
     "Ein langes Gespräch füllt das Kontextfenster. /compact fasst den Verlauf zusammen und schafft Platz, verliert dabei aber Einzelheiten.\n\nFür ein wirklich anderes Thema ist /new die bessere Wahl: ein leeres Gespräch ohne Altlasten. Das alte bleibt erhalten und ist über /sessions erreichbar."),
    ("Projektregeln in AGENTS.md festhalten", "Was immer gilt, gehört in die Datei, nicht in die Nachricht.",
     "Mit /init legst du AGENTS.md an oder schreibst sie fort. Dort steht, was dauerhaft gilt: wo die wichtigen Dateien liegen, wie gebaut und geprüft wird, welche Besonderheiten es gibt.\n\nSo musst du es nicht in jeder Sitzung wiederholen. Zugangsdaten gehören nicht hinein — die Datei liegt im Projektarchiv."),
    ("Berechtigungen zur Aufgabe passend wählen", "tools schaltet ab, permissions fragt nach.",
     "Für eine reine Erklärung reicht lesender Zugriff: Unter tools lassen sich write und bash auf false setzen.\n\nSoll gearbeitet werden, aber nicht blind, sind die permissions die feinere Stufe — sie können eine Aktion vor dem Ausführen bestätigen lassen, statt sie ganz zu verbieten."),
    ("Ergebnisse nachvollziehen", "Sieh dir an, was tatsächlich gelaufen ist.",
     "Mit /details blendest du die vollständigen Werkzeugaufrufe ein und siehst, welche Dateien gelesen und welche Befehle wirklich ausgeführt wurden.\n\nLass dir ausserdem sagen, welche Prüfungen und Builds gelaufen sind. Eine überzeugend formulierte Erklärung beweist allein noch nicht, dass ein Programm funktioniert."),
    ("Eigene Befehle anlegen", "Wiederkehrende Aufträge einmal aufschreiben.",
     "Für eine Aufgabe, die du immer wieder gleich stellst, legst du einen eigenen Befehl an: eine Markdown-Datei in commands/, etwa .opencode/commands/test.md, oder ein Eintrag unter command in der Konfiguration.\n\nDer Dateiname wird zum Befehl, der Inhalt zur Vorlage. Im Kopf stehen description, agent und model. Diese App dokumentiert die eingebauten Befehle — deine eigenen kennt nur deine Installation."),
    ("Vorsicht beim Teilen", "Im geteilten Verlauf steht mehr, als man denkt.",
     "/share macht ein Gespräch über eine Adresse abrufbar. Darin stehen aber nicht nur deine Fragen, sondern auch Dateiinhalte, Pfade und Befehlsausgaben.\n\nPrüfe den Verlauf, bevor du ihn weitergibst. Ist versehentlich ein Zugangsschlüssel darin gelandet, reicht /unshare nicht — dann gehört der Schlüssel gewechselt. Unter share lässt sich das Teilen für ein Projekt ganz abschalten."),
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--quellen", type=Path)
    args = parser.parse_args()

    def quelle(dateiname, url):
        if args.quellen:
            return (args.quellen / dateiname).read_text(encoding="utf-8")
        return hole(url)

    tui = quelle("tui.mdx", URL_TUI)
    version = ""
    if args.quellen and (args.quellen / "version.txt").exists():
        version = (args.quellen / "version.txt").read_text(encoding="utf-8").strip()
    else:
        releases = json.loads(hole(URL_RELEASES))
        for release in releases:
            if release.get("prerelease") or release.get("draft"):
                continue
            treffer = re.fullmatch(r"v([0-9]+\.[0-9]+\.[0-9]+)", release.get("tag_name", ""))
            if treffer:
                version = treffer.group(1)
                break
    if not version:
        raise SystemExit("Die OpenCode-Versionsnummer konnte nicht gelesen werden. Bestand nicht überschrieben.")

    gelesen = lies_slash_befehle(tui)
    if len(gelesen) < MINDEST_SLASH:
        raise SystemExit(f"Nur {len(gelesen)} Befehle gelesen, erwartet mindestens {MINDEST_SLASH}. "
                         "Bestand nicht überschrieben.")

    texte = {}
    for zeile in (ROOT / "tools/erklaerungen.txt").read_text(encoding="utf-8").splitlines():
        if not zeile or zeile.startswith("#"):
            continue
        name, kategorie, kurz, erklaerung = zeile.split("|", 3)
        texte[name] = (kategorie, kurz, erklaerung.replace(r"\n\n", "\n\n"))

    befehle = []
    for name, (haupt, englisch, art) in sorted(gelesen.items()):
        if haupt not in texte:
            raise SystemExit(f"Deutsche Erklärung fehlt: {haupt}. Bestand nicht überschrieben.")
        kategorie, kurz, erklaerung = texte[haupt]
        if haupt != name:
            kurz = f"Alternative Schreibweise für {haupt}."
            erklaerung = (f"{name} ist eine alternative Schreibweise für {haupt}. "
                          f"Beide rufen dieselbe Funktion auf.\n\n" + erklaerung)
        befehle.append(eintrag(name, kategorie, kurz, erklaerung, englisch, art))

    config = [eintrag(name, kategorie, kurz, erklaerung) for name, kategorie, kurz, erklaerung in CONFIG_TEXTE]
    praxis = [eintrag(name, "Arbeitsweise", kurz, erklaerung) for name, kurz, erklaerung in PRAXIS_TEXTE]

    assets = ROOT / "app/src/main/assets"
    assets.mkdir(parents=True, exist_ok=True)
    for dateiname, bereich, inhalt in [("slash_befehle.json", "slash", befehle),
                                       ("config_einstellungen.json", "config", config),
                                       ("best_practices.json", "praxis", praxis)]:
        nutzlast = dict(bereich=bereich, standVersion=version,
                        quellen=[URL_TUI, URL_CONFIG, URL_RELEASES], eintraege=inhalt)
        (assets / dateiname).write_text(json.dumps(nutzlast, ensure_ascii=False, indent=2) + "\n",
                                        encoding="utf-8")
    print(f"OpenCode {version}: {len(befehle)} Slash-Einträge, {len(config)} Config-Einträge, "
          f"{len(praxis)} Praxisartikel.")


if __name__ == "__main__":
    main()
