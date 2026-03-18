package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsExpert4(): List<Question> = listOf(

    // --- OLYMPISCHE GESCHICHTE (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Spielen wurde der olympische Fackellauf als modernes Ritual zum ersten Mal durchgefuehrt, mit einer Staffel von Olympia nach Berlin?",
        answerA = "Amsterdam 1928",
        answerB = "Los Angeles 1932",
        answerC = "Berlin 1936",
        answerD = "London 1948",
        correctAnswer = 2,
        explanation = "Der olympische Fackellauf von Olympia zum Austragungsort wurde 1936 in Berlin erstmals als Ritual eingefuehrt. Die Idee stammte von Carl Diem, dem Generalsekretaer des Organisationskomitees. Die Nazis nutzten das Spektakel bewusst als Propagandainstrument.",
        difficulty = 4,
        funFact = "Der erste Fackellaeufer war der griechische Sprinter Konstantinos Kondylis. Insgesamt 3.422 Laeufer trugen die Flamme ueber 3.187 Kilometer."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann bei den Olympischen Spielen 1980 in Moskau die meisten Goldmedaillen im Gesamtmedaillenspiegel?",
        answerA = "DDR",
        answerB = "Sowjetunion",
        answerC = "Kuba",
        answerD = "Bulgarien",
        correctAnswer = 1,
        explanation = "Die Sowjetunion dominierte die Spiele 1980 in Moskau mit 80 Goldmedaillen, 69 Silber und 46 Bronze. Der westliche Boykott (USA, BRD, Japan) aufgrund des Einmarschs der Sowjets in Afghanistan verzerzte den Medaillenspiegel erheblich.",
        difficulty = 4,
        funFact = "Die DDR belegte mit 47 Goldmedaillen den zweiten Platz — ein bis heute unuebertroffenes Ergebnis fuer ein deutsches Team bei Olympia."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fanden die ersten modernen Olympischen Spiele der Neuzeit statt, und wer gewann den ersten olympischen Marathonlauf?",
        answerA = "1896, Spyridon Louis (Griechenland)",
        answerB = "1896, Edwin Flack (Australien)",
        answerC = "1900, Michel Theato (Frankreich)",
        answerD = "1896, Gyula Kellner (Ungarn)",
        correctAnswer = 0,
        explanation = "Die ersten modernen Olympischen Spiele fanden 1896 in Athen statt. Den Marathonlauf gewann der griechische Wassertraeger Spyridon Louis in 2:58:50 Stunden ueber die damals noch nicht normierte Distanz von etwa 40 km. Sein Sieg wurde in Griechenland als nationales Ereignis gefeiert.",
        difficulty = 4,
        funFact = "Spyridon Louis nahm danach nie wieder an einem Wettkampf teil. Er lebte bis zu seinem Tod 1940 als einfacher Bauer nahe Athen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Sportler gewann bei den Olympischen Spielen 1936 in Berlin vier Goldmedaillen und widerlegte damit Hitlers Rassenideologie auf der Weltbuehne?",
        answerA = "Eulace Peacock",
        answerB = "Jesse Owens",
        answerC = "Ralph Metcalfe",
        answerD = "Mack Robinson",
        correctAnswer = 1,
        explanation = "Jesse Owens gewann 1936 in Berlin Gold im 100m, 200m, Weitsprung und mit der 4x100m-Staffel. Der US-Amerikaner afrikanischer Abstammung widerlegte Hitlers Vorstellung arischer Ueberlegenheit eindrucksvoll. Der deutsche Weitspringer Luz Long half Owens mit Ratschlaegen — eine beruehmte Episode der Sportgeschichte.",
        difficulty = 4,
        funFact = "Jesse Owens berichtete spaeter, dass Hitler ihm zugewinkt habe, waehrend US-Praesident Roosevelt ihm keine Glueckwunschtelegramme schickte und ihn nie ins Weisse Haus einlud."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Spielen wurden erstmals Profisportler offiziell zugelassen, nachdem der IOC die Amateurregel (Bylaw 26) abgeschafft hatte?",
        answerA = "Seoul 1988",
        answerB = "Calgary 1988",
        answerC = "Barcelona 1992",
        answerD = "Albertville 1992",
        correctAnswer = 2,
        explanation = "Formell wurden Profis schrittweise zugelassen: Tennis und Tischtennis ab Seoul 1988, Fussball und Basketball (NBA-Stars) ab Barcelona 1992. Das Dream Team der NBA bei den Spielen 1992 gilt als Symbol dieser Aera. Die vollstaendige Abschaffung der Amateurregel erfolgte 1992.",
        difficulty = 4,
        funFact = "Das US-Basketball Dream Team 1992 mit Michael Jordan, Magic Johnson und Larry Bird gewann seine Spiele im Schnitt mit 43,8 Punkten Vorsprung und gilt als das dominanteste Olympiateam aller Zeiten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches dramatische Ereignis unterbrach die Olympischen Spiele 1972 in Muenchen und hatte weltweit politische Auswirkungen?",
        answerA = "Attentat auf den israelischen Premierminister",
        answerB = "Geiselnahme und Ermordung von 11 israelischen Sportlern durch das Schwarze September-Kommando",
        answerC = "Bombenanschlag auf das olympische Dorf durch die IRA",
        answerD = "Politisches Attentat auf einen sowjetischen Funktionaer",
        correctAnswer = 1,
        explanation = "Am 5. September 1972 nahm die palaestaensische Terrorgruppe 'Schwarzer September' elf israelische Athleten und Betreuer als Geiseln. Bei einem missglueckten Befreiungsversuch am Flughafen Fuerstenfeldbrueck wurden alle elf Geiseln sowie ein Polizist getoetet. IOC-Praesident Avery Brundage entschied kontrovers, die Spiele weiterzufuehren.",
        difficulty = 4,
        funFact = "Die deutschen Sicherheitsbehörden waren fatal schlecht vorbereitet: Die erste Stuermung scheiterte, weil die Terroristen die geplante Aktion live im Fernsehen verfolgten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Stadt war als einzige dreimal Ausrichterin der Olympischen Sommerspiele?",
        answerA = "Paris",
        answerB = "London",
        answerC = "Athen",
        answerD = "Stockholm",
        correctAnswer = 1,
        explanation = "London war dreimal Austragungsort der Olympischen Sommerspiele: 1908 (als Ersatz fuer das durch den Vesuv-Ausbruch verhinderte Rom), 1948 (erste Nachkriegsspiele) und 2012. Damit ist London die einzige Stadt, die dreimal Gastgeber der Sommerspiele war.",
        difficulty = 4,
        funFact = "Bei den Spielen 1948 in London durften Deutschland und Japan nicht teilnehmen. Die Athleten lebten in umgebauten Schulen und Militaerlagern — das olympische Dorf war zu teuer."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Laeufer gewann bei den Olympischen Spielen 1968 in Mexico City den 200m-Lauf und hob zusammen mit seinem Teamkollegen die Faust als Black-Power-Geste auf dem Siegerpodest?",
        answerA = "John Carlos",
        answerB = "Tommie Smith",
        answerC = "Lee Evans",
        answerD = "Jim Ryun",
        correctAnswer = 1,
        explanation = "Tommie Smith (Gold, 200m) und John Carlos (Bronze, 200m) streckten bei der Siegerehrung eine schwarzbehandschuhte Faust in die Hoehe — als Zeichen der Black-Power-Bewegung und gegen Rassismus in den USA. Das IOC schloss beide sofort von den Spielen aus. Diese Geste gilt als eines der ikonischsten politischen Statements in der Sportgeschichte.",
        difficulty = 4,
        funFact = "Der australische Silbermedaillengewinner Peter Norman trug als Zeichen der Solidaritaet ein Abzeichen des Olympic Project for Human Rights und wurde dafuer von seiner eigenen Heimat jahrelang geaechtet."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation boycottierte die Olympischen Sommerspiele 1984 in Los Angeles als Gegenantwort auf den westlichen Boycott von 1980?",
        answerA = "China, Kuba und Nordkorea",
        answerB = "Sowjetunion und die meisten Laender des Ostblocks",
        answerC = "DDR und Polen",
        answerD = "Iran, Irak und Syrien",
        correctAnswer = 1,
        explanation = "Die Sowjetunion und 13 weitere Ostblock-Staaten boycottierten 1984 offiziell aus 'Sicherheitsgruenden'. Tatsaechlich war es eine Vergeltungsmassnahme fuer den westlichen Boycott 1980. Kuba und Aethiopien schlossen sich an, waehrend Rumaenien und Jugoslawien als Ostblock-Laender trotzdem teilnahmen.",
        difficulty = 4,
        funFact = "Die USA gewannen 1984 dank des Ostblock-Boycotts 83 Goldmedaillen — bis heute ihr bestes Ergebnis. Ohne den Boycott waere das Ergebnis laut Simulationen deutlich niedriger ausgefallen."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Spielen wurden Doping-Kontrollen erstmals systematisch eingefuehrt?",
        answerA = "Tokio 1964 — Stimulanzien-Tests",
        answerB = "Mexiko City 1968 — erste offizielle IOC-Dopingtests",
        answerC = "Muenchen 1972 — Kortikoide im Fokus",
        answerD = "Montreal 1976 — Anabolika-Nachweis eingefuehrt",
        correctAnswer = 1,
        explanation = "Mexiko City 1968 war das erste Mal, dass das IOC flaeachendeckende Doping-Tests durchfuehrte. Im Fokus standen Stimulanzien wie Amphetamine und Narkotika. Anabolika wurden erst ab 1976 in Montreal auf der Verbotsliste aufgenommen und getestet, nachdem entsprechende Nachweismethoden entwickelt worden waren.",
        difficulty = 4,
        funFact = "Der schwedische Moderner-Fuenfkaempfer Hans-Gunnar Liljenwall wurde 1968 als erster Olympionike wegen Dopings disqualifiziert — er hatte vor dem Pistolenschiessen zwei Bier getrunken, um die Nerven zu beruhigen."
    ),

    // --- LEICHTATHLETIK-WELTREKORDE (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Wer haelt den aktuellen Weltrekord im 100m-Sprint der Maenner, und welche Zeit wurde dabei erzielt?",
        answerA = "Tyson Gay — 9,69 Sekunden",
        answerB = "Usain Bolt — 9,58 Sekunden",
        answerC = "Yohan Blake — 9,69 Sekunden",
        answerD = "Asafa Powell — 9,72 Sekunden",
        correctAnswer = 1,
        explanation = "Usain Bolt stellte am 16. August 2009 bei der Leichtathletik-WM in Berlin mit 9,58 Sekunden den Weltrekord auf. Dieser Rekord steht bis heute und gilt vielen Experten als eine der beeindruckendsten sportlichen Leistungen aller Zeiten.",
        difficulty = 4,
        funFact = "Bolts Spitzengeschwindigkeit bei diesem Lauf wurde mit 44,72 km/h gemessen — etwa bei Meter 65. Ein Mensch kann theoretisch bis zu 45 km/h laufen, was die Biomechanik der Muskelfasern zulassen wuerde."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Athlet verbesserte den Weltrekord im Stabhochsprung auf ueber 6,20 Meter und stellte diesen Rekord im Jahr 2020 auf?",
        answerA = "Renaud Lavillenie",
        answerB = "Sam Kendricks",
        answerC = "Armand Duplantis",
        answerD = "Piotr Lisek",
        correctAnswer = 2,
        explanation = "Armand Duplantis aus Schweden verbesserte am 15. Februar 2020 in Glasgow den Weltrekord auf 6,17 m und steigerte ihn tags darauf auf 6,18 m. Er verbesserte seinen eigenen Rekord seitdem mehrfach auf 6,24 m (Stand 2023).",
        difficulty = 4,
        funFact = "Armand Duplantis wuchs in einer Sportlerfamilie auf — sein Vater Greg war US-College-Stabhochspringer, seine Mutter Helena schwedische Mehrkaempferin. Als Kind sprang er im Garten mit selbstgebauten Aufbauten."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer haelt den Weltrekord im Marathonlauf der Maenner (offizielle IAAF-Bestleistung auf regulaerer Strecke), und welche Zeit wurde dabei gelaufen?",
        answerA = "Eliud Kipchoge — 2:01:09 Stunden",
        answerB = "Kelvin Kiptum — 2:00:35 Stunden",
        answerC = "Kenenisa Bekele — 2:01:41 Stunden",
        answerD = "Geoffrey Kamworor — 2:02:13 Stunden",
        correctAnswer = 1,
        explanation = "Kelvin Kiptum aus Kenia stellte am 8. Oktober 2023 in Chicago mit 2:00:35 Stunden einen neuen Weltrekord auf und unterbot damit Eliud Kipchoge's Rekord (2:01:09) um mehr als eine Minute. Eliud Kipchoges inoffizielle Sub-2h-Zeit von 1:59:40 in Wien 2019 gilt nicht als Weltrekord, da keine regulaere Wettkampfbedingungen herrschten.",
        difficulty = 4,
        funFact = "Kelvin Kiptum kam tragischerweise im Februar 2024 bei einem Verkehrsunfall in Kenia ums Leben — sein Weltrekord blieb bestehen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Leichtathletin haelt den seit 1988 bestehenden Weltrekord im Siebenkampf?",
        answerA = "Eunice Barber — 6.898 Punkte",
        answerB = "Jackie Joyner-Kersee — 7.291 Punkte",
        answerC = "Carolina Klueft — 6.952 Punkte",
        answerD = "Antoinette Nana Djimou — 6.833 Punkte",
        correctAnswer = 1,
        explanation = "Jackie Joyner-Kersee stellte am 24. September 1988 bei den Olympischen Spielen in Seoul mit 7.291 Punkten den Weltrekord im Siebenkampf auf, der bis heute Bestand hat. Sie gilt als eine der groessten Mehrkampf-Athletinnen aller Zeiten.",
        difficulty = 4,
        funFact = "Joyner-Kersee litt ihr gesamtes Sportlerleben an Asthma, was ihren Leistungen jedoch keinen Abbruch tat. Sports Illustrated kuerte sie zur besten Sportlerin des 20. Jahrhunderts."
    ),

    Question(
        categoryId = 6,
        questionText = "Der Weltrekord im Dreisprung der Herren liegt bei 18,29 Metern. Wer haelt diesen Rekord, und bei welcher Veranstaltung wurde er aufgestellt?",
        answerA = "Mike Conley — Olympia 1996",
        answerB = "Jonathan Edwards — Leichtathletik-WM Goeteborg 1995",
        answerC = "Christian Taylor — Leichtathletik-WM Peking 2015",
        answerD = "Kenny Harrison — Olympia 1996",
        correctAnswer = 1,
        explanation = "Jonathan Edwards aus Grossbritannien stellte am 7. August 1995 bei der Leichtathletik-WM in Goeteborg mit 18,29 Metern den Weltrekord auf. Dieser Rekord steht seit fast 30 Jahren und gilt als einer der stabilsten Weltrekorde in der Leichtathletik.",
        difficulty = 4,
        funFact = "Edwards war tief glaeubig und weigerte sich lange, an Sonntagswettkampfen teilzunehmen. Den Weltrekord erzielte er ausgerechnet an einem Sonntag."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Huerdenlaeufer haelt den Weltrekord ueber 400m Huerden der Herren, aufgestellt 2021 bei den Olympischen Spielen in Tokio?",
        answerA = "Rai Benjamin",
        answerB = "Abderrahman Samba",
        answerC = "Karsten Warholm",
        answerD = "Kerron Clement",
        correctAnswer = 2,
        explanation = "Karsten Warholm aus Norwegen lief am 3. August 2021 bei den Olympischen Spielen in Tokio die 400m Huerden in Weltrekordzeit von 45,94 Sekunden. Er unterbot damit seinen eigenen kurz zuvor aufgestellten Weltrekord und gewann Olympiagold.",
        difficulty = 4,
        funFact = "Warholm trug in Tokio einen einteiligen aerodynamischen Anzug und lief in einem Rennen, das von Experten als eines der schnellsten 400m-Huerdrennen aller Zeiten bezeichnet wird. Silbermedaillengewinner Rai Benjamin lief ebenfalls unter dem alten Weltrekord."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr lief Hicham El Guerrouj den Weltrekord ueber 1.500 Meter in 3:26,00 Minuten, der bis heute steht?",
        answerA = "1995",
        answerB = "1997",
        answerC = "1998",
        answerD = "2001",
        correctAnswer = 2,
        explanation = "Hicham El Guerrouj aus Marokko stellte am 14. Juli 1998 in Rom mit 3:26,00 Minuten den Weltrekord ueber 1.500 Meter auf. El Guerrouj ist einer der groessten Mittelstrecklaeufer aller Zeiten und gewann 2004 in Athen sowohl 1.500m als auch 5.000m Gold.",
        difficulty = 4,
        funFact = "El Guerrouj haelt auch den Weltrekord ueber eine Meile (1.609 m) mit 3:43,13 Minuten, ebenfalls aufgestellt 1999 in Rom auf derselben Bahn."
    ),

    Question(
        categoryId = 6,
        questionText = "Die Wettkampfkugel im Kugel-Stossen der Herren wiegt im internationalen Wettkampf exakt wie viel Kilogramm?",
        answerA = "6,00 kg",
        answerB = "6,25 kg",
        answerC = "7,26 kg",
        answerD = "8,00 kg",
        correctAnswer = 2,
        explanation = "Die Wettkampfkugel fuer Herren wiegt exakt 7,26 kg (entspricht 16 Pfund). Fuer Frauen betraegt das Gewicht 4 kg, fuer Junioren (unter 20) 6 kg. Der aktuelle Weltrekord liegt bei 23,37 m (Randy Barnes, 1990).",
        difficulty = 4,
        funFact = "Das Kugel-Stossen hat seine Wurzeln im mittelalterlichen Hochland-Spiel 'Putting the Stone', bei dem echte Feldsteine geworfen wurden. Die Standardisierung auf Metallkugeln erfolgte erst im 19. Jahrhundert."
    ),

    // --- SCHWIMM-REKORDE (6 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Wer haelt den Weltrekord im 100m Freistil der Herren (Langspur 50m-Becken), und welche Zeit wurde erzielt?",
        answerA = "Caeleb Dressel — 47,02 Sekunden",
        answerB = "Cesar Cielo — 46,91 Sekunden",
        answerC = "David Popovici — 46,86 Sekunden",
        answerD = "Kyle Chalmers — 47,08 Sekunden",
        correctAnswer = 2,
        explanation = "David Popovici aus Rumaenien stellte am 13. August 2022 bei den Europameisterschaften in Rom mit 46,86 Sekunden einen neuen Weltrekord ueber 100m Freistil auf und unterbot damit Caeleb Dressels Rekord von 47,02 Sekunden.",
        difficulty = 4,
        funFact = "David Popovici war bei seinem Weltrekord gerade 17 Jahre alt — er ist einer der juengsten Weltrekordhalter in der Geschichte des Schwimmsports."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Spielen gewann Michael Phelps seine beruehmten acht Goldmedaillen in einer einzigen Ausgabe?",
        answerA = "Athen 2004",
        answerB = "Peking 2008",
        answerC = "London 2012",
        answerD = "Rio 2016",
        correctAnswer = 1,
        explanation = "Michael Phelps gewann bei den Olympischen Spielen 2008 in Peking acht Goldmedaillen in acht Wettkampfen — ein bis dahin unvorstellbares Ergebnis. Er verbesserte damit Mark Spitz' Rekord von sieben Goldmedaillen bei einer Olympiade (Muenchen 1972).",
        difficulty = 4,
        funFact = "Phelps' engste Goldmedaille in Peking war die 100m Schmetterling, wo er den serbischen Schwimmer Milorad Cavic um 0,01 Sekunden schlug. Zeitlupenaufnahmen zeigten, dass Phelps mit einer halben Armzuglaeange zulegte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Schwimmtechnik-Reihenfolge ist beim Einzellagen-Wettkampf vorgeschrieben?",
        answerA = "Schmetterling, Brust, Ruecken, Freistil",
        answerB = "Ruecken, Schmetterling, Brust, Freistil",
        answerC = "Schmetterling, Ruecken, Brust, Freistil",
        answerD = "Brust, Schmetterling, Ruecken, Freistil",
        correctAnswer = 2,
        explanation = "Im Einzellagen-Wettbewerb wird in der Reihenfolge Schmetterling, Ruecken, Brust und zuletzt Freistil geschwommen. Diese Reihenfolge gilt sowohl fuer den 200m als auch den 400m Einzellagen. In der Lagenstaffel ist die Reihenfolge anders: Ruecken, Brust, Schmetterling, Freistil.",
        difficulty = 4,
        funFact = "Die unterschiedliche Reihenfolge in Einzel- und Staffellagen hat historische Gruende: In der Staffel beginnt der Rueckenschwimmer, der vom Beckenrand ins Wasser gleitet, waehrend im Einzelwettbewerb Schmetterling als anspruchsvollste Technik zuerst geschwommen wird."
    ),

    Question(
        categoryId = 6,
        questionText = "Bis zu welchen Olympischen Spielen gab es kein 1.500m Freistil fuer Frauen bei Olympia, und wer gewann es beim Ersteinsatz?",
        answerA = "Bis Rio 2016 — Sarah Sjoestroem gewann 2020",
        answerB = "Bis Tokio 2020 — Katie Ledecky gewann bei der Premiere",
        answerC = "Bis London 2012 — Allison Schmitt gewann 2016",
        answerD = "Bis Peking 2008 — Federica Pellegrini gewann 2012",
        correctAnswer = 1,
        explanation = "Bis zu den Olympischen Spielen 2020 in Tokio gab es bei Olympia kein 1.500m Freistil fuer Frauen. Die laengste olympische Distanz fuer Frauen war die 800m Freistil. In Tokio wurde erstmals das 1.500m Freistil fuer Frauen ins Programm aufgenommen — und Katie Ledecky gewann prompt die Goldmedaille.",
        difficulty = 4,
        funFact = "Umgekehrt wurde das 800m Freistil ebenfalls erst 2020 in Tokio fuer Maenner als olympische Disziplin eingefuehrt. Beide Geschlechter haben jetzt die gleichen Langstreckendisziplinen im Programm."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Schwimmer gewann 1988 in Seoul Olympiagold ueber 100m Freistil — und was war der beruehnteste Dopingfall dieser Spiele (andere Sportart)?",
        answerA = "Matt Biondi (USA) — Ben Johnson (Kanada, 100m Sprint) war der groesste Dopingfall",
        answerB = "Brent Furber (Australien) — Ben Johnson war der groesste Dopingfall",
        answerC = "Matt Biondi (USA) — Carl Lewis war der groesste Dopingfall",
        answerD = "Stephan Caron (Frankreich) — Ben Johnson war der groesste Dopingfall",
        correctAnswer = 0,
        explanation = "Matt Biondi (USA) gewann 100m Freistil Gold in Seoul 1988. Der beruehnteste Dopingfall jener Spiele war Ben Johnson (Kanada) im 100m Sprint — sein Weltrekord von 9,79 Sekunden wurde wegen Stanozolol-Positivbefundes gestrichen. Johnson musste die Goldmedaille zurueckgeben, die an Carl Lewis fiel.",
        difficulty = 4,
        funFact = "Trotz Johnsons Disqualifikation wurden bei nachtraeglichen Nachtests mehrere weitere 100m-Finalisten als dopingpositiv identifiziert. Manche Analytiker bezeichnen das Rennen daher als das 'schmutzigste Rennen der Geschichte'."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Nation dominierte bei den Leichtathletik-Weltmeisterschaften 1983 in Helsinki den Medaillenspiegel, und was war historisch bedeutsam an diesem Turnier?",
        answerA = "DDR mit 12 Goldmedaillen — erste WM ueberhaupt",
        answerB = "USA mit 15 Goldmedaillen — erste eigenstaendige Leichtathletik-WM",
        answerC = "Sowjetunion mit 18 Goldmedaillen — erste WM nach dem Boycott",
        answerD = "Kenia mit 9 Goldmedaillen — erste afrikanische WM-Dominanz",
        correctAnswer = 1,
        explanation = "Die USA dominierten die ersten Leichtathletik-Weltmeisterschaften 1983 in Helsinki klar mit 15 Goldmedaillen. Die WM 1983 war die Premiere der Leichtathletik-WM als eigenstaendige Veranstaltung ausserhalb der Olympischen Spiele — ein Meilenstein fuer die Sportart.",
        difficulty = 4,
        funFact = "Helsinki war zuvor Austragungsort der Olympischen Spiele 1952 — das Olympiastadion fand nach 31 Jahren wieder Weltklasse-Leichtathletik statt."
    ),

    // --- SPORTMEDIZIN & DOPING-SKANDALE (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welche biochemische Rolle spielt Laktat tatsaechlich im menschlichen Koerper bei intensiver Belastung — entgegen der verbreiteten Fehlannahme?",
        answerA = "Laktat verursacht Muskelkater und blockiert Kontraktionen",
        answerB = "Laktat dient als Brennstoff fuer Herzmuskel und langsame Muskelfasern, nicht als Abfallprodukt",
        answerC = "Laktat blockiert die Sauerstoffaufnahme in Mitochondrien",
        answerD = "Laktat regeneriert ATP direkt aus Glukose ohne Sauerstoff",
        correctAnswer = 1,
        explanation = "Laktat (nicht 'Milchsaeure', wie oft faelschlicherweise gesagt) entsteht bei anaerober Belastung und dient als wichtiger Energietraeger — es wird vom Herzmuskel, der Leber und langsamen Muskelfasern als Brennstoff genutzt. Laktat selbst verursacht keinen Muskelkater; dieser entsteht durch Mikrorisse und Entzuendungsprozesse.",
        difficulty = 4,
        funFact = "Die alte Lehrmeinung, Laktat sei ein Abfallprodukt, wurde in den 1980ern durch George Brooks widerlegt. Hochtrainierte Ausdauerathleten koennen Laktat besonders effizient als Energiequelle nutzen — ein wichtiger Trainingsanpassungseffekt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Radsportler wurde im Zusammenhang mit dem groessten Dopingskandal der Radsportgeschichte ueberführt, und welche Konsequenzen hatte dies?",
        answerA = "Jan Ullrich — lebenslange Sperre durch den CAS",
        answerB = "Lance Armstrong — Aberkennung aller 7 Tour-de-France-Titel und lebenslange Sperrung",
        answerC = "Floyd Landis — Aberkennung des Tour-Titels 2006 und 2 Jahre Sperre",
        answerD = "Marco Pantani — posthume Aberkennung aller Titel",
        correctAnswer = 1,
        explanation = "Lance Armstrong wurde 2012 von der USADA fuer schuldig befunden, das elaborierteste Doping-System in der Radsportgeschichte organisiert zu haben. Die UCI erkannte ihm alle sieben Tour-de-France-Titel (1999–2005) ab und erteilte eine lebenslange Wettkampfsperre. Armstrong gab die Vergehen 2013 in einem Oprah-Winfrey-Interview zu.",
        difficulty = 4,
        funFact = "Armstrong hatte jahrelang unter Eid geleugnet, gedopt zu haben, und Ex-Teamkollegen sowie Journalisten juristisch verfolgt. Erst Ermittlungen der USADA unter Travis Tygart brachten das System zu Fall."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter 'Blutdoping' im Sport, und welche drei Methoden werden unterschieden?",
        answerA = "Injektion synthetischer Sauerstofftraeger, Eigenblutinfusion und homologe Transfusion",
        answerB = "EPO-Injektion, Altitudetraining und normobare Hypoxiekammern",
        answerC = "Autologe, homologe und heterologe Bluttransfusion",
        answerD = "Erythropoetin-Gabe, Vollblut-Transfusion und Perfluorcarbon-Infusion",
        correctAnswer = 2,
        explanation = "Blutdoping umfasst drei Methoden: autologe Transfusion (eigenes Blut wird abgenommen und spaeter reinfundiert), homologe Transfusion (Blut eines anderen Menschen derselben Blutgruppe) und heterologe Transfusion. Ziel ist die Erhoehung der Sauerstoffkapazitaet durch mehr Erythrozyten.",
        difficulty = 4,
        funFact = "Autologes Blutdoping ist bis heute schwierig nachzuweisen. Der Athletenbiologische Pass (ABP) der WADA soll indirekt Blutdoping durch Laengsschnittauswertung von Blutwerten aufdecken."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr wurde Erythropoetin (EPO) auf die Verbotsliste gesetzt, und wann gab es erstmals einen zuverlaessigen Nachweistest?",
        answerA = "EPO verboten ab 1990, Nachweis erst ab 2000 (Sydney)",
        answerB = "EPO verboten ab 1994, Nachweis ab 1998",
        answerC = "EPO verboten ab 1998, Nachweis ab 2002",
        answerD = "EPO verboten ab 2000, Nachweis ab 2003",
        correctAnswer = 0,
        explanation = "EPO wurde 1990 auf die IOC-Verbotsliste gesetzt, obwohl es bis 2000 (Sydney) keinen zuverlaessigen Nachweistest gab. Erst zur Olympiade in Sydney entwickelten Forscher einen kombinierten Blut- und Urintest, der erstmals EPO-Doping nachweisen konnte. Diese Luecke nutzten viele Radfahrer und Laeufer massiv aus.",
        difficulty = 4,
        funFact = "In der EPO-Aera der 1990er Jahre starben mehrere junge Radprofis an Herzversagen — vermutlich durch die Kombination aus EPO-verursachter Blutverdickerung und naechtlicher Ruhebradykardie."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Festina-Skandal' und in welchem Jahr erschuetterte er den Radsport?",
        answerA = "1997 — Festina-Teamfahrer werden bei der Vuelta mit Steroiden erwischt",
        answerB = "1998 — Festina-Teamauto mit Dopingmitteln bei der Tour de France beschlagnahmt",
        answerC = "2000 — Festina-Teamarzt gesteht EPO-Programm vor CAS",
        answerD = "2001 — Festina-Sponsor entzieht Team das Geld nach Dopingvorwuerfen",
        correctAnswer = 1,
        explanation = "Im Juli 1998 beschlagnahmte der franzoesische Zoll an der belgisch-franzoesischen Grenze das Auto des Festina-Betreuers Willy Voet mit massenhaft verbotenen Substanzen. Die anschliessende Polizeiermittlung fuehrte zur Verhaftung von Teamarzt Erik Ryckaert und Teamleiter Bruno Roussel. Das Festina-Team wurde von der Tour ausgeschlossen.",
        difficulty = 4,
        funFact = "Festina-Fahrer Richard Virenque weinte bei seiner Anhoerung und schwor, nie absichtlich gedopt zu haben. Jahre spaeter gab er zu, dass er regelmaessig Erythropoetin erhalten hatte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches russische Staatsdoping-Programm wurde durch den McLaren-Report 2016 aufgedeckt?",
        answerA = "Athleten nahmen eigenstaendig verbotene Substanzen ohne staatliche Kenntnis",
        answerB = "Russland betrieb ein staatlich gesteuertes Doping-System bei Sotschi 2014 mit manipulierten Urinproben",
        answerC = "Russische Labore faelschten nur Daten ohne tatsaechlichen Substanzeinsatz",
        answerD = "Ausschliesslich Leichtathleten waren betroffen, andere Sportarten nicht",
        correctAnswer = 1,
        explanation = "Der McLaren-Report legte 2016 offen, dass Russland bei den Heimspielen 2014 in Sotschi ein staatliches Dopingsystem betrieb: Urinproben wurden durch ein Loch in der Wand des Kontrolllabors ausgetauscht, der FSB war involviert. Ueber 1.000 russische Athleten aus 30 Sportarten waren betroffen.",
        difficulty = 4,
        funFact = "Die Probenbehaelter (BEREG-KIT) galten als faelschungssicher — bis russische Wissenschaftler eine Methode entwickelten, den Deckel mit einem speziellen Werkzeug zu oeffnen, ohne sichtbare Spuren zu hinterlassen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches synthetische Anabolikum wurde im beruehntesten Dopingfall der Olympiageschichte (Ben Johnson, Seoul 1988) nachgewiesen?",
        answerA = "Testosteron",
        answerB = "Stanozolol",
        answerC = "Nandrolon",
        answerD = "Oxandrolon",
        correctAnswer = 1,
        explanation = "Bei Ben Johnson wurde nach seinem Weltrekordsprint (9,79 Sekunden) bei Olympia 1988 das synthetische Anabolikum Stanozolol nachgewiesen. Anabole Steroide wie Stanozolol erhoehen die Proteinsynthese in Muskelzellen, foerdern den Muskelaufbau und reduzieren die Regenerationszeit.",
        difficulty = 4,
        funFact = "Trotz Johnsons Disqualifikation wurden bei nachtraeglichen Nachtests mehrere weitere 100m-Finalisten von Seoul als dopingpositiv identifiziert."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet das 'DOMS'-Phaenomen in der Sportmedizin und nach wie vielen Stunden erreicht es seinen Hoehepunkt?",
        answerA = "Delayed Onset Muscle Soreness — Muskelkater, Hoehepunkt 24-72 Stunden nach Belastung",
        answerB = "Dynamic Overloading Muscle Stress — akuter Muskelschmerz, sofort nach Belastung",
        answerC = "Direct Oxidative Muscle Stress — oxidativer Muskelschaden, Hoehepunkt nach 6-12 Stunden",
        answerD = "Degenerative Overuse Muskuloskeletal Syndrome — chronischer Overuse, kein Hoehepunkt",
        correctAnswer = 0,
        explanation = "DOMS (Delayed Onset Muscle Soreness) ist der verspaetet einsetzende Muskelschmerz nach ungewohnter oder exzentrischer Belastung. Er entsteht durch Mikrorisse in Muskelfasern und nachfolgende Entzuendungsreaktionen. Der Hoehepunkt liegt typischerweise 24-72 Stunden nach der Belastung.",
        difficulty = 4,
        funFact = "Entgegen der Volksmeinung wird DOMS nicht durch Laktat verursacht — Laktat wird bereits Stunden nach Belastung wieder abgebaut. Die exakten Mechanismen des Muskelkaters sind wissenschaftlich noch immer nicht vollstaendig geklaert."
    ),

    // --- FIFA-GESCHICHTE (6 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr und in welcher Stadt wurde die FIFA gegruendet?",
        answerA = "1900 in London",
        answerB = "1904 in Paris",
        answerC = "1908 in Zuerich",
        answerD = "1910 in Amsterdam",
        correctAnswer = 1,
        explanation = "Die FIFA wurde am 21. Mai 1904 in Paris gegruendet. Zu den Gruendungsmitgliedern gehoerten Verbaende aus Belgien, Daenemark, Frankreich, den Niederlanden, Schweden, der Schweiz und Spanien. England war nicht bei der Gruendung dabei, trat aber 1905 bei.",
        difficulty = 4,
        funFact = "Die FIFA hatte bei ihrer Gruendung 7 Mitgliedsverbände — heute sind es 211, mehr als die UNO Mitgliedsstaaten hat."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer ist der laengste amtierende FIFA-Praesident in der Geschichte?",
        answerA = "Joao Havelange (Brasilien) — 24 Jahre (1974-1998)",
        answerB = "Jules Rimet (Frankreich) — 33 Jahre (1921-1954)",
        answerC = "Sepp Blatter (Schweiz) — 17 Jahre (1998-2015)",
        answerD = "Gianni Infantino (Schweiz) — seit 2016 im Amt",
        correctAnswer = 1,
        explanation = "Jules Rimet war von 1921 bis 1954 insgesamt 33 Jahre lang FIFA-Praesident — die laengste Amtszeit in der Geschichte des Verbands. Er trieb die Einfuehrung der FIFA Fussball-Weltmeisterschaft voran, die erstmals 1930 in Uruguay stattfand. Die urspruengliche WM-Trophae wurde zu Ehren Rimets nach ihm benannt.",
        difficulty = 4,
        funFact = "Der Jules-Rimet-Pokal wurde zweimal gestohlen: 1966 in England (und von einem Hund namens Pickles wiedergefunden) und 1983 in Brasilien, wo er nie mehr auftauchte — man vermutet, er wurde eingeschmolzen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer haelt den Rekord fuer die meisten Tore in der Geschichte der FIFA Fussball-Weltmeisterschaften?",
        answerA = "Ronaldo (Brasilien) — 15 Tore",
        answerB = "Miroslav Klose (Deutschland) — 16 Tore",
        answerC = "Gerd Mueller (Deutschland) — 14 Tore",
        answerD = "Just Fontaine (Frankreich) — 13 Tore",
        correctAnswer = 1,
        explanation = "Miroslav Klose ist mit 16 WM-Toren (2002: 5, 2006: 5, 2010: 4, 2014: 2) der erfolgreichste WM-Torschuetze aller Zeiten. Er uebertraf 2014 Ronaldo Nazarios 15 Tore. Just Fontaines 13 Tore bei einem einzigen Turnier (1958) bleiben jedoch der Turnier-Rekord.",
        difficulty = 4,
        funFact = "Just Fontaine schoss seine 13 Tore bei der WM 1958 in Schweden in nur 6 Spielen — nachdem er eigentlich nur als Ersatz mitgefahren war, weil der Stammstuermer verletzt ausgefallen war."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches war die erste FIFA Fussball-Weltmeisterschaft, bei der Elfmeterschiessen als Entscheidungsmechanismus in der K.o.-Runde eingesetzt wurde?",
        answerA = "WM 1974 in Deutschland",
        answerB = "WM 1978 in Argentinien",
        answerC = "WM 1982 in Spanien",
        answerD = "WM 1986 in Mexiko",
        correctAnswer = 2,
        explanation = "Elfmeterschiessen wurde erstmals bei der WM 1982 in Spanien als Entscheidungsmethode eingesetzt. Das erste WM-Elfmeterschiessen fand im Halbfinale zwischen Deutschland und Frankreich statt — Deutschland gewann und zog ins Finale ein.",
        difficulty = 4,
        funFact = "Das erste WM-Elfmeterschiessen (BRD vs. Frankreich 1982) ist bis heute eines der dramatischsten Spiele der WM-Geschichte. Frankreich fuehrte in der Verlaengerung 3:1, Deutschland glich aus und gewann im Elfmeterschiessen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie hoch war der hoechste je bei einer FIFA-Weltmeisterschaft erzielte Sieg?",
        answerA = "Deutschland 8:0 gegen Saudi-Arabien (2002)",
        answerB = "Ungarn 10:1 gegen El Salvador (1982)",
        answerC = "Schweden 8:0 gegen Kuba (1938)",
        answerD = "Jugoslawien 9:0 gegen Zaire (1974)",
        correctAnswer = 1,
        explanation = "Das hoechste Ergebnis bei einer FIFA-Weltmeisterschaft war Ungarn gegen El Salvador mit 10:1 bei der WM 1982 in Spanien. Rekordtorschuetze war Laszlo Kiss mit drei Toren als Einwechselspieler.",
        difficulty = 4,
        funFact = "Das zweithöchste Ergebnis war Ungarn gegen Deutschland mit 8:3 bei der WM 1954 — obwohl Ungarn als Favorit galt und im Finale dann gegen Deutschland verlor ('Das Wunder von Bern')."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher FIFA-Praesident trat 2015 unter dem Druck von Korruptionsermittlungen zurueck, obwohl er kurz zuvor fuer eine fuenfte Amtszeit wiedergewaehlt worden war?",
        answerA = "Joao Havelange",
        answerB = "Sepp Blatter",
        answerC = "Michel Platini",
        answerD = "Jack Warner",
        correctAnswer = 1,
        explanation = "Sepp Blatter trat am 2. Juni 2015 als FIFA-Praesident zurueck — nur vier Tage nach seiner Wiederwahl fuer eine fuenfte Amtszeit. Dies geschah unter dem Druck massiver US-Ermittlungen (FBI/DoJ) gegen FIFA-Funktionaere wegen Korruption und Bestechung.",
        difficulty = 4,
        funFact = "Der FIFA-Korruptionsskandal 2015 fuehrte zu 47 Anklagen in den USA gegen FIFA-Offizielle und Sportvermarkter — unter anderem wegen Bestechung im Zusammenhang mit der Vergabe der WM 2018 und 2022."
    ),

    // --- UEFA-WETTBEWERBE & HISTORISCHE SPORTMOMENTE (12 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welche Mannschaft gewann die meisten UEFA Champions-League-Titel (Stand 2024)?",
        answerA = "FC Barcelona — 5 Titel",
        answerB = "AC Mailand — 7 Titel",
        answerC = "Real Madrid — 15 Titel",
        answerD = "FC Bayern Muenchen — 6 Titel",
        correctAnswer = 2,
        explanation = "Real Madrid ist mit 15 Champions-League-Titeln (Stand 2024) der erfolgreichste Verein in der Geschichte des Wettbewerbs. Hinzu kommen 6 Europapokal-der-Landesmeister-Titel aus der Vorgaenger-Aera.",
        difficulty = 4,
        funFact = "Real Madrid gewann den Europapokal der Landesmeister fuenfmal in Folge bei seiner Einfuehrung 1956-1960 — eine Dominanz, die seither kein Verein mehr erreicht hat."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Spieler haelt den Rekord fuer die meisten Tore in der Geschichte der UEFA Champions League?",
        answerA = "Lionel Messi — 129 Tore",
        answerB = "Cristiano Ronaldo — ueber 140 Tore",
        answerC = "Raul — 71 Tore",
        answerD = "Robert Lewandowski — 91 Tore",
        correctAnswer = 1,
        explanation = "Cristiano Ronaldo haelt mit ueber 140 Toren den Rekord als erfolgreichster Torschuetze in der Geschichte der UEFA Champions League. Er erzielte Tore fuer Manchester United, Real Madrid und Juventus Turin. Lionel Messi folgt auf Platz zwei.",
        difficulty = 4,
        funFact = "Ronaldo erzielte allein fuer Real Madrid ueber 100 Champions-League-Tore — in der Aera 2013-2018 traf er in praktisch jedem Spiel und dominierte den Wettbewerb wie kaum ein anderer Einzelspieler."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Spieler traf im UEFA Champions League Finale 2005 in Istanbul fuer den AC Mailand das erste Tor innerhalb der ersten Minute?",
        answerA = "Andriy Shevchenko",
        answerB = "Kaka",
        answerC = "Paolo Maldini",
        answerD = "Hernan Crespo",
        correctAnswer = 2,
        explanation = "Paolo Maldini erzielte das 1:0 fuer den AC Mailand im Champions-League-Finale 2005 in Istanbul bereits in der 53. Sekunde — das schnellste Tor in einem Champions-League-Finale. Hernan Crespo erhoehte auf 3:0, ehe Liverpool durch drei Tore in sechs Minuten ausglich und im Elfmeterschiessen gewann.",
        difficulty = 4,
        funFact = "Das 'Wunder von Istanbul' gilt als eines der unglaublichsten Comebacks der Fussballgeschichte. Liverpool-Torwart Jerzy Dudek parierte im Elfmeterschiessen entscheidende Schuesse mit der von Bruce Grobbelaar inspirierten 'Spaghetti-Bein'-Technik."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann die UEFA Europameisterschaft 2016 in Frankreich und besiegte dabei den Gastgeber im Finale?",
        answerA = "Deutschland",
        answerB = "Spanien",
        answerC = "Portugal",
        answerD = "Wales",
        correctAnswer = 2,
        explanation = "Portugal gewann die UEFA Europameisterschaft 2016 in Frankreich mit 1:0 (nach Verlaengerung) gegen den Gastgeber Frankreich. Cristiano Ronaldo verletzte sich frueh im Finale und feuerte sein Team vom Seitenrand an. Eder erzielte das entscheidende Tor in der 109. Minute.",
        difficulty = 4,
        funFact = "Portugal erreichte die EM 2016 als schlechtestplatzierte Mannschaft der Gruppenphase, die noch weiterkam. Sie wurden als einer der besten Dritten weitergeleitet und gewannen schliesslich den Titel."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war der 'Miracle on Ice' und bei welchen Olympischen Spielen ereignete er sich?",
        answerA = "USA besiegt Kanada bei der Eishockey-WM 1960 im Finale",
        answerB = "Das US-Eishockeyteam besiegt die Sowjetunion 4:3 bei den Olympischen Winterspielen 1980 in Lake Placid",
        answerC = "USA gewinnt Eishockey-Gold 1980 gegen Finland im Finale",
        answerD = "Kanada besiegt die Sowjetunion in der Summit Series 1972",
        correctAnswer = 1,
        explanation = "Der 'Miracle on Ice' ereignete sich am 22. Februar 1980 bei den Olympischen Winterspielen in Lake Placid: Das US-Eishockeyteam, bestehend aus Amateurspielern, besiegte die als unschlagbar geltende sowjetische Nationalmannschaft mit 4:3. Anschliessend gewann die USA das Finale gegen Finnland und holte Gold.",
        difficulty = 4,
        funFact = "Die Sowjets hatten die USA kurz vor Olympia in einem Testspiel 10:3 vernichtet. US-Trainer Herb Brooks wahlte gezielt Spieler, die miteinander harmonierten, und liess sie bis zur Erschoepfung trainieren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches historische Box-Event wird als 'Rumble in the Jungle' bezeichnet und wann und wo fand es statt?",
        answerA = "Muhammad Ali vs. Joe Frazier, Manila 1975",
        answerB = "Muhammad Ali vs. George Foreman, Kinshasa (Zaire) 1974",
        answerC = "Joe Frazier vs. George Foreman, Kingston (Jamaika) 1973",
        answerD = "Muhammad Ali vs. Sonny Liston, Miami Beach 1964",
        correctAnswer = 1,
        explanation = "Der 'Rumble in the Jungle' war der Schwergewichts-WM-Kampf zwischen Muhammad Ali und George Foreman am 30. Oktober 1974 in Kinshasa, Zaire. Ali gewann durch KO in Runde acht und holte damit den Weltmeistertitel zurueck. Ali entwickelte die 'Rope-a-dope'-Strategie: Er liess Foreman sich erschoepfen, bis er zuschlagen konnte.",
        difficulty = 4,
        funFact = "Foreman sagte spaeter, er sei ins Match gegangen und haette Alis Trick erst bemerkt, als er am Boden lag. Er war so erschoepft, dass er im letzten Moment zu muede war, die Arme zu heben."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war die 'Fosbury-Flop'-Revolution im Hochsprung und bei welchen Olympischen Spielen wurde diese Technik erstmals eingesetzt?",
        answerA = "Dick Fosbury gewann Gold in Mexiko City 1968 mit einer voellig neuartigen Rueckwaertsrolle ueber die Latte",
        answerB = "Dick Fosbury entwickelte den Flop 1972 in Muenchen und gewann Silber",
        answerC = "Valery Brumel verbesserte Fosburys Technik und gewann damit 1968 Gold",
        answerD = "Die Fosbury-Technik wurde 1964 in Tokio eingefuehrt, aber erst 1972 olympisch erlaubt",
        correctAnswer = 0,
        explanation = "Dick Fosbury revolutionierte den Hochsprung bei den Olympischen Spielen 1968 in Mexico City: Er sprang als erster olympischer Konkurrent rueckwaerts und mit dem Ruecken nach unten ueber die Latte und gewann Gold mit 2,24 Metern. Die Technik heisst seither 'Fosbury-Flop' und ist die universell verwendete Hochsprungtechnik.",
        difficulty = 4,
        funFact = "Vor dem Fosbury-Flop waren Straddle (Bauchrolle) und Western Roll die ueblichen Hochsprungtechniken. Manche Trainer hielten Fosburys Technik anfangs fuer gefaehrlich und anatomisch falsch."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war das 'Wunder von Bern' und warum hat es in der deutschen Sportgeschichte eine besondere kulturelle Bedeutung?",
        answerA = "Deutschland schlug England 1966 im WM-Halbfinale und gewann erstmals die Weltmeisterschaft",
        answerB = "Westdeutschland gewann 1954 gegen den favorisierten Ungarn die WM — ein Symbol fuer den Neuanfang nach dem Krieg",
        answerC = "Deutschland gewann 1974 das WM-Finale gegen die Niederlande und zementierte die Dominanz in Europa",
        answerD = "Deutschland schlug Brasilien 7:1 bei der WM 2014 in einem historischen Halbfinale",
        correctAnswer = 1,
        explanation = "Das 'Wunder von Bern' bezeichnet den WM-Finalsieg Westdeutschlands gegen Ungarn 3:2 am 4. Juli 1954 im Berner Wankdorfstadion. Ungarn galt als das beste Team der Welt und hatte Deutschland in der Vorrunde 8:3 geschlagen. Der Sieg gilt als Symbol des deutschen Nachkriegswiederaufstiegs.",
        difficulty = 4,
        funFact = "Der Torjubel von Helmut Rahn nach seinem entscheidenden dritten Tor, kommentiert von Herbert Zimmermann ('Schiesst Rahn! Tor! Tor! Tor! Deutschland ist Weltmeister!'), gehoert zu den beruehntesten Radiomomenten der deutschen Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler gewann 2001 in Wimbledon als Aussenseiter gegen Pete Sampras und beendete damit dessen Wimbledon-Unbesiegbarkeitsserie von 31 Spielen?",
        answerA = "Andre Agassi",
        answerB = "Roger Federer",
        answerC = "Lleyton Hewitt",
        answerD = "Tim Henman",
        correctAnswer = 1,
        explanation = "Roger Federer besiegte Pete Sampras im Viertelfinale von Wimbledon 2001 mit 7:6, 5:7, 6:4, 6:7, 7:5 und beendete damit Sampras' Wimbledon-Unbesiegbarkeitsserie von 31 Spielen. Dieser Sieg gilt als der Beginn von Federers Aufstieg zur Weltspitze.",
        difficulty = 4,
        funFact = "Sampras sagte nach der Niederlage, er haette gespuert, dass Federer eines Tages alle seine Rekorde brechen werde. Federer uebertraf spaeter tatsaechlich Sampras' Rekord von 14 Grand-Slam-Titeln und stellte 20 auf."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet man als den 'Black Sox Scandal' von 1919 und welche Konsequenzen hatte er?",
        answerA = "Acht Chicago-Cubs-Spieler setzten auf den Sieg der anderen Mannschaft und wurden lebenslang gesperrt",
        answerB = "Acht Chicago-White-Sox-Spieler manipulierten die World Series 1919 zugunsten der Cincinnati Reds",
        answerC = "Die New York Yankees wurden des Spielmanipulationsvorwurfes beschuldigt und aus der Liga ausgeschlossen",
        answerD = "Sechs Boston-Red-Sox-Spieler akzeptierten Bestechungsgelder vor der World Series 1921",
        correctAnswer = 1,
        explanation = "Acht Spieler der Chicago White Sox — darunter Shoeless Joe Jackson — wurden beschuldigt, die World Series 1919 gegen Geld absichtlich verloren zu haben. Obwohl alle acht vor Gericht freigesprochen wurden, sperrte Commissioner Kenesaw Mountain Landis alle acht Spieler lebenslang aus dem Baseball.",
        difficulty = 4,
        funFact = "Shoeless Joe Jackson hatte waehrend der manipulierten Serie dennoch eine statistische Glanzleistung erbracht, was Zweifel an seiner vollen Mitschuld naehrt. Er blieb bis zu seinem Tod 1951 aus der Baseball-Hall-of-Fame ausgeschlossen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche zwei Fahrer kollidierten beim Formel-1-Saisonfinale 1994 in Adelaide, was zu einem hoechst kontroversen WM-Titelgewinn fuehrte?",
        answerA = "Ayrton Senna und Alain Prost — Suzuka 1989",
        answerB = "Max Verstappen und Lewis Hamilton — Silverstone 2021",
        answerC = "Michael Schumacher und Damon Hill — Adelaide 1994",
        answerD = "Nico Rosberg und Lewis Hamilton — Monaco 2014",
        correctAnswer = 2,
        explanation = "Die Kollision zwischen Michael Schumacher (Benetton) und Damon Hill (Williams) beim Saisonfinale 1994 in Adelaide war eines der kontroversesten Ereignisse der Formel-1-Geschichte. Schumacher, der knapp vor Hill in der WM-Wertung lag, kollidierte mit Hill — beide schieden aus, Schumacher wurde Weltmeister mit einem Punkt Vorsprung.",
        difficulty = 4,
        funFact = "Die FIA untersuchte den Vorfall und fand keine ausreichenden Beweise fuer eine absichtliche Kollision. Hill bezeichnete es spaeter als 'Opportunismus'. Schumacher selbst kommentierte den Vorfall nie eindeutig."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Tennisereignis gilt als 'Krieg der Geschlechter' von 1973, und wer siegte dabei?",
        answerA = "Billie Jean King schlug Bobby Riggs in drei Saetzen vor ueber 30.000 Zuschauern in Houston",
        answerB = "Margaret Court gewann gegen Bobby Riggs und zeigte Ueberlegenheit der Frauen",
        answerC = "Chris Evert schlug Ilie Nastase in einem Exhibitionsmatch als Zeichen fuer Gleichberechtigung",
        answerD = "Martina Navratilova schlug John McEnroe in einem Charitaetswettkampf",
        correctAnswer = 0,
        explanation = "Der 'Krieg der Geschlechter' (Battle of the Sexes) fand am 20. September 1973 im Houston Astrodome statt. Die 29-jaehrige Billie Jean King besiegte den 55-jaehrigen Ex-Wimbledonsieger Bobby Riggs, der behauptet hatte, keine Frau koennte ihn schlagen, mit 6:4, 6:3, 6:3 vor 30.492 Zuschauern — Weltrekord fuer ein Tennismatch.",
        difficulty = 4,
        funFact = "Bobby Riggs hatte zuvor Margaret Court im Mothers-Day-Showdown 6:2, 6:1 besiegt. Es soll Berichte geben, wonach Riggs gegen King absichtlich verlor, weil er unter erheblichem Druck von Wettschulden stand."
    )

)
