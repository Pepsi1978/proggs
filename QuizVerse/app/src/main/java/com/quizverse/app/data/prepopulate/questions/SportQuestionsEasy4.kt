package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsEasy4(): List<Question> = listOf(

    // ─── SPORTARTEN MIT BALL (8) ───────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie viele Spieler stehen beim American Football auf dem Feld pro Mannschaft?",
        answerA = "9",
        answerB = "11",
        answerC = "13",
        answerD = "15",
        correctAnswer = 1,
        explanation = "Beim American Football spielen 11 Spieler pro Mannschaft gleichzeitig auf dem Feld – Offense und Defense wechseln sich ab.",
        difficulty = 1,
        funFact = "Der Ball im American Football hat eine ovale Form und wird auch 'Pigskin' (Schweinsleder) genannt – obwohl er heute aus Rindsleder besteht."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Try' beim Rugby?",
        answerA = "Ein Freistoss",
        answerB = "Das Dribbeln mit dem Ball",
        answerC = "Das Ablegen des Balls hinter der gegnerischen Mallinie fuer Punkte",
        answerD = "Ein fehlgeschlagener Pass",
        correctAnswer = 2,
        explanation = "Ein Try beim Rugby zaehlt 5 Punkte und wird erzielt, indem ein Spieler den Ball hinter der Torauslinie des Gegners niederdrueckt.",
        difficulty = 1,
        funFact = "Rugby bekam seinen Namen von der Rugby School in England, wo das Spiel laut Legende 1823 entstanden ist."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart wird auf einem Feld mit 4 Bases gespielt?",
        answerA = "Cricket",
        answerB = "Softball",
        answerC = "Baseball",
        answerD = "Rounders",
        correctAnswer = 2,
        explanation = "Baseball wird auf einem Diamantfeld mit 4 Bases gespielt – Home Plate, First Base, Second Base und Third Base.",
        difficulty = 1,
        funFact = "Baseball gilt als Nationalsport der USA und wird dort 'America's Pastime' (Amerikas Lieblingszeitvertreib) genannt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Farbe hat ein offizieller Tennisball?",
        answerA = "Weiss",
        answerB = "Orange",
        answerC = "Gelb-Gruen (optic yellow)",
        answerD = "Hellblau",
        correctAnswer = 2,
        explanation = "Offizielle Tennisbaelle sind gelb-gruen (optic yellow) gefaerbt – diese Farbe wurde 1972 eingefuehrt fuer bessere Sichtbarkeit im TV.",
        difficulty = 1,
        funFact = "Fruehe Tennisbaelle waren weiss oder schwarz – je nach Hintergrund des Platzes. Wimbledon nutzte weisse Baelle bis 1986."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Punkte zaehlt ein Freiwurf beim Basketball?",
        answerA = "1 Punkt",
        answerB = "2 Punkte",
        answerC = "3 Punkte",
        answerD = "Kein Punkt",
        correctAnswer = 0,
        explanation = "Ein verwandelter Freiwurf (Freithrow) zaehlt im Basketball genau 1 Punkt – er wird nach Fouls zugesprochen.",
        difficulty = 1,
        funFact = "Die Dreipunktelinie im Basketball wurde 1979 in der NBA eingefuehrt – seitdem ist das Spiel taktisch revolutioniert worden."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man die Linie in der Mitte eines Fussballfeldes?",
        answerA = "Mittellinie",
        answerB = "Halbzeitlinie",
        answerC = "Trennlinie",
        answerD = "Feldlinie",
        correctAnswer = 0,
        explanation = "Die Mittellinie teilt das Fussballfeld in zwei Haelften – beim Anstoss muss der Ball auf dieser Linie platziert werden.",
        difficulty = 1,
        funFact = "Der Anstosskreis um die Mittellinie hat einen Radius von 9,15 Metern – gegnerische Spieler muessen beim Anstoss diesen Kreis verlassen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Sportart nutzt einen gefiederten Ball und ein Netz?",
        answerA = "Squash",
        answerB = "Badminton",
        answerC = "Tischtennis",
        answerD = "Racquetball",
        correctAnswer = 1,
        explanation = "Badminton wird mit einem Shuttlecock (Federball) und einem Netz gespielt – der Shuttlecock darf den Boden nicht beruehren.",
        difficulty = 1,
        funFact = "Badminton hat seinen Namen vom Badminton House in England, wo das Spiel im 19. Jahrhundert populaer wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der Unterschied zwischen Futsal und Fussball?",
        answerA = "Futsal wird mit 5 Spielern in der Halle auf hartem Boden gespielt",
        answerB = "Futsal ist Fussball auf Sand",
        answerC = "Futsal nutzt einen groesseren Ball",
        answerD = "Futsal hat keine Torhüter",
        correctAnswer = 0,
        explanation = "Futsal ist die Hallenversion des Fussballs: 5 Spieler pro Team, kein Feld und ein kleinerer, schwerer Ball auf hartem Untergrund.",
        difficulty = 1,
        funFact = "Futsal wurde in Uruguay entwickelt und hat seinen Namen vom Portugiesischen 'futebol de salao' (Saalfussball)."
    ),

    // ─── WASSERSPORT-BASICS (7) ────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was ist Synchronschwimmen?",
        answerA = "Schwimmen mit exakt gleicher Zeitmessung",
        answerB = "Kunstvolle Wasserchoreografie, die von Gruppen zur Musik ausgefuehrt wird",
        answerC = "Gleichzeitiger Start mehrerer Schwimmer",
        answerD = "Eine Staffeldisziplin mit 4 x 4 Schwimmern",
        correctAnswer = 1,
        explanation = "Synchronschwimmen (heute: Artistic Swimming) ist eine Wassersportart, bei der Teams kunstvolle Figuren und Choreografien zur Musik vorfuehren.",
        difficulty = 1,
        funFact = "Beim Synchronschwimmen duerfen die Beine ueber Wasser sein – Benotung erfolgt nach Synchronizitaet, Technik und kuenstlerischem Ausdruck."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Wasserball?",
        answerA = "Eine Ballsportart im Wasser fuer zwei Teams mit je 7 Spielern",
        answerB = "Unterwasserfussball",
        answerC = "Volleyball im Schwimmbecken",
        answerD = "Eine olympische Tauchdisziplin",
        correctAnswer = 0,
        explanation = "Wasserball wird mit 7 Spielern pro Team im Schwimmbecken gespielt – Ziel ist es, den Ball oefter ins gegnerische Tor zu werfen.",
        difficulty = 1,
        funFact = "Wasserball ist eine der aeltesten olympischen Mannschaftssportarten – es war bereits bei den Spielen 1900 in Paris dabei."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Kajak-Slalom?",
        answerA = "Kajakfahren auf dem offenen Meer",
        answerB = "Schnelles Paddeln durch aufgehaengte Tore auf Wildwasser",
        answerC = "Kajakrennen auf ruhigem Wasser",
        answerD = "Kajakfahren im Schnee",
        correctAnswer = 1,
        explanation = "Beim Kajak-Slalom paddeln Athleten im Kajak durch haengende Tore auf kuenstlichem oder natuerlichem Wildwasser – so schnell wie moeglich.",
        difficulty = 1,
        funFact = "Deutschland ist im Kajak-Slalom eine der erfolgreichsten Nationen – unter anderem durch Sven Luding und Thomas Schmidt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Triathlon?",
        answerA = "Eine Sportart mit Schwimmen, Radfahren und Laufen nacheinander",
        answerB = "Ein Dreikampf aus Turnen, Boxen und Schwimmen",
        answerC = "Ein Mannschaftswettbewerb aus drei Laufdisziplinen",
        answerD = "Eine Ballsportart mit drei Spielphasen",
        correctAnswer = 0,
        explanation = "Triathlon kombiniert Schwimmen, Radfahren und Laufen – die Reihenfolge ist immer gleich, die Distanzen variieren je nach Format.",
        difficulty = 1,
        funFact = "Der beruemteste Triathlon ist der Ironman Hawaii: 3,86 km Schwimmen, 180,25 km Radfahren und 42,2 km Laufen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Tauchen als Wettkampfsport?",
        answerA = "Schnelles Tauchen auf Zeit",
        answerB = "Kunstvolle Spruenge vom Sprungbrett oder Turm ins Wasser",
        answerC = "Unterwasserschiessen auf Ziele",
        answerD = "Langes Luftanhalten unter Wasser",
        correctAnswer = 1,
        explanation = "Wettkampftauchen (Kunstspringen) bewertet kunstvolle Spruenge vom 3-Meter-Brett oder 10-Meter-Turm – nach Ablauffigur, Ausführung und Eintauchen.",
        difficulty = 1,
        funFact = "Beim Synchronspringen springen zwei Athleten gleichzeitig vom gleichen Brett – sie werden nach Synchronizitaet und Ausfuehrung bewertet."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Drachenbootrennen?",
        answerA = "Ein Wasserrennen mit langen Booten, die von vielen Paddlern angetrieben werden",
        answerB = "Ein Segelsport mit drachenfoermigen Segeln",
        answerC = "Eine asiatische Kampfsportart im Wasser",
        answerD = "Ein Wasserski-Wettbewerb",
        correctAnswer = 0,
        explanation = "Beim Drachenbootrennen paddeln bis zu 20 Athleten synchron in einem langen Boot – der Rhythmus wird von einem Trommler vorgegeben.",
        difficulty = 1,
        funFact = "Drachenbootrennen hat seinen Ursprung in China vor ueber 2.000 Jahren – heute ist es ein weltweiter Freizeitsport mit Millionen Aktiven."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der Unterschied zwischen Kraulschwimmen und Freistilschwimmen?",
        answerA = "Es gibt keinen Unterschied – Kraul ist die schnellste Form des Freistils",
        answerB = "Freistil ist langsamer als Kraul",
        answerC = "Kraul ist nur fuer Profis erlaubt",
        answerD = "Freistil bedeutet Rueckenschwimmen",
        correctAnswer = 0,
        explanation = "Freistil bedeutet, dass jede Schwimmart erlaubt ist. Da Kraul die schnellste Technik ist, wird Freistil fast immer als Kraulstil geschwommen.",
        difficulty = 1,
        funFact = "Die Kraul-Technik stammt von den Ureinwohnern Australiens und Amerikas – sie wurde von Arthur Trudgen im 19. Jahrhundert in Europa bekannt gemacht."
    ),

    // ─── WINTERSPORT-BASICS (7) ────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was ist Curling?",
        answerA = "Eine Eissportart, bei der Steine auf eine Zielscheibe geschossen werden",
        answerB = "Ein Eiskunstlauf-Stil",
        answerC = "Eine Schlittendisziplin auf Eis",
        answerD = "Eishockey auf kleinen Rinks",
        correctAnswer = 0,
        explanation = "Beim Curling werden schwere Granitsteine auf einer Eisbahn in Richtung einer Zielscheibe geschossen – Teamkollegen fegen mit Besen, um die Richtung zu beeinflussen.",
        difficulty = 1,
        funFact = "Curling wird oft 'Schach auf Eis' genannt – das taktische Element und die Praezision sind entscheidend fuer den Erfolg."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein Bob?",
        answerA = "Ein Schlitten fuer Wintersportler auf einer Eisrinne",
        answerB = "Ein Sprung auf Skiern",
        answerC = "Eine Kurztechnik beim Eislaufen",
        answerD = "Ein Eisstock fuer Curling",
        correctAnswer = 0,
        explanation = "Der Bob ist ein gelenkter Stahlschlitten, mit dem Teams von 2 oder 4 Personen durch eine Eisrinne rasen – Spitzengeschwindigkeiten ueber 140 km/h.",
        difficulty = 1,
        funFact = "Die laengste und berueckteste Bobbahn der Welt ist die Streamline-Strecke in Whistler (Kanada) mit 1.450 Metern Laenge."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Eiskunstlaufen?",
        answerA = "Schnellstmoegliches Laufen auf Eis",
        answerB = "Kunstvolle Figuren und Spruenge auf Schlittschuhen mit Musik",
        answerC = "Balanceuebungen auf einem Eisfeld",
        answerD = "Eishockey ohne Schlaeger",
        correctAnswer = 1,
        explanation = "Eiskunstlaufen verbindet akrobatische Spruenge, Pirouetten und Schrittfolgen auf Schlittschuhen – es wird nach technischem und kuenstlerischem Wert bewertet.",
        difficulty = 1,
        funFact = "Sonja Henie aus Norwegen gewann dreimal olympisches Gold (1928, 1932, 1936) und wurde spaeter ein Hollywood-Filmstar."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Skeleton?",
        answerA = "Ein Schlittenwettbewerb auf Eis, bei dem man kopfueber auf dem Bauch liegt",
        answerB = "Eine Knochen-Anatomie-Sportart",
        answerC = "Ein Sprung vom Skischanzentisch",
        answerD = "Eine Biathlon-Spezialdisziplin",
        correctAnswer = 0,
        explanation = "Beim Skeleton liegt der Athlet kopfueber und mit dem Bauch nach unten auf einem kleinen Schlitten und rast mit hoher Geschwindigkeit durch eine Eisrinne.",
        difficulty = 1,
        funFact = "Beim Skeleton kann der Athlet kaum lenken – die Richtung wird durch Koerperverlagerung und Schulterdruck gesteuert."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Snowboard-Halfpipe?",
        answerA = "Eine Disziplin, bei der Snowboarder Tricks in einer halbkreisfoermigen Schneerinne zeigen",
        answerB = "Snowboardfahren durch einen Rohrtunnel",
        answerC = "Eine Art Slalom fuer Snowboarder",
        answerD = "Snowboardspringen von einer Schanze",
        correctAnswer = 0,
        explanation = "Bei der Halfpipe fahren Snowboarder durch eine halbrunde Schneerampe und fuehren akrobatische Tricks und Spruenge aus – bewertet nach Stil und Hoehe.",
        difficulty = 1,
        funFact = "Shaun White aus den USA ist der erfolgreichste Halfpipe-Snowboarder der Geschichte – er gewann dreimal olympisches Gold (2006, 2010, 2018)."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Skispringen?",
        answerA = "Springen mit Alpinski den Hang hinunter",
        answerB = "Wettbewerb, bei dem Athleten mit Ski von einer Schanze abspringen und weit fliegen",
        answerC = "Spruenge auf Schnee ohne Ski",
        answerD = "Akrobatische Spruenge auf der Skipiste",
        correctAnswer = 1,
        explanation = "Beim Skispringen fliegen Athleten nach einem Anlauf von einer Schanze so weit wie moeglich – bewertet nach Weite und Stil.",
        difficulty = 1,
        funFact = "Der weiteste Skisprung der Geschichte wurde von Stefan Kraft (Oesterreich) mit 253,5 Metern in Vikersund (Norwegen) aufgestellt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Eisschnelllauf?",
        answerA = "Wettlaufen auf Schlittschuhen ueber festgelegte Distanzen auf einer ovalen Eisbahn",
        answerB = "Kunstlaufen auf Eis",
        answerC = "Hockey ohne Ball auf Eis",
        answerD = "Eisfechten",
        correctAnswer = 0,
        explanation = "Beim Eisschnelllauf rasen Athleten auf Schlittschuhen ueber Distanzen von 500 bis 10.000 Metern auf einer ovalen Eisbahn.",
        difficulty = 1,
        funFact = "Eisschnelllauf-Schlittschuhe haben extrem lange, duenne Klingen – die Schuhe werden 'Klappen' genannt, weil die Ferse beim Abdrucken hochklappt."
    ),

    // ─── SPORT UND GESUNDHEIT (7) ─────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie viele Minuten Sport pro Woche empfiehlt die WHO fuer Erwachsene?",
        answerA = "75 Minuten moderate Aktivitaet",
        answerB = "150 Minuten moderate Aktivitaet",
        answerC = "300 Minuten moderate Aktivitaet",
        answerD = "500 Minuten moderate Aktivitaet",
        correctAnswer = 1,
        explanation = "Die Weltgesundheitsorganisation (WHO) empfiehlt mindestens 150 Minuten moderate koerperliche Aktivitaet pro Woche fuer Erwachsene.",
        difficulty = 1,
        funFact = "Zusaetzlich zur moderaten Aktivitaet empfiehlt die WHO zweimal pro Woche Krafttraining fuer die groesseren Muskelgruppen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Aufwaermen' vor dem Sport?",
        answerA = "Das Duschen vor dem Training",
        answerB = "Leichte Bewegungsuebungen zur Vorbereitung des Koerpers auf Belastung",
        answerC = "Das Anziehen der Sportkleidung",
        answerD = "Das Trinken von warmem Tee vor dem Sport",
        correctAnswer = 1,
        explanation = "Aufwaermen bedeutet, den Koerper durch leichte Aktivitaet auf intensivere Belastung vorzubereiten – Muskeln und Gelenke werden durchblutet und beweglicher.",
        difficulty = 1,
        funFact = "Ein richtiges Aufwaermen kann die Sportleistung um bis zu 20 % steigern und das Verletzungsrisiko erheblich senken."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Dehnen' (Stretching) beim Sport?",
        answerA = "Das intensive Laufen am Trainingsende",
        answerB = "Uebungen zur Verbesserung der Beweglichkeit und Flexibilitaet der Muskeln",
        answerC = "Eine Massagetechnik",
        answerD = "Das Ausruhen nach dem Sport",
        correctAnswer = 1,
        explanation = "Dehnen (Stretching) sind gezielte Uebungen, die Muskeln und Sehnen verlangern – fuer mehr Beweglichkeit, Entspannung und Verletzungspraevention.",
        difficulty = 1,
        funFact = "Statisches Dehnen vor intensivem Sport kann die Leistung kurzfristig senken – dynamisches Aufwaermen ist daher besser als Vorbereitung."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter 'Pulsmessung' beim Sport?",
        answerA = "Das Zaehlen der Herzschlaege pro Minute zur Kontrolle der Trainingsintensitaet",
        answerB = "Die Messung des Blutdrucks",
        answerC = "Das Zaehlen der Atemzuege",
        answerD = "Eine Blutuntersuchung beim Arzt",
        correctAnswer = 0,
        explanation = "Der Puls (Herzfrequenz) zeigt, wie viele Male das Herz pro Minute schlaegt – Sportler nutzen ihn zur Steuerung der Trainingsintensitaet.",
        difficulty = 1,
        funFact = "Der Ruhepuls eines Spitzensportlers kann unter 40 Schlaegen pro Minute liegen – beim Untrainierten sind es ca. 60-80."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Ausdauersport?",
        answerA = "Sport mit sehr kurzer, intensiver Belastung",
        answerB = "Laengere koerperliche Aktivitaet, die das Herz-Kreislauf-System trainiert",
        answerC = "Sport nur fuer aeltere Menschen",
        answerD = "Krafttraining mit Gewichten",
        correctAnswer = 1,
        explanation = "Ausdauersport ist laengere koerperliche Belastung wie Laufen, Radfahren oder Schwimmen – er staerkt Herz, Lunge und die Gesamtausdauer.",
        difficulty = 1,
        funFact = "Regelmassiger Ausdauersport senkt das Risiko fuer Herzerkrankungen, Diabetes und Depressionen nachweislich."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Hydration' beim Sport?",
        answerA = "Das Ersetzen von Wasser und Elektrolyten, die beim Schwitzen verloren gehen",
        answerB = "Eine Massage mit Wasser",
        answerC = "Das Baden nach dem Sport",
        answerD = "Eine Entspannungstechnik",
        correctAnswer = 0,
        explanation = "Hydration bezeichnet die ausreichende Fluessigkeitszufuhr vor, waehrend und nach dem Sport – Dehydrierung verschlechtert Leistung und Gesundheit erheblich.",
        difficulty = 1,
        funFact = "Schon ein Fluessigkeitsverlust von 2 % des Koerpergewichts kann die sportliche Leistung um bis zu 10 % senken."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist Krafttraining?",
        answerA = "Training mit dem Ziel, Muskelkraft und Muskelvolumen zu erhoehen",
        answerB = "Mentales Training fuer Sportler",
        answerC = "Ein Laufprogramm fuer Kurzstreckler",
        answerD = "Intensives Aerobic",
        correctAnswer = 0,
        explanation = "Beim Krafttraining werden Muskeln gegen Widerstand (Gewichte, eigenes Koerpergewicht) trainiert – es staerkt Muskeln, Knochen und Stoffwechsel.",
        difficulty = 1,
        funFact = "Krafttraining ist auch fuer aeltere Menschen sehr empfehlenswert – es bremst den natuerlichen Muskelabbau (Sarkopenie) ab dem 30. Lebensjahr."
    ),

    // ─── SPORTVEREINE / TEAMS (7) ─────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welcher NBA-Verein gewann die meisten Meisterschaften?",
        answerA = "Chicago Bulls",
        answerB = "Golden State Warriors",
        answerC = "Los Angeles Lakers",
        answerD = "Boston Celtics",
        correctAnswer = 3,
        explanation = "Die Boston Celtics fuehren die ewige NBA-Meisterschaftsliste mit 17 Titeln an – sie und die Los Angeles Lakers sind die Rekordhalter.",
        difficulty = 1,
        funFact = "Die Rivalitaet zwischen den Boston Celtics und den Los Angeles Lakers gilt als groesste Rivalitaet in der NBA-Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Fussballverein traegt den Spitznamen 'Die Koenigsklasse' als Turnierbezeichnung?",
        answerA = "UEFA Champions League",
        answerB = "DFB-Pokal",
        answerC = "Europa League",
        answerD = "Bundesliga",
        correctAnswer = 0,
        explanation = "Die UEFA Champions League wird wegen ihres Prestiges oft als 'Koenigsklasse' bezeichnet – sie ist der wichtigste europaeische Clubwettbewerb.",
        difficulty = 1,
        funFact = "Real Madrid ist mit 15 Champions-League-Titeln der erfolgreichste Verein in der Geschichte dieses Wettbewerbs."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Stadt spielen die New York Yankees?",
        answerA = "Los Angeles",
        answerB = "Chicago",
        answerC = "New York",
        answerD = "Boston",
        correctAnswer = 2,
        explanation = "Die New York Yankees sind eines der beruemtesten Baseball-Teams der Welt und spielen in New York City – im Yankee Stadium in der Bronx.",
        difficulty = 1,
        funFact = "Die Yankees haben mehr World-Series-Titel (27) gewonnen als jedes andere MLB-Team – sie gelten als 'franchise' des amerikanischen Sports."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher deutsche Fussballklub hat die meisten Bundesliga-Meisterschaften gewonnen?",
        answerA = "Borussia Dortmund",
        answerB = "FC Bayern Muenchen",
        answerC = "Borussia Moenchengladbach",
        answerD = "Hamburger SV",
        correctAnswer = 1,
        explanation = "Der FC Bayern Muenchen ist Rekordmeister der Bundesliga mit weit mehr als 30 Meistertiteln – weit vor allen anderen deutschen Vereinen.",
        difficulty = 1,
        funFact = "Der FC Bayern Muenchen gewann von 2013 bis 2023 elf Bundesliga-Meisterschaften in Folge – ein europaweiter Rekord."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist die NFL?",
        answerA = "National Fussball League (Deutschland)",
        answerB = "National Football League (American Football, USA)",
        answerC = "Nordic Football League (Skandinavien)",
        answerD = "National Futsal League (USA)",
        correctAnswer = 1,
        explanation = "Die NFL (National Football League) ist die professionelle American-Football-Liga der USA – sie ist eine der kommerziell erfolgreichsten Sportligen der Welt.",
        difficulty = 1,
        funFact = "Das Endspiel der NFL heisst Super Bowl – es ist die meistgesehene Einzelsportsendung der Welt mit ueber 100 Millionen TV-Zuschauern."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisclub ist fuer das weisse Outfit seiner Spieler bekannt?",
        answerA = "Roland Garros Paris",
        answerB = "Wimbledon (All England Lawn Tennis Club)",
        answerC = "Australian Open Melbourne",
        answerD = "US Open New York",
        correctAnswer = 1,
        explanation = "Der All England Lawn Tennis Club (Wimbledon) schreibt weisse Kleidung vor – eine Tradition, die seit dem 19. Jahrhundert besteht.",
        difficulty = 1,
        funFact = "Die Wimbledon-Kleiderordnung ist so streng, dass bereits ein farbiger Markenaufdruck am Kragen zur Verwarnung fuehren kann."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist die Champions Hockey League (CHL)?",
        answerA = "Der wichtigste europaeische Eishockey-Clubwettbewerb",
        answerB = "Ein Basketballturnier",
        answerC = "Ein internationaler Fussball-Pokal",
        answerD = "Eine amerikanische Hockey-Liga",
        correctAnswer = 0,
        explanation = "Die Champions Hockey League ist der europaeische Club-Eishockeywettbewerb, bei dem die besten Teams aus verschiedenen europaeischen Ligen gegeneinander antreten.",
        difficulty = 1,
        funFact = "Deutsche Eishockeyteams wie der EHC Red Bull Muenchen und Adler Mannheim nehmen regelmaessig an der Champions Hockey League teil."
    ),

    // ─── SPORTREGELN FUER ANFAENGER (7) ───────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Abseits' im Fussball?",
        answerA = "Ein Tor, das nicht anerkannt wird, weil der Ball die Linie nicht ueberschritten hat",
        answerB = "Eine Regel, die ungueltigen Angriffsstellungen von Spielern verhindert",
        answerC = "Ein Freistoss nach Handspiel",
        answerD = "Eine Zeitstrafe fuer Unsportlichkeit",
        correctAnswer = 1,
        explanation = "Abseits liegt vor, wenn ein Angreifer beim Abspiel naeher als der vorletzte Gegenspieler am gegnerischen Tor steht – dann wird der Angriff abgepfiffen.",
        difficulty = 1,
        funFact = "Die Abseitsregel wurde 1925 geaendert – seitdem genuegen zwei Gegenspieler (statt drei) um Abseits zu verhindern, was viel mehr Tore ermoeglichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Was passiert im Fussball bei einem Unentschieden in einem K.o.-Spiel?",
        answerA = "Das Spiel endet ohne Sieger",
        answerB = "Es gibt Verlaengerung und ggf. Elfmeterschiessen",
        answerC = "Der Gastgeber gewinnt automatisch",
        answerD = "Es wird neu angesetzt",
        correctAnswer = 1,
        explanation = "Bei Unentschieden in K.o.-Spielen folgt eine Verlaengerung (2 x 15 Min) – bleibt es unentschieden, entscheidet das Elfmeterschiessen.",
        difficulty = 1,
        funFact = "Das laengste Elfmeterschiessen der WM-Geschichte war 2022: Costa Rica gegen Neuseeland – beide trafen bei den ersten 10 Schuessen, am Ende gewann Costa Rica 11:10."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist eine 'rote Karte' im Fussball?",
        answerA = "Die Auszeichnung fuer den besten Spieler",
        answerB = "Der sofortige Platzverweis fuer einen Spieler",
        answerC = "Ein Signal fuer eine Trinkpause",
        answerD = "Eine Warnung ohne Konsequenz",
        correctAnswer = 1,
        explanation = "Die rote Karte bedeutet Platzverweis – der Spieler muss das Feld sofort verlassen und sein Team spielt dann in Unterzahl weiter.",
        difficulty = 1,
        funFact = "Der erste Platzverweis der WM-Geschichte wurde 1966 in England ausgesprochen – an Antonio Rattin von Argentinien im Spiel gegen England."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lange darf ein Tennisspieler beim Aufschlag brauchen?",
        answerA = "5 Sekunden",
        answerB = "20 Sekunden (im Profitennis nach dem Punkt)",
        answerC = "60 Sekunden",
        answerD = "Es gibt kein Zeitlimit",
        correctAnswer = 1,
        explanation = "Im professionellen Tennis gilt eine Zeitregel von 25 Sekunden zwischen den Punkten – Verzoegerungen koennen als Zeitstrafe gewertet werden.",
        difficulty = 1,
        funFact = "Seit 2018 wird das Zeitlimit im Tennis strenger durchgesetzt – Schiedsrichter haben Uhren und koennen Zeitstrafen aussprechen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist beim Basketball 'doppeltes Dribbeln' (Double Dribble)?",
        answerA = "Zweimal springen, waehrend man dribbled",
        answerB = "Den Ball mit beiden Haenden gleichzeitig oder nach einem Stopp erneut dribbeln",
        answerC = "Zweimal hintereinander schiessen",
        answerD = "Den Ball zweimal den Boden beruehren lassen",
        correctAnswer = 1,
        explanation = "Double Dribble ist ein Regelverstoss: Einen Ball mit beiden Haenden gleichzeitig oder nach dem Stoppen erneut dribbeln ist verboten – der Gegner erhaelt den Ball.",
        difficulty = 1,
        funFact = "Basketball-Profis trainieren Hunderte von Stunden, um Dribbelpausen zu vermeiden – der Ballkontakt ist immer klar geregelt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet 'Love' im Tennis?",
        answerA = "Ein verlorener Satz",
        answerB = "Ein besonders schoener Schlag",
        answerC = "Null Punkte (0)",
        answerD = "Der Matchball",
        correctAnswer = 2,
        explanation = "Im Tennis bedeutet 'Love' null Punkte – der Begriff stammt moeglicherweise vom franzoesischen 'l'oeuf' (das Ei) fuer die Null.",
        difficulty = 1,
        funFact = "Wenn ein Spieler einen Satz 6:0 gewinnt, nennt man das 'Bagel' – weil die Null wie ein Bagel (rundes Brötchen) aussieht."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist beim Volleyball ein 'Lift' (Heben)?",
        answerA = "Ein legaler Angriffschlag",
        answerB = "Ein Regelverstos, bei dem der Ball zu lange auf den Haenden liegt",
        answerC = "Ein Block am Netz",
        answerD = "Das Aufschlagen des Balls",
        correctAnswer = 1,
        explanation = "Ein Lift (oder 'Carry') ist im Volleyball verboten – der Ball darf nicht auf den Haenden liegen bleiben oder mitgeschleppt werden, nur kurz beruehrt werden.",
        difficulty = 1,
        funFact = "Volleyball-Schiedsrichter beobachten Fingerposition und Kontaktdauer sehr genau, um zwischen legalem Spiel und Lift zu unterscheiden."
    ),

    // ─── SPORTARTEN-HERKUNFTSLAENDER (7) ──────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "In welchem Land wurde Basketball erfunden?",
        answerA = "Kanada / USA",
        answerB = "England",
        answerC = "Frankreich",
        answerD = "Brasilien",
        correctAnswer = 0,
        explanation = "Basketball wurde 1891 von dem Kanadier James Naismith in Springfield, Massachusetts (USA) erfunden – als Hallenalternative fuer kalte Wintermonate.",
        difficulty = 1,
        funFact = "Naismith befestigte zwei Pfirsichkoerbe an einer Galerie in 3,05 Meter Hoehe – noch heute ist das die offizielle Korbhoehe."
    ),

    Question(
        categoryId = 6,
        questionText = "Aus welchem Land stammt Sumo-Ringen?",
        answerA = "China",
        answerB = "Japan",
        answerC = "Korea",
        answerD = "Mongolei",
        correctAnswer = 1,
        explanation = "Sumo-Ringen stammt aus Japan und hat eine Jahrhunderte alte Geschichte – es ist die japanische Nationalsportart mit tief verwurzelten Ritualen.",
        difficulty = 1,
        funFact = "Sumo-Kaempfer (Rikishi) haben strenge Ernaehrungs- und Lebensstilregeln – sie koennen ueber 200 kg wiegen und essen spezielle Reintopf-Suppe (Chanko Nabe)."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Land entstand die Sportart Taekwondo?",
        answerA = "China",
        answerB = "Japan",
        answerC = "Korea",
        answerD = "Thailand",
        correctAnswer = 2,
        explanation = "Taekwondo stammt aus Korea und ist seit den Olympischen Spielen 2000 in Sydney eine olympische Kampfsportart.",
        difficulty = 1,
        funFact = "Taekwondo bedeutet auf Koreanisch ungefaehr 'Weg des Fuss- und Faustkampfes' – die Fusskaempfe sind das Herzstuck dieser Sportart."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Land wurde Cricket entwickelt?",
        answerA = "Australien",
        answerB = "Indien",
        answerC = "England",
        answerD = "Suedafrika",
        correctAnswer = 2,
        explanation = "Cricket wurde in England entwickelt – erste schriftliche Erwaehnung stammt aus dem 16. Jahrhundert, die modernen Regeln wurden im 18. Jahrhundert festgelegt.",
        difficulty = 1,
        funFact = "Cricket ist nach Fussball die zweitbeliebteste Sportart der Welt – besonders in Indien, Pakistan, Australien und England extrem populaer."
    ),

    Question(
        categoryId = 6,
        questionText = "Aus welchem Land stammt Judo?",
        answerA = "China",
        answerB = "Korea",
        answerC = "Brasilien",
        answerD = "Japan",
        correctAnswer = 3,
        explanation = "Judo wurde 1882 von Jigoro Kano in Japan entwickelt und ist seit den Olympischen Spielen 1964 in Tokio olympische Disziplin.",
        difficulty = 1,
        funFact = "Judo bedeutet auf Japanisch 'sanfter Weg' – trotzdem erfordert es erhebliche Koerperkraft und technisches Koennen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gilt als Heimat des modernen Marathonlaufs?",
        answerA = "Aethiopien",
        answerB = "Griechenland",
        answerC = "Kenia",
        answerD = "USA",
        correctAnswer = 1,
        explanation = "Der Marathonlauf erinnert an den Lauf des griechischen Boten Pheidippides von Marathon nach Athen im Jahr 490 v. Chr. – daher der Name.",
        difficulty = 1,
        funFact = "Der erste olympische Marathon 1896 in Athen wurde vom Griechen Spyridon Louis gewonnen – er trank Wein und ass Schokolade waehrend des Rennens."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Land wurde Volleyball erfunden?",
        answerA = "Brasilien",
        answerB = "USA",
        answerC = "Japan",
        answerD = "Kuba",
        correctAnswer = 1,
        explanation = "Volleyball wurde 1895 von William G. Morgan in Holyoke, Massachusetts (USA) erfunden – urspruenglich als weniger koerperkontaktreiche Alternative zu Basketball.",
        difficulty = 1,
        funFact = "Morgan nannte das Spiel urspruenglich 'Mintonette' – erst spaeter wurde es wegen des Hin-und-her-Schlagen des Balls 'Volleyball' genannt."
    ),

)
