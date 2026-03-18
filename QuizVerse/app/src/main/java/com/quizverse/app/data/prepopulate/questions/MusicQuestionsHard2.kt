package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsHard2(): List<Question> = listOf(

    // ── Operngeschichte (8) ─────────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Wer komponierte die Oper 'Tristan und Isolde' (1865), die durch ihre harmonische Kuehnheit die Musikgeschichte revolutionierte?",
        answerA = "Giuseppe Verdi",
        answerB = "Richard Wagner",
        answerC = "Carl Maria von Weber",
        answerD = "Giacomo Meyerbeer",
        correctAnswer = 1,
        explanation = "Richard Wagner komponierte 'Tristan und Isolde', die 1865 in Muenchen urauffgefuehrt wurde. Der beruehmte 'Tristan-Akkord' im Vorspiel gilt als Startpunkt der modernen Harmonielehre und leitete die Aufloesung der tonalen Musik ein.",
        difficulty = 3,
        funFact = "Wagner wartete 12 Jahre auf die Urauffuehrung. Koenig Ludwig II. von Bayern ermoeglichte sie schliesslich mit grosszuegiger Finanzierung."
    ),

    Question(
        categoryId = 5,
        questionText = "In welchem Jahr wurde Giacomo Puccinis Oper 'Turandot' nach seinem Tod urauffgefuehrt, und wer vollendete sie?",
        answerA = "1924, Pietro Mascagni",
        answerB = "1926, Franco Alfano",
        answerC = "1928, Ildebrando Pizzetti",
        answerD = "1930, Ottorino Respighi",
        correctAnswer = 1,
        explanation = "Puccini starb 1924, bevor er 'Turandot' vollenden konnte. Franco Alfano schloss die Oper anhand von Puccinis Skizzen ab. Die Urauffuehrung fand am 25. April 1926 an der Mailaender Scala unter Arturo Toscanini statt.",
        difficulty = 3,
        funFact = "Bei der Urauffuehrung legte Toscanini den Taktstock an der Stelle nieder, wo Puccinis eigene Noten endeten, wandte sich ans Publikum und sagte: 'Hier endet die Oper, weil der Maestro hier gestorben ist.'"
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Oper von Georges Bizet, die 1875 in Paris urauffgefuehrt wurde, war anfangs ein Misserfolg und gilt heute als eine der meistgespielten Opern der Welt?",
        answerA = "Faust",
        answerB = "Lakme",
        answerC = "Carmen",
        answerD = "Samson et Dalila",
        correctAnswer = 2,
        explanation = "Bizets 'Carmen' wurde am 3. Maerz 1875 an der Opera-Comique in Paris urauffgefuehrt und vom Publikum kuehl empfangen. Bizet starb nur drei Monate spaeter. Heute ist sie eine der am haeufigsten aufgefuehrten Opern weltweit.",
        difficulty = 3,
        funFact = "Die Rolle der Carmen gilt als eine der anspruchsvollsten Mezzosopran-Partien im gesamten Opernrepertoire. Die Habanera-Melodie im ersten Akt ist eigentlich von Sebastian Yradier entliehen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Begriff beschreibt Wagners Konzept eines Gesamtkunstwerks, das Musik, Drama, Dichtung, bildende Kunst und Tanz vereint?",
        answerA = "Singspiel",
        answerB = "Musikdrama",
        answerC = "Gesamtkunstwerk",
        answerD = "Opera seria",
        correctAnswer = 2,
        explanation = "Wagner praegte den Begriff 'Gesamtkunstwerk' fuer sein Ideal einer totalen Kunstsynthese. In seinen spaeten Opern strebte er danach, alle Kuenste gleichberechtigt zu verbinden, anstatt Musik, Text und Buehne getrennt zu behandeln.",
        difficulty = 3,
        funFact = "Wagner baute das Bayreuther Festspielhaus (1876) speziell nach seinen Vorstellungen: verdecktes Orchester, abgedunkelter Zuschauerraum und besondere Akustik fuer sein Gesamtkunstwerk."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche russische Oper von Modest Mussorgski (1874) gilt als Meisterwerk des Nationalismus und basiert auf einem Schauspiel von Alexander Puschkin?",
        answerA = "Rusalka",
        answerB = "Boris Godunow",
        answerC = "Eugen Onegin",
        answerD = "Prinz Igor",
        correctAnswer = 1,
        explanation = "Mussorgskis 'Boris Godunow' basiert auf Puschkins gleichnamigem Drama ueber den Zaren Boris Godunow (1598-1605). Die Oper existiert in zwei Fassungen (1869 und 1872) und gilt als Grundstein der russischen Nationaloper.",
        difficulty = 3,
        funFact = "Mussorgski hatte keine formale Kompositionsausbildung. Seine Harmonik und Rhythmik war so unorthodox, dass Rimski-Korsakow die Oper nach seinem Tod 'verbesserte' — viele Dirigenten bevorzugen heute die Originalfassung."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'Opera seria' und 'Opera buffa' in der italienischen Operntradition des 18. Jahrhunderts?",
        answerA = "Opera seria ist kuerzere, Opera buffa laenger",
        answerB = "Opera seria ist ernstes Drama mit Mythologie/Geschichte, Opera buffa ist komische Oper mit Alltagssituationen",
        answerC = "Opera seria wird a cappella gesungen, Opera buffa mit Orchester",
        answerD = "Opera seria hat kein Rezitativ, Opera buffa schon",
        correctAnswer = 1,
        explanation = "Opera seria (ernste Oper) behandelte ernste Themen aus Mythologie, antiker Geschichte oder hoeherer Gesellschaft und folgte strengen Konventionen. Opera buffa (komische Oper) zeigte Alltagsmenschen in komischen Situationen und war volksnaehe.",
        difficulty = 3,
        funFact = "Mozarts 'Le nozze di Figaro' (1786) ist ein Paradebeispiel der Opera buffa — der Adel wird satirisiert und die Dienerschaft triumphiert. Das war gesellschaftspolitisch explosiv kurz vor der Franzoesischen Revolution."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Saengerin gilt als 'La Divina' der Opergeschichte und revolutionierte in den 1950ern die Interpretation von Belcanto-Rollen?",
        answerA = "Renata Tebaldi",
        answerB = "Joan Sutherland",
        answerC = "Maria Callas",
        answerD = "Leontyne Price",
        correctAnswer = 2,
        explanation = "Maria Callas (1923-1977) erhielt den Beinamen 'La Divina'. Sie revolutionierte die Interpretation von Belcanto-Opern durch dramatische Expressivitaet und restaurierte vergessene Werke von Donizetti, Bellini und Rossini.",
        difficulty = 3,
        funFact = "Callas nahm zu Beginn ihrer Karriere innerhalb von 18 Monaten ca. 36 kg ab. Diese Gewichtsabnahme veraenderte ihre Stimme und trug zu deren fruehzeitigem Verfall bei — eines der grossen Mysterien der Operngeschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "In welcher Stadt wurde 1637 das erste oeffentliche Opernhaus der Welt, das Teatro San Cassiano, eroeffnet?",
        answerA = "Florenz",
        answerB = "Rom",
        answerC = "Neapel",
        answerD = "Venedig",
        correctAnswer = 3,
        explanation = "Das Teatro San Cassiano in Venedig wurde 1637 als erstes oeffentliches Opernhaus eroeffnet, bei dem man Eintritt zahlen musste. Zuvor war Oper ausschliesslich Hof- und Adelsvergnuegen. Venedig wurde damit zum Zentrum der fruehoeprativen Kultur.",
        difficulty = 3,
        funFact = "Im 17. Jahrhundert hatte Venedig bereits 16 Opernhaeuser — eine unglaubliche Dichte fuer eine Stadt von etwa 150.000 Einwohnern. Oper war Volksunterhaltung wie heute Kino."
    ),

    // ── Soul/R&B/Funk-Geschichte (8) ────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches Plattenlabel gruendete Berry Gordy Jr. 1959 in Detroit, das den Sound der Soul-Musik massggeblich praegte?",
        answerA = "Stax Records",
        answerB = "Atlantic Records",
        answerC = "Motown Records",
        answerD = "Chess Records",
        correctAnswer = 2,
        explanation = "Berry Gordy Jr. gruendete Motown Records (urspruenglich Tamla Records) 1959 in Detroit. Das Label formte einen eigenen Sound — den 'Motown Sound' — mit kuenstlern wie Stevie Wonder, Marvin Gaye und den Supremes.",
        difficulty = 3,
        funFact = "Gordy startete Motown mit einem Darlehen von 800 Dollar aus seinem Familien-Spar-Club. Das Label wurde zum meistverkauften Label der 1960er Jahre und veraenderte die amerikanische Musikindustrie dauerhaft."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Album von Marvin Gaye aus dem Jahr 1971 wird als Meilenstein des Soul betrachtet und befasste sich mit sozialen Themen wie Krieg und Armut?",
        answerA = "Let's Get It On",
        answerB = "What's Going On",
        answerC = "Here, My Dear",
        answerD = "Trouble Man",
        correctAnswer = 1,
        explanation = "Marvin Gayes 'What's Going On' (1971) war ein konzeptuelles Meisterwerk, das den Vietnamkrieg, Drogen, Umweltverschmutzung und soziale Unruhen thematisierte. Es brach mit der Motown-Formel und setzte neue Massstabe fuer den Soul.",
        difficulty = 3,
        funFact = "Motown-Chef Berry Gordy wollte das Album urspruenglich nicht veroeffentlichen — er hielt es fuer zu politisch. Gaye bestand darauf. Es wurde zum meistverkauften Motown-Album aller Zeiten."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer wird als 'Godfather of Soul' bezeichnet und praete den Funk-Stil mit Alben wie 'Papa's Got a Brand New Bag' (1965)?",
        answerA = "Otis Redding",
        answerB = "Wilson Pickett",
        answerC = "Sam Cooke",
        answerD = "James Brown",
        correctAnswer = 3,
        explanation = "James Brown erhielt den Titel 'Godfather of Soul'. Sein Stil basierte auf synkopierten Rhythmen, perkussiven Gitarren und einem stark betonten ersten Schlag — die Grundlage des Funk. 'Papa's Got a Brand New Bag' markierte diesen Stilwechsel.",
        difficulty = 3,
        funFact = "James Brown war bekannt fuer exzessiv lange Konzerte. Sein Buehnenprogramm mit dem Umhang — bei dem er vorgab zu kollabieren und wiederbelebt zu werden — wurde zu einer der ikonischsten Buehnenshows der Musikgeschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Stax-Records-Kuenstlerduo schuf mit Saenger Otis Redding eng zusammen und ist fuer die Instrumentals 'Green Onions' (1962) bekannt?",
        answerA = "Sam & Dave",
        answerB = "Booker T. & the M.G.'s",
        answerC = "The Impressions",
        answerD = "The Bar-Kays",
        correctAnswer = 1,
        explanation = "Booker T. & the M.G.'s waren das Hausband von Stax Records in Memphis. Ihr Instrumental 'Green Onions' (1962) wurde ein grosser Hit. Sie spielten auf Aufnahmen von Otis Redding, Wilson Pickett und vielen anderen.",
        difficulty = 3,
        funFact = "Die M.G.'s (Memphis Group) waren eine der ersten gemischtrassigen Bands des amerikanischen Suedens — ungewoehnlich und mutig in der Aera der Rassentrennung. Ihr Zusammenspiel gilt als Musterbeispiel von Groove und Zurueckhaltung."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Saengerin wurde mit dem Album 'Respect Yourself' bekannt und gilt als 'Queen of Soul'?",
        answerA = "Diana Ross",
        answerB = "Patti LaBelle",
        answerC = "Aretha Franklin",
        answerD = "Tina Turner",
        correctAnswer = 2,
        explanation = "Aretha Franklin wird als 'Queen of Soul' bezeichnet. Ihr Song 'Respect' (1967) — eine Coverversion von Otis Redding — wurde zu einer Hymne der Buergerrechts- und Frauenbewegung. Sie gewann 18 Grammy Awards und war die erste Frau in der Rock and Roll Hall of Fame.",
        difficulty = 3,
        funFact = "Aretha Franklin begann als Gospelsaengerin in der Baptistenkirche ihres Vaters. Columbia Records vermarktete sie erfolglos als Jazz-Pop-Saengerin. Erst nach ihrem Wechsel zu Atlantic Records 1966 entstand ihr charakteristischer Soul-Sound."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Band von Sly Stone gruendete einen einzigartigen Stil, der Rock, Soul, Funk und Psychedelia verband und mit 'Everyday People' (1968) bekannt wurde?",
        answerA = "Tower of Power",
        answerB = "Earth Wind & Fire",
        answerC = "Sly and the Family Stone",
        answerD = "The Ohio Players",
        correctAnswer = 2,
        explanation = "Sly and the Family Stone (1966 gegr.) vereinte schwarze und weisse Musiker beider Geschlechter — ungewoehnlich fuer die Zeit. Ihr Sound vermischte Funk, Rock, Soul und Psychedelia. 'Everyday People' war eine Hymne auf Gleichheit und wurde Nummer-1-Hit.",
        difficulty = 3,
        funFact = "Ihr Auftritt beim Woodstock-Festival 1969 um 3 Uhr nachts gilt als einer der elektrischsten Momente des Festivals. Die Band spielte sich in eine solche Ekstase, dass Hunderttausende im Morgengrauen tanzten."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Genre entstand in den 1970ern als Weiterentwicklung des Soul mit staerkerer Betonung des Rhythmus, verzerrten Baessen und repetitiven Grooves?",
        answerA = "Disco",
        answerB = "Funk",
        answerC = "Gospel",
        answerD = "Rhythm and Blues",
        correctAnswer = 1,
        explanation = "Funk entstand Mitte der 1960er als Weiterentwicklung des Soul und R&B. Charakteristisch sind der starke Downbeat (Betonung der ersten Zaehlzeit), synkopierte Rhythmen, verzerrte E-Baesse und repetitive, groovorientierte Strukturen.",
        difficulty = 3,
        funFact = "Der Begriff 'Funky' bezeichnete urspruenglich etwas mit starkem Geruch — oft negativ konnotiert. Im afroamerikanischen Slang wandelte er sich zu einem Lob fuer etwas Erdiges, Authentisches und Rhythmisch-Mitreissendes."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Album von Prince aus dem Jahr 1984 gilt als Meilenstein des Funk und Soul und enthielt den Titelsong zu seinem gleichnamigen Film?",
        answerA = "Dirty Mind",
        answerB = "Sign O the Times",
        answerC = "Purple Rain",
        answerD = "1999",
        correctAnswer = 2,
        explanation = "Princes 'Purple Rain' (1984) war das Soundtrack-Album zu seinem autobiografischen Film. Es vereinte Funk, Rock, Soul und Pop und verkaufte sich ueber 20 Millionen Mal. Es gewann den Oscar fuer die beste Filmmusik.",
        difficulty = 3,
        funFact = "Prince spielte fast alle Instrumente auf dem Album selbst ein. Das Gitarrensolo in 'Purple Rain' wurde von Rolling Stone als eines der groessten Gitarrensoli aller Zeiten eingestuft."
    ),

    // ── Metal/Punk-Subgenres (7) ─────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welche Band gilt als Begruender des Thrash Metal und veroeffentlichte 1983 das Debuetalblum 'Kill 'Em All'?",
        answerA = "Slayer",
        answerB = "Anthrax",
        answerC = "Metallica",
        answerD = "Megadeth",
        correctAnswer = 2,
        explanation = "Metallicas 'Kill 'Em All' (1983) gilt als eines der ersten Thrash-Metal-Alben ueberhaupt. Die Band kombinierte die Energie des Punk mit der technischen Komplexitaet des Heavy Metal und begann damit ein neues Genre.",
        difficulty = 3,
        funFact = "Das Album sollte urspruenglich 'Metal Up Your Ass' heissen — mit entsprechendem Cover. Plattenhaendler weigerten sich, es zu verkaufen, weshalb der Titel geaendert wurde."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches norwegische Black-Metal-Label wurde in den fruehen 1990ern bekannt und war mit Kuenstlern wie Mayhem, Burzum und Darkthrone verbunden?",
        answerA = "Century Media",
        answerB = "Deathlike Silence Productions",
        answerC = "Osmose Productions",
        answerD = "Peaceville Records",
        correctAnswer = 1,
        explanation = "Deathlike Silence Productions wurde von Oystein 'Euronymous' Aarseth (Mayhem) gegr. und veroeffentlichte fruehe Black-Metal-Alben. Die norwegische Szene dieser Zeit wurde von Kirchenbraenden und Gewalt begleitet und praete den 'True Norwegian Black Metal'-Stil.",
        difficulty = 3,
        funFact = "Die norwegische Black-Metal-Szene der 1990er brannte ueber 50 Kirchen nieder. Die Musiker verstanden sich als Gegenbewegung zum christlichen Mainstream — kulturell und religioes eine der extremsten Subkultur-Eskalationen der Rockgeschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches britische Band gruendete den Doom Metal und veroeffentlichte 1970 das Album 'Black Sabbath', das die dunkle, schwere Aestetik des Genres etablierte?",
        answerA = "Judas Priest",
        answerB = "Deep Purple",
        answerC = "Black Sabbath",
        answerD = "Led Zeppelin",
        correctAnswer = 2,
        explanation = "Black Sabbath aus Birmingham gilt als Begruender des Heavy und Doom Metal. Ihr Debuetalalbum (1970) mit Ozzy Osbourne an der Stimme setzte auf langsame, bedrohliche Riffs, okkulte Texte und einen bewusst duestere Klangatmosphaere.",
        difficulty = 3,
        funFact = "Der charakteristische 'Tritonus' (verminderte Quinte) im Einstiegsriff von 'Black Sabbath' wurde im Mittelalter als 'diabolus in musica' (Teufel in der Musik) bezeichnet und war in der Kirchenmusik verboten."
    ),

    Question(
        categoryId = 5,
        questionText = "Was unterscheidet Death Metal von regulaerem Heavy Metal musikalisch?",
        answerA = "Death Metal ist langsamer und melodischer",
        answerB = "Death Metal nutzt Growl-Gesang, extrem verzerrte Gitarren, komplexe Rhythmik und explizite Texte ueber Tod und Gewalt",
        answerC = "Death Metal verwendet ausschliesslich akustische Instrumente",
        answerD = "Death Metal entstand aus dem Jazz",
        correctAnswer = 1,
        explanation = "Death Metal entstand Mitte der 1980er in den USA (Tampa, Florida). Charakteristisch sind: gutturaler Growl-Gesang, stark verzerrte und schnell gespielte Gitarren, komplexe Rhythmik mit Blast-Beats und Texte ueber Tod, Gewalt oder okkulte Themen.",
        difficulty = 3,
        funFact = "Tampa, Florida gilt als Wiege des Death Metal. Bands wie Death (mit Chuck Schuldiner), Obituary und Cannibal Corpse stammten von dort. Das Morrisound Studio war das wichtigste Aufnahmestudio des Genres."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher britische Punk-Song von 1977, aufgefuehrt von den Sex Pistols, sorgte fuer einen Skandal und wurde von der BBC verboten?",
        answerA = "Pretty Vacant",
        answerB = "Anarchy in the U.K.",
        answerC = "God Save the Queen",
        answerD = "Holidays in the Sun",
        correctAnswer = 2,
        explanation = "Die Sex Pistols' 'God Save the Queen' (1977) erschien waehrend des Silver Jubilee von Koenigin Elizabeth II. Die BBC verbannte den Song sofort. Er erreichte dennoch Platz 2 der Charts — viele glauben, er war tatsaechlich Nummer 1 und wurde manipuliert.",
        difficulty = 3,
        funFact = "Die Sex Pistols fuhren auf einem Boot auf der Themse und spielten 'God Save the Queen' direkt vor dem Parlament — die Polizei stoppte das Konzert und verhaftete mehrere Bandmitglieder und Manager Malcolm McLaren."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches US-Punk-Genre entstand in den 1980ern als reaktion auf das kommerziell gewordene Punk, mit schnelleren Tempi, kuerzeren Songs und politischeren Texten?",
        answerA = "Post-Punk",
        answerB = "New Wave",
        answerC = "Hardcore Punk",
        answerD = "Oi! Punk",
        correctAnswer = 2,
        explanation = "Hardcore Punk entwickelte sich Ende der 1970er/Anfang der 1980er in den USA. Charakteristisch sind: extrem schnelle Tempi, kurze Songs (oft unter 2 Minuten), direkte politische Texte und DIY-Ethos. Bands wie Black Flag, Dead Kennedys und Minor Threat praegte das Genre.",
        difficulty = 3,
        funFact = "Ian MacKaye von Minor Threat erfand den Begriff 'Straight Edge' — eine Bewegung die Alkohol, Drogen und promiskuitiven Sex ablehnte. Diese subkulturelle Bewegung hat bis heute Millionen von Anhaengern weltweit."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Genre kombiniert Metal-Elemente mit Rap-Gesang und wurde in den 1990ern durch Bands wie Rage Against the Machine und Limp Bizkit bekannt?",
        answerA = "Post-Grunge",
        answerB = "Nu-Metal",
        answerC = "Metalcore",
        answerD = "Grunge",
        correctAnswer = 1,
        explanation = "Nu-Metal entstand Mitte der 1990er in den USA. Es kombiniert Heavy-Metal-Gitarren mit Hip-Hop-Rhythmik und Rap-Gesang, oft angereichert mit Elementen aus Funk und Alternative Rock. Korn, Limp Bizkit und Linkin Park waren die kommerziell erfolgreichsten Vertreter.",
        difficulty = 3,
        funFact = "Rage Against the Machine gelten als Vorreiter, obwohl sie sich selbst nie als Nu-Metal bezeichneten. Ihr politisch linker Rap-Metal war einer der ersten und einflussreichsten Beispiele des Genre-Mix."
    ),

    // ── Musikjournalismus/Kritik (6) ─────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches amerikanische Musikmagazin wurde 1967 von Jann Wenner in San Francisco gegruendet und praegte den Musikjournalismus entscheidend?",
        answerA = "Creem",
        answerB = "Rolling Stone",
        answerC = "Melody Maker",
        answerD = "Spin",
        correctAnswer = 1,
        explanation = "Rolling Stone wurde 1967 von Jann Wenner und Ralph Gleason in San Francisco gegruendet. Es war das erste Magazin, das Rockmusik ernst als Kultur- und Gesellschaftsphaenomen behandelte. Autoren wie Hunter S. Thompson machten es zur Instanz des Gonzo-Journalismus.",
        difficulty = 3,
        funFact = "Die erste Ausgabe kostete 25 Cent und hatte John Lennon auf dem Cover. Das Magazin wurde mit 7.500 Dollar Startkapital gegruendet und entwickelte sich zum einflussreichsten Musikmagazin der Welt."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer entwickelte den Begriff 'Gonzo Journalism' und schrieb ausgiebig ueber Musik fuer Rolling Stone, darunter den Essay 'Fear and Loathing in Las Vegas'?",
        answerA = "Lester Bangs",
        answerB = "Greil Marcus",
        answerC = "Hunter S. Thompson",
        answerD = "Robert Christgau",
        correctAnswer = 2,
        explanation = "Hunter S. Thompson (1937-2005) erfand den 'Gonzo Journalism' — einen subjektiven Stil, bei dem der Autor Teil der Geschichte wird. Fuer Rolling Stone schrieb er u.a. 'Fear and Loathing in Las Vegas' (1971) und Reportagen ueber die Nixon-Kampagne.",
        difficulty = 3,
        funFact = "Lester Bangs, ein anderer Rolling-Stone-Mitarbeiter, ist bekannt als einer der einflussreichsten Musikkritiker aller Zeiten. Sein anarchischer Stil — er schrieb ueber Musik mit Leidenschaft und Provokation — revolutionierte den Musikjournalismus."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches britische Musikmagazin war in den 1970er und 1980er Jahren das wichtigste Sprachrohr des Punk und New Wave und bekannt fuer seinen einflussreichen Jahres-Ranking?",
        answerA = "New Musical Express (NME)",
        answerB = "Sounds",
        answerC = "Melody Maker",
        answerD = "Q Magazine",
        correctAnswer = 0,
        explanation = "Der New Musical Express (NME) wurde 1952 gegruendet und war in den 1970er/80er Jahren die wichtigste Stimme fuer Punk, New Wave und alternative Musik in Grossbritannien. Autoren wie Nick Kent und Paul Morley praegte die Debatte um Rockmusik als Kulturphaenomen.",
        difficulty = 3,
        funFact = "Der NME war das erste britische Musikmagazin, das eine Einzelhits-Hitparade abdruckte. In seiner Bluetenzeit in den 1970ern hatte es eine Auflage von 300.000 Exemplaren pro Woche."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer ist der Autor von 'Mystery Train' (1975), dem Buch das Rockmusik erstmals als ernstzunehmende amerikanische Kunstform analysierte?",
        answerA = "Robert Christgau",
        answerB = "Lester Bangs",
        answerC = "Greil Marcus",
        answerD = "Dave Marsh",
        correctAnswer = 2,
        explanation = "Greil Marcus schrieb 'Mystery Train: Images of America in Rock 'n' Roll Music' (1975). Das Buch analysierte Kuenstler wie Robert Johnson, Elvis Presley und The Band als Ausdruck amerikanischer Kultur und Mythologie. Es gilt als Klassiker des Musikjournalismus.",
        difficulty = 3,
        funFact = "Greil Marcus war einer der ersten Autoren, der Rockmusik als Gegenstand akademischer Analyse behandelte. Er studierte an der UC Berkeley Amerikanistik und brachte diesen akademischen Ansatz in den Musikjournalismus."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Preis gilt als wichtigster Literaturpreis im englischsprachigen Musikjournalismus und wird jaehrlich fuer das beste Buch ueber Musik verliehen?",
        answerA = "Pulitzer Prize for Music",
        answerB = "Grammy Award for Best Music Book",
        answerC = "Penderyn Music Book Prize",
        answerD = "Ralph J. Gleason Music Book Award",
        correctAnswer = 3,
        explanation = "Der Ralph J. Gleason Music Book Award wurde nach dem Mitgruender von Rolling Stone benannt. Er ist einer der prestigetraechtigsten Auszeichnungen fuer englischsprachige Musikbuecher und wird von der Rock and Roll Hall of Fame verliehen.",
        difficulty = 3,
        funFact = "Der Penderyn Music Book Prize aus Wales ist tatsaechlich einer der anerkanntesten Buchpreise fuer Musikliteratur in Grossbritannien. Er wurde nach der walisischen Whisky-Destillerie benannt, die ihn sponsert."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Online-Plattform revolutionierte ab 1996 die Musikkritik durch numerische Bewertungen und wurde besonders fuer ihre einflussreiche Indie-Kritiik bekannt?",
        answerA = "AllMusic",
        answerB = "Pitchfork",
        answerC = "Metacritic",
        answerD = "Rate Your Music",
        correctAnswer = 1,
        explanation = "Pitchfork wurde 1996 von Ryan Schreiber gegruendet und revolutionierte die Musikkritik durch detaillierte Rezensionen mit numerischen Wertungen (0.0-10.0). Besonders fuer Indie- und alternative Musik wurde die Plattform zur wichtigsten Stimme der Branche.",
        difficulty = 3,
        funFact = "Pitchforks vernichtende Kritik an Travie McCoys 'Cupid's Chokehold' mit 0.0 Punkten und Kommentaren wie 'worst album of the decade' brachte dem Magazin sowohl Ansehen als auch viel Kritik. Eine negative Pitchfork-Kritik konnte eine Karriere beenden."
    ),

    // ── Musikbusiness/Labels (7) ─────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welche Vereinbarung bezeichnet man als '360-Grad-Deal' in der Musikindustrie?",
        answerA = "Ein Vertrag ueber 360 Alben",
        answerB = "Ein Vertrag, bei dem das Label an allen Einnahmen des Kuenstlers (Konzerte, Merchandise, Werbung) beteiligt wird",
        answerC = "Ein globaler Vertriebsvertrag in 360 Laendern",
        answerD = "Ein Vertrag der 360 Tage gilt",
        correctAnswer = 1,
        explanation = "Ein 360-Grad-Deal (auch 'Equitable Deal') ist ein Vertrag, bei dem Plattenlabels nicht nur an Albumverkaufen, sondern auch an Konzerteinnahmen, Merchandise, Werbevertraegen und anderen Einkommensquellen des Kuenstlers beteiligt werden.",
        difficulty = 3,
        funFact = "360-Deals wurden nach dem Einbruch der CD-Verkaufserloes durch Internetpiraterie populaer. Labels versuchten, ihre Einnahmen durch Beteiligung an anderen Einkommensquellen zu sichern. Viele Kuenstler sehen sie als unfair."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen einem Majorlabel und einem Independentlabel in der Musikindustrie?",
        answerA = "Major Labels haben mehr Mitarbeiter, sonst kein Unterschied",
        answerB = "Major Labels sind Teil grosser Medienkonzerne mit weltweiter Distribution; Independents sind unabhaengig und haben kuenstlerische Freiheit als Prioritaet",
        answerC = "Major Labels produzieren nur klassische Musik",
        answerD = "Independent Labels sind staatlich gefuerdert",
        correctAnswer = 1,
        explanation = "Die 'Big Three' (Universal Music Group, Sony Music, Warner Music) dominieren den globalen Markt mit ca. 70% Marktanteil. Independent Labels (Indie) sind wirtschaftlich unabhaengig und bieten Kuenstlern meist mehr kreative Kontrolle, aber weniger Distribution.",
        difficulty = 3,
        funFact = "Paradoxerweise besitzen die Major Labels oft Anteile an oder vertreiben Produkte von 'Independent' Labels. Etiketten wie Sub Pop, Matador oder XL Recordings gelten als unabhaengig, haben aber Distributoionsvertraege mit Majors."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher amerikanische Unternehmer gruendete Def Jam Recordings 1984, das zur wichtigsten Plattform fuer Hip-Hop wurde?",
        answerA = "Suge Knight",
        answerB = "Puffy Combs (Diddy)",
        answerC = "Russell Simmons",
        answerD = "Jimmy Iovine",
        correctAnswer = 2,
        explanation = "Russell Simmons und Rick Rubin gruendeten Def Jam Recordings 1984. Rubin produzierte den charakteristischen Sound, Simmons managte das Business. Das Label entwickelte LL Cool J, The Beastie Boys, Public Enemy und Jay-Z.",
        difficulty = 3,
        funFact = "Rick Rubin gruendete Def Jam als 19-jaehriger Student in seinem NYU-Wohnheimzimmer. Spaeter wurde er einer der einflussreichsten Produzenten der Geschichte und arbeitete mit Johnny Cash, Red Hot Chili Peppers und Kanye West."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Payola' in der Musikindustrie?",
        answerA = "Eine Bonuszahlung fuer Plattenverkaeufe",
        answerB = "Die illegale Praxis, Radiosender fuer das Abspielen bestimmter Songs zu bezahlen",
        answerC = "Ein Royalty-System fuer Komponisten",
        answerD = "Ein Vertriebsmodell fuer digitale Musik",
        correctAnswer = 1,
        explanation = "Payola bezeichnet die (in den USA nach 1960 illegale) Praxis, Radiodiscjockeys oder Senderverantwortliche zu bezahlen, damit sie bestimmte Songs haeufiger spielen. Der Payola-Skandal 1959/60 erschuetterte die Musikindustrie und fuehrte zum Ende der Karriere von DJ Alan Freed.",
        difficulty = 3,
        funFact = "Alan Freed, der die Rock-'n'-Roll-Revuen erfand und den Begriff 'Rock 'n' Roll' popularisierte, verlor durch den Payola-Skandal seine Karriere. Er starb 1965 mittellos an Alkoholismus."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bedeutet 'A&R' in einem Plattenlabel?",
        answerA = "Audio and Recording",
        answerB = "Artists and Repertoire",
        answerC = "Accounting and Revenue",
        answerD = "Advertising and Radio",
        correctAnswer = 1,
        explanation = "A&R steht fuer 'Artists and Repertoire'. A&R-Mitarbeiter sind verantwortlich fuer das Entdecken neuer Kuenstler, die Betreuung bestehender Kuenstler, die Auswahl von Songs und die Zusammenarbeit mit Produzenten. Sie sind die 'Talentscouts' der Musikindustrie.",
        difficulty = 3,
        funFact = "John Hammond Sr. gilt als einer der groessten A&R-Manager der Geschichte. Er entdeckte Billie Holiday, Aretha Franklin, Bob Dylan und Bruce Springsteen — allesamt fuer Columbia Records. Eine unglaubliche Erfolgsbilanz ueber fuenf Jahrzehnte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Streaming-Modell bezahlt Kuenstler pro Stream und wird dafuer kritisiert, dass es fuer die meisten Kuenstler wirtschaftlich unrentabel ist?",
        answerA = "Abo-Modell mit fixer Kuenstlergebueher",
        answerB = "Pro-Rata-Modell (Einnahmen werden proportional zur Gesamtstreamzahl verteilt)",
        answerC = "Direktkauf-Modell",
        answerD = "Fan-Powered Royalties (nur Fans eines Kuenstlers finanzieren ihn)",
        correctAnswer = 1,
        explanation = "Das Pro-Rata-Modell wird von Spotify, Apple Music und anderen genutzt. Alle Abonnementgebuehren fliessen in einen Pool und werden nach dem Anteil der Streams verteilt. Das bevorteilt massenkompatible Kuenstler und macht es fuer Nischenkuenstler wirtschaftlich kaum rentabel.",
        difficulty = 3,
        funFact = "Man braucht bei Spotify ca. 1.000 Streams fuer etwa 4 Dollar — das bedeutet, ein Kuenstler muss 250.000 Streams pro Monat generieren, um einen Mindestlohn zu verdienen. Taylor Swift zog ihren Katalog 2014 aus Protest ab."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Vertrag aus dem Jahr 1985 ermoeglichte Michael Jackson, den Musikverlag ATV Music (mit den Rechten an Beatles-Songs) fuer 47,5 Millionen Dollar zu kaufen?",
        answerA = "Er gewann eine Auktion gegen Paul McCartney",
        answerB = "Er uebernahm das Unternehmen durch Aktionaersbeschluss",
        answerC = "Er kaufte die Rechte nach dem Tod von John Lennon",
        answerD = "Er kaufte sie ueber einen Vermittler vor dem oeffentlichen Verkauf",
        correctAnswer = 0,
        explanation = "Jackson gewann 1985 eine Auktion fuer den ATV Music Catalogue (ca. 4.000 Songs, darunter fast alle Beatles-Kompositionen). Paul McCartney hatte mitgeboten und verloren. Diese Sammlung bildete spaeter die Basis von Sony/ATV, dem groessten Musikverlag der Welt.",
        difficulty = 3,
        funFact = "Paul McCartney hatte Jackson selbst auf die Bedeutung des Musikrechtekaufs hingewiesen. Als die Rechte zum Verkauf standen, handelte Jackson — und uebertbot seinen Mentor. McCartney nannte es spaeter eine 'sehr saure Lektion'."
    ),

    // ── Country/Folk/Bluegrass (7) ───────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Wer wird als 'Father of Bluegrass' bezeichnet und formte in den 1940ern mit seinem charakteristischen Mandolinenspiel ein neues Genre?",
        answerA = "Earl Scruggs",
        answerB = "Doc Watson",
        answerC = "Bill Monroe",
        answerD = "Lester Flatt",
        correctAnswer = 2,
        explanation = "Bill Monroe (1911-1996) gilt als Begruender des Bluegrass. Seine Band 'The Blue Grass Boys' praege den Sound: schnelle Tempi, hohe Harmonien, Improvisation und virtuoses Banjo- und Mandolinenspiel. Der Bandname gab dem Genre seinen Namen.",
        difficulty = 3,
        funFact = "Earl Scruggs entwickelte in Monroes Band den 3-Finger-Banjo-Picking-Stil, der zum Markenzeichen des Bluegrass wurde. Als Scruggs 1948 die Band verliess und mit Lester Flatt ein Duo gruendete, war Monroe wochenlang untroestlich."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Radiosender in Nashville, der seit 1927 sendet, ist als 'Tempel des Country' bekannt und machte die 'Grand Ole Opry' zur wichtigsten Country-Buehnerin?",
        answerA = "WSM Radio",
        answerB = "WLAC Radio",
        answerC = "WMAQ Radio",
        answerD = "WLS Radio",
        correctAnswer = 0,
        explanation = "WSM Radio in Nashville sendet seit 1925 und wurde 1927 Heimat der 'Grand Ole Opry' — einer woehnentlichen Live-Country-Show, die bis heute laeuft. Sie ist die laengst laufende Radio-Live-Show der Welt und machte Nashville zur Countrhauptstadt.",
        difficulty = 3,
        funFact = "Die Grand Ole Opry begann als 'WSM Barn Dance'. Der Name 'Grand Ole Opry' entstand 1927 live auf Sendung, als Moderator George D. Hay den Kontrast zur vorhergegangen Hochkultur-Sendung betonte: 'We've been listening to the Grand Opera... but from now on you'll get the Grand Ole Opry!'"
    ),

    Question(
        categoryId = 5,
        questionText = "Wer schrieb 'Blowin' in the Wind' (1962) und 'The Times They Are A-Changin'' (1964) und gilt als wichtigste Stimme des Folk-Revivals?",
        answerA = "Pete Seeger",
        answerB = "Woody Guthrie",
        answerC = "Joan Baez",
        answerD = "Bob Dylan",
        correctAnswer = 3,
        explanation = "Bob Dylan (geb. Robert Zimmermann) wurde mit diesen Songs zur Stimme der Buergerrechtsbewegung und des Anti-Kriegs-Protests. Er entwickelte den politisch engagierten Folk-Song-Stil und erhielt 2016 den Nobelpreis fuer Literatur.",
        difficulty = 3,
        funFact = "Als Dylan 1965 beim Newport Folk Festival mit einer E-Gitarre auftrat und elektrifizierte Folk-Rock spielte, wurde er vom Publikum ausgebuht. Pete Seeger soll versucht haben, die Stromkabel zu kappen. Dylan interpretierte es als Befreiung."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Musikerin veroeffentlichte 1971 'Blue', ein Album das als eines der einflussreichsten Singer-Songwriter-Alben aller Zeiten gilt?",
        answerA = "Carole King",
        answerB = "Joni Mitchell",
        answerC = "Linda Ronstadt",
        answerD = "Carly Simon",
        correctAnswer = 1,
        explanation = "Joni Mitchells 'Blue' (1971) ist ein intimes, verletzliches Bekenntnis-Album ueber Liebe, Verlust und Selbstfindung. Die ungewoehnlichen Gitarren-Stimmungen, lyrische Tiefe und Mitchells Sopranstimme machen es zu einem einzigartigen Meisterwerk.",
        difficulty = 3,
        funFact = "Mitchell spielte die meisten Gitarren auf 'Blue' in exotischen offenen Stimmungen, die sie selbst entwickelte. Sie hatte nach einer Kinderlaehme-Erkrankung Muskelschwaeche in der Hand, weshalb sie einfachere Griffe entwickelte, die ihren Stil praegte."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Outlaw Country' in der Musikgeschichte der 1970er Jahre?",
        answerA = "Country-Musik die explizite Texte enthielt",
        answerB = "Eine Bewegung von Nashville-Country-Abweichlern die kuenstlerische Kontrolle ueber ihre Musik forderten und rauhere Sounds bevorzugten",
        answerC = "Country-Musik ueber das Leben von Kriminellen",
        answerD = "Country-Musik die im Ausland produziert wurde",
        correctAnswer = 1,
        explanation = "Outlaw Country entstand als Gegenreaktion auf den glatten 'Nashville Sound' der 1960er. Waylon Jennings, Willie Nelson, Johnny Cash und Kris Kristofferson widersetzten sich der Kontrolle der Plattenlabels und bestanden auf eigenen Produzenten und Musikern.",
        difficulty = 3,
        funFact = "Das 1976er Sampler-Album 'Wanted! The Outlaws' war das erste Country-Album, das Platin-Zertifizierung errang (ueber eine Million Exemplare). Es vereinte Waylon Jennings, Willie Nelson, Jessi Colter und Tompall Glaser."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Zupfinstrument ist charakteristisch fuer den Bluegrass und wird im 3-Finger-Picking-Stil (Scruggs-Stil) gespielt?",
        answerA = "Dulcimer",
        answerB = "Dobro",
        answerC = "Banjo",
        answerD = "Mandoline",
        correctAnswer = 2,
        explanation = "Das fuenfstraitige Banjo ist das Zentralinstrument des Bluegrass. Earl Scruggs entwickelte den 3-Finger-Picking-Stil (Daumen, Zeige- und Mittelfinger), der schnelle, rollende Melodien ermoeglicht und das Instrument von seinem frueheren Claw-Hammer-Stil unterscheidet.",
        difficulty = 3,
        funFact = "Das Banjo hat afrikanische Wurzeln. Vorgaenger des Instruments wurden von versklavten Afrikanern in Amerika gespielt. Erst im 19. Jahrhundert uebernahmen weisse Musiker das Instrument fuer Minstrel-Shows — eine der vielen kulturellen Uebernahmen in der Musikgeschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer war Hank Williams und welchen Einfluss hatte er auf die Country-Musik?",
        answerA = "Ein Bluesmusiker der den Swing praete",
        answerB = "Der erste Country-Star im Fernsehen",
        answerC = "Ein Pionier des Rock 'n' Roll",
        answerD = "Ein Songwriter und Saenger (1923-1953) dessen emotionale Texte und Melodien den modernen Country definierten",
        correctAnswer = 3,
        explanation = "Hank Williams (1923-1953) schrieb und nahm Klassiker wie 'Your Cheatin' Heart', 'Jambalaya' und 'I'm So Lonesome I Could Cry' auf. Sein authentischer Ausdruck von Schmerz, Einsamkeit und Liebe setzte den Standard fuer Country-Songwriting.",
        difficulty = 3,
        funFact = "Williams starb am 1. Januar 1953 mit nur 29 Jahren an Herzversagen durch Alkohol- und Drogenmissbrauch — im Auto auf dem Weg zu einem Konzert. Er gilt als der erste Kuenstler, der an dem starb, was spaeter als '27 Club' Mythos bekannt wurde, obwohl er 29 war."
    ),

    // ── Reggae/Ska/Afrobeat (7) ──────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welche jamaikanische Musik war der Vorgaenger des Reggae und entstand in den 1950ern durch Einfluss amerikanischen Rhythm & Blues?",
        answerA = "Dancehall",
        answerB = "Ska",
        answerC = "Dub",
        answerD = "Mento",
        correctAnswer = 1,
        explanation = "Ska entstand in den fruehen 1960ern in Jamaika durch die Verbindung jamaikanischen Mento-Stils mit amerikanischem R&B. Charakteristisch ist der Off-Beat-Gitarrenakkord (der 'Skank') und ein aufwaerts treibender Rhythmus. Daraus entwickelte sich spaeter Rocksteady und dann Reggae.",
        difficulty = 3,
        funFact = "Ska erlebte in Grossbritannien zwei weitere Wellen: 2-Tone Ska in den 1980ern (The Specials, Madness) und einen dritten Ska-Boom in den 1990ern mit amerikanischen Bands wie No Doubt und Sublime. Das jamaikanische Original ist damit einer der global meistrecycletern Musikstile."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer gilt als 'Godfather of Reggae' und schrieb mit 'No Woman No Cry' und 'Redemption Song' weltberuehmt gewordene Reggae-Hymnen?",
        answerA = "Jimmy Cliff",
        answerB = "Toots Hibbert",
        answerC = "Burning Spear",
        answerD = "Bob Marley",
        correctAnswer = 3,
        explanation = "Bob Marley (1945-1981) ist der bekannteste Reggae-Kuenstler der Welt. Mit The Wailers brachte er Reggae, Rastafari-Philosophie und jamaikanische Kultur auf die Weltbuehne. Er starb 1981 an Hirnkrebs und ist seitdem zu einer globalen Ikone geworden.",
        difficulty = 3,
        funFact = "Als ein Attentaeter 1976 in Marleys Haus einbrach und ihn erschoss (er ueberlebte verletzt), trat Marley zwei Tage spaeter beim 'Smile Jamaica' Friedenskonzert auf. Die Narbe war sichtbar. Es gilt als eine der mutigsten Buehnenauftritte der Musikgeschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Dub' in der Reggae-Tradition und wer gilt als sein wichtigster Pionier?",
        answerA = "Ein Tanz-Stil aus Jamaika",
        answerB = "Eine Produktionstechnik, die Spuren neu arrangiert, mit Echo und Reverb verfremdet und Instrumente heraushebt oder entfernt — Pioneer: King Tubby",
        answerC = "Eine Liedform mit improvisiertem Text",
        answerD = "Ein Gitarren-Spielstil",
        correctAnswer = 1,
        explanation = "Dub ist eine Remix-Technik aus den 1960ern, bei der Tonspuren von Reggae-Aufnahmen manipuliert werden: Gesang wird entfernt oder verhallt, Baeasse werden betont, Percussion-Elemente werden isoliert. King Tubby (Osbourne Ruddock) gilt als Erfinder und Maestro des Dub.",
        difficulty = 3,
        funFact = "Dub gilt als direkter Vorgaenger von Hip-Hop, Elektronischer Musik und Remix-Kultur. Lee 'Scratch' Perry, ein anderer Dub-Pionier, arbeitete mit Bob Marley und The Wailers zusammen und gilt als einer der kreativsten Musikproduzenten aller Zeiten."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer ist der Begruender des Afrobeat und kombinierte in den 1970ern in Lagos Yoruba-Musik, Jazz und Funk mit politischen Texten gegen Korruption?",
        answerA = "King Sunny Ade",
        answerB = "Miriam Makeba",
        answerC = "Fela Kuti",
        answerD = "Hugh Masekela",
        correctAnswer = 2,
        explanation = "Fela Anikulapo-Kuti (1938-1997) gruendete den Afrobeat — eine explosive Mischung aus Yoruba-Musik, Jazz, Highlife und Funk mit explizit politischen Texten gegen die nigerianische Militaerregierung. Seine 'Kalakuta Republic' war ein offener Akt des Widerstands.",
        difficulty = 3,
        funFact = "Das nigerianische Militaer stuermt Felas Kalakuta Republic 1977 mit 1.000 Soldaten. Felas Mutter warf er ihre Auseinandersetzung bei dem Angriff aus dem Fenster — sie starb spaeter an den Folgen. Fela uebergab das Militaer ihren Sarg als Protest."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Reggae-Unterform entstand in den 1980ern in Jamaika mit schnelleren Rhythmen, computergenerierten Beats und raeuda gehenden Texten?",
        answerA = "Rocksteady",
        answerB = "Roots Reggae",
        answerC = "Dancehall",
        answerD = "Lovers Rock",
        correctAnswer = 2,
        explanation = "Dancehall entstand Anfang der 1980er als Gegenpol zum tiefgriundigen 'Roots Reggae'. Es nutzte Riddims (Backing Tracks) von Soundsystem-Selektoren, schnellere Rhythmen und DJs (Deejays/Toasters) die ueber die Tracks rappenartig sprechsingen. Kuenstler wie Shabba Ranks und Beenie Man praegte das Genre.",
        difficulty = 3,
        funFact = "Das jamaikanische Soundsystem — ein mobiles DJ-Setup mit riesigen Lautsprechern — ist die Mutter aller DJ-Kulturen. Es beeinflusste direkt die New Yorker Hip-Hop-DJ-Szene der 1970er, die jamaikanische Einwanderer wie DJ Kool Herc einbrachten."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher suedafrikanische Musikstil mit acapella-Mehrstimmigkeit und komplexen Rhythmen wurde durch Paul Simons Album 'Graceland' (1986) international bekannt?",
        answerA = "Jive",
        answerB = "Kwela",
        answerC = "Mbaqanga",
        answerD = "Isicathamiya",
        correctAnswer = 3,
        explanation = "Isicathamiya ist ein suedafrikanischer A-cappella-Gesangsstil der Zulu, der vor allem von Wanderarbeitern in den 1940ern in Hostels entwickelt wurde. Ladysmith Black Mambazo brachten ihn durch Simons 'Graceland' auf die Weltbuehne.",
        difficulty = 3,
        funFact = "Paul Simons Entscheidung, fuer 'Graceland' nach Suedafrika zu reisen und dort aufzunehmen, war waehrend des Apartheidregimes umstritten. Viele Aktivisten kritisierten es als Verst gegen den Kulturboykott; andere sahen es als Staerkung schwarzer suedafrikanischer Kuenstler."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches afrikanische Rhythmuskonzept beschreibt mehrere ueberlagerte Rhythmen, die sich gegenseitig erganzen und typisch fuer westafrikanische und afrokaribische Musik ist?",
        answerA = "Synkope",
        answerB = "Polyrhythmus",
        answerC = "Ostinato",
        answerD = "Polymeter",
        correctAnswer = 1,
        explanation = "Polyrhythmus bezeichnet das gleichzeitige Erklingen mehrerer unterschiedlicher Rhythmusmuster, die sich strukturell erganzen. In westafrikanischer Musik und ihren Abkoemmlingen (Reggae, Afrobeat, Salsa, Jazz) ist Polyrhythmus ein zentrales Element.",
        difficulty = 3,
        funFact = "Das Schlagzeug in der westlichen Musik wurde wesentlich durch afrikanische Polyrhythmik beeinflusst. Wenn ein Schlagzeuger gleichzeitig verschiedene Rhythmen mit verschiedenen Gliedmassen spielt, praktiziert er eine vereinfachte Form des afrikanischen Polyrhythmus."
    ),

)
