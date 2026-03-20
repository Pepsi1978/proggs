package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsMedium2(): List<Question> = listOf(

    // Question 1 — Gemaelde: Mona Lisa
    Question(
        categoryId = 11,
        questionText = "Von wem stammt das Gemaelde \"Mona Lisa\"?",
        answerA = "Michelangelo",
        answerB = "Raffael",
        answerC = "Leonardo da Vinci",
        answerD = "Tizian",
        correctAnswer = 2,
        explanation = "Die Mona Lisa wurde von Leonardo da Vinci zwischen ca. 1503 und 1519 gemalt. Sie haengt heute im Louvre in Paris und gilt als das bekannteste Gemaelde der Welt.",
        difficulty = 2,
        funFact = "Die Mona Lisa ist nur 77 x 53 cm gross — viele Besucher sind ueberrascht, wie klein das Original tatsaechlich ist."
    ),

    // Question 2 — Bauwerk: Eiffelturm
    Question(
        categoryId = 11,
        questionText = "Fuer welchen Anlass wurde der Eiffelturm in Paris erbaut?",
        answerA = "Als staendiges Denkmal fuer Napoleon",
        answerB = "Als Radioturm fuer ganz Frankreich",
        answerC = "Als Eingangstor zur Weltausstellung 1889",
        answerD = "Als Leuchtturm an der Seine",
        correctAnswer = 2,
        explanation = "Der Eiffelturm wurde von Gustave Eiffel als Eingangstor zur Weltausstellung 1889 in Paris gebaut, die den 100. Jahrestag der Franzoesischen Revolution feierte. Er war urspruenglich als temporaeres Bauwerk geplant.",
        difficulty = 2,
        funFact = "Als der Eiffelturm gebaut wurde, galt er vielen Parisern als haesslich. Schriftsteller Guy de Maupassant soll regelmaessig dort gegessen haben — weil es das einzige Gebaeude war, von dem aus man ihn nicht sehen konnte."
    ),

    // Question 3 — Kunstepoche: Impressionismus
    Question(
        categoryId = 11,
        questionText = "Welcher Kuenstler gilt als Hauptvertreter des Impressionismus?",
        answerA = "Pablo Picasso",
        answerB = "Claude Monet",
        answerC = "Edvard Munch",
        answerD = "Salvador Dali",
        correctAnswer = 1,
        explanation = "Claude Monet ist einer der Hauptvertreter des Impressionismus. Sein Gemaelde \"Impression, Sonnenaufgang\" (1872) gab der gesamten Bewegung ihren Namen.",
        difficulty = 2,
        funFact = "Monet litt in seinem Alter an grauem Star und konnte kaum noch Farben unterscheiden. Viele seiner spaeten Werke sind deshalb ungewoehnlich dunkel und rot-braun getoentet."
    ),

    // Question 4 — Skulptur: David
    Question(
        categoryId = 11,
        questionText = "Wo befindet sich das Original von Michelangelos Skulptur \"David\"?",
        answerA = "Im Vatikan in Rom",
        answerB = "In der Uffizien-Galerie in Florenz",
        answerC = "Im Louvre in Paris",
        answerD = "In der Galleria dell'Accademia in Florenz",
        correctAnswer = 3,
        explanation = "Das Original von Michelangelos \"David\" steht seit 1873 in der Galleria dell'Accademia in Florenz. Die bekannte Kopie auf dem Piazza della Signoria ist nur eine Nachbildung.",
        difficulty = 2,
        funFact = "Michelangelo schuf den \"David\" aus einem einzigen Marmorblock, der von anderen Bildhauern bereits begonnen und dann aufgegeben worden war. Er brauchte zwei Jahre fuer das Werk (1501–1504)."
    ),

    // Question 5 — Bauwerk: Kolosseum
    Question(
        categoryId = 11,
        questionText = "Wie viele Zuschauer fasste das roemische Kolosseum urspruenglich?",
        answerA = "Etwa 10.000",
        answerB = "Etwa 30.000",
        answerC = "Etwa 50.000",
        answerD = "Ueber 100.000",
        correctAnswer = 2,
        explanation = "Das Kolosseum in Rom fasste schaetzungsweise 50.000 bis 80.000 Zuschauer. Es wurde unter Kaiser Vespasian begonnen und im Jahr 80 n. Chr. unter Titus eingeweiht.",
        difficulty = 2,
        funFact = "Das Kolosseum hatte ein riesiges Sonnensegel namens \"Velarium\", das von Matrosen der roemischen Marine bedient wurde, um die Zuschauer vor Sonne und Regen zu schuetzen."
    ),

    // Question 6 — Gemaelde: Sternennacht
    Question(
        categoryId = 11,
        questionText = "In welchem Museum haengt Vincent van Goghs Gemaelde \"Sternennacht\"?",
        answerA = "Im Rijksmuseum Amsterdam",
        answerB = "Im Museum of Modern Art in New York",
        answerC = "Im Van Gogh Museum Amsterdam",
        answerD = "Im Musee d'Orsay in Paris",
        correctAnswer = 1,
        explanation = "Vincent van Goghs \"Sternennacht\" (1889) haengt im Museum of Modern Art (MoMA) in New York. Van Gogh malte es waehrend seines Aufenthalts in der psychiatrischen Anstalt Saint-Paul-de-Mausole in Saint-Remy.",
        difficulty = 2,
        funFact = "Van Gogh schrieb in Briefen an seinen Bruder Theo oft ueber seine Sehnsucht nach dem Nachthimmel. Er malte \"Sternennacht\" fast komplett aus der Erinnerung und Fantasie."
    ),

    // Question 7 — Architektur: Bauhaus
    Question(
        categoryId = 11,
        questionText = "Wer gruendete die Kunstschule \"Bauhaus\" im Jahr 1919?",
        answerA = "Ludwig Mies van der Rohe",
        answerB = "Le Corbusier",
        answerC = "Walter Gropius",
        answerD = "Paul Klee",
        correctAnswer = 2,
        explanation = "Walter Gropius gruendete das Bauhaus 1919 in Weimar. Die Schule verband Kunst, Handwerk und Architektur und praegt bis heute das moderne Design. Spaeter zog das Bauhaus nach Dessau und schliesslich nach Berlin.",
        difficulty = 2,
        funFact = "Das Bauhaus in Dessau, gebaut von Walter Gropius selbst, gilt als eines der wichtigsten Architekturwerke der Moderne und steht auf der UNESCO-Welterbeliste."
    ),

    // Question 8 — Gemaelde: Der Schrei
    Question(
        categoryId = 11,
        questionText = "Von wem stammt das Gemaelde \"Der Schrei\"?",
        answerA = "Gustav Klimt",
        answerB = "Egon Schiele",
        answerC = "Ernst Ludwig Kirchner",
        answerD = "Edvard Munch",
        correctAnswer = 3,
        explanation = "\"Der Schrei\" wurde 1893 vom norwegischen Maler Edvard Munch geschaffen. Es gilt als eines der Schluesselbild des Expressionismus und der modernen Kunst. Munch schuf insgesamt vier Versionen des Motivs.",
        difficulty = 2,
        funFact = "Edvard Munch beschrieb die Entstehungsidee in seinem Tagebuch: Er spazierte mit Freunden und fuellte sich ploetzlich von einem \"unendlichen Schrei der Natur\" erfasst."
    ),

    // Question 9 — Bauwerk: Taj Mahal
    Question(
        categoryId = 11,
        questionText = "Wer liess den Taj Mahal in Indien erbauen und warum?",
        answerA = "Kaiser Akbar als Herrscherpalast",
        answerB = "Schah Jahan als Mausoleum fuer seine Frau",
        answerC = "Aurangzeb als Moschee",
        answerD = "Humayun als Tempel",
        correctAnswer = 1,
        explanation = "Der Taj Mahal wurde von Mogulkaiser Schah Jahan als Mausoleum fuer seine verstorbene Lieblingsfrau Mumtaz Mahal erbauen lassen. Der Bau dauerte von 1631 bis 1648 und beschaeftigte etwa 20.000 Arbeiter.",
        difficulty = 2,
        funFact = "Es heisst, dass Schah Jahan nach Fertigstellung des Taj Mahal die Haende der Handwerker abschneiden liess, damit sie niemals ein vergleichbares Bauwerk errichten konnten. Historiker bezweifeln diese Geschichte jedoch."
    ),

    // Question 10 — Kunstepoche: Barock
    Question(
        categoryId = 11,
        questionText = "Welcher Zeitraum wird als Barockepoche in der Kunst bezeichnet?",
        answerA = "1200 bis 1400",
        answerB = "1400 bis 1600",
        answerC = "1600 bis 1750",
        answerD = "1800 bis 1900",
        correctAnswer = 2,
        explanation = "Die Barockepoche erstreckt sich grob von 1600 bis 1750. Sie ist gepraegt von Pracht, Dramatik und starken Licht-Schatten-Kontrasten (Chiaroscuro). Bedeutende Barockmeister sind Caravaggio, Rubens und Rembrandt.",
        difficulty = 2,
        funFact = "Das Wort \"Barock\" stammt vom portugiesischen \"barroco\" (unregelmassige Perle) und war urspruenglich ein abwertender Begriff fuer den als uebertrieben und unharmonisch empfundenen Stil."
    ),

    // Question 11 — Gemaelde: Guernica
    Question(
        categoryId = 11,
        questionText = "Was stellte Pablo Picasso in seinem Gemaelde \"Guernica\" dar?",
        answerA = "Den Sturm auf die Bastille",
        answerB = "Die Bombardierung einer baskischen Stadt im Spanischen Buergerkrieg",
        answerC = "Das Ende des Zweiten Weltkriegs",
        answerD = "Eine mythologische Szene aus der griechischen Antike",
        correctAnswer = 1,
        explanation = "Picassos \"Guernica\" (1937) zeigt das Leiden der Bevoelkerung der baskischen Stadt Guernica, die am 26. April 1937 von deutschen und italienischen Fliegern im Spanischen Buergerkrieg bombardiert wurde. Es ist ein Antikriegs-Symbol.",
        difficulty = 2,
        funFact = "Als ein Nazi-Offizier Picasso fragte: \"Haben Sie das gemacht?\", soll er geantwortet haben: \"Nein, das haben Sie gemacht.\" Das Gemaelde haengt heute im Museo Reina Sofia in Madrid."
    ),

    // Question 12 — Architektur: Sagrada Familia
    Question(
        categoryId = 11,
        questionText = "Wer entwarf die Sagrada Familia in Barcelona?",
        answerA = "Santiago Calatrava",
        answerB = "Antoni Gaudi",
        answerC = "Oscar Niemeyer",
        answerD = "Zaha Hadid",
        correctAnswer = 1,
        explanation = "Die Sagrada Familia wurde vom katalanischen Architekten Antoni Gaudi entworfen. Der Bau begann 1882 und Gaudi widmete ihm die letzten 43 Jahre seines Lebens. Er starb 1926 bei einem Strassenbahnunfall — die Kirche ist bis heute unvollendet.",
        difficulty = 2,
        funFact = "Antoni Gaudi ist in der Sagrada Familia begraben. Nach seinem Tod wurde er seliggesprochen und befindet sich auf dem Weg zur Heiligsprechung durch die katholische Kirche."
    ),

    // Question 13 — Gemaelde: Sistinische Kapelle
    Question(
        categoryId = 11,
        questionText = "Was malte Michelangelo an die Decke der Sixtinischen Kapelle im Vatikan?",
        answerA = "Das juengste Gericht",
        answerB = "Szenen aus dem Leben Jesu",
        answerC = "Bilder der Apostel",
        answerD = "Szenen aus dem Alten Testament, besonders aus dem Buch Genesis",
        correctAnswer = 3,
        explanation = "Michelangelo malte zwischen 1508 und 1512 Szenen aus dem Alten Testament auf die Decke der Sixtinischen Kapelle. Das bekannteste Motiv ist die \"Erschaffung Adams\". Das Juengste Gericht malte er spaeter an die Altarwand.",
        difficulty = 2,
        funFact = "Michelangelo soll die Deckenmalerei im Stehen oder Sitzen gemalt haben, nicht liegend — so die neueste Forschung. Die Legende vom Liegen auf dem Ruecken ist wohl ein Mythos."
    ),

    // Question 14 — Bauwerk: Chrysler Building
    Question(
        categoryId = 11,
        questionText = "In welcher Stadt steht das beruehmte Art-deco-Hochhaus \"Chrysler Building\"?",
        answerA = "Chicago",
        answerB = "Los Angeles",
        answerC = "New York",
        answerD = "Detroit",
        correctAnswer = 2,
        explanation = "Das Chrysler Building steht in New York City und wurde 1930 fertiggestellt. Es ist ein Meisterwerk des Art deco und war kurz das hoechste Gebaeude der Welt, bevor es vom Empire State Building uebertroffen wurde.",
        difficulty = 2,
        funFact = "Das Chrysler Building wurde nach dem Automobilhersteller Walter Chrysler benannt, der den Bau privat finanzierte — nicht mit dem Geld seiner Firma. Es hat eine charakteristische Edelstahlspitze."
    ),

    // Question 15 — Kunstepoche: Renaissance
    Question(
        categoryId = 11,
        questionText = "Was bedeutet das Wort \"Renaissance\" auf Deutsch?",
        answerA = "Neugeburt oder Wiedergeburt",
        answerB = "Goldenes Zeitalter",
        answerC = "Aufklaerung",
        answerD = "Himmelsgabe",
        correctAnswer = 0,
        explanation = "\"Renaissance\" ist Franzoesisch und bedeutet \"Wiedergeburt\". Es bezeichnet die europaeische Kulturepoche vom 14. bis 16. Jahrhundert, die die Ideale und Kunstformen der griechisch-roemischen Antike wiederbeleben wollte.",
        difficulty = 2,
        funFact = "Leonardo da Vinci war der Inbegriff des Renaissance-Menschen: Er war Maler, Bildhauer, Architekt, Musiker, Mathematiker, Ingenieur und Naturwissenschaftler zugleich."
    ),

    // Question 16 — Gemaelde: Kuss von Klimt
    Question(
        categoryId = 11,
        questionText = "Welches Material verwendete Gustav Klimt besonders charakteristisch in seinem Gemaelde \"Der Kuss\"?",
        answerA = "Silberne Farbe und Spiegelscherben",
        answerB = "Blattgold",
        answerC = "Echte Edelsteine",
        answerD = "Perlmutt und Lack",
        correctAnswer = 1,
        explanation = "Gustav Klimt verwendete in \"Der Kuss\" (1908) echtes Blattgold. Diese goldene Periode gilt als seine bedeutendste Schaffensphase. Das Werk haengt im Oesterreichischen Belvedere in Wien.",
        difficulty = 2,
        funFact = "Klimts \"Goldene Periode\" wurde durch eine Reise nach Ravenna inspiriert, wo er byzantinische Mosaike mit echtem Goldmosaik sah. Er war so fasziniert, dass er Blattgold in seine Kunst integrierte."
    ),

    // Question 17 — Architektur: Parthenon
    Question(
        categoryId = 11,
        questionText = "Welcher Gottheit war der Parthenon auf der Akropolis in Athen geweiht?",
        answerA = "Zeus",
        answerB = "Apollon",
        answerC = "Athena",
        answerD = "Poseidon",
        correctAnswer = 2,
        explanation = "Der Parthenon war der Tempel der Stadtgoettin Athena Parthenos (Athena die Jungfrau). Er wurde im 5. Jahrhundert v. Chr. unter Perikles erbaut und gilt als Hoechstleistung der griechischen Architektur.",
        difficulty = 2,
        funFact = "Der Parthenon enthielt einst eine riesige Statue der Athena aus Elfenbein und Gold, die etwa 12 Meter hoch war. Die Statue ist verloren gegangen; nur Beschreibungen und Kopien sind erhalten."
    ),

    // Question 18 — Gemaelde: Rembrandt
    Question(
        categoryId = 11,
        questionText = "Fuer welche malerische Technik ist Rembrandt van Rijn besonders bekannt?",
        answerA = "Pointillismus (Bilder aus Punkten)",
        answerB = "Chiaroscuro (starke Licht-Schatten-Kontraste)",
        answerC = "Freskotechnik (Malen auf nassen Putz)",
        answerD = "Aquarellmalerei",
        correctAnswer = 1,
        explanation = "Rembrandt van Rijn ist beruehmt fuer seine meisterhafte Beherrschung des Chiaroscuro — dem Spiel von Licht und Schatten. Warmes Licht beleuchtet die Hauptfiguren, waehrend der Hintergrund im Dunkel bleibt.",
        difficulty = 2,
        funFact = "Rembrandt ist der Kuenstler, der in der Kunstgeschichte die meisten Selbstportaets hinterlassen hat. Er malte sich ueber sein ganzes Leben hinweg und dokumentierte so sein Altern."
    ),

    // Question 19 — Bauwerk: Kolosseum vs. Pantheon
    Question(
        categoryId = 11,
        questionText = "Was ist das Pantheon in Rom?",
        answerA = "Ein antikes Sportstadion",
        answerB = "Ein Triumphbogen",
        answerC = "Ein Tempel fuer alle roemischen Goetter mit einer grossen Kuppel",
        answerD = "Eine Thermen-Anlage (Badehaus)",
        correctAnswer = 2,
        explanation = "Das Pantheon ist ein antiker roemischer Tempel, der allen Goettern geweiht war. Seine Kuppel mit dem offenen Opaion (Loch) hat einen Durchmesser von 43,3 Metern — lange Zeit die groesste Kuppel der Welt.",
        difficulty = 2,
        funFact = "Das Opaion (Loch in der Kuppel des Pantheons) hat einen Durchmesser von 9 Metern und ist die einzige Lichtquelle. Bei Regen faellt das Wasser einfach hinein und laeuft durch Abflusslocher im Boden ab."
    ),

    // Question 20 — Kunstepoche: Expressionismus
    Question(
        categoryId = 11,
        questionText = "Was ist ein Merkmal des Expressionismus in der Kunst?",
        answerA = "Genaue Wiedergabe der Realitaet mit fotorealistischen Details",
        answerB = "Darstellung innerer Gefuehle und Emotionen durch verzerrte Formen und kraeftige Farben",
        answerC = "Geometrische Abstraktion mit reinen Formen und Primaerfarben",
        answerD = "Darstellung von Traumwelten und dem Unbewussten",
        correctAnswer = 1,
        explanation = "Der Expressionismus (ca. 1905–1925) wollte innere Gefuehle und subjektive Erlebnisse ausdrucken. Dazu wurden Farben verstaerkt, Formen verzerrt und die Realitaet absichtlich entstellt. Wichtige Gruppen waren \"Die Bruecke\" und \"Der Blaue Reiter\".",
        difficulty = 2,
        funFact = "Die Kuenstlergruppe \"Die Bruecke\" gruendete sich 1905 in Dresden. Einer ihrer Gruender war Ernst Ludwig Kirchner, der Strassenszenen Berlins in grellen Farben und kantigen Formen malte."
    ),

    // Question 21 — Architektur: Berliner Philharmonie
    Question(
        categoryId = 11,
        questionText = "Wer entwarf die Berliner Philharmonie?",
        answerA = "Mies van der Rohe",
        answerB = "Hans Scharoun",
        answerC = "Walter Gropius",
        answerD = "Bruno Taut",
        correctAnswer = 1,
        explanation = "Die Berliner Philharmonie wurde von Hans Scharoun entworfen und 1963 eroeffnet. Ihr zeltartiges Dach und der vineyardaehnliche Konzertsaal (Orchester in der Mitte, Publikum ringsum) galten als revolutionaer.",
        difficulty = 2,
        funFact = "Der Dirigent Herbert von Karajan nannte die Berliner Philharmonie nach ihrer Eroeffnung das \"akustisch beste Konzertsaal der Welt\" — ein Urteil, das viele Musikexperten bis heute teilen."
    ),

    // Question 22 — Gemaelde: Sonnenblumen
    Question(
        categoryId = 11,
        questionText = "Wie viele verschiedene Versionen des Gemaeldes \"Sonnenblumen\" malte Vincent van Gogh?",
        answerA = "Nur eine",
        answerB = "Zwei",
        answerC = "Fuenf",
        answerD = "Zwoelf",
        correctAnswer = 2,
        explanation = "Van Gogh malte insgesamt fuenf Versionen seiner \"Sonnenblumen\" als Teil einer Serie von Wanddekorationen fuer das Gelbe Haus in Arles. Zwei davon entstanden im August 1888, drei weitere 1889.",
        difficulty = 2,
        funFact = "Eine der Sonnenblumen-Versionen von Van Gogh wurde 1987 bei Christie's fuer umgerechnet 36 Millionen Dollar versteigert — damals ein Weltrekord fuer ein Kunstwerk."
    ),

    // Question 23 — Bauwerk: Alhambra
    Question(
        categoryId = 11,
        questionText = "In welchem Land befindet sich die Palastanlage Alhambra?",
        answerA = "Marokko",
        answerB = "Portugal",
        answerC = "Tunesien",
        answerD = "Spanien",
        correctAnswer = 3,
        explanation = "Die Alhambra liegt in Granada, Spanien. Sie ist ein Palast- und Festungskomplex der maurischen Herrscher aus dem 13. und 14. Jahrhundert und gilt als Meisterwerk islamischer Architektur in Europa.",
        difficulty = 2,
        funFact = "Der Name \"Alhambra\" stammt vom Arabischen \"al-Hamra\" und bedeutet \"die Rote\" — ein Verweis auf die roten Mauerziegel, die das Ensemble von der Ebene aus gesehen rot erscheinen lassen."
    ),

    // Question 24 — Kunsttechnik: Fresko
    Question(
        categoryId = 11,
        questionText = "Was bedeutet die Maltechnik \"Fresko\"?",
        answerA = "Malen auf trockene Leinwand mit Oelfarbe",
        answerB = "Malen auf feuchten Kalkputz, sodass die Farbe beim Trocknen dauerhaft eingebunden wird",
        answerC = "Malen mit Aquarellfarben auf nassem Papier",
        answerD = "Zeichnen mit Kohle auf Kalkstein",
        correctAnswer = 1,
        explanation = "Beim Fresko wird Farbe direkt in frischen, feuchten Kalkputz gemalt. Beim Trocknen verbindet sich die Farbe chemisch mit dem Putz und wird extrem haltbar. Diese Technik wurde fuer grosse Wandmalereien verwendet, z.B. in der Sixtinischen Kapelle.",
        difficulty = 2,
        funFact = "\"Fresko\" kommt vom Italienischen \"fresco\" (frisch). Michelangelo musste beim Malen der Sixtinischen Kapelle sehr schnell arbeiten, da der Putz innerhalb weniger Stunden trocknet."
    ),

    // Question 25 — Gemaelde: Das Maedchen mit dem Perlenohrring
    Question(
        categoryId = 11,
        questionText = "Von wem stammt das Gemaelde \"Das Maedchen mit dem Perlenohrring\"?",
        answerA = "Jan Vermeer",
        answerB = "Frans Hals",
        answerC = "Peter Paul Rubens",
        answerD = "Jacob van Ruisdael",
        correctAnswer = 0,
        explanation = "\"Das Maedchen mit dem Perlenohrring\" (ca. 1665) ist ein Werk des niederlaendischen Meisters Jan Vermeer. Es wird oft als das \"Mona Lisa des Nordens\" bezeichnet und haengt im Mauritshuis in Den Haag.",
        difficulty = 2,
        funFact = "Es ist unbekannt, wer das Maedchen auf Vermeers Gemaelde ist. Manche Forscher glauben, es koennte seine Tochter Maria gewesen sein. Der Film \"Das Maedchen mit dem Perlenohrring\" (2003) erforschte diese Frage kuenstlerisch."
    ),

    // Question 26 — Architektur: Hagia Sophia
    Question(
        categoryId = 11,
        questionText = "Was ist die Hagia Sophia in Istanbul?",
        answerA = "Ein ostroemisch-byzantinisches Meisterwerk mit riesiger Kuppel",
        answerB = "Ein osmanischer Sultanspalast",
        answerC = "Ein antikes roemisches Theater",
        answerD = "Eine Grabmoschee",
        correctAnswer = 0,
        explanation = "Die Hagia Sophia wurde 537 n. Chr. als byzantinische Kathedrale geweiht. Ihre Zentralkuppel mit 31 Metern Durchmesser war 900 Jahre lang die groesste Kuppel der Welt. Heute ist sie eine Moschee und ein Museum.",
        difficulty = 2,
        funFact = "Beim Bau der Hagia Sophia soll Kaiser Justinian I. beim Betreten gesagt haben: \"Salomon, ich habe dich uebertroffen!\" — ein Vergleich mit dem beruehmdesten Tempel der Bibel."
    ),

    // Question 27 — Kunstepoche: Romantik
    Question(
        categoryId = 11,
        questionText = "Welcher deutsche Maler der Romantik malte die Ikone \"Der Wanderer ueber dem Nebelmeer\"?",
        answerA = "Eugene Delacroix",
        answerB = "Caspar David Friedrich",
        answerC = "Carl Spitzweg",
        answerD = "Philipp Otto Runge",
        correctAnswer = 1,
        explanation = "Caspar David Friedrich malte \"Der Wanderer ueber dem Nebelmeer\" um 1818. Es gilt als Inbegriff der deutschen Romantik und zeigt einen einsamen Mann, der von hinten auf eine nebelverhangene Landschaft blickt.",
        difficulty = 2,
        funFact = "Der Wanderer in Friedrichs Gemaelde schaut immer von hinten ins Bild — Friedrich malte seine Figuren oft so, damit der Betrachter sich selbst mit ihnen identifizieren und in die Landschaft versetzt fuehlen kann."
    ),

    // Question 28 — Bauwerk: Kolosseum Material
    Question(
        categoryId = 11,
        questionText = "Welches Material machte den roemischen Betonbau moeglich und revolutionaer?",
        answerA = "Granit aus Aegypten",
        answerB = "Opus caementicium (roemischer Beton mit Puzzolanaerde)",
        answerC = "Gebrannte Ziegel aus dem Nil-Tal",
        answerD = "Kalksteinbloecke aus Karthago",
        correctAnswer = 1,
        explanation = "Opus caementicium war der roemische Beton, gemischt mit Puzzolanasche aus der Gegend um Pozzuoli. Er wurde wasserresistent, ausserordentlich haltbar und erlaubte gewoelbte und kuppelfoermige Bauten.",
        difficulty = 2,
        funFact = "Roemischer Beton ist in vielen Aspekten haltbarer als moderner Beton. Forscher entdeckten, dass Meerwasser den antiken Beton chemisch staerker macht — ein Geheimnis, das die Bautechnik noch heute erforscht."
    ),

    // Question 29 — Gemaelde: Starke Frauen
    Question(
        categoryId = 11,
        questionText = "Welche Malerin ist beruhmt fuer ihre selbstportaets mit mexikanischer Volkskunst-Symbolik?",
        answerA = "Georgia O'Keeffe",
        answerB = "Mary Cassatt",
        answerC = "Frida Kahlo",
        answerD = "Tamara de Lempicka",
        correctAnswer = 2,
        explanation = "Frida Kahlo ist beruehmt fuer ihre eindringlichen Selbstportaets, in denen sie mexikanische Folklore, koerperlichen Schmerz und persoenliche Erlebnisse verband. Sie malte 55 Selbstportaets und erlangte erst nach ihrem Tod weltweiten Ruhm.",
        difficulty = 2,
        funFact = "Frida Kahlo begann erst nach einem schweren Busunglueck mit dem Malen. Waehrend ihrer langen Genesungszeit malte sie ihr erstes Selbstportaet mithilfe eines Spiegels, den ihre Mutter ueber ihrem Bett befestigt hatte."
    ),

    // Question 30 — Architektur: Gotik
    Question(
        categoryId = 11,
        questionText = "Was ist ein typisches Merkmal gotischer Kathedralen?",
        answerA = "Runde Kuppeln und Saeulenvorhallen wie in der Antike",
        answerB = "Spitzbogen, Strebewerk und grosse Fenster mit Buntglas",
        answerC = "Flache Decken und massige Mauern wie in der Romanik",
        answerD = "Weisse Kalkwände ohne Ornamente",
        correctAnswer = 1,
        explanation = "Die Gotik (ca. 1150–1500) ist gepraegt durch Spitzbogen, Kreuzrippengewoelbe und Strebewerk, das die Aussenmauern stuetzt. So konnten riesige Fenster eingebaut werden — z.B. im Koelner Dom oder der Kathedrale Notre-Dame.",
        difficulty = 2,
        funFact = "Der Koelner Dom brauchte ueber 600 Jahre bis zur Fertigstellung (1248–1880). Er war kurz das hoechste Gebaeude der Welt und ist heute UNESCO-Weltkulturerbe."
    ),

    // Question 31 — Gemaelde: Picasso Kubismus
    Question(
        categoryId = 11,
        questionText = "Was ist das Grundprinzip des Kubismus, den Pablo Picasso mitbegruendete?",
        answerA = "Darstellung von Gegenstaenden aus mehreren Perspektiven gleichzeitig",
        answerB = "Aufloesung aller Formen in Punkte und Farbtupfer",
        answerC = "Strenge Nachahmung der Natur mit wissenschaftlicher Prazision",
        answerD = "Darstellung von Traumbildern und dem Unbewussten",
        correctAnswer = 0,
        explanation = "Der Kubismus (ab ca. 1907) zerlegte Objekte in geometrische Formen und stellte sie aus mehreren Perspektiven gleichzeitig dar. Picasso und Georges Braque entwickelten ihn gemeinsam. Das beruehmdeste fruehe Werk ist Picassos \"Les Demoiselles d'Avignon\" (1907).",
        difficulty = 2,
        funFact = "Der Name \"Kubismus\" geht auf einen abwertenden Kommentar zurueck: Der Kritiker Louis Vauxcelles beschrieb Braques Bilder als \"bizarre cubiques\" (selsame Wuerfel) — der Name blieb."
    ),

    // Question 32 — Bauwerk: Neuschwanstein
    Question(
        categoryId = 11,
        questionText = "Wer liess Schloss Neuschwanstein in Bayern erbauen?",
        answerA = "Kaiser Wilhelm II.",
        answerB = "Koenig Ludwig II. von Bayern",
        answerC = "Koenig Maximilian I. von Bayern",
        answerD = "Koenig Friedrich I. von Preussen",
        correctAnswer = 1,
        explanation = "Schloss Neuschwanstein wurde von Koenig Ludwig II. von Bayern in Auftrag gegeben und zwischen 1869 und 1886 erbaut. Ludwig II. lebte dort nur kurze Zeit, bevor er unter mysteriosen Umstaenden starb.",
        difficulty = 2,
        funFact = "Neuschwanstein diente als Vorlage fuer das Schloss des Dornroeschens in den Disney-Zeichentrickfilmen. Walt Disney besuchte das Schloss in den 1950ern und war begeistert."
    ),

    // Question 33 — Kunsttechnik: Tempera vs. Oel
    Question(
        categoryId = 11,
        questionText = "Welche Maltechnik loeste die Temperamalerei in der Renaissance ab und erlaubte reichere Farbtiefen?",
        answerA = "Aquarell",
        answerB = "Gouache",
        answerC = "Oelmalerei",
        answerD = "Acrylmalerei",
        correctAnswer = 2,
        explanation = "Die Oelmalerei (mit Leinoelfarben) loeste ab dem 15. Jahrhundert die Temperamalerei ab. Sie erlaubt duennere Schichten, reichere Farbtiefen und laesst mehr Zeit zum Korrektieren. Jan van Eyck gilt als einer der Wegbereiter dieser Technik.",
        difficulty = 2,
        funFact = "Tempera (Eigelb und Pigment) trocknet sehr schnell und kann nicht gemischt werden — man muss in kleinen Strichen arbeiten. Oelfarbe hingegen trocknet langsam, was fliessende Uebergaenge ermoeglicht."
    ),

    // Question 34 — Architektur: Empire State Building
    Question(
        categoryId = 11,
        questionText = "Wie lange war das Empire State Building das hoechste Gebaeude der Welt?",
        answerA = "5 Jahre (1931–1936)",
        answerB = "Etwa 40 Jahre (1931–1972)",
        answerC = "10 Jahre (1931–1941)",
        answerD = "Nur 1 Jahr (1931–1932)",
        correctAnswer = 1,
        explanation = "Das Empire State Building in New York (Eroeffnung 1931) war etwa 40 Jahre das hoechste Gebaeude der Welt, bis es 1972 vom World Trade Center uebertroffen wurde. Es war in nur 410 Bautagen errichtet worden.",
        difficulty = 2,
        funFact = "Das Empire State Building wurde waehrend der Grossen Depression gebaut. Um Kosten zu senken, nutzte man Fliessbandprinzipien: Auf dem Hoehepunkt wurden pro Tag 14 Stockwerke fertiggestellt."
    ),

    // Question 35 — Gemaelde: Pointillismus
    Question(
        categoryId = 11,
        questionText = "Welcher Kuenstler begruendete den Pointillismus mit seinem Gemaelde \"Ein Sonntagnachmittag auf der Insel La Grande Jatte\"?",
        answerA = "Paul Gauguin",
        answerB = "Paul Cezanne",
        answerC = "Georges Seurat",
        answerD = "Henri de Toulouse-Lautrec",
        correctAnswer = 2,
        explanation = "Georges Seurat schuf mit \"Ein Sonntagnachmittag auf der Insel La Grande Jatte\" (1886) das Hauptwerk des Pointillismus. Er malte das Bild ausschliesslich aus kleinen Farbpunkten, die sich im Auge des Betrachters zu Farben mischen.",
        difficulty = 2,
        funFact = "Seurat arbeitete zwei Jahre an diesem Gemaelde. Das fertige Werk ist mehr als 3 Meter breit und 2 Meter hoch. Es enthaelt Millionen von Farbpunkten, die Seurat mit wissenschaftlicher Praezision aufgebracht hat."
    ),

    // Question 36 — Bauwerk: Stonehenge
    Question(
        categoryId = 11,
        questionText = "Auf welcher Insel befindet sich das Steinkreis-Monument Stonehenge?",
        answerA = "Irland",
        answerB = "Schottland",
        answerC = "England",
        answerD = "Daenemark",
        correctAnswer = 2,
        explanation = "Stonehenge liegt auf der Salisbury Plain in Wiltshire, England, und wurde in mehreren Phasen zwischen 3000 und 1500 v. Chr. errichtet. Es gilt als wichtigstes praehist. Bauwerk Europas.",
        difficulty = 2,
        funFact = "Einige der Steine von Stonehenge stammen aus Wales, ueber 200 km entfernt. Wie die Ureinwohner die bis zu 25 Tonnen schweren Blausteine transportierten, ist bis heute nicht vollstaendig geklaert."
    ),

    // Question 37 — Kunstepoche: Surrealismus
    Question(
        categoryId = 11,
        questionText = "Welches Gemaelde des Surrealisten Salvador Dali zeigt zerfliessende Uhren?",
        answerA = "Die Bestaendigkeit der Erinnerung",
        answerB = "Das Raetsel der Stunde",
        answerC = "Die Traumzeit",
        answerD = "Die weiche Konstruktion",
        correctAnswer = 0,
        explanation = "\"Die Bestaendigkeit der Erinnerung\" (\"The Persistence of Memory\", 1931) ist das beruehmdeste Werk von Salvador Dali. Die zerfliessenden Uhren gelten als Symbol der Relativitaet der Zeit.",
        difficulty = 2,
        funFact = "Dali soll die Idee fuer die schmelzenden Uhren gehabt haben, waehrend er Camembert beobachtete, der in der Sonne schmolz. Das Gemaelde ist ueberraschend klein: nur 24 x 33 cm."
    ),

    // Question 38 — Architektur: Chinesische Mauer
    Question(
        categoryId = 11,
        questionText = "Ist die Chinesische Mauer vom Weltraum aus mit blossem Auge sichtbar?",
        answerA = "Ja, deutlich von der Internationalen Raumstation (ISS)",
        answerB = "Ja, aber nur bei sehr gutem Wetter und Sonnenschein",
        answerC = "Nein, sie ist zu schmal — dieser Mythos ist widerlegt",
        answerD = "Nur vom Mond aus bei Nacht",
        correctAnswer = 2,
        explanation = "Der Mythos, die Chinesische Mauer sei vom Weltraum aus sichtbar, ist widerlegt. Sie ist zwar sehr lang (ueber 21.000 km), aber mit etwa 5–8 Metern Breite zu schmal, um mit blossem Auge erkannt zu werden.",
        difficulty = 2,
        funFact = "Chinesische Astronauten haben den Mythos persoenlich widerlegt. Yang Liwei, der erste Chinese im Weltraum (2003), bestaetigte, dass er die Mauer nicht sehen konnte."
    ),

    // Question 39 — Gemaelde: Botticelli
    Question(
        categoryId = 11,
        questionText = "Welches beruehmt Gemaelde von Sandro Botticelli zeigt die Geburt einer Goettin aus dem Meeresschaum?",
        answerA = "Primavera",
        answerB = "Die Geburt der Venus",
        answerC = "Die Sixtinische Madonna",
        answerD = "Die Verkuendigung",
        correctAnswer = 1,
        explanation = "\"Die Geburt der Venus\" (ca. 1485) von Sandro Botticelli zeigt Venus, die auf einer Muschel dem Meer entsteigt. Es ist eines der ikonischsten Werke der Fruehrenaissance und haengt in den Uffizien in Florenz.",
        difficulty = 2,
        funFact = "Das Modell fuer Botticellis Venus soll Simonetta Vespucci gewesen sein, eine beruehmte Florentiner Schoenheit und angebliche Geliebte von Lorenzo de' Medicis Bruder Giuliano. Sie starb jung mit nur 23 Jahren."
    ),

    // Question 40 — Architektur: Opernsydney
    Question(
        categoryId = 11,
        questionText = "Von welchem Architekten wurde das Opernhaus von Sydney entworfen?",
        answerA = "Renzo Piano",
        answerB = "Frank Gehry",
        answerC = "Joern Utzon",
        answerD = "Richard Rogers",
        correctAnswer = 2,
        explanation = "Das Opernhaus von Sydney wurde vom daenischen Architekten Joern Utzon entworfen und 1973 eroeffnet. Es gilt als eines der beruehmdesten Bauwerke der Welt und steht auf der UNESCO-Welterbeliste.",
        difficulty = 2,
        funFact = "Joern Utzon gewann den Wettbewerb 1957, musste das Projekt aber 1966 aufgrund von Streitigkeiten verlassen — lange bevor das Gebaeude fertig war. Er sah das Opernhaus nie fertiggestellt."
    ),

    // Question 41 — Gemaelde: Kandinsky
    Question(
        categoryId = 11,
        questionText = "Welcher Kuenstler gilt als Pionier der abstrakten Malerei?",
        answerA = "Pablo Picasso",
        answerB = "Paul Klee",
        answerC = "Wassily Kandinsky",
        answerD = "Henri Matisse",
        correctAnswer = 2,
        explanation = "Wassily Kandinsky gilt als einer der Begruender der abstrakten Malerei. Sein erstes abstraktes Aquarell von 1910 oder 1913 (Datum ist umstritten) ist als eines der ersten rein abstrakten Werke in der Kunstgeschichte bekannt.",
        difficulty = 2,
        funFact = "Kandinsky hatte Synasthesie: Er konnte Farben hoeren und Musik sehen. Dieses neurologische Phaenomen beinflusste stark seine Theorie, dass Farben und Formen eigene innere Klangwelten haben."
    ),

    // Question 42 — Bauwerk: Louvre Pyramide
    Question(
        categoryId = 11,
        questionText = "Wer entwarf die glaeserne Pyramide, die als Eingang des Louvre in Paris dient?",
        answerA = "Renzo Piano",
        answerB = "I. M. Pei",
        answerC = "Jean Nouvel",
        answerD = "Norman Foster",
        correctAnswer = 1,
        explanation = "Die glaeserne Louvre-Pyramide wurde vom amerikanisch-chinesischen Architekten I. M. Pei entworfen und 1989 eroeffnet. Sie war anfangs sehr umstritten, gilt heute aber als gelungene Verbindung von Antike und Moderne.",
        difficulty = 2,
        funFact = "Als Praesident Mitterrand die Louvre-Pyramide in Auftrag gab, protestierten viele Franzosen heftig. Eine Pariser Zeitung schrieb: \"Der Louvre ist gestuempert worden.\" Heute ist die Pyramide ein Symbol von Paris."
    ),

    // Question 43 — Kunsttechnik: Radierung
    Question(
        categoryId = 11,
        questionText = "Was ist eine Radierung in der Druckkunst?",
        answerA = "Eine Technik, bei der Motive mit Saeure in eine Metallplatte geaetzt werden",
        answerB = "Eine Technik, bei der Tinte direkt auf Papier aufgetragen wird",
        answerC = "Eine Technik, bei der Holz geschnitzt und eingefaerbt wird",
        answerD = "Eine Technik, bei der Schablonen auf Leinwand gedruckt werden",
        correctAnswer = 0,
        explanation = "Bei der Radierung (Aetzung) wird eine Metallplatte mit einer Schutzschicht bedeckt, dann werden Linien hineingeritzt und die Platte in Saeure gelegt. Diese aetzt nur die freiliegenden Stellen. Rembrandt war ein Meister der Radierung.",
        difficulty = 2,
        funFact = "Rembrandt schuf ueber 300 Radierungen und gilt als groesster Radierer der Kunstgeschichte. Er experimentierte mit verschiedenen Drucker-Methoden und erzielte einzigartige Hell-Dunkel-Effekte."
    ),

    // Question 44 — Architektur: Burj Khalifa
    Question(
        categoryId = 11,
        questionText = "In welcher Stadt steht der Burj Khalifa, das derzeitig hoechste Gebaeude der Welt?",
        answerA = "Abu Dhabi",
        answerB = "Riad",
        answerC = "Dubai",
        answerD = "Doha",
        correctAnswer = 2,
        explanation = "Der Burj Khalifa steht in Dubai (Vereinigte Arabische Emirate) und wurde 2010 eroeffnet. Er ist 828 Meter hoch und hat 163 Stockwerke. Entworfen wurde er von Adrian Smith (Skidmore, Owings & Merrill).",
        difficulty = 2,
        funFact = "Auf der Spitze des Burj Khalifa ist es 6 Grad kuehler als am Boden. Wer am Boden Silvester feiert und danach nach oben faehrt, kann die gleichen Feuerwerke ein zweites Mal erleben, weil die Nacht oben spaeter eintrifft."
    ),

    // Question 45 — Gemaelde: Dritte Mai
    Question(
        categoryId = 11,
        questionText = "Was zeigt Goyas Gemaelde \"El tres de mayo de 1808\" (Der dritte Mai)?",
        answerA = "Eine spanische Volksfeier zu Ehren des Koenigshauses",
        answerB = "Die Erschiessung spanischer Aufstaendischer durch franzoesische Soldaten",
        answerC = "Die Abreise Napoleons aus Spanien",
        answerD = "Den Sieg der spanischen Armee bei Salamanca",
        correctAnswer = 1,
        explanation = "Francisco de Goyas \"El tres de mayo de 1808\" (1814) zeigt die Erschiessung spanischer Aufstaendischer durch Napoleons Soldaten. Es gilt als eines der ersten modernen Antikriegsbilder und beeinflusste spaetere Werke wie Picassos \"Guernica\".",
        difficulty = 2,
        funFact = "Goya malte das Bild erst sechs Jahre nach den Ereignissen, im Jahr 1814, also nach Napoleons Niederlage. Heute haengt es im Museo del Prado in Madrid."
    ),

    // Question 46 — Architektur: Koelner Dom
    Question(
        categoryId = 11,
        questionText = "Wie lange dauerte der Bau des Koelner Doms insgesamt?",
        answerA = "Etwa 100 Jahre",
        answerB = "Etwa 200 Jahre",
        answerC = "Etwa 400 Jahre",
        answerD = "Ueber 600 Jahre",
        correctAnswer = 3,
        explanation = "Der Grundstein des Koelner Doms wurde 1248 gelegt, doch der Bau wurde 1560 unterbrochen und erst 1842–1880 abgeschlossen — insgesamt ueber 630 Jahre Bauzeit. Von 1880 bis 1884 war er das hoechste Gebaeude der Welt.",
        difficulty = 2,
        funFact = "Zum Zeitpunkt seiner Fertigstellung 1880 war der Koelner Dom das hoechste Gebaeude der Welt (157 Meter). Nur vier Jahre spaeter wurde er vom Ulmer Muenster uebertroffen."
    ),

    // Question 47 — Gemaelde: Letzte Abendmahl
    Question(
        categoryId = 11,
        questionText = "Wo befindet sich da Vincis beruehmt Wandgemaelde \"Das letzte Abendmahl\"?",
        answerA = "In der Sixtinischen Kapelle in Rom",
        answerB = "Im Refektorium des Klosters Santa Maria delle Grazie in Mailand",
        answerC = "In der Uffizien-Galerie in Florenz",
        answerD = "Im Louvre in Paris",
        correctAnswer = 1,
        explanation = "Das \"Letzte Abendmahl\" von Leonardo da Vinci (ca. 1495–1498) befindet sich an der Nordwand des Refektoriums (Speisesaal) im Kloster Santa Maria delle Grazie in Mailand.",
        difficulty = 2,
        funFact = "Da Vinci verwendete beim \"Letzten Abendmahl\" keine Freskomalerei, sondern Tempera auf trockenem Putz. Das erwies sich als Fehler: Das Gemaelde begann sich schon zu Lebzeiten da Vincis abzubloeckern."
    ),

    // Question 48 — Kunstepoche: Pop Art
    Question(
        categoryId = 11,
        questionText = "Welcher Kuenstler der Pop Art ist beruehmt fuer seine Siebdrucke von Marilyn Monroe und Campbell's Suppendosen?",
        answerA = "Roy Lichtenstein",
        answerB = "Jasper Johns",
        answerC = "Andy Warhol",
        answerD = "Robert Rauschenberg",
        correctAnswer = 2,
        explanation = "Andy Warhol ist der bekannteste Vertreter der Pop Art. Er reproduzierte Alltagsprodukte und Prominente (Marilyn Monroe, Mao, Campbell's Suppe) in bunten Siebdrucken als Kommentar auf Massenkonsum und Mediengesellschaft.",
        difficulty = 2,
        funFact = "Warhols beruehmtes Studio in New York hiess \"The Factory\". Er beschaeftigte eine Armee von Assistenten, die Siebdrucke nach seinen Anweisungen herstellten — ein Verweis auf industrielle Produktion als Kunst."
    ),

    // Question 49 — Architektur: Petersdom
    Question(
        categoryId = 11,
        questionText = "Wer entwarf die Kuppel des Petersdoms in Rom?",
        answerA = "Raffael",
        answerB = "Leonardo da Vinci",
        answerC = "Bernini",
        answerD = "Michelangelo",
        correctAnswer = 3,
        explanation = "Die Kuppel des Petersdoms in Rom wurde von Michelangelo entworfen, auch wenn er ihren Bau 1564 nicht mehr erlebte. Vollendung durch Giacomo della Porta 1590. Mit 137 m Hoehe ist sie eine der groessten Kuppeln der Welt.",
        difficulty = 2,
        funFact = "Als Michelangelo im Alter von fast 72 Jahren Chefarchitekt des Petersdoms wurde, soll er gesagt haben, er arbeite nur \"zur Ehre Gottes\" und gegen seinen Willen. Er lehnte jedes Honorar ab."
    ),

    // Question 50 — Gemaelde: Turner
    Question(
        categoryId = 11,
        questionText = "Welcher britische Maler ist bekannt fuer seine atmosphaerischen Darstellungen von Licht, Nebel und Meer im 19. Jahrhundert?",
        answerA = "John Constable",
        answerB = "William Turner",
        answerC = "George Stubbs",
        answerD = "Thomas Gainsborough",
        correctAnswer = 1,
        explanation = "Joseph Mallord William Turner (1775–1851) malte atmosphaerische Landschaften und Seestuecke, in denen Licht, Nebel und Wetter die Hauptrolle spielen. Er gilt als Vorlaeufer des Impressionismus.",
        difficulty = 2,
        funFact = "Turner soll sich bei einem Schneesturm an den Mast eines Schiffes gebunden haben, um das Erlebnis direkt fuer sein Gemaelde \"Schneegewoelk und Sturm\" zu erleben. Ob die Geschichte wahr ist, ist ungewiss."
    )

)
