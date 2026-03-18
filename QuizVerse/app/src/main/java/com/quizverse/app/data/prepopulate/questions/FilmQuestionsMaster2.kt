package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsMaster2(): List<Question> = listOf(

    // --- STUMMFILM-AERA ---

    // Question 1 - Lumiere-Patent
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr liessen Auguste und Louis Lumiere den Cinematographen patentieren?",
        answerA = "1894",
        answerB = "1895",
        answerC = "1896",
        answerD = "1893",
        correctAnswer = 1, // B
        explanation = "Die Brueder Lumiere liessen den Cinematographen am 13. Februar 1895 patentieren. Die erste oeffentliche Vorstellung gegen Eintrittsgeld fand am 28. Dezember 1895 im Grand Cafe in Paris statt — dieser Termin gilt als offizielles Geburtsdatum des Kinos.",
        difficulty = 5,
        funFact = "Der Cinematograph war gleichzeitig Kamera, Kopiergeraet und Projektor in einem Geraet. Thomas Edisons Kinetoskop (1894) zeigte Filme nur fuer einzelne Betrachter durch eine Guckloch-Oeffnung — die Lumiere-Erfindung war die erste echte Massenkino-Technologie."
    ),

    // Question 2 - Melies Filmlaenge
    Question(
        categoryId = 4,
        questionText = "Georges Melies' 'Le Voyage dans la Lune' (1902) war fuer seine Zeit ungewoehnlich lang. Wie viele Minuten dauert der Film in der Originalfassung?",
        answerA = "8 Minuten",
        answerB = "14 Minuten",
        answerC = "21 Minuten",
        answerD = "30 Minuten",
        correctAnswer = 1, // B
        explanation = "'Le Voyage dans la Lune' dauert in der Originalfassung bei 14 Frames pro Sekunde ca. 14 Minuten (manchmal auch mit 16 fps als 12 Minuten angegeben). Fuer 1902 war das aussergewoehnlich — die meisten Filme dauerten damals nur 1-3 Minuten. Melies schoepfte damit das Potenzial des Films als narratives Medium erstmals aus.",
        difficulty = 5,
        funFact = "Melies drehte insgesamt rund 500 Kurzfilme, bevor er 1913 bankrott ging. Er war seiner Zeit so weit voraus, dass seine Firma Star Film von amerikanischen Produzenten, die seine Erfindungen kopierten, ruiniert wurde. Er verkaufte seine Filmkopien schliesslich als Rohstoff fuer Schuhsohlen."
    ),

    // Question 3 - Griffith Birth of a Nation
    Question(
        categoryId = 4,
        questionText = "D.W. Griffiths 'The Birth of a Nation' (1915) war auch technisch revolutionaer. Welche Schnitttechnik entwickelte Griffith in diesem Film zur Perfektion, um Spannung zu erzeugen?",
        answerA = "Jump Cut",
        answerB = "Parallelmontage (Cross-Cutting)",
        answerC = "Match Cut",
        answerD = "Iris-Blende",
        correctAnswer = 1, // B
        explanation = "Die Parallelmontage (Cross-Cutting) — das abwechselnde Schneiden zwischen zwei zeitgleich ablaufenden Handlungstraengen — wurde von Griffith in 'The Birth of a Nation' zur Meisterschaft entwickelt. Das schnelle Wechseln zwischen der bedrohten Heldin und den heranreitenden Rettern erzeugte eine bis dahin unbekannte Spannung.",
        difficulty = 5,
        funFact = "'The Birth of a Nation' war der erste Film, der im Weissen Haus vorgefiehrt wurde — Praesident Woodrow Wilson soll gesagt haben, es sei 'wie Geschichte mit Blitzen schreiben'. Der Film glorifiziert den Ku-Klux-Klan und loeste massive Proteste der NAACP aus, fuehrte aber direkt zur Wiedergruendung des KKK."
    ),

    // Question 4 - Murnau Nosferatu
    Question(
        categoryId = 4,
        questionText = "F.W. Murnau drehte 'Nosferatu' (1922) ohne Genehmigung der Stoker-Erben. Was geschah daraufhin vor Gericht?",
        answerA = "Murnau wurde zu einer Geldstrafe verurteilt",
        answerB = "Das Gericht ordnete die Vernichtung aller Kopien an",
        answerC = "Die Prana-Film GmbH musste Schadensersatz zahlen",
        answerD = "Der Film wurde umbenannt und neu veroeffentlicht",
        correctAnswer = 1, // B
        explanation = "Florence Stoker, Witwe des Bram Stoker, gewann 1925 den Rechtsstreit gegen die Prana-Film GmbH. Das Gericht ordnete die Vernichtung aller Filmkopien an. Die Prana-Film war zu diesem Zeitpunkt bereits bankrott. Gluecklicherweise hatten sich bereits Kopien in andere Laender verbreitet, sodass der Film ueberlebte.",
        difficulty = 5,
        funFact = "Die Prana-Film GmbH wurde eigens fuer die Produktion von 'Nosferatu' gegruendet und produzierte nur diesen einen Film, bevor sie Bankrott anmeldete. Max Schreck, der den Grafen Orlok spielte, wurde so perfekt mit der Rolle identifiziert, dass Gerueichte entstanden, er sei selbst ein Vampir."
    ),

    // Question 5 - Battleship Potemkin Szene
    Question(
        categoryId = 4,
        questionText = "Sergei Eisensteins beruehrtmeste Sequenz in 'Panzerkreuzer Potemkin' (1925) — die Treppenszene — basiert auf welcher realen Begebenheit?",
        answerA = "Dem Massaker am Nevsky-Prospekt 1905",
        answerB = "Dem Massaker auf der Potemkin-Treppe in Odessa 1905",
        answerC = "Einer erfundenen Szene ohne historische Grundlage",
        answerD = "Dem Sturm auf die Bastille",
        correctAnswer = 2, // C
        explanation = "Die beruehrmte Treppenszene ist eine kuenstlerische Erfindung Eisensteins ohne direkte historische Grundlage. Waehrend des Aufstands von 1905 in Odessa gab es tatsaechlich Auseinandersetzungen, aber das spezifische Massaker auf der Treppe ist eine dramatische Verdichtung und Fiktion. Eisenstein nutzte es als symbolisches Bild zaristischer Repression.",
        difficulty = 5,
        funFact = "Die Treppe wurde nicht am realen Ort in Odessa gebaut — Eisenstein nutzte die echte Potemkin-Treppe (heute Primorsky-Treppe) als Drehort. Die Sequenz dauert etwa 6 Minuten und enthaelt ueber 150 Einzelschnitte. Alfred Hitchcock nannte sie 'die groesste Filmsequenz aller Zeiten'."
    ),

    // Question 6 - Metropolis Produktionskosten
    Question(
        categoryId = 4,
        questionText = "Fritz Langs 'Metropolis' (1927) ruinierte bei seiner Entstehung fast ein ganzes Filmstudio. Welches Studio produzierte den Film?",
        answerA = "UFA (Universum Film AG)",
        answerB = "Terra Film",
        answerC = "Decla-Bioscop",
        answerD = "Greenbaum Film",
        correctAnswer = 0, // A
        explanation = "Die UFA (Universum Film AG) produzierte 'Metropolis'. Die Produktionskosten von ca. 5,3 Millionen Reichsmark (damals astronomisch) fuehrten die UFA beinahe in den Bankrott. Medienmogul Alfred Hugenberg musste die UFA retten und uebernahm das Unternehmen — ein Schritt mit fatalen Folgen, da Hugenberg spaeter zum Unterstuetzer Hitlers wurde.",
        difficulty = 5,
        funFact = "Fuer 'Metropolis' wurden rund 36.000 Statisten und 1.100 kahlrasierte Maenner eingesetzt. Die weibliche Hauptdarstellerin Brigitte Helm spielte sowohl die tugendhafte Maria als auch die Roboter-Maria und war zum Zeitpunkt der Dreharbeiten erst 18 Jahre alt. Ihr beruehrmter Roboter-Tanz war eine Choreografie, die sie selbst entwickelte."
    ),

    // --- DEUTSCHER EXPRESSIONISMUS ---

    // Question 7 - Caligari Autoren
    Question(
        categoryId = 4,
        questionText = "Wer schrieb das Drehbuch zu 'Das Cabinet des Dr. Caligari' (1920)?",
        answerA = "Hans Janowitz und Carl Mayer",
        answerB = "Fritz Lang und Thea von Harbou",
        answerC = "F.W. Murnau und Carl Mayer",
        answerD = "Robert Wiene und Erich Pommer",
        correctAnswer = 0, // A
        explanation = "Das Drehbuch schrieben Hans Janowitz und Carl Mayer. Janowitz (tschechischer Autor) und Mayer (oesterreichischer Autor) arbeiteten gemeinsam daran. Beide hatten negative Erfahrungen mit dem deutschen Militaer im Ersten Weltkrieg gemacht, was die antiautoritaere Botschaft des Films erklaert. Regisseur war Robert Wiene.",
        difficulty = 5,
        funFact = "Janowitz behauptete spaeter, die Geschichte sei urspruenglich als politische Parabel ohne das 'Rahmenerzaehlung'-Ende geschrieben worden. Das Ende, das alles als Einbildung eines Irren darstellt, soll auf Wunsch des Produzenten Erich Pommer eingefuegt worden sein — was die antiautoritaere Botschaft stark abmilderte."
    ),

    // Question 8 - Expressionismus Merkmal
    Question(
        categoryId = 4,
        questionText = "Welches spezifische Set-Design-Merkmal war fuer den Deutschen Expressionismus im Film typisch und wurde in 'Caligari' erstmals konsequent eingesetzt?",
        answerA = "Realistische Strassenrekonstruktionen im Studio",
        answerB = "Schraege, verzerrte Winkel und bemalte Schatten statt echter Beleuchtung",
        answerC = "Verwendung echter Aussenlocations ohne Studiobauten",
        answerD = "Monochrome blaue Einfaerbung aller Szenen",
        correctAnswer = 1, // B
        explanation = "Das expressionistische Set-Design nutzte absichtlich schiefe Winkel, geneigte Waende und bemalte Schatten direkt auf die Kulissen statt echter Beleuchtungseffekte. In 'Caligari' wurden die Schatten auf die Leinwand gemalt, weil das Filmstudio keine ausreichende Beleuchtungsausruestung besass — die Not wurde zur praegendem Stilmittel.",
        difficulty = 5,
        funFact = "Der Kunstfilmer Siegfried Kracauer argumentierte in seinem Werk 'Von Caligari zu Hitler' (1947), der Deutsche Expressionismus habe die psychologische Disposition des deutschen Volkes gespiegelt, die spaeter zum Nationalsozialismus fuehrte. Diese These ist bis heute umstritten, aber akademisch einflussreich."
    ),

    // Question 9 - Murnau Der letzte Mann
    Question(
        categoryId = 4,
        questionText = "F.W. Murnaus Film 'Der letzte Mann' (1924) revolutionierte die Filmsprache durch eine spezifische Kameratechnik. Was war das Besondere daran?",
        answerA = "Erstmaliger Einsatz von Handkamera-Aufnahmen",
        answerB = "Die entfesselte Kamera — Murnau bewegte sie kontinuierlich und frei",
        answerC = "Erstmaliger Einsatz von Kranaufnahmen",
        answerD = "Vollstaendiger Verzicht auf Zwischentitel",
        correctAnswer = 3, // D
        explanation = "'Der letzte Mann' wurde vollstaendig ohne Zwischentitel gedreht — ein revolutionaerer Ansatz. Murnau erzaehlte die Geschichte ausschliesslich durch visuelle Mittel. Zugleich setzte er die 'entfesselte Kamera' ein, die er frei im Raum bewegte. Der Film gilt als Meilenstein der rein visuellen Filmsprache.",
        difficulty = 5,
        funFact = "Kameramann Karl Freund befestigte die Kamera an seinem Koerper, um Ego-Perspektive-Shots zu drehen — eine Vorlaeufer-Technik der modernen Steadicam. Fuer eine Traumsequenz des betrunkenen Tuerhueters wurde die Kamera an einem Fahrrad und einem Ballon befestigt, um Desorientierung darzustellen."
    ),

    // --- SOWJETISCHE MONTAGE-SCHULE ---

    // Question 10 - Kuleschow-Effekt
    Question(
        categoryId = 4,
        questionText = "Lew Kuleschow bewies mit seinem beruehrmten Experiment, dass der Zuschauer Emotionen in ein ausdrucksloses Gesicht hineinprojiziert, je nachdem was im naechsten Schnitt zu sehen ist. Welcher Schauspieler wurde fuer dieses Experiment verwendet?",
        answerA = "Sergei Eisenstein",
        answerB = "Ivan Mosjoukine",
        answerC = "Wsewolod Pudowkin",
        answerD = "Alexander Khanzhonkov",
        correctAnswer = 1, // B
        explanation = "Kuleschow verwendete Archivaufnahmen des zaristischen Stummfilmstars Ivan Mosjoukine (auch Ivan Mosyukhin). Er kombinierte denselben neutralen Gesichtsausdruck mit Bildern von Suppe, einem Sarg und einem Kind. Zuschauer interpretierten das gleiche Gesicht als Hunger, Trauer oder Freude — der Kuleschow-Effekt war bewiesen.",
        difficulty = 5,
        funFact = "Kuleschow fuehrte das Experiment ca. 1921 durch, aber es gibt keine erhaltene Filmkopie davon. Die Beschreibung stammt hauptsaechlich von Pudowkin, der als Student dabei war. Moderne Neuversuche des Experiments haben die Theorie bestaetigt, aber auch gezeigt, dass Zuschauer den Gesichtsausdruck teilweise unabhaengig vom Kontext interpretieren."
    ),

    // Question 11 - Eisenstein Montage-Theorie
    Question(
        categoryId = 4,
        questionText = "Eisenstein entwickelte eine Montage-Theorie mit mehreren Typen. Welchen Montage-Typ beschreibt er als 'Konflikt-Montage', bei der zwei Bilder zusammenstossen und eine neue Idee erzeugen?",
        answerA = "Metrische Montage",
        answerB = "Rhythmische Montage",
        answerC = "Dialektische Montage (Intellektuelle Montage)",
        answerD = "Tonale Montage",
        correctAnswer = 2, // C
        explanation = "Die Dialektische oder Intellektuelle Montage ist Eisensteins fortgeschrittenster Montage-Typ: Zwei kontrastierende Bilder werden aneinandergeschnitten und erzeugen beim Zuschauer eine abstrakte Idee, die in keinem der Einzelbilder vorhanden ist — aehnlich dem Hegelischen These-Antithese-Synthese-Prinzip. Sein bekanntestes Beispiel ist die 'Gott-und-Stier'-Sequenz in 'Oktober'.",
        difficulty = 5,
        funFact = "Eisenstein war auch Filmtheoretiker ersten Ranges. Er las Freud, Marx und japanische Schrift-Theorie und sah Montage als universelles semiotisches Prinzip. Sein unvollendeter Film 'Que Viva Mexico!' (1932) wurde ohne seine Zustimmung von Produzent Upton Sinclair zusammengeschnitten und als Rumpfwerk veroeffentlicht."
    ),

    // Question 12 - Dziga Vertov Kino-Auge
    Question(
        categoryId = 4,
        questionText = "Dziga Vertov entwickelte die Theorie des 'Kino-Auges' (Kino-Glaz). Was war sein zentrales Manifest gegen den Spielfilm?",
        answerA = "Spielfilme sollten in Farbe gedreht werden",
        answerB = "Das Kino-Auge solle die ungestellte Realitaet aufnehmen, Spielfilme seien Opiume des Volkes",
        answerC = "Schauspieler sollten durch Tiere ersetzt werden",
        answerD = "Alle Filme sollten ohne Schnitte gedreht werden",
        correctAnswer = 1, // B
        explanation = "Vertov wandte sich radikal gegen den Spielfilm und die narrative Tradition. Sein Kino-Auge-Konzept propagierte die versteckte Kamera, das Aufnehmen des wirklichen Lebens ohne Inszenierung. Er nannte Spielfilme 'Kinodrama' und sah sie als burgerliche Verblendung. Sein Meisterwerk 'Der Mann mit der Kamera' (1929) ist eine filmische Reflexion uber das Filmen selbst.",
        difficulty = 5,
        funFact = "'Der Mann mit der Kamera' (1929) enthalt keine Schauspieler, kein Drehbuch und keine Zwischentitel — Vertov lehnte alles Literarische im Film ab. Interessanterweise ist der Film heute als postmodernes Meisterwerk anerkannt, das Themen der Selbstbezuglichkeit behandelt, die erst Jahrzehnte spaeter im Avantgarde-Kino wieder aufgegriffen wurden."
    ),

    // Question 13 - Pudowkin vs Eisenstein
    Question(
        categoryId = 4,
        questionText = "Wsewolod Pudowkin und Eisenstein hatten unterschiedliche Montage-Konzepte. Was war Pudowkins grundlegender Unterschied zu Eisensteins 'Konflikt'-Theorie?",
        answerA = "Pudowkin lehnte Montage vollstaendig ab",
        answerB = "Pudowkin sah Montage als Verknuepfung (Linking), nicht als Konflikt — Schnitte als Ziegel, nicht als Kollision",
        answerC = "Pudowkin wollte nur Dokumentarfilme drehen",
        answerD = "Pudowkin entwickelte die Tonfilm-Montage, Eisenstein nur fuer Stummfilme",
        correctAnswer = 1, // B
        explanation = "Pudowkin beschrieb Montage als konstruktives Zusammenfuegen von Einzelteilen, die zusammen ein Ganzes ergeben — wie Ziegel, die ein Gebaeude formen. Eisenstein sah Montage als Kollision gegensaetzlicher Elemente, die etwas Neues erzeugen. Pudowkins Ansatz war emotionaler und narrativer orientiert, Eisensteins intellektueller und dialektischer.",
        difficulty = 5,
        funFact = "Pudowkins Film 'Mutter' (1926) gilt als Meisterwerk der Sowjetischen Montage-Schule und orientierte sich staerker an westlichen Erzhaehlkonventionen als Eisensteins Werke. Pudowkin schrieb auch einflussreiche theoretische Werke, darunter 'Film und Filmtechnik' (1929), das lange als Standardwerk galt."
    ),

    // --- TONFIM-REVOLUTION ---

    // Question 14 - Erster Tonfilm
    Question(
        categoryId = 4,
        questionText = "Welches technische System verwendete der erste populaere Tonfilm 'The Jazz Singer' (1927)?",
        answerA = "Movietone (Lichtton direkt auf dem Film)",
        answerB = "Vitaphone (synchronisierte Schallplatten)",
        answerC = "Phonofilm (Lee de Forest-Verfahren)",
        answerD = "Tri-Ergon (deutsches Lichtton-System)",
        correctAnswer = 1, // B
        explanation = "Warner Bros. nutzte das Vitaphone-System: Der Ton wurde auf grossen 16-Inch-Schallplatten aufgenommen, die synchron zum Film abgespielt wurden. Dieses System war technisch anfaellig (die Synchronisation konnte verrutschen), wurde aber bald durch das zuverlaessigere Lichtton-Verfahren (Ton direkt auf dem Film) abgeloest.",
        difficulty = 5,
        funFact = "Al Jolsons Linie 'Wait a minute, wait a minute, you ain't heard nothin' yet!' in 'The Jazz Singer' war technisch gesehen nicht der erste synchrone Ton im Film — Lee de Forest hatte bereits 1923 Kurztonfilme mit seinem Phonofilm-System gezeigt. Aber 'The Jazz Singer' war der erste abendfuellende Spielfilm, der Ton als dramatisches Mittel einsetzte."
    ),

    // Question 15 - Stummfilm-Stars und Ton
    Question(
        categoryId = 4,
        questionText = "Welche beruehrmte Stummfilm-Schauspielerin scheiterte beim Uebergang zum Tonfilm massgeblich wegen ihres schweren deutschen Akzents?",
        answerA = "Greta Garbo",
        answerB = "Pola Negri",
        answerC = "Lillian Gish",
        answerD = "Gloria Swanson",
        correctAnswer = 1, // B
        explanation = "Pola Negri, polnisch-deutsche Stummfilm-Diva, konnte ihren Akzent nicht ausreichend ablegen und verlor nach dem Tonfilm-Einfuehrung ihre Hollywood-Karriere. Greta Garbo hingegen schaffte den Uebergang erfolgreich — MGM vermarktete ihren ersten Tonfilm 'Anna Christie' (1930) mit dem Slogan 'Garbo speaks!'",
        difficulty = 5,
        funFact = "Der Uebergang zum Tonfilm war fuer viele Stummfilm-Stars katastrophal: Schlechte Stimmen, Akzente oder ungeeignetes Sprechorgan beendeten Karrieren. Buster Keaton verlor nicht an einem Akzent, sondern daran, dass MGM ihm kuenstlerische Kontrolle entzog. Charlie Chaplin weigerte sich bis 1940 konsequent, synchronen Dialog zu verwenden."
    ),

    // --- GOLDENES ZEITALTER HOLLYWOODS / STUDIO-SYSTEM ---

    // Question 16 - Studio System Big Five
    Question(
        categoryId = 4,
        questionText = "Das klassische Hollywood-Studio-System der 1930er-40er umfasste die 'Big Five'. Welches der folgenden Studios gehoerte NICHT zu den 'Big Five'?",
        answerA = "RKO Pictures",
        answerB = "Columbia Pictures",
        answerC = "Loew's/MGM",
        answerD = "Paramount Pictures",
        correctAnswer = 1, // B
        explanation = "Columbia Pictures gehoerte zu den 'Little Three' — zusammen mit Universal Pictures und United Artists. Die 'Big Five' waren: Paramount, MGM/Loew's, Warner Bros., 20th Century Fox und RKO. Der Unterschied lag im Kinobesitz: Die Big Five besassen eigene Kinoketten (vertikal integriert), die Little Three nicht.",
        difficulty = 5,
        funFact = "Das Studio-System endete durch das Paramount Decree von 1948 (US Supreme Court), das Studios zwang, ihre Kinoketten zu verkaufen. Damit war die vertikale Integration des Hollywoodsystems gebrochen — der Beginn des Niedergangs des klassischen Studio-Systems und die Oeffnung fuer unabhaengige Produzenten."
    ),

    // Question 17 - Block Booking
    Question(
        categoryId = 4,
        questionText = "Was verstand man im Hollywood-Studio-System unter 'Block Booking'?",
        answerA = "Das Eintragen von Filmen in einen Zeitplan-Block",
        answerB = "Kinobetreiber mussten ganze Pakete (inkl. Schlechter Filme) kaufen, um Blockbuster zu erhalten",
        answerC = "Das Reservieren ganzer Kino-Bloecke fuer Premieren",
        answerD = "Eine Vertragsklausel, die Schauspieler an ein Studio band",
        correctAnswer = 1, // B
        explanation = "Block Booking war eine Praxis, bei der Studios Kinobetreibern zwangen, Pakete von 10-50 Filmen auf einmal zu kaufen ('blind booking'), oft ohne die Moglichkeit, einzelne Titel abzulehnen. Wer einen begehrten Star-Film wollte, musste auch die schwachen B-Filme kaufen. Diese Praxis wurde durch das Paramount Decree von 1948 verboten.",
        difficulty = 5,
        funFact = "Block Booking und die damit verbundene Praxis des 'Blind Buying' (Kauf von Filmen ohne Voraufsicht) waren ein Hauptpunkt der kartellrechtlichen Klage gegen die Big Five. Das Paramount-Urteil von 1948 gilt als eines der wichtigsten wirtschaftsrechtlichen Urteile in der US-Filmgeschichte."
    ),

    // Question 18 - Louis B. Mayer
    Question(
        categoryId = 4,
        questionText = "Louis B. Mayer, Studiochef von MGM, schuf das 'Star System' der Goldenen Aera mit dem beruehrmten Motto 'More stars than there are in heaven'. Wie viele Jahre leitete er MGM als Studiochef?",
        answerA = "12 Jahre (1924-1936)",
        answerB = "27 Jahre (1924-1951)",
        answerC = "18 Jahre (1933-1951)",
        answerD = "35 Jahre (1916-1951)",
        correctAnswer = 1, // B
        explanation = "Louis B. Mayer leitete MGM von seiner Gruendung 1924 bis zu seiner Entlassung 1951 — 27 Jahre lang. In dieser Zeit war er zeitweise das bestbezahlte Vorstandsmitglied in den gesamten USA. Er formte das Star-System mit langfristigen Exklusivvertraegen, die Stars als 'Eigentum' des Studios behandelten.",
        difficulty = 5,
        funFact = "Mayers Methoden waren beruchtigt: Er kontrollierte das Privatleben seiner Stars, arrangierte Ehen fuer das Image und unterdruckte Skandale. Judy Garland wurde als Kind-Star unter Mayeers Regime mit Amphetaminen und Schlaftabletten versorgt, um ihre Produktivitaet zu steigern — mit fatalen Langzeitfolgen fuer ihre Gesundheit."
    ),

    // Question 19 - Hayes Code Einführung
    Question(
        categoryId = 4,
        questionText = "Der Hays Code (Motion Picture Production Code) wurde 1930 veroeffentlicht, aber erst ab welchem Jahr wurde seine Einhaltung streng durchgesetzt?",
        answerA = "1930",
        answerB = "1932",
        answerC = "1934",
        answerD = "1938",
        correctAnswer = 2, // C
        explanation = "Obwohl der Code 1930 formuliert wurde, blieb die Durchsetzung zaghaft. Erst 1934, nach massivem Druck der Katholischen Kirche (Legion of Decency) und unter der Leitung von Joseph Breen als neuem Direktor des Production Code Administration (PCA), wurde der Code streng durchgesetzt. Die Aera 1930-1934 mit seinen lockeren Regeln nennt man 'Pre-Code Hollywood'.",
        difficulty = 5,
        funFact = "Will Hays, nach dem der Code benannt ist, war republikanischer Politiker und diente als Postmaster General unter Praesident Harding. Er wurde 1922 als PR-Massnahme nach dem Roscoe 'Fatty' Arbuckle-Skandal von den Studios als Selbstzensur-Chef eingesetzt. Ironischerweise schrieb Hays den Code kaum selbst — der Jesuit Daniel A. Lord und der Filmkritiker Martin Quigley waren die eigentlichen Autoren."
    ),

    // --- HAYS CODE UND FILMZENSUR ---

    // Question 20 - Pre-Code Hollywood
    Question(
        categoryId = 4,
        questionText = "Welcher Film aus der Pre-Code-Aera zeigte eine verheiratete Frau, die einen Lover nimmt, und wurde nach 1934 jahrzehntelang nicht mehr aufgefuehrt?",
        answerA = "Morocco (1930)",
        answerB = "Red-Headed Woman (1932)",
        answerC = "Baby Face (1933)",
        answerD = "Ecstasy (1933)",
        correctAnswer = 2, // C
        explanation = "'Baby Face' (1933) mit Barbara Stanwyck zeigte eine Frau, die sich sexuell durch die Unternehmenshierarchie schlaeft — offen, ohne moralische Strafe. Nach 1934 wurde der Film nicht mehr veroeffentlicht. Eine zensierte Version existierte, aber die Originalfassung galt als verschollen und wurde erst 2004 in der Library of Congress wiederentdeckt.",
        difficulty = 5,
        funFact = "Die wiederentdeckte Originalfassung von 'Baby Face' enthielt Szenen, die Nietzsche explizit zitieren, um Stanywyks charakters Entschlossenheit zu rechtfertigen. Diese Nietzsche-Referenzen wurden in der zensierten Version entfernt. Der Film ist heute ein wichtiges Dokument der kurzen Freiheitsphase des fruehen Hollywood-Kinos."
    ),

    // Question 21 - Hays Code verbotene Inhalte
    Question(
        categoryId = 4,
        questionText = "Welches spezifische visuelle Element war laut Hays Code in Schlafzimmerszenen verboten, selbst bei verheirateten Paaren?",
        answerA = "Kussszenen laenger als 3 Sekunden",
        answerB = "Doppelbetten — Ehepaare mussten in Einzelbetten schlafen",
        answerC = "Nackte Schultern der Frau",
        answerD = "Sichtbare Unterkleidung",
        correctAnswer = 1, // B
        explanation = "Laut Hays Code mussten Ehepaare in Filmszenen in getrennten Einzelbetten schlafen — ein Doppelbett war verboten. Diese Regel galt auch dann, wenn eindeutig war, dass die Charaktere ein verheiratetetes Paar waren. Erst mit dem Niedergang des Code in den 1960ern wurden Doppelbetten in Schlafzimmerszenen zugelassen.",
        difficulty = 5,
        funFact = "Eine der bizarrsten Code-Regeln war, dass mindestens ein Fuss beider Partner den Boden beruehren musste, wenn sich ein Paar auf einem Bett befand. Regisseure entwickelten kreative Workarounds: In 'It Happened One Night' (1934) haengte Clark Gable eine Decke zwischen die Betten — die 'Wall of Jericho', die ein virales Kino-Meme der Aera wurde."
    ),

    // Question 22 - Production Code Ende
    Question(
        categoryId = 4,
        questionText = "Welcher konkrete Film loeste 1968 die offizielle Abschaffung des Hays Code und seine Ersetzung durch das MPAA-Altersfreigabe-System aus?",
        answerA = "Bonnie and Clyde (1967)",
        answerB = "The Graduate (1967)",
        answerC = "Blow-Up (1966)",
        answerD = "Midnight Cowboy (1969)",
        correctAnswer = 2, // C
        explanation = "Michelangelo Antonionis 'Blow-Up' (1966) zeigte Nacktheit und wurde ohne PCA-Seal von MGM veroeffentlicht — ein direkter Vertrauensbruch mit dem Code-System. MGMs Reaktion zeigte, dass grosse Studios bereit waren, den Code zu ignorieren. 1968 einfuehrte MPAA-Chef Jack Valenti das neue Altersfreigabe-System G/M/R/X.",
        difficulty = 5,
        funFact = "Das erste Film, das die X-Bewertung erhielt und damit als 'nur fuer Erwachsene' galt, war ironischerweise 'Midnight Cowboy' (1969), der spaeter den Oscar fuer den Besten Film gewann — als einziger X-bewerteter Film in der Oscar-Geschichte. Heute wuerde er hoechstens eine R-Bewertung erhalten."
    ),

    // Question 23 - Auslaendische Filmzensur
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr einfuehrte Deutschland die Reichsfilmkammer, die juedischen Filmschaffenden das Arbeiten im deutschen Kino verbot?",
        answerA = "1930",
        answerB = "1933",
        answerC = "1935",
        answerD = "1938",
        correctAnswer = 1, // B
        explanation = "Die Reichsfilmkammer wurde 1933 nach der Machtergreifung der Nationalsozialisten eingerichtet. Nur Mitglieder durften im deutschen Filmgewerbe arbeiten, und juedische Filmschaffende wurden systematisch ausgeschlossen. Dies loeste eine Emigrationswelle aus: Fritz Lang, Billy Wilder, Peter Lorre, Ernst Lubitsch und viele andere gingen in die USA und bereicherten Hollywood.",
        difficulty = 5,
        funFact = "Joseph Goebbels, Reichsminister fuer Volksaufklaerung und Propaganda, war gleichzeitig faszinier von und abgestossen durch Sergei Eisenstein. Er bewunderte 'Panzerkreuzer Potemkin' als Meisterwerk der Propaganda, liess aber gleichzeitig alle sowjetischen Filme in Deutschland verbieten."
    ),

    // --- FILM NOIR ---

    // Question 24 - Film Noir Begriff
    Question(
        categoryId = 4,
        questionText = "Der Begriff 'Film Noir' wurde erstmals von welchem franzoesischen Filmkritiker fuer Hollywood-Kriminalfilme der 1940er gepraegt?",
        answerA = "Andre Bazin",
        answerB = "Nino Frank",
        answerC = "Francois Truffaut",
        answerD = "Henri Langlois",
        correctAnswer = 1, // B
        explanation = "Der franzoesische Filmkritiker Nino Frank praegte 1946 den Begriff 'Film Noir' in einem Artikel ueber vier amerikanische Kriminalfilme, darunter 'The Maltese Falcon' und 'Murder, My Sweet'. Gleichzeitig publizierte Jean-Pierre Chartier einen aehnlichen Artikel. Die Amerikaner selbst nannten diese Filme nicht so — der Begriff war ein franzoesisches kritisches Konzept.",
        difficulty = 5,
        funFact = "Hollywood-Filmemacher selbst erkannten Film Noir lange nicht als Genre. Der Begriff wurde erst in den 1960ern von amerikanischen Filmwissenschaftlern uebernommen, als das klassische Noir bereits vorbei war. Viele Regisseure der klassischen Noir-Aera waren deutsch-oesterreichische Emigranten wie Fritz Lang, Otto Preminger und Billy Wilder, die den deutschen Expressionismus mitbrachten."
    ),

    // Question 25 - Noir Stilmerkmal Chiaroscuro
    Question(
        categoryId = 4,
        questionText = "Welcher Kameramann gilt als einer der wichtigsten Pioniere des Film-Noir-Lichtstils und drehte u.a. 'Double Indemnity' (1944)?",
        answerA = "Gregg Toland",
        answerB = "John Alton",
        answerC = "Joseph LaShelle",
        answerD = "John F. Seitz",
        correctAnswer = 3, // D
        explanation = "John F. Seitz war der Kameramann von Billy Wilders 'Double Indemnity' (1944), einem definierenden Film-Noir-Werk. Sein extremes Chiaroscuro — tiefe Schatten, harte Lichtkontraste, schraege Kamerawinkel — definierte den visuellen Stil des Genres. John Alton war ein weiterer wichtiger Noir-Kameramann ('T-Men', 'He Walked by Night').",
        difficulty = 5,
        funFact = "John Alton schrieb 1949 das Buch 'Painting with Light', das erste systematische Werk ueber Film-Kameraarbeit. Er beschrieb darin, wie er mit minimaler Beleuchtungsausruestung maximale Wirkung erzielte. Sein Motto war: 'Was man nicht sieht, ist genauso wichtig wie was man sieht' — ein Prinzip, das den Film Noir visual definiert."
    ),

    // Question 26 - Femme Fatale Ursprung
    Question(
        categoryId = 4,
        questionText = "Die 'Femme Fatale' des Film Noir hatte literarische Vorbilder. Welches Buch von James M. Cain, das 1934 erschien, wurde zu einem Urtext des Noir-Genres und zweimal verfilmt?",
        answerA = "Red Harvest",
        answerB = "The Postman Always Rings Twice",
        answerC = "Farewell, My Lovely",
        answerD = "In a Lonely Place",
        correctAnswer = 1, // B
        explanation = "'The Postman Always Rings Twice' (1934) von James M. Cain war ein Ur-Noir-Roman: verhaengnisvolle Frau, schwacher Mann, Mord, Schuldgefuehle. Er wurde 1946 mit Lana Turner und John Garfield und 1981 mit Jack Nicholson und Jessica Lange verfilmt. Cains 'Double Indemnity' (1936) wurde zur Vorlage fuer Wilders gleichnamigen Film.",
        difficulty = 5,
        funFact = "Die MGM-Verfilmung von 1946 galt als sehr gewagt: Censor Joseph Breen versuchte, die Produktion zu verhindern, aber MGM liess sich nicht abschrecken. Die Verfilmung war trotz Zensureinschraenkungen ein grosser Kassenerfolg und demonstrierte, dass dunklere Stoffe Publikum anzogen."
    ),

    // Question 27 - Noir Endphase
    Question(
        categoryId = 4,
        questionText = "Welcher Film gilt oft als letzter grosser klassischer Film Noir und beendete damit die 'klassische Noir-Periode' (1941-1958)?",
        answerA = "Sweet Smell of Success (1957)",
        answerB = "Touch of Evil (1958)",
        answerC = "The Big Sleep (1946)",
        answerD = "Vertigo (1958)",
        correctAnswer = 1, // B
        explanation = "Orson Welles' 'Touch of Evil' (1958) gilt weitgehend als Ende der klassischen Noir-Periode. Der Film war zugleich ein Meisterwerk und eine Parodie des Genres — Welles kannte die Noir-Konventionen so gut, dass er sie uebertrieb. Die beruehrmte 3,5-Minuten-Eingangseinstellung ohne Schnitt ist eine der technisch beeindruckendsten Sequenzen des amerikanischen Kinos.",
        difficulty = 5,
        funFact = "Welles' 'Touch of Evil' wurde von Universal stark umgeschnitten und anders veroeffentlicht als von Welles geplant. 1998 wurde nach Welles' Memo-Anweisungen eine restaurierte Version hergestellt, die seinem urspruenglichen Schnitt naher kommt. Janet Leigh drehte kurz nach diesem Film 'Psycho' (1960) — ein weiterer Schluesselpunkt im Uebergang vom Noir zum modernen Thriller."
    ),

    // --- NEW HOLLYWOOD ---

    // Question 28 - Easy Rider Bedeutung
    Question(
        categoryId = 4,
        questionText = "Dennis Hoppers 'Easy Rider' (1969) gilt als Beginn des New Hollywood. Was war das Budget des Films, und wie viel spielte er ein?",
        answerA = "Budget $400.000, Einnahmen $60 Millionen",
        answerB = "Budget $600.000, Einnahmen $40 Millionen",
        answerC = "Budget $1 Million, Einnahmen $20 Millionen",
        answerD = "Budget $200.000, Einnahmen $10 Millionen",
        correctAnswer = 1, // B
        explanation = "Easy Rider hatte ein Budget von ca. $360.000-$600.000 (je nach Quelle) und spielte weltweit ca. $40 Millionen ein — eine astronomische Rendite. Dieser Erfolg bewies den grossen Studios, dass guenstig produzierte, kuenstlerisch ambitionierte Filme fuer junges Publikum profitabel waren, und loeste die New Hollywood-Revolution aus.",
        difficulty = 5,
        funFact = "Fuer den Soundtrack verwendete Hopper bestehende Rockmusik (Steppenwolf, The Byrds, Jimi Hendrix) statt eines teuren Original-Scores — eine radikale Abweichung von Hollywood-Konventionen. Der Film war so improvisiert, dass Kameramann Laszlo Kovacs sagte: 'Wir wussten nie, was wir am naechsten Tag drehen wuerden.'"
    ),

    // Question 29 - Coppola Godfather Casting
    Question(
        categoryId = 4,
        questionText = "Francis Ford Coppola kaeampfte fuer das Casting von Marlon Brando als Vito Corleone in 'Der Pate' (1972). Wer war Paramount Pictures' bevorzugter Kandidat fuer die Rolle?",
        answerA = "Frank Sinatra",
        answerB = "Ernest Borgnine",
        answerC = "Orson Welles",
        answerD = "Burt Lancaster",
        correctAnswer = 3, // D
        explanation = "Paramount favorisierte Burt Lancaster fuer Don Corleone. Frank Sinatra war ebenfalls im Gesprach (er soll wuetend gewesen sein, dass Mario Puzo ihn nicht direkt fragte). Coppola kaempfte hartnackig fuer Brando und liess ihn eine Probe filmen, in der Brando seine Wangen mit Seidenpapier auffuellte — eine improvisation, die seinen Charakter sofort definierte.",
        difficulty = 5,
        funFact = "Marlon Brando gewann fuer 'Der Pate' den Oscar, lehnte ihn aber ab — ein indigenes Stammesmitglied nahm stattdessen die Buehne und sprach ueber Hollywoods Behandlung amerikanischer Ureinwohner. Es war der politisch aufgeladenste Oscar-Moment der 1970er-Dekade."
    ),

    // Question 30 - Apocalypse Now Produktion
    Question(
        categoryId = 4,
        questionText = "Bei den Dreharbeiten zu 'Apocalypse Now' (1979) ereignete sich ein aussergewoehnlicher Notfall. Was geschah mit dem urspruenglichen Hauptdarsteller Harvey Keitel?",
        answerA = "Keitel zog sich eine schwere Verletzung zu",
        answerB = "Coppola entliess Keitel nach wenigen Wochen und ersetzte ihn durch Martin Sheen",
        answerC = "Keitel starb waehrend der Dreharbeiten",
        answerD = "Keitel verweigerte den Dreh und wurde durch Dennis Hopper ersetzt",
        correctAnswer = 1, // B
        explanation = "Harvey Keitel wurde nach wenigen Wochen Dreharbeiten von Coppola entlassen und durch Martin Sheen ersetzt. Coppola fand Keitels Interpretation nicht passend. Sheen erlitt waehrend der extrem schwierigen Dreharbeiten einen Herzinfarkt mit 36 Jahren, erholte sich aber und beendete den Film.",
        difficulty = 5,
        funFact = "Die Dreharbeiten zu 'Apocalypse Now' dauerten 238 Tage statt geplanter 16 Wochen. Coppolas Frau Eleanor dokumentierte den Dreh im Dokumentarfilm 'Hearts of Darkness: A Filmmaker's Apocalypse' (1991). Coppola investierte sein privates Vermoegen in den Film und riskierte den finanziellen Ruin."
    ),

    // Question 31 - Scorsese Taxi Driver
    Question(
        categoryId = 4,
        questionText = "Wer schrieb das Drehbuch zu Martin Scorseses 'Taxi Driver' (1976), und in welchem Zeitraum entstand es?",
        answerA = "Martin Scorsese, ueber 5 Jahre",
        answerB = "Paul Schrader, in 10 Tagen",
        answerC = "Robert De Niro und Scorsese gemeinsam, in 3 Monaten",
        answerD = "Brian De Palma, nach einem Roman",
        correctAnswer = 1, // B
        explanation = "Paul Schrader schrieb das Drehbuch in ca. 10 Tagen, als er selbst in einer tiefen persoenlichen Krise steckte — obdachlos, depressiv, Alkohol-abhaengig. Er sagte spaeter, Travis Bickle sei ein Selbstportrait seiner damaligen Verfassung. Das Drehbuch lag jahrelang in der Schublade, bis Scorsese es umsetzte.",
        difficulty = 5,
        funFact = "Die kontroverse 'You talkin' to me?'-Szene von Robert De Niro wurde vollstaendig improvisiert — im Drehbuch stand nur die Regieanweisung, dass Travis vor dem Spiegel probt. Diese Improvisation wurde zu einem der meistzitierten Filmmomente der Geschichte und demonstriert, wie New-Hollywood-Regisseure ihren Schauspielern Freiheit liessen."
    ),

    // Question 32 - Lucas THX 1138
    Question(
        categoryId = 4,
        questionText = "George Lucas' Debut-Spielfilm war nicht 'Star Wars', sondern ein dystopischer Science-Fiction-Film. Wie hiess er und in welchem Jahr wurde er veroeffentlicht?",
        answerA = "American Graffiti (1973)",
        answerB = "THX 1138 (1971)",
        answerC = "Apocalypse Now (1979)",
        answerD = "Grease (1978)",
        correctAnswer = 1, // B
        explanation = "'THX 1138' (1971) war George Lucas' erster Spielfilm, basierend auf seinem Studentenfilm 'Electronic Labyrinth: THX 1138 4EB' von 1967. Der dystopische Film mit Robert Duvall war ein kommerzieller Flop, wurde aber kuenstlerisch anerkannt. Produzent war Francis Ford Coppola ueber seine Firma American Zoetrope.",
        difficulty = 5,
        funFact = "Warner Bros. liess 'THX 1138' ohne Lucas' Erlaubnis umschneiden — ein Trauma, das Lucas dazu brachte, bei zukuenftigen Projekten maximale kreative Kontrolle zu behalten. Die THX-Tonsystem-Marke, die er spaeter entwickelte, ist eine direkte Reverenz an diesen ersten Film."
    ),

    // Question 33 - New Hollywood Ende
    Question(
        categoryId = 4,
        questionText = "Welcher Film gilt als das kommerziell-symbolische Ende des New Hollywood und der Beginn der Blockbuster-Aera des modernen Hollywood?",
        answerA = "Jaws (1975)",
        answerB = "Star Wars (1977)",
        answerC = "Apocalypse Now (1979)",
        answerD = "Heaven's Gate (1980)",
        correctAnswer = 0, // A
        explanation = "Spielbergs 'Jaws' (1975) gilt als erster moderner Blockbuster: massives Marketing, landesweiter simultaner Start in Hunderten von Kinos, Fokus auf Unterhaltung statt kuenstlerischer Vision. Er etablierte das 'event movie'-Konzept. Manche Filmhistoriker nehmen auch 'Star Wars' (1977) oder 'Heaven's Gate'-Flop (1980) als Endpunkt des New Hollywood.",
        difficulty = 5,
        funFact = "'Jaws' war der erste Film, der $100 Millionen an den Kinokassen einspielte. Der mechanische Hai ('Bruce', intern so genannt nach Spielbergs Anwalt) funktionierte kaum — was Spielberg zwang, weniger zu zeigen und mehr Spannung durch Andeutung zu erzeugen. Das Budget verdoppelte sich von $3,5 auf $9 Millionen."
    ),

    // --- BLAXPLOITATION ---

    // Question 34 - Shaft Regisseur
    Question(
        categoryId = 4,
        questionText = "Wer fuehrte Regie bei 'Shaft' (1971), dem ikonischen Blaxploitation-Film, und welchen Oscar gewann dieser Film?",
        answerA = "Gordon Parks, Oscar fuer den Besten Film",
        answerB = "Gordon Parks Sr., Oscar fuer die Beste Filmmusik",
        answerC = "Melvin Van Peebles, Oscar fuer die Beste Filmmusik",
        answerD = "Ossie Davis, kein Oscar",
        correctAnswer = 1, // B
        explanation = "Gordon Parks Sr. fuehrte Regie bei 'Shaft'. Der Film gewann den Oscar fuer die Beste Filmmusik (Isaac Hayes) — Hayes war der erste schwarze Komponist, der einen Oscar gewann. Das Titelthema 'Theme from Shaft' wurde ein kulturelles Phaenomen und Symbol des Blaxploitation-Genres.",
        difficulty = 5,
        funFact = "MGM stand kurz vor dem Bankrott und produzierte 'Shaft' als Notfallmassnahme. Der Film kostete $1,2 Millionen und spielte $13 Millionen ein — er rettete MGM. Gordon Parks war auch ein renommierter Fotograf fuer Life Magazine und der erste schwarze Regisseur bei einem grossen Hollywood-Studio."
    ),

    // Question 35 - Sweet Sweetback
    Question(
        categoryId = 4,
        questionText = "Welcher Film von 1971 gilt als eigentlicher Starter des Blaxploitation-Genres und wurde vollstaendig unabhaengig ohne Hollywood-Studio produziert?",
        answerA = "Shaft (1971)",
        answerB = "Superfly (1972)",
        answerC = "Sweet Sweetback's Baadasssss Song (1971)",
        answerD = "Coffy (1973)",
        correctAnswer = 2, // C
        explanation = "Melvin Van Peebles' 'Sweet Sweetback's Baadasssss Song' (1971) wird oft als eigentlicher Urtext des Blaxploitation-Genres bezeichnet. Van Peebles produzierte, schrieb, fuehrte Regie, spielte die Hauptrolle und komponierte den Score selbst — ohne Studio. Er uebertrug sogar einen Gonorrhoe-Fall, den er sich am Set zuzog, als 'Berufsunfall' an die Directors Guild, um Versicherung zu erhalten.",
        difficulty = 5,
        funFact = "Van Peebles lieh sich $150.000 von Bill Cosby und erhielt einen SBA-Kredit, indem er behauptete, einen Pornofilm zu drehen. Der Film kostete insgesamt ca. $150.000 und spielte $15 Millionen ein. Mario Van Peebles verfilmte die Entstehungsgeschichte seines Vaters in 'Baadasssss!' (2003)."
    ),

    // Question 36 - Blaxploitation Ende
    Question(
        categoryId = 4,
        questionText = "Welche Organisation kritisierte das Blaxploitation-Genre massiv wegen negativer Stereotypen und gruendete 1972 eine Kampagne dagegen?",
        answerA = "NAACP und die schwarze Gemeinschaft in einer Koalition (CORE, Urban League u.a.)",
        answerB = "Die US-Zensurbehoerde MPAA",
        answerC = "Die Demokratische Partei",
        answerD = "Das Congressional Black Caucus",
        correctAnswer = 0, // A
        explanation = "Eine Koalition schwarzer Buergerrechtsorganisationen — NAACP, CORE (Congress of Racial Equality), Urban League u.a. — gruendete 1972 die Koalition gegen Blaxploitation (CAB). Sie kritisierten, die Filme stereotypisierten schwarze Menschen als Kriminelle, Drogenhändler und Prostituierte und dienten weiss-kontrollierten Studios zur Bereicherung.",
        difficulty = 5,
        funFact = "Das Genre endete auch, weil sich die wirtschaftlichen Bedingungen aenderten: 'Star Wars' (1977) und andere Blockbuster zogen das schwarze Publikum ab, das zuvor Blaxploitation-Filme unterstuetzt hatte. Quentin Tarantinos 'Jackie Brown' (1997) ist eine explizite Hommage an das Genre, besonders an die Schauspielerin Pam Grier."
    ),

    // --- UNABHAENGIGES KINO DER 90ER / SUNDANCE ---

    // Question 37 - Sundance Gruendung
    Question(
        categoryId = 4,
        questionText = "Das Sundance Film Festival heisst nach der Rolle benannt, die Robert Redford im Western 'Butch Cassidy and the Sundance Kid' (1969) spielte. In welchem Jahr uebernahn Redford das Festival und gab ihm seinen heutigen Namen?",
        answerA = "1978",
        answerB = "1981",
        answerC = "1984",
        answerD = "1991",
        correctAnswer = 2, // C
        explanation = "Das Utah/US Film Festival wurde 1978 gegruendet. Robert Redford wurde 1981 in den Vorstand berufen und brachte es schrittweise unter die Kontrolle seines Sundance Institute. 1984 wurde das Festival offiziell in 'Sundance Film Festival' umbenannt, nach Redfords Charakter.",
        difficulty = 5,
        funFact = "Das Sundance Institute selbst wurde 1981 von Redford als Labor fuer unabhaengige Filmemacher gegruendet, mit Workshops in den Bergen Utahs. Der Name 'Sundance' stammt aus 'Butch Cassidy and the Sundance Kid' (1969) — Redford spielte den Sundance Kid in dem Film, der sein Star-Image begruendete."
    ),

    // Question 38 - sex, lies, and videotape
    Question(
        categoryId = 4,
        questionText = "Steven Soderberghs 'sex, lies, and videotape' (1989) gewann die Palme d'Or in Cannes. Wie alt war Soderbergh bei der Veroeffentlichung des Films?",
        answerA = "23 Jahre",
        answerB = "26 Jahre",
        answerC = "29 Jahre",
        answerD = "32 Jahre",
        correctAnswer = 1, // B
        explanation = "Steven Soderbergh wurde am 14. Januar 1963 geboren und war 26 Jahre alt, als 'sex, lies, and videotape' 1989 die Palme d'Or in Cannes gewann. Er ist damit einer der juengsten Gewinner der Palme d'Or. Der Film kostete ca. $1,2 Millionen und spielte ueber $24 Millionen ein.",
        difficulty = 5,
        funFact = "Soderbergh schrieb das Drehbuch in 8 Tagen in einem Notizbuch, waehrend er einen Freund auf einer Autofahrt begleitete. Der Film wurde zum Prototyp des Sundance-Indie-Films der 90er: kleine Crew, psychologisch intensive Charaktere, kein Action-Plot. Soderbergh-Interviewt gab spaeter zu, der Film sei 'fast zu therapiefoermig'."
    ),

    // Question 39 - Pulp Fiction Produktionsgeschichte
    Question(
        categoryId = 4,
        questionText = "Quentin Tarantinos 'Pulp Fiction' (1994) wurde mit welchem ungewoehnlich kleinen Budget fuer einen Cannes-Palme-d'Or-Gewinner produziert?",
        answerA = "Ca. $2 Millionen",
        answerB = "Ca. $8,5 Millionen",
        answerC = "Ca. $15 Millionen",
        answerD = "Ca. $25 Millionen",
        correctAnswer = 1, // B
        explanation = "'Pulp Fiction' hatte ein Budget von ca. $8,5 Millionen — fuer einen Cannes-Gewinner und Oscar-Nominierten fuer den Besten Film sehr bescheiden. Produziert von Miramax spielte er weltweit ueber $213 Millionen ein. Das Verhaeltnis von Budget zu Einnahmen macht ihn zu einem der profitabelsten Filme der 90er.",
        difficulty = 5,
        funFact = "John Travolta wurde fuer $140.000 engagiert — Tarantino wollte ihn bewusst fuer seine Karriere revialisieren. Harvey Keitel spielte Winston Wolfe (den Aufraeumspezialist) in nur 3 Drehtagen. Die Struktur des Films mit seiner nicht-linearen Erzaehlung wurde von Akira Kurosawa's 'Rashomon' und Cains Noir-Romanen inspiriert."
    ),

    // Question 40 - Clerks Budget
    Question(
        categoryId = 4,
        questionText = "Kevin Smiths 'Clerks' (1994) ist beruehmt fuer sein Minimalbudget. Wie gross war es tatsaechlich?",
        answerA = "Ca. $27.000",
        answerB = "Ca. $75.000",
        answerC = "Ca. $150.000",
        answerD = "Ca. $230.000",
        correctAnswer = 0, // A
        explanation = "'Clerks' wurde fuer ca. $27.575 produziert — Smith finanzierte den Film durch Kreditkartenschulden und den Verkauf seiner Comicsammlung. Er drehte im echten Quickstop-Laden, in dem er arbeitete, nachts (damit er tags arbeiten konnte). Der Film spielte ueber $3 Millionen ein und wurde ein Kultfilm des Independent-Kinos der 90er.",
        difficulty = 5,
        funFact = "Kevin Smith musste den Quickstop-Laden heimlich nachts drehen, weil er keinen offiziellen Drehgenehmigung hatte. Er liess den Shutter am Fenster absichtlich heruntergezogen, darum ist der Film in Schwarzweiss — es sah aus als waere es Nacht, selbst wenn es Tag war. Die Schwarzweiss-Aesthetik wurde spaeter als kuenstlerische Entscheidung gelesen."
    ),

    // Question 41 - Sundance Effekt
    Question(
        categoryId = 4,
        questionText = "Welcher Independent-Film gewann 1992 den Sundance Grand Jury Prize und definierte das 'Sundance-Kino' der fruehen 90er mit?",
        answerA = "El Mariachi (1992)",
        answerB = "Laws of Gravity (1992)",
        answerC = "In the Soup (1992)",
        answerD = "Reservoir Dogs (1992)",
        correctAnswer = 2, // C
        explanation = "'In the Soup' (1992) von Alexandre Rockwell gewann den Sundance Grand Jury Prize. 'Reservoir Dogs' hatte zwar auch 1992 Premiere bei Sundance, aber gewann nicht. 'El Mariachi' gewann spaeter (1993 bei Sundance fuer 1992 produzierten Film). 'In the Soup' ist heute vergessen, war aber damals der offizielle Sundance-Sieger.",
        difficulty = 5,
        funFact = "Robert Rodriguez drehte 'El Mariachi' (1992) fuer $7.000, indem er sich fuer medizinische Experimente bezahlte liess. Columbia Pictures kaufte ihn fuer $1 Million und gab $250.000 aus, um ihn technisch aufzuwerten. Rodriguez wurde zum Inbegriff des Do-it-yourself-Filmemachers der 90er-Indie-Bewegung."
    ),

    // --- WEITERE STUMMFILM-THEMEN ---

    // Question 42 - Charlie Chaplin Tramp
    Question(
        categoryId = 4,
        questionText = "In welchem Film erschien Charlie Chaplins Tramp-Charakter zum ersten Mal?",
        answerA = "The Kid (1921)",
        answerB = "Making a Living (1914)",
        answerC = "Kid Auto Races at Venice (1914)",
        answerD = "Tillie's Punctured Romance (1914)",
        correctAnswer = 2, // C
        explanation = "'Kid Auto Races at Venice' (1914) war der erste Film, in dem der Tramp-Charakter mit dem charakteristischen Kostum (Melone, Schnurrbart, Stock, grosse Schuhe) gezeigt wurde — obwohl 'Mabel's Strange Predicament' unmittelbar davor gedreht wurde. Beide erschienen am selben Tag, aber Venice wurde zuerst veroeffentlicht.",
        difficulty = 5,
        funFact = "Chaplin entwickelte das Tramp-Kostum spontan im Kostumfundus von Keystone Studios: Er kombinierte Sachen von verschiedenen Kollegen — Fatty Arbuckles Hosen (zu weit), Ford Sterlings Schuhe (zu gross und vertauscht), Mack Swains Schnurrbart (verkleinert). Die zufaellige Zusammenstellung wurde zur ikonischsten Figur der Filmgeschichte."
    ),

    // Question 43 - Keaton Stunts
    Question(
        categoryId = 4,
        questionText = "Buster Keaton fuehrte alle seine Stunts selbst aus. Welcher Stunt in 'Steamboat Bill, Jr.' (1928) gilt als einer der gefaehrlichsten je gedrehten?",
        answerA = "Keaton haengt an einer Eisenbahn-Uhr",
        answerB = "Eine Fassade eines zweistoeckigen Hauses faellt auf Keaton, der durch ein Fenster unverletzt bleibt",
        answerC = "Keaton springt von einem Dach auf einen fahrenden Zug",
        answerD = "Keaton wird von einer echten Lawine begraben",
        correctAnswer = 1, // B
        explanation = "In 'Steamboat Bill, Jr.' faellt eine komplette Hausfassade auf Keaton — er steht exakt auf der Stelle, wo ein Fensterrahmen ihn verschont (nur wenige Zentimeter Spielraum). Der Stunt wurde ohne Sicherheitsnetz gedreht. Keatons Stunt-Koordinatoren sollen geweint haben, weil sie ueberzeugt waren, er wuerde sterben.",
        difficulty = 5,
        funFact = "Keaton brach sich bei einem anderen Film ('Sherlock Jr.', 1924) den Hals, ohne es zu wissen: Er wurde von einem Wasserstrahl getroffen, schlug mit dem Kopf auf die Schienen und drehte weiter. Jahrzehnte spaeter entdeckten Aerzte durch ein Roentgenbild, dass er damals eine Fraktur erlitten hatte."
    ),

    // Question 44 - Harold Lloyd Safety Last
    Question(
        categoryId = 4,
        questionText = "Harold Lloyds ikonische Uhrszene in 'Safety Last!' (1923) — wo er an einem Uhrturm haengt — wurde in welcher Stadt gedreht, die als Kulisse fuer das Bild diente?",
        answerA = "New York City",
        answerB = "Los Angeles (Gebaeudefassade im Studio)",
        answerC = "Chicago",
        answerD = "San Francisco",
        correctAnswer = 1, // B
        explanation = "Die beruehrmte Szene wurde nicht an einem echten Hochhaus gedreht. Lloyd hing tatsaechlich auf einem mehrstoeckigen Gebaeudeaufbau auf einem Studiodach in Los Angeles — mit Auffangnetzen tiefer unten, die aber im Bild nicht sichtbar waren. Die Hintergrund-Kulisse war Los Angeles, aber die Illusion der extremen Hoehe war durch clevere Kamerapositionierung erzeugt.",
        difficulty = 5,
        funFact = "Harold Lloyd fehlte durch einen Unfall am Set 1919 der Daumen und Zeigefinger der rechten Hand — er trug bei allen seinen Filmen einen speziellen Handschuh. Trotz dieser Behinderung fuehrte er die meisten Stunts selbst aus. Lloyd war wirtschaftlich der erfolgreichste Stummfilm-Komiker — reicher als Chaplin und Keaton zusammen."
    ),

    // Question 45 - Lumiere vs Melies
    Question(
        categoryId = 4,
        questionText = "Andre Bazin beschrieb in seinen einflussreichen Filmtheorien zwei Grundstroeme des Kinos, die er auf Lumiere und Melies zurueckfuehrte. Wie charakterisierte er den fundamentalen Unterschied?",
        answerA = "Lumiere fuer Dokumentation der Realitaet, Melies fuer Erschaffung von Illusionen",
        answerB = "Lumiere fuer Aussenaufnahmen, Melies fuer Studioaufnahmen",
        answerC = "Lumiere fuer Komoedie, Melies fuer Drama",
        answerD = "Lumiere fuer Kurzfilm, Melies fuer Langfilm",
        correctAnswer = 0, // A
        explanation = "Andre Bazin identifizierte in 'Was ist Kino?' zwei fundamentale Traditionen: die Lumiere-Tradition (Glaube an die Realitaet — dokumentarischer Impuls, der Welt zeigen wie sie ist) und die Melies-Tradition (Glaube an das Bild — illusionistischer Impuls, Fantasiewelten erschaffen). Diese Dichotomie ist ein Grundpfeiler der Filmtheorie.",
        difficulty = 5,
        funFact = "Bazin war einer der Mitgruender der Cahiers du Cinema (1951) und Mentor der Franzosischen Nouvelle Vague. Obwohl er 1958 kurz nach dem Beginn der Nouvelle Vague starb, pragen seine Theorien — besonders sein Konzept des 'ontologischen Realismus' und der Mise en Scene — die Filmwissenschaft bis heute."
    ),

    // --- NEUE WELLEN (INTERNATIONALE PERSPEKTIVE) ---

    // Question 46 - Nouvelle Vague Cahiers
    Question(
        categoryId = 4,
        questionText = "Die Politique des Auteurs — die Idee, dass der Regisseur der wahre Autor eines Films sei — wurde in welcher Zeitschrift von den spaaeteren Nouvelle-Vague-Regisseuren entwickelt?",
        answerA = "Positif",
        answerB = "Sight & Sound",
        answerC = "Cahiers du Cinema",
        answerD = "Film Comment",
        correctAnswer = 2, // C
        explanation = "Die Cahiers du Cinema, gegruendet 1951 unter Mitherausgeberschaft von Andre Bazin, war das Forum fuer Truffaut, Godard, Chabrol, Rivette und Rohmer. Ihre Kritiken — besonders Truffauts polemischer Essay 'Une certaine tendance du cinema francais' (1954) — entwickelten die Autorentheorie, die das moderne Filmdenken revolutionierte.",
        difficulty = 5,
        funFact = "Truffauts Artikel von 1954 war ein scharfer Angriff auf das damalige franzoesische 'Qualitaets-Kino' der Adaptationen literarischer Werke. Er nannte diese Regisseure veraecht 'Metteurs en scene' (bloss Inszeneure) statt echte Auteure. Der Artikel schockierte die franzoesische Filmwelt und etablierte eine neue kritische Sprache."
    ),

    // Question 47 - Italian Neorealism
    Question(
        categoryId = 4,
        questionText = "Welches war das erste Werk des Italienischen Neorealismus, gedreht waehrend der deutschen Besatzung Italiens mit Laienschauspielern und echter Kriegszerstoerung als Hintergrund?",
        answerA = "Paisa (1946)",
        answerB = "Roma, Citta Aperta (1945)",
        answerC = "Ladri di biciclette (1948)",
        answerD = "Ossessione (1943)",
        correctAnswer = 1, // B
        explanation = "Roberto Rossellinis 'Roma, Citta Aperta' (Roma, offene Stadt, 1945) gilt als erstes und definierendes Werk des Neorealismus. Gedreht kurz nach der Befreiung Roms mit gestohlenem Filmmaterial, auf echten Strassenscenes, grossteils mit Laienschauspielern. Der Film lief noch, als der Zweite Weltkrieg endete.",
        difficulty = 5,
        funFact = "Rossellini drehte 'Roma, Citta Aperta' mit so beschraenkten Mitteln, dass er Filmrollen von verschiedenen Quellen zusammenkaufte, die in unterschiedlichen Kontraststufen entwickelt wurden. Dies erklaert die ungleiche Bildqualitaet des Films. Paradoxerweise wurde diese technische Inkonsistenz zum aesthetischen Markenzeichen des Neorealismus."
    ),

    // Question 48 - Czech New Wave
    Question(
        categoryId = 4,
        questionText = "Die Tschechische Neue Welle (Ceska Nowa Vlna) der 1960er fand ein abruptes Ende. Welches historische Ereignis beendete sie 1968?",
        answerA = "Der Prager Fruehling und die sowjetische Invasion",
        answerB = "Ein Zensurgesetz der tschechischen Kommunistischen Partei",
        answerC = "Die Schliessung der FAMU-Filmhochschule",
        answerD = "Der Tod von Milos Forman",
        correctAnswer = 0, // A
        explanation = "Der Prager Fruehling (Januar-August 1968) und die sowjetische Invasion am 21. August 1968 beendeten die tschechische Neue Welle abrupt. Viele Regisseure emigrierten (Milos Forman, Ivan Passer nach USA; Jan Nemec nach Deutschland). Filme wie 'Der Feuerwehrball' wurden verboten. Die 'Normalisierung' unter Husak strangulierte die Filmkultur.",
        difficulty = 5,
        funFact = "'Schlafen fuer die Kommenden' ('Closely Watched Trains', 1966) von Jiri Menzel gewann den Oscar fuer den Besten Fremdsprachigen Film und machte die tschechische Nouvelle Vague weltweit bekannt. Der Regisseur Milos Forman emigrierte in die USA, wo er spaeter 'One Flew Over the Cuckoo's Nest' (1975) und 'Amadeus' (1984) drehte."
    ),

    // Question 49 - Dogme 95
    Question(
        categoryId = 4,
        questionText = "Das Dogme-95-Manifest wurde 1995 von Lars von Trier und Thomas Vinterberg veroeffentlicht. Welche technische Regel verbot das 'Keuschheitsgeluebde' des Manifests explizit?",
        answerA = "Keine Musikuntermalung ausser naturalistisch vorkommende Musik",
        answerB = "Kein kuenstliches Licht",
        answerC = "Keine Studioaufnahmen",
        answerD = "Alle oben genannten Regeln",
        correctAnswer = 3, // D
        explanation = "Das 'Keuschheitsgeluebde' (Vow of Chastity) des Dogme-95-Manifests verbot eine Vielzahl von Techniken: kein kuenstliches Licht, kein nicht-diegetischer Ton (keine Filmmusik), keine optischen Spezialeffekte, Handheld-Kamera, Dreh an Originalschauplätzen (kein Studio), keine Genrefilme, und mehr. Alle genannten Optionen sind korrekt.",
        difficulty = 5,
        funFact = "Lars von Trier entwarf das Manifest als Provokation gegen das Mainstream-Kino, aber auch als kuenstlerische Beschraenkung aehnlich OuLiPo in der Literatur. Ironischerweise brachen sowohl von Trier als auch Vinterberg ihre eigenen Regeln bei ihren ersten Dogme-Filmen. Vinterbergs 'Festen' ('Das Fest', 1998) war der erste offizielle Dogme-Film."
    ),

    // Question 50 - Murnau Sunrise
    Question(
        categoryId = 4,
        questionText = "F.W. Murnaus 'Sunrise: A Song of Two Humans' (1927) gewann bei der ersten Oscar-Verleihung 1929 eine einzigartige Kategorie, die nur einmal vergeben wurde. Welche war das?",
        answerA = "Bester Stummfilm",
        answerB = "Bestes Einzigartiges und Kuenstlerisches Bild (Unique and Artistic Picture)",
        answerC = "Bester fremdsprachiger Film",
        answerD = "Bester experimenteller Film",
        correctAnswer = 1, // B
        explanation = "Bei der ersten Oscar-Verleihung (1929) gab es zwei Kategorien fuer Beste Bilder: 'Outstanding Picture' (gewann 'Wings') und 'Unique and Artistic Picture' (gewann 'Sunrise'). Diese zweite Kategorie wurde danach nie wieder vergeben. Murnaus 'Sunrise' gilt vielen Filmhistorikern als der groesste Stummfilm überhaupt.",
        difficulty = 5,
        funFact = "Murnau drehte 'Sunrise' in Hollywood auf Einladung von Fox-Studio-Chef William Fox. Das Budget von $1,5 Millionen war gigantisch fuer 1927. Murnau baute eine komplette europaeische Stadt auf dem Studiogelaende, Schleppbahnen fuer die Kamera und entwickelte komplexe Doppelbelichtungstricks. Er starb 1931 bei einem Autounfall in Kalifornien, nur 42 Jahre alt."
    )

)
