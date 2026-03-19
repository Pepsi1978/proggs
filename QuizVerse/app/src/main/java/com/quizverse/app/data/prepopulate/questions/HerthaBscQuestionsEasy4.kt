package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun herthaBscQuestionsEasy4(): List<Question> = listOf(

    // ── TOPICS 1-10: Hertha BSC und die Stadt Berlin ──────────────────────────

    // Q1 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "In welchem Berliner Bezirk liegt das Olympiastadion, Herthas Heimstätte?",
        answerA = "Mitte",
        answerB = "Charlottenburg-Wilmersdorf",
        answerC = "Spandau",
        answerD = "Steglitz-Zehlendorf",
        correctAnswer = 1,
        explanation = "Das Olympiastadion befindet sich im Berliner Bezirk Charlottenburg-Wilmersdorf im Westen der Stadt.",
        difficulty = 1,
        funFact = "Das Stadiongelände trägt den Namen Olympiapark Berlin und umfasst neben dem Fußballstadion auch eine Schwimmhalle und eine Waldbühne."
    ),

    // Q2 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Welcher Fluss fließt durch Berlin, in dessen Nähe Hertha BSC beheimatet ist?",
        answerA = "Rhein",
        answerB = "Elbe",
        answerC = "Oder",
        answerD = "Spree",
        correctAnswer = 3,
        explanation = "Die Spree ist der Hauptfluss Berlins und prägt das Stadtbild der Hauptstadt, in der Hertha BSC zuhause ist.",
        difficulty = 1,
        funFact = "Der Vereinsname 'Hertha' geht auf ein Dampfschiff namens 'Hertha' zurück, das die Gründer des Klubs auf der Spree sahen."
    ),

    // Q3 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Wie wird Hertha BSC in Berlin liebevoll genannt?",
        answerA = "Die alte Dame",
        answerB = "Die junge Dame",
        answerC = "Die blaue Dame",
        answerD = "Die stolze Dame",
        correctAnswer = 0,
        explanation = "Hertha BSC wird von den Fans liebevoll 'die alte Dame' genannt – ein Ausdruck der Zuneigung für den traditionsreichen Berliner Klub.",
        difficulty = 1,
        funFact = "Juventus Turin aus Italien trägt denselben Spitznamen 'die alte Dame' – ein Zufall, der manchmal für Verwirrung sorgt."
    ),

    // Q4 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Welches Berliner Wahrzeichen steht in unmittelbarer Nähe des Olympiastadions?",
        answerA = "Brandenburger Tor",
        answerB = "Fernsehturm",
        answerC = "Glockenturm am Olympiagelände",
        answerD = "Berliner Dom",
        correctAnswer = 2,
        explanation = "Der Glockenturm am Olympiagelände steht direkt neben dem Olympiastadion und ist Teil des historischen Geländes von 1936.",
        difficulty = 1,
        funFact = "Vom Glockenturm aus hat man einen fantastischen Blick über das gesamte Olympiagelände und Teile Berlins."
    ),

    // Q5 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "In welchem Jahr wurde Hertha BSC gegründet?",
        answerA = "1900",
        answerB = "1910",
        answerC = "1885",
        answerD = "1892",
        correctAnswer = 3,
        explanation = "Hertha BSC wurde am 25. Juli 1892 in Berlin gegründet und gehört damit zu den ältesten Fußballvereinen Deutschlands.",
        difficulty = 1,
        funFact = "Bei der Gründung hieß der Verein noch BFC Hertha 92 – die Fusion mit dem Berliner Sport-Club erfolgte erst später."
    ),

    // Q6 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Welche U-Bahn-Linie fährt direkt zum Olympiastadion in Berlin?",
        answerA = "U2",
        answerB = "U1",
        answerC = "U5",
        answerD = "U9",
        correctAnswer = 0,
        explanation = "Die U-Bahn-Linie U2 hält an der Station Olympia-Stadion (Ost) und bringt Fans direkt zum Heimstadion von Hertha BSC.",
        difficulty = 1,
        funFact = "An Spieltagen fahren die Berliner Verkehrsbetriebe BVG häufig Sonderzüge, um den Andrang der Fans zu bewältigen."
    ),

    // Q7 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Für welches große Sportereignis wurde das Olympiastadion Berlin ursprünglich gebaut?",
        answerA = "Fußball-WM 1954",
        answerB = "Europameisterschaft 1988",
        answerC = "Weltmeisterschaft 1974",
        answerD = "Olympische Sommerspiele 1936",
        correctAnswer = 3,
        explanation = "Das Berliner Olympiastadion wurde für die Olympischen Sommerspiele 1936 errichtet und fasst heute über 74.000 Zuschauer.",
        difficulty = 1,
        funFact = "Im Jahr 2006 fand im Olympiastadion Berlin das Finale der FIFA Fußball-Weltmeisterschaft statt."
    ),

    // Q8 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Welches Berliner Stadtquartier ist als inoffizielles 'Hertha-Viertel' bekannt?",
        answerA = "Prenzlauer Berg",
        answerB = "Kreuzberg",
        answerC = "Westend",
        answerD = "Neukölln",
        correctAnswer = 2,
        explanation = "Das Westend in Charlottenburg, direkt rund ums Olympiastadion, gilt als das traditionelle Heimviertel von Hertha BSC.",
        difficulty = 1,
        funFact = "Viele Fankneipen und Merchandise-Shops rund um das Olympiastadion prägen das Bild des Westends an Spieltagen."
    ),

    // Q9 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Welcher bekannte Berliner See liegt im Westen der Stadt, nicht weit vom Olympiastadion entfernt?",
        answerA = "Wannsee",
        answerB = "Tegeler See",
        answerC = "Müggelsee",
        answerD = "Schlachtensee",
        correctAnswer = 0,
        explanation = "Der Wannsee liegt im Westen Berlins, nicht weit vom Olympiastadion entfernt, und ist ein beliebtes Ausflugsziel der Berliner.",
        difficulty = 1,
        funFact = "Viele Hertha-Fans verbinden einen Stadionbesuch gerne mit einem Ausflug an den Wannsee, besonders im Sommer."
    ),

    // Q10 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "Wie heißt der offizielle Vereinssong von Hertha BSC?",
        answerA = "Wir sind Hertha",
        answerB = "Blau-Weiß, wie lieb ich dich",
        answerC = "Nur nach Hause",
        answerD = "Alte Dame Berlin",
        correctAnswer = 1,
        explanation = "'Blau-Weiß, wie lieb ich dich' ist die offizielle Vereinshymne von Hertha BSC und wird im Olympiastadion gesungen.",
        difficulty = 1,
        funFact = "Die Hymne wird vor Heimspielen gespielt und sorgt regelmäßig für Gänsehaut bei Fans und Spielern gleichermaßen."
    ),

    // ── TOPICS 11-20: Grundlegende Bundesliga-Fakten rund um Hertha ──────────

    // Q11 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "In welcher Liga spielt Hertha BSC in der Saison 2023/24?",
        answerA = "Bundesliga",
        answerB = "3. Liga",
        answerC = "Regionalliga Nordost",
        answerD = "2. Bundesliga",
        correctAnswer = 3,
        explanation = "Hertha BSC spielt nach dem Abstieg aus der Bundesliga in der Saison 2023/24 in der 2. Bundesliga.",
        difficulty = 1,
        funFact = "Hertha BSC ist einer der Gründungsvereine der Bundesliga im Jahr 1963 und gehörte lange zur Bundesliga-Stammbesetzung."
    ),

    // Q12 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Wann wurde die Bundesliga in Deutschland gegründet?",
        answerA = "1955",
        answerB = "1970",
        answerC = "1963",
        answerD = "1949",
        correctAnswer = 2,
        explanation = "Die Fußball-Bundesliga wurde 1963 gegründet und nahm mit 16 Vereinen den Spielbetrieb auf – darunter auch Hertha BSC.",
        difficulty = 1,
        funFact = "Das erste Bundesligaspiel fand am 24. August 1963 statt. Werder Bremen gewann gegen Westfalia Herne mit 3:2."
    ),

    // Q13 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Wie viele Mannschaften spielen in einer Bundesliga-Saison?",
        answerA = "18",
        answerB = "16",
        answerC = "20",
        answerD = "22",
        correctAnswer = 0,
        explanation = "Die Bundesliga besteht aus 18 Vereinen, die in einer Hin- und Rückrunde gegeneinander spielen.",
        difficulty = 1,
        funFact = "Am Ende einer Saison steigen die letzten zwei Teams direkt ab, das drittletzte Team spielt Relegation gegen den Drittligisten."
    ),

    // Q14 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Was passiert mit den letzten zwei Vereinen am Ende einer Bundesliga-Saison?",
        answerA = "Sie spielen eine Relegation",
        answerB = "Sie werden aufgelöst",
        answerC = "Sie bleiben in der Bundesliga",
        answerD = "Sie steigen direkt in die 2. Bundesliga ab",
        correctAnswer = 3,
        explanation = "Die beiden letzten Vereine der Bundesliga steigen direkt in die 2. Bundesliga ab, ohne Relegation spielen zu müssen.",
        difficulty = 1,
        funFact = "Hertha BSC musste in seiner Geschichte mehrfach den direkten Abstieg in die 2. Bundesliga hinnehmen."
    ),

    // Q15 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Welchen Platz muss ein Verein in der 2. Bundesliga erreichen, um direkt aufzusteigen?",
        answerA = "Platz 1, 2 oder 3",
        answerB = "Nur Platz 1",
        answerC = "Platz 1 oder 2",
        answerD = "Platz 1, 2, 3 oder 4",
        correctAnswer = 2,
        explanation = "Die ersten beiden Vereine der 2. Bundesliga steigen direkt in die Bundesliga auf. Der Dritte spielt Relegation.",
        difficulty = 1,
        funFact = "Hertha BSC stieg in der Saison 2012/13 als Meister der 2. Bundesliga in die Bundesliga auf."
    ),

    // Q16 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Wie heißt das Entscheidungsspiel zwischen dem 16. der Bundesliga und dem 3. der 2. Bundesliga?",
        answerA = "Relegation",
        answerB = "Entscheidungsspiel",
        answerC = "Playoff",
        answerD = "Aufstiegsspiel",
        correctAnswer = 0,
        explanation = "Das Relegationsspiel (Relegation) entscheidet zwischen dem 16. der Bundesliga und dem 3. der 2. Bundesliga über Verbleib bzw. Aufstieg.",
        difficulty = 1,
        funFact = "Hertha BSC spielte 2012 eine dramatische Relegation gegen Fortuna Düsseldorf und stieg letztendlich ab."
    ),

    // Q17 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "Wie viele Spieltage hat eine komplette Bundesliga-Saison?",
        answerA = "32",
        answerB = "34",
        answerC = "30",
        answerD = "36",
        correctAnswer = 1,
        explanation = "Eine Bundesliga-Saison mit 18 Vereinen umfasst genau 34 Spieltage (17 Hin- und 17 Rückrundenspieltage).",
        difficulty = 1,
        funFact = "Hertha BSC hat in seiner Bundesliga-Geschichte insgesamt über 1.500 Bundesligaspiele absolviert."
    ),

    // Q18 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "In welchem Monat beginnt die Bundesliga-Saison üblicherweise?",
        answerA = "Juni",
        answerB = "September",
        answerC = "Juli",
        answerD = "August",
        correctAnswer = 3,
        explanation = "Die Bundesliga-Saison beginnt in der Regel im August und endet im Mai des darauffolgenden Jahres.",
        difficulty = 1,
        funFact = "Die Winterpause der Bundesliga dauert meist von Mitte Dezember bis Ende Januar – eine Besonderheit im Vergleich zu anderen Ligen."
    ),

    // Q19 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Was ist die 2. Bundesliga?",
        answerA = "Die zweite Spielklasse Deutschlands",
        answerB = "Die dritte Spielklasse Deutschlands",
        answerC = "Eine Jugendliga",
        answerD = "Eine Regionalliga",
        correctAnswer = 0,
        explanation = "Die 2. Bundesliga ist die zweite Spielklasse im deutschen Fußball direkt unterhalb der Bundesliga.",
        difficulty = 1,
        funFact = "Hertha BSC hat mehrere Saisons in der 2. Bundesliga verbracht und ist damit ein erfahrener Zweitligist."
    ),

    // Q20 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Wie nennt man den Verein, der am Ende einer Saison auf Platz 1 der Bundesliga steht?",
        answerA = "Pokalsieger",
        answerB = "Vizemeister",
        answerC = "Deutscher Meister",
        answerD = "Tabellenführer",
        correctAnswer = 2,
        explanation = "Der Verein auf Platz 1 am Ende der Bundesliga-Saison wird Deutscher Meister und erhält die Meisterschale.",
        difficulty = 1,
        funFact = "Hertha BSC wurde zweimal Deutscher Meister – zuletzt 1931, also noch lange vor der Gründung der Bundesliga."
    ),

    // ── TOPICS 21-30: Fan-Organisationen, Ultras, Ostkurve ───────────────────

    // Q21 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "Wie heißt die bekannteste Fankurve von Hertha BSC im Olympiastadion?",
        answerA = "Nordkurve",
        answerB = "Ostkurve",
        answerC = "Südkurve",
        answerD = "Westkurve",
        correctAnswer = 1,
        explanation = "Die Ostkurve im Olympiastadion ist die traditionelle Fankurve von Hertha BSC und der Treffpunkt der leidenschaftlichsten Fans.",
        difficulty = 1,
        funFact = "Die Ostkurve fasst über 10.000 Stehplätze und gilt als das Herz der Hertha-Fanszene."
    ),

    // Q22 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Was versteht man im Fußball unter 'Ultras'?",
        answerA = "Profispieler in der Reservemannschaft",
        answerB = "Schiedsrichter-Assistenten",
        answerC = "Vereinsmitarbeiter im Stadion",
        answerD = "Besonders leidenschaftliche und organisierte Fangruppen",
        correctAnswer = 3,
        explanation = "Ultras sind besonders leidenschaftliche und organisierte Fangruppen, die durch Gesänge, Choreografien und Fahnen ihre Mannschaft unterstützen.",
        difficulty = 1,
        funFact = "Die Ultra-Bewegung entstand in den 1960er Jahren in Italien und verbreitete sich ab den 1990ern auch in Deutschland."
    ),

    // Q23 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Wie nennt man große Bilddarstellungen, die Ultras in der Fankurve zeigen?",
        answerA = "Tifo",
        answerB = "Transparent",
        answerC = "Choreografie",
        answerD = "Display",
        correctAnswer = 0,
        explanation = "Ein Tifo (aus dem Italienischen) bezeichnet aufwendige Bilddarstellungen und Choreografien, die Ultra-Gruppen in der Fankurve zeigen.",
        difficulty = 1,
        funFact = "Herthas Ultras gehören zu den kreativsten Tifo-Machern Deutschlands und zeigen regelmäßig beeindruckende Choreografien."
    ),

    // Q24 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Was ist der Fanclub-Verband von Hertha BSC?",
        answerA = "Hertha-Fanvereinigung",
        answerB = "Fan-Community Berlin",
        answerC = "Hertha Fan Club Verband",
        answerD = "Blau-Weiß Fanverband",
        correctAnswer = 2,
        explanation = "Der Hertha Fan Club Verband ist der offizielle Dachverband der organisierten Hertha-Fanclubs.",
        difficulty = 1,
        funFact = "Im Hertha Fan Club Verband sind hunderte von Fanclubs aus ganz Deutschland und dem Ausland zusammengeschlossen."
    ),

    // Q25 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Was ist ein Stehplatz in der Fankurve?",
        answerA = "Ein Platz mit erhöhtem Sitz",
        answerB = "Ein VIP-Bereich",
        answerC = "Ein Bereich für Rollstuhlfahrer",
        answerD = "Ein Bereich ohne Sitzplätze, wo Fans stehend zuschauen",
        correctAnswer = 3,
        explanation = "Stehplätze sind Bereiche im Stadion ohne Sitzschalen, wo Fans stehend und oft besonders ausgelassen ihre Mannschaft anfeuern.",
        difficulty = 1,
        funFact = "Die Stehplätze in der Ostkurve kosten deutlich weniger als Sitzplätze und sind deshalb besonders bei jungen Fans und Ultras beliebt."
    ),

    // Q26 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Was singt man als Hertha-Fan häufig im Stadion?",
        answerA = "'Hertha, Hertha'-Schlachtrufe und Vereinshymnen",
        answerB = "Vereinslieder und 'Haie, Haie'-Rufe",
        answerC = "Nur die Nationalhymne",
        answerD = "Popmusik ohne Bezug zum Verein",
        correctAnswer = 0,
        explanation = "Typische Hertha-Fangesänge sind der 'Hertha, Hertha'-Schlachtruf sowie Fanlieder wie 'Blau-Weiß, wie lieb ich dich'.",
        difficulty = 1,
        funFact = "Einige Hertha-Fangesänge sind über Jahrzehnte gewachsen und haben Generationen von Fans verbunden."
    ),

    // Q27 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "Was ist das Gegenstück zu einer Fankurve im Stadion?",
        answerA = "Einlaufbereich",
        answerB = "Haupttribüne",
        answerC = "Pressebox",
        answerD = "Trainingsbank",
        correctAnswer = 1,
        explanation = "Die Haupttribüne (Gegengerade) liegt der Fankurve gegenüber und beherbergt meist Einzelsitze, VIP-Bereiche und die Pressebox.",
        difficulty = 1,
        funFact = "Im Olympiastadion sitzt die Haupttribüne (Westtribüne) gegenüber der Ostkurve und bietet die offiziellen VIP-Bereiche."
    ),

    // Q28 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Was sind 'Auswärtsfans' beim Fußball?",
        answerA = "Fans, die im Ausland wohnen",
        answerB = "Fans, die das Spiel nur im TV schauen",
        answerC = "Fans der Gastmannschaft, die im fremden Stadion mitreisen",
        answerD = "Fans ohne Stadionkarte",
        correctAnswer = 2,
        explanation = "Auswärtsfans sind Anhänger der Gastmannschaft, die ins fremde Stadion reisen, um ihre Mannschaft zu unterstützen.",
        difficulty = 1,
        funFact = "Hertha BSC hat eine treue Auswärtsfanszene, die die Mannschaft auch zu Spielen in anderen Städten begleitet."
    ),

    // Q29 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Wie nennt man den Bereich im Stadion, der nur für Fans der Gastmannschaft reserviert ist?",
        answerA = "Heimblock",
        answerB = "Neutralzone",
        answerC = "Fansektor",
        answerD = "Gästeblock",
        correctAnswer = 3,
        explanation = "Der Gästeblock ist ein abgetrennter Bereich im Stadion, der exklusiv für Fans der auswärts spielenden Mannschaft reserviert ist.",
        difficulty = 1,
        funFact = "Im Olympiastadion befindet sich der Gästeblock in der Nordkurve und fasst mehrere tausend Zuschauer."
    ),

    // Q30 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Was ist eine Fahne oder ein Schal im Fanbereich des Stadions?",
        answerA = "Fanausstattung, die die Verbundenheit zum Verein zeigt",
        answerB = "Pflichtausstattung der Spieler",
        answerC = "Offizielles Kommunikationsmittel des Schiedsrichters",
        answerD = "Sicherheitsausrüstung der Ordner",
        correctAnswer = 0,
        explanation = "Schals und Fahnen sind klassische Fanausstattung, mit denen Hertha-Fans ihre Zugehörigkeit und Leidenschaft für den Verein zeigen.",
        difficulty = 1,
        funFact = "Der blau-weiße Hertha-Schal gehört zu den meistverkauften Fanartikeln und ist ein Symbol der Berliner Fankultur."
    ),

    // ── TOPICS 31-40: Hertha BSC Trainer/Manager ─────────────────────────────

    // Q31 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Welcher Trainer war mehrfach Chefcoach von Hertha BSC?",
        answerA = "Jürgen Klopp",
        answerB = "Thomas Tuchel",
        answerC = "Pál Dárdai",
        answerD = "Hansi Flick",
        correctAnswer = 2,
        explanation = "Pál Dárdai war gleich zweimal Cheftrainer von Hertha BSC – von 2015 bis 2019 und erneut ab 2021.",
        difficulty = 1,
        funFact = "Pál Dárdai spielte selbst viele Jahre für Hertha BSC und ist damit einer der wenigen Trainer, die den Verein als Spieler und Coach vertreten haben."
    ),

    // Q32 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "Aus welchem Land stammt der Trainer Pál Dárdai?",
        answerA = "Polen",
        answerB = "Ungarn",
        answerC = "Tschechien",
        answerD = "Rumänien",
        correctAnswer = 1,
        explanation = "Pál Dárdai ist Ungar und war auch Trainer der ungarischen Nationalmannschaft, bevor er zu Hertha BSC zurückkehrte.",
        difficulty = 1,
        funFact = "Dárdai hat drei Söhne – Palko, Marton und Bence – die alle im Nachwuchs von Hertha BSC spielten bzw. spielen."
    ),

    // Q33 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Was ist die Hauptaufgabe eines Fußballtrainers?",
        answerA = "Tickets zu verkaufen",
        answerB = "Vertragsverhandlungen mit Spielern zu führen",
        answerC = "Das Stadion zu verwalten",
        answerD = "Die Mannschaft taktisch zu betreuen und die Aufstellung zu bestimmen",
        correctAnswer = 3,
        explanation = "Der Cheftrainer ist für die taktische Ausrichtung, das Training und die Aufstellung der Mannschaft verantwortlich.",
        difficulty = 1,
        funFact = "Herthas Trainer arbeitet eng mit einem Trainerteam zusammen, das aus Co-Trainern, Torwarttrainern und Athletiktrainern besteht."
    ),

    // Q34 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Wer war Geschäftsführer Sport bei Hertha BSC und wechselte 2022 zu Borussia Dortmund?",
        answerA = "Fredi Bobic",
        answerB = "Michael Preetz",
        answerC = "Ingo Schiller",
        answerD = "Thomas Kraft",
        correctAnswer = 0,
        explanation = "Fredi Bobic war von 2021 bis 2022 Geschäftsführer Sport bei Hertha BSC, bevor er zum BVB wechselte.",
        difficulty = 1,
        funFact = "Fredi Bobic war als Spieler vor allem durch seine Zeit bei Stuttgart und Dortmund bekannt, bevor er ins Sportmanagement wechselte."
    ),

    // Q35 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Was ist die Aufgabe eines Sportdirektors bei einem Fußballclub?",
        answerA = "Die Fangesänge zu koordinieren",
        answerB = "Das Spielfeld zu pflegen",
        answerC = "Spielerkäufe und -verkäufe sowie Trainerverpflichtungen zu verantworten",
        answerD = "Trikots zu entwerfen",
        correctAnswer = 2,
        explanation = "Der Sportdirektor verantwortet die sportliche Ausrichtung des Vereins, inklusive Transfers und der Verpflichtung des Trainerteams.",
        difficulty = 1,
        funFact = "Bei Hertha BSC hatte die Sportdirektor-Position in den letzten Jahren häufig gewechselt, was als Grund für sportliche Instabilität gilt."
    ),

    // Q36 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "Wie heißt die Einrichtung von Hertha BSC, in der Nachwuchstalente ausgebildet werden?",
        answerA = "Hertha Jugend GmbH",
        answerB = "Hertha BSC Nachwuchsleistungszentrum",
        answerC = "Berliner Fußballakademie",
        answerD = "Blau-Weiß Akademie",
        correctAnswer = 1,
        explanation = "Das Nachwuchsleistungszentrum von Hertha BSC bildet junge Talente aus und ist eines der wichtigsten Jugendzentren Deutschlands.",
        difficulty = 1,
        funFact = "Aus dem Nachwuchsleistungszentrum von Hertha BSC gingen bereits mehrere Nationalspieler hervor."
    ),

    // Q37 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Was bedeutet 'Cheftrainer' bei einem Fußballverein?",
        answerA = "Der Trainer der Jugendmannschaft",
        answerB = "Der Konditionstrainer",
        answerC = "Der Torwarttrainer",
        answerD = "Der ranghöchste Trainer, der die erste Mannschaft betreut",
        correctAnswer = 3,
        explanation = "Der Cheftrainer ist der leitende Trainer, der die erste Mannschaft betreut und für Taktik sowie Aufstellung verantwortlich ist.",
        difficulty = 1,
        funFact = "In der Geschichte von Hertha BSC gab es über 50 verschiedene Cheftrainer – ein Zeichen der turbulenten Vereinsgeschichte."
    ),

    // Q38 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Pál Dárdai spielte als Profi jahrelang für Hertha BSC. Auf welcher Position?",
        answerA = "Mittelfeldspieler",
        answerB = "Torhüter",
        answerC = "Stürmer",
        answerD = "Innenverteidiger",
        correctAnswer = 0,
        explanation = "Pál Dárdai spielte als defensiver Mittelfeldspieler für Hertha BSC und war bekannt für seine Zweikampfstärke und Übersicht.",
        difficulty = 1,
        funFact = "Dárdai bestritt über 280 Bundesligaspiele für Hertha BSC und ist damit einer der dienstältesten Spieler in der Vereinsgeschichte."
    ),

    // Q39 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Was ist der Unterschied zwischen einem Cheftrainer und einem Interimstrainer?",
        answerA = "Es gibt keinen Unterschied",
        answerB = "Der Interimstrainer ist zuständig für die U23",
        answerC = "Ein Interimstrainer übernimmt vorübergehend, bis ein neuer Cheftrainer gefunden wird",
        answerD = "Der Interimstrainer trainiert nur die Torhüter",
        correctAnswer = 2,
        explanation = "Ein Interimstrainer übernimmt die Mannschaft übergangsweise, bis ein festangestellter Cheftrainer verpflichtet wird.",
        difficulty = 1,
        funFact = "Hertha BSC hatte in der jüngeren Vereinsgeschichte mehrfach Interimstrainer – ein Zeichen der sportlichen Turbulenzen."
    ),

    // Q40 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "Was ist eine typische Trainingseinheit in einem Profi-Fußballverein wie Hertha BSC?",
        answerA = "Nur Kraft- und Ausdauertraining",
        answerB = "Taktik-, Technik- und konditionelles Training",
        answerC = "Ausschließlich Torschussübungen",
        answerD = "Nur Videoanalysen",
        correctAnswer = 1,
        explanation = "Das professionelle Training umfasst taktische Übungen, Technikeinheiten und konditionelle Einheiten zur optimalen Vorbereitung.",
        difficulty = 1,
        funFact = "Hertha BSC trainiert auf dem vereinseigenen Gelände in Berlin-Charlottenburg, dem 'Hertha-Trainingsgelände Hanns Braun'."
    ),

    // ── TOPICS 41-50: Internationale und europäische Wettbewerbe ─────────────

    // Q41 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "In welchem europäischen Wettbewerb spielte Hertha BSC in der Saison 1999/2000?",
        answerA = "UEFA Cup",
        answerB = "Europapokal der Pokalsieger",
        answerC = "Intertoto-Cup",
        answerD = "Champions League",
        correctAnswer = 3,
        explanation = "Hertha BSC nahm in der Saison 1999/2000 an der UEFA Champions League teil – ein historischer Meilenstein für den Berliner Klub.",
        difficulty = 1,
        funFact = "In der Champions-League-Saison 1999/2000 spielte Hertha BSC in einer Gruppe mit dem FC Chelsea und Galatasaray Istanbul."
    ),

    // Q42 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Wie heißt der wichtigste europäische Vereinswettbewerb im Fußball?",
        answerA = "UEFA Champions League",
        answerB = "UEFA Nations League",
        answerC = "FIFA Club World Cup",
        answerD = "DFB-Pokal",
        correctAnswer = 0,
        explanation = "Die UEFA Champions League ist der prestigereichste europäische Vereinswettbewerb und wird jährlich von der UEFA ausgetragen.",
        difficulty = 1,
        funFact = "Die Champions League wurde 1992 aus dem Europapokal der Landesmeister weiterentwickelt und trägt seitdem diesen Namen."
    ),

    // Q43 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Wie qualifiziert sich ein Bundesliga-Verein normalerweise für die Champions League?",
        answerA = "Durch Teilnahme am DFB-Pokal",
        answerB = "Durch Wildcard der UEFA",
        answerC = "Durch eine gute Platzierung in der Bundesliga (Plätze 1-4)",
        answerD = "Durch Sieg im Ligapokal",
        correctAnswer = 2,
        explanation = "Aus der Bundesliga qualifizieren sich die Vereine auf den Plätzen 1 bis 4 für die Champions League.",
        difficulty = 1,
        funFact = "Hertha BSC erreichte um die Jahrtausendwende mehrfach die Champions-League-Qualifikation durch starke Bundesliga-Platzierungen."
    ),

    // Q44 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "Wie heißt der zweitwichtigste europäische Vereinswettbewerb?",
        answerA = "DFB-Supercup",
        answerB = "UEFA Europa League",
        answerC = "UEFA Super Cup",
        answerD = "UEFA Intertoto Cup",
        correctAnswer = 1,
        explanation = "Die UEFA Europa League ist nach der Champions League der zweitwichtigste europäische Vereinswettbewerb.",
        difficulty = 1,
        funFact = "Die Europa League entstand 2009 aus der Zusammenlegung des UEFA Cups und des Intertoto Cups."
    ),

    // Q45 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Welche Organisation veranstaltet die Champions League?",
        answerA = "FIFA",
        answerB = "DFB",
        answerC = "IOC",
        answerD = "UEFA",
        correctAnswer = 3,
        explanation = "Die UEFA (Union of European Football Associations) ist der europäische Fußballverband und veranstaltet die Champions League.",
        difficulty = 1,
        funFact = "Die UEFA wurde 1954 gegründet und hat ihren Sitz in Nyon, Schweiz."
    ),

    // Q46 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "In welchem europäischen Wettbewerb spielte Hertha BSC mehrfach in den frühen 2000er Jahren?",
        answerA = "UEFA Cup (heute Europa League)",
        answerB = "Europapokal der Landesmeister",
        answerC = "Cup Winners' Cup",
        answerD = "Super Cup",
        correctAnswer = 0,
        explanation = "Hertha BSC nahm in den frühen 2000er Jahren mehrfach am UEFA Cup teil und sammelte so internationale Erfahrung.",
        difficulty = 1,
        funFact = "Der UEFA Cup war der Vorgänger der heutigen Europa League und endete 2009 nach 37 Jahren in dieser Form."
    ),

    // Q47 – correctAnswer = 2 (C)
    Question(
        categoryId = 13,
        questionText = "Was ist die Gruppenphase in der Champions League?",
        answerA = "Die Qualifikationsrunde vor dem Turnier",
        answerB = "Das Finale des Wettbewerbs",
        answerC = "Die Phase, in der Teams in Gruppen gegeneinander spielen, bevor das K.-o.-System beginnt",
        answerD = "Ein Freundschaftsturnier",
        correctAnswer = 2,
        explanation = "In der Gruppenphase spielen je 4 Teams in einer Gruppe gegeneinander. Die besten zwei Teams kommen ins K.-o.-System.",
        difficulty = 1,
        funFact = "Hertha BSC erreichte in der Champions League 1999/2000 die Gruppenphase, schied dort aber letztendlich aus."
    ),

    // Q48 – correctAnswer = 1 (B)
    Question(
        categoryId = 13,
        questionText = "Was bedeutet K.-o.-System in einem Fußballwettbewerb?",
        answerA = "Jeder Verein spielt gegen jeden",
        answerB = "Der Verlierer scheidet aus dem Wettbewerb aus",
        answerC = "Punkte werden addiert",
        answerD = "Das Hinspiel zählt doppelt",
        correctAnswer = 1,
        explanation = "Im K.-o.-System scheidet der Verlierer sofort aus dem Wettbewerb aus – es gibt keine zweite Chance.",
        difficulty = 1,
        funFact = "Ab dem Achtelfinale der Champions League gilt das K.-o.-System mit Hin- und Rückspielen."
    ),

    // Q49 – correctAnswer = 0 (A)
    Question(
        categoryId = 13,
        questionText = "Wie heißt der dritte europäische Vereinswettbewerb, der 2021 eingeführt wurde?",
        answerA = "UEFA Conference League",
        answerB = "UEFA Nations Cup",
        answerC = "UEFA Elite League",
        answerD = "UEFA Challenge Cup",
        correctAnswer = 0,
        explanation = "Die UEFA Conference League wurde zur Saison 2021/22 eingeführt und ist der dritte europäische Vereinswettbewerb der UEFA.",
        difficulty = 1,
        funFact = "Die Conference League bietet kleineren europäischen Vereinen die Möglichkeit, internationalen Fußball zu spielen."
    ),

    // Q50 – correctAnswer = 3 (D)
    Question(
        categoryId = 13,
        questionText = "Was ist das Finale in der Champions League?",
        answerA = "Die erste Runde des Turniers",
        answerB = "Das Spiel zwischen den Gruppenletzten",
        answerC = "Ein Freundschaftsspiel",
        answerD = "Das Spiel zwischen den besten zwei Mannschaften am Ende des Wettbewerbs",
        correctAnswer = 3,
        explanation = "Das Champions-League-Finale ist das entscheidende Spiel zwischen den zwei besten Mannschaften am Ende des Turniers.",
        difficulty = 1,
        funFact = "Das Champions-League-Finale findet jährlich in einem anderen europäischen Stadion statt und gilt als das größte Vereinsspiel der Welt."
    )

)
