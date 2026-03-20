package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsHard3(): List<Question> = listOf(

    // --- MATHEMATIK-GESCHICHTE: BERÜHMTE MATHEMATIKER (10) ---

    Question(
        categoryId = 11,
        questionText = "Welches Werk von Euklid, verfasst um 300 v. Chr., gilt als das einflussreichste mathematische Lehrbuch der Geschichte?",
        answerA = "Arithmetik",
        answerB = "Die Elemente",
        answerC = "Almagest",
        answerD = "Conics",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Euklids 'Elemente' (13 Bücher) stellten das mathematische Wissen der Antike systematisch zusammen. Nach der Bibel gilt es als das meistgedruckte Buch der Geschichte und wurde über 2000 Jahre lang als Schulbuch genutzt.",
        funFact = "Die Elemente wurden 1482 in Venedig als eines der ersten mathematischen Bücher überhaupt gedruckt — und galten bis ins 19. Jahrhundert als Pflichtlektüre an Universitäten."
    ),

    Question(
        categoryId = 11,
        questionText = "Carl Friedrich Gauß bewies 1796 als 18-Jähriger, dass ein regelmäßiges n-Eck mit Zirkel und Lineal konstruierbar ist, wenn n eine bestimmte Bedingung erfüllt. Welches n-Eck war seine historische Entdeckung?",
        answerA = "Regelmäßiges 11-Eck",
        answerB = "Regelmäßiges 13-Eck",
        answerC = "Regelmäßiges 17-Eck",
        answerD = "Regelmäßiges 19-Eck",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Gauß bewies, dass das reguläre 17-Eck (Heptadekagon) mit Zirkel und Lineal konstruierbar ist — die erste neue Konstruktion eines regulären Vielecks seit der Antike. Dieser Fund bewog ihn, Mathematik statt Philologie zu studieren.",
        funFact = "Gauß war von dieser Entdeckung so begeistert, dass er verfügte, ein 17-Eck solle auf seinem Grabstein abgebildet werden. Der Steinmetz lehnte jedoch ab, da es zu ähnlich wie ein Kreis aussähe."
    ),

    Question(
        categoryId = 11,
        questionText = "Leonhard Euler löste 1736 das Königsberger Brückenproblem und begründete damit ein neues Mathematikgebiet. Welches?",
        answerA = "Topologie",
        answerB = "Graphentheorie",
        answerC = "Kombinatorik",
        answerD = "Mengenlehre",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Euler bewies, dass es unmöglich ist, alle 7 Brücken Königsbergs genau einmal zu überqueren. Dabei abstrahierte er die Stadt als Graph aus Knoten und Kanten — dies gilt als Geburtsstunde der Graphentheorie.",
        funFact = "Euler war zeitlebens unglaublich produktiv: Er verfasste über 800 Werke. Als er 1766 erblindete, steigerte sich seine wissenschaftliche Produktion sogar noch — er diktierte seine Ergebnisse einfach."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer bewies im Jahr 1994/1995 nach über 350 Jahren Fermats letzten Satz, dass xⁿ + yⁿ = zⁿ für ganzzahlige x, y, z und n > 2 keine Lösung hat?",
        answerA = "Grigori Perelman",
        answerB = "Andrew Wiles",
        answerC = "Terence Tao",
        answerD = "John Conway",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Andrew Wiles (Universität Princeton) bewies Fermats letzten Satz nach 7 Jahren geheimer Arbeit. Der Beweis umfasst über 100 Seiten und verbindet Elliptische Kurven mit der Shimura-Taniyama-Vermutung.",
        funFact = "Als Wiles 1993 seinen Beweis vorstellte, hatte er einen Fehler drin. Er verbrachte ein weiteres Jahr damit, ihn heimlich zu reparieren — und schaffte es. Er gilt als einer der dramatischsten Momente der Mathematikgeschichte."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Mathematiker des 17. Jahrhunderts war eigentlich Jurist von Beruf und gilt dennoch als Mitbegründer der Wahrscheinlichkeitstheorie und der analytischen Geometrie?",
        answerA = "Blaise Pascal",
        answerB = "René Descartes",
        answerC = "Pierre de Fermat",
        answerD = "John Wallis",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Pierre de Fermat (1607–1665) war hauptberuflich Rat am Parlament von Toulouse. Er betrieb Mathematik als Hobby und kommunizierte seine Ergebnisse meist per Brief — oft ohne Beweis, was Generationen von Mathematikern beschäftigte.",
        funFact = "Fermat schrieb seinen berühmten 'letzten Satz' an den Rand eines Buches mit dem Kommentar: 'Ich habe einen wahrhaft wunderbaren Beweis, aber dieser Rand ist zu schmal, um ihn zu fassen.' Ob er wirklich einen hatte, bleibt offen."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Mathematikerin gilt als erste Programmiererin der Geschichte und schrieb 1843 Algorithmen für Charles Babbages Analytical Engine?",
        answerA = "Emmy Noether",
        answerB = "Sofja Kowalewskaja",
        answerC = "Ada Lovelace",
        answerD = "Maryam Mirzakhani",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Ada Lovelace (1815–1852), Tochter des Dichters Lord Byron, übersetzte einen Artikel über Babbages Maschine und ergänzte ihn um eigene Anmerkungen — darunter den ersten Algorithmus für eine Maschine. Programmiersprache Ada ist nach ihr benannt.",
        funFact = "Ada Lovelace erkannte das Potential von Babbages Maschine weit über reine Berechnungen hinaus — sie spekulierte, dass sie auch Musik komponieren könnte. Das geschah über 100 Jahre bevor Computer-Musik Realität wurde."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher deutsche Mathematiker und Physiker gilt als 'Fürst der Mathematiker' und vollendete mit 24 Jahren das Werk 'Disquisitiones Arithmeticae'?",
        answerA = "Leonhard Euler",
        answerB = "Gottfried Wilhelm Leibniz",
        answerC = "Carl Friedrich Gauß",
        answerD = "Bernhard Riemann",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Gauß (1777–1855) verfasste die 'Disquisitiones Arithmeticae' (1801) im Alter von 24 Jahren. Das Werk begründete die moderne Zahlentheorie als systematische Wissenschaft und führte unter anderem die modulare Arithmetik ein.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Bernhard Riemann formulierte 1859 eine Hypothese über die Verteilung der Primzahlen, die bis heute unbewiesen ist. Worum geht es bei der Riemann-Hypothese?",
        answerA = "Alle geraden Zahlen > 2 sind Summe zweier Primzahlen",
        answerB = "Die nichttrivialen Nullstellen der Zeta-Funktion liegen auf der Geraden Re(s) = 1/2",
        answerC = "Es gibt unendlich viele Primzahlzwillinge",
        answerD = "Jede natürliche Zahl lässt sich als Produkt von Primzahlen schreiben",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Die Riemann-Hypothese betrifft die Nullstellen der Riemannschen Zeta-Funktion. Sie ist eines der Millennium-Probleme — für ihren Beweis wurde 1 Million Dollar Preisgeld ausgesetzt. Alle bisher berechneten Nullstellen (über 10 Billionen) bestätigen die Hypothese.",
        funFact = "Die NSA soll die größte Ansammlung von Mathematikern weltweit beschäftigen, u.a. weil ein Beweis der Riemann-Hypothese moderne Verschlüsselung brechen könnte."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Mathematikerin erhielt 2014 als erste Frau die Fields-Medaille — die höchste Auszeichnung der Mathematik — für ihre Arbeiten in der Teichmüller-Theorie?",
        answerA = "Karen Uhlenbeck",
        answerB = "Maryam Mirzakhani",
        answerC = "Ingrid Daubechies",
        answerD = "Cathleen Morawetz",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Maryam Mirzakhani (1977–2017) aus dem Iran war die erste und bis heute einzige Frau mit der Fields-Medaille. Sie forschte an der Geometrie und Dynamik von Riemannschen Flächen und starb tragischerweise mit 40 Jahren an Krebs.",
        funFact = "Als Kind wollte Mirzakhani Schriftstellerin werden. Erst als sie von den Olympiade-Erfolgen iranischer Mathematiker hörte, wandte sie sich der Mathematik zu — und gewann selbst zweimal Gold bei der Internationalen Mathematikolympiade."
    ),

    Question(
        categoryId = 11,
        questionText = "Emmy Noether gilt als bedeutendste Mathematikerin des 20. Jahrhunderts. Welches fundamentale Theorem der theoretischen Physik geht auf sie zurück?",
        answerA = "Noethers Theorem: Jede Symmetrie entspricht einer Erhaltungsgröße",
        answerB = "Unschärferelation: Position und Impuls nicht gleichzeitig messbar",
        answerC = "Fermatsches Prinzip der kleinsten Wirkung",
        answerD = "Lagranges Variationsprinzip der Mechanik",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Noethers Theorem (1915) besagt: Jede kontinuierliche Symmetrie eines physikalischen Systems entspricht einer Erhaltungsgröße. Z.B. führt Zeitsymmetrie zur Energieerhaltung, Raumsymmetrie zum Impulserhaltungssatz. Einstein nannte sie 'bedeutendste kreative mathematische Genie'.",
        funFact = "Emmy Noether durfte in Deutschland lange keine ordentliche Professorin werden. Als die Universität Göttingen sie 1919 habilitieren wollte, protestierten Kollegen: 'Was werden unsere Soldaten denken, wenn sie heimkehren und von einer Frau lernen sollen?' Hilbert antwortete: 'Wir sind eine Universität, kein Badehaus!'"
    ),

    // --- MATHEMATIK-GESCHICHTE: ENTDECKUNGEN UND BEWEISE (10) ---

    Question(
        categoryId = 11,
        questionText = "Wer entdeckte unabhängig voneinander die Infinitesimalrechnung (Differenzial- und Integralrechnung) im 17. Jahrhundert?",
        answerA = "Newton und Leibniz",
        answerB = "Euler und Lagrange",
        answerC = "Cauchy und Weierstraß",
        answerD = "Fermat und Pascal",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Isaac Newton (ab 1666) und Gottfried Wilhelm Leibniz (ab 1675) entwickelten unabhängig voneinander die Infinitesimalrechnung. Leibniz' Notation (dx, ∫) setzte sich durch. Der daraus entstandene Prioritätsstreit spaltete die europäische Mathematik für Jahrzehnte.",
        funFact = "Der Streit zwischen Newton und Leibniz eskalierte so sehr, dass Newton als Präsident der Royal Society eine Untersuchungskommission einsetzte — und selbst heimlich das Urteil schrieb, das Leibniz des Plagiats beschuldigte."
    ),

    Question(
        categoryId = 11,
        questionText = "In welchem Jahr veröffentlichte Nikolai Lobatschewski die erste nichteuklidische Geometrie?",
        answerA = "1792",
        answerB = "1830",
        answerC = "1854",
        answerD = "1871",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Lobatschewski veröffentlichte 1830 seine hyperbolische Geometrie, in der die Parallelen-Axiom des Euklid nicht gilt. Gleichzeitig und unabhängig entwickelte János Bolyai dasselbe System. Dies revolutionierte das Verständnis von Geometrie als nicht zwingend euklidisch.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welche mathematische Vermutung, formuliert 1742, besagt, dass jede gerade Zahl größer als 2 als Summe zweier Primzahlen darstellbar ist?",
        answerA = "Fermats Vermutung",
        answerB = "Riemanns Hypothese",
        answerC = "Goldbachsche Vermutung",
        answerD = "Legendres Vermutung",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Christian Goldbach schrieb diese Vermutung 1742 in einem Brief an Leonhard Euler. Sie ist bis heute unbewiesen und gilt als eines der ältesten ungelösten Probleme der Mathematik. Per Computer wurde sie für alle Zahlen bis 4 × 10¹⁸ verifiziert.",
        funFact = "Der britische Verlag Faber & Faber bot 2000 eine Million Dollar für einen Beweis der Goldbachschen Vermutung — aber nur für zwei Jahre Laufzeit. Kein Beweis wurde gefunden."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Paradoxon bewies Bertrand Russell 1901, das die naive Mengenlehre von Georg Cantor erschütterte?",
        answerA = "Die Menge aller Mengen, die sich nicht selbst enthalten, führt zum Widerspruch",
        answerB = "Es gibt mehr reelle als rationale Zahlen",
        answerC = "Unendliche Summen können endliche Werte haben",
        answerD = "Nicht jede Aussage der Arithmetik ist beweisbar",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Russells Paradoxon: Sei R die Menge aller Mengen, die sich nicht selbst enthalten. Enthält R sich selbst? Falls ja, darf es das nicht. Falls nein, muss es das. Dies zerstörte die naive Mengenlehre und führte zur axiomatischen Mengenlehre (Zermelo-Fraenkel).",
        funFact = "Als Gottlob Frege gerade sein Hauptwerk 'Grundgesetze der Arithmetik' zum Druck fertiggestellt hatte, erhielt er Russells Brief mit dem Paradoxon. Er schrieb im Nachwort: 'Einem wissenschaftlichen Schriftsteller kann kaum etwas Unerwünschteres begegnen, als dass ihm nach Vollendung einer Arbeit eine der Grundlagen seines Baues erschüttert wird.'"
    ),

    Question(
        categoryId = 11,
        questionText = "Kurt Gödel bewies 1931 zwei Unvollständigkeitssätze. Was besagt der erste?",
        answerA = "Jedes widerspruchsfreie System enthält unbeweisbare wahre Aussagen",
        answerB = "Die Widerspruchsfreiheit eines Systems kann innerhalb des Systems bewiesen werden",
        answerC = "Jede mathematische Aussage ist entweder wahr oder falsch",
        answerD = "Primzahlen lassen sich nicht algorithmisch aufzählen",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Gödels erster Unvollständigkeitssatz: In jedem hinreichend mächtigen, konsistenten formalen System gibt es Aussagen, die wahr, aber nicht beweisbar sind. Dies zertrümmerte Hilberts Programm, die gesamte Mathematik vollständig zu formalisieren.",
        funFact = "Gödel wanderte 1940 aus Wien in die USA aus. Im Einbürgerungsinterview sagte er seinem Freund Einstein gegenüber, er habe einen logischen Fehler in der US-Verfassung entdeckt, der es ermögliche, die Demokratie legal in eine Diktatur umzuwandeln. Einstein unterbrach ihn eilig, bevor er es dem Richter sagen konnte."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches mathematische Instrument, um 1600 von John Napier erfunden, revolutionierte astronomische Berechnungen, indem es Multiplikationen in Additionen verwandelte?",
        answerA = "Pascalsche Rechenmaschine",
        answerB = "Logarithmen",
        answerC = "Rechenschieber",
        answerD = "Arabische Ziffern",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "John Napier (Schottland) veröffentlichte 1614 seine Logarithmentafeln. Da log(a·b) = log(a) + log(b), konnte man aufwändige Multiplikationen durch einfaches Addieren von Tabellenwerten ersetzen. Dies beschleunigte astronomische Berechnungen enorm.",
        funFact = "Kepler nutzte Napiers Logarithmen intensiv für seine Planetenberechnungen. Er schrieb, die Logarithmen hätten ihm 'viele Lebensjahre gespart'."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche mathematische Disziplin begründeten Pascal und Fermat 1654 durch ihren Briefwechsel über das 'Problem des Punktes'?",
        answerA = "Kombinatorik",
        answerB = "Wahrscheinlichkeitsrechnung",
        answerC = "Infinitesimalrechnung",
        answerD = "Algebraische Geometrie",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Das 'Problème des points' fragte: Wie soll der Einsatz aufgeteilt werden, wenn ein Würfelspiel unbeendet abbricht? Pascals und Fermats Lösungen legten die Grundlagen der modernen Wahrscheinlichkeitstheorie.",
        funFact = "Das Problem wurde von einem Glücksspieler namens Chevalier de Méré an Blaise Pascal herangetragen, weil er mathematisch verstehen wollte, warum bestimmte Wetten trotz günstiger Odds langfristig verloren gingen."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer beschrieb im 3. Jahrhundert v. Chr. erstmals einen Algorithmus zur Bestimmung des größten gemeinsamen Teilers zweier Zahlen — den ältesten noch in Gebrauch befindlichen Algorithmus?",
        answerA = "Archimedes",
        answerB = "Pythagoras",
        answerC = "Euklid",
        answerD = "Eratosthenes",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Der Euklidische Algorithmus (aus den 'Elementen', Buch VII) berechnet den ggT durch wiederholte Division mit Rest. Er ist über 2300 Jahre alt und wird heute noch in Computerprogrammen eingesetzt — einer der ältesten Algorithmen der Menschheit.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Grieche der Antike berechnete den Erdumfang auf bemerkenswert genaue Weise, nur mit einem Stock und der Geometrie der Schatten?",
        answerA = "Archimedes von Syrakus",
        answerB = "Hipparchos von Nikaia",
        answerC = "Eratosthenes von Kyrene",
        answerD = "Ptolemäus von Alexandria",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Eratosthenes (~240 v. Chr.) nutzte den Winkelunterschied von Schattenlängen in Syene und Alexandria, um den Erdumfang auf ~39.375 km zu berechnen. Der tatsächliche Wert beträgt 40.075 km — eine Abweichung von weniger als 2%.",
        funFact = "Eratosthenes war Direktor der berühmten Bibliothek von Alexandria. Seine Berechnung war so einfach wie genial: er wusste, dass in Syene zur Sommersonnenwende kein Schatten entsteht, und maß den Winkel in Alexandria."
    ),

    Question(
        categoryId = 11,
        questionText = "George Cantor zeigte Ende des 19. Jahrhunderts, dass es verschiedene 'Größen' von Unendlichkeit gibt. Welcher Begriff bezeichnet die Mächtigkeit der natürlichen Zahlen?",
        answerA = "Kontinuum",
        answerB = "Aleph-Null (ℵ₀)",
        answerC = "Epsilon-Null (ε₀)",
        answerD = "Omega (ω)",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Cantor bezeichnete die Mächtigkeit der abzählbar unendlichen Menge der natürlichen Zahlen als ℵ₀ (Aleph-Null). Die reellen Zahlen haben die größere Mächtigkeit ℵ₁ = 2^ℵ₀. Dies war der erste Beweis, dass es unendlich viele Stufen von Unendlichkeit gibt.",
        funFact = "Cantors Zeitgenossen lehnten seine Mengenlehre heftig ab. Henri Poincaré nannte sie 'eine Krankheit, von der die Mathematik sich erholen wird'. Cantor litt unter Depressionen und starb in einer psychiatrischen Klinik — heute gilt er als Begründer der modernen Mathematik."
    ),

    // --- MATHEMATIK-GESCHICHTE: ZAHLEN UND SYMBOLE (10) ---

    Question(
        categoryId = 11,
        questionText = "Welche Zivilisation führte als erste die Zahl Null als eigenständige Zahl ein und nicht nur als Platzhalter?",
        answerA = "Babylonier",
        answerB = "Ägypter",
        answerC = "Inder (Brahmagupta, 7. Jh.)",
        answerD = "Araber (Al-Chwarizmi, 9. Jh.)",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Der indische Mathematiker Brahmagupta definierte 628 n. Chr. erstmals die Null als Zahl mit eigenen Rechenregeln (a + 0 = a, a · 0 = 0). Die Babylonier hatten zuvor nur einen Platzhalter für fehlende Stellen, aber keine Zahl Null.",
        funFact = "Brahmaguptas Definition enthielt noch einen Fehler: Er schrieb '0 ÷ 0 = 0'. Die korrekte Antwort ist, dass Division durch Null undefiniert ist — das klärte sich erst Jahrhunderte später."
    ),

    Question(
        categoryId = 11,
        questionText = "Das Symbol π für das Verhältnis von Kreisumfang zu Durchmesser wurde erstmals 1706 von welchem Mathematiker eingeführt?",
        answerA = "Leonhard Euler",
        answerB = "William Jones",
        answerC = "Archimedes",
        answerD = "Isaac Newton",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "William Jones (Wales) verwendete 1706 als erster das Symbol π (pi) für dieses Verhältnis. Leonhard Euler popularisierte es ab 1737 in seinen einflussreichen Werken, was zur allgemeinen Verbreitung führte.",
        funFact = "Der Wert der Kreiszahl π war dem US-Bundesstaat Indiana 1897 offenbar nicht bekannt: Im Gesetzesentwurf 246 (House Bill 246) wurde vorgeschlagen, π gesetzlich auf 3,2 festzulegen. Das Gesetz passierte das Repräsentantenhaus, wurde aber glücklicherweise nicht verabschiedet."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher arabische Mathematiker des 9. Jahrhunderts gilt als Begründer der Algebra und gab ihr ihren Namen?",
        answerA = "Al-Kindi",
        answerB = "Ibn al-Haytham",
        answerC = "Al-Chwarizmi",
        answerD = "Omar Chayyam",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Muhammad ibn Musa al-Chwarizmi schrieb um 820 n. Chr. das Werk 'Al-Kitab al-mukhtasar fi hisab al-gabr wal-muqabala'. Das Wort 'Algebra' stammt aus dem Titel ('al-gabr'). Auch das Wort 'Algorithmus' ist eine Latinisierung seines Namens.",
        funFact = "Al-Chwarizmi arbeitete am 'Haus der Weisheit' in Bagdad — einer der ersten Institutionen für systematische wissenschaftliche Forschung, die Gelehrte aus aller Welt anzog."
    ),

    Question(
        categoryId = 11,
        questionText = "Die Fibonacci-Folge (1, 1, 2, 3, 5, 8, 13, ...) wurde von Leonardo von Pisa (Fibonacci) 1202 eingeführt. Mit welchem praktischen Problem begann sein Buch 'Liber Abaci', das die Folge enthielt?",
        answerA = "Berechnung von Zinsen für Kaufleute",
        answerB = "Wachstum einer Kaninchenpopulation",
        answerC = "Berechnung von Dreiecks-Flächen",
        answerD = "Teilung von Erbschaften",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Fibonacci beschrieb ein idealisiertes Kaninchenproblem: Wie viele Paare entstehen, wenn jedes Paar ab dem zweiten Monat monatlich ein neues Paar gebärt? Die Antwort liefert die Fibonacci-Zahlen. Das Buch 'Liber Abaci' führte auch die arabischen Ziffern in Europa ein.",
        funFact = "Der Name 'Fibonacci' ist ein Spitzname — er bedeutet 'Sohn des Bonacci'. Sein richtiger Name war Leonardo von Pisa. Die Fibonacci-Folge wurde nach ihm benannt, obwohl indische Mathematiker sie Jahrhunderte früher kannten."
    ),

    Question(
        categoryId = 11,
        questionText = "Das Dezimalsystem mit Stellenwert wurde in Europa erst im Mittelalter eingeführt. Welches Zahlensystem nutzten die Babylonier, das sich noch heute in unserer Zeitmessung widerspiegelt?",
        answerA = "Zwölfersystem (Duodezimal)",
        answerB = "Zwanzigsystem (Vigesimal)",
        answerC = "Sechzigsystem (Sexagesimal)",
        answerD = "Achtersystem (Oktal)",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Die Babylonier verwendeten ein Sexagesimalsystem (Basis 60). Deshalb haben wir 60 Minuten in einer Stunde, 60 Sekunden in einer Minute und 360 Grad im Kreis — diese Einteilungen stammen direkt aus dem babylonischen Erbe.",
        funFact = "60 ist eine praktische Basis: Sie ist durch 1, 2, 3, 4, 5, 6, 10, 12, 15, 20 und 30 teilbar — mehr Teiler als jede kleinere Zahl. Das machte Bruchrechnung im Kopf einfacher."
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Symbol führte Leonhard Euler in die Mathematik ein, das heute die Eulerzahl (≈ 2,71828) bezeichnet?",
        answerA = "Das Symbol e",
        answerB = "Das Symbol i für imaginäre Einheit",
        answerC = "Das Symbol Σ für Summen",
        answerD = "Das Symbol f(x) für Funktionen",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Euler führte das Symbol 'e' für die Basis des natürlichen Logarithmus ein. Wahrscheinlich steht 'e' für 'exponential'. Er entdeckte auch die berühmte Eulersche Identität: e^(iπ) + 1 = 0, die fünf fundamentale mathematische Konstanten verbindet.",
        funFact = "Euler führte mehr mathematische Symbole ein als jeder andere Mensch: e, i, π (popularisiert), Σ (Summe), f(x) für Funktion, das Zeichen für 'unendlich' ∞ war von John Wallis, aber Euler machte es Standard."
    ),

    Question(
        categoryId = 11,
        questionText = "Die Primzahlen des Siebs des Eratosthenes sind ein bekanntes Verfahren. Welcher Satz beschreibt die Verteilung der Primzahlen unter den natürlichen Zahlen asymptotisch?",
        answerA = "Fermats kleiner Satz",
        answerB = "Primzahlsatz: π(n) ~ n/ln(n)",
        answerC = "Wilsons Theorem über Primzahlen",
        answerD = "Bertrandsches Postulat",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Der Primzahlsatz besagt: Die Anzahl der Primzahlen bis n, π(n), nähert sich n/ln(n). D.h. etwa 1 von ln(n) Zahlen um n ist eine Primzahl. Der Satz wurde 1896 unabhängig von Hadamard und de la Vallée Poussin bewiesen.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher indische Mathematiker des frühen 20. Jahrhunderts, ohne formale Ausbildung, schickte Gauß' Nachfolgern in Cambridge Formeln, von denen viele neu und korrekt waren?",
        answerA = "Ramanujan",
        answerB = "Bhaskaracharya",
        answerC = "Aryabhata",
        answerD = "Brahmagupta",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Srinivasa Ramanujan (1887–1920) war Autodidakt aus Madras. Er schrieb 1913 an G. H. Hardy in Cambridge — die Formeln überzeugten Hardy sofort. Ramanujan entdeckte tausende Formeln, viele ohne Beweis und in Bereichen, die Experten jahrelang beschäftigten.",
        funFact = "Hardy besuchte Ramanujan in London im Krankenhaus und erwähnte, er sei mit Taxi Nr. 1729 gekommen — einer langweiligen Zahl. Ramanujan widersprach sofort: 1729 ist die kleinste Zahl, die sich auf zwei verschiedene Arten als Summe zweier Kuben schreiben lässt (1³+12³ = 9³+10³)."
    ),

    Question(
        categoryId = 11,
        questionText = "Die Zahl φ (Phi), der goldene Schnitt, ergibt sich als φ = (1+√5)/2 ≈ 1,618. Welche besondere Eigenschaft hat φ in Bezug auf sein Quadrat?",
        answerA = "φ² = φ + 1",
        answerB = "φ² = 2φ",
        answerC = "φ² = φ · π",
        answerD = "φ² = e",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Der goldene Schnitt erfüllt φ² = φ + 1. Daraus folgt auch 1/φ = φ - 1. Diese Selbstähnlichkeit macht φ einzigartig: es ist die am schwersten durch Brüche annäherbare irrationale Zahl und erscheint in Fibonacci-Folge, Pflanzenblättern und Nautilus-Spiralen.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Zahlensystem verwendeten die Maya, das unter anderem durch eine eigenständige Null und die Basis 20 bekannt ist?",
        answerA = "Dezimalsystem mit Null",
        answerB = "Duodezimalsystem",
        answerC = "Vigesimalsystem",
        answerD = "Binärsystem",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Das Vigesimalsystem (Basis 20) der Maya entwickelte sich wahrscheinlich durch Zählen mit Zehen und Fingern. Die Maya besaßen eine Null (als Muschelform dargestellt) unabhängig von Indien — und damit eines der ausgereiftesten Zahlensysteme der Antike.",
        funFact = "Das Maya-Zahlensystem war teilweise ein Mischsystem: Für Kalenderberechnungen nutzten sie 18 × 20 = 360 (nahe einem Sonnenjahr), nicht reines 20er-System. Ihr Kalender war präziser als der gregorianische Kalender."
    ),

    // --- MATHEMATIK-GESCHICHTE: GEOMETRIE UND TOPOLOGIE (10) ---

    Question(
        categoryId = 11,
        questionText = "Der Satz des Pythagoras war in Babylonien und Indien bereits Jahrhunderte vor Pythagoras bekannt. Für welchen praktischen Zweck nutzten ihn die alten Ägypter?",
        answerA = "Berechnung von Kreisflächen",
        answerB = "Konstruktion rechter Winkel beim Pyramidenbau",
        answerC = "Navigation auf dem Nil",
        answerD = "Berechnung des Mondkalenders",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Ägyptische Feldmesser ('Harpedonapten', Seilspanner) nutzten ein Seil mit 12 gleichmäßigen Knoten: Ein Dreieck 3-4-5 ergibt immer einen rechten Winkel (3²+4²=5²). So wurden rechte Winkel bei Grundsteinlegungen und Pyramidenbau konstruiert.",
        funFact = "Das ägyptische Seildreieck 3-4-5 ist das einfachste pythagoreische Zahlentripel. Pythagoras selbst soll in Ägypten gelernt haben — die Griechen nannten ihn später fälschlicherweise Urheber des Satzes."
    ),

    Question(
        categoryId = 11,
        questionText = "Das Möbiusband wurde 1858 unabhängig von August Möbius und Johann Benedict Listing entdeckt. Was ist seine topologisch einzigartige Eigenschaft?",
        answerA = "Es hat keine Ecken",
        answerB = "Es ist eine einseitige Fläche mit nur einer Kante",
        answerC = "Es kann nicht aus Papier gebaut werden",
        answerD = "Es hat zwei Seiten aber keine Kante",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Ein Möbiusband hat nur eine Seite und eine Kante. Zeichnet man eine Linie auf einer Seite, gelangt man auf die 'andere Seite', ohne das Band zu überqueren. Es ist nicht orientierbar — ein fundamentales Konzept der Topologie.",
        funFact = "Bandschmiermaschinen in Fabriken nutzten früher manchmal Möbiusbänder als Förderbänder — so verschleißt das Band gleichmäßig auf beiden Seiten, weil es nur eine Seite hat."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher griechische Mathematiker bewies um 250 v. Chr., dass der Kreis die Fläche maximiert unter allen Kurven gleicher Länge (isoperimetrisches Problem)?",
        answerA = "Euklid",
        answerB = "Pythagoras",
        answerC = "Zenodoros",
        answerD = "Archimedes",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Zenodoros (um 200 v. Chr.) bewies, dass unter allen Figuren gleichen Umfangs der Kreis die größte Fläche einschließt. Der strenge Beweis gelang jedoch erst im 19. Jahrhundert. Dieses Problem erklärt, warum Seifenblasen kugelförmig sind.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Die vier-Farben-Vermutung besagt, dass jede planare Landkarte mit höchstens vier Farben so eingefärbt werden kann, dass benachbarte Länder verschiedene Farben haben. Wann wurde sie bewiesen?",
        answerA = "1890 von Heawood",
        answerB = "1976 von Appel und Haken",
        answerC = "1931 von Gödel",
        answerD = "1995 von Robertson et al.",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Kenneth Appel und Wolfgang Haken bewiesen den Vier-Farben-Satz 1976 — aber erstmals in der Geschichte durch massiven Computereinsatz (1200 Stunden CPU-Zeit, 1936 Fälle geprüft). Viele Mathematiker akzeptierten dies zunächst nicht als 'echten' Beweis.",
        funFact = "Die Vier-Farben-Vermutung wurde 1852 von Francis Guthrie beim Einfärben einer Karte von England formuliert. Sein Bruder gab sie an Augustus de Morgan weiter. Über 120 Jahre gingen 'Beweise' ein — alle hatten Fehler."
    ),

    Question(
        categoryId = 11,
        questionText = "Wer beschrieb 1854 die Riemannsche Geometrie, die die Grundlage von Einsteins allgemeiner Relativitätstheorie wurde?",
        answerA = "Carl Friedrich Gauß",
        answerB = "Bernhard Riemann",
        answerC = "Felix Klein",
        answerD = "Hermann Minkowski",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Bernhard Riemanns Habilitationsvortrag 'Über die Hypothesen, welche der Geometrie zugrunde liegen' (1854) begründete die differenzielle Geometrie gekrümmter Räume. 60 Jahre später nutzte Einstein genau diese Mathematik für die allgemeine Relativitätstheorie.",
        funFact = "Riemann war so schüchtern und kränklich, dass er seinen Habilitationsvortrag mehrfach verschob. Als er ihn schließlich hielt, war sogar Gauß begeistert — ein seltenes Lob vom Schweiger Gauß."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das Königsberger Brückenproblem, das Euler 1736 löste?",
        answerA = "Kann man alle 7 Brücken über den Pregel genau einmal überqueren?",
        answerB = "Wie baut man die kürzeste Brücke über einen Fluss?",
        answerC = "Wie viele Brücken benötigt man, um alle Stadtteile zu verbinden?",
        answerD = "Auf wie viele Wege kann man Königsberg durchqueren?",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Euler abstrahierte Königsberg als Graph: 4 Stadtteile (Knoten) verbunden durch 7 Brücken (Kanten). Er bewies, dass ein Eulerweg (jede Kante genau einmal) nur existiert, wenn höchstens 2 Knoten ungeraden Grad haben. In Königsberg haben alle 4 Knoten ungeraden Grad — unmöglich.",
        funFact = "Die Brücken von Königsberg (heute Kaliningrad) wurden im Zweiten Weltkrieg größtenteils zerstört. Heute gibt es andere Brücken — das moderne Königsberg erfüllt die Euler-Bedingung und das Problem wäre lösbar."
    ),

    Question(
        categoryId = 11,
        questionText = "Henri Poincaré formulierte 1904 eine Vermutung über dreidimensionale Sphären. Wann und durch wen wurde sie bewiesen?",
        answerA = "2003 durch Grigori Perelman",
        answerB = "1994 durch Andrew Wiles",
        answerC = "2010 durch Terence Tao",
        answerD = "1976 durch Appel und Haken",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Grigori Perelman bewies die Poincaré-Vermutung 2003 mithilfe des Ricci-Flusses. Es ist das einzige der sieben Millennium-Probleme, das bisher gelöst wurde. Das Preisgeld von 1 Million Dollar lehnte Perelman ab.",
        funFact = "Perelman lehnte auch die Fields-Medaille 2006 ab und zog sich aus der Mathematik zurück. Er soll heute in St. Petersburg bei seiner Mutter leben und Pilze sammeln gehen."
    ),

    Question(
        categoryId = 11,
        questionText = "Die projektive Geometrie wurde im 17. Jahrhundert von Girard Desargues entwickelt. Was ist ihr zentrales Konzept gegenüber der euklidischen Geometrie?",
        answerA = "Parallele Linien schneiden sich im Unendlichen",
        answerB = "Dreiecke können keine Winkel haben",
        answerC = "Abstände bleiben unter Transformationen erhalten",
        answerD = "Kurven können keine Tangenten haben",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "In der projektiven Geometrie werden Fernpunkte eingeführt, wo parallele Linien sich treffen. Dies erklärt Perspektive in der Malerei und hat Anwendungen in der Computergrafik. Es gibt keine Ausnahmen für Parallelen — zwei Geraden schneiden sich immer.",
        funFact = "Desargues' Werk war so weit seiner Zeit voraus, dass es von Zeitgenossen kaum verstanden wurde. Es wurde erst 200 Jahre später durch Poncelet wiederentdeckt — wäre es nicht von einem Schüler abgeschrieben worden, wäre es verloren gegangen."
    ),

    Question(
        categoryId = 11,
        questionText = "Der Satz von Pick (1899) erlaubt die genaue Berechnung der Fläche eines Gitterpolygons. Was besagt er?",
        answerA = "Fläche = Innenpunkte + Randpunkte/2 - 1",
        answerB = "Fläche = Eckenzahl × Abstand²",
        answerC = "Fläche = Umfang² / (4π)",
        answerD = "Fläche = Diagonalen × sin(Winkel) / 2",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Georg Picks Satz: A = I + R/2 - 1, wobei I die Anzahl der Gitterpunkte im Inneren und R die Anzahl der Gitterpunkte auf dem Rand ist. Damit lässt sich die exakte Fläche eines Gitterpolygons durch reines Zählen bestimmen.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Eulers Polyederformel (1750) beschreibt einen Zusammenhang zwischen Ecken (V), Kanten (E) und Flächen (F) eines konvexen Polyeders. Wie lautet sie?",
        answerA = "V + E + F = 6",
        answerB = "V × E = F²",
        answerC = "V + F = E + 1",
        answerD = "V - E + F = 2",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Eulers Formel V - E + F = 2 gilt für alle konvexen Polyeder. Für einen Würfel: 8 - 12 + 6 = 2. Sie ist der Grundsatz der algebraischen Topologie und gilt auch für alle topologisch äquivalenten Flächen der Sphäre. Der Ausdruck V - E + F heißt Euler-Charakteristik.",
        funFact = "Descartes hatte eine ähnliche Formel 100 Jahre früher in unveröffentlichten Notizen — aber Euler erhielt die Anerkennung, weil er sie systematisch bewies und publizierte. Die Formel wurde zum Fundament der Topologie."
    ),

    // --- MATHEMATIK-GESCHICHTE: ANALYSE UND ALGEBRA (10) ---

    Question(
        categoryId = 11,
        questionText = "Welcher Mathematiker entwickelte um 1800 den strengen Epsilon-Delta-Beweis und legte damit das Fundament der modernen Analysis?",
        answerA = "Bernhard Bolzano und Augustin-Louis Cauchy",
        answerB = "Newton und Leibniz",
        answerC = "Lagrange und Laplace",
        answerD = "Fourier und Gauss",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Bolzano (1817) und Cauchy (1820er) formulierten den Grenzwertbegriff präzise mit Epsilon-Delta. Karl Weierstraß vollendete dies später. Zuvor arbeitete die Analysis mit 'unendlich kleinen' Größen, die logisch inkonsistent waren.",
        funFact = "Cauchys Lehrbücher 'Cours d'Analyse' (1821) und 'Résumé des leçons sur le calcul infinitésimal' gelten als erste moderne mathematische Lehrbücher — er war einer der ersten, der Mathematik wirklich rigoros lehrte."
    ),

    Question(
        categoryId = 11,
        questionText = "Evariste Galois entwickelte vor seinem Tod mit 20 Jahren eine Theorie, die erklärt, warum allgemeine Gleichungen 5. Grades keine Lösungsformel haben. Wie starb er?",
        answerA = "An Tuberkulose wie Ramanujan",
        answerB = "Im Duell nach einer gescheiterten Liebesaffäre",
        answerC = "Bei der Pariser Commune als Revolutionär",
        answerD = "In Schuldgefängnis für seine politischen Aktivitäten",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Evariste Galois (1811–1832) starb im Alter von 20 Jahren in einem Pistolenduel. Die Nacht davor schrieb er fieberhaft seine mathematischen Erkenntnisse auf — er ahnte, er würde sterben. Diese Notizen begründeten die Galois-Theorie und die moderne Gruppentheorie.",
        funFact = "Galois schrieb am Rand seiner Nacht-Notizen 'Ich habe keine Zeit' — und beschrieb trotzdem Theoreme, deren vollständiges Verständnis Mathematiker 50 Jahre beschäftigte. Er war auch zweimal politisch inhaftiert."
    ),

    Question(
        categoryId = 11,
        questionText = "Joseph Fourier entdeckte 1807, dass fast jede periodische Funktion als Summe von Sinus- und Kosinusfunktionen dargestellt werden kann. Wofür entwickelte er diese Methode ursprünglich?",
        answerA = "Analyse von Schallwellen und Musik",
        answerB = "Beschreibung der Wärmeausbreitung",
        answerC = "Berechnung von Planetenbahnen",
        answerD = "Lösung von Differenzialgleichungen in der Mechanik",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Fourier entwickelte seine Reihen zur Lösung der Wärmeleitungsgleichung. Er begleitete Napoleon auf dem Ägyptenfeldzug und war fasziniert von der Wärme der Wüste. Die Fourier-Transformation ist heute in Signalverarbeitung, MP3, JPEG und MRT allgegenwärtig.",
        funFact = "Fourier glaubte, Wärme sei heilsam und soll sich selbst in Decken gewickelt haben, auch im Sommer. Er starb 1830 vermutlich durch einen Sturz — er trug so viele Decken, dass er die Stufen nicht sah."
    ),

    Question(
        categoryId = 11,
        questionText = "Abel und Ruffini bewiesen Anfang des 19. Jahrhunderts, dass allgemeine Polynomgleichungen ab welchem Grad keine allgemeine Lösungsformel durch Wurzeln und Grundrechenarten haben?",
        answerA = "Grad 3",
        answerB = "Grad 4",
        answerC = "Grad 5",
        answerD = "Grad 7",
        correctAnswer = 2,
        difficulty = 3,
        explanation = "Paolo Ruffini (1799) und Niels Henrik Abel (1824) bewiesen unabhängig, dass es keine algebraische Lösungsformel für den allgemeinen Fall des Grades 5 oder höher gibt. Für Grade 1-4 gibt es Formeln (quadratische, kubische, Formel für Grad 4). Die genaue Klassifikation lieferte Galois.",
        funFact = "Abel schickte seinen Beweis an Gauß — der las ihn nicht, weil er Manuskripte unbekannter Autoren grundsätzlich nicht öffnete. Abel starb mit 26 Jahren an Tuberkulose in bitterer Armut. Zwei Tage nach seinem Tod traf ein Brief ein, der ihm eine Professur in Berlin anbot."
    ),

    Question(
        categoryId = 11,
        questionText = "Georg Cantor entwickelte die Mengenlehre und zeigte mit der Diagonalargument-Methode, dass reelle Zahlen überabzählbar unendlich sind. Was besagt dieses Argument?",
        answerA = "Es gibt keine Bijektion zwischen natürlichen und reellen Zahlen",
        answerB = "Jede Menge hat eine Potenzmenge mit größerer Mächtigkeit",
        answerC = "Unendliche Mengen sind alle gleich groß",
        answerD = "Die rationalen Zahlen bilden eine überabzählbare Menge",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Cantors Diagonalargument (1891): Angenommen, alle reellen Zahlen zwischen 0 und 1 lassen sich auflisten. Dann konstruiere eine neue Zahl, indem du die n-te Stelle der n-ten Zahl änderst — diese neue Zahl steht nicht in der Liste. Widerspruch: die Liste war nicht vollständig.",
        funFact = "Cantors Diagonalargument ist so elegant, dass es heute in vielen verschiedenen Kontexten verwendet wird — z.B. beim Beweis des Haltproblems in der Informatik (Turing 1936) und bei Gödels Unvollständigkeitssatz."
    ),

    Question(
        categoryId = 11,
        questionText = "Die hyperbolischen Funktionen sinh, cosh und tanh wurden im 18. Jahrhundert eingeführt. Wer prägte die heute übliche Schreibweise dieser Funktionen?",
        answerA = "Johann Heinrich Lambert",
        answerB = "Leonhard Euler",
        answerC = "Joseph-Louis Lagrange",
        answerD = "Jean le Rond d'Alembert",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Johann Heinrich Lambert führte 1768 die hyperbolischen Funktionen systematisch ein und gab ihnen Namen. Er bewies auch als erster, dass π irrational ist — eine bedeutende Leistung für das 18. Jahrhundert.",
        funFact = null
    ),

    Question(
        categoryId = 11,
        questionText = "Welches Theorem der Algebra, bewiesen von Gauß in seiner Dissertation 1799, garantiert die Existenz von Nullstellen?",
        answerA = "Satz von Vieta über Wurzeln",
        answerB = "Fundamentalsatz der Algebra: Jedes nicht-konstante Polynom hat mindestens eine komplexe Nullstelle",
        answerC = "Cayley-Hamilton-Theorem über Matrizen",
        answerD = "Lagrangescher Mittelwertsatz",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Der Fundamentalsatz der Algebra besagt, dass jedes nicht-konstante komplexe Polynom vom Grad n genau n komplexe Nullstellen hat (mit Vielfachheit). Gauß gab vier verschiedene Beweise in seinem Leben — der erste war sein Dissertationsthema.",
        funFact = "In Gauß' erstem Beweis von 1799 befand sich ein kleiner topologischer Lücke, die erst 1920 von Alexander Ostrowski vollständig geschlossen wurde — über 100 Jahre nach dem Originalbeweis."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das Hilbert-Programm, das David Hilbert 1900 formulierte und Gödel 1931 widerlegte?",
        answerA = "Die Axiomatisierung der gesamten Mathematik in einem vollständigen, widerspruchsfreien System",
        answerB = "Die Berechnung aller Primzahlen durch einen einzigen Algorithmus",
        answerC = "Die Vereinheitlichung von Algebra und Geometrie",
        answerD = "Die Formalisierung der Wahrscheinlichkeitsrechnung",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Hilbert wollte die Mathematik auf ein endliches System von Axiomen gründen, aus denen alle wahren mathematischen Aussagen ableitbar und die Widerspruchsfreiheit beweisbar sein sollte. Gödels Unvollständigkeitssatz zeigte 1931, dass dies unmöglich ist.",
        funFact = "Auf dem Internationalen Mathematikerkongress 1900 stellte Hilbert seine 23 berühmten ungelösten Probleme vor. Heute (2026) sind etwa 10 vollständig gelöst, mehrere teilweise, und einige noch vollständig offen."
    ),

    Question(
        categoryId = 11,
        questionText = "Sophie Germain entwickelte Anfang des 19. Jahrhunderts wichtige Ergebnisse zu Fermats letztem Satz. Warum kommunizierte sie mit Lagrange und Gauß unter falschem Namen?",
        answerA = "Weil Frauen an der École Polytechnique nicht zugelassen waren",
        answerB = "Weil ihr Vater ihre Forschung verboten hatte",
        answerC = "Weil Plagiatsvorwürfe gegen Frauen häufiger erhoben wurden",
        answerD = "Weil sie politisch verfolgt wurde",
        correctAnswer = 0,
        difficulty = 3,
        explanation = "Sophie Germain (1776–1831) schrieb zunächst als 'Monsieur LeBlanc', da Frauen nicht als ernsthafte Mathematiker angesehen wurden. Als Gauß ihre wahre Identität entdeckte, antwortete er bewundernd: eine Frau, die so denkt, ist ein außerordentlicheres Wesen.",
        funFact = "Sophie Germain erhielt als erste Frau überhaupt einen Vortrag an der Académie des Sciences — obwohl sie nie Mitglied werden durfte. Für ihre Arbeiten über Elastizität gewann sie dreimal hintereinander den Grand Prix der Académie."
    ),

    Question(
        categoryId = 11,
        questionText = "Die komplexen Zahlen wurden erst im 16. Jahrhundert bei der Lösung kubischer Gleichungen eingeführt. Wer nannte die imaginäre Einheit √(-1) erstmals 'imaginär'?",
        answerA = "Gerolamo Cardano",
        answerB = "René Descartes",
        answerC = "Leonhard Euler",
        answerD = "Carl Friedrich Gauß",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "René Descartes prägte 1637 den Begriff 'imaginäre Zahlen' (als abwertenden Begriff — er hielt sie für nicht real). Euler führte das Symbol 'i' ein. Gauß bewies, dass komplexe Zahlen geometrisch als Punkte der Ebene darstellbar sind und vollständig konsistent sind.",
        funFact = "Cardano nutzte komplexe Zahlen 1545 in seinen Lösungsformeln für kubische Gleichungen — er nannte sie 'sinnlos' und 'torturierend', aber rechnerisch nützlich. Es dauerte noch 200 Jahre, bis Mathematiker sie wirklich akzeptierten."
    ),

    // --- MATHEMATIK-GESCHICHTE: KURIOSITÄTEN UND ANWENDUNGEN (10) ---

    Question(
        categoryId = 11,
        questionText = "Das Knochen von Ishango, gefunden im Kongo und über 20.000 Jahre alt, gilt als mögliches frühestes Zeugnis mathematischen Denkens. Was zeigt es?",
        answerA = "Darstellungen geometrischer Formen",
        answerB = "Einkerbungen, die möglicherweise Primzahlen oder Mondphasen darstellen",
        answerC = "Ein Sexagesimalsystem für Jahreszeiten",
        answerD = "Die ersten bekannten Bruchdarstellungen",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "Der Ishango-Knochen hat drei Spalten mit Kerben: Die mittlere Spalte enthält die Zahlen 11, 13, 17, 19 — allesamt Primzahlen zwischen 10 und 20. Ob er ein Rechenwerkzeug, ein Mondkalender oder ein Zählstab war, ist unter Forschern umstritten.",
        funFact = "Der Ishango-Knochen ist im Naturkundemuseum Brüssel ausgestellt. Seine Bedeutung war über Jahrzehnte umstritten — manche sehen darin nur zufällige Kratzer. Jüngere Forschungen deuten aber auf bewusste, symmetrische Einteilung hin."
    ),

    Question(
        categoryId = 11,
        questionText = "Das Millennium-Problem P vs. NP ist das wichtigste offene Problem der Informatik/Mathematik. Was fragt es?",
        answerA = "Ob jedes Problem, das schnell gelöst werden kann, auch schnell verifiziert werden kann",
        answerB = "Ob jedes Problem, dessen Lösung schnell verifizierbar ist, auch schnell lösbar ist",
        answerC = "Ob Primzahlprüfung in polynomieller Zeit möglich ist",
        answerD = "Ob das Haltproblem entscheidbar ist",
        correctAnswer = 1,
        difficulty = 3,
        explanation = "P = Probleme, die in polynomieller Zeit lösbar sind. NP = Probleme, deren Lösung in polynomieller Zeit verifizierbar ist. Die Frage: Ist P = NP? Wenn ja, könnten viele als 'schwer' geltende Probleme (z.B. Kryptographie) effizient gelöst werden. Die meisten Experten vermuten P ≠ NP.",
        funFact = "Ein Beweis von P = NP würde die gesamte moderne Internetkryptographie brechen. Banken, E-Mail-Verschlüsselung, sichere Online-Kommunikation basieren darauf, dass bestimmte Probleme keine effizienten Lösungsalgorithmen haben."
    ),

    Question(
        categoryId = 11,
        questionText = "Welcher Mathematiker erfand 1854 die Boolesche Algebra, die heute die Grundlage aller digitalen Computer bildet?",
        answerA = "Charles Babbage",
        answerB = "Augustus de Morgan",
        answerC = "Alan Turing",
        answerD = "George Boole",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "George Boole veröffentlichte 1854 'An Investigation of the Laws of Thought', in der er Logik als algebraisches System beschrieb mit nur zwei Werten (wahr/falsch, 1/0). Claude Shannon erkannte 1937, dass Boolesche Algebra perfekt für elektrische Schaltkreise geeignet ist.",
        funFact = "George Boole starb 1864 auf eigenartige Weise: Er lief bei Regen zur Universität, kam nass an und legte sich ins nasse Bett — weil seine Frau glaubte, ein Leiden sei am besten mit dem zu kurieren, was es verursachte. Er starb an Lungenentzündung."
    ),

    Question(
        categoryId = 11,
        questionText = "Das Paradoxon von Zenon von Elea (5. Jh. v. Chr.) behauptet, Achilles könne eine Schildkröte nie einholen. Welches mathematische Konzept löst dieses Paradoxon?",
        answerA = "Das Konzept der irrationalen Zahlen",
        answerB = "Das Prinzip der vollständigen Induktion",
        answerC = "Die Theorie der komplexen Zahlen",
        answerD = "Konvergenz unendlicher Reihen",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Zenons Paradoxon entsteht, weil eine unendliche Folge von Schritten (1/2 + 1/4 + 1/8 + ...) zu einer endlichen Summe konvergiert (= 1). Unendlich viele Schritte können in endlicher Zeit durchlaufen werden, wenn die Schritte schnell genug kleiner werden.",
        funFact = "Zenon formulierte weitere Paradoxa: Der fliegende Pfeil steht still (weil er zu jedem Zeitpunkt an einem bestimmten Ort ist). Diese Probleme beschäftigten Philosophen und Mathematiker bis ins 19. Jahrhundert, als Cauchy den Grenzwertbegriff präzisierte."
    ),

    Question(
        categoryId = 11,
        questionText = "Die Schachbrettkorner-Aufgabe: Wenn man auf das erste Feld 1 Reiskorn legt, auf das zweite 2, auf das dritte 4 usw. — wie viele Körner liegen auf allen 64 Feldern zusammen?",
        answerA = "64² = 4096 Körner",
        answerB = "64! (Fakultät)",
        answerC = "Fibonacci(64) Körner",
        answerD = "2⁶⁴ - 1 = ca. 18 × 10¹⁸ Körner",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Die geometrische Reihe ergibt: 1 + 2 + 4 + ... + 2⁶³ = 2⁶⁴ - 1 ≈ 18,4 × 10¹⁸ Körner. Die Weltreisernte beträgt ca. 500 Millionen Tonnen pro Jahr — diese Menge würde für über 1000 Jahre reichen.",
        funFact = "Die Legende erzählt, ein weiser Mann habe dem indischen König diesen Wunsch als Belohnung für das Erfinden des Schachspiels gestellt. Als der König die scheinbar bescheidene Bitte erfüllte und nachrechnen ließ, stellte sich heraus, dass sein Königreich unmöglich zahlen konnte."
    ),

    Question(
        categoryId = 11,
        questionText = "Das Geburtstagsproblem der Wahrscheinlichkeitsrechnung: Ab wie vielen Personen in einem Raum ist die Wahrscheinlichkeit, dass zwei denselben Geburtstag haben, größer als 50%?",
        answerA = "183 Personen",
        answerB = "50 Personen",
        answerC = "73 Personen",
        answerD = "23 Personen",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Bereits ab 23 Personen übersteigt die Wahrscheinlichkeit eines gemeinsamen Geburtstags die 50%-Marke (genau: 50,73%). Dies überrascht Intuition, da man ans 365/23 denkt statt ans Gegenereignis zu rechnen. Das Gegenereignis (kein gemeinsamer) sinkt exponentiell.",
        funFact = "Das Geburtstagsproblem hat praktische Anwendungen in der Kryptographie: Beim 'Birthday Attack' sucht man Kollisionen in Hash-Funktionen. Eine 128-Bit-Hash-Funktion benötigt nur ~2⁶⁴ zufällige Eingaben, um mit hoher Wahrscheinlichkeit eine Kollision zu finden."
    ),

    Question(
        categoryId = 11,
        questionText = "Alan Turing erfand 1936 das Konzept der Turingmaschine. Was beweist die Unmöglichkeit des Halteproblems?",
        answerA = "Dass keine Computer gebaut werden können",
        answerB = "Dass Primzahlprüfung exponentielle Zeit braucht",
        answerC = "Dass künstliche Intelligenz unmöglich ist",
        answerD = "Dass es kein allgemeines Programm geben kann, das entscheidet, ob ein beliebiges Programm anhält",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Turing bewies, dass es kein Programm H gibt, das für jedes beliebige Programm P und Eingabe x entscheidet, ob P(x) terminiert. Der Beweis nutzt ein Diagonalargument ähnlich Cantors: Angenommen H existiert, konstruiere ein Programm, das H widerspricht.",
        funFact = "Turing erhielt für seine Arbeit an Enigma-Entschlüsselung im 2. Weltkrieg kaum öffentliche Anerkennung, da alles geheim war. 1952 wurde er wegen Homosexualität verurteilt und chemisch kastriert. Er starb 1954 — wahrscheinlich durch Suizid mit einem vergifteten Apfel."
    ),

    Question(
        categoryId = 11,
        questionText = "Die Spieltheorie wurde 1944 von John von Neumann und Oskar Morgenstern begründet. Welches bekannte Konzept entwickelte John Nash 1950 daraus weiter?",
        answerA = "Das Minimax-Theorem für Nullsummenspiele",
        answerB = "Die Theorie kooperativer Spiele und fairer Teilung",
        answerC = "Die Lösung des Gefangenendilemmas",
        answerD = "Das Nash-Gleichgewicht: kein Spieler kann durch einseitige Änderung profitieren",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Das Nash-Gleichgewicht (1950) beschreibt einen Zustand, in dem kein Spieler seinen Nutzen verbessern kann, wenn alle anderen ihre Strategie beibehalten. Es existiert in jedem endlichen Spiel. Nash erhielt 1994 den Wirtschaftsnobelpreis.",
        funFact = "John Nash litt ab den 1950ern an paranoider Schizophrenie. Er kämpfte Jahrzehnte gegen die Krankheit und erholte sich schließlich. Sein Leben wurde im Film 'A Beautiful Mind' (2001) verfilmt. Nash und seine Frau starben 2015 bei einem Taxiunfall kurz nach der Nobelpreisverleihung."
    ),

    Question(
        categoryId = 11,
        questionText = "Die Banach-Tarski-Paradoxon besagt, dass eine Kugel in endlich viele Teile zerlegt und zu zwei Kugeln gleicher Größe zusammengesetzt werden kann. Was ist der mathematische Trick dahinter?",
        answerA = "Fraktale mit unendlichem Umfang",
        answerB = "Die 4. Dimension des Raumes",
        answerC = "Die Riemannsche Geometrie",
        answerD = "Nicht messbare Mengen — die Teile haben keinen definierten Rauminhalt",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Das Banach-Tarski-Paradoxon (1924) nutzt das Auswahlaxiom der Mengenlehre, um nicht messbare Mengen zu konstruieren. Diese 'Teile' existieren mathematisch, sind aber physikalisch nicht konstruierbar — sie haben weder definierten Rauminhalt noch lassen sie sich schneiden.",
        funFact = "Das Paradoxon zeigt, dass das Auswahlaxiom zu kontraintuitiven Konsequenzen führt — aber die Alternative (Mathematik ohne Auswahlaxiom) wäre noch schwieriger. Mathematiker akzeptieren es meist pragmatisch, obwohl es 'Materie aus dem Nichts' erlaubt."
    ),

    Question(
        categoryId = 11,
        questionText = "Welche Papierfaltung beschreibt das exponentielle Wachstum: Wie viele Schichten entstehen nach 7 Faltungen?",
        answerA = "49 Schichten (7 × 7)",
        answerB = "14 Schichten (7 × 2)",
        answerC = "64 Schichten (2⁶)",
        answerD = "128 Schichten (2⁷)",
        correctAnswer = 3,
        difficulty = 3,
        explanation = "Nach n Faltungen entstehen 2ⁿ Schichten. Nach 7 Faltungen: 2⁷ = 128 Schichten. Theoretisch würde ein 0,1mm-dickes Blatt nach 42 Faltungen die Erde-Mond-Distanz erreichen — exponentielles Wachstum übertrifft jede Vorstellung.",
        funFact = "Die Schülerin Britney Gallivan bewies 2002 mathematisch, dass ein Blatt Toilettenpapier theoretisch 12 Mal gefaltet werden kann — und tat es auch. Zuvor galt 7 als absolute Grenze. Sie leitete eine exakte Formel für die Mindestlänge des Papiers ab."
    )

)
