package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsHard2(): List<Question> = listOf(

    // ── Segeln / Americas Cup (7) ─────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fand der allererste Americas Cup statt, und welches Schiff gewann gegen die gesamte britische Yacht-Flotte rund um die Isle of Wight?",
        answerA = "1851 – die Yacht 'America'",
        answerB = "1870 – die Yacht 'Magic'",
        answerC = "1883 – die Yacht 'Puritan'",
        answerD = "1895 – die Yacht 'Defender'",
        correctAnswer = 0,
        explanation = "1851 segelte die amerikanische Schoner-Yacht 'America' rund um die Isle of Wight und besiegte alle britischen Konkurrenten. Der anschliessend gestiftete Pokal traegt seitdem ihren Namen und ist der aelteste internationale Sportpokal, der noch ausgetragen wird.",
        difficulty = 3,
        funFact = "Als Koeinigin Victoria fragte, wer als Zweiter die Ziellinie ueberquert hat, soll die Antwort gelautet haben: 'Your Majesty, there is no second.' – ein Satz, der in die Segelgeschichte einging."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Deed of Gift' im Kontext des Americas Cup und welche rechtliche Funktion hat es?",
        answerA = "Ein internationaler Vertrag zwischen Segelverhaenden zur Regelung von Dopingtests",
        answerB = "Das Stiftungsdokument von 1887, das die Regeln und Bedingungen des Americas Cup festlegt",
        answerC = "Eine Versicherungspolice fuer Americas-Cup-Jachten",
        answerD = "Der Kaufvertrag der Yacht 'America' von 1851",
        correctAnswer = 1,
        explanation = "Das 'Deed of Gift' von 1887 ist das rechtlich bindende Stiftungsdokument, das die Grundregeln des Americas Cup festlegt. Es bestimmt, wer den Pokal verteidigen darf, unter welchen Bedingungen Herausforderungen angenommen werden muessen, und hat Vorrang vor allen anderen Regelwerken – Streitigkeiten darueber landen regelmaessig vor Gericht.",
        difficulty = 3,
        funFact = "Das Deed of Gift wurde dreimal durch Gerichtsbeschluss interpretiert – das letzte Mal 2007, als das Oracle-Team BMW versuchte, mit einem Mehrrumpfboot anzutreten, was zu einem jahrelangen Rechtsstreit fuehrte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche technische Revolution beim Americas Cup 34 (2013 in San Francisco) veraenderte den Segelsport fundamental?",
        answerA = "Einfuehrung von GPS-gesteuertem Autopiloten fuer Taktikentscheidungen",
        answerB = "Fluegelfoils die Katamarane vollstaendig aus dem Wasser heben und 'foilen' lassen",
        answerC = "Kohlefaser-Masten die sich automatisch an den Windwinkel anpassen",
        answerD = "Unterwasser-Propeller fuer motorunterstuetzte Manoeuverphasen",
        correctAnswer = 1,
        explanation = "Beim 34. Americas Cup 2013 setzten beide Teams (Oracle Team USA und Emirates Team New Zealand) AC72-Katamarane mit Foils ein, die das gesamte Boot aus dem Wasser hoben. Die Boote erreichten Geschwindigkeiten von ueber 80 km/h – weit mehr als doppelt so schnell wie der Wind. Dies markierte den Beginn der 'Foiling-Aera' im Profisegeln.",
        difficulty = 3,
        funFact = "Oracle Team USA lag im Finale 8:1 zurueck und gewann dann acht Rennen in Folge, um den Cup zu behalten – das groesste Comeback in der Geschichte des Americas Cup."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land hat bisher die meisten Americas-Cup-Verteidigungen gewonnen, und in welchem Jahr wurde diese Dominanz nach 132 Jahren zum ersten Mal gebrochen?",
        answerA = "Australien – erstmals gebrochen 1983 durch die USA",
        answerB = "USA – erstmals gebrochen 1983 durch Australien",
        answerC = "Grossbritannien – erstmals gebrochen 1958 durch die USA",
        answerD = "Neuseeland – erstmals gebrochen 2021 durch Bermuda",
        correctAnswer = 1,
        explanation = "Die USA verteidigten den Americas Cup von 1851 bis 1983 ununterbrochen – 132 Jahre und 25 Verteidigungen. 1983 gewann Australien II unter Skip John Bertrand mit dem revolutionaeren Keel-Design ('Winged Keel') und brach die laengste Siegesserie der Sportgeschichte.",
        difficulty = 3,
        funFact = "Australiens Premierminister Bob Hawke erklaerte nach dem Sieg, jeder Arbeitgeber der seinen Mitarbeitern nicht frei gab, um die Siegesfeier zu begehen, 'ein echter Schuft' sei – und dies live im australischen Fernsehen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet man im Segeln als 'IRC Handicap-System', und bei welchem Rennen ist es das massgebliche Wertungssystem?",
        answerA = "Ein elektronisches System zur automatischen Kurskorrektur bei Sturmwetter",
        answerB = "Ein mathematisches Zeitgutschrift-System, das unterschiedliche Bootsklassen vergleichbar macht",
        answerC = "Ein Punktesystem fuer Regattasiege ueber eine gesamte Saison",
        answerD = "Eine Sicherheitsklassifizierung fuer Hochseerennen",
        correctAnswer = 1,
        explanation = "Das International Rating Certificate (IRC) ist ein Handicap-System, das auf Basis von Rumpf, Rigg und Segelflaeche einen Korrekturfaktor (TCF) berechnet. Damit koennen verschieden grosse und schnelle Jachten gegeneinander antreten. Bei Rennen wie dem Rolex Fastnet Race oder der Sydney-Hobart-Regatta ist IRC das Hauptwertungssystem.",
        difficulty = 3,
        funFact = "Die Sydney-Hobart-Regatta von 1998 gilt als Katastrophen-Rennen: Bei einem extremen Sturm sanken 5 Boote, 6 Menschen starben, und nur 44 von 115 gestarteten Yachten erreichten Hobart."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter dem taktischen Begriff 'Leebow' beim Regatta-Segeln?",
        answerA = "Eine Wende direkt vor dem Bug eines anderen Bootes, um es im Abwind zu behindern",
        answerB = "Ein Startmanoeuvre bei dem man von Lee aus beschleunigt",
        answerC = "Das Segeln exakt auf Gegenkurs zum Wind ohne Wenden",
        answerD = "Eine Halse in ruhigem Wasser durch den Windschatten einer Landmasse",
        correctAnswer = 0,
        explanation = "Der 'Leebow' (lee bow) ist eine aggressive Regatta-Taktik: Man kreuzt knapp vor dem Bug des Gegners und wendet auf dessen Luvseite (Windseite). So faehrt man in den sauberen Wind, waehrend der Gegner im gestoerten Abwind steckt und langsamer wird. Richtig ausgefuehrt ist es eine der wirksamsten Behinderungstaktiken.",
        difficulty = 3,
        funFact = "Im Racing Rules of Sailing gibt es genaue Abstandsregeln: Ein Boot das wendet muss dem anderen ausreichend Raum lassen – sonst ist es eine Regeluebertretung. Die Abgrenzung zwischen legalem 'Leebow' und illegalem 'Tacking too close' entscheidet oft Protest-Verhandlungen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Team und welcher Skipper gewannen den Americas Cup 2021 in Auckland, und mit welchem Bootstyp?",
        answerA = "Oracle Team USA – Jimmy Spithill – AC75 Monohull-Foiler",
        answerB = "Luna Rossa Prada Pirelli – Francesco Bruni – AC75 Monohull-Foiler",
        answerC = "Emirates Team New Zealand – Peter Burling – AC75 Monohull-Foiler",
        answerD = "INEOS Team UK – Ben Ainslie – AC75 Monohull-Foiler",
        correctAnswer = 2,
        explanation = "Emirates Team New Zealand verteidigte 2021 in Auckland den Americas Cup erfolgreich unter Skipper Peter Burling. Beide Teams (Luna Rossa als Herausforderer) segelten AC75 – einrumpfige Monohull-Foiler, die sich durch hydraulisch angetriebene T-Foils selbst aus dem Wasser heben. Es war das erste Mal, dass Einrumpfboote fuer den Cup foilten.",
        difficulty = 3,
        funFact = "Peter Burling war beim Gewinn 2017 erst 26 Jahre alt und der juengste Americas-Cup-Skipper aller Zeiten. 2021 verteidigte er den Cup – und Neuseeland war damit das erste nicht-amerikanische Team, das erfolgreich verteidig hatte."
    ),

    // ── Schach-Geschichte (7) ─────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welcher Schachweltmeister gilt als erster offizieller Weltmeister der Geschichte, und in welchem Jahr fand der erste offizielle Weltmeisterschaftskampf statt?",
        answerA = "Paul Morphy – 1858",
        answerB = "Wilhelm Steinitz – 1886",
        answerC = "Emanuel Lasker – 1894",
        answerD = "Adolf Anderssen – 1870",
        correctAnswer = 1,
        explanation = "Wilhelm Steinitz gilt als erster offizieller Schachweltmeister. 1886 besiegte er Johannes Zukertort in einem Wettkampf in New York, St. Louis und New Orleans mit 12,5:7,5 Punkten. Steinitz gilt als Begruender der modernen positionalen Schachtheorie.",
        difficulty = 3,
        funFact = "Paul Morphy dominierte die Schachwelt 1857-1859 so vollstaendig, dass er sich fuer unschlagbar hielt. Er hoerte auf, professionell Schach zu spielen, weil er fand, das Spiel sei zu leicht – und lebte danach ein zunehmend isoliertes Leben."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war das 'Unsterbliche Spiel' (Immortal Game) von 1851 und wer spielte es?",
        answerA = "Karpow vs. Kasparow, 1985 WM-Kampf, Spiel 16",
        answerB = "Adolf Anderssen vs. Lionel Kieseritzky, ein Freundschaftsspiel in London",
        answerC = "Paul Morphy vs. Herzog von Braunschweig, Paris Opera 1858",
        answerD = "Wilhelm Steinitz vs. Zukertort, WM-Kampf 1886",
        correctAnswer = 1,
        explanation = "Das 'Unsterbliche Spiel' spielten Adolf Anderssen und Lionel Kieseritzky am 21. Juni 1851 in London waehrend einer Spielpause des ersten internationalen Schachturniers. Anderssen opferte beide Tuerme, einen Laeufer und seine Dame – und gewann trotzdem brillant. Es gilt als eines der schoensten Schachpartien aller Zeiten.",
        difficulty = 3,
        funFact = "Die Bezeichnung 'Immortal Game' pragte erst spaeter der Schachhistoriker Ernst Falkbeer. Das Spiel war nicht offiziell gewertet – es entstand spontan als Blitzpartie zwischen den Turnierrunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Was machte Bobby Fischers WM-Kampf 1972 gegen Boris Spassky in Reykjavik historisch aussergewoehnlich?",
        answerA = "Es war der erste WM-Kampf mit Computerhilfe fuer die Ereoeffnungsvorbereitung",
        answerB = "Es war der erste WM-Kampf im Kalten Krieg zwischen einem Amerikaner und einem Sowjeten, mit enormer geopolitischer Bedeutung",
        answerC = "Fischer gewann ohne eine einzige Niederlage – die erste perfekte WM-Performance",
        answerD = "Es war der erste WM-Kampf der live im Fernsehen uebertragen wurde",
        correctAnswer = 1,
        explanation = "Der Fischer-Spassky-Kampf 1972 ('Match of the Century') war weit mehr als Sport: Er stand symbolisch fuer den Kalten Krieg zwischen den USA und der UdSSR. Fischer forderte extreme Bedingungen, erschien zu Spielen zu spaet, gewann trotzdem 12,5:8,5 – und wurde in den USA als nationaler Held gefeiert. Es war der erste WM-Titel eines Amerikaners.",
        difficulty = 3,
        funFact = "Fischer erschien nicht zu Spiel 2 und verlor kampflos – was ihn 0:2 in Rueckstand brachte. Dann gewann er Spiel 3 in einem anderen Raum ohne Zuschauer-Kameras, weil ihm die Kameras zu laut waren. Die restlichen neun Spiele gewann er mit ueberlegener Spielstaerke."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'Zugzwang' im Schach, und in welcher Partiephase tritt er am haeufigsten auf?",
        answerA = "Die Pflicht, innerhalb einer bestimmten Zeit zu ziehen – haeufig im Eroeffnungsspiel",
        answerB = "Eine Position, in der jeder moegliche Zug die eigene Stellung verschlechtert – haeufig im Endspiel",
        answerC = "Ein erzwungenes Matt in maximal drei Zuegen – typisch im Mittelspiel",
        answerD = "Die Situation, in der man einen Bauern opfern muss – am haeufigsten in der Eroeffnung",
        correctAnswer = 1,
        explanation = "Zugzwang (aus dem Deutschen: 'Zwang zum Ziehen') beschreibt eine Position, in der der Spieler am Zug lieber nicht ziehen wuerde – weil jeder Zug seine Stellung verschlechtert. Im Endspiel (besonders Koenigs- und Bauer-Endspiele) ist Zugzwang ein entscheidendes strategisches Konzept, da der Koenig oft in Oppositon gehalten wird.",
        difficulty = 3,
        funFact = "Zugzwang ist ein deutsches Lehnwort, das direkt in die englische Schachsprache uebernommen wurde – eines von vielen (auch 'Endgame', 'Gambit', 'Blitz' haben deutsche oder deutsche Wurzeln)."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Partien umfasste der historische WM-Kampf 1984/85 zwischen Anatoli Karpow und Garri Kasparow, bevor er ohne Ergebnis abgebrochen wurde?",
        answerA = "24 Partien",
        answerB = "36 Partien",
        answerC = "48 Partien",
        answerD = "62 Partien",
        correctAnswer = 3,
        explanation = "Der WM-Kampf Karpow vs. Kasparow 1984/85 dauerte 48 gespielte Partien ueber fuenf Monate und wurde dann ohne Sieger abgebrochen – ein Novum in der WM-Geschichte. FIDE-Praesident Campomanes brach ab, als Kasparow aufgeholt hatte. Im Folgejahr 1985 gewann Kasparow den Retourkampf und wurde jueingster Weltmeister aller Zeiten (22 Jahre).",
        difficulty = 3,
        funFact = "Karpow verlor in den ersten neun Partien kein einziges Spiel und fuehrte 5:0 – doch dann spielte Kasparow so stark, dass er bis auf 5:3 aufholte. Der Abbruch durch Campomanes ist bis heute umstritten; viele glauben, die Sowjet-Fide wollte Karpow schuetzen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Eroeffnung traegt den Spitznamen 'Gift der Eroeffnungen', und warum?",
        answerA = "Sizilianische Verteidigung – weil sie komplexe, riskante Varianten erzeugt die viele Spieler nicht beherrschen",
        answerB = "Koenigsindische Verteidigung – weil Schwarz den Koenig temporaer in Gefahr bringt",
        answerC = "Narrenmatt – weil Weiss damit in zwei Zuegen mattsetzen kann",
        answerD = "Nimzo-Indische Verteidigung – weil sie statische Schwaechen gegen dynamisches Spiel tauscht",
        correctAnswer = 0,
        explanation = "Die Sizilianische Verteidigung (1.e4 c5) gilt als 'Gift der Eroeffnungen', weil sie statistisch die erfolgreichste Antwort auf 1.e4 ist – aber gleichzeitig extrem komplex und variationsreich. Schwarz uebernimmt von Beginn an die Initiative, akzeptiert aber oft strukturelle Nachteile. Weltmeister von Fischer bis Kasparow spielten sie regelmaessig.",
        difficulty = 3,
        funFact = "Die Sizilianische Verteidigung wurde erstmals 1594 vom Giulio Polerio erwahnt, aber erst im 20. Jahrhundert zur dominanten Eroeffnung. Heute machen Sizilianische Varianten etwa 25% aller Meisterschaftspartien aus."
    ),

    Question(
        categoryId = 6,
        questionText = "Was unterscheidet die FIDE-Weltmeisterschaft nach 1993 von der zuvor einheitlichen WM-Tradition, und wie wurde die Spaltung wieder beendet?",
        answerA = "1993 gruendete Kasparow eine Rival-Organisation (PCA); 2006 wurden beide Titel beim WM-Kampf Kramnik vs. Topalow vereint",
        answerB = "1993 einigte sich FIDE auf K.O.-Format; 2013 kehrte man zum Wettkampfformat zurueck",
        answerC = "1993 spaltete sich der Frauenschach-Weltverband ab; 2000 Wiedervereinigung",
        answerD = "1993 verbot FIDE Computerprogramme; 2006 wurden sie wieder zugelassen",
        correctAnswer = 0,
        explanation = "Als Garri Kasparow 1993 seinen WM-Kampf gegen Short ausserhalb der FIDE-Struktur ausrichtete, erkannte die FIDE den Wettkampf nicht an und kuerste Karpow zum 'FIDE-Weltmeister'. Kasparow gruendete die Professional Chess Association (PCA). Erst 2006 beim Wettkampf Kramnik vs. Topalow wurden beide Titel wieder vereint – Vladimir Kramnik setzte sich durch.",
        difficulty = 3,
        funFact = "Dieser Zustand war verwirrend: Von 1993 bis 2006 gab es gleichzeitig zwei 'Schachweltmeister'. Kasparows Titel wurde von der Schachgemeinschaft breiter anerkannt, da er als staerkster Spieler galt."
    ),

    // ── Triathlon / Ironman (6) ───────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welche drei Disziplinen und Distanzen umfasst ein Ironman-Triathlon in der 'Long Distance' (70.3 und 140.6)?",
        answerA = "70.3: 1,9km Schwimmen, 90km Radfahren, 21,1km Laufen / 140.6: 3,86km, 180,2km, 42,2km",
        answerB = "70.3: 1,5km Schwimmen, 80km Radfahren, 20km Laufen / 140.6: 3km, 160km, 42km",
        answerC = "70.3: 2km Schwimmen, 90km Radfahren, 21km Laufen / 140.6: 4km, 180km, 40km",
        answerD = "70.3: 1km Schwimmen, 70km Radfahren, 21km Laufen / 140.6: 2km, 140km, 42km",
        correctAnswer = 0,
        explanation = "Ein Ironman 70.3 (Half Ironman) umfasst: 1,9 km Schwimmen + 90 km Radfahren + 21,1 km Halbmarathon = 113 km Gesamtdistanz. Ein Ironman 140.6 (Full Ironman): 3,86 km + 180,2 km + 42,195 km Marathon = 226,3 km. Die Zahl im Namen ist die Meilen-Gesamtdistanz: 70.3 Meilen bzw. 140.6 Meilen.",
        difficulty = 3,
        funFact = "Der erste Ironman-Triathlon entstand 1978 auf Hawaii aus einer Wette unter US-Marines: Wer ist die haertere Sportler – Schwimmer, Radfahrer oder Laeufer? Commander John Collins kombinierte drei bestehende Rennen und sagte: 'Whoever finishes first, we'll call him the Iron Man.'"
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet man beim Triathlon als 'T1' und 'T2', und warum sind diese Phasen strategisch wichtig?",
        answerA = "Die erste und zweite Trainingseinheit am Tag; strategisch fuer die Tapering-Phase",
        answerB = "Transition-Zonen zwischen Schwimmen/Radfahren (T1) und Radfahren/Laufen (T2); Zeit wird zur Gesamtzeit gezaehlt",
        answerC = "Taktische Angriffspunkte auf der Radstrecke – Steigung 1 und 2",
        answerD = "Zeitlimit-Punkte auf der Strecke, nach denen man disqualifiziert wird",
        correctAnswer = 1,
        explanation = "T1 (erste Transition: Schwimmen → Radfahren) und T2 (zweite Transition: Radfahren → Laufen) sind Wechselzonen, in denen der Athlet Ausstattung wechselt (Neoprenanzug ausziehen, Helm aufsetzen, Rad nehmen / Laufschuhe anziehen). Die Zeit in den Transitions zaehlt zur Gesamtzeit – Profis trainieren Transitions intensiv, um Sekunden zu sparen.",
        difficulty = 3,
        funFact = "Profi-Triathleten koennen T1 in unter 60 Sekunden absolvieren: Neopren 'peelen', Helm auf, Brille drauf, Schuhe am Rad fixiert anziehen, Rad greifen – alles waehrend sie laufen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schweizer dominierte den Ironman-Triathlon in den 2010er Jahren und stellte 2019 einen neuen Weltrekord auf der Hawaii-Strecke auf?",
        answerA = "Alistair Brownlee",
        answerB = "Jan Frodeno",
        answerC = "Patrick Lange",
        answerD = "Sebastian Kienle",
        correctAnswer = 1,
        explanation = "Jan Frodeno (Deutsch-Australier, geboren in Koeln) dominierte den Ironman World Championship auf Hawaii mit Siegen 2015, 2016 und 2019. Im Oktober 2019 stellte er mit 7:51:13 einen neuen Streckenrekord auf – die erste Sub-7:52 in der Geschichte von Kona. Er ist dreifacher Olympiasieger (2008 Peking) und dreifacher Ironman-Weltmeister.",
        difficulty = 3,
        funFact = "Frodeno lief 2020 waehrend des COVID-Lockdowns einen kompletten Ironman auf einem Hometrainer, im Pool und auf dem Laufband in seiner Garage – live gestreamt. Er absolvierte die 226 km in 7:27:53 und stellte damit einen 'Indoor Ironman'-Rekord auf."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Kona-Slot'-System beim Ironman World Championship, und wie qualifiziert man sich?",
        answerA = "Ein Lotterie-Verfahren fuer Amateure; Profis qualifizieren sich automatisch",
        answerB = "Qualifikationsplaetze werden an Altersklassen-Athleten bei Ironman-Qualifikationsrennen vergeben; Anzahl der Slots pro Rennen haengt von Starterzahl ab",
        answerC = "Die schnellsten 100 Finisher aller weltweiten Ironman-Rennen eines Jahres kommen automatisch",
        answerD = "Man kauft einen 'Legacy Slot' nach 12 Ironman-Finishes",
        correctAnswer = 1,
        explanation = "Das 'Kona Slot'-System verteilt begrenzte Startplaetze (Slots) fuer den Ironman World Championship in Kona/Hawaii an Altersklassen-Athleten. Bei jedem offiziellen Ironman-Qualifikationsrennen werden Slots pro Altersklasse vergeben – je groesser das Rennen, desto mehr Slots. Die Sieger jeder Altersklasse erhalten zuerst einen Slot, dann werden ungenommene Plaetze nach unten verteilt.",
        difficulty = 3,
        funFact = "Neben dem Leistungs-System gibt es tatsaechlich auch ein 'Legacy Program': Athleten, die 12 Ironman-Finishes ohne Kona-Start vorweisen, werden in eine Lotterie aufgenommen. Tausende Athleten warten jahrelang auf diese Chance."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Swimrun'-Format und wie unterscheidet es sich grundlegend vom Triathlon?",
        answerA = "Swimrun kombiniert mehrfaches Schwimmen und Laufen im Wechsel – Partner-Team ohne separate Transition-Zonen, mit einem Anzug fuer beides",
        answerB = "Swimrun ist Triathlon ohne Radfahren – Schwimmen und Laufen in beliebiger Reihenfolge",
        answerC = "Swimrun ist eine offizielle olympische Disziplin seit 2020, bei der Schwimmen und Laufen kombiniert werden",
        answerD = "Swimrun ist eine Trainingsmethode fuer Triathleten, keine eigenstaendige Wettkampfsportart",
        correctAnswer = 0,
        explanation = "Swimrun (Ursprung: ÖTILLÖ-Rennen in Schweden, 2002) kombiniert mehrfaches, abwechselndes Schwimmen und Laufen. Teams treten zu zweit an und sind mit einer Leine verbunden. Man traegt waehrend des gesamten Rennens denselben Neoprenanzug (Laeuft und schwimmt darin), nimmt Schwimmpaddles und Pullbuoy mit auf die Laufstrecke. Es gibt keine Transition-Zonen.",
        difficulty = 3,
        funFact = "Das erste ÖTILLÖ (schwedisch: 'Insel zu Insel') entstand aus einer Wette: Zwei betrunkene Schweden behaupteten, sie koennen von einem Hotel in Stockholm zu einem anderen schwimmen und laufen. Das Rennen fuehrt durch 75 schwedische Schaerengarten-Inseln, 10 km Schwimmen, 65 km Laufen."
    ),

    // ── Wintersport-Technik (7) ───────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Carving-Ski' und welche biomechanische Eigenschaft unterscheidet ihn von traditionellen Alpinski?",
        answerA = "Ein Ski mit Metallkanten fuer Eispartien; der traditionelle Ski hat keine Metallkanten",
        answerB = "Ein Ski mit stark ausgepraegter taillierter Form (grosse Taillierung/Side-Cut), der durch Kantendruck allein Kurven schneidet ohne Verdriften",
        answerC = "Ein leichterer Ski ohne Stahlkern, entwickelt fuer Freestyleskifahren",
        answerD = "Ein breiterer Ski speziell fuer Tiefschnee, der das Gleiten auf lockerem Schnee verbessert",
        correctAnswer = 1,
        explanation = "Der Carving-Ski (taillierter Ski) hat eine ausgepraegt schmale Taille verglichen mit Spitze und Ende ('Hourglass-Shape'). Wenn der Ski auf die Kante gestellt wird, formt sein seitliches Profil automatisch einen Kreisbogen – der Ski schneidet ('carves') die Kurve ohne zu rutschen. Das Seitenspiegelmass (Taillierungsradius) bestimmt den Kurvenradius. In den 1990ern loeste er den klassischen geraden Ski ab.",
        difficulty = 3,
        funFact = "Die Taillierungsidee existierte schon im 19. Jahrhundert (Sondre Norheim erfand um 1864 taillierte Ski fuer Telemark), wurde aber erst 1993 von Elan und K2 mit modernen Materialien kommerziell erfolgreich. Innerhalb von fuenf Jahren verdrengten Carving-Ski den Markt vollstaendig."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche physikalische Groesse beschreibt den 'Gleitreibungskoeffizienten' von Langlauf-Ski-Wachs auf Schnee, und wie veraendert er sich mit der Temperatur?",
        answerA = "Der Koeffizient steigt mit sinkender Temperatur – je kaelter, desto besser gleiten harte Wachse",
        answerB = "Der Koeffizient ist temperaturunabhaengig; nur Schneetyp und Feuchtigkeit sind relevant",
        answerC = "Der Koeffizient ist bei -4°C optimal (Phasenuebergang Eis-Wasser); zu kalt oder zu warm erhoehen Reibung",
        answerD = "Der Koeffizient sinkt mit sinkender Temperatur – je waermer, desto besser gleiten weiche Wachse",
        correctAnswer = 3,
        explanation = "Langlauf-Wachs nutzt einen duennen Wasserfilm, der beim Gleiten zwischen Ski und Schnee entsteht (Reibungswaerme). Bei waermerem Schnee entsteht dieser Film leichter, sodass weiche (waermere) Wachse besser gleiten. Bei sehr kaltem Schnee (-15°C und kaelter) dominieren Fluor-Gleitmittel. Hartes Wachs bei Kaelte, weiches Wachs bei Waerme – das ist die Grundregel.",
        difficulty = 3,
        funFact = "Professionelle Waxing-Techniker bei Olympischen Winterspielen sind hochspezialisierte Experten. Sie beobachten Temperatur, Luftfeuchtigkeit, Schneetyp und -alter und mischen oft proprietary Fluoro-Compound-Wachse die Teams wie Staatsgeheimnisse hueten."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist die 'Schlittentechnik' im Skeleton und wie unterscheidet sie sich von Bob und Rennrodeln?",
        answerA = "Skeleton: Bauchlage vorwaerts / Bob: Mehrsitzer mit Lenkung / Rennrodeln: Rueckenlage fuessevoran",
        answerB = "Skeleton: Rueckenlage / Bob: Bauchlage / Rennrodeln: Seitenlage mit Gewichtsverlagerung",
        answerC = "Skeleton: stehend wie ein Snowboard / Bob: sitzend / Rennrodeln: kniend",
        answerD = "Skeleton: Bauchlage rueckwaerts / Bob: Sitzen / Rennrodeln: Bauchlage vorwaerts",
        correctAnswer = 0,
        explanation = "Skeleton wird in Bauchlage kopfvoran (vorwaerts) gefahren – der Athlet liegt auf einem einfachen Stahlschlitten und lenkt durch Schulter- und Koerperdruck sowie durch spezielle Spikes an den Schuhen. Bob wird sitzend in einem aerodynamischen Schlitten mit Lenkmechanismus gefahren. Rennrodeln erfolgt in Rueckenlage, Fuesse zuerst, auf einem Rodel.",
        difficulty = 3,
        funFact = "Skeleton-Piloten erreichen auf der Bobbahn Geschwindigkeiten von ueber 130 km/h – liegend, ohne Schutzhelm bis 2004, mit dem Gesicht nur Zentimeter ueber dem Eis. Der Name 'Skeleton' stammt vom englischen Wort fuer 'Gerippe', da der fruehe Metallschlitten wie ein Knochengerippe aussah."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Supercompensation' im Kontext des Skisprung-Trainings, und wie nuetzt es den Athleten?",
        answerA = "Eine ueberanstrengende Trainingsphase kurz vor dem Wettkampf, um Erschoepfung zu erzeugen",
        answerB = "Die Koerperanpassung ueber das Ausgangsniveau hinaus nach Trainingsbelastung und Regeneration – genutzt fuer peaking",
        answerC = "Das Training mit Gewichtsweste, um den Koennen-Abfall durch Entlastung zu kompensieren",
        answerD = "Eine Ernaehrungsstrategie mit Extra-Kohlenhydraten vor Springen",
        correctAnswer = 1,
        explanation = "Supercompensation beschreibt das Prinzip, dass der Koerper nach einer Belastungsphase (Training) und ausreichender Regeneration sich nicht nur auf das Ausgangsniveau erholt, sondern darueber hinaus adaptiert. Skispringer und Trainer planen Trainingszentren gezielt auf Wettkampfhoehenplanung: Intensivphase → Erholung → 'Peak' genau bei der Kompetition.",
        difficulty = 3,
        funFact = "Skispringer trainieren das ganze Jahr auf Sommerschanzen mit Kunststoff-Matten. Das erste Mal auf Schnee kommen viele Weltklasse-Springer erst im Oktober – und trotzdem sind die Leistungsunterschiede minimal, da die Technik identisch ist."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man im Eishockey unter dem 'Offside'-Regel, und welche Version gilt in der NHL versus IIHF?",
        answerA = "NHL: Puck muss vor Spieler in Zone / IIHF: Spieler muss auf Scheibe warten – beide identisch",
        answerB = "NHL: 'Tag-up' Offside erlaubt (Spieler kann Blaue Linie beruehren und zurueckkehren) / IIHF: kein Tag-up, Abseits wird sofort gepfiffen",
        answerC = "NHL: Offsides wird nie gepfiffen / IIHF: Abseits fuehrt zur Spielunterbrechung",
        answerD = "NHL und IIHF haben identische Abseitsregeln; keine Unterschiede",
        correctAnswer = 1,
        explanation = "In beiden Systemen gilt: Offside liegt vor, wenn ein Angreifer die gegnerische Blaue Linie ueberquert, bevor der Puck sie ueberquert. Unterschied: In der NHL gibt es 'Tag-up Offside' (seit 2005): Ein Spieler, der im Abseits steht, kann legal spielen, wenn er die Blaue Linie beruehrt (taggt) und damit zurueckkehrt. Die IIHF kennt diese Regel nicht – Abseits wird sofort gepfiffen.",
        difficulty = 3,
        funFact = "Die NHL einfuehrte 'Video-Review for Offside' 2015 – was zu minutenlangen Unterbrechungen fuehrte, wenn Trainer 'Coach's Challenge' fuer mogliche Abseits-Situationen vor Toren nutzten. Seit 2019 wurden die Challenge-Regeln verschaerft, um Missbrauch zu reduzieren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches physikalische Prinzip nutzt ein Biathlon-Athlet beim Schiessen, und warum ist der liegend-Anschlag signifikant genauer als der stehende?",
        answerA = "Liegend ist genauer, weil der Herzschlag den Puls verringert und der Knochen als stabiles Auflager dient",
        answerB = "Liegend ist genauer, weil der Ruecken als Dreh- und Ankerpunkt genutzt wird",
        answerC = "Liegend ist genauer wegen des niedrigeren Schwerpunktes und der Auflagepunkte (Ellbogen, Koerper) die Zittern minimieren",
        answerD = "Kein Unterschied in Genauigkeit; liegend ist nur weniger ermuedend fuer die Schultern",
        correctAnswer = 2,
        explanation = "Im Liegend-Anschlag haben Biathleten mehrere Auflagepunkte (beide Ellbogen, Koerper auf dem Boden), die das System 'Mensch-Gewehr' stabilisieren und Zittern minimieren. Stehend muessen alle Muskelgruppen unter Erschoepfung die Waffe halten – jeder Herzschlag und jede Atemzug-Bewegung uebertraegt sich auf das Gewehr. Die Liegend-Scheibe ist mit 45mm Durchmesser kleiner als die Stehend-Scheibe (115mm) – trotzdem ist die Trefferquote liegend hoeher.",
        difficulty = 3,
        funFact = "Weltklasse-Biathleten schliessen die Augen kurz vor dem Schuss und schliessen Atemanhalten instinktiv ein. Sie trainieren auch 'Herzschlag-Timing': Zwischen zwei Herzschlaegen gibt es einen kurzen Ruhemoment – viele schliessen in diesem Intervall ab."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'V-Style' im Skispringen und wer hat ihn im internationalen Wettkampf etabliert?",
        answerA = "Jan Bokloev (Schweden) entwickelte und zeigte den V-Style 1985; Matti Nykanen machte ihn bekannt",
        answerB = "Eddie 'The Eagle' Edwards erfand den V-Stil 1988 in Calgary",
        answerC = "Sven Hannawald (Deutschland) fuehrte den V-Stil 1998 bei den Olympischen Winterspielen ein",
        answerD = "Noriaki Kasai (Japan) entwickelte und verbesserte den V-Stil ab 1992",
        correctAnswer = 0,
        explanation = "Jan Bokloev aus Schweden zeigte 1985 den 'V-Stil' erstmals systematisch im internationalen Wettkampf: Statt parallelen Skier V-foermig spreizen. Aerodynamisch ist der V-Stil deutlich effizienter – er erzeugt mehr Auftrieb bei weniger Luftwiderstand. Innerhalb weniger Jahre uebernahmen alle Springer den Stil, da Weiten um 15-20% anstiegen.",
        difficulty = 3,
        funFact = "Die ersten Springer mit V-Stil wurden von Kampfrichtern schlechter bewertet, weil der Stil 'hhaesslich' aussah und Stilpunkte abzogen. Erst als die Weiten unbestreitbar laenger wurden, aenderten sich Bewertungskriterien."
    ),

    // ── Sportmedizin (7) ─────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was ist die 'RICE-Methode' in der Sportmedizin, und welche neuere Methode hat sie teilweise abgeloest?",
        answerA = "RICE (Rest, Ice, Compression, Elevation) wurde durch POLICE (Protection, Optimal Loading, Ice, Compression, Elevation) ergaenzt",
        answerB = "RICE (Run, Ice, Compression, Elevation) wurde durch MEAT (Movement, Exercise, Analgesia, Treatment) ersetzt",
        answerC = "RICE ist die aktuell empfohlene Methode ohne Alternative",
        answerD = "RICE wurde vollstaendig verworfen und durch TENS-Therapie ersetzt",
        correctAnswer = 0,
        explanation = "RICE (Rest, Ice, Compression, Elevation) war jahrzehntelang der Standard bei akuten Sportverletzungen (Verstauchungen, Prellungen). Neuere Forschung zeigt, dass vollstaendige Ruhe ('Rest') die Heilung verlangsamen kann. Das neuere POLICE-Modell ergaenzt: Protection (kurzfristig), Optimal Loading (froehzeitige kontrollierte Belastung), Ice, Compression, Elevation. Noch neuere Konzepte heissen PEACE & LOVE.",
        difficulty = 3,
        funFact = "Der Arzt Gabe Mirkin, der RICE 1978 in seinem Sportmedizin-Handbuch popularisierte, widerrief das Konzept 2014 selbst: Er erkannte, dass Eis Enthaendungen hemmt, die fuer die Heilung notwendig sind – besonders das Hormon IGF-1 wird durch Eis reduziert."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist eine 'Commotio cordis' im Sport, und welcher Sporttyp ist besonders gefaehrdet?",
        answerA = "Ein Herzstillstand durch Ueberhitzung (Hitzschlag) bei Ausdauersport; besonders Marathon-Laeufer betroffen",
        answerB = "Ein Kammerflimmern ausgeloest durch einen Schlag auf die Brust in einem kritischen Herzzyklus-Moment; besonders Ballsport",
        answerC = "Ein angeborener Herzfehler der durch Sport ausgeloest wird; besonders Schwimmer betroffen",
        answerD = "Herzrhythmusstoerunger durch chronische Ueberbelastung; besonders Radprofis nach 10+ Jahren",
        correctAnswer = 1,
        explanation = "Commotio cordis (lateinisch: 'Erschuetterung des Herzens') ist ein plotzliches Kammerflimmern, ausgeloest durch einen stumpfen Schlag auf die Brust waehrend eines sehr engen Zeitfensters (10-30 ms) im Herzzyklus. Der Schlag muss nicht stark sein – ein Baseball, Puck oder Softball reicht. Besonders gefaehrdet: Baseballer, Eishockeyspieler, Lacrosse-Spieler. Ohne sofortige Defibrillation ist es fast immer toedlich.",
        difficulty = 3,
        funFact = "Die Ueberlebensrate bei Commotio cordis hat sich dramatisch verbessert: 1995 wurden nur 15% gerettet, heute ueber 58% – dank AED-Verfuegbarkeit in Sportanlagen und schnellerer Reaktion. Viele US-Sportligen schreiben AEDs am Spielfeldrand vor."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt das 'Sports Hernia' (Sportlerleiste) und warum ist es schwer zu diagnostizieren?",
        answerA = "Ein Leistenbruch der nur bei intensiver koerperlicher Belastung auftritt; sichtbar im Ultraschall",
        answerB = "Eine schwaeche der hinteren Leistenkanal-Wand ohne sichtbaren Bruchsack; bildgebende Diagnostik oft unauffaellig",
        answerC = "Eine Muskelverletzung des Adduktors; klar diagnostizierbar per MRT",
        answerD = "Ein entzuendeter Iliopsoas-Sehnenansatz; klassisch sichtbar im Roentgenbild",
        correctAnswer = 1,
        explanation = "Sports Hernia (Sportlerleiste, Gilmore's Groin) ist eine Schwaechung der hinteren Leistenkanalwand ohne klassischen Bruchsack. Symptome sind chronische Leistenschmerzen bei Belastung. Diagnose ist schwierig: MRT und Ultraschall sind oft unauffaellig, Diagnose basiert klinisch auf Symptomatik und Ausschlussdiagnostik. Haeufig bei Fussballern, American-Football-Spielern, Hockeyspielern.",
        difficulty = 3,
        funFact = "Viele prominente Fussballer litten jahrelang mit falsch diagnostizierten Leistenschmerzen, bevor Sportlerleiste erkannt wurde. Laut Studien leiden bis zu 10% aller Fussballer an Sports Hernia-Symptomen, viele werden nie korrekt behandelt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Overtraining-Syndrom' (OTS) und welche Biomarker helfen bei der Diagnose?",
        answerA = "Temporaere Erschoepfung nach intensiver Trainingswoche; Laktat-Test am naechsten Morgen genuegt zur Diagnose",
        answerB = "Chronische Erschoepfung und Leistungsabfall trotz Trainingspause; HRV, Cortisol/Testosteron-Ratio, IL-6 als Biomarker",
        answerC = "Muskelabbau durch Uebertraining; Kreatinkinase (CK) im Blut ist der einzige verlassliche Biomarker",
        answerD = "Akute Muskelverletzung durch zu viele Trainingseinheiten; erkennbar an Muskelschmerzen",
        correctAnswer = 1,
        explanation = "Das Overtraining-Syndrom (OTS) ist ein schwerer chronischer Zustand: Trotz Ruhepausen erholt sich die Leistungsfaehigkeit nicht. Biomarker: Herzratenvariabilitaet (HRV) langfristig erniedrigt; Cortisol/Testosteron-Verhaeltnis verschoben (erhoehtes Cortisol, erniedrigtes Testosteron); Zytokine (IL-6, IL-1beta) erhoehen Entzuendungsmarker; Haemoglobin und Ferritin koennen abfallen. Abzugrenzen von 'Functional Overreaching' (kurzzeitig, reversibler).",
        difficulty = 3,
        funFact = "OTS kann Monate bis Jahre dauern. Bekannte Faelle: Alberto Salazar beschrieb jahrelanges OTS in seiner Laeufer-Karriere. Vollstaendige Erholung erfordert oft totale Trainingspause ueber 6-12 Monate – eine Karriere-bedrohende Diagnose."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Rhabdomyolyse' im sportlichen Kontext, und welches Symptom-Trias kennzeichnet sie klassisch?",
        answerA = "Muskelkrampf, Herzrasen, Schwindel – typisch nach Hitzschlag im Marathon",
        answerB = "Muskelschmerz/-schwaecha, dunkler (Cola-faerbiger) Urin, erhoehte Kreatinkinase (CK) im Blut",
        answerC = "Gelenkschmerzen, Schwellung, Roetung – typisch nach Ueberlastungs-Verletzung",
        answerD = "Kopfschmerz, Uebelkeit, Bewusstlosigkeit – typisch bei Hypoglykamie-Kollaps",
        correctAnswer = 1,
        explanation = "Rhabdomyolyse ('Rhabdo') ist der Zerfall von Muskelfasern mit Freisetzung von Myoglobin ins Blut. Myoglobin wird renal gefiltert und faerbt den Urin dunkelbraun bis schwarz ('Colourlösung'). Klassische Trias: Muskelschmerzen/Schwaerche, dunkler Urin, Kreatinkinase (CK) oft 10.000-1.000.000 U/L (Normal: <200). Gefaehrlich: Myoglobin kann Nieren schaedigen (akutes Nierenversagen).",
        difficulty = 3,
        funFact = "Rhabdomyolyse ist bei Crossfit-Begeisterten so bekannt, dass es im Jargon als 'Uncle Rhabdo' bezeichnet wird – sogar mit einem Cartoon-Maskottchen, das von Crossfit selbst (selbstkritisch) verwendet wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Female Athlete Triad', und welche drei Komponenten bilden es?",
        answerA = "Erhoehte Testosteronwerte, Knochenbrueche, Muskelhypertrophie – bei weiblichen Kraftsportlerinnen",
        answerB = "Niedrige Energieverfuegbarkeit (oft durch Essstoerung), Menstruationsstoerungen, verminderte Knochendichte",
        answerC = "Anaeraemie, Osteoporose, reduzierte Herzleistung – bei Ausdauersportlerinnen",
        answerD = "Uebergewicht, Insulinresistenz, polyzystisches Ovarialsyndrom – bei inaktiven Sportlerinnen",
        correctAnswer = 1,
        explanation = "Das Female Athlete Triad (2007 definiert vom American College of Sports Medicine) umfasst: 1. Niedrige Energieverfuegbarkeit (haeufig durch Ernaehrungs-Restriktion/Essstoerung); 2. Menstruationsstoerungen (bis zur Amenorrhoe); 3. Verminderte Knochendichte (bis zur Osteoporose). Es ist ein Kontinuum – nicht alle drei muessen voll ausgepraegt sein. 2014 erweitert zu RED-S (Relative Energy Deficiency in Sport).",
        difficulty = 3,
        funFact = "Studien zeigen, dass bis zu 60% von Athletinnen in Aesthetic Sports (Turnen, Eiskunstlauf, Ballett) subklinische Symptome des Triads aufweisen. Die langfristigen Knochenfolgen koennen bis ins Erwachsenenalter persistieren."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Tommy John Surgery' (UCL-Rekonstruktion) und in welchem Sport ist es am haeufigsten?",
        answerA = "Kreuzbandersatz am Knie; am haeufigsten bei Skifahrern und American-Football-Spielern",
        answerB = "Rekonstruktion des ulnaren Kollateralbandes am Ellbogen durch Sehnentransplantation; am haeufigsten bei Baseball-Werfern",
        answerC = "Schulter-Labrumrekonstruktion; am haeufigsten bei Schwimmern und Werfern",
        answerD = "Achillessehnen-Rekonstruktion; am haeufigsten bei Sprintern und Basketballspielern",
        correctAnswer = 1,
        explanation = "Tommy John Surgery (benannt nach dem Baseball-Pitcher Tommy John, der sie 1974 als erster hatte) ist die Rekonstruktion des ulnaren Kollateralbandes (UCL) am Ellbogen. Das UCL wird durch eine Transplantatsehne (meist Palmaris-longus-Sehne) ersetzt. Am haeufigsten bei Baseball-Pitchern, die extreme Wurfbelastungen auf den Ellbogen ausueiben. Rehab dauert 12-18 Monate.",
        difficulty = 3,
        funFact = "Tommy John kehrte 1976 nach seiner OP zurueck und warf noch 13 weitere MLB-Saisons – insgesamt 288 Siege. Der Arzt Frank Jobe gab seiner eigenen Erfolgschance 1 zu 100. Heute machen sich viele Pitcher die OP freiwillig mit der Hoffnung, anschliessend einen schelleren Wurf zu haben ('Tommy John Wunder'-Mythos)."
    ),

    // ── Fussball-WM-Raritaeten (8) ────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann die allererste FIFA-Weltmeisterschaft 1930 in Uruguay, und wer war der Torschuetze des Finaltores?",
        answerA = "Argentinien – Guillermo Stabile",
        answerB = "Uruguay – Hector Castro",
        answerC = "USA – Bert Patenaude",
        answerD = "Brasilien – Arthur Friedenreich",
        correctAnswer = 1,
        explanation = "Uruguay gewann die erste WM 1930 im heimischen Montevideo mit einem 4:2-Finalsieg ueber Argentinien. Hector Castro, der ironischerweise durch einen Arbeitsunfall seine rechte Hand verloren hatte, erzielte das 4:2 in der 89. Minute. Uruguay war zweifacher Olympiasieger (1924, 1928) und Favorit.",
        difficulty = 3,
        funFact = "13 Mannschaften nahmen 1930 teil; viele europaeische Laender boykottierten aufgrund der langen Schiffsreise. Europas einzige Teilnehmer waren Belgien, Frankreich, Rumaenien und Jugoslawien – Rumaenien durfte teilnehmen, weil Koenig Carol II persoenlich die Reise genehmigte."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war das 'Wunder von Bern' 1954, und gegen welches Land setzte sich Deutschland im Finale durch?",
        answerA = "Deutschland besiegte Ungarn 3:2 im Finale von Bern, obwohl Ungarn das bis dahin als unbesiegbar geltende Team war",
        answerB = "Deutschland besiegte Brasilien 4:1 im Finale von Bern nach dem 'Maracanazo'",
        answerC = "Deutschland besiegte Uruguay 2:1 nach Verlaengerung im Finale von Bern",
        answerD = "Deutschland besiegte England 3:0 im Halbfinale in Bern",
        correctAnswer = 0,
        explanation = "Das 'Wunder von Bern' war das WM-Finale 1954: Deutschland (BRD) schlug die als unschlagbar geltenden Ungarn mit 3:2. Die Ungarn ('Goldene Mannschaft' unter Puskas) hatten 32 Spiele ungeschlagen gespielt und Deutschland im Vorrunde 8:3 vernichtet. Helmut Rahn erzielte in der 84. Minute den Siegtreffer. Es war eine der groessten Ueberraschungen der WM-Geschichte.",
        difficulty = 3,
        funFact = "Herbert Zimmermanns legendaerer Radiokommentar ('Aus dem Hintergrund muesste Rahn schiessen... Rahn schiesst... Tooooor!') wurde so ikonisch, dass er 2004 im Deutschen Bundestag zitiert wurde. Das Spiel gilt als Mitgruendungsmoment des bundesrepublikanischen Nationalbewusstseins."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Spiel ist als 'Maracanazo' in die WM-Geschichte eingegangen und was war sein historischer Kontext?",
        answerA = "Uruguays 2:1-Sieg ueber Brasilien 1950 im Maracana – Brasilien hatte bereits eine Siegesfeier vorbereitet",
        answerB = "Deutschlands 7:1-Sieg ueber Brasilien 2014 im Maracana in Belo Horizonte",
        answerC = "Englands 4:2-Sieg ueber Deutschland 1966 im Wembley-Finale",
        answerD = "Italiens Niederlage 3:5 gegen Nordkorea 1966 – groesste WM-Uebberraschung",
        correctAnswer = 0,
        explanation = "Der 'Maracanazo' war Uruguays 2:1-Sieg ueber Brasilien am 16. Juli 1950 im Entscheidungsspiel der WM. Brasilien brauchte nur ein Unentschieden, fuehrte dominierend, galt als sicherer Weltmeister. Uruguay erzielte in der 79. Minute das 2:1 durch Ghiggia. Das Maracana-Stadion mit 200.000 Zuschauern war in Schockstarre – ein nationales Trauma Brasiliens.",
        difficulty = 3,
        funFact = "Offiziell war es kein 'Finale': 1950 gab es eine Schlussendrunde-Gruppe statt K.O. Uruguays Sieg sorgte fuer den entscheidenden Vorteil. Braziliens Torwart Moacir Barbosa trug die Last des Verlustes bis zu seinem Tod 2000 – er wurde von Brasilianern nie vergeben."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann als einziges aussereuropaeisches und aussersuedamerikanisches Land eine WM, und in welchem Jahr?",
        answerA = "Mexiko – 1970 in Mexiko",
        answerB = "USA – 1994 in den USA",
        answerC = "Kein Land – Europa und Suedamerika gewinnen immer",
        answerD = "Kenia – noch keine WM ausgetragen in Afrika",
        correctAnswer = 2,
        explanation = "Alle bisherigen Weltmeister kommen aus Europa oder Suedamerika: Brasilien (5x), Deutschland (4x), Italien (4x), Argentinien (3x), Frankreich (2x), Uruguay (2x), England (1x), Spanien (1x). Kein Team aus Nordamerika, Afrika, Asien oder dem Nahen Osten hat je die WM gewonnen – eine der bemerkenswertesten statistischen Regelmassigkeiten des Weltsports.",
        difficulty = 3,
        funFact = "Das 'Gesetz der zwei Kontinente' gilt sogar fuer den Austragungsort: Wenn die WM in Europa stattfindet, gewinnt meist ein europaeisches Team; in Amerika meist ein suedamerikanisches. Einzige Ausnahme: Deutschland gewann 2014 in Brasilien – und Spanien 2010 in Suedafrika."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war das 'Blutspiel von Bern' (1954) und zwischen welchen Teams wurde es ausgetragen?",
        answerA = "England vs. Argentinien – brutales Viertelfinalspiel mit 3 Platzverweisen",
        answerB = "Ungarn vs. Brasilien – Viertelfinale 1954 mit 4 Platzverweisen, Schlaegelei nach Abpfiff",
        answerC = "Deutschland vs. Oesterreich – aggressives Halbfinale mit 2 gebrochenen Beinen",
        answerD = "Frankreich vs. Italien – Finale-Qualifikationsspiel mit 5 Roten Karten",
        correctAnswer = 1,
        explanation = "Das Viertelfinale 1954 zwischen Ungarn und Brasilien in Bern wurde zum gewalttaetigsten Spiel der WM-Geschichte. 4 Platzverweise, zahlreiche Fouls, Ungarn gewann 4:2. Nach Abpfiff setzte sich die Schlaegelei in der Umkleidekabine fort – Spieler, Trainer und sogar Fotografen wurden verletzt. Der Begriff 'Blutspiel von Bern' oder 'Battle of Bern' haelt sich seitdem.",
        difficulty = 3,
        funFact = "Die diplomatischen Nachwirkungen waren erheblich: Der brasilianische Verband reichte offiziell Protest ein, Schiedsrichter Arthur Ellis erhielt Morddrohungen. Kameramann auf dem Platz filmte Szenen, die heutzutage zu langen Sperren gefuehrt haetten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches WM-Spiel 1982 gilt als das groesste Vorrundenspiel (und groesste Spielmanipulations-Verdachts) der Geschichte?",
        answerA = "Deutschland vs. Oesterreich 1:0 – 'Nichtangriffspakt von Gijon'",
        answerB = "Frankreich vs. Deutschland 3:3 – legendaeres Elfmeterschiessen",
        answerC = "Brasilien vs. Italien 2:3 – Zico, Socrates und Falcao verlieren gegen Rossi",
        answerD = "England vs. Argentinien 1:2 – 'Hand Gottes' von Maradona",
        correctAnswer = 0,
        explanation = "Das 'Disgrace of Gijon' (deutsch: 'Schande von Gijon'): Deutschland besiegte Oesterreich 1:0 in der Schlussminute-Gruppe. Dieses Ergebnis reichte beiden Teams zum Weiterkommen auf Kosten Algeriens. Nach dem Fuehrungstor spielten beide Teams das Spiel passiv runter. Es war technisch regelkonform, aber moralisch fragwuerdig. In Folge fuehrt FIFA alle Gruppenabschlussspiele simultan durch.",
        difficulty = 3,
        funFact = "Algerische Fans im Stadion breiteten Geldscheine aus und riefen 'Schmiergeldschiessen'. Spanische Radiostationen brachen die Uebertragung ab und spielten Flamenco-Musik. Die algerische Presse titelte 'Zwei Faelscher' auf der Titelseite."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Spieler erzielte bisher die meisten WM-Tore in der Geschichte, und wie viele?",
        answerA = "Ronaldo (Brasilien) – 15 Tore",
        answerB = "Miroslav Klose (Deutschland) – 16 Tore",
        answerC = "Pele (Brasilien) – 12 Tore",
        answerD = "Gerd Mueller (Deutschland) – 14 Tore",
        correctAnswer = 1,
        explanation = "Miroslav Klose haelt mit 16 WM-Toren den Rekord als erfolgreichster WM-Torschuetze der Geschichte. Seine Tore verteilten sich auf vier Turniere: 2002 (5 Tore), 2006 (5 Tore), 2010 (4 Tore), 2014 (2 Tore, darunter sein 16. im Halbfinale gegen Brasilien beim 7:1). Er ueberholte damit Ronaldo (Brasilien) mit 15 Toren.",
        difficulty = 3,
        funFact = "Klose uebertraf Ronaldos Rekord in der 23. Minute des Halbfinales Deutschland-Brasilien 2014 (7:1) in Belo Horizonte – ausgerechnet im Maracana-Halbfinale, das zum 'Mineirazo' (Brasiliens zweites nationales WM-Trauma) wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher WM-Ausgabe spielte ein Spieler das Finale und erzielte ein Tor, obwohl er aus einem Gefaengnis entlassen worden war, um daran teilzunehmen?",
        answerA = "Alcides Ghiggia (Uruguay) 1950 – er war inhaftiert wegen Steuerhinterziehung",
        answerB = "Garrincha (Brasilien) 1962 – er hatte eine Haushaltbewilligung nach Verhaftung",
        answerC = "Kein solcher Fall ist historisch belegt – urbane Legende",
        answerD = "Luis Monti (Argentinien) 1930 – freigelassen um WM zu spielen",
        correctAnswer = 2,
        explanation = "Diese Geschichte ist eine weit verbreitete Legende, fuer die es keinen historisch belegten Fall gibt. Es gibt aehnliche Storys (z.B. ueber Spieler die wegen kleinerer Vergehen kurz inhaftiert waren), aber kein Finaltorschuetze wurde je aus einem Gefaengnis fuer die WM entlassen. Stadtlegenden dieser Art entstehen haeufig in der Fussball-Folklore.",
        difficulty = 3,
        funFact = "Garrincha spielte tatsaechlich die WM 1962, obwohl er zuvor wegen Fahrerflucht unter Alkohol in Probleme geraten war – aber er war nicht inhaftiert. Er war einer der grossartigsten Dribbler der Geschichte und gewann mit Brasilien den Titel."
    ),

    // ── Sportpsychologie (8) ──────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'Flow-Zustand' (Flow State) in der Sportpsychologie, und wer praegte ihn wissenschaftlich?",
        answerA = "Ein Erschoepfungszustand am Ende eines Wettkampfs; beschrieben von Martin Seligman",
        answerB = "Ein optimaler Bewusstseinszustand hoechster Konzentration und muhelosem Handeln; praegte von Mihaly Csikszentmihalyi",
        answerC = "Eine Meditationstechnik zur Vorbereitung auf Wettkampf; entwickelt von Jon Kabat-Zinn",
        answerD = "Ein Begriff fuer 'Runner's High' – Endorphinausschuettung waehrend Sport; definiert von Roger Bannister",
        correctAnswer = 1,
        explanation = "Flow (auch 'being in the zone') ist ein optimaler Bewusstseinszustand, bei dem eine Person voellig in eine Aktivitaet aufgeht – maximale Konzentration ohne Anstrengung, Zeitverlust, klares Feedback. Psychologe Mihaly Csikszentmihalyi praege den Begriff und die Theorie ab den 1970ern. Im Sport ist Flow mit Bestleistungen verbunden: Der Athlet faehrt 'autopilot' auf hoechstem Niveau.",
        difficulty = 3,
        funFact = "Csikszentmihalyi's Flow-Theorie entstand aus Studien mit Bildhauern und Kletterern, die kostenlose Taetigkeit ausuebten ohne aussere Belohnung. Er bemerkte, dass intrinsische Motivation und die Balance zwischen Herausforderung und Faehigkeit die Schluessel sind."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Choking under pressure' (Versagen unter Druck) und welches Modell erklaert es am besten?",
        answerA = "Erschoepfung durch zu langen Wettkampf; erklaert durch das Energy-Depletion-Modell",
        answerB = "Leistungsabfall unter hohem Druck durch erhoehte selbstbewusste Aufmerksamkeit (Explicit Monitoring Theory) oder Erregungsueberschuss (Arousal-Performance)",
        answerC = "Psychologische Blockade durch Angst vor Verlust; erklaert durch Vermeidungs-Motivation-Theorie",
        answerD = "Koerperliche Verkrampfung durch Adrenalin-Ausschuettung; erklaert durch Kampf-oder-Flucht-Theorie",
        correctAnswer = 1,
        explanation = "Choking erklaert zwei konkurrierende Modelle: 1. Explicit Monitoring Theory (Masters): Hoher Druck lenkt Aufmerksamkeit auf automatisierte Bewegungsablaeufe – man 'denkt zu viel' und stoert automatisierte Kompetenz. 2. Attentional-Anxiety-Theory: Angst verursacht Ablenkung und schmaelert Fokus. Beide erklaeren, warum Profis unter extremem Druck (Penalty in Elfmeterschiessen) versagen koennen.",
        difficulty = 3,
        funFact = "Studien zeigen: Rechthaender kippen bei Druck tendenziell nach links (links-hemisphaerengestuert = Analyse), Linkshaender umgekehrt. Ablenkung durch einfache Aufgaben ('alphabetisch rueckwaerts zaehlen') reduziert Choking – es verhindert das 'Ueberdenken'."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Self-Talk' in der Sportpsychologie und welche zwei Formen zeigen unterschiedliche Effekte auf Leistung?",
        answerA = "Inneres Selbstgespraeche: instruktionaler Self-Talk (Was tun) verbessert technische Faehigkeiten; motivationaler Self-Talk verbessert Kraft/Ausdauer",
        answerB = "Oeffentliches Reden vor Spielern: positive Ansprache vor Spiel / kritische Analyse nach Spiel",
        answerC = "Mentales Training: Visualisierung (positiv) / Negativvisualisierung (Fehler vorstellen zur Vorbereitung)",
        answerD = "Teamkommunikation: offensiver Call-Out vs. defensiver Schweige-Strategie",
        correctAnswer = 0,
        explanation = "Self-Talk (innere Selbstgespraeche) ist ein zentrales sportpsychologisches Werkzeug. Instruktionaler Self-Talk ('Ellbogen halten', 'Knie beugen') verbessert besonders neue oder komplexe Techniken. Motivationaler Self-Talk ('Ich kann es', 'Stark bleiben') verbessert Kraft, Ausdauer und Belastungstoleranz. Metaanalysen (Hatzigeorgiadis et al., 2011) belegen beide Effekte statistisch signifikant.",
        difficulty = 3,
        funFact = "Rafael Nadal genuetzt Self-Talk intensiv – er fluestert sich im Spiel staendig Anweisungen. Serena Williams ist bekannt fuer lautes motivationales Self-Talk auf dem Platz. Muhammad Ali entwickelte 'I am the greatest' als Mantras-Strategie lange vor der modernen Sportpsychologie."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Yerkes-Dodson-Gesetz' im Sport, und welche Kurvenform beschreibt den Zusammenhang zwischen Erregung und Leistung?",
        answerA = "Linearer Zusammenhang: mehr Erregung = immer mehr Leistung; Gerade aufwaerts",
        answerB = "Invertierte U-Kurve: mittleres Erregungsniveau (optimal arousal) fuehrt zu Bestleistung; zu wenig oder zu viel verschlechtern die Leistung",
        answerC = "Exponentieller Zusammenhang: Erregung wirkt erst ab Schwellenwert positiv",
        answerD = "Keine Korrelation: Erregung und sportliche Leistung sind unabhaengig",
        correctAnswer = 1,
        explanation = "Das Yerkes-Dodson-Gesetz (Robert Yerkes & John Dodson, 1908) beschreibt eine Umgekehrte-U-Beziehung: Zu wenig Erregung (arousal) fuehrt zu Untermotivation und schlechter Leistung. Optimales Erregungsniveau fuehrt zur Bestleistung. Zu hohe Erregung fuehrt zu Angst, Verkrampfung, Fehlern. Der optimale Punkt haengt von Aufgabenkomplexitaet ab: einfache Aufgaben = hoeheres optimales Arousal; komplexe = niedrigeres.",
        difficulty = 3,
        funFact = "Das Gesetz gilt als erste wissenschaftliche Begruendung, warum 'Nervositaet schadet': Wettkampfangst bei Leichtathleten fuehrt zu Muskelverspannungen, die technische Bewegungsablaeufe storen. Entspannungstraining hat einen Platz in der Sportpsychologie also aus diesem Grund."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Mental Imagery' (Vorstellungskraft) im Athleten-Training, und welches Modell erklaert den Transfermechanismus auf Leistung?",
        answerA = "Bildliche Erinnerung an vergangene Erfolge; hilft durch positive Emotionsregulation (Mood State Modell)",
        answerB = "Mentale Simulation von Bewegungsablaeufen; Psycho-neuromuscular Theory: erzeugt schwache neuronale Aktivierung identisch zu echter Bewegung",
        answerC = "Visualisierung des Wettkampfortes; hilft durch Umgebungs-Vertrautheit (Environmental Familiarity Modell)",
        answerD = "Fokussierung auf Gegner-Schwaerchen; erklaert durch Strategic Planning Modell",
        correctAnswer = 1,
        explanation = "Mental Imagery (Vorstellung, Visualisierung) aktiviert laut Psycho-Neuromuskulaerer Theorie (Jacobson 1930, bestaetigt durch EMG-Studien) beim Vorstellen einer Bewegung tatsaechlich die entsprechenden Muskeln in schwachem Masse. Neuronale Pfade werden aktiviert – fast wie bei echter Ausfuehrung. Ergaenzt wird dies durch 'Symbolic Learning Theory': Vorstellung hilft, Bewegungsmuster kognitiv zu repraesentieren.",
        difficulty = 3,
        funFact = "Michael Phelps visualisierte vor jedem Rennen den gesamten Ablauf bis ins Detail – einschliesslich Szenarien wie 'Was wenn meine Brille beschlaegt?' Er schwamm 2008 tatsaechlich fast blind das 200m Schmetterling-Rennen und gewann Weltrekord, weil er diese Situation mental vorbereitet hatte."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Attribution Theory' im sportlichen Kontext, und welche Dimensionen beschreibt Weiners Modell?",
        answerA = "Die Theorie erklaert, wie Athleten ihre Erfolge/Niederlagen erklaeren; Weiner: Ort (intern/extern), Stabilitaet (stabil/variabel), Kontrollierbarkeit",
        answerB = "Die Theorie erklaert Team-Dynamik bei Niederlagen; Dimensionen: Vertrauen, Teamgeist, Fuehrung",
        answerC = "Die Theorie erklaert Zuschauereinfluss; Dimensionen: Heimvorteil, Stimmung, Druck",
        answerD = "Die Theorie erklaert Dopingmotivation; Dimensionen: Belohnung, Risiko, soziale Norm",
        correctAnswer = 0,
        explanation = "Bernard Weiners Attributionsmodell erklaert, wie Menschen Ursachen fuer Erfolge/Misserfolge zuschreiben. Drei Dimensionen: 1. Ort (Locus): intern (eigene Faehigkeit/Anstrengung) vs. extern (Glueck, Gegner); 2. Stabilitaet: stabil (Faehigkeit) vs. variabel (Tagesform); 3. Kontrollierbarkeit: kontrollierbar (Anstrengung) vs. unkontrollierbar (Glueck). Diese Attributionen beeinflussen zukunftige Motivation erheblich.",
        difficulty = 3,
        funFact = "Sportler, die Niederlagen intern-stabil attribuieren ('Ich bin einfach nicht gut genug') entwickeln Learned Helplessness. Sportpsychologen trainieren 'mastery-oriented attribution': Niederlagen variabel-intern erklaeren ('Ich habe heute nicht gut trainiert – das kann ich aendern')."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Goal Setting Theory' nach Locke und Latham und welche Anforderungen muessen effektive Ziele erfuellen (SMART)?",
        answerA = "Ziele sollten nur langfristig sein; S=Simple, M=Motivating, A=Attainable, R=Relevant, T=Traditional",
        answerB = "Ziele muessen spezifisch und herausfordernd sein; S=Specific, M=Measurable, A=Achievable, R=Relevant, T=Time-bound",
        answerC = "Ziele sollten nur von Trainern gesetzt werden; Athleten-Eigenziele fuehren zu Burnout",
        answerD = "Ziele sind individuell; kein universelles Schema wie SMART hat wissenschaftliche Basis",
        correctAnswer = 1,
        explanation = "Edwin Locke und Gary Lathams Goal Setting Theory (1990) gilt als eine der bestbegruendetsten Motivationstheorien im Sport. Kernprinzipien: Spezifische, herausfordernde (aber erreichbare) Ziele fuehren zu hoeherem Einsatz als vage ('tu dein Bestes') oder leichte Ziele. SMART-Kriterien: Specific, Measurable, Achievable, Relevant, Time-bound. Prozess-, Leistungs- und Ergebnisziele sollten kombiniert werden.",
        difficulty = 3,
        funFact = "Michael Jordan setzte sich in der Off-Season spezifische Ziele: 'Ich werde 500 Freiwuerfe taeglich trainieren.' Nicht: 'Ich werde besser.' Forschung zeigt, dass Prozessziele (wie trainieren) effektiver als reine Ergebnisziele (WM gewinnen) sind, da Athleten den Prozess kontrollieren koennen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Resilience' (Belastbarkeit) im Sport, und welche drei Komponenten beschreibt Richardson's Resilience Modell?",
        answerA = "Physische Widerstandsfaehigkeit gegen Verletzungen; Komponenten: Kraft, Ausdauer, Flexibilitaet",
        answerB = "Faehigkeit sich von Rueckschlaegen zu erholen; Komponenten: Disruption → Reintegration → adaptives Wachstum oder Schutzfaktoren",
        answerC = "Mentale Haerte bei Wettkampf; Komponenten: Fokus, Selbstvertrauen, Emotionskontrolle",
        answerD = "Widerstandsfaehigkeit gegen Burnout; Komponenten: soziale Unterstuetzung, Selbstwirksamkeit, Copingstrategien",
        correctAnswer = 1,
        explanation = "George Richardsons Resilienzmodell (2002) beschreibt Resilienz als dynamischen Prozess: Ein Rueckschlag (Disruption) fuehrt zu einem Desintegrations-Zustand. Daraus folgt Reintegration auf verschiedenen Ebenen: dysfunktional (Schutzfaktoren aufgeben), zu Homeostasis (Ausgangsniveau), mit Resilienz (hoeheres Funktionsniveau) oder mit Wachstum (tiefgreifende Transformationen). Im Sport wird Resilienz als Kapazitaet gesehen, aus Verletzungen, Niederlagen und Rueckschlaegen gestärkt heranzugehen.",
        difficulty = 3,
        funFact = "Tiger Woods ist ein Paradox-Beispiel: Extreme Resilienz (Rueckkehr nach 4 Rueckenoperationen, Skandalen, Autounfall zum Masters-Sieg 2019) und gleichzeitig ein Beispiel fuer mentalen Zusammenbruch. Resilience-Forscher diskutieren, ob sein 2019er Comeback das Lehrbuch-Beispiel fuer 'Adversarial Growth' ist."
    ),

)
