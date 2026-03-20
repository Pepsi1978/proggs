package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsExpert4(): List<Question> = listOf(

    // --- ARCHITEKTUR & BAUWERKE (50) ---

    // Frage 1 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welcher Architekt entwarf den Burj Khalifa in Dubai, der mit 828 Metern das höchste Gebäude der Welt ist?",
        answerA = "Adrian Smith (SOM)",
        answerB = "Norman Foster",
        answerC = "Renzo Piano",
        answerD = "Cesar Pelli",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Adrian Smith vom Büro Skidmore, Owings & Merrill (SOM) entwarf den Burj Khalifa, der 2010 fertiggestellt wurde. Das Gebäude hat 163 Stockwerke und eine Gesamthöhe von 828 Metern.",
        funFact = "Der Burj Khalifa ist so hoch, dass man vom Erdgeschoss aus seinen Fuß auf der obersten bewohnbaren Etage einige Minuten früher sieht als vom Boden aus — das Sonnenuntergangsdatum ist auf beiden Ebenen verschieden."
    ),

    // Frage 2 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Welche Brücke hält mit 164 Kilometern den Weltrekord als längste Brücke der Welt?",
        answerA = "Hangzhou-Bucht-Brücke, China",
        answerB = "Danyang-Kunshan-Großbrücke, China",
        answerC = "Lake Pontchartrain Causeway, USA",
        answerD = "Bang Na Expressway, Thailand",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Die Danyang-Kunshan-Großbrücke in China ist seit ihrer Fertigstellung 2011 die längste Brücke der Welt. Sie ist Teil der Hochgeschwindigkeitsbahnstrecke zwischen Peking und Shanghai.",
        funFact = null
    ),

    // Frage 3 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Für welche architektonische Leistung ist das Viadukt de Millau in Frankreich besonders bekannt?",
        answerA = "Es ist die längste Hängebrücke Europas mit einer Spannweite von 2.460 Metern",
        answerB = "Es wurde in Rekordzeit von nur 18 Monaten fertiggestellt",
        answerC = "Ein Pfeiler ragt mit 343 Metern höher als der Eiffelturm",
        answerD = "Es ist die erste Brücke Europas aus vollständig recyceltem Stahl",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Das Viadukt de Millau, entworfen von Norman Foster und Michel Virlogeux, hat sieben Pfeiler. Der höchste misst 343 Meter und überragt damit den Eiffelturm (300 Meter ohne Antenne). Die Fahrbahn selbst liegt 270 Meter über dem Fluss Tarn.",
        funFact = "Bei Nebel ragen die Pylone des Millau-Viadukts über die Wolken — Autofahrer fahren buchstäblich durch die Wolken und sehen die Pfeiler erst wieder über sich."
    ),

    // Frage 4 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was ist das architektonische Besondere an der Berliner Philharmonie von Hans Scharoun (1963)?",
        answerA = "Sie wurde als erster Konzertsaal Europas vollständig aus Stahlbeton gebaut",
        answerB = "Der Zuschauerraum ist oval und hat keine parallelen Wände",
        answerC = "Das Dach ist eine freischwingende Membranstruktur ohne Stützen",
        answerD = "Das Orchester sitzt in der Mitte, umgeben von Zuschauerrängen auf allen Seiten",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Scharouns revolutionäres Konzept der 'Weinbergterrassen' platziert das Orchester im Zentrum des Raums. Die Zuhörer sitzen in Terrassen rund um das Podium. Dieses Prinzip wurde weltweites Vorbild für modernen Konzertsaalbau.",
        funFact = null
    ),

    // Frage 5 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welcher Architekt prägte den Begriff 'Less is more' und entwarf das Seagram Building in New York (1958)?",
        answerA = "Ludwig Mies van der Rohe",
        answerB = "Le Corbusier",
        answerC = "Walter Gropius",
        answerD = "Philip Johnson",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Mies van der Rohes Ausspruch 'Less is more' steht für seinen reduktionistischen Minimalismus. Das Seagram Building ist ein Meilenstein der modernen Hochhausarchitektur: zurückgesetzt von der Straße, mit Bronzeverkleidung und freiliegendem Tragwerk.",
        funFact = "Mies van der Rohes Gegner Philip Johnson (der co-entwarf das Seagram Building) variierte später den Spruch zu 'Less is a bore' — als er seinen eigenen Stil zum Postmodernismus wechselte."
    ),

    // Frage 6 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "In welchem Jahr wurde die Golden Gate Bridge in San Francisco eröffnet, und wie lange war sie damals die Brücke mit der größten Hauptspannweite der Welt?",
        answerA = "1929 — 15 Jahre",
        answerB = "1937 — 27 Jahre",
        answerC = "1945 — 10 Jahre",
        answerD = "1933 — 20 Jahre",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Die Golden Gate Bridge wurde am 27. Mai 1937 eröffnet. Mit 1.280 Metern Hauptspannweite hielt sie den Weltrekord bis 1964, als die Verrazano-Narrows Bridge in New York sie mit 1.298 Metern überbot — also 27 Jahre.",
        funFact = "Die charakteristische orange Farbe heißt offiziell 'International Orange' und wurde ursprünglich als Grundierungsfarbe aufgetragen. Ingenieur Joseph Strauss ließ sie als Endanstrich beibehalten, da sie die Sichtbarkeit bei Nebel erhöht."
    ),

    // Frage 7 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was bezeichnet der 'Bilbao-Effekt' in der Stadtplanung und Architektur?",
        answerA = "Den Einsatz von Titan als Fassadenmaterial bei Museumsgebäuden",
        answerB = "Die dekonstruktivistische Formensprache nach Frank Gehry",
        answerC = "Die wirtschaftliche Revitalisierung einer Stadt durch ein ikonisches Architekturprojekt",
        answerD = "Die Verlagerung von Kulturinstitutionen in Randgebiete großer Städte",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Nach der Eröffnung des Guggenheim-Museums Bilbao (1997, Architekt Frank Gehry) erlebte die ehemalige Industriestadt Bilbao einen beispiellosen Tourismusboom. Seitdem beschreibt der 'Bilbao-Effekt' wie ein Prestigebauwerk eine ganze Region wirtschaftlich transformieren kann.",
        funFact = null
    ),

    // Frage 8 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Welches Bauprinzip machte die gotische Kathedrale mit ihren großen Fensterflächen erst möglich?",
        answerA = "Die Erfindung von Stahlbeton durch mittelalterliche Baumeister",
        answerB = "Die Verwendung von Ziegeln statt Naturstein für leichteres Mauerwerk",
        answerC = "Das Tonnengewölbe, das horizontalen Schub vollständig aufnimmt",
        answerD = "Das Rippengewölbe mit Strebebögen, das den Gewölbeschub nach außen ableitet",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Das gotische Strebewerk aus Rippengewölben, Strebebögen und Strebepfeilern leitet den horizontalen Gewölbeschub an die Außenwand ab. Dadurch konnten die Wände ausgedünnt und mit riesigen Glasfenstern gefüllt werden — optisch und statisch revolutionär.",
        funFact = "Die Kathedrale von Chartres besitzt über 170 Buntglasfenster mit einer Gesamtfläche von rund 2.600 Quadratmetern — und das alles erst möglich durch das gotische Strebesystem."
    ),

    // Frage 9 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welcher Architekt war der erste Deutsche, der den Pritzker-Preis erhielt, und in welchem Jahr?",
        answerA = "Gottfried Böhm, 1986",
        answerB = "Frei Otto, 1986",
        answerC = "Hans Hollein, 1985",
        answerD = "Günter Behnisch, 1990",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Gottfried Böhm erhielt 1986 als erster Deutscher den Pritzker-Preis, den sogenannten 'Nobelpreis der Architektur'. Er ist bekannt für Sakralbauten wie die Wallfahrtskirche Neviges (1968) und kombinierte Beton mit expressiver Form.",
        funFact = "Gottfried Böhm entstammt einer Architektendynastie: Sein Vater Dominikus Böhm, seine Söhne Stephan und Peter Böhm sowie sein Enkel sind ebenfalls renommierte Architekten — vier Generationen."
    ),

    // Frage 10 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Welches Strukturprinzip wendet Frei Otto bei seinen Zeltdachkonstruktionen (z.B. Olympiastadion München 1972) an?",
        answerA = "Druckaktive Bogenkonstruktionen aus vorgespanntem Beton",
        answerB = "Zugaktive Seilnetzstrukturen, die nur unter Zugspannung stabil sind",
        answerC = "Pneumatische Membranen, die durch Luftdruck ihre Form halten",
        answerD = "Freitragende Raumfachwerke aus Aluminiumrohren",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Frei Ottos Pionierleistung waren zugaktive Konstruktionen: Seilnetze und Membranen, die ihre Stabilität ausschließlich aus Zugkräften beziehen. Das Olympiadach München (1972) ist ein Meisterwerk dieser leichten Netzarchitektur.",
        funFact = "Frei Otto erhielt den Pritzker-Preis 2015 posthum — er starb am 9. März 2015, drei Tage bevor die Bekanntgabe offiziell gemacht wurde. Er hatte bereits vorab davon erfahren."
    ),

    // Frage 11 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welches architektonische Merkmal kennzeichnet den romanischen Baustil im Unterschied zur Gotik?",
        answerA = "Spitzbögen und skelettartige Wände mit großen Fensterflächen",
        answerB = "Verwendung von Stahlträgern in der Deckenkonstruktion",
        answerC = "Massive Mauern, kleine Rundfenster und Halbkreisbögen",
        answerD = "Flache Dächer und symmetrische Säulenhallen",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Der romanische Stil (ca. 10.–12. Jh.) ist geprägt durch dicke, massive Mauern, kleine Fensteröffnungen mit Halbkreisbögen und Rundfenster. Im Gegensatz zur Gotik dominiert das Gefühl von Schwere und Erdverbundenheit.",
        funFact = null
    ),

    // Frage 12 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was ist eine 'Kragkonstruktion' (Cantilever) in der Architektur?",
        answerA = "Eine Balkendecke, die auf beiden Seiten auf Wänden aufliegt",
        answerB = "Ein Fachwerk aus diagonalen Streben für Brücken",
        answerC = "Eine Bogenkonstruktion, die Lasten über Druck in Fundamente ableitet",
        answerD = "Ein Bauteil, das nur auf einer Seite eingespannt ist und auf der anderen frei auskragt",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Bei Kragkonstruktionen (Cantilever) ist ein Bauteil nur an einem Ende befestigt und ragt frei aus. Frank Lloyd Wrights 'Fallingwater' (1937) ist das berühmteste Beispiel: Die Terrassen ragen weit über den Wasserfall hinaus.",
        funFact = "Fallingwater in Bear Run, Pennsylvania, wurde von der American Institute of Architects als 'größtes amerikanisches Architekturwerk des 20. Jahrhunderts' ausgezeichnet."
    ),

    // Frage 13 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welcher Architekt entwarf das Centre Georges Pompidou in Paris (1977) und gilt als Mitbegründer des 'High-Tech-Stils'?",
        answerA = "Renzo Piano und Richard Rogers",
        answerB = "Norman Foster und Arata Isozaki",
        answerC = "Jean Nouvel und Christian de Portzamparc",
        answerD = "Zaha Hadid und Rem Koolhaas",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Renzo Piano und Richard Rogers entwarfen gemeinsam das Centre Pompidou, bei dem alle technischen Einbauten (Lüftung, Wasserleitungen, Rolltreppen) nach außen verlegt und farbig markiert wurden. Dieser 'Inside-Out'-Ansatz begründete den High-Tech-Stil.",
        funFact = "Die Farbkodierung des Centre Pompidou ist funktional: Blau steht für Klima und Luft, Grün für Wasser, Gelb für Elektrik und Rot für Aufzüge und Treppen."
    ),

    // Frage 14 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was versteht man in der Architektur unter dem Begriff 'Genius Loci'?",
        answerA = "Die mathematische Berechnung des optimalen Grundrisses für ein Gebäude",
        answerB = "Den spezifischen Geist oder die besondere Atmosphäre eines Ortes, die die Architektur widerspiegeln soll",
        answerC = "Das Prinzip der minimalen Eingriffe bei der Restaurierung historischer Gebäude",
        answerD = "Die Lehre von der optimalen Gebäudeausrichtung nach Sonnenstand",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Genius Loci (lat. 'Schutzgeist des Ortes') bezeichnet die unverwechselbare Identität eines Ortes. In der Architekturtheorie (besonders bei Norberg-Schulz) meint es das Wesenhafte eines Ortes, das gute Architektur aufgreifen und verstärken soll.",
        funFact = null
    ),

    // Frage 15 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Für welche Stadt plante Le Corbusier eine völlig neue Hauptstadt auf dem Reißbrett, die ab 1953 gebaut wurde?",
        answerA = "Brasília, Brasilien",
        answerB = "Islamabad, Pakistan",
        answerC = "Chandigarh, Indien",
        answerD = "Nairobi, Kenia",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Le Corbusier entwarf Chandigarh als neue Hauptstadt des indischen Bundesstaates Punjab nach der Teilung Indiens. Er plante den Kapitolkomplex mit Regierungsgebäuden, Gerichten und dem Palais de l'Assemblée.",
        funFact = "Le Corbusier selbst besuchte Chandigarh nur selten — ein Großteil der Ausführung lag bei seinem Cousin Pierre Jeanneret und den Architekten Maxwell Fry und Jane Drew."
    ),

    // Frage 16 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Welches Prinzip begründete der Architekt und Theoretiker Vitruv in seiner Abhandlung 'De architectura' (ca. 25 v. Chr.) als drei Grundsäulen der Architektur?",
        answerA = "Schönheit, Ordnung und Symmetrie (Venustas, Ordo, Symmetria)",
        answerB = "Proportion, Harmonie und Licht (Proportio, Harmonia, Lux)",
        answerC = "Konstruktion, Funktion und Ästhetik (Constructio, Utilitas, Aesthetica)",
        answerD = "Festigkeit, Nützlichkeit und Anmut (Firmitas, Utilitas, Venustas)",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Vitruvs drei Grundprinzipien 'Firmitas' (Standfestigkeit), 'Utilitas' (Nützlichkeit) und 'Venustas' (Schönheit) prägten die Architekturtheorie für zwei Jahrtausende. Sie gelten bis heute als Kurzformel für ganzheitlich gutes Bauen.",
        funFact = "De architectura ist das einzige vollständig überlieferte Werk über antike Architektur und war in der Renaissance die wichtigste Quelle für Architekten wie Bramante, Michelangelo und Palladio."
    ),

    // Frage 17 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welches Gebäude gilt als das erste große Beispiel des Expressionismus in der deutschen Architektur und wurde 1921 von Erich Mendelsohn entworfen?",
        answerA = "Einsteinturm in Potsdam",
        answerB = "Chilehaus in Hamburg",
        answerC = "Bauhaus Dessau",
        answerD = "AEG-Turbinenfabrik Berlin",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Erich Mendelsohns Einsteinturm in Potsdam (1921) ist das Wahrzeichen des Architekturexpressionismus. Die geschwungenen, plastischen Betonformen sollten Einsteins Relativitätstheorie symbolisch verkörpern — obwohl das Gebäude letztlich größtenteils aus Ziegel besteht.",
        funFact = "Als Albert Einstein den Turm besichtigte, fragte jemand nach seinem Eindruck. Einstein antwortete nur: 'Organisch.' — Was er damit meinte, blieb sein Geheimnis."
    ),

    // Frage 18 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was ist der Merdeka 118 Tower in Kuala Lumpur, und welchen Rang belegt er unter den höchsten Gebäuden der Welt?",
        answerA = "Das höchste Gebäude der Welt mit 700 Metern, Platz 1",
        answerB = "Das zweithöchste Gebäude der Welt mit 679 Metern",
        answerC = "Das dritthöchste Gebäude der Welt mit 632 Metern",
        answerD = "Das höchste Gebäude Südostasiens mit 558 Metern, Platz 5",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Der 2023 fertiggestellte Merdeka 118 Tower in Kuala Lumpur ist mit 678,9 Metern das zweithöchste Gebäude der Welt — nach dem Burj Khalifa (828 m). Er hat 118 Stockwerke und ist nach dem Jahr 1957 benannt, in dem Malaysia seine Unabhängigkeit erlangte.",
        funFact = null
    ),

    // Frage 19 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welcher Architekt entwarf das Jüdische Museum Berlin (2001) mit seiner zerbrochenen Zick-Zack-Form?",
        answerA = "Daniel Libeskind — Entwurf: 'Between the Lines'",
        answerB = "Frank Gehry — Entwurf: 'Shattered Memory'",
        answerC = "Daniel Libeskind — Entwurf: 'Between the Lines'",
        answerD = "Peter Zumthor — Entwurf: 'Leere und Erinnerung'",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Daniel Libeskind gewann 1989 den Wettbewerb für das Jüdische Museum Berlin. Sein Konzept 'Between the Lines' beschreibt zwei Linien: eine gerade (Geschichte der Deutschen) und eine gebrochene (Geschichte der Juden). Die Leerräume ('Voids') symbolisieren das Unsagbare.",
        funFact = "Das Jüdische Museum Berlin war zwei Jahre lang geöffnet, bevor irgendwelche Ausstellungsstücke einzogen — allein die leeren Räume zogen hunderttausende Besucher an."
    ),

    // Frage 20 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was ist das 'Goldene Schnitt'-Verhältnis (Φ, Phi), und welcher berühmte Renaissancearchitekt nutzte es systematisch?",
        answerA = "1 : 1,414 (Wurzel 2) — Filippo Brunelleschi",
        answerB = "1 : 1,333 (4:3) — Michelozzo di Bartolommeo",
        answerC = "1 : 1,618 — Leon Battista Alberti",
        answerD = "1 : 1,618 — Andrea Palladio",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Der Goldene Schnitt (Φ ≈ 1,618) gilt als ästhetisch ideales Verhältnis. Andrea Palladio (1508–1580) integrierte ihn systematisch in seine Villenarchitektur und beeinflusste damit Jahrhunderte westlicher Baukunst — bis hin zu Thomas Jeffersons Monticello.",
        funFact = "Le Corbusier entwickelte seinen 'Modulor' als anthropometrisches Maßsystem, das auf dem Goldenen Schnitt und menschlichen Körperproportionen basiert und alle seine späten Gebäude strukturierte."
    ),

    // Frage 21 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welche architektonische Besonderheit macht die Hagia Sophia in Istanbul zu einem Meisterwerk der byzantinischen Architektur?",
        answerA = "Ihre Kuppel wirkt schwebend, weil sie über einem Kranz von 40 Fenstern auf Pendentifs ruht",
        answerB = "Sie wurde ohne Mörtel aus exakt zugehauenen Marmorblöcken gefügt",
        answerC = "Das Mauerwerk ist mit Hohlziegeln gefüllt, die das Gewicht um ein Drittel reduzieren",
        answerD = "Die Kuppel ist eine geodätische Holzkonstruktion, die mit Blei gedeckt ist",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Die Kuppel der Hagia Sofia (537 n. Chr.) ruht auf vier Pendentifs — sphärischen Dreiecken, die einen runden Kuppelring in ein Quadrat überführen. Darunter sitzt ein Kranz von 40 Fenstern, sodass die Kuppel optisch zu schweben scheint.",
        funFact = "Prokopios von Caesarea beschrieb, die Hagia Sophia-Kuppel scheine 'an einem goldenen Faden vom Himmel aufgehängt' zu sein — ein Effekt, den die Lichtarchitektur bis heute bewirkt."
    ),

    // Frage 22 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Welcher Architekt entwarf das Opernhaus Sydney (1973) und gewann dafür 2003 den Pritzker-Preis?",
        answerA = "Glenn Murcutt",
        answerB = "Jørn Utzon",
        answerC = "Harry Seidler",
        answerD = "Renzo Piano",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Der dänische Architekt Jørn Utzon gewann 1957 den internationalen Wettbewerb für das Opernhaus Sydney. Die charakteristischen Schalen des Daches wurden durch ein System von Segmenten einer Kugel realisiert. 2003 erhielt Utzon den Pritzker-Preis.",
        funFact = "Utzon verließ das Projekt 1966 nach Streitigkeiten mit der australischen Regierung und betrat das fertige Gebäude zu seinen Lebzeiten nie wieder — trotz späterem Ehrenbürgerrecht Sydneys."
    ),

    // Frage 23 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Was beschreibt der architekturtheoretische Begriff 'Parametrismus' (Patrik Schumacher, Zaha Hadid Architects)?",
        answerA = "Die Verwendung von vorparametrierten Fertigteilsystemen im industriellen Wohnungsbau",
        answerB = "Die Berechnung statischer Parameter für Brücken- und Hochhauskonstruktionen",
        answerC = "Ein computerbasierter Entwurfsstil, bei dem alle Elemente durch algorithmische Parameter miteinander verknüpft sind und organische Formen erzeugen",
        answerD = "Die normierte Bemaßung von Architekturzeichnungen nach ISO-Standard",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Parametrismus ist ein Architekturstil, der computergestützte Algorithmen nutzt, um Gebäude als komplexe, organisch fließende Systeme zu entwerfen. Zaha Hadids MAXXI in Rom (2010) und das BMW Werk Leipzig sind Paradebeispiele.",
        funFact = null
    ),

    // Frage 24 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Welches Bauwerk war der erste Wolkenkratzer der Welt und verwendete erstmals eine Stahlskelettstruktur als tragendes Element?",
        answerA = "Flatiron Building, New York (1902)",
        answerB = "Wainwright Building, St. Louis (1891)",
        answerC = "Tribune Tower, Chicago (1925)",
        answerD = "Home Insurance Building, Chicago (1885)",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Das Home Insurance Building in Chicago (1885, Architekt William Le Baron Jenney) gilt als erstes Hochhaus mit tragendem Stahlskelett statt Massivmauern. Mit 10 Stockwerken und 42 Metern begründete es die Ära der modernen Wolkenkratzer.",
        funFact = "Chicago wurde zur Wiege des Wolkenkratzers, weil der große Brand von 1871 einen rasanten Wiederaufbau erzwang — Boden war knapp und teuer, also baute man nach oben."
    ),

    // Frage 25 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Was ist das Charaktermerkmal des 'Brutalismus' in der Architektur?",
        answerA = "Sichtbarer, unverkleideter Beton (béton brut) als ästhetisches Gestaltungsmittel",
        answerB = "Die bewusst rohe, aggressive Ästhetik mit Stacheldraht und Sicherheitselementen",
        answerC = "Massive Natursteinmauern ohne Putz in Anlehnung an mittelalterliche Burgen",
        answerD = "Schwarze Stahlfassaden mit minimalen Fensterflächen",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Brutalismus (frz. 'béton brut' = roher Beton) bezeichnet einen Stil der 1950er–1970er Jahre, der Sichtbeton als ehrliches Ausdrucksmittel feiert. Der Name bezieht sich nicht auf 'brutal' im negativen Sinne, sondern auf die Sichtbarkeit des Rohmaterials.",
        funFact = "Le Corbusiers Unité d'Habitation in Marseille (1952) gilt als Ikone des Brutalismus und inspirierte weltweit Sozialwohnungsbauten — mit gemischten Ergebnissen für die Bewohner."
    ),

    // Frage 26 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Wer entwarf den Louvre-Glaspyramiden-Eingang (1989) und welchem Stil wird er zugeordnet?",
        answerA = "Renzo Piano — High-Tech-Architektur",
        answerB = "I. M. Pei — Geometrischer Modernismus",
        answerC = "Jean Nouvel — Dekonstruktivismus",
        answerD = "Ieoh Ming Pei — Postmoderne",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "I. M. Pei (Ieoh Ming Pei) entwarf die gläserne Pyramide mit 673 Glasrauten als Haupteingang des Louvre. Der Eingang wurde 1989 eröffnet und gehört heute zu den bekanntesten Denkmälern Frankreichs — obwohl er anfangs heftig kritisiert wurde.",
        funFact = "Die Zahl der Glasscheiben in der Louvre-Pyramide wurde jahrzehntelang fälschlicherweise mit 666 angegeben — Umberto Eco und Dan Brown übernahmen dies in Büchern. Tatsächlich sind es 673 Rauten."
    ),

    // Frage 27 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welches architektonische Konzept verfolgt das Bauhaus, gegründet von Walter Gropius 1919 in Weimar?",
        answerA = "Die Rückkehr zur handwerklichen Tradition des Mittelalters ohne Maschineneinsatz",
        answerB = "Die reine Kunst ohne funktionale Aspekte als höchste Ausdrucksform",
        answerC = "Die Vereinigung von Kunst, Handwerk und Industrie zur Gestaltung aller Lebensbereiche",
        answerD = "Die Förderung regionaler Baustile gegen internationale Einheitsarchitektur",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Das Bauhaus verfolgte das Ziel, Kunst und Handwerk zu verschmelzen und industrielle Fertigungsmethoden für schöne, funktionale Alltagsgegenstände zu nutzen. Gropius' Motto: 'Die Einheit aller gestaltenden Tätigkeit.' Das Dessauer Bauhausgebäude (1926) ist UNESCO-Weltkulturerbe.",
        funFact = null
    ),

    // Frage 28 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was ist das Alleinstellungsmerkmal der Beipanjiang-Brücke in China, die 2016 fertiggestellt wurde?",
        answerA = "Sie ist die erste Hängebrücke weltweit mit Glasfahrbahn auf allen vier Spuren",
        answerB = "Sie überspannt die größte Wassertiefe aller Brücken (380 Meter)",
        answerC = "Sie hat die längste einzelne Spannweite aller Schrägkabelbrücken (1.520 Meter)",
        answerD = "Sie ist mit 565 Metern die höchste Brücke der Welt über dem Bodenniveau",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Die Beipanjiang-Brücke in Guizhou, China, hält mit 565 Metern über dem Tal des Beipan-Flusses den Weltrekord als höchste Brücke der Welt. Zum Vergleich: Der Eiffelturm ist 330 Meter hoch.",
        funFact = "Bei Sturm kann die Fahrbahn der Beipanjiang-Brücke um mehr als einen Meter schwingen — die Schwingungsdämpfer sind ein eigenes Ingenieursmeisterwerk."
    ),

    // Frage 29 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welche Technik der Vorfertigung revolutionierte Le Corbusiers sozialen Wohnungsbau und basierte auf dem Modulor-System?",
        answerA = "Dom-ino-System: freistehende Stahlbetonplatten auf Pilotis als universelles Grundskelett",
        answerB = "Curtain-Wall-System: vorgehängte Glasfassaden ohne tragende Funktion",
        answerC = "Balloon-Frame-Methode: genormte Holzstäbe als Leichtbauskelett",
        answerD = "Flying Buttress-System: außenliegende Stützbogen für Hochhausfassaden",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Le Corbusiers Dom-ino-System (1914–1915) besteht aus sechs Stützen, drei Deckenplatten und einer Treppe — ein frei kombinierbares Skelett, das beliebige Grundrisse erlaubt. 'Dom-ino' steht für 'domus' (Haus) und 'innovation'.",
        funFact = "Das Dom-ino-System war Le Corbusiers Antwort auf den Ersten Weltkrieg: Er wollte zerstörte Städte schnell und günstig wiederaufbauen. Ironisch setzte das System die Grundlage für den Betonmonotonismus der Nachkriegsjahrzehnte."
    ),

    // Frage 30 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Welches Bauwerk gilt als das bedeutendste Beispiel der Mogul-Architektur und wurde zwischen 1632 und 1653 in Agra, Indien, erbaut?",
        answerA = "Rotes Fort (Lal Qila)",
        answerB = "Taj Mahal",
        answerC = "Fatehpur Sikri",
        answerD = "Qutb Minar",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Das Taj Mahal wurde von Mughal-Kaiser Shah Jahan als Grabmahl für seine verstorbene Lieblingsfrau Mumtaz Mahal erbaut. Die weiße Marmorstruktur gilt als Höhepunkt islamisch-indischer Architektur und ist seit 1983 UNESCO-Weltkulturerbe.",
        funFact = "Es heißt, Shah Jahan habe nach Fertigstellung des Taj Mahal den Baumeister entlassen — oder ihm sogar die Hände abgehackt lassen —, damit er kein ähnliches Gebäude für jemand anderen bauen könnte. Diese Geschichte ist historisch nicht belegt."
    ),

    // Frage 31 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welche Stadt wurde vollständig auf dem Reißbrett geplant und diente als Modell für die 'Brasilianische Moderne' — entworfen von Lúcio Costa und Oscar Niemeyer?",
        answerA = "São Paulo (Stadtplanung 1955)",
        answerB = "Buenos Aires (Stadtplanung 1950)",
        answerC = "Brasília (Hauptstadt seit 1960)",
        answerD = "Montevideo (Stadtplanung 1952)",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Brasília wurde ab 1957 komplett geplant und 1960 als neue Hauptstadt Brasiliens eingeweiht. Der Stadtplan von Lúcio Costa ähnelt aus der Vogelperspektive einem Flugzeug oder Vogel. Niemeyer entwarf die ikonischen Regierungsgebäude.",
        funFact = null
    ),

    // Frage 32 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was bezeichnet 'Fenestration' in der Architektursprache?",
        answerA = "Die Verglasung von Glashäusern und Wintergärten mit speziellen Wärmedämmgläsern",
        answerB = "Die Ausrichtung von Gebäuden nach dem Sonnenstand für maximale Energieeffizienz",
        answerC = "Die konstruktive Verbindung zwischen Mauerwerk und Dachstuhl",
        answerD = "Die Anordnung, Gestaltung und Proportion von Fenstern und Öffnungen in einer Fassade",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Fenestration (von lat. 'fenestra' = Fenster) beschreibt das gesamte System der Fenster- und Türöffnungen eines Gebäudes — ihre Größe, Form, Rhythmik und Proportion in der Fassade. Sie ist ein zentrales Gestaltungsmittel der Architektur aller Epochen.",
        funFact = "Im Barock wurden Fenster bewusst größer gestaltet, als es für Belichtung nötig gewesen wäre — als Ausdruck von Reichtum, da Fensterglas damals extrem teuer war."
    ),

    // Frage 33 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welches irakisch-britische Architekturbüro entwarf das MAXXI in Rom (2010), das BMW Werk Leipzig und den Aquatics Centre in London?",
        answerA = "Zaha Hadid Architects",
        answerB = "Foster + Partners",
        answerC = "Rem Koolhaas / OMA",
        answerD = "BIG (Bjarke Ingels Group)",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Zaha Hadid Architects, gegründet von der irakisch-britischen Architektin Zaha Hadid (1950–2016), ist bekannt für organisch fließende Formen und parametrische Entwurfsmethoden. Hadid war die erste Frau, die den Pritzker-Preis erhielt (2004).",
        funFact = "Zaha Hadid hatte jahrelang Projekte gewonnen, die nie gebaut wurden — sie galt als 'paper architect'. Ihr erster realisierter Bau in Europa war erst 1993 die Feuerwache des Vitra-Campus in Weil am Rhein."
    ),

    // Frage 34 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was ist die 'Chicago School' der Architektur (ca. 1880–1900) und wer waren ihre wichtigsten Vertreter?",
        answerA = "Eine Vereinigung von Architekten, die historische Baustile (Neogotik, Neorenaissance) für Hochhäuser adaptierte — vertreten durch Burnham und Root",
        answerB = "Die erste Generation von Architekten, die das Stahlskelettbau-Hochhaus entwickelten — darunter Louis Sullivan, William Jenney und Burnham & Root",
        answerC = "Eine Schule für Stadtplanung, die Grünflächen und Parksysteme in den Mittelpunkt stellte",
        answerD = "Eine Bewegung für sozialen Wohnungsbau nach europäischem Vorbild in amerikanischen Industriestädten",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Die Chicago School revolutionierte das Bauen: Louis Sullivan ('Form follows function'), William Le Baron Jenney (erstes Stahlskelett) und Burnham & Root entwickelten die technischen und ästhetischen Grundlagen des modernen Hochhauses nach dem Chicago-Brand von 1871.",
        funFact = null
    ),

    // Frage 35 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welches architektonische Merkmal unterscheidet eine Basilika von einem Hallenkirche-Grundriss?",
        answerA = "Die Basilika hat keine Seitenschiffe, die Hallenkirche hat zwei",
        answerB = "Die Basilika verwendet ausschließlich Rundbogen, die Hallenkirche Spitzbögen",
        answerC = "Das Mittelschiff der Basilika ragt über die Seitenschiffe hinaus und hat Obergadenfenster",
        answerD = "Die Basilika hat eine Apsis im Westen, die Hallenkirche immer im Osten",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Das Hauptmerkmal der Basilika ist das erhöhte Mittelschiff, das über die gleich hohen Seitenschiffe hinausragt und durch Obergadenfenster direkt beleuchtet wird. Die Hallenkirche hat dagegen gleichhohe Schiffe — typisch für Westfalen und Spätgotik.",
        funFact = "Die Petersbasilika in Rom vereint beide Prinzipien: Der Querschiff-Bereich ist basilikalisch, der Langhausteil wirkt fast hallen artig durch Michelangelos Gestaltung."
    ),

    // Frage 36 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was beschreibt der Begriff 'Curtain Wall' (Vorhangfassade) in der modernen Hochhausarchitektur?",
        answerA = "Eine schwere Ziegelfassade, die das Gewicht des Gebäudes trägt",
        answerB = "Eine textile Sonnenschutzanlage vor der Fensterfläche",
        answerC = "Das Stahlskelett, das sichtbar an der Fassade nach außen geführt wird",
        answerD = "Eine nicht-tragende Außenhülle aus Glas und Metall, die wie ein Vorhang vor dem Tragwerk hängt",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Die Curtain Wall ist eine leichte, nicht-tragende Fassade aus Aluminiumprofilen und Glas, die von außen vor das eigentliche Tragwerk gehängt wird. Sie trennt Tragfunktion von Klimatisierung und prägt das Bild moderner Bürotürme weltweit.",
        funFact = "Mies van der Rohes Lake Shore Drive Apartments in Chicago (1951) waren unter den ersten Hochhäusern mit reiner Glasvorhangfassade — was damals als kühn radikal galt und heute überall selbstverständlich ist."
    ),

    // Frage 37 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welcher Architekt entwarf die Elbphilharmonie in Hamburg (2017) und ist auch für das Kulturzentrum in Luzern (KKL) bekannt?",
        answerA = "Herzog & de Meuron (Pierre de Meuron, Jacques Herzog)",
        answerB = "Renzo Piano Building Workshop",
        answerC = "Jean Nouvel Ateliers",
        answerD = "Zaha Hadid Architects",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Das Basler Büro Herzog & de Meuron entwarf die Elbphilharmonie in Hamburg, die 2017 nach langer Bauzeit und massiven Kostensteigerungen eröffnet wurde. Das Gebäude setzt auf dem historischen Kaispeicher B auf und wurde mit dem Pritzker-Preis gewürdigt.",
        funFact = "Die Elbphilharmonie kostete am Ende rund 866 Millionen Euro — ursprünglich waren 77 Millionen geplant. Der Akustikraum (Großer Saal) gilt dennoch als einer der besten Konzertsäle der Welt."
    ),

    // Frage 38 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was ist das architektonische Prinzip hinter dem 'Tensegrity'-System (Buckminster Fuller / Kenneth Snelson)?",
        answerA = "Starre Rahmenkonstruktionen aus Stahl mit maximaler Wiederholbarkeit",
        answerB = "Druckfeste Stäbe, die nur durch vorgespannte Zugseile zusammengehalten werden und sich nicht berühren",
        answerC = "Pneumatische Membranstrukturen ohne Stahlskelett",
        answerD = "Modulare Betonfertigteile, die ohne Mörtel gestapelt werden",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Tensegrity (Tension + Integrity) beschreibt Strukturen, bei denen druckfeste Stäbe frei im Raum schweben und nur durch Zugseile gehalten werden. Die Stäbe berühren sich nie direkt. Buckminster Fuller und Kenneth Snelson entwickelten das Konzept in den 1940er–50er Jahren.",
        funFact = null
    ),

    // Frage 39 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welches Gebäude in New York City gilt als Ikone des Art-Déco-Stils und war beim Bau 1930 kurzzeitig das höchste Gebäude der Welt?",
        answerA = "Empire State Building",
        answerB = "Flatiron Building",
        answerC = "Chrysler Building",
        answerD = "Woolworth Building",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Das Chrysler Building (1930, Architekt William Van Alen) war mit 319 Metern für elf Monate das höchste Gebäude der Welt — bis das Empire State Building 1931 fertig wurde. Die charakteristischen Edelstahlbögen der Krone gelten als Höhepunkt des Art Déco.",
        funFact = "Die Spitze des Chrysler Buildings wurde heimlich im Inneren des Gebäudes montiert und innerhalb von 90 Minuten durch den Turm nach oben geschoben — um dem rivalisierenden 40 Wall Street Gebäude den Rekord zu stehlen."
    ),

    // Frage 40 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was ist das Konzept des 'Metabolismus' (Metabolismus-Bewegung, Japan, 1960er Jahre)?",
        answerA = "Die Verwendung von Recyclingmaterialien im Bauwesen als ökologisches Prinzip",
        answerB = "Die Anpassung von Gebäuden an klimatische Veränderungen durch automatische Gebäudehüllen",
        answerC = "Die energetische Sanierung von Nachkriegsbauten nach japanischen Effizienzstandards",
        answerD = "Städte als lebende Organismen mit austauschbaren Modulen — Infrastruktur permanent, Wohnkapseln temporär",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Der japanische Metabolismus (Kenzo Tange, Kisho Kurokawa, Arata Isozaki) sah Städte als biologische Organismen: permanente Infrastruktur als 'Knochen', austauschbare Wohnkapseln als 'Zellen'. Nakagin Capsule Tower (1972) ist das bekannteste Beispiel.",
        funFact = "Der Nakagin Capsule Tower in Tokio (1972) von Kisho Kurokawa wurde 2022 abgerissen, obwohl er unter Architekten weltweite Kultstatushat hatte. Fast keine der 140 Kapseln war noch bewohnt."
    ),

    // Frage 41 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welches Strukturelement der klassisch-griechischen Tempelarchitektur bezeichnet man als 'Entasis'?",
        answerA = "Die leichte Schwellung in der Mitte der Säule, die optische Durchbiegung verhindert",
        answerB = "Die Neigung der Außenwände nach innen für visuelle Stabilität",
        answerC = "Das Kapitell als krönender Abschluss der Säule",
        answerD = "Die Rillen (Kanneluren) in der Säulenfläche",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Entasis (griech. 'Spannung') ist die kaum wahrnehmbare Ausbauchung einer Säule in der unteren Hälfte. Ohne diese optische Korrektur würden gerade Säulen eingedrückt wirken. Sie ist eine der subtilsten optischen Korrekturen der griechischen Tempelarchitektur.",
        funFact = "Das Parthenon in Athen enthält über 72 verschiedene optische Korrekturen — Entasis, leicht nach innen geneigte Säulen, gewölbte Stylobate — um ein mathematisch exaktes Erscheinungsbild zu erzeugen."
    ),

    // Frage 42 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Wer entwarf die Neue Nationalgalerie in Berlin (1968) und gilt als einer der einflussreichsten Architekten des 20. Jahrhunderts?",
        answerA = "Walter Gropius",
        answerB = "Ludwig Mies van der Rohe",
        answerC = "Hans Scharoun",
        answerD = "Alvar Aalto",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Mies van der Rohes Neue Nationalgalerie (1968) ist sein letztes großes Werk: ein stützenfreier Glaspavilion unter einem monumentalen Stahldach, das auf nur acht Außenstützen ruht. Das Gebäude gilt als Manifest seines Minimalismus.",
        funFact = null
    ),

    // Frage 43 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welche Bauform wird als 'Hypokaustenheizung' bezeichnet und war in der römischen Antike verbreitet?",
        answerA = "Ein Kaminheizungssystem in Privathäusern mit Ofenrohren in den Wandfugen",
        answerB = "Eine Fußbodenheizung durch heiße Wasserrohre unter Mosaikböden",
        answerC = "Eine Unterbodenheizung, bei der heiße Luft durch Hohlräume unter dem Fußboden und in Hohlwänden geleitet wird",
        answerD = "Eine Zentralheizung mit Dampf, der durch öffentliche Fernwärmeleitungen geleitet wird",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Das Hypokaustsystem der Römer erwärmte Gebäude durch heiße Luft, die von einem Feuerraum (Praefurnium) unter einem erhöhten Fußboden und durch Hohlziegel in den Wänden zirkulierte. In Thermen und Villen war es weit verbreitet.",
        funFact = "Das Grundprinzip der römischen Hypokaustenheizung ist identisch mit moderner Fußbodenheizung — nur dass heute Warmwasser statt heißer Luft durch die Rohre fließt."
    ),

    // Frage 44 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was versteht man unter 'Passivhaus-Standard' in der modernen Architektur?",
        answerA = "Ein Gebäude, das ausschließlich mit Solarenergie beheizt wird und keine fossilen Brennstoffe nutzt",
        answerB = "Ein Gebäude ohne aktive mechanische Lüftungsanlagen, das durch natürliche Konvektion klimatisiert wird",
        answerC = "Ein Gebäude aus natürlichen Materialien (Holz, Lehm, Stroh) ohne industrielle Baustoffe",
        answerD = "Ein extrem gut gedämmtes Gebäude, das kaum Heizenergie benötigt und hauptsächlich durch passive Wärmequellen warmgehalten wird",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Der Passivhaus-Standard (entwickelt von Wolfgang Feist, Darmstadt, 1988) definiert Gebäude mit einem Heizwärmebedarf von maximal 15 kWh/m²a — rund 10-mal weniger als herkömmliche Bauten. Passive Wärmequellen: Körperwärme, Sonneneinstrahlung, Abwärme von Geräten.",
        funFact = "Das erste Passivhaus der Welt wurde 1991 in Darmstadt-Kranichstein gebaut. Es verbrauchte über 20 Jahre lang so wenig Energie wie geplant — und wird noch heute bewohnt."
    ),

    // Frage 45 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welche Rolle spielte Viollet-le-Duc in der Architekturgeschichte des 19. Jahrhunderts?",
        answerA = "Er war der bedeutendste Restaurator gotischer Bauten Frankreichs und entwickelte eine eigene Theorie der stilgerechten Restaurierung",
        answerB = "Er begründete den Jugendstil in Frankreich und entwarf den Eiffelturm mit Gustave Eiffel",
        answerC = "Er entwarf alle Haussmannschen Boulevards in Paris nach Napoleons III. Auftrag",
        answerD = "Er schuf den Begriff 'Art Déco' und gestaltete die Pariser Weltausstellung 1900",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Eugène Viollet-le-Duc restaurierte viele gotische Bauten Frankreichs, darunter Notre-Dame de Paris und die Stadtmauer von Carcassonne. Sein Prinzip: Gebäude in einem stilreinen Idealzustand wiederherstellen — was heute oft als zu invasiv kritisiert wird.",
        funFact = null
    ),

    // Frage 46 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Welchen Namen trägt das Architekturprinzip, bei dem ein Gebäude auf schlanken Stützen ('Pilotis') über dem Boden erhoben wird, sodass der Boden darunter frei begehbar bleibt?",
        answerA = "Flächenfundament mit Hohlraumbelüftung",
        answerB = "Pilotis (Le Corbusiers 'Fünf Punkte der neuen Architektur')",
        answerC = "Kragarmkonstruktion auf Punktfundamenten",
        answerD = "Sockelzone nach dem Dreizonen-Prinzip",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Le Corbusier formulierte 1927 seine 'Cinq Points de l'Architecture Nouvelle': Pilotis (Stützen), Dachgarten, freier Grundriss, Langfenster und freie Fassade. Die Pilotis heben das Gebäude an und geben den Boden für Fußgänger frei.",
        funFact = "Le Corbusiers Villa Savoye (1931) bei Paris zeigt alle fünf Punkte exemplarisch — und steht unter Denkmalschutz als Ikone der Moderne."
    ),

    // Frage 47 — correctAnswer = 2
    Question(
        categoryId = 11,
        questionText = "Welches Bauwerk aus der Antike wird als eines der Sieben Weltwunder bezeichnet und ist das einzige, das bis heute erhalten ist?",
        answerA = "Der Koloss von Rhodos",
        answerB = "Der Leuchtturm von Alexandria",
        answerC = "Die Pyramiden von Gizeh",
        answerD = "Der Tempel der Artemis in Ephesus",
        correctAnswer = 2,
        difficulty = 4,
        explanation = "Die Großen Pyramiden von Gizeh (ca. 2560–2500 v. Chr.) sind das einzige der Sieben Weltwunder der Antike, das noch existiert. Die Cheops-Pyramide war mit 146,5 Metern fast 3.800 Jahre lang das höchste Bauwerk der Welt.",
        funFact = "Die Cheops-Pyramide besteht aus etwa 2,3 Millionen Steinblöcken mit einem Durchschnittsgewicht von 2,5 Tonnen. Wie genau die Ägypter diese bewegt haben, ist bis heute Gegenstand wissenschaftlicher Diskussionen."
    ),

    // Frage 48 — correctAnswer = 3
    Question(
        categoryId = 11,
        questionText = "Was beschreibt die Architekturtheorie der 'Five Points of New Architecture' von Le Corbusier (1927) als fünften Punkt?",
        answerA = "Das Flachdach als nutzbarer Dachgarten",
        answerB = "Der offene Grundriss ohne tragende Innenwände",
        answerC = "Das Langfenster als durchgehende horizontale Öffnung",
        answerD = "Die freie Fassadengestaltung unabhängig vom Tragsystem",
        correctAnswer = 3,
        difficulty = 4,
        explanation = "Le Corbusiers fünf Punkte: 1. Pilotis, 2. Dachgarten, 3. freier Grundriss, 4. horizontale Fensterbänder, 5. freie Fassade. Der fünfte Punkt — die freie Fassade — war erst durch das Betonskelett möglich, das Außenwände von ihrer Tragfunktion befreite.",
        funFact = null
    ),

    // Frage 49 — correctAnswer = 0
    Question(
        categoryId = 11,
        questionText = "Welcher Architekt entwarf den Reichstag-Glaskuppel-Umbau in Berlin (1999) und gilt als Pionier des 'High-Tech'-Stils?",
        answerA = "Norman Foster (Foster + Partners)",
        answerB = "Renzo Piano",
        answerC = "Richard Rogers",
        answerD = "Santiago Calatrava",
        correctAnswer = 0,
        difficulty = 4,
        explanation = "Norman Foster entwarf die gläserne Kuppel des deutschen Bundestages (1999). Das transparente Symbol der Demokratie ermöglicht Bürgern den Blick auf den Plenarsaal von oben. Foster ist auch bekannt für das 30 St Mary Axe ('Gurke') in London.",
        funFact = "Die Reichstagskuppel hat eine doppelte Helix-Rampe, auf der zwei Besucherströme gleichzeitig, aber in entgegengesetzte Richtungen gehen können, ohne sich zu begegnen — eine brillante Besucherlenkungslösung."
    ),

    // Frage 50 — correctAnswer = 1
    Question(
        categoryId = 11,
        questionText = "Was ist das architektonische Alleinstellungsmerkmal des Lotus Temple in Neu-Delhi (1986, Architekt Fariborz Sahba)?",
        answerA = "Er ist das größte Bahá'í-Gotteshaus der Welt und besteht aus 27 unabhängig gefügten Marmorblöcken",
        answerB = "Er besteht aus 27 freistehenden, marmorverk leideten Betonschalen in Lotusblütenform und ist ohne jegliche religiöse Symbole",
        answerC = "Er wurde vollständig aus recyceltem indischem Sandstein ohne Stahl oder Beton gebaut",
        answerD = "Der Tempel hat keine festen Wände — alle Seiten sind offen und nur durch Wasserbecken begrenzt",
        correctAnswer = 1,
        difficulty = 4,
        explanation = "Der Lotus Temple ist das Bahá'í-Gotteshaus für Südasien und symbolisiert Einheit der Religionen. Die 27 Marmorschalen bilden drei Gruppen zu je neun Schalen und sind von neun Teichen umgeben. Alle Religionen sind willkommen — keine Symbole, keine Statuen.",
        funFact = "Der Lotus Temple empfängt jährlich über 4 Millionen Besucher und ist damit eines der meistbesuchten Gebäude der Welt — vor dem Taj Mahal und dem Empire State Building."
    )

)
