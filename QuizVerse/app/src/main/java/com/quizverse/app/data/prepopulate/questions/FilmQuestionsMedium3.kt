package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsMedium3(): List<Question> = listOf(

    // ─── TV History ───────────────────────────────────────────────────────────

    // Question 1
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr begann das regelmaessige Fernsehprogramm der BBC in Grossbritannien?",
        answerA = "1926",
        answerB = "1936",
        answerC = "1946",
        answerD = "1956",
        correctAnswer = 1,
        explanation = "Die BBC startete am 2. November 1936 das weltweit erste regelmaessige hochaufloesende Fernsehprogramm aus dem Alexandra Palace in London.",
        difficulty = 2,
        funFact = "Das erste live uebertragene Ereignis der BBC war eine Tanzveranstaltung, gefolgt von einem Varieteprogramm."
    ),

    // Question 2
    Question(
        categoryId = 4,
        questionText = "Wann wurde in der Bundesrepublik Deutschland das regelmaessige Fernsehprogramm aufgenommen?",
        answerA = "25. Dezember 1952",
        answerB = "1. Januar 1954",
        answerC = "3. Oktober 1950",
        answerD = "1. September 1948",
        correctAnswer = 0,
        explanation = "Das Nordwestdeutsche Fernsehen (NWDR) nahm am 25. Dezember 1952 den regelmaessigen Sendebetrieb auf. Damit war Deutschland eines der ersten Laender mit regulaeren Fernsehprogrammen.",
        difficulty = 2,
        funFact = "Die erste Sendung war eine Weihnachtssendung mit Musik und Ansprachen, die nur wenige tausend Zuschauer empfangen konnten."
    ),

    // Question 3
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr wurden die Olympischen Spiele in Tokio als weltweit erste Sportveranstaltung in Farbe live im Fernsehen uebertragen?",
        answerA = "1956",
        answerB = "1960",
        answerC = "1964",
        answerD = "1968",
        correctAnswer = 2,
        explanation = "Die Olympischen Sommerspiele 1964 in Tokio wurden in Japan erstmals in Farbe ausgestrahlt. Die NHK nutzte dafuer das NTSC-Farbfernsehsystem.",
        difficulty = 2,
        funFact = "Japan einfuehrte das Farbfernsehen bereits 1960 kommerziell, weshalb Tokio 1964 die ideale Plattform fuer diese Premiere bot."
    ),

    // Question 4
    Question(
        categoryId = 4,
        questionText = "Wann startete in Deutschland das erste oeffentlich-rechtliche Farbfernsehen regulaer?",
        answerA = "1. Januar 1965",
        answerB = "25. August 1967",
        answerC = "1. September 1970",
        answerD = "3. Oktober 1973",
        correctAnswer = 1,
        explanation = "Am 25. August 1967 wurde auf der Internationalen Funkausstellung in Berlin das PAL-Farbfernsehen in Deutschland offiziell eingefuehrt. Willy Brandt drueckte symbolisch den Startknopf.",
        difficulty = 2,
        funFact = "Das PAL-System (Phase Alternating Line) wurde vom deutschen Ingenieur Walter Bruch entwickelt und gilt als technisch ueberlegen gegenueber dem amerikanischen NTSC."
    ),

    // Question 5
    Question(
        categoryId = 4,
        questionText = "Welcher deutsche Privatsender startete am 1. Januar 1984 als erster privater TV-Sender in Deutschland?",
        answerA = "RTL",
        answerB = "Sat.1",
        answerC = "ProSieben",
        answerD = "Kabel Eins",
        correctAnswer = 1,
        explanation = "Sat.1 (damals PKS - Programmgesellschaft fuer Kabel- und Satellitenrundfunk) ging am 1. Januar 1984 als erster privater Fernsehsender in Deutschland auf Sendung.",
        difficulty = 2,
        funFact = "RTL startete nur wenige Stunden spaeter am selben Tag, ebenfalls am 1. Januar 1984, sodass beide Sender den gleichen Sendestart beanspruchen."
    ),

    // Question 6
    Question(
        categoryId = 4,
        questionText = "Welches Satellitensystem ermoeglichte ab 1989 den Empfang von Hunderten TV-Kanaelen in Europa?",
        answerA = "Astra 1A",
        answerB = "Eutelsat I",
        answerC = "Intelsat V",
        answerD = "Olympus 1",
        correctAnswer = 0,
        explanation = "Der Satellit Astra 1A, betrieben von SES (Societe Europeenne des Satellites), wurde 1988 gestartet und revolutionierte ab 1989 den Satellitenempfang in Europa durch seine starken Transponder.",
        difficulty = 2,
        funFact = "Astra 1A sendete zunaechst nur wenige Kanaele, aber der Standort bei 19,2 Grad Ost wurde zur wichtigsten Satellitenposition fuer Europa."
    ),

    // ─── Famous TV Showrunners ────────────────────────────────────────────────

    // Question 7
    Question(
        categoryId = 4,
        questionText = "Wer ist der Schoefer und Showrunner der Serie 'The Wire'?",
        answerA = "David Chase",
        answerB = "Vince Gilligan",
        answerC = "David Simon",
        answerD = "Matthew Weiner",
        correctAnswer = 2,
        explanation = "David Simon, ehemaliger Polizeireporter der Baltimore Sun, schuf 'The Wire' (2002-2008) und basierte die Serie auf seinen Erfahrungen und dem Buch 'The Corner'.",
        difficulty = 2,
        funFact = "David Simon arbeitete jahrelang als Journalist und verbrachte ein Jahr mit der Polizei Baltimore, bevor er die Serie entwickelte."
    ),

    // Question 8
    Question(
        categoryId = 4,
        questionText = "Welcher Showrunner erschuf 'Breaking Bad' und das Spin-off 'Better Call Saul'?",
        answerA = "Shonda Rhimes",
        answerB = "J.J. Abrams",
        answerC = "Ryan Murphy",
        answerD = "Vince Gilligan",
        correctAnswer = 3,
        explanation = "Vince Gilligan erschuf 'Breaking Bad' (2008-2013) und entwickelte zusammen mit Peter Gould das Spin-off 'Better Call Saul' (2015-2022) fuer AMC.",
        difficulty = 2,
        funFact = "Vince Gilligan hatte die Idee zu Breaking Bad mit dem Gedanken: Was waere, wenn Mr. Rogers boeser als Darth Vader werden wuerde?"
    ),

    // Question 9
    Question(
        categoryId = 4,
        questionText = "Shonda Rhimes ist bekannt fuer welche erfolgreichen ABC-Serien?",
        answerA = "Lost, Alias und Fringe",
        answerB = "Grey's Anatomy, Scandal und How to Get Away with Murder",
        answerC = "Desperate Housewives, Ugly Betty und Brothers & Sisters",
        answerD = "Buffy the Vampire Slayer, Angel und Firefly",
        correctAnswer = 1,
        explanation = "Shonda Rhimes gruendete Shondaland und erschuf fuer ABC 'Grey's Anatomy' (2005), 'Scandal' (2012) und produzierte 'How to Get Away with Murder' (2014). Spaeter wechselte sie zu Netflix.",
        difficulty = 2,
        funFact = "Der Begriff 'Shondaland' bezeichnet sowohl ihre Produktionsfirma als auch den Donnerstagabend bei ABC, an dem ihre Serien liefen (T.G.I.T. - Thank God It's Thursday)."
    ),

    // ─── Sitcom History ───────────────────────────────────────────────────────

    // Question 10
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr hatte 'I Love Lucy' seine Premiere im amerikanischen Fernsehen?",
        answerA = "1948",
        answerB = "1951",
        answerC = "1955",
        answerD = "1958",
        correctAnswer = 1,
        explanation = "'I Love Lucy' startete am 15. Oktober 1951 bei CBS und wurde rasch zur meistgesehenen Sendung in den USA. Die Serie lief bis 1957 mit 180 Episoden.",
        difficulty = 2,
        funFact = "Lucille Ball und ihr Ehemann Desi Arnaz beharrten darauf, die Serie auf Film statt auf Videoband aufzuzeichnen - eine Entscheidung, die spaetere Wiederholungen ermoeglichte."
    ),

    // Question 11
    Question(
        categoryId = 4,
        questionText = "Was war die revolutionaere Produktionstechnik, die 'I Love Lucy' als eine der ersten Sitcoms einfuehrte?",
        answerA = "Vorab aufgezeichnete Lachspur",
        answerB = "Dreikaamera-Setup vor Live-Publikum",
        answerC = "Farbige Kostuemstudie",
        answerD = "Aussenaufnahmen in Hollywood",
        correctAnswer = 1,
        explanation = "Das Dreikaamera-System vor Live-Studiopublikum wurde bei 'I Love Lucy' von Kameramann Karl Freund perfektioniert und wurde zum Standard fuer Sitcoms. Jede Episode wurde dreimal aus verschiedenen Winkeln gefilmt.",
        difficulty = 2,
        funFact = "Das Dreikaamera-System ist bis heute der Standard fuer Multi-Camera-Sitcoms wie 'The Big Bang Theory' oder 'Two and a Half Men'."
    ),

    // Question 12
    Question(
        categoryId = 4,
        questionText = "Wie viele Staffeln hatte 'Seinfeld' insgesamt?",
        answerA = "7 Staffeln",
        answerB = "8 Staffeln",
        answerC = "9 Staffeln",
        answerD = "10 Staffeln",
        correctAnswer = 2,
        explanation = "'Seinfeld' lief von 1989 bis 1998 bei NBC mit insgesamt 9 Staffeln und 180 Episoden. Jerry Seinfeld entschied sich, die Serie auf dem Hoehepunkt ihres Erfolges zu beenden.",
        difficulty = 2,
        funFact = "Jerry Seinfeld lehnte 110 Millionen Dollar ab, um eine zehnte Staffel zu drehen. Das Serienfinale sahen ueber 76 Millionen Zuschauer."
    ),

    // Question 13
    Question(
        categoryId = 4,
        questionText = "Auf welchem echten Bueroraumkonzept basiert die britische Originalserie 'The Office'?",
        answerA = "Einem Callcenter in Manchester",
        answerB = "Einem Papierhaendler in Slough",
        answerC = "Einem Versicherungsbuero in London",
        answerD = "Einer Druckerei in Birmingham",
        correctAnswer = 1,
        explanation = "Ricky Gervais und Stephen Merchant setzten 'The Office' in einem fiktiven Papiervertriebsunternehmen (Wernham Hogg) im trostlosen Slough an. Die Wahl von Slough als Ort war bewusst - die Stadt galt als besonders unspektakulaer.",
        difficulty = 2,
        funFact = "Der Dichter John Betjeman schrieb 1937 ein Gedicht mit dem Titel 'Slough', in dem er die Stadt als haesslich beschrieb - genau der Geist, den Gervais einfangen wollte."
    ),

    // Question 14
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr startete die amerikanische Adaptation von 'The Office' bei NBC?",
        answerA = "2003",
        answerB = "2004",
        answerC = "2005",
        answerD = "2006",
        correctAnswer = 2,
        explanation = "Die amerikanische Version von 'The Office' startete am 24. Maerz 2005 bei NBC mit Steve Carell als Michael Scott. Greg Daniels adaptierte die britische Serie von Ricky Gervais.",
        difficulty = 2,
        funFact = "Die ersten Episoden der US-Version wurden stark kritisiert, weil sie zu nah an der britischen Vorlage blieben. Ab der zweiten Staffel entwickelte die Serie ihren eigenen Stil."
    ),

    // ─── Drama Series ─────────────────────────────────────────────────────────

    // Question 15
    Question(
        categoryId = 4,
        questionText = "In welcher Stadt spielt 'The Sopranos' hauptsaechlich?",
        answerA = "New York City",
        answerB = "New Jersey",
        answerC = "Philadelphia",
        answerD = "Chicago",
        correctAnswer = 1,
        explanation = "'The Sopranos' spielt in New Jersey, wo Tony Soprano eine Mafiaorganisation leitet. Die Serie dreht sich um sein Doppelleben als Familienvater und Mafiacapo.",
        difficulty = 2,
        funFact = "Showrunner David Chase lehnte zahlreiche Angebote ab, einen 'Sopranos'-Film zu machen, produzierte aber 2021 das Prequel 'The Many Saints of Newark'."
    ),

    // Question 16
    Question(
        categoryId = 4,
        questionText = "Wie viele Emmy Awards gewann 'The Sopranos' insgesamt waehrend seiner Laufzeit?",
        answerA = "12",
        answerB = "16",
        answerC = "21",
        answerD = "27",
        correctAnswer = 2,
        explanation = "'The Sopranos' gewann insgesamt 21 Emmy Awards bei 111 Nominierungen und 5 Golden Globes. Die Serie gilt als Meilenstein der modernen Fernsehgeschichte.",
        difficulty = 2,
        funFact = null
    ),

    // Question 17
    Question(
        categoryId = 4,
        questionText = "Wer ist der Schoefer von 'The Sopranos'?",
        answerA = "David Simon",
        answerB = "David Chase",
        answerC = "Tom Fontana",
        answerD = "Steven Bochco",
        correctAnswer = 1,
        explanation = "David Chase erschuf 'The Sopranos' (1999-2007) fuer HBO. Chase war zuvor vor allem als Produzent von Krimiserien wie 'The Rockford Files' bekannt.",
        difficulty = 2,
        funFact = "David Chase hat italoamerikanische Wurzeln, was der Serie ihre Authentizitaet in der Darstellung der italoamerikanischen Kultur verlieh."
    ),

    // Question 18
    Question(
        categoryId = 4,
        questionText = "In welcher Werbeagentur spielt 'Mad Men' hauptsaechlich?",
        answerA = "Young & Rubicam",
        answerB = "Ogilvy & Mather",
        answerC = "Sterling Cooper",
        answerD = "Doyle Dane Bernbach",
        correctAnswer = 2,
        explanation = "'Mad Men' spielt in der fiktiven Werbeagentur Sterling Cooper (spaeter Sterling Cooper Draper Pryce) an der Madison Avenue in New York, in den 1960er Jahren.",
        difficulty = 2,
        funFact = "Der Begriff 'Mad Men' bezieht sich auf die Werber der Madison Avenue (abgekuerzt MAD) und war ein selbstbezeichnender Begriff aus der Werbebranche der 1960er."
    ),

    // Question 19
    Question(
        categoryId = 4,
        questionText = "Wer spielt die Hauptrolle des Don Draper in 'Mad Men'?",
        answerA = "Bryan Cranston",
        answerB = "Michael C. Hall",
        answerC = "Jon Hamm",
        answerD = "Matthew Fox",
        correctAnswer = 2,
        explanation = "Jon Hamm spielt den geheimnisvollen Werbetexter Don Draper, eine Figur mit einer verborgenen Vergangenheit. Hamm wurde fuer die Rolle weltberuehmt.",
        difficulty = 2,
        funFact = "Jon Hamm hatte vor 'Mad Men' jahrelang kaum Rollen bekommen und arbeitete als Filmset-Dekorator, bevor die Serie sein Karrieredurchbruch wurde."
    ),

    // ─── Science Fiction TV ───────────────────────────────────────────────────

    // Question 20
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr hatte 'Star Trek: The Original Series' seine Erststrahlung in den USA?",
        answerA = "1964",
        answerB = "1966",
        answerC = "1968",
        answerD = "1970",
        correctAnswer = 1,
        explanation = "'Star Trek: The Original Series' startete am 8. September 1966 bei NBC. Obwohl die Serie nur drei Staffeln lief, wurde sie zur Grundlage eines riesigen Franchise.",
        difficulty = 2,
        funFact = "Star Trek wurde nach nur zwei Staffeln fast abgesetzt, aber eine Briefkampagne der Fans rettete die dritte Staffel - eines der fruehesten Beispiele organisierter Fan-Advocacy."
    ),

    // Question 21
    Question(
        categoryId = 4,
        questionText = "Welchen Rang bekleidet Captain Picard in 'Star Trek: The Next Generation'?",
        answerA = "Admiral",
        answerB = "Commander",
        answerC = "Captain",
        answerD = "Commodore",
        correctAnswer = 2,
        explanation = "Jean-Luc Picard bekleidet den Rang eines Captain und kommandiert die USS Enterprise-D. Gespielt wird er von Patrick Stewart, der die Rolle seit 1987 verkoepert.",
        difficulty = 2,
        funFact = "Patrick Stewart war zunaechst skeptisch, ob er eine Sci-Fi-Rolle annehmen sollte. Erst als er das Drehbuch las, war er von der Seriositaet der Serie ueberzeugt."
    ),

    // Question 22
    Question(
        categoryId = 4,
        questionText = "Auf welchem Planeten wurde Mr. Spock in 'Star Trek' geboren?",
        answerA = "Vulkan",
        answerB = "Andor",
        answerC = "Betazed",
        answerD = "Romulus",
        correctAnswer = 0,
        explanation = "Mr. Spock ist halb Vulkanier, halb Mensch und wurde auf dem Planeten Vulkan geboren. Die Vulkanier sind bekannt fuer ihre strikte Logik und Unterdrueckung von Emotionen.",
        difficulty = 2,
        funFact = "Der Vulkaniergruss (geteilte Hand mit gespreizten Fingern) wurde von Leonard Nimoy selbst erfunden und basiert auf einem juedischen Segensgruss, dem Dukhanen."
    ),

    // Question 23
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr startete die britische Sci-Fi-Serie 'Doctor Who' urspruenglich?",
        answerA = "1956",
        answerB = "1960",
        answerC = "1963",
        answerD = "1966",
        correctAnswer = 2,
        explanation = "'Doctor Who' startete am 23. November 1963 bei der BBC - einen Tag nach dem Attentat auf John F. Kennedy. Die Serie gilt als laengste laufende Sci-Fi-Serie der Welt.",
        difficulty = 2,
        funFact = "Die erste Episode wurde am naechsten Tag wiederholt, weil die Nachrichtenberichterstattung ueber das Kennedy-Attentat so viel Sendezeit beansprucht hatte."
    ),

    // Question 24
    Question(
        categoryId = 4,
        questionText = "Wie heisst das Raumschiff des Doctors in 'Doctor Who', das groesser innen als aussen ist?",
        answerA = "UNIT",
        answerB = "TARDIS",
        answerC = "DALEK",
        answerD = "SONIC",
        correctAnswer = 1,
        explanation = "Die TARDIS (Time And Relative Dimension In Space) ist das Raumschiff und die Zeitmaschine des Doctors. Von aussen sieht sie wie eine britische Polizeinotrufkabine aus den 1960ern aus.",
        difficulty = 2,
        funFact = "Die blaue Polizei-Telefonzellen-Form der TARDIS entstand, weil die Tarnvorrichtung fehlerhaft ist. Der Doctor konnte die Tarnung nie reparieren."
    ),

    // Question 25
    Question(
        categoryId = 4,
        questionText = "Auf welchem Konzept basiert die Serie 'Battlestar Galactica' (2004) in Bezug auf die Entstehung der Zylonen?",
        answerA = "Ausserirdische Invasion ohne Verbindung zur Menschheit",
        answerB = "Genetische Mutanten durch Strahlung",
        answerC = "Von Menschen erschaffene kuenstliche Intelligenzen, die sich gegen ihre Schoepfer wenden",
        answerD = "Zeitreisende aus der Zukunft der Menschheit",
        correctAnswer = 2,
        explanation = "In 'Battlestar Galactica' sind die Zylonen urspruenglich von Menschen erschaffene Roboter, die sich weiterentwickelten, gegen ihre Schoepfer aufstanden und fast die gesamte Menschheit vernichteten.",
        difficulty = 2,
        funFact = "Die neue BSG-Serie wurde urspruenglich als Miniserie konzipiert, war aber so erfolgreich, dass sie zu einer mehrteiligen Drama-Serie ausgebaut wurde."
    ),

    // Question 26
    Question(
        categoryId = 4,
        questionText = "Wer erschuf die Neuauflage von 'Battlestar Galactica' im Jahr 2004?",
        answerA = "Ronald D. Moore",
        answerB = "Bryan Fuller",
        answerC = "J. Michael Straczynski",
        answerD = "Glen A. Larson",
        correctAnswer = 0,
        explanation = "Ronald D. Moore, bekannt durch seine Arbeit an verschiedenen 'Star Trek'-Serien, entwickelte die Neuauflage von 'Battlestar Galactica' (2004-2009) als komplexe, politisch aufgeladene Dramserie.",
        difficulty = 2,
        funFact = "Ronald D. Moore verliess das Team von 'Star Trek: Voyager' nach Differenzen ueber die Serienrichtung und entwickelte danach BSG als bewussten Gegenentwurf."
    ),

    // ─── Miniseries ───────────────────────────────────────────────────────────

    // Question 27
    Question(
        categoryId = 4,
        questionText = "Auf welchem Buch basiert die HBO-Miniserie 'Band of Brothers' (2001)?",
        answerA = "Catch-22 von Joseph Heller",
        answerB = "The Longest Day von Cornelius Ryan",
        answerC = "Band of Brothers von Stephen E. Ambrose",
        answerD = "Saving Private Ryan von Robert Rodat",
        correctAnswer = 2,
        explanation = "'Band of Brothers' basiert auf dem gleichnamigen Sachbuch von Stephen E. Ambrose (1992), das die Geschichte der Easy Company, 506th Regiment, 101st Airborne Division erzaehlt.",
        difficulty = 2,
        funFact = "Steven Spielberg und Tom Hanks, die Produzenten der Serie, setzten ihr Engagement fuer WWII-Geschichten nach 'Saving Private Ryan' mit dieser Miniserie fort."
    ),

    // Question 28
    Question(
        categoryId = 4,
        questionText = "Welches reale Ereignis behandelt die HBO-Miniserie 'Chernobyl' (2019)?",
        answerA = "Den Drei-Meilen-Insel-Unfall 1979",
        answerB = "Die Nuklearkatastrophe von Tschernobyl 1986",
        answerC = "Die Fukushima-Katastrophe 2011",
        answerD = "Die Windscale-Katastrophe 1957",
        correctAnswer = 1,
        explanation = "'Chernobyl' behandelt die Nuklearkatastrophe vom 26. April 1986, als Reaktor Nr. 4 des Kernkraftwerks Tschernobyl in der ukrainischen SSR explodierte.",
        difficulty = 2,
        funFact = "Die Serie erzielte eine Bewertung von 9,4 bei IMDb und verdraengte damit kurzzeitig 'Game of Thrones' als bestbewertete Serie ueberhaupt."
    ),

    // Question 29
    Question(
        categoryId = 4,
        questionText = "Wer erschuf und schrieb die Miniserie 'Chernobyl'?",
        answerA = "Nic Pizzolatto",
        answerB = "Craig Mazin",
        answerC = "Neil Druckmann",
        answerD = "Mike White",
        correctAnswer = 1,
        explanation = "Craig Mazin, bekannt als Drehbuchautor von Komodien, schrieb alle fuenf Episoden von 'Chernobyl'. Er recherchierte jahrelang und stutzte sich auf sowjetische Quellen und Augenzeugenberichte.",
        difficulty = 2,
        funFact = "Craig Mazin und Johan Renck (Regisseur) arbeiteten spaeter erneut zusammen an der HBO-Serie 'The Last of Us' (2023)."
    ),

    // Question 30
    Question(
        categoryId = 4,
        questionText = "Wie viele Episoden umfasst 'Band of Brothers'?",
        answerA = "8",
        answerB = "10",
        answerC = "12",
        answerD = "14",
        correctAnswer = 1,
        explanation = "'Band of Brothers' besteht aus 10 Episoden und folgt der Easy Company von der Ausbildung in Toccoa, Georgia, bis zum Ende des Zweiten Weltkriegs in Europa.",
        difficulty = 2,
        funFact = "Viele der Veteranen der Easy Company waren noch am Leben, als die Serie produziert wurde, und wirkten als Zeitzeugen an der Produktion mit."
    ),

    // ─── TV Animation History ─────────────────────────────────────────────────

    // Question 31
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr hatte 'The Flintstones' seine Erststrahlung in den USA?",
        answerA = "1956",
        answerB = "1958",
        answerC = "1960",
        answerD = "1962",
        correctAnswer = 2,
        explanation = "'The Flintstones' (dt. 'Familie Feuerstein') startete am 30. September 1960 bei ABC und war die erste Zeichentrickserie, die zur Hauptsendezeit fuer Erwachsene ausgestrahlt wurde.",
        difficulty = 2,
        funFact = "The Flintstones wurde von William Hanna und Joseph Barbera erschaffen und war stark von der Sitcom 'The Honeymooners' (1955) mit Jackie Gleason inspiriert."
    ),

    // Question 32
    Question(
        categoryId = 4,
        questionText = "Von wem wurde 'Futurama' erschaffen?",
        answerA = "Seth MacFarlane",
        answerB = "Mike Judge",
        answerC = "Matt Groening",
        answerD = "Trey Parker",
        correctAnswer = 2,
        explanation = "Matt Groening, der Schoepfer der 'Simpsons', entwickelte 'Futurama' zusammen mit David X. Cohen. Die Serie startete 1999 bei Fox und spielt im Jahr 3000.",
        difficulty = 2,
        funFact = "Futurama wurde zweimal abgesetzt (2003 und 2013) und jedes Mal durch Fanproteste und Nachfrage wiederbelebt, zuletzt 2023 auf Hulu."
    ),

    // Question 33
    Question(
        categoryId = 4,
        questionText = "In welcher Stadt spielt 'Futurama'?",
        answerA = "New New York",
        answerB = "Neo Los Angeles",
        answerC = "Future Chicago",
        answerD = "Mega City One",
        correctAnswer = 0,
        explanation = "'Futurama' spielt in New New York im Jahr 3000, das auf den Ruinen des alten New York erbaut wurde. Die alte Stadt liegt kilometertief unter dem Boden.",
        difficulty = 2,
        funFact = "Matt Groening benannte 'Futurama' nach einem Ausstellungspavillon der New Yorker Weltausstellung von 1939, der die Welt von 1960 zeigte."
    ),

    // Question 34
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr hatte 'Rick and Morty' seine Erststrahlung auf Adult Swim?",
        answerA = "2011",
        answerB = "2012",
        answerC = "2013",
        answerD = "2014",
        correctAnswer = 2,
        explanation = "'Rick and Morty' startete am 2. Dezember 2013 auf Adult Swim (Cartoon Network). Die Serie wurde von Justin Roiland und Dan Harmon erschaffen.",
        difficulty = 2,
        funFact = "Die Serie basiert auf einem Parodie-Kurzfilm von Justin Roiland, der die Figuren Doc Brown und Marty McFly aus 'Zurueck in die Zukunft' persiflierte."
    ),

    // Question 35
    Question(
        categoryId = 4,
        questionText = "Wer erschuf 'Rick and Morty' zusammen mit Justin Roiland?",
        answerA = "Seth MacFarlane",
        answerB = "Dan Harmon",
        answerC = "Mike Judge",
        answerD = "Trey Parker",
        correctAnswer = 1,
        explanation = "Dan Harmon, bekannt als Schoepfer der Serie 'Community', entwickelte 'Rick and Morty' zusammen mit Justin Roiland. Harmon brachte sein Wissen ueber Erzaehlstrukturen in die Serie ein.",
        difficulty = 2,
        funFact = "Dan Harmon entwickelte ein Storytelling-Kreismodell (Harmon Story Circle), das auf Campbells Heldenreise basiert und bei Rick and Morty konsequent angewandt wird."
    ),

    // Question 36
    Question(
        categoryId = 4,
        questionText = "Welche Hanna-Barbera-Zeichentrickserie aus dem Jahr 1962 spielte in der Zukunft und galt als Gegenstueck zu 'The Flintstones'?",
        answerA = "Top Cat",
        answerB = "Yogi Bear",
        answerC = "The Jetsons",
        answerD = "Quick Draw McGraw",
        correctAnswer = 2,
        explanation = "'The Jetsons' startete 1962 bei ABC als Pendant zu 'The Flintstones': Waehrend die Flintstones in der Steinzeit lebten, spielten die Jetsons in der Raumfahrtzukunft des 21. Jahrhunderts.",
        difficulty = 2,
        funFact = "Obwohl 'The Jetsons' urspruenglich nur eine Staffel mit 24 Episoden hatte, wurde die Serie 1985-1987 mit neuen Episoden wiederbelebt."
    ),

    // Question 37
    Question(
        categoryId = 4,
        questionText = "Welche Zeichentrickserie lief ab 1969 auf CBS und zeigte eine Gruppe Teenager, die Mysterien loesen, zusammen mit einem grossen Hund?",
        answerA = "Josie and the Pussycats",
        answerB = "Scooby-Doo, Where Are You!",
        answerC = "The Archie Show",
        answerD = "Wacky Races",
        correctAnswer = 1,
        explanation = "'Scooby-Doo, Where Are You!' startete am 13. September 1969 bei CBS und war eine Produktion von Hanna-Barbera. Die Serie lief in verschiedenen Formen bis heute und wurde zu einem der langlebigsten Zeichentrick-Franchises.",
        difficulty = 2,
        funFact = "Die Originalidee von Scooby-Doo war deutlich gruseliger. CBS verlangte eine harmlosere Fassung, woraufhin Hanna-Barbera den Hund als Komikfigur einfuehrte."
    ),

    // ─── Mixed TV Topics ──────────────────────────────────────────────────────

    // Question 38
    Question(
        categoryId = 4,
        questionText = "Welche US-Talkshow moderierte Johnny Carson von 1962 bis 1992?",
        answerA = "The Ed Sullivan Show",
        answerB = "The Tonight Show",
        answerC = "Late Night with Johnny Carson",
        answerD = "The Johnny Carson Show",
        correctAnswer = 1,
        explanation = "Johnny Carson moderierte 'The Tonight Show' bei NBC 30 Jahre lang von 1962 bis 1992 und pragte damit das amerikanische Spaetnacht-Fernsehen wie kein anderer.",
        difficulty = 2,
        funFact = "Johnny Carsons letzte Sendung am 22. Mai 1992 sahen 50 Millionen Zuschauer. Sein Nachfolger Jay Leno trat das schwere Erbe an."
    ),

    // Question 39
    Question(
        categoryId = 4,
        questionText = "Welche britische Comedyserie parodierte Krankenhaeuser und lief von 1994 bis 1995 bei BBC?",
        answerA = "Father Ted",
        answerB = "Blackadder",
        answerC = "Green Wing",
        answerD = "Cardiac Arrest",
        correctAnswer = 3,
        explanation = "'Cardiac Arrest' war eine britische Dramaserie, die 1994-1996 bei BBC lief und den Alltag eines Krankenhauses auf realistische und teils satirische Weise zeigte.",
        difficulty = 2,
        funFact = null
    ),

    // Question 40
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr startete 'Game of Thrones' bei HBO?",
        answerA = "2009",
        answerB = "2010",
        answerC = "2011",
        answerD = "2012",
        correctAnswer = 2,
        explanation = "'Game of Thrones' startete am 17. April 2011 bei HBO und basierte auf George R.R. Martins Romanreihe 'A Song of Ice and Fire'. Die Serie lief bis 2019 ueber 8 Staffeln.",
        difficulty = 2,
        funFact = "David Benioff und D.B. Weiss mussten George R.R. Martin davon ueberzeugen, die Verfilmungsrechte zu erhalten - Martin testete sie mit einer einzigen Frage ueber Jon Snows Mutter."
    ),

    // Question 41
    Question(
        categoryId = 4,
        questionText = "Wie viele Emmy Awards gewann 'Game of Thrones' als Gesamtzahl waehrend seiner Laufzeit?",
        answerA = "38",
        answerB = "47",
        answerC = "59",
        answerD = "71",
        correctAnswer = 2,
        explanation = "'Game of Thrones' gewann insgesamt 59 Emmy Awards bei 161 Nominierungen und stellte damit einen Rekord fuer die meisten Emmys einer Dramaserie auf.",
        difficulty = 2,
        funFact = null
    ),

    // Question 42
    Question(
        categoryId = 4,
        questionText = "Welcher Sender strahlte 'Twin Peaks' von David Lynch erstmals aus?",
        answerA = "HBO",
        answerB = "ABC",
        answerC = "NBC",
        answerD = "CBS",
        correctAnswer = 1,
        explanation = "'Twin Peaks' von David Lynch und Mark Frost lief 1990-1991 bei ABC und gilt als eine der einflussreichsten Serien der Fernsehgeschichte, die moderne Qualitaetsserien massgeblich beeinflusste.",
        difficulty = 2,
        funFact = "Die Frage 'Wer hat Laura Palmer ermordet?' wurde zum kulturellen Phaenomen und verdoppelte zunaechst die Einschaltquoten von ABC."
    ),

    // Question 43
    Question(
        categoryId = 4,
        questionText = "Auf welchem Buch von Stephan King basiert die Miniserie 'IT' (1990)?",
        answerA = "Shining",
        answerB = "Carrie",
        answerC = "It",
        answerD = "Pet Sematary",
        correctAnswer = 2,
        explanation = "Die zweiteilige Miniserie 'IT' (1990) basiert auf Stephen Kings gleichnamigem Roman von 1986 und wurde zu einem der meistgesehenen Fernsehfilme jener Aera.",
        difficulty = 2,
        funFact = "Tim Curry als Pennywise der tanzende Clown wurde zu einer Ikone des Horrors und beeinflusste zahlreiche spaetere Clown-Darstellungen in der Populaerkultur."
    ),

    // Question 44
    Question(
        categoryId = 4,
        questionText = "Welches Netzwerk strahlte 'The Wire' aus?",
        answerA = "NBC",
        answerB = "AMC",
        answerC = "HBO",
        answerD = "Showtime",
        correctAnswer = 2,
        explanation = "'The Wire' lief von 2002 bis 2008 auf HBO. Das Netzwerk galt als Heimat anspruchsvoller Dramaserien wie 'The Sopranos', 'Six Feet Under' und spaeter 'True Detective'.",
        difficulty = 2,
        funFact = "Trotz kritischer Anerkennung waren die Einschaltquoten von 'The Wire' stets niedrig - die Serie gewann nie einen Emmy, obwohl sie regelmaessig auf Listen der besten Serien aller Zeiten erscheint."
    ),

    // Question 45
    Question(
        categoryId = 4,
        questionText = "Welche Figur ist das Herzstuck von 'The Wire' und dient als moralischer Kompass der Serie?",
        answerA = "Jimmy McNulty",
        answerB = "Bunk Moreland",
        answerC = "Omar Little",
        answerD = "Stringer Bell",
        correctAnswer = 0,
        explanation = "Jimmy McNulty (gespielt von Dominic West) ist der zentrale Protagonist der ersten Staffeln. Er ist ein brillanter, aber eigensinniger Mordermittler beim Baltimore Police Department.",
        difficulty = 2,
        funFact = "Dominic West ist Brite und musste einen amerikanischen Akzent lernen, um McNulty zu spielen. Viele Zuschauer wussten lange nicht, dass er kein Amerikaner ist."
    ),

    // Question 46
    Question(
        categoryId = 4,
        questionText = "Welchen Zeitraum der amerikanischen Geschichte deckt 'Mad Men' ab?",
        answerA = "1950er bis fruehe 1960er",
        answerB = "Spaete 1950er bis fruehe 1970er",
        answerC = "1960er bis 1980er",
        answerD = "Fruehe 1960er bis 1970",
        correctAnswer = 1,
        explanation = "'Mad Men' (2007-2015) spielt zwischen 1960 und 1970 und zeigt den gesellschaftlichen Wandel Amerikas: Burgerrechtsbewegung, Frauenbefreiung, Vietnamkrieg und das Ende der unschuldigen 1960er.",
        difficulty = 2,
        funFact = "Creator Matthew Weiner bewarb sich mit dem Mad-Men-Pilot bei einer Talkshow-Produktion und kam so in Kontakt mit den Machern von 'The Sopranos', wo er als Autor arbeitete."
    ),

    // Question 47
    Question(
        categoryId = 4,
        questionText = "Welche Fernsehserie gilt als erste wirklich erfolgreiche Sci-Fi-Serie des deutschen Fernsehens?",
        answerA = "Raumpatrouille Orion",
        answerB = "Perry Rhodan",
        answerC = "Das Himmelsschiff",
        answerD = "Die Galaxis",
        correctAnswer = 0,
        explanation = "'Raumpatrouille Orion' startete am 17. September 1966 bei ARD - am gleichen Tag wie 'Star Trek' in den USA. Die sieben Episoden dauernde Serie gilt als Meilenstein des deutschen Science-Fiction-Fernsehens.",
        difficulty = 2,
        funFact = "Raumpatrouille Orion wurde mit einem Budget produziert, das kaum einen Bruchteil amerikanischer Produktionen ausmachte - Alufolie, Spiegel und Plastikteile ersetzten teure Spezialeffekte."
    ),

    // Question 48
    Question(
        categoryId = 4,
        questionText = "Welche britische Zeichentrickserie zeigte ab 1974 Geschichten aus der Bibel?",
        answerA = "Auggie Doggie",
        answerB = "The Animated Bible",
        answerC = "Daydream Believer",
        answerD = "The Animated Stories from the New Testament",
        correctAnswer = 1,
        explanation = "Zahlreiche Zeichentrickserien mit biblischen Themen entstanden in den 1970er und 1980er Jahren. Bekannt sind vor allem die Hanna-Barbera-Produktion 'The Greatest Adventure: Stories from the Bible' (1985-1988).",
        difficulty = 2,
        funFact = null
    ),

    // Question 49
    Question(
        categoryId = 4,
        questionText = "Welche amerikanische Animationsserie, die ab 1972 auf CBS lief, war bekannt als erste Zeichentrickserie fuer Kinder, die Umweltschutz-Botschaften thematisierte?",
        answerA = "Superfriends",
        answerB = "Captain Planet",
        answerC = "The Smokey Bear Show",
        answerD = "Lassie's Rescue Rangers",
        correctAnswer = 3,
        explanation = "'Lassie's Rescue Rangers' (1973-1975) war eine der ersten Zeichentrickserien, die Umweltschutz explizit thematisierte. 'Captain Planet' folgte erst 1990 als bekanntere Umweltschutzserie.",
        difficulty = 2,
        funFact = "Captain Planet wurde von Ted Turner entwickelt und war ein direktes Resultat seines Engagements fuer Umweltschutz und seine Gruendung des Cable News Network CNN."
    ),

    // Question 50
    Question(
        categoryId = 4,
        questionText = "Welche Animationsserie von Seth MacFarlane, die seit 1999 laeuft, ist bekannt fuer ihre kontroversen und satirischen Inhalte?",
        answerA = "American Dad",
        answerB = "Family Guy",
        answerC = "The Orville",
        answerD = "Cleveland Show",
        correctAnswer = 1,
        explanation = "'Family Guy' startete am 31. Januar 1999 auf Fox und wurde zweimal abgesetzt (2002 und 2003), aber aufgrund starker DVD-Verkauefe und Einschaltquoten auf Cartoon Network wiederbelebt.",
        difficulty = 2,
        funFact = "Seth MacFarlane entwickelte 'Family Guy', als er 24 Jahre alt war. Nach der Absetzung wurden die DVD-Verkauefe so stark, dass Fox die Serie 2005 wieder aufnahm."
    )

)
