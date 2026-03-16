package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun foodQuestions(): List<Question> = listOf(

    // EASY (10)
    Question(
        categoryId = 8,
        questionText = "Welches Getreide wird für die Herstellung von Bier hauptsächlich verwendet?",
        answerA = "Weizen",
        answerB = "Gerste",
        answerC = "Roggen",
        answerD = "Hafer",
        correctAnswer = 1,
        explanation = "Gerste ist das klassische Braugetreide und wird nach dem Mälzen als Malz zur Bierherstellung verwendet.",
        difficulty = 1,
        funFact = "Deutschland hat über 1.500 Brauereien – mehr als jedes andere Land der Welt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Hauptbestandteil von Guacamole?",
        answerA = "Tomate",
        answerB = "Zwiebel",
        answerC = "Avocado",
        answerD = "Paprika",
        correctAnswer = 2,
        explanation = "Guacamole ist eine mexikanische Sauce, deren Grundlage zerdrückte Avocados sind.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Aus welchem Land stammt die Pizza Margherita ursprünglich?",
        answerA = "Spanien",
        answerB = "Griechenland",
        answerC = "Frankreich",
        answerD = "Italien",
        correctAnswer = 3,
        explanation = "Die Pizza Margherita stammt aus Neapel, Italien, und wurde angeblich 1889 zu Ehren der Königin Margherita kreiert.",
        difficulty = 1,
        funFact = "Die Farben der Pizza Margherita – Rot, Weiß und Grün – spiegeln die italienische Flagge wider."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Frucht wird für die Herstellung von Wein verwendet?",
        answerA = "Äpfel",
        answerB = "Trauben",
        answerC = "Kirschen",
        answerD = "Pflaumen",
        correctAnswer = 1,
        explanation = "Wein wird traditionell aus fermentierten Weintrauben hergestellt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Sushi?",
        answerA = "Ein chinesisches Nudelgericht",
        answerB = "Ein koreanisches Fleischgericht",
        answerC = "Ein japanisches Gericht mit gesäuertem Reis",
        answerD = "Ein thailändisches Currygericht",
        correctAnswer = 2,
        explanation = "Sushi ist ein japanisches Gericht, das aus mit Essig gewürztem Reis besteht, oft kombiniert mit Meeresfrüchten oder Gemüse.",
        difficulty = 1,
        funFact = "Das Wort 'Sushi' bezieht sich auf den gesäuerten Reis, nicht auf den Fisch."
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Gewürz gibt dem Curry seine gelbe Farbe?",
        answerA = "Paprika",
        answerB = "Safran",
        answerC = "Kurkuma",
        answerD = "Ingwer",
        correctAnswer = 2,
        explanation = "Kurkuma (Gelbwurz) enthält den Farbstoff Curcumin, der Curry seine charakteristische gelbe Farbe verleiht.",
        difficulty = 1,
        funFact = "Kurkuma wird seit über 4.000 Jahren in der indischen Medizin und Küche verwendet."
    ),

    Question(
        categoryId = 8,
        questionText = "Wie heißt das traditionelle deutsche Gericht aus Schweinefleisch und Sauerkraut?",
        answerA = "Rouladen",
        answerB = "Kassler mit Sauerkraut",
        answerC = "Sauerbraten",
        answerD = "Schnitzel",
        correctAnswer = 1,
        explanation = "Kassler ist gepökeltes und geräuchertes Schweinefleisch, das traditionell mit Sauerkraut serviert wird.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Nuss steckt in einem Nuss-Nougat-Aufstrich wie Nutella?",
        answerA = "Mandel",
        answerB = "Walnuss",
        answerC = "Haselnuss",
        answerD = "Cashew",
        correctAnswer = 2,
        explanation = "Nutella besteht hauptsächlich aus Haselnüssen und Kakao – Haselnüsse machen etwa 13 % der Rezeptur aus.",
        difficulty = 1,
        funFact = "Jedes Jahr werden für die Produktion von Nutella etwa 25 % der weltweiten Haselnussernte verwendet."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist ein Espresso?",
        answerA = "Kaffee mit viel Milch",
        answerB = "Stark konzentrierter Kaffee in kleiner Menge",
        answerC = "Kaffee mit Schokolade",
        answerD = "Entkoffeinierter Kaffee",
        correctAnswer = 1,
        explanation = "Espresso ist ein stark konzentriertes Kaffeegetränk, das durch Pressen von heißem Wasser durch fein gemahlenes Kaffeepulver hergestellt wird.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Tier liefert uns Mozzarella in seiner ursprünglichsten Form?",
        answerA = "Kuh",
        answerB = "Schaf",
        answerC = "Ziege",
        answerD = "Büffel",
        correctAnswer = 3,
        explanation = "Echter Mozzarella di Bufala wird traditionell aus Büffelmilch hergestellt. Kuhmilch-Mozzarella ist eine verbreitete, günstigere Alternative.",
        difficulty = 1,
        funFact = "Die Büffel in der Campania-Region Italiens produzieren Milch mit einem höheren Fettgehalt als Kuhmilch."
    ),

    // MEDIUM (15)
    Question(
        categoryId = 8,
        questionText = "Welche zwei Hauptzutaten werden für Mayonnaise benötigt?",
        answerA = "Öl und Essig",
        answerB = "Eigelb und Öl",
        answerC = "Sahne und Senf",
        answerD = "Joghurt und Zitrone",
        correctAnswer = 1,
        explanation = "Mayonnaise ist eine Emulsion aus Eigelb und Öl, die durch langsames Einrühren des Öls in das Eigelb entsteht.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was versteht man unter dem Begriff 'Al dente' bei Pasta?",
        answerA = "Sehr weich gekocht",
        answerB = "Mit viel Salz gewürzt",
        answerC = "Leicht bissfest gekocht",
        answerD = "Mit Knoblauch verfeinert",
        correctAnswer = 2,
        explanation = "'Al dente' bedeutet auf Italienisch 'auf den Zahn' und bezeichnet Pasta, die noch leicht bissfest ist.",
        difficulty = 2,
        funFact = "Al-dente-Pasta hat einen niedrigeren glykämischen Index als weichgekochte Pasta."
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Land ist der weltgrößte Kaffeeproduzent?",
        answerA = "Kolumbien",
        answerB = "Äthiopien",
        answerC = "Brasilien",
        answerD = "Vietnam",
        correctAnswer = 2,
        explanation = "Brasilien ist mit großem Abstand der weltgrößte Kaffeeproduzent und liefert etwa ein Drittel des weltweiten Kaffees.",
        difficulty = 2,
        funFact = "Brasilien produziert so viel Kaffee, dass das Land den Weltmarktpreis maßgeblich beeinflusst."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Kimchi?",
        answerA = "Japanisches Reisgericht",
        answerB = "Koreanisches fermentiertes Gemüse",
        answerC = "Chinesische Suppe",
        answerD = "Vietnamesischer Salat",
        correctAnswer = 1,
        explanation = "Kimchi ist ein koreanisches Nationalgericht, das aus fermentiertem Gemüse – meist Chinakohl – mit Gewürzen besteht.",
        difficulty = 2,
        funFact = "In Südkorea gibt es ein Kimchi-Museum und sogar einen Kimchi-Feiertag."
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Lebensmittel hat den höchsten Vitamin-C-Gehalt unter diesen Optionen?",
        answerA = "Orange",
        answerB = "Zitrone",
        answerC = "Hagebutte",
        answerD = "Kiwi",
        correctAnswer = 2,
        explanation = "Hagebutten enthalten mit bis zu 1.250 mg Vitamin C pro 100g deutlich mehr als Orangen (50 mg) oder Zitronen (53 mg).",
        difficulty = 2,
        funFact = "Eine einzige Hagebutte enthält mehr Vitamin C als eine ganze Orange."
    ),

    Question(
        categoryId = 8,
        questionText = "Aus welcher Region Deutschlands stammt der Schwarzwälder Schinken?",
        answerA = "Bayern",
        answerB = "Thüringen",
        answerC = "Schwarzwald (Baden-Württemberg)",
        answerD = "Westfalen",
        correctAnswer = 2,
        explanation = "Schwarzwälder Schinken ist eine geschützte geografische Angabe und stammt ausschließlich aus dem Schwarzwald in Baden-Württemberg.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Getränk wird aus fermentiertem Tee hergestellt?",
        answerA = "Kefir",
        answerB = "Kombucha",
        answerC = "Ayran",
        answerD = "Lassi",
        correctAnswer = 1,
        explanation = "Kombucha ist ein fermentiertes Teegetränk, das durch einen Symbiosepilz aus Bakterien und Hefen (SCOBY) hergestellt wird.",
        difficulty = 2,
        funFact = "Kombucha wird seit über 2.000 Jahren in Asien konsumiert und wurde als 'Tee des ewigen Lebens' bezeichnet."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Foie Gras?",
        answerA = "Französische Käsesorte",
        answerB = "Fettleber von Ente oder Gans",
        answerC = "Gebratenes Lammfleisch",
        answerD = "Trüffelsorte",
        correctAnswer = 1,
        explanation = "Foie Gras (Fettleber) ist eine Delikatesse aus der Leber von Enten oder Gänsen, die durch Stopfmast vergrößert wurde.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welcher Käse wird traditionell für ein echtes Wiener Schnitzel verwendet – zum Überbacken?",
        answerA = "Parmesan",
        answerB = "Emmentaler",
        answerC = "Gar keiner – ein echtes Wiener Schnitzel wird nicht überbacken",
        answerD = "Gouda",
        correctAnswer = 2,
        explanation = "Ein echtes Wiener Schnitzel aus Kalbfleisch wird nicht überbacken. Das 'Schnitzel Wiener Art' mit Käse ist eine abgewandelte Version.",
        difficulty = 2,
        funFact = "Das Wiener Schnitzel muss laut österreichischen Vorschriften aus Kalbfleisch bestehen, um sich so nennen zu dürfen."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Umami?",
        answerA = "Eine japanische Würzsauce",
        answerB = "Der fünfte Grundgeschmack",
        answerC = "Eine Kochmethode",
        answerD = "Eine Reissorte",
        correctAnswer = 1,
        explanation = "Umami ist der fünfte Grundgeschmack neben süß, sauer, salzig und bitter. Er wird als herzhaft-würzig beschrieben.",
        difficulty = 2,
        funFact = "Der Begriff 'Umami' wurde 1908 vom japanischen Chemiker Kikunae Ikeda geprägt."
    ),

    Question(
        categoryId = 8,
        questionText = "Wie viel Prozent Kakaoanteil muss Bitterschokolade mindestens haben?",
        answerA = "35 %",
        answerB = "50 %",
        answerC = "70 %",
        answerD = "85 %",
        correctAnswer = 1,
        explanation = "Nach EU-Recht muss Bitterschokolade mindestens 35 % Kakaoanteil haben. Für 'Zartbitter' sind es mindestens 43 %.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Öl wird traditionell in der mediterranen Küche bevorzugt verwendet?",
        answerA = "Sonnenblumenöl",
        answerB = "Rapsöl",
        answerC = "Olivenöl",
        answerD = "Kokosöl",
        correctAnswer = 2,
        explanation = "Olivenöl ist ein Grundpfeiler der mediterranen Küche und wird seit Jahrtausenden im Mittelmeerraum angebaut und verwendet.",
        difficulty = 2,
        funFact = "Griechenland hat den höchsten Pro-Kopf-Verbrauch an Olivenöl weltweit."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist ein Boeuf Bourguignon?",
        answerA = "Ein belgisches Kartoffelgericht",
        answerB = "Ein französisches Rindfleischragout mit Rotwein",
        answerC = "Ein Schweizer Käsefondue",
        answerD = "Ein spanisches Lammgericht",
        correctAnswer = 1,
        explanation = "Boeuf Bourguignon ist ein klassisches französisches Schmorgericht aus Rindfleisch, das in Burgunder-Rotwein geschmort wird.",
        difficulty = 2,
        funFact = "Das Gericht stammt aus der Bourgogne-Region und wurde durch Julia Childs Kochbuch in Amerika bekannt."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Pflanze liefert das Gewürz Vanille?",
        answerA = "Ein Kaktus",
        answerB = "Ein Baum",
        answerC = "Eine Orchidee",
        answerD = "Ein Strauch",
        correctAnswer = 2,
        explanation = "Vanille stammt von der Kletterpflanze Vanilla planifolia, einer Orchideenart. Die Schoten sind die fermentierten Früchte dieser Orchidee.",
        difficulty = 2,
        funFact = "Vanille ist nach Safran das zweitteuerste Gewürz der Welt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was unterscheidet Champagner von normalem Sekt?",
        answerA = "Champagner hat mehr Alkohol",
        answerB = "Champagner kommt ausschließlich aus der Region Champagne in Frankreich",
        answerC = "Champagner wird aus anderen Trauben hergestellt",
        answerD = "Champagner wird ohne Hefe hergestellt",
        correctAnswer = 1,
        explanation = "Champagner ist eine geschützte Herkunftsbezeichnung und darf nur aus der Champagne in Frankreich stammen. Alle anderen Schaumweine sind Sekt oder Crémant.",
        difficulty = 2,
        funFact = null
    ),

    // HARD (12)
    Question(
        categoryId = 8,
        questionText = "Welche Maillard-Reaktion beschreibt das Bräunen von Fleisch beim Braten?",
        answerA = "Eine Oxidationsreaktion zwischen Fetten und Sauerstoff",
        answerB = "Eine Reaktion zwischen Aminosäuren und Zuckern bei Hitze",
        answerC = "Eine Fermentationsreaktion durch Bakterien",
        answerD = "Eine Karamellisierung von Proteinen",
        correctAnswer = 1,
        explanation = "Die Maillard-Reaktion ist eine chemische Reaktion zwischen Aminosäuren und reduzierenden Zuckern, die beim Erhitzen die typische braune Färbung und Aromabildung erzeugt.",
        difficulty = 3,
        funFact = "Die Maillard-Reaktion wurde 1912 vom französischen Arzt Louis-Camille Maillard entdeckt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Koji in der japanischen Küche?",
        answerA = "Eine Fischsauce",
        answerB = "Ein Schimmelpilz (Aspergillus oryzae), der für Fermentation verwendet wird",
        answerC = "Eine Reissorte",
        answerD = "Eine Schneidtechnik",
        correctAnswer = 1,
        explanation = "Koji (Aspergillus oryzae) ist ein Schimmelpilz, der auf Reis oder Soja gezüchtet wird und essentielle Grundlage für Sake, Miso und Sojasauce ist.",
        difficulty = 3,
        funFact = "Koji wird manchmal als 'nationaler Schimmelpilz Japans' bezeichnet."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Weinlage gilt als die kleinste Großlage Deutschlands und produziert einen der teuersten deutschen Weine?",
        answerA = "Scharzhofberg",
        answerB = "Bernkasteler Doctor",
        answerC = "Schloss Johannisberg",
        answerD = "Trockenbeerenauslese vom Rhein",
        correctAnswer = 1,
        explanation = "Bernkasteler Doctor ist eine der bekanntesten und teuersten Weinlagen Deutschlands an der Mosel, mit nur 3,26 Hektar Fläche.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Sous-vide-Garen?",
        answerA = "Garen über offener Flamme",
        answerB = "Garen im Dampfgarer",
        answerC = "Garen vakuumverpackter Lebensmittel im Wasserbad bei niedrigen Temperaturen",
        answerD = "Garen in einer Salzteigkruste",
        correctAnswer = 2,
        explanation = "Sous vide (französisch: 'unter Vakuum') ist eine Garmethode, bei der Lebensmittel vakuumversiegelt bei präzisen Niedrigtemperaturen im Wasserbad gegart werden.",
        difficulty = 3,
        funFact = "Die Methode wurde in den 1970er Jahren von Georges Pralus entwickelt, um Foie Gras besser zuzubereiten."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Käsesorte wird ausschließlich in der Region Roquefort-sur-Soulzon hergestellt und mit Penicillium roqueforti gereift?",
        answerA = "Gorgonzola",
        answerB = "Stilton",
        answerC = "Roquefort",
        answerD = "Fourme d'Ambert",
        correctAnswer = 2,
        explanation = "Roquefort ist ein französischer Blauschimmelkäse aus Schafsmilch, der in den Höhlen von Combalou gereift wird und eine geschützte Ursprungsbezeichnung trägt.",
        difficulty = 3,
        funFact = "Roquefort war einer der ersten Käse, der eine AOC-Zertifizierung erhielt – bereits 1925."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Tempering bei Schokolade?",
        answerA = "Das Hinzufügen von Zucker zur Schokolade",
        answerB = "Ein gezielter Erhitzungs- und Abkühlungsprozess für eine stabile Kristallstruktur",
        answerC = "Das Schmelzen von Schokolade im Wasserbad",
        answerD = "Das Conchieren von Kakaomassse",
        correctAnswer = 1,
        explanation = "Beim Tempering wird Schokolade auf definierte Temperaturen erhitzt und abgekühlt, um die Beta-V-Kristallform des Kakaobutterfetts zu fördern – das gibt Glanz, Knack und Schmelz.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Enzym macht frische Ananas ungeeignet für Gelatinedesserts?",
        answerA = "Amylase",
        answerB = "Bromelain",
        answerC = "Papain",
        answerD = "Lipase",
        correctAnswer = 1,
        explanation = "Bromelain ist eine Protease in frischer Ananas, die Gelatineproteine abbaut und damit das Gelieren verhindert. Erhitzte Ananas (Dose) hat dieses Enzym nicht mehr.",
        difficulty = 3,
        funFact = "Bromelain wird in der Medizin als entzündungshemmendes Mittel eingesetzt."
    ),

    Question(
        categoryId = 8,
        questionText = "Wie nennt man das traditionelle österreichische Gericht aus Kalbslunge und -herz in einer sauren Rahmsauce?",
        answerA = "Tafelspitz",
        answerB = "Zwiebelrostbraten",
        answerC = "Beuschel",
        answerD = "Stelze",
        correctAnswer = 2,
        explanation = "Beuschel ist ein österreichisches Innereien-Ragout aus Kalbslunge und Kalbsherz in einer sauren Sahnesauce mit Kapern und Gewürzgurken.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Unterschied zwischen Arabica und Robusta bei Kaffeebohnen?",
        answerA = "Arabica hat mehr Koffein und einen stärkeren Geschmack",
        answerB = "Arabica hat weniger Koffein, mehr Aromen; Robusta ist kräftiger und bitterer",
        answerC = "Beide Sorten sind identisch – der Name unterscheidet nur die Herkunft",
        answerD = "Robusta wächst nur in Südamerika",
        correctAnswer = 1,
        explanation = "Arabica (Coffea arabica) hat etwa halb so viel Koffein wie Robusta, aber ein komplexeres Aromaprofil. Robusta ist kräftiger, bitterer und wird oft in Espresso-Blends verwendet.",
        difficulty = 3,
        funFact = "Arabica-Bohnen machen etwa 60–70 % der weltweiten Kaffeeproduktion aus."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Spherification' in der Molekularküche?",
        answerA = "Das Zerstäuben von Flüssigkeiten in einem Aerosol",
        answerB = "Eine Technik, die Flüssigkeiten in kugelförmige Gele verwandelt",
        answerC = "Das Gefrieren von Lebensmitteln in Stickstoff",
        answerD = "Eine Methode zum Schäumen von Suppen",
        correctAnswer = 1,
        explanation = "Spherification ist eine Technik der Molekularküche, bei der Natriumalginat und Calciumchlorid verwendet werden, um Flüssigkeiten in eine Gelschicht einzuhüllen und so Kugeln ('Kaviar') zu erzeugen.",
        difficulty = 3,
        funFact = "Die Technik wurde von Ferran Adrià im Restaurant El Bulli populär gemacht."
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Lebensmittelzusatzstoffkürzel steht für Glutamat?",
        answerA = "E 300",
        answerB = "E 621",
        answerC = "E 440",
        answerD = "E 951",
        correctAnswer = 1,
        explanation = "E 621 ist die EU-Kennzeichnung für Mononatriumglutamat (MSG), das als Geschmacksverstärker eingesetzt wird und den Umami-Geschmack intensiviert.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Technik wird beim Conchieren von Schokolade angewendet?",
        answerA = "Schokolade wird bei sehr hohen Temperaturen kurz erhitzt",
        answerB = "Schokoladenmasse wird stundenlang unter Wärme gemischt und belüftet",
        answerC = "Kakaobohnen werden getrocknet",
        answerD = "Schokolade wird mit Milchpulver vermischt",
        correctAnswer = 1,
        explanation = "Beim Conchieren wird Schokoladenmasse bis zu 78 Stunden lang erhitzt, geknetet und belüftet, um unerwünschte Aromen zu entfernen und Feinheit zu erreichen.",
        difficulty = 3,
        funFact = "Rodolphe Lindt erfand 1879 die Conchiermaschine, was den Durchbruch für zarte Schokolade bedeutete."
    ),

    // EXPERT (8)
    Question(
        categoryId = 8,
        questionText = "Welcher Wein-Klassifikation unterwirft sich das Château Pétrus, eines der teuersten Bordeaux?",
        answerA = "Premier Grand Cru Classé A",
        answerB = "Es ist nicht in die offizielle Klassifikation von 1855 eingestuft",
        answerC = "Grand Cru Classé",
        answerD = "Deuxième Grand Cru Classé",
        correctAnswer = 1,
        explanation = "Pétrus aus dem Pomerol-Appellation gehört keiner offiziellen Klassifikation an, da Pomerol nie klassifiziert wurde – trotzdem gilt es als eines der wertvollsten Weine der Welt.",
        difficulty = 4,
        funFact = "Eine Flasche Pétrus 1945 wurde für über 195.000 Euro versteigert."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Nduja'?",
        answerA = "Ein sizilianisches Olivenöl mit Basilikum",
        answerB = "Eine streichfähige, sehr scharfe Salami aus Kalabrien",
        answerC = "Ein sardischer Hartkäse",
        answerD = "Ein neapolitanisches Brotgebäck",
        correctAnswer = 1,
        explanation = "'Nduja ist eine kalabrische Spezialität: eine streichfähige, sehr pikante Rohwurst aus Schweinefleisch und einer großen Menge Chilischoten.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Aminosäure ist für den stechenden Geruch von gebratenen Zwiebeln hauptverantwortlich?",
        answerA = "Glutamin",
        answerB = "Cystein-Sulfoxid-Verbindungen (z.B. Propenylcysteinsulfoxid)",
        answerC = "Lysin",
        answerD = "Tryptophan",
        correctAnswer = 1,
        explanation = "Zwiebeln enthalten Cysteinsulfoxid-Verbindungen. Beim Erhitzen entstehen durch enzymatische Reaktionen Thiosulfinate und andere schwefelhaltige Verbindungen, die den typischen Geruch erzeugen.",
        difficulty = 4,
        funFact = "Beim Schneiden von Zwiebeln entsteht Propanthial-S-oxid, das die Augen zum Tränen bringt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Unterschied zwischen 'Brut Nature' und 'Demi-Sec' bei Champagner?",
        answerA = "Brut Nature enthält keinerlei zugesetzten Zucker (<3g/l); Demi-Sec ist deutlich süßer (32–50 g/l)",
        answerB = "Brut Nature ist teurer als Demi-Sec",
        answerC = "Brut Nature kommt aus dem Norden der Champagne",
        answerD = "Es gibt keinen Unterschied – beides bezeichnet trockenen Champagner",
        correctAnswer = 0,
        explanation = "Champagner-Kategorien nach Zuckergehalt: Brut Nature 0–3 g/l, Extra Brut 0–6 g/l, Brut 0–12 g/l, Extra Dry 12–17 g/l, Sec 17–32 g/l, Demi-Sec 32–50 g/l.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welches natürliche Emulgiermittel wird aus Sojalecithin oder Eigelb gewonnen und in der Lebensmittelindustrie weit verbreitet eingesetzt?",
        answerA = "Xanthan",
        answerB = "Lecithin (E 322)",
        answerC = "Carrageen (E 407)",
        answerD = "Guar-Gum (E 412)",
        correctAnswer = 1,
        explanation = "Lecithin (E 322) ist ein natürlicher Emulgator, der aus Soja oder Eigelb extrahiert wird. Es stabilisiert Emulsionen wie Mayonnaise oder Schokolade.",
        difficulty = 4,
        funFact = "Das Wort 'Lecithin' kommt vom griechischen 'lekithos' (Eigelb)."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Kopi Luwak' und warum ist er so teuer?",
        answerA = "Ein vietnamesischer Kaffee, der im Dunkeln fermentiert wird",
        answerB = "Ein Kaffee aus Kaffeebohnen, die vom Luwak (Fleckenmusang) gefressen und wieder ausgeschieden werden",
        answerC = "Ein sehr seltener äthiopischer Wildkaffee",
        answerD = "Ein japanischer Kaffee mit spezieller Röstmethode",
        correctAnswer = 1,
        explanation = "Kopi Luwak ist ein indonesischer Kaffee, bei dem Kaffeebohnen vom Fleckenmusang (Luwak) gefressen, im Verdauungstrakt fermentiert und dann aus dem Kot gesammelt werden.",
        difficulty = 4,
        funFact = "Kopi Luwak kann bis zu 700 Euro pro Kilogramm kosten."
    ),

    Question(
        categoryId = 8,
        questionText = "Was bezeichnet man in der Patisserie als 'Ganache'?",
        answerA = "Eine Zuckerglasurmasse",
        answerB = "Eine Emulsion aus Schokolade und Sahne (Fett)",
        answerC = "Eine Buttercremetorte",
        answerD = "Ein Biskuitteig mit Mandeln",
        correctAnswer = 1,
        explanation = "Ganache ist eine Emulsion aus Schokolade und Sahne (manchmal auch Butter), die als Füllung, Überzug oder Trüffelkern verwendet wird.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welcher Prozess macht Oliven essbar, die roh aufgrund von Oleuropein extrem bitter sind?",
        answerA = "Pasteurisierung",
        answerB = "Laktofermentation oder Lauge-Behandlung (Entbittern)",
        answerC = "Räuchern",
        answerD = "Gefriertrocknung",
        correctAnswer = 1,
        explanation = "Rohe Oliven enthalten Oleuropein, das extrem bitter ist. Durch Einlegen in Salzlake (Laktofermentation) oder Natriumhydroxid-Lösung wird das Oleuropein abgebaut.",
        difficulty = 4,
        funFact = "Dieser Entbitterungsprozess kann je nach Methode zwischen wenigen Stunden und mehreren Monaten dauern."
    ),

    // MASTER (5)
    Question(
        categoryId = 8,
        questionText = "Welche spezifische chemische Verbindung ist hauptverantwortlich für den Geschmack von Vanille?",
        answerA = "Eugenol",
        answerB = "Vanillin (4-Hydroxy-3-methoxybenzaldehyd)",
        answerC = "Thymol",
        answerD = "Linalool",
        correctAnswer = 1,
        explanation = "Vanillin (4-Hydroxy-3-methoxybenzaldehyd) ist die Hauptaromaverbindung in Vanille. Heute wird es hauptsächlich synthetisch oder aus Lignin gewonnen.",
        difficulty = 5,
        funFact = "Synthetisches Vanillin ist chemisch identisch mit natürlichem Vanillin aus Vanilleschoten."
    ),

    Question(
        categoryId = 8,
        questionText = "Was beschreibt das 'Hautes Côtes de Nuits'-Appellation im Burgund?",
        answerA = "Einen Grand-Cru-Weinberg in Gevrey-Chambertin",
        answerB = "Dörfer auf den Hochplateaus westlich der Côte de Nuits mit regionalem AOC-Status",
        answerC = "Die Kategorie für alle Pinot-Noir-Weine aus dem Burgund",
        answerD = "Einen Premier-Cru-Weinberg in Vosne-Romanée",
        correctAnswer = 1,
        explanation = "Hautes Côtes de Nuits bezeichnet Weine aus Dörfern auf den Höhenzügen westlich der berühmten Côte de Nuits. Sie haben einen Regional-AOC-Status und sind oft preiswerter als die klassischen Côte-de-Nuits-Weine.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welcher chemische Prozess erklärt, warum Fleisch nach dem Schlachten zunächst hart (Totenstarre) wird und dann durch Reifung wieder zart?",
        answerA = "Oxidation der Proteine durch Sauerstoff",
        answerB = "ATP-Abbau führt zu Aktomyosin-Bindung; endogene Proteasen (Calpaine, Cathepsine) bauen dann Myofibrillen ab",
        answerC = "Bakterielle Fermentation der Muskelfasern",
        answerD = "Koagulation von Kollagen durch Körperwärme",
        correctAnswer = 1,
        explanation = "Nach dem Tod erschöpft sich ATP, was zu Aktomyosin-Bindung (Totenstarre) führt. Beim Reifen bauen Proteasen (Calpaine, Cathepsine) Myofibrillen ab, das Fleisch wird wieder zart.",
        difficulty = 5,
        funFact = "Das sogenannte 'Dry Aged'-Fleisch wird oft 28–120 Tage gereift, um maximale Zartheit und Aromatik zu entwickeln."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Terroir' im Weinbau und welche Faktoren umfasst es?",
        answerA = "Ausschließlich die Bodenart eines Weinbergs",
        answerB = "Die Gesamtheit aller Umweltfaktoren (Boden, Klima, Mikroklima, Topografie, Weinbergspraxis), die den Charakter eines Weins prägen",
        answerC = "Die Rebsorte und das Alter der Reben",
        answerD = "Die Kellerausstattung und Reifetechnik",
        correctAnswer = 1,
        explanation = "Terroir umfasst alle natürlichen Faktoren eines Weinbergs: Geologie, Bodenstruktur, Topografie, Makro- und Mikroklima sowie menschliche Einflüsse wie Rebschnitt und Erziehungssystem.",
        difficulty = 5,
        funFact = "Der Begriff 'Terroir' stammt aus dem Französischen und hat keine direkte Übersetzung in andere Sprachen."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Transglutaminase' und wozu wird sie in der Lebensmittelindustrie eingesetzt?",
        answerA = "Ein Süßungsmittel auf Basis von Gluten",
        answerB = "Ein Enzym, das Proteine durch kovalente Bindungen verbindet und zum 'Verkleben' von Fleischstücken verwendet wird",
        answerC = "Ein Konservierungsstoff für Fleischprodukte",
        answerD = "Ein Emulgator für Wurstwaren",
        correctAnswer = 1,
        explanation = "Transglutaminase (oft 'Fleischkleber' genannt) ist ein Enzym, das Glutamin- und Lysin-Reste in Proteinen vernetzt und so Fleischstücke oder Proteine zusammenklebt. Es wird für 'restructured meat'-Produkte verwendet.",
        difficulty = 5,
        funFact = "Transglutaminase kommt natürlich in menschlichen Blutzellen vor und spielt eine Rolle bei der Blutgerinnung."
    ),

    // NEW EASY (15)
    Question(
        categoryId = 8,
        questionText = "Welche Frucht ist die Hauptzutat von Apfelstrudel?",
        answerA = "Birne",
        answerB = "Pflaume",
        answerC = "Apfel",
        answerD = "Kirsche",
        correctAnswer = 2,
        explanation = "Apfelstrudel wird aus einem hauchdünnen Strudelteig mit einer Füllung aus Äpfeln, Zucker, Rosinen und Zimt hergestellt.",
        difficulty = 1,
        funFact = "Der Wiener Apfelstrudel ist seit 2008 als immaterielles Kulturerbe Österreichs anerkannt."
    ),

    Question(
        categoryId = 8,
        questionText = "Wie nennt man das Einmachen von Lebensmitteln in Salz oder Salzlake?",
        answerA = "Räuchern",
        answerB = "Pökeln",
        answerC = "Marinieren",
        answerD = "Blanchieren",
        correctAnswer = 1,
        explanation = "Pökeln ist eine Konservierungsmethode, bei der Fleisch oder Fisch durch Salz, manchmal mit Nitritpökelsalz, haltbar gemacht wird.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Getränk wird aus gepressten Äpfeln hergestellt?",
        answerA = "Traubensaft",
        answerB = "Orangensaft",
        answerC = "Apfelsaft",
        answerD = "Birnensaft",
        correctAnswer = 2,
        explanation = "Apfelsaft wird durch Pressen von Äpfeln gewonnen. Je nach Verarbeitung entsteht naturtrüber oder geklärter Apfelsaft.",
        difficulty = 1,
        funFact = "Deutschland ist einer der größten Apfelsaft-Produzenten weltweit."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Tofu?",
        answerA = "Ein fermentiertes Reisprodukt",
        answerB = "Geronnene und gepresste Sojamilch",
        answerC = "Getrocknete Meeresfrüchte",
        answerD = "Ein Weizenprodukt",
        correctAnswer = 1,
        explanation = "Tofu wird hergestellt, indem Sojamilch zum Gerinnen gebracht und die entstehende Masse gepresst wird – ähnlich wie Käse aus Kuhmilch.",
        difficulty = 1,
        funFact = "Tofu enthält alle neun essentiellen Aminosäuren und ist eine wichtige Proteinquelle in der asiatischen Küche."
    ),

    Question(
        categoryId = 8,
        questionText = "Wie heißt die bekannteste deutsche Bratwurst aus Thüringen?",
        answerA = "Nürnberger Rostbratwurst",
        answerB = "Currywurst",
        answerC = "Thüringer Rostbratwurst",
        answerD = "Weißwurst",
        correctAnswer = 2,
        explanation = "Die Thüringer Rostbratwurst ist eine der ältesten und bekanntesten deutschen Bratwürste, gewürzt u. a. mit Majoran und Kümmel.",
        difficulty = 1,
        funFact = "Das älteste bekannte Rezept der Thüringer Rostbratwurst stammt aus dem Jahr 1404."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Milch wird für die Herstellung von Butter verwendet?",
        answerA = "Kokosmilch",
        answerB = "Sojamilch",
        answerC = "Mandelmilch",
        answerD = "Kuhmilch (Rahm)",
        correctAnswer = 3,
        explanation = "Butter wird durch Aufrahmen und Verbuttern von Kuhmilch hergestellt. Der Rahm (Sahne) enthält den hohen Fettanteil, der für Butter nötig ist.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Pesto alla Genovese?",
        answerA = "Eine Tomatensauce aus Genua",
        answerB = "Eine Sauce aus Basilikum, Pinienkernen, Parmesan, Knoblauch und Olivenöl",
        answerC = "Ein Nudelteig mit Spinat",
        answerD = "Eine Fleischsauce aus Norditalien",
        correctAnswer = 1,
        explanation = "Pesto alla Genovese ist eine unerhitzte Sauce aus frischem Basilikum, Pinienkernen, Parmigiano Reggiano, Knoblauch und nativem Olivenöl extra.",
        difficulty = 1,
        funFact = "Der Name 'Pesto' leitet sich vom Verb 'pestare' (zerstampfen) ab – traditionell wird Pesto im Mörser zubereitet."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Farbe hat reifer Emmentaler Käse?",
        answerA = "Weiß",
        answerB = "Blau",
        answerC = "Hellgelb",
        answerD = "Orange",
        correctAnswer = 2,
        explanation = "Emmentaler hat eine hellgelbe bis elfenbeinfarbene Masse und ist bekannt für seine charakteristischen Löcher (Augen), die durch CO₂-produzierende Bakterien entstehen.",
        difficulty = 1,
        funFact = "Die Löcher im Emmentaler entstehen durch Propionsäurebakterien, die CO₂ produzieren."
    ),

    Question(
        categoryId = 8,
        questionText = "Was versteht man unter 'Pasteurisieren'?",
        answerA = "Lebensmittel einfrieren",
        answerB = "Lebensmittel kurz stark erhitzen um Keime abzutöten",
        answerC = "Lebensmittel mit Salz konservieren",
        answerD = "Lebensmittel vakuumieren",
        correctAnswer = 1,
        explanation = "Pasteurisieren ist ein Verfahren, bei dem Lebensmittel kurzzeitig auf 72–85 °C erhitzt werden, um Krankheitserreger abzutöten und die Haltbarkeit zu verlängern.",
        difficulty = 1,
        funFact = "Das Verfahren wurde nach Louis Pasteur benannt, der es im 19. Jahrhundert entwickelte."
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Gewürz wird aus getrocknetem Chili gewonnen?",
        answerA = "Kreuzkümmel",
        answerB = "Paprikapulver",
        answerC = "Cayennepfeffer",
        answerD = "Koriander",
        correctAnswer = 2,
        explanation = "Cayennepfeffer wird aus getrockneten und gemahlenen Cayennechilis hergestellt und ist für seine starke Schärfe bekannt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist ein Croissant?",
        answerA = "Ein italienisches Gebäck aus Brandteig",
        answerB = "Ein französisches Plundergebäck aus Blätterteig mit Butter",
        answerC = "Ein deutsches Hefegebäck",
        answerD = "Ein Schweizer Waffeln-Gebäck",
        correctAnswer = 1,
        explanation = "Das Croissant ist ein französisches Gebäck aus einem mit viel Butter tourtierten Hefeteig, das seinen charakteristischen Blätterteig-Charakter durch mehrfaches Falten erhält.",
        difficulty = 1,
        funFact = "Das Croissant hat seinen Ursprung im österreichischen Kipferl und kam über Wiener Bäcker nach Paris."
    ),

    Question(
        categoryId = 8,
        questionText = "Welcher Käse schmilzt am besten und wird klassisch für Käsespätzle verwendet?",
        answerA = "Parmesan",
        answerB = "Bergkäse / Allgäuer Emmentaler",
        answerC = "Feta",
        answerD = "Ricotta",
        correctAnswer = 1,
        explanation = "Für Käsespätzle wird traditionell Allgäuer Bergkäse oder Emmentaler verwendet, da diese gut schmelzen und ein kräftiges Aroma haben.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Aus welcher Pflanze wird Zimt gewonnen?",
        answerA = "Aus der Rinde eines Zimtbaums",
        answerB = "Aus den Blüten einer Orchidee",
        answerC = "Aus den Samen einer Grassorte",
        answerD = "Aus den Früchten eines Strauches",
        correctAnswer = 0,
        explanation = "Zimt wird aus der inneren Rinde von Zimtbäumen (Gattung Cinnamomum) gewonnen. Die Rinde wird abgeschält, getrocknet und rollt sich dabei zu Zimtstangen auf.",
        difficulty = 1,
        funFact = "Ceylon-Zimt aus Sri Lanka gilt als der hochwertigere 'echte' Zimt im Vergleich zum günstigeren Cassia-Zimt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Risotto?",
        answerA = "Ein italienisches Reisgericht, das durch schrittweises Zugeben von Brühe cremig wird",
        answerB = "Eine spanische Paella-Variante",
        answerC = "Ein griechisches Nudelgericht",
        answerD = "Ein französisches Kartoffelgericht",
        correctAnswer = 0,
        explanation = "Risotto ist ein norditalienisches Gericht, bei dem Rundkornreis (z. B. Arborio) durch schrittweises Einrühren von heißer Brühe cremig gegart wird.",
        difficulty = 1,
        funFact = "Das Stärke-Protein des Rundkornreises gibt Risotto seine charakteristische cremige Konsistenz."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Unterschied zwischen Sahne und Crème fraîche?",
        answerA = "Crème fraîche ist fermentiert und säuerlicher als Sahne",
        answerB = "Sahne ist fermentiert, Crème fraîche nicht",
        answerC = "Beide sind identisch",
        answerD = "Crème fraîche enthält mehr Wasser",
        correctAnswer = 0,
        explanation = "Crème fraîche ist eine fermentierte Sahne mit mindestens 30 % Fett und einem leicht säuerlichen Geschmack. Sahne ist nicht fermentiert.",
        difficulty = 1,
        funFact = null
    ),

    // NEW MEDIUM (20)
    Question(
        categoryId = 8,
        questionText = "Welche Temperatur sollte Rindfleisch mindestens erreichen, um 'medium' gebraten zu sein?",
        answerA = "45 °C",
        answerB = "55–60 °C",
        answerC = "70 °C",
        answerD = "80 °C",
        correctAnswer = 1,
        explanation = "Ein 'medium' gebratenes Steak hat eine Kerntemperatur von etwa 55–60 °C. Das Fleisch ist innen noch rosa und saftig.",
        difficulty = 2,
        funFact = "Bei 'well done' liegt die Kerntemperatur über 70 °C – das Fleisch verliert dann deutlich mehr Saft."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Unterschied zwischen Béchamel- und Velouté-Sauce?",
        answerA = "Beide sind identisch – nur der Name unterscheidet sich regional",
        answerB = "Béchamel wird mit Milch, Velouté mit hellem Fond (Kalb/Geflügel) hergestellt",
        answerC = "Béchamel enthält Tomaten, Velouté nicht",
        answerD = "Velouté wird mit Rotwein zubereitet",
        correctAnswer = 1,
        explanation = "Beide sind klassische Grundsaucen (Mère-Saucen) auf Mehlschwitze-Basis: Béchamel verwendet Milch als Flüssigkeit, Velouté hellen Geflügel- oder Kalbsfond.",
        difficulty = 2,
        funFact = "Auguste Escoffier definierte fünf klassische Grundsaucen der französischen Küche, die sogenannten 'Mère-Saucen'."
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Land gilt als Ursprungsland des Hummus?",
        answerA = "Ägypten",
        answerB = "Griechenland",
        answerC = "Levante (Naher Osten – Libanon, Israel, Syrien)",
        answerD = "Türkei",
        correctAnswer = 2,
        explanation = "Hummus (arabisch für 'Kichererbsen') stammt aus der Levante-Region und wird aus Kichererbsen, Tahini, Zitronensaft und Knoblauch hergestellt.",
        difficulty = 2,
        funFact = "Libanon und Israel streiten seit Jahren diplomatisch über die 'Erfindung' des Hummus."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Tahini?",
        answerA = "Eine türkische Joghurtsauce",
        answerB = "Eine Paste aus gemahlenen Sesamsamen",
        answerC = "Ein arabisches Fladenbrot",
        answerD = "Eine nordafrikanische Gewürzmischung",
        correctAnswer = 1,
        explanation = "Tahini ist eine Paste aus geschälten, gerösteten und gemahlenen Sesamsamen. Sie ist Bestandteil von Hummus, Baba Ganoush und vielen anderen Gerichten.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was sind Capers (Kapern)?",
        answerA = "Eingelegte unreife Blütenknospen des Kapernstrauchs",
        answerB = "Kleine grüne Oliven",
        answerC = "Getrocknete Tomaten",
        answerD = "Eingelegte Gurkenfrüchte",
        correctAnswer = 0,
        explanation = "Kapern sind die eingelegten, unreifen Blütenknospen des Kapernstrauchs (Capparis spinosa). Sie haben einen salzigen, leicht bitteren Geschmack.",
        difficulty = 2,
        funFact = "Kapern werden traditionell in Salzlake oder Salz eingelegt und sind ein typisches Mittelmeer-Produkt."
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Lebensmittel hat den höchsten Eiweißgehalt pro 100 g?",
        answerA = "Hähnchenbrust",
        answerB = "Thunfisch (Dose)",
        answerC = "Parmesan",
        answerD = "Linsen (gekocht)",
        correctAnswer = 2,
        explanation = "Parmesan (Parmigiano Reggiano) hat mit etwa 36 g Eiweiß pro 100 g einen außergewöhnlich hohen Proteingehalt, der durch den langen Reifungsprozess entsteht.",
        difficulty = 2,
        funFact = "Parmesan reift mindestens 12 Monate – DOP-Produkte reifen 24 bis 36 Monate oder länger."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist ein Consommé?",
        answerA = "Ein dick eingekochtes Fleischragout",
        answerB = "Eine geklärte, klare Fleischbrühe",
        answerC = "Eine Cremesuppe mit Sahne",
        answerD = "Ein Kaltaufschnitt",
        correctAnswer = 1,
        explanation = "Consommé ist eine durch Klärfleisch (Hackfleisch, Eiweiß, Gemüse) geklärte, glasklare Kraftbrühe – ein Grundpfeiler der klassischen französischen Küche.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Reissorte wird für Sushi verwendet?",
        answerA = "Basmati",
        answerB = "Wildreis",
        answerC = "Jasminreis",
        answerD = "Japanischer Rundkornreis (Shari)",
        correctAnswer = 3,
        explanation = "Für Sushi wird japanischer Rundkornreis (Koshihikari oder ähnliche Sorten) verwendet, der nach dem Kochen mit einer Mischung aus Reisessig, Zucker und Salz gewürzt wird.",
        difficulty = 2,
        funFact = "Der gewürzte Sushi-Reis heißt 'Shari' oder 'Sumeshi' (Essig-Reis)."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Miso?",
        answerA = "Ein japanisches Reiswein-Getränk",
        answerB = "Eine fermentierte Paste aus Sojabohnen",
        answerC = "Ein chinesisches Nudelgericht",
        answerD = "Eine koreanische Chilipaste",
        correctAnswer = 1,
        explanation = "Miso ist eine japanische Würzpaste aus fermentierten Sojabohnen, Salz und Koji-Pilz. Es gibt helles (shiro), rotes (aka) und gemischtes (awase) Miso.",
        difficulty = 2,
        funFact = "Misosuppe wird in Japan täglich von Millionen Menschen zum Frühstück gegessen."
    ),

    Question(
        categoryId = 8,
        questionText = "Was macht den Unterschied zwischen Joghurt und Kefir?",
        answerA = "Kefir enthält sowohl Milchsäurebakterien als auch Hefen und ist leicht alkoholisch",
        answerB = "Joghurt ist flüssiger als Kefir",
        answerC = "Kefir wird aus Ziegenmilch hergestellt",
        answerD = "Beide sind identisch – nur der Name unterscheidet sich",
        correctAnswer = 0,
        explanation = "Kefir entsteht durch eine gemischte Fermentation mit Milchsäurebakterien UND Hefen, was zu einer leicht prickelnden, minimal alkoholischen Konsistenz führt. Joghurt wird nur durch Milchsäurebakterien fermentiert.",
        difficulty = 2,
        funFact = "Kefirkörner sind Zooglea – eine Symbiose aus Bakterien und Hefen in einer Polysaccharid-Matrix."
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Gewürz wird aus den Staubfäden der Krokusblüte gewonnen?",
        answerA = "Kardamom",
        answerB = "Muskatnuss",
        answerC = "Safran",
        answerD = "Piment",
        correctAnswer = 2,
        explanation = "Safran wird aus den Narben der Safran-Krokusblüte (Crocus sativus) gewonnen. Pro Blüte gibt es nur drei Narben, die von Hand geerntet werden müssen.",
        difficulty = 2,
        funFact = "Für 1 kg Safran werden etwa 150.000 bis 200.000 Blüten und bis zu 400 Arbeitsstunden benötigt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Tapioka?",
        answerA = "Ein Verdickungsmittel aus Maniokwurzel-Stärke",
        answerB = "Ein japanisches Fischprodukt",
        answerC = "Eine afrikanische Hülsenfrucht",
        answerD = "Ein Zuckerersatzstoff aus Mais",
        correctAnswer = 0,
        explanation = "Tapioka ist eine Stärke, die aus der Maniokwurzel (Cassava) gewonnen wird. Es wird als Verdickungsmittel und für Bubble-Tea-Perlen verwendet.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Buttermilch'?",
        answerA = "Milch mit extra Butterfett",
        answerB = "Das flüssige Nebenprodukt der Butterherstellung",
        answerC = "Aufgeschlagene Sahne",
        answerD = "Mit Butter angereicherte Vollmilch",
        correctAnswer = 1,
        explanation = "Buttermilch ist die Flüssigkeit, die beim Verbuttern von Rahm übrig bleibt. Sie hat einen niedrigen Fettgehalt und einen leicht säuerlichen Geschmack.",
        difficulty = 2,
        funFact = "Handelsübliche Buttermilch ist oft fermentierterte Magermilch – kein echtes Nebenprodukt der Butterherstellung."
    ),

    Question(
        categoryId = 8,
        questionText = "Welcher Pilz wird in der europäischen Küche als teuerster einheimischer Pilz gehandelt?",
        answerA = "Champignon",
        answerB = "Shiitake",
        answerC = "Pfifferling",
        answerD = "Trüffel",
        correctAnswer = 3,
        explanation = "Trüffel, besonders der weiße Alba-Trüffel (Tuber magnatum) und der schwarze Périgord-Trüffel (Tuber melanosporum), gehören zu den teuersten Lebensmitteln weltweit.",
        difficulty = 2,
        funFact = "Weiße Trüffel aus Alba können bis zu 5.000 Euro pro Kilogramm kosten."
    ),

    Question(
        categoryId = 8,
        questionText = "Was bedeutet 'Blanchieren' in der Kochkunst?",
        answerA = "Lebensmittel in Öl frittieren",
        answerB = "Lebensmittel kurz in kochendes Wasser tauchen und dann sofort in Eiswasser abschrecken",
        answerC = "Lebensmittel langsam im Ofen garen",
        answerD = "Lebensmittel in Sahne einkochen",
        correctAnswer = 1,
        explanation = "Blanchieren ist ein kurzes Überbrühen von Lebensmitteln in kochendem Salzwasser mit anschließendem schnellen Abschrecken in Eiswasser, um Farbe, Biss und Nährstoffe zu erhalten.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Rucola?",
        answerA = "Eine Kräutersorte der Minzfamilie",
        answerB = "Ein Blattsalat mit pfeffrig-nussigem Geschmack aus der Raketenfamilie",
        answerC = "Eine Kresseart",
        answerD = "Ein in Deutschland heimischer Feldsalat",
        correctAnswer = 1,
        explanation = "Rucola (auch Rauke oder Rocket) ist ein Kreuzblütler mit würzig-pfeffrigem Geschmack. Er stammt aus dem Mittelmeerraum und ist eine typische Zutat der italienischen Küche.",
        difficulty = 2,
        funFact = "Rucola war in der Antike bekannt und wurde auch als Aphrodisiakum verwendet."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist das Besondere an Wagyu-Rindfleisch?",
        answerA = "Es kommt aus Wagyu, einer Region in Südkorea",
        answerB = "Es hat eine außergewöhnlich starke intramuskuläre Fettmarmorierung",
        answerC = "Es stammt von einer wild lebenden Rinderrasse",
        answerD = "Es hat einen niedrigeren Fettgehalt als normales Rindfleisch",
        correctAnswer = 1,
        explanation = "Wagyu-Rinder (japanisch: 'japanisches Rind') produzieren Fleisch mit extrem feiner intramuskulärer Fettmarmorierung, die für einen außergewöhnlich buttrigen Geschmack und zarte Textur sorgt.",
        difficulty = 2,
        funFact = "Echtes Kobe-Beef ist eine spezielle Wagyu-Untersorte und darf nur in der Präfektur Hyōgo produziert werden."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Unterschied zwischen Mehl Type 405 und Type 1050?",
        answerA = "Type 405 enthält mehr Mineralstoffe und ist dunkler",
        answerB = "Type 1050 enthält mehr Mineralstoffe aus Randschichten des Korns und ist dunkler",
        answerC = "Beide sind identisch – die Zahl bezeichnet nur den Hersteller",
        answerD = "Type 405 eignet sich besser für Brot, Type 1050 für Kuchen",
        correctAnswer = 1,
        explanation = "Die Type-Zahl gibt den Mineralstoffgehalt in mg pro 100 g Trockenmasse an. Type 1050 enthält mehr Randschichten des Korns, ist dunkler und nährstoffreicher als das helle Type 405.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist ein Tapenade?",
        answerA = "Ein provenzalischer Brotaufstrich aus Oliven, Kapern und Sardellen",
        answerB = "Ein spanisches Olivenöl",
        answerC = "Eine griechische Vorspeise aus Joghurt",
        answerD = "Ein französischer Käse",
        correctAnswer = 0,
        explanation = "Tapenade ist eine provenzalische Paste aus gehackten Oliven, Kapern, Sardellen und Olivenöl. Das Wort leitet sich vom provenzalischen 'tapéno' (Kapern) ab.",
        difficulty = 2,
        funFact = "Tapenade stammt aus der Provence und wurde erstmals 1880 im Restaurant La Maison Dorée in Marseille erwähnt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist das traditionelle mexikanische Gericht 'Mole'?",
        answerA = "Eine scharfe Tomaten-Salsa",
        answerB = "Eine komplexe Sauce aus Chili, Schokolade, Gewürzen und oft Hühnchen",
        answerC = "Ein gefüllter Maistortilla",
        answerD = "Ein Maisbrei",
        correctAnswer = 1,
        explanation = "Mole ist eine mexikanische Sauce mit bis zu 30 Zutaten, darunter verschiedene Chilis, Schokolade, Nüsse, Gewürze und Tomatillos. Mole negro aus Oaxaca gilt als eines der komplexesten Gerichte der Welt.",
        difficulty = 2,
        funFact = "Ein traditionelles Mole-Rezept kann 2–3 Tage Zubereitungszeit erfordern."
    ),

    // NEW HARD (15)
    Question(
        categoryId = 8,
        questionText = "Was ist 'Osmose' in Bezug auf das Salzen von Fleisch und Gemüse?",
        answerA = "Salz zieht durch die Zellmembran Wasser aus dem Lebensmittel heraus",
        answerB = "Salz verteilt sich gleichmäßig durch aktiven Transport",
        answerC = "Osmose beschreibt das Einziehen von Salz in die Zelle",
        answerD = "Salz zerstört die Zellmembran durch chemische Reaktion",
        correctAnswer = 0,
        explanation = "Wenn Salz auf Lebensmittel gegeben wird, entsteht außen eine höhere Salzkonzentration. Durch Osmose wandert Wasser aus dem Zellinneren (niedrigerer Salzgehalt) durch die semipermeable Membran nach außen.",
        difficulty = 3,
        funFact = "Dieses Prinzip nutzt man beim Entwässern von Gurken oder Zucchini mit Salz vor dem Braten."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Aromastoffe entstehen beim Rösten von Kaffeebohnen hauptsächlich?",
        answerA = "Nur Koffein und Tannine",
        answerB = "Furanone, Pyrazine und Thiole durch Maillard-Reaktion und Karamellisierung",
        answerC = "Ausschließlich Chlorogensäuren",
        answerD = "Ätherische Öle aus der Kaffeekirsche",
        correctAnswer = 1,
        explanation = "Beim Rösten entstehen durch Maillard-Reaktion und Karamellisierung über 800 flüchtige Aromaverbindungen, darunter Furanone (karamellartig), Pyrazine (nussig-röstartig) und Thiole (kaffeetypisch).",
        difficulty = 3,
        funFact = "Grüner Kaffee ist geschmacksneutral – erst das Rösten erzeugt das charakteristische Kaffeearoma."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Deglacieren' beim Kochen?",
        answerA = "Einen Fond durch Zugabe von Flüssigkeit (Wein, Brühe) vom Bratensatz lösen",
        answerB = "Eine Sauce durch Reduktion eindicken",
        answerC = "Tiefgefrorene Lebensmittel schonend auftauen",
        answerD = "Zucker in einer Pfanne karamellisieren",
        correctAnswer = 0,
        explanation = "Beim Deglacieren gibt man nach dem Anbraten Flüssigkeit (Wein, Cognac, Brühe) in die heiße Pfanne, um den karamellisierten Bratensatz (der voller Aromen steckt) zu lösen und in die Sauce zu integrieren.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Winterspeck' bei einer Gans und warum ist er kulinarisch bedeutsam?",
        answerA = "Das ist intramuskuläres Fett, das die Gans für den Winter einlagert und für einen besonderen Geschmack sorgt",
        answerB = "Eine spezielle Fettschicht unter der Gänsehaut, die beim Braten wichtige Aromen freisetzt und die Haut knusprig macht",
        answerC = "Ein Begriff für das Fett älterer Gänse im Dezember",
        answerD = "Das Bauchfett der Gans, das entfernt werden muss",
        correctAnswer = 1,
        explanation = "Die dicke Unterhautfettschicht der Martinsgans dient dem Tier als Energiereserve. Beim langen, langsamen Garen schmilzt dieses Fett durch die Haut und macht sie außen knusprig, während das Fleisch saftig bleibt.",
        difficulty = 3,
        funFact = "Eine Martinsgans sollte traditionell nach dem 11. November (Martinstag) verzehrt werden."
    ),

    Question(
        categoryId = 8,
        questionText = "Was beschreibt die 'Hydrolyse von Kollagen' beim langen Schmoren von Fleisch?",
        answerA = "Kollagen (Bindegewebe) wird durch Säure aufgespalten",
        answerB = "Kollagen wandelt sich durch lang anhaltende Hitze und Wasser in Gelatine um",
        answerC = "Proteinketten werden durch Enzyme zersetzt",
        answerD = "Fett wird in das Kollagengerüst eingelagert",
        correctAnswer = 1,
        explanation = "Bei Temperaturen über 70 °C und langer Garzeit hydrolysiert Kollagen (Bindegewebsprotein) und wandelt sich in lösliche Gelatine um. Das macht zähes Fleisch (Schulter, Haxe) nach stundenlangem Schmoren zart und saftig.",
        difficulty = 3,
        funFact = "Dieser Vorgang ist der Grund, warum Schmorgerichte mit viel Bindegewebe (Ossobuco, Boeuf Bourguignon) so reichhaltige Saucen entwickeln."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Lactobacillus bulgaricus' und welche Rolle spielt er?",
        answerA = "Ein Schimmelpilz für Käsereifung",
        answerB = "Ein Milchsäurebakterium, das essenziell für die Joghurtherstellung ist",
        answerC = "Ein Hefestamm für Brotbacken",
        answerD = "Ein Konservierungsmittel in Wurstwaren",
        correctAnswer = 1,
        explanation = "Lactobacillus delbrueckii subsp. bulgaricus ist eines der beiden Bakterien (neben Streptococcus thermophilus), die für die Joghurtfermentation notwendig sind. Es wandelt Laktose in Milchsäure um und sorgt für den sauren Geschmack.",
        difficulty = 3,
        funFact = "Echter bulgarischer Joghurt nutzt Stämme aus Bulgarien, die sich von anderen Joghurt-Stämmen unterscheiden."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Schockfrosten' und warum ist es besser als langsames Einfrieren?",
        answerA = "Durch langsames Einfrieren entstehen größere Eiskristalle, die Zellstrukturen zerstören; Schockfrosten erzeugt kleinere Kristalle",
        answerB = "Schockfrosten tötet mehr Bakterien ab",
        answerC = "Schockfrosten spart mehr Energie",
        answerD = "Beide Methoden sind gleichwertig für die Lebensmittelqualität",
        correctAnswer = 0,
        explanation = "Beim langsamen Einfrieren bilden sich große Eiskristalle, die Zellwände durchstechen und beim Auftauen zu Qualitätsverlust führen. Schockfrosten (-40 °C oder kälter) erzeugt sehr kleine Kristalle, die Zellstrukturen intakt lassen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Guanciale' und worin unterscheidet es sich von Pancetta?",
        answerA = "Guanciale wird aus der Schweinewange hergestellt, Pancetta aus dem Bauch",
        answerB = "Beide sind identisch – regionale Namen für dasselbe Produkt",
        answerC = "Guanciale ist geräuchert, Pancetta nicht",
        answerD = "Pancetta stammt aus dem Kopf, Guanciale aus dem Bauch",
        correctAnswer = 0,
        explanation = "Guanciale (von 'guancia' = Wange) ist gepökeltes, luftgetrocknetes Schweinebackenfleisch mit höherem Fettanteil. Pancetta wird aus dem Schweinebauch hergestellt. Für authentische Carbonara und Amatriciana wird Guanciale verwendet.",
        difficulty = 3,
        funFact = "In Rom gilt die Verwendung von Speck oder Pancetta in Carbonara als Ketzerei – nur Guanciale ist akzeptiert."
    ),

    Question(
        categoryId = 8,
        questionText = "Was beschreibt der Begriff 'Gluten' in Bezug auf Mehl?",
        answerA = "Ein natürlicher Zucker in Getreide",
        answerB = "Ein Kleberprotein aus Gliadin und Glutenin, das Teig elastisch und dehnbar macht",
        answerC = "Ein Ballaststoff in Weizenkleie",
        answerD = "Ein Stärkepolysaccharid",
        correctAnswer = 1,
        explanation = "Gluten entsteht wenn Wasser zu Mehl gegeben wird und sich die Proteine Gliadin und Glutenin verbinden. Das entstehende Glutennetzwerk gibt Teig Elastizität und hält Gärgase beim Backen zurück.",
        difficulty = 3,
        funFact = "Bei Zöliakie löst Gluten eine Autoimmunreaktion aus, die die Dünndarmschleimhaut schädigt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Mirepoix' in der klassischen Küche?",
        answerA = "Eine Würzmischung aus Kräutern",
        answerB = "Ein aromatisches Gemüsebett aus Zwiebeln, Karotten und Sellerie (2:1:1) als Geschmacksbasis",
        answerC = "Eine Schneidetechnik für Brunoise",
        answerD = "Ein französischer Fond aus Gemüse",
        correctAnswer = 1,
        explanation = "Mirepoix ist die klassische aromatische Gemüsebasis der französischen Küche: 2 Teile Zwiebeln, 1 Teil Karotten, 1 Teil Sellerie. Es bildet die Geschmacksgrundlage für Fonds, Saucen und Schmorgerichte.",
        difficulty = 3,
        funFact = "Mirepoix wurde nach dem Duc de Lévis-Mirepoix benannt, einem Höfling Ludwigs XV."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Prosciutto di Parma' und wie unterscheidet er sich von normalem Schinken?",
        answerA = "Er ist geräuchert und hat einen starken Rauchgeschmack",
        answerB = "Er ist luftgetrockneter Rohschinken aus der Region Parma, der nur mit Salz – ohne Nitrite – gereift wird",
        answerC = "Er wird aus Wildschwein hergestellt",
        answerD = "Er ist gekochter Schinken mit einer speziellen Würzung",
        correctAnswer = 1,
        explanation = "Prosciutto di Parma ist ein DOP-geschützter, luftgetrockneter Rohschinken aus Parma. Er wird ausschließlich mit Meersalz ohne weitere Zusatzstoffe oder Nitrite mindestens 12 Monate gereift.",
        difficulty = 3,
        funFact = "Die Schweine für Prosciutto di Parma werden mit Molke aus der Parmesankäseherstellung gefüttert."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Homogenisierung' bei Milch?",
        answerA = "Das Pasteurisieren bei hohen Temperaturen",
        answerB = "Das mechanische Zerstören der Fettkügelchen, damit Sahne sich nicht mehr absetzt",
        answerC = "Das Anreichern von Milch mit Vitaminen",
        answerD = "Das Entfernen von Laktose aus der Milch",
        correctAnswer = 1,
        explanation = "Bei der Homogenisierung wird Milch unter hohem Druck durch feine Düsen gepresst, wodurch die Fettkügelchen so klein werden, dass sie gleichmäßig verteilt bleiben und sich keine Rahmschicht mehr bildet.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Dashi' in der japanischen Küche?",
        answerA = "Ein Reisweinessig",
        answerB = "Ein heller Fond aus Kombu-Algen und Bonitoflocken (Katsuobushi)",
        answerC = "Eine Sojasaucenvariation",
        answerD = "Ein fermentiertes Gemüsegericht",
        correctAnswer = 1,
        explanation = "Dashi ist der grundlegende Kochfond der japanischen Küche, hergestellt aus Kombu-Algen und getrockneten Bonitoflocken (Katsuobushi). Er bildet die Basis für Misosuppe, Ramen und viele Saucen.",
        difficulty = 3,
        funFact = "Dashi enthält natürlich Glutaminsäure und Inosinsäure – beide lösen den Umami-Geschmack aus."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Sauerteig' und was bewirken die Mikroorganismen darin?",
        answerA = "Sauerteig ist nur ein anderer Name für Hefe",
        answerB = "Sauerteig enthält wild vorkommende Hefen und Milchsäurebakterien, die Teig lockern und sauer machen",
        answerC = "Sauerteig ist ein chemisches Backtriebmittel wie Backpulver",
        answerD = "Sauerteig enthält ausschließlich Essigsäurebakterien",
        correctAnswer = 1,
        explanation = "Sauerteig ist eine Symbiose aus wilden Hefen (Triebkraft) und Milchsäurebakterien (Aroma, Säure, Konservierung). Die Hefen produzieren CO₂ für die Lockerung, die Bakterien erzeugen Milch- und Essigsäure.",
        difficulty = 3,
        funFact = "Ein gut gepflegter Sauerteigstarter kann Jahrzehnte oder sogar Jahrhunderte alt werden."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Agar-Agar' und woher stammt es?",
        answerA = "Ein tierisches Geliermittel aus Knochen wie Gelatine",
        answerB = "Ein pflanzliches Geliermittel aus Rotalgen, das auch für vegane Desserts geeignet ist",
        answerC = "Ein synthetisches Verdickungsmittel aus der Lebensmittelindustrie",
        answerD = "Ein Stärkeprodukt aus Tapioka",
        correctAnswer = 1,
        explanation = "Agar-Agar (E 406) ist ein Polysaccharid aus Rotalgen (v. a. Gelidium, Gracilaria). Es geliert stärker als Gelatine und ist bei Raumtemperatur stabil – ideal als vegane Alternative zu tierischer Gelatine.",
        difficulty = 3,
        funFact = "Agar-Agar wird auch in der Mikrobiologie als Nährboden für Bakterienkulturen verwendet."
    ),

    // ADDITIONAL EASY (25)
    Question(
        categoryId = 8,
        questionText = "Was ist Schokolade hauptsächlich?",
        answerA = "Ein Getreideprodukt",
        answerB = "Ein Produkt aus Kakaofrüchten",
        answerC = "Ein Milchprodukt",
        answerD = "Ein Nussprodukt",
        correctAnswer = 1,
        explanation = "Schokolade wird aus den Samen der Kakaofrucht (Theobroma cacao) hergestellt. Die Bohnen werden fermentiert, getrocknet, geröstet und verarbeitet.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welches Tier legt die Eier, die wir zum Frühstück essen?",
        answerA = "Ente",
        answerB = "Gans",
        answerC = "Huhn",
        answerD = "Wachtel",
        correctAnswer = 2,
        explanation = "Die meisten Frühstückseier stammen von Hühnern. Hühnereier sind weltweit das am häufigsten verzehrte Vogelei.",
        difficulty = 1,
        funFact = "Ein Huhn legt durchschnittlich 250–300 Eier pro Jahr."
    ),

    Question(
        categoryId = 8,
        questionText = "Wie nennt man das Getränk aus heißem Wasser und getrockneten Teeblättern?",
        answerA = "Kaffee",
        answerB = "Kakao",
        answerC = "Tee",
        answerD = "Brühe",
        correctAnswer = 2,
        explanation = "Tee ist ein Aufgussgetränk aus den Blättern der Teepflanze (Camellia sinensis) oder anderen getrockneten Pflanzenteilen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Marmelade?",
        answerA = "Eingekochtes Gemüse",
        answerB = "Eingekochtes Obst mit Zucker",
        answerC = "Fruchtjoghurt",
        answerD = "Frucht-Honig-Mischung",
        correctAnswer = 1,
        explanation = "Marmelade (oder Konfitüre) ist ein Aufstrich aus eingekochtem Obst mit Zucker, der durch Pektin geliert.",
        difficulty = 1,
        funFact = "Streng genommen darf im EU-Raum nur Zitrusfrüchteaufstrich 'Marmelade' heißen – der Rest heißt 'Konfitüre'."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Farbe hat eine reife Banane?",
        answerA = "Grün",
        answerB = "Rot",
        answerC = "Gelb",
        answerD = "Orange",
        correctAnswer = 2,
        explanation = "Reife Bananen sind gelb. Das Chlorophyll in der Schale wird beim Reifen abgebaut und gibt die gelben Carotinoide frei.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Aus was wird Brot hauptsächlich hergestellt?",
        answerA = "Kartoffeln",
        answerB = "Mehl, Wasser und Hefe",
        answerC = "Mais und Milch",
        answerD = "Hafer und Honig",
        correctAnswer = 1,
        explanation = "Brot besteht im Kern aus Mehl, Wasser, Salz und einem Triebmittel wie Hefe oder Sauerteig.",
        difficulty = 1,
        funFact = "Die ältesten bekannten Brotfunde sind rund 14.000 Jahre alt und wurden in Jordanien entdeckt."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Farbe hat rohes Rinderhackfleisch?",
        answerA = "Weiß",
        answerB = "Grau",
        answerC = "Rot",
        answerD = "Braun",
        correctAnswer = 2,
        explanation = "Frisches Rinderhackfleisch ist leuchtend rot, weil Myoglobin mit Sauerstoff reagiert (Oxymyoglobin). Braun wird es wenn kein Sauerstoff mehr vorhanden ist oder beim Erhitzen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Olivenöl?",
        answerA = "Ein Pflanzenöl aus gepressten Oliven",
        answerB = "Ein tierisches Fett",
        answerC = "Ein Öl aus Olivenkernen",
        answerD = "Ein Butterersatz aus Margarine",
        correctAnswer = 0,
        explanation = "Olivenöl wird durch Pressen von reifen oder halbreifen Oliven gewonnen. Extra vergine (kaltgepresst) ist die höchste Qualitätsstufe.",
        difficulty = 1,
        funFact = "Olivenbäume können mehrere Hundert Jahre alt werden – einige in Griechenland sind über 2.000 Jahre alt."
    ),

    Question(
        categoryId = 8,
        questionText = "Welche Flüssigkeit wird klassisch zum Zubereiten von Omelette verwendet?",
        answerA = "Milch und Eier",
        answerB = "Wasser und Mehl",
        answerC = "Sahne und Hefe",
        answerD = "Joghurt und Stärke",
        correctAnswer = 0,
        explanation = "Ein klassisches Omelette besteht aus verquirlten Eiern, oft mit etwas Milch oder Sahne, die in Butter in der Pfanne gegart werden.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Wie nennt man gebratene Kartoffelscheiben?",
        answerA = "Pommes frites",
        answerB = "Chips",
        answerC = "Bratkartoffeln",
        answerD = "Rösti",
        correctAnswer = 2,
        explanation = "Bratkartoffeln sind in Scheiben geschnittene Kartoffeln, die in der Pfanne in Fett gebraten werden. Sie sind ein klassisches deutsches Beilagengericht.",
        difficulty = 1,
        funFact = "Bratkartoffeln schmecken besonders gut aus vorgekochten Kartoffeln vom Vortag."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Honig?",
        answerA = "Ein von Bienen aus Blütennektar hergestelltes Lebensmittel",
        answerB = "Ein Pflanzensaft aus Zuckerrüben",
        answerC = "Ein Sirup aus Ahornbäumen",
        answerD = "Eine kristallisierte Zuckerform",
        correctAnswer = 0,
        explanation = "Honig wird von Honigbienen aus dem Nektar von Blüten produziert. Die Bienen sammeln den Nektar, verarbeiten ihn enzymatisch und lagern ihn in Waben.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Popcorn?",
        answerA = "Gerästete Reiskörner",
        answerB = "Aufgepoppte Maiskörner durch Hitze",
        answerC = "Getrocknete Weizenkörner",
        answerD = "Frittierte Kartoffelstückchen",
        correctAnswer = 1,
        explanation = "Popcorn entsteht wenn spezielle Maissorten (Zea mays var. everta) erhitzt werden. Die eingeschlossene Feuchtigkeit im Korn verdampft und lässt das Korn aufplatzen.",
        difficulty = 1,
        funFact = "Das älteste bekannte Popcorn ist etwa 5.000 Jahre alt und wurde in Peru gefunden."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist ein Smoothie?",
        answerA = "Ein klarer Fruchtsaft ohne Fruchtfleisch",
        answerB = "Ein püriertes Getränk aus frischen Früchten oder Gemüse",
        answerC = "Ein Milchshake mit Eis",
        answerD = "Ein fermentiertes Fruchtgetränk",
        correctAnswer = 1,
        explanation = "Ein Smoothie ist ein dickflüssiges Getränk, das durch Pürieren von frischen Früchten und/oder Gemüse (samt Fruchtfleisch) hergestellt wird.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Aus welchem Land stammt die Chorizo-Wurst ursprünglich?",
        answerA = "Italien",
        answerB = "Spanien / Portugal",
        answerC = "Mexiko",
        answerD = "Argentinien",
        correctAnswer = 1,
        explanation = "Chorizo ist eine gewürzte Wurst aus der Iberischen Halbinsel (Spanien und Portugal), die durch Paprika (Pimentón) ihre rote Farbe und ihr charakteristisches Aroma bekommt.",
        difficulty = 1,
        funFact = "Es gibt süße, halbscharfe und scharfe Chorizo – je nach Paprikasorte."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist ein Burger?",
        answerA = "Ein deutsches Schnitzel-Sandwich",
        answerB = "Ein belegtes Brötchen mit Fleischpattie",
        answerC = "Ein mexikanischer Taco",
        answerD = "Ein englisches Frühstücksgericht",
        correctAnswer = 1,
        explanation = "Ein Burger besteht typischerweise aus einem Brötchen (Bun), einem gegrillten oder gebratenen Fleischpattie und verschiedenen Beilagen wie Salat, Tomate und Sauce.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Mineralwasser?",
        answerA = "Destilliertes Wasser ohne Mineralstoffe",
        answerB = "Leitungswasser mit künstlich zugesetzter Kohlensäure",
        answerC = "Natürliches Quellwasser mit gelösten Mineralstoffen",
        answerD = "Gefiltertes Meerwasser",
        correctAnswer = 2,
        explanation = "Mineralwasser ist natürliches Grundwasser, das Mineralstoffe wie Calcium, Magnesium und Natrium enthält und aus geschützten Quellen gewonnen wird.",
        difficulty = 1,
        funFact = "Deutschland hat über 500 anerkannte Mineralwasserquellen."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Lasagne?",
        answerA = "Ein griechisches Nudelgericht mit Joghurt",
        answerB = "Ein italienisches Nudelgericht mit Schichten aus Teigplatten, Hackfleischsauce und Béchamel",
        answerC = "Ein spanisches Reiskasserole",
        answerD = "Ein deutsches Auflaufgericht ohne Nudeln",
        correctAnswer = 1,
        explanation = "Lasagne ist ein klassisches italienisches Gericht aus geschichteten Teigplatten, Ragù (Hackfleischsauce), Béchamelsauce und Käse, das im Ofen überbacken wird.",
        difficulty = 1,
        funFact = "Lasagne ist eines der ältesten Pasta-Gerichte und wurde bereits im 14. Jahrhundert in Italien erwähnt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist ein Wrap?",
        answerA = "Eine gefüllte Toastscheibe",
        answerB = "Ein gefülltes, aufgerolltes Fladenbrot oder Tortilla",
        answerC = "Ein gegrilltes Sandwich",
        answerD = "Ein Bagel mit Füllung",
        correctAnswer = 1,
        explanation = "Ein Wrap ist eine in ein dünnes Fladenbrot oder eine Tortilla eingerollte Füllung aus Fleisch, Gemüse, Salat und Saucen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Wasabi?",
        answerA = "Eine japanische Soßenbasis aus fermentiertem Fisch",
        answerB = "Eine sehr scharfe grüne Paste aus der Wasabipflanze",
        answerC = "Ein japanisches Reisgericht",
        answerD = "Eine süße Soße zu Sushi",
        correctAnswer = 1,
        explanation = "Wasabi ist eine scharfe, grüne Paste aus dem geriebenen Rhizom der Wasabipflanze (Eutrema japonicum), die zu Sushi und anderen japanischen Gerichten gereicht wird.",
        difficulty = 1,
        funFact = "Echter Wasabi ist sehr teuer – die meisten Sushi-Restaurants außerhalb Japans servieren gefärbten Meerrettich."
    ),

    Question(
        categoryId = 8,
        questionText = "Was bedeutet 'vegetarisch' bei einer Ernährungsweise?",
        answerA = "Kein Fleisch und keine tierischen Produkte",
        answerB = "Kein Fleisch, aber Milchprodukte und Eier sind erlaubt",
        answerC = "Nur rohes Obst und Gemüse",
        answerD = "Kein rotes Fleisch, aber Fisch erlaubt",
        correctAnswer = 1,
        explanation = "Vegetarier essen kein Fleisch und keinen Fisch, verzehren aber in der Regel Milchprodukte (Lakto-) und/oder Eier (Ovo-). Veganer hingegen verzichten auf alle tierischen Produkte.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Welcher Gewürz ist für die typische Schärfe von Ingwer verantwortlich?",
        answerA = "Capsaicin",
        answerB = "Piperin",
        answerC = "Gingerol",
        answerD = "Allicin",
        correctAnswer = 2,
        explanation = "Gingerol (und sein Abbauprodukt Shogaol in getrocknetem Ingwer) ist die scharf-würzige Verbindung im Ingwer.",
        difficulty = 1,
        funFact = "Ingwer wird seit über 3.000 Jahren als Gewürz und Heilmittel verwendet."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Frischkäse?",
        answerA = "Sehr langer gereifter Hartkäse",
        answerB = "Ein nicht oder kaum gereifter Käse mit hohem Wassergehalt",
        answerC = "Käse aus Frischmilch ohne Wärmebehandlung",
        answerD = "Gelatinierter Quark",
        correctAnswer = 1,
        explanation = "Frischkäse ist eine Käsesorte, die nicht oder kaum gereift ist und einen hohen Wassergehalt hat. Dazu gehören Quark, Ricotta, Philadelphia und Mascarpone.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Paella?",
        answerA = "Ein italienisches Risotto-Gericht",
        answerB = "Ein spanisches Reisgericht mit Meeresfrüchten, Fleisch und Gemüse",
        answerC = "Ein portugiesisches Fischgericht",
        answerD = "Ein mexikanisches Tortilla-Gericht",
        correctAnswer = 1,
        explanation = "Paella ist ein valencianisches Reisgericht aus Spanien, das im flachen Paella-Pan zubereitet wird und Zutaten wie Meeresfrüchte, Hähnchen, Gemüse und Safran enthalten kann.",
        difficulty = 1,
        funFact = "Die traditionelle Paella Valenciana enthält weder Meeresfrüchte noch Chorizo – sondern Kaninchen, Huhn und Bohnen."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist ein Brownie?",
        answerA = "Ein amerikanisches schokoladiges Gebäck",
        answerB = "Ein britischer Schokoladenkuchen mit Sahne",
        answerC = "Ein belgisches Waffelgebäck",
        answerD = "Ein französisches Törtchen aus Brandteig",
        correctAnswer = 0,
        explanation = "Brownies sind ein amerikanisches Schokodessert – dichte, feuchte Schokokuchen-Riegel, die zwischen Kuchen und Keks liegen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist Senf (als Lebensmittel)?",
        answerA = "Eine Sauce aus pürierten Tomaten mit Essig",
        answerB = "Eine Würzpaste aus gemahlenen Senfkörnern, Essig und Gewürzen",
        answerC = "Eine Marinade aus Zitronensaft",
        answerD = "Ein eingekochtes Chutney aus Zwiebeln",
        correctAnswer = 1,
        explanation = "Speisesenf wird aus gemahlenen Senfkörnern (weiß/gelb oder schwarz/braun), Essig, Wasser und Gewürzen hergestellt. Dijon-Senf, Bauernsenf und süßer Senf sind bekannte Varianten.",
        difficulty = 1,
        funFact = "Senf ist eines der ältesten Gewürze der Welt – Senfrezepte sind seit 3.000 v. Chr. belegt."
    ),

    // ADDITIONAL MEDIUM (15)
    Question(
        categoryId = 8,
        questionText = "Was versteht man unter 'Terrinen' in der Küche?",
        answerA = "Eine Pastete oder gepresste Fleisch-/Gemüsemasse, die in einer Kastenform gegart wird",
        answerB = "Eine flüssige Fleischbrühe",
        answerC = "Ein Dessert aus Fruchtgelee",
        answerD = "Eine Art Fondue",
        correctAnswer = 0,
        explanation = "Eine Terrine ist eine in einer Kastenform zubereitete Masse aus Fleisch, Fisch, Gemüse oder Früchten. Sie wird heiß oder kalt serviert und ist eng mit der Pastete verwandt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Paneer'?",
        answerA = "Ein fermentierter indischer Joghurt",
        answerB = "Ein frischer, ungereifter Käse aus der indischen Küche",
        answerC = "Eine indische Hülsenfruchtpaste",
        answerD = "Ein indisches Fladenbrot",
        correctAnswer = 1,
        explanation = "Paneer ist ein frischer, druckgepresster Käse aus Kuhmilch oder Büffelmilch, der in der süd- und südasiatischen Küche verbreitet ist. Er wird nicht zum Schmelzen gebracht und eignet sich zum Braten.",
        difficulty = 2,
        funFact = "Paneer ist eine der wichtigsten Proteinquellen in der vegetarischen indischen Küche."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Unterschied zwischen 'Roh-' und 'Brühwurst'?",
        answerA = "Rohwurst wird erhitzt und dann geräuchert; Brühwurst wird nicht erhitzt",
        answerB = "Rohwurst wird roh gereift (fermentiert/getrocknet); Brühwurst wird während der Herstellung gebrüht (erhitzt)",
        answerC = "Brühwurst enthält nur rohes Fleisch ohne Gewürze",
        answerD = "Beide sind identisch – der Name ist nur regional unterschiedlich",
        correctAnswer = 1,
        explanation = "Rohwürste (z. B. Salami, Mettwurst) werden nicht erhitzt, sondern durch Fermentation, Trocknung oder Räuchern haltbar gemacht. Brühwürste (Frankfurter, Mortadella, Leberkäse) werden im Herstellungsprozess erhitzt.",
        difficulty = 2,
        funFact = "Frankfurter Würstchen sind eine der bekanntesten Brühwürste der Welt und haben DOP-Schutz."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Stracciatella' in der Eiscreme-Welt?",
        answerA = "Ein Pistazieneis aus Sizilien",
        answerB = "Ein Vanilleeis mit eingearbeiteten Schokoladesplittern",
        answerC = "Ein Erdbeereis mit Sahnestreifen",
        answerD = "Ein Mandeleis aus Sardinien",
        correctAnswer = 1,
        explanation = "Stracciatella (von 'stracciare' = zerreißen) ist ein Vanilleeis, bei dem flüssige Schokolade eingearbeitet wird und beim Abkühlen zu unregelmäßigen Schokoladesplittern erstarrt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Mise en Place' in der professionellen Küche?",
        answerA = "Eine Kochtechnik für Saucen",
        answerB = "Das Vorbereiten und Bereitstellen aller Zutaten und Werkzeuge vor dem Kochen",
        answerC = "Eine Methode zum Anrichten von Tellern",
        answerD = "Die Küchenordnung nach Dienstende",
        correctAnswer = 1,
        explanation = "'Mise en Place' (französisch: 'alles an seinen Platz') bezeichnet das vollständige Vorbereiten aller Zutaten, Geräte und Werkzeuge, bevor mit dem eigentlichen Kochen begonnen wird.",
        difficulty = 2,
        funFact = "Mise en Place ist ein Grundprinzip jeder professionellen Küche und gilt als Zeichen eines disziplinierten Kochs."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Ceviche'?",
        answerA = "Ein mexikanisches Rindfleischgericht",
        answerB = "Ein lateinamerikanisches Gericht aus rohem Fisch, der in Zitrussaft 'gegart' wird",
        answerC = "Ein peruanisches Maisgebäck",
        answerD = "Ein brasilianisches Kokos-Dessert",
        correctAnswer = 1,
        explanation = "Ceviche ist ein Gericht aus frischem rohem Fisch (oder Meeresfrüchten), der durch die Säure von Limetten- oder Zitronensaft denaturiert (chemisch 'gegart') wird.",
        difficulty = 2,
        funFact = "Peru gilt als Ursprungsland des Ceviche und feiert es als Nationalgericht."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Unterschied zwischen 'Röstzwiebeln' und 'karamellisierten Zwiebeln'?",
        answerA = "Röstzwiebeln werden scharf und schnell gebraten bis sie knusprig sind; karamellisierte Zwiebeln werden bei mittlerer Hitze sehr lange geschmort bis sie süß-weich werden",
        answerB = "Karamellisierte Zwiebeln werden zusätzlich mit Zucker bestreut",
        answerC = "Beide Begriffe bezeichnen dasselbe",
        answerD = "Röstzwiebeln sind immer aus dem Handel, karamellisierte werden selbst zubereitet",
        correctAnswer = 0,
        explanation = "Röstzwiebeln werden bei hoher Hitze schnell knusprig gebraten. Karamellisierte Zwiebeln brauchen 30–60 Minuten bei mittlerer Hitze – dabei werden natürliche Zucker freigesetzt und die Zwiebeln werden süß und dunkelbraun.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Bonito' in der japanischen Küche?",
        answerA = "Eine Algenart für Sushi",
        answerB = "Ein Thunfischverwandter (Skipjack-Thunfisch), dessen Flocken als Katsuobushi verwendet werden",
        answerC = "Eine Sojasoßenvariation",
        answerD = "Ein japanischer Reisessig",
        correctAnswer = 1,
        explanation = "Bonito (Katsuo) ist ein Fisch, der geräuchert, fermentiert und getrocknet zu Katsuobushi (getrocknete Bonitoflocken) verarbeitet wird. Diese sind eine der wichtigsten Umami-Quellen und Grundlage von Dashi.",
        difficulty = 2,
        funFact = "Katsuobushi ist mit etwa 15 % Wassergehalt eines der härtesten Lebensmittel der Welt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Polenta'?",
        answerA = "Ein italienisches Risotto aus Mais",
        answerB = "Ein Brei oder Kuchen aus grobem Maisgrieß",
        answerC = "Ein sizilianisches Weizengebäck",
        answerD = "Ein norditaliensisches Nudelgericht",
        correctAnswer = 1,
        explanation = "Polenta ist ein Gericht aus grobem Maisgrieß (Bramata), der mit Wasser oder Brühe zu einem Brei gekocht wird. Es ist ein Grundnahrungsmittel der norditalienischen Küche.",
        difficulty = 2,
        funFact = "Feste Polenta kann nach dem Abkühlen geschnitten und gegrillt oder gebraten werden."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Pecorino Romano'?",
        answerA = "Ein Kuhmilchkäse aus der Toskana",
        answerB = "Ein harter, gereifter Schafsmilchkäse aus Latium und Sardinien",
        answerC = "Ein Weichkäse aus dem Piemont",
        answerD = "Ein Büffelmilchkäse aus Kampanien",
        correctAnswer = 1,
        explanation = "Pecorino Romano ist ein harter, salziger Schafsmilchkäse (Pecorino = Schaf) mit DOP-Schutz. Er wird in Latium, Sardinien und der Toskana hergestellt und ist unverzichtbar in Pasta-Gerichten wie Cacio e Pepe.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Pancake' und wie unterscheidet er sich vom deutschen Pfannkuchen?",
        answerA = "Pancakes sind dicker und luftiger als deutsche Pfannkuchen, da der Teig mehr Backpulver enthält",
        answerB = "Pancakes sind dünner als deutsche Pfannkuchen",
        answerC = "Pancakes werden mit Hefe hergestellt, Pfannkuchen ohne",
        answerD = "Beide sind identisch",
        correctAnswer = 0,
        explanation = "Amerikanische Pancakes sind deutlich dicker und fluffiger als deutsche Pfannkuchen, weil ihr Teig Backpulver oder Backnatron enthält, das CO₂-Blasen erzeugt. Deutsche Pfannkuchen sind dünner und werden oft aufgerollt.",
        difficulty = 2,
        funFact = "Pancakes mit Ahornsirup sind das klassische amerikanische Frühstück."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Antipasto'?",
        answerA = "Eine italienische Hauptmahlzeit",
        answerB = "Die Vorspeisen-Auswahl in der italienischen Küche",
        answerC = "Ein Pasta-Gericht gegen den Hunger",
        answerD = "Ein Dessert nach der Pasta",
        correctAnswer = 1,
        explanation = "'Antipasto' bedeutet wörtlich 'vor der Pasta' und bezeichnet den Vorspeisen-Gang der italienischen Mahlzeit, bestehend aus Aufschnitt, Käse, Oliven, Gemüse und anderen Häppchen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Gravad Lax'?",
        answerA = "Geräucherter skandinavischer Lachs",
        answerB = "In Salz, Zucker und Dill gepökelter roher Lachs",
        answerC = "Frittierter Lachs nach norwegischer Art",
        answerD = "Eingelegter Hering in Dillsauce",
        correctAnswer = 1,
        explanation = "Gravad Lax (oder Gravlax) ist ein skandinavisches Gericht aus rohem Lachs, der in einer Mischung aus grobem Salz, Zucker und frischem Dill für mehrere Tage gebeizt wird.",
        difficulty = 2,
        funFact = "Der Name 'Gravad Lax' bedeutet wörtlich 'eingegrabener Lachs' – ursprünglich wurde der Fisch unter der Erde fermentiert."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Baba Ganoush'?",
        answerA = "Eine libanesische Joghurt-Kräutersauce",
        answerB = "Ein Aufstrich aus gerösteten Auberginen, Tahini, Zitrone und Knoblauch",
        answerC = "Ein türkischer Linsenaufstrich",
        answerD = "Ein ägyptisches Fladenbrot",
        correctAnswer = 1,
        explanation = "Baba Ganoush ist ein levantinischer Aufstrich aus über offener Flamme gerösteten Auberginen, die mit Tahini, Zitronensaft, Knoblauch und Gewürzen vermischt werden.",
        difficulty = 2,
        funFact = "Der Rauchgeschmack von Baba Ganoush entsteht durch das Rösten der Aubergine direkt auf der Flamme."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Unterschied zwischen weißem und braunem Zucker?",
        answerA = "Brauner Zucker ist gesünder weil er aus anderen Pflanzen stammt",
        answerB = "Brauner Zucker enthält noch Melasse-Reste, weißer Zucker ist vollständig raffiniert",
        answerC = "Weißer Zucker hat mehr Kalorien als brauner",
        answerD = "Brauner Zucker entsteht aus Rüben, weißer aus Zuckerrohr",
        correctAnswer = 1,
        explanation = "Brauner Zucker enthält noch Melasse (den dunklen Nebenprodukt der Zuckerherstellung), die ihm Farbe, Aroma und minimale Spurenmineralien gibt. Weißer Zucker ist vollständig raffiniert.",
        difficulty = 2,
        funFact = null
    ),

    // ADDITIONAL HARD (23)
    Question(
        categoryId = 8,
        questionText = "Was ist 'Retronasal Olfaction' beim Schmecken?",
        answerA = "Das Riechen durch die Nase von außen beim Essen",
        answerB = "Das Wahrnehmen von Aromen durch den Rachenraum rückwärts zur Riechschleimhaut beim Kauen",
        answerC = "Die Empfindlichkeit der Geschmacksknospen nach Krankheit",
        answerD = "Eine Technik der Weinverkostung",
        correctAnswer = 1,
        explanation = "Retronasal Olfaction beschreibt das Wahrnehmen flüchtiger Aromastoffe über den Nasen-Rachen-Raum von innen während des Kauens. Dies ist der Hauptbeitrag zu dem, was wir als 'Geschmack' eines Lebensmittels erleben – die Zunge nimmt nur 5 Grundqualitäten wahr.",
        difficulty = 3,
        funFact = "Bis zu 80 % des empfundenen 'Geschmacks' ist eigentlich Geruch über den Retronasalweg."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Pektinierung' bei der Marmeladen- und Geleeherstellung?",
        answerA = "Das Hinzufügen von Konservierungsstoffen zur Fruchtzubereitung",
        answerB = "Das Gelieren durch Pektin, das bei hohem Zuckergehalt und niedrigem pH-Wert ein Netzwerk bildet",
        answerC = "Das Sterilisieren von Gläsern für die Aufbewahrung",
        answerD = "Das Karamellisieren von Fruchtzucker beim Einkochen",
        correctAnswer = 1,
        explanation = "Pektin ist ein Polysaccharid in Zellwänden vieler Früchte. Bei ausreichend Zucker (>55 %) und niedrigem pH (<3,5) bilden Pektinmoleküle Wasserstoffbrücken und ionische Netzwerke, die das Gel entstehen lassen.",
        difficulty = 3,
        funFact = "Äpfel und Quitten haben besonders viel natürliches Pektin – daher gelierten Quittenpüree und Apfelgelee so gut."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Cold Brew Coffee' und wie unterscheidet er sich chemisch von heißem Kaffee?",
        answerA = "Cold Brew ist gefrorener Kaffee – chemisch identisch mit heißem Kaffee",
        answerB = "Cold Brew wird mit kaltem Wasser extrahiert, wodurch weniger Säure und andere hitzeempfindliche Verbindungen entstehen",
        answerC = "Cold Brew enthält doppelt so viel Koffein wie heißer Kaffee",
        answerD = "Cold Brew wird mit Stickstoff versetzt und ist deshalb milder",
        correctAnswer = 1,
        explanation = "Cold Brew wird 12–24 Stunden mit kaltem Wasser extrahiert. Da keine Hitze verwendet wird, entstehen weniger Chlorogensäure-Abbauprodukte und andere Säuren – das Ergebnis ist ein milderer, weniger bitterer Kaffee mit anderem Aromaprofil.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Acrylamid' in Lebensmitteln und wann entsteht es?",
        answerA = "Ein natürlicher Süßungsmittel in reifen Früchten",
        answerB = "Eine potenziell krebserregende Verbindung, die bei der Maillard-Reaktion über 120 °C aus Asparagin und Zuckern entsteht",
        answerC = "Ein Konservierungsstoff in Tiefkühlprodukten",
        answerD = "Ein Fermentationsprodukt in Sauerkraut",
        correctAnswer = 1,
        explanation = "Acrylamid entsteht bei der Maillard-Reaktion aus der Aminosäure Asparagin und reduzierenden Zuckern bei Temperaturen über 120 °C (Backen, Frittieren, Rösten). Es gilt als potenziell krebserregend für Menschen.",
        difficulty = 3,
        funFact = "Pommes frites, Chips und dunkel gerösteter Kaffee enthalten besonders hohe Acrylamid-Mengen."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Isomaltulose' und warum wird sie als Alternative zu Haushaltszucker vermarktet?",
        answerA = "Ein synthetischer Süßstoff ohne Kalorien",
        answerB = "Ein natürliches Disaccharid aus Honig mit niedrigerem glykämischen Index als Saccharose",
        answerC = "Ein Zuckeralkohol aus Maiskleie",
        answerD = "Ein Fruktooligosaccharid aus Chicorée",
        correctAnswer = 1,
        explanation = "Isomaltulose (Palatinose) ist ein natürliches Disaccharid aus Honig und Zuckerrohr, bestehend aus Glukose und Fruktose. Es wird langsamer verdaut als Saccharose und hat einen niedrigeren glykämischen Index (GI 32 vs. 65).",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist das Besondere an 'Neapoletanischer Pizza' (Vera Pizza Napoletana) gemäß den AVPN-Regeln?",
        answerA = "Sie muss tiefgekühlt und dann aufgebacken werden",
        answerB = "Teig aus Typ-00-Mehl, Meersalz, Hefe und Wasser, mindestens 8 Stunden Gehzeit, Holzofengare bei 485 °C für 60–90 Sekunden",
        answerC = "Sie wird ohne Tomaten zubereitet",
        answerD = "Der Teig enthält Olivenöl und Oregano",
        correctAnswer = 1,
        explanation = "Die Associazione Verace Pizza Napoletana (AVPN) schreibt exakt vor: Typ-00-Mehl, frische Hefe, Meersalz, Wasser, mindestens 8 h Gehzeit, handgeformte Basis, San-Marzano-Tomaten, Fior-di-Latte-Mozzarella, Backen im Holzofen bei 485 °C für 60–90 Sek.",
        difficulty = 3,
        funFact = "Pizza Napoletana hat seit 2017 den Status des immateriellen UNESCO-Kulturerbes."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Pasteurisierung bei ESL-Milch' (Extended Shelf Life)?",
        answerA = "Eine schwache Pasteurisierung bei 63 °C",
        answerB = "Eine Kurzzeiterhitzung auf 125–130 °C für wenige Sekunden, die mehr Keime als normale Pasteurisierung abtötet",
        answerC = "Eine Sterilisierung bei 150 °C wie bei H-Milch",
        answerD = "Eine UV-Bestrahlung ohne Wärme",
        correctAnswer = 1,
        explanation = "ESL-Milch wird bei 125–130 °C für 2–4 Sekunden ultrahocherhitzt (UHT-ähnlich), was deutlich mehr Keime als normale Pasteurisierung (72 °C/15 s) vernichtet. Die Haltbarkeit beträgt ungekühlt bis zu 3 Wochen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Scobby' bei der Kombucha-Herstellung?",
        answerA = "Eine Heferasse für die primäre Fermentation",
        answerB = "Ein symbiotischer Komplex aus Essigsäurebakterien und Hefen in einer Zellulose-Matrix (SCOBY = Symbiotic Culture Of Bacteria and Yeast)",
        answerC = "Ein Enzym, das Koffein aus dem Tee abbaut",
        answerD = "Eine Mineralstoffzugabe zur Regulierung des pH-Werts",
        correctAnswer = 1,
        explanation = "SCOBY (Symbiotic Culture of Bacteria and Yeast) ist die geleeartige Zooglea-Matte aus Essigsäurebakterien und Hefen, die auf dem Kombucha schwimmt. Die Hefen produzieren Alkohol, die Bakterien wandeln ihn in Essigsäure um.",
        difficulty = 3,
        funFact = "Ein SCOBY kann weitergegeben werden – manche 'Starter' sind Jahrzehnte alt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was beschreibt 'Proteolysis' bei der Käsereifung?",
        answerA = "Das Schmelzen von Käsefett beim Erwärmen",
        answerB = "Der enzymatische Abbau von Kasein-Proteinen durch Enzyme des Milchgerinnsels und Reifungsorganismen",
        answerC = "Das Wachstum von Edelschimmel auf der Käserinde",
        answerD = "Das Einlegen von Käse in Salzlake",
        correctAnswer = 1,
        explanation = "Proteolysis ist der enzymatische Abbau von Kasein (Milchprotein) durch Lab-Enzyme, Bakterienenzyme und Pilzenzyme während der Käsereifung. Dabei entstehen Peptide und Aminosäuren, die für Aroma, Textur und Umami-Tiefe verantwortlich sind.",
        difficulty = 3,
        funFact = "Je länger ein Käse reift, desto mehr Proteolysis findet statt – deshalb sind alte Hartkäse so intensiv aromatisch."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Weck' oder 'Einwecken' als Konservierungsmethode?",
        answerA = "Eine Methode zum Trocknen von Lebensmitteln mit Heißluft",
        answerB = "Das Einkochen von Lebensmitteln in luftdicht verschlossenen Gläsern bei 75–100 °C zum Abtöten von Keimen und Erzeugen von Vakuum",
        answerC = "Eine Fermentationsmethode für Gemüse",
        answerD = "Das Einfrieren von Lebensmitteln in Schutzgasatmosphäre",
        correctAnswer = 1,
        explanation = "Das Einwecken (nach Johann Carl Weck, der das Verfahren 1900 patentierte) konserviert Lebensmittel durch Erhitzen in verschlossenen Gläsern. Das Erhitzen tötet Mikroorganismen ab, und beim Abkühlen entsteht ein Vakuum, das den Deckel versiegelt.",
        difficulty = 3,
        funFact = "Johann Weck gründete 1900 die Firma 'Weck', deren Glasnamen zum Gattungsbegriff wurde."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Pommes Soufflées' und was ist das physikalische Prinzip dahinter?",
        answerA = "Pommes frites mit Kräutern überbacken",
        answerB = "Aufgeblähte hohle Kartoffelscheiben, die durch zweifaches Frittieren entstehen: erst bei niedriger, dann bei sehr hoher Temperatur",
        answerC = "Kartoffeln die in Milch aufgekocht werden",
        answerD = "Pommes frites mit Soufflé-Masse gefüllt",
        correctAnswer = 1,
        explanation = "Pommes Soufflées entstehen durch zweistufiges Frittieren: Zuerst bei ~160 °C, dann bei ~190 °C. Der zweite Schritt erhitzt die eingeschlossene Restfeuchte explosionsartig, was die Kartoffelscheibe aufbläht wie einen Ballon.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Butter Poaching' (Butterpochieren) als Garmethode?",
        answerA = "Gemüse in Butter dünsten",
        answerB = "Lebensmittel in teilweise emulgierter Butter bei 55–85 °C schonend garen",
        answerC = "Butter in der Pfanne braun werden lassen um Nussbutter herzustellen",
        answerD = "Fleisch in heißer Butter mit Kräutern begießen",
        correctAnswer = 1,
        explanation = "Beim Butter Poaching wird eine mit Wasser emulgierte Butter (Beurre Monté) als Garmédium auf 55–85 °C gehalten. Zartes Fleisch oder Fisch gart darin extrem schonend, wird dabei vollständig mit Butter aromatisiert und bleibt saftig.",
        difficulty = 3,
        funFact = "Beurre Monté ist der Schlüssel: Erst wenn etwas Wasser in die Butter emulgiert wird, bleibt sie flüssig ohne zu brennen."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Reactive Oxygen Species (ROS)' im Kontext von Fettoxidation in Lebensmitteln?",
        answerA = "Vitamine, die Fette schützen",
        answerB = "Hochreaktive sauerstoffhaltige Verbindungen (Radikale), die Kettenlreaktionen in ungesättigten Fettsäuren auslösen und Ranzigkeit verursachen",
        answerC = "Enzyme die Fettsäuren spalten",
        answerD = "Konservierungsstoffe gegen Oxidation",
        correctAnswer = 1,
        explanation = "ROS wie Hydroxyl-Radikale (·OH) und Superoxid (O₂·⁻) starten Kettenreaktionen an ungesättigten Fettsäuren (Lipidperoxidation). Dabei entstehen Aldehyde und Ketone, die Ranzigkeit verursachen. Antioxidantien (Vitamin E, BHA) unterbrechen diese Ketten.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der Unterschied zwischen 'Osmotischem Einlegen' und 'Fermentativem Einlegen' bei Pickles?",
        answerA = "Beide Methoden sind identisch – nur der Zeitraum unterscheidet sich",
        answerB = "Osmotisches Einlegen nutzt Essiglake (direkte Ansäuerung, keine Fermentation); fermentatives Einlegen nutzt Salz, wodurch Milchsäurebakterien Lactat produzieren",
        answerC = "Fermentatives Einlegen braucht immer Essig, osmotisches nicht",
        answerD = "Nur fermentatives Einlegen macht Produkte haltbar",
        correctAnswer = 1,
        explanation = "Essiggurken aus dem Supermarkt sind meist osmotisch eingelegt (mit Essig-Salz-Lake direkt angesäuert). Echte fermentierte Pickles (z. B. Salzgurken) werden nur mit Salzlake eingelegt und von natürlichen Milchsäurebakterien fermentiert.",
        difficulty = 3,
        funFact = "Fermentierte Pickles enthalten lebende Probiotika – Essig-Pickles nicht."
    ),

    Question(
        categoryId = 8,
        questionText = "Was beschreibt das Konzept 'Flavor Pairing' (Aroma-Pairing) in der modernen Küche?",
        answerA = "Das Kombinieren von Lebensmitteln mit ähnlicher Farbe",
        answerB = "Die Hypothese, dass Lebensmittel mit gemeinsamen Schlüsselaromamolekülen gut harmonieren",
        answerC = "Das Paaren von Wein mit Fleischgerichten nach Region",
        answerD = "Die Kombination von Süß und Sauer für Balance",
        correctAnswer = 1,
        explanation = "Flavor Pairing ist ein Konzept, das Lebensmittel mit überlappenden flüchtigen Aromamolekülen als potenziell gut harmonierend beschreibt. Beispiel: Schokolade und Kaviar teilen bestimmte Aromastoffe. Das Konzept ist wissenschaftlich umstritten.",
        difficulty = 3,
        funFact = "Das Flavor Pairing wurde vom Heston Blumenthal popularisiert, der Schokolade und Kaviar als Paarung vorschlug."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Lipolysis' bei der Reifung von Käse und Schinken?",
        answerA = "Der Abbau von Proteinen durch proteolytische Enzyme",
        answerB = "Der enzymatische Abbau von Fetten zu freien Fettsäuren, die wichtige Aromastoffe sind",
        answerC = "Das Wachstum von Edelschimmel auf der Oberfläche",
        answerD = "Die Reduzierung des Wassergehalts durch Trocknung",
        correctAnswer = 1,
        explanation = "Lipolysis ist der enzymatische Abbau von Triglyceriden (Fetten) zu freien Fettsäuren und Glycerin durch Lipasen. Die entstehenden freien Fettsäuren und deren Derivate (Methyl-Ketone, Ester) sind wichtige Aromavorläufer in gereiftem Käse und Schinken.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Maltodextrin' und wie wird es in der modernen Küche genutzt?",
        answerA = "Ein Süßungsmittel aus Malz mit intensiver Süßkraft",
        answerB = "Ein Stärkehydrolysepartialprodukt, das in der Molekularküche verwendet wird um Fette in ein Pulver umzuwandeln",
        answerC = "Ein Emulgator aus Milcheiweiß",
        answerD = "Ein Ballaststoff aus Weizenkleie",
        correctAnswer = 1,
        explanation = "Maltodextrin (E 1400) ist ein partielles Hydrolyseprodukt von Stärke. Da es hydrophil ist und eine sehr große Oberfläche hat, kann es Fette (z. B. Olivenöl, Kakaobutter) absorbieren und in ein trockenes Pulver umwandeln – eine Technik der Molekularküche.",
        difficulty = 3,
        funFact = "Mit Maltodextrin lässt sich zum Beispiel Olivenölpulver herstellen, das sich auf der Zunge schmilzt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Umami-Synergismus' bei Glutamat und Inosinat?",
        answerA = "Glutamat und Inosinat zusammen schmecken nicht anders als einzeln",
        answerB = "Die Kombination von Glutamat (MSG) und Inosinat (IMP) oder Guanylat (GMP) verstärkt den Umami-Geschmack synergistisch um ein Vielfaches",
        answerC = "Umami wird nur durch Glutamat ausgelöst, Inosinat hat keine Wirkung",
        answerD = "Inosinat neutralisiert den Umami-Effekt von Glutamat",
        correctAnswer = 1,
        explanation = "Glutaminsäure und die Ribonukleotide Inosinat (IMP aus Fleisch) und Guanylat (GMP aus Pilzen) wirken synergetisch: Die Kombination erzeugt bis zu 8-fach stärkere Umami-Empfindung als jede Substanz allein. Deshalb schmeckt Dashi (Kombu + Katsuobushi) so intensiv.",
        difficulty = 3,
        funFact = "Dieser Synergismus ist der Grund, warum Fleisch mit getrockneten Pilzen so intensiv schmeckt."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Balut'?",
        answerA = "Eine philippinische Delikatesse – ein befruchtetes, bebrütetes Entenei, das gekocht wird",
        answerB = "Ein balinesisches Reisgericht mit Kokosmilch",
        answerC = "Eine indonesische Bananenspezialität",
        answerD = "Ein vietnamesisches Nudelgericht",
        correctAnswer = 0,
        explanation = "Balut ist eine philippinische Straßennahrung: ein befruchtetes Entenei, das 14–21 Tage bebrütet wird, bis ein Küken sichtbar ist, und dann hartgekocht und direkt aus der Schale gegessen wird.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist der 'Rangoon-Test' bei Bienenhonig?",
        answerA = "Ein Test zur Messung des Wassergehalts durch Refraktometrie",
        answerB = "Ein Verfahren zur Erkennung von Verunreinigungen durch Hydroxymethylfurfural (HMF)",
        answerC = "Ein sensorischer Test zur Bestimmung der Honigreife am Bienenvolk",
        answerD = "Ein genetischer Test zur Bestimmung der Bienenrasse",
        correctAnswer = 2,
        explanation = "Im Honighandel ist der 'Schleuderhonig-Reife-Test' nach Rangoon ein sensorischer Test: Wenn man einen offenen Honigrahmen horizontal hält und schwenkt, soll reifer Honig (Wassergehalt <18 %) nicht herausfließen. Unausreichend gereifter Honig fließt heraus.",
        difficulty = 3,
        funFact = "Honig mit über 20 % Wassergehalt kann fermentieren und gärt dann zu einer Art Met."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Dry Hopping' beim Bierbrauen?",
        answerA = "Das Darren von Hopfen vor der Verwendung",
        answerB = "Das Hinzufügen von frischem Hopfen nach der Gärung, ohne Hitze, um Aroma ohne Bitterkeit zu extrahieren",
        answerC = "Eine Trocknungsmethode für Malz",
        answerD = "Das Kochen des Bieres ohne Hopfen",
        correctAnswer = 1,
        explanation = "Beim Dry Hopping (Kalthopfung) wird frischer oder pelletierter Hopfen nach der Gärung bei Raumtemperatur oder Kühltemperatur zugegeben. Da keine Hitze beteiligt ist, lösen sich nur die flüchtigen Aromaöle (kein Alpha-Säure-Iso-Umwandlung) – das Bier wird aromatischer ohne bitterer zu werden.",
        difficulty = 3,
        funFact = "Dry Hopping ist besonders bei American IPAs beliebt und erzeugt typische tropisch-zitrische Hopfenprofile."
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Koji-Fermentation' in der westlichen Küche (Non-Traditional Use)?",
        answerA = "Koji wird nur für Sake und Miso eingesetzt, nie in der westlichen Küche",
        answerB = "Westliche Köche verwenden Koji-Sporen (Aspergillus oryzae) zum schnellen Reifen von Fleisch und Umami-Anreicherung in Proteinen innerhalb von Tagen statt Monaten",
        answerC = "Koji dient als Ersatz für Backpulver",
        answerD = "Koji-Fermentation ist nur für fermentierte Getränke geeignet",
        correctAnswer = 1,
        explanation = "In der modernen westlichen Küche (z. B. Noma) werden Koji-Sporen auf Fleisch, Hülsenfrüchte oder Getreide angewendet, um durch die Proteasen und Amalysen des Pilzes schnell Amino-Säuren (Umami) freizusetzen und die Textur zu verändern – in Tagen statt Monaten.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 8,
        questionText = "Was ist 'Pralinage' bei der Schokoladenherstellung?",
        answerA = "Das Verfahren des Temperierens von Kuvertüre",
        answerB = "Eine Paste aus karamellisierten Nüssen oder Mandeln, die gemahlen werden bis eine glatte Fettmasse entsteht",
        answerC = "Das Überziehen von Früchten mit Schokolade",
        answerD = "Das Conchieren von Kakaomasse über 24 Stunden",
        correctAnswer = 1,
        explanation = "Praliné (Praliné-Masse) ist eine glatte, fettreiche Paste aus karamellisierten Nüssen (Haselnüsse, Mandeln), die so fein gemahlen werden, dass das Nussfett austritt und eine cremige Paste entsteht. Sie ist die Grundlage für Pralinenfüllungen.",
        difficulty = 3,
        funFact = "Das Wort 'Praline' leitet sich vom Marshal du Plessis-Praslin ab, dessen Koch die Süßigkeit erfunden haben soll."
    )
)
