package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsMedium(): List<Question> = listOf(

    // --- Fussball-WM/EM (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr gewann Deutschland seine erste Fussball-Weltmeisterschaft?",
        answerA = "1950",
        answerB = "1954",
        answerC = "1958",
        answerD = "1966",
        correctAnswer = 1,
        explanation = "Deutschland (BRD) gewann 1954 in der Schweiz die WM. Das sogenannte Wunder von Bern: 3:2-Sieg im Finale gegen Ungarn.",
        difficulty = 2,
        funFact = "Der damalige Bundestrainer Sepp Herberger wahlte die Mannschaft entgegen aller Erwartungen und fuehrte sie zum Titel."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Tore erzielte Miroslav Klose insgesamt bei Fussball-Weltmeisterschaften?",
        answerA = "14",
        answerB = "15",
        answerC = "16",
        answerD = "17",
        correctAnswer = 2,
        explanation = "Miroslav Klose erzielte in vier WM-Turnieren (2002, 2006, 2010, 2014) insgesamt 16 Tore und ist damit WM-Rekordtorschuetze.",
        difficulty = 2,
        funFact = "Kloses letztes WM-Tor fiel beim 7:1-Sieg Deutschlands gegen Brasilien im Halbfinale 2014."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land war Gastgeber der Fussball-WM 1966?",
        answerA = "Frankreich",
        answerB = "Spanien",
        answerC = "England",
        answerD = "Italien",
        correctAnswer = 2,
        explanation = "England richtete die WM 1966 aus und gewann sie auch – 4:2 im Finale gegen Deutschland nach Verlaengerung.",
        difficulty = 2,
        funFact = "Das Wembley-Tor war eines der umstrittensten Tore der WM-Geschichte: Der Ball traf die Latte und sprang auf oder hinter die Linie."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welcher Fussball-EM gewann Griechenland ueberraschend den Titel?",
        answerA = "EM 1996",
        answerB = "EM 2000",
        answerC = "EM 2004",
        answerD = "EM 2008",
        correctAnswer = 2,
        explanation = "Griechenland gewann die EM 2004 in Portugal als krasser Aussenseiter. Im Finale besiegten sie den Gastgeber Portugal mit 1:0.",
        difficulty = 2,
        funFact = "Otto Rehhagel, ein deutscher Trainer, fuehrte Griechenland zum Titel und wurde dafuer in Griechenland zum Nationalhelden."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Mannschaften nehmen seit 2016 an der Fussball-Europameisterschaft teil?",
        answerA = "16",
        answerB = "20",
        answerC = "24",
        answerD = "32",
        correctAnswer = 2,
        explanation = "Seit der EM 2016 in Frankreich nehmen 24 Mannschaften teil – vorher waren es 16.",
        difficulty = 2,
        funFact = "Durch die Aufstockung konnten erstmalig Laender wie Island und Albanien an einer EM teilnehmen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land hat die meisten Fussball-WM-Titel gewonnen?",
        answerA = "Deutschland",
        answerB = "Argentinien",
        answerC = "Brasilien",
        answerD = "Italien",
        correctAnswer = 2,
        explanation = "Brasilien hat mit 5 WM-Titeln (1958, 1962, 1970, 1994, 2002) die meisten Weltmeisterschaften gewonnen.",
        difficulty = 2,
        funFact = "Brasilien ist das einzige Land, das an jeder Fussball-Weltmeisterschaft teilgenommen hat."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Stadt fand das legendaere WM-Finale 1950 in der Maracana statt?",
        answerA = "Sao Paulo",
        answerB = "Buenos Aires",
        answerC = "Rio de Janeiro",
        answerD = "Montevideo",
        correctAnswer = 2,
        explanation = "Das Endspiel der WM 1950 fand im Maracana-Stadion in Rio de Janeiro statt. Uruguay besiegte Gastgeber Brasilien 2:1.",
        difficulty = 2,
        funFact = "Das sogenannte Maracanazo – Uruguays Sieg vor 200.000 Zuschauern – gilt als eine der groessten Upset-Niederlagen der Sportgeschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Spieler wurde bei der WM 2006 in Deutschland fuer seine Kopfstoss-Aktion gegen Marco Materazzi verwiesen?",
        answerA = "Ronaldinho",
        answerB = "Zinedine Zidane",
        answerC = "Thierry Henry",
        answerD = "Patrick Vieira",
        correctAnswer = 1,
        explanation = "Zinedine Zidane bekam im WM-Finale 2006 die Rote Karte, nachdem er Marco Materazzi mit dem Kopf in die Brust gestossen hatte.",
        difficulty = 2,
        funFact = "Es war Zidanes letztes Spiel als Profi – ein bitterstes Ende einer glaenzenden Karriere."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation gewann die erste Fussball-Europameisterschaft im Jahr 1960?",
        answerA = "Frankreich",
        answerB = "Jugoslawien",
        answerC = "Sowjetunion",
        answerD = "Ungarn",
        correctAnswer = 2,
        explanation = "Die Sowjetunion gewann die erste Fussball-EM 1960 in Frankreich. Im Finale besiegten sie Jugoslawien 2:1 nach Verlaengerung.",
        difficulty = 2,
        funFact = "Westdeutschland und Spanien boykottierten das Turnier aus politischen Gruenden und verweigerten die Anreise."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Tore fielen beim deutschen 'Sommermaechen' WM-Halbfinale 2006 gegen Italien?",
        answerA = "0 – Deutschland verlor 0:2",
        answerB = "1 – Deutschland verlor 0:2 nach langer Fuehrung",
        answerC = "Deutschland verlor 0:2 nach Verlaengerung",
        answerD = "Deutschland verlor nach Elfmeterschiessen",
        correctAnswer = 2,
        explanation = "Deutschland verlor das WM-Halbfinale 2006 gegen Italien 0:2 nach Verlaengerung. Beide Tore fielen erst in der 119. und 120. Spielminute.",
        difficulty = 2,
        funFact = "Das Spiel gilt als eines der dramatischsten WM-Halbfinale aller Zeiten – 120 Minuten ohne Tor, dann zwei Treffer in der Schlussminute."
    ),

    // --- Olympische Geschichte (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "In welcher Stadt fanden die ersten modernen Olympischen Sommerspiele statt?",
        answerA = "Paris",
        answerB = "London",
        answerC = "Athen",
        answerD = "Stockholm",
        correctAnswer = 2,
        explanation = "Die ersten modernen Olympischen Spiele fanden 1896 in Athen, Griechenland statt. Sie wurden von Pierre de Coubertin initiiert.",
        difficulty = 2,
        funFact = "An den ersten Spielen nahmen nur 241 Athleten aus 14 Nationen teil – alle davon maennlich."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher US-Schwimmer gewann bei den Olympischen Spielen 2008 in Peking acht Goldmedaillen?",
        answerA = "Ryan Lochte",
        answerB = "Mark Spitz",
        answerC = "Michael Phelps",
        answerD = "Ian Thorpe",
        correctAnswer = 2,
        explanation = "Michael Phelps gewann 2008 in Peking acht Goldmedaillen in einem einzigen Olympia-Turnier und stellte damit einen Rekord auf.",
        difficulty = 2,
        funFact = "Phelps haelt mit insgesamt 23 Goldmedaillen den Rekord als erfolgreichster Olympionike aller Zeiten."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr wurden die Olympischen Spiele wegen des Zweiten Weltkriegs zuletzt abgesagt?",
        answerA = "1936",
        answerB = "1940",
        answerC = "1944",
        answerD = "Sowohl 1940 als auch 1944",
        correctAnswer = 3,
        explanation = "Wegen des Zweiten Weltkriegs wurden die Spiele 1940 (Tokio/Helsinki) und 1944 (London/Cortina) abgesagt – beide Male.",
        difficulty = 2,
        funFact = "Zuvor wurden auch die Spiele 1916 wegen des Ersten Weltkriegs gestrichen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Stadt richtete die Olympischen Winterspiele 1936 aus?",
        answerA = "Oslo",
        answerB = "Garmisch-Partenkirchen",
        answerC = "Innsbruck",
        answerD = "St. Moritz",
        correctAnswer = 1,
        explanation = "Die Olympischen Winterspiele 1936 fanden in Garmisch-Partenkirchen in Deutschland statt – gleichzeitig mit den Sommerspielen in Berlin.",
        difficulty = 2,
        funFact = "Es war das erste Mal, dass Sommer- und Winterspiele im gleichen Jahr im gleichen Land stattfanden."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Goldmedaillen gewann Carl Lewis bei den Olympischen Spielen 1984 in Los Angeles?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "5",
        correctAnswer = 2,
        explanation = "Carl Lewis gewann 1984 vier Goldmedaillen: 100m, 200m, 4x100m Staffel und Weitsprung – eine Leistung vergleichbar mit Jesse Owens 1936.",
        difficulty = 2,
        funFact = "Carl Lewis gewann insgesamt 9 olympische Goldmedaillen und gilt als einer der besten Leichtathleten aller Zeiten."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Spielen ereignete sich das Muenchner Olympia-Attentat?",
        answerA = "1968 Mexiko",
        answerB = "1972 Muenchen",
        answerC = "1976 Montreal",
        answerD = "1980 Moskau",
        correctAnswer = 1,
        explanation = "Das Olympia-Attentat fand 1972 in Muenchen statt. Palästinensische Terroristen nahmen israelische Olympia-Teilnehmer als Geiseln; 11 Israelis starben.",
        difficulty = 2,
        funFact = "Nach dem Attentat wurde der israelische Geheimdienst Mossad auf die Taeternbeauftragung zur Vergeltung – bekannt als 'Operation Zorn Gottes'."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Laenderin gewann bei den Olympischen Spielen 2016 die Goldmedaille im Zehnkampf?",
        answerA = "Usain Bolt",
        answerB = "Ashton Eaton",
        answerC = "Damian Warner",
        answerD = "Kevin Mayer",
        correctAnswer = 1,
        explanation = "Ashton Eaton (USA) gewann 2016 in Rio die Goldmedaille im Zehnkampf und verteidigte damit seinen Olympiasieg von 2012.",
        difficulty = 2,
        funFact = "Eaton haelt auch den Weltrekord im Zehnkampf mit 9045 Punkten, aufgestellt 2015 bei der WM in Peking."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Stadt fanden die ersten Olympischen Winterspiele statt?",
        answerA = "Oslo",
        answerB = "Chamonix",
        answerC = "Lake Placid",
        answerD = "St. Moritz",
        correctAnswer = 1,
        explanation = "Die ersten Olympischen Winterspiele fanden 1924 in Chamonix, Frankreich statt.",
        difficulty = 2,
        funFact = "Nansen-Rennen, Eisschnelllauf und Curling gehoerten zu den ersten Disziplinen der Winterspiele."
    ),

    // --- Sportrekorde (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welchen Weltrekord ueber 100 Meter haelt Usain Bolt?",
        answerA = "9,58 Sekunden",
        answerB = "9,63 Sekunden",
        answerC = "9,69 Sekunden",
        answerD = "9,72 Sekunden",
        correctAnswer = 0,
        explanation = "Usain Bolt lief 2009 bei der WM in Berlin die 100 Meter in 9,58 Sekunden – Weltrekord bis heute.",
        difficulty = 2,
        funFact = "Bolt feierte seinen Rekord-Lauf mit einem Blitz-Pose, die zur Ikone wurde. Der Titelverteidiger ist seit 2017 kein aktiver Athlet mehr."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler haelt den Rekord fuer die meisten Grand-Slam-Titel aller Zeiten (Stand 2024)?",
        answerA = "Roger Federer",
        answerB = "Rafael Nadal",
        answerC = "Novak Djokovic",
        answerD = "Pete Sampras",
        correctAnswer = 2,
        explanation = "Novak Djokovic haelt mit 24 Grand-Slam-Titeln (Stand 2024) den Rekord – mehr als Federer (20) und Nadal (22).",
        difficulty = 2,
        funFact = "Djokovic gewann 2023 alle vier Grand-Slam-Turniere und den Davis Cup in einem Kalenderjahr."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie weit sprang Mike Powell 1991 beim Weltrekord im Weitsprung?",
        answerA = "8,67 m",
        answerB = "8,79 m",
        answerC = "8,91 m",
        answerD = "9,02 m",
        correctAnswer = 2,
        explanation = "Mike Powell sprang 1991 bei der WM in Tokio 8,95 m und brach damit Bob Beamons 23 Jahre alten Weltrekord von 8,90 m.",
        difficulty = 2,
        funFact = "Bob Beamons Weltrekord von 1968 in Mexiko-Stadt galt als nahezu unschlagbar – die Hoehenluft half damals erheblich."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Formel-1-Fahrer gewann sieben Weltmeistertitel?",
        answerA = "Ayrton Senna",
        answerB = "Alain Prost",
        answerC = "Michael Schumacher und Lewis Hamilton",
        answerD = "Niki Lauda",
        correctAnswer = 2,
        explanation = "Sowohl Michael Schumacher als auch Lewis Hamilton gewannen jeweils sieben Formel-1-Weltmeistertitel – beide halten gemeinsam den Rekord.",
        difficulty = 2,
        funFact = "Hamilton ueberholte Schumachers Rekord von sieben Titeln im Jahr 2020 beim Saisonabschluss in Abu Dhabi."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Hochsprung-Hoehe stellte Javier Sotomayor 1993 als Weltrekord auf?",
        answerA = "2,40 m",
        answerB = "2,43 m",
        answerC = "2,45 m",
        answerD = "2,47 m",
        correctAnswer = 2,
        explanation = "Javier Sotomayor (Kuba) sprang 1993 in Salamanca auf 2,45 m und haelt damit bis heute den Weltrekord im Hochsprung.",
        difficulty = 2,
        funFact = "Der Fosbury-Flop – rueckwaerts ueber die Latte – wurde erst 1968 von Dick Fosbury eingefuehrt und revolutionierte den Hochsprung."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele aufeinanderfolgende Wimbledon-Titel gewann Roger Federer?",
        answerA = "3",
        answerB = "4",
        answerC = "5",
        answerD = "6",
        correctAnswer = 2,
        explanation = "Roger Federer gewann von 2003 bis 2007 fuenf Wimbledon-Titel in Folge – ein Rekord in der Open Era.",
        difficulty = 2,
        funFact = "Federer gewann insgesamt 8 Wimbledon-Titel, den letzten 2017 ohne einen einzigen Satzverlust im gesamten Turnier."
    ),

    Question(
        categoryId = 6,
        questionText = "Welchen Weltrekord stellte Eliud Kipchoge 2023 im Marathon auf?",
        answerA = "2:00:35 Stunden",
        answerB = "2:01:09 Stunden",
        answerC = "2:01:25 Stunden",
        answerD = "2:02:57 Stunden",
        correctAnswer = 1,
        explanation = "Eliud Kipchoge lief 2023 in Berlin den Weltrekord im offiziellen Marathonlauf in 2:01:09 Stunden.",
        difficulty = 2,
        funFact = "2019 brach Kipchoge die 2-Stunden-Grenze inoffiziell in 1:59:40 – das war jedoch kein offizieller Weltrekord, da Tempomacher eingesetzt wurden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmer hielt den Weltrekord ueber 100 m Freistil als erster unter 47 Sekunden?",
        answerA = "Michael Phelps",
        answerB = "Cesar Cielo",
        answerC = "Kyle Chalmers",
        answerD = "Pieter van den Hoogenband",
        correctAnswer = 1,
        explanation = "Cesar Cielo aus Brasilien schwamm 2009 die 100 m Freistil in 46,91 Sekunden – erstmals unter 47 Sekunden.",
        difficulty = 2,
        funFact = "Cielos Rekord besteht seit 2009 noch immer – er gilt als einer der bestaendigsten Weltrekorde der Schwimmgeschichte."
    ),

    // --- Regelkunde verschiedener Sportarten (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Wie viele Punkte zaehlt ein Touchdown im American Football?",
        answerA = "4",
        answerB = "5",
        answerC = "6",
        answerD = "7",
        correctAnswer = 2,
        explanation = "Ein Touchdown zaehlt 6 Punkte. Danach kann das Team noch einen Extra-Punkt (1) durch Kick oder zwei Punkte durch eine Two-Point Conversion erzielen.",
        difficulty = 2,
        funFact = "Die Bezeichnung 'Touchdown' stammt aus der fruehen Zeit, als der Ball tatsaechlich den Boden beruehren musste – das ist heute nicht mehr notwendig."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist eine 'technische Auszeit' im Volleyball?",
        answerA = "Eine Auszeit bei technischen Problemen",
        answerB = "Eine automatische Pause beim Stand von 8 und 16 Punkten im Satz",
        answerC = "Eine vom Schiedsrichter verhaengte Unterbrechung",
        answerD = "Eine Auszeit fuer Videobeweise",
        correctAnswer = 1,
        explanation = "Im Volleyball gibt es automatische technische Auszeiten (je 60 Sekunden) wenn ein Team 8 oder 16 Punkte im Satz erreicht.",
        difficulty = 2,
        funFact = "Technische Auszeiten wurden eingefuehrt, um den Teams mehr Erholungszeit zu geben und TV-Sendern Werbepausen zu ermoeglichen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lang ist eine Eisfeld-Halbzeit im Eishockey (NHL)?",
        answerA = "15 Minuten",
        answerB = "17 Minuten",
        answerC = "20 Minuten",
        answerD = "3 Drittel a 20 Minuten – keine Halbzeit",
        correctAnswer = 3,
        explanation = "Eishockey hat keine Halbzeiten, sondern drei Drittel a 20 Minuten Spielzeit. Zwischen den Dritteln gibt es 15 Minuten Pause.",
        difficulty = 2,
        funFact = "Das Eis wird in den Pausen von Eismaschinen (Zambonis) neu praeperiert, was einer der Gruende fuer die laengeren Pausen ist."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet 'Let' beim Tischtennis?",
        answerA = "Punkt fuer den Aufschlagspieler",
        answerB = "Wiederholung des Aufschlags",
        answerC = "Fehler des Aufschlagspielers",
        answerD = "Spielunterbrechung wegen Hindernissen",
        correctAnswer = 1,
        explanation = "Ein 'Let' beim Tischtennis bedeutet, dass der Aufschlag wiederholt wird – z.B. wenn der Ball die Netzoberkante beruehrt und auf die gegnerische Seite faellt.",
        difficulty = 2,
        funFact = "Beim regulaeren Tischtennis sind unbegrenzt Lets moeglich – beim Service-Let wird immer wiederholt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Punkte gibt es beim Rugby Union fuer einen Versuch (Try)?",
        answerA = "3",
        answerB = "4",
        answerC = "5",
        answerD = "6",
        correctAnswer = 2,
        explanation = "Im Rugby Union zaehlt ein Versuch (Try) 5 Punkte. Dazu kommt eine Umwandlung (Conversion) fuer weitere 2 Punkte.",
        difficulty = 2,
        funFact = "Frueher zaehlt ein Try nur 3 Punkte. Der Wert wurde schrittweise erhoeht, um das aktive Spiel zu foerdern."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist die maximale Punktzahl in einem einzelnen Bowling-Spiel (10 Frames)?",
        answerA = "270",
        answerB = "290",
        answerC = "300",
        answerD = "Variiert je nach Regeln",
        correctAnswer = 2,
        explanation = "Die maximale Punktzahl im Bowling betraegt 300 Punkte – erreicht durch 12 aufeinanderfolgende Strikes.",
        difficulty = 2,
        funFact = "Ein perfektes Spiel (300 Punkte) wird als 'Perfect Game' bezeichnet und kommt auch in Profiligen vor."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lange dauert eine regulaere Basketballpartie in der NBA (reine Spielzeit)?",
        answerA = "32 Minuten",
        answerB = "40 Minuten",
        answerC = "48 Minuten",
        answerD = "60 Minuten",
        correctAnswer = 2,
        explanation = "Eine NBA-Partie dauert 48 Minuten reine Spielzeit, aufgeteilt in vier Viertel zu je 12 Minuten. FIBA-Basketball hat 4x10 Minuten (40 Minuten).",
        difficulty = 2,
        funFact = "Durch Fouls, Auszeiten und Unterbrechungen dauert ein NBA-Spiel real oft ueber zwei Stunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Fehlversuche hat ein Laeufer beim 110-Meter-Huerdensprung, bevor er disqualifiziert wird?",
        answerA = "Keinen – jede umgestossene Huerden ist erlaubt",
        answerB = "Disqualifizierung bei vorsaetzlichem Umreissen oder Hinterlauffenheit",
        answerC = "Zwei Fehlversuche erlaubt",
        answerD = "Drei Fehlversuche erlaubt",
        correctAnswer = 1,
        explanation = "Im Huerdenspringen duerfen Huerden umgestossen werden – es gibt keine Strafpunkte dafuer. Disqualifizierung erfolgt nur bei vorsaetzlichem Abwerfen oder Laufen ausserhalb der eigenen Bahn.",
        difficulty = 2,
        funFact = "Frueher wurden Fehlversuche mit Zeitstrafen belegt – heute sind umgefallene Huerden im modernen Huerdensprinting vollkommen regelkonform."
    ),

    // --- Sportler-Biografien (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "In welcher Stadt wurde Muhammad Ali (Cassius Clay) geboren?",
        answerA = "Chicago",
        answerB = "New York",
        answerC = "Louisville",
        answerD = "Atlanta",
        correctAnswer = 2,
        explanation = "Muhammad Ali wurde am 17. Januar 1942 in Louisville, Kentucky, als Cassius Marcellus Clay Jr. geboren.",
        difficulty = 2,
        funFact = "Ali nahm 1960 bei den Olympischen Spielen in Rom Gold im Halbschwergewicht – als Cassius Clay, bevor er zum Islam konvertierte."
    ),

    Question(
        categoryId = 6,
        questionText = "Fuer welchen Verein spielte Pelé den Grossteil seiner Karriere?",
        answerA = "Flamengo",
        answerB = "Santos FC",
        answerC = "Corinthians",
        answerD = "Sao Paulo FC",
        correctAnswer = 1,
        explanation = "Pelé spielte von 1956 bis 1974 fuer den Santos FC in Brasilien und gewann mit dem Klub mehrere Weltmeisterschaften.",
        difficulty = 2,
        funFact = "Pelé erzielte in seiner Karriere 1281 Tore in 1363 Spielen – eine bis heute unuebertroffene Torquote."
    ),

    Question(
        categoryId = 6,
        questionText = "Welchen Spitznamen hatte der Tennisspieler John McEnroe wegen seiner Temperamentsausbrueche?",
        answerA = "The Brat",
        answerB = "SuperBrat",
        answerC = "Bad Boy",
        answerD = "The Volcano",
        correctAnswer = 1,
        explanation = "John McEnroe wurde wegen seiner heftigen Streitereien mit Schiedsrichtern als 'SuperBrat' bezeichnet – war aber einer der besten Tennisspieler seiner Zeit.",
        difficulty = 2,
        funFact = "McEnroes buehmter Ausruf 'You cannot be serious!' bei Wimbledon 1981 wurde zu einem der beruehntesten Sportzitate aller Zeiten."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie alt war Tiger Woods, als er 1997 das Masters Tournament in Augusta zum ersten Mal gewann?",
        answerA = "19",
        answerB = "21",
        answerC = "23",
        answerD = "25",
        correctAnswer = 1,
        explanation = "Tiger Woods gewann 1997 im Alter von 21 Jahren als juengster Spieler das Masters in Augusta und stellte dabei mit -18 einen Turnierrekord auf.",
        difficulty = 2,
        funFact = "Woods gewann das Masters mit 12 Schlaegen Vorsprung – die groesste Siegesmarge in der Geschichte des Turniers."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Sportart wurde Roger Bannister 1954 weltberuehmt?",
        answerA = "Radfahren – erster Tour de France-Sieg eines Briten",
        answerB = "Leichtathletik – erster Mensch unter 4 Minuten ueber eine Meile",
        answerC = "Schwimmen – erster Kanal-Durchschwimmer",
        answerD = "Boxen – Weltmeister im Mittelgewicht",
        correctAnswer = 1,
        explanation = "Roger Bannister lief am 6. Mai 1954 in Oxford die erste Meile unter vier Minuten (3:59,4) – eine Leistung, die lange fuer menschlich unmoeglich gehalten wurde.",
        difficulty = 2,
        funFact = "Ironischerweise brach sein Rivale John Landy den Rekord nur wenige Wochen spaeter. Beim Commonwealth Games trafen beide aufeinander – Bannister gewann."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Sprinter gewann bei den Olympischen Spielen 1988 in Seoul Gold ueber 100 m, wurde aber spaeter disqualifiziert?",
        answerA = "Carl Lewis",
        answerB = "Leroy Burrell",
        answerC = "Ben Johnson",
        answerD = "Linford Christie",
        correctAnswer = 2,
        explanation = "Ben Johnson gewann 1988 in Seoul das 100m-Finale in Weltrekordzeit (9,79 s), wurde aber danach wegen Dopings (Stanozolol) disqualifiziert.",
        difficulty = 2,
        funFact = "Der Fall Ben Johnson war einer der groessten Doping-Skandale der Olympischen Geschichte und praegte die Anti-Doping-Politik weltweit."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Fussballer wird oft als 'Der Kaiserliche' bezeichnet?",
        answerA = "Gerd Mueller",
        answerB = "Uwe Seeler",
        answerC = "Franz Beckenbauer",
        answerD = "Karl-Heinz Rummenigge",
        correctAnswer = 2,
        explanation = "Franz Beckenbauer wird als 'Kaiser Franz' bezeichnet – er war Weltmeister als Spieler (1974) und als Trainer (1990) und gilt als einer der besten Fussballer Deutschlands.",
        difficulty = 2,
        funFact = "Beckenbauer revolutionierte die Libero-Position und spielte eine entscheidende Rolle bei der Entwicklung des deutschen Fussballs."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchem Team begann LeBron James seine NBA-Karriere im Jahr 2003?",
        answerA = "Miami Heat",
        answerB = "Los Angeles Lakers",
        answerC = "Cleveland Cavaliers",
        answerD = "Chicago Bulls",
        correctAnswer = 2,
        explanation = "LeBron James wurde 2003 als erster Pick im NBA Draft von den Cleveland Cavaliers ausgewaehlt und begann dort seine Profikarriere.",
        difficulty = 2,
        funFact = "James kam direkt aus der High School in die NBA – als 18-Jaehriger war er der Nummer-1-Draft-Pick und galt schon damals als zukuenftiger Superstar."
    ),

    // --- Sportereignisse (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Miracle on Ice' von 1980?",
        answerA = "Ein skandaloeses Eiskunstlauf-Urteil bei den Olympischen Spielen",
        answerB = "Der Sieg der US-Eishockeymannschaft gegen die Sowjetunion bei den Olympischen Spielen",
        answerC = "Der erste perfekte Salto im olympischen Eiskunstlaufen",
        answerD = "Der Weltrekord im Eisschnelllauf 1980",
        correctAnswer = 1,
        explanation = "Das 'Miracle on Ice' bezeichnet den ueberraschenden 4:3-Sieg der US-Amateureishockeymannschaft gegen die professionelle Sowjetunion bei den Winterspielen 1980 in Lake Placid.",
        difficulty = 2,
        funFact = "Die US-Mannschaft bestand aus College-Spielern, die gegen das dominante sowjetische Profiteam als chancenlos galten. Die USA gewannen danach auch die Goldmedaille."
    ),

    Question(
        categoryId = 6,
        questionText = "Was geschah beim Radrennen Tour de France 2006 mit dem Gesamtsieger?",
        answerA = "Er stuerzte in der letzten Etappe",
        answerB = "Floyd Landis wurde wegen Dopings disqualifiziert",
        answerC = "Das Rennen wurde wegen Wetterstaerken abgebrochen",
        answerD = "Es gab ein geteiltes Ergebnis",
        correctAnswer = 1,
        explanation = "Floyd Landis gewann 2006 die Tour de France, wurde aber danach wegen eines positiven Dopingtests (synthetisches Testosteron) disqualifiziert.",
        difficulty = 2,
        funFact = "Der eigentliche Gewinner Oscar Pereiro erhielt den Sieg erst fast ein Jahr nach dem Rennen – nach langen Schiedsverfahren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Ereignis wird als 'Handspiel Gottes' von Diego Maradona bezeichnet?",
        answerA = "Ein irregulares Tor mit der Hand bei der WM 1986 gegen England",
        answerB = "Ein Handspiel im Finale der Copa America",
        answerC = "Eine regelwidrige Einlage beim Finale der Bundesliga",
        answerD = "Ein Torwartfehler im WM-Finale 1986",
        correctAnswer = 0,
        explanation = "Maradona erzielte 1986 im WM-Viertelfinale gegen England ein Tor mit der Hand, das der Schiedsrichter gab. Er bezeichnete es als 'La mano de Dios' (Hand Gottes).",
        difficulty = 2,
        funFact = "Im gleichen Spiel erzielte Maradona auch das 'Tor des Jahrhunderts' – nach einem 60-Meter-Dribbling an 5 englischen Spielern vorbei."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler gewann 2001 in Wimbledon und beendete damit Pete Sampras' Siegesserie?",
        answerA = "Andre Agassi",
        answerB = "Roger Federer",
        answerC = "Lleyton Hewitt",
        answerD = "Goran Ivanisevic",
        correctAnswer = 1,
        explanation = "Der junge Roger Federer besiegte Pete Sampras 2001 in der vierten Runde von Wimbledon – eine Sensation, da Sampras damals siebenmaliger Wimbledonsieger war.",
        difficulty = 2,
        funFact = "Federer gewann Wimbledon danach selbst acht Mal – sein erster Titel kam 2003."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchem Grand-Prix-Rennen 1994 verungluckte Ayrton Senna toedlich?",
        answerA = "Monaco",
        answerB = "Hockenheim",
        answerC = "Imola",
        answerD = "Spa-Francorchamps",
        correctAnswer = 2,
        explanation = "Ayrton Senna verungluckte am 1. Mai 1994 beim San Marino Grand Prix in Imola (Autodromo Enzo e Dino Ferrari) toedlich.",
        difficulty = 2,
        funFact = "Sennas Tod erschuetterte die Formel-1-Welt und fuehrte zu massiven Sicherheitsreformen im Motorsport."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Team gewann den ersten Super Bowl in der NFL-Geschichte (1967)?",
        answerA = "Dallas Cowboys",
        answerB = "Green Bay Packers",
        answerC = "Pittsburgh Steelers",
        answerD = "San Francisco 49ers",
        correctAnswer = 1,
        explanation = "Die Green Bay Packers gewannen den ersten Super Bowl am 15. Januar 1967 gegen die Kansas City Chiefs mit 35:10.",
        difficulty = 2,
        funFact = "Trainer Vince Lombardi fuehrte die Packers zum Sieg – zu seinen Ehren wurde die Super Bowl Trophae nach ihm benannt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwergewichtsboxer besiegte Muhammad Ali 1971 im 'Kampf des Jahrhunderts'?",
        answerA = "George Foreman",
        answerB = "Joe Frazier",
        answerC = "Ken Norton",
        answerD = "Sonny Liston",
        correctAnswer = 1,
        explanation = "Joe Frazier besiegte Muhammad Ali am 8. Maerz 1971 im Madison Square Garden nach 15 Runden nach Punkten – beide Boxer waren ungeschlagen.",
        difficulty = 2,
        funFact = "Der Kampf war der erste mit zwei ungeschlagenen Schwergewichtsweltmeistern – und das erste der drei beruhmten Ali-Frazier-Duelle."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Deutsche gewann 1997 die Tour de France?",
        answerA = "Erik Zabel",
        answerB = "Jan Ullrich",
        answerC = "Rolf Aldag",
        answerD = "Udo Boelts",
        correctAnswer = 1,
        explanation = "Jan Ullrich gewann 1997 als erster Deutscher die Tour de France – bis heute der einzige Sieg eines Deutschen in diesem Radklassiker.",
        difficulty = 2,
        funFact = "Ullrich war spaeter in den Lance-Armstrong-Dopingskandal verwickelt und gab zu, in seiner Karriere ebenfalls gedopt zu haben."
    ),

)
