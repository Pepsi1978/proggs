package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsMedium5(): List<Question> = listOf(

    // Question 1 — Sprachen: Meistgesprochene Muttersprache
    Question(
        categoryId = 11,
        questionText = "Welche Sprache hat weltweit die meisten Muttersprachler?",
        answerA = "Englisch",
        answerB = "Spanisch",
        answerC = "Mandarin-Chinesisch",
        answerD = "Hindi",
        correctAnswer = 2,
        explanation = "Mandarin-Chinesisch hat mit rund 920 Millionen Muttersprachlern weltweit die meisten Erstsprachler. Englisch hingegen hat zwar mehr Gesamtsprecher (inkl. Zweitsprache), aber weniger Muttersprachler als Mandarin.",
        difficulty = 2,
        funFact = "Einer von sechs Menschen auf der Erde kann Mandarin sprechen — obwohl es als eine der schwierigsten Sprachen gilt, die man als Erwachsener erlernen kann."
    ),

    // Question 2 — Schriftsysteme: Aelteste Schrift
    Question(
        categoryId = 11,
        questionText = "Welche Schrift gilt als eine der aeltesten der Welt und entstand um 3300 v. Chr.?",
        answerA = "Hieroglyphen",
        answerB = "Keilschrift",
        answerC = "Phoeziisches Alphabet",
        answerD = "Chinesische Schrift",
        correctAnswer = 1,
        explanation = "Die sumerische Keilschrift entstand um ca. 3300 v. Chr. in Mesopotamien (heutiger Irak) und gilt neben den aegyptischen Hieroglyphen als eine der aeltesten bekannten Schriften der Menschheit.",
        difficulty = 2,
        funFact = "Die Keilschrift begann als Bilderschrift mit rund 900 Piktogrammen, die in weichen Ton geritzt wurden — die Tontafeln wurden dann in der Sonne getrocknet oder im Feuer gehaertet."
    ),

    // Question 3 — Sprachfamilien: Indoeuropaeisch
    Question(
        categoryId = 11,
        questionText = "Zu welcher Sprachfamilie gehoeren Deutsch, Englisch, Hindi und Russisch gemeinsam?",
        answerA = "Afroasiatische Sprachfamilie",
        answerB = "Sino-tibetische Sprachfamilie",
        answerC = "Indoeuropaeische Sprachfamilie",
        answerD = "Austronesische Sprachfamilie",
        correctAnswer = 2,
        explanation = "Deutsch, Englisch, Hindi und Russisch gehoeren alle zur indoeuropaeischen Sprachfamilie, der sprecherreichsten der Welt mit rund 3 Milliarden Muttersprachlern. Alle gehen auf eine gemeinsame Ursprungssprache, das Proto-Indoeuropaeische, zurueck.",
        difficulty = 2,
        funFact = null
    ),

    // Question 4 — Fremdwoerter: Pyjama
    Question(
        categoryId = 11,
        questionText = "Aus welcher Sprache stammt das deutsche Wort \"Pyjama\"?",
        answerA = "Arabisch",
        answerB = "Urdu/Hindi",
        answerC = "Japanisch",
        answerD = "Persisch",
        correctAnswer = 1,
        explanation = "Das Wort \"Pyjama\" stammt aus dem Urdu (pa-jama) und bedeutet urspruenglich \"Beinbekleidung\". Es kam ueber den britischen Kolonialismus nach Europa und ist heute in vielen Sprachen gebräuchlich.",
        difficulty = 2,
        funFact = "Im britischen Englisch schreibt man \"pyjama\", in amerikanischem Englisch dagegen \"pajama\" — beides meint dasselbe Schlafobergewand."
    ),

    // Question 5 — Kommunikation: Nonverbal
    Question(
        categoryId = 11,
        questionText = "Wie nennt man Kommunikation, die ohne gesprochene Worte stattfindet (z.B. durch Mimik und Gestik)?",
        answerA = "Paraverbale Kommunikation",
        answerB = "Nonverbale Kommunikation",
        answerC = "Metakommunikation",
        answerD = "Digitale Kommunikation",
        correctAnswer = 1,
        explanation = "Nonverbale Kommunikation bezeichnet alle Formen der Verstaendigung, die nicht auf Sprache basieren — dazu gehoeren Mimik, Gestik, Koerpersprache, Augenkontakt und Stimmklang.",
        difficulty = 2,
        funFact = "Charles Darwin untersuchte als Erster systematisch, welche Basisemotionen (wie Freude, Trauer, Wut) in allen Kulturen der Welt durch dieselben Gesichtsausdruecke ausgedrueckt werden."
    ),

    // Question 6 — Alphabet: Phoeizier
    Question(
        categoryId = 11,
        questionText = "Welches Volk entwickelte das erste Alphabet aus reinen Lautzeichen (ohne Bilder), das die Grundlage fuer unser heutiges Alphabet bildet?",
        answerA = "Die Aegypter",
        answerB = "Die Sumerer",
        answerC = "Die Griechen",
        answerD = "Die Phoeizier",
        correctAnswer = 3,
        explanation = "Die Phoeizier, ein Seefahrervolk aus dem Gebiet des heutigen Libanons, entwickelten um 1050 v. Chr. das erste rein konsonantische Lautalphabet. Die Griechen uebernahmen es spaeter und fuegte Vokale hinzu.",
        difficulty = 2,
        funFact = "Das phoeizische Alphabet hatte nur 22 Buchstaben — alle Konsonanten, keine Vokale. Die Griechen erkannten, dass man fuer das Griechische auch Vokalzeichen brauchte, und ergaenzten es entsprechend."
    ),

    // Question 7 — Sprachen: Anzahl Sprachen weltweit
    Question(
        categoryId = 11,
        questionText = "Wie viele Sprachen gibt es weltweit ungefaehr?",
        answerA = "Etwa 700",
        answerB = "Etwa 2.500",
        answerC = "Etwa 7.000",
        answerD = "Etwa 15.000",
        correctAnswer = 2,
        explanation = "Weltweit gibt es ungefaehr 7.000 Sprachen. Die groesste Sprachenvielfalt findet sich in Papua-Neuguinea, wo auf engstem Raum ueber 800 verschiedene Sprachen gesprochen werden.",
        difficulty = 2,
        funFact = "Von den rund 7.000 Sprachen der Welt sind etwa die Haelfte vom Aussterben bedroht — sie werden nur noch von wenigen Dutzend oder Hundert Menschen gesprochen."
    ),

    // Question 8 — Fremdwoerter: Tomate
    Question(
        categoryId = 11,
        questionText = "Aus welcher Sprache stammt das Wort \"Tomate\"?",
        answerA = "Spanisch",
        answerB = "Nahuatl (Aztekisch)",
        answerC = "Italienisch",
        answerD = "Arabisch",
        correctAnswer = 1,
        explanation = "Das Wort \"Tomate\" leitet sich vom aztekischen Nahuatl-Wort \"tomatl\" ab. Die Tomate stammt urspruenglich aus Mittelamerika und kam nach der Entdeckung Amerikas nach Europa.",
        difficulty = 2,
        funFact = "In vielen europaeischen Sprachen heisst die Tomate \"Pomodoro\" (Italienisch) oder \"pomme d'amour\" — beide bedeuten in etwa \"Liebesapfel\", da man ihr frueher aphrodisische Wirkung nachsagte."
    ),

    // Question 9 — Linguistik: Morphem
    Question(
        categoryId = 11,
        questionText = "Was ist das kleinste bedeutungstragende Einheit einer Sprache in der Linguistik?",
        answerA = "Phonem",
        answerB = "Silbe",
        answerC = "Morphem",
        answerD = "Lexem",
        correctAnswer = 2,
        explanation = "Das Morphem ist die kleinste bedeutungstragende Einheit einer Sprache. Das Wort \"unlesbar\" besteht z.B. aus drei Morphemen: un- (Negation), les- (Wortstamm von lesen) und -bar (Faehigkeit).",
        difficulty = 2,
        funFact = null
    ),

    // Question 10 — Kommunikation: Emoji-Erfindung
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr wurde der Smiley (als Grundlage digitaler Emojis) vom amerikanischen Werbegrafiker Harvey Ball erfunden?",
        answerA = "1953",
        answerB = "1963",
        answerC = "1973",
        answerD = "1983",
        correctAnswer = 1,
        explanation = "Harvey Ball schuf 1963 den ikonischen gelben Smiley fuer eine Versicherungskampagne. Er erhielt dafuer 45 Dollar und liess das Design nie schuetzen — es wurde so zur weltweiten Ikone.",
        difficulty = 2,
        funFact = "Harvey Ball verdiente an seinem beruehm­testen Werk nur 45 Dollar. Der Smiley wurde spaeter von anderen kommerziell verwertet und ist heute eines der bekanntesten Symbole der Welt."
    ),

    // Question 11 — Sprachen: Meistgesprochene Gesamtsprache
    Question(
        categoryId = 11,
        questionText = "Welche Sprache hat weltweit die meisten Gesamt­sprecher (Muttersprachler + Zweitsprachler zusammen)?",
        answerA = "Mandarin-Chinesisch",
        answerB = "Spanisch",
        answerC = "Englisch",
        answerD = "Arabisch",
        correctAnswer = 2,
        explanation = "Englisch hat mit rund 1,5 Milliarden Gesamtsprechern (Mutter- und Zweitsprachler zusammen) weltweit die meisten Sprecher. Es gilt als die wichtigste internationale Verkehrssprache und ist Pflichtsprache fuer Piloten weltweit.",
        difficulty = 2,
        funFact = "Englisch wird auch als \"Sprache der Luefte\" bezeichnet: Alle Piloten und Fluglotsen der Welt muessen sich im internationalen Luftverkehr auf Englisch verstaendigen."
    ),

    // Question 12 — Schriftsysteme: Lateinische Schrift
    Question(
        categoryId = 11,
        questionText = "Durch welchen Hauptfaktor verbreitete sich die lateinische Schrift am staerksten weltweit?",
        answerA = "Die Erfindung des Internets",
        answerB = "Die olympischen Spiele der Antike",
        answerC = "Das Christentum, der Buchdruck und die Kolonialisierung",
        answerD = "Den Seidenstrassen-Handel",
        correctAnswer = 2,
        explanation = "Das Christentum (Bibel auf Latein), der Buchdruck (Gutenberg, 15. Jh.) und spaeter die europaeische Kolonialisierung trugen massgeblich dazu bei, dass die lateinische Schrift heute von den meisten Sprachen weltweit verwendet wird.",
        difficulty = 2,
        funFact = null
    ),

    // Question 13 — Sprachfamilien: Sino-tibetisch
    Question(
        categoryId = 11,
        questionText = "Welche Sprachen gehoeren zur sino-tibetischen Sprachfamilie?",
        answerA = "Japanisch und Koreanisch",
        answerB = "Chinesisch und Tibetisch",
        answerC = "Arabisch und Hebraeisch",
        answerD = "Swahili und Amharisch",
        correctAnswer = 1,
        explanation = "Die sino-tibetische Sprachfamilie umfasst Chinesisch (\"sino\") und Tibetisch sowie Birmanisch. Mit rund 1,3 Milliarden Sprechern ist sie die zweitgroesste Sprachfamilie der Erde.",
        difficulty = 2,
        funFact = "Japanisch und Koreanisch sind NICHT sino-tibetisch — sie gehoeren zu eigenen Sprachfamilien (Japanisch ist eine Sprachinsulate, Koreanisch ebenfalls isoliert oder alataisch, je nach Theorie)."
    ),

    // Question 14 — Fremdwoerter: Kaffee
    Question(
        categoryId = 11,
        questionText = "Aus welcher Sprache stammt das deutsche Wort \"Kaffee\"?",
        answerA = "Tuerkisch (ueber Arabisch)",
        answerB = "Persisch",
        answerC = "Portugiesisch",
        answerD = "Franzoesisch",
        correctAnswer = 0,
        explanation = "Das Wort \"Kaffee\" gelangte ueber das Tuerkische (kahve) und Arabische (qahwa) ins Deutsche. Der Kaffee stammt aus dem aethiopischen Hochland und verbreitete sich ueber die arabische Welt nach Europa.",
        difficulty = 2,
        funFact = "Der Name \"Kaffee\" soll vom aethiopischen Ort \"Kaffa\" stammen, wo wilde Kaffeepflanzen urspruenglich heimisch waren — ob das stimmt, ist unter Sprachwissenschaftlern aber umstritten."
    ),

    // Question 15 — Kommunikation: Schulz von Thun
    Question(
        categoryId = 11,
        questionText = "Wie viele \"Seiten\" hat eine Nachricht laut dem Kommunikationsmodell von Friedemann Schulz von Thun?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "5",
        correctAnswer = 2,
        explanation = "Laut Schulz von Thuns Kommunikationsquadrat hat jede Nachricht vier Seiten: den Sachinhalt, die Selbstoffenbarung, den Beziehungsaspekt und den Appell. Missverstaendnisse entstehen oft, weil Sender und Empfaenger auf unterschiedlichen Seiten kommunizieren.",
        difficulty = 2,
        funFact = null
    ),

    // Question 16 — Linguistik: Phonem
    Question(
        categoryId = 11,
        questionText = "Was bezeichnet das Wort \"Phonem\" in der Sprachwissenschaft?",
        answerA = "Die kleinste bedeutungsunterscheidende Lauteinheit einer Sprache",
        answerB = "Ein Fremdwort aus dem Griechischen",
        answerC = "Die Lehre von der Schrift",
        answerD = "Ein Zeichen in der Gebaerdensprache",
        correctAnswer = 0,
        explanation = "Ein Phonem ist die kleinste bedeutungsunterscheidende Einheit in der gesprochenen Sprache. So unterscheidet im Deutschen der einzige Lautunterschied zwischen \"Maus\" und \"Haus\" (/m/ vs. /h/) die Bedeutung der Woerter komplett.",
        difficulty = 2,
        funFact = null
    ),

    // Question 17 — Sprachen: Hebraeisch als wiederbelebte Sprache
    Question(
        categoryId = 11,
        questionText = "Welche Sprache ist die einzige, die nach Jahrhunderten des Nichtgebrauchs erfolgreich als Alltagssprache wiederbelebt wurde?",
        answerA = "Latein",
        answerB = "Altgriechisch",
        answerC = "Hebraeisch",
        answerD = "Sanskrit",
        correctAnswer = 2,
        explanation = "Hebraeisch ist die einzige Sprache der Geschichte, die nach langer Zeit als reine Schrift- und Liturgiesprache im 19./20. Jahrhundert erfolgreich als lebendige Umgangssprache wiederbelebt wurde — heute ist es Amtssprache Israels.",
        difficulty = 2,
        funFact = "Der Hauptverantwortliche fuer die Wiederbelebung des Hebraeischen war Eliezer Ben-Jehuda, der mit seiner Familie als erstes schwor, nur noch Hebraeisch zu sprechen — selbst zuhause mit seinen Kindern."
    ),

    // Question 18 — Schriftsysteme: Hieroglyphen
    Question(
        categoryId = 11,
        questionText = "Was bedeutet das griechische Wort \"Hieroglyphe\" wortwörtlich?",
        answerA = "Zeichen der Goetter",
        answerB = "Heilige Schrift",
        answerC = "Bild der Weisen",
        answerD = "Zeichen des Königs",
        correctAnswer = 1,
        explanation = "\"Hieroglyphe\" setzt sich aus den griechischen Worten hieros (heilig) und glyphein (einritzen/schreiben) zusammen und bedeutet wortwoertlich \"heilige Schrift\". Die Aegypter nannten ihre Schrift selbst \"Gottesworte\".",
        difficulty = 2,
        funFact = "Die aegyptischen Hieroglyphen wurden rund 3.500 Jahre lang verwendet — von ca. 3200 v. Chr. bis ins 4. Jahrhundert n. Chr. Dann geriet das Wissen um ihre Entzifferung verloren und wurde erst durch den Rosetta-Stein wiederentdeckt."
    ),

    // Question 19 — Sprachen: Franzoesisch
    Question(
        categoryId = 11,
        questionText = "Auf welchem Kontinent gibt es die meisten frankophonen (franzoesischsprachigen) Menschen?",
        answerA = "Europa",
        answerB = "Nordamerika",
        answerC = "Afrika",
        answerD = "Suedamerika",
        correctAnswer = 2,
        explanation = "Afrika hat heute die meisten Franzoesischsprecher weltweit — in sub-saharischen Laendern wie der Demokratischen Republik Kongo, Kamerun und der Elfenbeinkueste wird Franzoesisch als Amtssprache gesprochen. Die Gesamtzahl afrikanischer Frankophoner uebersteigt die aller anderen Kontinente.",
        difficulty = 2,
        funFact = null
    ),

    // Question 20 — Kommunikation: Erste Nachricht ARPAnet
    Question(
        categoryId = 11,
        questionText = "Was war die erste elektronisch uebertragene Nachricht im ARPAnet (Vorlaeufer des Internets) im Jahr 1969?",
        answerA = "Hello World",
        answerB = "LO",
        answerC = "Hi",
        answerD = "Test 123",
        correctAnswer = 1,
        explanation = "Die erste uebertragene Nachricht im ARPAnet 1969 sollte \"LOGIN\" lauten, doch nach den ersten zwei Buchstaben \"LO\" brach das System zusammen. So wurde unbeabsichtigt \"LO\" zur ersten elektronischen Nachricht der Geschichte.",
        difficulty = 2,
        funFact = "Obwohl \"LO\" ein Absturz war, klingt es rueckblickend fast poetisch wie ein Gruss: \"Lo and behold\" (\"Sieh mal an\") — so beschrieb es der beteiligte Wissenschaftler Leonard Kleinrock spaeter."
    ),

    // Question 21 — Linguistik: Graphem
    Question(
        categoryId = 11,
        questionText = "Wie nennt man die kleinste unterscheidende Einheit eines Schriftsystems (z.B. ein Buchstabe)?",
        answerA = "Morphem",
        answerB = "Phonem",
        answerC = "Graphem",
        answerD = "Lexem",
        correctAnswer = 2,
        explanation = "Das Graphem ist die kleinste bedeutungsunterscheidende Einheit eines Schriftsystems. Im deutschen Alphabet ist jeder Buchstabe (a, b, c ...) ein Graphem — der Unterschied zwischen \"Hase\" und \"Base\" liegt am Graphem H vs. B.",
        difficulty = 2,
        funFact = null
    ),

    // Question 22 — Sprachen: Spanisch Muttersprachler
    Question(
        categoryId = 11,
        questionText = "Auf welchem Platz liegt Spanisch bei den Sprachen mit den meisten Muttersprachlern weltweit?",
        answerA = "Platz 1",
        answerB = "Platz 2",
        answerC = "Platz 4",
        answerD = "Platz 6",
        correctAnswer = 1,
        explanation = "Spanisch liegt auf Platz 2 der Sprachen mit den meisten Muttersprachlern (nach Mandarin), mit rund 485-500 Millionen Erstsprachlern. Es ist in 21 Laendern Amtssprache.",
        difficulty = 2,
        funFact = "Spanisch ist nach Englisch die Sprache mit dem groessten Einfluss auf das Weltkulturerbe der UNESCO — und in den USA sprechen schon heute mehr Menschen Spanisch als in Spanien selbst."
    ),

    // Question 23 — Schriftsysteme: Arabische Schrift
    Question(
        categoryId = 11,
        questionText = "In welche Richtung wird die arabische Schrift geschrieben?",
        answerA = "Von links nach rechts",
        answerB = "Von oben nach unten",
        answerC = "Von rechts nach links",
        answerD = "Von unten nach oben",
        correctAnswer = 2,
        explanation = "Die arabische Schrift wird von rechts nach links geschrieben. Das gilt auch fuer Hebraeisch und Persisch (Farsi). Buecher auf Arabisch beginnen damit von unserer Sicht aus am \"hinteren\" Ende.",
        difficulty = 2,
        funFact = "Arabisch ist die viertgroesste Sprache der Welt und mit der arabischen Schrift werden mehrere Sprachen geschrieben: Arabisch, Persisch, Urdu und Paschtu nutzen alle das arabische Schriftsystem."
    ),

    // Question 24 — Fremdwoerter: Balkon
    Question(
        categoryId = 11,
        questionText = "Aus welcher Sprache stammt das deutsche Wort \"Balkon\"?",
        answerA = "Tuerkisch",
        answerB = "Italienisch (ueber Franzoesisch)",
        answerC = "Spanisch",
        answerD = "Arabisch",
        correctAnswer = 1,
        explanation = "Das Wort \"Balkon\" stammt aus dem Italienischen \"balcone\" und kam ueber das Franzoesische ins Deutsche. \"Balcone\" leitet sich vom germanischen Wort \"balko\" (Balken) ab — ein Balkon ist also urspruenglich ein hervorspringender Balken.",
        difficulty = 2,
        funFact = null
    ),

    // Question 25 — Kommunikation: Braille-Schrift
    Question(
        categoryId = 11,
        questionText = "Wer erfand die Blindenschrift (Brailleschrift)?",
        answerA = "Charles Braille",
        answerB = "Louis Braille",
        answerC = "Pierre Braille",
        answerD = "Henri Braille",
        correctAnswer = 1,
        explanation = "Louis Braille (1809-1852) erfand die nach ihm benannte Brailleschrift im Alter von nur 15 Jahren. Er erblindete selbst als Kind durch einen Unfall und entwickelte das System aus sechs tastbaren Punkten.",
        difficulty = 2,
        funFact = "Louis Braille erblindete mit drei Jahren durch einen Unfall in der Werkstatt seines Vaters. Die Brailleschrift basierte auf einem militaerischen Nachtschriftsystem des franzoesischen Offiziers Charles Barbier."
    ),

    // Question 26 — Linguistik: Syntax
    Question(
        categoryId = 11,
        questionText = "Womit beschaeftigt sich die linguistische Disziplin \"Syntax\"?",
        answerA = "Mit der Bedeutung von Woertern",
        answerB = "Mit dem Klang von Sprache",
        answerC = "Mit der Entstehung von Woertern",
        answerD = "Mit dem Aufbau und den Regeln von Saetzen",
        correctAnswer = 3,
        explanation = "Die Syntax (griech.: syntaxis = Anordnung) beschaeftigt sich mit den Regeln, nach denen Woerter zu Saetzen zusammengesetzt werden. Sie legt fest, welche Satzstrukturen in einer Sprache grammatisch korrekt sind.",
        difficulty = 2,
        funFact = null
    ),

    // Question 27 — Sprachen: Hindi
    Question(
        categoryId = 11,
        questionText = "In welchem Schriftsystem wird Hindi geschrieben?",
        answerA = "Arabische Schrift",
        answerB = "Devanagari",
        answerC = "Lateinische Schrift",
        answerD = "Bengalische Schrift",
        correctAnswer = 1,
        explanation = "Hindi wird in der Devanagari-Schrift geschrieben, die von links nach rechts verlaueft. Devanagari wird auch fuer Sanskrit, Marathi und Nepali verwendet und hat ca. 47 Grundzeichen.",
        difficulty = 2,
        funFact = "Urdu und Hindi sind linguistisch fast identisch — sie unterscheiden sich hauptsaechlich durch die Schrift: Hindi benutzt Devanagari, Urdu die arabisch-persische Nastaliq-Schrift."
    ),

    // Question 28 — Schriftsysteme: Logographie
    Question(
        categoryId = 11,
        questionText = "Wie nennt man ein Schriftsystem, bei dem jedes Zeichen ein ganzes Wort oder Konzept repraesentiert (wie chinesische Schriftzeichen)?",
        answerA = "Alphabetisches System",
        answerB = "Syllabisches System",
        answerC = "Logographisches System",
        answerD = "Abjad",
        correctAnswer = 2,
        explanation = "In einem logographischen Schriftsystem steht jedes Zeichen fuer ein ganzes Wort oder einen Begriff. Die chinesische Schrift ist das bekannteste Beispiel — ein gebildeter Zeitungsleser muss rund 3.000-4.000 Schriftzeichen kennen.",
        difficulty = 2,
        funFact = "Das japanische Schriftsystem kombiniert gleich drei Systeme: Kanji (logographisch aus dem Chinesischen), Hiragana und Katakana (beide silbenbasiert) — oft sogar innerhalb eines einzigen Satzes."
    ),

    // Question 29 — Kommunikation: Morse-Code
    Question(
        categoryId = 11,
        questionText = "Wer erfand den nach ihm benannten Morse-Code?",
        answerA = "Nikola Tesla",
        answerB = "Samuel Morse",
        answerC = "Alexander Bell",
        answerD = "Thomas Edison",
        correctAnswer = 1,
        explanation = "Samuel Morse (1791-1872) entwickelte zusammen mit Alfred Vail den Morse-Code, ein System aus Punkten und Strichen zur Uebertragung von Nachrichten ueber den Telegraphen. 1844 wurde die erste offizielle Telegraphen-Nachricht gesendet.",
        difficulty = 2,
        funFact = "Die bekannteste Morse-Sequenz ist SOS (... --- ...) — sie wurde nicht wegen der Buchstaben gewaehlt, sondern weil die Abfolge von drei Kurzen, drei Langen, drei Kurzen besonders leicht zu erkennen ist."
    ),

    // Question 30 — Fremdwoerter: Sofa
    Question(
        categoryId = 11,
        questionText = "Aus welcher Sprache stammt das Wort \"Sofa\"?",
        answerA = "Franzoesisch",
        answerB = "Arabisch",
        answerC = "Tuerkisch",
        answerD = "Persisch",
        correctAnswer = 1,
        explanation = "Das Wort \"Sofa\" kommt aus dem Arabischen \"suffa\", was eine Art erhoehtes Sitzpolster bezeichnete. Es verbreitete sich ueber das Tuerkische und Franzoesische im 18. Jahrhundert nach Deutschland.",
        difficulty = 2,
        funFact = null
    ),

    // Question 31 — Linguistik: Etymologie
    Question(
        categoryId = 11,
        questionText = "Was untersucht die Etymologie?",
        answerA = "Die Aussprache von Woertern",
        answerB = "Die Herkunft und Entwicklung von Woertern",
        answerC = "Die Grammatikregeln einer Sprache",
        answerD = "Den Satzbau",
        correctAnswer = 1,
        explanation = "Die Etymologie (griech.: etymos = wahr, logos = Wort) beschaeftigt sich mit der Herkunft und geschichtlichen Entwicklung von Woertern. Sie untersucht, wie Woerter entstanden, sich veraendert haben und aus welcher Sprache sie stammen.",
        difficulty = 2,
        funFact = "Das Wort \"Etymologie\" ist selbst ein gutes Beispiel: Es stammt aus dem Griechischen und bedeutet wortwoertlich \"die Lehre vom wahren Sinn der Woerter\"."
    ),

    // Question 32 — Sprachen: Papua-Neuguinea
    Question(
        categoryId = 11,
        questionText = "Welches Land hat mit ueber 800 Sprachen die groesste Sprachenvielfalt der Welt?",
        answerA = "Indien",
        answerB = "Nigeria",
        answerC = "Papua-Neuguinea",
        answerD = "Brasilien",
        correctAnswer = 2,
        explanation = "Papua-Neuguinea hat auf vergleichsweise kleinem Gebiet mehr als 800 verschiedene Sprachen — mehr als jedes andere Land der Welt. Das macht rund 12% aller Sprachen weltweit auf einem Gebiet aus.",
        difficulty = 2,
        funFact = "Die extreme Sprachenvielfalt in Papua-Neuguinea erklaert sich durch das zerklueeftete Gebirge: Die Bevoelkerung lebte jahrhundertelang in isolierten Taelern und entwickelte so unabhaengig voneinander voellig verschiedene Sprachen."
    ),

    // Question 33 — Schriftsysteme: Rosetta-Stein
    Question(
        categoryId = 11,
        questionText = "Was ermoeglichte es Wissenschaftlern, die aegyptischen Hieroglyphen endlich zu entziffern?",
        answerA = "Das Gilgamesch-Epos",
        answerB = "Der Rosetta-Stein",
        answerC = "Das Amarna-Archiv",
        answerD = "Der Papyrus Ebers",
        correctAnswer = 1,
        explanation = "Der Rosetta-Stein (1799 von Napoleon-Truppen in Aegypten entdeckt) traegt denselben Text in drei Schriften: Hieroglyphen, demotischer Schrift und Griechisch. Da Griechisch lesbar war, konnte Jean-Francois Champollion 1822 damit die Hieroglyphen entschluesseln.",
        difficulty = 2,
        funFact = "Der Rosetta-Stein liegt heute im Britischen Museum in London — was Aegypten seit Jahrzehnten bestreitet und seine Rueckgabe fordert."
    ),

    // Question 34 — Kommunikation: Gutenberg
    Question(
        categoryId = 11,
        questionText = "Welche Erfindung von Johannes Gutenberg revolutionierte um 1450 die Kommunikation und Wissensverbreitung in Europa?",
        answerA = "Die Dampfmaschine",
        answerB = "Der Buchdruck mit beweglichen Lettern",
        answerC = "Das Teleskop",
        answerD = "Die Papierherstellung",
        correctAnswer = 1,
        explanation = "Johannes Gutenberg erfand um 1450 den Buchdruck mit beweglichen Metalllettern. Dadurch konnten Buecher massenhaft und guenstig hergestellt werden, was die Reformation, die Renaissance und die Verbreitung von Wissen massgeblich beschleunigte.",
        difficulty = 2,
        funFact = "Vor Gutenbergs Buchdruck mussten Moenche Buecher von Hand abschreiben — ein einziges Buch konnte Jahre dauern. Nach Gutenberg konnten hunderte identische Kopien in wenigen Tagen gedruckt werden."
    ),

    // Question 35 — Fremdwoerter: Kiosk
    Question(
        categoryId = 11,
        questionText = "Aus welcher Sprache stammt das Wort \"Kiosk\" (z.B. Zeitungskiosk)?",
        answerA = "Franzoesisch",
        answerB = "Arabisch",
        answerC = "Tuerkisch (ueber Persisch)",
        answerD = "Griechisch",
        correctAnswer = 2,
        explanation = "Das Wort \"Kiosk\" stammt aus dem Tuerkischen \"koeshk\", was urspruenglich einen offenen Pavillon oder Gartenhaus bezeichnete. Es gelangte ueber das Franzoesische im 19. Jahrhundert in die deutsche Sprache.",
        difficulty = 2,
        funFact = null
    ),

    // Question 36 — Linguistik: Diglossie
    Question(
        categoryId = 11,
        questionText = "Was beschreibt der Begriff \"Diglossie\" in der Sprachwissenschaft?",
        answerA = "Eine Sprache mit zwei verschiedenen Alphabeten",
        answerB = "Das gleichzeitige Vorhandensein von zwei Sprachvarianten (Hochsprache und Umgangssprache) in einer Gesellschaft",
        answerC = "Eine Person, die zwei Sprachen spricht",
        answerD = "Ein Woerterbuch in zwei Sprachen",
        correctAnswer = 1,
        explanation = "Diglossie bezeichnet eine soziolinguistische Situation, in der zwei Varianten einer Sprache in verschiedenen Situationen verwendet werden: eine formelle Hochsprache (z.B. Hochdeutsch) und eine informelle Variante (z.B. Dialekt oder Umgangssprache).",
        difficulty = 2,
        funFact = "Ein klassisches Beispiel fuer Diglossie ist die Schweiz: Im Alltag wird Schweizerdeutsch gesprochen, in formellen Kontexten wie Zeitung oder Schule aber Hochdeutsch geschrieben und gesprochen."
    ),

    // Question 37 — Sprachen: Latein
    Question(
        categoryId = 11,
        questionText = "Welche modernen Sprachen sind direkte Nachfolger (romanische Sprachen) des Lateinischen?",
        answerA = "Deutsch, Englisch, Niederlaendisch",
        answerB = "Franzoesisch, Spanisch, Italienisch, Portugiesisch, Rumaenisch",
        answerC = "Russisch, Polnisch, Tschechisch",
        answerD = "Arabisch, Hebraeisch, Maltesisch",
        correctAnswer = 1,
        explanation = "Franzoesisch, Spanisch, Italienisch, Portugiesisch und Rumaenisch sind romanische Sprachen, die sich direkt aus dem Vulgaerlatein (Alltagslatein) des Roemischen Reiches entwickelt haben. Auch Katalanisch, Okzitanisch und andere gehoeren dazu.",
        difficulty = 2,
        funFact = "Englisch ist KEINE romanische Sprache, obwohl etwa 60% des englischen Wortschatzes lateinischen oder franzoesischen Ursprungs sind — durch die normannische Eroberung 1066 floss viel Franzoesisch ins Englische ein."
    ),

    // Question 38 — Kommunikation: Telegraf
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr wurde die erste transatlantische Telegrafenverbindung zwischen Europa und Amerika hergestellt?",
        answerA = "1844",
        answerB = "1858",
        answerC = "1876",
        answerD = "1901",
        correctAnswer = 1,
        explanation = "1858 wurde das erste transatlantische Telegrafenkabel zwischen Irland und Neufundland (Kanada) verlegt. Die erste Nachricht darauf war eine Glueckwunschbotschaft zwischen Koenigin Victoria und US-Praesident Buchanan — das Kabel brach jedoch bald danach.",
        difficulty = 2,
        funFact = "Das erste transatlantische Kabel 1858 funktionierte nur wenige Wochen, bevor es versagte. Erst 1866 gelang eine dauerhafte transatlantische Telegrafenverbindung."
    ),

    // Question 39 — Fremdwoerter: Algebra
    Question(
        categoryId = 11,
        questionText = "Aus welcher Sprache stammt das mathematische Wort \"Algebra\"?",
        answerA = "Griechisch",
        answerB = "Arabisch",
        answerC = "Latein",
        answerD = "Persisch",
        correctAnswer = 1,
        explanation = "Das Wort \"Algebra\" stammt aus dem Arabischen \"al-jabr\" (= das Zusammenfuegen von Knochen, metaphorisch: Ergaenzen). Es wurde vom arabischen Mathematiker al-Chwarizmi im 9. Jahrhundert gepraegt, von dem auch das Wort \"Algorithmus\" stammt.",
        difficulty = 2,
        funFact = "Der arabische Mathematiker al-Chwarizmi schrieb ein Buch \"Kitab al-mukhtasar fi hisab al-jabr\" (Buch ueber das Rechnen durch Ergaenzen), dessen Titel uns das Wort \"Algebra\" bescherte — sein Name gab uns \"Algorithmus\"."
    ),

    // Question 40 — Linguistik: Kreolsprache
    Question(
        categoryId = 11,
        questionText = "Was ist eine Kreolsprache?",
        answerA = "Eine ausgestorbene Sprache",
        answerB = "Eine kuenstlich erfundene Sprache",
        answerC = "Eine Sprache, die aus dem Kontakt mehrerer Sprachen entstanden und zur Muttersprache einer Gemeinschaft geworden ist",
        answerD = "Ein regionaler Dialekt",
        correctAnswer = 2,
        explanation = "Eine Kreolsprache entsteht, wenn eine Pidginsprache (vereinfachte Kontaktsprache) zur Muttersprache einer Gemeinschaft wird und sich zu einer vollstaendigen Sprache entwickelt. Bekannte Beispiele: Haitianisches Kreolisch, Tok Pisin (Papua-Neuguinea).",
        difficulty = 2,
        funFact = "Kreolsprachen entstanden oft waehrend der Kolonialzeit, wenn Sklaven verschiedener Herkunft miteinander kommunizieren mussten. Sie sind linguistisch vollwertige Sprachen — nicht etwa vereinfachtes Gebrochendeutsch."
    ),

    // Question 41 — Sprachen: Arabisch Verbreitung
    Question(
        categoryId = 11,
        questionText = "In wie vielen Laendern ist Arabisch Amtssprache?",
        answerA = "Etwa 10",
        answerB = "Etwa 15",
        answerC = "Etwa 22",
        answerD = "Etwa 30",
        correctAnswer = 2,
        explanation = "Arabisch ist in rund 22 Laendern Amtssprache, hauptsaechlich in der arabischen Welt von Marokko bis zum Oman. Es ist eine der sechs offiziellen Arbeitssprachen der Vereinten Nationen.",
        difficulty = 2,
        funFact = "Arabisch gilt mit seiner Geschichte von ueber 1.500 Jahren als eine der aeltesten noch gesprochenen Sprachen der Welt — der Koran wurde auf Arabisch verfasst und hat die Sprache stark konserviert."
    ),

    // Question 42 — Schriftsysteme: Chinesische Zeichen
    Question(
        categoryId = 11,
        questionText = "Wie viele Schriftzeichen muss ein gebildeter Zeitungsleser in China ungefaehr kennen?",
        answerA = "Etwa 500",
        answerB = "Etwa 3.000 bis 4.000",
        answerC = "Etwa 10.000",
        answerD = "Etwa 50.000",
        correctAnswer = 1,
        explanation = "Fuer das Lesen einer chinesischen Zeitung oder eines normalen Textes benoetigt man rund 3.000 bis 4.000 Schriftzeichen. Insgesamt gibt es ueber 50.000 chinesische Schriftzeichen, von denen aber nur ein Bruchteil regelmaessig gebraucht wird.",
        difficulty = 2,
        funFact = "Das groeßte chinesische Schriftzeichen-Woerterbuch (Zhonghua Zidian) enthaelt ueber 54.000 Eintraege — aber selbst hochgebildete Chinesen kennen nur einen Bruchteil davon."
    ),

    // Question 43 — Kommunikation: Zeichensprache
    Question(
        categoryId = 11,
        questionText = "Ist die Gebaerdensprache eine einheitliche Weltsprache?",
        answerA = "Ja, es gibt eine universelle internationale Gebaerdensprache",
        answerB = "Nein, es gibt viele verschiedene nationale Gebaerdensprachen",
        answerC = "Nein, Gebaerden sind keine echten Sprachen",
        answerD = "Ja, aber nur in Europa",
        correctAnswer = 1,
        explanation = "Es gibt keine einheitliche Welt-Gebaerdensprache. Jedes Land hat oft eine oder mehrere eigene Gebaerdensprachen (z.B. Deutsche Gebaerdensprache DGS, American Sign Language ASL). Es gibt zwar eine \"Internationale Gebaerdensprache\" (IS), sie wird aber nur begrenzt eingesetzt.",
        difficulty = 2,
        funFact = "Linguistisch gesehen sind Gebaerdensprachen vollwertige natuerliche Sprachen mit eigener Grammatik, Syntax und Idiomen — sie sind NICHT einfach Gebaerden fuer Lautsprachwoerter."
    ),

    // Question 44 — Fremdwoerter: Algorithmus
    Question(
        categoryId = 11,
        questionText = "Von wem leitet sich der Begriff \"Algorithmus\" ab?",
        answerA = "Dem griechischen Mathematiker Euklid",
        answerB = "Dem arabischen Mathematiker al-Chwarizmi",
        answerC = "Dem deutschen Mathematiker Leibniz",
        answerD = "Dem roemischen Gelehrten Cicero",
        correctAnswer = 1,
        explanation = "\"Algorithmus\" ist eine latinisierte Form des Namens \"al-Chwarizmi\", eines arabischen Mathematikers aus Bagdad (ca. 780-850 n. Chr.). Sein Name, der sich von der Stadt Chwarizm ableitete, wurde im Mittelalter mit Rechenverfahren gleichgesetzt.",
        difficulty = 2,
        funFact = "Al-Chwarizmi gab uns gleich zwei wichtige Woerter: \"Algorithmus\" (von seinem Namen) UND \"Algebra\" (vom Titel seines beruehm­testen Buches). Kein anderer Mensch hat die Sprache der Mathematik so stark gepraegt."
    ),

    // Question 45 — Sprachen: Lingua Franca
    Question(
        categoryId = 11,
        questionText = "Was bedeutet der Begriff \"Lingua Franca\"?",
        answerA = "Eine sehr alte Sprache",
        answerB = "Eine Sprache, die von Franzoesen gesprochen wird",
        answerC = "Eine gemeinsame Verstaendigungssprache zwischen Menschen mit verschiedenen Muttersprachen",
        answerD = "Eine tote Sprache",
        correctAnswer = 2,
        explanation = "\"Lingua Franca\" bezeichnet eine Sprache, die als gemeinsames Kommunikationsmittel zwischen Menschen verschiedener Muttersprachen dient. Heute ist Englisch die wichtigste globale Lingua Franca. Der Begriff stammt aus dem Mittelmeerraum, wo Haendler ein vereinfachtes Gemisch aus Sprachen nutzten.",
        difficulty = 2,
        funFact = "Die urspruengliche \"Lingua Franca\" des Mittelalters war ein Gemisch aus Italienisch, Franzoesisch, Spanisch, Arabisch und Tuerkisch — damit verstaendigten sich Haendler im Mittelmeer, unabhaengig ihrer Herkunft."
    ),

    // Question 46 — Kommunikation: Rhetorik
    Question(
        categoryId = 11,
        questionText = "Wer gilt als Begruender der Rhetorik als Wissenschaft der Beredsamkeit im antiken Griechenland?",
        answerA = "Sokrates",
        answerB = "Platon",
        answerC = "Aristoteles",
        answerD = "Demosthenes",
        correctAnswer = 2,
        explanation = "Aristoteles (384-322 v. Chr.) gilt als Begruender der Rhetorik als wissenschaftliche Disziplin. In seinem Werk \"Rhetorik\" beschrieb er systematisch die Mittel der Ueberzeugung: Logos (Vernunft), Ethos (Glaubwuerdigkeit) und Pathos (Emotion).",
        difficulty = 2,
        funFact = "Aristoteles' Drei­teilung Logos-Ethos-Pathos ist heute noch in jedem Rhetorikseminar die Grundlage — 2.300 Jahre nach seiner Entstehung."
    ),

    // Question 47 — Schriftsysteme: Protosinaitisch
    Question(
        categoryId = 11,
        questionText = "Wo entstand um 2000 v. Chr. das erste alphabetische Schriftsystem (Protosinaitisch)?",
        answerA = "In Mesopotamien",
        answerB = "In Aegypten (von semitischen Arbeitern)",
        answerC = "In China",
        answerD = "In Griechenland",
        correctAnswer = 1,
        explanation = "Das protosinaitische Alphabet entstand um ca. 1900-1700 v. Chr. von semitischen Arbeitern in aegyptischen Tuerquisminen auf dem Sinai. Sie vereinfachten aegyptische Hieroglyphen und schufen so die Grundlage fuer alle heutigen Alphabete.",
        difficulty = 2,
        funFact = "Faszinierend: Fast alle Alphabete der Welt gehen letztlich auf diesen einen Ursprung zurueck — die semitischen Wanderarbeiter in aegyptischen Bergwerken, die sich ein einfacheres Schriftsystem erfanden."
    ),

    // Question 48 — Linguistik: Semantik
    Question(
        categoryId = 11,
        questionText = "Womit beschaeftigt sich die Semantik als Teilgebiet der Linguistik?",
        answerA = "Mit der Geschichte von Sprachen",
        answerB = "Mit Bedeutungen und Sinn von Woertern und Saetzen",
        answerC = "Mit der Grammatik",
        answerD = "Mit der Aussprache",
        correctAnswer = 1,
        explanation = "Die Semantik untersucht die Bedeutungen von sprachlichen Zeichen — also was Woerter, Saetze oder Texte bedeuten und wie Bedeutung entsteht. Sie fragt z.B.: Warum hat \"Bank\" zwei verschiedene Bedeutungen (Sitzmoebel / Geldinstitut)?",
        difficulty = 2,
        funFact = null
    ),

    // Question 49 — Sprachen: Esperanto
    Question(
        categoryId = 11,
        questionText = "Wer erfand die Plansprache Esperanto?",
        answerA = "Charles K. Ogden",
        answerB = "Ludwig Lazarus Zamenhof",
        answerC = "Johann Martin Schleyer",
        answerD = "Julius Nyerere",
        correctAnswer = 1,
        explanation = "Ludwig Lazarus Zamenhof (1859-1917), ein polnisch-juedischer Augenarzt, entwickelte Esperanto und veroeffentlichte es 1887. Sein Ziel war eine neutrale Weltsprache fuer Voelkerverstaendigung. Heute sprechen es schaetzungsweise 1-2 Millionen Menschen.",
        difficulty = 2,
        funFact = "Zamenhof wuchs in Bialystok auf, wo Russen, Polen, Deutsche und Juden kaempften — oft weil sie sich nicht verstanden. Das praegte seinen Traum von einer gemeinsamen Sprache, die keinem Volk gehoert."
    ),

    // Question 50 — Kommunikation: WhatsApp Gruendung
    Question(
        categoryId = 11,
        questionText = "Von wem wurde WhatsApp gegruendet?",
        answerA = "Mark Zuckerberg und Dustin Moskovitz",
        answerB = "Jan Koum und Brian Acton",
        answerC = "Jack Dorsey und Biz Stone",
        answerD = "Travis Kalanick und Garrett Camp",
        correctAnswer = 1,
        explanation = "WhatsApp wurde 2009 von Jan Koum und Brian Acton gegruendet, beide ehemalige Yahoo-Mitarbeiter. 2014 kaufte Facebook (Meta) WhatsApp fuer 19 Milliarden Dollar — bis dahin der groesste Technologieaufkauf der Geschichte.",
        difficulty = 2,
        funFact = "Jan Koum wuchs in der Ukraine in bitterer Armut auf und wanderte als Teenager in die USA aus. Bevor er WhatsApp gruendete, hatte er sich erfolglos bei Facebook beworben. Wenige Jahre spaeter verkaufte er sein Unternehmen an Facebook fuer 19 Milliarden Dollar."
    ),

)
