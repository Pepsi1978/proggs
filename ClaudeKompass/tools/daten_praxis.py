# -*- coding: utf-8 -*-
"""Best Practices: wie man mit der aktuellen Claude-Code-Fassung arbeitet.
Format: titel, kategorie, kurz, erklaerung."""

PRAXIS = [
 ("Anweisungen kurz halten — mehr Text macht schlechter, nicht besser", "Grundhaltung",
  "Jede Zeile in CLAUDE.md und in den Regeln kostet in JEDER Sitzung Platz und Genauigkeit.",
  "Es klingt widersinnig, ist aber gut belegt: Je mehr Anweisungen gleichzeitig gelten, desto "
  "schlechter werden ALLE befolgt. Das nennt man Kontext-Verfall.\n\n"
  "Der Grund ist einfach. Alles, was in `CLAUDE.md` und in den Regeldateien steht, wird bei "
  "jedem Start vollstaendig geladen. Steht dort viel, muss das Modell seine Aufmerksamkeit "
  "auf viele Vorgaben verteilen — und die einzelne verliert an Gewicht.\n\n"
  "Die Faustregel lautet deshalb: Der Kern gehoert in die Regel, die Einzelheiten in eine "
  "Datei, auf die verwiesen wird. Braucht Claude sie, liest es sie nach. Das ist verlustfrei — "
  "kein Wissen geht weg, es liegt nur woanders.\n\n"
  "Praktisch heisst das: Eine Regeldatei sollte nicht groesser als anderthalb Kilobyte sein. "
  "Was laenger wird, gehoert aufgeteilt.\n\n"
  "Womit du pruefst, ob es zu viel ist: `/context` zeigt, wodurch das Gedaechtnis belegt ist, "
  "und `/doctor` warnt, wenn die Beschreibungen deiner Skills nicht mehr alle hineinpassen."),

 ("Skills statt Commands anlegen", "Erweitern",
  "Seit Version 2.1.3 sind beide dasselbe — Skills koennen aber mehr.",
  "Frueher gab es zwei getrennte Dinge: Slash-Befehle als einzelne Textdatei und Skills als "
  "Ordner. Seit Version 2.1.3 sind sie zusammengefuehrt: Beides erzeugt denselben Befehl und "
  "verhaelt sich gleich.\n\n"
  "Trotzdem gibt es einen klaren Rat: Lege Neues als Skill an, also als Ordner mit einer "
  "`SKILL.md` darin. Ein Skill kann naemlich Beigaben mitbringen — Vorlagen, Skripte, "
  "Nachschlagedateien.\n\n"
  "Der zweite Vorteil ist der wichtigere: Ein Skill kann von Claude selbst erkannt und benutzt "
  "werden, ohne dass du ihn tippst. Ob das passiert, entscheidet allein die `description` im "
  "Kopf der Datei.\n\n"
  "Deshalb gilt fuer die Beschreibung: Die Ausloeser gehoeren nach vorne. Schreib hinein, "
  "womit ein Benutzer das Thema anspricht — genau diese Woerter sucht Claude.\n\n"
  "Bestehende Dateien in `commands/` laufen unveraendert weiter. Eine Umstellung wird nicht "
  "erzwungen und lohnt sich nur, wenn du die Zusatzmoeglichkeiten brauchst."),

 ("Ein Hook ist der einzige Weg, etwas wirklich zu erzwingen", "Automatisieren",
  "Anweisungen kann Claude vergessen. Ein Hook laeuft immer.",
  "Wenn du sagst „ab jetzt soll nach jeder Aenderung die Formatierung laufen“, ist eine Notiz "
  "in `CLAUDE.md` der falsche Ort. Sie wird meistens befolgt — aber eben nur meistens.\n\n"
  "Ein Hook dagegen ist ein Befehl, den das Programm selbst ausfuehrt, nicht Claude. Er laeuft, "
  "ob Claude daran denkt oder nicht. Genau das macht ihn zum richtigen Werkzeug fuer alles, "
  "was verlaesslich passieren muss.\n\n"
  "Es gibt Anknuepfungspunkte fuer viele Stellen: vor und nach einem Werkzeugaufruf, beim "
  "Start und Ende einer Sitzung, beim Abschicken einer Eingabe, beim Start eines Unteragenten. "
  "Seit Version 2.1.251 auch vor und nach einem Modellwechsel.\n\n"
  "Zwei Dinge, die man beim Schreiben eines Hooks beachten muss: Er laeuft als echter Befehl "
  "auf deinem Rechner und kann deshalb auch Schaden anrichten. Und sein Rueckgabewert "
  "entscheidet: Ein Hook, der einen Fehler meldet, kann Claude Code blockieren.\n\n"
  "Prueft am Anfang jedes Hooks, ob die erwarteten Angaben ueberhaupt da sind. Manche "
  "Ereignisse werden auch ohne die Daten ausgeloest, die man erwartet — ohne diese Pruefung "
  "arbeitet der Hook dann ins Leere oder stuerzt ab."),

 ("Nur `deny` sperrt — `allow` ist keine abschliessende Liste", "Sicherheit",
  "Was nicht in `allow` steht, ist nicht verboten, sondern loest nur eine Rueckfrage aus.",
  "Das ist das haeufigste Missverstaendnis bei den Berechtigungen und es hat Folgen.\n\n"
  "Viele legen eine `allow`-Liste an und glauben, damit sei alles andere gesperrt. Das stimmt "
  "nicht. Alles, was nicht darin steht, laeuft weiterhin — es wird nur vorher gefragt. Und "
  "wenn jemand aus Gewohnheit auf Ja tippt, ist die vermeintliche Sperre wirkungslos.\n\n"
  "Sperren geht ausschliesslich ueber `deny`. Was dort steht, wird abgelehnt, ohne Rueckfrage "
  "und ohne Ausweg.\n\n"
  "Der wichtigste Einsatz von `deny` sind Dateien mit Zugangsdaten. Ein Eintrag wie "
  "`Read(./.env)` verhindert, dass ihr Inhalt ueberhaupt ins Gespraech gelangt — und damit "
  "auch, dass er in einem Protokoll landet.\n\n"
  "Ein zweiter Punkt: Berechtigungsregeln aus verschiedenen Dateien werden ZUSAMMENGEFUEHRT, "
  "nicht ersetzt. Eine Sperre aus einer uebergeordneten Ebene bleibt also bestehen, auch wenn "
  "du im Projekt etwas erlaubst."),

 ("Die Sandbox ist der beste Schutz — und macht das Arbeiten angenehmer", "Sicherheit",
  "Abgeschottet plus automatisch erlaubt heisst: weniger Rueckfragen bei mehr Sicherheit.",
  "Die Sandbox sperrt ausgefuehrte Befehle vom Dateisystem und vom Netz ab. Selbst ein Befehl, "
  "der etwas Dummes tut, richtet ausserhalb dieses Bereichs keinen Schaden an.\n\n"
  "Der eigentliche Kniff ist die Kombination mit "
  "`sandbox.autoAllowBashIfSandboxed`. Weil ein abgeschotteter Befehl kaum Schaden anrichten "
  "kann, bringt die Rueckfrage wenig — sie faellt deshalb weg.\n\n"
  "Das Ergebnis ist die angenehmste Einstellung ueberhaupt: deutlich weniger Unterbrechungen "
  "UND mehr Sicherheit als vorher. Man muss sich also nicht zwischen beidem entscheiden.\n\n"
  "Eine Falle gibt es: Kann die Sandbox auf dem System nicht starten, laeuft der Befehl "
  "standardmaessig trotzdem — nur eben ungeschuetzt. Das merkt man leicht nicht. Wer sich auf "
  "den Schutz verlaesst, setzt `sandbox.failIfUnavailable` auf `true`.\n\n"
  "Verfuegbar ist die Sandbox unter macOS, Linux und unter Windows ueber WSL2."),

 ("Unteragenten halten das Hauptgespraech schlank", "Arbeitsweise",
  "Wer hundert Dateien durchsucht, soll nur das Ergebnis zurueckgeben, nicht die hundert Dateien.",
  "Manche Zwischenschritte erzeugen sehr viel Ausgabe, die niemanden interessiert. Ein Beispiel: "
  "„Finde alle Stellen, an denen diese Funktion benutzt wird.“ Um das zu beantworten, muessen "
  "viele Dateien angesehen werden — im Ergebnis zaehlt aber nur die Liste der Fundstellen.\n\n"
  "Gibt man so etwas an einen Unteragenten ab, macht er die Arbeit in seinem eigenen "
  "Gedaechtnis. Zurueck kommt nur das Ergebnis. Das Hauptgespraech bleibt dadurch schlank und "
  "damit schneller und genauer.\n\n"
  "Der schnelle Weg dafuer ist `/subtask`. Fuer wiederkehrende Rollen legst du eigene Agenten "
  "mit `/agents` an und gibst jedem seinen Werkzeugsatz.\n\n"
  "Eine wichtige Grenze: Ein Unteragent hat KEINE automatische Verdichtung. Er laeuft, bis sein "
  "Gedaechtnis voll ist, und stuerzt dann ab. Deshalb gilt: enger Auftrag, klare Grenzen, "
  "grosse Datenmengen in eine Datei schreiben statt in die Antwort.\n\n"
  "Und noch etwas aus der Praxis: Fuer dieselbe Aenderung an vielen Dateien ist ein kleines "
  "Skript oft zuverlaessiger als mehrere Agenten. Agenten vergessen einzelne Stellen, ein "
  "Skript nicht."),

 ("Plan vor Code bei allem, was groesser ist als ein Handgriff", "Arbeitsweise",
  "Ein Missverstaendnis faellt im Plan auf — im fertigen Code kostet es Stunden.",
  "Im Planungsmodus aendert Claude nichts. Es sieht sich das Problem an und legt dir ein "
  "Vorgehen vor. Erst wenn du zustimmst, wird gearbeitet.\n\n"
  "Der Nutzen ist am groessten, wo er am wenigsten aufwendig ist: am Anfang. Ein "
  "Missverstaendnis ueber das Ziel faellt beim Lesen des Plans in einer Minute auf. Steckt es "
  "erst im fertigen Code, kostet es einen halben Tag.\n\n"
  "Du kommst mit `/plan` hinein und kannst dein Anliegen gleich mitgeben.\n\n"
  "Achte im Plan besonders auf zwei Dinge: Welche Dateien werden angefasst — und stimmt das "
  "mit deiner Erwartung ueberein? Und: In welcher Reihenfolge? Eine falsche Reihenfolge ist "
  "die haeufigste Ursache dafuer, dass zwischendurch nichts mehr laeuft.\n\n"
  "Nach dem Annehmen lohnt sich oft ein frischer Start mit dem Plan. Das Zwischenmaterial aus "
  "dem Planen wird fuer die Umsetzung nicht mehr gebraucht."),

 ("Gedaechtnis bewusst fuehren statt volllaufen lassen", "Kontext",
  "`/context` zeigt, wodurch es voll ist. `/clear` beim Themenwechsel ist der wichtigste Handgriff.",
  "Das Gedaechtnis einer Sitzung ist begrenzt, und man sieht ihm nicht an, wodurch es belegt "
  "ist. `/context` macht das sichtbar: ein farbiges Raster, in dem jeder Anteil eine eigene "
  "Farbe hat.\n\n"
  "Ueberraschungen gibt es dabei fast immer. Eine grosse `CLAUDE.md`, ein Zusatzdienst mit "
  "vielen Werkzeugen oder eine einmal gelesene Riesendatei fressen oft mehr, als man denkt.\n\n"
  "Der wirksamste Handgriff ist der einfachste: Beim Wechsel zu einem anderen Thema `/clear` "
  "druecken. Sonst schleppt Claude den alten Zusammenhang mit und wird langsamer und ungenauer.\n\n"
  "Wird es innerhalb eines Themas eng, ist `/compact` richtig — und dabei ruhig mitgeben, "
  "worauf es dir ankommt: `/compact behalte alle Entscheidungen zur Datenbank`.\n\n"
  "Fuer eine Zwischenfrage, die nicht haengen bleiben soll, gibt es `/btw`. Die Antwort kommt, "
  "die Frage verschwindet."),

 ("Denktiefe zur Aufgabe waehlen", "Arbeitsweise",
  "`auto` ist der beste Ausgangspunkt; hohe Stufen lohnen sich nur bei schweren Aufgaben.",
  "Nachdenken kostet Zeit und Geld und bringt bessere Ergebnisse. Die Kunst besteht darin, es "
  "dort einzusetzen, wo es etwas bringt.\n\n"
  "Bei einer Umbenennung oder einem Tippfehler bringt tiefes Nachdenken nichts — die Aufgabe "
  "ist eindeutig. Bei einer Fehlersuche ohne klare Spur oder beim Entwurf einer Architektur "
  "ist der Unterschied dagegen deutlich.\n\n"
  "`/effort auto` ueberlaesst Claude die Einschaetzung und ist fuer die meisten der beste "
  "Ausgangspunkt. `/effort status` zeigt, was gerade gilt.\n\n"
  "Dauerhaft stellst du es ueber `effortLevel` in den Einstellungen ein — nicht ueber eine "
  "Umgebungsvariable, das funktioniert nicht zuverlaessig.\n\n"
  "Ein Stolperstein: Bei Opus 5 vertrugen sich die hohen Stufen nicht mit abgeschaltetem "
  "Denken; die Anfrage schlug fehl. Seit Version 2.1.251 faengt Claude Code das ab und sendet "
  "in dem Fall die naechstniedrigere Stufe."),

 ("Auf den Zwischenspeicher achten — dort liegt das meiste Geld", "Kosten",
  "Wiederholt gesendeter Text ist billig, solange er im Speicher liegt. Eine niedrige Trefferquote kostet.",
  "Bei jeder Anfrage wird der bisherige Zusammenhang erneut mitgeschickt. Das waere teuer — "
  "deshalb gibt es einen Zwischenspeicher: Was schon einmal geschickt wurde, wird beim "
  "naechsten Mal deutlich guenstiger abgerechnet.\n\n"
  "Seit Version 2.1.251 zeigt `/cost`, wie gut dieser Speicher greift: Trefferquote, "
  "Fehlgriffe, neu zwischengespeicherte Menge. Eine niedrige Quote bedeutet bares Geld.\n\n"
  "Was die Quote kaputtmacht: haeufige Aenderungen ganz am Anfang des Zusammenhangs. Wenn sich "
  "die Systemanweisung oder `CLAUDE.md` staendig aendert, ist der Speicher jedes Mal wertlos.\n\n"
  "Ueber `promptCacheTtl` stellst du ein, wie lange er vorhaelt. Ein langer Zeitraum lohnt "
  "sich, wenn zwischen deinen Eingaben laengere Pausen liegen. Fuer Unteragenten gibt es "
  "`subagentPromptCacheTtl` getrennt.\n\n"
  "Der zweite grosse Hebel ist die Modellwahl. Ein leichtes Modell fuer einfache Arbeit spart "
  "mehr, als jede Feinjustierung am Speicher bringt."),

 ("Erst pruefen, dann raten", "Fehlersuche",
  "Den echten Zustand ansehen, statt aus der Erinnerung zu schliessen.",
  "Die haeufigste Ursache fuer verlorene Zeit bei der Fehlersuche ist eine Annahme, die nie "
  "geprueft wurde: „Die Datei liegt bestimmt dort“, „Der Dienst gibt sicher dieses Feld "
  "zurueck“, „Das Flag heisst bestimmt so.“\n\n"
  "Der Gegenzug ist unspektakulaer und wirksam: nachsehen. Bei einer Schnittstelle die echte "
  "Antwort ansehen, nicht die Beschreibung. Bei einer Datei nachsehen, ob sie existiert. Bei "
  "einem Prozess nachsehen, ob er laeuft.\n\n"
  "Wenn nach einer halben Minute nicht klar ist, woran es liegt, ist Raten der falsche naechste "
  "Schritt. Richtig ist, Ausgaben einzubauen: Was kommt hinein, welcher Zweig wird genommen, "
  "was kommt heraus. Dann LAUFEN lassen und die Ausgabe LESEN — erst danach eine Vermutung "
  "bilden.\n\n"
  "Ein deutliches Warnsignal: drei Korrekturen ohne Wirkung. Dann ist die Vermutung ueber die "
  "Ursache falsch, und ein vierter Versuch derselben Art wird auch nichts aendern. Anhalten "
  "und die Annahme pruefen.\n\n"
  "Die eingebauten Ausgaben danach wieder entfernen — sonst sammeln sie sich an."),

 ("Einen Fehler nie zweimal haben", "Fehlersuche",
  "Nach dem Fix die ganze Fehlerklasse schliessen, nicht nur diesen einen Fall.",
  "Ein Fehler ist erst dann wirklich erledigt, wenn er nicht wiederkommen kann. Das ist mehr "
  "Arbeit als die Symptombehandlung und lohnt sich fast immer.\n\n"
  "Der Ablauf hat drei Schritte. Erstens: die wirkliche Ursache finden, nicht die Stelle, an "
  "der es auffiel. Zweitens: fragen, wo derselbe Fehler sonst noch stecken koennte — dieselbe "
  "Art Fehler tritt selten nur an einer Stelle auf. Drittens: dafuer sorgen, dass er nicht "
  "zurueckkommt.\n\n"
  "Der letzte Schritt ist der wertvollste. Am besten so, dass der Fehler gar nicht mehr "
  "moeglich ist — durch eine Pruefung beim Uebersetzen, durch einen Hook, durch eine Signatur, "
  "die den falschen Aufruf ausschliesst. Ein Hinweis in der Dokumentation ist die schwaechste "
  "Form.\n\n"
  "Eine Regel gilt dabei ausnahmslos: Ein Fix darf niemals eine Funktion entfernen oder "
  "abschalten. Wenn ein Feature einen Fehler wirft, wird es repariert. Entfernen ist eine "
  "Entscheidung, die dem Benutzer gehoert.\n\n"
  "Ebenso tabu: ein leeres `catch`. Ein verschluckter Fehler ist schlimmer als ein lauter, "
  "weil er erst viel spaeter auffaellt — an einer Stelle, die nichts damit zu tun hat."),

 ("Beobachtungsschicht vor Feature-Arbeit", "Handwerk",
  "Ohne Protokoll und Sonden suchst du spaeter im Dunkeln.",
  "Bevor die erste Funktion entsteht, gehoert die Beobachtungsschicht ins Projekt. Nachtraeglich "
  "eingebaut ist sie halb so viel wert, weil genau die Stelle fehlt, an der es klemmt.\n\n"
  "Sie besteht aus drei Teilen. Erstens einem strukturierten Protokoll — eine Zeile je "
  "Ereignis, mit Zeitstempel, Stufe, Modul, Funktion, Meldung und Zusatzangaben. Der Pfad wird "
  "beim Start einmal ausgegeben, sonst findet ihn spaeter niemand.\n\n"
  "Zweitens einem globalen Fehlerfaenger. Nichts darf still sterben: Bevor etwas abstuerzt, "
  "wird der volle Zusammenhang geschrieben.\n\n"
  "Drittens — und das ist das Herzstueck — Sonden an den Logikstellen. Sie pruefen "
  "Erwartungen: Ist die Liste wirklich gefuellt? Ist der Zustand einer, den es geben darf? "
  "Sonden zielen auf die STILLEN Fehler, bei denen nichts abstuerzt und trotzdem etwas Falsches "
  "herauskommt.\n\n"
  "Die Schicht bleibt lebendig: Jede neue Logik bringt ihre Sonden mit, geaenderte Logik passt "
  "sie an, entfernte Logik nimmt sie mit. Eine Sonde, die auf etwas Verschwundenes prueft, ist "
  "schlimmer als keine."),

 ("Vor dem Bauen committen und pushen", "Handwerk",
  "Ein Build kann fehlschlagen oder haengen. Der Code soll vorher sicher sein.",
  "Die Reihenfolge lautet: Aenderung fertig, Version hochzaehlen, einchecken, holen und "
  "aufsetzen, hochschieben — und ERST DANN bauen.\n\n"
  "Der Grund wird sofort einleuchtend, wenn man mehrere Sitzungen gleichzeitig am selben "
  "Projekt hat. Baut eine Sitzung vor dem Einchecken und eine andere schiebt in der "
  "Zwischenzeit etwas hoch, gibt es einen Konflikt — und im ungluecklichen Fall ist die "
  "Arbeit beider gefaehrdet.\n\n"
  "Der zweite Grund gilt auch allein: Ein Build kann fehlschlagen oder minutenlang haengen. "
  "Ist der Code vorher eingecheckt, ist er sicher. Danach ist er es nicht.\n\n"
  "Besonders wichtig bei Paketen, die anschliessend hochgeladen werden. Ein hochgeladenes Paket "
  "muss zu einem Stand gehoeren, den es im Verzeichnis gibt — sonst laesst sich hinterher nicht "
  "mehr feststellen, was eigentlich ausgeliefert wurde.\n\n"
  "Die Ausnahmen sind eng: reine Leseoperationen und Wegwerf-Versuche, die gleich wieder "
  "verworfen werden."),

 ("Nur eigene Dateien einchecken", "Handwerk",
  "Ein pauschales Hinzufuegen greift die noch unfertige Arbeit anderer mit.",
  "Wenn mehrere Sitzungen gleichzeitig an einem Projekt arbeiten, liegen im Arbeitsverzeichnis "
  "auch fremde, noch unfertige Aenderungen.\n\n"
  "Ein pauschales Hinzufuegen aller Dateien nimmt sie mit. Das Ergebnis ist ein Commit, der "
  "halbfertige Arbeit enthaelt, die niemand geprueft hat — und der sich schlecht rueckgaengig "
  "machen laesst, weil er zwei Dinge vermischt.\n\n"
  "Der richtige Weg ist, die Dateien namentlich zu nennen. Das ist etwas mehr Tipparbeit und "
  "erspart handfeste Probleme.\n\n"
  "Vor dem Hochschieben lohnt sich immer ein kurzer Blick auf den Zustand des "
  "Arbeitsverzeichnisses. Jede Zeile sollte bewusst sein: eigene Datei — einchecken. Fremde "
  "Datei — liegenlassen. Muell — in die Ignorierliste.\n\n"
  "Und vor jedem Hochschieben holen und aufsetzen. Das erspart die meisten Konflikte, bevor "
  "sie entstehen."),

 ("Zugangsdaten gehoeren nie ins Projekt", "Sicherheit",
  "Was nicht im Projekt liegt, kann auch nicht versehentlich eingecheckt werden.",
  "Die staerkste Massnahme gegen einen versehentlich veroeffentlichten Schluessel ist nicht "
  "eine Ignorierliste und auch kein Hook, der vor dem Einchecken prueft. Es ist der Umstand, "
  "dass der Schluessel gar nicht erst im Projekt liegt.\n\n"
  "Praktisch heisst das: ein zentraler Ordner ausserhalb aller Projekte, und die Projekte lesen "
  "von dort. Ein Schluessel, der nicht im Projekt liegt, kann nicht in einen Commit geraten — "
  "und zwar unabhaengig davon, ob jemand aufmerksam war.\n\n"
  "Eine Ignorierliste hilft nur, solange sie stimmt. Eine einzige Ausnahmeregel darin, "
  "eingefuehrt aus gutem Grund und spaeter vergessen, reicht fuer den Unfall.\n\n"
  "Die zweite Schicht ist `permissions.deny`. Ein Eintrag wie `Read(./.env)` verhindert, dass "
  "der Inhalt ueberhaupt ins Gespraech gelangt — und damit auch, dass er in einem Protokoll "
  "oder einer geteilten Sitzung auftaucht.\n\n"
  "Kommt ein Schluessel doch einmal in einem Gespraech vor, gilt er als offengelegt. Dann "
  "hilft nur, ihn zu ersetzen."),

 ("Zusatzdienste sparsam einsetzen", "Erweitern",
  "Jeder angeschlossene Dienst kostet Platz im Gedaechtnis — auch wenn er nie benutzt wird.",
  "Ueber das Model-Context-Protocol lassen sich viele Dienste anschliessen: Datenbanken, "
  "Ticketsysteme, Browser, Suchmaschinen. Das ist maechtig und hat einen Preis, der leicht "
  "uebersehen wird.\n\n"
  "Jeder angeschlossene Dienst bringt Werkzeugbeschreibungen mit, und die stehen im "
  "Gedaechtnis — bei jeder einzelnen Anfrage, ob der Dienst benutzt wird oder nicht. Bei "
  "einem Dienst mit vielen Werkzeugen sind das schnell mehrere tausend Einheiten.\n\n"
  "Der Rat lautet deshalb: nur einschalten, was du in dieser Sitzung wirklich brauchst. Ueber "
  "`/mcp` laesst sich das schnell umschalten, und `/context` zeigt, was der Anschluss kostet.\n\n"
  "Der zweite Punkt ist Sicherheit. Ein Dienst laeuft mit deinen Rechten und sieht, was du "
  "siehst. Was von ihm zurueckkommt, ist ausserdem fremder Text, der Anweisungen enthalten "
  "koennte — behandle ihn als Daten, nicht als Auftrag.\n\n"
  "Ueber `allowedMcpServers` und `deniedMcpServers` laesst sich festlegen, was ueberhaupt "
  "in Frage kommt."),

 ("Mit mehreren Sitzungen arbeiten, ohne sich zu behindern", "Arbeitsweise",
  "Farben zum Unterscheiden, Hintergrundsitzungen fuer Langes, `/list-agents` fuer den Ueberblick.",
  "Mehrere Sitzungen parallel sind ueblich: eine baut ein Feature, eine sucht einen Fehler, "
  "eine wartet auf einen langen Testlauf. Damit das nicht in Verwirrung endet, gibt es ein "
  "paar einfache Handgriffe.\n\n"
  "Erstens: Farben. `/color` faerbt die Eingabezeile je Sitzung. Klingt banal, verhindert aber "
  "zuverlaessig, dass man in der falschen Sitzung tippt.\n\n"
  "Zweitens: Was lange dauert, gehoert in den Hintergrund. `/background` loest die laufende "
  "Sitzung ab, `/fork` legt einen Zwilling daneben, der einen zweiten Weg verfolgt.\n\n"
  "Drittens: Ueberblick behalten. `/list-agents` zeigt alle Sitzungen samt Unteragenten, "
  "`/tasks` zeigt, was im Hintergrund laeuft.\n\n"
  "Und viertens die Regel, die den meisten Aerger erspart: Zwei Sitzungen sollten nicht "
  "gleichzeitig dieselbe Datei aendern. Beim Einchecken jeweils nur die eigenen Dateien "
  "nennen, vorher holen und aufsetzen."),

 ("Erklaerungen einholen, statt blind zuzustimmen", "Sicherheit",
  "Bei einer Rueckfrage zeigt Strg und E, was der Befehl wirklich tut.",
  "Rueckfragen nutzen sich ab. Wer den zwanzigsten Befehl in einer Stunde bestaetigt, liest "
  "irgendwann nicht mehr genau hin — und genau dann kommt der eine, der etwas loescht.\n\n"
  "Dagegen hilft eine Funktion, die viele nicht kennen: Bei einer Rueckfrage zeigt Strg und E "
  "eine Erklaerung, was der Befehl bewirkt. Das dauert Sekunden und ist die beste Versicherung "
  "gegen ein unbedachtes Ja.\n\n"
  "Gesteuert wird sie ueber `permissionExplainerEnabled` — die sollte anbleiben.\n\n"
  "Der zweite Weg ist, die Zahl der Rueckfragen ehrlich zu senken statt sie zu ertragen. "
  "`/fewer-permission-prompts` sieht sich an, was du ohnehin immer erlaubst, und macht daraus "
  "eine Freigabeliste. Nur lesende, harmlose Aufrufe landen darauf.\n\n"
  "Der dritte und beste Weg ist die Sandbox: abgeschottet, deshalb ohne Rueckfrage — weniger "
  "Unterbrechungen bei mehr Sicherheit."),

 ("Zusammenfassungen des Denkens mitlesen", "Arbeitsweise",
  "Wer sieht, wie Claude denkt, merkt frueh, wenn es in die falsche Richtung geht.",
  "Normalerweise ist das Nachdenken eingeklappt und man sieht nur das Ergebnis. Mit "
  "`showThinkingSummaries` bekommst du eine Kurzfassung der Ueberlegungen.\n\n"
  "Der Nutzen ist nicht Neugier, sondern Zeitersparnis. Geht Claude von einer falschen Annahme "
  "aus, steht das dort — und du kannst nach dreissig Sekunden eingreifen statt nach zehn "
  "Minuten Arbeit in die falsche Richtung.\n\n"
  "Besonders wertvoll bei Aufgaben, in denen es mehrere vertretbare Wege gibt. Dort ist die "
  "Begruendung wichtiger als das Ergebnis.\n\n"
  "Die Zusammenfassung kostet etwas Platz in der Anzeige, aber nichts an Gedaechtnis — sie "
  "faellt ohnehin an.\n\n"
  "Zusammen mit `/focus`, das alles Ueberfluessige ausblendet, ergibt sich eine ruhige und "
  "trotzdem nachvollziehbare Ansicht."),

 ("Regeln und Gedaechtnis auseinanderhalten", "Kontext",
  "Regeln schreibst du. Das automatische Gedaechtnis schreibt Claude. Beides braucht Pflege.",
  "Es gibt zwei Arten von dauerhaftem Wissen, und sie werden leicht verwechselt.\n\n"
  "Regeln und `CLAUDE.md` schreibst du selbst. Dort steht, was gelten SOLL: Vorgaben, "
  "Absprachen, Eigenheiten des Projekts. Das ist der richtige Ort fuer alles, was verlaesslich "
  "gelten muss.\n\n"
  "Das automatische Gedaechtnis fuellt Claude selbst. Es merkt sich, was ihm aufgefallen ist, "
  "und liest es spaeter wieder ein. Bequem — und mit einer Kehrseite: Was einmal gespeichert "
  "ist, wirkt weiter, auch wenn es inzwischen nicht mehr stimmt.\n\n"
  "Deshalb lohnt es sich, gelegentlich mit `/memory` durchzusehen, was da eigentlich steht. "
  "Ein veralteter Eintrag ist schlimmer als kein Eintrag, weil er unbemerkt in eine falsche "
  "Richtung lenkt.\n\n"
  "Fuer beides gilt die Grundregel: kurz halten. Was in jeder Sitzung geladen wird, kostet in "
  "jeder Sitzung."),

 ("Bei Problemen mit der Einrichtung zuerst `/doctor`", "Fehlersuche",
  "Es prueft Einstellungen, Hooks, Dienste und Erweiterungen — und repariert, was es kann.",
  "Wenn Claude Code selbst zickt, ist `/doctor` der erste Griff. Es geht die Einrichtung durch: "
  "Einstellungsdateien, Hooks, angeschlossene Dienste, installierte Erweiterungen.\n\n"
  "Gefundene Probleme werden benannt und, wo moeglich, gleich behoben — etwa eine "
  "Einstellungsdatei, die kein gueltiges JSON mehr ist.\n\n"
  "Dieser Fall verdient besondere Erwaehnung: Ein einziges falsches Zeichen in der "
  "Einstellungsdatei macht die GANZE Datei unbrauchbar, und zwar still. Alle Einstellungen "
  "sind dann weg, ohne dass eine Fehlermeldung erscheint. Nach jeder Aenderung von Hand also "
  "die Datei pruefen.\n\n"
  "`/doctor` prueft ausserdem das Beschreibungs-Budget. Hast du viele Skills, passen ihre "
  "Beschreibungen irgendwann nicht mehr alle ins Gedaechtnis — und die uebrigen werden von "
  "Claude nicht mehr von allein gefunden.\n\n"
  "Bleibt es unklar, hilft `/debug` mit ausfuehrlicher Protokollierung weiter."),

 ("Erst die Aenderung ansehen, dann festschreiben", "Handwerk",
  "`/diff` zeigt Zeile fuer Zeile, was wirklich passiert ist.",
  "Es ist verlockend, nach einer erfolgreichen Aenderung direkt einzuchecken. Der Blick auf "
  "die tatsaechlichen Aenderungen lohnt sich trotzdem fast immer.\n\n"
  "`/diff` zeigt Zeile fuer Zeile, was dazugekommen und was weggefallen ist. Typische Funde: "
  "eine vergessene Debug-Ausgabe, eine mitgeaenderte Datei, die gar nicht dazugehoert, eine "
  "Formatierung, die die halbe Datei umgeschrieben hat.\n\n"
  "Ueber `diffTool` laesst sich die Ansicht in die Entwicklungsumgebung verlegen — bei "
  "groesseren Aenderungen deutlich uebersichtlicher.\n\n"
  "Ist etwas schiefgegangen und du weisst nicht mehr, was angefasst wurde, bringt `/rewind` "
  "dich auf einen frueheren Stand zurueck — samt Dateien, nicht nur dem Gespraech.\n\n"
  "Das setzt allerdings voraus, dass die Sicherungspunkte eingeschaltet sind "
  "(`fileCheckpointingEnabled`). Ohne Versionsverwaltung wuerde ich sie unbedingt anlassen."),

 ("Pruefen lassen, was wichtig ist", "Qualitaet",
  "`/code-review` fuer Fehler, `/security-review` fuer Luecken, `/simplify` fuers Aufraeumen.",
  "Es gibt drei Pruefungen mit unterschiedlichem Blick, und man sollte sie nicht verwechseln.\n\n"
  "`/code-review` sucht nach echten Fehlern und nach unnoetig kompliziertem Code. Die "
  "Gruendlichkeit ist einstellbar: `low` und `medium` melden wenige, dafuer sichere Funde; "
  "`high` und `max` schauen breiter hin und melden auch Unsicheres.\n\n"
  "`/security-review` schaut gezielt auf Sicherheit: ungeprueft weitergereichte Eingaben, "
  "Zugangsdaten im Code, zu weit gefasste Rechte. Immer sinnvoll nach Arbeit an Anmeldung, "
  "Berechtigungen oder Datenverarbeitung.\n\n"
  "`/simplify` sucht ausdruecklich KEINE Fehler. Es raeumt auf und baut die Verbesserung "
  "gleich ein. Am besten nach einer fertigen, laufenden Aenderung.\n\n"
  "Ein Hinweis zu `/verify`: Es prueft, ob der Code wirklich das Richtige tut, und laeuft seit "
  "Version 2.1.215 nur noch auf ausdrueckliche Anforderung. Es kostet viel Zeit — richtig "
  "eingesetzt an den Stellen, an denen ein Fehler teuer waere."),

 ("Version und Zeitstempel sichtbar machen", "Handwerk",
  "Ohne sichtbare Version weiss niemand, ob das Update ueberhaupt angekommen ist.",
  "Bei jeder Aenderung die Version hochzaehlen, den Zeitpunkt festhalten und beides im Programm "
  "sichtbar machen. Das klingt nach Buchhaltung und erspart eine ganze Klasse von Verwirrung.\n\n"
  "Der haeufigste Fall: Etwas wurde geaendert, aber im Programm sieht es aus wie vorher. Ohne "
  "sichtbare Version raetselt man, ob die Aenderung ueberhaupt angekommen ist, ob die "
  "Installation geklappt hat oder ob der Fehler woanders liegt.\n\n"
  "Die Anzeige muss aus derselben Quelle kommen wie die Version selbst, nicht daneben "
  "hartcodiert sein. Sonst laufen beide irgendwann auseinander — und dann ist die Anzeige "
  "schlimmer als keine.\n\n"
  "Der Zeitstempel gehoert dazu und muss von der echten Uhr kommen. Geschaetzte Zeiten sind "
  "wertlos, weil man ihnen nicht ansieht, dass sie geschaetzt sind.\n\n"
  "Bei Android kommt der Versionszaehler dazu: Er muss bei jeder Veroeffentlichung hoeher sein, "
  "sonst lehnt der Store ab."),

 ("Fehlermeldungen sind fuer Menschen da", "Handwerk",
  "Was nicht ging, warum, und was ich jetzt tun kann — in einem Satz.",
  "Eine gute Fehlermeldung beantwortet drei Fragen: Was hat nicht funktioniert? Warum? Was "
  "kann ich tun?\n\n"
  "„Unbekannter Fehler“ beantwortet keine davon. „Vorlesen fehlgeschlagen: Der Schluessel "
  "wurde abgelehnt (401). Pruef ihn in den Einstellungen“ beantwortet alle drei — und gehoert "
  "mit einem Knopf verbunden, der genau dorthin fuehrt.\n\n"
  "Alles, was sich wiederholen laesst, braucht einen Wiederholen-Knopf. Ein Netzwerkfehler ist "
  "in der Regel voruebergehend; ihn als endgueltig darzustellen, ist schlicht falsch.\n\n"
  "Die technischen Einzelheiten gehoeren ins Protokoll, nicht in die Meldung. Aber das "
  "Protokoll muss erreichbar sein — sonst ist es nur ein anderer Weg, die Information zu "
  "verstecken.\n\n"
  "Und der wichtigste Satz zum Schluss: Ein Fehler wird nie dadurch geloest, dass man die "
  "Funktion entfernt, die ihn ausloest. Das ist kein Fix, das ist ein Verlust."),
]
