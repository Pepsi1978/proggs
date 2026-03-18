package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsMaster4(): List<Question> = listOf(

    // --- DREHBUCH & ADAPTATION ---

    // Question 1 - William Goldman Prinzip
    Question(
        categoryId = 4,
        questionText = "William Goldman formulierte den beruehmt-beruechtigten Satz ueber Hollywood, der als Motto fuer die Unvorhersehbarkeit des Filmgeschaefts gilt. Wie lautet er exakt?",
        answerA = "Nobody knows anything.",
        answerB = "The audience is always right.",
        answerC = "Story is king, everything else is detail.",
        answerD = "Every picture tells a lie.",
        correctAnswer = 0,
        explanation = "William Goldman schrieb in seinem Buch 'Adventures in the Screen Trade' (1983) den Satz 'Nobody knows anything.' Er meinte damit, dass kein Mensch in Hollywood mit Sicherheit vorhersagen kann, ob ein Film erfolgreich sein wird oder nicht.",
        difficulty = 5,
        funFact = "Goldman gewann den Drehbuch-Oscar zweimal: fuer 'Butch Cassidy and the Sundance Kid' (1970) und 'All the President's Men' (1977). Er war ausserdem ein gefragter Script Doctor und arbeitete ungenannt an Dutzenden grosser Produktionen."
    ),

    // Question 2 - Charlie Kaufman Debüt
    Question(
        categoryId = 4,
        questionText = "Charlie Kaufmans Filmdebuel als Drehbuchautor war ein Meilenstein des meta-fiktionalen Kinos. Fuer welchen Film schrieb er sein erstes produziertes Drehbuch?",
        answerA = "Eternal Sunshine of the Spotless Mind (2004)",
        answerB = "Being John Malkovich (1999)",
        answerC = "Adaptation (2002)",
        answerD = "Synecdoche, New York (2008)",
        correctAnswer = 1,
        explanation = "'Being John Malkovich' (1999, Regie: Spike Jonze) war Kaufmans erstes produziertes Drehbuch. Der Film handelt von einem Puppenspieler, der einen Tunnel entdeckt, der direkt in den Geist von John Malkovich fuehrt.",
        difficulty = 5,
        funFact = "Kaufman erfand fuer 'Adaptation' den fiktiven Bruder Donald Kaufman, der im Film gemeinsam mit ihm als Co-Autor gelistet wird. Dieser erfundene Charakter wurde tatsaechlich fuer einen Oscar nominiert — einmalig in der Geschichte der Academy Awards."
    ),

    // Question 3 - Aaron Sorkin Technik
    Question(
        categoryId = 4,
        questionText = "Aaron Sorkin ist bekannt fuer seinen unverwechselbaren Dialogstil, der oft als 'Walk and Talk' bezeichnet wird. In welcher TV-Serie wurde diese Technik zuerst systematisch eingesetzt und perfektioniert?",
        answerA = "The Newsroom (2012)",
        answerB = "Sports Night (1998)",
        answerC = "The West Wing (1999)",
        answerD = "Studio 60 on the Sunset Strip (2006)",
        correctAnswer = 2,
        explanation = "'The West Wing' (1999-2006) wurde zur Vorzeige-Serie fuer Sorkins 'Walk and Talk'-Technik, bei der Charaktere schnell durch Flure gehen und dabei komplexe Dialoge fuehren. Regisseur Thomas Schlamme entwickelte die Methode gemeinsam mit Sorkin.",
        difficulty = 5,
        funFact = "Sorkin schrieb alle 22 Episoden der ersten Staffel von 'The West Wing' allein — eine ausserordentliche Leistung. Die Serie gewann vier Jahre in Folge den Emmy als beste Dramaserie, ein bis dahin nie dagewesener Rekord."
    ),

    // Question 4 - Coen-Brüder Adaptation
    Question(
        categoryId = 4,
        questionText = "Die Coen-Brueder adaptierten Cormac McCarthys Roman 'No Country for Old Men' mit bemerkenswerter Werktreue. Welches Element des Romans liessen sie jedoch bewusst weg bzw. veraenderten sie?",
        answerA = "Den Tod von Llewelyn Moss",
        answerB = "Den inneren Monolog des Sheriffs als Off-Kommentar am Ende",
        answerC = "Den gesamten dritten Akt mit dem Treffen zwischen Moss und Wells",
        answerD = "Die Schlussszene mit dem Traum des Sheriffs",
        correctAnswer = 1,
        explanation = "Im Roman endet das Buch mit ausgedehnten inneren Monologen des Sheriffs. Die Coens uebernahmen die Traumsequenz am Ende, liessen aber die erzaehlenden Zwischenkapitel des Sheriffs weg und verdichteten die Off-Kommentar-Struktur erheblich.",
        difficulty = 5,
        funFact = "Cormac McCarthy war so begeistert von der Adaption, dass er sie als eine der wenigen positiven Filmumsetzungen eines seiner Werke bezeichnete. Die Coens adaptierten auch McCarthys 'True Grit' und Dashiell Hammetts 'Red Harvest' (als 'Miller's Crossing')."
    ),

    // Question 5 - Tarantino Dialogkunst Ursprung
    Question(
        categoryId = 4,
        questionText = "Quentin Tarantinos einzigartiger Dialogstil wurde massgeblich von einem bestimmten Autoren beeinflusst, dessen hartem, rhythmischen Stil Tarantino selbst am haufigsten als primaere Inspiration nennt. Wer ist das?",
        answerA = "Elmore Leonard",
        answerB = "Raymond Chandler",
        answerC = "James Ellroy",
        answerD = "Jim Thompson",
        correctAnswer = 0,
        explanation = "Elmore Leonard gilt als Tarantinos groesste literarische Inspiration. 'Jackie Brown' (1997) basiert direkt auf Leonards Roman 'Rum Punch'. Leonards 10 Regeln des Schreibens — besonders 'Leave out the parts that readers skip' — spiegeln sich in Tarantinos Arbeit wider.",
        difficulty = 5,
        funFact = "Elmore Leonard hatte selbst eine Theorie ueber Tarantino: 'Quentin ist der einzige Filmemacher, der meinen Rhythmus wirklich versteht.' Beide teilten die Faehigkeit, Gewalt und schwarzen Humor nahtlos miteinander zu verweben."
    ),

    // Question 6 - Unverfilmtes Drehbuch Stanley Kubrick
    Question(
        categoryId = 4,
        questionText = "Stanley Kubrick arbeitete jahrzehntelang an einem Projekt, das er nie vollendete und das er als sein wichtigstes bezeichnete. Um welches Drehbuch handelte es sich?",
        answerA = "Ein Film ueber Napoleon Bonaparte",
        answerB = "Eine Adaption von Arthur C. Clarkes 'Childhood's End'",
        answerC = "Ein Holocaust-Drama mit dem Titel 'The Aryan Papers'",
        answerD = "Eine Verfilmung von 'Traumnovelle' von Arthur Schnitzler",
        correctAnswer = 0,
        explanation = "Kubrick recherchierte sein Napoleon-Projekt ab 1968 und sammelte ueber 15.000 Karteikarten mit historischen Details. Er bezeichnete es als 'den besten Film, den ich nie gemacht habe'. Das Projekt scheiterte u.a. weil Sergei Bondarchuks 'Waterloo' (1970) an den Kinokassen floppte.",
        difficulty = 5,
        funFact = "Kubricks Napoleon-Recherche fuellte ganze Schraenke. Sein Notizbuch mit Kostum- und Dekor-Ideen gilt als eines der detailliertesten nicht-realisierten Film-Dokumente der Geschichte. Steven Spielberg nutzte Kubricks Notizen fuer seine Miniserie 'Napoleon' als Hommage."
    ),

    // Question 7 - Script Doctor Hollywood
    Question(
        categoryId = 4,
        questionText = "Carrie Fisher war neben ihrer Schauspielerkarriere eine der gefragtesten Script Doctors Hollywoods. An welchem grossen Blockbuster-Drehbuch arbeitete sie ueberliefert als ungenannte Autorin?",
        answerA = "Hook (1991)",
        answerB = "Sister Act (1992)",
        answerC = "The Wedding Singer (1998)",
        answerD = "Outbreak (1995)",
        correctAnswer = 1,
        explanation = "Carrie Fisher arbeitete als Script Doctor an 'Sister Act' (1992) und ueberarbeitete die Dialoge erheblich. Sie war eine der meistbeschaeftigten Script Doctors Hollywoods in den 1990ern, ohne je im Abspann genannt zu werden.",
        difficulty = 5,
        funFact = "Fisher schrieb auch ihren autobiographischen Roman 'Postcards from the Edge' selbst zum Drehbuch um. Sie sagte einmal: 'Als Script Doctor verdiene ich in einem Tag mehr als in einem Monat als Schauspielerin.'"
    ),

    // Question 8 - 3-Akt-Struktur Ursprung
    Question(
        categoryId = 4,
        questionText = "Die klassische Drei-Akt-Struktur in Hollywood-Drehbuechern wird oft auf Syd Fields Buch 'Screenplay' (1979) zurueckgefuehrt. Welche antike Quelle beschreibt jedoch eine strukturell sehr aehnliche dramaturgische Form als erste?",
        answerA = "Platons 'Symposion'",
        answerB = "Aristoteles' 'Poetik'",
        answerC = "Ciceros 'De Oratore'",
        answerD = "Horaz' 'Ars Poetica'",
        correctAnswer = 1,
        explanation = "Aristoteles beschreibt in seiner 'Poetik' (ca. 335 v. Chr.) den Aufbau der Tragoedie mit Anfang, Mitte und Ende sowie dem Konzept des Umschlags (Peripetie) — Grundelemente, die direkt der modernen Drei-Akt-Struktur entsprechen.",
        difficulty = 5,
        funFact = "Syd Field selbst betonte, er habe Aristoteles nicht kopiert, sondern die Struktur aus der Analyse von Hunderten erfolgreicher Hollywood-Drehbuecher empirisch abgeleitet. Dennoch decken sich seine Wendepunkte auf Seite 30 und 90 fast exakt mit Aristoteles' dramaturgischen Konzepten."
    ),

    // Question 9 - Save the Cat Autor
    Question(
        categoryId = 4,
        questionText = "Blake Snyders 'Save the Cat!' (2005) ist eines der meistgelesenen Drehbuch-Ratgeber. Was genau bedeutet das 'Save the Cat'-Prinzip in Snyders Terminologie?",
        answerA = "Der Held rettet eine Katze, um Sympathie beim Publikum zu gewinnen",
        answerB = "Eine fruehe Szene zeigt den Helden bei einer guten Tat, die ihn sofort sympathisch macht",
        answerC = "Der Antagonist toetet eine Katze, um seine Boesartigkeit zu demonstrieren",
        answerD = "Die Katze symbolisiert den inneren moralischen Kompass des Protagonisten",
        correctAnswer = 1,
        explanation = "Das 'Save the Cat'-Prinzip bezeichnet eine Szene zu Beginn des Films, in der der Held etwas tut — wie eine Katze retten — das ihn sofort sympathisch und identifizierbar macht. Die konkrete Katze ist dabei nur das Beispiel, die Szene kann jede Art von moralisch positiver Handlung zeigen.",
        difficulty = 5,
        funFact = "Snyder starb 2009 kurz nach dem Erfolg seines Buches. Sein System wurde so einflussreich, dass Kritiker begannen, Hollywood-Blockbuster mit dem Begriff 'Save-the-Cat-Formel' zu kritisieren, wenn sie mechanisch und vorhersehbar wirkten."
    ),

    // Question 10 - Robert McKee Story
    Question(
        categoryId = 4,
        questionText = "Robert McKees Buch 'Story' (1997) gilt als Bibel des Drehbuchschreibens. In welchem Film wird McKee selbst als Figur portraetiert, gespielt von einem bekannten Schauspieler?",
        answerA = "The Player (1992)",
        answerB = "Adaptation (2002)",
        answerC = "Barton Fink (1991)",
        answerD = "Swimming with Sharks (1994)",
        correctAnswer = 1,
        explanation = "In Charlie Kaufmans 'Adaptation' (2002, Regie: Spike Jonze) wird Robert McKee von Brian Cox gespielt. Die Szene, in der McKee einen seiner beruehmt-gefuerchteten Vortraege haelt, ist eine der denkwuerdigsten des Films.",
        difficulty = 5,
        funFact = "McKee war anfaenglich nicht begeistert davon, in 'Adaptation' portraetiert zu werden, da der Film Kaufmans Kampf zeigt, McKees Regeln zu befolgen — und sie letztlich ironisch untergrabt. Brian Cox' Darstellung gilt jedoch als hochgradig treffsicher."
    ),

    // Question 11 - Literarische Adaptation Nabokov
    Question(
        categoryId = 4,
        questionText = "Vladimir Nabokov schrieb selbst das Drehbuch zur Verfilmung seines Romans 'Lolita'. Welcher Regisseur verfilmte dieses Drehbuch, nutzte es aber nur als lockere Vorlage?",
        answerA = "Adrian Lyne fuer die Version von 1997",
        answerB = "Stanley Kubrick fuer die Version von 1962",
        answerC = "Luis Bunuel fuer ein unveroffentlichtes Projekt",
        answerD = "Roman Polanski fuer seine geplante Version",
        correctAnswer = 1,
        explanation = "Nabokov schrieb ein ausfuehrliches Drehbuch fuer Kubricks 'Lolita' (1962), das Kubrick aber weitgehend ignorierte. Kubrick sagte spaeter, er haette den Film nie gedreht, wenn er gewusst haette, wie schwer die britische Zensur sein wuerde.",
        difficulty = 5,
        funFact = "Nabokovs unveroefflichtes Original-Drehbuch war ueber 400 Seiten lang — mehr als doppelt so lang wie ein normales Drehbuch. Es wurde erst 1974 als eigenstaendiges literarisches Werk veroeffentlicht."
    ),

    // Question 12 - WGA Streik 1988
    Question(
        categoryId = 4,
        questionText = "Der grosse WGA-Streik von 1988 dauerte fuenf Monate und war einer der laengsten in der Geschichte der Writers Guild of America. Was war der Hauptstreitpunkt?",
        answerA = "Residualzahlungen fuer den aufkommenden Home-Video-Markt",
        answerB = "Beteiligung an Auslandsverkaeufen und Kabelfernsehen",
        answerC = "Mindestvergaetung fuer TV-Drehbuchautoren",
        answerD = "Autorenrechte bei Feature-Adaptationen",
        correctAnswer = 1,
        explanation = "Der WGA-Streik 1988 drehte sich hauptsaechlich um die Beteiligung der Autoren an Auslandsverkaufserlosen und Kabelfernseh-Residuals. Er dauerte vom 10. Maerz bis zum 7. August 1988 — insgesamt 155 Tage.",
        difficulty = 5,
        funFact = "Waehrend des Streiks von 1988 wurden kaum neue Projekte entwickelt. Die Produktionsfirmen nutzten sogenannte 'Streikbrecher' und aeltere Drehbuecher. Viele Autoren, die den Streik durchstanden, berichten, er habe die WGA dauerhaft gestaerkt."
    ),

    // Question 13 - WGA Streik 2007/2008
    Question(
        categoryId = 4,
        questionText = "Der WGA-Streik 2007/2008 betraf als neues Medium vor allem digitale Inhalte. Wie viele Tage dauerte dieser Streik, der erhebliche Auswirkungen auf die TV-Saison hatte?",
        answerA = "58 Tage",
        answerB = "100 Tage",
        answerC = "127 Tage",
        answerD = "88 Tage",
        correctAnswer = 1,
        explanation = "Der WGA-Streik 2007/2008 dauerte genau 100 Tage, vom 5. November 2007 bis zum 12. Februar 2008. Hauptstreitpunkt war die Verguetung fuer Inhalte, die ueber das Internet (Streaming) verbreitet werden.",
        difficulty = 5,
        funFact = "Der Streik kostete die Unterhaltungsindustrie in Los Angeles schaetzungsweise 2,1 Milliarden Dollar. Die Golden Globes-Verleihung 2008 wurde aufgrund des Streiks abgesagt — zum ersten Mal in der Geschichte der Veranstaltung."
    ),

    // Question 14 - Drehbuch-Kreditstreit
    Question(
        categoryId = 4,
        questionText = "Welcher beruehmte Kreditstreit um ein Drehbuch fuehrte dazu, dass ein Autor seinen Namen aus dem Film entfernen liess und das Pseudonym 'Alan Smithee' des Regisseurs als Vorlage fuer Autoren diente?",
        answerA = "Der Streit um 'Chinatown' (1974) zwischen Robert Towne und Roman Polanski",
        answerB = "Der Streit um 'Casablanca' (1942) zwischen Murray Burnett und Philip Epstein",
        answerC = "Der Streit um 'The Front' (1976) wegen der Blacklist",
        answerD = "Der Streit um 'Basic Instinct' (1992) zwischen Joe Eszterhas und Paul Verhoeven",
        correctAnswer = 0,
        explanation = "Robert Towne und Roman Polanski hatten einen intensiven Kreativstreit um 'Chinatown'. Towne wollte ein anderes Ende, Polanski bestand auf dem nihilistischen Schluss. Towne gewann den Oscar, der Film gilt aber als Polanskis Vision.",
        difficulty = 5,
        funFact = "Robert Towne schrieb 'Chinatown' urspruenglich als erstes Teil einer geplanten Trilogie. Er schrieb spaeter tatsaechlich 'The Two Jakes' (1990) als Fortsetzung, aber der dritte Teil 'Cloverleaf' wurde nie realisiert."
    ),

    // Question 15 - Ghost-Writing Hollywood
    Question(
        categoryId = 4,
        questionText = "Welcher bekannte Drehbuchautor ueberarbeitete das Drehbuch zu 'Schindlers Liste' (1993) erheblich, ohne im Abspann genannt zu werden — ein klassischer Ghost-Writing-Fall?",
        answerA = "Kurt Luedtke",
        answerB = "Steve Zaillian",
        answerC = "John Toll",
        answerD = "Tom Stoppard",
        correctAnswer = 1,
        explanation = "Steve Zaillian schrieb das Drehbuch zu 'Schindlers Liste' und wurde auch im Abspann genannt. Er gewann den Oscar dafuer. Das ist kein Ghost-Writing-Fall — Spielberg beauftragte ihn offiziell nach dem ersten Entwurf von Kurt Luedtke.",
        difficulty = 5,
        funFact = "Kurt Luedtke ('Out of Africa') schrieb den ersten Entwurf von 'Schindlers Liste', uebergab das Projekt aber, da er nicht glaubte, der Aufgabe gewachsen zu sein. Zaillians Version wurde mit minimalen Aenderungen gedreht."
    ),

    // Question 16 - Autobiographisches Drehbuch
    Question(
        categoryId = 4,
        questionText = "Welches Drehbuch gilt als das autobiographischste Werk seines Autors und beschreibt detailliert seine eigene Kindheit, wobei der Autor sogar seinen richtigen Namen fuer die Hauptfigur verwendete?",
        answerA = "'8 1/2' von Federico Fellini (1963)",
        answerB = "'Amarcord' von Federico Fellini (1973)",
        answerC = "'The 400 Blows' von Francois Truffaut (1959)",
        answerD = "'Roma' von Federico Fellini (1972)",
        correctAnswer = 2,
        explanation = "Francois Truffauts 'The 400 Blows' ('Les Quatre Cents Coups', 1959) ist hochgradig autobiographisch. Die Hauptfigur Antoine Doinel wurde von Jean-Pierre Leaud gespielt und trug nicht Truffauts Namen, aber alle Ereignisse basieren auf Truffauts eigener Kindheit.",
        difficulty = 5,
        funFact = "Truffaut fuehrte die Figur Antoine Doinel durch vier weitere Filme und einen Kurzfilm. Jean-Pierre Leaud, der Doinel immer spielte, und Truffaut entwickelten eine tiefe Freundschaft, die Jahrzehnte dauerte."
    ),

    // Question 17 - Adaptation Shakespeare
    Question(
        categoryId = 4,
        questionText = "Welcher Film gilt als die gelungenste und kanonischste direkte Adaptation eines Shakespeare-Stuecks in die moderne Gegenwart, bei der der Text vollstaendig auf Englisch beibehalten wurde?",
        answerA = "Romeo + Juliet von Baz Luhrmann (1996)",
        answerB = "O von Tim Blake Nelson (2001)",
        answerC = "The Taming of the Shrew von Franco Zeffirelli (1967)",
        answerD = "Richard III von Richard Loncraine (1995)",
        correctAnswer = 3,
        explanation = "Ian McKellens 'Richard III' (1995, Regie: Richard Loncraine) verlegt das Stueck in ein faschistisches England der 1930er und gilt als Musterbeispiel fuer erfolgreiche Gegenwarts-Adaptation unter Beibehaltung des Originaltexts.",
        difficulty = 5,
        funFact = "McKellen und Loncraine entwickelten das Drehbuch gemeinsam aus McKellans Buehnenfassung. Viele Wissenschaftler betrachten diese Verlegung ins 1930er-Milieu als besonders erhellend fuer die politischen Dimensionen von Shakespeares Stueck."
    ),

    // Question 18 - True Crime Adaptation
    Question(
        categoryId = 4,
        questionText = "Truman Capotes 'In Cold Blood' (1966) begrundete das Genre des 'True Crime Novel'. Wer schrieb das Drehbuch zur Verfilmung von 1967, das als eine der praezisesten literarischen Adaptationen gilt?",
        answerA = "Gore Vidal",
        answerB = "Richard Brooks",
        answerC = "John Huston",
        answerD = "Dalton Trumbo",
        correctAnswer = 1,
        explanation = "Richard Brooks schrieb das Drehbuch und fuehrte auch Regie bei 'In Cold Blood' (1967). Er wurde fuer sein Drehbuch fuer den Oscar nominiert. Brooks war bekannt fuer seine literarisch ambitionierten Adaptationen.",
        difficulty = 5,
        funFact = "Capote war an der Produktion beteiligt und stand in engem Kontakt mit Brooks. Er bezeichnete die Verfilmung als 'so gut wie moeglich' — ein grosses Lob von einem Autor, der Adaptationen seiner Werke sonst misstrauisch gegenueber stand."
    ),

    // Question 19 - Drehbuch Adaption Hemingway
    Question(
        categoryId = 4,
        questionText = "Ernest Hemingway hasste generell Verfilmungen seiner Werke. Welche Adaption soll er jedoch als einzige positiv bewertet haben?",
        answerA = "A Farewell to Arms (1932)",
        answerB = "The Killers (1946)",
        answerC = "For Whom the Bell Tolls (1943)",
        answerD = "The Old Man and the Sea (1958)",
        correctAnswer = 1,
        explanation = "'The Killers' (1946, Regie: Robert Siodmak, Drehbuch: Anthony Veiller und John Huston) basiert auf Hemingways Kurzgeschichte. Die Adaption gilt als Film-Noir-Klassiker und soll Hemingways widerwillige Zustimmung erhalten haben.",
        difficulty = 5,
        funFact = "Die Kurzgeschichte 'The Killers' ist nur wenige Seiten lang. Die Filmemacher mussten die Handlung erheblich erweitern und eine ganze Vorgeschichte erfinden. Burt Lancaster gab hier sein Kinodebuel."
    ),

    // Question 20 - Dialogikonisch Pulp Fiction
    Question(
        categoryId = 4,
        questionText = "In 'Pulp Fiction' (1994) haelt Jules einen Monolog, der als Bibelzitat eingefuehrt wird. Das angebliche Zitat 'Hesekiel 25:17' existiert in dieser Form nicht. Was ist der tatsaechliche Ursprung des Textes?",
        answerA = "Es ist vollstaendig von Tarantino erfunden",
        answerB = "Es kombiniert eine echte Bibelstelle mit Tarantinos Ergaenzungen",
        answerC = "Es stammt aus dem japanischen Martial-Arts-Film 'Karate Kiba' (1973)",
        answerD = "Es stammt aus einem fruehen Entwurf von Roger Averys Drehbuch",
        correctAnswer = 2,
        explanation = "Der Text ist eine Kombination: Das echte Hesekiel 25:17 besteht nur aus zwei Saetzen. Tarantino ergaenzte den Rest — teils aus dem japanischen Film 'The Bodyguard' ('Karate Kiba', 1973), teils frei erfunden.",
        difficulty = 5,
        funFact = "Samuel L. Jackson wurde durch diesen Monolog weltberuehmt. Er gibt zu, dass er Jules' Wandlung in 'Pulp Fiction' ernst nahm: Nach dem Dreh trug Jackson tatsaechlich eine Kappe mit 'Bad Mother F***er' auf dem Set."
    ),

    // Question 21 - Monolog-Meisterwerk Network
    Question(
        categoryId = 4,
        questionText = "Paddy Chayefskys Monolog 'I'm as mad as hell and I'm not going to take this anymore!' aus 'Network' (1976) gilt als einer der grossen Drehbuch-Monologe. Wie viele Oscars gewann Chayefsky insgesamt fuer Original-Drehbuecher?",
        answerA = "Einen",
        answerB = "Zwei",
        answerC = "Drei",
        answerD = "Vier",
        correctAnswer = 2,
        explanation = "Paddy Chayefsky ist der einzige Autor, der drei Solo-Oscars fuer Original-Drehbuecher gewann: 'Marty' (1956), 'The Hospital' (1972) und 'Network' (1977). Kein anderer Autor hat diese Leistung je wiederholt.",
        difficulty = 5,
        funFact = "Chayefsky gab seinen Dankesrede-Oscar-Moment preis, als er 1978 bei der Verleihung — ein Jahr nach seinem Gewinn — oeffenlich Vanessa Redgrave kritisierte, die eine politische Rede gehalten hatte. Es war einer der dramatischsten Oscar-Momente je."
    ),

    // Question 22 - Berühmtes unverfilmtes Drehbuch
    Question(
        categoryId = 4,
        questionText = "Welches beruehmte unverfilmte Drehbuch von Frank Darabont wurde fuer sehr viel Geld an das Studio verkauft, aber nie realisiert, und gilt heute als Beweis fuer den sogenannten 'Hollywoodisierungseffekt'?",
        answerA = "Eine Adaption von Stephen Kings 'The Long Walk'",
        answerB = "Ein Indiana-Jones-Skript fuer den fuenften Teil",
        answerC = "Ein originales Science-Fiction-Script mit dem Titel 'Asteroid City'",
        answerD = "Eine George-Romero-Hommage mit dem Titel 'Dust to Dust'",
        correctAnswer = 0,
        explanation = "Frank Darabont schrieb eine Adaption von Stephen Kings 'The Long Walk', die seit Jahrzehnten in der Entwicklungshoelle steckt. Darabont, bekannt fuer 'The Shawshank Redemption' und 'The Green Mile', hat Kings Roman lange als sein liebstes unbezahltes Projekt beschrieben.",
        difficulty = 5,
        funFact = "Stephen King vertraute Frank Darabont die Filmrechte an mehreren seiner Werke fuer nur einen Dollar an, weil er Darabonts Sensibilitaet schAtze. Diese Praxis nennt sich im Branchenjargon 'Dollar Option' und ist unter befreundeten Kreativen nicht ungewoehnlich."
    ),

    // Question 23 - Pinter Drehbuch
    Question(
        categoryId = 4,
        questionText = "Harold Pinter gilt als einer der grossen Drehbuchadapteurs des 20. Jahrhunderts. Fuer welche Adaption eines Nobelpreistraeger-Romans erhielt er seinen groessten Drehbucherfolg?",
        answerA = "'The French Lieutenant's Woman' nach John Fowles (1981)",
        answerB = "'The Go-Between' nach L.P. Hartley (1971)",
        answerC = "'Accident' nach Nicholas Mosley (1967)",
        answerD = "'The Servant' nach Robin Maugham (1963)",
        correctAnswer = 0,
        explanation = "'The French Lieutenant's Woman' (1981, Regie: Karel Reisz) mit Meryl Streep und Jeremy Irons gilt als Pinters meistbewunderte Adaption. Er erfand eine geniale Meta-Struktur: Zwei Schauspieler spielen gleichzeitig ihre Filmrollen und eine moderne Liebesgeschichte.",
        difficulty = 5,
        funFact = "John Fowles' Roman gilt als unadaptierbar, weil er mehrere alternative Schlussszenen und einen postmodernen, selbstreferentiellen Erzaehler hat. Pinters Loesung — die Film-im-Film-Struktur — gilt als Meisterstuck dramaturgischer Kreativitaet."
    ),

    // Question 24 - Oscar für Adaptiertes Drehbuch
    Question(
        categoryId = 4,
        questionText = "Welches adaptierte Drehbuch gilt als eines der am staerksten von seiner literarischen Vorlage abweichenden Oscar-Gewinner, obwohl es denselben Titel traegt?",
        answerA = "'Forrest Gump' (1994) nach Winston Grooms Roman",
        answerB = "'The Silence of the Lambs' (1991) nach Thomas Harris' Roman",
        answerC = "'Schindler's List' (1993) nach Thomas Keneallys Buch",
        answerD = "'L.A. Confidential' (1997) nach James Ellroys Roman",
        correctAnswer = 0,
        explanation = "'Forrest Gump' weicht dramatisch von Grooms Roman ab: Im Buch ist Forrest viel derber, er reist ins All, spielt Schach mit einem russischen Kosmonauten und hat ein Orang-Utan-Kompagnon. Screenwriter Eric Roth schuf im Wesentlichen eine neue Geschichte.",
        difficulty = 5,
        funFact = "Winston Groom soll ueber die Verfilmung gesagt haben, sie sei 'ein anderer Film' als sein Buch. Das Sequel 'Gump and Co.' schrieb er bewusst so, dass es fuer eine Verfilmung ungeeignet waere — als Reaktion auf seinen Kontrollverlust."
    ),

    // Question 25 - Theaterstück zu Film
    Question(
        categoryId = 4,
        questionText = "Welches Theaterstueck von David Mamet wurde von ihm selbst zu einem der gelobten Drehbuecher der 1980er adaptiert, wobei er den offenen Buehnencharakter des Originals in filmische Dynamik umwandelte?",
        answerA = "'Glengarry Glen Ross' (1992)",
        answerB = "'American Buffalo' (1996)",
        answerC = "'Oleanna' (1994)",
        answerD = "'Speed-the-Plow' (1992)",
        correctAnswer = 0,
        explanation = "'Glengarry Glen Ross' (Regie: James Foley, 1992) adaptierte Mamet sein eigenes Buehnenwerk. Er fuegte eine neue Eroefffnungsszene hinzu — den beruehmt-beruechtigten 'Coffee is for Closers'-Monolog von Alec Baldwin, der im Stueck nicht existiert.",
        difficulty = 5,
        funFact = "Alec Baldwins Auftritt als Blake in 'Glengarry Glen Ross' dauert nur acht Minuten, ist aber so unvergesslich, dass er fuer den Oscar als Bester Nebendarsteller nominiert wurde. Baldwin selbst hat gesagt, es sei der beste Monolog, den er je gespielt habe."
    ),

    // Question 26 - Drehbuch ohne Vorlage
    Question(
        categoryId = 4,
        questionText = "Welcher Oscar-preisgekroente Film basiert auf einem vollstaendig originalen Drehbuch, das zuvor jahrelang als 'unverfilmbar' galt, weil es fast keinen konventionellen Plot hatte?",
        answerA = "'American Beauty' (1999) von Alan Ball",
        answerB = "'Lost in Translation' (2003) von Sofia Coppola",
        answerC = "'Manchester by the Sea' (2016) von Kenneth Lonergan",
        answerD = "'Birdman' (2014) von Alejandro Inarritu et al.",
        correctAnswer = 2,
        explanation = "'Manchester by the Sea' von Kenneth Lonergan galt lange als schwierig zu produzieren, da es sich konsequent klassischen Redemptionsboegen verweigert und bewusst auf Katharsis verzichtet. Amazon Studios sicherte sich das Projekt und es gewann zwei Oscars.",
        difficulty = 5,
        funFact = "Lonergan brauchte ueber zehn Jahre und zwei juristisch eskalierende Produktionskonflikte (bei seinem Vorgaenger 'Margaret'), bevor 'Manchester by the Sea' entstand. Der Film gilt als Musterbeispiel fuer authentiziTaet ueber Konvention."
    ),

    // Question 27 - Drehbuchautor Blacklist
    Question(
        categoryId = 4,
        questionText = "Dalton Trumbo, einer der beruehmt-beruechtigsten Opfer der Hollywood-Blacklist, schrieb unter Pseudonym mehrere Oscar-nominierte Drehbuecher. Unter welchem Namen gewann er 1956 den Oscar?",
        answerA = "Robert Rich",
        answerB = "Ian McLellan Hunter",
        answerC = "Nathan E. Douglas",
        answerD = "John Howard Lawson",
        correctAnswer = 0,
        explanation = "Dalton Trumbo gewann 1956 den Oscar fuer 'The Brave One' unter dem Pseudonym Robert Rich. Da Rich nicht zur Verleihung erschien, blieb der Preisgewinner lange ein Mysterium. Trumbo erhielt den Oscar erst posthum offiziell zuerkannt.",
        difficulty = 5,
        funFact = "Trumbo schrieb waehrend der Blacklist-Aera auf einer Badewannen-Schreibmaschine, weil er so schnell tippen konnte. Das Bild wurde ikonisch. Er schrieb in dieser Zeit mehrere hundert Drehbuecher unter verschiedenen Namen."
    ),

    // Question 28 - Familiendrama Adaptation
    Question(
        categoryId = 4,
        questionText = "Welcher Film basiert auf einem Theaterstueck, das der Regisseur in einer einzigen Woche nach dem Tod seines Vaters schrieb, und gilt als einer der roh-emotionalsten Drehbuecher aller Zeiten?",
        answerA = "'Ordinary People' (1980) nach Judith Guests Roman",
        answerB = "'August: Osage County' (2013) nach Tracy Letts' Stueck",
        answerC = "'Terms of Endearment' (1983) nach Larry McMurtrys Roman",
        answerD = "'Rabbit Hole' (2010) nach David Lindsays Abaire",
        correctAnswer = 1,
        explanation = "'August: Osage County' basiert auf Tracy Letts' Pulitzer-preisgekroenten Stueck von 2007. Letts schrieb es tatsaechlich in einem intensiven, kurzen Zeitraum nach einem persoenlichen Verlust und bezeichnete es als seine katharischste Arbeit.",
        difficulty = 5,
        funFact = "Die Film-Adaption vereinte Meryl Streep, Julia Roberts, Ewan McGregor, Benedict Cumberbatch und Sam Shepard in einem einzigen Cast — eines der starstudded Ensembles des Jahrzehnts. Beide Hauptdarstellerinnen wurden fuer den Oscar nominiert."
    ),

    // Question 29 - Syd Field Wendepunkte
    Question(
        categoryId = 4,
        questionText = "Syd Field definiert in 'Screenplay' zwei zentrale Wendepunkte ('Plot Points'). Auf welchen Seitenzahlen eines Standard-120-Seiten-Drehbuchs liegen sie laut Field idealerweise?",
        answerA = "Seite 25 und Seite 85",
        answerB = "Seite 30 und Seite 90",
        answerC = "Seite 20 und Seite 80",
        answerD = "Seite 35 und Seite 95",
        correctAnswer = 1,
        explanation = "Syd Field postuliert in 'Screenplay', dass der erste Wendepunkt (Plot Point I) etwa auf Seite 30 und der zweite (Plot Point II) auf Seite 90 liegen sollte. Diese Wendepunkte trennen die drei Akte voneinander.",
        difficulty = 5,
        funFact = "Field analysierte Dutzende erfolgreiche Hollywood-Drehbuecher und stellte fest, dass sich die Wendepunkte erstaunlich konsequent um diese Seiten gruppierten. Sein Buch war das erste, das diese Struktur klar bebenannte und lehrbar machte."
    ),

    // Question 30 - Nabokov Drehbuch Lolita-Seiten
    Question(
        categoryId = 4,
        questionText = "Welcher Drehbuchautor gilt als der produktivste nicht-amerikanische Autor Hollywoods der 1950er/60er Jahre und schrieb u.a. das Drehbuch fuer 'Ben-Hur' (1959), ohne im Abspann zu erscheinen?",
        answerA = "Gore Vidal",
        answerB = "Christopher Fry",
        answerC = "S.N. Behrman",
        answerD = "Ranald MacDougall",
        correctAnswer = 0,
        explanation = "Gore Vidal ueberarbeitete das 'Ben-Hur'-Drehbuch erheblich und fuegte eine von Karl Tunberg nicht geplante Subtext-Ebene ein. Er behauptete jahrzehntelang, er habe Charlton Heston ohne dessen Wissen eine homosexuelle Dimension in die Beziehung zwischen Judah und Messala eingeschrieben.",
        difficulty = 5,
        funFact = "Vidal und Heston stritten sich oeffentlich jahrelang ueber diese Behauptung. Regisseur William Wyler bestagte Vidals Version kurz vor seinem Tod in einem Interview — was die Kontroverse neu entfachte."
    ),

    // Question 31 - Paul Schrader Methode
    Question(
        categoryId = 4,
        questionText = "Paul Schrader schrieb 'Taxi Driver' (1976) in wenigen Wochen in einem intensiven, isolierten Schreibprozess. Wie lange soll er laut Eigenaussage fuer den ersten Entwurf benoetigt haben?",
        answerA = "Zehn Tage",
        answerB = "Drei Wochen",
        answerC = "Einen Monat",
        answerD = "Sechs Wochen",
        correctAnswer = 0,
        explanation = "Paul Schrader schrieb den ersten Entwurf von 'Taxi Driver' laut Eigenaussage in ca. zehn Tagen, in einem Zustand emotionaler Erschoepfung und Isolation. Er beschrieb es als eines seiner persoenlichsten und schmerzhaftesten Werke.",
        difficulty = 5,
        funFact = "Schrader lebte zu dieser Zeit in seinem Auto, nachdem seine Ehe gescheitert war. Er sah sich in Travis Bickle — dem isolierten, gewaltsamen Taxifahrer — und schrieb den Charakter als Selbsttherapie. Das Drehbuch landete Jahre bevor es verfilmt wurde bei Martin Scorsese."
    ),

    // Question 32 - Buch vs. Film strukturell
    Question(
        categoryId = 4,
        questionText = "Joseph Conrads Novelle 'Heart of Darkness' ist die Vorlage fuer 'Apocalypse Now'. Welche strukturelle Hauptveraenderung nahm Drehbuchautor John Milius vor?",
        answerA = "Er verlegte die Handlung vom Kongo nach Vietnam",
        answerB = "Er machte Willard zum aktiven Killer statt zum passiven Beobachter",
        answerC = "Er erfand den Charakter des Lieutenant Kilgore vollstaendig",
        answerD = "Er gab Kurtz einen Ueberlebensschluss statt des Todes",
        correctAnswer = 2,
        explanation = "Lieutenant Colonel Kilgore (gespielt von Robert Duvall) — mit seinem beruehmt-beruechtigten 'I love the smell of napalm in the morning' — existiert in Conrads Novelle nicht. Milius erfand ihn vollstaendig als Sinnbild amerikanischer Kriegsbegeisterung.",
        difficulty = 5,
        funFact = "Der 'Napalm'-Monolog von Kilgore wurde laut Duvall teils improvisiert. Die Szene der Surfboote-Landung unter Beschuss wurde von Coppola im Dreh massiv ausgebaut — urspruenglich war sie viel kleiner geplant."
    ),

    // Question 33 - Sergio Leone Dialog
    Question(
        categoryId = 4,
        questionText = "Sergio Leone ist beruehmt fuer Dialoge, die sparsamst eingesetzt werden. Wie viele Seiten Drehbuch hatte 'Il buono, il brutto, il cattivo' ('Zwei glorreiche Halunken', 1966) laut Ueberlieferung?",
        answerA = "Ueber 200 Seiten",
        answerB = "Etwa 150 Seiten",
        answerC = "Weniger als 80 Seiten",
        answerD = "Genau 105 Seiten",
        correctAnswer = 2,
        explanation = "Leones Drehbuch fuer 'Il buono, il brutto, il cattivo' war trotz der fast dreistuendigen Laufzeit ungewoehnlich kurz — weil ein Grossteil des Films aus visuell erzaehlten Sequenzen ohne Dialog besteht. Musik und Blicke ersetzen Worte.",
        difficulty = 5,
        funFact = "Der beruehmt-beruechtigte Dreiecks-Showdown am Ende dauert fast zehn Minuten ohne einen einzigen Dialogsatz. Leone und Kameramann Tonino Delli Colli planten jede einzelne Einstellung dieser Sequenz minutioes im Storyboard."
    ),

    // Question 34 - Bergman Drehbuch
    Question(
        categoryId = 4,
        questionText = "Ingmar Bergman veroeffentlichte viele seiner Drehbuecher als eigenstaendige Literatur. Welches Drehbuch schrieb er in einer Nacht, wie er selbst berichtet, nach einem Alptraum?",
        answerA = "'Wilde Erdbeeren' (1957)",
        answerB = "'Persona' (1966)",
        answerC = "'Das Schweigen' (1963)",
        answerD = "'Szenen einer Ehe' (1973)",
        correctAnswer = 1,
        explanation = "'Persona' entstand laut Bergman aus einer fieberhaften Schreibnacht, ausgeloest durch einen Traum. Das fragmentarische, experimentelle Ergebnis spiegelt diesen unwillkuerlichen Entstehungsprozess in seiner Struktur wider.",
        difficulty = 5,
        funFact = "'Persona' gilt als eines der raetselhaftesten und meistanalysierten Drehbuecher der Filmgeschichte. Die beruehmte Einstellungssequenz, in der der Filmstreifen zu schmelzen scheint, war eine bewusste Entscheidung Bergmans, die vierte Wand zu durchbrechen."
    ),

    // Question 35 - Whiplash Kurzfilm zu Langfilm
    Question(
        categoryId = 4,
        questionText = "Damien Chazelle konnte 'Whiplash' (2014) als Spielfilm erst realisieren, nachdem er eine Kurzfilmversion gedreht hatte, um Investoren zu ueberzeugen. Wie lang war dieser Kurzfilm?",
        answerA = "8 Minuten",
        answerB = "18 Minuten",
        answerC = "25 Minuten",
        answerD = "35 Minuten",
        correctAnswer = 1,
        explanation = "Chazelle drehte einen 18-minutigen Kurzfilm von 'Whiplash' mit dem gleichen Cast (J.K. Simmons, Miles Teller), der am Sundance Film Festival 2013 gezeigt wurde. Der Erfolg dieses Kurzfilms sicherte die Finanzierung fuer den Spielfilm.",
        difficulty = 5,
        funFact = "Chazelle schrieb das Drehbuch zu 'Whiplash' urspruenglich fuer sich selbst als potenziellen Regisseur, aber ohne Finanzierung. Die Idee, zuerst einen Kurzfilm zu drehen, stammte von Produzent Jason Blum (Blumhouse Productions)."
    ),

    // Question 36 - Coen Brüder Originalstruktur
    Question(
        categoryId = 4,
        questionText = "Die Coen-Brueder schrieben 'Fargo' (1996) mit dem Hinweis 'This is a true story.' Wie viel des Films basiert tatsaechlich auf wahren Begebenheiten?",
        answerA = "Der gesamte Plot basiert auf echten Ereignissen aus den 1980ern",
        answerB = "Nur die Mordserie im Mittelteil ist dokumentiert",
        answerC = "Praktisch nichts — es ist vollstaendig erfunden",
        answerD = "Die Charaktere Jerry und Marge basieren auf realen Personen",
        correctAnswer = 2,
        explanation = "Die Coens gaben zu, dass 'Fargo' im Wesentlichen vollstaendig erfunden ist. Sie verwendeten den 'true story'-Hinweis als dramaturgisches Mittel, um das Publikum in falscher Sicherheit zu wiegen und die Schockmomente zu verstaerken.",
        difficulty = 5,
        funFact = "Die Schredder-Szene ist zu ikonisch geworden, dass ein japanischer Tourist namens Takako Konishi 2001 nach North Dakota reiste, offenbar ueberzeugt, dass das Filmgeld wirklich existiere. Dieser tragische Irrtum wurde selbst zum Dokumentarfilm."
    ),

    // Question 37 - Drehbuch Polanski
    Question(
        categoryId = 4,
        questionText = "Robert Towne gilt als einer der grossen Drehbuchautoren Hollywoods. Fuer welchen anderen Blockbuster-Hit schrieb er das Drehbuch, der oft neben 'Chinatown' als sein Meisterwerk genannt wird?",
        answerA = "'Shampoo' (1975)",
        answerB = "'The Godfather' (1972, Mitarbeit)",
        answerC = "'Marathon Man' (1976)",
        answerD = "'Personal Best' (1982)",
        correctAnswer = 0,
        explanation = "'Shampoo' (1975, Regie: Hal Ashby) schrieb Towne gemeinsam mit Warren Beatty. Der Film gilt als eines der praezisesten Portraets des politischen Klimas Amerikas um die Nixon-Wahl 1968 und wurde fuer den Oscar nominiert.",
        difficulty = 5,
        funFact = "Towne ueberarbeitete auch Teile des 'Godfather'-Drehbuchs, insbesondere die Gartenszene zwischen Vito und Michael Corleone. Er erhielt jedoch keine Gutschrift. Francis Ford Coppola und Mario Puzo teilten sich den Drehbuch-Oscar."
    ),

    // Question 38 - Sorkin Tempo
    Question(
        categoryId = 4,
        questionText = "Aaron Sorkins 'The Social Network' (2010) ist bekannt fuer seinen extrem dichten Dialog. Wie viele Woerter pro Minute enthalten Sorkins Drehbuecher im Durchschnitt im Vergleich zu einem Standard-Hollywood-Drehbuch?",
        answerA = "Ungefaehr die gleiche Menge",
        answerB = "Etwa 10-15% mehr",
        answerC = "Fast doppelt so viele",
        answerD = "Drei- bis viermal mehr",
        correctAnswer = 2,
        explanation = "Analysen zeigen, dass Sorkins Drehbuecher im Schnitt fast doppelt so viele Dialogwoerter pro Seite enthalten wie Standard-Hollywood-Drehbuecher. 'The Social Network' ist besonders dicht — Schauspieler mussten die schnellsten Zeilen tatsaechlich neu lernen.",
        difficulty = 5,
        funFact = "Sorkin schrieb das Drehbuch zu 'The Social Network' innerhalb weniger Monate, nachdem er Ben Mezrichs Buch 'The Accidental Billionaires' gelesen hatte. Er traf sich nie mit Mark Zuckerberg — und wurde dafuer von Zuckerberg oeffenlich kritisiert."
    ),

    // Question 39 - Kurosawa Adaptation Shakespeare
    Question(
        categoryId = 4,
        questionText = "Akira Kurosawa adaptierte zweimal Shakespeare-Stuecke als Samurai-Epen. 'Ran' (1985) basiert auf 'Koenig Lear'. Auf welchem Shakespeare-Stueck basiert 'Kumonosu-jo' (dt. 'Das Schloss im Spinnwebwald', 1957)?",
        answerA = "Othello",
        answerB = "Macbeth",
        answerC = "Hamlet",
        answerD = "Richard III",
        correctAnswer = 1,
        explanation = "'Kumonosu-jo' ('Throne of Blood', 1957) ist Kurosawas Adaption von 'Macbeth'. Die Verlegung ins feudale Japan des 16. Jahrhunderts gilt als eine der gelungensten Shakespeare-Adaptionen ueberhaupt.",
        difficulty = 5,
        funFact = "Fuer die finale Pfeilszene liess Kurosawa echte Pfeile auf Toshiro Mifune abschiessen, die Bogenschiessen-Experten zwischen seinen koerpernah platzierten. Mifune sprach spaeter von der gefaehrlichsten Szene seiner Karriere."
    ),

    // Question 40 - Harold Pinter Schweigen
    Question(
        categoryId = 4,
        questionText = "Harold Pinter ist beruehmt fuer seine sogenannten 'Pinter Pauses' — absichtliche Schweigepausen im Dialog. Wie unterschied er zwischen 'Pause' und 'Silence' in seinen Regieanweisungen?",
        answerA = "Es gibt keinen Unterschied, beide Begriffe sind austauschbar",
        answerB = "'Pause' ist kurz (1-2 Sekunden), 'Silence' laenger als 5 Sekunden",
        answerC = "'Pause' bedeutet Nachdenken innerhalb des Gesprachs, 'Silence' bedeutet kommunikatives Scheitern",
        answerD = "'Silence' wird nur am Ende einer Szene verwendet, 'Pause' innerhalb",
        correctAnswer = 2,
        explanation = "Pinter unterschied prazise: Eine 'Pause' ist eine kurze Unterbrechung, bei der der Charakter nach Worten sucht oder nachdenkt — das Gespraech laeuft weiter. Eine 'Silence' hingegen bezeichnet einen Moment des vollstaendigen kommunikativen Zusammenbruchs.",
        difficulty = 5,
        funFact = "Pinter-Schauspieler wie Michael Gambon und Ian Holm berichten, dass die Pausen genauso schwer zu spielen sind wie der Dialog selbst. Pinter war als Regisseur seiner eigenen Werke extrem praezise in der Einhaltung dieser Unterscheidung."
    ),

    // Question 41 - Network Chayefsky Methode
    Question(
        categoryId = 4,
        questionText = "Paddy Chayefsky begann seine Karriere im fruehen amerikanischen Fernsehen als Autor von Live-Dramen. Welcher Fernsehfilm machte ihn beruehmt und wurde spaeter als Kinofilm neu veroeffentlicht?",
        answerA = "'Marty' (1953, dann 1955 als Kinofilm)",
        answerB = "'The Bachelor Party' (1957)",
        answerC = "'Middle of the Night' (1959)",
        answerD = "'The Hospital' (1958 als TV-Pilot)",
        correctAnswer = 0,
        explanation = "'Marty' wurde 1953 als TV-Live-Drama produziert (mit Rod Steiger) und 1955 als Kinofilm (mit Ernest Borgnine) neu verfilmt. Der Kinofilm gewann vier Oscars, darunter Besten Film und Bestes Originaldrehbuch fuer Chayefsky.",
        difficulty = 5,
        funFact = "'Marty' war das erste Kinofilm-Werk, das urspruenglich als Fernsehspiel geschrieben wurde und den Oscar fuer den Besten Film gewann. Chayefsky gilt als Pionier des 'small story'-Ansatzes: Alltagsmenschen in realistischen Situationen statt Heldenepen."
    ),

    // Question 42 - Adaptation Fitzgerald
    Question(
        categoryId = 4,
        questionText = "F. Scott Fitzgeralds 'Der grosse Gatsby' wurde mehrfach verfilmt. Welche Verfilmung gilt unter Literaturwissenschaftlern als die werkgetreueste Adaption trotz aller Hollywoodkonventionen?",
        answerA = "Die Version von 1974 mit Robert Redford, Drehbuch von Francis Ford Coppola",
        answerB = "Die Version von 1949 mit Alan Ladd",
        answerC = "Die Version von 2013 von Baz Luhrmann mit Leonardo DiCaprio",
        answerD = "Die Stummfilmversion von 1926",
        correctAnswer = 0,
        explanation = "Die Version von 1974 (Regie: Jack Clayton) mit einem Drehbuch von Francis Ford Coppola gilt trotz ihrer Schwaechen als die werktreuste Adaption. Coppola blieb dem Originaltext am naechsten, auch wenn Kritiker den Film als zu statisch empfanden.",
        difficulty = 5,
        funFact = "Coppolas Drehbuch fuer 'Gatsby' (1974) wurde von ihm selbst spaeter als eine seiner groessten Enttaeuschungen bezeichnet — nicht wegen des Texts, sondern weil er glaubte, die Regie haette sein Drehbuch nicht richtig umgesetzt."
    ),

    // Question 43 - Drehbuch Inception
    Question(
        categoryId = 4,
        questionText = "Christopher Nolan schrieb das Drehbuch zu 'Inception' (2010) ueber einen langen Zeitraum. Wie lange arbeitete er nach eigenen Angaben an der endgueltigen Version des Skripts?",
        answerA = "Etwa zwei Jahre",
        answerB = "Fast zehn Jahre",
        answerC = "Fuenf Jahre",
        answerD = "Siebzehn Jahre",
        correctAnswer = 1,
        explanation = "Christopher Nolan arbeitete nach eigenen Angaben fast zehn Jahre an 'Inception', wobei er immer wieder andere Projekte dazwischenschob ('Batman Begins', 'The Prestige', 'The Dark Knight'). Er begann das Konzept um die Jahrtausendwende.",
        difficulty = 5,
        funFact = "Nolan schrieb 'Inception' urspruenglich als Horrorfilm. Erst nachdem er das Konzept mit der Traumarchitektur gefunden hatte, wandelte es sich zum Thriller. Er bestand darauf, dass das Drehbuch vor Produktionsbeginn komplett fertig war — ungewoehnlich fuer ein 160-Millionen-Dollar-Projekt."
    ),

    // Question 44 - Bilderbuch-Adaption
    Question(
        categoryId = 4,
        questionText = "Maurice Sendaks 'Where the Wild Things Are' gilt als schwierigste Bilderbuch-Adaption Hollywoods. Spike Jonze's Version (2009) hatte ein turbulentes Produktionsleben. Was war der Hauptstreitpunkt mit dem Studio?",
        answerA = "Sendak wollte die Hauptrolle mit einem erwachsenen Schauspieler besetzen",
        answerB = "Das Studio wollte eine konventionellere, familienfreundlichere Version als Jonze lieferte",
        answerC = "Jonze bestand auf einem R-Rating fuer den Film",
        answerD = "Das Budget explodierende wegen der Wild-Things-Kostume",
        correctAnswer = 1,
        explanation = "Warner Bros. war schockiert ueber Jonzes dunkle, melancholische Version und liess den Film komplett neu bearbeiten. Jonze, Sendak und Drehbuchautor Dave Eggers kaempften fuer ihre Version. Sendak soll gesagt haben, er muesse sich bemuehen, nicht in Traenen auszubrechen.",
        difficulty = 5,
        funFact = "Maurice Sendak war begeistert von Jonzes dunkler Interpretation und sagte in Interviews: 'Das ist der erste Film nach einem meiner Buecher, der mich nicht zum Kotzen gebracht hat.' Er starb 2012, drei Jahre nach dem Filmstart."
    ),

    // Question 45 - Satirisches Drehbuch
    Question(
        categoryId = 4,
        questionText = "Stanley Kubricks 'Dr. Strangelove' (1964) basiert auf dem ernsthaften Roman 'Red Alert' von Peter George. Wer schrieb gemeinsam mit Kubrick und George das satirische Drehbuch, das den Ton radikal veraenderte?",
        answerA = "Joseph Heller",
        answerB = "Terry Southern",
        answerC = "Buck Henry",
        answerD = "Mel Brooks",
        correctAnswer = 1,
        explanation = "Terry Southern schrieb gemeinsam mit Kubrick und Peter George das Drehbuch, das den ernsthaften Thriller in eine schwarze Satire verwandelte. Southern war bekannt fuer seinen respektlosen Humor und hatte zuvor an 'Candy' und spaeter an 'Easy Rider' gearbeitet.",
        difficulty = 5,
        funFact = "Peter George, Autor des Originalromans 'Red Alert', stimmte dem satirischen Ansatz zu, obwohl er den Ernst seines Werkes bewahren wollte. Er veroeffentlichte spaeter den Roman 'Dr. Strangelove' als Tie-in zum Film, der sich besser verkaufte als seine urspruengliche 'Red Alert'."
    ),

    // Question 46 - Romanadaption Kubrick Barry Lyndon
    Question(
        categoryId = 4,
        questionText = "Stanley Kubrick adaptierte William Makepeace Thackerays Roman 'Barry Lyndon' (1975) weitgehend selbst. Welche bemerkenswerte technische Entscheidung beeinflusste direkt die Stimmung und damit die dramaturgische Struktur des Films?",
        answerA = "Er drehte ausschliesslich bei Tageslicht ohne kuenstliche Beleuchtung",
        answerB = "Er verwendete ausschliesslich Candlelight-Aufnahmen fuer Innenszenen mit Spezialobjektiven",
        answerC = "Er verzichtete vollstaendig auf Musik nach dem ersten Akt",
        answerD = "Er drehte chronologisch entsprechend dem Drehbuch, was extrem selten ist",
        correctAnswer = 1,
        explanation = "Kubrick liess Innenraumszenen ausschliesslich bei Kerzenlicht drehen, wofuer er NASA-Objektive verwendete (Zeiss 50mm f/0.7). Diese Entscheidung erzwang eine spezifische, langsame dramaturgische Rhythmisierung — Schauspieler mussten extrem ruhig agieren.",
        difficulty = 5,
        funFact = "Die NASA-Objektive wurden urspruenglich entwickelt, um bei minimalem Licht auf der Mondoberflaече zu fotografieren. Kubrick liess sie modifizieren, um auf eine normale Kamera zu passen. 'Barry Lyndon' gilt technisch als eine der kompliziertesten Produktionen der Filmgeschichte."
    ),

    // Question 47 - Ensembledrama Dialog
    Question(
        categoryId = 4,
        questionText = "Robert Altman entwickelte eine besondere Methode der Dialogregie, bei der mehrere Charaktere gleichzeitig sprechen. In welchem Film setzte er diese Technik am konsequentesten und beruehmt-beruechtigtsten ein?",
        answerA = "'Nashville' (1975)",
        answerB = "'M*A*S*H' (1970)",
        answerC = "'Short Cuts' (1993)",
        answerD = "'Gosford Park' (2001)",
        correctAnswer = 0,
        explanation = "'Nashville' (1975) gilt als Altmans radikalster Einsatz der Mehrkanal-Dialog-Technik: 24 Hauptfiguren, simultane Gespräche, die sich ueberlagern. Altman montierte die Tonspur spaeter selektiv, aber liess bewusst Ambiguitaet im Dialog.",
        difficulty = 5,
        funFact = "Fuer 'Nashville' liess Altman die Schauspieler ihre eigenen Countrysongs schreiben — ob sie Musiker waren oder nicht. Keith Carradine schrieb 'I'm Easy', der den Oscar fuer den Besten Originalsong gewann, und sang ihn im Film live."
    ),

    // Question 48 - Adaptation aus Kurzgeschichte
    Question(
        categoryId = 4,
        questionText = "Welcher Oscar-preisgekroente Film basiert auf einer Kurzgeschichte von weniger als zehn Druckseiten und gilt als eines der ausgedehntesten Expansionen einer literarischen Mini-Vorlage in der Filmgeschichte?",
        answerA = "'Brokeback Mountain' (2005) nach Annie Proulx",
        answerB = "'Minority Report' (2002) nach Philip K. Dick",
        answerC = "'Blade Runner' (1982) nach Philip K. Dick",
        answerD = "'The Birds' (1963) nach Daphne du Maurier",
        correctAnswer = 0,
        explanation = "'Brokeback Mountain' basiert auf Annie Proulx' Kurzgeschichte aus dem New Yorker von 1997, die etwa neun Druckseiten umfasst. Larry McMurtry und Diana Ossana expandierten sie zu einem 134-minuetigen Film und gewannen den Drehbuch-Oscar.",
        difficulty = 5,
        funFact = "Annie Proulx aeusserte nach der Oscar-Verleihung 2006, als 'Crash' den Hauptpreis gewann und 'Brokeback Mountain' leer ausging, ihrer Empörung in einem Artikel — einer der ungefilttertsten oeffentlichen Reaktionen einer Autorin auf eine Oscar-Entscheidung."
    ),

    // Question 49 - Autobiographisches Sorkin
    Question(
        categoryId = 4,
        questionText = "Aaron Sorkin hat das Schreiben selbst mehrfach als Thema in seinen Werken verarbeitet. Welches Buehnenwerk schrieb er, das direkt auf seiner eigenen Erfahrung mit Kokainabhaengigkeit basiert?",
        answerA = "'A Few Good Men'",
        answerB = "'The Farnsworth Invention'",
        answerC = "'Ariel Nimue'",
        answerD = "Er hat kein solches Werk veroeffentlicht",
        correctAnswer = 3,
        explanation = "Sorkin hat seine Drogenabhaengigkeit zwar oeffentlich diskutiert (er wurde 2001 mit Crack und Halluzinogenen verhaftet), aber kein direktes autobiographisches Buehnenwerk darueber geschrieben. Er thematisierte Sucht und Kreativdruck eher indirekt in seinen TV-Werken.",
        difficulty = 5,
        funFact = "Sorkin schrieb den Piloten von 'The West Wing' waehrend seiner schwersten Suchtperiode. Er hat gesagt, Schreiben sei fuer ihn eine Form von Kontrolle und Therapie gewesen — eine produktive Alternative zur Substanzabhaengigkeit."
    ),

    // Question 50 - Dialekt und Sprache im Drehbuch
    Question(
        categoryId = 4,
        questionText = "Coen-Brueder-Drehbuecher enthalten oft prazise regionale Dialekte und Sprechweisen. In 'Fargo' verwendeten sie den Minnesota-Dialekt ('Minnesota Nice'). Welcher Linguist beriet die Schauspieler fuer die authentische Wiedergabe?",
        answerA = "Die Coens verwendeten keinen Berater — sie sind selbst aus Minnesota",
        answerB = "Ein Dialektcoach von der University of Minnesota",
        answerC = "Frances McDormand recherchierte selbst ohne Berater vor Ort",
        answerD = "Die Coens hatten einen norwegisch-amerikanischen Sprachberater aus dem Mittleren Westen",
        correctAnswer = 0,
        explanation = "Joel und Ethan Coen sind aus Minnesota aufgewachsen (St. Louis Park) und verwendeten keinen externen Linguisten — der Dialekt war ihnen aus eigener Erfahrung vertraut. Sie schrieben ihn direkt in die Drehbuch-Regieanweisungen ein.",
        difficulty = 5,
        funFact = "Frances McDormand und die anderen Schauspieler besuchten Minnesota vor dem Dreh, um den Dialekt zu perfektionieren. McDormand gewann dafuer den Oscar als Beste Hauptdarstellerin — ein Triumph, bei dem ihr eigener Mann (Joel Coen) als Regisseur ausgezeichnet wurde: das erste Mal, dass ein Ehepaar gleichzeitig Oscars gewann."
    )

)
