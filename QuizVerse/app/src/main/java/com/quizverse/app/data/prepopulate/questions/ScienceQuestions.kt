package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestions(): List<Question> = listOf(

    // ── EASY (difficulty = 1) ─────────────────────────────────────────────────

    Question(
        categoryId = 2,
        questionText = "Welches Gas macht den größten Anteil der Erdatmosphäre aus?",
        answerA = "Sauerstoff",
        answerB = "Kohlendioxid",
        answerC = "Stickstoff",
        answerD = "Wasserstoff",
        correctAnswer = 2,
        explanation = "Stickstoff (N₂) macht etwa 78 % der Erdatmosphäre aus, Sauerstoff etwa 21 %.",
        difficulty = 1,
        funFact = "Obwohl Pflanzen Kohlendioxid aufnehmen, besteht die Luft nur zu 0,04 % daraus."
    ),

    Question(
        categoryId = 2,
        questionText = "Wie viele Knochen hat ein erwachsener Mensch?",
        answerA = "206",
        answerB = "215",
        answerC = "198",
        answerD = "230",
        correctAnswer = 0,
        explanation = "Ein erwachsener Mensch hat genau 206 Knochen. Neugeborene haben etwa 270–300, da sich viele Knochen im Laufe des Lebens zusammenschließen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist das chemische Symbol für Gold?",
        answerA = "Go",
        answerB = "Gd",
        answerC = "Ag",
        answerD = "Au",
        correctAnswer = 3,
        explanation = "Das Symbol Au kommt vom lateinischen Wort 'Aurum', was Gold bedeutet.",
        difficulty = 1,
        funFact = "Gold ist so dehnbar, dass man aus einem Gramm einen Draht von 2,5 km Länge ziehen kann."
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Organ produziert beim Menschen Insulin?",
        answerA = "Leber",
        answerB = "Niere",
        answerC = "Bauchspeicheldrüse",
        answerD = "Milz",
        correctAnswer = 2,
        explanation = "Die Bauchspeicheldrüse (Pankreas) produziert das Hormon Insulin, das den Blutzuckerspiegel reguliert.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Einheit der elektrischen Spannung?",
        answerA = "Ampere",
        answerB = "Watt",
        answerC = "Ohm",
        answerD = "Volt",
        correctAnswer = 3,
        explanation = "Die elektrische Spannung wird in Volt (V) gemessen, benannt nach Alessandro Volta.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welcher Planet ist der Sonne am nächsten?",
        answerA = "Venus",
        answerB = "Merkur",
        answerC = "Mars",
        answerD = "Erde",
        correctAnswer = 1,
        explanation = "Merkur ist mit einer durchschnittlichen Entfernung von ca. 58 Millionen km der sonnennächste Planet.",
        difficulty = 1,
        funFact = "Trotz seiner Sonnennähe ist Merkur nicht der heißeste Planet – das ist Venus, wegen ihres dichten Treibhauseffekts."
    ),

    Question(
        categoryId = 2,
        questionText = "Aus welchem Element besteht Wasser hauptsächlich?",
        answerA = "Nur Sauerstoff",
        answerB = "Nur Wasserstoff",
        answerC = "Wasserstoff und Sauerstoff",
        answerD = "Wasserstoff und Stickstoff",
        correctAnswer = 2,
        explanation = "Wasser (H₂O) besteht aus zwei Wasserstoffatomen und einem Sauerstoffatom.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist Photosynthese?",
        answerA = "Die Atmung von Tieren",
        answerB = "Die Umwandlung von Licht in chemische Energie durch Pflanzen",
        answerC = "Die Verdauung von Nährstoffen",
        answerD = "Der Transport von Wasser in Pflanzen",
        correctAnswer = 1,
        explanation = "Bei der Photosynthese wandeln Pflanzen Lichtenergie, Wasser und CO₂ in Glukose und Sauerstoff um.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches ist das leichteste Element im Periodensystem?",
        answerA = "Helium",
        answerB = "Lithium",
        answerC = "Wasserstoff",
        answerD = "Bor",
        correctAnswer = 2,
        explanation = "Wasserstoff (H) ist das erste und leichteste Element mit der Ordnungszahl 1.",
        difficulty = 1,
        funFact = "Wasserstoff ist das häufigste Element im Universum und macht etwa 75 % der normalen Materie aus."
    ),

    Question(
        categoryId = 2,
        questionText = "Wie schnell bewegt sich Licht im Vakuum?",
        answerA = "Etwa 300.000 km/s",
        answerB = "Etwa 150.000 km/s",
        answerC = "Etwa 500.000 km/s",
        answerD = "Etwa 100.000 km/s",
        correctAnswer = 0,
        explanation = "Licht bewegt sich im Vakuum mit genau 299.792.458 m/s, also rund 300.000 km/s.",
        difficulty = 1,
        funFact = "Mit Lichtgeschwindigkeit würde man die Erde in etwa 0,13 Sekunden einmal umrunden."
    ),

    // ── MEDIUM (difficulty = 2) ───────────────────────────────────────────────

    Question(
        categoryId = 2,
        questionText = "Welche Blutgruppe wird als 'universeller Spender' bezeichnet?",
        answerA = "AB positiv",
        answerB = "A negativ",
        answerC = "0 negativ",
        answerD = "B positiv",
        correctAnswer = 2,
        explanation = "0 negativ (Rh-) enthält keine A-, B- oder Rh-Antigene und kann daher an alle Blutgruppen gespendet werden.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das zweite Newtonsche Gesetz?",
        answerA = "Jede Wirkung hat eine gleiche und entgegengesetzte Reaktion",
        answerB = "Kraft ist gleich Masse mal Beschleunigung",
        answerC = "Ein Körper in Ruhe bleibt in Ruhe",
        answerD = "Energie bleibt immer erhalten",
        correctAnswer = 1,
        explanation = "Das zweite Newtonsche Gesetz besagt: F = m × a (Kraft = Masse × Beschleunigung).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Wie viele Chromosomen hat eine normale menschliche Körperzelle?",
        answerA = "23",
        answerB = "46",
        answerC = "48",
        answerD = "44",
        correctAnswer = 1,
        explanation = "Menschliche Körperzellen haben 46 Chromosomen (23 Paare). Keimzellen (Spermien, Eizellen) haben nur 23.",
        difficulty = 2,
        funFact = "Menschen teilen etwa 98,7 % ihrer DNA mit Schimpansen."
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Mineral ist das härteste in der Mohs-Skala?",
        answerA = "Rubin",
        answerB = "Korund",
        answerC = "Diamant",
        answerD = "Quarz",
        correctAnswer = 2,
        explanation = "Diamant hat den Härtegrad 10 auf der Mohs-Skala und ist das härteste natürlich vorkommende Mineral.",
        difficulty = 2,
        funFact = "Diamant ist eine Form von reinem Kohlenstoff – derselbe Stoff, aus dem auch Bleistiftmine (Graphit) besteht."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Säure befindet sich im Magensaft des Menschen?",
        answerA = "Schwefelsäure",
        answerB = "Essigsäure",
        answerC = "Salzsäure",
        answerD = "Salpetersäure",
        correctAnswer = 2,
        explanation = "Der Magensaft enthält Salzsäure (HCl) mit einem pH-Wert zwischen 1,5 und 3,5.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einer Arterie und einer Vene?",
        answerA = "Arterien führen Blut zum Herz, Venen vom Herz weg",
        answerB = "Arterien führen Blut vom Herz weg, Venen zum Herz hin",
        answerC = "Arterien tragen nur sauerstoffreiches, Venen nur sauerstoffarmes Blut",
        answerD = "Es gibt keinen Unterschied",
        correctAnswer = 1,
        explanation = "Arterien führen Blut vom Herzen weg (in der Regel sauerstoffreich), Venen führen es zum Herzen zurück.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Phänomen erklärt, warum der Himmel blau ist?",
        answerA = "Reflexion am Ozean",
        answerB = "Rayleigh-Streuung",
        answerC = "Brechung des Sonnenlichts",
        answerD = "Ozonabsorption",
        correctAnswer = 1,
        explanation = "Die Rayleigh-Streuung beschreibt, wie kurzwelliges (blaues) Licht stärker an Luftmolekülen gestreut wird als langwelliges (rotes).",
        difficulty = 2,
        funFact = "Bei Sonnenuntergang erscheint der Himmel rot, weil das Licht einen längeren Weg durch die Atmosphäre zurücklegt."
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Element hat die Ordnungszahl 6?",
        answerA = "Stickstoff",
        answerB = "Sauerstoff",
        answerC = "Kohlenstoff",
        answerD = "Bor",
        correctAnswer = 2,
        explanation = "Kohlenstoff (C) hat die Ordnungszahl 6 und ist die Grundlage aller organischen Verbindungen.",
        difficulty = 2,
        funFact = "Kohlenstoff ist nach Sauerstoff, Silizium und Aluminium das vierthäufigste Element in der Erdkruste."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Halbwertszeit eines radioaktiven Elements?",
        answerA = "Die Zeit, bis das Element vollständig zerfallen ist",
        answerB = "Die Zeit, in der die Hälfte der Atome zerfallen ist",
        answerC = "Die Zeit, bis die Strahlung halbiert wird",
        answerD = "Die Zeit, bis das Element stabil wird",
        correctAnswer = 1,
        explanation = "Die Halbwertszeit ist der Zeitraum, nach dem die Hälfte der ursprünglichen Menge eines radioaktiven Isotops zerfallen ist.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welcher Prozess findet in den Mitochondrien statt?",
        answerA = "Proteinbiosynthese",
        answerB = "Photosynthese",
        answerC = "Zellatmung (ATP-Produktion)",
        answerD = "DNA-Replikation",
        correctAnswer = 2,
        explanation = "In den Mitochondrien findet die aerobe Zellatmung statt, bei der ATP (Adenosintriphosphat) als Energieträger produziert wird.",
        difficulty = 2,
        funFact = "Mitochondrien haben eine eigene DNA und waren ursprünglich eigenständige Bakterien."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Kraft hält Elektronen in ihrer Bahn um den Atomkern?",
        answerA = "Gravitationskraft",
        answerB = "Elektromagnetische Kraft",
        answerC = "Starke Kernkraft",
        answerD = "Schwache Kernkraft",
        correctAnswer = 1,
        explanation = "Die elektromagnetische Kraft (Coulomb-Kraft) zwischen dem positiv geladenen Kern und den negativ geladenen Elektronen hält diese in ihrer Bahn.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Funktion des Chlorophylls in Pflanzen?",
        answerA = "Wasseraufnahme",
        answerB = "Absorption von Lichtenergie für die Photosynthese",
        answerC = "Nährstofftransport",
        answerD = "Schutz vor Schädlingen",
        correctAnswer = 1,
        explanation = "Chlorophyll ist das grüne Pigment in Pflanzen, das Lichtenergie absorbiert und für die Photosynthese nutzt.",
        difficulty = 2,
        funFact = "Chlorophyll absorbiert hauptsächlich rotes und blaues Licht, aber kaum grünes – deshalb erscheinen Pflanzen grün."
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Gesetz beschreibt die Beziehung zwischen Druck und Volumen eines Gases bei konstanter Temperatur?",
        answerA = "Gesetz von Avogadro",
        answerB = "Gesetz von Gay-Lussac",
        answerC = "Gesetz von Boyle-Mariotte",
        answerD = "Gesetz von Charles",
        correctAnswer = 2,
        explanation = "Das Gesetz von Boyle-Mariotte besagt: Bei konstanter Temperatur ist das Produkt aus Druck und Volumen eines idealen Gases konstant (p × V = const).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches ist das häufigste Element in der Erdkruste?",
        answerA = "Silizium",
        answerB = "Eisen",
        answerC = "Aluminium",
        answerD = "Sauerstoff",
        correctAnswer = 3,
        explanation = "Sauerstoff macht etwa 46 % der Erdkruste aus (meist in Verbindung mit anderen Elementen wie SiO₂).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem 'Treibhauseffekt'?",
        answerA = "Die Erwärmung von Gewächshäusern durch Sonnenlicht",
        answerB = "Die Erwärmung der Erdoberfläche durch Infrarotstrahlung absorbierende Gase",
        answerC = "Den Abbau der Ozonschicht durch UV-Strahlung",
        answerD = "Den Temperaturunterschied zwischen Tag und Nacht",
        correctAnswer = 1,
        explanation = "Treibhausgase wie CO₂ und Methan absorbieren die von der Erde abgestrahlte Infrarotstrahlung und geben sie zurück, was die Atmosphäre erwärmt.",
        difficulty = 2,
        funFact = "Ohne natürlichen Treibhauseffekt wäre die durchschnittliche Temperatur der Erde bei ca. -18 °C statt +15 °C."
    ),

    // ── HARD (difficulty = 3) ─────────────────────────────────────────────────

    Question(
        categoryId = 2,
        questionText = "Welches Enzym ist für die DNA-Replikation verantwortlich?",
        answerA = "RNA-Polymerase",
        answerB = "DNA-Polymerase",
        answerC = "Ligase",
        answerD = "Restriktionsendonuklease",
        correctAnswer = 1,
        explanation = "Die DNA-Polymerase synthetisiert neue DNA-Stränge, indem sie die komplementären Nucleotide an den Matrizenstrang anfügt.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Pauli-Ausschlussprinzip?",
        answerA = "Keine zwei Elektronen können denselben Quantenzustand einnehmen",
        answerB = "Elektronen bewegen sich auf festen Kreisbahnen um den Kern",
        answerC = "Licht verhält sich gleichzeitig als Welle und Teilchen",
        answerD = "Die Energie eines Photons ist proportional seiner Frequenz",
        correctAnswer = 0,
        explanation = "Das Pauli-Ausschlussprinzip besagt, dass keine zwei Fermionen (z. B. Elektronen) gleichzeitig denselben vollständigen Quantenzustand einnehmen können.",
        difficulty = 3,
        funFact = "Dieses Prinzip erklärt die Stabilität der Materie und die Struktur des Periodensystems."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Strahlung hat die höchste Energie?",
        answerA = "Gammastrahlung",
        answerB = "Röntgenstrahlung",
        answerC = "Ultraviolettstrahlung",
        answerD = "Infrarotstrahlung",
        correctAnswer = 0,
        explanation = "Gammastrahlung hat die kürzeste Wellenlänge und damit die höchste Energie im elektromagnetischen Spektrum.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Wie lautet die chemische Formel für Schwefelsäure?",
        answerA = "H₂SO₃",
        answerB = "HNO₃",
        answerC = "H₃PO₄",
        answerD = "H₂SO₄",
        correctAnswer = 3,
        explanation = "Schwefelsäure hat die Formel H₂SO₄. Sie ist eine der wichtigsten Industriechemikalien weltweit.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein Neutrino?",
        answerA = "Ein neutrales Baryon",
        answerB = "Ein elektrisch neutrales Lepton mit sehr kleiner Masse",
        answerC = "Ein Quark mit neutraler Ladung",
        answerD = "Ein radioaktives Zerfallsprodukt von Neutronen",
        correctAnswer = 1,
        explanation = "Neutrinos sind elektrisch neutrale Elementarteilchen aus der Leptonenfamilie mit extrem kleiner, aber nicht null Masse.",
        difficulty = 3,
        funFact = "Jede Sekunde passieren Billionen von Neutrinos von der Sonne ungehindert durch jeden Quadratzentimeter deines Körpers."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Gesteinsart entsteht durch Erstarrung von Magma?",
        answerA = "Sedimentgestein",
        answerB = "Metamorphes Gestein",
        answerC = "Magmatisches Gestein",
        answerD = "Organisches Gestein",
        correctAnswer = 2,
        explanation = "Magmatisches Gestein (Igneit) entsteht durch die Erstarrung von Magma, entweder an der Oberfläche (Vulkanit) oder in der Tiefe (Plutonit).",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist CRISPR-Cas9?",
        answerA = "Ein Antibiotikum gegen Viren",
        answerB = "Ein Werkzeug zur gezielten Bearbeitung von DNA-Sequenzen",
        answerC = "Eine Methode zur Herstellung von Stammzellen",
        answerD = "Ein Protein zur Immunabwehr",
        correctAnswer = 1,
        explanation = "CRISPR-Cas9 ist ein molekulares 'Genscherensystem', das präzise Veränderungen im Erbgut von Lebewesen ermöglicht.",
        difficulty = 3,
        funFact = "Das System wurde ursprünglich als Teil des Immunsystems von Bakterien gegen Viren entdeckt."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt die Heisenbergsche Unschärferelation?",
        answerA = "Die Geschwindigkeit eines Teilchens ist immer ungenau messbar",
        answerB = "Ort und Impuls eines Teilchens können nicht gleichzeitig beliebig genau bestimmt werden",
        answerC = "Die Energie eines Quantensystems ist unbestimmt",
        answerD = "Teilchen können sich nicht schneller als Licht bewegen",
        correctAnswer = 1,
        explanation = "Die Heisenbergsche Unschärferelation besagt: Δx · Δp ≥ ℏ/2 – je genauer der Ort, desto ungenauer der Impuls und umgekehrt.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Protein ist hauptsächlich für den Sauerstofftransport im Blut verantwortlich?",
        answerA = "Albumin",
        answerB = "Fibrinogen",
        answerC = "Hämoglobin",
        answerD = "Globulin",
        correctAnswer = 2,
        explanation = "Hämoglobin ist ein eisenhaltiges Protein in roten Blutkörperchen, das Sauerstoff reversibel binden und transportieren kann.",
        difficulty = 3,
        funFact = "Ein einzelnes Hämoglobin-Molekül kann vier Sauerstoffmoleküle gleichzeitig tragen."
    ),

    Question(
        categoryId = 2,
        questionText = "Wie wird der pH-Wert einer Lösung definiert?",
        answerA = "Als die Konzentration von OH⁻-Ionen",
        answerB = "Als der negative dekadische Logarithmus der H₃O⁺-Konzentration",
        answerC = "Als die Anzahl der Protonen pro Liter",
        answerD = "Als die elektrische Leitfähigkeit der Lösung",
        correctAnswer = 1,
        explanation = "pH = -log₁₀[H₃O⁺]. Ein pH unter 7 ist sauer, über 7 basisch, genau 7 ist neutral.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Schwarzschild-Radius?",
        answerA = "Der Radius, ab dem ein Stern zur Supernova wird",
        answerB = "Der kritische Radius, unterhalb dessen ein Objekt zu einem Schwarzen Loch kollabiert",
        answerC = "Die maximale Ausdehnung einer Neutronensternhülle",
        answerD = "Der Abstand zwischen einem Schwarzen Loch und seinem nächsten Stern",
        correctAnswer = 1,
        explanation = "Der Schwarzschild-Radius ist der Radius, unterhalb dessen die Fluchtgeschwindigkeit die Lichtgeschwindigkeit übersteigt – das Objekt wird zum Schwarzen Loch.",
        difficulty = 3,
        funFact = "Für die Erde beträgt der Schwarzschild-Radius nur etwa 9 mm."
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Phänomen erklärt die kontinentale Verschiebung?",
        answerA = "Erdmagnetismus",
        answerB = "Plattentektonik",
        answerC = "Isostasie",
        answerD = "Konvektion im äußeren Erdkern",
        correctAnswer = 1,
        explanation = "Die Plattentektonik beschreibt die Bewegung der Lithosphärenplatten, angetrieben durch Konvektionsströmungen im Erdmantel.",
        difficulty = 3,
        funFact = "Europa und Amerika entfernen sich pro Jahr um etwa 2–3 cm – in etwa so schnell, wie Fingernägel wachsen."
    ),

    // ── NEW EASY (difficulty = 1) — 15 questions ──────────────────────────────

    Question(
        categoryId = 2,
        questionText = "Welches Organ ist für die Blutfilterung im menschlichen Körper zuständig?",
        answerA = "Lunge",
        answerB = "Milz",
        answerC = "Niere",
        answerD = "Leber",
        correctAnswer = 2,
        explanation = "Die Nieren filtern täglich etwa 180 Liter Blut und scheiden Abfallstoffe über den Urin aus.",
        difficulty = 1,
        funFact = "Jede Niere enthält etwa eine Million winzige Filtereinheiten, die sogenannten Nephronen."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die SI-Einheit der Masse?",
        answerA = "Gramm",
        answerB = "Kilogramm",
        answerC = "Pfund",
        answerD = "Newton",
        correctAnswer = 1,
        explanation = "Das Kilogramm (kg) ist die SI-Basiseinheit der Masse. Seit 2019 wird es über die Planck-Konstante definiert.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Wie viele Planeten hat unser Sonnensystem?",
        answerA = "7",
        answerB = "9",
        answerC = "8",
        answerD = "10",
        correctAnswer = 2,
        explanation = "Seit 2006 erkennt die IAU 8 Planeten an: Merkur, Venus, Erde, Mars, Jupiter, Saturn, Uranus, Neptun. Pluto wurde zum Zwergplaneten zurückgestuft.",
        difficulty = 1,
        funFact = "Pluto war von 1930 bis 2006 offiziell als Planet anerkannt."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die chemische Formel für Kochsalz?",
        answerA = "KCl",
        answerB = "CaCO₃",
        answerC = "NaOH",
        answerD = "NaCl",
        correctAnswer = 3,
        explanation = "Kochsalz (Natriumchlorid) hat die Formel NaCl und besteht aus Natrium- und Chloridionen.",
        difficulty = 1,
        funFact = "Menschen benötigen täglich etwa 1,5 g Salz — die meisten nehmen jedoch viel mehr zu sich."
    ),

    Question(
        categoryId = 2,
        questionText = "Welcher Aggregatzustand hat weder eine feste Form noch ein festes Volumen?",
        answerA = "Fest",
        answerB = "Flüssig",
        answerC = "Gas",
        answerD = "Plasma",
        correctAnswer = 2,
        explanation = "Gase haben weder feste Form noch festes Volumen — sie dehnen sich aus und füllen jeden Behälter vollständig aus.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Aufgabe der weißen Blutkörperchen?",
        answerA = "Sauerstofftransport",
        answerB = "Blutgerinnung",
        answerC = "Abwehr von Krankheitserregern",
        answerD = "Nährstofftransport",
        correctAnswer = 2,
        explanation = "Weiße Blutkörperchen (Leukozyten) sind Teil des Immunsystems und bekämpfen Bakterien, Viren und andere Krankheitserreger.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Gas entsteht bei der Verbrennung von Holz hauptsächlich?",
        answerA = "Sauerstoff",
        answerB = "Stickstoff",
        answerC = "Kohlendioxid",
        answerD = "Methan",
        correctAnswer = 2,
        explanation = "Bei der Verbrennung von Holz (Kohlenstoffverbindungen) reagiert der Kohlenstoff mit Sauerstoff zu Kohlendioxid (CO₂).",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was misst ein Thermometer?",
        answerA = "Luftdruck",
        answerB = "Luftfeuchtigkeit",
        answerC = "Windgeschwindigkeit",
        answerD = "Temperatur",
        correctAnswer = 3,
        explanation = "Ein Thermometer misst die Temperatur, typischerweise in Grad Celsius (°C), Kelvin (K) oder Fahrenheit (°F).",
        difficulty = 1,
        funFact = "Das erste Thermometer wurde um 1592 von Galileo Galilei entwickelt."
    ),

    Question(
        categoryId = 2,
        questionText = "Welcher Planet ist der größte in unserem Sonnensystem?",
        answerA = "Saturn",
        answerB = "Neptun",
        answerC = "Uranus",
        answerD = "Jupiter",
        correctAnswer = 3,
        explanation = "Jupiter ist mit einem Äquatordurchmesser von etwa 143.000 km der größte Planet im Sonnensystem.",
        difficulty = 1,
        funFact = "In Jupiter würden mehr als 1.300 Erden passen."
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Organ pumpt das Blut durch den Körper?",
        answerA = "Lunge",
        answerB = "Leber",
        answerC = "Herz",
        answerD = "Niere",
        correctAnswer = 2,
        explanation = "Das Herz ist ein Hohlmuskel, der durch rhythmische Kontraktionen das Blut durch den Blutkreislauf pumpt.",
        difficulty = 1,
        funFact = "Das menschliche Herz schlägt im Laufe eines Lebens etwa 2,5 Milliarden Mal."
    ),

    Question(
        categoryId = 2,
        questionText = "Wie nennt man die Kraft, die Objekte zur Erde zieht?",
        answerA = "Magnetismus",
        answerB = "Reibung",
        answerC = "Trägheit",
        answerD = "Schwerkraft",
        correctAnswer = 3,
        explanation = "Die Schwerkraft (Gravitation) ist die Anziehungskraft, die die Erde auf alle Objekte mit Masse ausübt.",
        difficulty = 1,
        funFact = "Auf dem Mond beträgt die Schwerkraft nur etwa 1/6 der Erdanziehungskraft."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Siedepunkt von Wasser bei normalem Luftdruck?",
        answerA = "80 °C",
        answerB = "90 °C",
        answerC = "100 °C",
        answerD = "110 °C",
        correctAnswer = 2,
        explanation = "Wasser siedet bei 100 °C (373,15 K) bei einem Standarddruck von 1013,25 hPa (1 Atmosphäre).",
        difficulty = 1,
        funFact = "In großer Höhe, z. B. auf dem Everest, siedet Wasser bereits bei etwa 70 °C, weil der Luftdruck niedriger ist."
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Tier hat das größte Gehirn relativ zu seiner Körpergröße?",
        answerA = "Elefant",
        answerB = "Delfin",
        answerC = "Mensch",
        answerD = "Schimpanse",
        correctAnswer = 2,
        explanation = "Der Mensch hat im Verhältnis zur Körpergröße das größte Gehirn unter allen Tieren — das sogenannte Enzephalisierungsquotient (EQ) ist beim Menschen am höchsten.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Hauptfunktion des Skeletts im menschlichen Körper?",
        answerA = "Blut produzieren und Körper stützen",
        answerB = "Nahrung verdauen",
        answerC = "Sauerstoff transportieren",
        answerD = "Hormone produzieren",
        correctAnswer = 0,
        explanation = "Das Skelett stützt den Körper, schützt innere Organe und ist an der Blutbildung im Knochenmark beteiligt.",
        difficulty = 1,
        funFact = "Rote Blutkörperchen werden im roten Knochenmark produziert — hauptsächlich in Brustbein, Wirbeln und Beckenknochen."
    ),

    Question(
        categoryId = 2,
        questionText = "Wie nennt man den Vorgang, bei dem Wasser von flüssig zu gasförmig wird?",
        answerA = "Kondensation",
        answerB = "Sublimation",
        answerC = "Kristallisation",
        answerD = "Verdampfung",
        correctAnswer = 3,
        explanation = "Verdampfung (Evaporation) ist der Phasenübergang von flüssig zu gasförmig. Dieser kann durch Erhitzen (Sieden) oder langsam an der Oberfläche geschehen.",
        difficulty = 1,
        funFact = null
    ),

    // ── NEW MEDIUM (difficulty = 2) — 20 questions ────────────────────────────

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einer exothermen und einer endothermen Reaktion?",
        answerA = "Exotherm gibt Wärme ab, endotherm nimmt Wärme auf",
        answerB = "Exotherm läuft nur in Säuren ab, endotherm nur in Basen",
        answerC = "Exotherm verbraucht Katalysatoren, endotherm nicht",
        answerD = "Exotherm findet nur bei hoher Temperatur statt",
        correctAnswer = 0,
        explanation = "Bei exothermen Reaktionen wird Energie freigesetzt (z. B. Verbrennung). Bei endothermen Reaktionen wird Energie aufgenommen (z. B. Fotosynthese, Kältepacks).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Strahlung wird von der Ozonschicht hauptsächlich absorbiert?",
        answerA = "Infrarotstrahlung",
        answerB = "Gammastrahlung",
        answerC = "UV-B und UV-C Strahlung",
        answerD = "Röntgenstrahlung",
        correctAnswer = 2,
        explanation = "Die Ozonschicht in der Stratosphäre absorbiert den Großteil der UV-B- und nahezu die gesamte UV-C-Strahlung der Sonne.",
        difficulty = 2,
        funFact = "Ohne Ozonschicht wäre höheres Leben an Land aufgrund der schädigenden UV-Strahlung kaum möglich."
    ),

    Question(
        categoryId = 2,
        questionText = "Was versteht man unter 'osmotischem Druck'?",
        answerA = "Den Druck, den eine Flüssigkeit auf ihre Behälterwände ausübt",
        answerB = "Den Druck, der nötig ist, um Osmose durch eine semipermeable Membran zu verhindern",
        answerC = "Den Gasdruck in einer biologischen Zelle",
        answerD = "Die Kraft, mit der Ionen durch Membranen wandern",
        correctAnswer = 1,
        explanation = "Osmotischer Druck ist der Druck, der aufgebracht werden muss, um den Wasserstrom durch eine semipermeable Membran von der niedrig- zur hochkonzentrierten Seite zu stoppen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Organ produziert die meisten Verdauungsenzyme im menschlichen Körper?",
        answerA = "Leber",
        answerB = "Magen",
        answerC = "Bauchspeicheldrüse",
        answerD = "Dünndarm",
        correctAnswer = 2,
        explanation = "Die Bauchspeicheldrüse (Pankreas) produziert Amylase, Lipase, Proteasen und andere Verdauungsenzyme, die in den Dünndarm abgegeben werden.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Prinzip der natürlichen Selektion nach Darwin?",
        answerA = "Alle Organismen einer Art sind gleich angepasst",
        answerB = "Individuen mit vorteilhaften Merkmalen überleben häufiger und pflanzen sich fort",
        answerC = "Mutationen entstehen gezielt als Reaktion auf die Umwelt",
        answerD = "Alle Lebewesen entwickeln sich gleichmäßig schnell",
        correctAnswer = 1,
        explanation = "Natürliche Selektion: Individuen mit günstigen Merkmalen haben einen Überlebensvorteil, pflanzen sich häufiger fort und geben ihre Gene weiter.",
        difficulty = 2,
        funFact = "Darwin entwickelte seine Evolutionstheorie nach seiner Reise auf den Galapagosinseln (1835)."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen Kernenergie und chemischer Energie?",
        answerA = "Kernenergie stammt aus Elektronenhüllen, chemische Energie aus dem Kern",
        answerB = "Kernenergie wird bei Kernreaktionen freigesetzt, chemische Energie bei chemischen Reaktionen",
        answerC = "Kernenergie ist immer größer als chemische Energie",
        answerD = "Es gibt keinen Unterschied — beide stammen aus Elektronen",
        correctAnswer = 1,
        explanation = "Kernenergie entsteht durch Veränderungen im Atomkern (Spaltung/Fusion). Chemische Energie stammt aus dem Umbau chemischer Bindungen in der Elektronenhülle.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Zellen im menschlichen Auge sind für das Farbsehen verantwortlich?",
        answerA = "Stäbchen",
        answerB = "Ganglienzellen",
        answerC = "Bipolarzellen",
        answerD = "Zapfen",
        correctAnswer = 3,
        explanation = "Zapfen sind die Fotorezeptorzellen der Netzhaut, die für Farbsehen zuständig sind. Es gibt drei Typen (rot, grün, blau). Stäbchen ermöglichen das Sehen bei Dunkelheit.",
        difficulty = 2,
        funFact = "Etwa 8 % der Männer und 0,5 % der Frauen sind farbenblind."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist Diffusion?",
        answerA = "Der Transport von Stoffen gegen ein Konzentrationsgefälle unter Energieaufwand",
        answerB = "Die passive Bewegung von Teilchen von hoher zu niedriger Konzentration",
        answerC = "Die Aufnahme von Stoffen durch aktiven Transport",
        answerD = "Die Bewegung von Ionen durch elektrische Felder",
        correctAnswer = 1,
        explanation = "Diffusion ist der passive Transport von Teilchen entlang eines Konzentrationsgefälles (von hoch nach niedrig) ohne Energieaufwand.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einem Element und einer Verbindung?",
        answerA = "Elemente bestehen aus mehreren Atomen, Verbindungen aus einem",
        answerB = "Elemente bestehen aus einer Atomsorte, Verbindungen aus mehreren chemisch verbundenen Elementen",
        answerC = "Elemente können zerfallen, Verbindungen nicht",
        answerD = "Es gibt keinen chemischen Unterschied",
        correctAnswer = 1,
        explanation = "Ein Element besteht nur aus einer Atomsorte (z. B. O₂, Fe). Eine Verbindung enthält zwei oder mehr verschiedene Elemente, die chemisch gebunden sind (z. B. H₂O, NaCl).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welcher Vorgang findet bei der Meiose statt?",
        answerA = "Verdoppelung der Chromosomenzahl",
        answerB = "Halbierung der Chromosomenzahl zur Bildung von Keimzellen",
        answerC = "Reparatur beschädigter DNA",
        answerD = "Synthese von Proteinen",
        correctAnswer = 1,
        explanation = "Meiose ist eine spezielle Zellteilung, bei der die Chromosomenzahl halbiert wird, um haploide Keimzellen (Spermien, Eizellen) zu bilden.",
        difficulty = 2,
        funFact = "Die genetische Vielfalt entsteht durch Crossing-over und zufällige Verteilung der Chromosomen in der Meiose."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein Isotop?",
        answerA = "Ein Atom mit mehr Protonen als Elektronen",
        answerB = "Atome desselben Elements mit unterschiedlicher Neutronenzahl",
        answerC = "Ein radioaktives Atom mit instabilem Kern",
        answerD = "Ein Ion mit einer negativen Ladung",
        correctAnswer = 1,
        explanation = "Isotope sind Atome desselben Elements (gleiche Protonenzahl), die sich in der Neutronenzahl und damit der Massenzahl unterscheiden.",
        difficulty = 2,
        funFact = "Kohlenstoff-14 (¹⁴C) ist ein radioaktives Isotop und wird zur Altersbestimmung organischer Materialien genutzt."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Kraft ist für die Orbitalewegung der Erde um die Sonne verantwortlich?",
        answerA = "Elektromagnetische Kraft",
        answerB = "Zentrifugalkraft",
        answerC = "Gravitation",
        answerD = "Starke Kernkraft",
        correctAnswer = 2,
        explanation = "Die Gravitation der Sonne hält die Erde auf ihrer elliptischen Umlaufbahn. Die Erde bewegt sich mit ca. 29,8 km/s um die Sonne.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen aerober und anaerober Atmung?",
        answerA = "Aerobe Atmung benötigt Sauerstoff, anaerobe nicht",
        answerB = "Aerobe Atmung findet nur in Pflanzen statt",
        answerC = "Anaerobe Atmung produziert mehr ATP als aerobe",
        answerD = "Aerobe Atmung findet nur bei hoher Temperatur statt",
        correctAnswer = 0,
        explanation = "Aerobe Atmung verbraucht Sauerstoff und produziert CO₂, Wasser und viel ATP. Anaerobe Atmung läuft ohne Sauerstoff ab (z. B. Gärung) und liefert weniger Energie.",
        difficulty = 2,
        funFact = "Beim Muskelkater entsteht Laktat durch anaerobe Glykolyse bei intensiver Belastung."
    ),

    Question(
        categoryId = 2,
        questionText = "Was versteht man unter 'latenter Wärme'?",
        answerA = "Die Wärme, die ein Körper beim Abkühlen abgibt",
        answerB = "Die Energie, die bei einem Phasenübergang ohne Temperaturänderung aufgenommen oder abgegeben wird",
        answerC = "Die Wärme, die durch Strahlung übertragen wird",
        answerD = "Die gespeicherte Wärme in einem Isolierkörper",
        correctAnswer = 1,
        explanation = "Latente Wärme ist die Energie, die bei einem Phasenübergang (z. B. Schmelzen, Verdampfen) benötigt wird oder freigesetzt wird, ohne dass die Temperatur sich ändert.",
        difficulty = 2,
        funFact = "Schwitzen kühlt den Körper genau durch latente Wärme: Wasser verdunstet und entzieht der Haut dabei Energie."
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Prinzip beschreibt, dass ein Körper in einer Flüssigkeit Auftrieb erfährt, der gleich dem Gewicht der verdrängten Flüssigkeit ist?",
        answerA = "Pascalsches Gesetz",
        answerB = "Archimedisches Prinzip",
        answerC = "Bernoullisches Gesetz",
        answerD = "Gesetz von Avogadro",
        correctAnswer = 1,
        explanation = "Das Archimedische Prinzip besagt: Der Auftrieb auf einen eingetauchten Körper ist gleich dem Gewicht der von ihm verdrängten Flüssigkeit.",
        difficulty = 2,
        funFact = "Archimedes soll diese Erkenntnis in der Badewanne gehabt und 'Heureka!' gerufen haben."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Funktion der Ribosome in der Zelle?",
        answerA = "DNA-Replikation",
        answerB = "Proteinsynthese",
        answerC = "ATP-Produktion",
        answerD = "Membrantransport",
        correctAnswer = 1,
        explanation = "Ribosomen sind die molekularen Maschinen der Zelle, die mRNA-Sequenzen in Aminosäureketten (Proteine) übersetzen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen ionischen und kovalenten Bindungen?",
        answerA = "Ionische Bindungen teilen Elektronen, kovalente übertragen sie",
        answerB = "Ionische Bindungen übertragen Elektronen zwischen Atomen, kovalente teilen Elektronen",
        answerC = "Ionische Bindungen existieren nur in Gasen, kovalente in Feststoffen",
        answerD = "Beide Bindungstypen sind identisch",
        correctAnswer = 1,
        explanation = "Bei ionischen Bindungen werden Elektronen von einem Atom auf ein anderes übertragen (z. B. NaCl). Bei kovalenten Bindungen werden Elektronen gemeinsam genutzt (z. B. H₂O).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Gesetz der Thermodynamik besagt, dass Energie weder erzeugt noch vernichtet werden kann?",
        answerA = "Zweiter Hauptsatz der Thermodynamik",
        answerB = "Nullter Hauptsatz der Thermodynamik",
        answerC = "Erster Hauptsatz der Thermodynamik",
        answerD = "Dritter Hauptsatz der Thermodynamik",
        correctAnswer = 2,
        explanation = "Der erste Hauptsatz der Thermodynamik ist der Energieerhaltungssatz: Die Gesamtenergie eines abgeschlossenen Systems bleibt konstant.",
        difficulty = 2,
        funFact = "Der zweite Hauptsatz besagt, dass Entropie in einem abgeschlossenen System nie abnehmen kann."
    ),

    Question(
        categoryId = 2,
        questionText = "Welcher Teil des Gehirns ist hauptsächlich für Gleichgewicht und Koordination zuständig?",
        answerA = "Großhirnrinde",
        answerB = "Hirnstamm",
        answerC = "Kleinhirn",
        answerD = "Hippocampus",
        correctAnswer = 2,
        explanation = "Das Kleinhirn (Cerebellum) koordiniert Bewegungsabläufe, regelt das Gleichgewicht und verfeinert motorische Aktionen.",
        difficulty = 2,
        funFact = "Obwohl das Kleinhirn nur etwa 10 % des Hirnvolumens ausmacht, enthält es rund 50 % aller Neuronen des Gehirns."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein Katalysator?",
        answerA = "Ein Stoff, der eine Reaktion dauerhaft verlangsamt",
        answerB = "Ein Stoff, der eine chemische Reaktion beschleunigt, ohne dabei verbraucht zu werden",
        answerC = "Ein Stoff, der bei einer Reaktion selbst entsteht",
        answerD = "Ein Stoff, der nur bei hohen Temperaturen wirkt",
        correctAnswer = 1,
        explanation = "Katalysatoren senken die Aktivierungsenergie einer Reaktion und beschleunigen sie, ohne selbst verbraucht zu werden. Enzyme sind biologische Katalysatoren.",
        difficulty = 2,
        funFact = "Im menschlichen Körper wirken tausende verschiedene Enzyme als hochspezifische Biokatalysatoren."
    ),

    // ── NEW HARD (difficulty = 3) — 15 questions ──────────────────────────────

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Konzept der 'Quantenverschränkung'?",
        answerA = "Teilchen, die sich schneller als Licht bewegen können",
        answerB = "Zwei Teilchen, deren Quantenzustände so korreliert sind, dass die Messung eines sofort den anderen beeinflusst",
        answerC = "Das Überlappen von Elektronorbitalen in Molekülen",
        answerD = "Die Superposition von zwei Energiezuständen eines Atoms",
        correctAnswer = 1,
        explanation = "Quantenverschränkung beschreibt Teilchenpaare, deren Eigenschaften selbst nach großer Trennung miteinander korreliert bleiben — Einstein nannte es 'spukhafte Fernwirkung'.",
        difficulty = 3,
        funFact = "Verschränkte Teilchen können keine Informationen schneller als Licht übertragen — das No-Communication-Theorem verhindert das."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Aminosäure ist für die tertiäre Struktur von Proteinen durch Disulfidbrücken verantwortlich?",
        answerA = "Glycin",
        answerB = "Cystein",
        answerC = "Prolin",
        answerD = "Methionin",
        correctAnswer = 1,
        explanation = "Cystein enthält eine Thiolgruppe (-SH). Zwei Cysteinreste können durch Oxidation eine kovalente Disulfidbrücke (-S-S-) bilden, die Proteinstrukturen stabilisiert.",
        difficulty = 3,
        funFact = "Disulfidbrücken sind der Grund, warum Haare (Keratin) so stabil sind — Dauerwellen brechen und formen diese Brücken neu."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einem Neutronenstern und einem weißen Zwerg?",
        answerA = "Ein weißer Zwerg ist heißer als ein Neutronenstern",
        answerB = "Ein Neutronenstern entsteht aus massereicheren Sternen und besteht hauptsächlich aus Neutronen",
        answerC = "Neutronensterne sind weniger dicht als weiße Zwerge",
        answerD = "Weiße Zwerge haben stärkere Magnetfelder als Neutronensterne",
        correctAnswer = 1,
        explanation = "Weiße Zwerge entstehen aus Sternen bis etwa 8 Sonnenmassen und bestehen aus entartetem Elektronengas. Neutronensterne entstehen aus massereicheren Sternen und haben eine Dichte von ~10¹⁷ kg/m³.",
        difficulty = 3,
        funFact = "Ein Teelöffel Neutronensternenmaterie würde auf der Erde etwa eine Milliarde Tonnen wiegen."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein 'Wirkungsquerschnitt' in der Teilchenphysik?",
        answerA = "Der physische Querschnitt eines Atomkerns in Metern",
        answerB = "Ein Maß für die Wahrscheinlichkeit, dass eine Wechselwirkung zwischen Teilchen stattfindet",
        answerC = "Die Breite des Elektronenstrahls in einem Beschleuniger",
        answerD = "Die Fläche eines Detektors in einem Teilchenexperiment",
        correctAnswer = 1,
        explanation = "Der Wirkungsquerschnitt (σ) ist eine effektive Querschnittsfläche, die die Wahrscheinlichkeit einer Reaktion beschreibt. Er wird in Barn gemessen (1 barn = 10⁻²⁴ cm²).",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist das Prinzip hinter der PCR (Polymerasekettenreaktion)?",
        answerA = "Proteine werden mithilfe von RNA vervielfältigt",
        answerB = "DNA-Abschnitte werden durch wiederholte Zyklen von Denaturierung, Anlagerung und Synthese exponentiell vervielfältigt",
        answerC = "DNA wird durch Restriktionsenzyme in Fragmente geschnitten",
        answerD = "Genexpression wird durch chemische Signale gesteuert",
        correctAnswer = 1,
        explanation = "PCR (Mullis, 1983) amplifiziert DNA durch Temperaturzyklen: Denaturierung (DNA trennt sich), Primer-Anlagerung, Elongation (DNA-Polymerase synthetisiert neuen Strang).",
        difficulty = 3,
        funFact = "Kary Mullis erhielt 1993 den Chemie-Nobelpreis für die Entwicklung der PCR. COVID-Tests nutzen eine abgewandelte Version davon."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das 'Le Chatelier'sche Prinzip' in der Chemie?",
        answerA = "Gleichgewichtsreaktionen verlaufen immer exotherm",
        answerB = "Ein System im Gleichgewicht reagiert auf Störungen, indem es der Störung entgegenwirkt",
        answerC = "Die Konzentration von Produkten ist immer größer als die der Edukte",
        answerD = "Katalysatoren verschieben das Gleichgewicht auf die Produktseite",
        correctAnswer = 1,
        explanation = "Das Le Chatelier-Prinzip: Wenn ein Gleichgewichtssystem durch externe Einflüsse (Druck, Temperatur, Konzentration) gestört wird, verschiebt es sich so, dass der Störung entgegengewirkt wird.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen den Begriffen 'Genotyp' und 'Phänotyp'?",
        answerA = "Genotyp ist sichtbar, Phänotyp ist im Erbgut versteckt",
        answerB = "Genotyp beschreibt die genetische Ausstattung, Phänotyp die beobachtbaren Merkmale",
        answerC = "Genotyp gilt nur für Tiere, Phänotyp für Pflanzen",
        answerD = "Sie bedeuten dasselbe",
        correctAnswer = 1,
        explanation = "Der Genotyp ist die genetische Zusammensetzung eines Organismus. Der Phänotyp sind die sichtbaren, messbaren Merkmale — Ergebnis von Genotyp und Umwelteinflüssen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist supraleitfähigkeit?",
        answerA = "Die Fähigkeit eines Metalls, bei hoher Temperatur besonders gut Strom zu leiten",
        answerB = "Ein Zustand, in dem ein Material unterhalb einer kritischen Temperatur elektrischen Strom ohne Widerstand leitet",
        answerC = "Die magnetische Eigenschaft von Eisenkernen bei tiefen Temperaturen",
        answerD = "Die optische Transparenz bestimmter Materialien bei tiefen Temperaturen",
        correctAnswer = 1,
        explanation = "Supraleitung tritt unterhalb einer kritischen Temperatur auf: Der elektrische Widerstand fällt auf exakt null, und Magnetfelder werden ausgetrieben (Meißner-Effekt).",
        difficulty = 3,
        funFact = "Hochtemperatursupraleiter (bis ~135 K) werden in MRT-Geräten und Teilchenbeschleunigern wie dem LHC eingesetzt."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Funktion des Corpus callosum im Gehirn?",
        answerA = "Regulierung des Schlaf-Wach-Rhythmus",
        answerB = "Verbindung der linken und rechten Gehirnhemisphäre",
        answerC = "Koordination der Augenbewegungen",
        answerD = "Speicherung von Langzeiterinnerungen",
        correctAnswer = 1,
        explanation = "Der Corpus callosum (Balken) ist ein dicker Faserbündel, der die Kommunikation zwischen linker und rechter Gehirnhemisphäre ermöglicht.",
        difficulty = 3,
        funFact = "Menschen, bei denen der Corpus callosum durchtrennt wurde ('Split-Brain-Patienten'), zeigen faszinierende Effekte der Hemisphärentrennung."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Coulombsche Gesetz?",
        answerA = "Die Kraft zwischen zwei elektrischen Ladungen ist proportional zum Produkt der Ladungen und umgekehrt proportional zum Quadrat des Abstands",
        answerB = "Der Strom in einem Stromkreis ist proportional zur Spannung",
        answerC = "Gleiche Ladungen ziehen sich an, entgegengesetzte stoßen sich ab",
        answerD = "Die elektrische Feldstärke ist überall in einem Leiter gleich",
        correctAnswer = 0,
        explanation = "Das Coulombsche Gesetz: F = k · (q₁ · q₂) / r² — die elektrische Kraft zwischen zwei Punktladungen ist proportional zum Produkt der Ladungen und nimmt mit dem Quadrat des Abstands ab.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein Pulsar?",
        answerA = "Ein Stern, der in regelmäßigen Abständen explodiert",
        answerB = "Ein rotierender Neutronenstern, der regelmäßige elektromagnetische Strahlenimpulse aussendet",
        answerC = "Ein Schwarzes Loch, das Materie akkretiert",
        answerD = "Ein junger Stern in der Hauptreihe des Hertzsprung-Russell-Diagramms",
        correctAnswer = 1,
        explanation = "Ein Pulsar ist ein schnell rotierender Neutronenstern mit einem starken Magnetfeld, der wie ein kosmischer Leuchtturm gebündelte Strahlungskegel aussendet.",
        difficulty = 3,
        funFact = "Der erste Pulsar wurde 1967 entdeckt — die Astrophysikerin Jocelyn Bell Burnell nannte ihn zunächst LGM-1 (Little Green Men), da die Regelmäßigkeit des Signals an künstliche Signale erinnerte."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Bedeutung des chemischen Gleichgewichtskonstante K?",
        answerA = "Die maximale Konzentration der Produkte bei einer Reaktion",
        answerB = "Das Verhältnis der Konzentrationen von Produkten zu Edukten im Gleichgewichtszustand",
        answerC = "Die Reaktionsgeschwindigkeit bei Standardbedingungen",
        answerD = "Die Mindestenergie, die für eine Reaktion benötigt wird",
        correctAnswer = 1,
        explanation = "Die Gleichgewichtskonstante K = [Produkte]^n / [Edukte]^m beschreibt, in welchem Verhältnis Produkte und Edukte bei Gleichgewicht vorliegen. K > 1 bedeutet Reaktion läuft bevorzugt zu den Produkten.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Phänomen erklärt, warum die Sonne 'gelb-orange' erscheint, obwohl sie weißes Licht emittiert?",
        answerA = "Die Sonne emittiert tatsächlich gelbes Licht",
        answerB = "Atmosphärische Streuung (Rayleigh) filtert blaues Licht in niedrigen Elevationswinkeln heraus",
        answerC = "Das menschliche Auge ist für Gelb besonders empfindlich",
        answerD = "Die Magnetfelder der Sonne absorbieren blaues Licht",
        correctAnswer = 1,
        explanation = "Die Rayleigh-Streuung streut blaues Licht stärker als rotes. Bei niedrigem Stand der Sonne legt das Licht einen längeren Weg durch die Atmosphäre zurück, sodass das Blau herausgefiltert wird.",
        difficulty = 3,
        funFact = "Im Weltraum erscheint die Sonne strahlend weiß, da keine Atmosphäre das Licht streut."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einem Dom-Superprotein (Prion) und normalen Proteinen?",
        answerA = "Prionen enthalten RNA, normale Proteine nicht",
        answerB = "Prionen sind fehlgefaltete Proteine, die andere Proteine zur gleichen Fehlfaltung veranlassen können",
        answerC = "Prionen sind kleiner als normale Proteine",
        answerD = "Prionen entstehen ausschließlich durch Virusinfektionen",
        correctAnswer = 1,
        explanation = "Prionen sind infektiöse fehlgefaltete Proteinvarianten (PrPSc), die normale Versionen desselben Proteins (PrPC) zur Fehlfaltung veranlassen können — ohne Nukleinsäuren.",
        difficulty = 3,
        funFact = "Prionen verursachen Krankheiten wie BSE (Rinderwahn) und die Creutzfeldt-Jakob-Krankheit beim Menschen."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Eigenfrequenz in der Physik?",
        answerA = "Die maximale Schwingungsfrequenz eines Materials vor dem Bruch",
        answerB = "Die Frequenz, mit der ein System bei Störungen natürlich schwingt",
        answerC = "Die Frequenz elektromagnetischer Wellen in einem Hohlleiter",
        answerD = "Die minimale Frequenz, ab der Schall hörbar ist",
        correctAnswer = 1,
        explanation = "Die Eigenfrequenz (Resonanzfrequenz) ist die Frequenz, mit der ein schwingfähiges System nach einer Auslenkung natürlich oscilliert. Bei externer Anregung mit dieser Frequenz tritt Resonanz auf.",
        difficulty = 3,
        funFact = "Die Tacoma-Narrows-Brücke kollabierte 1940, weil Wind Resonanzschwingungen nahe der Eigenfrequenz der Brücke auslöste."
    ),

    // ── EXPERT (difficulty = 4) ───────────────────────────────────────────────

    Question(
        categoryId = 2,
        questionText = "Was beschreibt die Schrödinger-Gleichung?",
        answerA = "Die klassische Bewegung von Teilchen in einem elektrischen Feld",
        answerB = "Die Zeitentwicklung der Wellenfunktion eines Quantensystems",
        answerC = "Die Beziehung zwischen Energie und Masse in der Relativitätstheorie",
        answerD = "Die Verteilung von Elektronen in chemischen Bindungen",
        correctAnswer = 1,
        explanation = "Die Schrödinger-Gleichung ist die zentrale Gleichung der Quantenmechanik: iℏ ∂Ψ/∂t = ĤΨ. Sie beschreibt, wie sich Quantenzustände zeitlich entwickeln.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen mRNA und tRNA?",
        answerA = "mRNA ist doppelsträngig, tRNA einzelsträngig",
        answerB = "mRNA trägt die Proteinbauanleitung, tRNA transportiert Aminosäuren zum Ribosom",
        answerC = "mRNA befindet sich nur im Zellkern, tRNA nur im Zytoplasma",
        answerD = "Sie unterscheiden sich nur in der Länge",
        correctAnswer = 1,
        explanation = "mRNA (Boten-RNA) überträgt die genetische Information vom Zellkern zum Ribosom. tRNA (Transfer-RNA) bringt die passenden Aminosäuren zur Proteinsynthese.",
        difficulty = 4,
        funFact = "Es gibt 20 verschiedene Aminosäuren und über 60 verschiedene tRNA-Typen."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Konzept der 'chemischen Elektronegativität'?",
        answerA = "Die Fähigkeit eines Atoms, Elektronen bei einer chemischen Bindung anzuziehen",
        answerB = "Die negative Ladung eines Ions in Lösung",
        answerC = "Die Anzahl der Valenzelektronen eines Elements",
        answerD = "Die Affinität eines Atoms gegenüber Protonen",
        correctAnswer = 0,
        explanation = "Elektronegativität (nach Pauling) beschreibt die relative Fähigkeit eines Atoms, Elektronen in einer kovalenten Bindung anzuziehen. Fluor hat die höchste EN (3,98).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Konzept beschreibt die 'epigenetische Vererbung'?",
        answerA = "Vererbung durch Veränderungen der DNA-Sequenz selbst",
        answerB = "Vererbung von Merkmalen durch chemische Modifikationen der DNA oder Histone ohne Änderung der Sequenz",
        answerC = "Horizontaler Gentransfer zwischen verschiedenen Arten",
        answerD = "Rekombination von Genen während der Meiose",
        correctAnswer = 1,
        explanation = "Epigenetische Vererbung umfasst vererbbare Genregulationsänderungen (z. B. DNA-Methylierung, Histonmodifikation) ohne Veränderung der eigentlichen DNA-Sequenz.",
        difficulty = 4,
        funFact = "Umweltfaktoren wie Ernährung oder Stress können epigenetische Markierungen hinterlassen, die an Nachkommen weitergegeben werden."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen Fusion und Fission in der Kernphysik?",
        answerA = "Fusion verbrennt Atome, Fission kühlt sie ab",
        answerB = "Fusion vereint leichte Kerne, Fission spaltet schwere Kerne",
        answerC = "Fusion erzeugt Gammastrahlung, Fission nur Alphastrahlung",
        answerD = "Fusion findet nur in Sternen statt, Fission nur in Reaktoren",
        correctAnswer = 1,
        explanation = "Kernfusion vereinigt leichte Atomkerne (z. B. Wasserstoff → Helium in Sternen). Kernspaltung (Fission) spaltet schwere Kerne (z. B. Uran-235) in kleinere.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem 'Hardy-Weinberg-Gleichgewicht'?",
        answerA = "Das Gleichgewicht zwischen Predatoren und Beutetieren in einem Ökosystem",
        answerB = "Ein Modell, bei dem Allelfrequenzen in einer Population ohne evolutionäre Einflüsse konstant bleiben",
        answerC = "Die Stabilisierung von Proteinfaltungen in einer Zelle",
        answerD = "Das thermodynamische Gleichgewicht in biochemischen Reaktionen",
        correctAnswer = 1,
        explanation = "Das Hardy-Weinberg-Gleichgewicht beschreibt eine ideale Population ohne Mutation, Selektion, Gendrift oder Migration, in der Allelfrequenzen konstant bleiben.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein Fermion?",
        answerA = "Ein Teilchen mit ganzzahligem Spin, das dem Bose-Einstein-Statistik folgt",
        answerB = "Ein Teilchen mit halbzahligem Spin, das dem Pauli-Ausschlussprinzip unterliegt",
        answerC = "Ein hypothetisches Teilchen, das Schwerkraft überträgt",
        answerD = "Ein zusammengesetztes Teilchen aus drei Quarks",
        correctAnswer = 1,
        explanation = "Fermionen sind Teilchen mit halbzahligem Spin (1/2, 3/2, ...) und folgen der Fermi-Dirac-Statistik. Beispiele: Elektronen, Quarks, Protonen.",
        difficulty = 4,
        funFact = "Bosonen (ganzzahliger Spin) können denselben Quantenzustand teilen – daher ist ein Laser möglich."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt die 'allgemeine Relativitätstheorie' von Einstein?",
        answerA = "Die Äquivalenz von Masse und Energie (E=mc²)",
        answerB = "Die Krümmung der Raumzeit durch Masse und Energie",
        answerC = "Das Verhalten von Teilchen nahe der Lichtgeschwindigkeit",
        answerD = "Die Zeitdilatation bei hoher Geschwindigkeit",
        correctAnswer = 1,
        explanation = "Die allgemeine Relativitätstheorie (1915) beschreibt Gravitation als Krümmung der vierdimensionalen Raumzeit durch Masse und Energie.",
        difficulty = 4,
        funFact = "GPS-Satelliten müssen die Vorhersagen der allgemeinen Relativitätstheorie berücksichtigen – sonst wäre die Positionsgenauigkeit täglich um mehrere Kilometer falsch."
    ),

    // ── MASTER (difficulty = 5) ───────────────────────────────────────────────

    Question(
        categoryId = 2,
        questionText = "Was beschreibt die 'Quantenchromodynamik' (QCD)?",
        answerA = "Die Theorie der elektromagnetischen Wechselwirkung zwischen geladenen Teilchen",
        answerB = "Die Quantenfeldtheorie der starken Wechselwirkung zwischen Quarks und Gluonen",
        answerC = "Ein Modell zur Beschreibung von Farbwahrnehmung auf Quantenebene",
        answerD = "Die Vereinigung von Quantenmechanik und allgemeiner Relativitätstheorie",
        correctAnswer = 1,
        explanation = "Die QCD ist die Quantenfeldtheorie der starken Kernkraft. Sie beschreibt, wie Quarks durch Gluonen zusammengehalten werden, wobei 'Farbe' eine Quanteneigenschaft (rot, grün, blau) ist.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist 'Confinement' in der Quantenchromodynamik?",
        answerA = "Die Eigenschaft, dass Quarks nur in gebundenen Zuständen (Hadronen) existieren und nicht isoliert vorkommen können",
        answerB = "Die Einschränkung von Elektronen auf bestimmte Energieniveaus",
        answerC = "Die Begrenzung der Lichtgeschwindigkeit durch die starke Wechselwirkung",
        answerD = "Das Einschluss-Phänomen in Quantencomputern",
        correctAnswer = 0,
        explanation = "Confinement ist das Phänomen, dass freie Quarks niemals beobachtet wurden. Die Stärke der starken Kraft nimmt mit dem Abstand zu, sodass Quarks nie isoliert existieren können.",
        difficulty = 5,
        funFact = "Versucht man, Quarks zu trennen, wird so viel Energie eingebracht, dass neue Quark-Antiquark-Paare entstehen – statt freier Quarks."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt die 'Chandrasekhar-Grenze'?",
        answerA = "Die maximale Temperatur, bei der ein Stern noch existieren kann",
        answerB = "Die maximale Masse eines weißen Zwergs (~1,4 Sonnenmassen), bevor er kollabiert",
        answerC = "Den minimalen Radius für die Entstehung eines Schwarzen Lochs",
        answerD = "Die Grenzfrequenz, ab der Photonen Materie ionisieren können",
        correctAnswer = 1,
        explanation = "Die Chandrasekhar-Grenze (~1,44 Sonnenmassen) ist die maximale Masse, die ein weißer Zwerg durch den Quantendruck der Elektronen tragen kann. Wird sie überschritten, kollabiert er zu einem Neutronenstern oder explodiert als Supernova.",
        difficulty = 5,
        funFact = "Subrahmanyan Chandrasekhar berechnete diese Grenze 1930 im Alter von 19 Jahren auf dem Schiff von Indien nach England."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der 'Casimir-Effekt'?",
        answerA = "Die Emission von Strahlung durch schwarze Körper bei endlicher Temperatur",
        answerB = "Eine anziehende Kraft zwischen zwei parallelen leitenden Platten im Vakuum durch Quantenvakuumfluktuationen",
        answerC = "Die Änderung der Lichtgeschwindigkeit in verschiedenen Medien",
        answerD = "Ein Resonanzphänomen in Supraleitern",
        correctAnswer = 1,
        explanation = "Der Casimir-Effekt ist eine messbare Kraft zwischen nahen Oberflächen im Vakuum, verursacht durch quantenmechanische Nullpunktsenergie des elektromagnetischen Feldes.",
        difficulty = 5,
        funFact = "Der Effekt wurde 1948 theoretisch vorhergesagt und erst 1997 präzise experimentell bestätigt."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das 'No-Cloning-Theorem' in der Quanteninformation?",
        answerA = "Es ist unmöglich, zwei identische Quantencomputer zu bauen",
        answerB = "Es ist unmöglich, einen unbekannten Quantenzustand perfekt zu kopieren",
        answerC = "Quantensysteme können nicht von klassischen Systemen simuliert werden",
        answerD = "Verschränkte Teilchen können keine Informationen übertragen",
        correctAnswer = 1,
        explanation = "Das No-Cloning-Theorem (Wootters & Zurek, 1982) besagt, dass es physikalisch unmöglich ist, eine exakte Kopie eines beliebigen unbekannten Quantenzustands herzustellen.",
        difficulty = 5,
        funFact = "Dieses Theorem ist die physikalische Grundlage der Sicherheit von Quantenkryptographie – abgehörte Quanteninformationen hinterlassen immer Spuren."
    )
)
