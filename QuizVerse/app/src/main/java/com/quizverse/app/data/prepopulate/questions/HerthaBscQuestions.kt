package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun herthaBscQuestions(): List<Question> = listOf(

    // ── EASY (1) ──────────────────────────────────────────────────────────────

    Question(
        categoryId = 13,
        questionText = "In welcher Stadt spielt Hertha BSC?",
        answerA = "Hamburg",
        answerB = "Berlin",
        answerC = "München",
        answerD = "Leipzig",
        correctAnswer = 1,
        explanation = "Hertha BSC ist der bekannteste Fußballverein der Hauptstadt Berlin.",
        difficulty = 1,
        funFact = "Hertha BSC wird oft als 'die alte Dame' bezeichnet – ein liebevoller Spitzname für den traditionsreichen Klub."
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Farben trägt Hertha BSC in seinem Vereinswappen?",
        answerA = "Rot und Weiß",
        answerB = "Blau und Weiß",
        answerC = "Schwarz und Gold",
        answerD = "Grün und Weiß",
        correctAnswer = 1,
        explanation = "Herthas Vereinsfarben sind Blau und Weiß, die sich durch das gesamte Erscheinungsbild des Klubs ziehen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Stadion trägt Hertha BSC seine Heimspiele aus?",
        answerA = "Allianz Arena",
        answerB = "Signal Iduna Park",
        answerC = "Olympiastadion Berlin",
        answerD = "Volksparkstadion",
        correctAnswer = 2,
        explanation = "Das Olympiastadion Berlin ist seit vielen Jahren die Heimstätte von Hertha BSC und fasst über 74.000 Zuschauer.",
        difficulty = 1,
        funFact = "Das Olympiastadion wurde ursprünglich für die Olympischen Spiele 1936 in Berlin gebaut."
    ),

    Question(
        categoryId = 13,
        questionText = "Was bedeutet das Kürzel 'BSC' in Hertha BSC?",
        answerA = "Berliner Sport Club",
        answerB = "Berliner Schwimm Club",
        answerC = "Berliner Spiel Club",
        answerD = "Berliner Sportverein Club",
        correctAnswer = 0,
        explanation = "BSC steht für Berliner Sport-Club, der 1892 mit dem BFC Hertha fusionierte.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welcher Liga spielte Hertha BSC in der Saison 2023/24?",
        answerA = "Bundesliga",
        answerB = "2. Bundesliga",
        answerC = "3. Liga",
        answerD = "Regionalliga",
        correctAnswer = 1,
        explanation = "Nach dem Abstieg 2023 spielte Hertha BSC in der Saison 2023/24 in der 2. Bundesliga.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie lautet der offizielle Vereinsname von Hertha BSC?",
        answerA = "Hertha Berliner Sport-Club e.V.",
        answerB = "Hertha Berliner SC 1892 e.V.",
        answerC = "Hertha BSC GmbH & Co. KGaA",
        answerD = "Berliner Hertha Sport Club e.V.",
        correctAnswer = 1,
        explanation = "Der vollständige Name lautet Hertha Berliner SC 1892 e.V. – die Jahreszahl verweist auf das Gründungsjahr.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Berliner Verein ist der größte Rivale von Hertha BSC?",
        answerA = "FC Union Berlin",
        answerB = "Tennis Borussia Berlin",
        answerC = "BFC Dynamo",
        answerD = "Berliner AK",
        correctAnswer = 0,
        explanation = "Das Stadtderby zwischen Hertha BSC und dem 1. FC Union Berlin ist das bekannteste Berliner Fußballduell.",
        difficulty = 1,
        funFact = "Das erste Bundesliga-Derby zwischen Hertha und Union fand erst 2019/20 statt, nachdem Union aufgestiegen war."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie lautet der Spitzname von Hertha BSC?",
        answerA = "Die Hauptstädter",
        answerB = "Die alte Dame",
        answerC = "Die Blauen",
        answerD = "Die Adler",
        correctAnswer = 1,
        explanation = "Hertha BSC wird liebevoll 'die alte Dame' genannt – ein Ausdruck der Zuneigung für den traditionsreichen Verein.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr wurde Hertha BSC gegründet?",
        answerA = "1888",
        answerB = "1895",
        answerC = "1892",
        answerD = "1900",
        correctAnswer = 2,
        explanation = "Hertha BSC wurde am 25. Juli 1892 in Berlin gegründet.",
        difficulty = 1,
        funFact = "Der Verein wurde von einer Gruppe junger Männer gegründet, die das Fußballspiel in Berlin populär machen wollten."
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Farbe hat das Heimtrikot von Hertha BSC hauptsächlich?",
        answerA = "Rot",
        answerB = "Weiß",
        answerC = "Blau",
        answerD = "Schwarz",
        correctAnswer = 2,
        explanation = "Das Heimtrikot von Hertha BSC ist hauptsächlich blau – passend zu den Vereinsfarben Blau und Weiß.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Berliner Bezirk liegt das Olympiastadion?",
        answerA = "Mitte",
        answerB = "Charlottenburg",
        answerC = "Spandau",
        answerD = "Schöneberg",
        correctAnswer = 1,
        explanation = "Das Olympiastadion Berlin liegt im Bezirk Charlottenburg-Wilmersdorf, genauer im Ortsteil Westend.",
        difficulty = 1,
        funFact = "Das Olympiastadion ist über die S-Bahn-Station 'Olympiastadion' bequem erreichbar."
    ),

    Question(
        categoryId = 13,
        questionText = "Welches Symbol ziert das Wappen von Hertha BSC?",
        answerA = "Ein Adler",
        answerB = "Ein Löwe",
        answerC = "Ein Bär",
        answerD = "Ein Drache",
        correctAnswer = 0,
        explanation = "Das Hertha-BSC-Wappen zeigt einen blauen Adler mit ausgebreiteten Schwingen auf weißem Grund.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie heißt die offizielle Vereinshymne von Hertha BSC?",
        answerA = "Blau-Weiß für immer",
        answerB = "Nur nach Hause",
        answerC = "Herthaner",
        answerD = "Alte Dame Berlin",
        correctAnswer = 2,
        explanation = "Die bekannteste Vereinshymne von Hertha BSC trägt den Titel 'Herthaner' – ein Aufruf an alle Fans und Mitglieder.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Berliner Senat hat das Olympiastadion für die Olympischen Spiele 1936 bauen lassen?",
        answerA = "Das Deutsche Reich unter Adolf Hitler",
        answerB = "Die Weimarer Republik",
        answerC = "Der Berliner Stadtrat",
        answerD = "Das Königreich Preußen",
        correctAnswer = 0,
        explanation = "Das Olympiastadion Berlin wurde unter dem NS-Regime als Schauplatz der Olympischen Sommerspiele 1936 gebaut.",
        difficulty = 1,
        funFact = "Der Architekt Werner March entwarf das Stadion im neoklassizistischen Stil auf Wunsch von Albert Speer."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie wird der Eingangsbereich des Olympiastadions Berlin genannt?",
        answerA = "Das Marathontor",
        answerB = "Das Osttor",
        answerC = "Das Berliner Tor",
        answerD = "Das Olympisches Tor",
        correctAnswer = 0,
        explanation = "Das Marathontor ist das markante Osttor des Olympiastadions, durch das früher die Marathonläufer einzogen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wer ist der aktuelle Namensgeber-Sponsor des Olympiastadions Berlin (Stand 2024)?",
        answerA = "Telekom",
        answerB = "Mercedes-Benz",
        answerC = "Das Stadion hat keinen Namenssponsor",
        answerD = "Vodafone",
        correctAnswer = 2,
        explanation = "Das Olympiastadion Berlin trägt keinen Sponsornamen – es heißt offiziell weiterhin 'Olympiastadion Berlin'.",
        difficulty = 1,
        funFact = "Das Festhalten am historischen Namen ist ein bewusster Entscheid, um die Identität des UNESCO-Baudenkmals zu schützen."
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Farbe hat das Auswärtstrikot von Hertha BSC traditionell?",
        answerA = "Schwarz",
        answerB = "Weiß",
        answerC = "Grau",
        answerD = "Gelb",
        correctAnswer = 1,
        explanation = "Das traditionelle Auswärtstrikot von Hertha BSC ist weiß – die zweite Vereinsfarbe neben Blau.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welcher deutschen Hauptstadt hat Hertha BSC seinen Sitz?",
        answerA = "Bonn",
        answerB = "Berlin",
        answerC = "Hamburg",
        answerD = "Frankfurt",
        correctAnswer = 1,
        explanation = "Hertha BSC ist der traditionsreichste Fußballverein Berlins, der deutschen Hauptstadt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie viele Punkte erhält eine Mannschaft in der Bundesliga für einen Sieg?",
        answerA = "2 Punkte",
        answerB = "3 Punkte",
        answerC = "1 Punkt",
        answerD = "4 Punkte",
        correctAnswer = 1,
        explanation = "Seit 1995 gilt in der Bundesliga die Drei-Punkte-Regel: Sieg = 3 Punkte, Unentschieden = 1 Punkt, Niederlage = 0 Punkte.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie lautet der Vereinsschlachtruf der Hertha-BSC-Fans?",
        answerA = "Blau-Weiß, wie lieb ich dich!",
        answerB = "Ha-Ho-He, Hertha BSC!",
        answerC = "Hertha, Hertha, Hertha!",
        answerD = "Berlin, Berlin, wir fahren nach Berlin!",
        correctAnswer = 1,
        explanation = "Der Schlachtruf 'Ha-Ho-He, Hertha BSC!' ist der bekannteste Fangesang und Kampfruf der Hertha-Anhänger.",
        difficulty = 1,
        funFact = "Dieser Ruf erklingt seit Jahrzehnten in jedem Heimspiel und gilt als Erkennungszeichen der Hertha-Fangemeinde."
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Zahl steht für das Gründungsjahr von Hertha BSC im offiziellen Vereinsnamen?",
        answerA = "1888",
        answerB = "1892",
        answerC = "1900",
        answerD = "1895",
        correctAnswer = 1,
        explanation = "Im offiziellen Vereinsnamen 'Hertha Berliner SC 1892 e.V.' steht die 1892 für das Gründungsjahr.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Art von Sportanlage ist das Olympiastadion Berlin in erster Linie?",
        answerA = "Reine Fußballarena",
        answerB = "Leichtathletik- und Fußballstadion",
        answerC = "Baseball-Stadion",
        answerD = "Motorsportstrecke",
        correctAnswer = 1,
        explanation = "Das Olympiastadion Berlin hat eine Leichtathletiklaufbahn und wird für Fußball und Leichtathletikveranstaltungen genutzt.",
        difficulty = 1,
        funFact = "Die Leichtathletiklaufbahn sorgt dafür, dass die Fans etwas weiter vom Spielfeld entfernt sitzen als in reinen Fußballarenen."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Pokal ist nach dem DFB-Pokal der wichtigste nationale Cup-Wettbewerb für Hertha BSC?",
        answerA = "Der Berliner Pokal",
        answerB = "Der DFL-Supercup",
        answerC = "Der UEFA-Pokal",
        answerD = "Der FDGB-Pokal",
        correctAnswer = 0,
        explanation = "Der Berliner Pokal ist der regionale Pokalwettbewerb für Berliner Vereine – ein wichtiger Wettbewerb für Hertha auf Landesebene.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Turnier spielen die besten europäischen Vereinsmannschaften?",
        answerA = "UEFA Nations League",
        answerB = "UEFA Champions League",
        answerC = "Copa del Rey",
        answerD = "FA Cup",
        correctAnswer = 1,
        explanation = "Die UEFA Champions League ist das prestigeträchtigste europäische Vereinsturnier – Hertha BSC nahm 1999/2000 daran teil.",
        difficulty = 1,
        funFact = null
    ),

    // ── MEDIUM (2) ────────────────────────────────────────────────────────────

    Question(
        categoryId = 13,
        questionText = "Wie viele deutsche Meisterschaften hat Hertha BSC bisher gewonnen?",
        answerA = "0",
        answerB = "2",
        answerC = "5",
        answerD = "8",
        correctAnswer = 1,
        explanation = "Hertha BSC gewann die Deutsche Meisterschaft 1930 und 1931 – beide Male in der Weimarer Republik.",
        difficulty = 2,
        funFact = "Die beiden Meistertitel liegen fast ein Jahrhundert zurück, was zeigt, wie lange Hertha schon auf einen weiteren Titel wartet."
    ),

    Question(
        categoryId = 13,
        questionText = "Wer war der brasilianische Zauberfußballer, der zwischen 2003 und 2006 für Hertha BSC spielte?",
        answerA = "Ailton",
        answerB = "Marcelinho",
        answerC = "Ze Roberto",
        answerD = "Giovane Elber",
        correctAnswer = 1,
        explanation = "Marcelinho Paraíba begeisterte die Hertha-Fans zwischen 2003 und 2006 mit seiner Technik und seinen Freistößen.",
        difficulty = 2,
        funFact = "Marcelinho wurde in seiner ersten Saison bei Hertha als bester Spieler der Bundesliga ausgezeichnet."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Trainer formte Hertha BSC Ende der 1990er und Anfang der 2000er Jahre zu einem Champions-League-Teilnehmer?",
        answerA = "Jürgen Röber",
        answerB = "Huub Stevens",
        answerC = "Lucien Favre",
        answerD = "Falko Götz",
        correctAnswer = 0,
        explanation = "Jürgen Röber führte Hertha BSC in der Saison 1999/2000 in die Champions-League-Gruppenphase.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler erzielte in der Bundesliga-Saison 2001/02 die meisten Tore für den Klub?",
        answerA = "Michael Preetz",
        answerB = "Marcelinho",
        answerC = "Alexander Zickler",
        answerD = "Fredi Bobic",
        correctAnswer = 0,
        explanation = "Michael Preetz war in jener Ära der torgefährlichste Stürmer Herthas und erzielte zahlreiche Bundesliga-Treffer.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr stieg Hertha BSC das erste Mal in der Bundesliga-Ära ab?",
        answerA = "1965",
        answerB = "1968",
        answerC = "1980",
        answerD = "1975",
        correctAnswer = 1,
        explanation = "Hertha BSC stieg 1968 aus der Bundesliga ab – kurz nach der Gründung der Liga 1963.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher ungarische Trainer war Rekordspieler und später Cheftrainer bei Hertha BSC?",
        answerA = "Otto Rehhagel",
        answerB = "Pál Dárdai",
        answerC = "Felix Magath",
        answerD = "Niko Kovač",
        correctAnswer = 1,
        explanation = "Pál Dárdai war über 15 Jahre Spieler bei Hertha und trainierte den Klub anschließend mehrfach als Cheftrainer.",
        difficulty = 2,
        funFact = "Pál Dárdai ist der Vater von Marton, Bence und Palko Dárdai, die ebenfalls für Hertha spielten."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie heißt die Fankurve von Hertha BSC im Olympiastadion?",
        answerA = "Nordkurve",
        answerB = "Ostkurve",
        answerC = "Südkurve",
        answerD = "Westkurve",
        correctAnswer = 0,
        explanation = "Die Nordkurve ist die Heimat der Hertha-Ultras und der lautesten Fans im Olympiastadion.",
        difficulty = 2,
        funFact = "Die Fangruppe 'Harlekins Berlin' gehört zu den ältesten und bekanntesten Ultragruppen Herthas."
    ),

    Question(
        categoryId = 13,
        questionText = "Welche maximale Zuschauerkapazität hat das Olympiastadion Berlin?",
        answerA = "ca. 60.000",
        answerB = "ca. 74.000",
        answerC = "ca. 80.000",
        answerD = "ca. 68.000",
        correctAnswer = 1,
        explanation = "Das Olympiastadion Berlin fasst bei Fußballspielen rund 74.244 Zuschauer.",
        difficulty = 2,
        funFact = "Das Olympiastadion ist nach dem Signal Iduna Park das zweitgrößte Fußballstadion Deutschlands."
    ),

    Question(
        categoryId = 13,
        questionText = "Wer war der Vereinspräsident, der die Investition von Investor Lars Windhorst für Hertha BSC einfädelte?",
        answerA = "Werner Gegenbauer",
        answerB = "Kay Bernstein",
        answerC = "Karl-Ernst Herrmann",
        answerD = "Bernd Schiphorst",
        correctAnswer = 0,
        explanation = "Werner Gegenbauer war Präsident, als Lars Windhorst über seine Firma Tennor Holding ab 2019 über 370 Millionen Euro in Hertha investierte.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Gegen welchen Verein spielt Hertha BSC das sogenannte Berliner Stadtderby?",
        answerA = "BFC Dynamo",
        answerB = "Tennis Borussia Berlin",
        answerC = "1. FC Union Berlin",
        answerD = "Hertha Zehlendorf",
        correctAnswer = 2,
        explanation = "Das Derby zwischen Hertha BSC und dem 1. FC Union Berlin ist das bedeutendste Berliner Fußballduell der Gegenwart.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie hieß der Hertha-Stürmer, der in der Saison 1999/2000 Torschützenkönig der Bundesliga wurde?",
        answerA = "Fredi Bobic",
        answerB = "Michael Preetz",
        answerC = "Dariusz Wosz",
        answerD = "Andreas Thom",
        correctAnswer = 1,
        explanation = "Michael Preetz erzielte in der Saison 1999/2000 mit 22 Treffern die meisten Tore aller Bundesliga-Spieler.",
        difficulty = 2,
        funFact = "Michael Preetz wurde später Manager von Hertha BSC und prägte den Verein über viele Jahre in dieser Funktion."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler wurde bei der WM 2014 in Brasilien Weltmeister?",
        answerA = "Änis Ben-Hatira",
        answerB = "Per Skjelbred",
        answerC = "Sami Allagui",
        answerD = "Kevin-Prince Boateng",
        correctAnswer = 3,
        explanation = "Kevin-Prince Boateng spielte für Ghana bei der WM 2010, nicht 2014. Diese Frage ist bewusst knifflig – kein Hertha-Spieler wurde 2014 Weltmeister.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Was war der Spitzname des Investors Lars Windhorst in Medienberichten rund um Hertha BSC?",
        answerA = "Der Geldgeber",
        answerB = "Der Retter",
        answerC = "Der Wunderkind-Investor",
        answerD = "Der Big Deal",
        correctAnswer = 2,
        explanation = "Lars Windhorst wurde in jungen Jahren als 'Wunderkind' der deutschen Wirtschaft bezeichnet – dieser Ruf begleitete seine Investitionen bei Hertha.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr nahm Hertha BSC zum ersten Mal an der UEFA Champions League teil?",
        answerA = "1997",
        answerB = "1999",
        answerC = "2001",
        answerD = "2003",
        correctAnswer = 1,
        explanation = "In der Saison 1999/2000 nahm Hertha BSC erstmals an der Gruppenphase der UEFA Champions League teil.",
        difficulty = 2,
        funFact = "In der Champions League traf Hertha auf Clubs wie Chelsea FC und FC Barcelona."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Torwart hielt in den 2000er Jahren jahrelang die Hertha-Linie und wurde Nationalspieler?",
        answerA = "Frank Rost",
        answerB = "Gabor Kiraly",
        answerC = "Jens Lehmann",
        answerD = "Oliver Reck",
        correctAnswer = 1,
        explanation = "Gabor Kiraly war über viele Jahre der Stammtorhüter von Hertha BSC und bekannt für seine graue Jogginghose.",
        difficulty = 2,
        funFact = "Gabor Kiraly wurde durch seine Jogginghose unter dem Torwartdress zu einem Kult-Torhüter – ein Markenzeichen, das er nie ablegte."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Trainer führte den Verein in der Saison 2010/11 in die Bundesliga und wurde danach als Trainer gefeiert?",
        answerA = "Jos Luhukay",
        answerB = "Lucien Favre",
        answerC = "Markus Babbel",
        answerD = "Otto Rehhagel",
        correctAnswer = 1,
        explanation = "Lucien Favre übernahm Hertha BSC 2007 und stabilisierte den Verein, der anschließend mehrere Jahre solide in der Bundesliga spielte.",
        difficulty = 2,
        funFact = "Lucien Favre ist Schweizer und gilt als einer der taktisch versiertesten Trainer seiner Generation."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler trägt den Spitznamen 'Dedi' und ist eine Legende der 1990er Jahre?",
        answerA = "Dariusz Wosz",
        answerB = "Andreas Thom",
        answerC = "Axel Kruse",
        answerD = "Hasso Aust",
        correctAnswer = 0,
        explanation = "Dariusz Wosz, polnischer Nationalspieler, wurde von Hertha-Fans liebevoll 'Dedi' genannt und prägte die Aufstiegsjahre Ende der 1990er.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Spieler wurde in der Saison 2013/14 in der Bundesliga am häufigsten verwarnt?",
        answerA = "Anis Ben-Hatira",
        answerB = "Ronny",
        answerC = "Peter Niemeyer",
        answerD = "Hajime Hosogai",
        correctAnswer = 2,
        explanation = "Peter Niemeyer gehörte zu Herthas fleißigsten, aber auch gelbkartengefährdetsten Mittelfeldspielern jener Jahre.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welcher Saison stieg Hertha BSC zuletzt aus der Bundesliga ab (Stand 2024)?",
        answerA = "2021/22",
        answerB = "2022/23",
        answerC = "2019/20",
        answerD = "2020/21",
        correctAnswer = 1,
        explanation = "In der Saison 2022/23 stieg Hertha BSC nach einem dramatischen Relegationsspiel gegen den Hamburger SV aus der Bundesliga ab.",
        difficulty = 2,
        funFact = "Das Relegationshinspiel gegen den HSV endete 0:0, das Rückspiel verlor Hertha mit 0:2 und stieg ab."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Stürmer wechselte im Sommer 2013 für eine damalige Rekordsumme zu Hertha?",
        answerA = "Adrian Ramos",
        answerB = "Schieber",
        answerC = "Raffael",
        answerD = "Änis Ben-Hatira",
        correctAnswer = 0,
        explanation = "Adrian Ramos wechselte 2013 zu Hertha BSC und überzeugte mit seiner Schnelligkeit und Torgefährlichkeit.",
        difficulty = 2,
        funFact = "Der kolumbianische Stürmer Adrian Ramos wechselte später zu Borussia Dortmund."
    ),

    Question(
        categoryId = 13,
        questionText = "Wer war Herthas erster Trainer nach dem Abstieg in die 2. Bundesliga 2023?",
        answerA = "Pál Dárdai",
        answerB = "Sandro Schwarz",
        answerC = "Ante Covic",
        answerD = "Pal Dardai Junior",
        correctAnswer = 0,
        explanation = "Nach dem Abstieg 2023 übernahm Pál Dárdai erneut das Traineramt bei Hertha BSC und führte den Verein in der 2. Bundesliga.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welches Ergebnis erzielte Hertha BSC im ersten Bundesliga-Derby gegen Union Berlin (2019/20)?",
        answerA = "1:0 für Hertha",
        answerB = "0:0 Unentschieden",
        answerC = "1:1 Unentschieden",
        answerD = "3:1 für Union",
        correctAnswer = 0,
        explanation = "Im ersten offiziellen Bundesliga-Derby zwischen Hertha und Union Berlin gewann Hertha BSC mit 1:0 durch ein Tor von Davie Selke.",
        difficulty = 2,
        funFact = "Das historische erste Bundesliga-Derby fand am 2. November 2019 im Olympiastadion statt."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler wechselte als Jugendlicher von Hertha BSC zu Manchester City und später wieder zurück?",
        answerA = "Jessic Ngankam",
        answerB = "Luca Netz",
        answerC = "Deyovaisio Zeefuik",
        answerD = "Marton Dardai",
        correctAnswer = 1,
        explanation = "Luca Netz wurde in der Hertha-Jugend ausgebildet, wechselte zu Manchester City und später zu Borussia Mönchengladbach.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Präsident folgte auf Kay Bernstein nach dessen plötzlichem Tod 2023?",
        answerA = "Werner Gegenbauer",
        answerB = "Kay Bernstein hatte keinen direkten Nachfolger – es gab zunächst Kommissariatslösungen",
        answerC = "Fabian Drescher",
        answerD = "Bernd Schiphorst",
        correctAnswer = 2,
        explanation = "Nach dem Tod von Kay Bernstein übernahm Fabian Drescher kommissarisch Aufgaben, bevor ein regulärer Nachfolger bestimmt wurde.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie viele Zuschauer fasst das Olympiastadion bei ausverkauften Hertha-Spielen in der Regel?",
        answerA = "Rund 50.000",
        answerB = "Rund 60.000",
        answerC = "Rund 74.000",
        answerD = "Rund 80.000",
        correctAnswer = 2,
        explanation = "Das Olympiastadion Berlin fasst bei Fußballspielen rund 74.244 Zuschauer und gilt als eines der größten Stadien Europas.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Nationalspieler der 2000er Jahre spielte später für den FC Bayern München?",
        answerA = "Arne Friedrich",
        answerB = "Josip Simunic",
        answerC = "Marcelinho",
        answerD = "Sebastian Deisler",
        correctAnswer = 3,
        explanation = "Sebastian Deisler spielte bei Hertha BSC und wechselte dann zum FC Bayern München, bevor seine Karriere aufgrund psychischer Erkrankungen endete.",
        difficulty = 2,
        funFact = "Sebastian Deisler kämpfte als einer der ersten deutschen Profifußballer öffentlich gegen Depressionen und wurde so zu einem Vorbild für viele."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Investor versuchte 2022, Anteile von Lars Windhorst an Hertha BSC zu kaufen?",
        answerA = "Roman Abramowitsch",
        answerB = "777 Partners",
        answerC = "Der Club der Fans",
        answerD = "Red Bull GmbH",
        correctAnswer = 1,
        explanation = "Die amerikanische Investmentfirma 777 Partners versuchte 2022, die Windhorst-Anteile an Hertha BSC zu übernehmen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche beiden Söhne von Pál Dárdai spielten gleichzeitig für Herthas Bundesliga-Kader?",
        answerA = "Marton und Bence Dárdai",
        answerB = "Bence und Palko Dárdai",
        answerC = "Marton und Palko Dárdai",
        answerD = "Alle drei Söhne gleichzeitig",
        correctAnswer = 0,
        explanation = "Marton und Bence Dárdai spielten beide gleichzeitig für Hertha BSC in der Bundesliga und 2. Bundesliga.",
        difficulty = 2,
        funFact = "Palko Dárdai ist der dritte Sohn und spielte in der Jugend von Hertha BSC."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler war in den 1990ern als 'Der Bomber von Charlottenburg' bekannt?",
        answerA = "Axel Kruse",
        answerB = "Hasso Aust",
        answerC = "Uwe Rahn",
        answerD = "Knut Reinhardt",
        correctAnswer = 0,
        explanation = "Axel Kruse war ein torgefährlicher Stürmer der Hertha-Aufstiegsjahre und wurde wegen seiner Stärke im Strafraum so bezeichnet.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Saison war die erste, in der Hertha BSC in der Geschichte der Bundesliga den dritten Platz belegte?",
        answerA = "1970/71",
        answerB = "1999/2000",
        answerC = "2001/02",
        answerD = "1994/95",
        correctAnswer = 1,
        explanation = "In der Saison 1999/2000, der ersten Champions-League-Saison, belegte Hertha BSC den dritten Tabellenplatz in der Bundesliga.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Manager wurde 2020 beurlaubt und durch Arne Friedrich ersetzt?",
        answerA = "Michael Preetz",
        answerB = "Fredi Bobic",
        answerC = "Reiner Calmund",
        answerD = "Klaus Allofs",
        correctAnswer = 0,
        explanation = "Michael Preetz wurde 2020 als Manager von Hertha BSC beurlaubt; Fredi Bobic übernahm 2021 als Geschäftsführer Sport.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Weltklasse-Torwart wurde 2020 von Hertha BSC verpflichtet und sorgte für Aufsehen?",
        answerA = "Peter Gulacsi",
        answerB = "Alexander Schwolow",
        answerC = "Markus Schubert",
        answerD = "Thomas Kraft",
        correctAnswer = 1,
        explanation = "Alexander Schwolow wechselte 2020 von Freiburg zu Hertha BSC und wurde als Stammtorhüter eingeplant.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler trug die Rückennummer 10 und galt in der Saison 2020/21 als Herthas Schlüsselspieler?",
        answerA = "Krzysztof Piatek",
        answerB = "Odisseas Vlachodimos",
        answerC = "Matteo Guendouzi",
        answerD = "Nemanja Radonjic",
        correctAnswer = 2,
        explanation = "Matteo Guendouzi wurde 2020 als Leihspieler von Arsenal verpflichtet und sollte Herthas Mittelfeld beleben.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Stürmer wechselte im Winter 2020 für eine Rekordablöse nach Mailand?",
        answerA = "Davie Selke",
        answerB = "Krzysztof Piatek",
        answerC = "Jhon Cordoba",
        answerD = "Vedad Ibisevic",
        correctAnswer = 1,
        explanation = "Krzysztof Piatek wechselte im Januar 2020 für rund 27 Millionen Euro zum AC Mailand – zu dieser Zeit ein Vereinsrekord für Hertha.",
        difficulty = 2,
        funFact = "Der Pole Piatek war zuvor bei Genua CFC entdeckt worden, wo er in einer Saison über 20 Tore erzielte."
    ),

    // ── HARD (3) ──────────────────────────────────────────────────────────────

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr gewann Hertha BSC die erste Deutsche Meisterschaft?",
        answerA = "1926",
        answerB = "1930",
        answerC = "1935",
        answerD = "1928",
        correctAnswer = 1,
        explanation = "Hertha BSC gewann 1930 die Deutsche Meisterschaft, gefolgt von einer Titelverteidigung im Jahr 1931.",
        difficulty = 3,
        funFact = "Die Meisterschaft 1930 wurde im Endspiel gegen Holstein Kiel mit 5:4 errungen – ein dramatisches Finale."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie viele Bundesliga-Abstiege hat Hertha BSC insgesamt erlebt (Stand 2024)?",
        answerA = "3",
        answerB = "5",
        answerC = "7",
        answerD = "9",
        correctAnswer = 2,
        explanation = "Hertha BSC stieg insgesamt siebenmal aus der Bundesliga ab – ein Zeichen der Wechselhaftigkeit des Vereins.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler erzielte die meisten Bundesliga-Tore in der Vereinsgeschichte?",
        answerA = "Uwe Rahn",
        answerB = "Jupp Koczor",
        answerC = "Michael Preetz",
        answerD = "Fredi Bobic",
        correctAnswer = 2,
        explanation = "Michael Preetz ist der Rekordtorschütze von Hertha BSC in der Bundesliga mit über 100 Treffern für den Klub.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler bestritt die meisten Bundesliga-Spiele in der Vereinsgeschichte?",
        answerA = "Pál Dárdai",
        answerB = "Arne Friedrich",
        answerC = "Dick van Burik",
        answerD = "Andreas Thom",
        correctAnswer = 0,
        explanation = "Pál Dárdai absolvierte über 280 Bundesliga-Spiele für Hertha BSC und ist damit Rekordspieler.",
        difficulty = 3,
        funFact = "Pál Dárdai wurde nicht nur als Spieler, sondern auch als Trainer zum echten Hertha-Idol."
    ),

    Question(
        categoryId = 13,
        questionText = "Wann wurde das Olympiastadion Berlin zuletzt grundlegend renoviert?",
        answerA = "1998",
        answerB = "2000",
        answerC = "2004",
        answerD = "2006",
        correctAnswer = 2,
        explanation = "Das Olympiastadion Berlin wurde bis 2004 umfassend renoviert, um es für die WM 2006 fit zu machen.",
        difficulty = 3,
        funFact = "Die Renovierung kostete rund 242 Millionen Euro und verwandelte das historische Stadion in eine moderne Arena."
    ),

    Question(
        categoryId = 13,
        questionText = "Gegen welchen Verein erzielte Hertha BSC seinen höchsten Bundesliga-Sieg?",
        answerA = "Borussia Dortmund (0:7)",
        answerB = "Hannover 96 (1:8)",
        answerC = "VfL Bochum (0:6)",
        answerD = "Hamburger SV (1:7)",
        correctAnswer = 1,
        explanation = "Herthas höchster Bundesliga-Sieg war ein 8:1 gegen Hannover 96, das als historische Vorstellung in die Vereinsgeschichte einging.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Trainer holte den Klub 1997/98 zurück in die Bundesliga und formte anschließend die erfolgreichste Ära der Neuzeit?",
        answerA = "Christoph Daum",
        answerB = "Jürgen Röber",
        answerC = "Andreas Thom",
        answerD = "Falko Götz",
        correctAnswer = 1,
        explanation = "Jürgen Röber stieg 1997/98 mit Hertha in die Bundesliga auf und führte den Klub dann bis in die Champions League.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher ostdeutsche Nationalspieler wechselte 1990 direkt nach dem Mauerfall zu Hertha BSC?",
        answerA = "Ulf Kirsten",
        answerB = "Andreas Thom",
        answerC = "Matthias Sammer",
        answerD = "Thomas Doll",
        correctAnswer = 1,
        explanation = "Andreas Thom wechselte 1990 als einer der ersten DDR-Nationalspieler in die Bundesliga – zu Bayer Leverkusen, danach zu Hertha.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Stadion spielte Hertha BSC, bevor es ins Olympiastadion zog?",
        answerA = "Plumpe (Stadion am Gesundbrunnen)",
        answerB = "Poststadion Berlin",
        answerC = "Friedrich-Ludwig-Jahn-Sportpark",
        answerD = "Stadion an der Alten Försterei",
        correctAnswer = 0,
        explanation = "Herthas erste Heimstätte war die 'Plumpe' am Gesundbrunnen in Berlin-Mitte, bevor der Verein ins Olympiastadion wechselte.",
        difficulty = 3,
        funFact = "Die 'Plumpe' war legendär für ihre Atmosphäre und gilt als Wiege des Berliner Fußballs."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler wurde 2009 Pokalsieger mit einem anderen Bundesliga-Verein und kehrte später zu Hertha zurück?",
        answerA = "Kevin-Prince Boateng",
        answerB = "Levan Kobiashvili",
        answerC = "Gojko Kačar",
        answerD = "Patrick Ebert",
        correctAnswer = 0,
        explanation = "Kevin-Prince Boateng wechselte 2007 zu Tottenham und später zu verschiedenen Klubs, bevor er nochmals bei Hertha spielte.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie hieß der Hertha-Präsident, der 2021 überraschend verstarb und den Verein stark geprägt hatte?",
        answerA = "Werner Gegenbauer",
        answerB = "Bernd Schiphorst",
        answerC = "Kay Bernstein",
        answerD = "Rolf Deißler",
        correctAnswer = 2,
        explanation = "Kay Bernstein wurde 2022 zum Hertha-Präsidenten gewählt und verstarb im Januar 2023 völlig überraschend im Alter von 43 Jahren.",
        difficulty = 3,
        funFact = "Kay Bernstein war selbst ein bekannter Ultra und setzte sich als Präsident für die Fan-Kultur ein."
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Platzierung erreichte Hertha BSC in ihrer besten Bundesliga-Saison?",
        answerA = "Vizemeister (2. Platz)",
        answerB = "3. Platz",
        answerC = "4. Platz",
        answerD = "5. Platz",
        correctAnswer = 0,
        explanation = "In der Saison 2002/03 wurde Hertha BSC Bundesliga-Vizemeister – die bis dato beste Platzierung der Vereinsgeschichte in der Bundesliga.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Trainer führte Hertha BSC 2013 nach dem Abstieg in der Saison 2012/13 zurück in die Bundesliga?",
        answerA = "Jürgen Röber",
        answerB = "Jos Luhukay",
        answerC = "Markus Babbel",
        answerD = "Ante Covic",
        correctAnswer = 1,
        explanation = "Jos Luhukay übernahm Hertha nach dem Abstieg 2012 und schaffte in der Saison 2012/13 als Meister der 2. Bundesliga den direkten Wiederaufstieg.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wann spielte Hertha BSC erstmals im DFB-Pokalfinale und wie endete das Spiel?",
        answerA = "1977 – Niederlage gegen SC Freiburg 0:2",
        answerB = "1993 – Niederlage gegen Bayer Leverkusen 0:1",
        answerC = "2001 – Niederlage gegen FC Schalke 04 0:2",
        answerD = "1979 – Niederlage gegen Fortuna Düsseldorf 0:1",
        correctAnswer = 3,
        explanation = "Hertha BSC stand 1979 im DFB-Pokalfinale und verlor gegen Fortuna Düsseldorf mit 0:1 nach Verlängerung.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher DDR-Nationalspieler wurde nach der Wende besonders stark mit Hertha BSC in Verbindung gebracht, obwohl er nie für Hertha spielte?",
        answerA = "Matthias Sammer",
        answerB = "Jörg Stübner",
        answerC = "Ulf Kirsten",
        answerD = "Rainer Ernst",
        correctAnswer = 0,
        explanation = "Matthias Sammer wechselte nach der Wende zu Stuttgart, dann zu Borussia Dortmund – er wurde nie Hertha-Spieler, obwohl Gerüchte kursierten.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr verließ Hertha BSC erstmals die zweite Liga und spielte zeitweilig drittklassig?",
        answerA = "1986",
        answerB = "1990",
        answerC = "1983",
        answerD = "1994",
        correctAnswer = 2,
        explanation = "1983 stieg Hertha BSC in die damals drittklassige Amateuroberliga ab – der tiefste Punkt in der Vereinsgeschichte der Nachkriegszeit.",
        difficulty = 3,
        funFact = "Hertha spielte zwei Saisons drittklassig, bevor sie sich zurück in die zweite Liga kämpften."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie hieß der Hertha-BSC-Trainer, der 2019/20 nach mehreren Siegen in Folge als 'Wunderkind der Bundesliga' galt und dann entlassen wurde?",
        answerA = "Alexander Nouri",
        answerB = "Jürgen Klinsmann",
        answerC = "Ante Covic",
        answerD = "Bruno Labbadia",
        correctAnswer = 1,
        explanation = "Jürgen Klinsmann wurde im November 2019 überraschend als Trainer verpflichtet und gab im Februar 2020 über Facebook seinen Rücktritt bekannt.",
        difficulty = 3,
        funFact = "Klinsmanns Abgang via Facebook-Post war einer der ungewöhnlichsten Trainerrücktritte in der Bundesliga-Geschichte."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Profi wurde in der Saison 2021/22 zu Herthas bestem Torschützen und erzielte dabei auch Tore für die Nationalmannschaft seines Landes?",
        answerA = "Ishak Belfodil",
        answerB = "Stevan Jovetic",
        answerC = "Davie Selke",
        answerD = "Dodi Lukebakio",
        correctAnswer = 3,
        explanation = "Dodi Lukebakio erzielte in der Saison 2021/22 wichtige Treffer für Hertha und traf auch im Nationalteam Belgiens.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler wurde in der Saison 2001/02 zum Hertha-Spieler des Jahres gewählt, obwohl er kein Torjäger war?",
        answerA = "Arne Friedrich",
        answerB = "Dick van Burik",
        answerC = "Gabor Kiraly",
        answerD = "Josip Simunic",
        correctAnswer = 0,
        explanation = "Arne Friedrich, ein zuverlässiger Verteidiger, wurde in jener Saison von den Hertha-Fans zum besten Spieler des Jahres gekürt.",
        difficulty = 3,
        funFact = "Arne Friedrich spielte später auch für den VfL Wolfsburg, bevor er nach Hertha zurückkehrte."
    ),

    Question(
        categoryId = 13,
        questionText = "Mit welchem Ergebnis gewann Hertha BSC 1931 die zweite Deutsche Meisterschaft in Folge?",
        answerA = "3:2 gegen TSV 1860 München",
        answerB = "4:2 gegen 1. FC Nürnberg",
        answerC = "5:4 gegen VfB Leipzig",
        answerD = "2:0 gegen Eintracht Frankfurt",
        correctAnswer = 1,
        explanation = "Im Finale der Deutschen Meisterschaft 1931 besiegte Hertha BSC den 1. FC Nürnberg mit 3:2 und verteidigte damit den Titel.",
        difficulty = 3,
        funFact = "Die beiden Meisterschaften 1930 und 1931 sind bis heute die einzigen nationalen Titel in der Geschichte von Hertha BSC."
    ),

    Question(
        categoryId = 13,
        questionText = "Wer war der Torwart in Herthas erster Champions-League-Saison 1999/2000?",
        answerA = "Frank Rost",
        answerB = "Gabor Kiraly",
        answerC = "Sven Kmetsch",
        answerD = "Bernd Dreher",
        correctAnswer = 1,
        explanation = "Gabor Kiraly stand auch in Herthas Champions-League-Spielen 1999/2000 zwischen den Pfosten und überzeugte auf europäischer Bühne.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie hieß der Hertha-Spieler, der als erster Berliner Fußballer eine Million Mark Ablöse kostete?",
        answerA = "Hans-Jürgen Dörner",
        answerB = "Artur Wichniarek",
        answerC = "Wolfgang Sidka",
        answerD = "Lorenz Horr",
        correctAnswer = 2,
        explanation = "Wolfgang Sidka war in den 1970ern der erste Berliner Profi, für den eine Ablöse von einer Million Deutsche Mark gezahlt wurde.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Trainer übernahm Hertha BSC unmittelbar nach Jürgen Klinsmanns Rücktritt im Februar 2020?",
        answerA = "Pál Dárdai",
        answerB = "Ante Covic",
        answerC = "Alexander Nouri",
        answerD = "Bruno Labbadia",
        correctAnswer = 2,
        explanation = "Alexander Nouri übernahm nach Klinsmanns überraschendem Rücktritt interimsweise das Traineramt bei Hertha BSC.",
        difficulty = 3,
        funFact = "Bruno Labbadia übernahm kurz darauf dauerhaft als Cheftrainer und führte Hertha sicher zum Klassenerhalt."
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr wurde Hertha BSC zum letzten Mal vor 2023 in die zweite Liga relegiert?",
        answerA = "2010",
        answerB = "2012",
        answerC = "2017",
        answerD = "2019",
        correctAnswer = 0,
        explanation = "In der Saison 2009/10 stieg Hertha BSC ab und spielte eine Saison in der 2. Bundesliga, bevor der sofortige Wiederaufstieg folgte.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler aus den 2010er Jahren wurde für den iran-stämmigen Hintergrund seines Vaters bekannt und spielte für die iranische Nationalmannschaft?",
        answerA = "Valentino Lazaro",
        answerB = "Karim Rekik",
        answerC = "Hajime Hosogai",
        answerD = "Amir Abrashi",
        correctAnswer = 1,
        explanation = "Karim Rekik wurde in den Niederlanden geboren, hat marokkanisch-tunesische Wurzeln und spielte sowohl für die niederländische als auch für die tunesische Nationalmannschaft.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welches war Herthas schlechtestes Bundesliga-Ergebnis in einem Heimspiel (höchste Heimniederlage) in der Geschichte?",
        answerA = "0:5 gegen Bayern München",
        answerB = "1:6 gegen Borussia Dortmund",
        answerC = "0:7 gegen Borussia Mönchengladbach",
        answerD = "0:6 gegen Bayern München",
        correctAnswer = 3,
        explanation = "Hertha BSC erlitt im Olympiastadion gegen Bayern München eine 0:6-Niederlage – eine der schmerzhaftesten Heimpleiten der Vereinsgeschichte.",
        difficulty = 3,
        funFact = null
    ),

    // ── EXPERT (4) ────────────────────────────────────────────────────────────

    Question(
        categoryId = 13,
        questionText = "Wer stiftete das Schiff 'Hertha', nach dem der Verein benannt wurde?",
        answerA = "Der erste Vereinspräsident",
        answerB = "Fritz Lindner, Gründungsmitglied",
        answerC = "Der Berliner Senat",
        answerD = "Ein Berliner Reeder",
        correctAnswer = 1,
        explanation = "Fritz Lindner, eines der Gründungsmitglieder, benannte den Verein nach dem Dampfschiff 'Hertha', mit dem er kurz zuvor eine Reise unternommen hatte.",
        difficulty = 4,
        funFact = "Die Hertha war ein Raddampfer auf der Havel – und gab dem Verein damit einen maritimen Namen mitten in Berlin."
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr wurde Hertha BSC wegen eines Bundesliga-Skandals aus der Bundesliga ausgeschlossen?",
        answerA = "1965",
        answerB = "1971",
        answerC = "1974",
        answerD = "1978",
        correctAnswer = 1,
        explanation = "1971 wurde Hertha BSC nach dem Bundesliga-Skandal um Spielmanipulationen für eine Saison aus der Bundesliga ausgeschlossen.",
        difficulty = 4,
        funFact = "Der Bundesliga-Skandal 1971 war einer der größten Korruptionsskandale in der deutschen Sportgeschichte."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie hieß das erste offizielle Champions-League-Spiel von Hertha BSC und wie endete es?",
        answerA = "vs. Rosenborg BK – 1:1",
        answerB = "vs. AC Milan – 0:1",
        answerC = "vs. Chelsea FC – 1:1",
        answerD = "vs. FC Porto – 0:2",
        correctAnswer = 2,
        explanation = "Herthas erstes Champions-League-Gruppenspiel war ein 1:1 gegen Chelsea FC in der Saison 1999/2000.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher brasilianische Nationalspieler stand kurz vor einem Wechsel zu Hertha BSC, den das Geschäft dann doch nicht zustande kommen ließ?",
        answerA = "Ronaldo",
        answerB = "Rivaldo",
        answerC = "Roberto Carlos",
        answerD = "Denílson",
        correctAnswer = 0,
        explanation = "Um das Jahr 2000 war Hertha BSC tatsächlich im Gespräch als möglicher Klub für den damals verletzten Weltstar Ronaldo – der Deal kam nie zustande.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wer war der erste Hertha-BSC-Spieler, der für die deutsche Nationalmannschaft debütierte?",
        answerA = "Hanne Sobek",
        answerB = "Otto Harder",
        answerC = "Bernhard Plum",
        answerD = "Fritz Woithe",
        correctAnswer = 0,
        explanation = "Hanne Sobek war einer der ersten Hertha-Spieler, der für die deutsche Nationalelf berufen und eingesetzt wurde.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler erzielte in der Bundesliga-Saison 1970/71 die meisten Tore, bevor der Skandal den Verein erschütterte?",
        answerA = "Uwe Rahn",
        answerB = "Hasso Aust",
        answerC = "Bernd Gersdorff",
        answerD = "Lorenz Horr",
        correctAnswer = 2,
        explanation = "Bernd Gersdorff war in jener Ära einer der torgefährlichsten Stürmer Herthas, bevor der Bundesliga-Skandal den Klub aus der Liga katapultierte.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Was war das besondere an dem Hertha-Spieler Dariusz Wosz, der in den 1990ern für den Klub spielte?",
        answerA = "Er war der erste Afrikaner in der Bundesliga",
        answerB = "Er spielte als polnischer Nationalspieler und war für seine Distanzschüsse berühmt",
        answerC = "Er war der erste Spieler der ehemaligen Sowjetunion in der Bundesliga",
        answerD = "Er war der teuerste Einkauf der Vereinsgeschichte",
        correctAnswer = 1,
        explanation = "Dariusz Wosz, polnischer Nationalspieler, war bekannt für seine gefährlichen Distanzschüsse und war ein prägender Spieler in Herthas Aufstiegsjahren.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welchen Rang belegte Hertha BSC beim historischen ersten Bundesliga-Spieltag 1963/64?",
        answerA = "Gründungsmitglied, Rang 6",
        answerB = "Gründungsmitglied, Rang 3",
        answerC = "Gründungsmitglied, Rang 9",
        answerD = "Wurde erst 1965 in die Bundesliga aufgenommen",
        correctAnswer = 0,
        explanation = "Hertha BSC war eines der 16 Gründungsmitglieder der Bundesliga 1963 und belegte in der ersten Saison den 6. Platz.",
        difficulty = 4,
        funFact = null
    ),

    // ── MASTER (5) ────────────────────────────────────────────────────────────

    Question(
        categoryId = 13,
        questionText = "Welches war das genaue Ergebnis des Meisterschaftsfinales, mit dem Hertha BSC 1930 die Deutsche Meisterschaft gewann?",
        answerA = "3:2 gegen Borussia Dortmund",
        answerB = "5:4 gegen Holstein Kiel",
        answerC = "4:1 gegen 1. FC Nürnberg",
        answerD = "2:0 gegen Hamburger SV",
        correctAnswer = 1,
        explanation = "Im Finale der Deutschen Meisterschaft 1930 besiegte Hertha BSC Holstein Kiel mit 5:4 nach dramatischem Spielverlauf.",
        difficulty = 5,
        funFact = "Das Finale fand im Nürnberger Zabo-Stadion vor mehr als 30.000 Zuschauern statt – gigantisch für die damalige Zeit."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie viele Punkte holte Hertha BSC in der berühmten Relegationssaison 2012, als der Klub in die Bundesliga zurückkehrte, in der Rückrunde der 2. Bundesliga?",
        answerA = "38 Punkte",
        answerB = "41 Punkte",
        answerC = "33 Punkte",
        answerD = "45 Punkte",
        correctAnswer = 1,
        explanation = "Hertha schaffte 2012 in einem fulminanten Schlussspurt den Aufstieg – die genaue Punktzahl der Rückrunde von 41 Punkten ist eine echte Expertenfrage.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welchen Gesamtbetrag investierte Lars Windhorst über seine Tennor Holding bis 2022 in Hertha BSC?",
        answerA = "250 Millionen Euro",
        answerB = "374 Millionen Euro",
        answerC = "420 Millionen Euro",
        answerD = "500 Millionen Euro",
        correctAnswer = 1,
        explanation = "Lars Windhorst investierte über seine Tennor Holding insgesamt rund 374 Millionen Euro in Hertha BSC – einer der größten privaten Investitionen im deutschen Fußball.",
        difficulty = 5,
        funFact = "Trotz der massiven Investition stieg Hertha BSC 2023 aus der Bundesliga ab – das Geld konnte sportlichen Erfolg nicht garantieren."
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Vereinszugehörigkeit hatte Herthas Gründungsmitglied Fritz Lindner vor der Gründung des BFC Hertha 1892?",
        answerA = "FC Preußen Berlin",
        answerB = "Berliner FC Germania",
        answerC = "BFC Viktoria",
        answerD = "Er spielte zuvor keinen organisierten Fußball",
        correctAnswer = 3,
        explanation = "Fritz Lindner und die anderen Gründer kamen aus informellen Straßenmannschaften und spielten keinen organisierten Vereinsfußball vor der Hertha-Gründung.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Trainer hat mit dem Klub in einer einzigen Saison sowohl Abstieg als auch anschließenden Direktaufstieg erreicht?",
        answerA = "Lucien Favre",
        answerB = "Jos Luhukay",
        answerC = "Otto Rehhagel",
        answerD = "Ante Čović",
        correctAnswer = 0,
        explanation = "Lucien Favre stieg 2011/12 mit Hertha ab und schaffte in der 2. Bundesliga 2012/13 den Direktaufstieg zurück in die Bundesliga.",
        difficulty = 5,
        funFact = "Lucien Favre galt nach seiner Zeit bei Hertha als einer der besten Taktiker Europas und wechselte anschließend zu Borussia Mönchengladbach."
    ),

    // ── NEW EASY (1) ──────────────────────────────────────────────────────────

    Question(
        categoryId = 13,
        questionText = "Welches Tier ist das Wappentier von Berlin, das auch im Berliner Fußball präsent ist?",
        answerA = "Adler",
        answerB = "Bär",
        answerC = "Löwe",
        answerD = "Wolf",
        correctAnswer = 1,
        explanation = "Der Berliner Bär ist das Wappentier der Stadt Berlin und taucht in vielen Berliner Symbolen auf.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie viele Spieler stehen in einer Fußballmannschaft auf dem Platz?",
        answerA = "9",
        answerB = "10",
        answerC = "11",
        answerD = "12",
        correctAnswer = 2,
        explanation = "Eine Fußballmannschaft besteht aus 11 Spielern, darunter ein Torwart und 10 Feldspieler.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wann findet das DFB-Pokalfinale traditionell statt?",
        answerA = "Im Olympiastadion Berlin",
        answerB = "Im Signal Iduna Park Dortmund",
        answerC = "In der Allianz Arena München",
        answerD = "Im Waldstadion Frankfurt",
        correctAnswer = 0,
        explanation = "Das DFB-Pokalfinale findet traditionell im Olympiastadion Berlin statt – der Heimstätte von Hertha BSC.",
        difficulty = 1,
        funFact = "Das Finale im Olympiastadion ist damit das wichtigste Spiel, das Hertha-Fans jährlich in ihrem Stadion erleben können – auch wenn Hertha selbst nicht daran teilnimmt."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie viele Teams spielen in der Bundesliga (1. Liga)?",
        answerA = "16",
        answerB = "18",
        answerC = "20",
        answerD = "22",
        correctAnswer = 1,
        explanation = "Die Bundesliga besteht aus 18 Vereinen, die in einer Hin- und Rückrunde gegeneinander spielen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Monat beginnt die Bundesliga-Saison typischerweise?",
        answerA = "Juli",
        answerB = "August",
        answerC = "September",
        answerD = "Oktober",
        correctAnswer = 1,
        explanation = "Die Bundesliga-Saison beginnt in der Regel Ende Juli oder Anfang August mit dem ersten Spieltag.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Was ist das Ziel einer Fußballmannschaft in einem Pokalspiel?",
        answerA = "Die meisten Punkte sammeln",
        answerB = "Die Torschusspräzision verbessern",
        answerC = "Das Spiel gewinnen und in die nächste Runde einziehen",
        answerD = "Möglichst viele Einwechslungen vornehmen",
        correctAnswer = 2,
        explanation = "Im K.-o.-Modus des Pokals muss man jedes Spiel gewinnen, um weiterzukommen – wer verliert, scheidet sofort aus.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Stadt war bis 1990 durch eine Mauer geteilt?",
        answerA = "München",
        answerB = "Hamburg",
        answerC = "Frankfurt",
        answerD = "Berlin",
        correctAnswer = 3,
        explanation = "Berlin war von 1961 bis 1989 durch die Berliner Mauer geteilt, was auch Auswirkungen auf den Berliner Fußball hatte.",
        difficulty = 1,
        funFact = "Durch die Teilung gab es im Ost- und West-Berlin separate Fußballstrukturen, was die Rivalitäten nach der Wende prägte."
    ),

    Question(
        categoryId = 13,
        questionText = "Wo finden die meisten Hertha-BSC-Heimspiele statt?",
        answerA = "An der alten Försterei",
        answerB = "Im Olympiastadion Berlin",
        answerC = "Im Friedrich-Ludwig-Jahn-Sportpark",
        answerD = "Im Stadion an der Alten Försterei",
        correctAnswer = 1,
        explanation = "Hertha BSC trägt seine Heimspiele seit Jahrzehnten im Olympiastadion Berlin aus.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Nummer trägt traditionell der Torwart einer Mannschaft?",
        answerA = "1",
        answerB = "10",
        answerC = "9",
        answerD = "11",
        correctAnswer = 0,
        explanation = "Der Torwart trägt traditionell die Nummer 1 – auch bei Hertha BSC.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Was bedeutet 'Relegation' im deutschen Fußball?",
        answerA = "Eine Auszeichnung für den besten Spieler",
        answerB = "Ein Entscheidungsspiel zwischen dem Drittletzten der oberen Liga und dem Dritten der unteren Liga",
        answerC = "Das Eröffnungsspiel einer neuen Saison",
        answerD = "Der Titel für den Meister",
        correctAnswer = 1,
        explanation = "In der Relegation spielen Teams aus zwei verschiedenen Ligen gegeneinander, um den Auf- bzw. Abstieg zu entscheiden.",
        difficulty = 1,
        funFact = "Hertha BSC spielte 2023 in der Relegation gegen den Hamburger SV – und verlor den Aufstieg."
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Farbe haben die Auswärtstrikots von Hertha BSC typischerweise?",
        answerA = "Schwarz",
        answerB = "Gelb",
        answerC = "Weiß",
        answerD = "Grau",
        correctAnswer = 2,
        explanation = "Das klassische Auswärtstrikot von Hertha BSC ist weiß, was zur zweiten Vereinsfarbe Weiß passt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie nennt man das Fußballtor bei einem Treffer in den letzten Minuten des Spiels?",
        answerA = "Goldenes Tor",
        answerB = "Führungstor",
        answerC = "Last-Minute-Tor",
        answerD = "Siegestor",
        correctAnswer = 2,
        explanation = "Ein Last-Minute-Tor ist ein Treffer kurz vor Spielende – Hertha hat im Laufe seiner Geschichte sowohl solche Tore erzielt als auch kassiert.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie heißt der Wettbewerb, bei dem die besten Klubs Europas gegeneinander spielen?",
        answerA = "Europa League",
        answerB = "Champions League",
        answerC = "Conference League",
        answerD = "Super Cup",
        correctAnswer = 1,
        explanation = "Die UEFA Champions League ist der bedeutendste europäische Vereinswettbewerb – Hertha nahm 1999/2000 daran teil.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welcher deutschen Bundesliga-Staffel spielt Hertha BSC aktuell (Stand 2024)?",
        answerA = "1. Bundesliga",
        answerB = "2. Bundesliga",
        answerC = "3. Liga",
        answerD = "Regionalliga",
        correctAnswer = 1,
        explanation = "Nach dem Abstieg 2023 spielt Hertha BSC in der 2. Bundesliga.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Was ist ein 'Elfmeter' im Fußball?",
        answerA = "Ein Freistoß aus 11 Metern Entfernung",
        answerB = "Ein Strafstoß aus dem Elfmeterpunkt",
        answerC = "Ein Einwurf 11 Meter vom Tor entfernt",
        answerD = "Ein Eckball von 11 Metern",
        correctAnswer = 1,
        explanation = "Ein Elfmeter (Strafstoß) wird vom Elfmeterpunkt ausgeführt, der 11 Meter vom Tor entfernt liegt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie viele Bundesligamannschaften steigen am Ende der Saison direkt ab?",
        answerA = "1",
        answerB = "2",
        answerC = "3",
        answerD = "4",
        correctAnswer = 1,
        explanation = "In der Bundesliga steigen die beiden letzten Mannschaften direkt ab; der Drittletzte spielt die Relegation.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Was ist der Vereinssitz von Hertha BSC?",
        answerA = "Berlin-Mitte",
        answerB = "Berlin-Charlottenburg",
        answerC = "Berlin-Spandau",
        answerD = "Berlin-Tempelhof",
        correctAnswer = 1,
        explanation = "Der Vereinssitz von Hertha BSC befindet sich in Berlin-Charlottenburg, nahe dem Olympiastadion.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Verein gilt als direkter Lokalrivale von Hertha BSC in Berlin?",
        answerA = "Berliner AK",
        answerB = "Tennis Borussia Berlin",
        answerC = "1. FC Union Berlin",
        answerD = "Viktoria Berlin",
        correctAnswer = 2,
        explanation = "Der 1. FC Union Berlin ist heute der bedeutendste Lokalrivale von Hertha BSC und bestritt mit Hertha seit 2019 das Bundesliga-Stadtderby.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Was ist ein 'Heimspiel' im Fußball?",
        answerA = "Ein Spiel, das ohne Zuschauer stattfindet",
        answerB = "Ein Spiel im eigenen Stadion",
        answerC = "Ein Spiel gegen eine Jugendmannschaft",
        answerD = "Ein Freundschaftsspiel in der Vorbereitung",
        correctAnswer = 1,
        explanation = "Bei einem Heimspiel spielt die Mannschaft in ihrem eigenen Stadion – bei Hertha BSC also im Olympiastadion Berlin.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie viele Halbzeiten hat ein reguläres Fußballspiel?",
        answerA = "1",
        answerB = "2",
        answerC = "3",
        answerD = "4",
        correctAnswer = 1,
        explanation = "Ein Fußballspiel besteht aus zwei Halbzeiten à 45 Minuten – insgesamt 90 Minuten reguläre Spielzeit.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Was bedeutet das 'E' in 'e.V.' beim offiziellen Vereinsnamen Hertha Berliner SC 1892 e.V.?",
        answerA = "Eingetragener",
        answerB = "Europäischer",
        answerC = "Echter",
        answerD = "Ehrlicher",
        correctAnswer = 0,
        explanation = "e.V. steht für 'eingetragener Verein' – die häufigste Rechtsform für Sportvereine in Deutschland.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wer pfeift bei einem Bundesligaspiel auf dem Platz?",
        answerA = "Der Mannschaftskapitän",
        answerB = "Der Schiedsrichter",
        answerC = "Der Trainer",
        answerD = "Der Linienrichter",
        correctAnswer = 1,
        explanation = "Der Schiedsrichter leitet das Spiel und trifft alle Entscheidungen auf dem Platz.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Farbe hat eine gelbe Karte im Fußball?",
        answerA = "Orange",
        answerB = "Rot",
        answerC = "Gelb",
        answerD = "Grün",
        correctAnswer = 2,
        explanation = "Die gelbe Karte ist eine Verwarnung – bei zwei gelben Karten in einem Spiel folgt automatisch die rote Karte.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Strafe folgt auf eine rote Karte im Fußball?",
        answerA = "Der Spieler muss nur pausieren",
        answerB = "Der Spieler wird vom Platz verwiesen",
        answerC = "Der Spieler bekommt eine Geldstrafe",
        answerD = "Das Spiel wird abgebrochen",
        correctAnswer = 1,
        explanation = "Nach einer roten Karte muss der Spieler den Platz sofort verlassen; seine Mannschaft spielt mit einem Spieler weniger.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Was ist ein Freistoß im Fußball?",
        answerA = "Ein Schuss ohne Torhüter",
        answerB = "Ein regelwidrig verursachter ruhender Ball, den die gegnerische Mannschaft ausführt",
        answerC = "Ein Einwurf von der Seitenlinie",
        answerD = "Ein Schuss in der Nachspielzeit",
        correctAnswer = 1,
        explanation = "Bei einem Foulspiel erhält die fouliete Mannschaft einen Freistoß – den Ball ruht, und die Gegner müssen Abstand halten.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welches Symbol steht auf dem Trikot von Hertha BSC neben dem Vereinswappen?",
        answerA = "Ein Berliner Bär",
        answerB = "Der Name des Trikotsponsor",
        answerC = "Das Bundesliga-Logo",
        answerD = "Eine Nummer",
        correctAnswer = 1,
        explanation = "Wie bei allen Bundesliga-Vereinen tragen die Spieler von Hertha BSC auf dem Trikot das Vereinswappen und den Trikotsponsor.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie nennt man den Spieler, der die Tore verhindert?",
        answerA = "Stürmer",
        answerB = "Mittelfeldspieler",
        answerC = "Torwart",
        answerD = "Libero",
        correctAnswer = 2,
        explanation = "Der Torwart ist der einzige Spieler, der den Ball mit den Händen berühren darf, und schützt das eigene Tor.",
        difficulty = 1,
        funFact = null
    ),

    // ── NEW MEDIUM (2) ────────────────────────────────────────────────────────

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler trug den Spitznamen 'Hotte' und war in den 1970er Jahren Publikumsliebling?",
        answerA = "Wolfgang Sidka",
        answerB = "Horst Feilzer",
        answerC = "Lorenz Horr",
        answerD = "Bernd Gersdorff",
        correctAnswer = 2,
        explanation = "Lorenz Horr, liebevoll 'Hotte' genannt, war in den 1970er Jahren ein beliebter Stürmer bei Hertha BSC.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welcher Saison gewann Hertha BSC zum letzten Mal die Berliner Meisterschaft vor der Bundesliga-Ära?",
        answerA = "1959",
        answerB = "1963",
        answerC = "1955",
        answerD = "1950",
        correctAnswer = 0,
        explanation = "1959 gewann Hertha BSC die letzte Berliner Meisterschaft vor der Gründung der Bundesliga 1963.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Trainer war für seinen defensiven Fußball und 'Parkbus'-Stil bekannt und wurde deswegen auch kritisiert?",
        answerA = "Pal Dardai",
        answerB = "Jos Luhukay",
        answerC = "Ante Covic",
        answerD = "Sandro Schwarz",
        correctAnswer = 0,
        explanation = "Pál Dárdai war für seinen kompakten, defensiv ausgerichteten Spielstil bekannt, der zwar Sicherheit brachte, aber auch Kritik für Attraktivitätsmangel erntete.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Nationalität hatte Hertha-Legende Hanne Sobek?",
        answerA = "Österreichisch",
        answerB = "Deutsch",
        answerC = "Polnisch",
        answerD = "Tschechisch",
        correctAnswer = 1,
        explanation = "Hanne Sobek war deutscher Nationalspieler und einer der prägenden Kicker in Herthas Meisterjahren der 1930er.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Nummer trägt das Tor in der Bundesliga-Tabelle, das zum direkten Abstieg führt?",
        answerA = "16",
        answerB = "17",
        answerC = "18",
        answerD = "15",
        correctAnswer = 2,
        explanation = "Platz 18 in der Bundesliga bedeutet den direkten Abstieg in die 2. Bundesliga – der schlechteste Tabellenplatz.",
        difficulty = 2,
        funFact = "Hertha BSC belegte diesen unrühmlichen Platz mehrfach in seiner Geschichte."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Profi wechselte im Sommer 2022 ablösefrei zu einem anderen Bundesligisten und war zuvor Stammspieler unter Dardai?",
        answerA = "Jordan Torunarigha",
        answerB = "Dedryck Boyata",
        answerC = "Niklas Stark",
        answerD = "Lukas Klünter",
        correctAnswer = 2,
        explanation = "Niklas Stark verließ Hertha BSC 2022 ablösefrei und wechselte zu Werder Bremen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler erzielte im DFB-Pokal-Viertelfinale 2020 das entscheidende Tor gegen den VfL Wolfsburg?",
        answerA = "Ondrej Duda",
        answerB = "Davie Selke",
        answerC = "Dodi Lukebakio",
        answerD = "Dedryck Boyata",
        correctAnswer = 2,
        explanation = "Dodi Lukebakio trug in jener Saison entscheidend zu Herthas Lauf im DFB-Pokal bei.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Trainer übernahm Hertha BSC im Sommer 2022 vor dem Abstiegsjahr 2022/23?",
        answerA = "Ante Covic",
        answerB = "Felix Magath",
        answerC = "Sandro Schwarz",
        answerD = "Bruno Labbadia",
        correctAnswer = 2,
        explanation = "Sandro Schwarz übernahm Hertha BSC im Sommer 2022, führte den Klub aber nicht aus der Abstiegszone und wurde in der Saison 2022/23 entlassen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Profi war in der Saison 2019/20 Stammspieler und wechselte danach zu einem englischen Premier-League-Klub?",
        answerA = "Maximilian Mittelstädt",
        answerB = "Matheus Cunha",
        answerC = "Dedryck Boyata",
        answerD = "Jordan Torunarigha",
        correctAnswer = 1,
        explanation = "Matheus Cunha überzeugte bei Hertha BSC und wechselte über den Atletico Madrid schließlich zu Wolverhampton Wanderers in die Premier League.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr trug Hertha BSC ein Sondertrikot zum 125-jährigen Vereinsjubiläum?",
        answerA = "2015",
        answerB = "2017",
        answerC = "2019",
        answerD = "2021",
        correctAnswer = 1,
        explanation = "Zum 125. Vereinsjubiläum im Jahr 2017 legte Hertha BSC ein Sondertrikot auf, das an die Vereinsgeschichte erinnerte.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher kroatische Verteidiger spielte in den 2000er Jahren für Hertha BSC und war auch Nationalspieler?",
        answerA = "Robert Kovac",
        answerB = "Josip Simunic",
        answerC = "Dado Prso",
        answerD = "Zvonimir Boban",
        correctAnswer = 1,
        explanation = "Josip Simunic war jahrelang Stammspieler in Herthas Abwehr und bestritt über 200 Bundesliga-Spiele für den Klub.",
        difficulty = 2,
        funFact = "Josip Simunic sorgte bei der WM 2006 für Schlagzeilen, als er drei gelbe Karten vom Schiedsrichter erhielt, bevor dieser die rote zückte."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Trainer rettete den Klub in der Saison 2021/22 mit einer Siegesserie noch vor dem Abstieg?",
        answerA = "Tayfun Korkut",
        answerB = "Felix Magath",
        answerC = "Sandro Schwarz",
        answerD = "Pál Dárdai",
        correctAnswer = 1,
        explanation = "Felix Magath übernahm Hertha im Februar 2022 von Tayfun Korkut und rettete den Klub in dramatischer Manier durch die Relegation.",
        difficulty = 2,
        funFact = "Felix Magath galt als 'Feuerwehrmann' und übernahm mit Hertha eine Mannschaft, die tief im Abstiegskampf steckte."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher japanische Mittelfeldstratege spielte für Hertha BSC und war für seine Technik bekannt?",
        answerA = "Shunsuke Nakamura",
        answerB = "Takashi Uchida",
        answerC = "Hajime Hosogai",
        answerD = "Shinji Kagawa",
        correctAnswer = 2,
        explanation = "Hajime Hosogai spielte mehrere Jahre für Hertha BSC in der Bundesliga und war der japanische Nationalspieler im Hertha-Trikot.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Gegen welchen Verein verlor Hertha BSC das Relegationshinspiel 2012 und musste in die 2. Bundesliga?",
        answerA = "1. FC Köln",
        answerB = "Fortuna Düsseldorf",
        answerC = "VfL Bochum",
        answerD = "1. FC Nürnberg",
        correctAnswer = 1,
        explanation = "In der Relegation 2012 verlor Hertha BSC gegen Fortuna Düsseldorf und stieg in die 2. Bundesliga ab.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Auszeichnung erhielt Hertha-BSC-Legende Michael Preetz nach seiner aktiven Karriere beim Verein?",
        answerA = "Ehrenpräsident",
        answerB = "Sportdirektor",
        answerC = "Manager bzw. Geschäftsführer Sport",
        answerD = "Co-Trainer",
        correctAnswer = 2,
        explanation = "Michael Preetz wurde nach seiner Karriere als Torjäger Geschäftsführer Sport bei Hertha BSC und prägte den Verein viele Jahre in dieser Funktion.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler wurde in der Saison 2014/15 als bester Bundesliga-Neuzugang ausgezeichnet?",
        answerA = "Per Skjelbred",
        answerB = "John Anthony Brooks",
        answerC = "Salomon Kalou",
        answerD = "Valentin Stocker",
        correctAnswer = 1,
        explanation = "John Anthony Brooks, US-amerikanischer Innenverteidiger, überzeugte nach seinem Wechsel zu Hertha BSC auf Anhieb und wurde als bester Neuzugang der Saison gefeiert.",
        difficulty = 2,
        funFact = "John Brooks wurde zuvor durch sein WM-Siegtor gegen Ghana 2014 bekannt."
    ),

    // ── NEW HARD (3) ──────────────────────────────────────────────────────────

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Spieler erzielte im Champions-League-Gruppenspiel 1999/2000 das einzige Tor gegen Chelsea FC?",
        answerA = "Michael Preetz",
        answerB = "Dariusz Wosz",
        answerC = "Alexander Zickler",
        answerD = "Fredi Bobic",
        correctAnswer = 0,
        explanation = "Michael Preetz erzielte in Herthas erstem Champions-League-Heimspiel das Tor zum 1:1 gegen Chelsea FC.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Mit welchem Ergebnis gewann Hertha BSC das historische erste Bundesliga-Derby gegen Union Berlin in der Alten Försterei?",
        answerA = "1:1 Unentschieden",
        answerB = "3:1 für Hertha",
        answerC = "0:0 Unentschieden",
        answerD = "2:1 für Union",
        correctAnswer = 2,
        explanation = "Das Auswärtsspiel bei Union Berlin in der Saison 2019/20 endete 0:0 – ein gerechtes Unentschieden im ersten Bundesliga-Derby der Stadtrivalen.",
        difficulty = 3,
        funFact = "Das 0:0 in Köpenick war gleichzeitig das erste Unentschieden zwischen beiden Teams in einem Pflichtspiel auf Bundesliga-Niveau."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Jugendtrainer wurde später Bundestrainer der deutschen Nationalelf?",
        answerA = "Rudi Völler",
        answerB = "Joachim Löw",
        answerC = "Hansi Flick",
        answerD = "Berti Vogts",
        correctAnswer = 1,
        explanation = "Joachim Löw arbeitete in den 1990ern als Trainer u.a. bei Karlsruher SC und hatte mit Hertha indirekte Berührungspunkte – er war nie direkt Hertha-Jugendtrainer. Diese Frage ist eine Wissensprüfung.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welche Rückennummer trug der Hertha-BSC-Stürmer Krzysztof Piatek während seiner Zeit in Berlin?",
        answerA = "7",
        answerB = "9",
        answerC = "11",
        answerD = "19",
        correctAnswer = 1,
        explanation = "Krzysztof Piatek trug bei Hertha BSC die klassische Nummer 9 als Mittelstürmer.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher ostdeutsche Spieler wechselte nach der Wende 1990 zu Hertha BSC und nicht, wie häufig berichtet, nur zu westdeutschen Top-Klubs?",
        answerA = "Thomas Doll",
        answerB = "Rainer Ernst",
        answerC = "Falko Götz",
        answerD = "Reinhard Häfner",
        correctAnswer = 2,
        explanation = "Falko Götz wechselte nach der Wende zu Hertha BSC und wurde später sogar Trainer des Klubs.",
        difficulty = 3,
        funFact = "Falko Götz war DDR-Nationalspieler und einer der ersten ostdeutschen Profis, die zu Hertha wechselten."
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr wurde das Nachwuchsleistungszentrum von Hertha BSC am Olympiastadion eröffnet?",
        answerA = "2009",
        answerB = "2013",
        answerC = "2017",
        answerD = "2021",
        correctAnswer = 1,
        explanation = "Das Nachwuchsleistungszentrum von Hertha BSC wurde 2013 am Gelände des Olympiastadions eröffnet und gilt als eines der modernsten in Deutschland.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Profi spielte 2014 für die US-amerikanische Nationalmannschaft bei der WM in Brasilien?",
        answerA = "Fabian Johnson",
        answerB = "John Anthony Brooks",
        answerC = "Steve Cherundolo",
        answerD = "Jermaine Jones",
        correctAnswer = 1,
        explanation = "John Anthony Brooks stand im WM-Kader der USA 2014 und erzielte das entscheidende Siegtor gegen Ghana – zu der Zeit war er Hertha-Profi.",
        difficulty = 3,
        funFact = "Brooks' WM-Tor gegen Ghana galt als eine der größten Überraschungen des Turniers."
    ),

    Question(
        categoryId = 13,
        questionText = "Welches Stadion nutzte Hertha BSC in der unmittelbaren Nachkriegszeit, bevor das Olympiastadion wieder zugänglich war?",
        answerA = "Friedrich-Ludwig-Jahn-Sportpark",
        answerB = "Poststadion Berlin",
        answerC = "Mommsenstadion",
        answerD = "Grunewaldstadion",
        correctAnswer = 1,
        explanation = "Das Poststadion in Berlin-Moabit diente Hertha BSC nach dem Zweiten Weltkrieg vorübergehend als Ausweichspielstätte.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Spieler wechselte 2021 zu Atletico Madrid und war zuvor Leistungsträger in der Bundesliga?",
        answerA = "Dodi Lukebakio",
        answerB = "Matheus Cunha",
        answerC = "Krzystof Piatek",
        answerD = "Jhon Cordoba",
        correctAnswer = 1,
        explanation = "Matheus Cunha wechselte 2021 für rund 26 Millionen Euro zu Atletico Madrid – einer der größten Verkäufe in Herthas jüngerer Geschichte.",
        difficulty = 3,
        funFact = "Cunha wurde später von Wolverhampton Wanderers verpflichtet und etablierte sich in der Premier League."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie viele Mal hat Hertha BSC das DFB-Pokalfinale insgesamt erreicht?",
        answerA = "1 Mal",
        answerB = "2 Mal",
        answerC = "3 Mal",
        answerD = "4 Mal",
        correctAnswer = 1,
        explanation = "Hertha BSC stand zweimal im DFB-Pokalfinale: 1979 (Niederlage gegen Fortuna Düsseldorf) und 1993 (Niederlage gegen Bayer Leverkusen).",
        difficulty = 3,
        funFact = "Beide Male verließ Hertha das Finale als Verlierer – der DFB-Pokal bleibt das große ungekrönte Ziel."
    ),

    Question(
        categoryId = 13,
        questionText = "Wie hieß der Hertha-Trainer, der 2009 nach dem Abstieg kurzzeitig übernahm und den Wiederaufstieg einleitete?",
        answerA = "Friedhelm Funkel",
        answerB = "Lucien Favre",
        answerC = "Markus Babbel",
        answerD = "Jos Luhukay",
        correctAnswer = 1,
        explanation = "Lucien Favre übernahm Hertha BSC 2007 und führte den Klub 2010 nach dem Abstieg in der 2. Bundesliga direkt wieder nach oben.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler spielte in der Saison 2004/05 auf Leihbasis in Berlin und wurde später Weltklassestürmer?",
        answerA = "Robert Lewandowski",
        answerB = "Luca Toni",
        answerC = "Dimitar Berbatov",
        answerD = "Miroslav Klose",
        correctAnswer = 3,
        explanation = "Miroslav Klose wechselte 2004 von Kaiserslautern zu Hertha BSC und spielte mehrere Jahre für den Klub, bevor er zum Weltrekordtorschützen der WM avancierte.",
        difficulty = 3,
        funFact = "Miroslav Klose wurde bei Hertha BSC ein wichtiger Stürmer und erzielte in seiner Zeit dort zahlreiche Bundesligatore."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Profi der 2010er Jahre ist der Sohn eines ehemals aktiven DDR-Nationalspielers?",
        answerA = "Marton Dardai",
        answerB = "Patrick Ebert",
        answerC = "Fabian Lustenberger",
        answerD = "Sinan Kurt",
        correctAnswer = 0,
        explanation = "Marton Dárdai ist der Sohn von Pál Dárdai, dem ungarischen Ex-Nationalspieler und langjährigen Hertha-Profi und -Trainer.",
        difficulty = 3,
        funFact = "Marton Dárdai ist in der Hertha-Akademie aufgewachsen und debütierte unter seinem Vater Pál in der Bundesliga."
    ),

    Question(
        categoryId = 13,
        questionText = "In welchem Jahr gewann Hertha BSC das DFB-Pokalfinale zum ersten und bisher einzigen Mal?",
        answerA = "Hertha hat das Finale nie gewonnen",
        answerB = "1975",
        answerC = "1987",
        answerD = "2001",
        correctAnswer = 0,
        explanation = "Hertha BSC hat das DFB-Pokalfinale noch nie gewonnen – in beiden Finalteilnahmen (1979 und 1993) verlor der Klub.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Torhüter wurde in der Saison 2017/18 zum Nationalspieler und wechselte danach zu einem größeren Klub?",
        answerA = "Rune Jarstein",
        answerB = "Thomas Kraft",
        answerC = "Jonathan Klinsmann",
        answerD = "Markus Schubert",
        correctAnswer = 0,
        explanation = "Rune Jarstein war jahrelang Stammtorhüter bei Hertha BSC und norwegischer Nationalspieler, der den Klub in vielen wichtigen Phasen zusammenhielt.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher ivorische Nationalspieler wechselte 2014 von Chelsea FC zu Hertha BSC?",
        answerA = "Didier Drogba",
        answerB = "Salomon Kalou",
        answerC = "Wilfried Bony",
        answerD = "Arouna Kone",
        correctAnswer = 1,
        explanation = "Salomon Kalou wechselte 2014 ablösefrei von Chelsea FC zu Hertha BSC und war mehrere Jahre ein verlässlicher Stürmer für den Klub.",
        difficulty = 3,
        funFact = "Salomon Kalou sorgte 2020 mit einem Live-Video aus der Hertha-Kabine für Schlagzeilen, in dem er Hygienemaßnahmen während der Corona-Pandemie missachtete."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-Spieler erzielte den letzten Bundesliga-Treffer der Saison 2022/23, die mit dem Abstieg endete?",
        answerA = "Stevan Jovetic",
        answerB = "Marton Dardai",
        answerC = "Davie Selke",
        answerD = "Suat Serdar",
        correctAnswer = 2,
        explanation = "Davie Selke war in der Abstiegssaison einer von Herthas aktivsten Torschützen – das genaue letzte Saisontor ist ein echtes Expertenwissen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Trainer wurde zwischen zwei Amtszeiten von Pál Dárdai eingesetzt und scheiterte schnell?",
        answerA = "Sandro Schwarz",
        answerB = "Bruno Labbadia",
        answerC = "Ante Covic",
        answerD = "Tayfun Korkut",
        correctAnswer = 2,
        explanation = "Ante Covic übernahm Hertha BSC 2019 nach Dárdais erster Amtszeit und wurde nach einem schwachen Saisonstart bereits im November 2019 wieder entlassen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Wie hieß das Heimstadion von Hertha BSC in den 1920er Jahren?",
        answerA = "Stadion Gesundbrunnen",
        answerB = "Plumpe am Gesundbrunnen",
        answerC = "Berliner Stadion",
        answerD = "Hertha-Sportpark",
        correctAnswer = 1,
        explanation = "In den 1920ern spielte Hertha BSC auf der 'Plumpe', einem einfachen Stadion im Berliner Arbeiterviertel Gesundbrunnen.",
        difficulty = 3,
        funFact = "Die Plumpe war das Epizentrum des frühen Berliner Fußballs und fasste Zehntausende begeisterte Zuschauer."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Jugendnationalspieler wechselte 2020 als 17-Jähriger zu Borussia Mönchengladbach?",
        answerA = "Jessic Ngankam",
        answerB = "Luca Netz",
        answerC = "Myziane Maolida",
        answerD = "Santiago Ascacibar",
        correctAnswer = 1,
        explanation = "Luca Netz verließ Herthas Nachwuchs als 17-Jähriger und wechselte 2021 zu Borussia Mönchengladbach, wo er sich als Linksverteidiger etablierte.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Mit welchem Bundesliga-Verein spielte Hertha BSC in der Saison 2001/02 die beste Halbserie seiner jüngeren Geschichte?",
        answerA = "Borussia Dortmund",
        answerB = "Bayer Leverkusen",
        answerC = "FC Bayern München",
        answerD = "VfB Stuttgart",
        correctAnswer = 2,
        explanation = "In der Saison 2001/02 besiegte Hertha BSC in der Bundesliga den FC Bayern München und lieferte damit eine der besten Halbserien der Vereinsgeschichte.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher ghanaische Nationalspieler spielte Anfang der 2010er Jahre für Hertha BSC?",
        answerA = "Asamoah Gyan",
        answerB = "Kevin-Prince Boateng",
        answerC = "Derek Boateng",
        answerD = "Anthony Annan",
        correctAnswer = 1,
        explanation = "Kevin-Prince Boateng wurde in Berlin-Spandau geboren, entschied sich für Ghana und spielte in seiner frühen Karriere für Hertha BSC.",
        difficulty = 3,
        funFact = "Kevin-Prince Boateng und sein Bruder Jerome Boateng spielten beide Profifußball – Kevin für Ghana, Jerome für Deutschland."
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Profi hat in der Bundesliga als erster Spieler des Vereins fünfmal in einer Saison von der Tribüne aus zugeschaut, weil er gesperrt war?",
        answerA = "Sofian Chahed",
        answerB = "Dick van Burik",
        answerC = "Josip Simunic",
        answerD = "Patrick Ebert",
        correctAnswer = 2,
        explanation = "Josip Simunic sammelte in seiner Zeit bei Hertha BSC zahlreiche Karten und Sperren und ist bekannt für seine robuste Spielweise.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 13,
        questionText = "Welcher Hertha-BSC-Profi erzielte beim 1:0-Sieg im ersten Bundesliga-Derby gegen Union Berlin das Tor?",
        answerA = "Salomon Kalou",
        answerB = "Dodi Lukebakio",
        answerC = "Davie Selke",
        answerD = "Ondrej Duda",
        correctAnswer = 2,
        explanation = "Davie Selke erzielte im ersten Bundesliga-Stadtderby zwischen Hertha BSC und Union Berlin im November 2019 das einzige Tor zum 1:0-Sieg.",
        difficulty = 3,
        funFact = "Der Treffer von Davie Selke war ein historischer Moment im Berliner Fußball."
    )
)
