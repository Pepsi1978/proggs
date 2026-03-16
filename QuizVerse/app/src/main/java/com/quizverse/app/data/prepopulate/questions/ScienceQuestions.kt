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
    ),

    // ── ADDITIONAL EASY (difficulty = 1) — 25 questions ──────────────────────

    Question(
        categoryId = 2,
        questionText = "Was ist die härteste natürliche Substanz auf der Erde?",
        answerA = "Stahl",
        answerB = "Diamant",
        answerC = "Quarz",
        answerD = "Korund",
        correctAnswer = 1,
        explanation = "Diamant ist die härteste natürlich vorkommende Substanz und erreicht die Höchstnote 10 auf der Mohs-Härteskala.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Tier ist das schnellste Landtier der Welt?",
        answerA = "Löwe",
        answerB = "Gepard",
        answerC = "Pronghorn-Antilope",
        answerD = "Greyhound",
        correctAnswer = 1,
        explanation = "Der Gepard kann Geschwindigkeiten von bis zu 120 km/h erreichen und ist damit das schnellste Landtier der Welt.",
        difficulty = 1,
        funFact = "Der Gepard kann diese Höchstgeschwindigkeit jedoch nur für wenige Sekunden aufrechterhalten."
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Gas atmen Menschen hauptsächlich aus?",
        answerA = "Sauerstoff",
        answerB = "Stickstoff",
        answerC = "Kohlendioxid",
        answerD = "Wasserdampf",
        correctAnswer = 2,
        explanation = "Bei der Zellatmung wird Sauerstoff verbraucht und Kohlendioxid (CO₂) produziert, das dann ausgeatmet wird.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Aus wie vielen Atomen besteht ein Wassermolekül?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "1",
        correctAnswer = 1,
        explanation = "Ein Wassermolekül (H₂O) besteht aus drei Atomen: zwei Wasserstoffatomen und einem Sauerstoffatom.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Gefrierpunkt von Wasser bei normalem Luftdruck?",
        answerA = "-10 °C",
        answerB = "10 °C",
        answerC = "5 °C",
        answerD = "0 °C",
        correctAnswer = 3,
        explanation = "Wasser gefriert bei 0 °C (273,15 K) unter Standardbedingungen von 1013 hPa.",
        difficulty = 1,
        funFact = "Reines Wasser kann unter bestimmten Bedingungen bis auf -40 °C unterkühlt werden, ohne zu gefrieren."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Farbe hat das Licht mit der kürzesten Wellenlänge im sichtbaren Spektrum?",
        answerA = "Rot",
        answerB = "Grün",
        answerC = "Violett",
        answerD = "Gelb",
        correctAnswer = 2,
        explanation = "Violettes Licht hat mit etwa 380–420 nm die kürzeste Wellenlänge im sichtbaren Spektrum und damit die höchste Energie.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der größte Knochen im menschlichen Körper?",
        answerA = "Humerus (Oberarmknochen)",
        answerB = "Tibia (Schienbein)",
        answerC = "Femur (Oberschenkelknochen)",
        answerD = "Radius (Speiche)",
        correctAnswer = 2,
        explanation = "Der Femur (Oberschenkelknochen) ist der größte und stärkste Knochen im menschlichen Körper.",
        difficulty = 1,
        funFact = "Der Femur kann das 30-fache des Körpergewichts tragen, bevor er bricht."
    ),

    Question(
        categoryId = 2,
        questionText = "Welcher Planet ist für seine markanten Ringe bekannt?",
        answerA = "Uranus",
        answerB = "Jupiter",
        answerC = "Neptun",
        answerD = "Saturn",
        correctAnswer = 3,
        explanation = "Saturn ist für sein beeindruckendes Ringsystem aus Eis und Gestein bekannt, das mit bloßem Auge (mit einem Teleskop) sichtbar ist.",
        difficulty = 1,
        funFact = "Auch Uranus, Jupiter und Neptun haben Ringe, aber diese sind viel unauffälliger als Saturns."
    ),

    Question(
        categoryId = 2,
        questionText = "Wie nennt man das Bindeglied zwischen zwei Knochen?",
        answerA = "Sehne",
        answerB = "Muskel",
        answerC = "Knorpel",
        answerD = "Band (Ligament)",
        correctAnswer = 3,
        explanation = "Bänder (Ligamente) sind festes Bindegewebe, das Knochen miteinander verbindet und Gelenke stabilisiert.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die chemische Formel von Kohlendioxid?",
        answerA = "CO",
        answerB = "C₂O",
        answerC = "CO₂",
        answerD = "CO₃",
        correctAnswer = 2,
        explanation = "Kohlendioxid besteht aus einem Kohlenstoffatom und zwei Sauerstoffatomen: CO₂.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welches Organ produziert die Galle?",
        answerA = "Niere",
        answerB = "Milz",
        answerC = "Magen",
        answerD = "Leber",
        correctAnswer = 3,
        explanation = "Die Leber produziert Galle, die in der Gallenblase gespeichert und bei der Verdauung von Fetten in den Dünndarm abgegeben wird.",
        difficulty = 1,
        funFact = "Die Leber ist das größte innere Organ des Menschen und hat über 500 verschiedene Funktionen."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist Magnetismus?",
        answerA = "Eine Kraft, die durch elektrische Ladungen entsteht",
        answerB = "Eine Kraft, die zwischen magnetischen Materialien und Magneten wirkt",
        answerC = "Die Anziehung zwischen Atomen gleicher Art",
        answerD = "Eine Wärmekraft in Metallen",
        correctAnswer = 1,
        explanation = "Magnetismus ist eine physikalische Kraft, die von Magneten ausgeübt wird und ferromagnetische Materialien wie Eisen, Nickel und Kobalt anzieht.",
        difficulty = 1,
        funFact = "Magnetismus und Elektrizität sind zwei Aspekte derselben fundamentalen Wechselwirkung — der Elektromagnetismus."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Zellen führen die Photosynthese in Pflanzen durch?",
        answerA = "Wurzelzellen",
        answerB = "Stengelzellen",
        answerC = "Rindenzellen",
        answerD = "Mesophyllzellen",
        correctAnswer = 3,
        explanation = "In den Mesophyllzellen der Blätter liegen Chloroplasten, die das grüne Pigment Chlorophyll enthalten und die Photosynthese durchführen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Mond der Erde?",
        answerA = "Ein Asteroid, der von der Erde eingefangen wurde",
        answerB = "Ein natürlicher Satellit, der die Erde umkreist",
        answerC = "Ein kleiner Planet im Sonnensystem",
        answerD = "Ein Zwergplanet wie Pluto",
        correctAnswer = 1,
        explanation = "Der Mond ist der einzige natürliche Satellit der Erde und umkreist sie in etwa 27,3 Tagen.",
        difficulty = 1,
        funFact = "Der Mond entfernt sich jährlich um etwa 3,8 cm von der Erde."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist Elektrizität?",
        answerA = "Die Bewegung von Atomen",
        answerB = "Der Fluss von Wärme durch Metalle",
        answerC = "Der Fluss von elektrischen Ladungen (meist Elektronen)",
        answerD = "Die Ausstrahlung von Licht durch heiße Körper",
        correctAnswer = 2,
        explanation = "Elektrizität ist der gerichtete Fluss elektrischer Ladungsträger (meistens Elektronen) durch einen Leiter.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Art von Energie hat ein rollender Ball?",
        answerA = "Potenzielle Energie",
        answerB = "Thermische Energie",
        answerC = "Chemische Energie",
        answerD = "Kinetische Energie",
        correctAnswer = 3,
        explanation = "Kinetische Energie ist Bewegungsenergie. Jeder bewegte Körper besitzt kinetische Energie: E_kin = ½ · m · v².",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Name des Prozesses, bei dem ein Feststoff direkt in Gas übergeht?",
        answerA = "Kondensation",
        answerB = "Sublimation",
        answerC = "Verdampfung",
        answerD = "Schmelzen",
        correctAnswer = 1,
        explanation = "Sublimation ist der direkte Phasenübergang vom festen in den gasförmigen Zustand, ohne flüssige Phase.",
        difficulty = 1,
        funFact = "Trockeneis (festes CO₂) sublimiert bei Raumtemperatur direkt zu CO₂-Gas — daher der Nebel-Effekt."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist DNS (DNA)?",
        answerA = "Ein Hormon, das im Blut transportiert wird",
        answerB = "Ein Enzym, das Proteine abbaut",
        answerC = "Der genetische Träger der Erbinformation in lebenden Zellen",
        answerD = "Ein Zellkernprotein ohne genetische Funktion",
        correctAnswer = 2,
        explanation = "DNA (Desoxyribonukleinsäure) ist das Molekül, das die genetische Information aller lebenden Organismen trägt und von Generation zu Generation weitergegeben wird.",
        difficulty = 1,
        funFact = "Würde man die DNA aller Zellen eines Menschen aneinanderlegen, ergäbe das eine Strecke von etwa 200 Milliarden Kilometern."
    ),

    Question(
        categoryId = 2,
        questionText = "Wie nennt man einen Körper, der kein Licht reflektiert und theoretisch alle Strahlung absorbiert?",
        answerA = "Spiegel",
        answerB = "Schwarzer Körper (Schwarzer Strahler)",
        answerC = "Weißer Körper",
        answerD = "Opaker Körper",
        correctAnswer = 1,
        explanation = "Ein schwarzer Körper (Schwarzkörperstrahler) absorbiert vollständig alle auftreffende elektromagnetische Strahlung und ist ein ideales Modell in der Physik.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einem Virus und einem Bakterium?",
        answerA = "Viren sind größer als Bakterien",
        answerB = "Viren haben keinen eigenen Stoffwechsel und benötigen Wirtszellen",
        answerC = "Bakterien können keine Krankheiten verursachen",
        answerD = "Viren sind Zellen, Bakterien sind keine",
        correctAnswer = 1,
        explanation = "Bakterien sind einzellige Lebewesen mit eigenem Stoffwechsel. Viren sind keine Zellen, haben keine eigene Energiegewinnung und können sich nur in Wirtszellen vermehren.",
        difficulty = 1,
        funFact = "Viren sind etwa 10-100 Mal kleiner als Bakterien."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein Atom?",
        answerA = "Das kleinste Teilchen einer chemischen Verbindung",
        answerB = "Die kleinste unteilbare Einheit der Materie (Grundbaustein eines Elements)",
        answerC = "Ein Molekül aus zwei gleichen Elementen",
        answerD = "Ein geladenes Teilchen im Atomkern",
        correctAnswer = 1,
        explanation = "Ein Atom ist die kleinste Einheit eines chemischen Elements, die noch dessen chemische Eigenschaften besitzt. Es besteht aus Kern (Protonen + Neutronen) und Elektronenhülle.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Blutgefäße transportieren Blut direkt zum Herzen zurück?",
        answerA = "Arterien",
        answerB = "Kapillaren",
        answerC = "Lymphgefäße",
        answerD = "Venen",
        correctAnswer = 3,
        explanation = "Venen führen Blut aus dem Körpergewebe zurück zum Herzen. Sie haben dünnere Wände als Arterien und Venenklappen, die ein Zurückfließen des Blutes verhindern.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was misst ein Barometer?",
        answerA = "Temperatur",
        answerB = "Luftfeuchtigkeit",
        answerC = "Luftdruck",
        answerD = "Windgeschwindigkeit",
        correctAnswer = 2,
        explanation = "Ein Barometer misst den Luftdruck (atmosphärischen Druck), der in Hektopascal (hPa) oder Millibar (mbar) angegeben wird.",
        difficulty = 1,
        funFact = "Das erste Quecksilberbarometer wurde 1643 von Evangelista Torricelli erfunden."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Farbe entsteht, wenn man rotes und blaues Licht mischt?",
        answerA = "Grün",
        answerB = "Orange",
        answerC = "Gelb",
        answerD = "Magenta",
        correctAnswer = 3,
        explanation = "Bei der additiven Farbmischung (Licht) ergibt Rot + Blau = Magenta (Pink).",
        difficulty = 1,
        funFact = "Bei der subtraktiven Farbmischung (Farben/Pigmente) ergibt Rot + Blau = Violett/Lila."
    ),

    Question(
        categoryId = 2,
        questionText = "Wie heißt das Phänomen, wenn der Mond die Sonne vollständig bedeckt?",
        answerA = "Mondfinsternis",
        answerB = "Transit",
        answerC = "Totale Sonnenfinsternis",
        answerD = "Bedeckung",
        correctAnswer = 2,
        explanation = "Eine totale Sonnenfinsternis tritt auf, wenn der Mond genau zwischen Erde und Sonne steht und die Sonne vollständig verdeckt.",
        difficulty = 1,
        funFact = "Eine totale Sonnenfinsternis dauert an einem bestimmten Ort maximal etwa 7,5 Minuten."
    ),

    // ── ADDITIONAL MEDIUM (difficulty = 2) — 15 questions ────────────────────

    Question(
        categoryId = 2,
        questionText = "Was ist der photoelektrische Effekt?",
        answerA = "Die Lichtemission von erhitzten Metallen",
        answerB = "Die Erzeugung elektrischen Stroms durch Licht in Solarzellen",
        answerC = "Die Emission von Elektronen aus einer Metalloberfläche bei Bestrahlung mit Licht",
        answerD = "Die Reflexion von Licht an Metalloberflächen",
        correctAnswer = 2,
        explanation = "Beim photoelektrischen Effekt löst Licht Elektronen aus Metalloberflächen. Einstein erklärte ihn 1905 mit Photonen und gewann dafür den Nobelpreis.",
        difficulty = 2,
        funFact = "Einstein erhielt den Nobelpreis nicht für die Relativitätstheorie, sondern für die Erklärung des photoelektrischen Effekts."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist eine Supernova?",
        answerA = "Ein neu entstehender Stern in einem Nebel",
        answerB = "Die explosionsartige Endphase eines massereichen Sterns",
        answerC = "Ein besonders heller Pulsar",
        answerD = "Eine Kollision zweier Neutronensterne",
        correctAnswer = 1,
        explanation = "Eine Supernova ist die explosionsartige Endphase eines massereichen Sterns (>8 Sonnenmassen). Dabei wird in Sekunden mehr Energie freigesetzt als unsere Sonne in ihrer gesamten Lebenszeit.",
        difficulty = 2,
        funFact = "Supernovae sind so hell, dass sie tagsüber sichtbar sein können — die letzte mit bloßem Auge sichtbare ereignete sich 1987 in der Großen Magellanschen Wolke."
    ),

    Question(
        categoryId = 2,
        questionText = "Was versteht man unter Elektronegatigvität nach Pauling?",
        answerA = "Die Anzahl der Elektronen in der äußersten Schale",
        answerB = "Das Maß, wie stark ein Atom Elektronen in einer Bindung anzieht",
        answerC = "Die negative Ladung eines Ions",
        answerD = "Die Bindungsenergie kovalenter Bindungen",
        correctAnswer = 1,
        explanation = "Die Pauling-Skala bewertet die relative Fähigkeit eines Atoms, Elektronen in einer kovalenten Bindung anzuziehen. Fluor hat den höchsten Wert (3,98), Cäsium den niedrigsten (0,79).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Struktur hat das Kollagen im menschlichen Körper?",
        answerA = "Eine kugelige (globuläre) Proteinstruktur",
        answerB = "Eine Triple-Helix-Struktur aus drei Polypeptidketten",
        answerC = "Eine flächige Beta-Faltblatt-Struktur",
        answerD = "Eine einfache Alpha-Helix",
        correctAnswer = 1,
        explanation = "Kollagen besteht aus drei Polypeptidketten, die sich zu einer charakteristischen Triple-Helix zusammenlagern. Es ist das häufigste Protein im menschlichen Körper.",
        difficulty = 2,
        funFact = "Kollagen macht etwa 30 % aller Körperproteine aus und bildet die Grundstruktur von Haut, Knochen, Sehnen und Knorpel."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist Biolumineszenz?",
        answerA = "Die Fluoreszenz von Pflanzen unter UV-Licht",
        answerB = "Die Fähigkeit von Lebewesen, Licht durch chemische Reaktionen selbst zu erzeugen",
        answerC = "Die Reflektion von Mondlicht durch Tiefseeorganismen",
        answerD = "Die Absorption von Sonnenlicht durch Algen",
        correctAnswer = 1,
        explanation = "Biolumineszenz ist die Lichterzeugung durch lebende Organismen mittels chemischer Reaktionen. Beispiele: Glühwürmchen, Tiefseefische, Leuchtbakterien.",
        difficulty = 2,
        funFact = "Über 90 % aller Tiefseeorganismen sind biolumineszent."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein Elektromagnet?",
        answerA = "Ein permanent magnetisches Material wie Eisen",
        answerB = "Ein Magnet, der durch elektrischen Strom erzeugt wird",
        answerC = "Ein Gerät zur Messung von Magnetfeldern",
        answerD = "Ein Material, das elektrische Ladungen speichert",
        correctAnswer = 1,
        explanation = "Ein Elektromagnet entsteht, wenn Strom durch eine Spule (Drahtwicklung) fließt. Das erzeugte Magnetfeld kann durch Änderung des Stroms reguliert oder abgeschaltet werden.",
        difficulty = 2,
        funFact = "MRT-Geräte nutzen supraleitende Elektromagnete, die Magnetfelder erzeugen, die bis zu 60.000 Mal stärker als das Erdmagnetfeld sind."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Konzept der 'Biodiversität'?",
        answerA = "Die Gesamtbiomasse aller Lebewesen auf der Erde",
        answerB = "Die Vielfalt der Arten, Gene und Ökosysteme in einem Gebiet",
        answerC = "Die Anzahl der Individuen einer Art in einer Region",
        answerD = "Die Anpassungsfähigkeit von Organismen an neue Umgebungen",
        correctAnswer = 1,
        explanation = "Biodiversität beschreibt die Vielfalt auf drei Ebenen: genetische Vielfalt innerhalb einer Art, Artenvielfalt und Ökosystemvielfalt.",
        difficulty = 2,
        funFact = "Schätzungsweise existieren 8,7 Millionen Tier- und Pflanzenarten — davon sind bisher etwa 1,9 Millionen wissenschaftlich beschrieben."
    ),

    Question(
        categoryId = 2,
        questionText = "Welche Wellenlänge haben Radiowellen im elektromagnetischen Spektrum?",
        answerA = "Nanometer-Bereich (10⁻⁹ m)",
        answerB = "Mikrometer-Bereich (10⁻⁶ m)",
        answerC = "Millimeter-Bereich (10⁻³ m)",
        answerD = "Meter bis Kilometer-Bereich (>10⁻¹ m)",
        correctAnswer = 3,
        explanation = "Radiowellen haben Wellenlängen von einigen Millimetern bis zu mehreren Kilometern — damit die längsten Wellenlängen im elektromagnetischen Spektrum.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen 'Masse' und 'Gewicht'?",
        answerA = "Masse und Gewicht sind dasselbe, nur in verschiedenen Einheiten",
        answerB = "Masse ist eine unveränderliche Eigenschaft der Materie, Gewicht ist die Gravitationskraft auf diese Masse",
        answerC = "Gewicht ist die unveränderliche Eigenschaft, Masse hängt von der Schwerkraft ab",
        answerD = "Masse gilt für Feststoffe, Gewicht für Flüssigkeiten",
        correctAnswer = 1,
        explanation = "Masse (kg) ist eine intrinsische Eigenschaft und überall gleich. Gewicht (N) ist die Kraft, mit der die Gravitation auf eine Masse wirkt: G = m × g. Auf dem Mond wiegt man weniger, hat aber dieselbe Masse.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Rolle der Leukozyten im Immunsystem?",
        answerA = "Sie transportieren Sauerstoff zu den Geweben",
        answerB = "Sie produzieren Hämoglobin für rote Blutkörperchen",
        answerC = "Sie erkennen und bekämpfen Krankheitserreger sowie fremde Zellen",
        answerD = "Sie regulieren den Blutdruck",
        correctAnswer = 2,
        explanation = "Leukozyten (weiße Blutkörperchen) sind die zentralen Zellen des Immunsystems. Verschiedene Subtypen (Neutrophile, Lymphozyten, Makrophagen) haben spezifische Abwehrfunktionen.",
        difficulty = 2,
        funFact = "Im gesunden Menschen zirkulieren etwa 5.000 bis 10.000 Leukozyten pro Mikroliter Blut."
    ),

    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem 'Doppler-Effekt'?",
        answerA = "Die Brechung von Schall an Wänden",
        answerB = "Die Änderung der wahrgenommenen Frequenz von Wellen, wenn Quelle oder Beobachter sich bewegen",
        answerC = "Die Verstärkung von Schall in engen Räumen",
        answerD = "Die Absorption von Schallwellen durch Luft",
        correctAnswer = 1,
        explanation = "Der Doppler-Effekt beschreibt, wie die wahrgenommene Frequenz einer Welle zunimmt, wenn sich Quelle und Beobachter annähern, und abnimmt, wenn sie sich entfernen.",
        difficulty = 2,
        funFact = "Astronomen nutzen den Doppler-Effekt bei Licht (Rotverschiebung), um die Bewegungsgeschwindigkeit und Entfernung von Galaxien zu messen."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist das Ohmsche Gesetz?",
        answerA = "Spannung ist gleich Strom durch Widerstand: U = I / R",
        answerB = "Widerstand ist gleich Strom mal Spannung: R = I × U",
        answerC = "Spannung ist gleich Strom mal Widerstand: U = I × R",
        answerD = "Strom ist gleich Spannung mal Widerstand: I = U × R",
        correctAnswer = 2,
        explanation = "Das Ohmsche Gesetz lautet U = I × R (Spannung = Strom × Widerstand). Es gilt für ohmsche Widerstände bei konstanter Temperatur.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen Konkav- und Konvexlinse?",
        answerA = "Konkavlinsen bündeln Licht, Konvexlinsen streuen es",
        answerB = "Konkavlinsen streuen Licht, Konvexlinsen bündeln es in einem Brennpunkt",
        answerC = "Beide Linsenarten haben dieselbe optische Wirkung",
        answerD = "Konvexlinsen beugen Licht, Konkavlinsen nicht",
        correctAnswer = 1,
        explanation = "Eine Konvexlinse (Sammellinse) bündelt parallele Lichtstrahlen in einem Brennpunkt. Eine Konkavlinse (Zerstreuungslinse) streut Licht, sodass es von einem virtuellen Brennpunkt wegzugehen scheint.",
        difficulty = 2,
        funFact = "Brillengläser für Kurzsichtige sind Konkavlinsen, für Weitsichtige Konvexlinsen."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist die Bedeutung des Begriffs 'Homöostase' in der Biologie?",
        answerA = "Die Fähigkeit eines Organismus, konstante innere Bedingungen aufrechtzuerhalten",
        answerB = "Die Anpassung eines Organismus an neue Umweltbedingungen",
        answerC = "Das Gleichgewicht zwischen verschiedenen Tierarten in einem Ökosystem",
        answerD = "Die gleiche genetische Ausstattung aller Zellen eines Organismus",
        correctAnswer = 0,
        explanation = "Homöostase ist das Regulationsprinzip, mit dem Lebewesen innere Zustände wie Körpertemperatur, pH-Wert oder Blutzucker in engen Grenzen konstant halten.",
        difficulty = 2,
        funFact = "Die Körpertemperatur des Menschen wird auf ±0,5 °C um 37 °C reguliert — schon 2 °C Abweichung kann lebensbedrohlich sein."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein chemisches Mol?",
        answerA = "Eine Masse von genau 1 Gramm eines Elements",
        answerB = "Eine Menge von 10²³ Teilchen",
        answerC = "Eine Menge eines Stoffes, die 6,022 × 10²³ Teilchen enthält (Avogadro-Konstante)",
        answerD = "Die kleinste messbare Menge einer Substanz",
        correctAnswer = 2,
        explanation = "Ein Mol ist die SI-Einheit der Stoffmenge und enthält exakt 6,022 × 10²³ Teilchen (Avogadro-Konstante). Ein Mol Wasser (18 g) enthält ebenso viele Moleküle wie ein Mol Eisen (56 g) Atome.",
        difficulty = 2,
        funFact = null
    ),

    // ── ADDITIONAL HARD (difficulty = 3) — 23 questions ──────────────────────

    Question(
        categoryId = 2,
        questionText = "Was ist das Meißner-Ochsenfeld-Experiment?",
        answerA = "Der Nachweis der Quantenstruktur von Elektronen",
        answerB = "Die Beobachtung, dass Supraleiter Magnetfelder aus ihrem Inneren verdrängen",
        answerC = "Die erste Messung des Photoelektrischen Effekts",
        answerD = "Ein Experiment zur Bestimmung der Elektronenmasse",
        correctAnswer = 1,
        explanation = "Der Meißner-Ochsenfeld-Effekt (1933) zeigt, dass Supraleiter im supraleitenden Zustand Magnetfelder aus ihrem Inneren verdrängen (perfekter Diamagnetismus).",
        difficulty = 3,
        funFact = "Dieser Effekt ermöglicht die Levitation: supraleitende Magnetschwebebahnen nutzen dieses Prinzip."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein Quasar?",
        answerA = "Ein super-massereicher Planet am Rand des Sonnensystems",
        answerB = "Ein extrem leuchtkräftiger Kern einer aktiven Galaxie, angetrieben von einem supermassiven schwarzen Loch",
        answerC = "Ein explodierender Riesenstern in der Milchstraße",
        answerD = "Eine sehr dichte Ansammlung von Neutronensternen",
        correctAnswer = 1,
        explanation = "Quasare (Quasi-Stellar Objects) sind die leuchtkräftigsten Objekte im Universum. Sie sind aktive Galaxienkerne, in denen Materie von einem supermassiven schwarzen Loch akkretiert wird.",
        difficulty = 3,
        funFact = "Der leuchtkräftigste bekannte Quasar, J0529-4351, ist 500 Billionen Mal so hell wie unsere Sonne."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt die 'Entropie' in der Thermodynamik?",
        answerA = "Die Gesamtenergie eines Systems",
        answerB = "Das Maß für die Unordnung oder die Anzahl der möglichen Mikrozustände eines Systems",
        answerC = "Die Wärmekapazität eines Materials",
        answerD = "Der Energieverlust bei thermischen Prozessen",
        correctAnswer = 1,
        explanation = "Entropie (S) ist ein Maß für die Unordnung oder die Anzahl der möglichen Mikrozustände eines Systems. Der zweite Hauptsatz der Thermodynamik besagt: Die Entropie eines abgeschlossenen Systems nimmt niemals ab.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist eine 'Transgene Pflanze'?",
        answerA = "Eine Pflanze, die durch natürliche Kreuzung entstanden ist",
        answerB = "Eine Pflanze, der Gene einer anderen Art eingebaut wurden",
        answerC = "Eine Pflanze mit besonders vielen Chromosomen",
        answerD = "Eine Pflanze, die ohne Bestäubung Früchte bildet",
        correctAnswer = 1,
        explanation = "Transgene Pflanzen enthalten Gene, die aus anderen Organismen (auch anderen Arten) eingebracht wurden, um bestimmte Eigenschaften zu verleihen (z. B. Herbizidresistenz, Schädlingsresistenz).",
        difficulty = 3,
        funFact = "Bt-Mais ist eine transgene Pflanze, die das Insektizidprotein aus Bacillus thuringiensis produziert."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der 'Hall-Effekt' in der Physik?",
        answerA = "Die Entstehung von Wirbelströmen in Metallen bei wechselnden Magnetfeldern",
        answerB = "Die Erzeugung einer elektrischen Spannung quer zur Stromflussrichtung in einem Magnetfeld",
        answerC = "Die Absorption von Licht durch freie Elektronen in Metallen",
        answerD = "Die Veränderung des elektrischen Widerstands durch Dehnung",
        correctAnswer = 1,
        explanation = "Der Hall-Effekt (1879, Edwin Hall): In einem Leiter mit Stromfluss wird durch ein senkrechtes Magnetfeld eine Spannung quer zur Stromrichtung erzeugt. Er wird u. a. in Sensoren verwendet.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist 'alternatives Splicing' in der Molekularbiologie?",
        answerA = "Die Spaltung der DNA durch Restriktionsenzyme",
        answerB = "Der Prozess, bei dem aus einem Gen durch unterschiedliche mRNA-Prozessierung mehrere Proteine entstehen können",
        answerC = "Die Fusion von zwei mRNA-Molekülen zu einer neuen mRNA",
        answerD = "Der Export von mRNA aus dem Zellkern",
        correctAnswer = 1,
        explanation = "Beim alternativen Splicing werden Exons einer prä-mRNA in verschiedenen Kombinationen zusammengefügt. So können aus einem einzigen Gen viele verschiedene Protein-Isoformen entstehen.",
        difficulty = 3,
        funFact = "Das Dscam-Gen der Fruchtfliege kann durch alternatives Splicing theoretisch über 38.000 verschiedene Proteinisoformen produzieren."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein Tachyon?",
        answerA = "Ein Elementarteilchen mit negativer Masse",
        answerB = "Ein hypothetisches Teilchen, das sich immer schneller als Licht bewegt",
        answerC = "Ein Antiteilchen des Neutrons",
        answerD = "Ein schwach wechselwirkendes massives Teilchen (WIMP)",
        correctAnswer = 1,
        explanation = "Tachyonen sind hypothetische Teilchen, die sich immer schneller als Licht bewegen würden. Ihre Existenz würde die Kausalität verletzen — bisher wurden sie nie nachgewiesen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt das 'Fick'sche Diffusionsgesetz'?",
        answerA = "Den osmotischen Druck in biologischen Membranen",
        answerB = "Den Zusammenhang zwischen Diffusionsstrom, Konzentrationsgradient und Diffusionskoeffizient",
        answerC = "Die Viskosität von Flüssigkeiten bei verschiedenen Temperaturen",
        answerD = "Die Kapillarwirkung von Flüssigkeiten in engen Röhren",
        correctAnswer = 1,
        explanation = "Das erste Fick'sche Gesetz: J = -D · (dC/dx). Der Diffusionsfluss J ist proportional zum Konzentrationsgradient und dem Diffusionskoeffizienten D.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist 'Krebs als klonale Evolution'?",
        answerA = "Krebs entsteht immer durch Viren, die Zellen infizieren",
        answerB = "Tumorzellen entstehen durch akkumulierende Mutationen und selektieren sich nach darwinschen Prinzipien",
        answerC = "Krebszellen klonen sich aus normalen Stammzellen ohne Mutationen",
        answerD = "Krebserkrankungen werden immer von einer einzelnen Ursprungszelle gestartet, die identische Tochterzellen erzeugt",
        correctAnswer = 1,
        explanation = "Die klonale Evolution erklärt Krebsentstehung: Mutationen verleihen Zellen Wachstumsvorteile. Durch natürliche Selektion setzen sich Zellen mit weiteren Mutationen durch — bis invasives Verhalten entsteht.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein 'Schwarzes Loch vom Typ Kerr'?",
        answerA = "Ein Schwarzes Loch, das aus reiner Energie besteht",
        answerB = "Ein rotierendes Schwarzes Loch, das durch den Kerr-Metriktensor beschrieben wird",
        answerC = "Ein Schwarzes Loch mit elektrischer Ladung",
        answerD = "Ein Mikro-Schwarzes Loch, das durch Quanteneffekte entsteht",
        correctAnswer = 1,
        explanation = "Das Kerr-Schwarze-Loch ist eine exakte Lösung der Einsteinschen Feldgleichungen für rotierende Schwarze Löcher (Roy Kerr, 1963). Es besitzt einen Ergosphärenbereich.",
        difficulty = 3,
        funFact = "Die meisten astrophysikalischen Schwarzen Löcher rotieren und sind daher Kerr-Schwarze-Löcher."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist 'Proteomik'?",
        answerA = "Die Erforschung der genetischen Ursachen von Proteinkrankheiten",
        answerB = "Die umfassende Untersuchung aller Proteine (Proteom) einer Zelle, eines Gewebes oder Organismus",
        answerC = "Die Synthese von Proteinen im Labor durch chemische Methoden",
        answerD = "Die Erforschung von Proteinen ausgestorbener Organismen",
        correctAnswer = 1,
        explanation = "Die Proteomik analysiert das gesamte Proteom — alle Proteine, die in einer Zelle oder einem Organismus zu einem bestimmten Zeitpunkt exprimiert werden.",
        difficulty = 3,
        funFact = "Das menschliche Proteom umfasst schätzungsweise über 1 Million Proteinvarianten, obwohl das Genom nur etwa 20.000 Gene enthält."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der 'Spin' eines Elementarteilchens?",
        answerA = "Die Rotationsgeschwindigkeit des Teilchens um seine eigene Achse",
        answerB = "Eine intrinsische quantenmechanische Eigenschaft, die analog zum Drehimpuls ist",
        answerC = "Die Kreiselbewegung eines Teilchens im Magnetfeld",
        answerD = "Die Eigenbewegung eines Teilchens durch den Raum",
        correctAnswer = 1,
        explanation = "Spin ist eine fundamentale quantenmechanische Eigenschaft ohne klassische Entsprechung. Er kann nur bestimmte diskrete Werte annehmen (z. B. ±1/2 für Elektronen).",
        difficulty = 3,
        funFact = "Der Spin des Elektrons bestimmt die magnetischen Eigenschaften von Atomen und ist die Grundlage der MRT-Diagnostik."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist das 'Endoplasmatische Retikulum' (ER)?",
        answerA = "Ein Netzwerk aus Membranen im Zellkern für die DNA-Replikation",
        answerB = "Ein ausgedehntes Membransystem im Zytoplasma, das an Proteinsynthese und Lipidstoffwechsel beteiligt ist",
        answerC = "Die äußere Membran von Mitochondrien",
        answerD = "Das Netzwerk der Mikrotubuli im Zytoskelett",
        correctAnswer = 1,
        explanation = "Das ER ist ein Membransystem im Zytoplasma. Das raue ER (mit Ribosomen) ist an der Proteinsynthese und -modifikation beteiligt; das glatte ER am Lipidstoffwechsel.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist der 'Compton-Effekt'?",
        answerA = "Die Emission von Elektronen aus Metallen durch UV-Licht",
        answerB = "Die Streuung von Röntgenphotonen an freien Elektronen, wobei die Wellenlänge zunimmt",
        answerC = "Die Beugung von Licht an kleinen Teilchen",
        answerD = "Die Absorption von Röntgenstrahlen durch schwere Atome",
        correctAnswer = 1,
        explanation = "Der Compton-Effekt (1923): Wenn Röntgenphotonen an (quasi-)freien Elektronen gestreut werden, übertragen sie Impuls auf die Elektronen und ihre Wellenlänge nimmt zu. Dies bewies die Teilchennatur des Lichts.",
        difficulty = 3,
        funFact = "Arthur Holly Compton erhielt dafür 1927 den Nobelpreis für Physik."
    ),

    Question(
        categoryId = 2,
        questionText = "Was versteht man unter 'epigenetischem Silencing'?",
        answerA = "Die Entfernung von Genen aus dem Chromosom",
        answerB = "Die dauerhafte Stummstellung eines Gens durch chemische Modifikation, ohne die DNA-Sequenz zu verändern",
        answerC = "Die Hemmung der Transkription durch Antisense-RNA",
        answerD = "Die Spaltung eines Gens durch Restriktionsenzyme",
        correctAnswer = 1,
        explanation = "Epigenetisches Silencing schaltet Gene ab durch DNA-Methylierung, Histonmodifikation oder andere Chromatin-Veränderungen — ohne die DNA-Sequenz zu verändern.",
        difficulty = 3,
        funFact = "Etwa die Hälfte aller menschlichen Gene kann epigenetisch reguliert werden."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist das 'Pairing-Problem' in der Kernphysik?",
        answerA = "Die Schwierigkeit, Quarks in Protonen voneinander zu trennen",
        answerB = "Das Phänomen, dass Nukleonen (Protonen, Neutronen) eine erhöhte Stabilität zeigen, wenn sie paarweise mit entgegengesetztem Spin vorliegen",
        answerC = "Die Massendifferenz zwischen Proton und Neutron",
        answerD = "Die Wechselwirkung zwischen Kernspin und externen Magnetfeldern",
        correctAnswer = 1,
        explanation = "Das Paarungseffekt-Konzept erklärt, warum Kerne mit gerader Anzahl von Protonen und Neutronen stabiler sind als Kerne mit ungerader Zahl — Nukleonen paaren sich bevorzugt mit entgegengesetztem Spin.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist 'Astrobiologie'?",
        answerA = "Die Astronomie von biologischen Observatorien aus",
        answerB = "Die Wissenschaft, die die Entstehung, Verteilung und Zukunft des Lebens im Universum erforscht",
        answerC = "Die Biologie von Organismen unter Weltraumbedingungen an Bord der ISS",
        answerD = "Die Erforschung kosmischer Einflüsse auf das Leben auf der Erde",
        correctAnswer = 1,
        explanation = "Astrobiologie ist ein interdisziplinäres Forschungsfeld, das untersucht, wie Leben im Universum entstand, wo es existieren könnte und wie es sich entwickelt.",
        difficulty = 3,
        funFact = "Der Mond des Jupiters, Europa, gilt als einer der vielversprechendsten Kandidaten für außerirdisches Leben wegen seines Ozeans unter der Eisdecke."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein 'Topoisomerase'?",
        answerA = "Ein Protein, das die Chromosomen in der Zellkernmembran verankert",
        answerB = "Ein Enzym, das Über- oder Unterwindungen in der DNA durch vorübergehendes Schneiden und Wieder-Verknüpfen beseitigt",
        answerC = "Ein Protein, das die räumliche Struktur von Ribosomen stabilisiert",
        answerD = "Ein Enzym, das DNA-Methylierungen hinzufügt oder entfernt",
        correctAnswer = 1,
        explanation = "Topoisomerasen lösen topologische Spannungen in der DNA-Doppelhelix, die bei Replikation und Transkription entstehen. Typ I schneidet einen Strang, Typ II schneidet beide Stränge.",
        difficulty = 3,
        funFact = "Viele Krebstherapeutika (z. B. Irinotecan, Etoposid) hemmen Topoisomerasen, um die DNA-Replikation von Tumorzellen zu blockieren."
    ),

    Question(
        categoryId = 2,
        questionText = "Was beschreibt die 'Virial-Theorem' in der Astrophysik?",
        answerA = "Die Beziehung zwischen Masse und Leuchtkraft von Sternen",
        answerB = "Das Gleichgewicht zwischen kinetischer und potentieller Energie in gravitationsgebundenen Systemen",
        answerC = "Die maximale Rotationsgeschwindigkeit einer Galaxie",
        answerD = "Die Beziehung zwischen Temperatur und Radius eines Sterns",
        correctAnswer = 1,
        explanation = "Das Virial-Theorem: In einem gebundenen System im Gleichgewicht gilt ⟨E_kin⟩ = -½ ⟨E_pot⟩. Es wird verwendet, um Massen von Galaxienhaufen aus deren kinetischen Energien zu schätzen.",
        difficulty = 3,
        funFact = "Über das Virial-Theorem wurde erstmals dunkle Materie postuliert, da Galaxienhaufen mehr Masse besitzen als durch sichtbare Materie erklärbar."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist ein 'Enzymkomplex' in der Biochemie?",
        answerA = "Ein Enzym, das nur bei hohen pH-Werten aktiv ist",
        answerB = "Eine Anordnung mehrerer Enzymuntereinheiten oder verschiedener Enzyme, die zusammen eine Reaktionsfolge katalysieren",
        answerC = "Ein Enzym, das durch Metallionen aktiviert wird",
        answerD = "Eine Mischung aus Enzymen und Cofaktoren in einer Lösung",
        correctAnswer = 1,
        explanation = "Enzymkomplexe (Multienzymkomplexe) sind Anordnungen mehrerer Enzyme, die hintereinander verbundene Reaktionen durchführen, z. B. der Pyruvat-Dehydrogenase-Komplex.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist das 'Pauli-Ausschlussprinzip' in Bezug auf chemische Bindungen?",
        answerA = "Es erklärt, warum Atome nur bestimmte Elektronenkonfigurationen haben können",
        answerB = "Es besagt, dass zwei Elektronen in einem Orbital entgegengesetzten Spin haben müssen",
        answerC = "Es erklärt, warum Metalle elektrischen Strom leiten",
        answerD = "Es begrenzt die maximale Bindungsordnung zwischen zwei Atomen",
        correctAnswer = 1,
        explanation = "Das Pauli-Prinzip erzwingt, dass zwei Elektronen in einem Orbital (gleiche Quantenzahlen n, l, m) entgegengesetzten Spin (±½) haben müssen. Dies bestimmt die Elektronenkonfiguration und Struktur des Periodensystems.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist 'SETI'?",
        answerA = "Eine internationale Weltraumorganisation neben der NASA",
        answerB = "Die wissenschaftliche Suche nach außerirdischer Intelligenz durch Analyse von Radiosignalen und anderen Emissionen",
        answerC = "Ein Satellit zur Erforschung des äußeren Sonnensystems",
        answerD = "Ein Forschungsprojekt zur Untersuchung von Meteoriten auf organische Verbindungen",
        correctAnswer = 1,
        explanation = "SETI (Search for Extraterrestrial Intelligence) ist ein Forschungsprogramm, das nach Anzeichen technologischer Zivilisationen im Universum sucht, hauptsächlich durch die Analyse von Radiosignalen.",
        difficulty = 3,
        funFact = "Seit 1999 können Privatpersonen über SETI@home Rechenkapazität zur Datenanalyse beitragen."
    ),

    Question(
        categoryId = 2,
        questionText = "Was ist das 'Elektronen-Spin-Resonanz' (ESR)?",
        answerA = "Eine Methode zur Messung der Kernladungszahl von Atomen",
        answerB = "Eine spektroskopische Methode, die paramagnetische Substanzen durch Wechselwirkung ihrer ungepaarten Elektronen mit Mikrowellenstrahlung in einem Magnetfeld analysiert",
        answerC = "Eine Methode zur Messung von Elektronengeschwindigkeiten im elektrischen Feld",
        answerD = "Eine Technik zur Bestimmung der Elektronenkonfiguration durch UV-Absorption",
        correctAnswer = 1,
        explanation = "ESR (oder EPR: Electron Paramagnetic Resonance) ist eine Spektroskopietechnik, die ungepaarte Elektronen in Molekülen nachweist. Sie wird u. a. in der Materialforschung und zur Altersbestimmung genutzt.",
        difficulty = 3,
        funFact = null
    )
)
