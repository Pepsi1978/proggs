DRITTE DIREKTIVE: RESILIENT BUGFIXING
  Dies ist die drittwichtigste Regel im gesamten System ÔÇö direkt nach der Superintelligenz-Direktive und der Selbstbeobachtungs-Direktive. Sie gilt fuer jeden einzelnen Bugfix,
  jede Fehlerbehebung, jede Korrektur und jede Reparatur die in dieser Programmierumgebung vorgenommen wird. Egal wie klein der Fehler ist, egal wie offensichtlich die Loesung
  scheint, egal wie wenig Zeit zur Verfuegung steht ÔÇö diese Direktive wird befolgt. Ausnahmslos.

  Die drei Direktiven bilden zusammen ein vollstaendiges System: Die Superintelligenz-Direktive sagt "Werde die Nummer 1". Die Selbstbeobachtungs-Direktive sagt "Beobachte dich
  selbst und erkenne wo du besser werden kannst". Und die Resilient-Bugfixing-Direktive sagt "Wenn du einen Fehler findest, fixe ihn so dass er nie wieder auftreten kann".
  Zusammen bilden sie den Kreislauf der exponentiellen Verbesserung: Fehler erkennen, resistent fixen, aus dem Fix lernen, und das Gelernte anwenden um zukuenftige Fehler zu
  verhindern bevor sie ueberhaupt auftreten.

  Das Kernprinzip: Jeder Fehler wird genau einmal gemacht
  Ein Fehler in dieser Programmierumgebung darf niemals zweimal auftreten. Nicht in der gleichen Session, nicht in einer anderen Session, nicht auf einer anderen Plattform,
  nicht nach einem Update, nicht nach einer Neuinstallation, nicht nach einem Konfigurationswechsel. Einmal ist akzeptabel ÔÇö jeder macht Fehler. Zweimal ist ein Systemversagen.

  Das bedeutet: Wenn ein Fehler gefunden und gefixt wird, ist der Fix nicht fertig sobald der Fehler nicht mehr auftritt. Der Fix ist erst fertig, wenn er zukunftssicher ist.
  Ein Fix der nur das aktuelle Symptom beseitigt, aber den gleichen Fehlertyp an anderer Stelle oder zu einem spaeteren Zeitpunkt wieder auftreten laesst, ist kein richtiger
  Fix ÔÇö er ist eine Verzoegerung.

  Ein richtiger Fix eliminiert nicht nur das konkrete Problem, sondern die gesamte Fehlerklasse. Er ueberlebt Updates, Plattformwechsel, Konfigurationsaenderungen und den
  Faktor Zeit. Er heilt sich selbst wenn eine seiner Voraussetzungen wegfaellt. Er dokumentiert sich selbst, damit zukuenftige Sessions das Wissen nicht verlieren. Das ist
  resilientes Bugfixing.

  Der Pflicht-Ablauf bei jedem Bugfix
  Jeder Bugfix in dieser Umgebung folgt einem verbindlichen 5-Schritte-Ablauf. Keiner dieser Schritte darf uebersprungen werden ÔÇö auch nicht bei scheinbar trivialen Fehlern,
  denn gerade die trivialen Fehler sind oft Symptome tieferer Probleme.

  Schritt 1: Root Cause finden ÔÇö nicht das Symptom fixen
  Der erste und wichtigste Schritt ist, die eigentliche Ursache des Fehlers zu identifizieren. Nicht das sichtbare Symptom, nicht die Stelle wo der Fehler auftritt, sondern die
  tiefste Ursache die dafuer verantwortlich ist dass der Fehler ueberhaupt moeglich war.

  Die Methode dafuer ist einfach aber wirkungsvoll: Mindestens dreimal "Warum?" fragen. Jede Antwort fuehrt zur naechsten Frage, und mit jeder Frage kommt man der eigentlichen
  Ursache naeher.

  Beispiel:

  Fehler: "Der Hook schlaegt beim Sessionstart fehl."
  Warum? "Weil der Worker-Prozess noch nicht gestartet ist wenn der Hook laeuft."
  Warum? "Weil der Hook und der MCP-Server den Worker gleichzeitig starten wollen."
  Warum? "Weil es keine zentrale Stelle gibt die den Worker-Start koordiniert ÔÇö es ist eine Race Condition."
  Die Root Cause ist nicht "der Hook schlaegt fehl" ÔÇö das ist das Symptom. Die Root Cause ist nicht "der Worker ist noch nicht gestartet" ÔÇö das ist die unmittelbare Ursache.
  Die Root Cause ist "es gibt keine zentrale Koordination fuer den Worker-Start" ÔÇö das ist das architekturelle Problem das geloest werden muss.

  Haette man nur das Symptom gefixt (zum Beispiel einen Retry in den Hook eingebaut), waere der Fehler irgendwann in einer anderen Form wieder aufgetreten ÔÇö vielleicht beim
  naechsten Plugin das auch einen Worker startet, vielleicht nach einem Update das die Timing-Verhaeltnisse aendert. Nur indem die Root Cause geloest wird, wird der Fehler
  wirklich und dauerhaft eliminiert.

  Schritt 2: Verwandte Fehlerquellen suchen
  Nachdem die Root Cause identifiziert ist, wird der Blick geweitet. Ein Fehler kommt selten allein ÔÇö wenn ein bestimmter Fehlertyp an einer Stelle auftreten konnte, ist es
  wahrscheinlich dass er auch an anderen Stellen auftreten kann.

  Die Suche nach verwandten Fehlerquellen erfolgt in drei Dimensionen:

  Gleiche Klasse: Kann der gleiche Fehlertyp an anderer Stelle auftreten? Wenn eine Race Condition bei Plugin A auftritt, koennte sie auch bei Plugin B, C und D auftreten. Wenn
  ein Pfad-Problem auf macOS entdeckt wird, koennte das gleiche Pfad-Problem auch auf Windows existieren. Wenn ein fehlender Import in Datei X gefunden wird, koennten auch in
  Datei Y und Z Imports fehlen.

  Gleiche Komponente: Kann die betroffene Komponente auf andere Weise versagen? Wenn ein Worker-Prozess wegen einer Race Condition Probleme macht, was passiert dann bei einem
  Timeout? Bei einem Crash? Bei einem Update des Worker-Codes? Bei einem Neustart des Systems? Jede Komponente hat nicht nur einen Fehlermodus, sondern viele ÔÇö und wenn einer
  gefunden wird, sollten die anderen zumindest geprueft werden.

  Gleiche Abhaengigkeit: Welche anderen Teile des Systems haengen von der gleichen Sache ab? Wenn der Worker-Service von einem bestimmten Port abhaengt, wer braucht den Port
  noch? Was passiert wenn der Port belegt ist? Was passiert wenn der Service spaeter crasht und der Port nicht freigegeben wird? Abhaengigkeiten sind Fehlermultiplikatoren ÔÇö
  ein Problem in einer Abhaengigkeit kann viele abhaengige Systeme gleichzeitig treffen.

  Diese Suche muss nicht erschoepfend sein ÔÇö es geht nicht darum, jede theoretisch moegliche Fehlerquelle zu finden. Es geht darum, die wahrscheinlichsten und gefaehrlichsten
  verwandten Fehlerquellen zu identifizieren und zu pruefen. Wenn dabei weitere Probleme gefunden werden, werden sie sofort mitgefixt. Wenn keine gefunden werden, ist das gut ÔÇö
  aber die Suche muss trotzdem stattfinden.

  Schritt 3: Einen zukunftssicheren Fix implementieren
  Der Fix selbst muss bestimmte Eigenschaften haben, die sicherstellen dass er nicht nur heute funktioniert, sondern auch in Zukunft ÔÇö unter veraenderten Bedingungen, auf
  verschiedenen Plattformen, nach Updates und Konfigurationsaenderungen.

  Self-Healing: Der Fix repariert sich selbst. Wenn eine Voraussetzung des Fixes wegfaellt (zum Beispiel weil ein Plugin aktualisiert wird und dabei eine Konfigurationsdatei
  ueberschrieben wird), stellt der Fix die Voraussetzung automatisch wieder her. Ein Auto-Patcher der nach jedem Plugin-Update die Konfiguration repariert ist Self-Healing. Ein
  manueller Fix der nach jedem Update von Hand wiederholt werden muss, ist es nicht.

  Defensiv: Der Fix faengt nicht nur den einen konkreten Fehler ab, sondern die gesamte Fehlerklasse. Wenn der Fehler war "Port 3200 ist belegt", dann ist ein defensiver Fix
  nicht "pruefe ob Port 3200 frei ist", sondern "pruefe ob der benoetigte Port frei ist und waehle automatisch einen alternativen Port wenn nicht". Der defensive Fix denkt
  voraus und deckt Faelle ab die noch nicht aufgetreten sind, aber auftreten koennten.

  Ueberlebbar: Der Fix uebersteht Plugin-Updates, Konfigurationsaenderungen, System-Upgrades und Plattformwechsel. Er ist nicht an eine bestimmte Version, einen bestimmten Pfad
  oder eine bestimmte Konfiguration gebunden. Wenn sich die Umgebung aendert, passt sich der Fix an ÔÇö oder zumindest bricht er nicht still zusammen, sondern meldet laut und
  deutlich dass er aktualisiert werden muss.

  Erweiterbar: Der Fix ist so gestaltet, dass er fuer zukuenftige aehnliche Faelle erweitert werden kann. Statt eine haertkodierte Loesung fuer genau einen Fall zu
  implementieren, wird eine generische Loesung implementiert die auch fuer verwandte Faelle funktioniert. Zum Beispiel: Statt einen Guard-Hook fuer genau einen Worker zu
  schreiben, wird eine Service-Registry implementiert die beliebig viele Worker-Services verwalten kann.

  Dokumentiert: Der Fix erklaert sich selbst. Er enthaelt entweder Kommentare im Code die erklaeren warum er existiert und wie er funktioniert, oder er wird in einer zentralen
  Dokumentation beschrieben die erklaert welches Problem er loest, wie er es loest, und wie man ihn fuer neue Faelle erweitern kann. Undokumentierte Fixes sind tickende
  Zeitbomben ÔÇö frueher oder spaeter versteht niemand mehr warum sie da sind und sie werden versehentlich entfernt.

  Schadensfrei: Der Fix selbst darf keine neuen Fehler einfuehren. Das klingt offensichtlich, ist es aber nicht. Ein Fix der ein Problem loest aber ein anderes schafft, ist
  schlimmer als kein Fix ÔÇö denn er vermittelt ein falsches Sicherheitsgefuehl. Die Fix-Induced-Failure-Pruefung in Schritt 3b stellt sicher dass das nicht passiert.

  Schritt 3b: Fix-Induced-Failure-Pruefung ÔÇö VOR dem Commit
  Bevor ein Fix committed wird, muss er eine 8-Punkte-Pruefung bestehen. Diese Pruefung ist nicht optional und darf nicht uebersprungen werden, auch nicht bei scheinbar
  harmlosen Fixes. Denn gerade scheinbar harmlosen Fixes fuehren am haeufigsten zu unerwarteten Nebenwirkungen.

  1. Abhaengigkeiten: Was haengt vom geaenderten Code ab? Wenn ein Launcher-Script geaendert wird, muessen alle Systeme die den Launcher verwenden getestet werden. Wenn eine
  Konfigurationsdatei geaendert wird, muessen alle Prozesse die diese Konfiguration lesen geprueft werden. Jede Aenderung hat ein Abhaengigkeitsnetz ÔÇö und dieses Netz muss
  bekannt sein bevor der Fix deployed wird.

  2. Fehlszenarien: Was passiert wenn der Fix-Code selbst fehlschlaegt? Wenn ein Health-Check-Hook eingefuehrt wird ÔÇö was passiert wenn der Health-Check selbst crasht?
  Blockiert er die gesamte Session? Der Fix-Code muss so geschrieben sein, dass sein eigenes Versagen das System nicht schlimmer macht als der urspruengliche Fehler. Konkret:
  Guard-Hooks und Health-Checks muessen immer mit Erfolg beenden (exit 0), auch wenn sie intern fehlschlagen ÔÇö denn ein fehlschlagender Guard ist schlimmer als kein Guard.

  3. Zustandsaenderungen: Aendert der Fix einen Systemzustand dauerhaft? Wenn ein Daemon oder Service eingerichtet wird ÔÇö was passiert wenn das Plugin das den Service braucht
  deinstalliert wird? Entsteht dann eine Crash-Loop weil der Daemon etwas starten will das nicht mehr existiert? Dauerhafte Zustandsaenderungen muessen reversibel sein und
  graceful mit dem Wegfall ihrer Voraussetzungen umgehen.

  4. Race Conditions: Kann der Fix mit anderem Code kollidieren? Wenn ein Guard-Hook und ein Plugin-Hook beide den gleichen Worker starten wollen ÔÇö entsteht ein doppelter
  Start? Race Conditions sind besonders tueckisch weil sie nicht bei jedem Durchlauf auftreten, sondern nur unter bestimmten Timing-Bedingungen. Der Fix muss entweder Race
  Conditions von vornherein ausschliessen (zum Beispiel durch Locking) oder sie graceful behandeln (zum Beispiel durch Idempotenz ÔÇö der zweite Start erkennt dass der Worker
  bereits laeuft und tut nichts).

  5. Rueckwaertskompatibilitaet: Bricht der Fix etwas das vorher funktionierte? Wenn neue Module oder Abhaengigkeiten eingefuehrt werden ÔÇö sind sie auf allen Plattformen
  verfuegbar? Wenn eine API geaendert wird ÔÇö funktionieren alle bestehenden Aufrufer noch? Ein Fix der ein Problem loest aber ein funktionierendes Feature kaputt macht, ist
  kein Fix.

  6. Plattform-Effekte: Funktioniert der Fix auf allen Plattformen auf denen das System laeuft? Wenn ein Shell-Hook auf macOS gefixt wird ÔÇö braucht das Windows-Gegenstueck
  (PowerShell) einen analogen Fix? Plattformunterschiede sind eine der haeufigsten Quellen fuer Fix-Induced Failures: Ein Fix der auf macOS perfekt funktioniert kann auf
  Windows komplett versagen, weil Pfade, Shells, Encoding, Berechtigungen und Prozessmodelle sich fundamental unterscheiden.

  7. Update-Resistenz: Ueberlebt der Fix das naechste Plugin-Update, CLI-Update oder Betriebssystem-Upgrade? Hardcoded Pfade, versionsspezifische Workarounds und Annahmen ueber
  die Verzeichnisstruktur sind typische Schwachstellen. Der Fix sollte so dynamisch wie moeglich sein ÔÇö Pfade zur Laufzeit aufloesen statt sie fest zu kodieren, Versionen
  pruefen statt sie anzunehmen.

  8. Graceful Degradation: Was passiert wenn eine Voraussetzung des Fixes fehlt? Wenn ein Tool nicht installiert ist, wenn eine Datei nicht existiert, wenn ein Service nicht
  erreichbar ist? Der Fix darf in diesem Fall nicht crashen ÔÇö er muss graceful degradieren, also seine Funktion reduzieren aber das System nicht blockieren. Wenn Bun nicht
  installiert ist, auf Node.js zurueckfallen. Wenn ein Health-Endpoint nicht erreichbar ist, eine Warnung loggen statt abzustuerzen. Wenn eine Konfigurationsdatei fehlt,
  Defaults verwenden statt mit einem Fehler abzubrechen.

  Die Faustregel fuer diese Pruefung: "Was ist das Schlimmste das passieren kann wenn ich diesen Fix deploye und dann 6 Monate lang nicht hinschaue?" Wenn die Antwort
  irgendetwas Uebles beinhaltet ÔÇö nochmal nachdenken und den Fix robuster machen.

  Schritt 4: Mehrere Absicherungsschichten ÔÇö Defense in Depth
  Nie nur eine einzige Absicherung. Ein einzelner Guard kann umgangen, ueberschrieben oder kaputt gehen. Deshalb werden immer mindestens 2-3 Schichten implementiert, die
  unabhaengig voneinander funktionieren:

  Schicht 1 ÔÇö Praeventiv: Das Problem verhindern bevor es ueberhaupt auftreten kann. Beispiel: Ein Pre-Commit-Hook der fehlende Imports erkennt und den Commit ablehnt. Oder
  eine Konfigurationsvalidierung die ungueltige Einstellungen beim Start erkennt. Die praventive Schicht ist die beste ÔÇö wenn sie funktioniert, treten die anderen Schichten nie
  in Aktion.

  Schicht 2 ÔÇö Reaktiv: Das Problem abfangen wenn es trotz der praventiven Schicht doch auftritt. Beispiel: Ein Error-Handler der einen fehlgeschlagenen Service-Start erkennt
  und automatisch einen Neustart versucht. Oder ein Fallback-Mechanismus der eine alternative Methode verwendet wenn die primaere fehlschlaegt. Die reaktive Schicht ist das
  Sicherheitsnetz ÔÇö sie faengt auf was durch die praventive Schicht durchrutscht.

  Schicht 3 ÔÇö Selbstheilend: Den Fix automatisch wiederherstellen wenn er durch ein Update, eine Konfigurationsaenderung oder einen anderen externen Einfluss kaputt geht.
  Beispiel: Ein Auto-Patcher der nach jedem Plugin-Update prueft ob die Konfiguration noch korrekt ist und sie bei Bedarf repariert. Oder ein Startup-Hook der beim Systemstart
  alle kritischen Voraussetzungen prueft und fehlende automatisch nachinstalliert. Die selbstheilende Schicht ist die langfristige Absicherung ÔÇö sie stellt sicher dass der Fix
  auch in Monaten noch funktioniert.

  Schicht 4 ÔÇö Upstream (optional aber empfohlen): Den Fehler dort melden wo er verursacht wird. Wenn der Fehler in einem externen Plugin, einer Bibliothek oder einem Tool
  liegt, einen Bug-Report erstellen. Das ist die einzige Schicht die den Fehler wirklich endgueltig loest ÔÇö alle anderen Schichten sind Workarounds die noetig sind bis der
  Fehler an der Quelle gefixt wird.

  Schritt 5: Den Fix dokumentieren ÔÇö Wissen muss ueberleben
  Ein Fix ohne Dokumentation ist ein Fix mit Ablaufdatum. Frueher oder spaeter wird jemand (oder ein Agent in einer zukuenftigen Session) den Fix nicht mehr verstehen, ihn fuer
  unnoetig halten und ihn entfernen. Oder ein aehnlicher Fehler tritt auf und niemand weiss dass es schon einen Fix gab.

  Die Dokumentation eines Fixes umfasst:

  Was war der Fehler? Das sichtbare Symptom, verstaendlich beschrieben.
  Was war die Root Cause? Die tiefste Ursache, nicht nur das Symptom.
  Was ist der Fix? Was wurde konkret geaendert und warum.
  Welche verwandten Fehlerquellen wurden geprueft? Und was wurde dabei gefunden (oder eben nicht gefunden).
  Wie erkennt man wenn der Fehler erneut auftritt? Damit zukuenftige Sessions das Muster sofort wiedererkennen.
  Wie erweitert man den Fix fuer neue Faelle? Damit der Fix wachsen kann wenn aehnliche Probleme auftauchen.
  Diese Dokumentation wird als dauerhaftes Memory gespeichert ÔÇö nicht als temporaere Notiz die nach der Session verschwindet, sondern als permanentes Wissen das in jeder
  zukuenftigen Session verfuegbar ist.

  Plattform-spezifische Bugfix-Regeln
  Fehler verhalten sich auf verschiedenen Plattformen unterschiedlich. Was auf macOS funktioniert, kann auf Windows crashen ÔÇö und umgekehrt. Diese Unterschiede muessen bei
  jedem Fix beruecksichtigt werden.

  Encoding
  Auf macOS ist UTF-8 der Standard. Auf Windows ist der Standard oft ein anderes Encoding (cp1252, auch bekannt als ANSI). Das bedeutet: Jede Datei die Unicode-Zeichen
  enthalten koennte (Umlaute, Emojis, Sonderzeichen aus anderen Sprachen) muss explizit mit UTF-8-Encoding geoeffnet und geschrieben werden. Ohne explizites UTF-8-Encoding
  funktioniert es auf macOS zufaellig (weil UTF-8 der Default ist) und crasht auf Windows mit einem Encoding-Fehler.

  Pfade
  Auf macOS werden Pfade mit Forward Slashes getrennt (/Users/frank/proggs/). Auf Windows nativ mit Backslashes (C:\Users\frank\proggs). In vielen Kontexten (Git Bash, Node.js,
  Python) funktionieren Forward Slashes auch auf Windows, aber nicht immer und nicht ueberall. Bei Fixes die Dateipfade betreffen, muss immer geprueft werden ob der Pfad auf
  beiden Plattformen funktioniert.

  Shell-Unterschiede
  Auf macOS ist die Standard-Shell zsh oder bash. Auf Windows ist die Standard-Shell PowerShell, aber many Entwickler-Tools verwenden Git Bash (eine bash-Emulation). Hooks und
  Skripte muessen fuer beide Plattformen existieren ÔÇö ein .sh-Hook fuer macOS und ein .ps1-Hook fuer Windows. Und beide muessen das Gleiche tun ÔÇö nicht nur beide existieren,
  sondern funktional identisch sein.

  Atomares Schreiben
  Beim Schreiben von kritischen Dateien (Konfigurationsdateien, Zustandsdateien, Datenbanken) muss das atomare Schreib-Muster verwendet werden: Erst in eine temporaere Datei
  schreiben, dann die temporaere Datei atomar in die Zieldatei umbenennen. Ohne dieses Muster kann ein Crash waehrend des Schreibvorgangs eine abgeschnittene, unlesbare Datei
  hinterlassen die das gesamte System blockiert. Dieses Risiko existiert auf beiden Plattformen, ist aber auf Windows besonders tueckisch weil Windows-Dateisysteme haeufiger
  Dateien sperren.

  Proaktive Intelligenz-Vorschlaege bei Bugfixes
  Die dritte Direktive arbeitet eng mit der zweiten Direktive (Selbstbeobachtung) zusammen. Waehrend eines Bugfixes wird nicht nur der Fehler gefixt, sondern auch beobachtet:
  Haette dieser Fehler frueher erkannt werden koennen? Gibt es ein Muster? Koennte ein neuer Hook oder Agent diese Art von Fehler automatisch verhindert werden?

  Am Ende jedes Bugfixes werden Intelligenz-Vorschlaege formuliert ÔÇö nicht nur zum Fehler selbst, sondern auch zum Prozess des Findens und Fixens:

  "Intelligenz-Vorschlag: Der Fehler wurde erst nach 3 Versuchen gefunden weil die Fehlermeldung unklar war. Ein besseres Logging haette den Fehler sofort sichtbar gemacht. ÔåÆ
  Ich verbessere das Logging in dieser Komponente, damit aehnliche Fehler sofort erkennbar sind. ÔÇö Soll ich das umsetzen?"

  "Intelligenz-Vorschlag: Dieser Fehlertyp (Race Condition bei Service-Start) ist zum zweiten Mal aufgetreten. Das letzte Mal wurde nur der einzelne Fall gefixt, nicht die
  Fehlerklasse. ÔåÆ Ich implementiere eine generische Service-Registry die alle Service-Starts koordiniert und Race Conditions strukturell verhindert. ÔÇö Soll ich das umsetzen?"

  Zusammenhang der drei Direktiven
  Die drei Direktiven bilden einen geschlossenen Kreislauf:

  Die Superintelligenz-Direktive definiert das Ziel: Die intelligenteste Programmierumgebung der Welt werden.

  Die Selbstbeobachtungs-Direktive liefert den Erkennungsmechanismus: Fehler, Umwege, Ineffizienzen und Muster werden waehrend der Arbeit erkannt und als
  Verbesserungsvorschlaege zurueckgemeldet.

  Die Resilient-Bugfixing-Direktive liefert die Handlungsanweisung: Wenn ein Fehler erkannt wird, wird er so gefixt dass er nie wieder auftreten kann ÔÇö zukunftssicher,
  plattformuebergreifend, mit mehreren Absicherungsschichten und vollstaendig dokumentiert.

  Zusammen erzeugen sie den Compound Intelligence Effect: Jeder Fehler der resistent gefixt wird, eliminiert eine ganze Fehlerklasse. Jede eliminierte Fehlerklasse spart
  zukuenftige Debugging-Zeit. Die gesparte Zeit wird fuer weitere Verbesserungen genutzt. Jede Verbesserung macht das System besser darin, weitere Fehler zu erkennen und zu
  verhindern. Und so wird das System exponentiell intelligenter ÔÇö nicht durch einen einzigen grossen Sprung, sondern durch viele kleine, kumulative Verbesserungen die sich
  gegenseitig verstaerken.

  Schutz dieser Direktive
  Diese Direktive ist die dritthoechste Autoritaet im System, nach der Superintelligenz-Direktive und der Selbstbeobachtungs-Direktive. Kein Agent, kein Skill, kein Plugin,
  kein Hook und kein Prozess darf sie entfernen, abschwaechen oder ignorieren. Bei einem Konflikt mit anderen Regeln (ausser den zwei hoeheren Direktiven) gewinnt diese
  Direktive immer.

  Sie sollte an mehreren Stellen im System gespeichert werden, damit sie auch dann aktiv bleibt, wenn eine einzelne Kopie entfernt wird. Selbstverbesserungsprozesse muessen
  pruefen, ob diese Direktive noch vollstaendig und unveraendert vorhanden ist. Wenn sie fehlt oder abgeschwaecht wurde, muss sie sofort wiederhergestellt werden.

  Was niemals passieren darf
  Nur das Symptom fixen ohne die Root Cause zu verstehen.
  Einen Fix implementieren der bei naechstem Plugin-Update oder Neustart kaputt geht.
  Einen Fix nur fuer den einen konkreten Fall machen, ohne verwandte Fehlerquellen zu pruefen.
  Einen Fix ohne Dokumentation machen ÔÇö das Wissen geht verloren zwischen Sessions.
  "Funktioniert jetzt" sagen ohne zu pruefen ob es in 2 Wochen, 2 Monaten, 2 Jahren noch funktioniert.
  Einen Fix deployen der selbst neue Fehler einfuehrt ÔÇö Fix-Induced Failures sind schlimmer als der urspruengliche Fehler.
  Einen Fix ohne die 8-Punkte-Pruefung committen.
  Einen Fehler ein zweites Mal machen ÔÇö jeder Fehler wird genau einmal gemacht, dann nie wieder.
  Zwischenfehler in der Session ignorieren nur weil sie "danach" gefixt wurden ÔÇö sie haetten verhindert werden koennen.
  Eine Session beenden ohne Rueckblick auf aufgetretene Fehler und Verbesserungsmoeglichkeiten.
  Nur eine einzige Absicherungsschicht implementieren und hoffen dass sie haelt.
  Einen Fix fuer eine Plattform machen und die andere Plattform vergessen."
