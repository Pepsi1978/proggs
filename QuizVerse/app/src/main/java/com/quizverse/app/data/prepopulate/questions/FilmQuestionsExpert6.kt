package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsExpert6(): List<Question> = listOf(

    // --- KAMERATECHNIKEN ---

    // Question 1 - Steadicam-Erfindung
    Question(
        categoryId = 4,
        questionText = "Garrett Brown erfand die Steadicam und stellte sie erstmals 1976 oeffentlich vor. In welchem Film wurde sie zum ersten Mal in einem grossen Studioproduktion eingesetzt?",
        answerA = "Rocky (1976)",
        answerB = "Bound for Glory (1976)",
        answerC = "Marathon Man (1976)",
        answerD = "Network (1976)",
        correctAnswer = 1,
        explanation = "Haskell Wexlers 'Bound for Glory' (1976) war die erste grosse Studioproduktion, die die Steadicam einsetzte. Garrett Brown bediente das Geraet selbst. Die beruehmt gewordene Rocky-Sequenz auf den Stufen des Philadelphia Museum of Art erschien zwar im selben Jahr, wurde aber nach Bound for Glory gedreht.",
        difficulty = 4,
        funFact = "Garrett Brown entwickelte die Steadicam urspruenglich in seiner Garage. Er liess sich von der Idee inspirieren, nach einem Autounfall moeglich wirkungslose Verwacklungen bei Handkamera-Aufnahmen zu eliminieren."
    ),

    // Question 2 - Kubrick Steadicam The Shining
    Question(
        categoryId = 4,
        questionText = "Stanley Kubrick nutzte fuer 'The Shining' (1980) die Steadicam auf eine besondere Weise. Welche Aufnahmetechnik kombinierte er mit der Steadicam, um das Kind auf dem Dreirad zu filmen?",
        answerA = "Er liess die Kamera in Huefthoehe laufen mit angepasster Stativmontage",
        answerB = "Er montierte die Steadicam auf einen Rollstuhl in niedriger Position",
        answerC = "Er nutzte einen speziell angefertigten Kinderwagen-Dolly als Traeger",
        answerD = "Die Kamera wurde von einem liegenden Kameramann auf Hoehe des Dreirads gehalten",
        correctAnswer = 1,
        explanation = "Fuer die beruehmt gewordenen Szenen mit Danny auf dem Dreirad wurde die Steadicam auf einem modifizierten Rollstuhl montiert, der auf Kniehoehe des Kindes abgesenkt war. Garrett Brown bediente das System, das so eine gespenstisch gleichmaessige Verfolgung in Bodennaeher ermoeglichte.",
        difficulty = 4,
        funFact = "Kubrick liess einige dieser Flurszenen bis zu 35 Mal wiederholen. Der Junge, Danny Lloyd, wusste beim Dreh nicht, dass er in einem Horrorfilm spielte — Kubrick beschrieb den Film ihm gegenueber als ein Geisterdrama fuer Erwachsene."
    ),

    // Question 3 - Dolly Zoom / Vertigo-Effekt
    Question(
        categoryId = 4,
        questionText = "Der 'Dolly Zoom' oder 'Vertigo-Effekt' entstand durch Gegenbewegung von Kamera und Zoom. Wer entwickelte diesen Effekt, bevor Hitchcock ihn 1958 beruehmtmachte?",
        answerA = "Gregg Toland bei 'Citizen Kane'",
        answerB = "Irmin Roberts bei einem Universal-Film",
        answerC = "Jack Cardiff bei einem Powell/Pressburger-Film",
        answerD = "James Wong Howe bei 'Picnic'",
        correctAnswer = 1,
        explanation = "Irmin Roberts, ein Kameratechniker bei Universal Pictures, entwickelte den Dolly-Zoom-Effekt und setzte ihn erstmals ein, bevor Alfred Hitchcock ihn 1958 fuer 'Vertigo' verwendete. Roberts hatte den Effekt mit einem umgebauten Stativobjektiv entwickelt.",
        difficulty = 4,
        funFact = "Der Dolly-Zoom heisst auch 'Trombone Shot', weil er aussieht wie das Aus- und Einfahren einer Posaune. Steven Spielberg verwendete ihn in 'Der Weisse Hai' so wirkungsvoll, dass er seitdem als 'Jaws Shot' bekannt ist."
    ),

    // Question 4 - IMAX-Kamera
    Question(
        categoryId = 4,
        questionText = "Die IMAX-15/65-Kamera verwendet 15-Perforation-65mm-Film und erzeugt damit das groesste Filmformat der Welt. Wie viel Filmflaeche hat ein einzelnes IMAX-Filmbild im Vergleich zu normalem 35mm?",
        answerA = "Dreimal so gross",
        answerB = "Siebenmal so gross",
        answerC = "Zehnmal so gross",
        answerD = "Zwanzigmal so gross",
        correctAnswer = 2,
        explanation = "Ein IMAX-Bild auf 15/65mm-Film hat eine Flaeche von rund 69,6 x 48,5 mm, was ca. zehn Mal so gross ist wie ein Standard-35mm-Filmbild (24,89 x 18,67 mm). Dieses gigantische Negativ ermoeglicht extreme Bildschaerfe und Detaildichte auch auf riesigen Leinwaenden.",
        difficulty = 4,
        funFact = "Eine einzelne IMAX-Kamera wiegt etwa 23 kg und ist so gross und laut, dass sie bei Dialogszenen oft nicht eingesetzt werden kann. Christopher Nolan liess fuer 'Dunkirk' speziell gebaeudegrossen Filmrollen anfertigen, weil IMAX-Filmrollen sonst nur wenige Minuten Spielzeit liefern."
    ),

    // Question 5 - Anamorphotisches Objektiv
    Question(
        categoryId = 4,
        questionText = "Anamorphotische Objektive quetschen das Bild horizontal beim Filmen und dehnen es bei der Projektion wieder. Welchen visuellen Nebeneffekt erzeugen sie, der heute als Stilmittel gilt?",
        answerA = "Radiale Unschaerfe an den Bildraendern",
        answerB = "Horizontale Bokeh-Streifen ('Lens Flares')",
        answerC = "Blaue Farbverschiebung in den Schatten",
        answerD = "Doppelkontur bei hellen Quellen",
        correctAnswer = 1,
        explanation = "Anamorphotische Objektive erzeugen charakteristische horizontale Lichtstreifen ('anamorphic lens flares') bei hellen Lichtquellen im Bild, die durch die elliptische Optik entstehen. Diese wurden in der Digitalaera von Regisseuren wie J.J. Abrams bewusst als Stilmittel eingesetzt.",
        difficulty = 4,
        funFact = "Das anamorphotische Verfahren wurde vom franzoesischen Forscher Henri Chretien entwickelt und von CinemaScope lizenziert. 20th Century Fox kaufte 1952 die Rechte und startete damit die Scope-Aera des Breitwandkinos."
    ),

    // Question 6 - Dutch Angle
    Question(
        categoryId = 4,
        questionText = "Der 'Dutch Angle' (geneigte Kamera) wurde massgeblich vom deutschen Expressionismus beeinflusst. Welcher Begriff beschreibt im Deutschen dasselbe Stilmittel technisch korrekt?",
        answerA = "Schraegkameraachse",
        answerB = "Kameraneigung",
        answerC = "Hollaendischer Winkel",
        answerD = "Suedafrikanische Neigetechnik",
        correctAnswer = 0,
        explanation = "Der 'Dutch Angle' heisst auf Englisch 'Dutch' nicht wegen der Niederlande, sondern leitet sich vom deutschen 'Deutsch' ab — die englische Aussprache der fruehen Filmepoche. Der korrekte deutsche Fachbegriff ist 'Schraegkameraachse' oder 'Kantung'. Das Stilmittel kam aus dem deutschen Stummfilmexpressionismus.",
        difficulty = 4,
        funFact = "Der Dutch Angle wurde so sehr zum Klischee fuer 'Boese Charaktere' und 'psychologische Instabilitaet', dass das Fernsehprogramm 'Batman' (1966) ihn fast in jeder Szene mit den Schurken einsetzte, um sie von den Helden visuell zu unterscheiden."
    ),

    // --- SPEZIALEFFEKTE ---

    // Question 7 - Miniatureffekte Blade Runner
    Question(
        categoryId = 4,
        questionText = "Die futuristische Skyline von Los Angeles in Ridley Scotts 'Blade Runner' (1982) war ein aufwendiges Miniaturmodell. Wie wurde der atmosphaerische Smog-Effekt in den Miniaturaufnahmen erzeugt?",
        answerA = "Mit Trockeneisnebel in einem geschlossenen Filmstudio",
        answerB = "Mit Zigarettenrauch und Glycerin-Nebel unter Glasscheiben",
        answerC = "Mit Oeldampf und einem modifizierten Aquarium-Nebelgeraet",
        answerD = "Mit Milch, die in ein mit Wasser gefuelltes Tank geschuettet wurde",
        correctAnswer = 1,
        explanation = "Das Visual Effects-Team von 'Blade Runner' erzeugte den charakteristischen Smog-Dunst in den Miniaturaufnahmen durch eine Kombination aus Zigarettenrauch und Glycerin-Nebel, der unter Glasscheiben unterhalb der Miniaturbauten eingefangen wurde. Dies gab den Stadtaufnahmen ihre beruehmt diesige Atmosphaere.",
        difficulty = 4,
        funFact = "Die Blade Runner-Miniaturmodelle wurden so detailliert gebaut, dass einige davon eine Hoehe von ueber drei Metern hatten. Die Hauptmodelle lagen teilweise auf dem Boden und wurden von oben gefilmt, mit umgekehrter Beleuchtung fuer die Fensser."
    ),

    // Question 8 - Motion Control Kamera Star Wars
    Question(
        categoryId = 4,
        questionText = "Fuer 'Star Wars' (1977) entwickelte Industrial Light & Magic das 'Dykstraflex'-System, das praezise wiederholbare Kamerabewegungen fuer Composite-Shots ermoeglichte. Nach wem ist es benannt?",
        answerA = "Richard Edlund",
        answerB = "Dennis Muren",
        answerC = "John Dykstra",
        answerD = "Phil Tippett",
        correctAnswer = 2,
        explanation = "Das Dykstraflex-System ist nach John Dykstra benannt, dem Leiter der Spezialeffekte bei ILM fuer den ersten Star Wars-Film. Es war ein computergesteuerter Motion-Control-Kamerakopf, der exakt dieselbe Bewegung mehrfach wiederholen konnte, was das Compositing mehrerer Raumschiff-Elemente ermoeglichte.",
        difficulty = 4,
        funFact = "Dykstra gewann fuer seine Arbeit an Star Wars einen Ehren-Oscar fuer technische Innovation. Er verliess ILM danach und gruendete Apogee Productions. Das Dykstraflex-System revolutionierte Spezialeffekte so grundlegend, dass es den Begriff 'Photorealistic VFX' erst moeglich machte."
    ),

    // Question 9 - CGI Meilenstein Jurassic Park
    Question(
        categoryId = 4,
        questionText = "In 'Jurassic Park' (1993) waren die dinosaurierhaften CGI-Szenen revolutionaer. Wie viele Minuten CGI-Footage enthielt der Film tatsaechlich?",
        answerA = "Etwa 4 Minuten",
        answerB = "Etwa 14 Minuten",
        answerC = "Etwa 25 Minuten",
        answerD = "Etwa 40 Minuten",
        correctAnswer = 0,
        explanation = "Trotz seiner revolutionaeren Wirkung enthielt 'Jurassic Park' nur etwa 4 Minuten und 15 Sekunden echter CGI-Footage. Der Rest der Dinosaurier-Aufnahmen wurde mit animatronischen Modellen von Stan Winston Studios realisiert. Die Kombination aus beidem taeuscht weit mehr CGI vor, als tatsaechlich vorhanden ist.",
        difficulty = 4,
        funFact = "Phil Tippett, verantwortlich fuer die Go-Motion-Puppets (Weiterentwicklung des Stop-Motion), sollte urspruenglich alle Dinosaurier animieren. Als er das fruehe CGI-Test-Material sah, sagte er: 'Ich bin am Aussterben'. Spielberg liess diesen Satz als Dialog in den Film einfliessen."
    ),

    // Question 10 - Bullet Time Matrix
    Question(
        categoryId = 4,
        questionText = "Der 'Bullet Time'-Effekt in 'Matrix' (1999) entstand durch eine Anordnung von Kameras. Wie viele Standbildkameras wurden fuer die beruehmt gewordene Szene mit Neo auf dem Dach eingesetzt?",
        answerA = "12 Kameras",
        answerB = "52 Kameras",
        answerC = "120 Kameras",
        answerD = "420 Kameras",
        correctAnswer = 2,
        explanation = "Fuer die Dach-Bullet-Time-Sequenz wurden 120 Standbildkameras in einem Bogen rund um den Schauspieler angeordnet, die in schneller Abfolge ausloesten. Die entstandenen Einzelbilder wurden dann digital zusammengefuegt und interpoliert, um die fliessende Zeitlupen-Kamerabewegung zu erzeugen.",
        difficulty = 4,
        funFact = "Die Idee des Bullet Time basiert auf einem Konzept aus dem Anime 'Ghost in the Shell' (1995) sowie Arbeiten des Fotografen Tim MacMillan, der bereits in den 1980er Jahren mit Zeitschleifen-Fotografie experimentierte. Die Wachowskis zitierten MacMillan als Inspiration."
    ),

    // Question 11 - Praktische Effekte Inception
    Question(
        categoryId = 4,
        questionText = "Die beruehmt rotierende Hotelkorridor-Szene in 'Inception' (2010) wurde hauptsaechlich praktisch realisiert. Wie lange dauerte der Bau des rotierenden Sets?",
        answerA = "Vier Wochen",
        answerB = "Sechs Wochen",
        answerC = "Neun Wochen",
        answerD = "Dreizehn Wochen",
        correctAnswer = 0,
        explanation = "Der rotierende Hotelkorridor fuer 'Inception' wurde in vier Wochen gebaut und war acht Stockwerke lang. Das gesamte Set rotierte um 360 Grad, angetrieben von grossen Elektromotoren. Joseph Gordon-Levitt trainierte wochenlang, um die komplexen Kampfchoreographien in dem rotierenden Raum durchzufuehren.",
        difficulty = 4,
        funFact = "Das rotierende Set war so schwer, dass der Boden aus Stahl gefertigt werden musste. Gordon-Levitt musste alle Stunts selbst durchfuehren, da ein Double in dem engen, sich drehenden Korridor zu gefaehrlich gewesen waere. Er verletzte sich dabei mehrfach leicht."
    ),

    // Question 12 - Matte Painting Gone with the Wind
    Question(
        categoryId = 4,
        questionText = "Die beruehmt silhouettierten Baumszene in 'Vom Winde verweht' (1939), in der Scarlett O'Hara gegen den roten Himmel steht, ist ein Matte Painting. Wer schuf dieses Gemaelder?",
        answerA = "Albert Whitlock",
        answerB = "Jack Cosgrove",
        answerC = "Peter Ellenshaw",
        answerD = "Chesley Bonestell",
        correctAnswer = 1,
        explanation = "Jack Cosgrove schuf das ikonische Matte Painting der silhouettierten Scarlett O'Hara vor dem dramatischen roten Sonnenuntergangshimmel in 'Vom Winde verweht'. Cosgrove war einer der fuehrenden Matte-Painting-Kuenstler Hollywoods und arbeitete an zahlreichen Selznick-Produktionen.",
        difficulty = 4,
        funFact = "Das Tara-Herrenhaus in 'Vom Winde verweht' war zum Grossteil eine Fassade — nur die Vorderseite war gebaut. Die ausgedehnten Plantagen-Szenen entstanden durch Matte Paintings. Das echte Haus war nie groesser als ein einzelnes Stockwerk mit begehbarem Foyer."
    ),

    // --- SET-DESIGN UND PRODUCTION DESIGN ---

    // Question 13 - Production Design Metropolis
    Question(
        categoryId = 4,
        questionText = "Fritz Langs 'Metropolis' (1927) ist fuer sein Zukunfts-Stadtdesign beruehmt. Welcher Architekt und Production Designer entwarf die ikonischen futuristischen Bauten?",
        answerA = "Walter Reimann",
        answerB = "Ernst Kettelhut",
        answerC = "Erich Kettelhut",
        answerD = "Otto Hunte",
        correctAnswer = 3,
        explanation = "Otto Hunte war der Hauptverantwortliche Production Designer fuer die ikonische Metropolis-Stadtarchitektur, unterstuetzt von Erich Kettelhut und Karl Vollbrecht. Hunte entwarf die charakteristischen Turmbauten, Strassenebenen und die Art-Deco-Industrieaesthetik, die den visuellen Stil des Films praegte.",
        difficulty = 4,
        funFact = "Die Miniaturmodelle fuer 'Metropolis' waren teilweise mehrere Meter hoch und kosteten astronomische Summen fuer die damalige Zeit. Der Film verschlang das gesamte Jahresbudget der UFA-Studios und trieb das Unternehmen fast in den Bankrott."
    ),

    // Question 14 - Production Design 2001
    Question(
        categoryId = 4,
        questionText = "Ken Adam entwarf keine Szenen in '2001: Odyssee im Weltraum'. Wer war der tatsaechliche Production Designer fuer Kubricks Meisterwerk?",
        answerA = "John Barry",
        answerB = "Harry Lange",
        answerC = "Anthony Masters",
        answerD = "Ernest Archer",
        correctAnswer = 2,
        explanation = "Anthony Masters war der Production Designer von '2001: Odyssee im Weltraum'. Er arbeitete eng mit Harry Lange und Ernest Archer zusammen, die beide zu den Art Directors zaehlen. Masters hatte zuvor 'Lawrence von Arabien' gestaltet und brachte dieselbe Auffassung von epischer Raumgestaltung mit.",
        difficulty = 4,
        funFact = "Die Centrifuge-Sequenz in '2001', in der astronauten an den Innenseiten der rotierenden Raumstation laufen, war ein voll funktionsfaehiges mechanisches Set das wirklich rotierte. Es kostete mehrere Millionen Pfund und wog viele Tonnen — die Kamera war fest montiert und drehte sich mit."
    ),

    // Question 15 - William Cameron Menzies Erfinder Art Director
    Question(
        categoryId = 4,
        questionText = "William Cameron Menzies gilt als Erfinder des Berufs 'Production Designer'. Bei welchem Film erhielt er als erster ueberhaupt den offiziellen Titel 'Production Designer' in den Credits?",
        answerA = "Things to Come (1936)",
        answerB = "Vom Winde verweht (1939)",
        answerC = "Rebecca (1940)",
        answerD = "The Thief of Bagdad (1924)",
        correctAnswer = 1,
        explanation = "William Cameron Menzies erhielt bei 'Vom Winde verweht' (1939) als erster Kuenstler ueberhaupt den Titel 'Production Designer' in den Credits. Produzent David O. Selznick erfand diesen Titel speziell fuer Menzies, um die Einzigartigkeit seiner Rolle bei der visuellen Konzeption des gesamten Films zu wuerdigen.",
        difficulty = 4,
        funFact = "Menzies erstellte fuer 'Vom Winde verweht' Tausende von Storyboard-Skizzen, die den gesamten Film vor dem Dreh visuell festlegten. Er legte sogar fuer jede Szene die Farbpalette fest — eine Methode, die heute als Standard gilt."
    ),

    // Question 16 - Kubrick Barry Lyndon Kerzenlicht
    Question(
        categoryId = 4,
        questionText = "Fuer 'Barry Lyndon' (1975) fotografierte Kubrick Szenen nur mit Kerzenlicht, wofuer er ein spezielles Objektiv verwendete, das urspruenglich fuer die NASA entwickelt worden war. Um welchen Objektivtyp handelte es sich?",
        answerA = "Carl Zeiss Planar T* 50mm f/0.7",
        answerB = "Leitz Noctilux 50mm f/1.0",
        answerC = "Cooke Speed Panchro 35mm f/1.4",
        answerD = "Schneider Xenon 25mm f/0.95",
        correctAnswer = 0,
        explanation = "Kubrick verwendete das Carl Zeiss Planar T* 50mm f/0.7 Objektiv, das urspruenglich fuer die NASA fuer Fotoaufnahmen bei Mondlandungen entwickelt wurde. Mit seiner extremen Lichstaerke von f/0.7 ermoeglichte es Aufnahmen bei fast voelliger Dunkelheit, allein beleuchtet durch Kerzen.",
        difficulty = 4,
        funFact = "Kubrick kaufte drei dieser NASA-Objektive von Zeiss und liess einen speziellen Mitchell-Filmkamerakoerper anfertigen, der die grossen Objektive aufnehmen konnte. Der Effekt der Kerzenlichtszenen war so realistisch, dass er wie ein Gemaelde wirkte."
    ),

    // Question 17 - Sets Avatar Pandora
    Question(
        categoryId = 4,
        questionText = "James Camerons 'Avatar' (2009) kombinierte echte Sets mit CGI-Erweiterungen. Welche technische Bezeichnung traegt das System, das Cameron entwickeln liess, um CGI-Elemente in Echtzeit im Viewfinder der Kamera zu sehen?",
        answerA = "Virtual Cinema System",
        answerB = "Simulcam",
        answerC = "PreVis Monitor",
        answerD = "Hybridview",
        correctAnswer = 1,
        explanation = "James Cameron liess das 'Simulcam'-System entwickeln, das es ermoeglichte, CGI-Elemente (Pandoras Flora, Na'vi-Charaktere etc.) in Echtzeit im Kamerasucher zu sehen, waehrend Schauspieler in einem leeren Studio spielten. Cameron konnte so wie in einem echten Set filmen, obwohl die Umgebung computergeneriert war.",
        difficulty = 4,
        funFact = "Das Simulcam-System erlaubte Cameron, spontane Entscheidungen auf dem Set zu treffen, wie er das auch bei realen Sets tun wuerde. Er konnte durch den Sucher schauen und sehen, wie ein Schauspieler neben einem zehn Meter grossen CGI-Tier steht und die Einstellung live anpassen."
    ),

    // --- KOSTUEM UND MAKEUP-DESIGN ---

    // Question 18 - Edith Head Oscars
    Question(
        categoryId = 4,
        questionText = "Edith Head ist die Kostuemdesignerin mit den meisten Oscar-Gewinnen in der Geschichte Hollywoods. Wie viele Oscars gewann sie in ihrer Karriere?",
        answerA = "Sechs",
        answerB = "Acht",
        answerC = "Zehn",
        answerD = "Zwoelf",
        correctAnswer = 1,
        explanation = "Edith Head gewann acht Oscars fuer das beste Kostuemdesign, was sie zur meistpraemierten Frau in der Oscar-Geschichte macht (Stand vor 2024). Sie arbeitete ueber fuenf Jahrzehnte fuer Paramount Pictures und war die Kostuemdesignerin von Hitchcocks Lieblingsschauspielerin Grace Kelly.",
        difficulty = 4,
        funFact = "Edith Head pflegte ein charakteristisches Erscheinungsbild: runde Sonnenbrillen, weissgraues Haar und dunkle Kleidung. Sie war das Vorbild fuer die Figur Edna Mode in Pixars 'The Incredibles' — eine Hommage von Regisseur Brad Bird, der ihre Arbeit verehrte."
    ),

    // Question 19 - Planet der Affen Makeup 1968
    Question(
        categoryId = 4,
        questionText = "John Chambers gewann fuer das Affenmakeup in 'Planet der Affen' (1968) einen Ehren-Oscar. Was war das bahnbrechende an seiner Makeup-Technik?",
        answerA = "Er entwickelte das erste hitzebestaendige Latex fuer Film-Prothesen",
        answerB = "Er erfand flexible, schaumlatex-basierte Prothesen die Gesichtsausdruecke ermoeglichten",
        answerC = "Er nutzte erstmals Computer-gesteuerte Schminktische",
        answerD = "Er verwendete echtes Affenfell kombiniert mit plastischem Makeup",
        correctAnswer = 1,
        explanation = "John Chambers entwickelte flexible Schaumlatex-Prothesen, die es den Darstellern ermoeglichten, trotz der umfangreichen Affenmasken Gesichtsausdruecke zu zeigen. Die Prothesen passten sich den Muskelbewegungen des Gesichts an, was vorher mit starren Latex-Masken unmoeglich war.",
        difficulty = 4,
        funFact = "Chambers' Makeup-Technik war so geheim, dass die CIA ihn spaeter anheuerte, um fuer die Operation 'Argo' (1979-1980) Tarnidentitaeten und Tarnverkleidungen herzustellen — die Geschichte wurde im Film 'Argo' (2012) verfilmt."
    ),

    // Question 20 - Makeup Darkest Hour Churchill
    Question(
        categoryId = 4,
        questionText = "Kazu Hiro (ehemals Kazuhiro Tsuji) gewann einen Oscar fuer seine Verwandlung von Gary Oldman in Winston Churchill in 'Die dunkelste Stunde' (2017). Wie lange dauerte die taeglich Makeup-Prozedur?",
        answerA = "Zwei Stunden",
        answerB = "Drei Stunden",
        answerC = "Vier Stunden",
        answerD = "Fuenf Stunden",
        correctAnswer = 1,
        explanation = "Die Verwandlung von Gary Oldman in Winston Churchill dauerte taeglich drei Stunden. Kazu Hiro schuf einen vollstaendigen Kopfaufsatz aus Silikon und verschiedene Prothesen fuer Wangen, Kinn und Haelse. Oldman musste ausserdem Gewicht zunehmen und saß stundenlang im Stuhl, bevor er ueberhaupt ans Set durfte.",
        difficulty = 4,
        funFact = "Kazu Hiro hatte sich eigentlich aus der Filmbranche zurueckgezogen und betrieb ein Spielzeuggeschaeft, bevor ihn Regisseur Joe Wright fuer 'Die dunkelste Stunde' ueberzeugte zurueckzukehren. Er gewann spaeter einen zweiten Oscar fuer seine Arbeit an 'Bombshell' (2019)."
    ),

    // Question 21 - Schminke Avatar
    Question(
        categoryId = 4,
        questionText = "Fuer 'Avatar' (2009) wurden die Na'vi-Charaktere hauptsaechlich via Motion Capture realisiert. Bei welchem Charakter im Film wurde tatsaechlich traditionelles Makeup verwendet, um einen humanoiden Aliencharakter darzustellen?",
        answerA = "Dr. Grace Augustine",
        answerB = "Neytiri",
        answerC = "Die Na'vi-Schamanen",
        answerD = "Parker Selfridge",
        correctAnswer = 2,
        explanation = "Einige der aelteren Na'vi-Schamanen und Stammesmitglieder, die im Hintergrund erschienen, wurden mit praktischem Makeup und Kostuemen realisiert, anstatt per Motion Capture. Dies war kostenguenstiger fuer Hintergrundcharaktere und ergaenzte die CGI-Na'vi im Vordergrund.",
        difficulty = 4,
        funFact = "Fuer 'Avatar: The Way of Water' (2022) entwickelte Camerons Team Unterwasser-Motion-Capture, das zuvor technisch unmoeglich war. Die Schauspieler mussten monatelang Tauchen lernen und konnten am Grund von riesigen Wassertanks stundenlang filmen."
    ),

    // Question 22 - Kostueme Elizabeth 1998
    Question(
        categoryId = 4,
        questionText = "Alexandra Byrne entwarf die Kostueme fuer 'Elizabeth' (1998) mit Cate Blanchett. Was war der unkonventionelle Ansatz, den Byrne gegenueber historischer Genauigkeit verfolgte?",
        answerA = "Sie verwendete moderne synthetische Stoffe fuer einen zeitlosen Look",
        answerB = "Sie kombinierte elisabethanische Formen mit modernen Schnitttraditionen",
        answerC = "Sie liess sich von Tudor-Portraets inspirieren, aber verzichtete auf Originalrekonstruktionen",
        answerD = "Sie nutzte nur zeitgenoessische Stoffe mit kuenstlicher Alterung",
        correctAnswer = 2,
        explanation = "Alexandra Byrne liess sich von Tudor-Zeitportraets und zeitgenoessischer Kunst inspirieren, waehrend sie moderne Interpretationen schuf, die die emotionale Entwicklung der Konigin visuell unterstuetzten. Sie priorisierte erzaehlerische und emotionale Wirkung gegenueber historischer Praezision.",
        difficulty = 4,
        funFact = "Byrne ist eine der vielseitigsten Kostuemdesignerinnen Hollywoods und hat sowohl historische Dramen ('Elizabeth', 'Thor', 'Hamlet') als auch Marvel-Blockbuster gestaltet. Sie gewann einen Oscar fuer 'Elizabeth: Das goldene Koenigreich' (2007)."
    ),

    // --- STUNTS UND ACTION ---

    // Question 23 - Stuntfrau Jackie Chan
    Question(
        categoryId = 4,
        questionText = "Jackie Chan ist beruehmt dafuer, seine Stunts selbst zu fuehren. Welchen schwerwiegenden Unfall erlitt er 1986 beim Dreh von 'Armour of God', der ihn beinahe das Leben kostete?",
        answerA = "Er brach sich beide Beine bei einem Sprung",
        answerB = "Ein Ast durchbohrte seinen Schaedel bei einem Sturz vom Baum",
        answerC = "Er brannte sich bei einer Explosionsszene schwer",
        answerD = "Er erlitt eine schwere Rueckenmarksverletzung bei einem Auto-Stunt",
        correctAnswer = 1,
        explanation = "Beim Dreh von 'Armour of God' sprang Jackie Chan von einer Burg auf einen Baum, der Ast brach und er stuerzte auf felsigen Boden. Ein Knochensplitter drang durch seinen Schaedel in sein Gehirn. Er wurde notoperiert und traegt seitdem eine permanente Kunststoffplatte im Schaedel.",
        difficulty = 4,
        funFact = "Trotz dieses Beinahe-Todeserlebnisses setzte Jackie Chan seine Stunt-Karriere fort. Er liess am Ende seiner Filme immer 'Blooper'-Outtakes einblenden, in denen misslueckte Stunts und kleine Verletzungen zu sehen waren — eine Tradition, die er bis heute beibehielt."
    ),

    // Question 24 - Stunt Koordinator Mad Max Fury Road
    Question(
        categoryId = 4,
        questionText = "Guy Norris war der Stunt Coordinator fuer 'Mad Max: Fury Road' (2015). Wie viel Prozent der Actionsequenzen wurden ungefaehr mit echten Fahrzeugen und praktischen Stunts gedreht, ohne CGI?",
        answerA = "Etwa 50 Prozent",
        answerB = "Etwa 70 Prozent",
        answerC = "Etwa 80 Prozent",
        answerD = "Nahezu 90 Prozent",
        correctAnswer = 3,
        explanation = "Regisseur George Miller und Stunt Coordinator Guy Norris realisierten nahezu 90 Prozent aller Actionsequenzen in 'Mad Max: Fury Road' mit echten Fahrzeugen, echten Stunts und praktischen Effekten auf echten Sandduenen in Namibia. CGI wurde hauptsaechlich fuer Sky-Replacements und kleinere Korrekturen eingesetzt.",
        difficulty = 4,
        funFact = "Der Dreh von 'Mad Max: Fury Road' dauerte 138 Tage und erzeugte ueber 480 Stunden Rohmaterial. Die Montage des Films dauerte fast 3,5 Jahre. Tom Hardy und Charlize Theron gaben zu, sich waehrend des Drehs zeitweise zutiefst zu hassen — was seltsamerweise ihrer gemeinsamen Chemie zugutekam."
    ),

    // Question 25 - Buster Keaton Stuntlegende
    Question(
        categoryId = 4,
        questionText = "Buster Keaton galt als der kuehnste Stuntdarsteller der Stummfilmaera. In welchem Film liess er eine Hausfassade auf sich fallen, wobei er durch ein Fensterloch unverletzt blieb?",
        answerA = "The General (1926)",
        answerB = "Sherlock Jr. (1924)",
        answerC = "Steamboat Bill, Jr. (1928)",
        answerD = "Our Hospitality (1923)",
        correctAnswer = 2,
        explanation = "In 'Steamboat Bill, Jr.' (1928) faellt eine komplette Hausfassade auf Keaton — er ueberlebt, weil er exakt in der richtigen Position steht, sodass ein offenes Fenster ueber ihm ihn unberuehrt laesst. Der Stunt war echt und wurde ohne Net oder Sicherheitssystem durchgefuehrt.",
        difficulty = 4,
        funFact = "Das Fenster hatte nur wenige Zentimeter Spielraum zu beiden Seiten von Keatons Koerper. Wenn er auch nur minimal seitlich gestanden haette, waere er von der Fassade erschlagen worden. Er fuehlte sich so sicher, dass er nicht mal zurueckwich, als die Mauer fiel."
    ),

    // Question 26 - Stuntperson Zoie Palmer
    Question(
        categoryId = 4,
        questionText = "Welche Organisation vergibt den 'Taurus World Stunt Award', den bedeutendsten Preis fuer Stuntarbeit, und seit welchem Jahr existiert er?",
        answerA = "Screen Actors Guild, seit 1995",
        answerB = "Taurus World Stunt Awards GmbH, seit 2001",
        answerC = "International Stunt Association, seit 1998",
        answerD = "American Stuntmens Association, seit 1990",
        correctAnswer = 1,
        explanation = "Die Taurus World Stunt Awards werden von der Taurus World Stunt Awards GmbH (mit Sitz in Deutschland) vergeben und fanden erstmals 2001 statt. Die Verleihung findet jedes Jahr in Los Angeles statt und ehrt herausragende Stuntarbeit in Film und Fernsehen weltweit.",
        difficulty = 4,
        funFact = "Die Stunt-Industrie klagte jahrelang darueber, dass die Academy of Motion Picture Arts and Sciences keine eigene Oscar-Kategorie fuer Stunts hat. Trotz mehrfacher Abstimmungen und Kampagnen wurde eine solche Kategorie bis heute nicht eingefuehrt."
    ),

    // --- BELEUCHTUNGSTECHNIKEN ---

    // Question 27 - Chiaroscuro Rembrandt Lighting
    Question(
        categoryId = 4,
        questionText = "Das sogenannte 'Rembrandt Lighting' ist eine klassische Portraetbeleuchtungstechnik aus dem Kino. Was ist das charakteristische Merkmal dieser Lichttechnik?",
        answerA = "Ein kleines Dreieck aus hellem Licht auf der beschatteten Wangenpartie",
        answerB = "Licht von direkt vorne oben, das Augen und Nase gleichmaessig ausleuchtet",
        answerC = "Seitliches Licht das genau die Haelfte des Gesichts ausleuchtet",
        answerD = "Licht von unten, das unheimliche Schatten auf der Stirn erzeugt",
        correctAnswer = 0,
        explanation = "Rembrandt Lighting bezeichnet die Technik, bei der ein kleines, dreieckiges Licht-Dreieck auf der beschatteten Wange des Motivs erscheint. Dies entsteht durch schrages Seitenlicht von oben, das so justiert ist, dass Nase und Wangenknochen einen kleinen hellen Fleck auf der Schattenpartie werfen.",
        difficulty = 4,
        funFact = "Rembrandt van Rijn schuf seine Portraets tatsaechlich mit natuerlichem Tageslicht aus einem hohen Nordlicht-Atelierfenster. Filmkameraleute rekonstruierten spaeter seine Lichttechnik und benannten sie nach ihm. Das erste Kino, das diese Technik systematisch dokumentierte, war das amerikansiche Studiosystem der 1930er Jahre."
    ),

    // Question 28 - Trois-Point-Licht
    Question(
        categoryId = 4,
        questionText = "Das klassische Drei-Punkt-Licht-Setup besteht aus Hauptlicht (Key Light), Fuellicht (Fill Light) und Kantenlicht (Back Light/Rim Light). Welche Funktion hat das Fuellicht primar?",
        answerA = "Es betont die Silhouette des Motivs gegen den Hintergrund",
        answerB = "Es reduziert die Kontraste der vom Key Light erzeugten Schatten",
        answerC = "Es beleuchtet den Hintergrund und trennt ihn vom Vordergrund",
        answerD = "Es ergaenzt das natuerliche Licht aus Fenstern",
        correctAnswer = 1,
        explanation = "Das Fuellicht (Fill Light) hat die Aufgabe, die Schatten abzumildern, die das Hauptlicht (Key Light) auf dem Motiv erzeugt. Es reduziert den Kontrast und haelt Schattendetails sichtbar. Die Intensitaet des Fuelllichts bestimmt das 'Lichtverhaeltnis' (Lighting Ratio) und damit den dramatischen Charakter der Szene.",
        difficulty = 4,
        funFact = "Filmkameraleute messen das Lichtverhaeltnis numerisch: Ein 2:1-Verhaeltnis (Key doppelt so hell wie Fill) erzeugt weiches, natuerliches Licht. Ein 8:1-Verhaeltnis erzeugt dramatisches Film-Noir-Licht mit tiefen Schatten. Cinematographer Vilmos Zsigmond verwendete fuer 'The Deer Hunter' ein extremes Verhaeltnis um eine bedrohliche Stimmung zu erzeugen."
    ),

    // Question 29 - Natu Licht und Roger Deakins
    Question(
        categoryId = 4,
        questionText = "Roger Deakins, einer der angesehensten Kameraleute Hollywoods, gewann seinen ersten Oscar fuer welchen Film?",
        answerA = "No Country for Old Men (2007)",
        answerB = "Blade Runner 2049 (2017)",
        answerC = "Sicario (2015)",
        answerD = "Skyfall (2012)",
        correctAnswer = 1,
        explanation = "Roger Deakins gewann seinen ersten Oscar fuer 'Blade Runner 2049' (2017) — nach 14 vorherigen Nominierungen ohne Gewinn. Sein revolutionaerer Einsatz von riesigen LED-Lichtwaenden als Hauptlichtquellen statt traditionellen Filmleuchten wurde als wegweisend fuer die Zukunft der Filmbeleuchtung gefeiert.",
        difficulty = 4,
        funFact = "Fuer 'Blade Runner 2049' verwendete Deakins eine riesige LED-Wand von ueber 30 Metern Breite als einzige Lichtquelle fuer die 'Wuesten-Sequenzen'. Die Wand konnte verschiedene Farbtemperaturen und Lichtmuster simulieren — ein Prototyp der heute weit verbreiteten 'In-Camera VFX'-Technik."
    ),

    // Question 30 - Licht in Citizen Kane
    Question(
        categoryId = 4,
        questionText = "Gregg Toland entwickelte fuer 'Citizen Kane' (1941) die extreme Tiefenschaerfe, bei der Vordergrund und Hintergrund gleichzeitig schaerfe bleiben. Welche Technik ermoeglichte das?",
        answerA = "Verwendung eines Weitwinkelobjektivs mit kleiner Blende und extrem heller Beleuchtung",
        answerB = "Separate Belichtung von Vorder- und Hintergrund in der Nachbearbeitung",
        answerC = "Ein neues Objektiv von Leitz mit aussergewoehnlicher Tiefenschaerfe",
        answerD = "Fokus-Stacking mehrerer Aufnahmen in der optischen Druckerei",
        correctAnswer = 0,
        explanation = "Gregg Toland erzielte die extreme Tiefenschaerfe durch Kombination von Weitwinkelobjektiven (14mm und 24mm), sehr kleinen Blenden (f/11 bis f/22) und extrem intensiver Beleuchtung. Die hohe Lichtmenge ermoeglichte die kleinen Blenden, die wiederum maximale Tiefenschaerfe lieferten.",
        difficulty = 4,
        funFact = "Gregg Toland bestand darauf, im Abspann von 'Citizen Kane' neben Orson Welles erwaehnt zu werden — ungewoehnlich fuer einen Kameramann der damaligen Zeit. Welles liess sogar eine gemeinsame Aufnahme von beiden fuer das Pressematerial machen, um Tolands Beitrag zu wuerdigen."
    ),

    // --- FILMFORMATE ---

    // Question 31 - CinemaScope Einfuehrung
    Question(
        categoryId = 4,
        questionText = "20th Century Fox fuehrte das CinemaScope-Verfahren 1953 ein. Mit welchem Film wurde dieses Breitwandformat erstmals der Oeffentlichkeit vorgestellt?",
        answerA = "How to Marry a Millionaire",
        answerB = "The Robe - Das Gewand",
        answerC = "Gentlemen Prefer Blondes",
        answerD = "The Big Fisherman",
        correctAnswer = 1,
        explanation = "'The Robe' (Das Gewand, 1953) war der erste Film, der im CinemaScope-Breitwandformat in den Kinos gezeigt wurde. Der Bibelfilm mit Richard Burton in der Hauptrolle wurde bewusst als Eroeffnungsfilm fuer das neue Format gewaehlt, um das grosse, opulente Erlebnis des Formats zu demonstrieren.",
        difficulty = 4,
        funFact = "'How to Marry a Millionaire' mit Marilyn Monroe wurde zeitgleich mit 'The Robe' produziert und war der zweite CinemaScope-Film — Aufgrund der Postproduktion wurde er erst Monate spaeter veroeffentlicht. Fox verwendete 'The Robe' als Eroefffnungsfilm, da ein Bibelfilm als wuerdigere Premiere fuer das neue Format galt."
    ),

    // Question 32 - 70mm Todd AO
    Question(
        categoryId = 4,
        questionText = "Das Todd-AO-Verfahren (70mm) wurde von Mike Todd und der American Optical Company entwickelt. Bei welchem Film wurde es 1955 erstmals eingesetzt?",
        answerA = "Ben-Hur (1959)",
        answerB = "Oklahoma! (1955)",
        answerC = "Around the World in 80 Days (1956)",
        answerD = "South Pacific (1958)",
        correctAnswer = 1,
        explanation = "Das Todd-AO-70mm-Format wurde 1955 erstmals bei der Verfilmung des Musicals 'Oklahoma!' eingesetzt. Das Format bot eine deutlich groessere Bildflaeche als 35mm und eine 6-Kanal-Magnettonspur fuer fruehen Surroundsound.",
        difficulty = 4,
        funFact = "Mike Todd starb 1958 bei einem Flugzeugabsturz, bevor er den Erfolg seines Formats vollstaendig miterleben konnte. Das Todd-AO-Format wurde von Paul Thomas Anderson fuer 'The Master' (2012) und Quentin Tarantino fuer 'The Hateful 8' (2015) wiederbelebt."
    ),

    // Question 33 - Digital vs Film
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr wurden erstmals mehr digitale als analoge Filmkopien in den internationalen Kinos verbreitet — ein historischer Wendepunkt in der Kinogeschichte?",
        answerA = "2007",
        answerB = "2010",
        answerC = "2013",
        answerD = "2016",
        correctAnswer = 2,
        explanation = "Laut Branchendaten uebertraf die Anzahl der digitalen Kinoprojektoren weltweit erstmals 2013 die der analogen 35mm-Projektoren. Bis 2015 hatten die meisten grossen Kinoketten weltweit vollstaendig auf digitale Projektion umgestellt und verarbeiteten keinen 35mm-Film mehr.",
        difficulty = 4,
        funFact = "Christopher Nolan und Quentin Tarantino sind die prominentesten Verfechter des analogen Films in der Gegenwart. Nolan hat bei allen seinen Filmen auf echtem Celluloid-Film bestanden, und Tarantino drehte 'The Hateful Eight' (2015) auf 70mm-Film und liess eigens alte 70mm-Projektoren restaurieren."
    ),

    // Question 34 - VistaVision
    Question(
        categoryId = 4,
        questionText = "VistaVision war ein von Paramount entwickeltes Hochqualitaets-Filmverfahren, das 35mm-Film horizontal laufen liess, um eine groessere Bildflaeche zu erhalten. Welcher Hitchcock-Film war einer der ersten, der dieses Format nutzte?",
        answerA = "Rear Window (1954)",
        answerB = "To Catch a Thief (1955)",
        answerC = "The Man Who Knew Too Much (1956)",
        answerD = "Vertigo (1958)",
        correctAnswer = 1,
        explanation = "'To Catch a Thief' (1955) war einer der ersten Hitchcock-Filme im VistaVision-Format. Paramount entwickelte VistaVision als Antwort auf CinemaScope — anstatt das Bild optisch zu stauchen, lief der 35mm-Film horizontal durch die Kamera, was eine doppelt so grosse Bildflaeche ergab.",
        difficulty = 4,
        funFact = "VistaVision wurde nach wenigen Jahren fuer Spielfilme aufgegeben, da die horizontale Filmfuehrung aufwendigere Kameras erforderte. Die Technologie lebt jedoch in der Special-Effects-Industrie weiter: ILM nutzte VistaVision-Kameras noch Jahrzehnte spaeter fuer Effektaufnahmen, die in normale Filme eincomposited wurden."
    ),

    // --- FARBKORREKTUR UND COLOR GRADING ---

    // Question 35 - Technicolor Three-Strip
    Question(
        categoryId = 4,
        questionText = "Das Technicolor-Drei-Streifen-Verfahren (Three-Strip) erzeugte gesaettigte Farben durch drei separate Filmstreifen fuer Rot, Gruen und Blau. Welcher Film war der erste spiellangere Film in diesem Verfahren?",
        answerA = "Becky Sharp (1935)",
        answerB = "Snow White and the Seven Dwarfs (1937)",
        answerC = "Gone with the Wind (1939)",
        answerD = "The Wizard of Oz (1939)",
        correctAnswer = 0,
        explanation = "'Becky Sharp' (1935) war der erste spiellangere Realfilm im Technicolor-Drei-Streifen-Verfahren. Rouben Mamoulian nutzte die satten Farben bewusst dramaturgisch, mit intensiven Rottoen in emotionalen Hoehepunkten. Der Film gilt als Meilenstein in der Geschichte des Farbkinos.",
        difficulty = 4,
        funFact = "Das Technicolor-Verfahren war extrem aufwendig: Die Kamera war dreimal so gross wie eine normale Filmkamera und brauchte drei Mal so viel Licht. Technicolor verlieh die Kameras nur und schickte eigene 'Farbberater' mit, die sicherstellten, dass die Farben korrekt wirkten — was oft zu Konflikten mit Regisseuren fuehrte."
    ),

    // Question 36 - Digital Color Grading DaVinci
    Question(
        categoryId = 4,
        questionText = "DaVinci Resolve ist heute der Industriestandard fuer digitales Color Grading. Von welchem Unternehmen wurde das System urspruenglich entwickelt, bevor Blackmagic Design es 2009 erwarb?",
        answerA = "Quantel",
        answerB = "Da Vinci Systems",
        answerC = "Avid Technology",
        answerD = "Discreet Logic",
        correctAnswer = 1,
        explanation = "DaVinci Resolve wurde urspruenglich von Da Vinci Systems in Fort Lauderdale, Florida entwickelt und war seit den 1980er Jahren das Standardsystem der Profi-Coloristen in der Filmbranche. Blackmagic Design erwarb das System 2009 und machte es mit einer kostenlosen Version einer viel breiteren Nutzerschaft zugaenglich.",
        difficulty = 4,
        funFact = "Als Blackmagic Design DaVinci Resolve 2009 erwarb und kurz darauf eine kostenlose Basisversion herausbrachte, konnten erstmals auch kleine Independent-Filmemacher professionelles Color Grading verwenden. Das System, das zuvor bis zu 500.000 Dollar kostete, ist heute kostenlos herunterladbar."
    ),

    // Question 37 - Look Development Mad Max
    Question(
        categoryId = 4,
        questionText = "Der charakteristische orange-teal Look in vielen modernen Blockbustern entstand als dominanter Trend. Welcher technische Prozess erzeugt diesen spezifischen Farbkontrast systematisch?",
        answerA = "HSL-Qualifizierung in den Midtones",
        answerB = "Complementary Color Grading in der Schatten-Highlight-Achse",
        answerC = "Selective Hue-Rotation in den Hauttonen und Hintergruenden",
        answerD = "LUT-basierte Farbverschiebung des blauen Farbkanals",
        correctAnswer = 1,
        explanation = "Der orange-teal Look entsteht durch complementaere Farbgebung: Hauttone (die im Rohfilm eher orange sind) werden in den Highlights nach Orange verschoben, waehrend Hintergruende und Schatten nach dem komplementaeren Teal (blau-gruen) verschoben werden. Dieser Kontrast erzeugt ein dramatisch-modernes Aussehen.",
        difficulty = 4,
        funFact = "Der orange-teal Trend begann um 2000 und erreichte seinen Hoehepunkt um 2010-2015. Filmkritiker und Coloristen kritisierten den Trend scharf, da er viele Filme optisch austauschbar machte. Bis 2020 begannen grosse Studios bewusst originellere Farbpaletten zu verwenden, um sich davon abzuheben."
    ),

    // Question 38 - Bleach Bypass
    Question(
        categoryId = 4,
        questionText = "Der 'Bleach Bypass' (auch Silbererhaltungsverfahren) ist ein chemischer Entwicklungsprozess, der den Bleichschritt beim Filmentwickeln ueberspringt. Welchen optischen Effekt erzeugt er?",
        answerA = "Erhoehte Farbsaettigung mit weichen Schatten",
        answerB = "Desaturierte Farben mit harten, koernigen Kontrasten",
        answerC = "Pastelltoene mit erhoehter Helligkeit in den Midtones",
        answerD = "Warme Hauttone mit kuehlen Schatten",
        correctAnswer = 1,
        explanation = "Der Bleach Bypass-Prozess erzeugt durch das Belassen der Silberschicht im Film desaturierte, kontrastreiche Bilder mit erhoehtem Korn. Farben wirken gedaempft und fast monochromatisch, waehrend der Kontrast dramatisch erhoert ist. Beruehmte Beispiele sind 'Saving Private Ryan' und 'Se7en'.",
        difficulty = 4,
        funFact = "Fuer 'Saving Private Ryan' (1998) wendete Steven Spielberg und Kameramann Janusz Kaminski den Bleach-Bypass-Prozess auf alle Kriegsszenen an, waehrend die Rahmenhandlung mit dem alten Veteran normal entwickelt wurde — was einen sofortigen, visuellen Kontrast zwischen Gegenwart und Vergangenheit schuf."
    ),

    // --- PRODUKTIONSTECHNIKEN ---

    // Question 39 - One-Take Birdman
    Question(
        categoryId = 4,
        questionText = "Alejandro G. Inarritu und Kameramann Emmanuel Lubezki liessen 'Birdman' (2014) wie eine einzige, ununterbrochene Kamerafahrt aussehen. Wie erzielte das Team diesen Eindruck?",
        answerA = "Mit echten Einzelaufnahmen, die jeweils stundenlang dauerten",
        answerB = "Durch digitale Verbindungen an dunklen Stellen und schwierigen Schnitten",
        answerC = "Ausschliesslich durch Live-Schnitte in der Kamera",
        answerD = "Durch Zeitraffersequenzen als Uebergaenge",
        correctAnswer = 1,
        explanation = "Die Pseudo-One-Take-Optik von 'Birdman' entstand durch sorgfaeltig geplante, lange Takes, die digital an dunklen Momenten (Schattendurchgaenge, schnelle Schwenks, Haende vor die Kamera) zusammengefuegt wurden. Nur zwei Stellen enthalten deutlichere, aber gut versteckte Schnitte.",
        difficulty = 4,
        funFact = "Emmanuel Lubezki ist der einzige Kameramann, der drei Oscars in Folge gewann: fuer 'Gravity' (2014), 'Birdman' (2015) und 'The Revenant' (2016). Sein charakteristischer Stil mit natuerlichem Licht und langen Steadicam-Fahrten veraenderte die Aesthetik des modernen Kinos massgeblich."
    ),

    // Question 40 - Welles Deep Focus
    Question(
        categoryId = 4,
        questionText = "Tiefenschaerfe als dramaturgisches Mittel: In 'The Magnificent Ambersons' (1942) verwendete Orson Welles Weitwinkelobjektive in engen Raeumen. Welcher ungewoehnliche Ort musste dafuer baulich modifiziert werden?",
        answerA = "Das Treppenhaus wurde um einen Meter verbreitert",
        answerB = "Decken wurden aus Gips gefertigt um niedrige Kamerawinkel zu ermoelichen",
        answerC = "Die Aussenwand eines Hauses wurde entfernt fuer Tiefenzoom-Shots",
        answerD = "Ein Keller wurde speziell gegraben fuer Froschperspektive-Aufnahmen",
        correctAnswer = 1,
        explanation = "Wie bereits bei 'Citizen Kane' (1941) liess Orson Welles fuer 'The Magnificent Ambersons' Filmsets mit echten Decken aus Gips bauen, damit die Kamera von unten nach oben schwenken und extreme Froschperspektiven einnehmen konnte. Hollywood-Sets hatten damals keine echten Decken.",
        difficulty = 4,
        funFact = "Bis zu Welles' Innovation waren Hollywoodsets oben offen, damit die Beleuchtungstechniker von oben arbeiten konnten. Welles' Decken zwangen das Beleuchtungsteam, alle Lampen auf Aughoehe oder darunter zu platzieren — was seltsamerweise realistischere Beleuchtung erzeugte."
    ),

    // Question 41 - Regie vs Kamera Konflikt
    Question(
        categoryId = 4,
        questionText = "Die Arbeitsteilung zwischen Regisseur und Director of Photography (DP) kann zu Konflikten fuehren. Bei welchem Hitchcock-Film wurde der OP-Robert Burks durch seinen extrem intensiven Dreh mit dem Regisseur bekannt?",
        answerA = "Dial M for Murder (1954)",
        answerB = "Rear Window (1954)",
        answerC = "The Birds (1963)",
        answerD = "Psycho (1960)",
        correctAnswer = 3,
        explanation = "Hitchcock vertraute Robert Burks so sehr, dass sie 12 Filme zusammen drehten. Jedoch fotografierte John L. Russell 'Psycho' (1960), weil Hitchcock bewusst sein B-Movie-Fernsehteam einsetzen wollte, um die Kosten zu senken. Burks war bei Psycho NICHT dabei — was ein bekanntes Filmgeschichte-Detail ist.",
        difficulty = 4,
        funFact = "Hitchcock drehte 'Psycho' mit seinem Fernseh-Crew vom Alfred Hitchcock Presents-Team, was dem Film seinen koeernigen, direkten Look verlieh. Das Budget betrug nur 800.000 Dollar — Hitchcock finanzierte den Film selbst, um die volle kreative Kontrolle zu behalten."
    ),

    // Question 42 - Kubrick One-Point Perspective
    Question(
        categoryId = 4,
        questionText = "Stanley Kubrick ist beruehmt fuer seine One-Point-Perspective-Aufnahmen, bei denen alle Linien im Bild auf einen einzigen Fluchtpunkt im Zentrum zulaufen. In welchem Film setzte er diese Technik am systematischsten ein?",
        answerA = "2001: Odyssee im Weltraum (1968)",
        answerB = "Eyes Wide Shut (1999)",
        answerC = "Barry Lyndon (1975)",
        answerD = "The Shining (1980)",
        correctAnswer = 3,
        explanation = "In 'The Shining' (1980) verwendete Kubrick die One-Point-Perspective am systematischsten und wirkungsvollsten: Die langen Korridor-Shots mit Danny auf dem Dreirad, die Flure des Overlook Hotels und die Bar-Szenen sind alle auf einen einzigen Fluchtpunkt ausgerichtet. Dies erzeugt psychologische Desorientierung und Klaustrophobie.",
        difficulty = 4,
        funFact = "Kubrick liess fuer 'The Shining' einen speziellen, niedrig montierten Kamerawagen entwickeln, den sogenannten 'Kubrick Dolly', der exakt in der Korridor-Mitte bleiben konnte. Dieser Wagen ist heute im Kubrick-Archiv der Deutschen Kinemathek in Berlin zu sehen."
    ),

    // Question 43 - Kuleshov Effekt
    Question(
        categoryId = 4,
        questionText = "Der Kuleshov-Effekt beweist, dass die Bedeutung eines Filmbildes durch das folgende Bild bestimmt wird. Lev Kuleshov demonstrierte diesen Effekt urspruenglich mit Aufnahmen des russischen Schauspielers Iwan Mosjoukine. Welche drei Bilder kombinierte er mit Mosjoukines unveraendertem Gesichtsausdruck?",
        answerA = "Suppe, Frau, Sarg",
        answerB = "Kind, Abteil, Messer",
        answerC = "Schloss, Geld, Maedchen",
        answerD = "Teller Suppe, weinendes Kind, Sarg",
        correctAnswer = 0,
        explanation = "Lev Kuleshov kombinierte dasselbe, unveraenderte Bild von Mosjoukines neutralem Gesicht mit drei verschiedenen Bildern: einem Teller Suppe (Hunger), einer Frau im Sarg (Trauer) und einem kleinen Maedchen (Freude). Zuschauer schrieben Mosjoukine jeweils unterschiedliche, passende Emotionen zu — obwohl sein Gesicht identisch war.",
        difficulty = 4,
        funFact = "Der Kuleshov-Effekt ist so fundamental, dass er als Beweis gilt, dass der Film als Kunstform der Wirklichkeit ueberlegen ist: Durch Montage kann Bedeutung geschaffen werden, die in keinem Einzelbild vorhanden ist. Dieses Prinzip nutzten Hitchcock, Eisenstein und praktisch jeder bedeutende Regisseur seitdem."
    ),

    // Question 44 - Abtastung und Projektion
    Question(
        categoryId = 4,
        questionText = "Die Standardprojektionsgeschwindigkeit fuer Tonfilme betraegt 24 Bilder pro Sekunde. Warum wurde historisch genau diese Zahl als Standard gewahlt?",
        answerA = "Weil die fruehesten optischen Drucker mit 24 fps liefen",
        answerB = "Weil 24 fps das Minimum fuer fluessige Tonwiedergabe auf Magnetton-Lichtton war",
        answerC = "Weil es ein Kompromiss zwischen Filmverbrauch und Tonqualitaet war",
        answerD = "Weil Agfa und Kodak ihre ersten Filmmaterialien auf 24 fps optimierten",
        correctAnswer = 2,
        explanation = "24 Bilder pro Sekunde wurden als Standard etabliert, weil diese Geschwindigkeit einen praktikablen Kompromiss zwischen dem notwendigen Filmverbrauch (und damit Kosten) und der notwendigen Minimalgeschwindigkeit fuer qualitativ hochwertigen Lichtton (der eine Mindestbandbreite der Tonspur erfordert) darstellte.",
        difficulty = 4,
        funFact = "Stummfilme liefen typischerweise mit 16-18 fps, weshalb sie bei moderner 24-fps-Projektion beschleunigt wirken. Heute experimentieren Regisseure wie Peter Jackson (48 fps fuer 'The Hobbit') und James Cameron (60 fps fuer 'Avatar 2') mit hoeheren Frequenzen, die von vielen Zuschauern als ungewohnt glatt empfunden werden."
    ),

    // Question 45 - Visual Effects Supervisor
    Question(
        categoryId = 4,
        questionText = "Wer war der Visual Effects Supervisor fuer 'Inception' (2010) und gewann dafuer den Oscar fuer die besten visuellen Effekte?",
        answerA = "Paul Franklin",
        answerB = "Chris Corbould",
        answerC = "Andrew Lockley",
        answerD = "Dan Glass",
        correctAnswer = 0,
        explanation = "Paul Franklin von der VFX-Firma Double Negative (DNEG) war der Visual Effects Supervisor fuer 'Inception' und gewann zusammen mit Chris Corbould, Andrew Lockley und Peter Bebb den Oscar fuer die besten visuellen Effekte. Franklin arbeitete danach auch an 'Interstellar' (2014) und simulierte dort erstmals wissenschaftlich korrekte Schwarze Loecher.",
        difficulty = 4,
        funFact = "Fuer 'Interstellar' arbeitete Paul Franklin mit dem Astrophysiker Kip Thorne zusammen, um das Aussehen des Schwarzen Lochs 'Gargantua' wissenschaftlich korrekt zu berechnen. Die Simulationssoftware produzierte dabei Erkenntnisse, die tatsaechlich in einem peer-reviewed Wissenschaftsartikel veroeffentlicht wurden."
    ),

    // Question 46 - Produktionsdesign Blade Runner 2049
    Question(
        categoryId = 4,
        questionText = "Patrice Vermette gestaltete das Production Design von 'Blade Runner 2049' (2017). Welche reale Ortschaft diente als Hauptinspiration fuer die vereiste Wuesten-Sequenzen rund um das verlassene Las Vegas?",
        answerA = "Die Salton Sea in Kalifornien",
        answerB = "Das Salzsee-Gebiet von Utah",
        answerC = "Die Strassen und Spielhallen von Detroit",
        answerD = "Die Canyonlands in Utah",
        correctAnswer = 0,
        explanation = "Die verlassenen, versalzten Sequenzen rund um das apokalyptische Las Vegas in 'Blade Runner 2049' wurden von der echten Salton Sea in Suedkalifornien inspiriert — einem ausgetrockneten, versalzten See mit verlassenen Siedlungen und einer surrealen, apokalyptischen Atmosphaere.",
        difficulty = 4,
        funFact = "Vermette gewann den Oscar fuer Production Design fuer 'Blade Runner 2049'. Er baute in Budapest auf dem Origo Film Studios eine riesige Indoor-Wueste, inklusive echter Sandduenen, um die Las Vegas-Sequenzen zu drehen. Die orangefarbene Farbpalette dieser Szenen war ein bewusstes Homage an die Wuestenschoepfungen von David Lean."
    ),

    // Question 47 - Kostueme Mad Max Fury Road
    Question(
        categoryId = 4,
        questionText = "Jenny Beavan gewann den Oscar fuer das Kostuemdesign in 'Mad Max: Fury Road' (2015). Welche ungewoehnliche Herangehensweise verfolgte sie bei der Beschaffung der Materialien?",
        answerA = "Alle Kostueme wurden aus recycelten Plastikmaterialien hergestellt",
        answerB = "Sie kaufte Hunderte von Pfund Altkleidung und Materialien auf Troedelmaerkten weltweit",
        answerC = "Saemtliche Stoffe wurden in einer Spezialwerkstaett alt gemaacht",
        answerD = "Realkleidung aus Namibia und Australien wurde direkt verwendet",
        correctAnswer = 1,
        explanation = "Jenny Beavan beschaffte Rohmaterialien fuer 'Mad Max: Fury Road' hauptsaechlich auf Troedelmaerkten in Australien und weltweit — Hunderte Kilogramm gebrauchter Kleidung, Gurte, Metall und Sonstiges. Diese wurden zerschnitten, neu zusammengesetzt, verwittert und zu den post-apokalyptischen Kostuemen zusammengefuegt.",
        difficulty = 4,
        funFact = "Jenny Beavan erschien bei der Oscar-Verleihung 2016 in einer Lederjaeke mit dem Emblem der War Boys — ihrem eigenen, von ihr selbst entworfenem Kostuem, passend zu den Charakteren des Films. Es wurde zur Schlagzeile, weil die anderen Nominierten in formellen Abendkleidern erschienen."
    ),

    // Question 48 - Schienenfahrten und Dollys
    Question(
        categoryId = 4,
        questionText = "Der 'Louma Crane' war ein revolutionaerer Kamerakran, der in den 1970er Jahren entwickelt wurde und Aufnahmen aus schwer erreichbaren Winkeln ermoeglichte. Wer entwickelte diesen Kran?",
        answerA = "Jean-Marie Lavalou und Alain Masseron",
        answerB = "Chapman und Leonard",
        answerC = "Technocrane AG",
        answerD = "Ronford-Baker in Zusammenarbeit mit der BBC",
        correctAnswer = 0,
        explanation = "Der Louma Crane wurde von den franzoesischen Ingenieuren Jean-Marie Lavalou und Alain Masseron entwickelt. Der Kran erlaubte es, die Kamera an einem langen Ausleger ferngesteuert zu positionieren, ohne dass ein Kameramann am Kran sitzen musste — revolutionaer fuer Aufnahmen in engen Raeumen oder aus extremen Winkeln.",
        difficulty = 4,
        funFact = "Der Louma Crane wurde erstmals auf dem Filmset von Francois Truffauts 'Die abgehackte Hand' eingesetzt und wurde schnell zum Standard-Kran in der europaeischen und amerikanischen Filmproduktion. Er ermoeglichte Aufnahmen, die mit normalen Krans physikalisch unmoeglich waren."
    ),

    // Question 49 - Makeup Rick Baker
    Question(
        categoryId = 4,
        questionText = "Rick Baker gilt als einer der innovativsten Makeup-Kuenstler der Filmgeschichte und gewann sieben Oscars. Fuer welchen Film gewann er den allerersten Oscar, der jemals in der Kategorie 'Beste Makeup-Effekte' vergeben wurde?",
        answerA = "Star Wars (1977)",
        answerB = "An American Werewolf in London (1981)",
        answerC = "The Howling (1981)",
        answerD = "Videodrome (1983)",
        correctAnswer = 1,
        explanation = "Rick Baker gewann den ersten Oscar in der Kategorie 'Beste Makeup-Effekte', der jemals vergeben wurde, fuer 'An American Werewolf in London' (1981). Die Academy hatte diese Kategorie erstmals 1982 (fuer Filme des Jahres 1981) eingerichtet — Baker gewann sie bei ihrer allerersten Vergabe.",
        difficulty = 4,
        funFact = "Die beruehmt Werwolf-Verwandlungssequenz in 'An American Werewolf in London' gilt bis heute als eine der technisch aufwendigsten und realistischsten Makeup-Effekte der Filmgeschichte. Baker verwendete eine Kombination aus mechanischen Prothesen, expandierbarem Schaumlatex und pnematischen Kontraktionsmechanismen."
    ),

    // Question 50 - Production Design Wes Anderson
    Question(
        categoryId = 4,
        questionText = "Wes Andersons Production Designer Adam Stockhausen und Set Decorator Anna Pinnock gewannen den Oscar fuer welchen Anderson-Film?",
        answerA = "The Life Aquatic with Steve Zissou (2004)",
        answerB = "The Grand Budapest Hotel (2014)",
        answerC = "Moonrise Kingdom (2012)",
        answerD = "The French Dispatch (2021)",
        correctAnswer = 1,
        explanation = "Adam Stockhausen und Anna Pinnock gewannen 2015 den Oscar fuer Production Design fuer 'The Grand Budapest Hotel'. Das Hotel selbst wurde in einem verlassenen Kaufhaus in Goerlitz, Deutschland gedreht — das echte Hotel wurde nicht gebaut, sondern war eine Mischung aus dem Goerlitzer Kaufhaus, Miniaturmodellen und CGI-Erweiterungen.",
        difficulty = 4,
        funFact = "Wes Anderson drehte die verschiedenen Zeitebenen in 'The Grand Budapest Hotel' bewusst in verschiedenen Filmformaten: 1.33:1 (akademisches Format) fuer die 1930er-Sequenzen, 1.85:1 fuer die 1960er und 2.35:1 fuer die Gegenwartsszenen. Ein filmhistorisch einzigartiger Ansatz, Zeitebenen durch Seitenverhaeltnisse zu unterscheiden."
    ),

)
