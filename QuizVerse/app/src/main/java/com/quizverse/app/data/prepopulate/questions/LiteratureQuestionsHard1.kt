package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun literatureQuestionsHard1(): List<Question> = listOf(

    // Question 1 — Stilmittel: Chiasmus
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welches Stilmittel liegt in Goethes Faust vor: 'Die Kunst ist lang; und kurz ist unser Leben'?",
        answerA = "Parallelismus",
        answerB = "Chiasmus",
        answerC = "Antithese",
        answerD = "Anapher",
        correctAnswer = 1,
        explanation = "Das ist ein Chiasmus: Die Satzglieder werden in umgekehrter Reihenfolge wiederholt (ABBA-Schema). Subjekt-Verb kehrt sich um zu Verb-Subjekt. Gleichzeitig liegt eine Antithese vor, aber das prägende Strukturmerkmal ist der Chiasmus.",
        difficulty = 3,
        funFact = "Der Begriff 'Chiasmus' leitet sich vom griechischen Buchstaben Chi (X) ab, der die Kreuzstruktur symbolisiert. Schiller nutzte dasselbe Muster in 'Maria Stuart': 'Ihr Leben ist dein Tod! Ihr Tod dein Leben!'"
    ),

    // Question 2 — Stilmittel: Oxymoron vs. Paradoxon
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was ist der entscheidende Unterschied zwischen einem Oxymoron und einem Paradoxon?",
        answerA = "Ein Oxymoron steht in der Lyrik, ein Paradoxon in der Prosa",
        answerB = "Das Paradoxon verbindet zwei Wörter, das Oxymoron zwei Sätze",
        answerC = "Das Oxymoron verbindet zwei gegensätzliche Wörter direkt, das Paradoxon formuliert einen scheinbar unsinnigen Gedanken, der eine tiefere Wahrheit birgt",
        answerD = "Beide Begriffe bezeichnen dasselbe Phänomen mit unterschiedlicher Herkunft",
        correctAnswer = 2,
        explanation = "'Hassliebe' ist ein Oxymoron — zwei Wörter werden direkt verknüpft. 'Weniger ist mehr' ist ein Paradoxon — ein scheinbar widersprüchlicher Satz, der bei näherer Betrachtung wahr ist. Das Oxymoron ist gewissermaßen ein komprimiertes Paradoxon auf Wortebene.",
        difficulty = 3,
        funFact = "Im Barock liebten Dichter wie Andreas Gryphius das Paradoxon als theologisches Ausdrucksmittel: 'Ich leb' und weiß nicht wie lang' spiegelt die religiöse Vergänglichkeitsangst der Epoche wider."
    ),

    // Question 3 — Stilmittel: Metonymie
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "In welchem Beispiel liegt eine Metonymie vor?",
        answerA = "'Ich habe gestern Schiller gelesen' (Autor steht für sein Werk)",
        answerB = "'Das Herz des Unternehmens schlägt' (Herz für Zentrum)",
        answerC = "'Alle Hände auf Deck' (Teil für Ganzes)",
        answerD = "'Er hat goldene Hände' (konkret für abstrakte Eigenschaft)",
        correctAnswer = 0,
        explanation = "Bei der Metonymie steht ein Begriff für einen anderen, mit dem er in einer realen sachlichen Beziehung steht (Autor→Werk). Option B ist eine Metapher (Bedeutungsübertragung ohne realen Zusammenhang). Option C ist eine Synekdoche (Teil für Ganzes). Option D ist ebenfalls eine Metapher.",
        difficulty = 3,
        funFact = "Die Metonymie ist im Alltag allgegenwärtig: 'Das Weiße Haus hat gesprochen' (Gebäude für US-Regierung), 'Die Krone entschied' (Symbol für Monarchie). Linguisten wie Roman Jakobson sehen Metonymie und Metapher als die zwei Grundpole sprachlicher Bedeutungsübertragung."
    ),

    // Question 4 — Stilmittel: Synekdoche
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welche der folgenden Formulierungen ist eine Synekdoche in der Form 'Totum pro parte' (Ganzes für Teil)?",
        answerA = "'Alle Hände an Deck!' (Hände für Matrosen)",
        answerB = "'Das Dach über dem Kopf' (Dach für Haus)",
        answerC = "'Er hat kein Brot' (Brot für Nahrung allgemein)",
        answerD = "'Deutschland hat das Finale verloren' (Land für Nationalmannschaft)",
        correctAnswer = 3,
        explanation = "Bei 'Totum pro parte' steht das Ganze (Deutschland = das ganze Land) für einen Teil (die Nationalelf). Optionen A und C sind 'Pars pro toto' (Teil für Ganzes). Option B ist eine Metonymie (räumliche/gegenständliche Beziehung: Dach = Obdach).",
        difficulty = 3,
        funFact = "Die Synekdoche ist die 'Schwester' der Metonymie und wird oft mit ihr verwechselt. Aristoteles zählte beide noch zur Metapher. Erst die Rhetorik der Renaissance trennte sie als eigenständige Figuren."
    ),

    // Question 5 — Stilmittel: Zeugma
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "In welchem Satz liegt ein Zeugma vor?",
        answerA = "'Er lief schnell und rannte fort'",
        answerB = "'Er verlor sein Herz und seinen Schlüssel'",
        answerC = "'Komm, geh, lauf!' (drei Verben in Folge)",
        answerD = "'Wie der Vater, so der Sohn' (Parallelismus)",
        correctAnswer = 1,
        explanation = "Das Zeugma verbindet ein Verb mit mehreren Objekten, obwohl es nur zu einem davon semantisch korrekt passt. 'Er verlor sein Herz' (im übertragenen Sinn: verliebte sich) und 'seinen Schlüssel' (wörtlich) — das Verb 'verlor' verbindet beide auf ungleiche Weise, was eine komische oder pointierende Wirkung erzeugt.",
        difficulty = 3,
        funFact = "Das Zeugma kommt vom griechischen 'zeugma' = Joch/Verbindung. Heinrich Heine meisterte dieses Stilmittel: 'Er schlug die Trommel auf und nieder / Er weckte seine alten Brüder' — das Verb 'wecken' wirkt wörtlich und übertragen zugleich."
    ),

    // Question 6 — Stilmittel: Katachrese
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was ist eine Katachrese in ihrer zweiten, literarischen Bedeutung?",
        answerA = "Eine bewusst falsch verwendete Metapher zum Zweck der Komik durch das Mischen zweier unvereinbarer Bilder",
        answerB = "Ein veralteter Begriff für die Personifikation",
        answerC = "Dasselbe wie in Antwort A, aber auch: die ursprüngliche Bedeutung bezeichnet die 'lexikalisierte Metapher' (z.B. 'Tischbein'), bei der ein Ausdruck aus einem Bereich auf einen anderen übertragen wurde, um eine Lücke zu füllen",
        answerD = "Eine Figur, bei der ein Wort aus einem Fremdtext eingesetzt wird",
        correctAnswer = 2,
        explanation = "Die Katachrese hat zwei Bedeutungen: (1) die 'tote Metapher' oder lexikalisierte Metapher wie 'Tischbein' oder 'Fuß des Berges', bei der die bildhafte Übertragung so eingeschliffen ist, dass man sie nicht mehr als Bild wahrnimmt. (2) Die bewusste Vermischung unvereinbarer Bilder: 'Auch ein blindes Huhn hat Gold im Mund' (aus 'blindes Huhn findet ein Korn' + 'nicht auf den Mund gefallen').",
        difficulty = 3,
        funFact = "Katachrese stammt vom griechischen 'katachresthai' = missbrauchen. Im Stilistik-Unterricht wird oft nur die zweite Bedeutung (Bildbruch) gelehrt, obwohl Linguisten die erste (lexikalisierte Metapher) als bedeutsamer betrachten."
    ),

    // Question 7 — Erzähltechnik: Erlebte Rede
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welches grammatische Merkmal kennzeichnet die 'erlebte Rede' im Gegensatz zum inneren Monolog?",
        answerA = "Dritte Person und Präteritum — der Erzähler bleibt präsent, aber die Gedanken der Figur werden ohne Anführungszeichen wiedergegeben",
        answerB = "Erste Person und Präsens — die Figur spricht direkt zum Leser",
        answerC = "Konjunktiv II und dritte Person — unechte indirekte Rede",
        answerD = "Zweite Person und Futur — der Erzähler spricht die Figur direkt an",
        correctAnswer = 0,
        explanation = "Die erlebte Rede verwendet die dritte Person und das Präteritum, aber ohne Redeeinleitung ('er dachte'). Dadurch verschmilzt die Erzählerstimme mit der Innenwelt der Figur. Beispiel: 'Er schaute auf die Uhr. Schon so spät! Heute würde er es nicht mehr schaffen.' — kein 'er dachte', aber dennoch seine Gedanken.",
        difficulty = 3,
        funFact = "Die erlebte Rede wurde von dem Stilistiker Charles Bally als 'style indirect libre' beschrieben. Meister dieser Technik sind Flaubert (Madame Bovary), Theodor Fontane (Effi Briest) und Thomas Mann."
    ),

    // Question 8 — Erzähltechnik: Stream of Consciousness
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was unterscheidet den 'Bewusstseinsstrom' (Stream of Consciousness) vom inneren Monolog?",
        answerA = "Beide Begriffe sind synonym und bezeichnen dasselbe Verfahren",
        answerB = "Der Bewusstseinsstrom verwendet immer Gedankenstriche, der innere Monolog nicht",
        answerC = "Der innere Monolog ist der Oberbegriff; der Bewusstseinsstrom ist nur in der englischen Literatur gebräuchlich",
        answerD = "Der Bewusstseinsstrom ist radikaler: Er verzichtet auf jede Grammatik-Ordnung und bildet den ungefilterten, assoziativen Gedankenfluss ab; der innere Monolog ist strukturierter",
        correctAnswer = 3,
        explanation = "Der innere Monolog ist eine strukturierte Form der Gedankenwiedergabe in erster Person. Der Bewusstseinsstrom (geprägt von William James) geht weiter: Er imitiert die sprunghaften, ungeordneten, assoziativen Gedankenmuster. James Joyce' 'Ulysses' (Molly Blooms Schlussmonolog ohne Satzzeichen) ist das Paradebeispiel.",
        difficulty = 3,
        funFact = "William James, Bruder des Schriftstellers Henry James, prägte den Begriff 'stream of consciousness' 1890 in der Psychologie. Virginia Woolf und James Joyce übertrugen ihn in die Literatur. Woolfs 'Mrs Dalloway' gilt als das eleganteste Beispiel."
    ),

    // Question 9 — Erzähltechnik: Unzuverlässiger Erzähler
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was kennzeichnet einen 'unzuverlässigen Erzähler' (unreliable narrator)?",
        answerA = "Er macht häufig Rechtschreibfehler und lässt Details aus",
        answerB = "Seine Darstellung der Ereignisse weicht erkennbar von dem ab, was tatsächlich geschehen ist — durch Selbstbetrug, begrenzte Wahrnehmung oder bewusste Lüge",
        answerC = "Er berichtet aus zu vielen Perspektiven gleichzeitig",
        answerD = "Er bricht die vierte Wand und spricht den Leser direkt an",
        correctAnswer = 1,
        explanation = "Der unzuverlässige Erzähler (Begriff geprägt von Wayne C. Booth in 'The Rhetoric of Fiction', 1961) gibt eine verzerrte Version der Realität wieder. Der Leser muss aktiv zwischen den Zeilen lesen. Klassische Beispiele: Stevens in Kazuo Ishiguros 'Remains of the Day', Humbert Humbert in Nabokovs 'Lolita'.",
        difficulty = 3,
        funFact = "Auch in der deutschen Literatur gibt es unzuverlässige Erzähler: Günter Grasss Oskar Matzerath in 'Die Blechtrommel' ist ein Meisterbeispiel — er lügt, verdrängt und rationalisiert seine Vergangenheit ständig."
    ),

    // Question 10 — Stilmittel: Litotes
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welche Funktion erfüllt die Litotes als Stilmittel?",
        answerA = "Sie übertreibt eine Aussage ins Extreme, um sie zu verstärken",
        answerB = "Sie stellt zwei gegensätzliche Begriffe direkt nebeneinander",
        answerC = "Sie bejaht etwas durch doppelte Verneinung oder bewusstes Untertreiben, um die eigentliche Aussage zu verstärken",
        answerD = "Sie ersetzt einen direkten Begriff durch einen umschreibenden Ausdruck",
        correctAnswer = 2,
        explanation = "Die Litotes funktioniert durch doppelte Verneinung ('nicht unklug' = sehr klug) oder durch Understatement ('eine kleine Summe' für eine riesige Menge). Sie wirkt oft ironisch oder höflich. Beispiel: 'Das ist kein schlechter Plan' bedeutet eigentlich 'Das ist ein sehr guter Plan'.",
        difficulty = 3,
        funFact = "Die Litotes ist besonders typisch für die englische Umgangssprache ('not bad' für 'excellent'), aber auch im Deutschen verbreitet. In der Alltagssprache sagen wir oft 'nicht gerade dumm', wenn wir 'äußerst klug' meinen."
    ),

    // Question 11 — Stilmittel: Allegorie
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Worin unterscheidet sich die Allegorie vom Symbol?",
        answerA = "Die Allegorie ist eine ausgedehnte, durchgehende Metaphernkette, die eine abstrakte Idee systematisch verbildlicht; das Symbol ist ein einzelnes Bild mit vielschichtiger Bedeutung",
        answerB = "Das Symbol ist immer religiösen Ursprungs, die Allegorie stammt aus der Antike",
        answerC = "Die Allegorie steht nur in der Lyrik, das Symbol in der Prosa",
        answerD = "Beide Begriffe sind weitgehend synonym; die Allegorie ist nur umfangreicher",
        correctAnswer = 0,
        explanation = "Die Allegorie ist eine systematische, fortlaufende Umsetzung einer abstrakten Idee in ein konkretes Bild (z.B. Justitia als Allegorie der Gerechtigkeit). Ein Symbol (z.B. die Rose als Liebe) trägt Bedeutung durch kulturelle Konvention oder Kontext, ohne ein vollständiges Bildsystem aufzubauen.",
        difficulty = 3,
        funFact = "Dantes 'Divina Commedia' ist eine der komplexesten Allegorien der Weltliteratur: Die Reise durch Hölle, Fegefeuer und Paradies steht für den spirituellen Weg der Menschenseele zur Vollkommenheit."
    ),

    // Question 12 — Stilmittel: Anapher vs. Epipher
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was ist eine Symploke?",
        answerA = "Ein Stilmittel, bei dem alle Sätze mit demselben Wort enden",
        answerB = "Ein Stilmittel, bei dem nur der Anfang wiederholt wird",
        answerC = "Ein griechischer Begriff für den Parallelismus",
        answerD = "Die Kombination von Anapher (Wiederholung am Satzanfang) und Epipher (Wiederholung am Satzende) in denselben Sätzen",
        correctAnswer = 3,
        explanation = "Die Symploke (griech. 'Verflechtung') vereint Anapher und Epipher: 'Wer hätte das gedacht? Wer hätte das gewollt?' — Wenn dasselbe Wort auch am Ende stünde, wäre es eine Symploke. Ein reines Beispiel: 'Alles ist gut, alles ist wahr, alles ist schön.'",
        difficulty = 3,
        funFact = "Die Anapher ist eines der ältesten und wirkungsvollsten Stilmittel der Rhetorik. Martin Luther Kings 'I have a dream' ist die bekannteste Anapher der Neuzeit. In der deutschen Literatur nutzte sie Schiller exzessiv."
    ),

    // Question 13 — Stilmittel: Klimax und Antiklimax
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Caesars 'Veni, vidi, vici' ist ein Beispiel für welches Stilmittel?",
        answerA = "Antiklimax, da die Verben an Bedeutung abnehmen",
        answerB = "Klimax — die Handlungen steigern sich von Ankunft über Erkundung zur vollständigen Unterwerfung",
        answerC = "Asyndeton — drei Verben ohne Konjunktion, aber keine Klimax",
        answerD = "Parallelismus — drei gleichwertige Verben in gleicher Form",
        correctAnswer = 1,
        explanation = "Hier liegen sowohl ein Asyndeton (Verbindung ohne 'und') als auch eine Klimax vor. Die Klimax ist das dominierende Strukturmerkmal: kommen < sehen < siegen steigert sich von bloßer Anwesenheit über Wahrnehmung zur vollständigen Überwindung des Gegners. Die Kürze und Steigerung verleihen dem Satz seine rhetorische Kraft.",
        difficulty = 3,
        funFact = "Das Asyndeton (Auslassung von Konjunktionen) verstärkt hier die Klimax durch Tempo. Das Gegenteil, das Polysyndeton (viele 'und'), verlangsamt den Rhythmus und wirkt häufig episch oder feierlich."
    ),

    // Question 14 — Stilmittel: Synästhesie
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was ist eine Synästhesie als literarisches Stilmittel?",
        answerA = "Die gleichzeitige Verwendung mehrerer Metaphern in einem Satz",
        answerB = "Die Beschreibung innerer Zustände durch äußere Naturbilder",
        answerC = "Die Übertragung eines Sinneseindrucks auf einen anderen Sinn, z.B. 'lautloses Grün' oder 'schreiende Farben'",
        answerD = "Die Personifikation von abstrakten Begriffen wie Zeit und Tod",
        correctAnswer = 2,
        explanation = "Synästhesie verbindet zwei verschiedene Sinnesbereiche: Ein Geräusch wird als Farbe beschrieben, eine Farbe als Klang oder Geschmack. 'Das schreiende Rot der Fahne' überträgt den Hörsinn (schreien) auf den Sehsinn (Farbe). Arthur Rimbauds 'Vokale'-Sonett ('A schwarz, E weiß, I rot') ist das berühmteste Beispiel.",
        difficulty = 3,
        funFact = "Synästhesie ist auch ein neurologisches Phänomen: Manche Menschen hören Farben oder sehen Töne. Komponist Nikolai Rimski-Korsakow und Schriftsteller Vladimir Nabokov waren nachweislich Synästhetiker."
    ),

    // Question 15 — Erzähltechnik: Auktorialer Erzähler
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was ist das Besondere am 'auktorialen Erzähler'?",
        answerA = "Er ist allwissend, kommentiert das Geschehen, kennt die Gedanken aller Figuren und kann die Erzählung direkt ansprechen",
        answerB = "Er erzählt strikt aus der Perspektive einer einzigen Figur, ohne deren Bewusstsein zu verlassen",
        answerC = "Er ist eine Figur im Roman, die selbst erlebt, was sie erzählt",
        answerD = "Er berichtet nur äußere Handlungen ohne Zugang zu Innenperspektiven",
        correctAnswer = 0,
        explanation = "Der auktoriale Erzähler ist der 'Gott' des Romans: Er weiß alles, kann vorausgreifen (Prolepse), zurückblicken (Analepse), kommentieren und urteilen. Typisch für den klassischen Bildungsroman. Der personale Erzähler (Option B) haftet an einer Figur; der Ich-Erzähler (C) ist selbst beteiligt; der neutrale Erzähler (D) beobachtet nur äußerlich.",
        difficulty = 3,
        funFact = "Thomas Manns Erzähler im 'Zauberberg' ist ein Paradebeispiel für den auktorialen Erzähler: Er kommentiert ironisch, springt in der Zeit, kennt alle Figuren bis ins Innerste — und macht den Leser zum Komplizen."
    ),

    // Question 16 — Stilmittel: Euphemismus
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welches Beispiel ist ein politischer Euphemismus?",
        answerA = "'Er ist von uns gegangen' (für: gestorben)",
        answerB = "'Hausgemacht' (für: selbst produziert)",
        answerC = "'Frühstücksdirektor' (für: nutzloser Manager)",
        answerD = "'Kollateralschaden' (für: zivile Todesopfer im Krieg)",
        correctAnswer = 3,
        explanation = "'Kollateralschaden' (engl. 'collateral damage') ist ein klassischer militärisch-politischer Euphemismus: Er verschleiert die moralische Schwere von Zivilopfern hinter technischer Sprache. Option A ist ein gesellschaftlicher Euphemismus um den Tod. Option C ist ein Dysphemismus (absichtlich abwertende Benennung).",
        difficulty = 3,
        funFact = "George Orwell analysierte politische Euphemismen in seinem Essay 'Politics and the English Language' (1946): Sprache, die Grausamkeit verschleiert, nennt er 'Newspeak' — ein Konzept, das er in '1984' literarisch ausarbeitete."
    ),

    // Question 17 — Stilmittel: Hyperbel
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was unterscheidet die Hyperbel von der Klimax?",
        answerA = "Die Hyperbel steigert systematisch in mehreren Schritten; die Klimax übertreibt in einem einzigen Ausdruck",
        answerB = "Die Hyperbel übertreibt eine einzelne Aussage ins Extreme; die Klimax ordnet mehrere Elemente in aufsteigender Intensität",
        answerC = "Hyperbel ist nur in der Lyrik möglich; Klimax nur in der Prosa",
        answerD = "Beide Figuren sind identisch — der Unterschied ist nur terminologisch",
        correctAnswer = 1,
        explanation = "Die Hyperbel ist eine punktuelle, extreme Übertreibung: 'Ich habe dir das schon tausendmal erklärt!' Die Klimax hingegen ist eine Reihung mit zunehmender Intensität: 'Er kam, er sah, er siegte.' Eine Klimax kann Hyperbeln enthalten, ist aber selbst ein strukturelles Prinzip.",
        difficulty = 3,
        funFact = "Homer nutzte in der 'Ilias' extreme Hyperbeln: Helden sind 'schnell wie der Wind', ihre Speere 'verdunkeln die Sonne'. Shakespeare übertraf ihn noch: 'I will wear my heart upon my sleeve' — das Herz sichtbar auf dem Ärmel tragen."
    ),

    // Question 18 — Erzähltechnik: Intertextualität
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was bezeichnet der Begriff 'Intertextualität' in der Literaturwissenschaft?",
        answerA = "Die Fähigkeit eines Textes, in verschiedene Sprachen übersetzt zu werden",
        answerB = "Die inhaltliche Ähnlichkeit zwischen zwei Werken desselben Autors",
        answerC = "Das Netz von Bezügen, Verweisen, Zitaten und Anspielungen zwischen verschiedenen Texten",
        answerD = "Die Technik, mehrere Handlungsstränge in einem Roman zu verweben",
        correctAnswer = 2,
        explanation = "Intertextualität (Begriff geprägt von Julia Kristeva, 1967) beschreibt, wie jeder Text ein 'Geflecht' aus anderen Texten ist. Formen: direktes Zitat, Anspielung (Allusion), Parodie, Pastiche, Travestie. T.S. Eliots 'The Waste Land' gilt als Höhepunkt intertextueller Dichtung — er verwebt Dutzende Quellen.",
        difficulty = 3,
        funFact = "Roland Barthes formulierte radikaler: 'Der Tod des Autors' (1967) — ein Text gehört nicht dem Autor, sondern entsteht im Leser durch sein intertextuelles Wissen. Der Autor stirbt, der Text lebt in seinen Beziehungen zu anderen Texten."
    ),

    // Question 19 — Stilmittel: Ellipse
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welche Funktion hat die Ellipse als Stilmittel?",
        answerA = "Sie lässt syntaktisch notwendige Elemente weg, was den Satz verdichtet, beschleunigt oder emotionalisiert",
        answerB = "Sie wiederholt Satzteile aus dem vorherigen Satz an neuer Stelle",
        answerC = "Sie verlängert einen Satz durch eingeschobene Nebensätze ins Extreme",
        answerD = "Sie stellt das Objekt vor das Subjekt, um Betonung zu erzeugen",
        correctAnswer = 0,
        explanation = "Die Ellipse lässt grammatisch notwendige Elemente weg: 'Kommst du morgen?' — 'Wenn möglich.' (statt: 'Wenn es möglich ist, komme ich.') In der Literatur dient sie zur Intensivierung von Dramatik oder Tempo. Telegrammstil und Dialogsprache nutzen sie häufig.",
        difficulty = 3,
        funFact = "In Kafkas Prosa finden sich prägnante Ellipsen, die das Unvollendete, Bedrohliche verstärken. Auch Brechts episches Theater nutzt elliptische Dialoge bewusst: Lücken sollen den Zuschauer zum Nachdenken zwingen."
    ),

    // Question 20 — Stilmittel: Periphrase
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was ist eine Periphrase?",
        answerA = "Ein Reim am Ende zweier aufeinanderfolgender Zeilen",
        answerB = "Die Wiederholung desselben Gedankens mit anderen Worten (Pleonasmus)",
        answerC = "Ein Stilmittel, das nur in der Barockdichtung vorkommt",
        answerD = "Die Umschreibung eines Begriffs durch mehrere Wörter, oft um ihn zu verschleiern, zu verschönern oder zu präzisieren",
        correctAnswer = 3,
        explanation = "Die Periphrase (Umschreibung) ersetzt ein einfaches Wort durch einen erklärenden Ausdruck: 'das schwarze Gold' (Erdöl), 'der Sensenmann' (Tod), 'das Haus Gottes' (Kirche). Sie kann euphemistisch sein (Tod umschreiben) oder poetisch (in der Lyrik). In der Kenning der altnordischen Dichtung war sie besonders ausgeprägt.",
        difficulty = 3,
        funFact = "Die altnordischen Skalden liebten extravagante Periphrasen: Das Meer hieß 'Straße der Wale', ein König war 'Verteiler von Gold', ein Schwert war 'Wunde-Schlange'. Diese poetischen Doppelausdrücke heißen 'Kenningar'."
    ),

    // Question 21 — Stilmittel: Ironie vs. Sarkasmus
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Worin liegt der entscheidende Unterschied zwischen Ironie und Sarkasmus?",
        answerA = "Ironie ist nur schriftlich möglich; Sarkasmus nur mündlich",
        answerB = "Ironie sagt das Gegenteil des Gemeinten ohne Verletzungsabsicht; Sarkasmus ist eine verletzende, beißende Form der Ironie, oft mit aggressiver Absicht",
        answerC = "Beide Begriffe sind identisch; Sarkasmus ist nur der griechische Fachbegriff für Ironie",
        answerD = "Sarkasmus richtet sich gegen Institutionen; Ironie gegen einzelne Personen",
        correctAnswer = 1,
        explanation = "Ironie und Sarkasmus sagen beide das Gegenteil des Gemeinten. Der Unterschied liegt im Ton und in der Absicht: Ironie kann spielerisch, humorvoll oder milde sein. Sarkasmus (griech. 'sarkazein' = Fleisch zerfetzen) verletzt absichtlich. 'Wie praktisch!' ironisch gesagt kann witzig sein; als Sarkasmus ist es eine Attacke.",
        difficulty = 3,
        funFact = "Heinrich Heine war Meister der feinen Ironie. In 'Deutschland. Ein Wintermärchen' schreibt er: 'Franzosen und Russen gehört das Land / Das Meer gehört den Briten / Wir aber besitzen im Luftreich des Traums / Die Herrschaft unbestritten.' — Ironie über deutsche Innerlichkeit statt politischer Macht."
    ),

    // Question 22 — Erzähltechnik: Analepse und Prolepse
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was ist der Unterschied zwischen Analepse und Prolepse?",
        answerA = "Die Analepse ist eine Vorausschau auf künftige Ereignisse; die Prolepse ein Rückblick",
        answerB = "Beide Begriffe bezeichnen Zeitsprünge, aber Analepse ist in der Lyrik, Prolepse in der Prosa üblich",
        answerC = "Die Analepse (Rückblende/Flashback) springt in die Vergangenheit; die Prolepse (Vorausdeutung/Foreshadowing) kündigt künftige Ereignisse an",
        answerD = "Analepse ist ein Erzählerwechsel; Prolepse ist ein Perspektivwechsel",
        correctAnswer = 2,
        explanation = "Analepse (griech. 'Rücknahme') = Rückblende, Flashback, Rückblick auf vergangene Ereignisse. Prolepse (griech. 'Vorwegnahme') = Vorausdeutung, Foreshadowing, Andeutung künftiger Ereignisse. Beide sind Formen der zeitlichen Diskordanz (Gerard Genette: Erzählzeit ≠ erzählte Zeit).",
        difficulty = 3,
        funFact = "Homer beginnt die 'Ilias' 'in medias res' (in der Mitte der Handlung) und nutzt dann ausgedehnte Analepsen, um Vorgeschichte nachzuliefern. Dieses Muster prägt Thriller und Kriminalromane bis heute."
    ),

    // Question 23 — Stilmittel: Antithese
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "In welchem Beispiel liegt eine reine Antithese vor, die KEIN Chiasmus ist?",
        answerA = "'Der Geist ist willig, aber das Fleisch ist schwach'",
        answerB = "'Die Kunst ist lang, und kurz ist unser Leben' (Goethe/Faust)",
        answerC = "'Ihr Leben ist dein Tod! Ihr Tod dein Leben!' (Schiller)",
        answerD = "'Veni, vidi, vici' (Caesar)",
        correctAnswer = 0,
        explanation = "Option A ist eine reine Antithese (zwei entgegengesetzte Gedanken nebeneinandergestellt), ohne dass die Satzglieder gespiegelt werden. Option B ist ein Chiasmus (Kunst=lang vs. kurz=Leben — gespiegelte Struktur). Option C ist ebenfalls ein Chiasmus mit antithetischem Inhalt. Option D ist Klimax und Asyndeton.",
        difficulty = 3,
        funFact = "Das Bibelzitat (Matthäus 26:41) 'Der Geist ist willig, aber das Fleisch ist schwach' ist eine der bekanntesten Antithesen des Abendlandes. Sie beeinflusste Jahrhunderte theologischer und literarischer Debatten über Sünde und Tugend."
    ),

    // Question 24 — Erzähltechnik: Personaler Erzähler
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was kennzeichnet den 'personalen Erzähler' im Unterschied zum auktorialen?",
        answerA = "Er ist selbst eine Figur im Roman und erzählt in der ersten Person",
        answerB = "Er kennt die Gedanken aller Figuren, urteilt aber nicht über sie",
        answerC = "Er wechselt ständig die Erzählperspektive zwischen verschiedenen Figuren",
        answerD = "Er haftet an der Innenperspektive einer einzigen Figur und weiß nur, was diese weiß und denkt",
        correctAnswer = 3,
        explanation = "Der personale Erzähler (auch: 'figural narrative situation', Franz Karl Stanzel) bleibt strikt an eine Figur gebunden: Er sieht durch deren Augen, denkt deren Gedanken, weiß nicht mehr als diese. Anders als der Ich-Erzähler ist er grammatisch in der dritten Person. Franz Kafkas 'Der Prozess' ist ein Paradebeispiel.",
        difficulty = 3,
        funFact = "Franz Karl Stanzels 'Theorie des Erzählens' (1979) ist das einflussreichste deutsche Werk zur Erzähltechnik. Er unterscheidet drei 'Erzählsituationen': auktorial, personal und Ich-Erzählung — die in der Praxis oft ineinandergleiten."
    ),

    // Question 25 — Stilmittel: Apostrophe
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was ist eine Apostrophe als rhetorisches Stilmittel?",
        answerA = "Ein Satzzeichen, das Auslassungen markiert",
        answerB = "Die direkte Anrede einer abwesenden Person, einer abstrakten Idee oder eines Gegenstandes als wäre sie/er anwesend",
        answerC = "Die plötzliche Unterbrechung eines Satzes für dramatische Wirkung (Aposiopese)",
        answerD = "Die Wiederholung eines Wortes am Ende aufeinanderfolgender Sätze",
        correctAnswer = 1,
        explanation = "Die Apostrophe richtet sich an Abwesende, Tote, abstrakte Begriffe oder Gegenstände: 'O Freiheit, du bist so wertvoll!' oder Schillers 'An die Freude'. Sie verleiht der Aussage feierliche, leidenschaftliche oder ironische Qualität und ist ein Kernmittel der Ode und des Hymnus.",
        difficulty = 3,
        funFact = "Goethes 'Faust' beginnt mit einer ausgedehnten Apostrophe: 'Ihr naht euch wieder, schwankende Gestalten...' Faust spricht die Geister seiner Vergangenheit an — eine der eindringlichsten Eröffnungen der Weltliteratur."
    ),

    // Question 26 — Stilmittel: Aposiopese
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was ist eine Aposiopese?",
        answerA = "Eine besonders lange, verschachtelte Satzkonstruktion",
        answerB = "Die Wiederholung desselben Wortes am Anfang und Ende eines Satzes",
        answerC = "Ein absichtlicher, dramatisch wirkender Abbruch eines Satzes, der unvollendet bleibt — oft durch Gedankenstriche oder drei Punkte markiert",
        answerD = "Ein Stilmittel, bei dem ein negativer Aspekt betont wird, um Mitleid zu erzeugen",
        correctAnswer = 2,
        explanation = "Die Aposiopese (griech. 'Verstummen') bricht einen Satz bewusst ab: 'Wenn du das noch einmal tust, dann...!' Der unausgesprochene Rest wirkt oft bedrohlicher als das Ausgesprochene. In der Lyrik signalisiert sie Überwältigung durch Emotion.",
        difficulty = 3,
        funFact = "In Shakespeares 'King Lear' nutzt der wahnsinnige König Aposiopesen, um seine zerbrochene Psyche darzustellen. Das Stilmittel ist ein Zeichen für emotionale Überwältigung oder bedrohliches Schweigen."
    ),

    // Question 27 — Erzähltechnik: Erzählzeit vs. erzählte Zeit
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was bedeutet 'Zeitraffung' in der Narratologie?",
        answerA = "Die erzählte Zeit ist länger als die Erzählzeit — Jahre werden in wenigen Sätzen zusammengefasst",
        answerB = "Die Erzählzeit ist länger als die erzählte Zeit — ein kurzer Moment wird sehr ausführlich beschrieben",
        answerC = "Erzählzeit und erzählte Zeit sind identisch — Echtzeit-Erzählung",
        answerD = "Der Erzähler springt ohne Ankündigung zwischen verschiedenen Zeiten",
        correctAnswer = 0,
        explanation = "Zeitraffung (Zusammenfassung): Viele Jahre werden in kurzen Sätzen erzählt ('Er lebte noch dreißig Jahre und starb im Frieden'). Das Gegenteil ist die Zeitdehnung (Dehnung/Szene): Ein kurzer Moment wird extrem ausgedehnt. Die Zeitdeckung (isochrony) bedeutet Gleichheit beider Zeiten (wie im Drama).",
        difficulty = 3,
        funFact = "Gerard Genettes 'Discours du récit' (1972) ist das Standardwerk zu narrativer Zeit. Er unterscheidet Ordnung (Chronologie vs. Anachronien), Dauer (Tempo) und Frequenz (wie oft ein Ereignis erzählt wird) als drei Dimensionen narrativer Zeit."
    ),

    // Question 28 — Stilmittel: Parallelismus
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was unterscheidet den Parallelismus vom Chiasmus?",
        answerA = "Der Parallelismus ist eine Steigerungsform des Chiasmus",
        answerB = "Beide sind identisch — der Chiasmus ist nur der griechische Fachbegriff",
        answerC = "Der Chiasmus enthält immer eine Antithese; der Parallelismus nicht",
        answerD = "Der Parallelismus wiederholt syntaktische Strukturen in gleicher Reihenfolge (ABAB); der Chiasmus spiegelt sie (ABBA)",
        correctAnswer = 3,
        explanation = "Parallelismus: 'Ich kam, ich sah, ich siegte' (ABC — gleiche Struktur dreimal). Chiasmus: 'Die Kunst ist lang, und kurz ist unser Leben' (ABC-CBA — gespiegelt). Der Parallelismus kann inhaltlich eine Steigerung (Klimax) enthalten; der Chiasmus erzeugt durch die Spiegelung Spannung oder Kontrast.",
        difficulty = 3,
        funFact = "Der Parallelismus ist das Grundprinzip der hebräischen Bibeldichtung (Psalmen): 'Der Herr ist mein Hirte, mir wird nichts mangeln' folgt dem synonymen Parallelismus — beide Hälften sagen dasselbe auf verschiedene Weise."
    ),

    // Question 29 — Erzähltechnik: Fokalisierung
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was versteht die Narratologie unter 'Nullfokalisierung'?",
        answerA = "Die Erzählung hat keinen klaren Fokus und springt chaotisch zwischen Perspektiven",
        answerB = "Der Erzähler weiß mehr als alle Figuren zusammen — er ist allwissend; es gibt keine Einschränkung der Wissensposition",
        answerC = "Die Erzählung konzentriert sich ausschließlich auf äußere Handlungen ohne Innenperspektive",
        answerD = "Der Erzähler weiß weniger als die Figuren — er ist ahnungsloser Beobachter",
        correctAnswer = 1,
        explanation = "Gerard Genettes Fokaliserungsmodell unterscheidet: Nullfokalisierung (Erzähler > Figur: allwissend), interne Fokalisierung (Erzähler = Figur: gebunden an eine Perspektive), externe Fokalisierung (Erzähler < Figur: nur äußeres Verhalten sichtbar). Der klassische auktoriale Erzähler operiert mit Nullfokalisierung.",
        difficulty = 3,
        funFact = "Ernest Hemingways 'iceberg theory' fordert externe Fokalisierung: Nur Oberfläche zeigen, den Rest weglassen. Der Leser soll das Verborgene erspüren. In 'Hills Like White Elephants' wird nie direkt gesagt, worum es geht — nur der Dialog zeigt es."
    ),

    // Question 30 — Stilmittel: Anastrophe
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was ist eine Anastrophe?",
        answerA = "Die Steigerung einer Aussage durch drei identische Wiederholungen",
        answerB = "Ein Stilmittel, das nur in Gedichten verwendet wird, um Reime zu erzeugen",
        answerC = "Die Umkehrung der normalen Wortstellung im Satz zur Betonung oder aus poetischen Gründen",
        answerD = "Die Übertragung eines Adjektivs auf das falsche Nomen im Satz",
        correctAnswer = 2,
        explanation = "Anastrophe (griech. 'Umkehrung') ist eine Inversion der üblichen Wortfolge: 'Dunkel war die Nacht' statt 'Die Nacht war dunkel'. Damit wird 'dunkel' betont. In der Lyrik dient sie oft dem Rhythmus und Reim. Häufig in biblischer Sprache: 'Dein ist das Reich und die Kraft und die Herrlichkeit.'",
        difficulty = 3,
        funFact = "Yodas Sprache in Star Wars ist eine systematische Anastrophe: 'Viel zu lernen du noch hast.' Das klingt fremd und weise zugleich — die ungewohnte Wortstellung signalisiert eine andere Art zu denken."
    ),

    // Question 31 — Stilmittel: Alliteration vs. Assonanz
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was ist der Unterschied zwischen Alliteration und Assonanz?",
        answerA = "Alliteration ist die Wiederholung des gleichen Anlautkonsononanten; Assonanz ist die Wiederholung des gleichen Vokalklangs im Innern der Wörter",
        answerB = "Alliteration bezieht sich auf Vokale; Assonanz auf Konsonanten",
        answerC = "Alliteration tritt nur in der Lyrik auf; Assonanz nur in der Prosa",
        answerD = "Beide Begriffe bezeichnen dasselbe Phänomen mit unterschiedlicher Herkunft",
        correctAnswer = 0,
        explanation = "Alliteration: 'Milch macht müde Männer munter' — gleicher Anlautkonsonant. Assonanz: 'Wein, Weib und Gesang' — gleicher Vokalklang (ei/ei/e). Assonanz ist ein 'unreiner Reim': Die Vokale stimmen überein, aber nicht die Konsonanten. Beide Figuren erzeugen Klangwirkung und Rhythmus.",
        difficulty = 3,
        funFact = "Das althochdeutsche Hildebrandslied (ca. 820 n. Chr.) nutzt Alliterationen als strukturierendes Prinzip — lange vor dem Endreim. Dieser Stabreim war das poetische Grundprinzip der germanischen Dichtung."
    ),

    // Question 32 — Stilmittel: Ironie vs. Parodie
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was ist der Unterschied zwischen Parodie und Travestie als literarische Formen?",
        answerA = "Parodie ist eine ernste Nachahmung; Travestie ist eine komische",
        answerB = "Beide Formen sind identisch — Travestie ist nur der veraltete Begriff",
        answerC = "Parodie bezieht sich auf Musik; Travestie auf Literatur",
        answerD = "Die Parodie übernimmt den Inhalt eines Werkes, verändert aber Form und Stil komisch; die Travestie behält die Form, behandelt aber einen unpassend trivialen Inhalt darin",
        correctAnswer = 3,
        explanation = "Parodie: Schillers 'Glocke' wird mit Bier-Thematik parodiert — gleicher Rhythmus, neuer (banaler) Inhalt. Travestie: Die 'Odyssee' wird in der Trivialsprache eines Touristen erzählt — gleicher Inhalt, andere (erniedrigte) Form. Das Pastiche ahmt einen Stil liebevoll nach, ohne zu verzerren.",
        difficulty = 3,
        funFact = "Friedrich Schillers 'Lied von der Glocke' wurde so oft parodiert, dass Schiller selbst witzelte, das Gedicht sei unsterblich — zumindest durch seine Parodien. Heinrich Heine schrieb eine berühmte Parodie darauf."
    ),

    // Question 33 — Epochen: Sturm und Drang
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welches Stilmerkmal ist typisch für die Epoche 'Sturm und Drang' (1767–1785)?",
        answerA = "Strenge Einhaltung der aristotelischen Drei Einheiten (Ort, Zeit, Handlung)",
        answerB = "Emphase auf Gefühl, Natur, Leidenschaft und Rebellion gegen Standesschranken; Abkehr von rationalen Regelpoetiken",
        answerC = "Bevorzugung von Sonett und Ode als Hauptgattungen",
        answerD = "Didaktische Absicht und moralische Belehrung des Lesers",
        correctAnswer = 1,
        explanation = "Der Sturm und Drang reagierte gegen die Aufklärung und ihre Vernunftdominanz. Leitbegriff: 'Genie' — der schöpferische Einzelne, der sich über Regeln erhebt. Goethes 'Götz von Berlichingen', Schillers 'Die Räuber', Klingers Drama (das der Epoche den Namen gab) sprengen Struktur und Schicklichkeit zugunsten von Leidenschaft.",
        difficulty = 3,
        funFact = "Goethe schrieb seinen Briefroman 'Die Leiden des jungen Werthers' (1774) in nur vier Wochen — die prototypische Sturm-und-Drang-Figur: sensibel, unglücklich verliebt, im Konflikt mit der Gesellschaft. Das Buch löste in Europa angeblich eine Selbstmordepidemie aus ('Werther-Effekt')."
    ),

    // Question 34 — Stilmittel: Euphonie vs. Kakophonie
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welchen Effekt erzeugt Kakophonie im literarischen Text bewusst eingesetzt?",
        answerA = "Sie erzeugt Wohlklang und harmonische Wirkung durch weiche Vokale",
        answerB = "Sie ist stets ein unbeabsichtigter Stilfehler",
        answerC = "Sie erzeugt Missklang, Härte oder Bedrohlichkeit durch harte Konsonanten und unharmonische Lautfolgen — um inhaltliche Dissonanz klanglich zu spiegeln",
        answerD = "Sie ist ein Merkmal mittelhochdeutscher Dichtung",
        correctAnswer = 2,
        explanation = "Kakophonie ('schlechter Klang') ist das Gegenteil der Euphonie. Bewusst eingesetzt, spiegelt sie inhaltliche Gewalt, Chaos oder Hässlichkeit: 'Das Kratzen, Kreischen, Knirschen der Räder' — die Konsonantenhäufung imitiert den Lärm. In Rilkes 'Archaischem Torso Apollos' wechseln Euphonie und Kakophonie, um Schönheit und Schock zu kontrastieren.",
        difficulty = 3,
        funFact = "Das Prinzip heißt 'Lautmalerei' oder 'Onomatopoetik': Klang und Bedeutung werden bewusst synchronisiert. 'Miauen', 'Zischen', 'Donner' sind onomatopoetische Wörter, die klingen wie das, was sie bezeichnen."
    ),

    // Question 35 — Erzähltechnik: Multiperspektivität
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was ist Multiperspektivität als erzähltechnisches Verfahren?",
        answerA = "Dasselbe Ereignis wird aus verschiedenen Erzählerperspektiven dargestellt, sodass eine eindeutige 'Wahrheit' fehlt und der Leser selbst urteilen muss",
        answerB = "Ein Erzähler schildert simultane Ereignisse an verschiedenen Orten",
        answerC = "Der Leser übernimmt selbst die Erzählerperspektive (interaktives Erzählen)",
        answerD = "Ein Text enthält mehrere Kapitel, die je einer anderen Figur gewidmet sind",
        correctAnswer = 0,
        explanation = "Multiperspektivität (auch: Polyperspektive) zeigt dasselbe Ereignis aus mehreren, oft widersprüchlichen Blickwinkeln. Klassisches Beispiel: Rashomon-Effekt (nach Kurosawas Film 1950, basierend auf Akutagawa). In der deutschen Literatur: Döblins 'Berlin Alexanderplatz', Ransmayr, Kehlmann.",
        difficulty = 3,
        funFact = "Daniel Kehlmanns 'Die Vermessung der Welt' (2005) wechselt zwischen den Perspektiven von Gauß und Humboldt — und macht durch die Differenz deutlich, wie unterschiedlich dieselbe Epoche erlebt werden kann."
    ),

    // Question 36 — Stilmittel: Ironie in der Romantik
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was versteht Friedrich Schlegel unter 'romantischer Ironie'?",
        answerA = "Die ironische Darstellung von Liebesbeziehungen in romantischen Gedichten",
        answerB = "Die Verwendung von Ironie als Mittel der Gesellschaftskritik im Roman",
        answerC = "Das ironische Brechen mit dem Zeitgeist der Romantik durch realistische Elemente",
        answerD = "Die selbstreflexive Haltung des Künstlers, der sein eigenes Werk gleichzeitig erschafft und distanziert betrachtet — die Freiheit, es zu schaffen und sofort zu zerstören",
        correctAnswer = 3,
        explanation = "Friedrich Schlegels romantische Ironie (Fragment 108 des Athenäums, 1798) ist eine philosophische Haltung: Der Künstler hält sich selbst auf Abstand von seinem Werk. Er erschafft es leidenschaftlich und demontiert es gleichzeitig durch Einschübe, Selbstkommentare oder Brüche. Ludwig Tiecks 'Der gestiefelte Kater' ist das Paradebeispiel: Figuren kommentieren das Stück, in dem sie spielen.",
        difficulty = 3,
        funFact = "Schlegels Konzept beeinflusst noch heute postmoderne Literatur: Metafiktion, self-aware narratives, breaking the fourth wall — all das hat seine Wurzeln in der romantischen Ironie von 1798."
    ),

    // Question 37 — Stilmittel: Personifikation vs. Anthropomorphismus
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was ist der Unterschied zwischen Personifikation und Prosopopöie?",
        answerA = "Personifikation betrifft nur Tiere; Prosopopöie nur Gegenstände",
        answerB = "Personifikation verleiht Dingen oder Abstrakta menschliche Eigenschaften; Prosopopöie lässt Abwesende oder Tote sprechen — beide sind verwandt, die Prosopopöie ist die sprechende Variante",
        answerC = "Prosopopöie ist ein veralteter Begriff; heute spricht man nur noch von Personifikation",
        answerD = "Die Personifikation ist ein visuelles Stilmittel; die Prosopopöie ein akustisches",
        correctAnswer = 1,
        explanation = "Personifikation: 'Der Wind klagt durch die Bäume' (Wind handelt wie ein Mensch). Prosopopöie (griech. 'Person erschaffen'): Tote oder Abwesende sprechen direkt: 'Wäre mein Vater noch da, er würde sagen...' oder ein Brief, der 'seine Stimme erhebt'. Apostrophe (Anrede) und Prosopopöie überschneiden sich.",
        difficulty = 3,
        funFact = "In Ciceros Reden lässt er oft die Gesetze, den Staat oder die Götter direkt sprechen — das ist klassische Prosopopöie. In der mittelhochdeutschen Dichtung spricht oft der personifizierte Tod oder die Minne (Liebe) als Figur."
    ),

    // Question 38 — Erzähltechnik: Episches Theater
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was versteht Bertolt Brecht unter dem 'Verfremdungseffekt' (V-Effekt)?",
        answerA = "Die Verwendung von Fremdwörtern und unbekannten Begriffen, um den Zuschauer zu irritieren",
        answerB = "Das Einsetzen von Musik und Tanz, um die Handlung zu unterbrechen",
        answerC = "Techniken, die verhindern, dass sich der Zuschauer mit den Figuren identifiziert — er soll kritisch denken statt mitfühlen, das Dargestellte als veränderbar erkennen",
        answerD = "Der Einsatz von Masken und Kostümen, die die Figuren unkenntlich machen",
        correctAnswer = 2,
        explanation = "Brechts V-Effekt (Entfremdungseffekt) ist das Kernprinzip des Epischen Theaters: Schauspieler zeigen Figuren, ohne sich mit ihnen zu identifizieren; Schilder erklären was als nächstes passiert; Songs unterbrechen die Handlung; Figuren sprechen das Publikum an. Ziel: der Zuschauer soll denken statt fühlen und gesellschaftliche Verhältnisse als machbar, also veränderbar, erkennen.",
        difficulty = 3,
        funFact = "Brechts Gegenbegriff zum V-Effekt ist das aristotelische 'einfühlsame Theater': Der Zuschauer identifiziert sich mit der Figur, durchlebt Katharsis (Reinigung) durch Mitleid und Schrecken. Brecht lehnte dieses Konzept als politisch konservativ ab."
    ),

    // Question 39 — Stilmittel: Enumeratio
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was ist eine Enumeratio?",
        answerA = "Eine Aufzählung von Einzeldingen, Eigenschaften oder Handlungen zur Verdeutlichung oder Intensivierung einer übergeordneten Aussage",
        answerB = "Die Zusammenfassung eines langen Textes in einem einzigen Satz",
        answerC = "Eine numerische Auflistung von Fakten in wissenschaftlichen Texten",
        answerD = "Die stufenweise Steigerung einer Aussage (synonym zu Klimax)",
        correctAnswer = 0,
        explanation = "Die Enumeratio (lat. 'Aufzählung') entfaltet eine Idee durch Nennung ihrer Bestandteile: 'Chaos, Krieg, Hunger, Elend — das war das Erbe dieser Politik.' Die Wirkung entsteht durch Häufung: Je mehr Elemente, desto überwältigender der Eindruck. Sie kann sowohl sachlich als auch rhetorisch-emotional eingesetzt werden.",
        difficulty = 3,
        funFact = "Walt Whitmans 'Leaves of Grass' (1855) nutzt die Enumeratio exzessiv als poetisches Grundprinzip — endlose Aufzählungen demokratisieren die Welt, indem jedes Element gleichberechtigt erscheint. Dieses Verfahren nennt man auch 'Whitmanscher Katalog'."
    ),

    // Question 40 — Erzähltechnik: Rahmenerzählung
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welchen literarischen Effekt erzeugt die Rahmenerzählung?",
        answerA = "Sie ermöglicht dem Autor, anonym zu bleiben und keine eigene Perspektive einzunehmen",
        answerB = "Sie beschleunigt das Erzähltempo, da Vorgeschichten weggelassen werden",
        answerC = "Sie ist ein Merkmal ausschließlich mittelalterlicher Literatur (z.B. Boccaccio)",
        answerD = "Sie schafft eine Erzählebene, die die Binnenerzählung distanziert, beglaubigt oder kommentiert — und kann die Zuverlässigkeit des Erzählten in Frage stellen",
        correctAnswer = 3,
        explanation = "Die Rahmenerzählung (z.B. Boccaccios 'Decamerone', Tausendundeine Nacht, Storms 'Der Schimmelreiter') umgibt eine Binnenhandlung mit einer äußeren Erzählsituation. Das erzeugt mehrfache Distanz: Wir hören eine Geschichte, die jemand erzählt. Das kann Authentizität suggerieren oder — bei unzuverlässigen Rahmenerzählern — Zweifel säen.",
        difficulty = 3,
        funFact = "Theodor Storms 'Der Schimmelreiter' (1888) hat eine doppelte Rahmenerzählung: Ein Ich-Erzähler liest in einer alten Zeitschrift, dass dort ein alter Mann erzählt, was er selbst erlebt hat. Drei Erzählebenen — jede distanziert die Binnenhandlung um Hauke Haien weiter."
    ),

    // Question 41 — Stilmittel: Correctio
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was ist eine Correctio (Epanorthose)?",
        answerA = "Die grammatikalische Verbesserung eines Fehlers im Nachhinein",
        answerB = "Die rhetorisch wirkungsvolle Selbstkorrektur: Ein Begriff wird sofort durch einen treffenderen, schärferen oder extremeren ersetzt",
        answerC = "Die Verwendung eines Synonyms, um Wiederholungen zu vermeiden",
        answerD = "Die Ergänzung eines unvollständigen Gedankens durch einen Nachsatz",
        correctAnswer = 1,
        explanation = "Die Correctio korrigiert sich scheinbar selbst, verstärkt dadurch aber die Aussage: 'Er war gut — nein, er war brillant!' oder 'Er hat gelogen — nein, er hat die Wahrheit verschleiert!' Die scheinbare Verbesserung steigert die emotionale Intensität. Sie ist ein beliebtes Mittel in politischen Reden.",
        difficulty = 3,
        funFact = "In der klassischen Rhetorik war die Epanorthose eine Kunstfigur, die Bescheidenheit vortäuscht, um eine noch stärkere Aussage zu rechtfertigen. Cicero nutzte sie systematisch: 'Ich sage nicht, er ist ein Verräter — er ist schlimmer als das.'"
    ),

    // Question 42 — Erzähltechnik: Diegesis vs. Mimesis
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was beschreibt Platon mit dem Gegensatz zwischen Diegesis und Mimesis in der Dichtung?",
        answerA = "Diegesis ist der handlungsreiche Teil eines Textes; Mimesis ist der beschreibende Teil",
        answerB = "Diegesis bezeichnet Lyrik; Mimesis bezeichnet Drama und Epik",
        answerC = "Diegesis ist das Erzählen (der Autor berichtet in eigener Person); Mimesis ist das Nachahmen (der Autor lässt Figuren direkt sprechen, verschwindet hinter ihnen)",
        answerD = "Beide Begriffe sind gleichwertig und werden synonym verwendet",
        correctAnswer = 2,
        explanation = "Platon unterscheidet in der 'Politeia': Diegesis = der Dichter erzählt in eigener Person. Mimesis = der Dichter schlüpft in Figuren und lässt sie direkt sprechen. Das Drama ist reine Mimesis. Epik mischt beides. Aristoteles wertete die Mimesis positiv; Platon sah sie kritisch: Der Dichter imitiert nur Erscheinungen, nicht die Wahrheit.",
        difficulty = 3,
        funFact = "Aristoteles' 'Poetik' (ca. 335 v. Chr.) rettet die Mimesis: Dichtung imitiert nicht nur die Wirklichkeit, sondern zeigt, was sein könnte — sie ist damit philosophischer (wahrheitsähnlicher) als die Geschichtsschreibung, die nur zeigt, was war."
    ),

    // Question 43 — Stilmittel: Hendiadyoin
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was ist ein Hendiadyoin?",
        answerA = "Die Ausdrucksweise, bei der ein einziger Begriff durch zwei gleichwertig koordinierte Wörter ausgedrückt wird: 'mit Rat und Tat' statt 'mit tatkräftigem Rat'",
        answerB = "Die Verdopplung eines Wortes zur Intensivierung: 'sehr sehr wichtig'",
        answerC = "Ein Reim, bei dem zwei Wörter mit gleichem Stamm reimend nebeneinandergestellt werden",
        answerD = "Eine Figur, bei der ein Adjektiv als Nomen verwendet wird",
        correctAnswer = 0,
        explanation = "Hendiadyoin (griech. 'eins durch zwei') drückt einen Gedanken durch zwei koordinierte Ausdrücke aus: 'mit Rat und Tat', 'auf Biegen und Brechen', 'in Saus und Braus'. In der Lyrik dient es der rhythmischen Füllung und emotionalen Verdoppelung. Goethes 'Licht und Liebe' ist ein klassisches Beispiel.",
        difficulty = 3,
        funFact = "Im Lateinischen ist das Hendiadyoin sehr häufig: 'pateris libamus et auro' (Vergil) = 'wir trinken aus Schalen aus Gold' — zwei Nomen stehen für ein Attributivgefüge. Im Deutschen ist 'Hab und Gut' das bekannteste feste Hendiadyoin."
    ),

    // Question 44 — Erzähltechnik: Karneval / Dialogizität
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was versteht Michail Bachtin unter 'Polyphonie' im Roman?",
        answerA = "Die Verwendung verschiedener Erzählstimmen, die abwechselnd erzählen, ohne sich zu widersprechen",
        answerB = "Der Einsatz musikalischer Prinzipien (Kontrapunkt) in der Prosa",
        answerC = "Die gleichzeitige Erzählung mehrerer unabhängiger Handlungsstränge",
        answerD = "Verschiedene Figuren haben gleichwertige, nicht vom Autor dominierte Stimmen und Weltanschauungen, die miteinander in Dialog treten, ohne dass eine Wahrheit siegt",
        correctAnswer = 3,
        explanation = "Bachtin ('Probleme der Poetik Dostojewskis', 1929) nennt Dostojewski den Erfinder des 'polyphonen Romans': Raskolnikow, Sonja, der Untersuchungsrichter — jede Figur hat eine eigene vollwertige Weltanschauung, der Autor stellt sich nicht über sie. Der Gegenbegriff: der 'monologische Roman', in dem alles der Autorenintention untergeordnet ist.",
        difficulty = 3,
        funFact = "Bachtins Konzept der 'Dialogizität' geht noch weiter: Jedes Wort trägt fremde Stimmen in sich — Sprache ist immer schon ein Dialog mit anderen, die dasselbe Wort vor uns benutzt haben. Kein Text existiert ohne den Widerhall anderer Texte."
    ),

    // Question 45 — Stilmittel: Tautologie vs. Pleonasmus
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was ist der Unterschied zwischen Tautologie und Pleonasmus?",
        answerA = "Beide Begriffe sind identisch und bezeichnen dieselbe Bedeutungswiederholung",
        answerB = "Die Tautologie wiederholt denselben Sachverhalt in anderen Worten ('rund um den Kreis'); der Pleonasmus fügt ein überflüssiges Attribut hinzu, das bereits im Hauptwort steckt ('weißer Schimmel', 'alter Greis')",
        answerC = "Tautologie ist absichtlich; Pleonasmus ist unabsichtlich",
        answerD = "Pleonasmus ist ein Stilfehler; Tautologie ein legitimes Stilmittel",
        correctAnswer = 1,
        explanation = "Pleonasmus: 'weißer Schimmel' (ein Schimmel ist per Definition ein weißes Pferd), 'nasser Regen' — das Attribut steckt schon im Substantiv. Tautologie: 'Das ist was es ist', 'Ein Kreis ist rund und hat keine Ecken' — der Sachverhalt wird zweimal mit anderen Worten gesagt. Beide können Stilfehler oder bewusste rhetorische Mittel sein.",
        difficulty = 3,
        funFact = "Oscar Wildes Witz funktioniert oft durch paradoxale Tautologie: 'I can resist everything except temptation.' Philosophisch ist die Tautologie ein Grundprinzip der Logik: 'Es regnet oder es regnet nicht' ist immer wahr, sagt aber nichts über die Welt aus."
    ),

    // Question 46 — Erzähltechnik: Pikaresker Roman
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was kennzeichnet den pikaresken Roman (Schelmenroman)?",
        answerA = "Er beschreibt den tugendhaften Aufstieg eines Helden durch die Gesellschaft",
        answerB = "Er ist ein Briefroman, in dem der Held sein Leben in Briefen erzählt",
        answerC = "Er folgt einem Schurken oder Außenseiter (Picaro), der durch alle Schichten der Gesellschaft wandert, diese satirisch entlarvend, und sich durch List und Anpassung durchschlägt",
        answerD = "Er gehört ausschließlich der spanischen Literatur des 17. Jahrhunderts an",
        correctAnswer = 2,
        explanation = "Der pikareske Roman (span. 'pícaro' = Schelm) entstand in Spanien ('Lazarillo de Tormes', 1554). Merkmale: episodische Struktur, satirische Gesellschaftsschau, Ich-Erzählung, moralische Ambiguität des Helden. In der deutschen Literatur: Grimmelshausens 'Simplicissimus' (1668), Grass' 'Blechtrommel' (1959) als moderner Nachklang.",
        difficulty = 3,
        funFact = "Günter Grass nannte Oskar Matzerath ausdrücklich einen 'Picaro der Gegenwart'. Oskar trommelt sich durch Nazi-Deutschland und die Nachkriegszeit — er überlebt durch Naivität, List und Anpassung, genau wie seine pikaresken Vorfahren."
    ),

    // Question 47 — Stilmittel: Metalepse
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was ist eine narrative Metalepse?",
        answerA = "Das Durchbrechen der Grenze zwischen Erzählebenen: Eine Figur tritt aus ihrer Erzählung heraus oder der Autor tritt in seine Erzählung ein",
        answerB = "Die Übertragung eines Begriffs auf ein anderes Wortfeld (synonym zu Metapher)",
        answerC = "Ein Stilmittel, bei dem ein Erzähler zugegeben explizit lügt",
        answerD = "Die vollständige Auflösung der Handlung, bevor sie beginnt",
        correctAnswer = 0,
        explanation = "Metalepse (geprägt von Gerard Genette) durchbricht ontologische Grenzen im Erzählen: Ein Romancharakter weiß, dass er in einem Roman vorkommt und spricht mit dem Autor. Beispiele: Cervantes' 'Don Quixote' (Figuren haben das erste Buch über sich gelesen), Pirandello ('Sechs Personen suchen einen Autor'), moderne Metafiction.",
        difficulty = 3,
        funFact = "In Laurence Sternes 'Tristram Shandy' (1759) stoppt der Erzähler die Handlung, um mit dem Leser zu diskutieren, macht Witze über seinen Schreibprozess und bricht alle Erzählkonventionen. Dieses Buch gilt als das erste postmoderne Werk — obwohl es 200 Jahre vor der Postmoderne erschien."
    ),

    // Question 48 — Stilmittel: Ambiguität
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welche literarische Funktion hat bewusste Ambiguität (Mehrdeutigkeit) in einem Text?",
        answerA = "Sie ist stets ein Fehler, der durch sorgfältige Überarbeitung beseitigt werden sollte",
        answerB = "Sie dient ausschließlich der Komik durch Missverständnisse",
        answerC = "Sie ist ein Merkmal schlechter Übersetzungen",
        answerD = "Sie öffnet den Text für mehrere gleichwertige Interpretationen, verweigert eine eindeutige Sinnfestlegung und aktiviert die Mitarbeit des Lesers bei der Bedeutungskonstruktion",
        correctAnswer = 3,
        explanation = "Bewusste Ambiguität ist ein Zeichen literarischer Qualität: William Empson beschrieb in 'Seven Types of Ambiguity' (1930), wie mehrdeutige Wörter und Strukturen den Text bereichern. Kafkas Texte sind radikal ambig — 'Der Prozess' lässt sich als religiöse Allegorie, Psychodrama oder Gesellschaftskritik lesen, ohne dass eine Interpretation die andere ausschließt.",
        difficulty = 3,
        funFact = "Paul Celan, der Dichter der Shoah, machte Ambiguität zum Prinzip. In 'Todesfuge' ist jedes Bild mehrfach deutbar — das Gedicht widersteht der Vereindeutigung, weil jede Festlegung die Komplexität des Traumas verkürzen würde."
    ),

    // Question 49 — Erzähltechnik: Homodiegetisch vs. heterodiegetisch
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was bedeutet 'homodiegetischer Erzähler' in Genettes Terminologie?",
        answerA = "Ein Erzähler, der dieselbe Geschichte wie ein anderer Erzähler im selben Roman erzählt",
        answerB = "Ein Erzähler, der zur erzählten Welt gehört — der selbst als Figur an der Handlung beteiligt ist (Ich-Erzähler)",
        answerC = "Ein Erzähler, der homogene Sprache ohne Dialekte oder Stilwechsel verwendet",
        answerD = "Ein Erzähler, dessen Identität im gesamten Roman verborgen bleibt",
        correctAnswer = 1,
        explanation = "Genette unterscheidet: Homodiegetisch = Erzähler existiert in der erzählten Welt (Figur im Roman, typischerweise Ich-Erzähler). Heterodiegetisch = Erzähler steht außerhalb der erzählten Welt (auktorialer Erzähler). Autodiegetisch = Sonderfall des Homodiegetischen: Erzähler ist die Hauptfigur der Geschichte.",
        difficulty = 3,
        funFact = "Moby-Dick beginnt mit 'Call me Ishmael' — homodiegetischer, autodiegetischer Erzähler. Aber Ishmael erzählt Szenen, bei denen er gar nicht dabei sein konnte (z.B. Gespräche unter vier Augen). Das ist eine narratologische Unmöglichkeit, die Melville bewusst einsetzt, um die Grenzen des Erzählens auszuloten."
    ),

    // Question 50 — Stilmittel: Topos
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was bezeichnet man in der Rhetorik und Literaturwissenschaft als 'Topos' (Plural: Topoi)?",
        answerA = "Den thematischen Schwerpunkt eines einzelnen Werkes",
        answerB = "Eine Metapher, die so oft verwendet wurde, dass sie ihre bildliche Wirkung verloren hat (synonym zu 'toter Metapher')",
        answerC = "Ein überliefertes, wiedererkennbares Denk- und Argumentationsmuster, ein konventionelles Motiv oder Bild, das in der literarischen Tradition immer wieder auftaucht",
        answerD = "Den geografischen Schauplatz, der für eine bestimmte Epoche typisch ist",
        correctAnswer = 2,
        explanation = "Der Topos (griech. 'Ort' — im übertragenen Sinn: 'Fundstelle') ist ein standardisiertes Argumentations- oder Bildmuster, das Autoren aus dem kulturellen Gedächtnis schöpfen. Ernst Robert Curtius katalogisierte in 'Europäische Literatur und lateinisches Mittelalter' (1948) die wichtigsten Topoi: 'Welt als Theater', 'puer senex' (jugendlicher Greis), 'ubi sunt' (Klage über Vergänglichkeit).",
        difficulty = 3,
        funFact = "Der 'ubi sunt'-Topos ('Wo sind sie nun?') zieht sich durch die gesamte Weltliteratur: von François Villons 'Où sont les neiges d'antan?' bis zu Bob Dylans 'Where have all the flowers gone?' Manche Topoi sind 3000 Jahre alt und erscheinen noch in heutigen Werbetexten."
    )

)
