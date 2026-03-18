package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsEasy2(): List<Question> = listOf(

    // 50 easy mammal questions (difficulty = 1, categoryId = 9)

    Question(
        categoryId = 9,
        questionText = "Wie viele Beine hat ein Hund?",
        answerA = "Zwei",
        answerB = "Sechs",
        answerC = "Vier",
        answerD = "Acht",
        correctAnswer = 2,
        explanation = "Hunde sind Vierbeiner. Alle Hunderassen haben vier Beine.",
        difficulty = 1,
        funFact = "Hunde sind seit etwa 15.000 Jahren die engsten tierischen Begleiter des Menschen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist bekannt als \"Koenig der Tiere\"?",
        answerA = "Tiger",
        answerB = "Loewe",
        answerC = "Elefant",
        answerD = "Gorilla",
        correctAnswer = 1,
        explanation = "Der Loewe wird traditionell als Koenig der Tiere bezeichnet, weil er stolz, kraeftig und furchtlos wirkt.",
        difficulty = 1,
        funFact = "Nur maennliche Loewen haben eine Maehne. Diese waechst mit dem Alter und wird dunkler."
    ),

    Question(
        categoryId = 9,
        questionText = "Was fressen Kaninchen hauptsaechlich?",
        answerA = "Fleisch",
        answerB = "Fische",
        answerC = "Insekten",
        answerD = "Gras und Heu",
        correctAnswer = 3,
        explanation = "Kaninchen sind Pflanzenfresser und ernaehren sich hauptsaechlich von Gras, Heu und frischem Gemuese.",
        difficulty = 1,
        funFact = "Kaninchen muessen ihren Darm zweimal verdauen und fressen bestimmte Kotpellets erneut auf."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Saeugetier kann fliegen?",
        answerA = "Fliegendes Eichhoernchen",
        answerB = "Fledermaus",
        answerC = "Fliegender Fuchs",
        answerD = "Flugbeutler",
        correctAnswer = 1,
        explanation = "Die Fledermaus ist das einzige Saeugetier, das aktiv fliegen kann. Andere Arten gleiten nur.",
        difficulty = 1,
        funFact = "Fledermaeuse orientieren sich mit Ultraschall, einer Methode namens Echolot."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Farbe hat ein Eisbear?",
        answerA = "Weiss",
        answerB = "Gelb",
        answerC = "Hellgrau",
        answerD = "Cremefarben",
        correctAnswer = 0,
        explanation = "Das Fell des Eisbaeren erscheint weiss, was ihm hilft, sich im Schnee zu tarnen.",
        difficulty = 1,
        funFact = "Die Haare des Eisbaeren sind eigentlich farblos und hohl. Sie leiten Sonnenlicht zur dunklen Haut."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Junge einer Kuh?",
        answerA = "Ferkel",
        answerB = "Lamm",
        answerC = "Kalb",
        answerD = "Fohlen",
        correctAnswer = 2,
        explanation = "Das Junge einer Kuh heisst Kalb. Es wird nach etwa 9 Monaten Tragzeit geboren.",
        difficulty = 1,
        funFact = "Ein neugeborenes Kalb kann kurz nach der Geburt bereits stehen und laufen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat den laengsten Hals der Welt?",
        answerA = "Dromedar",
        answerB = "Giraffe",
        answerC = "Elefant",
        answerD = "Okapi",
        correctAnswer = 1,
        explanation = "Die Giraffe hat mit bis zu 2,4 Metern den laengsten Hals aller lebenden Tiere.",
        difficulty = 1,
        funFact = "Trotz des langen Halses hat die Giraffe genauso viele Halswirbel wie der Mensch: sieben."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier wird als Haustier gehalten und sagt \"Miau\"?",
        answerA = "Hund",
        answerB = "Hamster",
        answerC = "Katze",
        answerD = "Meerschweinchen",
        correctAnswer = 2,
        explanation = "Katzen sind bekannt fuer ihr Miauen. Sie kommunizieren damit hauptsaechlich mit Menschen.",
        difficulty = 1,
        funFact = "Erwachsene Katzen miauen fast nur noch mit Menschen, nicht mehr mit anderen Katzen."
    ),

    Question(
        categoryId = 9,
        questionText = "In welchem Kontinent leben wilde Loewen hauptsaechlich?",
        answerA = "Asien",
        answerB = "Suedamerika",
        answerC = "Australien",
        answerD = "Afrika",
        correctAnswer = 3,
        explanation = "Wilde Loewen leben hauptsaechlich in Afrika suedlich der Sahara. Eine kleine Population lebt in Indien.",
        difficulty = 1,
        funFact = "Loewen sind die einzigen Katzen, die in Gruppen (Rudeln) zusammenleben."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Junge eines Pferdes?",
        answerA = "Fohlen",
        answerB = "Welpe",
        answerC = "Kueken",
        answerD = "Kaelber",
        correctAnswer = 0,
        explanation = "Das Junge eines Pferdes wird Fohlen genannt. Ein maennliches Fohlen heisst Hengstfohlen, ein weibliches Stutfohlen.",
        difficulty = 1,
        funFact = "Fohlen koennen wenige Stunden nach der Geburt bereits laufen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches ist das groesste Saeugetier der Welt?",
        answerA = "Elefant",
        answerB = "Blauwal",
        answerC = "Nilpferd",
        answerD = "Walross",
        correctAnswer = 1,
        explanation = "Der Blauwal ist das groesste Tier, das jemals auf der Erde gelebt hat. Er kann bis zu 30 Meter lang werden.",
        difficulty = 1,
        funFact = "Das Herz eines Blauwals ist so gross wie ein kleines Auto und kann bis zu 600 kg wiegen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat Streifen und lebt in Afrika?",
        answerA = "Zebra",
        answerB = "Gepard",
        answerC = "Loewe",
        answerD = "Giraffe",
        correctAnswer = 0,
        explanation = "Das Zebra ist bekannt fuer sein schwarz-weisses Streifenmuster und lebt in afrikanischen Savannen.",
        difficulty = 1,
        funFact = "Jedes Zebra hat ein einzigartiges Streifenmuster, aehnlich wie menschliche Fingerabdruecke."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das typische Merkmal eines Stachelschweins?",
        answerA = "Ein langer Schwanz",
        answerB = "Grosse Ohren",
        answerC = "Stacheln am Ruecken",
        answerD = "Gestreifte Fell",
        correctAnswer = 2,
        explanation = "Das Stachelschwein hat scharfe Stacheln an Ruecken und Flanken, die der Verteidigung gegen Raeuber dienen.",
        difficulty = 1,
        funFact = "Stacheln des Stachelschweins sind modifizierte Haare aus hartem Keratin."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie viele Hoecker hat ein Dromedar?",
        answerA = "Keinen",
        answerB = "Zwei",
        answerC = "Drei",
        answerD = "Einen",
        correctAnswer = 3,
        explanation = "Das Dromedar hat nur einen Hoecker, das Baktrische Kamel hingegen hat zwei Hoecker.",
        difficulty = 1,
        funFact = "Im Hoecker des Dromedars ist Fett gespeichert, keine Wasser, wie oft irrtuemlicherweise angenommen wird."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist das schnellste Landsaeugetier?",
        answerA = "Loewe",
        answerB = "Gepard",
        answerC = "Gazelle",
        answerD = "Pferd",
        correctAnswer = 1,
        explanation = "Der Gepard ist mit Spitzengeschwindigkeiten von bis zu 120 km/h das schnellste Landsaeugetier.",
        difficulty = 1,
        funFact = "Geparden koennen ihre Hoechstgeschwindigkeit nur fuer kurze Sprints von etwa 20-30 Sekunden halten."
    ),

    Question(
        categoryId = 9,
        questionText = "Was trinken Saeugetier-Babys kurz nach der Geburt?",
        answerA = "Wasser",
        answerB = "Fruchtsaft",
        answerC = "Muttermilch",
        answerD = "Blut",
        correctAnswer = 2,
        explanation = "Saeugetiere sind die einzige Tiergruppe, deren Jungtiere Muttermilch trinken. Daher der Name Saeugetier.",
        difficulty = 1,
        funFact = "Die Milch des Blauwals ist so reich an Fett, dass ein Kalb taeglich bis zu 90 Liter trinkt."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist fuer seinen langen Ruessel bekannt?",
        answerA = "Nilpferd",
        answerB = "Tapir",
        answerC = "Nashon",
        answerD = "Elefant",
        correctAnswer = 3,
        explanation = "Der Elefant besitzt einen langen muskuloesen Ruessel, den er zum Greifen, Trinken und Kommunizieren nutzt.",
        difficulty = 1,
        funFact = "Der Ruessel eines Elefanten enthaelt etwa 40.000 Muskeln und ist extrem beweglich."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Tiere leben in Rudeln und jagen gemeinsam?",
        answerA = "Woelfe",
        answerB = "Baeren",
        answerC = "Tiger",
        answerD = "Geparden",
        correctAnswer = 0,
        explanation = "Woelfe sind Rudeltiere, die in Familiengruppen zusammenleben und gemeinsam jagen.",
        difficulty = 1,
        funFact = "Ein Wolfsrudel wird von einem Alphapaar angefuehrt, das als einziges im Rudel Nachwuchs bekommt."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Saeugetier schlaft im Winter einen langen Schlaf?",
        answerA = "Hase",
        answerB = "Bar",
        answerC = "Eichhoernchen",
        answerD = "Fuchs",
        correctAnswer = 1,
        explanation = "Baeren halten einen Winterschlaf, bei dem ihre Koerpertemperatur und ihr Stoffwechsel stark absinken.",
        difficulty = 1,
        funFact = "Waehrend des Winterschlafs gebaren Baeren-Weibchen ihre Jungen, ohne aufzuwachen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Junge eines Hundes?",
        answerA = "Kitten",
        answerB = "Welpe",
        answerC = "Kaelber",
        answerD = "Fohlen",
        correctAnswer = 1,
        explanation = "Das Junge eines Hundes heisst Welpe. Hundewelpen werden mit geschlossenen Augen und Ohren geboren.",
        difficulty = 1,
        funFact = "Hundewelpen schlafen bis zu 20 Stunden pro Tag, da ihr Gehirn und Koerper stark wachsen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist das groesste Landraubtier?",
        answerA = "Loewe",
        answerB = "Tiger",
        answerC = "Eisbaer",
        answerD = "Leopard",
        correctAnswer = 2,
        explanation = "Der Eisbaer ist das groesste Landraubtier der Welt. Maennchen koennen bis zu 800 kg wiegen.",
        difficulty = 1,
        funFact = "Eisbaeren sind ausgezeichnete Schwimmer und koennen ohne Unterbrechung bis zu 100 Kilometer schwimmen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier traegt ihr Junges in einem Beutel?",
        answerA = "Kuh",
        answerB = "Hamster",
        answerC = "Kaenguru",
        answerD = "Walross",
        correctAnswer = 2,
        explanation = "Das Kaenguru ist ein Beuteltier und traegt sein Junges in einem Bauchbeutel, bis es gross genug ist.",
        difficulty = 1,
        funFact = "Ein neugeborenes Kaenguru ist nur so gross wie eine Weintraube und krabbelt selbst in den Beutel."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist verwandt mit dem Hausrind und lebt in der freien Natur Nordamerikas?",
        answerA = "Gnu",
        answerB = "Bison",
        answerC = "Yak",
        answerD = "Wisent",
        correctAnswer = 1,
        explanation = "Der amerikanische Bison ist eng mit Hausrind verwandt und lebt in Nordamerika. Er ist das Nationaltier der USA.",
        difficulty = 1,
        funFact = "Frueher lebten ueber 60 Millionen Bisons in Nordamerika. Durch Jagd wurden sie fast ausgerottet."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Farbe hat ein ausgewachsener Gepard hauptsaechlich?",
        answerA = "Grau mit Streifen",
        answerB = "Schwarz",
        answerC = "Braun mit weissen Punkten",
        answerD = "Gelblich mit schwarzen Punkten",
        correctAnswer = 3,
        explanation = "Geparden haben ein gelbliches Fell mit schwarzen Punkten, was ihnen gute Tarnung in der Savanne bietet.",
        difficulty = 1,
        funFact = "Geparden haben charakteristische schwarze Traenenstreifen im Gesicht, die die Sonnenblendung reduzieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier produziert Wolle fuer die Textilindustrie?",
        answerA = "Ziege",
        answerB = "Kuh",
        answerC = "Schaf",
        answerD = "Pferd",
        correctAnswer = 2,
        explanation = "Schafe werden hauptsaechlich wegen ihrer Wolle gehalten. Ein Schaf liefert jedes Jahr mehrere Kilogramm Wolle.",
        difficulty = 1,
        funFact = "Schafe muessen jaehrlich geschoren werden, da ihre Wolle sonst endlos weiterwaeachst."
    ),

    Question(
        categoryId = 9,
        questionText = "Wo leben wilde Pandas natuerlich?",
        answerA = "Indien",
        answerB = "China",
        answerC = "Japan",
        answerD = "Thailand",
        correctAnswer = 1,
        explanation = "Wilde Riesenpandas leben ausschliesslich in den Bergwaeldern Suedwestchinas.",
        difficulty = 1,
        funFact = "Riesenpandas verbringen bis zu 14 Stunden taeglich mit dem Fressen von Bambus."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist der groesste Primat der Welt?",
        answerA = "Schimpanse",
        answerB = "Gorilla",
        answerC = "Orang-Utan",
        answerD = "Gibbon",
        correctAnswer = 1,
        explanation = "Der Gorilla ist der groesste lebende Primat. Maennliche Gorillas koennen bis zu 220 kg wiegen.",
        difficulty = 1,
        funFact = "Gorillas koennen einige Gebaerdensprache erlernen und wurden erfolgreich trainiert, mit Menschen zu kommunizieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Was frisst ein Delphin hauptsaechlich?",
        answerA = "Algen",
        answerB = "Krebse",
        answerC = "Fische",
        answerD = "Plankton",
        correctAnswer = 2,
        explanation = "Delfine sind Raeuber und ernaehren sich hauptsaechlich von Fischen und Tintenfischen.",
        difficulty = 1,
        funFact = "Delfine jagen oft in Gruppen und treiben Fischschwaerme gemeinsam zusammen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat ein schwarz-weisses Fell und gilt als Nationalwappentier Chinas?",
        answerA = "Schneeleopard",
        answerB = "Riesenpanda",
        answerC = "Roter Panda",
        answerD = "Sibirischer Tiger",
        correctAnswer = 1,
        explanation = "Der Riesenpanda mit seinem markanten schwarz-weissen Fell ist das inoffizielle Nationaltier Chinas.",
        difficulty = 1,
        funFact = "Der Riesenpanda ist trotz seiner Baerenverwandtschaft kein reiner Fleischfresser, sondern frisst fast nur Bambus."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man einen maennlichen Elefanten?",
        answerA = "Elefantenbulle",
        answerB = "Elefantenhahn",
        answerC = "Elefantenbock",
        answerD = "Elefantenhengst",
        correctAnswer = 0,
        explanation = "Ein maennlicher Elefant heisst Bulle oder Elefantenbulle. Das Weibchen heisst Kuh.",
        difficulty = 1,
        funFact = "Maennliche Elefanten leben nach der Geschlechtsreife meist als Einzelgaenger oder in kleinen Maennchen-Gruppen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist bekannt fuer seinen sehr langen Winterschlaf von bis zu 8 Monaten?",
        answerA = "Igel",
        answerB = "Murmeltier",
        answerC = "Siebenschlaefer",
        answerD = "Haselmaus",
        correctAnswer = 2,
        explanation = "Der Siebenschlaefer ist bekannt fuer seinen langen Winterschlaf von bis zu 8 Monaten. Sein Name leitet sich davon ab.",
        difficulty = 1,
        funFact = "Waehrend des Winterschlafs faellt die Koerpertemperatur des Siebenschlaeferss auf fast Umgebungstemperatur ab."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat Flossen statt Beine und lebt im Meer, ist aber ein Saeugetier?",
        answerA = "Hai",
        answerB = "Delfin",
        answerC = "Tintenfisch",
        answerD = "Barsch",
        correctAnswer = 1,
        explanation = "Der Delfin ist ein Saeugetier, das vollstaendig im Wasser lebt, Lungen hat und Luft atmen muss.",
        difficulty = 1,
        funFact = "Delfine schlafen mit einer Hirnhaelfte gleichzeitig, sodass sie weiter atmen und auf Gefahren reagieren koennen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist ein Symbol fuer Geschwindigkeit und lebt in der afrikanischen Savanne?",
        answerA = "Hyaene",
        answerB = "Schakal",
        answerC = "Antilope",
        answerD = "Gepard",
        correctAnswer = 3,
        explanation = "Der Gepard gilt als Symbol fuer Schnelligkeit und ist das schnellste Landsaeugetier der Erde.",
        difficulty = 1,
        funFact = "Geparden koennen in drei Sekunden von 0 auf 100 km/h beschleunigen, schneller als die meisten Sportwagen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie kommunizieren Wale hauptsaechlich miteinander?",
        answerA = "Durch Gesten",
        answerB = "Durch Gerueche",
        answerC = "Durch Gesaenge und Laute",
        answerD = "Durch visuelle Signale",
        correctAnswer = 2,
        explanation = "Wale, besonders Buckelwale, kommunizieren durch komplexe Gesaenge und Laute, die weite Strecken ueberwinden.",
        difficulty = 1,
        funFact = "Der Gesang des Buckelwals kann bis zu 20 Minuten dauern und aendert sich jedes Jahr leicht."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist fuer seine Hornplatten auf dem Ruecken bekannt?",
        answerA = "Guerteltier",
        answerB = "Igel",
        answerC = "Schildkroete",
        answerD = "Pangolin",
        correctAnswer = 3,
        explanation = "Das Pangolin, auch Schuppentier genannt, ist das einzige Saeugetier mit Hornschuppen auf dem Koerper.",
        difficulty = 1,
        funFact = "Das Pangolin ist das am meisten gehandelte Wildtier der Welt und daher stark vom Aussterben bedroht."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier gilt als das intelligenteste Landsaeugetier nach dem Menschen?",
        answerA = "Elefant",
        answerB = "Hund",
        answerC = "Schimpanse",
        answerD = "Delfin",
        correctAnswer = 2,
        explanation = "Schimpansen gelten als die intelligentesten Tiere nach dem Menschen. Sie nutzen Werkzeuge und haben soziale Strukturen.",
        difficulty = 1,
        funFact = "Schimpansen teilen etwa 98,7 Prozent ihrer DNA mit dem Menschen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist ein Wal?",
        answerA = "Ein Fisch",
        answerB = "Ein Reptil",
        answerC = "Ein Saeugetier",
        answerD = "Ein Amphibium",
        correctAnswer = 2,
        explanation = "Wale sind keine Fische, sondern Saeugetiere. Sie atmen Luft, gebaeren lebende Junge und saeugen diese.",
        difficulty = 1,
        funFact = "Wale stammen von Landtieren ab, die vor etwa 50 Millionen Jahren ins Meer zurueckkehrten."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie heisst das weibliche Pferd?",
        answerA = "Stute",
        answerB = "Kuh",
        answerC = "Henne",
        answerD = "Ziege",
        correctAnswer = 0,
        explanation = "Ein weibliches Pferd heisst Stute. Das maennliche Pferd heisst Hengst und das kastrierte Maennchen Wallach.",
        difficulty = 1,
        funFact = "Pferde schlafen oft im Stehen dank eines besonderen Schlosssystems in ihren Beinen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier grunzt und lebt auf dem Bauernhof?",
        answerA = "Pferd",
        answerB = "Kuh",
        answerC = "Schwein",
        answerD = "Schaf",
        correctAnswer = 2,
        explanation = "Schweine grunzen und sind bekannt als Nutztiere auf Bauernhoefen. Sie sind sehr intelligente Tiere.",
        difficulty = 1,
        funFact = "Schweine sind in mancher Hinsicht intelligenter als Hunde und koennen einfache Videospiele steuern."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Tierart sind Woelfe?",
        answerA = "Katzenartige",
        answerB = "Hundartige",
        answerC = "Baerenartige",
        answerD = "Marderartige",
        correctAnswer = 1,
        explanation = "Woelfe gehoeren zur Familie der Hundartigen (Canidae), zu der auch Fuechse, Kojoten und Hausunde gehoeren.",
        difficulty = 1,
        funFact = "Alle Hausundrassen der Welt stammen vom Wolf ab. Der Wolf ist der Vorfahr aller Hunde."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist fuer seine roten oder orangen Felder bekannt und lebt im Wald?",
        answerA = "Waschbaer",
        answerB = "Dachs",
        answerC = "Fuchs",
        answerD = "Marder",
        correctAnswer = 2,
        explanation = "Der Fuchs hat ein rotoranges Fell und lebt in Waeldern, Feldern und sogar in Stadten.",
        difficulty = 1,
        funFact = "Fuechse sind sehr anpassungsfaehig und findet sich mittlerweile in fast allen Kontinenten, auch in Staedten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat ein Geweih?",
        answerA = "Ziege",
        answerB = "Elch",
        answerC = "Antilope",
        answerD = "Bueffel",
        correctAnswer = 1,
        explanation = "Elche haben ein beeindruckendes Geweih, das jedes Jahr neu waechst. Nur die Maennchen tragen ein Geweih.",
        difficulty = 1,
        funFact = "Das Geweih eines Elches kann eine Spannweite von bis zu 2 Metern erreichen und bis zu 20 kg wiegen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist fuer seinen lauten Gesang bekannt und lebt in Nordamerika?",
        answerA = "Grizzlybaer",
        answerB = "Puma",
        answerC = "Kojote",
        answerD = "Bison",
        correctAnswer = 2,
        explanation = "Kojoten sind bekannt fuer ihr charakteristisches Heulen, besonders in der Nacht.",
        difficulty = 1,
        funFact = "Kojoten haben sich trotz intensiver Bejaagung stark ausgebreitet und leben heute in ganz Nordamerika."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier schlaeft koeflueberhaengend an Baumzweigen?",
        answerA = "Waschbaer",
        answerB = "Fledermaus",
        answerC = "Faultier",
        answerD = "Opossum",
        correctAnswer = 2,
        explanation = "Das Faultier haengt fast sein ganzes Leben kopfueber an Baumzweigen und schlaeft bis zu 20 Stunden am Tag.",
        difficulty = 1,
        funFact = "Faultiere bewegen sich so langsam, dass Algen auf ihrem Fell wachsen, die ihnen Tarnung bieten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist der naechste Verwandte des Hausschweins in der Wildnis?",
        answerA = "Nilpferd",
        answerB = "Tapir",
        answerC = "Wildschwein",
        answerD = "Pekari",
        correctAnswer = 2,
        explanation = "Das Wildschwein ist der direkte wilde Vorfahre des Hausschweins. Beide koennen sich miteinander paaren.",
        difficulty = 1,
        funFact = "Wildschweine sind ausgezeichnete Schwimmer und koennen weite Fluesse problemlos ueberqueren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier hat die laengste Tragzeit unter den Landtieren?",
        answerA = "Giraffe",
        answerB = "Nilpferd",
        answerC = "Nashorn",
        answerD = "Elefant",
        correctAnswer = 3,
        explanation = "Elefanten haben mit etwa 22 Monaten die laengste Tragzeit aller Landtiere.",
        difficulty = 1,
        funFact = "Wegen der langen Tragzeit wird ein Elefantenjunges schon weit entwickelt und gross geboren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier fliegt nachts und frisst hauptsaechlich Muecken und Insekten?",
        answerA = "Eule",
        answerB = "Schwalbe",
        answerC = "Fledermaus",
        answerD = "Nachtfalter",
        correctAnswer = 2,
        explanation = "Fledermaeuse jagen nachts Insekten wie Muecken. Eine einzige Fledermaus kann hunderte Muecken pro Nacht fressen.",
        difficulty = 1,
        funFact = "Fledermaeuse sind die einzigen Saeugetiere mit echtem Flugvermoegen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie heisst das maennliche Rind?",
        answerA = "Eber",
        answerB = "Bulle",
        answerC = "Bock",
        answerD = "Hengst",
        correctAnswer = 1,
        explanation = "Ein maennliches Rind heisst Bulle. Das weibliche Rind heisst Kuh und das Junge Kalb.",
        difficulty = 1,
        funFact = "Rinder haben vier Magenabschnitte, was ihnen hilft, schwer verdauliches Gras zu verwerten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist bekannt fuer seinen geringelten buschigen Schwanz und lebt in Nordamerika?",
        answerA = "Fuchs",
        answerB = "Waschbaer",
        answerC = "Stinktier",
        answerD = "Dachs",
        correctAnswer = 1,
        explanation = "Der Waschbaer hat einen charakteristischen geringelten Schwanz und eine schwarze Gesichtsmaske.",
        difficulty = 1,
        funFact = "Waschbaeren waschen ihre Nahrung oft in Wasser, bevor sie sie fressen, was ihnen ihren Namen gab."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist bekannt dafuer, dass es sein Fell an Dornen oder Felsspalten zum Trocknen aufhaengt?",
        answerA = "Otter",
        answerB = "Biber",
        answerC = "Wasserspitzmaus",
        answerD = "Nerz",
        correctAnswer = 1,
        explanation = "Biber bauen Daemme aus Holz und Schlamm und legen Vorratsstaepel fuer den Winter an.",
        difficulty = 1,
        funFact = "Biber koennen Baeume mit einem Durchmesser von 20 cm in wenigen Stunden nagen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier heult und lebt oft in Europa in Waeldern?",
        answerA = "Luchs",
        answerB = "Wolf",
        answerC = "Fuechs",
        answerD = "Wildkatze",
        correctAnswer = 1,
        explanation = "Woelfe heulen um mit Rudelmitgliedern zu kommunizieren, Reviere zu markieren und sich zu koordinieren.",
        difficulty = 1,
        funFact = "Das Heulen eines Wolfs kann bei ruhigem Wetter bis zu 10 Kilometer weit gehoert werden."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Junge einer Katze?",
        answerA = "Welpe",
        answerB = "Fohlen",
        answerC = "Kaetzchen oder Kitten",
        answerD = "Ferkel",
        correctAnswer = 2,
        explanation = "Das Junge einer Katze heisst Kaetzchen oder auf Englisch Kitten. Katzen gebaren meist 3-6 Junge.",
        difficulty = 1,
        funFact = "Kaetzchen werden blind und taueboren geboren und oeffnen die Augen erst nach etwa 10 Tagen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Tier ist der naeherste Verwandte des Elefanten?",
        answerA = "Nilpferd",
        answerB = "Nashorn",
        answerC = "Seekuh",
        answerD = "Tapir",
        correctAnswer = 2,
        explanation = "Genetisch ist die Seekuh (Manati) tatsaechlich einer der naeheren Verwandten des Elefanten.",
        difficulty = 1,
        funFact = "Elefanten und Seekuehe teilen einen gemeinsamen Vorfahren, der vor etwa 55 Millionen Jahren lebte."
    ),

)
