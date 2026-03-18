package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsHard3(): List<Question> = listOf(

    // ── Wassersport / Segeln – technische Tiefe (8) ───────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'Foiling' im modernen Hochleistungssegeln?",
        answerA = "Ein Rumpfdesign mit breitem Flachboden fuer maximale Stabilitaet im Wellengang",
        answerB = "Das Abheben des Rumpfes aus dem Wasser auf hydrostatischen Tragflaecheln, die durch Fahrt Auftrieb erzeugen",
        answerC = "Eine spezielle Segelstellung bei achterlichem Wind zur Geschwindigkeitssteigerung",
        answerD = "Die Technik, durch gezielte Kielgewichtsverlagerung Kraefte auszubalancieren",
        correctAnswer = 1,
        explanation = "Beim Foiling hebt der gesamte Rumpf bei ausreichender Fahrt aus dem Wasser – aehnlich wie ein Wasserflugzeug. Der Wasserwiderstand sinkt drastisch, wodurch Boote auf Hydrofoils bis zu drei Mal schneller als der Wind segeln koennen. AC75-Jachten beim Americas Cup nutzen dieses Prinzip.",
        difficulty = 3,
        funFact = "Beim Americas Cup 2021 in Auckland erreichten die Foiling-AC75-Yachten regelmaessig ueber 50 Knoten (ca. 93 km/h) – schneller als die meisten Motorboote."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist ein 'Gyibe' (auch 'Gibe') im Segelsport und worin liegt das technische Risiko?",
        answerA = "Das Wenden des Bootes durch den Wind an der Vordseite – risikolos bei allen Windstaerken",
        answerB = "Das Wenden durch den Wind an der Hinterseite, wobei der Grossbaum unkontrolliert von einer Seite auf die andere schlaegt",
        answerC = "Ein kontrolliertes Manoeuvre nur bei schwachem Wind, bei dem das Segel abgebaut wird",
        answerD = "Das Belegen einer Leine am Klampen zur Kurskorrektur",
        correctAnswer = 1,
        explanation = "Beim Gyibe kreuzt das Heck des Bootes die Windlinie. Der Grossbaum wechselt dabei blitzartig die Seite und kann bei unkontrolliertem Ablauf schwere Schaeden am Rigg oder Verletzungen verursachen. Profis kontrollieren den Gyibe durch gezielte Schot-Bedienung.",
        difficulty = 3,
        funFact = "Ein unkontrollierter Gyibe im Sturm kann den Mast brechen. Bei der Vendee Globe 2020/21 beendeten mehrere solcher Manoeuverfehler die Regatten von Teilnehmern vorzeitig."
    ),

    Question(
        categoryId = 6,
        questionText = "Was misst die 'True Wind Speed' (TWS) im Unterschied zur 'Apparent Wind Speed' (AWS) beim Segeln?",
        answerA = "TWS ist der tatsaechlich am Standort herrschende Wind; AWS ist der vom Boot aus gemessene Wind, der durch die Eigengeschwindigkeit des Bootes beeinflusst wird",
        answerB = "TWS beschreibt den Wind in Bodennaehe, AWS den Wind auf Masttophoehe",
        answerC = "TWS ist die Windgeschwindigkeit bei Windstille, AWS die maximale Boeengeschwindigkeit",
        answerD = "Beide Werte sind identisch – sie unterscheiden sich nur in der Messeinheit",
        correctAnswer = 0,
        explanation = "Ein fahrendes Boot 'erzeugt' durch seine Eigengeschwindigkeit einen zusaetzlichen scheinbaren Gegenwind. Der scheinbare Wind (AWS) ist eine Vektoraddition aus wahrem Wind und Fahrtwind. Je schneller das Boot, desto staerker weicht AWS von TWS ab – entscheidend fuer optimales Segelsetzen.",
        difficulty = 3,
        funFact = "Bei Foiling-Jachten, die schneller als der Wind segeln, kommt der scheinbare Wind von vorne, obwohl der wahre Wind von achtern kommt – ein physikalisch faszinierendes Paradoxon."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man im Kanusport unter 'Eskimorolle' (auch 'Eskimo-Roll')?",
        answerA = "Eine Sicherheitstechnik, bei der der Paddler nach einem Kentern mithilfe des Paddels und Huefschwung das Boot ohne Ausstieg wieder aufrichtet",
        answerB = "Eine Angriffsstrategie im Wildwasser-Slalom um Torstangen zu umfahren",
        answerC = "Die Drehbewegung des Kajaks um die Laengsachse bei Winkelstroemen",
        answerD = "Ein Aufwaermmanoeuvre vor Wildwasserfahrten in Eisenwaessern",
        correctAnswer = 0,
        explanation = "Die Eskimorolle ist eine der wichtigsten Sicherheitstechniken im Kajaksport. Der Paddler nutzt eine praezise Kombination aus Huefschwung (Bootsbewegung) und Paddeleinsatz, um sich aus der gekenterten Position wieder aufzurichten, ohne das Boot verlassen zu muessen.",
        difficulty = 3,
        funFact = "Inuit-Voelker entwickelten die Technik vor tausenden Jahren als Ueberlebenstechnik im eisigen arktischen Meer – ein Ausstieg bei -2 Grad Wassertemperatur war lebensbedrohlich."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Windstaerkeskala wird international im Segelsport zur Einschaetzung der Wetterbedingungen genutzt?",
        answerA = "Saffir-Simpson-Skala (0–5)",
        answerB = "Fujita-Skala (F0–F5)",
        answerC = "Beaufort-Skala (0–12)",
        answerD = "Richter-Skala (1–10)",
        correctAnswer = 2,
        explanation = "Die Beaufort-Skala wurde 1805 von Admiral Francis Beaufort entwickelt und beschreibt Windstaerken von 0 (Windstille) bis 12 (Orkan, ueber 118 km/h). Urspruenglich anhand von Segelmanoeuvern skaliert, ist sie heute die weltweit anerkannte Referenz im maritimen Bereich.",
        difficulty = 3,
        funFact = "Beaufort skalierte urspruenglich die Windstaerke danach, wie viel Segeltuch eine Fregatte der Royal Navy noch sicher setzen konnte – ein rein prakischer Ansatz ohne Messgeraete."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Polarpolardiagramm' (Polar Diagram / Polar Curve) im Segelsport?",
        answerA = "Eine Karte der Windverhaeltnisse auf polaren Segelrouten fuer hochseegaengige Jachten",
        answerB = "Eine grafische Darstellung der optimalen Bootsgeschwindigkeit in Abhaengigkeit vom Windeinfallswinkel und der Windstaerke",
        answerC = "Ein Navigationsinstrument zur Bestimmung des magnetischen Nordpols auf dem offenen Ozean",
        answerD = "Eine Methode zur Berechnung des optimalen Anlaufs in Haefen mit starker Gezeitenstroemung",
        correctAnswer = 1,
        explanation = "Das Polardiagramm zeigt fuer jede Windrichtung (Winkel zum Kurs) und Windstaerke die theoretisch erreichbare Hoechstgeschwindigkeit des Bootes. Taktiker lesen daraus die optimalen Am-Wind- und Raumschotwinkel ab, um VMG (Velocity Made Good) zu maximieren.",
        difficulty = 3,
        funFact = "In der Vendee Globe nutzen Segler digitale Polardiagramme und lassen Bordecomputer staendig berechnen, ob der aktuelle Kurs dem Optimum entspricht – Abweichungen von einem Grad koennen tagesueber Meilen kosten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Disziplin wird als 'Langstreckentriathlon des Rudersports' bezeichnet und welche Strecke umfasst sie?",
        answerA = "Die Oxford-Cambridge Boat Race – ca. 6,8 km auf der Themse in London",
        answerB = "Die Great Race – 50 km auf dem Hudson River in New York",
        answerC = "Die Head of the River Race – 6,8 km, aber als Einzelzeitfahren fuer bis zu 420 Mannschaften",
        answerD = "Die Royal Henley Regatta – 2,1 km auf dem Thames-Kanal in Henley",
        correctAnswer = 2,
        explanation = "Das Head of the River Race in London ist das groesste Ruder-Rennen der Welt mit bis zu 420 startenden Achtern. Alle Mannschaften fahren die 6,8 km auf der Themse als Einzelzeitfahren – ein logistisches und sportliches Mammutprojekt.",
        difficulty = 3,
        funFact = "Das Head of the River Race wurde erstmals 1926 gestartet – seither hat es nur wenige Unterbrechungen gegeben (Weltkrieg, Hochwasser). Es ist ein geliebtes Spektakel fuer Zehntausende Londoner Zuschauer am Themseufer."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'Planing' im Motorboot- und Segelsport im Unterschied zum 'Verdraengungsfahren'?",
        answerA = "Das Fahren mit Elektromotor ohne Geraeuschentwicklung – umweltfreundliche Alternative zum Verbrennungsmotor",
        answerB = "Das Gleiten des Rumpfes auf der Wasseroberflaeche, wenn die Fahrtgeschwindigkeit die Rumpfgeschwindigkeit uebersteigt und hydrodynamischer Auftrieb entsteht",
        answerC = "Das Steuern des Boots ausschliesslich durch Gewichtsverlagerung der Crew ohne Ruder",
        answerD = "Ein Manoeuvrier-Modus bei sehr niedrigen Geschwindigkeiten im Hafen",
        correctAnswer = 1,
        explanation = "Beim Verdraengungsfahren drueckt der Rumpf Wasser seitlich weg – die Maximalgeschwindigkeit ist physikalisch limitiert (Rumpfgeschwindigkeit = 1,34 x Wurzel der Wasserlinienlaenge in Fuß). Beim Planing gleitet der Rumpf auf dem Wasser und kann diese Grenze deutlich ueberschreiten.",
        difficulty = 3,
        funFact = "Normale Segelboote (Einrumpfer) koennen wegen ihrer Rumpfform nicht 'planen'. Katamarane und flache Skiffs koennen es – und sind deshalb bei gleichem Wind oft doppelt so schnell."
    ),

    // ── Klettern / Bergsteigen (7) ────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was versteht man im Sportklettern unter dem Begriff 'Rotpunkt-Begehung'?",
        answerA = "Eine Route, die ausschliesslich in der Daemmerung oder Nacht geklettert wird, ueblicherweise unter Stirnlampen",
        answerB = "Das erstmalige freie Durchsteigen einer Route von unten nach oben ohne Sturz oder Ausruhen am Seil, nach vorheriger Erkundung der Zuege",
        answerC = "Das Vorsteigen einer neuen Route ohne jegliche vorherige Sicherungshaken – nur mit eigenem Sicherungsmaterial",
        answerD = "Das Klettern einer Route in der Farbe Rot markierter Hakenlinien in Alpine-Klettergebieten",
        correctAnswer = 1,
        explanation = "Der Begriff 'Rotpunkt' (englisch 'redpoint') wurde vom deutschen Kletterpionier Kurt Albert gepraegt, der in den 1970ern erfolgreich gekletterte Routen mit einem roten Punkt markierte. Heute bezeichnet er das freie, sturzfreie Durchsteigen einer Route, die vorher beuebt und studiert wurde.",
        difficulty = 3,
        funFact = "Kurt Albert malte buchstaeblich rote Punkte an den Fuss jeder Route, die er frei geklettert hatte – daher der Name. Seine Methode revolutionierte die Kletterethik und wird heute weltweit angewendet."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bedeutet der Begriff 'Freies Klettern' im alpinen Kontext – und was ist der entscheidende Unterschied zum 'Freien Solo'?",
        answerA = "Freies Klettern = Klettern ohne jegliche Sicherungsmittel; Freies Solo = Klettern mit Seil, aber alleine ohne Partner",
        answerB = "Freies Klettern = Klettern mit Seil und Haken, aber die Sicherungsmittel tragen das Koerpergewicht im Sturz, nicht beim Aufstieg; Freies Solo = dasselbe, aber ohne Seil",
        answerC = "Freies Klettern = Klettern an kuenstlichen Griffen in der Kletterhalle; Freies Solo = Klettern an Naturfels ohne Partner",
        answerD = "Freies Klettern = Ausstieg aus jeder Baumkrone; Freies Solo = Alpinklettern alleine ohne Fuehrer",
        correctAnswer = 1,
        explanation = "Beim freien Klettern nutzt der Kletterer Seil und Haken nur zur Sicherung im Sturzfall – der Aufstieg selbst erfolgt ausschliesslich mit den eigenen Haenden und Fuessen. Beim Freien Solo ist kein Seil vorhanden: Ein Sturz ist toedlich. Alex Honnold machte das Free Solo am El Capitan 2017 weltberuehmt.",
        difficulty = 3,
        funFact = "Alex Honnolds Free Solo am El Capitan (900 m, Yosemite) 2017 gilt als groesste klettersportliche Leistung aller Zeiten. Die Route 'Freerider' hat Schluesselstellen im Schwierigkeitsgrad 7c – ohne Seil."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Sicherungssystem wird im Sportklettern als 'Toprope' bezeichnet?",
        answerA = "Das Seil laeuft vom Kletterer durch Zwischensicherungen zur Belay-Station – der Sturz fuehrt zum Seilsturzpendel nach unten",
        answerB = "Das Seil laeuft vom Kletterer direkt nach oben zu einem fixen Umlenkpunkt an der Routenspitze und dann zum Sicherungspartner – ein Sturz verursacht keinen weiten Fall",
        answerC = "Ein selbstsicherndes System mit motorbetriebenem Gegengewicht ohne Sicherungspartner",
        answerD = "Das Klettern ohne Sicherung, aber mit Sturzmatte (Crashpad) am Boden",
        correctAnswer = 1,
        explanation = "Beim Toprope befindet sich der Umlenkpunkt bereits oben an der Route. Der Sicherungspartner steht am Boden und nimmt das Seil auf, waehrend der Kletterer aufsteigt. Ein Sturz fuehrt nur zu einem minimalen Fall – ideal fuer Anfaenger oder zum Erarbeiten schwieriger Bewegungsabfolgen.",
        difficulty = 3,
        funFact = "Toprope-Klettern ist die sicherste Form des Seilkletterns. In Kletterhallen weltweit lernen Einsteiger fast ausschliesslich mit Toprope-Setups, bevor sie zum Vorstieg wechseln."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man im Bergsteigen unter 'Death Zone' beim Himalaya-Klettern?",
        answerA = "Den Bereich oberhalb von 6.000 m, in dem Lawinen besonders haeufig abgehen",
        answerB = "Die Zone oberhalb von etwa 8.000 m Hoehe, in der der Sauerstoffpartialdruck so niedrig ist, dass der menschliche Koerper dauerhaft abstirbt, anstatt sich zu akklimatisieren",
        answerC = "Den Abschnitt des Khumbu-Eisbruchs am Everest, der besonders viele Gletscherspalten aufweist",
        answerD = "Die Zone zwischen 5.500 m und 7.000 m, in der Hoehenoedem am haeufigsten auftritt",
        correctAnswer = 1,
        explanation = "Oberhalb von ~8.000 m (je nach individuellem Koerper +/- 500 m) kann der menschliche Organismus nicht mehr genug Sauerstoff aufnehmen, um sich zu regenerieren. Muskelgewebe und Organe werden abgebaut. Laengere Aufenthalte fuehren unweigerlich zum Tod – Bergsteiger muessen schnell auf- und absteigen.",
        difficulty = 3,
        funFact = "Reinhold Messner und Peter Habeler bestiegeen den Everest 1978 erstmals ohne Flaschensauerstoff und bewiesen damit, dass Menschen die Death Zone ueberleben koennen – bis dahin hielten das die meisten Wissenschaftler fuer physiologisch unmoeglich."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Achttausender' und wie viele Berge mit dieser Bezeichnung gibt es?",
        answerA = "Berge ueber 7.000 m – es gibt 24 davon weltweit",
        answerB = "Berge mit einer Hoehe von mindestens 8.000 m ueber dem Meeresspiegel – es gibt genau 14 davon, alle im Himalaya oder Karakorum",
        answerC = "Alle Berge ueber 8.000 m – es gibt 18 davon, hauptsaechlich in Nepal und Tibet",
        answerD = "Berge ueber 8.500 m – es gibt 3 davon: Everest, K2 und Kangchendzonga",
        correctAnswer = 1,
        explanation = "Genau 14 Berge uebersteigen die 8.000-Meter-Marke, alle in der Himalaya-Karakorum-Region. Der hoechste ist der Everest (8.848,86 m), der schwierigste gilt als K2 (8.611 m). Reinhold Messner war 1986 der erste Mensch, der alle 14 Achttausender bestiegen hatte.",
        difficulty = 3,
        funFact = "Der K2 wird oft als 'wilder Berg' bezeichnet, weil er unberechenbarer und gefaehrlicher ist als der Everest. Die Todesrate am K2 ist pro Gipfelversuch mehr als viermal hoeher als am Everest."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der klettertechnische Begriff 'Manteln' (Mantle Move)?",
        answerA = "Das Abseilen mit einer Reepschnur ueber einen Felsvorsprung",
        answerB = "Eine Klettertechnik, bei der man mit den Haenden auf einen Griff drueckt und sich gleichzeitig mit dem Koerper darueber schiebt, aehnlich wie man aus einem Swimmingpool steigt",
        answerC = "Das Anlegen einer Seilschlinge um einen Felsblock als Sicherungspunkt",
        answerD = "Das Einsteigen in eine Felsspalte mit eingeklemmten Fersen als Sicherungstechnik",
        correctAnswer = 1,
        explanation = "Der Mantle ist eine fundamentale Klettertechnik: Statt einen Griff zu umfassen, wird die Handflaehe aufgelegt und der Koerper durch Streckung der Arme nach oben geschoben, bis man auf dem Griff oder Vorsprung steht. Erfordert viel Schulter- und Trizepskraft.",
        difficulty = 3,
        funFact = "Das Wort 'Mantle' kommt vom englischen Begriff fuer Kaminsims – die Bewegung ahmt das Hochklettern ueber einen Kaminsims nach, was fruehe Kletterer oft buchstaeblich tun mussten."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Bouldern' im Klettersport und welche Sicherungsmittel werden dabei typischerweise verwendet?",
        answerA = "Klettern an kuenstlichen Felsblockimiten in Sporthallen ohne jegliche Sicherung – Stuerze landen auf Hartmatten",
        answerB = "Klettern an niedrigen Felsformationen (max. 5–6 m) ohne Seil, mit Sturzmatten (Crashpads) als einzige Sicherung",
        answerC = "Klettern in grossen Gruppen an breiten Felswaenden mit gemeinsamem Toprope-System",
        answerD = "Hochalpines Klettern an Granitvorstufen (Boulder = Granit) mit speziellen Steigeisenstahlplatten",
        correctAnswer = 1,
        explanation = "Bouldern bedeutet Klettern an kleinen Felsblöcken oder tief gelegenen Routen ohne Seil. Die einzige Sicherung sind Crashpads am Boden und ein Spotter (Beobachter), der den Kletternden im Sturzfall lenkt. Technische Schwierigkeit und Kraft sind entscheidend, nicht Ausdauer.",
        difficulty = 3,
        funFact = "Der Begriff 'Boulder' stammt aus dem englischen und bezeichnet grosse Felsbloecke. Das Camp 4 im Yosemite-Valley gilt als Wiege des modernen Boulderns – dort feilten Kletterer in den 1960ern ihren Stil an kleinen Felsen."
    ),

    // ── Leichtathletik-Technik (7) ────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was ist die 'Fosbury-Flop'-Technik im Hochsprung und wer erfand sie?",
        answerA = "Eine Technik, bei der der Athlet seitlich ueber die Latte rollt – von Dick Fosbury 1968 entwickelt und revolutionaer",
        answerB = "Eine Technik, bei der der Athlet rueckwaerts mit dem Kopf zuerst und gewoelbtem Ruecken die Latte ueberquert – eingefuehrt von Dick Fosbury bei den Olympischen Spielen 1968 in Mexiko",
        answerC = "Ein veraltetes Bauchflop-Verfahren aus den 1950er Jahren, bei dem Athleten mit dem Bauch nach unten springen",
        answerD = "Die Straddle-Technik, bei der der Athlet mit dem Koerper parallel zur Latte und Bauch nach unten ueberquert",
        correctAnswer = 1,
        explanation = "Dick Fosbury gewann 1968 in Mexiko-City Olympiagold mit seiner revolutionaeren Rueckwarts-Technik. Statt des bis dahin ueblichen Straddles (Bauch nach unten) ueberflog er die Latte mit dem Ruecken nach unten und dem Bogen des Koerpers nach oben – physikalisch optimal, weil der Koerperschwerpunkt unter der Latte bleibt.",
        difficulty = 3,
        funFact = "Fosbury gewann Gold mit einer Hoehe von 2,24 m. Trainer und Experten lachten zunaechst ueber seine Technik – 10 Jahre spaeter verwendeten praktisch alle Topspringer weltweit ausschliesslich den Fosbury-Flop."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche biomechanische Funktion hat der Wurfarm beim Speerwurf unmittelbar vor dem Abwurf?",
        answerA = "Der Arm wird gestreckt und senkrecht nach oben gefuehrt, um maximale Wurfhoehe zu erzielen",
        answerB = "Durch einen Peitscheneffekt wird zuerst die Hueftrotation erzeugt, dann der Schulterguerteldrehung folgend der Arm als letztes schnell nach vorne 'gepeitscht' – Energietransfer aus dem gesamten Koerper in den Wurfarm",
        answerC = "Der Arm ist waehrend der gesamten Bewegung gestreckt und horizontal – Kreisbewegung wie beim Diskuswurf",
        answerD = "Das Handgelenk bleibt neutral; der Speer wird ausschliesslich durch Beinstreckung beschleunigt",
        correctAnswer = 1,
        explanation = "Der Speerwurf nutzt die kinetische Kette: Bodendruck → Beinstreckung → Huefte → Schulter → Ellenbogen → Handgelenk. Der Arm folgt zuletzt und wirkt wie eine Peitsche. Handgelenkschnalzen am Ende verleiht dem Speer zusaetzlich Spin fuer Flugstabilitaet.",
        difficulty = 3,
        funFact = "1986 wurde das Speerwurf-Modell veraendert, nachdem Uwe Hohn 1984 mit 104,80 m einen Weltrekord aufstellte – der Speer landete gefaehrlich nah am Gegenfeld. Der neue Speer mit veraendertem Schwerpunkt reduzierte die Weiten um rund 10 Meter."
    ),

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet der Begriff 'Laktatschwelle' im Ausdauersport und welche praktische Bedeutung hat sie?",
        answerA = "Die maximale Milchsaeure-Konzentration im Blut, bei der ein Athlet nach einem Sprintversuch kollabiert",
        answerB = "Die Belastungsintensitaet, ab der die Laktatproduktion die Laktatabbaurate uebersteigt und Laktat im Blut ansteigt – oberhalb dieser Schwelle ist Dauerleistung stark limitiert",
        answerC = "Die Schwellentemperatur, ab der Muskeln aufgrund von Milchsaeureablagerungen permanent beschaedigt werden",
        answerD = "Ein theoretischer Wert ohne praktische Bedeutung fuer das Training – relevant nur fuer Labormessungen",
        correctAnswer = 1,
        explanation = "An der aeroben/anaeroben Schwelle (oft bei 4 mmol/l Blutlaktat) halten sich Produktion und Abbau die Waage. Training knapp unterhalb dieser Schwelle (Tempolauf, Schwellenlauf) verbessert die aerobe Kapazitaet gezielt. Oberhalb akkumuliert Laktat – der Athlet 'versauert' und muss das Tempo reduzieren.",
        difficulty = 3,
        funFact = "Profiradsportler wie Chris Froome trainierten jahrelang mit Laktatmessungen und Leistungsmessern, um praezise an ihrer Schwelle zu fahren. Die Laktatkurve ist so individuell wie ein Fingerabdruck."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie funktioniert die Technik des 'O'Brien-Stils' (Glide-Technik) im Kugelstossen?",
        answerA = "Der Athlet dreht sich mehrfach (3-4 Umdrehungen) wie beim Diskuswurf im Kreis, bevor er die Kugel abstosst",
        answerB = "Der Athlet startet mit dem Ruecken zur Stossrichtung, gleitet rueckwaerts durch den Kreis und dreht sich erst unmittelbar vor dem Abwurf in die Stossrichtung – Impulsaufbau durch die Gleitbewegung",
        answerC = "Der Athlet nimmt Anlauf und stosst die Kugel aus dem Anlauf heraus ohne zu drehen",
        answerD = "Die Kugel wird mit gestrecktem Arm wie beim Weitwurf geschleudert, nicht gestossen",
        correctAnswer = 1,
        explanation = "Parry O'Brien entwickelte die Technik 1952 und dominierte damit den Kugelstoss jahrelang. Die rueckwaertige Gleitbewegung erlaubt einen laengeren Beschleunigungsweg fuer die Kugel. Heute hat die Drehtechnik (wie beim Diskus) den O'Brien-Stil weitgehend verdraengt, da sie noch mehr Weg bietet.",
        difficulty = 3,
        funFact = "O'Brien verbesserte den Weltrekord im Kugelstossen zwischen 1953 und 1959 sage und schreibe 17 Mal – von 17,95 m bis 19,30 m. Sein Technikannsatz war so revolutionaer, dass er zunächst mit Fassungslosigkeit aufgenommen wurde."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche biomechanische Besonderheit macht den 400-Meter-Lauf physiologisch so extrem anspruchsvoll?",
        answerA = "Er erfordert ausschliesslich anaerobe Energiebereitstellung – der Koerper verbrennt keine Kohlenhydrate, sondern nur Fettreserven",
        answerB = "Der 400-m-Lauf erfordert eine Mischung aus aerober und anaerober Energiebereitstellung bei nahezu maximalem Tempo – Athleten laufen zu schnell fuer vollstaendig aerobes Arbeiten, aber zu lang fuer rein anaerobes Sprinten",
        answerC = "Er belastet ausschliesslich die Beinmuskulatur – Rumpf und Arme spielen keine Rolle",
        answerD = "Der Sauerstoffverbrauch ist identisch mit dem 800-m-Lauf – der Unterschied liegt nur in der Laenge, nicht im Stoffwechsel",
        correctAnswer = 1,
        explanation = "Der 400-m-Lauf gilt als 'toedlichste Distanz'. Bei 100-200 m dominiert das anaerobe System (Phosphocreatinin, Glykolyse ohne Sauerstoff). Ab 300 m muss das aerobe System einspringen, kann die Laktatbelastung aber nicht mehr vollstaendig kompensieren. Das Endergebnis: extreme metabolische Erschoepfung.",
        difficulty = 3,
        funFact = "Michael Johnsons Weltrekord von 43,18 s aus 1999 galt 17 Jahre – van Niekerk brach ihn 2016 mit 43,03 s. Johnson lief die zweite Haelfte nur unwesentlich langsamer als die erste – das ist aussergewoehnlich, denn fast alle anderen Laeufer brechen in der zweiten Haelfte stark ein."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Block-Clearance-Time' beim Sprinterstart und warum ist es taktisch entscheidend?",
        answerA = "Die Zeit, die ein Sprinter braucht, um seine Startbloecke vor dem Rennen aufzubauen und zu justieren",
        answerB = "Die Zeitspanne vom Startschuss bis der Athlet beide Fuesse vollstaendig aus den Startbloecken geloest hat – beeinflusst die Startphasen-Effizienz entscheidend",
        answerC = "Die maximale erlaubte Reaktionszeit beim Sprinterstart – unter 0,1 s gilt als Fehlstart",
        answerD = "Der Zeitraum, in dem nach einem Fehlstart erneut gestartet werden darf",
        correctAnswer = 1,
        explanation = "Die Block-Clearance-Time bestimmt, wie schnell der Athlet aus den Bloecken kommt. Sie wird durch Beinstreckung, Armarbeit und Rumpfspannung optimiert. Elite-Sprinter loesen die Fersen nach ca. 0,3-0,4 Sekunden aus den Bloecken. Eine schlechte Blockphase kann durch Endgeschwindigkeit kaum kompensiert werden.",
        difficulty = 3,
        funFact = "Die Mindest-Reaktionszeit eines Menschen liegt bei etwa 0,1 Sekunden (neurologische Verarbeitungszeit). Unter 0,1 s gilt als Fehlstart, weil ein menschliches Nervensystem physikalisch nicht schneller reagieren kann – eine Antwort darauf, wie prazise die Startregeln sind."
    ),

    // ── Sportdoping-Skandale (7) ──────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welcher Sprinter wurde bei den Olympischen Spielen 1988 in Seoul disqualifiziert, nachdem sein Weltrekord im 100-m-Finale wegen Dopings annulliert wurde?",
        answerA = "Carl Lewis",
        answerB = "Linford Christie",
        answerC = "Ben Johnson",
        answerD = "Leroy Burrell",
        correctAnswer = 2,
        explanation = "Ben Johnson (Kanada) stellte mit 9,79 s einen Weltrekord auf – das Rennen ging als 'dreckigstes Rennen der Geschichte' in die Annalen ein. Im Urin wurde Stanozolol, ein anaboles Steroid, nachgewiesen. Johnsons Medaille wurde aberkannt. Ironisch: sechs der acht Finalisten waren in Dopingvergehen verwickelt.",
        difficulty = 3,
        funFact = "Carl Lewis, der nach Johnsons Disqualifikation Gold erhielt, wurde selbst nachtraeglich mit positiven Doping-Testergebnissen von Trials 1988 konfrontiert – das USOC schuetzte ihn jedoch vor Konsequenzen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war der 'BALCO-Skandal' und welchen bahnbrechenden Effekt hatte er auf den Anti-Doping-Kampf?",
        answerA = "Ein Dopingskandal im chinesischen Schwimmsport der 1990er Jahre, der zum Ausschluss Chinas von der WM 1998 fuehrte",
        answerB = "Ein amerikanischer Dopingskandal (ab 2002), bei dem das Labor BALCO Spitzensportlern massgeschneiderte Designerdrogen – insbesondere THG – lieferte, die damals nicht nachweisbar waren",
        answerC = "Der Dopingskandal der Tour de France 1998, bei dem das Team Festina mit EPO und Wachstumshormonen erwischt wurde",
        answerD = "Ein ostdeutsches staatlich organisiertes Dopingprogramm der DDR von 1974–1989",
        correctAnswer = 1,
        explanation = "BALCO (Bay Area Laboratory Co-operative) entwickelte THG (Tetrahydrogestrinon), ein Designer-Steroid, das so neu war, dass keine Tests existierten. Die USADA erhielt eine anonyme Spritze mit der Substanz und entwickelte in kurzer Zeit einen Nachweis. Betroffen waren Marion Jones, Tim Montgomery, mehrere NFL-Spieler und andere.",
        difficulty = 3,
        funFact = "Marion Jones, fuenffache Olympiamedaillengewinnerin in Sydney 2000, gab 2007 zu, THG waehrend der Spiele benutzt zu haben. Sie verlor alle Medaillen und wurde zu sechs Monaten Gefaengnis verurteilt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist EPO (Erythropoietin) und warum war es im Radsport so verbreitet?",
        answerA = "Ein Betaeubungsmittel zur Schmerzunterdrueckung bei Knochen-Stressfrakturen – half Radfahrern, durch den Schmerz zu fahren",
        answerB = "Ein Hormon, das die Produktion roter Blutkoerperchen stimuliert, dadurch den Sauerstofftransport erhoht und die aerobe Ausdauerleistung signifikant steigert",
        answerC = "Ein Stimulans fuer das zentrale Nervensystem, das Ermuedungsgefuehle unterdrueckt und Aggression foerdert",
        answerD = "Ein Masking-Agent, der andere Dopingmittel im Urin unsichtbar macht",
        correctAnswer = 1,
        explanation = "EPO (endogen produziert von den Nieren) steuert die Erythropoiese. Synthetisches EPO steigert den Haematokrit (rote Blutkoerperchen-Anteil) erheblich, was die VO2max und aerobe Leistung steigert. Im Radsport der 1990er war es so verbreitet, dass man von der 'EPO-Generation' spricht. Synthetisches EPO war bis 2000 kaum nachweisbar.",
        difficulty = 3,
        funFact = "Zu Hochzeiten des EPO-Missbrauchs in den 1990ern starben mehrere junge Profiradrennfahrer im Schlaf – vermutlich weil EPO das Blut verdickte und bei langsamer Herzfrequenz im Schlaf Thromben verursachte. Die genaue Zahl ist bis heute umstritten."
    ),

    Question(
        categoryId = 6,
        questionText = "Was deckte die McLaren-Untersuchung 2016 ueber das russische Doping-System auf?",
        answerA = "Einzelne russische Athleten hatten eigenstaendig Doping betrieben – kein staatliches System nachweisbar",
        answerB = "Das russische Sportministerium hatte ein staatlich orchestriertes Doping-System betrieben, bei dem ueber 1.000 Athleten profitiert hatten und positive Proben bei den Sotschi-Spielen 2014 durch ein Loch in der Wand gegen saubere Proben ausgetauscht wurden",
        answerC = "Russland hatte ausschliesslich im Gewichtheben und Ringen gedopt – andere Sportarten waren nicht betroffen",
        answerD = "Der McLaren-Report stellte fest, dass russische Doping-Verstoesze vergleichbar mit anderen Nationen waren und keine Sonderbehandlung verdienen",
        correctAnswer = 1,
        explanation = "Richard McLarens unabhaengige Untersuchung fuer WADA enthueillte ein staatlich gesteuertes Doping-System. Bei Sotschi 2014 wurden positive Urinproben durch ein klandestines Loch in der Wand des Anti-Doping-Labors nachts ausgetauscht. Russlands Leichtathletik-Verband wurde ausgeschlossen – bei Tokio und Paris starteten Russen als 'AIN' (Neutrale Athleten).",
        difficulty = 3,
        funFact = "Der zustaendige FSB-Offizier Grigory Rodchenkov, der das Austausch-System leitete, floh in die USA und wurde zum Kronzeugen der Untersuchung. Er lebt seitdem im Zeugenschutzprogramm."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Blutdoping-Verfahren nutzte Lance Armstrongs Team bei der Tour de France, wie es spaeter eingeraeumt wurde?",
        answerA = "Ausschliesslich EPO-Injektionen – kein anderes Verfahren",
        answerB = "Autologes Blutdoping: Eigenblut wurde vor Bergankuenften entnommen, gekuehlt gelagert und kurz vor den entscheidenden Etappen wieder transfundiert – kombiniert mit EPO, HGH und Kortison",
        answerC = "Xenogenes Blutdoping: Fremdes Blut mit sehr hohem Haematokrit wurde transfundiert",
        answerD = "Ausschliesslich Testosteron-Pflaster – kein Blutdoping",
        correctAnswer = 1,
        explanation = "Armstrongs Doping-System, enthuellung durch USADA 2012, war das sophistizierteste bis dahin bekannte. Das Team nutzte Eigenbluttransfusionen (damals schwer nachweisbar), EPO, Kortison, Testosteron und Wachstumshormon in einem perfekt organisierten System – Teamarzt Michele Ferrari war der Architekt.",
        difficulty = 3,
        funFact = "Armstrong besiegte Tour-Rivalen nicht nur sportlich, sondern terrorisierte auch Teamkollegen: Wer nicht mitmachte oder reden wollte, wurde aus dem Team geworfen oder mit Klagen bedroht. Mehrere fruehfaellige Aussagen wurden durch Einschuechterung verhindert."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Blutdoping' und worin unterscheiden sich die autologe, homologe und synthetische Variante?",
        answerA = "Blutdoping = Einnahme von Blutverdunnern; autolog = vom Pferd, homolog = vom Menschen, synthetisch = EPO",
        answerB = "Autolog = eigenes Blut wird entnommen und spaeter zurueckgefuehrt; homolog = Fremdblut eines kompatiblen Spenders; synthetisch = kuenstliche Sauerstofftraeger (Blutersatzstoffe)",
        answerC = "Alle Varianten sind identisch – nur der Begriff 'autolog' ist korrekt, die anderen existieren nicht als anerkannte Kategorien",
        answerD = "Blutdoping beschreibt ausschliesslich EPO-Injektionen – andere Formen werden nicht als Blutdoping klassifiziert",
        correctAnswer = 1,
        explanation = "Beim autologen Blutdoping wird eigenes Blut entnommen (Erythrozytenkonzentrat), eingefroren und vor dem Wettkampf transfundiert. Homologes Doping nutzt Fremdblut – nachweisbar durch DNA-Matching im Blut. Synthetische Sauerstofftraeger (HBOC, PFC) sind chemische Verbindungen als Haemoglobin-Ersatz.",
        difficulty = 3,
        funFact = "Die Tour de France 1998 ('Tour du dopage') gilt als Wendepunkt: Als das Festina-Team mit 400 Dopingmitteln erwischt wurde, brach das Rennen fast zusammen. 9 der 20 Teams zogen sich aus Protest gegen Polizeiermittlungen zurueck."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der WADA-Code und welche Kerninhalte enthalten die verbotenen Methoden 'M1' und 'M2'?",
        answerA = "M1 = verbotene Substanzen (Steroide, Stimulanzien); M2 = verbotene Techniken (Doping-Methoden) wie Blutmanipulation",
        answerB = "M1 = Blut- und Sauerstofftraeger-Manipulation (Transfusionen, EPO); M2 = chemische und physikalische Manipulation (Katheterisierung, Urin-Substitution)",
        answerC = "M1 = Doping im Schwimmen; M2 = Doping in der Leichtathletik – nach Sportart gegliedert",
        answerD = "WADA-Codes M1 und M2 beziehen sich auf Ersteinstieg (M1) und Wiederholungstaeter (M2) – keine Substanz-Klassifikation",
        correctAnswer = 1,
        explanation = "Der WADA-Verbotenen-Code unterscheidet Substanzen (S-Klassen) und Methoden (M-Klassen). M1 verbietet Blutverfaelschung/Manipulation (Transfusionen, EPO, Kunstblut), M2 verbietet chemische/physikalische Manipulation der Proben (Katheter, Urinsubstitution, Probenverfalschung).",
        difficulty = 3,
        funFact = "Die WADA hat seit 2004 'Whereabouts' einfuehrt – Athleten muessen 365 Tage im Jahr ihren Aufenthaltsort melden, damit unangekuendigte Tests moeglich sind. Drei versaeumte oder fehlerhafte Meldungen innerhalb von 12 Monaten gelten als Doping-Verstoß."
    ),

    // ── Sporternaehrung (7) ───────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was ist das Prinzip der 'Carbohydrate Loading' (Kohlenhydrat-Loading) und fuer welche Athleten ist es relevant?",
        answerA = "Taeglich grosse Mengen Kohlenhydrate essen – relevant fuer alle Kraftsportler zur Muskelmasseaufbau",
        answerB = "In den Tagen vor einem Langzeitausdauerwettkampf die Kohlenhydratspeicher (Glykogen) in Muskeln und Leber durch erhohte KH-Zufuhr maximal auffuellen, um 'Hitting the Wall' hinauszuzoegern",
        answerC = "Nach einem Marathon innerhalb von 30 Minuten grosse Mengen Fett essen, um die Kohlenhydratspeicher zu regenerieren",
        answerD = "Eine Diaetmethode zur Gewichtsreduktion vor Wettkampf durch Kohlenhydratreduktion",
        correctAnswer = 1,
        explanation = "Carboloading fuellt die Glykogenspeicher der Muskeln vor Wetkaempfen ueber 90 Minuten (Marathon, Triathlon, Radrennen). Klassisch: 3 Tage reduzierte KH + Training 'entleeren' die Speicher, dann 3 Tage hochkalorische KH-Zufuhr bei Tapering. Moderne Methode: nur 1-2 Tage hochkalorische KH geniegen.",
        difficulty = 3,
        funFact = "Der Begriff 'Hitting the Wall' (Auf die Wand laufen) beschreibt den Moment beim Marathon (meist ab km 30-35), wenn die Glykogenspeicher leer sind. Der Koerper muss auf Fettverbrennung umschalten – viel langsamer und ineffizienter."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Aminosaeuren gelten als 'BCAA' und warum sind sie fuer Kraft- und Ausdauersportler relevant?",
        answerA = "Glutamin, Taurin, Kreatin – sie werden direkt im Gehirn verstoffwechselt und verbessern Fokus",
        answerB = "Leucin, Isoleucin, Valin – verzweigtkettige Aminosaeuren (Branched-Chain Amino Acids), die direkt im Muskel oxidiert werden koennen und Muskelproteinabbau reduzieren",
        answerC = "Tryptophan, Methionin, Lysin – essentielle Aminosaeuren, die die Testosteronproduktion ankurbeln",
        answerD = "Alanin, Glycin, Serin – nicht-essentielle Aminosaeuren fuer Gelenkknorpel-Regeneration",
        correctAnswer = 1,
        explanation = "BCAAs (Leucin, Isoleucin, Valin) sind essentielle Aminosaeuren mit verzweigter Kohlenstoffkette. Besonders Leucin aktiviert mTOR – den Hauptschalter der Muskelproteinsynthese. BCAAs werden im Muskel direkt oxidiert, koennen Energie liefern und Muskelkatabolismus (Abbau) bei Belastung reduzieren.",
        difficulty = 3,
        funFact = "Der Leucin-Schwellenwert fuer maximale Muskelproteinsynthese liegt bei ca. 2-3 g pro Mahlzeit. Diese Menge steckt in etwa 25-30 g Whey-Protein – weshalb Ernährungswissenschaftler Mahlzeiten mit mindestens 20-40 g Protein empfehlen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter dem 'anabolen Fenster' (Post-Workout Window) und wie differenziert aktuelle Forschung diese Annahme?",
        answerA = "Das anabole Fenster ist real und betraegt exakt 30 Minuten nach dem Training – wer spaeter isst, verliert alle Trainingseffekte",
        answerB = "Die Idee, dass Protein unmittelbar nach dem Training gegessen werden muss, um Muskelaufbau zu maximieren – neuere Forschung zeigt jedoch, dass das Gesamtprotein ueber den Tag wichtiger ist als das genaue Timing",
        answerC = "Das anabole Fenster betrifft ausschliesslich Kohlenhydrate, nicht Protein – Zucker direkt nach dem Training ist entscheidend",
        answerD = "Das anabole Fenster ist ein Marketingbegriff der Supplementindustrie ohne jegliche wissenschaftliche Grundlage",
        correctAnswer = 1,
        explanation = "Frueherer Konsens: 30-60 Minuten nach dem Training schnell Protein und Kohlenhydrate essen. Aktuelle Metaanalysen (Aragon & Schoenfeld, 2013) zeigen: Das Timing hat einen bescheidenen Effekt; entscheidend ist das Gesamtprotein (1,6-2,2 g/kg Koerpergewicht/Tag) verteilt auf mehrere Mahlzeiten. Das 'Fenster' ist breiter als gedacht.",
        difficulty = 3,
        funFact = "Die Supplementindustrie verdient Milliarden mit Post-Workout-Produkten, die auf dem anabolen Fenster basieren. Wer ausreichend Protein verteilt ueber den Tag essen, braucht diese teuren Praeparate wissenschaftlich gesehen nicht."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Funktion hat Kreatin (Creatine) als Nahrungsergaenzungsmittel im Sport und wie wirkt es?",
        answerA = "Kreatin erhoht die Fettsaeureoxidation und hilft beim Gewichtsverlust, nicht bei Kraftleistungen",
        answerB = "Kreatin fuellt die Phosphokreatin-Speicher in den Muskeln auf, was die Regeneration von ATP bei kurzen intensiven Belastungen (Sprints, schwere Saetze) beschleunigt und Maximalkraft sowie explosive Kraft erhoht",
        answerC = "Kreatin ist ein Hormon-Vorlaeufer fuer Testosteron – es wirkt durch Steigerung des Hormonspiegels",
        answerD = "Kreatin verbessert die aerobe Ausdauer bei Marathonlaeufen und Triathlons, hat aber keinen Effekt auf Kraft",
        correctAnswer = 1,
        explanation = "Phosphokreatin (PCr) ist das schnellste ATP-Regenerationssystem im Muskel (erste 10 Sekunden maximaler Belastung). Kreatin-Supplementierung erhoht PCr-Speicher um ca. 20%. Meta-Analysen zeigen konsistente Effekte auf Maximalkraft (+5-15%), Wiederholungsanzahl und Kurzzeitausdauer. Das am besten untersuchte legale Supplement.",
        difficulty = 3,
        funFact = "Kreatin ist eine der wenigen Supplemente, fuer die ein wissenschaftlicher Konsens besteht. Sogar konservative Institutionen wie ISSN (International Society of Sports Nutrition) bezeichnen es als 'das effektivste ergoagene Nahrungsergaenzungsmittel'."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Besonderheit hat die Energieversorgung beim Ultra-Ausdauersport (z.B. 100-Meilen-Laeufen) im Vergleich zum Marathon?",
        answerA = "Ultrasport laeuft ausschliesslich auf Glukose-Basis – der Koerper kann kein Fett als Energiequelle nutzen, wenn der Glykogenspeicher leer ist",
        answerB = "Bei Ultradistanzen (ab ca. 6+ Stunden) wird Fettoxidation zur dominanten Energiequelle; der Koerper muss trainiert sein, Fett effizient bei maessiger Intensitaet zu verbrennen ('fat adaptation'), und der GI-Trakt wird zur begrenzenden Komponente",
        answerC = "Ultra-Athleten essen ausschliesslich feste Nahrung – Fluessignahrung (Gele, Getranke) ist bei Ultradistanzen kontraindiziert",
        answerD = "Der Energieverbrauch bei Ultralaufen ist pro Stunde identisch mit einem Marathon-Pace – nur die Gesamtdauer ist laenger",
        correctAnswer = 1,
        explanation = "Bei Ultras (>6 h) sind Glykogenspeicher irgendwann leer. Der Koerper muss auf Lipolyse umschalten – Fettsaeuren aus Depotfett liefern Energie (Betaoxidation). Die Intensitaet sinkt proportional. Entscheidend: Gastrointestinale Toleranz (kein Erbrechung, keine Magenprobleme) bestimmt oft den Wettkampfausgang.",
        difficulty = 3,
        funFact = "Kilian Jornet, einer der besten Ultra-Traillaeufer aller Zeiten, lief 2017 den Everest (Gipfelversuch) in 26 Stunden vom Basislager und verbrauchte dabei schatzungsweise 15.000 Kalorien. Sein Ernaehrungskonzept waehrend des Laufs: Suppe, Nuesse und Riegel."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Relative Energy Deficiency in Sport' (RED-S) und wer ist besonders gefaehrdet?",
        answerA = "Ein Vitamin-D-Mangel bei Innen-Athleten (Hallentrainingssportler) ohne Sonnenexposition",
        answerB = "Ein Syndrom, bei dem chronisch zu wenig Energie verfuegbar ist – Kalorienzufuhr reicht nicht aus, um Trainingsbelastung und koerperliche Funktionen zu stuetzen; gefaehrdet sind besonders Ausdauersportler, Springsportler und Kampfkuenstler mit Gewichtslimits",
        answerC = "Eine seltene Stoffwechselkrankheit, die ausschliesslich genetisch bedingt ist und nichts mit Ernaehrung zu tun hat",
        answerD = "Der Zustand von Kraftsportlern nach einer 'Bulk-Phase' mit zu vielen Kalorien – Energy Surplus, nicht Deficit",
        correctAnswer = 1,
        explanation = "RED-S (vormals als 'Athletentriade' bekannt) beschreibt den Zustand, wenn Energieverfuegbarkeit = Kalorienaufnahme minus Trainingsenergie zu gering ist. Folgen: Hormonstoeungen, Knochendichteverlust (Stressfrakturen), Immunsuppression, kardiovaskulaere Probleme und Leistungsabfall. Betroffen sind beide Geschlechter.",
        difficulty = 3,
        funFact = "RED-S war lange als 'Frauenproblem' (Athletentriade: Essstoerung, Amenorrhoe, Osteoporose) verkannt. Seit 2014 erkennt das IOC, dass Maenner ebenfalls betroffen sind – z.B. Leichtgewichts-Ruderer, Jockeys und Ringer mit chronischer Gewichtsreduktion."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Rolle spielt die intestinale Mikrobiota (Darmflora) fuer die sportliche Leistung?",
        answerA = "Die Darmflora hat keinen nachgewiesenen Einfluss auf Sport – sie betrifft nur die Verdauungsgesundheit",
        answerB = "Eine diverse Darmflora verbessert nachweislich Naehrstoffabsorption, Immunfunktion und Entzuendungsreaktion; bestimmte Bakterienstaemme (z.B. Veillonella) wurden bei Elite-Laeuern in hoeherer Konzentration gefunden und koennen Laktat in Propionat umwandeln",
        answerC = "Die Darmflora ist ausschliesslich durch Probiotika-Supplemente beeinflussbar – Ernaehrung hat keinen Effekt",
        answerD = "Ausdauersport schaedigt die Darmflora irreversibel – sportliche Athleten haben immer schlechtere Darmgesundheit als Nicht-Sportler",
        correctAnswer = 1,
        explanation = "Eine 2019 in Nature Medicine veroeffentliche Studie fand, dass Boston-Marathon-Laeufer signifikant mehr Veillonella atypica im Darm hatten. Dieses Bakterium metabolisiert Laktat (Abfallprodukt der Muskeln) in Propionat – einen Kurzkettenfettsaeuren, die die Ausdauer verbessern koennen. Ernaehrung und Sport beeinflussen das Mikrobiom wechselseitig.",
        difficulty = 3,
        funFact = "Wenn Maeusen das Bakterium Veillonella atypica eingegeben wurde, liefen sie im Laufrad nachweislich laenger. Das Forschungsfeld 'Sport-Mikrobiom' ist eines der heissesten in der Sportwissenschaft der 2020er Jahre."
    ),

    // ── Wintersport-Technik (7) – neue Aspekte ────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was unterscheidet den 'klassischen Stil' vom 'Skating-Stil' (Freistil) im Skilanglauf?",
        answerA = "Klassisch = Ski parallel in zwei vorgefertigten Spuren, Abstossung nach hinten; Skating = seitliches Abstossen wie beim Schlittschuhlaufen in einem V-Winkel ohne Spuren",
        answerB = "Klassisch = Skating-Bewegung; Skating = langsame Diagonalschrittbewegung – die Begriffe sind historisch vertauscht worden",
        answerC = "Klassisch = nur ohne Stoecke; Skating = nur mit Stoecken – der einzige Unterschied ist die Stocknutzung",
        answerD = "Beide Stile sind identisch – der einzige Unterschied ist das Gelaende, auf dem sie genutzt werden",
        correctAnswer = 0,
        explanation = "Im klassischen Stil laufen die Ski parallel in Spurrillen; der Skier stosst sich nach hinten ab (Schuppentechnik oder Kick-and-Glide mit praepariertem Mittelfeld). Beim Skating wird der Ski in einem Winkel von ca. 15-25 Grad nach aussen gesetzt und seitlich abgestossen – deutlich schneller, erfordert breit praeparierte Loipen.",
        difficulty = 3,
        funFact = "Skating im Skilanglauf wurde erst in den 1980ern populaer. Bill Koch (USA) revolutionierte 1982 die Szene, als er den Weltcup mit Skating gewann – damals noch ohne klare Regelung. Seitdem gibt es getrennte Bewerbe fuer klassisch und Skating."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter 'Carving' beim alpinen Skisport?",
        answerA = "Das Bremsen durch Querstellung der Ski (Schneepflug/Parallelfahrt)",
        answerB = "Skifahren, bei dem die Ski auf ihren Seitenkanten 'eingeschnitten' werden und einen sauberen Kurvenradius ohne Seitwaeisgleiten (Drift) beschreiben – moeglich durch den taillierten Ski",
        answerC = "Die Technik, den Schnee mit dem Skischuh zu verdichten, um stabilere Pisten zu erzeugen",
        answerD = "Eine Sprungdisziplin, bei der Skifahrer mit dem Ski dekorative Muster in den Schnee schneiden",
        correctAnswer = 1,
        explanation = "Carving-Ski haben eine ausgepragte Taille (schmal in der Mitte, breiter vorne und hinten). Wird der Ski auf die Kante gestellt und Druck ausgeuebt, biegt er sich und laeuft von selbst in einem Radius, der seinem Taillierungsradius entspricht – kein Seitwaeisgleiten noetig. Revolutionaerer Durchbruch der 1990er.",
        difficulty = 3,
        funFact = "Vor Carving-Ski waren Ski fast gerade – Kurven entstanden durch aktives Kanten und 'Rutschen'. Carving-Ski aus den 1990ern hatten einen Seitenradius von 20-40 m; moderne Race-Carver kommen auf 9-13 m und ermoelichen dramatisch engere Kurven bei hoeheren Geschwindigkeiten."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Telemark-Fahren' und welche historische Bedeutung hat diese Skitechnik?",
        answerA = "Eine moderne Abfahrtstechnik mit fixierten Schuhbindungen – identisch mit alpinem Skifahren ausser dem Materialgewicht",
        answerB = "Eine Skitechnik mit nicht fixierter Ferse, bei der Kurven durch Ausfallschritt (hinteres Knie sinkt ab) gefahren werden – urspruenglich aus der norwegischen Region Telemark stammend",
        answerC = "Eine Langlauftechnik aus Finnland fuer extrem steiles Gelaende mit Spikes unter den Skiern",
        answerD = "Die erste alpine Renndisziplin der Olympischen Winterspiele 1924 in Chamonix",
        correctAnswer = 1,
        explanation = "Telemark-Ski haben eine freie Ferse (Bindung fixiert nur Schuhspitze). Kurven werden durch den charakteristischen 'Telemarkschritt' gefahren: Das aeussere (Bergski) Knie ist gestreckt, das innere Knie sinkt in einem tiefen Ausfallschritt ab. Die Technik wurde von Sondre Norheim im norwegischen Telemark (ca. 1860) entwickelt.",
        difficulty = 3,
        funFact = "Sondre Norheim gilt als Vater des modernen Skisports – er entwickelte nicht nur Telemark, sondern auch die erste praktische Skibindung und den Christiania-Schwung. Beim ersten Skirennen 1868 in Christiana (Oslo) gewann er mit Abstand."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches physikalische Prinzip erklaert, warum Skilanglaeufer bergab Aerodynamik optimieren, obwohl kein aktiver Energieeinsatz stattfindet?",
        answerA = "Bergab brauchen Skilaeufer mehr Muskelkraft – Aerodynamik ist irrelevant bei Abfahrten",
        answerB = "Die potentielle Energie (Lageenergie) wird beim Abfahren in kinetische Energie (Bewegungsenergie) umgewandelt; Luftwiderstand (steigt mit dem Quadrat der Geschwindigkeit) ist der groesste bremsende Faktor – Aerodynamik spart keine Energie, sondern mindert den Energieverlust durch Reibung",
        answerC = "Skilanglaeufer bergab erzeugen elektrische Energie durch den Schneereibungseffekt, die sie in Steigungen nutzen",
        answerD = "Aerodynamik ist beim Skilanglauf irrelevant – Schneereibung ist der einzige begrenzende Faktor",
        correctAnswer = 1,
        explanation = "Ep = m × g × h wird zu Ekin = ½ × m × v² umgewandelt. Der Luftwiderstand FW = ½ × ρ × v² × CW × A waechst quadratisch mit der Geschwindigkeit. Ab ca. 60 km/h ist Aerodynamik dominanter als Schneereibung. Deshalb gehen Skilanglaeufer bergab in Aeroduck-Haltung (Egg-Position), um Luftwiderstand zu minimieren.",
        difficulty = 3,
        funFact = "Der Weltrekord im Speedskiing (Abfahrt auf Ski) liegt bei 254,958 km/h (Ivan Origone, 2016). Bei dieser Geschwindigkeit ist Aerodynamik absolut dominierend – der Athlet traegt einen Rennanzug, der aerodynamisch perfekter ist als manche Autokarosserie."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Mogul-Ski' (Buckelpisten-Fahren) und welche technischen Anforderungen stellt es?",
        answerA = "Eine Disziplin, bei der Ski mit extra breiten Buckeln (Moguls) ausgestattet werden fuer bessere Kraftuebertragung im Tiefschnee",
        answerB = "Eine Freestyle-Disziplin auf kuenstlich oder natuerlich angelegten Buckelpisten (Moguls), bei der Athleten schnell durch die Buckeln fahren (Knietechnik: Beine als Stossdaempfer), zwei Spruenge absolvieren und nach Geschwindigkeit, Turns und Spruengen bewertet werden",
        answerC = "Eine historische Skitechnik aus Oesterreich, bei der der Fahrer seitlich die Piste hinuntergleitet",
        answerD = "Eine Alpin-Disziplin ohne Tore, bei der der kuerzeste Weg bergab genommen wird",
        correctAnswer = 1,
        explanation = "Mogul-Fahren ist seit 1992 olympisch. Die Buckelpiste hat festgelegte Dimensionen; Athleten muessen aktiv durch die Moguls absorbieren (Knie wirken als Stossdaempfer, Oberkörper bleibt ruhig) und zwei Spruenge mit Akrobatik einbauen. Bewertet wird: 50% Turns, 25% Spruenge, 25% Geschwindigkeit.",
        difficulty = 3,
        funFact = "Die groeßte Herausforderung beim Mogul-Fahren: Die Knie machen bei voller Fahrt durch enge Moguls bis zu 10-12 Beugungen pro Sekunde. Mogul-Spezialisten haben eine der hoechsten Knie-Belastungen im gesamten Wintersport – Knieoperationen sind in dieser Disziplin sehr haeufig."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'Halfpipe' im Snowboard- und Freeski-Kontext?",
        answerA = "Eine U-foermige Schneestruktur (halbes Rohr), in der Athleten zwischen den Waenden hin- und herpendeln, bei jedem Ausfahren Tricks vollziehen und nach Hoehe, Tricks und Landungen bewertet werden",
        answerB = "Eine Disziplin, bei der Snowboarder eine Roehre aus Eis durchfahren, aehnlich wie eine Wasserrohrrutsche aus Schnee",
        answerC = "Eine Trainingstechnik, bei der Snowboarder Halbpfeife aus Holz trainieren, bevor sie auf echtem Schnee starten",
        answerD = "Die unterirdische Aufwaermzone direkt unter der Starthuette grosser Snowboard-Wettbewerbe",
        correctAnswer = 0,
        explanation = "Die Halfpipe (ca. 150-170 m lang, Waende ca. 6-7 m hoch) wird mit speziellen Pistenmaschinen praepariert. Athleten fahren von Wand zu Wand (im Rhythmus ca. 10-12 Durchgaenge), schrauben sich bei jedem Ausfahren bis zu 7 m ueber den Wannenrand und fuehren Tricks (Rotationen, Grabs) aus.",
        difficulty = 3,
        funFact = "Shaun White gewann bei drei Olympischen Spielen (2006, 2010, 2018) Halfpipe-Gold. Sein beruehmter Trick 'Double McTwist 1260' (zwei Ueberschlaege + 3,5 Rotationen) war jahrelang so schwierig, dass er ihn im Wettkampf oft weglies – er hatte genug Vorsprung ohne ihn."
    ),

    // ── Olympische Kuriositaeten (7) ──────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welche olympische Disziplin wurde bei den Spielen 1900 in Paris ausgetragen, die heute laengst nicht mehr olympisch ist?",
        answerA = "Tauziehen – und es war auch noch bei den Spielen 1920 olympisch",
        answerB = "Motorsport – Autorennen galten 1900 als Sport",
        answerC = "Schachboxen – eine Kombination aus Schach und Boxen war 1900 olympisch",
        answerD = "Luftgewehr-Fechten – eine Kombination beider Disziplinen",
        correctAnswer = 0,
        explanation = "Tauziehen war von 1900 bis 1920 olympisch. Das britische Team der Polizei (City of London Police) gewann 1908 Gold. Auch weitere kuriose Disziplinen gehorten zum Programm der fruehen Spiele: Tauchen nach Entfernung, Hindernisschwimmen, Unterwasser-Schwimmen.",
        difficulty = 3,
        funFact = "Bei den Olympischen Spielen 1900 in Paris wurden auch Tauben geschossen (Live Pigeon Shooting) – ein echter Sport, bei dem Tauben in Echtzeit erschossen wurden. Es war das einzige Mal in der olympischen Geschichte, bei dem Tiere aktiv getoetet wurden."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war das 'Phantom der Olympischen Spiele 1904' – der Fall des Felix Carvajal?",
        answerA = "Ein Laeufer, der unsichtbar startete, da kein Startblock vorhanden war – er lief barfuss und wurde disqualifiziert",
        answerB = "Ein kubanischer Postbote, der beim Marathon 1904 in St. Louis aus dem Publikum startete, unterwegs Aepfel von einem Obstgarten ass, einen Ruheausflug machte und trotzdem Vierter wurde",
        answerC = "Ein amerikanischer Sprinter, der mit falschen Papieren unter fremdem Namen startete und erst nach 50 Jahren entlarvt wurde",
        answerD = "Ein Ringer, der einen ganzen Wettkampftag verschlief und trotzdem Silber gewann, weil sein Gegner disqualifiziert wurde",
        correctAnswer = 1,
        explanation = "Felix Carvajal aus Kuba joggete zum Startplatz (er hatte kein Geld fuer die Einreise), startete in Strassenschuhen und langen Hosen (ein Athlet schnitt die Hosen zu Shorts ab). Unterwegs biss er in verfaulte Aepfel, wurde krank, machte eine Pause – und wurde trotzdem Vierter. Die Spiele 1904 in St. Louis gelten als chaotischste Olympischen Spiele der Geschichte.",
        difficulty = 3,
        funFact = "Der eigentliche Marathonsieger 1904, Fred Lorz, wurde disqualifiziert: Er hatte 18 km im Auto mitgefahren. Thomas Hicks, der schliesslich Gold bekam, war mit Strychnin und Brandy 'behandelt' worden – beides damals legal."
    ),

    Question(
        categoryId = 6,
        questionText = "Was passierte beim 100-m-Finale der Olympischen Spiele 1996 in Atlanta und welche historische Besonderheit hatte Donovan Baileys Sieg?",
        answerA = "Bailey gewann Gold in 9,84 s – ein neuer Weltrekord und der erste unter 9,85 s – ausserdem disqualifizierte sich Linford Christie nach zwei Fehlstarts",
        answerB = "Bailey wurde wegen Dopings nachtraeglich disqualifiziert – das Rennen ist in der Statistik geloescht",
        answerC = "Das Rennen wurde zweimal wiederholt, weil Bogenschuetzen die Bahn ueberquerten",
        answerD = "Bailey gewann, aber der Weltrekord war unguelig, weil der Rueckenwind ueber dem erlaubten Limit lag",
        correctAnswer = 0,
        explanation = "Donovan Bailey (Kanada) gewann 1996 in 9,84 s (damaliger Weltrekord) und wurde Olympiasieger. Linford Christie (Grossbritannien, Titelverteidiger) wurde nach zwei Fehlstarts disqualifiziert – er weigerte sich zunaechst, die Bahn zu verlassen. Christie beteuerte bis zuletzt, er habe legal gestartet.",
        difficulty = 3,
        funFact = "1997 veranstaltete Bailey einen direkten Showdown gegen Michael Johnson (400-m-Weltrekordler) ueber 150 m – wer war schneller? Bailey gewann, als Johnson bei 110 m mit Oberschenkelverletzung aufgab. Das Rennen war ein kommerzielles Spektakel, aber sportlich nie eindeutig aufgeloest."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war die 'Skandalsitzung' bei den Olympischen Spielen 2002 in Salt Lake City im Eiskunstlauf-Paarlauf?",
        answerA = "Ein kanadisches Paar wurde disqualifiziert, weil einer der Laeufer betrunken auftrat",
        answerB = "Die Kampfrichterin Marie-Reine Le Gougne (Frankreich) gestand, unter Druck des franzoesischen Verbandes das russische Paar Berezhnaya/Sikharulidze vor dem kanadischen Paar Sale/Pelletier bewertet zu haben – was zu zwei Gold-Medaillen fuehrte",
        answerC = "Das russische Paar stuerzte im freien Lauf, wurde aber trotzdem Olympiasieger durch eine Regelluecke",
        answerD = "Drei Paare wurden wegen identischer Choreographien disqualifiziert, die von derselben Choreographin entworfen worden waren",
        correctAnswer = 1,
        explanation = "Sale/Pelletier (Kanada) liefen nach allgemeinem Empfinden besser als Berezhnaya/Sikharulidze (Russland), verloren aber mit 5:4 Kampfrichterstimmen. Le Gougne gab zu, abgesprochen zu haben. IOC vergab daraufhin zwei Goldmedaillen. Der Skandal fuehrte zur Einfuehrung des ISU Judging Systems (IJS), das das alte 6,0-System abloeiste.",
        difficulty = 3,
        funFact = "Hinter dem Skandal soll ein Deal stehen: Frankreich bewertet Russland im Paarlauf hoch, Russland bewertet Frankreich im Eistanz hoch. Der franzoesische Funktionaer Didier Gailhaguet, angeblich Hauptdrahtzieher, wurde zeitweise suspendiert und trat 2020 nach weiteren Vorwuerfen zurueck."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche olympische Regel verbietet politische Statements und welches Ereignis von 1968 ist das bekannteste Beispiel fuer einen Verstoss?",
        answerA = "Regel 40 des Olympic Charters; bekanntestes Beispiel: Jesse Owens streckte 1936 den Arm aus Protest gegen Hitler",
        answerB = "Regel 50 des Olympic Charters; bekanntestes Beispiel: Tommie Smith und John Carlos streckten bei der Siegerehrung 1968 die Faeueste in schwarzen Handschuhen als Black-Power-Salut",
        answerC = "Regel 25 des IOC-Statuts; bekanntestes Beispiel: Mark Spitz trug 1972 muendlich Kritik am Olympiasystem vor",
        answerD = "Regel 70 des Olympic Charters; bekanntestes Beispiel: Naomi Osaka verweigerte 2021 die Siegerehrung aus mentalen Gesundheitsgruenden",
        correctAnswer = 1,
        explanation = "Regel 50 verbietet politische, religioese und rassische Propaganda bei Olympia. Smith und Carlos (USA) streckten 1968 in Mexiko-City barfuss auf dem Podium die Faeueste – ein stilles, kraftvolles Symbol. Beide wurden daraufhin aus dem olympischen Dorf ausgewiesen und in den USA als Verraeter beschimpft, spaeter aber geehrt.",
        difficulty = 3,
        funFact = "Der Silbermedaillengewinner Peter Norman (Australien) trug in Solidaritaet ein Abzeichen des Olympic Project for Human Rights. Er wurde danach in Australien politisch isoliert und nicht fuer die 1972er Spiele nominiert, obwohl er die Qualifikation hatte. Smith und Carlos trugen seinen Sarg bei seiner Beerdigung 2006."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter 'Olympischer Flame Relay' und wie begann diese Tradition?",
        answerA = "Das Weitergeben des olympischen Feuers per Fackellauf vom letzten Austragungsort zum naechsten – Tradition seit 1896",
        answerB = "Das Entzuenden eines Feuers mit Sonnenspiegeln in Olympia (Griechenland) und das anschliessende Relay mit Laeufer-Staffeln zum Gastgeberland – moderne Tradition erstmals 1936 in Berlin einfuehrt von Carl Diem",
        answerC = "Das Feuer wird in einem olympischen Kochtopf aus dem Vorjahr bei jedem Austragungsort aufbewahrt und zum naechsten getragen",
        answerD = "Laeufer tragen seit 1948 eine Fackel vom Olympia-Museum in Lausanne (Schweiz) zum Gastgeberort",
        correctAnswer = 1,
        explanation = "Carl Diem organisierte fuer die Spiele 1936 in Berlin erstmals den modernen Fackellauf vom Herd der Vesta im antiken Olympia durch mehrere Laender. Die Flamme wird mit einem konkaven Spiegel (Parabolspiegel) von Sonnenstrahlen entzuendet – kein Feuerzeug, keine Zuendholzer.",
        difficulty = 3,
        funFact = "Bei den Olympischen Spielen in Tokio 2020 (ausgetragen 2021) wurde das Fackellauf-Konzept stark modifiziert – COVID-19-Bedenken fuehrten dazu, dass viele oeffentliche Events ausfielen oder nur mit minimialer Zuschauerzahl stattfanden. Die Flamme reiste trotzdem weltweit."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war der 'Equestrian Scandal' bei den Olympischen Spielen 2008 in Peking und welche Konsequenz hatte er?",
        answerA = "Das irische Dressurpferd Waterford Crystal wurde nach Dopingtest positiv getestet – Irland verlor eine Bronzemedaille",
        answerB = "Das deutsche Dressurpferd Warrior wurde nach Wettkampfende eingeschlaefert, was internationale Proteste ausloeiste",
        answerC = "Das koreanische Bunsenbrenner-Team wurde disqualifiziert, weil die Springpferde nicht die richtige Mindesthoehe hatten",
        answerD = "Drei Laenderdelegationen wurden ausgeschlossen, weil sie Pferde mit gefaelschten Herkunftsnachweisen eingesetzt hatten",
        correctAnswer = 0,
        explanation = "Das irische Springpferd Waterford Crystal testete positiv auf Capsaicin (Pfefferspray-Wirkstoff) – laut Teammanagement versehentlich. Irland verlor Bronze im Springreiten. Der Fall loeste eine Debatte ueber Dopingregeln im Reitsport aus, bei dem nicht der Athlet, sondern das Tier getestet wird.",
        difficulty = 3,
        funFact = "Im olympischen Reitsport wird das Pferd als 'Athlet' betrachtet – es wird gedopt, nicht der Reiter. Substanzen, die Schmerzen uebertuenchen (wie Capsaicin) sind verboten, weil sie Pferde dazu bringen koennen, Hindernisse zu ueberspringen trotz Verletzungen."
    )
)
