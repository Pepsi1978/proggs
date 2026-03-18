package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsExpert5(): List<Question> = listOf(

    // --- TENNIS GRAND SLAMS (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Wer gewann die Australian Open 2005 und besiegte im Finale Lleyton Hewitt mit 3:6, 6:3, 6:4, 6:4?",
        answerA = "Roger Federer",
        answerB = "Marat Safin",
        answerC = "Andy Roddick",
        answerD = "Ivan Ljubicic",
        correctAnswer = 1,
        explanation = "Marat Safin schlug im Finale der Australian Open 2005 den Lokalmatador Lleyton Hewitt in vier Saetzen. Es war Safins zweiter und letzter Grand-Slam-Titel seiner Karriere nach den US Open 2000.",
        difficulty = 4,
        funFact = "Safin hatte im Halbfinale Roger Federer besiegt, der damals eine Rekordserie von 26 Grand-Slam-Matches in Folge ohne Niederlage aufwies."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Spielerin gewann die French Open 2009 und beendete damit die Dominanz der Williams-Schwestern bei diesem Turnier?",
        answerA = "Jelena Jankovic",
        answerB = "Dinara Safina",
        answerC = "Svetlana Kuznetsova",
        answerD = "Ana Ivanovic",
        correctAnswer = 2,
        explanation = "Svetlana Kuznetsova gewann Roland Garros 2009 durch einen 6:4, 6:2-Finalsieg ueber Dinara Safina. Es war Kuznetsowas zweiter Grand-Slam-Titel nach den US Open 2004.",
        difficulty = 4,
        funFact = "Dinara Safina erreichte 2009 dreimal hintereinander ein Grand-Slam-Finale (Australian Open, French Open, Wimbledon), verlor aber alle drei."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie viele French-Open-Titel hat Rafael Nadal zwischen 2005 und 2023 insgesamt gewonnen?",
        answerA = "12",
        answerB = "13",
        answerC = "14",
        answerD = "11",
        correctAnswer = 2,
        explanation = "Rafael Nadal gewann Roland Garros insgesamt 14 Mal (2005-2008, 2010-2014, 2017-2020, 2022). Damit haelt er den alleinigen Rekord fuer die meisten Titel bei einem einzelnen Grand-Slam-Turnier.",
        difficulty = 4,
        funFact = "Nadals einzige Niederlagen bei den French Open gingen an Robin Soederling (2009) und Novak Djokovic (2021 Halbfinale und 2015 Viertelfinale)."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr und gegen welchen Gegner gewann Stan Wawrinka seinen ersten Grand-Slam-Titel bei den Australian Open?",
        answerA = "2014 gegen Rafael Nadal",
        answerB = "2015 gegen Novak Djokovic",
        answerC = "2014 gegen Novak Djokovic",
        answerD = "2013 gegen Andy Murray",
        correctAnswer = 0,
        explanation = "Stan Wawrinka besiegte Rafael Nadal im Finale der Australian Open 2014 mit 6:3, 6:2, 3:6, 6:3. Nadal trat aufgrund eines Rueckenleidens im dritten Satz sichtlich angeschlagen an.",
        difficulty = 4,
        funFact = "Wawrinka ist einer von wenigen Spielern, der alle vier Grand Slams mindestens einmal gewonnen hat — Australian Open 2014, French Open 2015, US Open 2016."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Spieler hielt bis 2011 den Rekord fuer die meisten Wimbledon-Titel bei den Herren (6 Titel)?",
        answerA = "Pete Sampras",
        answerB = "Bjorn Borg",
        answerC = "Boris Becker",
        answerD = "John McEnroe",
        correctAnswer = 0,
        explanation = "Pete Sampras gewann Wimbledon siebenmal (1993-1995, 1997-2000) und haelt damit nach Roger Federer (8 Titel) den zweithoechsten Rekord. Sampras dominierte den Rasen in den 1990er Jahren wie kein anderer.",
        difficulty = 4,
        funFact = "Bjorn Borg gewann Wimbledon fuenfmal in Folge (1976-1980), trat dann aber mit 26 Jahren zurueck, bevor er die Rekordmarke von Sampras haette brechen koennen."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer gewann die US Open 1994 im Alter von 17 Jahren und wurde damit zur juengsten US-Open-Siegerin der Profi-Aera?",
        answerA = "Martina Hingis",
        answerB = "Arantxa Sanchez Vicario",
        answerC = "Jennifer Capriati",
        answerD = "Steffi Graf",
        correctAnswer = 1,
        explanation = "Arantxa Sanchez Vicario gewann die US Open 1994 durch einen Sieg ueber Steffi Graf. Sanchez Vicario war zu diesem Zeitpunkt 22 Jahre alt — der Rekord der juengsten Siegerin der Profi-Aera gehoert Tracey Austin (1979, 16 Jahre).",
        difficulty = 4,
        funFact = "Die juengste Grand-Slam-Siegerin der Neuzeit ist Martina Hingis, die 1997 die Australian Open im Alter von 16 Jahren und 3 Monaten gewann."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches ungewoehnliche Material wurde beim Wimbledon-Court-Belag bis 1960er-Jahre auch eingesetzt, bevor auf reinen Rasen standardisiert wurde?",
        answerA = "Roter Ziegelstaub",
        answerB = "Hartgummi",
        answerC = "Gemischtes Gras mit Lehm",
        answerD = "Kunstrasen",
        correctAnswer = 2,
        explanation = "Wimbledon nutzte historisch gemischte Grasmischungen mit unterschiedlichen Lehmuntergruenden. Seit den 1990er Jahren wird die Rasenqualitaet durch praezisere Pflege und einheitlichere Grasarten kontinuierlich verbessert.",
        difficulty = 4,
        funFact = "Der Wimbledon-Rasen wird aus einer spezifischen Mischung aus Perennial Ryegrass (100%) gezogen und auf exakt 8mm Schnitthoehe gehalten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Spieler gewann bei den Australian Open 2008 sein erstes Grand-Slam-Finale gegen Jo-Wilfried Tsonga und blieb damit in Australien ungeschlagen?",
        answerA = "Roger Federer",
        answerB = "Rafael Nadal",
        answerC = "Novak Djokovic",
        answerD = "Andy Roddick",
        correctAnswer = 2,
        explanation = "Novak Djokovic gewann sein erstes Grand-Slam-Turnier bei den Australian Open 2008 durch einen 4:6, 6:4, 6:3, 7:6-Sieg gegen den ueberraschend ins Finale vorgestossenen Jo-Wilfried Tsonga.",
        difficulty = 4,
        funFact = "Tsonga spielte bei diesem Turnier das Turnier seines Lebens und schlug im Halbfinale Rafael Nadal — eine der groessten Ueberraschungen in der jungen Grand-Slam-Geschichte."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst der Fachbegriff fuer den Grand Slam im Tennis, wenn ein Spieler alle vier Major-Turniere im selben Kalenderjahr gewinnt?",
        answerA = "Golden Slam",
        answerB = "Calendar Year Grand Slam",
        answerC = "Super Slam",
        answerD = "Royal Slam",
        correctAnswer = 1,
        explanation = "Der 'Calendar Year Grand Slam' bezeichnet den Gewinn aller vier Grand Slams (Australian Open, French Open, Wimbledon, US Open) innerhalb eines Kalenderjahres. Steffi Graf gelang dies 1988 als bisher letzter Spielerin — zusaetzlich mit Olympiagold = Golden Slam.",
        difficulty = 4,
        funFact = "Bei den Herren gelang der Calendar Year Grand Slam zuletzt Donald Budge (1938) und Rod Laver (1962 und 1969). Novak Djokovic scheiterte 2021 im US-Open-Finale knapp daran."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Spieler gewann Wimbledon 2001 und beendete damit Pete Sampras' Siegesserie von 31 Wimbledon-Matches in Folge?",
        answerA = "Pat Rafter",
        answerB = "Tim Henman",
        answerC = "Goran Ivanisevic",
        answerD = "Lleyton Hewitt",
        correctAnswer = 2,
        explanation = "Goran Ivanisevic schlug Pete Sampras im Viertelfinale 2001 und gewann anschliessend als Wildcard-Spieler seinen einzigen Grand-Slam-Titel. Das Finale gegen Pat Rafter gilt als eines der emotionalsten in Wimbledon-Geschichte.",
        difficulty = 4,
        funFact = "Ivanisevic hatte dreimal zuvor das Wimbledon-Finale verloren (1992, 1994, 1998) bevor er als Wildcard-Spieler mit Weltranglistenplatz 125 endlich triumphierte."
    ),

    // --- FORMEL 1 TECHNIK UND GESCHICHTE (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welches aerodynamische Konzept fuerhrte Red Bull Racing zur Dominanz in der Saison 2011 und wurde als entscheidend fuer deren Downforce-Vorsprung angesehen?",
        answerA = "Double Diffuser",
        answerB = "Blown Diffuser (Auspuffstroemung in den Diffusor)",
        answerC = "F-Duct System",
        answerD = "Active Suspension mit Bodywork",
        correctAnswer = 1,
        explanation = "Der 'Blown Diffuser' nutzte die Auspuffgase um den Diffusor bei niedrigen Geschwindigkeiten aktiv mit Stroemung zu versorgen und so zusaetzlichen Abtrieb zu erzeugen. Red Bull perfektionierte dieses Konzept und dominierte damit 2011.",
        difficulty = 4,
        funFact = "Die FIA verbot den Blown Diffuser ab 2012 durch Auspuffpositions-Vorschriften, was Red Bull trotzdem nicht hinderte, erneut Weltmeister zu werden."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr wurde das KERS-System (Kinetic Energy Recovery System) in der Formel 1 erstmals eingesetzt, und welche Leistung durfte es maximal bereitstellen?",
        answerA = "2007, maximal 60 kW",
        answerB = "2009, maximal 60 kW",
        answerC = "2010, maximal 80 kW",
        answerD = "2009, maximal 80 kW",
        correctAnswer = 1,
        explanation = "KERS wurde 2009 in der Formel 1 eingefuehrt und durfte maximal 60 kW (etwa 82 PS) fuer 6,67 Sekunden pro Runde abgeben. Die meisten Teams verzichteten 2009 jedoch aus Gewichtsgruenden darauf.",
        difficulty = 4,
        funFact = "KERS wurde 2011 wieder eingefuehrt und 2014 durch das viel leistungsstarkere MGU-K-System (Motor Generator Unit - Kinetic) mit 120 kW ersetzt, das Teil des hybriden Antriebsstrangs ist."
    ),

    Question(
        categoryId = 6,
        questionText = "Wer war der erste Fahrer der Formel-1-Geschichte, der fuenf Weltmeistertitel gewann, und in welchem Jahr sicherte er sich den fuenften?",
        answerA = "Ayrton Senna, 1991",
        answerB = "Michael Schumacher, 2002",
        answerC = "Juan Manuel Fangio, 1957",
        answerD = "Alain Prost, 1993",
        correctAnswer = 2,
        explanation = "Juan Manuel Fangio gewann seinen fuenften und letzten Weltmeistertitel 1957 mit Maserati. Er hatte zuvor mit Alfa Romeo (1951), Mercedes (1954, 1955) und Ferrari (1956) triumphiert — als einziger Fahrer mit vier verschiedenen Konstrukteuren.",
        difficulty = 4,
        funFact = "Fangios Rekord von fuenf Weltmeisterschaften bestand 46 Jahre bis Michael Schumacher ihn 2003 mit seinem sechsten Titel brach."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Regelwerk fuerhrte die Formel 1 2014 ein und veraenderte die Motorentechnik grundlegend?",
        answerA = "Natuerlich abgesaugte 2,4-Liter-V8 durch turbocharged 1,6-Liter-V6-Hybrid",
        answerB = "Natuerlich abgesaugte 3,0-Liter-V10 durch turbocharged 2,0-Liter-V8-Hybrid",
        answerC = "Natuerlich abgesaugte 2,4-Liter-V8 durch natuerlich abgesaugte 2,0-Liter-V6",
        answerD = "Turbocharged 1,5-Liter-V4 durch turbocharged 1,6-Liter-V6-Hybrid",
        correctAnswer = 0,
        explanation = "2014 ersetzte die FIA die 2,4-Liter-V8-Saugmotoren durch hochkomplexe 1,6-Liter-V6-Turbohybrid-Einheiten (Power Units), bestehend aus Verbrennungsmotor, MGU-K, MGU-H, ES und TC. Mercedes dominierte sofort mit ueberragender Motorleistung.",
        difficulty = 4,
        funFact = "Die neuen Hybrid-Power-Units leisten ueber 1000 PS, wobei der elektrische Anteil etwa 160 PS beisteuert. Der thermische Wirkungsgrad uebertrifft mit ueber 50% selbst modernste Serienfahrzeuge."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie heisst das Prinzip, nach dem Formel-1-Reifen bei Pirelli seit 2011 nach Farbe markiert werden, und welche Mischung ist die haerteste?",
        answerA = "Das Compound-System — haerteste ist Weiss/Hard",
        answerB = "Das Compound-System — haerteste ist Orange/Hard",
        answerC = "Das Colour-Coding-System — haerteste ist Blau/Hard",
        answerD = "Das Compound-System — haerteste ist Silber/Hard",
        correctAnswer = 0,
        explanation = "Pirelli markiert seine Formel-1-Reifen nach Compound (C1-C5), wobei C1 am haertesten und C5 am weichsten ist. Die Farben variieren je nach Rennen: Hard=Weiss, Medium=Gelb, Soft=Rot sind die drei Mischungen die an einem Rennwochenende eingesetzt werden.",
        difficulty = 4,
        funFact = "Pirelli stellt pro Rennen drei Compounds zur Verfuegung, ausgewaehlt aus ihren fuenf Grundmischungen. Die Auswahl haengt von Streckenprofil und erwarteten Temperaturen ab."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Grand Prix der Formel-1-Geschichte gilt als der am wenigsten geplante Rennsieg eines Fahrers, da er als Dritter gestartet und gewann, weil beide fuehrenden Fahrer ausfuhren?",
        answerA = "Jochen Rindt, Monaco GP 1970",
        answerB = "Nico Rosberg, Australien GP 2008",
        answerC = "Oliver Panis, Monaco GP 1996",
        answerD = "Jarno Trulli, Monaco GP 2004",
        correctAnswer = 2,
        explanation = "Oliver Panis gewann den Monaco GP 1996 fuer Ligier als einer der groessten Ueberraschungssieger der F1-Geschichte. Bei einem Massenunfall schieden 10 von 21 Fahrern aus, und Panis ueberquerte als einer von nur acht Finishern die Ziellinie.",
        difficulty = 4,
        funFact = "Es war Panis' einziger Formel-1-Sieg seiner Karriere. Er fuhr fuer das franzoesische Team Ligier, das kurz danach von Alain Prost uebernommen und in Prost Grand Prix umbenannt wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Parc Ferme'-Regeln in der Formel 1, und seit wann gilt das strikte Verbot, das Auto nach dem Qualifying zu veraendern?",
        answerA = "Verbot von Aenderungen seit 2003, Reifenwechsel verboten",
        answerB = "Verbot von Aenderungen seit 2008, ausser Sicherheitsreparaturen",
        answerC = "Verbot von mechanischen Aenderungen seit 1998, Reifentausch moeglich",
        answerD = "Verbot seit 2010, komplett unveraendertes Setup",
        correctAnswer = 1,
        explanation = "Seit 2008 gilt der strenge Parc-Ferme-Status ab dem Qualifying: Das Auto darf nicht mehr mechanisch veraendert werden, ausser bei nachgewiesenen Sicherheitsrisiken. Ausnahmen beduerften der Genehmigung der Rennleitung.",
        difficulty = 4,
        funFact = "Der Begriff 'Parc Ferme' stammt aus dem Rallye-Sport, wo Fahrzeuge in einem gesicherten Bereich abgestellt werden um Manipulationen zwischen Wertungspruefungen zu verhindern."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie funktioniert das DRS-System (Drag Reduction System) technisch, und in welchem Jahr wurde es in der Formel 1 eingefuehrt?",
        answerA = "Heckfluegelklappe oeffnet sich hydraulisch, eingefuehrt 2010",
        answerB = "Heckfluegelklappe oeffnet sich per Fahreraktivierung in DRS-Zonen, eingefuehrt 2011",
        answerC = "Frontfluegel flacht automatisch ab bei hohen Geschwindigkeiten, eingefuehrt 2011",
        answerD = "Gesamtes Aero-Paket reduziert automatisch Downforce auf Geraden, eingefuehrt 2012",
        correctAnswer = 1,
        explanation = "DRS wurde 2011 eingefuehrt und erlaubt dem Fahrer in definierten Streckenzonen, die Heckfluegelklappe zu oeffnen wenn er sich innerhalb von einer Sekunde zum Vordermann befindet. Dies reduziert den Luftwiderstand um ca. 10-12 km/h zusaetzliche Hoechstgeschwindigkeit.",
        difficulty = 4,
        funFact = "DRS wurde entwickelt um Ueberholmanoeuvre zu erleichtern, die durch aerodynamisch ueberlegene Autos immer seltener wurden. Kritiker bezeichnen es als 'kuenstliches Ueberholen'."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Fahrer verungluckte beim San Marino GP 1994 in Imola toedlich und veraenderte die Formel 1 grundlegend in Bezug auf Sicherheitsstandards?",
        answerA = "Roland Ratzenberger",
        answerB = "Ayrton Senna",
        answerC = "Karl Wendlinger",
        answerD = "Michele Alboreto",
        correctAnswer = 1,
        explanation = "Ayrton Senna starb am 1. Mai 1994 beim San Marino Grand Prix in Imola nach einem Unfall in der Tamburello-Kurve. Sein Tod — zusammen mit dem toedlichen Unfall von Roland Ratzenberger am Vortag — loesteeine Revolution der Sicherheitsstandards in der Formel 1 aus.",
        difficulty = 4,
        funFact = "Nach Sennas Tod gruendete die FIA die Expert Advisory Safety Committee und fuehrte binnen zwei Jahren ueber 40 sicherheitsrelevante Regelaenderungen ein, u.a. das HANS-Geraet und verbesserte Leitplanken."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Fahrwerk-Konzept verwendete Lotus 1983 als erstes Team und ermoeglichte erstmals eine activ gesteuerte Fahrzeughoehe?",
        answerA = "Computer-gesteuertes aktives Fahrwerk (Active Suspension)",
        answerB = "Semi-aktives Daempfersystem",
        answerC = "Hydropneumatisches Niveau-Regulierungssystem",
        answerD = "Reaktives Gummifedersystem",
        correctAnswer = 0,
        explanation = "Lotus entwickelte 1983 das erste computer-gesteuerte aktive Fahrwerk der Formel 1, das die Federhaerte und Fahrzeughoehe in Echtzeit anpassen konnte. Das System debuetuerte 1987 erfolgreich und wurde ab 1994 aus Sicherheitsgruenden verboten.",
        difficulty = 4,
        funFact = "Williams perfektionierte das aktive Fahrwerk Ende der 1980er und Anfang der 1990er Jahre so sehr, dass ihre Autos auf holprigen Strecken kaum Bewegung zeigten — ein entscheidender Wettbewerbsvorteil."
    ),

    // --- RADSPORT (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Wie viele Etappen umfasst die Tour de France traditionell, und wie lang ist das Gesamtrennen ungefaehr?",
        answerA = "18 Etappen, ca. 3.200 km",
        answerB = "21 Etappen, ca. 3.500 km",
        answerC = "21 Etappen, ca. 2.800 km",
        answerD = "23 Etappen, ca. 4.000 km",
        correctAnswer = 1,
        explanation = "Die Tour de France umfasst traditionell 21 Fahretappen plus zwei Ruhetage ueber drei Wochen, mit einer Gesamtstrecke von etwa 3.200-3.600 km (typisch ca. 3.500 km). Die genaue Strecke variiert jedes Jahr.",
        difficulty = 4,
        funFact = "Die erste Tour de France 1903 hatte nur sechs Etappen und eine Gesamtlaenge von 2.428 km. Gewinner Maurice Garin absolvierte diese in 94 Stunden, 33 Minuten und 14 Sekunden."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet der Begriff 'Lanterne Rouge' bei der Tour de France und welche besondere Bedeutung hat dieser Status?",
        answerA = "Der letzte Fahrer im Gesamtklassement, der das Rennen beendet",
        answerB = "Der langsamste Fahrer einer Etappe",
        answerC = "Ein Fahrer der das Zeitlimit ueberschreitet",
        answerD = "Der Kapitaen der Mannschaft mit der schlechtesten Platzierung",
        correctAnswer = 0,
        explanation = "Die 'Rote Laterne' bezeichnet den letzten Fahrer im Gesamtklassement, der das Rennen vollstaendig absolviert. Trotz des symbolisch letzten Platzes wird dieser Status von manchen Fahrern als Ehre betrachtet, das haerteste Radrennen der Welt ueberhaupt beendet zu haben.",
        difficulty = 4,
        funFact = "Historisch wurde die Lanterne Rouge manchmal bewusst angestrebt, da der letzte Fahrer bei Kriterien nach der Tour haeufig hohe Startgelder erhielt — was zu strategischen Verzoegerungen fuehrte."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Radprofi gewann den Giro d'Italia 2018 und machte damit als erster britischer Fahrer das Rennen in seiner Karriere?",
        answerA = "Chris Froome",
        answerB = "Geraint Thomas",
        answerC = "Simon Yates",
        answerD = "Tom Dumoulin",
        correctAnswer = 0,
        explanation = "Chris Froome gewann den Giro d'Italia 2018 nach einem spektakulaeren Alleingang auf der 19. Etappe, bei dem er 80 km vor dem Ziel angriff. Er ist damit erst der dritter Fahrer nach Eddy Merckx und Bernard Hinault, der alle drei Grand Tours (Tour, Giro, Vuelta) gewann.",
        difficulty = 4,
        funFact = "Froomes Angriff auf der Finestra-Etappe 2018 gilt als einer der kuehnsten im modernen Radsport — er gewann die Etappe mit ueber 3 Minuten Vorsprung und holte das Gesamtklassement auf."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Bergankunft der Tour de France gilt als der mythischste Anstieg und traegt den Beinamen 'Gigant der Provence'?",
        answerA = "Col du Galibier",
        answerB = "Mont Ventoux",
        answerC = "Alpe d'Huez",
        answerD = "Col du Tourmalet",
        correctAnswer = 1,
        explanation = "Der Mont Ventoux (1912 m) wird wegen seiner Kahlheit, extremen Wetterbedingungen und der Laenge seiner Rampen (21 km ab Bedoin, 1610 Hm) als 'Gigant der Provence' oder 'Kahler Berg' bezeichnet. Er ist einer der brutallsten Anstiege der Tour de France.",
        difficulty = 4,
        funFact = "Tom Simpson starb 1967 am Mont Ventoux durch Erschoepfung und Amphetaminmissbrauch. Bis heute erinnert ein Denkmal nahe dem Gipfel an den britischen Champion."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Taktgefuehl-Rennen' Paris-Roubaix bekannt fuer, und warum wird es 'Hoelle des Nordens' genannt?",
        answerA = "Extreme Hoehenmeter durch die Ardennen",
        answerB = "Kopfsteinpflaster-Sektoren ('Pave') im Norden Frankreichs",
        answerC = "Ueberquerung von 10 Flussdurchquerungen",
        answerD = "Schaechte und Tunnel unter Paris",
        correctAnswer = 1,
        explanation = "Paris-Roubaix ist beruehmt fuer seine Kopfsteinpflaster-Sektoren ('Pave'), die bis zu fuenf Sterne Schwierigkeit erreichen. Die holprigen, manchmal matschigen Abschnitte durch nordfranzoeische Felder erfordern spezielle Reifen, gefederte Sattelstuetzen und ueberragende Balance.",
        difficulty = 4,
        funFact = "Das Velodrom von Roubaix, wo das Rennen endet, hat einen legendaeren Ruf: Der Sieger erhalt einen Backstein vom Velodrom als Trophae — eines der ungewoehnlichsten Siegerpraemien im Sport."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Fahrer gewann die Vuelta a Espana 2017, 2019 und 2020 und dominierte damit das Rennen in dieser Aera?",
        answerA = "Alberto Contador",
        answerB = "Chris Froome",
        answerC = "Primoz Roglic",
        answerD = "Richard Carapaz",
        correctAnswer = 2,
        explanation = "Primoz Roglic gewann die Vuelta a Espana 2019, 2020 und 2021 dreimal in Folge. 2017 gewann Chris Froome die Vuelta. Roglic ist damit einer der dominantesten Vuelta-Spezialisten der modernen Radsportaera.",
        difficulty = 4,
        funFact = "Roglic begann seine Sportkarriere als Skispringer und wechselte erst mit 23 Jahren zum Radsport — ein ungewoehnlicher Werdegang zum Profi-Radfahrer."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet man im Radsport als 'Domestique' und welche taktische Rolle erfuellt diese Position im Peloton?",
        answerA = "Fahrer der speziell fuer Bergankuenfte ausgebildet ist",
        answerB = "Mannschaftshelfer, der den Kapitaen unterstuetzt und eigene Chancen opfert",
        answerC = "Spezialist fuer Zeitfahrten",
        answerD = "Fahrer der ausschliesslich Ausreissergruppen kontrolliert",
        correctAnswer = 1,
        explanation = "Ein Domestique (franzoesisch: Diener) ist ein Fahrer, der seine Rennergebnisse dem Teamkapitaen opfert. Er haengt Flaschenstafeln, faehrt Windschatten, gibt sein Rad bei Defekten ab und kontrolliert gefaehrliche Angriffe der Gegner.",
        difficulty = 4,
        funFact = "Eddy Merckx hatte mit seinem Helfer Vic Van Schil einen der bekanntesten Domestiques der Geschichte — Van Schil verzichtete auf alle Eigeninitiative und war vollstaendig dem Dienst am Boss gewidmet."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr fuehrte die UCI das Biological Passport-Programm ein, das Doping durch Laengzeit-Blutprofilmonitoring erkennen soll?",
        answerA = "2004",
        answerB = "2008",
        answerC = "2010",
        answerD = "2006",
        correctAnswer = 1,
        explanation = "Die UCI (Union Cycliste Internationale) einfuehrte das Athlete Biological Passport (ABP) 2008 als erstes Sportverband weltweit. Es ueberwacht haematologische und endokrinologische Parameter laengerfristig, um indirekte Hinweise auf Doping zu erkennen.",
        difficulty = 4,
        funFact = "Das Biological Passport fuehrte zur nachtraeglichen Disqualifikation mehrerer Fahrer, darunter Alberto Contador (Clenbuterol, Tour 2010) und verschiedene UCI-ProTour-Fahrer mit auffaelligen Blutprofilen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Anstieg der Tour de France wurde seit 2011 nach einem ehemaligen Tour-Gewinner benannt und liegt in den Pyrenaaen?",
        answerA = "Col de la Croix de Fer — nach Bernard Hinault",
        answerB = "Port de Bales — nach Marco Pantani",
        answerC = "Col du Tourmalet — nach Eddy Merckx",
        answerD = "Col de Peyresourde — nach Laurent Fignon",
        correctAnswer = 1,
        explanation = "Der Port de Bales wurde 2011 inoffiziell dem 2004 verstorbenen Marco Pantani gewidmet. Offiziell benannte Anstiege nach Fahrern sind selten, aber Pantanis Erbe am Berg ist durch Gedenksteine und Fan-Inschriften lebendig.",
        difficulty = 4,
        funFact = "Marco Pantani, genannt 'Il Pirata', gilt als einer der begnadetsten Bergfahrer der Geschichte. Sein Sieg bei Tour und Giro 1998 im selben Jahr gehoert zu den grossen Einzelleistungen des Radsports."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der Unterschied zwischen einem 'Peloton' und einem 'Autobus' (auch 'Grupetto') im professionellen Rennradsport?",
        answerA = "Peloton = Hauptfeld, Autobus = Gruppe langsamerer Fahrer die gemeinsam das Zeitlimit einhalten",
        answerB = "Peloton = Ausreisser, Autobus = Hauptfeld",
        answerC = "Peloton = Bergspezialisten-Gruppe, Autobus = Sprinter-Gruppe",
        answerD = "Peloton = Gesamtklassement-Fahrer, Autobus = Helfer-Fahrer",
        correctAnswer = 0,
        explanation = "Das Peloton ist das Hauptfeld des Rennens. Der Autobus (Grupetto) ist eine Gruppe meist aus Sprintern und Helfern, die sich auf Bergetappen zusammenschliessen um gemeinsam das Zeitlimit zu unterbieten ohne Krafte zu verschwenden.",
        difficulty = 4,
        funFact = "Im Autobus wird genau kalkuliert: Die Gruppe muss langsam genug fahren um Energie zu sparen, aber schnell genug um nicht eliminiert zu werden. Mark Cavendish ist beruehmt als 'Autobus-Kapitaen' bei Bergankuenften."
    ),

    // --- BASKETBALL NBA/FIBA (5 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Wer haelt den Rekord fuer die meisten Punkte in einem einzigen NBA-Spiel (100 Punkte) und in welchem Jahr erzielte er diese Leistung?",
        answerA = "Michael Jordan, 1987",
        answerB = "Kobe Bryant, 2006",
        answerC = "Wilt Chamberlain, 1962",
        answerD = "Kareem Abdul-Jabbar, 1971",
        correctAnswer = 2,
        explanation = "Wilt Chamberlain erzielte am 2. Maerz 1962 gegen die New York Knicks exakt 100 Punkte fuer die Philadelphia Warriors — ein Rekord der bis heute unerreicht ist. Er traf 36 von 63 Feldwuerfen und 28 von 32 Freiwuerfen.",
        difficulty = 4,
        funFact = "Von Chamberlains 100-Punkte-Spiel existiert keine Videoaufnahme. Das einzige bekannte Erinnerungsstueck ist ein Foto, auf dem Chamberlain einen handgeschriebenen '100'-Zettel haelt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Taktik bezeichnete Phil Jackson bei den Chicago Bulls als 'Triangle Offense' und wer entwickelte dieses System urspruenglich?",
        answerA = "Entwickelt von Red Auerbach in den 1950ern",
        answerB = "Entwickelt von Sam Barry, popularisiert von Tex Winter",
        answerC = "Entwickelt von Chuck Daly fuer die Detroit Pistons",
        answerD = "Entwickelt von Pat Riley fuer die Showtime Lakers",
        correctAnswer = 1,
        explanation = "Die Triangle Offense wurde urspruenglich von Sam Barry entwickelt und von Tex Winter verfeinert. Phil Jackson nutzte dieses System mit Michael Jordan bei den Chicago Bulls (6 Meisterschaften) und spaeter mit Kobe Bryant bei den LA Lakers (5 Meisterschaften).",
        difficulty = 4,
        funFact = "Die Triangle Offense funktioniert ohne feste Spielzuege — stattdessen lesen die Spieler die Verteidigung und handeln entsprechend vorher eingeubter Prinzipien, was extreme Spielintelligenz erfordert."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches NBA-Team gewann 1996 mit 72 Siegen in der regulaeren Saison (damals Rekord) und wer war der Star dieser Mannschaft?",
        answerA = "LA Lakers mit Shaquille O'Neal",
        answerB = "Chicago Bulls mit Michael Jordan",
        answerC = "San Antonio Spurs mit David Robinson",
        answerD = "Utah Jazz mit Karl Malone",
        correctAnswer = 1,
        explanation = "Die Chicago Bulls setzten 1995-96 mit 72 Siegen und 10 Niederlagen einen Meilenstein der NBA-Geschichte. Michael Jordan kehrte nach seinem Baseball-Ausflug zurueck und gewann mit Dennis Rodman, Scottie Pippen und Phil Jackson die Meisterschaft.",
        difficulty = 4,
        funFact = "Der Rekord von 72 Siegen wurde 2015-16 von den Golden State Warriors mit 73 Siegen gebrochen — paradoxerweise verloren die Warriors dennoch die NBA Finals gegen LeBron Jamess Cleveland Cavaliers."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet im Basketball-Regelwerk der Begriff 'Flagrant Foul' und welche zwei Kategorien gibt es?",
        answerA = "Absichtliches Foul an einem werfenden Spieler — Flagrant 1 (unsportlich) und Flagrant 2 (Platzverweis)",
        answerB = "Unnoetig hartes und unkontrolliertes Foul — Flagrant 1 (Freiwuerfe + Ballbesitz) und Flagrant 2 (Platzverweis)",
        answerC = "Technisches Foul gegen Schiedsrichter — einmalig oder wiederholend",
        answerD = "Defensives Foul ohne Ballkontakt — Kategorie A und B",
        correctAnswer = 1,
        explanation = "Ein Flagrant Foul ist ein unnoetig hartes, unkontrolliertes oder gefaehrliches Foul. Flagrant 1 = Freiwuerfe fuer das Opfer plus Ballbesitz; Flagrant 2 = sofortiger Platzverweis des Foulers plus Freiwuerfe und Ballbesitz.",
        difficulty = 4,
        funFact = "Flagrant Fouls koennen auch nachtraeglich von der NBA-Disziplinarbehoerde verhaengt werden wenn Schiedsrichter ein Foul im Spiel falsch eingestuft haben — mit entsprechenden Geldstrafen oder Sperren."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Land gewann bei den Olympischen Spielen 2004 in Athen Gold im Herren-Basketball und schlug dabei das favorisierte USA-Team?",
        answerA = "Frankreich",
        answerB = "Argentinien",
        answerC = "Serbien",
        answerD = "Spanien",
        correctAnswer = 1,
        explanation = "Argentinien gewann 2004 in Athen olympisches Gold im Basketball und besiegt im Halbfinale das hochfavorisierte US-Team (89:81) mit NBA-Stars wie LeBron James, Carmelo Anthony und Dwyane Wade. Im Finale schlug Argentinien Italien 84:69.",
        difficulty = 4,
        funFact = "Manu Ginobili war der ueberragende Spieler des Turniers und wurde zum MVP gewahlt. Das argentinische Team galt als eines der bestorganisierten Mannschaftsteams der olympischen Geschichte."
    ),

    // --- HANDBALL-WM/EM (5 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welche Nation gewann die Handball-Weltmeisterschaft 2003 und war damit zweifacher Titelverteidiger nach 1999 und 2001?",
        answerA = "Frankreich",
        answerB = "Deutschland",
        answerC = "Spanien",
        answerD = "Kroatien",
        correctAnswer = 0,
        explanation = "Frankreich gewann die Handball-WM 2003 in Portugal und damit zum dritten Mal in Folge den WM-Titel (nach 1995, 2001 — tatsaechlich war es der zweite Titel, 2001 und 2003 in Folge). Frankreich ist eine der erfolgreichsten Handball-Nationen der Geschichte.",
        difficulty = 4,
        funFact = "Frankreich gewann die Handball-WM 2003 ungeschlagen und ohne Gegentore in der Golden-Goal-Regel. Nikola Karabatic war zu dieser Zeit bereits eines der groessten Talente des Welhandball."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie gross ist das offizielle Spielfeld im Handball (Laenge x Breite) und welchen Durchmesser hat der Torkreis?",
        answerA = "40 x 20 m, Torkreis-Radius 6 m",
        answerB = "40 x 20 m, Torkreis-Radius 7 m",
        answerC = "44 x 22 m, Torkreis-Radius 6 m",
        answerD = "38 x 18 m, Torkreis-Radius 6 m",
        correctAnswer = 0,
        explanation = "Das offizielle Handball-Spielfeld ist 40 m lang und 20 m breit. Der Torkreis (Wurfkreis) hat einen Radius von 6 Metern vom Mittelpunkt des Tores. Das Tor selbst ist 3 m breit und 2 m hoch.",
        difficulty = 4,
        funFact = "Der 9-Meter-Bereich (Freiwurflinie) liegt 3 Meter ausserhalb des Torkreises. Aus dieser Zone koennen Spieler direkt auf Tor werfen, ohne durch den Kreis zu muessen."
    ),

    Question(
        categoryId = 6,
        questionText = "Deutschland gewann die Handball-Europameisterschaft 2016 in Polen. Wer war der Torhueter, der im Finale entscheidende Paraden zeigte?",
        answerA = "Silvio Heinevetter",
        answerB = "Carsten Lichtlein",
        answerC = "Andreas Wolff",
        answerD = "Henning Fritz",
        correctAnswer = 2,
        explanation = "Andreas Wolff war der ueberragende Torhueter der deutschen Mannschaft bei der EM 2016 in Polen. Im Halbfinale gegen Spanien und im Finale gegen Spanien (nochmals) zeigte er spektakulaere Paraden und wurde zum MVP des Turniers gewahlt.",
        difficulty = 4,
        funFact = "Deutschland schlug im Finale Spanien 24:17 und gewann damit seinen ersten Europameister-Titel seit 2004. Bob Hanning als Vizepraesident des DHB hatte massgeblich am Aufbau dieser Generation mitgewirkt."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Handball-WM-Rekordnation hat die meisten Weltmeisterschaften gewonnen (Stand 2024)?",
        answerA = "Frankreich (6 Titel)",
        answerB = "Rumaenien (4 Titel)",
        answerC = "Deutschland (4 Titel)",
        answerD = "Schweden (3 Titel)",
        correctAnswer = 0,
        explanation = "Frankreich haelt mit sechs Weltmeistertiteln (1995, 2001, 2003, 2009, 2011, 2021) den Rekord. Nikola Karabatic ist dabei der erfolgreichste Spieler der WM-Geschichte mit vier Titeln als Feldspieler.",
        difficulty = 4,
        funFact = "Nikola Karabatic gilt als bester Handballer aller Zeiten — er gewann WM, EM, Olympia-Gold (2012 und 2021) sowie zahlreiche Champions-League-Titel mit Paris Saint-Germain und Barcelona."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet die Taktik 'Empty Goal' (leeres Tor) im Handball und wann wird sie typischerweise eingesetzt?",
        answerA = "Torwart wird durch siebten Feldspieler ersetzt um numerische Ueberlegenheit zu erzielen",
        answerB = "Angriff ohne Torhueter wenn Team im Rueckstand ist",
        answerC = "Torhueter verlaesst den Torkreis fuer einen Strafwurf des Gegners",
        answerD = "Spielsystem bei dem alle sieben Spieler angreifen",
        correctAnswer = 0,
        explanation = "Das 'Empty Goal' oder '7-gegen-6'-Spiel bedeutet, dass der Torhueter durch einen siebten Feldspieler ersetzt wird. Dies ergibt numerische Ueberlegenheit im Angriff, laesst aber das Tor unbehuetet — strategisch eingesetzt wenn der Angriff Treffer erzwingen muss.",
        difficulty = 4,
        funFact = "Die Einwechslung des siebten Feldspielers wurde lange als Risiko gesehen, hat sich aber besonders in Schlussphasen als effektive Taktik etabliert. Ein Ballverlust bedeutet allerdings ein garantiertes Gegentor ins leere Netz."
    ),

    // --- TISCHTENNIS UND BADMINTON (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welche Tischtennis-Spielerin dominierte die Weltrangliste von 2003 bis 2016 und gilt als die erfolgreichste Spielerin aller Zeiten?",
        answerA = "Wang Nan",
        answerB = "Zhang Yining",
        answerC = "Ding Ning",
        answerD = "Liu Shiwen",
        correctAnswer = 1,
        explanation = "Zhang Yining dominierte den Damen-Tischtennis von 2003 bis 2008 und gewann alle grossen Titel: Olympiagold 2004 und 2008, Weltmeisterin 2005 und 2009 (als amtierende Weltranglisten-Erste). Sie gilt als eine der besten Spielerinnen aller Zeiten.",
        difficulty = 4,
        funFact = "China dominiert den Tischtennis-Sport so stark, dass bei Weltmeisterschaften und Olympischen Spielen fast alle Medaillen an chinesische Spieler gehen. Die groessten Rivalen sind oft andere chinesische Nationalmannschaftsmitglieder."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der Unterschied zwischen einem 'Penhold'-Griff und einem 'Shakehand'-Griff im Tischtennis?",
        answerA = "Penhold: Schlager wie ein Stift gehalten mit Daumen und Zeigefinger; Shakehand: Universalgriff wie beim Haendeschuetteln",
        answerB = "Penhold: Beide Seiten des Schlagers nutzbar; Shakehand: Nur eine Seite nutzbar",
        answerC = "Penhold: Europaeischer Griff; Shakehand: Asiatischer Griff",
        answerD = "Penhold: Fuer Rueckhand optimiert; Shakehand: Fuer Vorhand optimiert",
        correctAnswer = 0,
        explanation = "Beim Penhold-Griff wird der Schlager wie ein Stift zwischen Daumen und Zeigefinger gehalten. Die anderen drei Finger unterstuetzen die Rueckseite. Beim Shakehand-Griff (heute dominierend) wird der Griffende wie beim Haendeschuetteln gefasst — er erlaubt starke Vor- und Rueckhand.",
        difficulty = 4,
        funFact = "Der Penhold-Griff war lange in Asien (besonders China und Korea) dominierend und erlaubt extrem schnelle Vorhand-Angriffe, hat aber Schwaechen bei der Rueckhand. Ma Lin gewann 2008 Olympiagold mit dem Penhold-Griff."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Deutsche gilt als einer der groessten Tischtennisspieler Europas und gewann WM-Titel sowohl im Einzel als auch im Team?",
        answerA = "Werner Schlager",
        answerB = "Jan-Ove Waldner",
        answerC = "Joerg Rosskopf",
        answerD = "Timo Boll",
        correctAnswer = 3,
        explanation = "Timo Boll ist Deutschlands erfolgreichster Tischtennisspieler und war mehrfach Europameister sowie Weltranglistenerster. Er gewann EM-Titel, Champions-League-Titel mit Borussia Duesseldorf und zahlreiche Grand-Slam-Ereignisse der ITTF.",
        difficulty = 4,
        funFact = "Timo Boll spielte in Einzelpartien mehrfach gegen chinesische Weltelite auf Augenhöhe. Sein enger Freund und Rivale Ma Long bezeichnete Boll als einen der gefaehrlichsten Nicht-Chinesen in der Geschichte des Sports."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet man im Tischtennis als 'ITTF-Butterfly' und welche Bedeutung hat dieser Begriff?",
        answerA = "Ein spezieller Schlagtypus mit Seitwaearts-Rotation",
        answerB = "Der groesste Ausruesterhersteller im Tischtennis (Tamasu/Butterfly)",
        answerC = "Die Bezeichnung fuer den Weltverband ITTF",
        answerD = "Eine Regelaenderung zum Tischtennisball von 2000",
        correctAnswer = 1,
        explanation = "Butterfly (offiziell: Tamasu Butterfly Co., Ltd.) ist der bekannteste und prestigetraechtigste Ausruester im Tischtennis-Profisport. Die japanische Firma stellt Schlager, Belaege und Baelle her und sponsert Topspieler weltweit.",
        difficulty = 4,
        funFact = "Butterfly-Belaege wie Tenergy 05 gelten als die begehrtesten und teuersten Profi-Tischtennis-Belaege der Welt. Ein professioneller Tischtennis-Schlager mit Butterfly-Belaegen kann ueber 200 Euro kosten."
    ),

    Question(
        categoryId = 6,
        questionText = "In welchem Jahr wurde der Tischtennis-Ball von 38mm auf 40mm Durchmesser vergroessert und welchen Grund hatte diese Regelaenderung?",
        answerA = "2000, um das Spiel fuer Zuschauer und TV langsamer und verfolgbarer zu machen",
        answerB = "2004, um Verletzungsrisiken zu reduzieren",
        answerC = "1998, auf Druck asiatischer Verbaende",
        answerD = "2000, um Belaege mit extremem Spin zu benachteiligen",
        correctAnswer = 0,
        explanation = "Der groessere 40mm-Ball wurde 2000 eingefuehrt um das Spiel fuer TV-Zuschauer verfolgbarer zu machen und die Spielgeschwindigkeit leicht zu reduzieren. 2015 folgte eine weitere Aenderung auf 40mm Plastikball (statt Zelluloid), was Spieler erneut anpassen mussten.",
        difficulty = 4,
        funFact = "Die Umstellung auf Plastikball 2015 war fuer viele Topspieler eine grosse Herausforderung, da der Plastikball weniger Spin annimmt und anders springt als der Zelluloid-Vorgaenger — besonders bei Aufschlagvariationen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Badminton-Spieler gilt als GOAT (Greatest of All Time) mit 5 Olympia-Goldmedaillen und dominierende Leistungen ueber 15 Jahre?",
        answerA = "Peter Gade",
        answerB = "Lin Dan",
        answerC = "Lee Chong Wei",
        answerD = "Viktor Axelsen",
        correctAnswer = 1,
        explanation = "Lin Dan (China) gilt als bester Badminton-Spieler aller Zeiten mit Olympiagold 2008 und 2012, 5 WM-Titeln (2006, 2007, 2009, 2011, 2013) sowie zahlreichen All England-Titeln. Er dominierte das Spiel ueber 15 Jahre.",
        difficulty = 4,
        funFact = "Lin Dan gewann als erster Spieler der Geschichte alle neun grossen Badminton-Titel in seiner Karriere (Olimpia, WM, All England, Asian Games, Thomas Cup etc.) — ein als 'Super Dan' bezeichneter Meilenstein."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Thomas Cup' im Badminton und wann wird er ausgetragen?",
        answerA = "Das Herren-Mannschafts-Weltturnier, alle zwei Jahre",
        answerB = "Das Damen-Einzel-Weltturnier, jährlich",
        answerC = "Das Mixed-Doubles-Weltturnier, alle vier Jahre",
        answerD = "Der groesste Herren-Einzel-Grand-Prix der Saison",
        correctAnswer = 0,
        explanation = "Der Thomas Cup ist das Weltturnier im Herren-Mannschaftsbadminton und wird alle zwei Jahre ausgetragen. Er ist nach Sir George Thomas benannt, einem ehemaligen England-Champion. Indonesien haelt mit 14 Titeln den Rekord.",
        difficulty = 4,
        funFact = "Das Pendant fuer Damen heisst Uber Cup, benannt nach Betty Uber. Beide Turniere werden oft gleichzeitig ausgetragen und gelten als Olympia des Mannschafts-Badminton."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie schnell kann ein Badminton-Federball beim professionellen Smash maximal fliegen und was macht ihn zum schnellsten Projectil im Sport?",
        answerA = "Ueber 300 km/h, wegen der aerodynamischen Befiederung",
        answerB = "Ueber 400 km/h, da er kaum Gewicht hat und kein Luftwiderstand ihn bremst",
        answerC = "Ueber 490 km/h, Rekord von Mads Pieler Kolding",
        answerD = "Etwa 250 km/h, die Feder bremst bei hoeherem Speed",
        correctAnswer = 2,
        explanation = "Der schnellste Badminton-Smash wurde von Mads Pieler Kolding (Daenemark) mit 493 km/h gemessen (2017, als inoffizieller Rekord). Offiziell anerkannte Rekorde liegen bei ca. 370-420 km/h — Badminton-Federbaelle sind die schnellsten Sportprojektile.",
        difficulty = 4,
        funFact = "Der Federball verlangsamt sich durch die Naturfedern sehr schnell — von 400 km/h beim Smash auf etwa 20-30 km/h beim Aufkommen auf dem Boden in nur wenigen Metern. Diese extreme Verzoegerung ist einzigartig im Sport."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Federball-Material verwenden Profis bei internationalen Turnieren und warum wird es bevorzugt?",
        answerA = "Synthetische Nylonfedern, da bestaendiger und wetterresistenter",
        answerB = "Natuerliche Gaensefedern (Linksseitige Schwungfedern), da ueberlegene Flugeigenschaften",
        answerC = "Carbon-Faserfedern fuer maximale Stabilitaet",
        answerD = "Naturkork mit Kunststofffedern als Kompromiss",
        correctAnswer = 1,
        explanation = "Profi-Turniere verwenden Federbaelle mit natuerlichen Gaensefedern — spezifisch die linken Schwungfedern (ca. 16 Stueck pro Ball), die gleichmaessigere Rotation erzeugen. Die aerodynamischen Eigenschaften natuerlicher Federn sind Kunststoff ueberlegen.",
        difficulty = 4,
        funFact = "Ein Profi-Badminton-Match kann Dutzende Federbaelle verbrauchen, da natuerliche Federn leicht brechen oder sich verbiegen. Bei grossen Turnieren werden speziell angezuechte Gaense fuer premium Federbaelle verwendet."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet der Begriff 'Net Kill' im Badminton und wann ist er besonders effektiv?",
        answerA = "Harter Schlag aus dem Hinterfeld der den Gegner am Netz ueberrascht",
        answerB = "Aggressives Abtoeten eines hohen Netzballs durch steilen Smash nach unten vom Netz aus",
        answerC = "Tropfschlag direkt hinter das Netz",
        answerD = "Defensivposition am Netz gegen feindliche Smashes",
        correctAnswer = 1,
        explanation = "Der Net Kill ist ein aggressiver Schlag direkt am Netz, bei dem ein hoch gesetzter Ball steil und flaechendeckend nach unten geschlagen wird. Er ist besonders effektiv wenn der Gegner einen 'Lift' zu flach ans Netz schlaegt — der Angreifer kann dann punkten ohne dass der Gegner reagieren kann.",
        difficulty = 4,
        funFact = "Im professionellen Badminton ist das Netspiel eine eigene Disziplin: Spieler wie Kevin Sanjaya Sukamuljo (Indonesien) sind beruehmt fuer reflexartige Net Kills, die oft schneller als das Auge folgen kann."
    )

)
