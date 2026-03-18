package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsEasy4(): List<Question> = listOf(

    // ─── MUSICAL FILMS ────────────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "In welchem Musicalfilm singt John Travolta den Song 'Summer Nights'?",
        answerA = "Saturday Night Fever",
        answerB = "Grease",
        answerC = "Hairspray",
        answerD = "Footloose",
        correctAnswer = 1,
        explanation = "In 'Grease' (1978) singt John Travolta als Danny Zuko gemeinsam mit Olivia Newton-John als Sandy den Hit 'Summer Nights'.",
        difficulty = 1,
        funFact = "'Grease' wurde urspruenglich als Broadway-Musical aufgefuehrt, bevor es 1978 verfilmt wurde."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Schauspielerin spielt Sandy in dem Musicalfilm 'Grease' (1978)?",
        answerA = "Meryl Streep",
        answerB = "Farrah Fawcett",
        answerC = "Olivia Newton-John",
        answerD = "Donna Summer",
        correctAnswer = 2,
        explanation = "Olivia Newton-John spielte Sandy Olsson, die naive australische Schuelerfreundin von Danny Zuko.",
        difficulty = 1,
        funFact = "Olivia Newton-John war anfangs unsicher, ob sie die Rolle annehmen sollte, weil sie befuerchtete, fuer die Teenagerrolle zu alt zu sein."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Land spielt das Musical 'The Sound of Music'?",
        answerA = "Deutschland",
        answerB = "Schweiz",
        answerC = "Oesterreich",
        answerD = "Italien",
        correctAnswer = 2,
        explanation = "'The Sound of Music' (1965) spielt in Salzburg, Oesterreich, und basiert auf der wahren Geschichte der Familie von Trapp.",
        difficulty = 1,
        funFact = "Salzburg zieht bis heute viele Touristen an, die Drehorte aus 'The Sound of Music' besuchen wollen."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Schauspielerin spielt in 'The Sound of Music' (1965) die Gouvernante Maria?",
        answerA = "Audrey Hepburn",
        answerB = "Julie Andrews",
        answerC = "Doris Day",
        answerD = "Grace Kelly",
        correctAnswer = 1,
        explanation = "Julie Andrews verkoepert die lebhafte Noviziatin Maria, die Gouvernante der sieben Kinder von Trapp wird.",
        difficulty = 1,
        funFact = "Julie Andrews gewann kurz vor 'The Sound of Music' ihren ersten Oscar fuer 'Mary Poppins' (1964)."
    ),

    Question(
        categoryId = 4,
        questionText = "In welcher Stadt spielt das Musicalfilm-Drama 'La La Land' (2016)?",
        answerA = "New York",
        answerB = "Chicago",
        answerC = "San Francisco",
        answerD = "Los Angeles",
        correctAnswer = 3,
        explanation = "'La La Land' spielt in Los Angeles und folgt einer Schauspielerin und einem Jazzmusiker, die ihren Traemen nachjagen.",
        difficulty = 1,
        funFact = "Der Titel 'La La Land' ist ein Spitzname fuer Los Angeles und bedeutet gleichzeitig 'Traumwelt'."
    ),

    Question(
        categoryId = 4,
        questionText = "Wer spielte in 'La La Land' (2016) den Jazzpianisten Sebastian?",
        answerA = "Jake Gyllenhaal",
        answerB = "Ryan Gosling",
        answerC = "Channing Tatum",
        answerD = "Chris Evans",
        correctAnswer = 1,
        explanation = "Ryan Gosling spielte Sebastian, einen leidenschaftlichen Jazzpianisten, der fuer seine Traumfrau und sein Traumlokal kaempft.",
        difficulty = 1,
        funFact = "Ryan Gosling lernte eigens fuer die Rolle monatelang Klavier spielen."
    ),

    // ─── ROMANTIC COMEDIES ────────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "In welchem Liebesfilm sagt Hugh Grant als Buchaendler: 'Ich bin nur ein Junge, der steht vor einem Maedchen...'?",
        answerA = "Vier Hochzeiten und ein Todesfall",
        answerB = "Notting Hill",
        answerC = "Bridget Jones - Schokolade zum Fruehstueck",
        answerD = "About a Boy",
        correctAnswer = 1,
        explanation = "In 'Notting Hill' (1999) spricht William Thacker (Hugh Grant) diesen ruehrenden Satz zu der Filmschauspielerin Anna Scott (Julia Roberts).",
        difficulty = 1,
        funFact = "'Notting Hill' wurde groesstenteils im gleichnamigen Stadtteil von London gedreht."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film zeigt Meg Ryan und Tom Hanks als Paar, die sich erst am Ende treffen?",
        answerA = "E-Mail fuer Dich",
        answerB = "Joe gegen den Vulkan",
        answerC = "Schlaflos in Seattle",
        answerD = "Vergiss Mein Nicht",
        correctAnswer = 2,
        explanation = "'Schlaflos in Seattle' (1993) zeigt Annie Reed (Meg Ryan) und Sam Baldwin (Tom Hanks), die sich erst ganz am Ende auf dem Empire State Building begegnen.",
        difficulty = 1,
        funFact = "Der Film wurde inspiriert von 'Affaere in New York' (1957), auf den im Film auch direkt Bezug genommen wird."
    ),

    Question(
        categoryId = 4,
        questionText = "In welcher romantischen Komoedie streiten sich Billy Crystal und Meg Ryan darueber, ob Maenner und Frauen befreundet sein koennen?",
        answerA = "City Slickers",
        answerB = "Wenn Harry Sally trifft",
        answerC = "Notting Hill",
        answerD = "Pretty Woman",
        correctAnswer = 1,
        explanation = "In 'Wenn Harry Sally trifft' (1989) debattieren Harry Burns und Sally Albright ueber die Frage, ob Freundschaft zwischen Mann und Frau moeglich ist.",
        difficulty = 1,
        funFact = "Die beruehmt-beruehmte Restaurant-Szene mit dem gespielten Orgasmus fand im Katz's Delicatessen in New York statt."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Schauspielerin spielt in 'Pretty Woman' (1990) die Prostituierte Vivian?",
        answerA = "Sandra Bullock",
        answerB = "Demi Moore",
        answerC = "Julia Roberts",
        answerD = "Winona Ryder",
        correctAnswer = 2,
        explanation = "Julia Roberts spielte Vivian Ward, eine Strassenprostituierte, die sich in den reichen Geschaeftsmann Edward Lewis (Richard Gere) verliebt.",
        difficulty = 1,
        funFact = "Julia Roberts wurde durch 'Pretty Woman' zum Weltstar und verdiente fuer die Rolle 300.000 Dollar."
    ),

    // ─── ACTION FILMS ─────────────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt in 'Stirb Langsam' (Die Hard) den Polizisten John McClane?",
        answerA = "Sylvester Stallone",
        answerB = "Arnold Schwarzenegger",
        answerC = "Bruce Willis",
        answerD = "Steven Seagal",
        correctAnswer = 2,
        explanation = "Bruce Willis wurde durch seine Rolle als John McClane in 'Stirb Langsam' (1988) zum Actionstar.",
        difficulty = 1,
        funFact = "Die Produzenten wollten urspruenglich Arnold Schwarzenegger fuer die Rolle, doch Bruce Willis wurde schliesslich gecastet."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Gebaeude findet der groessteTeil des Films 'Stirb Langsam' (1988) statt?",
        answerA = "Pentagon",
        answerB = "Wolkenkratzer Nakatomi Plaza",
        answerC = "Kennedy-Flughafen",
        answerD = "Weisses Haus",
        correctAnswer = 1,
        explanation = "Fast der gesamte Film spielt im Nakatomi Plaza, einem Buerogeb Hochhaus in Los Angeles, das von Terroristen besetzt wird.",
        difficulty = 1,
        funFact = "Das echte Gebaeude heisst Fox Plaza und ist der Firmensitz von 20th Century Fox in Los Angeles."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt Ethan Hunt in der 'Mission: Impossible'-Filmreihe?",
        answerA = "Keanu Reeves",
        answerB = "Tom Cruise",
        answerC = "Matt Damon",
        answerD = "Vin Diesel",
        correctAnswer = 1,
        explanation = "Tom Cruise spielt seit 1996 den IMF-Agenten Ethan Hunt und ist bekannt dafuer, viele Stunts selbst auszufuehren.",
        difficulty = 1,
        funFact = "Tom Cruise haengte sich fuer 'Mission: Impossible - Rogue Nation' tatsaechlich an einem echten Airbus A400M waehrend des Starts fest."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt Dominic Toretto in der 'Fast & Furious'-Filmreihe?",
        answerA = "Dwayne Johnson",
        answerB = "Paul Walker",
        answerC = "Vin Diesel",
        answerD = "Tyrese Gibson",
        correctAnswer = 2,
        explanation = "Vin Diesel spielt seit dem ersten Teil (2001) Dominic Toretto, den Anführer einer Gruppe von Straßenrennfahrern.",
        difficulty = 1,
        funFact = "Vin Diesel produziert die Filme auch selbst und hat massgeblich daran mitgewirkt, die Serie zu einer der erfolgreichsten Franchises weltweit zu machen."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielte den Boesewich Hans Gruber in 'Stirb Langsam' (1988)?",
        answerA = "Gary Oldman",
        answerB = "Alan Rickman",
        answerC = "Jeremy Irons",
        answerD = "Anthony Hopkins",
        correctAnswer = 1,
        explanation = "Alan Rickman spielte den deutschen Terroristen Hans Gruber und machte mit dieser Rolle seinen Durchbruch als Filmschauspieler.",
        difficulty = 1,
        funFact = "Es war Alan Rickmans erster Kinofilm ueberhaupt, und er wurde sofort zu einem der beliebtesten Filmboesewichte aller Zeiten."
    ),

    // ─── SCI-FI CLASSICS ──────────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur hat den Film 'E.T. - Der Ausserirdische' (1982) gedreht?",
        answerA = "George Lucas",
        answerB = "James Cameron",
        answerC = "Steven Spielberg",
        answerD = "Ridley Scott",
        correctAnswer = 2,
        explanation = "Steven Spielberg drehte 'E.T.' und schuf damit einen der herzruehrendsten und erfolgreichsten Filme der Kinogeschichte.",
        difficulty = 1,
        funFact = "'E.T.' war bis 1993 der erfolgreichste Film aller Zeiten, bis er von Spielbergs eigenem 'Jurassic Park' ueberholt wurde."
    ),

    Question(
        categoryId = 4,
        questionText = "Was ist der Spitzname des kleinen Ausserirdischen im Film 'E.T.' (1982)?",
        answerA = "Yoda",
        answerB = "E.T.",
        answerC = "Alf",
        answerD = "Roger",
        correctAnswer = 1,
        explanation = "Das Kind Elliott nennt den Ausserirdischen 'E.T.', was fuer 'Extra-Terrestrial' (Ausserirdischer) steht.",
        difficulty = 1,
        funFact = "Der Ruf 'E.T. nach Hause telefonieren' zaehlt zu den bekanntesten Filmzitaten der Geschichte."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Jahr setzt die Handlung des Films 'Zurueck in die Zukunft' (1985) an, als Marty McFly in die Vergangenheit reist?",
        answerA = "1945",
        answerB = "1955",
        answerC = "1965",
        answerD = "1975",
        correctAnswer = 1,
        explanation = "Marty McFly landet im Jahr 1955 und muss sicherstellen, dass sich seine Eltern ineinander verlieben, damit er selbst existiert.",
        difficulty = 1,
        funFact = "Der DeLorean als Zeitmaschine wurde gewaehlt, weil er wie ein Raumschiff aussieht und Kinder beim Anblick dachten, er komme aus der Zukunft."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie heisst der verueckte Wissenschaftler in 'Zurueck in die Zukunft' (1985)?",
        answerA = "Professor Brown",
        answerB = "Dr. Emmett Brown",
        answerC = "Dr. Victor Frankenstein",
        answerD = "Professor Flux",
        correctAnswer = 1,
        explanation = "Dr. Emmett 'Doc' Brown, gespielt von Christopher Lloyd, ist der Erfinder der Zeitmaschine aus einem DeLorean.",
        difficulty = 1,
        funFact = "Christopher Lloyd entwickelte die Figur des Doc Brown als jemanden, der vollstaendig in seiner eigenen Welt lebt."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Fahrzeug dient in 'Zurueck in die Zukunft' als Zeitmaschine?",
        answerA = "Ferrari Testarossa",
        answerB = "Porsche 911",
        answerC = "DeLorean DMC-12",
        answerD = "Lamborghini Countach",
        correctAnswer = 2,
        explanation = "Der DeLorean DMC-12 mit Fluegeltueren wurde von Doc Brown zur Zeitmaschine umgebaut und benoetigt 88 Meilen pro Stunde zur Zeitreise.",
        difficulty = 1,
        funFact = "Der echte DeLorean wurde von 1981 bis 1982 produziert und war damals ein kommerzieller Misserfolg."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Film erscheint das Alien-Monster zum ersten Mal aus dem Koerper eines Besatzungsmitglieds?",
        answerA = "Predator",
        answerB = "The Thing",
        answerC = "Alien",
        answerD = "Species",
        correctAnswer = 2,
        explanation = "In Ridley Scotts 'Alien' (1979) bricht ein Xenomorph spektakulaer aus dem Brustkorb des Astronauten Kane hervor.",
        difficulty = 1,
        funFact = "Die Schauspieler wussten nicht genau, wie das Monster aussehen wuerde, weshalb ihre Schreckensreaktionen echt waren."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Schauspielerin spielt die Heldin Ripley im Film 'Alien' (1979)?",
        answerA = "Linda Hamilton",
        answerB = "Sigourney Weaver",
        answerC = "Jamie Lee Curtis",
        answerD = "Jodie Foster",
        correctAnswer = 1,
        explanation = "Sigourney Weaver spielte Ellen Ripley und wurde damit zur Ikone des Sci-Fi-Actionkinos.",
        difficulty = 1,
        funFact = "Sigourney Weaver war bei den Oscars nominiert fuer ihre Darstellung in 'Aliens' (1986), dem zweiten Teil der Reihe."
    ),

    // ─── WAR FILMS ────────────────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur drehte den Kriegsfilm 'Der Soldat James Ryan' (1998)?",
        answerA = "Oliver Stone",
        answerB = "Clint Eastwood",
        answerC = "Steven Spielberg",
        answerD = "Francis Ford Coppola",
        correctAnswer = 2,
        explanation = "Steven Spielberg inszenierte den Film, der mit einer brutalen Darstellung der Landung am Omaha Beach beginnt.",
        difficulty = 1,
        funFact = "Spielberg spendete seinen gesamten Regiehonorar von 'Der Soldat James Ryan' an Kriegsveteranen-Organisationen."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt die Hauptrolle in 'Der Soldat James Ryan' (1998)?",
        answerA = "Tom Hanks",
        answerB = "Tom Cruise",
        answerC = "Brad Pitt",
        answerD = "Mel Gibson",
        correctAnswer = 0,
        explanation = "Tom Hanks spielt Captain John Miller, der den Auftrag bekommt, den letzten ueberlebenden Ryan-Bruder zu finden und zurueckzubringen.",
        difficulty = 1,
        funFact = "Viele Kriegsveteranen berichteten, dass die Omaha-Beach-Szene die realistischste Darstellung war, die sie je im Kino gesehen hatten."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur drehte den Kriegsfilm 'Dunkirk' (2017)?",
        answerA = "Steven Spielberg",
        answerB = "Christopher Nolan",
        answerC = "Ridley Scott",
        answerD = "Denis Villeneuve",
        correctAnswer = 1,
        explanation = "Christopher Nolan drehte 'Dunkirk' und erzaehlte die Geschichte der Evakuierung von Duenkirchen aus drei Perspektiven.",
        difficulty = 1,
        funFact = "Nolan verzichtete weitgehend auf CGI und nutzte echte Schiffe und Flugzeuge aus dem Zweiten Weltkrieg."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches historische Ereignis zeigt der Film 'Dunkirk' (2017)?",
        answerA = "D-Day-Landung in der Normandie",
        answerB = "Evakuierung britischer Soldaten aus Frankreich 1940",
        answerC = "Belagerung von Stalingrad",
        answerD = "Luftschlacht um England",
        correctAnswer = 1,
        explanation = "'Dunkirk' zeigt die Evakuierung 'Operation Dynamo' von 1940, bei der ueber 300.000 alliierte Soldaten aus dem nordfranzœsischen Duenkirchen gerettet wurden.",
        difficulty = 1,
        funFact = "An der Rettungsaktion beteiligten sich neben Kriegsschiffen auch Hunderte zivile Boote aus England."
    ),

    // ─── GERMAN TV SHOWS ──────────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "In welchem Jahrzehnt spielt die deutsche Spionageserie 'Deutschland 83'?",
        answerA = "1970er Jahre",
        answerB = "1980er Jahre",
        answerC = "1990er Jahre",
        answerD = "2000er Jahre",
        correctAnswer = 1,
        explanation = "'Deutschland 83' spielt 1983 und folgt einem jungen DDR-Soldaten, der als Spion in den Westen eingeschleust wird.",
        difficulty = 1,
        funFact = "'Deutschland 83' war die erste deutschsprachige Serie, die auf dem US-Sender SundanceTV ausgestrahlt wurde."
    ),

    Question(
        categoryId = 4,
        questionText = "In welcher deutschen Stadt spielt die Netflix-Serie 'Dark'?",
        answerA = "Hamburg",
        answerB = "Berlin",
        answerC = "Winden",
        answerD = "Koeln",
        correctAnswer = 2,
        explanation = "Die Serie spielt in der fiktiven Kleinstadt Winden und dreht sich um Zeitreisen, verschwundene Kinder und vier miteinander verwobene Familien.",
        difficulty = 1,
        funFact = "'Dark' wurde in der fiktiven Stadt Winden gedreht, die eigentlich mehrere echte Orte in Deutschland darstellt."
    ),

    Question(
        categoryId = 4,
        questionText = "Worum geht es hauptsaechlich in der deutschen Netflix-Serie 'Dark'?",
        answerA = "Bankraub in Berlin",
        answerB = "Zeitreisen und verschwundene Kinder",
        answerC = "Mafia in Hamburg",
        answerD = "Polizeiermittlungen in Frankfurt",
        correctAnswer = 1,
        explanation = "'Dark' dreht sich um ein Zeitreise-Paradoxon in einer deutschen Kleinstadt, bei dem Kinder verschwinden und vier Familien ueber Generationen miteinander verflochten sind.",
        difficulty = 1,
        funFact = "Die Serie umfasst drei Staffeln und deckt einen Zeitraum von 1953 bis 2053 ab."
    ),

    Question(
        categoryId = 4,
        questionText = "In welcher deutschen Stadt spielt die historische Krimiserie 'Babylon Berlin'?",
        answerA = "Muenchen",
        answerB = "Hamburg",
        answerC = "Berlin",
        answerD = "Dresden",
        correctAnswer = 2,
        explanation = "'Babylon Berlin' spielt in der Weimarer Republik der 1920er Jahre und zeigt das dekadente, aber politisch instabile Berlin dieser Zeit.",
        difficulty = 1,
        funFact = "'Babylon Berlin' ist die teuerste nicht-englischsprachige Fernsehserie der Geschichte mit einem Budget von ueber 40 Millionen Euro."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt den Kriminalkommissar Gereon Rath in 'Babylon Berlin'?",
        answerA = "Tom Beck",
        answerB = "Volker Bruch",
        answerC = "Florian David Fitz",
        answerD = "Moritz Bleibtreu",
        correctAnswer = 1,
        explanation = "Volker Bruch spielt Gereon Rath, einen Koelner Kriminalkommissar, der mit traumatischen Kriegserinnerungen kaempft und in Berlin Fuss fasst.",
        difficulty = 1,
        funFact = "Die Serie basiert auf den Romanen von Volker Kutscher, die bereits vor der Serienadaption Bestseller waren."
    ),

    // ─── CHILDREN'S TV ────────────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Auf welchem deutschen Fernsehsender laeuft die 'Sendung mit der Maus' traditionell?",
        answerA = "RTL",
        answerB = "ProSieben",
        answerC = "Das Erste (ARD)",
        answerD = "ZDF",
        correctAnswer = 2,
        explanation = "Die 'Sendung mit der Maus' wird seit 1971 im WDR und im Ersten (ARD) sonntags ausgestrahlt.",
        difficulty = 1,
        funFact = "Die 'Sendung mit der Maus' existiert seit 1971 und gilt als eine der aeltesten und beliebtesten Kindersendungen Deutschlands."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Tiere sind die Maskottchen der 'Sendung mit der Maus'?",
        answerA = "Maus und Elefant",
        answerB = "Maus und Ente",
        answerC = "Maus und Hase",
        answerD = "Maus und Katze",
        correctAnswer = 0,
        explanation = "Die orangefarbene Maus und der blaue Elefant sind die bekanntesten Figuren der Sendung, der kleine Ente kommt noch dazu.",
        difficulty = 1,
        funFact = "Die Maus hat trotz ihres Namens keinen offiziellen Eigennamen und wird einfach nur 'die Maus' genannt."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie heisst die amerikanische Kinderfernsehserie, die in Deutschland als 'Sesamstrasse' bekannt ist?",
        answerA = "Sesame Street",
        answerB = "Mr. Rogers' Neighborhood",
        answerC = "Mister Rogers",
        answerD = "Captain Kangaroo",
        correctAnswer = 0,
        explanation = "'Sesame Street' (Sesamstrasse) ist eine amerikanische Sendung, die 1969 startete und in Deutschland stark bearbeitet ausgestrahlt wurde.",
        difficulty = 1,
        funFact = "In der deutschen Version der Sesamstrasse gibt es zusaetzliche, eigens produzierte Segmente mit deutschen Charakteren."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher pelzige Charakter aus der Sesamstrasse wohnt in einer Muelltonne?",
        answerA = "Ernie",
        answerB = "Bert",
        answerC = "Oscar, der Griesgram",
        answerD = "Das Kruemelmonster",
        correctAnswer = 2,
        explanation = "Oscar der Griesgram ist ein grummeliger gruener Charakter, der in einer Muelltonne an der Sesamstrasse wohnt.",
        difficulty = 1,
        funFact = "Oscar war urspruenglich orange gef aerbt, wurde aber in der zweiten Staffel dauerhaft gruen."
    ),

    Question(
        categoryId = 4,
        questionText = "Welchen Snack liebt das Kruemelmonster aus der Sesamstrasse ueber alles?",
        answerA = "Pizza",
        answerB = "Kekse",
        answerC = "Eis",
        answerD = "Spaghetti",
        correctAnswer = 1,
        explanation = "Das Kruemelmonster (im Original Cookie Monster) ist bekannt fuer seine unstillbare Liebe zu Keksen und seinen Ruf 'NOM NOM NOM'.",
        difficulty = 1,
        funFact = "In neueren Episoden erklaert das Kruemelmonster, dass Kekse ein 'manchmal'-Essen sind, um gesunde Ernaehrung zu foerdern."
    ),

    // ─── FAMOUS CHILD ACTORS ──────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher Kinderstar spielte Kevin McCallister in 'Kevin - Allein zu Haus' (1990)?",
        answerA = "Haley Joel Osment",
        answerB = "Macaulay Culkin",
        answerC = "Jake Lloyd",
        answerD = "Jonathan Taylor Thomas",
        correctAnswer = 1,
        explanation = "Macaulay Culkin spielte den 8-jaehrigen Kevin, der allein zu Hause zwei Einbrecher austrickst.",
        difficulty = 1,
        funFact = "Macaulay Culkin verdiente mit 'Kevin - Allein zu Haus' 100.000 Dollar und wurde zu einem der bestbezahlten Kinderstars der Geschichte."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Film sah Haley Joel Osment 'tote Menschen'?",
        answerA = "Stuart Little",
        answerB = "Der sechste Sinn",
        answerC = "A.I. Kuenstliche Intelligenz",
        answerD = "Pay It Forward",
        correctAnswer = 1,
        explanation = "In 'Der sechste Sinn' (1999) spielt Haley Joel Osment den Jungen Cole Sear, der tote Menschen sehen kann.",
        difficulty = 1,
        funFact = "Die Schlusswendung des Films gilt als eine der ueberraschendsten in der Filmgeschichte."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Kinderstar spielte als Kind Anakin Skywalker in 'Star Wars: Episode I'?",
        answerA = "Elijah Wood",
        answerB = "Jake Lloyd",
        answerC = "Hayden Christensen",
        answerD = "Daniel Radcliffe",
        correctAnswer = 1,
        explanation = "Jake Lloyd spielte den jungen Anakin Skywalker in 'Die dunkle Bedrohung' (1999), bevor Hayden Christensen die Rolle als Erwachsener uebernahm.",
        difficulty = 1,
        funFact = "Jake Lloyd wurde fuer die Rolle unter mehr als 3.000 Kindern ausgewaehlt."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler wurde durch die Rolle des Harry Potter weltberuehmt?",
        answerA = "Rupert Grint",
        answerB = "Tom Felton",
        answerC = "Daniel Radcliffe",
        answerD = "Matthew Lewis",
        correctAnswer = 2,
        explanation = "Daniel Radcliffe spielte Harry Potter in allen acht Verfilmungen von 2001 bis 2011 und wurde damit zum bekanntesten Kinderstar der 2000er Jahre.",
        difficulty = 1,
        funFact = "Daniel Radcliffe war 11 Jahre alt, als er fuer 'Harry Potter und der Stein der Weisen' gecastet wurde."
    ),

    // ─── FILM STUDIOS ─────────────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welches Filmstudio ist bekannt fuer sein markantes Logo mit dem rohrenden Loewen?",
        answerA = "Universal Pictures",
        answerB = "Paramount Pictures",
        answerC = "Warner Bros.",
        answerD = "Metro-Goldwyn-Mayer (MGM)",
        correctAnswer = 3,
        explanation = "Das brüllende Loewen-Logo ist das Markenzeichen von Metro-Goldwyn-Mayer (MGM), einem der aeltesten Filmstudios Hollywoods.",
        difficulty = 1,
        funFact = "Das MGM-Loewen-Logo gibt es seit 1924, und im Laufe der Zeit wurden verschiedene Loewen fuer die Aufnahmen verwendet."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Filmstudio ist bekannt fuer sein Logo mit einem Berg und Sternen?",
        answerA = "Warner Bros.",
        answerB = "Paramount Pictures",
        answerC = "Universal Pictures",
        answerD = "Sony Pictures",
        correctAnswer = 1,
        explanation = "Das Paramount-Logo zeigt einen schneebedeckten Berg mit einem Sternenkranz und ist eines der bekanntesten Logos der Filmbranche.",
        difficulty = 1,
        funFact = "Paramount Pictures ist das aelteste noch bestehende Filmstudio in Hollywood und wurde 1912 gegruendet."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Filmstudio steckt hinter der Harry-Potter-Filmreihe?",
        answerA = "Universal Pictures",
        answerB = "Walt Disney Pictures",
        answerC = "Warner Bros.",
        answerD = "Paramount Pictures",
        correctAnswer = 2,
        explanation = "Warner Bros. produzierte alle acht Harry-Potter-Filme und besitzt auch die Filmrechte am Wizarding-World-Universum.",
        difficulty = 1,
        funFact = "Warner Bros. eroeffnete in London den 'Harry Potter Studio Tour', der Millionen von Fans jaehrlich besuchen."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Filmstudio hat das 'Jurassic Park'-Franchise produziert?",
        answerA = "Warner Bros.",
        answerB = "Universal Pictures",
        answerC = "Paramount Pictures",
        answerD = "20th Century Fox",
        correctAnswer = 1,
        explanation = "Universal Pictures produzierte 'Jurassic Park' (1993) und alle Nachfolger, einschliesslich der 'Jurassic World'-Trilogie.",
        difficulty = 1,
        funFact = "Das Universal-Logo mit der sich drehenden Weltkugel existiert seit 1914 und ist eines der aeltesten Filmlogos der Welt."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Filmstudio produziert die Marvel-Superheldenfilme (MCU)?",
        answerA = "Warner Bros.",
        answerB = "Universal Pictures",
        answerC = "Walt Disney Pictures / Marvel Studios",
        answerD = "Sony Pictures",
        correctAnswer = 2,
        explanation = "Seit der Uebernahme von Marvel durch Disney im Jahr 2009 werden die MCU-Filme von Walt Disney Pictures und Marvel Studios produziert.",
        difficulty = 1,
        funFact = "Disney kaufte Marvel Entertainment 2009 fuer etwa 4 Milliarden Dollar - eine der profitabelsten Uebernahmen in der Filmgeschichte."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Filmstudio ist bekannt fuer die Looney-Tunes-Charaktere wie Bugs Bunny?",
        answerA = "Universal Pictures",
        answerB = "Walt Disney Pictures",
        answerC = "Warner Bros.",
        answerD = "Paramount Pictures",
        correctAnswer = 2,
        explanation = "Bugs Bunny, Daffy Duck, Tweety und alle anderen Looney-Tunes-Figuren gehoeren zu Warner Bros., wo sie seit den 1930er Jahren entstanden sind.",
        difficulty = 1,
        funFact = "Bugs Bunny feierte sein offizielles Debut im Kurzfilm 'A Wild Hare' im Jahr 1940."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Filmstudio verwendet das Logo mit dem animierten Zauberer-Schloss?",
        answerA = "DreamWorks Animation",
        answerB = "Pixar Animation Studios",
        answerC = "Walt Disney Pictures",
        answerD = "Universal Pictures",
        correctAnswer = 2,
        explanation = "Das Schloss Cinderella-Schloss mit Feuerwerk ist das Markenzeichen von Walt Disney Pictures.",
        difficulty = 1,
        funFact = "Das Schloss im Disney-Logo ist inspiriert vom Schloss Neuschwanstein in Bayern, das wiederum Walt Disney zu Dornroeschenschloss inspirierte."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Filmstudio produzierte die 'Transformers'-Filmreihe von Michael Bay?",
        answerA = "Universal Pictures",
        answerB = "Warner Bros.",
        answerC = "Paramount Pictures",
        answerD = "Sony Pictures",
        correctAnswer = 2,
        explanation = "Paramount Pictures produzierte die gesamte Michael-Bay-'Transformers'-Reihe von 2007 bis 2017.",
        difficulty = 1,
        funFact = "Die 'Transformers'-Filme basierten auf dem Hasbro-Spielzeug und der Zeichentrickserie aus den 1980er Jahren."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Filmstudio steht hinter der 'Schindlers Liste' (1993)?",
        answerA = "Warner Bros.",
        answerB = "Universal Pictures",
        answerC = "Paramount Pictures",
        answerD = "MGM",
        correctAnswer = 1,
        explanation = "Universal Pictures produzierte 'Schindlers Liste', Steven Spielbergs historisches Drama ueber den Holocaust.",
        difficulty = 1,
        funFact = "Spielberg lehnte sein Regiehonorar ab und bezeichnete es als 'Blutgeld' - er spendete es an die USC Shoah Foundation."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches grosse Filmstudio wurde von den Brueder Warner im Jahr 1923 gegruendet?",
        answerA = "Universal Pictures",
        answerB = "Warner Bros.",
        answerC = "Paramount Pictures",
        answerD = "Columbia Pictures",
        correctAnswer = 1,
        explanation = "Harry, Albert, Sam und Jack Warner gruendeten Warner Bros. Pictures 1923 in Burbank, Kalifornien.",
        difficulty = 1,
        funFact = "Warner Bros. produzierte 1927 mit 'The Jazz Singer' den ersten Tonfilm der Geschichte."
    )

)
