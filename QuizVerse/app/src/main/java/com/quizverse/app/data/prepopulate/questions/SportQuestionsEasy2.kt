package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsEasy2(): List<Question> = listOf(

    // ─── HANDBALL (7) ─────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler hat eine Handballmannschaft auf dem Feld?",
        answerA = "5",
        answerB = "6",
        answerC = "7",
        answerD = "8",
        correctAnswer = 2,
        explanation = "Eine Handballmannschaft spielt mit 7 Feldspielern – sechs Feldspieler und ein Torhueter.",
        difficulty = 1,
        funFact = "Handball wurde 1919 in Deutschland entwickelt und ist heute besonders in Europa sehr beliebt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lange dauert ein Handballspiel bei Erwachsenen?",
        answerA = "60 Minuten (2 x 30)",
        answerB = "70 Minuten (2 x 35)",
        answerC = "80 Minuten (2 x 40)",
        answerD = "90 Minuten (2 x 45)",
        correctAnswer = 0,
        explanation = "Ein Handballspiel bei Erwachsenen dauert 60 Minuten, aufgeteilt in zwei Halbzeiten zu je 30 Minuten.",
        difficulty = 1,
        funFact = "Bei Unentschieden in K.-o.-Spielen gibt es zwei Verlaengerungen von je 5 Minuten, danach entscheidet das Siebenmeterwerfern."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie gross ist ein Handballfeld?",
        answerA = "30 m x 15 m",
        answerB = "40 m x 20 m",
        answerC = "50 m x 25 m",
        answerD = "60 m x 30 m",
        correctAnswer = 1,
        explanation = "Ein offizielles Handballfeld ist 40 Meter lang und 20 Meter breit.",
        difficulty = 1,
        funFact = "Urspruenglich wurde Handball im Freien auf einem Fussballfeld gespielt – erst spaeter zog er in die Halle."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Siebenmeter' im Handball?",
        answerA = "Ein Freiwurf aus sieben Metern Entfernung",
        answerB = "Ein Strafwurf direkt auf das Tor aus sieben Metern",
        answerC = "Eine Regel fuer Jugendspieler",
        answerD = "Ein Tor, das siebenmal geworfen werden darf",
        correctAnswer = 1,
        explanation = "Der Siebenmeter ist ein Strafwurf, der aus sieben Metern Entfernung direkt auf das Tor ausgefuehrt wird – vergleichbar mit dem Elfmeter im Fussball.",
        difficulty = 1,
        funFact = "Bei einem Siebenmeter ist der Torhueter der einzige Verteidiger – alle anderen Spieler muessen zurueckbleiben."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation gewann die Handball-Weltmeisterschaft 2019?",
        answerA = "Frankreich",
        answerB = "Spanien",
        answerC = "Deutschland",
        answerD = "Daenemark",
        correctAnswer = 3,
        explanation = "Daenemark gewann 2019 auf heimischem Boden die Handball-Weltmeisterschaft und setzte sich damit weltwaeit an die Spitze.",
        difficulty = 1,
        funFact = "Daenemark ist auch amtierender Olympiasieger und gilt als eine der staerksten Handballnationen der Welt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie gross ist ein Handballtor?",
        answerA = "2 m hoch, 3 m breit",
        answerB = "2 m hoch, 4 m breit",
        answerC = "2,44 m hoch, 3 m breit",
        answerD = "1,5 m hoch, 3 m breit",
        correctAnswer = 0,
        explanation = "Ein Handballtor ist 2 Meter hoch und 3 Meter breit – deutlich kleiner als ein Fussballtor.",
        difficulty = 1,
        funFact = "Der Torraum im Handball hat einen Radius von 6 Metern – Feldspieler duerfen diesen nicht betreten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Handball-Bundesligist ist Rekordsmeister?",
        answerA = "THW Kiel",
        answerB = "SG Flensburg-Handewitt",
        answerC = "Rhein-Neckar Loewen",
        answerD = "TBV Lemgo Lippe",
        correctAnswer = 0,
        explanation = "THW Kiel ist mit zahlreichen Meistertiteln Rekordsmeister der Handball-Bundesliga und auch mehrfacher Champions-League-Sieger.",
        difficulty = 1,
        funFact = "THW Kiel wird wegen seiner dominanten Geschichte als 'Zebras' bezeichnet – ein Spitzname der auf das schwarz-weisse Trikot zurueckgeht."
    ),

    // ─── TISCHTENNIS / BADMINTON (7) ──────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie viele Punkte braucht man im Tischtennis, um einen Satz zu gewinnen?",
        answerA = "11",
        answerB = "15",
        answerC = "21",
        answerD = "25",
        correctAnswer = 0,
        explanation = "Seit 2001 wird Tischtennis auf 11 Punkte pro Satz gespielt – vorher waren es 21 Punkte.",
        difficulty = 1,
        funFact = "Die Umstellung von 21 auf 11 Punkte machte die Spiele kuerzer und dynamischer fuer TV-Uebertragungen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation dominiert den Tischtennis-Sport weltweit?",
        answerA = "Japan",
        answerB = "Suedkorea",
        answerC = "China",
        answerD = "Deutschland",
        correctAnswer = 2,
        explanation = "China dominiert den Tischtennis-Sport seit Jahrzehnten und gewinnt bei fast jeder Weltmeisterschaft und Olympiade die meisten Titel.",
        difficulty = 1,
        funFact = "Deutschland ist fuer einen europaeischen Verband aussergewoehnlich stark im Tischtennis – Timo Boll war jahrelang Weltranglistenerster."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Federball' im Badminton?",
        answerA = "Ein Ball aus Federn und Gummi",
        answerB = "Der offizielle Name fuer einen Badminton-Schlaeger",
        answerC = "Ein kegelfoermiges Wurfgeraet mit Federn (Shuttlecock)",
        answerD = "Ein leichter Plastikball",
        correctAnswer = 2,
        explanation = "Im Badminton wird kein runder Ball gespielt, sondern ein Shuttlecock – ein konischer Gegenstand mit Federkranz, auch Federball genannt.",
        difficulty = 1,
        funFact = "Echte Shuttlecocks bestehen aus 16 Gansefedern – auf hohem Wettkampfniveau werden keine Kunststoffversionen verwendet."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie hoch ist das Netz beim Badminton in der Mitte?",
        answerA = "1,24 m",
        answerB = "1,55 m",
        answerC = "1,00 m",
        answerD = "0,76 m",
        correctAnswer = 3,
        explanation = "Das Badmintonnetz haengt in der Mitte exakt 1,524 m (ca. 5 Fuss) hoch – an den Pfosten sind es 1,55 m.",
        difficulty = 1,
        funFact = "Ein Badminton-Shuttlecock kann beim Smash Geschwindigkeiten von ueber 400 km/h erreichen – die schnellste Bewegung in einer Racketsportart."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Sätze werden bei einem olympischen Badminton-Einzel gespielt?",
        answerA = "1 Gewinnsatz",
        answerB = "Best of 3 (maximal 3 Saetze)",
        answerC = "Best of 5 (maximal 5 Saetze)",
        answerD = "Immer genau 2 Saetze",
        correctAnswer = 1,
        explanation = "Im Badminton wird nach dem Best-of-3-Modus gespielt – wer zuerst zwei Saetze gewinnt (je 21 Punkte), gewinnt das Match.",
        difficulty = 1,
        funFact = "Badminton ist nach Fussball die beliebteste Teamsportart der Welt – besonders in Asien hat sie Millionen aktiver Spieler."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst der beruemteste deutsche Tischtennis-Spieler?",
        answerA = "Jan-Ove Waldner",
        answerB = "Timo Boll",
        answerC = "Joern Waldner",
        answerD = "Peter Karlsson",
        correctAnswer = 1,
        explanation = "Timo Boll ist der beruemteste deutsche Tischtennisspieler – er war mehrfach Weltranglistenerster und Europaameister.",
        difficulty = 1,
        funFact = "Timo Boll nahm an sechs Olympischen Spielen teil und gilt als einer der besten europaeischen Tischtennisspieler aller Zeiten."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie klein ist ein offizieller Tischtennisball?",
        answerA = "38 mm Durchmesser",
        answerB = "40 mm Durchmesser",
        answerC = "44 mm Durchmesser",
        answerD = "50 mm Durchmesser",
        correctAnswer = 1,
        explanation = "Seit 2000 werden Tischtennisbaelle mit einem Durchmesser von 40 mm verwendet – davor waren es 38 mm.",
        difficulty = 1,
        funFact = "Tischtennisbaelle wurden 2015 von Zelluloid auf Kunststoff (Poly) umgestellt – was das Spielverhalten leicht veraenderte."
    ),

    // ─── BOXEN (7) ────────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie lange dauert eine Runde im Profi-Boxen?",
        answerA = "2 Minuten",
        answerB = "3 Minuten",
        answerC = "4 Minuten",
        answerD = "5 Minuten",
        correctAnswer = 1,
        explanation = "Eine Runde im Profiboxen dauert 3 Minuten – dazwischen gibt es eine Pause von 1 Minute.",
        difficulty = 1,
        funFact = "Im Frauen-Profiboxen dauern Runden oft nur 2 Minuten."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man den Bereich, in dem geboxt wird?",
        answerA = "Arena",
        answerB = "Ring",
        answerC = "Feld",
        answerD = "Court",
        correctAnswer = 1,
        explanation = "Der Boxring ist das quadratische Kampffeld mit Seilen an den vier Seiten – trotz des Namens ist er viereckig, nicht rund.",
        difficulty = 1,
        funFact = "Der Begriff 'Ring' stammt aus frueher Zeit, als Kaempfe in einem von Zuschauern gebildeten Kreis ausgetragen wurden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Gewichtsklasse ist die schwerste im Boxen?",
        answerA = "Halbschwergewicht",
        answerB = "Schwergewicht",
        answerC = "Superschwergewicht",
        answerD = "Cruisergewicht",
        correctAnswer = 2,
        explanation = "Das Superschwergewicht (auch Schwergewicht+ genannt) ist die schwerste Klasse im Amateurboxen – im Profibereich ist das klassische Schwergewicht oft die hoechste Kategorie.",
        difficulty = 1,
        funFact = "Muhammad Ali kaempfte im Schwergewicht und gilt als einer der groessten Boxer aller Zeiten."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet 'K.o.' im Boxen?",
        answerA = "Kampf offen",
        answerB = "Knock-out – der Gegner kann nicht weiterkämpfen",
        answerC = "Keine Obstruktion",
        answerD = "Kampf ohne Wertung",
        correctAnswer = 1,
        explanation = "K.o. steht fuer Knock-out: Der Boxer geht zu Boden und kann sich nicht innerhalb von 10 Sekunden erheben – das Spiel ist beendet.",
        difficulty = 1,
        funFact = "Den klaersten K.o. der WM-Geschichte erzielte Mike Tyson 1988 gegen Michael Spinks – der Kampf dauerte nur 91 Sekunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Boxer traegt den Spitznamen 'The Greatest'?",
        answerA = "Mike Tyson",
        answerB = "Muhammad Ali",
        answerC = "Joe Frazier",
        answerD = "George Foreman",
        correctAnswer = 1,
        explanation = "Muhammad Ali nannte sich selbst 'The Greatest' – er gilt weithin als der beste Schwergewichtsboxer der Geschichte.",
        difficulty = 1,
        funFact = "Ali war bekannt fuer seinen Ausdruck 'Float like a butterfly, sting like a bee' – ein Hinweis auf seine Schnelligkeit und harten Schlaege."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Runden hat ein WM-Kampf im Profi-Boxen?",
        answerA = "8",
        answerB = "10",
        answerC = "12",
        answerD = "15",
        correctAnswer = 2,
        explanation = "WM-Kaempfe im Profiboxen gehen seit 1983 maximal 12 Runden – zuvor waren es 15 Runden.",
        difficulty = 1,
        funFact = "Die Reduzierung auf 12 Runden wurde eingefuehrt, nachdem 1982 der Boxer Duk-koo Kim nach einem 14-Runden-Kampf an Hirnverletzungen starb."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Jab' im Boxen?",
        answerA = "Eine kraftvolle Aufwaertsrechte",
        answerB = "Ein schneller, gerader Schlag mit der fuehrenden Hand",
        answerC = "Ein Haken von der Seite",
        answerD = "Ein Verteidigungsmanover",
        correctAnswer = 1,
        explanation = "Der Jab ist ein schneller, gerader Schlag mit der fuehrenden (schwaecher dominanten) Hand – er dient zur Distanzkontrolle und Eroeffnung von Kombinationen.",
        difficulty = 1,
        funFact = "Der Jab ist die am haeufigsten eingesetzte Technik im Boxen – selbst die groessten Kraftboxer nutzen ihn als Fundament ihrer Taktik."
    ),

    // ─── GOLF (6) ─────────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie nennt man es, wenn ein Golfer das Loch in der Vorgabeanzahl an Schlaegen spielt?",
        answerA = "Birdie",
        answerB = "Eagle",
        answerC = "Par",
        answerD = "Bogey",
        correctAnswer = 2,
        explanation = "Par bedeutet, das Loch mit der vorgesehenen Anzahl von Schlaegen zu beenden – bei einem Par-4-Loch also in 4 Schlaegen.",
        difficulty = 1,
        funFact = "Der Begriff 'Par' kommt vom lateinischen 'par' – was 'gleich' bedeutet."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Hole-in-One' beim Golf?",
        answerA = "Ein Loch mit nur einem Hindernis",
        answerB = "Das Loch direkt vom Abschlag mit einem Schlag versenken",
        answerC = "Ein Spezialschlaeger",
        answerD = "Eine Sonderregel auf Kurzloechern",
        correctAnswer = 1,
        explanation = "Ein Hole-in-One (Ass) bedeutet, den Ball mit dem ersten Schlag direkt vom Abschlag ins Loch zu spielen.",
        difficulty = 1,
        funFact = "Die statistische Wahrscheinlichkeit eines Hole-in-One fuer einen Amateurgolfer liegt bei etwa 1 zu 12.500."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Loecher hat ein regulaerer Golfplatz?",
        answerA = "9",
        answerB = "12",
        answerC = "18",
        answerD = "24",
        correctAnswer = 2,
        explanation = "Ein regulaerer Golfplatz hat 18 Loecher – es gibt auch 9-Loch-Plaetze, aber 18 ist die internationale Norm.",
        difficulty = 1,
        funFact = "Der St-Andrews-Golfplatz in Schottland gilt als die Wiege des Golfsports und hat ebenfalls 18 Loecher."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Golfer gewann die meisten Major-Turniere?",
        answerA = "Arnold Palmer",
        answerB = "Tiger Woods",
        answerC = "Jack Nicklaus",
        answerD = "Phil Mickelson",
        correctAnswer = 2,
        explanation = "Jack Nicklaus gewann insgesamt 18 Major-Turniere und gilt als der erfolgreichste Golfer der Geschichte.",
        difficulty = 1,
        funFact = "Tiger Woods gewann 15 Majors und jagte lange den Rekord von Jack Nicklaus – er steht auf Platz 2 der ewigen Bestenliste."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst der beruemteste Golfschlag mit einem einfachen langen Schlaeger vom Abschlag?",
        answerA = "Putt",
        answerB = "Drive",
        answerC = "Chip",
        answerD = "Pitch",
        correctAnswer = 1,
        explanation = "Der Drive ist der Abschlag mit dem Driver (Holz 1) – dem laengsten Schlaeger – um moeglichst viel Entfernung zu erzielen.",
        difficulty = 1,
        funFact = "Profi-Golfer schaffen mit dem Drive Weiten von ueber 300 Metern – der Weltrekord liegt bei ueber 400 Metern."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler hat eine Handball-Mannschaft auf dem Feld (inklusive Torwart)?",
        answerA = "5",
        answerB = "6",
        answerC = "7",
        answerD = "8",
        correctAnswer = 2,
        explanation = "Eine Handball-Mannschaft besteht aus 7 Spielern auf dem Feld: 6 Feldspieler und 1 Torwart. Insgesamt duerfen bis zu 14 Spieler im Kader stehen.",
        difficulty = 1,
        funFact = "Handball wurde 1936 in Berlin zum ersten Mal als olympische Sportart gespielt – damals noch als Feldhandball mit 11 Spielern pro Team."
    ),

    // ─── REITSPORT (6) ────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welche drei Disziplinen gehoeren zur Reiterlichen Vereinigung (FEI) in der Olympiadreikampf?",
        answerA = "Dressur, Springreiten, Vielseitigkeit",
        answerB = "Dressur, Polo, Gelaenderennen",
        answerC = "Springreiten, Rennen, Voltigieren",
        answerD = "Westernreiten, Dressur, Polo",
        correctAnswer = 0,
        explanation = "Die drei olympischen Reitsportdisziplinen sind Dressur, Springreiten (Parcours) und Vielseitigkeit (Military/Eventing).",
        difficulty = 1,
        funFact = "Reiten ist eine der wenigen olympischen Sportarten, bei denen Frauen und Maenner gleichberechtigt gegeneinander antreten."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Dressurreiten?",
        answerA = "Schnellstmoegliches Reiten durch einen Parcours",
        answerB = "Kunstvolle Ausfuehrung von Reitfiguren nach einem Programm",
        answerC = "Reiten ueber Hindernisse",
        answerD = "Galopprennen ueber lange Strecken",
        correctAnswer = 1,
        explanation = "Dressurreiten ist die kunstvollste Reitdisziplin – Pferd und Reiter fuehren ein Pflichtprogramm aus eleganten Bewegungsfolgen vor.",
        difficulty = 1,
        funFact = "Die Piaffe (Trab auf der Stelle) und die Pirouette (Kreisbewegung auf dem Hinterhand) sind die spektakulaersten Dressurfiguren."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man die groesste Galopprennen-Veranstaltung in Deutschland?",
        answerA = "Deutsches Derby Hamburg",
        answerB = "Bayern-Rennen Muenchen",
        answerC = "Rhein-Cup Duesseldorf",
        answerD = "Grosser Preis von Berlin",
        correctAnswer = 0,
        explanation = "Das Deutsche Derby in Hamburg ist das bedeutendste und traditionsreichste Galopprennen in Deutschland, erstmals ausgetragen 1869.",
        difficulty = 1,
        funFact = "Das Deutsche Derby findet traditionell Anfang Juli statt und zieht jaehrlich Zehntausende Besucher auf die Galopprennbahn Hamburg-Horn."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Zubehoer schreibt man dem Pferdesattel zum Fuessen des Reiters zu?",
        answerA = "Steigbuegel",
        answerB = "Trense",
        answerC = "Martingal",
        answerD = "Longe",
        correctAnswer = 0,
        explanation = "Steigbuegel sind die Metallbuegel, die am Sattel haengen und dem Reiter als Fussauflage und Stabilisierung dienen.",
        difficulty = 1,
        funFact = "Steigbuegel wurden erst um das 5. Jahrhundert n. Chr. erfunden und veraenderten die Kriegsfuehrung zu Pferde revolutionaer."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man einen Fehler beim Springreiten, wenn das Pferd eine Stange abwirft?",
        answerA = "Abwurf",
        answerB = "Fehler",
        answerC = "Refusal",
        answerD = "Abzug",
        correctAnswer = 1,
        explanation = "Wirft das Pferd beim Springreiten eine Stange ab, gibt es 4 Strafpunkte – im Turnierjargon nennt man dies schlicht einen 'Fehler'.",
        difficulty = 1,
        funFact = "Im internationalen Parcoursspringen haben Pferd und Reiter meist 90 Sekunden Zeit, um den Kurs ohne Fehler zu bewaeltigen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst die schnellste Gangart eines Pferdes?",
        answerA = "Trab",
        answerB = "Schritt",
        answerC = "Galopp",
        answerD = "Kanter",
        correctAnswer = 2,
        explanation = "Galopp ist die schnellste natuerliche Gangart des Pferdes – Rennpferde erreichen dabei Geschwindigkeiten von ueber 60 km/h.",
        difficulty = 1,
        funFact = "Der Weltrekord fuer das schnellste Galopprennen-Pferd liegt bei 70,76 km/h – aufgestellt von Winning Brew im Jahr 2008."
    ),

    // ─── GYMNASTIK / TURNEN (6) ───────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie viele Turner-Disziplinen gibt es im Maenner-Kunstturnen bei Olympia?",
        answerA = "4",
        answerB = "6",
        answerC = "8",
        answerD = "10",
        correctAnswer = 1,
        explanation = "Im Maenner-Kunstturnen gibt es 6 Geraete: Boden, Pauschenpferd, Ringe, Sprung, Barren und Reck.",
        difficulty = 1,
        funFact = "Frauen turnen im Kunstturnen an 4 Geraeten: Sprung, Stufenbarren, Schwebebalken und Boden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land ist traditionell im Kunstturnen am erfolgreichsten?",
        answerA = "Deutschland",
        answerB = "USA",
        answerC = "Sowjetunion / Russland",
        answerD = "Japan",
        correctAnswer = 2,
        explanation = "Die Sowjetunion (spaeter Russland) dominierte das Kunstturnen jahrzehntelang – heute sind die USA und China die staerksten Nationen.",
        difficulty = 1,
        funFact = "Nadia Comaneci aus Rumaenien bekam 1976 in Montreal die erste perfekte 10,0 in der olympischen Turngeschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Rhythmische Sportgymnastik'?",
        answerA = "Turnen an festen Geraeten wie Reck und Barren",
        answerB = "Gruppenturnen mit Musik und Handgeraeten wie Band oder Reifen",
        answerC = "Eine Kampfsportart mit Turnschuhen",
        answerD = "Akrobatik auf dem Trampolin",
        correctAnswer = 1,
        explanation = "Die Rhythmische Sportgymnastik kombiniert Tanzelemente, akrobatische Bewegungen und Handgeraete (Ball, Band, Reifen, Keule) zur Musik.",
        difficulty = 1,
        funFact = "Rhythmische Sportgymnastik ist seit 1984 olympisch und wird ausschliesslich von Frauen betrieben."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst eine bekannte deutsche Turnerin, die bei Olympia erfolgreich war?",
        answerA = "Elisabeth Seitz",
        answerB = "Magdalena Neuner",
        answerC = "Katharina Witt",
        answerD = "Franziska van Almsick",
        correctAnswer = 0,
        explanation = "Elisabeth Seitz ist eine der bekanntesten deutschen Kunstturnerinnen – sie ist mehrfache Europameisterin am Stufenbarren.",
        difficulty = 1,
        funFact = "Elisabeth Seitz ist fuer ihre Stufenbarren-Elemente bekannt, von denen eines sogar nach ihr benannt ist – das 'Seitz-Element'."
    ),

    Question(
        categoryId = 6,
        questionText = "Auf welchem Geraet fuehren Turner den 'Riesenaufschwung' aus?",
        answerA = "Pauschenpferd",
        answerB = "Barren",
        answerC = "Reck",
        answerD = "Ringe",
        correctAnswer = 2,
        explanation = "Der Riesenaufschwung (auch Riesenwelle) ist eine vollstaendige 360-Grad-Rotation am Reck – eine Grundvoraussetzung fuer fortgeschrittene Uebungen.",
        difficulty = 1,
        funFact = "Moderne Reck-Kueruebungen erreichen Hoehen von mehreren Metern ueber dem Geraet – ein Sicherheitsnetz ist bei Trainingseinheiten Pflicht."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Handstand' in der Turnsprache?",
        answerA = "Stehen auf einer Hand",
        answerB = "Stehen auf beiden Haenden mit dem Koerper senkrecht nach oben",
        answerC = "Ein Sprung mit halber Drehung",
        answerD = "Ein Griff am Reck mit beiden Haenden",
        correctAnswer = 1,
        explanation = "Im Handstand steht der Turner auf beiden Haenden mit dem Koerper vollstaendig senkrecht nach oben – ein Grundelement des Turnens.",
        difficulty = 1,
        funFact = "Professionelle Turner koennen einen perfekten Handstand mehrere Minuten lang halten."
    ),

    // ─── EISHOCKEY (6) ────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler hat eine Eishockeymannschaft auf dem Eis (einschliesslich Torhueter)?",
        answerA = "5",
        answerB = "6",
        answerC = "7",
        answerD = "8",
        correctAnswer = 1,
        explanation = "Jede Eishockeymannschaft spielt mit 6 Spielern: 5 Feldspieler und einem Torhueter.",
        difficulty = 1,
        funFact = "Beim Eishockey darf ein Team bis zu 20 Spieler nominieren, aber immer nur 6 gleichzeitig auf dem Eis haben."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst der beruemteste Eishockey-Pokal der Welt?",
        answerA = "Champions Trophy",
        answerB = "Stanley Cup",
        answerC = "World Cup",
        answerD = "Presidents Trophy",
        correctAnswer = 1,
        explanation = "Der Stanley Cup ist der begehrteste Preis im Eishockey – er wird jaehrlich in der NHL vergeben und seit 1893 gespielt.",
        difficulty = 1,
        funFact = "Der Stanley Cup ist einer der aeltesten Sportpokale der Welt – er wurde von Lord Stanley of Preston gestiftet und ist heute ueber ein Meter hoch."
    ),

    Question(
        categoryId = 6,
        questionText = "Aus welchem Material besteht eine Eishockey-Puck?",
        answerA = "Leder",
        answerB = "Kunststoff",
        answerC = "Vulkanisierter Gummi",
        answerD = "Metall",
        correctAnswer = 2,
        explanation = "Eine Eishockey-Puck besteht aus vulkanisiertem Hartgummi und wiegt zwischen 156 und 170 Gramm.",
        difficulty = 1,
        funFact = "Pucks werden vor dem Spiel eingefroren, damit sie weniger springen und besser auf dem Eis gleiten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Liga gilt als die beste Eishockey-Liga der Welt?",
        answerA = "KHL (Kontinental Hockey League)",
        answerB = "NHL (National Hockey League)",
        answerC = "DEL (Deutsche Eishockey Liga)",
        answerD = "SHL (Swedish Hockey League)",
        correctAnswer = 1,
        explanation = "Die NHL in Nordamerika gilt als die beste und attraktivste Eishockeyliga der Welt – hier spielen die besten Profis aus aller Welt.",
        difficulty = 1,
        funFact = "Die NHL wurde 1917 in Kanada gegruendet und zaehlt heute 32 Teams aus Kanada und den USA."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist eine 'Strafe' (Penalty) im Eishockey?",
        answerA = "Eine Zeitstrafe, bei der ein Spieler fuer 2 Minuten vom Eis muss",
        answerB = "Ein direkter Schuss auf das Tor ohne Verteidiger",
        answerC = "Ein Freischuss nach einem Foul im Torraum",
        answerD = "Ein Punkt Abzug fuer Unsportlichkeit",
        correctAnswer = 0,
        explanation = "Bei einem Regelverstoss erhält ein Spieler meist eine 2-Minuten-Strafe – sein Team spielt waehrenddessen in Unterzahl.",
        difficulty = 1,
        funFact = "Wenn ein Team in Unterzahl spielt, heisst das 'Penalty Killing' – die groesste Kunst ist es, in dieser Zeit kein Gegentor zu kassieren."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst die bekannteste Nationalhymne, die beim Eishockey gesungen wird – und von welchem Land?",
        answerA = "O Canada – Kanada",
        answerB = "The Star-Spangled Banner – USA",
        answerC = "God Save the King – England",
        answerD = "La Marseillaise – Frankreich",
        correctAnswer = 0,
        explanation = "Kanada ist die Eishockey-Nation Nummer eins – 'O Canada' ist die Hymne, die bei internationalen Spielen am leidenschaftlichsten mitgesungen wird.",
        difficulty = 1,
        funFact = "Eishockey ist in Kanada offiziell Nationalsport – zusammen mit Lacrosse, das im Sommer diese Rolle einnimmt."
    ),

    // ─── VOLLEYBALL (5) ───────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler hat eine Volleyballmannschaft auf dem Feld?",
        answerA = "4",
        answerB = "5",
        answerC = "6",
        answerD = "7",
        correctAnswer = 2,
        explanation = "Eine Volleyballmannschaft spielt mit 6 Spielern auf dem Feld – drei vorn und drei hinten.",
        difficulty = 1,
        funFact = "Volleyball wurde 1895 von William Morgan in den USA erfunden – urspruenglich als weniger koerperkontaktreicher Sport als Basketball."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Beruehrungen darf ein Team im Volleyball pro Ballwechsel haben?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "5",
        correctAnswer = 1,
        explanation = "Pro Ballwechsel darf eine Mannschaft den Ball maximal dreimal beruehren, bevor er ueber das Netz gespielt werden muss.",
        difficulty = 1,
        funFact = "Die drei typischen Aktionen sind: Annahme, Zuspiel und Angriff – fachlich: Bagger, Zuspiel und Schmetterball."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Ass' im Volleyball?",
        answerA = "Ein Block, der direkt zum Punkt fuehrt",
        answerB = "Ein Aufschlag, den der Gegner nicht annehmen kann",
        answerC = "Ein Schmetterball aus zwei Metern Hoehe",
        answerD = "Ein Fehler des Gegners beim Aufschlag",
        correctAnswer = 1,
        explanation = "Ein Aufschlag-Ass (Service Ace) ist ein Aufschlag, der die gegnerische Mannschaft so ueberrascht, dass sie den Ball nicht mehr annehmen kann.",
        difficulty = 1,
        funFact = "Im Beach-Volleyball ist das Ass besonders wertvoll, da es den Spieler auf den gesamten Halbfeldabdeckung beschraenkt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie hoch ist das Netz beim Maenner-Volleyball?",
        answerA = "2,14 m",
        answerB = "2,24 m",
        answerC = "2,43 m",
        answerD = "2,55 m",
        correctAnswer = 2,
        explanation = "Das Volleyball-Netz ist bei Maennern 2,43 Meter hoch – bei Frauen sind es 2,24 Meter.",
        difficulty = 1,
        funFact = "Profi-Volleyballer koennen bis zu 3,70 Meter hoch springen und schlagen den Ball weit ueber das Netz."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Beach-Volleyball im Vergleich zu Hallenvolleyball?",
        answerA = "Wird in einer Halle auf Sand gespielt",
        answerB = "Wird zu zweit im Freien auf Sand gespielt",
        answerC = "Ist eine Version mit 4 Spielern auf jeder Seite",
        answerD = "Ist eine Variante nur fuer Kinder",
        correctAnswer = 1,
        explanation = "Beach-Volleyball wird im Freien auf Sand gespielt – jedes Team hat nur zwei Spieler, und es gelten leicht abgewandelte Regeln.",
        difficulty = 1,
        funFact = "Beach-Volleyball wurde 1996 in Atlanta erstmals olympisch – mittlerweile zaehlt es zu den beliebtesten Sommerspielen-Sportarten."
    ),
)
