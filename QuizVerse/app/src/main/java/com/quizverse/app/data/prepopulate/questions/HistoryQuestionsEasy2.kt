package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsEasy2(): List<Question> = listOf(

    // --- Ancient China ---

    Question(
        categoryId = 3,
        questionText = "Welches Bauwerk ließ der erste chinesische Kaiser Qin Shihuangdi errichten?",
        answerA = "Den Kaiserkanal",
        answerB = "Die Große Mauer",
        answerC = "Den Verbotenen Palast",
        answerD = "Die Pagode von Xi'an",
        correctAnswer = 1,
        explanation = "Qin Shihuangdi ließ Teile der Großen Mauer verbinden und ausbauen, um das Reich gegen Nomaden aus dem Norden zu schützen.",
        difficulty = 1,
        funFact = "In seinem Mausoleum wurde 1974 die berühmte Terrakotta-Armee entdeckt – über 8.000 lebensgroße Soldaten aus Ton."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Erfindung stammt aus dem alten China?",
        answerA = "Der Buchdruck mit beweglichen Lettern",
        answerB = "Das Rad",
        answerC = "Das Papier",
        answerD = "Die Dampfmaschine",
        correctAnswer = 2,
        explanation = "Papier wurde in China um das Jahr 105 n. Chr. von Cai Lun erfunden und verbreitete sich von dort aus in die ganze Welt.",
        difficulty = 1,
        funFact = "Vor dem Papier schrieben die Chinesen auf Bambus- oder Seidenstreifen, die viel schwerer und teurer waren."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Konfuzius?",
        answerA = "Ein chinesischer Kaiser",
        answerB = "Ein chinesischer Feldherr",
        answerC = "Ein chinesischer Philosoph und Lehrer",
        answerD = "Ein chinesischer Händler auf der Seidenstraße",
        correctAnswer = 2,
        explanation = "Konfuzius (551–479 v. Chr.) war ein chinesischer Philosoph, dessen Lehren über Moral, Familie und Gesellschaft bis heute das chinesische Denken prägen.",
        difficulty = 1,
        funFact = "Der Name Konfuzius ist die lateinische Schreibweise seines chinesischen Namens Kǒng Fūzǐ, was „Meister Kong" bedeutet."
    ),

    // --- Ancient India ---

    Question(
        categoryId = 3,
        questionText = "Welche Schrift verwendeten die Bewohner der Indus-Tal-Zivilisation?",
        answerA = "Keilschrift",
        answerB = "Hieroglyphen",
        answerC = "Indus-Schrift",
        answerD = "Devanagari",
        correctAnswer = 2,
        explanation = "Die Indus-Tal-Zivilisation (ca. 2600–1900 v. Chr.) entwickelte eine eigene Schrift, die bis heute nicht vollständig entschlüsselt werden konnte.",
        difficulty = 1,
        funFact = "Städte wie Mohenjo-Daro hatten bereits ein ausgeklügeltes Abwassersystem – eine Meisterleistung der frühen Stadtplanung."
    ),

    Question(
        categoryId = 3,
        questionText = "Kaiser Ashoka war ein bedeutender Herrscher welchen Reiches?",
        answerA = "Des Gupta-Reiches",
        answerB = "Des Mogulreiches",
        answerC = "Des Maurya-Reiches",
        answerD = "Des Kushanenreiches",
        correctAnswer = 2,
        explanation = "Ashoka (268–232 v. Chr.) herrschte über das Maurya-Reich und wurde nach einer blutigen Eroberung zum Buddhismus bekehrt, den er aktiv verbreitete.",
        difficulty = 1,
        funFact = "Das heutige Staatswappen Indiens und die Flagge zeigen das Chakra (Rad) – ein Symbol aus Ashokas Herrschaft."
    ),

    // --- Mesopotamia ---

    Question(
        categoryId = 3,
        questionText = "In welchem heutigen Land lag das antike Mesopotamien hauptsächlich?",
        answerA = "Iran",
        answerB = "Irak",
        answerC = "Saudi-Arabien",
        answerD = "Syrien",
        correctAnswer = 1,
        explanation = "Mesopotamien lag zwischen den Flüssen Tigris und Euphrat, im Gebiet des heutigen Irak. Es gilt als eine der Wiegen der Zivilisation.",
        difficulty = 1,
        funFact = "Der Name Mesopotamien stammt aus dem Griechischen und bedeutet „Land zwischen den Flüssen"."
    ),

    Question(
        categoryId = 3,
        questionText = "Was ist die Keilschrift?",
        answerA = "Eine ägyptische Bilderschrift",
        answerB = "Eine der ältesten bekannten Schriften der Welt aus Mesopotamien",
        answerC = "Eine chinesische Zeichensprache",
        answerD = "Eine griechische Geheimschrift",
        correctAnswer = 1,
        explanation = "Die Keilschrift wurde um 3200 v. Chr. in Sumer (Mesopotamien) entwickelt und ist eine der ältesten Schriften der Menschheit.",
        difficulty = 1,
        funFact = "Die Schrift heißt Keilschrift, weil die Schreiber mit einem keilförmigen Griffel in Tontafeln drückten."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Hammurabi?",
        answerA = "Ein ägyptischer Pharao",
        answerB = "Ein babylonischer König, bekannt für sein Gesetzbuch",
        answerC = "Ein assyrischer Feldherr",
        answerD = "Ein persischer Großkönig",
        correctAnswer = 1,
        explanation = "Hammurabi (ca. 1792–1750 v. Chr.) war König von Babylon und schuf eines der ältesten bekannten Gesetzbücher der Geschichte.",
        difficulty = 1,
        funFact = "Hammurabis Gesetze enthielten das Prinzip „Auge um Auge, Zahn um Zahn" – Vergeltung sollte der Tat entsprechen."
    ),

    // --- Vikings ---

    Question(
        categoryId = 3,
        questionText = "Aus welchem Teil Europas stammten die Wikinger?",
        answerA = "Aus Osteuropa (Russland, Ukraine)",
        answerB = "Aus Südeuropa (Spanien, Portugal)",
        answerC = "Aus Skandinavien (Norwegen, Schweden, Dänemark)",
        answerD = "Aus dem Britischen Königreich",
        correctAnswer = 2,
        explanation = "Die Wikinger stammten aus Skandinavien und waren für ihre Seefahrten, Handelsreisen und Überfälle in Europa bekannt.",
        difficulty = 1,
        funFact = "Das Wort „Wiking" bedeutete ursprünglich so viel wie „Piratenfahrt" oder „Beutefahrt" – nicht eine Bezeichnung für ein Volk."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Land entdeckten Wikinger um das Jahr 1000 n. Chr. als Erste?",
        answerA = "Australien",
        answerB = "Grönland",
        answerC = "Island",
        answerD = "Nordamerika",
        correctAnswer = 3,
        explanation = "Leif Eriksson segelte um 1000 n. Chr. nach Nordamerika (Vinland) – fast 500 Jahre vor Kolumbus. Grönland und Island wurden ebenfalls von Wikingern besiedelt.",
        difficulty = 1,
        funFact = "Die Wikinger nannten Nordamerika „Vinland" (Weinland), wohl wegen der wilden Trauben, die sie dort fanden."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher nordische Gott war der Hauptgott der Wikinger?",
        answerA = "Thor",
        answerB = "Odin",
        answerC = "Freyr",
        answerD = "Loki",
        correctAnswer = 1,
        explanation = "Odin war der Hauptgott der nordischen Mythologie – Gott der Weisheit, des Krieges und des Todes. Thor (Gott des Donners) war jedoch unter gewöhnlichen Menschen sehr populär.",
        difficulty = 1,
        funFact = "Unser Wochentag Donnerstag (englisch: Thursday) ist nach Thor benannt – „Thor's Day"."
    ),

    // --- Crusades ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr begann der Erste Kreuzzug?",
        answerA = "1054",
        answerB = "1095",
        answerC = "1099",
        answerD = "1147",
        correctAnswer = 1,
        explanation = "Papst Urban II. rief 1095 zum Ersten Kreuzzug auf. Die Kreuzfahrer zogen nach Jerusalem, das sie 1099 eroberten.",
        difficulty = 1,
        funFact = "Auf dem Weg nach Jerusalem wurden auf dem Ersten Kreuzzug auch in Europa große jüdische Gemeinschaften verfolgt und ermordet."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war das erklärte Ziel der Kreuzzüge?",
        answerA = "Die Bekehrung der Mongolen zum Christentum",
        answerB = "Die Rückeroberung des Heiligen Landes (Jerusalem) für das Christentum",
        answerC = "Die Ausbreitung des Christentums nach Asien",
        answerD = "Der Sturz des Byzantinischen Reiches",
        correctAnswer = 1,
        explanation = "Die Kreuzzüge (1095–1291) sollten das Heilige Land, besonders Jerusalem, aus muslimischer Hand befreien und für Christen zurückerobern.",
        difficulty = 1,
        funFact = "Beim Vierten Kreuzzug (1204) plünderten die Kreuzfahrer Konstantinopel – die Hauptstadt des christlichen Byzantinischen Reiches."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher muslimische Feldherr eroberte Jerusalem im Jahr 1187 zurück?",
        answerA = "Suleiman der Prächtige",
        answerB = "Mehmed II.",
        answerC = "Saladin",
        answerD = "Tariq ibn Ziyad",
        correctAnswer = 2,
        explanation = "Saladin (Salah ad-Din) war Sultan von Ägypten und Syrien. Er besiegte die Kreuzfahrer 1187 in der Schlacht bei Hattin und nahm Jerusalem ein.",
        difficulty = 1,
        funFact = "Saladin war für seine Ritterlichkeit bekannt – selbst christliche Chronisten berichteten bewundernd über seinen Edelmut gegenüber besiegten Feinden."
    ),

    // --- Renaissance ---

    Question(
        categoryId = 3,
        questionText = "In welchem Land begann die Renaissance?",
        answerA = "Frankreich",
        answerB = "Deutschland",
        answerC = "Italien",
        answerD = "England",
        correctAnswer = 2,
        explanation = "Die Renaissance (Wiedergeburt) begann im 14. Jahrhundert in Italien, besonders in Städten wie Florenz, Venedig und Rom.",
        difficulty = 1,
        funFact = "Die Medici-Familie in Florenz war ein wichtiger Mäzen der Renaissance – sie förderte Künstler wie Botticelli, Leonardo da Vinci und Michelangelo."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer malte die Sixtinische Kapelle in Rom?",
        answerA = "Leonardo da Vinci",
        answerB = "Raffael",
        answerC = "Michelangelo",
        answerD = "Tizian",
        correctAnswer = 2,
        explanation = "Michelangelo malte zwischen 1508 und 1512 die berühmten Deckengemälde der Sixtinischen Kapelle im Auftrag von Papst Julius II.",
        difficulty = 1,
        funFact = "Michelangelo malte die Decke liegend auf einem Gerüst – die Arbeit dauerte vier Jahre und schadete dauerhaft seiner Gesundheit."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer malte das berühmteste Lächeln der Kunstgeschichte – die Mona Lisa?",
        answerA = "Michelangelo",
        answerB = "Raffael",
        answerC = "Leonardo da Vinci",
        answerD = "Albrecht Dürer",
        correctAnswer = 2,
        explanation = "Leonardo da Vinci schuf die Mona Lisa zwischen 1503 und 1519. Das Gemälde hängt heute im Louvre in Paris und ist das bekannteste Gemälde der Welt.",
        difficulty = 1,
        funFact = "Die Mona Lisa ist hinter Panzerglas gesichert und mit einem eigenen Klimasystem ausgestattet. Täglich kommen Tausende Besucher nur für dieses Bild."
    ),

    // --- Reformation ---

    Question(
        categoryId = 3,
        questionText = "Wann veröffentlichte Martin Luther seine 95 Thesen?",
        answerA = "1517",
        answerB = "1521",
        answerC = "1534",
        answerD = "1545",
        correctAnswer = 0,
        explanation = "Am 31. Oktober 1517 soll Martin Luther seine 95 Thesen gegen den Ablasshandel an die Tür der Schlosskirche in Wittenberg genagelt haben.",
        difficulty = 1,
        funFact = "Das „Thesenanschlagen" ist eine Legende – wahrscheinlicher ist, dass Luther seine Thesen als Brief an den Erzbischof schickte."
    ),

    Question(
        categoryId = 3,
        questionText = "Wogegen richteten sich Martin Luthers 95 Thesen?",
        answerA = "Gegen die Macht des deutschen Kaisers",
        answerB = "Gegen den Ablasshandel der katholischen Kirche",
        answerC = "Gegen die Kreuzzüge",
        answerD = "Gegen die Bibel auf Latein",
        correctAnswer = 1,
        explanation = "Luther kritisierte den Ablasshandel, bei dem die Kirche Urkunden verkaufte, mit denen Gläubige Sündenstrafen erlassen werden sollten.",
        difficulty = 1,
        funFact = "Der Ablasshandel war eine wichtige Einnahmequelle der Kirche – das Geld sollte u.a. für den Bau des Petersdoms in Rom verwendet werden."
    ),

    Question(
        categoryId = 3,
        questionText = "Was übersetzte Martin Luther ins Deutsche?",
        answerA = "Die griechischen Klassiker",
        answerB = "Die Bibel",
        answerC = "Das Gesetzbuch des Heiligen Römischen Reiches",
        answerD = "Die Werke des Aristoteles",
        correctAnswer = 1,
        explanation = "Martin Luther übersetzte das Neue Testament 1522 und die gesamte Bibel 1534 ins Deutsche und machte sie so für einfache Menschen zugänglich.",
        difficulty = 1,
        funFact = "Luthers Bibelübersetzung prägte die deutsche Schriftsprache maßgeblich und half, eine einheitliche deutsche Standardsprache zu entwickeln."
    ),

    Question(
        categoryId = 3,
        questionText = "Auf welchem Reichstag weigerte sich Martin Luther, seine Schriften zu widerrufen?",
        answerA = "Auf dem Reichstag zu Augsburg (1530)",
        answerB = "Auf dem Reichstag zu Worms (1521)",
        answerC = "Auf dem Konzil zu Trient (1545)",
        answerD = "Auf dem Reichstag zu Speyer (1526)",
        correctAnswer = 1,
        explanation = "Auf dem Reichstag zu Worms 1521 weigerte Luther sich vor Kaiser Karl V., seine Schriften zu widerrufen. Daraufhin wurde er als Ketzer geächtet.",
        difficulty = 1,
        funFact = "Kurfürst Friedrich der Weise ließ Luther nach dem Reichstag auf die Wartburg „entführen", um ihn zu schützen. Dort übersetzte Luther das Neue Testament."
    ),

    // --- Colonialism ---

    Question(
        categoryId = 3,
        questionText = "Welches Land war im 16. Jahrhundert die führende Kolonialmacht?",
        answerA = "England",
        answerB = "Frankreich",
        answerC = "Spanien und Portugal",
        answerD = "Die Niederlande",
        correctAnswer = 2,
        explanation = "Spanien und Portugal teilten durch den Vertrag von Tordesillas (1494) die neu entdeckte Welt unter sich auf und wurden zu den ersten großen Kolonialmächten.",
        difficulty = 1,
        funFact = "Der Papst vermittelte den Vertrag von Tordesillas – er zog eine Linie durch den Atlantik, die bestimmte, welche Länder zu Spanien und welche zu Portugal gehörten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die sogenannte Sklaverei im Dreieckshandel?",
        answerA = "Tauschhandel zwischen Europa, Asien und Amerika",
        answerB = "Handel zwischen Europa, Afrika und Amerika, bei dem Menschen versklavt wurden",
        answerC = "Ein mittelalterliches Handelssystem zwischen drei europäischen Städten",
        answerD = "Gewürzhandel zwischen Portugal, Indien und China",
        correctAnswer = 1,
        explanation = "Im Dreieckshandel (16.–19. Jh.) verschifften Europäer Waren nach Afrika, kauften oder raubten dort Menschen, verkauften diese in Amerika und brachten Kolonialwaren zurück.",
        difficulty = 1,
        funFact = "Schätzungsweise 12 bis 15 Millionen Afrikaner wurden im Rahmen des transatlantischen Sklavenhandels verschleppt."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Vasco da Gama?",
        answerA = "Ein spanischer Konquistador in Südamerika",
        answerB = "Ein portugiesischer Seefahrer, der den Seeweg nach Indien entdeckte",
        answerC = "Ein britischer Admiral, der Australien entdeckte",
        answerD = "Ein französischer Kartograf des 15. Jahrhunderts",
        correctAnswer = 1,
        explanation = "Vasco da Gama umsegelte 1497/98 als erster Europäer das Kap der Guten Hoffnung und erreichte auf dem Seeweg Indien.",
        difficulty = 1,
        funFact = "Durch diesen Seeweg konnten die Portugiesen den Gewürzhandel mit Asien umgehen und direkt mit Indien handeln."
    ),

    // --- American Revolution ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr erklärten die amerikanischen Kolonien ihre Unabhängigkeit von Großbritannien?",
        answerA = "1773",
        answerB = "1776",
        answerC = "1783",
        answerD = "1789",
        correctAnswer = 1,
        explanation = "Am 4. Juli 1776 verabschiedeten die 13 amerikanischen Kolonien die Unabhängigkeitserklärung und trennten sich von der britischen Krone.",
        difficulty = 1,
        funFact = "Der 4. Juli wird in den USA als „Independence Day" gefeiert – mit Feuerwerk, Paraden und Familienfeierlichkeiten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Boston Tea Party?",
        answerA = "Ein Staatsbankett der amerikanischen Gründungsväter",
        answerB = "Ein Protest amerikanischer Kolonisten gegen die britische Teesteuer",
        answerC = "Ein Handelsabkommen zwischen Amerika und England",
        answerD = "Eine Seeschlacht im amerikanischen Unabhängigkeitskrieg",
        correctAnswer = 1,
        explanation = "1773 warfen amerikanische Kolonisten in Boston britischen Tee ins Meer, um gegen die Teesteuer ohne politische Mitsprache zu protestieren.",
        difficulty = 1,
        funFact = "Die Teilnehmer der Boston Tea Party verkleideten sich als Mohawk-Indianer, um ihre Identität zu verbergen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der erste Präsident der Vereinigten Staaten von Amerika?",
        answerA = "Benjamin Franklin",
        answerB = "Thomas Jefferson",
        answerC = "John Adams",
        answerD = "George Washington",
        correctAnswer = 3,
        explanation = "George Washington wurde 1789 zum ersten Präsidenten der USA gewählt. Er führte die amerikanischen Truppen im Unabhängigkeitskrieg gegen Großbritannien.",
        difficulty = 1,
        funFact = "George Washington war der einzige US-Präsident, der einstimmig gewählt wurde – und zwar zweimal."
    ),

    // --- Napoleon ---

    Question(
        categoryId = 3,
        questionText = "Aus welchem Land stammte Napoleon Bonaparte?",
        answerA = "Frankreich",
        answerB = "Korsika (damals Frankreich, zuvor Genua)",
        answerC = "Italien",
        answerD = "Spanien",
        correctAnswer = 1,
        explanation = "Napoleon wurde 1769 auf Korsika geboren, kurz nachdem die Insel von Genua an Frankreich abgetreten worden war. Er wuchs als Franzose auf.",
        difficulty = 1,
        funFact = "Napoleons Muttersprache war Korsisch-Italienisch – Französisch lernte er erst als Kind und sprach es sein Leben lang mit Akzent."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher berühmten Schlacht erlitt Napoleon 1815 seine endgültige Niederlage?",
        answerA = "In der Schlacht bei Leipzig",
        answerB = "In der Schlacht bei Austerlitz",
        answerC = "In der Schlacht bei Waterloo",
        answerD = "In der Schlacht bei Trafalgar",
        correctAnswer = 2,
        explanation = "In der Schlacht bei Waterloo (18. Juni 1815) wurde Napoleon von britischen und preußischen Truppen unter Wellington und Blücher besiegt.",
        difficulty = 1,
        funFact = "„Mein Waterloo haben" – dieser Ausdruck für eine vernichtende Niederlage geht direkt auf Napoleons Niederlage zurück."
    ),

    Question(
        categoryId = 3,
        questionText = "Wohin wurde Napoleon nach seiner endgültigen Niederlage verbannt?",
        answerA = "Auf die Insel Elba",
        answerB = "Nach Australien",
        answerC = "Auf die Insel Sankt Helena",
        answerD = "In den Turm von London",
        correctAnswer = 2,
        explanation = "Nach Waterloo wurde Napoleon auf die abgelegene Atlantikinsel Sankt Helena verbannt, wo er 1821 starb.",
        difficulty = 1,
        funFact = "Auf Elba war Napoleon zuvor schon einmal verbannt – doch von dort floh er 1815 und kehrte für die sogenannten „Hundert Tage" an die Macht zurück."
    ),

    Question(
        categoryId = 3,
        questionText = "Was ist der Napoleonische Code (Code Civil)?",
        answerA = "Eine geheime Sprache Napoleons für seine Generäle",
        answerB = "Ein von Napoleon eingeführtes Gesetzbuch, das ganz Frankreich vereinheitlichte",
        answerC = "Die Strategie Napoleons für seine Feldzüge",
        answerD = "Ein Vertrag Napoleons mit dem Papst",
        correctAnswer = 1,
        explanation = "Der Code Civil (1804) war Napoleons Gesetzbuch, das auf den Grundsätzen der Gleichheit und Freiheit der Französischen Revolution basierte und viele Länder beeinflusste.",
        difficulty = 1,
        funFact = "Der Code Civil gilt bis heute in Frankreich und war Vorbild für die Rechtssysteme vieler Länder weltweit, u.a. in Québec und Louisiana."
    ),

    // --- Unification of Germany ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde das Deutsche Reich gegründet?",
        answerA = "1848",
        answerB = "1866",
        answerC = "1871",
        answerD = "1888",
        correctAnswer = 2,
        explanation = "Das Deutsche Reich wurde am 18. Januar 1871 im Spiegelsaal von Versailles gegründet, nachdem Preußen im Deutsch-Französischen Krieg gesiegt hatte.",
        difficulty = 1,
        funFact = "Die Reichsgründung im Spiegelsaal von Versailles war bewusst als Demütigung Frankreichs gewählt – Frankreich musste die Gründung des deutschen Kaisertums in seinem eigenen Schloss erleben."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der „Eiserne Kanzler", der die Einigung Deutschlands politisch vorantrieb?",
        answerA = "Kaiser Wilhelm II.",
        answerB = "Otto von Bismarck",
        answerC = "Helmuth von Moltke",
        answerD = "Friedrich III.",
        correctAnswer = 1,
        explanation = "Otto von Bismarck war Ministerpräsident Preußens und später erster Reichskanzler. Er trieb die Einigung Deutschlands durch „Blut und Eisen" (Kriege und Machtpolitik) voran.",
        difficulty = 1,
        funFact = "Bismarck führte in Deutschland das erste staatliche Sozialversicherungssystem der Welt ein – Kranken-, Unfall- und Altersversicherung."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Krieg führte unmittelbar zur Gründung des Deutschen Reiches 1871?",
        answerA = "Der Deutsch-Dänische Krieg",
        answerB = "Der Österreichisch-Preußische Krieg",
        answerC = "Der Deutsch-Französische Krieg",
        answerD = "Der Dreißigjährige Krieg",
        correctAnswer = 2,
        explanation = "Der Deutsch-Französische Krieg (1870/71) endete mit der französischen Niederlage. Im Zuge des Sieges schlossen sich die deutschen Staaten zum Deutschen Reich zusammen.",
        difficulty = 1,
        funFact = "Frankreich musste nach dem Krieg Elsass-Lothringen an Deutschland abtreten – ein Gebiet, das erst nach dem Ersten Weltkrieg zurückkam."
    ),

    // --- Famous Inventions ---

    Question(
        categoryId = 3,
        questionText = "Wer erfand den Buchdruck mit beweglichen Lettern um 1450?",
        answerA = "Leonardo da Vinci",
        answerB = "Johannes Gutenberg",
        answerC = "Martin Luther",
        answerD = "Albrecht Dürer",
        correctAnswer = 1,
        explanation = "Johannes Gutenberg erfand um 1450 in Mainz den Buchdruck mit beweglichen Metalllettern. Seine Bibel war eines der ersten gedruckten Bücher Europas.",
        difficulty = 1,
        funFact = "Vor Gutenbergs Erfindung mussten Bücher von Hand abgeschrieben werden. Ein einziges Buch konnte Jahre dauern – danach konnten Hunderte in Wochen gedruckt werden."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer erfand die Dampfmaschine (in ihrer modernen Form) im 18. Jahrhundert?",
        answerA = "James Watt",
        answerB = "George Stephenson",
        answerC = "Thomas Edison",
        answerD = "Isaac Newton",
        correctAnswer = 0,
        explanation = "James Watt verbesserte die Dampfmaschine 1769 entscheidend und gilt als ihr wichtigster Entwickler. Seine Maschine wurde zum Motor der Industriellen Revolution.",
        difficulty = 1,
        funFact = "Die Einheit der Leistung „Watt" wurde zu Ehren von James Watt benannt. Auch der Begriff „Pferdestärke" (PS) geht auf ihn zurück."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Erfindung des Deutschen Karl Benz aus dem Jahr 1886 veränderte die Mobilität für immer?",
        answerA = "Die Lokomotive",
        answerB = "Das Automobil mit Verbrennungsmotor",
        answerC = "Das Motorrad",
        answerD = "Das Fahrrad",
        correctAnswer = 1,
        explanation = "Karl Benz meldete 1886 sein Patent für den Motorwagen an – das gilt als Geburtsstunde des Automobils mit Verbrennungsmotor.",
        difficulty = 1,
        funFact = "Bertha Benz, die Ehefrau von Karl Benz, unternahm 1888 als erste Person der Welt eine Fernfahrt mit einem Automobil – über 100 km von Mannheim nach Pforzheim."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer erfand das Telefon im Jahr 1876?",
        answerA = "Nikola Tesla",
        answerB = "Thomas Edison",
        answerC = "Alexander Graham Bell",
        answerD = "Guglielmo Marconi",
        correctAnswer = 2,
        explanation = "Alexander Graham Bell erhielt 1876 das Patent auf das Telefon und gilt als dessen Erfinder, obwohl es damals bereits konkurrierende Entwicklungen gab.",
        difficulty = 1,
        funFact = "Bell wollte seinen Apparat ursprünglich dem Telegrafenunternehmen Western Union für 100.000 Dollar verkaufen – die Firma lehnte ab. Ein teurer Fehler."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer erfand das Penicillin, das erste Antibiotikum?",
        answerA = "Louis Pasteur",
        answerB = "Robert Koch",
        answerC = "Alexander Fleming",
        answerD = "Marie Curie",
        correctAnswer = 2,
        explanation = "Alexander Fleming entdeckte 1928 zufällig das Penicillin, als Schimmel auf seiner Bakterienkultur wuchs und die Bakterien abtötete.",
        difficulty = 1,
        funFact = "Flemings Entdeckung war ein Zufallsfund – er hatte vergessen, eine Petrischale zu verschließen. Dieser „Fehler" rettete spätere Millionen von Leben."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches revolutionäre Verkehrsmittel wurde 1825 von George Stephenson in Betrieb genommen?",
        answerA = "Das erste Dampfschiff",
        answerB = "Das erste Flugzeug",
        answerC = "Die erste öffentliche Eisenbahn",
        answerD = "Die erste U-Bahn",
        correctAnswer = 2,
        explanation = "George Stephensons „Locomotion No. 1" fuhr 1825 auf der Linie Stockton–Darlington und war die erste öffentliche dampfbetriebene Eisenbahn der Welt.",
        difficulty = 1,
        funFact = "Stephensons spätere Lokomotive „Rocket" (Rakete) aus dem Jahr 1829 wurde zur Mutter aller modernen Dampflokomotiven."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer entwickelte die erste erfolgreiche Glühbirne für den Heimgebrauch?",
        answerA = "Nikola Tesla",
        answerB = "Thomas Alva Edison",
        answerC = "Heinrich Hertz",
        answerD = "Werner von Siemens",
        correctAnswer = 1,
        explanation = "Thomas Edison entwickelte 1879 eine praktische Glühbirne, die lange genug leuchtete, um kommerziell nutzbar zu sein, und baute dazu ein ganzes Stromnetz auf.",
        difficulty = 1,
        funFact = "Edison meldete in seinem Leben über 1.000 Patente an – mehr als jeder andere Erfinder der Geschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war das erste Menschenpaar, das im Dezember 1903 mit einem Motorflugzeug flog?",
        answerA = "Charles Lindbergh",
        answerB = "Die Gebrüder Wright (Orville und Wilbur)",
        answerC = "Otto Lilienthal",
        answerD = "Louis Blériot",
        correctAnswer = 1,
        explanation = "Am 17. Dezember 1903 führten Orville und Wilbur Wright in Kitty Hawk (USA) den ersten erfolgreichen Motorflug der Geschichte durch.",
        difficulty = 1,
        funFact = "Der erste Flug der Wrights dauerte nur 12 Sekunden und legte 36 Meter zurück – aber er veränderte die Welt für immer."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer entdeckte im Jahr 1895 die Röntgenstrahlen?",
        answerA = "Wilhelm Conrad Röntgen",
        answerB = "Max Planck",
        answerC = "Albert Einstein",
        answerD = "Heinrich Hertz",
        correctAnswer = 0,
        explanation = "Der deutsche Physiker Wilhelm Conrad Röntgen entdeckte 1895 die nach ihm benannten Röntgenstrahlen und erhielt dafür 1901 den ersten Nobelpreis für Physik.",
        difficulty = 1,
        funFact = "Röntgen lehnte es ab, sein Verfahren patentieren zu lassen – er wollte, dass es der gesamten Menschheit frei zugute kommt."
    ),

    // --- Mixed Historical Contexts ---

    Question(
        categoryId = 3,
        questionText = "Welches Reich galt als das größte zusammenhängende Landreich der Geschichte?",
        answerA = "Das Britische Empire",
        answerB = "Das Römische Reich",
        answerC = "Das Mongolische Reich",
        answerD = "Das Russische Kaiserreich",
        correctAnswer = 2,
        explanation = "Das Mongolische Reich unter Dschingis Khan und seinen Nachfolgern war im 13. Jahrhundert das größte zusammenhängende Landreich der Geschichte.",
        difficulty = 1,
        funFact = "Das Mongolische Reich erstreckte sich von Korea bis Polen – etwa 24 Millionen Quadratkilometer, was einem Fünftel der gesamten Landfläche der Erde entspricht."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Seuche tötete im 14. Jahrhundert rund ein Drittel der europäischen Bevölkerung?",
        answerA = "Die Spanische Grippe",
        answerB = "Der Schwarze Tod (Pest)",
        answerC = "Die Pocken",
        answerD = "Die Cholera",
        correctAnswer = 1,
        explanation = "Der Schwarze Tod (Beulenpest) wütete von 1347 bis 1353 in Europa und tötete schätzungsweise 25 bis 30 Millionen Menschen.",
        difficulty = 1,
        funFact = "Die Pest veränderte die europäische Gesellschaft tiefgreifend – Arbeitskräftemangel stärkte die Position der Bauern und trug zum Ende des Feudalsystems bei."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann fand die Französische Revolution statt?",
        answerA = "1789",
        answerB = "1776",
        answerC = "1799",
        answerD = "1815",
        correctAnswer = 0,
        explanation = "Die Französische Revolution begann 1789 mit dem Sturm auf die Bastille am 14. Juli. Sie beendete die absolute Monarchie in Frankreich.",
        difficulty = 1,
        funFact = "Der 14. Juli (Bastillesturm) ist bis heute der französische Nationalfeiertag – „Quatorze Juillet" wird mit Paraden und Feuerwerk gefeiert."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Seidenstraße?",
        answerA = "Ein chinesischer Kaiserkanal",
        answerB = "Eine antike Handelsroute zwischen China und Europa",
        answerC = "Ein Seeweg der Portugiesen nach Asien",
        answerD = "Ein mittelalterlicher Pilgerweg nach Jerusalem",
        correctAnswer = 1,
        explanation = "Die Seidenstraße war ein Netz von Handelsrouten, das China mit Zentralasien, dem Nahen Osten und Europa verband – benannt nach dem begehrten chinesischen Seide-Export.",
        difficulty = 1,
        funFact = "Nicht nur Seide reiste auf der Seidenstraße – auch Gewürze, Glas, Papier, Religionen und Krankheitserreger wie die Pest wurden so von Kontinent zu Kontinent getragen."
    ),

    Question(
        categoryId = 3,
        questionText = "Was feiern Christen an Weihnachten – welches historische Ereignis liegt zugrunde?",
        answerA = "Die Auferstehung Jesu Christi",
        answerB = "Die Geburt von Jesus Christus",
        answerC = "Das letzte Abendmahl Jesu",
        answerD = "Die Taufe Jesu im Jordan",
        correctAnswer = 1,
        explanation = "Weihnachten (25. Dezember) feiert die Geburt von Jesus Christus in Bethlehem, dem Begründer des Christentums.",
        difficulty = 1,
        funFact = "Das genaue Geburtsdatum Jesu ist unbekannt. Der 25. Dezember wurde im 4. Jahrhundert festgelegt – möglicherweise um mit heidnischen Wintersonnenwend-Festen zu konkurrieren."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis gilt als das Ende des Mittelalters und der Beginn der Neuzeit?",
        answerA = "Die Erfindung des Buchdrucks (1450)",
        answerB = "Der Fall von Konstantinopel (1453)",
        answerC = "Die Entdeckung Amerikas (1492)",
        answerD = "Alle drei genannten Ereignisse (zwischen 1450 und 1492)",
        correctAnswer = 3,
        explanation = "Historiker nennen mehrere Ereignisse als Zäsur: den Fall Konstantinopels (1453), Gutenbergs Buchdruck (1450) und die Entdeckung Amerikas (1492). Sie alle fallen in dieselbe Epoche.",
        difficulty = 1,
        funFact = "Die Einteilung in Mittelalter und Neuzeit ist eine nachträgliche Erfindung von Historikern – die Menschen damals wussten nicht, dass sie in einem „Mittelalter" lebten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher ägyptische Pharao ließ die größte Pyramide von Gizeh bauen?",
        answerA = "Ramses II.",
        answerB = "Tutanchamun",
        answerC = "Cheops (Khufu)",
        answerD = "Kleopatra",
        correctAnswer = 2,
        explanation = "Die Große Pyramide von Gizeh wurde um 2560 v. Chr. für Pharao Cheops (Khufu) errichtet und ist das einzige noch weitgehend erhaltene Weltwunder der Antike.",
        difficulty = 1,
        funFact = "Die Große Pyramide war mit 146 Metern fast 4.000 Jahre lang das höchste Bauwerk der Welt – bis im 14. Jahrhundert der Lincolner Dom in England gebaut wurde."
    ),

)
