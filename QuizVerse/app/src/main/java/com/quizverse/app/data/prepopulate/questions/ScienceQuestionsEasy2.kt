package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsEasy2(): List<Question> = listOf(

    // Question 1 — Weather & Climate
    Question(
        categoryId = 2,
        questionText = "Wie nennt man weiße, flauschige Wolken, die bei schönem Wetter am Himmel zu sehen sind?",
        answerA = "Cirrus",
        answerB = "Nimbus",
        answerC = "Cumulus",
        answerD = "Stratus",
        correctAnswer = 2, // C
        explanation = "Cumuluswolken sind die typischen weißen, geblähten Schönwetterwolken. Sie entstehen durch aufsteigende Warmluft und sehen wie Wattebäusche aus.",
        difficulty = 1,
        funFact = "Cumuluswolken können sich bei starker Aufheizung zu mächtigen Gewitterwolken (Cumulonimbus) entwickeln, die bis zu 15 km hoch reichen."
    ),

    // Question 2 — Weather & Climate
    Question(
        categoryId = 2,
        questionText = "Welche Niederschlagsart entsteht, wenn Regentropfen beim Fall durch eiskalte Luft gefrieren?",
        answerA = "Schnee",
        answerB = "Hagel",
        answerC = "Graupel",
        answerD = "Eisregen",
        correctAnswer = 3, // D
        explanation = "Eisregen entsteht, wenn Regentropfen eine sehr kalte Luftschicht nahe dem Boden durchqueren und dabei gefrieren. Er bildet eine gefährliche Eisschicht auf Oberflächen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 3 — Weather & Climate
    Question(
        categoryId = 2,
        questionText = "Welche der vier Jahreszeiten hat auf der Nordhalbkugel die kürzeste Tageslichtdauer?",
        answerA = "Frühling",
        answerB = "Herbst",
        answerC = "Sommer",
        answerD = "Winter",
        correctAnswer = 3, // D
        explanation = "Im Winter ist die Tageslichtdauer am kürzesten, weil die Nordhalbkugel von der Sonne abgeneigt ist. Am 21. Dezember ist der kürzeste Tag des Jahres (Wintersonnenwende).",
        difficulty = 1,
        funFact = "Am Nordpol gibt es im Winter mehrere Wochen Polarnacht — die Sonne geht gar nicht auf."
    ),

    // Question 4 — Weather & Climate
    Question(
        categoryId = 2,
        questionText = "Was ist ein Tornado?",
        answerA = "Ein tropischer Wirbelsturm über dem Meer",
        answerB = "Ein rotierender Luftwirbel, der den Boden berührt",
        answerC = "Ein starker Schneesturm",
        answerD = "Eine Sandwolke in der Wüste",
        correctAnswer = 1, // B
        explanation = "Ein Tornado ist ein rotierender Luftwirbel, der sich von einer Gewitterwolke bis zum Boden erstreckt. Er kann Windgeschwindigkeiten von über 500 km/h erreichen.",
        difficulty = 1,
        funFact = "Die USA erleben jährlich etwa 1.200 Tornados — mehr als jedes andere Land der Welt. Die meisten entstehen in der sogenannten 'Tornado Alley'."
    ),

    // Question 5 — Weather & Climate
    Question(
        categoryId = 2,
        questionText = "In welcher Klimazone liegt Deutschland?",
        answerA = "Tropisch",
        answerB = "Polar",
        answerC = "Gemäßigt",
        answerD = "Subtropisch",
        correctAnswer = 2, // C
        explanation = "Deutschland liegt in der gemäßigten Klimazone mit vier deutlichen Jahreszeiten, milden Temperaturen und gleichmäßigen Niederschlägen über das Jahr verteilt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 6 — Nutrition & Food Science
    Question(
        categoryId = 2,
        questionText = "Welches Vitamin wird vom menschlichen Körper gebildet, wenn die Haut Sonnenlicht ausgesetzt ist?",
        answerA = "Vitamin A",
        answerB = "Vitamin C",
        answerC = "Vitamin B12",
        answerD = "Vitamin D",
        correctAnswer = 3, // D
        explanation = "Vitamin D wird in der Haut durch UV-B-Strahlung der Sonne gebildet. Es ist wichtig für starke Knochen, da es die Aufnahme von Calcium fördert.",
        difficulty = 1,
        funFact = "Im deutschen Winter ist die Sonneneinstrahlung oft zu schwach für die Vitamin-D-Bildung — daher empfehlen viele Ärzte im Winter ein Supplement."
    ),

    // Question 7 — Nutrition & Food Science
    Question(
        categoryId = 2,
        questionText = "Welches Mineral ist besonders wichtig für die Bildung von Hämoglobin im Blut?",
        answerA = "Calcium",
        answerB = "Eisen",
        answerC = "Magnesium",
        answerD = "Zink",
        correctAnswer = 1, // B
        explanation = "Eisen ist ein wesentlicher Bestandteil von Hämoglobin, dem roten Blutfarbstoff, der Sauerstoff transportiert. Eisenmangel führt zu Blutarmut (Anämie).",
        difficulty = 1,
        funFact = null
    ),

    // Question 8 — Nutrition & Food Science
    Question(
        categoryId = 2,
        questionText = "In welchem Organ findet der größte Teil der Verdauung statt?",
        answerA = "Magen",
        answerB = "Dickdarm",
        answerC = "Leber",
        answerD = "Dünndarm",
        correctAnswer = 3, // D
        explanation = "Im Dünndarm wird der Großteil der Nahrung verdaut und durch die Darmwand ins Blut aufgenommen. Er ist beim Menschen etwa 3 bis 6 Meter lang.",
        difficulty = 1,
        funFact = "Würde man die innere Oberfläche des Dünndarms ausbreiten, hätte sie die Fläche von etwa einem halben Tennisplatz."
    ),

    // Question 9 — Nutrition & Food Science
    Question(
        categoryId = 2,
        questionText = "Welche der folgenden Lebensmittelgruppen liefert die meiste Energie pro Gramm?",
        answerA = "Kohlenhydrate",
        answerB = "Proteine",
        answerC = "Fette",
        answerD = "Ballaststoffe",
        correctAnswer = 2, // C
        explanation = "Fette liefern mit 9 Kilokalorien pro Gramm mehr als doppelt so viel Energie wie Kohlenhydrate oder Proteine (je 4 kcal/g).",
        difficulty = 1,
        funFact = null
    ),

    // Question 10 — Nutrition & Food Science
    Question(
        categoryId = 2,
        questionText = "Welches Vitamin ist vor allem in Zitrusfrüchten wie Orangen enthalten?",
        answerA = "Vitamin K",
        answerB = "Vitamin E",
        answerC = "Vitamin C",
        answerD = "Vitamin A",
        correctAnswer = 2, // C
        explanation = "Zitrusfrüchte sind reich an Vitamin C (Ascorbinsäure), das das Immunsystem stärkt und als Antioxidans wirkt. Ein Mangel verursacht Skorbut.",
        difficulty = 1,
        funFact = "Früher litten Seeleute auf langen Reisen häufig an Skorbut. Die Briten erkannten, dass Zitronen dagegen helfen — daher der Spitzname 'Limeys' für Briten."
    ),

    // Question 11 — Simple Machines
    Question(
        categoryId = 2,
        questionText = "Was ist ein Hebel?",
        answerA = "Eine schiefe Ebene mit Rollen",
        answerB = "Ein fester Stab, der um einen Drehpunkt bewegt wird",
        answerC = "Ein Rad mit Zähnen",
        answerD = "Ein Seil über einer Rolle",
        correctAnswer = 1, // B
        explanation = "Ein Hebel ist eine der einfachsten Maschinen: ein starrer Stab, der sich um einen festen Punkt (Drehpunkt oder Fulkrum) dreht, um Kraft zu vervielfachen.",
        difficulty = 1,
        funFact = "Archimedes soll gesagt haben: 'Gebt mir einen festen Punkt und ich werde die Erde aus den Angeln heben.' — Er meinte damit das Prinzip des Hebels."
    ),

    // Question 12 — Simple Machines
    Question(
        categoryId = 2,
        questionText = "Welche einfache Maschine nutzt man, wenn man eine schwere Kiste auf einem schrägen Brett nach oben schiebt statt sie zu heben?",
        answerA = "Hebel",
        answerB = "Rolle",
        answerC = "Schraube",
        answerD = "Schiefe Ebene",
        correctAnswer = 3, // D
        explanation = "Die schiefe Ebene (schräge Fläche) verringert die benötigte Kraft, um ein Objekt anzuheben, indem der Weg verlängert wird. Rampen sind ein typisches Beispiel.",
        difficulty = 1,
        funFact = null
    ),

    // Question 13 — Simple Machines
    Question(
        categoryId = 2,
        questionText = "Wozu dient eine Rolle (Flaschenzug)?",
        answerA = "Um Geschwindigkeit zu erhöhen",
        answerB = "Um Reibung zu erzeugen",
        answerC = "Um Wärme in Bewegung umzuwandeln",
        answerD = "Um Kraft umzulenken oder zu vervielfachen",
        correctAnswer = 3, // D
        explanation = "Eine Rolle lenkt die Richtung einer Kraft um. Ein Flaschenzug mit mehreren Rollen kann die benötigte Kraft beim Heben schwerer Lasten stark verringern.",
        difficulty = 1,
        funFact = "Kräne auf Baustellen nutzen das Prinzip des Flaschenzugs, um viele Tonnen schwere Lasten mit vergleichsweise kleinen Motoren zu heben."
    ),

    // Question 14 — Simple Machines
    Question(
        categoryId = 2,
        questionText = "Was ist der Vorteil eines Rades mit Achse gegenüber dem Schleppen einer Last?",
        answerA = "Es erzeugt mehr Reibung",
        answerB = "Es macht die Last schwerer",
        answerC = "Es verwandelt Drehbewegung in Vorwärtsbewegung und reduziert Reibung",
        answerD = "Es erhöht die Temperatur der Last",
        correctAnswer = 2, // C
        explanation = "Das Rad wandelt Drehbewegung in Fortbewegung um und reduziert die Reibung drastisch, weil nur ein kleiner Punkt Bodenkontakt hat statt der ganzen Fläche.",
        difficulty = 1,
        funFact = "Das Rad gilt als eine der wichtigsten Erfindungen der Menschheit und wurde vor etwa 5.500 Jahren in Mesopotamien erfunden."
    ),

    // Question 15 — Simple Machines
    Question(
        categoryId = 2,
        questionText = "Was ist eine Schraube im physikalischen Sinne?",
        answerA = "Ein gebogener Hebel",
        answerB = "Eine aufgerollte schiefe Ebene",
        answerC = "Ein Flaschenzug mit Gewinden",
        answerD = "Eine rotierende Rolle",
        correctAnswer = 1, // B
        explanation = "Eine Schraube ist im Grunde eine schiefe Ebene, die spiralförmig um eine Achse gewickelt ist. Durch Drehen wird axiale Kraft in sehr große Druckkraft umgewandelt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 16 — Plants & Botany
    Question(
        categoryId = 2,
        questionText = "Was verlieren Laubbäume im Herbst?",
        answerA = "Ihre Rinde",
        answerB = "Ihre Wurzeln",
        answerC = "Ihre Blätter",
        answerD = "Ihre Äste",
        correctAnswer = 2, // C
        explanation = "Laubbäume werfen im Herbst ihre Blätter ab, um Wasser zu sparen und Schäden durch Frost zu vermeiden. Im Frühling wachsen neue Blätter.",
        difficulty = 1,
        funFact = "Bevor Blätter fallen, bauen Bäume wertvolle Nährstoffe aus ihnen zurück in den Stamm — deshalb verfärben sich Blätter gelb und rot, wenn das Chlorophyll abgebaut wird."
    ),

    // Question 17 — Plants & Botany
    Question(
        categoryId = 2,
        questionText = "Was braucht ein Samen, um zu keimen?",
        answerA = "Nur Sonnenlicht",
        answerB = "Wasser, Wärme und Luft (Sauerstoff)",
        answerC = "Nur Erde und Dünger",
        answerD = "Nur Wasser",
        correctAnswer = 1, // B
        explanation = "Die meisten Samen benötigen zur Keimung Wasser (aktiviert Enzyme), Wärme (fördert Stoffwechsel) und Sauerstoff (für die Atmung). Licht ist erst nach dem Keimen wichtig.",
        difficulty = 1,
        funFact = null
    ),

    // Question 18 — Plants & Botany
    Question(
        categoryId = 2,
        questionText = "Welche Aufgabe haben die Wurzeln einer Pflanze hauptsächlich?",
        answerA = "Fotosynthese betreiben",
        answerB = "Samen produzieren",
        answerC = "Wasser und Nährstoffe aus dem Boden aufnehmen",
        answerD = "Sauerstoff freisetzen",
        correctAnswer = 2, // C
        explanation = "Die Wurzeln verankern die Pflanze im Boden und nehmen Wasser sowie gelöste Mineralstoffe auf. Diese werden durch den Stängel in die Blätter transportiert.",
        difficulty = 1,
        funFact = "Manche Baumwurzeln können bis zu 20 Meter tief in den Boden reichen, um Grundwasser zu erreichen."
    ),

    // Question 19 — Plants & Botany
    Question(
        categoryId = 2,
        questionText = "Welche Insekten übertragen beim Blütenbesuch Pollen und bestäuben so Pflanzen?",
        answerA = "Ameisen",
        answerB = "Bienen",
        answerC = "Termiten",
        answerD = "Käfer",
        correctAnswer = 1, // B
        explanation = "Bienen sind die wichtigsten Bestäuber vieler Pflanzen. Beim Sammeln von Nektar kleben Pollenkörner an ihrem Körper und werden auf die nächste Blüte übertragen.",
        difficulty = 1,
        funFact = "Ohne Bienen würden etwa ein Drittel aller Nahrungspflanzen des Menschen nicht bestäubt werden und keine Früchte bilden."
    ),

    // Question 20 — Plants & Botany
    Question(
        categoryId = 2,
        questionText = "Was ist die Funktion der Blätter bei einer Pflanze?",
        answerA = "Wasser speichern",
        answerB = "Fotosynthese durchführen und Zucker produzieren",
        answerC = "Die Wurzeln schützen",
        answerD = "Samen verbreiten",
        correctAnswer = 1, // B
        explanation = "Blätter sind die Hauptorgane der Fotosynthese. Sie fangen Sonnenlicht auf und wandeln es zusammen mit Wasser und CO2 in Zucker (Glukose) und Sauerstoff um.",
        difficulty = 1,
        funFact = null
    ),

    // Question 21 — Oceans & Water
    Question(
        categoryId = 2,
        questionText = "Welcher Ozean ist der größte der Erde?",
        answerA = "Atlantischer Ozean",
        answerB = "Indischer Ozean",
        answerC = "Arktischer Ozean",
        answerD = "Pazifischer Ozean",
        correctAnswer = 3, // D
        explanation = "Der Pazifische Ozean ist mit über 165 Millionen km² der größte Ozean der Erde. Er bedeckt mehr als ein Drittel der gesamten Erdoberfläche.",
        difficulty = 1,
        funFact = "Der Pazifik ist so groß, dass alle Kontinente der Erde zusammen hineinpassen würden."
    ),

    // Question 22 — Oceans & Water
    Question(
        categoryId = 2,
        questionText = "Was verursacht die Gezeiten (Ebbe und Flut) an den Küsten?",
        answerA = "Der Wind",
        answerB = "Die Erdrotation",
        answerC = "Die Anziehungskraft des Mondes",
        answerD = "Unterwasser-Vulkane",
        correctAnswer = 2, // C
        explanation = "Die Gezeiten werden hauptsächlich durch die Gravitationskraft des Mondes verursacht, der das Wasser der Ozeane anzieht. Auch die Sonne trägt dazu bei.",
        difficulty = 1,
        funFact = "In der Bay of Fundy in Kanada beträgt der Tidenhub bis zu 16 Meter — das ist der größte Tidenunterschied weltweit."
    ),

    // Question 23 — Oceans & Water
    Question(
        categoryId = 2,
        questionText = "Wie viel Prozent des Wassers auf der Erde ist Salzwasser?",
        answerA = "Etwa 50 %",
        answerB = "Etwa 97 %",
        answerC = "Etwa 75 %",
        answerD = "Etwa 30 %",
        correctAnswer = 1, // B
        explanation = "Rund 97 % des gesamten Wassers auf der Erde ist Salzwasser in den Ozeanen. Nur etwa 3 % ist Süßwasser, wovon der Großteil in Gletschern gebunden ist.",
        difficulty = 1,
        funFact = null
    ),

    // Question 24 — Oceans & Water
    Question(
        categoryId = 2,
        questionText = "Was sind Korallenriffe?",
        answerA = "Unterwasser-Berge aus Vulkangestein",
        answerB = "Strukturen aus Muschelschalen",
        answerC = "Strukturen aus Kalkskeletten winziger Lebewesen namens Korallen",
        answerD = "Algenmatten am Meeresboden",
        correctAnswer = 2, // C
        explanation = "Korallenriffe entstehen durch die Kalkgehäuse von Korallenpolypen über Tausende von Jahren. Sie sind die artenreichsten Ökosysteme der Ozeane.",
        difficulty = 1,
        funFact = "Das Great Barrier Reef in Australien ist das größte Korallenriff der Welt und von der Raumstation aus sichtbar."
    ),

    // Question 25 — Oceans & Water
    Question(
        categoryId = 2,
        questionText = "Welcher Ozean trennt Europa von Amerika?",
        answerA = "Indischer Ozean",
        answerB = "Pazifischer Ozean",
        answerC = "Arktischer Ozean",
        answerD = "Atlantischer Ozean",
        correctAnswer = 3, // D
        explanation = "Der Atlantische Ozean liegt zwischen Europa/Afrika im Osten und Nord-/Südamerika im Westen. Er ist der zweitgrößte Ozean der Erde.",
        difficulty = 1,
        funFact = "Der Atlantik wächst jedes Jahr um einige Zentimeter, weil die tektonischen Platten auseinanderdriften."
    ),

    // Question 26 — Space basics
    Question(
        categoryId = 2,
        questionText = "Was ist ein Komet?",
        answerA = "Ein Gesteinsklumpen, der die Erde umkreist",
        answerB = "Ein toter Stern",
        answerC = "Ein Eisklumpen aus dem äußeren Sonnensystem, der beim Sonnenannähern einen Schweif entwickelt",
        answerD = "Ein kleines Schwarzes Loch",
        correctAnswer = 2, // C
        explanation = "Kometen sind Körper aus Eis, Staub und Gestein aus dem äußeren Sonnensystem. Wenn sie sich der Sonne nähern, verdampft das Eis und bildet einen leuchtenden Schweif.",
        difficulty = 1,
        funFact = "Der Halleysche Komet besucht die innere Sonnensystem alle 75 bis 76 Jahre. Das nächste Mal ist er etwa 2061 sichtbar."
    ),

    // Question 27 — Space basics
    Question(
        categoryId = 2,
        questionText = "Wo befinden sich die meisten Asteroiden in unserem Sonnensystem?",
        answerA = "Zwischen Mars und Jupiter im Asteroidengürtel",
        answerB = "Hinter dem Neptun",
        answerC = "Zwischen Venus und Erde",
        answerD = "Im Innern der Sonne",
        correctAnswer = 0, // A
        explanation = "Der Asteroidengürtel zwischen Mars und Jupiter enthält Hunderttausende von Asteroiden — Überreste aus der Entstehung des Sonnensystems.",
        difficulty = 1,
        funFact = "Obwohl der Asteroidengürtel Millionen von Objekten enthält, liegt der durchschnittliche Abstand zwischen ihnen bei Hunderttausenden von Kilometern."
    ),

    // Question 28 — Space basics
    Question(
        categoryId = 2,
        questionText = "Wie nennt man ein System aus Milliarden von Sternen, die durch Gravitation zusammengehalten werden?",
        answerA = "Sternhaufen",
        answerB = "Galaxie",
        answerC = "Nebel",
        answerD = "Planetensystem",
        correctAnswer = 1, // B
        explanation = "Eine Galaxie ist ein riesiges System aus Milliarden von Sternen, Gas, Staub und Dunkler Materie, das durch Gravitation zusammengehalten wird. Die Erde liegt in der Milchstraße.",
        difficulty = 1,
        funFact = "Die Milchstraße enthält schätzungsweise 100 bis 400 Milliarden Sterne und ist Teil der Lokalen Gruppe, einem Cluster von etwa 50 Galaxien."
    ),

    // Question 29 — Space basics
    Question(
        categoryId = 2,
        questionText = "Was ist ein Sternbild?",
        answerA = "Eine Gruppe von Planeten",
        answerB = "Ein Muster aus scheinbar zusammengehörenden Sternen am Himmel",
        answerC = "Ein Schwarzes Loch mit vielen Sternen",
        answerD = "Ein Stern mit Ringen wie Saturn",
        correctAnswer = 1, // B
        explanation = "Sternbilder sind Muster aus Sternen, die von der Erde aus gesehen scheinbar zusammengehören. Die IAU hat 88 offizielle Sternbilder festgelegt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 30 — Space basics
    Question(
        categoryId = 2,
        questionText = "Welcher Mensch betrat als Erster den Mond?",
        answerA = "Juri Gagarin",
        answerB = "Buzz Aldrin",
        answerC = "Neil Armstrong",
        answerD = "Michael Collins",
        correctAnswer = 2, // C
        explanation = "Neil Armstrong war am 20. Juli 1969 der erste Mensch auf dem Mond, als die Apollo-11-Mission erfolgreich landete. Seine Worte: 'Ein kleiner Schritt für einen Menschen...'",
        difficulty = 1,
        funFact = "Neil Armstrong hinterließ seine Fußabdrücke auf dem Mond. Da es auf dem Mond keine Atmosphäre gibt, könnten sie noch Millionen von Jahren erhalten bleiben."
    ),

    // Question 31 — Human senses
    Question(
        categoryId = 2,
        questionText = "Welcher Teil des Ohres wandelt Schallwellen in Nervenimpulse um?",
        answerA = "Ohrmuschel",
        answerB = "Trommelfell",
        answerC = "Hammer",
        answerD = "Hörschnecke (Cochlea)",
        correctAnswer = 3, // D
        explanation = "Die Hörschnecke (Cochlea) im Innenohr enthält winzige Haarzellen, die Schwingungen in elektrische Signale umwandeln, die dann ans Gehirn weitergeleitet werden.",
        difficulty = 1,
        funFact = "Das menschliche Ohr kann Töne im Bereich von etwa 20 bis 20.000 Hz wahrnehmen. Hunde hören bis zu 65.000 Hz."
    ),

    // Question 32 — Human senses
    Question(
        categoryId = 2,
        questionText = "Welche der vier Grundgeschmacksrichtungen nehmen Menschen wahr?",
        answerA = "Süß, Sauer, Salzig und Scharf",
        answerB = "Süß, Sauer, Salzig und Bitter",
        answerC = "Süß, Sauer, Bitter und Minzig",
        answerD = "Süß, Scharf, Salzig und Fettig",
        correctAnswer = 1, // B
        explanation = "Die vier klassischen Grundgeschmäcker sind süß, sauer, salzig und bitter. Heute gilt auch Umami (herzhaft/würzig) als fünfter Grundgeschmack.",
        difficulty = 1,
        funFact = null
    ),

    // Question 33 — Human senses
    Question(
        categoryId = 2,
        questionText = "Warum schmeckt Essen schlechter, wenn man erkältet ist und die Nase verstopft ist?",
        answerA = "Weil Erkältungsviren die Zunge befallen",
        answerB = "Weil Geschmack stark vom Geruch abhängt",
        answerC = "Weil Fieber die Geschmacksknospen deaktiviert",
        answerD = "Weil man weniger Speichel produziert",
        correctAnswer = 1, // B
        explanation = "Etwa 80 % des wahrgenommenen Geschmacks entsteht durch den Geruchssinn. Ist die Nase verstopft, gelangen keine Aromastoffe zur Riechschleimhaut, und das Essen schmeckt fade.",
        difficulty = 1,
        funFact = "Astronauten im Weltall haben aufgedunstete Gesichter durch die fehlende Schwerkraft — das verstopft die Nasennebenhöhlen und alles schmeckt weniger intensiv."
    ),

    // Question 34 — Human senses
    Question(
        categoryId = 2,
        questionText = "Welches Sinnesorgan nimmt Berührung, Druck und Schmerz wahr?",
        answerA = "Das Auge",
        answerB = "Das Ohr",
        answerC = "Die Haut",
        answerD = "Die Zunge",
        correctAnswer = 2, // C
        explanation = "Die Haut ist das größte Sinnesorgan des Menschen. Sie enthält Millionen von Rezeptoren für Berührung, Druck, Schmerz, Wärme und Kälte.",
        difficulty = 1,
        funFact = "Die Fingerkuppen sind mit bis zu 2.500 Berührungsrezeptoren pro cm² die empfindlichsten Hautbereiche des menschlichen Körpers."
    ),

    // Question 35 — Human senses
    Question(
        categoryId = 2,
        questionText = "Was ist Propriozeption?",
        answerA = "Das Sehen von Dingen in der Ferne",
        answerB = "Das Wahrnehmen der eigenen Körperposition und Bewegung",
        answerC = "Das Riechen sehr schwacher Gerüche",
        answerD = "Das Hören sehr hoher Töne",
        correctAnswer = 1, // B
        explanation = "Propriozeption ist der 'Körpersinn' — er informiert das Gehirn ständig über die Position und Bewegung der Körperteile, auch ohne hinzusehen.",
        difficulty = 1,
        funFact = "Dank Propriozeption können wir mit geschlossenen Augen die Nase berühren. Alkohol beeinträchtigt diesen Sinn — daher fordert die Polizei bei Alkoholtests diesen Nasentest."
    ),

    // Question 36 — Electricity basics
    Question(
        categoryId = 2,
        questionText = "Was ist ein elektrischer Stromkreis?",
        answerA = "Eine Batterie, die Strom speichert",
        answerB = "Ein geschlossener Weg, durch den Elektronen fließen können",
        answerC = "Ein Material, das Strom nicht leitet",
        answerD = "Ein Gerät zum Messen von Spannung",
        correctAnswer = 1, // B
        explanation = "Ein elektrischer Stromkreis ist ein geschlossener Weg von einer Spannungsquelle (z.B. Batterie) durch Leitungen und Verbraucher (z.B. Lampe) zurück zur Quelle.",
        difficulty = 1,
        funFact = null
    ),

    // Question 37 — Electricity basics
    Question(
        categoryId = 2,
        questionText = "Welches Material leitet elektrischen Strom gut?",
        answerA = "Holz",
        answerB = "Glas",
        answerC = "Kupfer",
        answerD = "Gummi",
        correctAnswer = 2, // C
        explanation = "Kupfer ist ein hervorragender elektrischer Leiter, weil seine Elektronen sich leicht frei bewegen können. Deshalb werden Kabel aus Kupfer hergestellt.",
        difficulty = 1,
        funFact = "Silber leitet Strom sogar noch besser als Kupfer, ist aber zu teuer für normale Kabel. Kupfer ist das ideale Kompromissmaterial."
    ),

    // Question 38 — Electricity basics
    Question(
        categoryId = 2,
        questionText = "Was ist statische Elektrizität?",
        answerA = "Strom aus der Steckdose",
        answerB = "Elektrische Ladung, die sich auf einem Material angesammelt hat und nicht fließt",
        answerC = "Die Spannung in einer Batterie",
        answerD = "Der Strom durch einen Magneten",
        correctAnswer = 1, // B
        explanation = "Statische Elektrizität entsteht, wenn sich elektrische Ladungen auf einem Material ansammeln, z.B. durch Reiben. Der Funken beim Anziehen eines Pullovers ist statische Elektrizität.",
        difficulty = 1,
        funFact = "Blitze sind ein natürliches Beispiel für statische Elektrizität — es entlädt sich eine enorme elektrische Ladung zwischen Wolke und Boden."
    ),

    // Question 39 — Electricity basics
    Question(
        categoryId = 2,
        questionText = "Was macht ein Isolator in der Elektrik?",
        answerA = "Er verstärkt den Strom",
        answerB = "Er leitet Strom besonders gut",
        answerC = "Er verhindert den Stromfluss",
        answerD = "Er speichert elektrische Energie",
        correctAnswer = 2, // C
        explanation = "Isolatoren sind Materialien, die elektrischen Strom nicht oder kaum leiten. Gummi, Plastik und Glas sind typische Isolatoren und schützen vor Stromschlägen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 40 — Electricity basics
    Question(
        categoryId = 2,
        questionText = "Wozu dient eine Batterie?",
        answerA = "Zur Messung elektrischer Spannung",
        answerB = "Zur Umwandlung von Strom in Licht",
        answerC = "Als Isolator für Kabel",
        answerD = "Zur Speicherung chemischer Energie, die in elektrische Energie umgewandelt wird",
        correctAnswer = 3, // D
        explanation = "Eine Batterie speichert chemische Energie und wandelt sie durch chemische Reaktionen in elektrische Energie um. Sie liefert Gleichstrom über ihre Pole.",
        difficulty = 1,
        funFact = "Die erste echte Batterie wurde 1800 von Alessandro Volta erfunden. Ihm zu Ehren wurde die Einheit der elektrischen Spannung 'Volt' benannt."
    ),

    // Question 41 — Materials & Matter
    Question(
        categoryId = 2,
        questionText = "Was sind Metalle typischerweise?",
        answerA = "Durchsichtig und spröde",
        answerB = "Gut leitend für Wärme und Strom, glänzend und formbar",
        answerC = "Leicht und nicht leitend",
        answerD = "Weich wie Wachs bei Raumtemperatur",
        correctAnswer = 1, // B
        explanation = "Metalle sind typischerweise gute Leiter für Wärme und elektrischen Strom, haben einen metallischen Glanz und sind dehnbar und biegsam (duktil und malleable).",
        difficulty = 1,
        funFact = "Quecksilber ist das einzige Metall, das bei Raumtemperatur flüssig ist. Alle anderen Metalle sind bei 20 °C fest."
    ),

    // Question 42 — Materials & Matter
    Question(
        categoryId = 2,
        questionText = "Woraus wird Glas hauptsächlich hergestellt?",
        answerA = "Sand (Siliziumdioxid)",
        answerB = "Kalkstein",
        answerC = "Ton",
        answerD = "Kohle",
        correctAnswer = 0, // A
        explanation = "Glas wird hauptsächlich aus Quarzsand (Siliziumdioxid, SiO₂) hergestellt, der bei sehr hohen Temperaturen geschmolzen wird. Soda und Kalk werden zugegeben, um den Schmelzpunkt zu senken.",
        difficulty = 1,
        funFact = "Glas ist eigentlich kein Feststoff im wissenschaftlichen Sinne, sondern eine amorphe (nicht kristalline) Substanz — manchmal auch als unterkühlte Flüssigkeit bezeichnet."
    ),

    // Question 43 — Materials & Matter
    Question(
        categoryId = 2,
        questionText = "Welche der folgenden Materialien ist ein Kunststoff (Plastik)?",
        answerA = "Aluminium",
        answerB = "Polyethylen",
        answerC = "Quarz",
        answerD = "Zement",
        correctAnswer = 1, // B
        explanation = "Polyethylen (PE) ist einer der häufigsten Kunststoffe der Welt, aus dem Plastiktüten, Flaschen und Verpackungen hergestellt werden.",
        difficulty = 1,
        funFact = null
    ),

    // Question 44 — Materials & Matter
    Question(
        categoryId = 2,
        questionText = "Was passiert bei der Papierherstellung mit Holz?",
        answerA = "Holz wird zu Pulver gemahlen und direkt gepresst",
        answerB = "Holz wird zu Holzfasern aufgelöst, die zu einem dünnen Bogen gepresst und getrocknet werden",
        answerC = "Holz wird erhitzt, bis es zu einer Flüssigkeit wird",
        answerD = "Holz wird mit Säure behandelt und zu Glas geschmolzen",
        correctAnswer = 1, // B
        explanation = "Bei der Papierherstellung wird Holz chemisch oder mechanisch zu Zellulosefasern (Zellstoff) aufgelöst. Diese werden in Wasser suspendiert, auf ein Sieb gegossen, gepresst und getrocknet.",
        difficulty = 1,
        funFact = "Für eine Tonne Papier werden etwa 2 bis 3 Tonnen Holz benötigt. Recyceltes Papier spart erhebliche Mengen an Holz, Wasser und Energie."
    ),

    // Question 45 — Materials & Matter
    Question(
        categoryId = 2,
        questionText = "Welches Symbol auf Verpackungen zeigt an, dass ein Material recycelt werden kann?",
        answerA = "Ein rotes Kreuz",
        answerB = "Ein Dreieck mit Pfeilen (Recycling-Symbol)",
        answerC = "Ein blaues Quadrat",
        answerD = "Ein grüner Stern",
        correctAnswer = 1, // B
        explanation = "Das universelle Recycling-Symbol ist ein Dreieck aus drei Pfeilen, die einen Kreislauf symbolisieren. Es zeigt an, dass ein Material recycelbar ist.",
        difficulty = 1,
        funFact = "Das Recycling-Symbol wurde 1970 von dem Studenten Gary Anderson für einen Wettbewerb anlässlich des ersten Earth Day entworfen."
    ),

    // Question 46 — Animals & Biology
    Question(
        categoryId = 2,
        questionText = "Warum wandern viele Vogelarten im Herbst in wärmere Gebiete?",
        answerA = "Um neue Nester zu bauen",
        answerB = "Weil sie den kürzeren Tagen folgen",
        answerC = "Um Nahrung zu finden, die im Winter in der Heimat fehlt",
        answerD = "Weil andere Tiere sie vertreiben",
        correctAnswer = 2, // C
        explanation = "Zugvögel wandern im Herbst in wärmere Regionen, weil in der Heimat Insekten und andere Nahrungsquellen im Winter fehlen. Im Frühling kehren sie zur Brut zurück.",
        difficulty = 1,
        funFact = "Der Arktische Seeschwalbe legt den längsten Zugweg aller Tiere zurück: fast 70.000 km pro Jahr zwischen Arktis und Antarktis."
    ),

    // Question 47 — Animals & Biology
    Question(
        categoryId = 2,
        questionText = "Was tun Bären im Winter?",
        answerA = "Sie wandern in wärmere Gebiete",
        answerB = "Sie halten Winterschlaf oder Winterruhe",
        answerC = "Sie häuten sich",
        answerD = "Sie werden aktiver und jagen mehr",
        correctAnswer = 1, // B
        explanation = "Bären halten im Winter Winterruhe (eine tiefe, aber nicht echte Winterschlaf-Torpor), in der ihr Stoffwechsel verlangsamt, und zehren von ihren Fettreserven.",
        difficulty = 1,
        funFact = "Bärinnen bringen während der Winterruhe ihre Jungen zur Welt. Die Kleinen trinken Milch, während die Mutter schläft und dabei keine Nahrung zu sich nimmt."
    ),

    // Question 48 — Animals & Biology
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter Tarnung (Camouflage) bei Tieren?",
        answerA = "Eine Färbung, die Feinde warnt",
        answerB = "Das Anpassen an das Aussehen der Umgebung, um nicht aufzufallen",
        answerC = "Giftige Substanzen auf der Haut",
        answerD = "Das Nachahmung von Geräuschen",
        correctAnswer = 1, // B
        explanation = "Tarnung ist eine Anpassung, bei der Tiere durch Farbe, Muster oder Form ihrer Umgebung ähneln, um für Räuber oder Beute unsichtbar zu sein.",
        difficulty = 1,
        funFact = "Der Tintenfisch kann seine Hautfarbe und -textur in Millisekunden ändern — er ist eines der schnellsten Tarnungskünstler der Natur."
    ),

    // Question 49 — Animals & Biology
    Question(
        categoryId = 2,
        questionText = "Was ist ein Ökosystem?",
        answerA = "Eine einzelne Tierart und ihr Lebensraum",
        answerB = "Die Gesamtheit aller Lebewesen und ihrer unbelebten Umwelt in einem Gebiet",
        answerC = "Ein Schutzgebiet ohne menschliche Einflüsse",
        answerD = "Eine Liste aller Tier- und Pflanzenarten eines Landes",
        correctAnswer = 1, // B
        explanation = "Ein Ökosystem umfasst alle Lebewesen (Pflanzen, Tiere, Mikroorganismen) in einem Gebiet sowie die unbelebte Umwelt (Wasser, Boden, Klima) und ihre Wechselbeziehungen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 50 — Animals & Biology
    Question(
        categoryId = 2,
        questionText = "Was beschreibt eine Nahrungskette?",
        answerA = "Die Verdauungswege eines einzelnen Tieres",
        answerB = "Die Reihenfolge, in der Energie und Nährstoffe von einer Art zur nächsten weitergegeben werden",
        answerC = "Eine Liste von Tieren, die dieselbe Nahrung fressen",
        answerD = "Den täglichen Nahrungsbedarf eines Tieres",
        correctAnswer = 1, // B
        explanation = "Eine Nahrungskette zeigt, wer wen frisst: z.B. Pflanze → Raupe → Vogel → Habicht. Energie und Nährstoffe fließen von Produzenten über Konsumenten zu Destruenten.",
        difficulty = 1,
        funFact = "In der Natur gibt es selten einfache Nahrungsketten — meist bilden sich komplexe Nahrungsnetze, in denen viele Arten miteinander verbunden sind."
    ),

)
