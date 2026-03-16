package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestions(): List<Question> = listOf(

    // --- EASY (difficulty = 1) ---

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler hat eine Fußballmannschaft auf dem Feld?",
        answerA = "9",
        answerB = "10",
        answerC = "11",
        answerD = "12",
        correctAnswer = 2,
        explanation = "Eine Fußballmannschaft besteht aus 11 Spielern auf dem Feld, inklusive Torwart.",
        difficulty = 1,
        funFact = "Das erste Regelwerk mit 11 Spielern pro Team wurde 1863 von der Football Association festgelegt."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Land fanden die Olympischen Sommerspiele 2020 statt?",
        answerA = "China",
        answerB = "Brasilien",
        answerC = "Japan",
        answerD = "Australien",
        correctAnswer = 2,
        explanation = "Die Olympischen Sommerspiele 2020 fanden 2021 in Tokio, Japan statt – verschoben wegen der Corona-Pandemie.",
        difficulty = 1,
        funFact = "Es waren die ersten Olympischen Spiele in der Geschichte, die um ein Jahr verschoben wurden."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Sets gewinnt ein Tennisspieler in einem Grand-Slam-Herrenfinale?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "5",
        correctAnswer = 1,
        explanation = "Bei Grand-Slam-Turnieren spielen Herren im Best-of-5-Modus. Wer zuerst 3 Sätze gewinnt, gewinnt das Match.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das Ziel beim Golfen?",
        answerA = "Den Ball so oft wie möglich zu schlagen",
        answerB = "Den Ball mit möglichst wenigen Schlägen ins Loch zu bringen",
        answerC = "Den Ball so weit wie möglich zu werfen",
        answerD = "Den Ball über eine Linie zu schießen",
        correctAnswer = 1,
        explanation = "Im Golf gewinnt der Spieler, der die 18 Löcher mit den wenigsten Schlägen absolviert.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Farbe hat die Rückseite einer Tischtennis-Schlägerseite laut offiziellen Regeln?",
        answerA = "Blau und Gelb",
        answerB = "Rot und Schwarz",
        answerC = "Grün und Weiß",
        answerD = "Orange und Grau",
        correctAnswer = 1,
        explanation = "Laut offiziellen ITTF-Regeln muss ein Seite des Tischtennisschlägers rot und die andere schwarz sein.",
        difficulty = 1,
        funFact = "Diese Regel wurde 1986 eingeführt, damit Gegner erkennen können, welcher Belag benutzt wird."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart wird in Wimbledon ausgetragen?",
        answerA = "Badminton",
        answerB = "Squash",
        answerC = "Tennis",
        answerD = "Golf",
        correctAnswer = 2,
        explanation = "Wimbledon ist das bekannteste Tennisturnier der Welt und wird jährlich in London ausgetragen.",
        difficulty = 1,
        funFact = "Wimbledon ist das älteste Tennisturnier der Welt – die erste Ausgabe fand 1877 statt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lange dauert ein reguläres Basketballspiel in der NBA?",
        answerA = "32 Minuten",
        answerB = "40 Minuten",
        answerC = "48 Minuten",
        answerD = "60 Minuten",
        correctAnswer = 2,
        explanation = "Ein NBA-Spiel dauert 48 Minuten, aufgeteilt in vier Viertel à 12 Minuten.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation hat die meisten Olympia-Goldmedaillen in der Geschichte gewonnen?",
        answerA = "Russland",
        answerB = "Deutschland",
        answerC = "China",
        answerD = "USA",
        correctAnswer = 3,
        explanation = "Die USA führen die ewige Bestenliste der Olympischen Sommerspiele mit der höchsten Anzahl an Goldmedaillen an.",
        difficulty = 1,
        funFact = "Die USA haben bisher über 1000 Goldmedaillen bei Olympischen Sommerspielen gewonnen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet 'Eigentor' im Fußball?",
        answerA = "Ein Tor aus großer Distanz",
        answerB = "Ein Tor, das ein Spieler in sein eigenes Netz schießt",
        answerC = "Ein Tor nach einem Freistoß",
        answerD = "Ein Tor nach einem Elfmeter",
        correctAnswer = 1,
        explanation = "Ein Eigentor entsteht, wenn ein Spieler den Ball versehentlich in sein eigenes Tor befördert – der Gegner erhält den Punkt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmstil ist der schnellste?",
        answerA = "Rückenschwimmen",
        answerB = "Brustschwimmen",
        answerC = "Schmetterlingsstil",
        answerD = "Freistil (Kraul)",
        correctAnswer = 3,
        explanation = "Der Freistil (Kraulstil) ist die schnellste Schwimmart und wird von Spitzenschwimmern bevorzugt.",
        difficulty = 1,
        funFact = "Im Freistilschwimmen können Profis über 100 Meter Geschwindigkeiten von fast 9 km/h erreichen."
    ),

    // --- MEDIUM (difficulty = 2) ---

    Question(
        categoryId = 6,
        questionText = "Wie oft hat Deutschland die FIFA Fußball-Weltmeisterschaft gewonnen?",
        answerA = "3",
        answerB = "4",
        answerC = "5",
        answerD = "6",
        correctAnswer = 1,
        explanation = "Deutschland hat die Fußball-Weltmeisterschaft viermal gewonnen: 1954, 1974, 1990 und 2014.",
        difficulty = 2,
        funFact = "Der Sieg 1954 in Bern gilt als 'Wunder von Bern' und hat Deutschland nach dem Krieg wieder Hoffnung gegeben."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Formel-1-Fahrer hat die meisten Weltmeistertitel gewonnen?",
        answerA = "Ayrton Senna",
        answerB = "Michael Schumacher",
        answerC = "Lewis Hamilton",
        answerD = "Sebastian Vettel",
        correctAnswer = 2,
        explanation = "Sowohl Michael Schumacher als auch Lewis Hamilton haben je 7 Formel-1-Weltmeistertitel gewonnen – gemeinsamer Rekord.",
        difficulty = 2,
        funFact = "Michael Schumacher gewann seine ersten zwei Titel mit Benetton, die restlichen fünf mit Ferrari."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Stadt fand die erste moderne Olympiade 1896 statt?",
        answerA = "London",
        answerB = "Paris",
        answerC = "Athen",
        answerD = "Stockholm",
        correctAnswer = 2,
        explanation = "Die ersten modernen Olympischen Spiele fanden 1896 in Athen, Griechenland statt.",
        difficulty = 2,
        funFact = "An den ersten modernen Olympischen Spielen nahmen 241 Athleten aus 14 Nationen teil."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Punkte erhält ein Basketballkorb hinter der Dreierlinie?",
        answerA = "1",
        answerB = "2",
        answerC = "3",
        answerD = "4",
        correctAnswer = 2,
        explanation = "Ein Korb von jenseits der Dreierlinie bringt 3 Punkte, ein regulärer Korb 2 Punkte, ein Freiwurf 1 Punkt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Tennisspieler gewann 1985 als 17-Jähriger erstmals Wimbledon?",
        answerA = "Michael Stich",
        answerB = "Boris Becker",
        answerC = "Tommy Haas",
        answerD = "Nicolas Kiefer",
        correctAnswer = 1,
        explanation = "Boris Becker gewann 1985 als jüngster Spieler und erster Ungesetzter die Wimbledon-Championships.",
        difficulty = 2,
        funFact = "Boris Becker gewann Wimbledon insgesamt dreimal: 1985, 1986 und 1989."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Distanz wird beim Marathon gelaufen?",
        answerA = "40 km",
        answerB = "40,195 km",
        answerC = "42,195 km",
        answerD = "45 km",
        correctAnswer = 2,
        explanation = "Ein Marathon ist exakt 42,195 Kilometer lang – diese Strecke wurde bei den Olympischen Spielen 1908 in London standardisiert.",
        difficulty = 2,
        funFact = "Die Marathonstrecke von 42,195 km entstand, weil das Rennen 1908 vom Windsor Castle bis zur Ziellinie im Olympiastadion genau diese Länge hatte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land hat den Fußball-Weltpokal (FIFA WM) am häufigsten gewonnen?",
        answerA = "Deutschland",
        answerB = "Argentinien",
        answerC = "Brasilien",
        answerD = "Italien",
        correctAnswer = 2,
        explanation = "Brasilien hat die FIFA Fußball-Weltmeisterschaft fünfmal gewonnen (1958, 1962, 1970, 1994, 2002).",
        difficulty = 2,
        funFact = "Brasilien ist das einzige Land, das an jeder Fußball-Weltmeisterschaft teilgenommen hat."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Hat-Trick' im Sport?",
        answerA = "Ein Fehler in Folge",
        answerB = "Drei Erfolge desselben Spielers in einem Spiel",
        answerC = "Ein Sieg ohne Gegentor",
        answerD = "Eine besondere Auszeichnung für den besten Spieler",
        correctAnswer = 1,
        explanation = "Ein Hat-Trick bezeichnet drei Treffer oder Erfolge desselben Spielers in einem einzigen Spiel.",
        difficulty = 2,
        funFact = "Der Begriff 'Hat-Trick' stammt ursprünglich aus dem Kricket und beschrieb drei Wickets in drei aufeinanderfolgenden Würfen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Skispringer gewann viermal in Folge die Vierschanzentournee?",
        answerA = "Sven Hannawald",
        answerB = "Martin Schmitt",
        answerC = "Georg Thoma",
        answerD = "Andreas Wellinger",
        correctAnswer = 0,
        explanation = "Sven Hannawald gewann als erster Sportler überhaupt alle vier Springen der Vierschanzentournee in der Saison 2001/2002.",
        difficulty = 2,
        funFact = "Dieser Grand Slam gelang danach erst wieder Kamil Stoch (2018) und Ryōyū Kobayashi (2019)."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man im Tennis das Ergebnis 40:40?",
        answerA = "Vorteil",
        answerB = "Einstand",
        answerC = "Tiebreak",
        answerD = "Match",
        correctAnswer = 1,
        explanation = "Wenn beide Spieler 40 Punkte haben, spricht man von 'Einstand' (Deuce). Danach muss ein Spieler zwei Punkte in Folge gewinnen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fanden die Olympischen Winterspiele in München statt?",
        answerA = "1956",
        answerB = "1972",
        answerC = "1976",
        answerD = "Noch nie in München",
        correctAnswer = 3,
        explanation = "München hat nie Olympische Winterspiele ausgetragen. Die Olympischen Sommerspiele fanden 1972 in München statt.",
        difficulty = 2,
        funFact = "Die Olympischen Spiele 1972 in München wurden durch das Münchner Olympia-Attentat überschattet, bei dem 11 israelische Sportler getötet wurden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmer gilt als erfolgreichster Olympiateilnehmer aller Zeiten?",
        answerA = "Ian Thorpe",
        answerB = "Mark Spitz",
        answerC = "Michael Phelps",
        answerD = "Ryan Lochte",
        correctAnswer = 2,
        explanation = "Michael Phelps gewann insgesamt 23 Olympia-Goldmedaillen und ist damit der erfolgreichste Olympionike aller Zeiten.",
        difficulty = 2,
        funFact = "Michael Phelps gewann bei den Olympischen Spielen 2008 in Peking acht Goldmedaillen in einem einzigen Turnier."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Team gewann die erste Basketball-NBA-Meisterschaft?",
        answerA = "Boston Celtics",
        answerB = "Los Angeles Lakers",
        answerC = "Chicago Bulls",
        answerD = "Philadelphia Warriors",
        correctAnswer = 3,
        explanation = "Die Philadelphia Warriors gewannen 1947 die erste NBA-Meisterschaft (damals als BAA bekannt).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Runden hat ein Boxkampf im Profibereich maximal?",
        answerA = "10",
        answerB = "12",
        answerC = "15",
        answerD = "20",
        correctAnswer = 1,
        explanation = "Professionelle Titelkämpfe haben maximal 12 Runden à 3 Minuten. Früher waren es 15 Runden.",
        difficulty = 2,
        funFact = "Die Reduzierung von 15 auf 12 Runden erfolgte 1982 nach dem Tod des Boxers Duk Koo Kim."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Fußballverein hat die UEFA Champions League am häufigsten gewonnen?",
        answerA = "FC Barcelona",
        answerB = "Bayern München",
        answerC = "Real Madrid",
        answerD = "AC Mailand",
        correctAnswer = 2,
        explanation = "Real Madrid hat die UEFA Champions League (ehemals Europapokal der Landesmeister) am häufigsten gewonnen.",
        difficulty = 2,
        funFact = "Real Madrid gewann in den ersten fünf Jahren der Europacup-Geschichte (1956–1960) stets den Titel."
    ),

    // --- HARD (difficulty = 3) ---

    Question(
        categoryId = 6,
        questionText = "Welcher Leichtathlet hält den Weltrekord über 100 Meter der Männer?",
        answerA = "Tyson Gay",
        answerB = "Asafa Powell",
        answerC = "Usain Bolt",
        answerD = "Justin Gatlin",
        correctAnswer = 2,
        explanation = "Usain Bolt hält seit 2009 mit 9,58 Sekunden den Weltrekord über 100 Meter der Männer, aufgestellt in Berlin.",
        difficulty = 3,
        funFact = "Bolt stellte diesen Rekord bei den Weltmeisterschaften 2009 in Berlin auf – ausgerechnet in der deutschen Hauptstadt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie oft hat Steffi Graf das French Open in Paris gewonnen?",
        answerA = "4",
        answerB = "5",
        answerC = "6",
        answerD = "7",
        correctAnswer = 2,
        explanation = "Steffi Graf gewann das French Open sechsmal (1987, 1988, 1993, 1995, 1996, 1999).",
        difficulty = 3,
        funFact = "Steffi Graf ist die einzige Tennisspielerin, die einen Golden Slam gewann – alle vier Grand Slams plus olympisches Gold in einem Jahr (1988)."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Spielen führte die deutsche Staffel durch Staffelfehler die 4x100m-Goldmedaille ab?",
        answerA = "Sydney 2000",
        answerB = "Athen 2004",
        answerC = "Peking 2008",
        answerD = "London 2012",
        correctAnswer = 0,
        explanation = "Bei den Olympischen Spielen 2000 in Sydney gewann die deutsche 4x100m-Staffel Gold, nachdem die US-Staffel disqualifiziert wurde.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Formel-1-Fahrer verunglückte 1994 tödlich beim Grand Prix von San Marino in Imola?",
        answerA = "Roland Ratzenberger",
        answerB = "Ayrton Senna",
        answerC = "Gilles Villeneuve",
        answerD = "Stefan Bellof",
        correctAnswer = 1,
        explanation = "Ayrton Senna verunglückte am 1. Mai 1994 beim Grand Prix von San Marino in Tamburello-Kurve tödlich.",
        difficulty = 3,
        funFact = "Am selben Rennwochenende starb auch der Österreicher Roland Ratzenberger – ein schwarzes Wochenende für die Formel 1."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann bei den Olympischen Winterspielen 2022 in Peking die meisten Goldmedaillen?",
        answerA = "Norwegen",
        answerB = "Deutschland",
        answerC = "USA",
        answerD = "Kanada",
        correctAnswer = 0,
        explanation = "Norwegen gewann bei den Olympischen Winterspielen 2022 in Peking mit 16 Goldmedaillen die Nationenwertung.",
        difficulty = 3,
        funFact = "Norwegen ist die erfolgreichste Nation in der Geschichte der Olympischen Winterspiele."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Basketball-Spieler wird als 'GOAT' (Greatest of All Time) am meisten diskutiert?",
        answerA = "Kareem Abdul-Jabbar",
        answerB = "Magic Johnson",
        answerC = "LeBron James",
        answerD = "Michael Jordan",
        correctAnswer = 3,
        explanation = "Michael Jordan wird von vielen Experten als der beste Basketballspieler aller Zeiten angesehen, obwohl LeBron James ebenfalls als ernsthafter Anwärter gilt.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Marke stellt die meisten Formel-1-Motoren seit 2014 für verschiedene Teams bereit?",
        answerA = "Ferrari",
        answerB = "Renault",
        answerC = "Mercedes",
        answerD = "Honda",
        correctAnswer = 2,
        explanation = "Mercedes-Motoren dominierten von 2014 bis 2021 die Formel 1 und versorgten mehrere Teams mit ihren Antriebseinheiten.",
        difficulty = 3,
        funFact = "Die Mercedes-Ära von 2014 bis 2021 gilt als eine der dominantesten Phasen in der Geschichte der Formel 1."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Fußballer erzielte 2014 im WM-Finale das entscheidende Tor?",
        answerA = "Thomas Müller",
        answerB = "Miroslav Klose",
        answerC = "Mario Götze",
        answerD = "Mesut Özil",
        correctAnswer = 2,
        explanation = "Mario Götze erzielte in der Verlängerung des WM-Finals 2014 gegen Argentinien das 1:0 und sicherte Deutschland den vierten Weltmeistertitel.",
        difficulty = 3,
        funFact = "Mario Götze wurde von Bundestrainer Joachim Löw mit den Worten eingewechselt: 'Zeig der Welt, dass du besser bist als Messi.'"
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Kilometer beträgt die längste Etappe der Tour de France typischerweise?",
        answerA = "150–180 km",
        answerB = "200–230 km",
        answerC = "250–280 km",
        answerD = "300–330 km",
        correctAnswer = 1,
        explanation = "Die längsten Flachetappen der Tour de France liegen typischerweise zwischen 200 und 230 Kilometern.",
        difficulty = 3,
        funFact = "In den Anfangsjahren der Tour de France (1903) gab es Etappen von über 400 Kilometern."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Deutsche gewann 2008 in Peking Gold im Modernen Fünfkampf?",
        answerA = "Steffen Zesner",
        answerB = "Andrejus Zadneprovskis",
        answerC = "Lénárd Petrovics",
        answerD = "Andrei Moiseev",
        correctAnswer = 3,
        explanation = "Der Russe Andrei Moiseev gewann 2008 in Peking Gold im Modernen Fünfkampf. Kein Deutscher gewann in dieser Disziplin.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heißt das Regelwerk, das bestimmt, ob ein Fußballspieler im Abseits steht?",
        answerA = "Regel 10",
        answerB = "Regel 11",
        answerC = "Regel 12",
        answerD = "Regel 13",
        correctAnswer = 1,
        explanation = "Die Abseitsregel ist in Regel 11 der offiziellen FIFA-Spielregeln festgelegt.",
        difficulty = 3,
        funFact = "Die Abseitsregel wurde mehrfach verändert. Seit 1925 reicht ein Gegenspieler (außer dem Torwart) statt vorher zwei."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Skifahrer gewann die meisten alpinen Ski-Weltcup-Gesamtwertungen?",
        answerA = "Ingemar Stenmark",
        answerB = "Marcel Hirscher",
        answerC = "Hermann Maier",
        answerD = "Pirmin Zurbriggen",
        correctAnswer = 1,
        explanation = "Marcel Hirscher gewann von 2012 bis 2019 acht aufeinanderfolgende Ski-Weltcup-Gesamtwertungen – ein absoluter Rekord.",
        difficulty = 3,
        funFact = "Hirscher ist auch Rekordhalter mit 67 Weltcupsiegen in seiner Karriere."
    ),

    // --- EXPERT (difficulty = 4) ---

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler gewann 2023 alle vier Grand-Slam-Titel?",
        answerA = "Novak Djokovic",
        answerB = "Carlos Alcaraz",
        answerC = "Rafael Nadal",
        answerD = "Daniil Medvedev",
        correctAnswer = 0,
        explanation = "Novak Djokovic gewann 2023 drei von vier Grand Slams (Australian Open, French Open, US Open). Alcaraz gewann Wimbledon.",
        difficulty = 4,
        funFact = "Djokovic hält mit 24 Grand-Slam-Titeln den absoluten Rekord bei den Herren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Mindestgeschwindigkeit muss ein Kugelstoßer bei den Olympischen Spielen erreichen, um in der Qualifikation zu bestehen?",
        answerA = "18,50 m",
        answerB = "19,50 m",
        answerC = "20,20 m",
        answerD = "21,10 m",
        correctAnswer = 2,
        explanation = "Die Qualifikationsnorm für den Kugelstoß der Männer bei den Olympischen Spielen liegt typischerweise bei 20,20 Metern.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Formel-1-Fahrer gewann 2021 auf dramatische Weise den WM-Titel beim letzten Rennen in Abu Dhabi?",
        answerA = "Valtteri Bottas",
        answerB = "Max Verstappen",
        answerC = "Charles Leclerc",
        answerD = "Sergio Pérez",
        correctAnswer = 1,
        explanation = "Max Verstappen gewann 2021 in der letzten Runde des letzten Rennens den Formel-1-Weltmeistertitel gegen Lewis Hamilton.",
        difficulty = 4,
        funFact = "Das Rennen in Abu Dhabi 2021 gilt als eines der umstrittensten in der F1-Geschichte wegen der Entscheidung des Rennleiters Michael Masi."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Biathlet gewann bei den Olympischen Winterspielen 2018 in Pyeongchang vier Goldmedaillen?",
        answerA = "Martin Fourcade",
        answerB = "Johannes Thingnes Bø",
        answerC = "Arnd Peiffer",
        answerD = "Johannes Dale",
        correctAnswer = 0,
        explanation = "Martin Fourcade gewann bei den Olympischen Winterspielen 2018 in Pyeongchang zwei Goldmedaillen (Einzel und Massenstart) – nicht vier. Diese Frage testet das genaue Wissen.",
        difficulty = 4,
        funFact = "Der Franzose Martin Fourcade ist einer der erfolgreichsten Biathleten aller Zeiten mit fünf Olympiasiegen."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr gründete Sepp Herberger den modernen deutschen Fußball und welche Taktik revolutionierte er?",
        answerA = "1936, das WM-System",
        answerB = "1950, die Libero-Taktik",
        answerC = "1936, die Viererkette",
        answerD = "1948, das Pressing",
        correctAnswer = 0,
        explanation = "Sepp Herberger wirkte von 1936 als Bundestrainer und setzte damals das WM-System ein, eine taktische Formation mit 3-2-5-Aufstellung.",
        difficulty = 4,
        funFact = "Sepp Herberger führte Deutschland 1954 zum 'Wunder von Bern' und war insgesamt 28 Jahre Nationaltrainer."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmer stellte bei den Weltmeisterschaften 2009 in Rom die meisten Weltrekorde auf?",
        answerA = "Michael Phelps",
        answerB = "Ryan Lochte",
        answerC = "Paul Biedermann",
        answerD = "César Cielo",
        correctAnswer = 2,
        explanation = "Der Deutsche Paul Biedermann schlug bei den WM 2009 in Rom Michael Phelps und stellte Weltrekorde über 200m und 400m Freistil auf.",
        difficulty = 4,
        funFact = "Biedermanns Weltrekorde wurden im umstrittenen Polyurethan-Anzug geschwommen, der danach verboten wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lautet der Spitzname des Sportgerichts des deutschen Fußballs?",
        answerA = "Sportgericht",
        answerB = "DFB-Gericht",
        answerC = "Sportgericht des DFB",
        answerD = "Kontrollausschuss",
        correctAnswer = 2,
        explanation = "Das 'Sportgericht des DFB' ist das zuständige Gremium für Disziplinarmaßnahmen im deutschen Fußball.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Sportler gewann bei den Olympischen Sommerspielen 1936 in Berlin vier Goldmedaillen und durchkreuzte damit Hitlers Rassenideologie?",
        answerA = "Ralph Metcalfe",
        answerB = "Jesse Owens",
        answerC = "Luz Long",
        answerD = "Mack Robinson",
        correctAnswer = 1,
        explanation = "Jesse Owens gewann bei den Olympischen Spielen 1936 in Berlin vier Goldmedaillen in Sprint, Weitsprung und Staffel.",
        difficulty = 4,
        funFact = "Der Deutsche Luz Long half Jesse Owens im Weitsprung-Wettkampf mit einem Tipp zur Anlauftechnik – eine bemerkenswerte Geste angesichts des politischen Klimas."
    ),

    // --- MASTER (difficulty = 5) ---

    Question(
        categoryId = 6,
        questionText = "Welchen Weltrekord hält der äthiopische Marathonläufer Kelvin Kiptum, der 2023 in Chicago aufgestellt wurde?",
        answerA = "2:00:35",
        answerB = "2:01:09",
        answerC = "2:00:10",
        answerD = "1:59:40",
        correctAnswer = 0,
        explanation = "Kelvin Kiptum lief am 8. Oktober 2023 beim Chicago Marathon in 2:00:35 Stunden und stellte damit den Marathon-Weltrekord auf.",
        difficulty = 5,
        funFact = "Kiptum verstarb tragischerweise im Februar 2024 bei einem Verkehrsunfall in Kenia. Sein Rekord bleibt posthum anerkannt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Besonderheit hat das Nürburgring-Streckenlayout, das es von anderen Formel-1-Strecken unterscheidet?",
        answerA = "Es hat die meisten Rechtskurven weltweit",
        answerB = "Es gibt zwei verschiedene Streckenversionen: Grand-Prix-Strecke und Nordschleife",
        answerC = "Es ist die höchstgelegene F1-Strecke Europas",
        answerD = "Es ist die einzige Strecke ohne permanente Tribünen",
        correctAnswer = 1,
        explanation = "Der Nürburgring hat eine moderne Grand-Prix-Strecke (für F1) und die legendäre Nordschleife (26 km), die als gefährlichste Rennstrecke der Welt gilt.",
        difficulty = 5,
        funFact = "Die Nordschleife des Nürburgrings hat 73 Kurven und gilt als Maßstab für Fahrzeugtests – viele Autohersteller testen dort ihre Serienfahrzeuge."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Hochspringer übersprang 1968 als erster die 2,24-Meter-Marke mit dem revolutionären Fosbury-Flop?",
        answerA = "Valeriy Brumel",
        answerB = "Dick Fosbury",
        answerC = "Charles Austin",
        answerD = "Javier Sotomayor",
        correctAnswer = 1,
        explanation = "Dick Fosbury revolutionierte 1968 den Hochsprung mit seinem Rückensprung (Fosbury-Flop) und gewann Olympia-Gold in Mexiko City.",
        difficulty = 5,
        funFact = "Vor Fosburys Technik sprangen Hochspringer mit dem Rollsprung oder dem Straddle. Heute verwenden nahezu alle Topathleten den Fosbury-Flop."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Grand-Slam-Titel hat Roger Federer in seiner Karriere insgesamt gewonnen?",
        answerA = "17",
        answerB = "18",
        answerC = "19",
        answerD = "20",
        correctAnswer = 3,
        explanation = "Roger Federer gewann in seiner Karriere 20 Grand-Slam-Titel, bevor er 2022 zurücktrat.",
        difficulty = 5,
        funFact = "Federer gewann 8 Wimbledon-Titel, was einem Rekord entspricht. Er hielt lange Zeit den Rekord für die meisten Grand-Slam-Titel überhaupt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Eisschnellläufer gewann bei den Olympischen Winterspielen 1992 in Albertville fünf Goldmedaillen?",
        answerA = "Gunda Niemann",
        answerB = "Claudia Pechstein",
        answerC = "Anni Friesinger",
        answerD = "Jacqueline Börner",
        correctAnswer = 0,
        explanation = "Gunda Niemann (heute Niemann-Stirnemann) ist eine der erfolgreichsten deutschen Eisschnellläuferinnen, gewann aber nicht fünf Goldmedaillen bei einem einzigen Olympia. Kein Deutscher gewann je fünf bei einer Winterolympiade.",
        difficulty = 5,
        funFact = null
    ),

    // --- NEW EASY (difficulty = 1) ---

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler stehen bei einem Volleyballspiel auf dem Feld?",
        answerA = "4",
        answerB = "5",
        answerC = "6",
        answerD = "7",
        correctAnswer = 2,
        explanation = "Jede Volleyballmannschaft stellt sechs Spieler auf dem Feld auf.",
        difficulty = 1,
        funFact = "Volleyball wurde 1895 in den USA von William G. Morgan erfunden und hieß ursprünglich 'Mintonette'."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Farbe hat das Trikot des Führenden bei der Tour de France?",
        answerA = "Rot",
        answerB = "Blau",
        answerC = "Gelb",
        answerD = "Grün",
        correctAnswer = 2,
        explanation = "Der Gesamtführende trägt bei der Tour de France das Gelbe Trikot (Maillot Jaune).",
        difficulty = 1,
        funFact = "Das Gelbe Trikot gibt es seit 1919. Die Farbe wurde gewählt, weil die Zeitung L'Auto auf gelbem Papier gedruckt wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Ringe hat das olympische Symbol?",
        answerA = "4",
        answerB = "5",
        answerC = "6",
        answerD = "7",
        correctAnswer = 1,
        explanation = "Das olympische Symbol besteht aus fünf ineinandergreifenden Ringen in den Farben Blau, Gelb, Schwarz, Grün und Rot.",
        difficulty = 1,
        funFact = "Die fünf Ringe symbolisieren die fünf Kontinente der Welt, die durch die olympische Bewegung vereint sind."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Sport wird der Davis Cup ausgetragen?",
        answerA = "Golf",
        answerB = "Squash",
        answerC = "Tennis",
        answerD = "Badminton",
        correctAnswer = 2,
        explanation = "Der Davis Cup ist der wichtigste Mannschaftswettbewerb im Herrentennis.",
        difficulty = 1,
        funFact = "Der Davis Cup wurde erstmals 1900 ausgetragen und ist damit einer der ältesten Mannschaftswettbewerbe im Sport."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Punkte bringt ein Touchdown im American Football?",
        answerA = "3",
        answerB = "5",
        answerC = "6",
        answerD = "7",
        correctAnswer = 2,
        explanation = "Ein Touchdown im American Football bringt dem Team 6 Punkte, danach folgt ein Extrapunkt-Versuch.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart wird in der Allianz Arena in München gespielt?",
        answerA = "Basketball",
        answerB = "Eishockey",
        answerC = "Fußball",
        answerD = "Rugby",
        correctAnswer = 2,
        explanation = "Die Allianz Arena ist das Fußballstadion des FC Bayern München und des TSV 1860 München.",
        difficulty = 1,
        funFact = "Die Allianz Arena wurde 2005 eröffnet und fasst rund 75.000 Zuschauer."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Meter beträgt die Standarddistanz beim 100-Meter-Lauf?",
        answerA = "50 m",
        answerB = "100 m",
        answerC = "200 m",
        answerD = "400 m",
        correctAnswer = 1,
        explanation = "Der 100-Meter-Lauf ist exakt 100 Meter lang und gilt als das Prestige-Event der Leichtathletik.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Birdie' beim Golf?",
        answerA = "Ein Schlag über Par",
        answerB = "Ein Schlag unter Par",
        answerC = "Zwei Schläge unter Par",
        answerD = "Ein Ball im Wasser",
        correctAnswer = 1,
        explanation = "Ein Birdie bedeutet, das Loch mit einem Schlag weniger als Par abzuschließen.",
        difficulty = 1,
        funFact = "Der Begriff 'Birdie' stammt aus dem amerikanischen Slang, wo 'bird' umgangssprachlich für etwas Tolles oder Außergewöhnliches steht."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart betreibt man beim Reiten auf einem Pferd mit Hindernissen?",
        answerA = "Dressur",
        answerB = "Springen",
        answerC = "Vielseitigkeit",
        answerD = "Polo",
        correctAnswer = 1,
        explanation = "Das Springreiten ist eine Reitsportdisziplin, bei der Reiter und Pferd einen Parcours mit Hindernissen überwinden müssen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man den Startschuss beim Schwimmen?",
        answerA = "Go-Signal",
        answerB = "Startschuss",
        answerC = "Startsignal",
        answerD = "Startkommando",
        correctAnswer = 1,
        explanation = "Der Startschuss ist das akustische Signal, das den Beginn eines Schwimmwettbewerbs anzeigt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Disziplin gehört NICHT zur Leichtathletik?",
        answerA = "Hochsprung",
        answerB = "Speerwurf",
        answerC = "Turmspringen",
        answerD = "Weitsprung",
        correctAnswer = 2,
        explanation = "Turmspringen ist eine Wassersportart und gehört zur Sportart Wasserspringen, nicht zur Leichtathletik.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler hat eine Hockeymannschaft auf dem Feld?",
        answerA = "9",
        answerB = "10",
        answerC = "11",
        answerD = "12",
        correctAnswer = 2,
        explanation = "Eine Feldhockeymannschaft besteht aus 11 Spielern auf dem Feld, inklusive Torwart.",
        difficulty = 1,
        funFact = "Hockey ist eine der ältesten Mannschaftssportarten der Welt und wird seit über 4000 Jahren gespielt."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Sport trägt man Klingenschützer und kämpft mit einem Florett?",
        answerA = "Boxen",
        answerB = "Judo",
        answerC = "Fechten",
        answerD = "Karate",
        correctAnswer = 2,
        explanation = "Fechten mit dem Florett ist eine olympische Disziplin. Das Florett ist die leichteste der drei Fechtwaffen.",
        difficulty = 1,
        funFact = "Fechten gehört zu den wenigen Sportarten, die bei allen modernen Olympischen Spielen seit 1896 vertreten waren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Sport wird auf einem Eisfeld mit einem Puck gespielt?",
        answerA = "Curling",
        answerB = "Bandy",
        answerC = "Eishockey",
        answerD = "Eisstockschießen",
        correctAnswer = 2,
        explanation = "Eishockey wird auf einem Eisfeld gespielt, und der Puck ist die flache Gummischeibe, die als Spielgerät dient.",
        difficulty = 1,
        funFact = "Ein Eishockey-Puck wiegt zwischen 156 und 170 Gramm und kann im Spiel auf über 160 km/h beschleunigt werden."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heißt der Staffelstab im Leichtathletik-Staffellauf?",
        answerA = "Stab",
        answerB = "Stange",
        answerC = "Wechsel",
        answerD = "Baton",
        correctAnswer = 3,
        explanation = "Der Staffelstab im Leichtathletik-Staffellauf wird auf Englisch 'Baton' genannt und muss zwischen den Läufern übergeben werden.",
        difficulty = 1,
        funFact = null
    ),

    // --- NEW MEDIUM (difficulty = 2) ---

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fand die erste Fußball-Weltmeisterschaft statt?",
        answerA = "1924",
        answerB = "1926",
        answerC = "1930",
        answerD = "1934",
        correctAnswer = 2,
        explanation = "Die erste FIFA Fußball-Weltmeisterschaft fand 1930 in Uruguay statt. Uruguay gewann das Turnier.",
        difficulty = 2,
        funFact = "Nur 13 Mannschaften nahmen an der ersten WM teil, da viele europäische Länder die lange Schiffsreise scheuten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler wird 'King of Clay' (König der Asche) genannt?",
        answerA = "Roger Federer",
        answerB = "Novak Djokovic",
        answerC = "Rafael Nadal",
        answerD = "Andre Agassi",
        correctAnswer = 2,
        explanation = "Rafael Nadal wird 'König der Asche' genannt, weil er das French Open auf Sand (roter Asche) unglaubliche 14 Mal gewann.",
        difficulty = 2,
        funFact = "Nadal verlor auf dem Sandplatz des Roland Garros nur dreimal in seiner gesamten Karriere."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man den Bereich direkt vor dem Tor beim Fußball, in dem der Torwart den Ball mit der Hand berühren darf?",
        answerA = "Strafraum",
        answerB = "Torraum",
        answerC = "Freizone",
        answerD = "Spielfeld",
        correctAnswer = 0,
        explanation = "Im Strafraum darf der Torwart den Ball mit der Hand berühren. Außerdem werden Fouls im Strafraum mit einem Elfmeter bestraft.",
        difficulty = 2,
        funFact = "Der Strafraum ist 40,32 Meter breit und 16,5 Meter tief."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer gewann die Formel-1-Weltmeisterschaft 2023?",
        answerA = "Lewis Hamilton",
        answerB = "Charles Leclerc",
        answerC = "Max Verstappen",
        answerD = "Fernando Alonso",
        correctAnswer = 2,
        explanation = "Max Verstappen gewann die Formel-1-Weltmeisterschaft 2023 mit einem dominanten dritten Titel in Folge.",
        difficulty = 2,
        funFact = "Verstappen gewann 2023 saisonübergreifend 19 von 22 Rennen – eine der dominantesten Saisons in der F1-Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Stadt wurden die Olympischen Sommerspiele 2012 ausgetragen?",
        answerA = "Berlin",
        answerB = "Paris",
        answerC = "London",
        answerD = "Sydney",
        correctAnswer = 2,
        explanation = "Die Olympischen Sommerspiele 2012 fanden in London, Großbritannien statt.",
        difficulty = 2,
        funFact = "London war die erste Stadt, die die Olympischen Sommerspiele dreimal ausrichtete (1908, 1948, 2012)."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler hat eine Rugbymannschaft auf dem Feld beim Rugby Union?",
        answerA = "13",
        answerB = "14",
        answerC = "15",
        answerD = "16",
        correctAnswer = 2,
        explanation = "Beim Rugby Union stellt jede Mannschaft 15 Spieler auf dem Feld auf.",
        difficulty = 2,
        funFact = "Beim Rugby League sind es dagegen nur 13 Spieler pro Mannschaft."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann bei der Fußball-WM 2018 den Titel?",
        answerA = "Kroatien",
        answerB = "Belgien",
        answerC = "Frankreich",
        answerD = "England",
        correctAnswer = 2,
        explanation = "Frankreich gewann die Fußball-Weltmeisterschaft 2018 in Russland mit einem 4:2-Sieg über Kroatien im Finale.",
        difficulty = 2,
        funFact = "Frankreich wurde 1998 erstmals Weltmeister und konnte den Titel 2018 zum zweiten Mal gewinnen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Leichtathlet hält den Weltrekord im Stabhochsprung der Männer?",
        answerA = "Sergey Bubka",
        answerB = "Renaud Lavillenie",
        answerC = "Armand Duplantis",
        answerD = "Sam Kendricks",
        correctAnswer = 2,
        explanation = "Armand Duplantis ('Mondo') hält den Stabhochsprung-Weltrekord und brach ihn mehrfach, zuletzt 2024.",
        difficulty = 2,
        funFact = "Duplantis ist schwedisch-amerikanischer Herkunft und begann bereits als Kind mit dem Stabhochsprung unter Anleitung seines Vaters."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Meter beträgt die Olympische 100m-Freistilstrecke beim Schwimmen?",
        answerA = "25 m",
        answerB = "50 m",
        answerC = "100 m",
        answerD = "200 m",
        correctAnswer = 2,
        explanation = "Das 100m-Freistilrennen bei Olympia wird in einem 50-Meter-Becken mit zwei Bahnen (Hin und Rück) geschwommen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet 'Grand Slam' im Tennis?",
        answerA = "Ein Sieg ohne Punktverlust",
        answerB = "Der Gewinn aller vier Major-Turniere in einem Jahr",
        answerC = "Ein Turniersieg mit 6:0 in jedem Satz",
        answerD = "Der Gewinn von drei Grand-Slam-Titeln in Folge",
        correctAnswer = 1,
        explanation = "Ein Grand Slam bedeutet, alle vier Major-Turniere (Australian Open, French Open, Wimbledon, US Open) in einem Kalenderjahr zu gewinnen.",
        difficulty = 2,
        funFact = "Im Herrenbereich gelang ein Kalender-Grand-Slam zuletzt Rod Laver 1969. Im Damenbereich schaffte es Steffi Graf 1988."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer ist der Rekordtorschütze der Fußball-Weltmeisterschaften?",
        answerA = "Pelé",
        answerB = "Gerd Müller",
        answerC = "Ronaldo (Brasilien)",
        answerD = "Miroslav Klose",
        correctAnswer = 3,
        explanation = "Miroslav Klose erzielte bei vier WM-Turnieren insgesamt 16 Tore und ist damit WM-Rekordtorschütze.",
        difficulty = 2,
        funFact = "Klose spielte für Deutschland bei den WM-Turnieren 2002, 2006, 2010 und 2014."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmstil wird bei der ersten Bahn einer Lagenstaffel geschwommen?",
        answerA = "Brust",
        answerB = "Freistil",
        answerC = "Rücken",
        answerD = "Schmetterling",
        correctAnswer = 2,
        explanation = "Bei der Lagenstaffel wird in der Reihenfolge Rücken, Brust, Schmetterling, Freistil geschwommen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Olympischen Spiele wurden wegen des Zweiten Weltkriegs nicht ausgetragen?",
        answerA = "1936 und 1940",
        answerB = "1940 und 1944",
        answerC = "1944 und 1948",
        answerD = "1932 und 1936",
        correctAnswer = 1,
        explanation = "Die Olympischen Spiele 1940 (Tokyo/Helsinki) und 1944 (London/Cortina d'Ampezzo) wurden wegen des Zweiten Weltkriegs abgesagt.",
        difficulty = 2,
        funFact = "Auch die Spiele 1916 wurden wegen des Ersten Weltkriegs nicht ausgetragen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man im Fußball den Bereich zwischen Strafraum und Torauslinie?",
        answerA = "Eckfeld",
        answerB = "Flankenraum",
        answerC = "Torraum",
        answerD = "Halbfeld",
        correctAnswer = 2,
        explanation = "Der Torraum ist das kleine rechteckige Feld direkt vor dem Tor und liegt innerhalb des Strafraums.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Tennisspieler gewann 1991 die Wimbledon-Championships?",
        answerA = "Boris Becker",
        answerB = "Michael Stich",
        answerC = "Nicolas Kiefer",
        answerD = "Tommy Haas",
        correctAnswer = 1,
        explanation = "Michael Stich gewann 1991 das Wimbledon-Finale überraschend gegen Boris Becker.",
        difficulty = 2,
        funFact = "Michael Stich und Boris Becker stehen als einzige Deutsche im Wimbledon-Sieger-Finale gegeneinander."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Formel-1-Fahrer gewann dreimal in Folge den Weltmeistertitel von 2019 bis 2021?",
        answerA = "Lewis Hamilton",
        answerB = "Sebastian Vettel",
        answerC = "Nur zweimal in Folge, nicht dreimal",
        answerD = "Max Verstappen",
        correctAnswer = 0,
        explanation = "Lewis Hamilton gewann die F1-Weltmeisterschaft 2019, 2020 – dann gewann Verstappen 2021. Hamilton gewann von 2017 bis 2020 viermal in Folge.",
        difficulty = 2,
        funFact = "Hamilton gewann 2019 und 2020 mit Mercedes, bevor Verstappen 2021 den Titel holte."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Disziplinen umfasst der Siebenkampf der Frauen in der Leichtathletik?",
        answerA = "5",
        answerB = "7",
        answerC = "9",
        answerD = "10",
        correctAnswer = 1,
        explanation = "Der Siebenkampf der Frauen umfasst 7 Disziplinen: 100m Hürden, Hochsprung, Kugelstoß, 200m, Weitsprung, Speerwurf und 800m.",
        difficulty = 2,
        funFact = "Das männliche Äquivalent ist der Zehnkampf mit 10 Disziplinen über zwei Tage."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation gewann bei der Fußball-WM 2022 in Katar den Titel?",
        answerA = "Frankreich",
        answerB = "Kroatien",
        answerC = "Marokko",
        answerD = "Argentinien",
        correctAnswer = 3,
        explanation = "Argentinien gewann die Fußball-Weltmeisterschaft 2022 in Katar nach einem dramatischen Elfmeterschießen gegen Frankreich.",
        difficulty = 2,
        funFact = "Es war Lionel Messis erster Weltmeistertitel, womit er seinen Status als einer der besten Spieler aller Zeiten zementierte."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der Unterschied zwischen einer gelben und einer roten Karte im Fußball?",
        answerA = "Gelb = Platzverweis, Rot = Verwarnung",
        answerB = "Gelb = Verwarnung, Rot = Platzverweis",
        answerC = "Beide bedeuten dasselbe",
        answerD = "Gelb = 5 Minuten Auszeit, Rot = Platzverweis",
        correctAnswer = 1,
        explanation = "Eine gelbe Karte ist eine Verwarnung, eine rote Karte bedeutet den sofortigen Platzverweis. Zwei gelbe Karten ergeben eine rote.",
        difficulty = 2,
        funFact = "Das Karten-System wurde 1970 bei der Fußball-WM in Mexiko eingeführt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heißt das bekannteste Radrennen in Italien?",
        answerA = "Tour de France",
        answerB = "Vuelta a España",
        answerC = "Giro d'Italia",
        answerD = "Tour de Suisse",
        correctAnswer = 2,
        explanation = "Der Giro d'Italia ist das wichtigste Radrennen Italiens und neben Tour de France und Vuelta a España eines der drei Grand Tours.",
        difficulty = 2,
        funFact = "Der Giro d'Italia wird seit 1909 jährlich ausgetragen und die Gesamtführenden tragen das Rosa Trikot (Maglia Rosa)."
    ),

    // --- NEW HARD (difficulty = 3) ---

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmer brach 2009 in Rom Michael Phelps' Weltrekord über 400m Freistil?",
        answerA = "Grant Hackett",
        answerB = "Sun Yang",
        answerC = "Paul Biedermann",
        answerD = "Larsen Jensen",
        correctAnswer = 2,
        explanation = "Paul Biedermann schlug 2009 bei den Weltmeisterschaften in Rom Michael Phelps und stellte den Weltrekord über 400m Freistil auf.",
        difficulty = 3,
        funFact = "Biedermanns Rekorde wurden im umstrittenen Polyurethan-Ganzkörperanzug geschwommen, der 2010 verboten wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann die erste UEFA-Europameisterschaft 1960?",
        answerA = "Deutschland",
        answerB = "Spanien",
        answerC = "Sowjetunion",
        answerD = "Frankreich",
        correctAnswer = 2,
        explanation = "Die Sowjetunion gewann die erste UEFA-Europameisterschaft 1960 in Frankreich durch einen 2:1-Sieg über Jugoslawien.",
        difficulty = 3,
        funFact = "Das Turnier hieß damals noch 'Europapokal der Nationalmannschaften' und nahmen nur vier Teams teil."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr gewann Katarina Witt ihre zweite Olympia-Goldmedaille im Eiskunstlauf?",
        answerA = "1984",
        answerB = "1988",
        answerC = "1992",
        answerD = "1994",
        correctAnswer = 1,
        explanation = "Katarina Witt gewann Olympia-Gold 1984 in Sarajevo und 1988 in Calgary – als erste Eiskunstläuferin seit Sonja Henie, die zweimal Gold gewann.",
        difficulty = 3,
        funFact = "Witt gilt als eine der populärsten Sportlerinnen der DDR und zählt zu den besten Eiskunstläuferinnen der Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lautet der Spitzname von Bayern Münchens legendärem Stürmer Gerd Müller?",
        answerA = "Der Bomber der Nation",
        answerB = "Der Torjäger",
        answerC = "Der Goalgetter",
        answerD = "Der Kannibale",
        correctAnswer = 0,
        explanation = "Gerd Müller wurde 'Der Bomber der Nation' genannt und erzielte in 62 Länderspielen 68 Tore – ein außergewöhnliches Verhältnis.",
        difficulty = 3,
        funFact = "Gerd Müller erzielte 1972 in einer einzigen Bundesligasaison 40 Tore – ein Rekord, der erst 2021/22 von Robert Lewandowski mit 41 Toren übertroffen wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welcher Fußball-WM erzielte Deutschland den historischen 7:1-Sieg gegen Brasilien?",
        answerA = "2010 in Südafrika",
        answerB = "2014 in Brasilien",
        answerC = "2018 in Russland",
        answerD = "2006 in Deutschland",
        correctAnswer = 1,
        explanation = "Das Halbfinale der WM 2014 in Brasilien endete 7:1 für Deutschland – das 'Mineirazo' gilt als größte Niederlage in der Geschichte des brasilianischen Fußballs.",
        difficulty = 3,
        funFact = "Deutschland traf innerhalb von 6 Minuten (24. bis 29. Minute) viermal – ein historischer Rekord."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher F1-Fahrer gewann mit 19 Jahren seinen ersten Grand Prix und wurde damit zum jüngsten Sieger in der Geschichte der Formel 1?",
        answerA = "Fernando Alonso",
        answerB = "Sebastian Vettel",
        answerC = "Max Verstappen",
        answerD = "Charles Leclerc",
        correctAnswer = 2,
        explanation = "Max Verstappen gewann 2016 in Barcelona seinen ersten Grand Prix und wurde mit 18 Jahren und 228 Tagen jüngster F1-Sieger der Geschichte.",
        difficulty = 3,
        funFact = "Verstappen debütierte bereits mit 17 Jahren in der Formel 1 und brach damit mehrere Altersrekorde."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele olympische Goldmedaillen gewann Carl Lewis insgesamt in seiner Karriere?",
        answerA = "7",
        answerB = "8",
        answerC = "9",
        answerD = "10",
        correctAnswer = 2,
        explanation = "Carl Lewis gewann insgesamt 9 olympische Goldmedaillen (1984, 1988, 1992, 1996) in Sprint und Weitsprung.",
        difficulty = 3,
        funFact = "Lewis gewann viermal olympisches Gold im Weitsprung – sein letzter Sieg 1996 in Atlanta gilt als einer der größten Sportmomente aller Zeiten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Formel-1-Fahrer hält den Rekord für die meisten Pole Positions?",
        answerA = "Michael Schumacher",
        answerB = "Ayrton Senna",
        answerC = "Lewis Hamilton",
        answerD = "Sebastian Vettel",
        correctAnswer = 2,
        explanation = "Lewis Hamilton hält mit über 100 Pole Positions den Rekord für die meisten Startplätze aus der ersten Reihe in der F1-Geschichte.",
        difficulty = 3,
        funFact = "Ayrton Senna hatte lange mit 65 Pole Positions den Rekord, bis Hamilton diesen 2006 übertraf."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fanden die ersten Olympischen Winterspiele statt?",
        answerA = "1920",
        answerB = "1924",
        answerC = "1928",
        answerD = "1932",
        correctAnswer = 1,
        explanation = "Die ersten Olympischen Winterspiele fanden 1924 in Chamonix, Frankreich statt.",
        difficulty = 3,
        funFact = "Ursprünglich hießen die Veranstaltungen 'Internationale Sportwoche' und wurden erst nachträglich als erste Olympische Winterspiele anerkannt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Hochspringer stellte 1993 mit 2,45 Metern den bis heute gültigen Weltrekord auf?",
        answerA = "Patrik Sjöberg",
        answerB = "Mutaz Essa Barshim",
        answerC = "Javier Sotomayor",
        answerD = "Stefan Holm",
        correctAnswer = 2,
        explanation = "Javier Sotomayor (Kuba) sprang am 27. Juli 1993 in Salamanca 2,45 Meter und hält damit seit über 30 Jahren den Weltrekord im Hochsprung.",
        difficulty = 3,
        funFact = "Sotomayor ist der einzige Mensch, der je die 2,40-Meter-Marke im Hochsprung überwunden hat."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Skispringer hat die Vierschanzentournee 2017/2018 gewonnen?",
        answerA = "Richard Freitag",
        answerB = "Markus Eisenbichler",
        answerC = "Andreas Wellinger",
        answerD = "Severin Freund",
        correctAnswer = 2,
        explanation = "Andreas Wellinger gewann die Vierschanzentournee 2017/2018 als erster Deutscher nach Sven Hannawald.",
        difficulty = 3,
        funFact = "Wellinger war beim Sieg erst 22 Jahre alt und galt damals als großes Talent des deutschen Skispringens."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation gewann bei den Leichtathletik-Weltmeisterschaften 2023 in Budapest die meisten Goldmedaillen?",
        answerA = "Kenia",
        answerB = "Jamaika",
        answerC = "USA",
        answerD = "Äthiopien",
        correctAnswer = 2,
        explanation = "Die USA gewannen bei den Weltmeisterschaften 2023 in Budapest die meisten Goldmedaillen in der Leichtathletik.",
        difficulty = 3,
        funFact = "Budapest war damit die erste osteuropäische Stadt, die Leichtathletik-Weltmeisterschaften ausrichtete."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Schwimmdisziplin wird beim Triathlon zusätzlich zu Radfahren und Laufen absolviert?",
        answerA = "Brustschwimmen",
        answerB = "Rückenschwimmen",
        answerC = "Freistilschwimmen",
        answerD = "Lagenschwimmen",
        correctAnswer = 2,
        explanation = "Beim Triathlon wird Freistilschwimmen (Kraulen) eingesetzt, da es der schnellste Schwimmstil ist.",
        difficulty = 3,
        funFact = "Beim Ironman-Triathlon beträgt die Schwimmstrecke 3,86 km, gefolgt von 180,25 km Radfahren und 42,195 km Laufen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Boxer verlor 1990 überraschend seinen WBC-Schwergewichtstitel gegen Buster Douglas?",
        answerA = "Evander Holyfield",
        answerB = "Lennox Lewis",
        answerC = "Mike Tyson",
        answerD = "George Foreman",
        correctAnswer = 2,
        explanation = "Mike Tyson verlor am 11. Februar 1990 überraschend durch K.o. in Runde 10 gegen James 'Buster' Douglas in Tokio.",
        difficulty = 3,
        funFact = "Douglas galt vor dem Kampf mit Quoten von 42:1 als klarer Außenseiter. Es gilt als eine der größten Überraschungen in der Boxgeschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Fußballverein gewann die erste FIFA Klub-Weltmeisterschaft im Jahr 2000?",
        answerA = "Real Madrid",
        answerB = "Corinthians",
        answerC = "Manchester United",
        answerD = "Bayern München",
        correctAnswer = 1,
        explanation = "Corinthians aus Brasilien gewann die erste FIFA Klub-Weltmeisterschaft im Jahr 2000 in Brasilien.",
        difficulty = 3,
        funFact = "Das Turnier 2000 war das erste seiner Art und wurde von der FIFA als offizieller Weltklub-Wettbewerb ausgerichtet."
    ),

    // --- ADDITIONAL EASY (difficulty = 1) ---

    Question(
        categoryId = 6,
        questionText = "Welche Sportart wird mit einem gelben Filzball gespielt?",
        answerA = "Squash",
        answerB = "Badminton",
        answerC = "Tennis",
        answerD = "Tischtennis",
        correctAnswer = 2,
        explanation = "Tennis wird mit einem gelben Filzball gespielt, der einen Durchmesser von 6,35 bis 6,67 cm hat.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Goldmedaillen gibt es bei den Olympischen Spielen pro Disziplin?",
        answerA = "1",
        answerB = "2",
        answerC = "3",
        answerD = "4",
        correctAnswer = 0,
        explanation = "Pro Disziplin wird genau eine Goldmedaille vergeben, außerdem gibt es je eine Silber- und Bronzemedaille.",
        difficulty = 1,
        funFact = "Bei Mannschaftssportarten erhalten alle Mitglieder des Siegerkaders eine Goldmedaille."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Zahl steht auf einem Standarddartboard in der Mitte?",
        answerA = "20",
        answerB = "Bull (kein Zahlenwert)",
        answerC = "1",
        answerD = "10",
        correctAnswer = 1,
        explanation = "Die Mitte eines Dartboards heißt 'Bull' oder 'Bullseye' und ist kein reguläres Zahlensegment, sondern ein eigener Treffbereich.",
        difficulty = 1,
        funFact = "Ein Treffer ins Bullseye zählt 50 Punkte, der äußere Ring (Single Bull) zählt 25 Punkte."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet beim Schwimmen die Abkürzung 'WR'?",
        answerA = "Wettkampfrekord",
        answerB = "Weltrekord",
        answerC = "Wertungsrunde",
        answerD = "Wiederholungsrennen",
        correctAnswer = 1,
        explanation = "WR steht für Weltrekord – die beste je erzielte Zeit auf einer bestimmten Strecke.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Farbe hat die Mittellinie auf einem Fußballfeld?",
        answerA = "Gelb",
        answerB = "Rot",
        answerC = "Weiß",
        answerD = "Blau",
        correctAnswer = 2,
        explanation = "Alle Linien auf einem Fußballfeld sind weiß und 12 cm breit.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Ass' beim Tennis?",
        answerA = "Ein Fehler beim Aufschlag",
        answerB = "Ein Aufschlag, den der Gegner nicht berührt",
        answerC = "Ein Punkt nach einem Netzball",
        answerD = "Ein Sieg nach einem Tiebreak",
        correctAnswer = 1,
        explanation = "Ein Ass (Ace) ist ein Aufschlag, der so gut platziert ist, dass der Gegner ihn nicht berühren kann – direkter Punktgewinn.",
        difficulty = 1,
        funFact = "Der Rekord für die meisten Asses in einer Saison hält John Isner mit über 1.300 Assen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man den Startblock beim Schwimmen?",
        answerA = "Sprungbrett",
        answerB = "Startplattform",
        answerC = "Startblock",
        answerD = "Absprungblock",
        correctAnswer = 2,
        explanation = "Der erhöhte Block am Beckenrand, von dem Schwimmer ins Wasser springen, heißt Startblock.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart wird bei den Olympischen Sommerspielen NICHT ausgetragen?",
        answerA = "Boxen",
        answerB = "Schach",
        answerC = "Gewichtheben",
        answerD = "Ringen",
        correctAnswer = 1,
        explanation = "Schach ist kein olympischer Sport. Es wird zwar vom IOC anerkannt, aber nicht im olympischen Programm geführt.",
        difficulty = 1,
        funFact = "Der Weltschachverband FIDE bewirbt sich seit Jahrzehnten um die olympische Aufnahme."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist beim Fußball ein 'Freistoß'?",
        answerA = "Ein Schuss ohne Torhüter",
        answerB = "Ein Schuss nach einem Regelverstoß des Gegners",
        answerC = "Ein Elfmeter von der Mittellinie",
        answerD = "Ein Schuss aus dem Aus",
        correctAnswer = 1,
        explanation = "Ein Freistoß wird nach einem Foul oder einer Regelübertretung des Gegners ausgeführt und darf ohne Behinderung geschossen werden.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land veranstaltete die Olympischen Sommerspiele 2016?",
        answerA = "USA",
        answerB = "Spanien",
        answerC = "Brasilien",
        answerD = "China",
        correctAnswer = 2,
        explanation = "Die Olympischen Sommerspiele 2016 fanden in Rio de Janeiro, Brasilien statt.",
        difficulty = 1,
        funFact = "Es war das erste Mal, dass Olympia in Südamerika ausgetragen wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist beim Leichtathletik ein 'Fehlstart'?",
        answerA = "Ein Start ohne Startsignal",
        answerB = "Ein Start nach dem Startsignal",
        answerC = "Ein Sturz kurz nach dem Start",
        answerD = "Ein Start mit falschem Schuh",
        correctAnswer = 0,
        explanation = "Ein Fehlstart liegt vor, wenn ein Athlet vor dem offiziellen Startsignal losläuft. Seit 2010 führt der erste Fehlstart zur Disqualifikation.",
        difficulty = 1,
        funFact = "Usain Bolt wurde 2011 bei den Weltmeisterschaften in Daegu wegen eines Fehlstarts im 100m-Finale disqualifiziert."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lange dauert eine Halbzeit im Fußball?",
        answerA = "30 Minuten",
        answerB = "40 Minuten",
        answerC = "45 Minuten",
        answerD = "50 Minuten",
        correctAnswer = 2,
        explanation = "Eine Halbzeit dauert 45 Minuten, hinzu kommt eine vom Schiedsrichter festgelegte Nachspielzeit.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Sprint' in der Leichtathletik?",
        answerA = "Ein Langlauf über 10 km",
        answerB = "Ein Lauf über kurze Distanzen bei maximaler Geschwindigkeit",
        answerC = "Ein Hindernislauf",
        answerD = "Ein Staffellauf über 400 m",
        correctAnswer = 1,
        explanation = "Ein Sprint ist ein Lauf über kurze Distanzen (typischerweise 60m, 100m, 200m) bei maximaler Geschwindigkeit.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Sportart gibt es den Begriff 'Schlagmann' (Batter)?",
        answerA = "Softball/Baseball",
        answerB = "Cricket",
        answerC = "Baseball",
        answerD = "Alle drei",
        correctAnswer = 3,
        explanation = "Den Begriff 'Batter' oder 'Schlagmann' gibt es in Baseball, Softball und Cricket.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Farbe hat das Trikot des Gesamtführenden beim Giro d'Italia?",
        answerA = "Gelb",
        answerB = "Rot",
        answerC = "Rosa",
        answerD = "Blau",
        correctAnswer = 2,
        explanation = "Der Führende beim Giro d'Italia trägt das Maglia Rosa – das Rosa Trikot.",
        difficulty = 1,
        funFact = "Die Farbe Rosa wurde gewählt, weil die organisierende Zeitung 'La Gazzetta dello Sport' auf rosa Papier gedruckt wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist beim Fußball ein 'Eckball'?",
        answerA = "Ein Ball, der das Feld in der Ecke verlässt",
        answerB = "Ein Einwurf von der Eckfahne",
        answerC = "Ein Schuss aus der Ecke des Strafraums",
        answerD = "Ein Freistoß aus der Torecke",
        correctAnswer = 0,
        explanation = "Ein Eckball wird ausgeführt, wenn der Ball die Torlinie überquert hat und zuletzt von einem Verteidiger berührt wurde.",
        difficulty = 1,
        funFact = "Ein direkt verwandelter Eckball heißt 'Olympiator' oder 'Tor direkt aus der Ecke'."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man die olympischen Spiele im Winter?",
        answerA = "Winterolympiade",
        answerB = "Kalte Spiele",
        answerC = "Olympische Winterspiele",
        answerD = "Schnee-Olympia",
        correctAnswer = 2,
        explanation = "Die Olympischen Winterspiele sind der offizielle Name der Multisportveranstaltung für Wintersportarten.",
        difficulty = 1,
        funFact = "Die Olympischen Winterspiele finden seit 1994 im Wechsel mit den Sommerspielen alle zwei Jahre statt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart wird in einem Pool mit Bahnen ausgeübt?",
        answerA = "Wasserball",
        answerB = "Synchronschwimmen",
        answerC = "Schwimmen",
        answerD = "Tauchen",
        correctAnswer = 2,
        explanation = "Wettkampfschwimmen findet in einem 50-Meter-Becken mit Bahnen statt, die durch Schwimmbahnen getrennt sind.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet beim Fußball ein 'Elfmeter'?",
        answerA = "Ein Freistoß aus elf Metern Entfernung",
        answerB = "Ein Schuss vom Elfmeterpunkt, der 11 Meter vor dem Tor liegt",
        answerC = "Ein Tor, das aus elf Metern erzielt wurde",
        answerD = "Ein Schuss nach einer Elfmeter-Prüfung",
        correctAnswer = 1,
        explanation = "Der Elfmeterpunkt liegt 11 Meter vor der Torlinie. Ein Elfmeter wird nach einem Foul im Strafraum verhängt.",
        difficulty = 1,
        funFact = "Bei einem Elfmeter liegt die Torschusswahrscheinlichkeit bei etwa 75-80 Prozent."
    ),

    Question(
        categoryId = 6,
        questionText = "Welchen Sport betreibt man, wenn man auf einem Surfbrett über Wellen gleitet?",
        answerA = "Kitesurfen",
        answerB = "Wellenreiten (Surfen)",
        answerC = "Wasserski",
        answerD = "Wakeboarden",
        correctAnswer = 1,
        explanation = "Wellenreiten oder Surfen ist der Sport, bei dem man auf einem Brett die Energie von Meereswellen nutzt.",
        difficulty = 1,
        funFact = "Surfen wurde 2020 erstmals bei den Olympischen Spielen in Tokio als olympischer Sport ausgetragen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist beim Tennis ein 'Doppel'?",
        answerA = "Ein Spiel mit zwei Bällen",
        answerB = "Ein Spiel mit zwei Spielern pro Seite",
        answerC = "Ein Satz mit doppelter Länge",
        answerD = "Ein Punkt nach zwei Netzberührungen",
        correctAnswer = 1,
        explanation = "Beim Tennis-Doppel spielen je zwei Spieler pro Seite. Es gibt Herrendoppel, Damendoppel und Mixed (gemischtes Doppel).",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land hat die Formel 1 erfunden?",
        answerA = "Deutschland",
        answerB = "Frankreich",
        answerC = "Großbritannien",
        answerD = "Italien",
        correctAnswer = 2,
        explanation = "Die Formel 1 wurde maßgeblich in Großbritannien entwickelt. Die erste Formel-1-Weltmeisterschaft fand 1950 statt, mit dem ersten Rennen in Silverstone, England.",
        difficulty = 1,
        funFact = "Das erste Formel-1-Rennen der Weltmeisterschaft 1950 in Silverstone gewann Giuseppe Farina aus Italien."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man den Bereich über dem Torpfosten, in dem der Ball ins Netz muss?",
        answerA = "Winkel",
        answerB = "Kreuzeck",
        answerC = "Torecke",
        answerD = "Oberes Dreieck",
        correctAnswer = 1,
        explanation = "Das Kreuzeck ist der Bereich, wo Torpfosten und Querlatte sich treffen – ein Treffer dort ist besonders schwierig und spektakulär.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler stehen bei einem Rugbyspiel (Rugby League) auf dem Feld?",
        answerA = "11",
        answerB = "13",
        answerC = "15",
        answerD = "17",
        correctAnswer = 1,
        explanation = "Beim Rugby League stellt jede Mannschaft 13 Spieler auf dem Feld auf.",
        difficulty = 1,
        funFact = "Rugby League und Rugby Union sind zwei verschiedene Varianten des Rugbysports mit unterschiedlichen Regeln."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist bei der Leichtathletik der Unterschied zwischen Kugelstoßen und Diskuswerfen?",
        answerA = "Die Größe des Athleten",
        answerB = "Das Gerät: Kugel (rund/schwer) vs. Diskus (flach/rund)",
        answerC = "Die Wurfrichtung",
        answerD = "Kein Unterschied",
        correctAnswer = 1,
        explanation = "Beim Kugelstoßen wird eine schwere Metallkugel (Männer: 7,26 kg) gestoßen, beim Diskuswerfen eine flache Scheibe (Männer: 2 kg) geworfen.",
        difficulty = 1,
        funFact = "Kugelstoßen gehört seit den ersten modernen Olympischen Spielen 1896 zum olympischen Programm."
    ),

    // --- ADDITIONAL MEDIUM (difficulty = 2) ---

    Question(
        categoryId = 6,
        questionText = "Welche Nation hat die Fußball-EM (UEFA Europameisterschaft) am häufigsten gewonnen?",
        answerA = "Frankreich",
        answerB = "Deutschland",
        answerC = "Spanien",
        answerD = "Italien",
        correctAnswer = 2,
        explanation = "Spanien hat die UEFA Europameisterschaft dreimal gewonnen (1964, 2008, 2012) – mehr als jede andere Nation.",
        difficulty = 2,
        funFact = "Spaniens EM-Sieg 2008 war der Beginn einer Ära: Das Team gewann danach auch WM 2010 und EM 2012 – insgesamt drei Titel in Folge."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fand die erste Formel-1-Weltmeisterschaft statt?",
        answerA = "1948",
        answerB = "1950",
        answerC = "1952",
        answerD = "1955",
        correctAnswer = 1,
        explanation = "Die erste Formel-1-Weltmeisterschaft fand 1950 statt. Giuseppe Farina wurde der erste F1-Weltmeister der Geschichte.",
        difficulty = 2,
        funFact = "Der erste F1-Weltmeister Giuseppe Farina war Italiener und fuhr für Alfa Romeo."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmer hält den Weltrekord über 100m Freistil der Männer (Stand 2024)?",
        answerA = "Caeleb Dressel",
        answerB = "Kyle Chalmers",
        answerC = "David Popovici",
        answerD = "Pan Zhanle",
        correctAnswer = 3,
        explanation = "Pan Zhanle (China) stellte 2024 in Paris bei den Olympischen Spielen mit 46,40 Sekunden den Weltrekord über 100m Freistil auf.",
        difficulty = 2,
        funFact = "Pan Zhanle verbesserte seinen eigenen Weltrekord im Olympia-Finale und war der erste Mensch unter 46,50 Sekunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heißt das Fußball-Turnier, das alle vier Jahre zwischen europäischen Nationalmannschaften ausgetragen wird?",
        answerA = "Euro Cup",
        answerB = "UEFA Europameisterschaft",
        answerC = "Nations League",
        answerD = "Continental Cup",
        correctAnswer = 1,
        explanation = "Die UEFA Europameisterschaft (EURO) findet alle vier Jahre statt und ist das wichtigste Turnier für europäische Nationalmannschaften.",
        difficulty = 2,
        funFact = "Deutschland hat die Europameisterschaft dreimal gewonnen: 1972, 1980 und 1996."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Leichtathlet gewann bei den Olympischen Spielen 2024 in Paris Gold über 100m der Männer?",
        answerA = "Marcell Jacobs",
        answerB = "Noah Lyles",
        answerC = "Fred Kerley",
        answerD = "Christian Coleman",
        correctAnswer = 1,
        explanation = "Noah Lyles (USA) gewann bei den Olympischen Spielen 2024 in Paris die Goldmedaille über 100m der Männer mit 9,79 Sekunden.",
        difficulty = 2,
        funFact = "Lyles gewann mit einem Vorsprung von nur 5 Tausendstel Sekunden vor Kishane Thompson – einer der knappsten 100m-Olympiaentscheidungen der Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer gewann die Formel-1-Weltmeisterschaft 2022?",
        answerA = "Lewis Hamilton",
        answerB = "Charles Leclerc",
        answerC = "Max Verstappen",
        answerD = "George Russell",
        correctAnswer = 2,
        explanation = "Max Verstappen gewann die Formel-1-Weltmeisterschaft 2022 und holte damit seinen zweiten Titel in Folge.",
        difficulty = 2,
        funFact = "Verstappen gewann 2022 insgesamt 15 von 22 Rennen – ein Rekord in einer einzigen Saison."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land richtete die Olympischen Sommerspiele 2024 aus?",
        answerA = "USA",
        answerB = "Australien",
        answerC = "Frankreich",
        answerD = "Japan",
        correctAnswer = 2,
        explanation = "Die Olympischen Sommerspiele 2024 fanden in Paris, Frankreich statt.",
        difficulty = 2,
        funFact = "Paris richtete die Olympischen Spiele bereits 1900 und 1924 aus. 2024 war die dritte Austragung."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Tore erzielte Ronaldo (Cristiano) bei der Fußball-WM 2022 in Katar?",
        answerA = "0",
        answerB = "1",
        answerC = "2",
        answerD = "3",
        correctAnswer = 1,
        explanation = "Cristiano Ronaldo erzielte bei der WM 2022 in Katar 1 Tor – einen Elfmeter gegen Ghana. Portugal schied im Viertelfinale aus.",
        difficulty = 2,
        funFact = "Ronaldo ist der einzige Spieler, der bei fünf verschiedenen Fußball-Weltmeisterschaften mindestens ein Tor erzielt hat."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Strecke gilt als 'der Klassiker' im Radsport und führt von Mailand nach Sanremo?",
        answerA = "Paris–Roubaix",
        answerB = "Mailand–Sanremo",
        answerC = "Lüttich–Bastogne–Lüttich",
        answerD = "Flandern-Rundfahrt",
        correctAnswer = 1,
        explanation = "Mailand–Sanremo ist das längste Eintagesrennen im Radsport (ca. 290 km) und einer der fünf 'Monuments' des Radsports.",
        difficulty = 2,
        funFact = "Mailand–Sanremo wird wegen seiner Länge und der Dominanz von Sprintern oft als 'La Classicissima' bezeichnet."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Disziplin beendet den Triathlon?",
        answerA = "Schwimmen",
        answerB = "Radfahren",
        answerC = "Laufen",
        answerD = "Sprinten",
        correctAnswer = 2,
        explanation = "Die Reihenfolge beim Triathlon ist immer: Schwimmen → Radfahren → Laufen. Gelaufen wird zuletzt.",
        difficulty = 2,
        funFact = "Beim olympischen Triathlon sind die Distanzen: 1,5 km Schwimmen, 40 km Radfahren, 10 km Laufen."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welcher Fußball-WM spielte Deutschland erstmals unter dem Namen 'Deutschland' statt 'Westdeutschland'?",
        answerA = "1990",
        answerB = "1994",
        answerC = "1998",
        answerD = "2002",
        correctAnswer = 1,
        explanation = "Nach der Wiedervereinigung 1990 spielte Deutschland bei der WM 1994 erstmals als gesamtdeutsche Mannschaft unter dem Namen 'Deutschland'.",
        difficulty = 2,
        funFact = "Bei der WM 1990 gewann noch 'Westdeutschland' den Titel – kurz bevor die Wiedervereinigung vollzogen wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Sprinter hält den Weltrekord über 200m der Männer?",
        answerA = "Yohan Blake",
        answerB = "Michael Johnson",
        answerC = "Usain Bolt",
        answerD = "Frank Fredericks",
        correctAnswer = 2,
        explanation = "Usain Bolt hält mit 19,19 Sekunden auch den Weltrekord über 200m, aufgestellt bei den WM 2009 in Berlin.",
        difficulty = 2,
        funFact = "Bolt stellte beide Weltrekorde (100m und 200m) bei denselben Weltmeisterschaften 2009 in Berlin auf."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr gewann Frankreich zum ersten Mal die Fußball-Weltmeisterschaft?",
        answerA = "1990",
        answerB = "1994",
        answerC = "1998",
        answerD = "2002",
        correctAnswer = 2,
        explanation = "Frankreich gewann die FIFA Weltmeisterschaft 1998 im eigenen Land durch ein 3:0 gegen Brasilien im Finale.",
        difficulty = 2,
        funFact = "Zinedine Zidane erzielte im WM-Finale 1998 zwei Kopfballtore und wurde zum Helden des Landes."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann die Fußball-EM 2024?",
        answerA = "England",
        answerB = "Deutschland",
        answerC = "Frankreich",
        answerD = "Spanien",
        correctAnswer = 3,
        explanation = "Spanien gewann die UEFA Europameisterschaft 2024 in Deutschland durch einen 2:1-Sieg gegen England im Finale.",
        difficulty = 2,
        funFact = "Spanien ist damit Rekordeuropameister mit vier EM-Titeln (1964, 2008, 2012, 2024)."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmer gewann bei den Olympischen Spielen 2024 in Paris die meisten Goldmedaillen?",
        answerA = "Caeleb Dressel",
        answerB = "Leon Marchand",
        answerC = "Adam Peaty",
        answerD = "Florent Manaudou",
        correctAnswer = 1,
        explanation = "Leon Marchand (Frankreich) gewann bei den Olympischen Spielen 2024 in Paris vier Einzelgoldmedaillen und wurde zum Star des Heimturniers.",
        difficulty = 2,
        funFact = "Marchand gewann Gold über 200m Schmetterling, 200m Brust, 400m Lagen und 200m Lagen – eine herausragende Leistung."
    ),

    // --- ADDITIONAL HARD (difficulty = 3) ---

    Question(
        categoryId = 6,
        questionText = "Welcher F1-Fahrer gewann mit Ferrari zuletzt die Konstrukteurswertung vor der Mercedes-Dominanz?",
        answerA = "Fernando Alonso",
        answerB = "Kimi Räikkönen",
        answerC = "Michael Schumacher",
        answerD = "Felipe Massa",
        correctAnswer = 0,
        explanation = "Fernando Alonso und Ferrari gewannen zuletzt 2008 die Konstrukteurswertung, bevor Mercedes ab 2014 die Ära dominierte.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann bei den Olympischen Sommerspielen 2024 in Paris die Nationenwertung nach Goldmedaillen?",
        answerA = "China",
        answerB = "Australien",
        answerC = "USA",
        answerD = "Großbritannien",
        correctAnswer = 0,
        explanation = "China und die USA lagen nach Goldmedaillen bei den Olympischen Spielen 2024 in Paris gleichauf mit je 40 Goldmedaillen. Nach der ITF-Wertung führten die USA.",
        difficulty = 3,
        funFact = "Frankreich überraschte als Gastgeberland und landete in der Nationenwertung auf einem der vorderen Plätze."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler gewann 2024 die French Open?",
        answerA = "Novak Djokovic",
        answerB = "Carlos Alcaraz",
        answerC = "Rafael Nadal",
        answerD = "Jannik Sinner",
        correctAnswer = 1,
        explanation = "Carlos Alcaraz gewann 2024 die French Open durch einen Sieg gegen Alexander Zverev im Finale.",
        difficulty = 3,
        funFact = "Alcaraz ist einer der wenigen Spieler, der sowohl French Open als auch Wimbledon in aufeinanderfolgenden Jahren gewinnen konnte."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fand die erste Schwimm-Weltmeisterschaft statt?",
        answerA = "1969",
        answerB = "1973",
        answerC = "1978",
        answerD = "1982",
        correctAnswer = 1,
        explanation = "Die ersten Schwimm-Weltmeisterschaften wurden 1973 in Belgrad, Jugoslawien ausgetragen.",
        difficulty = 3,
        funFact = "Deutschland gehörte bei den ersten Weltmeisterschaften zu den stärksten Nationen im Schwimmen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Leichtathlet gewann bei den Olympischen Spielen 2021 in Tokio Gold im 400m-Hürdenlauf mit Weltrekord?",
        answerA = "Karsten Warholm",
        answerB = "Rai Benjamin",
        answerC = "Alison dos Santos",
        answerD = "Kevin Young",
        correctAnswer = 0,
        explanation = "Karsten Warholm (Norwegen) stellte 2021 in Tokio mit 45,94 Sekunden einen Weltrekord im 400m-Hürdenlauf auf und gewann Gold.",
        difficulty = 3,
        funFact = "Warholm verbesserte den damaligen Weltrekord um fast ganze Sekunde – eine Leistung, die als eine der größten in der Leichtathletikgeschichte gilt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Formel-1-Fahrer gewann seinen ersten WM-Titel 2005 mit Renault?",
        answerA = "Michael Schumacher",
        answerB = "Jenson Button",
        answerC = "Fernando Alonso",
        answerD = "Kimi Räikkönen",
        correctAnswer = 2,
        explanation = "Fernando Alonso gewann 2005 mit Renault seinen ersten Formel-1-Weltmeistertitel und wurde der jüngste Weltmeister bis dahin.",
        difficulty = 3,
        funFact = "Alonso gewann 2005 und 2006 jeweils die Weltmeisterschaft – Max Verstappen brach später Alonsos Altersrekord."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Schwimmerin gewann bei den Olympischen Spielen 2024 in Paris die meisten Medaillen?",
        answerA = "Katie Ledecky",
        answerB = "Summer McIntosh",
        answerC = "Kaylee McKeown",
        answerD = "Sarah Sjöström",
        correctAnswer = 0,
        explanation = "Katie Ledecky (USA) gewann bei den Olympischen Spielen 2024 in Paris mehrere Medaillen und festigte ihren Status als größte Schwimmerin der Geschichte.",
        difficulty = 3,
        funFact = "Ledecky hat seit 2012 bei jedem Olympia mindestens eine Goldmedaille gewonnen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wann fand das berühmte 'Wembley-Tor' bei der WM 1966 statt, bei dem bis heute gestritten wird, ob der Ball hinter der Linie war?",
        answerA = "Im Viertelfinale",
        answerB = "Im Halbfinale",
        answerC = "Im Finale",
        answerD = "In der Gruppenphase",
        correctAnswer = 2,
        explanation = "Das 'Wembley-Tor' fiel im WM-Finale 1966 zwischen England und Deutschland. Geoff Hursts Schuss in der Verlängerung wurde als Tor gewertet – England gewann 4:2.",
        difficulty = 3,
        funFact = "Noch heute ist nicht endgültig geklärt, ob der Ball vollständig die Torlinie überquert hatte. Deutschland verlor das Finale mit 2:4."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Leichtathlet hält den Weltrekord im Dreisprung der Männer?",
        answerA = "Christian Taylor",
        answerB = "Jonathan Edwards",
        answerC = "Pedro Pablo Pichardo",
        answerD = "Hugues Fabrice Zango",
        correctAnswer = 1,
        explanation = "Jonathan Edwards (Großbritannien) hält mit 18,29 Metern seit dem 7. August 1995 den Weltrekord im Dreisprung.",
        difficulty = 3,
        funFact = "Edwards stellte den Weltrekord bei den Weltmeisterschaften 1995 in Göteborg auf und war der erste Mensch über der 18-Meter-Marke."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler hält den Rekord für die meisten Wimbledon-Herreneinzel-Titel?",
        answerA = "Pete Sampras",
        answerB = "Björn Borg",
        answerC = "Roger Federer",
        answerD = "Novak Djokovic",
        correctAnswer = 2,
        explanation = "Roger Federer gewann Wimbledon achtmal (2003–2012 mit einer Unterbrechung) und hält damit den Rekord bei den Herren.",
        difficulty = 3,
        funFact = "Federer gewann zwischen 2003 und 2007 fünf Wimbledon-Titel in Folge."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation gewann beim Formel-1-Konstrukteursweltmeisterschaft 2023?",
        answerA = "Ferrari",
        answerB = "Mercedes",
        answerC = "Red Bull Racing",
        answerD = "Aston Martin",
        correctAnswer = 2,
        explanation = "Red Bull Racing gewann 2023 die Konstrukteursweltmeisterschaft mit großem Abstand – Max Verstappen holte den Fahrertitel.",
        difficulty = 3,
        funFact = "Red Bull gewann 2023 alle außer einem Rennen – eine der dominantesten Saisons in der F1-Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Leichtathlet gewann 1996 in Atlanta olympisches Gold im Weitsprung?",
        answerA = "Heike Drechsler",
        answerB = "Lars Riedel",
        answerC = "Dieter Baumann",
        answerD = "Heike Drechsler gewann, aber im Frauenwettbewerb",
        correctAnswer = 3,
        explanation = "Heike Drechsler gewann 1996 in Atlanta die Goldmedaille im Weitsprung der Frauen. Im Männer-Weitsprung gewann Carl Lewis.",
        difficulty = 3,
        funFact = "Heike Drechsler gewann auch 1992 in Barcelona Olympia-Gold im Weitsprung und war die Dominanzsportlerin dieser Disziplin."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler gewann als erster Spieler in der Open Era alle vier Grand Slams auf unterschiedlichem Untergrund?",
        answerA = "Andre Agassi",
        answerB = "Rafael Nadal",
        answerC = "Roger Federer",
        answerD = "Novak Djokovic",
        correctAnswer = 0,
        explanation = "Andre Agassi gewann als erster Spieler alle vier Grand Slams (auf Hartplatz, Sand und Rasen) und vervollständigte den Karriere-Grand-Slam.",
        difficulty = 3,
        funFact = "Agassi gewann auch olympisches Gold 1996 in Atlanta und wurde damit der einzige Spieler mit einem 'Golden Slam' zu diesem Zeitpunkt."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Spielen wurde Doping durch staatliches russisches System systematisch betrieben und enthüllt?",
        answerA = "Peking 2008",
        answerB = "London 2012",
        answerC = "Sotschi 2014",
        answerD = "Rio 2016",
        correctAnswer = 2,
        explanation = "Bei den Olympischen Winterspielen 2014 in Sotschi betrieb Russland nach späteren Ermittlungen ein staatliches Dopingprogramm, das durch den McLaren-Report enthüllt wurde.",
        difficulty = 3,
        funFact = "Als Konsequenz traten russische Athleten bei späteren Olympischen Spielen unter neutraler Flagge als 'ROC' oder 'AIN' an."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Sprinter lief 2019 als zweiter Mensch die 100m unter 9,60 Sekunden?",
        answerA = "Tyson Gay",
        answerB = "Christian Coleman",
        answerC = "Yohan Blake",
        answerD = "Kemar Bailey-Cole",
        correctAnswer = 2,
        explanation = "Yohan Blake lief 2012 in Lausanne 9,69 Sekunden und ist neben Usain Bolt der einzige Mensch, der die 100m je unter 9,70 Sekunden gelaufen ist.",
        difficulty = 3,
        funFact = "Blake trainierte jahrelang gemeinsam mit Usain Bolt und war sein schärfster Trainingspartner."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Formel-1-Fahrer verstarb bei einem Trainingsunfall in Hockenheim im Jahr 2014?",
        answerA = "Jules Bianchi",
        answerB = "Carlos Reutemann",
        answerC = "Dan Wheldon",
        answerD = "Maria de Villota",
        correctAnswer = 0,
        explanation = "Jules Bianchi verunglückte beim Großen Preis von Japan 2014 in Suzuka schwer und verstarb im Juli 2015 an seinen Verletzungen.",
        difficulty = 3,
        funFact = "Bianchi war der erste F1-Fahrer, der bei einem WM-Lauf seit Ayrton Senna 1994 tödlich verunglückte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Hochspringer teilte sich 2021 bei den Olympischen Spielen in Tokio die Goldmedaille mit Mutaz Essa Barshim?",
        answerA = "Maksim Nedasekau",
        answerB = "Ilya Ivanyuk",
        answerC = "Gianmarco Tamberi",
        answerD = "Mikhail Akimenko",
        correctAnswer = 2,
        explanation = "Gianmarco Tamberi (Italien) und Mutaz Essa Barshim (Katar) teilten sich einvernehmlich die Goldmedaille im Hochsprung bei den Olympischen Spielen 2021 in Tokio.",
        difficulty = 3,
        funFact = "Die gemeinsame Goldmedaille war eine seltene Situation – beide hatten die gleiche Höhe übersprungen und keiner konnte die nächste Höhe bewältigen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler gewann 2024 bei den Olympischen Spielen in Paris die Goldmedaille im Einzel?",
        answerA = "Carlos Alcaraz",
        answerB = "Novak Djokovic",
        answerC = "Rafael Nadal",
        answerD = "Jannik Sinner",
        correctAnswer = 1,
        explanation = "Novak Djokovic gewann 2024 in Paris olympisches Gold im Tennis-Einzel – der einzige fehlende Titel in seiner Karriere.",
        difficulty = 3,
        funFact = "Djokovic besiegte im Finale Carlos Alcaraz und holte sich damit den letzten großen Titel, der ihm in seiner Karriere noch gefehlt hatte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmer stellte 2024 in Paris den Weltrekord über 200m Brust auf?",
        answerA = "Adam Peaty",
        answerB = "Qin Haiyang",
        answerC = "Leon Marchand",
        answerD = "Nicolo Martinenghi",
        correctAnswer = 2,
        explanation = "Leon Marchand stellte bei den Olympischen Spielen 2024 in Paris Weltrekorde auf und dominierte die Lagen- und Schmetterlingstrecken.",
        difficulty = 3,
        funFact = "Marchand wurde zum Gesicht der Olympischen Spiele 2024 in Paris und als Nachfolger Michael Phelps' gehandelt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Leichtathlet hält den Weltrekord im 1500m-Lauf der Männer?",
        answerA = "Hicham El Guerrouj",
        answerB = "Yomif Kejelcha",
        answerC = "Jakob Ingebrigtsen",
        answerD = "Faith Kipyegon",
        correctAnswer = 0,
        explanation = "Hicham El Guerrouj (Marokko) hält seit 1998 mit 3:26,00 Minuten den Weltrekord über 1500m der Männer.",
        difficulty = 3,
        funFact = "El Guerrouj hält auch den Weltrekord über die Meile (3:43,13 min) und war einer der dominantesten Mittelstreckler der Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation gewann bei der Fußball-WM 2010 in Südafrika den Titel?",
        answerA = "Brasilien",
        answerB = "Deutschland",
        answerC = "Niederlande",
        answerD = "Spanien",
        correctAnswer = 3,
        explanation = "Spanien gewann die Fußball-Weltmeisterschaft 2010 in Südafrika durch einen 1:0-Sieg nach Verlängerung gegen die Niederlande.",
        difficulty = 3,
        funFact = "Andrés Iniesta erzielte das entscheidende Tor in der Verlängerung des WM-Finals 2010."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler gewann 2023 Wimbledon und beendete damit Carlos Alcaraz' kurze Herrschaft?",
        answerA = "Carlos Alcaraz",
        answerB = "Novak Djokovic",
        answerC = "Holger Rune",
        answerD = "Daniil Medvedev",
        correctAnswer = 0,
        explanation = "Carlos Alcaraz gewann Wimbledon 2023 gegen Novak Djokovic im Finale – es war sein zweiter Grand-Slam-Titel.",
        difficulty = 3,
        funFact = "Alcaraz war der erste Spieler seit Rafael Nadal, der Djokovic auf dem Wimbledon-Rasen besiegen konnte."
    )
)
