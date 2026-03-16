package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestions(): List<Question> = listOf(

    // ─── EASY (10 questions) ───────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher Film beginnt mit dem Satz: 'Es war einmal in einer weit, weit entfernten Galaxis...'?",
        answerA = "Star Wars",
        answerB = "Star Trek",
        answerC = "Interstellar",
        answerD = "Avatar",
        correctAnswer = 0,
        explanation = "Der berühmte Eröffnungstext 'A long time ago in a galaxy far, far away....' ist das Markenzeichen der Star-Wars-Filmreihe von George Lucas.",
        difficulty = 1,
        funFact = "Der erste Star-Wars-Film erschien 1977 und revolutionierte die Spezialeffekte im Kino."
    ),

    Question(
        categoryId = 4,
        questionText = "Wer spielte Jack Dawson im Film 'Titanic' (1997)?",
        answerA = "Brad Pitt",
        answerB = "Tom Hanks",
        answerC = "Leonardo DiCaprio",
        answerD = "Johnny Depp",
        correctAnswer = 2,
        explanation = "Leonardo DiCaprio spielte den mittellosen Künstler Jack Dawson, der sich auf dem Schiff in Rose verliebt.",
        difficulty = 1,
        funFact = "Titanic war der erste Film, der weltweit über eine Milliarde Dollar einspielte."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Film sagt die Figur Forrest Gump: 'Das Leben ist wie eine Schachtel Pralinen'?",
        answerA = "Cast Away",
        answerB = "Forrest Gump",
        answerC = "Philadelphia",
        answerD = "Big",
        correctAnswer = 1,
        explanation = "In 'Forrest Gump' (1994) sagt Forrest diesen berühmten Satz, den er von seiner Mutter gelernt hat.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Tier ist der Hauptcharakter im Animationsfilm 'Findet Nemo' (2003)?",
        answerA = "Delfin",
        answerB = "Schildkröte",
        answerC = "Clownfisch",
        answerD = "Blauer Streifendoktorfisch",
        correctAnswer = 2,
        explanation = "Nemo ist ein Clownfisch. Sein Vater Marlin, ebenfalls ein Clownfisch, sucht ihn im gesamten Ozean.",
        difficulty = 1,
        funFact = "Nach dem Kinostart von 'Findet Nemo' stiegen die Verkäufe von Clownfischen in Tierhandlungen um 40 %."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielte den Joker in 'The Dark Knight' (2008)?",
        answerA = "Jared Leto",
        answerB = "Joaquin Phoenix",
        answerC = "Jack Nicholson",
        answerD = "Heath Ledger",
        correctAnswer = 3,
        explanation = "Heath Ledger verkörperte den Joker in Christopher Nolans 'The Dark Knight' und gewann dafür posthum einen Oscar.",
        difficulty = 1,
        funFact = "Heath Ledger starb kurz nach den Dreharbeiten und wurde posthum mit dem Oscar für die beste Nebenrolle ausgezeichnet."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie heißt die Hauptfigur in der deutschen Serie 'Dark'?",
        answerA = "Jonas Kahnwald",
        answerB = "Ulrich Nielsen",
        answerC = "Peter Doppler",
        answerD = "Bartosz Tiedemann",
        correctAnswer = 0,
        explanation = "Jonas Kahnwald ist der jugendliche Protagonist der Netflix-Serie 'Dark', die in Winden spielt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film gewann 1998 den Oscar für den besten Film?",
        answerA = "Good Will Hunting",
        answerB = "L.A. Confidential",
        answerC = "Titanic",
        answerD = "As Good as It Gets",
        correctAnswer = 2,
        explanation = "'Titanic' von James Cameron gewann 1998 elf Oscars, darunter Bester Film und Beste Regie.",
        difficulty = 1,
        funFact = "Titanic und 'Ben-Hur' teilen mit je elf Oscars den Rekord für die meisten Auszeichnungen eines einzelnen Films."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Figur sagt in 'Der Herr der Ringe': 'Mein Schatz!'?",
        answerA = "Frodo",
        answerB = "Sauron",
        answerC = "Gollum",
        answerD = "Gandalf",
        correctAnswer = 2,
        explanation = "Gollum, das vom Ring besessene Wesen, nennt den einen Ring immer 'mein Schatz' (engl. 'My precious').",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Wer ist der Regisseur des Films 'Jurassic Park' (1993)?",
        answerA = "George Lucas",
        answerB = "James Cameron",
        answerC = "Steven Spielberg",
        answerD = "Tim Burton",
        correctAnswer = 2,
        explanation = "Steven Spielberg inszenierte 'Jurassic Park', basierend auf dem Roman von Michael Crichton.",
        difficulty = 1,
        funFact = "Die Dinosaurier in 'Jurassic Park' wurden durch eine revolutionäre Kombination aus CGI und Animatronics zum Leben erweckt."
    ),

    Question(
        categoryId = 4,
        questionText = "In welcher Stadt spielt die Serie 'Stranger Things'?",
        answerA = "Springfield",
        answerB = "Hawkins",
        answerC = "Riverdale",
        answerD = "Sunnydale",
        correctAnswer = 1,
        explanation = "'Stranger Things' spielt in der fiktiven Kleinstadt Hawkins im Bundesstaat Indiana, USA.",
        difficulty = 1,
        funFact = null
    ),

    // ─── MEDIUM (15 questions) ────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher deutscher Film gewann als erster den Oscar für den besten fremdsprachigen Film?",
        answerA = "Das Boot",
        answerB = "Nirgendwo in Afrika",
        answerC = "Das Leben der Anderen",
        answerD = "Die Blechtrommel",
        correctAnswer = 3,
        explanation = "'Die Blechtrommel' von Volker Schlöndorff gewann 1980 als erster westdeutscher Film den Oscar für den besten fremdsprachigen Film.",
        difficulty = 2,
        funFact = "Die Blechtrommel teilte sich den Oscar in jenem Jahr mit dem kubanischen Film 'Almendros'."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur ist für die Trilogie 'Der Pate' verantwortlich?",
        answerA = "Martin Scorsese",
        answerB = "Francis Ford Coppola",
        answerC = "Brian De Palma",
        answerD = "Sidney Lumet",
        correctAnswer = 1,
        explanation = "Francis Ford Coppola inszenierte alle drei Teile der 'Pate'-Trilogie (1972, 1974, 1990).",
        difficulty = 2,
        funFact = "Marlon Brando lehnte seinen Oscar für 'Der Pate' als Protest gegen die Behandlung der Ureinwohner Amerikas ab."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Christopher Nolan behandelt das Thema des gemeinsamen Träumens?",
        answerA = "Memento",
        answerB = "Interstellar",
        answerC = "Inception",
        answerD = "The Prestige",
        correctAnswer = 2,
        explanation = "'Inception' (2010) handelt von einer Gruppe, die in die Träume anderer Menschen eindringt, um Ideen zu stehlen oder zu pflanzen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Jahr begann die HBO-Serie 'Game of Thrones'?",
        answerA = "2009",
        answerB = "2010",
        answerC = "2011",
        answerD = "2012",
        correctAnswer = 2,
        explanation = "'Game of Thrones' wurde erstmals am 17. April 2011 auf HBO ausgestrahlt und basiert auf der Buchreihe 'Das Lied von Eis und Feuer' von George R. R. Martin.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Schauspielerin gewann für 'Monster's Ball' (2001) den Oscar als beste Hauptdarstellerin?",
        answerA = "Angela Bassett",
        answerB = "Halle Berry",
        answerC = "Viola Davis",
        answerD = "Taraji P. Henson",
        correctAnswer = 1,
        explanation = "Halle Berry gewann 2002 als erste afroamerikanische Schauspielerin den Oscar für die beste Hauptdarstellerin.",
        difficulty = 2,
        funFact = "Halle Berrys Oscar-Sieg war ein historischer Meilenstein – bis heute blieb sie die einzige afroamerikanische Frau, die diesen Preis gewann."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Animationsfilm von Studio Ghibli handelt von einem Mädchen, das in einer Geisterherberge arbeitet?",
        answerA = "Mein Nachbar Totoro",
        answerB = "Prinzessin Mononoke",
        answerC = "Chihiros Reise ins Zauberland",
        answerD = "Das Schloss im Himmel",
        correctAnswer = 2,
        explanation = "'Chihiros Reise ins Zauberland' (2001) von Hayao Miyazaki folgt dem Mädchen Chihiro, das in einer Geisterwelt für eine Hexe arbeiten muss.",
        difficulty = 2,
        funFact = "Der Film gewann 2003 den Oscar für den besten Animationsfilm und ist der erfolgreichste japanische Film aller Zeiten."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie viele Staffeln hat die US-amerikanische Serie 'Breaking Bad'?",
        answerA = "4",
        answerB = "5",
        answerC = "6",
        answerD = "7",
        correctAnswer = 1,
        explanation = "'Breaking Bad' lief von 2008 bis 2013 mit insgesamt 5 Staffeln und 62 Episoden auf AMC.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt Tony Stark / Iron Man im Marvel Cinematic Universe?",
        answerA = "Chris Evans",
        answerB = "Chris Hemsworth",
        answerC = "Robert Downey Jr.",
        answerD = "Mark Ruffalo",
        correctAnswer = 2,
        explanation = "Robert Downey Jr. verkörperte Tony Stark in insgesamt zehn MCU-Filmen, beginnend mit 'Iron Man' (2008).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Quentin Tarantino spielt hauptsächlich in einem Diner und einem Apartment in Los Angeles?",
        answerA = "Jackie Brown",
        answerB = "Reservoir Dogs",
        answerC = "Kill Bill",
        answerD = "Pulp Fiction",
        correctAnswer = 3,
        explanation = "'Pulp Fiction' (1994) erzählt mehrere miteinander verwobene Geschichten aus der Unterwelt von Los Angeles in nicht-linearer Erzählweise.",
        difficulty = 2,
        funFact = "Das Drehbuch zu 'Pulp Fiction' wurde 2013 für fünf Millionen Dollar versteigert."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film gewann 2020 als erster nicht-englischsprachiger Film den Oscar für den besten Film?",
        answerA = "Roma",
        answerB = "Parasite",
        answerC = "Das Leben der Anderen",
        answerD = "Amour",
        correctAnswer = 1,
        explanation = "'Parasite' von Bong Joon-ho (Südkorea) gewann 2020 vier Oscars, darunter Bester Film – als erster nicht-englischsprachiger Film in der Oscar-Geschichte.",
        difficulty = 2,
        funFact = "Regisseur Bong Joon-ho nannte den Golden Globe für fremdsprachige Filme einst spöttisch 'imperialistisch'."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche deutsche TV-Serie von 2017 erzählt eine Zeitreisegeschichte in einem fiktiven deutschen Dorf?",
        answerA = "Babylon Berlin",
        answerB = "Dark",
        answerC = "4 Blocks",
        answerD = "Tatort",
        correctAnswer = 1,
        explanation = "'Dark' (Netflix, 2017–2020) ist eine deutsche Science-Fiction-Mysteryserie über Zeitreisen in vier Zeitebenen im Dorf Winden.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielte Hannibal Lecter in 'Das Schweigen der Lämmer' (1991)?",
        answerA = "Gary Oldman",
        answerB = "Anthony Hopkins",
        answerC = "Brian Cox",
        answerD = "Mads Mikkelsen",
        correctAnswer = 1,
        explanation = "Anthony Hopkins spielte den genialen Kannibalen Hannibal Lecter und gewann dafür den Oscar als bester Hauptdarsteller.",
        difficulty = 2,
        funFact = "Anthony Hopkins' Bildschirmzeit als Hannibal Lecter beträgt nur 16 Minuten – trotzdem gewann er den Oscar für die Hauptrolle."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur drehte die Filme 'Schindlers Liste' und 'E.T.'?",
        answerA = "Stanley Kubrick",
        answerB = "Martin Scorsese",
        answerC = "Steven Spielberg",
        answerD = "Ridley Scott",
        correctAnswer = 2,
        explanation = "Steven Spielberg ist einer der einflussreichsten Regisseure Hollywoods und schuf sowohl Blockbuster als auch ernste Dramen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Land spielt der Film 'Amélie' (2001)?",
        answerA = "Belgien",
        answerB = "Schweiz",
        answerC = "Frankreich",
        answerD = "Italien",
        correctAnswer = 2,
        explanation = "'Die fabelhafte Welt der Amélie' spielt in Paris, Frankreich, und wurde von Jean-Pierre Jeunet inszeniert.",
        difficulty = 2,
        funFact = "Für die Rolle der Amélie wurde ursprünglich Emily Watson vorgesehen, bevor Audrey Tautou besetzt wurde."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt Walter White in 'Breaking Bad'?",
        answerA = "Bryan Cranston",
        answerB = "Aaron Paul",
        answerC = "Bob Odenkirk",
        answerD = "Dean Norris",
        correctAnswer = 0,
        explanation = "Bryan Cranston spielt den Chemielehrer Walter White, der zum Drogenproduzenten wird. Er gewann für die Rolle viermal den Emmy.",
        difficulty = 2,
        funFact = null
    ),

    // ─── HARD (12 questions) ──────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher deutsche Film gewann 2007 den Oscar für den besten fremdsprachigen Film?",
        answerA = "Nirgendwo in Afrika",
        answerB = "Sophie Scholl",
        answerC = "Das Leben der Anderen",
        answerD = "Der Untergang",
        correctAnswer = 2,
        explanation = "'Das Leben der Anderen' von Florian Henckel von Donnersmarck über die Stasi in der DDR gewann 2007 den Oscar für den besten fremdsprachigen Film.",
        difficulty = 3,
        funFact = "Regisseur Florian Henckel von Donnersmarck war bei der Oscar-Verleihung erst 33 Jahre alt."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie heißt der Regisseur, der für die Filme '2001: Odyssee im Weltraum' und 'A Clockwork Orange' bekannt ist?",
        answerA = "Orson Welles",
        answerB = "Alfred Hitchcock",
        answerC = "Billy Wilder",
        answerD = "Stanley Kubrick",
        correctAnswer = 3,
        explanation = "Stanley Kubrick gilt als einer der größten Filmregisseure aller Zeiten. Er war bekannt für seine akribische Perfektionismus und visuellen Stil.",
        difficulty = 3,
        funFact = "Kubrick drehte 'The Shining' und ließ eine Szene angeblich 127 Mal wiederholen."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Werner Herzog aus dem Jahr 1972 zeigt einen Mann, der einen Dampfer über einen Berg schleppt?",
        answerA = "Aguirre, der Zorn Gottes",
        answerB = "Fitzcarraldo",
        answerC = "Nosferatu – Phantom der Nacht",
        answerD = "Woyzeck",
        correctAnswer = 1,
        explanation = "In 'Fitzcarraldo' (1982) – nicht 1972 – lässt Herzog tatsächlich ein echtes Schiff über einen Hügel ziehen. Der Film von 1972 war 'Aguirre'.",
        difficulty = 3,
        funFact = "Werner Herzog ließ für 'Fitzcarraldo' wirklich ein 340-Tonnen-Schiff über einen Hügel ziehen – ohne CGI."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Schauspielerin gewann den Oscar für die Hauptrolle in 'Sophies Entscheidung' (1982)?",
        answerA = "Meryl Streep",
        answerB = "Jessica Lange",
        answerC = "Sigourney Weaver",
        answerD = "Jane Fonda",
        correctAnswer = 0,
        explanation = "Meryl Streep gewann für ihre Darstellung der polnischen Auschwitz-Überlebenden Sophie Zawistowski den Oscar als beste Hauptdarstellerin.",
        difficulty = 3,
        funFact = "Meryl Streep lernte für die Rolle fließend Polnisch und Deutsch."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Kubrick-Film spielt Jack Nicholson einen Schriftsteller, der in einem Berghotel den Verstand verliert?",
        answerA = "The Shining",
        answerB = "Eyes Wide Shut",
        answerC = "Full Metal Jacket",
        answerD = "Barry Lyndon",
        correctAnswer = 0,
        explanation = "'The Shining' (1980) basiert auf dem Roman von Stephen King. Jack Nicholson spielt Jack Torrance, der als Winterwächter des Overlook Hotels zum Mörder wird.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Wer komponierte die Filmmusik für 'Schindlers Liste' (1993)?",
        answerA = "Hans Zimmer",
        answerB = "Ennio Morricone",
        answerC = "John Williams",
        answerD = "Bernard Herrmann",
        correctAnswer = 2,
        explanation = "John Williams komponierte die emotionale Filmmusik für Spielbergs 'Schindlers Liste' und gewann dafür den Oscar für die beste Filmmusik.",
        difficulty = 3,
        funFact = "John Williams und Steven Spielberg arbeiten seit 'Sugarland Express' (1974) zusammen – eine der längsten Regisseur-Komponisten-Partnerschaften der Filmgeschichte."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur ist bekannt für die Neuen-Hollywood-Bewegung und schuf 'Apocalypse Now'?",
        answerA = "Michael Cimino",
        answerB = "Brian De Palma",
        answerC = "Francis Ford Coppola",
        answerD = "Peter Bogdanovich",
        correctAnswer = 2,
        explanation = "'Apocalypse Now' (1979) von Francis Ford Coppola gilt als eines der bedeutendsten Antikriegsfilme und war für das Neue Hollywood prägend.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Rainer Werner Fassbinder handelt von einer deutschen Witwe, die sich in einen Gastarbeiter verliebt?",
        answerA = "Die Ehe der Maria Braun",
        answerB = "Angst essen Seele auf",
        answerC = "Effi Briest",
        answerD = "Die bitteren Tränen der Petra von Kant",
        correctAnswer = 1,
        explanation = "'Angst essen Seele auf' (1974) erzählt die Geschichte der älteren Emmi und des marokkanischen Gastarbeiters Ali – ein Meisterwerk des deutschen Kinos.",
        difficulty = 3,
        funFact = "Fassbinder drehte den Film in nur zehn Tagen."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Jahr erschien der erste Teil der 'Alien'-Filmreihe?",
        answerA = "1976",
        answerB = "1977",
        answerC = "1979",
        answerD = "1981",
        correctAnswer = 2,
        explanation = "'Alien' von Ridley Scott erschien 1979 und gilt als eines der besten Science-Fiction-Horrorfilme aller Zeiten.",
        difficulty = 3,
        funFact = "Die Szene, in der das Alien aus dem Bauch von Kane ausbricht, war für die meisten Schauspieler eine echte Überraschung."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielte die Hauptrolle in Wim Wenders' 'Paris, Texas' (1984)?",
        answerA = "Dennis Hopper",
        answerB = "Harvey Keitel",
        answerC = "Harry Dean Stanton",
        answerD = "Sam Shepard",
        correctAnswer = 2,
        explanation = "Harry Dean Stanton spielt Travis Henderson, einen Mann, der nach langer Abwesenheit durch die Wüste wandert und seine Familie sucht.",
        difficulty = 3,
        funFact = "'Paris, Texas' gewann die Goldene Palme in Cannes 1984."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur führte bei 'Schindlers Liste' und 'Der Soldat James Ryan' Regie?",
        answerA = "Oliver Stone",
        answerB = "Clint Eastwood",
        answerC = "Steven Spielberg",
        answerD = "Terrence Malick",
        correctAnswer = 2,
        explanation = "Steven Spielberg inszenierte beide preisgekrönten Kriegs-/Historienfilme. 'Schindlers Liste' (1993) und 'Der Soldat James Ryan' (1998) gelten als Meisterwerke.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Fritz Lang aus dem Jahr 1927 gilt als Meilenstein des expressionistischen deutschen Stummfilms?",
        answerA = "Nosferatu",
        answerB = "Der Blaue Engel",
        answerC = "Metropolis",
        answerD = "M – Eine Stadt sucht einen Mörder",
        correctAnswer = 2,
        explanation = "'Metropolis' (1927) von Fritz Lang ist ein Science-Fiction-Stummfilm, der eine dystopische Zukunft zeigt und die Filmsprache nachhaltig beeinflusst hat.",
        difficulty = 3,
        funFact = "Für lange Zeit galt ein großer Teil von 'Metropolis' als verloren, bis 2008 in Argentinien fast vollständige Kopien gefunden wurden."
    ),

    // ─── EXPERT (8 questions) ─────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur gewann beim Oscar 1998 für 'Titanic' als Bester Regisseur und zitierte dabei seinen eigenen Film auf der Bühne?",
        answerA = "Steven Spielberg",
        answerB = "James Cameron",
        answerC = "Ridley Scott",
        answerD = "Peter Jackson",
        correctAnswer = 1,
        explanation = "James Cameron rief bei seiner Oscar-Rede für 'Titanic' den berühmten Satz: 'I'm the king of the world!' – eine Referenz auf seinen eigenen Film.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Ingmar-Bergman-Film spielt der Tod Schach mit einem Ritter?",
        answerA = "Wilde Erdbeeren",
        answerB = "Persona",
        answerC = "Das siebente Siegel",
        answerD = "Szenen einer Ehe",
        correctAnswer = 2,
        explanation = "'Das siebente Siegel' (1957) zeigt den Ritter Antonius Block, der nach seiner Rückkehr aus dem Kreuzzug mit dem Tod eine Schachpartie spielt.",
        difficulty = 4,
        funFact = "Ingmar Bergman gilt als einer der bedeutendsten Filmemacher aller Zeiten. Das Schachspiel mit dem Tod wurde zu einem der ikonischsten Filmbilder überhaupt."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler gewann für 'Network' (1976) als erster posthum den Oscar für die beste Hauptdarstellung?",
        answerA = "Peter Finch",
        answerB = "Faye Dunaway",
        answerC = "William Holden",
        answerD = "Robert Duvall",
        correctAnswer = 0,
        explanation = "Peter Finch starb im Januar 1977, kurz vor der Oscar-Verleihung, und wurde posthum als bester Hauptdarsteller für seine Rolle als Howard Beale ausgezeichnet.",
        difficulty = 4,
        funFact = "Peter Finch ist bis heute der einzige Schauspieler, der posthum den Oscar für die beste Hauptrolle gewann."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie lautet der bürgerliche Name des deutschen Regisseurs, der als 'Wim Wenders' bekannt ist?",
        answerA = "Ernst Wilhelm Wenders",
        answerB = "Wilhelm Friedrich Wenders",
        answerC = "Hans-Ernst Wenders",
        answerD = "Wilhelm Ernst Wenders",
        correctAnswer = 3,
        explanation = "Wim Wenders wurde als Wilhelm Ernst Wenders am 14. August 1945 in Düsseldorf geboren.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Akira Kurosawa war die Vorlage für das Western-Remake 'Für eine Handvoll Dollar'?",
        answerA = "Rashomon",
        answerB = "Die sieben Samurai",
        answerC = "Yojimbo",
        answerD = "Sanjuro",
        correctAnswer = 2,
        explanation = "Sergio Leones 'Für eine Handvoll Dollar' (1964) mit Clint Eastwood basiert auf Kurosawas 'Yojimbo' (1961). Kurosawa gewann daraufhin einen Prozess.",
        difficulty = 4,
        funFact = "Akira Kurosawa war zunächst verärgert über das Remake, gewann aber später Tantiemen und soll gesagt haben: 'Yojimbo' war eine gute Idee."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Michael Haneke gewann zweimal die Goldene Palme in Cannes (2005 und 2012)?",
        answerA = "Das weiße Band",
        answerB = "Funny Games",
        answerC = "Caché",
        answerD = "Amour",
        correctAnswer = 3,
        explanation = "Michael Haneke gewann die Goldene Palme zunächst mit 'Das weiße Band' (2009) und dann mit 'Amour' (2012).",
        difficulty = 4,
        funFact = "Haneke ist einer von nur fünf Regisseuren, die zweimal die Goldene Palme in Cannes gewonnen haben."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Jahr wurde das erste Technicolor-Verfahren für Spielfilme kommerziell eingesetzt?",
        answerA = "1917",
        answerB = "1922",
        answerC = "1935",
        answerD = "1939",
        correctAnswer = 0,
        explanation = "Das erste Technicolor-Verfahren wurde 1917 mit dem Stummfilm 'The Gulf Between' eingesetzt, obwohl es erst in den 1930ern wirklich populär wurde.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schweizer Filmemacher schuf die experimentellen Essayfilme 'Histoire(s) du cinéma'?",
        answerA = "Alain Tanner",
        answerB = "Jean-Luc Godard",
        answerC = "Claude Goretta",
        answerD = "Fredi M. Murer",
        correctAnswer = 1,
        explanation = "Jean-Luc Godard, geboren in Paris, aber Schweizer Staatsbürger, schuf zwischen 1988 und 1998 die monumentale Filmessay-Reihe 'Histoire(s) du cinéma'.",
        difficulty = 4,
        funFact = "Godard gilt als einer der wichtigsten Vertreter der Nouvelle Vague und revolutionierte die Filmsprache mit Filmen wie 'Außer Atem' (1960)."
    ),

    // ─── MASTER (5 questions) ─────────────────────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Luchino Visconti aus dem Jahr 1963 gilt als einer der längsten je produzierten Mainstream-Filme mit einer Laufzeit von über 4 Stunden?",
        answerA = "Rocco und seine Brüder",
        answerB = "Senso",
        answerC = "Il Gattopardo (Der Leopard)",
        answerD = "Ludwig",
        correctAnswer = 2,
        explanation = "'Il Gattopardo' (Der Leopard, 1963) von Luchino Visconti mit Burt Lancaster hat eine Originallaufzeit von 205 Minuten und gewann die Goldene Palme in Cannes.",
        difficulty = 5,
        funFact = "Die Ballszene in 'Der Leopard' dauert allein 45 Minuten und wurde mit 500 Komparsen gedreht."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Filmtheoretiker prägte den Begriff 'Politique des auteurs' in den Cahiers du Cinéma?",
        answerA = "André Bazin",
        answerB = "François Truffaut",
        answerC = "Siegfried Kracauer",
        answerD = "Sergei Eisenstein",
        correctAnswer = 1,
        explanation = "François Truffaut formulierte 1954 in seinem Aufsatz 'Une certaine tendance du cinéma français' die Grundlagen der Autorentheorie ('Politique des auteurs').",
        difficulty = 5,
        funFact = "Truffaut war zunächst Filmkritiker und Lieblingsschüler von André Bazin, bevor er mit 'Sie küssten und sie schlugen ihn' (1959) Regisseur wurde."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Film von Carl Theodor Dreyer spielte Maria Falconetti die Rolle der Johanna von Orleans ohne professionelle Schauspielerfahrung?",
        answerA = "Vampyr",
        answerB = "Gertrud",
        answerC = "Dies irae",
        answerD = "La Passion de Jeanne d'Arc",
        correctAnswer = 3,
        explanation = "'La Passion de Jeanne d'Arc' (1928) von Carl Th. Dreyer mit Renée Falconetti gilt als einer der größten Stummfilme aller Zeiten. Falconetti wurde von Dreyer entdeckt.",
        difficulty = 5,
        funFact = "Dreyer soll Falconetti für die intensiven Aufnahmen tatsächlich auf dem Steinboden knien lassen und ihr Haare für echte Reaktionen ausgerissen haben."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Robert Bresson aus dem Jahr 1966 basiert auf dem Roman von Fjodor Dostojewski 'Der Idiot' als Vorlage für eine moderne Pariser Adaptation?",
        answerA = "Au hasard Balthazar",
        answerB = "Mouchette",
        answerC = "Pickpocket",
        answerD = "Une femme douce",
        correctAnswer = 3,
        explanation = "'Une femme douce' (1969) basiert auf Dostojewskis Kurzgeschichte 'Die Sanfte'. Bressons Adaption von 'Der Idiot'-Motiven in Paris war 'Vier Nächte einer Träumerin' (1971).",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher japanische Regisseur schuf den Film 'Tokyo Story' (1953), der in Kritikerumfragen regelmäßig unter den zehn besten Filmen aller Zeiten gelistet wird?",
        answerA = "Akira Kurosawa",
        answerB = "Kenji Mizoguchi",
        answerC = "Yasujiro Ozu",
        answerD = "Mikio Naruse",
        correctAnswer = 2,
        explanation = "'Tokyo Story' (Tōkyō Monogatari) von Yasujiro Ozu zeigt ein älteres Ehepaar, das ihre erwachsenen Kinder in Tokio besucht. In der Sight & Sound-Umfrage 2012 wurde er zum drittbesten Film aller Zeiten gewählt.",
        difficulty = 5,
        funFact = "Ozu verwendete fast immer eine sehr niedrige, am Boden positionierte Kamera – bekannt als 'Tatami-Perspektive' – die den Blick eines auf dem Boden sitzenden Japaners imitiert."
    ),

    // ─── NEW EASY (15 questions, difficulty=1) ────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Wie heißt der Zauberlehrling in der Harry-Potter-Filmreihe?",
        answerA = "Ron Weasley",
        answerB = "Neville Longbottom",
        answerC = "Harry Potter",
        answerD = "Draco Malfoy",
        correctAnswer = 2,
        explanation = "Harry Potter ist der Titelheld der Filmreihe, basierend auf den Büchern von J.K. Rowling. Er wird von Daniel Radcliffe gespielt.",
        difficulty = 1,
        funFact = "Daniel Radcliffe spielte Harry Potter in allen acht Filmen der Hauptreihe, von 2001 bis 2011."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Film singt Elsa das Lied 'Lass jetzt los' (englisch: 'Let It Go')?",
        answerA = "Moana",
        answerB = "Tangled",
        answerC = "Brave",
        answerD = "Frozen",
        correctAnswer = 3,
        explanation = "In Disneys 'Frozen – Die Eiskönigin' (2013) singt Königin Elsa 'Lass jetzt los', als sie ihren Eispalast baut.",
        difficulty = 1,
        funFact = "'Let It Go' gewann den Oscar für den besten Filmsong und wurde in über 40 Sprachen eingesungen."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Superhelden-Studio produziert Filme wie 'Avengers: Endgame' und 'Spider-Man: Homecoming'?",
        answerA = "DC Films",
        answerB = "Marvel Studios",
        answerC = "Sony Pictures",
        answerD = "Warner Bros.",
        correctAnswer = 1,
        explanation = "Marvel Studios ist das Produktionsstudio hinter dem Marvel Cinematic Universe (MCU), zu dem Avengers, Iron Man, Spider-Man und viele weitere Helden gehören.",
        difficulty = 1,
        funFact = "'Avengers: Endgame' (2019) ist mit über 2,79 Milliarden Dollar Einspiel der erfolgreichste Film aller Zeiten."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt den König der Löwen Simba in der Realverfilmung von 2019?",
        answerA = "Will Smith",
        answerB = "Dwayne Johnson",
        answerC = "Donald Glover",
        answerD = "Idris Elba",
        correctAnswer = 2,
        explanation = "Donald Glover (auch bekannt als Childish Gambino) leiht Simba in der CGI-Realverfilmung von 2019 seine Stimme.",
        difficulty = 1,
        funFact = "Beyoncé spricht und singt in der deutschen Version die Stimme von Nala, Simbas Freundin."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Film reist ein Hobbit namens Bilbo Beutlin auf ein großes Abenteuer?",
        answerA = "Der Herr der Ringe",
        answerB = "Der Hobbit",
        answerC = "Narnia",
        answerD = "Eragon",
        correctAnswer = 1,
        explanation = "In Peter Jacksons 'Der Hobbit' (2012–2014) begleiten wir Bilbo Beutlin auf einer Reise mit dreizehn Zwergen, um einen Drachen zu besiegen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film zeigt einen Roboter namens WALL-E, der allein die Erde aufräumt?",
        answerA = "Robots",
        answerB = "Interstellar",
        answerC = "WALL-E",
        answerD = "Big Hero 6",
        correctAnswer = 2,
        explanation = "Pixars 'WALL-E' (2008) erzählt die Geschichte eines kleinen Müllroboters, der als letzter auf der verlassenen Erde zurückbleibt.",
        difficulty = 1,
        funFact = "WALL-E spricht kaum Worte, erzählt aber durch Mimik und Gestik eine tiefe Liebesgeschichte."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film aus dem Jahr 2010 spielt in der Unterwasserwelt und war ein riesiger 3D-Kinoerfolg?",
        answerA = "Aquaman",
        answerB = "Avatar",
        answerC = "The Abyss",
        answerD = "Life of Pi",
        correctAnswer = 1,
        explanation = "James Camerons 'Avatar' (2009/2010) revolutionierte das 3D-Kino und ist mit 2,92 Milliarden Dollar (nach Neuauswertung 2023) der umsatzstärkste Film aller Zeiten.",
        difficulty = 1,
        funFact = "Avatar spielt auf dem Mond Pandora und nicht unter Wasser — aber die Farbgebung und Naturszenerie erinnern an ein Aquarium."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie heißt der grüne Oger in dem gleichnamigen Animationsfilm von DreamWorks aus dem Jahr 2001?",
        answerA = "Fiona",
        answerB = "Shrek",
        answerC = "Donkey",
        answerD = "Puss",
        correctAnswer = 1,
        explanation = "Shrek ist der liebenswerte grüne Oger, der in seinem Sumpf lebt und wider Willen auf Abenteuer geht.",
        difficulty = 1,
        funFact = "Shrek gewann 2002 als erster Film den Oscar für den besten Animationsfilm."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt James Bond in den Filmen von 2006 bis 2021?",
        answerA = "Pierce Brosnan",
        answerB = "Roger Moore",
        answerC = "Daniel Craig",
        answerD = "Sean Connery",
        correctAnswer = 2,
        explanation = "Daniel Craig verkörperte James Bond in fünf Filmen: Casino Royale, Quantum of Solace, Skyfall, Spectre und Keine Zeit zu sterben.",
        difficulty = 1,
        funFact = "Daniel Craig war der erste blonde James Bond – eine Änderung, die zunächst kontrovers aufgenommen wurde."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Film kämpft Simba gegen seinen Onkel Scar?",
        answerA = "Bambi",
        answerB = "Der König der Löwen",
        answerC = "Tarzan",
        answerD = "Das Dschungelbuch",
        correctAnswer = 1,
        explanation = "In Disneys 'Der König der Löwen' (1994) kehrt Simba zurück, um gegen seinen mörderischen Onkel Scar zu kämpfen und seinen rechtmäßigen Platz einzunehmen.",
        difficulty = 1,
        funFact = "Die Geschichte des Königs der Löwen ist lose an Shakespeares 'Hamlet' angelehnt."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Figur sagt in 'Iron Man' (2008) den berühmten Satz: 'I am Iron Man'?",
        answerA = "Nick Fury",
        answerB = "Pepper Potts",
        answerC = "Tony Stark",
        answerD = "James Rhodes",
        correctAnswer = 2,
        explanation = "Am Ende von 'Iron Man' (2008) enthüllt Tony Stark überraschend öffentlich seine Identität als Iron Man.",
        difficulty = 1,
        funFact = "Robert Downey Jr. improvisierte die Szene 'I am Iron Man' – sie stand so nicht im Drehbuch."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie heißt die Magierschule, die Harry Potter besucht?",
        answerA = "Durmstrang",
        answerB = "Beauxbatons",
        answerC = "Hogwarts",
        answerD = "Ilvermorny",
        correctAnswer = 2,
        explanation = "Harry Potter besucht die Hogwarts-Schule für Hexerei und Zauberei in Schottland.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Filmcharakter lebt in der Sesame Street und ist ein großes, gelbes Federvieh?",
        answerA = "Kermit",
        answerB = "Big Bird",
        answerC = "Elmo",
        answerD = "Cookie Monster",
        correctAnswer = 1,
        explanation = "Big Bird ist eine der bekanntesten Figuren der Sesamstraße – ein 2,46 Meter großes gelbes Küken.",
        difficulty = 1,
        funFact = "Die Sesamstraße läuft seit 1969 – sie ist eine der am längsten laufenden Kindersendungen der Welt."
    ),

    Question(
        categoryId = 4,
        questionText = "In welcher Stadt spielt der Film 'The Dark Knight' (2008)?",
        answerA = "New York",
        answerB = "Metropolis",
        answerC = "Gotham City",
        answerD = "Chicago",
        correctAnswer = 2,
        explanation = "Batman schützt die fiktive Stadt Gotham City – obwohl der Film in Chicago gedreht wurde.",
        difficulty = 1,
        funFact = "Christopher Nolan drehte Szenen von 'The Dark Knight' tatsächlich in Chicago – die Skyline der Stadt ist im Film deutlich erkennbar."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Animationsfilm von 2004 zeigt Superhelden, die ihr Leben als normale Bürger verbergen müssen?",
        answerA = "Big Hero 6",
        answerB = "Die Unglaublichen",
        answerC = "Megamind",
        answerD = "Sky High",
        correctAnswer = 1,
        explanation = "Pixars 'Die Unglaublichen' (The Incredibles, 2004) erzählt von einer Superfamilie, die im normalen Alltag untertauchen muss.",
        difficulty = 1,
        funFact = "Die Unglaublichen wurde 2018 mit 'Incredibles 2' fortgesetzt – ein Pixar-Film ohne direkten Vorgänger nach 14 Jahren."
    ),

    // ─── NEW MEDIUM (20 questions, difficulty=2) ──────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Denis Villeneuve aus dem Jahr 2017 ist eine Fortsetzung eines Science-Fiction-Klassikers von 1982?",
        answerA = "Arrival",
        answerB = "Dune",
        answerC = "Blade Runner 2049",
        answerD = "Sicario",
        correctAnswer = 2,
        explanation = "'Blade Runner 2049' (2017) ist die Fortsetzung von Ridley Scotts 'Blade Runner' (1982) und spielt 30 Jahre nach dem Original.",
        difficulty = 2,
        funFact = "Ryan Gosling und Harrison Ford spielen die Hauptrollen – Ford reprised seine Rolle als Rick Deckard aus dem Original."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler gewann für seine Rolle in 'The Revenant' (2015) seinen ersten Oscar?",
        answerA = "Tom Hardy",
        answerB = "Leonardo DiCaprio",
        answerC = "Will Smith",
        answerD = "Michael Fassbender",
        correctAnswer = 1,
        explanation = "Leonardo DiCaprio gewann nach mehreren Nominierungen endlich seinen ersten Oscar als bester Hauptdarsteller für 'The Revenant'.",
        difficulty = 2,
        funFact = "Für 'The Revenant' schlief DiCaprio in Tierkadavern und aß rohe Bisonleber für authentische Szenen."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche TV-Serie von HBO aus dem Jahr 2019 handelt von der Nuklearkatastrophe in der Sowjetunion?",
        answerA = "Band of Brothers",
        answerB = "The Wire",
        answerC = "Chernobyl",
        answerD = "True Detective",
        correctAnswer = 2,
        explanation = "'Chernobyl' (2019) ist eine HBO-Miniserie, die die Reaktorkatastrophe von Tschernobyl 1986 und ihre Folgen dramatisiert.",
        difficulty = 2,
        funFact = "'Chernobyl' erreichte bei IMDb zeitweise eine Bewertung von 9,4 und gilt als eine der bestbewerteten Serien aller Zeiten."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt den Hauptcharakter Maximus in 'Gladiator' (2000)?",
        answerA = "Gerard Butler",
        answerB = "Russell Crowe",
        answerC = "Mel Gibson",
        answerD = "Colin Farrell",
        correctAnswer = 1,
        explanation = "Russell Crowe spielt den römischen General Maximus, der zum Sklaven und Gladiator wird. Er gewann dafür den Oscar als bester Hauptdarsteller.",
        difficulty = 2,
        funFact = "Der Satz 'Are you not entertained?' von Russell Crowe im Kolosseum wurde zu einem der bekanntesten Filmzitate."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Jordan Peele aus dem Jahr 2017 thematisiert Rassismus in Amerika auf Horrorfilm-Art?",
        answerA = "Us",
        answerB = "Get Out",
        answerC = "Nope",
        answerD = "Candyman",
        correctAnswer = 1,
        explanation = "'Get Out' (2017) ist Jordan Peeles Regiedebüt, ein Horrorfilm über einen schwarzen Mann, der die weiße Familie seiner Freundin besucht.",
        difficulty = 2,
        funFact = "Jordan Peele gewann für das Drehbuch zu 'Get Out' den Oscar – als erster schwarzer Autor für ein Originaldrehbuch."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie viele Filme umfasst die originale 'Star Wars'-Saga (Episode I bis VI)?",
        answerA = "4",
        answerB = "5",
        answerC = "6",
        answerD = "7",
        correctAnswer = 2,
        explanation = "Die originale Star-Wars-Saga besteht aus sechs Episoden: der Prequel-Trilogie (I–III) und der Original-Trilogie (IV–VI).",
        difficulty = 2,
        funFact = "George Lucas drehte zunächst Episode IV (1977) und fügte später die Prequels hinzu – die Episode I erschien erst 1999."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt den Bösewicht Thanos in den Avengers-Filmen?",
        answerA = "Idris Elba",
        answerB = "Josh Brolin",
        answerC = "Laurence Fishburne",
        answerD = "Chiwetel Ejiofor",
        correctAnswer = 1,
        explanation = "Josh Brolin verleiht Thanos Stimme und Performance-Capture in den MCU-Filmen, besonders in 'Avengers: Infinity War' und 'Endgame'.",
        difficulty = 2,
        funFact = "Thanos schnippt in 'Infinity War' mit dem Infinity-Handschuh, um die Hälfte aller Lebewesen im Universum zu vernichten – bekannt als 'The Snap'."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Christopher Nolan aus dem Jahr 2014 zeigt Astronauten, die durch ein Wurmloch reisen?",
        answerA = "Inception",
        answerB = "The Prestige",
        answerC = "Dunkirk",
        answerD = "Interstellar",
        correctAnswer = 3,
        explanation = "'Interstellar' (2014) handelt von Astronauten, die durch ein Wurmloch in der Nähe des Saturn reisen, um bewohnbare Planeten zu finden.",
        difficulty = 2,
        funFact = "Für die realistischen Darstellungen von Schwarzen Löchern und Zeitdilatation arbeitete Nolan mit dem Astrophysiker Kip Thorne zusammen."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Land spielt die Serie 'Money Heist' (La Casa de Papel)?",
        answerA = "Italien",
        answerB = "Spanien",
        answerC = "Frankreich",
        answerD = "Portugal",
        correctAnswer = 1,
        explanation = "'La Casa de Papel' ist eine spanische Serie, in der eine Räuberbande die spanische Münzanstalt überfällt.",
        difficulty = 2,
        funFact = "Die Serie wurde für Netflix international lizenziert und wurde eine der meistgesehenen nicht-englischsprachigen Serien weltweit."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Ridley Scott aus dem Jahr 2000 gewann den Oscar für den besten Film?",
        answerA = "Blade Runner",
        answerB = "Alien",
        answerC = "Gladiator",
        answerD = "Black Hawk Down",
        correctAnswer = 2,
        explanation = "'Gladiator' (2000) von Ridley Scott gewann fünf Oscars, darunter Bester Film und Bester Hauptdarsteller.",
        difficulty = 2,
        funFact = "Ridley Scott hatte bis zu 'Gladiator' noch keinen Oscar-nominierten Film als besten Film – es war sein erster großer Academy-Award-Triumph."
    ),

    Question(
        categoryId = 4,
        questionText = "Wer spielte die Hauptrolle in dem Biopic 'Bohemian Rhapsody' über Freddie Mercury?",
        answerA = "Ben Whishaw",
        answerB = "Taron Egerton",
        answerC = "Rami Malek",
        answerD = "Sacha Baron Cohen",
        correctAnswer = 2,
        explanation = "Rami Malek spielt Freddie Mercury in 'Bohemian Rhapsody' (2018) und gewann dafür den Oscar als bester Hauptdarsteller.",
        difficulty = 2,
        funFact = "Sacha Baron Cohen war ursprünglich für die Rolle vorgesehen, schied aber aufgrund kreativer Differenzen aus."
    ),

    Question(
        categoryId = 4,
        questionText = "Welche Streaming-Serie handelt von einer mexikanischen Drogenhändlerfamilie und basiert auf wahren Begebenheiten?",
        answerA = "Ozark",
        answerB = "Narcos",
        answerC = "El Chapo",
        answerD = "Queen of the South",
        correctAnswer = 1,
        explanation = "'Narcos' (Netflix, 2015) dokumentiert den Aufstieg und Fall von Pablo Escobar und dem Medellín-Kartell in Kolumbien.",
        difficulty = 2,
        funFact = "Teile von 'Narcos' wurden an den originalgetreuen Schauplätzen in Kolumbien gedreht."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Film spielt Meryl Streep eine strenge Modechefin in der Fashionbranche?",
        answerA = "Mamma Mia!",
        answerB = "Kramer gegen Kramer",
        answerC = "Der Teufel trägt Prada",
        answerD = "Julie & Julia",
        correctAnswer = 2,
        explanation = "In 'Der Teufel trägt Prada' (2006) spielt Meryl Streep die einschüchternde Chefredakteurin Miranda Priestly, basierend auf Vogue-Chefin Anna Wintour.",
        difficulty = 2,
        funFact = "Anne Hathaway spielt Mirandas Assistentin Andy – der Film ist lose an das reale Modemagazin Vogue angelehnt."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Schauspieler spielt den Hauptcharakter in der Serie 'The Crown' in den Staffeln 1 und 2 als Prinz Philip?",
        answerA = "Matt Smith",
        answerB = "Tobias Menzies",
        answerC = "Jonathan Pryce",
        answerD = "Ben Miles",
        correctAnswer = 0,
        explanation = "Matt Smith spielte Prinz Philip in den ersten beiden Staffeln von 'The Crown' (2016–2017) auf Netflix.",
        difficulty = 2,
        funFact = "Matt Smith ist auch bekannt als der elfte Doctor Who in der BBC-Serie 'Doctor Who'."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Damien Chazelle gewann 2017 den Oscar für den besten Film, obwohl anfangs irrtümlich ein anderer Film ausgerufen wurde?",
        answerA = "Moonlight",
        answerB = "La La Land",
        answerC = "Hidden Figures",
        answerD = "Arrival",
        correctAnswer = 0,
        explanation = "'Moonlight' von Barry Jenkins gewann den Oscar für den besten Film 2017. Irrtümlicherweise wurde zunächst 'La La Land' ausgerufen – ein beispielloser Fehler in der Oscar-Geschichte.",
        difficulty = 2,
        funFact = "Der Umschlag-Fehler 2017 war der größte Fauxpas in der 89-jährigen Geschichte der Academy Awards."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Film von 1999 entdeckt ein Mathematiker, dass seine Realität eine simulierte Welt ist?",
        answerA = "Dark City",
        answerB = "eXistenZ",
        answerC = "The Thirteenth Floor",
        answerD = "The Matrix",
        correctAnswer = 3,
        explanation = "'The Matrix' (1999) der Wachowski-Schwestern zeigt Neo, der erkennt, dass die Welt eine von Maschinen erzeugte Computersimulation ist.",
        difficulty = 2,
        funFact = "Die berühmte 'Bullet-Time'-Technik aus The Matrix wurde eigens für diesen Film entwickelt und beeinflusste die Filmtechnik nachhaltig."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Darsteller spielt den Schurken Voldemort in den Harry-Potter-Filmen?",
        answerA = "Alan Rickman",
        answerB = "Ralph Fiennes",
        answerC = "Jason Isaacs",
        answerD = "Gary Oldman",
        correctAnswer = 1,
        explanation = "Ralph Fiennes spielt Lord Voldemort in den Harry-Potter-Filmen ab Teil 4 ('Der Feuerkelch').",
        difficulty = 2,
        funFact = "In den ersten beiden Filmen spielte Ian Hart und später Richard Bremmer die Stimme/den Körper von Voldemort."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Martin Scorsese aus dem Jahr 2006 gewann den Oscar für den besten Film?",
        answerA = "Gangs of New York",
        answerB = "The Aviator",
        answerC = "Departed – Unter Feinden",
        answerD = "Shutter Island",
        correctAnswer = 2,
        explanation = "'Departed – Unter Feinden' (The Departed, 2006) ist Scorseses erstes Oscar-prämiiertes Werk als bester Film, ein Remake des Hongkong-Thrillers 'Infernal Affairs'.",
        difficulty = 2,
        funFact = "Scorsese erhielt mit 'The Departed' endlich seinen ersten Oscar für die beste Regie, nach mehreren früheren Nominierungen ohne Gewinn."
    ),

    Question(
        categoryId = 4,
        questionText = "Wie heißt der Bösewicht in der Serie 'Breaking Bad', der als Drogenbaron auftritt?",
        answerA = "Tuco Salamanca",
        answerB = "Gus Fring",
        answerC = "Hector Salamanca",
        answerD = "Mike Ehrmantraut",
        correctAnswer = 1,
        explanation = "Gustavo 'Gus' Fring ist der gefürchtete Drogenbaron hinter einer Fast-Food-Kette, der in Staffel 3 und 4 zum Hauptantagonisten wird.",
        difficulty = 2,
        funFact = "Giancarlo Esposito, der Darsteller von Gus Fring, wurde für seine Rolle mehrfach für den Emmy nominiert."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Oscar-prämierte Film von 2022 handelt von einer tauben Musikerin in einem hörenden Umfeld?",
        answerA = "The Father",
        answerB = "Sound of Metal",
        answerC = "CODA",
        answerD = "Drive My Car",
        correctAnswer = 2,
        explanation = "'CODA' (2021/2022) gewann den Oscar für den besten Film und handelt von Ruby, der hörenden Tochter tauber Eltern (CODA = Child of Deaf Adults).",
        difficulty = 2,
        funFact = "'CODA' war der erste Film eines Streaming-Dienstes (Apple TV+), der den Oscar für den besten Film gewann."
    ),

    // ─── NEW HARD (15 questions, difficulty=3) ────────────────────────────────

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Andrei Tarkowski aus dem Jahr 1972 basiert auf einem Roman von Stanisław Lem?",
        answerA = "Stalker",
        answerB = "Andrei Rublev",
        answerC = "Solaris",
        answerD = "Der Spiegel",
        correctAnswer = 2,
        explanation = "'Solaris' (1972) von Andrei Tarkowski basiert auf dem gleichnamigen Roman von Stanisław Lem und gilt als eines der bedeutendsten Werke des Science-Fiction-Kinos.",
        difficulty = 3,
        funFact = "Tarkowski und Lem sollen sich nach dem Film überworfen haben, da Tarkowski das Buch zu wenig wörtlich adaptiert habe."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur schuf die Spaghetti-Western-Trilogie 'Dollar-Trilogie' mit Clint Eastwood?",
        answerA = "Dario Argento",
        answerB = "Bernardo Bertolucci",
        answerC = "Luchino Visconti",
        answerD = "Sergio Leone",
        correctAnswer = 3,
        explanation = "Sergio Leone drehte mit Clint Eastwood die 'Dollar-Trilogie': 'Für eine Handvoll Dollar', 'Für ein paar Dollar mehr' und 'Zwei glorreiche Halunken'.",
        difficulty = 3,
        funFact = "Ennio Morricone komponierte die berühmten Soundtracks für alle drei Leone-Western – sein Pfeifen und Gitarrenriff sind ikonisch."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Roman Polański aus dem Jahr 1974 gilt als eines der größten Neo-Noir-Meisterwerke Hollywoods?",
        answerA = "Rosemaries Baby",
        answerB = "Chinatown",
        answerC = "Der Mieter",
        answerD = "Das Messer im Wasser",
        correctAnswer = 1,
        explanation = "'Chinatown' (1974) mit Jack Nicholson ist ein klassischer Neo-Noir, bei dem Polański Regie führte und Robert Towne das Drehbuch schrieb.",
        difficulty = 3,
        funFact = "Das Drehbuch zu 'Chinatown' gilt als eines der besten Drehbücher Hollywoods und wird an Filmhochschulen als Vorlage genutzt."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Jahr erschien Orson Welles' revolutionärer Film 'Citizen Kane'?",
        answerA = "1939",
        answerB = "1941",
        answerC = "1944",
        answerD = "1946",
        correctAnswer = 1,
        explanation = "'Citizen Kane' erschien 1941 und wird regelmäßig als bester Film aller Zeiten gelistet. Orson Welles war bei der Produktion erst 25 Jahre alt.",
        difficulty = 3,
        funFact = "Der Medientycoon im Film ist lose an William Randolph Hearst angelehnt, der versuchte, den Film zu verhindern."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher schwedische Regisseur drehte neben Ingmar Bergman ebenfalls mehrere international bekannte Filmklassiker, darunter 'Elvira Madigan' (1967)?",
        answerA = "Jan Troell",
        answerB = "Bo Widerberg",
        answerC = "Roy Andersson",
        answerD = "Lukas Moodysson",
        correctAnswer = 1,
        explanation = "Bo Widerberg inszenierte 'Elvira Madigan' (1967), ein romantisches Drama über eine verbotene Liebe, das für seine außergewöhnliche Bildsprache bekannt ist.",
        difficulty = 3,
        funFact = "Das Mozarts Klavierkonzert Nr. 21, das im Film verwendet wird, wird seitdem oft als 'Elvira Madigan'-Konzert bezeichnet."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher österreichisch-deutsche Schauspieler wurde für seinen Auftritt in 'Shine' (1996) für den Oscar als bester Nebendarsteller nominiert?",
        answerA = "Klaus Maria Brandauer",
        answerB = "Bruno Ganz",
        answerC = "Armin Mueller-Stahl",
        answerD = "Maximilian Schell",
        correctAnswer = 2,
        explanation = "Armin Mueller-Stahl wurde für seine Rolle als Geigenvater in 'Shine' (1996) für den Oscar als bester Nebendarsteller nominiert, gewann aber nicht.",
        difficulty = 3,
        funFact = "Maximilian Schell war 1961 der erste deutschsprachige Schauspieler, der den Oscar als bester Hauptdarsteller gewann – für 'Das Urteil von Nürnberg'."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Luis Buñuel und Salvador Dalí aus dem Jahr 1929 gilt als Meilenstein des Surrealismus?",
        answerA = "L'Age d'Or",
        answerB = "Un Chien Andalou",
        answerC = "Belle de Jour",
        answerD = "Viridiana",
        correctAnswer = 1,
        explanation = "'Un Chien Andalou' (Ein andalusischer Hund, 1929) ist ein Kurzfilm von Luis Buñuel und Salvador Dalí, berühmt für seine schockierende Augapfel-Szene.",
        difficulty = 3,
        funFact = "Der Film wurde angeblich aus Träumen von Buñuel und Dalí zusammengesetzt und hat keine konventionelle Erzählstruktur."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher US-amerikanische Regisseur drehte 'Apocalypse Now' und basierte den Film auf welchem literarischen Werk?",
        answerA = "Francis Ford Coppola, basierend auf 'Lord Jim' von Joseph Conrad",
        answerB = "Francis Ford Coppola, basierend auf 'Herz der Finsternis' von Joseph Conrad",
        answerC = "Michael Cimino, basierend auf 'Heart of Darkness' von Graham Greene",
        answerD = "Oliver Stone, basierend auf 'Catch-22' von Joseph Heller",
        correctAnswer = 1,
        explanation = "'Apocalypse Now' (1979) von Francis Ford Coppola basiert auf Joseph Conrads Novelle 'Herz der Finsternis' (Heart of Darkness), verlegt in den Vietnamkrieg.",
        difficulty = 3,
        funFact = "Die legendären Dreharbeiten dauerten 16 Monate in den Philippinen und wurden wegen Taifunen, Erkrankungen und dem Herzinfarkt des Hauptdarstellers unterbrochen."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Alfonso Cuarón aus dem Jahr 2018 gewann drei Oscars, darunter Bester fremdsprachiger Film und Beste Regie?",
        answerA = "Gravity",
        answerB = "Children of Men",
        answerC = "Roma",
        answerD = "Y Tu Mamá También",
        correctAnswer = 2,
        explanation = "'Roma' (2018) ist ein schwarzweißes Drama über eine Haushaltshilfe in Mexiko-Stadt in den 1970ern. Es gewann als erster Netflix-Film die Oscars für Bester fremdsprachiger Film und Beste Regie.",
        difficulty = 3,
        funFact = "'Roma' ist semi-autobiografisch und basiert auf Cuaróns Kindheitserinnerungen an seine eigene Kindheit in Mexiko."
    ),

    Question(
        categoryId = 4,
        questionText = "In welchem Polanski-Film aus dem Jahr 1968 wird eine Frau von satanistischen Nachbarn mit dem Teufel geschwängert?",
        answerA = "Der Mieter",
        answerB = "Tanz der Vampire",
        answerC = "Rosemaries Baby",
        answerD = "Ekel",
        correctAnswer = 2,
        explanation = "'Rosemaries Baby' (1968) ist ein psychologischer Horrorfilm mit Mia Farrow, basierend auf dem Roman von Ira Levin.",
        difficulty = 3,
        funFact = "Die Filmmusik von Krzysztof Komeda und Mia Farrows Spiel machten den Film zu einem Klassiker des Psycho-Horrors."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher österreichische Regisseur drehte 'Funny Games' zweimal – 1997 und 2007 als Hollywood-Remake?",
        answerA = "Stefan Ruzowitzky",
        answerB = "Ulrich Seidl",
        answerC = "Michael Haneke",
        answerD = "Barbara Albert",
        correctAnswer = 2,
        explanation = "Michael Haneke drehte 'Funny Games' 1997 auf Deutsch und 2007 als englischsprachiges Remake mit nahezu identischem Drehbuch und ähnlicher Besetzung.",
        difficulty = 3,
        funFact = "Haneke selbst sagte, er habe das Remake speziell für das amerikanische Publikum gemacht, das den Originalfilm nie sehen würde."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Wolfgang Petersen aus dem Jahr 1981 zeigt das Leben auf einem deutschen U-Boot im Zweiten Weltkrieg?",
        answerA = "Das Boot",
        answerB = "Die Patriotin",
        answerC = "Kriegsfilm",
        answerD = "Der Untergang",
        correctAnswer = 0,
        explanation = "'Das Boot' (1981) von Wolfgang Petersen ist eines der bedeutendsten deutschen Filme und wurde für sechs Oscars nominiert.",
        difficulty = 3,
        funFact = "'Das Boot' war ursprünglich ein fünfstündiger Fernsehfilm, der für die internationale Kinofassung auf 2,5 Stunden gekürzt wurde."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Filmkomponist schrieb die Musik zu 'Star Wars' (1977) und 'Jaws' (1975)?",
        answerA = "Ennio Morricone",
        answerB = "Bernard Herrmann",
        answerC = "Jerry Goldsmith",
        answerD = "John Williams",
        correctAnswer = 3,
        explanation = "John Williams komponierte die ikonischen Soundtracks zu 'Star Wars' und 'Der weiße Hai' und ist einer der bedeutendsten Filmkomponisten aller Zeiten.",
        difficulty = 3,
        funFact = "John Williams hat über 50 Oscar-Nominierungen erhalten – mehr als jeder andere lebende Mensch."
    ),

    Question(
        categoryId = 4,
        questionText = "Welcher Film von Volker Schlöndorff basiert auf einem Roman von Günter Grass und gewann 1980 den Oscar für den besten fremdsprachigen Film?",
        answerA = "Die Fälschung",
        answerB = "Der Fangschuss",
        answerC = "Die Blechtrommel",
        answerD = "Der Unhold",
        correctAnswer = 2,
        explanation = "'Die Blechtrommel' (1979) von Volker Schlöndorff basiert auf Günter Grass' gleichnamigem Roman und gewann als erster westdeutscher Film den Oscar für den besten fremdsprachigen Film.",
        difficulty = 3,
        funFact = "Die Hauptrolle des Oskar Matzerath, der aufhört zu wachsen, spielt David Bennent – er war bei den Dreharbeiten erst 12 Jahre alt."
    ),

    Question(
        categoryId = 4,
        questionText = "Welches Filmfestival verleiht den Hauptpreis 'Goldenen Bären'?",
        answerA = "Cannes Film Festival",
        answerB = "Venedig Film Festival",
        answerC = "Sundance Film Festival",
        answerD = "Berlinale",
        correctAnswer = 3,
        explanation = "Die Berlinale (Internationale Filmfestspiele Berlin) verleiht den Goldenen Bären als höchste Auszeichnung – analog zur Goldenen Palme in Cannes und dem Goldenen Löwen in Venedig.",
        difficulty = 3,
        funFact = "Die Berlinale wurde 1951 gegründet und ist eines der ältesten und bedeutendsten Filmfestivals der Welt."
    )
)
