package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsMaster2(): List<Question> = listOf(

    // ── Musikhandschriften / Quellenforschung (8) ────────────────────────────

    // Q1
    Question(
        categoryId = 5,
        questionText = "Welcher Musikforscher identifizierte 1943 die sogenannte 'Lochamer Liederbuch'-Handschrift als wichtigstes Zeugnis fuer deutschsprachige weltliche Vokalmusik des 15. Jahrhunderts?",
        answerA = "Friedrich Blume",
        answerB = "Konrad Ameln",
        answerC = "Heinrich Besseler",
        answerD = "Ernst Mohr",
        correctAnswer = 1,
        explanation = "Konrad Ameln leistete die grundlegende Erschliessung des Lochamer Liederbuches (entstanden um 1452-1460). Die Handschrift enthaelt rund 45 einstimmige weltliche Lieder und ist eine Hauptquelle fuer die Kenntnis des deutschen Liedgesangs in der fruehen Mensuralnotation.",
        difficulty = 5,
        funFact = "Das Lochamer Liederbuch ist nach dem Nurnberger Patrizier Wolflein von Lochamer benannt, der vermutlich der erste Besitzer war. Es enthaelt auch fruehe Intabulierungen fuer Lauteninstrumente."
    ),

    // Q2
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Squarcialupi-Codex' und wann wurde er beschrieben?",
        answerA = "Ein venezianischer Messenkodex des 15. Jahrhunderts mit ausschliesslich Dufay-Werken",
        answerB = "Eine Florentiner Prachthandschrift (ca. 1410-1415) mit italienischen Trecento-Kompositionen von 12 Meistern samt deren Portraets",
        answerC = "Ein roemischer Quellenkodex des fruehen 16. Jahrhunderts mit Motetten Josquins",
        answerD = "Eine Sammlung burgundischer Chansons, kopiert fuer den Hof Philipps des Guten",
        correctAnswer = 1,
        explanation = "Der Squarcialupi-Codex (Biblioteca Medicea Laurenziana, Florenz) ist die reichhaltigste Quelle der Trecento-Musik. Er enthaelt Werke von Francesco Landini, Jacopo da Bologna, Giovanni da Cascia u.a. und wurde nach dem florentinischen Organisten Antonio Squarcialupi benannt, der ihn besass.",
        difficulty = 5,
        funFact = "Die Handschrift zeigt Miniaturportraets der 12 dargestellten Komponisten — eine fuer das Mittelalter aussergewoehnliche Wuerdigung von Musikerpersoenlichkeiten als Individuen."
    ),

    // Q3
    Question(
        categoryId = 5,
        questionText = "Welche Bedeutung hat das 'Old Hall Manuscript' fuer die englische Musikgeschichte?",
        answerA = "Es ist die einzige bekannte Quelle fuer Henry Purcells fruehe Kirchenmusik",
        answerB = "Es ist die bedeutendste Handschrift englischer Polyphonie um 1400-1420, darunter Werke von Leonel Power und moeglicherweise dem jungen John Dunstaple",
        answerC = "Es enthaelt die ersten schriftlich ueberlieferten anglikanischen Anthems aus der Tudor-Zeit",
        answerD = "Es ist eine Kopie der Notre-Dame-Schule, angefertigt am Hof Edwards III.",
        correctAnswer = 1,
        explanation = "Das Old Hall Manuscript (British Library, ca. 1410-1420) ist die wichtigste Quelle englischer Polyphonie aus der fruehen Mensuralzeit. Mit etwa 140 Kompositionen dokumentiert es Messen, Motetten und Hymnen. Leonel Power ist mit den meisten Stuecken vertreten; ein Beitrag des jungen Dunstaple wird diskutiert.",
        difficulty = 5,
        funFact = "Das Manuskript kam nach der Reformation in den Besitz des Augustinerklosters St. Edmund's College in Old Hall (Hertfordshire) — daher sein Name. Es wurde erst 1893 von William Barclay Squire wissenschaftlich erschlossen."
    ),

    // Q4
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Fachbegriff 'Stemma' (oder 'Stemma codicum') in der Musikphilologie?",
        answerA = "Die chronologische Liste aller Autographe eines Komponisten",
        answerB = "Ein Stammbaum der handschriftlichen Ueberlieferung, der verwandtschaftliche Beziehungen zwischen Quellen und einen gemeinsamen Archetypus zeigt",
        answerC = "Die Tabelle der Schluessel und Vorzeichen in mittelalterlichen Handschriften",
        answerD = "Das Verzeichnis aller Kopisten, die an einer Handschrift gearbeitet haben",
        correctAnswer = 1,
        explanation = "Das Stemma codicum (Hs.-Stammbaum) rekonstruiert anhand gemeinsamer Fehler und Lesarten die Abstammungsverhaeltnisse zwischen Handschriften. Es erlaubt, korrupte Textstellen zu identifizieren und den hypothetischen Archetypus (naechste gemeinsame Vorstufe aller Zeugen) zu bestimmen — Grundlage jeder kritischen Edition.",
        difficulty = 5,
        funFact = "Die Methode geht auf Karl Lachmann (19. Jahrhundert) zurueck, der sie fuer Textkritik antiker Literatur entwickelte. Musikphilologen wenden sie seit dem fruehen 20. Jahrhundert auf Notenhandschriften an."
    ),

    // Q5
    Question(
        categoryId = 5,
        questionText = "In welchem Archiv befindet sich die umfangreichste Sammlung von Beethoven-Autographen weltweit?",
        answerA = "Oesterreichische Nationalbibliothek, Wien",
        answerB = "Beethoven-Haus, Bonn",
        answerC = "Staatsbibliothek zu Berlin, Preussischer Kulturbesitz",
        answerD = "British Library, London",
        correctAnswer = 2,
        explanation = "Die Staatsbibliothek zu Berlin (Musikabteilung) bewahrt die groesste Sammlung von Beethoven-Autographen, darunter die Originalpartituren der 3., 5., 6. und 9. Sinfonie. Die Sammlung gehoerte urspruenglich der Preussischen Staatsbibliothek und war waehrend des Zweiten Weltkrieges nach Krakau ausgelagert, was ihre Rettung sicherte.",
        difficulty = 5,
        funFact = "Beethovens Skizzenhefte ('Konversationshefte') sind eine einzigartige Quelle: Er fuehlte sie als Tauber — seine Gespraechspartner schrieben ihre Seite des Dialogs, er antwortete muendlich. Nur seine Partnerseite ist ueberliefert."
    ),

    // Q6
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Codex Calixtinus' und welche musikhistorische Bedeutung hat er?",
        answerA = "Ein karolingisches Gesangbuch mit der aeltesten bekannten mehrstimmigen Musik",
        answerB = "Ein Pilgerhandbuch des 12. Jahrhunderts aus Santiago de Compostela mit den aeltesten bekannten notationstechnisch praezisen zweiteiligen Organa",
        answerC = "Der Kodex mit Palestrinas saemtlichen Papstmessen in autographer Ueberlieferung",
        answerD = "Eine franzoesische Troubadour-Handschrift mit Melodien des 12.-13. Jahrhunderts",
        correctAnswer = 1,
        explanation = "Der Codex Calixtinus (ca. 1150, Kathedrale Santiago de Compostela) enthaelt im fuenften Buch (Liber sancti Jacobi) mehrstimmige Musiktexte, darunter das 'Congaudeant catholici' — moeglichweise das aelteste dreistimmige Stueck der europaeischen Musikgeschichte. Die Notation ist fuer das 12. Jahrhundert bemerkenswert praezise.",
        difficulty = 5,
        funFact = "Der Codex wurde 2011 gestohlen — ein ehemaliger Hausmeister der Kathedrale versteckte ihn 10 Jahre in seiner Garage. Der Diebstahl schuettelte die Fachwelt auf; das Buch gilt als eines der wertvollsten mittelalterlichen Manuskripte Spaniens."
    ),

    // Q7
    Question(
        categoryId = 5,
        questionText = "Was versteht man in der Quellenforschung unter einem 'Concordance'-Befund bei Musikhandschriften?",
        answerA = "Das Vorhandensein desselben Werkes in mehreren unabhaengigen Quellen, was Rueckschluesse auf Verbreitung und Textvarianten erlaubt",
        answerB = "Die uebereinstimmende Datierung einer Handschrift durch Papier- und Tintenanalyse",
        answerC = "Ein Eintrag im Incipit-Register, der Werke nach Anfangssilben des Textes verzeichnet",
        answerD = "Die Uebereinstimmung zwischen Neumen und ihrer spaeten Quadratnoten-Transkription",
        correctAnswer = 0,
        explanation = "Eine Concordance bezeichnet das Auftreten des gleichen Stueckes in mehreren Quellen (Handschriften oder Drucken). Concordances sind methodisch zentral: Sie erlauben den Textvergleich, Fehlerkorrektur und Rueckschluesse auf die Verbreitungswege eines Werkes. Fehlen Concordances, spricht man von 'unikaler Ueberlieferung'.",
        difficulty = 5,
        funFact = "Das RISM (Repertoire International des Sources Musicales) ist das groesste internationale Projekt zur systematischen Erschliessung von Musikquellen und Concordances — mit uber 1,5 Millionen erfassten Musikhandschriften weltweit."
    ),

    // Q8
    Question(
        categoryId = 5,
        questionText = "Welche Besonderheit kennzeichnet die Notation des 'Bamberger Kodex' (Ba) aus dem 13. Jahrhundert?",
        answerA = "Erstmaliger Einsatz von schwarzen Mensuralnotenkoepfen mit vollstaendiger Rhythmusdifferenzierung",
        answerB = "Modalnotation in fuenf Modi, ergaenzt durch eine fruehe Form der Modalrhythmus-Differenzierung fuer Motetten",
        answerC = "Quadratnoten-Choralnotation ohne jede rhythmische Bezeichnung (nota quadrata)",
        answerD = "Fruehes Tablatursystem fuer Zupfinstrumente mit Griffpositionen statt Tonhoehen",
        correctAnswer = 1,
        explanation = "Der Bamberger Kodex (Staatsbibliothek Bamberg, ca. 1280) enthaelt rund 100 lateinische und franzoesische Motetten der Notre-Dame-Epoche und ihrer Nachfolge. Er nutzt Modalnotation (Ligaturen nach modale Rhythmik), zeigt aber auch Uebergangsformen zur freieren Mensural-Notation — ein Schluesselzeugnis des Stils der ars antiqua.",
        difficulty = 5,
        funFact = "Der Kodex ist eines der aeltesten Zeugnisse fuer die Motette als eigenstaendige Gattung. Die Texte wechseln oft mitten im Stueck zwischen Latein und Alt-Franzoesisch — typisch fuer die pluritextuelle Motette des 13. Jahrhunderts."
    ),

    // ── Akustische Physik / Psychoakustik (8) ───────────────────────────────

    // Q9
    Question(
        categoryId = 5,
        questionText = "Was beschreibt das 'Haas-Effekt' (Praezedenzeffekt) in der Psychoakustik?",
        answerA = "Das Phaenomen, dass ein Echo, das innerhalb von ca. 40 ms nach dem Direktschall eintrifft, nicht als getrenntes Echo wahrgenommen wird, sondern die Richtungslokalisation des Direktschalls verstaerkt",
        answerB = "Die Wahrnehmung von Schwebungen bei zwei nahezu identischen Frequenzen",
        answerC = "Die Verdopplung der empfundenen Lautstaerke bei Verdopplung der Schallquellenanzahl",
        answerD = "Der Effekt, dass hohe Frequenzen als lauter empfunden werden als tiefe bei gleicher physikalischer Amplitude",
        correctAnswer = 0,
        explanation = "Helmut Haas beschrieb 1949 (Dissertation Goettingen), dass Schallreflexionen innerhalb von 30-40 ms nach dem Direktschall nicht als Echo, sondern als Richtungsverstaerkung wahrgenommen werden (Fusion). Erst darueber hinaus entstehen wahrnehmbare Echos. Grundlage fuer die Konzeption von Beschallungsanlagen und Delay-Einstellungen in der PA-Technik.",
        difficulty = 5,
        funFact = "Der Haas-Effekt wird bei Delay-Towers in Konzerthallen und Open-Air-Veranstaltungen genutzt: Fernere Lautsprecher werden leicht verzeoegert, damit der Schall klanglich vom nahen Direktschall kommt — das verhindert irritierendes Echonachhoeren."
    ),

    // Q10
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Bark-Skala' und wozu wird sie in der Psychoakustik verwendet?",
        answerA = "Eine logarithmische Frequenzskala, die dem menschlichen Hoersystem angepasst ist und 24 kritische Baender (Barks) entsprechend der Basilarmembran-Topologie abbildet",
        answerB = "Eine Lautstaerkeskala, die dBA-Werte in subjektive Phonwerte umrechnet",
        answerC = "Das Masking-Threshold-Diagramm fuer die MP3-Kompression",
        answerD = "Eine Skala zur Berechnung der Nachhallzeit (RT60) in Konzertsaelen",
        correctAnswer = 0,
        explanation = "Die Bark-Skala (benannt nach dem Akustiker Heinrich Barkhausen) teilt das Hoerspektrum in 24 'kritische Baender', die den Frequenzgruppen der Basilarmembran entsprechen. Innerhalb eines Barks wirkt Masking am staerksten. Die Skala ist Grundlage psychoakustischer Modelle in der Audiokompression (MP3, AAC) und der Raumakustik.",
        difficulty = 5,
        funFact = "Die 24 Bark-Baender reichen von 0-100 Hz (Bark 1) bis 13,5-15,5 kHz (Bark 24). Die Bandbreite der Baender waechst mit der Frequenz — genau wie die Frequenzaufloesung der Basilarmembran abnimmt."
    ),

    // Q11
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Kombinationstönen' (auch 'Tartini-Toene') bei der gleichzeitigen Erklingen zweier Toene?",
        answerA = "Toene, deren Frequenz der Differenz oder Summe der Ausgangsfrequenzen entspricht und die durch nichtlineare Verarbeitung im Gehoer entstehen",
        answerB = "Obertoeane, die entstehen, wenn ein Instrument bei bestimmten Dynamikniveaus anklingt",
        answerC = "Schwebungsfrequenzen zwischen zwei identischen Tonquellen mit minimalem Stimmversatz",
        answerD = "Resonanzverstaerkungen im Gehoergang, die bestimmte Frequenzen bevorzugen",
        correctAnswer = 0,
        explanation = "Kombinationstoeane (Diff.-Ton = f2-f1, Summationston = f1+f2) entstehen durch nichtlineare Verarbeitung im Innenohr (Basilarmembran-Nichtlinearitaet). Giuseppe Tartini beschrieb den Differenzton 1714 als 'terzo suono'. Violinisten nutzten ihn als Intonationshilfe bei Doppelgriffen.",
        difficulty = 5,
        funFact = "Tartinis Entdeckung blieb lange ein Mysterium: Warum hört man einen Ton, der physikalisch nicht in der Luft vorhanden ist? Die Antwort liegt in der nichtlinearen Membranmechanik des Corti-Organs — das Ohr rechnet aktiv mit."
    ),

    // Q12
    Question(
        categoryId = 5,
        questionText = "Was ist der 'Missing Fundamental'-Effekt (Residualton)?",
        answerA = "Die Wahrnehmung eines tiefen Grundtons, obwohl dieser physikalisch nicht vorhanden ist, wenn seine Obertoene praesent sind",
        answerB = "Der Verlust der Hochfrequenzhoerigkeit im Alter (Presbyakusis)",
        answerC = "Das Fehlen der Grundfrequenz in der Stimmfunktion bei Patienten nach Laryngektomie",
        answerD = "Die fehlende Oktave in historischen Orgeln (kurze Oktave)",
        correctAnswer = 0,
        explanation = "Der Missing-Fundamental-Effekt: Das Gehoer nimmt den Grundton einer harmonischen Reihe auch dann wahr, wenn er fehlt. Beispiel: 300, 400, 500 Hz klingen als 100 Hz Grundton. Grundlage dieser Periodizitaetshoerung ist die mustererkennnende Verarbeitung im Hirnstamm (Nucleus cochlearis). Ermoeglicht Telefonverstehbarkeit bei fehlendem Bassanteil.",
        difficulty = 5,
        funFact = "Telefone uebertragen Frequenzen nur bis etwa 3,4 kHz — der menschliche Grundton liegt bei 80-300 Hz. Trotzdem erkennt das Gehoer die Stimme klar, weil die Obertoeane genuegen, um den Grundton zu rekonstruieren."
    ),

    // Q13
    Question(
        categoryId = 5,
        questionText = "Was beschreibt das Konzept der 'Auditory Scene Analysis' nach Albert Bregman (1990)?",
        answerA = "Die Faehigkeit des Gehoersystems, simultane und sukzessive akustische Reize in getrennte Wahrnehmungsstroeme (Streams) zu gruppieren — Grundlage des Cocktailparty-Effekts",
        answerB = "Die Analyse von Raumakustik mittels binauralem Impulsantwort-Verfahren",
        answerC = "Das psychoakustische Modell zur Berechnung optimaler Lautstaerke-Normierung in Rundfunkproduktionen",
        answerD = "Die kognitive Karte des Klangraums, die Menschen nach Musikgenus-Praeferenz differenziert",
        correctAnswer = 0,
        explanation = "Albert Bregmans 'Auditory Scene Analysis' (1990, MIT Press) ist das Standardwerk ueber auditive Gruppierung: Das Gehoer trennt akustische Szenen in Stroeme nach Prinzipien wie Naehe (Frequenz, Zeit), Aehnlichkeit, Kontinuitaet und gemeinsamem Schicksal. Grundlage fuer das Verstaendnis von Polyphoniebewusstsein und der Cocktailparty-Phaenomenologie.",
        difficulty = 5,
        funFact = "Bregmans Ideen erklaeren, warum wir im vollen Orchester einzelne Instrumente heraushoren koennen — und warum das Gehoer KI-Systeme bis heute ueberlfuegelt, wenn es darum geht, Stimmen in Laerm zu trennen."
    ),

    // Q14
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Equal Loudness Contour' (Fletcher-Munson-Kurve)?",
        answerA = "Eine Kurve, die zeigt, welchen Schalldruckpegel verschiedene Frequenzen benoetigen, um als gleich laut empfunden zu werden — mit Maximum der Empfindlichkeit um 3-4 kHz",
        answerB = "Die Abklingkurve eines Klanges von seinem Peak-Pegel bis zur Unhoebarkeitsgrenze",
        answerC = "Das Spektrum weissen Rauschens nach Bandbegrenzung auf das Hoerfeld",
        answerD = "Die Kurve des Frequenzgangs eines kalibrierten Messmikrofons (IEC 61094)",
        correctAnswer = 0,
        explanation = "Fletcher und Munson (1933, Bell Labs) mass erstmals systematisch, wie der benoetigte Schalldruckpegel fuer gleiche empfundene Lautstaerke frequenzabhaengig variiert. Bei 1 kHz ist die Kurve Referenz (Phon = dB). Bei tiefen Frequenzen und Hoechstfrequenzen muss der Schall viel lauter sein, um gleich laut zu klingen. Grundlage der Lautstaerkenormierung (Phon, Sone).",
        difficulty = 5,
        funFact = "Der 'Loudness'-Knopf alter HiFi-Verstaerker boostete bei geringen Lautstaerken Baeasse und Hoehen gezielt nach dem Fletcher-Munson-Prinzip — damit leise Musik 'vollstaendig' klingt."
    ),

    // Q15
    Question(
        categoryId = 5,
        questionText = "Was beschreibt der Begriff 'Informationsgehalt' eines Tons im Sinne der Shannon'schen Informationstheorie in Bezug auf Musik?",
        answerA = "Die Menge an Bits, die benoetigt wird, um einen Klang digital zu kodieren (Bitrate x Zeit)",
        answerB = "Den Grad der Unerwartheit eines musikalischen Ereignisses relativ zu einem probabilistischen Modell des Kontextes — Mass fuer Ueberraschung und damit auch Ausdruck",
        answerC = "Das Verhaeltnis zwischen Nutzsignal und Rauschen in der Audiouebertragung (SNR)",
        answerD = "Die Anzahl gleichzeitiger Stimmen, die ein Hoerer bei einem Musikstueck unterscheiden kann",
        correctAnswer = 1,
        explanation = "In der Informationsaesthetik (Meyer, Huron, Margulis) gilt: Informationsgehalt = -log2(P) eines Ereignisses. Hohe Erwartung → geringe Information → wenig Ausdruck. Unerwartetem → hohe Information → Spannung/Ausdruck. Das Leonard B. Meyers Werk 'Emotion and Meaning in Music' (1956) und David Hurons 'Sweet Anticipation' (2006) bauen hierauf auf.",
        difficulty = 5,
        funFact = "David Huron zeigte experimentell, dass musikalische 'Gaensehaut'-Momente oft an Stellen auftreten, die statistisch unerwartet sind — aber unmittelbar danach werden die erwarteten Fortfuehrungen umso intensiver empfunden (ITPRA-Theorie: Imagination, Tension, Prediction, Reaction, Appraisal)."
    ),

    // Q16
    Question(
        categoryId = 5,
        questionText = "Was ist der 'Eigenresonanzbereich' (Formant) in der Vokalakustik und welche Formanten unterscheiden Vokalqualitaeten?",
        answerA = "Formanten sind Resonanzgipfel im Spektrum der menschlichen Stimme, erzeugt durch Vokaltrakt-Resonanzen: F1 korreliert mit Zungenhoehenposition, F2 mit Zungenfrontposition",
        answerB = "Formanten sind Obertoeane, die durch selektives Daempfen bestimmter Frequenzen im Larynx erzeugt werden",
        answerC = "Formanten bezeichnen die Grundfrequenzanteile, die ein Saenger betont, um durch einen grossen Saal zu tragen",
        answerD = "Formanten sind Resonanzphaenomene des Brustresonators, nicht des Vokaltrakts",
        correctAnswer = 0,
        explanation = "Die Formanten F1 und F2 sind Resonanzgipfel des Vokaltrakts (Mund-Rachen-Raum). F1 (500-1000 Hz) steigt mit sinkender Zungenlage (/a/ > /e/ > /i/). F2 (1000-3000 Hz) steigt von hinten nach vorne (/u/ < /o/ < /a/). Die Kombination F1/F2 definiert jeden Vokalklang eindeutig — Grundlage der Sprachakustik nach Peterson und Barney (1952).",
        difficulty = 5,
        funFact = "Opernsaenger trainieren einen 'Saengerformanten' bei ca. 2500-3000 Hz, der es ihnen erlaubt, ohne Mikrofonverstaerkung ueber ein vollstaendiges Orchester zu singen — das menschliche Ohr ist in diesem Bereich am empfindlichsten."
    ),

    // ── Musikphilosophie (7) ─────────────────────────────────────────────────

    // Q17
    Question(
        categoryId = 5,
        questionText = "Welche Position vertritt Eduard Hanslick in seiner Schrift 'Vom Musikalisch-Schoenen' (1854) bezueglich des Wesens der Musik?",
        answerA = "Musik ist der direkteste Ausdruck seelischer Gefuehle, darin allen anderen Kuensten ueberlegen",
        answerB = "Das Schoene der Musik ist 'tonendbewegter Form' — sie hat keinen aussermusikalischen Inhalt, ihr Wesen liegt im selbstbezueglichen Spiel mit Klangmaterial",
        answerC = "Musik ist Sprache der absoluten Idee im Sinne Hegels und vermittelt metaphysische Wahrheit",
        answerD = "Der Wert der Musik ist ausschliesslich durch ihre soziale Funktion in Ritualen bestimmt",
        correctAnswer = 1,
        explanation = "Hanslicks 'Formalismus' (1854) ist die einflussreichste Position der autonomen Musikaesthetik: Musik bedeutet nichts ausser sich selbst. 'Tonendbewegten Form' — das Spiel der Toene in ihrer zeitlichen Entfaltung — ist ihr eigentlicher Gehalt. Damit wandte er sich gegen das Programm der 'Neudeutschen Schule' (Liszt, Wagner), die Musik als Ausdruck aussermusikalischer Inhalte verstand.",
        difficulty = 5,
        funFact = "Hanslick war Musikkritiker in Wien und scharfer Widersacher Wagners. Wagner antwortete mit einer kaum verhuellten Karikatur: Der Beckmesser in den 'Meistersingern' hiess urspruenglich 'Hanslich' im Entwurf."
    ),

    // Q18
    Question(
        categoryId = 5,
        questionText = "Was versteht Theodor W. Adorno unter 'Warencharakter' der Musik in seiner 'Philosophie der neuen Musik' (1949)?",
        answerA = "Die Reduktion von Musik auf handelbare Produkte, bei der der Tauschwert (Popularitaet, Verkaufszahl) den Gebrauchswert (aesthetischen Gehalt) verdraengt und Musik zum Konsumartikel wird",
        answerB = "Die Notwendigkeit, Musik als materielle Ware zu schiffen, bevor sie Konzertsaele erreicht",
        answerC = "Die kompositorische Anpassung an Marktbeduerfnisse, die Adorno bei der Zweiten Wiener Schule kritisierte",
        answerD = "Das Urheberrechtssystem, das Noten und Aufnahmen rechtlich zur Ware macht",
        correctAnswer = 0,
        explanation = "Adornos Warencharakter-Kritik (im Anschluss an Marx) besagt: In der kapitalistischen Kulturindustrie verliert Musik ihren immanenten aesthetischen Sinn und wird nach Tauschwert-Logik produziert und konsumiert. Popularmusik wird so zu 'Kulturindustrie' (mit Horkheimer: 'Dialektik der Aufklaerung', 1944) — standardisiert, pseudoindividuell und affirmmativ.",
        difficulty = 5,
        funFact = "Adorno war selbst Komponist und Schoenberg-Schueler. Seine Analyse der Kulturindustrie blieb hochumstritten — Kritiker warfen ihm eurozentrischen Hochkulturduelkel vor, waehrend andere seine Diagnose der digitalen Streaming-Oekonomie bestaetigst sehen."
    ),

    // Q19
    Question(
        categoryId = 5,
        questionText = "Was meint Arthur Schopenhauer mit seiner These, Musik sei 'Abbild des Willens selbst' (Welt als Wille und Vorstellung, 1818)?",
        answerA = "Musik imitiert die sichtbare Welt der Erscheinungen wie Malerei und Dichtung, jedoch mit groesserer Praezision",
        answerB = "Musik ist einzigartig unter den Kuensten: Sie stellt nicht (wie andere Kuenste) Ideen (Platonische Urbilder) dar, sondern den Willen — das metaphysische Urprinzip der Welt — direkt und unvermittelt",
        answerC = "Musik drueckt den Willen des Komponisten zum Kunstschaffen aus, nicht des Publikums",
        answerD = "Musik ist Abbild des Willens zum Leben in seinem biologisch-triebhaften Aspekt, mithin eine minderwertige Kunst",
        correctAnswer = 1,
        explanation = "Schopenhauers Musikphilosophie: Alle anderen Kuenste stellen Ideen dar (Platon'sche Urbilder des Willens). Musik hingegen umgeht diese Repraesentationsebene und ist Abbild des Willens selbst — des blinden Strebens, das Grundprinzip aller Realitaet. Tiefe Basst oene = unorganische Natur; Melodie = Erkenntnisstufen bis zum Menschlichen. Wagner griff diese Idee enthusiastisch auf.",
        difficulty = 5,
        funFact = "Schopenhauer praeferierte Mozart und Rossini — seine Philosophie beguenstigte Wagners Musikdrama, obwohl er Wagners Werk noch nicht kannte. Wagner las Schopenhauer 1854, bezeichnete ihn als seine groesste Entdeckung und widmete ihm die Huldigung 'Wesendonck-Lieder' indirekt."
    ),

    // Q20
    Question(
        categoryId = 5,
        questionText = "Was besagt Nelson Goodmans Unterscheidung zwischen 'autographischen' und 'allographischen' Kuensten in 'Languages of Art' (1968) fuer die Musik?",
        answerA = "Musik ist allographisch: Identitaet eines Werkes beruht auf korrekter Notation, nicht auf physischer Herkunft; jede werktreue Auffuehrung ist eine Instanz des Werkes",
        answerB = "Musik ist autographisch, weil jede Auffuehrung ein einzigartiges Ereignis ist, das nicht reproduziert werden kann",
        answerC = "Musik ist autographisch in der Komposition, allographisch in der Aufnahme",
        answerD = "Die Unterscheidung ist auf Musik nicht anwendbar, weil Notation kein vollstaendiges Werk-Aquivalent ist",
        correctAnswer = 0,
        explanation = "Goodman: Autographische Kuenste (Malerei, Skulptur) haben Originale, deren Geschichte konstitutiv ist — eine Faelschung ist aesthetisch minderwertig. Allographische Kuenste (Musik, Literatur) definieren Werkidentitaet durch Einhaltung der Notation: Jede authentische Auffuehrung ist das Werk, unabhaengig davon, wer oder wann. Die Originalpartitur ist nicht das Werk.",
        difficulty = 5,
        funFact = "Goodmans Theorie hatte praktische Folgen fuer die Auffuehrungspraxis-Debatte: Wenn Werkidentitaet durch Notationstreue definiert ist, sind 'historisch informierte Auffuehrungen' nicht aesthetisch privilegiert — es genuegt die korrekte Notation."
    ),

    // Q21
    Question(
        categoryId = 5,
        questionText = "Was versteht Peter Kivy unter 'Absolute Music' als epistemischer Kategorie in seiner Musikphilosophie?",
        answerA = "Musik, die von jeglichem Programminhalt, Text oder repraesentativen Elementen frei ist und ihren Sinn allein aus struktureller Organisation gewinnt",
        answerB = "Musik, die hoechste aesthetische Vollkommenheit erreicht und deshalb als unuebertroffen gilt",
        answerC = "Musik, die ausschliesslich unveraendert nach Originalpartituren aufgefuehrt werden darf",
        answerD = "Instrumnentalmusik des 18. Jahrhunderts, die nach absolutistischen Fuerstenwuenschen entstand",
        correctAnswer = 0,
        explanation = "Peter Kivy (u.a. 'Absolute Music', 2009) verteidigt den Begriff 'absolute Musik' als Bezeichnung fuer rein instrumentale, 'textlose' Musik ohne repraesentationalen Gehalt. Ihr Genuss sei 'enhanced formalism' — man hoert strukturelle Zusammenhaenge direkt als aesthetisch befriedigend. Er wendet sich gegen Emotionstheorien, die Musik quasi-sprachliche Inhalte zuschreiben.",
        difficulty = 5,
        funFact = "Der Begriff 'absolute Musik' wurde von Wagner 1846 als polemische Ablehnung der rein instrumentalen Tradition gepraegt — er wollte damit zeigen, dass Musik ohne Drama 'unvollstaendig' sei. Ironischerweise adaptierte die autonome Musikaesthetik den Begriff als Ehrentitel."
    ),

    // Q22
    Question(
        categoryId = 5,
        questionText = "Was ist das Kernargument von Susanne Langers Musikphilosophie in 'Philosophy in a New Key' (1942)?",
        answerA = "Musik ist ein 'Symbol der menschlichen Gefuehlslebens' — kein Ausdruck von Gefuehlen, sondern ein ikonisches Abbild ihrer morphologischen Form (Spannung, Aufloessung, Rhythmus)",
        answerB = "Musik ist ein System von Zeichen mit eindeutigem Referenten wie eine Sprache, nur ohne Propositionsgehalt",
        answerC = "Musik hat keine semantische Dimension; sie ist 'sinnliche Selbstgenuegsamkeit' im Kantischen Sinne",
        answerD = "Musik ist Ausdruck kollektiver unbewusster Archetypen nach C.G. Jung",
        correctAnswer = 0,
        explanation = "Langer unterscheidet 'diskursive Symbolik' (Sprache, logische Strukturen) von 'praesentativer Symbolik' (Musik, bildende Kunst). Musik ist eine praesentative Symbolik: Sie bildet die Form des Gefuehlslebens (nicht spezifische Gefuehle) ab — Spannungsboegen, Pulse, Rhythmen. Sie ist kein Gegenstand aesthetischen Vergnuegens, sondern Wissen der Innenlebens-Morphologie.",
        difficulty = 5,
        funFact = "Langer war Schuelerlin Ernst Cassirers. Ihr Ansatz beeinflusste spaeter die Kognitionswissenschaft der Musik: Musikpsychologen wie John Sloboda und David Huron greifen implizit auf Langers Symbol-Konzept zurueck."
    ),

    // Q23
    Question(
        categoryId = 5,
        questionText = "Was meinte Friedrich Nietzsche mit der Gegenuebersetzung von 'apollinisch' und 'dionysisch' bezueglich der Musik in 'Die Geburt der Tragoedie' (1872)?",
        answerA = "Apollinisch = klare Formen, Mass, Individuation, Bild; dionysisch = Selbstaufloessung, Rausch, Urklang jenseits aller Form — Musik ist das dionysische Prinzip par excellence, Aufloessung des Individualprinzips",
        answerB = "Apollinisch = Melodie und Einfachheit (Mozart); dionysisch = Harmonik und Chromatik (Wagner)",
        answerC = "Beide Prinzipien sind in der Musik gleichwertig vertreten — das Apollinische im Rhythmus, das Dionysische in der Harmonie",
        answerD = "Apollinisch = griechische Volksmusik; dionysisch = oestliche Einflueasse auf die griechische Kunst",
        correctAnswer = 0,
        explanation = "In Nietzsches Fruehwerk: Das Apollinische schafft schoene Formen und Grenzen (Plastik, Epos). Das Dionysische ist das rauschhaft-ekstatische Prinzip, das alle Grenzen aufloestu — sein direkter Ausdruck ist die Musik. Die griechische Tragoedie war fuer Nietzsche die Synthese beider Prinzipien, deren Verfall durch den sokratischen Rationalismus (Euripides) begann.",
        difficulty = 5,
        funFact = "Nietzsche widmete die 'Geburt der Tragoedie' Richard Wagner und verstand Wagners Musikdrama als Erneuerung der Antike. Die Schrift ist von Wagner beeinflusst — Nietzsche brach spaeter radikal mit Wagner, den er danach als 'Kulturkrankheit' bezeichnete."
    ),

    // ── Mittelalterliche Musiktheorie (7) ────────────────────────────────────

    // Q24
    Question(
        categoryId = 5,
        questionText = "Welche Funktion hatte die 'Guidonische Hand' (Mano guidoniana), die Guido von Arezzo im 11. Jahrhundert entwickelte oder die unter seinem Namen ueberliefert wurde?",
        answerA = "Ein mnemonisches System, das jedem Fingergelenk der linken Hand eine Silbe des Hexachord-Systems (ut, re, mi, fa, sol, la) zuordnete und Saengern half, Intervalle aus dem Gedaechtnis abzurufen",
        answerB = "Eine Methode zur Handzeichen-Dirigierung im Chorgesang, vergleichbar der modernen Taktierpraxis",
        answerC = "Ein System zur Notation von Rhythmus durch Fingerstellungen, das die Modalnotation vorbereitete",
        answerD = "Ein Geraet zur Stimmung von Kirchenorgeln durch Handmass-Proportionen",
        correctAnswer = 0,
        explanation = "Die Guidonische Hand weist jedem Glied des linken Handskeletts (20 Positionen + Fingerspitzen) eine der Hexachord-Silben der gamut (Gesamttonleiter) zu. Saenger konnten so mit dem Zeigefinger der rechten Hand die linke Hand 'lesen' und Intervalle und Tonfolgen ohne Partitur abrufen. Das System erleichterte die muendliche Lehrpraxis.",
        difficulty = 5,
        funFact = "Ob Guido die Hand selbst erfand, ist historisch umstritten — sie erscheint nicht in seinen datierten Schriften, sondern in spaeterem Lehrkontext. Dennoch wurde sie als 'guidonisch' kanonisiert und war bis ins 16. Jahrhundert in Gebrauch."
    ),

    // Q25
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Musica ficta' in der mittelalterlichen und renaissance-zeitlichen Musiktheorie?",
        answerA = "Die chromatischen Alterationen (Erhoehung/Erniedrigung) von Toenen ausserhalb des offiziellen Hexachord-Systems, die in der Praxis noetig waren, aber in Handschriften oft nicht notiert wurden",
        answerB = "Mehrstimmige Musik, die nicht nach theoretischen Regeln, sondern nach Gehoer komponiert wurde",
        answerC = "Fingierte oder unechte Kompositionen, die faelschlicherweise beruehmten Meistern zugeschrieben wurden",
        answerD = "Instrumentalmusik, die aus Vokalsaetzen ohne Text transkribiert wurde",
        correctAnswer = 0,
        explanation = "Musica ficta (auch musica falsa) bezeichnet Toene ausserhalb des hexachordalen Systems (die 'ficta' stehen), die Saenger/Spieler nach Konvention hinzufuegten: Leittonbildung vor Kadenzen, Tritonus-Vermeidung, Terz-Vergroesserung. Ihre Nicht-Notation in Quellen stellt Editoren vor grosse Rekonstruktionsprobleme.",
        difficulty = 5,
        funFact = "Die Frage, wieviel Musica ficta historisch hinzuzufuegen ist, ist heute eine der umstrittensten Debatten der historischen Auffuehrungspraxis. Falsche Entscheidungen koennen harmonischen Charakter und Modalitaet eines Stueckes grundlegend veraendern."
    ),

    // Q26
    Question(
        categoryId = 5,
        questionText = "Welche Intervalle galten in der Musiktheorie des Mittelalters als 'perfecte Konsonanzen', und warum?",
        answerA = "Prim, Quinte, Oktave — ihre Frequenzverhaeltnisse (1:1, 2:3, 1:2) liegen als kleinste ganzzahlige Proportionen am naechsten bei der Reinheit des 'numerus sonorus'",
        answerB = "Terz und Sexte, weil sie die melodischsten Intervalle der Gregorianik sind",
        answerC = "Prim, Quarte und grosse Terz, nach dem Vorbild der pythagoraeischen Saiten-Experimente",
        answerD = "Alle Intervalle der naturtonreihe bis zum achten Partialton",
        correctAnswer = 0,
        explanation = "Im mittelalterlichen Konsonanzdenken (Boethius, Johannes de Garlandia, Franco von Koeln) gelten Prim (1:1), Oktave (1:2), Quinte (2:3) und Quarte (3:4) als perfecte Konsonanzen — ihre Proportionen beruhen auf kleinen ganzzahligen Zahlen. Terzen und Sexten waren 'imperfekte Konsonanzen'. Dissonanzen (Sekunden, Septimen) mussten aufgeloest werden.",
        difficulty = 5,
        funFact = "Die Quarte galt im Mittelalter als 'perfekte Konsonanz' (3:4-Proportion). Im mehrstimmigen Satz des 16.-17. Jahrhunderts wurde sie jedoch gegenueber dem Bass zur Dissonanz — ein konzeptueller Wandel, der die Generalbasslehre praegt."
    ),

    // Q27
    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'Modus', 'Tempus' und 'Prolatio' in der Ars-nova-Mensurallehre (14. Jahrhundert)?",
        answerA = "Modus = Verhaltnis Longa:Brevis; Tempus = Verhaltnis Brevis:Semibrevis; Prolatio = Verhaltnis Semibrevis:Minima — jeweils mit Unterteilung in 2 oder 3 Einheiten",
        answerB = "Modus = Melodietyp (Kirchentonart); Tempus = Taktart; Prolatio = Aussierdung der Noten durch Rhythmuspunkte",
        answerC = "Modus = Anzahl der Stimmen; Tempus = Grundtempo; Prolatio = Wiederholungsstruktur einer Form",
        answerD = "Alle drei Begriffe bezeichnen Aspekte der rythmischen Modi im Notre-Dame-Stil des 12./13. Jahrhunderts",
        correctAnswer = 0,
        explanation = "Die Mensurallehre Philipps de Vitry (Ars nova, ca. 1322) und Johannes de Muris definiert drei hierarchische Ebenen: Modus (Longa/Brevis-Verhaeltnis), Tempus (Brevis/Semibrevis, dreiteilig = tempus perfectum, zweiteilig = imperfectum) und Prolatio (Semibrevis/Minima, ebenfalls perfekt/imperfekt). Vier Kombinationen ergeben die vier Grundtaktarten.",
        difficulty = 5,
        funFact = "Die vier Kombinationen aus Tempus (perfekt/imperfekt) und Prolatio (maior/minor) entsprechen modern: 9/8, 3/4, 6/8 und 2/4 — das rhythmische Denken des 14. Jahrhunderts ist erstaunlich nah an unserer heutigen Taktsystem-Logik."
    ),

    // Q28
    Question(
        categoryId = 5,
        questionText = "Was versteht Boethius in 'De institutione musica' (ca. 500 n. Chr.) unter 'Musica mundana'?",
        answerA = "Die Musik der Sphaeren — die harmonisch-mathematische Ordnung der Planetenbewegungen, die zwar fuer das menschliche Ohr unhoorbar ist, aber das Universum in Proportionen ordnet",
        answerB = "Weltliche Musik im Gegensatz zur Kirchenmusik — Volkslieder und Tanzmusik",
        answerC = "Die Musik, die von Handwerkern und Kaufleuten als Unterhaltung gespielt wurde",
        answerD = "Die Musica humana in ihrem Bezug zur aussereuropaeischen Welt (Orientmusik)",
        correctAnswer = 0,
        explanation = "Boethius teilt Musik in drei Arten: Musica mundana (Sphaerenharmonie der Planeten und Jahreszeiten), Musica humana (Harmonie von Koerper und Seele) und Musica instrumentalis (klingende Musik durch Instrumente). Nur die letzte ist hoerbar; die ersten beiden sind mathematische Realitaeten jenseits des Gehoers.",
        difficulty = 5,
        funFact = "Boethius' Dreiteilung pragte das europaeische Musikdenken bis ins 17. Jahrhundert. Kepler griff die Sphaerenharmonie in 'Harmonices Mundi' (1619) auf und versuchte, Planetengeschwindigkeiten als Intervallverhaeltnisse zu deuten."
    ),

    // Q29
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Organum purum' in der Notre-Dame-Polyphonie des 12./13. Jahrhunderts?",
        answerA = "Ein Abschnitt im mehrstimmigen Organum, in dem die Oberstimme(n) in freiem, gedehntem Rhythmus ueber einem sehr langen, ausgehaltenen Cantus-Firmus-Ton singen",
        answerB = "Ein Organum, das ausschliesslich auf der reinen Pfeifenorgel ohne Begleitung gespielt wird",
        answerC = "Die urspruengliche parallele Organum-Form (Quart-/Quintparallelen) des fruehen Mittelalters",
        answerD = "Eine notierte Organum-Form ohne Musica-ficta-Alterationen (rein nach Notation)",
        correctAnswer = 0,
        explanation = "Organum purum (auch Organum duplum) bezeichnet Abschnitte in den grossen Notre-Dame-Organa (Leonin, Perotin), in denen der Tenor einen Gregorianischen Ton extrem lange haelt, waehrend die Oberstimme(n) in elaborierter, florid-melismatischer Weise darueber agieren. Im Gegensatz dazu stehen die 'Discantus'-Abschnitte mit gemessenem Rhythmus beider Stimmen.",
        difficulty = 5,
        funFact = "Leonins 'Magnus Liber Organi' (ca. 1170-1180) enthaelt Organa fuer das gesamte Kirchenjahr. Die extremen Laengen der Tenor-Toene (manchmal mehrere Minuten fuer einen einzigen Ton) machen eine chorische Besetzung technisch notwendig."
    ),

    // Q30
    Question(
        categoryId = 5,
        questionText = "Was ist ein 'Isorhythmischer Motette' und welches rhythmische Strukturprinzip liegt ihr zugrunde?",
        answerA = "Eine Motette, in der die Tenorstimme ein unveraenderlich wiederholtes rhythmisches Muster (Talea) mit einer unveraenderlich wiederholten Tonfolge (Color) kombiniert, die unterschiedliche Laengen haben koennen",
        answerB = "Eine Motette, in der alle Stimmen identische Rhythmen singen (Homorhythmik)",
        answerC = "Eine Motette, die auf einem isomelischen Tenor beruht (gleiche Melodie in allen Talea-Wiederholungen)",
        answerD = "Eine Motette, bei der das Tempus sich durch Addition verlaengert (isorhythmische Augmentation)",
        correctAnswer = 0,
        explanation = "In der isorhythmischen Motette (Ars nova, 14.-15. Jh.) wird der Tenor durch zwei unabhaengige Zyklen strukturiert: Die Talea (rhythmisches Muster) und der Color (Tonreihe). Weil Talea und Color meist verschieden lang sind, entstehen immer neue Melodie-Rhythmus-Kombinationen bei jeder Wiederholung. Philippe de Vitry und Guillaume de Machaut sind Hauptmeister dieser Technik.",
        difficulty = 5,
        funFact = "In Machauts 'Messe de Nostre Dame' (ca. 1365), der ersten vollstaendig erhaltenen mehrstimmigen Messvertonung eines Komponisten, finden sich isorhythmische Strukturen im Gloria und Credo — ein ausgepraegte Architektur-Denken in der Musik."
    ),

    // ── Mikrotonale Musik (6) ────────────────────────────────────────────────

    // Q31
    Question(
        categoryId = 5,
        questionText = "Welche Stimmung verwendete Harry Partch in seinen Kompositionen und wie viele Toene pro Oktave hatte sein System?",
        answerA = "Just Intonation mit 43 Toenen pro Oktave, abgeleitet aus Oberton-Proportionen bis zum 11-Limit",
        answerB = "43-stufige gleich temperierte Skala (43-TET) nach dem Vorbild arabischer Maqam-Systeme",
        answerC = "Just Intonation mit 31 Toenen pro Oktave nach der Theorie Christiaan Huygens'",
        answerD = "19-stufige gleich temperierte Stimmung (19-TET), die er selbst mathematisch entwickelte",
        correctAnswer = 0,
        explanation = "Harry Partch (1901-1974) entwickelte ein Just-Intonation-System mit 43 Toenen pro Oktave, basierend auf Oberton-Proportionen bis zum 11-limit (Primzahlen bis 11). Er baute alle Instrumente selbst (Chromelodeon, Spoils of War, Quadrangularis Reversum etc.), da keine Standardinstrumente seine Stimmung spielen konnten.",
        difficulty = 5,
        funFact = "Partch nahm kaum Aufnahmen auf und lebte lange verarmt. Sein Hauptwerk 'Delusion of the Fury' (1966) fuer grosse Ensembles seiner Eigenbauten ist eine der radikalsten Gesamtkunstwerke des 20. Jahrhunderts — Theater, Musik und visuelle Kunst in einem."
    ),

    // Q32
    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'EDO' (Equal Divisions of the Octave) und 'JI' (Just Intonation) als mikrotonale Systeme?",
        answerA = "EDO teilt die Oktave in N gleiche logarithmische Schritte (z.B. 31-EDO, 72-EDO); JI definiert Toene durch ganzzahlige Frequenzverhaeltnisse (reine Oberton-Proportionen) ohne Temperierung",
        answerB = "EDO ist ein historisches System des 16. Jahrhunderts; JI ist eine moderne Computersimulation purer Stimmung",
        answerC = "EDO bezeichnet europaeische Systeme; JI indische und arabische Mikrotonalsysteme",
        answerD = "EDO und JI bezeichnen dasselbe — die Abkuerzungen stammen aus verschiedenen nationalen Theorietraditionen",
        correctAnswer = 0,
        explanation = "EDO: Logarithmisch gleiche Schritte, z.B. 12-EDO = gleichstufige Stimmung, 31-EDO (Huygens), 72-EDO (Maneri/Sims). JI: Toene als Bruchzahlen (3/2 = reine Quinte, 5/4 = reine grosse Terz). EDO-Systeme haben Strukturvorteile (Transposierbarkeit); JI hat maximale harmonische Reinheit. Viele Mikrotonal-Komponisten mischen oder waehlen je nach aesthetischen Zielen.",
        difficulty = 5,
        funFact = "31-EDO approximiert viele reine JI-Intervalle sehr gut und erlaubt Transposition — Christiaan Huygens (17. Jh.) und spaeter Adriaan Fokker (20. Jh.) bauten Orgeln in dieser Stimmung. Das Fokker-Organ in Amsterdam ist noch spielbar."
    ),

    // Q33
    Question(
        categoryId = 5,
        questionText = "Welcher Komponist entwickelte die 'Spectral'-Kompositionsmethode und nutzte dabei Mikrotoene als direkte Ableitung aus dem Oberton-Spektrum?",
        answerA = "Tristan Murail und Gerard Grisey (Ensemble L'Itineraire), die ab den 1970er Jahren Spektralanalysen in Partituren umsetzten und dabei Viertel- und Achteltoenee zur genauen Darstellung von Oberton-Inharmonizitaeten einsetzten",
        answerB = "Giacinto Scelsi, der Mikrotoene durch intensive meditatve Improvisation auf Einzeltoenen entdeckte",
        answerC = "Alois Haba, der ab 1927 als erster systematisch Vierteltoen-Opern komponierte",
        answerD = "Ben Johnston, der Partials bis zum 31-Limit in Just-Intonation-Streichquartetten darstellte",
        correctAnswer = 0,
        explanation = "Die Spektralisten (Murail, Grisey) leiteten Mikrotoene wissenschaftlich aus Fourier-Analysen realer Klaenge ab. Obertone realer Instrumente sind oft leicht inharmonisch; ihre genaue Darstellung in der Partitur erfordert Viertel- oder Achteltoen-Notation. 'Partiels' (Grisey, 1975) beginnt mit dem Spektrum eines Blechblaesers als kompositorischem Rohstoff.",
        difficulty = 5,
        funFact = "Murails 'Gondwana' (1980) fuer Orchester benutzt Computerberechnungen des Glockenspektrums als harmonische Basis. Die Partitur ist durch und durch aus einem einzigen Klangspektrum abgeleitet — ein radikaler Materialismus in der Komposition."
    ),

    // Q34
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Srutis'-System in der indischen Musiktheorie und wie viele Mikrotoen-Einheiten umfasst eine Oktave?",
        answerA = "22 Sruti pro Oktave — die kleinsten theoretisch unterschiedenen Tonschritte, aus denen die Ragas ihre charakteristischen Intonationen beziehen",
        answerB = "12 Sruti pro Oktave, identisch mit dem westlichen Halbton-System",
        answerC = "53 Sruti pro Oktave nach dem System des persischen Musiktheoretikers Safi al-Din",
        answerD = "72 Sruti pro Oktave, entsprechend dem Melakarta-System der karnatischen Musik",
        correctAnswer = 0,
        explanation = "Das klassische indische Sruti-System zaehlt 22 Mikroschritte pro Oktave, die jedoch keine gleich grossen Intervalle sind. Sie dienen als theoretische Bezugsgroessen fuer die Intonation der Ragas. Die genaue Definition der 22 Sruti wurde im Natyashastra (ca. 200 v. Chr. - 200 n. Chr.) beschrieben und spaeter von Bhatkhande und Vishnu Digambar Paluskar systematisiert.",
        difficulty = 5,
        funFact = "Die 22 Sruti teilen sich auf die sieben Grundstufen der Skala ungleich auf: Einige Stufen haben 4 Sruti 'Spielraum', andere nur 2 oder 3. Diese Ungleichmaessigkeit erzeugt die charakteristische Intonation verschiedener Ragas."
    ),

    // Q35
    Question(
        categoryId = 5,
        questionText = "Welcher tschechische Komponist schrieb als erster eine Oper in Vierteltoenen ('Die Mutter', 1929) und entwickelte eine systematische Viertelton-Musiktheorie?",
        answerA = "Alois Haba",
        answerB = "Leos Janacek",
        answerC = "Bohuslav Martinu",
        answerD = "Josef Suk",
        correctAnswer = 0,
        explanation = "Alois Haba (1893-1973) war Pionier der Mikrotonal-Komposition. Seine Oper 'Die Mutter' (uraufgefuehrt 1931 in Muenchen) war die erste vollstaendige Vierteltoen-Oper. Er entwickelte eine systematische Theorie und Notation fuer Vierteltoene, Sechsteltoene und sogar Zwolfteltoenee und unterrichtete an der Prager Konservatoriums ab 1923.",
        difficulty = 5,
        funFact = "Fuer die Urauffuehrung von Habas Viertelton-Oper mussten Spezial-Instrumente gebaut werden: ein Viertelton-Klavier, -Klarinette, -Trompete. Das stellt bis heute logistische Hindernisse dar, die regelmassige Auffuehrungen verhindert."
    ),

    // Q36
    Question(
        categoryId = 5,
        questionText = "Was ist das Besondere an Ben Johnstons Streichquartett Nr. 4 'Amazing Grace' (1973) bezueglich seines Mikrotonal-Systems?",
        answerA = "Es ist in Just Intonation bis zum 7-Limit komponiert: alle Toene sind reine Oberton-Verhaeltnisse, was das bekannte Lied in einer Reinheit erklingen laesst, die mit gleichstufiger Stimmung unerreichbar ist",
        answerB = "Es verwendet 72-EDO und fuehrt das Lied durch alle 72 Stufen der Skala als Transpositionszyklus",
        answerC = "Es verteilt das Lied auf vier Stimmen in vier unterschiedlichen Mikrotonal-Systemen gleichzeitig",
        answerD = "Es ist in 31-EDO nach Fokkers Theorie und verwendet Spezialnotation mit drei Vorzeichen-Systemen",
        correctAnswer = 0,
        explanation = "Ben Johnstons Streichquartett Nr. 4 setzt 'Amazing Grace' in Just Intonation um, wobei alle Akkorde aus reinen Oberton-Proportionen konstruiert sind. Der Effekt ist fuer geoebte Oehren sofort hoerbar: Die Terzen und Sexten 'sitzen' mit einer Reinheit, die in gleich temperierter Stimmung nicht moeglich ist. Das Stueck vertraegt sich gleichzeitig mit Variationstechnik.",
        difficulty = 5,
        funFact = "Johnston entwickelte eine spezielle Erweiterungs-Notation des traditionellen Notensystems fuer JI — mit + und - Zeichen sowie Pfeilen fuer verschiedene Syntonic-Comma-Korrekturen. Sein Notationssystem ist nach wie vor nicht standardisiert."
    ),

    // ── Stimmungssysteme (7) ─────────────────────────────────────────────────

    // Q37
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Pythagoraeische Komma' und welche praktische Bedeutung hat es fuer Stimmungssysteme?",
        answerA = "Die Differenz zwischen zwolf reinen Quinten (3/2)^12 und sieben Oktaven (2^7): ca. 23,46 Cent — das Mass der Stimmungsunschraerfe, die jedes Stimmungssystem verteilen oder unterdruecken muss",
        answerB = "Die Differenz zwischen einer reinen Terz (5/4) und vier pythagoraeischen Ganztoenen",
        answerC = "Der Frequenzunterschied zwischen dem Kammerton a' und dem pythagoraeischen a' bei 432 Hz",
        answerD = "Die Pentatonik-Luecke zwischen dem sechsten und siebten Ton der griechischen Oktavgattungen",
        correctAnswer = 0,
        explanation = "Das Pythagoraeische Komma (~23.46 Cent) entsteht, weil zwolf reine Quinten (3^12/2^19 = 531441/524288) nicht exakt sieben Oktaven ergeben. Jedes Stimmungssystem muss diesen Ueberschuss unterbringen: Meantone-Stimmungen legen ihn in eine 'Wolfsquinte'; Wohltemperierte Stimmungen verteilen ihn auf viele Quinten; Gleichstufige Stimmung verteilt ihn gleichmaessig (jede Quinte = 700 Cent statt 702).",
        difficulty = 5,
        funFact = "Die 'Wolfsquinte' der Mitteltonstimmung ist so dissonant, dass sie wie ein heulender Wolf klang — daher der Name. Sie ist fast zwei Vierteltone von der reinen Quinte entfernt und macht bestimmte Tonarten auf Instrumenten mit Mitteltonstimmung unspielbar."
    ),

    // Q38
    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'Kirnberger III' und 'Werckmeister III' als historische Wohltemperierungen des 18. Jahrhunderts?",
        answerA = "Kirnberger III (1779) haelt bestimmte Quinten rein (3/2) und konzentriert die Temperierung auf wenige Quinten; Werckmeister III (1691) verteilt Temperierung gleichmaessiger auf vier schwebende Quinten, was mehr Tonarten gut spielbar macht",
        answerB = "Kirnberger stimmte nach dem 5-Limit-JI-Prinzip, Werckmeister nach dem pythagoraeischen 3-Limit",
        answerC = "Beide Stimmungen sind identisch, unterscheiden sich nur in den historischen Quellen",
        answerD = "Werckmeister III ist eine zwoelfstufige Gleichstimmung; Kirnberger III eine unregelmaessige Temperierung",
        correctAnswer = 0,
        explanation = "Werckmeister III (1691): Vier Quinten sind um je ein Viertel des Pythagoraeischen Kommas vermindert (C-G-D-A-E); alle anderen rein. Kirnberger III (1779): Haelt die Quinten F-C-G-D-A rein (3/2) und konzentriert die noetige Temperierung in zwei Quinten (D-A und H-Fis). Beide erzeugen ungleiche Charaktere in verschiedenen Tonarten — beabsichtigt fuer den 'Affekt' der Tonarten.",
        difficulty = 5,
        funFact = "Die Frage, ob Bach das Wohltemperierte Klavier in Werckmeister III, Kirnberger III oder einer anderen Stimmung beabsichtigt hat, ist eine der heissumstrittensten in der Aufluehrungspraxis. Andreas Sparschuh und Bradley Lehman haben aus dem Titelseiten-Ornament der WTK eigene Stimmungsvorschlaege abgeleitet."
    ),

    // Q39
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Syntonic Comma' und welche Rolle spielt sie in der Mitteltonstimmung (Mean Tone Temperament)?",
        answerA = "Das Syntonic Comma (81/80, ca. 21,5 Cent) ist die Differenz zwischen einem pythagoraeischen Ganzton (9/8) und einem 'reinen' Ganzton (10/9); in der Mitteltonstimmung wird es durch gleichmaessige Verkleinerung der Quinten kompensiert, um reine Terzen (5/4) zu erhalten",
        answerB = "Das Syntonic Comma bezeichnet die kleine Terz in einem reinen Dur-Akkord (6/5) und ist Grundlage der Mollstimmung",
        answerC = "Das Syntonic Comma ist identisch mit dem Pythagoraeischen Komma, wird aber in der JI-Theorie anders bezeichnet",
        answerD = "Das Syntonic Comma bezeichnet die Stimmungsdifferenz zwischen franzoesischem und deutschem Kammerton im 18. Jahrhundert",
        correctAnswer = 0,
        explanation = "Das Syntonic Comma (81/80 ≈ 21.5 Cent) ist die Differenz zwischen einer rein pythagoraeischen und einer reinen Terz. In der Quarter-Comma-Meantone-Stimmung wird jede Quinte um 1/4 Syntonic Comma verkleinert (statt 702 Cent: 697 Cent). Dies ergibt reine grosse Terzen (386 Cent = 5/4) auf den haupigen Stufen — ideal fuer die reine Polyphonie des 16.-17. Jahrhunderts.",
        difficulty = 5,
        funFact = "Mitteltonstimmung war vom 16.-18. Jahrhundert die Standardstimmung fuer Tasten- und Orgelinstrumente in Westeuropa. Viele Bach-Orgelwerke klingen in Mitteltonstimmung ganz anders — manche Stellen klingen reiner, andere wie beabsichtigte 'Reibungen' entstehen durch die Wolfsquinte."
    ),

    // Q40
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet 'adaptive Just Intonation' als Stimmungskonzept fuer computergesteuerte Instrumente?",
        answerA = "Ein System, in dem jeder Akkord in Echtzeit in die naechste reine Stimmung versetzt wird, indem Einzeltoene mikrotonal angepasst werden, ohne dass das Ohr diskrete Sprunge wahrnimmt",
        answerB = "Die automatische Transpositionsfunktion eines Synthesizers, der sich an den Saenger anpasst",
        answerC = "Ein MIDI-System, das JI-Stimmungen aus einer Datenbank laedt und auf Klaviertasten mappt",
        answerD = "Eine Methode, mit der Sinfonieorchester JI durch gegenseitiges Einhoeren erzeugen, ohne Dirigent",
        correctAnswer = 0,
        explanation = "Adaptive JI (z.B. nach Marc Sabat/Wolfgang von Schweinitz, oder im Tonescape-Software-Projekt) erlaubt jedem harmonischen Kontext reine Intervalle, indem Einzeltoene des laufenden Akkords mikrotonal korrigiert werden. Uebergangsmomente zwischen Akkorden werden so interpoliert, dass horizontale Tonhoehenspruenge minimal bleiben. Ermoeglicht auf Synthesizern harmonisch reines Spiel in allen Tonarten.",
        difficulty = 5,
        funFact = "Das HEJI (Helmholtz-Ellis Just Intonation)-Notationssystem von Marc Sabat erlaubt die prazise Notation von JI-Toenen bis zum 31-Limit — auf jede Note wird ein Pfeil-Vorzeichen gesetzt, das den genauen Abstand in Cent vom gleichstufigen Referenzton angibt."
    ),

    // Q41
    Question(
        categoryId = 5,
        questionText = "Was ist das Ziel der '432 Hz'-Bewegung und was sagt die Akustikwissenschaft dazu?",
        answerA = "Kammerton a' bei 432 Hz statt 440 Hz soll harmonischer mit Naturgesetzen uebereinstimmen — die Akustikwissenschaft findet hierfuer keine belastbaren empirischen Belege; die Wahl des Kammertons ist historisch-konventionell, nicht physikalisch-notwendig",
        answerB = "432 Hz entspricht der reinen pythagoraeischen Quinte des C-Grundtons bei 256 Hz und war der offizielle Kammerton vor 1939",
        answerC = "432 Hz ist das Ergebnis der natur-reinen Stimmung nach Werckmeister und wurde 1711 international festgelegt",
        answerD = "432 Hz ist die Frequenz, bei der Schall Wasser-Kristallmuster erzeugt, was von Masaru Emoto bewiesen wurde",
        correctAnswer = 0,
        explanation = "Die 432-Hz-Bewegung behauptet metaphysische und kosmische Harmonie bei diesem Kammerton. Wissenschaftlich gibt es keine Grundlage: Der Kammerton ist eine historische Konvention (a' schwankte von ca. 392 bis 465 Hz je nach Zeit und Ort). 1939/1955 wurde 440 Hz international normiert (ISO 16). Masaru Emotos 'Wasserkristall'-Experimente sind wissenschaftlich nicht replizierbar.",
        difficulty = 5,
        funFact = "Historisch spielten Instrumente auf unterschiedlichsten Kammertoenen: Barockmusik oft bei a' = 415 Hz (ein Halbton tiefer als heute), manche Kirchenorgeln sogar bei 466 Hz. Die 440-Hz-Norm ist ein pragmatischer Standard — kein Naturgesetz."
    ),

    // Q42
    Question(
        categoryId = 5,
        questionText = "Was beschreibt das 'Kleisma' als Intervall in der mikrotonalen Stimmungstheorie?",
        answerA = "Das Kleisma (15625/15552 ≈ 8,1 Cent) ist ein winziges JI-Intervall, die Differenz zwischen drei reinen Quarten und einer Oktave plus reiner grosser Sext — relevant fuer 5-Limit-JI-Stimmungen hoeherer Komplexitaet",
        answerB = "Ein arabisches Mikrointervall des Rast-Maqam, kleiner als ein Viertelton",
        answerC = "Die Differenz zwischen Syntonic Comma und Pythagoraeischem Komma",
        answerD = "Das kleinste wahrnehmbare Tonhoehenintervall des menschlichen Gehoers (ca. 3-5 Cent)",
        correctAnswer = 0,
        explanation = "Das Kleisma (15625/15552) ist ein kleines Komma des 5-Limit-JI-Systems. Es entsteht, wenn man sechs reine kleine Terzen stapelt minus eine reine Quinte. In Stimmungssystemen, die das Kleisma ignorieren (temperierten), entstehen einfachere Strukturen. Relevanz in der modernen JI-Theorie und bei der Untersuchung mikrotonaler Skalen.",
        difficulty = 5,
        funFact = "Die Nomenklatur der Mikrointervalle (Komma, Schisma, Kleisma, Diaschisma) geht auf den grossen Akustiker und Stimmungstheoretiker Hermann von Helmholtz zurueck, der in 'Die Lehre von den Tonempfindungen' (1863) das System zusammenfassend darstellte."
    ),

    // Q43
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Schisma' in der Stimmungstheorie und wie gross ist es?",
        answerA = "Das Schisma (32805/32768 ≈ 1,95 Cent) ist die Differenz zwischen einem pythagoraeischen Komma und einem Syntonic Comma — das kleinste praktisch relevante Komma des 5-Limit-JI",
        answerB = "Das Schisma ist die Differenz zwischen einer reinen und einer gleichstufigen Oktave (2 Cent)",
        answerC = "Das Schisma bezeichnet den Quintenzirkel-Fehler in der pythagoraeischen Stimmung beim Schliessen des Zirkels",
        answerD = "Das Schisma ist das Intervall zwischen dem letzten Ton des griechischen Tetrachords und dem ersten Ton des naechsten",
        correctAnswer = 0,
        explanation = "Das Schisma (32805/32768 ≈ 1.95 Cent) ist die Differenz aus Pythagoraeischem Komma (531441/524288) und Syntonic Comma (81/80). Es ist so klein, dass es oft als vernachlaessigbar gilt. In Stimmungssystemen, die das Schisma temperierten (Schisma-Temperierung), entstehen fast reine Terzen bei weniger Quintabweichung.",
        difficulty = 5,
        funFact = "In der Schisma-Temperierung (z.B. nach Helmholtz) weichen Quinten nur um ca. 2 Cent von der reinen Quinte ab — kaum hoorbar. Dadurch koennen fast alle Terzen nahezu rein sein, ohne die Hoorbarkeits-Probleme der Mitteltonstimmung."
    ),

    // ── Algorithmische / KI-Komposition (7) ─────────────────────────────────

    // Q44
    Question(
        categoryId = 5,
        questionText = "Was war 'Illiac Suite' (1957) von Lejaren Hiller und Leonard Isaacson und warum ist sie historisch bedeutsam?",
        answerA = "Das erste Stueck, das vollstaendig von einem Computer (ILLIAC-Rechner) nach algorithmischen Regeln komponiert wurde — ein Streichquartett in vier Saetzen mit zunehmend komplexen Regelsystemen",
        answerB = "Die erste computergesteuerte Orchesterpartitur, die von einem Dirigier-Roboter aufgefuehrt wurde",
        answerC = "Ein elektronisches Stueck, das die ILLIAC-Rechnerprozesse selbst als Klangmaterial verwendete",
        answerD = "Die erste KI-Komposition im Pop-Bereich, die einen Chartplatz erreichte",
        correctAnswer = 0,
        explanation = "Lejaren Hiller und Leonard Isaacson programmierten den ILLIAC-Computer der University of Illinois so, dass er nach definierten Kompositionsregeln (Kontrapunktregeln, stochastische Verfahren) ein Streichquartett erzeugte. Die 'Illiac Suite' (1957) gilt als erstes vollstaendig computerkomponiertes Musikstueck — Beginn der algorithmischen Komposition.",
        difficulty = 5,
        funFact = "ILLIAC (Illinois Automatic Computer) war ein Vakuumroehren-Rechner. Die Komposition dauerte Monate Rechenzeit; die Ausgabe war eine Lochkarten-Partitur, die danach manuell in spielbare Notation uebertragen wurde."
    ),

    // Q45
    Question(
        categoryId = 5,
        questionText = "Was ist 'CHANT' (CHanTeur ANThropomorphe) und welche Bedeutung hatte es fuer die computergesteuerte Klangsynthese?",
        answerA = "Ein Vokal-Syntheseprogramm des IRCAM (1981), das erstmals physikalische Modellierung des menschlichen Stimmtrakts fuer realistische Gesangssynthese nutzte",
        answerB = "Ein Notationsprogramm fuer gregorianische Choralmelodien der Abtei Solesmes",
        answerC = "Ein KI-System, das aus MIDI-Eingaben automatisch gregorianische Harmonisierungen erstellte",
        answerD = "Das erste kommerzielle Musikkompositionsprogramm fuer Apple Macintosh (1984)",
        correctAnswer = 0,
        explanation = "CHANT (Xavier Rodet, IRCAM, 1979/1981) war bahnbrechend: Es nutzte physikalische Modellierung (Formantsynthese) des Vokaltrakts zur realistischen Stimmsynthese. Zum ersten Mal klangen computergenerierte Stimmen annaehernd menschlich. Xavier Rodet verwendete es in 'Chant/Formes' (1984), einem der ersten vollstaendig computeranimierten Musikstuecke.",
        difficulty = 5,
        funFact = "IRCAM (Institut de Recherche et Coordination Acoustique/Musique) in Paris, gegruendet von Pierre Boulez 1977, ist bis heute das einflussreichste Forschungszentrum fuer computergestuetzte Musikkomposition und -synthese weltweit."
    ),

    // Q46
    Question(
        categoryId = 5,
        questionText = "Was ist der 'Markov-Ketten'-Ansatz in der algorithmischen Komposition und welcher Komponist nutzte ihn als erster systematisch?",
        answerA = "Markov-Ketten definieren Uebergangswahrscheinlichkeiten zwischen Zustaenden (z.B. Toenen); Iannis Xenakis nutzte wahrscheinlichkeitstheoretische Modelle als erster systematisch, spaeter formalisiert durch Hiller fuer Computerprogramme",
        answerB = "Markov-Ketten erzeugen periodische Sequenzen nach festem Muster; Steve Reich entwickelte damit die Minimal Music",
        answerC = "Markov-Ketten sind Regelwerke fuer serielle Komposition, erstmals von Stockhausen angewandt",
        answerD = "Markov-Ketten bezeichnen in der Musik die Verkettung von Motiven ohne Wiederholung — zuerst systematisch von Schoenberg genutzt",
        correctAnswer = 0,
        explanation = "Markov-Ketten (nach Andrei Markov) definieren probabilistische Zustandsuebergaenge: P(n|n-1) = Wahrscheinlichkeit des naechsten Tons gegeben den vorherigen. In der Musik erzeugen sie statistisch 'stilaehnliche' Musik, wenn sie aus einem Corpus gelernt wurden. Xenakis nutzte Markov-Prozesse in 'Analogique A' (1958); moderne ML-Systeme (GPT-Music, Magenta) sind hochdimensionale Verallgemeinerungen.",
        difficulty = 5,
        funFact = "David Cope programmierte ab 1981 'Experiments in Musical Intelligence' (EMI), ein regelbasiertes System, das im Stil von Bach, Beethoven und Chopin komponierte. Seine Stilkopien waren so ueberzeugend, dass sie Musikforscher taeuschen konnten — bis heute ethisch und kunstphilosophisch kontrovers."
    ),

    // Q47
    Question(
        categoryId = 5,
        questionText = "Was ist 'Max/MSP' und welche Bedeutung hat es fuer die Live-Elektronik-Komposition seit den 1980er Jahren?",
        answerA = "Eine visuelle Programmierumgebung fuer Echtzeit-Audioverarbeitung (Miller Puckette, IRCAM, 1985), in der Komponisten grafisch Klangobjekte verschalten koennen ohne textbasiertes Programmieren",
        answerB = "Ein Notationssoftware fuer mikrotonale Partituren, entwickelt am MIT Media Lab",
        answerC = "Ein digitales Mischpultprogramm fuer Live-Konzert-Beschallung, das MIDI-gesteuert ist",
        answerD = "Ein KI-Kompositionsprogramm, das grosse Mengen Notentexte analysiert und neue Werke erzeugt",
        correctAnswer = 0,
        explanation = "Max (Miller Puckette, IRCAM 1985, spaeter Cycling'74 als 'Max/MSP') ist eine grafische Datenstrom-Programmiersprache fuer Echtzeit-Audioverarbeitung. Komponisten verbinden 'Objekte' durch 'Patches' grafisch. Es ermoeglicht Live-Elektronik ohne tiefe Programmierkenntnisse und ist Standard in der zeitgenoessischen Elektroakustik (Pierre Boulez, George Benjamin, Kaija Saariaho nutzten es alle).",
        difficulty = 5,
        funFact = "Miller Puckette veroeffentlichte spaeter auch 'Pure Data' (Pd) als freie Open-Source-Alternative zu Max. Beide Systeme haben riesige Communities und werden in Kunst, Forschung und Lehre eingesetzt — Pd laeuft sogar auf Raspberry Pi."
    ),

    // Q48
    Question(
        categoryId = 5,
        questionText = "Was ist 'OpenAI Jukebox' und wie unterscheidet es sich von frueheren KI-Musiksystemen?",
        answerA = "Ein neuronales Netz (2020), das Rohaudiosignale im Zeitbereich generiert — nicht Noten oder MIDI — und damit Gesang mit Lyrics in spezifischen Stilkopien erzeugen kann (z.B. Elvis, Beatles-Stil)",
        answerB = "Ein MIDI-generierendes System, das nach Trainingsdaten im symbolischen Notenformat Kompositionen erzeugt",
        answerC = "Ein Streaming-Service, der KI-generierte Musik ohne Urheberrecht fuer Werbezwecke bereitstellt",
        answerD = "Ein Kompositions-Assistent fuer Filmmusik, der aus Storyboard-Beschreibungen Partituren erzeugt",
        correctAnswer = 0,
        explanation = "OpenAI Jukebox (2020) nutzt hierarchische VQ-VAE-Modelle, um Rohaudiosignale direkt zu erzeugen. Davor arbeiteten KI-Systeme meist auf MIDI/symbolischer Ebene (Magenta, MuseNet). Jukebox kann Gesangsmelodien mit Lyrics und Instrumentierung in einem Stil generieren — ein qualitativer Sprung, auch wenn die Qualitaet noch begrenzt ist.",
        difficulty = 5,
        funFact = "Jukebox wurde 2020 veroeffentlicht, ist aber rechenintensiv: Eine Minute Musik dauerte Stunden Generierungszeit. Die ethischen Fragen ueber Stilkopien und Urheberrecht wurden sofort intensiv diskutiert — besonders da das System Elvis- oder Beatles-Stil aus echten Aufnahmen gelernt hatte."
    ),

    // Q49
    Question(
        categoryId = 5,
        questionText = "Was ist das 'Continuator'-System von Francois Pachet (Sony CSL, 2002) und welches KI-Konzept liegt ihm zugrunde?",
        answerA = "Ein Echtzeit-Begleitsystem, das den Stil eines Musikers durch Markov-Modelle lernt und in dessen Stil improvisiert — mit dem Ziel, als Co-Improvisationspartner wahrgenommen zu werden",
        answerB = "Ein automatisches Transkriptionssystem, das Jazzimprovisation in Noten umwandelt",
        answerC = "Ein generatives System, das Noten aus Liedtexten erzeugt durch NLP-Verarbeitung",
        answerD = "Ein Synthesizer, der akustische Instrumente durch KI-Modellierung imitiert",
        correctAnswer = 0,
        explanation = "Der Continuator (Pachet, 2002) lernt in Echtzeit den Stil des spielenden Musikers durch Markov-Modelle hoeherer Ordnung und improvisiert dann im selben Stil als interaktiver Partner. Es war ein Meilenstein der 'interactive machine listening'-Forschung und wurde von Jazzpianisten wie Herbie Hancock getestet. Ziel war genuine stilistische Kohaeenz, nicht bloss Begleitung.",
        difficulty = 5,
        funFact = "Pachet berichtete, dass professionelle Musiker beim Hoeren des Continuators oft nicht sofort erkannten, welche Licks sie selbst gespielt hatten — das System war stilimitatorisch so praezise, dass es die Musiker mit sich selbst verwechseln liess."
    ),

    // Q50
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Corpus-Based Concatenative Synthesis' (CBCS) in der computergestuetzten Klangsynthese?",
        answerA = "Eine Synthesetechnik, in der ein grosses Klangkorpus in kleine Einheiten (Grains) segmentiert wird und Zielpegel durch Auswahl und Verkettung passender Segmente aus dem Korpus erzeugt werden",
        answerB = "Eine Methode, bei der ein Korpus von Noten durch Markov-Ketten in Echtzeit zu Melodien kombiniert wird",
        answerC = "Die statistische Analyse grosser Musikdatenmengen, um Stilmuster zu extrahieren und als Kompositionsregeln zu formalisieren",
        answerD = "Ein Sample-Recycling-Verfahren, bei dem alte Aufnahmen neu abgemischt werden",
        correctAnswer = 0,
        explanation = "CBCS (Schwarz, Diemo u.a.) segmentiert einen Klangkorpus in Frames/Grains und beschreibt jeden Frame durch Merkmalsvektoren (Spektral-Schwerpunkt, Pitch, Lautstaerke etc.). Zur Synthese wird aus dem Korpus die Sequenz von Frames ausgewaehlt, die einem Zielsignal oder einer Beschreibung am besten entspricht, und verkettet. Wichtiges Werkzeug in der zeitgenoessischen elektroakustischen Musik.",
        difficulty = 5,
        funFact = "Das 'CataRT'-System (Diemo Schwarz, IRCAM) macht CBCS interaktiv: Musiker navigieren in einem 2D-Plot des Korpus und 'spielen' Sounds durch Auswahl von Korpus-Regionen. Es klingt wie ein Instrument, besteht aber aus real aufgenommenen Fragmenten."
    ),
)
