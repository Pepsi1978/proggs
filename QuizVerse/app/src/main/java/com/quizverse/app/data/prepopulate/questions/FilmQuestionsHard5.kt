package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsHard5(): List<Question> = listOf(

    // --- VFX HISTORY: INDUSTRIAL LIGHT & MAGIC ---

    // Question 1 - ILM founding year
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr gruendete George Lucas Industrial Light & Magic (ILM)?",
        answerA = "1971",
        answerB = "1975",
        answerC = "1977",
        answerD = "1980",
        correctAnswer = 1, // B
        explanation = "George Lucas gruendete ILM 1975 in Van Nuys, Kalifornien, um die VFX fuer den ersten Star-Wars-Film zu produzieren. Das Studio wurde damit 2 Jahre vor dem Filmstart aufgebaut.",
        difficulty = 3,
        funFact = "ILM begann in einer Lagerhalle mit einem Team von jungen Filmemachern, die damals kaum Erfahrung mit Computereffekten hatten."
    ),

    // Question 2 - ILM first CGI creature
    Question(
        categoryId = 4,
        questionText = "Welcher Film enthielt 1985 die erste vollstaendig computeranimierte Sequenz in einem Spielfilm, die von ILM produziert wurde?",
        answerA = "Tron",
        answerB = "The Last Starfighter",
        answerC = "Young Sherlock Holmes",
        answerD = "Star Trek III",
        correctAnswer = 2, // C
        explanation = "In 'Young Sherlock Holmes' (1985) schuf ILM den ersten vollstaendig CGI-animierten Charakter in einem Realfilm: einen Ritter aus einem Buntglasfenster. Die Sequenz dauerte nur wenige Sekunden.",
        difficulty = 3,
        funFact = "Der CGI-Ritter brauchte 6 Monate Rechenzeit an den damals leistungsstarksten Computern und kostete mehr als eine Million Dollar."
    ),

    // Question 3 - ILM morphing technique T-1000
    Question(
        categoryId = 4,
        questionText = "Welche VFX-Technik wurde von ILM fuer den T-1000 in 'Terminator 2' (1991) entwickelt und revolutionierte die Branche?",
        answerA = "Motion Capture",
        answerB = "Particle Simulation",
        answerC = "Photorealistisches Morphing und fluessige Metalloberflaechenanimation",
        answerD = "Ray Tracing in Echtzeit",
        correctAnswer = 2, // C
        explanation = "ILM entwickelte fuer T2 eine Kombination aus digitalen Morph-Techniken und der Simulation fluessiger Metalloberflaechen. Diese Technik erforderte neue Software und setzte einen Standard fuer photorealistische CGI-Charaktere.",
        difficulty = 3,
        funFact = "Fuer den T-1000 musste ILM den Schauspieler Robert Patrick vollstaendig in ein 3D-Modell digitalisieren - ein Prozess, der Wochen dauerte."
    ),

    // Question 4 - Weta Digital founding
    Question(
        categoryId = 4,
        questionText = "Wann und von wem wurde Weta Digital in Neuseeland gegruendet?",
        answerA = "1989 von Peter Jackson und Richard Taylor",
        answerB = "1993 von Peter Jackson, Richard Taylor und Jamie Selkirk",
        answerC = "1996 von Peter Jackson allein",
        answerD = "2000 kurz vor dem Beginn der Herr-der-Ringe-Trilogie",
        correctAnswer = 1, // B
        explanation = "Weta Digital wurde 1993 von Peter Jackson, Richard Taylor und Jamie Selkirk in Wellington, Neuseeland, gegruendet. Das Studio war urspruenglich als kleines VFX-Haus gedacht, bevor die Herr-der-Ringe-Trilogie es weltbekannt machte.",
        difficulty = 3,
        funFact = "Weta Digital und Weta Workshop sind zwei getrennte Firmen - Weta Digital macht CGI, waehrend Weta Workshop physische Requisiten, Kostueme und Prothesen herstellt."
    ),

    // Question 5 - Weta Digital Massive software
    Question(
        categoryId = 4,
        questionText = "Welche eigens entwickelte Software nutzte Weta Digital fuer die Massenschlachten in 'Der Herr der Ringe', um zehntausende individuelle KI-Krieger zu simulieren?",
        answerA = "Renderman",
        answerB = "Massive",
        answerC = "Houdini",
        answerD = "RealFlow",
        correctAnswer = 1, // B
        explanation = "Weta Digital entwickelte 'Massive' (Multiple Agent Simulation System in Virtual Environment), eine KI-Software, bei der jeder digitale Soldat eigenstaendig auf seine Umgebung reagiert. Massive wurde spaeter kommerziell veroeffentlicht.",
        difficulty = 3,
        funFact = "In der Pelennor-Felder-Schlacht in 'Die Rueckkehr des Koenigs' wurden bis zu 200.000 KI-gesteuerte Charaktere gleichzeitig simuliert."
    ),

    // Question 6 - VFX: Jurassic Park dinosaurs
    Question(
        categoryId = 4,
        questionText = "Welches VFX-Prinzip setzte 'Jurassic Park' (1993) bezueglich der Kombination von CGI und praktischen Effekten um?",
        answerA = "Ausschliesslich CGI-Dinosaurier in allen Szenen",
        answerB = "Animatronics fuer alle Nahaufnahmen, CGI nur fuer Totalen",
        answerC = "CGI hauptsaechlich fuer bewegte Szenen, Animatronics fuer Nahaufnahmen und statische Momente",
        answerD = "Stop-Motion als Basis, digital nachbearbeitet",
        correctAnswer = 2, // C
        explanation = "Spielberg und ILM verwendeten CGI primaer fuer Szenen mit Bewegung und weiten Einstellungen, waehrend Stan Winstons Animatronics bei Nahaufnahmen und statischen Momenten eingesetzt wurden. Diese Hybrid-Methode galt jahrelang als Best Practice.",
        difficulty = 3,
        funFact = "Urspruenglich war Jurassic Park als Stop-Motion-Film geplant. ILM ueberzeugte Spielberg mit einem geheimen CGI-Test-Render eines laufenden T-Rex, der ihn zum Umdenken brachte."
    ),

    // --- VFX HISTORY: SPECIFIC TECHNIQUES ---

    // Question 7 - Bullet time Matrix
    Question(
        categoryId = 4,
        questionText = "Welche Technik steckt hinter dem 'Bullet Time'-Effekt in 'The Matrix' (1999) und wie viele Kameras wurden dafuer eingesetzt?",
        answerA = "Virtuelle Kamera mit CGI-Interpolation, keine echten Kameras",
        answerB = "120 stationaere Fotokameras im Halbkreis, kombiniert mit CGI-Erweiterungen",
        answerC = "12 hochwertige Filmkameras mit extremer Zeitlupe",
        answerD = "Greenscreen mit nachtraeglich animierter Kamera",
        correctAnswer = 1, // B
        explanation = "Fuer Bullet Time wurden 120 stationaere Fotokameras im Halbkreis um den Darsteller positioniert und synchron ausgeloest. Die Einzelbilder wurden danach digital zu einer fliessenden Bewegung zusammengesetzt. Die CGI-Erweiterung fuegte die virtuellen Kamerabewegungen hinzu.",
        difficulty = 3,
        funFact = "Die Technik basierte auf Ideen des franzoesischen Fotografen Michel Gondry und des VFX-Supervisors John Gaeta, der dafuer einen Oscar gewann."
    ),

    // Question 8 - LOTR scale doubles technique
    Question(
        categoryId = 4,
        questionText = "Welche Spezialeffekt-Technik nutzte Peter Jackson hauptsaechlich, um Hobbits kleiner als Menschen wirken zu lassen, bevor CGI eingesetzt wurde?",
        answerA = "Forced Perspective und Scale Doubles",
        answerB = "Digitale Verkleinerung im Post-Processing",
        answerC = "Ausschliesslich CGI-Ersatz der Schauspieler",
        answerD = "Speziell gebaute Kameralinsen mit Verzerrungseffekt",
        correctAnswer = 0, // A
        explanation = "Jackson verwendete klassische Forced Perspective (unterschiedliche Abstands- und Kamerawinkelkombinationen) sowie Scale Doubles - Schauspieler unterschiedlicher Koerpergroesse, die die Rollen in bestimmten Einstellungen uebernahmen. CGI wurde ergaenzend eingesetzt.",
        difficulty = 3,
        funFact = "Fuer Szenen am runden Tisch wurden spezielle Spezialeffekt-Schauspieler als Hobbit-Scale-Doubles eingesetzt, die exakt die Bewegungen der Hauptdarsteller nachahmen mussten."
    ),

    // --- FAMOUS MOVIE PROPS AND COSTUMES ---

    // Question 9 - Rosebud Citizen Kane
    Question(
        categoryId = 4,
        questionText = "Was ist aus dem originalen 'Rosebud'-Schlitten aus 'Citizen Kane' (1941) geworden?",
        answerA = "Er wird im Smithsonian Museum aufbewahrt",
        answerB = "Steven Spielberg kaufte ihn 1982 fuer 60.500 US-Dollar bei einer Auktion",
        answerC = "Er verbrannte bei einem Studiobrand in den 1950er Jahren",
        answerD = "Er befindet sich in der Orson-Welles-Gedenkstaette in Kenosha",
        correctAnswer = 1, // B
        explanation = "Steven Spielberg ersteigerte den originalen Rosebud-Schlitten 1982 fuer 60.500 US-Dollar. Es ist eines der beruehm testen Requisiten der Filmgeschichte. Welles liess mehrere Schlitten anfertigen, von denen einer tatsaechlich verbrennt.",
        difficulty = 3,
        funFact = "Orson Welles behauptete spaeter, das Wort 'Rosebud' sei seiner Meinung nach das billigste Filmdevice, das er je gesehen habe."
    ),

    // Question 10 - Dorothy ruby slippers
    Question(
        categoryId = 4,
        questionText = "In welchem Museum befindet sich das bekannteste erhaltene Paar der Ruby Slippers aus 'Der Zauberer von Oz' (1939)?",
        answerA = "Smithsonian National Museum of American History",
        answerB = "Academy Museum of Motion Pictures in Los Angeles",
        answerC = "Hollywood Museum",
        answerD = "Metropolitan Museum of Art",
        correctAnswer = 0, // A
        explanation = "Das bekannteste Paar der Ruby Slippers aus dem Original-Film befindet sich im Smithsonian National Museum of American History in Washington D.C., wohin es 1979 gespendet wurde. Es gibt mehrere erhaltene Paare.",
        difficulty = 3,
        funFact = "Die Schuhe sind mit echten roten Pailletten besetzt. Eines der Paare wurde 2005 gestohlen und erst 2018 wiedergefunden."
    ),

    // Question 11 - HAL 9000 lens
    Question(
        categoryId = 4,
        questionText = "Welches spezielle Kameraobjektiv verwendete Kubrick fuer die HAL-9000-Perspektive in '2001: Odyssee im Weltraum' (1968), um den Eindruck eines allsehenden Auges zu erzeugen?",
        answerA = "Canon 8mm Fisheye",
        answerB = "Nikkor 8mm f/8 Fisheye-Objektiv",
        answerC = "Zeiss 10mm Ultra-Weitwinkel",
        answerD = "Panavision Anamorphot 14mm",
        correctAnswer = 1, // B
        explanation = "Kubrick verwendete ein Nikkor 8mm f/8 Fisheye-Objektiv fuer HAL 9000s Point-of-View-Aufnahmen. Die extreme Weitwinkelverzerrung erzeugte den beunruhigenden Eindruck eines alles ueberwachenden, fast sphaerischen Blicks.",
        difficulty = 3,
        funFact = "Das rote HAL-9000-'Auge' ist in Wirklichkeit die Frontlinse des Fisheye-Objektivs, leicht rot beleuchtet. Kubrick hielt die technischen Details seiner Kamerawahl jahrzehntelang geheim."
    ),

    // Question 12 - Darth Vader costume
    Question(
        categoryId = 4,
        questionText = "Wer entwarf das originale Darth-Vader-Kostuem fuer 'Star Wars' (1977) und von welchem historischen Helm liess er sich dabei inspirieren?",
        answerA = "Ralph McQuarrie, inspiriert vom deutschen Stahlhelm des 2. Weltkriegs",
        answerB = "John Mollo, inspiriert vom Samurai-Helm",
        answerC = "Ralph McQuarrie konzipierte das Design, John Mollo realisierte das Kostuem - inspiriert von deutschem Stahlhelm und Samurai-Ruestung",
        answerD = "Lucas selbst, nach mittelalterlichen Rittern",
        correctAnswer = 2, // C
        explanation = "Ralph McQuarrie erstellte die ikonischen Concept-Art-Zeichnungen, John Mollo (spaeterer Oscar-Gewinner) baute das Kostuem. Der Helm kombiniert Elemente des deutschen Stahlhelms und japanischer Samurai-Ruestungen. Der Atemgeraeusch-Sound wurde von Ben Burtt kreiert.",
        difficulty = 3,
        funFact = "Das originale Vader-Helm-Masken-Set wurde 2019 fuer ueber 900.000 US-Dollar versteigert."
    ),

    // --- FILM MUSIC: LEITMOTIFS ---

    // Question 13 - John Williams leitmotif technique
    Question(
        categoryId = 4,
        questionText = "Aus welcher Musiktradition uebernahm John Williams die Leitmotiv-Technik fuer seine Filmkompositionen, und welcher Komponist gilt als Erfinder dieser Technik?",
        answerA = "Romantische Klaviermusik, Franz Liszt",
        answerB = "Romantische Oper, Richard Wagner",
        answerC = "Barocke Polyphonie, Johann Sebastian Bach",
        answerD = "Impressionismus, Claude Debussy",
        correctAnswer = 1, // B
        explanation = "Williams uebernahm die Leitmotiv-Technik direkt von Richard Wagners Musik-Drama-Konzept (Gesamtkunstwerk). Jede wichtige Figur, jedes Objekt und jede Idee erhaelt ein eigenes, wiederkehrendes musikalisches Motiv, das transformiert und variiert wird.",
        difficulty = 3,
        funFact = "Williams hat oeffentlich zugegeben, dass seine Star-Wars-Partitur strukturell Wagners Ring-Zyklus nachempfunden ist."
    ),

    // Question 14 - Hans Zimmer temp track Inception
    Question(
        categoryId = 4,
        questionText = "Welchen Song verlangsamte Hans Zimmer drastisch, um das beruehmt-gedehnte Braaam-Klangmotiv fuer 'Inception' (2010) zu erschaffen?",
        answerA = "'Non, Je Ne Regrette Rien' von Edith Piaf",
        answerB = "'La Vie en Rose' von Edith Piaf",
        answerC = "'Time' aus dem eigenen Score",
        answerD = "'Clair de lune' von Debussy",
        correctAnswer = 0, // A
        explanation = "Zimmer verlangsamte 'Non, Je Ne Regrette Rien' von Edith Piaf extrem (auf Bruchteile der normalen Geschwindigkeit) und transformierte die melodischen Intervalle in das dumpfe, bass-schwere Braaam-Motiv. Da der Song im Film eine Handlungsrolle spielt, war dies eine bewusste dramaturgische Verknuepfung.",
        difficulty = 3,
        funFact = "Das 'Braaam'-Trailerklischee, das seitdem in tausenden Filmtrailern auftaucht, geht direkt auf Zimmers Inception-Komposition zurueck."
    ),

    // Question 15 - Ennio Morricone technique
    Question(
        categoryId = 4,
        questionText = "Welche ungewoehnliche Instrumental-Technik setzte Ennio Morricone in seinen Sergio-Leone-Western ein, um den Klang der kargen Landschaft zu erzeugen?",
        answerA = "Ausschliesslich klassische Orchesterinstrumente ohne Besonderheiten",
        answerB = "Menschliche Stimme als Instrument, elektrische Gitarre, Peitschenknall und Pfeifen als melodische Elemente",
        answerC = "Synthesizer und Drum-Machines als Ersatz fuer echte Instrumente",
        answerD = "Indische Sitar und afrikanische Perkussion",
        correctAnswer = 1, // B
        explanation = "Morricone integrierte in seine Spaghetti-Western-Scores menschliche Stimmen als melodische Instrumente, elektrische Gitarren in ungewoehnlicher Stimmung, Peitschenknalle und Pfeifen als Melodiefuehrer. Dieser hybriden Klangpalette gab den Scores ihren einzigartigen Charakter.",
        difficulty = 3,
        funFact = "Morricone und Leone kannten sich seit der Grundschule und entwickelten eine Arbeitsweise, bei der Morricone die Musik oft VOR den Dreharbeiten komponierte, damit Leone beim Drehen mit der Musik im Ohr arbeiten konnte."
    ),

    // Question 16 - temp track usage
    Question(
        categoryId = 4,
        questionText = "Was bezeichnet der Begriff 'Temp Track' (Tempospur) in der Filmproduktion und welches Problem entsteht haeufig daraus?",
        answerA = "Die vorlaeufi ge Schnittfassung des Films ohne endgueltige Effekte",
        answerB = "Vorlaeufig verwendete Musik fremder Komponisten im Schnitt, an die Regisseur und Studio sich gewoehnen - was den endgueltigen Komponisten einengt",
        answerC = "Die Metronom-Markierungen fuer den endgueltigen Komponisten",
        answerD = "Testvorstellungen fuer Testpublikum vor der Fertigstellung",
        correctAnswer = 1, // B
        explanation = "Beim Schnitt wird oft Musik bekannter Filme als Temp Track eingesetzt. Regisseure und Studios gewoehnen sich so sehr daran, dass sie vom endgueltigen Komponisten erwarten, aehnlich zu klingen - was kreative Originalitaet einschraenkt. John Williams klagte, dass Regisseure manchmal den Temp Track bevorzugen.",
        difficulty = 3,
        funFact = "Der Temp Track fuer 'Space Odyssee 2001' enthielt klassische Stuecke von Strauss und Ligeti - Kubrick gefiel die Musik so gut, dass er sie als endgueltigen Score beibehielt und den beauftragten Komponisten Alex North abloeste."
    ),

    // Question 17 - Bernard Herrmann Psycho strings
    Question(
        categoryId = 4,
        questionText = "Welche ungewoehnliche Orchestrierung waehlte Bernard Herrmann fuer den Score von Hitchcocks 'Psycho' (1960) und warum?",
        answerA = "Nur Blechblaeseinstrumente fuer maximalen Horror-Effekt",
        answerB = "Ausschliesslich Streichorchester ohne jede andere Instrumentengruppe, teilweise um das Budget zu senken",
        answerC = "Nur Orgel und Cembalo fuer einen archaischen Klang",
        answerD = "Elektronische Musik kombiniert mit einem kleinen Kammerorchester",
        correctAnswer = 1, // B
        explanation = "Herrmann entschied sich bewusst fuer ein reines Streichorchester ohne Blaeser, Holzblaesser oder Perkussion. Teilweise war dies eine budgetaere Massnahme, teils eine aesthetische - der trockene, scharfe Streicherklang passte zu der schwarzweissen Bildsprache und der psychologischen Kaelte des Films.",
        difficulty = 3,
        funFact = "Die beruehmt-schrillen Geigen-Akkorde in der Duschszene wurden urspruenglich von Hitchcock abgelehnt, der die Szene ohne Musik wollte - erst nach einer Demo liess er sich ueberzeugen."
    ),

    // --- STUNT COORDINATION HISTORY ---

    // Question 18 - Yakima Canutt
    Question(
        categoryId = 4,
        questionText = "Welcher Stuntman gilt als Erfinder moderner Stunttechniken und entwickelte in den 1930er Jahren den sogenannten 'Fall under the wagon'-Stunt, der noch heute Grundlage fuer viele Gag-Techniken ist?",
        answerA = "Hal Needham",
        answerB = "Yakima Canutt",
        answerC = "Dar Robinson",
        answerD = "Glenn Wilder",
        correctAnswer = 1, // B
        explanation = "Yakima Canutt gilt als Vater der modernen Stuntarbeit. Er entwickelte zahlreiche grundlegende Techniken fuer Pferde- und Wagenstunts und arbeitete eng mit John Wayne zusammen. Er choreografierte u.a. die beruehmt-gefahrliche Wagenrennen-Sequenz in 'Ben-Hur' (1959).",
        difficulty = 3,
        funFact = "Yakima Canutt erhielt 1967 einen Ehren-Oscar fuer seine Beitraege zur Filmkunst als Stuntman und zweiten Regisseur."
    ),

    // Question 19 - Buster Keaton stunt philosophy
    Question(
        categoryId = 4,
        questionText = "Was war Buster Keatons Grundsatz bezueglich seiner eigenen Stuntarbeit und welche schwere Verletzung erlitt er beim Drehen von 'Steamboat Bill Jr.' (1928)?",
        answerA = "Er lehnte gefaehrliche Stunts ab und nutzte Doubles; er erlitt keine Verletzungen",
        answerB = "Er machte alle Stunts selbst; das Haus fiel auf ihn und er brach sich den Hals, ohne es zu merken",
        answerC = "Er machte alle Stunts selbst; er brach sich bei einem Sturz den Arm",
        answerD = "Er machte die meisten Stunts selbst, nutzte aber bei extremer Gefahr Doubles; er erlitt keine gravierenden Verletzungen",
        correctAnswer = 1, // B
        explanation = "Keatons beruehmtester Hausfassaden-Gag in 'Steamboat Bill Jr.' war echt: Die Fassade fiel tatsaechlich auf ihn, und nur ein kleines offenes Fenster rettete sein Leben. Jahre spaeter stellte ein Roentgenbild fest, dass er sich dabei den Hals gebrochen hatte - ohne es je bemerkt zu haben.",
        difficulty = 3,
        funFact = "Der Fassaden-Gag erforderte praezise Platzierung auf den Zentimeter: Der Spalt des Fensterrahmens liess gerade genug Platz, damit Keatons Koerper hindurchpasste."
    ),

    // Question 20 - Dar Robinson height record
    Question(
        categoryId = 4,
        questionText = "Fuer welchen Film fiel Stuntman Dar Robinson 1980 aus der hoechsten je ohne Sicherheitsnetz absolvierten Hoehe von ueber 220 Metern und in welcher Stadt fand dieser Stunt statt?",
        answerA = "Highpoint, Toronto",
        answerB = "The Stunt Man, Los Angeles",
        answerC = "Sharky's Machine, Atlanta",
        answerD = "Cobra, New York",
        correctAnswer = 0, // A
        explanation = "Dar Robinson sprang 1979/1980 fuer den Film 'Highpoint' in Toronto vom CN Tower - eine Hoehe von ca. 220 Metern. Obwohl ein Sicherheitsdraht vorhanden war, war es der hoechste beabsichtigte freie Fall eines Stuntmans fuer einen Film.",
        difficulty = 3,
        funFact = "Robinson galt als der erste Stuntman, der Stunts wissenschaftlich und ingenieurmaessig kalkulierte, bevor er sie durchfuehrte."
    ),

    // --- FILM CRITICISM HISTORY ---

    // Question 21 - Pauline Kael
    Question(
        categoryId = 4,
        questionText = "Bei welchem Magazin war Pauline Kael von 1968 bis 1991 als Filmkritikerin taetig und welchen Film rettete sie nach eigenem Bekunden durch eine einflussreiche Kritik vor dem Misserfolg?",
        answerA = "Time Magazine; 'Bonnie und Clyde'",
        answerB = "The New Yorker; 'Bonnie und Clyde'",
        answerC = "The New Yorker; 'The Godfather'",
        answerD = "Village Voice; '2001: Odyssee im Weltraum'",
        correctAnswer = 1, // B
        explanation = "Pauline Kael schrieb ab 1968 fuer 'The New Yorker'. Ihre begeisterte 9000-Woerter-Rezension von 'Bonnie und Clyde' (1967) erschien in einem Moment, als der Film von der Kritik weitgehend verrissen wurde - und gilt als mitentscheidend fuer seine Rehabilitierung und den Erfolg der New-Hollywood-Bewegung.",
        difficulty = 3,
        funFact = "Kaels Einfluss war so gross, dass man 'Paulettes' - Kritiker, die ihr kritisches Urteil uebernahmen - als eigene Gruppe wahrnahm. Sie und Andrew Sarris fuehrten einen jahrzehntelangen oeffentlichen Streit ueber die Auteur-Theorie."
    ),

    // Question 22 - Roger Ebert thumb
    Question(
        categoryId = 4,
        questionText = "Mit welchem Kollegen entwickelte Roger Ebert das beruehmt-gewordene Daumen-hoch/Daumen-runter-Bewertungssystem, und wie hiess ihre gemeinsame TV-Sendung urspruenglich?",
        answerA = "Richard Roeper; 'Ebert & Roeper'",
        answerB = "Gene Siskel; 'Sneak Previews'",
        answerC = "Gene Siskel; 'At the Movies'",
        answerD = "Owen Gleiberman; 'Siskel & Ebert'",
        correctAnswer = 1, // B
        explanation = "Roger Ebert und Gene Siskel vom Chicago Tribune entwickelten das Daumen-System in ihrer gemeinsamen Sendung 'Sneak Previews' (ab 1975 auf PBS). Die Sendung wurde spaeter zu 'Siskel & Ebert & The Movies' umbenannt.",
        difficulty = 3,
        funFact = "Ebert und Siskel mochten sich persoenlich oft nicht und stritten haeufig - was die Sendung gerade interessant machte. Nach Siskels Tod 1999 fand Ebert die Sendung nie mehr gleichwertig."
    ),

    // Question 23 - Cahiers du Cinema
    Question(
        categoryId = 4,
        questionText = "Welcher Verleih und welcher Herausgeber gruendete die Cahiers du Cinema 1951, und welche Theorie wurde in der Zeitschrift massgeblich entwickelt?",
        answerA = "Andre Bazin und Jacques Doniol-Valcroze; die Mise-en-Scene-Theorie",
        answerB = "Andre Bazin, Jacques Doniol-Valcroze und Lo Duca; die Auteur-Theorie",
        answerC = "Jean-Luc Godard und Francois Truffaut; die Nouvelle-Vague-Theorie",
        answerD = "Roger Vadim und Henri Langlois; die Montage-Theorie",
        correctAnswer = 1, // B
        explanation = "Die Cahiers du Cinema wurden 1951 von Andre Bazin, Jacques Doniol-Valcroze und Lo Duca gegruendet. Bazin und spaetere junge Kritiker (Godard, Truffaut, Chabrol) entwickelten die politique des auteurs - die Sichtweise des Regisseurs als kuenstlerischen Hauptautor eines Films.",
        difficulty = 3,
        funFact = "Fast alle wichtigen Regisseure der Nouvelle Vague (Godard, Truffaut, Chabrol, Rivette, Rohmer) schrieben zuerst fuer die Cahiers, bevor sie selbst Filmemacher wurden."
    ),

    // Question 24 - Andrew Sarris auteur theory
    Question(
        categoryId = 4,
        questionText = "Wie hiess Andrew Sarris' einflussreiches Buch von 1968, in dem er amerikanische Regisseure nach der Auteur-Theorie in Qualitaetsstufen einteilte?",
        answerA = "The American Cinema: Directors and Directions 1929-1968",
        answerB = "Notes on the Auteur Theory",
        answerC = "Hollywood Directors: A Critical Survey",
        answerD = "The Film Director as Superstar",
        correctAnswer = 0, // A
        explanation = "Sarris' Buch 'The American Cinema: Directors and Directions 1929-1968' (1968) klassifizierte amerikanische Regisseure in Kategorien von 'The Pantheon Directors' bis 'Straw Hats and Pompoms' und war jahrzehntelang das Standardwerk der Filmkritik in den USA.",
        difficulty = 3,
        funFact = "Pauline Kael griff Sarris in ihrem Essay 'Circles and Squares' (1963) scharf an und startete damit eine jahrzehntelange Fehde zwischen beiden."
    ),

    // --- GERMAN FILM HISTORY POST-2000 ---

    // Question 25 - Toni Erdmann
    Question(
        categoryId = 4,
        questionText = "Bei welchem Filmfestival hatte 'Toni Erdmann' (2016) von Maren Ade seine Weltpremiere und welche ungewoehnliche Auszeichnung erhielt der Film trotz seiner Laenge von 162 Minuten?",
        answerA = "Berlinale 2016; Goldener Baer",
        answerB = "Cannes 2016; FIPRESCI-Preis der Kritiker sowie Laengste Pressekonferenz-Ovation",
        answerC = "Cannes 2016; zwanzigjahrige Publikumsdebatte durch Standing Ovation von 14 Minuten",
        answerD = "Venedig 2016; Jurypreis der internationalen Kritiker",
        correctAnswer = 2, // C
        explanation = "Toni Erdmann hatte seine Weltpremiere in Cannes 2016 und erhielt dort eine Standing Ovation von 14 Minuten - eine der laengsten in der Geschichte des Festivals. Der Film gewann den FIPRESCI-Preis und zahlreiche Kritikerpreise, verlor den Palme d'Or jedoch ueberraschenderweise.",
        difficulty = 3,
        funFact = "Toni Erdmann wurde fuer den Academy Award als bester fremdsprachiger Film nominiert und verlor - viele Kritiker betrachten das als eine der groessten Oscar-Niederlagen der Geschichte."
    ),

    // Question 26 - Victoria one-shot
    Question(
        categoryId = 4,
        questionText = "Wie lange dauerte die tatsaechliche Drehzeit fuer den einzigen One-Shot-Take von Sebastian Schinajels 'Victoria' (2015), und an welchem Wochentag und zu welcher Uhrzeit wurde gedreht?",
        answerA = "138 Minuten; Samstagnacht ab 4:30 Uhr",
        answerB = "140 Minuten; Samstagnacht ab 4:00 Uhr bis 6:20 Uhr morgens",
        answerC = "138 Minuten; Freitagnacht ab Mitternacht",
        answerD = "140 Minuten; Donnerstagnacht ab 2:00 Uhr",
        correctAnswer = 1, // B
        explanation = "Victoria von Sebastian Schipper wurde in einem einzigen kontinuierlichen Take von ca. 138-140 Minuten gedreht - von Samstag ca. 4:00 Uhr bis 6:20 Uhr morgens in Berlin. Die Laenge des Takes entspricht exakt der Laenge des fertigen Films.",
        difficulty = 3,
        funFact = "Das Team hatte drei Versuche fuer den One-Shot geplant - der zweite Take wurde der endgueltige Film. Die Schauspieler kannten das Drehbuch, improvisierten aber viele Dialoge."
    ),

    // Question 27 - Oh Boy
    Question(
        categoryId = 4,
        questionText = "Von welchem Regisseur stammt der Berliner Schwarzweiss-Film 'Oh Boy' (2012) und welchen deutschen Filmpreis gewann er in welcher Kategorie?",
        answerA = "Christian Petzold; Lola fuer besten Film",
        answerB = "Jan Ole Gerster; Lola in sechs Kategorien inklusive Bester Film, Beste Regie und Bestes Drehbuch",
        answerC = "Sebastian Schipper; Lola fuer besten Schnitt",
        answerD = "Maren Ade; Bayerischer Filmpreis",
        correctAnswer = 1, // B
        explanation = "Oh Boy ist das Debuetregie-Werk von Jan Ole Gerster und gewann beim Deutschen Filmpreis 2013 sechs Lolas, darunter Bester Film, Beste Regie, Bestes Drehbuch und Bester Hauptdarsteller (Tom Schilling). Der Film zeigt einen Tag im Leben eines antriebslosen jungen Berliners.",
        difficulty = 3,
        funFact = "Oh Boy wurde bewusst in Schwarzweiss gedreht - Gerster war von New-Hollywood-Filmen und der Nouvelle Vague inspiriert und wollte Berlin zeitlos und literarisch zeigen."
    ),

    // Question 28 - Systemsprenger
    Question(
        categoryId = 4,
        questionText = "Was bedeutet der Begriff 'Systemsprenger' im sozialen Kontext, auf den sich der gleichnamige Film von Nora Fingscheidt (2019) bezieht?",
        answerA = "Ein Kind, das wegen psychischer Erkrankung aus jedem Betreuungssystem herausfallt und weitervermittelt werden muss",
        answerB = "Ein Kind, das gezielt versucht, staatliche Institutionen zu sabotieren",
        answerC = "Ein Jugendlicher, der das Schulsystem aktiv boykottiert",
        answerD = "Ein Kind in einem zerrissenen Familiensystem nach Scheidung",
        correctAnswer = 0, // A
        explanation = "Als 'Systemsprenger' bezeichnet die deutsche Kinder- und Jugendhilfe Kinder und Jugendliche, die aufgrund extremer Verhaltensweisen nicht dauerhaft in einem Betreuungssystem untergebracht werden koennen und staendig weitervermittelt werden. Fingscheidts Film zeigt das Schicksal der 9-jaehrigen Benni.",
        difficulty = 3,
        funFact = "Hauptdarstellerin Helena Zengel war bei den Dreharbeiten 9 Jahre alt und wurde fuer ihre Leistung bei zahlreichen internationalen Festivals ausgezeichnet. Spaeter spielte sie neben Tom Hanks in 'Neues aus der Welt'."
    ),

    // Question 29 - Christian Petzold
    Question(
        categoryId = 4,
        questionText = "In welcher deutschen Filmschule-Gruppe wurde Christian Petzold ausgebildet und wie nannte die Kritik seinen Regiestil, der sich durch Zurückhaltung und elliptisches Erzaehlen auszeichnet?",
        answerA = "Munchner Filmschule; Minimalismus",
        answerB = "DFFB Berlin; Berliner Schule",
        answerC = "HFF Potsdam-Babelsberg; Neuer Deutscher Film",
        answerD = "Hochschule fuer Gestaltung Karlsruhe; Neue Sachlichkeit",
        correctAnswer = 1, // B
        explanation = "Christian Petzold studierte an der DFFB (Deutsche Film- und Fernsehakademie Berlin) und gilt als Hauptvertreter der sogenannten Berliner Schule - eine Bewegung des deutschen Autorenfilms, die emotionale Zurueckhaltung, elliptisches Erzaehlen und realistische Milieus bevorzugt.",
        difficulty = 3,
        funFact = "Zur Berliner Schule zaehlen auch Angela Schanelec, Thomas Arslan und Ulrich Koehler. Die Bezeichnung 'Berliner Schule' wurde nicht von den Regisseuren selbst gepraegt, sondern von Filmkritikern."
    ),

    // --- DOCUMENTARY SUBGENRES ---

    // Question 30 - Cinema verite definition
    Question(
        categoryId = 4,
        questionText = "Welcher franzoesische Dokumentarfilmer praegte den Begriff 'Cinema Verite' und welches technische Merkmal war konstitutiv fuer diesen Stil?",
        answerA = "Jean Rouch; handheld Kamera, synchroner Ton, Verzicht auf Kommentarerzaehler",
        answerB = "Chris Marker; Super-8-Kamera und nachtraegliche Vertonung",
        answerC = "Dziga Vertov; Kamera-Auge ohne Inszenierung",
        answerD = "Frederick Wiseman; observationaler Stil ohne Interviews",
        correctAnswer = 0, // A
        explanation = "Jean Rouch praegte Cinema Verite (wahrer Film) - benannt nach Dziga Vertovs Kino-Prawda. Konstitutiv waren leichte tragbare Kameras (16mm), synchroner Direktton und das bewusste Einbeziehen des Filmers in die soziale Situation. Rouchs 'Chronik eines Sommers' (1961) gilt als Schluessel-werk.",
        difficulty = 3,
        funFact = "Cinema Verite unterscheidet sich von Direct Cinema: Beim Cinema Verite interagiert der Filmer mit den Gefilmten und provoziert Reaktionen, beim Direct Cinema (Leacock, Pennebaker) versucht man unsichtbar zu bleiben."
    ),

    // Question 31 - Riefenstahl Triumph des Willens
    Question(
        categoryId = 4,
        questionText = "Welche filmtechnische Innovation setzte Leni Riefenstahl in 'Triumph des Willens' (1935) ein, die spaeter Standard in der Dokumentarfilm-Inszenierung wurde?",
        answerA = "Verwendung von Unterwasser-Kameraeinheiten",
        answerB = "Kamerawagen auf Schienen fuer Fahraufnahmen, Aufzugkameras, Luftaufnahmen und choreografierte Masseninszenierungen mit gezielt platzierten Kameras",
        answerC = "Erstmaliger Einsatz von Ton-Dokumentation in einem politischen Film",
        answerD = "Farbfilm-Technik fuer ein politisches Ereignis",
        correctAnswer = 1, // B
        explanation = "Riefenstahl setzte im Triumph des Willens eine Reihe technischer Innovationen ein: Kamerakrane, Laufbahn-Dolly, Luftaufnahmen, untersichtige Einstellungen, choreografierte Kameraplaetze in der Menge. Viele dieser Techniken wurden spaeter im Sport- und Dokumentarfilm Standard.",
        difficulty = 3,
        funFact = "Riefenstahl hatte fuer den Nurnberger Parteitag 1934 ein Team von 170 Personen und 30 Kameras. Das Ereignis wurde teilweise fuer die Kamera inszeniert."
    ),

    // Question 32 - Essay film definition
    Question(
        categoryId = 4,
        questionText = "Was unterscheidet den Essayfilm als dokumentarisches Subgenre von anderen Dokumentarformen, und welcher Filmmaker gilt als einer der Hauptbegründer des filmischen Essays?",
        answerA = "Der Essayfilm vermeidet jede persoenliche Perspektive; Dziga Vertov",
        answerB = "Der Essayfilm verbindet subjektives Nachdenken, Kommentar und assoziative Bildmontage; Chris Marker",
        answerC = "Der Essayfilm basiert ausschliesslich auf historischem Archivmaterial; Michael Moore",
        answerD = "Der Essayfilm zeigt nur Interviews ohne Erzaehlerkommentar; Frederick Wiseman",
        correctAnswer = 1, // B
        explanation = "Der Essayfilm verbindet subjektive Reflexion (oft Stimme des Autors), nicht-lineares assoziatives Montieren und intellektuelles Nachdenken als Kernprinzipien. Chris Markers 'Sans Soleil' (1983) und 'La Jetee' (1962) gelten als Meisterwerke des Genres.",
        difficulty = 3,
        funFact = "Chris Marker vermied zeitlebens Fotos von sich selbst und Interviews - sein Gesicht ist kaum bekannt. Als Avatar fuer sich selbst nutzte er immer eine Katze namens Guillaume-en-Egypte."
    ),

    // Question 33 - Direct Cinema vs Cinema Verite
    Question(
        categoryId = 4,
        questionText = "D.A. Pennebakers Dokumentarfilm 'Dont Look Back' (1967) ueber Bob Dylans Englandtournee 1965 gilt als Schluesselwerk welches Dokumentarfilm-Stils?",
        answerA = "Cinema Verite nach Jean Rouch",
        answerB = "Direct Cinema nach Robert Drew und Richard Leacock",
        answerC = "Propaganda-Dokumentar nach Grierson-Tradition",
        answerD = "Performativer Dokumentarfilm",
        correctAnswer = 1, // B
        explanation = "Dont Look Back ist ein Paradebeispiel des Direct Cinema: Pennebaker und Richard Leacock filmten Dylan ohne Kommentar, ohne Inszenierung, ohne Interview-Fragen - als unsichtbare Beobachter. Die handheld Kamera, der Direktton und der Verzicht auf Erlaeuterungen sind typisch.",
        difficulty = 3,
        funFact = "Das beruehmt-gewordene Einstiegsbild von Dylan, der Pappkarten mit Liedtexten hochhaelt - bekannt als 'Subterranean Homesick Blues'-Clip - gilt als Vorlaeuf er des Musikvideos."
    ),

    // Question 34 - Flaherty Nanook
    Question(
        categoryId = 4,
        questionText = "Welche ethische Kontroverse umgibt Robert Flahertys 'Nanook of the North' (1922), der als erster wichtiger Langfilm-Dokumentarfilm gilt?",
        answerA = "Flaherty bezahlte die Inuit-Darsteller nicht",
        answerB = "Viele Szenen wurden inszeniert oder rekonstruiert, und der echte 'Nanook' hielt den Protagonisten-Namen gar nicht",
        answerC = "Der Film zeigte geheime Stammesrituale ohne Erlaubnis",
        answerD = "Flaherty klaute das Filmmaterial von anderen Expeditionen",
        correctAnswer = 1, // B
        explanation = "Nanook of the North galt als Dokumentarfilm, war aber stark inszeniert: Viele Jagdszenen wurden nachgestellt, der echte Protagonist hiess Allakariallak (nicht Nanook), und die 'Frau' war nicht seine echte Frau. Flaherty beeinflusste die Inuit, veraltete Jagdmethoden zu zeigen statt moderner.",
        difficulty = 3,
        funFact = "Trotz dieser Kritik gilt Flaherty als Vater des Dokumentarfilms - er entwickelte erstmals eine Langfilm-Dokumentarform mit Protagonisten und Dramaturgie."
    ),

    // Question 35 - Werner Herzog style
    Question(
        categoryId = 4,
        questionText = "Welchen Begriff praegte Werner Herzog fuer sein Verstaendnis von Wahrhaftigkeit im Dokumentarfilm, die er von blossem Faktentreue abgrenzt?",
        answerA = "Die 'Grosse Wahrheit' - ueber einzelne Fakten hinausgehende existenzielle Erkenntnis",
        answerB = "Das 'Ekstatische Wahre' - eine poetische, visionaere Wahrheit jenseits der bloszen Fakten",
        answerC = "Das 'Subjektive Wahre' - die persoenliche Wahrnehmung des Filmemachers",
        answerD = "Die 'Handwerkliche Wahrheit' - technisch praezise Wiedergabe der Realitaet",
        correctAnswer = 1, // B
        explanation = "Herzog unterscheidet zwischen 'accountant's truth' (buchhalterische Faktenwahrheit) und dem 'ecstatic truth' (ekstatische Wahrheit) - einer tieferen, visionaeren Wahrheit, die durch Inszenierung, Imagination und poetische Verdichtung erreichbar ist. Diese Philosophie rechtfertigt Herzogs Inszenierungen in Dokus.",
        difficulty = 3,
        funFact = "Herzogs Dokumentar-Essay 'Minnesota Declaration' (1999) fasst seine Filmphilosophie zusammen und wendet sich explizit gegen Cinema Verite."
    ),

    // --- VFX: FURTHER TECHNIQUES ---

    // Question 36 - ILM Star Wars practical miniatures
    Question(
        categoryId = 4,
        questionText = "Welche spezifische Kameratechnik entwickelte ILM fuer die Raumschlachtszenen in 'Star Wars' (1977), die zuvor so nicht existierte?",
        answerA = "Computer Generated Imagery fuer alle Raumschiffe",
        answerB = "Motion Control Photography - computergesteuerte Kamerabewegungen, die millimetergenau wiederholt werden konnten fuer die Komposition von Miniatur-Modellen",
        answerC = "Optische Drucker-Technologie aus dem 2. Weltkrieg",
        answerD = "Front Projection System mit Miniaturmodellen",
        correctAnswer = 1, // B
        explanation = "ILM entwickelte fuer Star Wars die Motion Control Photography wesentlich weiter: Computer steuerten und repetierten Kamerabewegungen mit Miniatur-Modellen pixelgenau, sodass verschiedene Einstellungen mit unterschiedlichen Elementen perfekt uebereinander komponiert werden konnten.",
        difficulty = 3,
        funFact = "Die urspruengliche Motion-Control-Anlage bei ILM hiess Dykstraflex, benannt nach dem VFX-Supervisor John Dykstra, der spaeter einen Oscar dafuer erhielt."
    ),

    // Question 37 - CGI water effects
    Question(
        categoryId = 4,
        questionText = "Welcher Film aus dem Jahr 1989 enthielt die erste ueberzeugende CGI-Wassersequenz und welches Studio produzierte den Effekt?",
        answerA = "The Abyss - ILM schuf den pseudopodalen Wassercharakter",
        answerB = "Leviathan - Weta Digital",
        answerC = "DeepStar Six - Digital Domain",
        answerD = "The Little Mermaid - Disney CGI-Abteilung",
        correctAnswer = 0, // A
        explanation = "The Abyss (1989, James Cameron) enthielt die erste ueberzeugend animierte fluessige CGI-Figur - den pseudopodalen Wassercharakter, der Menschengesichter imitiert. ILM entwickelte dafuer komplett neue Software fuer fluessige Oberflaechen-Simulationen.",
        difficulty = 3,
        funFact = "Die Wassersequenz in The Abyss dauerte im Film nur wenige Minuten, kostete aber Millionen und Monate Rechenzeit - sie gilt als direkter Vorlaeuf er der Morphing-Technologie in T2."
    ),

    // --- FILM MUSIC: SPECIFIC SCORES ---

    // Question 38 - Max Steiner King Kong
    Question(
        categoryId = 4,
        questionText = "Welcher Pionier der Filmmusik komponierte 1933 den Score zu 'King Kong' und etablierte damit eine Technik, bei der die Musik synchron auf jede Bewegung des Films reagiert?",
        answerA = "Erich Wolfgang Korngold",
        answerB = "Max Steiner",
        answerC = "Bernard Herrmann",
        answerD = "Dimitri Tiomkin",
        correctAnswer = 1, // B
        explanation = "Max Steiner komponierte den King-Kong-Score und setzte konsequent das 'Mickey Mousing' ein - die Synchronisierung der Musik auf jede Bewegung und jeden Schritt der Figuren im Bild. Obwohl haeufig kritisiert, etablierte diese Technik den Grundstein fuer orchestrale Filmmusik.",
        difficulty = 3,
        funFact = "Vor King Kong war Filmmusik oft nur ein Hintergrundscore ohne direkten Bezug zum Bild. Steiner gilt als erster Komponist, der Filmmusik als dramaturgisches Mittel verstand."
    ),

    // Question 39 - Philip Glass Koyaanisqatsi
    Question(
        categoryId = 4,
        questionText = "Welchen Kompositionsstil setzte Philip Glass in seinem Score fuer 'Koyaanisqatsi' (1982) ein, der dem Film seine meditative Qualitaet verleiht?",
        answerA = "Serielle Kompositionstechnik nach Schoenberg",
        answerB = "Minimal Music mit sich langsam verschiebenden Repetitionen und Arpeggios",
        answerC = "Aleatorische Komposition mit zufaelligen Noten-Ereignissen",
        answerD = "Freie Improvisation auf Basis von Hintergrundgeraeusch",
        correctAnswer = 1, // B
        explanation = "Glass verwendete seine charakteristische Minimal-Music-Technik: repetitive Figuren, langsam sich verschiebende harmonische Felder und kreisende Arpeggio-Strukturen. Der Score ist untrennbar mit Godfrey Reggios Zeitraffer-Bildern verwoben und gilt als Meisterwerk der Verbindung von Bild und Musik.",
        difficulty = 3,
        funFact = "Der Titel 'Koyaanisqatsi' stammt aus der Sprache der Hopi und bedeutet 'Leben ausser Balance' oder 'Leben, das eine andere Lebensweise verlangt'."
    ),

    // --- GERMAN FILM: SPECIFIC WORKS ---

    // Question 40 - Das Leben der Anderen
    Question(
        categoryId = 4,
        questionText = "Florian Henckel von Donnersmarck drehte 'Das Leben der Anderen' (2006) grossteils in welchem Gebaeude und welche historische Ironie enthielt der Drehort?",
        answerA = "Im originalen Stasi-Untersuchungsgefaengnis Berlin-Hohenschoenhausen, das zu diesem Zeitpunkt bereits Gedenkstaette war",
        answerB = "In rekonstruierten Sets in Babelsberg, da Originalschauplaetatze nicht zugaenglich waren",
        answerC = "In Wien, da keine geeigneten DDR-Gebaeude erhalten waren",
        answerD = "In Leipzig in echten Buerogebaeuden aus der DDR-Zeit",
        correctAnswer = 0, // A
        explanation = "Teile des Films wurden tatsaechlich im ehemaligen Stasi-Untersuchungsgefaengnis Berlin-Hohenschoenhausen gedreht, das seit 1994 Gedenkstaette ist. Die historische Ironie: Am selben Ort, wo die Stasi Menschen verhort hatte, wurde jetzt ein Film gegen die Stasi gedreht.",
        difficulty = 3,
        funFact = "Der Film gewann 2007 den Oscar als bester fremdsprachiger Film - einer der wenigen deutschen Filme, dem das gelang."
    ),

    // Question 41 - Good Bye Lenin continuity detail
    Question(
        categoryId = 4,
        questionText = "Welches bekannte Detail des DDR-Alltags spielt in 'Good Bye Lenin!' (2003) von Wolfgang Becker eine symbolische Hauptrolle, das der Held Alex fuer seine kranke Mutter authentisch reproduzieren muss?",
        answerA = "Der Trabant als Fahrzeug und Statussymbol",
        answerB = "Das DDR-Fernsehprogramm, Spreewaldgurken und abgekuendigte Ostprodukte",
        answerC = "Die Stasi-Akten der Familie",
        answerD = "Die Schuluniformen und Pionier-Halstuecher",
        correctAnswer = 1, // B
        explanation = "Alex muss fuer seine Mutter, die die Mauerofallnacht im Koma verbrachte, den Schein des Fortbestehens der DDR aufrechterhalten: Er jagt abgekuendigten DDR-Produkten wie Spreewaldgurken hinterher, produziert gefaelschte DDR-Fernsehnachrichten und versucht, jeden westlichen Einfluss zu verbergen.",
        difficulty = 3,
        funFact = "Der Film war ein grosser internationaler Erfolg und praegte den Begriff 'Ostalgie' (Nostalgie fuer die DDR) als kulturellen Begriff."
    ),

    // Question 42 - Run Lola Run cinematography
    Question(
        categoryId = 4,
        questionText = "Welche besonderen audiovisuellen Stilmittel setzte Tom Tykwer in 'Lola rennt' (1998) ein, um die drei Zeitschleifen formal voneinander zu differenzieren?",
        answerA = "Schwarzweiss vs. Farbe fuer verschiedene Zeitlinien",
        answerB = "Wechsel zwischen 35mm Film, Videobild und Animationssequenzen, verstaerkt durch schnellen Schnitt und Techno-Score",
        answerC = "Ausschliesslich Handkamera vs. statische Einstellungen",
        answerD = "Verschiedene Farbfilter fuer die drei Durchlaeufe",
        correctAnswer = 1, // B
        explanation = "Tykwer mischte bewusst verschiedene Bildtraeager: 35mm-Filmmaterial wechselt mit Videobild und kurzen Animationssequenzen. Diese formale Heterogenitaet wurde durch extremen Schnitt (kuerzeste Einstellungen) und den elektronischen Score unterstuezt und machte den Film zu einem stilistischen Meilenstein.",
        difficulty = 3,
        funFact = "Der Techno-Score wurde von Tykwer selbst gemeinsam mit Johnny Klimek und Reinhold Heil komponiert und vorab fertiggestellt, bevor gedreht wurde."
    ),

    // --- DOCUMENTARY SUBGENRES CONTINUED ---

    // Question 43 - Michael Moore approach
    Question(
        categoryId = 4,
        questionText = "Welchen Dokumentarfilm-Ansatz wird Michael Moore zugeschrieben und welche Kritik wird an seiner Arbeit methodisch geaeubert?",
        answerA = "Observationaler Direct-Cinema-Stil; er inszeniert zu wenig",
        answerB = "Performativer Agitations-Dokumentarfilm mit starker Praesenz des Autors; Selektive Faktenauswahl und manipulative Montage zugunsten der Aussage",
        answerC = "Klassische Interviewdokumentation; mangelnde Tiefe der Recherche",
        answerD = "Essayfilmischer Stil nach Chris Marker; zu literarisch und unverstaendlich",
        correctAnswer = 1, // B
        explanation = "Moore wird dem performativen Dokumentarfilm zugeordnet: Er selbst tritt als Protagonist auf, konfrontiert CEOs und Politiker direkt, und setzt Humor ein. Kritiker bemaaengeln, dass er Fakten selektiv einsetzt, Sequenzen chronologisch verfaelscht und Montage manipulativ nutzt - auch wenn seine Kernaussagen oft korrekt sind.",
        difficulty = 3,
        funFact = "Moores 'Fahrenheit 9/11' (2004) ist mit ca. 222 Millionen Dollar weltweit der kommerziell erfolgreichste Dokumentarfilm aller Zeiten."
    ),

    // Question 44 - Errol Morris
    Question(
        categoryId = 4,
        questionText = "Welche Interviewtechnik erfand Errol Morris und welchen Namen gab er ihr?",
        answerA = "Direct Camera - Interviewte schauen direkt in die Kamera",
        answerB = "Interrotron - ein Spiegelsystem, damit Interviewte gleichzeitig die Kameralinse und Morris' Gesicht anschauen",
        answerC = "Split Screen - Interviewer und Interviewter gleichzeitig sichtbar",
        answerD = "Teleprompt-Interview - Interviewte lesen Antworten ab ohne es zu merken",
        correctAnswer = 1, // B
        explanation = "Errol Morris erfand den Interrotron: Ein Spiegelsystem (ahnlich einem Teleprompter), das es dem Interviewten ermoeglicht, gleichzeitig direkt in die Kamera und in Morris' Augen zu schauen. Dadurch wirken Interviews intensiv persoenlich, weil der Blickkontakt direkt zur Kamera gehalten wird.",
        difficulty = 3,
        funFact = "Morris' 'The Thin Blue Line' (1988) ist eines der wenigen Dokumentarfilme, das tatsaechlich zur Freilassung eines zu Unrecht Verurteilten beitrug."
    ),

    // --- FILM MUSIC: FURTHER ---

    // Question 45 - Nino Rota Godfather waltz
    Question(
        categoryId = 4,
        questionText = "Welcher Komponist schrieb den Score zu 'Der Pate' (1972) und warum wurde er bei der Oscar-Nominierung zunaechst disqualifiziert?",
        answerA = "Bernard Herrmann; wegen Plagiats-Vorwurfen",
        answerB = "Nino Rota; weil Teile der Musik aus einem frueheren Rota-Film stammten",
        answerC = "Ennio Morricone; wegen eines Namensstreits mit Paramount",
        answerD = "Henry Mancini; aufgrund einer technischen Einreichungsfrist",
        correctAnswer = 1, // B
        explanation = "Nino Rota wurde zunaechst von der Oscar-Nominierung ausgeschlossen, weil die Love-Theme-Melodie aus einem frueheren Film von ihm ('Fortunella', 1958) stammte. Er wurde spaeter fuer 'Der Pate Teil II' (1974) nominiert und gewann den Oscar.",
        difficulty = 3,
        funFact = "Rota arbeitete jahrzehntelang mit Federico Fellini zusammen und komponierte Scores fuer fast alle Fellini-Filme, darunter 8 1/2 und La Dolce Vita."
    ),

    // Question 46 - Koyaanisqatsi Reggio
    Question(
        categoryId = 4,
        questionText = "Godfrey Reggios 'Koyaanisqatsi' (1982) gehoert zu welchem Dokumentarfilm-Subgenre und auf welche besondere Weise fehlt jeder klassische Dokumentarfilm-Bestandteil?",
        answerA = "Propaganda-Dokumentar; es fehlen Fakten und Daten",
        answerB = "Essayfilm / Non-narrativer Dokumentarfilm; es gibt weder Kommentar noch Interviews noch klassische Narration - nur Bilder und Musik",
        answerC = "Cinema Verite; es fehlt die typische Interaktion des Filmers",
        answerD = "Mockumentary; es stellt sich als Dokumentarfilm aus, ist aber fiktional",
        correctAnswer = 1, // B
        explanation = "Koyaanisqatsi verzichtet vollstaendig auf Kommentar, Interviews, Intertitel oder narrative Struktur. Es ist ein reiner Bild-Musik-Film, der durch Zeitraffersequenzen, Zeitlupen und Philip Glass' Score eine meditativ-apokalyptische Aussage ueber das Verhaeltnis von Mensch und Natur macht.",
        difficulty = 3,
        funFact = "Reggio finanzierte den Film durch Spenden und unterstuetzte sich jahrelang selbst. Das Projekt begann als Anzeigenkampagne und entwickelte sich erst zum Film."
    ),

    // --- STUNT HISTORY: FURTHER ---

    // Question 47 - Hal Needham Cannonball Run
    Question(
        categoryId = 4,
        questionText = "Hal Needham war nicht nur Stunt-Koordinator, sondern auch Regisseur. Welcher beruehm te Film machte ihn als Regisseur bekannt und welche Stunt-Legende war sein langjaeaehriger Partner?",
        answerA = "Mad Max; Mel Gibson",
        answerB = "Smokey and the Bandit (1977); Burt Reynolds",
        answerC = "Cannonball Run; Sylvester Stallone",
        answerD = "Death Race 2000; Roger Corman",
        correctAnswer = 1, // B
        explanation = "Hal Needham war Stuntdouble und bester Freund von Burt Reynolds. Sein Regiedebuet 'Smokey and the Bandit' (1977) mit Reynolds wurde ein Riesenerfolg. Needham gilt als einer der letzten grossen Stunt-Koordinatoren der pre-CGI-Aera.",
        difficulty = 3,
        funFact = "Needham soll laut eigener Aussage bei Stunts insgesamt 56 Knochen gebrochen haben. Er erhielt 2012 einen Ehren-Oscar fuer seine Verdienste um den Stuntbereich."
    ),

    // Question 48 - Michelle Yeoh stunt training
    Question(
        categoryId = 4,
        questionText = "In welcher Hinsicht unterschied sich Michelle Yeohs Herangehensweise an Stunt-Arbeit in hongkonger Actionfilmen der 1980er und 90er von typischen westlichen Filmproduktionen?",
        answerA = "Sie nutzte als erste asiatische Schauspielerin westliche Stunt-Doubles",
        answerB = "Sie trainierte und fuerhrte alle Stunts selbst aus, ohne Double - ein in Hongkong uebliches Vorgehen, das von westlichen Studios kaum toleriert wird",
        answerC = "Sie verzichtete komplett auf Stuntarbeit und bevorzugte CGI",
        answerD = "Sie war ausgebildete Kampfsportmeisterin mit nationalen Titeln",
        correctAnswer = 1, // B
        explanation = "In der hongkonger Filmtradition (Jackie Chan, Sammo Hung) war es ueblich, dass Hauptdarsteller alle Stunts selbst ausfuehren. Yeoh trainierte jahrelang und erlitt bei Dreharbeiten ernsthafte Verletzungen - ein im westlichen Film durch Versicherungen und Studios kaum zu replizierendes Modell.",
        difficulty = 3,
        funFact = "Yeoh erlitt beim Drehen von 'Police Story 3: Supercop' (1992) mit Jackie Chan eine ernste Knieverletzung. Trotzdem fuehrte sie alle Stunts durch, darunter einen Sprung auf einen fahrenden Zug."
    ),

    // --- FILM CRITICISM CONTINUED ---

    // Question 49 - Siegfried Kracauer
    Question(
        categoryId = 4,
        questionText = "Welche These vertritt Siegfried Kracauer in seinem Werk 'Von Caligari zu Hitler' (1947) ueber das Kino der Weimarer Republik?",
        answerA = "Das Weimarer Kino war technisch ueberlegen, aber politisch neutral",
        answerB = "Die deutschen Stummfilme der Weimarer Republik spiegelten kollektive psychologische Dispositionen, die den Aufstieg Hitlers erklaerbar machen",
        answerC = "Das expressionistische Kino war eine direkte Reaktion auf nationalsozialistische Propaganda",
        answerD = "Weimarer Kino war primaer vom amerikanischen Film beeinflusst",
        correctAnswer = 1, // B
        explanation = "Kracauer argumentiert, dass Stummfilme wie Das Cabinet des Dr. Caligari, Nosferatu und M kollektive Traumata, Autoritaetshoerigkeit und Unterwerfungsphantasien widerspiegelten - und damit die psychologischen Bedingungen vorabbildeten, die Hitlers Aufstieg ermoeglichten. Die These ist einflussreich, aber auch umstritten.",
        difficulty = 3,
        funFact = "Kracauer schrieb das Buch im amerikanischen Exil, wohin er vor den Nationalsozialisten geflohen war. Seine Analyse ist doppelt gepraeagt durch persoenliche Betroffenheit und kritische Distanz."
    ),

    // Question 50 - Sontag on film
    Question(
        categoryId = 4,
        questionText = "In welchem Essay kritisierte Susan Sontag 1996 den Zustand des Kinos und was war ihre Kernthese bezueglich der Filmkultur?",
        answerA = "She kritisierte ausschliesslich Hollywood fuer kommerzielle Ueberwaeaeltigung",
        answerB = "In 'The Decay of Cinema' klagte sie den Tod der cinephilen Liebe zum Film, ausgeloest durch veraenderte Sehegewohnheiten (Fernsehen, Video) und den Rueckzug aus dem Kinosaals als rituellem Erlebnis",
        answerC = "In 'Against Interpretation' sprach sie sich gegen Filmkritik als solche aus",
        answerD = "Sie forderte das Ende des Autorenkinos zugunsten kollektiver Filmproduktion",
        correctAnswer = 1, // B
        explanation = "In 'The Decay of Cinema' (New York Times, 1996) beklagte Sontag den Tod der Cinephilie: Die rituelle Erfahrung des Kinobesuchs, die intensive Filmliebe einer bestimmten Generation, das ernsthafte Nachdenken ueber Film - all das sei durch Videokassetten, Fernsehen und postmoderne Gleichgueltigkeit verdraengt worden.",
        difficulty = 3,
        funFact = "Sontags Essay erschien 100 Jahre nach der ersten oeffentlichen Kinoprojektion der Lumiere-Brueder - ein bewusst gewaoehltes symbolisches Datum."
    ),

    // Question 51 - Fatih Akin integration theme
    Question(
        categoryId = 4,
        questionText = "Welchen thematischen Bogen spannt Fatih Akins informelle 'Liebe-Tod-Teufel'-Trilogie (auch bekannt als 'Zwischen den Welten'-Trilogie) und welche drei Filme gehoeren dazu?",
        answerA = "Jugend, Arbeit, Alter - Kurz und schmerzlos, Solino, Im Juli",
        answerB = "Liebe (Gegen die Wand), Tod (Auf der anderen Seite), Teufel (Soul Kitchen) - dreifache Auseinandersetzung mit deutsch-tuerischen Existenzen",
        answerC = "Hamburg, Istanbul, New York - drei Staedte als Schauplaetatze",
        answerD = "Vergangenheit, Gegenwart, Zukunft - historische Trilogie",
        correctAnswer = 1, // B
        explanation = "Akins Trilogie Liebe-Tod-Teufel besteht aus 'Gegen die Wand' (2004, Liebe), 'Auf der anderen Seite' (2007, Tod) und 'Soul Kitchen' (2009, Teufel). Sie untersucht deutschtuerische Identitaeten, Grenzerfahrungen zwischen Kulturen und Generationen - und zeigt das Potenzial eines europaeischen Migrantenfilms.",
        difficulty = 3,
        funFact = "'Gegen die Wand' gewann 2004 den Goldenen Baer der Berlinale und gilt als Wendepunkt im deutschen Migrantenfilm - erstmals wurde ein Film ueber deutsch-tuerische Erfahrungen ohne folkloristischen Blick von aussen gedreht."
    )

)
