package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsMedium3(): List<Question> = listOf(

    // ── Konzerte und Live-Auftritte (8 Fragen) ────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches Konzert der Rolling Stones 2006 auf der Copacabana in Rio de Janeiro gilt als eines der meistbesuchten aller Zeiten?",
        answerA = "Voodoo Lounge Tour",
        answerB = "Bridges to Babylon Tour",
        answerC = "A Bigger Bang Tour",
        answerD = "No Filter Tour",
        correctAnswer = 2,
        explanation = "Das Gratiskonzert der Rolling Stones am 18. Februar 2006 auf der Copacabana im Rahmen der A Bigger Bang Tour zog schaetzungsweise 1,5 Millionen Zuschauer an und gilt als eines der groessten Konzerte der Musikgeschichte.",
        difficulty = 2,
        funFact = "Der brasilianische Regierung half bei der Logistik: Busse fuhren kostenlos, und Millionen reisten aus ganz Brasilien an. Die Band spielte fast zwei Stunden ohne Pause."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Kuenstler gilt mit seiner residency in Las Vegas als einer der groessten Concert-Draws des 21. Jahrhunderts und spielte ab 2007 im Colosseum des Caesars Palace?",
        answerA = "Celine Dion",
        answerB = "Elton John",
        answerC = "Rod Stewart",
        answerD = "Barbra Streisand",
        correctAnswer = 0,
        explanation = "Celine Dions 'A New Day...' Residency (2003-2007) und spaetere Shows im Caesars Palace setzten den Standard fuer Las-Vegas-Residencies. Mit ueber 1.100 Auftritten war es die laengste Residency eines Hauptacts in der Geschichte von Las Vegas.",
        difficulty = 2,
        funFact = "Das Colosseum im Caesars Palace wurde speziell fuer Celine Dion gebaut und fasst 4.298 Zuschauer. Die Buehne kostete 95 Millionen Dollar und ist eine der aufwendigsten permanenten Konzertbuehnen der Welt."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie heisst der Begriff fuer ein Konzert, bei dem ein Kuenstler mehrere aufeinanderfolgende Abende an einem einzigen Ort spielt?",
        answerA = "Sold-Out Run",
        answerB = "Residency",
        answerC = "Stadium Series",
        answerD = "Encore Circuit",
        correctAnswer = 1,
        explanation = "Eine 'Residency' bezeichnet eine Serie von Konzerten eines Kuenstlers an einem festen Ort ueber mehrere Abende oder Wochen. Las Vegas ist bekannt fuer solche Residencies, bei denen Kuenstler wie Elton John, Bruno Mars oder Adele regelmaessig auftreten.",
        difficulty = 2,
        funFact = "Bruno Mars spielte ueber 80 Abende in Las Vegas im Park MGM und verdiente dabei Berichten zufolge ueber 90 Millionen Dollar – mehr als mit jeder seiner Welttourneen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Kuenstler spielte 2023 im Rahmen seiner 'Eras Tour' und brach dabei Rekorde als meistbesuchte Tour aller Zeiten?",
        answerA = "Beyonce",
        answerB = "Taylor Swift",
        answerC = "Harry Styles",
        answerD = "Ed Sheeran",
        correctAnswer = 1,
        explanation = "Taylor Swifts Eras Tour (2023-2024) wurde mit einem Umsatz von ueber einer Milliarde Dollar zur finanziell erfolgreichsten Konzerttournee der Geschichte und uebertraf sogar Elton Johns Abschiedstournee.",
        difficulty = 2,
        funFact = "Oekonomisten errechneten, dass die Eras Tour in einzelnen Stadten den Tourismus-Umsatz um bis zu 100 Millionen Dollar steigerte. In Schweden wurde sogar eine messbare Wirkung auf den BIP des Landes diskutiert."
    ),

    Question(
        categoryId = 5,
        questionText = "Der legendaere Auftritt von Jimi Hendrix beim Woodstock-Festival 1969 endete auf Montagmorgen. Welches Lied spielte er zum Abschluss, das zum Symbol des Protests gegen den Vietnamkrieg wurde?",
        answerA = "Purple Haze",
        answerB = "All Along the Watchtower",
        answerC = "The Star-Spangled Banner",
        answerD = "Hey Joe",
        correctAnswer = 2,
        explanation = "Jimi Hendrix' psychedelische Interpretation der US-Nationalhymne 'The Star-Spangled Banner' mit Gitarrensimulationen von Bombern und Schreien galt als radikaler Antikriegs-Kommentar und wurde zum ikonischsten Moment des Woodstock-Festivals.",
        difficulty = 2,
        funFact = "Hendrix trat als letzter Act bei Woodstock auf, weil sein Management auf dem Headliner-Slot bestanden hatte. Da das Festival massiv in Verzug war, spielte er erst Montag frueh – vor nur noch 30.000 der urspruenglich 400.000 Zuschauer."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Musikformat bezeichnet einen unverstarkten Liveauftritt eines normalerweise elektrisch spielenden Kuenstlers, popularisiert durch eine MTV-Sendereihe?",
        answerA = "Stripped",
        answerB = "Acoustic Session",
        answerC = "Unplugged",
        answerD = "Raw Show",
        correctAnswer = 2,
        explanation = "'MTV Unplugged' startete 1989 und praesentierte Rock- und Pop-Kuenstler in akustischen Live-Settings. Nirvanas Unplugged-Album (1993) und Eric Claptons Layla-Version wurden besonders beruehmt und kommerziell aeusserst erfolgreich.",
        difficulty = 2,
        funFact = "Eric Claptons MTV-Unplugged-Auftritt 1992 brachte seine Version von 'Layla' hervor, die sich stilistisch so stark vom elektrischen Original unterschied, dass viele Zuhoerer nicht erkannten, welchen Song er spielte. Das Album verkaufte sich ueber 10 Millionen Mal."
    ),

    Question(
        categoryId = 5,
        questionText = "Bei welchem historischen Livekonzert 1956 tanzte Elvis Presley so freizuegig, dass die Kameramanner beim zweiten Auftritt in der Ed Sullivan Show angewiesen wurden, nur von der Huefte aufwaerts zu filmen?",
        answerA = "American Bandstand",
        answerB = "The Ed Sullivan Show",
        answerC = "The Tonight Show",
        answerD = "The Milton Berle Show",
        correctAnswer = 3,
        explanation = "Elvis trat im Juni 1956 in der Milton Berle Show auf und tanzte in einer Art, die als anstoeessig galt. Daraufhin wurden spaetere TV-Auftritte bewusst zensiert. Bei der Ed Sullivan Show wurde er tatsaechlich erst 1957 nur von der Huefte aufwaerts gefilmt.",
        difficulty = 2,
        funFact = "Trotz der Zensur schauten 82,6% aller TV-Zuschauer Amerikas Presley bei seiner Ed-Sullivan-Premiere – ein Rekord, der jahrzehntelang ungebrochen blieb und zeigt, welchen kulturellen Einfluss er hatte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Konzerttournee der 1990er Jahre gilt als die umsatzstaerkste ihrer Aera und wurde von The Rolling Stones im Rahmen ihrer 'Voodoo Lounge Tour' unternommen?",
        answerA = "1994-1995, ca. 320 Millionen Dollar Umsatz",
        answerB = "1997-1998, ueber 400 Millionen Dollar Umsatz",
        answerC = "1999-2000, ueber 500 Millionen Dollar Umsatz",
        answerD = "1992-1993, ca. 200 Millionen Dollar Umsatz",
        correctAnswer = 0,
        explanation = "Die Voodoo Lounge Tour (1994-1995) war die bis dahin umsatzstaerkste Konzerttournee aller Zeiten und brachte den Rolling Stones ca. 320 Millionen Dollar ein. Sie tourten durch Nordamerika, Europa und den Fernen Osten.",
        difficulty = 2,
        funFact = "Die Buehnenkonstruktion der Voodoo Lounge Tour war so gigantisch, dass fuer jeden Stadionauftritt eigene Baukraenoepquipment und ein komplettes Aufbau-Team von mehreren Hundert Personen benoetigt wurde."
    ),

    // ── Musiktheater und Musical (8 Fragen) ───────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches Andrew-Lloyd-Webber-Musical basiert auf dem Buch 'Das Phantom der Oper' von Gaston Leroux und wurde 1986 in London uraufgefuehrt?",
        answerA = "Cats",
        answerB = "The Phantom of the Opera",
        answerC = "Evita",
        answerD = "Sunset Boulevard",
        correctAnswer = 1,
        explanation = "Das Phantom der Oper hatte seine Weltpremiere am 9. Oktober 1986 im Her Majesty's Theatre in London. Das Musical wurde zu einem der laengstlaufenden Shows am Broadway und West End und hat weltweit ueber 70 Milliarden Dollar eingespielt.",
        difficulty = 2,
        funFact = "Das Phantom der Oper lief am Broadway von 1988 bis 2023 – 35 Jahre lang mit ueber 13.000 Vorstellungen. Kein anderes Musical in der Broadway-Geschichte hat laenger gespielt."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Musical von Lin-Manuel Miranda erzaehlt die Geschichte von Alexander Hamilton und kombiniert Hip-Hop mit Buehnendrama?",
        answerA = "In the Heights",
        answerB = "Hamilton",
        answerC = "Rent",
        answerD = "Dear Evan Hansen",
        correctAnswer = 1,
        explanation = "'Hamilton' (2015) ist ein revolutionaeres Musical, das die Geschichte des Gruendervaters Alexander Hamilton mit Hip-Hop, R&B und Jazz erzaehlt. Es gewann 11 Tony Awards und veraenderte das Broadway-Musical nachhaltig.",
        difficulty = 2,
        funFact = "Lin-Manuel Miranda bekam die Idee zu Hamilton, als er Ron Chernows Hamilton-Biographie im Urlaub las. Er begann, die Texte als Rap umzuschreiben und praesentierte eine fruehe Version bei einem Abend im Weissen Haus vor Barack Obama."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Musical spielt in der Nachkriegszeit in Wien und basiert auf den ABBA-Songs, ohne jedoch direkt die ABBA-Geschichte zu erzaehlen?",
        answerA = "Muriel's Wedding",
        answerB = "Mamma Mia!",
        answerC = "Rock of Ages",
        answerD = "Jersey Boys",
        correctAnswer = 1,
        explanation = "'Mamma Mia!' (1999) ist ein Jukebox-Musical, das ABBA-Songs zu einer neuen Geschichte verbindet. Es spielt auf einer griechischen Insel, nicht in Wien. Die Show wurde zu einer der meistgespielten Musicals weltweit.",
        difficulty = 2,
        funFact = "Mamma Mia! wurde von Judy Craymer produziert, die ABBA jahrelang um die Rechte bat, bevor Benny Andersson und Bjorn Ulvaeus einwilligten. Sie hatten selbst nie ein Musical geschrieben und ueberliessen alles kreative Songauswahl der Buehnenproduktion."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Buehnenprogramm fuer Kinder- und Familienunterhaltung, basierend auf dem Disney-Animationsfilm von 1994, laeuft seit 1997 am Broadway?",
        answerA = "Beauty and the Beast",
        answerB = "The Little Mermaid",
        answerC = "The Lion King",
        answerD = "Aladdin",
        correctAnswer = 2,
        explanation = "The Lion King (Buehnenpremiere 1997 in Minneapolis, Broadway-Premiere November 1997) ist bekannt fuer seine aufwendigen Kostume und Tierfiguren-Buehnenbilder von Julie Taymor. Es ist eines der finanziell erfolgreichsten Musicals aller Zeiten.",
        difficulty = 2,
        funFact = "Die Regisseurin Julie Taymor entschied sich bewusst, die Tierfiguren sichtbar als Puppen und Kostume darzustellen – nicht als echte Tiere. Das Publikum sollte den Theaterkuenstlern zuschauen. Diese Entscheidung machte das Musical kunstlerisch einzigartig."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Musical, uraufgefuehrt 1957, erzaehlt eine moderne Romeo-und-Julia-Geschichte zwischen verfeindeten Jugendgangs in New York City?",
        answerA = "Grease",
        answerB = "West Side Story",
        answerC = "Hair",
        answerD = "Chicago",
        correctAnswer = 1,
        explanation = "West Side Story hatte seine Uraufführung am Broadway am 26. September 1957. Das Musical von Leonard Bernstein (Musik), Stephen Sondheim (Texte) und Arthur Laurents (Buch) gilt als eines der grössten Musicals der Theatergeschichte.",
        difficulty = 2,
        funFact = "Stephen Sondheim war anfaenglich zoeghaft, die Texte zu West Side Story zu schreiben, weil er eigene Musicals schreiben wollte. Oscar Hammerstein II ueberredete ihn mit dem Argument, es sei eine einmalige Lernchance bei Leonard Bernstein."
    ),

    Question(
        categoryId = 5,
        questionText = "Das Musical 'Cats' basiert auf einem Gedichtband. Wer schrieb diese Gedichte, auf denen Andrew Lloyd Webber sein Musical aufbaute?",
        answerA = "T.S. Eliot",
        answerB = "W.B. Yeats",
        answerC = "Rudyard Kipling",
        answerD = "Dylan Thomas",
        correctAnswer = 0,
        explanation = "'Old Possum's Book of Practical Cats' von T.S. Eliot (1939) war die literarische Vorlage fuer Andrew Lloyd Webbers 'Cats' (1981). Das Musical lief 21 Jahre am Broadway und wurde eines der meistgespielten der Geschichte.",
        difficulty = 2,
        funFact = "T.S. Eliot schrieb die Katzengedichte urspruenglich fuer seine Patenkinder. Er starb 1965 und konnte das Musical nicht mehr erleben. Aber seine Witwe Valerie Eliot half Lloyd Webber dabei, unveroffentlichte Gedichte Eliots fuer das Finale zu nutzen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Musical beruehmt sich damit, die erste Hip-Hop-Ballade auf dem Broadway zu praesentieren, und handelt von armen Kuenstlern im New York der 1990er?",
        answerA = "Avenue Q",
        answerB = "Rent",
        answerC = "Spring Awakening",
        answerD = "Next to Normal",
        correctAnswer = 1,
        explanation = "'Rent' (1996) von Jonathan Larson ist eine moderne Adaption der Oper 'La Boheme' und spielt im New Yorker East Village waehrend der AIDS-Krise. Das Musical gewann den Pulitzer-Preis und vier Tony Awards.",
        difficulty = 2,
        funFact = "Jonathan Larson starb in der Nacht nach der ersten Generalprobe an einem Aortenaneurysma – er erlebte den sensationellen Erfolg seines Werkes nie. 'Rent' wurde posthum zu einem der einflussreichsten Musicals der 90er Jahre."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Musical, das auf dem Leben und den Songs von Tina Turner basiert, feierte 2018 seine Weltpremiere?",
        answerA = "What's Love Got to Do with It",
        answerB = "Simply the Best",
        answerC = "Tina – The Tina Turner Musical",
        answerD = "Proud Mary",
        correctAnswer = 2,
        explanation = "'Tina – The Tina Turner Musical' hatte seine Weltpremiere im November 2018 am Aldwych Theatre in London. Es basiert auf Turners Autobiographie 'I, Tina' und erzaehlt ihr Leben von der Kindheit bis zum Comeback.",
        difficulty = 2,
        funFact = "Tina Turner war an der Produktion des Musicals beteiligt und gab persoenlich ihren Segen. Sie sagte, sie wollte, dass junge Frauen ihre Geschichte kennen, damit sie wissen, dass man aus schwierigsten Umstaenden aufstehen kann."
    ),

    // ── Vinyl und Schallplatten (7 Fragen) ────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Mit welcher Geschwindigkeit drehen sich Standard-LPs (Langspielplatten) auf einem Plattenspieler?",
        answerA = "45 RPM",
        answerB = "78 RPM",
        answerC = "33 1/3 RPM",
        answerD = "16 RPM",
        correctAnswer = 2,
        explanation = "Standard-LPs drehen sich mit 33 1/3 Umdrehungen pro Minute (RPM). Singles drehen sich meist mit 45 RPM, aeltere Schellack-Platten mit 78 RPM. Einige Spezialplatten nutzen 16 RPM fuer Sprachaufnahmen.",
        difficulty = 2,
        funFact = "Die 33 1/3-RPM-Geschwindigkeit fuer LPs wurde 1948 von Columbia Records eingefuehrt. RCA Victor konterte ein Jahr spaeter mit dem 45-RPM-Format fuer Singles – was zu einem der ersten grossen 'Formatkriege' der Unterhaltungselektronik fuehrte."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnet man als 'Pressing' im Kontext von Schallplatten?",
        answerA = "Den Vorgang des Aufschneidens der Rillen",
        answerB = "Das physische Herstellungsverfahren, bei dem eine Vinylplatte aus einer Mastermatrize gepresst wird",
        answerC = "Das Verpacken der fertigen Schallplatte",
        answerD = "Das Mastern der Audio-Aufnahme",
        correctAnswer = 1,
        explanation = "Als 'Pressing' bezeichnet man den industriellen Vorgang, bei dem erwaermtes Vinylmaterial unter hohem Druck in eine Metallmatrize (Stamper) gepresst wird, um die Rillen zu erzeugen. Erstpressungen sind bei Sammlern oft besonders wertvoll.",
        difficulty = 2,
        funFact = "Original-Erstpressungen beruehm ter Alben wie 'The Dark Side of the Moon' von Pink Floyd oder 'Nevermind' von Nirvana koennen bei Auktionen tausende Euro erzielen – besonders wenn sie noch versiegelt sind."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Farbe hat eine Standard-Vinylschallplatte, und woraus besteht sie hauptsaechlich?",
        answerA = "Schwarz, aus PVC (Polyvinylchlorid)",
        answerB = "Schwarz, aus Schellack (Naturharz)",
        answerC = "Grau, aus Polycarbonat",
        answerD = "Dunkelblau, aus Acrylharz",
        correctAnswer = 0,
        explanation = "Standard-Vinylplatten sind schwarz und bestehen hauptsaechlich aus PVC (Polyvinylchlorid) mit Russpigmenten. Farbige Pressungen aus demselben Material sind moglich und bei Sammlern beliebt, klingen aber manchmal schlechter.",
        difficulty = 2,
        funFact = "Schellack-Platten (78 RPM), die Vorgaenger der Vinyl-LPs, bestanden aus Naturharz und waren viel zerbrechlicher. Viele wertvolle Aufnahmen aus der fruehen Jazzzeit existieren nur noch auf diesen fragilen Schellack-Platten."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Vinyl-Album ist laut dem Magazin Goldmine mit einer Erstpressung von 1963 eines der wertvollsten Sammlerstuecke und stammt von den Beatles?",
        answerA = "Please Please Me (erster UK-Press)",
        answerB = "Help! (Original-Mono-Pressing)",
        answerC = "Meet the Beatles (US-Erstpressung)",
        answerD = "With the Beatles (Original Parlophone)",
        correctAnswer = 0,
        explanation = "Die Mono-Erstpressung von 'Please Please Me' (1963) auf Parlophone mit schwarzem und goldgelbem Label gilt als eine der wertvollsten Schallplatten der Welt. Gut erhaltene Exemplare wurden fuer ueber 10.000 Pfund versteigert.",
        difficulty = 2,
        funFact = "Beatles-Platten der fruehen 60er Jahre auf dem schwarzen Parlophone-Label mit goldener Aufschrift sind extrem selten, da sie nach wenigen Pressungen durch andere Labeldesigns ersetzt wurden. Serioes erhaltene Exemplare sind ein Traum fuer Sammler."
    ),

    Question(
        categoryId = 5,
        questionText = "Ab welchem Jahrzehnt erlebte Vinyl ein kommerzielles Revival und ueberholte in manchen Laendern wieder den CD-Verkauf?",
        answerA = "1990er",
        answerB = "2000er",
        answerC = "2010er",
        answerD = "2020er",
        correctAnswer = 2,
        explanation = "Das Vinyl-Revival begann um 2007-2008 und verstaeake sich in den 2010er Jahren deutlich. In den USA uebertrafen Vinyl-Alben 2020 erstmals seit Jahrzehnten wieder den CD-Verkauf in Stueckzahlen. Youngere Hoerer entdeckten das Format neu.",
        difficulty = 2,
        funFact = "Taylor Swift ist eine der groessten Treiber des Vinyl-Booms. Ihre Alben brachen regelmaeßig Vinyl-Verkaufsrekorde. 'Midnights' (2022) wurde in einer Woche oefter auf Vinyl verkauft als jedes andere Album seit den fruehen 1990er Jahren."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnet man als 'Mastering' bei der Schallplattenproduktion?",
        answerA = "Das Aufnehmen der Musiker im Studio",
        answerB = "Den finalen Schritt der Klangoptimierung und Uebertragung auf den Schneidestichel fuer das Lacquermaster",
        answerC = "Das Vervielfaeltigen der fertigen Platten",
        answerD = "Das Pruefen der Plattenqualitaet nach der Pressung",
        correctAnswer = 1,
        explanation = "Mastering ist der letzte Schritt der Tonbearbeitung, bei dem die Stereo-Mischung klanglich optimiert und auf ein Lacquer-Disc geschnitten wird. Von diesem Lacquer werden Stempel gefertigt, die dann tausende Platten pressen.",
        difficulty = 2,
        funFact = "Der legendaere Mastering-Engineer Bob Ludwig hat mehr als 3.000 Alben gemastert, darunter Klassiker von Springsteen, Led Zeppelin und U2. Ein gut gemaster tes Vinyl klingt nach Ansicht vieler Hoerer waermer als jede digitale Quelle."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Schallplattenformat mit 17 cm Durchmesser und 45 RPM wurde hauptsaechlich fuer Singles verwendet?",
        answerA = "Maxi-Single",
        answerB = "EP (Extended Play)",
        answerC = "7-Inch-Single",
        answerD = "12-Inch-Maxi",
        correctAnswer = 2,
        explanation = "Die 7-Inch-Single (ca. 17 cm Durchmesser, 45 RPM) war von den 1950ern bis in die CD-Aera das Standard-Format fuer Hitsingle-Veroeffentlichungen. Auf jeder Seite passen ca. 5-6 Minuten Musik.",
        difficulty = 2,
        funFact = "Manche 7-Inch-Singles haben eine grosse Mittelbohrung ('Jukebox-Center'), damit sie in alten Jukeboxen laufen. Diese wurden gesondert fuer Jukeboxen produziert und sind heute seltene Sammlerexemplare."
    ),

    // ── Musikindustrie-Meilensteine (7 Fragen) ────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "In welchem Jahr wurde Napster gegruendet, die erste grosse Tauschboerse fuer MP3-Dateien, die die Musikindustrie erschuetterte?",
        answerA = "1997",
        answerB = "1999",
        answerC = "2001",
        answerD = "2003",
        correctAnswer = 1,
        explanation = "Napster wurde 1999 von Shawn Fanning und Sean Parker gegruendet und ermoeglichte kostenlose Peer-to-Peer-Musik-Tausch. Auf dem Hoehepunkt hatte es 80 Millionen Nutzer. Klagen der RIAA und einzelner Kuenstler erzwangen die Schliessung 2001.",
        difficulty = 2,
        funFact = "Metallica waren unter den ersten Kuenstlern, die Napster verklagten, nachdem sie herausgefunden hatten, dass ein unveroffentlichter Demo-Track ('I Disappear') massenhaft getauscht wurde. Die Klage machte Metallicas Lars Ulrich bei Fans sehr unpopulaer."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Ereignis im Jahr 1983 wird als Beginn der digitalen Musik-Aera fuer Endverbraucher bezeichnet?",
        answerA = "Die Einfuehrung der Cassette",
        answerB = "Die Marktinfuehrung der Compact Disc (CD) in Europa",
        answerC = "Die ersten digitalen Radio-Stationen",
        answerD = "Die Entwicklung des Walkman",
        correctAnswer = 1,
        explanation = "Die Compact Disc wurde 1982 in Japan eingefuehrt und ab 1983 in Europa und Nordamerika vermarktet. Sie laeut ete das digitale Zeitalter fuer den Musikkonsum ein und verdraengte innerhalb weniger Jahre die Schallplatte als dominantes Format.",
        difficulty = 2,
        funFact = "Die erste kommerziell veroeffentlichte CD war Billy Joels '52nd Street' in Japan (1. Oktober 1982). Das erste CD-Abspielgeraet, der Sony CDP-101, kostete umgerechnet fast 1.000 Euro in heutiger Kaufkraft."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Plattenlabel, 1958 gegrndet, gilt als das praegendste Label der Rock-Geschichte und veroeffentlichte Alben von Led Zeppelin und den Rolling Stones?",
        answerA = "Capitol Records",
        answerB = "Atlantic Records",
        answerC = "Columbia Records",
        answerD = "Decca Records",
        correctAnswer = 1,
        explanation = "Atlantic Records, gegruendet 1947 von Ahmet und Nesuhi Ertegun, veroeffentlichte Musik von Led Zeppelin, den Rolling Stones, Eric Clapton, ABBA und Aretha Franklin. Es gilt als eines der einflussreichsten Labels der Popgeschichte.",
        difficulty = 2,
        funFact = "Ahmet Ertegun gruendete Atlantic Records, weil er leidenschaftlicher Musikliebhaber war, aber keine Plattenfirma bereit war, die schwarzen Bluesmusiker unter Vertrag zu nehmen, die er bewunderte. Atlantic aenderte das."
    ),

    Question(
        categoryId = 5,
        questionText = "Der Begriff 'Payola' bezeichnet in der Musikindustrie eine illegale Praxis. Was ist damit gemeint?",
        answerA = "Steuerhinterziehung durch Plattenfirmen",
        answerB = "Bezahlung von Radiomoderatorern, um bestimmte Songs bevorzugt zu spielen",
        answerC = "Faelschung von Chartpositionen",
        answerD = "Verkauf von Konzerttickets an Wiederverkaeufer",
        correctAnswer = 1,
        explanation = "Payola bezeichnet die illegale Praxis, Radiomoderatorern oder DJs Geld oder Geschenke zu zahlen, damit sie bestimmte Songs bevorzugt spielen. In den USA wurde Payola 1959-60 durch den beruehmten Payola-Skandal publik, in dem DJ Alan Freed verurteilt wurde.",
        difficulty = 2,
        funFact = "Alan Freed, der DJ der den Begriff 'Rock and Roll' populaer machte, verlor durch den Payola-Skandal seinen Job, seinen Reichtum und seine Gesundheit. Er starb 1965 mittellos an Alkohol-Folgeerkrankungen. Dabei war Payola in der Branche weit verbreitet."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches schwedisches Unternehmen revolutionierte ab 2008 die Musikindustrie und zahlte Kuenstlern erstmals Streaming-Lizenzen in grossem Massstab?",
        answerA = "Tidal",
        answerB = "SoundCloud",
        answerC = "Deezer",
        answerD = "Spotify",
        correctAnswer = 3,
        explanation = "Spotify, 2008 lanciert, war der erste grosse kommerzielle Streaming-Dienst, der die Musikindustrie erfolgreich vom Piraterie-Problem ablenkte. Es einigte sich mit den grossen Labels auf ein Lizenzmodell und pragte das Streaming-Zeitalter.",
        difficulty = 2,
        funFact = "Spotify zahlte in seinem ersten Jahrzehnt ueber 10 Milliarden Dollar an Musikrechteinhaber aus. Kuenstler erhalten pro Stream zwischen 0,003 und 0,005 Dollar – was bedeutet, dass ein Song 250.000 mal gestreamt werden muss, um 1.000 Dollar einzubringen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Musiker aktion im Jahr 2000 gilt als Symbol fuer den Kampf von Kuenstlern gegen Plattenfirmen und fuehrte zu einem Gerichtsfall mit Prince?",
        answerA = "Prince schrieb 'SLAVE' auf sein Gesicht und aenderte seinen Namen in ein Symbol",
        answerB = "Michael Jackson verklagte Sony Music wegen Rassismus",
        answerC = "George Michael verweigerte die Zusammenarbeit mit Sony und gewann den Prozess",
        answerD = "David Bowie verkaufte seine Katalogrechte an die Oeffentlichkeit",
        correctAnswer = 0,
        explanation = "Prince schrieb in den 1990ern 'SLAVE' auf seine Wange und aenderte seinen Namen in ein unaussprechliches Symbol, um gegen seinen Plattenvertrag mit Warner Bros. zu protestieren. Er wollte die Kontrolle ueber seinen Musikkatalog zurueck.",
        difficulty = 2,
        funFact = "Nach dem Ende seines Warner-Bros.-Vertrags 1996 erlangte Prince die Kontrolle ueber seine neuen Werke. Er wurde zu einem Vorkampfer fuer Kuenstlerrechte und war einer der ersten grossen Stars, der direkt ueber das Internet Musik verkaufte."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist die RIAA und wofuer steht die Abkuerzung?",
        answerA = "Recording Industry Association of America",
        answerB = "Radio Industry Artists Association",
        answerC = "Rock International Artists Alliance",
        answerD = "Rights Institute for Audio and Acoustics",
        correctAnswer = 0,
        explanation = "Die RIAA (Recording Industry Association of America) ist die wichtigste US-Handelsorganisation der Musikindustrie. Sie vergibt Zertifikate wie Gold, Platin und Diamant fuer hohe Verkaufszahlen und fuehrt Urheberrechtsklagen durch.",
        difficulty = 2,
        funFact = "Die RIAA klagte in den fruehen 2000er Jahren Tausende einzelner Musikpiraten an, darunter eine 12-jaehrige Maidchen und eine Grossmutter. Diese Taktik wurde als PR-Desaster angesehen und machte die Plattenfirmen beim Publikum ausserst unpopulaer."
    ),

    // ── Supergruppen und Nebenprojekte (7 Fragen) ────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welche Supergroup wurde 1968 von Eric Clapton, Ginger Baker und Jack Bruce gegruendet und galt als erste Rockband mit Virtuosen-Status aller Mitglieder?",
        answerA = "Traffic",
        answerB = "Cream",
        answerC = "Blind Faith",
        answerD = "Derek and the Dominos",
        correctAnswer = 1,
        explanation = "Cream (1966-1968) war eine der ersten Rockbands, die aus ausgewiesenen Virtuosen bestand: Eric Clapton (Gitarre), Jack Bruce (Bass, Gesang) und Ginger Baker (Schlagzeug). Mit 'Sunshine of Your Love' und 'White Room' praegten sie den Blues-Rock.",
        difficulty = 2,
        funFact = "Cream loste sich nach nur zwei Jahren trotz grossem Erfolg auf, weil die Rivalitaet zwischen Ginger Baker und Jack Bruce untragbar war. Baker soll Baker ein Messer gezogen haben. Eric Clapton gruendete daraufhin Blind Faith."
    ),

    Question(
        categoryId = 5,
        questionText = "Das Travelling Wilburys war eine Supergroup der spaeten 80er. Welches Mitglied war KEIN Teil der Gruppe?",
        answerA = "George Harrison",
        answerB = "Tom Petty",
        answerC = "Bob Dylan",
        answerD = "Bruce Springsteen",
        correctAnswer = 3,
        explanation = "Die Travelling Wilburys bestanden aus George Harrison, Bob Dylan, Tom Petty, Roy Orbison und Jeff Lynne. Bruce Springsteen war nicht dabei. Das erste Album (1988) wurde ein ueberraschender Hit, obwohl es quasi als Spass entstand.",
        difficulty = 2,
        funFact = "Die Travelling Wilburys wurden aus einer Laune geboren: George Harrison brauchte eine B-Seite fuer eine Single und rief spontan alle Freunde an, die gerade in der Naehe waren. Die Aufnahmesession im Garten von Bob Dylans Haus wurde so gut, dass daraus ein Album wurde."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Nebenprojekt gruendete Paul McCartney nach dem Ende der Beatles und hatte Hits wie 'My Love' und 'Band on the Run'?",
        answerA = "Wings",
        answerB = "Plastic Ono Band",
        answerC = "Quarrymen",
        answerD = "The Fireman",
        correctAnswer = 0,
        explanation = "Wings (1971-1981) war Paul McCartneys Hauptprojekt nach den Beatles. Mit seiner damaligen Frau Linda McCartney und wechselnden Besetzungen hatte die Band grosse Hits. Das Album 'Band on the Run' (1973) gilt als sein bestes Solowerk.",
        difficulty = 2,
        funFact = "Wings wurde vom Musikpublikum anfangs sehr kritisch beaeugt, da die Erwartungen nach den Beatles enorm waren. McCartney setzte sie absichtlich als echte Tourband ein und spielte spontan in kleinen Universitaetssaelen – ohne Vorankuendigung."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Supergroup wurde 1984 von Ex-Mitgliedern von Fleetwood Mac und anderen Stars gegruendet und hatte Hits wie 'You're So Good to Me'?",
        answerA = "Toto",
        answerB = "Asia",
        answerC = "Foreigner",
        answerD = "GTR",
        correctAnswer = 1,
        explanation = "Asia wurde 1981 von John Wetton (King Crimson), Steve Howe (Yes), Carl Palmer (ELP) und Geoff Downes (Yes) gegruendet. Mit dem selbstbetitelten Debuetalum (1982) hatten sie massive Hits wie 'Heat of the Moment'.",
        difficulty = 2,
        funFact = "Asias Debuetalum war 1982 das meistverkaufte Album in den USA. Ein bemerkenswerter Erfolg fuer eine neu gegruendete Supergroup. Das Album verbrachte neun Wochen auf Platz 1 der Billboard-Charts."
    ),

    Question(
        categoryId = 5,
        questionText = "John Lennon hatte ein beruehmt gewordenes Nebenprojekt, bevor er die Beatles verliess. Wie hiess seine Gruppe mit Yoko Ono?",
        answerA = "Plastic Ono Band",
        answerB = "Unfinished Music",
        answerC = "Imagine Project",
        answerD = "Double Fantasy Group",
        correctAnswer = 0,
        explanation = "Die Plastic Ono Band war John Lennons experimentelles Avantgarde-Projekt mit Yoko Ono, das er 1969 – noch als Beatle – gruendete. Das selbstbetitelte Album 'John Lennon/Plastic Ono Band' (1970) gilt als eines seiner kuenstlerisch bedeutendsten Werke.",
        difficulty = 2,
        funFact = "Das 'Cold Turkey'-Single der Plastic Ono Band, aufgenommen 1969, war Lennons deutlichster Hinweis auf seine Drogenprobleme. Er bot das Lied zuerst den Beatles an, aber die anderen lehnten es ab – woraufhin er es als Plastic Ono Band veroeffentlichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Supergroup bestand aus Mitgliedern von CSNY und hatte mit 'Teach Your Children' und 'Our House' Hits?",
        answerA = "Crosby, Stills & Nash",
        answerB = "Crosby, Stills, Nash & Young",
        answerC = "The Byrds mit Neil Young",
        answerD = "Buffalo Springfield",
        correctAnswer = 1,
        explanation = "Crosby, Stills, Nash & Young (CSNY) entstand 1969, als Neil Young zur bereits bestehenden Gruppe CSN stiess. 'Teach Your Children' und 'Our House' stammten vom Woodstock-Album 'Deja Vu' (1970), einem der besten Alben der Folkrock-Aera.",
        difficulty = 2,
        funFact = "Neil Young und Stephen Stills hatten zuvor schon in Buffalo Springfield zusammen gespielt. Die Spannungen zwischen Young und Stills waren legendaer – aber musikalisch ergaenzten sie sich perfekt. CSNY reformierte und trennte sich mehrfach in ihrer Geschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Supergroup-Projekt des Gitarristen Jeff Lynne enthielt neben ihm auch Roy Orbison, Tom Petty, George Harrison und Bob Dylan?",
        answerA = "ELO",
        answerB = "Travelling Wilburys",
        answerC = "Highway 61 Band",
        answerD = "Full Moon Fever",
        correctAnswer = 1,
        explanation = "Die Travelling Wilburys (1988-1990) war Jeff Lynnes Initiative. Er produzierte auch die Soloalben von George Harrison und Roy Orbison zu dieser Zeit. Die Gruppe nutzte Pseudonyme wie 'Lucky Wilbury' (Bob Dylan) und 'Lefty Wilbury' (Roy Orbison).",
        difficulty = 2,
        funFact = "Roy Orbison starb kurz nach der Aufnahme des ersten Travelling-Wilburys-Albums im Dezember 1988. Das zweite Album wurde dann ohne ihn aufgenommen, und sein Part wurde dem fiktiven Bruder 'Lefty Wilbury, Geist' gewidmet."
    ),

    // ── Rapper-Karrieren (7 Fragen) ────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welcher Rapper wird oft als 'Greatest of All Time' (G.O.A.T.) des Hip-Hop bezeichnet und hat Alben wie 'Illmatic' (1994) veroeffentlicht?",
        answerA = "Jay-Z",
        answerB = "Nas",
        answerC = "Eminem",
        answerD = "Rakim",
        correctAnswer = 1,
        explanation = "'Illmatic' (1994) von Nas gilt als eines der besten Hip-Hop-Alben aller Zeiten. Nas wuchs in den Wohnprojekten der Queensbridge Houses auf und etablierte mit seinem Album den lyrischen Standard des Hardcore-Rap.",
        difficulty = 2,
        funFact = "Nas nahm 'Illmatic' auf, als er erst 19 Jahre alt war. Das Album war nur 40 Minuten lang, enthielt aber 10 Tracks, die bis heute als Meisterwerk gelten. Es wurde von Produzenten wie DJ Premier, Large Professor und Pete Rock produziert."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Rapper, bekannt als 'Tupac Shakur', verwendete auch das Pseudonym Makaveli und starb 1996 nach einem Anschlag in Las Vegas?",
        answerA = "Biggie Smalls",
        answerB = "Snoop Dogg",
        answerC = "2Pac",
        answerD = "Ice Cube",
        correctAnswer = 2,
        explanation = "2Pac (Tupac Amaru Shakur) verwendete das Pseudonym Makaveli fuer sein posthum veroeffentlichtes Album 'The Don Killuminati: The 7 Day Theory'. Er wurde am 13. September 1996 nach einem Autoangriff in Las Vegas erschossen.",
        difficulty = 2,
        funFact = "Das Makaveli-Pseudonym bezog sich auf den Philosophen Niccolo Machiavelli, den 2Pac im Gefaengnis gelesen hatte. Machiavelli schrieb einmal ueber das Simulieren des eigenen Todes – was Spekulationen ueber 2Pacs moegliches Ueberleben ausloeeste."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher US-Rapper hatte mit dem Album 'The Marshall Mathers LP' (2000) den schnellsten Verkaufsstart eines Rapalbums und verkaufte in der ersten Woche 1,76 Millionen Kopien?",
        answerA = "Jay-Z",
        answerB = "50 Cent",
        answerC = "Kanye West",
        answerD = "Eminem",
        correctAnswer = 3,
        explanation = "Eminems 'The Marshall Mathers LP' (2000) verkaufte sich in der ersten Woche 1,76 Millionen Mal in den USA – ein Rekord fuer ein Soloalbum. Das Album thematisierte Misogynie, Gewalt und kontroverse Themen, was zu massiver Kritik und Bewunderung fuehrte.",
        difficulty = 2,
        funFact = "Eminem wuchs in groeßter Armut in Detroit auf und wohnte zeitweise in einer Bruchbude ohne Heizung. Dr. Dre entdeckte ihn nach einer Freestyle-Rap-Kassette auf einem Wettbewerb. Von da an stieg seine Karriere so schnell, dass selbst er es kaum glauben konnte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher New Yorker Rapper gruendete das Label Roc-A-Fella Records und zaehlt zu den reichsten Musikern der Welt?",
        answerA = "Puff Daddy (P. Diddy)",
        answerB = "Jay-Z",
        answerC = "Busta Rhymes",
        answerD = "Nas",
        correctAnswer = 1,
        explanation = "Jay-Z (Shawn Carter) gruendete Roc-A-Fella Records 1995 gemeinsam mit Damon Dash und Kareem Burke. Er hat seitdem das Label Bad Boy und spaeter Roc Nation aufgebaut und gilt als einer der erfolgreichsten Unternehmer des Hip-Hop.",
        difficulty = 2,
        funFact = "Jay-Z war vor seinem Musikerfolg als Drogenhändler taetig – ein Leben, das er in vielen seiner Songs offen thematisiert. Er kaufte sich spaeter in verschiedene Unternehmen ein, darunter Armand de Brignac Champagner und D'Usse Cognac."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Rapper, geboren als Kendrick Lamar Duckworth, gewann 2018 als erster nicht-klassischer Musiker den Pulitzer-Preis fuer das Album 'DAMN.'?",
        answerA = "J. Cole",
        answerB = "Kendrick Lamar",
        answerC = "Drake",
        answerD = "Childish Gambino",
        correctAnswer = 1,
        explanation = "Kendrick Lamar gewann 2018 den Pulitzer-Preis fuer Musik fuer sein Album 'DAMN.' (2017) – als erster nicht-klassischer Kuenstler in der Geschichte des Preises. Das Album wurde als poetisch, vielschichtig und kulturell bedeutsam gewuerdigt.",
        difficulty = 2,
        funFact = "Kendrick Lamar wuchs in Compton, Suedkalifornien auf, einem der beruechtigtsten Viertel der USA. Er bezeugte Morde aus naeechster Naehe als Kind. Seine Musik verarbeitet diese Erfahrungen mit einer Lyrik-Dichte, die Literaturkritiker begeistert."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Rapper ist bekannt fuer das Album 'To Pimp a Butterfly' (2015) und gilt als einer der einflussreichsten Kuenstler seiner Generation?",
        answerA = "Tyler, the Creator",
        answerB = "Kendrick Lamar",
        answerC = "Big K.R.I.T.",
        answerD = "ScHoolboy Q",
        correctAnswer = 1,
        explanation = "Kendrick Lamars 'To Pimp a Butterfly' (2015) verbindet Rap mit Jazz, Funk und Spoken Word und thematisiert afroamerikanische Identitaet, Rassismus und Institutionalisierung. Es gilt als eines der wichtigsten Alben der 2010er Jahre.",
        difficulty = 2,
        funFact = "'To Pimp a Butterfly' enthielt ein Telefongespraech mit Tupac, zusammengestellt aus verschiedenen alten Interviewmitschnitten. Kendrick selbst hatte 2Pac als groesste Inspiration und dieses 'Gespraech' galt als sein musikalisches Vermaechtnis."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Rap-Duo der 90er, bestehend aus Big Boi und Andre 3000, praegten den Suedstaaten-Rap mit Alben wie 'Aquemini' (1998)?",
        answerA = "UGK",
        answerB = "Outkast",
        answerC = "8Ball & MJG",
        answerD = "Goodie Mob",
        correctAnswer = 1,
        explanation = "Outkast aus Atlanta, Georgia bestand aus Big Boi (Antwan Patton) und Andre 3000 (Andre Benjamin). 'Aquemini' (1998) und spaeter 'Stankonia' (2000) und 'Speakerboxxx/The Love Below' (2003) definierten den Atlanta-Rap und crossten in den Mainstream.",
        difficulty = 2,
        funFact = "Outkast wurden 1995 bei den Source Awards in New York ausgebuht, als sie den Award fuer beste neue Group erhielten – weil das New Yorker Publikum Suedstaaten-Rap verachtete. Andre 3000 antwortete: 'The South got something to say!' – ein beruehmter Hip-Hop-Moment."
    ),

    // ── Klassik-Grundlagen (6 Fragen) ─────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Wie viele Sinfonien schrieb Ludwig van Beethoven insgesamt?",
        answerA = "7",
        answerB = "9",
        answerC = "11",
        answerD = "12",
        correctAnswer = 1,
        explanation = "Ludwig van Beethoven schrieb neun vollendete Sinfonien. Die 9. Sinfonie in d-Moll op. 125 mit dem 'Ode an die Freude'-Finale ist die bekannteste und wurde 1824 uraufgefuehrt – als Beethoven bereits vollstaendig taub war.",
        difficulty = 2,
        funFact = "Beethoven soll bei der Uraufführung der 9. Sinfonie mit dem Ruecken zum Publikum gestanden und den Takt geschlagen haben – ohne die Musik hoeren zu koennen. Als das Publikum am Ende stuermsich applaudierte, musste eine Saengerin ihn umdrehen, damit er die Ovationen sehen konnte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Epoche der Klassik liegt zwischen Barock und Romantik und ist verbunden mit Komponisten wie Haydn, Mozart und dem fruehen Beethoven?",
        answerA = "Renaissance",
        answerB = "Impressionismus",
        answerC = "Klassik (Wiener Klassik)",
        answerD = "Spaebarock",
        correctAnswer = 2,
        explanation = "Die Wiener Klassik (ca. 1750-1820) war die Epoche nach dem Barock und vor der Romantik. Ihre wichtigsten Vertreter waren Joseph Haydn, Wolfgang Amadeus Mozart und Ludwig van Beethoven. Kennzeichen sind klare Strukturen, Ausgewogenheit und melodioese Eleganz.",
        difficulty = 2,
        funFact = "Die Bezeichnung 'Wiener Klassik' entstand erst im 19. Jahrhundert. Zur Zeit von Haydn, Mozart und Beethoven nannten diese Komponisten ihre eigene Musik nicht 'klassisch' – das war ein Rueckblick der Musikgeschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Werk von Johann Sebastian Bach, ein Zyklus von 48 Praeludi en und Fugen, gilt als Fundament der Klavierliteratur?",
        answerA = "Goldberg-Variationen",
        answerB = "Das Wohltemperierte Klavier",
        answerC = "Die Kunst der Fuge",
        answerD = "Inventionen und Sinfonien",
        correctAnswer = 1,
        explanation = "'Das Wohltemperierte Klavier' (BWV 846-893) besteht aus zwei Teilen mit je 24 Praeludi en und Fugen in allen Dur- und Moll-Tonarten. Es gilt als das Alte Testament der Klaviermusik und wurde von nahezu jedem bedeutenden Pianisten studiert.",
        difficulty = 2,
        funFact = "Bach schrieb 'Das Wohltemperierte Klavier' (Teil 1) im Jahr 1722 'zum Nutzen und Gebrauch der Lehrbegierigen Musicalischen Jugend'. Auf einem Autograph schrieb er, es sei fuer alle Instrumente mit Tasten bestimmt – also auch Cembalo und Orgel."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Form eines mehrsaetzigen Orchesterwerks, typisch fuer die Klassik und Romantik, besteht meist aus drei bis vier Saetzen und ist groesser als ein Concerto?",
        answerA = "Ouvertuere",
        answerB = "Sinfonie",
        answerC = "Kantate",
        answerD = "Suite",
        correctAnswer = 1,
        explanation = "Eine Sinfonie ist ein mehrsaetziges Orchesterwerk, typischerweise in vier Saetzen: Sonatensatz (schnell), langsamer Satz, Menuett oder Scherzo (tanzhaft), Finale (schnell). Haydn und Mozart etablierten die Form, Beethoven weitete sie dramatisch aus.",
        difficulty = 2,
        funFact = "Joseph Haydn schrieb 104 Sinfonien und gilt als 'Vater der Sinfonie'. Mozart schrieb 41, Beethoven 9, Brahms 4, Bruckner 9. Die extrem geringe Zahl bei Brahms erklaert sich durch seinen Perfektionismus: er verwarf jahrzehntelang Entwuerfe, bevor er die erste Sinfonie vollendete."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches beruehmte Ballett von Tschaikowsky zeigt eine Prinzessin, die tagsue ber in einen Schwan verwandelt wird?",
        answerA = "Der Nussknoecker",
        answerB = "Dornroeschen",
        answerC = "Schwanensee",
        answerD = "Petrushka",
        correctAnswer = 2,
        explanation = "Schwanensee (Op. 20) von Peter Iljitsch Tschaikowsky uraufgefuehrt 1877 ist eines der bekanntesten Ballette weltweit. Die Geschichte der Prinzessin Odette, die durch einen Zauberer in einen Schwan verwandelt wurde, ist ein Symbol des klassischen Balletts.",
        difficulty = 2,
        funFact = "Die Uraufführung 1877 war ein Misserfolg – schlechte Choreographie und mittelmaeßige Aufführung. Erst nach Tschaikowskys Tod 1893 wurde das Ballett mit einer neuen Choreographie von Marius Petipa und Lew Iwanow zum Triumph."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Komponist schrieb die 'Vier Jahreszeiten', vier Violinkonzerte, die zur bekanntesten Barockmusik gehoeren?",
        answerA = "Johann Sebastian Bach",
        answerB = "Georg Frideric Haendel",
        answerC = "Antonio Vivaldi",
        answerD = "Arcangelo Corelli",
        correctAnswer = 2,
        explanation = "Die 'Vier Jahreszeiten' (Le quattro stagioni, 1723) sind vier Violinkonzerte von Antonio Vivaldi, die jeweils eine Jahreszeit musikalisch beschreiben. Sie gehoeren zur meistgespielte und meistverkauften Barockmusik der Welt.",
        difficulty = 2,
        funFact = "Vivaldi war roter Priester und Geigenlehrer am Ospedale della Pieta in Venedig, einem Waisenhaus fuer Maedchen. Er schrieb den Grossteil seiner Musik fuer das hoch geschaetzte Ensemble dieses Waisenhauses, das ganz Europa Publikum anzog."
    ),

)
