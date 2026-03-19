package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun literatureQuestionsEasy6(): List<Question> = listOf(

    // ── EASY (difficulty = 1) ── Nobelpreis, Bestseller & Allgemeine Literatur ──

    // correctAnswer distribution: 0=A(13), 1=B(13), 2=C(12), 3=D(12) → total 50

    Question(
        categoryId = 10,
        questionText = "Welcher deutsche Autor erhielt 1929 den Nobelpreis für Literatur?",
        answerA = "Hermann Hesse",
        answerB = "Bertolt Brecht",
        answerC = "Thomas Mann",
        answerD = "Günter Grass",
        correctAnswer = 2,
        explanation = "Thomas Mann erhielt 1929 den Nobelpreis für Literatur, hauptsächlich für seinen Roman 'Die Buddenbrooks'.",
        difficulty = 1,
        funFact = "Thomas Mann schrieb die 'Buddenbrooks' mit nur 25 Jahren – und gewann dafür später den Nobelpreis."
    ),

    Question(
        categoryId = 10,
        questionText = "Hermann Hesse ist Autor welches berühmten Romans über eine spirituelle Reise in Indien?",
        answerA = "Siddhartha",
        answerB = "Demian",
        answerC = "Der Steppenwolf",
        answerD = "Narziß und Goldmund",
        correctAnswer = 0,
        explanation = "Siddhartha (1922) erzählt die spirituelle Reise eines jungen Brahmanen im alten Indien und ist weltweit eines der meistgelesenen Werke Hesses.",
        difficulty = 1,
        funFact = "Hermann Hesse erhielt 1946 den Nobelpreis für Literatur – 24 Jahre nach Erscheinen von Siddhartha."
    ),

    Question(
        categoryId = 10,
        questionText = "Günter Grass erhielt 1999 den Nobelpreis für Literatur. Welcher Roman machte ihn weltberühmt?",
        answerA = "Ein weites Feld",
        answerB = "Der Butt",
        answerC = "Die Blechtrommel",
        answerD = "Katz und Maus",
        correctAnswer = 2,
        explanation = "Die Blechtrommel (1959) ist Günter Grass' bekanntester Roman und erzählt die Geschichte des Oskar Matzerath, der mit drei Jahren beschließt, nicht mehr zu wachsen.",
        difficulty = 1,
        funFact = "Die Blechtrommel wurde 1979 von Volker Schlöndorff verfilmt und gewann den Oscar als bester fremdsprachiger Film."
    ),

    Question(
        categoryId = 10,
        questionText = "Welche deutsche Autorin gewann 2009 den Nobelpreis für Literatur?",
        answerA = "Christa Wolf",
        answerB = "Elfriede Jelinek",
        answerC = "Herta Müller",
        answerD = "Ingeborg Bachmann",
        correctAnswer = 2,
        explanation = "Herta Müller, geboren im rumänischen Banat, erhielt 2009 den Nobelpreis für Literatur für ihr Werk über Leben unter Diktaturen.",
        difficulty = 1,
        funFact = "Herta Müller war erst die zwölfte Frau, die den Nobelpreis für Literatur gewann."
    ),

    Question(
        categoryId = 10,
        questionText = "Bob Dylan erhielt 2016 den Nobelpreis für Literatur. Womit ist er hauptsächlich bekannt?",
        answerA = "Als Schriftsteller von Kurzgeschichten",
        answerB = "Als Dichter und Musiker",
        answerC = "Als Drehbuchautor",
        answerD = "Als Theaterdramatiker",
        correctAnswer = 1,
        explanation = "Bob Dylan ist primär als Musiker und Songwriter bekannt. Die Nobelpreisvergabe an ihn war äußerst ungewöhnlich, da er der erste Musiker war, der diesen Literaturpreis erhielt.",
        difficulty = 1,
        funFact = "Bob Dylan nahm seinen Nobelpreis nicht persönlich entgegen – er schickte die Musikerin Patti Smith zur Zeremonie."
    ),

    Question(
        categoryId = 10,
        questionText = "Wer schrieb den Weltbestseller 'The Da Vinci Code' (Sakrileg)?",
        answerA = "Dan Brown",
        answerB = "John Grisham",
        answerC = "James Patterson",
        answerD = "Tom Clancy",
        correctAnswer = 0,
        explanation = "Dan Brown veröffentlichte 'The Da Vinci Code' im Jahr 2003. Das Buch wurde ein weltweiter Megabestseller mit über 80 Millionen verkauften Exemplaren.",
        difficulty = 1,
        funFact = "Für die deutsche Übersetzung wurde der Titel 'Sakrileg' gewählt, da 'Der Da Vinci Code' ungeschützt ist."
    ),

    Question(
        categoryId = 10,
        questionText = "Der Harvard-Professor Robert Langdon ist die Hauptfigur in Büchern von welchem Autor?",
        answerA = "Lee Child",
        answerB = "Harlan Coben",
        answerC = "Michael Connelly",
        answerD = "Dan Brown",
        correctAnswer = 3,
        explanation = "Robert Langdon, ein Harvard-Professor für Symbologie, ist Dan Browns berühmteste Figur und tritt in Romanen wie 'Sakrileg', 'Illuminati' und 'Inferno' auf.",
        difficulty = 1,
        funFact = "Dan Brown schrieb zunächst 'Illuminati' (Angels & Demons), in dem Langdon ebenfalls vorkommt – aber 'Sakrileg' wurde zuerst verfilmt."
    ),

    Question(
        categoryId = 10,
        questionText = "Stieg Larssons Millennium-Trilogie beginnt mit welchem Roman?",
        answerA = "Verdammnis",
        answerB = "Verblendung",
        answerC = "Vergebung",
        answerD = "Verschwörung",
        correctAnswer = 1,
        explanation = "Die Millennium-Trilogie beginnt mit 'Verblendung' (Originaltitel: 'Män som hatar kvinnor' – Männer, die Frauen hassen), dem ersten Band mit Lisbeth Salander.",
        difficulty = 1,
        funFact = "Stieg Larsson starb 2004, bevor er die Veröffentlichung seiner Bücher erlebte – sie wurden posthum zu Welterfolgen."
    ),

    Question(
        categoryId = 10,
        questionText = "Wer ist die wichtigste weibliche Hauptfigur in Stieg Larssons Millennium-Trilogie?",
        answerA = "Erika Berger",
        answerB = "Lisbeth Salander",
        answerC = "Harriet Vanger",
        answerD = "Dragan Armansky",
        correctAnswer = 1,
        explanation = "Lisbeth Salander, eine hochbegabte Hackerin mit einer schwierigen Vergangenheit, ist die zentrale Figur der Millennium-Trilogie.",
        difficulty = 1,
        funFact = "Lisbeth Salander wurde von Zeitschriften mehrfach zur einflussreichsten Figur der Kriminalliteratur des 21. Jahrhunderts gewählt."
    ),

    Question(
        categoryId = 10,
        questionText = "Joanne K. Rowling schrieb die weltberühmte Buchreihe über welchen Zauberlehrling?",
        answerA = "Harry Potter",
        answerB = "Percy Jackson",
        answerC = "Merlin",
        answerD = "Frodo Beutlin",
        correctAnswer = 0,
        explanation = "Harry Potter ist der Protagonist der gleichnamigen Buchreihe von J.K. Rowling. Die Reihe umfasst 7 Bände und ist die meistverkaufte Buchreihe der Geschichte.",
        difficulty = 1,
        funFact = "J.K. Rowling hatte die Idee zu Harry Potter 1990 in einem verspäteten Zug von Manchester nach London."
    ),

    Question(
        categoryId = 10,
        questionText = "In welcher Zauberschule lernt Harry Potter?",
        answerA = "Durmstrang",
        answerB = "Hogwarts",
        answerC = "Beauxbatons",
        answerD = "Ilvermorny",
        correctAnswer = 1,
        explanation = "Hogwarts School of Witchcraft and Wizardry ist die britische Zauberschule, in der Harry Potter, Hermine Granger und Ron Weasley ihre magische Ausbildung erhalten.",
        difficulty = 1,
        funFact = "Der Name 'Hogwarts' soll J.K. Rowling eingefallen sein, als sie an einer Hogwort-Pflanze im Botanischen Garten vorbeikam."
    ),

    Question(
        categoryId = 10,
        questionText = "Wie viele Hauptbände umfasst die Harry-Potter-Reihe von J.K. Rowling?",
        answerA = "5",
        answerB = "6",
        answerC = "7",
        answerD = "8",
        correctAnswer = 2,
        explanation = "Die Harry-Potter-Hauptreihe umfasst 7 Bände, vom Stein der Weisen (1997) bis zu den Heiligtümern des Todes (2007).",
        difficulty = 1,
        funFact = "Die Harry-Potter-Reihe ist mit über 600 Millionen verkauften Exemplaren die meistverkaufte Buchreihe der Geschichte."
    ),

    Question(
        categoryId = 10,
        questionText = "Stephen King schrieb den Horror-Roman über den Clown 'Pennywise'. Wie heißt das Buch?",
        answerA = "Carrie",
        answerB = "Es",
        answerC = "Shining",
        answerD = "Pet Sematary",
        correctAnswer = 1,
        explanation = "Stephen Kings Roman 'Es' (1986) handelt von einer Gruppe Kinder in der Stadt Derry, die von einem übernatürlichen Wesen verfolgt werden, das häufig als Clown Pennywise erscheint.",
        difficulty = 1,
        funFact = "Stephen Kings 'Es' ist mit über 1.100 Seiten einer der längsten Horror-Romane aller Zeiten."
    ),

    Question(
        categoryId = 10,
        questionText = "Welches Buch gilt als das meistgedruckte Buch der Welt?",
        answerA = "Das Kapital von Karl Marx",
        answerB = "Don Quijote von Cervantes",
        answerC = "Die Bibel",
        answerD = "Harry Potter und der Stein der Weisen",
        correctAnswer = 2,
        explanation = "Die Bibel ist das meistgedruckte Buch der Menschheitsgeschichte mit schätzungsweise 5 bis 7 Milliarden gedruckten Exemplaren.",
        difficulty = 1,
        funFact = "Die Bibel wurde in über 3.500 Sprachen und Dialekte übersetzt – kein anderes Buch der Welt ist in so viele Sprachen übertragen worden."
    ),

    Question(
        categoryId = 10,
        questionText = "Aus wie vielen Büchern besteht der protestantische Bibelkanon (Altes und Neues Testament)?",
        answerA = "39",
        answerB = "73",
        answerC = "52",
        answerD = "66",
        correctAnswer = 3,
        explanation = "Der protestantische Bibelkanon umfasst 66 Bücher (39 im Alten und 27 im Neuen Testament). Der katholische Kanon enthält 73 Bücher.",
        difficulty = 1,
        funFact = "Das kürzeste Buch der Bibel ist der 2. Johannesbrief mit nur 13 Versen, das längste ist der Psalm 119 mit 176 Versen."
    ),

    Question(
        categoryId = 10,
        questionText = "C.S. Lewis schrieb 'Die Chroniken von Narnia'. Wie heißt der erste und bekannteste Band?",
        answerA = "Prinz Kaspian von Narnia",
        answerB = "Der König von Narnia",
        answerC = "Der Löwe, die Hexe und der Kleiderschrank",
        answerD = "Der Ritt nach Narnia",
        correctAnswer = 2,
        explanation = "Der erste und bekannteste Band der Narnia-Reihe ist 'Der Löwe, die Hexe und der Kleiderschrank' (1950), in dem vier Geschwister durch einen Kleiderschrank ins magische Narnia gelangen.",
        difficulty = 1,
        funFact = "C.S. Lewis war ein enger Freund von J.R.R. Tolkien – beide Autoren tauschten regelmäßig ihre Manuskripte aus."
    ),

    Question(
        categoryId = 10,
        questionText = "Wie viele Bücher umfasst die Narnia-Reihe von C.S. Lewis insgesamt?",
        answerA = "7",
        answerB = "5",
        answerC = "9",
        answerD = "6",
        correctAnswer = 0,
        explanation = "Die Chroniken von Narnia umfassen 7 Bände, beginnend mit 'Der Löwe, die Hexe und der Kleiderschrank' (1950) und endend mit 'Der letzte Kampf' (1956).",
        difficulty = 1,
        funFact = "C.S. Lewis schrieb alle 7 Narnia-Bände in nur 6 Jahren – von 1950 bis 1956."
    ),

    Question(
        categoryId = 10,
        questionText = "Wer schrieb das Fantasy-Epos 'Der Herr der Ringe'?",
        answerA = "C.S. Lewis",
        answerB = "J.R.R. Tolkien",
        answerC = "Terry Pratchett",
        answerD = "George R.R. Martin",
        correctAnswer = 1,
        explanation = "J.R.R. Tolkien veröffentlichte 'Der Herr der Ringe' in drei Bänden zwischen 1954 und 1955. Das Werk gilt als Begründer der modernen Fantasy-Literatur.",
        difficulty = 1,
        funFact = "Tolkien erfand für 'Der Herr der Ringe' eigene Sprachen, darunter Quenya und Sindarin, vollständige Elvish-Sprachen mit eigener Grammatik."
    ),

    Question(
        categoryId = 10,
        questionText = "Wie heißt der dunkle Herrscher in Tolkiens 'Der Herr der Ringe'?",
        answerA = "Sauron",
        answerB = "Saruman",
        answerC = "Balrog",
        answerD = "Morgoth",
        correctAnswer = 0,
        explanation = "Sauron ist der dunkle Herrscher in 'Der Herr der Ringe', der den Einen Ring erschuf, um alle anderen Machtringe zu beherrschen.",
        difficulty = 1,
        funFact = "Tolkien entwickelte die Welt von Mittelerde ursprünglich als Mythologie für England – sein Land hatte keine eigene epische Mythologie wie die Nordischen."
    ),

    Question(
        categoryId = 10,
        questionText = "Suzanne Collins schrieb 'Die Tribute von Panem'. Wie heißt die Protagonistin?",
        answerA = "Tris Prior",
        answerB = "Bella Swan",
        answerC = "Clary Fray",
        answerD = "Katniss Everdeen",
        correctAnswer = 3,
        explanation = "Katniss Everdeen ist die Heldin der Tribute-von-Panem-Trilogie. Sie stammt aus Distrikt 12 und wird zur Symbolfigur des Widerstands gegen das totalitäre Capitol.",
        difficulty = 1,
        funFact = "Suzanne Collins soll die Idee zu den Hungerspielen beim Zappen zwischen Reality-TV und Kriegsberichterstattung im Fernsehen gehabt haben."
    ),

    Question(
        categoryId = 10,
        questionText = "Wie viele Bücher umfasst die Originaltrilogie 'Die Tribute von Panem'?",
        answerA = "3",
        answerB = "4",
        answerC = "2",
        answerD = "5",
        correctAnswer = 0,
        explanation = "Die originale Tribute-von-Panem-Trilogie besteht aus 3 Bänden: 'Die Tribute von Panem', 'Gefährliche Liebe' und 'Flammender Zorn' (2008–2010).",
        difficulty = 1,
        funFact = "2020 erschien mit 'Ballade der Singvögel und Schlangen' ein Prequel – 10 Jahre nach dem letzten Originalband."
    ),

    Question(
        categoryId = 10,
        questionText = "Rick Riordan schrieb 'Percy Jackson'. Welcher Gott ist Percys Vater?",
        answerA = "Zeus",
        answerB = "Poseidon",
        answerC = "Ares",
        answerD = "Hades",
        correctAnswer = 1,
        explanation = "Percy Jackson ist der Sohn von Poseidon, dem Gott des Meeres. Dies macht ihn zu einem der mächtigsten Halbgötter in Rick Riordans Reihe.",
        difficulty = 1,
        funFact = "Rick Riordan erfand Percy Jackson ursprünglich als Gute-Nacht-Geschichte für seinen Sohn Haley, der an einer Lese-Rechtschreib-Schwäche leidet."
    ),

    Question(
        categoryId = 10,
        questionText = "Der Film 'Der Herr der Ringe: Die Gefährten' basiert auf dem Roman von wem?",
        answerA = "J.R.R. Tolkien",
        answerB = "C.S. Lewis",
        answerC = "George R.R. Martin",
        answerD = "Terry Brooks",
        correctAnswer = 0,
        explanation = "Die Filmtrilogie 'Der Herr der Ringe' von Peter Jackson basiert auf J.R.R. Tolkiens gleichnamigen Romanen (1954–1955).",
        difficulty = 1,
        funFact = "Peter Jacksons Verfilmung gewann 17 Oscars – allein 'Die Rückkehr des Königs' gewann alle 11 Kategorien, für die es nominiert wurde."
    ),

    Question(
        categoryId = 10,
        questionText = "Der Film 'Schindlers Liste' basiert auf einem Buch. Wie lautet der Originaltitel?",
        answerA = "Schindlers Ark",
        answerB = "Schindlers List",
        answerC = "The Last Train",
        answerD = "One Thousand and One Nights",
        correctAnswer = 0,
        explanation = "Steven Spielbergs Film 'Schindlers Liste' (1993) basiert auf Thomas Keneallys Roman 'Schindlers Ark' (1982), der auf wahren Begebenheiten beruht.",
        difficulty = 1,
        funFact = "Thomas Keneally traf den echten Oskar Schindler nie – er schrieb das Buch nach Gesprächen mit geretteten Juden."
    ),

    Question(
        categoryId = 10,
        questionText = "Auf welchem Roman von Mario Puzo basiert der Film 'Der Pate'?",
        answerA = "Ein Mann aus Ehre",
        answerB = "Der Pate",
        answerC = "Die Sicilianer",
        answerD = "Omerta",
        correctAnswer = 1,
        explanation = "Francis Ford Coppolas 'Der Pate' (1972) basiert auf Mario Puzos gleichnamigem Roman von 1969, der die Geschichte der Mafia-Familie Corleone erzählt.",
        difficulty = 1,
        funFact = "Mario Puzo war so verschuldet, dass er 'Der Pate' schrieb, um Geld zu verdienen – er hatte keine hohen literarischen Ambitionen damit."
    ),

    Question(
        categoryId = 10,
        questionText = "Sebastian Fitzek ist bekannt für welches Literaturgenre?",
        answerA = "Historische Romane",
        answerB = "Liebesromane",
        answerC = "Science-Fiction",
        answerD = "Psychothriller",
        correctAnswer = 3,
        explanation = "Sebastian Fitzek ist Deutschlands erfolgreichster Psychothriller-Autor. Seine Romane wie 'Die Therapie', 'Das Kind' und 'Der Insasse' erreichen regelmäßig die Spitze der Bestsellerlisten.",
        difficulty = 1,
        funFact = "Sebastian Fitzek arbeitet auch als Fernsehmoderator und Journalist – er ist also auch außerhalb der Bücher in der Öffentlichkeit präsent."
    ),

    Question(
        categoryId = 10,
        questionText = "Charlotte Link ist eine der erfolgreichsten deutschen Autorinnen. Für welches Genre ist sie bekannt?",
        answerA = "Fantasy-Romane",
        answerB = "Kriminalromane und Thriller",
        answerC = "Biografien",
        answerD = "Historische Liebesromane",
        correctAnswer = 1,
        explanation = "Charlotte Link ist vor allem für ihre Kriminalromane und Thriller bekannt, darunter 'Die Stunde der Erben', 'Die Rosenzüchterin' und die Kate-Linville-Reihe.",
        difficulty = 1,
        funFact = "Charlotte Link ist seit den 1990er Jahren auf den deutschen Bestsellerlisten vertreten – eine außergewöhnlich lange Karriere."
    ),

    Question(
        categoryId = 10,
        questionText = "Für welche weltberühmte Buchreihe ist die deutsche Autorin Cornelia Funke bekannt?",
        answerA = "Die Schattenjäger-Reihe",
        answerB = "Die Eragon-Reihe",
        answerC = "Die Tintenwelt-Trilogie",
        answerD = "Die Drachenzähmer-Reihe",
        correctAnswer = 2,
        explanation = "Cornelia Funke ist vor allem für die Tintenwelt-Trilogie bekannt (Tintenherz, Tintenblut, Tintentod), in der Figuren aus Büchern lebendig werden.",
        difficulty = 1,
        funFact = "Cornelia Funke lebt seit 2005 in Malibu, Kalifornien, und arbeitet eng mit Hollywood zusammen."
    ),

    Question(
        categoryId = 10,
        questionText = "Wer schrieb den russischen Klassiker 'Krieg und Frieden'?",
        answerA = "Leo Tolstoi",
        answerB = "Fjodor Dostojewski",
        answerC = "Anton Tschechow",
        answerD = "Maxim Gorki",
        correctAnswer = 0,
        explanation = "Leo Tolstoi schrieb 'Krieg und Frieden' zwischen 1865 und 1869. Das Epos schildert die napoleonische Invasion Russlands und gilt als eines der größten Werke der Weltliteratur.",
        difficulty = 1,
        funFact = "Krieg und Frieden hat über 580 Figuren – Tolstoi arbeitete 7 Jahre daran und schrieb das Buch achtmal komplett um."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist das zentrale Thema in Dostojewskis 'Schuld und Sühne'?",
        answerA = "Ein Student begeht einen Mord und kämpft mit seinem Gewissen",
        answerB = "Ein Adeliger verliebt sich in eine Bäuerin",
        answerC = "Ein Soldat kehrt aus dem Krieg zurück",
        answerD = "Ein Kaufmann verliert sein Vermögen durch Spielsucht",
        correctAnswer = 0,
        explanation = "In 'Schuld und Sühne' (1866) begeht der Student Raskolnikow einen Mord aus philosophischen Überzeugungen und leidet danach unter extremen Gewissensbissen.",
        difficulty = 1,
        funFact = "Dostojewski schrieb 'Schuld und Sühne' unter extremem Zeitdruck, um Schulden bei einem Verleger zu begleichen – und vollendete es trotzdem meisterhaft."
    ),

    Question(
        categoryId = 10,
        questionText = "F. Scott Fitzgeralds 'Der große Gatsby' spielt in welchem Jahrzehnt?",
        answerA = "1930er Jahre",
        answerB = "1910er Jahre",
        answerC = "1920er Jahre",
        answerD = "1940er Jahre",
        correctAnswer = 2,
        explanation = "'Der große Gatsby' (1925) spielt in den Roaring Twenties und schildert Reichtum, Dekadenz und verlorene Träume im Amerika der 1920er Jahre.",
        difficulty = 1,
        funFact = "Fitzgerald soll für Jay Gatsby einen realen Bootlegger aus Long Island als Vorbild gehabt haben."
    ),

    Question(
        categoryId = 10,
        questionText = "Für welches Werk erhielt Harper Lee den Pulitzer-Preis?",
        answerA = "Geh, stelle einen Wächter",
        answerB = "Essays über Rassismus im Süden",
        answerC = "Maycomb: Eine Kleinstadt",
        answerD = "Wer die Nachtigall stört",
        correctAnswer = 3,
        explanation = "Harper Lee erhielt 1961 den Pulitzer-Preis für 'Wer die Nachtigall stört' (To Kill a Mockingbird). Der Roman behandelt Rassismus und Ungerechtigkeit im amerikanischen Süden.",
        difficulty = 1,
        funFact = "Harper Lee veröffentlichte 55 Jahre lang kein zweites Buch – bis 2015 'Geh, stelle einen Wächter' erschien."
    ),

    Question(
        categoryId = 10,
        questionText = "Wer ist die junge Erzählerin in Harper Lees 'Wer die Nachtigall stört'?",
        answerA = "Atticus Finch",
        answerB = "Boo Radley",
        answerC = "Tom Robinson",
        answerD = "Scout Finch",
        correctAnswer = 3,
        explanation = "Scout Finch (Jean Louise Finch) ist die junge Erzählerin des Romans. Als Tochter des Anwalts Atticus Finch erlebt sie als Kind die Ungerechtigkeit rassistischer Gerichtsprozesse.",
        difficulty = 1,
        funFact = "Harper Lee soll die Figur der Scout nach ihrer eigenen Kindheit in Alabama modelliert haben."
    ),

    Question(
        categoryId = 10,
        questionText = "Welches Buch gilt als das meistverkaufte Buch nach der Bibel?",
        answerA = "Harry Potter und der Stein der Weisen",
        answerB = "Don Quijote von Miguel de Cervantes",
        answerC = "Der Herr der Ringe",
        answerD = "Das Kapital von Karl Marx",
        correctAnswer = 1,
        explanation = "Don Quijote von Miguel de Cervantes (1605/1615) wird oft als das meistverkaufte Buch nach der Bibel bezeichnet, mit über 500 Millionen verkauften Exemplaren weltweit.",
        difficulty = 1,
        funFact = "Don Quijote gilt als erster moderner Roman der Weltliteratur überhaupt."
    ),

    Question(
        categoryId = 10,
        questionText = "Marcel Prousts 'Auf der Suche nach der verlorenen Zeit' gilt als welcher Literaturrekord?",
        answerA = "Kürzester veröffentlichter Roman",
        answerB = "Einer der längsten Romane der Weltliteratur",
        answerC = "Ältester erhaltener Roman",
        answerD = "Erster ins Deutsche übersetzter Roman",
        correctAnswer = 1,
        explanation = "Marcel Prousts 'Auf der Suche nach der verlorenen Zeit' (1913–1927) gilt mit rund 1,5 Millionen Wörtern als einer der längsten Romane der Weltliteratur.",
        difficulty = 1,
        funFact = "Proust schrieb die meisten seiner Bände liegend im Bett – er litt an schwerer Asthma und lebte in einem korkbeschichteten Zimmer."
    ),

    Question(
        categoryId = 10,
        questionText = "James Joyce ist bekannt für welches monumentale Werk der englischsprachigen Literatur?",
        answerA = "Ulysses",
        answerB = "1984",
        answerC = "Brave New World",
        answerD = "The Great Gatsby",
        correctAnswer = 0,
        explanation = "James Joyce gilt als einer der bedeutendsten englischsprachigen Autoren des 20. Jahrhunderts, bekannt vor allem für 'Ulysses' und 'Dubliners'.",
        difficulty = 1,
        funFact = "James Joyce soll an 'Ulysses' 17 Jahre lang gearbeitet haben und dabei fast erblindet."
    ),

    Question(
        categoryId = 10,
        questionText = "In welchem Jahr erschien der erste Harry-Potter-Band?",
        answerA = "1999",
        answerB = "1995",
        answerC = "2000",
        answerD = "1997",
        correctAnswer = 3,
        explanation = "Harry Potter und der Stein der Weisen erschien am 26. Juni 1997 beim britischen Verlag Bloomsbury. Es war J.K. Rowlings Debütroman.",
        difficulty = 1,
        funFact = "Der erste Harry-Potter-Band wurde von 12 Verlagen abgelehnt, bevor Bloomsbury ihn annahm – und nur weil die Tochter des Verlegers das Manuskript mochte."
    ),

    Question(
        categoryId = 10,
        questionText = "In Dan Browns 'Illuminati' plant welche Gruppe einen Terroranschlag auf den Vatikan?",
        answerA = "Die Freimaurer",
        answerB = "Die Opus Dei",
        answerC = "Die Illuminati",
        answerD = "Die CIA",
        correctAnswer = 2,
        explanation = "In 'Illuminati' (Angels & Demons, 2000) kämpft Robert Langdon gegen die geheime Gesellschaft der Illuminati, die einen Terroranschlag auf den Vatikan plant.",
        difficulty = 1,
        funFact = "Dan Brown recherchierte für 'Illuminati' in den Vatikanischen Archiven – einige Einblicke erhielt er durch besondere Genehmigungen."
    ),

    Question(
        categoryId = 10,
        questionText = "Leo Tolstoi schrieb neben 'Krieg und Frieden' noch welchen anderen weltbekannten Roman?",
        answerA = "Die Brüder Karamasow",
        answerB = "Anna Karenina",
        answerC = "Schuld und Sühne",
        answerD = "Der Idiot",
        correctAnswer = 1,
        explanation = "Leo Tolstoi schrieb sowohl 'Anna Karenina' (1877) als auch 'Krieg und Frieden' (1869) – beide gelten als Meisterwerke der Weltliteratur.",
        difficulty = 1,
        funFact = "Ironischerweise erhielt Tolstoi selbst nie den Nobelpreis – obwohl er mehrfach nominiert wurde, lehnten ihn die Juroren wegen seiner anarchistischen Ansichten ab."
    ),

    Question(
        categoryId = 10,
        questionText = "Wie heißt der Drache in Cornelia Funkes Kinderbuch 'Drachenreiter'?",
        answerA = "Feuerfrank",
        answerB = "Gluthaar",
        answerC = "Silberschuppe",
        answerD = "Firedrake",
        correctAnswer = 3,
        explanation = "In 'Drachenreiter' (1994) heißt der Drache Firedrake. Zusammen mit dem Waisenjungen Ben macht er sich auf die Suche nach dem Saum des Himmels.",
        difficulty = 1,
        funFact = "Cornelia Funkes 'Drachenreiter' war ihr erstes international erfolgreiches Buch – es wurde in über 40 Sprachen übersetzt."
    ),

    Question(
        categoryId = 10,
        questionText = "Auf welchem Roman von Noah Gordon basiert der Kinofilm 'Der Medicus' (2013)?",
        answerA = "Der Arzt",
        answerB = "Der Medicus",
        answerC = "Das Haus der Schmerzen",
        answerD = "Der Chirurg",
        correctAnswer = 1,
        explanation = "Der Film 'Der Medicus' (2013) basiert auf Noah Gordons Roman 'Der Medicus' (The Physician, 1986), der die Geschichte von Rob Cole im Mittelalter erzählt.",
        difficulty = 1,
        funFact = "'Der Medicus' von Noah Gordon blieb jahrelang auf der Spiegel-Bestsellerliste – ein Rekord für einen Roman auf Deutsch."
    ),

    Question(
        categoryId = 10,
        questionText = "Welcher schwedische Autor schrieb die Krimireihe um Kommissar Wallander?",
        answerA = "Stieg Larsson",
        answerB = "Jo Nesbø",
        answerC = "Camilla Läckberg",
        answerD = "Henning Mankell",
        correctAnswer = 3,
        explanation = "Kurt Wallander ist der Protagonist der schwedischen Krimireihe von Henning Mankell. Die Bücher wurden in über 40 Sprachen übersetzt und weltweit millionenfach verkauft.",
        difficulty = 1,
        funFact = "Henning Mankell verbrachte jedes Jahr mehrere Monate in Mosambik und engagierte sich stark gegen Aids in Afrika."
    ),

    Question(
        categoryId = 10,
        questionText = "Das älteste bekannte schriftliche Erzählwerk der Menschheit ist das Gilgamesch-Epos. Woher stammt es?",
        answerA = "Ägypten",
        answerB = "Mesopotamien (heutiger Irak)",
        answerC = "Griechenland",
        answerD = "Indien",
        correctAnswer = 1,
        explanation = "Das Gilgamesch-Epos stammt aus Mesopotamien (dem heutigen Irak) und ist rund 4.000 Jahre alt. Es gilt als ältestes erhaltenes literarisches Werk der Menschheit.",
        difficulty = 1,
        funFact = "Das Gilgamesch-Epos enthält eine Sintflut-Geschichte, die der biblischen Geschichte von Noah sehr ähnelt – beide könnten auf dieselbe reale Flutkatastrophe zurückgehen."
    ),

    Question(
        categoryId = 10,
        questionText = "Unter welchem Pseudonym veröffentlichte J.K. Rowling ihre Krimireihe um Detektiv Cormoran Strike?",
        answerA = "Ellis Peters",
        answerB = "James Harriot",
        answerC = "Richard Bachman",
        answerD = "Robert Galbraith",
        correctAnswer = 3,
        explanation = "J.K. Rowling veröffentlichte ihre Kriminalromane um den Detektiv Cormoran Strike zunächst unter dem Pseudonym Robert Galbraith.",
        difficulty = 1,
        funFact = "Das Pseudonym flog auf, weil Rowlings Anwalt einem Freund gegenüber erwähnte, dass Galbraith eigentlich Rowling sei – dieser informierte daraufhin eine Journalistin."
    ),

    Question(
        categoryId = 10,
        questionText = "Stephen King veröffentlichte mehrere Bücher unter welchem Pseudonym?",
        answerA = "Richard Bachman",
        answerB = "Dean Koontz",
        answerC = "Peter Straub",
        answerD = "Clive Barker",
        correctAnswer = 0,
        explanation = "Stephen King veröffentlichte in den 1970er und 80er Jahren mehrere Bücher als 'Richard Bachman', um zu testen, ob seine Bücher auch ohne seinen berühmten Namen erfolgreich wären.",
        difficulty = 1,
        funFact = "Das Pseudonym 'Richard Bachman' flog auf, weil ein Fan in einem Verlagskatalog Gemeinsamkeiten im Schreibstil entdeckte und King direkt konfrontierte."
    ),

    Question(
        categoryId = 10,
        questionText = "In wie viele Sprachen wurden die Harry-Potter-Bücher weltweit übersetzt?",
        answerA = "über 85",
        answerB = "über 25",
        answerC = "über 50",
        answerD = "über 35",
        correctAnswer = 0,
        explanation = "Die Harry-Potter-Bücher wurden in über 85 Sprachen übersetzt, darunter auch Latein und Altgriechisch – was sie zu den am meisten übersetzten Buchreihen der Geschichte macht.",
        difficulty = 1,
        funFact = "Es gibt sogar eine Harry-Potter-Übersetzung ins Ukrainische Krimtatarische, eine Sprache mit nur wenigen zehntausend Sprechern."
    ),

    Question(
        categoryId = 10,
        questionText = "Wer schrieb den Bestseller 'Das Parfum – Die Geschichte eines Mörders'?",
        answerA = "Martin Walser",
        answerB = "Bernhard Schlink",
        answerC = "Patrick Süskind",
        answerD = "Christoph Ransmayr",
        correctAnswer = 2,
        explanation = "Patrick Süskind schrieb 'Das Parfum' (1985). Der Roman über den Mörder Jean-Baptiste Grenouille wurde einer der meistverkauften deutschen Romane des 20. Jahrhunderts.",
        difficulty = 1,
        funFact = "Patrick Süskind lehnte jahrelang alle Verfilmungsanfragen ab – erst 2006 gab er nach, und Tom Tykwer verfilmte den Roman mit Ben Whishaw in der Hauptrolle."
    ),

    Question(
        categoryId = 10,
        questionText = "Bernhard Schlinks 'Der Vorleser' verarbeitet welche historische Epoche?",
        answerA = "Den Holocaust im Zweiten Weltkrieg",
        answerB = "Die Weimarer Republik",
        answerC = "Den Ersten Weltkrieg",
        answerD = "Die DDR",
        correctAnswer = 0,
        explanation = "In 'Der Vorleser' (1995) verarbeitet Bernhard Schlink das Thema der deutschen Schuld im Holocaust – ein KZ-Aufseher und ein junger Mann verbindet eine komplexe Liebesgeschichte.",
        difficulty = 1,
        funFact = "Der Vorleser war das erste deutsche Buch, das die Nummer-1-Position auf der amerikanischen Bestsellerliste der New York Times erreichte."
    ),

    Question(
        categoryId = 10,
        questionText = "Die Buchreihe 'Percy Jackson' basiert auf welcher antiken Mythologie?",
        answerA = "Nordische Mythologie",
        answerB = "Ägyptische Mythologie",
        answerC = "Römische Mythologie",
        answerD = "Griechische Mythologie",
        correctAnswer = 3,
        explanation = "Rick Riordans 'Percy Jackson'-Reihe basiert auf der griechischen Mythologie. Percy ist der Sohn des Meeresgottes Poseidon und kämpft gegen Titanen und Monster aus der griechischen Sagenwelt.",
        difficulty = 1,
        funFact = "Rick Riordan schrieb nach Percy Jackson weitere Serien basierend auf ägyptischer ('Kane Chronicles'), nordischer ('Magnus Chase') und anderer Mythologie."
    ),

    Question(
        categoryId = 10,
        questionText = "Wer schrieb 'Der kleine Prinz', eines der meistübersetzten Bücher der Welt?",
        answerA = "Jules Verne",
        answerB = "Victor Hugo",
        answerC = "Albert Camus",
        answerD = "Antoine de Saint-Exupéry",
        correctAnswer = 3,
        explanation = "Antoine de Saint-Exupéry schrieb und illustrierte 'Der kleine Prinz' (1943). Das Buch wurde in über 500 Sprachen übersetzt.",
        difficulty = 1,
        funFact = "Antoine de Saint-Exupéry war Pilot und verschwand 1944 auf einem Aufklärungsflug über dem Mittelmeer – sein Verschwinden blieb jahrzehntelang ein Rätsel."
    ),

    Question(
        categoryId = 10,
        questionText = "Wer erhielt den Literaturnobelpreis 2015 für Werke über die Nachwirkungen der Sowjetunion?",
        answerA = "Haruki Murakami",
        answerB = "Patrick Modiano",
        answerC = "Alice Munro",
        answerD = "Svetlana Alexijewitsch",
        correctAnswer = 3,
        explanation = "Svetlana Alexijewitsch erhielt 2015 den Nobelpreis für Literatur für ihre dokumentarischen Werke über die Nachwirkungen der Sowjetunion, darunter 'Stimmen aus Tschernobyl'.",
        difficulty = 1,
        funFact = "Svetlana Alexijewitsch ist Weißrussin und schreibt auf Russisch – ihr bekanntestes Werk dokumentiert Aussagen von Tschernobyl-Überlebenden."
    ),

    Question(
        categoryId = 10,
        questionText = "Georges Simenon ist vor allem bekannt für eine Krimireihe um welchen Kommissar?",
        answerA = "Wallander",
        answerB = "Maigret",
        answerC = "Montalbano",
        answerD = "Morse",
        correctAnswer = 1,
        explanation = "Georges Simenon ist vor allem bekannt für seine Krimireihe um Kommissar Maigret, die über 75 Romane und Dutzende Kurzgeschichten umfasst.",
        difficulty = 1,
        funFact = "Georges Simenon schrieb über 200 Romane und Dutzende Kurzgeschichten – er galt als einer der produktivsten Autoren des 20. Jahrhunderts."
    ),

    Question(
        categoryId = 10,
        questionText = "In welchem Jahr erschien Sebastian Fitzeks Debütroman 'Die Therapie'?",
        answerA = "2006",
        answerB = "2010",
        answerC = "2001",
        answerD = "2015",
        correctAnswer = 0,
        explanation = "'Die Therapie' erschien 2006 und war Sebastian Fitzeks Debütroman. Er wurde sofort ein Bestseller und begründete seinen Ruf als führender deutscher Psychothriller-Autor.",
        difficulty = 1,
        funFact = "Fitzek begann mit dem Schreiben von Thrillern, weil er nachts nicht schlafen konnte und seine eigenen Ängste literarisch verarbeiten wollte."
    ),

    Question(
        categoryId = 10,
        questionText = "Der Pulitzer Prize ist ein renommierter Literaturpreis. Wer ist berechtigt, ihn zu erhalten?",
        answerA = "Nur britische Staatsangehörige",
        answerB = "Nur US-amerikanische Bürger und Journalisten",
        answerC = "Alle englischsprachigen Autoren weltweit",
        answerD = "Nur Autoren aus Nordamerika und Europa",
        correctAnswer = 1,
        explanation = "Der Pulitzer Prize wird ausschließlich an US-amerikanische Journalisten, Autoren und Komponisten vergeben – er ist einer der angesehensten Literaturpreise in den USA.",
        difficulty = 1,
        funFact = "Der Pulitzer Prize wurde 1917 durch ein Erbe des Zeitungsverlegers Joseph Pulitzer ins Leben gerufen."
    ),

    Question(
        categoryId = 10,
        questionText = "Welcher Verlag veröffentlichte 1997 den ersten Harry-Potter-Band in Großbritannien?",
        answerA = "Penguin",
        answerB = "Random House",
        answerC = "Bloomsbury",
        answerD = "HarperCollins",
        correctAnswer = 2,
        explanation = "Bloomsbury Publishing veröffentlichte 1997 'Harry Potter und der Stein der Weisen'. Der Verlag war damals noch relativ klein und wurde durch Harry Potter weltberühmt.",
        difficulty = 1,
        funFact = "Bloomsbury erhielt das Manuskript nur, weil die achtjährige Tochter des Verlagsleiters die ersten Kapitel las und mehr wollte."
    ),

    Question(
        categoryId = 10,
        questionText = "Welche Figur erzählt die Geschichte in 'Der große Gatsby' von F. Scott Fitzgerald?",
        answerA = "Jay Gatsby selbst",
        answerB = "Tom Buchanan",
        answerC = "Daisy Buchanan",
        answerD = "Nick Carraway",
        correctAnswer = 3,
        explanation = "Nick Carraway, Gatsbys Nachbar und entfernter Verwandter von Daisy, ist der Erzähler des Romans 'Der große Gatsby'.",
        difficulty = 1,
        funFact = "F. Scott Fitzgerald erlebte den Welterfolg seines Romans nicht – 'Der große Gatsby' wurde erst posthum als Meisterwerk anerkannt."
    ),

    Question(
        categoryId = 10,
        questionText = "Cornelia Funke schrieb 'Tintenherz'. Welche besondere Fähigkeit hat die Hauptfigur Mo?",
        answerA = "Er kann unsichtbar werden",
        answerB = "Er kann in die Zukunft sehen",
        answerC = "Er kann Figuren aus Büchern vorlesen",
        answerD = "Er kann Bücher in Gold verwandeln",
        correctAnswer = 2,
        explanation = "Mo (Mortimer) hat die Gabe, beim Vorlesen Figuren und Gegenstände aus Büchern in die Wirklichkeit zu holen – und gleichzeitig verschwinden echte Menschen in die Bücherwelt.",
        difficulty = 1,
        funFact = "Cornelia Funke ließ sich für 'Tintenherz' von ihrer eigenen Leidenschaft für Bücher inspirieren – die Idee, Figuren könnten aus Büchern herausspringen, faszinierte sie schon als Kind."
    ),

    Question(
        categoryId = 10,
        questionText = "Welcher Autor schrieb 'Sofies Welt', einen philosophischen Roman für Jugendliche?",
        answerA = "Jostein Gaarder",
        answerB = "Per Petterson",
        answerC = "Karl Ove Knausgård",
        answerD = "Erlend Loe",
        correctAnswer = 0,
        explanation = "Der norwegische Autor Jostein Gaarder schrieb 'Sofies Welt' (1991), einen Roman, der die Geschichte der Philosophie in einer Rahmenhandlung für junge Leser erzählt.",
        difficulty = 1,
        funFact = "'Sofies Welt' wurde in über 60 Sprachen übersetzt und verkaufte sich weltweit über 40 Millionen Mal."
    )

)
