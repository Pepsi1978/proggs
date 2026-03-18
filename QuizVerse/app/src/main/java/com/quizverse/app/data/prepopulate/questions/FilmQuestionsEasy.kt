package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsEasy(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 4,
        questionText = "Welcher Disney-Film handelt von einem Koenig der Loewen?",
        answerA = "Bambi",
        answerB = "Dschungelbuch",
        answerC = "Der Koenig der Loewen",
        answerD = "Dumbo",
        correctAnswer = 2, // C
        explanation = "Der Koenig der Loewen aus dem Jahr 1994 ist ein Disney-Klassiker ueber den jungen Loewen Simba, der seinen Platz als Koenig der Savanne finden muss.",
        difficulty = 1,
        funFact = "Der Koenig der Loewen war bei seiner Veroeffentlichung der bis dahin erfolgreichste Zeichentrickfilm aller Zeiten."
    ),

    // Question 2
    Question(
        categoryId = 4,
        questionText = "In welchem Film singt eine Meerjungfrau den Song 'Unter dem Meer'?",
        answerA = "Arielle, die Meerjungfrau",
        answerB = "Findet Nemo",
        answerC = "Moana",
        answerD = "Die kleine Seejungfrau",
        correctAnswer = 0, // A
        explanation = "In Arielle, die Meerjungfrau (1989) singt der Krabbe Sebastian den beruehm ten Song 'Unter dem Meer', um Arielle davon zu ueberzeugen, in der See zu bleiben.",
        difficulty = 1,
        funFact = "Arielle, die Meerjungfrau laeute te eine neue Bluetezeit der Disney-Animationsfilme ein, die als 'Disney Renaissance' bekannt ist."
    ),

    // Question 3
    Question(
        categoryId = 4,
        questionText = "Wer ist der Boesewich t in 'Star Wars: Eine neue Hoffnung'?",
        answerA = "Yoda",
        answerB = "Darth Vader",
        answerC = "Han Solo",
        answerD = "Obi-Wan Kenobi",
        correctAnswer = 1, // B
        explanation = "Darth Vader ist der Hauptschurke in Star Wars und einer der bekanntesten Filmboesewichte der Geschichte. Er ist der Anfuehrer der imperialen Streitkraefte.",
        difficulty = 1,
        funFact = "Die ikonische Atemmaske von Darth Vader wurde so gestaltet, dass sie sowohl bedrohlich als auch unvergesslich wirkt."
    ),

    // Question 4
    Question(
        categoryId = 4,
        questionText = "In welcher Schule lernt Harry Potter Zauberei?",
        answerA = "Durmstrang",
        answerB = "Beauxbatons",
        answerC = "Hogwarts",
        answerD = "Ilvermorny",
        correctAnswer = 2, // C
        explanation = "Hogwarts ist die Zauberschule, in der Harry Potter zusammen mit seinen Freunden Ron und Hermine das Zaubern erlernt. Sie liegt in Schottland.",
        difficulty = 1,
        funFact = "Die Hogwarts-Szenen in den Filmen wurden hauptsaechlich in echten englischen Kathedralen und Schloessern gedreht."
    ),

    // Question 5
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt Iron Man / Tony Stark im Marvel Cinematic Universe?",
        answerA = "Chris Evans",
        answerB = "Chris Hemsworth",
        answerC = "Robert Downey Jr.",
        answerD = "Mark Ruffalo",
        correctAnswer = 2, // C
        explanation = "Robert Downey Jr. verkoepert Tony Stark alias Iron Man seit dem ersten Marvel-Film aus dem Jahr 2008. Seine Darstellung praeg te das gesamte Marvel Cinematic Universe.",
        difficulty = 1,
        funFact = "Robert Downey Jr. spielte Iron Man in insgesamt zehn Marvel-Filmen und wurde damit zum Gesicht des MCU."
    ),

    // Question 6
    Question(
        categoryId = 4,
        questionText = "Welcher Film gewann 1998 den Oscar als Bester Film?",
        answerA = "Schindlers Liste",
        answerB = "Titanic",
        answerC = "Gladiator",
        answerD = "Braveheart",
        correctAnswer = 1, // B
        explanation = "Titanic von Regisseur James Cameron gewann 1998 elf Oscars, darunter Bester Film und Beste Regie, und stellte damit den damaligen Rekord ein.",
        difficulty = 1,
        funFact = "Titanic war der erste Film ueberhaupt, der weltweit mehr als eine Milliarde US-Dollar an den Kinokassen einspielte."
    ),

    // Question 7
    Question(
        categoryId = 4,
        questionText = "Welche Figur ist die Hauptfigur in 'Findet Nemo'?",
        answerA = "Dorie",
        answerB = "Marlin",
        answerC = "Nemo",
        answerD = "Bruce",
        correctAnswer = 2, // C
        explanation = "Nemo ist der kleine Clownfisch, nach dem gesucht wird. Sein Vater Marlin begibt sich auf die abenteuerliche Suche nach ihm, nachdem Nemo von einem Taucher gefangen wurde.",
        difficulty = 1,
        funFact = "Findet Nemo war bei seiner Veroeffentlichung im Jahr 2003 der bis dahin erfolgreichste Animationsfilm aller Zeiten."
    ),

    // Question 8
    Question(
        categoryId = 4,
        questionText = "In welchem Land spielt die Serie 'Breaking Bad'?",
        answerA = "Mexiko",
        answerB = "Texas",
        answerC = "New Mexico",
        answerD = "Arizona",
        correctAnswer = 2, // C
        explanation = "Breaking Bad spielt hauptsaechlich in Albuquerque, New Mexico. Die Wuestenlandschaft der Region spielt eine wichtige Rolle in der Serie.",
        difficulty = 1,
        funFact = "Der Name 'Breaking Bad' ist ein suedstaatlicher Slangausdruck fuer das Einschlagen eines schlechten Weges."
    ),

    // Question 9
    Question(
        categoryId = 4,
        questionText = "In welcher Stadt spielt die Sitcom 'Friends'?",
        answerA = "Los Angeles",
        answerB = "Chicago",
        answerC = "Boston",
        answerD = "New York",
        correctAnswer = 3, // D
        explanation = "Friends spielt in New York City. Die sechs Freunde treffen sich regelmaessig im Central Perk Cafe und wohnen in zwei Wohnungen im selben Gebaeude.",
        difficulty = 1,
        funFact = "Das Appartementgebaeude in Friends befindet sich tatsaechlich in New York, wurde aber hauptsaechlich in einem Studio in Los Angeles gedreht."
    ),

    // Question 10
    Question(
        categoryId = 4,
        questionText = "Wer spielt den Hobbit Frodo Beutlin in 'Der Herr der Ringe'?",
        answerA = "Ian McKellen",
        answerB = "Viggo Mortensen",
        answerC = "Elijah Wood",
        answerD = "Sean Astin",
        correctAnswer = 2, // C
        explanation = "Elijah Wood verkoepert den Hobbit Frodo Beutlin in der Herr-der-Ringe-Trilogie von Peter Jackson. Frodo ist der Ringtraeger, der den Einen Ring in den Schicksalsberg bringen muss.",
        difficulty = 1,
        funFact = "Elijah Wood war erst 20 Jahre alt, als er die Rolle des Frodo in der Trilogie uebernahm."
    ),

    // Question 11
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler ist bekannt fuer Filme wie 'Forrest Gump', 'Cast Away' und 'Philadelphia'?",
        answerA = "Tom Hanks",
        answerB = "Tom Cruise",
        answerC = "Jack Nicholson",
        answerD = "Dustin Hoffman",
        correctAnswer = 0, // A
        explanation = "Tom Hanks ist einer der erfolgreichsten Schauspieler Hollywoods. Er gewann fuer 'Philadelphia' und 'Forrest Gump' zwei Oscars in Folge als Bester Hauptdarsteller.",
        difficulty = 1,
        funFact = "Tom Hanks ist erst die zweite Person in der Geschichte der Oscars, die zwei Jahre hintereinander als Bester Hauptdarsteller ausgezeichnet wurde."
    ),

    // Question 12
    Question(
        categoryId = 4,
        questionText = "Welche Farbe hat Supermans Umhang?",
        answerA = "Blau",
        answerB = "Gruen",
        answerC = "Schwarz",
        answerD = "Rot",
        correctAnswer = 3, // D
        explanation = "Supermans Umhang ist rot, sein Anzug blau und das 'S' auf seiner Brust gelb. Diese Farben sind seit der Einfuehrung der Figur im Jahr 1938 ikonisch.",
        difficulty = 1,
        funFact = "Supermans Kost uem wurde fuer die Filmadaptionen mehrfach leicht veraendert, die Grundfarben blieben jedoch stets gleich."
    ),

    // Question 13
    Question(
        categoryId = 4,
        questionText = "Wie heisst die Cowboy-Figur in 'Toy Story'?",
        answerA = "Rex",
        answerB = "Buzz",
        answerC = "Woody",
        answerD = "Hamm",
        correctAnswer = 2, // C
        explanation = "Woody ist der Cowboy und das Lieblingsspielzeug von Andy in Toy Story. Er wird im englischen Original von Tom Hanks gesprochen.",
        difficulty = 1,
        funFact = "Toy Story aus dem Jahr 1995 war der erste vollstaendig computergenerierte Spielfilm der Kinogeschichte."
    ),

    // Question 14
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler gewann fuer 'The Revenant' seinen ersten Oscar?",
        answerA = "Brad Pitt",
        answerB = "Leonardo DiCaprio",
        answerC = "Johnny Depp",
        answerD = "Matt Damon",
        correctAnswer = 1, // B
        explanation = "Leonardo DiCaprio gewann 2016 nach mehreren Nominierungen endlich seinen ersten Oscar als Bester Hauptdarsteller fuer seine Rolle in 'The Revenant'.",
        difficulty = 1,
        funFact = "DiCaprio wartete ueber 20 Jahre auf seinen ersten Oscar. Seine erste Nominierung erhielt er 1994 fuer 'Gilbert Grape'."
    ),

    // Question 15
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr erschien der erste Harry-Potter-Film?",
        answerA = "1999",
        answerB = "2000",
        answerC = "2001",
        answerD = "2002",
        correctAnswer = 2, // C
        explanation = "Harry Potter und der Stein der Weisen erschien im Jahr 2001. Der Film basiert auf dem gleichnamigen Roman von Joanne K. Rowling aus dem Jahr 1997.",
        difficulty = 1,
        funFact = "Der erste Harry-Potter-Film spielte weltweit ueber 974 Millionen US-Dollar ein und war damit einer der erfolgreichsten Filme des Jahres 2001."
    ),

    // Question 16
    Question(
        categoryId = 4,
        questionText = "Wie viele Oscars gewann der Film 'Titanic'?",
        answerA = "7",
        answerB = "9",
        answerC = "11",
        answerD = "13",
        correctAnswer = 2, // C
        explanation = "Titanic gewann 1998 bei der Oscar-Verleihung elf Oscars und stellte damit den Rekord von 'Ben-Hur' ein. Er war fuer 14 Kategorien nominiert.",
        difficulty = 1,
        funFact = "Den Rekord von elf Oscars teilte Titanic jahrelang mit 'Ben-Hur', bis 'Return of the King' ebenfalls elf Oscars gewann."
    ),

    // Question 17
    Question(
        categoryId = 4,
        questionText = "Welches Tier ist das Markenzeichen von Pixar?",
        answerA = "Ein Hase",
        answerB = "Eine Ente",
        answerC = "Ein Hund",
        answerD = "Eine Lampe",
        correctAnswer = 3, // D
        explanation = "Luxo Jr., die huepfende Schreibtischlampe, ist das Maskottchen von Pixar Animation Studios und erscheint in jedem Pixar-Film am Anfang.",
        difficulty = 1,
        funFact = "Luxo Jr. war 1986 der erste vollstaendig computergenerierte Kurzfilm von Pixar und erhielt eine Oscar-Nominierung."
    ),

    // Question 18
    Question(
        categoryId = 4,
        questionText = "Welche deutsche Krimiserie laeuft seit 1970 im Fernsehen?",
        answerA = "Derrick",
        answerB = "Tatort",
        answerC = "Der Alte",
        answerD = "Kommissar Rex",
        correctAnswer = 1, // B
        explanation = "Tatort ist eine der laengstlaufenden deutschen Krimiserien und wird seit 1970 im ARD ausgestrahlt. Verschiedene Ermittlerteams aus unterschiedlichen Staedten ermitteln in den Faellen.",
        difficulty = 1,
        funFact = "Tatort ist mit ueber 1.200 Episoden eine der laengstlaufenden Krimiserien der Welt und wird bis heute produziert."
    ),

    // Question 19
    Question(
        categoryId = 4,
        questionText = "Welcher Film von 1981 zeigt deutsche U-Boot-Soldaten im Zweiten Weltkrieg?",
        answerA = "Stalingrad",
        answerB = "Das Boot",
        answerC = "Der Untergang",
        answerD = "Unsere Muetter, unsere Vaeter",
        correctAnswer = 1, // B
        explanation = "Das Boot von Regisseur Wolfgang Petersen aus dem Jahr 1981 gilt als einer der besten deutschen Filme aller Zeiten und zeigt das Leben an Bord eines deutschen U-Boots im Zweiten Weltkrieg.",
        difficulty = 1,
        funFact = "Das Boot wurde nach seinem deutschen Kinostart auch international ein grosser Erfolg und war fuer sechs Oscars nominiert."
    ),

    // Question 20
    Question(
        categoryId = 4,
        questionText = "Wie heisst der Zauberstab-Lehrer in Harry Potter?",
        answerA = "Professor Snape",
        answerB = "Professor Quirrell",
        answerC = "Professor Flitwick",
        answerD = "Professor Dumbledore",
        correctAnswer = 2, // C
        explanation = "Professor Filius Flitwick unterrichtet in Hogwarts das Fach Zaubersprueche. Er ist ein halbbluetig er Zauberer und Experte fuer Charms.",
        difficulty = 1,
        funFact = "Professor Flitwick wird in den Filmen von Schauspieler Warwick Davis dargestellt, der durch seine Koerpergroesse bekannt ist."
    ),

    // Question 21
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt den Joker in 'The Dark Knight' (2008)?",
        answerA = "Jack Nicholson",
        answerB = "Jared Leto",
        answerC = "Joaquin Phoenix",
        answerD = "Heath Ledger",
        correctAnswer = 3, // D
        explanation = "Heath Ledger spielte den Joker in The Dark Knight und gewann posthum den Oscar fuer die Beste Nebenrolle. Seine Darstellung gilt als eine der besten Schurkenrollen der Filmgeschichte.",
        difficulty = 1,
        funFact = "Heath Ledger bereitete sich monatelang auf die Rolle vor und isolierte sich in einem Hotelzimmer, um den Charakter zu entwickeln."
    ),

    // Question 22
    Question(
        categoryId = 4,
        questionText = "In welchem Disney-Film singt 'Lass jetzt los' (Let It Go)?",
        answerA = "Moana",
        answerB = "Frozen",
        answerC = "Brave",
        answerD = "Tangled",
        correctAnswer = 1, // B
        explanation = "Der Song 'Lass jetzt los' (englisch: Let It Go) stammt aus dem Disney-Film Frozen aus dem Jahr 2013 und wird von der Eiskoenigin Elsa gesungen.",
        difficulty = 1,
        funFact = "Let It Go wurde in 42 Sprachen aufgenommen und gewann den Oscar fuer den Besten Originalsong."
    ),

    // Question 23
    Question(
        categoryId = 4,
        questionText = "Wie viele Teile hat die urspruengliche Star-Wars-Trilogie?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "6",
        correctAnswer = 1, // B
        explanation = "Die originale Star-Wars-Trilogie besteht aus drei Filmen: Eine neue Hoffnung (1977), Das Imperium schlaegt zurueck (1980) und Die Rueckkehr der Jedi-Ritter (1983).",
        difficulty = 1,
        funFact = "George Lucas plante urspruenglich neun Episoden fuer Star Wars, doch schliesslich entstanden in der Hauptreihe neun Filme."
    ),

    // Question 24
    Question(
        categoryId = 4,
        questionText = "Welche Schauspielerin ist bekannt fuer 'Meryl Streep'?",
        answerA = "Sie hat noch nie einen Oscar gewonnen",
        answerB = "Sie ist die am haeufigsten fuer den Oscar nominierte Schauspielerin",
        answerC = "Sie spielte hauptsaechlich Actionrollen",
        answerD = "Sie begann ihre Karriere als Saengerin",
        correctAnswer = 1, // B
        explanation = "Meryl Streep ist die am haeufigsten fuer den Oscar nominierte Schauspielerin der Geschichte. Sie erhielt 21 Nominierungen und gewann drei Oscars.",
        difficulty = 1,
        funFact = "Meryl Streep gewann ihren dritten Oscar fuer die Darstellung der britischen Premierministerin Margaret Thatcher in 'Die Eiserne Lady'."
    ),

    // Question 25
    Question(
        categoryId = 4,
        questionText = "In welchem Pixar-Film geht es um Spielzeug, das lebendig wird?",
        answerA = "Monster AG",
        answerB = "Toy Story",
        answerC = "A Bug's Life",
        answerD = "Cars",
        correctAnswer = 1, // B
        explanation = "Toy Story aus dem Jahr 1995 war der erste Pixar-Spielfilm und erzaehlt die Geschichte von Spielzeugfiguren, die lebendig werden, wenn keine Menschen in der Naehe sind.",
        difficulty = 1,
        funFact = "Toy Story war der erste vollstaendig computergenerierte Langfilm der Kinogeschichte und revolutionierte die Animationsbranche."
    ),

    // Question 26
    Question(
        categoryId = 4,
        questionText = "Welche Figur sagt in Star Wars: 'Ich bin dein Vater'?",
        answerA = "Obi-Wan Kenobi",
        answerB = "Emperor Palpatine",
        answerC = "Darth Vader",
        answerD = "Yoda",
        correctAnswer = 2, // C
        explanation = "Darth Vader verraet Luke Skywalker in 'Das Imperium schlaegt zurueck', dass er sein Vater ist. Diese Szene gehoert zu den bekanntesten der Filmgeschichte.",
        difficulty = 1,
        funFact = "Diese Enthuellung ist einer der meistzitierten Filmmomente aller Zeiten, obwohl der genaue Wortlaut oft falsch erinnert wird."
    ),

    // Question 27
    Question(
        categoryId = 4,
        questionText = "Wer ist Batman in Christopher Nolans 'The Dark Knight'?",
        answerA = "Michael Keaton",
        answerB = "Ben Affleck",
        answerC = "Christian Bale",
        answerD = "George Clooney",
        correctAnswer = 2, // C
        explanation = "Christian Bale spielte Bruce Wayne alias Batman in Christopher Nolans Dark-Knight-Trilogie, bestehend aus Batman Begins (2005), The Dark Knight (2008) und The Dark Knight Rises (2012).",
        difficulty = 1,
        funFact = "Fuer seine Rolle als Batman legte Christian Bale stark an Muskelmasse zu, nachdem er sich fuer 'The Machinist' extrem abgemagert hatte."
    ),

    // Question 28
    Question(
        categoryId = 4,
        questionText = "In welchem Film lebt ein Roboter namens WALL-E alleine auf der Erde?",
        answerA = "Robots",
        answerB = "WALL-E",
        answerC = "Megamind",
        answerD = "Big Hero 6",
        correctAnswer = 1, // B
        explanation = "WALL-E ist ein Pixar-Film aus dem Jahr 2008, in dem ein kleiner Muell-Roboter als letztes Wesen die verlassene Erde bewohnt und sich in den Roboter EVE verliebt.",
        difficulty = 1,
        funFact = "WALL-E hat kaum Dialog, erzaehlt aber eine tiefgruendige Geschichte und gewann den Oscar fuer den Besten Animationsfilm."
    ),

    // Question 29
    Question(
        categoryId = 4,
        questionText = "Welche Serie handelt von einem Chemie lehrer, der Drogen herstellt?",
        answerA = "Dexter",
        answerB = "Breaking Bad",
        answerC = "Narcos",
        answerD = "Ozark",
        correctAnswer = 1, // B
        explanation = "In Breaking Bad verwandelt sich der krebskranke Chemielehrer Walter White in einen gefaehrlichen Drogenkoch. Die Serie gilt als eine der besten in der TV-Geschichte.",
        difficulty = 1,
        funFact = "Breaking Bad gewann 16 Primetime Emmy Awards, darunter Outstanding Drama Series fuer die letzten zwei Staffeln."
    ),

    // Question 30
    Question(
        categoryId = 4,
        questionText = "In welchem Land spielt 'Game of Thrones'?",
        answerA = "In einem fiktiven Mittelalterland namens Westeros",
        answerB = "Im mittelalterlichen England",
        answerC = "In einem zukuenftigen Nordamerika",
        answerD = "Im alten Roemischen Reich",
        correctAnswer = 0, // A
        explanation = "Game of Thrones spielt auf dem fiktiven Kontinent Westeros sowie in anderen erfundenen Laendern. Die Welt wurde von Autor George R. R. Martin erschaffen.",
        difficulty = 1,
        funFact = "Fuer Game of Thrones wurden ueber sieben Staffeln mehr als 150 Drehorte in 14 Laendern genutzt."
    ),

    // Question 31
    Question(
        categoryId = 4,
        questionText = "Wie heisst der Schneemann in 'Frozen'?",
        answerA = "Kristoff",
        answerB = "Sven",
        answerC = "Olaf",
        answerD = "Hans",
        correctAnswer = 2, // C
        explanation = "Olaf ist der suesse Schneemann in Frozen, der von Elsa erschaffen wurde. Er traeumt davon, einmal den Sommer zu erleben.",
        difficulty = 1,
        funFact = "Olaf wurde so populaer, dass er eine eigene Kurzfilm-Reihe bei Disney+ erhielt."
    ),

    // Question 32
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt Thor im Marvel Cinematic Universe?",
        answerA = "Chris Evans",
        answerB = "Chris Hemsworth",
        answerC = "Chris Pratt",
        answerD = "Chris Pine",
        correctAnswer = 1, // B
        explanation = "Chris Hemsworth spielt den nordischen Donnergott Thor im Marvel Cinematic Universe. Er erschien erstmals 2011 im gleichnamigen Film.",
        difficulty = 1,
        funFact = "Fuer seine Rolle als Thor musste Chris Hemsworth ein intensives Kraft training absolvieren und legte dabei ueber 20 Kilogramm Muskelmasse zu."
    ),

    // Question 33
    Question(
        categoryId = 4,
        questionText = "Welcher Pixar-Film handelt von einem Kochenden Ratten?",
        answerA = "Ratatouille",
        answerB = "Cars",
        answerC = "Up",
        answerD = "Brave",
        correctAnswer = 0, // A
        explanation = "In Ratatouille (2007) traeumt die Ratte Remy davon, ein grossartiger Koch zu werden, und hilft dem untalentierten Linguini in einem Pariser Luxusrestaurant.",
        difficulty = 1,
        funFact = "Ratatouille gewann den Oscar fuer den Besten Animationsfilm und gilt als einer der kulinarisch praezisesten Filme, die je gemacht wurden."
    ),

    // Question 34
    Question(
        categoryId = 4,
        questionText = "Welche Studio Ghibli Film zeigt Figuren wie Totoro?",
        answerA = "Prinzessin Mononoke",
        answerB = "Spirited Away",
        answerC = "Mein Nachbar Totoro",
        answerD = "Nausicaa",
        correctAnswer = 2, // C
        explanation = "Mein Nachbar Totoro von Hayao Miyazaki aus dem Jahr 1988 zeigt zwei Maedchen, die das Waldgeist-Wesen Totoro kennenlernen. Totoro ist seitdem das Symbol von Studio Ghibli.",
        difficulty = 1,
        funFact = "Totoro ist so ikonisch fuer Studio Ghibli, dass er als Maskottchen auf allen Produkten und dem Studiologos erscheint."
    ),

    // Question 35
    Question(
        categoryId = 4,
        questionText = "Welcher Film von Hayao Miyazaki gewann den Oscar fuer den Besten Animationsfilm?",
        answerA = "Chihiros Reise ins Zauberland",
        answerB = "Prinzessin Mononoke",
        answerC = "Das Schloss im Himmel",
        answerD = "Nausicaa",
        correctAnswer = 0, // A
        explanation = "Chihiros Reise ins Zauberland (Spirited Away) von Hayao Miyazaki gewann 2003 den Oscar fuer den Besten Animationsfilm und ist damit der erste nicht-englischsprachige Animationsfilm, der diesen Preis gewann.",
        difficulty = 1,
        funFact = "Spirited Away ist in Japan der erfolgreichste Film aller Zeiten und uebertraf sogar 'Titanic' an den japanischen Kinokassen."
    ),

    // Question 36
    Question(
        categoryId = 4,
        questionText = "Wer ist der Regisseur von 'Der Herr der Ringe'?",
        answerA = "Steven Spielberg",
        answerB = "James Cameron",
        answerC = "Peter Jackson",
        answerD = "Ridley Scott",
        correctAnswer = 2, // C
        explanation = "Peter Jackson aus Neuseeland inszenierte die Herr-der-Ringe-Trilogie sowie die spaeteren Hobbit-Filme. Er drehte alle drei Teile der urspruenglichen Trilogie gleichzeitig.",
        difficulty = 1,
        funFact = "Peter Jackson drehte alle drei Herr-der-Ringe-Filme gleichzeitig ueber eine Zeitraum von 438 Drehtagen."
    ),

    // Question 37
    Question(
        categoryId = 4,
        questionText = "Welche Nummer hat der geheime Agent James Bond?",
        answerA = "005",
        answerB = "006",
        answerC = "007",
        answerD = "008",
        correctAnswer = 2, // C
        explanation = "James Bond traegt die Codename 007 und arbeitet fuer den britischen Geheimdienst MI6. Die Figur wurde von Ian Fleming erschaffen und erstmals 1962 im Film 'Dr. No' verkoepert.",
        difficulty = 1,
        funFact = "Die Zahl 007 kommt aus dem Buch: '00' bedeutet, dass der Agent das Recht hat zu toeten, die '7' ist seine individuelle Nummer."
    ),

    // Question 38
    Question(
        categoryId = 4,
        questionText = "Welches Tier hilft Simba in 'Der Koenig der Loewen' seine Vergangenheit zu vergessen?",
        answerA = "Timon und Pumbaa (Erdmaennchen und Warzenschwein)",
        answerB = "Nala (Loewenmaedchen)",
        answerC = "Rafiki (Pavian)",
        answerD = "Zazu (Hornvogel)",
        correctAnswer = 0, // A
        explanation = "Timon (Erdmaennchen) und Pumbaa (Warzenschwein) nehmen Simba auf, nachdem er geflohen ist, und lehren ihm ihre 'Hakuna Matata'-Philosophie ohne Sorgen zu leben.",
        difficulty = 1,
        funFact = "Hakuna Matata ist ein echter Swahili-Ausdruck aus Ostafrika und bedeutet in etwa 'keine Probleme' oder 'kein Stress'."
    ),

    // Question 39
    Question(
        categoryId = 4,
        questionText = "In welchem Marvel-Film vereinen sich die Avengers zum ersten Mal?",
        answerA = "Iron Man 2",
        answerB = "The Avengers",
        answerC = "Captain America: Civil War",
        answerD = "Thor: The Dark World",
        correctAnswer = 1, // B
        explanation = "In 'The Avengers' (2012) kommen Iron Man, Captain America, Thor, Hulk, Black Widow und Hawkeye erstmals als Team zusammen, um die Erde vor Loki zu retten.",
        difficulty = 1,
        funFact = "The Avengers spielte in seinem Eroeffnungswochenende ueber 207 Millionen US-Dollar ein und stellte damit damals einen neuen Rekord auf."
    ),

    // Question 40
    Question(
        categoryId = 4,
        questionText = "Welche beruehmte Schauspielerin spielt in 'Kramer gegen Kramer' und 'Sophie's Choice'?",
        answerA = "Cate Blanchett",
        answerB = "Meryl Streep",
        answerC = "Judie Dench",
        answerD = "Helen Mirren",
        correctAnswer = 1, // B
        explanation = "Meryl Streep gewann den Oscar fuer die Beste Hauptdarstellerin sowohl fuer 'Kramer gegen Kramer' (1979) als auch fuer 'Sophie's Choice' (1982) und zeigte damit ihre aussergewoehnliche Vielseitigkeit.",
        difficulty = 1,
        funFact = "Meryl Streep ist bekannt dafuer, sich extrem intensiv auf ihre Rollen vorzubereiten, einschliesslich dem Erlernen von Fremdsprachen und Akzenten."
    ),

    // Question 41
    Question(
        categoryId = 4,
        questionText = "In welchem Film faehrt ein Schulbus in einem Sturm mit DeLorean durch die Zeit?",
        answerA = "Terminator",
        answerB = "Jurassic Park",
        answerC = "Ghostbusters",
        answerD = "Zurueck in die Zukunft",
        correctAnswer = 3, // D
        explanation = "In 'Zurueck in die Zukunft' benutzt der Wissenschaftler Doc Brown einen DeLorean-Sportwagen als Zeitmaschine. Der Film erschien 1985 und wurde ein Klassiker.",
        difficulty = 1,
        funFact = "Der DeLorean DMC-12 aus dem Film wurde speziell ausgesucht, weil seine Fluegeltueren wie etwas aus der Zukunft aussehen."
    ),

    // Question 42
    Question(
        categoryId = 4,
        questionText = "Wie heisst der boeseliche Diktator in 'Der Koenig der Loewen'?",
        answerA = "Banzai",
        answerB = "Scar",
        answerC = "Shenzi",
        answerD = "Ed",
        correctAnswer = 1, // B
        explanation = "Scar ist der hinterhaeltige Onkel von Simba, der seinen eigenen Bruder Mufasa ermordet, um Koenig zu werden. Er ist einer der bekanntesten Disney-Schurken.",
        difficulty = 1,
        funFact = "Scar wurde nach Simba benannt, da seine Narbe sein auffaelligstes Merkmal ist. Im englischen Original wird er von Jeremy Irons gesprochen."
    ),

    // Question 43
    Question(
        categoryId = 4,
        questionText = "In welchem Film sucht der alte Mann Carl mit Luftballons ein Haus in Suedamerika zu erreichen?",
        answerA = "Brave",
        answerB = "Ratatouille",
        answerC = "Up",
        answerD = "Coco",
        correctAnswer = 2, // C
        explanation = "In 'Up' (2009) befestigt der alte Carl Fredricksen Tausende von Luftballons an seinem Haus und fliegt damit nach Suedamerika, um seinen Lebenstraum zu erfuellen.",
        difficulty = 1,
        funFact = "Die ersten zehn Minuten von Up gelten als einer der emotionalsten und handwerklich besten Eroeffnungssequenzen der Filmgeschichte."
    ),

    // Question 44
    Question(
        categoryId = 4,
        questionText = "Welche Serie zeigt den Mittelalter-Kontinent Westeros mit Drachen und politischen Intrigen?",
        answerA = "The Witcher",
        answerB = "Vikings",
        answerC = "The Last Kingdom",
        answerD = "Game of Thrones",
        correctAnswer = 3, // D
        explanation = "Game of Thrones ist eine HBO-Serie, die auf den Romanreihe 'Das Lied von Eis und Feuer' von George R. R. Martin basiert und von Machtkampf, Intrigen und Drachen handelt.",
        difficulty = 1,
        funFact = "Game of Thrones wurde in seiner ersten Staffel mit einem Budget von rund 60 Millionen US-Dollar produziert und avancierte zur teuersten TV-Serie ihrer Zeit."
    ),

    // Question 45
    Question(
        categoryId = 4,
        questionText = "Welcher Film von 1993 behandelt die Rettung juedischer Menschen durch den Unternehmer Oskar Schindler?",
        answerA = "Der Untergang",
        answerB = "Die Pianistin",
        answerC = "Schindlers Liste",
        answerD = "Das Leben ist schoen",
        correctAnswer = 2, // C
        explanation = "Schindlers Liste von Steven Spielberg aus dem Jahr 1993 erzaehlt die wahre Geschichte von Oskar Schindler, der waehrend des Zweiten Weltkriegs mehr als 1.000 Juden das Leben rettete.",
        difficulty = 1,
        funFact = "Schindlers Liste gewann sieben Oscars, darunter Bester Film und Beste Regie, und wurde in Schwarzweiss gedreht, um die historische Epoche zu unterstreichen."
    ),

    // Question 46
    Question(
        categoryId = 4,
        questionText = "Welches Studio hat Filme wie 'Toy Story', 'WALL-E' und 'Up' produziert?",
        answerA = "DreamWorks Animation",
        answerB = "Blue Sky Studios",
        answerC = "Illumination",
        answerD = "Pixar Animation Studios",
        correctAnswer = 3, // D
        explanation = "Pixar Animation Studios, eine Tochtergesellschaft von Disney, hat alle diese Filme produziert. Pixar revolutionierte die Animationsbranche mit ihren bahnbrechenden 3D-Filmen.",
        difficulty = 1,
        funFact = "Pixar wurde 1986 von Steve Jobs von Lucasfilm abgekauft und spaeter 2006 von Disney uebernommen."
    ),

    // Question 47
    Question(
        categoryId = 4,
        questionText = "Wie heisst der Hai in 'Findet Nemo'?",
        answerA = "Jaws",
        answerB = "Gill",
        answerC = "Bruce",
        answerD = "Anchor",
        correctAnswer = 2, // C
        explanation = "Der Weisse Hai in Findet Nemo heisst Bruce. Er ist Mitglied einer Hai-Selbsthilfegruppe, die versucht vegetarisch zu leben und 'Fische sind Freunde, kein Essen'.",
        difficulty = 1,
        funFact = "Bruce ist eine Anspielung auf den mechanischen Hai aus Steven Spielbergs 'Der Weisse Hai' von 1975, der am Set ebenfalls Bruce genannt wurde."
    ),

    // Question 48
    Question(
        categoryId = 4,
        questionText = "Welche deutsche Kinoproduktion von 2004 zeigt die letzten Tage von Adolf Hitler?",
        answerA = "Das Boot",
        answerB = "Stalingrad",
        answerC = "Der Untergang",
        answerD = "Sophie Scholl",
        correctAnswer = 2, // C
        explanation = "Der Untergang von Regisseur Oliver Hirschbiegel zeigt die letzten Tage von Adolf Hitler in seinem Bunker unter Berlin und ist einer der erfolgreichsten deutschen Filme der Nachkriegszeit.",
        difficulty = 1,
        funFact = "Der Untergang wurde fuer den Oscar als Bester fremdsprachiger Film nominiert und war der erste deutsche Film, der Hitler direkt als Hauptfigur zeigte."
    ),

    // Question 49
    Question(
        categoryId = 4,
        questionText = "Welcher Disney-Prinzessin gehoert der Glasschuh?",
        answerA = "Schneewittchen",
        answerB = "Dornroeschen",
        answerC = "Cinderella",
        answerD = "Belle",
        correctAnswer = 2, // C
        explanation = "Der Glasschuh gehoert Cinderella, die nach dem Ball fliehen muss und einen ihrer Glasschuhe verliert. Prinz Charming sucht mit dem Schuh nach ihr.",
        difficulty = 1,
        funFact = "In der urspruenglichen Maerchenversion von Charles Perrault aus dem Jahr 1697 waren die Schuhe tatsaechlich aus Glas - was in frueheren Versionen oft als 'vair' (Pelz) missverstanden wurde."
    ),

    // Question 50
    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt Captain Jack Sparrow in 'Fluch der Karibik'?",
        answerA = "Orlando Bloom",
        answerB = "Geoffrey Rush",
        answerC = "Johnny Depp",
        answerD = "Javier Bardem",
        correctAnswer = 2, // C
        explanation = "Johnny Depp spielt den exzentrischen Pirat Captain Jack Sparrow in der Fluch-der-Karibik-Reihe. Seine einzigartige Darstellung machte die Figur zu einer Kultfigur.",
        difficulty = 1,
        funFact = "Johnny Depp basierte seine Darstellung von Jack Sparrow auf dem Rolling-Stones-Gitarristen Keith Richards und dem Cartoon-Charakter Pepe Le Pew."
    ),

)
