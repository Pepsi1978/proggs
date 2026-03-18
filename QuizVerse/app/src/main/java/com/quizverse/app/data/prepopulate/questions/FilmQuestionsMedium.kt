package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsMedium(): List<Question> = listOf(

    // Question 1 - Oscar history: first ceremony
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr fand die allererste Oscar-Verleihung statt?",
        answerA = "1927",
        answerB = "1929",
        answerC = "1932",
        answerD = "1935",
        correctAnswer = 1, // B
        explanation = "Die erste Oscar-Verleihung fand am 16. Mai 1929 im Hollywood Roosevelt Hotel statt. Es war ein ruhiges Bankett fuer nur 270 Gaeste, und die Gewinner waren schon drei Monate vorher bekanntgegeben worden.",
        difficulty = 2,
        funFact = "Die erste Verleihung dauerte nur 15 Minuten. Erster Gewinner des Besten Films war 'Wings' von 1927/1928."
    ),

    // Question 2 - Oscar history: first host
    Question(
        categoryId = 4,
        questionText = "Wer moderierte die Oscar-Verleihung am haeufigsten in der Geschichte?",
        answerA = "Bob Hope",
        answerB = "Billy Crystal",
        answerC = "Johnny Carson",
        answerD = "Steve Martin",
        correctAnswer = 0, // A
        explanation = "Bob Hope moderierte die Oscar-Verleihung 18 Mal zwischen 1940 und 1978 und ist damit der haeufigste Moderator in der Geschichte der Academy Awards.",
        difficulty = 2,
        funFact = "Bob Hope moderierte so viele Oscar-Verleihungen, dass er von der Academy einen Ehrenoscar fuer seine Verdienste erhielt."
    ),

    // Question 3 - Oscar record: most wins
    Question(
        categoryId = 4,
        questionText = "Welcher Film haelt den Rekord fuer die meisten Oscar-Gewinne in einer einzigen Verleihung?",
        answerA = "Titanic (1997)",
        answerB = "La La Land (2016)",
        answerC = "Ben-Hur (1959)",
        answerD = "Alle drei: je 11 Oscars",
        correctAnswer = 3, // D
        explanation = "Ben-Hur (1959), Titanic (1997) und Die Rueckkehr des Koenigs (2003) gewannen alle je 11 Oscars in einer einzigen Verleihung und teilen damit den Rekord als Hoechstgewinner.",
        difficulty = 2,
        funFact = "Ben-Hur war 1960 der erste Film, der diesen Rekord aufstellte. Titanic brach ihn 1998 nicht, sondern egalisierte ihn."
    ),

    // Question 4 - Hitchcock
    Question(
        categoryId = 4,
        questionText = "In welchem Hitchcock-Film wird eine Frau unter der Dusche ermordet, bevor die eigentliche Geschichte beginnt?",
        answerA = "Vertigo",
        answerB = "Psycho",
        answerC = "Die Voegel",
        answerD = "Der fremde Fahrgast",
        correctAnswer = 1, // B
        explanation = "In 'Psycho' (1960) wird die Protagonistin Marion Crane in der beruehm ten Duschszene ermordet, obwohl der Zuschauer dachte, sie sei die Hauptfigur. Diese narrative Trickerei revolutionierte das Kino.",
        difficulty = 2,
        funFact = "Die Duschszene in Psycho dauert nur 45 Sekunden, besteht aber aus 70 verschiedenen Schnitten und 77 Kamerawinkeln."
    ),

    // Question 5 - Hitchcock film
    Question(
        categoryId = 4,
        questionText = "In welchem Hitchcock-Film leidet der Detektiv Scottie Ferguson an Hoehenangst?",
        answerA = "Rear Window",
        answerB = "North by Northwest",
        answerC = "Vertigo",
        answerD = "The Wrong Man",
        correctAnswer = 2, // C
        explanation = "In 'Vertigo' (1958) spielt James Stewart den Ex-Polizisten Scottie, der an Schwindel und Hoehenangst leidet. Der Film gilt heute als eines der groessten Meisterwerke der Filmgeschichte.",
        difficulty = 2,
        funFact = "Hitchcock selbst nannte Vertigo seinen persoenlichen Lieblingsfilm. Das British Film Institute kuerte ihn 2012 zum besten Film aller Zeiten."
    ),

    // Question 6 - Hitchcock MacGuffin
    Question(
        categoryId = 4,
        questionText = "Welchen Begriff praegte Alfred Hitchcock fuer ein Filmobjekt, das die Handlung antreibt, inhaltlich aber unwichtig ist?",
        answerA = "McGuffin",
        answerB = "Leitmotiv",
        answerC = "Plot Device",
        answerD = "Red Herring",
        correctAnswer = 0, // A
        explanation = "Der Begriff 'MacGuffin' wurde von Alfred Hitchcock popularisiert und bezeichnet ein Objekt oder Ziel, das fuer die Figuren im Film enorm wichtig erscheint, fuer den Zuschauer aber eigentlich bedeutungslos ist.",
        difficulty = 2,
        funFact = "Hitchcock erklaeerte den MacGuffin so: 'In einem Hitchcock-Film koennten es die Plaene fuer einen Flugzeugmotor sein, in einem anderen Einbruch-Plaene, es koennte alles sein.'"
    ),

    // Question 7 - Kubrick
    Question(
        categoryId = 4,
        questionText = "Welcher Stanley Kubrick Film basiert auf einem Roman von Anthony Burgess und zeigt eine dystopische Zukunft voller Gewalt?",
        answerA = "2001: Odyssee im Weltraum",
        answerB = "A Clockwork Orange",
        answerC = "Full Metal Jacket",
        answerD = "Eyes Wide Shut",
        correctAnswer = 1, // B
        explanation = "'A Clockwork Orange' (1971) basiert auf Anthony Burgess' Roman von 1962 und zeigt den Jugendlichen Alex in einem gewalttaetigen England der nahen Zukunft. Kubrick zog den Film in Grossbritannien selbst aus dem Verleih.",
        difficulty = 2,
        funFact = "Kubrick zog 'A Clockwork Orange' 1973 aus dem britischen Verleih, weil er Gewaltandrohungen gegen seine Familie erhielt. Der Film wurde erst nach seinem Tod 1999 wieder in UK gezeigt."
    ),

    // Question 8 - Kubrick
    Question(
        categoryId = 4,
        questionText = "Welcher Kubrick-Film endet mit dem ikonischen Bild eines explodie renden Atompilzes, waehrend Vera Lynn singt?",
        answerA = "Full Metal Jacket",
        answerB = "Spartacus",
        answerC = "Dr. Seltsam oder: Wie ich lernte, die Bombe zu lieben",
        answerD = "Barry Lyndon",
        correctAnswer = 2, // C
        explanation = "'Dr. Strangelove or: How I Learned to Stop Worrying and Love the Bomb' (1964) endet mit einer Montage von Atomexplosionen, unterlegt mit Vera Lynns 'We'll Meet Again'. Es ist eine schwarze Komoedie ueber den Kalten Krieg.",
        difficulty = 2,
        funFact = "Peter Sellers spielte in Dr. Strangelove gleich drei verschiedene Rollen: den britischen Offizier Mandrake, US-Praesident Muffley und den Titelcharakter Dr. Strangelove."
    ),

    // Question 9 - Coen Brothers
    Question(
        categoryId = 4,
        questionText = "Welcher Film der Coen Brothers gewann 2008 den Oscar fuer den Besten Film?",
        answerA = "Fargo",
        answerB = "No Country for Old Men",
        answerC = "True Grit",
        answerD = "The Big Lebowski",
        correctAnswer = 1, // B
        explanation = "'No Country for Old Men' (2007) gewann vier Oscars, darunter Bester Film und Beste Regie fuer Joel und Ethan Coen. Der Film basiert auf Cormac McCarthys Roman und zeigt den psychopatischen Killer Anton Chigurh.",
        difficulty = 2,
        funFact = "Javier Bardem gewann fuer seine Rolle als Anton Chigurh den Oscar als Bester Nebendarsteller. Sein ungewoehnlicher Haarschnitt stammte aus der Epoche des Films, den 1980er Jahren."
    ),

    // Question 10 - Coen Brothers
    Question(
        categoryId = 4,
        questionText = "In welchem Coen-Brothers-Film spielt Jeff Bridges den faul en Hobbykegeler 'The Dude'?",
        answerA = "True Grit",
        answerB = "Burn After Reading",
        answerC = "Blood Simple",
        answerD = "The Big Lebowski",
        correctAnswer = 3, // D
        explanation = "'The Big Lebowski' (1998) zeigt Jeff 'The Dude' Lebowski, einen Hobbykegeler in Los Angeles, der in eine Geschichte aus Entfuehrung und Betrug hineingezogen wird. Der Film wurde zum Kultobjekt.",
        difficulty = 2,
        funFact = "Der Film war beim Start kein grosser Kassenerfolg, entwickelte sich aber zum Kultfilm. Jedes Jahr findet ein 'Lebowski Fest' statt, wo Fans als 'The Dude' verkleidet feiern."
    ),

    // Question 11 - Wes Anderson
    Question(
        categoryId = 4,
        questionText = "Welcher Wes-Anderson-Film spielt in einem fiktiven europaeischen Hotel und gewann vier Oscars?",
        answerA = "Moonrise Kingdom",
        answerB = "The Royal Tenenbaums",
        answerC = "The Grand Budapest Hotel",
        answerD = "Rushmore",
        correctAnswer = 2, // C
        explanation = "'The Grand Budapest Hotel' (2014) spielt in der Zwischenkriegszeit in einem fiktiven europaeischen Land und gewann vier Oscars, darunter fuer Kostuem und Set-Design. Ralph Fiennes spielt Concierge Gustave H.",
        difficulty = 2,
        funFact = "Wes Anderson liess sich von den Schriften des Wiener Autors Stefan Zweig fuer das Hotel-Setting inspirieren. Die Filmstruktur hat vier verschiedene Bildformate fuer verschiedene Zeitebenen."
    ),

    // Question 12 - Wes Anderson visual style
    Question(
        categoryId = 4,
        questionText = "Fuer welches visuelle Stilmittel ist Regisseur Wes Anderson besonders bekannt?",
        answerA = "Extreme Weitwinkelaufnahmen und verwackelte Handkamera",
        answerB = "Symmetrische Bildkompositionen und Pastellfarben",
        answerC = "Schwarzweiss-Bilder und harte Schatten",
        answerD = "Zeitlupe und Zeitraffer als Hauptstilmittel",
        correctAnswer = 1, // B
        explanation = "Wes Anderson ist beruehmt fuer seine perfekt symmetrischen Bildkompositionen, seinen distinkten Pastellfarbpaletten und die Verwendung von frontalen Aufnahmen ('flat compositions'). Sein Stil ist sofort wiedererkennbar.",
        difficulty = 2,
        funFact = "Wes Andersons symmetrischer Stil hat auf Instagram den Trend 'Accidentally Wes Anderson' ausgeloest, bei dem Menschen Fotos von symmetrischen, bunten Orten posten."
    ),

    // Question 13 - Film noir
    Question(
        categoryId = 4,
        questionText = "Welches der folgenden Filme gilt als klassisches Beispiel des Film Noir?",
        answerA = "Casablanca (1942)",
        answerB = "Double Indemnity (1944)",
        answerC = "Gone with the Wind (1939)",
        answerD = "Citizen Kane (1941)",
        correctAnswer = 1, // B
        explanation = "'Double Indemnity' (1944) von Billy Wilder ist ein Musterbeispiel des Film Noir: eine manipulative Femme Fatale, ein schwacher Mann, Mord fuer Geld und ein unausweichliches Schicksal.",
        difficulty = 2,
        funFact = "Der Begriff 'Film Noir' wurde von franzoesischen Kritikern gepraegt, nachdem sie nach dem Zweiten Weltkrieg eine Serie dunkler amerikanischer Kriminalfilme sahen, die sie noch nicht kannten."
    ),

    // Question 14 - Film noir characteristics
    Question(
        categoryId = 4,
        questionText = "Welche Figur ist das typische Gegenstueck zum Detektiv im klassischen Film Noir?",
        answerA = "Der brave Polizist",
        answerB = "Die Femme Fatale",
        answerC = "Der Gangsterboss",
        answerD = "Das Waisenkind",
        correctAnswer = 1, // B
        explanation = "Die 'Femme Fatale' (franzoesisch fuer 'verhaengnisvolle Frau') ist eine zentrale Figur des Film Noir. Sie ist verfuehrerisch, intelligent und manipuliert den maennlichen Protagonisten oft in seinen Untergang.",
        difficulty = 2,
        funFact = "Die Femme Fatale des Film Noir wird oft als Reaktion auf die neuen Frauenrollen des Zweiten Weltkriegs interpretiert, als Frauen in Maennerberufen arbeiteten."
    ),

    // Question 15 - New Hollywood
    Question(
        categoryId = 4,
        questionText = "Welcher Film gilt als Beginn des New-Hollywood-Movements in den 1960er Jahren?",
        answerA = "Easy Rider (1969)",
        answerB = "Bonnie und Clyde (1967)",
        answerC = "The Graduate (1967)",
        answerD = "2001: Odyssee im Weltraum (1968)",
        correctAnswer = 1, // B
        explanation = "'Bonnie und Clyde' (1967) von Arthur Penn wird oft als der Film zitiert, der das New-Hollywood-Movement einlaeute te. Er brach mit konventionellen Erzaehlformen und Moralvorstellungen des klassischen Hollywood-Systems.",
        difficulty = 2,
        funFact = "'Bonnie und Clyde' zeigte explizitere Gewalt als bis dahin im amerikanischen Mainstream-Kino ueblich und loeuste eine heftige Kontroverse aus."
    ),

    // Question 16 - New Hollywood directors
    Question(
        categoryId = 4,
        questionText = "Welcher der folgenden Regisseure gehoert NICHT zur sogenannten 'Movie Brat'-Generation des New Hollywood?",
        answerA = "Francis Ford Coppola",
        answerB = "Martin Scorsese",
        answerC = "John Huston",
        answerD = "Steven Spielberg",
        correctAnswer = 2, // C
        explanation = "John Huston (Jahrgang 1906) gehoert zur klassischen Hollywood-Generation und nicht zu den 'Movie Brats' des New Hollywood. Coppola, Scorsese und Spielberg studierten alle an Filmhochschulen und praegten das Kino der 1970er.",
        difficulty = 2,
        funFact = "Der Begriff 'Movie Brats' bezieht sich auf die erste Generation von Filmemachern, die an Filmhochschulen ausgebildet wurden, anstatt sich durch die Studiohierarchie hochzuarbeiten."
    ),

    // Question 17 - Method acting
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler-Lehrer entwickelte die Grundlagen des Method Acting in Amerika und gruendete das Actors Studio?",
        answerA = "Lee Strasberg",
        answerB = "Konstantin Stanisl awski",
        answerC = "Elia Kazan",
        answerD = "Stella Adler",
        correctAnswer = 0, // A
        explanation = "Lee Strasberg fuehrte als Leiter des Actors Studio in New York das Method Acting in Amerika ein, basierend auf Stanisla wskis Techniken. Er praeg te Schauspieler wie Marlon Brando, James Dean und Dustin Hoffman.",
        difficulty = 2,
        funFact = "Marlon Brando und Marilyn Monroe gehoerten beide zum Actors Studio unter Lee Strasberg. Monroe war sogar in seinem Testament bedacht."
    ),

    // Question 18 - Method acting example
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler verlor echtes Gewicht und verbrachte Monate auf der Strasse, um sich auf seine Rolle in 'Der Teufel in Miss Jones' vorzubereiten? Gemeint ist: Welcher Schauspieler ist bekannt fuer extreme physische Rollenvorbereitung a la Method Acting?",
        answerA = "Dustin Hoffman in 'Der Marathon-Mann'",
        answerB = "Christian Bale in 'The Machinist'",
        answerC = "Robert De Niro in 'Raging Bull'",
        answerD = "Alle drei: Alle sind klassische Method-Acting-Beispiele",
        correctAnswer = 3, // D
        explanation = "Alle drei gelten als Paradebeispiele des Method Acting: Hoffman weigerte sich, fuer eine Szene zu schlafen; Bale verlor fuer 'The Machinist' 28 kg; De Niro nahm fuer 'Raging Bull' 27 kg zu und lernte echter Boxen.",
        difficulty = 2,
        funFact = "Als Dustin Hoffman fuer eine Szene in 'Marathon Man' naechtela ng wach blieb, sagte sein Co-Star Laurence Olivier: 'Lieber Junge, hast du es nicht mal mit Schauspielern versucht?'"
    ),

    // Question 19 - Film scores: John Williams
    Question(
        categoryId = 4,
        questionText = "Fuer welchen Film komponierte John Williams NICHT die Filmmusik?",
        answerA = "Jurassic Park",
        answerB = "Schindlers Liste",
        answerC = "E.T. der Ausserirdische",
        answerD = "Interstellar",
        correctAnswer = 3, // D
        explanation = "Die Filmmusik zu 'Interstellar' (2014) wurde von Hans Zimmer komponiert, nicht von John Williams. Williams komponierte fuer Spielbergs Filme wie Jurassic Park, Schindlers Liste und E.T., sowie Star Wars und Indiana Jones.",
        difficulty = 2,
        funFact = "John Williams hat mehr Oscar-Nominierungen als jeder andere lebende Mensch. Er erhielt 54 Nominierungen und gewann fuenfmal."
    ),

    // Question 20 - Film scores: Ennio Morricone
    Question(
        categoryId = 4,
        questionText = "Welcher Komponist ist besonders fuer seine Spaghetti-Western-Soundtracks bekannt, etwa fuer 'Zwei Gluecklose Helden'?",
        answerA = "Bernard Herrmann",
        answerB = "Ennio Morricone",
        answerC = "Nino Rota",
        answerD = "Jerry Goldsmith",
        correctAnswer = 1, // B
        explanation = "Ennio Morricone (1928-2020) schuf die ikonischen Soundtracks fuer Sergio Leones Spaghetti-Western, darunter 'Fuer eine Handvoll Dollar', 'Fuer ein paar Dollar mehr' und 'Zwei gluecklose Helden' (The Good, the Bad and the Ugly).",
        difficulty = 2,
        funFact = "Morricone erhielt erst 2007 einen Ehrenoscar fuer sein Lebenswerk, bevor er 2016 den regulaeren Oscar fuer 'The Hateful Eight' gewann."
    ),

    // Question 21 - Film scores: Bernard Herrmann
    Question(
        categoryId = 4,
        questionText = "Welcher Filmkomponist schrieb die beruehmt e Streicher-Musik fuer die Duschszene in Alfred Hitchcocks 'Psycho'?",
        answerA = "Ennio Morricone",
        answerB = "John Williams",
        answerC = "Bernard Herrmann",
        answerD = "Max Steiner",
        correctAnswer = 2, // C
        explanation = "Bernard Herrmann komponierte die beruehm te 'Mordmusik' fuer die Duschszene in 'Psycho' mit Streichern, die 'Messer' imitieren. Hitchcock wollte die Szene urspruenglich ohne Musik, doch Herrmann bestand darauf.",
        difficulty = 2,
        funFact = "Bernard Herrmann arbeitete auch an 'Vertigo', 'North by Northwest' und 'Taxi Driver'. Beim Bruch mit Hitchcock nach 'Torn Curtain' war er am Boden zerstoert."
    ),

    // Question 22 - Cinematography: depth of field
    Question(
        categoryId = 4,
        questionText = "Was bezeichnet der cinematografische Begriff 'Tiefenschaerfe' (Deep Focus)?",
        answerA = "Eine Technik, bei der nur das Motiv im Vordergrund scharf ist",
        answerB = "Eine Technik, bei der sowohl Vordergrund als auch Hintergrund scharf sind",
        answerC = "Eine Technik fuer extreme Nahaufnahmen",
        answerD = "Eine Technik fuer Zeitlupenaufnahmen",
        correctAnswer = 1, // B
        explanation = "Deep Focus (Tiefenschaerfe) bezeichnet eine Kameratechnik, bei der das gesamte Bild vom Vordergrund bis zum Hintergrund scharf ist. Kameramann Gregg Toland setzte sie in 'Citizen Kane' meisterhaft ein.",
        difficulty = 2,
        funFact = "Gregg Tolands tiefenscharfe Bilder in 'Citizen Kane' (1941) wurden durch besonders kleine Blendenwerte erzielt und inspirierten Generationen von Kameramaennern."
    ),

    // Question 23 - Cinematography: famous DPs
    Question(
        categoryId = 4,
        questionText = "Welcher Kameramann (Director of Photography) arbeitete eng mit Ingmar Bergman zusammen und praegte den nordischen Filmstil?",
        answerA = "Vilmos Zsigmond",
        answerB = "Sven Nykvist",
        answerC = "Gordon Willis",
        answerD = "Roger Deakins",
        correctAnswer = 1, // B
        explanation = "Sven Nykvist war Ingmar Bergmans bevorzugter Kameramann und arbeitete bei 21 seiner Filme mit. Sein Einsatz von natuerlichem Licht und die Betonung menschlicher Gesichter praegten den schwedischen Filmstil.",
        difficulty = 2,
        funFact = "Sven Nykvist gewann zweimal den Oscar fuer die beste Kamera: fuer 'Aus dem Leben der Marionetten' (1974) und 'Fanny und Alexander' (1982)."
    ),

    // Question 24 - Film editing
    Question(
        categoryId = 4,
        questionText = "Was ist ein 'Jump Cut' in der Filmtechnik?",
        answerA = "Ein ploetzlicher Wechsel zu einer anderen Szene ohne uebergangslose Verbindung",
        answerB = "Ein absichtlicher Schnitt, der die Zeit innerhalb einer Szene ueberspringt und den Fluss unterbricht",
        answerC = "Ein Schnitt, bei dem die Kamera von links nach rechts springt",
        answerD = "Ein Schnitt, der mit einem Soundeffekt verbunden ist",
        correctAnswer = 1, // B
        explanation = "Ein Jump Cut ist ein Schnitt innerhalb derselben Szene, bei dem eine kurze Zeitspanne uebersprungen wird. Er wirkt beabsichtigt unruhig und wurde besonders von Jean-Luc Godard in der Nouvelle Vague populaer gemacht.",
        difficulty = 2,
        funFact = "Godard nutzte Jump Cuts in 'Ausser Atem' (1960) teils aus Not, weil er Material kuerzen musste, erkannte aber den kuenstlerischen Wert und machte es zum Stilmittel."
    ),

    // Question 25 - Film editing: Eisenstein
    Question(
        categoryId = 4,
        questionText = "Welcher sowjetische Regisseur entwickelte die Theorie des 'Intellektuellen Montage', bei der Schnitte Ideen erzeugen?",
        answerA = "Andrei Tarkowski",
        answerB = "Dziga Vertov",
        answerC = "Sergei Eisenstein",
        answerD = "Vsevolod Pudovkin",
        correctAnswer = 2, // C
        explanation = "Sergei Eisenstein entwickelte die Montagetheorie, nach der zwei aufeinanderfolgende Bilder eine neue Bedeutung erzeugen, die in keinem der Einzelbilder steckt. Sein Film 'Panzerkreuzer Potemkin' (1925) ist das bekannteste Beispiel.",
        difficulty = 2,
        funFact = "Eisensteins 'Odessa-Treppe'-Sequenz aus Panzerkreuzer Potemkin gilt als eine der einflussreichsten Szenen der Filmgeschichte und wurde von Brian De Palma und viele andere Regisseure zitiert."
    ),

    // Question 26 - Famous sequel: Godfather
    Question(
        categoryId = 4,
        questionText = "Welcher Film ist einer der wenigen Sequels, der bei den Oscars den Preis fuer den Besten Film gewann?",
        answerA = "Star Wars: Das Imperium schlaegt zurueck",
        answerB = "Der Pate II",
        answerC = "Aliens",
        answerD = "Terminator 2",
        correctAnswer = 1, // B
        explanation = "'Der Pate II' (1974) von Francis Ford Coppola gewann den Oscar fuer den Besten Film und machte damit als einer der ersten Sequels diesen Preis. Der Film gilt vielen als ueberlegen gegenueber dem ersten Teil.",
        difficulty = 2,
        funFact = "Der Pate II ist einer der seltenen Sequels, der als besser bewertet wird als das Original. Robert De Niro gewann den Oscar fuer seine Darstellung des jungen Vito Corleone."
    ),

    // Question 27 - Trilogy: Lord of the Rings
    Question(
        categoryId = 4,
        questionText = "Welcher Teil der Herr-der-Ringe-Trilogie von Peter Jackson gewann als erster und einziger Teil den Oscar fuer den Besten Film?",
        answerA = "Die Gefaehrten",
        answerB = "Die zwei Tuerme",
        answerC = "Die Rueckkehr des Koenigs",
        answerD = "Kein Teil gewann den Oscar",
        correctAnswer = 2, // C
        explanation = "'Die Rueckkehr des Koenigs' (2003) gewann alle 11 Oscars, fuer die er nominiert war, inklusive Bester Film. Es ist damit gemeinsam mit 'Ben-Hur' und 'Titanic' der film mit dem meisten Oscar-Gewinnen in einer Verleihung.",
        difficulty = 2,
        funFact = "Alle drei Herr-der-Ringe-Teile wurden gleichzeitig ueber 438 Drehtage gedreht. Peter Jackson kannte die Ergebnisse der ersten Oscars, als er noch den dritten Teil schnitt."
    ),

    // Question 28 - Kubrick 2001
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr spielt Kubricks '2001: Odyssee im Weltraum' hauptsaechlich?",
        answerA = "2001",
        answerB = "2010",
        answerC = "Teils 4 Millionen v. Chr., teils 2001",
        answerD = "2050",
        correctAnswer = 2, // C
        explanation = "Kubricks '2001: Odyssee im Weltraum' (1968) spielt auf zwei Zeitebenen: Teils in der praehistorischen Vergangenheit (bei der Entstehung des Werkzeuggebrauchs) und teils im Jahr 2001, wo die KI HAL 9000 eine Raumcrew bedroht.",
        difficulty = 2,
        funFact = "2001: Odyssee im Weltraum hatte so wenig Dialog, dass der erste gesprochene Satz erst 25 Minuten nach Filmbeginn faellt."
    ),

    // Question 29 - Coen Brothers Fargo
    Question(
        categoryId = 4,
        questionText = "Wofuer gewann der Coen-Brothers-Film 'Fargo' (1996) Oscars?",
        answerA = "Bester Film und Beste Regie",
        answerB = "Bestes Originaldrehbuch und Beste Hauptdarstellerin",
        answerC = "Bester Film und Bester Schnitt",
        answerD = "Bestes Originaldrehbuch und Beste Regie",
        correctAnswer = 1, // B
        explanation = "'Fargo' gewann zwei Oscars: Bestes Originaldrehbuch fuer Joel und Ethan Coen sowie Beste Hauptdarstellerin fuer Frances McDormand als Polizistin Marge Gunderson. Der Film war fuer sieben Oscars nominiert.",
        difficulty = 2,
        funFact = "'Fargo' behauptet im Vorspann, eine wahre Geschichte zu sein, was nicht stimmt. Die Coens erfanden das als Stilmittel."
    ),

    // Question 30 - New Hollywood: Coppola
    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur drehte sowohl 'Der Pate' als auch 'Apocalypse Now'?",
        answerA = "Martin Scorsese",
        answerB = "Brian De Palma",
        answerC = "Francis Ford Coppola",
        answerD = "Michael Cimino",
        correctAnswer = 2, // C
        explanation = "Francis Ford Coppola drehte sowohl die Pate-Trilogie (1972, 1974, 1990) als auch 'Apocalypse Now' (1979). Beide gelten als Meisterwerke des New Hollywood und der Filmgeschichte.",
        difficulty = 2,
        funFact = "Die Dreharbeiten zu Apocalypse Now waren so chaotisch, dass ein Dokumentarfilm darueber gedreht wurde: 'Hearts of Darkness: A Filmmaker's Apocalypse' (1991)."
    ),

    // Question 31 - Method acting: Brando
    Question(
        categoryId = 4,
        questionText = "Welches Kissen schob Marlon Brando sich in die Wangen, um Don Corleone in 'Der Pate' darzustellen?",
        answerA = "Einen echten Mundschutz eines Boxers",
        answerB = "Eine Wattierung aus dem Zahnarzt",
        answerC = "Keines, seine Backen wurden mit Makeup veraendert",
        answerD = "Keine dieser Antworten -- es war ein Zahnprothesen-aehnlicher Einsatz",
        correctAnswer = 3, // D
        explanation = "Marlon Brando nutzte fuer seine Darstellung des alten Don Corleone einen speziellen Zahnprothesen-aehnlichen Einsatz, der seine Wangen aufblies, sowie eine tiefere Stimme. Auf die Idee kam er beim Vorsprechen.",
        difficulty = 2,
        funFact = "Brando sprach beim Oscar-Empfang fuer 'Der Pate' nicht persoenlich -- er schickte eine Aktivistin der amerikanischen Ureinwohner, Sacheen Littlefeather, um seinen Preis abzulehnen."
    ),

    // Question 32 - Hans Zimmer
    Question(
        categoryId = 4,
        questionText = "Welcher deutsche Filmkomponist ist bekannt fuer den 'BRAAAM'-Ton in 'Inception' und den Score zu 'Gladiator'?",
        answerA = "Klaus Doldinger",
        answerB = "Hans Zimmer",
        answerC = "Michael Kamen",
        answerD = "Thomas Newman",
        correctAnswer = 1, // B
        explanation = "Hans Zimmer, geboren 1957 in Frankfurt am Main, komponierte unter anderem die Soundtracks fuer 'Gladiator', 'Inception', 'The Dark Knight Trilogy', 'Interstellar' und 'Dune'. Der tiefe BRAAAM-Ton wurde sein Markenzeichen.",
        difficulty = 2,
        funFact = "Hans Zimmer ist kein klassisch ausgebildeter Musiker. Er lernte das Komponieren groesstente ils autodidaktisch und ist bekannt dafuer, elektronische Musik mit Orchesterklang zu verbinden."
    ),

    // Question 33 - Famous sequel: Aliens
    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur drehte die Fortsetzung 'Aliens' (1986) von Ridley Scotts 'Alien' (1979)?",
        answerA = "Ridley Scott",
        answerB = "David Fincher",
        answerC = "James Cameron",
        answerD = "Joss Whedon",
        correctAnswer = 2, // C
        explanation = "James Cameron drehte 'Aliens' (1986) als Fortsetzung von Ridley Scotts Horror-Klassiker 'Alien'. Cameron verwandelte den Horror in einen Actionfilm. Sigourney Weaver wurde fuer ihre Rolle als Ripley fuer den Oscar nominiert.",
        difficulty = 2,
        funFact = "James Cameron drehte sowohl 'Aliens' (1986) als auch 'Terminator 2' (1991), zwei der bekanntesten Sequels der Filmgeschichte -- beide gelten als ebenbuertig oder besser als das Original."
    ),

    // Question 34 - Film noir: The Maltese Falcon
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielte Privatdetektiv Sam Spade im Film-Noir-Klassiker 'Der Malteser Falke' (1941)?",
        answerA = "Cary Grant",
        answerB = "James Cagney",
        answerC = "Humphrey Bogart",
        answerD = "Edward G. Robinson",
        correctAnswer = 2, // C
        explanation = "Humphrey Bogart spielte Sam Spade in 'Der Malteser Falke' (The Maltese Falcon, 1941) von John Huston. Der Film gilt als einer der ersten und besten Film Noirs und machte Bogart zum Leinwandstar.",
        difficulty = 2,
        funFact = "Der Malteser Falke war das Regiedebut von John Huston. Das Studio hatte so wenig Vertrauen in den Film, dass er kaum promotet wurde -- er wurde dennoch ein Hit."
    ),

    // Question 35 - Hitchcock: The Birds
    Question(
        categoryId = 4,
        questionText = "Welche ungewoehnliche Eigenschaft hat die Filmmusik von Alfred Hitchcocks 'Die Voegel' (1963)?",
        answerA = "Keine Musik, nur elektronische Vogelgeraeuesche",
        answerB = "Nur Streicher ohne Blaesser",
        answerC = "Ausschliesslich klassische Musik des 18. Jahrhunderts",
        answerD = "Der Film hat ueberhaupt keine Tonspur ausser den Dialogen",
        correctAnswer = 0, // A
        explanation = "In 'Die Voegel' gibt es keine konventionelle Filmmusik. Stattdessen nutzte Hitchcock und Sound-Designer Bernard Herrmann ausschliesslich elektronisch erzeugte und manipulierte Vogelgeraeuesche als Klangkulisse.",
        difficulty = 2,
        funFact = "Fuer die Tonspur zu 'Die Voegel' wurden echte Vogelstimmen aufgenommen und dann mit dem Trautonium -- einem fruehen elektronischen Instrument -- bearbeitet und verfremdet."
    ),

    // Question 36 - Cinematography: Chiaroscuro
    Question(
        categoryId = 4,
        questionText = "Welcher Begriff aus der Malerei beschreibt im Kino den Kontrast zwischen Licht und Schatten, typisch fuer Film Noir?",
        answerA = "Chiaroscuro",
        answerB = "Sfumato",
        answerC = "Impasto",
        answerD = "Fresco",
        correctAnswer = 0, // A
        explanation = "Chiaroscuro (italienisch fuer 'hell-dunkel') bezeichnet den dramatischen Kontrast zwischen Licht und Schatten in der Malerei und Kinematografie. Im Film Noir wurde dieser Stil mit starken Schatten und hellen Flecken eingesetzt.",
        difficulty = 2,
        funFact = "Die Chiaroscuro-Technik wurde in der Malerei von Leonardo da Vinci und Caravaggio perfektioniert. Im Film uebernahmen es die deutschen Expressionisten der 1920er."
    ),

    // Question 37 - New Hollywood: Scorsese
    Question(
        categoryId = 4,
        questionText = "Welcher Martin-Scorsese-Film von 1976 zeigt Robert De Niro als verwirrten Vietnam-Veteran in New York?",
        answerA = "Raging Bull",
        answerB = "Mean Streets",
        answerC = "Taxi Driver",
        answerD = "The Departed",
        correctAnswer = 2, // C
        explanation = "'Taxi Driver' (1976) zeigt Robert De Niro als Travis Bickle, einen schlaflosen Taxifahrer und Vietnam-Veteranen, der sich von der Gesellschaft abgelehnt fuehlt und eine 12-jaehrige Prostituierte retten will.",
        difficulty = 2,
        funFact = "Der beruehm te 'You talkin' to me?'-Monolog von Robert De Niro war nicht im Drehbuch. De Niro improvisierte ihn und Scorsese liess ihn stehen."
    ),

    // Question 38 - Wes Anderson: stop-motion
    Question(
        categoryId = 4,
        questionText = "Welcher Wes-Anderson-Film verwendet Stop-Motion-Animation und basiert auf einem Roman von Roald Dahl?",
        answerA = "Isle of Dogs",
        answerB = "Fantastic Mr. Fox",
        answerC = "Moonrise Kingdom",
        answerD = "The Life Aquatic",
        correctAnswer = 1, // B
        explanation = "'Fantastic Mr. Fox' (2009) ist Wes Andersons Stop-Motion-Abenteuerfilm, basierend auf Roald Dahls Roman 'Fantastischer Mr. Fox'. George Clooney und Meryl Streep sprachen die Hauptfiguren.",
        difficulty = 2,
        funFact = "Wes Anderson verwendete traditionelle Stop-Motion-Puppen aus echtem Fell fuer den Film und drehte es in einem Studio in England. Jede Sekunde des Films entspricht 24 einzeln fotografierten Bildern."
    ),

    // Question 39 - Oscar: first colour film
    Question(
        categoryId = 4,
        questionText = "Welcher Film gewann als erster farbiger Film den Oscar fuer den Besten Film?",
        answerA = "Das zauberhafte Land (Wizard of Oz, 1939)",
        answerB = "Vom Winde verweht (Gone with the Wind, 1939)",
        answerC = "Schneewittchen (Snow White, 1937)",
        answerD = "Fantasia (1940)",
        correctAnswer = 1, // B
        explanation = "'Vom Winde verweht' (Gone with the Wind, 1939) war der erste farbige Film, der den Oscar fuer den Besten Film gewann. Er gewann 10 Oscars und war jahrzehntelang der kommerziell erfolgreichste Film aller Zeiten.",
        difficulty = 2,
        funFact = "'Vom Winde verweht' hat inflationsbereinigt immer noch den hoechsten Kassenerfolg aller Zeiten -- ueber 3,7 Milliarden US-Dollar in heutigem Geld."
    ),

    // Question 40 - Film editing: match cut
    Question(
        categoryId = 4,
        questionText = "Was bezeichnet ein 'Match Cut' in der Filmtechnik?",
        answerA = "Ein Schnitt, bei dem zwei visuelle oder thematische Elemente miteinander verbunden werden",
        answerB = "Ein Schnitt, der Ton und Bild versetzt",
        answerC = "Ein Schnitt innerhalb einer Einstellung ohne Kamerafahrt",
        answerD = "Ein Schnitt zwischen zwei Szenen mit identischer Laufzeit",
        correctAnswer = 0, // A
        explanation = "Ein Match Cut verbindet zwei Einstellungen durch ein aehnliches visuelles Element, eine Bewegung oder ein Thema. Das bekannteste Beispiel ist in '2001: Odyssee im Weltraum': ein Knochen wird in die Luft geworfen und schneidet direkt zu einem Raumschiff.",
        difficulty = 2,
        funFact = "Der Knochen-zu-Raumschiff-Match-Cut in '2001' gilt als der groesste Zeitsprung in der Filmgeschichte -- von der Urzeit zu einem zukuenftigen Raumzeitalter in einem einzigen Schnitt."
    ),

    // Question 41 - Sequels: Dark Knight
    Question(
        categoryId = 4,
        questionText = "Was ist die genaue Bezeichnung des zweiten Films in Christopher Nolans Batman-Trilogie?",
        answerA = "Batman Returns",
        answerB = "The Dark Knight Rises",
        answerC = "The Dark Knight",
        answerD = "Batman Forever",
        correctAnswer = 2, // C
        explanation = "'The Dark Knight' (2008) ist der zweite Teil von Nolans Batman-Trilogie zwischen 'Batman Begins' (2005) und 'The Dark Knight Rises' (2012). Er gilt als einer der besten Superhelden-Filme aller Zeiten.",
        difficulty = 2,
        funFact = "The Dark Knight erzielte als erster Superheldenfilm ueber eine Milliarde Dollar und war der erste, der bei den Oscars als Bester Film erwaehnt wurde."
    ),

    // Question 42 - Film noir: Double Indemnity
    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur drehte den Film-Noir-Klassiker 'Double Indemnity' (1944) und spaeter die Komodien 'Manche moegen's heiss' und 'Das Apartment'?",
        answerA = "John Huston",
        answerB = "Howard Hawks",
        answerC = "Otto Preminger",
        answerD = "Billy Wilder",
        correctAnswer = 3, // D
        explanation = "Billy Wilder, geboertig aus Wien, drehte sowohl Film-Noir-Klassiker wie 'Double Indemnity' als auch Komodien wie 'Manche moegen's heiss' und 'Das Apartment', das den Oscar als Bester Film gewann.",
        difficulty = 2,
        funFact = "Billy Wilder floh in den 1930er Jahren aus Nazi-Deutschland nach Hollywood und wurde zu einem der vielseitigsten Regisseure der Filmgeschichte."
    ),

    // Question 43 - Famous score: Nino Rota
    Question(
        categoryId = 4,
        questionText = "Welcher Komponist schrieb die beruehm te Filmmusik zu 'Der Pate' (1972)?",
        answerA = "Ennio Morricone",
        answerB = "Bernard Herrmann",
        answerC = "Nino Rota",
        answerD = "Angelo Badalamenti",
        correctAnswer = 2, // C
        explanation = "Nino Rota komponierte die ikonische Filmmusik zu 'Der Pate' (1972), darunter die beruehm te 'Love Theme from The Godfather'. Er arbeitete eng mit Regisseur Francis Ford Coppola zusammen.",
        difficulty = 2,
        funFact = "Nino Rota war auch der bevorzugte Komponist von Federico Fellini und schrieb Musik fuer '8 1/2' und 'La Dolce Vita'."
    ),

    // Question 44 - Cinematography: aspect ratio
    Question(
        categoryId = 4,
        questionText = "Welches Bildseitenverhaaeltnis (Aspect Ratio) nutzen die meisten modernen Kinofilme?",
        answerA = "1.33:1 (4:3)",
        answerB = "1.85:1 oder 2.39:1",
        answerC = "16:9",
        answerD = "1:1",
        correctAnswer = 1, // B
        explanation = "Die meisten Kinofilme nutzen 1.85:1 (Flat) oder 2.39:1 (Scope/Anamorphic). Das fruehe Kino nutzte 1.33:1, aber mit der TV-Konkurrenz in den 1950ern wechselte Hollywood zu breiteren Formaten.",
        difficulty = 2,
        funFact = "Wes Anderson drehte 'The Grand Budapest Hotel' bewusst in drei verschiedenen Aspect Ratios fuer verschiedene Zeitebenen: 1.33:1, 1.85:1 und 2.35:1."
    ),

    // Question 45 - New Hollywood: Spielberg
    Question(
        categoryId = 4,
        questionText = "Welcher Spielberg-Film von 1975 gilt als erster moderner Blockbuster und hatte ein neuartiges Vermarktungskonzept?",
        answerA = "Close Encounters of the Third Kind",
        answerB = "Der Weisse Hai (Jaws)",
        answerC = "1941",
        answerD = "Raiders of the Lost Ark",
        correctAnswer = 1, // B
        explanation = "'Der Weisse Hai' (Jaws, 1975) gilt als der erste moderne Blockbuster. Universal Pictures zeigte ihn gleichzeitig in ueber 400 Kinos (damals ein Rekord) mit einer intensiven TV-Werbekampagne -- das Marketingmodell, das Hollywood bis heute praegte.",
        difficulty = 2,
        funFact = "Spielberg war bei Jaws nur 26 Jahre alt. Der Film kostete 9 Millionen Dollar und spielte 470 Millionen Dollar ein -- und erschreckte eine Generation von Strandbesuchern dauerhaft."
    ),

    // Question 46 - Kubrick: Eyes Wide Shut
    Question(
        categoryId = 4,
        questionText = "Mit welchem Schauspieler-Ehepaar drehte Stanley Kubrick seinen letzten Film 'Eyes Wide Shut' (1999)?",
        answerA = "Jodie Foster und Richard Gere",
        answerB = "Tom Hanks und Meg Ryan",
        answerC = "Tom Cruise und Nicole Kidman",
        answerD = "Harrison Ford und Calista Flockhart",
        correctAnswer = 2, // C
        explanation = "'Eyes Wide Shut' (1999) war Kubricks letzter Film, der kurz vor seinem Tod im Maerz 1999 fertiggestellt wurde. Er drehte ihn mit Tom Cruise und Nicole Kidman, die damals noch verheiratet waren.",
        difficulty = 2,
        funFact = "Die Dreharbeiten zu Eyes Wide Shut dauerten 400 Tage -- Kubricks laengste Produktion. Der Film schlaegt den Guinness-Weltrekord fuer die laengste ununterbrochene Filmproduktion."
    ),

    // Question 47 - Oscar: Best Animated Feature
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr wurde die Oscar-Kategorie 'Bester Animationsfilm' zum ersten Mal vergeben?",
        answerA = "1991",
        answerB = "1995",
        answerC = "2002",
        answerD = "2006",
        correctAnswer = 2, // C
        explanation = "Die Oscar-Kategorie 'Bester Animationsfilm' wurde erstmals bei der 74. Verleihung 2002 vergeben. Der erste Gewinner war 'Shrek' von DreamWorks. 'Spirited Away' gewann sie im Jahr darauf.",
        difficulty = 2,
        funFact = "Bevor die Kategorie einfuehrt wurde, waren Animationsfilme gelegentlich fuer 'Besten Film' nominiert: 'Schoenheit und das Tier' (1991) war der erste Animationsfilm, der dafuer nominiert wurde."
    ),

    // Question 48 - Coen Brothers: True Grit
    Question(
        categoryId = 4,
        questionText = "Wessen Roman liegt dem Coen-Brothers-Film 'True Grit' (2010) zugrunde, und von wem war das beruehm te Original von 1969?",
        answerA = "Buch: Cormac McCarthy / Original mit Clint Eastwood",
        answerB = "Buch: Charles Portis / Original mit John Wayne",
        answerC = "Buch: Larry McMurtry / Original mit Kirk Douglas",
        answerD = "Originaldrehbuch der Coens / kein frueheres Original",
        correctAnswer = 1, // B
        explanation = "Sowohl der Coen-Film (2010) als auch das Original von 1969 basieren auf Charles Portis' Roman von 1968. Im Original spielte John Wayne den Ranger Rooster Cogburn und gewann dafuer seinen einzigen Oscar.",
        difficulty = 2,
        funFact = "John Wayne spielte in seiner gesamten Karriere meist aehnliche Heldencharaktere. Rooster Cogburn in True Grit war seine einzige Oscar-gewinnende Rolle -- und eine der wenigen, wo er einen komplexen, fehlerhaften Charakter spielte."
    ),

    // Question 49 - Cinematography: tracking shot
    Question(
        categoryId = 4,
        questionText = "Wie nennt man eine lange, ununterbrochene Kamerafahrt ohne Schnitte, bekannt durch Scorseses 'GoodFellas' oder Orson Welles' 'Touch of Evil'?",
        answerA = "Pan Shot",
        answerB = "Crane Shot",
        answerC = "Long Take / Oner",
        answerD = "Dolly Shot",
        correctAnswer = 2, // C
        explanation = "Eine 'Long Take' (auch 'Oner') ist eine lange Einstellung ohne Schnitte. Das bekannteste Beispiel ist die Kopino-Einfahrt in 'GoodFellas' (1990) und die Plansequenz am Anfang von 'Touch of Evil' (1958).",
        difficulty = 2,
        funFact = "Der gesamte Film '1917' von Sam Mendes (2019) scheint eine einzige Long Take zu sein -- in Wirklichkeit wurden Schnitte unsichtbar kaschiert, oft wenn die Kamera kurz ins Dunkel schwenkt."
    ),

    // Question 50 - Famous trilogy: Star Wars original
    Question(
        categoryId = 4,
        questionText = "Was ist die chronologisch richtige Reihenfolge der Entstehung der ersten drei Star-Wars-Episodennummern?",
        answerA = "Episode I, II, III wurden zuerst gedreht",
        answerB = "Episode IV, V, VI wurden zuerst gedreht",
        answerC = "Episode VII, VIII, IX wurden zuerst gedreht",
        answerD = "Alle wurden gleichzeitig gedreht",
        correctAnswer = 1, // B
        explanation = "Die originale Trilogie (Episoden IV, V, VI) entstand zwischen 1977 und 1983. George Lucas begann mit Episode IV 'Eine neue Hoffnung', weil er Angst hatte, das Projekt werde kein Erfolg, und wollte mit der spannendsten Geschichte beginnen.",
        difficulty = 2,
        funFact = "George Lucas schrieb urspruenglich eine deutlich laengere Geschichte und nummerierte seinen Film rueckwirkend als Episode IV, nachdem er erkannte, dass er nur einen Teil davon verfilmen konnte."
    ),

)
