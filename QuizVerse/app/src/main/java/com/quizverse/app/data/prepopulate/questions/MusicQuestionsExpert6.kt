package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsExpert6(): List<Question> = listOf(

    // ── Kirchenmusik / Liturgie (7) ──────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Wie bezeichnet man den liturgischen Gesang, bei dem ein Vorsaenger und die Gemeinde (oder ein Chor) abwechselnd singen?",
        answerA = "Homophonie",
        answerB = "Antiphon",
        answerC = "Responsorium",
        answerD = "Conductus",
        correctAnswer = 2,
        explanation = "Ein Responsorium ist ein Wechselgesang zwischen Vorsaenger (Cantor) und Schola oder Gemeinde. Es unterscheidet sich von der Antiphon: Antiphonen rahmen Psalmen ein, waehrend Responsorien eigenstaendige liturgische Stuecke nach Lesungen sind.",
        difficulty = 4,
        funFact = "Das Grosse Responsorium (Responsorium prolixum) ist mit seinen aufwendigen Melismen ein Hochpunkt des gregorianischen Chorals. Im Officium, dem stundenliturgischen Stundengebet, folgt auf jede Lesung ein solches Responsorium."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Begriff bezeichnet den Teil der roemischen Messe, in dem Kyrie, Gloria, Credo, Sanctus/Benedictus und Agnus Dei zusammengefasst werden?",
        answerA = "Proprium",
        answerB = "Ordinarium",
        answerC = "Graduale",
        answerD = "Offertorium",
        correctAnswer = 1,
        explanation = "Das Ordinarium Missae umfasst die unveraenderlichen Texte der Messe: Kyrie, Gloria, Credo, Sanctus mit Benedictus und Agnus Dei. Diese Texte bilden die Grundlage fuer klassische Messvertonungen von Palestrina bis Beethoven und Brahms.",
        difficulty = 4,
        funFact = "Das Proprium dagegen enthaelt die von Kirchenjahr und Tagesliturgie abhaengigen Gesaenge (Introitus, Graduale, Alleluja, Offertorium, Communio). Komponisten vertonen fast ausschliesslich das Ordinarium, weil es fuer alle Sonntage gilt."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Kirchentonart entspricht einer Durtonleiter, die auf der zweiten Stufe beginnt (also D-E-F-G-A-H-C-D ohne Vorzeichen)?",
        answerA = "Dorisch",
        answerB = "Phrygisch",
        answerC = "Lydisch",
        answerD = "Mixolydisch",
        correctAnswer = 0,
        explanation = "Die dorische Kirchentonart (Modus I) entspricht einer Tonleiter von D bis D auf den weissen Klaviertasten. Sie klingt durch die grosse Sexte charakteristisch 'heller' als Moll und ist im Gregorianischen Choral sowie in Folk- und Jazzmusik weit verbreitet.",
        difficulty = 4,
        funFact = "Simon and Garfunkels 'Scarborough Fair' und das irische Volkslied 'What Shall We Do with the Drunken Sailor' stehen in der dorischen Tonart — ein Beleg dafuer, wie kirchliche Modi bis in die Popmusik nachwirken."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Komponist schrieb die 'Missa Solemnis' op. 123, die er selbst als sein groesstes Werk bezeichnete?",
        answerA = "Franz Schubert",
        answerB = "Ludwig van Beethoven",
        answerC = "Giovanni Pierluigi da Palestrina",
        answerD = "Anton Bruckner",
        correctAnswer = 1,
        explanation = "Ludwig van Beethoven komponierte die Missa Solemnis zwischen 1819 und 1823 fuer die Inthronisation seines Schuelers Erzherzog Rudolph als Erzbischof von Olmuetz. Das Werk uebersteigt mit seiner Dauer und Komplexitaet alle liturgisch praktikablen Massen.",
        difficulty = 4,
        funFact = "Beethoven schrieb auf die erste Seite des Manuskripts: 'Von Herzen — Moege es wieder — zu Herzen gehen!' Die Urauffuehrung der vollstaendigen Messe fand 1824 in St. Petersburg statt, nicht in Wien."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Gesangsform des roemischen Ritus besteht aus einem Psalmton mit einer melodischen Formel (Intonation, Tenor, Mediante, Terminatio)?",
        answerA = "Tropos",
        answerB = "Sequence",
        answerC = "Psalmton (Psalmodie)",
        answerD = "Melisma",
        correctAnswer = 2,
        explanation = "Die Psalmodie ist die strukturierte Rezitation der Psalmen auf einem Rezitationston (Tenor). Jeder der acht Psalmtoene folgt demselben Schema: Intonation beim ersten Vers, Rezitation auf dem Tenor, Mediante als Halbezeile, Terminatio als Abschlussformel.",
        difficulty = 4,
        funFact = "Die acht Psalmtoene entsprechen den acht Kirchentonaerten. Bei der Auswahl des Psalmtons fuer eine Antiphon ist die Terminatio entscheidend: Sie muss melodisch zur Anfangsnote der folgenden Antiphon passen (Differenz)."
    ),

    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Falsobordone' in der Renaissance-Kirchenmusik?",
        answerA = "Eine mehrstimmige Improvisation auf einem Cantus firmus in der Oberstimme",
        answerB = "Eine Technik, bei der Paralleldreiklaenge einen Psalmton harmonisieren",
        answerC = "Ein einstimmiger ornamentierter Choral fuer Solosopran",
        answerD = "Ein Bordun-Bass unter einem Orgelpunkt",
        correctAnswer = 1,
        explanation = "Falsobordone (ital.: 'falscher Burdun') bezeichnet eine Harmonisierungstechnik, bei der Psalmen in schlichten, meist parallelen Dreiklaengen vorgetragen werden. Die Melodie liegt in der Oberstimme, waehrend die anderen Stimmen Akkordtransposition begleiten.",
        difficulty = 4,
        funFact = "Der Falsobordone entwickelte sich im 15. Jahrhundert aus dem englischen Fauxbourdon. Spaeter wuchs er zu improvisierten Choralsaetzen und beeinflusste massgeblich die venezianische Polychorie-Tradition."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Konzil im 16. Jahrhundert legte verbindliche Richtlinien fuer die katholische Kirchenmusik fest und beauftragte Palestrina faktisch mit einer Reform des gregorianischen Chorals?",
        answerA = "Konzil von Konstanz (1414-1418)",
        answerB = "Konzil von Basel (1431-1449)",
        answerC = "Tridentinisches Konzil (1545-1563)",
        answerD = "Laterankonzil V (1512-1517)",
        correctAnswer = 2,
        explanation = "Das Konzil von Trient verabschiedete in seiner letzten Sitzungsperiode Dekrete zur Kirchenmusik: Weltliche Melodien als Cantus firmus und unverstaendliche Polyphonie sollten vermieden werden. Palestrina gilt als Musterkomponist, der diese Anforderungen erfuellte.",
        difficulty = 4,
        funFact = "Die Legende, Palestrina habe mit seiner Missa Papae Marcelli das Konzil ueberzeugt, mehrstimmige Musik nicht zu verbieten, ist historisch unbelegt — aber bis heute in Musikgeschichtsbuechern verbreitet."
    ),

    // ── Blasorchester-Repertoire (7) ─────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welcher US-amerikanische Komponist gilt als 'March King' und schrieb ueber 130 Maersche, darunter 'The Stars and Stripes Forever'?",
        answerA = "Edwin Franko Goldman",
        answerB = "John Philip Sousa",
        answerC = "Karl King",
        answerD = "Henry Fillmore",
        correctAnswer = 1,
        explanation = "John Philip Sousa (1854-1932) dirigierte die US-Marine-Band und gruendete spaeter seine eigene Konzertband. 'The Stars and Stripes Forever' (1896) ist der offizielle Marsch der Vereinigten Staaten und gilt als Inbegriff des amerikanischen Konzertmarsches.",
        difficulty = 4,
        funFact = "Sousa lehnte die Erfindung der Schallplatte zeitlebens ab — er befuerchtete, aufgezeichnete Musik wuerde lebendige Konzerte verdraengen und 'Canned Music' produzieren. Der Begriff stammt tatsaechlich von ihm."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Werk von Alfred Reed gilt als eines der bedeutendsten Originalwerke fuer Blasorchester und ist in drei Saetzen gegliedert, inspiriert von der spanischen Kultur?",
        answerA = "Festive Overture",
        answerB = "Armenian Dances",
        answerC = "A Festival Prelude",
        answerD = "Russian Christmas Music",
        correctAnswer = 1,
        explanation = "Alfred Reeds 'Armenian Dances' (1972-1976, in vier Teilen) gehoert zu den meistgespielten Werken im Blasorchester-Repertoire. Es basiert auf armenischen Volksmelodien, die Reed aus einer Sammlung von Gomidas Vartabed bearbeitete — nicht auf spanischer Kultur.",
        difficulty = 4,
        funFact = "Alfred Reed stuetzte sich auf das Notenbuch 'Armenian Folk Songs and Dances' von Komitas Vardapet (Gomidas). Der erste Teil der Armenian Dances entstand als Auftragswerk fuer das Florida State University Symphonic Band."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Besetzung unterscheidet ein 'Sinfonisches Blasorchester' typischerweise von einer 'Brass Band' britischer Tradition?",
        answerA = "Das Sinfonische Blasorchester hat keine Schlaginstrumente",
        answerB = "Die Brass Band besteht ausschliesslich aus Blechblaesern und Schlagwerk, das Blasorchester zusaetzlich aus Holzblaesern",
        answerC = "Die Brass Band enthaelt Oboen und Fagotte, das Blasorchester nicht",
        answerD = "Beide Ensembles sind identisch besetzt, unterscheiden sich nur in der Spielweise",
        correctAnswer = 1,
        explanation = "Die britische Brass Band (Cornet, Flugelhorn, Tenorhorn, Bariton, Euphonium, Posaunen, Es- und B-Tuba) kennt keine Holzblasinstrumente. Ein Sinfonisches Blasorchester hingegen umfasst Floeten, Oboen, Klarinetten, Fagotte sowie Blechblaeser und Schlagwerk.",
        difficulty = 4,
        funFact = "In der Brass-Band-Tradition werden Cornet und Flugelhorn in B notiert und klingen eine grosse Sekunde tiefer — alle Blechblasinstrumente ausser Posaunen und Bassen sind transponierend notiert. Das erfordert eine eigene Notationspraxis."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Komponist schrieb 'Lincolnshire Posy', eine Suite von sechs Saetzen fuer Blasorchester basierend auf englischen Volksliedern?",
        answerA = "Ralph Vaughan Williams",
        answerB = "Gustav Holst",
        answerC = "Percy Grainger",
        answerD = "Benjamin Britten",
        correctAnswer = 2,
        explanation = "Percy Grainger (1882-1961) komponierte 'Lincolnshire Posy' 1937 fuer das American Bandmasters Association Meeting. Die sechs Saetze basieren auf englischen Volksliedern aus Lincolnshire, die Grainger 1905-1906 selbst gesammelt hatte.",
        difficulty = 4,
        funFact = "Grainger notierte in 'Lincolnshire Posy' eine Taktart von 1,5/4 — anderthalb Viertel pro Takt. Seine unorthodoxe Notation und Verwendung von Dialektwoertern ('louden lots', 'slow off') macht das Werk bis heute zu einer Herausforderung fuer Dirigenten."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie heisst die Technik beim Blechblasen, bei der Spieler ohne Absetzen weiterspielen, indem sie gleichzeitig durch die Nase einatmen?",
        answerA = "Flatterzunge",
        answerB = "Zirkulaeres Atmen (Zirkulararmen)",
        answerC = "Doppelzunge",
        answerD = "Bouche fermee",
        correctAnswer = 1,
        explanation = "Beim Zirkularatmen (Circular Breathing) werden die Wangenmuskel komprimiert, um waehrend des Einatmens durch die Nase weiter Luft durch das Instrument zu drucken. Diese Technik ermoeglicht theoretisch endlos lange Toene ohne Atemunterbrechung.",
        difficulty = 4,
        funFact = "Der australische Didgeridoo-Spieler David Hudson schaffte es 1997, einen ununterbrochenen Ton fuer ueber eine Stunde zu halten. In der klassischen Musik ist die Technik vor allem bei Oboe und Klarinette bekannt — auf Blechblasinstrumenten ist sie wegen des hohen Anblasdrucks schwieriger."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Werk von Karel Husa erhielt 1969 den Pulitzer Prize for Music und war urspruenglich fuer Blasorchester komponiert?",
        answerA = "Music for Prague 1968",
        answerB = "Apotheosis of This Earth",
        answerC = "Al Fresco",
        answerD = "Concerto for Wind Ensemble",
        correctAnswer = 0,
        explanation = "Karel Husas 'Music for Prague 1968' entstand als direkte Reaktion auf den Einmarsch der Warschauer-Pakt-Truppen in die Tschechoslowakei. Die Originalversion fuer Blasorchester erhielt 1969 den Pulitzer Prize; eine Orchesterfassung folgte spaeter.",
        difficulty = 4,
        funFact = "Die Urauffuehrung fand am 20. Januar 1969 in Washington D.C. statt, nur wenige Monate nach dem Prager Fruehling. In der Tschechoslowakei wurde das Werk jahrzehntelang verboten — Husa selbst lebte im US-Exil."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Concert Band' im amerikanischen Musikbetrieb im Unterschied zu einer 'Marching Band'?",
        answerA = "Eine Band, die ausschliesslich im Freien spielt",
        answerB = "Ein sinfonisch ausgerichtetes Blasensemble fuer Konzertsaalauftritte im Sitzen",
        answerC = "Eine Jazz-Band mit Blaesern und Schlagzeug",
        answerD = "Ein Ensemble ausschliesslich fuer Militaerzeremonien",
        correctAnswer = 1,
        explanation = "Eine Concert Band (auch Symphonic Band oder Wind Ensemble) ist ein Blasensemble, das sitzend im Konzertsaal auftritt und anspruchsvolle Originalliteratur sowie Transkriptionen interpretiert. Eine Marching Band ist dagegen fuer Auftritte in Bewegung (Paraden, Halbzeitshows) konzipiert.",
        difficulty = 4,
        funFact = "Frederick Fennell gruendete 1952 das Eastman Wind Ensemble in Rochester (New York) und revolutionierte das Konzept: Er besetzte jede Stimme nur einfach statt mehrfach — was saemlliche Blaeserfehler hoerbar machte und hoechste Qualitaet forderte."
    ),

    // ── Musikjournalismus / Fachzeitschriften (7) ────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welche britische Musikzeitschrift, 1923 gegruendet, gilt als eine der aeltesten und renommiertesten Fachpublikationen fuer klassische Musik weltweit?",
        answerA = "Gramophone",
        answerB = "The Wire",
        answerC = "Q Magazine",
        answerD = "Melody Maker",
        correctAnswer = 0,
        explanation = "Gramophone wurde 1923 von Compton Mackenzie und Christopher Stone gegruendet und ist die aelteste regelmaessig erscheinende Zeitschrift fuer klassische Aufnahmen. Die jaehrlichen Gramophone Awards gehoeren zu den prestigetraechtigsten Auszeichnungen der Klassikbranche.",
        difficulty = 4,
        funFact = "Die allererste Ausgabe des Gramophone kostete sechs Pence und enthielt Kritiken zu Schellackplatten. Der Name spielte auf das damals neue Medium des Grammophons an — Vinyl- und CD-Zeitalter wurden spaeter einfach mitgenommen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher amerikanische Musikkritiker praegte den Begriff 'Rockism' und schrieb jahrelang fuer den Rolling Stone?",
        answerA = "Greil Marcus",
        answerB = "Robert Christgau",
        answerC = "Lester Bangs",
        answerD = "Dave Marsh",
        correctAnswer = 1,
        explanation = "Robert Christgau, oft 'Dean of American Rock Critics' genannt, praegte massgeblich den Begriff 'Rockism' — die Tendenz, Rock als die einzig legitime Popform zu privilegieren und andere Genres abzuwerten. Er schrieb Jahrzehnte fuer den Village Voice, nicht den Rolling Stone.",
        difficulty = 4,
        funFact = "Christgau entwickelte ein einzigartiges Bewertungssystem: Noten von A+ bis E, erganzt durch kryptische Symbole wie 'dud' (kompletter Fehlschlag) oder 'turkey' (Totalausfall). Seine Consumer Guides sind heute Musikgeschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche deutsche Musikfachzeitschrift wurde 1798 von Friedrich Rochlitz gegruendet und war die erste periodische Musikpublikation von internationalem Rang?",
        answerA = "Neue Zeitschrift fuer Musik",
        answerB = "Allgemeine Musikalische Zeitung",
        answerC = "Musikalisches Wochenblatt",
        answerD = "Der Merker",
        correctAnswer = 1,
        explanation = "Die 'Allgemeine Musikalische Zeitung' erschien von 1798 bis 1848 in Leipzig beim Verlag Breitkopf & Haertel. Friedrich Rochlitz gruendete und leitete sie; spaeter schrieben auch E.T.A. Hoffmann und andere bedeutende Kritiker darin.",
        difficulty = 4,
        funFact = "E.T.A. Hoffmanns beruehmte Rezension von Beethovens Fuenfter Sinfonie erschien 1810 in der Allgemeinen Musikalischen Zeitung — ein Text, der die romantische Beethoven-Rezeption bis heute praegt."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer gruendete 1834 die 'Neue Zeitschrift fuer Musik' und nutzte sie als Plattform fuer seine romantischen Musikanschauungen?",
        answerA = "Franz Liszt",
        answerB = "Hector Berlioz",
        answerC = "Robert Schumann",
        answerD = "Richard Wagner",
        correctAnswer = 2,
        explanation = "Robert Schumann gruendete die Neue Zeitschrift fuer Musik in Leipzig, um gegen den musikalischen Konservatismus zu kaempfen und neue Talente zu foerdern. Er erfand die fiktiven Figuren Florestan und Eusebius als Alter Egos fuer verschiedene Kritikerstandpunkte.",
        difficulty = 4,
        funFact = "Schumanns Rezension von Chopins Variationen ueber 'La ci darem la mano' von 1831 begann mit dem beruehten Satz: 'Hut ab, ihr Herren — ein Genie!' — ein Satz, der Chopin schlagartig bekannt machte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche US-amerikanische Musikzeitschrift vergibt seit 1979 die 'Pazz & Jop Critics Poll' und gilt als wichtigstes Forum des amerikanischen Rockkritik-Establishments?",
        answerA = "Rolling Stone",
        answerB = "Pitchfork",
        answerC = "Village Voice",
        answerD = "Spin",
        correctAnswer = 2,
        explanation = "Die Village Voice fuehlte in New York erscheinende Alternativzeitung vergibt seit 1971 ihre Jahresend-Kritikerumfrage 'Pazz & Jop' (ein Wortspiel mit Jazz und Pop). Sie galt als wichtigstes Barometer des amerikanischen Kritik-Establishments.",
        difficulty = 4,
        funFact = "Die Village Voice stellte 2017 den Printbetrieb ein und 2018 den Online-Betrieb. Das Ende dieser Institution war symbolisch fuer den Niedergang des klassischen Musikjournalismus im Zeitalter der Streaming-Algorithmen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Online-Musikmagazin, 1996 gegruendet, wurde durch seine dezimale Bewertungsskala (0,0 bis 10,0) und seine Wirkung auf Indie-Plattenverkaeufe bekannt?",
        answerA = "AllMusic",
        answerB = "Pitchfork",
        answerC = "Metacritic",
        answerD = "NME Online",
        correctAnswer = 1,
        explanation = "Pitchfork wurde 1996 von Ryan Schreiber in Minnesota gegruendet und wuchs zum einflussreichsten Indie-Musikmagazin im Internet. Eine Pitchfork-Bewertung von 8.0+ konnte Alben unbekannter Bands in die Bestsellerlisten katapultieren — ein Effekt, der als 'Pitchfork Effect' bekannt wurde.",
        difficulty = 4,
        funFact = "Pitchfork vergab 2002 dem Debuetalbuem von Interpol, 'Turn On the Bright Lights', die Wertung 9.0 — was das Album aus der Obskuritaet zur Indie-Sensation machte. Die Macht einzelner Redakteure war damit groeszer als je zuvor."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Begriff beschreibt die journalistische Praxis, Musikkritiken zu veroffentlichen, bevor ein Album offiziell erscheint, und wie heisst die entsprechende Materiallieferung?",
        answerA = "Advance Copy / Promo-Exemplar",
        answerB = "Demo-Leak / Bootleg",
        answerC = "White Label / Test-Pressing",
        answerD = "Acetate / Lacquer",
        correctAnswer = 0,
        explanation = "Ein 'Advance Copy' (auch 'Promo' oder Rezensionsexemplar) wird Musikkritikern vor dem Erscheinungsdatum zugeschickt, damit Kritiken zeitgleich mit oder kurz vor der Veroffentlichung erscheinen koennen. Dies ist gaengige Praxis in der Musikbranche.",
        difficulty = 4,
        funFact = "Promo-CDs waren frueherer aeusserlich markiert (oft mit gestanzten Loechern oder Aufklebern), um den Weiterverkauf zu verhindern. Im Streaming-Zeitalter erfolgen Embargos ueber gesperrte Streaming-Links mit Ablaufdatum."
    ),

    // ── Opernhaeuser / Konzertsaele-Geschichte (7) ───────────────────────────

    Question(
        categoryId = 5,
        questionText = "In welchem Jahr wurde die Wiener Staatsoper (damals k.k. Hof-Operntheater) eroeffnet, und mit welcher Oper?",
        answerA = "1857 mit Verdis 'Rigoletto'",
        answerB = "1869 mit Mozarts 'Don Giovanni'",
        answerC = "1875 mit Wagners 'Lohengrin'",
        answerD = "1863 mit Meyerbeers 'L'Africaine'",
        correctAnswer = 1,
        explanation = "Das k.k. Hof-Operntheater (heute Wiener Staatsoper) wurde am 25. Mai 1869 mit Mozarts 'Don Giovanni' eroeffnet. Der Bau der Architekten Eduard van der Null und August Sicard von Sicardsburg wurde fuer sein wuchtiges Erscheinungsbild so stark kritisiert, dass van der Null Suizid beging.",
        difficulty = 4,
        funFact = "Kaiser Franz Joseph soll das Gebaeude als 'versunkene Kiste' bezeichnet haben — obwohl dieser Ausspruch historisch umstritten ist. Der Kaiser bereute spaeter, sich so geaeussert zu haben, und wurde in der Folge vorsichtiger mit oeffentlichen Kunstkritiken."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Konzerthaus in Amsterdam gilt wegen seiner herausragenden Akustik als eines der drei besten Konzerthaeuser der Welt und wurde 1888 eroeffnet?",
        answerA = "Muziekgebouw aan 't IJ",
        answerB = "Concertgebouw",
        answerC = "Royal Carré Theatre",
        answerD = "Paradiso",
        correctAnswer = 1,
        explanation = "Das Concertgebouw (Konzertgebaeude) in Amsterdam wurde 1888 eroeffnet und ist Heimat des Koninklijk Concertgebouworkest. Seine Schuebox-Akustik mit einem Nachhall von ca. 2,2 Sekunden gilt als Referenz weltweit.",
        difficulty = 4,
        funFact = "Der Architekt Adolf Leonard van Gendt hatte keine akustische Ausbildung — die Akustik entstand nach dem Vorbild der Gewandhaussaal in Leipzig. Die Entscheidung, keine Saeulen im Innenraum zu platzieren, war zufaellig optimal fuer die Schalldiffusion."
    ),

    Question(
        categoryId = 5,
        questionText = "Die Mailaender Scala wurde 1778 eroeffnet und ersetzte ein zuvor abgebranntes Opernhaus. Wie hiess dieses Vorgaengergebaeude?",
        answerA = "Teatro Regio",
        answerB = "Teatro Ducale",
        answerC = "Teatro San Carlo",
        answerD = "Teatro della Pergola",
        correctAnswer = 1,
        explanation = "Das Teatro Ducale (Herzogstheater) in Mailand brannte 1776 nieder. Kaiserin Maria Theresia genehmigte den Bau eines neuen Theaters auf dem Gelaende der abgerissenen Kirche Santa Maria della Scala — daher der Name 'La Scala'.",
        difficulty = 4,
        funFact = "Zur Eroeffnung der Scala 1778 wurde Antonio Salieris Oper 'L'Europa riconosciuta' uraufgefuehrt. Salieri, der heutige Schatten Mozarts, war damals einer der renommiertesten Opernkomponisten Europas."
    ),

    Question(
        categoryId = 5,
        questionText = "Das Bayreuther Festspielhaus wurde 1876 mit Wagners 'Ring des Nibelungen' eroeffnet. Welche akustische Besonderheit machte Wagner zur Bedingung?",
        answerA = "Ein vollstaendig vergoldeter Resonanzboden unter dem Orchesterpult",
        answerB = "Der verdeckte Orchestergraben, der das Orchester unter der Buehne verbirgt",
        answerC = "Seitliche Schallreflektoren aus Eichenholz fuer gleichmaessige Diffusion",
        answerD = "Ein doppelter Buehnenvorhang aus Damast fuer Nachhallkontrolle",
        correctAnswer = 1,
        explanation = "Wagners zentralste akustische Innovation im Festspielhaus ist der tief versenkte, abgedeckte Orchestergraben. Das Orchester ist unsichtbar; der Klang steigt geblendet auf und mischt sich unter der Buehne, bevor er den Zuschauerraum erreicht. Dies erzeugt den charakteristischen 'mystischen Abgrund'-Klang.",
        difficulty = 4,
        funFact = "Wagner nannte den Orchestergraben den 'mystischen Abgrund'. Das Konzept bewirkt, dass Saengerstimmen gegenueber dem Orchester nie uebertoebt werden — ein Problem, das in normalen Opernhaeusern chronisch ist. Der Graben fasst bis zu 115 Musiker."
    ),

    Question(
        categoryId = 5,
        questionText = "Wann wurde Carnegie Hall in New York eroeffnet, und wer dirigierte das Eroeffnungskonzert?",
        answerA = "1891, Peter Iljitsch Tschaikowski",
        answerB = "1900, Arturo Toscanini",
        answerC = "1885, Anton Dvorak",
        answerD = "1895, Johannes Brahms",
        correctAnswer = 0,
        explanation = "Carnegie Hall wurde am 5. Mai 1891 eroeffnet. Peter Iljitsch Tschaikowski dirigierte selbst eigene Werke beim Eroeffnungskonzert — es war einer seiner wenigen Besuche in Amerika. Walter Damrosch dirigierte den ersten Abend, Tschaikowski folgende Programme.",
        difficulty = 4,
        funFact = "Andrew Carnegie finanzierte das Gebaeude auf Bitten des Dirigenten Walter Damrosch. Carnegie zahlte 1,1 Millionen Dollar und erhielt die ewige Namensgebung. 1960 drohte der Abriss — erst eine Buergerbewegung (und Isaac Stern) retteten das Gebaeude."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Architekt entwarf die Sydney Opera House, das 1973 eroeffnet wurde und spaeter zum UNESCO-Weltkulturerbe erklaert wurde?",
        answerA = "Renzo Piano",
        answerB = "Frank Gehry",
        answerC = "Joern Utzon",
        answerD = "Oscar Niemeyer",
        correctAnswer = 2,
        explanation = "Der daenische Architekt Joern Utzon gewann 1957 den Wettbewerb mit seinem Entwurf der schalenartig gefaecherten Daecher. Wegen politischer Konflikte trat er 1966 zurueck, bevor das Gebaeude fertiggestellt war — er betrat es nach der Eroeffnung nie wieder.",
        difficulty = 4,
        funFact = "Utzon erhielt 2003 den Pritzker-Preis, die hoechste Auszeichnung der Architektur. In seiner Dankesrede erwaehnte er das Sydney Opera House mit keinem Wort — ein stilles Zeugnis des jahrzehntelangen Konflikts mit der australischen Regierung."
    ),

    Question(
        categoryId = 5,
        questionText = "Die Elbphilharmonie in Hamburg eroeffnete 2017. Welches akustische Prinzip praegt den Grossen Saal, entworfen vom Akustiker Yasuhisa Toyota?",
        answerA = "Vineyard-Anordnung mit dem Orchester auf erhoehten Terrassen ringsum",
        answerB = "Schuebox-Prinzip mit parallelen langen Seitenwanden",
        answerC = "Vineyard-Anordnung mit dem Podium in der Saalmitte und Zuhoerern rundherum",
        answerD = "Reflektionsdecke aus Beton ohne Diffusoren",
        correctAnswer = 2,
        explanation = "Der Grosse Saal der Elbphilharmonie nutzt das Vineyard-Prinzip: Das Orchester steht in der Mitte, die Zuschauerraenge (wie Weinbergterrassen) umgeben es rings. Die weisse Gipsfaserfaser-'Haut' der Wandverkleidung erzeugt eine praezise kontrollierte Schallstreuung.",
        difficulty = 4,
        funFact = "Die Weiss-Gips-Wandverkleidung des Saals besteht aus rund 10.000 einzeln gefraesten, computerberechneten Gipsfaserplatten. Kein Panel ist identisch — jedes wurde individuell gefraest, um optimale Diffusion in jedem Winkel zu gewaehrleisten."
    ),

    // ── Popularmusik-Analyse (7) ─────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches harmonische Muster bezeichnet die '50s-Progression', die in unzaehligen Pop-Songs der Nachkriegszeit vorkommt?",
        answerA = "I - IV - V - I",
        answerB = "I - V - vi - IV",
        answerC = "I - vi - IV - V",
        answerD = "ii - V - I - IV",
        correctAnswer = 2,
        explanation = "Die '50s-Progression' lautet I - vi - IV - V (z.B. in C-Dur: C-Am-F-G). Sie praegt unzaehlige Songs von 'Earth Angel' ueber 'Stand by Me' bis zu modernen Chartproduktionen. Das Grundmuster laeuft zur Tonika, Tonikaparallele, Subdominante und Dominante.",
        difficulty = 4,
        funFact = "Ben E. Kings 'Stand by Me' (1961) wurde mit dieser Progression produziert. Das Muster taucht spaeter unveraendert in 'Every Breath You Take' (The Police, 1983) und hunderten anderen Songs auf — eine der meistkopierten Progressionen der Popgeschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Was versteht man im Kontext der Popularmusik-Analyse unter dem Begriff 'Truck Driver's Gear Change'?",
        answerA = "Ein Rhythmuswechsel von geraden zu triolischen Achteln",
        answerB = "Eine abrupte Modulation um einen Halbton oder Ganzton nach oben zur Steigerung der letzten Wiederholung",
        answerC = "Ein Tempowechsel von langsam zu schnell im Mittelteil",
        answerD = "Eine ploetzliche Transposition um eine Quinte nach oben",
        correctAnswer = 1,
        explanation = "Der 'Truck Driver's Gear Change' bezeichnet eine abrupte, harmonisch unvermittelte Modulation um eine Sekunde (halb oder ganz) nach oben am Ende eines Popsongs. Der Effekt soll Energie erzeugen, wirkt aber auf geuebbte Hoerer oft mechanisch und klischeehaft.",
        difficulty = 4,
        funFact = "Whitney Houstons 'I Will Always Love You' moduliert dreimal nach oben. Die Technik wird im englischsprachigen Internet humorvoll als 'Truck Driver's Gear Change' oder 'Shotgun Modulation' bezeichnet und hat eine ganze Subkultur aus Kritikern und Liebhabern."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Methode der Musikanalyse, entwickelt von Allan Moore, untersucht, 'wer in einem Song zu wem spricht', und nennt sich...?",
        answerA = "Semiotische Analyse nach Tagg",
        answerB = "Grounded Theory nach Strauss",
        answerC = "Persona-Analyse / 'Persona and Listener Subjectivity'",
        answerD = "Schichten-Modell nach Middleton",
        correctAnswer = 2,
        explanation = "Allan Moore entwickelte in 'Song Means' (2012) das Konzept der 'Persona und Listener Subjectivity': Die Analyse fragt, wer die Sprecherperspektive im Song einnimmt, an wen er sich richtet, und welche subjektive Bedeutung Hoerer konstruieren — ein Kernkonzept moderner Popularmusik-Hermeneutik.",
        difficulty = 4,
        funFact = "Moore lehnte einfache strukturalistische Analysen von Popmusik ab, weil sie den Hoerer als passives Subjekt behandeln. Sein Ansatz betont, dass Bedeutung im Wechselspiel zwischen Aufnahme, Kontext und individuellem Hoerer entsteht."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt der Begriff 'Blue Note' in der Musiktheorie des Blues und Jazz?",
        answerA = "Eine Note, die um einen Halbton erniedrigt wird, typischerweise die Terz, Quinte oder Septime einer Durtonleiter",
        answerB = "Eine Modulation in eine enharmonisch verwechselte Tonart",
        answerC = "Ein Crescendo auf der 3. Zahlzeit eines 4/4-Takts",
        answerD = "Eine spezifische Artikulation (Staccatissimo) auf Blechblasinstrumenten",
        correctAnswer = 0,
        explanation = "Blue Notes sind erniedrigte Toene (meist die kleine Terz, verminderte Quinte oder kleine Septime) gegenueber der Durtonleiter. Sie entstammen der afrikanisch-amerikanischen Musiktradition und erzeugen die charakteristische Spannung und 'Blaue' Faerbung des Blues.",
        difficulty = 4,
        funFact = "Die Theorie der Blue Notes ist komplex: Im Blues werden sie oft als Glissando zwischen grosser und kleiner Terz gespielt — kein fixer Ton, sondern ein expressives Gleiten. Das laesst sich auf Tasteninstrumenten nur annaeherungsweise darstellen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Musikwissenschaftler praegte in seinem Werk 'Analyzing Popular Music' (1982) den Begriff der 'syncresis' in der audiovisuellen Medienanalyse?",
        answerA = "Simon Frith",
        answerB = "Philip Tagg",
        answerC = "Richard Middleton",
        answerD = "Michel Chion",
        correctAnswer = 1,
        explanation = "Philip Tagg entwickelte eine systematische semiotische Methode zur Analyse von Popularmusik und Filmmusik. 'Syncresis' (eigentlich von Michel Chion gepraegt) bezeichnet die spontane Verbindung von Ton und Bild beim Betrachter. Taggs Methode nutzt Musemen (kleinste bedeutungstragende Einheiten) als Analyseeinheit.",
        difficulty = 4,
        funFact = "Tagg wurde als Schwede an einem britischen Institut zum Pionier der Popularmusikwissenschaft. Sein Analysemodell, das klassische Musiktheorie mit Semiotik verbindet, wurde an Universitaeten weltweit uebernommen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Rhythmusstruktur bezeichnet der Begriff 'Clave' in der kubanischen und lateinamerikanischen Musik?",
        answerA = "Ein zweitaktiges Grundrhythmusschema mit asymmetrischer Akzentuierung",
        answerB = "Eine regelmaessige Achtelnotenfolge im 6/8-Takt",
        answerC = "Eine Synkopierung auf der dritten Zahlzeit im 4/4-Takt",
        answerD = "Eine Polyrhythmik aus 3-gegen-2-Mustern ueber vier Takte",
        correctAnswer = 0,
        explanation = "Die Clave ist ein zweitaktiges Rhythmusmuster, das in Son, Salsa, Mambo und anderen kubanischen Stilen die gesamte Ensemble-Struktur organisiert. In der '3-2 Clave' fallen in Takt 1 drei Schlaege und in Takt 2 zwei Schlaege; in der '2-3 Clave' ist es umgekehrt.",
        difficulty = 4,
        funFact = "Der Begriff 'Clave' bedeutet auf Spanisch 'Schluessel' — der Rhythmus ist buchstaeblich der Schluessel, der bestimmt, wie alle anderen Instrumente ihre Phrasierung ausrichten. Ein Musiker, der gegen die Clave spielt, 'bricht' die musikalische Struktur."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt der Begriff 'Hamonischer Rhythmus' (Harmonic Rhythm) in der Popularmusik-Analyse?",
        answerA = "Die Geschwindigkeit, mit der Akkorde wechseln, im Verhaeltnis zum Melodierhythmus",
        answerB = "Die Anzahl der Taktzahlzeiten, die ein Instrument pro Takt spielt",
        answerC = "Das Tempo-Verhaeltnis zwischen Bass und Schlagzeug",
        answerD = "Die Frequenzmodulation eines Synthesizers im Rhythmustakt",
        correctAnswer = 0,
        explanation = "Der harmonische Rhythmus beschreibt, wie schnell Akkordwechsel stattfinden — also ob Harmonien taktweise, halbaktweise oder in anderen Abstanden wechseln. Langsamer harmonischer Rhythmus erzeugt Stabilitaet und Ruhe, schneller harmonischer Rhythmus Spannung und Energie.",
        difficulty = 4,
        funFact = "In Beethovens Symphonien kann der harmonische Rhythmus plotzlich von einem Akkord pro Takt auf mehrere Akkorde pro Takt beschleunigen — ein Mittel, das Komponisten und Songschreiber aller Epochen einsetzen, um Spannung in Schluesselmomenten zu steigern."
    ),

    // ── Musikmanagement / Booking (7) ────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Was regelt ein 'Rider' in einem Kuenstlervertrag?",
        answerA = "Die kuenstlerische Setliste und Reihenfolge der Stuecke",
        answerB = "Die technischen und hospitality-bezogenen Anforderungen des Kuenstlers an den Veranstalter",
        answerC = "Die Honoraraufteilung zwischen Kuenstler und Bookingagentur",
        answerD = "Die Urheberrechtsvereinbarung fuer gespielte Fremdkompositionen",
        correctAnswer = 1,
        explanation = "Ein Rider ist ein Zusatz zum Auftrittvertrag, der technische Anforderungen (Stage Plot, Beschallungsanlage, Monitoring), Hospitality-Anforderungen (Catering, Unterkunft, Transport) und weitere Sonderwuensche des Kuenstlers verbindlich festlegt.",
        difficulty = 4,
        funFact = "Van Halens beruehmt-beruechtigter Rider enthielt die Klausel, alle braunen M&Ms aus den Suessigkeiten zu entfernen. Dies war kein Launenakt: Eddie Van Halen nutzte die M&M-Regel als Pruefstein — fand er braune M&Ms, wusste er, dass der Veranstalter den Rider nicht sorgfaeltig gelesen hatte, und liess die gesamte Buehnentechnik kontrollieren."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt der Begriff 'Guarantee vs. Percentage Deal' im Konzertbooking?",
        answerA = "Die Aufteilung der Ticketeinnahmen zwischen Veranstalter und Tickethändler",
        answerB = "Das Honorarmodell: feste Mindestgage vs. prozentualer Anteil an Ticketeinnahmen (ggf. als 'vs.' = Vorzug des hoeheren Betrags)",
        answerC = "Die Unterscheidung zwischen Festgage und Sachleistungen",
        answerD = "Der Unterschied zwischen Exklusiv- und Nicht-Exklusivvertraegen",
        correctAnswer = 1,
        explanation = "Bei einem 'Guarantee vs. Percentage Deal' erhaelt der Kuenstler entweder eine Mindestgage (Guarantee) oder einen Prozentsatz der Bruttoeinnahmen abzueglich Kosten — je nachdem, welcher Betrag hoeher ist. Das Modell schuetzt den Kuenstler vor schwacher Auslastung.",
        difficulty = 4,
        funFact = "Typische Splits sind 85/15 oder 90/10 (Kuenstler/Veranstalter) nach Abzug der Kosten. Bei grossen Stadion-Acts kann der Kuenstler 100% der Ticket-Bruttoeinnahmen jenseits der Break-even-Schwelle einfordern, was Veranstaltern kaum Gewinnspanne laesst."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Radius Clause' in einem Exklusiv-Auftrittvertrag?",
        answerA = "Eine Klausel, die dem Kuenstler verbietet, innerhalb einer bestimmten Distanz und Zeitspanne rund um eine gebuchte Show aufzutreten",
        answerB = "Eine Gewinnbeteiligung des Agenten an Merchandising-Erloesen",
        answerC = "Eine Vereinbarung ueber den maximalen Reisenradius bei Tour-Auftritten",
        answerD = "Die geographische Exklusivitaet eines Booking-Agenten fuer eine bestimmte Region",
        correctAnswer = 0,
        explanation = "Die Radius Clause (auch Exclusivity Clause) schuetzt den Veranstalter: Sie verbietet dem Kuenstler, fuer eine bestimmte Zeit vor und nach dem gebuchten Konzert innerhalb eines definierten Radius (z.B. 100 km) aufzutreten. So soll sichergestellt werden, dass das Publikum das gebuchte Event besucht.",
        difficulty = 4,
        funFact = "Radius Clauses sind in grossen Staedten wie New York oder London besonders umkämpft, da Kuenstler dort oft viele nahegelegene Venues bespielen. Manchmal wird der Radius auf 100 Meilen und die Sperrzeit auf 90 Tage vor und nach dem Konzert ausgedehnt."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen einem 'Manager' und einem 'Booking Agent' im Musikgeschaeft?",
        answerA = "Der Manager organisiert nur Tourneen, der Agent die kuenstlerische Entwicklung",
        answerB = "Der Manager betreut die gesamte Karrierestrategie; der Agent vermittelt Auftritte gegen Provision",
        answerC = "Der Manager verdient Festgehalt, der Agent verdient nichts",
        answerD = "Beide sind rechtlich identisch, nur der Titel unterscheidet sich",
        correctAnswer = 1,
        explanation = "Ein Musikmanager betreut die langfristige Karriere und Strategie (Deals, Image, Entscheidungen) und verdient typischerweise 15-20% aller Einnahmen. Ein Booking Agent vermittelt konkrete Auftritte gegen eine Provision von 10-15% der Gage und ist oft als Buchungsagentur lizenziert.",
        difficulty = 4,
        funFact = "In den USA muessen Booking Agents in vielen Bundesstaaten lizenziert sein (Talent Agency License). Ohne Lizenz duerfen sie keine Auftritte vermitteln. Manager unterliegen dagegen keiner gesetzlichen Lizenzpflicht — ein bewusster Graubereich, den viele ausnutzen."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bedeutet 'Merch Split' im Kontext von Live-Konzerten?",
        answerA = "Die Aufteilung der Ticketeinnahmen zwischen Vor- und Hauptband",
        answerB = "Der Anteil des Veranstalters an den Merchandising-Erloesen des Kuenstlers",
        answerC = "Die Aufteilung der Merchandise-Produktion zwischen Kuenstler und Label",
        answerD = "Die Provision des Managers an Merchandise-Gewinnen",
        correctAnswer = 1,
        explanation = "Der Merch Split (auch 'Hall Fee' oder 'Venue Percentage') ist der prozentuale Anteil, den eine Konzertlocation oder ein Veranstalter von den Merchandising-Erloesen des Kuenstlers einbehaelt. Typische Werte sind 20-35% fuer die Venue.",
        difficulty = 4,
        funFact = "In grossen Arenen kann der Merch Split bis zu 40% betragen. Da Merchandise bei populaeren Kuenstlern oft mehr einbringt als die Toureengage selbst, ist der Merch Split ein hoch umstrittener Verhandlungspunkt bei Konzertvertraegen."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist ein 'Settlement' nach einem Konzert im Musikbusiness?",
        answerA = "Die Steuererklarung nach einer Tournee",
        answerB = "Die finale Abrechnung zwischen Veranstalter und Kuenstler nach der Show, bei der Einnahmen, Kosten und die endgueltige Auszahlung ermittelt werden",
        answerC = "Die Einigung auf einen Schiedsspruch bei Vertragstreitigkeiten",
        answerD = "Die Zahlung von Lizenzgebuehren an die GEMA nach dem Konzert",
        correctAnswer = 1,
        explanation = "Das Settlement ist die buchhalterische Endabrechnung unmittelbar nach der Show. Ticketeinnahmen, Veranstaltungskosten, Gage-Garantie und eventuelle Prozentbeteiligungen werden gegenueberstellt, und der Kuenstler erhaelt sofort (in bar oder per Transfer) den ausstehenden Betrag.",
        difficulty = 4,
        funFact = "Das Settlement erfolgt traditionell in bar und unmittelbar nach der Show. Erfahrene Tour-Manager packen am Abend Bargeld in Umschlaege fuer alle Crew-Mitglieder. Der Begriff stammt aus einer Zeit, als kein elektronischer Zahlungsverkehr nach Mitternacht moeglich war."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Organisation repraesentiert in Deutschland die Interessen von Konzertagenturen, Veranstaltern und Tourneeproduzenten?",
        answerA = "GEMA (Gesellschaft fuer musikalische Auffuehrungs- und mechanische Vervielfaeltigungsrechte)",
        answerB = "GVL (Gesellschaft zur Verwertung von Leistungsschutzrechten)",
        answerC = "BDKV (Bundesverband der Konzert- und Veranstaltungswirtschaft)",
        answerD = "VUT (Verband unabhaengiger Musikunternehmen)",
        correctAnswer = 2,
        explanation = "Der BDKV (Bundesverband der Konzert- und Veranstaltungswirtschaft) vertritt die wirtschaftlichen und politischen Interessen der Konzert- und Veranstaltungsbranche in Deutschland. Er setzt sich fuer faere Rahmenbedingungen, Urheberrecht und Live-Entertainment-Foerderung ein.",
        difficulty = 4,
        funFact = "Waehrend der COVID-19-Pandemie war der BDKV einer der lautstaerksten Interessenvertreter fuer staatliche Hilfen fuer die Veranstaltungsbranche. Die Branche gehoerte zu den laengsten 'Lockdown-Opfern' — in Deutschland war das Live-Geschaeft ueber 18 Monate weitgehend zum Erliegen gekommen."
    )

)
