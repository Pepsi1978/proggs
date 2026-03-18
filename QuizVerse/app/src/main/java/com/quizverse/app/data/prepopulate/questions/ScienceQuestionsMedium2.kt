package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsMedium2(): List<Question> = listOf(

    // Question 1 – Thermodynamics: Entropy
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der zweite Hauptsatz der Thermodynamik bezüglich der Entropie?",
        answerA = "Die Entropie eines geschlossenen Systems bleibt immer konstant",
        answerB = "Die Entropie eines geschlossenen Systems kann nur abnehmen",
        answerC = "Die Entropie eines geschlossenen Systems nimmt mit der Zeit zu oder bleibt gleich",
        answerD = "Entropie ist nur in offenen Systemen definiert",
        correctAnswer = 2, // C
        explanation = "Der zweite Hauptsatz der Thermodynamik besagt, dass die Entropie (ein Maß für Unordnung) in einem abgeschlossenen System nie abnimmt. Irreversible Prozesse erhöhen die Entropie stets.",
        difficulty = 2,
        funFact = "Die steigende Entropie erklärt, warum die Zeit nur vorwärts verläuft — physikalisch nennt man das den 'Zeitpfeil'."
    ),

    // Question 2 – Thermodynamics: Heat transfer methods
    Question(
        categoryId = 2,
        questionText = "Welche drei Arten der Wärmeübertragung gibt es in der Physik?",
        answerA = "Diffusion, Osmose und Konvektion",
        answerB = "Leitung (Konduktion), Strömung (Konvektion) und Wärmestrahlung",
        answerC = "Strahlung, Konvektion und Absorption",
        answerD = "Induktion, Konduktion und Reflexion",
        correctAnswer = 1, // B
        explanation = "Wärme wird auf drei Wegen übertragen: durch Wärmeleitung (Konduktion) bei direktem Kontakt, durch Konvektion (Mitführung durch strömende Flüssigkeiten/Gase) und durch Wärmestrahlung (Infrarotstrahlung ohne Trägermedium).",
        difficulty = 2,
        funFact = null
    ),

    // Question 3 – Thermodynamics: Specific heat capacity
    Question(
        categoryId = 2,
        questionText = "Welches Material hat die höchste spezifische Wärmekapazität unter diesen Optionen?",
        answerA = "Aluminium",
        answerB = "Eisen",
        answerC = "Kupfer",
        answerD = "Wasser",
        correctAnswer = 3, // D
        explanation = "Wasser hat mit ca. 4186 J/(kg·K) eine außergewöhnlich hohe spezifische Wärmekapazität — mehr als viermal so hoch wie Aluminium (900 J/(kg·K)) und fast zehnmal so hoch wie Kupfer (385 J/(kg·K)).",
        difficulty = 2,
        funFact = "Die hohe Wärmekapazität des Wassers ist der Grund, warum Küstenregionen gemäßigtere Temperaturen haben als Binnengebiete."
    ),

    // Question 4 – Thermodynamics: Phase diagrams
    Question(
        categoryId = 2,
        questionText = "Was bezeichnet man als 'Tripelpunkt' in einem Phasendiagramm?",
        answerA = "Den Punkt, an dem eine Substanz drei verschiedene Aggregatzustände gleichzeitig annimmt",
        answerB = "Den Druck, bei dem ein Stoff von flüssig zu gasförmig wechselt",
        answerC = "Den Punkt maximaler Dichte einer Flüssigkeit",
        answerD = "Die Temperatur, bei der alle drei Schmelzprozesse gleichzeitig ablaufen",
        correctAnswer = 0, // A
        explanation = "Der Tripelpunkt ist der einzige Punkt in einem Phasendiagramm, bei dem alle drei Aggregatzustände (fest, flüssig, gasförmig) einer Substanz im thermodynamischen Gleichgewicht koexistieren.",
        difficulty = 2,
        funFact = "Der Tripelpunkt des Wassers liegt bei genau 273,16 K (0,01 °C) und 611,73 Pa — er wird als Referenzpunkt für die Kelvin-Skala genutzt."
    ),

    // Question 5 – Thermodynamics: Thermal equilibrium
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der nullte Hauptsatz der Thermodynamik?",
        answerA = "Wenn zwei Systeme jeweils im thermischen Gleichgewicht mit einem dritten sind, sind sie auch untereinander im Gleichgewicht",
        answerB = "Die innere Energie eines idealen Gases ist nur von der Temperatur abhängig",
        answerC = "Energie kann weder erzeugt noch vernichtet werden",
        answerD = "Die Entropie eines perfekten Kristalls ist am absoluten Nullpunkt gleich null",
        correctAnswer = 0, // A
        explanation = "Der nullte Hauptsatz definiert das Konzept der Temperatur: Sind Körper A und B je im thermischen Gleichgewicht mit Körper C, dann sind A und B auch miteinander im Gleichgewicht. Dies ist die Grundlage für Temperaturmessung.",
        difficulty = 2,
        funFact = null
    ),

    // Question 6 – Electromagnetism: Maxwell simplified
    Question(
        categoryId = 2,
        questionText = "Was bewirkt ein zeitlich veränderliches Magnetfeld laut den Maxwellschen Gleichungen?",
        answerA = "Es erzeugt ein elektrisches Feld (Induktion)",
        answerB = "Es erhöht die elektrische Leitfähigkeit im umgebenden Material",
        answerC = "Es hält das elektrische Feld konstant",
        answerD = "Es neutralisiert elektrische Ladungen in seiner Umgebung",
        correctAnswer = 0, // A
        explanation = "Laut Faradaysches Induktionsgesetz (eine der Maxwellschen Gleichungen) erzeugt ein sich zeitlich änderndes Magnetfeld ein elektrisches Wirbelfeld. Dieses Prinzip ist die Grundlage von Transformatoren und Generatoren.",
        difficulty = 2,
        funFact = "James Clerk Maxwell fasste 1865 vier Gleichungen zusammen, die das gesamte klassische Elektromagnetismus beschreiben — und sagte damit die Existenz elektromagnetischer Wellen voraus."
    ),

    // Question 7 – Electromagnetism: Electromagnetic spectrum
    Question(
        categoryId = 2,
        questionText = "Welche elektromagnetische Strahlung hat die höchste Frequenz?",
        answerA = "Infrarotstrahlung",
        answerB = "Ultraviolettstrahlung",
        answerC = "Röntgenstrahlung",
        answerD = "Gammastrahlung",
        correctAnswer = 3, // D
        explanation = "Das elektromagnetische Spektrum reicht von Radiowellen (niedrigste Frequenz) über Mikrowellen, Infrarot, sichtbares Licht, UV, Röntgen bis zu Gammastrahlung (höchste Frequenz und Energie).",
        difficulty = 2,
        funFact = null
    ),

    // Question 8 – Electromagnetism: Inductance
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter der Induktivität (Selbstinduktion) einer Spule?",
        answerA = "Den elektrischen Widerstand einer Spule bei Gleichstrom",
        answerB = "Die Eigenschaft einer Spule, einer Änderung des durch sie fließenden Stroms zu widerstehen",
        answerC = "Die maximale Stromstärke, die eine Spule ohne Überhitzung leiten kann",
        answerD = "Die Fähigkeit einer Spule, elektrische Ladungen zu speichern",
        correctAnswer = 1, // B
        explanation = "Durch Selbstinduktion erzeugt eine Spule bei Stromänderung eine Gegenspannung, die der Änderung entgegenwirkt. Die Induktivität L (in Henry) beschreibt diesen Effekt — er wird z.B. in Drosselspulen genutzt.",
        difficulty = 2,
        funFact = "Induktivitäten werden in Schaltnetzteilen und Hochfrequenzfiltern eingesetzt — fast jedes Ladegerät enthält eine Spule."
    ),

    // Question 9 – Electromagnetism: Faraday's law
    Question(
        categoryId = 2,
        questionText = "Wovon hängt die induzierte Spannung in einer Spule laut dem Faradayschen Induktionsgesetz ab?",
        answerA = "Nur von der Stärke des Magnetfeldes",
        answerB = "Nur von der Geschwindigkeit der Spulenbewegung",
        answerC = "Vom elektrischen Widerstand der Spule",
        answerD = "Von der Anzahl der Windungen und der zeitlichen Änderungsrate des magnetischen Flusses",
        correctAnswer = 3, // D
        explanation = "Das Faradaysche Induktionsgesetz besagt: U = -N · dΦ/dt. Die induzierte Spannung ist proportional zur Windungszahl N und zur Änderungsrate des magnetischen Flusses Φ über die Zeit.",
        difficulty = 2,
        funFact = null
    ),

    // Question 10 – Electromagnetism: Magnetic fields
    Question(
        categoryId = 2,
        questionText = "Was sind Feldlinien in einem Magnetfeld und welche Eigenschaft haben sie?",
        answerA = "Sie sind geschlossene Kurven, die vom Nordpol zum Südpol außerhalb und vom Südpol zum Nordpol innerhalb verlaufen",
        answerB = "Sie verlaufen immer in geraden Linien und zeigen die Richtung der Kraft an",
        answerC = "Sie beginnen am Nordpol und enden am Südpol, ohne sich zu schließen",
        answerD = "Sie verlaufen immer senkrecht zur Richtung der magnetischen Kraft",
        correctAnswer = 0, // A
        explanation = "Magnetische Feldlinien sind immer geschlossen. Sie verlaufen außerhalb eines Magneten vom Nord- zum Südpol und innerhalb des Magneten vom Süd- zum Nordpol. Sie schneiden sich nie und ihre Dichte gibt die Feldstärke an.",
        difficulty = 2,
        funFact = "Im Gegensatz zu elektrischen Feldlinien (die an Ladungen beginnen und enden) bilden magnetische Feldlinien immer geschlossene Schleifen — es gibt keine magnetischen 'Monopole'."
    ),

    // Question 11 – Cell Biology: Organelles
    Question(
        categoryId = 2,
        questionText = "Welches Zellorganell wird als 'Kraftwerk der Zelle' bezeichnet und warum?",
        answerA = "Der Zellkern, weil er die genetische Information speichert",
        answerB = "Das Mitochondrium, weil dort ATP durch Zellatmung produziert wird",
        answerC = "Das Endoplasmatische Retikulum, weil es Proteine transportiert",
        answerD = "Der Golgi-Apparat, weil er Substanzen für den Export verpackt",
        correctAnswer = 1, // B
        explanation = "Mitochondrien erzeugen durch oxidative Phosphorylierung ATP (Adenosintriphosphat), den universellen Energieträger der Zelle. Sie besitzen eine eigene DNA und stammen evolutionär von Bakterien ab (Endosymbiontentheorie).",
        difficulty = 2,
        funFact = "Mitochondrien haben eine eigene DNA und vermehren sich durch Teilung unabhängig von der Zelle — ein Hinweis auf ihre bakterielle Herkunft vor ca. 1,5 Milliarden Jahren."
    ),

    // Question 12 – Cell Biology: Cell membrane
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das 'Fluid-Mosaik-Modell' der Zellmembran?",
        answerA = "Die Membran besteht aus einer fließenden Phospholipid-Doppelschicht mit eingelagerten Proteinen",
        answerB = "Die Membran ist eine starre Doppelschicht aus Proteinen",
        answerC = "Die Membran besteht ausschließlich aus Cholesterin-Molekülen",
        answerD = "Die Membran besteht aus einer einschichtigen Proteinstruktur",
        correctAnswer = 0, // A
        explanation = "Das Fluid-Mosaik-Modell (Singer & Nicolson, 1972) beschreibt die Zellmembran als dynamische Phospholipid-Doppelschicht, in der Proteine wie Inseln (Mosaik) schwimmen. Die Membran ist flexibel (fluid) und reguliert den Stoffaustausch.",
        difficulty = 2,
        funFact = null
    ),

    // Question 13 – Cell Biology: Mitosis stages
    Question(
        categoryId = 2,
        questionText = "In welcher Phase der Mitose werden die Chromosomen an der Äquatorialplatte der Zelle ausgerichtet?",
        answerA = "Prophase",
        answerB = "Metaphase",
        answerC = "Anaphase",
        answerD = "Telophase",
        correctAnswer = 1, // B
        explanation = "In der Metaphase wandern die Chromosomen zur Zellmitte (Äquatorialplatte) und werden dort durch Spindelfasern gehalten. Dies ermöglicht die gleichmäßige Aufteilung auf die Tochterzellen in der nachfolgenden Anaphase.",
        difficulty = 2,
        funFact = "Die korrekte Ausrichtung aller Chromosomen in der Metaphase ist entscheidend — fehlt ein Chromosom oder liegt es falsch, löst ein Kontrollmechanismus den Zellselbstmord (Apoptose) aus."
    ),

    // Question 14 – Cell Biology: Cell signaling
    Question(
        categoryId = 2,
        questionText = "Was ist ein Rezeptor im Zusammenhang mit der zellulären Signalübertragung?",
        answerA = "Ein Zellorganell, das Energie umwandelt",
        answerB = "Ein Protein (meist in der Zellmembran), das spezifisch an Botenstoffe bindet und ein Signal ins Zellinnere weiterleitet",
        answerC = "Eine Zellstruktur, die Hormone produziert",
        answerD = "Ein Abschnitt der DNA, der auf Umweltreize reagiert",
        correctAnswer = 1, // B
        explanation = "Rezeptoren sind spezialisierte Proteine, die Signalmoleküle (Liganden) wie Hormone, Neurotransmitter oder Wachstumsfaktoren binden. Die Bindung löst eine Signalkaskade aus, die das Verhalten der Zelle verändert.",
        difficulty = 2,
        funFact = null
    ),

    // Question 15 – Cell Biology: Stem cells
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen totipotenten und pluripotenten Stammzellen?",
        answerA = "Pluripotente Zellen sind reifer und spezialisierter als totipotente",
        answerB = "Totipotente Stammzellen können nur Körperzellen bilden, pluripotente auch Keimzellen",
        answerC = "Es gibt keinen Unterschied, beide Begriffe sind synonym",
        answerD = "Totipotente Zellen können einen vollständigen Organismus bilden, pluripotente nur alle Körperzelltypen (nicht extraembryonales Gewebe)",
        correctAnswer = 3, // D
        explanation = "Totipotente Stammzellen (z.B. befruchtete Eizelle, frühe Blastomeren) können ein vollständiges Lebewesen inklusive Plazenta entwickeln. Pluripotente Stammzellen (z.B. embryonale Stammzellen) können alle Zelltypen des Körpers, aber kein extraembryonales Gewebe bilden.",
        difficulty = 2,
        funFact = "2006 gelang es Shinya Yamanaka, adulte Körperzellen in pluripotente Stammzellen (iPS-Zellen) umzuprogrammieren — dafür erhielt er 2012 den Nobelpreis."
    ),

    // Question 16 – Biochemistry: Enzymes
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem 'aktiven Zentrum' eines Enzyms?",
        answerA = "Den Teil des Enzyms, der Energie speichert",
        answerB = "Die spezifische Bindungsstelle am Enzym, an der das Substrat bindet und die Reaktion katalysiert wird",
        answerC = "Den Kern des Enzyms, der aus DNA besteht",
        answerD = "Die äußere Hülle des Enzyms, die es vor Abbau schützt",
        correctAnswer = 1, // B
        explanation = "Das aktive Zentrum ist eine dreidimensionale Kavität am Enzymprotein mit einer spezifischen Form, die nur zu bestimmten Substraten passt (Schlüssel-Schloss-Prinzip). Dort wird die chemische Reaktion durch Senkung der Aktivierungsenergie katalysiert.",
        difficulty = 2,
        funFact = "Temperatur und pH-Wert können die Form des aktiven Zentrums verändern (Denaturierung) — deshalb arbeiten Enzyme nur in engen Bedingungsbereichen optimal."
    ),

    // Question 17 – Biochemistry: ATP
    Question(
        categoryId = 2,
        questionText = "Wie wird ATP in der Zelle als Energiequelle genutzt?",
        answerA = "ATP wird direkt verbrannt, um Wärme zu erzeugen",
        answerB = "ATP wird in Glukose umgewandelt und dann weiter genutzt",
        answerC = "ATP gibt Elektronen an andere Moleküle ab",
        answerD = "Die Hydrolyse der dritten Phosphatbindung setzt Energie frei, die für zelluläre Prozesse genutzt wird",
        correctAnswer = 3, // D
        explanation = "ATP (Adenosintriphosphat) ist der universelle Energieträger der Zelle. Bei der Hydrolyse von ATP zu ADP und Phosphat werden ca. 30 kJ/mol freigesetzt. Diese Energie treibt Muskelkontraktion, aktiven Transport, Biosynthese und andere Prozesse an.",
        difficulty = 2,
        funFact = "Ein Mensch produziert täglich sein eigenes Körpergewicht an ATP — das Molekül wird dabei im Sekundentakt recycelt (ADP → ATP)."
    ),

    // Question 18 – Biochemistry: Amino acids
    Question(
        categoryId = 2,
        questionText = "Was sind essentielle Aminosäuren?",
        answerA = "Aminosäuren, die vom Körper aus Glukose selbst synthetisiert werden können",
        answerB = "Aminosäuren, die nur in tierischen Produkten vorkommen",
        answerC = "Aminosäuren, die ausschließlich in Proteinen vorkommen und nicht frei in der Zelle existieren",
        answerD = "Aminosäuren, die der Körper nicht selbst herstellen kann und mit der Nahrung aufnehmen muss",
        correctAnswer = 3, // D
        explanation = "Der menschliche Körper kann 9 der 20 proteinogenen Aminosäuren nicht selbst synthetisieren — diese müssen über die Nahrung aufgenommen werden. Dazu gehören u.a. Leucin, Isoleucin, Valin, Lysin und Tryptophan.",
        difficulty = 2,
        funFact = null
    ),

    // Question 19 – Biochemistry: Lipids
    Question(
        categoryId = 2,
        questionText = "Welche Funktion haben Phospholipide im menschlichen Körper?",
        answerA = "Sie dienen hauptsächlich als Energiespeicher in Fettzellen",
        answerB = "Sie sind der Hauptbestandteil der Zellmembranen aller Körperzellen",
        answerC = "Sie transportieren Sauerstoff im Blut",
        answerD = "Sie fungieren als Hormone und regulieren den Stoffwechsel",
        correctAnswer = 1, // B
        explanation = "Phospholipide bilden die Grundstruktur aller biologischen Membranen. Ihr amphiphiler Charakter (hydrophiler Kopf, hydrophober Schwanz) führt spontan zur Bildung der Lipiddoppelschicht, die Zellen umhüllt.",
        difficulty = 2,
        funFact = "Der menschliche Körper enthält etwa 37 Billionen Zellen — jede ist von einer Phospholipid-Doppelschicht umgeben, deren Gesamtfläche mehrere Tausend Quadratmeter betragen würde."
    ),

    // Question 20 – Biochemistry: Carbohydrate metabolism
    Question(
        categoryId = 2,
        questionText = "Was ist Glykolyse?",
        answerA = "Der Abbau von Glykogen in der Leber zu Glukose",
        answerB = "Die Synthese von Glukose aus Aminosäuren in der Leber",
        answerC = "Die schrittweise Aufspaltung von Glukose zu Pyruvat im Zytoplasma unter ATP-Gewinn",
        answerD = "Die vollständige Oxidation von Glukose zu CO₂ und H₂O im Mitochondrium",
        correctAnswer = 2, // C
        explanation = "Die Glykolyse ist der erste Schritt der Zellatmung: Im Zytoplasma wird ein Molekül Glukose (C₆H₁₂O₆) in zwei Moleküle Pyruvat (C₃H₄O₃) aufgespalten. Dabei entstehen netto 2 ATP und 2 NADH.",
        difficulty = 2,
        funFact = "Die Glykolyse ist eines der ältesten Stoffwechselwege — sie kommt in fast allen lebenden Organismen vor und ist wahrscheinlich früh in der Evolution entstanden."
    ),

    // Question 21 – Plate Tectonics: Subduction zones
    Question(
        categoryId = 2,
        questionText = "Was passiert an einer Subduktionszone?",
        answerA = "Zwei Platten driften auseinander und es entsteht neue Ozeankruste",
        answerB = "Zwei kontinentale Platten kollidieren und falten sich zu Gebirgen",
        answerC = "Eine tektonische Platte taucht unter eine andere in den Erdmantel ab",
        answerD = "Eine Platte gleitet horizontal an der anderen vorbei",
        correctAnswer = 2, // C
        explanation = "An Subduktionszonen schiebt sich eine ozeanische Platte unter eine andere (kontinentale oder ozeanische) Platte in den Erdmantel. Dabei entstehen Tiefseegräben, Erdbeben und Vulkanketten (z.B. Pazifischer Feuerring).",
        difficulty = 2,
        funFact = "Der Marianengraben — mit 11.034 m der tiefste Punkt der Erde — ist das Ergebnis einer Subduktionszone, wo die Pazifische Platte unter die Mariana-Platte abtaucht."
    ),

    // Question 22 – Plate Tectonics: Mid-ocean ridges
    Question(
        categoryId = 2,
        questionText = "Was ist ein mittelozeanischer Rücken?",
        answerA = "Eine tiefe ozeanische Rinne an Subduktionszonen",
        answerB = "Eine unterseeische Gebirgskette, an der neue Ozeankruste durch aufsteigendes Magma gebildet wird",
        answerC = "Eine Verwerfungslinie auf dem Festland, die durch tektonische Kräfte entstanden ist",
        answerD = "Ein flacher Bereich im Ozean mit erhöhter seismischer Aktivität",
        correctAnswer = 1, // B
        explanation = "Mittelozeanische Rücken sind divergente Plattengrenzen unter dem Meeresspiegel. Aufsteigendes Magma erstarrt zu neuer Ozeankruste und drängt die Platten auseinander (Seafloor Spreading). Das mittelatlantische Rückensystem ist 16.000 km lang.",
        difficulty = 2,
        funFact = "Island liegt genau auf dem Mittelatlantischen Rücken — es wächst durch Vulkanismus jedes Jahr um etwa 2,5 cm und man kann die tektonische Grenze zwischen Europa und Nordamerika dort zu Fuß überqueren."
    ),

    // Question 23 – Plate Tectonics: Hot spots
    Question(
        categoryId = 2,
        questionText = "Was ist ein tektonischer 'Hot Spot' und wie entstehen die Hawaiischen Inseln dadurch?",
        answerA = "Ein Hot Spot ist ein seismisch aktiver Bereich an einer Konvergenzgrenze",
        answerB = "Ein Hot Spot ist eine besonders heiße Subduktionszone; Hawaii entstand durch Plattenkollision",
        answerC = "Ein Hot Spot ist eine stationäre Magmaquelle im Mantel; Hawaii entstand, weil die Pazifische Platte darüber hinwegwandert und eine Inselkette bildet",
        answerD = "Ein Hot Spot ist ein erhitzter Bereich der Ozeankruste durch hydrothermale Aktivität",
        correctAnswer = 2, // C
        explanation = "Hot Spots sind stationäre Quellen besonders heißen Mantelgesteins. Da die lithosphärische Platte über den Hot Spot hinwegwandert, entsteht eine Kette von Vulkaninseln. Die ältesten Inseln sind bereits abgetragen; der aktivste Vulkanismus liegt auf Big Island, dem jüngsten Teil der Kette.",
        difficulty = 2,
        funFact = null
    ),

    // Question 24 – Plate Tectonics: Volcanic types
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet einen Schildvulkan von einem Stratovulkan?",
        answerA = "Schildvulkane sind höher und haben steilere Flanken als Stratovulkane",
        answerB = "Schildvulkane entstehen nur an Subduktionszonen, Stratovulkane nur an Hot Spots",
        answerC = "Schildvulkane haben flache, breite Profile durch dünnflüssige Lava; Stratovulkane sind steilkegelförmig durch zähflüssige Lava und Ascheschichten",
        answerD = "Schildvulkane sind immer explosiver als Stratovulkane",
        correctAnswer = 2, // C
        explanation = "Schildvulkane (z.B. Mauna Loa auf Hawaii) entstehen aus basaltischer, dünnflüssiger Lava, die weit fließt und flache Strukturen bildet. Stratovulkane (Composite Volcanoes, z.B. Fuji, Vesuv) haben steile Kegel aus abwechselnden Lava- und Ascheschichten und neigen zu explosiven Ausbrüchen.",
        difficulty = 2,
        funFact = "Gemessen vom Meeresgrund ist Mauna Kea auf Hawaii mit 10.200 m der höchste Berg der Erde — höher als der Mount Everest."
    ),

    // Question 25 – Plate Tectonics: Tectonic boundaries
    Question(
        categoryId = 2,
        questionText = "Welcher Grenztyp liegt vor, wenn zwei tektonische Platten horizontal aneinander vorbeigleiten?",
        answerA = "Konvergente Grenze",
        answerB = "Divergente Grenze",
        answerC = "Konservative (transform) Grenze",
        answerD = "Kollisionsgrenze",
        correctAnswer = 2, // C
        explanation = "An konservativen (Transform-)Grenzen gleiten Platten horizontal aneinander vorbei, ohne dass Kruste gebildet oder vernichtet wird. Der San-Andreas-Graben in Kalifornien ist ein berühmtes Beispiel — hier gleitet die Pazifische Platte an der Nordamerikanischen vorbei.",
        difficulty = 2,
        funFact = "Entlang des San-Andreas-Grabens bewegen sich die Platten mit ca. 5–6 cm pro Jahr. In etwa 15 Millionen Jahren wird Los Angeles nördlich von San Francisco liegen."
    ),

    // Question 26 – Meteorology: Atmospheric layers
    Question(
        categoryId = 2,
        questionText = "In welcher Atmosphärenschicht findet das Wetter statt und wie heißt sie?",
        answerA = "Mesosphäre (50–80 km Höhe)",
        answerB = "Troposphäre (0–12 km Höhe)",
        answerC = "Thermosphäre (80–700 km Höhe)",
        answerD = "Stratosphäre (10–50 km Höhe)",
        correctAnswer = 1, // B
        explanation = "Die Troposphäre ist die unterste Schicht der Erdatmosphäre (0–ca. 12 km). Hier findet das gesamte Wettergeschehen statt: Wolkenbildung, Niederschlag, Stürme. Die Temperatur nimmt mit der Höhe ab (Temperaturgradient ca. -6,5 °C/km).",
        difficulty = 2,
        funFact = null
    ),

    // Question 27 – Meteorology: Jet streams
    Question(
        categoryId = 2,
        questionText = "Was sind Jetstreams und wo befinden sie sich?",
        answerA = "Schnelle Meeresströmungen im Pazifik",
        answerB = "Sturmsysteme in der Nähe des Äquators",
        answerC = "Aufwinde über heißen Wüstengebieten",
        answerD = "Schmale, schnelle Luftströmungen in der oberen Troposphäre/unteren Stratosphäre an den Grenzen von Luftmassen",
        correctAnswer = 3, // D
        explanation = "Jetstreams sind schmale Hochgeschwindigkeits-Luftströmungen in der oberen Troposphäre (9–12 km Höhe) mit Windgeschwindigkeiten von 100–400 km/h. Sie entstehen an Grenzen zwischen Luftmassen unterschiedlicher Temperatur und beeinflussen maßgeblich das Wetter.",
        difficulty = 2,
        funFact = "Jetstreams wurden im 2. Weltkrieg von amerikanischen B-29-Bomberpiloten beim Anflug auf Japan entdeckt, als sie auf unerklärlich starken Gegenwind trafen."
    ),

    // Question 28 – Meteorology: Pressure systems
    Question(
        categoryId = 2,
        questionText = "Warum dreht sich Luft in einem Hochdruckgebiet auf der Nordhalbkugel im Uhrzeigersinn?",
        answerA = "Wegen der Corioliskraft durch die Erdrotation, die auf der Nordhalbkugel Bewegungen nach rechts ablenkt",
        answerB = "Wegen der Erdanziehungskraft, die die Luft nach unten zieht",
        answerC = "Wegen der Gebirge, die Luftströmungen umlenken",
        answerD = "Wegen der Sonneneinstrahlung, die die Luft erwärmt und spiralförmig aufsteigen lässt",
        correctAnswer = 0, // A
        explanation = "Die Corioliskraft, entstanden durch die Erdrotation, lenkt bewegte Luftmassen auf der Nordhalbkugel nach rechts ab. Im Hochdruck strömt Luft von der Mitte nach außen, wird nach rechts abgelenkt → Uhrzeigersinn. Im Tief strömt Luft von außen in die Mitte → Gegenuhrzeigersinn.",
        difficulty = 2,
        funFact = null
    ),

    // Question 29 – Meteorology: Hurricanes
    Question(
        categoryId = 2,
        questionText = "Was ist die Energiequelle eines tropischen Hurrikans (Wirbelsturms)?",
        answerA = "Die Rotationsenergie der Erde (Corioliskraft)",
        answerB = "Die Sonneneinstrahlung, die direkt Winde antreibt",
        answerC = "Die latente Wärme, die beim Aufsteigen und Kondensieren von warmem, feuchtem Meerwasser freigesetzt wird",
        answerD = "Der Temperaturunterschied zwischen Ozean und oberer Atmosphäre",
        correctAnswer = 2, // C
        explanation = "Hurrikane sind thermisch angetriebene Maschinen: Warmes Meerwasser (>26 °C) verdunstet, steigt auf und kondensiert in der Höhe. Dabei wird enorme latente Wärme freigesetzt, die den Sturm antreibt. Deshalb schwächen sich Hurrikane über kühlerem Wasser oder Land ab.",
        difficulty = 2,
        funFact = "Ein ausgewachsener Hurrikan setzt täglich mehr Energie frei als alle Atomkraftwerke der Welt zusammen produzieren."
    ),

    // Question 30 – Meteorology: El Niño
    Question(
        categoryId = 2,
        questionText = "Was ist das Klimaphänomen El Niño?",
        answerA = "Ein saisonaler Monsunregen im westlichen Pazifik",
        answerB = "Eine dauerhafte ozeanische Zirkulation im Südpazifik",
        answerC = "Eine periodische Abkühlung der Meeresoberfläche im östlichen Pazifik, die zu Dürren in Südamerika führt",
        answerD = "Eine unregelmäßige Erwärmung der Meeresoberfläche im östlichen Pazifik, die globale Wetter- und Klimamuster verändert",
        correctAnswer = 3, // D
        explanation = "El Niño bezeichnet die anomale Erwärmung der Meeresoberfläche im zentralen und östlichen Pazifik. Es schwächt die Passatwinde und verändert Niederschlagsmuster weltweit: mehr Regen in Peru/Ecuador, Dürren in Australien und Südostasien.",
        difficulty = 2,
        funFact = "Der Name 'El Niño' (das Christkind) stammt von peruanischen Fischern, die das Phänomen meist um Weihnachten bemerken, wenn der warme Strom die Fischbestände dezimiert."
    ),

    // Question 31 – Oceanography: Ocean zones
    Question(
        categoryId = 2,
        questionText = "Wie wird die ozeanische Zone zwischen 200 und 1000 m Tiefe bezeichnet, in die kaum noch Sonnenlicht eindringt?",
        answerA = "Abyssopelagial",
        answerB = "Bathypelagial",
        answerC = "Mesopelagial",
        answerD = "Epipelagial",
        correctAnswer = 2, // C
        explanation = "Das Mesopelagial (200–1000 m) wird auch als 'Dämmerungszone' bezeichnet, da nur noch schwaches Licht eindringt. Darunter liegt das vollständig lichtlose Bathypelagial (1000–4000 m), gefolgt vom Abyssopelagial (4000–6000 m).",
        difficulty = 2,
        funFact = null
    ),

    // Question 32 – Oceanography: Thermohaline circulation
    Question(
        categoryId = 2,
        questionText = "Was treibt die thermohaline Zirkulation (globale Meeresströmung) an?",
        answerA = "Primär die Windkräfte an der Meeresoberfläche",
        answerB = "Die Gezeitenkräfte von Mond und Sonne",
        answerC = "Die Rotationsenergie der Erde (Corioliskraft)",
        answerD = "Unterschiede in Temperatur und Salzgehalt des Meerwassers, die Dichteunterschiede erzeugen",
        correctAnswer = 3, // D
        explanation = "Die thermohaline Zirkulation wird durch Dichteunterschiede angetrieben: Kaltes, salzreiches Wasser ist dichter und sinkt ab (z.B. im Nordatlantik), während wärmeres, leichteres Wasser nachfließt. Dieser 'globale Förderband'-Strom verteilt Wärme und Nährstoffe weltweit.",
        difficulty = 2,
        funFact = "Der Golfstrom ist Teil der thermohalinen Zirkulation und macht das Klima Europas um bis zu 5–10 °C wärmer als es ohne ihn wäre."
    ),

    // Question 33 – Oceanography: Marine ecosystems
    Question(
        categoryId = 2,
        questionText = "Was sind Korallenriffe und warum sind sie so artenreich?",
        answerA = "Strukturen aus Kalkskeletten von Korallentieren, die einen komplexen dreidimensionalen Lebensraum mit vielen Nischen für Arten bieten",
        answerB = "Algenansammlungen an der Meeresoberfläche",
        answerC = "Vulkanische Gesteinsformationen mit heißen Quellen",
        answerD = "Unterwassersteinformationen ohne biologische Bedeutung",
        correctAnswer = 0, // A
        explanation = "Korallenriffe bestehen aus den Kalkskeletten koloniebildender Korallenpolypen. Die komplexe dreidimensionale Struktur schafft unzählige Lebensräume (Nischen), was Korallenriffe zu den artenreichsten marinen Ökosystemen macht — sie beherbergen ca. 25% aller Meeresarten.",
        difficulty = 2,
        funFact = "Das Great Barrier Reef ist so groß, dass es vom Weltraum aus sichtbar ist — es erstreckt sich über 2.300 km entlang der australischen Küste."
    ),

    // Question 34 – Oceanography: Ocean acidification
    Question(
        categoryId = 2,
        questionText = "Wie führt erhöhtes atmosphärisches CO₂ zur Ozeanversauerung?",
        answerA = "CO₂ löst sich im Meerwasser und bildet Kohlensäure, die Protonen abgibt und den pH-Wert senkt",
        answerB = "CO₂ verdrängt Sauerstoff im Wasser, was zu anaeroben Gärungsprozessen führt",
        answerC = "CO₂ erhöht die Wassertemperatur, was die Säureproduktion durch Mikroorganismen steigert",
        answerD = "CO₂ blockiert das Sonnenlicht und vermindert die Photosynthese der Algen",
        correctAnswer = 0, // A
        explanation = "CO₂ + H₂O → H₂CO₃ (Kohlensäure) → H⁺ + HCO₃⁻. Die freigesetzten H⁺-Ionen senken den pH-Wert. Seit Beginn der Industrialisierung ist der Ozean-pH von 8,25 auf 8,1 gesunken — eine 30%ige Zunahme der Azidität.",
        difficulty = 2,
        funFact = null
    ),

    // Question 35 – Oceanography: Bioluminescence
    Question(
        categoryId = 2,
        questionText = "Wie erzeugen Tiefseeorganismen Biolumineszenz?",
        answerA = "Durch chemische Reaktion von Luziferin mit Luziferase unter ATP-Verbrauch, die Licht erzeugt",
        answerB = "Durch Reflexion des schwachen Sonnenlichts über spezielle Kristalle in ihrer Haut",
        answerC = "Durch elektrische Entladungen ähnlich wie beim Zitteraal",
        answerD = "Durch radioaktiven Zerfall in speziellen Zellorganellen",
        correctAnswer = 0, // A
        explanation = "Biolumineszenz entsteht durch die enzymatische Reaktion: Luziferin + O₂ → Oxyluziferin + Licht (mit Luziferase als Katalysator). Diese 'kalte Lichterzeugung' hat einen Wirkungsgrad von fast 100% und dient zur Kommunikation, Tarnung oder Anlockung von Beute.",
        difficulty = 2,
        funFact = "Etwa 76% aller Tiefseetiere können Licht erzeugen — Biolumineszenz ist die häufigste Form der Kommunikation in der größten Biosphäre der Erde."
    ),

    // Question 36 – Optics: Reflection
    Question(
        categoryId = 2,
        questionText = "Was besagt das Reflexionsgesetz bei Licht?",
        answerA = "Der Einfallswinkel ist immer größer als der Reflexionswinkel",
        answerB = "Das Licht wird immer senkrecht zur einfallenden Oberfläche reflektiert",
        answerC = "Der Reflexionswinkel hängt von der Wellenlänge des Lichts ab",
        answerD = "Der Einfallswinkel ist gleich dem Reflexionswinkel, beide gemessen zur Einfallslinie (Normale)",
        correctAnswer = 3, // D
        explanation = "Das Reflexionsgesetz besagt: Einfallswinkel (α) = Reflexionswinkel (α'), beide gemessen zur Flächennormalen. Zudem liegen Einfallsstrahl, Normale und reflektierter Strahl in einer Ebene.",
        difficulty = 2,
        funFact = null
    ),

    // Question 37 – Optics: Refraction
    Question(
        categoryId = 2,
        questionText = "Warum bricht sich Licht, wenn es von Luft in Glas übergeht?",
        answerA = "Das Licht wird durch das Magnetfeld des Glases abgelenkt",
        answerB = "Der Brechungswinkel entsteht durch Interferenz des einlaufenden und reflektierten Strahls",
        answerC = "Glasmoleküle stoßen die Lichtphotonen seitlich ab",
        answerD = "Die Lichtgeschwindigkeit ändert sich beim Übergang in ein dichteres Medium, woraus eine Richtungsänderung resultiert",
        correctAnswer = 3, // D
        explanation = "Licht bewegt sich in verschiedenen Medien unterschiedlich schnell. Beim Übergang von Luft in Glas verringert sich die Lichtgeschwindigkeit (von c auf c/n). Diese Geschwindigkeitsänderung bewirkt gemäß dem Snelliusschen Gesetz eine Richtungsänderung — Brechung.",
        difficulty = 2,
        funFact = "Der Brechungsindex von Diamant (n ≈ 2,42) ist besonders hoch, was sein charakteristisches Funkeln erzeugt."
    ),

    // Question 38 – Optics: Lenses
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einer Sammellinse und einer Zerstreuungslinse?",
        answerA = "Zerstreuungslinsen erzeugen reelle, Sammellinsen nur virtuelle Bilder",
        answerB = "Sammellinsen reflektieren Licht; Zerstreuungslinsen brechen es",
        answerC = "Sammellinsen sind immer dicker als Zerstreuungslinsen",
        answerD = "Sammellinsen bündeln parallele Strahlen in einem Brennpunkt; Zerstreuungslinsen spreizen parallele Strahlen auseinander",
        correctAnswer = 3, // D
        explanation = "Konvexe (Sammel-)Linsen sind in der Mitte dicker und bündeln parallele Lichtstrahlen im Brennpunkt — sie erzeugen reelle oder virtuelle Bilder je nach Gegenstandsweite. Konkave (Zerstreuungs-)Linsen sind in der Mitte dünner und divergieren parallele Strahlen.",
        difficulty = 2,
        funFact = "Das menschliche Auge nutzt eine flexible Sammellinse — die Akkommodation (Anpassung der Linsenkrümmung) ermöglicht das Scharfsehen in verschiedenen Entfernungen."
    ),

    // Question 39 – Optics: Prisms
    Question(
        categoryId = 2,
        questionText = "Warum zerlegt ein Glasprisma weißes Licht in seine Spektralfarben?",
        answerA = "Das Prisma filtert bestimmte Wellenlängen durch Absorption",
        answerB = "Das Prisma erzeugt durch Interferenz unterschiedliche Farben",
        answerC = "Verschiedene Wellenlängen haben unterschiedliche Brechungsindizes im Glas, sodass sie unterschiedlich stark gebrochen werden",
        answerD = "Die verschiedenen Farben des weißen Lichts reflektieren an verschiedenen Glasoberflächen",
        correctAnswer = 2, // C
        explanation = "Die Dispersion im Glas bewirkt, dass Licht unterschiedlicher Wellenlänge (Farbe) verschieden stark gebrochen wird. Violettes Licht (kurze Wellenlänge) wird am stärksten gebrochen, rotes Licht (lange Wellenlänge) am schwächsten — so entsteht das Spektrum.",
        difficulty = 2,
        funFact = "Newton zeigte 1666 mit einem Prisma, dass weißes Sonnenlicht aus allen Spektralfarben zusammengesetzt ist. Ein zweites Prisma fasste die Farben wieder zu Weiß zusammen."
    ),

    // Question 40 – Optics: Light interference
    Question(
        categoryId = 2,
        questionText = "Was ist Lichtinterferenz und wann tritt destruktive Interferenz auf?",
        answerA = "Interferenz ist die Reflexion an Grenzflächen; destruktiv wenn der Einfallswinkel 45° beträgt",
        answerB = "Interferenz ist die Polarisation von Licht; destruktiv bei gekreuzten Polarisationsfiltern",
        answerC = "Interferenz ist die Überlagerung von Lichtwellen; destruktiv wenn zwei Wellen einen Gangunterschied von einer halben Wellenlänge haben und sich auslöschen",
        answerD = "Interferenz ist die Beugung von Licht an engen Spalten; destruktiv wenn die Spaltbreite kleiner als die Wellenlänge ist",
        correctAnswer = 2, // C
        explanation = "Interferenz entsteht durch Überlagerung kohärenter Lichtwellen. Konstruktive Interferenz (Verstärkung) tritt auf bei Gangunterschied = n·λ; destruktive Interferenz (Auslöschung) bei Gangunterschied = (n+½)·λ. Dies erklärt die Farben von Seifenblasen und Ölfilmen.",
        difficulty = 2,
        funFact = null
    ),

    // Question 41 – Periodic Table: Groups and periods
    Question(
        categoryId = 2,
        questionText = "Was bedeuten 'Gruppen' und 'Perioden' im Periodensystem der Elemente?",
        answerA = "Gruppen sind senkrechte Spalten mit ähnlichen chemischen Eigenschaften; Perioden sind waagerechte Reihen, in denen die Elektronenschalen aufgefüllt werden",
        answerB = "Gruppen und Perioden sind unterschiedliche Namen für dasselbe Konzept",
        answerC = "Gruppen bezeichnen den Aggregatzustand; Perioden die Radioaktivität der Elemente",
        answerD = "Gruppen sind waagerechte Reihen mit gleicher Elektronenanzahl; Perioden sind senkrechte Spalten mit ähnlichen Eigenschaften",
        correctAnswer = 0, // A
        explanation = "Im Periodensystem sind die Elemente nach steigender Ordnungszahl geordnet. Perioden (Zeilen) zeigen, welche Elektronenschale gerade aufgefüllt wird. Gruppen (Spalten) fassen Elemente mit gleicher Valenzorbitalkonfiguration und ähnlichen Eigenschaften zusammen.",
        difficulty = 2,
        funFact = "Mendelejew sagte 1871 die Existenz noch unentdeckter Elemente voraus, indem er Lücken im Periodensystem ließ — und beschrieb ihre Eigenschaften erstaunlich genau."
    ),

    // Question 42 – Periodic Table: Electron configuration
    Question(
        categoryId = 2,
        questionText = "Wie lautet die Elektronenkonfiguration von Sauerstoff (Ordnungszahl 8)?",
        answerA = "1s⁸",
        answerB = "1s² 2s⁴ 2p²",
        answerC = "1s² 2s² 2p⁴",
        answerD = "1s² 2s² 2p⁶",
        correctAnswer = 2, // C
        explanation = "Sauerstoff hat die Ordnungszahl 8, also 8 Elektronen. Diese verteilen sich auf: 1s² (2 Elektronen), 2s² (2 Elektronen), 2p⁴ (4 Elektronen). Die 2p-Unterschale kann maximal 6 Elektronen aufnehmen; Sauerstoff fehlen noch 2 (daher 2 Bindungen in H₂O).",
        difficulty = 2,
        funFact = null
    ),

    // Question 43 – Periodic Table: Noble gases
    Question(
        categoryId = 2,
        questionText = "Warum sind Edelgase chemisch so reaktionsträge?",
        answerA = "Weil ihre Atomkerne besonders stabil sind und keine Elektronen abgeben",
        answerB = "Weil ihre Außenschale vollständig mit Elektronen besetzt ist und keine Tendenz zur Abgabe oder Aufnahme von Elektronen besteht",
        answerC = "Weil sie sehr schwer sind und kaum Verbindungen eingehen können",
        answerD = "Weil sie ausschließlich als Gase vorkommen und Gase grundsätzlich nicht reagieren",
        correctAnswer = 1, // B
        explanation = "Edelgase (Helium, Neon, Argon, Krypton, Xenon, Radon) haben vollständig gefüllte Außenschalen (Oktettregel erfüllt), was eine besonders stabile Elektronenkonfiguration ergibt. Die Energie für eine Reaktion überwiegt den möglichen Energiegewinn.",
        difficulty = 2,
        funFact = "Xenon bildet dennoch einige Verbindungen — 1962 synthetisierte Neil Bartlett erstmals XePtF₆ und bewies damit, dass Edelgasverbindungen möglich sind."
    ),

    // Question 44 – Periodic Table: Transition metals
    Question(
        categoryId = 2,
        questionText = "Was ist eine charakteristische Eigenschaft der Übergangsmetalle im Periodensystem?",
        answerA = "Sie befinden sich ausschließlich in der ersten Periode des Periodensystems",
        answerB = "Sie haben unvollständig gefüllte d-Orbitale und können mehrere Oxidationsstufen annehmen",
        answerC = "Sie reagieren nicht mit Wasser oder Säuren",
        answerD = "Sie sind alle radioaktiv und instabil",
        correctAnswer = 1, // B
        explanation = "Übergangsmetalle (Gruppen 3–12) haben unvollständige d-Unterschalen. Dies erklärt ihre charakteristischen Eigenschaften: variable Oxidationsstufen (z.B. Eisen: Fe²⁺ und Fe³⁺), Bildung farbiger Verbindungen, katalytische Aktivität und Bildung von Komplexverbindungen.",
        difficulty = 2,
        funFact = "Die meisten Enzyme, die lebensnotwendige Reaktionen katalysieren, enthalten Übergangsmetalle: Hämoglobin enthält Eisen, Cytochrom c Oxidase Kupfer, Nitrogenase Molybdän."
    ),

    // Question 45 – Periodic Table: Periodic trends
    Question(
        categoryId = 2,
        questionText = "Wie verändert sich die Elektronegativität im Periodensystem von links nach rechts in einer Periode?",
        answerA = "Sie nimmt zu, weil die Kernladung steigt und die Elektronen stärker angezogen werden",
        answerB = "Sie bleibt konstant innerhalb einer Periode",
        answerC = "Sie nimmt zunächst zu und dann wieder ab",
        answerD = "Sie nimmt ab, weil die Atome größer werden",
        correctAnswer = 0, // A
        explanation = "Innerhalb einer Periode nimmt die Kernladungszahl zu, während die abschirmenden Elektronenschalen gleich bleiben. Die steigende effektive Kernladung zieht die Elektronen stärker an → höhere Elektronegativität. Fluor (F) hat mit 3,98 die höchste Elektronegativität aller Elemente.",
        difficulty = 2,
        funFact = null
    ),

    // Question 46 – Evolution: Natural selection
    Question(
        categoryId = 2,
        questionText = "Was sind die vier Voraussetzungen, die Darwin für natürliche Selektion identifizierte?",
        answerA = "Variation, Vererblichkeit, Überproduktion und unterschiedliche Überlebensrate (Fitness)",
        answerB = "Mutation, Rekombination, Genfluss und genetische Drift",
        answerC = "Wettbewerb, Kooperation, Migration und Mutation",
        answerD = "Adaptation, Isolation, Selektion und Aussterben",
        correctAnswer = 0, // A
        explanation = "Natürliche Selektion benötigt: (1) Variation in der Population, (2) diese Variation muss erblich sein, (3) mehr Nachkommen werden produziert als überleben können (Überproduktion) und (4) bestimmte Varianten haben eine höhere Überlebens- und Reproduktionsfähigkeit (Fitness).",
        difficulty = 2,
        funFact = "Darwin beobachtete auf den Galápagos-Inseln 13 Finkenarten mit unterschiedlichen Schnabelformen — alle stammten von einer gemeinsamen Vorfahrenart ab (adaptive Radiation)."
    ),

    // Question 47 – Evolution: Speciation
    Question(
        categoryId = 2,
        questionText = "Was ist allopatrische Artbildung?",
        answerA = "Artbildung durch geografische Trennung einer Population, nach der sich die getrennten Gruppen unabhängig entwickeln",
        answerB = "Artbildung durch Polyploidisierung (Vervielfachung des Chromosomensatzes)",
        answerC = "Artbildung durch sexuelle Selektion innerhalb einer Population",
        answerD = "Artbildung durch horizontalen Gentransfer zwischen verschiedenen Arten",
        correctAnswer = 0, // A
        explanation = "Bei der allopatrischen Artbildung wird eine Population durch geografische Barrieren (Gebirge, Ozeane, Gletscher) geteilt. Die getrennten Gruppen entwickeln sich unabhängig und werden genetisch so unterschiedlich, dass sie sich nach Zusammentreffen nicht mehr erfolgreich fortpflanzen können.",
        difficulty = 2,
        funFact = null
    ),

    // Question 48 – Evolution: Adaptive radiation
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter adaptiver Radiation in der Evolutionsbiologie?",
        answerA = "Die Entstehung von Radioaktivität durch evolutionäre Prozesse",
        answerB = "Die schnelle Diversifizierung einer Stammart in viele verschiedene Arten, die unterschiedliche ökologische Nischen besetzen",
        answerC = "Die Ausbreitung von Populationen durch Licht als Orientierungssignal",
        answerD = "Die Anpassung einer Art an Strahlungsbelastung in ihrer Umwelt",
        correctAnswer = 1, // B
        explanation = "Adaptive Radiation beschreibt die schnelle Entstehung vieler Arten aus einer gemeinsamen Vorfahrenart, wenn neue ökologische Nischen verfügbar werden. Klassische Beispiele sind die Darwin-Finken auf den Galápagos-Inseln und die Beutelsäuger in Australien.",
        difficulty = 2,
        funFact = "Nach dem Massenaussterben der Dinosaurier vor 66 Millionen Jahren entwickelten sich Säugetiere in einer explosionsartigen adaptiven Radiation und besetzten alle freigewordenen Nischen."
    ),

    // Question 49 – Evolution: Convergent evolution
    Question(
        categoryId = 2,
        questionText = "Was ist konvergente Evolution und welches Beispiel illustriert sie?",
        answerA = "Die Übertragung von Genen zwischen verschiedenen Arten durch Viren",
        answerB = "Die Evolution zweier Arten aus einem gemeinsamen Vorfahren; Beispiel: Mensch und Schimpanse",
        answerC = "Die unabhängige Entwicklung ähnlicher Merkmale bei nicht verwandten Arten durch ähnliche Selektionsdrücke; Beispiel: Delfin-Flosse und Haifisch-Flosse",
        answerD = "Die Entstehung neuer Arten durch Hybridisierung zweier bestehender Arten",
        correctAnswer = 2, // C
        explanation = "Konvergente Evolution entsteht wenn nicht verwandte Arten ähnliche Umweltanforderungen haben und unabhängig voneinander ähnliche anatomische Lösungen entwickeln. Delfine (Säugetiere) und Haie (Fische) entwickelten unabhängig eine torpedoförmige Körperform als optimale Anpassung ans Schwimmen.",
        difficulty = 2,
        funFact = null
    ),

    // Question 50 – Evolution: Fossil record
    Question(
        categoryId = 2,
        questionText = "Was sind Übergangsformen (Transitionalfossilien) und welches ist ein bekanntes Beispiel?",
        answerA = "Fossilien von ausgestorbenen Arten, die keine lebenden Verwandten haben",
        answerB = "Fossilien, die anatomische Merkmale zweier evolutionär benachbarter Gruppen vereinen und Übergänge belegen; Beispiel: Archaeopteryx (Reptil-Vogel-Übergang)",
        answerC = "Fossilien, die an zwei verschiedenen Orten gleichzeitig gefunden wurden; Beispiel: Mammut",
        answerD = "Besonders vollständig erhaltene Fossilien ohne fehlende Knochen",
        correctAnswer = 1, // B
        explanation = "Transitionalfossilien zeigen Mosaikmuster von Merkmalen zweier verwandter Gruppen und belegen so evolutionäre Übergänge. Archaeopteryx (ca. 150 Mio. Jahre alt) hatte Vogelfedern und Reptilienmerkmale (Zähne, Klauen, knöcherner Schwanz) und belegt den Übergang von Theropodadinosauriern zu Vögeln.",
        difficulty = 2,
        funFact = "Archaeopteryx wurde 1861 in Bayern gefunden — nur zwei Jahre nach Darwins 'On the Origin of Species'. Das Timing konnte kaum besser sein, um Darwins Theorie zu stützen."
    )
)
