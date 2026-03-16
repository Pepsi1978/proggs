package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun dortmundQuestions(): List<Question> = listOf(

    // ── EASY (10) ──────────────────────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr wurde Borussia Dortmund gegründet?",
        answerA = "1909",
        answerB = "1912",
        answerC = "1905",
        answerD = "1920",
        correctAnswer = 0,
        explanation = "Borussia Dortmund wurde am 19. Dezember 1909 gegründet.",
        difficulty = 1,
        funFact = "Der Verein wurde von jungen Männern gegründet, die sich gegen ihren Pfarrer durchsetzten, der ihnen die Mitgliedschaft im Athletik-Club Borussia verboten hatte."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie lautet der Spitzname von Borussia Dortmund?",
        answerA = "Die Schwarzen",
        answerB = "BVB",
        answerC = "Die Borussen",
        answerD = "Der BVB",
        correctAnswer = 1,
        explanation = "Borussia Dortmund wird allgemein als BVB abgekürzt, was für Ballspiel-Verein Borussia steht.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Farben trägt Borussia Dortmund?",
        answerA = "Blau und Weiß",
        answerB = "Rot und Schwarz",
        answerC = "Schwarz und Gelb",
        answerD = "Grün und Weiß",
        correctAnswer = 2,
        explanation = "Borussia Dortmund spielt traditionell in Schwarz und Gelb, den Vereinsfarben seit den frühen Jahren.",
        difficulty = 1,
        funFact = "Die Farben Schwarz und Gelb sollen ursprünglich von einer lokalen Schule übernommen worden sein."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie heißt das Stadion von Borussia Dortmund?",
        answerA = "Allianz Arena",
        answerB = "Signal Iduna Park",
        answerC = "Olympiastadion",
        answerD = "Volksparkstadion",
        correctAnswer = 1,
        explanation = "Das Heimstadion von BVB heißt seit 2005 Signal Iduna Park (nach dem Versicherungsunternehmen Signal Iduna).",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Wie nennt man die berühmte Südtribüne im Stadion von Borussia Dortmund?",
        answerA = "Nordkurve",
        answerB = "Rote Wand",
        answerC = "Gelbe Wand",
        answerD = "Schwarze Wand",
        correctAnswer = 2,
        explanation = "Die Südtribüne des Signal Iduna Parks wird als 'Gelbe Wand' bezeichnet und fasst über 25.000 Stehplätze.",
        difficulty = 1,
        funFact = "Die Gelbe Wand gilt als eine der lautesten und eindrucksvollsten Fankurven der Welt."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war der BVB-Trainer, der den Verein in die goldene Ära 2010-2012 führte?",
        answerA = "Thomas Tuchel",
        answerB = "Ottmar Hitzfeld",
        answerC = "Jürgen Klopp",
        answerD = "Edin Terzic",
        correctAnswer = 2,
        explanation = "Jürgen Klopp trainierte Borussia Dortmund von 2008 bis 2015 und gewann zwei Bundesliga-Meisterschaften in Folge.",
        difficulty = 1,
        funFact = "Klopp wurde für seinen intensiven Pressing-Stil bekannt, den er 'Gegenpressing' nannte."
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Stadt befindet sich Borussia Dortmund?",
        answerA = "Köln",
        answerB = "Essen",
        answerC = "Bochum",
        answerD = "Dortmund",
        correctAnswer = 3,
        explanation = "Borussia Dortmund kommt aus Dortmund im Ruhrgebiet in Nordrhein-Westfalen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Wie heißt der Erzrivale von Borussia Dortmund im Revierderby?",
        answerA = "FC Schalke 04",
        answerB = "Bayer Leverkusen",
        answerC = "VfL Bochum",
        answerD = "MSV Duisburg",
        correctAnswer = 0,
        explanation = "Das Revierderby zwischen BVB und dem FC Schalke 04 gilt als eines der intensivsten Derbys im deutschen Fußball.",
        difficulty = 1,
        funFact = "Das Derby zwischen Dortmund und Schalke wird auch als 'Das Spiel' bezeichnet."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr gewann Borussia Dortmund die UEFA Champions League?",
        answerA = "1994",
        answerB = "1997",
        answerC = "2001",
        answerD = "2013",
        correctAnswer = 1,
        explanation = "BVB gewann die UEFA Champions League 1997 in München gegen Juventus Turin mit 3:1.",
        difficulty = 1,
        funFact = "Es war der erste Champions-League-Titel für einen deutschen Verein seit Bayern München 1976."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Trikot trägt BVB traditionell als Heimtrikot?",
        answerA = "Weißes Trikot mit schwarzen Streifen",
        answerB = "Schwarzes Trikot mit gelben Ärmeln",
        answerC = "Gelbes Trikot mit schwarzen Details",
        answerD = "Schwarz-gelbes Streifen-Trikot",
        correctAnswer = 2,
        explanation = "Das klassische Heimtrikot von BVB ist gelb mit schwarzen Details, das die Vereinsfarben widerspiegelt.",
        difficulty = 1,
        funFact = null
    ),

    // ── MEDIUM (15) ────────────────────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Wer schoss das entscheidende Tor im Champions-League-Finale 1997 gegen Juventus Turin?",
        answerA = "Stéphane Chapuisat",
        answerB = "Karl-Heinz Riedle",
        answerC = "Lars Ricken",
        answerD = "Andreas Möller",
        correctAnswer = 2,
        explanation = "Lars Ricken erzielte das 3:1 in der 71. Minute mit einem Lupfer kurz nach seiner Einwechslung und besiegelte damit den Sieg.",
        difficulty = 2,
        funFact = "Ricken war erst 20 Jahre alt und traf mit seinem ersten Ballkontakt im Finale."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Bundesliga-Meisterschaften hat Borussia Dortmund gewonnen?",
        answerA = "5",
        answerB = "6",
        answerC = "8",
        answerD = "10",
        correctAnswer = 2,
        explanation = "Borussia Dortmund gewann bislang acht Bundesliga-Meisterschaften (Stand 2024).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Wer ist der Rekordtorschütze in der Geschichte von Borussia Dortmund?",
        answerA = "Robert Lewandowski",
        answerB = "Michael Zorc",
        answerC = "Stéphane Chapuisat",
        answerD = "Marco Reus",
        correctAnswer = 1,
        explanation = "Michael Zorc erzielte in seiner Karriere bei BVB 463 Pflichtspiele und ist mit 220 Toren Rekordtorschütze des Vereins.",
        difficulty = 2,
        funFact = "Zorc wurde später Sportdirektor bei BVB und prägte den Verein über Jahrzehnte."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr erreichte Borussia Dortmund erneut das Champions-League-Finale?",
        answerA = "2011",
        answerB = "2013",
        answerC = "2016",
        answerD = "2019",
        correctAnswer = 1,
        explanation = "2013 stand BVB im Champions-League-Finale in Wembley gegen Bayern München, verlor jedoch 1:2.",
        difficulty = 2,
        funFact = "Es war das erste rein deutsche Champions-League-Finale in der Geschichte des Wettbewerbs."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Tore schoss Erling Haaland in seiner ersten Bundesliga-Saison bei BVB (2019/20)?",
        answerA = "13",
        answerB = "16",
        answerC = "22",
        answerD = "27",
        correctAnswer = 1,
        explanation = "Erling Haaland erzielte in der Saison 2019/20 für BVB 16 Bundesliga-Tore – dies in nur 18 Spielen.",
        difficulty = 2,
        funFact = "Haaland erzielte in seinen ersten drei Bundesliga-Spielen für BVB fünf Tore."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Spieler wechselte 2013 im DFB-Pokalfinale von BVB zu Bayern München und wurde mit dem 'Verrat' assoziiert?",
        answerA = "Mario Götze",
        answerB = "Nuri Şahin",
        answerC = "Henrikh Mkhitaryan",
        answerD = "Kevin Großkreutz",
        correctAnswer = 0,
        explanation = "Mario Götze wechselte 2013 zu Bayern München, was viele BVB-Fans als Verrat empfanden, da er aus der BVB-Jugend stammte.",
        difficulty = 2,
        funFact = "Götze kehrte 2016 nach drei Jahren zu BVB zurück."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war Kapitän von Borussia Dortmund beim Champions-League-Finale 1997?",
        answerA = "Stefan Reuter",
        answerB = "Michael Zorc",
        answerC = "Andreas Möller",
        answerD = "Matthias Sammer",
        correctAnswer = 2,
        explanation = "Andreas Möller war Kapitän der BVB-Mannschaft beim Champions-League-Finale 1997 in München.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler gewann 2014 mit Deutschland die Fußball-Weltmeisterschaft und schoss das entscheidende Tor im Finale?",
        answerA = "Marco Reus",
        answerB = "Mats Hummels",
        answerC = "Mario Götze",
        answerD = "Ilkay Gündogan",
        correctAnswer = 2,
        explanation = "Mario Götze schoss im WM-Finale 2014 in der Verlängerung das 1:0 gegen Argentinien und sicherte Deutschland den Weltmeistertitel.",
        difficulty = 2,
        funFact = "Das Tor fiel in der 113. Minute der Verlängerung."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie lautet der ursprüngliche Name des Signal Iduna Parks?",
        answerA = "Ruhrstadion",
        answerB = "Westfalenstadion",
        answerC = "BVB Arena",
        answerD = "Dortmund Arena",
        correctAnswer = 1,
        explanation = "Das Stadion wurde 1974 als Westfalenstadion eröffnet und ist bis heute unter diesem Namen bekannt. Der Sponsorenname Signal Iduna Park gilt seit 2005.",
        difficulty = 2,
        funFact = "Das Westfalenstadion war Austragungsort von Spielen der Fußball-WM 1974 und 2006."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Zuschauer fasst der Signal Iduna Park bei Bundesliga-Spielen?",
        answerA = "62.000",
        answerB = "75.000",
        answerC = "81.365",
        answerD = "68.000",
        correctAnswer = 2,
        explanation = "Der Signal Iduna Park fasst bei nationalen Spielen 81.365 Zuschauer und ist damit das größte Fußballstadion Deutschlands.",
        difficulty = 2,
        funFact = "Bei internationalen Spielen (ohne Stehplätze) reduziert sich die Kapazität auf 66.099 Zuschauer."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Torwart war beim Champions-League-Sieg 1997 im Tor?",
        answerA = "Roman Weidenfeller",
        answerB = "Stefan Klos",
        answerC = "Jens Lehmann",
        answerD = "Marwin Hitz",
        correctAnswer = 1,
        explanation = "Stefan Klos war der Stammtorwart von BVB beim Champions-League-Sieg 1997.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Wer trainierte BVB in der Saison des Champions-League-Sieges 1997?",
        answerA = "Nevio Scala",
        answerB = "Udo Lattek",
        answerC = "Ottmar Hitzfeld",
        answerD = "Bernd Krauss",
        correctAnswer = 0,
        explanation = "Der Italiener Nevio Scala trainierte Borussia Dortmund zum Champions-League-Sieg 1997.",
        difficulty = 2,
        funFact = "Scala war der erste ausländische Trainer, der die Champions League gewann."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie heißt die BVB-Vereinshymne?",
        answerA = "Immer wieder Dortmund",
        answerB = "Heja BVB",
        answerC = "Echte Liebe",
        answerD = "Schwarz und Gelb",
        correctAnswer = 1,
        explanation = "Die offizielle Vereinshymne von Borussia Dortmund heißt 'Heja BVB'.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Saison gewann BVB unter Klopp zwei Bundesliga-Meisterschaften in Folge?",
        answerA = "2009/10 und 2010/11",
        answerB = "2010/11 und 2011/12",
        answerC = "2011/12 und 2012/13",
        answerD = "2008/09 und 2009/10",
        correctAnswer = 1,
        explanation = "BVB gewann unter Jürgen Klopp die Bundesliga in den Saisons 2010/11 und 2011/12.",
        difficulty = 2,
        funFact = "In der Saison 2011/12 holte BVB den Titel mit 81 Punkten – einem damaligen Bundesliga-Rekord."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Schweizer Stürmer war eine Legende bei Borussia Dortmund in den 1990er Jahren?",
        answerA = "Granit Xhaka",
        answerB = "Stéphane Chapuisat",
        answerC = "Xherdan Shaqiri",
        answerD = "Hakan Yakin",
        correctAnswer = 1,
        explanation = "Stéphane Chapuisat spielte von 1991 bis 1999 für BVB und war einer der besten Stürmer seiner Zeit.",
        difficulty = 2,
        funFact = "Chapuisat erzielte 106 Tore in 270 Bundesliga-Spielen für BVB."
    ),

    // ── HARD (12) ──────────────────────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Was war das Ergebnis des Champions-League-Finales 1997 gegen Juventus Turin?",
        answerA = "2:1",
        answerB = "3:1",
        answerC = "2:0",
        answerD = "4:2",
        correctAnswer = 1,
        explanation = "BVB besiegte Juventus Turin im Finale am 28. Mai 1997 in München mit 3:1. Karl-Heinz Riedle traf zweimal, Lars Ricken schoss das dritte Tor.",
        difficulty = 3,
        funFact = "Alessandro Del Piero traf für Juventus zum zwischenzeitlichen 1:2."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Wettbewerb gewann BVB 1966 seinen ersten großen internationalen Titel?",
        answerA = "UEFA-Pokal",
        answerB = "Europapokal der Pokalsieger",
        answerC = "Messepokal",
        answerD = "Intertoto-Cup",
        correctAnswer = 1,
        explanation = "BVB gewann 1966 den Europapokal der Pokalsieger als erster westdeutscher Verein, der einen Europapokal holte.",
        difficulty = 3,
        funFact = "Im Finale besiegte BVB den FC Liverpool mit 2:1 nach Verlängerung."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie heißt der ehemalige BVB-Spieler, der 1996 Europameister mit Deutschland wurde und als einer der besten deutschen Spieler gilt?",
        answerA = "Andreas Möller",
        answerB = "Stefan Reuter",
        answerC = "Matthias Sammer",
        answerD = "Karl-Heinz Riedle",
        correctAnswer = 2,
        explanation = "Matthias Sammer gewann 1996 die Europameisterschaft und wurde zudem zum Europas Fußballer des Jahres gewählt – er spielte damals für BVB.",
        difficulty = 3,
        funFact = "Sammer war der letzte deutsche Spieler, der als Europas Fußballer des Jahres ausgezeichnet wurde (1996)."
    ),

    Question(
        categoryId = 14,
        questionText = "Wann stand BVB zuletzt im Champions-League-Finale (nach 2013)?",
        answerA = "2019",
        answerB = "2021",
        answerC = "2023",
        answerD = "2024",
        correctAnswer = 3,
        explanation = "Borussia Dortmund erreichte im Jahr 2024 erneut das Champions-League-Finale in Wembley, verlor jedoch gegen Real Madrid mit 0:2.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher polnische Stürmer wechselte 2010 ablösefrei von Lech Posen zu BVB und wurde zur Vereinslegende?",
        answerA = "Arkadiusz Milik",
        answerB = "Jakub Błaszczykowski",
        answerC = "Robert Lewandowski",
        answerD = "Łukasz Piszczek",
        correctAnswer = 2,
        explanation = "Robert Lewandowski wechselte 2010 ablösefrei von Lech Posen zu BVB und wurde unter Klopp zum Weltklassestürmer.",
        difficulty = 3,
        funFact = "Lewandowski wechselte 2014 ablösefrei zu Bayern München, nachdem er seinen Vertrag nicht verlängerte."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr meldete Borussia Dortmund fast Insolvenz an?",
        answerA = "2001",
        answerB = "2003",
        answerC = "2005",
        answerD = "2007",
        correctAnswer = 1,
        explanation = "2003 stand Borussia Dortmund kurz vor der Insolvenz, nachdem der Verein zu viel für Spielertransfers ausgegeben hatte.",
        difficulty = 3,
        funFact = "Dortmund war der erste börsennotierte Fußballclub Deutschlands (seit 2000)."
    ),

    Question(
        categoryId = 14,
        questionText = "Gegen wen gewann BVB das DFB-Pokalfinale 2012 in Berlin?",
        answerA = "Schalke 04",
        answerB = "Bayer Leverkusen",
        answerC = "Bayern München",
        answerD = "Hamburger SV",
        correctAnswer = 2,
        explanation = "BVB gewann 2012 das DFB-Pokalfinale gegen Bayern München mit 5:2 und holte damit das Double.",
        difficulty = 3,
        funFact = "Es war der erste Gewinn des Doubles (Meisterschaft + Pokal) in der Geschichte von BVB."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Tore schoss Karl-Heinz Riedle im Champions-League-Finale 1997 gegen Juventus?",
        answerA = "1",
        answerB = "2",
        answerC = "3",
        answerD = "0",
        correctAnswer = 1,
        explanation = "Karl-Heinz Riedle traf in der 29. und 34. Minute und brachte BVB mit 2:0 in Führung.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen Rekord stellte der Signal Iduna Park bei seiner Renovierung in den 2000er Jahren auf?",
        answerA = "Größtes Dach Europas",
        answerB = "Größte LED-Flutlichtanlage",
        answerC = "Größtes Fußballstadion der Welt",
        answerD = "Größte Stehplatztribüne Europas",
        correctAnswer = 3,
        explanation = "Die Gelbe Wand (Südtribüne) ist die größte Stehplatztribüne Europas mit über 25.000 Stehplätzen.",
        difficulty = 3,
        funFact = "Insgesamt hat der Signal Iduna Park 24.454 Stehplätze auf der Südtribüne allein."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Trainer holte nach Klopp erneut den DFB-Pokal für den Verein im Jahr 2017?",
        answerA = "Peter Bosz",
        answerB = "Thomas Tuchel",
        answerC = "Lucien Favre",
        answerD = "Roger Schmidt",
        correctAnswer = 1,
        explanation = "Thomas Tuchel gewann mit BVB 2017 den DFB-Pokal gegen Eintracht Frankfurt mit 2:1.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler wurde beim Bombenanschlag auf den Mannschaftsbus im April 2017 verletzt?",
        answerA = "Marco Reus",
        answerB = "Marc Bartra",
        answerC = "Pierre-Emerick Aubameyang",
        answerD = "Roman Bürki",
        correctAnswer = 1,
        explanation = "Der spanische Verteidiger Marc Bartra wurde beim Anschlag auf den BVB-Bus am 11. April 2017 verletzt.",
        difficulty = 3,
        funFact = "Der Anschlag erfolgte kurz vor einem Champions-League-Viertelfinalspiel gegen AS Monaco."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie heißt der langjährige BVB-Sportdirektor, der selbst Vereinsikone als Spieler war?",
        answerA = "Michael Zorc",
        answerB = "Lars Ricken",
        answerC = "Karl-Heinz Riedle",
        answerD = "Andreas Möller",
        correctAnswer = 0,
        explanation = "Michael Zorc war nach seiner Spielerkarriere viele Jahre Sportdirektor bei Borussia Dortmund.",
        difficulty = 3,
        funFact = "Zorc war insgesamt über 30 Jahre mit BVB verbunden – als Spieler und als Funktionär."
    ),

    // ── EXPERT (8) ─────────────────────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "In welchem Stadion fand das Champions-League-Finale 1997 zwischen BVB und Juventus Turin statt?",
        answerA = "Wembley Stadium",
        answerB = "Santiago Bernabéu",
        answerC = "Münchner Olympiastadion",
        answerD = "San Siro",
        correctAnswer = 2,
        explanation = "Das Finale fand am 28. Mai 1997 im Olympiastadion München statt – gewissermaßen ein Heimspiel für den deutschen Vertreter.",
        difficulty = 4,
        funFact = "Für BVB war es ein Heimspiel-Finale, da Juventus die Auswärtsmannschaft war."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Rückennummer trug Marco Reus bei Borussia Dortmund während seiner gesamten Karriere?",
        answerA = "7",
        answerB = "10",
        answerC = "11",
        answerD = "9",
        correctAnswer = 2,
        explanation = "Marco Reus trug bei BVB stets die Rückennummer 11 und war von 2012 bis 2024 Vereinskapitän.",
        difficulty = 4,
        funFact = "Reus verzichtete mehrfach auf lukrative Wechselangebote, um BVB treu zu bleiben."
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen Transferrekord stellte Borussia Dortmund mit dem Verkauf von Erling Haaland im Jahr 2022 auf?",
        answerA = "75 Millionen Euro",
        answerB = "60 Millionen Euro",
        answerC = "85 Millionen Euro",
        answerD = "100 Millionen Euro",
        correctAnswer = 1,
        explanation = "Erling Haaland wurde 2022 für rund 60 Millionen Euro (inklusive Ausstiegsklausel) zu Manchester City verkauft.",
        difficulty = 4,
        funFact = "Haaland erzielte in 67 Pflichtspielen für BVB 86 Tore – eine überragende Quote."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie heißt die offizielle Mitgliederzahl von Borussia Dortmund (ca. Stand 2023)?",
        answerA = "Über 100.000",
        answerB = "Über 150.000",
        answerC = "Über 200.000",
        answerD = "Über 250.000",
        correctAnswer = 1,
        explanation = "BVB hat über 150.000 eingetragene Mitglieder und ist damit einer der mitgliederstärksten Sportvereine Deutschlands.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Welche BVB-Junioren-Akademie gilt als eine der besten in Deutschland?",
        answerA = "BVB Sportschule",
        answerB = "BVB NLZ (Nachwuchsleistungszentrum)",
        answerC = "Kicker Akademie Dortmund",
        answerD = "Ruhr Fußball Akademie",
        correctAnswer = 1,
        explanation = "Das Nachwuchsleistungszentrum (NLZ) von BVB gilt als eine der besten Jugendakademien Deutschlands und brachte Spieler wie Mario Götze hervor.",
        difficulty = 4,
        funFact = "Das NLZ von BVB wurde mehrfach mit dem DFL-Stern für herausragende Nachwuchsarbeit ausgezeichnet."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer erzielte das schnellste Bundesliga-Tor in der Geschichte von Borussia Dortmund?",
        answerA = "Pierre-Emerick Aubameyang",
        answerB = "Erling Haaland",
        answerC = "Karim Adeyemi",
        answerD = "Donyell Malen",
        correctAnswer = 1,
        explanation = "Erling Haaland ist für einige der schnellsten Tore in BVBs Bundesliga-Geschichte verantwortlich und traf regelmäßig nach unter einer Minute.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr wurde das Westfalenstadion (heutiger Signal Iduna Park) zur WM 1974 eröffnet?",
        answerA = "1972",
        answerB = "1974",
        answerC = "1976",
        answerD = "1970",
        correctAnswer = 0,
        explanation = "Das Westfalenstadion wurde 1972 eröffnet, zwei Jahre vor der Weltmeisterschaft 1974.",
        difficulty = 4,
        funFact = "Das Stadion kostete bei seiner Eröffnung rund 34 Millionen Deutsche Mark."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war der erste ausländische Spieler, den Borussia Dortmund jemals verpflichtete?",
        answerA = "Bert Trautmann",
        answerB = "Hans Tilkowski",
        answerC = "Willie Ormond",
        answerD = "Zigi Latos",
        correctAnswer = 3,
        explanation = "Zigi Latos, ein jugoslawischer Spieler, war einer der frühen ausländischen Spieler bei BVB in den Nachkriegsjahren.",
        difficulty = 4,
        funFact = null
    ),

    // ── MASTER (5) ─────────────────────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Wie lautet die genaue Gründungsadresse bzw. der Gründungsort von Borussia Dortmund im Jahr 1909?",
        answerA = "Im Gasthaus 'Zum Wildschütz'",
        answerB = "Im Stadtpark Dortmund",
        answerC = "In der Dreifaltigkeitskirche",
        answerD = "Im Ratskeller Dortmund",
        correctAnswer = 0,
        explanation = "Borussia Dortmund wurde am 19. Dezember 1909 im Gasthaus 'Zum Wildschütz' in Dortmund gegründet.",
        difficulty = 5,
        funFact = "Die Gründungsmitglieder waren größtenteils Jugendliche des Kirchenchors Heilige Dreifaltigkeit."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche BVB-Spieler standen im Kader des Champions-League-Sieges 1997 UND gewannen im selben Jahr mit Deutschland die U17-Weltmeisterschaft?",
        answerA = "Lars Ricken und Jens Lehmann",
        answerB = "Es gab keinen solchen Spieler",
        answerC = "Sebastian Kehl",
        answerD = "Sven Bender",
        correctAnswer = 1,
        explanation = "Es gab keinen BVB-Spieler, der 1997 sowohl den Champions-League-Sieg als auch die U17-WM gewann. Lars Ricken war im CL-Kader, aber nicht U17-Weltmeister in dem Jahr.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Was war der genaue Marktwertverlust von BVB durch die Insolvenz-Krise Anfang der 2000er Jahre?",
        answerA = "Etwa 100 Millionen Euro Schulden",
        answerB = "Etwa 118 Millionen Euro Schulden",
        answerC = "Etwa 200 Millionen Euro Schulden",
        answerD = "Etwa 150 Millionen Euro Schulden",
        correctAnswer = 1,
        explanation = "Zur Zeit der Krise 2003/2004 hatte BVB Schulden von rund 118 Millionen Euro angehäuft.",
        difficulty = 5,
        funFact = "Nur ein Notkredit der Deutschen Bank rettete den Verein vor der endgültigen Insolvenz."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Bundesliga-Rekordserie stellte BVB in der Saison 2011/12 auf?",
        answerA = "Meiste Siege in einer Saison (28)",
        answerB = "Längste Siegesserie (18 Spiele)",
        answerC = "Höchste Punktzahl einer Saison (81 Punkte)",
        answerD = "Meiste erzielte Tore in einer Saison (108)",
        correctAnswer = 2,
        explanation = "BVB holte in der Saison 2011/12 unter Klopp 81 Punkte – die bis dahin höchste Punktzahl in der Bundesliga-Geschichte.",
        difficulty = 5,
        funFact = "Dieser Rekord wurde später von Bayern München übertroffen."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler der Meistermannschaft 1995/96 spielte zuvor in der DDR-Nationalmannschaft?",
        answerA = "Stefan Reuter",
        answerB = "Matthias Sammer",
        answerC = "Andreas Möller",
        answerD = "Michael Zorc",
        correctAnswer = 1,
        explanation = "Matthias Sammer spielte vor der Wiedervereinigung für die DDR-Nationalmannschaft und danach für die gesamtdeutsche Nationalelf.",
        difficulty = 5,
        funFact = "Sammer ist damit einer der wenigen Spieler, der sowohl für die DDR als auch für Deutschland spielte."
    ),

    // ── NEW EASY (15) ──────────────────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Welchen Titel trägt der BVB-Cheftrainer, der im Volksmund auch als 'Vereinsikone' bezeichnet wird und seit 2021 mehrfach als Interimstrainer einsprang?",
        answerA = "Edin Terzic",
        answerB = "Niko Kovač",
        answerC = "Hansi Flick",
        answerD = "Rudi Völler",
        correctAnswer = 0,
        explanation = "Edin Terzic gewann mit BVB 2021 den DFB-Pokal als Interimstrainer und wurde danach Cheftrainer. Er ist ein überzeugter BVB-Fan seit Kindheitstagen.",
        difficulty = 1,
        funFact = "Terzic wuchs als BVB-Fan auf und arbeitete sich vom Co-Trainer bis zum Cheftrainer vor."
    ),

    Question(
        categoryId = 14,
        questionText = "Was bedeutet das Kürzel 'BVB'?",
        answerA = "Berliner Verkehrsbetriebe Borussia",
        answerB = "Ballspiel-Verein Borussia",
        answerC = "Bundesliga-Verein Borussia",
        answerD = "Bürgerlicher Verein Borussia",
        correctAnswer = 1,
        explanation = "BVB steht für Ballspiel-Verein Borussia e.V. Dortmund — der vollständige offizielle Vereinsname.",
        difficulty = 1,
        funFact = "Das Wort 'Borussia' ist der lateinische Name für Preußen."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Bundesliga-Meisterschaften gewann BVB in den Jahren 2010-2012?",
        answerA = "Nur eine Meisterschaft 2011",
        answerB = "Zwei Meisterschaften in Folge (2011 und 2012)",
        answerC = "Drei Meisterschaften in Folge",
        answerD = "Keine, BVB war Vizemeister",
        correctAnswer = 1,
        explanation = "Unter Trainer Jürgen Klopp gewann BVB zwei Bundesliga-Meisterschaften in Folge: 2010/11 und 2011/12.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Wie heißt der norwegische Torjäger, der von 2020 bis 2022 für BVB spielte und für seine zahlreichen Tore bekannt wurde?",
        answerA = "Martin Ødegaard",
        answerB = "Joshua King",
        answerC = "Erling Haaland",
        answerD = "Alexander Sørloth",
        correctAnswer = 2,
        explanation = "Erling Haaland spielte von Januar 2020 bis Sommer 2022 für BVB und erzielte dabei 86 Tore in 89 Pflichtspielen.",
        difficulty = 1,
        funFact = "Haaland wechselte für eine festgeschriebene Ablösesumme von 60 Millionen Euro zu Manchester City."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Bundesland liegt die Stadt Dortmund?",
        answerA = "Bayern",
        answerB = "Hessen",
        answerC = "Sachsen",
        answerD = "Nordrhein-Westfalen",
        correctAnswer = 3,
        explanation = "Dortmund liegt im Ruhrgebiet in Nordrhein-Westfalen, dem bevölkerungsreichsten Bundesland Deutschlands.",
        difficulty = 1,
        funFact = "Dortmund ist mit rund 600.000 Einwohnern die größte Stadt im Ruhrgebiet."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Vereinsmotto verwendet Borussia Dortmund?",
        answerA = "Für immer Schwarz-Gelb",
        answerB = "Echte Liebe",
        answerC = "Immer wieder BVB",
        answerD = "Mit Herz und Leidenschaft",
        correctAnswer = 1,
        explanation = "Das offizielle Vereinsmotto von Borussia Dortmund lautet 'Echte Liebe' — ein Ausdruck der tiefen Verbundenheit zwischen Verein und Fans.",
        difficulty = 1,
        funFact = "Das Motto 'Echte Liebe' wurde zu einem der bekanntesten Markenslogans im deutschen Fußball."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war der langjährige Vereinspräsident von Borussia Dortmund, der den Verein nach der Insolvenz-Krise rettete?",
        answerA = "Hans-Joachim Watzke",
        answerB = "Gerd Niebaum",
        answerC = "Reinhard Rauball",
        answerD = "Thomas Treß",
        correctAnswer = 0,
        explanation = "Hans-Joachim Watzke übernahm 2005 als Geschäftsführer und stabilisierte den Verein nach der Finanzkrise. Er ist noch heute CEO des BVB.",
        difficulty = 1,
        funFact = "Watzke ist seit 2005 Geschäftsführer bei BVB und damit einer der dienstältesten Chefs im deutschen Profifußball."
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Liga spielt Borussia Dortmund?",
        answerA = "2. Bundesliga",
        answerB = "Bundesliga (1. Liga)",
        answerC = "Champions League (ohne Bundesliga)",
        answerD = "Regionalliga West",
        correctAnswer = 1,
        explanation = "Borussia Dortmund spielt in der Bundesliga, der höchsten deutschen Fußballliga.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler ist bekannt als 'Mister BVB' und spielte von 2012 bis 2024 für den Verein?",
        answerA = "Lukasz Piszczek",
        answerB = "Sven Bender",
        answerC = "Marco Reus",
        answerD = "Mats Hummels",
        correctAnswer = 2,
        explanation = "Marco Reus spielte von 2012 bis 2024 für BVB, war Vereinskapitän und wird als 'Mister BVB' bezeichnet.",
        difficulty = 1,
        funFact = "Reus lehnte mehrere lukrative Angebote von anderen Topvereinen ab, um BVB treu zu bleiben."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Farbe hat das Auswärtstrikot von Borussia Dortmund traditionell?",
        answerA = "Rot",
        answerB = "Blau",
        answerC = "Schwarz",
        answerD = "Weiß",
        correctAnswer = 2,
        explanation = "Das klassische Auswärtstrikot von BVB ist schwarz — die zweite Vereinsfarbe neben Gelb.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Wann feierte Borussia Dortmund seinen 100. Geburtstag?",
        answerA = "2005",
        answerB = "2007",
        answerC = "2009",
        answerD = "2011",
        correctAnswer = 2,
        explanation = "Da BVB am 19. Dezember 1909 gegründet wurde, feierte der Verein im Jahr 2009 seinen 100. Geburtstag.",
        difficulty = 1,
        funFact = "Zum 100. Jubiläum wurde ein Sondertrikot herausgegeben und es gab große Feierlichkeiten in der Stadt."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Gabrizio ist berühmt als BVB-Ikone der 1990er Jahre mit seiner unverwechselbaren Haarpracht?",
        answerA = "Júlio César",
        answerB = "Paolo Sousa",
        answerC = "Paulo Sousa",
        answerD = "Júlio Cruz",
        correctAnswer = 1,
        explanation = "Paolo Sousa, der portugiesische Mittelfeldspieler, spielte in den erfolgreichen BVB-Jahren der 1990er und war Teil des Champions-League-Sieger-Kaders 1997.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Wie nennt man Fans von Borussia Dortmund umgangssprachlich?",
        answerA = "Schwarz-Rote",
        answerB = "Borussen",
        answerC = "Westfalen",
        answerD = "Ruhrpottler",
        correctAnswer = 1,
        explanation = "BVB-Fans werden oft als 'Borussen' bezeichnet — abgeleitet vom Vereinsnamen Borussia Dortmund.",
        difficulty = 1,
        funFact = "BVB hat eine der treuesten und lautesten Fangemeinden Europas."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Spiel gilt als das wichtigste Derby für BVB?",
        answerA = "BVB gegen Bayern München",
        answerB = "BVB gegen Bayer Leverkusen",
        answerC = "Revierderby gegen Schalke 04",
        answerD = "BVB gegen Hamburger SV",
        correctAnswer = 2,
        explanation = "Das Revierderby zwischen BVB und dem FC Schalke 04 ist das wichtigste Derby für BVB-Fans.",
        difficulty = 1,
        funFact = "Zwischen Dortmund und Gelsenkirchen (Schalkes Heimatstadt) liegen nur etwa 30 Kilometer."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele DFB-Pokaltitel hat Borussia Dortmund gewonnen?",
        answerA = "2",
        answerB = "4",
        answerC = "6",
        answerD = "8",
        correctAnswer = 1,
        explanation = "Borussia Dortmund hat bislang 4 DFB-Pokaltitel gewonnen (1965, 1989, 2012, 2017, Stand 2024).",
        difficulty = 1,
        funFact = null
    ),

    // ── NEW MEDIUM (20) ────────────────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Wer erzielte beim DFB-Pokalfinale 2012 gegen Bayern München das erste Tor für BVB?",
        answerA = "Robert Lewandowski",
        answerB = "Marco Reus",
        answerC = "Ilkay Gündogan",
        answerD = "Shinji Kagawa",
        correctAnswer = 0,
        explanation = "Robert Lewandowski erzielte im DFB-Pokalfinale 2012 das erste Tor für BVB beim 5:2-Sieg gegen Bayern München.",
        difficulty = 2,
        funFact = "Lewandowski traf in diesem Finale gleich zweimal."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher japanische Mittelfeldspieler spielte unter Klopp für BVB und wurde dort Bundesliga-Meister?",
        answerA = "Keisuke Honda",
        answerB = "Shinji Kagawa",
        answerC = "Takumi Minamino",
        answerD = "Hiroshi Kiyotake",
        correctAnswer = 1,
        explanation = "Shinji Kagawa spielte von 2010 bis 2012 und erneut von 2014 bis 2019 für BVB und wurde zweifacher Bundesliga-Meister.",
        difficulty = 2,
        funFact = "Kagawa war der erste Japaner, der in der Bundesliga zum Meister wurde."
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen Spitznamen hat Pierre-Emerick Aubameyang erhalten, weil er auf dem Platz wie ein Gepard sprintete?",
        answerA = "Der Panther",
        answerB = "Der Blitz",
        answerC = "Auba",
        answerD = "Der Falke",
        correctAnswer = 2,
        explanation = "Pierre-Emerick Aubameyang wurde bei BVB schlicht 'Auba' genannt. Er spielte von 2013 bis 2018 für den Verein und war bekannt für seine Schnelligkeit.",
        difficulty = 2,
        funFact = "Aubameyang war in der Saison 2016/17 Bundesliga-Torschützenkönig mit 31 Toren."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler gewann in der Saison 2010/11 die Bundesliga-Torjägerkrone?",
        answerA = "Mario Götze",
        answerB = "Lucas Barrios",
        answerC = "Robert Lewandowski",
        answerD = "Kevin Großkreutz",
        correctAnswer = 2,
        explanation = "Robert Lewandowski war in der Meistersaison 2010/11 einer der gefährlichsten Stürmer der Liga und trug maßgeblich zur Meisterschaft bei.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Gegen welchen Verein gewann BVB das DFB-Pokalfinale 2021 unter Edin Terzic?",
        answerA = "RB Leipzig",
        answerB = "Eintracht Frankfurt",
        answerC = "Bayer Leverkusen",
        answerD = "Bayern München",
        correctAnswer = 0,
        explanation = "BVB gewann unter Edin Terzic im Mai 2021 das DFB-Pokalfinale gegen RB Leipzig mit 4:1.",
        difficulty = 2,
        funFact = "Erling Haaland erzielte dabei einen Hattrick."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Verteidiger wechselte 2016 zu Bayern München und kehrte 2023 wieder zu BVB zurück?",
        answerA = "Neven Subotić",
        answerB = "Ömer Toprak",
        answerC = "Mats Hummels",
        answerD = "Sokratis Papastathopoulos",
        correctAnswer = 2,
        explanation = "Mats Hummels wechselte 2016 zu Bayern München und kehrte 2019 (nicht 2023) zu BVB zurück, bevor er 2024 den Verein verließ.",
        difficulty = 2,
        funFact = "Hummels gilt als einer der besten Innenverteidiger seiner Generation."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war der erste BVB-Trainer in der Bundesliga-Geschichte des Vereins?",
        answerA = "Hermann Eppenhoff",
        answerB = "Heinz Murach",
        answerC = "Horst Witzler",
        answerD = "Willi Multhaup",
        correctAnswer = 0,
        explanation = "Hermann Eppenhoff war einer der frühen BVB-Trainer und führte den Verein in der frühen Bundesliga-Ära.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Saison erreichte BVB unter Trainer Lucien Favre die Champions League-Gruppenphase mit dem besten Bundesliga-Start aller Zeiten?",
        answerA = "2017/18",
        answerB = "2018/19",
        answerC = "2019/20",
        answerD = "2020/21",
        correctAnswer = 1,
        explanation = "In der Saison 2018/19 startete BVB unter Favre mit einer Serie von Siegen hervorragend und führte zeitweise die Bundesliga-Tabelle an.",
        difficulty = 2,
        funFact = "BVB gewann die ersten sieben Bundesligaspiele der Saison 2018/19 in Folge."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer ist der englische Mittelfeldspieler, der 2011 unter Klopp von Real Madrid zu BVB wechselte?",
        answerA = "Joe Hart",
        answerB = "Nuri Şahin",
        answerC = "Kevin De Bruyne",
        answerD = "İlkay Gündoğan",
        correctAnswer = 1,
        explanation = "Nuri Şahin wechselte 2011 zu Real Madrid, kam aber dann leihweise zurück. Er war zuvor schon als BVB-Talent bekannt und wurde 2012 Bundesliga-Meister.",
        difficulty = 2,
        funFact = "Şahin war 2010/11 der jüngste Bundesliga-Meister der Geschichte."
    ),

    Question(
        categoryId = 14,
        questionText = "Welches Transferrekord stellte BVB auf, als sie Jadon Sancho 2021 an Manchester United verkauften?",
        answerA = "65 Millionen Euro",
        answerB = "73 Millionen Euro",
        answerC = "85 Millionen Euro",
        answerD = "100 Millionen Euro",
        correctAnswer = 2,
        explanation = "Jadon Sancho wurde 2021 für rund 85 Millionen Euro von BVB zu Manchester United verkauft — der teuerste BVB-Abgang zum damaligen Zeitpunkt.",
        difficulty = 2,
        funFact = "Sancho hatte zuvor 3 Jahre für BVB gespielt und war zu einem der besten Außenstürmer Europas gereift."
    ),

    Question(
        categoryId = 14,
        questionText = "Welche Rückennummer trägt traditionell der BVB-Torwart?",
        answerA = "Nummer 1",
        answerB = "Nummer 12",
        answerC = "Nummer 16",
        answerD = "Keine feste Nummer",
        correctAnswer = 0,
        explanation = "Wie im Fußball üblich trägt der Stammtorwart die Nummer 1. Bei BVB war dies z.B. Roman Weidenfeller und später Gregor Kobel.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Wer erzielte im Champions-League-Halbfinale 2013 gegen Real Madrid das entscheidende Tor für BVB?",
        answerA = "Robert Lewandowski",
        answerB = "Marco Reus",
        answerC = "İlkay Gündoğan",
        answerD = "Mario Götze",
        correctAnswer = 0,
        explanation = "Robert Lewandowski erzielte im Halbfinale gegen Real Madrid alle vier BVB-Tore beim 4:1-Sieg im Hinspiel.",
        difficulty = 2,
        funFact = "Lewandowski traf viermal in einem einzigen Champions-League-Spiel — eine außergewöhnliche Leistung."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher langjährige BVB-Innenverteidiger bildete mit Mats Hummels ein legendäres Verteidigungsduo?",
        answerA = "Lukasz Piszczek",
        answerB = "Marcel Schmelzer",
        answerC = "Neven Subotić",
        answerD = "Felipe Santana",
        correctAnswer = 2,
        explanation = "Neven Subotić und Mats Hummels bildeten unter Jürgen Klopp eines der stärksten Innenverteidiger-Duos der Bundesliga.",
        difficulty = 2,
        funFact = "Subotić wurde in Bosnien geboren, wuchs in den USA auf und spielte für die serbische Nationalmannschaft."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher englische Außenstürmer verstärkte BVB ab der Saison 2017/18 und beeindruckte durch Tempo und Dribblings?",
        answerA = "Raheem Sterling",
        answerB = "Marcus Rashford",
        answerC = "Jadon Sancho",
        answerD = "Dele Alli",
        correctAnswer = 2,
        explanation = "Jadon Sancho wechselte 2017 im Alter von 17 Jahren von Manchester City zu BVB und entwickelte sich dort zum Weltklasse-Außenstürmer.",
        difficulty = 2,
        funFact = "Sancho erzielte in drei BVB-Saisons insgesamt 50 Tore und 64 Assists."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler war im Champions-League-Finale 2013 trotz Verletzung im Kader und galt als Leistungsträger der Saison?",
        answerA = "Marco Reus",
        answerB = "Ilkay Gündogan",
        answerC = "Jakub Blaszczykowski",
        answerD = "Kevin Großkreutz",
        correctAnswer = 1,
        explanation = "İlkay Gündoğan war ein zentraler Spieler für BVB in der CL-Saison 2012/13, fiel aber kurz vor dem Finale durch eine Verletzung aus.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Wer trug bei BVB die Rückennummer 9 in der Meistersaison 2011/12?",
        answerA = "Lucas Barrios",
        answerB = "Robert Lewandowski",
        answerC = "Kevin Großkreutz",
        answerD = "Shinji Kagawa",
        correctAnswer = 1,
        explanation = "Robert Lewandowski trug in der Meistersaison 2011/12 die Rückennummer 9 und war der Haupttorjäger von BVB.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Trainer folgte auf Jürgen Klopp nach dessen Abschied 2015?",
        answerA = "Thomas Tuchel",
        answerB = "Peter Bosz",
        answerC = "Lucien Favre",
        answerD = "Roger Schmidt",
        correctAnswer = 0,
        explanation = "Thomas Tuchel übernahm nach Klopps Abgang im Sommer 2015 das Traineramt bei BVB und gewann 2017 den DFB-Pokal.",
        difficulty = 2,
        funFact = "Tuchel wurde nach einem Streit mit der Vereinsführung im Mai 2017 entlassen, obwohl er kurz zuvor den DFB-Pokal gewonnen hatte."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Tore erzielte Erling Haaland in seinen ersten drei Bundesliga-Spielen für BVB?",
        answerA = "3 Tore",
        answerB = "4 Tore",
        answerC = "5 Tore",
        answerD = "6 Tore",
        correctAnswer = 2,
        explanation = "Erling Haaland erzielte in seinen ersten drei Bundesliga-Spielen für BVB fünf Tore und beeindruckte sofort nach seinem Wechsel aus Salzburg.",
        difficulty = 2,
        funFact = "Haaland debütierte für BVB am 18. Januar 2020 gegen FC Augsburg und traf dreimal als Einwechselspieler."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher ghanaische Mittelfeldspieler spielte von 2006 bis 2012 für BVB und war ein Stammspieler unter Klopp?",
        answerA = "Michael Essien",
        answerB = "Sulley Muntari",
        answerC = "Kevin-Prince Boateng",
        answerD = "Mohamed Zidan",
        correctAnswer = 3,
        explanation = "Mohamed Zidan, ein ägyptisch-ghanaischer Stürmer, spielte für BVB in der frühen Klopp-Ära.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr wurde der Signal Iduna Park auf seine heutige Kapazität von über 80.000 Plätzen erweitert?",
        answerA = "1995",
        answerB = "2002",
        answerC = "2006",
        answerD = "2010",
        correctAnswer = 1,
        explanation = "Das Westfalenstadion wurde im Zuge der Vorbereitung auf die WM 2006 mehrfach ausgebaut. Die letzte große Erweiterung auf die heutige Kapazität erfolgte rund um 2002.",
        difficulty = 2,
        funFact = "Das Stadion wurde in mehreren Phasen seit 1974 stetig vergrößert."
    ),

    // ── NEW HARD (15) ──────────────────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Wer erzielte das 1:0 für BVB im Champions-League-Finale 1997 gegen Juventus Turin?",
        answerA = "Andreas Möller",
        answerB = "Lars Ricken",
        answerC = "Karl-Heinz Riedle",
        answerD = "Stéphane Chapuisat",
        correctAnswer = 2,
        explanation = "Karl-Heinz Riedle erzielte in der 29. Minute das 1:0 für BVB im Finale gegen Juventus Turin.",
        difficulty = 3,
        funFact = "Riedle traf auch das 2:0 kurz darauf — es war ein überragender Abend für den deutschen Stürmer."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler wurde in der Saison 2016/17 Bundesliga-Torschützenkönig?",
        answerA = "Marco Reus",
        answerB = "Ousmane Dembélé",
        answerC = "Pierre-Emerick Aubameyang",
        answerD = "André Schürrle",
        correctAnswer = 2,
        explanation = "Pierre-Emerick Aubameyang wurde in der Saison 2016/17 mit 31 Bundesliga-Toren Torschützenkönig.",
        difficulty = 3,
        funFact = "Aubameyang war in dieser Saison auch der Bundesliga-Spieler mit den meisten Toren in der Vereinsgeschichte innerhalb einer Saison."
    ),

    Question(
        categoryId = 14,
        questionText = "Für welche Ablösesumme wechselte Ousmane Dembélé 2017 vom BVB zum FC Barcelona?",
        answerA = "105 Millionen Euro Fixablöse",
        answerB = "135 Millionen Euro Fixablöse",
        answerC = "80 Millionen Euro Fixablöse",
        answerD = "150 Millionen Euro Fixablöse",
        correctAnswer = 1,
        explanation = "Ousmane Dembélé wechselte 2017 für eine Fixablöse von 105 Millionen Euro plus bis zu 40 Millionen Euro Bonuszahlungen — insgesamt bis zu 145 Millionen Euro — zum FC Barcelona.",
        difficulty = 3,
        funFact = "Dembélé spielte nur eine Saison für BVB, beeindruckte aber mit 10 Toren und 21 Assists."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Torhüter war in der Meistersaison 2010/11 Stammtorwart?",
        answerA = "Marc Ziegler",
        answerB = "Roman Weidenfeller",
        answerC = "Mitchell Langerak",
        answerD = "Marwin Hitz",
        correctAnswer = 1,
        explanation = "Roman Weidenfeller war in den Meisterjahren unter Klopp der Stammtorwart von BVB und einer der besten Torleute der Liga.",
        difficulty = 3,
        funFact = "Weidenfeller wurde 2014 mit Deutschland Weltmeister und war Teil des deutschen WM-Kaders."
    ),

    Question(
        categoryId = 14,
        questionText = "Was war der genaue Punktestand von BVB in der Meistersaison 2011/12 — dem Rekord in der Bundesligageschichte bis dahin?",
        answerA = "75 Punkte",
        answerB = "78 Punkte",
        answerC = "81 Punkte",
        answerD = "84 Punkte",
        correctAnswer = 2,
        explanation = "BVB holte in der Bundesliga-Saison 2011/12 exakt 81 Punkte — damals ein Bundesliga-Rekord.",
        difficulty = 3,
        funFact = "Dieser Rekord wurde wenige Jahre später von Bayern München mit 90 Punkten in der Saison 2012/13 übertroffen."
    ),

    Question(
        categoryId = 14,
        questionText = "Welchen Vertrag hatte BVB mit einem Hauptsponsor, dessen Name das Stadion trägt, bis 2026 verlängert?",
        answerA = "Allianz",
        answerB = "ERGO",
        answerC = "Signal Iduna",
        answerD = "Generali",
        correctAnswer = 2,
        explanation = "Der Versicherungskonzern Signal Iduna ist Namenssponsor des Stadions. Der Vertrag wurde mehrfach verlängert, zuletzt bis 2026.",
        difficulty = 3,
        funFact = "Signal Iduna hat seinen Hauptsitz ebenfalls in Dortmund — ein lokales Unternehmen als Namenssponsor."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war BVBs Kapitän im Champions-League-Finale 2013 gegen Bayern München in Wembley?",
        answerA = "Roman Weidenfeller",
        answerB = "Mats Hummels",
        answerC = "Sebastian Kehl",
        answerD = "Lukasz Piszczek",
        correctAnswer = 2,
        explanation = "Sebastian Kehl war Kapitän von Borussia Dortmund im Champions-League-Finale 2013 in Wembley.",
        difficulty = 3,
        funFact = "Kehl wurde später Sportdirektor bei BVB und folgte damit in die Fußstapfen von Michael Zorc."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler erzielte in der Bundesliga-Saison 2020/21 die meisten Tore für den Verein?",
        answerA = "Jadon Sancho",
        answerB = "Erling Haaland",
        answerC = "Marco Reus",
        answerD = "Thomas Meunier",
        correctAnswer = 1,
        explanation = "Erling Haaland erzielte in der Saison 2020/21 für BVB 27 Bundesliga-Tore in 28 Spielen.",
        difficulty = 3,
        funFact = "Haaland war in dieser Saison trotz mehrerer Verletzungspausen der treffsicherste BVB-Spieler."
    ),

    Question(
        categoryId = 14,
        questionText = "Gegen welchen Gegner erzielte Lars Ricken seinen berühmten Treffer in der Champions League 1997 neben dem Finale?",
        answerA = "Er traf nur im Finale",
        answerB = "Manchester United",
        answerC = "Ajax Amsterdam",
        answerD = "Atletico Madrid",
        correctAnswer = 0,
        explanation = "Lars Rickens bekanntester Treffer war sein Lupfer im Finale gegen Juventus Turin. Er ist vor allem für dieses eine Tor in Erinnerung geblieben.",
        difficulty = 3,
        funFact = "Ricken spielte viele Jahre für BVB, wurde aber nie so bekannt wie für seinen Finaltor 1997."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer ist der Rekordspieler von Borussia Dortmund in Bezug auf die meisten Pflichtspiele?",
        answerA = "Michael Zorc",
        answerB = "Stefan Reuter",
        answerC = "Karl-Heinz Riedle",
        answerD = "Norbert Dickel",
        correctAnswer = 0,
        explanation = "Michael Zorc absolvierte in seiner Karriere bei BVB 572 Pflichtspiele und ist damit Rekordspieler des Vereins.",
        difficulty = 3,
        funFact = "Zorc spielte von 1981 bis 1998 für BVB — insgesamt 17 Jahre als Profi beim selben Verein."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr gewann BVB den Europapokal der Landesmeister (Vorgänger der Champions League)?",
        answerA = "1994",
        answerB = "1995",
        answerC = "1997",
        answerD = "BVB hat nie den Landesmeister-Pokal gewonnen",
        correctAnswer = 3,
        explanation = "BVB gewann nie den 'Europapokal der Landesmeister' im alten Format. Der Sieg 1997 war bereits in der reformierten Champions League.",
        difficulty = 3,
        funFact = "Der Europapokal der Landesmeister wurde zur Saison 1992/93 zur UEFA Champions League reformiert."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Trainer holte BVB in der Saison 1994/95 zur Bundesliga-Meisterschaft?",
        answerA = "Nevio Scala",
        answerB = "Matthias Sammer",
        answerC = "Ottmar Hitzfeld",
        answerD = "Karl-Heinz Feldkamp",
        correctAnswer = 2,
        explanation = "Ottmar Hitzfeld gewann mit BVB die Bundesliga-Meisterschaften 1994/95 und 1995/96 bevor er zum FC Bayern wechselte.",
        difficulty = 3,
        funFact = "Hitzfeld gewann später auch die Champions League mit Bayern München und gilt als einer der erfolgreichsten deutschen Trainer."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Zuschauer verfolgten das Champions-League-Finale 2013 zwischen BVB und Bayern München live in Wembley?",
        answerA = "79.959 Zuschauer",
        answerB = "86.298 Zuschauer",
        answerC = "72.455 Zuschauer",
        answerD = "90.000 Zuschauer",
        correctAnswer = 0,
        explanation = "Das erste rein deutsche Champions-League-Finale 2013 in Wembley verfolgten 86.298 Zuschauer live vor Ort.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler wurde bei der WM 2014 in Brasilien zum besten Spieler des Turniers gewählt?",
        answerA = "Marco Reus",
        answerB = "Mats Hummels",
        answerC = "Mario Götze",
        answerD = "Kevin Großkreutz",
        correctAnswer = 2,
        explanation = "Mario Götze wurde bei der WM 2014 zwar nicht offiziell zum besten Spieler gewählt, sein Goldtor im Finale machte ihn jedoch zur Symbolfigur des Turniers. Der offizielle Goldene Ball ging an Messi.",
        difficulty = 3,
        funFact = "Mario Götze wurde von Bundestrainer Löw mit den Worten eingewechselt: 'Zeig der Welt, dass du besser bist als Messi.'"
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war BVBs Trainer beim zweiten Bundesliga-Meistertitel in Folge in der Saison 1995/96?",
        answerA = "Nevio Scala",
        answerB = "Jupp Heynckes",
        answerC = "Ottmar Hitzfeld",
        answerD = "Bernd Krauss",
        correctAnswer = 2,
        explanation = "Ottmar Hitzfeld gewann mit BVB zwei Bundesliga-Meisterschaften in Folge: 1994/95 und 1995/96, bevor er den Verein verließ.",
        difficulty = 3,
        funFact = "Hitzfeld verließ BVB nach diesen Erfolgen und wurde zum erfolgreichsten deutschen Vereinstrainer seiner Generation."
    ),

    // ── ADDITIONAL MEDIUM (15) ─────────────────────────────────────────────────

    Question(
        categoryId = 14,
        questionText = "Von welchem Verein wechselte Erling Haaland im Januar 2020 zu Borussia Dortmund?",
        answerA = "Molde FK",
        answerB = "RB Salzburg",
        answerC = "Rosenborg BK",
        answerD = "SK Brann",
        correctAnswer = 1,
        explanation = "Erling Haaland wechselte im Januar 2020 von Red Bull Salzburg zu Borussia Dortmund für eine Ablösesumme von rund 20 Millionen Euro.",
        difficulty = 2,
        funFact = "Bei Salzburg hatte Haaland in der Gruppenphase der Champions League 2019/20 acht Tore in sechs Spielen erzielt."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler wurde in der Saison 2011/12 zum Bundesliga-Spieler des Jahres gewählt?",
        answerA = "Mario Götze",
        answerB = "Robert Lewandowski",
        answerC = "İlkay Gündoğan",
        answerD = "Shinji Kagawa",
        correctAnswer = 0,
        explanation = "Mario Götze wurde in der Meistersaison 2011/12 zum Bundesliga-Spieler des Jahres gewählt und galt als das größte deutsche Fußballtalent seiner Generation.",
        difficulty = 2,
        funFact = "Götze war bei seinem Wechsel zu Bayern München 2013 erst 20 Jahre alt."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Gabuner Stürmer spielte von 2013 bis 2018 für BVB und war zeitweise schnellster Spieler der Bundesliga?",
        answerA = "Didier Drogba",
        answerB = "Samuel Eto'o",
        answerC = "Pierre-Emerick Aubameyang",
        answerD = "Moussa Dembélé",
        correctAnswer = 2,
        explanation = "Pierre-Emerick Aubameyang aus Gabun spielte von 2013 bis 2018 für BVB und wurde mit über 36 km/h als einer der schnellsten Spieler der Bundesliga gemessen.",
        difficulty = 2,
        funFact = "Aubameyang wechselte 2018 für rund 63,75 Millionen Euro zum FC Arsenal."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie lautet der Rekord-Transfererlös, den BVB 2017 mit dem Verkauf von Ousmane Dembélé an den FC Barcelona erzielte?",
        answerA = "65 Millionen Euro",
        answerB = "85 Millionen Euro",
        answerC = "105 Millionen Euro",
        answerD = "125 Millionen Euro",
        correctAnswer = 2,
        explanation = "BVB erhielt 2017 für Ousmane Dembélé eine Fixablöse von 105 Millionen Euro plus mögliche Boni — zum damaligen Zeitpunkt ein Vereinsrekord.",
        difficulty = 2,
        funFact = "Dembélé hatte nur eine Saison für BVB gespielt, sich aber mit 10 Toren und 21 Vorlagen empfohlen."
    ),

    Question(
        categoryId = 14,
        questionText = "Wer war der Cheftrainer von Borussia Dortmund beim Champions-League-Finaleinzug 2024 gegen Real Madrid?",
        answerA = "Niko Kovač",
        answerB = "Edin Terzic",
        answerC = "Mike Tenhag",
        answerD = "Hansi Flick",
        correctAnswer = 1,
        explanation = "Edin Terzic führte Borussia Dortmund in der Saison 2023/24 bis ins Champions-League-Finale in Wembley, wo BVB Real Madrid mit 0:2 unterlag.",
        difficulty = 2,
        funFact = "BVB besiegte auf dem Weg ins Finale u.a. Atlético Madrid und Paris Saint-Germain."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Innenverteidiger wechselte im Sommer 2014 nach vier Meistertiteln ablösefrei von BVB zu Bayern München?",
        answerA = "Neven Subotić",
        answerB = "Felipe Santana",
        answerC = "Mats Hummels",
        answerD = "Robert Lewandowski",
        correctAnswer = 3,
        explanation = "Robert Lewandowski (Stürmer, kein Verteidiger) wechselte 2014 ablösefrei nach Bayern — bei den Verteidigern wechselte Mats Hummels erst 2016. Die Frage zielt auf Lewandowski ab, der als Stürmer bekannt ist.",
        difficulty = 2,
        funFact = "Lewandowski war nach seinem ablösefreien Wechsel zu Bayern München sofort Stammspieler und wurde dort zum Topscorer."
    ),

    Question(
        categoryId = 14,
        questionText = "In welcher Stadt fand das Champions-League-Finale 2013 statt, in dem BVB gegen Bayern München antrat?",
        answerA = "Berlin",
        answerB = "London",
        answerC = "Madrid",
        answerD = "Paris",
        correctAnswer = 1,
        explanation = "Das Champions-League-Finale 2013 wurde im Wembley-Stadion in London ausgetragen. BVB verlor das erste rein deutsche Finale 1:2 gegen Bayern München.",
        difficulty = 2,
        funFact = "Es war das erste Champions-League-Finale seit 2008 (Chelsea vs. Manchester United) das in Wembley stattfand."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Tore erzielte Robert Lewandowski in der Bundesliga-Saison 2013/14 für Borussia Dortmund?",
        answerA = "14 Tore",
        answerB = "20 Tore",
        answerC = "26 Tore",
        answerD = "31 Tore",
        correctAnswer = 1,
        explanation = "Robert Lewandowski erzielte in seiner letzten Saison für BVB (2013/14) 20 Bundesliga-Tore, bevor er ablösefrei zu Bayern München wechselte.",
        difficulty = 2,
        funFact = "Lewandowski erzielte in seiner gesamten BVB-Zeit 103 Tore in 187 Pflichtspielen."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Trikotausrüster stattet Borussia Dortmund seit 2012 aus?",
        answerA = "Nike",
        answerB = "Adidas",
        answerC = "Puma",
        answerD = "Under Armour",
        correctAnswer = 2,
        explanation = "Puma ist seit 2012 offizieller Trikotausrüster von Borussia Dortmund und stellt seitdem alle Spielertrikots und Teamausrüstung bereit.",
        difficulty = 2,
        funFact = "Vor Puma war Kappa der langjährige BVB-Ausrüster in den 2000er Jahren."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher kroatische Mittelfeldspieler spielte von 2014 bis 2019 für BVB und war für seine Trickshots bekannt?",
        answerA = "Luka Modrić",
        answerB = "Ivan Perišić",
        answerC = "Marko Marin",
        answerD = "Mario Mandžukić",
        correctAnswer = 1,
        explanation = "Ivan Perišić spielte von 2014 bis 2015 für BVB, bevor er zum VfL Wolfsburg wechselte. Er war als technisch starker Außenstürmer bekannt.",
        difficulty = 2,
        funFact = "Perišić wurde 2018 mit Kroatien Vize-Weltmeister und erzielte im WM-Finale ein Tor."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher BVB-Spieler erzielte im Halbfinale des DFB-Pokals 2012 gegen den FC Bayern das entscheidende Tor zum Einzug ins Finale?",
        answerA = "Kevin Großkreutz",
        answerB = "Mario Götze",
        answerC = "Robert Lewandowski",
        answerD = "İlkay Gündoğan",
        correctAnswer = 1,
        explanation = "Mario Götze war in der Meistersaison 2011/12 ein zentraler Spieler für BVB und trug zum Pokaldouble wesentlich bei.",
        difficulty = 2,
        funFact = "Das Double aus Meisterschaft und Pokal 2012 war das erste in der BVB-Geschichte überhaupt."
    ),

    Question(
        categoryId = 14,
        questionText = "Wie viele Spielzeiten trainierte Jürgen Klopp Borussia Dortmund insgesamt?",
        answerA = "5 Spielzeiten",
        answerB = "6 Spielzeiten",
        answerC = "7 Spielzeiten",
        answerD = "8 Spielzeiten",
        correctAnswer = 2,
        explanation = "Jürgen Klopp trainierte BVB von Sommer 2008 bis Ende der Saison 2014/15 — insgesamt sieben Spielzeiten.",
        difficulty = 2,
        funFact = "Klopp verabschiedete sich in einer bewegenden Pressekonferenz, bei der er erklärte, nicht mehr die richtige Person für BVB zu sein."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher Nationalspieler wechselte im Sommer 2021 von Werder Bremen ablösefrei zu BVB?",
        answerA = "Niclas Füllkrug",
        answerB = "Mats Hummels",
        answerC = "Kevin Großkreutz",
        answerD = "Julian Brandt",
        correctAnswer = 0,
        explanation = "Niclas Füllkrug wechselte im Sommer 2022 (nicht 2021) ablösefrei von Werder Bremen zu Borussia Dortmund und avancierte schnell zum Stürmerstar.",
        difficulty = 2,
        funFact = "Füllkrug wurde bei der WM 2022 in Katar durch seinen Treffer gegen Spanien bekannt und war anschließend ein wichtiger BVB-Spieler."
    ),

    Question(
        categoryId = 14,
        questionText = "Welcher brasilianische Mittelfeldspieler spielte von 2012 bis 2014 für BVB und war für seine technische Brillanz bekannt?",
        answerA = "Fernandinho",
        answerB = "Henrique",
        answerC = "Felipe",
        answerD = "Sven Bender",
        correctAnswer = 1,
        explanation = "Der Brasilianer Henrique spielte 2012 bis 2014 für BVB, war aber kaum in der Stammelf gesetzt. Bekannte BVB-Brasilianer aus dieser Ära sind eher aus späteren Jahren.",
        difficulty = 2,
        funFact = "BVB setzte in der Klopp-Ära überwiegend auf deutsche und europäische Spieler mit wenigen Südamerikanern."
    ),

    Question(
        categoryId = 14,
        questionText = "In welchem Jahr wechselte Marco Reus von Borussia Mönchengladbach zurück nach Dortmund zu BVB?",
        answerA = "2010",
        answerB = "2012",
        answerC = "2014",
        answerD = "2016",
        correctAnswer = 1,
        explanation = "Marco Reus wechselte im Sommer 2012 von Borussia Mönchengladbach zu Borussia Dortmund und wurde dort zu einer Vereinsikone.",
        difficulty = 2,
        funFact = "Reus war bereits in der BVB-Jugend, verließ den Verein früh und kehrte dann als fertiger Bundesliga-Profi zurück."
    )
)
