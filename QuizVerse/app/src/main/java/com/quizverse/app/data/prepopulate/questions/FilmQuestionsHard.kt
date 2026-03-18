package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsHard(): List<Question> = listOf(

    // ── ACADEMY AWARD CONTROVERSIES & RECORDS ────────────────────────────────

    // Question 1 – Most Oscar wins by a single film
    Question(
        categoryId = 4,
        questionText = "Welche drei Filme teilen sich den Rekord von je 11 Academy Awards?",
        answerA = "'Ben-Hur', 'Titanic' und 'The Lord of the Rings: The Return of the King'",
        answerB = "'Gone with the Wind', 'Schindler's List' und 'Avatar'",
        answerC = "'All About Eve', 'Titanic' und 'Gladiator'",
        answerD = "'Ben-Hur', 'Lawrence of Arabia' und 'Braveheart'",
        correctAnswer = 0, // A
        explanation = "'Ben-Hur' (1959), 'Titanic' (1997) und 'The Lord of the Rings: The Return of the King' (2003) gewannen je 11 Oscars — der hoechste je erreichte Wert. 'Ben-Hur' war dabei bei 12 Nominierungen erfolgreich.",
        difficulty = 3,
        funFact = "'The Return of the King' gewann alle 11 nominierten Kategorien ohne eine einzige Niederlage — ein bis dato einmaliges Ergebnis."
    ),

    // Question 2 – Most nominations without a win
    Question(
        categoryId = 4,
        questionText = "Welcher Film haelt mit 14 Nominierungen und null Gewinnen den unruhmlichen Rekord der meisten Oscar-Nominierungen ohne einen einzigen Sieg?",
        answerA = "'The Color Purple' (1985)",
        answerB = "'Gangs of New York' (2002)",
        answerC = "'The Turning Point' (1977)",
        answerD = "'Uncut Gems' (2019)",
        correctAnswer = 2, // C
        explanation = "'The Turning Point' (1977) und 'The Color Purple' (1985) teilen sich den Rekord mit je 11 Nominierungen ohne Sieg. Doch den absoluten Rekord haelt 'The Turning Point' unter den damaligen Filmen.",
        difficulty = 3,
        funFact = "Tatsaechlich teilen sich 'The Turning Point' und 'The Color Purple' je 11 Nominierungen und 0 Siege — beide gelten als historisch groesste Oscar-Niederlagen."
    ),

    // Question 3 – Marlon Brando Oscar refusal
    Question(
        categoryId = 4,
        questionText = "Wer nahm 1973 den Oscar fuer Marlon Brando entgegen und aus welchem Grund lehnte Brando die Auszeichnung ab?",
        answerA = "Jane Fonda, wegen Brandos Protest gegen den Vietnamkrieg",
        answerB = "Sacheen Littlefeather, wegen der Darstellung von Indigenen im amerikanischen Kino",
        answerC = "Francis Ford Coppola, wegen eines Vertragsstreits mit der Academy",
        answerD = "Al Pacino, wegen Brandos Weigerung gegen die Kommerzialisierung Hollywoods",
        correctAnswer = 1, // B
        explanation = "Die Apache-Aktivistin Sacheen Littlefeather trat 1973 stellvertretend auf die Buehne und lehnte Brandos Oscar fuer 'Der Pate' ab. Sie verlas eine Erklaerung gegen die Darstellung von Native Americans im Film und Fernsehen.",
        difficulty = 3,
        funFact = "Die Academy entschuldigte sich erst 2022 — fast 50 Jahre spaeter — offiziell bei Sacheen Littlefeather fuer das Verhalten der Branche gegenueber ihr an jenem Abend."
    ),

    // Question 4 – First non-English Best Picture winner
    Question(
        categoryId = 4,
        questionText = "Welcher Film gewann als erster nicht-englischsprachiger Film den Oscar fuer den besten Film?",
        answerA = "'Roma' (2018) von Alfonso Cuaron",
        answerB = "'Crouching Tiger, Hidden Dragon' (2000) von Ang Lee",
        answerC = "'Parasite' (2019) von Bong Joon-ho",
        answerD = "'Das Leben der Anderen' (2006) von Florian Henckel von Donnersmarck",
        correctAnswer = 2, // C
        explanation = "'Parasite' (Gisaengchung) gewann 2020 als erster fremdsprachiger Film den Oscar fuer den besten Film und raeumte zugleich Beste Regie, Bestes Original-Drehbuch und Besten Internationalen Film ab.",
        difficulty = 3,
        funFact = "Regisseur Bong Joon-ho erinnerte das Publikum in seiner Dankesrede an die Aussage von Martin Scorsese: 'Die persoenlichste Kunst ist auch die universellste.'"
    ),

    // Question 5 – Harvey Weinstein Oscar controversy
    Question(
        categoryId = 4,
        questionText = "In welchem Jahr gewann 'Shakespeare in Love' ueberraschend den Oscar fuer den besten Film gegen den grossen Favoriten 'Saving Private Ryan'?",
        answerA = "1997",
        answerB = "1998",
        answerC = "1999",
        answerD = "2000",
        correctAnswer = 2, // C
        explanation = "Bei der Verleihung 1999 (fuer Filme des Jahres 1998) gewann 'Shakespeare in Love' den Oscar fuer den besten Film. Harvey Weinsteins aggressiver Wahlkampf fuer den Film gilt bis heute als Wendepunkt in der Oscar-Kampagnenstrategie.",
        difficulty = 3,
        funFact = "Steven Spielberg galt mit 'Saving Private Ryan' als so sicherer Sieger, dass viele Kritiker den Gewinn von 'Shakespeare in Love' als den groessten Oscar-Skandal der modernen Aera bezeichneten."
    ),

    // ── AUTEUR THEORY ────────────────────────────────────────────────────────

    // Question 6 – Politique des auteurs origin
    Question(
        categoryId = 4,
        questionText = "In welcher Publikation formulierte Francois Truffaut 1954 erstmals seine 'Politique des auteurs', die spaeter zur Auteur-Theorie wurde?",
        answerA = "Le Monde",
        answerB = "Positif",
        answerC = "Cahiers du Cinema",
        answerD = "L'Humanite",
        correctAnswer = 2, // C
        explanation = "Truffauts Aufsatz 'Une certaine tendance du cinema francais' in den 'Cahiers du Cinema' (Nr. 31, Januar 1954) legte den Grundstein der Auteur-Theorie und attackierte die damalige Tradition des 'Cinema de qualite' in Frankreich.",
        difficulty = 3,
        funFact = "Andrew Sarris popularisierte den Begriff 'auteur theory' im anglophonen Raum durch seinen einflussreichen Aufsatz in der amerikanischen Zeitschrift 'Film Culture' von 1962."
    ),

    // Question 7 – Stanley Kubrick auteur
    Question(
        categoryId = 4,
        questionText = "Stanley Kubrick adaptierte fuer '2001: A Space Odyssey' eine Kurzgeschichte von Arthur C. Clarke. Wie hiess sie?",
        answerA = "'The Sentinel'",
        answerB = "'Childhood's End'",
        answerC = "'Rendezvous with Rama'",
        answerD = "'The Star'",
        correctAnswer = 0, // A
        explanation = "Kubricks '2001: A Space Odyssey' (1968) basiert auf Clarkes Kurzgeschichte 'The Sentinel' (1951). Clarke schrieb parallel zur Produktion den Roman, der gleichzeitig mit dem Film veroeffentlicht wurde.",
        difficulty = 3,
        funFact = "Kubrick und Clarke arbeiteten gleichzeitig an Roman und Drehbuch. Kubrick untersagte Clarke, das Buch vor dem Filmstart zu veroeffentlichen, um die Deutungsoffenheit des Films zu bewahren."
    ),

    // Question 8 – Ingmar Bergman and his cinematographer
    Question(
        categoryId = 4,
        questionText = "Mit welchem Kameramann drehte Ingmar Bergman den Grossteil seiner Meisterwerke, darunter 'Das siebente Siegel' und 'Wilde Erdbeeren'?",
        answerA = "Vilmos Zsigmond",
        answerB = "Gunnar Fischer",
        answerC = "Sven Nykvist",
        answerD = "Jorgen Persson",
        correctAnswer = 2, // C
        explanation = "Sven Nykvist war ab Ende der 1950er Jahre Bergmans bevorzugter Kameramann und drehte mit ihm Klassiker wie 'Persona' (1966), 'Schreie und Fluestern' (1972) und 'Fanny und Alexander' (1982). Nykvist gewann zwei Oscars fuer die Kamera.",
        difficulty = 3,
        funFact = "Vor Nykvist drehte Gunnar Fischer Bergmans fruehe Klassiker — darunter 'Das siebente Siegel' (1957) und 'Wilde Erdbeeren' (1957)."
    ),

    // Question 9 – Jean-Luc Godard filmmaking technique
    Question(
        categoryId = 4,
        questionText = "Welche Schnitttechnik wurde durch Jean-Luc Godards 'A bout de souffle' (1960) zum Markenzeichen der Nouvelle Vague?",
        answerA = "Match Cut",
        answerB = "Jump Cut",
        answerC = "L-Cut",
        answerD = "Smash Cut",
        correctAnswer = 1, // B
        explanation = "Der 'Jump Cut' — ein abrupter Schnitt innerhalb einer Szene, der die zeitliche Kontinuitaet bricht — wurde durch 'A bout de souffle' (Ausser Atem) weltberuehmt. Godard setzte ihn bewusst ein, um klassische Konventionen zu zerstoeren.",
        difficulty = 3,
        funFact = "Godard schnitt urspruenglich zu lange Szenen einfach durch herausschneiden von Mittelteilen zusammen — was aus technischer Notwendigkeit zur stilistischen Revolution wurde."
    ),

    // Question 10 – Akira Kurosawa influence
    Question(
        categoryId = 4,
        questionText = "Welcher Kurosawa-Film diente als direkte Vorlage fuer Sergio Leones 'Fuer eine Handvoll Dollar' (1964)?",
        answerA = "'Rashomon' (1950)",
        answerB = "'Yojimbo' (1961)",
        answerC = "'Sanjuro' (1962)",
        answerD = "'Die sieben Samurai' (1954)",
        correctAnswer = 1, // B
        explanation = "'Yojimbo' (1961) ist die direkte, nicht lizenzierte Vorlage fuer Leones 'Per un pugno di dollari'. Kurosawa verklagte Leone erfolgreich; er erhielt 15 % der Welterloes-Rechte und eine pauschale Abfindung.",
        difficulty = 3,
        funFact = "Kurosawa schrieb Sergio Leone einen Brief: 'Ich habe Ihren Film gesehen. Er ist sehr gut, aber es ist mein Film.' Die Einigung soll Kurosawa mehr eingebracht haben als jeder seiner eigenen Filme."
    ),

    // ── FILM MOVEMENTS ────────────────────────────────────────────────────────

    // Question 11 – Dogme 95 rules (Vow of Chastity)
    Question(
        categoryId = 4,
        questionText = "Welche der folgenden Regeln gehoert NICHT zum 'Keuschheitsgeluebde' (Vow of Chastity) des Dogme-95-Manifests?",
        answerA = "Dreharbeiten muessen am Originalschauplatz stattfinden",
        answerB = "Kein Einsatz von nicht-diegetischer Musik",
        answerC = "Drehbuch und Regie muessen von derselben Person stammen",
        answerD = "Der Film muss in Farbe gedreht werden",
        correctAnswer = 2, // C
        explanation = "Das Keuschheitsgeluebde schreibt u.a. Originalschauplatz, kein nicht-diegetisches Sounddesign, Handkamera und Farbe vor. Eine Identitaet von Autor und Regisseur ist jedoch keine Dogme-Regel.",
        difficulty = 3,
        funFact = "Lars von Trier und Thomas Vinterberg unterzeichneten das Manifest am 13. März 1995 in Paris. Vinterbergs 'Das Fest' (1998) gilt als erster echter Dogme-Film."
    ),

    // Question 12 – First Dogme 95 film
    Question(
        categoryId = 4,
        questionText = "Welcher Film tragt die offizielle Zertifikatnummer Dogme Nr. 1?",
        answerA = "'Idioten' von Lars von Trier (1998)",
        answerB = "'Mifune' von Soren Kragh-Jacobsen (1999)",
        answerC = "'Das Fest' (Festen) von Thomas Vinterberg (1998)",
        answerD = "'Julien Donkey-Boy' von Harmony Korine (1999)",
        correctAnswer = 2, // C
        explanation = "Thomas Vinterbergs 'Festen' (Das Fest, 1998) tragt die Zertifikatnummer Dogme Nr. 1. Lars von Triers 'Idioterne' hat die Nr. 2. Beide Filme feierten auf Cannes 1998 Premiere.",
        difficulty = 3,
        funFact = "Vinterberg gestand spaeter, eine der Dogme-Regeln gebrochen zu haben: Eine Szene wurde durch ein Fenster gefilmt, das er entfernen liess — was die Nutzung eines kuenstlichen Lichtrahmens bedeutete."
    ),

    // Question 13 – Third Cinema (Tercer Cine)
    Question(
        categoryId = 4,
        questionText = "Das 'Dritte Kino' (Tercer Cine) wurde massgeblich durch das argentinische Manifest 'Hacia un Tercer Cine' gepraegt. Wer verfasste es 1969?",
        answerA = "Glauber Rocha und Nelson Pereira dos Santos",
        answerB = "Fernando Solanas und Octavio Getino",
        answerC = "Jorge Sanjines und Antonio Eguino",
        answerD = "Tomás Gutiérrez Alea und Juan Carlos Tabío",
        correctAnswer = 1, // B
        explanation = "Fernando Solanas und Octavio Getino verfassten 1969 das Manifest 'Hacia un Tercer Cine' und definierten das Dritte Kino als revolutionaeres Gegenprojekt zum Ersten Kino (Hollywood) und Zweiten Kino (Autorenfilm).",
        difficulty = 3,
        funFact = "Solanas und Getino schufen auch 'La hora de los hornos' (1968), ein vierstuendiges Dokumentarfilm-Manifest, das im Untergrund verbreitet wurde und als Gruendungsdokument des Dritten Kinos gilt."
    ),

    // Question 14 – Cinema Vérité founder
    Question(
        categoryId = 4,
        questionText = "Der Begriff 'Cinema Verite' geht auf das Konzept 'Kino-Wahrheit' eines sowjetischen Filmemachers zurueck. Wer war er?",
        answerA = "Sergei Eisenstein",
        answerB = "Dziga Vertov",
        answerC = "Alexander Dovzhenko",
        answerD = "Vsevolod Pudovkin",
        correctAnswer = 1, // B
        explanation = "Dziga Vertovs Konzept 'Kino-Pravda' (Kino-Wahrheit, 1922-1925) war die direkte Vorlage fuer Jean Rouchs und Edgar Morins Begriff 'Cinema Verite', mit dem sie 1961 ihren Film 'Chronique d'un ete' bezeichneten.",
        difficulty = 3,
        funFact = "Vertovs Meisterwerk 'Der Mann mit der Kamera' (1929) gilt als radikalstes Experiment des fruehen Dokumentarfilms und beeinflusst Filmemacher bis heute."
    ),

    // Question 15 – Direct Cinema vs. Cinema Verité
    Question(
        categoryId = 4,
        questionText = "Was unterscheidet 'Direct Cinema' (USA) vom franzoesischen 'Cinema Verite' in der zentralen Methodik?",
        answerA = "Direct Cinema bevorzugt Studioaufnahmen, Cinema Verite ausschliesslich Aussenaufnahmen",
        answerB = "Direct Cinema behauptet passive Beobachtung ohne Eingriff, Cinema Verite provoziert bewusst Reaktionen durch Filmemacher-Beteiligung",
        answerC = "Direct Cinema verzichtet auf Ton, Cinema Verite nutzt Synchronton",
        answerD = "Direct Cinema arbeitet mit 16mm-Film, Cinema Verite ausschliesslich mit Video",
        correctAnswer = 1, // B
        explanation = "Direct Cinema (Leacock, Pennebaker, Maysles) behauptet die 'Fliege an der Wand' — passives Beobachten ohne Eingriff. Cinema Verite (Rouch, Morin) sieht den Filmemacher als aktiven Provokateur, der durch seine Praesenz Realitaet erst sichtbar macht.",
        difficulty = 3,
        funFact = "Frederick Wiseman, ein Vertreter des Direct Cinema, drehte 'Titicut Follies' (1967) in einem Gefaengnis fuer psychisch Kranke — der erste US-Film, der wegen seines Inhalts (nicht Pornographie) gerichtlich verboten wurde."
    ),

    // ── FAMOUS CINEMATOGRAPHERS ──────────────────────────────────────────────

    // Question 16 – Roger Deakins Oscar record
    Question(
        categoryId = 4,
        questionText = "Roger Deakins war 15-mal fuer den Oscar nominiert, bevor er ihn gewann. Fuer welchen Film erhielt er 2018 endlich seine erste Auszeichnung?",
        answerA = "'Sicario' (2015)",
        answerB = "'The Shawshank Redemption' (1994)",
        answerC = "'Blade Runner 2049' (2017)",
        answerD = "'No Country for Old Men' (2007)",
        correctAnswer = 2, // C
        explanation = "Roger Deakins gewann nach 14 vergeblichen Nominierungen 2018 seinen ersten Oscar fuer die Kameraarbeit in Denis Villeneuves 'Blade Runner 2049'. Seither gewann er noch einen zweiten fuer '1917' (2020).",
        difficulty = 3,
        funFact = "Deakins ist fuer seine enge Zusammenarbeit mit den Coen-Brueder bekannt: Er drehte 'Fargo', 'The Big Lebowski', 'No Country for Old Men' und 'True Grit' mit ihnen."
    ),

    // Question 17 – Vittorio Storaro and color theory
    Question(
        categoryId = 4,
        questionText = "Vittorio Storaro, dreifacher Oscar-Gewinner, entwickelte ein persoenliches Farbkonzept, das er 'Chromatic Writing' nennt. Fuer welchen Regisseur drehte er 'Apocalypse Now' (1979) und 'Reds' (1981)?",
        answerA = "Martin Scorsese",
        answerB = "Francis Ford Coppola und Warren Beatty",
        answerC = "Bernardo Bertolucci",
        answerD = "Luchino Visconti",
        correctAnswer = 1, // B
        explanation = "Storaro gewann den Oscar fuer 'Apocalypse Now' (Regie: Francis Ford Coppola, 1979), 'Reds' (Regie: Warren Beatty, 1981) und 'The Last Emperor' (Regie: Bernardo Bertolucci, 1987).",
        difficulty = 3,
        funFact = "Bertolucci und Storaro arbeiteten seit 'Der Konformist' (1970) zusammen. Storaro setzte in 'The Last Emperor' verschiedene Farben systematisch ein, um Zeitperioden und emotionale Zustande zu kennzeichnen."
    ),

    // Question 18 – Emmanuel Lubezki and long takes
    Question(
        categoryId = 4,
        questionText = "Emmanuel Lubezki ('El Chivo') gewann drei Oscar-Auszeichnungen in Folge. Fuer welche drei Filme?",
        answerA = "'Children of Men', 'Gravity' und 'The Revenant'",
        answerB = "'Gravity', 'Birdman' und 'The Revenant'",
        answerC = "'Birdman', 'The Revenant' und 'Silence'",
        answerD = "'Y Tu Mama Tambien', 'Gravity' und 'Birdman'",
        correctAnswer = 1, // B
        explanation = "Emmanuel Lubezki gewann drei aufeinanderfolgende Oscars fuer 'Gravity' (2014), 'Birdman' (2015) und 'The Revenant' (2016) — ein historisch einmaliger Dreierpack in dieser Kategorie.",
        difficulty = 3,
        funFact = "Lubezki drehte 'The Revenant' ausschliesslich mit natuerlichem Licht. Das zwang das Team, jeden Tag nur waehrend des kurzen 'Magic Hour'-Fensters bei Daemmerung zu filmen."
    ),

    // Question 19 – Gregg Toland and deep focus
    Question(
        categoryId = 4,
        questionText = "Welcher Kameramann entwickelte die 'Tiefenschaerfe' (deep focus photography) und setzte sie in 'Citizen Kane' (1941) bahnbrechend ein?",
        answerA = "James Wong Howe",
        answerB = "Gregg Toland",
        answerC = "Karl Freund",
        answerD = "Lee Garmes",
        correctAnswer = 1, // B
        explanation = "Gregg Toland revolutionierte mit seiner Tiefenschaerfe-Technik in 'Citizen Kane' die Kinoaesthetik. Durch spezielle Linsen und extreme Blendenwerte waren Vordergrund und Hintergrund gleichzeitig scharf.",
        difficulty = 3,
        funFact = "Orson Welles ehrte Tolands Leistung, indem er im Abspann von 'Citizen Kane' Welles' und Tolands Namen nebeneinander in einem Rahmen zeigte — etwas damals Unerhoeates fuer einen Kameramann."
    ),

    // Question 20 – Néstor Almendros and natural light
    Question(
        categoryId = 4,
        questionText = "Nestor Almendros, Kameramann fuer Truffaut und Terrence Malick, ist beruehmt fuer eine Szene in 'Days of Heaven' (1978), bei der ausschliesslich eine bestimmte Tageszeit genutzt wurde. Welche?",
        answerA = "Mittagssonne (Zenith Light)",
        answerB = "Blaue Stunde vor Sonnenaufgang",
        answerC = "Magische Stunde (Magic Hour) bei Sonnenuntergang",
        answerD = "Mondlicht-Aufnahmen",
        correctAnswer = 2, // C
        explanation = "Almendros nutzte in 'Days of Heaven' konsequent das zwanzigminutige 'Magic Hour'-Licht bei Sonnenuntergang. Diese Entscheidung pragte den visuellen Stil des Films fundamental und brachte Almendros den Oscar.",
        difficulty = 3,
        funFact = "Da Almendros waehrend der Dreharbeiten anderweitig verpflichtet war, uebernahm Haskell Wexler einen Teil der Kameraarbeit, erhielt aber keinen eigenen Credit — eine bis heute diskutierte Ungerechtigkeit."
    ),

    // ── FILM SCORING TECHNIQUES ───────────────────────────────────────────────

    // Question 21 – Leitmotif in film music
    Question(
        categoryId = 4,
        questionText = "Das Leitmotiv-Konzept in der Filmmusik stammt aus der Oper. Welcher Komponist setzte es am systematischsten in seinen Filmpartituren ein?",
        answerA = "Ennio Morricone",
        answerB = "Bernard Herrmann",
        answerC = "John Williams",
        answerD = "Max Steiner",
        correctAnswer = 2, // C
        explanation = "John Williams ist beruehmt fuer seine konsequente Verwendung von Leitmotiven: In 'Star Wars' hat jede Hauptfigur ein eigenes Thema (Imperialer Marsch, Leia-Thema, Force-Thema etc.), das kompositorisch entwickelt wird.",
        difficulty = 3,
        funFact = "Max Steiner wird oft als 'Vater der Filmmusik' bezeichnet und war einer der ersten, der Leitmotive systematisch in Hollywood einsetzte — etwa in 'King Kong' (1933)."
    ),

    // Question 22 – Bernard Herrmann and Psycho
    Question(
        categoryId = 4,
        questionText = "Bernard Herrmanns Musik fuer Hitchcocks 'Psycho' (1960) besteht ausschliesslich aus welcher Besetzung?",
        answerA = "Streichorchester ohne Blaeser und Schlagzeug",
        answerB = "Klaviersolo mit Schlagzeugbegleitung",
        answerC = "Kammerorchester mit ausschliesslich Holzblaeser",
        answerD = "Synthesizer und elektronische Instrumente",
        correctAnswer = 0, // A
        explanation = "Herrmanns Partitur fuer 'Psycho' ist legendaer, weil sie ausschliesslich fuer Streichorchester geschrieben wurde — keine Blaeser, kein Schlagzeug, kein Klavier. Dies verleiht der Musik ihren einzigartigen, schrillen und nervoesen Charakter.",
        difficulty = 3,
        funFact = "Alfred Hitchcock wollte 'Psycho' urspruenglich ohne Musikbegleitung veroeffentlichen. Herrmann ueberzeugte ihn, indem er die fertige Musik unter die Duschszene legte — Hitchcock verdoppelte daraufhin Herrmanns Honorar."
    ),

    // Question 23 – Ennio Morricone and spaghetti western
    Question(
        categoryId = 4,
        questionText = "Ennio Morricone entwickelte fuer Sergio Leones Western einen unverwechselbaren Klang durch den Einsatz ungewoehnlicher Klangelemente. Welche Instrumente und Effekte sind charakteristisch fuer 'Zwei glorreiche Halunken' (1966)?",
        answerA = "Elektrische Gitarre, Pfeifstimme, Marimba und Schussgeraeusche",
        answerB = "Akkordeon, Trompete und Chor",
        answerC = "Synthesizer, Bass-Harmonika und Kirchenorgel",
        answerD = "Banjo, Country-Gitarre und Mundharmonika",
        correctAnswer = 0, // A
        explanation = "Morricones Klangpalette in den Leone-Western umfasst Elektrische Gitarre, Pfeifstimme (oft von Alessandro Alessandroni), Marimba, menschliche Laute und reale Effekte wie Peitschenknall und Schussgeraeusche als musikalische Elemente.",
        difficulty = 3,
        funFact = "Morricone und Leone arbeiteten nach einem ungewoehnlichen Prinzip: Die Musik wurde VOR den Dreharbeiten komponiert, und Leone drehte nach dem Rhythmus der Musik — das Gegenteil des Hollywood-Standards."
    ),

    // Question 24 – Hans Zimmer and hybrid scoring
    Question(
        categoryId = 4,
        questionText = "Hans Zimmers Musik fuer 'Inception' (2010) basiert auf einer verlangsamten Version welches bekannten Chansons?",
        answerA = "'La Vie en Rose' von Edith Piaf",
        answerB = "'Non, je ne regrette rien' von Edith Piaf",
        answerC = "'Sous le ciel de Paris' von Yves Montand",
        answerD = "'Que sera, sera' von Doris Day",
        correctAnswer = 1, // B
        explanation = "Zimmers 'Time'-Leitmotiv und die gesamte 'Inception'-Partitur sind um extreme Verlangsamungen von 'Non, je ne regrette rien' (Edith Piaf) herum aufgebaut — passend zum Traum-in-Traum-Konzept, in dem Zeit sich dehnt.",
        difficulty = 3,
        funFact = "Im Film spielt das Lied selbst eine Rolle als akustisches Signal zwischen den Traumebenen. Zimmer dehnte es um den Faktor 400, was aus den wenigen Sekunden des Liedes die gesamte Filmdauer ergibt."
    ),

    // Question 25 – Temp tracks and film music
    Question(
        categoryId = 4,
        questionText = "Was bezeichnet der Begriff 'Temp Track' im Filmproduktionsprozess, und welches beruhmte Beispiel fuehrte zu einem Plagiatsstreit?",
        answerA = "Vorlaeufige Tonspur mit Platzhalterdialog; John Williams klagte wegen '2001' gegen George Lucas",
        answerB = "Vorlaeufige Musikunterlegung im Schnitt; Jerry Goldsmith klagte, weil sein Temp-Track fuer 'Alien' fast unveraendert in 'Braveheart' verwendet wurde",
        answerC = "Musikalischer Platzhalter aus existierenden Werken, der Komponisten unbewusst beeinflusst; bekanntes Beispiel ist die Aehnlichkeit von 'Gladiator' zu Goldsmith-Werken",
        answerD = "Originaltonspur die spaeter geloescht wird; Morricone verlor deswegen einen Vertrag mit Universal",
        correctAnswer = 2, // C
        explanation = "Ein Temp Track ist eine vorlaeufige Musikunterlegung aus bestehenden Werken, die Regisseure waehrend des Schnitts nutzen. Das Problem: Regisseure verlieben sich oft in den Temp Track, was Komponisten unbewusst in diese Richtung drangt — oder zu offensichtlichen Aehnlichkeiten fuehrt.",
        difficulty = 3,
        funFact = "Hans Zimmer bezeichnet Temp Tracks als 'den Feind der Originalitaet in der Filmmusik'. Stanley Kubrick hingegen nutzte Temp Tracks aus klassischen Werken und liess sie bewusst im fertigen Film — was zu einer legendaeren Kollaboration mit dem Konzertsaal wurde."
    ),

    // ── PRODUCTION DESIGN & ART DIRECTION ────────────────────────────────────

    // Question 26 – Ken Adam and James Bond
    Question(
        categoryId = 4,
        questionText = "Welcher Production Designer schuf die ikonischen Kulissen der fruehen James-Bond-Filme, darunter den vulkanischen Hauptsitz in 'You Only Live Twice' (1967)?",
        answerA = "Richard Sylbert",
        answerB = "Dean Tavoularis",
        answerC = "Ken Adam",
        answerD = "Syd Mead",
        correctAnswer = 2, // C
        explanation = "Sir Ken Adam (buergerlich Klaus Hugo Adam) schuf als Production Designer den unverwechselbaren 'Bond-Look': riesige funktionale Verbrecherhauptquartiere, futuristische Interieurs und den legendaeren Kontrollraum in Goldfinger. Er gewann zwei Oscars.",
        difficulty = 3,
        funFact = "Ken Adam war auch Production Designer fuer Stanley Kubricks 'Dr. Seltsam' (1964) und schuf den ikonischen 'War Room' — einen der beruehmt komentierten Sets der Filmgeschichte."
    ),

    // Question 27 – William Cameron Menzies
    Question(
        categoryId = 4,
        questionText = "Wer erhielt 1939 erstmals in der Filmgeschichte den Titel 'Production Designer' — eine Bezeichnung, die er selbst mitmachte zu pragen?",
        answerA = "Cedric Gibbons",
        answerB = "William Cameron Menzies",
        answerC = "Hans Dreier",
        answerD = "Van Nest Polglase",
        correctAnswer = 1, // B
        explanation = "William Cameron Menzies erhielt fuer 'Vom Winde verweht' (1939) erstmals offiziell den Credit 'Production Designer', um seine ausserordentliche visuelle Gesamtverantwortung zu wuerdigen, die ueber klassische Art Direction hinausging.",
        difficulty = 3,
        funFact = "Menzies entwarf fuer 'Vom Winde verweht' eine komplette visuelle Bibel mit Farbkonzept und Lichtplanung fuer jede einzelne Szene — ein damals revolutionaerer Ansatz."
    ),

    // Question 28 – Blade Runner production design
    Question(
        categoryId = 4,
        questionText = "Welches Kunstkonzept pragte die Aesthetik von 'Blade Runner' (1982) und wurde als 'Retro-Futurismus' oder 'Used Future' bekannt?",
        answerA = "Eine sterile, makellose Hochglanzzukunft nach dem Vorbild von Stanley Kubrick",
        answerB = "Eine abgenutzte, verfallende Zukunft wo alte Technologie mit neuer koexistiert",
        answerC = "Ein mittelalterliches Ambiente mit Hochteichnologie-Einschluessen",
        answerD = "Eine organisch-bionische Welt nach dem Vorbild der Art Nouveau",
        correctAnswer = 1, // B
        explanation = "Das 'Used Future' oder 'Retro-Futurismus'-Konzept in Blade Runner zeigt eine Zukunft, in der alte und neue Technologien nebeneinander existieren, alles abgenutzt und repariert ist. Production Designer Lawrence G. Paull und Visual Consultant Syd Mead priagten diesen Look.",
        difficulty = 3,
        funFact = "Ridley Scott orientierte sich bei der Blade-Runner-Aesthetik stark an Moebius/Jean Girauts Comicwelt und dem Megazine 'Heavy Metal'. Syd Meads Industriedesign-Hintergrund verlieh dem Film seine technische Glaubwuerdigkeit."
    ),

    // Question 29 – Cedric Gibbons and Oscar statuette
    Question(
        categoryId = 4,
        questionText = "Cedric Gibbons, langjaehrer Art Director bei MGM, entwarf neben zahllosen Filmsets auch ein ikonisches Objekt. Welches?",
        answerA = "Das Goldene Loewen-Symbol der Filmgesellschaft",
        answerB = "Die Oscar-Statuette",
        answerC = "Das Hollywood-Sign auf dem Griffith-Park",
        answerD = "Das Stern-Logo auf dem Hollywood Walk of Fame",
        correctAnswer = 1, // B
        explanation = "Cedric Gibbons entwarf 1928 die Oscar-Statuette, die bis heute unveraendert verwendet wird. Gibbons gewann selbst elf Oscars fuer Art Direction — einen Rekord, der bis heute steht.",
        difficulty = 3,
        funFact = "Die Oscar-Statuette zeigt einen Ritter, der auf einer Filmspule steht und ein Schwert haelt. Es gibt verschiedene Theorien zur Figur-Vorlage — am haeufigsten wird Emilio Fernandez als Modell genannt."
    ),

    // Question 30 – Eugenio Zanetti
    Question(
        categoryId = 4,
        questionText = "Welcher Film gewann 1996 den Oscar fuer das beste Szenenbild fuer eine nahezu vollstaendig im Studio rekonstruierte viktorianische Welt?",
        answerA = "'Sense and Sensibility' (1995)",
        answerB = "'Restoration' (1995)",
        answerC = "'The Age of Innocence' (1993)",
        answerD = "'Interview with the Vampire' (1994)",
        correctAnswer = 1, // B
        explanation = "Eugenio Zanetti gewann den Oscar fuer das Szenenbild in 'Restoration' (1995, Regie: Michael Hoffman), einem Film ueber das englische Koenigshaus der Restaurationszeit. Zanettis handgemalte, theatralische Kulissenarbeit gewann den Preis.",
        difficulty = 3,
        funFact = "Zanetti hatte keine klassische Film-Ausbildung — er begann als Maurer und bildete sich autodidaktisch zum Production Designer aus."
    ),

    // ── FAMOUS LONG TAKES & TRACKING SHOTS ───────────────────────────────────

    // Question 31 – Orson Welles Touch of Evil opening
    Question(
        categoryId = 4,
        questionText = "Die berihmte Eroefffnungseinstellung von Orson Welles' 'Touch of Evil' (1958) ist ein langer, ununterbrochener Kameraschwenk. Wie lang dauert diese Einstellung ungefaehr?",
        answerA = "1 Minute 20 Sekunden",
        answerB = "3 Minuten 20 Sekunden",
        answerC = "5 Minuten 40 Sekunden",
        answerD = "8 Minuten 10 Sekunden",
        correctAnswer = 1, // B
        explanation = "Der Eroefffnungs-Plansequenz von 'Touch of Evil' dauert ca. 3 Minuten 20 Sekunden und zeigt in einem einzigen Schwenk, wie eine Bombe platziert wird, Autos durch Strassenzuege folgen und die Explosion erfolgt.",
        difficulty = 3,
        funFact = "Welles drehte diese Einstellung nachts auf dem Paramount-Gelaende. Der Kran-gesteuerte Schwenk erforderte praezise Koordination von Darstellern, Fahrzeugen und Kamerabewegung — komplett ohne Schnitt."
    ),

    // Question 32 – Children of Men tracking shot
    Question(
        categoryId = 4,
        questionText = "In Alfonso Cuarons 'Children of Men' (2006) gibt es eine berihmte, blutbefleckte Kameralinse in einer Kriegszone. Wie viele Tage Vorbereitung benoetigt die laengste solche Einstellung, und wie lang ist sie?",
        answerA = "3 Tage, 4 Minuten",
        answerB = "5 Tage, 8 Minuten",
        answerC = "14 Tage, 6 Minuten",
        answerD = "21 Tage, 10 Minuten",
        correctAnswer = 1, // B
        explanation = "Die laengste Sequenz in 'Children of Men' — die Kampfsequenz in Bexhill — dauert etwa 8 Minuten und war nach Angaben von Kameramann Emmanuel Lubezki etwa 5 Tage in Vorbereitung und Umsetzung.",
        difficulty = 3,
        funFact = "Die Blutspritzer auf der Kameralinse gegen Ende der Einstellung waren ein Unfall — eine Pyrotechnik-Ladung traf die Linse. Cuaron und Lubezki entschieden sich, die Aufnahme zu behalten, da sie der Szene extreme Authentizitaet verlieh."
    ),

    // Question 33 – Goodfellas Copa shot
    Question(
        categoryId = 4,
        questionText = "Martin Scorseses berihmter Steadicam-Shot in 'Goodfellas' (1990) folgt Henry und Karen Hill durch den Hintereingang des Copacabana-Nachtclubs. Wie viele Meter legt die Kamera dabei zurueck?",
        answerA = "Ca. 50 Meter",
        answerB = "Ca. 100 Meter",
        answerC = "Ca. 180 Meter",
        answerD = "Ca. 280 Meter",
        correctAnswer = 1, // B
        explanation = "Der 'Copa Shot' in 'Goodfellas' ist etwa 100 Meter lang und dauert ca. 3 Minuten. Steadicam-Operator Larry McConkey fuegte Ray Liotta und Lorraine Bracco durch Kueche, Flure und schliesslich in den Club.",
        difficulty = 3,
        funFact = "Scorsese nutzte die lange Einstellung bewusst, um Henry Hills Selbstsicherheit und Macht zu visualisieren — die Kamera folgt ihm wie ein bewundernder Schatten durch alle Tuersteher und Hindernisse hindurch."
    ),

    // Question 34 – Russian Ark (Russkiy kovcheg)
    Question(
        categoryId = 4,
        questionText = "Alexander Sokurovs 'Russian Ark' (2002) ist weltweit einzigartig durch eine technische Besonderheit. Was ist sie?",
        answerA = "Er wurde vollstaendig als Animation produziert und dann real-texturiert",
        answerB = "Er ist der erste Spielfilm, der als einzige durchgehende Einstellung ohne jeden Schnitt gedreht wurde",
        answerC = "Er kombiniert Filmmaterial aus 80 verschiedenen Jahren der Filmgeschichte",
        answerD = "Er wurde ausschliesslich mit Handys und Consumer-Kameras gedreht",
        correctAnswer = 1, // B
        explanation = "Alexander Sokurovs 'Russian Ark' ist der erste Langspielfilm der Kinogeschichte, der als einzige durchgehende Einstellung ohne jeden Schnitt gedreht wurde — 96 Minuten, 33 Raeume der Eremitage, 867 Schauspieler, eine einzige Kamerabewegung.",
        difficulty = 3,
        funFact = "Der Film wurde auf einer Betacam SX-Kamera auf ein einziges HD-Speichergeraet aufgenommen. Das Team hatte nur einen Tag in der Eremitage — beim dritten Versuch klappte es."
    ),

    // Question 35 – Birdman technical illusion
    Question(
        categoryId = 4,
        questionText = "Alejandro Gonzalez Inarritus 'Birdman' (2014) erweckt den Eindruck, als One-Take-Film gedreht worden zu sein. Wie viele tatsaechliche Einstellungen wurden fuer den finalen Film verwendet und wie wurden die Uebergaenge kaschiert?",
        answerA = "Ca. 500 kurze Einstellungen, verbunden durch digitale Uebergaenge die klassische Hollywood-Schnitte imitieren",
        answerB = "Ca. 30 lange Einstellungen, verbunden durch versteckte digitale Uebergaenge z.B. in dunklen Teilen des Bildes",
        answerC = "Genau 16 Einstellungen, verbunden durch Match Cuts im klassischen Sinne",
        answerD = "Eine einzige Einstellung, unterbrochen durch drei versteckte Montagen",
        correctAnswer = 1, // B
        explanation = "Kameramann Emmanuel Lubezki drehte 'Birdman' in langen Einstellungen, die dann durch digitale Uebergaenge in Dunkelzonen, schwarzen Flaechen oder Schwenks zu einer nahtlosen Kette verbunden wurden — ca. 30 reale Einstellungen.",
        difficulty = 3,
        funFact = "Lubezki und Inarritu inspierierten sich an den langen Theaterproben der Hauptdarsteller — die technische Idee entstand aus dem Wunsch, den Theaterrhythmus ins Kino zu uebertragen."
    ),

    // ── FILM RESTORATION & PRESERVATION ─────────────────────────────────────

    // Question 36 – Nitrate film fire risk
    Question(
        categoryId = 4,
        questionText = "Nitratfilme des fruehen Kinos galten als hochexplosiv. Bei welcher Temperatur beginnt Nitratfilm selbststaendig zu verbrennen, und welche beruhmte Katastrophe macht dieses Risiko deutlich?",
        answerA = "Ab 40 Grad Celsius; der Brand des MGM-Archivlagers in Culver City 1965",
        answerB = "Ab 120 Grad Celsius; der Brand im Filmarchiv der Cinematheque Francaise 1959",
        answerC = "Ab 69 Grad Celsius; der Brand in Vincennes vernichtete 80% des Lumiere-Archivs 1903",
        answerD = "Ab 35 Grad Celsius; der Brand des Fox-Archivs in Little Ferry 1937",
        correctAnswer = 3, // D
        explanation = "Nitratfilm beginnt ab ca. 35-40 Grad Celsius zu verbrennen und brennt auch ohne Sauerstoffzufuhr. Der Brand des Fox Film-Archivs in Little Ferry, New Jersey (1937) vernichtete unwiederbringlich den Grossteil des fruehen Fox-Filmbestands.",
        difficulty = 3,
        funFact = "Schätzungsweise 70-80% aller Stummfilme sind verloren gegangen — durch Brände, bewusste Vernichtung und den Einschmelzprozess zur Silbergewinnung. Nitratfilm enthalt erhebliche Mengen an wertvollem Silber."
    ),

    // Question 37 – Martin Scorsese Film Foundation
    Question(
        categoryId = 4,
        questionText = "Martin Scorsese gruendete 1990 eine Stiftung zur Filmrestaurierung und -bewahrung. Wie heisst sie?",
        answerA = "The Cinema Legacy Foundation",
        answerB = "The Film Foundation",
        answerC = "Scorsese Preservation Trust",
        answerD = "World Cinema Project",
        correctAnswer = 1, // B
        explanation = "Martin Scorsese gruendete 1990 'The Film Foundation' mit dem Ziel, gefaehrdete Filmwerke zu restaurieren und zu bewahren. Bis heute wurden ueber 900 Filme restauriert.",
        difficulty = 3,
        funFact = "Scorsese erweiterte das Projekt spaeter um das 'World Cinema Project', das sich speziell der Restaurierung von Filmen aus Afrika, Asien, Lateinamerika und dem Nahen Osten widmet — Regionen, in denen Filmbewahrung besonders schwierig ist."
    ),

    // Question 38 – Metropolis restoration
    Question(
        categoryId = 4,
        questionText = "Fritz Langs 'Metropolis' (1927) galt lange als unvollstaendig. In welchem Land wurden 2008 die bis dato laengsten bisher unbekannten Filmrollen entdeckt, die den Film um ca. 25 Minuten verlaengerten?",
        answerA = "Deutschland (Bundesarchiv Berlin)",
        answerB = "USA (Library of Congress)",
        answerC = "Argentinien (Museo del Cine Buenos Aires)",
        answerD = "Russland (Gosfilmofond)",
        correctAnswer = 2, // C
        explanation = "2008 entdeckte Paula Felix-Didier im Museo del Cine in Buenos Aires eine 16mm-Kopie von 'Metropolis', die Szenen enthielt, die seit der Uroauffuehrung 1927 als verloren galten. Diese Restaurierung wurde 2010 der Oeffentlichkeit praesentiert.",
        difficulty = 3,
        funFact = "'Metropolis' war der erste Film, der ins UNESCO-Weltdokumentenerbe aufgenommen wurde (2001). Die Buenos-Aires-Kopie war in einem privaten Filmclub aufgetaucht und jahrzehntelang nicht als das Original erkannt worden."
    ),

    // Question 39 – 4K vs. photochemical restoration
    Question(
        categoryId = 4,
        questionText = "Was bezeichnet der Begriff 'Wet Gate Printing' bei der analogen Filmrestaurierung?",
        answerA = "Ein digitales Verfahren zur Kratzerentfernung per KI-Algorithmus",
        answerB = "Ein Verfahren, bei dem der Film waehrend des Kopierens in eine Fluessigkeit getaucht wird, um Kratzer optisch zu minimieren",
        answerC = "Ein chemisches Bad, das verblasste Farben in alten Nitrat-Farbfilmen reaktiviert",
        answerD = "Eine Feuchtigkeitskammer zur Stabilisierung fragiler Filmstreifen vor dem Digitalisieren",
        correctAnswer = 1, // B
        explanation = "Beim 'Wet Gate Printing' wird der Filmstreifen waehrend des Kopierens in eine Fluessigkeit mit aehnlichem Brechungsindex wie das Filmmaterial getaucht. Dadurch werden Kratzer optisch 'aufgefuellt' und im Abzug unsichtbar.",
        difficulty = 3,
        funFact = "Wet Gate ist eine der aeltesten analogen Restaurierungstechniken und wird auch heute noch parallel zu digitalen Verfahren eingesetzt — besonders wenn das physische Original erhalten und durch Digitalisierung nicht beschaedigt werden soll."
    ),

    // Question 40 – George Eastman House
    Question(
        categoryId = 4,
        questionText = "Welches Archiv in Rochester, New York gilt als eines der aeltesten und bedeutendsten Filmarchive der Welt und bewahrt ueber 30.000 Filmtitel?",
        answerA = "Museum of Modern Art Film Archive",
        answerB = "George Eastman Museum",
        answerC = "Library of Congress Film Collection",
        answerD = "Academy Film Archive",
        correctAnswer = 1, // B
        explanation = "Das George Eastman Museum in Rochester, NY (ehemals George Eastman House) wurde 1947 gegruendet und beherbergt eine der wichtigsten Filmsammlungen der Welt mit ueber 30.000 Titeln sowie bedeutenden Sammlungen von Fotografie und Filmequipment.",
        difficulty = 3,
        funFact = "George Eastman, Gruender von Kodak, wohnte in dem Haus, das heute das Museum beherbergt. Es gibt eine tragische Ironie: Eastman erschoss sich 1932 mit einer Notiz: 'Meine Arbeit ist getan. Worauf warte ich?'"
    ),

    // ── MIXED ADVANCED TOPICS ────────────────────────────────────────────────

    // Question 41 – Cahiers du Cinema top 10
    Question(
        categoryId = 4,
        questionText = "Die Zeitschrift 'Cahiers du Cinema' waehlt alle zehn Jahre die besten Filme aller Zeiten. Welcher Film fuertet seit Jahrzehnten die meisten solcher Bestenlisten an?",
        answerA = "'Citizen Kane' (1941) von Orson Welles",
        answerB = "'Vertigo' (1958) von Alfred Hitchcock",
        answerC = "'Tokyo Story' (1953) von Yasujiro Ozu",
        answerD = "'2001: A Space Odyssey' (1968) von Stanley Kubrick",
        correctAnswer = 0, // A
        explanation = "'Citizen Kane' fueerte die Sight & Sound-Liste (vergleichbar mit Cahiers) von 1962 bis 2012 ohne Unterbrechung an, bis 'Vertigo' den Spitzenplatz uebernahm. Heute teilen sich beide Filme den Status als meistdiskutierte Kandidaten.",
        difficulty = 3,
        funFact = "In der Sight & Sound-Liste 2022 wurde Chantal Akermans 'Jeanne Dielman' (1975) ueberraschend auf Platz 1 gewaehlt — ein Film, der die Tagesroutine einer Hausfrau in Echtzeit zeigt."
    ),

    // Question 42 – Panavision and anamorphic lenses
    Question(
        categoryId = 4,
        questionText = "Das anamorphische Filmformat komprimiert das Bild horizontal beim Drehen und dehnt es bei der Projektion wieder aus. Welches Seitenverhaeltnis ergibt sich bei klassischem anamorphischem 35mm-Film?",
        answerA = "1.85:1",
        answerB = "2.35:1 bis 2.39:1",
        answerC = "1.66:1",
        answerD = "2.75:1",
        correctAnswer = 1, // B
        explanation = "Das klassische anamorphische Format ergibt ein Seitenverhaeltnis von etwa 2.39:1 (frueher als 2.35:1 angegeben). Dies ist das am weitesten verbreitete 'Cinemascope'-Format und wird mit charakteristischen horizontalen Lens Flares assoziiert.",
        difficulty = 3,
        funFact = "Der Hauptvorteil des anamorphischen Formats ist nicht nur das weite Bild, sondern auch die charakteristischen optischen Artefakte: ovale Bokeh-Kreise, horizontale Lens-Flares und ein bestimmtes 'Rollen' in der Tiefenschaerfe."
    ),

    // Question 43 – The Red Camera and digital cinema
    Question(
        categoryId = 4,
        questionText = "Welche Aufloesung bot die 'RED ONE'-Kamera, die 2007 als erste erschwingliche Digitalfilmkamera den Markt erschuetterte?",
        answerA = "2K (2048 x 1080 Pixel)",
        answerB = "4K (4096 x 2304 Pixel)",
        answerC = "6K (6144 x 3240 Pixel)",
        answerD = "8K (8192 x 4320 Pixel)",
        correctAnswer = 1, // B
        explanation = "Die RED ONE bot bei ihrer Einfuehrung 2007/2008 eine maximale Aufloesung von 4K (4096 x 2304 Pixel) — zu einem Preis, der weit unter professionellen Filmkameras lag und die Demokratisierung der Hochaufloesung einleitete.",
        difficulty = 3,
        funFact = "RED-Gruender Jim Jannard hatte sein Vermoegen mit der Oakley-Sonnenbrillenmarke gemacht und investierte es in die Entwicklung der RED-Kamera. 'District 9' (2009) war einer der ersten grossen Hollywoodfilme, der mit RED gedreht wurde."
    ),

    // Question 44 – Walter Murch and sound design
    Question(
        categoryId = 4,
        questionText = "Walter Murch, der Schnitt-Meister von 'Apocalypse Now' und 'The Godfather Part II', praegte welchen Begriff, der heute als Standard der Filmpostproduktion gilt?",
        answerA = "Foley Art",
        answerB = "Sound Design",
        answerC = "ADR (Automated Dialogue Replacement)",
        answerD = "Dolby Mix",
        correctAnswer = 1, // B
        explanation = "Walter Murch pragte den Begriff 'Sound Design' als eigenstaendige kuenstlerische Disziplin. Bei 'Apocalypse Now' (1979) erhielt er erstmals den Credit 'Sound Designer' und etablierte damit ein neues Berufsbild in der Filmbranche.",
        difficulty = 3,
        funFact = "Murch arbeitete nach einer eigenen Schnitt-Philosophie, der 'Rule of Six': Er priorisierte Emotion, Geschichte, Rhythmus, Blickrichtung, 2D-Bildachse und 3D-Raumachse in genau dieser Reihenfolge — Emotion ist wichtiger als jede technische Regel."
    ),

    // Question 45 – Kinetoscope and early film history
    Question(
        categoryId = 4,
        questionText = "Der Brüder-Lumiere-Konkurrent Thomas Edison entwickelte das Kinetoscope, ein Einzelbetrachter-Geraet fuer kurze Filmstreifen. Welches erste 'Filmstudio' liess Edison dafuer bauen?",
        answerA = "The Glass Box (New York, 1890)",
        answerB = "Black Maria (West Orange, New Jersey, 1893)",
        answerC = "The Edison Oval (Chicago, 1891)",
        answerD = "Kinetograph Hall (London, 1894)",
        correctAnswer = 1, // B
        explanation = "Thomas Edisons erstes Filmstudio, die 'Black Maria' (offiziell Kinetographic Theater), wurde 1893 in West Orange, New Jersey gebaut. Das teerbedeckte Gebaeude rotierte auf Schienen, um der Sonne folgen zu koennen.",
        difficulty = 3,
        funFact = "Die 'Black Maria' war nur 15 x 7 Meter gross und hatte kein Dach — Tageslicht war die einzige Beleuchtungsquelle. Das rotierbare Design war eine Notloesung fuer das Problem der sich aendernden Sonneneinfallswinkel."
    ),

    // Question 46 – IMAX format and camera
    Question(
        categoryId = 4,
        questionText = "Das IMAX-Format nutzt 70mm-Film, der horizontal laeuft statt vertikal wie im Standard-Format. Wie gross ist ein einzelnes IMAX-Filmbild im Vergleich zu einem Standard-35mm-Bild?",
        answerA = "Ca. 3x so gross",
        answerB = "Ca. 6x so gross",
        answerC = "Ca. 10x so gross",
        answerD = "Ca. 15x so gross",
        correctAnswer = 2, // C
        explanation = "Ein IMAX-Filmbild auf 70mm-Horizontalfilm ist ungefaehr 10 Mal so gross wie ein Standard-35mm-Bild. Dies ermoeglicht aussergewoehnliche Bildschaerfe bei grossen Projektionsflaechen von bis zu 40 Metern Breite.",
        difficulty = 3,
        funFact = "Christopher Nolan ist der prominenteste Verfechter von echtem 65mm-IMAX. 'The Dark Knight' (2008) war der erste Spielfilm mit laengeren IMAX-Sequenzen — seitdem wird 'IMAX' oft auch fuer digitale Aufwertungen gebraucht, was Nolan als Qualitaetsverwaesserung kritisiert."
    ),

    // Question 47 – Dogville and theatrical staging
    Question(
        categoryId = 4,
        questionText = "Lars von Triers 'Dogville' (2003) wurde in einem speziellen theatralischen Setting gedreht, das minimales Production Design nutzt. Wie wurde das gesamte Stadtbild dargestellt?",
        answerA = "Als Schwarzweissfilm mit digital eingefuegten Hintergrundelementen",
        answerB = "Als Grundriss auf einem schwarzen Studioboden, mit Gebaeuden nur durch Kreidelinien und Beschriftungen angedeutet",
        answerC = "Als vollstaendig leere Buehne, auf der Requisiten nur von Schauspielern angedeutet wurden",
        answerD = "In einem alten verlassenen Industriegebaeude, das minimal als Kulisse angepasst wurde",
        correctAnswer = 1, // B
        explanation = "In 'Dogville' ist die fiktive Stadt nur als Grundriss auf einem schwarzen Studioboden skizziert: Strassen, Haeuser und Gegenstaende sind durch Kreidelinien und Beschriftungen markiert. Die Schauspieler agieren als waere die Kulisse real.",
        difficulty = 3,
        funFact = "Von Trier wurde durch Bertolt Brechts Konzept des Verfremdungseffekts inspiriert. Die radikale Reduktion des Buehnenbillds sollte die Aufmerksamkeit vollstaendig auf das Spiel und die Geschichte lenken."
    ),

    // Question 48 – Film grain vs digital noise
    Question(
        categoryId = 4,
        questionText = "Was ist der technische Unterschied zwischen 'Filmkorn' (Film Grain) und 'digitalem Rauschen' (Digital Noise) in der Bildcharakteristik?",
        answerA = "Filmkorn tritt in hohen Kontrasten auf, digitales Rauschen in Schatten",
        answerB = "Filmkorn ist organisch-zufaellig und zeitlich variierend, digitales Rauschen ist gleichfoermig und strukturiert",
        answerC = "Filmkorn ist ausschliesslich bei Schwarz-Weiss-Film vorhanden, digitales Rauschen nur bei Farbfilm",
        answerD = "Es gibt keinen sichtbaren Unterschied — beide erscheinen dem Auge identisch",
        correctAnswer = 1, // B
        explanation = "Filmkorn entsteht durch zufaellig verteilte Silberhalogenid-Kristalle und variiert organisch von Bild zu Bild. Digitales Rauschen entsteht durch Sensorrauschen und erscheint statischer, gleichmaessiger und oft farbig — visuell weniger angenehm als Filmkorn.",
        difficulty = 3,
        funFact = "Viele moderne Filme und Serien fueegn nachtraeglich kuenstliches Filmkorn hinzu (z.B. per DaVinci Resolve), um dem sterilen digitalen Bild eine organischere Qualitaet zu geben — ein Praeferenz, die von Roger Deakins und anderen Meisterkameraleuten unterstuetzt wird."
    ),

    // Question 49 – Three-point lighting
    Question(
        categoryId = 4,
        questionText = "Was ist der Unterschied zwischen 'High-Key-Lighting' und 'Low-Key-Lighting' in der Filmbeleuchtung?",
        answerA = "High-Key nutzt blaues Licht, Low-Key warmes Licht",
        answerB = "High-Key hat hohe Helligkeit und wenig Schatten (typisch Komoedien), Low-Key nutzt starke Kontraste und tiefe Schatten (typisch Film Noir)",
        answerC = "High-Key bedeutet die Hauptquelle ist hoch positioniert, Low-Key bedeutet sie kommt von unten",
        answerD = "High-Key ist ein Tageslichtformat, Low-Key ausschliesslich fuer Nachtaufnahmen",
        correctAnswer = 1, // B
        explanation = "High-Key-Beleuchtung erzeugt helles, gleichmaessiges Licht mit minimalem Schattenanteil — typisch fuer Komoedien und Werbung. Low-Key-Beleuchtung schafft starke Hell-Dunkel-Kontraste und tiefe Schatten — typisch fuer Film Noir, Thriller und Drama.",
        difficulty = 3,
        funFact = "Der klassische Film Noir verwendet Low-Key-Beleuchtung aus expressionistischen Schatten, Venetian-Blind-Muster durch Jalousien und stark gerichtetes Licht — direkt beeinflusst durch deutsche expressionistische Kameramanner wie Karl Freund und Fritz Lang."
    ),

    // Question 50 – Tarkovsky and poetic cinema
    Question(
        categoryId = 4,
        questionText = "Andrei Tarkovskij beschrieb sein Filmkonzept in seinem Buch 'Die versiegelte Zeit' durch eine spezifische Metapher. Was verstand er darunter?",
        answerA = "Film als Gemälde, das den Raum auf zwei Dimensionen reduziert",
        answerB = "Film als Skulptur aus Zeit, bei der der Regisseur Zeit wie ein Bildhauer formt und versiegelt",
        answerC = "Film als Musikpartiture, bei der Bilder wie Noten in einer Komposition angeordnet werden",
        answerD = "Film als Traum, der die Grenze zwischen Realitaet und Unterbewusstsein aufhebt",
        correctAnswer = 1, // B
        explanation = "Tarkovskij bezeichnete den Film in 'Die versiegelte Zeit' als 'Skulptur aus Zeit': Der Regisseur forme Zeit wie ein Bildhauer Ton — durch das Selektieren, Verwandeln und Versiegeln von Zeitstroemen in Bildern.",
        difficulty = 3,
        funFact = "Tarkovskijs Filme wie 'Andrej Rublev', 'Stalker' und 'Der Spiegel' sind fuer extrem lange, kontemplative Einstellungen bekannt. 'Stalker' (1979) musste nach einer Filmproduktionspanne komplett neu gedreht werden — das Originalmaterial war durch fehlerhafte Entwicklung zerstoert worden."
    )

)
