package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun literatureQuestionsHard5(): List<Question> = listOf(

    // Question 1 - Bulgakov
    Question(
        categoryId = 10,
        questionText = "In welcher Stadt spielt der Gegenwartsteil von Bulgakovs 'Der Meister und Margarita'?",
        answerA = "Moskau",
        answerB = "Kiew",
        answerC = "Leningrad",
        answerD = "Odessa",
        correctAnswer = 0
        explanation = "Der Gegenwartsteil von Bulgakovs Roman spielt im Moskau der 1930er-Jahre, wo der Teufel als Professor Woland auftaucht und das Treiben der sowjetischen Gesellschaft satirisch entlarvt. Der historische Teil hingegen spielt im Jerusalem der Antike zur Zeit von Pontius Pilatus.",
        difficulty = 3,
        funFact = "Bulgakov verbrannte 1930 das Manuskript von 'Der Meister und Margarita' aus Angst vor Repressalien, schrieb es aber aus dem Gedächtnis neu."
    ),

    // Question 2 - Bulgakov
    Question(
        categoryId = 10,
        questionText = "Unter welchem Namen erscheint der Teufel in Bulgakovs 'Der Meister und Margarita'?",
        answerA = "Doktor Faust",
        answerB = "Professor Woland",
        answerC = "Graf Mephisto",
        answerD = "Meister Behemoth",
        correctAnswer = 1
        explanation = "Der Teufel erscheint in Bulgakovs Roman als Professor Woland, ein mysteriöser ausländischer Magier. Er besucht Moskau mit seinem skurrilen Gefolge, darunter der riesenhafte schwarze Kater Behemoth und der Vampir Hella.",
        difficulty = 3,
        funFact = "Der Name Woland stammt aus Goethes 'Faust', wo Mephisto sich kurz als 'Junker Voland' bezeichnet — eine Anspielung, die in der Übersetzung oft verloren geht."
    ),

    // Question 3 - Bulgakov
    Question(
        categoryId = 10,
        questionText = "Welcher berühmte Satz gilt als Leitmotiv in Bulgakovs 'Der Meister und Margarita'?",
        answerA = "Schreiben ist leiden, nicht schreiben ist Qual",
        answerB = "Liebe überwindet alle Grenzen",
        answerC = "Manuskripte brennen nicht",
        answerD = "Feigheit ist keine Tugend",
        correctAnswer = 2
        explanation = "Der Satz 'Manuskripte brennen nicht' ist eines der bekanntesten Zitate aus Bulgakovs Roman. Er wird von Woland gesprochen, als er dem Meister sein verbranntes Manuskript unversehrt zurückgibt — ein Sinnbild für die Unzerstörbarkeit echter Kunst.",
        difficulty = 3,
        funFact = "Bulgakov selbst verbrannte sein Manuskript 1930, schrieb es aber neu. Der Roman erschien erst 1966–67 in der Sowjetunion, 26 Jahre nach Bulgakovs Tod."
    ),

    // Question 4 - Bulgakov
    Question(
        categoryId = 10,
        questionText = "Was tut Margarita in Bulgakovs Roman, um den Meister aus der Psychiatrie zu befreien?",
        answerA = "Sie besticht einen Beamten",
        answerB = "Sie verbrennt Wolands Buch",
        answerC = "Sie flieht mit dem Meister nach Jerusalem",
        answerD = "Sie wird Wolands Gastgeberin beim Hexensabbat",
        correctAnswer = 3
        explanation = "Margarita schließt einen Pakt mit dem Teufel Woland und fungiert als nackte fliegende Hexe und Gastgeberin bei seinem jährlichen Satansball. Als Lohn darf sie einen Wunsch äußern und befreit damit den Meister aus der Psychiatrie.",
        difficulty = 3,
        funFact = "Der Satansball in Bulgakovs Roman versammelt legendäre Verbrecher und Sünder der Weltgeschichte — Margarita muss jedem einzeln die Hand geben, was sie körperlich erschöpft."
    ),

    // Question 5 - Umberto Eco
    Question(
        categoryId = 10,
        questionText = "Wer ist der Mörder in Umberto Ecos 'Der Name der Rose'?",
        answerA = "Jorge von Burgos",
        answerB = "Abt Abbon",
        answerC = "Adso von Melk",
        answerD = "Bruder Salvatore",
        correctAnswer = 0
        explanation = "Jorge von Burgos, ein alter blinder Mönch, ist der Mörder. Er hat Aristoteles' verloren geglaubtes zweites Buch der Poetik — über die Komödie — mit Gift präpariert, um zu verhindern, dass das Lachen als legitim angesehen wird. Wer darin liest und die Finger ableckt, stirbt.",
        difficulty = 3,
        funFact = "Die Figur Jorge von Burgos ist eine Anspielung auf den argentinischen Autor Jorge Luis Borges, der ebenfalls blind war und Bibliotheken liebte."
    ),

    // Question 6 - Umberto Eco
    Question(
        categoryId = 10,
        questionText = "Welches antike Werk bildet das Zentrum des Geheimnisses in Ecos 'Der Name der Rose'?",
        answerA = "Platons 'Symposion'",
        answerB = "Aristoteles' zweites Buch der Poetik über die Komödie",
        answerC = "Ovids 'Ars amatoria'",
        answerD = "Homers 'Odyssee'",
        correctAnswer = 1
        explanation = "Im Mittelpunkt des Romans steht das einzige erhaltene Exemplar von Aristoteles' verlorenem zweiten Buch der Poetik, das die Komödie behandelt. Der Mörder Jorge von Burgos vergiftet es, weil er fürchtet, das Lachen werde als gottgegeben legitimiert und die religiöse Ordnung untergraben.",
        difficulty = 3,
        funFact = "Ob Aristoteles wirklich ein zweites Buch der Poetik über die Komödie geschrieben hat, ist unter Gelehrten bis heute umstritten — Eco machte dieses Rätsel zum literarischen Motor seines Romans."
    ),

    // Question 7 - Umberto Eco
    Question(
        categoryId = 10,
        questionText = "Welcher Ermittler löst in Ecos 'Der Name der Rose' den Fall in der Abtei?",
        answerA = "Roger Bacon",
        answerB = "Thomas von Aquin",
        answerC = "William von Baskerville",
        answerD = "Bernhard von Clairvaux",
        correctAnswer = 2
        explanation = "William von Baskerville, ein englischer Franziskanermönch mit scharfem analytischem Verstand, ist der Detektiv des Romans. Sein Name spielt auf Sir Arthur Conan Doyles 'Der Hund von Baskerville' und auf William von Ockham an. Sein junger Novize Adso von Melk begleitet ihn und erzählt die Geschichte.",
        difficulty = 3,
        funFact = "Umberto Ecos Roman 'Der Name der Rose' erschien 1980 und wurde mit dem Premio Strega ausgezeichnet. Er gilt als einer der meistverkauften europäischen Romane des 20. Jahrhunderts."
    ),

    // Question 8 - Milan Kundera
    Question(
        categoryId = 10,
        questionText = "In welcher Stadt spielt der überwiegende Teil von Kunderás 'Die unerträgliche Leichtigkeit des Seins'?",
        answerA = "Wien",
        answerB = "Budapest",
        answerC = "Bratislava",
        answerD = "Prag",
        correctAnswer = 3
        explanation = "Der Roman spielt hauptsächlich in Prag, vor und nach dem Prager Frühling 1968. Die sowjetische Invasion der Tschechoslowakei ist ein zentrales historisches Ereignis, das das Leben der Protagonisten Tomas und Tereza grundlegend verändert.",
        difficulty = 3,
        funFact = "Milan Kundera schrieb den Roman 1982, lebte aber da bereits im französischen Exil. Er hatte 1975 die Tschechoslowakei verlassen und seine tschechische Staatsbürgerschaft verloren."
    ),

    // Question 9 - Milan Kundera
    Question(
        categoryId = 10,
        questionText = "Wie heißt der Hund in Kunderás 'Die unerträgliche Leichtigkeit des Seins'?",
        answerA = "Karenin",
        answerB = "Anna",
        answerC = "Sabina",
        answerD = "Franz",
        correctAnswer = 0
        explanation = "Der Hund heißt Karenin — eine bewusste Anspielung auf Anna Karenins Ehemann in Tolstois Roman. Tereza gibt dem Hund diesen Namen. Karenin wird zum Symbol bedingungsloser Liebe, die im Gegensatz zur komplizierten menschlichen Liebe steht.",
        difficulty = 3,
        funFact = "Kundera nutzt den Hundnamen Karenin als intertextuelle Brücke zu Tolstoi: Wie Karenin in 'Anna Karenina' ist der Hund eine loyale, stabile Konstante in einer Welt des Wandels."
    ),

    // Question 10 - Milan Kundera
    Question(
        categoryId = 10,
        questionText = "Auf welchem philosophischen Konzept Nietzsches basiert Kunderás 'Die unerträgliche Leichtigkeit des Seins' thematisch?",
        answerA = "Der Wille zur Macht",
        answerB = "Die ewige Wiederkehr des Gleichen",
        answerC = "Der Übermensch",
        answerD = "Der apollinische Trieb",
        correctAnswer = 1
        explanation = "Der Roman setzt sich mit Nietzsches Konzept der ewigen Wiederkehr auseinander: Wenn jedes Ereignis unendlich oft wiederholt wird, bekommt es Schwere und Bedeutung. Da das Leben aber nur einmal stattfindet, ist es 'leicht' — doch diese Leichtigkeit ist unerträglich, weil sie keine zweite Chance kennt.",
        difficulty = 3,
        funFact = "Kundera eröffnet den Roman mit einer philosophischen Reflexion über Nietzsche — ein ungewöhnlicher Einstieg für einen Liebesroman, der ihn zum literarisch-philosophischen Hybrid macht."
    ),

    // Question 11 - José Saramago
    Question(
        categoryId = 10,
        questionText = "Wer ist in Saramagos 'Die Stadt der Blinden' die einzige Person, die sehen kann?",
        answerA = "Der erste Blinde",
        answerB = "Der Arzt",
        answerC = "Die Frau des Arztes",
        answerD = "Das Mädchen mit der Brille",
        correctAnswer = 2
        explanation = "In Saramagos Roman erblinden plötzlich alle Menschen bis auf eine: die Frau des Arztes. Sie begleitet ihren Mann freiwillig in die Quarantäne, ohne ihre Sehfähigkeit zu verraten, und wird zur stillen Führerin der Gruppe in einer zerfallenden Gesellschaft.",
        difficulty = 3,
        funFact = "Saramago nannte keinen seiner Figuren im Roman beim Namen — sie heißen nur 'die Frau des Arztes', 'der erste Blinde' usw., um den universellen Charakter der Allegorie zu betonen."
    ),

    // Question 12 - José Saramago
    Question(
        categoryId = 10,
        questionText = "In welchem Jahr erschien Saramagos 'Die Stadt der Blinden' auf Portugiesisch?",
        answerA = "1990",
        answerB = "2001",
        answerC = "1998",
        answerD = "1995",
        correctAnswer = 3
        explanation = "Saramagos 'Ensaio sobre a Cegueira' (deutsch: 'Die Stadt der Blinden') erschien 1995 auf Portugiesisch. Drei Jahre später, 1998, erhielt Saramago den Nobelpreis für Literatur — als erster und bisher einziger portugiesischsprachiger Autor.",
        difficulty = 3,
        funFact = "Der Vatikan kritisierte Saramago als Atheisten und trug dazu bei, dass sein früheres Werk 'Das Evangelium nach Jesus Christus' in Portugal von einem Literaturpreis ausgeschlossen wurde."
    ),

    // Question 13 - Marcel Proust
    Question(
        categoryId = 10,
        questionText = "Wie viele Bände umfasst Prousts Romanzyklus 'Auf der Suche nach der verlorenen Zeit'?",
        answerA = "7",
        answerB = "6",
        answerC = "5",
        answerD = "9",
        correctAnswer = 0
        explanation = "Prousts Romanzyklus besteht aus sieben Bänden, beginnend mit 'In Swanns Welt' (1913) bis hin zu 'Die wiedergefundene Zeit' (1927). Die letzten drei Bände erschienen posthum, da Proust 1922 starb, bevor er die Überarbeitung abschließen konnte.",
        difficulty = 3,
        funFact = "Der Gesamtromanzyklus umfasst über 1,5 Millionen Wörter und gilt damit als einer der längsten Romane der Weltliteratur."
    ),

    // Question 14 - Marcel Proust
    Question(
        categoryId = 10,
        questionText = "Welches Gebäck löst in Prousts Roman die berühmte Erinnerungskaskade aus?",
        answerA = "Ein Croissant",
        answerB = "Eine Madeleine",
        answerC = "Ein Briochestück",
        answerD = "Ein Macaron",
        correctAnswer = 1
        explanation = "Die berühmte 'Madeleine-Episode' in 'In Swanns Welt' zeigt, wie der Erzähler beim Eintauchen einer kleinen muschelförmigen Madeleine in Tee unwillkürlich Kindheitserinnerungen an Combray erlebt. Proust nennt dies 'mémoire involontaire' — unwillkürliche Erinnerung.",
        difficulty = 3,
        funFact = "In einem frühen Entwurf verwendete Proust statt der Madeleine einen Zwieback. Die Madeleine wurde erst in späteren Versionen eingeführt und ist heute das bekannteste literarische Gebäck der Welt."
    ),

    // Question 15 - Marcel Proust
    Question(
        categoryId = 10,
        questionText = "Welcher Charakter in Prousts Romanzyklus erlebt eine obsessive, unglückliche Liebe zu einer Kurtisane namens Odette?",
        answerA = "Baron de Charlus",
        answerB = "Saint-Loup",
        answerC = "Charles Swann",
        answerD = "Der Erzähler Marcel",
        correctAnswer = 2
        explanation = "Charles Swann, ein jüdischer Kunstkenner der Pariser Gesellschaft, verfällt in eine obsessive Liebe zur Kurtisane Odette de Crécy, die er schließlich heiratet. Swanns Eifersucht und Obsession sind ein zentrales Thema des ersten Bandes 'In Swanns Welt'.",
        difficulty = 3,
        funFact = "Swanns berühmter Satz über seine Leidenschaft für Odette — 'Zu denken, dass ich Jahre meines Lebens verschwendet habe, dass ich sterben wollte, dass ich meine größte Liebe für eine Frau empfand, die mir nicht einmal gefiel' — gilt als eine der bittersten Liebeserinnerungen der Literatur."
    ),

    // Question 16 - James Joyce
    Question(
        categoryId = 10,
        questionText = "An welchem Tag des Jahres 1904 spielt James Joyces 'Ulysses' vollständig?",
        answerA = "1. Januar",
        answerB = "21. Oktober",
        answerC = "4. Juli",
        answerD = "16. Juni",
        correctAnswer = 3
        explanation = "Joyces 'Ulysses' spielt vollständig am 16. Juni 1904 in Dublin. Dieser Tag wird heute weltweit als 'Bloomsday' gefeiert, benannt nach dem Protagonisten Leopold Bloom. Das Datum war auch das erste Date zwischen Joyce und seiner späteren Frau Nora Barnacle.",
        difficulty = 3,
        funFact = "Jeden 16. Juni pilgern Tausende von Literaturbegeisterten nach Dublin, gehen Blooms Route nach und lesen laut aus dem Roman vor — ein einzigartiges literarisches Volksfest."
    ),

    // Question 17 - James Joyce
    Question(
        categoryId = 10,
        questionText = "Welcher epische Text dient als strukturelles Vorbild für Joyces 'Ulysses'?",
        answerA = "Homers 'Odyssee'",
        answerB = "Dantes 'Göttliche Komödie'",
        answerC = "Virgils 'Aeneis'",
        answerD = "Miltons 'Das verlorene Paradies'",
        correctAnswer = 0
        explanation = "Joyces 'Ulysses' ist eine Parallelstruktur zu Homers 'Odyssee': Leopold Bloom entspricht Odysseus/Ulysses, Stephen Dedalus dem Telemachos und Molly Bloom der Penelope. Jede Episode des Romans entspricht einer Episode der 'Odyssee'.",
        difficulty = 3,
        funFact = "Joyce schuf für jeden der 18 Episoden ein eigenes stilistisches Experiment — von inneren Monologen über Zeitungsparodien bis zum berühmten letzten Kapitel, das ohne Satzzeichen geschrieben ist."
    ),

    // Question 18 - James Joyce
    Question(
        categoryId = 10,
        questionText = "Welche Figur in Joyces 'Ulysses' entspricht der Homer'schen Penelope?",
        answerA = "Stephen Dedalus",
        answerB = "Molly Bloom",
        answerC = "Buck Mulligan",
        answerD = "Gerty MacDowell",
        correctAnswer = 1
        explanation = "Molly Bloom, Leopolds Ehefrau und Berufssängerin, entspricht der Homer'schen Penelope. Das letzte Kapitel des Romans — der 'Penelope'-Abschnitt — besteht aus Mollys unzensiertem innerem Monolog in einem einzigen langen Satz ohne Satzzeichen.",
        difficulty = 3,
        funFact = "Mollys Monolog endet mit dem berühmten Wort 'yes' — ein bewusster Gegenpol zum Ende des Vorkapitels, das mit 'No' endet, und steht für Lebensejahung trotz allem."
    ),

    // Question 19 - Chinua Achebe
    Question(
        categoryId = 10,
        questionText = "Wie heißt der Protagonist in Chinua Achebes 'Alles zerfällt'?",
        answerA = "Obierika",
        answerB = "Unoka",
        answerC = "Okonkwo",
        answerD = "Nwoye",
        correctAnswer = 2
        explanation = "Okonkwo ist der Protagonist von Achebes Roman. Er ist ein geachteter Krieger und Dorfführer der Igbo, der alles tut, um nicht wie sein als feige geltender Vater Unoka zu werden. Sein tragisches Schicksal wird durch die koloniale Invasion der Briten besiegelt.",
        difficulty = 3,
        funFact = "Achebes Romantitel 'Things Fall Apart' stammt aus W.B. Yeats' Gedicht 'The Second Coming' — 'Things fall apart; the centre cannot hold' — ein Hinweis auf den Zerfall der traditionellen Igbo-Ordnung."
    ),

    // Question 20 - Chinua Achebe
    Question(
        categoryId = 10,
        questionText = "In welchem Jahr erschien Chinua Achebes 'Alles zerfällt'?",
        answerA = "1950",
        answerB = "1954",
        answerC = "1963",
        answerD = "1958",
        correctAnswer = 3
        explanation = "Achebes Debütroman 'Things Fall Apart' erschien 1958, ein Jahr vor der nigerianischen Unabhängigkeit. Das Buch gilt als Begründungswerk der modernen afrikanischen Literatur in englischer Sprache und war maßgeblich für die nigerianische Literaturrenaissance der 1960er-Jahre.",
        difficulty = 3,
        funFact = "Achebe schrieb den Roman als bewusste Antwort auf Joseph Conrads 'Herz der Finsternis', das er als rassistisches Bild Afrikas kritisierte. Er wollte Afrikas eigene Stimme und Perspektive zeigen."
    ),

    // Question 21 - Nikolai Gogol
    Question(
        categoryId = 10,
        questionText = "Was kauft der Protagonist Tschitschikow in Gogols 'Tote Seelen'?",
        answerA = "Verstorbene Leibeigene, die noch als lebend geführt werden",
        answerB = "Verfallene Gutshöfe",
        answerC = "Gestohlene Gemälde aus Adelshäusern",
        answerD = "Schulden von bankrotten Adligen",
        correctAnswer = 0
        explanation = "Tschitschikow nutzt eine russische Steuerlücke: Leibeigene, die zwischen zwei Volkszählungen gestorben sind, müssen weiter versteuert werden. Er kauft diese 'toten Seelen' billig auf, um sie als Sicherheit für einen Kredit zu verpfänden und sich so zu bereichern.",
        difficulty = 3,
        funFact = "Gogol plante 'Tote Seelen' als dreiteiliges Werk nach dem Vorbild von Dantes Göttlicher Komödie. Er vollendete nur den ersten Band und verbrannte kurz vor seinem Tod 1852 den zweiten Teil."
    ),

    // Question 22 - Nikolai Gogol
    Question(
        categoryId = 10,
        questionText = "Worum geht es in Gogols Kurzgeschichte 'Der Mantel'?",
        answerA = "Ein Beamter kauft einen neuen Mantel und wird ermordet",
        answerB = "Ein armer Schreiber opfert alles für einen neuen Mantel und stirbt nach dessen Diebstahl",
        answerC = "Ein Bürgermeister verliert seinen Mantel und deckt damit Korruption auf",
        answerD = "Ein Soldat findet einen Mantel, der ihm Unsichtbarkeit verleiht",
        correctAnswer = 1
        explanation = "In 'Der Mantel' (1842) spart der arme Beamte Akaki Akakiewitsch jahrelang für einen neuen Mantel, wird ihn gleich in der ersten Nacht beraubt und stirbt kurz darauf an Kummer. Dostojewski kommentierte: 'Wir sind alle Gogols Mantel entstiegen' — ein Hinweis auf den sozialkritischen Einfluss dieser Geschichte.",
        difficulty = 3,
        funFact = "Gogols 'Der Mantel' gilt als Begründungstext des russischen Realismus und des literarischen Mitgefühls für den 'kleinen Mann'. Die Phrase 'Wir alle kamen aus Gogols Mantel hervor' (Dostojewski zugeschrieben) ist eines der meistzitierten Literaturzitate."
    ),

    // Question 23 - Nikolai Gogol
    Question(
        categoryId = 10,
        questionText = "In Gogols Komödie 'Der Revisor' halten Provinzbeamte einen mittellosen Reisenden für einen gefürchteten Geheiminspektor. Wie heißt dieser Reisende?",
        answerA = "Tschitschikow",
        answerB = "Oblomow",
        answerC = "Chlestakow",
        answerD = "Basarow",
        correctAnswer = 2
        explanation = "Chlestakow ist der mittelslose junge Beamte, der aus Versehen für den erwarteten Geheiminspektor gehalten wird. Die korrupten Provinzbeamten besterchen ihn und erzählen ihm Günstlingsgeschichten, bis er mit dem Geld flieht und der echte Inspektor ankündigt wird.",
        difficulty = 3,
        funFact = "Gogol ließ 'Der Revisor' 1836 von Zar Nikolaus I. persönlich absegnen, der nach der Uraufführung lachend sagte: 'Alle haben ihr Teil bekommen, und ich am meisten!' — ohne die Satire auf seine Bürokratie zu bemerken."
    ),

    // Question 24 - Jorge Luis Borges
    Question(
        categoryId = 10,
        questionText = "Welche Erzählung von Jorge Luis Borges handelt von einem Schriftsteller, der 'Don Quixote' Wort für Wort neu schreibt, obwohl das Resultat identisch mit Cervantes' Text ist?",
        answerA = "Die Bibliothek von Babel",
        answerB = "Der Garten der sich verzweigenden Pfade",
        answerC = "Tlön, Uqbar, Orbis Tertius",
        answerD = "Pierre Menard, Autor des Quixote",
        correctAnswer = 3
        explanation = "In 'Pierre Menard, Autor des Quixote' schreibt der französische Symbolist Menard denselben Text wie Cervantes, doch Borges argumentiert, der identische Wortlaut bedeute im 20. Jahrhundert etwas völlig anderes. Diese Erzählung ist ein Meisterwerk über Autorschaft, Bedeutung und Intertextualität.",
        difficulty = 3,
        funFact = "Borges' Erzählung ist selbst als gefälschte Literaturkritik geschrieben — eine Form, die er perfektionierte und die die Grenze zwischen Literaturkritik und Fiktion aufhebt."
    ),

    // Question 25 - Jorge Luis Borges
    Question(
        categoryId = 10,
        questionText = "Welches Werk von Borges beschreibt eine fiktive Bibliothek, die alle möglichen Bücher enthält?",
        answerA = "Die Bibliothek von Babel",
        answerB = "Der Garten der sich verzweigenden Pfade",
        answerC = "Das Aleph",
        answerD = "Tlön, Uqbar, Orbis Tertius",
        correctAnswer = 0
        explanation = "In 'Die Bibliothek von Babel' (1941) beschreibt Borges eine unendliche Bibliothek, die alle möglichen 410-seitigen Bücher enthält — also alle denkbaren Kombinationen von Buchstaben. Irgendwo darin findet sich die absolute Wahrheit, aber auch jeder erdenkliche Unsinn.",
        difficulty = 3,
        funFact = "Borges arbeitete tatsächlich jahrelang als Bibliothekar in Buenos Aires. Als er später erblindet, wurde er ironischerweise Nationalbibliotheksdirektor Argentiniens — er konnte die Bücher nicht mehr lesen."
    ),

    // Question 26 - Italo Calvino
    Question(
        categoryId = 10,
        questionText = "In Italo Calvinos 'Wenn ein Reisender in einer Winternacht' wird der Leser direkt als 'du' angesprochen. Was ist der Grundkonflikt des Romans?",
        answerA = "Ein Detektiv sucht einen gestohlenen Roman",
        answerB = "Ein Leser findet immer wieder nur Anfänge von Büchern, die abrupt enden",
        answerC = "Ein Schriftsteller verliert sein Gedächtnis und muss seine Werke neu schreiben",
        answerD = "Ein Verleger fälscht Bücher berühmter Autoren",
        correctAnswer = 1
        explanation = "In Calvinos metafiktionalem Roman von 1979 kauft 'der Leser' (du) ein Buch, das abbricht. Er sucht die Fortsetzung, findet aber stets wieder nur Anfänge fremder Bücher. Der Roman besteht aus zehn Romanfragmenten in verschiedenen Stilen, umrahmt von einer Rahmenhandlung über das Lesen selbst.",
        difficulty = 3,
        funFact = "Calvinos Roman ist auch eine Liebesgeschichte zwischen dem Leser und einer Leserin, die er beim Suchen der Bücher trifft — Literatur als Liebesmedium."
    ),

    // Question 27 - Italo Calvino
    Question(
        categoryId = 10,
        questionText = "In Calvinos 'Unsichtbare Städte' führt Marco Polo welchem Herrscher seine imaginären Städte vor?",
        answerA = "Dschingis Khan",
        answerB = "Timur Lenk",
        answerC = "Kublai Khan",
        answerD = "Saladin",
        correctAnswer = 2
        explanation = "In Calvinos Roman von 1972 beschreibt Marco Polo dem alten Kublai Khan 55 imaginäre Städte — jede eine Meditation über Erinnerung, Begehren, Zeichen oder Zeit. Am Ende stellt sich heraus, dass alle Städte in Wirklichkeit Variationen von Venedig sind.",
        difficulty = 3,
        funFact = "Calvino teilte die Städte in Kategorien wie 'Städte und die Erinnerung', 'Städte und das Begehren', 'Städte und Zeichen' — eine strukturelle Poesie, die selbst die Form zur Bedeutung macht."
    ),

    // Question 28 - Italo Calvino
    Question(
        categoryId = 10,
        questionText = "In welchem Calvino-Roman klettert ein zwölfjähriger Adeliger auf einen Baum und verlässt den Boden für den Rest seines Lebens nie wieder?",
        answerA = "Der geteilte Visconte",
        answerB = "Der nicht existierende Ritter",
        answerC = "Die Kosmischen Geschichten",
        answerD = "Der Baron auf den Bäumen",
        correctAnswer = 3
        explanation = "In 'Der Baron auf den Bäumen' (1957) klettert der zwölfjährige Cosimo Piovasco di Rondò nach einem Familienstreit beim Mittagessen auf einen Baum und bleibt dort sein ganzes Leben — er liebt, liest, kämpft und stirbt auf den Bäumen der Ligurie. Das Werk ist Teil der 'Unsere Vorfahren'-Trilogie.",
        difficulty = 3,
        funFact = "Die Trilogie 'Unsere Vorfahren' entstand in den 1950er-Jahren als Calvinos allegorische Auseinandersetzung mit Individualität, Freiheit und dem Verhältnis des Menschen zur Gesellschaft."
    ),

    // Question 29 - Salman Rushdie
    Question(
        categoryId = 10,
        questionText = "Mit welchem historischen Ereignis fällt die Geburt des Protagonisten Saleem Sinai in Rushdies 'Mitternachtskinder' zusammen?",
        answerA = "Mit der Unabhängigkeit Indiens am 15. August 1947",
        answerB = "Mit dem Ende des Zweiten Weltkriegs",
        answerC = "Mit der Teilung Indiens und Pakistans",
        answerD = "Mit Mahatma Gandhis Ermordung",
        correctAnswer = 0
        explanation = "Saleem Sinai wird in der Mitternachtsstunde des 15. August 1947 geboren — genau in dem Moment, als Indien seine Unabhängigkeit erlangt. Alle in dieser Mitternachtsstunde geborenen Kinder haben übernatürliche Fähigkeiten. Saleem kann Gedanken hören; sein Gegenstück Shiva kämpft mit Knien wie Waffen.",
        difficulty = 3,
        funFact = "Rushdies 'Mitternachtskinder' gewann 1981 den Booker Prize und wurde später zum 'Booker of Bookers' (1993) und 'Best of the Booker' (2008) gewählt — das beste Booker-Buch aller Zeiten in zwei Jubiläumswahlen."
    ),

    // Question 30 - Salman Rushdie
    Question(
        categoryId = 10,
        questionText = "Welches Werk Rushdies führte 1989 zur Ausrufung einer Fatwa durch Ayatollah Khomeini?",
        answerA = "Mitternachtskinder",
        answerB = "Die Satanischen Verse",
        answerC = "Scham",
        answerD = "Harun und das Meer der Geschichten",
        correctAnswer = 1
        explanation = "Rushdies 1988 erschienene 'Die Satanischen Verse' lösten weltweite islamische Proteste aus. Ayatollah Khomeini verhängte 1989 eine Fatwa und rief zur Ermordung Rushdies auf. Rushdie lebte fast ein Jahrzehnt im Untergrund; mehrere Übersetzer des Romans wurden ermordet oder schwer verletzt.",
        difficulty = 3,
        funFact = "Im August 2022 wurde Rushdie bei einer Veranstaltung in New York mit einem Messer angegriffen und verlor dabei die Sehkraft auf einem Auge sowie die Funktion einer Hand."
    ),

    // Question 31 - Yukio Mishima
    Question(
        categoryId = 10,
        questionText = "Aus wie vielen Bänden besteht Yukio Mishimas letztes Werk 'Das Meer der Fruchtbarkeit'?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "5",
        correctAnswer = 2
        explanation = "Mishimas letzte Tetralogie 'Das Meer der Fruchtbarkeit' besteht aus vier Bänden: 'Frühlingsschnee', 'Durchgedrehtes Pferd', 'Der Tempel der Morgendämmerung' und 'Der Verfall des Engels'. Mishima übergab das Manuskript des letzten Bandes am 25. November 1970 an seinen Verleger — und beging noch am selben Tag rituellen Selbstmord.",
        difficulty = 3,
        funFact = "Mishimas Selbstmord durch Seppuku nach einem missglückten Putschversuch im japanischen Verteidigungsministerium schockierte die Welt. Er wollte damit für eine Rückkehr zum Kaiserkult protestieren."
    ),

    // Question 32 - Yukio Mishima
    Question(
        categoryId = 10,
        questionText = "In Mishimas 'Der Tempelbrand' verbrennt ein junger Mönch das Goldene Pavillon. Was treibt ihn dazu an?",
        answerA = "Gier nach dem wertvollen Gold",
        answerB = "Eine politische Protestaktion gegen den amerikanischen Besatz",
        answerC = "Rache an seinem Abt",
        answerD = "Die obsessive Überzeugung, dass nur die Zerstörung des Schönen ihn von seiner Schönheitssucht befreit",
        correctAnswer = 3
        explanation = "In 'Der Tempelbrand' (1956, dt. 'Der Tempel des goldenen Pavillons') brennt der junge Novize Mizoguchi den berühmten Goldenen Pavillon in Kyoto nieder. Er ist von der Schönheit des Tempels so besessen, dass er glaubt, ihn vernichten zu müssen, um sich davon zu befreien.",
        difficulty = 3,
        funFact = "Der Roman basiert auf einem realen Ereignis: 1950 brannte ein junger buddhistischer Novize tatsächlich den Goldenen Pavillon nieder. Mishima verarbeitete diesen Fall als psychologische Studie über Ästhetizismus und Destruktion."
    ),

    // Question 33 - Naguib Mahfouz
    Question(
        categoryId = 10,
        questionText = "Welcher arabischsprachige Autor gewann als Erster den Nobelpreis für Literatur?",
        answerA = "Naguib Mahfouz",
        answerB = "Adonis",
        answerC = "Tayeb Salih",
        answerD = "Mahmoud Darwish",
        correctAnswer = 0
        explanation = "Naguib Mahfouz erhielt 1988 als erster arabischsprachiger Autor den Nobelpreis für Literatur. Die Schwedische Akademie würdigte sein Werk, das durch 'Realismus und Erzählkunst, die für alle Menschheit bedeutsam sind, die arabische Erzählkunst geformt hat'.",
        difficulty = 3,
        funFact = "Mahfouz wurde 1994 — sechs Jahre nach dem Nobelpreis — auf offener Straße in Kairo von einem islamistischen Extremisten niedergestochen. Er überlebte, aber sein rechter Arm war teilweise gelähmt."
    ),

    // Question 34 - Naguib Mahfouz
    Question(
        categoryId = 10,
        questionText = "Welches Werk von Naguib Mahfouz wurde in Ägypten lange Zeit verboten, weil es religiöse Figuren wie Moses und Mohammed als Romancharaktere verwendete?",
        answerA = "Midaq-Gasse",
        answerB = "Kinder der Gabalawi",
        answerC = "Der Dieb und die Hunde",
        answerD = "Echnaton",
        correctAnswer = 1
        explanation = "'Kinder der Gabalawi' (arabisch: 'Awlad Haratina', 1959) zeigt allegorische Figuren, die auf Adam, Kain, Abel, Moses, Jesus und Mohammed anspielen. Das Buch wurde in Ägypten jahrzehntelang verboten und trug zu Todesdrohungen gegen Mahfouz bei.",
        difficulty = 3,
        funFact = "Mahfouz schrieb über 50 Romane und mehr als 350 Kurzgeschichten in einem Zeitraum von 70 Jahren. Sein Werk spiegelt die gesamte moderne Geschichte Ägyptens wider."
    ),

    // Question 35 - Naguib Mahfouz
    Question(
        categoryId = 10,
        questionText = "Welche drei Viertel Kairos bilden die Kulisse von Mahfouz' Kairo-Trilogie?",
        answerA = "Gizeh, Heliopolis und Maadi",
        answerB = "Khan el-Khalili, Muqattam und Zamalek",
        answerC = "Bayn al-Qasrayn, Qasr al-Shauq und Al-Sukkariyya",
        answerD = "Bulaq, Shubra und Imbaba",
        correctAnswer = 2
        explanation = "Die drei Bände der Kairo-Trilogie heißen nach historischen Straßen und Vierteln Kairos: 'Bayn al-Qasrayn' (Zwischen den Palästen), 'Qasr al-Shauq' (Palast der Sehnsucht) und 'Al-Sukkariyya' (Zuckergasse). Sie folgen drei Generationen einer Kairoer Familie vom Ersten Weltkrieg bis zur Revolution von 1952.",
        difficulty = 3,
        funFact = "Mahfouz schrieb die Kairo-Trilogie in den Jahren 1946–1952, veröffentlichte sie aber erst 1956–57. Sie gilt als das Hauptwerk der arabischen Romanliteratur und Mahfouz' Meisterwerk."
    ),

    // Question 36 - Anton Tschechow
    Question(
        categoryId = 10,
        questionText = "Welches Tschechow-Stück gilt als sein letztes vollendetes Drama und wurde am 17. Januar 1904 uraufgeführt?",
        answerA = "Drei Schwestern",
        answerB = "Onkel Wanja",
        answerC = "Die Möwe",
        answerD = "Der Kirschgarten",
        correctAnswer = 3
        explanation = "Tschechows letztes Stück 'Der Kirschgarten' wurde am 17. Januar 1904 im Moskauer Künstlertheater uraufgeführt — an Tschechows 44. Geburtstag. Es zeigt den Niedergang einer russischen Adelsfamilie, die ihren Kirschgarten nicht retten kann. Tschechow starb nur sechs Monate nach der Uraufführung.",
        difficulty = 3,
        funFact = "Tschechow und Regisseur Stanislawski hatten einen berühmten Streit über das Genre: Tschechow bestand darauf, der Kirschgarten sei eine Komödie, Stanislawski inszenierte ihn als Tragödie — ein Konflikt, der bis heute in Inszenierungen nachwirkt."
    ),

    // Question 37 - Anton Tschechow
    Question(
        categoryId = 10,
        questionText = "Welche Erzählung Tschechows beschreibt einen Arzt, der in einem psychiatrischen Krankenhaus eine tiefe geistige Verbindung mit einem Patienten findet und selbst eingesperrt wird?",
        answerA = "Station Nr. 6",
        answerB = "Der schwarze Mönch",
        answerC = "Ein ödes Leben",
        answerD = "Die Dame mit dem Hündchen",
        correctAnswer = 0
        explanation = "In 'Station Nr. 6' (1892) verliert der Arzt Ragin seine gesellschaftliche Distanz, wird mit dem Patienten Gromow befreundet, als philosophischer Gesprächspartner angesehen und schließlich selbst in der Anstalt eingesperrt. Das Werk gilt als Tschechows schärfste Gesellschaftskritik.",
        difficulty = 3,
        funFact = "Lenin soll nach der Lektüre von 'Station Nr. 6' gesagt haben: 'Gestern Nacht las ich Tschechows Station Nr. 6. Zuerst fror es mich, dann wurde es mir heiß.' Das Werk gilt als Allegorie auf die Verhältnisse in Russland."
    ),

    // Question 38 - Ivan Turgenew
    Question(
        categoryId = 10,
        questionText = "Wie heißt der nihilistische Arzt in Turgenjews 'Väter und Söhne', der zum Prototyp des russischen Nihilisten wurde?",
        answerA = "Arkadi Kirsanow",
        answerB = "Jewgeni Basarow",
        answerC = "Nikolai Kirsanow",
        answerD = "Pjotr Lwowitsch",
        correctAnswer = 1
        explanation = "Jewgeni Basarow ist der Prototyp des russischen Nihilisten: Er lehnt alle Autoritäten, Traditionen und romantischen Gefühle ab und glaubt nur an die Naturwissenschaften. Turgenjew prägte mit dieser Figur den Begriff 'Nihilist' für die russische politische Kultur.",
        difficulty = 3,
        funFact = "Als 'Väter und Söhne' 1862 erschien, löste das Buch einen Sturm der Empörung aus: Die radikale Jugend sah sich in Basarow verspottet, die Konservativen sahen ihn als Held. Turgenjew war von beiden Seiten angegriffen."
    ),

    // Question 39 - Ivan Turgenew
    Question(
        categoryId = 10,
        questionText = "Welche Turgenjew-Werk besteht aus Jagdgeschichten, die die Unmenschlichkeit der Leibeigenschaft enthüllen und zur Abschaffung derselben beitrugen?",
        answerA = "Rudin",
        answerB = "Ein Adelsnest",
        answerC = "Aufzeichnungen eines Jägers",
        answerD = "Am Vorabend",
        correctAnswer = 2
        explanation = "Turgenjews 'Aufzeichnungen eines Jägers' (1852) ist eine Sammlung von Erzählungen, in denen der Erzähler auf Jagd trifft und die Leibeigenen mit Würde und Menschlichkeit zeigt. Das Werk erschütterte das Gewissen der russischen Gesellschaft und gilt als Mitauslöser für die Aufhebung der Leibeigenschaft 1861.",
        difficulty = 3,
        funFact = "Zar Alexander II. soll gesagt haben, die 'Aufzeichnungen eines Jägers' hätten ihn in seiner Jugend stark beeindruckt und sein Bild von den Bauern geprägt — ein Beitrag zu seiner Entscheidung, die Leibeigenschaft abzuschaffen."
    ),

    // Question 40 - Ivan Gontscharow
    Question(
        categoryId = 10,
        questionText = "Welcher Begriff aus Gontscharows Roman 'Oblomow' beschreibt krankhaften Müßiggang und Antriebslosigkeit als gesellschaftliches Phänomen?",
        answerA = "Basarowismus",
        answerB = "Rudintum",
        answerC = "Tschitschikismus",
        answerD = "Oblomowschtschina",
        correctAnswer = 3
        explanation = "Der Begriff 'Oblomowschtschina' (Oblomovismus) bezeichnet die von Gontscharow beschriebene Kombination aus Apathie, Faulheit und Handlungsunfähigkeit als gesellschaftliche Krankheit des russischen Adels. Der Literaturkritiker Dobroljubow machte den Begriff weltberühmt.",
        difficulty = 3,
        funFact = "Oblomow verbringt die ersten 50 Seiten des Romans damit, aufzustehen — oder vielmehr: daran zu scheitern. Gontscharow beschreibt dieses Nichtstun mit einer verblüffenden psychologischen Präzision."
    ),

    // Question 41 - Ivan Gontscharow
    Question(
        categoryId = 10,
        questionText = "Wer ist Oblomows energischer Gegenspieler in Gontscharows Roman, der das Ideal des aktiven modernen Menschen verkörpert?",
        answerA = "Stolz",
        answerB = "Tarantyew",
        answerC = "Mukhojrow",
        answerD = "Sudbinski",
        correctAnswer = 0
        explanation = "Andrei Stolz, halb Deutsch, halb Russisch, ist Oblomows Gegenentwurf: aktiv, leistungsorientiert, zielstrebig. Er ist Oblomows einziger wirklicher Freund. Die Frau Olga, in die sich Oblomow verliebt, wählt am Ende Stolz — eine Entscheidung, die symbolisch für die Zukunft Russlands steht.",
        difficulty = 3,
        funFact = "Die Figur Stolz ist autobiografisch: Gontscharows Vater war ein russischer Kaufmann, aber sein Vormund — ein Bildungsspender — war Deutscher. Diese deutsch-russische Dualität spiegelt sich in Stolz wider."
    ),

    // Question 42 - Borges vertieft
    Question(
        categoryId = 10,
        questionText = "In welcher Borges-Erzählung wird eine geheime Gesellschaft beschrieben, die eine fiktive Welt namens Tlön erschafft, die allmählich die Realität verdrängt?",
        answerA = "Das Aleph",
        answerB = "Tlön, Uqbar, Orbis Tertius",
        answerC = "Die Lotterie in Babylon",
        answerD = "Der Garten der sich verzweigenden Pfade",
        correctAnswer = 1
        explanation = "In 'Tlön, Uqbar, Orbis Tertius' (1940) entdeckt der Erzähler, dass eine geheime Gesellschaft eine vollständige fiktive Welt mit eigener Sprache und Philosophie erschaffen hat. Tlön beginnt, in die Realität einzudringen — Gegenstände aus Tlön tauchen in der realen Welt auf.",
        difficulty = 3,
        funFact = "Borges schrieb die Geschichte 1940, als der Faschismus Europa dominierte. Tlön ist eine Allegorie auf totalitäre Ideologien, die die Realität durch eine konstruierte Weltanschauung ersetzen."
    ),

    // Question 43 - Rushdie vertieft
    Question(
        categoryId = 10,
        questionText = "In welchem Land spielt Rushdies Roman 'Scham', der auf zeitgenössischen politischen Ereignissen basiert?",
        answerA = "Indien",
        answerB = "Bangladesch",
        answerC = "Pakistan",
        answerD = "Iran",
        correctAnswer = 2
        explanation = "'Scham' (1983) spielt in einem fiktiven Land, das deutlich Pakistan ähnelt, und behandelt die politische Rivalität zwischen zwei Familien, die an Zulfikar Ali Bhutto und Zia ul-Haq erinnern. Rushdie verkleidete politische Satire als magischen Realismus.",
        difficulty = 3,
        funFact = "Das Buch wurde in Pakistan verboten. Rushdie selbst sagte: 'Ich bin kein Pakistani, aber Pakistan ist in mich eingebaut.' Er wurde in Mumbai geboren, seine Familie zog aber später nach Pakistan."
    ),

    // Question 44 - Proust vertieft
    Question(
        categoryId = 10,
        questionText = "Wie heißt der erste Band von Prousts 'Auf der Suche nach der verlorenen Zeit'?",
        answerA = "Im Schatten junger Mädchenblüte",
        answerB = "Die Gefangene",
        answerC = "Sodom und Gomorrha",
        answerD = "In Swanns Welt",
        correctAnswer = 3
        explanation = "Der erste Band heißt 'Du côté de chez Swann' — auf Deutsch 'In Swanns Welt' oder 'In der Richtung von Swann'. Er enthält die berühmte Madeleine-Episode und die 'Liebe Swanns', eine eigenständige Novelle über Swanns Eifersucht auf Odette.",
        difficulty = 3,
        funFact = "Mehrere Verlage lehnten den ersten Band ab, darunter auch André Gide für die Nouvelle Revue Française — ein Fehler, den Gide später als 'den größten Fehler meines Lebens' bezeichnete."
    ),

    // Question 45 - Calvino vertieft
    Question(
        categoryId = 10,
        questionText = "Welcher Calvino-Roman erzählt von einem Mann, der nach einem Kanonenschuss in zwei Hälften gespalten wird — eine gute und eine böse?",
        answerA = "Der geteilte Visconte",
        answerB = "Der nicht existierende Ritter",
        answerC = "Der Baron auf den Bäumen",
        answerD = "Kosmische Geschichten",
        correctAnswer = 0
        explanation = "'Der geteilte Visconte' (1952) ist der erste Band von Calvinos 'Unsere Vorfahren'-Trilogie. Der Visconte Medardo wird durch eine Kanonenkugel in eine böse und eine gute Hälfte gespalten. Beide Hälften treiben in seiner Heimat ihr Unwesen, bis sie durch einen Zweikampf wieder vereint werden.",
        difficulty = 3,
        funFact = "Calvino schrieb die Trilogie als Allegorie auf den modernen Menschen: Der gespaltene Visconte steht für die innere Zerrissenheit, der Baron auf den Bäumen für Isolation, der nicht existierende Ritter für Identitätslosigkeit."
    ),

    // Question 46 - Mishima vertieft
    Question(
        categoryId = 10,
        questionText = "Wie heißt Mishimas autobiografisch gefärbter Debütroman, der die verborgene Homosexualität eines jungen Mannes beschreibt?",
        answerA = "Verbotene Farben",
        answerB = "Geständnis einer Maske",
        answerC = "Lärm und Wirrnis",
        answerD = "Thirst for Love",
        correctAnswer = 1
        explanation = "'Geständnis einer Maske' (1949) ist Mishimas Debütroman, der einem homosexuellen Protagonisten folgt, der eine heterosexuelle Normalität vorspielt. Das Buch machte den 24-jährigen Mishima über Nacht berühmt und gilt als Pionierwerk der japanischen Literatur über Homosexualität.",
        difficulty = 3,
        funFact = "Mishima selbst war homosexuell, führte aber auf gesellschaftlichen Druck hin eine Ehe. Sein literarisches Werk, seine körperliche Selbststilisierung und sein dramatischer Tod sind Teile eines bewusst gestalteten Lebenskunstwerks."
    ),

    // Question 47 - Gogol vertieft
    Question(
        categoryId = 10,
        questionText = "In welcher Gogol-Erzählung verlässt eine Nase ihren Besitzer und treibt als unabhängige Person in St. Petersburg ihr Unwesen?",
        answerA = "Der Mantel",
        answerB = "Das Porträt",
        answerC = "Die Nase",
        answerD = "Der Newsky-Prospekt",
        correctAnswer = 2
        explanation = "In Gogols absurdistischer Erzählung 'Die Nase' (1836) verlässt die Nase des Majors Kowaljow eines Morgens sein Gesicht und beginnt, als Staatsrat verkleidet ein unabhängiges Leben in St. Petersburg zu führen. Die Erzählung gilt als Meisterwerk des Grotesken und des literarischen Absurdismus.",
        difficulty = 3,
        funFact = "Gogol sagte selbst über 'Die Nase': 'Das ist absoluter Unsinn.' Er wollte sie nicht veröffentlichen, ließ sich aber durch Freunde überreden. Die Geschichte gilt heute als einer seiner bedeutendsten Texte."
    ),

    // Question 48 - Achebe vertieft
    Question(
        categoryId = 10,
        questionText = "Welchen Titel trägt Achebes Fortsetzungsroman zu 'Alles zerfällt', der die Kolonialverwaltung aus Sicht eines nigerianischen Beamten zeigt?",
        answerA = "Arrow of God",
        answerB = "Anthills of the Savannah",
        answerC = "A Man of the People",
        answerD = "No Longer at Ease",
        correctAnswer = 3
        explanation = "'No Longer at Ease' (1960) ist die Fortsetzung von 'Things Fall Apart' und zeigt Obis Okonkwo, Enkelsohn des Protagonisten aus dem ersten Roman, als städtischen Beamten in Lagos, der zwischen westlichen und traditionellen Werten zerrissen wird und schließlich Bestechungsgelder nimmt.",
        difficulty = 3,
        funFact = "Der Titel stammt aus T.S. Eliots Gedicht 'The Journey of the Magi': 'No longer at ease here, in the old dispensation' — eine Zeile, die Achebes Thema der kulturellen Desorientierung perfekt beschreibt."
    ),

    // Question 49 - Tschechow vertieft
    Question(
        categoryId = 10,
        questionText = "Wie lautet Tschechows bekanntes dramaturgisches Prinzip über angekündigte Requisiten im Theater?",
        answerA = "Tschechows Gewehr",
        answerB = "Tschechows Kanone",
        answerC = "Das Messer-Prinzip",
        answerD = "Das Fenster-Prinzip",
        correctAnswer = 0
        explanation = "Tschechows Gewehr-Prinzip lautet: 'Wenn im ersten Akt ein Gewehr an der Wand hängt, muss es im letzten Akt abgefeuert werden.' Dieses dramaturgische Prinzip besagt, dass jedes Element in einer Geschichte seine narrative Funktion erfüllen muss — nichts darf überflüssig eingeführt werden.",
        difficulty = 3,
        funFact = "Tschechow formulierte dieses Prinzip in Briefen an befreundete Schriftsteller. Es wird heute in Drehbuchschulen weltweit gelehrt und gilt als eines der wichtigsten Grundgesetze dramatischer Struktur."
    ),

    // Question 50 - Joyce vertieft
    Question(
        categoryId = 10,
        questionText = "In welcher Stadt und in welchem Verlag wurde James Joyces 'Ulysses' 1922 erstmals als Buch veröffentlicht?",
        answerA = "London, bei Jonathan Cape",
        answerB = "Paris, bei Shakespeare and Company",
        answerC = "Zürich, bei der Rhein-Verlag",
        answerD = "New York, bei Scribner's",
        correctAnswer = 1
        explanation = "Joyces 'Ulysses' wurde am 2. Februar 1922 — Joyces 40. Geburtstag — von Sylvia Beachs Shakespeare and Company in Paris veröffentlicht. Das Buch war in den USA und Großbritannien wegen Obszönität verboten; in Amerika durfte es erst nach einem Gerichtsprozess 1933 erscheinen.",
        difficulty = 3,
        funFact = "Sylvia Beach riskierte mit der Veröffentlichung ihre gesamte finanzielle Existenz. Sie druckte 1000 Exemplare und ließ sich von Subskribenten im Voraus bezahlen. Heute ist ein Erstdruck Hunderttausende Euro wert."
    ),

    // Question 51 - Saramago vertieft
    Question(
        categoryId = 10,
        questionText = "Welche literarische Technik ist typisch für Saramagos Stil, die 'Die Stadt der Blinden' besonders auffällig macht?",
        answerA = "Kurze, fragmentierte Sätze und häufige Absätze",
        answerB = "Wechselnde Erzählperspektiven in jedem Kapitel",
        answerC = "Lange, fließende Sätze mit minimaler Interpunktion und ohne Anführungszeichen für Dialoge",
        answerD = "Alphabetisch geordnete Kapitel",
        correctAnswer = 2
        explanation = "Saramagos Prosa ist für ihre langen, atemraubenden Sätze bekannt, die oft eine ganze Seite umfassen und Dialoge ohne Anführungszeichen direkt in den Erzählfluss einbetten. Diese Technik erzeugt einen hypnotischen Sog und spiegelt in 'Die Stadt der Blinden' das Chaos und die Orientierungslosigkeit der blinden Gesellschaft wider.",
        difficulty = 3,
        funFact = "Saramagos Verleger bat ihn zunächst, seinen Stil zu ändern und Anführungszeichen zu verwenden. Saramago weigerte sich und behielt seinen unverwechselbaren Stil bei — der ihm letztlich den Nobelpreis einbrachte."
    )
)
