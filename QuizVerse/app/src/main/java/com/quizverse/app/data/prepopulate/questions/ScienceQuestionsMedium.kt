package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsMedium(): List<Question> = listOf(

    // Question 1 – Organic Chemistry
    Question(
        categoryId = 2,
        questionText = "Welche funktionelle Gruppe ist charakteristisch für Aldehyde?",
        answerA = "Hydroxylgruppe (-OH)",
        answerB = "Carboxylgruppe (-COOH)",
        answerC = "Formylgruppe (-CHO)",
        answerD = "Aminogruppe (-NH₂)",
        correctAnswer = 2, // C
        explanation = "Aldehyde besitzen die Formylgruppe (-CHO), bei der ein Kohlenstoffatom doppelt mit Sauerstoff und einfach mit einem Wasserstoffatom verbunden ist.",
        difficulty = 2,
        funFact = "Der Name 'Aldehyd' leitet sich vom lateinischen 'alcohol dehydrogenatus' ab, also dehydrierter Alkohol."
    ),

    // Question 2 – Genetics
    Question(
        categoryId = 2,
        questionText = "Welche Art von RNA trägt die genetische Information vom Zellkern zu den Ribosomen?",
        answerA = "ribosomale RNA (rRNA)",
        answerB = "Transfer-RNA (tRNA)",
        answerC = "kleine nukleare RNA (snRNA)",
        answerD = "Boten-RNA (mRNA)",
        correctAnswer = 3, // D
        explanation = "Die Boten-RNA (mRNA) überträgt die genetische Information aus dem Zellkern zu den Ribosomen im Zytoplasma, wo daraus Proteine hergestellt werden.",
        difficulty = 2,
        funFact = null
    ),

    // Question 3 – Physics
    Question(
        categoryId = 2,
        questionText = "Was besagt das erste Newtonsche Gesetz (Trägheitsprinzip)?",
        answerA = "Kraft gleich Masse mal Beschleunigung",
        answerB = "Jede Wirkung hat eine gleich große Gegenwirkung",
        answerC = "Ein Körper verharrt im Ruhezustand oder gleichförmiger Bewegung, solange keine Kraft wirkt",
        answerD = "Die Arbeit ist das Produkt aus Kraft und Weg",
        correctAnswer = 2, // C
        explanation = "Das erste Newtonsche Gesetz beschreibt die Trägheit: Ein Körper bleibt in Ruhe oder bewegt sich geradlinig gleichförmig, solange keine äußere Kraft auf ihn einwirkt.",
        difficulty = 2,
        funFact = null
    ),

    // Question 4 – Astronomy
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter der Astronomischen Einheit (AE)?",
        answerA = "Der mittlere Abstand zwischen Erde und Mond",
        answerB = "Der mittlere Abstand zwischen Erde und Sonne",
        answerC = "Die Lichtjahre bis zur nächsten Galaxie",
        answerD = "Der Durchmesser unserer Milchstraße",
        correctAnswer = 1, // B
        explanation = "Eine Astronomische Einheit (AE) entspricht dem mittleren Abstand zwischen Erde und Sonne, also etwa 149,6 Millionen Kilometern.",
        difficulty = 2,
        funFact = "1 AE entspricht etwa 8 Lichtminuten – so lange braucht Sonnenlicht, um die Erde zu erreichen."
    ),

    // Question 5 – Ecology
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der Begriff 'Ökologische Nische'?",
        answerA = "Den geografischen Lebensraum einer Art",
        answerB = "Die Gesamtheit der Umweltbedingungen, unter denen eine Art überleben kann",
        answerC = "Die Nahrungsbeziehungen in einem Ökosystem",
        answerD = "Den Schutzstatus einer gefährdeten Art",
        correctAnswer = 1, // B
        explanation = "Die ökologische Nische beschreibt die Gesamtheit aller Umweltbedingungen und Ressourcen, die eine Art für ihr Überleben und ihre Reproduktion benötigt – also ihre Rolle im Ökosystem.",
        difficulty = 2,
        funFact = null
    ),

    // Question 6 – Human Body
    Question(
        categoryId = 2,
        questionText = "Welches Hormon wird als Reaktion auf Stress von der Nebennierenrinde ausgeschüttet?",
        answerA = "Insulin",
        answerB = "Melatonin",
        answerC = "Cortisol",
        answerD = "Serotonin",
        correctAnswer = 2, // C
        explanation = "Cortisol ist das wichtigste Stresshormon des menschlichen Körpers. Es wird in der Nebennierenrinde produziert und reguliert den Stoffwechsel sowie die Immunreaktion.",
        difficulty = 2,
        funFact = "Chronisch erhöhte Cortisolspiegel können das Immunsystem schwächen und zu Schlafproblemen führen."
    ),

    // Question 7 – Geology
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen Magma und Lava?",
        answerA = "Magma ist heißer als Lava",
        answerB = "Magma befindet sich unterhalb der Erdoberfläche, Lava an der Oberfläche",
        answerC = "Lava enthält mehr Minerale als Magma",
        answerD = "Magma kommt nur bei Unterwasservulkanen vor",
        correctAnswer = 1, // B
        explanation = "Magma ist geschmolzenes Gestein im Erdinneren. Sobald es durch einen Vulkan an die Erdoberfläche tritt, bezeichnet man es als Lava.",
        difficulty = 2,
        funFact = null
    ),

    // Question 8 – Organic Chemistry
    Question(
        categoryId = 2,
        questionText = "Welche Reaktion beschreibt die Veresterung in der organischen Chemie?",
        answerA = "Reaktion einer Säure mit einem Alkohol unter Wasserabspaltung",
        answerB = "Reaktion zweier Alkohole unter Sauerstoffentwicklung",
        answerC = "Oxidation eines Aldehyds zur Carbonsäure",
        answerD = "Spaltung einer Doppelbindung durch Wasserstoff",
        correctAnswer = 0, // A
        explanation = "Bei der Veresterung reagiert eine Carbonsäure mit einem Alkohol unter Abspaltung von Wasser zu einem Ester. Dies ist eine gleichgewichtsreaktive, umkehrbare Reaktion.",
        difficulty = 2,
        funFact = "Ester sind oft für angenehme Düfte und Aromen verantwortlich – etwa Isoamylacetat riecht nach Banane."
    ),

    // Question 9 – Genetics
    Question(
        categoryId = 2,
        questionText = "Was ist ein dominantes Allel in der Genetik?",
        answerA = "Ein Allel, das nur in homozygoten Organismen exprimiert wird",
        answerB = "Ein Allel, dessen Merkmalsausprägung sich auch in Anwesenheit des anderen Allels durchsetzt",
        answerC = "Ein Allel, das nur auf dem Y-Chromosom vorkommt",
        answerD = "Ein Allel, das durch Mutation entstanden ist",
        correctAnswer = 1, // B
        explanation = "Ein dominantes Allel setzt seine Merkmalsausprägung durch, auch wenn das andere Allel (das rezessive) eine andere Ausprägung kodiert. Bei heterozygoten Organismen ist nur das dominante Merkmal sichtbar.",
        difficulty = 2,
        funFact = null
    ),

    // Question 10 – Physics
    Question(
        categoryId = 2,
        questionText = "Was gibt die spezifische Wärmekapazität einer Substanz an?",
        answerA = "Die Wärmemenge, die nötig ist, um 1 kg einer Substanz um 1 Kelvin zu erwärmen",
        answerB = "Den Schmelzpunkt einer Substanz in Kelvin",
        answerC = "Die maximale Temperatur, die eine Substanz erreichen kann",
        answerD = "Die Wärmeleitfähigkeit eines Materials",
        correctAnswer = 0, // A
        explanation = "Die spezifische Wärmekapazität gibt an, wie viel Energie (in Joule) benötigt wird, um 1 kg einer Substanz um 1 Kelvin (oder 1 °C) zu erwärmen. Wasser hat eine besonders hohe spezifische Wärmekapazität.",
        difficulty = 2,
        funFact = "Wasser hat eine sehr hohe spezifische Wärmekapazität von 4186 J/(kg·K), weshalb Ozeane das Klima ausgleichen."
    ),

    // Question 11 – Astronomy
    Question(
        categoryId = 2,
        questionText = "Was ist ein Lichtjahr?",
        answerA = "Die Zeit, die Licht benötigt, um die Erde einmal zu umrunden",
        answerB = "Die Strecke, die Licht in einem Jahr zurücklegt",
        answerC = "Die Entfernung von der Erde zur Sonne",
        answerD = "Die Helligkeit eines Sterns im Vergleich zur Sonne",
        correctAnswer = 1, // B
        explanation = "Ein Lichtjahr ist eine Entfernungseinheit und beschreibt die Strecke, die Licht in einem Jahr zurücklegt – das sind etwa 9,46 Billionen Kilometer.",
        difficulty = 2,
        funFact = "Der uns nächste Stern, Proxima Centauri, ist etwa 4,24 Lichtjahre entfernt."
    ),

    // Question 12 – Ecology
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem Begriff 'Biomagnifikation'?",
        answerA = "Die Vergrößerung von Organismen unter dem Mikroskop",
        answerB = "Die Zunahme der Konzentration von Schadstoffen in höheren Gliedern der Nahrungskette",
        answerC = "Das Wachstum von Pflanzen durch erhöhten CO₂-Gehalt",
        answerD = "Die Vermehrung von Bakterien in Nährlösung",
        correctAnswer = 1, // B
        explanation = "Bei der Biomagnifikation reichern sich Schadstoffe wie DDT oder Schwermetalle in jedem Schritt der Nahrungskette weiter an, da Tiere die Gifte nicht vollständig ausscheiden können.",
        difficulty = 2,
        funFact = "Bei Adlern und anderen Greifvögeln wurden durch DDT-Biomagnifikation so hohe Konzentrationen gemessen, dass ihre Eierschalen brüchig wurden."
    ),

    // Question 13 – Human Body
    Question(
        categoryId = 2,
        questionText = "Welche Struktur verbindet Muskel und Knochen?",
        answerA = "Ligament",
        answerB = "Sehne",
        answerC = "Knorpel",
        answerD = "Faszien",
        correctAnswer = 1, // B
        explanation = "Sehnen bestehen aus straffem Bindegewebe und verbinden Muskeln mit Knochen. Ligamente hingegen verbinden Knochen miteinander.",
        difficulty = 2,
        funFact = null
    ),

    // Question 14 – Geology
    Question(
        categoryId = 2,
        questionText = "Auf welcher Skala wird die Stärke von Erdbeben üblicherweise gemessen?",
        answerA = "Beaufort-Skala",
        answerB = "Richter-Skala",
        answerC = "Mohs-Skala",
        answerD = "Saffir-Simpson-Skala",
        correctAnswer = 1, // B
        explanation = "Die Richter-Skala (und die modernere Momenten-Magnituden-Skala) misst die Energie eines Erdbebens. Die Skala ist logarithmisch – ein Wert von 7 ist 10-mal stärker als ein Wert von 6.",
        difficulty = 2,
        funFact = "Das stärkste je gemessene Erdbeben hatte die Magnitude 9,5 und ereignete sich 1960 in Chile."
    ),

    // Question 15 – Organic Chemistry
    Question(
        categoryId = 2,
        questionText = "Was sind Isomere in der organischen Chemie?",
        answerA = "Verbindungen mit gleicher Summenformel, aber unterschiedlicher Struktur",
        answerB = "Atome des gleichen Elements mit unterschiedlicher Neutronenzahl",
        answerC = "Moleküle mit unterschiedlicher Summenformel aber gleichen Eigenschaften",
        answerD = "Chemische Verbindungen, die nur in der Natur vorkommen",
        correctAnswer = 0, // A
        explanation = "Isomere haben die gleiche Summenformel (gleiche Anzahl und Art der Atome), unterscheiden sich aber in der räumlichen Anordnung oder Verknüpfung der Atome.",
        difficulty = 2,
        funFact = "Glucose und Fructose sind Isomere – beide haben die Formel C₆H₁₂O₆, schmecken aber unterschiedlich süß."
    ),

    // Question 16 – Genetics
    Question(
        categoryId = 2,
        questionText = "Welcher Wissenschaftler formulierte die Grundlagen der Vererbungslehre durch Kreuzungsexperimente mit Erbsen?",
        answerA = "Charles Darwin",
        answerB = "Gregor Mendel",
        answerC = "Francis Crick",
        answerD = "Thomas Morgan",
        correctAnswer = 1, // B
        explanation = "Gregor Mendel (1822–1884) führte systematische Kreuzungsexperimente mit Erbsen durch und formulierte die drei Mendelschen Gesetze, die die Grundlage der Genetik bilden.",
        difficulty = 2,
        funFact = "Mendel war Mönch im Augustinerkloster zu Brünn. Seine Erkenntnisse wurden erst 16 Jahre nach seinem Tod wiederentdeckt."
    ),

    // Question 17 – Physics
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem Brechungsgesetz (Snelliussches Gesetz)?",
        answerA = "Das Gesetz, das die Reflexion von Licht an Spiegeln beschreibt",
        answerB = "Das Verhältnis der Sinus-Werte von Einfalls- und Brechungswinkel bleibt konstant",
        answerC = "Die Beziehung zwischen Wellenlänge und Lichtgeschwindigkeit",
        answerD = "Das Gesetz der totalen inneren Reflexion",
        correctAnswer = 1, // B
        explanation = "Das Snelliussche Brechungsgesetz besagt: n₁ · sin(α₁) = n₂ · sin(α₂), wobei n die Brechungsindizes und α die Winkel zur Senkrechten sind.",
        difficulty = 2,
        funFact = null
    ),

    // Question 18 – Astronomy
    Question(
        categoryId = 2,
        questionText = "Was ist ein Planetennebel in der Astronomie?",
        answerA = "Eine Gaswolke, aus der neue Planeten entstehen",
        answerB = "Die leuchtende Hülle, die ein sterbender Stern beim Abstoßen seiner äußeren Schichten bildet",
        answerC = "Eine Ansammlung von Planeten außerhalb unseres Sonnensystems",
        answerD = "Der Staub- und Gasring um einen Planeten wie Saturn",
        correctAnswer = 1, // B
        explanation = "Ein Planetennebel entsteht, wenn ein sonnenähnlicher Stern am Ende seines Lebens seine äußeren Gashüllen abstößt. Der verbleibende Kern wird zum weißen Zwerg.",
        difficulty = 2,
        funFact = "Der Name 'Planetennebel' ist irreführend – er hat nichts mit Planeten zu tun. Frühe Astronomen hielten sie durch kleine Teleskope für planetenähnliche Scheiben."
    ),

    // Question 19 – Ecology
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einem Produzenten und einem Konsumenten in einem Ökosystem?",
        answerA = "Produzenten sind Tiere, Konsumenten sind Pilze",
        answerB = "Produzenten erzeugen organische Substanz durch Photosynthese, Konsumenten nehmen sie auf",
        answerC = "Produzenten zerlegen Totmaterial, Konsumenten jagen lebende Beute",
        answerD = "Es gibt keinen Unterschied, beide Begriffe sind synonym",
        correctAnswer = 1, // B
        explanation = "Produzenten (meist Pflanzen) erzeugen durch Photosynthese organische Biomasse. Konsumenten (Tiere) können selbst keine organische Substanz aus anorganischen Stoffen herstellen und ernähren sich von anderen Organismen.",
        difficulty = 2,
        funFact = null
    ),

    // Question 20 – Human Body
    Question(
        categoryId = 2,
        questionText = "Welche Funktion hat das Lymphsystem im menschlichen Körper?",
        answerA = "Es transportiert Sauerstoff zu den Zellen",
        answerB = "Es reguliert den Blutzuckerspiegel",
        answerC = "Es leitet überschüssige Gewebeflüssigkeit zurück ins Blut und unterstützt die Immunabwehr",
        answerD = "Es produziert rote Blutkörperchen",
        correctAnswer = 2, // C
        explanation = "Das Lymphsystem transportiert überschüssige Gewebeflüssigkeit (Lymphe) über Lymphgefäße zurück in den Blutkreislauf und spielt eine zentrale Rolle bei der Immunabwehr.",
        difficulty = 2,
        funFact = "Der menschliche Körper enthält etwa doppelt so viel Lymphflüssigkeit wie Blut."
    ),

    // Question 21 – Geology
    Question(
        categoryId = 2,
        questionText = "Was ist die Mohorovičić-Diskontinuität (Moho)?",
        answerA = "Die Grenze zwischen Erdkern und Erdmantel",
        answerB = "Die Grenze zwischen Erdkruste und Erdmantel",
        answerC = "Eine Verwerfungslinie im Pazifischen Ozean",
        answerD = "Die Oberfläche der inneren Erde, wo Magma entsteht",
        correctAnswer = 1, // B
        explanation = "Die Mohorovičić-Diskontinuität, kurz Moho, ist die seismische Grenzfläche zwischen der Erdkruste und dem darunterliegenden oberen Erdmantel.",
        difficulty = 2,
        funFact = null
    ),

    // Question 22 – Organic Chemistry
    Question(
        categoryId = 2,
        questionText = "Was ist Benzol und welche besondere Eigenschaft hat es?",
        answerA = "Ein gesättigter Kohlenwasserstoff mit einer linearen Kettenstruktur",
        answerB = "Ein aromatischer Kohlenwasserstoff mit einem Ring aus sechs Kohlenstoffatomen und delokalisierten Elektronen",
        answerC = "Ein Alkohol mit sechs Hydroxylgruppen",
        answerD = "Eine Carbonsäure mit ringförmiger Struktur",
        correctAnswer = 1, // B
        explanation = "Benzol (C₆H₆) ist ein aromatischer Kohlenwasserstoff mit einem sechsgliedrigen Ring. Die Elektronen sind über den gesamten Ring delokalisiert, was Benzol besondere Stabilität verleiht.",
        difficulty = 2,
        funFact = "August Kekulé soll die Ringstruktur des Benzols im Traum entdeckt haben, als er eine sich in den eigenen Schwanz beißende Schlange sah."
    ),

    // Question 23 – Genetics
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der Begriff 'Mutation' in der Genetik?",
        answerA = "Die normale Verdopplung der DNA vor der Zellteilung",
        answerB = "Eine dauerhafte Veränderung der DNA-Sequenz",
        answerC = "Das Ablesen der genetischen Information am Ribosom",
        answerD = "Die Trennung homologer Chromosomen in der Meiose",
        correctAnswer = 1, // B
        explanation = "Eine Mutation ist eine dauerhafte Veränderung der DNA-Sequenz. Sie kann spontan auftreten oder durch Mutagene wie UV-Strahlung, Chemikalien oder Viren ausgelöst werden.",
        difficulty = 2,
        funFact = "Die meisten Mutationen sind neutral oder werden durch Reparaturmechanismen der Zelle behoben. Nur wenige führen zu Krankheiten oder evolutionären Veränderungen."
    ),

    // Question 24 – Physics
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einem Halbleiter und einem Leiter?",
        answerA = "Halbleiter leiten Strom nur bei sehr niedrigen Temperaturen",
        answerB = "Halbleiter haben eine elektrische Leitfähigkeit zwischen Leitern und Isolatoren, die durch Dotierung steuerbar ist",
        answerC = "Halbleiter leiten nur Gleichstrom, aber keinen Wechselstrom",
        answerD = "Halbleiter bestehen ausschließlich aus Metallen",
        correctAnswer = 1, // B
        explanation = "Halbleiter wie Silizium haben eine Leitfähigkeit zwischen Leitern und Isolatoren. Durch gezieltes Einbringen von Fremdstoffen (Dotierung) lässt sich ihre Leitfähigkeit präzise steuern.",
        difficulty = 2,
        funFact = "Silizium ist das wichtigste Halbleitermaterial – praktisch alle modernen Computer- und Mobilfunktechnik basiert darauf."
    ),

    // Question 25 – Astronomy
    Question(
        categoryId = 2,
        questionText = "Was ist die Hauptsequenz im Hertzsprung-Russell-Diagramm?",
        answerA = "Die Reihenfolge der Planeten nach ihrer Entfernung zur Sonne",
        answerB = "Ein diagonaler Bereich, in dem sich Sterne während ihrer normalen Wasserstofffusion befinden",
        answerC = "Die zeitliche Abfolge der Entwicklung eines Sterns",
        answerD = "Die Anordnung von Galaxien im Universum nach ihrer Leuchtkraft",
        correctAnswer = 1, // B
        explanation = "Im Hertzsprung-Russell-Diagramm zeigt die Hauptsequenz einen diagonalen Bereich, in dem sich Sterne befinden, die Wasserstoff in ihrem Kern fusionieren. Unsere Sonne ist ein typischer Hauptreihenstern.",
        difficulty = 2,
        funFact = null
    ),

    // Question 26 – Ecology
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der Begriff 'Symbiose' in der Biologie?",
        answerA = "Eine Beziehung, bei der ein Organismus einen anderen tötet",
        answerB = "Eine enge, dauerhafte Lebensgemeinschaft zweier verschiedener Arten zum gegenseitigen Vorteil",
        answerC = "Die Anpassung einer Art an eine neue Umgebung",
        answerD = "Das Verdrängen einer Art durch eine andere im gleichen Lebensraum",
        correctAnswer = 1, // B
        explanation = "Symbiose bezeichnet eine enge Wechselbeziehung zwischen Individuen verschiedener Arten, bei der beide Partner einen Nutzen haben. Ein Beispiel sind Mykorrhiza-Pilze, die mit Pflanzenwurzeln zusammenleben.",
        difficulty = 2,
        funFact = "Flechten sind ein klassisches Symbiose-Beispiel: Sie bestehen aus Pilzen und Algen, wobei beide voneinander profitieren."
    ),

    // Question 27 – Human Body
    Question(
        categoryId = 2,
        questionText = "Welche Aufgabe hat die Niere bei der Regulation des Blutdrucks?",
        answerA = "Sie produziert Adrenalin, das den Blutdruck erhöht",
        answerB = "Sie schüttet das Hormon Renin aus, das das Renin-Angiotensin-System aktiviert",
        answerC = "Sie filtert Adrenalin aus dem Blut heraus",
        answerD = "Sie reguliert die Blutgerinnung durch Vitamin K",
        correctAnswer = 1, // B
        explanation = "Die Niere schüttet bei niedrigem Blutdruck das Enzym Renin aus, das das Renin-Angiotensin-Aldosteron-System aktiviert. Dies führt zu Blutgefäßverengung und erhöhter Natriumretention, wodurch der Blutdruck steigt.",
        difficulty = 2,
        funFact = null
    ),

    // Question 28 – Geology
    Question(
        categoryId = 2,
        questionText = "Was sind Sedimentgesteine?",
        answerA = "Gesteine, die durch hohen Druck und hohe Temperatur umgewandelt wurden",
        answerB = "Gesteine, die durch Abkühlung von Magma entstanden sind",
        answerC = "Gesteine, die durch Ablagerung und Verfestigung von Sedimenten entstanden sind",
        answerD = "Gesteine, die ausschließlich auf dem Meeresgrund vorkommen",
        correctAnswer = 2, // C
        explanation = "Sedimentgesteine entstehen durch Ablagerung, Verdichtung und Verfestigung von Sedimenten wie Sand, Schlamm oder organischen Resten. Beispiele sind Sandstein, Kalkstein und Ton.",
        difficulty = 2,
        funFact = "Fossilien kommen fast ausschließlich in Sedimentgesteinen vor, da die Ablagerungsbedingungen die Erhaltung von Organismen begünstigen."
    ),

    // Question 29 – Organic Chemistry
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen gesättigten und ungesättigten Fettsäuren?",
        answerA = "Gesättigte Fettsäuren haben eine oder mehrere Doppelbindungen, ungesättigte nicht",
        answerB = "Gesättigte Fettsäuren haben keine Doppelbindungen, ungesättigte haben eine oder mehrere",
        answerC = "Gesättigte Fettsäuren kommen nur in pflanzlichen Ölen vor",
        answerD = "Ungesättigte Fettsäuren sind immer fest bei Raumtemperatur",
        correctAnswer = 1, // B
        explanation = "Gesättigte Fettsäuren haben ausschließlich Einfachbindungen zwischen den Kohlenstoffatomen. Ungesättigte Fettsäuren haben mindestens eine Doppelbindung, was ihre Struktur und Eigenschaften verändert.",
        difficulty = 2,
        funFact = "Ungesättigte Fettsäuren sind bei Raumtemperatur flüssig (Öle), während gesättigte Fettsäuren meist fest sind (Butter, Schmalz)."
    ),

    // Question 30 – Genetics
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen mitotischer und meiotischer Zellteilung?",
        answerA = "Mitose erzeugt Zellen mit halbem Chromosomensatz, Meiose erzeugt identische Tochterzellen",
        answerB = "Mitose erzeugt zwei identische Tochterzellen mit vollständigem Chromosomensatz, Meiose erzeugt vier Zellen mit halbem Chromosomensatz",
        answerC = "Mitose findet nur in Keimzellen statt, Meiose in allen Körperzellen",
        answerD = "Es gibt keinen Unterschied, beide Begriffe beschreiben denselben Prozess",
        correctAnswer = 1, // B
        explanation = "Bei der Mitose entstehen zwei genetisch identische Tochterzellen mit dem vollständigen (diploiden) Chromosomensatz. Bei der Meiose entstehen vier genetisch verschiedene Zellen mit dem halben (haploiden) Chromosomensatz.",
        difficulty = 2,
        funFact = null
    ),

    // Question 31 – Physics
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der Begriff 'elektrischer Widerstand' und welche Einheit hat er?",
        answerA = "Die elektrische Ladung in einem Stromkreis, gemessen in Coulomb",
        answerB = "Den Widerstand eines Leiters gegen den Stromfluss, gemessen in Ohm",
        answerC = "Die Energie, die ein elektrisches Gerät verbraucht, gemessen in Watt",
        answerD = "Die Stärke eines Magnetfeldes, gemessen in Tesla",
        correctAnswer = 1, // B
        explanation = "Der elektrische Widerstand gibt an, wie stark ein Leiter den Fluss elektrischer Ladungen hemmt. Die Einheit ist Ohm (Ω), benannt nach Georg Simon Ohm.",
        difficulty = 2,
        funFact = "Der spezifische Widerstand von Kupfer ist sehr niedrig, weshalb es das bevorzugte Material für elektrische Leitungen ist."
    ),

    // Question 32 – Astronomy
    Question(
        categoryId = 2,
        questionText = "Was ist die kosmische Hintergrundstrahlung?",
        answerA = "Die Strahlung, die von der Sonne kontinuierlich ausgesandt wird",
        answerB = "Elektromagnetische Strahlung aus den ersten 380.000 Jahren nach dem Urknall, die das Universum gleichmäßig erfüllt",
        answerC = "Die Strahlung schwarzer Löcher am Rand des Universums",
        answerD = "Gamma-Strahlung aus aktiven Galaxienkernen",
        correctAnswer = 1, // B
        explanation = "Die kosmische Mikrowellen-Hintergrundstrahlung (CMB) ist das 'Nachglühen' des Urknalls. Sie entstand ca. 380.000 Jahre nach dem Urknall und wurde 1965 von Penzias und Wilson entdeckt.",
        difficulty = 2,
        funFact = "Die CMB hat heute eine Temperatur von nur noch 2,725 Kelvin, fast dem absoluten Nullpunkt."
    ),

    // Question 33 – Ecology
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen Parasitismus und Kommensalismus?",
        answerA = "Beim Parasitismus profitieren beide Partner, beim Kommensalismus nur einer",
        answerB = "Beim Parasitismus schadet der Gast dem Wirt, beim Kommensalismus profitiert einer ohne dem anderen zu schaden",
        answerC = "Parasitismus kommt nur bei Tieren vor, Kommensalismus nur bei Pflanzen",
        answerD = "Beide Begriffe sind synonym für einseitig nützliche Beziehungen",
        correctAnswer = 1, // B
        explanation = "Beim Parasitismus schädigt der Parasit seinen Wirt (z.B. Bandwurm). Beim Kommensalismus profitiert ein Organismus, ohne dem anderen zu nutzen oder zu schaden (z.B. Darmbakterien beim Menschen).",
        difficulty = 2,
        funFact = null
    ),

    // Question 34 – Human Body
    Question(
        categoryId = 2,
        questionText = "Welche Substanz ist der wichtigste Energieträger für Gehirnzellen?",
        answerA = "Fettsäuren",
        answerB = "Proteine",
        answerC = "Glukose",
        answerD = "Laktat",
        correctAnswer = 2, // C
        explanation = "Das Gehirn ist fast ausschließlich auf Glukose als Energiequelle angewiesen. Es verbraucht trotz seines geringen Gewichts (ca. 2% der Körpermasse) etwa 20% der gesamten Körperenergie.",
        difficulty = 2,
        funFact = "Bei längerem Fasten kann das Gehirn auf Ketonkörper als alternative Energiequelle umschalten – ein Mechanismus, der beim Intervallfasten genutzt wird."
    ),

    // Question 35 – Geology
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einem Erdbeben und einem Tsunami?",
        answerA = "Ein Erdbeben ist immer stärker als ein Tsunami",
        answerB = "Erdbeben sind seismische Erschütterungen der Erdkruste, Tsunamis sind durch sie ausgelöste Meereswellen",
        answerC = "Tsunamis entstehen nur durch Vulkanausbrüche, nie durch Erdbeben",
        answerD = "Tsunamis sind kleinere Erdbeben im Ozean",
        correctAnswer = 1, // B
        explanation = "Erdbeben sind Erschütterungen der Erdkruste durch tektonische Verschiebungen. Ein Tsunami ist eine Reihe von Meereswellen, die meist durch ein unterseeisches Erdbeben ausgelöst werden.",
        difficulty = 2,
        funFact = "In der offenen See können Tsunamis kaum wahrgenommen werden – die Wellen sind lang und flach. Erst im flachen Küstenbereich stauen sie sich zu zerstörerischen Höhen auf."
    ),

    // Question 36 – Organic Chemistry
    Question(
        categoryId = 2,
        questionText = "Was sind Polymere?",
        answerA = "Moleküle mit genau drei Kohlenstoffatomen",
        answerB = "Große Moleküle, die aus vielen gleichartigen oder ähnlichen Einheiten (Monomeren) aufgebaut sind",
        answerC = "Verbindungen aus zwei verschiedenen Metallen",
        answerD = "Organische Verbindungen mit einer Ringstruktur aus sechs Kohlenstoffatomen",
        correctAnswer = 1, // B
        explanation = "Polymere sind Makromoleküle, die durch Verknüpfung vieler kleiner Einheiten (Monomere) entstehen. Beispiele sind Cellulose, Stärke, Proteine sowie synthetische Kunststoffe wie Polyethylen.",
        difficulty = 2,
        funFact = "DNA ist ebenfalls ein Polymer – ein Polynukleotid, dessen Monomere die vier Nukleobasen Adenin, Thymin, Guanin und Cytosin sind."
    ),

    // Question 37 – Genetics
    Question(
        categoryId = 2,
        questionText = "Was ist ein Karyotyp?",
        answerA = "Die Gesamtheit aller Gene eines Organismus",
        answerB = "Die vollständige Darstellung aller Chromosomen einer Zelle nach Anzahl, Form und Größe",
        answerC = "Die Sequenz aller Proteine in einer Zelle",
        answerD = "Der Anteil repetitiver DNA-Abschnitte im Genom",
        correctAnswer = 1, // B
        explanation = "Der Karyotyp ist die systematische Darstellung aller Chromosomen eines Organismus geordnet nach Größe und Form. Beim Menschen sind es 46 Chromosomen (23 Paare).",
        difficulty = 2,
        funFact = null
    ),

    // Question 38 – Physics
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen Kernfusion und Kernspaltung?",
        answerA = "Bei der Fusion werden schwere Kerne gespalten, bei der Spaltung leichte Kerne vereinigt",
        answerB = "Bei der Fusion vereinigen sich leichte Kerne zu einem schwereren, bei der Spaltung zerfällt ein schwerer Kern",
        answerC = "Beide Prozesse laufen nur bei sehr niedrigen Temperaturen ab",
        answerD = "Fusion erzeugt Energie, Spaltung verbraucht Energie",
        correctAnswer = 1, // B
        explanation = "Bei der Kernfusion verschmelzen leichte Atomkerne (z.B. Wasserstoff) zu schwereren Kernen (z.B. Helium). Bei der Kernspaltung zerfällt ein schwerer Kern (z.B. Uran) in kleinere Fragmente. Beide Prozesse setzen Energie frei.",
        difficulty = 2,
        funFact = "Die Sonne erzeugt ihre Energie durch Kernfusion – pro Sekunde werden dabei etwa 600 Millionen Tonnen Wasserstoff zu Helium fusioniert."
    ),

    // Question 39 – Astronomy
    Question(
        categoryId = 2,
        questionText = "Wie entsteht eine Mondfinsternis?",
        answerA = "Wenn der Mond die Sonne bedeckt",
        answerB = "Wenn die Erde in den Schatten des Mondes gerät",
        answerC = "Wenn der Mond in den Kernschatten der Erde eintaucht",
        answerD = "Wenn die Sonne hinter der Erde verschwindet",
        correctAnswer = 2, // C
        explanation = "Eine Mondfinsternis entsteht, wenn der Mond in den Kernschatten (Umbra) der Erde tritt. Die Erde steht dabei zwischen Sonne und Mond. Im Gegensatz zur Sonnenfinsternis ist eine Mondfinsternis von der ganzen Nachtseite der Erde sichtbar.",
        difficulty = 2,
        funFact = "Während einer totalen Mondfinsternis erscheint der Mond rötlich – daher der Name 'Blutmond'. Das Rot entsteht durch gebrochenes Sonnenlicht in der Erdatmosphäre."
    ),

    // Question 40 – Ecology
    Question(
        categoryId = 2,
        questionText = "Was ist der Stickstoffkreislauf?",
        answerA = "Die Bewegung von Stickstoff durch die Atmosphäre, Böden, Wasser und Lebewesen",
        answerB = "Die chemische Reaktion, bei der Stickstoff in Sauerstoff umgewandelt wird",
        answerC = "Die Zirkulation von Luftmassen rund um den Globus",
        answerD = "Der Kreislauf der Nahrungsaufnahme bei Fleischfressern",
        correctAnswer = 0, // A
        explanation = "Der Stickstoffkreislauf beschreibt den Weg des Stickstoffs durch Atmosphäre, Boden, Wasser und lebende Organismen. Stickstoff ist essenziell für den Aufbau von Proteinen und Nukleinsäuren.",
        difficulty = 2,
        funFact = null
    ),

    // Question 41 – Human Body
    Question(
        categoryId = 2,
        questionText = "Welche der vier Grundgeschmacksrichtungen wird durch Glutaminsäure ausgelöst?",
        answerA = "Süß",
        answerB = "Sauer",
        answerC = "Umami",
        answerD = "Salzig",
        correctAnswer = 2, // C
        explanation = "Umami ist der fünfte Grundgeschmack (neben süß, sauer, salzig, bitter) und wird durch Glutaminsäure und deren Salze (Glutamate) ausgelöst. Er beschreibt einen fleischigen, würzigen Geschmack.",
        difficulty = 2,
        funFact = "Umami wurde 1908 vom japanischen Chemiker Kikunae Ikeda entdeckt, der den Wirkstoff aus Kombu-Seetang isolierte."
    ),

    // Question 42 – Geology
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einer Verwerfung und einer Falte in der Geologie?",
        answerA = "Verwerfungen kommen nur im Ozean vor, Falten nur auf Kontinenten",
        answerB = "Bei einer Verwerfung bricht das Gestein und verschiebt sich, bei einer Falte biegt es sich plastisch",
        answerC = "Verwerfungen entstehen durch Erosion, Falten durch Vulkanismus",
        answerD = "Beide Begriffe beschreiben dasselbe geologische Phänomen",
        correctAnswer = 1, // B
        explanation = "Eine Verwerfung ist ein Bruch im Gestein, entlang dem sich Gesteinsblöcke verschieben. Eine Falte entsteht, wenn Gesteinsschichten durch tektonischen Druck plastisch verbogen werden, ohne zu brechen.",
        difficulty = 2,
        funFact = "Der Himalaya entstand durch Faltung, als die Indische Platte vor ca. 50 Millionen Jahren in die Eurasische Platte drückte."
    ),

    // Question 43 – Organic Chemistry
    Question(
        categoryId = 2,
        questionText = "Was ist Chiralität in der organischen Chemie?",
        answerA = "Die Fähigkeit eines Moleküls, Licht zu absorbieren",
        answerB = "Die Eigenschaft eines Moleküls, sich nicht mit seinem Spiegelbild zur Deckung bringen zu lassen",
        answerC = "Die Reaktion eines organischen Moleküls mit Wasser",
        answerD = "Die Anzahl der Kohlenstoff-Kohlenstoff-Doppelbindungen in einem Molekül",
        correctAnswer = 1, // B
        explanation = "Ein chirales Molekül verhält sich zu seinem Spiegelbild wie linke und rechte Hand – sie sind nicht deckungsgleich. Solche Spiegelbilder heißen Enantiomere und können trotz gleicher Summenformel sehr unterschiedliche biologische Wirkungen haben.",
        difficulty = 2,
        funFact = "Das Medikament Thalidomid (Contergan) ist ein Beispiel: Eine Form war als Schlafmittel wirksam, die Spiegelform verursachte schwere Missbildungen bei Föten."
    ),

    // Question 44 – Genetics
    Question(
        categoryId = 2,
        questionText = "Was ist ein Promotor in der Molekularbiologie?",
        answerA = "Ein Enzym, das die DNA verdoppelt",
        answerB = "Eine DNA-Sequenz, an die RNA-Polymerase bindet, um die Transkription eines Gens zu starten",
        answerC = "Ein Protein, das Mutationen repariert",
        answerD = "Ein Abschnitt der mRNA, der für den Abbau verantwortlich ist",
        correctAnswer = 1, // B
        explanation = "Ein Promotor ist eine regulatorische DNA-Sequenz stromaufwärts eines Gens, an die die RNA-Polymerase bindet, um die Transkription (Abschreiben der DNA in RNA) zu starten.",
        difficulty = 2,
        funFact = null
    ),

    // Question 45 – Physics
    Question(
        categoryId = 2,
        questionText = "Was ist Radioaktivität?",
        answerA = "Die Fähigkeit eines Metalls, Radiowellen zu reflektieren",
        answerB = "Der spontane Zerfall instabiler Atomkerne unter Aussendung ionisierender Strahlung",
        answerC = "Die Absorption von Sonnenlicht durch bestimmte Materialien",
        answerD = "Die Aussendung sichtbaren Lichts durch angeregte Elektronen",
        correctAnswer = 1, // B
        explanation = "Radioaktivität ist der spontane Zerfall instabiler Atomkerne, dabei wird ionisierende Strahlung (Alpha-, Beta- oder Gammastrahlung) ausgesendet. Der Kern wandelt sich dabei in einen anderen Kern um.",
        difficulty = 2,
        funFact = "Marie Curie prägte den Begriff 'Radioaktivität' und entdeckte die Elemente Polonium und Radium. Sie starb 1934 vermutlich an den Folgen ihrer Strahlenexposition."
    ),

    // Question 46 – Astronomy
    Question(
        categoryId = 2,
        questionText = "Was ist Dunkle Materie?",
        answerA = "Materie in schwarzen Löchern, die kein Licht reflektiert",
        answerB = "Hypothetische Materie, die keine elektromagnetische Strahlung emittiert oder absorbiert, aber gravitativ wirkt",
        answerC = "Interstellarer Staub und Gas zwischen den Sternen",
        answerD = "Die Materie, die durch Kernfusion in der Sonne verbraucht wird",
        correctAnswer = 1, // B
        explanation = "Dunkle Materie ist eine hypothetische Form der Materie, die nicht mit elektromagnetischer Strahlung interagiert (also unsichtbar ist), aber durch ihre Schwerkraft auf sichtbare Materie wirkt. Sie soll etwa 27% des Universums ausmachen.",
        difficulty = 2,
        funFact = "Die Existenz der Dunklen Materie wurde erschlossen, weil Galaxien sich schneller drehen als durch sichtbare Materie allein erklärbar wäre."
    ),

    // Question 47 – Ecology
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem Begriff 'Sukzession' in der Ökologie?",
        answerA = "Das Aussterben einer Art durch Überjagung",
        answerB = "Die gerichtete, zeitliche Abfolge von Lebensgemeinschaften in einem Gebiet bis zum Klimax-Stadium",
        answerC = "Die saisonale Wanderung von Tieren in wärmere Regionen",
        answerD = "Die Einwanderung invasiver Arten in ein Ökosystem",
        correctAnswer = 1, // B
        explanation = "Ökologische Sukzession ist der geordnete Prozess, bei dem sich eine Lebensgemeinschaft über Zeit verändert und stabilisiert. Primäre Sukzession beginnt auf nacktem Gestein, sekundäre Sukzession nach einer Störung in einem bestehenden Ökosystem.",
        difficulty = 2,
        funFact = null
    ),

    // Question 48 – Human Body
    Question(
        categoryId = 2,
        questionText = "Was ist die Funktion des Kleinhirns im menschlichen Gehirn?",
        answerA = "Es verarbeitet Sinneseindrücke und steuert die Sprache",
        answerB = "Es koordiniert Bewegungen, Gleichgewicht und Feinmotorik",
        answerC = "Es reguliert Hunger und Schlaf-Wach-Rhythmus",
        answerD = "Es ist für das Langzeitgedächtnis zuständig",
        correctAnswer = 1, // B
        explanation = "Das Kleinhirn (Cerebellum) ist hauptsächlich für die Koordination von Bewegungen, das Gleichgewicht und die Feinmotorik zuständig. Es empfängt Signale aus dem Großhirn und von Muskeln und Gelenken.",
        difficulty = 2,
        funFact = "Das Kleinhirn macht nur etwa 10% des Hirnvolumens aus, enthält aber mehr als 50% aller Nervenzellen des Gehirns."
    ),

    // Question 49 – Geology
    Question(
        categoryId = 2,
        questionText = "Was ist ein Metamorphit?",
        answerA = "Ein Gestein, das beim Abkühlen von Lava entsteht",
        answerB = "Ein Gestein, das durch Druck und Wärme aus einem anderen Gestein umgebildet wurde",
        answerC = "Ein fossilienhaltiges Sedimentgestein",
        answerD = "Ein Gestein, das nur in der Tiefsee vorkommt",
        correctAnswer = 1, // B
        explanation = "Metamorphite (metamorphe Gesteine) entstehen, wenn vorhandene Gesteine durch hohen Druck und/oder hohe Temperatur in der Erdkruste umgewandelt werden, ohne dabei zu schmelzen. Beispiele sind Marmor (aus Kalkstein) und Schiefer.",
        difficulty = 2,
        funFact = "Diamant ist die härteste natürliche Substanz und entsteht durch extreme Metamorphose von Kohlenstoff unter dem immensen Druck in etwa 150 km Tiefe."
    ),

    // Question 50 – Physics
    Question(
        categoryId = 2,
        questionText = "Was beschreibt die Relativitätstheorie von Einstein in ihrer speziellen Form?",
        answerA = "Die Beschreibung der Gravitation als Krümmung der Raumzeit",
        answerB = "Die Gesetze der Physik gelten für alle Beobachter gleich, und die Lichtgeschwindigkeit ist konstant",
        answerC = "Teilchen können sich nicht schneller als Schall bewegen",
        answerD = "Die Energie eines Systems hängt nur von seiner Temperatur ab",
        correctAnswer = 1, // B
        explanation = "Die spezielle Relativitätstheorie basiert auf zwei Postulaten: Die Naturgesetze haben die gleiche Form für alle gleichförmig bewegten Beobachter, und die Lichtgeschwindigkeit im Vakuum ist konstant (ca. 299.792 km/s) – unabhängig von der Bewegung der Quelle.",
        difficulty = 2,
        funFact = "Aus Einsteins berühmter Formel E = mc² folgt, dass Masse und Energie äquivalent sind. Die Umwandlung kleiner Massen setzt enorme Energiemengen frei."
    )
)
