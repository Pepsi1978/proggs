package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsEasy(): List<Question> = listOf(

    // ─── FUSSBALL (12) ────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wer war Torschuetzenkoenig der FIFA WM 2014 in Brasilien?",
        answerA = "Thomas Mueller",
        answerB = "Lionel Messi",
        answerC = "James Rodriguez",
        answerD = "Robin van Persie",
        correctAnswer = 2,
        explanation = "James Rodriguez aus Kolumbien wurde mit 6 Toren Torschuetzenkoenig der WM 2014 – und gewann dabei auch den Preis fuer das schoenste Tor.",
        difficulty = 1,
        funFact = "James Rodriguez war damals erst 22 Jahre alt und wurde zum Shooting-Star des Turniers."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Farbe hat die Karte, die ein Fussballschiedsrichter bei einer Verwarnung zeigt?",
        answerA = "Rot",
        answerB = "Gelb",
        answerC = "Blau",
        answerD = "Orange",
        correctAnswer = 1,
        explanation = "Die gelbe Karte steht fuer eine Verwarnung. Bekommt ein Spieler zwei gelbe Karten, folgt automatisch eine rote Karte.",
        difficulty = 1,
        funFact = "Das Kartensystem wurde 1970 bei der WM in Mexiko zum ersten Mal eingesetzt – erfunden von Ken Aston, einem englischen Schiedsrichter."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst das beruemteste Stadion in Deutschland?",
        answerA = "Allianz Arena",
        answerB = "Signal Iduna Park",
        answerC = "Olympiastadion Berlin",
        answerD = "Volksparkstadion Hamburg",
        correctAnswer = 0,
        explanation = "Die Allianz Arena in Muenchen ist das bekannteste und modernste Fussballstadion Deutschlands und Heimat des FC Bayern Muenchen.",
        difficulty = 1,
        funFact = "Die Allianz Arena kann ihre Fassade in Rot, Weiss oder Blau beleuchten – je nachdem welches Team spielt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer ist Rekordtorschuetze der deutschen Nationalmannschaft?",
        answerA = "Gerd Mueller",
        answerB = "Miroslav Klose",
        answerC = "Karl-Heinz Rummenigge",
        answerD = "Thomas Mueller",
        correctAnswer = 1,
        explanation = "Miroslav Klose erzielte in 137 Länderspielen 71 Tore fuer Deutschland und ist damit Rekordtorschuetze der Nationalmannschaft.",
        difficulty = 1,
        funFact = "Klose ist auch WM-Rekordtorschuetze mit insgesamt 16 Treffern bei Weltmeisterschaften."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie gross ist ein Fussballtor laut offizieller Regelung?",
        answerA = "6 m breit, 2 m hoch",
        answerB = "7,32 m breit, 2,44 m hoch",
        answerC = "8 m breit, 2,5 m hoch",
        answerD = "7 m breit, 2 m hoch",
        correctAnswer = 1,
        explanation = "Ein Fussballtor ist laut FIFA-Regelwerk exakt 7,32 Meter breit und 2,44 Meter hoch.",
        difficulty = 1,
        funFact = "In Fussschuhen haette ein durchschnittlicher Erwachsener etwa 11 Schritte Abstand zwischen den beiden Pfosten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nationalmannschaft traegt den Spitznamen 'Die Mannschaft'?",
        answerA = "Oesterreich",
        answerB = "Schweiz",
        answerC = "Deutschland",
        answerD = "Niederlande",
        correctAnswer = 2,
        explanation = "Die deutsche Fussballnationalmannschaft traegt offiziell den Spitznamen 'Die Mannschaft', der 2014 eingefuehrt wurde.",
        difficulty = 1,
        funFact = "Der Spitzname wurde vom DFB 2014 nach dem WM-Sieg populaer gemacht, stiess aber bei vielen Fans auf wenig Begeisterung."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Land wurde Lionel Messi geboren?",
        answerA = "Spanien",
        answerB = "Brasilien",
        answerC = "Argentinien",
        answerD = "Uruguay",
        correctAnswer = 2,
        explanation = "Lionel Messi wurde am 24. Juni 1987 in Rosario, Argentinien geboren und ist argentinischer Nationalspieler.",
        difficulty = 1,
        funFact = "Messi wurde als Jugendlicher wegen eines Wachstumshormon-Mangels behandelt – der FC Barcelona uebernahm die Kosten dafuer."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein Elfmeter im Fussball?",
        answerA = "Ein Freistoss aus 11 Metern Entfernung direkt auf das Tor",
        answerB = "Ein Schuss aus der Mittellinie",
        answerC = "Ein Eckball nach 11 Minuten",
        answerD = "Ein Freistoss von der Eckfahne",
        correctAnswer = 0,
        explanation = "Ein Elfmeter ist ein Strafstoss, der vom Elfmeterpunkt (11 Meter vor dem Tor) direkt auf das Tor ausgefuehrt wird.",
        difficulty = 1,
        funFact = "Der Elfmeterpunkt wurde 1891 in den Fussballregeln eingefuehrt. Der erste Elfmeter der Geschichte wurde in Irland geschossen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Verein spielt im Westfalenstadion (Signal Iduna Park)?",
        answerA = "FC Schalke 04",
        answerB = "Borussia Moenchengladbach",
        answerC = "Borussia Dortmund",
        answerD = "Bayer Leverkusen",
        correctAnswer = 2,
        explanation = "Borussia Dortmund spielt im Signal Iduna Park, der frueher als Westfalenstadion bekannt war und 81.365 Zuschauer fasst.",
        difficulty = 1,
        funFact = "Die beruehmte 'Suedtribuene' des Signal Iduna Parks ist die groesste Stehplatztribuene Europas."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man im Fussball einen Schuss auf das Tor?",
        answerA = "Dribbling",
        answerB = "Torschuss",
        answerC = "Graetstsche",
        answerD = "Flanke",
        correctAnswer = 1,
        explanation = "Ein Torschuss ist ein gezielter Schuss eines Spielers in Richtung des gegnerischen Tores mit dem Ziel, einen Treffer zu erzielen.",
        difficulty = 1,
        funFact = "Die stärksten Torschuesse im Profifussball erreichen Geschwindigkeiten von ueber 200 km/h."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Farbe tragen die Trikots des FC Bayern Muenchen traditionell?",
        answerA = "Blau-Weiss",
        answerB = "Rot-Weiss",
        answerC = "Gruen-Weiss",
        answerD = "Schwarz-Gelb",
        correctAnswer = 1,
        explanation = "Der FC Bayern Muenchen traegt traditionell rote Heimtrikots mit weissen Details – seit der Gruendung des Vereins im Jahr 1900.",
        difficulty = 1,
        funFact = "Das Wappen des FC Bayern Muenchen zeigt die Raute der bayerischen Flagge in den Vereinsfarben Rot und Weiss."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter einem 'Freistoss' im Fussball?",
        answerA = "Ein Schuss ohne Torhueter",
        answerB = "Ein Stoss, der nach einem Foulspiel verhaengt wird",
        answerC = "Ein Penalty nach Handspiel",
        answerD = "Ein Schuss von der Grundlinie",
        correctAnswer = 1,
        explanation = "Ein Freistoss wird nach einem Regelverstoss (Foul, Handspiel etc.) verhaengt. Der Ball wird vom Tatort neu ins Spiel gebracht.",
        difficulty = 1,
        funFact = "Es gibt zwei Arten von Freistoessen: direkte (Tor moeglich) und indirekte (Ball muss beruehrt werden bevor ein Tor zaehlt)."
    ),

    // ─── OLYMPIA (8) ──────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Aus wie vielen Ringen besteht das Olympische Symbol?",
        answerA = "4",
        answerB = "5",
        answerC = "6",
        answerD = "7",
        correctAnswer = 1,
        explanation = "Das Olympische Symbol besteht aus fuenf ineinander verschlungenen Ringen in den Farben Blau, Gelb, Schwarz, Gruen und Rot.",
        difficulty = 1,
        funFact = "Die fuenf Ringe stehen fuer die fuenf Kontinente der Welt, die durch die Olympischen Spiele vereint werden."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Stadt fanden die Olympischen Sommerspiele 2024 statt?",
        answerA = "Los Angeles",
        answerB = "Brisbane",
        answerC = "Paris",
        answerD = "London",
        correctAnswer = 2,
        explanation = "Die Olympischen Sommerspiele 2024 fanden in Paris, Frankreich statt – zum dritten Mal nach 1900 und 1924.",
        difficulty = 1,
        funFact = "Paris war die erste Stadt, die die Olympischen Spiele in modernem Flair mit Wettkampfstaetten mitten in der Stadt austrug."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Medaillen werden bei Olympia vergeben?",
        answerA = "Gold, Silber und Bronze",
        answerB = "Platin, Gold und Silber",
        answerC = "Gold, Kupfer und Bronze",
        answerD = "Diamant, Gold und Silber",
        correctAnswer = 0,
        explanation = "Bei Olympia erhalten die drei besten Athleten Gold (1. Platz), Silber (2. Platz) und Bronze (3. Platz).",
        difficulty = 1,
        funFact = "Olympische Goldmedaillen bestehen zu mehr als 90 % aus Silber – nur mit einer duennen Goldschicht ueberzogen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie oft finden die Olympischen Sommerspiele statt?",
        answerA = "Alle 2 Jahre",
        answerB = "Alle 4 Jahre",
        answerC = "Alle 3 Jahre",
        answerD = "Jaehrlich",
        correctAnswer = 1,
        explanation = "Die Olympischen Sommerspiele finden alle vier Jahre statt. Dieser Rhythmus wird als 'Olympiade' bezeichnet.",
        difficulty = 1,
        funFact = "Die Olympischen Winter- und Sommerspiele wechseln sich im Zwei-Jahres-Rhythmus ab, seit 1994 zeitversetzt."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Land wurden die ersten modernen Olympischen Spiele 1896 ausgetragen?",
        answerA = "Frankreich",
        answerB = "England",
        answerC = "Griechenland",
        answerD = "Italien",
        correctAnswer = 2,
        explanation = "Die ersten modernen Olympischen Spiele fanden 1896 in Athen, Griechenland statt – als Wiederbelebung der antiken Spiele.",
        difficulty = 1,
        funFact = "Der Franzose Pierre de Coubertin war der Initiator der modernen Olympischen Spiele und gruendete das Internationale Olympische Komitee."
    ),

    Question(
        categoryId = 6,
        questionText = "Was traegt der Athlet bei der Eroeffnungsfeier der Olympischen Spiele?",
        answerA = "Eine Trophae",
        answerB = "Die Landesflagge",
        answerC = "Das olympische Feuer",
        answerD = "Ein Siegerkranz",
        correctAnswer = 1,
        explanation = "Bei der Eroeffnungsfeier zieht jede Nation mit einem Fahnentraeger ein, der die Flagge seines Landes traegt.",
        difficulty = 1,
        funFact = "Traditionell zieht Griechenland als erstes in das Stadion ein, da es die Heimat der Olympischen Spiele ist."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart wird NICHT bei den Olympischen Winterspielen ausgetragen?",
        answerA = "Curling",
        answerB = "Skifahren",
        answerC = "Schwimmen",
        answerD = "Eisschnelllauf",
        correctAnswer = 2,
        explanation = "Schwimmen ist eine Sommersportart und wird bei den Olympischen Sommerspielen ausgetragen, nicht bei den Winterspielen.",
        difficulty = 1,
        funFact = "Curling ist seit 1998 offiziell olympisch und gilt als eine der taktisch anspruchsvollsten Wintersportarten."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer entzuendet das olympische Feuer bei der Eroeffnungsfeier?",
        answerA = "Der Praesident des Gastgeberlandes",
        answerB = "Der IOC-Praesident",
        answerC = "Eine bekannte Persoenlichkeit oder Sportler des Gastgeberlandes",
        answerD = "Immer der letzte Olympiasieger im 100-Meter-Lauf",
        correctAnswer = 2,
        explanation = "Das olympische Feuer wird traditionell von einer bekannten Persoenlichkeit oder einem verdienstvollen Sportler des Gastgeberlandes entzuendet.",
        difficulty = 1,
        funFact = "Das olympische Feuer wird in Olympia, Griechenland, mit Hilfe eines Hohlspiegels von der Sonne entzuendet."
    ),

    // ─── TENNIS (5) ───────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welche vier Turniere gehoeren zum Grand Slam im Tennis?",
        answerA = "Wimbledon, French Open, US Open, Australian Open",
        answerB = "Wimbledon, US Open, Masters, Davis Cup",
        answerC = "French Open, Wimbledon, ATP Finals, Rogers Cup",
        answerD = "Australian Open, Roland Garros, Wimbledon, Miami Open",
        correctAnswer = 0,
        explanation = "Die vier Grand-Slam-Turniere sind Australian Open (Melbourne), French Open (Paris), Wimbledon (London) und US Open (New York).",
        difficulty = 1,
        funFact = "Ein 'Grand Slam' bezeichnet den Gewinn aller vier Turniere in einem Kalenderjahr – bisher von nur wenigen Spielern erreicht."
    ),

    Question(
        categoryId = 6,
        questionText = "Auf welchem Untergrund wird in Wimbledon gespielt?",
        answerA = "Sand",
        answerB = "Hartplatz",
        answerC = "Rasen",
        answerD = "Kunststoff",
        correctAnswer = 2,
        explanation = "Wimbledon wird auf Rasenplaetzen gespielt – daher gilt es als das traditionsreichste und exklusivste Grand-Slam-Turnier.",
        difficulty = 1,
        funFact = "Die weissen Outfits bei Wimbledon sind Pflicht – eine Regel, die seit dem 19. Jahrhundert besteht."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lautet der erste Punkt im Tennis?",
        answerA = "10",
        answerB = "15",
        answerC = "1",
        answerD = "5",
        correctAnswer = 1,
        explanation = "Im Tennis lautet die Punktzaehlung: 0 (Love), 15, 30, 40, Spiel – der erste gewonnene Punkt ergibt also 15.",
        difficulty = 1,
        funFact = "Die ungewoehnliche Zaehlung im Tennis stammt moeglicherweise von einer alten Uhr: Die Zeiger wurden fuer jeden Punkt um 15 Minuten weitergedreht."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst die bekannteste deutsche Tennisspielerin aller Zeiten?",
        answerA = "Anke Huber",
        answerB = "Sabine Lisicki",
        answerC = "Steffi Graf",
        answerD = "Andrea Petkovic",
        correctAnswer = 2,
        explanation = "Steffi Graf gewann 22 Grand-Slam-Titel und war 377 Wochen lang die Weltranglistenerste – absoluter Rekord.",
        difficulty = 1,
        funFact = "Steffi Graf ist die einzige Tennisspielerin, die alle vier Grand Slams und eine Olympia-Goldmedaille im selben Jahr gewann (1988)."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet 'Ass' im Tennis?",
        answerA = "Ein Aufschlag, den der Gegner nicht beruehren kann",
        answerB = "Ein doppelter Aufschlagfehler",
        answerC = "Ein Volley direkt am Netz",
        answerD = "Ein Rueckhand-Winner",
        correctAnswer = 0,
        explanation = "Ein Ass ist ein Aufschlag, der das Feld des Gegners trifft, ohne dass dieser den Ball beruehren kann – direkter Punktgewinn.",
        difficulty = 1,
        funFact = "Der Spieler mit den meisten Asses in der ATP-Geschichte ist Ivo Karlovic – er hat ueber 13.000 Asse in seiner Karriere geschlagen."
    ),

    // ─── BASKETBALL / NBA (5) ─────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr wurde die NBA (National Basketball Association) gegruendet?",
        answerA = "1946",
        answerB = "1956",
        answerC = "1963",
        answerD = "1971",
        correctAnswer = 0,
        explanation = "Die NBA wurde 1946 als Basketball Association of America (BAA) gegruendet und erhielt 1949 ihren heutigen Namen.",
        difficulty = 1,
        funFact = "Die ersten Basketballspiele wurden 1891 von James Naismith in Springfield, Massachusetts, erfunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Team aus Los Angeles spielt in der NBA?",
        answerA = "Los Angeles Dodgers",
        answerB = "Los Angeles Lakers",
        answerC = "Los Angeles Rams",
        answerD = "Los Angeles Kings",
        correctAnswer = 1,
        explanation = "Die Los Angeles Lakers sind eines der beruemtesten NBA-Teams und spielen im Crypto.com Arena in Los Angeles.",
        difficulty = 1,
        funFact = "Der Name 'Lakers' stammt aus der Zeit als das Team in Minneapolis spielte – Minnesota ist bekannt als 'Land of 10,000 Lakes'."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie hoch ist ein Basketball-Korb vom Boden?",
        answerA = "2,75 m",
        answerB = "3,05 m",
        answerC = "3,50 m",
        answerD = "2,50 m",
        correctAnswer = 1,
        explanation = "Ein Basketballkorb haengt exakt 3,05 Meter (10 Fuss) ueber dem Boden – diese Hoehe ist weltweit standardisiert.",
        difficulty = 1,
        funFact = "James Naismith haengte 1891 einen Pfirsichkorb an eine Empore – zufaellig 3,05 Meter hoch."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Slam Dunk' im Basketball?",
        answerA = "Ein Dreier von ausserhalb",
        answerB = "Ein Freiwurf",
        answerC = "Ein Korbleger direkt von unten",
        answerD = "Ein Wurf, bei dem der Ball von oben direkt in den Korb gedrueckt wird",
        correctAnswer = 3,
        explanation = "Ein Slam Dunk ist ein Korbwurf, bei dem der Spieler springt und den Ball von oben kraftvoll direkt in den Korb stopft.",
        difficulty = 1,
        funFact = "Der Slam Dunk Contest der NBA existiert seit 1984 und gilt als eines der spektakulaersten Events im Profibasketball."
    ),

    Question(
        categoryId = 6,
        questionText = "Welchen Spitznamen traegt LeBron James?",
        answerA = "The Flash",
        answerB = "King James",
        answerC = "Air Jordan",
        answerD = "The Answer",
        correctAnswer = 1,
        explanation = "LeBron James traegt den Spitznamen 'King James' oder einfach 'The King' – ein Verweis auf seine dominante Stellung im Basketball.",
        difficulty = 1,
        funFact = "LeBron James wurde 2003 als 18-Jaehriger direkt von der High School an erster Stelle im NBA-Draft von den Cleveland Cavaliers ausgewaehlt."
    ),

    // ─── FORMEL 1 (5) ─────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "In welchem Land wird der Formel-1-Grand-Prix von Monaco ausgetragen?",
        answerA = "Frankreich",
        answerB = "Italien",
        answerC = "Monaco",
        answerD = "Schweiz",
        correctAnswer = 2,
        explanation = "Der Grand Prix von Monaco findet im Fuerstentum Monaco statt und gilt als einer der prestigetraechtigsten Laeufe der Formel 1.",
        difficulty = 1,
        funFact = "Der Grand Prix von Monaco wird seit 1929 ausgetragen und ist einer der wenigen Rennen, das noch immer auf oeffentlichen Strassen faehrt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst das bekannteste Formel-1-Team aus Italien?",
        answerA = "McLaren",
        answerB = "Red Bull Racing",
        answerC = "Scuderia Ferrari",
        answerD = "Renault",
        correctAnswer = 2,
        explanation = "Scuderia Ferrari ist das aelteste und beruemteste Team der Formel 1 und hat seinen Sitz in Maranello, Italien.",
        difficulty = 1,
        funFact = "Ferrari nahm an allen Formel-1-Weltmeisterschaften seit 1950 teil – als einziges Team ohne Unterbrechung."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Weltmeistertitel hat Michael Schumacher gewonnen?",
        answerA = "5",
        answerB = "6",
        answerC = "7",
        answerD = "8",
        correctAnswer = 2,
        explanation = "Michael Schumacher gewann insgesamt 7 Formel-1-Weltmeistertitel: 1994, 1995 und von 2000 bis 2004.",
        difficulty = 1,
        funFact = "Michael Schumacher teilt den Rekord von 7 Weltmeistertiteln mit Lewis Hamilton, der diesen 2020 egalisierte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Flagbe signalisiert dem Fahrer, dass das Rennen beendet ist?",
        answerA = "Rote Flagge",
        answerB = "Schwarz-weiss karierte Flagge",
        answerC = "Gelbe Flagge",
        answerD = "Gruene Flagge",
        correctAnswer = 1,
        explanation = "Die schwarz-weiss karierte Flagge signalisiert den Rennfahrern das Ende des Rennens – wer sie als erster sieht, hat gewonnen.",
        difficulty = 1,
        funFact = "Die Herkunft der karierten Flagge im Rennsport ist umstritten – sie wurde seit den 1900er Jahren verwendet."
    ),

    Question(
        categoryId = 6,
        questionText = "Aus welchem Land stammt Sebastian Vettel?",
        answerA = "Oesterreich",
        answerB = "Deutschland",
        answerC = "Schweiz",
        answerD = "Luxemburg",
        correctAnswer = 1,
        explanation = "Sebastian Vettel stammt aus Heppenheim, Deutschland, und wurde viermaliger Formel-1-Weltmeister (2010–2013).",
        difficulty = 1,
        funFact = "Sebastian Vettel war bei seinen vier Weltmeistertiteln das juengste dreifache Weltmeister der Formel-1-Geschichte."
    ),

    // ─── LEICHTATHLETIK (5) ───────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wer hielt den Weltrekord ueber 100 Meter der Maenner bis 2009?",
        answerA = "Carl Lewis",
        answerB = "Asafa Powell",
        answerC = "Justin Gatlin",
        answerD = "Tim Montgomery",
        correctAnswer = 1,
        explanation = "Asafa Powell aus Jamaika hielt den 100-Meter-Weltrekord mit 9,74 Sekunden, bevor Usain Bolt ihn 2008 unterbot.",
        difficulty = 1,
        funFact = "Asafa Powell stellte den Weltrekord ueber 100 Meter viermal in seiner Karriere auf und wurde dennoch nie Weltmeister."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Disziplinen umfasst der Zehnkampf bei Maennern?",
        answerA = "8",
        answerB = "10",
        answerC = "12",
        answerD = "7",
        correctAnswer = 1,
        explanation = "Der Zehnkampf besteht aus 10 Disziplinen: 100m, Weitsprung, Kugelstoessen, Hochsprung, 400m, 110m Huerden, Diskus, Stabhochsprung, Speer und 1500m.",
        difficulty = 1,
        funFact = "Das Aequivalent fuer Frauen heisst Siebenkampf und besteht aus 7 Disziplinen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart wird auch 'Koenig der Sportarten' genannt?",
        answerA = "Schwimmen",
        answerB = "Leichtathletik",
        answerC = "Fussball",
        answerD = "Turnen",
        correctAnswer = 1,
        explanation = "Leichtathletik wird oft als 'Koenig der Sportarten' bezeichnet, da sie die grundlegendsten menschlichen Bewegungsformen – Laufen, Springen, Werfen – vereint.",
        difficulty = 1,
        funFact = "Leichtathletik war seit den ersten modernen Olympischen Spielen 1896 immer Teil des olympischen Programms."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart beinhaltet das Werfen einer Scheibe?",
        answerA = "Hammerwurf",
        answerB = "Speerwurf",
        answerC = "Diskuswerfen",
        answerD = "Kugelstoessen",
        correctAnswer = 2,
        explanation = "Beim Diskuswerfen wird eine runde, flache Scheibe (Diskus) durch Rotation so weit wie moeglich geworfen.",
        difficulty = 1,
        funFact = "Der Diskus war bereits im antiken Griechenland eine olympische Disziplin – der werfende Diskuswerfer wurde von Myron in einer beruehmten Skulptur verewigt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man das Startgeraet, das Sprinter in der Leichtathletik benutzen?",
        answerA = "Startblock",
        answerB = "Startlinie",
        answerC = "Startbrett",
        answerD = "Absprungbalken",
        correctAnswer = 0,
        explanation = "Startbloecke sind Vorrichtungen, gegen die Sprinter ihre Fuesse stemmen, um einen kraftvollen Start zu erzielen.",
        difficulty = 1,
        funFact = "Startbloecke wurden erst 1937 im Profisport erlaubt – davor gruben Sprinter Loecher in den Boden."
    ),

    // ─── WINTERSPORT (5) ──────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "In welchem Wintersport gibt es eine 'Piste'?",
        answerA = "Biathlon",
        answerB = "Eisschnelllauf",
        answerC = "Skifahren",
        answerD = "Curling",
        correctAnswer = 2,
        explanation = "Eine Skipiste ist die praeparierte Abfahrtsstrecke fuer Skifahrer – sie wird von Pistenraupern geglättet und verdichtet.",
        difficulty = 1,
        funFact = "Weltcup-Abfahrtspisten haben Steilabschnitte von bis zu 70 % Neigung, auf denen Skifahrer ueber 130 km/h erreichen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land dominiert den Biathlonsport traditionell?",
        answerA = "Kanada",
        answerB = "Frankreich",
        answerC = "Norwegen",
        answerD = "Oesterreich",
        correctAnswer = 2,
        explanation = "Norwegen dominiert den Biathlon-Weltcup und die Olympischen Spiele seit Jahrzehnten mit Athleten wie Ole Einar Bjoerndalen.",
        difficulty = 1,
        funFact = "Ole Einar Bjoerndalen gewann 8 Olympia-Goldmedaillen im Biathlon und ist der erfolgreichste Wintersportler der Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der Unterschied zwischen Skating und klassischem Langlauf?",
        answerA = "Skating ist schneller, Klassisch nutzt parallele Skier",
        answerB = "Klassisch ist schneller, Skating nutzt breitere Ski",
        answerC = "Es gibt keinen Unterschied",
        answerD = "Skating benutzt Stocke, Klassisch nicht",
        correctAnswer = 0,
        explanation = "Beim Skating bewegen sich Laeufer aehnlich wie beim Schlittschuhlaufen mit seitlichen Abstoessen. Beim klassischen Stil laufen die Ski in parallelen Spuren.",
        difficulty = 1,
        funFact = "Der Skating-Stil wurde erst in den 1980er Jahren im Wettkampf zugelassen und verbreitete sich schnell wegen seiner hoeheren Geschwindigkeit."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Wintersportart schiesst man auf eine Zielscheibe?",
        answerA = "Slalom",
        answerB = "Skispringen",
        answerC = "Biathlon",
        answerD = "Bobfahren",
        correctAnswer = 2,
        explanation = "Biathlon verbindet Skilanglauf mit Schiessen – Athleten muessen an mehreren Schiessstaenden Zielscheiben treffen.",
        difficulty = 1,
        funFact = "Im Biathlon schiesst man auf 50 Meter entfernte Zielscheiben: im Stehendanschlag 11,5 cm und im Liegendanschlag 4,5 cm Durchmesser."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Slalom' im Skifahren?",
        answerA = "Eine Abfahrt durch aufgestellte Stangen (Tore)",
        answerB = "Eine Sprungdisziplin",
        answerC = "Ein Rennen auf der Geraden",
        answerD = "Eine Fahrt ohne Zeitnahme",
        correctAnswer = 0,
        explanation = "Slalom ist eine alpine Skidisziplin, bei der Fahrer eine enge Torenfolge aus roten und blauen Stangen meistern muessen.",
        difficulty = 1,
        funFact = "Der Begriff 'Slalom' stammt aus dem Norwegischen und bedeutet 'schraeger Weg' oder 'Schlangenweg'."
    ),

    // ─── SCHWIMMEN / WASSERSPORT (5) ──────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie lang ist ein olympisches Schwimmbecken?",
        answerA = "25 Meter",
        answerB = "50 Meter",
        answerC = "100 Meter",
        answerD = "75 Meter",
        correctAnswer = 1,
        explanation = "Ein olympisches Schwimmbecken ist exakt 50 Meter lang und 25 Meter breit mit 10 Bahnen.",
        difficulty = 1,
        funFact = "Im Schwimmen gibt es zwei Bahnlaengen fuer Weltrekorde: 25 m (Kurzbahn) und 50 m (Langbahn/olympisch)."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Schwimmart wird auch 'Froschart' genannt?",
        answerA = "Kraul",
        answerB = "Rueckenschwimmen",
        answerC = "Brustschwimmen",
        answerD = "Schmetterling",
        correctAnswer = 2,
        explanation = "Brustschwimmen wird wegen der froschaehnlichen Beinbewegung im Volksmund als 'Froschart' bezeichnet.",
        difficulty = 1,
        funFact = "Brustschwimmen ist technisch die komplizierteste Schwimmart und erfordert exaktes Timing zwischen Arm- und Beinbewegungen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man die Wand, an der Schwimmer umdrehen?",
        answerA = "Abstuetzplatte",
        answerB = "Wendewand",
        answerC = "Startblock",
        answerD = "Umkehrpunkt",
        correctAnswer = 1,
        explanation = "Die Wendewand ist der Endpunkt des Beckens, an dem Schwimmer abzutauchen, abzustossen oder umzudrehen.",
        difficulty = 1,
        funFact = "Weltklasse-Schwimmer benoetigen fuer eine Wende im Kraulstil oft weniger als 0,3 Sekunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Wassersport wird auf einem Brett mit einem Segel betrieben?",
        answerA = "Wasserskifahren",
        answerB = "Surfen",
        answerC = "Windsurfen",
        answerD = "Kajakfahren",
        correctAnswer = 2,
        explanation = "Windsurfen verbindet Surfen und Segeln – der Sportler steht auf einem Brett und steuert mit einem beweglichen Segel.",
        difficulty = 1,
        funFact = "Windsurfen ist seit 1984 olympisch und erfordert ein aussergewoehnliches Gleichgewicht und Gefuehl fuer den Wind."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist eine 'Staffel' im Schwimmen?",
        answerA = "Ein Einzelwettbewerb ueber vier Strecken",
        answerB = "Ein Teamwettbewerb, bei dem vier Schwimmer je eine Strecke schwimmen",
        answerC = "Ein Sprint ueber 25 Meter",
        answerD = "Eine Disziplin nur fuer Frauen",
        correctAnswer = 1,
        explanation = "Bei einer Staffel schwimmt jeder der vier Teammitglieder eine festgelegte Strecke nacheinander – aehnlich wie die Staffel in der Leichtathletik.",
        difficulty = 1,
        funFact = "Die 4x100m-Freistilstaffel der USA gewann bei fast jeder Olympiade seit 1964 eine Medaille."
    ),
)
