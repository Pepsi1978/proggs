package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsMedium4(): List<Question> = listOf(

    // --- Paralympics-Details (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fanden die ersten Sommer-Paralympics der modernen Aera statt?",
        answerA = "1948 in London",
        answerB = "1960 in Rom",
        answerC = "1964 in Tokio",
        answerD = "1952 in Helsinki",
        correctAnswer = 1,
        explanation = "Die ersten offiziellen Sommer-Paralympics fanden 1960 in Rom statt – parallel zu den Olympischen Spielen. 400 Athleten aus 23 Laendern nahmen teil. Die Vorgaengerspiele 1948 und 1952 waren reine Rollstuhlsport-Wettbewerbe fuer Kriegsversehrte (Stoke Mandeville Games).",
        difficulty = 2,
        funFact = "Die Paralympics wurden von Ludwig Guttmann gegruendet, einem deutschen Neurologen, der nach seiner Emigration in England mit Sport als Rehabilitationstherapie fuer querschnittsgelaehmte Soldaten experimentierte."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet die Klassifikation 'S1' beim Paralympischen Schwimmen?",
        answerA = "Die schnellste Startklasse ohne Einschraenkungen",
        answerB = "Schwimmer mit minimaler Funktionsfaehigkeit der Extremitaeten, oft mit Tetraplegie",
        answerC = "Eine Sehbehinderungs-Klasse fuer vollblinde Schwimmer",
        answerD = "Einsteigerklasse fuer neue Paralympioniken",
        correctAnswer = 1,
        explanation = "Im Paralympischen Schwimmen beschreiben S-Klassen die koerperliche Beeintraechtigung. S1-Schwimmer haben eine stark eingeschraenkte Rumpf- und Extremitaetenfunktion, oft bei Tetraplegie oder aehnlichen Erkrankungen. Hoehere Nummern bedeuten weniger starke Beeintraechtigung.",
        difficulty = 2,
        funFact = "Das Klassifikationssystem im Parasport wird regelmaessig aktualisiert. Athleten werden von unabhaengigen Klassifikatoren eingestuft – ein fairer Sport bedeutet, Menschen mit aehnlicher Funktionalitaet gegeneinander antreten zu lassen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Sportarten waren bei den Sommer-Paralympics 2024 in Paris vertreten?",
        answerA = "15 Sportarten",
        answerB = "22 Sportarten",
        answerC = "29 Sportarten",
        answerD = "35 Sportarten",
        correctAnswer = 2,
        explanation = "Bei den Sommer-Paralympics 2024 in Paris wurden 549 Medaillenwettbewerbe in 22 Sportarten ausgetragen. Das Programm umfasste u.a. Leichtathletik, Schwimmen, Rollstuhlbasketball, Boccia, Goalball und Paracyclng.",
        difficulty = 2,
        funFact = "Boccia ist eine Sportart, die ausschliesslich bei den Paralympics gespielt wird – sie existiert nicht im olympischen Programm. Sie wird von Athleten mit schweren motorischen Beeintraechtigungen gespielt und erfordert enorme Praezision."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Goalball' und fuer welche Gruppe von Athleten wurde es entwickelt?",
        answerA = "Fussball fuer Rollstuhlfahrer mit angepassten Toren",
        answerB = "Ein Mannschaftsspiel fuer sehbehinderte Athleten, die einen klingenden Ball werfen",
        answerC = "Basketball fuer Athleten mit Armprothesen",
        answerD = "Eine Torschuss-Sportart fuer Athleten mit Beinprothesen",
        correctAnswer = 1,
        explanation = "Goalball wurde 1946 fuer sehbehinderte Kriegsveteranen entwickelt. Zwei Dreier-Teams versuchen, einen klingenden Ball ins gegnerische Tor zu rollen oder zu werfen. Alle Spieler tragen Augenbinden, sodass auch Teilsehende gleichgestellt sind.",
        difficulty = 2,
        funFact = "Beim Goalball herrscht absolute Stille im Publikum, waehrend der Ball im Spiel ist – die Spieler muessen das Klingeln im Ball hoeren, um ihn abfangen zu koennen. Das Publikum klatscht nur in Spielpausen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation fuehrt seit Jahrzehnten die Medaillenwertung der Sommer-Paralympics an?",
        answerA = "Grossbritannien",
        answerB = "Deutschland",
        answerC = "USA",
        answerD = "China",
        correctAnswer = 3,
        explanation = "China ist seit den Spielen 2004 in Athen die dominante Nation im Medaillenspiegel der Sommer-Paralympics. Bei den Spielen 2024 in Paris gewann China erneut die meisten Goldmedaillen. Die USA und Grossbritannien folgen regelmaessig auf den Plaetzen.",
        difficulty = 2,
        funFact = "China investiert massiv in den Parasport seit den eigenen Spielen 2008 in Peking. Die Staatsforderung beinhaltet spezielle Talentfoerderprogramme fuer Athleten mit Behinderungen – mit deutlichem sportlichem Erfolg."
    ),

    Question(
        categoryId = 6,
        questionText = "Was unterscheidet Paratriathlon von olympischem Triathlon bezueglich der Startkategorien?",
        answerA = "Paratriathlon hat kuerzerere Distanzen und teilt Athleten in Funktionsklassen ein",
        answerB = "Paratriathlon findet nur im Hallenbad statt",
        answerC = "Es gibt keine Schwimmdisziplin beim Paratriathlon",
        answerD = "Paratriathlon verwendet ausschliesslich Handcycles statt Fahrraedern",
        correctAnswer = 0,
        explanation = "Paratriathlon nutzt kuerzerere Distanzen (750m Schwimmen, 20km Radfahren, 5km Laufen) und teilt Athleten in Funktionsklassen ein – z.B. PTVI (sehbehindert), PTS (stehend), PTWC (Rollstuhl). Handcycles, Tandems und andere Hilfsmittel sind je nach Klasse erlaubt.",
        difficulty = 2,
        funFact = "Paratriathlon wurde 2016 in Rio de Janeiro erstmals olympisch. Deutsche Athleten wie Martin Schulz (Amputee) gehoeren zur Weltspitze in dieser Sportart."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer war der erste Athlet, der sowohl bei Olympischen Spielen als auch bei Paralympics an Sprints teilnahm?",
        answerA = "Alistair McAlpine",
        answerB = "Oscar Pistorius",
        answerC = "Markus Rehm",
        answerD = "Heinrich Popow",
        correctAnswer = 1,
        explanation = "Oscar Pistorius aus Suedafrika nahm 2012 in London sowohl an den Olympischen Spielen (400m) als auch an den Paralympics teil – als erster doppelbeinig amputierter Athlet im olympischen Sprint. Er lief mit Karbonfaser-Prothesen (Cheetah-Blades).",
        difficulty = 2,
        funFact = "Vor Pistorius' Olympia-Teilnahme gab es eine jahrelange wissenschaftliche Debatte, ob seine Karbonprothesen einen Vorteil gegenueber biologischen Beinen bringen. Das IAAF erlaubte seine Teilnahme schliesslich nach einem Schiedsspruch des CAS."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Boccia' und wie wird die Spielflaeche bei Wettkaeampfen bezeichnet?",
        answerA = "Ein Wasserball-Spiel fuer Querschnittsgelaehmte im Schwimmbad",
        answerB = "Ein Praezisionswurf-Spiel auf einem Boccia-Court, bei dem Baelle nah an einen Zielball geworfen werden",
        answerC = "Eine Tennisform fuer Athleten mit nur einem Arm",
        answerD = "Ein Kegelsport fuer sehbehinderte Athleten",
        correctAnswer = 1,
        explanation = "Boccia ist ein Praezisionssport fuer Athleten mit schweren motorischen Beeintraechtigungen. Spieler werfen, rollen oder kicken – oder nutzen Hilfsmittel – rote oder blaue Lederbaelle so nah wie moeglich an einen weissen Zielball (Jack) heran. Der Court ist 12,5m x 6m gross.",
        difficulty = 2,
        funFact = "In der Boccia-Klasse BC3 koennen Athleten, die weder Baelle werfen noch treten koennen, eine Rampe mit Kopf-, Mund- oder Haendsteuerung verwenden. Einige Athleten benoetigen Assistenten, die aber wegschauen muessen, wenn der Athlet spielt."
    ),

    // --- Sportstaetten und Stadien (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Was ist die groesste Sportstaette der Welt nach Fassungsvermoegen (Stand 2024)?",
        answerA = "Camp Nou in Barcelona mit 99.000 Plaetzen",
        answerB = "Rungrado 1st of May Stadium in Pjoengjang mit ca. 114.000 Plaetzen",
        answerC = "Melbourne Cricket Ground mit 100.024 Plaetzen",
        answerD = "Michigan Stadium in Ann Arbor mit 107.601 Plaetzen",
        correctAnswer = 1,
        explanation = "Das Rungrado 1st of May Stadium in Nordkoreas Hauptstadt Pjoengjang fasst offiziell ca. 114.000 Zuschauer und gilt als die groesste Sportstaette der Welt. Es wurde 1989 eroeffnet und wird hauptsaechlich fuer Fussball und Massenveranstaltungen genutzt.",
        difficulty = 2,
        funFact = "Das Rungrado-Stadion ist benannt nach einer Insel im Fluss Taedong. Es wurde gebaut fuer die 13. Weltjugendfestspiele 1989 – eine sozialistische Grossveranstaltung mit 22.000 Teilnehmern aus 177 Laendern."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Stadt steht das Wembley-Stadion und wann wurde das aktuelle Stadion eroeffnet?",
        answerA = "Manchester, 1999",
        answerB = "London, 2007",
        answerC = "Birmingham, 2003",
        answerD = "Liverpool, 2005",
        correctAnswer = 1,
        explanation = "Das aktuelle Wembley-Stadion steht in London und wurde 2007 eroeffnet. Es fasst 90.000 Zuschauer und ist das Nationalstadion Englands. Das Vorgaengerstadion (1923–2000) war ebenfalls in Wembley und beherbergte das WM-Finale 1966.",
        difficulty = 2,
        funFact = "Der markante Wembley-Bogen ist 133 Meter hoch und das groesste Einzeldach-Bogenbauwerk der Welt. Er ist von jedem Punkt auf dem Spielfeld sichtbar – und von weitem ein unverwechselbares Wahrzeichen Londons."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Stadion nennen seine Fans 'La Bombonera' und in welcher Stadt steht es?",
        answerA = "Estadio Monumental, Buenos Aires",
        answerB = "Estadio Alberto J. Armando, Buenos Aires (Boca Juniors)",
        answerC = "Estadio Santiago Bernabeu, Madrid",
        answerD = "Maracana-Stadion, Rio de Janeiro",
        correctAnswer = 1,
        explanation = "Die 'Bombonera' (Pralinenschachtel) ist das Heimstadion von Boca Juniors in Buenos Aires. Sein offizieller Name ist Estadio Alberto J. Armando. Die ungewoehnliche D-Form und die steilen Tribueenen erzeugen eine besondere Atmosfhaere – eine der lautesten der Welt.",
        difficulty = 2,
        funFact = "La Bombonera ist so laut, dass bei vollem Haus die Tribueenen spuerbar vibrieren. Der Spitzname kommt von der Form des Stadions – es sieht aus wie ein Schokoladenschachtel (Bombonera). Kapazitaet: ca. 54.000 Zuschauer."
    ),

    Question(
        categoryId = 6,
        questionText = "Wo fand das WM-Finale 1950 statt, das Brasilien gegen Uruguay verlor und als 'Maracanazo' in die Geschichte einging?",
        answerA = "Estadio Monumental, Buenos Aires",
        answerB = "Estadio do Maracana, Rio de Janeiro",
        answerC = "Estadio Azteca, Mexiko-Stadt",
        answerD = "Estadi Olimpic de Montjuic, Barcelona",
        correctAnswer = 1,
        explanation = "Das Maracana-Stadion in Rio de Janeiro war Schauplatz des WM-Endspiels 1950. Brasilien verlor vor ~200.000 Zuschauern 1:2 gegen Uruguay – ein nationales Trauma, bekannt als 'Maracanazo'. Das Stadion ist heute auf ~78.000 Plaetze modernisiert.",
        difficulty = 2,
        funFact = "Das Maracana war beim Bau 1950 das groesste Stadion der Welt mit offiziell 173.850 Plaetzen. Beim WM-Spiel 1950 drangen sich schaetzungsweise 199.854 Zuschauer hinein – bis heute der Weltrekord fuer Zuschauer bei einem Fussballspiel."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Bird's Nest' und wo steht es?",
        answerA = "Ein Rugbystadion in Sydney, Australien",
        answerB = "Das Nationalstadion Peking, China – Hauptstaette der Olympischen Sommerspiele 2008",
        answerC = "Ein Formel-1-Circuit in Singapur",
        answerD = "Ein indoor-Velodrom in Tokio, Japan",
        correctAnswer = 1,
        explanation = "Das 'Bird's Nest' (Vogelnest) ist der Spitzname des Nationalstadions Peking. Es wurde fuer die Olympischen Sommerspiele 2008 gebaut, fasst ~91.000 Zuschauer und ist fuer seine charakteristische Stahl-Gitterstruktur bekannt. Architekt: Herzog & de Meuron.",
        difficulty = 2,
        funFact = "Das Pekinger Nationalstadion wurde auch bei den Winterspielen 2022 genutzt – fuer die Eroeffnungs- und Abschlusszeremonie. Damit ist es das einzige Stadion, das sowohl Sommer- als auch Winter-Olympia-Zeremonien beherbergte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Formel-1-Rennen findet in Monaco statt und was macht den Circuit de Monaco besonders?",
        answerA = "Monaco-GP auf einem permanenten Rennkurs ausserhalb der Stadt",
        answerB = "Monaco-GP auf oeffentlichen Stadtstrassen mit sehr engen Kurven und ohne Auslaufzonen",
        answerC = "Monaco-GP auf einem hybriden Kurs aus Strassensegmenten und permanentem Oval",
        answerD = "Monaco-GP im Stadttunnel, fast vollstaendig unterirdisch",
        correctAnswer = 1,
        explanation = "Der Circuit de Monaco fuehrt durch die engen Strassen des Fuerstentums Monaco. Es ist einer der langsamsten F1-Kurse wegen enger Kurven und hat kaum Auslaufzonen. Ein Fehler bedeutet fast immer Kontakt mit Leitplanken – technisch einer der anspruchsvollsten Kurse der Welt.",
        difficulty = 2,
        funFact = "Der Monaco-GP ist Teil der inoffiziellen 'Triple Crown of Motorsport' – neben dem Indianapolis 500 und dem 24-Stunden-Rennen von Le Mans. Nur Graham Hill und Juan Manuel Fangio gewannen alle drei. Ayrton Senna gewann Monaco sechsmal."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das Fassungsvermoegen des Olympiastadions Berlin und welche Bedeutung hat es historisch?",
        answerA = "55.000 Plaetze – gebaut 1972 fuer die Muenchner Olympischen Spiele",
        answerB = "74.244 Plaetze – urspruenglich gebaut 1936 fuer die Olympischen Sommerspiele in Berlin",
        answerC = "89.000 Plaetze – das groesste Stadion Deutschlands, gebaut 1990",
        answerD = "62.000 Plaetze – eroeffnet 1974 fuer die Fussball-WM",
        correctAnswer = 1,
        explanation = "Das Olympiastadion Berlin fasst 74.244 Zuschauer und wurde fuer die Olympischen Sommerspiele 1936 erbaut. Es war Schauplatz von Jesse Owens' vier Goldmedaillen. Das Stadion wurde 2004 renoviert und ist heute Heimstaette von Hertha BSC.",
        difficulty = 2,
        funFact = "Bei den Olympischen Spielen 1936 gewann der afroamerikanische Leichtathlet Jesse Owens vier Goldmedaillen – 100m, 200m, Weitsprung, 4x100m-Staffel – direkt vor Adolf Hitler. Dies gilt als eine der groessten sportlichen Gesten gegen den Nationalsozialismus."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Tennisstadion ist bekannt fuer 'Court Philippe Chatrier' und wo liegt es?",
        answerA = "Wimbledon All England Club, London",
        answerB = "Stade Roland Garros, Paris",
        answerC = "USTA Billie Jean King National Tennis Center, New York",
        answerD = "Melbourne Park, Melbourne",
        correctAnswer = 1,
        explanation = "Der Court Philippe Chatrier ist das Hauptstadion im Stade Roland Garros in Paris, Heimstaette der French Open. Er fasst ~15.000 Zuschauer und ist mit einem Retractable-Dach ausgestattet (seit 2020). Benannt nach dem franzoesischen Tennisverbaendsfunktionaer Philippe Chatrier.",
        difficulty = 2,
        funFact = "Roland Garros wurde nach einem franzoesischen Weltkriegs-Flugass benannt, der im Ersten Weltkrieg ein Synchronisierungsgetriebe fuer Maschinengewehre erfand und 1918 abgeschossen wurde. Er hatte keine direkte Verbindung zu Tennis."
    ),

    // --- Sportrecht und VAR (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Was bedeutet die Abkuerzung VAR im Fussball?",
        answerA = "Velocity Analysis Report – Geschwindigkeitsmessung fuer Offside-Entscheidungen",
        answerB = "Video Assistant Referee – ein Videoschiedsrichter-System zur Ueberpruefung von Entscheidungen",
        answerC = "Virtual Action Replay – eine Grafik-Darstellung von Spielszenen",
        answerD = "Variable Advantage Rule – eine flexible Vorteilsregel",
        correctAnswer = 1,
        explanation = "VAR steht fuer Video Assistant Referee. Ein Team von Videoschiedsrichtern in einem Video Operations Room (VOR) ueberprueft kritische Entscheidungen (Tore, Elfmeter, Rote Karten, Personenverwechslungen) und informiert den Hauptschiedsrichter, der final entscheidet.",
        difficulty = 2,
        funFact = "Der VAR wurde zuerst in der Bundesliga getestet (2017) und in der WM 2018 in Russland erstmals offiziell bei einer Weltmeisterschaft eingesetzt. In der Premier League wurde er erst 2019/20 eingefuehrt – spaeter als in vielen anderen Ligen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche vier Spielsituationen kann der VAR im Fussball ueberpruefen?",
        answerA = "Alle Freistoesse, Einwuerfe, Eckbälle und Abseitsentscheidungen",
        answerB = "Tore, Elfmeter-Entscheidungen, Rote Karten und Spielerverwechslungen",
        answerC = "Gelbe Karten, Verletzungsunterbrechungen, Einwechslungen und Zeitspiel",
        answerD = "Nur Abseitsentscheidungen und Torentscheidungen",
        correctAnswer = 1,
        explanation = "Der VAR darf vier klar definierte Spielsituationen pruefen: Tor/Kein-Tor, Elfmeter-Entscheidungen, Rote Karten (direkte und verhinderte klare Torchancen) sowie Spielerverwechslungen (falscher Spieler bestraft oder verwechselt). Alles andere liegt beim Hauptschiedsrichter.",
        difficulty = 2,
        funFact = "Der VAR greift nur bei 'clear and obvious errors' (klaren und offensichtlichen Fehlern) ein – nicht bei Entscheidungen, die umstritten sind, aber vertretbar waren. Der Standard ist ein Unterschied zwischen einem Fehler und einer anderen Interpretation."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Handspiel-Regelwerk' nach der FIFA-Reform 2021 und warum war es umstritten?",
        answerA = "Jeder Handkontakt fuehrt zu einem Elfmeter, egal ob unabsichtlich",
        answerB = "Nur absichtliche Handspieler werden bestraft, aber Tore nach unabsichtlichem Handspiel des Angreifers werden nicht anerkannt",
        answerC = "Handspiel ist nur in der eigenen Haelfte strafbar",
        answerD = "Das Handball-Regelwerk wurde vollstaendig abgeschafft",
        correctAnswer = 1,
        explanation = "Nach der FIFA-Reform gilt: Absichtliches Handspiel wird bestraft. Tore nach unabsichtlichem Handspiel des Torschuetzen oder eines Mitspielers im Vorfeld werden aberkannt. Das sorgte fuer Kontroversen, da unabsichtliche Handspiele strittig blieben.",
        difficulty = 2,
        funFact = "Die Handball-Regel ist eine der aeltesten und umstrittensten Regeln im Fussball. Im Englischen heisst es 'handball' – in Deutschland unterscheidet man 'Handspiel' (unabsichtlich) und 'Hand' (absichtlich). Die Grenze ist oft eine Ermessensfrage."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Bosman-Urteil' und welche Institution faellte es?",
        answerA = "FIFA-Disziplinarkommission, Urteil 1992 zu Transfergebuehren",
        answerB = "Europaeischer Gerichtshof (EuGH), Urteil 1995 ueber die Freizuegigkeit von EU-Fussballspielern",
        answerC = "UEFA-Exekutivkomitee, Beschluss 2001 zu Auslaenderkontingenten",
        answerD = "Internationaler Sportgerichtshof (CAS), Urteil 1998 zu Spielerrechten",
        correctAnswer = 1,
        explanation = "Der Europaeische Gerichtshof entschied 1995 im Fall Bosman (Jean-Marc Bosman vs. RFC Liege), dass EU-Spieler nach Vertragsende den Verein frei wechseln duerfen – ohne Abloesegebuehr. Ausserdem wurde die Begrenzung auf bestimmte EU-Spieler pro Mannschaft fuer rechtswidrig erklaert.",
        difficulty = 2,
        funFact = "Jean-Marc Bosman selbst profitierte kaum vom Urteil, das seinen Namen traegt. Er spielte nie mehr auf hohem Niveau und kaeampfte jahrelang mit persoenlichen Problemen. Das Urteil veraenderte den Fussball weltweit, aber nicht Bosmans eigene Karriere."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der CAS und welche Rolle spielt er im Sport?",
        answerA = "Court of Arbitration for Sport – das international Schiedsgericht fuer sportrechtliche Streitigkeiten",
        answerB = "Council of Athletic Standards – eine Koerperschaft die Weltrekorde anerkennt",
        answerC = "Central Anti-Doping Service – die Hauptkontrollbehoerde fuer Doping",
        answerD = "Committee for Amateur Sports – die Behoerde fuer Amateursport-Regeln",
        correctAnswer = 0,
        explanation = "Der CAS (Court of Arbitration for Sport) ist das hoehere Schiedsgericht fuer Streitigkeiten im internationalen Sport mit Sitz in Lausanne (Schweiz). Hier werden Urteile ueber Doping-Sperren, Transferstreitigkeiten, Olympia-Ausschluss und andere Sportrechtsfaelle gefaellt.",
        difficulty = 2,
        funFact = "Der CAS wurde 1984 gegruendet und hat seit 2000 einen stark wachsenden Klagestapel. Bei den Olympischen Spielen ist jeweils ein CAS-Ad-hoc-Gremium vor Ort, um schnell Entscheidungen zu treffen – manchmal innerhalb von Stunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Was regelt der 'Offsideline Freeze' beim VAR bei Abseitsentscheidungen?",
        answerA = "Die Linie wird im Moment des letzten Kontakts des passspielenden Spielers eingefroren",
        answerB = "Die Abseitsposition des Angreifers wird im Moment des Torschusses gemessen",
        answerC = "Beide Linien werden gleichzeitig in Echtzeit gezogen, ohne Einfrieren",
        answerD = "Die Linie wird immer beim Empfang des Balles durch den Angreifer gemessen",
        correctAnswer = 0,
        explanation = "Beim VAR-Abseitscheck wird das Bild exakt im Moment des letzten Ballkontakts des Passspielers eingefroren. Dann werden semi-automatische Linien gezogen, die die koerperliche Position aller relevanten Spieler in diesem Moment abbilden.",
        difficulty = 2,
        funFact = "Die semi-automatische Abseitstechnologie (SAOT) wurde erstmals bei der WM 2022 in Katar eingesetzt. Sie nutzt KI-Kameras und Sensoren, um Spielerpositionen in Millisekunden zu bestimmen – mit einer Genauigkeit von 0,001 Sekunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter 'Sportswashing' im modernen Sport?",
        answerA = "Die Reinigung von Sporttrikots mit umweltfreundlichen Mitteln",
        answerB = "Der Einsatz grosser Sportveranstaltungen oder Investitionen zur Aufbesserung des internationalen Images eines Landes oder Regimes",
        answerC = "Das Aufdecken von Korruption in Sportorganisationen",
        answerD = "Die Transferstrategie, Spieler zu verkaufen um Bilanzen zu bereinigen",
        correctAnswer = 1,
        explanation = "Sportswashing bezeichnet die Strategie, Sportveranstaltungen oder Vereins-Investitionen zu nutzen, um das internationale Ansehen eines Landes oder Regimes zu verbessern und von Menschenrechtsverletzungen abzulenken. Beispiele: WM 2022 in Katar, Saudi-Arabiens Investitionen im Golf, Formel 1 in autoritaeren Staaten.",
        difficulty = 2,
        funFact = "Der Begriff 'Sportswashing' ist eine Abwandlung von 'Greenwashing'. Erstmals weit verbreitet wurde er im Zusammenhang mit der Vergabe der Fussball-WM 2018 an Russland und 2022 an Katar, wo Menschenrechtsorganisationen massive Kritik aeusserten."
    ),

    // --- Frauen im Sport (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Wer gewann 2023 die erste Fussball-Weltmeisterschaft der Frauen?",
        answerA = "USA",
        answerB = "England",
        answerC = "Schweden",
        answerD = "Spanien",
        correctAnswer = 3,
        explanation = "Spanien gewann 2023 die FIFA Frauen-WM in Australien und Neuseeland mit einem 1:0-Finalsieg gegen England. Es war Spaniens erster WM-Titel der Frauen. Aiyana Bonmati wurde als beste Spielerin des Turniers ausgezeichnet.",
        difficulty = 2,
        funFact = "Die Frauen-WM 2023 war die meistbesuchte und meistgeschaute Frauen-WM aller Zeiten. Das Finale in Sydney verfolgten ca. 2 Milliarden TV-Zuschauer weltweit. Spaniens Triumph war trotz eines internen Verbandsskandals um Praesident Luis Rubiales moeglich."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Frau hielt den 100-Meter-Weltrekord bei den Frauen mit 10,49 Sekunden seit 1988?",
        answerA = "Marion Jones",
        answerB = "Florence Griffith-Joyner (FloJo)",
        answerC = "Evelyn Ashford",
        answerD = "Shelly-Ann Fraser-Pryce",
        correctAnswer = 1,
        explanation = "Florence Griffith-Joyner (bekannt als FloJo) lief 1988 bei den US-Trials 10,49 Sekunden ueber 100 Meter – ein Weltrekord, der ueber 35 Jahre Bestand hatte. Sie gewann bei den Olympischen Spielen 1988 in Seoul dreimal Gold und war fuer ihre aufwendigen Laufkostueme bekannt.",
        difficulty = 2,
        funFact = "Der 10,49-Sekunden-Rekord von FloJo wurde erst 2024 in Eugene gebrochen – von Sha'Carri Richardson und dann definitiv von Julien Alfred (St. Lucia) bei den Olympischen Spielen in Paris 2024 mit 10,72 s (Goldmedaille). FloJos offizieller Weltrekord stand aber bis 2024."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Tennisspielerin gewann die meisten Grand-Slam-Einzel-Titel in der Geschichte (Frauen und Maenner)?",
        answerA = "Serena Williams mit 23 Titeln",
        answerB = "Margaret Court mit 24 Titeln",
        answerC = "Steffi Graf mit 22 Titeln",
        answerD = "Novak Djokovic mit 24 Titeln",
        correctAnswer = 1,
        explanation = "Margaret Court (Australien) gewann 24 Grand-Slam-Einzel-Titel – die meisten in der Geschichte aller Tennisspielerinnen und -spieler. Aktuelle Konkurrenten: Novak Djokovic hat ebenfalls 24 Titel gewonnen (Stand 2024). Serena Williams gewann 23 Grand Slams.",
        difficulty = 2,
        funFact = "Margaret Court gewann 1970 den Calendar Grand Slam – alle vier Grand Slams im gleichen Kalenderja hr. Steffi Graf gelang dies 1988 in der Open Era und sie fuegt noch Olympia-Gold hinzu (Golden Slam). Court spielte in einer Aera ohne freie Berichterstattung und bleibt unterschaetzt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war das historische Spiel 'Battle of the Sexes' 1973 im Tennis?",
        answerA = "Das erste Mixed-Doubles-Finale bei Wimbledon",
        answerB = "Ein Schaukampf zwischen Billie Jean King und Bobby Riggs, den King gewann",
        answerC = "Die erste offizielle Frauen-Tennismeisterschaft gleichberechtigt neben Maennern",
        answerD = "Das erste Spiel bei dem Frauen und Maenner im selben Turnier antraten",
        correctAnswer = 1,
        explanation = "Bobby Riggs, ehemaliger Weltranglisten-Erster, behauptete 1973, keine Frau koenne ihn besiegen. Billie Jean King nahm die Herausforderung an und gewann im Houston Astrodome vor 30.492 Zuschauern und Millionen TV-Zuschauern klar in drei Saetzen (6:4, 6:3, 6:3).",
        difficulty = 2,
        funFact = "Das 'Battle of the Sexes' hatte enormen kulturellen Einfluss auf die Frauenrechtsbewegung. Billie Jean King hatte bereits 1972 dafuer gekaempft, dass die US Open gleiche Preisgelder zahlt – und gewann. Das Spiel ist 2017 mit Emma Stone und Steve Carell verfilmt worden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Frau ist die erfolgreichste Skifahrerin der Geschichte nach Weltcup-Gesamtsiegen?",
        answerA = "Lindsey Vonn (USA)",
        answerB = "Mikaela Shiffrin (USA)",
        answerC = "Marlies Schild (Oesterreich)",
        answerD = "Maria Hoell (Oesterreich)",
        correctAnswer = 1,
        explanation = "Mikaela Shiffrin ist die erfolgreichste alpine Skifahrerin der Geschichte mit ueber 97 Weltcup-Einzelsiegen (Stand 2024) – mehr als jeder andere Skifahrer, Frau oder Mann. Sie hat ausserdem drei olympische Goldmedaillen und mehrere Weltmeistertitel gewonnen.",
        difficulty = 2,
        funFact = "Shiffrin brach 2023 den Gesamtrekord ihres Idols Ingemar Stenmark (86 Siege) und setzt ihn weiter fort. Sie ist besonders stark im Slalom und Riesenslalom, faehrt aber auch Speed-Disziplinen auf hohem Niveau."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr durften Frauen erstmals beim Boston-Marathon offiziell starten?",
        answerA = "1966",
        answerB = "1972",
        answerC = "1960",
        answerD = "1980",
        correctAnswer = 1,
        explanation = "Frauen durften beim Boston-Marathon erst 1972 offiziell starten. Zuvor liefen Frauen inoffiziell mit – Kathrine Switzer lief 1967 als erste Frau mit einer offiziellen Startnummer (unter falschem Namen) und wurde von Rennorganisatoren vom Kurs gestoben.",
        difficulty = 2,
        funFact = "Die Szene von 1967, als ein Offizieller versuchte, Kathrine Switzer (Startnummer 261) vom Kurs zu draengen, wurde fotografiert und ging um die Welt. Ihr Freund Thomas Miller stiess den Offiziellen weg. Switzer finishte das Rennen – und kaempfte spaeter erfolgreich fuer die offizielle Zulassung von Frauen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Fussballspielerin gewann viermal die FIFA-Auszeichnung 'Weltfussballerin des Jahres'?",
        answerA = "Marta (Brasilien)",
        answerB = "Aitana Bonmati (Spanien)",
        answerC = "Birgit Prinz (Deutschland)",
        answerD = "Sam Kerr (Australien)",
        correctAnswer = 0,
        explanation = "Marta Vieira da Silva aus Brasilien wurde sechsmal zur FIFA-Weltfussballerin des Jahres gewaehlt (2006–2010, 2018) – mehr als jede andere Spielerin. Sie gilt als groesste Fussballerin der Geschichte.",
        difficulty = 2,
        funFact = "Marta ist dafuer bekannt, bei jeder Frauen-WM (2003–2023) mindestens ein Tor erzielt zu haben – ein einzigartiger Rekord. Ihr Zitat nach einem verlorenen WM-Spiel 2019 – 'Cry now, but keep playing' – wurde zur Inspirationsquelle fuer Millionen Maedchen weltweit."
    ),

    // --- E-Sport und Gaming (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Was ist die groesste E-Sport-Liga der Welt gemessen an Preisgeld und Zuschauerzahlen?",
        answerA = "League of Legends World Championship",
        answerB = "The International (Dota 2)",
        answerC = "ESL One Major Series (CS2)",
        answerD = "Fortnite World Cup",
        correctAnswer = 1,
        explanation = "The International ist das weltgroesste Dota-2-Turnier und hat historisch die hoechsten E-Sport-Preisgelder ausgeschuettet – zeitweise ueber 40 Millionen US-Dollar. Es wird jaehrlich von Valve veranstaltet und gilt als das Superbowl des E-Sports.",
        difficulty = 2,
        funFact = "Das Preisgeld bei The International wird durch ein Crowdfunding-System (Battle Pass) zusammengetragen – Millionen von Spielern kaufen ein spezielles Ingame-Item und ein Teil fliesst ins Turniersprisegeld. Das macht The International finanziell einzigartig in der Esport-Welt."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr wurden E-Sport-Disziplinen erstmals als Demonstrationswettbewerbe bei den Asian Games anerkannt?",
        answerA = "2010 in Guangzhou",
        answerB = "2018 in Jakarta",
        answerC = "2014 in Incheon",
        answerD = "2022 in Hangzhou",
        correctAnswer = 1,
        explanation = "E-Sports wurden 2018 bei den Asian Games in Jakarta, Indonesien, als offizielle Demonstrationswettbewerbe eingefuehrt. Bei den Asian Games 2022 in Hangzhou, China, waren E-Sports erstmals als offizieller Medaillenwettkampf dabei.",
        difficulty = 2,
        funFact = "Bei den Asian Games 2022 waren Spiele wie League of Legends, Dota 2, FIFA, Arena of Valor und andere Teil des offiziellen Medaillenprogramms. Suedkorea und China dominieren den E-Sport-Wettkampf in Asien."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist die 'Koreanische Dominanz' im E-Sport und welche Spiele sind besonders betroffen?",
        answerA = "Suedkoreaner dominieren vor allem Ego-Shooter wie CS2 und Valorant",
        answerB = "Suedkorea dominiert besonders in StarCraft und League of Legends dank professioneller Infrastruktur seit den 1990ern",
        answerC = "Nordkorea stellt die meisten E-Sport-Champions in Strategie-Spielen",
        answerD = "Korea dominiert nur den mobilen E-Sport-Bereich",
        correctAnswer = 1,
        explanation = "Suedkorea hat seit Ende der 1990er eine professionelle E-Sport-Struktur aufgebaut – mit TV-Kanaelen nur fuer StarCraft, staatlicher Forderung, Ligen und Akademien. Diese Infrastruktur fuehrt bis heute zu ueberproportionalem Erfolg in Spielen wie StarCraft, League of Legends und anderen.",
        difficulty = 2,
        funFact = "In Suedkorea sind Profi-Gamer Beruehmtheiten mit Fan-Clubs und Markenwerbung wie traditionelle Sportler. Die OGN (OnGameNet) sendete ab 2000 taeglich StarCraft-Partien live – ein weltweit einzigartiges Phaenomen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'International Olympic Committee's E-Sport-Strategie' (Stand 2024) bezueglich E-Sports?",
        answerA = "E-Sports werden vollstaendig aus Olympia ausgeschlossen – keine Ausnahmen",
        answerB = "Das IOC plant eigene Olympische E-Sport-Spiele, die erste Ausgabe findet 2025 statt",
        answerC = "E-Sports werden ab 2028 in Los Angeles ins volle Olympia-Programm aufgenommen",
        answerD = "E-Sports koennen nur als Demonstrationssportarten bei Olympia teilnehmen",
        correctAnswer = 1,
        explanation = "Das IOC kuendigte die ersten 'Olympic Esports Games' an, die 2025 stattfinden sollen – als eigenstaendige Veranstaltung neben den traditionellen Olympischen Spielen. Das IOC moechte E-Sports foerdern, aber vorerst getrennt von den klassischen Olympischen Spielen halten.",
        difficulty = 2,
        funFact = "Die ersten 'Olympic Esports Games' sollen in Saudi-Arabien stattfinden – ein Land, das massiv in E-Sports investiert. Dies loeste erneut eine Debatte ueber Sportswashing aus."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Spiel war das erste, das ein Preisgeld von ueber 1 Million Dollar bei einem einzelnen E-Sport-Turnier ausschuettete?",
        answerA = "Counter-Strike: Global Offensive",
        answerB = "Quake III Arena bei den Cyberathlete Professional League-Finals",
        answerC = "StarCraft II",
        answerD = "Dota 2 bei The International 2011",
        correctAnswer = 3,
        explanation = "The International 2011 (Dota 2) war das erste E-Sport-Turnier mit einem Preisgeld von 1 Million US-Dollar. Das Preisgeld von Valve finanzierte das Turnier komplett – ein Meilenstein in der Geschichte des E-Sports, der die Branche legitimierte.",
        difficulty = 2,
        funFact = "Vor The International 2011 galten 100.000 Dollar schon als aussergewoehnlich hohes E-Sport-Preisgeld. Die Ankuendigung von 1 Million Dollar durch Valve-Chef Gabe Newell auf der Gamescom 2011 galt als Schock und Wendepunkt fuer professionellen Gaming."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Esports Integrity Commission' (ESIC) und warum wurde sie gegruendet?",
        answerA = "Eine Behoerde, die Spiellizenzen fuer E-Sport-Profis ausstellt",
        answerB = "Eine unabhaengige Organisation zur Bekaempfung von Match-Fixing und Betrug im E-Sport",
        answerC = "Ein Organ das Altersfreigaben fuer E-Sport-Spiele vergibt",
        answerD = "Eine Marketing-Organisation fuer E-Sport-Sponsoring",
        correctAnswer = 1,
        explanation = "Die ESIC wurde 2016 gegruendet, um Intergritaetsprobleme im E-Sport zu bekaempfen – besonders Match-Fixing, Doping (ja, es gibt Doping-Tests im E-Sport) und Betrug. Sie arbeitet mit Ligen wie ESL und FACEIT zusammen und hat mehrere Spieler gesperrt.",
        difficulty = 2,
        funFact = "Im CS:GO-Sektor gab es 2020 einen grossen Skandal: ESIC entdeckte, dass Spieler einen 'bug' ausnutzten, der ihnen einen unfairen Vorteil bei Online-Spielen verschaffte. Es wurden 37 Spieler mit unterschiedlich langen Sperren belegt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man im E-Sport unter 'LAN-Turnier' im Gegensatz zu 'Online-Turnier'?",
        answerA = "LAN-Turniere verwenden Glasfaserinternet, Online-Turniere Kupferkabel",
        answerB = "LAN-Turniere finden an einem physischen Ort statt, wo alle Spieler vor Ort spielen; Online-Turniere von zuhause aus",
        answerC = "LAN steht fuer 'Large Arena Network' – Turniere in Stadien ueber 10.000 Personen",
        answerD = "LAN-Turniere sind offizielle Weltmeisterschaften, Online-Turniere nur Qualifikationen",
        correctAnswer = 1,
        explanation = "LAN (Local Area Network) bedeutet, dass alle Spieler physisch an einem Ort sind und ueber ein lokales Netzwerk spielen. Das eliminiert Verbindungsprobleme, Ping-Vorteile und Cheating-Risiken. LAN-Turniere gelten im E-Sport als die offiziellsten und prestigetraechtigsten Wettkaeampfe.",
        difficulty = 2,
        funFact = "Waehrend der COVID-19-Pandemie wurden fast alle E-Sport-Ligen auf Online-Format umgestellt. Dies fuehrte zu neuen Problemen: unterschiedliche Ping-Zeiten durch geographische Lage gaben einigen Spielern messbaren Vorteil – was zu heftigen Debatten ueber Fairness fuehrte."
    ),

    // --- Sport und Olympische Ringe (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Was symbolisieren die fuenf Olympischen Ringe?",
        answerA = "Die fuenf Kontinente der Welt, verbunden durch den olympischen Gedanken",
        answerB = "Die fuenf Gruendervaetern des IOC",
        answerC = "Die fuenf Sportarten des antiken Pentathlon",
        answerD = "Die fuenf Werte des Olympismus: Exzellenz, Respekt, Freundschaft, Einheit, Fairness",
        correctAnswer = 0,
        explanation = "Die fuenf Olympischen Ringe stehen fuer die fuenf bewohnten Kontinente der Welt (Afrika, Amerika, Asien, Europa, Ozeanien), die durch den olympischen Gedanken verbunden sind. Die Farben (Blau, Gelb, Schwarz, Gruen, Rot) wurden gewaehlt, weil jede Nationalflagge mindestens eine dieser Farben enthaelt.",
        difficulty = 2,
        funFact = "Das Olympische Ringe-Symbol wurde 1913 von Pierre de Coubertin entworfen und 1920 in Antwerpen erstmals bei den Olympischen Spielen gezeigt. Die weit verbreitete Annahme, jede Farbe stehe fuer einen Kontinent, ist eine Vereinfachung – Coubertin wies nur auf die Nationalflaggen hin."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lautet der vollstaendige olympische Eid, den Athleten und Richter ablegen?",
        answerA = "Ich schwore, im Namen aller Athleten, im olympischen Geist zu kaeampfen und die Regeln zu befolgen",
        answerB = "Im Namen aller Wettkaeampfer nehme ich teil und verpflichte mich, alle Sportler zu respektieren",
        answerC = "Wir schworen, am olympischen Wettkampf im Geist des Sports teilzunehmen, die Regeln zu achten und auf Doping zu verzichten",
        answerD = "Ich werde in dieser Stunde nicht aufgeben und meiner Nation Ehre machen",
        correctAnswer = 2,
        explanation = "Der olympische Eid lautet sinngemaeass: 'Im Namen aller Wettkaeampfer nehmen wir teil und verpflichten uns, die Regeln, die diese Spiele regeln, zu achten, und uns daran zu halten, und am wahren Geist der Sportlichkeit teilzunehmen, fuer den Ruhm des Sports und die Ehre unserer Teams.' Er wird bei der Eroeffnungsfeier von einem Athleten aus dem Gastgeberland gesprochen.",
        difficulty = 2,
        funFact = "Der olympische Eid wurde 1920 in Antwerpen eingefuehrt. Seitdem wurde der Text mehrfach modifiziert. Seit 1968 legen auch Kampfrichter einen eigenen Eid ab, und seit 2012 sprechen Athlet und Kampfrichter den Eid gemeinsam."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Olympic Truce' (Olympischer Waffenstillstand) und wie lange gilt er?",
        answerA = "Ein informeller Appell an Nationen, waehrend der Spiele keine Kriege zu fuehren",
        answerB = "Eine voelkerrechtlich bindende UN-Resolution, die Kriege fuer 30 Tage stoppt",
        answerC = "Ein Vertrag zwischen Gastgebernation und allen teilnehmenden Laendern ueber waffenfreie Zonen",
        answerD = "Eine wirtschaftliche Sanktion fuer Laender, die waehrend Olympia Konflikte fuehren",
        correctAnswer = 0,
        explanation = "Der Olympic Truce (Olympischer Waffenstillstand) ist ein seit 1992 von der UN verabschiedeter Appell, der Nationen bittet, waehrend der Olympischen Spiele (7 Tage vor Eroeffnung bis 7 Tage nach Abschluss) Kriege zu beenden. Er ist nicht bindend und wird oft ignoriert.",
        difficulty = 2,
        funFact = "Der olympische Waffenstillstand ('Ekecheiria') stammt aus der Antike: Waehrend der Spiele von Olympia (ca. 776 v.Chr. bis 393 n.Chr.) durften Kriege ruhen. Im Unterschied zur Antike ist der moderne Waffenstillstand jedoch rechtlich nicht durchsetzbar."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Nationen nahmen an den ersten modernen Olympischen Sommerspielen 1896 in Athen teil?",
        answerA = "6 Nationen",
        answerB = "14 Nationen",
        answerC = "27 Nationen",
        answerD = "33 Nationen",
        correctAnswer = 1,
        explanation = "An den ersten modernen Olympischen Sommerspielen 1896 in Athen nahmen 14 Nationen mit insgesamt 241 Athleten teil – ausschliesslich Maenner. Die meisten Athleten kamen aus Griechenland. Der US-Amerikaner James Connolly gewann das erste olympische Gold (Dreisprung).",
        difficulty = 2,
        funFact = "Viele Athleten bei den Spielen 1896 waren Touristen oder Sportler, die zufaellig in Athen waren. Einige reisten auf eigene Kosten an – die Laender schickten keine offiziellen Delegationen im modernen Sinne."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land hat in der Geschichte die meisten Olympia-Goldmedaillen gewonnen (Sommer und Winter)?",
        answerA = "Sowjetunion",
        answerB = "USA",
        answerC = "Deutschland (gesamt, DDR+BRD)",
        answerD = "China",
        correctAnswer = 1,
        explanation = "Die USA fuehren die ewige Medaillenwertung der Olympischen Sommerspiele mit Abstand an (ueber 1.000 Goldmedaillen). Auch bei den Winterspielen sind die USA stark. Die Sowjetunion (als Rechtsnachfolger Russland) ist ebenfalls fuehrend, aber insgesamt liegen die USA vor allen anderen Nationen.",
        difficulty = 2,
        funFact = "Wenn man DDR und BRD zusammenzaehlt, hatte 'Deutschland' in manchen Perioden des Kalten Krieges die groesste Medaillendichte. Die DDR war besonders bei Winterspielen und Leichtathletik bemerkenswert erfolgreich – dank staatlich unterstuetzter Doping-Programme, wie spaeter belegt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was passierte bei den Olympischen Spielen 1980 in Moskau und 1984 in Los Angeles?",
        answerA = "Beide Spiele wurden wegen Terroranschlaegen abgebrochen",
        answerB = "Es gab gegenseitige Boykotte: 1980 boykottierten USA/Westlaender, 1984 boykottierte der Ostblock",
        answerC = "Erstmals durften Profisportler teilnehmen, was zu politischen Konflikten fuehrte",
        answerD = "Beide Ausrichtungen wurden aus finanziellen Gruenden stark gekuerzt",
        correctAnswer = 1,
        explanation = "1980 boykottierten die USA und ca. 65 andere Laender die Olympischen Spiele in Moskau als Protest gegen die sowjetische Invasion in Afghanistan. 1984 antworteten die Sowjetunion und Ostblocklaender mit einem Gegenboykott der Spiele in Los Angeles.",
        difficulty = 2,
        funFact = "Der Olympia-Boykott 1980 traf viele Sportler brutal: Sie hatten Jahre trainiert und durften nicht starten. Einige Laender – darunter Grossbritannien und Frankreich – liessen Athleten unter olympischer Fahne (nicht nationaler) antreten, um Olympia nicht vollstaendig zu boykottieren."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Rule 40' des IOC und welche Auswirkungen hat es auf Olympia-Athleten?",
        answerA = "Eine Altersgrenze, die Athleten unter 16 Jahren von der Olympia-Teilnahme ausschliesst",
        answerB = "Eine Regelung, die Athleten waehrend Olympia verbietet, Werbung fuer nicht-olympische Sponsoren zu machen",
        answerC = "Die maximale Anzahl von Athleten, die ein Land pro Disziplin entsenden darf",
        answerD = "Die Regel, nach der Doping-Sperren rueckwirkend auf Medaillen angewendet werden",
        correctAnswer = 1,
        explanation = "Rule 40 des IOC beschraenkt Athleten, Trainer und Offiziellen waehrend der Olympischen Spiele von Werbemassnahmen fuer Marken, die keine offiziellen olympischen Sponsoren sind. Dies soll den Wert der offiziellen Sponsorschaft schuetzen, wurde aber oft als unfair gegenueber Athleten kritisiert.",
        difficulty = 2,
        funFact = "Rule 40 wurde nach Athleten-Protest zuletzt 2019 reformiert und ist nun lockerer. Athleten duerfen ihre persoenlichen Sponsoren waehrend Olympia wieder erwähnen – aber keine explizite Olympia-Werbung machen."
    ),

    // --- Jugend- und Nachwuchssport (6 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Was sind die 'Youth Olympic Games' (YOG) und wie unterscheiden sie sich von regulaeren Olympischen Spielen?",
        answerA = "Spiele ausschliesslich fuer unter 14-Jaehrige, nur im Winter",
        answerB = "Olympische Spiele fuer Jugendliche zwischen 14 und 18 Jahren mit Bildungsprogramm",
        answerC = "Regionale Spiele in jeweils einem Kontinent, nicht international",
        answerD = "Virtuelle E-Sport-Spiele fuer Jugendliche als Vorbereitung fuer Olympia",
        correctAnswer = 1,
        explanation = "Die Youth Olympic Games (Jugend-Olympische Spiele) wurden 2010 eingefuehrt und richten sich an Jugendliche im Alter von 14 bis 18 Jahren. Neben Wettkaeampfen gibt es ein Bildungs- und Kulturprogramm, das olympische Werte vermittelt. Sommer-YOG und Winter-YOG wechseln sich ab.",
        difficulty = 2,
        funFact = "Die ersten Sommer-Youth Olympic Games fanden 2010 in Singapur statt. Nadia Comaneci – die Turnlegende, die als erste Turnerin eine 10,0 bei Olympia bekam – war eine der Botschafterinnen der ersten Ausgabe."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'DFB-Nachwuchsleistungssport' und welches Programm wurde nach der WM 2006 lanciert?",
        answerA = "Ein Trainingsprogramm nur fuer Torhuetertalen te",
        answerB = "Das DFB-Talentfoerderprogramm mit Stuetzpunkten in allen deutschen Bundeslaendern",
        answerC = "Ein Stipendienprogramm fuer Spieler in der Bundesliga-Reserve",
        answerD = "Eine private Fussball-Akademie in Frankfurt",
        correctAnswer = 1,
        explanation = "Das DFB-Talentfoerderprogramm wurde 2002 gestartet und nach der WM 2006 massiv ausgebaut. Es umfasst ueber 380 DFB-Stuetzpunkte in ganz Deutschland mit rund 15.000 Talenten, die von Scouts und Trainern regelmaessig gesichtet und gefördert werden.",
        difficulty = 2,
        funFact = "Dieses Programm gilt als einer der Gruende fuer Deutschlands WM-Sieg 2014: Viele Titeltraeger – wie Thomas Mueller, Mesut Oezil und Toni Kroos – durchliefen das DFB-Talentfoerdersystem. Die Investition von 2002-2014 kostete ueber 100 Millionen Euro."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Red Bull Salzburg's Talentmodell' und welche Spieler sind daraus hervorgegangen?",
        answerA = "Ein Nachwuchsprogramm fuer Torhueter, das Marc-Andre ter Stegen hervorgebracht hat",
        answerB = "Ein System, das junge Weltklassespieler wie Erling Haaland, Sadio Mane und Naby Keita ausgebildet hat",
        answerC = "Ein oesterreichisches Modell fuer Sportgymnasien mit Integration von Schule und Training",
        answerD = "Das Akademieprogramm des FC Bayern fuer internationale Talente",
        correctAnswer = 1,
        explanation = "Red Bull Salzburg gilt als eines der erfolgreichsten Nachwuchssysteme Europas. Spieler wie Erling Haaland, Sadio Mane, Naby Keita, Dominik Szoboszlai und viele andere wurden hier entdeckt oder entwickelt, bevor sie zu Weltstars wurden.",
        difficulty = 2,
        funFact = "Das Red-Bull-Netzwerk (Salzburg, Leipzig, New York, Belo Horizonte) transferiert Spieler systematisch zwischen den Klubs – junge Talente starten in Salzburg, Topspieler wechseln nach Leipzig. Kritiker sprechen von einem 'Durchlauferhitzer', Befuerworter von brillantem Scouting."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Jugendsportlerprogramm' der deutschen Nationalen Anti-Doping-Agentur (NADA)?",
        answerA = "Eine Schul-Dopingtestreihe fuer Sportschueler ab 12 Jahren",
        answerB = "Ein Aufklaerungsprogramm, das junge Athleten ueber Doping-Risiken und sauberen Sport informiert",
        answerC = "Ein sportliches Foerderprogramm mit finanzieller Unterstuetzung fuer unter 18-Jaehrige",
        answerD = "Eine Datenbank mit Nachwuchssportlern, die jaehrlich Doping-Tests absolvieren muessen",
        correctAnswer = 1,
        explanation = "Die NADA betreibt umfangreiche Bildungsprogramme fuer junge Athleten – 'Schule ohne Doping', interaktive Workshops und Online-Ressourcen. Ziel ist fruehzeitige Aufklaerung ueber verbotene Substanzen, die Folgen von Doping und die Foerderung von fairem Sport.",
        difficulty = 2,
        funFact = "Deutschland hat mit der NADA seit 2002 eine nationale Anti-Doping-Agentur. Nach dem DDR-Doping-Skandal, bei dem staatliches Doping Zehntausende von Athleten betraf, ist Deutschland besonders sensibel fuer das Thema und investiert stark in Praevention."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart hat mit dem 'Kinderhockey'-Modell in den Niederlanden besondere internationale Ergebnisse erzielt?",
        answerA = "Fussball",
        answerB = "Basketball",
        answerC = "Feldhockey",
        answerD = "Schwimmen",
        correctAnswer = 2,
        explanation = "Die Niederlande haben durch fruehzeitige Foerderung im Feldhockey ein weltdominierendes System entwickelt. Der niederlaendische Feldhockey-Verband (KNHB) investiert massiv in Nachwuchs – die Frauen und Maenner der Niederlande gehoeren seit Jahrzehnten zur Weltspitze und haben zahlreiche Olympia- und WM-Titel gewonnen.",
        difficulty = 2,
        funFact = "In den Niederlanden spielen ueber 300.000 Mitglieder in Hockeyvereinen – bei einer Bevoelkerung von 17 Millionen eine aussergewoehnliche Dichte. Feldhockey ist nach Fussball eine der beliebtesten Mannschaftssportarten in Holland."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das Hauptziel der 'Street-Sport'-Bewegung im Bereich Nachwuchssport?",
        answerA = "Sportveranstaltungen auf Stadtstrassen fuer Erwachsene zu organisieren",
        answerB = "Junge Menschen aus benachteiligten Stadtvierteln ohne teure Ausstattung fuer Bewegung und Sport zu begeistern",
        answerC = "Skateboarding und Parkour als E-Sport zu etablieren",
        answerD = "Strassenrennen und Radfahren in Staedten gesetzlich zu ermoglichen",
        correctAnswer = 1,
        explanation = "Street-Sport-Programme zielen darauf ab, Kinder und Jugendliche in urbanem Umfeld – oft in sozial benachteiligten Vierteln – durch niedrigschwellige Sportangebote zu erreichen. Mobile Sportplaetze, Streetball-Events und kostenlose Kurse sollen Barrieren abbauen.",
        difficulty = 2,
        funFact = "Das IOC hat 'Urban Sport' als Strategie erkannt und neue Disziplinen wie Skateboarden, Sport Climbing und Breaking (Breakdance) ins Olympia-Programm aufgenommen – Sportarten, die in staedtischem Umfeld entstanden sind und besonders junge Menschen ansprechen."
    ),

)
