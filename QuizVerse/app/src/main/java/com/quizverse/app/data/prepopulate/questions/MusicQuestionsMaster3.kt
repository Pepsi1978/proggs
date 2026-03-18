package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsMaster3(): List<Question> = listOf(

    // ── Kontrapunkt / Fugenanalyse (10) ──────────────────────────────────────

    // Q1
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Engfuehrung' (Stretto) in der Fugenkomposition, und wo setzt Bach ihn strukturell besonders wirkungsvoll ein?",
        answerA = "Die engere Chromatik des Fugenthemas im Vergleich zur diatonischen Gegenstimme",
        answerB = "Die zeitliche Ueberlappung von Themeneinsaetzen, bei der ein neuer Einsatz beginnt, bevor der vorherige abgeschlossen ist — Bach nutzt ihn oft als Steigerungsmittel im Durchfuehrungs- oder Schlussteil",
        answerC = "Die Verdichtung aller Stimmen auf einen engen Tonraum von weniger als einer Oktave",
        answerD = "Ein Kanon in engstem Abstand (eine Viertelnote), der nur in der Schlussgruppe einer Fuge vorkommt",
        correctAnswer = 1,
        explanation = "Im Stretto beginnt der Fugenthemen-Einsatz einer Stimme, bevor die vorherige Stimme das Thema beendet hat. Die Stimmen 'verfolgen' sich gegenseitig. Bach setzt Stretto-Passagen haeufig als dramatischen Hoehepunkt ein (z.B. in der c-Moll-Fuge BWV 847 oder in der D-Dur-Fuge BWV 850 des Wohltemperierten Klaviers I), um den kontrapunktischen Druck zu erhoehen.",
        difficulty = 5,
        funFact = "Die maximale Strettodichte nennt man 'Stretto maestrale': Hier koennen alle vier Stimmen gleichzeitig das Thema in voller Laenge tragen, ohne Kollidieren — eine analytische Meisterleistung, die Bach gezielt in der C-Dur-Fuge BWV 846 demonstriert."
    ),

    // Q2
    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'Tonal Answer' und 'Real Answer' in der Fugenexposition?",
        answerA = "Real Answer transportiert das Thema exakt in die Quinte (oder Quarte); Tonal Answer modifiziert Intervalle, um zwischen Tonika- und Dominantbereichen zu vermitteln und harmonische Ambivalenzen zu vermeiden",
        answerB = "Tonal Answer erscheint in der Tonika, Real Answer in der Subdominante — beide Typen sind in der Barockfuge gleich haeufig",
        answerC = "Real Answer ist die buchstaebliche Wiederholung des Themas ohne Transpositionen; Tonal Answer transponiert es eine grosse Terz nach oben",
        answerD = "Tonal Answer bezeichnet die Antwort mit rhythmischer Augmentation, Real Answer die Antwort in originaler Rhythmik",
        correctAnswer = 0,
        explanation = "Bei der Real Answer wird das Thema exakt um eine Quinte hoeher (oder Quarte tiefer) transponiert — alle Intervalle bleiben identisch. Bei der Tonal Answer werden einzelne Intervalle angepasst, meist wenn das Thema auf dem fuenften Skalenton beginnt oder endet, um zu vermeiden, dass die Antwort auf den zweiten Skalenton eines neuen Modus weist (Tonart-Ambivalenz). Bach verwendet beide Typen bewusst.",
        difficulty = 5,
        funFact = "Die Terminologie 'tonal/real' stammt nicht von Bach selbst, sondern wurde im 19. Jahrhundert von Musiktheoretikern wie Cherubini und Reicha systematisiert. In Bachs Praxis entschied er intuitiv nach harmonischen und melodischen Kriterien."
    ),

    // Q3
    Question(
        categoryId = 5,
        questionText = "Welche Funktion uebernimmt das 'Codetta' in der Fugenexposition?",
        answerA = "Ein kurzes Zwischenspiel zwischen dem Thema und seiner Antwort, das harmonisch zur Dominante ueberleitet und den naechsten Einsatz vorbereitet",
        answerB = "Der abschliessende Kadenzabschnitt der gesamten Fuge, analog zum Koda in der Sonatenform",
        answerC = "Ein zweiter, kuerzerer Kontrasubjekt-Strang, der parallel zum Hauptkontrasubjekt laeuft",
        answerD = "Die letzte Durchfuehrung der Fuge, die das Thema in der Subdominante praesentiert",
        correctAnswer = 0,
        explanation = "Das Codetta (kleiner Abschluss) ist ein kurzes Verbindungsstueck innerhalb der Fugenexposition zwischen dem Ende des Themas und dem Beginn der Antwort in der zweiten Stimme. Es dient der harmonischen Vorbereitung und fuellt die kontrapunktische Luecke, die entstuende, wenn die Antwort sofort beginnen wuerde. Nicht alle Fugen benoetigen eine Codetta.",
        difficulty = 5,
        funFact = "In der Sekundaerliteratur wird 'Codetta' gelegentlich mit 'Zwischenspiel' (Episode) verwechselt. Das Zwischenspiel erscheint hingegen zwischen Durchfuehrungen und modelliert sequenziell die Modulation — es ist strukturell viel umfangreicher als die Codetta."
    ),

    // Q4
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter dem 'invertiblen Kontrapunkt in der Oktave' und warum ist er fuer die Fugenkomposition unverzichtbar?",
        answerA = "Kontrapunkt, bei dem die obere und untere Stimme ausgetauscht werden koennen, ohne dass Regelverstoesse entstehen — dies bedingt, dass die Sexte zur Terz wird und Quinten vermieden werden muessen",
        answerB = "Eine Technik, bei der das Thema um eine Oktave nach unten transponiert und dann spiegelbildlich invertiert wird",
        answerC = "Die Umkehrung eines Kontrapunkts durch Spiegelung aller Intervalle an der Mittelachse, ohne Stimmtausch",
        answerD = "Der zweistimmige Kontrapunkt, bei dem beide Stimmen strikt im Abstand einer Oktave gefuehrt werden",
        correctAnswer = 0,
        explanation = "Beim invertierbaren Kontrapunkt in der Oktave koennen Ober- und Unterstimme vertauscht werden (die Oberstimme wird zur Unterstimme und umgekehrt), ohne Regelverstoesse zu erzeugen. Der kritische Punkt: Die Quinte wird bei Stimmtausch zur Quarte, die als Dissonanz gilt — deshalb muessen parallele Quinten grundsaetzlich vermieden werden. Bach benutzt invertiblen Kontrapunkt, um dasselbe Material in unterschiedlichen Kombinationen wiederzuverwenden.",
        difficulty = 5,
        funFact = "Fortspinnbare invertierbare Kontrapunkte in der Duodezime (Abstand 12) und in der Dezime (Abstand 10) existieren ebenfalls und sind deutlich schwerer zu konstruieren. Bach demonstriert alle drei Typen implizit in 'Die Kunst der Fuge'."
    ),

    // Q5
    Question(
        categoryId = 5,
        questionText = "Welches formale Merkmal unterscheidet eine 'Doppelfuge' von einer regulaeren Fuge, und welches bekannte Werk Bachs gilt als Hauptbeispiel?",
        answerA = "Eine Doppelfuge hat zwei vollstaendige Expositionen mit zwei selbstaendigen Themen, die spaeter kombiniert werden — ein Hauptbeispiel ist die Fuge cis-Moll BWV 873 aus dem WTK II",
        answerB = "Eine Doppelfuge hat doppelt so viele Stimmen wie eine normale Fuge und laeuft stets in zwei gegenlaeufigen Tonarten",
        answerC = "Doppelfuge bedeutet, dass dasselbe Thema in Augmentation und Diminution gleichzeitig erscheint",
        answerD = "Eine Doppelfuge praesentiert das Thema zuerst in Dur, dann in Moll, wobei beide Expositionen voneinander getrennt sind",
        correctAnswer = 0,
        explanation = "Die Doppelfuge besteht aus zwei selbstaendigen Themen: Zuerst wird Thema 1 exponiert und durchgefuehrt; dann erscheint Thema 2 in einer eigenen Exposition; schliesslich werden beide kombiniert. Die cis-Moll-Fuge BWV 873 (WTK II) ist ein klassisches Bach-Beispiel. 'Die Kunst der Fuge' enthaelt ebenfalls Doppel- und Tripelfugen (Contrapuncti X-XIV).",
        difficulty = 5,
        funFact = "Mozarts Fuge in c-Moll KV 546 fuer Streichquartett (1788) gilt als Meisterbeispiel einer Doppelfuge in der Wiener Klassik — Mozart studierte intensiv Bachs Wohltemperiertes Klavier unter Anleitung von Baron van Swieten."
    ),

    // Q6
    Question(
        categoryId = 5,
        questionText = "Was ist der analytische Begriff 'Soggetto cavato' und welcher Komponist verwendete ihn erstmals systematisch?",
        answerA = "Ein Thema, das aus den Vokalsilben eines Namens oder Textes durch Solmisation herausgeschnitzt wird — Josquin des Prez galt als erster systematischer Anwender",
        answerB = "Ein Kontrapunktthema, das ausschliesslich aus diatonischen Schritten besteht (keine Spruenge) — Palestrina nannte es 'soggetto piano'",
        answerC = "Das Hauptthema einer Messvertonung, gegrundet auf dem Cantus firmus des Gregorianischen Chorals",
        answerD = "Ein Fugenthema mit genau sieben Noten, korrespondierend zu den sieben Silben des Vaterunsers",
        correctAnswer = 0,
        explanation = "Soggetto cavato (ital. 'herausgeschnitztes Thema') bezeichnet ein Fugenthema, das durch Hexachord-Solmisation aus Silben eines Namens oder Wortes gewonnen wird: Jeder Vokal eines Namens entspricht einem Solmisationssilben-Ton (ut, re, mi, fa, sol, la). Josquin de Prez verwendete dieses Verfahren in seiner 'Missa Hercules Dux Ferrariae', wo der Name des Herzogs Ercole d'Este in Toene umgewandelt wird.",
        difficulty = 5,
        funFact = "B-A-C-H als Thema ist kein echter Soggetto cavato, sondern ein Buchstaben-Tonhoehen-Mapping (H = dt. H = B-Natur). Das BACH-Motiv erscheint explizit in Contrapunctus XIV der 'Kunst der Fuge' — der Fuge, bei der Bach starb und die unvollendet blieb."
    ),

    // Q7
    Question(
        categoryId = 5,
        questionText = "Welchen kontrapunktischen Begriff beschrieb Johann Joseph Fux 1725 in 'Gradus ad Parnassum' mit 'Species Counterpoint', und wie viele Species definierte er?",
        answerA = "Vier Species: 1:1, 2:1, 3:1 und syncopierter Kontrapunkt; Fux verwendete Gattungen zur pädagogischen Stufenfolge",
        answerB = "Fuenf Species: 1. Note gegen Note, 2. zwei Noten gegen eine, 3. vier Noten gegen eine, 4. Synkopen-Kontrapunkt, 5. Florid-Kontrapunkt — als pedadogischer Stufengang von einfach nach komplex",
        answerC = "Drei Species: Einfach, Doppelt und Dreifach; jede mit steigender Stimmenzahl",
        answerD = "Sechs Species mit ansteigender Note-gegen-Note-Dichte; die sechste Species beinhaltet Chromatik",
        correctAnswer = 1,
        explanation = "Johann Joseph Fux systematisierte in 'Gradus ad Parnassum' (1725) den Kontrapunkt in fuenf Gattungen (Species): 1. Note gegen Note (1:1), 2. Zwei Noten gegen eine (2:1), 3. Vier Noten gegen eine (4:1), 4. Synkopengebundener Kontrapunkt (Vorhalte), 5. Florid (gemischter Kontrapunkt). Dieses System blieb bis ins 20. Jahrhundert das grundlegende Lehrmodell (Haydn, Mozart, Beethoven lernten danach).",
        difficulty = 5,
        funFact = "Beethovens Studien bei Albrechtsberger nach dem Fux-System fuellten ganze Notizbucher. Er hatte aufgrund von Fux' Methode ein tiefes Verstaendnis des strengen Satzes, auch wenn er dessen Regeln in seinen eigenen Werken systematisch erweiterte."
    ),

    // Q8
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet man als 'Pedalpoint' (Orgelpunkt) in der kontrapunktischen Tradition, und welche dissonanten Zusammenklaenge sind dabei erlaubt?",
        answerA = "Ein langer gehaltener Ton in einer Stimme (meist Bass), ueber dem die anderen Stimmen frei modulieren und dabei Dissonanzen bilden duerfen, die gegenueberdem Pedalton auftreten",
        answerB = "Ein schnell wiederholter Ton im Pedal der Orgel, der ein Ostinato-Motiv erzeugt",
        answerC = "Der Cantus firmus in der Tenorstimme, der jeweils eine Ganze Note gehalten wird",
        answerD = "Eine Kontrapunktfigur, bei der alle Stimmen auf derselben Note verharren, waehrend nur der Rhythmus variiert",
        correctAnswer = 0,
        explanation = "Der Orgelpunkt (Pedalpoint) ist ein lang gehaltener (oder wiederholter) Ton, meist in der Bassstimme, ueber dem die anderen Stimmen harmonisch und melodisch frei weiterschreiten. Dadurch entstehen Dissonanzen (Septimen, None, Sekunden) zwischen dem Pedalton und den anderen Stimmen. Diese Dissonanzen sind erlaubt, weil sie durch die harmonische Bewegung der Oberstimmen aufgeloest werden — der Pedalton selbst konsoniert dann wieder mit dem letzten Akkord.",
        difficulty = 5,
        funFact = "Beethovens Schlusssatz der 'Waldstein-Sonate' op. 53 beginnt mit einem langen C-Dur-Orgelpunkt in der Bassstimme. Der Effekt des 'Schwebens' ueber einem stabilen Fundament ist eine der wirkungsvollsten Spannungstechniken der tonalen Musik."
    ),

    // Q9
    Question(
        categoryId = 5,
        questionText = "Was unterscheidet den 'strengen' vom 'freien' Kontrapunkt in der kompositorischen Praxis des 16.-18. Jahrhunderts?",
        answerA = "Im strengen Kontrapunkt sind alle Dissonanzen verboten; im freien Kontrapunkt sind Dissonanzen erlaubt, wenn sie vorbereitet und aufgeloest werden",
        answerB = "Strenger Kontrapunkt (Palestrina-Stil) basiert auf den Species-Regeln mit kontrollierten Dissonanztypen (Durchgang, Wechselnote, vorbereitete Dissonanz); freier Kontrapunkt erlaubt mehr Dissonanzfreiheit und chromatische Bewegungen (Bach-Stil)",
        answerC = "Strenger Kontrapunkt ist fuer Vokalmusik, freier Kontrapunkt fuer Instrumentalmusik — beide verbieten parallele Quinten",
        answerD = "Freier Kontrapunkt bezeichnet die Improvisation; strenger Kontrapunkt bezieht sich auf notierte Polyphonie",
        correctAnswer = 1,
        explanation = "Im strengen Kontrapunkt (nach dem Vorbild Palestrinas/Fux) werden Dissonanzen streng kontrolliert: nur als Durchgangstoene auf leichten Taktzeiten, Wechselnoeten oder vorbereitete Haltetoene (Vorhalte). Im freien Kontrapunkt (Barockstil, Bach) sind mehr Dissonanztypen erlaubt: unbetonte Durchgangstoene, nicht vorbereitete Dissonanzen in raschen Passagen, chromatische Baesse. Die Grundregeln (keine Parallelen von Quinten/Oktaven) gelten in beiden.",
        difficulty = 5,
        funFact = "Hugo Riemanns Unterscheidung zwischen 'Vokal-' und 'Instrumental-Kontrapunkt' im 19. Jahrhundert erfasste dieselbe Dichotomie unter anderen Begriffen. Pauline Viardots Unterrichtspraxis basierte auf dieser Unterscheidung."
    ),

    // Q10
    Question(
        categoryId = 5,
        questionText = "Welche kompositorische Technik bezeichnet Moritz Hauptmann (1853) mit 'Thematische Arbeit' und wie unterscheidet sie sich von der Fugenthemadurchfuehrung?",
        answerA = "Thematische Arbeit (motivische Entwicklung) fragmentiert und transformiert Motive aus dem Hauptthema in der Durchfuehrung; Fugenthemadurchfuehrung praesentiert das Thema unveraendert in verschiedenen Stimmen und Tonarten",
        answerB = "Thematische Arbeit ist ein Begriff aus der Zwoefltonmusik und bezeichnet die serielle Transformation einer Themenreihe",
        answerC = "Beide Begriffe sind synonym; Hauptmann uebertrug die Fugenterminologie auf die Sonatenform",
        answerD = "Thematische Arbeit bezieht sich auf das Entwickeln von Volksmelodien; Fugenthemadurchfuehrung auf die abstrakte Polyphonie",
        correctAnswer = 0,
        explanation = "Thematische Arbeit (engl. 'developing variation', Schoenbergs spaeterer Begriff) bezeichnet die motivisch-thematische Transformation in der Durchfuehrung einer Sonate: Motive werden verkuerzt, sequenziert, rhythmisch veraendert, umgekehrt. In der Fuge hingegen erscheint das Thema vollstaendig und weitgehend unveraendert in jeder Durchfuehrung — die Entwicklung geschieht durch harmonische Modulation und Stimmenverteilung, nicht durch motivische Veraenderung.",
        difficulty = 5,
        funFact = "Brahms' Entwicklungstechnik, die Schoenberg als 'Developing Variation' bezeichnete, kann als Synthese beider Prinzipien gesehen werden: Brahms kombiniert die Vollstaendigkeit des Fugenthemas mit der motivischen Flexibilitaet der thematischen Arbeit."
    ),

    // ── Musiksemiotik / Analyse (8) ──────────────────────────────────────────

    // Q11
    Question(
        categoryId = 5,
        questionText = "Was beschreibt Charles Sanders Peirces Zeichentriade 'Symbol, Index, Ikon' in der Anwendung auf Musik?",
        answerA = "Ikonische Zeichen ahmen etwas klanglich nach (z.B. Vogelruf), indexikalische Zeichen zeigen auf einen Ursprung (z.B. Tonartensymbolik), symbolische Zeichen sind konventionell und willkuerlich (z.B. Notenschrift)",
        answerB = "Symbol ist der Notentext, Index die Auffuehrung, Ikon die Idee des Komponisten",
        answerC = "Alle musikalischen Zeichen sind reine Symbole, weil Musik keine direkte Referenz auf ausserklaengliche Realitaet hat",
        answerD = "Peirces Triade ist nicht auf Musik anwendbar, weil Musik keine Referenzsemantik besitzt",
        correctAnswer = 0,
        explanation = "Nach Peirce (1839-1914): Ikonische Zeichen basieren auf Aehnlichkeit (Musik imitiert Donner, Vogelgesang). Indexikalische Zeichen weisen auf eine Ursache oder Beziehung hin (chromatische Abwaertsbewegung als Lamento-Index). Symbolische Zeichen sind konventionell (Notenschrift, Taktvorzeichnung). Musiksemiotiker wie Jean-Jacques Nattiez und Kofi Agawu nutzen diese Kategorien zur Analyse musikalischer Bedeutung.",
        difficulty = 5,
        funFact = "Nattiez unterscheidet zudem 'poietische' (Produktionsperspektive), 'neutral' (immanente Struktur) und 'aesthesische' (Rezeptionsperspektive) Dimensionen musikalischer Bedeutung — ein semiotisches Tripartitionsmodell fuer die Musikanalyse."
    ),

    // Q12
    Question(
        categoryId = 5,
        questionText = "Was versteht der Musiktheoretiker Leonard Ratner (1980) unter dem Begriff 'Topos' in der klassischen Musik des 18. Jahrhunderts?",
        answerA = "Ein konventionalisiertes musikalisches Ausdrucksmuster (z.B. 'Jagd', 'Sturm und Drang', 'Empfindsamkeit'), das der Hoerer des 18. Jahrhunderts sofort erkannte und mit bestimmten Bedeutungen assoziierte",
        answerB = "Das Hauptthema eines Satzes in der Sonatenform, das alle weiteren Motive topologisch ableitet",
        answerC = "Die tonale Struktur eines Menuetts, die als 'topographische Karte' der Harmonie gilt",
        answerD = "Ein Begriff aus der Rhetorischen Theorie, der auf kontrapunktische Figuren des Barock angewendet wird",
        correctAnswer = 0,
        explanation = "Ratner definiert in 'Classic Music' (1980) Topoi als konventionalisierte musikalische Ausdruckstypen: Menuett, Sturm und Drang, Pastoral, Alla Turca, Lamento, Fanfare usw. Haydn, Mozart und Beethoven bedienten sich dieser allgemein verstandenen Codes. Kofi Agawu erweiterte Ratners Theorie in 'Playing with Signs' (1991) und unterschied zwischen 'introversiven' (strukturellen) und 'extroversiven' (rhetorischen) Zeichen.",
        difficulty = 5,
        funFact = "Das Alla-Turca-Topos (tuerkische Militaermusik) war im Wien des 18. Jahrhunderts nach der Tuerkengefahr hoch populaer. Mozarts 'Tuerkischer Marsch' und Beethovens 'Ruinen von Athen' nutzten diesen Topos mit Triangel, Becken und Piccolo."
    ),

    // Q13
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet Heinrich Schenkers Begriff der 'Urlinie' in seiner Theorie der tonalen Musik?",
        answerA = "Die historisch erste melodische Linie, die einem Volkslied zugrunde liegt",
        answerB = "Die fundamentale Abwaertsbewegung der Oberstimme (von der Terz, Quinte oder Oktave zur Prim) im tiefsten Schicht-Niveau der musikalischen Struktur, die den gesamten tonalen Verlauf zusammenhaelt",
        answerC = "Die Basslinie eines Basso-continuo-Satzes in ihrer einfachsten Form ohne Verzierungen",
        answerD = "Der motivische Kern, aus dem alle melodischen Linien eines Werkes abgeleitet werden koennen",
        correctAnswer = 1,
        explanation = "Schenkers Schichtenlehre (in 'Der freie Satz', 1935) sieht die tiefste Schicht (Ursatz) als Gerueststruktur: Die Urlinie ist die schrittweise Abwaertsbewegung in der Oberstimme (z.B. 3-2-1 oder 5-4-3-2-1), kombiniert mit einem Bassbrechungsarpeggium (I-V-I). Alles in der Musik ist 'Auskomponierung' dieser Grundstruktur durch Prolongations- und Transformationsprozesse.",
        difficulty = 5,
        funFact = "Schenkers Methode ist umstritten: Kritiker werfen ihr vor, alle tonale Musik auf dasselbe Schema zu reduzieren. Begruendet er jedoch die 'organische Einheit' grosser Werke, wird er als einer der bedeutendsten Musiktheoretiker des 20. Jahrhunderts anerkannt."
    ),

    // Q14
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der semiotische Begriff 'Signifiant' vs. 'Signifie' (Saussure) in der Anwendung auf das Notationssystem?",
        answerA = "Signifiant ist das Klangbild (was man hoert), Signifie ist die abstrakte Tonhoehenkategorie — beides zusammen bildet das musikalische Zeichen nach Saussure",
        answerB = "Signifiant ist das graphische Notenzeichen, Signifie ist der bezeichnete Klang bzw. die musikalische Kategorie (z.B. 'vierte Stufe')",
        answerC = "Saussures Dichotomie ist fuer Musik irrelevant, weil Noten keine linguistischen Zeichen sind",
        answerD = "Signifiant bezeichnet den Kompositionsvorgang, Signifie das fertige Werk als Klangbild",
        correctAnswer = 1,
        explanation = "Ferdinand de Saussures Zeichenkonzept (Signifiant = Ausdruck/Form; Signifie = Inhalt/Konzept) wird in der Musiksemiotik auf die Notation angewendet: Das Notenzeichen (graphische Form) ist der Signifiant, die bezeichnete Klangkategorie (z.B. 'C-Dur-Dreiklang') ist das Signifie. Entscheidend: Die Beziehung ist arbitraer (willkuerlich) und konventionell — wie in Sprache. Musiksemiotiker wie Nattiez haben Saussures Modell kritisch erweitert.",
        difficulty = 5,
        funFact = "Die Arbitraritaet des musikalischen Zeichens wird besonders in der Transkulturalitaet sichtbar: Was in europaeischer Tradition als 'Terz' gilt, hat in anderen Musiksystemen (arabische Maqam, indisches Raga) eine voellig andere semantische Rolle."
    ),

    // Q15
    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'immanenter' und 'aussermusikalischer' (referentieller) Bedeutung in der Musiksemiotik nach Hanslick vs. Cooke?",
        answerA = "Hanslick (1854) besteht darauf, dass Musik nur sich selbst bedeutet ('toenend bewegte Formen'); Cooke (1959) argumentiert, dass bestimmte melodisch-harmonische Muster stabile Bedeutungen (Emotionen) repraesentieren",
        answerB = "Hanslick und Cooke sind sich einig, dass Musik emotionale Bedeutung hat; sie unterscheiden sich nur in der Theorie, wie diese Bedeutung entsteht",
        answerC = "Hanslick sieht Programmusik als bedeutsamer an; Cooke lehnt jede aussermusikalische Referenz ab",
        answerD = "Cooke beschraenkt seine Analyse auf Vokalmusik; Hanslick analysiert ausschliesslich Instrumentalmusik",
        correctAnswer = 0,
        explanation = "Eduard Hanslick ('Vom Musikalisch-Schoenen', 1854) ist der prototypische Formalismus-Vertreter: Musik bedeutet nichts ausser ihren eigenen strukturellen Beziehungen. Deryck Cooke ('The Language of Music', 1959) argumentiert umgekehrt: Bestimmte melodisch-harmonische Muster (z.B. absteigende Dur-Sexte = Sehnsucht) haben in der europaeischen Musikgeschichte stabile emotionale Bedeutungen.",
        difficulty = 5,
        funFact = "Cookes Theorie wurde empirisch von der Musikpsychologie teilweise bestaetigt (Molltonarten gelten international als 'trauriger'), aber auch kritisiert: Kulturelle und kontextuelle Faktoren sind mindestens genauso wichtig wie die musikalische Struktur selbst."
    ),

    // Q16
    Question(
        categoryId = 5,
        questionText = "Welche Analysetechnik wendet Fred Lerdahl und Ray Jackendoffs 'Generative Theory of Tonal Music' (GTTM, 1983) an?",
        answerA = "Schenker'sche Reduktion kombiniert mit transformationeller Grammatik aus der Linguistik Chomskys — das Hoerverstaendnis wird als Hierarchie von Gruppenstrukturen, metrischen Strukturen, Zeit-Span-Reduktion und prolongationaler Reduktion modelliert",
        answerB = "Fourier-Analyse des Klangspektrums kombiniert mit einem rekursiven Algorithmus zur Erkennung von Themen",
        answerC = "Semiotische Analyse nach Peirce, erganzt durch Corpus-Statistik ueber Haeufigkeit harmonischer Progressionen",
        answerD = "Statistische Auswertung von Hoerexperimenten, die dann in eine formale Grammatik uebersetzt werden",
        correctAnswer = 0,
        explanation = "GTTM (Lerdahl/Jackendoff 1983) modelliert das musikalische Verstaendnis als mehrschichtige Hierarchie, inspiriert von Chomskys Generativer Grammatik: 1. Grouping Structure (Wie werden Toene zu Phrasen gruppiert?), 2. Metrical Structure (Wo sind starke/schwache Taktzeiten?), 3. Time-Span Reduction (Welche Toene sind strukturell wichtig?), 4. Prolongational Reduction (Wie spannen Toene ueber grosse Zeitraeume?). Das Modell beschreibt, wie ein kompetenter Hoerer Musik intuitiv strukturiert.",
        difficulty = 5,
        funFact = "GTTM wurde seither von Computerwissenschaftlern implementiert: Algorithmen koennen nach den GTTM-Regeln metrische Strukturen automatisch analysieren. Dieser Ansatz verbindet Musiktheorie direkt mit der kuenstlichen Intelligenz-Forschung."
    ),

    // Q17
    Question(
        categoryId = 5,
        questionText = "Was beschreibt Roland Barthes' Konzept des 'Grain of the Voice' (Le grain de la voix, 1972) in der Musiksemiotik?",
        answerA = "Die technische Stimmqualitaet eines Saengers, gemessen in Formant-Frequenzen",
        answerB = "Das Koerperliche, Materielle der Stimme jenseits des Kommunizierten — die spuerbare Leiblichkeit der Stimmproduktion, die sich der reinen Bedeutungsuebertragung entzieht",
        answerC = "Die historische Entwicklung der Stimmtechnik vom Mittelalter bis zur Gegenwart",
        answerD = "Barthes' Ablehnung des Gesamtkunstwerks zugunsten der Autonomie des Klangs",
        correctAnswer = 1,
        explanation = "Barthes beschreibt 'grain' als das Leibliche der Stimme — das Reiben der Zunge am Gaumen, die Koerperlichkeit der Artikulation — das er dem 'pheno-song' (vermittelter Ausdruck) gegenueberstellt. Er bevorzugte Charles Panzerans rauhere Stimme gegenueber Dietrich Fischer-Dieskaus 'zu perfekter' Technik. 'Grain' ist das Nicht-Kodifizierbare, das der Semiotik widersteht.",
        difficulty = 5,
        funFact = "Barthes Essai beeinflusste die Popularmusikforschung erheblich: Simon Frith und andere nutzten das Konzept, um die Besonderheit von Saengern wie Bob Dylan oder Tom Waits zu beschreiben, deren 'Grain' wesentlich zu ihrer Bedeutung betraegt."
    ),

    // Q18
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Semiosis' in der Musikanalyse nach Nattiez, und welche drei Dimensionen unterscheidet er?",
        answerA = "Semiosis bezeichnet den Prozess, durch den ein Klang zum Zeichen wird; Nattiez unterscheidet poietische (Entstehung/Komposition), neutrale (immanente Struktur des Werkes) und aesthesische (Rezeption/Interpretation) Dimension",
        answerB = "Semiosis ist der Rezeptionsprozess; Nattiez unterscheidet kognitive, emotionale und motorische Reaktionen des Hoerers",
        answerC = "Semiosis bezeichnet die Transformation eines Notentextes in Klang — Nattiez teilt sie in Partitur, Aufgaben-Durchfuehrung und Ergebnis auf",
        answerD = "Semiosis ist Nattiez' Begriff fuer die Entstehung von Musikstilen; er unterscheidet Avantgarde, Tradition und Volksmusik",
        correctAnswer = 0,
        explanation = "Nattiez ('Musicologie generale et semiologie', 1987) uebernimmt Peirces Semiosis-Begriff und entwickelt ein tripartites Modell: 1. Poietische Dimension (wie der Komponist/Performer das Werk erzeugt), 2. Neutrale oder immanente Dimension (die physikalische/strukturelle Realitaet des Werkes), 3. Aesthesische Dimension (wie der Hoerer es wahrnimmt und interpretiert). Bedeutung entsteht im Wechselspiel aller drei Dimensionen.",
        difficulty = 5,
        funFact = "Nattiez' Analyse von Wagners 'Tristan und Isolde' in 'Wagner Androgyne' (1990) ist ein Paradebeispiel: Er zeigt, wie dasselbe Werk poietisch (als psychobiographisches Dokument Wagners), neutral (als harmonische Struktur) und aesthesisch (als erotisches Erleben) analysiert werden kann."
    ),

    // ── Alte Musik vor 1400 (8) ──────────────────────────────────────────────

    // Q19
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Organum purum' in der Notre-Dame-Schule des 12. Jahrhunderts und wie unterscheidet es sich vom 'Organum discantus'?",
        answerA = "Organum purum bezeichnet mehrstimmigen Gesang ohne Cantus firmus; Organum discantus ist einstimmig mit instrumentaler Begleitung",
        answerB = "Im Organum purum gleitet die Oberstimme in langen, melismatischen Boegen ueber einem gehaltenen Cantus-firmus-Ton; im Organum discantus bewegen sich beide Stimmen im messbar koordinierten Rhythmus (modalem Rhythmus)",
        answerC = "Organum purum ist nur fuer Zwei-Stimmigkeit zugelassen; Organum discantus meint Drei- und Vielstimmigkeit",
        answerD = "Organum purum verwendet ausschliesslich Perfekte Konsonanzen; Organum discantus erlaubt Terzen und Sexten als Konsonanzen",
        correctAnswer = 1,
        explanation = "In der Notre-Dame-Schule (Leonin, Perotin, ca. 1160-1250) bezeichnet Organum purum (oder Organum duplum) den Abschnitt, in dem der Tenor (Cantus firmus) einen Ton so lange haelt, bis die Oberstimme einen melismatischen Bogen abgeschlossen hat — der Rhythmus ist dort kaum notierbar. Organum discantus (oder Clausula) sind Abschnitte, in denen beide Stimmen im Modal-Rhythmus koordiniert sind.",
        difficulty = 5,
        funFact = "Leonins 'Magnus Liber Organi' (ca. 1170) ist eine Sammlung von Organa fuer das gesamte Kirchenjahr. Perotin ueberarbeitete und erweiterte Leonins Werk und schrieb die ersten datierbaren vierstimmigen Kompositionen ('Viderunt omnes', 'Sederunt principes', ca. 1198/1199)."
    ),

    // Q20
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Ars Nova' und welches Traktat definierte sie 1322/23 als musikalische Neuerung?",
        answerA = "Die Ars Nova (neue Kunst) bezeichnete das neue Rhythmus- und Notationssystem, das Philippe de Vitry in seinem gleichnamigen Traktat einfuehrte — Hauptneuerung: die Einfuehrung des Tempus imperfectum (binaere Einteilung) und neue Notenwerte",
        answerB = "Ars Nova ist ein Begriff des 15. Jahrhunderts fuer die flandrische Polyphonie unter Dufay und Binchois",
        answerC = "Philippe de Vitry pragte den Begriff als Reaktion auf Palestrinas Vokalstil im 16. Jahrhundert",
        answerD = "Ars Nova bezeichnet das neue Orchestrierungsprinzip des fruehen Barock (Generalbass, Basso continuo)",
        correctAnswer = 0,
        explanation = "Philippe de Vitry (1291-1361) schrieb das Traktat 'Ars Nova' (ca. 1322/23), das neue Notationsprinzipien einfuehrte: Das Tempus imperfectum (binaere Dreiteilung des Brevis) neben dem alten Tempus perfectum (ternear), neue Notenwerte (Minima/Semiminima) und eine systematischere Rhythmusdarstellung. Die Ars Nova stellte die Ars Antiqua (Notre-Dame-Schule und ihre Nachfolge) in Frage.",
        difficulty = 5,
        funFact = "Johannes de Muris schrieb parallel das Traktat 'Ars Novae Musicae' (1319/21) — die beiden Texte zusammen definierten die neue Stilepoche. Papst Johannes XXII. verurteilte 1324 in der Bulle 'Docta sanctorum' die neuen Rhythmen als frech und unangemessen fuer den Gottesdienst."
    ),

    // Q21
    Question(
        categoryId = 5,
        questionText = "Was ist 'Isorhythmie' und wie verwendete sie Guillaume de Machaut in seinen Motetten?",
        answerA = "Isorhythmie ist die Wiederholung desselben rhythmischen Musters (Talea) in der Tenorstimme, waehrend die Melodie (Color) sich ebenfalls nach einem separaten Zyklus wiederholt — beide Zyklen sind oft verschieden lang, was komplexe Ueberlagerungen schafft",
        answerB = "Isorhythmie bedeutet gleiche Rhythmik in allen Stimmen gleichzeitig (Homophonie), das Gegenteil von Polyphonie",
        answerC = "Die Verwendung von zwei identischen Rhythmusschemata in Ober- und Unterstimme ohne melodische Variaion",
        answerD = "Ein Notationsverfahren, bei dem rhythmische Werte durch Farbe (rot/schwarz) statt durch Notenformen unterschieden werden",
        correctAnswer = 0,
        explanation = "Die Isorhythmie (von griech. 'iso' = gleich) kombiniert zwei zyklische Strukturen im Tenor: die Talea (rhythmisches Muster) und den Color (Melodielinie). Diese Zyklen sind oft unterschiedlich lang — z.B. Talea mit 12 Toenen, Color mit 36 Toenen — was bei jeder Wiederholung neue Kombinationen erzeugt. Machaut vollendete dieses System und wandte es auch auf die Oberstimmen an ('pan-isorhythmische Motette').",
        difficulty = 5,
        funFact = "Machauts 'Messe de Notre Dame' (ca. 1365) ist die erste vollstaendige mehrstimmige Vertonung des Messordinarium durch einen bekannten Komponisten. Sie verwendet Isorhythmie im Gloria und Credo — ein Meilenstein der Musikgeschichte."
    ),

    // Q22
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Trittsystem' (Hexachord-System) des Guido von Arezzo und warum war es fuer die Musikpraxis des Mittelalters revolutionaer?",
        answerA = "Guido erfand um 1025 ein System von Hexachorden (Sechstongruppen) mit den Solmisationssilben ut-re-mi-fa-sol-la, das das Blattsingen (Solfeggio) und die Erinnerung an Tonhoehen revolutionierte und die Notationspraxis vereinheitlichte",
        answerB = "Das Trittsystem war ein Dreitonmodell (Trias = Terz, Quinte, Oktave) das Guido als Grundlage der Mehrstimmigkeit definierte",
        answerC = "Guido erfand ein Zehntonschema (Dekachord), aus dem er spaeters das Hexachord ableitet",
        answerD = "Das Trittsystem bezeichnet Guidons Klaviatur-Systematik, die erstmals chromatische Halbtöne in die Orgel einfuehrte",
        correctAnswer = 0,
        explanation = "Guido von Arezzo (ca. 990-1050) einfuehrte die Hexachord-Solmisation: Die Silben ut-re-mi-fa-sol-la galten fuer drei verschiedene Hexachorde (Naturale, Durum, Molle), die gemeinsam alle Toene des Systems abdeckten. Die Silbe 'mi-fa' markierte immer den Halbton — ein zentrales Orientierungsmerkmal. Das 'Mutationsverfahren' erlaubte den Wechsel zwischen Hexachorden bei grossen Melodiebogen.",
        difficulty = 5,
        funFact = "Die Silben 'ut-re-mi-fa-sol-la' stammen aus der ersten Strophe des Hymnus 'Ut queant laxis' (Johannes-Hymnus): Jede Phrase beginnt auf einem hoeheren Ton. Im 17. Jahrhundert wurde 'ut' durch 'do' ersetzt; 'si' (fuer B/H) kam als siebte Silbe hinzu."
    ),

    // Q23
    Question(
        categoryId = 5,
        questionText = "Was ist der 'Trecento'-Stil und welcher Komponist gilt als sein bedeutendster Vertreter?",
        answerA = "Trecento (ital. '14. Jahrhundert') bezeichnet die weltliche und geistliche Polyphonie Italiens 1300-1400; Francesco Landini (ca. 1325-1397) ist der wichtigste Vertreter mit seinen Ballaten und Madrigalen",
        answerB = "Trecento bezeichnet die norditalienische Instrumentalmusik des 13. Jahrhunderts mit Giovanni de Bologna als Hauptkomponist",
        answerC = "Der Trecento-Stil ist identisch mit dem franzoesischen Ars-Nova-Stil; Vitry ist sein bedeutendster Vertreter",
        answerD = "Trecento meint die fruehe venezianische Opernkomposition vor 1600, mit Claudio Monteverdi als Hauptfigur",
        correctAnswer = 0,
        explanation = "Die Trecento-Musik (ca. 1300-1420) ist die weltliche und geistliche Polyphonie Italiens im 14. Jahrhundert. Wichtige Gattungen: Madrigal (zweistimmig, oft ohne Cantus firmus), Caccia (zweis. Kanon mit Jagdthemen), Ballata (Tanzlied). Francesco Landini aus Florenz — blind seit der Kindheit — hinterliess ueber 150 Werke, vorwiegend Ballaten; er ist im Squarcialupi-Codex mit Portraet ueberliefert.",
        difficulty = 5,
        funFact = "Die 'Landini-Kadenz' (auch 'Landini-Klausel') ist eine charakteristische Schlusswendung: Vor der Schlusskadenz faellt die Oberstimme kurz auf die Sexte, bevor sie zur Oktave aufsteigt. Sie ist so praevalent im Trecento, dass sie nach Landini benannt wurde, obwohl er sie nicht erfunden hat."
    ),

    // Q24
    Question(
        categoryId = 5,
        questionText = "Was sind 'Neumen' in der mittelalterlichen Musiknotation und wann wurde die Liniensystem-Notation eingeführt?",
        answerA = "Neumen sind fruehe graphische Zeichen (Winkel, Haken, Wellenlinien) ueber dem Text, die Melodieverlauf und Vortragsgesten andeuteten, aber keine exakten Tonhoehen fixierten — Guido von Arezzo einfuehrte ca. 1025 das Vier-Linien-System zur genauen Tonhoehenfixierung",
        answerB = "Neumen sind die Linien des Notensystems; sie wurden von Arezzo als Sechslinien-System erfunden",
        answerC = "Neumen bezeichnen die fruehen chromatischen Zeichen (Kreuz und B) des gregorisnischen Chorals",
        answerD = "Neumen sind gleichbedeutend mit Ligaturen in der mensurierten Mehrstimmigkeit des 13. Jahrhunderts",
        correctAnswer = 0,
        explanation = "Neumen (von griech. 'neuma' = Wink, Geste) sind die altesten westeuropaeischen Musiknotationszeichen. Sogenannte 'adiastematische' Neumen (keine Linien) zeigten nur den ungefaehren Melodieverlauf und dienten der Gestutzung der Erinnerung. Guido von Arezzo einfuehrte ca. 1025 ein Linien-Notationssystem (zunachst 4 Linien), das exakte Tonhoehen fixierte — eine Revolution fuer das Erlernen und Notieren von Melodien.",
        difficulty = 5,
        funFact = "Die Notation der sogenannten 'Choralneuemen' entwickelte sich regional verschieden: Die Saint-Galler, Messiner, Aquitanischen und Visenzer Schulen hatten je eigene Zeichensysteme. Die Vereinheitlichung durch das Linien-System war ein langer historischer Prozess."
    ),

    // Q25
    Question(
        categoryId = 5,
        questionText = "Welche Bedeutung hatte das Konzept der 'Musica Mundana', 'Musica Humana' und 'Musica Instrumentalis' bei Boethius?",
        answerA = "Boethius (ca. 480-524) unterschied in 'De institutione musica' drei Ebenen: Musica Mundana (Sphaerenharmonie/Planetenordnung), Musica Humana (Harmonie von Koerper und Seele), Musica Instrumentalis (klingende Musik). Hoechste Stufe war die Musica Mundana als rein mathematisch-kosmologische Ordnung",
        answerB = "Boethius unterschied nur instrumentale und vokale Musik; das Sphaerenmodell stammt von Ptolemaeus",
        answerC = "Die drei Kategorien meinen Kirchenmusik, Hofmusik und Volksmusik als soziale Klassen der Musik",
        answerD = "Musica Mundana bezeichnet Weltmusik (Volksmusik anderer Kulturen), Musica Humana abendlaendische Kunstmusik",
        correctAnswer = 0,
        explanation = "Boethius' 'De institutione musica' (ca. 510) war das massgebliche Musiktheorie-Lehrbuch des europaeischen Mittelalters. Sein Dreiebenen-Modell (Pythagoreisch-neuplatonisch): Musica Mundana = der unhoerbare mathematische Einklang des Kosmos; Musica Humana = die Harmonie des menschlichen Organismus; Musica Instrumentalis = der hoerbare Klang. Paradoxerweise galt Instrumentalmusik als die niedrigste Stufe — vollkommene Erkenntnis war nur dem Verstand zugaenglich.",
        difficulty = 5,
        funFact = "Boethius bezeichnete den wahren Musiker als 'musicus' — jemanden, der Musik versteht und theoretisch reflektiert. Den blossen Ausfuehrenden nannte er 'cantor' oder 'artifex' — ein Handwerker ohne Verstaendnis. Diese Hierarchie pragte das Mittelalter stark."
    ),

    // Q26
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Conductus' und wie unterscheidet es sich von der mittelalterlichen Motette?",
        answerA = "Das Conductus (12.-13. Jh.) ist eine mehrstimmige Form auf einem neu komponierten Tenor (kein Cantus-firmus-Choral), mit identischem Text in allen Stimmen — die Motette hingegen verwendet einen Choral-Tenor und hat verschiedene Texte in den einzelnen Stimmen",
        answerB = "Conductus und Motette sind synonyme Begriffe fuer lateinische geistliche Polyphonie des 13. Jahrhunderts",
        answerC = "Das Conductus hat einen Cantus firmus, die Motette nicht — genau umgekehrt wie oft angenommen",
        answerD = "Das Conductus ist immer einstimmig; die Motette ist immer mehrstimmig",
        correctAnswer = 0,
        explanation = "Das Conductus ist eine polyphone Form des 12.-13. Jahrhunderts mit einem neu komponierten (nicht gregorianischen) Tenor und demselben Text in allen Stimmen — somit quasi-homophoner Text, auch wenn die Stimmen rhythmisch variieren. Die Motette hingegen verwendet einen choralbasierten Tenor (Cantus firmus) aus einem melismatischen Choralstueck und hat verschiedene Texte in den Oberstimmen — oft lateinisch und franzoesisch gleichzeitig (polytextuell).",
        difficulty = 5,
        funFact = "Der Begriff 'Conductus' leitet sich von 'conducere' (begleiten, fuehren) ab und bezieht sich moeglicherweise auf seine liturgische Funktion: das Geleiten von Geistlichen in der Prozession. Notre-Dame-Componenten wie Perotin schrieben sowohl Conducti als auch Organa."
    ),

    // ── Elektronische Musikgeschichte / Pioniere (8) ─────────────────────────

    // Q27
    Question(
        categoryId = 5,
        questionText = "Wer erfand das Theremin, wann wurde es erstmals vorgefuehrt, und was macht es spieltechnisch einzigartig?",
        answerA = "Leon Theremin (Lev Termen) praesentierte sein Instrument 1920 in Petrograd; es wird beruehrungslos durch Bewegung der Haende in zwei elektromagnetischen Feldern (Tonhoehe und Lautstaerke) gespielt — das erste elektronische Instrument dieser Art",
        answerB = "Robert Moog erfand das Theremin 1954 als Vorlaeufer seines Synthesizers; es hat eine Klaviatur",
        answerC = "Das Theremin wurde 1896 von Thaddeus Cahill erfunden und war urspruenglich ein Orgelersatz fuer oeffenliche Gebaeude",
        answerD = "Das Theremin ist ein russisches Volksinstrument, das 1917 elektrifiziert wurde",
        correctAnswer = 0,
        explanation = "Leon (Lev) Theremin (1896-1993) praesentierte sein Instrument 1920 in Petrograd und 1921 Lenin persoenlich. Es besitzt zwei Antennen: eine vertikale (Tonhoehe durch Naeherung) und eine horizontale Bogenantenne (Lautstaerke). Clara Rockmore galt als groesste Thereminispistin; das Instrument ist in der Filmmusik weit verbreitet (z.B. 'Spellbound' von Hitchcock, Herrmann's Noten fuer 'The Day the Earth Stood Still').",
        difficulty = 5,
        funFact = "Leon Theremin wurde 1938 vom sowjetischen Geheimdienst nach Moskau entfuehrt und arbeitete dort zwangsweise an Abhoergeraeten. Er erfand dabei das 'The Thing' — ein passives Abhoergeraet im Siegel der US-Botschaft. 1991 tauchte er bei einem Besuch in den USA auf und wurde als lebende Legende gefeiert."
    ),

    // Q28
    Question(
        categoryId = 5,
        questionText = "Was ist 'Musique concrète' und wer begruendete sie am Studio in Paris?",
        answerA = "Pierre Schaeffer entwickelte ab 1948 am Radiodiffusion Francaise (RTF) Paris die Musique concrète: Kompositionen aus aufgezeichneten Alltagsgeraeusche (konkrete Klange), die durch Manipulation (Transposition, Umkehrung, Schichtung) zu Musik wurden",
        answerB = "Musique concrète wurde von Pierre Boulez am IRCAM entwickelt und verwendet ausschliesslich Orchesterklange in synthetischer Veraenderung",
        answerC = "Karlheinz Stockhausen gruendete 1951 in Koeln das erste Studio fuer konkrete Musik; Schaeffer arbeitete in Koeln",
        answerD = "Musique concrète ist ein amerikanischer Begriff fuer experimentelle Computermusik der 1960er Jahre",
        correctAnswer = 0,
        explanation = "Pierre Schaeffer (1910-1995) begann 1948 am RTF Paris mit Experimenten: Er nahm Alltagsgeraeusche auf Schallplatte auf und manipulierte sie (Abspielen von hinten, Schleifen, Transposition). Sein 'Etude aux chemins de fer' (1948) gilt als erstes Werk der Musique concrète. 1958 veroeffentlichte er sein Hauptwerk 'Traite des objets musicaux', die theoretische Grundlegung seiner 'Solfege de l'objet sonore'.",
        difficulty = 5,
        funFact = "Schaeffers zentrales Konzept 'ecoute reduite' (reduziertes Hoeren) nach Husserls Phaenomenologie bedeutet: Einen Klang hoeren ohne Bezug zu seiner Quelle oder seiner Bedeutung — nur sein akustisches Wesen wahrnehmen. Dieses Konzept wurde zum Eckpfeiler der elektroakustischen Musikaesthetik."
    ),

    // Q29
    Question(
        categoryId = 5,
        questionText = "Welche Bedeutung hatte das WDR Studio fuer Elektronische Musik in Koeln (gegr. 1951) fuer die Entwicklung der elektronischen Musik?",
        answerA = "Es war das erste Studio, das ausschliesslich mit synthetisch erzeugten Toenen (Sinustoene, Rauschen, Impulse) arbeitete — im Gegensatz zur franzoesischen Musique concrète. Herbert Eimert und Karlheinz Stockhausen praegten es; Stockhausens 'Gesang der Juenglinge' (1955-56) ist sein beruehm testes Werk",
        answerB = "Das Koelner Studio wurde erst 1965 gegruendet und war das erste Studio weltweit fuer Computermusik",
        answerC = "WDR Koeln produzierte hauptsaechlich Rundfunk-Hoerspiele; elektronische Komposition war nur ein Nebenprodukt",
        answerD = "Das Studio wurde von Stockhausen als privatem Komponisten gegrundet und erst spaeter vom WDR uebernommen",
        correctAnswer = 0,
        explanation = "Das WDR Elektronische Studio (1951, Koeln) unter Herbert Eimert war das Gegenmodell zur Pariser Musique concrète: Man arbeitete mit rein synthetischen Klaengen (Generatoren, Sinustoene), waerendKoeln bald auch Natuerliches integrierte. Stockhausens 'Gesang der Juenglinge' (1955-56) vereinte erstmals synthetische Klaenge und manipulierte Kinderstimme — ein Schluesselwerk. Das Studio war jahrzehntelang Pilgerstatte fuer Komponisten weltweit.",
        difficulty = 5,
        funFact = "Stockhausens 'Kontakte' (1958-60) kombinierte live gespieltes Schlagzeug und Klavier mit viererkanaligem Tonband. Die raeumliche Rotation des Klangs um das Publikum herum war 1960 revolutionaer und nahm Surroundsound-Konzepte vorweg."
    ),

    // Q30
    Question(
        categoryId = 5,
        questionText = "Wer war Raymond Scott und welchen Beitrag leistete er zur Geschichte der elektronischen Musik?",
        answerA = "Raymond Scott (buergerlich Harry Warnow, 1908-1994) war ein US-amerikanischer Komponist und Erfinder, der bereits in den 1940er/50er Jahren eigenstaendige Sequenzer, Klangautomaten und einen fruehen Synthesizer ('Electronium') entwickelte — jahrzehntelang vergessen, heute als Pionier anerkannt",
        answerB = "Raymond Scott war der Begrunder des Moog-Synthesizer-Unternehmens",
        answerC = "Scott erfand das Mellotron — ein Bandspeicher-Tastatur-Instrument, das echte Orchester simulierte",
        answerD = "Raymond Scott war Direktor des Columbia-Princeton Electronic Music Center ab 1958",
        correctAnswer = 0,
        explanation = "Raymond Scott (1908-1994) war seiner Zeit weit voraus: Er baute in seinem privaten Labor in den 1940er-50ern automatische Klanginstrumente (den 'Clavivox', ein fruehes Keyboard-Synthesizer-Instrument), und spaeter das 'Electronium' — einen Synthesizer mit Sequencer und Klangspeicher, der autonom Musik erzeugen konnte. Als Motown-Musikdirektor nutzte er elektronische Mittel in der Popmusik. Sein Werk wurde erst posthum wiederentdeckt.",
        difficulty = 5,
        funFact = "Scotts Aufnahmen wie 'Powerhouse' und 'Twilight in Turkey' aus den 1930ern wurden so haeufig in Bugs-Bunny-Cartoons verwendet, dass sie heute ikonisch sind — kaum jemand weiss, dass ihr Schoepfer ein Elektronik-Pionier war."
    ),

    // Q31
    Question(
        categoryId = 5,
        questionText = "Was war das IRCAM in Paris und welche Rolle spielte Pierre Boulez bei seiner Gruendung?",
        answerA = "Das IRCAM (Institut de Recherche et Coordination Acoustique/Musique), gegr. 1977, ist das franzoesische Forschungsinstitut fuer Computermusik und Akustik; Pierre Boulez war Grunder und erster Direktor. Es wurde unter dem Centre Pompidou errichtet und ist Weltspitze in computergesteuerter Live-Elektronik",
        answerB = "Das IRCAM wurde 1963 von Pierre Schaeffer als Nachfolger seines RTF-Studios gegruendet",
        answerC = "IRCAM steht fuer ein amerikanisches Forschungszentrum in New York, das Boulez von Frankreich aus beriet",
        answerD = "Boulez hat das IRCAM nie geleitet; er unterrichtete dort nur als Gastdozent",
        correctAnswer = 0,
        explanation = "Pierre Boulez (1925-2016) betrieb Jahrzehnte, um das IRCAM zu verwirklichen. Eroffnet 1977 im Untergeschoss des Centre Pompidou, kombiniert es Wissenschaft und Komposition: Akustiker, Mathematiker und Komponisten arbeiten zusammen. Aus dem IRCAM stammen u.a. die IRCAM-Computersprachen 'CHANT' (Sprachsynthese) und 'MAX' (Miller Puckette, spaeter 'Max/MSP'). Kaija Saariaho, Jonathan Harvey, George Benjamin, Tod Machover schufen dort Hauptwerke.",
        difficulty = 5,
        funFact = "Boulez' eigenes Hauptwerk 'Repons' (1981) war das erste grosse oeffentliche IRCAM-Stueck: Sechs Solisten sitzen im Publikumsraum, das Orchester ist am Rand, und der Computer verarbeitet und transformiert den Klang der Solisten in Echtzeit — ein spektakulaeres raeumliches Klangerlebnis."
    ),

    // Q32
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Mellotron' und welche Bedeutung hatte es fuer die Rockmusik der 1960/70er Jahre?",
        answerA = "Das Mellotron (entwickelt ca. 1963) ist ein Bandspeicher-Tastatur-Instrument: Jede Taste loest ein kurzes Tonband-Fragment eines echten Instruments aus (Floetenensemble, Streicher, Chor). Es wurde von den Beatles, King Crimson und anderen praegende Bands verwendet",
        answerB = "Das Mellotron ist ein elektrisches Klavier mit Metallzungen, entwickelt von Harold Rhodes in den 1950ern",
        answerC = "Das Mellotron ist ein amerikanischer Begriff fuer den Moog Minimoog (1970), der so heisst, weil er 'mellifluous tones' erzeugt",
        answerD = "Das Mellotron war ein fruehes Computersystem fuer Musikproduktion, das IBM 1965 auf den Markt brachte",
        correctAnswer = 0,
        explanation = "Das Mellotron (Birmingham, ca. 1963) spielt voraufgenommene Tonband-Streifen ab — jede Taste hat ca. 8 Sekunden Band. Charakteristisch sind die Aufnahmen von Floetenensemble, Streichern und Chor. John Lennon hoerete es 1965 auf einer Partynach; Paul McCartney nutzte es in 'Strawberry Fields Forever' (1967). King Crimson's 'In the Court of the Crimson King' (1969) ist ein Mellotron-Meisterwerk.",
        difficulty = 5,
        funFact = "Das Mellotron-Band ist 8 Sekunden lang. Wenn eine Taste laenger als 8 Sekunden gedrueckt wird, gibt es keinen Ton mehr — was Spieler zwang, den Ton loszulassen und neu anzuschlagen. Diese Eigenschaft gab der Mellotron-Musik ihren charakteristischen, schwankenden Charakter."
    ),

    // Q33
    Question(
        categoryId = 5,
        questionText = "Was ist 'Phase Music' und welcher Komponist entwickelte sie in den 1960ern als eigene Technik?",
        answerA = "Steve Reich entwickelte ab 1965 die Phasen-Technik: Zwei oder mehrere identische Loops laufen leicht asynchron auseinander, wodurch sich immer neue rhythmische Muster ergeben — 'It's Gonna Rain' (1965) und 'Piano Phase' (1967) sind Hauptbeispiele",
        answerB = "Terry Riley erfand Phase Music 1964 als Bezeichnung fuer seine 'In C' — alle Spieler durchlaufen dieselben Phasen in unterschiedlichem Tempo",
        answerC = "Philip Glass entwickelte Phasentechnik als Bezeichnung fuer seine additive Prozesse in der fruehen Minimal Music",
        answerD = "Karlheinz Stockhausen erfand die Phasenmodulation in 'Mantra' (1970) als erste Phase-Music-Komposition",
        correctAnswer = 0,
        explanation = "Steve Reich entdeckte 1965 zufall ig die Phasing-Technik: Zwei Tonbandmaschinen mit demselben Loop liefen leicht asynchron auseinander. Er erkannte das kompositorische Potential und entwickelte es zu einer eigenstaendigen Methode. 'It's Gonna Rain' (1965, Tonband) und 'Come Out' (1966) sind Tonband-Phasenstucke; 'Piano Phase' (1967) uebertraegt das Prinzip auf zwei Klaviere, die dasselbe Pattern spielen, eines leicht schneller.",
        difficulty = 5,
        funFact = "Reichs Erlebnisbericht lautet: Als beide Bandmaschinen dieselbe Phrase spielten und eine langsam vorauseilte, entstand eine raeumliche Bewegung des Klangs zwischen zwei Lautsprechern — er beschrieb es als hypnotisches, meditatives Erlebnis, das ihn sofort fesselte."
    ),

    // Q34
    Question(
        categoryId = 5,
        questionText = "Was ist das Columbia-Princeton Electronic Music Center und welche historische Bedeutung hat es?",
        answerA = "Das 1959 gegruendete Columbia-Princeton Electronic Music Center in New York war das erste grosse amerikanische Studio fuer elektronische Komposition; Vladimir Ussachevsky, Otto Luening, Milton Babbitt und Edgar Varese arbeiteten dort. Der RCA Mark II Synthesizer (1957) war sein Herzstuck",
        answerB = "Es wurde von Edgar Varese 1940 als Partiturlager gegruendet und spaeters um Elektronik erweitert",
        answerC = "Das Center wurde 1970 von John Cage als aleatorisches Labor gegrundet",
        answerD = "Milton Babbitt leitete allein das Zentrum; Ussachevsky und Luening arbeiteten in Boston",
        correctAnswer = 0,
        explanation = "Das Columbia-Princeton Electronic Music Center (eroeffnet 1959) war das fuhrende amerikanische Zentrum fuer elektronische Komposition. Das Herzstuck war der RCA Mark II Sound Synthesizer (1957) — ein riesengroesses, prammables Geraet. Milton Babbitts seriell kontrollierte 'Composition for Synthesizer' (1961) ist ein Hauptwerk; Varese vollendete dort 'Poeme electronique' (1958, ausgefuehrt im Philips Pavilion der Expo 58).",
        difficulty = 5,
        funFact = "Der RCA Mark II Synthesizer war das erste Instrument, das vollstaendige serielle Kontrolle ueber alle musikalischen Parameter erlaubte. Babbitt nutzte diese Moeglichkeit, um totales Serialismus technisch zu realisieren — was mit Orchester praktisch unspielbar gewesen waere."
    ),

    // ── Musikcognition / Neurowissenschaft (8) ───────────────────────────────

    // Q35
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Absolute Pitch' (absolutes Gehoer) und wie haeufig kommt es in der Bevoelkerung vor?",
        answerA = "Absolutes Gehoer bezeichnet die Faehigkeit, Tonhoehen ohne Referenzton zu identifizieren oder zu produzieren. Es kommt bei etwa 1 von 10.000 Personen in westlichen Kulturen vor, ist aber in Populationen mit Tonsprachen (Mandarin, Vietnamesisch) mit bis zu 9% deutlich haeufiger",
        answerB = "Absolutes Gehoer ist die Faehigkeit, grosse Intervalle praeziese zu intonieren — es kommt bei ca. 30% aller Berufsmusiker vor",
        answerC = "Absolutes Gehoer bedeutet, alle Frequenzen im Hoerbereich wahrzunehmen (20 Hz bis 20 kHz) — es ist universal",
        answerD = "Absolutes Gehoer ist identisch mit 'perfektem Rhythmusgefuehl' und tritt bei Perkussionisten am haeufigsten auf",
        correctAnswer = 0,
        explanation = "Absolute Pitch (AP) erlaubt die muehlose Identifikation von Tonhoehen ohne Referenz. In westlichen Populationen ca. 1:10.000; in osasiatischen Populationen mit Tonsprachen bis zu 9%. Entscheidend: Fruehes musikalisches Training kombiniert mit sprachlicher Tonalit aet beguenstigt AP. Neurowissenschaftlich zeigen AP-Traeger eine groessere linke planum temporale und abweichende neuronale Netzwerke fuer Tonhoehenkategorisierung.",
        difficulty = 5,
        funFact = "Mozart, Beethoven und Bach hatten sehr wahrscheinlich absolutes Gehoer. Der Pianist Glenn Gould hatte es definitiv — er konnte Toene auf einem verstimmten Klavier mit erschreckender Genauigkeit benennen, ohne nachzudenken."
    ),

    // Q36
    Question(
        categoryId = 5,
        questionText = "Was ist 'Musical Anhedonia' und wie wurde sie neurowissenschaftlich untersucht?",
        answerA = "Musical Anhedonia ist das vollstaendige Fehlen emotionaler Reaktion auf Musik (kein 'Chills', keine Freude) bei ansonsten gesunden Menschen. Salimpoor et al. (2011) zeigten mit fMRT, dass normale Musikhoerer Dopaminausschuettung im Nucleus accumbens haben — bei Anhedonie-Patienten fehlt diese",
        answerB = "Musical Anhedonia ist ein anderer Begriff fuer Amusie — die Unfaehigkeit, Toene zu unterscheiden",
        answerC = "Musical Anhedonia bezeichnet Depressionssymptome, die durch Musik ausgeloest werden",
        answerD = "Musical Anhedonia ist die Abneigung gegen bestimmte Musikgenres — etwa klassische Musik — und gilt nicht als neuropsychologisches Phaenomen",
        correctAnswer = 0,
        explanation = "Musical Anhedonia (Beschrieben von Mas-Herrero et al., 2014) ist ein bisher wenig bekanntes Phaenomen: Betroffene sind neurologisch und emotional gesund, empfinden aber keine Freude an Musik. Josep Marco-Pallar und Kollegen entwickelten die Barcelona Music Reward Questionnaire (BMRQ). Neuroimaging zeigt reduzierte funktionale Konnektivitaet zwischen auditiven und belohnungsrelevanten Regionen (Nucleus accumbens, ventrales Striatum).",
        difficulty = 5,
        funFact = "Musical Anhedonia ist nicht das Gleiche wie Amusie (Toene nicht unterscheiden koennen) oder Misophonie (Geraeusche als aversiv erleben). Betroffene koennen Musik durchaus technisch analysieren — sie empfinden nur keine emotionale Belohnung."
    ),

    // Q37
    Question(
        categoryId = 5,
        questionText = "Was ist der 'ERAN' (Early Right Anterior Negativity) in der Musikcognitions-Forschung?",
        answerA = "Eine fruehe ERP-Komponente (ca. 150-200 ms nach einem harmonischen Regelverstos), die das schnelle, automatische Erkennen von harmonischen Irregularitaeten im Gehoersystem widerspiegelt — entdeckt von Stefan Koelsch und Kollegenn",
        answerB = "Eine spaete ERP-Komponente (ca. 600 ms), die das bewusste Erkennen von Melodiefehlern misst",
        answerC = "Eine motorische Komponente des EEG, die das Mitwippen des Fusses bei Musik misst",
        answerD = "Ein bildgebender Befund aus fMRT-Studien, der die rechte Amygdala bei Horrorfilmmusik aktiviert zeigt",
        correctAnswer = 0,
        explanation = "Der ERAN (Early Right Anterior Negativity) ist eine ERP-Komponente (Event-Related Potential), die ca. 150-200 ms nach einem harmonischen Regelverstos (z.B. ein Neapolitanischer Akkord im falschen Kontext) auftritt. Sie wird ueber rechtsfrontalen Elektroden gemessen und gilt als Indikator fuer die fruehe, praeatentive Verarbeitung harmonischer Irregularitaeten. Stefan Koelsch (MPI Leipzig) identifizierte und erforschte sie systematisch.",
        difficulty = 5,
        funFact = "Interessanterweise zeigt sich der ERAN auch bei Musikern, die keine theoretische Ausbildung haben — d.h. das harmonische Regelwissen wird implizit durch blosses Hoeren erworben, aehnlich wie Grammatik in der Sprachentwicklung."
    ),

    // Q38
    Question(
        categoryId = 5,
        questionText = "Was ist 'Entrainment' in der musikbezogenen Neurowissenschaft und welche Bedeutung hat es fuer soziales Verhalten?",
        answerA = "Entrainment bezeichnet das synchronisierte Mitfolgen neuronaler Oszillationen mit dem Rhythmus der Musik — es ist die neuronale Grundlage des Mitklatschen, Tanzen und musikalischen Zusammenspiels und foerdert soziale Bindung und Kooperation",
        answerB = "Entrainment ist ein Begriff aus der Musikpsychologie fuer das Zuhoeren ohne eigene motorische Reaktion",
        answerC = "Entrainment bezeichnet die Fachausbildung von Musikern in Ensemble-Spiel; es hat keine neurophysiologische Bedeutung",
        answerD = "Entrainment ist die emotionale Reaktion auf Musik, ausgeloest durch den Limbic arc",
        correctAnswer = 0,
        explanation = "Neurales Entrainment (auch 'neural oscillatory coupling') beschreibt das Mitfolgen neuronaler Schwingungen (delta, beta, gamma) mit dem Rhythmus externaliserter Stimuli. Bei Musik synchronisieren sich Bewegung, Atmung und neuronale Aktivitaet. Aniruddh Patel (2006) formulierte die 'OPERA-Hypothese': Musik nutzt dieselben Schaltkreise wie Sprache, was erklraet, warum Musik soziale Bindung foerdert — es ist die selbe Synchronisation, die Sprach-Gemeinschaft ermoeglicht.",
        difficulty = 5,
        funFact = "Gemeinsames Musizieren ('joint music-making') foerdert messbar prosoziales Verhalten: Kinder, die zusammen gesungen hatten, halfen einander bei darauffolgenden Aufgaben mehr als Kinder ohne Musikerfahrung — ein direkter Effekt von Entrainment auf Kooperation."
    ),

    // Q39
    Question(
        categoryId = 5,
        questionText = "Was ist 'Amusia' und welche zwei Haupttypen unterscheidet die Neuropsychologie?",
        answerA = "Amusia (Toentaubheit) ist die Unfaehigkeit, Musik normal zu verarbeiten; man unterscheidet kongenitale Amusia (angeboren, genetisch, ca. 4% der Bevoelkerung) und erworbene Amusia (nach Hirnlaesion, meist linke Hemisphare oder rechter frontale Kortex)",
        answerB = "Amusia ist Taubheit gegenueber rhythmischen Strukturen — eine Unterkategorie von Dyslexie",
        answerC = "Amusia bezeichnet ausschliesslich die Unfaehigkeit, Musik zu produzieren (keine Sangesmoeglichkeit), waehrend Rezeption intakt bleibt",
        answerD = "Amusia ist ein veralteter Begriff fuer Atonalitaet und hat keine neurologische Bedeutung",
        correctAnswer = 0,
        explanation = "Amusia bezeichnet eine beeintraechtigte Faehigkeit zur Musikverarbeitung. Kongenitale Amusia (Isabel Peretz, Montreal) betrifft ca. 4% der Bevoelkerung ohne bekannte Ursache: Betroffene koennen Tonhoehenkonturen nur schwer unterscheiden und erkennen vertraute Melodien nicht. Erworbene Amusia entsteht nach Schlaganfall oder Hirnverletzung — oft rechtshemisphaerisch. Die Dissoziation von Sprache und Musik (Musik gestort, Sprache intakt) zeigt, dass separate neuronale Systeme vorliegen.",
        difficulty = 5,
        funFact = "Der Komponist Maurice Ravel entwickelte spaet im Leben eine progressive Erkrankung (wahrscheinlich frontotemporale Demenz), die ihn unfaehig machte, Musik zu notieren, obwohl er sie in seinem Kopf hoerte. Er konnte 'Bolero' noch dirigieren, aber nicht mehr aufschreiben — ein dramatisches Beispiel fuer erworbene Amusia."
    ),

    // Q40
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Dopamin-Hypothese der Musik' und welche Studie von Salimpoor et al. (2011) bestaetigt sie?",
        answerA = "Valorie Salimpoor und Kollegen zeigten 2011 mit PET/fMRT, dass Musikgenuss Dopaminausschuettung im Striatum auslost — im Nucleus caudatus bei antizipierter Belohnung (pre-peak) und im Nucleus accumbens beim Klanggipfel (Chills/Frisson)",
        answerB = "Die Dopamin-Hypothese besagt, dass Musik direkt Adrenalin freisetzt und kein Dopamin involviert ist",
        answerC = "Salimpoor zeigte, dass nur Dur-Tonarten Dopamin freisetzen; Moll-Tonarten hemmen die Ausschuettung",
        answerD = "Die Studie von 2011 verwendete EEG und zeigte, dass Dopamin ausschliesslich in der Amygdala freigesetzt wird",
        correctAnswer = 0,
        explanation = "Salimpoor et al. (Nature Neuroscience, 2011) verwendeten PET mit dem Dopamin-Bindungsliganden [11C]raclopride kombiniert mit fMRT. Sie fanden: Wenn ein Musikhoerer 'Chills' (Gaensehaut) erlebt, wird Dopamin im ventralen Striatum (Nucleus accumbens, assoziiert mit Belohnung) freigesetzt. Bei Antizipation (kurz vor dem ersehnten Moment) aktiviert sich der Nucleus caudatus (assoziiert mit Vorfreude) — ein zeitlich differenzierter Dopaminprozess.",
        difficulty = 5,
        funFact = "Die Probanden waehlten selbst ihre persoenlichsten, 'chills'-ausloesenden Musikstuecke — kein Laborstimulus, sondern hochpersoenliche Klangmomente. Das Ergebnis: Musik aktiviert dasselbe Belohnungssystem wie Essen, Sex und Drogen — ein starkes Argument fuer den evolutionaeren Wert von Musik."
    ),

    // Q41
    Question(
        categoryId = 5,
        questionText = "Was ist 'Earworm' (Ohrwurm) aus neurowissenschaftlicher Perspektive und welche Theorie erklaert sein Entstehen?",
        answerA = "Ohrwurm (Involuntary Musical Imagery, INMI) ist das unfreiwillige Nachklingen einer Melodie im Kopf; James Kellaris' 'stuck song syndrome' und neuere Studien (Beaman, Williams) zeigen: Lieder mit einfachem, leicht variiertem, rhythmisch praegantem Muster sind am anfaelligsten — das Default Mode Network ist dabei aktiviert",
        answerB = "Ohrwurm ist ein Phaenomen, das ausschliesslich bei Musikern auftritt; Nicht-Musiker haben keine INMI",
        answerC = "Ohrwurm entsteht durch Schaedigung des auditiven Kortex und tritt nur bei Amusia-Patienten auf",
        answerD = "Ohrwurm ist kein neurowissenschaftliches Konzept, sondern ein umgangssprachlicher Begriff ohne wissenschaftliche Grundlage",
        correctAnswer = 0,
        explanation = "INMI (Involuntary Musical Imagery) betrifft 91% aller Menschen (Beaman & Williams, 2010). Eigenschaften anfaelliger Lieder: einfache, memorierbares Melodiemuster mit leichter Irregularitaet (Gaps im rhythmischen Schema), hohe Exposition (oft im Radio), emotionale Aufladung. Neuroimaging zeigt Aktivierung des auditorischen Kortex und des Default Mode Networks beim INMI — das Gehirn 'vervollstaendigt' unfertige Klangmuster automatisch.",
        difficulty = 5,
        funFact = "James Kellaris (2003) identifizierte den Zearfoss-Effekt: Wenn ein Lied mit einer Luecke (z.B. abgebrochene Phrase) gehort wird, versucht das Gehirn die Luecke zu fuellen — und bleibt dabei stecken. Das ist auch der Grund, warum vollstaendige Stuecke weniger INMI ausloesen als Songs, die man abgebrochen hoert."
    ),

    // Q42
    Question(
        categoryId = 5,
        questionText = "Was ist die 'OPERA-Hypothese' von Aniruddh Patel (2008) und welche evolutionaere Frage adressiert sie?",
        answerA = "OPERA (Overlap, Precision, Emotion, Repetition, Attention) erklaert, warum Musik das Sprechen plastisch verbessert: Musik und Sprache teilen neuronale Schaltkreise; Musik stellt hoehere Anforderungen (Praezision, Rhythmik), und die Emotion/Motivation durch Musik fuehrt zu Ubungswiederholung, die die Sprachschaltkreise verbessert",
        answerB = "OPERA ist eine Evolutionstheorie, die besagt, Musik sei eine Fehlanpassung ('spandrel') ohne eigenen Selektionsvorteil",
        answerC = "OPERA beschreibt das Gehirn-Netzwerk fuer Opern-Rezeption und zeigt, warum Saenger ein groesseres Broca-Areal haben",
        answerD = "Die OPERA-Hypothese widerspricht der Natuerlichkeit von Musik und behauptet, alle musikalischen Faehigkeiten seien rein kulturell erworben",
        correctAnswer = 0,
        explanation = "Aniruddh Patel ('Music, Language and the Brain', 2008) formulierte die OPERA-Hypothese als Erklarung fuer die Uberlappung von Sprach- und Musiknetzwerken. OPERA: O = Overlap (gemeinsame neuronale Strukturen), P = Precision (Musik erfordert praezisere Timingkontrolle als Sprache), E = Emotion (Musik motiviert und belohnt Ubung), R = Repetition (Musiker ueben Muster wiederholend), A = Attention (fokussierte Aufmerksamkeit bei Musikaktivitaet). Musiktraining verbessert dadurch tatsaechlich Sprachwahrnehmung.",
        difficulty = 5,
        funFact = "Patels Forschung zum Rhythmus zeigt: Von allen nicht-menschlichen Tieren synchronisieren nur Vogelarten mit vokaler Nachahmungsfahigkeit (z.B. Kakadus, Wellensittiche) zum Beat — Hunde, Affen und Elefanten ohne vokale Imitation koennen es nicht. Rhythmus-Entrainment scheint an Sprachlernen gekoppelt zu sein."
    ),

    // ── Musikinformatik / DSP (8) ─────────────────────────────────────────────

    // Q43
    Question(
        categoryId = 5,
        questionText = "Was besagt das Nyquist-Shannon-Abtasttheorem und welche Konsequenz hat es fuer die Audio-CD?",
        answerA = "Das Theorem (1928/1949) besagt: Ein Signal muss mit mindestens doppelter Frequenz des hoechsten enthaltenen Anteils abgetastet werden, um fehlerfrei rekonstruiert zu werden. Bei der CD (44.100 Hz Abtastrate) ergibt sich eine maximal darstellbare Frequenz von 22.050 Hz — etwas ueber dem menschlichen Hoerbereich (20 kHz)",
        answerB = "Das Nyquist-Theorem definiert die maximale Bittiefe einer digitalen Aufnahme (16 Bit bei CD-Standard)",
        answerC = "Das Theorem besagt, dass zwei gleichfrequente Signale destruktiv interferieren, wenn sie phasenverschoben sind",
        answerD = "Nyquist bewies, dass analoge Signale durch digitale Mittel nicht verlustfrei abgetastet werden koennen",
        correctAnswer = 0,
        explanation = "Harry Nyquist (1928) und Claude Shannon (1949) formulierten: Abtastfrequenz f_s >= 2 * f_max (Nyquist-Bedingung). Die CD-Norm (44.100 Hz, 16 Bit) wurde 1980 von Sony/Philips festgelegt: 44.100 Hz liegt sicher ueber 2 * 20.000 Hz. Aliasing-Artefakte (Falschfrequenzen) entstehen, wenn f_s < 2 * f_max — deshalb werden vor der Digitalisierung Anti-Aliasing-Filter eingesetzt.",
        difficulty = 5,
        funFact = "Die 44.100-Hz-Zahl hat einen praktischen Ursprung: Early digital recording used U-matic video tape machines. NTSC (US) Video hatte 30 fps x 490 Lines x 3 samples = 44.100 samples/sec — die Audioingenieure nutzten das Video-Format fuer die erste Digitalspeicherung von Ton."
    ),

    // Q44
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Diskrete Fourier-Transformation' (DFT) und welche Bedeutung hat sie fuer die Musikanalyse?",
        answerA = "Die DFT zerlegt ein zeitdiskretes Signal in seine Frequenzkomponenten (Magnitudenspektrum): Gegeben N Abtastwerte ergibt die DFT N komplexe Koeffizienten, die Amplitude und Phase jeder Frequenzkomponente beschreiben. In der Musikanalyse wird sie (via FFT) zur Tonhoehenerkennung, Spektralanalyse und Klangfarbenbeschreibung eingesetzt",
        answerB = "Die DFT ist ein Algorithmus zur Komprimierung von Audio-Dateien durch Quantisierung der Frequenzinformation",
        answerC = "Die DFT misst die zeitliche Varianz eines Audiosignals und erkennt daraus den Taktmetrum",
        answerD = "Die Diskrete Fourier-Transformation wandelt Tonsymbole (Noten) aus einer Partitur in Klangwellen um",
        correctAnswer = 0,
        explanation = "Die DFT (Joseph Fourier, mathematisch formuliert 1822; diskrete Version fuer digitale Signale) nutzt die Formel X[k] = Summe x[n] * e^(-j2pikn/N). Cooley und Tukey (1965) entwickelten die Fast Fourier Transform (FFT) als effizienten Algorithmus (O(N log N) statt O(N^2)). In der Musikinformatik ist FFT die Grundlage von Spektrogrammen (Zeit-Frequenz-Darstellung), Tonhoehenerkennung und Timbre-Analyse.",
        difficulty = 5,
        funFact = "Die STFT (Short-Time Fourier Transform) loest das Problem der zeitlichen Aufloesung der FFT: Sie wendet die FFT auf kurze, sich ueberlappende Fenster an und erzeugt ein Spektrogramm — die 'Partitur des Klangs' in Frequenz x Zeit, die man in jedem Audio-Editor sieht."
    ),

    // Q45
    Question(
        categoryId = 5,
        questionText = "Was ist 'Pitch Detection' (Tonhoehenerkennung) in der Musikinformatik und welche zwei grundlegende Methoden gibt es?",
        answerA = "Zeitbereich-Methoden (z.B. Autocorrelation, YIN-Algorithmus): Messen die Periodizitaet des Signals direkt in der Zeit. Frequenzbereich-Methoden (z.B. FFT-basierte Cepstral-Analyse, HPS): Analysieren das Spektrum und identifizieren Harmonische. Beide haben verschiedene Trade-offs in Latenz, Praezision und Robustheit",
        answerB = "Pitch Detection verwendet ausschliesslich neuronale Netzwerke (CNN) und hat keine klassische algorithmische Methode",
        answerC = "Pitch Detection ist nur fuer Sinustoene definiert; bei realen Instrumenten mit Obertounen ist sie nicht moeglich",
        answerD = "Pitch Detection und Beat Tracking sind identische Algorithmen mit unterschiedlichem Anwendungsfall",
        correctAnswer = 0,
        explanation = "Zeitbereich: Autocorrelation findet die Periode durch Korrelation des Signals mit sich selbst; der YIN-Algorithmus (de Cheveigne & Kawahara, 2002) verbessert Autocorrelation durch Differenz-Funktion und kumulierte Normalisierung. Frequenzbereich: Cepstrale Analyse (IFFT des logarithmischen Spektrums) und Harmonic Product Spectrum (HPS, Schroeder 1968) multiplizieren Spektrum mit sich selbst bei harmonischen Verhaeltnissen. Moderne Systeme (CREPE, 2018) nutzen CNNs.",
        difficulty = 5,
        funFact = "Der YIN-Algorithmus (benannt nach dem Yin-Yang-Symbol) gilt noch immer als Benchmark fuer monophone Pitcherkennung. CREPE (Convolutional Representation for Pitch Estimation, Google Brain 2018) uebertrifft ihn bei polyphonen Signalen erheblich, braucht aber 10.000x mehr Rechenleistung."
    ),

    // Q46
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Mel-Frequenz-Cepstrum' (MFC) und warum wird es in der Sprachsynthese und Musikerkennung verwendet?",
        answerA = "MFCCs (Mel-Frequency Cepstral Coefficients) sind Merkmale, die das Klangspektrum auf der nichtlinearen Mel-Skala (die die menschliche Tonhoehenwahrehmung nachbildet) darstellen und dann das Cepstrum berechnen — sie sind robust gegenueber Lautstaerkeaenderungen und beschreiben Klangfarbe kompakt",
        answerB = "MFCCs sind die Fourier-Koeffizienten eines Signals in der Mel-Skala — identisch mit dem Spektrogramm, aber kleiner",
        answerC = "Das Mel-Cepstrum ist ein Verfahren aus den 1990ern zur verlustbehafteten Audio-Kompression (Vorlaeufer von MP3)",
        answerD = "MFCCs messen ausschliesslich den Grundton (Pitch) eines Klangsignals in logarithmischer Form",
        correctAnswer = 0,
        explanation = "Mel-Frequency Cepstral Coefficients (MFCCs, Davis & Mermelstein 1980) imitieren die Nichtlinearitaet des menschlichen Gehoers: Die Mel-Skala (Stevens et al. 1937) ist bei tiefen Frequenzen linear und bei hohen logarithmisch. Das MFCC-Verfahren: 1. FFT, 2. Mel-Filterbank anwenden, 3. Log-Kompression, 4. Discrete Cosine Transform (DCT). Das Ergebnis sind typisch 13-40 Koeffizienten, die Klangfarbe kompakt repraesentieren — Kern aller Spracherkennungs- und Musikklassifikationssysteme.",
        difficulty = 5,
        funFact = "MFCCs wurden urspruenglich fuer Spracherkennung entwickelt und erst in den 1990ern fuer Musik adaptiert. Die meisten Shazam-aehnlichen Audiofingerprint-Systeme und Musikgenreklassifizierer basieren auf MFCC-Varianten als Kern-Features."
    ),

    // Q47
    Question(
        categoryId = 5,
        questionText = "Was ist 'Beat Tracking' und welche Algorithmen gelten als Standard in der Musikinformatik?",
        answerA = "Beat Tracking erkennt den musikalischen Pulsschlag in Echtzeit oder im offline-Modus; klassische Methoden: Onset-Detection (Energiesprunge im Spektrum) gefolgt von Comb-Filter oder Dynamic Programming (Dixon 2001, 'BeatThis'); neuere Systeme (Madmom, Bock et al. 2016) nutzen Recurrent Neural Networks",
        answerB = "Beat Tracking ist ein Hardware-Algorithmus in Drummachines, der MIDI-Takt synchronisiert",
        answerC = "Beat Tracking erkennt Melodien und Harmonieverlaeufe — nicht den Rhythmus",
        answerD = "Beat Tracking ist identisch mit BPM-Messung und liefert nur eine Zahl (Tempoangabe), keine zeitgenauen Taktschlaege",
        correctAnswer = 0,
        explanation = "Beat Tracking besteht aus zwei Schritten: 1. Onset Detection (wo beginnen Noten/Transients?) durch Spectral Flux, High Frequency Content, Complex Domain; 2. Tempo- und Beat-Schlaetze schaetzen, z.B. durch Dynamic Programming (Viterbi-Algorithmus ueber wahrscheinliche Beatpositionen). Madmom (Bock et al., 2016) nutzt Bidirektionale RNNs und ist aktuell Benchmark. Anwendung: DJ-Synchronisation, Musikeditor, automatische Transkription.",
        difficulty = 5,
        funFact = "Das Tracking menschlicher Musikausfuehrung ist besonders schwierig, weil echte Musiker systematisch 'rubato' spielen — sie eilen oder zoegern bewusst vor/nach dem metrischen Schlag. Algorithmen muessen zwischen 'gesuchte Tempoflexibilitaet' und 'echtem Tempowechsel' unterscheiden."
    ),

    // Q48
    Question(
        categoryId = 5,
        questionText = "Was ist MP3 aus technischer Sicht und auf welchem psychoakustischen Modell basiert es?",
        answerA = "MP3 (MPEG-1 Audio Layer III, ISO/IEC 1992) ist ein Audiokompressionsformat, das auf dem psychoakustischen Maskierungseffekt basiert: Lautere Toene 'maskieren' leise Toene in aehnlicher Frequenzregion. Das Codec berechnet, welche Frequenzanteile unhoorbar sind, und verwirft sie — dadurch reduziert sich die Datenmenge drastisch (10:1 bis 12:1)",
        answerB = "MP3 komprimiert Audio durch Quantisierung jedes Abtastwerts auf 8 Bit (statt 16 Bit bei CD)",
        answerC = "MP3 ist ein verlustfreies Format, das identisch mit der Original-WAV-Datei ist, aber platzsparender codiert",
        answerD = "MP3 basiert auf Wavelets statt Fourier-Transformation und hat daher bessere Zeitaufloesung als CD",
        correctAnswer = 0,
        explanation = "MP3 (Karlheinz Brandenburg et al., Fraunhofer IIS, 1989-1993) nutzt das psychoakustische Maskierungsmodell: Simultane Maskierung (lauter Ton bei 1000 Hz macht benachbarte Frequenzen unhoorbar) und temporale Maskierung (nach einem lauten Geraeusch sind leise Geraeusche kurzfristig unhoorbar). Das MDCT (Modified Discrete Cosine Transform) zerteilt das Signal in Frequenzbaender; Teile unter der Maskierungsschwelle werden weggelassen. Bei 128 kbps ist die Datenmenge ca. 1/11 der CD.",
        difficulty = 5,
        funFact = "Karlheinz Brandenburg arbeitete als Doktorand bei Juergen Herre am IIS Erlangen. Das erste gesampelte Lied, das er zur Qualitaetskontrolle seines Algorithmus verwendete, war Suzanne Vegas 'Tom's Diner' — weshalb sie scherzhaft als 'Mutter des MP3' bezeichnet wird."
    ),

    // Q49
    Question(
        categoryId = 5,
        questionText = "Was ist 'Score Following' in der Musikinformatik und welche Algorithmen werden eingesetzt?",
        answerA = "Score Following ist die Echtzeit-Synchronisation zwischen einer Partitur und einer live gespielten Auffuehrung: Ein System liest fortlaufend den Klang und positioniert sich in der Partitur. Klassische Methoden: Hidden Markov Models (HMM), Dynamic Time Warping (DTW); moderne Systeme nutzen Attention-basierte neuronale Netzwerke",
        answerB = "Score Following bezeichnet das automatische Transponieren einer Partitur waehrend der Auffuehrung",
        answerC = "Score Following ist ein Hardware-System in digitalen Pianos, das Noten visuell auf dem Display anzeigt",
        answerD = "Score Following ist die Zuordnung von Taktstrichen zu Zeitpunkten in einer Audio-Aufnahme",
        correctAnswer = 0,
        explanation = "Score Following (Dannenberg & Raphael, 1984ff.) loest das Problem: Wo in der Partitur ist der Musiker gerade? Bei Echtzeit-Performance werden Onset-Merkmale extrahiert und mit der Partitur verglichen. HMM behandelt die Partitur als Zustandssequenz; der Beobachtungsvektor (aus Audio) wird dem wahrscheinlichsten Zustand zugeordnet. DTW (Dynamic Time Warping) erlaubt nichtlineare Zeitverzerrung. Anwendung: Automatische Seitenumblatt-Systeme, interaktive Begleit-Systeme (Antescofo).",
        difficulty = 5,
        funFact = "Das System 'Antescofo' (IRCAM, Marc Chemillier und Arshia Cont) ist das fortschrittlichste Score-Following-System: Es koppelt Score Following mit einer eigenen reaktiven Programmiersprache fuer elektronische Echtzeitbegleitung, die von Komponisten wie Philippe Manoury genutzt wird."
    ),

    // Q50
    Question(
        categoryId = 5,
        questionText = "Was ist 'Music Information Retrieval' (MIR) und welche Hauptaufgaben umfasst es?",
        answerA = "MIR ist das interdisziplinaere Forschungsfeld, das automatische Analyse, Suche und Beschreibung von Musik durch Algorithmen untersucht. Hauptaufgaben: Tonhoehen-/Akkorderkennung, Genreklassifikation, Stimmungsanalyse (Valence/Arousal), Ahnlichkeitssuche, automatische Transkription, Quelltrennung (Source Separation)",
        answerB = "MIR bezeichnet das Digitalisierungsprojekt fuer historische Musikhandschriften in Bibliotheken",
        answerC = "MIR ist ein proprietaeres System von Spotify zur Empfehlung von Musiktiteln, nicht ein akademisches Forschungsfeld",
        answerD = "MIR beschraenkt sich auf die Suche nach Musik anhand von Text-Tags und hat keine Verbindung zur Audioanalyse",
        correctAnswer = 0,
        explanation = "MIR (Ende 1990er entstanden, ISMIR-Konferenz seit 2000) verbindet Signalverarbeitung, maschinelles Lernen, Musikwissenschaft und kognitive Wissenschaft. Kernaufgaben: Automatic Chord Recognition (ACR), Melody Extraction, Beat Tracking, Genre/Mood Classification, Music Similarity (Fingerprinting a la Shazam), Source Separation (Stimme von Instrumenten trennen), Automatic Music Transcription (AMT). Datensets: RWC, MIREX, MusicNet, MAESTRO.",
        difficulty = 5,
        funFact = "Sanjay Ghemawat und Shreyans Bhatt entwickelten bei Google den Music Genome Project-Konkurrenten durch MIR-Techniken. Das Music Genome Project (Pandora) codiert 450+ Attribute pro Song haendisch — ein Hybrid aus menschlicher Expertise und maschineller Analyse, der heute als Hybridansatz gilt."
    )
)
