package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsExpert7(): List<Question> = listOf(

    // --- FILMMUSIK-KOMPONISTEN ---

    // Question 1 - John Williams Leitmotiv
    Question(
        categoryId = 4,
        questionText = "Welches kompositorische Prinzip nutzte John Williams in der Star-Wars-Saga konsequent, bei dem jede Figur ein eigenes musikalisches Thema erhält?",
        answerA = "Tonale Programmmusik",
        answerB = "Leitmotivtechnik",
        answerC = "Dodekaphonie",
        answerD = "Ostinato-Prinzip",
        correctAnswer = 1,
        explanation = "John Williams übernahm die aus der Opernkomposition stammende Leitmotivtechnik von Richard Wagner: Jede wichtige Figur, jeder Ort und jedes Konzept erhält ein eigenes, wiedererkennbares musikalisches Thema, das variiert und weiterentwickelt werden kann. Das Darth-Vader-Thema (Imperial March) oder das Force-Thema sind klassische Beispiele.",
        difficulty = 4,
        funFact = "Williams schrieb für die Star-Wars-Saga über 20 verschiedene Leitmotive. Der Imperial March wurde erst in Episode V eingeführt — in Episode IV gab es noch kein eigenständiges Vader-Thema."
    ),

    // Question 2 - Hans Zimmer Inception
    Question(
        categoryId = 4,
        questionText = "Welches akustische Phänomen nutzte Hans Zimmer im Inception-Soundtrack, bei dem ein Ton scheinbar endlos ansteigt, obwohl er sich im Kreis dreht?",
        answerA = "Shepard-Ton",
        answerB = "Tartini-Ton",
        answerC = "Haas-Effekt",
        answerD = "Doppler-Illusion",
        correctAnswer = 0,
        explanation = "Der Shepard-Ton ist eine akustische Illusion: Mehrere Sinustöne in Oktavabständen werden gleichzeitig gespielt, wobei höhere Töne leiser und tiefere Töne lauter werden. Das Ergebnis ist ein Ton, der sich scheinbar endlos nach oben bewegt — eine perfekte Metapher für die Traumebenen in Inception.",
        difficulty = 4,
        funFact = "Der Inception-Soundtrack enthält auch eine extrem verlangsamte Version von Edith Piafs 'Non, je ne regrette rien' — die Idee entstand, weil der Film den Song als Wecksignal in der Traumsequenz einsetzt."
    ),

    // Question 3 - Ennio Morricone Spaghetti-Western
    Question(
        categoryId = 4,
        questionText = "Welches ungewöhnliche Instrument verwendete Ennio Morricone in seinen Sergio-Leone-Western-Soundtracks als charakteristisches Klangelement?",
        answerA = "Theremin",
        answerB = "Menschliche Pfeifstimme und E-Gitarre",
        answerC = "Ondes Martenot",
        answerD = "Hammondorgel",
        correctAnswer = 1,
        explanation = "Morricone kombinierte für die Leone-Western-Trilogie (Dollars-Trilogie) ungewöhnliche Klangquellen: menschliches Pfeifen, Jagdrufe, E-Gitarren-Riffs, Trompeten und Chor. Besonders das Pfeifen (gespielt von Alessandro Alessandroni) wurde durch Echoeffekte verfremdet und ist bis heute mit dem Spaghetti-Western-Genre assoziiert.",
        difficulty = 4,
        funFact = "Morricone und Sergio Leone kannten sich seit der Grundschule. Sie komponierten oft zusammen: Leone summte Melodielinien, Morricone setzte sie um. Für 'Spiel mir das Lied vom Tod' entstand die Musik sogar vor dem Dreh."
    ),

    // Question 4 - Bernard Herrmann Psycho
    Question(
        categoryId = 4,
        questionText = "Bernard Herrmann komponierte den berühmten Duschszenen-Score für Hitchcocks 'Psycho' ausschließlich für welche Instrumentengruppe?",
        answerA = "Blechbläser und Schlagwerk",
        answerB = "Nur Streicher",
        answerC = "Holzbläser und Klavier",
        answerD = "Streicher und Orgel",
        correctAnswer = 1,
        explanation = "Herrmanns berühmter Psycho-Score verwendet ausschließlich Streicher — keine Blechbläser, keine Holzbläser, kein Schlagwerk. Die Entscheidung war zunächst budgetär motiviert, erwies sich aber als künstlerisch genial. Die 'screaming strings' der Duschszene erzeugen Horror durch hohe Intensität und Dissonanz allein mit Streichinstrumenten.",
        difficulty = 4,
        funFact = "Alfred Hitchcock wollte die Duschszene ursprünglich ohne Musik. Herrmann überzeugte ihn, die Szene mit und ohne Score zu sehen — danach erhöhte Hitchcock Herrmanns Gage auf das Doppelte."
    ),

    // Question 5 - Jerry Goldsmith
    Question(
        categoryId = 4,
        questionText = "Jerry Goldsmith gewann 1976 den Oscar für den Filmmusik zu welchem Horror-Klassiker, der zwölf Tonreihen-Techniken mit satanischen Chorälen verband?",
        answerA = "Der Exorzist",
        answerB = "Das Omen",
        answerC = "Rosemaries Baby",
        answerD = "Carrie",
        correctAnswer = 1,
        explanation = "Jerry Goldsmith gewann seinen einzigen Oscar für die Filmmusik zu 'Das Omen' (1976). Besonders berühmt ist das Chorstück 'Ave Satani', das gregorianische Choralformen mit düsterem, lateinischem Text ('Sanguis bibimus, corpus edimus') verbindet. Goldsmith nutzte dabei Zwölftonreihen und atonale Passagen.",
        difficulty = 4,
        funFact = "Goldsmith war so prolifisch, dass er an manchen Tagen Musik für bis zu drei verschiedene Projekte gleichzeitig schrieb. In seiner Karriere komponierte er über 200 Filmscores."
    ),

    // Question 6 - Nino Rota
    Question(
        categoryId = 4,
        questionText = "Nino Rota wurde 1972 bei der Oscar-Verleihung zunächst disqualifiziert, weil er eine Melodie aus einem früheren Film wiederverwendet hatte — für welches Werk?",
        answerA = "Amarcord",
        answerB = "Der Pate",
        answerC = "8½",
        answerD = "Fellinis Roma",
        correctAnswer = 1,
        explanation = "Nino Rotas Liebesthema aus 'Der Pate' (1972) basiert teilweise auf einer Melodie, die er bereits 1958 für den italienischen Film 'Fortunella' verwendet hatte. Die Academy disqualifizierte ihn zunächst, später wurde er für Teil II des Paten nominiert und gewann. Das Paten-Thema gilt trotzdem als eines der bekanntesten Filmmusikstücke aller Zeiten.",
        difficulty = 4,
        funFact = "Francis Ford Coppola und Nino Rota arbeiteten sehr eng zusammen — Coppola sagte, Rota habe das Liebesthema in nur einer Stunde komponiert, nachdem er eine kurze Beschreibung der Szene gehört hatte."
    ),

    // Question 7 - Philip Glass Minimalismus
    Question(
        categoryId = 4,
        questionText = "Philip Glass prägte mit seinem Soundtrack zu 'Koyaanisqatsi' (1982) einen kompositorischen Stil, der auf ständiger Wiederholung kleiner Muster basiert. Wie nennt sich diese Strömung?",
        answerA = "Serielle Musik",
        answerB = "Minimalismus",
        answerC = "Spektralismus",
        answerD = "Neue Sachlichkeit",
        correctAnswer = 1,
        explanation = "Philip Glass ist einer der Hauptvertreter der minimalistischen Musik: Kurze, sich wiederholende Motive (Ostinati) werden langsam verändert, was hypnotische, meditative Wirkungen erzeugt. In 'Koyaanisqatsi' synchronisiert Glass diese Technik mit Godfrey Reggios Zeitrafferaufnahmen der modernen Zivilisation — der Titel bedeutet in der Hopi-Sprache 'Leben aus dem Gleichgewicht'.",
        difficulty = 4,
        funFact = "Glass wurde als Student von Ravi Shankar beeinflusst: Das indische Prinzip, Musik durch additive Rhythmen aufzubauen statt durch harmonische Entwicklung, ist ein Grundpfeiler seines Minimalismus."
    ),

    // Question 8 - Composer Collaboration
    Question(
        categoryId = 4,
        questionText = "Welcher Komponist schuf den revolutionären Score für '2001: Odyssee im Weltraum' (1968) — obwohl Stanley Kubrick die eigens bestellte Originalmusik verwarf und ausschließlich klassische Vorrecordings verwendete?",
        answerA = "Krzysztof Penderecki",
        answerB = "Alex North",
        answerC = "György Ligeti",
        answerD = "Karlheinz Stockhausen",
        correctAnswer = 1,
        explanation = "Alex North komponierte einen vollständigen Score für '2001', den Kubrick jedoch ohne Vorwarnung durch klassische Stücke (Strauss, Ligeti, Khachaturian) ersetzte. North erfuhr davon erst bei der Weltpremiere im Kino. Kubrick nutzte Ligetis atonale Orchesterstücke, aber als Voraufnahmen — nicht als Auftragskomposition. North's Score wurde 1993 posthum eingespielt und veröffentlicht.",
        difficulty = 4,
        funFact = "Kubrick hatte Ligetis Musik ohne dessen Erlaubnis verwendet. Ligeti klagte erfolgreich und erhielt eine finanzielle Entschädigung — er bestand aber darauf, dass sein Name im Abspann erscheint."
    ),

    // --- KAMERATECHNIKEN ---

    // Question 9 - Steadicam Erfindung
    Question(
        categoryId = 4,
        questionText = "Wer erfand die Steadicam und gewann dafür 1978 einen technischen Ehren-Oscar?",
        answerA = "Gordon Willis",
        answerB = "Haskell Wexler",
        answerC = "Garrett Brown",
        answerD = "Conrad Hall",
        correctAnswer = 2,
        explanation = "Garrett Brown erfand die Steadicam, ein Kamerastabilisierungssystem, das die Kamera über ein Federgelenksystem vom Körper des Kameramanns entkoppelt. Die Kamera kann so fließend bewegt werden, ohne das Verwackeln handgehaltener Shots oder die Starrheit eines Krans zu haben. Brown führte die Steadicam selbst in Kubricks 'Shining' und Stallones 'Rocky' ein.",
        difficulty = 4,
        funFact = "Brown baute den ersten Prototyp der Steadicam in seinem Keller. Die berühmte Treppenszene in 'Rocky' (1976) war eine der ersten kommerziellen Verwendungen — sie wurde in einem einzigen Take gedreht."
    ),

    // Question 10 - Dolly Zoom (Vertigo-Effekt)
    Question(
        categoryId = 4,
        questionText = "Der 'Dolly Zoom' — auch bekannt als 'Vertigo-Effekt' — erzeugt ein Gefühl von Desorientierung. Was passiert dabei technisch?",
        answerA = "Die Kamera dreht sich um die eigene Achse, während das Motiv feststeht",
        answerB = "Die Kamera wird vom Motiv wegbewegt, während gleichzeitig das Objektiv heranzoomt",
        answerC = "Die Brennweite wird konstant gehalten, aber das Bild wird digital nachträglich gezoomt",
        answerD = "Zwei Kameras mit verschiedenen Brennweiten werden übergeblendet",
        correctAnswer = 1,
        explanation = "Beim Dolly Zoom bewegt sich die Kamera physisch vom Motiv weg (oder darauf zu), während gleichzeitig das Zoomobjektiv in die entgegengesetzte Richtung zoomt. Das Hauptmotiv bleibt dadurch gleich groß im Bild, während sich der Hintergrund dramatisch ausdehnt oder zusammenzieht. Hitchcock und Kameramann Robert Burks entwickelten den Effekt für 'Vertigo' (1958).",
        difficulty = 4,
        funFact = "Steven Spielberg verwendete den Dolly Zoom in 'Der weiße Hai' (1975) für Chief Brodys Reaktionsshot am Strand so wirkungsvoll, dass der Effekt im Englischen auch 'Jaws Shot' genannt wird."
    ),

    // Question 11 - Jump Cut Erfindung
    Question(
        categoryId = 4,
        questionText = "Der Jump Cut — das absichtliche Entfernen von Mittelstücken aus einer kontinuierlichen Szene — wurde durch welchen Filmemacher salonfähig gemacht?",
        answerA = "François Truffaut",
        answerB = "Jean-Luc Godard",
        answerC = "Alain Resnais",
        answerD = "Louis Malle",
        correctAnswer = 1,
        explanation = "Jean-Luc Godard verwendete Jump Cuts bewusst und provokativ in 'Außer Atem' (À bout de souffle, 1960). Godard schnitt innerhalb einer Einstellung — eine formale Regel, die bis dahin als Fehler galt. Die Technik erzeugt Diskontinuität, Tempo und Verfremdung. Godards Kameramann Raoul Coutard und er wollten damit konventionelle Sehgewohnheiten bewusst brechen.",
        difficulty = 4,
        funFact = "Die Jump Cuts in 'Außer Atem' entstanden zum Teil aus der Not: Der Film war zu lang und musste gekürzt werden. Godard entfernte einfach Teile aus laufenden Szenen statt ganze Szenen — und schuf damit einen neuen Stil."
    ),

    // Question 12 - Match Cut
    Question(
        categoryId = 4,
        questionText = "Stanley Kubricks berühmtester Match Cut verbindet einen geworfenen Knochen mit einem Raumschiff in '2001'. Welches Prinzip macht diesen Schnitt so wirkungsvoll?",
        answerA = "Tonale Kontinuität durch gleichbleibenden Soundtrack",
        answerB = "Formale und thematische Analogie zwischen zwei Bildern",
        answerC = "Eyeline Match zwischen zwei Figuren in verschiedenen Szenen",
        answerD = "Kontinuierliche Kamerabewegung über den Schnitt hinweg",
        correctAnswer = 1,
        explanation = "Ein Match Cut verbindet zwei Einstellungen durch eine visuelle oder inhaltliche Ähnlichkeit der Bilder im Moment des Schnitts. Kubricks Knochen-zu-Raumschiff-Schnitt ist ein thematischer Match Cut: Beide Objekte sind Werkzeuge der Macht und Intelligenz, beide fliegen durch die Luft. Der Schnitt überspringt Millionen Jahre Evolution in einer einzigen Bildsequenz.",
        difficulty = 4,
        funFact = "Der Knochen-zu-Raumschiff-Schnitt gilt als einer der größten Zeitsprünge in der Filmgeschichte: Von der Frühgeschichte der Menschheit zu einer Zukunft, in der Menschen den Weltraum erkunden — verbunden durch einen einzigen Schnitt."
    ),

    // Question 13 - Dutch Angle
    Question(
        categoryId = 4,
        questionText = "Der 'Dutch Angle' — eine schräg geneigte Kameraachse — wurde in welcher deutschen Filmströmung der 1920er Jahre besonders häufig eingesetzt?",
        answerA = "Neue Sachlichkeit",
        answerB = "Expressionismus",
        answerC = "Kammerspielfilm",
        answerD = "Bergfilm",
        correctAnswer = 1,
        explanation = "Der Deutsche Expressionismus (Stummfilmära, 1920er) nutzte den Dutch Angle extensiv, um psychologische Instabilität, Wahnsinn und Bedrohung zu visualisieren. Filme wie 'Das Cabinet des Dr. Caligari' (1920) oder 'Nosferatu' (1922) prägten diesen Stil. Der englische Begriff 'Dutch Angle' leitet sich wahrscheinlich von 'Deutsch Angle' ab.",
        difficulty = 4,
        funFact = "Im deutschen Expressionismus wurden Kameraneigung und schräge Sets oft kombiniert: Nicht nur die Kamera war schräg, sondern auch Wände, Möbel und Straßen waren bewusst verzerrt gebaut, um eine alptraumhafte Welt zu erschaffen."
    ),

    // Question 14 - Split Diopter
    Question(
        categoryId = 4,
        questionText = "Brian De Palma verwendete häufig einen 'Split Diopter' — was ist das und welche visuelle Wirkung erzeugt er?",
        answerA = "Ein Halblinsen-Vorsatz, der zwei verschiedene Fokusebenen gleichzeitig scharf abbildet",
        answerB = "Ein Prisma, das das Bild horizontal teilt und spiegelt",
        answerC = "Eine Doppelbelichtungstechnik für überlagerte Bildebenen",
        answerD = "Ein anamorphes Objektiv, das das Bild auf Cinemascope-Format streckt",
        correctAnswer = 0,
        explanation = "Ein Split Diopter ist ein Vorsatzfilter, der nur eine Hälfte des Objektivs abdeckt. Die Hälfte mit dem Diopter fokussiert auf ein nahes Objekt, die andere Hälfte fokussiert auf etwas Weites. Das Ergebnis: Beide Bereiche erscheinen gleichzeitig scharf, obwohl sie unterschiedliche Abstände haben — physikalisch normalerweise unmöglich. De Palma nutzte ihn in 'Carrie', 'Dressed to Kill' und anderen Filmen.",
        difficulty = 4,
        funFact = "Der Split Diopter hinterlässt eine charakteristische unschärfe Linie in der Bildmitte, wo die zwei Brennweiten aufeinandertreffen. Geübte Zuschauer können diesen 'Split Diopter Seam' oft erkennen."
    ),

    // --- SCHNITTTECHNIKEN ---

    // Question 15 - Kuleschow-Effekt
    Question(
        categoryId = 4,
        questionText = "Der Kuleschow-Effekt bewies in den 1920er Jahren, dass Bedeutung im Film nicht durch einzelne Einstellungen entsteht. Welches Prinzip beschreibt er?",
        answerA = "Schnelle Schnittfolge erhöht die emotionale Intensität linear",
        answerB = "Die Bedeutung einer Einstellung wird durch die vorherige und nachfolgende Einstellung beeinflusst",
        answerC = "Lange Einstellungen erzeugen mehr Spannung als kurze",
        answerD = "Bewegung im Bild bestimmt das Schnittrhythmus",
        correctAnswer = 1,
        explanation = "Lew Kuleschow demonstrierte: Zeigt man das gleiche neutrale Gesicht eines Schauspielers abwechselnd nach einem Teller Suppe, einem Sarg und einer spielenden Frau, interpretieren Zuschauer die gleiche Einstellung als Hunger, Trauer bzw. Verlangen. Die Bedeutung entsteht durch die Montage, nicht durch das Bild allein — ein Grundprinzip der Filmsprache.",
        difficulty = 4,
        funFact = "Kuleschow führte das Experiment mit dem russischen Stummfilm-Star Ivan Mosjoukine durch. In Wahrheit existieren Zweifel, ob das Experiment exakt so stattfand — aber das Prinzip wurde durch spätere Studien bestätigt."
    ),

    // Question 16 - Walter Murch Schnittregeln
    Question(
        categoryId = 4,
        questionText = "Walter Murch, Oscar-Gewinner für 'Apocalypse Now' und 'Der englische Patient', formulierte die 'Rule of Six' für gute Schnittentscheidungen. Was steht bei ihm an erster Stelle?",
        answerA = "Kontinuität des Raums",
        answerB = "Kontinuität der Bewegung",
        answerC = "Emotionale Wirkung auf den Zuschauer",
        answerD = "Kontinuität des Tons",
        correctAnswer = 2,
        explanation = "Walter Murch's 'Rule of Six' priorisiert: 1. Emotion, 2. Story, 3. Rhythmus, 4. Eyeline, 5. Zweidimensionale Filmebene, 6. Dreidimensionaler Raum. Das Wichtigste ist die emotionale Wirkung — ein Schnitt der emotional richtig sitzt, ist wichtiger als räumliche oder zeitliche Kontinuität. Technische Fehler werden vom Zuschauer verziehen, emotionale nicht.",
        difficulty = 4,
        funFact = "Murch war der erste, der einen Spielfilm digital schnitt: 'Der englische Patient' (1996) schnitt er auf einem Avid-System. Er gewann dafür den Oscar für den besten Schnitt."
    ),

    // Question 17 - Parallelmontage
    Question(
        categoryId = 4,
        questionText = "D.W. Griffith gilt als Erfinder der 'Parallelmontage' (Cross-Cutting). Was beschreibt diese Technik?",
        answerA = "Gleichzeitige Handlungsstränge werden abwechselnd gezeigt, um Spannung zu erzeugen",
        answerB = "Zwei Kameras filmen dieselbe Szene aus verschiedenen Winkeln gleichzeitig",
        answerC = "Szenen werden zeitversetzt mit identischem Bildmaterial gezeigt",
        answerD = "Handlungsmomente werden durch Zwischentitel unterbrochen",
        correctAnswer = 0,
        explanation = "Griffith entwickelte die Parallelmontage in Stummfilmen wie 'Birth of a Nation' (1915) und 'Intolerance' (1916): Zwei oder mehr gleichzeitig stattfindende Handlungsstränge werden alternierend gezeigt. Der Zuschauer versteht die Gleichzeitigkeit implizit. Diese Technik erzeugt Spannung (wird die Rettung rechtzeitig kommen?) und ist bis heute Standard in Actionfilmen.",
        difficulty = 4,
        funFact = "Griffith soll diese Technik von der zeitgenössischen Roman-Erzählstruktur inspiriert haben — wo Autoren zwischen verschiedenen Schauplätzen wechselten. Er übertrug das literarische Prinzip auf den Film."
    ),

    // --- FILMPRODUKTIONS-KNOW-HOW ---

    // Question 18 - Chroma Key Geschichte
    Question(
        categoryId = 4,
        questionText = "Das moderne 'Green Screen' (Chroma-Key)-Verfahren nutzt spezifisch grünen Stoff — aus welchem praktischen Grund wurde Grün gegenüber Blau bevorzugt?",
        answerA = "Grüner Stoff ist billiger und leichter erhältlich",
        answerB = "Digitale Bildsensoren sind empfindlicher für grüne Wellenlängen und erzeugen weniger Rauschen",
        answerC = "Grün ist die Komplementärfarbe zu Hauttönen",
        answerD = "Grüne Farbkanäle haben mehr natürliche Kontraste in der Außenwelt",
        correctAnswer = 1,
        explanation = "Digitale Kamerasensoren (Bayer-Filter) verwenden doppelt so viele grüne Photodioden wie rote oder blaue — weil das menschliche Auge am empfindlichsten für grünes Licht ist. Dadurch hat der Grünkanal die höchste Auflösung und das geringste Rauschen, was präzisere Keys mit saubereren Kanten ermöglicht. Bei Filmkameras mit analogem Material war Blau häufiger.",
        difficulty = 4,
        funFact = "Für die Hobbit-Trilogie und andere Fantasy-Produktionen wird manchmal auch roter oder orangefarbener Screen verwendet — wenn Schauspieler grüne Kostüme tragen müssen, die sonst weggekeyt würden."
    ),

    // Question 19 - Motion Capture
    Question(
        categoryId = 4,
        questionText = "Bei der 'Performance Capture'-Technik in 'Avatar' (2009) wurde eine spezielle Kamera verwendet, die James Cameron als 'Simulcam' bezeichnete. Was konnte diese?",
        answerA = "Vier verschiedene Brennweiten gleichzeitig aufnehmen",
        answerB = "CGI-Charaktere in Echtzeit in die Live-Action-Aufnahmen einfügen",
        answerC = "3D-Stereoskopie ohne nachträgliche Konvertierung aufnehmen",
        answerD = "Infrarotmarker automatisch erkennen und verfolgen",
        correctAnswer = 1,
        explanation = "Camerons Simulcam kombinierte eine Kamerafeed mit der virtuellen CGI-Welt in Echtzeit: Wenn Cameron die Kamera bewegte, konnte er auf dem Monitor sofort sehen, wie CGI-Charaktere und -Umgebungen live in die Szene integriert wirkten. Dies ermöglichte ihm, Regie zu führen, als wären die virtuellen Charaktere physisch anwesend.",
        difficulty = 4,
        funFact = "Andy Serkis, der Gollum in 'Herr der Ringe' und Caesar in 'Planet der Affen' spielte, kämpfte jahrelang dafür, dass Performance-Capture-Schauspieler für Oscar-Nominierungen in der Schauspielkategorie berücksichtigt werden."
    ),

    // Question 20 - Foley-Kunst
    Question(
        categoryId = 4,
        questionText = "Jack Foley gilt als Erfinder des 'Foley'-Verfahrens, bei dem Alltagsgeräusche live zur Filmbild synchronisiert werden. Welchen bekannten Klang erzeugen Foley-Künstler traditionell durch Maismehl?",
        answerA = "Das Knirschen von Sand oder Schnee unter Füßen",
        answerB = "Das Knacken brechender Knochen",
        answerC = "Das Geräusch eines explodierenden Feuerballs",
        answerD = "Das Rauschen von Meeresrauschen",
        correctAnswer = 0,
        explanation = "Foley-Künstler erzeugen das Knirschen von Sand oder Schnee unter Füßen, indem sie Maismehl (Cornstarch) in einem Ledersack oder Kissenüberzug komprimieren und damit auf ein Metallblech treten. Das erzeugt genau den charakteristischen knirschenden Sound, der echt klingt. Jeder Sound hat sein eigenes kreatives Rezept.",
        difficulty = 4,
        funFact = "Foley-Künstler haben eine eigene Sammlung skurriler Materialien: Selleriestangen für brechende Knochen, Kokosnussschalen für Pferdehufe (aus 'Monty Python') und nasse Lederhandschuhe für Schläge im Kampf."
    ),

    // Question 21 - ADR (Automated Dialogue Replacement)
    Question(
        categoryId = 4,
        questionText = "ADR steht für 'Automated Dialogue Replacement'. Bei welchem berühmten Film wurde so viel ADR nachbearbeitet, dass der Originaldialog fast vollständig ersetzt wurde?",
        answerA = "Apocalypse Now",
        answerB = "2001: Odyssee im Weltraum",
        answerC = "Blade Runner",
        answerD = "Fitzcarraldo",
        correctAnswer = 0,
        explanation = "Bei 'Apocalypse Now' (1979) wurden große Teile des Dialogs in ADR-Sessions nachbearbeitet — einerseits wegen technischer Probleme während der Dreharbeiten im Dschungel, andererseits weil Marlon Brando ohne das Buch gelesen zu haben am Set improvisierte und viele Takes von unterschiedlicher Qualität waren. Coppola und Murch mussten den Film in der Postproduktion buchstäblich neu zusammensetzen.",
        difficulty = 4,
        funFact = "Die Dreharbeiten zu 'Apocalypse Now' wurden von so vielen Katastrophen begleitet — Taifun, Herz-OP von Coppola, Herzattacke von Martin Sheen, Brando zu dick —, dass Eleanor Coppola alles dokumentierte. Daraus entstand der Dokumentarfilm 'Hearts of Darkness' (1991)."
    ),

    // Question 22 - VistaVision
    Question(
        categoryId = 4,
        questionText = "Alfred Hitchcock drehte 'Vertigo' (1958) in VistaVision — einem von Paramount entwickelten Format. Was war das technische Besondere daran?",
        answerA = "Der Film lief horizontal durch die Kamera statt vertikal",
        answerB = "Es wurden zwei Filmrollen gleichzeitig belichtet",
        answerC = "Das Format verwendete 70mm-Film statt 35mm",
        answerD = "Die Kamera rotierte um 90 Grad zur Aufnahme",
        correctAnswer = 0,
        explanation = "VistaVision lief den Standard-35mm-Film horizontal durch die Kamera (statt vertikal wie üblich), wodurch jedes Einzelbild eine achtmal größere Fläche hatte als normales 35mm. Dies ermöglichte extreme Schärfe und Detailreichtum, besonders wichtig für Special Effects wie den Dolly Zoom in 'Vertigo'. Das Negativ wurde dann auf normales vertikales 35mm umkopiert.",
        difficulty = 4,
        funFact = "VistaVision wurde von Paramount als Antwort auf Cinemascope entwickelt, das von Fox eingeführt worden war — beide Firmen kämpften in den 1950ern um das beste Breitwandformat, um dem aufkommenden Fernsehen zu begegnen."
    ),

    // Question 23 - Anamorphe Objektive
    Question(
        categoryId = 4,
        questionText = "Anamorphe Objektive erzeugen bei Lichtquellen im Bild charakteristische horizontale Lichtstreifen ('Lens Flares'). Woran liegt das?",
        answerA = "An der elliptischen Form der Linsenelemente, die Licht horizontal stärker beugen",
        answerB = "An der geraden Blendengeometrie, die durch die Kompression entsteht",
        answerC = "Am speziellen Antireflexbeschichtung, die horizontale Wellenlängen bevorzugt",
        answerD = "An internen Prismen, die das Bild in der Horizontalen komprimieren",
        correctAnswer = 0,
        explanation = "Anamorphe Objektive verwenden zylindrische Linsenelemente, die das Bild horizontal auf die Hälfte komprimieren (2:1) und beim Projizieren wieder entzerren. Diese zylindrischen Elemente beugen Lichtquellen elliptisch — die Horizontale wird stärker gebrochen als die Vertikale. Das Ergebnis: Charakteristische horizontale Lichtstreifen, die das 'cinematic look' anamorphischer Aufnahmen prägen.",
        difficulty = 4,
        funFact = "J.J. Abrams ist so fasziniert von Lens Flares, dass er sie in 'Star Trek' (2009) exzessiv einsetzte — sogar in Szenen, die keine sichtbare Lichtquelle hatten. Später räumte er ein, es übertrieben zu haben."
    ),

    // Question 24 - Weta Digital vs. ILM
    Question(
        categoryId = 4,
        questionText = "Industrial Light & Magic (ILM) wurde 1975 von George Lucas gegründet, um die VFX für 'Star Wars' zu realisieren. Welche Technik revolutionierten sie für 'The Abyss' (1989)?",
        answerA = "Digitales Rotoscoping für photoreal compositing",
        answerB = "Computeranimierte wasserbasierte Figuren (flüssige Morphs)",
        answerC = "Motion Capture für non-humane Charaktere",
        answerD = "Hochgeschwindigkeits-Filmkameras für Zeitlupeneffekte",
        correctAnswer = 1,
        explanation = "Für James Camerons 'The Abyss' (1989) entwickelte ILM die erste computeranimierte realistische Wasserfigur: Das 'Water Tentacle' war eine CGI-Kreatur, die organisch und glaubwürdig Wasser simulierte. Dieser Durchbruch ebnete den Weg für den flüssigen T-1000 in 'Terminator 2' (1991) und alle späteren Fluid-Simulationen.",
        difficulty = 4,
        funFact = "Das Water Tentacle in 'The Abyss' kostete mehr Zeit und Geld zu animieren als alle anderen VFX-Elemente zusammen — obwohl es nur wenige Sekunden im Film zu sehen ist. ILM brauchte 6 Monate allein für diese eine Sequenz."
    ),

    // Question 25 - Praktische vs. digitale Effekte
    Question(
        categoryId = 4,
        questionText = "Christopher Nolan ist bekannt dafür, praktische Effekte gegenüber CGI zu bevorzugen. Wie wurde der drehende Korridor in 'Inception' (2010) realisiert?",
        answerA = "Vollständig als CGI im Nachhinein erstellt",
        answerB = "Ein vollständig rotierendes Set, das echte Schauspieler aufnahm",
        answerC = "Mit Kabeln aufgehängte Schauspieler in einem stillen Set",
        answerD = "Mit Hilfe von Tilt-Shift-Objektiven und forced perspective",
        correctAnswer = 1,
        explanation = "Produktionsdesigner Guy Hendrix Dyas baute einen 100 Meter langen Hotelkorridor auf einem riesigen Drehgestell, das sich vollständig um 360 Grad rotieren konnte. Der Korridor mit allen Möbeln, Lampen und dem Filmset drehte sich wirklich — Schauspieler Gordon-Levitt trainierte monatelang Choreografien für die Schwerelosigkeitsszenen. Der Dreh dauerte sechs Wochen.",
        difficulty = 4,
        funFact = "Gordon-Levitt und die Stuntmen trainierten wochenlang in einer kleinen Prototyp-Version des Korridors, bevor das vollständige Set gebaut wurde. Mehrere Crewmitglieder trugen Verletzungen davon."
    ),

    // --- SET-DESIGN ---

    // Question 26 - Production Designer Ken Adam
    Question(
        categoryId = 4,
        questionText = "Production Designer Ken Adam prägte mit seinen futuristischen Sets das visuelle Erscheinungsbild einer ganzen Filmreihe. Für welche Produktionen ist er besonders berühmt?",
        answerA = "Star-Trek-Filmreihe und 2001: Odyssee im Weltraum",
        answerB = "James-Bond-Filme und Dr. Strangelove",
        answerC = "Alien-Franchise und Blade Runner",
        answerD = "Indiana-Jones-Reihe und Hook",
        correctAnswer = 1,
        explanation = "Ken Adam (1921–2016) schuf die ikonischen Interieurs für sieben James-Bond-Filme von 'Dr. No' (1962) bis 'Moonraker' (1979) sowie Stanley Kubricks 'Dr. Seltsam oder: Wie ich lernte, die Bombe zu lieben' (1964). Sein War Room in 'Dr. Strangelove' gilt als eines der einflussreichsten Filmsets aller Zeiten. Adam kombinierte expressionistische Geometrie mit Science-Fiction-Ästhetik.",
        difficulty = 4,
        funFact = "Der War Room in 'Dr. Strangelove' existierte nie wirklich — er war ein reines Bühnenbild. Als US-Präsident Ronald Reagan nach seiner Wahl zum ersten Mal das Pentagon besuchte, fragte er, wo der War Room sei. Sein Stab musste ihn korrigieren."
    ),

    // Question 27 - Blade Runner Set
    Question(
        categoryId = 4,
        questionText = "Das visuelle Konzept des 'Retrofitting' in Ridley Scotts 'Blade Runner' (1982) — eine Zukunft, in der alte Gebäude mit neuer Technologie überladen werden — basierte auf welchem realen Stadtbild?",
        answerA = "Hongkong und Tokio",
        answerB = "Los Angeles Downtown und das dichte Zusammenwachsen von Alt und Neu",
        answerC = "São Paulo und Lagos als Megastädte",
        answerD = "Amsterdam und seine Kanal-Viertel",
        correctAnswer = 0,
        explanation = "Production Designer Lawrence G. Paull und Visual Futurist Syd Mead inspirierten sich an asiatischen Großstädten — besonders Hongkong und Tokio — wo neonskimmernde Werbetafeln, dichte Besiedlung und veraltete Architektur mit neuen Technologien verschmelzen. Das 'Noir Cityscape' kombinierte Art Deco mit Cyberpunk-Elementen: alte Gebäude mit Neonreklame, Kabelgewirr und fliegenden Autos.",
        difficulty = 4,
        funFact = "Viele Szenen von 'Blade Runner' wurden auf dem Warner-Bros.-Backlot in Burbank gedreht — dem gleichen Set, das für 'Dick Tracy' und viele andere Filme verwendet wurde. Der Regen war notwendig, um die Tageslichtshoots wie Nacht wirken zu lassen."
    ),

    // Question 28 - Barry Lyndon Kerzenlicht
    Question(
        categoryId = 4,
        questionText = "Stanley Kubrick drehte für 'Barry Lyndon' (1975) Szenen ausschließlich bei Kerzenlicht. Welches spezielle Objektiv verwendete er dafür?",
        answerA = "Ein Fisheye-Objektiv mit f/0,7 Blende",
        answerB = "Ein NASA-Objektiv mit f/0,7 Blende",
        answerC = "Ein Anamorphot-Objektiv für maximale Lichtausbeute",
        answerD = "Ein Teleobjektiv mit extremem Lichtverstärker",
        correctAnswer = 1,
        explanation = "Kubrick erwarb drei Zeiss Planar 50mm f/0,7-Objektive, die ursprünglich für die NASA entwickelt worden waren (für Mondlandungsaufnahmen im Apollo-Programm). Diese extrem lichtstarken Objektive mit einer Anfangsblende von f/0,7 ermöglichten es, Szenen bei echtem Kerzenlicht zu drehen ohne künstliche Beleuchtung. Kameramann John Alcott entwickelte spezielle Rigs für die starren Linsen.",
        difficulty = 4,
        funFact = "Die Zeiss-f/0,7-Objektive haben eine extrem geringe Schärfentiefe: Schon wenige Zentimeter vor oder hinter dem Fokuspunkt verschwimmt das Bild. Das gibt den Kerzenszenen in 'Barry Lyndon' ihre charakteristische, gemäldehafte Tiefe."
    ),

    // Question 29 - Set-Design Citizen Kane
    Question(
        categoryId = 4,
        questionText = "Citizen Kane (1941) war technisch revolutionär durch 'Deep Focus' — viele Ebenen scharf abgebildet. Welche Set-Design-Entscheidung ermöglichte den tiefen Blickwinkel von unten?",
        answerA = "Spezielle Weitwinkelobjektive ohne Bildverzerrung",
        answerB = "Abgehängte Decken aus Musselin, die im Bild bleiben konnten",
        answerC = "Niedrig gebaute Böden, sodass Kameras im Erdgeschoss filmen konnten",
        answerD = "Vollständige 360-Grad-Sets ohne Vorderwand",
        correctAnswer = 1,
        explanation = "Für 'Citizen Kane' baute Produktionsdesigner Van Nest Polglase Sets mit echten Decken aus Musselin-Stoff — anstatt offener Sets, die von oben beleuchtet werden. Diese Decken ermöglichten extreme Untersichten (Low Angles), bei denen die Kamera fast auf dem Boden lag und nach oben schaute. Das war damals absolut unüblich und gilt als eine der innovativsten Set-Design-Entscheidungen der Filmgeschichte.",
        difficulty = 4,
        funFact = "Gregg Toland, der Kameramann von 'Citizen Kane', hatte Orson Welles bewusst um die Möglichkeit gebeten, den Film zu drehen — er sah Welles als jemanden, der bereit wäre, unkonventionelle Techniken auszuprobieren. Toland wurde als Co-Regisseur im Abspann genannt, was extrem selten war."
    ),

    // Question 30 - Avatar Pandora Set
    Question(
        categoryId = 4,
        questionText = "Für 'Avatar' (2009) entwickelte James Cameron zusammen mit Botanikern und Illustratoren eine vollständige Ökologie für Pandora. Wie viele einzigartige Tier- und Pflanzenarten wurden für den Film konzipiert?",
        answerA = "Über 1000 verschiedene Spezies",
        answerB = "Circa 350 Spezies",
        answerC = "Genau 120 Spezies für das finale Design",
        answerD = "Unter 50, aber extrem detailliert ausgearbeitet",
        correctAnswer = 0,
        explanation = "Cameron arbeitete mit einem Team aus Botanikern, Zoologen und Illustratoren über Jahre hinweg, um eine glaubwürdige Biologie für Pandora zu entwickeln. Über 1000 verschiedene Flora- und Fauna-Arten wurden konzipiert, von denen ein Bruchteil im Film sichtbar ist. Das vollständige Pandora-Ökosystem wurde in einem 'Pandorapedia' genannten internen Nachschlagewerk dokumentiert.",
        difficulty = 4,
        funFact = "Viele Pandora-Pflanzen sind biolumineszent inspiriert durch Meeresorganismen. Cameron tauchte tief in die Tiefsee und ließ sich von realen biolumineszenten Tieren für das Navi-Wald-Design inspirieren."
    ),

    // --- KOSTÜMDESIGN ---

    // Question 31 - Edith Head
    Question(
        categoryId = 4,
        questionText = "Edith Head gewann acht Academy Awards für das Kostümdesign — mehr als jede andere Frau in der Oscar-Geschichte. Für welchen Film gewann sie ihren ersten Oscar?",
        answerA = "Sunset Boulevard",
        answerB = "All About Eve",
        answerC = "Rear Window",
        answerD = "Roman Holiday",
        correctAnswer = 1,
        explanation = "Edith Head gewann ihren ersten Oscar für das Kostümdesign zu 'All About Eve' (1950), einem der meistnomin Oscar-Filme aller Zeiten. Sie arbeitete eng mit Alfred Hitchcock zusammen und kleidete Stars wie Grace Kelly, Audrey Hepburn und Elizabeth Taylor ein. Ihr Stil kombinierte elegante Einfachheit mit strategischer Charakterpsychologie.",
        difficulty = 4,
        funFact = "Edith Head inspirierte die Figur der Edna Mode in Pixars 'The Incredibles' (2004) — die Mode-Designerin, die keine Umhänge für Superhelden akzeptiert. Die Ähnlichkeit in Erscheinung und Persönlichkeit ist bewusst."
    ),

    // Question 32 - Mad Max Fury Road Kostüm
    Question(
        categoryId = 4,
        questionText = "Costume Designer Jenny Beavan gewann für 'Mad Max: Fury Road' (2016) den Oscar — trotz großem Widerstand. Welches ungewöhnliche Material bildete die Basis vieler Kostüme in diesem Film?",
        answerA = "Recyceltes Militärmaterial aus echten Armee-Beständen",
        answerB = "Fundstücke aus echtem Müll, Schrottplätzen und Secondhand-Läden",
        answerC = "Handgefertigte Rohleder-Rohlinge aus Australien",
        answerD = "3D-gedruckte Polymer-Komponenten in Kombination mit Stoff",
        correctAnswer = 1,
        explanation = "Jenny Beavan und ihr Team sammelten Materialien aus Schrottplätzen, Müllhalden und Secondhand-Läden in Namibia (dem Drehort) und Australien. Die zerstörte Post-Apokalypse-Welt von Fury Road verlangte Kostüme, die wie echte Überreste einer zerfallenden Zivilisation wirken — dafür verwendeten sie tatsächlichen Abfall, den sie verarbeiteten, modifizierten und kombinierten.",
        difficulty = 4,
        funFact = "Bei der Oscarverleihung sorgte Beavans Erscheinung in Lederjacke und Schal für Aufsehen — viele fanden es respektlos. Beavan selbst erklärte, ihr Outfit sei genau das, was sie auch bei der Arbeit trägt: praktisch und ehrlich."
    ),

    // Question 33 - Sandy Powell Kostümdesign
    Question(
        categoryId = 4,
        questionText = "Sandy Powell ist die einzige Person, die drei Oscars für Kostümdesign gewonnen hat, ohne in einem einzigen Jahr nominiert zu sein. Für welchen Film gewann sie ihren dritten Oscar?",
        answerA = "Shakespeare in Love",
        answerB = "The Aviator",
        answerC = "The Young Victoria",
        answerD = "Mrs. Doubtfire",
        correctAnswer = 2,
        explanation = "Sandy Powell gewann Oscars für 'Shakespeare in Love' (1998), 'The Aviator' (2004) und 'The Young Victoria' (2009). Sie ist bekannt für ihre detailgenaue Recherche historischer Kostüme und ihre Fähigkeit, authentische Perioden-Kleidung zu schaffen, die gleichzeitig filmisch funktioniert. Ihre Arbeit umfasst Genres von der Periode bis zur Gegenwart.",
        difficulty = 4,
        funFact = "Powell beginnt jedes Projekt mit umfangreicher Archivrecherche — für 'The Aviator' studierte sie Hunderte von Magazinen, Fotografien und Filmaufnahmen aus den 1920er bis 1950er Jahren, um Howard Hughes' Kostüme akkurat darzustellen."
    ),

    // Question 34 - Milena Canonero
    Question(
        categoryId = 4,
        questionText = "Milena Canonero gewann vier Oscars für das Kostümdesign. Für welchen Wes-Anderson-Film erhielt sie ihren bisher letzten Oscar?",
        answerA = "The Royal Tenenbaums",
        answerB = "The Life Aquatic",
        answerC = "The Grand Budapest Hotel",
        answerD = "Moonrise Kingdom",
        correctAnswer = 2,
        explanation = "Milena Canonero gewann 2015 den Oscar für 'The Grand Budapest Hotel' (2014). Sie hatte zuvor bereits drei Oscars für 'Barry Lyndon' (1975), 'Chariots of Fire' (1981) und 'Marie Antoinette' (2006) erhalten. Für 'The Grand Budapest Hotel' entwarf sie die pastellfarbenen, präzise designten Kostüme, die Wes Andersons visuellen Stil perfekt ergänzen.",
        difficulty = 4,
        funFact = "Canonero arbeitete mit Wes Anderson sehr eng zusammen: Viele seiner Charaktere werden durch ihre Kostüme definiert, bevor das Drehbuch fertig ist. Für 'The Grand Budapest Hotel' inspirierten Canoneros Kostümentwürfe Andersons weitere Ausarbeitung der Charaktere."
    ),

    // Question 35 - Natürliche Farbpaletten im Kostüm
    Question(
        categoryId = 4,
        questionText = "In Steven Spielbergs 'Schindlers Liste' (1993) erscheint ein kleines Mädchen in einem roten Mantel — als einziges Farbelement in einem Schwarzweißfilm. Welchem dramaturgischen Zweck dient diese Technik?",
        answerA = "Technischer Fehler beim Kolorierungsprozess, der absichtlich belassen wurde",
        answerB = "Symbolisierung der unschuldigen Opfer und ihrer Sichtbarkeit in der Geschichte",
        answerC = "Markierung einer Schlüsselfigur, der Schindler persönlich helfen wird",
        answerD = "Referenz auf eine tatsächlich dokumentierte Person im realen Schindler-Fall",
        correctAnswer = 1,
        explanation = "Spielberg und Production Designer Allan Starski verwendeten das rote Mädchen als Symbol für die Unschuld der Opfer: Obwohl die NS-Gräuel sichtbar stattfanden, schaute die Welt weg. Das Rot steht für das, was man hätte sehen müssen. Das Mädchen taucht später auf — in einer Szene, die das Versagen der Welt symbolisiert. Die Technik bezeichnet man als 'selective color' oder 'spot color'.",
        difficulty = 4,
        funFact = "Das Mädchen im roten Mantel ist inspiriert von Roma Ligocka, einer polnischen Autorin, die als Kind die NS-Zeit überlebte. Sie erkannte sich nach dem Film und schrieb eine Memoiren. Spielberg wusste nichts von ihr — es war reine Parallelität."
    ),

    // Question 36 - Kostüm als Charakterentwicklung
    Question(
        categoryId = 4,
        questionText = "Costume Designerin Trish Summerville kleidete Katniss Everdeen in der 'Tribute von Panem'-Reihe konsequent nach einem Farbprinzip. Was signalisierte der Wechsel von natürlichen zu metallischen Farben?",
        answerA = "Den Übergang von Armut zu Wohlstand in der Kapitol-Gesellschaft",
        answerB = "Die zunehmende Verwandlung von einer normalen Person zu einer Symbol-Figur des Widerstands",
        answerC = "Die praktische Notwendigkeit für Kampfszenen im Späteren der Reihe",
        answerD = "Die Jahreszeiten der Hungerspiele, die sich über die Trilogie ziehen",
        correctAnswer = 1,
        explanation = "Summerville und Regisseur Francis Lawrence entwarfen Katniss' Kostümentwicklung als visuelles Narrativ: Zu Beginn trägt sie erdig-natürliche Töne (Grau, Braun) für ihre Herkunft aus Distrikt 12. Im Verlauf werden die Kostüme zunehmend aufwendiger, metallischer und symbolischer — sie wird zur 'Spotttölpel', einer Symbolfigur, die selbst zum Kostüm geworden ist. Das Kostüm spiegelt ihre innere Transformation.",
        difficulty = 4,
        funFact = "Das Mockingjay-Flammenkostüm in 'Die Tribute von Panem' wurde von einem realen Feuereffekt-Techniker mitentworfen: Die Flammen an Katniss' Rüstung sollten im Film auch tatsächlich real brennen können — eine Herausforderung für das Kostümdesign-Team."
    ),

    // Question 37 - Colleen Atwood Tim Burton
    Question(
        categoryId = 4,
        questionText = "Colleen Atwood ist Tim Burtons bevorzugte Costume Designerin und hat für ihn zahlreiche Filme eingekleidet. Für welchen Film gewann sie ihren ersten von drei Oscars?",
        answerA = "Edward Scissorhands",
        answerB = "Chicago",
        answerC = "Sleepy Hollow",
        answerD = "Alice in Wonderland",
        correctAnswer = 1,
        explanation = "Colleen Atwood gewann ihren ersten Oscar für Rob Marshalls 'Chicago' (2002), nicht für einen Tim Burton-Film. Sie hat jedoch drei Oscars: Chicago (2002), Memoirs of a Geisha (2005) und Alice in Wonderland (2010 — tatsächlich ein Burton-Film). Ihre Zusammenarbeit mit Burton umfasst unter anderen 'Edward Scissorhands', 'Sleepy Hollow' und 'Sweeney Todd'.",
        difficulty = 4,
        funFact = "Für 'Edward Scissorhands' entwarf Atwood Edward's Kostüm so, dass Johnny Depp in jeder Szene authentisch mit den Scheren interagieren konnte — echte Klingen mussten manche Bewegungen einschränken, was Depp in seine Darstellung integrierte."
    ),

    // Question 38 - Period-Accurate Kostüme
    Question(
        categoryId = 4,
        questionText = "Für welchen historischen Film wurden Kostüme aus Originalperiodenmaterial angefertigt, was bedeutete, dass Kleider hunderte Jahre alter Stoffe direkt verarbeitet wurden?",
        answerA = "Anna Karenina (2012)",
        answerB = "The Favourite (2018)",
        answerC = "Barry Lyndon (1975)",
        answerD = "Marie Antoinette (2006)",
        correctAnswer = 2,
        explanation = "Für Kubricks 'Barry Lyndon' wurden viele Kostüme aus authentischen Stoffen des 18. Jahrhunderts gefertigt oder waren tatsächlich originale Kleidungsstücke aus der Periode. Milena Canonero reiste durch Europa, um Stoffe, Knöpfe, Spitzen und andere Materialien aus der georgianischen Ära zu erwerben. Teile der Garderobe stammten aus Museumsbeständen.",
        difficulty = 4,
        funFact = "Die Uniformen in 'Barry Lyndon' sind so authentisch, dass Militärhistoriker den Film als Referenzwerk für die Uniformkunde des 18. Jahrhunderts nutzen. Kubrick ließ sogar die Knopfnähte nach historischen Quellen prüfen."
    ),

    // --- WEITERE TECHNIKEN & FILMWISSEN ---

    // Question 39 - Tonformat Dolby Atmos
    Question(
        categoryId = 4,
        questionText = "Dolby Atmos wurde 2012 mit welchem Film für Kinobesucher eingeführt und ermöglichte erstmals eine 3D-Audiodimension?",
        answerA = "The Dark Knight Rises",
        answerB = "Brave – Merida, die tapfere Bogenschützin",
        answerC = "Prometheus",
        answerD = "The Avengers",
        correctAnswer = 1,
        explanation = "Dolby Atmos wurde im Juni 2012 mit Pixars 'Brave – Merida, die tapfere Bogenschützin' erstmals im Kino eingesetzt. Atmos ermöglicht es, Tonsignale als Objekte im dreidimensionalen Raum zu positionieren — nicht mehr nur auf Kanälen links, rechts, center, surround, sondern auch über dem Publikum an beliebigen Positionen. Das System unterstützt bis zu 128 unabhängige Audio-Objekte gleichzeitig.",
        difficulty = 4,
        funFact = "Dolby Atmos kann im Kino bis zu 64 Lautsprecher ansprechen, davon viele an der Decke für die Höhendimension. Zu Hause implementieren Heimkino-Systeme Atmos mit weniger Lautsprechern oder per virtueller Audioverarbeitung."
    ),

    // Question 40 - Color Grading DI
    Question(
        categoryId = 4,
        questionText = "Das 'Digital Intermediate' (DI)-Verfahren revolutionierte die Farbbearbeitung im Film. In welchem Film wurde es 2001 erstmals für einen Kinofilm eingesetzt?",
        answerA = "O Brother, Where Art Thou?",
        answerB = "Moulin Rouge!",
        answerC = "Black Hawk Down",
        answerD = "The Lord of the Rings: The Fellowship of the Ring",
        correctAnswer = 0,
        explanation = "Joel und Ethan Coens 'O Brother, Where Art Thou?' (2000, Kinostart 2001) war der erste Spielfilm, bei dem das gesamte Filmmaterial digitalisiert, farbkorrigiert und dann wieder auf Film ausgegeben wurde — das Digital Intermediate-Verfahren. Kameramann Roger Deakins arbeitete mit dem Labor EFilm zusammen, um die charakteristische, ausgeblichene Sepia-Farbpalette zu erzeugen.",
        difficulty = 4,
        funFact = "Das DI-Verfahren ermöglichte Deakins, den üppig grünen Mississippi-Drehort in eine trockene, sepia-getönte Landschaft zu verwandeln — alles in der Postproduktion. Die Crew musste am Set keine speziellen Filter oder Lichtsetzungen vornehmen."
    ),

    // Question 41 - Filmstock Kodak vs. Fuji
    Question(
        categoryId = 4,
        questionText = "Viele Kameramänner schworen lange auf Kodak- oder Fuji-Filmstock, weil jeder einen unterschiedlichen 'look' hatte. Was ist der Hauptunterschied zwischen diesen beiden Filmstocks?",
        answerA = "Fuji hatte mehr Blaustich und kältere Hauttöne, Kodak wärmere, sattere Rottöne",
        answerB = "Kodak war lichtempfindlicher (höheres ISO), Fuji schärfer",
        answerC = "Fuji war billiger, Kodak hatte bessere Schärfentiefenkontrolle",
        answerD = "Kodak war für Tageslicht, Fuji für Kunstlicht optimiert",
        correctAnswer = 0,
        explanation = "Fuji-Filmstock (besonders Fuji Eterna) renderte Blau- und Grüntöne lebendiger und erzeugte etwas kühlere, blaustichigere Hauttöne. Kodak (besonders Kodak Vision) hatte wärmere Rottöne und ein charakteristisches, sattigeres Aussehen bei Hauttönen. Diese Unterschiede führten zu starken Präferenzen: z.B. schwor David Fincher auf Fuji, während Steven Spielberg Kodak bevorzugte.",
        difficulty = 4,
        funFact = "Fuji zog sich 2013 aus dem Kinofilmstockmarkt zurück, was bei vielen Filmemachern Bedauern auslöste. Kodak war kurz vor der Insolvenz, rettete sich aber durch den anhaltenden Support von Filmemachern wie Quentin Tarantino und Christopher Nolan."
    ),

    // Question 42 - Aspect Ratio und Emotionswirkung
    Question(
        categoryId = 4,
        questionText = "Wes Anderson verwendete in 'The Grand Budapest Hotel' (2014) drei verschiedene Seitenverhältnisse (1.37:1, 1.85:1 und 2.40:1) für verschiedene Zeitebenen. Was ist der kunsthistorische Hintergrund der 1.37:1-Ratio?",
        answerA = "Es entspricht dem Goldenen Schnitt und wurde von Eisenstein propagiert",
        answerB = "Es ist das 'Academy Ratio' — der Standard für Hollywoodfilme von den 1930ern bis in die frühen 1950er Jahre",
        answerC = "Es wurde von Orson Welles für Citizen Kane als einziger Film benutzt",
        answerD = "Es ist das Standardformat für 16mm-Schmalfilm",
        correctAnswer = 1,
        explanation = "Das 1.37:1 Seitenverhältnis ist das 'Academy Ratio' — der Standard des SMPTE (Society of Motion Picture and Television Engineers), der von den 1930ern bis zu den frühen 1950ern für fast alle Hollywood-Produktionen galt, bevor das Breitwandformat (2.35:1 Cinemascope) eingeführt wurde. Anderson nutzte dieses quadratischere Format für die 1930er-Zeitebene in 'The Grand Budapest Hotel' als bewussten historischen Verweis.",
        difficulty = 4,
        funFact = "Anderson ließ sogar das IMAX-Format des Films in verschiedenen Seitenverhältnissen projizieren — in Kinos, die technisch dazu in der Lage waren, wechselte das Bild sichtbar während des Films zwischen den Epochen."
    )
)
