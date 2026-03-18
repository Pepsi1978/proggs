package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsExpert6(): List<Question> = listOf(

    // --- WINTERSPORT: SKI ALPIN ---

    Question(
        categoryId = 6,
        questionText = "Welche Mindestlaenge schreibt der FIS-Regelmensch fuer einen Riesenslalom-Ski der Herren im Weltcup vor?",
        answerA = "185 cm",
        answerB = "193 cm",
        answerC = "195 cm",
        answerD = "188 cm",
        correctAnswer = 1,
        explanation = "Seit der Saison 2012/13 schreibt die FIS eine Mindestlaenge von 193 cm fuer Riesenslalom-Ski der Herren vor. Diese Regel wurde eingefuehrt, um die Kurvengeschwindigkeiten zu reduzieren und das Verletzungsrisiko zu senken.",
        difficulty = 4,
        funFact = "Vor der Regelaenderung fuhren Athleten teilweise mit Ski unter 180 cm, was enorme Kurvengeschwindigkeiten ermoeglichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie wird die Durchschneidungsregel im alpinen Skisport bei einem Torstangenfehler bezeichnet?",
        answerA = "Gate Penalty Rule",
        answerB = "DNF-Regel",
        answerC = "Korrekte Torpassage: beide Skispitzen und beide Skistiefel muessen die Torebene passieren",
        answerD = "Nur ein Ski und ein Stiefel muessen durch das Tor",
        correctAnswer = 2,
        explanation = "Gemaess FIS-Reglement muessen beide Skispitzen und beide Skistiefel die gedachte Torebene zwischen den Torstangen passieren. Wird diese Bedingung nicht erfuellt, wird der Fahrer disqualifiziert (DNF).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Marcel Hirscher gewann den alpinen Ski-Gesamtweltcup achtmal in Folge. In welchem Jahr gewann er seinen ersten Gesamtweltcup?",
        answerA = "2010",
        answerB = "2011",
        answerC = "2012",
        answerD = "2013",
        correctAnswer = 1,
        explanation = "Marcel Hirscher gewann seinen ersten Gesamtweltcup in der Saison 2011/12 und dominierte den alpinen Skisport bis zu seinem Ruecktritt nach der Saison 2018/19 mit acht Titeln in Folge.",
        difficulty = 4,
        funFact = "Hirscher holte in seiner Karriere 67 Weltcupsiege und ist damit einer der erfolgreichsten alpinen Skirennlaeufer aller Zeiten."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie hoch ist der maximale Haengeunterschied (Vertikaldistanz) eines olympischen Abfahrtskurses der Maenner gemaess FIS-Reglement?",
        answerA = "700 bis 1000 m",
        answerB = "800 bis 1100 m",
        answerC = "500 bis 800 m",
        answerD = "600 bis 900 m",
        correctAnswer = 1,
        explanation = "Laut FIS-Reglement muss ein olympischer Abfahrtskurs der Maenner einen Hohenunterschied von 800 bis 1100 Metern aufweisen. Bei Damen liegt die Vorgabe bei 450 bis 800 Metern.",
        difficulty = 4,
        funFact = "Die Streif in Kitzbuehel hat einen Hoehenunterschied von rund 860 Metern und gilt als anspruchsvollste Abfahrt der Welt."
    ),

    // --- BIATHLON ---

    Question(
        categoryId = 6,
        questionText = "Welchen Durchmesser haben die Scheiben beim Biathlon-Schiessen im liegenden Anschlag?",
        answerA = "40 mm",
        answerB = "45 mm",
        answerC = "50 mm",
        answerD = "35 mm",
        correctAnswer = 1,
        explanation = "Im liegenden Anschlag betraegt der Scheibendurchmesser 45 mm, im stehenden Anschlag 115 mm. Der kleinere Durchmesser im Liegen spiegelt die groessere Praezision wider, die in dieser Position moeglich ist.",
        difficulty = 4,
        funFact = "Biathlon-Athleten muessen ihren Puls von ueber 180 Schlaegen pro Minute im Laufen auf unter 160 beim Schiessen senken – alles innerhalb weniger Sekunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der Unterschied zwischen der Verfolgung (Pursuit) und dem Massenstart im Biathlon hinsichtlich der Startbedingungen?",
        answerA = "Beim Massenstart starten alle gleichzeitig, bei der Verfolgung starten die Athleten mit dem Zeitrueckstand aus dem Vorrennen",
        answerB = "Beim Massenstart starten die Besten zuerst, bei der Verfolgung alle gleichzeitig",
        answerC = "Es gibt keinen Unterschied, beide Formate haben einen Massenstart",
        answerD = "Bei der Verfolgung starten nur 15 Athleten, beim Massenstart alle qualifizierten",
        correctAnswer = 0,
        explanation = "Bei der Verfolgung starten die Athleten mit genau dem Zeitabstand, den sie im vorherigen Rennen hatten – der Erstplatzierte startet zuerst. Beim Massenstart hingegen starten alle qualifizierten Athleten gleichzeitig.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lang ist eine Strafrunde im Biathlon, die nach einem Fehlschuss abgeleistet werden muss (in bestimmten Wettkampfformaten)?",
        answerA = "100 Meter",
        answerB = "150 Meter",
        answerC = "200 Meter",
        answerD = "250 Meter",
        correctAnswer = 1,
        explanation = "Eine Strafrunde im Biathlon betraegt 150 Meter. Sie wird in der Verfolgung, beim Massenstart und in der Mixed-Staffel fuer jeden Fehlschuss abgeleistet. Im Einzel hingegen wird pro Fehler eine Strafminute auf die Zeit addiert.",
        difficulty = 4,
        funFact = "Ein geuebtter Biathlet absolviert eine Strafrunde in etwa 20-25 Sekunden."
    ),

    // --- EISKUNSTLAUF ---

    Question(
        categoryId = 6,
        questionText = "Welcher Sprung im Eiskunstlauf ist der einzige, der von der Vorwaertskante des Schlittschuhs abgeleitet wird?",
        answerA = "Lutz",
        answerB = "Flip",
        answerC = "Toe Loop",
        answerD = "Loop",
        correctAnswer = 0,
        explanation = "Der Lutz ist der einzige Sprung im Eiskunstlauf, bei dem der Laeufer auf einer Rueckwaertsaussenkante gleitet und sich vom Zehenpick des anderen Schlittschuhs abdruckt. Die Gegendrehung zur Einlaufrichtung macht ihn besonders anspruchsvoll.",
        difficulty = 4,
        funFact = "Der Lutz wurde nach dem oesterreichischen Schlittschulaeufer Alois Lutz benannt, der ihn 1913 erstmals ausfuehrte."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet man im Eiskunstlauf als 'Flutz'?",
        answerA = "Eine fehlerhafte Kombination aus Flip und Lutz, bei der die Aussenkante beim Lutz nicht korrekt gehalten wird",
        answerB = "Eine spezielle Variante des Axels mit zusaetzlicher halber Umdrehung",
        answerC = "Eine verbotene Sprungtechnik bei Paarlaeufern",
        answerD = "Den dreifachen Flip im Kurzkuerprogramm",
        correctAnswer = 0,
        explanation = "Ein 'Flutz' ist ein fehlerhaft ausgefuehrter Lutz-Sprung, bei dem der Laeufer statt der korrekten Rueckwaertsaussenkante eine Innenkante benutzt – was eigentlich einem Flip entspricht. Kampfrichter werten diesen Fehler mit einem negativen Qualitaetsmerkmal (GOE).",
        difficulty = 4,
        funFact = "Viele hochrangige Eiskunstlaeufer kaempfen mit dem Flutz-Problem, darunter auch Weltklasseathletinnen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wieviele Umdrehungen umfasst ein Axel-Sprung bei einem dreifachen Axel?",
        answerA = "2,5 Umdrehungen",
        answerB = "3,0 Umdrehungen",
        answerC = "3,5 Umdrehungen",
        answerD = "4,0 Umdrehungen",
        correctAnswer = 2,
        explanation = "Ein dreifacher Axel umfasst 3,5 Umdrehungen, da der Axel als einziger Sprung vorwaerts abgesprungenist. Jede zusaetzliche halbe Umdrehung macht ihn schwieriger als alle anderen dreifachen Spruenge.",
        difficulty = 4,
        funFact = "Yuzuru Hanyu war der erste Olympiasieger, der im Wettkampf regelmassig vierfache Axels mit 4,5 Umdrehungen in seinen Programmen einbaute."
    ),

    // --- BOB UND SKELETON ---

    Question(
        categoryId = 6,
        questionText = "Welches Maximalgewicht (Fahrer plus Schlitten) ist beim Zweier-Bob der Maenner im Weltcuprennen zulaessig?",
        answerA = "390 kg",
        answerB = "390 kg",
        answerC = "450 kg",
        answerD = "410 kg",
        correctAnswer = 0,
        explanation = "Das Maximalgewicht eines Zweierbobs inklusive Fahrer betraegt 390 kg. Beim Viererbob liegt das Limit bei 630 kg. Schwerere Teams duerfen Ballast entfernen, leichtere Teams koennen Gewichte hinzufuegen.",
        difficulty = 4,
        funFact = "Bob-Piloten brauchen sowohl Sprint-Kraft fuer den Anschub als auch feine motorische Praezision fuer das Lenken – eine seltene athletische Kombination."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher Koerperlage faehrt ein Skeleton-Pilot die Bahn hinab?",
        answerA = "Auf dem Ruecken, mit den Fuessen voran",
        answerB = "Auf dem Bauch, mit dem Kopf voran",
        answerC = "Auf dem Bauch, mit den Fuessen voran",
        answerD = "Auf dem Ruecken, mit dem Kopf voran",
        correctAnswer = 1,
        explanation = "Beim Skeleton liegt der Fahrer auf dem Bauch und faehrt kopfvoran die Bobbahn hinunter. Im Gegensatz dazu liegt der Rodelfahrer auf dem Ruecken und faehrt fusswaerts. Der Name 'Skeleton' stammt vermutlich von der skelettartigen Form des fruehen Schlittens.",
        difficulty = 4,
        funFact = "Skeleton-Fahrer koennen bei Olympischen Spielen Geschwindigkeiten von ueber 130 km/h erreichen und erleben Fliehkraefte von bis zu 5g in engen Kurven."
    ),

    // --- JUDO ---

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet der Begriff 'Koka' im klassischen Judo-Wertungssystem?",
        answerA = "Eine kleinere Wertung fuer einen Wurf mit teilweiser Kontrolle",
        answerB = "Die hoechste Wertung im Judo, gleichwertig mit Ippon",
        answerC = "Eine Bodentechnik mit Immobilisierung von weniger als 10 Sekunden",
        answerD = "Die Straf- und Verwarnoungszone des Tatami",
        correctAnswer = 0,
        explanation = "Koka war im alten Wertungssystem des Judo die kleinste Wertung fuer einen Wurf, bei dem der Gegner auf Gesass oder Oberschenkel geworfen wurde. Das System wurde jedoch 2010 abgeschafft; heute gibt es nur noch Waza-ari und Ippon.",
        difficulty = 4,
        funFact = "Das alte Wertungssystem Koka-Yuko-Waza-ari-Ippon galt Jahrzehnte lang als fair und praezise, wurde aber wegen strategischen Missbrauchs reformiert."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Judotechnik bezeichnet 'Uchi-mata'?",
        answerA = "Innenschenkelwurf",
        answerB = "Aussenschulterrolle",
        answerC = "Hueftwurf vorwaerts",
        answerD = "Fussfeger rueckwaerts",
        correctAnswer = 0,
        explanation = "Uchi-mata (Innenschenkelwurf) ist eine der effektivsten und haeufigsten Wurfttechniken im Judo. Der Werfende schwingt ein Bein zwischen die Beine des Gegners und hebt ihn dabei nach vorne.",
        difficulty = 4,
        funFact = "Uchi-mata gilt als einer der am haeufigsten ausgefuehrten Wuerfe im olympischen Judo-Wettkampf und ist fuer seine Vielseitigkeit bekannt."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lang darf eine Haltetechnik (Osaekomi) im Judo maximal dauern, um einen Ippon zu erzielen?",
        answerA = "20 Sekunden",
        answerB = "25 Sekunden",
        answerC = "30 Sekunden",
        answerD = "15 Sekunden",
        correctAnswer = 1,
        explanation = "Eine Haltetechnik (Osaekomi) muss mindestens 25 Sekunden gehalten werden, um einen Ippon zu erzielen. Bei 10-24 Sekunden gibt es Waza-ari. Dieses System wurde 2010 bei der Regelreform eingefuehrt (vorher waren es 30 bzw. 25 Sekunden).",
        difficulty = 4,
        funFact = null
    ),

    // --- KARATE ---

    Question(
        categoryId = 6,
        questionText = "Wie viele Punkte bringt ein 'Jodan'-Tritt (Kopftreffer) im WKF-Karate-Kumite?",
        answerA = "1 Punkt (Ippon)",
        answerB = "2 Punkte (Waza-ari)",
        answerC = "3 Punkte (Sanbon)",
        answerD = "4 Punkte",
        correctAnswer = 2,
        explanation = "Im WKF-Kumite bringt ein Jodan-Tritt (Tritt in den Kopfbereich) drei Punkte (Sanbon). Chudan-Tritte (Koerpertreffer) bringen zwei Punkte (Nihon), und Handtechniken gegen den Koerper (Chudan) bringen einen Punkt (Ippon).",
        difficulty = 4,
        funFact = "Karate war erstmals bei den Olympischen Spielen 2020 in Tokio als offizielle Sportart vertreten, wurde aber nicht in das Programm der Spiele 2024 in Paris aufgenommen."
    ),

    // --- TAEKWONDO ---

    Question(
        categoryId = 6,
        questionText = "Welches elektronische System wird im olympischen Taekwondo seit 2012 zur Treffererkennung eingesetzt?",
        answerA = "PSS – Protector and Scoring System",
        answerB = "EDS – Electronic Detection System",
        answerC = "TRS – Taekwondo Recording System",
        answerD = "KSS – Korean Scoring System",
        correctAnswer = 0,
        explanation = "Das PSS (Protector and Scoring System) ist ein elektronisches Wertungssystem mit Drucksensoren in Koerrperschutzausstattung und Helm. Es registriert Treffer automatisch ab einem definierten Kraftschwellwert und sendet die Daten drahtlos an die Anzeigetafel.",
        difficulty = 4,
        funFact = "Das PSS hat das subjektive Punktrichterurteil im Taekwondo erheblich objektiviert und Kontroversen um falsche Entscheidungen reduziert."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele Punkte bringt ein Drehtritt in den Kopf im olympischen Taekwondo?",
        answerA = "3 Punkte",
        answerB = "4 Punkte",
        answerC = "5 Punkte",
        answerD = "2 Punkte",
        correctAnswer = 2,
        explanation = "Ein Drehtritt (spinning kick) in den Kopf bringt im olympischen Taekwondo 5 Punkte – das Maximum pro Technik. Ein normaler Kopftreffer gibt 3 Punkte, ein Drehtritt in den Koerper 4 Punkte und ein Koerperkick 2 Punkte.",
        difficulty = 4,
        funFact = null
    ),

    // --- MMA ---

    Question(
        categoryId = 6,
        questionText = "Welche Regelversion bildet die Grundlage fuer die meisten professionellen MMA-Organisationen weltweit (inkl. UFC)?",
        answerA = "PRIDE FC Rules",
        answerB = "Unified Rules of Mixed Martial Arts",
        answerC = "Nevada Athletic Commission Basic Rules",
        answerD = "International Sport Karate Association Rules",
        correctAnswer = 1,
        explanation = "Die 'Unified Rules of Mixed Martial Arts' wurden 2001 von der New Jersey State Athletic Control Board und verschiedenen US-Kommissionen entwickelt und sind heute der weltweit dominierende Regelrahmen fuer professionelles MMA, inklusive der UFC.",
        difficulty = 4,
        funFact = "Vor den Unified Rules gab es in den USA einen Flickenteppich verschiedener staatlicher Regelwerke, was es schwer machte, MMA landesweit zu sanktionieren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Gewichtsklasse liegt im MMA zwischen Flyweight (bis 56,7 kg) und Featherweight (bis 65,8 kg)?",
        answerA = "Strawweight",
        answerB = "Bantamweight",
        answerC = "Super Flyweight",
        answerD = "Lightweight",
        correctAnswer = 1,
        explanation = "Das Bantamweight (bis 61,2 kg) liegt zwischen Flyweight (bis 56,7 kg) und Featherweight (bis 65,8 kg). Die UFC hat dieses Gewichtslimit seit 2010 in ihr Programm aufgenommen.",
        difficulty = 4,
        funFact = null
    ),

    // --- REITSPORT ---

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet man im Dressurreiten als 'Piaffe'?",
        answerA = "Eine Trabarbeit auf der Stelle mit hoher Kadenz und maximaler Kollektion",
        answerB = "Eine Passage mit verlaengertem Schritt",
        answerC = "Einen Galoppsprung mit Beinwechsel in der Luft",
        answerD = "Eine Pirouette im Schritt",
        correctAnswer = 0,
        explanation = "Die Piaffe ist eine hochkolektierte Trabarbeit auf der Stelle. Das Pferd hebt diagonal je zwei Beine (Diagonalpaare) mit gleicher Kadenz und bleibt dabei nahezu bewegungslos am Boden. Sie gilt als Hoehepunkt der klassischen Dressur.",
        difficulty = 4,
        funFact = "Die Piaffe stammt aus der klassischen Reitkunst des 17. Jahrhunderts und wurde urspruenglich als Kriegsuebung entwickelt, um Pferde in beengten Raeumen manoevrierbar zu machen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie wird im Springreiten ein 'In-und-Out' oder 'Oxer' korrekt bezeichnet, wenn zwei Hindernisse hintereinander ohne Galoppsprung dazwischen gesprungen werden?",
        answerA = "Steilsprung",
        answerB = "Kombination (Distanz: 1 Sprung = 1-2-Sprung-Abstand)",
        answerC = "Doppelsteilsprung",
        answerD = "Liverpool",
        correctAnswer = 1,
        explanation = "Ein 'In-und-Out' ist eine Kombination aus zwei (oder drei) Hindernissen, zwischen denen nur ein oder zwei Galoppspruenge Platz sind. Im Gegensatz dazu ist ein Oxer ein einzelnes breites Hindernis mit zwei parallelen oder ansteigenden Stangen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche olympische Disziplin im Reitsport kombiniert Dressur, Gelaendereiten und Springreiten?",
        answerA = "Vielseitigkeit (Eventing)",
        answerB = "Voltigieren",
        answerC = "Westernreiten",
        answerD = "Distanzreiten",
        correctAnswer = 0,
        explanation = "Die Vielseitigkeit (Eventing) besteht aus drei Teilpruefungen: Dressur (am ersten Tag), Gelaendereiten (Cross-Country, am zweiten Tag) und Springreiten (am dritten Tag). Sie gilt als das kompletteste Pruefungsformat fuer Reiter und Pferd.",
        difficulty = 4,
        funFact = "Die Vielseitigkeit war urspruenglich eine militaerische Disziplin, bei der Kavallerie-Offiziere und ihre Pferde auf alle Einsatzsituationen vorbereitet wurden."
    ),

    // --- SEGELN ---

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'Kreuzen' (Kreuzen am Wind) im Segeln?",
        answerA = "Das Segeln gegen den Wind durch wechselseitiges Wenden (Aufkreuzen)",
        answerB = "Das Segeln exakt mit dem Wind von hinten",
        answerC = "Eine Manoeuvrier-Technik bei Flautengebieten",
        answerD = "Den Kurswechsel durch Lee-Halse",
        correctAnswer = 0,
        explanation = "Da Segelboote nicht direkt gegen den Wind segeln koennen, erreichen sie einen Ziel-Punkt gegen den Wind durch 'Kreuzen': abwechselnd am Wind auf Steuer- und Backbordbug segeln (Wenden), um sich im Zickzack dem Wind entgegen vorzuarbeiten.",
        difficulty = 4,
        funFact = "Moderne Rennsegler schaffen durch optimierte Segel und Rumpfformen Kreuzwinkel von unter 30 Grad zum tatsaechlichen Wind."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Bootsklasse wird bei den Olympischen Spielen im Einhand-Segeln der Maenner (seit 2024 Paris) eingesetzt?",
        answerA = "Laser (ILCA 7)",
        answerB = "Finn",
        answerC = "iQFOiL",
        answerD = "470er",
        correctAnswer = 2,
        explanation = "Seit den Olympischen Spielen 2024 in Paris wird im Einhand-Segeln der Maenner die iQFOiL-Klasse eingesetzt – ein Windsurf-Foilbrett, das durch hydrofoils auf Geschwindigkeiten ueber 50 km/h beschleunigen kann. Der klassische Laser (ILCA 7) ist nicht mehr olympisch.",
        difficulty = 4,
        funFact = "Das iQFOiL ersetzt den traditionellen RS:X Windsurf und markiert einen Paradigmenwechsel hin zu Foiling-Technologie im olympischen Segeln."
    ),

    // --- RUDERN ---

    Question(
        categoryId = 6,
        questionText = "Was ist der Unterschied zwischen 'Skull' (Sculling) und 'Riemen' (Sweep) beim Rudern?",
        answerA = "Beim Sculling haelt jeder Ruderer zwei Ruder, beim Sweep-Rudern haelt jeder Ruderer nur ein Ruder",
        answerB = "Beim Riemen werden vier Ruderer eingesetzt, beim Skull nur zwei",
        answerC = "Sculling bezeichnet das Steuermannsruder, Riemen die Antriebsruder",
        answerD = "Es gibt keinen Unterschied, beide Begriffe bezeichnen dasselbe",
        correctAnswer = 0,
        explanation = "Beim Sculling (Skull) haelt jeder Ruderer zwei kurze Ruder (Skulls), eines auf jeder Seite. Beim Sweep-Rudern haelt jeder Athlet nur ein langes Ruder (Riemen) auf einer Seite. Skull-Boote sind u.a. Einer, Doppelzweier und Doppelvierer.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Ueber welche Distanz werden olympische Ruderrennen ausgetragen?",
        answerA = "1500 Meter",
        answerB = "1800 Meter",
        answerC = "2000 Meter",
        answerD = "2500 Meter",
        correctAnswer = 2,
        explanation = "Olympische Ruderrennen werden ueber 2000 Meter ausgetragen. Diese Distanz wurde 1912 als olympischer Standard festgelegt und gilt weltweit als Grundlage fuer alle wichtigen Wettkampfe der FISA (World Rowing).",
        difficulty = 4,
        funFact = "Ein olympisches Ruderrennen dauert bei Spitzenbooten weniger als 6 Minuten – was enorme Ausdauer und explosive Kraft gleichzeitig erfordert."
    ),

    // --- KANU ---

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet 'C1' und 'K1' im Kanurennsport?",
        answerA = "C1 = Canadier-Einzel (einseitig), K1 = Kajak-Einzel (beidseits)",
        answerB = "C1 = Kajak-Einzel, K1 = Canadier-Einzel",
        answerC = "C1 = Zweier-Canadier, K1 = Einzel-Kajak",
        answerD = "Beide Bezeichnungen beziehen sich auf Kajak-Disziplinen",
        correctAnswer = 0,
        explanation = "C1 bezeichnet den Canadier-Einzel: der Paddler kniet im Boot und paddelt einseitig mit einem einblattigen Paddel. K1 bezeichnet den Kajak-Einzel: der Athlet sitzt und paddelt beidseits mit einem zweiblattigen Paddel.",
        difficulty = 4,
        funFact = "Im Kanuslalom werden C1, C2, K1 auf kuenstlichen Wildwasserstrecken mit Toren ausgetragen – eine der visuell spektakulaersten olympischen Disziplinen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lang ist die olympische Kanurennsport-Strecke fuer den K1 1000m der Maenner?",
        answerA = "1000 Meter",
        answerB = "500 Meter",
        answerC = "200 Meter",
        answerD = "750 Meter",
        correctAnswer = 0,
        explanation = "Der K1 1000m ist eine olympische Disziplin ueber exakt 1000 Meter. Seit den Olympischen Spielen in Tokio 2020 wurde die Distanz fuer mehrere Kanu-Disziplinen angepasst; der 1000m bleibt aber fuer einige Klassen olympisch.",
        difficulty = 4,
        funFact = null
    ),

    // --- TRIATHLON ---

    Question(
        categoryId = 6,
        questionText = "Welche Distanzen umfasst ein Olympischer Triathlon (Kurzdistanz)?",
        answerA = "1,5 km Schwimmen – 40 km Radfahren – 10 km Laufen",
        answerB = "3,8 km Schwimmen – 180 km Radfahren – 42,2 km Laufen",
        answerC = "1,9 km Schwimmen – 90 km Radfahren – 21,1 km Laufen",
        answerD = "0,75 km Schwimmen – 20 km Radfahren – 5 km Laufen",
        correctAnswer = 0,
        explanation = "Der Olympische Triathlon (Kurzdistanz) besteht aus 1,5 km Schwimmen, 40 km Radfahren und 10 km Laufen. Der Ironman hingegen umfasst 3,8 km Schwimmen, 180 km Radfahren und 42,195 km Laufen.",
        difficulty = 4,
        funFact = "Triathleten wechseln in der 'Transition Zone' innerhalb von Sekunden zwischen den Sportarten – das Wechselmanagement gilt als vierte Disziplin."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet man im Triathlon als 'Drafting' und wann ist es verboten?",
        answerA = "Das Fahren im Windschatten des Vordermanns beim Radfahren – verboten auf Nicht-Draft-legal-Rennen, aber erlaubt bei olympischen Wettkampfen",
        answerB = "Das vorzeitige Wechseln der Disziplin in der Transition Zone",
        answerC = "Das Schwimmen im Kielwasser des Vorderschwimmers",
        answerD = "Das Benutzen eines Pacemakers beim Laufen",
        correctAnswer = 0,
        explanation = "Drafting bezeichnet das Fahren im aerodynamischen Windschatten eines anderen Radfahrers. Bei olympischen Triathlon-Rennen ist Drafting erlaubt (draft-legal). Bei Langdistanz-Rennen (Ironman, ITU Long Distance) ist es streng verboten und wird mit Zeitstrafen sanktioniert.",
        difficulty = 4,
        funFact = "Im Windschatten eines vorausfahrenden Radfahrers kann man bis zu 30% Energie sparen – ein enormer Vorteil, der die Taktik bei draft-legal Rennen grundlegend veraendert."
    ),

    // --- FECHTEN ---

    Question(
        categoryId = 6,
        questionText = "Welche Trefffläche ist beim Degen-Fechten gueltig?",
        answerA = "Nur der Oberkörper und Arme",
        answerB = "Nur die Brust",
        answerC = "Der gesamte Koerper inklusive Füsse und Kopf",
        answerD = "Nur die Beine und der Rumpf",
        correctAnswer = 2,
        explanation = "Beim Degen ist der gesamte Koerper des Gegners als Trefferflaeche zulaessig – von den Schuhen bis zum Helm. Dies unterscheidet den Degen vom Florett (Rumpf) und Saebel (Oberkörper und Arme).",
        difficulty = 4,
        funFact = "Der Degen ist die schwerste der drei Fechtwaffenarten und die einzige ohne Vorrecht – es gibt keine Prioritaet des Angriffs, wer zuerst trifft, bekommt den Punkt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man im Fechten unter dem Begriff 'Prioritaet' oder 'Vorrecht'?",
        answerA = "Die Regel, dass bei gleichzeitigen Treffern beim Florett und Saebel derjenige zaehlt, der den Angriff initiiert hat",
        answerB = "Die Rangliste der Fechter im Weltcup",
        answerC = "Das Recht, die Waffe zuerst zu waehlen",
        answerD = "Die Regel, dass der schwerere Fechter zuerst angreift",
        correctAnswer = 0,
        explanation = "Das Vorrecht (Prioritaet) ist eine Regel bei Florett und Saebel: Bei gleichzeitigen Treffern (Doublette) entscheidet die taktische Handlungslogik – wer den Angriff initiiert hat oder ihn durch eine Parade zuruckgewonnen hat, bekommt den Punkt. Beim Degen gibt es kein Vorrecht.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Wie lang ist eine Fechtbahn (Piste) im olympischen Fechten?",
        answerA = "12 Meter",
        answerB = "14 Meter",
        answerC = "18 Meter",
        answerD = "10 Meter",
        correctAnswer = 1,
        explanation = "Eine Fechtbahn (Piste) ist 14 Meter lang und 1,5 bis 2 Meter breit. Die mittlere Linie und die Warnlinie (2 Meter vom Rand) sind markiert. Tritt ein Fechter ueber die rueckwaertige Grenze, bekommt der Gegner einen Punkt.",
        difficulty = 4,
        funFact = "Die streng rechteckige Piste zwingt Fechter zur raeumlichen Kalkulation – ein Stratege, der den Gegner in die Ecke drängt, hat erhebliche taktische Vorteile."
    ),

    // --- BOGENSCHIESSEN ---

    Question(
        categoryId = 6,
        questionText = "Aus welcher Entfernung schiesst man im olympischen Bogenschiessen auf die Zielscheibe?",
        answerA = "50 Meter",
        answerB = "60 Meter",
        answerC = "70 Meter",
        answerD = "90 Meter",
        correctAnswer = 2,
        explanation = "Im olympischen Recurvebogen-Schiessen betraegt die Wettkampfdistanz 70 Meter. Die Zielscheibe hat einen Durchmesser von 122 cm, das Innengold (10-Ring) misst 12,2 cm Durchmesser.",
        difficulty = 4,
        funFact = "Ein olympischer Bogenschuetze muss aus 70 Metern das Innengold von 12,2 cm Durchmesser treffen – das entspricht einem Sehwinkel von nur 0,1 Grad."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet die 'Draw Weight' (Zuggewicht) beim Bogenschiessen?",
        answerA = "Die Kraft in Pfund oder Kilogramm, die benoetigt wird, um die Sehne auf volle Zuglaenge zu spannen",
        answerB = "Das Gewicht des Pfeils in Grain",
        answerC = "Das Gesamtgewicht des Bogens inklusive Zubehoer",
        answerD = "Die Spannung der Bogensehne in Newton",
        correctAnswer = 0,
        explanation = "Die Draw Weight gibt an, wie viel Kraft (in Pfund oder Kilogramm) aufgewendet werden muss, um die Bogensehne auf die festgelegte Zuglaenge zu spannen. Bei olympischen Recurvebögen liegt sie typischerweise zwischen 40 und 50 Pfund.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Bogenart ist bei den Olympischen Spielen zugelassen?",
        answerA = "Recurvebogen (Reflexbogen)",
        answerB = "Compoundbogen",
        answerC = "Traditioneller Langbogen",
        answerD = "Alle drei Bogenarten",
        correctAnswer = 0,
        explanation = "Bei den Olympischen Spielen wird ausschliesslich mit dem Recurvebogen geschossen. Der technisch weiterentwickelte Compoundbogen, der durch Flaschenzugrollen die Zugkraft am Ende des Zuges reduziert, ist olympisch nicht zugelassen.",
        difficulty = 4,
        funFact = "Der Compoundbogen ermoeglicht durch sein Rollen-System praeziseres und entspannteres Zielen – weshalb Compoundschiessen bei Weltmeisterschaften oft noch hoehere Scores erzielen als Recurve."
    ),

    // --- WEITERE SPEZIALDISZIPLINEN ---

    Question(
        categoryId = 6,
        questionText = "Welche Koerperzone ist beim Saebel-Fechten als Trefferflaeche gueltig?",
        answerA = "Gesamter Koerper",
        answerB = "Nur Brust und Bauch",
        answerC = "Oberkörper, Arme und Maske (alles oberhalb der Huefte)",
        answerD = "Nur der Rumpf ohne Arme",
        correctAnswer = 2,
        explanation = "Beim Saebel (Sabre) ist alles oberhalb der Huefte als Trefferflaeche gueltig: Rumpf, Arme und Kopf (Maske). Im Gegensatz zum Florett, bei dem nur der Rumpf zaehlt, kann man beim Saebel auch Arme und Kopf treffen.",
        difficulty = 4,
        funFact = "Der Saebel ist die einzige Fechtwaffenart, bei der Schnitte (nicht nur Stiche) als gueltige Treffer zaehlen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist bei der Kanu-Slalom-Weltmeisterschaft das Strafprinzip, wenn ein Athlet eine Torstange beruehrt?",
        answerA = "2 Strafsekunden pro beruehrter Stange",
        answerB = "5 Strafsekunden pro beruehrter Stange",
        answerC = "10 Strafsekunden und Disqualifikation beim zweiten Fehler",
        answerD = "Sofortige Disqualifikation",
        correctAnswer = 1,
        explanation = "Im Kanu-Slalom werden 2 Strafsekunden fuer das Beruehren einer Torstange addiert. Ein verfehltes oder falsch passiertes Tor kostet 50 Strafsekunden – faktisch eine Disqualifikation von der Medaillenkonkurrenz.",
        difficulty = 4,
        funFact = "Kanu-Slalom-Weltklassefahrer erinnern sich alle Tore (20-25 Stucke) in exakter Reihenfolge auswendig und fahren den Kurs mental durch, bevor sie starten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Gewichtsklassen-System gilt im olympischen Judo fuer Maenner?",
        answerA = "Sieben Gewichtsklassen: bis 60, 66, 73, 81, 90, 100 kg und ueber 100 kg",
        answerB = "Fuenf Gewichtsklassen: bis 60, 73, 90 kg und offene Klasse",
        answerC = "Acht Gewichtsklassen mit je 10 kg Abstand",
        answerD = "Sechs Gewichtsklassen: bis 60, 70, 80, 90, 100 kg und offen",
        correctAnswer = 0,
        explanation = "Im olympischen Judo der Maenner gibt es sieben Gewichtsklassen: bis 60 kg, bis 66 kg, bis 73 kg, bis 81 kg, bis 90 kg, bis 100 kg und ueber 100 kg (Schwergewicht). Bei den Frauen sind es ebenfalls sieben Klassen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man im Pferdesport unter dem Begriff 'Galoppwechsel in der Luft' (Flying Change)?",
        answerA = "Den Wechsel des fuehrenden Beines beim Galopp ohne Zwischentritt im Schritt oder Trab",
        answerB = "Einen Sprung mit Seitenwechsel ueber ein Hindernis",
        answerC = "Die Wechsel der Galopprichtung in der Pirouette",
        answerD = "Den Uebergang vom Galopp in den Trab",
        correctAnswer = 0,
        explanation = "Beim Galoppwechsel in der Luft (Flying Change) wechselt das Pferd das fuehrende Bein im Galopp waehrend der Schwebephase – ohne Zwischentritt im Schritt oder Trab. In der Grand Prix-Dressur werden bis zu 15 Wechsel pro Sprung (Tempi-Wechsel) verlangt.",
        difficulty = 4,
        funFact = "Ein Pferd, das jeden Schritt einen Galoppwechsel macht (Ein-Schritt-Wechsel), zeigt eine der technisch anspruchsvollsten Dressur-Lektionen ueberhaupt."
    ),

    Question(
        categoryId = 6,
        questionText = "In welcher olympischen Disziplin muss ein Athlet gleichzeitig laeuft, schiesst und schwimmt?",
        answerA = "Triathlon",
        answerB = "Moderner Fuenfkampf",
        answerC = "Biathlon",
        answerD = "Duathlon",
        correctAnswer = 1,
        explanation = "Der Moderne Fuenfkampf (Pentathlon) kombiniert Fechten, Schwimmen, Reiten (Springreiten), Schiessen und Laufen. Seit den Olympischen Spielen 2024 in Paris wurde Reiten durch Hindernislauf ersetzt, da das Reiten auf unbekannten Pferden als zu problematisch galt.",
        difficulty = 4,
        funFact = "Der Moderne Fuenfkampf wurde 1912 in Stockholm von Pierre de Coubertin eingefuehrt, inspiriert von der Legende eines Offiziers, der eine Botschaft zu Pferd, zu Fuss, schwimmend und fechtend ueberbrachte."
    )

)
