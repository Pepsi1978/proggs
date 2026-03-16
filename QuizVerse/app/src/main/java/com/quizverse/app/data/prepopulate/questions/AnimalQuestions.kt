package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestions(): List<Question> = listOf(

    // ── EASY (difficulty = 1) ── ~10 questions ──────────────────────────────

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist das größte Landsäugetier der Erde?",
        answerA = "Nilpferd",
        answerB = "Nashorn",
        answerC = "Afrikanischer Elefant",
        answerD = "Giraffe",
        correctAnswer = 2,
        explanation = "Der afrikanische Elefant ist das größte Landsäugetier der Erde und kann bis zu 7 Tonnen wiegen.",
        difficulty = 1,
        funFact = "Elefanten sind die einzigen Tiere, die nicht springen können."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Lieblingsnahrung eines Koalabären?",
        answerA = "Bambus",
        answerB = "Eukalyptusblätter",
        answerC = "Früchte",
        answerD = "Gras",
        correctAnswer = 1,
        explanation = "Koalas ernähren sich fast ausschließlich von Eukalyptusblättern, die für die meisten anderen Tiere giftig sind.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Farbe haben Flamingos bei der Geburt?",
        answerA = "Rosa",
        answerB = "Weiß",
        answerC = "Grau",
        answerD = "Hellrot",
        correctAnswer = 2,
        explanation = "Flamingos werden mit grauem Gefieder geboren. Ihre rosa Farbe entsteht durch Carotinoide in ihrer Nahrung.",
        difficulty = 1,
        funFact = "Je mehr Krebse und Algen ein Flamingo frisst, desto intensiver wird seine rosa Farbe."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Haus einer Schnecke?",
        answerA = "Schale",
        answerB = "Panzer",
        answerC = "Gehäuse",
        answerD = "Kokon",
        correctAnswer = 2,
        explanation = "Das Haus einer Schnecke heißt Gehäuse. Es besteht hauptsächlich aus Kalk und wächst mit der Schnecke mit.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist als 'König der Tiere' bekannt?",
        answerA = "Tiger",
        answerB = "Leopard",
        answerC = "Gepard",
        answerD = "Löwe",
        correctAnswer = 3,
        explanation = "Der Löwe wird traditionell als 'König der Tiere' bezeichnet, vermutlich wegen seiner imposanten Mähne und seines majestätischen Auftretens.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat den längsten Hals?",
        answerA = "Kamel",
        answerB = "Giraffe",
        answerC = "Strauß",
        answerD = "Flamingo",
        correctAnswer = 1,
        explanation = "Die Giraffe hat den längsten Hals aller lebenden Tiere. Ihr Hals kann bis zu 2,4 Meter lang sein.",
        difficulty = 1,
        funFact = "Trotz des langen Halses hat die Giraffe genauso viele Halswirbel wie der Mensch – nämlich sieben."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier legt die größten Eier der Welt?",
        answerA = "Strauß",
        answerB = "Pelikan",
        answerC = "Kondor",
        answerD = "Albatros",
        correctAnswer = 0,
        explanation = "Der Strauß legt die größten Eier aller lebenden Vögel. Ein Straußenei kann bis zu 1,5 kg wiegen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man einen männlichen Schwan?",
        answerA = "Rüde",
        answerB = "Hahn",
        answerC = "Kober",
        answerD = "Enter",
        correctAnswer = 2,
        explanation = "Ein männlicher Schwan heißt Kober, das Weibchen heißt Penne und das Jungtier heißt Schwänlein.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier schläft den ganzen Winter?",
        answerA = "Wolf",
        answerB = "Igel",
        answerC = "Fuchs",
        answerD = "Reh",
        correctAnswer = 1,
        explanation = "Der Igel hält echten Winterschlaf. Dabei sinkt seine Körpertemperatur fast auf Außentemperatur und sein Herzschlag verlangsamt sich stark.",
        difficulty = 1,
        funFact = "Während des Winterschlafs atmet ein Igel nur noch etwa einmal pro Minute."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier kann seinen Schwanz nachwachsen lassen?",
        answerA = "Schildkröte",
        answerB = "Eidechse",
        answerC = "Krokodil",
        answerD = "Schlange",
        correctAnswer = 1,
        explanation = "Viele Eidechsenarten können ihren Schwanz abwerfen (Autotomie) und innerhalb weniger Wochen einen neuen nachwachsen lassen.",
        difficulty = 1,
        funFact = null
    ),

    // ── MEDIUM (difficulty = 2) ── ~15 questions ─────────────────────────────

    Question(
        categoryId = 9,
        questionText = "Wie lange ist die Trächtigkeitsdauer eines afrikanischen Elefanten?",
        answerA = "12 Monate",
        answerB = "18 Monate",
        answerC = "22 Monate",
        answerD = "30 Monate",
        correctAnswer = 2,
        explanation = "Afrikanische Elefanten haben mit etwa 22 Monaten die längste Trächtigkeitsdauer aller Landsäugetiere.",
        difficulty = 2,
        funFact = "Ein neugeborenes Elefantenbaby kann bereits kurz nach der Geburt laufen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Insekt produziert Honig?",
        answerA = "Wespe",
        answerB = "Hummel",
        answerC = "Honigbiene",
        answerD = "Hornisse",
        correctAnswer = 2,
        explanation = "Die Honigbiene (Apis mellifera) produziert Honig aus dem Nektar von Blüten, den sie in ihrem Bienenstock lagert und eindickt.",
        difficulty = 2,
        funFact = "Eine Biene muss rund zwei Millionen Blüten besuchen, um ein Kilogramm Honig zu produzieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Meerestier hat drei Herzen?",
        answerA = "Delfin",
        answerB = "Krake",
        answerC = "Tintenfisch",
        answerD = "Seepferdchen",
        correctAnswer = 1,
        explanation = "Der Krake (Oktopus) besitzt drei Herzen: ein Hauptherz und zwei Kiemenherzen, die das Blut durch die Kiemen pumpen.",
        difficulty = 2,
        funFact = "Das Blut eines Kraken ist blau, weil es Hämocyanin statt Hämoglobin enthält."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der schnellste Vogel im Sturzflug?",
        answerA = "Mauersegler",
        answerB = "Wanderfalke",
        answerC = "Adler",
        answerD = "Albatros",
        correctAnswer = 1,
        explanation = "Der Wanderfalke ist im Sturzflug das schnellste Tier der Welt und erreicht Geschwindigkeiten von über 300 km/h.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Säugetier kann fliegen?",
        answerA = "Fliegender Hund",
        answerB = "Flughörnchen",
        answerC = "Fledermaus",
        answerD = "Flugbeutler",
        correctAnswer = 2,
        explanation = "Fledermäuse sind die einzigen Säugetiere, die aktiv fliegen können. Alle anderen genannten Tiere gleiten nur.",
        difficulty = 2,
        funFact = "Es gibt weltweit über 1.400 verschiedene Fledermausarten – das sind fast 20% aller Säugetierarten."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie orientieren sich Zugvögel auf ihren langen Reisen?",
        answerA = "Ausschließlich nach der Sonne",
        answerB = "Nach dem Magnetfeld der Erde",
        answerC = "Nach Flussverläufen",
        answerD = "Durch Lernverhalten von den Eltern",
        correctAnswer = 1,
        explanation = "Zugvögel nutzen mehrere Navigationssysteme, darunter das Erdmagnetfeld. In bestimmten Zellen ihrer Augen gibt es Magnetrezeptoren.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat die meisten Beine?",
        answerA = "Tausendfüßer (Millipede)",
        answerB = "Tausendfüßer (Centipede)",
        answerC = "Krabbe",
        answerD = "Seestern",
        correctAnswer = 0,
        explanation = "Tausendfüßer der Klasse Diplopoda (Doppelfüßer) haben die meisten Beine – rekordverdächtig sind über 1.300 Beine bei Eumillipes persephone.",
        difficulty = 2,
        funFact = "Trotz des Namens hat kein Tausendfüßer exakt 1.000 Beine."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist ein Hermelin?",
        answerA = "Ein Raubtier aus der Familie der Marder",
        answerB = "Eine Vogelart",
        answerC = "Ein Nagetier",
        answerD = "Ein Reptil",
        correctAnswer = 0,
        explanation = "Das Hermelin (Mustela erminea) ist ein kleines Raubtier aus der Familie der Marder. Im Winter wird sein Fell weiß.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist der nächste lebende Verwandte des Menschen?",
        answerA = "Gorilla",
        answerB = "Orang-Utan",
        answerC = "Schimpanse",
        answerD = "Gibbon",
        correctAnswer = 2,
        explanation = "Der Schimpanse teilt mit dem Menschen etwa 98,7% der DNA und ist damit unser nächster lebender Verwandter.",
        difficulty = 2,
        funFact = "Schimpansen können Werkzeuge herstellen und verwenden – zum Beispiel nutzen sie Stöcke, um Termiten aus Hügeln zu angeln."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier produziert Seide?",
        answerA = "Seidenraupe",
        answerB = "Stabheuschrecke",
        answerC = "Gottesanbeterin",
        answerD = "Libelle",
        correctAnswer = 0,
        explanation = "Die Seidenraupe (Bombyx mori), die Larve des Maulbeerspinners, produziert Seide für ihren Kokon.",
        difficulty = 2,
        funFact = "Ein einziger Seidenkokon kann bis zu 1.500 Meter Seidentfaden enthalten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel kann rückwärts fliegen?",
        answerA = "Kolibri",
        answerB = "Schwalbe",
        answerC = "Specht",
        answerD = "Eisvogel",
        correctAnswer = 0,
        explanation = "Der Kolibri ist der einzige Vogel, der dauerhaft rückwärts fliegen kann. Seine Flügel schlagen bis zu 80 Mal pro Sekunde.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie heißt der Lebensraum der Polarfüchse?",
        answerA = "Taiga",
        answerB = "Tundra",
        answerC = "Steppe",
        answerD = "Savanne",
        correctAnswer = 1,
        explanation = "Polarfüchse leben in der arktischen Tundra und sind hervorragend an extreme Kälte angepasst.",
        difficulty = 2,
        funFact = "Das Fell des Polarfuchses ist das wärmste aller arktischen Tiere im Verhältnis zu seinem Gewicht."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Jungtier eines Pferdes?",
        answerA = "Fohlen",
        answerB = "Küken",
        answerC = "Welpe",
        answerD = "Ferkel",
        correctAnswer = 0,
        explanation = "Das Jungtier eines Pferdes heißt Fohlen. Weibliche Jungtiere nennt man Stutfohlen, männliche Hengstfohlen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Meerestier ist für seine Biolumineszenz bekannt?",
        answerA = "Weißer Hai",
        answerB = "Tiefseequalle",
        answerC = "Seehund",
        answerD = "Mantarochen",
        correctAnswer = 1,
        explanation = "Viele Tiefseequallen können Licht produzieren (Biolumineszenz). Dieses Phänomen kommt bei bis zu 80% aller Tiefseebewohner vor.",
        difficulty = 2,
        funFact = "Das Meeresleuchten, das man manchmal an Stränden beobachten kann, wird von biolumineszenten Plankton verursacht."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist ein Allesfresser?",
        answerA = "Löwe",
        answerB = "Koala",
        answerC = "Braunbär",
        answerD = "Gepard",
        correctAnswer = 2,
        explanation = "Der Braunbär ist ein Allesfresser (Omnivore) und frisst Beeren, Nüsse, Fisch, Insekten sowie gelegentlich Säugetiere.",
        difficulty = 2,
        funFact = null
    ),

    // ── HARD (difficulty = 3) ── ~12 questions ───────────────────────────────

    Question(
        categoryId = 9,
        questionText = "Wie nennt man die Kommunikationssprache der Delfine?",
        answerA = "Infraschall",
        answerB = "Echoortung und Klick-Laute",
        answerC = "Ultraviolettes Signaling",
        answerD = "Chemische Signale",
        correctAnswer = 1,
        explanation = "Delfine kommunizieren hauptsächlich durch Klick-Laute, Pfiffe und Echoortung (Biosonar). Jeder Delfin hat einen einzigartigen Signaturpfiff.",
        difficulty = 3,
        funFact = "Delfine schlafen, indem sie eine Gehirnhälfte ausschalten – die andere bleibt wach, um die Atmung zu kontrollieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat das größte Gehirn im Verhältnis zur Körpergröße?",
        answerA = "Delfin",
        answerB = "Elefant",
        answerC = "Ameise",
        answerD = "Mensch",
        correctAnswer = 2,
        explanation = "Ameisen haben das größte Gehirn im Verhältnis zur Körpergröße. Ihr Gehirn macht etwa 6% ihres Körpergewichts aus.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Besonderheit des Schnabeltiers unter den Säugetieren?",
        answerA = "Es hat Schuppen statt Haare",
        answerB = "Es legt Eier und säugt seine Jungen",
        answerC = "Es kann sich wie ein Chamäleon tarnen",
        answerD = "Es hat kein Skelett",
        correctAnswer = 1,
        explanation = "Das Schnabeltier ist ein Kloakentier – es legt Eier wie Reptilien, säugt aber seine Jungen wie Säugetiere. Es gehört zu den drei Kloakentierarten.",
        difficulty = 3,
        funFact = "Das Schnabeltier ist eines der wenigen giftigen Säugetiere – Männchen haben an den Hinterbeinen Giftstachel."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Funktion haben die roten Flecken auf den Flügeln des Kaiserpinguins?",
        answerA = "Sie dienen der Tarnung",
        answerB = "Sie regulieren die Körpertemperatur",
        answerC = "Sie spielen eine Rolle bei der Partnerwahl",
        answerD = "Kaiserpinguine haben keine roten Flecken",
        correctAnswer = 3,
        explanation = "Kaiserpinguine haben keine roten Flecken auf ihren Flügeln. Sie haben jedoch gelb-orangefarbene Markierungen am Kopf, die bei der Partnersuche eine Rolle spielen.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Mimikry im Tierreich?",
        answerA = "Die Fähigkeit, Laute anderer Tiere nachzuahmen",
        answerB = "Das Nachahmen von Aussehen oder Verhalten anderer Arten zum Schutz",
        answerC = "Ein Balzritual bei Vögeln",
        answerD = "Die Anpassung an saisonale Nahrungsquellen",
        correctAnswer = 1,
        explanation = "Mimikry ist die evolutionäre Anpassung, bei der eine Art das Aussehen, die Farben oder das Verhalten einer anderen Art imitiert – meist zum Schutz vor Fressfeinden.",
        difficulty = 3,
        funFact = "Das Schmetterlings-Ei der Passionsblume ahmt Schmetterlingseiern nach, um echte Schmetterlinge davon abzuhalten, dort Eier abzulegen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Phänomen, wenn ein Tier seine Farbe wechselt wie das Chamäleon?",
        answerA = "Biolumineszenz",
        answerB = "Chromatophorie",
        answerC = "Autotomie",
        answerD = "Aposematismus",
        correctAnswer = 1,
        explanation = "Chromatophorie beschreibt den Farbwechsel durch spezielle Zellen, die Chromatophoren. Chamäleons ändern die Farbe hauptsächlich zur Kommunikation, nicht zur Tarnung.",
        difficulty = 3,
        funFact = "Tintenfische und Kraken können ihre Farbe in Millisekunden ändern und sind damit die schnellsten Farbwechsler im Tierreich."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel legt sein Ei in die Nester anderer Vögel?",
        answerA = "Elster",
        answerB = "Kuckuck",
        answerC = "Rabenkrähe",
        answerD = "Wendehals",
        correctAnswer = 1,
        explanation = "Der Kuckuck ist ein Brutparasit – er legt seine Eier in fremde Nester. Das Kuckucksjunge wirft dann die anderen Eier oder Nestlinge hinaus.",
        difficulty = 3,
        funFact = "Das Weibchen des Kuckucks kann die Farbe und Muster seiner Eier an die Wirtsvögeleier anpassen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen einem Alligator und einem Krokodil?",
        answerA = "Alligatoren sind größer",
        answerB = "Bei Alligatoren ist der Unterkiefer-Vierzahn im geschlossenen Maul nicht sichtbar",
        answerC = "Krokodile leben ausschließlich im Salzwasser",
        answerD = "Alligatoren haben keine Zähne",
        correctAnswer = 1,
        explanation = "Der auffälligste Unterschied: Bei Krokodilen ist der vierte Zahn des Unterkiefers sichtbar, wenn das Maul geschlossen ist. Bei Alligatoren verschwindet er in einer Ausbuchtung des Oberkiefers.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Geweih des Rehs im ersten Jahr?",
        answerA = "Spießer",
        answerB = "Kitz-Geweih",
        answerC = "Erstling",
        answerD = "Bock-Geweih",
        correctAnswer = 0,
        explanation = "Jungrehe tragen im ersten Jahr einfache, spießartige Geweihe ohne Verästelungen – daher werden sie als Spießer bezeichnet.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat die längste Lebensdauer unter den Wirbellosen?",
        answerA = "Riesenschildkröte",
        answerB = "Grönlandhai",
        answerC = "Ozeanische Qualle (Turritopsis dohrnii)",
        answerD = "Islandmuschel (Arctica islandica)",
        correctAnswer = 3,
        explanation = "Die Islandmuschel kann über 500 Jahre alt werden. Das älteste bekannte Individuum, 'Ming', war 507 Jahre alt.",
        difficulty = 3,
        funFact = "Die Qualle Turritopsis dohrnii gilt als biologisch unsterblich, da sie sich nach der Fortpflanzung wieder in ihr Jugendstadium zurückbilden kann."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Hauptunterschied zwischen Affen und Menschenaffen?",
        answerA = "Menschenaffen haben einen Schwanz",
        answerB = "Affen haben keinen Schwanz",
        answerC = "Menschenaffen haben keinen Schwanz",
        answerD = "Affen leben nur in Afrika",
        correctAnswer = 2,
        explanation = "Menschenaffen (Gorilla, Schimpanse, Orang-Utan, Gibbon, Mensch) haben im Gegensatz zu Affen keinen Schwanz und haben ein größeres Gehirn.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist bekannt für seine Fähigkeit, den eigenen Arm zu regenerieren?",
        answerA = "Tintenfisch",
        answerB = "Seestern",
        answerC = "Krabbe",
        answerD = "Seegurke",
        correctAnswer = 1,
        explanation = "Seesterne können verlorene Arme vollständig regenerieren. Einige Arten können sogar aus einem einzigen Arm einen neuen Seestern wachsen lassen.",
        difficulty = 3,
        funFact = "Seegurken können bei Gefahr ihre inneren Organe ausstoßen und regenerieren sie später – ein extremes Überlebensstrategie."
    ),

    // ── EXPERT (difficulty = 4) ── ~8 questions ──────────────────────────────

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Phänomen, bei dem Bienenvölker plötzlich zusammenbrechen?",
        answerA = "Kollapssyndrom",
        answerB = "Colony Collapse Disorder (CCD)",
        answerC = "Schwarmfieber",
        answerD = "Varroatose",
        correctAnswer = 1,
        explanation = "Colony Collapse Disorder (CCD) oder Bienenvölkersterben bezeichnet das massenhafte Verschwinden von Arbeiterbienen. Pestizide, Varroamilben und Monokultur gelten als Ursachen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter dem Begriff 'K-Strategie' in der Ökologie?",
        answerA = "Tiere die viele Nachkommen mit wenig elterlicher Fürsorge produzieren",
        answerB = "Tiere die wenige Nachkommen mit intensiver elterlicher Fürsorge aufziehen",
        answerC = "Tiere die sowohl Beute als auch Räuber sein können",
        answerD = "Tiere die in Gruppen leben und sich koordinieren",
        correctAnswer = 1,
        explanation = "K-Strategen (z.B. Elefanten, Wale, Menschen) investieren viel in wenige Nachkommen und haben eine hohe Lebenserwartung. Im Gegensatz dazu stehen r-Strategen wie Insekten.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus ermöglicht dem Wüstenhahn-Gecko, an glatten Wänden zu laufen?",
        answerA = "Klebrige Sekretion an den Zehen",
        answerB = "Van-der-Waals-Kräfte durch Milliarden winziger Haare",
        answerC = "Saugnäpfe an den Zehen",
        answerD = "Statische Elektrizität",
        correctAnswer = 1,
        explanation = "Geckos haften durch Van-der-Waals-Kräfte – intermolekulare Anziehungskräfte zwischen Milliarden winziger Haare (Setae) auf ihren Zehen und der Oberfläche.",
        difficulty = 4,
        funFact = "Die Haftfläche eines Geckos könnte theoretisch das 40-fache seines Körpergewichts tragen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das 'Red Queen'-Prinzip in der Evolutionsbiologie?",
        answerA = "Das Aussterben der stärksten Art durch Übernutzung",
        answerB = "Die kontinuierliche Co-Evolution von Räuber und Beute",
        answerC = "Die Dominanz weiblicher Tiere in Matriarchaten",
        answerD = "Die evolutionäre Stagnation in stabilen Ökosystemen",
        correctAnswer = 1,
        explanation = "Das Red-Queen-Prinzip beschreibt, wie Arten sich ständig weiterentwickeln müssen, nur um im evolutionären Wettrüsten mit anderen Arten mitzuhalten – analog zu Alices Lauf im Wunderland.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier kann bis zu 3,8 km Wassertiefe überleben und dabei dem extremen Druck standhalten?",
        answerA = "Blauwal",
        answerB = "Pottwal",
        answerC = "Schnabelwal (Ziphius cavirostris)",
        answerD = "Riesenkalmar",
        correctAnswer = 2,
        explanation = "Ziphius cavirostris (Cuvier-Schnabelwal) hält mit Tauchgängen bis 3.800 m Tiefe und Tauchzeiten über 3,5 Stunden den Rekord unter den Meeressäugetieren.",
        difficulty = 4,
        funFact = "Beim Auftauchen nach Tieftauchgängen müssen Schnabelwale langsam aufsteigen, um Dekompressionskrankheit zu vermeiden."
    ),

    Question(
        categoryId = 9,
        questionText = "Was bezeichnet man als 'Müller'sche Mimikry'?",
        answerA = "Eine harmlose Art ahmt eine gefährliche nach",
        answerB = "Mehrere giftige Arten ähneln sich gegenseitig",
        answerC = "Ein Räuber tarnt sich als Beute",
        answerD = "Eine Art imitiert unbelebte Objekte",
        correctAnswer = 1,
        explanation = "Müller'sche Mimikry liegt vor, wenn mehrere tatsächlich giftige oder ungenießbare Arten ein ähnliches Warnmuster entwickeln – zum gemeinsamen Schutz gegenüber Fressfeinden.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Tierart hat die höchste bekannte Anzahl an Chromosomen?",
        answerA = "Mensch (46)",
        answerB = "Hund (78)",
        answerC = "Krallenfarn-Schmetterlingsling (Polyommatus icarus) mit 448",
        answerD = "Adlerfarn (kein Tier)",
        correctAnswer = 2,
        explanation = "Einige Schmetterlingsarten haben extrem viele Chromosomen. Beim Bläuling Polyommatus icarus wurden bis zu 448 Chromosomen gezählt – eine außergewöhnlich hohe Zahl.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Funktion der Jacobsonschen Organe bei Schlangen?",
        answerA = "Seitenlinienorgan zum Spüren von Vibrationen",
        answerB = "Wärmewahrnehmung zur Jagd",
        answerC = "Chemisches Sinnesorgan zum Riechen und Schmecken von Luftpartikeln",
        answerD = "Gleichgewichtsorgan",
        correctAnswer = 2,
        explanation = "Das Jacobsonsche Organ (Vomeronasalorgan) im Gaumen ermöglicht Schlangen, Geruchspartikel aus der Luft chemisch zu analysieren – ihr Zungen-Flimmern sammelt diese Partikel.",
        difficulty = 4,
        funFact = "Schlangen 'sehen' ihre Beute durch eine Kombination aus Zungenschmecken, Wärmewahrnehmung und Sehvermögen."
    ),

    // ── MASTER (difficulty = 5) ── ~5 questions ──────────────────────────────

    Question(
        categoryId = 9,
        questionText = "Was bezeichnet der Begriff 'Eigentumsmarkierung' durch Infraschall bei Elefanten?",
        answerA = "Elefanten nutzen Ultraschall zur Kommunikation über große Distanzen",
        answerB = "Elefanten produzieren tieffrequente Laute unter 20 Hz, die durch den Boden übertragen werden",
        answerC = "Elefanten erzeugen Schallwellen durch Trampeln auf dem Boden",
        answerD = "Elefanten kommunizieren durch elektrische Felder im Boden",
        correctAnswer = 1,
        explanation = "Elefanten produzieren Infraschall (unter 20 Hz) durch Kehlkopfschwingungen, der Hunderte von Kilometern durch Boden und Luft übertragen wird. Artgenossen empfangen ihn über empfindliche Fußsohlen.",
        difficulty = 5,
        funFact = "Elefantenkommunikation durch Infraschall wurde erst in den 1980er-Jahren von Katy Payne entdeckt, die auch die Infraschall-Kommunikation bei Walen erforscht hatte."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus ermöglicht dem Mantis-Krebses (Fangschreckenkrebs) den tödlichsten Schlag im Tierreich?",
        answerA = "Muskelkontraktion mit höchster bekannter Beschleunigung im Tierreich",
        answerB = "Einlagerung von Energie in einem Federmechanismus aus Exoskelett-Material",
        answerC = "Chemische Explosion durch Mischung von Sekreten",
        answerD = "Kavitationsblase durch Beschleunigung über die Schallgeschwindigkeit im Wasser",
        correctAnswer = 1,
        explanation = "Mantis-Krebse laden ihre Schläger wie eine Feder. Die Energie wird im saddle-förmigen Exoskelett gespeichert und innerhalb von 2,7 Millisekunden freigesetzt – erzeugt kurz Temperaturen wie auf der Sonnenoberfläche durch Kavitation.",
        difficulty = 5,
        funFact = "Der Schlag des Mantis-Krebses ist so kraftvoll, dass er Aquarienglas zerbrechen kann."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das 'Handicap-Prinzip' in der sexuellen Selektion nach Zahavi?",
        answerA = "Tiere wählen Partner mit möglichst symmetrischen Körpermerkmalen",
        answerB = "Kostspielige, hinderliche Merkmale signalisieren ehrlich genetische Qualität",
        answerC = "Weibchen bevorzugen stets die größten Männchen ihrer Art",
        answerD = "Tiere passen ihr Verhalten an Umweltbedingungen an",
        correctAnswer = 1,
        explanation = "Amotz Zahavi postulierte: Nur genetisch überlegene Individuen können sich kostspielige Merkmale (wie Pfauenrad, Hirschgeweih) leisten. Das Handicap beweist gerade dadurch die genetische Qualität.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher evolutionäre Prozess erklärt die Entstehung der Mitochondrien in eukaryotischen Zellen?",
        answerA = "Virale Integration",
        answerB = "Endosymbiose – Aufnahme eines Alphaproteobakteriums",
        answerC = "Horizontaler Gentransfer aus Archaeen",
        answerD = "Spontane Membranevolution",
        correctAnswer = 1,
        explanation = "Laut Endosymbiontentheorie (Lynn Margulis) wurden Mitochondrien durch die Aufnahme eines Alphaproteobakteriums in eine Wirtszelle vor ca. 1,5 Milliarden Jahren gebildet.",
        difficulty = 5,
        funFact = "Mitochondrien haben noch immer eine eigene kreisförmige DNA und teilen sich unabhängig von der Zelle – als Relikt ihrer Herkunft als freie Bakterien."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das 'Müller-Haeckel-Prinzip' und welches Tier widerlegt es am deutlichsten?",
        answerA = "Die Ontogenese rekapituliert die Phylogenese; der Schnabeligel widerlegt es durch fehlende Embryonalstadien",
        answerB = "Alle Tiere durchlaufen in der Embryonalentwicklung ähnliche Stadien; Bandwürmer widerlegen es durch direkte Entwicklung",
        answerC = "Alle Arten streben nach Komplexitätszunahme; sessile Tiere widerlegen es",
        answerD = "Verwandte Arten haben ähnliche Verhaltensweisen; Parasiten widerlegen es durch Verhaltensreduktion",
        correctAnswer = 1,
        explanation = "Das biogenetische Grundgesetz ('Ontogenese rekapituliert Phylogenese') gilt nur eingeschränkt. Bandwürmer und andere Parasiten zeigen direkte Entwicklung ohne ancestrale Larvenstadien.",
        difficulty = 5,
        funFact = null
    ),

    // ── NEW EASY (difficulty = 1) ── 15 questions ─────────────────────────────

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist das schnellste Landtier der Welt?",
        answerA = "Löwe",
        answerB = "Gepard",
        answerC = "Gazelle",
        answerD = "Gepard",
        correctAnswer = 1,
        explanation = "Der Gepard ist das schnellste Landtier und kann Geschwindigkeiten von bis zu 120 km/h erreichen.",
        difficulty = 1,
        funFact = "Ein Gepard beschleunigt in etwa 3 Sekunden von 0 auf 100 km/h – schneller als die meisten Sportwagen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie viele Beine hat eine Spinne?",
        answerA = "6",
        answerB = "8",
        answerC = "10",
        answerD = "12",
        correctAnswer = 1,
        explanation = "Spinnen sind Spinnentiere und haben 8 Beine. Insekten hingegen haben nur 6 Beine.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist ein Säugetier?",
        answerA = "Krokodil",
        answerB = "Schildkröte",
        answerC = "Delfin",
        answerD = "Thunfisch",
        correctAnswer = 2,
        explanation = "Der Delfin ist ein Säugetier: Er atmet mit Lungen, ist warmblütig und säugt seine Jungtiere.",
        difficulty = 1,
        funFact = "Delfine müssen regelmäßig an die Oberfläche kommen, um Luft zu holen – sie können nicht unter Wasser atmen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Jungtier einer Katze?",
        answerA = "Welpe",
        answerB = "Küken",
        answerC = "Kätzchen",
        answerD = "Fohlen",
        correctAnswer = 2,
        explanation = "Das Jungtier einer Katze heißt Kätzchen oder Kitten. Neugeborene Kätzchen öffnen ihre Augen erst nach etwa 10 Tagen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier produziert Wolle?",
        answerA = "Ziege",
        answerB = "Schaf",
        answerC = "Kuh",
        answerD = "Schwein",
        correctAnswer = 1,
        explanation = "Schafe werden geschoren, um ihre Wolle zu gewinnen. Die Wolle wächst jedes Jahr nach und muss regelmäßig geschoren werden.",
        difficulty = 1,
        funFact = "Ein Schaf liefert pro Jahr durchschnittlich 2–4 kg Wolle – genug für etwa zwei Pullover."
    ),

    Question(
        categoryId = 9,
        questionText = "Was fressen Pandas?",
        answerA = "Fleisch",
        answerB = "Früchte",
        answerC = "Bambus",
        answerD = "Insekten",
        correctAnswer = 2,
        explanation = "Große Pandas ernähren sich zu fast 99% von Bambus. Sie müssen täglich bis zu 15 kg Bambus fressen, da er sehr nährstoffarm ist.",
        difficulty = 1,
        funFact = "Obwohl Pandas Fleischfresser-Verdauung haben, fressen sie fast ausschließlich Bambus."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie heißt das weibliche Rind?",
        answerA = "Stute",
        answerB = "Kuh",
        answerC = "Henne",
        answerD = "Sau",
        correctAnswer = 1,
        explanation = "Das weibliche Rind heißt Kuh. Der männliche Stier heißt Bulle, das Jungtier Kalb.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier baut Dämme aus Holz und Schlamm?",
        answerA = "Otter",
        answerB = "Biber",
        answerC = "Waschbär",
        answerD = "Murmeltier",
        correctAnswer = 1,
        explanation = "Biber bauen Dämme aus Ästen, Holzstämmen und Schlamm, um Teiche zu schaffen, in denen sie ihre Burgen sicher anlegen können.",
        difficulty = 1,
        funFact = "Biberdämme können bis zu 100 Meter lang werden und verändern ganze Landschaften."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier trägt sein Junges im Beutel?",
        answerA = "Koala",
        answerB = "Känguru",
        answerC = "Wombat",
        answerD = "Alle genannten",
        correctAnswer = 3,
        explanation = "Känguru, Koala und Wombat sind alle Beuteltiere. Ihre unreif geborenen Jungtiere entwickeln sich im Beutel der Mutter weiter.",
        difficulty = 1,
        funFact = "Ein neugeborenes Känguru ist nur etwa 2 cm groß – so klein wie eine Bohne."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie viele Höcker hat ein Dromedar?",
        answerA = "Keinen",
        answerB = "Einen",
        answerC = "Zwei",
        answerD = "Drei",
        correctAnswer = 1,
        explanation = "Das Dromedar hat einen Höcker, das Trampeltier (Baktrisches Kamel) hat zwei Höcker. Im Höcker wird Fett gespeichert, kein Wasser.",
        difficulty = 1,
        funFact = "Ein erschöpftes Kamel hat einen schlaffen Höcker – das Fett wurde aufgebraucht."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist für seine schwarzweißen Streifen bekannt?",
        answerA = "Gepard",
        answerB = "Zebra",
        answerC = "Gnu",
        answerD = "Impala",
        correctAnswer = 1,
        explanation = "Zebras sind für ihr schwarz-weißes Streifenmuster bekannt. Jedes Zebra hat ein einzigartiges Streifenmuster, ähnlich wie menschliche Fingerabdrücke.",
        difficulty = 1,
        funFact = "Forscher diskutieren noch, ob Zebras schwarze Tiere mit weißen Streifen oder weiße mit schwarzen Streifen sind."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist ein Oktopus?",
        answerA = "Ein Fisch",
        answerB = "Ein Weichtier",
        answerC = "Ein Krebstier",
        answerD = "Ein Stachelhäuter",
        correctAnswer = 1,
        explanation = "Der Oktopus (Krake) ist ein Weichtier aus der Klasse der Kopffüßer. Er ist eng mit Tintenfischen und Nautilussen verwandt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel kann nicht fliegen und ist der größte lebende Vogel?",
        answerA = "Pinguin",
        answerB = "Kiwi",
        answerC = "Strauß",
        answerD = "Nandu",
        correctAnswer = 2,
        explanation = "Der Strauß ist der größte lebende Vogel und kann nicht fliegen. Er kann jedoch bis zu 70 km/h rennen.",
        difficulty = 1,
        funFact = "Strauße haben die größten Augen aller Landtiere – größer als ihr eigenes Gehirn."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Insekt verwandelt sich von einer Raupe zu einem Schmetterling?",
        answerA = "Motte",
        answerB = "Tagfalter",
        answerC = "Beide (A und B)",
        answerD = "Libelle",
        correctAnswer = 2,
        explanation = "Sowohl Tagfalter als auch Motten durchlaufen eine vollständige Verwandlung (Metamorphose): Ei → Raupe → Puppe → Falter.",
        difficulty = 1,
        funFact = "Die Puppe des Schmetterlings heißt Chrysalis. In ihr löst sich die Raupe fast vollständig auf, bevor der Falter entsteht."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man eine Gruppe von Wölfen?",
        answerA = "Rudel",
        answerB = "Herde",
        answerC = "Schwarm",
        answerD = "Meute",
        correctAnswer = 0,
        explanation = "Eine Gruppe von Wölfen heißt Rudel. Ein Rudel besteht meist aus einem Elternpaar (Alphapaar) und ihren Nachkommen.",
        difficulty = 1,
        funFact = "Wölfe kommunizieren durch Heulen, um ihr Rudel zusammenzuhalten und ihr Revier zu markieren."
    ),

    // ── NEW MEDIUM (difficulty = 2) ── 20 questions ───────────────────────────

    Question(
        categoryId = 9,
        questionText = "Welches Organ nutzen Fledermäuse zur Navigation?",
        answerA = "Infrarotwahrnehmung",
        answerB = "Echoortung (Ultraschall)",
        answerC = "Elektrorezeption",
        answerD = "Magnetrezeptoren in der Schnauze",
        correctAnswer = 1,
        explanation = "Fledermäuse senden Ultraschallwellen aus und hören deren Echo. So können sie Insekten und Hindernisse in totaler Dunkelheit präzise orten.",
        difficulty = 2,
        funFact = "Einige Fledermausarten senden Rufe mit bis zu 120 Dezibel – lauter als ein Presslufthammer."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie schläft ein Delfin?",
        answerA = "Auf dem Meeresgrund liegend",
        answerB = "Mit einer Gehirnhälfte schlafen, die andere bleibt wach",
        answerC = "Schwimmend an der Oberfläche für maximal 2 Stunden",
        answerD = "In Höhlen am Meeresgrund",
        correctAnswer = 1,
        explanation = "Delfine praktizieren den unihemisphärischen Schlaf: Eine Gehirnhälfte schläft, die andere bleibt aktiv. So können sie weiter schwimmen und atmen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist ein Marsupial?",
        answerA = "Ein Raubtier aus Australien",
        answerB = "Ein Beuteltier",
        answerC = "Ein flugunfähiger Vogel",
        answerD = "Ein wasserbewohnendes Säugetier",
        correctAnswer = 1,
        explanation = "Marsupialier (Beuteltiere) sind Säugetiere, deren Jungtiere unreif geboren werden und sich im Beutel der Mutter weiterentwickeln.",
        difficulty = 2,
        funFact = "Fast alle Beuteltiere leben auf der Südhalbkugel – vor allem in Australien und Südamerika."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat das größte Herz (absolut) aller Lebewesen?",
        answerA = "Afrikanischer Elefant",
        answerB = "Blauwal",
        answerC = "Riesenschildkröte",
        answerD = "Buckelwal",
        correctAnswer = 1,
        explanation = "Das Herz des Blauwals ist das größte aller lebenden Tiere – es wiegt etwa 180 kg und ist so groß wie ein kleines Auto.",
        difficulty = 2,
        funFact = "Durch die Aorta des Blauwals könnte ein kleines Kind hindurchkrabbeln."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen Schmetterlingslarven (Raupen) und Maden?",
        answerA = "Raupen haben Beine, Maden nicht",
        answerB = "Maden haben Beine, Raupen nicht",
        answerC = "Es gibt keinen Unterschied",
        answerD = "Raupen leben im Wasser, Maden an Land",
        correctAnswer = 0,
        explanation = "Schmetterlingsraupen haben echte Beine (3 Paar Thoraxbeine) und Bauchfüße (Prolegs). Fliegenlarven (Maden) sind beinlos.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat kein natürliches Fressfeind in seiner Heimat?",
        answerA = "Kaninchen",
        answerB = "Nilkrokodil",
        answerC = "Eisbär",
        answerD = "Polarfuchs",
        correctAnswer = 2,
        explanation = "Erwachsene Eisbären haben in der Arktis keine natürlichen Fressfeinde. Sie stehen an der Spitze der arktischen Nahrungskette.",
        difficulty = 2,
        funFact = "Eisbären haben keine weißen Haare – ihre Haare sind farblos und hohl. Die Fellfarbe entsteht durch Lichtbrechung."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man den Vorgang, wenn eine Schlange ihre Haut abstreift?",
        answerA = "Häutung",
        answerB = "Metamorphose",
        answerC = "Mauserung",
        answerD = "Exuviation",
        correctAnswer = 0,
        explanation = "Das Abstreifen der alten Haut bei Schlangen und anderen Reptilien heißt Häutung. Schlangen häuten sich mehrmals im Jahr.",
        difficulty = 2,
        funFact = "Kurz vor der Häutung trüben sich die Augen der Schlange milchig – ein Zeichen, dass die alte Haut sich löst."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier produziert die giftigste bekannte Substanz in der Tierwelt?",
        answerA = "Kobra",
        answerB = "Würfelqualle",
        answerC = "Pfeilgiftfrosch",
        answerD = "Schwarze Witwe",
        correctAnswer = 1,
        explanation = "Die Würfelqualle (Chironex fleckeri) gilt als eines der giftigsten Tiere der Welt. Ihr Gift kann in weniger als 5 Minuten töten.",
        difficulty = 2,
        funFact = "Würfelquallen haben 24 Augen in 4 Gruppen, aber kein zentrales Gehirn."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man Tiere, die sowohl an Land als auch im Wasser leben?",
        answerA = "Reptilien",
        answerB = "Amphibien",
        answerC = "Aquatische Säugetiere",
        answerD = "Semiaquatische Tiere",
        correctAnswer = 1,
        explanation = "Amphibien (Lurche) leben typischerweise im Wasser (Larvalstadium) und an Land (Erwachsenenstadium). Dazu gehören Frösche, Kröten, Molche und Salamander.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel hat die größte Flügelspannweite aller lebenden Vögel?",
        answerA = "Kondor",
        answerB = "Wanderalbatros",
        answerC = "Pelikan",
        answerD = "Marabu",
        correctAnswer = 1,
        explanation = "Der Wanderalbatros hat mit bis zu 3,7 m die größte Flügelspannweite aller lebenden Vögel.",
        difficulty = 2,
        funFact = "Wanderalbatrosse schlafen im Gleitflug und legen jährlich bis zu 120.000 km zurück."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist ein Raubtier (Karnivore)?",
        answerA = "Ein Tier, das nur Pflanzen frisst",
        answerB = "Ein Tier, das sich hauptsächlich von anderen Tieren ernährt",
        answerC = "Ein Tier, das Aas frisst",
        answerD = "Ein Tier, das Insekten frisst",
        correctAnswer = 1,
        explanation = "Karnivore sind Tiere, die sich hauptsächlich von anderen Tieren (Fleisch) ernähren. Dazu gehören Löwen, Adler, Haie und viele andere.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist bekannt für seine Echolokation im Wasser?",
        answerA = "Seehund",
        answerB = "Wal",
        answerC = "Walross",
        answerD = "Seelöwe",
        correctAnswer = 1,
        explanation = "Zahnwale wie Delfine und Pottwale nutzen Echolokation: Sie senden Klicks aus und orten Objekte anhand des zurückkehrenden Echos.",
        difficulty = 2,
        funFact = "Pottwale produzieren die lautesten Laute aller Tiere – bis zu 230 Dezibel."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man Tiere, die nachts aktiv sind?",
        answerA = "Diurnal",
        answerB = "Nokturnal",
        answerC = "Krepuskulär",
        answerD = "Arrhythmisch",
        correctAnswer = 1,
        explanation = "Nachtaktive Tiere werden als nokturnal bezeichnet. Dazu gehören Eulen, Fledermäuse und viele Insekten. Tagaktive Tiere nennt man diurnal.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat die längste Zunge im Verhältnis zur Körpergröße?",
        answerA = "Chamäleon",
        answerB = "Ameisenbär",
        answerC = "Specht",
        answerD = "Chamäleon",
        correctAnswer = 0,
        explanation = "Chamäleons können ihre Zunge blitzschnell herausschleudern – sie ist oft länger als ihr gesamter Körper. Ein spezieller Zungenknochen dient als Abschussmechanismus.",
        difficulty = 2,
        funFact = "Die Zunge eines Chamäleons schießt mit einer Beschleunigung von bis zu 41 g vor – schneller als ein Kampfjet."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist ein Kaltblüter?",
        answerA = "Ein Tier, das in kalten Regionen lebt",
        answerB = "Ein Tier, dessen Körpertemperatur von der Umgebungstemperatur abhängt",
        answerC = "Ein Tier, das im Winter Winterschlaf hält",
        answerD = "Ein Tier mit niedriger Herzfrequenz",
        correctAnswer = 1,
        explanation = "Kaltblüter (Poikilotherme) regulieren ihre Körpertemperatur nicht selbst – sie nehmen die Temperatur ihrer Umgebung an. Dazu gehören Reptilien, Fische und Amphibien.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat einen einzigartigen Fingerabdruck ähnlich wie Menschen?",
        answerA = "Gorilla",
        answerB = "Koala",
        answerC = "Schimpanse",
        answerD = "Orang-Utan",
        correctAnswer = 1,
        explanation = "Koalas haben Fingerabdrücke, die menschlichen so ähnlich sind, dass sie forensisch kaum zu unterscheiden sind – eine der wenigen nicht-menschlichen Arten mit solchen Merkmalen.",
        difficulty = 2,
        funFact = "Auch Gorillas und Schimpansen haben Fingerabdrücke, aber die des Koalas ähneln menschlichen am stärksten."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man Tiere, die sich von Aas ernähren?",
        answerA = "Herbivore",
        answerB = "Omnivore",
        answerC = "Aasfresser (Scavenger)",
        answerD = "Detritivore",
        correctAnswer = 2,
        explanation = "Aasfresser (Scavenger) ernähren sich von toten und verwesenden Tieren. Geier, Hyänen und Krähen sind bekannte Beispiele.",
        difficulty = 2,
        funFact = "Geier haben eine hochentwickelte Magensäure, die selbst gefährliche Krankheitserreger wie Milzbrand abtötet."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat die empfindlichste Nase im Tierreich?",
        answerA = "Hund",
        answerB = "Bär",
        answerC = "Elefant",
        answerD = "Haifisch",
        correctAnswer = 2,
        explanation = "Bären haben den schärfsten Geruchssinn aller Landsäugetiere – bis zu 2.100 Mal feiner als beim Menschen. Sie können Nahrungsquellen aus über 30 km Entfernung riechen.",
        difficulty = 2,
        funFact = "Der Haifisch gilt dagegen als der empfindlichste Geruchssinn unter den Meerestieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Parthenogenese im Tierreich?",
        answerA = "Geschlechtliche Fortpflanzung durch externe Befruchtung",
        answerB = "Ungeschlechtliche Fortpflanzung ohne Befruchtung durch ein Männchen",
        answerC = "Zweigeschlechtliche Fortpflanzung bei Zwittern",
        answerD = "Klonierung durch Zellteilung",
        correctAnswer = 1,
        explanation = "Parthenogenese ist die Entwicklung eines Organismus aus einer unbefruchteten Eizelle. Komodo-Warane, Haie und einige Insekten können sich so fortpflanzen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Tiere bilden die größten Wanderherden der Welt?",
        answerA = "Elefanten in Botswana",
        answerB = "Gnus in der Serengeti",
        answerC = "Rentiere in Kanada",
        answerD = "Karibu in Sibirien",
        correctAnswer = 1,
        explanation = "Die Große Gnu-Wanderung in der Serengeti (Tansania/Kenia) ist die größte Landsäuger-Wanderung der Welt – bis zu 1,5 Millionen Gnus bewegen sich jährlich im Kreis.",
        difficulty = 2,
        funFact = "Auf ihrer Wanderung überqueren die Gnus den Krokodil-verseuchten Mara-Fluss – eines der gefährlichsten Naturschauspiele Afrikas."
    ),

    // ── NEW HARD (difficulty = 3) ── 15 questions ─────────────────────────────

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter 'Allopatrie' in der Evolutionsbiologie?",
        answerA = "Artbildung durch geografische Trennung von Populationen",
        answerB = "Artbildung innerhalb desselben Lebensraums durch ökologische Nischen",
        answerC = "Das Aussterben einer Art durch Konkurrenz",
        answerD = "Die Rückkehr einer Art in ihr ursprüngliches Verbreitungsgebiet",
        correctAnswer = 0,
        explanation = "Allopatrische Artbildung entsteht, wenn Populationen geografisch getrennt werden und sich unabhängig voneinander entwickeln, bis sie nicht mehr erfolgreich interbreeden können.",
        difficulty = 3,
        funFact = "Die Darwin-Finken auf den Galapagos-Inseln sind ein klassisches Beispiel für allopatrische Artbildung."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man die spezielle Zelle, mit der Nesseltiere (Cnidaria) ihre Beute lähmen?",
        answerA = "Nesselzelle (Cnidozyt/Nematozyt)",
        answerB = "Giftdrüsenzelle",
        answerC = "Phagozyt",
        answerD = "Chromatophor",
        correctAnswer = 0,
        explanation = "Cnidozyten (Nesselzellen) enthalten eine Stechkapsel (Nematocyst). Bei Berührung wird ein giftiger Faden mit bis zu 5 m/s herausgeschossen – der schnellste biologische Mechanismus.",
        difficulty = 3,
        funFact = "Eine Qualle löst die Nesseln auch noch nach dem Tod aus – tote Quallen am Strand können noch stechen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Merkmal kennzeichnet Fische der Klasse Chondrichthyes?",
        answerA = "Knöchernes Skelett",
        answerB = "Knorpeliges Skelett ohne echte Knochen",
        answerC = "Keine Kiemen",
        answerD = "Lungen statt Kiemen",
        correctAnswer = 1,
        explanation = "Chondrichthyes (Knorpelfische) wie Haie, Rochen und Chimären haben ein vollständig knorpeliges Skelett. Im Gegensatz dazu haben Osteichthyes (Knochenfische) ein verknöchertes Skelett.",
        difficulty = 3,
        funFact = "Haie haben kein Schwimmblasenorgan und müssen ständig schwimmen, um nicht abzusinken."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Besondere an der Fortpflanzung des Seepferdchens?",
        answerA = "Das Weibchen bebrütet die Eier im Mund",
        answerB = "Das Männchen trägt die Eier in einem Beutel und gebiert die Jungtiere",
        answerC = "Seepferdchen sind Hermaphroditen",
        answerD = "Die Eier werden im offenen Wasser abgelegt",
        correctAnswer = 1,
        explanation = "Beim Seepferdchen übernimmt das Männchen die Schwangerschaft: Das Weibchen legt Eier in den Beutel des Männchens, wo sie befruchtet und ausgetragen werden.",
        difficulty = 3,
        funFact = "Ein Seepferdchen-Männchen kann bis zu 2.000 Jungtiere in einer Geburt auf einmal zur Welt bringen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Gift produziert der Pfeilgiftfrosch selbst nicht – woher kommt es?",
        answerA = "Aus speziellen Giftdrüsen in der Haut",
        answerB = "Aus der Nahrung (Insekten mit Alkaloiden)",
        answerC = "Aus einer Symbiose mit Bakterien",
        answerD = "Es wird in der Leber produziert",
        correctAnswer = 1,
        explanation = "Pfeilgiftfrösche akkumulieren ihr Gift aus ihrer Nahrung – vor allem aus Ameisen, Käfern und Milben, die pflanzliche Alkaloide enthalten. In Gefangenschaft ohne diese Nahrung werden sie ungiftig.",
        difficulty = 3,
        funFact = "Indigene Völker Südamerikas nutzten das Batrachotoxin aus Pfeilgiftfröschen für vergiftete Blasrohrpfeile."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen Homologie und Analogie im Tierreich?",
        answerA = "Homologe Organe haben gleiche Funktion, analoge haben gemeinsamen Ursprung",
        answerB = "Homologe Organe haben gemeinsamen evolutionären Ursprung, analoge haben gleiche Funktion durch konvergente Evolution",
        answerC = "Homologien betreffen Verhaltensweisen, Analogien betreffen körperliche Merkmale",
        answerD = "Es gibt keinen relevanten Unterschied",
        correctAnswer = 1,
        explanation = "Homologe Organe haben den gleichen evolutionären Ursprung (z.B. Menschenhand und Flosse des Wals). Analoge Organe haben durch konvergente Evolution ähnliche Funktion, aber verschiedenen Ursprung (z.B. Insekten- und Vogelflügel).",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Vogelart ist bekannt dafür, dass sie komplexe Werkzeuge aus Haken und Haften herstellt?",
        answerA = "Tannenhäher",
        answerB = "Neukaledonische Krähe (Corvus moneduloides)",
        answerC = "Grüner Specht",
        answerD = "Papagei",
        correctAnswer = 1,
        explanation = "Neukaledonische Krähen stellen aus Blättern und Zweigen Haken-Werkzeuge her, um Insektenlarven aus Rindenspalten zu angeln – eine der komplexesten Werkzeugnutzungen im Tierreich.",
        difficulty = 3,
        funFact = "Diese Krähen lösen mehrstufige Problemstellungen, die sogar für Kleinkinder herausfordernd sind."
    ),

    Question(
        categoryId = 9,
        questionText = "Was bezeichnet der Begriff 'Torpor' bei Tieren?",
        answerA = "Einen tiefen Schlafzustand identisch mit Winterschlaf",
        answerB = "Einen kurzfristigen Zustand reduzierter Körperfunktionen und Temperatur",
        answerC = "Einen dauerhaften Ruhezustand bei Wirbellosen",
        answerD = "Die sommerliche Version des Winterschlafs",
        correctAnswer = 1,
        explanation = "Torpor ist ein kurzfristiger Energiesparzustand mit stark reduzierter Körpertemperatur und Stoffwechsel. Er unterscheidet sich vom Winterschlaf durch seine Kürze und Umkehrbarkeit in Minuten.",
        difficulty = 3,
        funFact = "Kolibris fallen jede Nacht in Torpor – ohne diesen würden sie ihre Energiereserven über Nacht erschöpfen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man die Pheromon-Kommunikation bei Ameisen, die Wegmarkierungen hinterlässt?",
        answerA = "Trophallaxis",
        answerB = "Tandem Running",
        answerC = "Pheromonspur (Trail Pheromone)",
        answerD = "Rekrutierungstanz",
        correctAnswer = 2,
        explanation = "Ameisen hinterlassen chemische Duftspuren aus Pheromonen, die andere Arbeiterinnen zur Nahrungsquelle leiten. Je stärker die Spur, desto mehr Ameisen folgen ihr.",
        difficulty = 3,
        funFact = "Trophallaxis bezeichnet den Nahrungsaustausch zwischen Ameisen durch Mund-zu-Mund-Weitergabe von verdauten Substanzen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat tatsächlich vier Gehirne (oder das Äquivalent)?",
        answerA = "Krake (Oktopus)",
        answerB = "Tintenfisch",
        answerC = "Blauwal",
        answerD = "Seestern",
        correctAnswer = 0,
        explanation = "Kraken haben ein zentrales Gehirn plus je ein Nervenganglion in jedem der 8 Tentakel – die Tentakel agieren damit weitgehend autonom und verarbeiten Informationen unabhängig vom Zentralgehirn.",
        difficulty = 3,
        funFact = "Etwa zwei Drittel der Neuronen eines Krakens befinden sich in seinen Tentakeln, nicht im Gehirn."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Viviparie' in der Zoologie?",
        answerA = "Lebendgeburt mit Placenta-ähnlicher Nährstoffversorgung",
        answerB = "Eierlegung mit anschließender Bebrütung",
        answerC = "Brutpflege im Maul",
        answerD = "Entwicklung der Jungen im Mutterleib ohne Placenta",
        correctAnswer = 0,
        explanation = "Viviparie bezeichnet die echte Lebendgeburt, bei der der Embryo über eine Placenta oder ähnliche Struktur mit Nährstoffen versorgt wird. Die meisten Säugetiere sind vivipar.",
        difficulty = 3,
        funFact = "Einige Hai-Arten sind ebenfalls vivipar – der Sandtigerhai zeigt sogar intrauterinen Kannibalismus."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier setzt bei Gefahr eine blaue Flüssigkeit aus seinen Poren frei?",
        answerA = "Krake",
        answerB = "Blauringkrake",
        answerC = "Schleimaal",
        answerD = "Blaue Pfeilgiftfrösche",
        correctAnswer = 2,
        explanation = "Der Schleimaal (Myxine) produziert bei Bedrohung enorme Mengen zähen Schleim aus Hunderten von Drüsen. Dieser Schleim kann die Kiemen von Fressfeinden verkleben.",
        difficulty = 3,
        funFact = "Aus dem Schleim des Schleimaaals entwickeln Forscher neue Materialien – er ist elastischer als Nylon und zäher als Kevlar."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie lange kann ein Grönlandwal (Bowhead Whale) leben?",
        answerA = "Bis zu 70 Jahre",
        answerB = "Bis zu 100 Jahre",
        answerC = "Bis zu 150 Jahre",
        answerD = "Über 200 Jahre",
        correctAnswer = 3,
        explanation = "Grönlandwale sind die ältesten bekannten Säugetiere – Individuen über 200 Jahre wurden dokumentiert. In Walkörpern wurden Harpunenspitzen aus dem 19. Jahrhundert gefunden.",
        difficulty = 3,
        funFact = "Grönlandwale haben die dickste Speckschicht aller Wale – bis zu 50 cm Isolierung."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen einem Horn und einem Geweih?",
        answerA = "Hörner sind aus Knochen, Geweihe aus Keratin",
        answerB = "Hörner wachsen nicht nach, Geweihe werden jährlich abgeworfen und neu gebildet",
        answerC = "Geweihe haben keine Knochenbasis, Hörner schon",
        answerD = "Nur Männchen haben Hörner, nur Weibchen Geweihe",
        correctAnswer = 1,
        explanation = "Hörner (z.B. bei Rindern) bestehen aus einem Knochenzapfen mit einer Keratinhülle und wachsen lebenslang. Geweihe (z.B. bei Hirschen) sind reine Knochengebilde, werden jährlich abgeworfen und neu gebildet.",
        difficulty = 3,
        funFact = "Geweihe sind das am schnellsten wachsende Körpergewebe bei Säugetieren – bis zu 2,5 cm pro Tag."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Phänomen beschreibt den 'Müller'schen Körper' bei Ameisenpflanzen?",
        answerA = "Giftstoff zur Abwehr von Insekten",
        answerB = "Nährstoffreiche Körper als Nahrung für Symbiose-Ameisen",
        answerC = "Extrafloreale Nektarien an den Blättern",
        answerD = "Hohlräume als Nestplatz für Ameisenkolonien",
        correctAnswer = 1,
        explanation = "Müller'sche Körper sind eiweißreiche Strukturen an bestimmten Pflanzen (z.B. Cecropia), die als Nahrung für Schutzameisen dienen. Die Ameisen verteidigen im Gegenzug die Pflanze vor Herbivoren.",
        difficulty = 3,
        funFact = "Diese Wechselbeziehung ist ein klassisches Beispiel für Mutualismus – beide Partner profitieren voneinander."
    ),

    // ── NEW MEDIUM BATCH 2 (difficulty = 2) ── 15 questions ──────────────────

    Question(
        categoryId = 9,
        questionText = "Welches Tier schläft stehend und kann dabei tatsächlich einnicken?",
        answerA = "Elefant",
        answerB = "Pferd",
        answerC = "Giraffe",
        answerD = "Nilpferd",
        correctAnswer = 1,
        explanation = "Pferde können im Stehen dösen, weil ein spezielles Sehnen-Bänder-System (Schlafstellung) ihre Gelenke blockiert. Für den Tiefschlaf legen sie sich jedoch hin.",
        difficulty = 2,
        funFact = "Pferde liegen pro Tag nur etwa 2–3 Stunden – zu langes Liegen wäre für ihre Muskeln schädlich."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Phänomen, wenn Vögel in einer V-Formation fliegen?",
        answerA = "Synchronflug",
        answerB = "Aerodinamischer Gruppenflug",
        answerC = "Windschatten-Formation",
        answerD = "Aerodynamischer Auftriebseffekt (Formationsflug)",
        correctAnswer = 3,
        explanation = "In V-Formation nutzen Vögel den Auftriebswirbel des Vordervogels. Jeder nachfolgende Vogel spart so bis zu 20% Energie. Die Tiere wechseln regelmäßig die Spitzenposition.",
        difficulty = 2,
        funFact = "Zugvögel können in V-Formation Strecken zurücklegen, die ohne Formation physisch unmöglich wären."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat keine Stimmbänder und kommuniziert dennoch durch Lautgebung?",
        answerA = "Giraffe",
        answerB = "Krokodil",
        answerC = "Schlange",
        answerD = "Schildkröte",
        correctAnswer = 2,
        explanation = "Schlangen haben keine Stimmbänder, erzeugen aber durch Zischen Laute – indem sie Luft durch die Glottis (Stimmritze) pressen. Das Zischen dient der Abschreckung.",
        difficulty = 2,
        funFact = "Giraffen galten lange als stumm, kommunizieren aber tatsächlich durch Infraschall unter 20 Hz."
    ),

    Question(
        categoryId = 9,
        questionText = "Was fressen erwachsene Schmetterlinge hauptsächlich?",
        answerA = "Blätter",
        answerB = "Blütenstaub (Pollen)",
        answerC = "Nektar",
        answerD = "Insekten",
        correctAnswer = 2,
        explanation = "Erwachsene Schmetterlinge ernähren sich hauptsächlich von Nektar, den sie mit ihrem Saugrüssel (Proboscis) aus Blüten aufnehmen. Einige Arten trinken auch Baumsäfte oder fressen faulende Früchte.",
        difficulty = 2,
        funFact = "Schmetterlinge schmecken mit ihren Füßen – auf den Fußsohlen sitzen Geschmacksrezeptoren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat ein natürliches Sonar, das so präzise ist, dass es Fische unter Sand aufspüren kann?",
        answerA = "Delfin",
        answerB = "Zitterrochen",
        answerC = "Großer Weißer Hai",
        answerD = "Tümmler",
        correctAnswer = 0,
        explanation = "Delfine nutzen ihr Echolokations-Sonar so präzise, dass sie vergrabene Beute im Meeresboden aufspüren können. Das Melonenorgan im Kopf bündelt die Ultraschallwellen.",
        difficulty = 2,
        funFact = "Die Echolokation von Delfinen kann sogar den Knochenaufbau von Fischen erkennen – ähnlich wie ein Ultraschallgerät."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist der einzige Wirbellose, dem Selbsterkennung im Spiegel nachgewiesen wurde?",
        answerA = "Tintenfisch",
        answerB = "Krake",
        answerC = "Putzerlippfisch (Labroides dimidiatus)",
        answerD = "Hornisse",
        correctAnswer = 2,
        explanation = "Der Putzerlippfisch bestand den Spiegeltest – er erkannte Markierungen an seinem eigenen Körper und versuchte, sie zu entfernen. Damit ist er der erste bekannte Fisch mit Selbstwahrnehmung.",
        difficulty = 2,
        funFact = "Der Spiegeltest gilt als Hinweis auf Selbstbewusstsein. Ihn bestehen sonst nur Schimpansen, Gorillas, Delfine, Elefanten und Elstern."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie lange kann ein Kamel ohne Wasser überleben?",
        answerA = "3 Tage",
        answerB = "1 Woche",
        answerC = "Bis zu 2 Wochen",
        answerD = "Bis zu einem Monat",
        correctAnswer = 2,
        explanation = "Kamele können je nach Bedingungen bis zu zwei Wochen ohne Wasser überleben. Sie speichern Fett (nicht Wasser) im Höcker und können ihren Wasserverlust extrem reduzieren.",
        difficulty = 2,
        funFact = "Ein dürstiges Kamel kann auf einmal bis zu 200 Liter Wasser trinken – innerhalb weniger Minuten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Eigenschaft macht den Axolotl (Ambystoma mexicanum) biologisch besonders?",
        answerA = "Er kann Farbe wechseln wie ein Chamäleon",
        answerB = "Er bleibt zeitlebens im Larvenstadium (Neotenie)",
        answerC = "Er ist das einzige Amphibium mit warmem Blut",
        answerD = "Er lebt ausschließlich von Licht durch Photosynthese-Symbionten",
        correctAnswer = 1,
        explanation = "Der Axolotl zeigt Neotenie: Er behält zeitlebens Larvenmerkmale (äußere Kiemen, Flossensaum) und wird geschlechtsreif, ohne sich in eine adulte Form zu verwandeln.",
        difficulty = 2,
        funFact = "Axolotl können verlorene Gliedmaßen, Teile des Herzens und sogar Gehirngewebe vollständig regenerieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat das stärkste Gebiss im Verhältnis zur Körpergröße?",
        answerA = "Krokodil",
        answerB = "Hyäne",
        answerC = "Flossenbär (Pterois)",
        answerD = "Wüstenameise (Myrmecia)",
        correctAnswer = 1,
        explanation = "Die gefleckte Hyäne hat mit bis zu 11.000 Newton Beißkraft das stärkste Gebiss im Verhältnis zu ihrer Körpergröße unter den Säugetieren. Sie kann Knochen zermalmen, die für andere Raubtiere unzugänglich sind.",
        difficulty = 2,
        funFact = "Hyänen verdauen selbst Knochen vollständig – ihr Kot ist durch das Kalzium kreideweiss."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist für seinen außergewöhnlichen Schwanz bekannt, der bis zu 1 Meter lang sein kann?",
        answerA = "Paradiesvogel",
        answerB = "Pfau",
        answerC = "Fasane",
        answerD = "Quetzal",
        correctAnswer = 1,
        explanation = "Das Rad des Pfaus ist kein eigentlicher Schwanz, sondern verlängerte Oberschwanzdeckfedern. Diese können über 1,5 m lang werden und werden zur Balz aufgefächert.",
        difficulty = 2,
        funFact = "Nur männliche Pfauen haben das prächtige Rad. Das Weibchen (Pfauenhenne) ist unscheinbar braun gefärbt."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Besondere an den Augen der Tintenfische (Cephalopoden)?",
        answerA = "Sie haben keine Augen und orientieren sich durch Echolokation",
        answerB = "Ihre Augen haben keine blinde Stelle und entstand unabhängig vom Wirbeltierauge",
        answerC = "Sie können nur Schwarzweiß sehen",
        answerD = "Sie haben Facettenaugen wie Insekten",
        correctAnswer = 1,
        explanation = "Das Tintenfischauge ist ein Paradebeispiel für konvergente Evolution: Es ist dem Wirbelstierauge ähnlich aufgebaut, entstand aber unabhängig und hat im Gegensatz zu Wirbeltieraugen keinen blinden Fleck.",
        difficulty = 2,
        funFact = "Tintenfische sind farbenblind, nutzen aber die Polarisation des Lichts, um Farben indirekt wahrzunehmen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier benutzt Steine als Werkzeug, um Muscheln zu öffnen?",
        answerA = "Seeotter",
        answerB = "Pinguin",
        answerC = "Meeresschildkröte",
        answerD = "Robbe",
        correctAnswer = 0,
        explanation = "Seeotter legen beim Schwimmen auf dem Rücken einen flachen Stein auf ihren Bauch und schlagen Muscheln und Seeigel daran auf. Sie tragen bevorzugte Werkzeugsteine in Hautfalten mit sich.",
        difficulty = 2,
        funFact = "Seeotter sind eines der wenigen Tiere, die Werkzeuge verwenden. Sie halten sich auch beim Schlafen mit anderen Ottern an den Pfoten fest, um nicht davonzutreiben."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Katzenart kann als einzige große Katze brüllen UND schnurren?",
        answerA = "Leopard",
        answerB = "Keine – Katzen können entweder brüllen oder schnurren, nicht beides",
        answerC = "Schnee-Leopard",
        answerD = "Puma",
        correctAnswer = 2,
        explanation = "Der Schneeleopard kann als einzige der Großkatzen weder brüllen noch schnurren wie Hauskatzen – er macht ein eigentümliches Puffgeräusch. Alle anderen Großkatzen (Löwe, Tiger, Leopard, Jaguar) brüllen, können aber nicht schnurren.",
        difficulty = 2,
        funFact = "Kleine Katzen wie Puma und Gepard können schnurren (beim Ein- und Ausatmen), aber nicht brüllen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie orientieren sich Haie beim Jagen aus großer Entfernung?",
        answerA = "Durch ihr hervorragendes Sehvermögen",
        answerB = "Durch den Geruchssinn – sie riechen Blut auf Kilometerdistanz",
        answerC = "Durch das Seitenlinienorgan, das Druckwellen im Wasser wahrnimmt",
        answerD = "Durch elektrische Felder der Muskelaktivität von Beutetieren",
        correctAnswer = 1,
        explanation = "Aus großer Entfernung leitet primär der Geruchssinn Haie zur Beute – sie können geringste Blutspuren noch auf Hunderte Meter wahrnehmen. Auf kurze Distanz übernimmt das Ampullensystem elektrische Felder.",
        difficulty = 2,
        funFact = "Das Seitenlinienorgan des Hais registriert Druckwellen im Wasser und ermöglicht es ihm, Beute auch in totaler Dunkelheit zu orten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat die längste bekannte Wanderung aller Tiere?",
        answerA = "Grauer Wal",
        answerB = "Kaiserpinguin",
        answerC = "Küstenseeschwalbe (Sterna paradisaea)",
        answerD = "Monarchfalter",
        correctAnswer = 2,
        explanation = "Die Küstenseeschwalbe legt jährlich bis zu 90.000 km zurück – von der Arktis in die Antarktis und zurück. Das ist die längste bekannte Tierwanderung.",
        difficulty = 2,
        funFact = "Eine Küstenseeschwalbe erlebt in ihrem Leben bis zu 1,5 Millionen Kilometer Wanderung – das entspricht etwa 3 Reisen zum Mond und zurück."
    ),

    // ── ADDITIONAL EASY (difficulty = 1) ── 25 new questions ─────────────────

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist das größte Tier der Welt?",
        answerA = "Afrikanischer Elefant",
        answerB = "Buckelwal",
        answerC = "Blauwal",
        answerD = "Weißer Hai",
        correctAnswer = 2,
        explanation = "Der Blauwal ist das größte Tier, das je auf der Erde gelebt hat. Er kann über 30 Meter lang werden und bis zu 180 Tonnen wiegen.",
        difficulty = 1,
        funFact = "Das Herz eines Blauwals ist so groß wie ein kleines Auto."
    ),

    Question(
        categoryId = 9,
        questionText = "Was fressen Eulen hauptsächlich?",
        answerA = "Beeren und Früchte",
        answerB = "Fische und Algen",
        answerC = "Mäuse und andere kleine Tiere",
        answerD = "Blätter und Insektenlarven",
        correctAnswer = 2,
        explanation = "Eulen sind Raubtiere und ernähren sich hauptsächlich von Kleinsäugern wie Mäusen, aber auch von Vögeln, Fröschen und Insekten.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Jungtier eines Hundes?",
        answerA = "Kätzchen",
        answerB = "Welpe",
        answerC = "Ferkel",
        answerD = "Küken",
        correctAnswer = 1,
        explanation = "Das Jungtier eines Hundes heißt Welpe. Welpen werden blind und taub geboren und öffnen ihre Augen erst nach etwa zwei Wochen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier stellt den größten Anteil aller Tierarten auf der Erde?",
        answerA = "Fische",
        answerB = "Vögel",
        answerC = "Insekten",
        answerD = "Säugetiere",
        correctAnswer = 2,
        explanation = "Insekten sind mit über einer Million beschriebener Arten die artenreichste Tiergruppe der Erde – mehr als alle anderen Tiergruppen zusammen.",
        difficulty = 1,
        funFact = "Auf jeden Menschen kommen schätzungsweise 1,4 Milliarden Insekten."
    ),

    Question(
        categoryId = 9,
        questionText = "Wo leben Pinguine in der freien Natur?",
        answerA = "In der Arktis (Nordpol)",
        answerB = "In der Antarktis und auf der Südhalbkugel",
        answerC = "In Nordeuropa",
        answerD = "In Zentralafrika",
        correctAnswer = 1,
        explanation = "Alle Pinguinarten leben auf der Südhalbkugel – hauptsächlich in der Antarktis, aber auch in Südafrika, Südamerika, Neuseeland und auf den Galapagos-Inseln.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist ein reines Raubtier (kein Pflanzenfresser)?",
        answerA = "Kuh",
        answerB = "Pferd",
        answerC = "Wolf",
        answerD = "Schaf",
        correctAnswer = 2,
        explanation = "Der Wolf ist ein Fleischfresser und jagt Tiere wie Hirsche, Hasen und Mäuse. Kuh, Pferd und Schaf sind Pflanzenfresser.",
        difficulty = 1,
        funFact = "Wölfe können auf der Jagd bis zu 60 km/h erreichen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was kennzeichnet ein Reptil?",
        answerA = "Federn und Schnabel",
        answerB = "Schuppen und Kaltblütigkeit",
        answerC = "Kiemen und Wasserleben",
        answerD = "Fell und Warmblütigkeit",
        correctAnswer = 1,
        explanation = "Reptilien sind kaltblütige Tiere mit Schuppen oder Schilden. Dazu gehören Schlangen, Eidechsen, Schildkröten und Krokodile.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie heißt die Tiergruppe, zu der Frösche und Kröten gehören?",
        answerA = "Reptilien",
        answerB = "Insekten",
        answerC = "Amphibien",
        answerD = "Säugetiere",
        correctAnswer = 2,
        explanation = "Frösche und Kröten gehören zu den Amphibien (Lurchen). Sie leben als Larven im Wasser und als Erwachsene an Land.",
        difficulty = 1,
        funFact = "Frösche trinken kein Wasser mit dem Mund – sie nehmen Feuchtigkeit durch ihre Haut auf."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat einen Rüssel?",
        answerA = "Nashorn",
        answerB = "Elefant",
        answerC = "Nilpferd",
        answerD = "Giraffe",
        correctAnswer = 1,
        explanation = "Elefanten haben einen langen, muskulösen Rüssel als verlängerter Nasen-Lippen-Komplex. Sie nutzen ihn zum Greifen, Trinken und zur Kommunikation.",
        difficulty = 1,
        funFact = "Im Rüssel eines Elefanten befinden sich über 40.000 Muskeln – aber kein einziger Knochen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist eine Metamorphose?",
        answerA = "Ein Winterschlaf",
        answerB = "Eine grundlegende Körperveränderung während der Entwicklung",
        answerC = "Die Häutung bei Schlangen",
        answerD = "Ein Farbwechsel zur Tarnung",
        correctAnswer = 1,
        explanation = "Metamorphose ist die grundlegende Veränderung des Körperbaus während der Entwicklung – zum Beispiel von der Raupe zum Schmetterling.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier legt keine Eier?",
        answerA = "Krokodil",
        answerB = "Pinguin",
        answerC = "Delfin",
        answerD = "Ente",
        correctAnswer = 2,
        explanation = "Delfine sind Säugetiere und bringen lebende Junge zur Welt. Krokodile, Pinguine und Enten legen hingegen Eier.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was frisst eine Kuh hauptsächlich?",
        answerA = "Fische",
        answerB = "Insekten",
        answerC = "Gras und Heu",
        answerD = "Wurzeln und Pilze",
        correctAnswer = 2,
        explanation = "Kühe sind Pflanzenfresser und fressen hauptsächlich Gras und Heu. Ihr viergeteilter Magen ermöglicht eine effiziente Verdauung.",
        difficulty = 1,
        funFact = "Eine Kuh macht täglich bis zu 30.000 Kaubewegungen beim Wiederkäuen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist ein Zugvogel, der im Winter aus Deutschland wegfliegt?",
        answerA = "Spatz",
        answerB = "Schwalbe",
        answerC = "Taube",
        answerD = "Amsel",
        correctAnswer = 1,
        explanation = "Schwalben sind Zugvögel und fliegen im Herbst nach Afrika. Im Frühjahr kehren sie zur Brut zurück.",
        difficulty = 1,
        funFact = "Schwalben legen auf ihrem Zug bis zu 10.000 km zurück – hin und zurück fast 20.000 km pro Jahr."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man ein weibliches Pferd?",
        answerA = "Stute",
        answerB = "Henne",
        answerC = "Kuh",
        answerD = "Ziege",
        correctAnswer = 0,
        explanation = "Das weibliche Pferd heißt Stute. Den männlichen Hengst nennt man Hengst, einen kastrierten Hengst Wallach.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Tiergruppe hat Federn?",
        answerA = "Reptilien",
        answerB = "Amphibien",
        answerC = "Säugetiere",
        answerD = "Vögel",
        correctAnswer = 3,
        explanation = "Federn sind ein einzigartiges Merkmal der Vögel. Alle Vögel haben Federn – auch flugunfähige wie Pinguine und Strauße.",
        difficulty = 1,
        funFact = "Federn bestehen aus Keratin – demselben Protein wie menschliche Haare und Fingernägel."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier baut sein Netz aus Seide?",
        answerA = "Ameise",
        answerB = "Spinne",
        answerC = "Wespe",
        answerD = "Tausendfüßer",
        correctAnswer = 1,
        explanation = "Spinnen bauen Netze aus Seide, die sie aus Spinndrüsen am Hinterleib produzieren. Das Netz dient zum Fangen von Insekten.",
        difficulty = 1,
        funFact = "Spinnenseide ist pro Gewicht stärker als Stahl – Forscher untersuchen sie für medizinische und technische Anwendungen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Laut ist typisch für eine Katze?",
        answerA = "Bellen",
        answerB = "Grunzen",
        answerC = "Schnurren und Miauen",
        answerD = "Zwitschern",
        correctAnswer = 2,
        explanation = "Katzen kommunizieren durch Miauen, Schnurren, Fauchen und Zischen. Das Schnurren entsteht durch rhythmische Stimmbandvibrationen.",
        difficulty = 1,
        funFact = "Katzen miauen fast ausschließlich gegenüber Menschen – untereinander kommunizieren sie kaum durch Miauen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier gilt als Wappentier Deutschlands?",
        answerA = "Löwe",
        answerB = "Wolf",
        answerC = "Adler",
        answerD = "Bär",
        correctAnswer = 2,
        explanation = "Der Adler ist das Wappentier Deutschlands und steht im Bundeswappen als Symbol für Stärke, Mut und Freiheit.",
        difficulty = 1,
        funFact = "Der Weißkopfseeadler ist das Nationaltier der USA – Benjamin Franklin hätte lieber den Truthahn gewählt."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man ein Tier, das nur Pflanzen frisst?",
        answerA = "Karnivore",
        answerB = "Omnivore",
        answerC = "Herbivore",
        answerD = "Detritivore",
        correctAnswer = 2,
        explanation = "Herbivore sind Pflanzenfresser. Beispiele sind Kühe, Pferde, Schafe, Hasen und Elefanten.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist bekannt dafür, sich tot zu stellen?",
        answerA = "Fuchs",
        answerB = "Opossum",
        answerC = "Waschbär",
        answerD = "Stinktier",
        correctAnswer = 1,
        explanation = "Das Opossum ist berühmt dafür, sich bei Gefahr tot zu stellen. Es fällt in einen starren Zustand und kann Minuten bis Stunden reglos bleiben.",
        difficulty = 1,
        funFact = "Das Totstellen beim Opossum ist keine bewusste Entscheidung, sondern eine unwillkürliche Reaktion des Nervensystems."
    ),

    Question(
        categoryId = 9,
        questionText = "Zu welcher Tiergruppe gehört der Hai?",
        answerA = "Säugetiere",
        answerB = "Amphibien",
        answerC = "Fische",
        answerD = "Reptilien",
        correctAnswer = 2,
        explanation = "Haie sind Fische – genauer gesagt Knorpelfische (Chondrichthyes). Sie atmen mit Kiemen und sind kaltblütig.",
        difficulty = 1,
        funFact = "Haie haben ihr Skelett aus Knorpel, nicht aus Knochen – das macht sie leichter und wendiger."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Tierart lebt in einem Bienenstock?",
        answerA = "Wespen",
        answerB = "Hummeln",
        answerC = "Honigbienen",
        answerD = "Hornissen",
        correctAnswer = 2,
        explanation = "Honigbienen leben in Völkern im Bienenstock. Ein Volk kann bis zu 80.000 Individuen umfassen und besteht aus einer Königin, Arbeiterinnen und Drohnen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wo lebt ein Pinguin hauptsächlich – an Land oder im Wasser?",
        answerA = "Nur an Land",
        answerB = "Nur im Wasser",
        answerC = "Hauptsächlich im Wasser, Brut an Land",
        answerD = "In Bäumen nahe am Wasser",
        correctAnswer = 2,
        explanation = "Pinguine verbringen den Großteil ihres Lebens im Meer auf Nahrungssuche. An Land kommen sie nur zur Brut und Mauser.",
        difficulty = 1,
        funFact = "Pinguine können bis zu 27 km/h im Wasser schwimmen und bis zu 500 Meter tauchen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist für seinen Bau von Dämmen in Flüssen bekannt?",
        answerA = "Otter",
        answerB = "Biber",
        answerC = "Waschbär",
        answerD = "Murmeltier",
        correctAnswer = 1,
        explanation = "Biber bauen Dämme aus Ästen, Stämmen und Schlamm, um Teiche zu schaffen. Diese Teiche schützen ihren Eingang zur Biberburg vor Fressfeinden.",
        difficulty = 1,
        funFact = "Biberdämme können bis zu 100 Meter lang werden und ganze Landschaften umgestalten."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Besondere an Vögeln als Tiergruppe?",
        answerA = "Sie sind die einzigen Tiere, die Eier legen",
        answerB = "Sie haben Flügel und Federn",
        answerC = "Alle Vögel können fliegen",
        answerD = "Sie haben keine Zähne und keine Knochen",
        correctAnswer = 1,
        explanation = "Vögel sind die einzige Tiergruppe, die Federn besitzt. Nicht alle Vögel können fliegen (z.B. Pinguine, Strauße), aber alle haben Federn und Flügel.",
        difficulty = 1,
        funFact = null
    ),

    // ── ADDITIONAL HARD (difficulty = 3) ── 25 new questions ─────────────────

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter 'Trophobiose' in der Zoologie?",
        answerA = "Eine parasitische Räuber-Beute-Beziehung",
        answerB = "Eine Symbiose, bei der Ameisen Blattläuse für Honigtau bewirtschaften",
        answerC = "Verhaltenslernen durch Beobachtung anderer Tiere",
        answerD = "Territoriale Abgrenzung durch Nahrungsressourcen",
        correctAnswer = 1,
        explanation = "Trophobiose beschreibt eine Symbiose, bei der Ameisen Blattläuse vor Fressfeinden schützen und im Gegenzug zuckerhaltigen Honigtau erhalten.",
        difficulty = 3,
        funFact = "Manche Ameisenarten tragen Blattläuse in ihre Nester, wenn es kalt wird – sie halten sie wie Nutztiere."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der 'suprachiasmatische Nucleus' (SCN)?",
        answerA = "Das Gehirnareal für Geruchsverarbeitung bei Raubtieren",
        answerB = "Die biologische Innenuhr im Hypothalamus",
        answerC = "Ein Sinnesorgan zur Magnetfeldwahrnehmung",
        answerD = "Das Zentrum für Schmerzverarbeitung im Hirnstamm",
        correctAnswer = 1,
        explanation = "Der suprachiasmatische Nucleus (SCN) ist ein kleines Areal im Hypothalamus, das als zentrale biologische Uhr fungiert und den circadianen 24-Stunden-Rhythmus steuert.",
        difficulty = 3,
        funFact = "Der SCN enthält nur etwa 20.000 Neuronen, kontrolliert aber nahezu alle tagesrhythmischen Körperfunktionen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt Hamiltons Konzept der Verwandtenselektion (Kin Selection)?",
        answerA = "Bevorzugung der stärksten Nachkommen bei Ressourcenverteilung",
        answerB = "Altruismus gegenüber Verwandten zur Maximierung der inklusiven Fitness",
        answerC = "Aggressive Konkurrenz zwischen Geschwistern",
        answerD = "Territoriales Verhalten zum Schutz verwandter Gruppen",
        correctAnswer = 1,
        explanation = "Hamiltons Kin-Selection-Theorie erklärt Altruismus durch gemeinsame Gene: Ein Tier hilft Verwandten, wenn r·B > C (Verwandtschaftsgrad × Nutzen > Kosten für den Helfer).",
        difficulty = 3,
        funFact = "J.B.S. Haldane formulierte es so: 'Ich würde mein Leben für zwei Geschwister oder acht Cousins opfern.'"
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt das Konzept der ökologischen Nische nach Hutchinson?",
        answerA = "Den physischen Lebensraum einer Art",
        answerB = "Den n-dimensionalen Hyperraum aller Umweltbedingungen, unter denen eine Art existieren kann",
        answerC = "Die Nahrungsquelle einer bestimmten Tierart",
        answerD = "Den Aktionsradius eines Tieres",
        correctAnswer = 1,
        explanation = "G.E. Hutchinson definierte die ökologische Nische als n-dimensionalen Hyperraum aller abiotischen und biotischen Faktoren (Temperatur, Nahrung, Feinde), die das Überleben einer Art ermöglichen.",
        difficulty = 3,
        funFact = "Man unterscheidet fundamentale Nische (theoretisch möglich) und realisierte Nische (tatsächlich besetzt, eingeschränkt durch Konkurrenz)."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Organ nutzen Haie zur Wahrnehmung elektrischer Felder ihrer Beute?",
        answerA = "Seitenlinienorgan",
        answerB = "Lorenzini'sche Ampullen",
        answerC = "Jacobsonsche Organe",
        answerD = "Nares (Nasenöffnungen)",
        correctAnswer = 1,
        explanation = "Die Lorenzini'schen Ampullen sind Elektrorezeptoren in der Schnauze von Haien. Sie können elektrische Felder unter 5 Nanoampere wahrnehmen – erzeugt von Muskelkontraktionen der Beute.",
        difficulty = 3,
        funFact = "Haie spüren Beute, die komplett im Sand vergraben ist, allein durch ihr elektrisches Feld."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Haplodiplodie und bei welchen Tieren kommt sie vor?",
        answerA = "Fortpflanzung durch Zellteilung bei Einzellern",
        answerB = "System, bei dem Weibchen diploid und Männchen haploid sind; bei Hymenoptera (Bienen, Ameisen, Wespen)",
        answerC = "Doppelte Chromosomenzahl nach Befruchtung; bei Reptilien",
        answerD = "Geschlechtsbestimmung durch Temperatur; bei Krokodilen",
        correctAnswer = 1,
        explanation = "Bei haplodiploiden Insekten entstehen Weibchen aus befruchteten (diploiden) und Männchen aus unbefruchteten (haploiden) Eiern, was ihre Sozialstruktur grundlegend beeinflusst.",
        difficulty = 3,
        funFact = "Durch Haplodiplodie teilen Schwestern 75% ihrer Gene – mehr als Mutter und Tochter. Das erklärt extreme Kooperation in Bienenvölkern."
    ),

    Question(
        categoryId = 9,
        questionText = "Was erklärt die Navigation von Meeresschildkröten zu ihrem genauen Geburtsstand?",
        answerA = "Geruchsgedächtnis an den Heimatstrand",
        answerB = "Magnetisches Imprinting auf das lokale Erdmagnetfeld beim Schlüpfen",
        answerC = "Visuelle Wegmarkierungen im Gedächtnis",
        answerD = "Soziale Orientierung durch ältere Tiere",
        correctAnswer = 1,
        explanation = "Meeresschildkröten prägen sich beim Schlüpfen die magnetische Signatur ihres Geburtsstandes ein und kehren Jahrzehnte später zu exakt diesen magnetischen Koordinaten zurück.",
        difficulty = 3,
        funFact = "Manche Meeresschildkröten legen bis zu 12.000 km zurück, um an ihren Geburtsstand zu gelangen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Cryptobiose und welches Tier ist dafür bekannt?",
        answerA = "Tarnung durch Biolumineszenz; Tiefseefische",
        answerB = "Metabolischer Ruhezustand mit Überlebensfähigkeit bei extremen Bedingungen; Bärtierchen (Tardigrada)",
        answerC = "Einfrieren bei -20°C ohne Schäden; Polarfrösche",
        answerD = "Vollständige Trockenresistenz durch Sporulation; Krebstiere",
        correctAnswer = 1,
        explanation = "Bärtierchen (Tardigrada) treten in Cryptobiose ein und überleben so Vakuum, extremen Druck, Strahlung und Temperaturen von -272°C bis +150°C.",
        difficulty = 3,
        funFact = "Bärtierchen wurden dem offenen Weltraum ausgesetzt und überlebten – sie sind die widerstandsfähigsten bekannten Tiere."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist sympatrische Artbildung?",
        answerA = "Artbildung durch geografische Trennung",
        answerB = "Artbildung innerhalb desselben Verbreitungsgebiets ohne geografische Barriere",
        answerC = "Artbildung durch Klimaveränderungen",
        answerD = "Artbildung durch Hybridisierung zweier Elternarten",
        correctAnswer = 1,
        explanation = "Sympatrische Artbildung entsteht, wenn sich neue Arten im selben Lebensraum ohne geografische Trennung entwickeln – oft durch ökologische Spezialisierung.",
        difficulty = 3,
        funFact = "Die Buntbarsche (Cichliden) im Victoriasee sind ein Paradebeispiel: Über 500 Arten entstanden in wenigen Millionen Jahren im gleichen See."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches neurobiologische Prinzip ermöglicht Zugvögeln die Wahrnehmung des Erdmagnetfeldes?",
        answerA = "Magnetit-Kristalle im Schnabel reagieren mechanisch",
        answerB = "Kryptochrom-Proteine im Auge reagieren durch quantenmechanischen Radikalpaar-Mechanismus",
        answerC = "Elektrische Haarzellen im Innenohr detektieren Magnetfelder",
        answerD = "Magnetosomen aus Darmbakterien ermöglichen die Orientierung",
        correctAnswer = 1,
        explanation = "Kryptochrom-Proteine in den Augen von Zugvögeln ermöglichen durch den Radikalpaar-Mechanismus die Magnetfeldwahrnehmung – die Vögel könnten das Magnetfeld buchstäblich sehen.",
        difficulty = 3,
        funFact = "Es gibt Hinweise, dass Kryptochrome auch bei Säugetieren vorkommen könnten."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Viviparie im zoologischen Sinne?",
        answerA = "Lebendgeburt mit Nährstoffversorgung über eine Placenta oder ähnliche Struktur",
        answerB = "Eierlegung mit anschließender Bebrütung",
        answerC = "Brutpflege im Maul des Elternteils",
        answerD = "Entwicklung im Mutterleib ohne Placenta",
        correctAnswer = 0,
        explanation = "Viviparie bezeichnet echte Lebendgeburt, bei der der Embryo über eine Placenta oder ähnliche Struktur versorgt wird. Die meisten Säugetiere sind vivipar.",
        difficulty = 3,
        funFact = "Einige Hai-Arten sind ebenfalls vivipar – der Sandtigerhai zeigt sogar intrauterinen Kannibalismus."
    ),

    Question(
        categoryId = 9,
        questionText = "Was besagt das Gausesche Gesetz (Kompetitives Ausschlussprinzip)?",
        answerA = "Zwei Arten können koexistieren, wenn sie um dieselbe Ressource konkurrieren",
        answerB = "Zwei Arten, die dieselbe Nische vollständig besetzen, können nicht dauerhaft koexistieren",
        answerC = "Invasive Arten verdrängen stets einheimische Arten",
        answerD = "Räuber-Beute-Zyklen stabilisieren sich automatisch",
        correctAnswer = 1,
        explanation = "Das Gausesche Gesetz besagt, dass zwei Arten, die exakt dieselbe ökologische Nische besetzen, nicht dauerhaft koexistieren können – eine wird die andere verdrängen.",
        difficulty = 3,
        funFact = "In der Realität koexistieren ähnliche Arten durch subtile Nischendifferenzierung – das 'Planktonparadox' zeigt, wie dieses Gesetz in komplexen Systemen an Grenzen stößt."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Neotenie in der Zoologie?",
        answerA = "Das Auftreten von Alterungsmerkmalen in Jungtieren",
        answerB = "Das Beibehalten larvaler oder juveniler Merkmale im geschlechtsreifen Erwachsenen",
        answerC = "Beschleunigte Entwicklung durch Hormonstimulation",
        answerD = "Rückbildung von Organen nach der Metamorphose",
        correctAnswer = 1,
        explanation = "Neotenie bezeichnet das Beibehalten von Larvenmerkmalen im geschlechtsreifen Adultstadium. Der Axolotl ist das prominenteste Beispiel – er bleibt zeitlebens mit externen Kiemen.",
        difficulty = 3,
        funFact = "Auch Menschen zeigen im Vergleich zu anderen Primaten neotenische Merkmale: großer Schädel, flaches Gesicht und langes Jugendalter."
    ),

    Question(
        categoryId = 9,
        questionText = "Was kodiert der Schwänzeltanz der Honigbiene nach Karl von Frisch?",
        answerA = "Nur die Entfernung zur Nahrungsquelle",
        answerB = "Sowohl Richtung (relativ zur Sonne) als auch Entfernung zur Nahrungsquelle",
        answerC = "Warnhinweise auf Fressfeinde",
        answerD = "Nur die Qualität der Nahrungsquelle durch Tanzgeschwindigkeit",
        correctAnswer = 1,
        explanation = "Der Schwänzeltanz kodiert die Richtung zur Nahrungsquelle (Winkel zur Senkrechten = Winkel zur Sonne) und die Entfernung (Tanzdauer). Für diese Entdeckung erhielt Karl von Frisch 1973 den Nobelpreis.",
        difficulty = 3,
        funFact = "Bienen korrigieren ihren Schwänzeltanz für den Sonnenlauf über den Tag – sie kennen die Uhrzeit und rechnen die Sonnenposition voraus."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Aposematismus in der Zoologie?",
        answerA = "Lauteinsatz zur Reviermarkierung",
        answerB = "Auffällige Warntracht als Signal für Ungenießbarkeit oder Gefährlichkeit",
        answerC = "Das Totstellen bei Bedrohung",
        answerD = "Körperanpassung an die Umgebung (Tarnung)",
        correctAnswer = 1,
        explanation = "Aposematismus ist die Verwendung auffälliger Farben (gelb-schwarz, rot-schwarz) als Warnsignal für Fressfeinde. Giftige Tiere wie Pfeilgiftfrösche und Wespen nutzen ihn.",
        difficulty = 3,
        funFact = "Fressfeinde lernen Warnfarben sehr schnell zu meiden – ein einziges unangenehmes Erlebnis reicht oft für eine lebenslange Konditionierung."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie überlebt der Waldfrosch (Rana sylvatica) das Einfrieren ohne Zelltod?",
        answerA = "Antifrierproteine verhindern Eiskristallbildung",
        answerB = "Glucose als Kryoprotektivum verhindert intrazelluläre Eiskristalle",
        answerC = "Ausschütten von Frostschutzmitteln aus Hautdrüsen",
        answerD = "Vollständige Dehydratation aller Körperzellen",
        correctAnswer = 1,
        explanation = "Der Waldfrosch akkumuliert bei Kälte Glucose in seinen Zellen als Kryoprotektivum. Wasser gefriert im extrazellulären Raum, Glucose verhindert zerstörerische Kristalle in den Zellen.",
        difficulty = 3,
        funFact = "Bis zu 65% des Körperwassers des Waldfrosts können gefrieren. Sein Herz hört auf zu schlagen – im Frühjahr taut er auf und wird wieder aktiv."
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt reziproker Altruismus nach Robert Trivers?",
        answerA = "Hilfe gegenüber Verwandten aufgrund geteilter Gene",
        answerB = "Gegenseitige Hilfe zwischen nicht-verwandten Individuen auf Basis zukünftiger Gegenleistung",
        answerC = "Aggressives Schutzverhalten für die eigene Gruppe",
        answerD = "Kooperation nur bei sofortigem garantiertem Nutzen",
        correctAnswer = 1,
        explanation = "Reziproker Altruismus erklärt Hilfsverhalten zwischen Nicht-Verwandten: 'Ich helfe dir jetzt, du hilfst mir später.' Voraussetzung sind langfristiger Kontakt und die Fähigkeit, Betrüger zu erkennen.",
        difficulty = 3,
        funFact = "Vampirfledermäuse teilen Blut mit hungrigen Artgenossen – und merken sich genau, wer geteilt hat und wer nicht."
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt der Allee-Effekt in Tierpopulationen?",
        answerA = "Überlebensvorteil bei sehr hoher Populationsdichte",
        answerB = "Abnahme der individuellen Fitness bei sehr geringen Populationsdichten",
        answerC = "Schnelles Populationswachstum nach einem genetischen Engpass",
        answerD = "Bevorzugte Besiedelung von Randlebensräumen",
        correctAnswer = 1,
        explanation = "Der Allee-Effekt: Unterhalb einer kritischen Mindestpopulationsgröße sinkt die Fitness – durch erschwerte Partnerfindung, verminderte Kooperation und erhöhte Inzucht.",
        difficulty = 3,
        funFact = "Der Allee-Effekt ist ein Hauptgrund, warum kleine Restpopulationen gefährdeter Arten so schwer zu retten sind."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist ein Superorganismus in der Verhaltensbiologie?",
        answerA = "Ein besonders dominantes Leitindividuum in einer Herde",
        answerB = "Eine Kolonie eusocialer Insekten, die funktionell als eine Einheit agiert",
        answerC = "Ein Symbiont aus mehreren Tierarten",
        answerD = "Ein Tier mit besonders komplexem Nervensystem",
        correctAnswer = 1,
        explanation = "Ein Superorganismus ist eine Kolonie eusocialer Tiere (Bienen, Ameisen, Termiten), deren Individuen so spezialisiert kooperieren, dass die Kolonie wie ein einzelner Organismus funktioniert.",
        difficulty = 3,
        funFact = "Eine Blattschneiderameisen-Kolonie kann bis zu 8 Millionen Individuen umfassen und koordiniert sich ohne zentrale Steuerung."
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt Brutparasitismus beim Kuckuck aus evolutionärer Sicht?",
        answerA = "Fürsorge für fremde Jungtiere als soziales Lernverhalten",
        answerB = "Ein co-evolutionäres Wettrüsten zwischen parasitischer Art und Wirtsarten",
        answerC = "Aufnahme verwaister Jungtiere zum Vorteil der Gruppe",
        answerD = "Nahrungsdiebstahl zur Versorgung der eigenen Brut",
        correctAnswer = 1,
        explanation = "Brutparasitismus beim Kuckuck ist co-evolutionäres Wettrüsten: Wirtsvögel entwickeln bessere Eierkennung, Kuckucke bessere Ei-Mimikry – ein evolutionäres Katz-und-Maus-Spiel.",
        difficulty = 3,
        funFact = "Kuckucksweibchen sind auf bestimmte Wirtsarten spezialisiert und legen Eier, die perfekt zur Eifarbe dieser Wirtsart passen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Schutzprotein produzieren Bärtierchen (Tardigrada) gegen Strahlung?",
        answerA = "Chaperon-Proteine mit Doppelmembran-Struktur",
        answerB = "Damage suppressor Protein (Dsup), das DNA vor Strahlung abschirmt",
        answerC = "Melanin-Einlagerungen in allen Körperzellen",
        answerD = "Glycerol als Kryoprotektivum wie bei Insekten",
        correctAnswer = 1,
        explanation = "Tardigrada produzieren das Protein 'Damage suppressor' (Dsup), das sich um die DNA wickelt und ionisierende Strahlung sowie Radikale abschirmt.",
        difficulty = 3,
        funFact = "Forscher haben Dsup-Gene in menschliche Zellen eingebracht – diese zeigten daraufhin 40% weniger Strahlenschäden."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Epigenetik im tierbiologischen Kontext?",
        answerA = "Veränderungen der DNA-Sequenz durch Umwelteinflüsse",
        answerB = "Vererbbare Veränderungen der Genexpression ohne Änderung der DNA-Sequenz",
        answerC = "Horizontaler Gentransfer zwischen nicht verwandten Tierarten",
        answerD = "Mutationen durch kosmische Strahlung über Generationen",
        correctAnswer = 1,
        explanation = "Epigenetik bezeichnet vererbbare Änderungen der Genexpression durch DNA-Methylierung oder Histonmodifikationen, ohne die DNA-Sequenz selbst zu verändern.",
        difficulty = 3,
        funFact = "Hunger- und Trauma-Erfahrungen der Eltern können epigenetisch an Nachkommen weitergegeben werden."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie erzeugt der Pistolenkrebse (Alpheid-Garnelen) seinen tödlichen Schlag?",
        answerA = "Er schießt giftige Nadeln aus einer Scherenkammer",
        answerB = "Er erzeugt durch schnelles Scherenklappen eine Kavitationsblase mit extremer Temperatur",
        answerC = "Er produziert konzentrierte Säure in seiner Schere",
        answerD = "Er erzeugt elektrische Entladungen ähnlich dem Zitteraal",
        correctAnswer = 1,
        explanation = "Der Pistolenkrebs schließt seine Schere mit bis zu 100 km/h. Die kollabierte Kavitationsblase erzeugt einen 218-Dezibel-Knall und kurzzeitig Temperaturen von ~8.000 Kelvin.",
        difficulty = 3,
        funFact = "Pistolenkrebs-Kolonien erzeugten so viel Lärm, dass U-Boot-Kommandanten im Pazifik ihre Boote in Pistolenkrebsfeldern versteckten."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Funktion des Seitenlinienorgans bei Fischen?",
        answerA = "Elektrorezeption zur Beuteortung im Dunkeln",
        answerB = "Wahrnehmung von Druckveränderungen und Strömungen im Wasser",
        answerC = "Thermoregulation durch Wärmeaustausch",
        answerD = "Chemische Geruchswahrnehmung",
        correctAnswer = 1,
        explanation = "Das Seitenlinienorgan besteht aus Haarzellen entlang der Körperflanke, die Druckveränderungen, Strömungen und Vibrationen im Wasser registrieren.",
        difficulty = 3,
        funFact = "Das Seitenlinienorgan ist evolutionär verwandt mit dem menschlichen Innenohr – beide stammen vom gleichen embryonalen Gewebe ab."
    ),

    Question(
        categoryId = 9,
        questionText = "Was bezeichnet sexuelle Selektion nach Darwin?",
        answerA = "Natürliche Selektion, die auf Überlebensfähigkeit wirkt",
        answerB = "Selektion durch Partnerwahl oder Konkurrenz um Fortpflanzungspartner",
        answerC = "Genetische Drift in kleinen Populationen",
        answerD = "Selektion durch Parasiten und Krankheitserreger",
        correctAnswer = 1,
        explanation = "Sexuelle Selektion erklärt Merkmale wie Pfauenrad oder Hirschgeweih: Sie erschweren das Überleben, steigern aber den Fortpflanzungserfolg durch intrasexuelle Konkurrenz oder Partnerwahl.",
        difficulty = 3,
        funFact = "Darwin erkannte, dass viele auffällige Tiermerkmale nicht durch natürliche Selektion erklärbar sind – und entwickelte daher das Konzept der sexuellen Selektion."
    )
)