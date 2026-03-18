package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsMedium3(): List<Question> = listOf(

    // 50 medium bird questions (difficulty = 2, categoryId = 9)

    Question(
        categoryId = 9,
        questionText = "Welche Vogelart legt ihre Eier in die Nester anderer Voegel?",
        answerA = "Habicht",
        answerB = "Kuckuck",
        answerC = "Elster",
        answerD = "Kraehe",
        correctAnswer = 1,
        explanation = "Der Kuckuck ist ein Brutschmarotzer: Er legt seine Eier in fremde Nester und ueberlasst die Aufzucht den Wirtseltern.",
        difficulty = 2,
        funFact = "Das Kuckuckskueken wirft die anderen Eier aus dem Nest, sobald es geschluepft ist."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie weit kann ein Mauersegler pro Tag im Flug zuruecklegen?",
        answerA = "Bis zu 200 km",
        answerB = "Bis zu 500 km",
        answerC = "Bis zu 800 km",
        answerD = "Bis zu 1.000 km",
        correctAnswer = 3,
        explanation = "Mauersegler koennen taeglich bis zu 1.000 km zuruecklegen und verbringen den Grossteil ihres Lebens in der Luft.",
        difficulty = 2,
        funFact = "Mauersegler koennen sogar im Flug schlafen, indem sie abwechselnd eine Gehirnhaelfte ausruhen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel hat die laengste bekannte Zugstrecke aller Voegel?",
        answerA = "Weissschnabel-Eisvogel",
        answerB = "Kuestenseeschwalbe",
        answerC = "Wanderfalke",
        answerD = "Kranichs",
        correctAnswer = 1,
        explanation = "Die Kuestenseeschwalbe legt jaehrlich bis zu 70.000 km zurueck und zieht von der Arktis in die Antarktis und zurueck.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Vogelart bruetest in Erdbauhoehlen statt in Baumhoehlen oder offenen Nestern?",
        answerA = "Eisvogel",
        answerB = "Mehlschwalbe",
        answerC = "Star",
        answerD = "Bachstelze",
        correctAnswer = 0,
        explanation = "Der Eisvogel grabt seine Brutroehren selbst in Steilwaende an Ufern, oft 50 bis 90 cm tief.",
        difficulty = 2,
        funFact = "Der Eisvogel taucht mit bis zu 40 km/h ins Wasser, um Fische zu fangen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was bezeichnet man als 'Balz' bei Voegeln?",
        answerA = "Das Futtersuchen im Winter",
        answerB = "Das Paarungsverhalten zur Fortpflanzung",
        answerC = "Den Vogelzug im Herbst",
        answerD = "Das Mausern des Gefieders",
        correctAnswer = 1,
        explanation = "Als Balz bezeichnet man das Werbeverhalten von Voegeln, das Gesang, Imponiergebaerden und Tanzrituale einschliesst.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Greifvogel hat die groesste Flugelspannweite in Europa?",
        answerA = "Steinadler",
        answerB = "Seeadler",
        answerC = "Bartgeier",
        answerD = "Schmutzgeier",
        correctAnswer = 1,
        explanation = "Der Seeadler hat eine Flugelspannweite von bis zu 2,45 m und ist damit Europas groesster Greifvogel.",
        difficulty = 2,
        funFact = "Der Seeadler wurde in Deutschland in den 1980er Jahren fast ausgerottet und ist heute dank Schutzprogrammen wieder haeufiger."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das erste Federkleid eines juengen Vogels nach dem Dunenstadium?",
        answerA = "Prachtgefieder",
        answerB = "Jugendkleid",
        answerC = "Schlichtkleid",
        answerD = "Brutkleid",
        correctAnswer = 1,
        explanation = "Nach dem Daunenstadium entwickeln Jungvoegel das sogenannte Jugendkleid, das sich oft deutlich vom Adulten unterscheidet.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel kann rueckwaerts fliegen?",
        answerA = "Mauersegler",
        answerB = "Kolibri",
        answerC = "Schwalbe",
        answerD = "Turmfalke",
        correctAnswer = 1,
        explanation = "Kolibris sind die einzigen Voegel, die dauerhaft rueckwaerts fliegen koennen, da sie ihre Fluegel in einer einzigartigen Achterbewegung schlagen.",
        difficulty = 2,
        funFact = "Kolibris schlagen ihre Fluegel bis zu 80 Mal pro Sekunde."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Laufvogel ist das schwerste flugunfaehige Tier der Welt?",
        answerA = "Emu",
        answerB = "Nandu",
        answerC = "Straussenvogel",
        answerD = "Kasuar",
        correctAnswer = 2,
        explanation = "Der Afrikanische Strauß ist mit bis zu 156 kg das schwerste lebende Vogel und das schwerste flugunfaehige Tier.",
        difficulty = 2,
        funFact = "Das Straußenei ist das groesste Ei aller lebenden Voegel — ein Ei wiegt etwa 1,5 kg."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Besondere am Nest des Webervogels?",
        answerA = "Es wird aus Schlamm gebaut",
        answerB = "Es wird aus Spinnweben geformt",
        answerC = "Es wird kunstvoll aus Grashalmen geflochten",
        answerD = "Es wird in Felsen gehauen",
        correctAnswer = 2,
        explanation = "Webervogel-Maennchen flechten kunstvolle Kugelnester aus Grashalmen, um Weibchen anzulocken.",
        difficulty = 2,
        funFact = "Webervoegelkolonien koennen aus Hunderten einzelner Nester bestehen, die wie Fruechte an einem Baum haengen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Sinn ist bei den meisten Voegeln am schwaechsten ausgepraegt?",
        answerA = "Sehsinn",
        answerB = "Gleichgewichtssinn",
        answerC = "Gehoer",
        answerD = "Geruchssinn",
        correctAnswer = 3,
        explanation = "Die meisten Voegel haben einen sehr schwachen Geruchssinn. Ausnahmen sind Geier und Kiwis, die gut riechen koennen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie bezeichnet man Voegel, die ihre Kueken sofort nach dem Schluepfen selbst fuehren und fuettern koennen?",
        answerA = "Nesthocker",
        answerB = "Nestfluechter",
        answerC = "Brutfluechter",
        answerD = "Laufvoegel",
        correctAnswer = 1,
        explanation = "Nestfluechter wie Enten oder Huehnern sind von Geburt an beweglich und koennen sofort laufen oder schwimmen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel singt nachts und ist bekannt fuer seine melodiereiche Stimme?",
        answerA = "Buchfink",
        answerB = "Rotkehlchen",
        answerC = "Nachtigall",
        answerD = "Amsel",
        correctAnswer = 2,
        explanation = "Die Nachtigall gilt als einer der melodischsten Saenger Europas und singt auch nachts, besonders intensiv zur Balzzeit.",
        difficulty = 2,
        funFact = "Eine Nachtigall beherrscht bis zu 260 verschiedene Strophen und Tonfolgen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was bedeutet 'Zugvogel'?",
        answerA = "Ein Vogel der besonders schnell fliegt",
        answerB = "Ein Vogel der saisonal zwischen Brutgebiet und Ueberwinterungsgebiet wandert",
        answerC = "Ein Vogel der bei starkem Wind fliegt",
        answerD = "Ein Vogel der andere Voegel zieht und fuehrt",
        correctAnswer = 1,
        explanation = "Zugvoegel wandern regelmaessig zwischen saisonalen Lebensraeumen, um guenstige Brutbedingungen und Nahrungsangebote zu nutzen.",
        difficulty = 2,
        funFact = "Zugvoegel orientieren sich am Erdmagnetfeld, an Sternen und an Landmarken gleichzeitig."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel baut sein Nest aus Speichel an Felsenwaenden?",
        answerA = "Mauersegler",
        answerB = "Salangane",
        answerC = "Schwalbe",
        answerD = "Strandlaeufer",
        correctAnswer = 1,
        explanation = "Salanganen (Schwalbenverwandte) bauen ihre Nester fast ausschliesslich aus gehaertetem Speichel. Daraus wird das asiatische Gericht Vogelnestsuppe hergestellt.",
        difficulty = 2,
        funFact = "Ein Salanganen-Nest kann bis zu 35 Tage Bauzeit erfordern und kostet als Delikatesse mehrere hundert Euro."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Vogelart bruetet als einzige regelmaessig unter dem Gefrierpunkt in der Antarktis?",
        answerA = "Albatros",
        answerB = "Kaiserpinguin",
        answerC = "Eselspinguin",
        answerD = "Eissturmvogel",
        correctAnswer = 1,
        explanation = "Der Kaiserpinguin bruetet im antarktischen Winter bei bis zu -60 Grad Celsius. Das Maennchen haelt das Ei waermend auf den Fuessen.",
        difficulty = 2,
        funFact = "Maennliche Kaiserpinguine fasten waehrend der gesamten Brutzeit von etwa 65 Tagen und verlieren dabei bis zu 40 % ihres Koerpergewichts."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man den periodischen Federwechsel bei Voegeln?",
        answerA = "Metamorphose",
        answerB = "Mauser",
        answerC = "Haeutung",
        answerD = "Gefiederregeneration",
        correctAnswer = 1,
        explanation = "Die Mauser ist der regelmaessige Wechsel des Gefieders bei Voegeln, der meist nach der Brutzeit stattfindet.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Meeresvogel hat die groesste Flugelspannweite aller lebenden Voegel?",
        answerA = "Grosskampflaeufer",
        answerB = "Krauskopfpelikan",
        answerC = "Wanderalbatros",
        answerD = "Riesensturmvogel",
        correctAnswer = 2,
        explanation = "Der Wanderalbatros erreicht eine Flugelspannweite von bis zu 3,5 m und ist damit der Vogel mit der groessten Spannweite weltweit.",
        difficulty = 2,
        funFact = "Albatrosse koennen jahrelang ohne Landung fliegen und nutzen Windstroemungen uebers Meer, um sich kaum anzustrengen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist das Wappentier Deutschlands?",
        answerA = "Seeadler",
        answerB = "Wanderfalke",
        answerC = "Schwarzstorch",
        answerD = "Steinadler",
        correctAnswer = 0,
        explanation = "Der Bundesadler im deutschen Wappen ist ein stilisierter Seeadler und Symbol seit dem Mittelalter.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist eine 'Imponierpose' bei Voegeln?",
        answerA = "Eine Schlafstellung",
        answerB = "Eine Drohgeberde gegenueber Fressfeinden",
        answerC = "Ein Verhalten zur Partnergewinnung oder Territorialverteidigung",
        answerD = "Eine Koerperhaltung beim Baden",
        correctAnswer = 2,
        explanation = "Imposierposen sind visuelle Signale, mit denen Voegel potenzielle Partner beeindrucken oder Rivalen einschuechtern.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist bekannt dafuer, Werkzeuge zu benutzen?",
        answerA = "Kanadagans",
        answerB = "Neukaledonienkraehe",
        answerC = "Schwarzspecht",
        answerD = "Trottellumme",
        correctAnswer = 1,
        explanation = "Die Neukaledonienkraehe fertigt und benutzt Werkzeuge aus Aesten und Blaettern, um Insekten aus Rindenspalten zu holen.",
        difficulty = 2,
        funFact = "Neukaledonienkraehenindividuen koennen Werkzeugtechniken voneinander erlernen und weitergeben — eine Form von Vogelkultur."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Vogelgruppe legt die wenigsten Eier pro Gelege?",
        answerA = "Huehner",
        answerB = "Greifvoegel",
        answerC = "Sturmvoegel und Albatrosse",
        answerD = "Enten",
        correctAnswer = 2,
        explanation = "Albatrosse und viele Sturmvoegel legen nur ein einziges Ei pro Saison. Sie investieren dafuer viel Zeit in die Aufzucht des Kuekes.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Womit navigieren Zugvoegel in mondlosen Naechten?",
        answerA = "Mit dem Mondlicht",
        answerB = "Mit dem Sternenhimmel",
        answerC = "Mit dem Geruch der Heimat",
        answerD = "Mit Infraschall",
        correctAnswer = 1,
        explanation = "Viele nachtziehende Voegel nutzen den Sternenhimmel als Kompass, besonders die Lage des Polarsterns.",
        difficulty = 2,
        funFact = "Voegel lernen die Sternenkarte in ihrer Jugend und koennen sie ein Leben lang anwenden."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen Nest- und Hoehlenbrueter?",
        answerA = "Hoehlenbrueter bauen immer groessere Nester",
        answerB = "Nestbrueter legen mehr Eier",
        answerC = "Hoehlenbrueter nutzen Baumhoehlen oder Felsnischen, Nestbrueter bauen freihaengende Nester",
        answerD = "Es gibt keinen Unterschied, nur die Bezeichnung variiert",
        correctAnswer = 2,
        explanation = "Hoehlenbrueter wie Meisen oder Spechte nutzen Hohlraeume als Nistplatz, waehrend Nestbrueter offene Strukturen errichten.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Wasservogel kann senkrecht aus dem Wasser starten?",
        answerA = "Hoeckerschwan",
        answerB = "Haubentaucher",
        answerC = "Stockente",
        answerD = "Blasshuhn",
        correctAnswer = 2,
        explanation = "Stockenten koennen fast senkrecht aus dem Wasser aufsteigen, waehrend schwerere Wasservoegel eine lange Anlaufstrecke benoetigen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist der schnellste im Sturzflug?",
        answerA = "Steinadler",
        answerB = "Wanderfalke",
        answerC = "Habicht",
        answerD = "Roter Milan",
        correctAnswer = 1,
        explanation = "Der Wanderfalke erreicht im Sturzflug Geschwindigkeiten von ueber 320 km/h und ist damit das schnellste Tier der Welt.",
        difficulty = 2,
        funFact = "Der Wanderfalke hat besondere Knochenstruktur in den Nasenloechern, die den Luftstrom beim Sturzflug reguliert."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Phaenomen, wenn Voegel in grossen Schwaermen synchron fliegen?",
        answerA = "Zugformation",
        answerB = "Schwarmintelligenz",
        answerC = "Staren-Murmuration",
        answerD = "Schwarmflug",
        correctAnswer = 2,
        explanation = "Die Murmuration ist das spektakulaere Schwarmfliegen von Staren, bei dem Tausende Voegel synchron komplexe Muster in den Himmel zeichnen.",
        difficulty = 2,
        funFact = "Jeder Stare im Schwarm reagiert auf seine sieben naechsten Nachbarn und schafft so ohne zentrale Steuerung fliessende Muster."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist in Deutschland fuer das Aushoehlen von Baumstaemmen bekannt und schafft damit Lebensraum fuer andere Arten?",
        answerA = "Buntspecht",
        answerB = "Star",
        answerC = "Kleiber",
        answerD = "Schwarzspecht",
        correctAnswer = 3,
        explanation = "Der Schwarzspecht hackt grosse Bruthohlen in Baeume, die spaeter von anderen Tieren wie Dohlen, Mardern und Fledermaeuse genutzt werden.",
        difficulty = 2,
        funFact = "Der Schwarzspecht kann mit seinem Schnabel bis zu 20 Schlage pro Sekunde gegen Holz fuehren."
    ),

    Question(
        categoryId = 9,
        questionText = "Was frisst der Bartgeier hauptsaechlich?",
        answerA = "Lebende Fische",
        answerB = "Insekten und Larven",
        answerC = "Knochen von Aas",
        answerD = "Fruechte und Beeren",
        correctAnswer = 2,
        explanation = "Der Bartgeier ist der einzige Vogel, der sich fast ausschliesslich von Knochen ernaehrt. Er laesst grosse Knochen auf Felsen fallen, um sie zu zerbrechen.",
        difficulty = 2,
        funFact = "Der Bartgeier faerbt sein Gefieder durch Baeden in eisen- und mineralhaltigem Wasser rostrot ein."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher europaeische Vogel gilt als 'Wetterprophet' und zeigt durch sein Verhalten kommendes Schlechtwetter an?",
        answerA = "Schwalbe",
        answerB = "Spatz",
        answerC = "Turteltaube",
        answerD = "Wiedehopf",
        correctAnswer = 0,
        explanation = "Schwalben fliegen bei fallendem Luftdruck tiefer, da ihre Insektenbeute dann ebenfalls niedriger fliegt. Dies gilt seit alters als Schlechtwetteranzeichen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel benutzt seinen Schnabel als Ambosspunkt, um harte Schneckenschalen aufzuschlagen?",
        answerA = "Turmfalke",
        answerB = "Singdrossel",
        answerC = "Wacholderdrossel",
        answerD = "Amsel",
        correctAnswer = 1,
        explanation = "Die Singdrossel schlaegt Schnecken an bestimmten Steinen auf, die als 'Ambosse' bekannt sind. Diese genutzten Steine sind oft mit Schalenresten bedeckt.",
        difficulty = 2,
        funFact = "Singdrosseln merken sich die Positionen ihrer bevorzugten Ambossteine und kehren regelmaessig zu ihnen zurueck."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie orientieren sich Zugvoegel bei bewolktem Himmel tagsuebers?",
        answerA = "Sie warten auf Sonnenaufgang",
        answerB = "Am Erdmagnetfeld",
        answerC = "An Geraeuschkulissen",
        answerD = "Am Windmuster",
        correctAnswer = 1,
        explanation = "Voegel besitzen magnetische Rezeptoren, vermutlich im Schnabel oder im Auge, mit denen sie das Erdmagnetfeld als Kompass nutzen koennen.",
        difficulty = 2,
        funFact = "Forscher haben herausgefunden, dass Zugvoegel das Magnetfeld moeglicherweise sogar 'sehen' koennen, als Ueberlagerung ueber ihr normales Sichtfeld."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Besondere am Brutverhalten des Kiwis?",
        answerA = "Beide Eltern wechseln sich beim Brueten ab",
        answerB = "Das Maennchen allein bruetet das riesige Ei aus",
        answerC = "Die Weibchen brueten in Gruppen",
        answerD = "Die Eier werden im Sand vergraben",
        correctAnswer = 1,
        explanation = "Beim Kiwi bruetet ausschliesslich das Maennchen. Das Ei ist im Verhaeltnis zur Koerpergroesse das groesste aller Voegel.",
        difficulty = 2,
        funFact = "Ein Kiwi-Ei macht 15-20 % des Koerpergewichts des Weibchens aus — beim Menschen waere das ein Neugeborenes von 10-12 kg."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Allopreening' bei Voegeln?",
        answerA = "Gemeinschaftliches Bauern eines Nests",
        answerB = "Gegenseitiges Putzen des Gefieders beim Partner",
        answerC = "Synchrones Tauchen bei Wasservoegeln",
        answerD = "Nachahmung anderer Vogelrufe",
        correctAnswer = 1,
        explanation = "Allopreening (gegenseitiges Gefiederputzen) staerkt die Paarbindung und hilft, schwer erreichbare Stellen am Kopf zu pflegen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel kann seinen Kopf um fast 270 Grad drehen?",
        answerA = "Habicht",
        answerB = "Turmfalke",
        answerC = "Eule",
        answerD = "Rabe",
        correctAnswer = 2,
        explanation = "Eulen koennen den Kopf bis zu 270 Grad drehen, da ihre Augen unbeweglich im Schaedel sitzen und sie so trotzdem ein breites Sichtfeld haben.",
        difficulty = 2,
        funFact = "Eulen haben speziell angepasste Blutgefaesse im Hals, die bei extremen Kopfdrehungen den Blutfluss aufrechterhalten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Vogelart ist bekannt dafuer, Glanzstuecke in ihrem Nest zu sammeln?",
        answerA = "Dohle",
        answerB = "Elster",
        answerC = "Kolkrabe",
        answerD = "Eichelhaehr",
        correctAnswer = 1,
        explanation = "Elstern haben den Ruf, glitzernde Objekte zu sammeln, obwohl Studien zeigen, dass sie glaenzendes Neues eher meiden als sammeln.",
        difficulty = 2,
        funFact = "Untersuchungen zeigen, dass Elstern neue glaenzende Gegenstaende tatsaechlich aus Vorsicht meiden — der 'diebische Elster'-Mythos ist weitgehend Legende."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Wasservogel kann unter Wasser 'fliegen'?",
        answerA = "Stockente",
        answerB = "Haubentaucher",
        answerC = "Pinguin",
        answerD = "Blasshuhn",
        correctAnswer = 2,
        explanation = "Pinguine benutzen ihre Fluegel als Flossen und bewegen sich im Wasser mit aehnlichen Bewegungen wie fliegende Voegel in der Luft.",
        difficulty = 2,
        funFact = "Kaiserpinguine koennen bis zu 564 m tief tauchen und dabei ueber 20 Minuten die Luft anhalten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher europaeische Singvogel ahmt die Rufe anderer Vogelarten nach?",
        answerA = "Zaunkoenig",
        answerB = "Sprosser",
        answerC = "Sumpfrohrsaenger",
        answerD = "Zilpzalp",
        correctAnswer = 2,
        explanation = "Der Sumpfrohrsaenger ist ein hervorragender Imitator und ahmt in seinem Gesang Dutzende afrikanische und europaeische Vogelarten nach.",
        difficulty = 2,
        funFact = "Ein einziger Sumpfrohrsaenger kann die Gesaenge von bis zu 70 verschiedenen Vogelarten imitieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist ein 'Kolonie-Brueter'?",
        answerA = "Ein Vogel der nur einmal im Leben bruetet",
        answerB = "Eine Vogelart die in dichten Gruppen gemeinsam bruetet",
        answerC = "Ein Vogel der in tropischen Kolonien lebt",
        answerD = "Eine Vogelart deren Kueken gemeinsam aufgezogen werden",
        correctAnswer = 1,
        explanation = "Koloniebrueter wie Moewen, Kormorane und Flamingos brueten in grossen Gruppen, was Schutz vor Fressfeinden bietet.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Strandvogel taeucht seinen Schnabel beim Laufen rhythmisch in den nassen Sand?",
        answerA = "Austernfischer",
        answerB = "Saebel-schnaebler",
        answerC = "Sanderling",
        answerD = "Kiebitz",
        correctAnswer = 2,
        explanation = "Der Sanderling laueft den Wellen nach und pickt hektisch Kleintiere aus dem nassen Sand, die durch die Wellen freigelegt werden.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Warum haben Flamingos ihre charakteristische rosa Faerbung?",
        answerA = "Genetische Pigmentierung",
        answerB = "Karotinoids in ihrer Nahrung",
        answerC = "UV-Strahlung der Sonne",
        answerD = "Algenwachstum auf dem Gefieder",
        correctAnswer = 1,
        explanation = "Flamingos erhalten ihre rosa-rote Farbe durch Carotinoide in ihrer Nahrung, hauptsaechlich aus Algen und Krebstieren.",
        difficulty = 2,
        funFact = "Junge Flamingos sind weiss und faerben sich erst durch ihre Nahrung pink. Zoo-Flamingos muessen extra mit Carotinoiden gefuettert werden."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Eigenschaft macht den Schnabel des Tukans besonders?",
        answerA = "Er ist extrem hart und dient zum Holzspalten",
        answerB = "Er ist unverhaeteltnismaessig gross, aber sehr leicht durch seine Hohlraumstruktur",
        answerC = "Er kann wie ein Saugrohr Nektar aufnehmen",
        answerD = "Er veraendert die Farbe je nach Gemuetszustand",
        correctAnswer = 1,
        explanation = "Der Tukan-Schnabel ist riesig, aber leicht, da er aus einer wabenfoermigen Knochenstruktur besteht, die ihn stabil und trotzdem leicht macht.",
        difficulty = 2,
        funFact = "Der grosse Schnabel des Tukans dient als Kuehlkoerper: Blut wird durch den Schnabel geleitet und gibt Waerme ab."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Nahrungsbalz' bei Voegeln?",
        answerA = "Das Suchen nach Nahrung in der Balzzeit",
        answerB = "Das Ueberreichen von Futter als Bestandteil der Partnerschaftswerbung",
        answerC = "Das gemeinschaftliche Fressen in Paaren",
        answerD = "Der Kampf um Futterquellen",
        correctAnswer = 1,
        explanation = "Bei vielen Vogelarten uebergibt das Maennchen der Weibchen Futter als Balzritual. Dies zeigt seine Faehigkeit, fuer Nachwuchs zu sorgen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel legt seine Eier nicht in ein eigenes Nest, sondern direkt auf felsigen Boden ohne jedes Nistmaterial?",
        answerA = "Trottellumme",
        answerB = "Sterntaucher",
        answerC = "Textor-Weber",
        answerD = "Sturmmoewe",
        correctAnswer = 0,
        explanation = "Die Trottellumme legt ihr einziges Ei direkt auf nackte Felsvorspruenge ohne Nest. Die birnenfoermige Form des Eis verhindert, dass es wegrollt.",
        difficulty = 2,
        funFact = "Das birnformige Ei der Trottellumme dreht sich bei Erschuetterung im Kreis statt in eine Richtung zu rollen — ein einzigartiger Evolutionsmechanismus."
    ),

    Question(
        categoryId = 9,
        questionText = "Was bedeutet 'monogam' im Zusammenhang mit Voegeln?",
        answerA = "Ein Vogel der sich nur von einer Nahrungsquelle ernaehrt",
        answerB = "Ein Vogel der ein Leben lang denselben Partner behaelt",
        answerC = "Ein Vogel der alleine lebt und nicht bruetet",
        answerD = "Ein Vogel der nur einmal Nachwuchs bekommt",
        correctAnswer = 1,
        explanation = "Monogame Voegel bilden langjaehrige oder lebenslange Paarbindungen. Beispiele sind Storche, Schwane und viele Albatrosse.",
        difficulty = 2,
        funFact = "Selbst als 'monogam' geltende Vogelarten haben laut genetischen Studien haeufig 'aussereheliche' Kueken — etwa 11 % aller Nestlinge."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Tropenvogel hat ein aussergewoehnlich langes Schwanzgefieder, das laenger als sein Koerper sein kann?",
        answerA = "Paradiesvogel",
        answerB = "Pfauenfasan",
        answerC = "Federschwanzkolibri",
        answerD = "Langschwanzsittich",
        correctAnswer = 0,
        explanation = "Maennliche Paradiesvoegel haben oft extrem langes, farbenpraechtiges Schwanzgefieder, das zur Balz eingesetzt wird.",
        difficulty = 2,
        funFact = "Es gibt ueber 40 Paradiesvogelarten in Neuguinea, viele mit einzigartigen Federformationen die Evolution durch sexuelle Selektion veranschaulichen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Vogelart ist in Deutschland seit Jahrzehnten vom Aussterben bedroht und Gegenstand intensiver Schutzprogramme?",
        answerA = "Stadtmoewe",
        answerB = "Grosse Trappe",
        answerC = "Stockente",
        answerD = "Blaumeise",
        correctAnswer = 1,
        explanation = "Die Grosse Trappe ist einer der schwersten fluegelfaehigen Voegel der Welt und in Deutschland stark gefaehrdet. Es gibt nur noch wenige Hundert Tiere.",
        difficulty = 2,
        funFact = "Maennliche Grosse Trappen wiegen bis zu 18 kg und fuhren waehrend der Balz spektakulaere Rad-Vorfuhrungen durch, bei denen sie ihr Gefieder wenden."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Aufgabe des Federkleids bei Wasservoegeln?",
        answerA = "Nur zur Tarnung",
        answerB = "Thermische Isolierung und Wasserabweisung durch Fettdruesen",
        answerC = "Erhoehte Schwimmgeschwindigkeit durch Stromlinienform",
        answerD = "Schutz vor Sonnenstrahlen",
        correctAnswer = 1,
        explanation = "Wasservoegel pflegen ihr Gefieder intensiv mit Oel aus der Buerzeldruse, das Wasser abweist und gleichzeitig isoliert.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was bezeichnet man als 'Brutfleck' bei Voegeln?",
        answerA = "Eine Farbmarkierung auf dem Gefieder bruetender Voegel",
        answerB = "Eine gefiederlosen, gut durchbluteten Hautstelle am Bauch zur Eierwaermung",
        answerC = "Den Standort des Nests im Territorium",
        answerD = "Die Verfaerbung des Schnabels waehrend der Brutzeit",
        correctAnswer = 1,
        explanation = "Der Brutfleck ist eine gefiederlose und stark durchblutete Bauchstelle, die direkten Hautkontakt zum Ei ermoeglicht und es auf Koerpertemperatur haelt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Bei welcher Vogelart singt ausschliesslich das Maennchen?",
        answerA = "Buchfink",
        answerB = "Rotkehlchen",
        answerC = "Star",
        answerD = "Dompfaff (Gimpel)",
        correctAnswer = 0,
        explanation = "Beim Buchfinken singt nur das Maennchen, um Territorium und Weibchen zu gewinnen. Das Weibchen ist stumm.",
        difficulty = 2,
        funFact = "Buchfinken-Maennchen lernen ihren Gesang in der Jugend von benachbarten Maennchen — regionale Dialekte entstehen so ueber Generationen."
    ),

)
