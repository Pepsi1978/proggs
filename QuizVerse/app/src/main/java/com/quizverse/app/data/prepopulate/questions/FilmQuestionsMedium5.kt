package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsMedium5(): List<Question> = listOf(

    // ── FILM TECHNOLOGY: TECHNICOLOR ─────────────────────────────────────────

    // Question 1 – Technicolor: Three-strip process introduction year
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr wurde das Drei-Streifen-Technicolor-Verfahren erstmals in einem Spielfilm eingesetzt?",
        answerA = "1922",
        answerB = "1928",
        answerC = "1934",
        answerD = "1939",
        correctAnswer = 2, // C
        explanation = "Das Drei-Streifen-Technicolor-Verfahren wurde 1934 erstmals in dem Kurzfilm 'La Cucaracha' eingesetzt und ermoeglichte erstmals naturgetreue Farben auf der Leinwand.",
        difficulty = 2,
        funFact = "Bei Technicolor wurden gleichzeitig drei Filmstreifen durch prismatische Strahlteilung belichtet, je einer fuer Rot, Gruen und Blau."
    ),

    // Question 2 – Technicolor: Famous early feature film
    Question(
        categoryId = 4,
        questionText = "Welcher ikonische Film von 1939 nutzte Technicolor, um den Wechsel von Kansas nach Oz darzustellen?",
        answerA = "Schneewittchen und die sieben Zwerge",
        answerB = "Der Zauberer von Oz",
        answerC = "Vom Winde verweht",
        answerD = "Pinocchio",
        correctAnswer = 1, // B
        explanation = "Der Zauberer von Oz (1939) nutzte Technicolor gezielt: Das trostlose Kansas wurde in Schwarz-Weiss gedreht, waehrend das maerchenhaf te Oz in satten Technicolor-Farben erstrahlte.",
        difficulty = 2,
        funFact = "Vom Winde verweht (ebenfalls 1939) war zum Zeitpunkt seiner Premiere der teuerste in Technicolor gedrehte Film und nutzte alle vier Streifen des Verfahrens."
    ),

    // ── FILM TECHNOLOGY: CINEMASCOPE ─────────────────────────────────────────

    // Question 3 – CinemaScope: Aspect ratio
    Question(
        categoryId = 4,
        questionText = "Welches Bildverhaeltnis (Aspect Ratio) war charakteristisch fuer das CinemaScope-Format der 1950er-Jahre?",
        answerA = "1,33:1 (klassisches Academy-Format)",
        answerB = "1,85:1 (widescreen)",
        answerC = "2,39:1 (anamorphotisch)",
        answerD = "2,76:1 (Ultra Panavision)",
        correctAnswer = 2, // C
        explanation = "CinemaScope verwendete anamorphotische Linsen, die das Bild beim Drehen horizontal komprimierten und beim Projizieren wieder aufdehnten, was ein Bildverhaeltnis von ca. 2,39:1 ergab.",
        difficulty = 2,
        funFact = "CinemaScope wurde 1953 von 20th Century Fox entwickelt, um das Fernsehen zu konkurrenzieren und das Publikum wieder in die Kinos zu locken."
    ),

    // Question 4 – CinemaScope: First feature film
    Question(
        categoryId = 4,
        questionText = "Welcher Film war 1953 der erste Spielfilm, der im CinemaScope-Verfahren gedreht und veroeffentlicht wurde?",
        answerA = "Ben-Hur",
        answerB = "Wie man einen Millionaer heiratet",
        answerC = "Das Gewand",
        answerD = "The Robe (Das Gewand)",
        correctAnswer = 3, // D
        explanation = "The Robe (Das Gewand, 1953) von 20th Century Fox war der erste Spielfilm, der im CinemaScope-Format in die Kinos kam. Die Bibel-Epik erzielte damit enorme Publikumswirkung.",
        difficulty = 2,
        funFact = "Fox vermarktete CinemaScope mit dem Slogan 'The Modern Miracle You See Without Glasses' als Gegenentwurf zum damals aufkommenden 3D-Kino."
    ),

    // ── FILM TECHNOLOGY: DOLBY ATMOS ─────────────────────────────────────────

    // Question 5 – Dolby Atmos: Introduction
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr wurde Dolby Atmos erstmals in einem kommerziellen Kinofilm eingesetzt?",
        answerA = "2005",
        answerB = "2009",
        answerC = "2012",
        answerD = "2016",
        correctAnswer = 2, // C
        explanation = "Dolby Atmos wurde 2012 mit dem Pixar-Film 'Merida - Legende der Highlands' (Brave) eingefuehrt. Das System ermoeglicht Ton-Objekte im dreidimensionalen Raum statt fixer Kanalzuweisungen.",
        difficulty = 2,
        funFact = "Dolby Atmos kann bis zu 128 gleichzeitige Audio-Objekte und bis zu 64 Lautsprecher-Feeds verarbeiten, auch Decken-Lautsprecher fuer 'Hoehenton'."
    ),

    // Question 6 – Dolby Atmos: Technical principle
    Question(
        categoryId = 4,
        questionText = "Was ist das Hauptmerkmal von Dolby Atmos gegenueber klassischen 5.1- oder 7.1-Surround-Systemen?",
        answerA = "Es verwendet doppelt so viele Subwoofer fuer mehr Bass",
        answerB = "Klangobjekte werden im dreidimensionalen Raum positioniert statt festen Kanaelen zugewiesen",
        answerC = "Es synchronisiert Bild und Ton 100 Millisekunden frueher",
        answerD = "Es komprimiert den Ton staerker fuer kleinere Dateigroessen",
        correctAnswer = 1, // B
        explanation = "Bei Dolby Atmos werden Klaenge als 'Objekte' mit dreidimensionalen Koordinaten gespeichert. Das Abmischsystem platziert diese dann dynamisch auf die vorhandenen Lautsprecher – inklusive Deckenlautsprecher fuer vertikale Raumwirkung.",
        difficulty = 2,
        funFact = "Dolby Atmos im Heimkino laeuft ueber normale Soundbars oder AV-Receiver, die den 3D-Sound dann auf vorhandene Lautsprecher herunterrechnen."
    ),

    // ── FILM TECHNOLOGY: MOTION CAPTURE ──────────────────────────────────────

    // Question 7 – Motion Capture: Gollum
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler verlieh Gollum in 'Der Herr der Ringe' durch Motion-Capture Leben und gilt als Pionier dieser Technik?",
        answerA = "Ian McKellen",
        answerB = "Viggo Mortensen",
        answerC = "Andy Serkis",
        answerD = "Orlando Bloom",
        correctAnswer = 2, // C
        explanation = "Andy Serkis spielte Gollum vollstaendig per Motion-Capture und wurde damit zum Pionier der Performance-Capture-Technik. Sein koerperliches Spiel wurde digital in den Charakter uebertragen.",
        difficulty = 2,
        funFact = "Peter Jackson setzte sich stark dafuer ein, dass Andy Serkis fuer den Oscar nominiert wird - was die Academy jedoch ablehnte, da Gollum ein digitaler Charakter ist."
    ),

    // Question 8 – Motion Capture: Technical process
    Question(
        categoryId = 4,
        questionText = "Was wird beim Motion-Capture-Verfahren am Koerper des Schauspielers befestigt, um Bewegungen zu erfassen?",
        answerA = "Kleine Elektroden, die Muskelspannung messen",
        answerB = "Reflektierende Markierungspunkte, die von Infrarotkameras verfolgt werden",
        answerC = "Ein Exoskelett aus Carbonfasern",
        answerD = "Faseroptische Sensoren in den Gelenken",
        correctAnswer = 1, // B
        explanation = "Beim klassischen Motion Capture werden retroreflektierende Kugeln (Marker) an Anzug und Gesicht befestigt. Infrarotkameras erfassen deren Position im Raum, woraus ein digitales Skelett berechnet wird.",
        difficulty = 2,
        funFact = "Fuer Gesichts-Performance-Capture wird oft eine Helmkamera eingesetzt, die direkt das Gesicht des Schauspielers filmt und Mikroausdrucke in Echtzeit digitalisiert."
    ),

    // ── FILM TECHNOLOGY: DE-AGING CGI ────────────────────────────────────────

    // Question 9 – De-aging CGI: The Irishman
    Question(
        categoryId = 4,
        questionText = "In welchem Film von 2019 wurden Robert De Niro und Al Pacino per De-Aging-CGI auf ihr juengeres Erscheinungsbild retuschiert?",
        answerA = "Once Upon a Time in Hollywood",
        answerB = "The Irishman",
        answerC = "Joker",
        answerD = "Marriage Story",
        correctAnswer = 1, // B
        explanation = "Martin Scorseses 'The Irishman' (2019) nutzte die 'de-aging'-Technologie des ILM-Systems 'Flux', um die Hauptdarsteller fuer Rueckblenden um Jahrzehnte juenger aussehen zu lassen.",
        difficulty = 2,
        funFact = "Die De-Aging-Technologie in The Irishman kostete angeblich rund 15 Millionen Dollar und erforderte spezielle Mehrfachkamera-Setups fuer jede Szene."
    ),

    // Question 10 – De-aging CGI: Star Wars Rogue One
    Question(
        categoryId = 4,
        questionText = "In 'Rogue One: A Star Wars Story' (2016) wurde eine Figur posthum per CGI eingesetzt. Welche Figur war das?",
        answerA = "Han Solo",
        answerB = "Darth Vader",
        answerC = "Grand Moff Tarkin (Peter Cushing)",
        answerD = "Obi-Wan Kenobi",
        correctAnswer = 2, // C
        explanation = "Peter Cushing, der Grand Moff Tarkin spielte, war 1994 verstorben. In 'Rogue One' wurde sein Gesicht per Digital-Double und Performance Capture vollstaendig digital rekonstruiert - was ethische Debatten ausloeste.",
        difficulty = 2,
        funFact = "Dieses Vorgehen loeste eine Debatte aus, ob posthume digitale Verwendung von Schauspieler-Abbildern ohne deren lebzeitliche Einwilligung ethisch vertretbar ist."
    ),

    // ── FAMOUS MOVIE STUNTS ───────────────────────────────────────────────────

    // Question 11 – Buster Keaton: Train stunt
    Question(
        categoryId = 4,
        questionText = "Buster Keaton liess in 'The General' (1926) ein echtes Dampfschiff von einer Bruecke in einen Fluss stuerzen. Was machte diesen Stunt einzigartig?",
        answerA = "Es war der erste computerunterstuetzte Filmstunt",
        answerB = "Es war der teuerste Stunt der Stummfilmaera und wurde nur einmal gedreht",
        answerC = "Keaton selbst saß im fahrenden Zug",
        answerD = "Es war der erste Stunt, bei dem ein Stuntman einen Schauspiele ersetzte",
        correctAnswer = 1, // B
        explanation = "Der Zugsturz in 'The General' war der teuerste Stunt der Stummfilmaera (ca. 42.000 Dollar allein fuer die Lokomotive) und konnte aus offensichtlichen Gruenden nur einmal gedreht werden.",
        difficulty = 2,
        funFact = "Die verungluckte Lokomotive blieb jahrzehntelang im Fluss liegen und wurde erst in den 1980er-Jahren geborgen - sie ist heute ein Touristenziel in Oregon."
    ),

    // Question 12 – Tom Cruise: Burj Khalifa stunt
    Question(
        categoryId = 4,
        questionText = "In 'Mission: Impossible - Phantom Protokoll' (2011) kletterte Tom Cruise an der Aussenfassade des Burj Khalifa. In welcher Stadt steht dieses Gebaeude?",
        answerA = "Abu Dhabi",
        answerB = "Riad",
        answerC = "Dubai",
        answerD = "Doha",
        correctAnswer = 2, // C
        explanation = "Der Burj Khalifa steht in Dubai (VAE) und ist mit 828 Metern das hoechste Gebaeude der Welt. Tom Cruise fuehlte den Stunt persoenlich an der Aussenfassade in ueber 130 Metern Hoehe durch.",
        difficulty = 2,
        funFact = "Tom Cruise soll auf dem Set gesagt haben, er koennte den Stunt ewig wiederholen - die Crew war deutlich nervoeser als er selbst."
    ),

    // Question 13 – Jackie Chan: Clock Tower stunt
    Question(
        categoryId = 4,
        questionText = "In welchem Film haengt Jackie Chan in einem beruehmt-beruechtigten Stunt an einem riesigen Uhrturm-Uhrzeiger?",
        answerA = "Rumble in the Bronx",
        answerB = "Project A",
        answerC = "Police Story",
        answerD = "Shanghai Knights",
        correctAnswer = 1, // B
        explanation = "In 'Project A' (1983) haengt Jackie Chan am Uhrzeiger eines grossen Uhrenturms und laesst sich dann fallen - eine direkte Hommage an Harold Lloyd in 'Safety Last!' (1923).",
        difficulty = 2,
        funFact = "Jackie Chan verletzte sich beim Dreh von 'Project A' mehrfach ernsthaft, zog sich aber durch, da Wiederholungen gefaehrliche Stunts noch gefaehrlicher machen."
    ),

    // Question 14 – Stunt: Mad Max Fury Road
    Question(
        categoryId = 4,
        questionText = "Was war das besondere an den Stunts und Action-Sequenzen in 'Mad Max: Fury Road' (2015) im Vergleich zu anderen Blockbustern?",
        answerA = "Alle Explosionen wurden digital am Computer erstellt",
        answerB = "Fast alle Stunts und Explosionen wurden praktisch in der Realitaet gedreht, CGI nur minimal eingesetzt",
        answerC = "Stuntleute wurden durch Robotik-Systeme vollstaendig ersetzt",
        answerD = "Der gesamte Film wurde ohne Stuntleute gedreht",
        correctAnswer = 1, // B
        explanation = "Regisseur George Miller bestand darauf, dass fast alle Fahrzeugaktionen, Explosionen und Stunts real gedreht wurden. CGI wurde nur zurueckhaltend eingesetzt - ein bewusster Gegenentwurf zur damaligen CGI-Schwemme.",
        difficulty = 2,
        funFact = "Fuer Mad Max: Fury Road wurden ueber 150 echte Fahrzeuge gebaut und viele davon auch wirklich zerstoert. Der Film wurde ueber 17 Jahre lang entwickelt."
    ),

    // ── BEHIND-THE-SCENES FACTS ───────────────────────────────────────────────

    // Question 15 – Jurassic Park: Practical effects
    Question(
        categoryId = 4,
        questionText = "Bei den Dreharbeiten zu 'Jurassic Park' (1993) wurden neben CGI auch aufwaendige animatronische Dinosaurier gebaut. Welches Studio baute diese mechanischen Modelle?",
        answerA = "Rick Baker Studios",
        answerB = "Stan Winston Studio",
        answerC = "Jim Henson's Creature Shop",
        answerD = "KNB EFX Group",
        correctAnswer = 1, // B
        explanation = "Stan Winston Studio baute lebensgrosse animatronische Dinosaurier-Modelle fuer Jurassic Park, darunter einen voll funktionsfaehigen Triceratops und Teile des T-Rex. Die Kombination mit ILM-CGI revolutionierte den Blockbuster.",
        difficulty = 2,
        funFact = "Der kranke Triceratops in Jurassic Park war ein real gebautes Modell und benoetigte ein Team von 50 Leuten, die es per Fernsteuerung in Bewegung versetzten."
    ),

    // Question 16 – Apocalypse Now: Production chaos
    Question(
        categoryId = 4,
        questionText = "Die Produktion von Francis Ford Coppolas 'Apocalypse Now' (1979) gilt als eine der chaotischsten Hollywoodgeschichten. Was passierte am Set auf den Philippinen?",
        answerA = "Das Hauptstudio brannte vollstaendig nieder",
        answerB = "Ein Taifun zerstoerte grosse Teile des Sets und Martin Sheen erlitt einen Herzinfarkt",
        answerC = "Alle fuenf Hauptdarsteller verliessen gleichzeitig das Set",
        answerD = "Die Philippinische Armee beschlagnahmte alle Filmkameras",
        correctAnswer = 1, // B
        explanation = "Ein Taifun zerstoerte grosse Teile des am Ort gebauten Sets und erforderte einen wochenlangen Neuaufbau. Zudem erlitt Hauptdarsteller Martin Sheen waehrend der Dreharbeiten einen echten Herzinfarkt.",
        difficulty = 2,
        funFact = "Regisseur Coppola war dem Nervenzusammenbruch nahe und lieh selbst Geld, um die Produktion zu retten. Der Film lief schlussendlich 16 Monate lang auf den Philippinen."
    ),

    // Question 17 – The Dark Knight: Heath Ledger preparation
    Question(
        categoryId = 4,
        questionText = "Welche ungewoehnliche Vorbereitungsmethode nutzte Heath Ledger fuer seine Joker-Darstellung in 'The Dark Knight' (2008)?",
        answerA = "Er liess sich in eine psychiatrische Klinik einweisen",
        answerB = "Er isolierte sich 6 Wochen lang in einem Hotelzimmer und fuehrte ein Joker-Tagebuch",
        answerC = "Er sprach nie ausserhalb des Sets ohne Joker-Schminke",
        answerD = "Er verbrachte drei Monate mit echten Strassenkriminellen",
        correctAnswer = 1, // B
        explanation = "Heath Ledger isolierte sich etwa sechs Wochen in einem Londoner Hotelzimmer, um die Psyche des Jokers zu entwickeln. Er fuehrte dabei ein detailliertes Tagebuch aus der Perspektive der Figur.",
        difficulty = 2,
        funFact = "Ledgers Joker-Tagebuch enthielt unter anderem Fotos, Gedanken und Ideen fuer Momente im Film. Es wurde nach seinem Tod seiner Familie uebergeben."
    ),

    // ── FILM FINANCING & PRODUCTION BUDGETS ──────────────────────────────────

    // Question 18 – Blair Witch: Low budget success
    Question(
        categoryId = 4,
        questionText = "Das Found-Footage-Horrorfilm-Phaenomen 'Blair Witch Project' (1999) hatte ein minimales Budget. Wie hoch war es ungefaehr?",
        answerA = "Ca. 500.000 Dollar",
        answerB = "Ca. 60.000 Dollar",
        answerC = "Ca. 2 Millionen Dollar",
        answerD = "Ca. 200.000 Dollar",
        correctAnswer = 1, // B
        explanation = "The Blair Witch Project wurde mit einem Budget von nur ca. 60.000 Dollar produziert und spielte weltweit ueber 248 Millionen Dollar ein - eine der spektakulaersten Renditen der Filmgeschichte.",
        difficulty = 2,
        funFact = "Die Darsteller wussten kaum, was waehrend des Drehs passieren wuerde - Regisseure hinterliessen taeglich Hinweise im Wald und erzeugten echte Angst fuer die Kamera."
    ),

    // Question 19 – Most expensive film ever
    Question(
        categoryId = 4,
        questionText = "Welcher Film galt lange als der teuerste in der Filmgeschichte, mit einem Produktionsbudget von ca. 300 Millionen Dollar?",
        answerA = "Avengers: Endgame",
        answerB = "Pirates of the Caribbean: Am Ende der Welt",
        answerC = "Avatar",
        answerD = "Transformers: The Last Knight",
        correctAnswer = 1, // B
        explanation = "'Pirates of the Caribbean: Am Ende der Welt' (2007) hatte ein Budget von ca. 300 Millionen Dollar und war damit jahrelang der teuerste Einzelfilm der Filmgeschichte. 'Avengers: Endgame' ueberholte ihn spaeter.",
        difficulty = 2,
        funFact = "Disney/Bruckheimer drehten Teil 2 und Teil 3 der Pirates-Reihe gleichzeitig in 500 Drehtagen, was die Kosten auf beide Produktionen verteilte."
    ),

    // Question 20 – Tax incentives in film
    Question(
        categoryId = 4,
        questionText = "Warum werden viele grosse Hollywood-Produktionen heute in Laendern wie Neuseeland, Kanada oder Grossbritannien gedreht statt in den USA?",
        answerA = "Weil die dortigen Schauspieler besser ausgebildet sind",
        answerB = "Wegen grosszuegiger staatlicher Steuerrueckerstattungen und Produktionszuschlaegen",
        answerC = "Weil das Wetter besser und kalkulierbarer ist",
        answerD = "Wegen guenstigerer Kamera- und Technikausruestung",
        correctAnswer = 1, // B
        explanation = "Viele Laender bieten erhebliche Steueranreize (Tax Credits) fuer Filmproduktionen, die vor Ort drehen. Kanada zum Beispiel erstattet bis zu 30% der lokalen Produktionskosten - ein entscheidender Kostenfaktor bei Budgets von hunderten Millionen.",
        difficulty = 2,
        funFact = "Neuseeland bot Peter Jackson fuer die Herr-der-Ringe-Trilogie nicht nur landschaftliche Kulissen, sondern auch erhebliche steuerliche Vorteile, die die Produktionskosten senkten."
    ),

    // ── VOICE ACTING: GERMAN DUB ACTORS ──────────────────────────────────────

    // Question 21 – Thomas Fritsch: James Bond
    Question(
        categoryId = 4,
        questionText = "Welcher deutschen Synchronsprecher lieh Sean Connery in den fruehen James-Bond-Filmen seine Stimme?",
        answerA = "Klaus Kindler",
        answerB = "Thomas Fritsch",
        answerC = "Rolf Schult",
        answerD = "Gert Gunther Hoffmann",
        correctAnswer = 1, // B
        explanation = "Thomas Fritsch synchronisierte Sean Connery als James Bond in den ersten Bond-Filmen der 1960er-Jahre und praegt damit das deutsche Bond-Bild einer ganzen Generation.",
        difficulty = 2,
        funFact = "Thomas Fritsch war nicht nur als Synchronsprecher taetig, sondern auch ein bekannter Schauspieler, Saenger und Moderator im deutschen Fernsehen."
    ),

    // Question 22 – German dub: Arnold Schwarzenegger
    Question(
        categoryId = 4,
        questionText = "Welcher langjaerige deutsche Synchronsprecher ist als 'die Stimme von Arnold Schwarzenegger' bekannt?",
        answerA = "Gerhard Jilka",
        answerB = "Klaus Kindler",
        answerC = "Helmut Zierl",
        answerD = "Thomas Nero Wolff",
        correctAnswer = 1, // B
        explanation = "Klaus Kindler synchronisierte Arnold Schwarzenegger ueber viele Jahre hinweg in Deutschland und ist eng mit dessen Stimmbild im deutschen Fernsehen und Kino verbunden.",
        difficulty = 2,
        funFact = "Deutsche Synchronschauspieler sind oft fuer ihre Stammschauspieler bekannt - wenn ein Stammschauspieler stirbt, wird manchmal dessen Synchronsprecher ebenfalls fuer aeltere Rollen weiter eingesetzt."
    ),

    // Question 23 – German dub tradition
    Question(
        categoryId = 4,
        questionText = "Deutschland gilt weltweit als eines der wenigen Laender mit einer professionellen Vollsynchronisations-Kultur. Wie nennt man den Prozess des Synchronisierens in der Fachsprache?",
        answerA = "Lip-Sync-Recording",
        answerB = "Postsynchronisation (Dubbing)",
        answerC = "Overdubbing",
        answerD = "Audio-Description",
        correctAnswer = 1, // B
        explanation = "Die professionelle Vollsynchronisation (Postsynchronisation oder Dubbing) ist in Deutschland, Frankreich, Italien und Spanien weit verbreitet. Andere Laender wie Skandinavien oder die Niederlande bevorzugen Untertitel.",
        difficulty = 2,
        funFact = "Die erste vollstaendig synchronisierte deutsche Fassung eines amerikanischen Films war 'Der blaue Engel' (1930) - Marlene Dietrich sprach ihre eigene Rolle sowohl auf Deutsch als auch auf Englisch ein."
    ),

    // Question 24 – German dub: Homer Simpson
    Question(
        categoryId = 4,
        questionText = "Welcher deutsche Synchronsprecher spricht Homer Simpson in der deutschen Version der Simpsons?",
        answerA = "Manfred Erdmann",
        answerB = "Norbert Gastell",
        answerC = "Arne Elsholtz",
        answerD = "Roland Hemmo",
        correctAnswer = 1, // B
        explanation = "Norbert Gastell ist die Stimme von Homer Simpson in der deutschen Synchronisation und hat diese Rolle seit der ersten Staffel inne. Er ist damit eine Institution im deutschen Hoerspiel.",
        difficulty = 2,
        funFact = "Norbert Gastell hat Homer Simpson in ueber 700 Episoden synchronisiert und gilt damit als einer der am meisten beschaeftigten deutschen Synchronsprecher."
    ),

    // ── VIDEO GAME FILM ADAPTATIONS ───────────────────────────────────────────

    // Question 25 – Mortal Kombat 1995
    Question(
        categoryId = 4,
        questionText = "Die Verfilmung von 'Mortal Kombat' (1995) gilt als eine der besseren Videospiel-Adaptionen. Wer komponierte den ikonischen elektronischen Soundtrack?",
        answerA = "Hans Zimmer",
        answerB = "The Immortals (KMFDM-Kollaboration)",
        answerC = "Danny Elfman",
        answerD = "Junkie XL",
        correctAnswer = 1, // B
        explanation = "Den Mortal-Kombat-Soundtrack komponierte 'The Immortals', eine Kollaboration unter massgeblicher Beteiligung von KMFDM-Mitglied Sascha Konietzko. Der Titelsong 'Techno Syndrome (Mortal Kombat)' wurde zum Kult.",
        difficulty = 2,
        funFact = "Das ikonische 'Mortal Kombat!'-Rufen im Titelsong wurde von zwei Freunden des Produzenten eingesprochen, die spontan ins Studio kamen."
    ),

    // Question 26 – Super Mario Bros. 1993 flop
    Question(
        categoryId = 4,
        questionText = "Warum gilt der Film 'Super Mario Bros.' (1993) mit Bob Hoskins und Dennis Hopper als einer der groessten Videospiel-Verfilmungs-Flops?",
        answerA = "Der Film wurde nie fertiggestellt und nach 3 Drehtagen abgebrochen",
        answerB = "Er nahm ein dystopisches Setting an statt der farbenfrohen Spielwelt und scheiterte an den Kinokassen",
        answerC = "Nintendo verbat die Veroeffentlichung, und er erschien nur auf VHS",
        answerD = "Alle Schauspieler lehnten den Film nachtraeglich ab und forderten ihre Namen zu entfernen",
        correctAnswer = 1, // B
        explanation = "Der Super-Mario-Film ignorierte das farbenfrohe Spielkonzept und setzte auf eine duestre, dystopische Aesthetik mit Dinosaurieren als evolutionaere Nachfolger der Menschen. Das Publikum war verwirrt und der Film floppte.",
        difficulty = 2,
        funFact = "Bob Hoskins sagte spater in einem Interview, er haette nicht gewusst, was Super Mario ist, und nannte den Film seine schlechteste Arbeit."
    ),

    // Question 27 – Resident Evil film series
    Question(
        categoryId = 4,
        questionText = "Wer regissierte die erste Verfilmung von 'Resident Evil' (2002) und war massgeblich fuer die gesamte Filmreihe verantwortlich?",
        answerA = "Uwe Boll",
        answerB = "Paul W.S. Anderson",
        answerC = "Michael Bay",
        answerD = "Brett Ratner",
        correctAnswer = 1, // B
        explanation = "Paul W.S. Anderson schrieb und regissierte den ersten Resident-Evil-Film und blieb der Reihe als Autor und Produzent treu. Seine Ehefrau Milla Jovovich spielte die Hauptrolle Alice in allen sechs Teilen.",
        difficulty = 2,
        funFact = "Die Resident-Evil-Filmreihe ist trotz schlechter Kritiken eine der erfolgreichsten Videospiel-Adaptionen: Alle sechs Teile zusammen spielten ueber 1,2 Milliarden Dollar ein."
    ),

    // ── COMIC BOOK ADAPTATIONS BEYOND MARVEL/DC ───────────────────────────────

    // Question 28 – Judge Dredd / Dredd
    Question(
        categoryId = 4,
        questionText = "Aus welchem britischen Comicmagazin stammt die Figur Judge Dredd, die zweimal verfilmt wurde (1995 und 2012)?",
        answerA = "Beano",
        answerB = "2000 AD",
        answerC = "Viz",
        answerD = "Deadline",
        correctAnswer = 1, // B
        explanation = "Judge Dredd erschien erstmals 1977 im britischen Comicmagazin '2000 AD'. Die Figur des Polizeirichters in einer dystopischen Megastadt ist ein Klassiker der britischen Comictradition.",
        difficulty = 2,
        funFact = "Sylvester Stallone weigerte sich im Film von 1995, den Helm zu tragen - obwohl Judge Dredd in den Comics niemals sein Gesicht zeigt. Karl Urban korrigierte dies im 2012er-Film bewusst."
    ),

    // Question 29 – The Mask: Dark Horse Comics
    Question(
        categoryId = 4,
        questionText = "Der Film 'The Mask' (1994) mit Jim Carrey basierte auf einer Comicreihe. Von welchem Verlag stammt dieser Comic?",
        answerA = "DC Comics",
        answerB = "Image Comics",
        answerC = "Dark Horse Comics",
        answerD = "Valiant Comics",
        correctAnswer = 2, // C
        explanation = "The Mask (im Original: 'The Mask') erschien ab 1991 bei Dark Horse Comics. Die Originalcomics sind jedoch deutlich blutiger und dunkler als der Familienfilm mit Jim Carrey.",
        difficulty = 2,
        funFact = "In den Originalcomics ist die Maske deutlich gefaehrlicher und verwandelt Trager in unkontrollierte, gewalttaetige Killer - nicht in eine spassige Figur."
    ),

    // Question 30 – Sin City: Frank Miller adaptation
    Question(
        categoryId = 4,
        questionText = "Robert Rodriguez' Film 'Sin City' (2005) war eine ungewoehnliche Adaptation. Was machte die visuelle Umsetzung besonders?",
        answerA = "Der Film wurde vollstaendig in Stop-Motion-Animation gedreht",
        answerB = "Schwarzweiss mit selektiven Farbakzenten, direkt aus Frank Millers Comicpanels uebersetzt",
        answerC = "Es wurden Rotoskopie-Verfahren wie in 'Waking Life' verwendet",
        answerD = "Der gesamte Film wurde am Greenscreen gedreht und spaeter digital eingefaerbt",
        correctAnswer = 1, // B
        explanation = "Sin City uebersetzte Frank Millers Noir-Comics nahezu panel-fuer-panel in Spielfilm-Schwarzweiss mit gezielten Farbakzenten (z.B. rote Lippen, gelbe Figur). Robert Rodriguez entwickelte diesen 'digitalen Backlot'-Stil gemeinsam mit Frank Miller als Co-Regisseur.",
        difficulty = 2,
        funFact = "Frank Miller war so begeistert von Rodriguez' Herangehensweise, dass er auf ein normales Honorar verzichtete und stattdessen als Co-Regisseur genannt werden wollte - was die Directors Guild of America fast verhinderte."
    ),

    // Question 31 – Asterix films
    Question(
        categoryId = 4,
        questionText = "Die Realkino-Asterix-Filme (ab 1999) wurden in Frankreich produziert. Wer spielte Asterix im ersten Realfilm 'Asterix & Obelix gegen Caesar'?",
        answerA = "Depardieu",
        answerB = "Gerard Jugnot",
        answerC = "Christian Clavier",
        answerD = "Jean Reno",
        correctAnswer = 2, // C
        explanation = "Christian Clavier spielte Asterix und Gerard Depardieu spielte Obelix im ersten Realfilm 'Asterix & Obelix gegen Caesar' (1999). Beide kehrten im zweiten Teil 'Asterix & Obelix: Mission Kleopatra' (2002) zurueck.",
        difficulty = 2,
        funFact = "Der Asterix-Realfilm von 2002 ('Mission Kleopatra') war der meistgesehene Kinofilm Frankreichs seit Jahren und ist bis heute eine der erfolgreichsten franzoesischen Produktionen."
    ),

    // ── DOCUMENTARY FILMMAKERS ────────────────────────────────────────────────

    // Question 32 – Michael Moore: Roger & Me
    Question(
        categoryId = 4,
        questionText = "In Michael Moores Debuetdokumentarfilm 'Roger & Me' (1989) versuchte er vergeblich, einen Interview-Termin zu bekommen. Mit wem?",
        answerA = "US-Praesident Ronald Reagan",
        answerB = "Roger Smith, dem GM-CEO, der die Fabrik in Flint schloss",
        answerC = "Roger Ebert, dem Filmkritiker",
        answerD = "Roger Waters von Pink Floyd",
        correctAnswer = 1, // B
        explanation = "Moore versuchte in 'Roger & Me', den General-Motors-Chef Roger Smith zu konfrontieren, der durch die Schliessung der Flint-Fabrik 30.000 Menschen arbeitslos gemacht hatte. Der Film praegt das Genre des politischen Dokumentarfilms bis heute.",
        difficulty = 2,
        funFact = "Michael Moore war selbst in Flint aufgewachsen und finanzierte den Film unter anderem durch Bingo-Gewinne und Zuschausse von Freunden."
    ),

    // Question 33 – Michael Moore: Fahrenheit 9/11
    Question(
        categoryId = 4,
        questionText = "Welche Auszeichnung erhielt Michael Moores 'Fahrenheit 9/11' (2004) beim Filmfestival in Cannes?",
        answerA = "Sonderpreis der Jury",
        answerB = "Beste Regie",
        answerC = "Goldene Palme (Palme d'Or)",
        answerD = "Grosser Preis der Jury",
        correctAnswer = 2, // C
        explanation = "'Fahrenheit 9/11', Moores Dokumentarfilm ueber George W. Bush und den Irak-Krieg, gewann 2004 die Goldene Palme in Cannes - das erste Mal, dass ein Dokumentarfilm diesen Preis erhielt.",
        difficulty = 2,
        funFact = "Fahrenheit 9/11 ist bis heute der kommerziell erfolgreichste Dokumentarfilm aller Zeiten mit einem Einspielergebnis von ueber 222 Millionen Dollar."
    ),

    // Question 34 – Werner Herzog: Fitzcarraldo
    Question(
        categoryId = 4,
        questionText = "Wofuer sind die Dreharbeiten zu Werner Herzogs 'Fitzcarraldo' (1982) in der Filmwelt legendaer?",
        answerA = "Herzog drehte vollstaendig unterwasser ohne Pressluftgeraet",
        answerB = "Ein echtes Dampfschiff wurde ueber einen Berghang geschleppt, ohne CGI oder Modell",
        answerC = "Der Film wurde ohne Drehbuch im Amazonas improvisieret",
        answerD = "Alle 200 Komparsen mussten die reale Strecke des Films zu Fuss absolvieren",
        correctAnswer = 1, // B
        explanation = "Fuer 'Fitzcarraldo' liess Werner Herzog tatsaechlich ein schweres Dampfschiff ueber einen Berghang im peruanischen Dschungel ziehen - ohne CGI, ohne Modell. Die chaotischen Dreharbeiten wurden in Herzogs eigenem Dokumentarfilm 'Burden of Dreams' festgehalten.",
        difficulty = 2,
        funFact = "Urspruenglicher Hauptdarsteller war Jason Robards, der nach Wochen abbrechen musste. Klaus Kinski uebernahm die Rolle, was die Arbeitsatmosphaere noch intensiver machte."
    ),

    // Question 35 – Werner Herzog: Grizzly Man
    Question(
        categoryId = 4,
        questionText = "Herzogs Dokumentarfilm 'Grizzly Man' (2005) handelt von Timothy Treadwell. Was passierte Treadwell?",
        answerA = "Er wurde von einem Eisbaren in Alaska toedlich verletzt",
        answerB = "Er und seine Freundin wurden von einem Grizzlybaer in Alaska getoetet",
        answerC = "Er starb an einem Herzinfarkt waehrend einer Baeren-Begegnung",
        answerD = "Er verschwand spurlos im Yellowstone-Nationalpark",
        correctAnswer = 1, // B
        explanation = "Timothy Treadwell lebte 13 Sommer lang in Alaska unter Grizzlybaeren und filmte sich selbst dabei. Im Jahr 2003 wurde er zusammen mit seiner Freundin Amie Huguenard von einem Baer getoetet.",
        difficulty = 2,
        funFact = "Die Tonaufnahme des Angriffs existiert tatsaechlich, da die Kamera lief aber die Linse abgedeckt war. Herzog hoerte die Aufnahme und beschloss sie im Film nicht abzuspielen."
    ),

    // Question 36 – David Attenborough: Blue Planet
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr erschien David Attenboroughs BBC-Dokumentarserie 'The Blue Planet' (Blue Planet), die erstmals Tiefseelebewesen in groesserer Breite zeigte?",
        answerA = "1995",
        answerB = "2001",
        answerC = "2007",
        answerD = "2014",
        correctAnswer = 1, // B
        explanation = "'The Blue Planet' erschien 2001 und zeigte mit damals revolutionaerer Unterwasser-Kameratechnik Tiefseebewohner, die in Dokumentationen vorher kaum zu sehen waren. Die Serie hatte enormen Einfluss auf das Umweltbewusstsein.",
        difficulty = 2,
        funFact = "Fuer 'Blue Planet II' (2017) wurden neue U-Boot-Kameras verwendet, die bis zu 1000 Meter Tiefe filmen konnten - Lebewesen, die vorher niemals gefilmt worden waren."
    ),

    // Question 37 – David Attenborough: Frozen Planet
    Question(
        categoryId = 4,
        questionText = "Welche Kontroverse umgab die US-Ausstrahlung von David Attenboroughs Serie 'Frozen Planet' (2011)?",
        answerA = "Die US-Sendeanstalt Discovery kuerzte alle Szenen mit sterbenden Tieren",
        answerB = "Die US-Ausstrahlung liess die letzte Folge ueber den Klimawandel weg",
        answerC = "Attenborough wurde nachtraeglich durch einen amerikanischen Sprecher ersetzt",
        answerD = "Die Eisbaerszenen wurden als Tierschutzverletzung kritisiert",
        correctAnswer = 1, // B
        explanation = "Die US-Ausstrahlung auf Discovery Channel liess die finale Episode von 'Frozen Planet' aus, die explizit den vom Menschen verursachten Klimawandel thematisiert - vermutlich aus Ruecksicht auf ein konservatives Publikumssegment.",
        difficulty = 2,
        funFact = "Attenborough kritisierte diese Entscheidung oeffentlich und betonte, dass der Klimawandel eine wissenschaftliche Tatsache sei und nicht weggelassen werden durfte."
    ),

    // ── ADDITIONAL TOPICS: MIXED ──────────────────────────────────────────────

    // Question 38 – Uwe Boll: Video game adaptations
    Question(
        categoryId = 4,
        questionText = "Der deutsche Regisseur Uwe Boll ist beruehmt-beruechtig fuer seine zahlreichen Videospiel-Verfilmungen. Welche steuerliche Regelung ermoeglichte ihm die Finanzierung vieler dieser Projekte?",
        answerA = "EU-Foerdergelder fuer deutsch-kanadische Koproduktionen",
        answerB = "Deutsche Steuergesetze, die Verluste aus Filmproduktionen absetzbar machten",
        answerC = "Subventionen des Bundesfilmfoerderungsfonds fuer Genrefilme",
        answerD = "Kanadas Film-Tax-Credit-Programm fuer internationale Produktionen",
        correctAnswer = 1, // B
        explanation = "Deutsche Steuergesetze bis in die 2000er-Jahre erlaubten es Investoren, Verluste aus Filmproduktionen steuerlich abzusetzen. Boll nutzte dieses System geschickt, um Investor-Kapital anzuziehen - unabhaengig vom Kassenerfolg seiner Filme.",
        difficulty = 2,
        funFact = "Als das Steuergesetz geaendert wurde, versiegte Bolls Finanzierungsquelle und sein Output sank drastisch. Er gab 2016 offiziell das Filmemachen auf."
    ),

    // Question 39 – Comic: Hellboy adaptations
    Question(
        categoryId = 4,
        questionText = "Wer schuf die Comicreihe 'Hellboy', die zweimal verfilmt wurde (2004/2008 von Guillermo del Toro und 2019)?",
        answerA = "Frank Miller",
        answerB = "Mike Mignola",
        answerC = "Alan Moore",
        answerD = "Brian Michael Bendis",
        correctAnswer = 1, // B
        explanation = "Mike Mignola schuf Hellboy 1993 fuer Dark Horse Comics. Die Figur des halbaemonie Daemons, der auf der Seite des Guten kaempft, ist eine der originellsten Comic-Kreationen ausserhalb der grossen Verlage.",
        difficulty = 2,
        funFact = "Guillermo del Toro und Mike Mignola arbeiteten sehr eng zusammen: Mignola bewundert del Toros Verstaendnis fuer seine Figur, das er in vielen Interviews betonte."
    ),

    // Question 40 – IMAX: Technical development
    Question(
        categoryId = 4,
        questionText = "IMAX-Filmkameras nutzen ein besonders grosses Filmformat. Wie gross ist ein IMAX-Einzelbild im Vergleich zu einem Standard-35mm-Filmbild?",
        answerA = "Doppelt so gross",
        answerB = "Viermal so gross",
        answerC = "Zehnmal so gross",
        answerD = "Etwa fuenfzehnmal so gross",
        correctAnswer = 2, // C
        explanation = "Ein IMAX-Einzelbild auf 70mm-Film ist etwa zehnmal groesser als ein Standard-35mm-Bild. Das ergibt enorm hohe Bilddetails bei Projektion auf grossen Leinwaenden.",
        difficulty = 2,
        funFact = "Wegen der enormen Bildgroesse und des lauten Kameramotors koennen Schauspieler waehrend IMAX-Aufnahmen keine normalen Dialoge fuehren - der Ton wird getrennt aufgenommen."
    ),

    // Question 41 – Spirited Away: Oscar history
    Question(
        categoryId = 4,
        questionText = "Welche besondere Premiere feierte Hayao Miyazakis 'Chihiros Reise ins Zauberland' (2003) bei der Oscar-Verleihung?",
        answerA = "Erster Film, der den Oscar ohne englische Premiere gewann",
        answerB = "Erster nicht-englischsprachiger Film, der den Oscar fuer besten Animationsfilm gewann",
        answerC = "Erster Animationsfilm, der fuer den Oscar als bester Film nominiert wurde",
        answerD = "Erster japanischer Oscar-Gewinner in einer technischen Kategorie",
        correctAnswer = 1, // B
        explanation = "'Chihiros Reise ins Zauberland' gewann 2003 als erster nicht-englischsprachiger Film den Academy Award fuer den besten Animationsfilm. Miyazaki selbst kam nicht zur Verleihung aus Protest gegen den Irak-Krieg.",
        difficulty = 2,
        funFact = "Der Film war in Japan bereits 2001 erschienen und dort ein Kassenrekord - er lief bis Avatar (2009) als erfolgreichster Film in Japan aller Zeiten."
    ),

    // Question 42 – Motion capture: Avatar's development
    Question(
        categoryId = 4,
        questionText = "James Cameron entwickelte fuer 'Avatar' (2009) eine neue Form des Motion Capture. Was war das Wesentliche am sogenannten 'Performance-Capture'-Verfahren?",
        answerA = "Schauspieler mussten nicht mehr selbst am Set anwesend sein",
        answerB = "Neben Koerperbewegungen wurden auch Gesichtsbewegungen und Mimik hochpraezise erfasst",
        answerC = "Der Computer ersetzte die Schauspieler vollstaendig durch KI",
        answerD = "Das System arbeitete ohne Marker direkt ueber Magnetfelder",
        correctAnswer = 1, // B
        explanation = "Camerons Performance-Capture-System erfasste nicht nur Koerperbewegungen, sondern auch hochpraezise Gesichtsbewegungen und Mimik. Die Schauspieler trugen Helmkameras, die ihre Gesichter in Echtzeit aufnahmen.",
        difficulty = 2,
        funFact = "Cameron entwickelte fuer Avatar auch eine neue Kameratechnik, die ihm erlaubte, die virtuellen Avatar-Figuren quasi 'live' auf einem Monitor zu sehen waehrend er Szenen drehte."
    ),

    // Question 43 – Tarantino: Reservoir Dogs low budget
    Question(
        categoryId = 4,
        questionText = "Quentin Tarantinos Debuetfilm 'Reservoir Dogs' (1992) wurde mit einem sehr kleinen Budget gedreht. Wie hoch war es ungefaehr?",
        answerA = "Ca. 30.000 Dollar",
        answerB = "Ca. 1,5 Millionen Dollar",
        answerC = "Ca. 5 Millionen Dollar",
        answerD = "Ca. 500.000 Dollar",
        correctAnswer = 1, // B
        explanation = "Reservoir Dogs wurde mit einem Budget von ca. 1,2 bis 1,5 Millionen Dollar produziert und feierte auf dem Sundance Film Festival grosse Kritikererfolge. Der Film machte Tarantino schlagartig bekannt.",
        difficulty = 2,
        funFact = "Tarantino arbeitete urspruenglich als Videothekenkassierer und finanzierte sein fruehs Filmprojekt 'My Best Friend's Birthday' mit dem Verkauf eines Drehbuchs."
    ),

    // Question 44 – Documentary: March of the Penguins
    Question(
        categoryId = 4,
        questionText = "Welche Drehbedingungen machten die Produktion des Dokumentarfilms 'Die Reise der Pinguine' (2005) besonders herausfordernd?",
        answerA = "Die Kameras wurden unter Wasser auf dem Meeresboden aufgestellt",
        answerB = "Das Filmteam arbeitete bei Temperaturen bis minus 60 Grad in der Antarktis",
        answerC = "Die Dreharbeiten fanden ausschliesslich nachts waehrend der Polarnacht statt",
        answerD = "Das Team musste monatelang im Taucheranzug unter Eis filmen",
        correctAnswer = 1, // B
        explanation = "Das Filmteam verbrachte 13 Monate in der Antarktis und filmte bei Temperaturen von bis zu minus 60 Grad Celsius. Spezielle Kameragehaeuse mussten entwickelt werden, damit die Ausruestung nicht einfror.",
        difficulty = 2,
        funFact = "Fuer die franzosische Originalversion wurden die Pinguine mit Vor- und Nachnamen bezeichnet und als Charaktere mit Handlung erzhlt - ein Ansatz, den die amerikanische Version mit Morgan Freeman als Erzhler aenderte."
    ),

    // Question 45 – Comic: V for Vendetta
    Question(
        categoryId = 4,
        questionText = "Die Graphic Novel 'V for Vendetta', die 2005 verfilmt wurde, stammt von welchem Autor?",
        answerA = "Frank Miller",
        answerB = "Neil Gaiman",
        answerC = "Alan Moore",
        answerD = "Grant Morrison",
        correctAnswer = 2, // C
        explanation = "Alan Moore schrieb 'V for Vendetta' in den 1980er Jahren als Kommentar auf die konservative Politik unter Margaret Thatcher in Grossbritannien. Wie bei vielen seiner Verfilmungen (Watchmen, From Hell) distanzierte er sich oeffentlich vom Film.",
        difficulty = 2,
        funFact = "Die Guy-Fawkes-Maske aus dem Film wurde zum weltweiten Symbol fuer Protestbewegungen wie Anonymous - Alan Moore kommentierte das ironisch und genussreich."
    ),

    // Question 46 – Behind the scenes: Forrest Gump technology
    Question(
        categoryId = 4,
        questionText = "Welche bahnbrechende CGI-Technik wurde in 'Forrest Gump' (1994) verwendet, um Tom Hanks in historisches Archivmaterial einzubauen?",
        answerA = "Rotoskopie nach dem klassischen Disney-Verfahren",
        answerB = "Digitales Compositing, das Forrest nahtlos in echtes Nachrichtenmaterial einfugte",
        answerC = "Ein 3D-Modell von Tom Hanks wurde in die historischen Aufnahmen eingebettet",
        answerD = "Nachgedrehte Archivsequenzen mit exakten historischen Kostumen",
        correctAnswer = 1, // B
        explanation = "ILM nutzte digitales Compositing, um Tom Hanks als Forrest Gump nahtlos in echte historische Archivaufnahmen mit Praesidenten, Buergerrechtlern und Sportereignissen einzubauen - fuer 1994 eine revolutionaere Technik.",
        difficulty = 2,
        funFact = "Fuer die Szene mit JFK wurden die Originalaufnahmen analysiert, Koerperbewegung und Lichtverhaeltnisse des 'neuen' Forrest digital angepasst und der Mund von JFK per CGI neu animiert, damit er passende Saetze zu sagen scheint."
    ),

    // Question 47 – Comic: Tank Girl
    Question(
        categoryId = 4,
        questionText = "Der Film 'Tank Girl' (1995) basierte auf einem britischen Underground-Comic. In welchem Magazin erschien der Original-Comic zuerst?",
        answerA = "2000 AD",
        answerB = "Deadline",
        answerC = "Viz",
        answerD = "Heavy Metal",
        correctAnswer = 1, // B
        explanation = "Tank Girl erschien erstmals 1988 im britischen Underground-Musikmagazin 'Deadline', kreiert von Jamie Hewlett und Alan Martin. Jamie Hewlett wurde spaeter durch Gorillaz weltbekannt.",
        difficulty = 2,
        funFact = "Gorillaz-Mitgruender Jamie Hewlett, der Tank Girl zeichnete, sah den Film als Verrat an seiner Figur und bezeichnete ihn als Desaster."
    ),

    // Question 48 – Werner Herzog: Fitzcarraldo documentary
    Question(
        categoryId = 4,
        questionText = "Welcher Dokumentarfilm zeigt die chaotischen Dreharbeiten zu Herzogs 'Fitzcarraldo' und gilt selbst als Meisterwerk des Dokumentarfilms?",
        answerA = "My Best Fiend (Mein liebster Feind)",
        answerB = "Burden of Dreams",
        answerC = "Grizzly Man",
        answerD = "Into the Abyss",
        correctAnswer = 1, // B
        explanation = "'Burden of Dreams' (1982) von Les Blank dokumentierte die extremen Drehbedingungen von Herzogs 'Fitzcarraldo' im Amazonas. Der Dokumentarfilm gilt selbst als eines der faszinierendsten Metawerke ueber Filmproduktion.",
        difficulty = 2,
        funFact = "Herzogs beruehmt-beruechtigte Aussagen im Interview in 'Burden of Dreams' ('Die Natur ist kein Tempel, sie ist ein Abgrund voller Dummheit und Chaos') wurden zum Werbezitat fuer seinen Filmstil."
    ),

    // Question 49 – Technicolor: Decline
    Question(
        categoryId = 4,
        questionText = "Warum verdraeangten guenstigere Farbtechnologien wie Eastmancolor in den 1950ern das aufwaendige Technicolor-Verfahren?",
        answerA = "Eastmancolor war farbtreuer und zeigte keine Farbstreifen-Artefakte",
        answerB = "Technicolor-Kameras waren zu schwer und teuer, und ein einzelner Farbfilm war gegenueber drei Streifen deutlich guenstiger",
        answerC = "Technicolor wurde durch ein Patentrechtstreit verboten",
        answerD = "Eastmancolor lieferte hoeheren Kontrast fuer Schwarzweiss-Fernsehsendungen",
        correctAnswer = 1, // B
        explanation = "Das Drei-Streifen-Technicolor-Verfahren erforderte spezielle Kameras und das gleichzeitige Belichten von drei Filmstreifen - dreimal so viel Filmmaterial. Eastmancolor verwendete nur einen einzigen Farbfilm mit mehreren Schichten und war erheblich guenstiger.",
        difficulty = 2,
        funFact = "Paradoxerweise verblasst Eastmancolor-Material (Vinegar-Syndrome) deutlich schneller als alte Technicolor-Drucke, die dank des Drei-Streifen-Verfahrens noch heute in exzellenter Qualitaet erhalten sind."
    ),

    // Question 50 – De-aging and ethical debate
    Question(
        categoryId = 4,
        questionText = "Welches ethische Kernproblem wirft die posthume oder unverantwortliche Nutzung von De-Aging-CGI und digitalen Doppelaengerungen von Schauspielern auf?",
        answerA = "CGI-Darsteller verdraengen echte Schauspieler aus ihren Rollen",
        answerB = "Fehlende Einwilligung der Betroffenen und Verlust der Kontrolle ueber das eigene Abbild",
        answerC = "Studios muessen keine Gagen mehr zahlen und umgehen so Gewerkschaftsregeln",
        answerD = "Das Publikum kann nicht unterscheiden, ob Szenen echt oder digital sind",
        correctAnswer = 1, // B
        explanation = "Der Kern der ethischen Debatte ist das Recht auf das eigene Bild: Verstorbene Schauspieler koennen keine Einwilligung mehr geben, und auch lebende Darsteller verlieren potentiell die Kontrolle darueber, wie ihr Abbild in zukuenftigen Produktionen genutzt wird.",
        difficulty = 2,
        funFact = "Die Screen Actors Guild (SAG-AFTRA) hat in ihren Tarifvertraegen mittlerweile explizite Regelungen fuer den Einsatz digitaler Doppelaengerungen und KI-Stimmen aufgenommen."
    )

)
