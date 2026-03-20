package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsHard5(): List<Question> = listOf(

    // --- ENTDECKUNGSREISEN (17) ---

    Question(
        categoryId = 11,
        questionText = "Mit welchem Schiff segelte Christoph Kolumbus als Flaggschiff auf seiner ersten Reise 1492 in Richtung Amerika?",
        answerA = "Pinta",
        answerB = "Niña",
        answerC = "Victoria",
        answerD = "Santa María",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Die Santa María war das Flaggschiff von Kolumbus auf der ersten Reise 1492. Sie lief jedoch am 25. Dezember 1492 auf einer Sandbank auf Haiti auf Grund und musste aufgegeben werden.",
        funFact = "Aus den Trümmern der Santa María bauten Kolumbus und seine Männer das Fort La Navidad — die erste europäische Siedlung in Amerika."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Seefahrer umrundete 1488 als erster Europäer das Kap der Guten Hoffnung?",
        answerA = "Vasco da Gama",
        answerB = "Ferdinand Magellan",
        answerC = "Bartolomeu Dias",
        answerD = "Pedro Álvares Cabral",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Bartolomeu Dias umrundete 1488 als erster Europäer das südlichste Kap Afrikas. Er nannte es zunächst 'Cabo das Tormentas' (Kap der Stürme) — der portugiesische König João II. gab ihm den Namen 'Kap der Guten Hoffnung'.",
        funFact = "Bartolomeu Dias ertrank 1500 im Sturm in der Nähe desselben Kaps, das er als erster entdeckt hatte."
    ),

    Question(
        categoryId = 11,
        questionText = "Wann erreichte Vasco da Gama auf dem Seeweg über das Kap der Guten Hoffnung erstmals Indien?",
        answerA = "1498",
        answerB = "1492",
        answerC = "1503",
        answerD = "1510",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Vasco da Gama erreichte am 20. Mai 1498 den Hafen von Calicut an der indischen Malabarküste. Damit war der Seeweg nach Indien um Afrika herum erschlossen — ein Meilenstein für den Welthandel.",
        funFact = "Die Gewürze, die Vasco da Gama nach Portugal brachte, waren so wertvoll, dass der Erlös mehr als das 60-fache der Expeditionskosten deckte."
    ),

    Question(
        categoryId = 11,
        questionText = "Wie viele Schiffe starteten bei Magellans Weltumsegelung 1519 — und wie viele kehrten zurück?",
        answerA = "3 Schiffe / 1 zurück",
        answerB = "4 Schiffe / 2 zurück",
        answerC = "5 Schiffe / 1 zurück",
        answerD = "5 Schiffe / 2 zurück",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Magellan startete 1519 mit 5 Schiffen und rund 270 Mann. Nur die Victoria mit 18 überlebenden Männern kehrte 1522 zurück. Magellan selbst starb 1521 auf den Philippinen.",
        funFact = "Der überlebende Kapitän Juan Sebastián Elcano vollendete die erste Weltumsegelung und bekam als Wappenmotiv eine Weltkugel mit der Inschrift: 'Du umkreistest mich als Erster'."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Meeresstraße entdeckte Magellan 1520 und benannte sie nach sich selbst?",
        answerA = "Drakestraße",
        answerB = "Beringstraße",
        answerC = "Sundastraße",
        answerD = "Magellanstraße",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Die Magellanstraße (Estrecho de Magallanes) trennt das südamerikanische Festland von Feuerland. Magellan durchquerte sie im Oktober/November 1520 und gelangte so in den von ihm 'Pazifik' (ruhiges Meer) getauften Ozean.",
        funFact = "Die Durchquerung der Straße dauerte 38 Tage. Danach überquerte Magellan den Pazifik ohne Halt — 99 Tage auf See mit verdorbenem Proviant, bis er die Marianen erreichte."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war das Schiffstyp der Wahl für portugiesische Entdecker im 15. Jahrhundert?",
        answerA = "Galeone",
        answerB = "Karavelle",
        answerC = "Drachenschiff",
        answerD = "Holk",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die Karavelle war das revolutionäre Segelschiff des 15. Jahrhunderts: klein (20–25 m), wendig, mit geringem Tiefgang und Lateinersegeln, die ein Segeln gegen den Wind ermöglichten. Sie war das Hauptschiff der portugiesischen Entdecker.",
        funFact = "Die dreieckigen Lateinersegel der Karavelle stammten aus dem arabischen Schiffbau — die Portugiesen übernahmen diese Technik von den Mauren."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Entdecker landete 1497/98 an der Küste Nordamerikas und erkundete im Auftrag Englands den nordatlantischen Raum?",
        answerA = "John Cabot",
        answerB = "Jacques Cartier",
        answerC = "Giovanni da Verrazzano",
        answerD = "Henry Hudson",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "John Cabot (Giovanni Caboto) fuhr 1497 im Auftrag von König Heinrich VII. von England und erreichte die Küste Nordamerikas (wahrscheinlich Neufundland oder Labrador). Er war damit der erste Europäer seit den Wikingern, der Nordamerika erreichte.",
        funFact = "England beanspruchte auf Basis von Cabots Reise später den Anspruch auf weite Teile Nordamerikas — für eine Investition von gerade einmal 10 Pfund."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Kartograph gab dem amerikanischen Kontinent 1507 seinen Namen?",
        answerA = "Gerardus Mercator",
        answerB = "Amerigo Vespucci",
        answerC = "Abraham Ortelius",
        answerD = "Martin Waldseemüller",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Der deutsche Kartograph Martin Waldseemüller schlug 1507 in seiner 'Cosmographiae Introductio' vor, den neuen Kontinent nach Amerigo Vespucci 'America' zu nennen. Vespucci hatte erkannt, dass es sich nicht um Asien, sondern um einen neuen Erdteil handelt.",
        funFact = "Waldseemüller bereute seinen Vorschlag später und verwendete den Namen 'America' in seinen späteren Karten nicht mehr — doch da war es bereits zu spät, der Name hatte sich durchgesetzt."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Ereignis 1453 trieb Europa dazu, neue Seewege nach Asien zu suchen?",
        answerA = "Der Fall von Granada",
        answerB = "Der Tod von Heinrich dem Seefahrer",
        answerC = "Die Pest in Konstantinopel",
        answerD = "Die Eroberung Konstantinopels durch die Osmanen",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Die Eroberung Konstantinopels durch die Osmanen 1453 blockierte die traditionellen Landhandelswege nach Asien. Gewürze und Luxusgüter aus Indien wurden dadurch nahezu unbezahlbar — der Druck, neue Seewege zu finden, war enorm.",
        funFact = "Pfeffer war damals so wertvoll, dass er als Währung akzeptiert wurde. In einigen Städten konnten Mieten und Löhne in Pfeffer bezahlt werden."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Entdecker leitete 1513 die erste europäische Expedition, die den Stillen Ozean von der amerikanischen Seite erblickte?",
        answerA = "Vasco Núñez de Balboa",
        answerB = "Juan Ponce de León",
        answerC = "Hernán Cortés",
        answerD = "Francisco Pizarro",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Vasco Núñez de Balboa überquerte 1513 die Landenge von Panama und sah als erster Europäer den Stillen Ozean von der Neuen Welt aus. Er nannte ihn 'Mar del Sur' (Südmeer).",
        funFact = "Balboa watete in voller Rüstung ins Meer und beanspruchte den Pazifik und alle an ihm liegenden Länder im Namen der spanischen Krone — ein der dramatischsten Momente der Entdeckungsgeschichte."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Polarforscher erreichte am 14. Dezember 1911 als erster Mensch den Südpol?",
        answerA = "Roald Amundsen",
        answerB = "Ernest Shackleton",
        answerC = "Fridtjof Nansen",
        answerD = "Robert Falcon Scott",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Roald Amundsen erreichte am 14. Dezember 1911 als erster Mensch den geographischen Südpol — 33 Tage vor Robert Falcon Scott. Scott und seine Begleiter starben auf dem Rückmarsch an Erschöpfung und Kälte.",
        funFact = "Amundsens Erfolgsgeheimnis waren Grönlandhunde statt Ponys: Er verwendete Schlittenhunde wie die Inuit und fraß bei Bedarf sogar einzelne Hunde auf dem Weg, um Gewicht zu sparen."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher portugiesische Prinz gilt als Initiator des Zeitalters der Entdeckungen, obwohl er selbst kaum auf See war?",
        answerA = "Infante Henrique (Heinrich der Seefahrer)",
        answerB = "König João II.",
        answerC = "König Manuel I.",
        answerD = "Infante Pedro",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Infante Henrique, bekannt als Heinrich der Seefahrer (1394–1460), förderte systematisch Expeditionen entlang der afrikanischen Küste von seiner Residenz in Sagres aus. Er gründete eine Navigationsschule und finanzierte zahlreiche Entdeckungsfahrten.",
        funFact = "Der Spitzname 'der Seefahrer' ist irreführend: Heinrich nahm selbst nur an wenigen Expeditionen teil — sein Genie lag in der Organisation und Finanzierung der Erkundungsfahrten."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Entdecker reiste zwischen 1271 und 1295 durch Asien und beschrieb seine Erlebnisse im berühmten 'Buch der Wunder'?",
        answerA = "Ibn Battuta",
        answerB = "Marco Polo",
        answerC = "Wilhelm von Rubruk",
        answerD = "Zheng He",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Marco Polo reiste als junger Mann mit seinem Vater und Onkel durch Zentralasien bis nach China, wo er am Hof von Kublai Khan diente. Sein Reisebericht 'Il Milione' inspirierte Generationen von Entdeckern — darunter Kolumbus.",
        funFact = "Marco Polo diktierte sein Buch einem Mitgefangenen, als er 1296 in Genua inhaftiert war. Viele Zeitgenossen hielten seine Geschichten für pure Erfindung."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher chinesische Admiral unternahm zwischen 1405 und 1433 sieben Großexpeditionen in den Indischen Ozean und nach Ostafrika?",
        answerA = "Kublai Khan",
        answerB = "Cheng Ho (Zheng He)",
        answerC = "Xuande Kaiser",
        answerD = "Yongle Kaiser",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Zheng He (Cheng Ho) führte mit einer Flotte von bis zu 300 Schiffen und 28.000 Mann sieben Großexpeditionen durch. Seine größten Schiffe ('Schatzschiffe') waren mit über 120 m Länge die größten Holzschiffe, die je gebaut wurden.",
        funFact = "Zheng He war ein Moslem und Eunuch. Nach seinem Tod und dem Ende der Ming-Expeditionen wurden alle seine Reiseberichte und Schiffspläne auf kaiserlichen Befehl vernichtet — China zog sich aus der Welteroberung zurück."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Wikinger-Entdecker erreichte um das Jahr 1000 als erster Europäer den nordamerikanischen Kontinent?",
        answerA = "Erik der Rote",
        answerB = "Björn Ironside",
        answerC = "Leif Eriksson",
        answerD = "Ragnar Lothbrok",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Leif Eriksson erreichte um 1000 n.Chr. einen Ort, den er 'Vinland' nannte — vermutlich die heutige Küste Neufundlands in Kanada. Archäologische Funde in L'Anse aux Meadows bestätigen eine wikingische Siedlung aus dieser Zeit.",
        funFact = "Erik der Rote, Leifs Vater, hatte Grönland besiedelt und gab der Eisinsel ihren grünen Namen als Marketingstrategie, um Siedler anzulocken."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Vertrag von 1494 teilte die Welt zwischen Portugal und Spanien auf?",
        answerA = "Vertrag von Alcáçovas",
        answerB = "Vertrag von Tordesillas",
        answerC = "Vertrag von Granada",
        answerD = "Vertrag von Zaragoza",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Der Vertrag von Tordesillas (1494) zog eine Demarkationslinie 370 Seemeilen westlich der Kapverdischen Inseln. Alles östlich davon gehörte Portugal, alles westlich Spanien — damit fiel Brasilien später zu Portugal.",
        funFact = "Der Rest der Welt — Engländer, Franzosen, Niederländer — erkannte diesen Vertrag schlicht nicht an. Francis Drake soll gesagt haben: 'Kein König kann fremde Fürsten an der Schifffahrt hindern.'"
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Entdecker kartographierte als erster die Nordwestküste Australiens und ist Namensgeber für die Dampierstraße?",
        answerA = "James Cook",
        answerB = "Abel Tasman",
        answerC = "William Dampier",
        answerD = "Matthew Flinders",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "William Dampier erkundete als erster Brite 1688 und 1699 die australische Nordwestküste. Er war Buccaneer, Naturforscher und Kartograph in einer Person und schrieb ausführliche Berichte über die australische Natur.",
        funFact = "Dampier war der erste Mensch, der dreimal die Welt umsegelte. Sein Buch 'A New Voyage Round the World' (1697) war ein Bestseller und inspirierte Daniel Defoe zu 'Robinson Crusoe'."
    ),

    // --- TRANSPORT & EISENBAHN (17) ---

    Question(
        categoryId = 11,
        questionText = "Zwischen welchen Städten fuhr am 7. Dezember 1835 die erste Eisenbahn auf deutschem Boden?",
        answerA = "Berlin und Potsdam",
        answerB = "Hamburg und Lübeck",
        answerC = "Nürnberg und Fürth",
        answerD = "Köln und Düsseldorf",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Am 7. Dezember 1835 fuhr die Dampflokomotive 'Adler' — gebaut von Robert Stephenson in England — zwischen Nürnberg und Fürth. Die Strecke war 6 Kilometer lang, und die Fahrt dauerte etwa 9 Minuten.",
        funFact = "Der erste Lokomotivführer in Deutschland, William Wilson, war ein Engländer — denn die Deutschen hatten noch keine Erfahrung mit Dampflokomotiven. Er wurde extra aus England eingeflogen."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer baute 1804 die erste funktionstüchtige Dampflokomotive der Welt?",
        answerA = "George Stephenson",
        answerB = "James Watt",
        answerC = "Matthew Murray",
        answerD = "Richard Trevithick",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Richard Trevithick konstruierte 1804 die erste Dampflokomotive und fuhr damit in Wales von Merthyr Tydfil zum Kanal Abercynon — rund 16 km mit 10 Tonnen Eisen und 70 Passagieren an Bord.",
        funFact = "Trevithick starb 1833 verarmt, obwohl er einer der bedeutendsten Erfinder der Industriellen Revolution war. George Stephenson (sein Schüler) und andere verdienten Millionen mit seinen Ideen."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Strecke eröffnete George Stephenson 1825 als erste öffentliche Eisenbahnlinie mit Dampfbetrieb?",
        answerA = "London – Brighton",
        answerB = "Manchester – Liverpool",
        answerC = "London – Birmingham",
        answerD = "Stockton – Darlington",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Am 27. September 1825 fuhr Stephensons Lokomotive 'Locomotion' auf der Strecke Stockton–Darlington (40 km) mit Kohle und Passagieren. Es war die erste öffentliche Eisenbahnlinie der Welt mit Dampfbetrieb.",
        funFact = "Ein Mann zu Pferd ritt der Locomotion voraus, um die Strecke freizumachen — die Lokomotive holte ihn bald ein und ließ ihn weit hinter sich. Das Zeitalter des Pferdes war vorbei."
    ),

    Question(
        categoryId = 11,
        questionText = "In welchem Jahr nahm der Orient-Express seinen regulären Betrieb von Paris nach Konstantinopel auf?",
        answerA = "1883",
        answerB = "1875",
        answerC = "1891",
        answerD = "1905",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Der Orient-Express nahm am 4. Oktober 1883 seinen regulären Betrieb auf. Die Strecke von Paris nach Konstantinopel (Istanbul) dauerte damals rund 82 Stunden.",
        funFact = "Agatha Christie schrieb ihren Roman 'Mord im Orient-Express' (1934), nachdem sie selbst mit dem Zug gereist war und er wegen Schneesturms im Balkan steckenblieb — genau wie im Buch."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Land führte 1964 als erstes Hochgeschwindigkeitszüge (Shinkansen) ein?",
        answerA = "Frankreich",
        answerB = "Deutschland",
        answerC = "Japan",
        answerD = "USA",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Japan eröffnete am 1. Oktober 1964 — pünktlich zu den Olympischen Spielen in Tokio — den ersten Shinkansen zwischen Tokio und Osaka. Der 'Bullet Train' fuhr mit 210 km/h und revolutionierte den Schienenverkehr.",
        funFact = "Der Shinkansen hat in über 60 Jahren Betrieb (bis 2024) keinen einzigen Todesfall durch Unfall verzeichnet. Die durchschnittliche Verspätung liegt unter einer Minute."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer gilt als Erfinder des Automobils mit Verbrennungsmotor und in welchem Jahr reichte er das Patent ein?",
        answerA = "Gottlieb Daimler, 1883",
        answerB = "Karl Benz, 1886",
        answerC = "Henry Ford, 1896",
        answerD = "Wilhelm Maybach, 1885",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Karl Benz erhielt am 29. Januar 1886 das Deutsche Reichspatent Nr. 37435 für seinen dreirädrigen 'Motorwagen Nr. 1' — das erste Automobil mit Benzinmotor. Dieses Datum gilt offiziell als Geburtstag des Automobils.",
        funFact = "Die erste Fernfahrt mit dem Auto unternahm nicht Karl Benz, sondern seine Frau Bertha: Sie fuhr 1888 heimlich von Mannheim nach Pforzheim (106 km) — ohne Wissen ihres Mannes und mit ihren Söhnen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war die erste Nation, die einen öffentlichen Omnibus-Linienbetrieb einführte — und in welcher Stadt?",
        answerA = "Frankreich / Paris",
        answerB = "England / London",
        answerC = "Deutschland / Berlin",
        answerD = "USA / New York",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Die erste Omnibuslinie der Welt startete am 18. März 1662 in Paris — als Kutschenverbindung mit festem Fahrplan und festem Preis. Initiator war Blaise Pascal. Dieser Dienst wurde jedoch nach wenigen Jahren mangels Rentabilität eingestellt.",
        funFact = "Der moderne Pferdeomnibusdienst wurde 1828 in Nantes (Frankreich) wiederbelebt. Das Wort 'Omnibus' kommt vom Latein und bedeutet 'für alle' — ein sozialer Anspruch von Anfang an."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Kanal verbindet seit 1869 das Mittelmeer mit dem Roten Meer und revolutionierte den Welthandel?",
        answerA = "Panamakanal",
        answerB = "Suezkanal",
        answerC = "Kielkanal",
        answerD = "Korinthoskanal",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Der Suezkanal wurde am 17. November 1869 eröffnet. Die 193 km lange Wasserstraße verbindet Port Said am Mittelmeer mit Port Tewfik am Roten Meer und ermöglicht Schiffen, den Umweg um Afrika zu vermeiden.",
        funFact = "Vor dem Suezkanal mussten Schiffe von Europa nach Indien rund 7.000 km weiter fahren — der Weg ums Kap der Guten Hoffnung. Der Kanal sparte Wochen an Reisezeit."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Kanal verbindet seit 1914 den Atlantik mit dem Pazifik und verkürzt Seereisen um bis zu 15.000 Kilometer?",
        answerA = "Suezkanal",
        answerB = "Nicaraguakanal",
        answerC = "Korinthoskanal",
        answerD = "Panamakanal",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Der Panamakanal wurde am 15. August 1914 eröffnet. Die USA bauten ihn nach dem Scheitern eines französischen Projekts. Die 81 km lange Wasserstraße überwindet mittels Schleusen einen Höhenunterschied von 26 Metern.",
        funFact = "Beim Bau des Panamakanals starben über 20.000 Arbeiter — die meisten nicht durch Unfälle, sondern durch Gelbfieber und Malaria. Erst die Bekämpfung der Stechmücken ermöglichte die Fertigstellung."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche technische Verbesserung machte James Watts Dampfmaschine (1769) so viel effizienter als alle Vorgänger?",
        answerA = "Doppelte Zylindergröße",
        answerB = "Nutzung von Hochdruckdampf",
        answerC = "Der separate Kondensator",
        answerD = "Elektrische Zündung",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "James Watts entscheidende Verbesserung war der separate Kondensator (1769). Bei früheren Maschinen (Newcomen) musste der Zylinder abwechselnd erhitzt und abgekühlt werden — enorm ineffizient. Watts Kondensator ließ den Zylinder stets heiß bleiben.",
        funFact = "Watt erfand die Einheit 'Pferdestärke' (PS), um seine Dampfmaschinen mit der Arbeitsleistung von Pferden vergleichen zu können — als Marketinginstrument für skeptische Bergwerksbesitzer."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Schiff gilt als das größte jemals gebaute Passagierschiff und sank 1912 auf seiner Jungfernfahrt?",
        answerA = "Lusitania",
        answerB = "Britannic",
        answerC = "Titanic",
        answerD = "Olympic",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Die Titanic war mit 269 m Länge und 46.328 Bruttoregistertonnen das größte Schiff ihrer Zeit. Sie sank in der Nacht vom 14. auf den 15. April 1912 nach einer Kollision mit einem Eisberg im Nordatlantik — mehr als 1.500 Menschen starben.",
        funFact = "Die Titanic hatte 20 Rettungsboote — genug für 1.178 Menschen. An Bord waren über 2.200. Nach dem Untergang wurden internationale Vorschriften für ausreichend Rettungsboote für alle Passagiere eingeführt."
    ),

    Question(
        categoryId = 11,
        questionText = "Wie hieß das erste Dampfschiff, das 1838 den Atlantik von Europa nach Amerika überquerte, angetrieben ausschließlich durch Dampfkraft?",
        answerA = "SS Great Western",
        answerB = "SS Savannah",
        answerC = "SS Sirius",
        answerD = "SS Great Britain",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Die SS Sirius war das erste Schiff, das den Atlantik von Irland nach New York ausschließlich unter Dampfkraft überquerte — am 22. April 1838, einen Tag vor der SS Great Western. Es war ein Rennen zwischen beiden Schiffen.",
        funFact = "Auf der letzten Etappe der Sirius gingen der Kohlevorrat und die Reserven zur Neige. Die Besatzung soll sogar Holzmöbel und einen Schiffsmasten verfeuert haben, um New York zu erreichen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war der Hauptgrund für die Einstellung des kommerziellen Concorde-Betriebs im Jahr 2003?",
        answerA = "Zu hoher Treibstoffverbrauch und steigende Ölpreise nach 9/11",
        answerB = "Verbot von Überschallflügen über besiedeltem Gebiet weltweit",
        answerC = "Technisch veraltete Triebwerke ohne mögliche Ersatzteile",
        answerD = "Absturz 2000, gestiegene Betriebskosten und sinkende Passagierzahlen nach 9/11",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Der Concorde-Absturz vom 25. Juli 2000 in Paris (113 Tote) erschütterte das Vertrauen. Kombiniert mit dem Nachfrageeinbruch nach 9/11 und astronomischen Betriebskosten war die Weiterführung wirtschaftlich nicht mehr darstellbar.",
        funFact = "Die Concorde flog von London nach New York in unter 3 Stunden — wegen des Zeitunterschieds landete man Ortszeit früher, als man abgeflogen war. Passagiere konnten 'rückwärts in der Zeit' reisen."
    ),

    // --- LUFTFAHRT (16) ---

    Question(
        categoryId = 11,
        questionText = "Wann und wo führten die Brüder Wright den ersten motorgetriebenen Flug der Geschichte durch?",
        answerA = "1903 in Kitty Hawk, North Carolina",
        answerB = "1900 in Ohio",
        answerC = "1905 in Dayton, Ohio",
        answerD = "1901 in Washington D.C.",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Am 17. Dezember 1903 hob Orville Wright in Kitty Hawk (North Carolina) mit dem Flyer I ab — der erste motorgetriebene, kontrollierte und dauerhafte Flug der Geschichte. Der erste Flug dauerte 12 Sekunden und überwand 37 Meter.",
        funFact = "Am selben Tag absolvierten die Wrights vier Flüge. Der längste dauerte 59 Sekunden über 260 Meter. Abends wurde das Flugzeug vom Wind erfasst und so stark beschädigt, dass es nie wieder flog."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer überquerte 1927 als erster Pilot allein und ohne Zwischenlandung den Atlantik von New York nach Paris?",
        answerA = "Amelia Earhart",
        answerB = "Richard Byrd",
        answerC = "Howard Hughes",
        answerD = "Charles Lindbergh",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Charles Lindbergh startete am 20. Mai 1927 in New York und landete 33,5 Stunden später auf dem Flughafen Le Bourget bei Paris. Er flog allein in seinem einmotorigen Flugzeug 'Spirit of St. Louis' — 5.800 km non-stop.",
        funFact = "25.000 jubelnde Franzosen empfingen Lindbergh in Paris. Er gewann den 'Orteig Prize' von 25.000 Dollar — damals eine enorme Summe. Sein Ruhm war so groß, dass er jahrelang keine Ruhe fand."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Unglück im Jahr 1937 beendete das Zeitalter der kommerziellen Luftschifffahrt?",
        answerA = "Absturz der Graf Zeppelin über dem Atlantik",
        answerB = "Feuer der R101 in Frankreich",
        answerC = "Brandkatastrophe der Hindenburg in Lakehurst",
        answerD = "Kollision zweier Zeppeline über Berlin",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Am 6. Mai 1937 fing das Luftschiff Hindenburg (LZ 129) beim Anlegemanöver in Lakehurst, New Jersey, Feuer und verbrannte in 34 Sekunden. 36 Menschen starben. Das live im Radio übertragene Drama beendete die Ära der Passagier-Luftschiffe.",
        funFact = "Die Hindenburg war mit Wasserstoff gefüllt, obwohl Helium sicherer gewesen wäre — die USA verkauften kein Helium an Nazi-Deutschland. Die genaue Brandursache ist bis heute umstritten."
    ),

    Question(
        categoryId = 11,
        questionText = "In welchem Jahr startete der erste reguläre Transatlantikflug mit einem Passagierflugzeug?",
        answerA = "1939",
        answerB = "1945",
        answerC = "1952",
        answerD = "1958",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Pan American Airways startete am 28. Juni 1939 den ersten regulären Transatlantik-Passagierflug mit einem Flugboot (Boeing 314) von New York nach Southampton. Der Flug dauerte rund 29 Stunden mit Zwischenstopps.",
        funFact = "Das Ticket für den ersten Transatlantikflug kostete 375 Dollar hin und zurück — das Dreifache eines durchschnittlichen Monatsgehalts in den USA. Transatlantikfliegen war zunächst ein Privileg der Superreichen."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer war die erste Frau, die den Atlantik allein im Flugzeug überquerte?",
        answerA = "Harriet Quimby",
        answerB = "Bessie Coleman",
        answerC = "Hanna Reitsch",
        answerD = "Amelia Earhart",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Amelia Earhart überquerte am 20./21. Mai 1932 als erste Frau allein den Atlantik — von Neufundland nach Nordirland in 14 Stunden 56 Minuten. Sie war auch die erste Person überhaupt, die den Atlantik zweimal solo überquerte.",
        funFact = "Amelia Earhart verschwand am 2. Juli 1937 über dem Pazifik beim Versuch einer Weltumrundung. Ihr Flugzeug wurde nie gefunden. Ihr Schicksal bleibt eines der größten Geheimnisse der Luftfahrtgeschichte."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Land und welche Brüder führten am 21. November 1783 den ersten bemannten Freiballonflug durch?",
        answerA = "Frankreich / Brüder Montgolfier",
        answerB = "England / Brüder Wright",
        answerC = "Deutschland / Brüder Zeppelin",
        answerD = "Frankreich / Brüder Lumière",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Die Brüder Joseph und Étienne Montgolfier ließen am 21. November 1783 in Paris den ersten bemannten Heißluftballon aufsteigen. Piloten waren Jean-François Pilâtre de Rozier und der Marquis d'Arlandes — sie flogen 25 Minuten über Paris.",
        funFact = "Beim ersten unbemannten Test hatten die Montgolfiers eine Ente, einen Schaf und einen Hahn als Passagiere geschickt. Alle überlebten — das Schaf soll beim Landen auf dem Hahn ausgerutscht sein."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Flugzeug war das erste in Serienproduktion hergestellte Düsenverkehrsflugzeug der Welt?",
        answerA = "Boeing 707",
        answerB = "Tupolev Tu-104",
        answerC = "Douglas DC-8",
        answerD = "de Havilland Comet",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Die de Havilland Comet nahm am 2. Mai 1952 den regulären Linienbetrieb auf — als erstes Düsenverkehrsflugzeug der Welt. Mehrere mysteriöse Abstürze führten zu umfangreichen Untersuchungen, die das Phänomen der Metallermüdung aufdeckten.",
        funFact = "Die Comet-Abstürze (1953/54) revolutionierten die Luftfahrtsicherheit: Die Untersuchungen entdeckten, dass die quadratischen Fenster unter Druckschwankungen rissen. Seitdem haben Flugzeuge ovale Fenster."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Tier zog das erste öffentliche Straßenbahnfahrzeug der Welt (1832 in New York)?",
        answerA = "Ochsen",
        answerB = "Pferde",
        answerC = "Elektromotor",
        answerD = "Dampfmaschine",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die erste Straßenbahn der Welt wurde am 26. November 1832 in New York von Pferden gezogen — auf Schienen von der City Hall bis zur 14th Street. Pferdebahnen dominierten den städtischen Nahverkehr bis zur Elektrifizierung Ende des 19. Jahrhunderts.",
        funFact = "Eine Pferdebahn brauchte pro Wagen täglich 10–12 Pferde im Wechsel. New York hielt Anfang der 1880er Jahre rund 100.000 Pferde für den Stadtverkehr — das erzeugte täglich über 1.000 Tonnen Pferdemist."
    ),

    Question(
        categoryId = 11,
        questionText = "Wann und wo wurde die erste elektrische Straßenbahn der Welt eröffnet?",
        answerA = "1881 in Berlin-Lichterfelde",
        answerB = "1888 in Richmond, Virginia",
        answerC = "1879 in Wien",
        answerD = "1890 in London",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Die erste elektrische Straßenbahn der Welt fuhr 1881 in Berlin-Lichterfelde — gebaut von Werner von Siemens. Sie bediente eine 2,5 km lange Strecke und war die Keimzelle des weltweiten elektrischen Nahverkehrs.",
        funFact = "Werner von Siemens hatte die Idee der elektrischen Bahn bereits 1879 auf der Berliner Gewerbeausstellung präsentiert — eine Miniaturlokomotive, die 30 Passagiere in Kreisen fuhr und sofort zum Publikumsrenner wurde."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Graf ließ am 2. Juli 1900 am Bodensee das erste starre Luftschiff LZ 1 aufsteigen?",
        answerA = "Graf Otto von Bismarck",
        answerB = "Graf August von Mackensen",
        answerC = "Graf Ferdinand von Zeppelin",
        answerD = "Graf Helmuth von Moltke",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Graf Ferdinand von Zeppelin ließ am 2. Juli 1900 am Bodensee das erste starre Luftschiff LZ 1 aufsteigen. Das 128 m lange Aluminiumgerippe mit Gasbeuteln revolutionierte die Luftfahrt und machte seinen Namen unsterblich.",
        funFact = "Als Kaiser Wilhelm II. Zeppelin anfangs nicht unterstützte, wurde der Graf öffentlich verspottet. Nach einem spektakulären Unglück sammelten Zeitungsleser spontan 6 Millionen Mark für ihn — ein Volksopfer für die Luftfahrt."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war das Hauptprinzip von Henry Fords Fließbandproduktion beim Modell T ab 1913?",
        answerA = "Das Fahrzeug wird von spezialisierten Handwerkern komplett gefertigt",
        answerB = "Das Fahrzeug bewegt sich durch die Fabrik, jeder Arbeiter führt nur eine Aufgabe aus",
        answerC = "Das Fahrzeug wird in Modulen vorgefertigt und dann zusammengebaut",
        answerD = "Das Fahrzeug wird computergesteuert nach Kundenwunsch produziert",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Ford führte 1913 das Fließband ein: Das Fahrzeug bewegte sich auf einem Band durch die Fabrik, jeder Arbeiter führte nur einen einzigen Handgriff aus. Die Montagezeit sank von 12 Stunden auf 93 Minuten — eine Revolution der Industrieproduktion.",
        funFact = "Ford bezahlte seinen Arbeitern ab 1914 5 Dollar pro Tag — das Doppelte des Branchendurchschnitts. Kalkulation: Wer ein Modell T baut, soll es sich auch kaufen können. Der Markt explodierte."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Erfindung des Jahres 1450 legte — über die massenhafte Verbreitung von Seekarten und Reiseberichten — indirekt den Grundstein für das Zeitalter der Entdeckungen?",
        answerA = "Der Kompass",
        answerB = "Das Astrolabium",
        answerC = "Die Karavelle",
        answerD = "Der Buchdruck mit beweglichen Lettern",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Gutenbergs Buchdruck (um 1450) ermöglichte die massenhafte Verbreitung von Seekarten, geographischen Werken und Marco Polos Reisebeschreibungen. Ohne gedruckte Wissensverbreitung wäre die koordinierte Entdeckungsfahrt undenkbar gewesen.",
        funFact = "Kolumbus besaß ein Exemplar von Marco Polos 'Il Milione', das er mit zahlreichen Randnotizen versah. Sein Irrtum — er dachte, er sei in Asien gelandet — beruhte zum Teil auf Polos übertriebenen Entfernungsangaben."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Navigationsinstrument ermöglichte den Seefahrern des 15. Jahrhunderts, den Breitengrad auf See zu bestimmen?",
        answerA = "Kompass",
        answerB = "Sextant",
        answerC = "Astrolabium",
        answerD = "Chronometer",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Das Astrolabium und der Jakobsstab erlaubten es, die Sonnenhöhe oder die Höhe des Polarsterns zu messen und daraus den Breitengrad zu berechnen. Die Portugiesen verfeinerten das Instrument für den Einsatz auf See.",
        funFact = "Der Längengrad konnte bis zur Erfindung des Schiffschronometers (1759 durch John Harrison) nicht präzise bestimmt werden. Ungenaue Längenangaben führten zu zahllosen Schiffsunglücken — es war das größte Navigationsproblem des Zeitalters."
    ),

    Question(
        categoryId = 11,
        questionText = "Auf welchem Kontinent befand sich das Gebiet, das James Cook auf seiner ersten Reise 1770 für England in Besitz nahm?",
        answerA = "Australien",
        answerB = "Antarktis",
        answerC = "Asien",
        answerD = "Nordamerika",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "James Cook landete am 19. April 1770 an der Ostküste Australiens und erklärte das Land zur britischen Besitzung — er nannte es 'New South Wales'. Damit legte er den Grundstein für die spätere Besiedelung durch Großbritannien.",
        funFact = "Cook kartographierte bei seinen drei Weltreisen mehr Küstenlinie als jeder andere Seefahrer zuvor. Auf seiner dritten Reise wurde er 1779 auf Hawaii von Einheimischen getötet — ein schockierendes Ende für den größten Entdecker seiner Zeit."
    ),

    Question(
        categoryId = 11,
        questionText = "Was versteht man unter dem Begriff 'Columbian Exchange' im Zusammenhang mit den Entdeckungsreisen?",
        answerA = "Den Handelsvertrag zwischen Spanien und der Neuen Welt von 1494",
        answerB = "Den gegenseitigen Austausch von Pflanzen, Tieren, Krankheiten und Kulturgütern zwischen Alter und Neuer Welt",
        answerC = "Die Sklaverei-Handelsrouten des Dreieckshandels im Atlantik",
        answerD = "Die diplomatischen Verhandlungen nach Kolumbus' Rückkehr nach Spanien",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Der 'Columbian Exchange' bezeichnet den massiven biologischen und kulturellen Austausch nach 1492: Europa brachte Pferde, Rinder, Weizen und Masernviren in die Neue Welt; Amerika gab Kartoffeln, Tomaten, Mais, Kakao und Syphilis nach Europa.",
        funFact = "Die Kartoffel aus Amerika rettete Millionen Europäer vor dem Hungertod — besonders in Irland und Deutschland. Ohne die Kartoffel hätte das europäische Bevölkerungswachstum des 18./19. Jahrhunderts nicht stattgefunden."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher russische Kosmonaut umkreiste als erster Mensch am 12. April 1961 die Erde?",
        answerA = "Aleksei Leonov",
        answerB = "Juri Gagarin",
        answerC = "Sergei Koroljow",
        answerD = "Gherman Titov",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Juri Gagarin umkreiste am 12. April 1961 in 108 Minuten die Erde in der Kapsel Wostok 1 und landete mit dem Fallschirm bei Saratow. Dieser Flug gilt als größte Entdeckungsreise des 20. Jahrhunderts.",
        funFact = "Gagarin hatte vor dem Start keine Garantie, dass er den Wiedereintritt in die Atmosphäre überleben würde — die Wissenschaftler waren sich nicht sicher, ob ein Mensch die extreme Hitze übersteht. Er überlebte. Die Kapsel fast nicht."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Strecke legte Stephensons Lokomotive 'Rocket' bei den Rainhill-Trials 1829 zurück und welche Höchstgeschwindigkeit erreichte sie?",
        answerA = "10 km / 29 km/h",
        answerB = "35 km / 47 km/h",
        answerC = "60 km / 58 km/h",
        answerD = "100 km / 82 km/h",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die Rocket legte beim Wettbewerb in Rainhill 35 km zurück und erreichte eine Höchstgeschwindigkeit von 47 km/h — sensationell für 1829. Sie gewann das Preisgeld von 500 Pfund und sicherte Stephenson den Auftrag für die Liverpool-Manchester-Bahn.",
        funFact = "Bei der Eröffnung der Liverpool-Manchester-Bahn 1830 wurde der Parlamentsabgeordnete William Huskisson vom ersten Zug überfahren und starb — das erste bekannte Todesopfer einer Dampflokomotive in der Geschichte."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer überquerte 1878–1879 als erster die Nordostpassage durch das sibirische Eismeer von Europa nach Asien?",
        answerA = "Fridtjof Nansen",
        answerB = "Adolf Erik Nordenskiöld",
        answerC = "John Franklin",
        answerD = "Roald Amundsen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Der schwedische Polarforscher Adolf Erik Nordenskiöld bezwang 1878/79 mit dem Schiff Vega als erster die Nordostpassage — die Seestraße nördlich von Sibirien zwischen Europa und dem Pazifik.",
        funFact = "Die Nordostpassage ist heute durch den Klimawandel und das Abschmelzen des Polareises immer häufiger eisfrei — russische und chinesische Frachter nutzen sie als Abkürzung zwischen Europa und Ostasien."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Erfinder meldete 1876 das Patent für das Telefon an und eröffnete damit eine neue Ära der Kommunikation?",
        answerA = "Alexander Graham Bell",
        answerB = "Nikola Tesla",
        answerC = "Elisha Gray",
        answerD = "Thomas Edison",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Alexander Graham Bell meldete am 14. Februar 1876 das Patent für das Telefon an — wenige Stunden vor seinem Konkurrenten Elisha Gray. Das Telefon revolutionierte Kommunikation und Transport-Koordination weltweit.",
        funFact = "Bell meldete das Patent am selben Tag an wie sein Rivale Gray — Bell war nur 2 Stunden früher beim Patentamt. Gray protestierte, verlor aber den Kampf. Bis heute ist umstritten, wer das Telefon wirklich 'zuerst' erfunden hat."
    ),

    Question(
        categoryId = 11,
        questionText = "Wie hieß das erste Dampfschiff, das 1807 als reguläres Linienboot zwischen New York und Albany auf dem Hudson River fuhr?",
        answerA = "SS Great Western",
        answerB = "Clermont",
        answerC = "SS Savannah",
        answerD = "Comet",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Robert Fultons Clermont fuhr ab 1807 regelmäßig auf dem Hudson River zwischen New York und Albany — 240 km in 32 Stunden. Es war das erste kommerziell erfolgreiche Dampfschiff der Welt.",
        funFact = "Beim ersten Anblick des dampfenden und krachenden Schiffes flohen Bewohner der Flussufer panikartig — sie glaubten, ein Seeungeheuer oder der Teufel selbst käme den Fluss hinauf."
    ),

)

