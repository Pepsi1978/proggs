package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsExpert2(): List<Question> = listOf(

    // --- IRANIAN NEW WAVE ---

    // Question 1 - Forough Farrokhzad
    Question(
        categoryId = 4,
        questionText = "Forough Farrokhzad drehte 1963 einen einzigen Dokumentarfilm, der als Vorlaeufer des Iranischen Neuen Kinos gilt. Wie lautet sein Titel?",
        answerA = "Khaneh Siah Ast (Das Haus ist schwarz)",
        answerB = "Gav (Der Bulle)",
        answerC = "Shazde Ehtejab (Prinz Ehtejab)",
        answerD = "Gozaresh (Der Bericht)",
        correctAnswer = 0, // A
        explanation = "Farrokhzads Kurzfilm 'Khaneh Siah Ast' (1963) ueber eine Leprakolonie gilt als wichtigster Vorlaeufer des Iranischen Neuen Kinos. Er verband poetischen Kommentar mit rohen dokumentarischen Bildern und beeinflusste Abbas Kiarostami massgeblich.",
        difficulty = 4,
        funFact = "Farrokhzad starb 1967 bei einem Autounfall mit nur 32 Jahren. Der Film war ihr einziges visuelles Werk neben ihrer umfangreichen Lyrik."
    ),

    // Question 2 - Dariush Mehrjui Gav
    Question(
        categoryId = 4,
        questionText = "Dariush Mehrjuis Film 'Gav' (1969) gilt als Startpunkt des Iranischen Neuen Kinos. Welches literarische Werk bildete die Vorlage?",
        answerA = "Eine Novelle von Sadegh Hedayat",
        answerB = "Eine Kurzgeschichte von Gholam-Hossein Saedi",
        answerC = "Ein Roman von Sadegh Chubak",
        answerD = "Ein Theaterstueck von Bahram Beizai",
        correctAnswer = 1, // B
        explanation = "Dariush Mehrjui adaptierte eine Kurzgeschichte des Psychiaters und Dramatikers Gholam-Hossein Saedi. Der Film wurde von der Zensur des Schahs zunaechst verboten und erst nach seiner Aufnahme beim Venezianischen Filmfestival 1971 freigegeben.",
        difficulty = 4,
        funFact = "Nach der Islamischen Revolution wurde 'Gav' paradoxerweise als eines der ersten Filme wieder zugelassen, da Khomeini ihn als Beispiel fuer die guten Schauspieler des iranischen Kinos lobte."
    ),

    // Question 3 - Abbas Kiarostami Koker trilogy
    Question(
        categoryId = 4,
        questionText = "Abbas Kiarostamis sogenannte Koker-Trilogie umfasst drei Filme aus dem Dorf Koker. Welcher der folgenden gehoert NICHT zur Trilogie?",
        answerA = "Wo ist das Haus meines Freundes? (1987)",
        answerB = "Das Leben geht weiter (1992)",
        answerC = "Nema-ye Nazdik - Nahaufnahme (1990)",
        answerD = "Durch den Olivenhain (1994)",
        correctAnswer = 2, // C
        explanation = "'Nahaufnahme' (Nema-ye Nazdik, 1990) gehoert nicht zur Koker-Trilogie. Die Trilogie besteht aus 'Wo ist das Haus meines Freundes?' (1987), 'Das Leben geht weiter' (1992) und 'Durch den Olivenhain' (1994), alle drei in und um das Dorf Koker gedreht.",
        difficulty = 4,
        funFact = "'Das Leben geht weiter' entstand nach dem schweren Erdbeben im Nordiran 1990. Kiarostami reiste ins Erdbebengebiet, um nach den Kinderdarstellern seines ersten Films zu suchen."
    ),

    // Question 4 - Jafar Panahi Verbote
    Question(
        categoryId = 4,
        questionText = "Jafar Panahi wurde 2010 vom iranischen Regime verurteilt. Was besagt das Urteil genau?",
        answerA = "10 Jahre Haft und lebenslanges Berufsverbot",
        answerB = "6 Jahre Haft, 20 Jahre Berufsverbot und Reiseverbot",
        answerC = "6 Jahre Haft und 20 Jahre Filmverbot ohne Reiseverbot",
        answerD = "Lebenslange Haft, spaeter in Hausarrest umgewandelt",
        correctAnswer = 1, // B
        explanation = "Jafar Panahi wurde 2010 zu 6 Jahren Haft, einem 20-jaehrigen Filmverbot und einem Reiseverbot verurteilt. Er blieb trotzdem aktiv und drehte im Verborgenen Filme wie 'This Is Not a Film' (2011), der auf einem USB-Stick aus dem Iran geschmuggelt wurde.",
        difficulty = 4,
        funFact = "'This Is Not a Film' wurde in einer Torte zum Filmfestival Cannes geschmuggelt und dort uraufgefuehrt."
    ),

    // --- TAIWANESE NEW CINEMA ---

    // Question 5 - Hou Hsiao-hsien A City of Sadness
    Question(
        categoryId = 4,
        questionText = "Hou Hsiao-hsiens Film 'A City of Sadness' (1989) war der erste taiwanesische Film, der ein bestimmtes historisches Thema offen behandelte. Welches?",
        answerA = "Die japanische Kolonialherrschaft 1895-1945",
        answerB = "Das Massaker vom 28. Februar 1947 und den Weissen Terror",
        answerC = "Den Buergerkrieg zwischen KMT und Kommunisten",
        answerD = "Die Gruendung der Republik China auf Taiwan",
        correctAnswer = 1, // B
        explanation = "Das Massaker vom 28. Februar 1947 war bis dahin ein tabuisiertes Thema in Taiwan. Bei diesem Ereignis toetete das KMT-Regime zehntausende einheimische Taiwanesen. 'A City of Sadness' gewann den Goldenen Loewen in Venedig und brach damit dieses gesellschaftliche Tabu.",
        difficulty = 4,
        funFact = "Tony Leung Chiu-wai spielte in 'A City of Sadness' einen tauben Fotografen, weil er kein Mandarin sprach und Hou diese Sprachbarriere kreativ umging."
    ),

    // Question 6 - Edward Yang Yi Yi
    Question(
        categoryId = 4,
        questionText = "Edward Yangs 'Yi Yi' (2000) gewann in Cannes den Preis fuer den Besten Regisseur. Welche formale Besonderheit macht den Film unter den Werken des Taiwanese New Cinema besonders herausragend?",
        answerA = "Er ist komplett ohne Dialogton gedreht",
        answerB = "Er hat eine Laufzeit von fast 3 Stunden mit parallelen Familienstrang-Erzaehlungen",
        answerC = "Er ist als einziger Film der Bewegung in Schwarz-Weiss gehalten",
        answerD = "Er verwendet ausschliesslich Laiendarsteller ohne professionelle Schauspieler",
        correctAnswer = 1, // B
        explanation = "'Yi Yi' hat eine Laufzeit von 173 Minuten und verfolgt drei Generationen einer Taipeh-Familie in parallelen Erzaehlstraengen. Diese epische Struktur bei gleichzeitiger Intimaet gilt als Meisterleistung. Yang starb 2007, sodass 'Yi Yi' sein letzter Spielfilm blieb.",
        difficulty = 4,
        funFact = "Edward Yang verstand den Film als Reflexion ueber Zeit: Grossvater am Lebensende, Vater in der Lebensmitte, Kind am Beginn — alle drei sind gleichermassen verloren."
    ),

    // Question 7 - Tsai Ming-liang Stil
    Question(
        categoryId = 4,
        questionText = "Tsai Ming-liang gilt als wichtiger Vertreter des Post-New-Cinema in Taiwan. Welches formale Merkmal ist fuer sein Werk am charakteristischsten?",
        answerA = "Extrem lange ungeschnittene Einstellungen mit minimaler Handlung (Slow Cinema)",
        answerB = "Schnelle Schnittfolgen und handheld-Kamera im Cinéma-vérité-Stil",
        answerC = "Aufwendige Choreografien mit Musical-Elementen",
        answerD = "Nutzung von nur einem einzigen Schauspieler pro Film",
        correctAnswer = 0, // A
        explanation = "Tsai Ming-liangs Stil ist durch extrem lange Plansequenzen ohne Dialog gepraegt. Filme wie 'Stray Dogs' (2013) enthalten Einstellungen von 14 Minuten Laenge. Sein Kino gilt als extremster Vertreter des 'Slow Cinema', das Empfinden von Zeit selbst zum Gegenstand macht.",
        difficulty = 4,
        funFact = "Tsais Stammschauspieler Lee Kang-sheng erscheint in fast allen seinen Filmen und taucht auch in Kurzfilmen und Installationswerken auf."
    ),

    // --- BRAZILIAN CINEMA NOVO ---

    // Question 8 - Cinema Novo Manifest
    Question(
        categoryId = 4,
        questionText = "Wer formulierte den Schlachtruf des brasilianischen Cinema Novo: 'Uma camera na mao e uma ideia na cabeca' (Eine Kamera in der Hand und eine Idee im Kopf)?",
        answerA = "Glauber Rocha",
        answerB = "Nelson Pereira dos Santos",
        answerC = "Joaquim Pedro de Andrade",
        answerD = "Paulo Saraceni",
        correctAnswer = 0, // A
        explanation = "Glauber Rocha formulierte diesen Schlachtruf als programmatischen Gegenentwurf zum teuren Hollywood-Kino. Rocha war die praegendste Stimme des Cinema Novo und veroffentlichte 1965 auch das Manifest 'Aesthetik des Hungers', das die Armut als revolutionaere Kraft feierte.",
        difficulty = 4,
        funFact = "Glauber Rochas 'Deus e o Diabo na Terra do Sol' (1964) verbindet Cangacos (Outlaws), religioesen Fanatismus und soziale Revolution in einer mythischen Landschaft des brasilianischen Sertao."
    ),

    // Question 9 - Glauber Rocha Aesthetik des Hungers
    Question(
        categoryId = 4,
        questionText = "In Glauber Rochas Manifest 'Aesthetik des Hungers' (1965) formuliert er eine zentrale politische These. Welche?",
        answerA = "Nur staatlich gefoerdertes Kino kann gesellschaftliche Veraenderung bewirken",
        answerB = "Hunger und Elend sind keine Themen fuer Mitleid, sondern Quellen revolutionaerer Gewalt",
        answerC = "Das Kino muss alle Colonos auf Portugiesisch zugaenglich sein",
        answerD = "Die Zusammenarbeit mit Hollywood ist der einzige Weg zur globalen Verbreitung",
        correctAnswer = 1, // B
        explanation = "Rocha argumentierte, dass die Gewalt der Armen eine logische und legitime Antwort auf die koloniale Unterdrueckung sei. Hunger sei keine Tragoedie, die Mitgefuehl verdiene, sondern Ausdruck eines Systems, gegen das Rebellion notwendig ist. Dieses Manifest beeinflusste den gesamten lateinamerikanischen politischen Film.",
        difficulty = 4,
        funFact = "Das Manifest wurde erstmals beim Europaeischen Filmkongress in Genua 1965 vorgelesen und sorgte international fuer Aufsehen."
    ),

    // Question 10 - Nelson Pereira dos Santos
    Question(
        categoryId = 4,
        questionText = "Nelson Pereira dos Santos' Film 'Vidas Secas' (1963) adaptierte einen beruhmten brasilianischen Roman. Von welchem Autor?",
        answerA = "Jorge Amado",
        answerB = "Graciliano Ramos",
        answerC = "Joao Guimaraes Rosa",
        answerD = "Clarice Lispector",
        correctAnswer = 1, // B
        explanation = "Graciliano Ramos schrieb den Roman 'Vidas Secas' (1938) ueber eine verarmte Familie im Sertao. Nelson Pereira dos Santos adaptierte ihn mit neo-realistischen Mitteln, gedreht in extremer Trockenheit Nordbrasiliens. Der Film gilt als einer der Schluessel des Cinema Novo.",
        difficulty = 4,
        funFact = "Graciliano Ramos war selbst ein Kommunist, der unter Vargas inhaftiert wurde und das Gefaengnisleben in 'Memorias do Carcere' beschrieb."
    ),

    // --- SENEGALESE CINEMA / OUSMANE SEMBENE ---

    // Question 11 - Sembene erster afrikanischer Film
    Question(
        categoryId = 4,
        questionText = "Ousmane Sembene gilt als Vater des afrikanischen Kinos. Sein Kurzfilm von 1963 war der erste Film eines Subsahara-Afrikaners. Wie lautet der Titel?",
        answerA = "La Noire de... (Das schwarze Maedchen von...)",
        answerB = "Borom Sarret",
        answerC = "Mandabi (Die Geldanweisung)",
        answerD = "Xala",
        correctAnswer = 1, // B
        explanation = "'Borom Sarret' (1963) ist ein 20-minuetiger Kurzfilm ueber einen Karrenzieher in Dakar und gilt als erster Film eines Subsahara-Afrikaners. 'La Noire de...' (1966) war Sembenes erster Spielfilm und der erste Spielfilm eines schwarzafrikanischen Filmemachers.",
        difficulty = 4,
        funFact = "Sembene studierte Film in Moskau an der VGIK (Sowjetisches Staatsinstitut fuer Filmkunst) und liess sich von Eisenstein und Pudowkin beeinflussen."
    ),

    // Question 12 - Sembene La Noire de
    Question(
        categoryId = 4,
        questionText = "In welcher Sprache ist Sembenes 'La Noire de...' (1966) gedreht, und was ist die politische Bedeutung dieser Entscheidung?",
        answerA = "In Wolof, um das koloniale Franzoesisch zu verweigern",
        answerB = "In Franzoesisch, um internationale Verbreitung zu sichern und den Kolonialismus von innen zu kritisieren",
        answerC = "In Englisch, um das amerikanische Publikum anzusprechen",
        answerD = "Ohne jeglichen Dialog, ausschliesslich mit Bildsprache",
        correctAnswer = 1, // B
        explanation = "'La Noire de...' ist in Franzoesisch gedreht, was eine bewusste Entscheidung war: Sembene nutzte die Sprache der Kolonisatoren, um die koloniale Ausbeutung anzuklagen. Spaeter wechselte er bewusst zu Wolof, um sein Publikum in Senegal direkt anzusprechen.",
        difficulty = 4,
        funFact = "Sembene nannte sich selbst einen 'Griot mit einer Kamera' - Griots sind traditionelle westafrikanische Geschichtenerzaehler, Musiker und Geschichtshueter."
    ),

    // Question 13 - Sembene Xala
    Question(
        categoryId = 4,
        questionText = "Sembenes 'Xala' (1975) ist eine politische Satire. Was bedeutet 'Xala' auf Wolof, und was ist die Kernaussage des Films?",
        answerA = "Reichtum; Kritik an westlichem Materialismus im postkolonialen Afrika",
        answerB = "Impotenz; die neue afrikanische Bourgeoisie ist unfaehig, ohne koloniale Strukturen zu existieren",
        answerC = "Fluch; traditionelle Hexerei bedroht den modernen Staat",
        answerD = "Korruption; westliche NGOs korrumpieren afrikanische Institutionen",
        correctAnswer = 1, // B
        explanation = "'Xala' bedeutet auf Wolof 'Impotenz' (ritueller Fluch). Der Film zeigt einen senegalesischen Geschaeftsmann, der am Tag seiner dritten Hochzeit impotent wird. Der Fluch ist eine Metapher: Die neue afrikanische Elite, die die Strukturen des Kolonialismus uebernahm, ist unfaehig, selbst zu regieren.",
        difficulty = 4,
        funFact = "Der Film wurde in Senegal von der Regierung zensiert, da er die politische Elite zu deutlich persiflierte."
    ),

    // --- PHILIPPINE NEW WAVE ---

    // Question 14 - Lav Diaz
    Question(
        categoryId = 4,
        questionText = "Lav Diaz ist der bekannteste Vertreter des Philippine New Wave. Wofuer sind seine Filme vor allem bekannt?",
        answerA = "Exzessiv kurze Laufzeiten unter 30 Minuten als politisches Statement",
        answerB = "Extreme Laufzeiten von 6 bis 12+ Stunden in Schwarzweiss",
        answerC = "Digitale Hochgeschwindigkeitskamera-Techniken",
        answerD = "Ausschliessliche Nutzung von Kinderschauspielern",
        correctAnswer = 1, // B
        explanation = "Lav Diaz dreht Filme von aussergewoehnlicher Laenge: 'Evolution of a Filipino Family' dauert 11 Stunden, 'From What Is Before' (2014, Goldener Loewe Locarno) ueber 5 Stunden. Er betrachtet die extreme Laenge als politische und aesthetische Notwendigkeit, um die Zeit und Geschichte der Philippinen erfahrbar zu machen.",
        difficulty = 4,
        funFact = "Diaz dreht oft in einer einzigen langen Produktion ueber Monate, schreibt das Drehbuch unterwegs und hat fast kein Budget."
    ),

    // Question 15 - Kidlat Tahimik
    Question(
        categoryId = 4,
        questionText = "Kidlat Tahimik gilt als Vater des philippinischen Independentkinos. Sein Debuetfilm von 1977 gewann bei der Berlinale den FIPRESCI-Preis. Wie lautet der Titel?",
        answerA = "Mababangong Bangungot (Perfumado)",
        answerB = "Turumba",
        answerC = "Memories of Overdevelopment",
        answerD = "Ang Babaeng Humayo",
        correctAnswer = 0, // A
        explanation = "'Mababangong Bangungot' (auf Englisch 'Perfumado', 1977) ist ein halbautobiografischer Film ueber einen philippinischen Mann in Paris. Er gilt als Gruendungswerk des philippinischen Independentkinos und kritisiert die kulturelle Unterwerfung unter den Westen.",
        difficulty = 4,
        funFact = "Kidlat Tahimik drehte den Film mit einer 16mm-Kamera weitgehend allein und ohne professionelle Schauspieler."
    ),

    // --- MAYA DEREN ---

    // Question 16 - Maya Deren Meshes of the Afternoon
    Question(
        categoryId = 4,
        questionText = "Maya Deren und Alexander Hammid drehten 1943 'Meshes of the Afternoon'. Was gilt als die bedeutendste technische Innovation dieses Films fuer den experimentellen Film?",
        answerA = "Der erste Einsatz von Split-Screen-Technik im Kurzfilm",
        answerB = "Nutzung von subjektiver Kamera und Loop-Strukturen zur Darstellung unbewusster Psyche",
        answerC = "Erste Verwendung von Farbfilm in einem Avantgarde-Werk",
        answerD = "Einsetzen von Stroboskoplicht zur Bewusstseinserweiterung",
        correctAnswer = 1, // B
        explanation = "Derens Film nutzt subjektive Kamerafahrten, Loop-Strukturen (Sequenzen wiederholen sich mit Variationen) und Traumlogik, um den inneren psychischen Zustand darzustellen. Der Film gilt als Grundstein des amerikanischen avantgardistischen Kinos und der 'trance film'-Tradition.",
        difficulty = 4,
        funFact = "Maya Deren wurde in Kiew geboren und kam als Teenager in die USA. Ihr Vater war Psychiater, was ihre Faszination fuer das Unbewusste beeinflusste."
    ),

    // Question 17 - Maya Deren Theorie
    Question(
        categoryId = 4,
        questionText = "In welchem Essay-Band beschrieb Maya Deren ihre Filmtheorie und unterschied zwischen 'vertikalem' und 'horizontalem' Kino?",
        answerA = "An Anagram of Ideas on Art, Form and Film (1946)",
        answerB = "The Poetic Cinema and the Film Experience (1953)",
        answerC = "At Land: Notes on the Film Experience (1944)",
        answerD = "Experimental Film: A New Art Form (1949)",
        correctAnswer = 0, // A
        explanation = "In 'An Anagram of Ideas on Art, Form and Film' (1946) unterschied Deren zwischen 'horizontalem' (narrativem, kausalen) und 'vertikalem' (poetischem, intensiven) Kino. Das vertikale Kino vertiefe einen Moment, statt Ereignisse kausal zu verknuepfen.",
        difficulty = 4,
        funFact = "Deren bezeichnete Filmemachen als 'die poetische Form des 20. Jahrhunderts' und lehrte an der New School for Social Research in New York."
    ),

    // --- STAN BRAKHAGE ---

    // Question 18 - Stan Brakhage Dog Star Man
    Question(
        categoryId = 4,
        questionText = "Stan Brakhages Werk 'Dog Star Man' (1961-1964) besteht aus einem Prolog und vier Teilen. Was ist die zentrale formale Technik?",
        answerA = "Mehrfache Belichtung von Filmstreifen, Direktbearbeitung der Emulsion und Echtzeit-Verarbeitung",
        answerB = "Computeranimation verbunden mit dokumentarischen Aufnahmen",
        answerC = "Synchronisierter mehrkanaliger Ton mit abstrakten Bildern",
        answerD = "Handgezeichnete Animationen auf 8mm-Filmstreifen",
        correctAnswer = 0, // A
        explanation = "Brakhage nutzte extensive Mehrfachbelichtungen, ritzkte direkt in die Filmemulsion, bemalt Filmstreifen mit Farbe und Tinte. Das Ergebnis sind polyphone visuelle Texturen ohne lineares Narrativ. 'Dog Star Man' gilt als monumentales Werk des nicht-narrativen amerikanischen Films.",
        difficulty = 4,
        funFact = "Brakhage drehte auch mit blossem Auge, indem er direkt in die Sonne schaute und die resultierenden Nachbilder auf Film bannte."
    ),

    // Question 19 - Brakhage Mothlight
    Question(
        categoryId = 4,
        questionText = "Stan Brakhages 'Mothlight' (1963) ist ein Film ohne Kamera. Wie wurde er hergestellt?",
        answerA = "Durch Projection von Licht auf fotosensitives Papier",
        answerB = "Durch direktes Aufkleben von Mottenfluegelresten, Blueten und Grashalmen zwischen zwei Filmstreifen",
        answerC = "Durch Fotografieren von Motten unter dem Mikroskop",
        answerD = "Durch Verbrennen von Zellulosematerial auf transparentem Film",
        correctAnswer = 1, // B
        explanation = "Brakhage klebte tatsaechliche Mottenfluegel, Bluetenblaetter und Grashalme zwischen zwei transparente Klebefilmstreifen, die dann durch den Projektor liefen. Das Ergebnis ist ein flackerndes, organisches Bild. Die Technik nennt sich 'cameraless filmmaking'.",
        difficulty = 4,
        funFact = "'Mothlight' dauert nur 3 Minuten und 12 Sekunden, wurde aber in der George Eastman Museum Collection als eines der bedeutendsten Kunstwerke des 20. Jahrhunderts archiviert."
    ),

    // --- KENNETH ANGER ---

    // Question 20 - Kenneth Anger Scorpio Rising
    Question(
        categoryId = 4,
        questionText = "Kenneth Angers 'Scorpio Rising' (1963) war ein Pionierwerk des Einsatzes von Rockmusik im Film. Welche zusaetzliche Dimension macht den Film zu einem Klassiker der Queer-Filmgeschichte?",
        answerA = "Er war der erste Film mit offen schwulem Hauptcharakter in Hollywood",
        answerB = "Er kombinierte homo-erotische Bilder von Bikern mit Nazi-Ikonographie und Pop-Ikonographie in einem subversiven Kommentar",
        answerC = "Er dokumentierte das erste offen schwule Filmfestival in San Francisco",
        answerD = "Er zeigte erstmals eine gleichgeschlechtliche Heirat im amerikanischen Kino",
        correctAnswer = 1, // B
        explanation = "'Scorpio Rising' montiert homo-erotische Bilder von Motorradfahrern, Nazi-Symbolik, Jesus-Filme und Rock-and-Roll zu einem subversiven Kommentar ueber amerikanische Maennlichkeit, Fetischismus und faschistische Aesthetik. Der Film beeinflusste die Queer-Theorie und wurde wegen Obszoenitaet angeklagt.",
        difficulty = 4,
        funFact = "Anger verwendete 13 Popsongs wie Elvis Presley und Ricky Nelson — das war bahnbrechend, da Pop-Songs zuvor nicht als ernste Filmmusik verwendet wurden."
    ),

    // Question 21 - Anger Magick Lantern Cycle
    Question(
        categoryId = 4,
        questionText = "Kenneth Angers Werkzyklus 'Magick Lantern Cycle' ist stark von welchem okkulten System beeinflusst?",
        answerA = "Satanskult nach LaVey (Church of Satan)",
        answerB = "Thelema und Aleister Crowleys Magick-System",
        answerC = "Rosenkreuzertum des fruehen 17. Jahrhunderts",
        answerD = "Kabbala und juedische Mystik",
        correctAnswer = 1, // B
        explanation = "Anger war ein ueberzeugter Thelemit und bekennender Crowley-Anhaenger. Sein gesamter Werkzyklus 'Magick Lantern Cycle' verwendet Crowleys okkultistische Symbolik. 'Inauguration of the Pleasure Dome' (1954) und 'Lucifer Rising' (1972) sind die prominentesten Beispiele.",
        difficulty = 4,
        funFact = "Anger kaufte Crowleys ehemaliges Haus Boleskine House am Loch Ness und lebte dort zeitweise. Das Haus brannte 2015 aus."
    ),

    // --- ANDY WARHOL FILMS ---

    // Question 22 - Andy Warhol Empire
    Question(
        categoryId = 4,
        questionText = "Andy Warhols Film 'Empire' (1964) zeigt ungeschnitten das Empire State Building. Wie lang ist der Film und welche Idee steckt dahinter?",
        answerA = "1 Stunde; Kommentar auf die Vergaenglichkeit des Kapitalismus",
        answerB = "8 Stunden 5 Minuten; Film als Zeitlichkeitserfahrung, nicht als Narrativ",
        answerC = "24 Stunden; Simulation eines vollstaendigen Tages in New York",
        answerD = "3 Stunden; Hommage an King Kong und Architektur",
        correctAnswer = 1, // B
        explanation = "'Empire' dauert 8 Stunden und 5 Minuten, zeigt das Empire State Building in einer einzigen fixen Einstellung, aufgenommen in einer Nacht. Warhol sagte: 'Ich wollte, dass du an den Film denken kannst, ohne ihn sehen zu muessen.' Es geht um die Erfahrung von Zeit selbst.",
        difficulty = 4,
        funFact = "Der Film wurde mit 16 fps aufgenommen, aber mit 24 fps projiziert, was ihn beschleunigt und Tagesanbruch zeigt."
    ),

    // Question 23 - Warhol Screen Tests
    Question(
        categoryId = 4,
        questionText = "Warhols 'Screen Tests' (1964-66) umfassen mehr als 400 Kurzfilme. Wie wurden diese gedreht und was ist ihr kuenstlerisches Konzept?",
        answerA = "3-Minuten-Aufnahmen sitzender Personen mit fixer Kamera, als 'lebende Portraets' konzipiert",
        answerB = "Heimliche Aufnahmen von Menschen in der Factory ohne deren Wissen",
        answerC = "Tanzperformances zu Velvet-Underground-Musik",
        answerD = "Improvisierte Gespraeche ueber Drogen und Sex",
        correctAnswer = 0, // A
        explanation = "Warhol bat seine Besucher in der Factory, 3 Minuten still in die Kamera zu blicken. Die stummen, in Schwarzweiss gedrehten Kurzfilme werden als 'living portraits' bezeichnet. Beim Abspielen mit der empfohlenen reduzierten Geschwindigkeit entstehen fast fotografisch wirkende Sequenzen.",
        difficulty = 4,
        funFact = "Die 'Screen Tests' zeigen unter anderem Bob Dylan, Edie Sedgwick, Lou Reed, Nico und Susan Sontag."
    ),

    // --- PARALLEL CINEMA INDIEN ---

    // Question 24 - Satyajit Ray Apu-Trilogie
    Question(
        categoryId = 4,
        questionText = "Satyajit Rays 'Pather Panchali' (1955) begann das Parallel Cinema in Indien. Welche Institution finanzierte diesen Film nach langem Scheitern bei kommerziellen Produzenten?",
        answerA = "Das British Film Institute",
        answerB = "Die Regierung von Westbengalen",
        answerC = "Das Ford Foundation-Stipendium",
        answerD = "Jawaharlal Nehrus persoenliche Foerderung",
        correctAnswer = 1, // B
        explanation = "Ray begann den Film 1952 ohne gesicherte Finanzierung. Erst die Regierung Westbengalens gab ihm ein Darlehen, das er aus Musikrechten und dem Verkauf seiner Frau Bijoya Rays Schmuck ergaenzte. Der Film debuetuierte 1955 und gewann 1956 in Cannes den Preis fuer das 'bestes menschliches Dokument'.",
        difficulty = 4,
        funFact = "Ravi Shankar komponierte die Musik fuer den Film, was eine der fruehesten Film-Zusammenarbeiten des Sitarvirtuosen war."
    ),

    // Question 25 - Ritwik Ghatak
    Question(
        categoryId = 4,
        questionText = "Ritwik Ghatak wird neben Satyajit Ray als zweite Saeulengestalt des bengalischen Parallel Cinema betrachtet. Was unterscheidet seinen Ansatz von Rays?",
        answerA = "Ghatak nutzte ausschliesslich Farbfilm, Ray fast ausschliesslich Schwarzweiss",
        answerB = "Ghataks Filme sind brechtscher, melodramatischer und politisch radikaler mit Fokus auf die Teilung Bengalens",
        answerC = "Ghatak drehte ausschliesslich Dokumentarfilme, keine Spielfilme",
        answerD = "Ghatak war rein kommerzieller Filmemacher, Ray der Kunstfilmer",
        correctAnswer = 1, // B
        explanation = "Ghataks Werk ist melodramatischer und brechtscher als Rays elegantes Erzaehlkino. Seine Trilogie ueber die Teilung Bengalens (Meghe Dhaka Tara, Komal Gandhar, Subarnarekha) zeigt zerbrochene Familien als Spiegel der Partition von 1947. Ghatak trank schwer und starb 1976 mit 50 Jahren.",
        difficulty = 4,
        funFact = "Ghatak lehrte am Film and Television Institute of India (FTII) in Pune und beeinflusste spaetere Filmemacher wie Kumar Shahani massgeblich."
    ),

    // Question 26 - Shyam Benegal NFDC
    Question(
        categoryId = 4,
        questionText = "Shyam Benegal war ein Schluessel-Figur des Parallel Cinema der 1970er Jahre. Welche Institution wurde 1975 gegruendet, um das indische Arthouse-Kino institutionell zu foerdern?",
        answerA = "NFDC - National Film Development Corporation",
        answerB = "FFC - Film Finance Corporation (und spaeter NFDC)",
        answerC = "CBFC - Central Board of Film Certification",
        answerD = "FTII - Film and Television Institute of India",
        correctAnswer = 1, // B
        explanation = "Die Film Finance Corporation (FFC) wurde 1960 gegruendet und finanzierte Rays und Ghataks fruehe Werke. 1975 wurde sie zur National Film Development Corporation (NFDC) umstrukturiert. Beide Institutionen foerderten das Parallel Cinema systematisch gegen das Bollywood-Mainstream-Kino.",
        difficulty = 4,
        funFact = "Shyam Benegals 'Ankur' (1974) war einer der ersten FFC/NFDC-geförderten Erfolge und machte Shabana Azmi und Smita Patil als Stars des Parallel Cinema bekannt."
    ),

    // --- NOLLYWOOD ---

    // Question 27 - Nollywood Grundung
    Question(
        categoryId = 4,
        questionText = "Der Nollywood-Boom begann 1992 mit einem Film, der als erster direkt auf Video verteilt wurde. Wie lautet der Titel?",
        answerA = "Glamour Girls",
        answerB = "Living in Bondage",
        answerC = "Ritual",
        answerD = "True Confessions",
        correctAnswer = 1, // B
        explanation = "'Living in Bondage' (1992) von Kenneth Nnebue gilt als Geburtsfilm Nollywoods. Er wurde direkt auf VHS veroeffentlicht, verkaeufte sich ueber eine Million Mal und bewies, dass nigerianische Geschichten ein riesiges Publikum hatten. Das direkte Video-Vertriebsmodell vermied teure Kinoverleih-Strukturen.",
        difficulty = 4,
        funFact = "'Living in Bondage' wurde auf Igbo produziert und hatte spaeter einen englischen Untertitel bekommen — das war bewusst, um die eigene Sprache zu valorisieren."
    ),

    // Question 28 - Nollywood Produktionszahlen
    Question(
        categoryId = 4,
        questionText = "Nollywood ist nach Bollywood die produktivste Filmindustrie der Welt. Wieviele Filme produziert sie jaehrlich ungefaehr?",
        answerA = "200-300 Filme",
        answerB = "500-600 Filme",
        answerC = "1.500-2.500 Filme",
        answerD = "5.000+ Filme",
        correctAnswer = 2, // C
        explanation = "Nollywood produziert jaehrlich etwa 1.500 bis 2.500 Filme und erreicht damit global Rang 2 nach Bollywood. Die extrem niedrigen Budgets (oft unter 15.000 USD pro Film), schnelle Drehzeiten (7-10 Tage) und direkte Video/DVD-Distribution machen dieses Volumen moeglich.",
        difficulty = 4,
        funFact = "Nollywood beschaeftigt Millionen Menschen und ist nach der Landwirtschaft der zweitgroesste Arbeitgeber Nigerias."
    ),

    // --- DOGME 95 ---

    // Question 29 - Dogme 95 Keuschheitsgeluebde
    Question(
        categoryId = 4,
        questionText = "Das Dogme-95-Keuschheitsgeluebde enthaelt 10 Regeln. Welche der folgenden ist KEINE originale Dogme-Regel?",
        answerA = "Musik darf nur verwendet werden, wenn sie in der Szene diegetisch vorkommt",
        answerB = "Drehbuch und Regie muessen von derselben Person stammen",
        answerC = "Der Regisseur darf nicht im Abspann erscheinen",
        answerD = "Mord und Waffen sind im Film verboten",
        correctAnswer = 3, // D
        explanation = "Das Keuschheitsgeluebde verbietet NICHT explizit Gewalt. Die echten Regeln umfassen u.a.: Drehort-Authentizitaet, keine Requisiten, Handkamera, keine optischen Filter, keine nicht-diegetische Musik, Gegenwartshandlung, kein Genrefilm, kein Breitbild (1.33:1-Format) und keine Director-Credit.",
        difficulty = 4,
        funFact = "Lars von Trier gab zu, dass 'Idioten' (1998) selbst mehrere Dogme-Regeln brach, was zum programmatischen Widerspruch des Manifests gehoerte."
    ),

    // Question 30 - Dogme 95 Susanne Bier
    Question(
        categoryId = 4,
        questionText = "Susanne Bier drehte mit 'Open Hearts' (2002) einen offiziell zertifizierten Dogme-Film. Welche spaeteren Auszeichnungen machen ihren Werdegang nach Dogme besonders bemerkenswert?",
        answerA = "Oscar fuer Besten Fremdsprachigen Film (In einer besseren Welt, 2011) und BAFTA fuer The Night Manager",
        answerB = "Goldener Loewe Venedig und Goldene Palme Cannes",
        answerC = "Drei Silberne Baeren Berlin in Folge",
        answerD = "Emmy und Golden Globe fuer dasselbe Werk",
        correctAnswer = 0, // A
        explanation = "Susanne Bier erhielt 2011 den Oscar fuer den Besten Nicht-Englischsprachigen Film fuer 'In einer besseren Welt'. Spaeter gewann sie den BAFTA fuer 'The Night Manager' (2016). Ihr Weg vom No-Budget-Dogme-Film zum internationalen Prestige-TV ist aussergewoehnlich.",
        difficulty = 4,
        funFact = "Dogme 95 wurde am 20. Maerz 1995 in Paris von Lars von Trier und Thomas Vinterberg mit einem roten Pamphlet oeffentlich proklamiert."
    ),

    // Question 31 - Dogme 95 Harmony Korine
    Question(
        categoryId = 4,
        questionText = "Der amerikanische Regisseur Harmony Korine drehte den einzigen amerikanischen zertifizierten Dogme-Film. Wie heisst er?",
        answerA = "Kids (1995)",
        answerB = "Gummo (1997)",
        answerC = "julien donkey-boy (1999)",
        answerD = "Trash Humpers (2009)",
        correctAnswer = 2, // C
        explanation = "'julien donkey-boy' (1999) ist der einzige Dogme-Film aus den USA und erhielt das offizielle Zertifikat #7 des Dogme-Kollektivs. Korine arbeitete mit Werner Herzog, der eine Nebenrolle spielte.",
        difficulty = 4,
        funFact = "Werner Herzog spielte in 'julien donkey-boy' den missbilligenden Vater — er und Korine wurden danach zu Freunden und gegenseitigen Bewunderern."
    ),

    // Question 32 - Dogme 95 Kristian Levring
    Question(
        categoryId = 4,
        questionText = "Welcher der folgenden Dogme-95-Kerngruender drehte mit 'The King Is Alive' (2000) in der Wueste Namibias eine radikale Adaption eines Shakespeare-Stuecks?",
        answerA = "Lars von Trier",
        answerB = "Thomas Vinterberg",
        answerC = "Soren Kragh-Jacobsen",
        answerD = "Kristian Levring",
        correctAnswer = 3, // D
        explanation = "Kristian Levring, einer der vier Gruender von Dogme 95, drehte 'The King Is Alive' (2000) mit einer Gruppe Gestrandeter in der Wueste Namibias, die 'King Lear' aufzufuehren versuchen. Der Film erhielt das Zertifikat #4 des Dogme-Kollektivs.",
        difficulty = 4,
        funFact = "Die vier Gruender von Dogme 95 waren Lars von Trier, Thomas Vinterberg, Soren Kragh-Jacobsen und Kristian Levring — alle vier Daenen."
    ),

    // --- EXPERIMENTELLER FILM: VERTIEFTE FRAGEN ---

    // Question 33 - Deren At Land
    Question(
        categoryId = 4,
        questionText = "In Maya Derens 'At Land' (1944) wandelt die Protagonistin durch traumartige Raeume. Welche spezifische Schnitttechnik machte den Film zu einem Referenzwerk fuer Match Cuts?",
        answerA = "Grafikschnitttechnik (Abschneiden an Linienkonturen)",
        answerB = "Raeumliche Kontinuitaet durch Anschlusschnitt ueber verschiedene, nicht zusammenhaengende Raeume",
        answerC = "Zeitraffersequenzen innerhalb von Plansequenzen",
        answerD = "Rueckwaerts-Abspiel von vorwaerts gedrehtem Material",
        correctAnswer = 1, // B
        explanation = "Deren schneidet raeumlich unverbundene Orte so zusammen, dass eine Bewegung oder Geste nahtlos durch voellig verschiedene Umgebungen weiterlaueft. Dies schafft einen Traumzustand des Raumverlust und beeinflusste Filmemacher wie David Lynch massgeblich.",
        difficulty = 4,
        funFact = "Maya Deren spielte die Hauptrolle in ihren eigenen Filmen, da sie sich keine Schauspieler leisten konnte und glaubte, dass der Koerper des Regisseurs das direkteste Ausdrucksmittel sei."
    ),

    // Question 34 - Bruce Conner A Movie
    Question(
        categoryId = 4,
        questionText = "Bruce Conners 'A Movie' (1958) gilt als Grundlagenwerk des Found-Footage-Experimentalfilms. Welches Konzept illustriert der Film am eindruecklichsten?",
        answerA = "Dass Archivbilder objektive historische Wahrheit transportieren",
        answerB = "Dass Bedeutung ausschliesslich durch die Juxtaposition von Archivbildern entsteht, unabhaengig vom urspruenglichen Kontext",
        answerC = "Dass Nachrichtenbilder kuenstlerisch wertvoller sind als Spielfilm-Bilder",
        answerD = "Dass Katastrophenbilder aesthetisch ansprechend sind",
        correctAnswer = 1, // B
        explanation = "Conner montiert disparate Archivbilder (Explosionen, Naturkatastrophen, Erotik, Militaer, Sport) zu scheinbar sinnhaften Sequenzen und demonstriert, dass Bedeutung im Schnitt entsteht, nicht im Material selbst — eine radikale Erweiterung des Kuleschow-Effekts.",
        difficulty = 4,
        funFact = "Der Filmtitel ist zugleich die definitivste Aussage der Filmdefinition: 'A Movie' ist definitiv 'ein Film'."
    ),

    // Question 35 - Michael Snow Wavelength
    Question(
        categoryId = 4,
        questionText = "Michael Snows 'Wavelength' (1967) gilt als Schluessel des 'structural film'. Was ist das Konzept des Films?",
        answerA = "45-minuetiger ununterbrochener Zoom von einer Wand zur gegenueberliegenden Wand eines Lofts",
        answerB = "45-minuetiger Zoom von der Strasse auf ein Fenster eines Lofts, der mit einem Foto von Meereswellen endet",
        answerC = "45-minuetiger Zoom durch ein Teleskop auf den Mond",
        answerD = "45-minuetiger Zoom durch ein Mikroskop auf eine einzelne Zelle",
        correctAnswer = 1, // B
        explanation = "Snows 'Wavelength' ist ein 45-minuetiger langsamer Zoom durch ein New Yorker Loft auf ein Foto von Meereswellen. Sporadische menschliche Ereignisse passieren im Vordergrund (darunter ein Todesfall), aber der Zoom ignoriert sie und endet mit dem Foto. Der Film zelebriert den Vorrang der Filmform ueber Inhalt.",
        difficulty = 4,
        funFact = "Schnitt gibt es in 'Wavelength' kaum — nur Veraenderungen in Lichtfarbe, Belichtung und Filmbearbeitung markieren den Zeitverlauf."
    ),

    // --- AVANT-GARDE DETAILS ---

    // Question 36 - Jonas Mekas
    Question(
        categoryId = 4,
        questionText = "Jonas Mekas gilt als 'Godfather des amerikanischen Avantgarde-Kinos'. Welche Institution gruendete er 1962, die bis heute das wichtigste Archiv und Vertriebssystem fuer experimentelle Filme ist?",
        answerA = "Anthology Film Archives",
        answerB = "Film-Makers' Cooperative",
        answerC = "Lightcone Paris",
        answerD = "Canyon Cinema",
        correctAnswer = 1, // B
        explanation = "Mekas gruendete 1962 die Film-Makers' Cooperative in New York als Vertriebsnetzwerk fuer experimentelle Filme. Spaeter gruendete er 1970 auch die Anthology Film Archives, ein Archiv und Kino fuer avantgardistische Filme. Beide Institutionen sind bis heute aktiv.",
        difficulty = 4,
        funFact = "Mekas selbst drehte jahrzehntelang Tagebuch-Filme auf 16mm, die ihn als Pionier des autobiografischen Experimentalfilms etablierten."
    ),

    // Question 37 - Brakhage Sehtheorie
    Question(
        categoryId = 4,
        questionText = "In seinem Essay 'Metaphors on Vision' (1963) postuliert Stan Brakhage eine spezifische Sehtheorie. Was ist die Kernthese?",
        answerA = "Das Kino sollte alle Sinne simultan ansprechen, nicht nur das visuelle",
        answerB = "Das untrainierte menschliche Auge sieht mehr und besser als durch Sprache und Tradition konditioniertes Sehen",
        answerC = "Nur Schwarz-Weiss-Kino erreicht authentische visuelle Wahrheit",
        answerD = "Die Kamera kann die menschliche Retina vollstaendig ersetzen",
        correctAnswer = 1, // B
        explanation = "Brakhage fragte: 'Wie wuerde eine Hummel aussehen, wenn du von Geburt an blind waerst und ploetzlich siehst?' Er kritisierte das 'konditionierte Sehen', das Sprache und kulturelle Konventionen auferlegen, und suchte nach einem primaeren visuellen Erleben vor aller Benennung.",
        difficulty = 4,
        funFact = "Brakhage entfernte tatsaechlich seine Brille und schaute stundelang in den Himmel, um die visuellen Phaenomene zu studieren, die er dann in seine Filme uebertrug."
    ),

    // Question 38 - Anger Inauguration of the Pleasure Dome
    Question(
        categoryId = 4,
        questionText = "Kenneth Angers 'Inauguration of the Pleasure Dome' (1954/1966) zeigt eine okkulte Kostuemorgie. Welches Musikalbueck bildet die Tonspur der revidierten Version von 1966?",
        answerA = "Erik Saties Gymnopedies",
        answerB = "Janaceks Glagolitische Messe",
        answerC = "Kraftwerks Autobahn",
        answerD = "Richard Wagners Parsifal",
        correctAnswer = 1, // B
        explanation = "Anger vertonte die 1966-Version mit Janaceks Glagolitischer Messe, die er als passend fuer die rituelle, nicht-westliche spirituelle Atmosphaere empfand. Fruehversionen hatten Leos Janaceks 'Sinfonietta' oder andere Klassiker als Soundtrack.",
        difficulty = 4,
        funFact = "Der Film zeigt Marjorie Cameron, die Witwe des Crowley-Schuelers Jack Parsons, der eine der Schluessel-Figuren der amerikanischen Raketenentwicklung war."
    ),

    // --- WEITERE BEWEGUNGEN UND VERTIEFUNGEN ---

    // Question 39 - Sembene Ceddo
    Question(
        categoryId = 4,
        questionText = "Sembenes Film 'Ceddo' (1977) wurde in Senegal mehrere Jahre verboten. Was war offizieller Grund des Verbots?",
        answerA = "Die explizite Kritik am Islam und der Islamisierung Westafrikas",
        answerB = "Eine Streitigkeit ueber die korrekte Schreibweise des Titels laut staatlicher Orthographiereform",
        answerC = "Die Darstellung von Nacktheit und Gewalt",
        answerD = "Finanzielle Betrugsvorwuerfe gegen Sembene",
        correctAnswer = 1, // B
        explanation = "Der offiziell angegebene Grund war die Schreibweise: Die Regierung bestand auf 'Cedo' (ein 'd') laut staatlicher Wolof-Orthographienorm, Sembene bestand auf 'Ceddo' (zwei 'd'). Der eigentliche Grund war die politisch explosive Kritik am islamischen Fundamentalismus und der Sklaverei.",
        difficulty = 4,
        funFact = "Sembene beharrte auf der traditionellen Schreibweise als politisches Statement. 'Ceddo' bezeichnet Menschen, die sich dem Islam widersetzten."
    ),

    // Question 40 - Brakhage Window Water Baby Moving
    Question(
        categoryId = 4,
        questionText = "'Window Water Baby Moving' (1959) von Stan Brakhage zeigt die Geburt seines ersten Kindes. Was macht den Film innerhalb des Avantgarde-Kanons so bedeutsam?",
        answerA = "Er war der erste Film, der eine Hausgeburt zeigte",
        answerB = "Er verbindet biologischen Koerper-Dokumentarismus mit einem poetischen Schnittrhythmus und war lange in mehreren Laendern verboten",
        answerC = "Er zeigte erstmals medizinische Eingriffe im Film",
        answerD = "Er war der erste Film ohne jegliche Schnitte (ein Take)",
        correctAnswer = 1, // B
        explanation = "Brakhage zeigt die Geburt seines Sohnes ohne voyeuristischen oder medizinischen Rahmen, sondern als poetisches Erlebnis mit rhythmischem Schnitt. Der Film war jahrzehntelang in Grossbritannien und Kanada verboten. Er gilt als Beispiel dafuer, wie der private Koerper als filmisches Thema legitimiert wurde.",
        difficulty = 4,
        funFact = "Brakhages Frau Jane sagte, die Geburt und der Film seien ein gemeinsames kuenstlerisches Projekt gewesen."
    ),

    // Question 41 - Cineaste Third Cinema Theorie
    Question(
        categoryId = 4,
        questionText = "Fernando Solanas und Octavio Getino formulierten 1969 das 'Manifest des Dritten Kinos'. Was unterscheidet 'Drittes Kino' vom 'Ersten' und 'Zweiten Kino'?",
        answerA = "Erstes Kino (Hollywood), Zweites Kino (Arthouse/Auteur), Drittes Kino (revolutionaeres, anti-kolonialistisches Kino als Werkzeug der Befreiung)",
        answerB = "Erstes Kino (USA), Zweites Kino (Europa), Drittes Kino (Asien)",
        answerC = "Erstes Kino (Stummfilm), Zweites Kino (Tonfilm), Drittes Kino (Digitalfilm)",
        answerD = "Erstes Kino (Spielfilm), Zweites Kino (Dokumentarfilm), Drittes Kino (Experimentalfilm)",
        correctAnswer = 0, // A
        explanation = "Im Manifest unterschieden Solanas und Getino: Erstes Kino = Hollywood-Unterhaltungsware (kommerzielle Kolonisierung), Zweites Kino = europaeisches Autorenfilm-Kino (Individualismus ohne politische Wirkung), Drittes Kino = revolutionaeres Kino des globalen Sudens als Waffe des Volkes.",
        difficulty = 4,
        funFact = "Solanas und Getino drehten selbst mit dem 4,5-stuendigen Dokumentarfilm 'La hora de los hornos' (1968) das wichtigste Beispiel des Dritten Kinos."
    ),

    // Question 42 - Taiwanese New Cinema Manifesto
    Question(
        categoryId = 4,
        questionText = "Das 'Manifesto of Taiwanese Cinema' wurde 1987 veroffentlicht. Welche Forderung stand im Mittelpunkt?",
        answerA = "Staatliche Foerderung fuer Hochglanz-Kommerzkino gegen Hongkong-Importe",
        answerB = "Ablehnung des Unterhaltungs-Mainstreams und Einsatz fuer sozialkritisches, realistisches Kino mit lokaler Identitaet",
        answerC = "Vollstaendige Privatisierung der Filmindustrie",
        answerD = "Fokus auf digitale Filmproduktion als neue Avantgarde",
        correctAnswer = 1, // B
        explanation = "Das Manifest, unterzeichnet von Hou Hsiao-hsien, Edward Yang, Wan Jen und anderen, forderte ein Kino, das die reale taiwanesische Gesellschaft zeigt, lokale Geschichten erzaehlt und sich gegen das eskapistische Unterhaltungskino stellt. Es folgte auf die Bewegung, die 1982 mit 'In Our Time' begonnen hatte.",
        difficulty = 4,
        funFact = "Der Sammelfilm 'In Our Time' (1982) mit Segmenten von Tao Te-chen, Edward Yang, Ko Yi-cheng und Chang Yi gilt als Startschuss des Taiwanese New Cinema."
    ),

    // Question 43 - Nollywood New Nollywood
    Question(
        categoryId = 4,
        questionText = "Ab etwa 2010 entstand ein 'New Nollywood' mit hoeheren Produktionsstandards. Welcher Film von Kunle Afolayan gilt als Vorzeigeprojekt dieser Bewegung?",
        answerA = "The CEO (2016)",
        answerB = "Figurine: Araromire (2009)",
        answerC = "Phone Swap (2012)",
        answerD = "October 1 (2014)",
        correctAnswer = 1, // B
        explanation = "'Figurine: Araromire' (2009) von Kunle Afolayan gilt als Wendepunkt zum 'New Nollywood'. Der Film hatte ein Budget von 65 Millionen Naira, lief im regulaeren Kino (nicht nur auf DVD) und zeigte, dass nigerianische Filme Kinoqualitaet erreichen koennen.",
        difficulty = 4,
        funFact = "Kunle Afolayan ist der Sohn von Ade Love, einem der beruehemtesten Yoruba-Reisenden-Theater-Sternchen, der die erste Generation nigerianischer Filmemacher massgeblich pragte."
    ),

    // Question 44 - Kiarostami Close-Up
    Question(
        categoryId = 4,
        questionText = "Kiarostamis 'Nahaufnahme' (Close-Up, 1990) ist ein Hybridfilm zwischen Dokumentar- und Spielfilm. Was ist die wahre Geschichte, die er verfilmt?",
        answerA = "Einen Filmkritiker, der sich als Regisseur ausgab, um bei einem Filmdreh mitzumachen",
        answerB = "Einen Arbeitslosen, der sich als Regisseur Mohsen Makhmalbaf ausgab, um Zugang zur Mittelklasse-Familie Ahankhah zu erhalten",
        answerC = "Einen Immigranten, der sich als Abbas Kiarostami ausgab, um Visa zu erhalten",
        answerD = "Einen Darsteller, der mehrere Identitaeten gleichzeitig lebte",
        correctAnswer = 1, // B
        explanation = "Hossain Sabzian gab sich gegenueber der Familie Ahankhah als Regisseur Makhmalbaf aus und verbrachte Wochen mit ihnen. Er wurde wegen Betrugs verurteilt. Kiarostami durfte den Prozess filmen und bat die echten Beteiligten, sich selbst in Rekonstruktionen zu spielen — was die Grenzen zwischen Realitaet und Fiktion aufloest.",
        difficulty = 4,
        funFact = "Am Ende des Films trifft der echte Makhmalbaf auf den echten Sabzian — eine Begegnung, die Kiarostami spontan filmte und die zu einer der ruehrendsten Szenen des Kinos gehoert."
    ),

    // Question 45 - Warhol Chelsea Girls
    Question(
        categoryId = 4,
        questionText = "Andy Warhols 'Chelsea Girls' (1966) war sein kommerziell erfolgreichster Film. Welche technische Besonderheit im Screening-Konzept macht ihn einzigartig?",
        answerA = "Drei Projektoren gleichzeitig auf einer runden Leinwand",
        answerB = "Zwei Projektoren gleichzeitig nebeneinander auf geteilter Leinwand mit unterschiedlichen Geschwindigkeiten",
        answerC = "Projektion auf beide Seiten einer transparenten Leinwand gleichzeitig",
        answerD = "Ein Projektor mit zufaelliger Reihenfolge durch Zufallsgenerator",
        correctAnswer = 1, // B
        explanation = "'Chelsea Girls' wird mit zwei Projektoren gleichzeitig vorgefiehrt, die unterschiedliche Rollen zeigen, manchmal mit und manchmal ohne Ton. Der Zufall der jeweiligen Kombination bedeutet, dass jede Vorstellung einzigartig ist. Dies machte den Film zu einer installativen Performance.",
        difficulty = 4,
        funFact = "'Chelsea Girls' lief im Film-Makers' Cinematheque und war der erste Warhol-Film, der regulaer in kommerziellen Kinos gezeigt wurde."
    ),

    // Question 46 - Mrinal Sen Calcutta Trilogie
    Question(
        categoryId = 4,
        questionText = "Mrinal Sen, neben Ray und Ghatak der dritte Grossmeister des bengalischen Films, drehte eine Kalkutta-Trilogie. Wie heisst der erste Teil (1971)?",
        answerA = "Bhuvan Shome",
        answerB = "Interview",
        answerC = "Calcutta 71",
        answerD = "Padatik",
        correctAnswer = 1, // B
        explanation = "'Interview' (1971) ist der erste Teil von Sens Kalkutta-Trilogie, gefolgt von 'Calcutta 71' (1972) und 'Padatik' (1973). Sen war politisch radikaler als Ray und verwendete Verfremdungseffekte nach Brecht. 'Bhuvan Shome' (1969) war sein frueherer Durchbruch.",
        difficulty = 4,
        funFact = "Mrinal Sen war bekennender Kommunist und Mitglied der Communist Party of India. Seine Filme wurden vom CPI gefoerdert, was in Indien und international fuer Aufmerksamkeit sorgte."
    ),

    // Question 47 - Structural Film P. Adams Sitney
    Question(
        categoryId = 4,
        questionText = "Der Filmkritiker P. Adams Sitney praegte 1969 den Begriff 'Structural Film'. Welche vier formalen Eigenschaften definieren laut Sitney diesen Filmtyp?",
        answerA = "Montage, Licht, Ton, Kamera",
        answerB = "Feste Kameraposition, Flicker-Effekt, Loop-Struktur, Rephotography vom Bildschirm",
        answerC = "Lange Takes, geringe Tiefenschaerfe, nicht-lineare Zeit, politischer Kommentar",
        answerD = "Mehrfachbelichtung, Slow Motion, Jump Cuts, fehlende Musik",
        correctAnswer = 1, // B
        explanation = "Sitney definierte Structural Film durch: feste Kameraposition (fixed frame), Flicker-Effekt, Loop-Struktur und Rephotography (Abfilmen von Projektionen oder Monitoren). Zu den Hauptvertretern zaehlen Michael Snow, Hollis Frampton, George Landow und Paul Sharits.",
        difficulty = 4,
        funFact = "Paul Sharits' 'T,O,U,C,H,I,N,G' (1968) ist eines der extremsten Flicker-Filme und loest bei laengerer Betrachtung epileptische aehnliche visuelle Erlebnisse aus."
    ),

    // Question 48 - Senegal Djibril Diop Mambety
    Question(
        categoryId = 4,
        questionText = "Djibril Diop Mambety gilt neben Sembene als zweiter Grossmeister des senegalesischen Kinos. Sein beruhmtester Film aus dem Jahr 1973 heisst wie?",
        answerA = "Xala",
        answerB = "Touki Bouki",
        answerC = "Ceddo",
        answerD = "Camp de Thiaroye",
        correctAnswer = 1, // B
        explanation = "'Touki Bouki' (1973, woertlich: 'Die Reise der Hyaene') zeigt zwei junge Senegalesen, die nach Paris fliehen wollen. Der Film ist formal radikal, non-linear und beeinflusste Filmemacher weltweit. Jourja Thiong und Maremma Le sind die Hauptdarsteller. Er wurde 2008 in die Sight-and-Sound-Liste aufgenommen.",
        difficulty = 4,
        funFact = "'Touki Bouki' fehlt in vielen Filmarchiven, da kein vollstaendiges 35mm-Original bekannt war. Das World Cinema Project von Martin Scorsese restaurierte den Film 2008 aufwendig."
    ),

    // Question 49 - Philippine Ishmael Bernal
    Question(
        categoryId = 4,
        questionText = "Ishmael Bernal gilt als bedeutendster philippinischer Filmemacher der 'Second Golden Age' der 1970er-80er Jahre. Welchen Film von ihm nennt die Kritik haeufig als sein Meisterwerk?",
        answerA = "Nunal sa Tubig (1976)",
        answerB = "Himala (1982)",
        answerC = "Tubog sa Ginto (1970)",
        answerD = "Manila by Night (1980)",
        correctAnswer = 1, // B
        explanation = "'Himala' (Mirakel, 1982) mit Nora Aunor gilt als Bernals Meisterwerk und einer der bedeutendsten philippinischen Filme. Eine Frau auf einer dueren Insel behauptet, Marienvisionen zu haben, und zieht Pilgerscharen an — eine beissende Kritik an Aberglauben, Medien und politischer Manipulation.",
        difficulty = 4,
        funFact = "'Himala' entstand kurz vor dem Ende der Marcos-Diktatur und wurde als subversiver Kommentar auf die Imelda-Marcos-Inszenierung als heilige Figur gelesen."
    ),

    // Question 50 - Cinema Novo Phase 3
    Question(
        categoryId = 4,
        questionText = "Das brasilianische Cinema Novo hatte drei Phasen. Was kennzeichnet die dritte Phase (ab 1968) unter der Militaerdiktatur?",
        answerA = "Rueckkehr zum kommerziellen Unterhaltungskino durch staatlichen Druck",
        answerB = "Tropicalismo-Aesthetik: Vermischung von Surrealismus, Pop, Kannibalismus-Metapher und allegori-scher Verschluesselung gegen die Zensur",
        answerC = "Emigration aller wichtigen Filmemacher nach Europa",
        answerD = "Vollstaendiger Uebergang zu Dokumentarfilmen",
        correctAnswer = 1, // B
        explanation = "Unter der Militaerdiktatur (nach dem AI-5 von 1968) war direkte politische Kritik unmoeglich. Das Cinema Novo entwickelte eine Tropicalismo-Aesthetik: Glauber Rocha, Joaquim Pedro de Andrade und andere nutzen Allegorie, Surrealismus und die 'Metapher des Kannibalismus' (Brasilien frisst westliche Kulturimporte und wandelt sie um), um Zensur zu umgehen.",
        difficulty = 4,
        funFact = "Joaquim Pedro de Andrades 'Macunaima' (1969) ist das Paradebeispiel der dritten Phase: Eine indigene Figur aus der Literatur wird zur kulturellen Allegorie fuer Brasiliens hybride Identitaet."
    )

)
