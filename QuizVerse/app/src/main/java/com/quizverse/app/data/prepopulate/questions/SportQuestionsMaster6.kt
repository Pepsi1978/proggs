package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsMaster6(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) ── 45 questions ──────────────────────────────

    // ── Olympische Spiele 1964-2024 (18) ─────────────────────────────────────

    // Question 1 – Tokyo 1964: first doping control
    Question(
        categoryId = 6,
        questionText = "Bei den Olympischen Spielen 1964 in Tokio wurde erstmals in der olympischen Geschichte eine offizielle Massnahme gegen eine bestimmte Substanzklasse eingefuehrt. Welche Massnahme war das und wen betraf sie?",
        answerA = "Erstmaliges Verbot von Anabolika fuer alle Athleten nach dem Skandal um sowjetische Gewichtheber",
        answerB = "Stichprobenartige Bluttests auf Erythropoetin bei Mittel- und Langstrecklern",
        answerC = "Formales Verbot von Stimulanzien mit Urintests, nachdem der daenische Radfahrer Knud Jensen 1960 bei den Rom-Spielen an Amphetaminen gestorben war",
        answerD = "Einfuehrung von Bluttests auf Wachstumshormon ausschliesslich bei Schwimmern und Gewichthebern",
        correctAnswer = 2,
        explanation = "Nach dem Tod von Knud Jensen bei den Spielen 1960 in Rom, der auf Amphetaminkonsum zurueckgefuehrt wurde, fuehrte das IOC 1964 in Tokio erstmals offizielle Doping-Kontrollen auf Stimulanzien mit Urintests ein. Ein systematisches Testprogramm fuer alle Sportarten begann jedoch erst 1968 in Grenoble und Mexico City.",
        difficulty = 5,
        funFact = "Knud Jensens Tod wurde jahrzehntelang offiziell als Hitzschlag eingestuft. Erst spaetere Untersuchungen belegten den Amphetaminkonsum. Die Wahrheit wurde von daenischen Sporthistorikern erst 2004 vollstaendig dokumentiert."
    ),

    // Question 2 – Mexico City 1968: altitude controversy
    Question(
        categoryId = 6,
        questionText = "Die Olympischen Spiele 1968 in Mexico City auf 2240 Metern Hoehe erzeugten extreme Leistungsunterschiede zwischen Disziplinen. Welcher physiologische Effekt erklaert, warum Bob Beamon seinen Weitsprung-Weltrekord von 8,90 m teilweise der Hoehe verdankte, waehrend 5000-m-Laeufer massiv benachteiligt waren?",
        answerA = "Geringere Luftdichte reduzierte den Luftwiderstand bei Sprints und Wuerfen um ca. 15%, waehrend Ausdauerleistungen wegen 30% weniger verfuegbarem Sauerstoff einbrachen",
        answerB = "Geringere Luftdichte reduzierte den Luftwiderstand bei horizontalen Bewegungen um ca. 2-3% und ermoeglichte laengeren Wurfflug; Ausdauersportarten litten unter reduziertem O2-Partialdruck bei gleichem O2-Gehalt der Luft",
        answerC = "Die reduzierte Schwerkraft auf grosser Hoehe verlaengerte Sprung- und Wurfweiten um durchschnittlich 8%, Ausdauerleistungen blieben weitgehend unveraendert",
        answerD = "Hoehenklima erhoehte die Haematokrit-Werte aller Sportler gleichmaessig, was Sprinter und Ausdauerlaeufer gleichermassen beguenstigte",
        correctAnswer = 1,
        explanation = "In 2240 m Hoehe ist die Luftdichte ca. 75% des Meeresnivaaus, was den Luftwiderstand bei Sprintdisziplinen und ballistischen Wuerfen reduziert. Bei Beamons Sprung schlagte diese aerodynamische Wirkung und der duennere Gegenwind mit geschaetzten 20-25 cm zu Buche. Ausdauersportler litten dagegen unter dem niedrigeren O2-Partialdruck (gleicher 21% O2-Anteil, aber weniger Luftmolekuele pro Atemzug).",
        difficulty = 5,
        funFact = "Bob Beamons Rekord von 8,90 m uebertraf den alten Rekord von Ralph Boston um 55 cm — in einem Sport, wo Verbesserungen typischerweise in Zentimetern gemessen werden. Der Rekord stand 23 Jahre, bis Mike Powell 1991 mit 8,95 m in Tokyo (auf Meereshoehe!) uebertraf."
    ),

    // Question 3 – Montreal 1976: African boycott cause
    Question(
        categoryId = 6,
        questionText = "28 afrikanische Laender boykottierten die Olympischen Spiele 1976 in Montreal. Was war der unmittelbare Ausloeser dieses Boycotts, und welche Sportorganisation hatte zuvor die entscheidende Forderung gestellt?",
        answerA = "Die Aufnahme Suedafrikas in das IOC gegen den Willen der Mehrheit der afrikanischen Nationen, gefordert von der Organisation Afrikanischer Einheit",
        answerB = "Die Zulassung der neuseelaendischen Rugby-Nationalmannschaft (All Blacks) zu Spielen gegen Suedafrika, nachdem das Supreme Council for Sport in Africa verlangte, Neuseeland solle ausgeschlossen werden",
        answerC = "Die Weigerung des IOC, die Republik China (Taiwan) unter dem Namen 'Taiwan' statt 'Republik China' starten zu lassen, was die Afrikanische Union als Unterstuetzung Taiwans wertete",
        answerD = "Der Ausschluss von Rhodesien aus dem IOC trotz Qualifikation, was als antiafrika nische Diskriminierung interpretiert wurde",
        correctAnswer = 1,
        explanation = "Der Supreme Council for Sport in Africa (SCSA) verlangte den Ausschluss Neuseelands, weil die All Blacks kurz zuvor eine Tournee durch Suedafrika unternommen hatten, was gegen das Sportapartheid-Embargo verstiess. Das IOC weigerte sich, da Rugbyverbands-Entscheidungen nicht in olympischer Jurisdiktion lagen. Daraufhin zogen 28 Nationen ihre Teams kurz vor Spielbeginn zurueck.",
        difficulty = 5,
        funFact = "Tanzania war das erste Land, das den Boycott ankuendigte, nur Stunden nach der IOC-Entscheidung. Der Boycott kostete viele qualifizierte afrikanische Athleten ihre einzige Olympia-Chance — darunter die kenianischen Laeufer, die als Favoriten fuer mehrere Goldmedaillen galten."
    ),

    // Question 4 – Moscow 1980: West boycott sports impact
    Question(
        categoryId = 6,
        questionText = "Beim US-gefuehrten Boycott der Moskauer Spiele 1980 nahmen 65-80 Nationen nicht teil (je nach Quelle). Welche olympische Disziplin verlor durch den Boycott ihre starksten Konkurrenten am vollstaendigsten, sodass die verbleibenden Goldmedaillen-Leistungen historisch besonders schwach eingestuft werden?",
        answerA = "Maenner-100-Meter-Sprint, da die karibischen und US-Sprinter fehlten und der Sieger Allan Wells mit 10,25 s gewann",
        answerB = "Gewichtheben, da die gesamte US-amerikanische und westdeutsche Elite fehlte",
        answerC = "Maenner-Schwimmen, da die US-amerikanischen Schwimmer (damals weltweit dominierend) komplett fehlten und osteuropaeische Schwimmer ohne nennenswerte Konkurrenz siegten",
        answerD = "Maenner-Basketabll, da die USA ohne ihren olympischen Kader antraten und Jugoslawien ohne Widerstand gewann",
        correctAnswer = 2,
        explanation = "Das US-Schwimmteam war 1980 das weltbeste und gewann bei jedem Olympia zuvor ueberwaeltigend viele Goldmedaillen. Durch den Boycott dominierten ostdeutsche und sowjetische Schwimmer praktisch konkurrenzlos. Historiker bewerten mehrere Zeiten als 30-50% unter dem damaligen Weltrekordniveau. Auch der Sprint war betroffen, aber weniger drastisch, da einige karibische Nationen trotzdem teilnahmen.",
        difficulty = 5,
        funFact = "Der britische Sprinter Allan Wells gewann in Moskau Gold ueber 100 m mit 10,25 s — einer Zeit, die in der westlichen Welt kaum nationale Meisterschaften gewonnen haette. Wells selbst erkannte spaeter an, dass sein Titel angesichts der Abwesenheit der Weltspitze historisch eingeschraenkt zu bewerten sei."
    ),

    // Question 5 – Seoul 1988: Ben Johnson scandal detail
    Question(
        categoryId = 6,
        questionText = "Ben Johnsons positiver Dopingtest bei den Olympischen Spielen 1988 in Seoul gilt als groesster Dopingskandal der olympischen Geschichte. Welche Substanz wurde nachgewiesen, und welcher biochemische Umstand machte den Nachweis moeglich, obwohl Johnson behauptet hatte, die Substanz sei ihm 'untergeschoben' worden?",
        answerA = "Testosteron — nachgewiesen durch ein unguenstiges Testosteron/Epitestosteron-Verhaeltnis von 14:1; ein Verhaeltnis ueber 6:1 galt als Doping-Beweis",
        answerB = "Stanozolol (ein synthetisches Anabolikum) — nachgewiesen durch spezifische Steroid-Metaboliten im Urin, deren Halbwertszeit zeigt, dass die Einnahme vor dem Rennen stattfand, nicht durch eine Kontamination im Sieges-Drink",
        answerC = "Nandrolon — das IOC-Labor in Seoul fand den 19-Norandrosterons-Metaboliten, der typischerweise bei exogenem Nandrolon-Konsum erscheint",
        answerD = "HGH (humanes Wachstumshormon) — nachgewiesen durch einen damals neuen Isoformtest, der exogenes von endogenem HGH unterschied",
        correctAnswer = 1,
        explanation = "Das IOC-Labor in Seoul wies Stanozolol-Metaboliten nach — spezifisch 16-beta-Hydroxystanozolol, ein stabiler Abbauprodukt mit einer Halbwertszeit von mehreren Tagen. Dies widerlegte die 'Untergeschoben'-Theorie, da der Metabolitenspiegel zeigte, dass die Substanz ueber Tage eingenommen worden war. Johnsons Coach Charlie Francis gestand spaeter das systematische Doping-Programm.",
        difficulty = 5,
        funFact = "Von den acht Finalisten des 100-m-Laufs 1988 in Seoul wurden spaeter sechs mit Doping in Verbindung gebracht — entweder durch positive Tests oder Gestaendnisse. Nur Linford Christie und Calvin Smith blieben weitgehend unverdaechtig, obwohl Christie spaeter noch andere Verfahren hatte."
    ),

    // Question 6 – Barcelona 1992: Dream Team economics
    Question(
        categoryId = 6,
        questionText = "Das US-amerikanische 'Dream Team' bei den Olympischen Spielen 1992 in Barcelona war das erste NBA-All-Star-Team in der olympischen Geschichte. Welche Regel-Aenderung ermoeglichte die Teilnahme, und welcher NBA-Spieler lehnte die Teilnahme aus einem bis heute vieldiskutierten Grund ab?",
        answerA = "Das FIBA-Statut ueber 'Amateure' wurde 1989 gestrichen; Isiah Thomas wurde vom Team ausgeschlossen, da Michael Jordan sich weigerte, mit ihm zu spielen",
        answerB = "Das IOC verabschiedete eine Sonderregel fuer Teamsportarten im Jahr 1990; Charles Barkley verzichtete aus kommerziellen Sponsoring-Konflikten mit dem USOC",
        answerC = "Das IOC-Amateurstatut wurde 1986 aufgehoben; Magic Johnson lehnte zunachst ab, da er kurz zuvor seine HIV-Infektion bekanntgegeben hatte, trat aber dann doch an",
        answerD = "Die NBA-Spielergewerkschaft (NBPA) erzielte 1991 ein Abkommen mit FIBA; Scottie Pippen lehnte ab, da er den Schatten von Michael Jordan nicht mehr ertragen wollte",
        correctAnswer = 0,
        explanation = "Die FIBA strich 1989 das Amateurstatut und erlaubte NBA-Profis erstmals die Olympia-Teilnahme. Isiah Thomas, damals Superstar der Detroit Pistons, wurde bewusst nicht nominiert, weil Michael Jordan erklaerte, er wuerde nicht mit Thomas spielen — eine persoenliche Feindschaft, die aus den brutal harten Playoff-Duellen stammte. Thomas' Ausschluss gilt als eines der groessten sportpolitischen Hinterzimmer-Dramen des olympischen Basketballs.",
        difficulty = 5,
        funFact = "Das Dream Team gewann alle acht Spiele mit einem Durchschnitt von 43,8 Punkten Vorsprung. Im Finale gegen Kroatien siegte Team USA mit 117:85. Kroatiens Toni Kukoc, der spaeter bei den Bulls spielte, bat Jordan und Pippen nach dem Spiel um Autogramme."
    ),

    // Question 7 – Atlanta 1996: Bombing and security
    Question(
        categoryId = 6,
        questionText = "Beim Bombenanschlag auf den Centennial Olympic Park bei den Spielen 1996 in Atlanta starben zwei Menschen. Welcher Sicherheitsmitarbeiter fand die Bombe und wurde danach faelschlicherweise selbst als Verdaechtiger behandelt, und wie endete sein Fall?",
        answerA = "Richard Jewell entdeckte den Rucksack mit der Bombe, rettete durch seine Warnungen vermutlich dutzende Leben, wurde danach vom FBI als Verdaechtiger behandelt und jahrelang von der Presse ruiniert; er wurde 2005 offiziell rehabilitiert",
        answerB = "Eric Rudolph, der spaetere Taefer, arbeitete als Sicherheitsmitarbeiter und meldete die Bombe, bevor er untertauchte; er wurde erst 2003 gefasst",
        answerC = "Mark Fuhrman, bekannt aus dem OJ-Simpson-Prozess, war Sicherheitsberater der Spiele und fand die Bombe, wurde aber wegen seiner belasteten Vergangenheit verdaechtigt",
        answerD = "Johnny Walker, ein privater Sicherheitsdienst-Mitarbeiter, entdeckte den Rucksack, wurde kurz verdaechtigt, aber nach 48 Stunden als unschuldig eingestuft und erhielt eine staatliche Auszeichnung",
        correctAnswer = 0,
        explanation = "Richard Jewell entdeckte den Rucksack, alarmierte die Polizei und half beim Raeumen des Bereichs, wodurch er moeglicherweise Dutzende Leben rettete. Das FBI leckte seinen Namen als Verdaechtigen an die Presse, und er wurde in einem Medientrial zerrissen. Nach 88 Tagen Ermittlungen wurde er offiziell von Verdacht befreit. Eric Rudolph wurde 2003 als tatsaechlicher Taefer gefasst. Jewells Klage gegen NBC News fuehrte zu einem aussergerichtlichen Vergleich.",
        difficulty = 5,
        funFact = "Der Fall Richard Jewell wurde 2019 von Clint Eastwood verfilmt. Jewell starb 2007 im Alter von 44 Jahren, ohne dass der Staat ihn je offiziell rehabilitiert oder entschaedigt hatte. Der Film brachte seinen Fall erneut in die oeffentliche Diskussion."
    ),

    // Question 8 – Sydney 2000: Cathy Freeman ceremony
    Question(
        categoryId = 6,
        questionText = "Bei der Eroeffnungsfeier der Olympischen Spiele 2000 in Sydney entzuendete Cathy Freeman das olympische Feuer in einer historisch bedeutsamen Inszenierung. Welche zwei Symbole vereinigte ihre Fackeltraeger-Rolle, und welche technische Panne fast die Eroeffnungsfeier ruiniert haette?",
        answerA = "Freeman repraesentierte die Einheit von Aborigines und nicht-indigenen Australiern; der Wasserring um die Fackelschale oeffnete sich zuerst nicht vollstaendig, und Freeman stand 3 Minuten lang in einer Wasserpfuetze",
        answerB = "Freeman stand symbolisch fuer die Versoehnung nach der 'Stolen Generations'-Entschuldigung; die Fackel erlosch beim Aufstieg auf die Rampe und musste zweimal neu gezuendet werden",
        answerC = "Freeman vereinigte olympischen Sport und australische Ureinwohner-Kultur; ein hydraulischer Fehler liess das Becken waehrend der Live-Uebertragung fuer 27 Sekunden stecken bleiben, waehrend Freeman wartete",
        answerD = "Freeman repraesentierte erstmals eine Indigene als alleinige Fackeltraegerin; die elektrische Zuendvorrichtung versagte und ein Techniker musste mit einem Handfeuerzeug einspringen",
        correctAnswer = 2,
        explanation = "Cathy Freeman stand symbolisch fuer die Brucke zwischen indigener und nicht-indigener australischer Identitaet, besonders sensibel nach der 'Stolen Generations'-Debatte der 1990er Jahre. Tatsaechlich gab es eine hydraulische Stoerung: Das schwimmende Becken blieb stecken, als Freeman die Flamme entzuendete. Sie stand stehend in der Wasserpfuetze, waehrend Techniker das Problem behoben — weltweit live uebertragen.",
        difficulty = 5,
        funFact = "Cathy Freeman gewann drei Tage spaeter Gold ueber 400 Meter vor Heimpublikum und lief danach mit sowohl der australischen Flagge als auch der Aborigines-Flagge um das Stadion — ein Bild, das zu einem der ikonischsten der olympischen Geschichte wurde."
    ),

    // Question 9 – Athens 2004: Vanderlei de Lima incident
    Question(
        categoryId = 6,
        questionText = "Bei den Olympischen Spielen 2004 in Athen erlitt der brasilianische Marathon-Laeufer Vanderlei de Lima einen Vorfall, der ihm die Goldmedaille kostete. Was geschah genau, welche Auszeichnung erhielt er stattdessen, und wer war der Stoerer?",
        answerA = "Der irische Priester Neil Horan lief auf die Strecke und hielt de Lima fuer mehrere Sekunden auf; de Lima verlor fuenf Sekunden und wurde Dritter; das IOC verlieh ihm den Pierre-de-Coubertin-Preis fuer Fairplay",
        answerB = "Ein griechischer Zuschauer warf eine Plastikflasche auf de Lima; dieser stolperte, verlor das Gleichgewicht und fiel; das IOC ordnete eine Silbermedaille fuer de Lima zusaetzlich zur Bronzemedaille an",
        answerC = "Ein betrunkener Zuschauer lief neben de Lima her und schubste ihn gegen die Absperrung; Italiens Stefano Baldini holte den Rueckstand auf und gewann Gold; de Lima wurde der Fairplay-Preis des IOC verliehen",
        answerD = "Der irische Aktivist Neil Horan stuermte die Strecke und versuchte, de Lima zu stoppen; nach dem Vorfall sank de Limas Tempo und er wurde von zwei Laeufer ueberholt; er erhielt den Pierre-de-Coubertin-Preis",
        correctAnswer = 3,
        explanation = "Der irische Ex-Priester Neil Horan — bekannt fuer bizarre Aktionen bei Sportveranstaltungen — stuermte kurz vor Kilomenter 36 die Marathonstrecke und hielt de Lima fuer mehrere Sekunden auf. De Lima, bis dahin klar fuehrend, verlor seinen Rhythmus und wurde von Stefano Baldini (Italien) und Mebrahtom Keflezighi (USA) ueberholt. Er gewann Bronze. Das IOC verlieh ihm den Pierre-de-Coubertin-Preis fuer Sportlichkeit und Fairplay.",
        difficulty = 5,
        funFact = "Vanderlei de Lima entzuendete bei der Eroeffnungsfeier der Olympischen Spiele 2016 in Rio de Janeiro das olympische Feuer — eine spaete Ehrung von Brasilien fuer den Mann, dem 2004 das Gold gestohlen wurde. Horan hatte zuvor den Grand Prix von Grossbritannien 2003 in Silverstone gestuermt."
    ),

    // Question 10 – Beijing 2008: opening ceremony fakery
    Question(
        categoryId = 6,
        questionText = "Bei der Eroeffnungsfeier der Olympischen Spiele 2008 in Peking wurden zwei spaeter bekannt gewordene 'Faelschungen' entdeckt. Welche beiden Taeuschungen wurden nach den Spielen eingeraeumt, und was waren die jeweiligen Begruendungen der chinesischen Organisatoren?",
        answerA = "Die Feuerwerks-Fussspuren zum Stadion waren am TV komplett CGI; das singende Maedchen sang in Wirklichkeit Playback, da das eigentlich singende Maedchen als 'nicht attraktiv genug' eingestuft wurde",
        answerB = "Die Fackeltragerin Li Ning flog durch das Stadion nur scheinbar selbst; ausserdem wurden die ethnischen Minderheiten-Kinder in Nationaltrachten grossteils von Han-Kindern gespielt, die in Kostueme gekleidet wurden",
        answerC = "Das Einmarsch-Feuerwerk ueber Peking war am Fernsehen teils vorab aufgenommenes Material; die tatsaechliche Live-Uebertragung zeigte andere Kameraperspektiven, die die Vorab-Aufnahmen nicht zeigten",
        answerD = "Der olympische Fackeltraeger in der Schlusssequenz war ein Stuntman statt des angekuendigten Turners; ausserdem wurde der Chorgesang am TV nachtraeglich mit einer anderen Stimme ueberdubbt",
        correctAnswer = 0,
        explanation = "Die chinesischen Organisatoren raeumstn zwei Taeuschungen ein: Erstens wurden die spektakulaeren Feuerwerks-Fussspuren, die im TV zu sehen waren, zum Teil am Computer generiert (CGI), da echte Helikopter-Aufnahmen aus Sicherheitsgruenden nicht moeglich waren. Zweitens sang Lin Miaoke auf der Buehne Playback, waehrend Yang Peiyi (die eigentliche Saengerin) wegen ihres Aussehens hinter den Kulissen sang — eine Entscheidung von Politbuero-Mitglied Wang Wei.",
        difficulty = 5,
        funFact = "Yang Peiyis Fall loeste international eine Debatte ueber Schoenheidsnormen und Kindheit im olympischen Showbusiness aus. Ironischerweise wurde Yang Peiyi durch die Enthuellung weltberuehmt und sprach spaeter offen ueber die Erfahrung, ohne Bitterkeit zu zeigen."
    ),

    // Question 11 – London 2012: Saudi women debut
    Question(
        categoryId = 6,
        questionText = "London 2012 war das erste Olympia, an dem alle teilnehmenden Nationen weibliche Athletinnen entsandten. Welche drei Laender schickten erstmals Frauen, und was war der konkrete diplomatische Druck, der dies erzwang?",
        answerA = "Saudi-Arabien, Katar und Brunei — das IOC drohte mit Ausschluss der nationalen Olympischen Komitees, falls sie weiter rein maennliche Teams schickten",
        answerB = "Saudi-Arabien, Iran und Vereinigte Arabische Emirate — US-Aussenministerin Hillary Clinton ueberredete die Koenige persoenlich in bilateralen Gespraechen",
        answerC = "Saudi-Arabien, Katar und Brunei — der Britische Olympische Verband drohte mit Nichtakkreditierung der Teams; ausserdem hatte das IOC 2010 eine neue Regel erlassen",
        answerD = "Saudi-Arabien und Sudan — beide Laender schickten je eine Athletin unter der Bedingung, dass diese in voller islamischer Bedeckung antreten duerfe",
        correctAnswer = 0,
        explanation = "Saudi-Arabien, Katar und Brunei schickten 2012 erstmals Frauen zu Olympia. Das IOC hatte seit Jahren Druck aufgebaut und 2012 offiziell erklaert, dass nationale Komitees, die keine Frauen nominieren, von der Teilnahme ausgeschlossen werden koennten. Saudi-Arabiens Athletinnen Sarah Attar (800 m) und Wojdan Ali Seraj Abdulrahim Shahrkhani (Judo) wurden von vielen als 'Botschafterinnen' gesehen, auch wenn beide weit hinter den Qualifikationsstandards lagen.",
        difficulty = 5,
        funFact = "Wojdan Shahrkhani trat im Judo an, ohne eine einzige internationale Judo-Erfahrung zu haben, und verlor in 82 Sekunden. Das IOC-Mitglied Dick Pound nannte sie spaeter eine 'Feigenblatt-Athletin'. Dennoch gilt ihr Auftritt als historischer Moment fuer Frauenrechte im islamischen Sport."
    ),

    // Question 12 – Rio 2016: Refugee Olympic Team
    Question(
        categoryId = 6,
        questionText = "Bei den Olympischen Spielen 2016 in Rio de Janeiro nahm erstmals ein 'Refugee Olympic Team' teil. Wer genehmigte die Teilnahme, aus wie vielen Athleten bestand das Team, und welcher Athlet des Teams erlangte besondere internationale Bekanntheit?",
        answerA = "IOC-Praesident Thomas Bach; 10 Athleten aus Syrien, Aethiopien, Sued-Sudan und Kongo; Yusra Mardini (Schwimmen, Syrien) wurde weltberuehmt, da sie zuvor im Mittelmeer ein sinkendes Boot gerettet hatte",
        answerB = "UNHCR und IOC gemeinsam; 8 Athleten aus Syrien, Eritrea und Somalia; Rami Anis (Schwimmen, Syrien) wurde bekannt durch seinen Lebensweg als Kriegsfluechtling",
        answerC = "Der UN-Generalsekretaer Ban Ki-moon auf Antrag des IOC; 12 Athleten aus sechs Laendern; die suedsudan esische Laeuerin Rose Nathike Lokonyen wurde zur Symbolfigur",
        answerD = "Das IOC allein per Beschluss des Exekutivkomitees; 6 Athleten aus Syrien und Kongo; der Schwimmer James Nyang Chiengjiek wurde zum Gesicht des Teams",
        correctAnswer = 0,
        explanation = "IOC-Praesident Thomas Bach initiierte und genehmigte das Fluechtlings-Olympia-Team, das aus 10 Athleten aus Syrien, Aethiopien, Sued-Sudan und der Demokratischen Republik Kongo bestand. Yusra Mardini (Syrien, Schwimmen) erlangte weltweite Bekanntheit: Sie hatte 2015 im Aegaeischen Meer ein mit Fluechtlingen ueberladenes Boot gezogen, um es zu retten, und schwamm dabei ueber drei Stunden im offenen Meer.",
        difficulty = 5,
        funFact = "Yusra Mardinis Geschichte wurde 2022 von Netflix verfilmt ('The Swimmers'). Bei den Spielen 2020 in Tokyo und 2024 in Paris gab es erneut ein Refugee Olympic Team, das auf 29 Athleten (2024) anwuchs — ein Zeichen, wie die Initiative institutionalisiert wurde."
    ),

    // Question 13 – Tokyo 2021: mental health and Simone Biles
    Question(
        categoryId = 6,
        questionText = "Simone Biles zog sich bei den Olympischen Spielen 2021 in Tokyo aus mehreren Finalwettbewerben zurueck und nannte 'mentale Gesundheit' als Begruendung. Welcher spezifische gymnast. Zustand, der ernsthafte Verletzungsgefahren birgt, nannte sie als unmittelbaren Ausloeser ihres Rueckzugs?",
        answerA = "Eine akute Schulterluxation waehrend des Aufwaermens, die durch mentalen Stress verstaerkt wurde und zur Diagnose 'PTBS-bedingte Koordinationsstoerung' fuehrte",
        answerB = "Die 'Twisties' — ein desorientierendes Phaenomen, bei dem Turnerinnen mitten in der Luft das Gefuehl fuer ihre Koerperlage verlieren, was bei Sprung- und Abgangs-Elementen letal enden kann",
        answerC = "Eine zuvor nicht bekannte Herzrhythmusstoerung, die durch den olympischen Stress ausgeloest wurde und aerztlich als 'situativer Vagusnerv-Reflex' diagnostiziert wurde",
        answerD = "Dissoziation durch PTBS infolge des Larry-Nassar-Missbrauchsskandals, die sich als Sehstoerung beim Absprung manifestierte und von einem Sportpsychologen des USOC diagnostiziert wurde",
        correctAnswer = 1,
        explanation = "Biles beschrieb die 'Twisties' als den konkreten Ausloeser: Ihr Koerper fuegte mitten im Sprung unkontrollierte Drehbewegungen hinzu, die sie nicht korrigieren konnte. Bei Hochleistungsturnen — mit mehrfachen Salti und Schrauben am Hochreck oder beim Sprung — fuehrt Desorientierung in der Luft zu schweren Lande-Verletzungen. Biles trat spaeter nur noch im Balken an, wo die Absturzgefahr bei 'Twisties' geringer ist.",
        difficulty = 5,
        funFact = "Biles' Rueckzug loeste eine globale Debatte ueber mentale Gesundheit im Leistungssport aus. Tausende von Sportlern meldeten sich oeffentlich zu Wort. Die 'Twisties' — ein Phaenomen, das in der Gymnastics-Community bekannt ist, aber selten oeffentlich diskutiert wird — wurde durch Biles' Aussagen einem Weltpublikum erklaert."
    ),

    // Question 14 – Paris 2024: Seine swimming quality
    Question(
        categoryId = 6,
        questionText = "Bei den Olympischen Spielen 2024 in Paris fanden Triathlon- und Freigewasser-Schwimmwettbewerbe in der Seine statt. Welche konkrete EU-Richtlinie definierte die zulassigen Grenzwerte fuer Escherichia coli und Enterococcen, die erfuellt werden mussten, und welcher Wert wurde unmittelbar vor dem Wettbewerb gemessen, der zu einer Verschiebung der Triathlon-Maenner fuehrte?",
        answerA = "EU-Badegewaesser-Richtlinie 2006/7/EG; der E.-coli-Wert ueberschritt 1000 KbE/100 ml (Grenzwert: 500 KbE), ausgeloest durch Starkniederschlag, der Pariser Mischwasserkanalisations-Ueberlaeufe aktivierte",
        answerB = "WHO-Trinkwasser-Richtlinie von 2011 in Kombination mit franzoesischem Umweltrecht; gemessener Enterococcen-Wert von 2300 KbE/100 ml, 15-mal ueber dem zulaessigen Wert von 150 KbE",
        answerC = "IOC-eigene Schwimm-Wasserqualitaets-Richtlinie von 2016; E.-coli 4800 KbE/100 ml, dreifach ueber dem Grenzwert; verursacht durch einen Abwasserausbruch nach 48-Stunden-Dauerregen",
        answerD = "EU-Kommunalabwasser-Richtlinie 91/271/EWG; Enterococcen-Wert von 900 KbE/100 ml gegenueber dem Grenzwert von 400 KbE; Paris hatte 1,4 Mrd. EUR in Kanalisation investiert, die im Sommer 2024 noch nicht fertig war",
        correctAnswer = 0,
        explanation = "Die EU-Badegewaesser-Richtlinie 2006/7/EG definiert 'ausreichende Qualitaet' bei E. coli unter 500 KbE/100 ml (Medianwert). Starkregen kurz vor dem Triathlon aktivierte Pariser Mischwasserkanaele, die in die Seine entwasssern, und der E.-coli-Wert ueberschritt den Grenzwert erheblich. Das Maenner-Triathlon wurde um 24 Stunden verschoben; nach Verbesserung der Werte fanden alle Wettbewerbe statt, mehrere Athleten erkrankten dennoch nach dem Rennen.",
        difficulty = 5,
        funFact = "Paris investierte 1,4 Milliarden Euro in die Seine-Wasserqualitaet als Teil des Olympia-Projekts. Buergermeisterin Anne Hidalgo schwamm kurz vor den Spielen oeffentlich in der Seine, um die Qualitaet zu demonstrieren — ein PR-Akt, der sie spaeter in einer Erklaerung verteidigen musste."
    ),

    // Question 15 – Paralympics history
    Question(
        categoryId = 6,
        questionText = "Die modernen Paralympics gehen auf Sir Ludwig Guttmann zurueck. In welchem Jahr wurden die 'Internationalen Stoke-Mandeville-Spiele' erstmals ausgetragen, welche Verletzungsgruppe nahm urspruenglich teil, und wann wurden sie offiziell zum ersten Mal als 'Paralympics' bezeichnet?",
        answerA = "1948 (parallel zu den Londoner Olympischen Spielen) — ausschliesslich Rollstuhlfahrer mit Rueckenmarksverletzungen; der Begriff 'Paralympics' wurde erstmals 1960 bei den Spielen in Rom verwendet",
        answerB = "1952 (erstmals mit internationaler Beteiligung, Niederlande) — Veteranen aller Verletzungsarten; der Begriff 'Paralympics' wurde 1964 in Tokyo offiziell eingefuehrt",
        answerC = "1944 (als Therapie in Stoke Mandeville Hospital) — Kriegsversehrte mit Rueckenmarksverletzungen; der Begriff 'Paralympics' stammt von 1948 und wurde vom britischen Koenigshaus verliehen",
        answerD = "1948 (in Stoke Mandeville, England) — ausschliesslich britische Rueckenmarksverletzten-Veteranen; 'Paralympics' als offizieller Begriff wurde 1960 in Rom gebraeucht, als erstmals nichteuropaeische Nationen teilnahmen",
        correctAnswer = 0,
        explanation = "Sir Ludwig Guttmann organisierte 1948 die ersten 'Internationalen Stoke-Mandeville-Spiele' als therapeutischen Wettbewerb fuer Rollstuhlfahrer mit Rueckenmarksverletzungen, parallel zu den Londoner Olympischen Spielen. 1960 fanden in Rom die ersten Spiele unter dem Begriff 'Paralympics' statt — 'para' stand dabei fuer 'parallel' (zu Olympia) und nicht fuer 'Paraplegiker', wie oft faelschlich angenommen wird.",
        difficulty = 5,
        funFact = "Guttmanns urspruengliches Ziel war nicht Sport, sondern Rehabilitation: Er glaubte, dass kompetitiver Sport Kriegsversehrten mit Rueckenmarksverletzungen psychologisch half, ihr Leben nach der Verletzung neu zu strukturieren. Die sportliche Komponente entwickelte sich aus therapeutischen Uebungen heraus."
    ),

    // Question 16 – Paralympic classification system
    Question(
        categoryId = 6,
        questionText = "Das Klassifikationssystem der Paralympics unterteilt Athleten nach Art und Ausmass ihrer Behinderung in Klassen. Welches uebergeordnete Prinzip liegt dem modernen Klassifikationssystem zugrunde, und welche historisch umstrittene Praxis wurde 1992 beim IPC abgeschafft?",
        answerA = "Das Minimale-Beeintraechtigung-Prinzip (MIE): Nur Athleten mit spezifisch definierter Mindestbehinderung duerfen antreten; 1992 wurde das System der 'Medical Classification' durch 'Sport Classification' ersetzt",
        answerB = "Das Evidenz-basierte Klassifikationsprinzip: Klassen sollen sicherstellen, dass sportliche Leistung den Athleten entscheidet, nicht das Ausmass der Behinderung; 1992 wurde die Trennung nach Behinderungsart (Blindheit vs. Querschnitt vs. Amputationen) in separaten Wettbewerben abgeschafft und durch sportspezifische Klassen ersetzt",
        answerC = "Das Gleichheitsprinzip der WHO ICF-Klassifikation: Behinderungen werden nach Aktivitaets- und Partizipationseinschraenkungen klassifiziert; 1992 wurde die Einteilung nach Knochen-Loesungen (z.B. Amputation Level) aufgegeben",
        answerD = "Das Sport-Funktions-Prinzip: Klassen basieren ausschliesslich auf medizinischen Diagnosen; 1992 schufen IOC und IPC ein gemeinsames Komitee, das alle para-olympischen Sportarten unter ein Dach brachte",
        correctAnswer = 1,
        explanation = "Das moderne Klassifikationssystem soll sicherstellen, dass Behinderungen den Ausgang eines Wettkampfs moeglichst wenig beeinflussen — sportliche Leistung soll entscheiden. Historisch hatten separate Spielformate fuer jede Behinderungsgruppe (Blind-Olympiade, Rollstuhl-Spiele, Amputierten-Spiele) existiert. 1992 in Barcelona wurden erstmals alle Behinderungsarten unter dem IPC-Dach in integrierten Wettbewerben zusammengefuehrt.",
        difficulty = 5,
        funFact = "Das Klassifikationssystem ist bis heute umstritten: 'Klassifikations-Betrug' — bei dem Athleten ihre Behinderung bei Klassifikationsuntersuchungen uebertreiben — gilt als eine der grossen Integritaetsherausforderungen des Parasports. Mehrere Skandale haben das IPC zu strengeren Verfahren gezwungen."
    ),

    // Question 17 – E-Sport Olympic recognition
    Question(
        categoryId = 6,
        questionText = "Das IOC veranstaltete 2023 die ersten 'Olympic Esports Series' und gruendete 2024 die 'Olympic Esports Games'. Welches Grundprinzip definierte das IOC bei der Auswahl der Spiele, und welches Spiel/Genre wurde explizit ausgeschlossen, obwohl es weltweit das meistgesehene E-Sport-Genre ist?",
        answerA = "Nur Spiele, die reale olympische Sportarten simulieren, sind zugelassen; Ego-Shooter-Spiele (First-Person-Shooter wie CS:GO, Valorant) wurden wegen Gewaltdarstellung explizit ausgeschlossen",
        answerB = "Spiele muessen 'olympische Werte' widerspiegeln; FIFA (Fussball-Simulation) wurde ausgeschlossen, da EA Sports keine Lizenzvereinbarung mit dem IOC akzeptierte",
        answerC = "Nur Spiele mit physischer Aktivitaetskomponente sind zugelassen (z.B. Tanzsimulation); Strategie- und MOBA-Spiele wurden als 'nicht sport-repraesentativ' ausgeschlossen",
        answerD = "Exklusiv Echtzeit-Sport-Simulationen werden akzeptiert; Battle-Royale-Spiele wie Fortnite wurden wegen loot-box-Mechanismen ausgeschlossen, die dem IOC-Anti-Spielsucht-Prinzip widersprechen",
        correctAnswer = 0,
        explanation = "Das IOC legte fest, dass in den Olympic Esports Games nur solche Spiele zugelassen werden, die reale Sportarten simulieren oder olympische Werte foerdern. Ego-Shooter — darunter die populaersten E-Sport-Genres (Counter-Strike, Valorant, Call of Duty) — wurden explizit ausgeschlossen, da das IOC keine 'Verherrlichung von Gewalt' foerdern wolle. Dies obwohl Shooter-E-Sport weltweit die meisten Zuschauer hat.",
        difficulty = 5,
        funFact = "Die ersten Olympic Esports Games 2025 in Saudi-Arabien umfassten Spiele wie Fortnite (Null-Schaden-Modus), Gran Turismo, Virtual Regatta und Chess.com. Kritiker bemerkten die Ironie, dass Saudi-Arabien — das erhebliche Menschenrechtsprobleme hat — als Ausrichter gewahlt wurde, um olympische Werte zu bewerben."
    ),

    // Question 18 – Oscar Pistorius Olympic participation
    Question(
        categoryId = 6,
        questionText = "Oscar Pistorius stritt jahrelang um das Recht, mit Karbonfaser-Prothesen (Cheetah Flex-Foot) bei olympischen Wettbewerben anzutreten. Welches Gremium entschied letztlich zu seinen Gunsten, welches Argument der IAAF wies es zurueck, und welche Paradoxie zeigte spaetere biomechanische Forschung?",
        answerA = "Der CAS (Court of Arbitration for Sport) 2008; er wies das IAAF-Argument zurueck, die Prothesen gaeben ihm einen mechanischen Energievorteil; spaetere Studien zeigten jedoch, dass Pistorius bei Kurvenlaeufen (Biegung in der Kurve) biomechanisch benachteiligt war",
        answerB = "Der Internationale Sportgerichtshof (IGH) in Den Haag 2007; er wies das IAAF-Argument zurueck, er gaebe eine 'unhaltbare Ermuedungsresistenz' — spaetere Studien zeigten, dass die Prothesen in der Anfangsphase des Rennens (erste 100 m) tatsaechlich einen Vorteil boten",
        answerC = "Der CAS 2008; er wies das IAAF-Argument zurueck, die Prothesen verbrauchten weniger metabolische Energie — spaetere biomechanische Studien bestatigten aber einen Vorteil in der Endphase eines 400-m-Laufs, da keine Ermuedung der Wadenmuskulatur eintritt",
        answerD = "Das Schweizer Bundesgericht 2009 auf Antrag von Athletics South Africa; das IAAF-Argument mechanischer Energierueckgewinnung wurde verworfen; Forschung zeigte jedoch, dass Pistorius bei Laeufen unter Windstille signifikant besser abschnitt als bei Gegenwind",
        correctAnswer = 2,
        explanation = "Der CAS hob 2008 das IAAF-Startverbot auf und wies das Argument zurueck, die Cheetah-Prothesen verbrauchten weniger metabolische Energie. Spaetere biomechanische Studien (Grabowski et al., 2010) zeigten jedoch eine komplexe Paradoxie: Die Prothesen bieten in der Schlusphase eines 400-m-Laufs moeglicherweise einen Vorteil, da keine Ermuedung der Wadenmuskulatur eintritt, waehrend die Knie- und Hueftmuskulatur durch den anderen Hebelarm staerker belastet wird.",
        difficulty = 5,
        funFact = "Pistorius wurde 2012 der erste amputierte Laeufer in der olympischen Geschichte und nahm am 400-m-Lauf sowie der 4x400-m-Staffel teil. 2013 wurde er wegen des Mordes an seiner Freundin Reeva Steenkamp verhaftet und 2015 zu 13 Jahren Haft verurteilt — das abrupteste Ende einer olympischen Karriere in der neueren Geschichte."
    ),

    // ── Paralympics-Geschichte & Behindertensport (7) ─────────────────────────

    // Question 19 – IPC founding and structure
    Question(
        categoryId = 6,
        questionText = "Das Internationale Paralympische Komitee (IPC) wurde 1989 gegruendet. Welche vier vorher unabhaengigen Dachorgansationen wurden in die IPC-Struktur integriert, und welches Land war Gruendungsort?",
        answerA = "ISOD (Amputierte/Sehbehinderte), IBSA (Blind), CP-ISRA (Zerebralparese), ISMGF (Rollstuhl) — gegruendet in Dusseldorf, Deutschland",
        answerB = "ISOD, IBSA, CP-ISRA, INAS-FMH (kognitive Behinderungen) — gegruendet in Arnhem, Niederlande",
        answerC = "ISMWSF (Rollstuhl-Sport), ISOD, IBSA, CP-ISRA — gegruendet in London, Grossbritannien",
        answerD = "ISMGF, ISOD, IBSA, INAS-FID — gegruendet in Wien, Oesterreich",
        correctAnswer = 0,
        explanation = "Das IPC wurde am 22. September 1989 in Dusseldorf, Deutschland gegruendet und integrierte die vier bis dahin autonomen Verbande: ISOD (International Sports Organisation for the Disabled — Amputierte und Sehbehinderte), IBSA (International Blind Sports Federation), CP-ISRA (Cerebral Palsy International Sports and Recreation Association) und ISMGF (International Stoke Mandeville Games Federation — Rollstuhl). Der Hauptsitz des IPC ist seitdem in Bonn, Deutschland.",
        difficulty = 5,
        funFact = "Obwohl das IPC seit 2003 einen Zusammenarbeitsvertrag mit dem IOC hat, ist es rechtlich vollstaendig unabhaengig. Das gemeinsame Ausrichter-Modell (Olympia und Paralympics in einer Stadt) ist vertraglich festgelegt, aber das IPC verhandelt separat Fernsehrechte und Sponsoren."
    ),

    // Question 20 – Blade Runner technology
    Question(
        categoryId = 6,
        questionText = "Die Cheetah-Flex-Foot-Prothesen, die Laufamputierten im Sprint eingesetzt werden, basieren auf einem bestimmten physikalischen Prinzip der Energiespreicherung. Welches Material und welches technische Prinzip werden eingesetzt, und welche Firma und welcher Erfinder entwickelten den Cheetah-Urtyp?",
        answerA = "Titan mit Memory-Effekt (Formgedaechtnis-Legierung) — elastische Verformung gibt kinetische Energie zurueck; entwickelt von Ossur (Island) und dem Amputierten-Athleten Dennis Oehler",
        answerB = "Karbonfaser-Verbundwerkstoff — speichert beim Aufprall elastische Energie (Biegeverformung) und gibt sie beim Abstoss zurueck; entwickelt von Van Phillips (selbst Amputierter) und Flex-Foot Inc., spaeter von Ossur erworben",
        answerC = "Glasfaser-Titan-Hybrid — piezoelektrischer Effekt wandelt Druckenergie in kinetische Energie um; entwickelt von Otto Bock Deutschland in Zusammenarbeit mit Carl Lewis",
        answerD = "Karbonfaser-Polyurethan-Komposit — pneumatische Kammern speichern Luftdruck beim Aufprall; entwickelt von Hanger Inc. (USA) und dem Biomechaniker Hugh Herr am MIT",
        correctAnswer = 1,
        explanation = "Die Cheetah-Prothese besteht aus Karbonfaser-Verbundwerkstoff, der beim Bodenkontakt wie eine Feder gebogen wird und elastische Potential-Energie speichert. Beim Abstoss wird diese Energie als kinetische Energie zurueckgegeben. Van Phillips, der nach einem Wasserskiing-Unfall sein Bein verlor und mit der Qualitaet damaliger Prothesen unzufrieden war, entwickelte den Prototyp Ende der 1980er Jahre. Flex-Foot Inc. wurde spaeter von der islaendischen Firma Ossur uebernommen.",
        difficulty = 5,
        funFact = "Van Phillips liess sich von der Schwanzflosse eines Delfins und dem Sprungbein einer Katze inspirieren — daher der Name 'Cheetah' (Gepard). Die C-foermige Karbonfaser-Konstruktion ahmt die Funktion der menschlichen Achillessehne nach, ist aber im Energierueckgabe-Prozentsatz effizienter als biologisches Gewebe."
    ),

    // Question 21 – Wheelchair tennis Grand Slam
    Question(
        categoryId = 6,
        questionText = "Rollstuhltennis ist seit 1992 paralympische Disziplin. Welcher Athlet hielt bis 2024 das Allzeit-Rekord bei Grand-Slam-Titeln im Rollstuhltennis, wie viele Titel hatte er, und welche technische Regel unterscheidet Rollstuhltennis von Standard-Tennis?",
        answerA = "Shingo Kunieda (Japan) mit 50 Grand-Slam-Titeln (Einzel + Doppel kombiniert); Rollstuhltennis erlaubt zwei Abspruenge des Balles statt einem",
        answerB = "Esther Vergeer (Niederlande) mit 21 Einzel-Grand-Slam-Titeln; der Ball darf nach dem zweiten Absprung gespielt werden, wenn der erste Absprung im Spielfeld war",
        answerC = "Shingo Kunieda mit 28 Grand-Slam-Einzeltiteln; der Rollstuhl darf das Netz beruehren ohne Punktverlust",
        answerD = "Gordon Reid (UK) mit 16 Grand-Slam-Doppeltiteln; der Aufschlag muss nicht diagonal gespielt werden, sondern kann gerade ueber das Netz erfolgen",
        correctAnswer = 0,
        explanation = "Shingo Kunieda (Japan) ist der rekordverdaechtige Rollstuhltennis-Spieler mit ueber 50 Grand-Slam-Titeln in Einzel und Doppel zusammen (Stand 2024), darunter 28 Einzel-Majors. Die entscheidende Regelabweichung: Im Rollstuhltennis darf der Ball zweimal abspringen, bevor der Spieler ihn schlaegt — wobei der erste Absprung im Spielfeld sein muss. Der zweite Absprung darf ausserhalb des Courts sein.",
        difficulty = 5,
        funFact = "Esther Vergeer (Niederlande) gilt als die dominanteste Rollstuhlsportlerin der Geschichte: Sie gewann von 2003 bis zu ihrem Ruecktritt 2013 kein einziges Spiel verloren — eine Serie von 470 Matches ohne Niederlage, unerreicht in jeder Sportart."
    ),

    // Question 22 – Goalball sport rules
    Question(
        categoryId = 6,
        questionText = "Goalball ist eine paralympische Sportart fuer Sehbehinderte. Welche drei fundamentalen Spielregeln machen Goalball einzigartig unter allen Teamsportarten, und seit wann ist es paralympische Disziplin?",
        answerA = "Alle Spieler tragen Augenbinden (auch sehbehinderte Spieler, um Chancengleichheit zu gewaehrleisten); der Ball enthaelt eine Glocke; das Publikum muss waehrend des Spiels voellig still sein — seit 1976 in Toronto paralympisch",
        answerB = "Nur voellig blinde Spieler duerfen antreten; der Ball hat Loecher mit integriertem Lautsprecher, der ein Signal aussendet; das Spielfeld ist taktil markiert — seit 1980 in Arnhem paralympisch",
        answerC = "Spieler duerfen nicht sprechen; der Ball wird nur gerollt (kein Werfen); das Spielfeld hat tastbare Linien — seit 1972 in Heidelberg paralympisch",
        answerD = "Alle Spieler tragen Augenbinden; der Ball enthaelt eine Glocke; Spieler liegen beim Blocken auf dem Boden — seit 1976 in Toronto paralympisch",
        correctAnswer = 3,
        explanation = "Goalball hat drei einzigartige Regeln: Erstens tragen alle Spieler — auch jene mit Sehrueckresten — Augenbinden, damit die Behinderung keine Rolle spielt. Zweitens enthaelt der Ball eine Glocke, damit Spieler ihn hoeren koennen. Drittens muessen Verteidiger den rollenden Ball mit dem ganzen Koerper (liegend) abblocken. Das Publikum muss still sein, damit Spieler den Ball hoeren koennen. Goalball wurde 1976 bei den Paralympics in Toronto erstmals ausgetragen.",
        difficulty = 5,
        funFact = "Goalball wurde 1946 von dem oesterreichischen Hans Lorenzen und dem deutschen Sepp Reindle als Rehabilitationsgeraet fuer im Zweiten Weltkrieg erblindte Kriegsveteranen erfunden. Es ist eine der wenigen Sportarten, die speziell fuer Menschen mit Behinderungen erfunden wurde, anstatt adaptiert zu werden."
    ),

    // ── Extrem- und Randsportarten (8) ───────────────────────────────────────

    // Question 23 – Curling: history and stones
    Question(
        categoryId = 6,
        questionText = "Curling-Steine muessen nach World Curling Federation-Regeln aus einem bestimmten Gesteinstyp von zwei spezifischen Gesteinsquellen der Welt stammen. Welche Gesteinsart und welche Abbauorte sind vorgeschrieben, und welche physikalische Eigenschaft des Materials macht es einzigartig geeignet?",
        answerA = "Granit aus dem Ailsa Craig (Schottland) oder Trefor Granite Quarry (Wales); extrem niedriger Wasseraufnahme-Koeffizient (unter 0,1%) und hohe Druckfestigkeit von 200+ MPa verhindert Abplatzungen bei -5 Grad",
        answerB = "Schottischer Blaustein (Blairgowrie Granite) aus Perthshire oder Kinesischer Jadeit; Einzigartigkeit liegt in der piezoelektrischen Eigenschaft, die beim Gleiten Reibungswaerme erzeugt",
        answerC = "Ailsa Craig Microgranite (Schottland) oder Welsh Granite; Hauptvorteil ist der thermisch stabile Ausdehnungskoeffizient bei Temperaturen zwischen -5 und +5 Grad Celsius",
        answerD = "Nordsee-Basalt aus dem norwegischen Kinn oder Schottischer Ailsa Craig Granit; einzig diese Materialien haben den noetigen Elastizitaetsmodul fuer praezises Gleiten auf Eis ohne Verformung",
        correctAnswer = 0,
        explanation = "WCF-Regelwerk schreibt vor, dass Curling-Steine aus Granit vom Ailsa Craig (einer schottischen Insel im Firth of Clyde) oder vom Trefor Quarry in Wales bestehen muessen. Ailsa Craig 'Blue Hone'-Granit hat einen extrem niedrigen Wasser-Absorptionskoeffizient (unter 0,1%), was bei Frost verhindert, dass sich Wasser im Stein ausdehnt und Abplatzungen verursacht. Die hohe Dichte (ca. 2750 kg/m3) und Haerte sorgen fuer praezises Gleiten.",
        difficulty = 5,
        funFact = "Der Ailsa Craig (schottisch-gaelisch: 'Felsenfairy') ist ein unbewohnter Vulkankegel, der das Habitat von 36.000 Basstolpeln beherbergt. Der Granit-Abbau ist streng reguliert; es werden nur sporadische Abbau-Expeditionen zugelassen, um das Oekosystem zu schuetzen. Weltweit sind etwa 70% aller Curling-Steine aus Ailsa Craig-Granit gefertigt."
    ),

    // Question 24 – Skeleton origins
    Question(
        categoryId = 6,
        questionText = "Skeleton (Eisschlittensport) wurde 1928 und 1948 olympisch, dann fuer fuenf Jahrzehnte ausgesetzt. Was unterscheidet Skeleton technisch von Bobsport und Rennrodeln in Bezug auf Fahrtrichtung und Korperposition, und welche biomechanische Herausforderung erzeugt die typische Kopfvoraus-Fahrt bei Kurvendurchfahrten?",
        answerA = "Skeleton: Fahrt kopfvoraus, Bauchlage auf dem Schlitten; Bob: Fuessvoraus sitzend; Rennrodeln: Fuessvoraus liegend rueckwaerts. Kopfvoraus-Kurven erzeugen bis zu 5g Querbeschleunigung direkt auf den Schaedel ohne schuetzende Nackenmuskeln",
        answerB = "Skeleton: Fahrt kopfvoraus, Bauchlage; Bob: sitzend mit Steuerrad; Rennrodeln: liegend auf dem Ruecken. Kopfvoraus-Kurven zwingen Fahrer, den Kopf seitlich zu neigen, was die Halswirbel-Belastung auf das 3-4-Fache erhoehen kann",
        answerC = "Skeleton: Fahrt rueckwaerts kopfvoraus, Bauchlage; Bob: vorwaerts sitzend; Rennrodeln: vorwaerts liegend. Kurven erzeugen bis zu 7g lateral; da Fahrer nach hinten schauen, haben sie keine visuelle Vorbereitung auf Kurven",
        answerD = "Skeleton: kopfvoraus auf dem Bauch; Bob: sitzend; Rennrodeln: Rueckenlage fuessvoraus. Kopfvoraus-Position erfordert aktive Nackenmuskeln als einzige Steuerung; Kurven erzeugen 4-5g, wobei Fahrer Kinn und Schultern zur Lenkung einsetzen",
        correctAnswer = 3,
        explanation = "Skeleton-Fahrer liegen auf dem Bauch, kopfvoraus, und lenken durch minimale Gewichtsverlagerung mit Schultern, Knie und Kinn — es gibt keine mechanische Steuerung. Rennrodler liegen auf dem Ruecken, fuessvoraus. Bei Skeleton-Kurven mit bis zu 4-5g Querbeschleunigung muessen Fahrer aktiv mit der Nackenmuskulatur gegensteuern; ein passiver Kopf wuerde bei diesen g-Kraften unkontrolliert zur Seite schlagen und zu Verletzungen fuehren.",
        difficulty = 5,
        funFact = "Der Name 'Skeleton' stammt laut gaengiger Erklarung vom metallischen Aussehen des fruehen Schlittens, der wie ein Skelett aussah. Eine andere Theorie besagt, der Name komme vom norwegischen 'skelk' (Schlittenart). Skeleton wurde 2002 in Salt Lake City nach 54-jaehriger olympischer Abwesenheit wieder aufgenommen."
    ),

    // Question 25 – Water polo: history and rules
    Question(
        categoryId = 6,
        questionText = "Wasserball ist eine der aeltesten olympischen Teamsportarten und war seit den ersten modernen Spielen 1900 (fuer Maenner) praesentiert. Welche technische Regelaenderung wurde 2005 eingefuehrt, die das Spiel fundamental veraenderte, und welche taktische Konsequenz ergab sich daraus?",
        answerA = "Einfuehrung eines Shot Clocks (30 Sekunden Angriff) statt 35 Sekunden; Teams mussten schnellere Angriffe entwickeln und die 'Eckenspiel'-Taktik wurde dominanter",
        answerB = "Einfuehrung des 'Penalty Shot' nach Foul im letzten Drittel ohne Zeitstrafe; die sogenannte 'Extraschwimmer'-Taktik (6-gegen-5) wurde dadurch seltener eingesetzt",
        answerC = "Verkuerzung der Angriffszeit von 45 auf 30 Sekunden und gleichzeitig Einfuehrung der 'Doppelfoulregel'; diese aenderte Defensivtaktiken fundamental, da Doppelfouls zur sofortigen Spieler-Auszeit fuehrten",
        answerD = "Einfuehrung der Zwei-Meter-Linie als Sperrzone fuer Feldspieler im Angriff; die Spieler mussten aktiver nach aussen oeffnen, was schnellere Passkombinationen erzwang",
        correctAnswer = 0,
        explanation = "FINA fuehrte 2005 den 30-Sekunden-Shot-Clock (zuvor 35 Sekunden) ein, was den Spielrhythmus spuerbar beschleunigte. Teams konnten nicht mehr so lange in der Peripherie auf Oeffnungen warten und mussten schnellere Transitionsspiele entwickeln. Die taktische Konsequenz war eine Zunahme von Eckspielen und schnellen Kontern statt langem Positionsspiel.",
        difficulty = 5,
        funFact = "Wasserball-Maenner stehen seit 1900 bei olympischen Spielen auf dem Programm — damit ist es (neben Leichtathletik und Schwimmen) eine der aeltesten Olympia-Disziplinen. Frauen-Wasserball wurde erst 2000 in Sydney olympisch, obwohl Frauen den Sport seit den 1920er Jahren ausueaben."
    ),

    // Question 26 – Polo: rules and handicap system
    Question(
        categoryId = 6,
        questionText = "Polo besitzt ein einzigartiges Handicap-System, das Spieler auf einer Skala von -2 bis +10 Gorals bewertet. Wie viele aktive Spieler weltweit haben das Maximal-Handicap von +10, und welche Sonderregel gilt fuer die Ponies (Pferde) in Bezug auf Wechsel waehrend eines Spiels?",
        answerA = "Weltweit gibt es stets weniger als 20 aktive +10-Handicap-Spieler; Ponies muessten nach jedem Chukka (7-Minuten-Spielabschnitt) gewechselt werden, da wiederholte Belastung Herz-Kreislauf-Schaden riskiert",
        answerB = "Weltweit gibt es ca. 50 aktive +10-Spieler; Ponies koennen unbegrenzt eingesetzt werden, muessen aber nach zwei aufeinanderfolgenden Chukkas pausieren",
        answerC = "Weltweit gibt es weniger als 100 aktive Spieler mit +8 oder hoeher; Ponies duerfen je Spiel maximal zwei Chukkas spielen und muessen dann mindestens 30 Minuten pausieren",
        answerD = "Weltweit gibt es stets 20-25 aktive +10-Spieler (fast ausschliesslich Argentinier); Ponies spielen typischerweise nur einen Chukka pro Spiel, benoetigen aber keine Pflicht-Pause per Regelwerk",
        correctAnswer = 3,
        explanation = "Das +10-Handicap (die hoechste Bewertung) halten weltweit stets nur 20-25 aktive Spieler, von denen die grosse Mehrheit Argentinier sind — Argentinien dominiert Polo seit Jahrzehnten aehnlich wie Kenia das Langstreckenlaufen. Pro Spieler werden typischerweise 4-8 Pferde fuer ein Spiel mitgebracht, jedes Pony spielt idealerweise nur einen Chukka (ca. 7 Minuten), da die Belastung durch Galoppieren und abrupte Richtungswechsel enorm ist.",
        difficulty = 5,
        funFact = "Polo war 1900, 1908, 1920, 1924 und 1936 olympisch, wurde dann gestrichen. Die aufwaendige Logistik (Dutzende Pferde pro Team, Transport, Stallung) und die extreme Exklusivitaet des Sports galten als unvereinbar mit dem modernen olympischen Ideal. Eine Wiederzulassung wird gelegentlich diskutiert, gilt aber als unrealistisch."
    ),

    // Question 27 – Cricket history and formats
    Question(
        categoryId = 6,
        questionText = "Cricket ist der einzige Olympia-Sport, der 1900 in Paris gespielt, aber seitdem nie wieder aufgenommen wurde. Das Spiel selbst kennt drei offizielle Formate. Welches Format hat die laengste moegliche Spieldauer, und welche hochspezialisierte statistische Kategorie existiert ausschliesslich im Cricket und in keiner anderen Sportart der Welt?",
        answerA = "Test Cricket (bis 5 Tage je 6 Stunden = 30 Stunden); die 'Duck'-Statistik zaehlt, wie oft ein Schlaeger out ist, ohne einen einzigen Lauf erzielt zu haben — einzige Sportart mit 'null-Punkte-Out-Statistik'",
        answerB = "Test Cricket (5 Tage); 'Extras' ist eine Statistikkategorie, die Punkte zahlt, die nicht durch Schlae erzielt wurden — in keiner anderen Sportart werden Punkte ohne Spieleraktion vergeben",
        answerC = "First-Class Cricket (bis 4 Tage); 'LBW' (Leg Before Wicket) ist eine Auswurfentscheidung durch den Schiedsrichter, die in keiner anderen Ballsportart vorkommt",
        answerD = "Test Cricket (5 Tage); der 'Strike Rate' — Laeufe pro 100 Balle — ist die einzige Sportstatistik, die eine Ratenkennzahl mit einer Gesamtzahl kombiniert, um individuelle Effizienz zu messen",
        correctAnswer = 0,
        explanation = "Test Cricket dauert bis zu fuenf Tage (mit je bis zu 6 Spielstunden, theoretisch 30+ Stunden Nettosspielzeit), was keine andere Teamsportart erreicht. Die 'Duck'-Statistik (benannt nach dem Ei, das der Zahl 0 aehnelt) zaehlt, wie oft ein Batsman dismissal ist ohne einen einzigen Run erzielt zu haben — spezifisch fuer Cricket ist auch die 'Golden Duck' (out mit dem ersten Ball), 'Silver Duck' (zweiter Ball) etc.",
        difficulty = 5,
        funFact = "Das einzige Olympia-Cricket-Spiel 1900 in Paris gewann Grossbritannien (vertreten vom Devon and Somerset Wanderers Cricket Club) gegen Frankreich. Das Spiel dauerte zwei Tage. Frankreichs 'Team' bestand grossteils aus britischen Expatriates in Paris — womoeglich war es faktisch ein internes britisches Turnier."
    ),

    // Question 28 – Rugby sevens Olympic return
    Question(
        categoryId = 6,
        questionText = "Rugby Sevens wurde 2016 in Rio nach 92-jaehriger olympischer Abwesenheit (Rugby Union war 1900-1924 olympisch) wieder aufgenommen. Welche strukturelle Eigenschaft von Rugby Sevens — im Vergleich zu Rugby Union (15er) — macht es fuer Olympia besonders geeignet, und welche Nation gewann sowohl das Maenner- als auch das Frauen-Turnier in Rio 2016?",
        answerA = "Sieben Spieler statt 15, Spielzeit 14 Minuten (2x7) statt 80 Minuten — ermoeglicht ein vollstaendiges Turnier in 3 Tagen; Fidschi (Maenner) und Australien (Frauen) gewannen jeweils Gold",
        answerB = "Sieben Spieler, 14 Minuten Spielzeit — ganzes Turnier in 2 Tagen abschliessbar; USA (Maenner) und Kanada (Frauen) gewannen Gold",
        answerC = "Sieben Spieler, 20 Minuten Spielzeit (2x10) — ein Turnier dauert wie Fussball ca. 10-14 Tage; Suedafrika (Maenner) und Neuseeland (Frauen) gewannen Gold",
        answerD = "Sieben Spieler, 14 Minuten Spielzeit — ermoeglicht Turnier in 3 Tagen; Neuseeland (Maenner) und Australien (Frauen) gewannen jeweils Gold in Rio",
        correctAnswer = 0,
        explanation = "Rugby Sevens spielt mit 7 (statt 15) Spielern pro Team und dauert 14 Minuten (2x7 min) pro Spiel. Ein komplettes olympisches Turnier mit 12 Teams kann so in drei Tagen abgewickelt werden, was es ideal fuer das verdichtete olympische Programm macht. 2016 in Rio gewann Fidschi sensationell Gold bei den Maennern (ihr erster olympischer Medaillengewinn ueberhaupt), Australien gewann Gold bei den Frauen.",
        difficulty = 5,
        funFact = "Fidschis Gold 2016 war die erste olympische Medaille fuer den Inselstaat in seiner Geschichte. Der Empfang des Teams bei der Rueckkehr nach Suva galt als das groesste Massenereignis in Fidschis Geschichte. Das ganze Land hatte einen inoffiziellen Feiertag."
    ),

    // ── Sport-Oekonomie: Transferrekorde & Finanzierung (7) ──────────────────

    // Question 29 – Neymar transfer record
    Question(
        categoryId = 6,
        questionText = "Neymars Transfer von FC Barcelona zu Paris Saint-Germain im Sommer 2017 fuer 222 Millionen Euro war der teuerste Spielertransfer aller Zeiten. Welche Besonderheit hatte dieser Transfer aus Sicht des Finanzrechts, und welche UEFA-Regel wurde trotzdem nicht verletzt?",
        answerA = "PSG zahlte die Abloesesum me direkt aus dem staatlichen Qatar Investment Authority-Fonds, ohne sie als Klubeinnahmen zu deklarieren — die UEFA Financial Fair Play-Pruefung akzeptierte dies, da Staatsfondsgelder als 'lokales Sponsoring' galten",
        answerB = "Neymar loeste seine Ausstiegsklausel (Buyout Clause) selbst, statt dass PSG die Summe an Barcelona zahlte — die Zahlung erfolgte direkt durch einen Neymar-nahen Fonds. UEFA FFP war nicht verletzt, da Transfers als Investition, nicht als Ausgabe gelten",
        answerC = "Der Transfer wurde als 'Imagen-Rechte-Verkauf' strukturiert, sodass PSG technisch keine Transfergebuehr zahlte, sondern Sponsoring-Einnahmen geltend machte — UEFA akzeptierte diese Klassifikation nach einjaehriger Pruefung",
        answerD = "PSG nutzte eine Sonderregelung der Ligue 1 fuer Spieler aus Lusophonen Laendern, die Steuervorteile in Frankreich genoss; UEFA FFP wurde durch zeitlich versetzt gebuchte Einnahmen aus TV-Rechteverkauf kompensiert",
        correctAnswer = 1,
        explanation = "Neymar loeste technisch seine eigene Ausstiegsklausel (222 Mio. EUR) — eine im spanischen Recht verankerte Regelung, die jedem Spieler das Recht gibt, den Vertrag durch Zahlung der Klausel zu kuendigen. Die Zahlung lief nicht direkt von PSG zu Barcelona (was FFP-Pruefungen ausgeloest haette), sondern ueber eine zwischengeschaltete Gesellschaft. UEFA untersuchte FFP-Compliance, stellte aber keinen Verstoss fest.",
        difficulty = 5,
        funFact = "Barcelona hatte Neymars Klausel auf 222 Mio. EUR bewusst so hoch gesetzt, dass sie als unrealistisch galt. Die Zahlung machte Barcelona zu einem der ersten Klubs, dem ein Spieler mit einer 200+-Mio.-Klausel abgekauft wurde — und zeigte, dass selbst astronomische Klauseln keine Schutzwirkung haben, wenn genug Geld vorhanden ist."
    ),

    // Question 30 – Stadium financing models
    Question(
        categoryId = 6,
        questionText = "Die Allianz Arena in Muenchen (eroeffnet 2005) und das Wembley-Stadion (eroeffnet 2007) wurden mit fundamental unterschiedlichen Finanzierungsmodellen gebaut. Welches Modell nutzte jeweils welches Stadion, und welcher Aspekt der deutschen Stadionfinanzierung ist europaweit einzigartig?",
        answerA = "Allianz Arena: rein privat durch Bayern Muenchen und TSV 1860 Muenchen (urspruenglich je 50%); Wembley: oeffentlich-privat (150 Mio. von Sport England + FA-Kredit); in Deutschland gibt es kein oeffentliches Geld fuer Fussballstadien der Bundesliga",
        answerB = "Allianz Arena: 340 Mio. EUR Bankkredit des FC Bayern allein; Wembley: 757 Mio. Pfund, vollstaendig privat durch die Football Association; Deutschland subventioniert keine Profi-Stadien aus Steuergeldern",
        answerC = "Allianz Arena: kommunale Anleihe der Stadt Muenchen rueckgezahlt durch Mieteinnahmen; Wembley: oeffentlich-privates Modell; Besonderheit Deutschland: Kommunen besitzen oft Stadien und vermieten sie an Klubs",
        answerD = "Allianz Arena: 340 Mio. EUR PPP-Modell mit Allianz SE als Minderheitsgesellschafter; Wembley: Lottery Fund + FA; Deutschland hat das 'Mietmodell': Kommunen bauen, Klubs mieten langfristig",
        correctAnswer = 0,
        explanation = "Die Allianz Arena wurde vollstaendig privat finanziert (340 Mio. EUR Bankkredit), urspruenglich je zur Haelfte durch Bayern Muenchen und 1860 Muenchen (1860 stieg spaeter aus). Wembley kostete 757 Mio. Pfund und wurde teils durch Lottery-Gelder und Sport England-Foerderung mitfinanziert. Das deutsche Modell ist europaweit einmalig: Bundesliga-Klubs finanzieren ihre Stadien selbst ohne Subventionen, waehrend in England, Frankreich, Spanien haeufig oeffentliche Gelder fliessen.",
        difficulty = 5,
        funFact = "1860 Muenchen verkaufte seinen 50%-Anteil an der Allianz Arena 2006 wegen finanzieller Noete an FC Bayern zurueck — fuer 11 Mio. EUR, obwohl der Anteil weit mehr wert war. Heute gehoert die Arena dem FC Bayern allein, und 1860 spielt in der Drittklassigkeit ohne eigenes Stadion."
    ),

    // Question 31 – Sports sponsoring history
    Question(
        categoryId = 6,
        questionText = "Der Sporttrikot-Sponsoring-Markt hat eine spezifische Rechtsgeschichte. Welches Bundesgericht-Urteil (in Deutschland) legte den rechtlichen Grundstein fuer Trikot-Werbung in der Bundesliga, und in welchem Jahr trugen Bundesliga-Vereine erstmals offizielle Trikotsponsor-Logos?",
        answerA = "BGH-Urteil 'Jaegermeister' von 1973 staerkte das Recht auf Trikot-Werbung; erstmals trugen Eintracht Braunschweig (Jaesermeister) und mehrere andere Klubs 1973/74 Sponsorlogos",
        answerB = "BVerfG-Urteil zur Rundfunkfreiheit 1972 erlaubte Werbung im oeffentlich-rechtlichen Sport-TV; Trikotsponsoring begann 1975/76 nach DFB-Zustimmung mit Bayern Muenchen (Adidas-Logo sichtbarer)",
        answerC = "Kein Gerichtsurteil war noetig — der DFB erlaubte Trikotwerbung 1974 auf eigene Initiative; Eintracht Braunschweig (Jaegermeister) war der erste Klub in der Saison 1974/75",
        answerD = "BGH-Urteil 1976 zu 'Persoenlichkeitsrechten im Werbebild'; Trikotwerbung war vor 1976 verboten, weil TV-Anstalten Werberechte beanspruchten; erste Trikots 1977/78 nach DFB-Reform",
        correctAnswer = 2,
        explanation = "Es gab kein Gerichtsurteil als Grundstein: Der DFB erlaubte Trikotwerbung aus eigenem Antrieb, beginnend mit der Saison 1974/75. Eintracht Braunschweig war der erste Bundesligaklub mit einem offiziellen Trikotsponsor — Jaesermeister (Edelbrandwerk) — nachdem Klub-Praesident Ernst Fricke die DFB-Ausnahme durchsetzte. Das Jaesermeister-Hirsch-Logo wurde eines der ersten bekannten Trikot-Sponsoring-Bilder der Welt.",
        difficulty = 5,
        funFact = "Jaesermeister-Chef Guenter Mast war massgeblich daran beteiligt: Er erkannte das Marketing-Potential von TV-Uebertragungen und ueberzeugte DFB und Braunschweig. Braunschweig war 1974/75 ueberraschend Tabellenerster der Bundesliga — die Kombination aus Tabellenspitze und Pionier-Sponsoring machte das Jaesermeister-Logo weltberuehmt."
    ),

    // Question 32 – Saudi Arabia sportswashing economics
    Question(
        categoryId = 6,
        questionText = "Das saudische Sportinvestitions-Programm ('Saudi Vision 2030') umfasst neben dem PIF-Sportsfonds massiven Einfluss im Golf, Boxen, Formel 1, Fussball und Wrestling. Welcher oekonomische Fachbegriff beschreibt die Strategie, Sportereignisse zur Verbesserung des internationalen Ansehens zu nutzen, und welches akademische Paper (Autor, Jahr) praeg te diesen Begriff?",
        answerA = "'Sportswashing' — gepraegt von der Menschenrechtsorganisation Human Rights Watch in einem Bericht 2018 ueber Azerbaidschan; spaeter akademisch definiert von Jules Boykoff (2022)",
        answerB = "'Sportswashing' — der Begriff wird Andrew Jennings (Investigativjournalist) zugeschrieben, der ihn 2015 im Guardian im Kontext des FIFA-Korruptionsskandals verwendete",
        answerC = "'Soft Power through Sports' — Joseph Nye (Harvard) beschrieb das Phaenomen 1990 in 'Bound to Lead'; 'Sportswashing' als spezifischer Begriff entstand organisch in sozialen Medien um 2017-2018",
        answerD = "'Reputational Laundering via Sports' — David Goldblatt in 'The Games' (2016); der Kurzausdruck 'Sportswashing' wurde 2018 durch Medienberichte ueber Katar-WM popularisiert",
        correctAnswer = 2,
        explanation = "Joseph Nyes Konzept der 'Soft Power' (1990) bildet die akademische Grundlage. Der Begriff 'Sportswashing' selbst ist ein journalistisch-aktivistischer Neologismus, der organisch um 2017-2018 in sozialen Medien und Presseberichten (besonders zur Katar-WM und zu Investitionen der Gulf States) entstand — ohne einen einzelnen akademischen Urheber. HRW und Amnesty International popularisierten ihn, aber keine Organisation 'erfand' ihn in einem Paper.",
        difficulty = 5,
        funFact = "Katar investierte fuer die WM 2022 geschaetzte 220 Milliarden USD — das ca. 10-Fache aller vorherigen WM-Kosten zusammen. Saudi-Arabiens Public Investment Fund (PIF) hat seit 2021 mehr als 6 Milliarden USD in Sport investiert. Kritiker nennen es 'die teuerste PR-Kampagne der Geschichte'."
    ),

    // Question 33 – NFL franchise value growth
    Question(
        categoryId = 6,
        questionText = "NFL-Franchises haben die hoechsten Marktwerte aller Sportteams weltweit. Welches oekonomische Merkmal der NFL erklaert, warum der Marktwert von Franchises auch bei sportlich schlechten Teams kontinuierlich steigt, und welche NFL-Regel sichert diese Wertentwicklung strukturell ab?",
        answerA = "Revenue Sharing: 60% aller nationalen Einnahmen (TV, Sponsoring, Merchandise) werden gleichmaessig auf alle 32 Teams verteilt; kombiniert mit einer 'Hard Salary Cap' verhindert dies, dass reiche Teams den Markt leerkaufen — selbst schlechte Teams profitieren vom Gesamt-TV-Deal",
        answerB = "Die Anti-Trust-Ausnahme der NFL (Sports Broadcasting Act 1961) erlaubt kollektive TV-Verhandlungen; kombiniert mit Relegations-Verbot (kein Abstieg) sichert jede Franchise dauerhaften Marktwert",
        answerC = "Das Franchise-System (keine freie Gruendung neuer Teams) und der Draft (schlechteste Teams waehlen zuerst) schaffen kuenstliche Knappheit und Wettbewerbs-Balance, was TV-Attraktivitaet maximiert",
        answerD = "Alle drei Faktoren A, B und C wirken zusammen; isoliert erklaert keiner davon allein die Wertentwicklung — es ist ein System aus Anti-Trust-Ausnahme, Revenue Sharing und Franchise-Knappheit",
        correctAnswer = 3,
        explanation = "Der NFL-Marktwert ergibt sich aus dem Zusammenspiel: Revenue Sharing (nationale Einnahmen gleichmaessig verteilt), Anti-Trust-Ausnahme (kollektive TV-Verhandlungen moeglich, was gigantische Deals ermoeglicht), Hard Salary Cap (Wettbewerbsbalance) und Franchise-Knappheit (begrenzte Teamzahl, kein Abstieg, kein freier Markteintritt). Isoliert erklaert kein einzelner Faktor die kontinuierliche Wertsteigerung vollstaendig.",
        difficulty = 5,
        funFact = "Das Dallas Cowboys-Franchise wurde 2024 auf ca. 10 Milliarden USD bewertet — mehr als Real Madrid, Manchester United und alle anderen Sportfranchises der Welt. Dalllas hat seit 1995 keinen Super Bowl gewonnen, was zeigt, dass sportlicher Erfolg fuer den Franchisewert sekundaer ist."
    ),

    // Question 34 – Bosman ruling economic impact
    Question(
        categoryId = 6,
        questionText = "Das Bosman-Urteil des Europaeischen Gerichtshofs von 1995 revolutionierte die Sportoekonmie. Welche drei konkreten Aenderungen bewirkte das Urteil, und welcher belgische Spieler klagte mit welchem Argument, das der EuGH akzeptierte?",
        answerA = "Jean-Marc Bosman klagte, dass Transfergebuehren fuer ausgelaufene Vertraege das Recht auf Freizuegigkeit von EU-Arbeitnehmern verletzten. Urteil: Transfergebuehren bei Vertragsende abgeschafft; Auslaenderregel (max. 3 Nicht-EU-Spieler) abgeschafft; Spieler duerfen nach Vertragsende frei wechseln",
        answerB = "Bosman klagte gegen sein belgisches Transferrecht, das ihn zwang, in Belgien zu bleiben. Urteil: Inlaendische Transferrechte ungueltig; EU-weite Freizuegigkeit fuer Profisportler; Mindestloehne in Profiligen als EU-Recht",
        answerC = "Bosman klagte gegen UEFA-Obergrenzen fuer EU-Spieler in Vereinskaders. Urteil: Nationale Kontingent-Regeln abgeschafft; Transfermarkt-Transparenzpflicht eingefuehrt; Spielerberater lizenzierungspflichtig",
        answerD = "Bosman klagte gegen seinen Klub RC Luttich und die belgische Liga wegen ungerechtfertigter Gehaltsabsenkung. Urteil: Gehaltsabsenkungen ohne Einwilligung des Spielers ungueltig; Auslaenderregeln fuer EU-Buerger abgeschafft; Transfergebuehren auf 20% gedeckelt",
        correctAnswer = 0,
        explanation = "Jean-Marc Bosman klagte 1990-1995, nachdem sein Vertrag bei RFC Luttich auslief und der Klub eine Transfergebuehr verlangte, obwohl sein Vertrag beendet war. Er argumentierte, dies verstosse gegen die EU-Arbeitnehmerfreizuegigkeit (Art. 45 AEUV). Der EuGH gab ihm recht: Transfergebuehren bei Vertragsende wurden abgeschafft; die damalige 3+2-Auslaenderregel (max. 3 Nicht-EU-Spieler) wurde als diskriminierend verworfen; Spieler koennen nach Vertragsende frei zu einem anderen EU-Klub wechseln.",
        difficulty = 5,
        funFact = "Jean-Marc Bosman selbst profitierte kaum von seinem historischen Sieg: Er spielte nach dem Urteil nie wieder in einer hoeherklassigen Liga, verfiel in Alkoholismus und Armut und lebte zeitweise von staatlichen Beihilfen. Die Millionen-Gehaltssteigerungen, die sein Urteil anderen Spielern brachte, erreichten ihn nicht."
    ),

    // Question 35 – E-Sport prize money ecosystem
    Question(
        categoryId = 6,
        questionText = "Das Dota-2-Turnier 'The International' hat seit 2011 regelmassig die hoechsten Preisgelder aller E-Sport-Events ausgezahlt. Welches einzigartige Crowdfunding-Modell finanziert das Preisgeld, und welches Team hat 'The International' am haeufigstet gewonnen (Stand 2024)?",
        answerA = "Valve verkauft 'Battle Passes' (Saisonpaesse mit kosmetischen Items), von denen 25% direkt ins Preisgeld fliessen; OG (Europa) hat The International am haeufigsten gewonnen (2018 und 2019, als erstes Back-to-Back-Team)",
        answerB = "Fangruppen koennen direkt 'Crowd-Tokens' kaufen, die 1:1 ins Preisgeld umgewandelt werden; Team Liquid hat mit 4 Titeln die meisten gewonnen",
        answerC = "Ein NFT-System von Valve verkauft Spieler-Avatare, deren Erloese ins Preisgeld fliessen; Evil Geniuses (USA) hat mit 3 Titeln am meisten gewonnen",
        answerD = "Twitch-Abonnements waehrend des Turniers mit Preisgeld-Anteil; Natus Vincere (Ukraine) fuehrt mit 5 Gesamttiteln bei The International",
        correctAnswer = 0,
        explanation = "Valve finanziert das Preisgeld bei 'The International' (TI) durch den 'Battle Pass': Ein Saison-Zusatzinhalt fuer Dota 2, von dessen Kaufpreis 25% direkt zum Preispool von TI beitragen. Das Basispreisgeld betraegt 1,6 Mio. USD, der Rest kommt aus dem Community-Crowdfunding. 2021 erreichte der Pool 40 Mio. USD — Allzeit-Rekord aller E-Sports. OG (Organisation) gewann 2018 und 2019, die einzigen Back-to-Back-Sieger in TI-Geschichte.",
        difficulty = 5,
        funFact = "OGs Triumph 2018 gilt als einer der groessten Turnierueberraschungen der E-Sport-Geschichte: Das Team formierte sich erst Wochen vor dem Turnier neu nach internen Konflikten, qualifizierte sich durch ein Last-Chance-Turnier und gewann dann den 25-Mio.-Dollar-Hauptpreis als krasser Aussenseiter."
    ),

    // ── E-Sport als anerkannte Sportart (5) ──────────────────────────────────

    // Question 36 – E-Sport legal recognition Germany
    Question(
        categoryId = 6,
        questionText = "Deutschland hat E-Sport bis 2024 nicht als offiziellen Sport im Sinne des Gemeinnuetzigkeitsrechts anerkannt. Welche zwei Kriterien fehlen E-Sport nach Ansicht des Deutschen Olympischen Sportbundes (DOSB) fuer eine Aufnahme, und welcher Unterschied besteht zu Schach und Bridge, die ebenfalls 'mental' sind?",
        answerA = "E-Sport fehlt koerperliche Aktivitaet (Mindest-Energieverbrauch 300 kcal/Stunde) und physische Risikokompetenz; Schach ist DOSB-Mitglied als 'Denksport', weil es seit 1951 anerkannt ist und Bestandsschutz hat",
        answerB = "E-Sport hat keine standortunabhaengige Infrastruktur und keine Schutzfunktion fuer Kinder/Jugendliche; Schach erfordert physische Prasenz am Brett als 'korporale Mindestanforderung'",
        answerC = "DOSB fordert 'ueberwiegende koerperliche Aktivitaet' und 'nicht-kommerziellen Charakter'; E-Sport ist zu sehr auf Profitinteressen ausgerichtet; Schach gilt als 'Geistessport' mit historischem Bestandsschutz",
        answerD = "E-Sport fehlt 'Allgemeingueltigkeit der Regelwerke' (jedes Spiel hat andere Regeln) und 'Moeglichkeit des Breitensports ohne kommerzielle Hardware'; Schach und Bridge haben universelle, publikationsfreie Regelwerke",
        correctAnswer = 2,
        explanation = "Der DOSB lehnt E-Sport-Aufnahme ab, weil erstens 'ueberwiegende koerperliche Aktivitaet' als Wesensmerkmal von Sport fehlt und zweitens E-Sport mit privatwirtschaftlichen Publisher-Interessen (Spielentwicklerfirmen kontrollieren Regelwerke) unvereinbar mit dem DOSB-Vereinsprinzip ist. Schach hat als 'Geistessport' historischen Bestandsschutz im DOSB seit den 1950ern — eine Aufnahme vor der modernen DOSB-Kriterien-Formulierung.",
        difficulty = 5,
        funFact = "Das Paradox: Der DOSB erkennt Motorsport (Formel 1) als Sport an, obwohl Fahrer weitgehend sitzen und die Maschine die Hauptarbeit leistet. E-Sportler haben dagegen nachweislich 400+ HandBewegungen pro Minute (APM) und hoeheren Kortisol-Stress als viele traditionelle Sportler. Die Grenzziehung ist akademisch umstritten."
    ),

    // Question 37 – South Korea e-sport infrastructure
    Question(
        categoryId = 6,
        questionText = "Suedkorea gilt als Wiege des professionnellen E-Sports. Welches historische Ereignis in der suedkoreanischen Wirtschaft foerderte massgeblich den E-Sport-Boom in den spaeten 1990ern, und welche staatliche Institution reguliert professionellen E-Sport in Suedkorea?",
        answerA = "Die Asien-Finanzkrise 1997-98 fuehrte zu Massenarbeitslosigkeit; die Kim Dae-jung-Regierung investierte massiv in Breitband-Internet-Infrastruktur als Konjunkturprogramm; PC-Baengs (Internet-Cafes) wurden Zufluchtsort fuer Arbeitslose; das Korea e-Sports Association (KeSPA) wurde 2000 vom Kulturministerium als offizieller Verband gegruendet",
        answerB = "Die Olympischen Spielen Seoul 1988 schufen Medien-Infrastruktur; Hyundai investierte in PC-Herstellung fuer Haushalte; das Ministry of Science and ICT gruendete 2002 den E-Sport-Verband KeSPA",
        answerC = "Samsungs Chip-Boom Mitte der 1990er finanzierte guenstige PC-Produktion; StarCraft wurde von Blizzard gezielt fuer den koreanischen Markt lokalisiert; KeSPA entstand 1998 als private Initiative grosser Telekommunikationsunternehmen",
        answerD = "Die suedkoreanische Regierung fuehrte 1999 pflichtigen Informatik-Unterricht ein; E-Sport entwickelte sich als Schulaktivitaet; das Ministry of Education gruendete 2001 den nationalen E-Sport-Verband",
        correctAnswer = 0,
        explanation = "Die IMF-Krise (Asian Financial Crisis) 1997-98 trieb Millionen Koreaner in die Arbeitslosigkeit. Die Regierung reagierte mit massiven Investitionen in Breitbandinfrastruktur — Korea wurde weltweiter Fuehrer in Internet-Geschwindigkeit. PC-Baengs (PC-Cafes) wurden guenstige Freizeitstaedte fuer Arbeitslose. StarCraft wurde der Sport dieser Generation; KeSPA (Korea e-Sports Association) wurde 2000 unter dem Kulturministerium gegruendet und ist weltweit die erste staatlich anerkannte E-Sport-Regulierungsbehoerde.",
        difficulty = 5,
        funFact = "Auf dem Hoehepunkt des StarCraft-Booms wurden professionelle StarCraft-Matches auf drei dedizierten TV-Kanaelen live uebertragen (OGN, MBC Game, Air Force e-Sports). Spitzenspieler wie Lim Yo-hwan ('SlayerS_'Boxer') wurden zu Nationalhelden mit Fan-Clubs von Hunderttausenden."
    ),

    // Question 38 – E-Sport doping
    Question(
        categoryId = 6,
        questionText = "E-Sport hat ein eigenes Doping-Problem: Leistungssteigernde Substanzen, die in traditionellen Sportarten verboten sind, werden auch von Profis eingesetzt. Welche Substanzklasse ist die verbreitetste im E-Sport, welchen Effekt versuchen Spieler damit zu erzielen, und welche Organisation fuehrt seit wann Doping-Tests in E-Sport durch?",
        answerA = "Stimulanzien (v.a. Adderall/Amphetamine) zur Steigerung von Konzentration und Reaktionszeit; ESL (Electronic Sports League) fuehrt seit 2015 Doping-Tests nach WADA-Protokoll bei bestimmten Turnieren durch",
        answerB = "Betablocker zur Reduzierung von Zittern und Herzrate unter Stress; ESIC (Esports Integrity Commission) testet seit 2016 bei allen Major-Turnieren nach angepasstem EU-Dopingrecht",
        answerC = "Koffein in Hochdosen (Energydrinks) und Modafinil (Wachheitsfoerderer); IeSF (International Esports Federation) schrieb 2013 erstmals Doping-Tests vor, angelehnt an CAS-Standards",
        answerD = "Nootropika (kognitive Enhancer wie Piracetam) und Microdosing von LSD; ESL und Riot Games testen seit 2017 gemeinsam mit der NADA auf 14 definierte Substanzen",
        correctAnswer = 0,
        explanation = "Stimulanzien — insbesondere Adderall (Amphetamin-Salze, eigentlich ADHS-Medikament) — sind die meistgenutzten Performance-Enhancer im E-Sport. Sie verbessern Konzentration, reduzieren Ermuedung und koennen Reaktionszeiten minimal verbessern. Die ESL fuehrte 2015 Doping-Tests nach WADA-Protokoll ein, nachdem Kory 'Semphis' Friesen oeffentlich zugab, bei der ESL One Cologne 2015 unter Adderall angetreten zu sein.",
        difficulty = 5,
        funFact = "Kory Friesens Gestaendnis in einem Twitch-Stream 2015 war der erste oeffentliche Doping-Skandal im E-Sport. Er beschrieb es mit den Worten 'Everyone was on Adderall', was eine oeffentliche Debatte ueber die Verbreitung von Stimulanzien im professionellen E-Sport ausloeste — ahnlich wie Ben Johnsons Gestaendnis 1988 im traditionellen Sport."
    ),

    // Question 39 – IOC e-sport recognition 2024
    Question(
        categoryId = 6,
        questionText = "Das IOC-Exekutivkomitee stimmte im Dezember 2023 fuer die Gruendung der 'Olympic Esports Federation' (OEF). Welches Land wurde als Sitz gewahlt, welche rechtliche Struktur hat die OEF, und welche Bedingung koppelt das IOC an die langfristige Foerderung des E-Sports als anerkannte Sportform?",
        answerA = "Sitz: Lausanne (Schweiz), wie alle IOC-Tochteroranisationen; OEF ist eine Schweizer Stiftung des oeffentlichen Rechts; Bedingung: E-Sport-Spiele duerfen keine Gewalt- oder Gluecksspiel-Elemente enthalten",
        answerB = "Sitz: Singapur (als asiatische E-Sport-Metropole); OEF ist eine eigenstaendige internationale NGO nach Schweizer Vereinsrecht; Bedingung: Publisher muessen Regelwerke dem IOC zur Genehmigung vorlegen",
        answerC = "Sitz: Saudi-Arabien (Riad), da dort die ersten Olympic Esports Games 2025 stattfinden; OEF ist eine IOC-Kommission ohne eigene Rechtspersoenlichkeit; Bedingung: Spielertransparenz bei Alter und Nationalitaet",
        answerD = "Sitz: Seoul (Suedkorea) als weltweite E-Sport-Hauptstadt; OEF ist als AISBL nach belgischem Recht eingetragen; Bedingung: Mindestens 50% der Teilnehmer muessen Amateurspieler sein",
        correctAnswer = 1,
        explanation = "Die Olympic Esports Federation hat ihren Sitz in Singapur — ein strategischer Entscheid angesichts Asiens dominanter Rolle im globalen E-Sport. Die OEF ist als eigenstaendige internationale NGO konzipiert, nicht als direkte IOC-Tochter. Das IOC knuepft die Foerderung daran, dass Publisher ihre Regelwerke zur IOC-Pruefung vorlegen und keine Inhalte produzieren, die olympischen Werten widersprechen (insbesondere Gewalt und Gluecksspiel).",
        difficulty = 5,
        funFact = "Singapurs Wahl als OEF-Sitz ist oekonomisch und geopolitisch motiviert: Singapur hat keine eigene E-Sport-Tradition, bietet aber stabile rechtliche Rahmenbedingungen, niedrige Unternehmens-Steuern und geografische Naehe zu den groessten E-Sport-Maerkten China, Korea und Suedostasien."
    ),

    // Question 40 – Paris 2024 breaking (breakdancing)
    Question(
        categoryId = 6,
        questionText = "Breaking (Breakdance) debuetierten bei den Olympischen Spielen 2024 in Paris als olympische Disziplin. Welches Bewertungssystem wurde verwendet, welche Athletin gewann Gold bei den Frauen in einem kontroversen Finale, und warum wurde Breaking bereits fuer die Spiele 2028 in Los Angeles aus dem Programm gestrichen?",
        answerA = "Ein 5-Kriterien-Punktsystem (Technik, Kreativitaet, Musikalitaet, Ausdruckskraft, Originalitaet) bewertet von neun Juroren; Ami (Japan) gewann Frauen-Gold; Breaking wurde gestrichen, weil LA 2028 aus Kostengruenden das Programm verkuerzt und Breaking als 'wenig TV-zugaenglich' gilt",
        answerB = "Ein Battle-Format mit direkten Juroren-Entscheidungen (5 von 9 Stimmen); Nicka (Frankreich) gewann Frauen-Gold nach einem kontroversen Entscheid gegen die Australierin Raygun; IOC strich Breaking, da es nicht genug Nationen haben, die international konkurrenzfaehig sind",
        answerC = "Ein 9-Kriterien-Bewertungssystem aehnlich Eiskunstlauf; Logan Edra (USA) gewann Frauen-Gold; Breaking wurde nach massiver Kritik an Bewertungs-Inkonsistenz von der IOC-Vollversammlung abgelehnt",
        answerD = "Ein Battle-Format mit 5 Juroren; Ami (Japan) gewann Gold bei den Frauen; Breaking wurde aus dem LA-2028-Programm gestrichen, weil das IOC Sportarten bevorzugt, die von Jugendlichen in Entwicklungslaendern ausgebbt werden koennen",
        correctAnswer = 0,
        explanation = "Das Breaking-Bewertungssystem bei Paris 2024 umfasste fuenf Kriterien (Technik, Kreativitaet, Musikalitaet, Originalitaet, Ausdruckskraft), bewertet von neun unabhaengigen Juroren. Ami (Ami Yuasa, Japan) gewann Gold bei den Frauen. Breaking wurde aus dem LA-2028-Programm entfernt, um Platz fuer Sportarten zu schaffen, die das Organisationskomitee (LA28) fuer das amerikanische Publikum attraktiver hielt — darunter Flag-Football, Kricket T20, Squash und Lacrosse.",
        difficulty = 5,
        funFact = "Die australische Breaking-Athletin Rachael 'Raygun' Gunn wurde nach ihrem Auftritt in Paris viral: Sie verlor alle ihre Battle-Matches mit 0:18 Juroren-Stimmen. Ihre teils unkonventionellen Moves (einschliesslich einer Känguru-inspirierten Pose) wurden weltweit belacht. Raygun ist ausgebildete Akademikerin und hatte sich als Pionierin des Breaking in Australien verdient gemacht — der Kontrast zum Weltklasse-Niveau in Paris war jedoch extrem."
    ),

    // Question 41 – Esport Olympics 2025 Saudi details
    Question(
        categoryId = 6,
        questionText = "Die ersten Olympic Esports Games fanden im Juni 2025 in Saudi-Arabien (Riad) statt. Welche konkrete Spieltitel wurden im Programm zugelassen, welche Nationalitaet stellte das groesste Aufgebot, und welche Kritik aeusserten Athletenverbande bezueglich des Ausrichterlands?",
        answerA = "Zugelassen: Gran Turismo 7, Fortnite Zero Build, Virtual Regatta, Chess.com, EA Sports FC; Korea stellte das groesste Aufgebot; Athleten kritisierten fehlende LGBTQ+-Schutzrechte in Saudi-Arabien und den Widerspuch zu olympischen Anti-Diskriminierungswerten",
        answerB = "Zugelassen: League of Legends, Rocket League, FIFA 24, Archery (VR), Tennis (Wii-Nachfolger); China stellte die meisten Teilnehmer; Kritik betraf fehlende Pressefreiheit und Einschraenkungen fuer weibliche Athletinnen",
        answerC = "Zugelassen: Dota 2, Counter-Strike 2, eBasketball, Cycling (VR-Simulation), Chess; USA stellte das groesste Aufgebot; Kritik galt dem Ausschluss von Open-Source-Spielen zugunsten kommerzieller Publisher",
        answerD = "Zugelassen: Fortnite, Rocket League, eArchery, Assetto Corsa (Rennsimulation), eTableTennis; Japan stellte die meisten Teilnehmer; Kritik bezog sich auf fehlende Infrastruktur und unangemessene Preisgelder",
        correctAnswer = 0,
        explanation = "Die Olympic Esports Games 2025 in Riad umfassten Titel wie Gran Turismo 7, Fortnite (Zero Build-Modus ohne Waffen-Fokus), Virtual Regatta (Segelsimulation), Chess.com und EA Sports FC. Suedkorea stellte das groesste Kontingent qualifizierter Athleten. Mehrere Athleten und Organisationen kritisierten Saudi-Arabien als Ausrichter angesichts der Kriminalisierung gleichgeschlechtlicher Beziehungen, was im direkten Widerspruch zur olympischen Non-Diskriminierungs-Charta steht.",
        difficulty = 5,
        funFact = "Saudi-Arabien hat sich als globaler E-Sport-Hub positioniert: Das Land investiert via den Saudi Esports Federation (SEF) und den PIF-Fonds Milliarden in E-Sport-Turniere, Teams und Infrastruktur. Kritiker sprechen von 'E-Sport-Sportswashing' — dem Versuch, durch E-Sports ein moderneres, jugendlicheres Image zu projizieren."
    ),

    // Question 42 – E-Sport athlete working conditions
    Question(
        categoryId = 6,
        questionText = "Professionelle E-Sportler haben trotz hoher Preisgelder oft prekare Arbeitsverhaeltnisse. Welches strukturelle Problem macht E-Sport-Vertraege juristisch einzigartig im Vergleich zu traditionellen Sportvertraegen, und welche spezifische Klausel in E-Sport-Vertraegen wurde von Spielergewerkschaften am haefigsten kritisiert?",
        answerA = "Publisher koennen das Spiel jederzeit aendern oder einstellen, was Spielervertraege wertlos machen kann; 'IP-Ownership-Klauseln' verpflichten Spieler, alle In-Game-Accounts dem Club zu uebergeben, ohne Entschaedigung",
        answerB = "E-Sport-Ligen haben keine gemeinsamen Tarifvertraege; 'Streaming-Restrictions-Klauseln' verbieten Spielern, ausserhalb offizieller Turniere zu streamen, was ihre einzige alternative Einkommensquelle blockiert",
        answerC = "E-Sport-Klubs sind oft in mehreren Laendern aktiv; 'Multi-Territorial-Klauseln' verpflichten Spieler, in beliebige Laender umzuziehen; da kein Arbeitsrecht greift, sind Weigerungen vertragsbruch",
        answerD = "Spielergewerkschaften existieren nicht; 'Scrim-Geheimhaltungsklauseln' verpflichten Spieler zu 12-14-Stunden-Trainingstagen ohne Ueberstundenausgleich, was in traditionellen Sportarten als Arbeitsrechtsverstoss waere",
        correctAnswer = 0,
        explanation = "Das fundamentale Problem: Ein E-Sport-Spieler hat einen Vertrag fuer 'Dota-2-Spieler', aber Valve kann das Spiel jederzeit grundlegend veraendern (Patch-Updates) oder theoretisch einstellen. Damit verliert der Vertrag seinen Inhalt ohne Entschaedigung. Hinzu kommen 'IP-Account-Klauseln': Profispieler muessen beim Vereinsaustritt oft In-Game-Accounts, Skins und Ranglisten-Positionen abgeben, obwohl sie diese oft jahrelang aufgebaut haben.",
        difficulty = 5,
        funFact = "North America's League of Legends Championship Series (LCS) war 2018 die erste E-Sport-Liga, die mit einer Spielergewerkschaft (NALCSPA) Basisstandards verhandelte: Mindestgehalt 75.000 USD/Jahr, Gesundheitsversorgung, Reisebedingungen. Dies gilt als Meilenstein, auch wenn die meisten anderen E-Sport-Ligen solche Standards noch nicht erreicht haben."
    ),

    // Question 43 – Paralympic world record classification
    Question(
        categoryId = 6,
        questionText = "Bei den Paralympics werden in manchen Disziplinen Weltrekorde aufgestellt, die objektiv besser sind als olympische Weltrekorde. Welche spezifische Para-Klasse und Disziplin hatte um 2020-2024 einen Weltrekord, der den olympischen Entsprechungsrekord uebertraf, und was erklaert diesen Umstand biomechanisch?",
        answerA = "100-m-Sprinter der F44-Klasse (einseitige Unterschenkelamputation mit Prothese): Blake Leepers Zeiten lagen unter dem olympischen Weltrekord; die Cheetah-Prothese ermoeglicht in der Auslaufphase (80-100 m) mehr Energierueckgabe als biologische Wadenmuskulatur",
        answerB = "Kugel-Stossen F20-Klasse (intellektuelle Behinderung): Athleten dieser Klasse erzielen regelmaessig Wuerfe ueber dem olympischen WR, da ihre Schmerzschwelle hoeher ist und sie extreme Maximalkraft einsetzen koennen",
        answerC = "Schwimmen S12-Klasse (starke Sehbehinderung): Da diese Schwimmer keine visuelle Korrektur der Wasserlage vornehmen, entwickeln sie eine hydrodynamischere Koerperposition als sehende Schwimmer",
        answerD = "Diskuswerfen F11-Klasse (voellige Blindheit): Blinde Werfer haben eine ausgepragtere propriozeptive Koordination, die praezisere Rotationsbewegungen als sehende Athleten ermoeglicht",
        correctAnswer = 0,
        explanation = "Blake Leeper (USA, F44 — einseitige Unterschenkelamputation) erzielte in der 400-m-Disziplin Zeiten, die nahe an den olympischen Weltrekorden lagen. Er und andere F44-Sprinter nutzen die biomechanische Eigenschaft der Karbonfaser-Prothese: In der Auslaufphase (Kilometer 80-100 eines 100-m-Rennens, wo Wadenmuskulatur natuerlicher Athleten ermueden) gibt die Prothese konstant gleich viel Energie zurueck, ohne Ermuedungsabfall. IAAF/World Athletics sperrte Leeper 2020 von olympischen Laeufen — dieser Prozess ist noch nicht rechtskraeftig abgeschlossen.",
        difficulty = 5,
        funFact = "Die Frage, ob Karbonfaser-Prothesen einen Vorteil gegenueber biologischen Beinen bieten, ist bis heute wissenschaftlich nicht abschliessend geklaert. Verschiedene Studien kommen zu entgegengesetzten Ergebnissen — abhaengig von der Distanz, der Kurvenfrequenz und der individuellen Biomechanik des Athleten."
    ),

    // Question 44 – Curling: Olympic history and sweep science
    Question(
        categoryId = 6,
        questionText = "Curling-Sweeping (das Buersen vor dem Stein) veraendert die Steinkurve und Distanz. Welcher wissenschaftliche Mechanismus erklaert die Wirkung des Sweepens, und welche technologische Aenderung der Besen fuehlte 2015 zu einer Regelaenderung des World Curling Federation?",
        answerA = "Sweeping erwaermt das Eis minimal durch Reibung, was einen Wasserfilm erzeugt und Reibung reduziert (Haematit-Theorie); 2015 fuehrten neue synthetische Buerstenkoepfe ('Directional Fabric') zu so praeziser Steuerung, dass WCF die Besen-Technik regulierte",
        answerB = "Buersten regen Eiskristalle auf, die sich in Fahrtrichtung ausrichten und Reibung anisotrop reduzieren; 2015 wurden Titan-Draht-Buersten verboten, da sie die Eisoberflaeche zu stark abrasiv beeinflussten",
        answerC = "Sweeping erzeugt elektrostatische Felder, die Wassermolekuele im Eis polarisieren; 2015 fuehrte der Einsatz von Hochfrequenz-Vibrationsgeraeten zu einer Verbotsregel des WCF",
        answerD = "Buesten komprimieren Mikro-Luftblasen im Eiskristallgitter und reduzieren den Reibungskoeffizienten; 2015 mussten Buersten auf einheitliche Haarlaenge reguliert werden, da asymmetrische Buersten den Stein seitlich ablenkten",
        correctAnswer = 0,
        explanation = "Sweeping erzeugt durch Reibungswaerme einen mikroskopisch duennen Wasserfilm auf dem Eis, der den Gleitreibungskoeffizienten des Steins reduziert. Direktionale Faserbuersen ('IcePad' und aehnliche Fabrics), die 2015 aufkamen, ermoeglichten durch gerichtetes Buersen nicht nur mehr Distanz, sondern auch praezise Richtungssteuerung. WCF schraenkte 2016 zugelassene Besen-Materialien ein, da das traditionelle 'Shotmaking'-Konzept sonst bedeutungslos geworden waere.",
        difficulty = 5,
        funFact = "Der 'Broomgate'-Skandal 2015 (wie die Besen-Kontroverse genannt wurde) spaltete die Curling-Community. Einige Teams trainierten heimlich mit den neuen Directional-Fabric-Buersten, was andere als 'technisches Doping' bezeichneten. Die WCF-Entscheidung, die Besen zu regulieren, gilt als eine der umstrittensten Regelaenderungen in der Curling-Geschichte."
    ),

    // Question 45 – E-Sport gender gap
    Question(
        categoryId = 6,
        questionText = "E-Sport hat trotz niedrigerer physischer Barrieren als traditionelle Sportarten eine der groessten Geschlechterdisparitaeten im Profisport. Welche drei strukturellen Faktoren erklaert die Spielwissenschaft als Hauptursachen, und welches Spiel hat die hoechste Frauenbeteiligung im Profisegment (Stand 2024)?",
        answerA = "1) Gamergate und Online-Harassment schufen feindselige Umgebungen; 2) Biologische Unterschiede in Reaktionszeit bevorteilen Maenner (umstritten); 3) Mangel an Rollvorbildern. Valorant hat den hoechsten Frauenanteil im Profibereich, mit dedizierten 'Game Changers'-Circuits",
        answerB = "1) Hardware-Kosten bevorteilen Haushalte mit groesseren Gaming-Budgets (historisch eher maennlich); 2) MOBA-Communities haben ausgepragte toxische Kulturen; 3) Publisher vermarkten zielgruppenspezifisch an Maenner. League of Legends fueehrt mit 18% Frauenanteil im Profibereich",
        answerC = "1) Fehlende Frauenfoerderungs-Programme bis 2010; 2) Spiele wurden technisch mit Mechaniken entwickelt, die maennliche Spielstile bevorteilen; 3) Sponsoren investierten nur in maennliche Teams. Overwatch hat die ausgeglichenste Geschlechterverteilung mit 22% Frauen",
        answerD = "1) Kulturelle Stereotypen und fehlende Eltern-Unterstuetzung fuer gamedende Maedchen; 2) Team-hierarchien in E-Sport bevorzugen maennliche Fuehrungsstile; 3) Preisgelder fuer Frauen-Turniere sind 90% niedriger. Hearthstone (Kartenspiel) hat die hoechste Frauenbeteiligung im kompetitiven Bereich",
        correctAnswer = 0,
        explanation = "Spielwissenschaftliche Forschung (Witkowski 2012, Taylor 2012, Paul 2018) identifiziert mehrere Schichten: Online-Harassment (verstaerkt durch 'Gamergate' 2014) machte E-Sport fuer Frauen strukturell unsicherer; fehlende Rollvorbilder schufen psychologische Barrieren; Community-Toxizitaet in kompetitiven Spielen trifft Frauen ueberproportional. Valorant hat mit Riots 'Game Changers' (ein dediziertes Turniersystem fuer Frauen und marginalisierte Geschlechter) die hoechste institutionelle Frauenfoerderung.",
        difficulty = 5,
        funFact = "Das 'Gamergate'-Ereignis 2014 gilt als Wendepunkt: Nach koordinierten Online-Angriffen gegen Spieleentwicklerinnen und Journalistinnen zogen sich viele Frauen aus sichtbaren Rollen im Gaming zurueck. Studien zeigten danach eine messbare Zunahme von Frauen, die Online-Spiele mit Gender-neutralen Usernamen spielten, um Harassment zu vermeiden."
    )
)
