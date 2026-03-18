package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsHard2(): List<Question> = listOf(

    // Question 1 - Silent Film: D.W. Griffith
    Question(
        categoryId = 4,
        questionText = "Welche revolutionaere Schnitttechnik verwendete D.W. Griffith in 'The Birth of a Nation' (1915), bei der er zwei Handlungstraenge abwechselnd schneidet, um Spannung zu erzeugen?",
        answerA = "Jump Cut",
        answerB = "Match Cut",
        answerC = "Parallelmontage",
        answerD = "Continuity Editing",
        correctAnswer = 2,
        explanation = "D.W. Griffith machte die Parallelmontage populaer, bei der zwischen zwei gleichzeitig ablaufenden Handlungsstraengen hin- und hergeschnitten wird. Dieses Verfahren erzeugt grosse Spannung, beispielsweise bei Rettungsszenen. Griffith wandte es systematisch und verfeinert an, auch wenn er es nicht als Erster erfand.",
        difficulty = 3,
        funFact = "Griffiths Film 'The Birth of a Nation' war trotz seiner filmhistorischen Bedeutung wegen seiner rassistischen Darstellung von Beginn an hoch umstritten und fuehlte die Gruendung des Ku-Klux-Klan als Helden."
    ),

    // Question 2 - Silent Film: D.W. Griffith
    Question(
        categoryId = 4,
        questionText = "In welchem Film von 1916 verwob D.W. Griffith vier voneinander unabhaengige Geschichten aus verschiedenen Epochen zu einem einzigen Werk?",
        answerA = "Way Down East",
        answerB = "Intolerance",
        answerC = "Broken Blossoms",
        answerD = "The General",
        correctAnswer = 1,
        explanation = "Griffiths 'Intolerance' (1916) erzaehlt vier Geschichten aus unterschiedlichen Epochen - dem antiken Babylonien, der Passion Christi, dem franzoesischen Bartholomaeusnacht-Massaker und der modernen Zeit - und schneidet diese parallel. Es war eine der teuersten und ambitioniertesten Produktionen des Stummfilmzeitalters.",
        difficulty = 3,
        funFact = "Das babylonische Set von 'Intolerance' war so gewaltig, dass die Holzkulissen jahrelang in Hollywood standen und erst nach dem Erdbeben 1921 abgerissen wurden."
    ),

    // Question 3 - Silent Film: F.W. Murnau
    Question(
        categoryId = 4,
        questionText = "Welche besondere Kameratechnik setzte F.W. Murnau in 'Der letzte Mann' (1924) ein, um die Perspektive der betrunkenen Hauptfigur zu zeigen?",
        answerA = "Vogelperspektive",
        answerB = "Entfesselte Kamera",
        answerC = "Froschperspektive",
        answerD = "Split-Screen",
        correctAnswer = 1,
        explanation = "Murnau und sein Kameramann Karl Freund entwickelten die 'entfesselte Kamera' (Unchained Camera): Die Kamera wurde von ihrem Stativ befreit und bewegte sich frei durch Raeume, auf Schienen, per Seilzug oder wurde koerpergebunden getragen. Fuer die Trunkenheitsszene befestigten sie die Kamera am Koerper des Kameramanns.",
        difficulty = 3,
        funFact = "Karl Freund, Murnaus Kameramann, wanderte spaeter in die USA aus und wurde dort zum gesuchten Cinematographer, unter anderem fuer Bela Lugosis 'Dracula' (1931) und als TV-Kamerachef fuer 'I Love Lucy'."
    ),

    // Question 4 - Silent Film: F.W. Murnau
    Question(
        categoryId = 4,
        questionText = "Welche Technik nutzte Murnau in 'Nosferatu' (1922), um die Szene darzustellen, in der Graf Orlok seinen Sarg blitzschnell aufrichtet?",
        answerA = "Springfeder unter dem Sarg",
        answerB = "Rueckwaertslaufende Kamera",
        answerC = "Doppelbelichtung",
        answerD = "Stop-Motion-Animation",
        correctAnswer = 1,
        explanation = "Murnau filmte, wie der Schauspieler Max Schreck sich langsam in den Sarg legte, und liess den Film dann rueckwaerts abspielen. Dadurch entstand der Eindruck, Orlok richte sich mit unnatuerlicher Starrheit blitzschnell auf. Diese In-Kamera-Tricktechnik war eine der fruehesten Verwendungen des Rueckwaertsfilm-Effekts.",
        difficulty = 3,
        funFact = "Der Film 'Nosferatu' wurde wegen Urheberrechtsverletzung an Bram Stokers 'Dracula' gerichtlich zur Vernichtung aller Kopien verurteilt - einige Kopien ueberlebten jedoch im Verborgenen."
    ),

    // Question 5 - Silent Film: Fritz Lang
    Question(
        categoryId = 4,
        questionText = "Welches optische Prinzip nutzte Fritz Lang bei 'Metropolis' (1927) fuer die Miniaturaufnahmen der Zukunftsstadt, um sie groesser wirken zu lassen?",
        answerA = "Schufftan-Verfahren",
        answerB = "Matte Painting",
        answerC = "Rear Projection",
        answerD = "Anamorphisches Objektiv",
        correctAnswer = 0,
        explanation = "Das Schufftan-Verfahren, entwickelt von Eugen Schufftan, nutzt einen halbdurchlaessigen Spiegel: Ein Miniaturbild wird darin reflektiert und mit Realaufnahmen der Schauspieler optisch kombiniert. Dadurch konnten kleine Modelle mit echten Darstellern in einer Einstellung erscheinen, ohne kostspielige Grosskulissen zu bauen.",
        difficulty = 3,
        funFact = "Eugen Schufftan wanderte spaeter in die USA aus, wo er 1962 fuer 'The Hustler' (mit Paul Newman) den Oscar fuer die beste Kamera erhielt."
    ),

    // Question 6 - Silent Film: Fritz Lang
    Question(
        categoryId = 4,
        questionText = "Fritz Langs 'M - Eine Stadt sucht einen Moerder' (1931) gilt als Langs erster Tonfilm. Welches Leitmotiv nutzt Lang, um den Kindermorder Peter Lorre fuer den Zuschauer akustisch ankuendigen?",
        answerA = "Das Lied 'Lili Marleen'",
        answerB = "Einen Glockenton",
        answerC = "Das Pfeifen von 'In the Hall of the Mountain King'",
        answerD = "Kinderlachen",
        correctAnswer = 2,
        explanation = "Immer wenn der Moerder Hans Beckert (Peter Lorre) erscheint oder sich einer potenziellen Opferin naehert, pfeift er den Anfang von Edvard Griegs 'In der Halle des Bergkoenigs' aus der Peer-Gynt-Suite. Dieses akustische Leitmotiv verknuepft Ton und Dramaturgie auf voellig neue Weise.",
        difficulty = 3,
        funFact = "Peter Lorre, der in Wirklichkeit nicht pfeifen konnte, spielte die Szenen ohne Ton; das Pfeifen wurde spaeter nachtraeglich eingespielt."
    ),

    // Question 7 - Golden Age Hollywood: Studio System
    Question(
        categoryId = 4,
        questionText = "Wie nannte man das Wirtschaftssystem der Golden-Age-Stu dios, bei dem ein Studio von der Produktion ueber den Verleih bis zum Kinobetrieb die gesamte Kette kontrollierte?",
        answerA = "Block Booking",
        answerB = "Vertikale Integration",
        answerC = "Horizontal Integration",
        answerD = "Unit Production",
        correctAnswer = 1,
        explanation = "Die grossen Studios (MGM, Paramount, Warner Bros., RKO, 20th Century Fox) betrieben eine vertikale Integration: Sie produzierten die Filme, vertrieben sie und besassen zugleich eigene Kinoketten. Dieses Monopolsystem wurde 1948 durch das Paramount Decree des US Supreme Courts gebrochen, das den Studios den Besitz von Kinos untersagte.",
        difficulty = 3,
        funFact = "Beim sogenannten 'Block Booking' zwangen Studios die Kinobetreiber, schwache B-Filme zusammen mit begehrten A-Titeln als Paket abzunehmen - ein weiterer Aspekt, der im Paramount Decree verboten wurde."
    ),

    // Question 8 - Golden Age Hollywood: Studio System
    Question(
        categoryId = 4,
        questionText = "Welches Supreme-Court-Urteil von 1948 brach das Monopol der grossen Studios und verpflichtete sie, ihre Kinoketten zu veraeussern?",
        answerA = "United States v. Paramount Pictures",
        answerB = "Loew's Inc. v. Columbia Broadcasting",
        answerC = "MGM v. Grokster",
        answerD = "Burstyn v. Wilson",
        correctAnswer = 0,
        explanation = "Im Fall 'United States v. Paramount Pictures' (1948) entschied der Supreme Court, dass das Studio-System eine illegale Monopolbildung darstellte. Die Entscheidung, bekannt als 'Paramount Decree', zwang die grossen Studios, ihre Kinoketten zu veraeussern, was das klassische Hollywood-Studio-System grundlegend veraenderte.",
        difficulty = 3,
        funFact = "Das Paramount Decree wurde erst 2020 formell aufgehoben, da das Justizministerium es fuer veraltet hielt - angesichts des Streamings als neuer Dominanzkraft in der Filmbranche."
    ),

    // Question 9 - Golden Age Hollywood: Hays Code
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr trat der Motion Picture Production Code (Hays Code) in den USA verbindlich in Kraft?",
        answerA = "1922",
        answerB = "1930",
        answerC = "1934",
        answerD = "1941",
        correctAnswer = 2,
        explanation = "Obwohl der Code bereits 1930 formuliert wurde, wurde er erst ab 1934 unter Joseph Breen strikt durchgesetzt. Das Production Code Administration (PCA) verlangte seitdem ein Zertifikat fuer jeden Film vor dem Verleih. Ohne dieses Zertifikat verweigerten die Studios den Verleih und die meisten Kinos die Vorstellung.",
        difficulty = 3,
        funFact = "Will Hays, Namensgeber des Codes, war zuvor Postminister unter Praesident Harding. Die Filmindustrie engagierte ihn 1922, um nach mehreren Skandalen das oeffentliche Image Hollywoods zu verbessern."
    ),

    // Question 10 - Golden Age Hollywood: Hays Code
    Question(
        categoryId = 4,
        questionText = "Welche spezifische Regelung des Hays Codes besagte, dass ein Bett im Schlafzimmer einer Filmfigur nicht hoher sein durfte als das andere Bett?",
        answerA = "Der 'Double Bed'-Paragraph",
        answerB = "Die 'Twin Beds'-Klausel",
        answerC = "Die 'Marital Bedroom'-Regel",
        answerD = "Der 'Decency Standard'",
        correctAnswer = 1,
        explanation = "Der Hays Code verlangte, dass verheiratete Filmpaare in separaten Einzelbetten ('twin beds') gezeigt werden mussten, sobald eine Schlafszene vorhanden war. Sogar Eheleute durften im Film kein gemeinsames Doppelbett benutzen. Diese Regel sollte jede Andeutung von Sexualitaet - auch innerhalb der Ehe - vermeiden.",
        difficulty = 3,
        funFact = "Lucille Ball und Desi Arnaz in 'I Love Lucy' (1951) waren eines der ersten TV-Paare, das ein Doppelbett im Schlafzimmer zeigen durfte - nach langem Aushandeln mit dem Sender."
    ),

    // Question 11 - Golden Age Hollywood: Contract Players
    Question(
        categoryId = 4,
        questionText = "Was verstand man unter einem 'Contract Player' im Golden Age Hollywood?",
        answerA = "Ein freiberuflicher Schauspieler, der pro Film bezahlt wurde",
        answerB = "Ein Schauspieler in einem langjaehrigen Exklusivvertrag mit einem Studio",
        answerC = "Ein Regisseur mit Endschnittrecht",
        answerD = "Ein Agent, der Vertraege zwischen Studios aushandelte",
        correctAnswer = 1,
        explanation = "Contract Players waren Schauspieler, die in langjaehrigen Exklusivvertraegen an ein Studio gebunden waren - oft fuer sieben Jahre. Das Studio kontrollierte ihre Rollen, ihr oeffentliches Image, ihre Frisur und sogar ihr Privatleben. Wer einen Auftritt verweigerte, wurde suspendiert. Stars wie Bette Davis und Olivia de Havilland kaempften juristisch gegen dieses System.",
        difficulty = 3,
        funFact = "Olivia de Havillands Sieg vor dem Berufungsgericht 1944 ('de Havilland Law') setzte eine Grenze: Suspendierungszeiten durften nicht zur Vertragslaufzeit hinzugerechnet werden - was das System fuer die Studios deutlich unattraktiver machte."
    ),

    // Question 12 - Golden Age Hollywood: Contract Players
    Question(
        categoryId = 4,
        questionText = "Welches Studio war dafuer bekannt, unter Irving Thalberg das Konzept des 'Producer's System' zu perfektionieren, bei dem Produzenten - nicht Regisseure - die kreative Kontrolle hatten?",
        answerA = "Warner Bros.",
        answerB = "RKO Pictures",
        answerC = "Metro-Goldwyn-Mayer (MGM)",
        answerD = "Columbia Pictures",
        correctAnswer = 2,
        explanation = "MGM unter Irving Thalberg (Chefproduzent 1924-1933) war das Paradebeispiel des Producer's System: Der Produzent kontrollierte Drehbuch, Casting, Schnitt und das Endprodukt. Regisseure waren austauschbare Handwerker. Thalberg formulierte: 'Der Direktor ist das wichtigste Zahnrad in einer Maschine, die ich betreibe.'",
        difficulty = 3,
        funFact = "Irving Thalberg ist der Namensgeber des Irving G. Thalberg Memorial Award der Oscars, der seit 1937 an Produzenten fuer ihr Lebenswerk verliehen wird."
    ),

    // Question 13 - Film Editing: Kuleshov Effect
    Question(
        categoryId = 4,
        questionText = "Was demonstrierte Lev Kuleschow mit seinem beruehmt gewordenen Experiment, dem 'Kuleschow-Effekt'?",
        answerA = "Dass Farbfilme emotional wirkungsvoller sind als Schwarzweissfilme",
        answerB = "Dass die Bedeutung einer Einstellung durch die benachbarte Einstellung beeinflusst wird",
        answerC = "Dass Musik die Wahrnehmung von Schauspielerleistungen veraendert",
        answerD = "Dass Zeitlupe emotionale Reaktionen verstaerkt",
        correctAnswer = 1,
        explanation = "Kuleschow zeigte dasselbe neutrale Gesicht eines Schauspielers (Ivan Mosjoukine) nacheinander neben drei verschiedenen Bildern: einer Suppe, einem Sarg und einem spielenden Kind. Das Publikum schrieb dem Gesicht jeweils unterschiedliche Emotionen zu (Hunger, Trauer, Freude). Dies bewies, dass Bedeutung im Schnitt entsteht, nicht im Einzelbild.",
        difficulty = 3,
        funFact = "Der Kuleschow-Effekt wird auch heute in der Werbung systematisch eingesetzt - indem Produkte neben positiv belegten Bildern gezeigt werden, um positive Gefuehle auf das Produkt zu uebertragen."
    ),

    // Question 14 - Film Editing: Eisenstein Montage
    Question(
        categoryId = 4,
        questionText = "Welche Montageform beschreibt Sergei Eisenstein, bei der zwei inhaltlich widerspruechliche Einstellungen zusammengeschnitten werden, um eine neue Idee zu erzeugen?",
        answerA = "Metrische Montage",
        answerB = "Rhythmische Montage",
        answerC = "Dialektische Montage",
        answerD = "Oberton-Montage",
        correctAnswer = 2,
        explanation = "Eisensteins dialektische Montage (auch intellektuelle Montage) basiert auf dem Prinzip These + Antithese = Synthese: Zwei sich widersprechende Bilder erzeugen im Kopf des Zuschauers einen neuen, dritten Begriff oder eine Idee. Diese Methode unterschied sich grundlegend von der westlichen Continuity-Schnittpraxis.",
        difficulty = 3,
        funFact = "Eisenstein formulierte insgesamt fuenf Montagetypen: metrisch, rhythmisch, tonal, Oberton und intellektuell. Sein Essay 'The Cinematographic Principle and the Ideogram' (1929) ist bis heute Pflichtlektuere an Filmhochschulen."
    ),

    // Question 15 - Film Editing: Eisenstein Montage
    Question(
        categoryId = 4,
        questionText = "In welchem Film von Sergei Eisenstein befindet sich die beruehmt gewordene 'Odessa-Treppe'-Sequenz?",
        answerA = "Oktober",
        answerB = "Der Streik",
        answerC = "Panzerkreuzer Potemkin",
        answerD = "Alexander Newskij",
        correctAnswer = 2,
        explanation = "Die Odessa-Treppe-Sequenz aus 'Panzerkreuzer Potemkin' (1925) gilt als eine der einflussreichsten Schnittsequenzen der Filmgeschichte. Eisenstein dehnte die real wenige Minuten dauernde Schiesserei durch rhythmische Montage auf fast sieben Filmminuten aus und verstaerkte so die emotionale und politische Wirkung massiv.",
        difficulty = 3,
        funFact = "Die Treppe heisst im Volksmund 'Potemkin-Treppe', obwohl die historischen Ereignisse von 1905 dort gar nicht stattfanden - Eisensteins Film hat die kollektive Erinnerung neu geschrieben."
    ),

    // Question 16 - Film Editing: Walter Murch
    Question(
        categoryId = 4,
        questionText = "Welches von Walter Murch formulierte Kriterium beschreibt er als das wichtigste beim Filmschnitt?",
        answerA = "Technische Sauberkeit des Schnitts",
        answerB = "Einhaltung der 180-Grad-Regel",
        answerC = "Emotionaler Wahrheitsgehalt der Szene",
        answerD = "Kontinuitaet der Bewegungsrichtung",
        correctAnswer = 2,
        explanation = "In seinem Buch 'In the Blink of an Eye' formuliert Walter Murch eine Hierarchie von sechs Kriterien. An erster Stelle steht der 'emotionale Wahrheitsgehalt' einer Szene. Murch argumentiert, dass ein technisch 'falscher' Schnitt akzeptabel ist, wenn er emotional richtig wirkt - aber ein technisch 'richtiger' Schnitt versagt, wenn er die Emotion toetet.",
        difficulty = 3,
        funFact = "Walter Murch ist der einzige Editor, der jemals sowohl den Oscar fuer den besten Ton als auch den Oscar fuer den besten Schnitt fuer denselben Film gewann - fuer 'Der englische Patient' (1996)."
    ),

    // Question 17 - Film Editing: Walter Murch
    Question(
        categoryId = 4,
        questionText = "Fuer welchen Francis-Ford-Coppola-Film rettete Walter Murch 1979 durch genialen Schnitt einen nahezu gescheiterten Film?",
        answerA = "Der Pate",
        answerB = "Die Konversation",
        answerC = "Apocalypse Now",
        answerD = "Rumble Fish",
        correctAnswer = 2,
        explanation = "'Apocalypse Now' war nach dem chaotischen Dreh ein scheinbar unrettbares Rohschnitt-Konglomerat. Walter Murch gestaltete den finalen Schnitt massgeblich und schuf eine zusammenhaengende, epische Struktur aus hunderten Stunden Material. Er entwickelte dabei auch ein innovatives Mehrkanal-Sounddesign.",
        difficulty = 3,
        funFact = "Murch erfand fuer 'Apocalypse Now' den Begriff 'Sound Design' - bis dahin gab es keinen offiziellen Berufsbezeichnung fuer jemanden, der die gesamte Klangebene eines Films gestaltet."
    ),

    // Question 18 - Sound in Film: First Talkie
    Question(
        categoryId = 4,
        questionText = "Welches Synchrontonsystem verwendete 'The Jazz Singer' (1927), dem ersten kommerziell erfolgreichen Tonfilm Hollywoods?",
        answerA = "Movietone (Lichtton auf dem Film)",
        answerB = "Vitaphone (Ton auf Schellackplatte)",
        answerC = "RCA Photophone",
        answerD = "Western Electric Westrex",
        correctAnswer = 1,
        explanation = "Warner Bros. nutzte fuer 'The Jazz Singer' das Vitaphone-System: Der Ton wurde auf einer separaten Schellackplatte aufgezeichnet, die synchron mit dem Filmprojektor lief. Das System war fehleranfaellig, setzte sich aber kurzzeitig durch, bis das ueberlegene Lichttonverfahren (Ton direkt auf dem Film) Vitaphone verdraengte.",
        difficulty = 3,
        funFact = "Al Jolsons spontan gesprochener Satz 'You ain't heard nothin' yet' im Film wurde nicht geskriptet - er war ein spontaner Kommentar, der aber im Film blieb und zur Filmgeschichte wurde."
    ),

    // Question 19 - Sound in Film: First Talkie
    Question(
        categoryId = 4,
        questionText = "Welches Lichttonverfahren setzte sich Anfang der 1930er Jahre als Industriestandard durch und druckte den Ton als optische Spur direkt auf den Filmstreifen?",
        answerA = "Vitaphone",
        answerB = "Photophone",
        answerC = "Movietone",
        answerD = "Phonofilm",
        correctAnswer = 2,
        explanation = "Das Fox-Movietone-Verfahren (entwickelt von Theodore Case und Lee de Forest) druckte den Ton als optisch lesbare Schwingungsspur direkt auf den Filmstreifen. Dies machte die separate Schallplatte (Vitaphone) ueberflussig und war zuverlaessiger. Movietone setzte sich als globaler Standard durch und ist der Vorgaenger aller modernen Lichttonverfahren.",
        difficulty = 3,
        funFact = "Das erste Tonfilmdokument der Filmgeschichte ist nicht 'The Jazz Singer', sondern frueherer Kurzfilme - darunter ein Film von Benito Mussolini aus dem Jahr 1927, aufgezeichnet mit dem Movietone-System."
    ),

    // Question 20 - Sound in Film: Foley Artistry
    Question(
        categoryId = 4,
        questionText = "Nach wem ist die Foley-Kunst benannt, also das nachtragliche Aufzeichnen von Alltagsgeraeusche zu einem fertigen Film?",
        answerA = "Jack Foley",
        answerB = "John Foley",
        answerC = "James Foley",
        answerD = "Frank Foley",
        correctAnswer = 0,
        explanation = "Jack Foley (1891-1967) war ein Universal-Studios-Tontechniker, der die Technik des Geraeusche-Nachvertonens in einem spezialausgestatteten Tonstudio systematisierte. Er erzeugte Alltagsgeraeusche (Schritte, Knarren, Kleidungsrascheln) live zur Projektion des fertigen Films, was deutlich bessere Ergebnisse lieferte als das Aufzeichnen am Set.",
        difficulty = 3,
        funFact = "Jack Foley blieb sein ganzes Leben lang bei Universal Studios. Da er nie in den Abspann grosser Produktionen aufgenommen wurde, ist sein Name ausserhalb der Branche kaum bekannt - obwohl sein Handwerk in jedem Film zu hoeren ist."
    ),

    // Question 21 - Sound in Film: Foley Artistry
    Question(
        categoryId = 4,
        questionText = "Welches klassische Beispiel der Foley-Kunst zeigt, womit Pferdehufe-Geraeusche haeufig erzeugt werden?",
        answerA = "Echte Hufe auf Holzdielen",
        answerB = "Kokosnussschalen auf verschiedenen Untergrundes",
        answerC = "Plastikhufeisen auf Metallplatten",
        answerD = "Lederschuhe mit Metallkapppen auf Stein",
        correctAnswer = 1,
        explanation = "Das klopfen mit Kokosnussschalen ist eines der aeltesten und bekanntesten Foley-Tricks: Zwei hohle Haelften einer Kokosnuss werden auf Sand, Stein oder Holz geschlagen, um den Rhythmus und Klang galoppierender oder trabender Pferde nachzuahmen. Diese Technik wird seit den fruehsten Tagen des Tonfilms eingesetzt.",
        difficulty = 3,
        funFact = "Monty Python and the Holy Grail (1975) machte diesen Foley-Trick zur Filmkomoedie-Parodie: Die Ritter 'reiten' zu Fuss, waehrend Knappe Kokosnussschalen schlagen - ein direktes Augenzwinkern an die Foley-Praxis."
    ),

    // Question 22 - Sound in Film: Sound Design Evolution
    Question(
        categoryId = 4,
        questionText = "Welches Mehrkanal-Tonsystem einfuehrte Dolby Laboratories 1977 mit 'Star Wars' als erstem grossen Kinohit?",
        answerA = "Dolby Stereo (4-Kanal-Lichtton)",
        answerB = "Dolby Digital 5.1",
        answerC = "Dolby Atmos",
        answerD = "Dolby Pro Logic",
        correctAnswer = 0,
        explanation = "Dolby Stereo war ein 4-Kanal-System (Links, Mitte, Rechts, Mono-Surround), das als optischer Lichtton auf den Filmstreifen kodiert wurde. 'Star Wars' (1977) war der erste grosse Film, der dieses System nutzte und damit eine neue Aera des Kinoklangs einlaeute. Aus diesem System entwickelte sich spaeter Dolby Digital.",
        difficulty = 3,
        funFact = "Der Sound Designer Ben Burtt erschuf fuer 'Star Wars' komplett neuartige Geraeusche: Das Lichtschwert-Summen entstand aus der Kombination eines alten Filmprojektors und einem Fernseher-Feedback-Ton."
    ),

    // Question 23 - Sound in Film: Sound Design Evolution
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr und fuer welchen Film fuehrte Dolby Laboratories das Dolby Atmos-System ein, das dreidimensionales Objektaudio mit bis zu 128 Audioobjekten ermoeglicht?",
        answerA = "2010, 'Tron: Legacy'",
        answerB = "2012, 'Brave - Legende der Highlands'",
        answerC = "2014, 'Guardians of the Galaxy'",
        answerD = "2015, 'Mad Max: Fury Road'",
        correctAnswer = 1,
        explanation = "Dolby Atmos wurde 2012 mit Pixars 'Brave' (Merida - Legende der Highlands) eingefuehrt. Das System platziert Klangobjekte dreidimensional im Kinoraum - einschliesslich deckenmontierter Lautsprecher. Mit bis zu 128 individuellen Audioobjekten (statt kanalbasiertem Ton) ermoeglicht es eine beispiellose raeumliche Praezision.",
        difficulty = 3,
        funFact = "Heimkino-Atmos (ab 2014) emuliert die Deckenlautsprecher durch nach oben abstrahlende Lautsprecher, die den Ton an der Decke reflektieren - ein akustischer Trick, der erstaunlich gut funktioniert."
    ),

    // Question 24 - Color Film: Technicolor Process
    Question(
        categoryId = 4,
        questionText = "Wie viele Farbfilmstreifen wurden beim Drei-Streifen-Technicolor-Verfahren (ab 1932) gleichzeitig in der Kamera belichtet?",
        answerA = "Zwei",
        answerB = "Drei",
        answerC = "Vier",
        answerD = "Sechs",
        correctAnswer = 1,
        explanation = "Das Drei-Streifen-Technicolor-Verfahren (auch Technicolor Process 4) belichtete mit einem Prisma gleichzeitig drei separate Schwarzweissfilmstreifen hinter Rot-, Gruen- und Blaufiltern. Diese drei Streifen wurden auf dem Spezialdruck zusammengefuehrt und erzeugten aussergewoehnlich saettigte, leuchtende Farben.",
        difficulty = 3,
        funFact = "Die Technicolor-Kamera wog ueber 100 Kilogramm und war so gross, dass sie kaum handgehalten werden konnte. Ausserdem verlangte Technicolor, dass sogenannte 'Colour Consultants' am Set anwesend waren, was viele Regisseure als Einmischung empfanden."
    ),

    // Question 25 - Color Film: Technicolor Process
    Question(
        categoryId = 4,
        questionText = "Welcher Film von 1939 gilt als bekanntestes Beispiel des klassischen Drei-Streifen-Technicolors und wechselt dramaturgisch zwischen Schwarzweiss und Farbe?",
        answerA = "Schneewittchen und die sieben Zwerge",
        answerB = "Der Zauberer von Oz",
        answerC = "Vom Winde verweht",
        answerD = "Robin Hood",
        correctAnswer = 1,
        explanation = "'Der Zauberer von Oz' (1939) nutzt den Wechsel von Schwarzweiss (Kansas) zu leuchtendem Drei-Streifen-Technicolor (Oz) als dramaturgisches Mittel. Dieser Kontrast ist bis heute eine der bekanntesten Film-Kameratechniken. Der Film war eine der teuersten Produktionen seiner Zeit.",
        difficulty = 3,
        funFact = "Die leuchtend roten Rubyslippers aus 'Der Zauberer von Oz' waren urspruenglich in L. Frank Baums Buch silbern - sie wurden fuer den Technicolor-Film rot eingefaerbt, da Rot im neuen Farbsystem besonders dramatisch wirkte."
    ),

    // Question 26 - Color Film: Technicolor Process
    Question(
        categoryId = 4,
        questionText = "Wie hiess die Technicolor-Chefberaterin, die massgeblich die Farbpaletten zahlreicher Hollywoodfilme der 1930er und 40er Jahre pragte und steuerte?",
        answerA = "Mary Pickford",
        answerB = "Natalie Kalmus",
        answerC = "Lillian Gish",
        answerD = "Edith Head",
        correctAnswer = 1,
        explanation = "Natalie Kalmus war von 1933 bis 1949 als offizielle Colour Consultant bei fast allen Technicolor-Produktionen beteiligt. Als Ex-Frau von Technicolor-Mitgruender Herbert Kalmus hatte sie vertraglich das Recht, die Farbgestaltung aller Produktionen zu ueberwachen. Viele Regisseure empfanden ihren Einfluss als kuenstlerische Einschraenkung.",
        difficulty = 3,
        funFact = "Natalie Kalmus entwickelte die 'Kalmus-Theorie der Filmfarbe', die Farben psychologischen Wirkungen zuordnete: Rot fuer Leidenschaft und Gefahr, Blau fuer Ruhe und Traurigkeit usw."
    ),

    // Question 27 - Color Film: Eastmancolor
    Question(
        categoryId = 4,
        questionText = "Welcher wesentliche Vorteil machte Eastmancolor in den fruehen 1950er Jahren zur ernsthaften Konkurrenz fuer Technicolor?",
        answerA = "Hoehere Farbsaettigung und schoenere Hauttöne",
        answerB = "Einstufiges Negativ-Positiv-Verfahren mit normaler Kamera",
        answerC = "Deutlich groessere Bildstaerke fuer Kinoleinwaende",
        answerD = "Bessere Kopierstabilitaet bei Langzeitlagerung",
        correctAnswer = 1,
        explanation = "Eastmancolor (eingefuehrt 1950) verwendete einen einzigen mehrschichtigen Farbfilm, der in jeder normalen Kamera belichtet werden konnte - im Gegensatz zur schweren, teuren Drei-Streifen-Technicolor-Spezialkamera. Dies senkte die Produktionskosten drastisch und vereinfachte die Dreharbeiten erheblich.",
        difficulty = 3,
        funFact = "Eastmancolor hatte jedoch einen gravierenden Nachteil: Die Farbstoffe des fruehenEastmancolor verblassten mit der Zeit stark. Viele Filme aus den 1950er und 60er Jahren haben heute stark verfaerbte Kopien - ein Problem, das zur modernen Filmrestaurierung fuehlte."
    ),

    // Question 28 - Color Film: Eastmancolor
    Question(
        categoryId = 4,
        questionText = "Welcher oscargekroente Film von 1953 war einer der ersten grossen Hollywoodfilme, der Eastmancolor verwendete und gleichzeitig das neue CinemaScope-Breitwandformat einfuehrte?",
        answerA = "Shane",
        answerB = "From Here to Eternity",
        answerC = "The Robe - Das Gewand",
        answerD = "Roman Holiday",
        correctAnswer = 2,
        explanation = "'The Robe - Das Gewand' (1953) von 20th Century Fox war der erste in CinemaScope gedrehte Film UND einer der ersten grossen Studiotitel auf Eastmancolor-Basis. Der Film demonstrierte, dass das neue breitere Bildformat (2.39:1) und die guenstigere Einstoff-Farbfilmtechnologie die teure Technicolor-Spezialausruestung ersetzen konnten.",
        difficulty = 3,
        funFact = "Fuer 'The Robe' verwendete 20th Century Fox anamorphotische Objektive von Bausch & Lomb, die das Bild horizontal komprimierten und bei der Projektion wieder entzerrten - so entstand das charakteristische CinemaScope-Breitbild."
    ),

    // Question 29 - Aspect Ratio History
    Question(
        categoryId = 4,
        questionText = "Welches Seitenverhaeltnis hatte das urspruengliche 'Academy Ratio', das Thomas Edison und William Dickson 1892 fuer den 35mm-Film festlegten?",
        answerA = "4:3 (1.33:1)",
        answerB = "16:9 (1.78:1)",
        answerC = "1.85:1",
        answerD = "2.39:1",
        correctAnswer = 0,
        explanation = "Das urspruengliche Academy Ratio von 1.33:1 (4:3) wurde faktisch von Thomas Edison und William Dickson um 1892 fuer den 35mm-Film etabliert. Es entsprach in etwa dem goldenen Schnitt und wurde 1932 von der Academy of Motion Picture Arts and Sciences als offizieller Standard (1.37:1, 'Academy Ratio') bestaetigt.",
        difficulty = 3,
        funFact = "Das Fernsehen uebernahm das 4:3-Seitenverhaeltnis direkt vom Kino, was erklaert, warum alte Fernsehgeraete quadratisch wirkten. Die Einfuehrung von 16:9-Breitbild-TV sollte dem Kinoformat naherkommen."
    ),

    // Question 30 - Aspect Ratio History
    Question(
        categoryId = 4,
        questionText = "Was bewirkte die Einfuehrung von CinemaScope im Jahr 1953 im Bezug auf das Standard-Kinobildseitenverhaeltnis?",
        answerA = "Eine Verbreiterung von 1.33:1 auf 2.39:1",
        answerB = "Die Einfuehrung des 1.85:1-Standards als Kompromissformat",
        answerC = "Die Abschaffung von Schwarzbalken bei TV-Uebertragungen",
        answerD = "Die Normierung auf 70mm-Film als neuen Standard",
        correctAnswer = 0,
        explanation = "CinemaScope verwendete anamorphotische Objektive und dehnte das Bild auf ein Seitenverhaeltnis von 2.39:1 (gelegentlich 2.35:1 genannt). Damit war der Sprung vom quadratischen 1.33:1 Academy Ratio zum fast doppelt so breiten Breitwandbild eine der groessten Veraenderungen in der Filmgeschichte des Formats.",
        difficulty = 3,
        funFact = "Regisseur Fritz Lang aeusserte sich abschaetzig ueber das CinemaScope-Format: Es eigne sich nur fuer die Aufnahme von Schlangen und Beerdigugenszuegen, nicht fuer das menschliche Gesicht."
    ),

    // Question 31 - Aspect Ratio History
    Question(
        categoryId = 4,
        questionText = "Was ist der Unterschied zwischen 'anamorphotischer' und 'sphaarischer' Breitwandproduktion?",
        answerA = "Anamorphotisch verwendet spezielle Linsen zur Bildkompression; sphaarisch nutzt Kameramaskierung",
        answerB = "Anamorphotisch ist digitale Nachbearbeitung; sphaarisch ist analoge Kameratechnik",
        answerC = "Anamorphotisch erlaubt keine Postproduktionsveraenderungen; sphaarisch schon",
        answerD = "Es gibt keinen Unterschied - beide Begriffe sind Synonyme",
        correctAnswer = 0,
        explanation = "Anamorphotische Objektive komprimieren das Bild horizontal beim Filmen und werden bei der Projektion wieder entzerrt. Sphaarische (nicht-anamorphotische) Breitbild-Produktion filmt mit normalen Objektiven und maskiert oben und unten beim Dreh oder Kino-Druck. Anamorphotisch nutzt mehr des Filmmaterials und erzeugt charakteristische Bokeh-Ellipsen und Lens Flares.",
        difficulty = 3,
        funFact = "J.J. Abrams ist bekannt fuer seinen bewussten Einsatz von Lens Flares (Objektivreflexionen) in Spielfilmen wie 'Star Trek' (2009) - ein charakteristisches Merkmal anamorphotischer Optiken."
    ),

    // Question 32 - Aspect Ratio History
    Question(
        categoryId = 4,
        questionText = "Welches Format verwendete Stanley Kubricks '2001: Odyssee im Weltraum' (1968) und welche besondere Kameraeigenschaft hatte das dabei eingesetzte Objektiv?",
        answerA = "Todd-AO 70mm; extrem lange Brennweite",
        answerB = "Super Panavision 70; spezielles NASA-Ultraweitwinkelobjektiv von Zeiss",
        answerC = "CinemaScope 35mm; anamorphotische Zeiss-Linsen",
        answerD = "VistaVision 35mm; hohe Lichtstärke bei wenig Kunstlicht",
        correctAnswer = 1,
        explanation = "Kubrick verwendete Super Panavision 70 (65mm-Film, 70mm-Kopie) und liess sich von NASA und Carl Zeiss ein ultraseltenes 50mm f/0.7-Objektiv (urspruenglich fuer Weltraumaufnahmen entwickelt) leihen. Dieses extremst lichtstarke Objektiv ermoeglichte spaeter die Kerzenlichtzenen in 'Barry Lyndon' ohne kuenstliches Licht.",
        difficulty = 3,
        funFact = "Das Zeiss-50mm-f0.7-Objektiv ist das lichtstaerkste Serienobjektiv, das je fuer einen Spielfilm eingesetzt wurde. Kubrick kaufte nach dem NASA-Leihvertrag saemtliche verbleibenden Exemplare auf."
    ),

    // Question 33 - Silent Film Technique: Intertitles
    Question(
        categoryId = 4,
        questionText = "Wie nennt man die eingeschobenen Text-Tafeln im Stummfilm, die Dialoge oder Erlaeuterungen liefern?",
        answerA = "Intercuts",
        answerB = "Title Cards (Intertitel)",
        answerC = "Insert Shots",
        answerD = "Narration Frames",
        correctAnswer = 1,
        explanation = "Intertitel (englisch: intertitles oder title cards) sind Textkarten, die zwischen Filmaufnahmen eingeschnitten werden. Sie liefern Dialoge, Narration oder Hintergrundinformationen. Im klassischen Stummfilm machten sie bis zu einem Drittel der Laufzeit aus. F.W. Murnau und andere Autorenfilmer versuchten, Intertitel so weit wie moeglich zu reduzieren.",
        difficulty = 3,
        funFact = "Murnaus 'Der letzte Mann' (1924) gilt als einer der ersten Spielfilme, der fast vollstaendig ohne Intertitel auskommt - die Geschichte wird ausschliesslich durch Bildkomposition und Schauspiel erzaehlt."
    ),

    // Question 34 - Silent Film: Iris and Mask
    Question(
        categoryId = 4,
        questionText = "Welche Kameratechnik verwendeten Stummfilmregisseure, um die Aufmerksamkeit auf ein Detail zu lenken, indem alles ausser einem runden Bildausschnitt abdunkelt wurde?",
        answerA = "Vignette",
        answerB = "Iris Shot",
        answerC = "Matte Shot",
        answerD = "Split Screen",
        correctAnswer = 1,
        explanation = "Der Iris Shot (dt. Irisblende) nutzt eine kreisfoermige Maske vor dem Objektiv, die sich oeffnen oder schliessen laesst. Eine sich oeffnende Irisblende ('iris in') war die klassische Szenenoeffnung; eine sich schliessende ('iris out') markierte das Szenenende. D.W. Griffith setzte diese Technik intensiv zur Publikumslenkung ein.",
        difficulty = 3,
        funFact = "Der Iris Shot verschwand mit dem Tonfilm fast vollstaendig aus dem Mainstream-Kino. Wes Anderson (The Royal Tenenbaums, The Grand Budapest Hotel) nutzt ihn gelegentlich als bewusste stilistische Hommage an den Stummfilm."
    ),

    // Question 35 - Golden Age: Sound Transition Problems
    Question(
        categoryId = 4,
        questionText = "Welches technische Problem der fruehen Tonfilmzeit zwang die Regisseure dazu, die Kamera in schallisolierten Holzboxen einzusperren?",
        answerA = "Das Motorengeraeusch des Kameraantriebs wurde vom Mikrofon aufgezeichnet",
        answerB = "Das Licht der Bogenlampen erzeugte Brummtoene",
        answerC = "Die Kameralinsen erzeugten akustische Resonanzen",
        answerD = "Der Filmtransport knarrte durch thermische Ausdehnung",
        correctAnswer = 0,
        explanation = "Die fruehen Tonfilm-Kameras erzeugten lautes Motorengeraeusch, das die empfindlichen Mikrofone aufzeichneten. Da die Mikrofone nicht gut zwischen Kameramotor und Schauspielertoenen unterscheiden konnten, wurden die Kameras in schalldichte Holzboxen ('Blimps' oder 'Iceboxes') gesperrt. Dies machte die Kamera unbeweglicher und fuehrte zum statischen fruehen Tonfilm.",
        difficulty = 3,
        funFact = "Diese Einschraenkung erklaert den Qualitaetsabfall vieler fruehen Tonfilme 1928-1932: Die befangene Kamera, Schauspieler die ans Mikrofon gebunden waren und steife Inszenierung liessen viele fruehe Tonfilme schlechter wirken als ihre Stummfilm-Vorlaeufer."
    ),

    // Question 36 - Sound in Film: Dolby History
    Question(
        categoryId = 4,
        questionText = "Welches Rauschunterdrueckungsverfahren, entwickelt von Ray Dolby, wurde zuerst 1965 fuer professionelle Tonaufnahmen und spaeter fuer den Kinoton standardisiert?",
        answerA = "Dolby A",
        answerB = "Dolby B",
        answerC = "Dolby C",
        answerD = "Dolby HX Pro",
        correctAnswer = 0,
        explanation = "Dolby A war das erste professionelle Rauschunterdrueckungssystem (1965), entwickelt fuer Studioanwendungen. Es teilte das Signal in vier Frequenzbaender und unterdrueckte Rauschen in jedem Band separat. Dolby B (1968) war die vereinfachte Konsumentenversion fuer Kassettenrekorder, wurde aber auch als erster Kinolichtton-Rauschunterdrueckungsstandard eingesetzt.",
        difficulty = 3,
        funFact = "Ray Dolby gruendete Dolby Laboratories 1965 in London mit einem Kapital von einigen tausend Pfund. Das Unternehmen wurde zu einem der einflussreichsten Audiotechnologieunternehmen der Welt."
    ),

    // Question 37 - Film Editing: Continuity Editing
    Question(
        categoryId = 4,
        questionText = "Was versteht man unter der '180-Grad-Regel' im Continuity-Filmschnitt?",
        answerA = "Die Kamera muss bei jedem Schnitt um 180 Grad gedreht werden",
        answerB = "Eine imaginaere Achse zwischen zwei Figuren darf die Kamera nicht ueberschreiten",
        answerC = "Jede Einstellung muss aus 180 Grad Perspektive zur Figur aufgenommen werden",
        answerD = "Zwischen zwei Einstellungen muessen mindestens 180 Frames liegen",
        correctAnswer = 1,
        explanation = "Die 180-Grad-Regel besagt, dass alle Kamerapositionen einer Szene auf einer Seite einer imaginaeren Achse (Handlungsachse) bleiben muessen. Wird die Achse ueberquert ('Achsensprung'), verlieren die Figuren ihre Blick- und Bewegungsrichtung aus Zuschauerperspektive, was raumliche Desorientierung erzeugt.",
        difficulty = 3,
        funFact = "Regisseur Park Chan-wook bricht die 180-Grad-Regel in 'Oldboy' (2003) bewusst waehrend des beruehmt-beruechtigten Korridorkampfes, um die Desorientierung des Protagonisten visuell zu vermitteln."
    ),

    // Question 38 - Film Editing: Match Cut
    Question(
        categoryId = 4,
        questionText = "In welchem Film von Stanley Kubrick befindet sich einer der beruehmt gewordenen Match Cuts der Filmgeschichte, bei dem ein Knochen nahtlos in ein Raumschiff uebergeht?",
        answerA = "Dr. Seltsam",
        answerB = "2001: Odyssee im Weltraum",
        answerC = "Uhrwerk Orange",
        answerD = "Lolita",
        correctAnswer = 1,
        explanation = "In '2001: Odyssee im Weltraum' (1968) wirft ein Affe in der Dawnof-Man-Sequenz einen Knochen in die Luft - der Schnitt verbindet ihn nahtlos mit einem Raumschiff im Jahr 2001. Dieser Match Cut umspannt Millionen Jahre Menschheitsgeschichte in einer einzigen Schnittgeste und gilt als einer der kuhnsten Zeitspruenge der Filmgeschichte.",
        difficulty = 3,
        funFact = "Kubrick brauchte keine Erklaerung - dieser einzige Schnitt kommuniziert das gesamte Thema des Films: Werkzeug und Technologie als Wesen des Menschen, von der Steinzeit bis zur Raumfahrt."
    ),

    // Question 39 - Color Film: Early Color Experiments
    Question(
        categoryId = 4,
        questionText = "Welches war das erste kommerzielle Zweifarbverfahren von Technicolor (Process 1 und 2), das von den 1910ern bis fruehen 1930er Jahren eingesetzt wurde?",
        answerA = "Rot-Grun-Subtraktion",
        answerB = "Rot-Cyanblau-Praegung auf zwei Filmstreifen",
        answerC = "Gelb-Violett-Additionsverfahren",
        answerD = "Orthochromatisches Einfarbverfahren",
        correctAnswer = 1,
        explanation = "Technicolor Process 2 (1922) nutzte zwei Schwarzweissfilmstreifen, die durch ein Prisma gleichzeitig hinter einem Rot- und einem Gruenfilter belichtet wurden. Diese wurden dann auf die Rueckseiten zweier Filmkopien uebertragen (eine rot gefaerbt, eine cyanblau), die zusammengeklebt wurden. Das Ergebnis war ein Zweifarbbild mit stark begrenztem Farbspektrum.",
        difficulty = 3,
        funFact = "Das erste Technicolor-Feature war 'The Toll of the Sea' (1922), gedreht in diesem Zweifarbverfahren. Das leuchtende, aber farblich begrenzte Bild liess rotes und gruenliches Haar, aber kein reines Blau oder Gelb zu."
    ),

    // Question 40 - Silent Film: German Expressionism Techniques
    Question(
        categoryId = 4,
        questionText = "Welches visuelle Gestaltungsprinzip des deutschen expressionistischen Films wie 'Das Cabinet des Dr. Caligari' (1920) erzeugte Albtraum-Atmosphaere durch Buehnenbilder statt durch Kameratricks?",
        answerA = "Chiaroscuro-Beleuchtung mit echten Schatten",
        answerB = "Aufgemalte Schatten und verzerrte Perspektiven auf flachen Kulissen",
        answerC = "Untersicht-Kameraperspektive mit Weitwinkelobjektiv",
        answerD = "Mehrfachbelichtung zur Erzeugung von Traumbildern",
        correctAnswer = 1,
        explanation = "Das Cabinet des Dr. Caligari und aehnliche Expressionisten-Filme verwendeten absichtlich verzerrte, nicht-perspektivische Buehnenbild-Kulissen, auf denen Schatten direkt aufgemalt waren. Da Kunstlicht teuer war, malten die Designer Licht- und Schattenwirkungen direkt auf die Flachkulissen - was einen surrealen, albtraumhaften Effekt erzeugte.",
        difficulty = 3,
        funFact = "Dieser Ansatz war auch oekonomisch begruendet: Malerisch aufgebrachte Schatten erforderten keine teure Lichttechnik. Die Not des Nachkriegs-Deutschlands wurde so zum aesthetischen Merkmal einer ganzen Bewegung."
    ),

    // Question 41 - Sound Film: Multitrack Recording
    Question(
        categoryId = 4,
        questionText = "Welches Aufnahmeformat setzte sich ab den 1970er Jahren in Hollywood als Standard fuer die mehrkanalige Endtonmischung durch?",
        answerA = "16-Spur-Magnetband (2-Zoll)",
        answerB = "35mm-Lichtton-Mehrspur",
        answerC = "6-Kanal-70mm-Magnetton",
        answerD = "24-Spur-1-Zoll-Magnetband",
        correctAnswer = 2,
        explanation = "70mm-Premierenkopien wurden mit bis zu sechs separaten Magnetton-Spuren (aufgeklebt auf den Filmrand) ausgeliefert. Dieses Format erlaubte mehrere diskrete Surroundkanaele und galt als die qualitaetsmaessig hoechste Kino-Tonwiedergabe der analogen Aera. Produzenten von Grossprestigen wie 'Ben-Hur' (1959) und spaeter 'Apocalypse Now' (1979) nutzten es intensiv.",
        difficulty = 3,
        funFact = "Der wiederveroeffentlichte 70mm-Print von Quentin Tarantinos 'The Hateful Eight' (2015) wurde als 'Roadshow' mit echter 70mm-Projektion in ausgewaehlten Kinos gezeigt - als nostalge Hommage an die Kinoerfahrung der 1960er Jahre."
    ),

    // Question 42 - Film Editing: Jump Cut
    Question(
        categoryId = 4,
        questionText = "Welcher Regisseur machte den Jump Cut als bewusstes Stilmittel bekannt, indem er ihn 1960 in 'Ausser Atem' (A bout de souffle) einsetzte?",
        answerA = "Francois Truffaut",
        answerB = "Jean-Luc Godard",
        answerC = "Alain Resnais",
        answerD = "Louis Malle",
        correctAnswer = 1,
        explanation = "Jean-Luc Godard setzte in 'Ausser Atem' (1960) Jump Cuts als expressives Stilmittel ein. Ein Jump Cut ist ein Schnitt zwischen zwei sehr aehnlichen Einstellungen derselben Szene, der eine sprunghafte Zeitverkuerzung erzeugt. Godard nutzte ihn bewusst, um Konventionen zu brechen und die Materialhaftigkeit des Films zu zeigen.",
        difficulty = 3,
        funFact = "Godards Jump Cuts entstanden teilweise aus der Not: Szenen wurden eingekuerzt, um das Laufzeitlimit zu unterschreiten. Die 'zufaelligen' Kuerzungen wirkten so frisch und modern, dass Godard sie als Stilmittel beibehielt."
    ),

    // Question 43 - Film Editing: Montage Theory
    Question(
        categoryId = 4,
        questionText = "Dziga Vertovs Stummfilm-Manifest 'Kino-Glaz' (1924) stellte welches Konzept in den Mittelpunkt?",
        answerA = "Das Auge der Kamera als uebermenschliches Beobachtungsorgan",
        answerB = "Die Ueberlegenheit des Tonfilms gegenueber dem Stummfilm",
        answerC = "Die Notwendigkeit von Laien-Darstellern statt professionellen Schauspielern",
        answerD = "Die Ablehnung von Intertiteln zugunsten reiner Bilderzaehlung",
        correctAnswer = 0,
        explanation = "Dziga Vertovs 'Kino-Glaz' (Kinoauge) propagierte die Kamera als ein dem menschlichen Auge ueberlegenes Instrument: Sie kann einfrieren, verlangsamen, beschleunigen und gleichzeitig an verschiedenen Orten sein. Vertov sah den Film als dokumentarisches Instrument, das die Wirklichkeit authentischer abbilden kann als das menschliche Auge.",
        difficulty = 3,
        funFact = "Vertovs 'Der Mann mit der Kamera' (1929) gilt als Meisterwerk des Avantgardefilms und beeinflusste direkt die spaetere Dogme-95-Bewegung (Lars von Trier, Thomas Vinterberg) und die moderne Dokumentarfilm-Aesthetik."
    ),

    // Question 44 - Sound in Film: ADR
    Question(
        categoryId = 4,
        questionText = "Wie nennt man das Verfahren, bei dem Schauspieler ihre Dialoge nach dem Dreh in einem Tonstudio neu einsprechen und zur Lippenbewegung im fertigen Bild synchronisieren?",
        answerA = "Playback Dubbing",
        answerB = "Automated Dialogue Replacement (ADR)",
        answerC = "Post-Sync Recording",
        answerD = "Loop Recording",
        correctAnswer = 1,
        explanation = "ADR (Automated Dialogue Replacement), auch Looping genannt, ist das Nachsynchronisationsverfahren, bei dem Schauspieler ihre Dialoge im Tonstudio neu aufzeichnen. Die Aufnahmen werden synchron zur fertigen Bildmontage angepasst. ADR wird eingesetzt, wenn O-Ton-Aufnahmen durch Umgebungslaerm, Mikrofon-Positioning-Probleme oder Aenderungen im Skript unbrauchbar sind.",
        difficulty = 3,
        funFact = "Marlon Brandos beruehmt gemurmelter Tonfall in 'Der Pate' (1972) war zu einem erheblichen Teil auf ADR-Probleme zurueckzufuehren - viele seiner Originalton-Aufnahmen waren durch seine umgestopfte Wange und den Mummelstil technisch schwierig."
    ),

    // Question 45 - Aspect Ratio: Widescreen History
    Question(
        categoryId = 4,
        questionText = "Was war der Hauptgrund, warum Hollywood 1953 massiv auf Breitbildformate umstellte, und welches war das erste grosse Breitbild-System?",
        answerA = "Technologischer Fortschritt machte es moeglich; das erste System war VistaVision",
        answerB = "Konkurrenz zum Fernsehen; das erste System war CinemaScope",
        answerC = "Kuenstlerische Avantgarde-Bewegung; das erste System war Todd-AO",
        answerD = "Europaische Koproduktionen forderten es; das erste System war Superscope",
        correctAnswer = 1,
        explanation = "Das aufkommende Fernsehen war eine existenzielle Bedrohung fuer Hollywood: Die Kinozuschauerzahlen sanken drastisch. Breitbildformate wie CinemaScope boten ein Erlebnis, das das kleine Fernsehbild nicht replizieren konnte. 20th Century Fox fuehlte mit 'The Robe' (1953) das erste CinemaScope-Feature ein.",
        difficulty = 3,
        funFact = "Die gleiche Logik gilt heute: IMAX, 4DX und HDR sind die modernen Antworten auf Streaming. Die Kinobranche wiederholt ein 70 Jahre altes Muster: Wenn ein neues Heimmedium droht, macht das Kino das Erlebnis spektakulaerer."
    ),

    // Question 46 - Silent Film: Soviet Montage Schools
    Question(
        categoryId = 4,
        questionText = "Welche sowjetische Filmschule, geleitet von Lev Kuleschow, gilt als Wiege der modernen Montagetheorie?",
        answerA = "WGIK Moskau",
        answerB = "Kuleschow-Werkstatt am Staatlichen Filminstitut (GTK)",
        answerC = "Leningrader Filmschule",
        answerD = "FEKS-Gruppe Petrograd",
        correctAnswer = 1,
        explanation = "Lev Kuleschow leitete eine Werkstatt am Staatlichen Technischen Filminstitut (GTK, spaeter WGIK) in Moskau. Da kaum Rohfilm vorhanden war, analysierten und remontieren Kuleschow und seine Studenten (darunter Vsevolod Pudowkin) vorhandene westliche Filme. Diese erzwungene Theorie- und Remontagearbeit fuehrte zur Kuleschow-Schule und ihren Montageexperimenten.",
        difficulty = 3,
        funFact = "Die sowjetischen Filmpioniere Eisenstein, Pudowkin und Dziga Vertov vertraten dabei unterschiedliche Montage-Theorien: Eisenstein betonte den Konflikt von Einstellungen, Pudowkin ihre kausale Verkettung, Vertov das dokumentarische Auge."
    ),

    // Question 47 - Technicolor: Imbibition Process
    Question(
        categoryId = 4,
        questionText = "Wie nannte sich das Druckverfahren, mit dem Technicolor seine Farbkopien ab den 1920er Jahren herstellte und das dem Hochdruckverfahren der Druckindustrie aehnelt?",
        answerA = "Dye Transfer",
        answerB = "Chromogenic Development",
        answerC = "Silver Bleach",
        answerD = "Contact Printing",
        correctAnswer = 0,
        explanation = "Das Dye-Transfer-Verfahren (auch Imbibition-Druck genannt) verwendete Gelatine-Matrizen fuer die drei Grundfarben (Cyan, Magenta, Gelb), die nacheinander auf einen Empfaengerfilm gepresst wurden. Die Gelatine sog den Farbstoff auf wie ein Schwamm. Dieses Verfahren erzeugte die charakteristisch gesaettigten, stabilen Technicolor-Farben, die nicht verblassten.",
        difficulty = 3,
        funFact = "Das Dye-Transfer-Verfahren wurde 1974 von Technicolor eingestellt. Eastmancolor war billiger. In den 1990er Jahren wurde es kurz wiederbelebt - Kubrick verlangte es fuer 'Eyes Wide Shut', um die charakteristische Farbpraesenz zu erhalten."
    ),

    // Question 48 - Sound Film: Surround History
    Question(
        categoryId = 4,
        questionText = "Welcher Film von 1940 gilt als Pionier des mehrkanaligen Surround-Tons und wurde in einem speziell entwickelten 54-Lautsprecher-System in ausgewaehlten Kinos vorgefuehrt?",
        answerA = "Der Zauberer von Oz",
        answerB = "Fantasia",
        answerC = "Rebecca",
        answerD = "Pinocchio",
        correctAnswer = 1,
        explanation = "Disneys 'Fantasia' (1940) wurde in einem von Walt Disney und RCA entwickelten System namens 'Fantasound' abgespielt, das bis zu 54 Lautsprecher nutzte und erstmals Ton aus verschiedenen Richtungen im Kinoraum ertönen liess. Fantasound gilt als Vorlaefer aller modernen Surround-Sound-Systeme.",
        difficulty = 3,
        funFact = "Wegen der hohen Installationskosten des Fantasound-Systems konnte 'Fantasia' nur in wenigen Kinos im Original-Surround-Format gezeigt werden. Der Film war beim Erststart ein kommerzieller Misserfolg - er wurde erst mit Wiederauffuehrungen in den 1950er-70er Jahren zum Kultfilm."
    ),

    // Question 49 - Film Editing: Non-Linear Editing
    Question(
        categoryId = 4,
        questionText = "Welches Computersystem revolutionierte ab 1989 den Filmschnitt, indem es erstmals ermoeglichte, Szenen digital und nicht-linear am Computer zu bearbeiten?",
        answerA = "Adobe Premiere",
        answerB = "Avid Media Composer",
        answerC = "Final Cut Pro",
        answerD = "Lightworks",
        correctAnswer = 1,
        explanation = "Avid Technology's Media Composer (1989) war das erste Non-Linear Editing System (NLE), das im professionellen Filmbereich breite Akzeptanz fand. Es digitalisierte Filmmaterial und ermoeglichte es Editoren, beliebig Szenen zu verschieben, ohne physisch Filmstreifen zu schneiden. Dies ersetzte das jahrzehntealte Moviola- und KEM-Schneidetisch-System.",
        difficulty = 3,
        funFact = "Walter Murch war ein Fruehnutzer von Avid und schnitt 1996 'Der englische Patient' als einen der ersten Majorstudio-Spielfilme komplett mit einem NLE-System. Er beschrieb die Umgewoehnungsphase als aehnlich dem Wechsel von der Schreibmaschine zum Computer."
    ),

    // Question 50 - Silent Film: Iris Techniques and D.W. Griffith
    Question(
        categoryId = 4,
        questionText = "Welche Kameraeinstellungsgroesse, heute als 'Extreme Close-up' bekannt, wurde von D.W. Griffith als erster Regisseur systematisch fuer dramatische Wirkung eingesetzt, obwohl fruehe Kritiker sie fuer 'verstummelnd' hielten?",
        answerA = "Die Totale",
        answerB = "Das Grossbild (Gross Close-up)",
        answerC = "Die Amerikanische Einstellung",
        answerD = "Die Vogelperspektive",
        correctAnswer = 1,
        explanation = "Griffith etablierte das Grossbild (extreme Nahaufnahme) als dramatisches Mittel: Einzelne Koerperteile, Augen, Haende fuellten den ganzen Bildschirm. Fruehe Kritiker lehnten dies ab, da eine einzelne Hand oder ein abgeschnittenes Gesicht sie als 'koerperlich unvollstaendige' Darstellung befremdete. Griffith zeigte, dass solche Einstellungen emotionale Intensitaet erzeugen koennen.",
        difficulty = 3,
        funFact = "Die 'Amerikanische Einstellung' (Halbnah, Brust aufwaerts) hat ihren Namen tatsaechlich aus dem europaeischen Filmjargon des fruehem 20. Jahrhunderts - sie bezeichnete eine Einstellungsgroesse, die den Colt-Revolver am Guertel noch sichtbar liess."
    )

)
