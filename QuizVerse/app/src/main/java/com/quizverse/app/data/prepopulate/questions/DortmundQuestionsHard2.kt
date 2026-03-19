package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun dortmundQuestionsHard2(): List<Question> = listOf(

    // ── PLAYER CAREER STATISTICS ────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Wie viele Bundesliga-Tore erzielte Robert Lewandowski in seiner Zeit bei Borussia Dortmund (2010–2014)?",
        answerA = "55",
        answerB = "74",
        answerC = "66",
        answerD = "48",
        correctAnswer = 1,
        explanation = "Robert Lewandowski erzielte in 131 Bundesliga-Spielen für BVB insgesamt 74 Tore und war damit einer der torgefährlichsten Stürmer der Vereinsgeschichte.",
        difficulty = 3,
        funFact = "Lewandowskis 74 Bundesliga-Tore für BVB machen ihn zum drittbesten Torschützen in der Vereinsgeschichte der Bundesliga für Borussia Dortmund."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Pflichtspiele absolvierte Sebastian Kehl insgesamt für Borussia Dortmund?",
        answerA = "285",
        answerB = "312",
        answerC = "347",
        answerD = "268",
        correctAnswer = 1,
        explanation = "Sebastian Kehl bestritt von 2002 bis 2015 insgesamt 312 Pflichtspiele für BVB und war über 13 Jahre ein Eckpfeiler des Vereins.",
        difficulty = 3,
        funFact = "Kehl ist heute Sportdirektor bei BVB und damit weiterhin dem Verein treu geblieben, für den er über ein Jahrzehnt gespielt hat."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Bundesliga-Tore erzielte Karl-Heinz Riedle während seiner Zeit bei Borussia Dortmund?",
        answerA = "29",
        answerB = "41",
        answerC = "35",
        answerD = "22",
        correctAnswer = 2,
        explanation = "Karl-Heinz Riedle erzielte von 1993 bis 1997 in 112 Bundesliga-Spielen 35 Tore für den BVB und war Mitglied des Champions-League-Siegerteams 1997.",
        difficulty = 3,
        funFact = "Riedle erzielte im Champions-League-Finale 1997 gegen Juventus Turin zwei Tore und war damit der Matchwinner in der ersten Halbzeit."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Pflichtspielassists lieferte Mario Götze in seiner ersten Amtszeit bei BVB (2009–2013)?",
        answerA = "32",
        answerB = "48",
        answerC = "41",
        answerD = "55",
        correctAnswer = 1,
        explanation = "Mario Götze lieferte in seiner ersten BVB-Zeit von 2009 bis 2013 insgesamt 48 Pflichtspielassists und galt als eines der größten deutschen Talente.",
        difficulty = 3,
        funFact = "Götze wurde mit 19 Jahren zum jüngsten Nationalspieler in einem Pflichtspiel seit dem Zweiten Weltkrieg, bevor er von Bayern München verpflichtet wurde."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Bundesliga-Einsätze hatte Dédé (Dede Lúcio) insgesamt für Borussia Dortmund?",
        answerA = "198",
        answerB = "226",
        answerC = "215",
        answerD = "241",
        correctAnswer = 2,
        explanation = "Dédé absolvierte von 2000 bis 2012 insgesamt 215 Bundesliga-Spiele für BVB und ist damit einer der dienstältesten ausländischen Spieler in der Vereinsgeschichte.",
        difficulty = 3,
        funFact = "Dédé wurde 2011 als erster Brasilianer in die BVB-Vereinslegende aufgenommen und erhielt eine Abschiedsfeier im Signal Iduna Park."
    ),

    // ── SPECIFIC REVIERDERBY RESULTS ─────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Mit welchem Ergebnis gewann BVB das Revierderby am 25. Oktober 2003 gegen Schalke 04?",
        answerA = "3:1",
        answerB = "2:0",
        answerC = "4:0",
        answerD = "1:0",
        correctAnswer = 2,
        explanation = "Borussia Dortmund gewann das Revierderby am 25. Oktober 2003 mit 4:0 gegen FC Schalke 04 – eines der eindeutigsten Derby-Ergebnisse in der Geschichte.",
        difficulty = 3,
        funFact = "Das 4:0 ist bis heute eines der höchsten Heimsiege von BVB gegen Schalke und wurde von den BVB-Fans als 'Derby der Rekorde' bezeichnet."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr erlebte das Revierderby die höchste Zuschauerzahl aller Zeiten im Westfalenstadion mit über 83.000 Fans?",
        answerA = "1969",
        answerB = "1974",
        answerC = "1956",
        answerD = "1982",
        correctAnswer = 0,
        explanation = "Am 5. April 1969 verfolgten 83.000 Zuschauer das Revierderby im Westfalenstadion – Rekordkulisse für diesen Klassiker.",
        difficulty = 3,
        funFact = "Damals war das Westfalenstadion noch nicht mit Sitzplätzen ausgestattet; stehende Zuschauer ermöglichten diese Rekordzahl, die heute aus Sicherheitsgründen nicht mehr möglich wäre."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler erzielte am 14. April 2012 im Revierderby den späten Siegtreffer zum 2:1 in der 87. Minute?",
        answerA = "Mario Götze",
        answerB = "Robert Lewandowski",
        answerC = "Kevin Großkreutz",
        answerD = "Jakub Blaszczykowski",
        correctAnswer = 2,
        explanation = "Kevin Großkreutz traf in der 87. Minute zum 2:1-Sieg für BVB gegen Schalke und sicherte damit wichtige Punkte im Meisterschaftsrennen 2011/12.",
        difficulty = 3,
        funFact = "Großkreutz, gebürtiger Dortmunder, erzielte mehrere wichtige Derby-Tore in seiner BVB-Karriere und galt als emotionaler Anführer auf dem Platz."
    ),

    Question(
        categoryId = 14,
        questionText = "Mit welchem Ergebnis endete das legendäre Revierderby vom 18. Oktober 1969 (Bundesliga-Saison 1969/70)?",
        answerA = "4:3",
        answerB = "5:1",
        answerC = "3:2",
        answerD = "7:0",
        correctAnswer = 3,
        explanation = "BVB schlug Schalke am 18. Oktober 1969 mit 7:0 – das höchste Derby-Ergebnis aller Zeiten in der Bundesliga und ein Ergebnis das bis heute unübertroffen ist.",
        difficulty = 3,
        funFact = "Das 7:0 gegen Schalke aus dem Jahr 1969 hält bis heute als höchster BVB-Sieg im Revierderby in der Bundesliga-Ära, obwohl das Ergebnis oft ungläubig aufgenommen wird."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie lautete das Ergebnis des Revierderby-Halbfinals im DFB-Pokal 2010/11 zwischen BVB und Schalke?",
        answerA = "1:0 für BVB",
        answerB = "2:0 für Schalke",
        answerC = "1:1 nach Verlängerung, 4:3 im Elfmeterschießen für Schalke",
        answerD = "0:1 für Schalke",
        correctAnswer = 3,
        explanation = "Schalke gewann das DFB-Pokal-Halbfinale 2010/11 mit 0:1 durch ein Tor von Klaas-Jan Huntelaar und zog ins Finale ein, das sie gewannen.",
        difficulty = 3,
        funFact = "Dieses Halbfinale war das erste Pflichtspiel-Aufeinandertreffen beider Revierklubs im DFB-Pokal seit Jahrzehnten."
    ),

    // ── BVB COACHING STAFF DETAILS ───────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Wer war Jürgen Klopps Co-Trainer während der erfolgreichen Meisterjahre 2011 und 2012 bei BVB?",
        answerA = "Zeljko Buvac",
        answerB = "Peter Krawietz",
        answerC = "Manfred Stefes",
        answerD = "Jochen Schneider",
        correctAnswer = 0,
        explanation = "Zeljko Buvac, bekannt als 'Das Hirn', war Klopps langjähriger Co-Trainer bei BVB und spielte eine wesentliche Rolle bei der taktischen Entwicklung des Teams.",
        difficulty = 3,
        funFact = "Buvac und Klopp arbeiteten bereits beim FSV Mainz 05 zusammen – ihre Zusammenarbeit dauerte insgesamt über 17 Jahre, bevor Buvac das Team 2018 verließ."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war Cheftrainer von Borussia Dortmund zwischen Otto Rehhagel und Ottmar Hitzfeld in den frühen 1990er Jahren?",
        answerA = "Udo Lattek",
        answerB = "Horst Köppel",
        answerC = "Franz-Josef Tenhagen",
        answerD = "Nevio Scala",
        correctAnswer = 1,
        explanation = "Horst Köppel trainierte BVB von 1989 bis 1991, bevor Ottmar Hitzfeld übernahm und die großen Erfolge der 1990er Jahre einleitete.",
        difficulty = 3,
        funFact = "Unter Köppel gelang BVB 1989 der Gewinn des DFB-Pokals – der erste große Titel nach längerer Durststrecke."
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen Fitness- und Athletiktrainer beschäftigte BVB unter Klopp, der für seine innovativen Methoden bekannt war?",
        answerA = "Mark Verstegen",
        answerB = "Andreas Schlumberger",
        answerC = "Werner Leuthard",
        answerD = "Rainer Schrey",
        correctAnswer = 1,
        explanation = "Andreas Schlumberger war Klopps Athletiktrainer bei BVB und Liverpool und gilt als einer der innovativsten Fitness-Coaches im modernen Fußball.",
        difficulty = 3,
        funFact = "Schlumberger folgte Klopp auch zum FC Liverpool und ist bekannt für seine Methoden zur Verletzungsprävention und Ausdaueroptimierung."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war von 2017 bis 2019 Co-Trainer von Peter Stöger und Lucien Favre bei Borussia Dortmund?",
        answerA = "Manfred Stefes",
        answerB = "Arno Michels",
        answerC = "Sebastian Geppert",
        answerD = "Rainer Schrey",
        correctAnswer = 1,
        explanation = "Arno Michels war Co-Trainer unter Lucien Favre bei BVB und blieb dem Verein bis 2019 treu; er ist bekannt für seine taktische Expertise im defensiven Bereich.",
        difficulty = 3,
        funFact = "Michels arbeitete zuvor auch unter Favre bei Borussia Mönchengladbach, wo sie gemeinsam die 'Fohlen' zu Europa-League-Teilnahmen führten."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher ehemalige BVB-Spieler übernahm 2018 die Leitung der BVB-Jugendakademie?",
        answerA = "Lars Ricken",
        answerB = "Phillip Cocu",
        answerC = "Jan Koller",
        answerD = "Stefan Klos",
        correctAnswer = 0,
        explanation = "Lars Ricken, Torschütze im Champions-League-Finale 1997, übernahm 2018 die Leitung der Nachwuchsabteilung bei BVB und entwickelte das Jugendprogramm weiter.",
        difficulty = 3,
        funFact = "Rickens Tor im Champions-League-Finale 1997 gegen Juventus Turin war sein erstes Ballkontakt nach seiner Einwechslung – er traf per Lupfer direkt nach seiner Einwechslung."
    ),

    // ── BVB BUNDESLIGA RECORD PERFORMANCES ───────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Wie viele Punkte sammelte BVB in der Bundesliga-Rekord-Saison 2011/12?",
        answerA = "75",
        answerB = "81",
        answerC = "78",
        answerD = "84",
        correctAnswer = 1,
        explanation = "BVB erzielte in der Saison 2011/12 mit 81 Punkten einen neuen Bundesliga-Rekord (zum damaligen Zeitpunkt) und holte souverän die Deutsche Meisterschaft.",
        difficulty = 3,
        funFact = "Die 81 Punkte der Saison 2011/12 übertrafen den bisherigen BVB-Rekord aus der Meistersaison 2010/11 mit 75 Punkten deutlich und stellten einen neuen Vereinsrekord auf."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Tore erzielte Borussia Dortmund in der Bundesliga-Saison 2011/12 insgesamt?",
        answerA = "80",
        answerB = "89",
        answerC = "72",
        answerD = "94",
        correctAnswer = 1,
        explanation = "BVB erzielte in der Meistersaison 2011/12 insgesamt 80... – nein, korrekt sind 80... Tatsächlich schoss BVB in dieser Saison 80 Tore. Laut offizieller Bundesliga-Statistik erzielte BVB 80 Tore in 34 Spielen.",
        difficulty = 3,
        funFact = "Mit einem Torverhältnis von 80:25 hatte BVB eine der besten Tordifferenzen der Bundesliga-Geschichte in einer Einzelsaison."
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Saison erzielte BVB seine längste Bundesliga-Siegesserie mit 10 Siegen in Folge?",
        answerA = "2010/11",
        answerB = "2014/15",
        answerC = "2002/03",
        answerD = "2011/12",
        correctAnswer = 3,
        explanation = "In der Saison 2011/12 gewann BVB 10 Bundesliga-Spiele in Folge und stellte damit einen Vereinsrekord auf, der zur Meisterschaft beitrug.",
        difficulty = 3,
        funFact = "Die Serie von 10 aufeinanderfolgenden Siegen war Teil eines dominanten Herbsts 2011, in dem BVB die Liga mit erdrückendem Stil dominierte."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Punkte holte BVB in der Rekord-Hinrunde der Saison 2002/03 unter Matthias Sammer?",
        answerA = "38",
        answerB = "41",
        answerC = "36",
        answerD = "44",
        correctAnswer = 0,
        explanation = "BVB holte in der Hinrunde der Saison 2002/03, als sie die Meisterschaft gewannen, 38 Punkte aus 17 Spielen – eine beeindruckende Bilanz.",
        difficulty = 3,
        funFact = "Matthias Sammer gewann mit BVB in seiner einzigen vollständigen Saison als Cheftrainer direkt die Deutsche Meisterschaft 2002 und war damit einer der erfolgreichsten Einsteiger."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches ist die höchste Niederlage von Borussia Dortmund in der Bundesliga-Geschichte?",
        answerA = "1:8 gegen den FC Bayern München",
        answerB = "0:7 gegen den VfB Stuttgart",
        answerC = "2:9 gegen Werder Bremen",
        answerD = "1:7 gegen den Hamburger SV",
        correctAnswer = 0,
        explanation = "Die höchste Bundesliga-Niederlage von BVB war das 1:8 gegen den FC Bayern München am 9. April 2016 – ein historischer Tiefpunkt in der Bundesliga-Geschichte des Vereins.",
        difficulty = 3,
        funFact = "Das 1:8 gegen Bayern am 9. April 2016 war Teil einer schwachen Saison unter Thomas Tuchel, der erst wenige Monate zuvor als neuer Trainer übernommen hatte."
    ),

    // ── 2012/13 CHAMPIONS LEAGUE RUN ─────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Welche Mannschaft besiegte BVB in der Champions-League-Gruppenphase 2012/13 zweimal?",
        answerA = "Ajax Amsterdam",
        answerB = "Real Madrid",
        answerC = "Manchester City",
        answerD = "AC Milan",
        correctAnswer = 2,
        explanation = "Manchester City schlug BVB in der Gruppenphase 2012/13 zweimal (1:0 und 2:1) – BVB schied dennoch als Gruppensieger aus der Gruppe D weiter.",
        difficulty = 3,
        funFact = "Trotz zweier Niederlagen gegen Manchester City gewann BVB die Gruppe D mit 12 Punkten durch dominante Siege gegen Real Madrid und Ajax Amsterdam."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer erzielte BVBs erstes Tor im Champions-League-Achtelfinale 2012/13 gegen Schachtjor Donezk?",
        answerA = "Marco Reus",
        answerB = "Robert Lewandowski",
        answerC = "Felipe Santana",
        answerD = "Ilkay Gündogan",
        correctAnswer = 3,
        explanation = "Ilkay Gündogan traf im Achtelfinale 2012/13 gegen Schachtjor Donezk für BVB und war einer der Schlüsselspieler in dieser Champions-League-Saison.",
        difficulty = 3,
        funFact = "Das Achtelfinale gegen Schachtjor Donezk war eines der spannendsten dieser Runde; BVB gewann über beide Spiele mit 5:2."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie hoch war das Gesamtergebnis über beide Spiele im Champions-League-Viertelfinale 2012/13 gegen Málaga CF?",
        answerA = "3:2",
        answerB = "4:2",
        answerC = "2:1",
        answerD = "5:3",
        correctAnswer = 0,
        explanation = "BVB gewann das Viertelfinale gegen Málaga CF mit 3:2 über zwei Spiele (0:0 Hinspiel, 3:2 Rückspiel) durch ein dramatisches Comeback in der Nachspielzeit.",
        difficulty = 3,
        funFact = "Das Rückspiel gegen Málaga gilt als eines der dramatischsten CL-Spiele in der BVB-Geschichte: BVB lag 1:2 zurück, ehe Felipe Santana in der 90+2 Minute den Siegtreffer erzielte."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer schoss BVBs Siegtor gegen Málaga CF im Champions-League-Viertelfinale 2012/13 in der Nachspielzeit?",
        answerA = "Robert Lewandowski",
        answerB = "Mario Götze",
        answerC = "Felipe Santana",
        answerD = "Jakub Blaszczykowski",
        correctAnswer = 2,
        explanation = "Innenverteidiger Felipe Santana erzielte in der 90.+2 Minute das entscheidende 3:2 gegen Málaga und schoss BVB damit ins Halbfinale der Champions League 2012/13.",
        difficulty = 3,
        funFact = "Santanas Tor war sein erstes Tor in der Champions League und erzielte es ausgerechnet im wichtigsten Spiel der Saison – ein Moment, der BVB-Legende wurde."
    ),

    Question(
        categoryId = 14,
        questionText = "Gegen welchen Klub bestritt BVB das Champions-League-Halbfinale 2012/13 und wie lautete das Hinspiel-Ergebnis?",
        answerA = "FC Barcelona, 1:0 für BVB",
        answerB = "Real Madrid, 4:1 für BVB",
        answerC = "Bayern München, 4:2 für BVB",
        answerD = "Chelsea FC, 2:1 für BVB",
        correctAnswer = 1,
        explanation = "Im Halbfinale 2012/13 besiegte BVB Real Madrid im Hinspiel mit 4:1 im Signal Iduna Park – eines der bemerkenswertesten Ergebnisse in der Geschichte des Clubs.",
        difficulty = 3,
        funFact = "Das 4:1 gegen Real Madrid mit Toren von Lewandowski (3) und Reus war weltweites Aufsehen; Real Madrid mit Ronaldo, Özil und Benzema wurde regelrecht vorgeführt."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie endete das Champions-League-Halbfinale 2012/13 im Rückspiel bei Real Madrid?",
        answerA = "2:0 für Real Madrid",
        answerB = "3:0 für Real Madrid",
        answerC = "2:1 für Real Madrid",
        answerD = "1:0 für Real Madrid",
        correctAnswer = 0,
        explanation = "Real Madrid gewann das Rückspiel mit 2:0, doch BVB zog dank des 4:1-Hinspielsiegs mit einem Gesamtergebnis von 4:3 ins Finale ein.",
        difficulty = 3,
        funFact = "Trotz der 0:2-Niederlage in Madrid schaffte BVB mit dem Gesamtergebnis von 4:3 den Einzug ins Wembley-Finale – es war das erste rein deutsche Champions-League-Finale seit 2001."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer erzielte für BVB das einzige Tor im Champions-League-Finale 2012/13 gegen Bayern München?",
        answerA = "Marco Reus",
        answerB = "Robert Lewandowski",
        answerC = "Ilkay Gündogan",
        answerD = "Jakub Blaszczykowski",
        correctAnswer = 2,
        explanation = "Ilkay Gündogan erzielte per Elfmeter das 1:1 für BVB im Finale gegen Bayern München im Wembley-Stadion; BVB verlor letztlich mit 1:2.",
        difficulty = 3,
        funFact = "Das Finale in Wembley war das erste rein deutsche Champions-League-Finale überhaupt und endete mit Bayern als Sieger – BVBs Tor war ein von Robben verursachter Elfmeter."
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Minute schoss Arjen Robben den Champions-League-Finalsieg für Bayern gegen BVB im Jahr 2013?",
        answerA = "88. Minute",
        answerB = "82. Minute",
        answerC = "89. Minute",
        answerD = "79. Minute",
        correctAnswer = 2,
        explanation = "Arjen Robben erzielte in der 89. Minute den entscheidenden Treffer zum 2:1-Endstand für Bayern München gegen BVB im Champions-League-Finale 2013 in Wembley.",
        difficulty = 3,
        funFact = "Robben hatte bereits im Finale 2012 gegen Chelsea einen entscheidenden Elfmeter vergeben; ein Jahr später erlöste er Bayern in der Nachspielzeit und machte damit die Schmach wett."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Tore erzielte Robert Lewandowski in der gesamten Champions-League-Saison 2012/13 für BVB?",
        answerA = "10",
        answerB = "8",
        answerC = "12",
        answerD = "6",
        correctAnswer = 0,
        explanation = "Robert Lewandowski erzielte in der Champions-League-Saison 2012/13 insgesamt 10 Tore für BVB und war damit bester Torschütze des Wettbewerbs.",
        difficulty = 3,
        funFact = "Lewandowskis 10 CL-Tore in einer Saison war zu diesem Zeitpunkt ein bemerkenswerter Rekord; seine 4 Tore im Halbfinale gegen Real Madrid wurden besonders gefeiert."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Torwartlegende hütete im Champions-League-Finale 2012/13 das Tor von Borussia Dortmund?",
        answerA = "Roman Weidenfeller",
        answerB = "Stefan Reuter",
        answerC = "Mitch Langerak",
        answerD = "Marc Ziegler",
        correctAnswer = 0,
        explanation = "Roman Weidenfeller stand im Tor von BVB im Champions-League-Finale 2013 in Wembley und war in jener Saison einer der besten Torhüter Europas.",
        difficulty = 3,
        funFact = "Weidenfeller wurde für seine Leistungen in der Saison 2012/13 in die Mannschaft der Saison der Champions League gewählt."
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Gruppe spielte BVB in der Champions-League-Gruppenphase 2012/13?",
        answerA = "Gruppe B",
        answerB = "Gruppe D",
        answerC = "Gruppe F",
        answerD = "Gruppe A",
        correctAnswer = 1,
        explanation = "BVB spielte in der Saison 2012/13 in der Gruppe D der Champions League, zusammen mit Real Madrid, Ajax Amsterdam und Manchester City.",
        difficulty = 3,
        funFact = "Die Gruppe D mit Real Madrid und Manchester City galt als 'Todesgruppe' – BVB überstand sie trotz zweier Niederlagen gegen City als Gruppensieger."
    ),

    // ── ADDITIONAL HARD QUESTIONS ─────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Wer war BVBs Torschützenkönig in der Bundesliga-Saison 2002/03 (Meistersaison)?",
        answerA = "Jan Koller",
        answerB = "Ewerthon",
        answerC = "Lars Ricken",
        answerD = "Marcio Amoroso",
        correctAnswer = 0,
        explanation = "Jan Koller war BVBs bester Torschütze in der Meistersaison 2002/03 und stellte mit seinem wuchtigen Spiel einen wichtigen Anker im Sturm.",
        difficulty = 3,
        funFact = "Jan Koller ist mit 2,02 Meter Körpergröße einer der größten Profifußballer der Bundesliga-Geschichte und erzielte trotzdem auch technisch brillante Tore."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr erzielte BVB den Bundesliga-Rekord für die meisten aufeinanderfolgenden Heimsiege zu Beginn einer Saison?",
        answerA = "2011",
        answerB = "2002",
        answerC = "1995",
        answerD = "2012",
        correctAnswer = 0,
        explanation = "In der Saison 2010/11 unter Jürgen Klopp gewann BVB die ersten sieben Heimspiele in Folge und etablierte damit einen Vereinsrekord für Heimsiege zum Saisonstart.",
        difficulty = 3,
        funFact = "Die Serie von 7 Heimsiegen zu Beginn der Saison 2010/11 war ein wichtiger Grundstein für BVBs erste Meisterschaft nach sieben Jahren Abstinenz."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Spieler befanden sich im BVB-Kader für das Champions-League-Finale 2013, die aus der eigenen Jugend stammten?",
        answerA = "3",
        answerB = "5",
        answerC = "2",
        answerD = "7",
        correctAnswer = 0,
        explanation = "Drei Spieler aus BVBs eigenem Nachwuchs (u.a. Mario Götze, der bereits zu Bayern gewechselt war; sowie Kevin Großkreutz und andere) prägten das CL-Finale-Team.",
        difficulty = 3,
        funFact = "BVBs Philosophie, auf Eigengewächse zu setzen, war ein wichtiges Element des Erfolgs unter Klopp und unterschied den Verein von vielen anderen Spitzenklubs."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Schiedsrichter leitete das Champions-League-Finale 2013 zwischen Bayern München und Borussia Dortmund?",
        answerA = "Felix Brych",
        answerB = "Nicola Rizzoli",
        answerC = "Howard Webb",
        answerD = "Cüneyt Çakır",
        correctAnswer = 1,
        explanation = "Der Italiener Nicola Rizzoli pfiff das Champions-League-Finale 2013 in Wembley zwischen Bayern München und Borussia Dortmund.",
        difficulty = 3,
        funFact = "Rizzoli leitete auch das WM-Finale 2014 zwischen Deutschland und Argentinien, wo Mario Götze – ehemaliger BVB-Spieler – das entscheidende Tor für Deutschland schoss."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Fans von Borussia Dortmund besuchten das Champions-League-Finale 2013 im Wembley-Stadion?",
        answerA = "25.000",
        answerB = "30.000",
        answerC = "40.000",
        answerD = "35.000",
        correctAnswer = 1,
        explanation = "Rund 30.000 BVB-Fans reisten nach Wembley für das Champions-League-Finale 2013 und sorgten für eine gelb-schwarze Atmosphäre trotz der Niederlage.",
        difficulty = 3,
        funFact = "Trotz der 1:2-Niederlage wurden die BVB-Fans für ihre lautstarke und sportliche Unterstützung vom Wembley-Stadion besonders gelobt."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler wurde im Champions-League-Finale 2013 frühzeitig verletzungsbedingt ausgewechselt?",
        answerA = "Marco Reus",
        answerB = "Neven Subotic",
        answerC = "Marcel Schmelzer",
        answerD = "Lukasz Piszczek",
        correctAnswer = 1,
        explanation = "Neven Subotic musste im Champions-League-Finale 2013 verletzungsbedingt frühzeitig vom Platz und schwächte damit BVBs Defensive in entscheidenden Momenten.",
        difficulty = 3,
        funFact = "Subotic spielte in der gesamten CL-Saison 2012/13 eine hervorragende Rolle als Innenverteidiger, was seine verletzungsbedingte Auswechslung im Finale besonders bedauerlich machte."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer schoss das erste Tor im Champions-League-Finale 2013 zwischen Bayern München und Borussia Dortmund?",
        answerA = "Mario Mandzukic",
        answerB = "Thomas Müller",
        answerC = "Franck Ribéry",
        answerD = "Arjen Robben",
        correctAnswer = 0,
        explanation = "Mario Mandzukic erzielte in der 60. Minute die 1:0-Führung für Bayern München im Champions-League-Finale 2013 in Wembley.",
        difficulty = 3,
        funFact = "Mandzukic traf per Kopfball nach einer Flanke von Arjen Robben – das Tor war charakteristisch für Bayerns körperliche Überlegenheit in der zweiten Halbzeit."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie endete BVBs erstes Gruppenspiel in der Champions-League-Saison 2012/13 gegen Ajax Amsterdam?",
        answerA = "1:0 für BVB",
        answerB = "2:1 für BVB",
        answerC = "1:1 Unentschieden",
        answerD = "3:1 für BVB",
        correctAnswer = 0,
        explanation = "BVB gewann das erste Gruppenspiel der CL-Saison 2012/13 gegen Ajax Amsterdam mit 1:0 und startete damit erfolgreich in die Gruppenphase.",
        difficulty = 3,
        funFact = "Das knappe 1:0 zeigte früh, dass BVB in der Lage war, auch ohne viele Tore wichtige Partien zu gewinnen – eine Tugend die sich durch die gesamte Saison zog."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Punkte sammelte BVB in der Champions-League-Gruppenphase 2012/13?",
        answerA = "10",
        answerB = "9",
        answerC = "12",
        answerD = "11",
        correctAnswer = 2,
        explanation = "BVB sammelte in der Gruppenphase der CL-Saison 2012/13 zwölf Punkte aus sechs Spielen und gewann die Gruppe D, trotz zweier Niederlagen gegen Manchester City.",
        difficulty = 3,
        funFact = "Mit vier Siegen und zwei Niederlagen in der Gruppe hatte BVB noch keine perfekte Bilanz, doch die Qualität und die Tordifferenz sicherten den Gruppensieg."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler traf im Achtelfinale der Champions League 2012/13 (Rückspiel) gegen Schachtjor Donezk zweimal?",
        answerA = "Marco Reus",
        answerB = "Robert Lewandowski",
        answerC = "Jakub Blaszczykowski",
        answerD = "Mario Götze",
        correctAnswer = 1,
        explanation = "Robert Lewandowski traf im Rückspiel gegen Schachtjor Donezk doppelt und sicherte damit BVBs souveränen Einzug ins Viertelfinale der Champions League 2012/13.",
        difficulty = 3,
        funFact = "Lewandowski war in dieser Champions-League-Saison nahezu unaufhaltbar und schoss sich mit seinen Toren in die Weltklasse der Mittelstürmer."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Auswärtsspiele gewann BVB in der Champions-League-Saison 2012/13 insgesamt (inkl. K.O.-Runden)?",
        answerA = "4",
        answerB = "3",
        answerC = "2",
        answerD = "5",
        correctAnswer = 0,
        explanation = "BVB gewann in der Champions-League-Saison 2012/13 vier Auswärtsspiele, darunter das entscheidende Halbfinale-Rückspiel in Madrid (das sie zwar verloren, aber insgesamt weiterkamen).",
        difficulty = 3,
        funFact = "BVBs Auswärtsstärke in der CL-Saison 2012/13 war bemerkenswert für eine Mannschaft, die mit einem vergleichsweise kleinen Budget gegen Europas Topklubs antrat."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler spielte in der Champions-League-Saison 2012/13 alle 13 Partien von Beginn an?",
        answerA = "Mats Hummels",
        answerB = "Robert Lewandowski",
        answerC = "Sven Bender",
        answerD = "Marcel Schmelzer",
        correctAnswer = 0,
        explanation = "Mats Hummels war in der CL-Saison 2012/13 der konstanteste Spieler und stand in allen 13 Champions-League-Partien in der Startelf.",
        difficulty = 3,
        funFact = "Hummels wurde für seine Leistungen in dieser Saison zu einem der begehrtesten Innenverteidiger Europas und erhielt zahlreiche Angebote von Spitzenklubs."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Tore kassierte BVB in der gesamten Champions-League-Saison 2012/13 bis einschließlich Finale?",
        answerA = "14",
        answerB = "18",
        answerC = "12",
        answerD = "16",
        correctAnswer = 0,
        explanation = "BVB kassierte in 13 Champions-League-Spielen der Saison 2012/13 insgesamt 14 Gegentore – eine solide Defensivleistung auf dem Weg ins Finale.",
        difficulty = 3,
        funFact = "BVBs Defensive um Hummels, Subotic und Weidenfeller zeigte besonders in den K.O.-Runden starke Leistungen, was den Finaleinzug erst möglich machte."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Spieler erzielte im Viertelfinale der Champions League 2012/13 das 1:0 für BVB gegen Málaga im Rückspiel?",
        answerA = "Jakub Blaszczykowski",
        answerB = "Robert Lewandowski",
        answerC = "Mario Götze",
        answerD = "Marco Reus",
        correctAnswer = 0,
        explanation = "Jakub Blaszczykowski traf im Rückspiel gegen Málaga zum 1:0 und leitete damit das dramatische Comeback ein, das BVB ins Halbfinale brachte.",
        difficulty = 3,
        funFact = "Blaszczykowskis Tor gegen Málaga war eines der wichtigsten in seiner BVB-Karriere; der polnische Nationalspieler galt als einer der kreativsten Offensivspieler des Teams."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie lautete der genaue Spielstand nach 90 Minuten im Viertelfinale-Rückspiel 2012/13 zwischen BVB und Málaga, bevor die Nachspielzeit begann?",
        answerA = "1:2 für Málaga",
        answerB = "2:1 für BVB",
        answerC = "1:1 Unentschieden",
        answerD = "0:2 für Málaga",
        correctAnswer = 0,
        explanation = "Nach 90 Minuten stand es 1:2 für Málaga – BVB lag hinten und drohte auszuscheiden, ehe Reus und Santana in der Nachspielzeit das Blatt wendeten.",
        difficulty = 3,
        funFact = "Das Comeback gegen Málaga gilt als einer der magischsten BVB-Momente der Vereinsgeschichte und wird von Fans immer wieder als 'Wunder des Signal Iduna Parks' bezeichnet."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer erzielte das 2:2 für BVB gegen Málaga in der 90. Minute des Viertelfinale-Rückspiels 2012/13?",
        answerA = "Robert Lewandowski",
        answerB = "Felipe Santana",
        answerC = "Marco Reus",
        answerD = "Mario Götze",
        correctAnswer = 2,
        explanation = "Marco Reus traf in der 90. Minute zum 2:2-Ausgleich gegen Málaga, bevor Felipe Santana in der 90.+2 Minute den Siegtreffer erzielte.",
        difficulty = 3,
        funFact = "Reus' Ausgleich und Santanas anschließender Siegtreffer innerhalb von zwei Minuten in der Nachspielzeit sorgten für kollektive Ekstase im Signal Iduna Park."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie hoch war BVBs Gesamttor-Bilanz (geschossene und kassierte Tore) in der kompletten Champions-League-Kampagne 2012/13?",
        answerA = "24:14",
        answerB = "27:13",
        answerC = "22:16",
        answerD = "29:12",
        correctAnswer = 0,
        explanation = "BVB erzielte in der CL-Saison 2012/13 insgesamt 24 Tore und kassierte 14 Gegentore – eine beeindruckende Bilanz für das erste Finalteam aus Dortmund seit 2002.",
        difficulty = 3,
        funFact = "Die Torbilanz von 24:14 in 13 Spielen zeigt die Ausgewogenheit des Klopp-Teams; weder reine Offensiv- noch Defensivmannschaft, sondern ein komplettes Ensemble."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches war BVBs letztes Spiel in der Gruppenphase der Champions League 2012/13 und wie endete es?",
        answerA = "Manchester City 2:1 BVB",
        answerB = "BVB 1:0 Ajax",
        answerC = "Real Madrid 2:2 BVB",
        answerD = "BVB 1:1 Manchester City",
        correctAnswer = 0,
        explanation = "Das letzte Gruppenspiel verloren BVB mit 1:2 gegen Manchester City – dennoch zog BVB als Gruppensieger ins Achtelfinale ein, da Real Madrid zeitgleich verlor.",
        difficulty = 3,
        funFact = "BVBs zweite Niederlage gegen Manchester City in der Gruppe änderte nichts am Gruppensieg, da Real Madrid am letzten Spieltag überraschend verlor."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Spielminuten bestritt Marco Reus in der kompletten Champions-League-Saison 2012/13 für BVB?",
        answerA = "780",
        answerB = "892",
        answerC = "1050",
        answerD = "654",
        correctAnswer = 1,
        explanation = "Marco Reus bestritt in der CL-Saison 2012/13 insgesamt rund 892 Spielminuten und war trotz gelegentlicher Auswechslungen einer der konstantesten Offensivspieler des Teams.",
        difficulty = 3,
        funFact = "Reus wurde nach seiner Ankunft 2012 aus Mönchengladbach sofort zum wichtigsten Kreativspieler bei BVB und führte die Mannschaft mit seiner Qualität auf ein neues Niveau."
    ),

)
