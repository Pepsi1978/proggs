package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun literatureQuestionsHard4(): List<Question> = listOf(

    // Question 1 — Homer: Ilias — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welcher griechische Held tötet Hektor vor den Mauern Trojas im Zweikampf?",
        answerA = "Achilleus",
        answerB = "Agamemnon",
        answerC = "Odysseus",
        answerD = "Diomedes",
        correctAnswer = 0,
        explanation = "Achilleus tötet Hektor im Zweikampf vor den Mauern Trojas, um den Tod seines Freundes Patroklos zu rächen.",
        difficulty = 3,
        funFact = "Achilleus schleift Hektors Leichnam danach dreimal um die Stadtmauern Trojas, bevor Hektors Vater Priamos ihn zurückkauft."
    ),

    // Question 2 — Homer: Ilias — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welches Mädchen nimmt Agamemnon dem Achilleus weg und löst damit dessen Rückzug aus dem Kampf aus?",
        answerA = "Kassandra",
        answerB = "Briseis",
        answerC = "Andromache",
        answerD = "Chryseis",
        correctAnswer = 1,
        explanation = "Agamemnon entreißt Achilleus die Kriegsbeute Briseis, woraufhin Achilleus beleidigt den Kampf verweigert und sich in sein Zelt zurückzieht.",
        difficulty = 3,
        funFact = "Chryseis ist die Tochter des Priesters Chryses, die Agamemnon zuerst als Beute hatte — er musste sie zurückgeben und nahm dafür Briseis."
    ),

    // Question 3 — Homer: Ilias — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wer kämpft in der Ilias als Stellvertreter des Achilleus und fällt dabei?",
        answerA = "Antilóchos",
        answerB = "Aias der Große",
        answerC = "Patroklos",
        answerD = "Menelaos",
        correctAnswer = 2,
        explanation = "Patroklos, der engste Freund des Achilleus, nimmt dessen Rüstung und führt die Griechen an — wird aber von Hektor getötet.",
        difficulty = 3,
        funFact = "Patroklos wird zuerst von Apollon betäubt, dann von Euphorbos verwundet und schließlich von Hektor getötet."
    ),

    // Question 4 — Homer: Odyssee — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wie viele Jahre hält die Nymphe Kalypso den Odysseus auf ihrer Insel Ogygia fest?",
        answerA = "Drei Jahre",
        answerB = "Fünf Jahre",
        answerC = "Zehn Jahre",
        answerD = "Sieben Jahre",
        correctAnswer = 3,
        explanation = "Kalypso hält Odysseus sieben Jahre lang auf der Insel Ogygia zurück und bietet ihm sogar Unsterblichkeit an, doch er sehnt sich nach Hause.",
        difficulty = 3,
        funFact = "Erst auf Geheiß des Zeus schickt Hermes die Nymphe Kalypso an, Odysseus ziehen zu lassen."
    ),

    // Question 5 — Homer: Odyssee — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie lautet der Name des einäugigen Riesen, den Odysseus in der Odyssee blendet?",
        answerA = "Polyphem",
        answerB = "Antiphates",
        answerC = "Laistrigone",
        answerD = "Kyklops Brontes",
        correctAnswer = 0,
        explanation = "Polyphem, Sohn des Meeresgottes Poseidon, sperrt Odysseus und seine Männer in seiner Höhle ein — Odysseus blendet ihn mit einem glühenden Pfahl.",
        difficulty = 3,
        funFact = "Odysseus nennt sich Polyphem gegenüber 'Niemand' (Outis), damit dieser seinem Vater Poseidon nicht sagen kann, wer ihn geblendet hat."
    ),

    // Question 6 — Homer: Odyssee — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was lässt Odysseus tun, damit er den Gesang der Sirenen hören kann, ohne zu sterben?",
        answerA = "Er stopft sich die Ohren mit Wachs",
        answerB = "Er lässt sich an den Mast fesseln",
        answerC = "Er trägt ein Amulett der Göttin Kirke",
        answerD = "Er singt laut gegen die Sirenen an",
        correctAnswer = 1,
        explanation = "Odysseus lässt sich an den Schiffsmast fesseln und seinen Männern die Ohren mit Wachs verstopfen — so kann er den Sirenengesang hören, ohne sein Schiff zu gefährden.",
        difficulty = 3,
        funFact = "Die Sirenen wurden in der Antike als Mischwesen aus Frau und Vogel dargestellt — erst im Mittelalter wurden daraus Meerjungfrauen."
    ),

    // Question 7 — Vergil: Aeneis — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Woran stirbt die karthagische Königin Dido in Vergils Aeneis?",
        answerA = "Sie ertrinkt im Meer",
        answerB = "Sie wird von Aeneas im Kampf getötet",
        answerC = "Sie stürzt sich in ein Schwert auf einem Scheiterhaufen",
        answerD = "Sie vergiftet sich mit einem Liebestrank",
        correctAnswer = 2,
        explanation = "Als Aeneas Karthago verlässt, lässt Dido einen Scheiterhaufen errichten und stürzt sich darauf in ihr Schwert — aus Schmerz und verletzter Ehre.",
        difficulty = 3,
        funFact = "Mit ihrem Tod schwört Dido Rache und legt symbolisch den Grundstein für die Punischen Kriege zwischen Rom und Karthago."
    ),

    // Question 8 — Vergil: Aeneis — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wer wird am Ende der Aeneis von Aeneas getötet?",
        answerA = "König Latinus",
        answerB = "Camilla, die Kriegerin",
        answerC = "Mezentius, der Tyrann",
        answerD = "Turnus, Fürst der Rutuler",
        correctAnswer = 3,
        explanation = "Das Epos endet mit dem Zweikampf zwischen Aeneas und Turnus — Aeneas tötet Turnus, der Lavinia versprochen war, und sichert so seine Herrschaft in Latium.",
        difficulty = 3,
        funFact = "Vergil starb, bevor er die Aeneis fertigstellen konnte — er wollte das Werk ursprünglich vernichten lassen, doch Kaiser Augustus bewahrte es."
    ),

    // Question 9 — Vergil: Aeneis — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welcher antike Dichter führt Aeneas in der Unterwelt (Buch 6 der Aeneis)?",
        answerA = "Die Sibylle von Cumae",
        answerB = "Vergil selbst",
        answerC = "Der Seher Teiresias",
        answerD = "Merkur, der Götterbote",
        correctAnswer = 0,
        explanation = "Die Sibylle von Cumae führt Aeneas durch die Unterwelt, wo er seinen verstorbenen Vater Anchises trifft, der ihm Roms zukünftige Größe prophezeit.",
        difficulty = 3,
        funFact = "Vergil selbst wird später in Dantes Göttlicher Komödie als Führer durch die Unterwelt gewählt — als Hommage an diese Szene aus der Aeneis."
    ),

    // Question 10 — Ovid: Metamorphosen — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "In welches Tier wird Narcissus laut Ovids Metamorphosen verwandelt?",
        answerA = "In einen weißen Schwan",
        answerB = "In eine Narzissenblume",
        answerC = "In einen Felsen am Wasser",
        answerD = "In einen Spiegelkarpfen",
        correctAnswer = 1,
        explanation = "Narcissus verliebt sich in sein eigenes Spiegelbild im Wasser und verzehrt sich vor Sehnsucht — nach seinem Tod verwandelt er sich in die Narzissenblume.",
        difficulty = 3,
        funFact = "Echo, die sich in Narcissus verliebt hatte, wird zur Strafe zur bloßen Stimme — sie kann nur noch die letzten Worte wiederholen, die sie hört."
    ),

    // Question 11 — Ovid: Metamorphosen — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was betet Pygmalion in Ovids Metamorphosen bei Venus' Fest?",
        answerA = "Um Ruhm als größter Bildhauer Zyperns",
        answerB = "Um die Liebe der Priesterin Galatea",
        answerC = "Um eine Frau, die seiner Elfenbeinstatue ähnelt",
        answerD = "Um die Fähigkeit, Steine in Gold zu verwandeln",
        correctAnswer = 2,
        explanation = "Pygmalion, der sich in seine eigene Elfenbeinstatue verliebt hat, bittet Venus um eine Frau wie seine Statue — als er heimkommt, ist die Statue lebendig geworden.",
        difficulty = 3,
        funFact = "Ovid nennt die Statue nie beim Namen — den Namen Galatea gab ihr erst die spätere Rezeption."
    ),

    // Question 12 — Ovid: Metamorphosen — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Mit welcher Bedingung darf Orpheus seine tote Gattin Eurydike aus dem Totenreich mitführen?",
        answerA = "Er muss der Göttin Persephone ein Opfer bringen",
        answerB = "Er muss den dreiköpfigen Hund Kerberos bezwingen",
        answerC = "Er muss schweigend aus der Unterwelt gehen",
        answerD = "Er darf sich auf dem Weg zurück nicht nach ihr umschauen",
        correctAnswer = 3,
        explanation = "Orpheus darf Eurydike zurückbringen, solange er sich nicht umschaut — kurz vor dem Ausgang dreht er sich um, und sie wird sofort wieder in die Unterwelt zurückgezogen.",
        difficulty = 3,
        funFact = "Orpheus stirbt schließlich, als thrakische Mänaden ihn zerreißen — sein Kopf soll noch singend auf der Insel Lesbos angespült worden sein."
    ),

    // Question 13 — Sophokles: Ödipus Rex — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was tut Ödipus, als er erfährt, dass er unwissentlich seinen Vater getötet und seine Mutter geheiratet hat?",
        answerA = "Er sticht sich die Augen aus",
        answerB = "Er wirft sich von einem Fels",
        answerC = "Er verbannt sich selbst nach Theben",
        answerD = "Er tötet sich mit dem Schwert",
        correctAnswer = 0,
        explanation = "Ödipus sticht sich mit den goldenen Spangen seiner Mutter Iokaste die Augen aus — ein Symbol für die Blindheit, die er trotz physischer Sehkraft hatte.",
        difficulty = 3,
        funFact = "Sophokles benutzte dieses Motiv, um eine tiefe Wahrheit zu zeigen: Der sehende Ödipus war blind für die Wahrheit, während der blinde Seher Teiresias sah."
    ),

    // Question 14 — Sophokles: Ödipus Rex — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welcher blinde Seher prophezeit Ödipus die Wahrheit über seine Herkunft?",
        answerA = "Kalchas",
        answerB = "Teiresias",
        answerC = "Amphiaraos",
        answerD = "Bakis",
        correctAnswer = 1,
        explanation = "Der blinde Seher Teiresias offenbart Ödipus, dass er selbst der Mörder seines Vaters Laios ist — Ödipus glaubt ihm zunächst nicht.",
        difficulty = 3,
        funFact = "Teiresias taucht in mehreren griechischen Tragödien auf und war laut Mythos als Strafe für sein Wissen sieben Menschenleben lang blind."
    ),

    // Question 15 — Sophokles: Antigone — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Warum verbietet König Kreon die Bestattung von Polyneikes in Sophokles' Antigone?",
        answerA = "Weil Polyneikes ein Feind der Götter war",
        answerB = "Weil Polyneikes seine Schwester Antigone beleidigt hatte",
        answerC = "Weil Polyneikes als Verräter mit einem Fremdheer gegen Theben kämpfte",
        answerD = "Weil das Orakel von Delphi es verboten hatte",
        correctAnswer = 2,
        explanation = "Polyneikes griff mit einem Fremdheer seine Heimatstadt Theben an — Kreon erklärt ihn zum Staatsfeind und verbietet seine Bestattung als politische Strafe.",
        difficulty = 3,
        funFact = "Die unbestattete Seele galt in der Antike als ruhelos — Antigones Widerstand gegen Kreon ist ein Konflikt zwischen göttlichem und menschlichem Gesetz."
    ),

    // Question 16 — Sophokles: Antigone — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wie endet Antigone in Sophokles' gleichnamiger Tragödie?",
        answerA = "Sie flieht mit ihrem Verlobten Haimon nach Argos",
        answerB = "Sie wird von Kreon begnadigt und freigelassen",
        answerC = "Sie wird vor dem Volk von Theben gesteinigt",
        answerD = "Sie erhängt sich in der Grabkammer, in die Kreon sie eingesperrt hat",
        correctAnswer = 3,
        explanation = "Kreon lässt Antigone lebendig in eine Felsenkammer einmauern — dort erhängt sie sich, woraufhin ihr Verlobter Haimon und dann dessen Mutter Eurydike sich ebenfalls das Leben nehmen.",
        difficulty = 3,
        funFact = "Haimon ist Kreons Sohn und Antigones Verlobter — Kreons starres Festhalten an seiner Entscheidung kostet ihm also Sohn, Schwiegertochter und schließlich auch seine Frau."
    ),

    // Question 17 — Euripides: Medea — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie rächt sich Medea an Jason, der sie für eine andere Frau verlässt?",
        answerA = "Sie tötet ihre eigenen Kinder und Jasons neue Braut",
        answerB = "Sie verwandelt Jason mit einem Zaubertrank in einen Stier",
        answerC = "Sie verbrennt Jasons Schiff Argo",
        answerD = "Sie ruft die Göttin Hekate herbei, um Jason zu verfluchen",
        correctAnswer = 0,
        explanation = "Medea schickt Jasons neuer Braut Kreusa ein vergiftetes Gewand und tötet danach ihre eigenen gemeinsamen Kinder, um Jason den größten Schmerz zuzufügen.",
        difficulty = 3,
        funFact = "Euripides war der erste Dichter, der Medea als Kindsmörderin darstellte — ältere Versionen des Mythos sahen die Korinther als Schuldige am Tod der Kinder."
    ),

    // Question 18 — Euripides: Medea — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welcher König gewährt Medea nach ihrer Tat Zuflucht in seiner Stadt?",
        answerA = "König Laios von Theben",
        answerB = "König Aigeus von Athen",
        answerC = "König Pelias von Iolkos",
        answerD = "König Kreon von Korinth",
        correctAnswer = 1,
        explanation = "König Aigeus von Athen verspricht Medea Schutz, nachdem sie ihm mit ihrer Magie Hilfe bei seinem Kinderwunsch in Aussicht stellt.",
        difficulty = 3,
        funFact = "Medea flieht nach der Tat auf einem von Drachen gezogenen Wagen durch die Luft — ein drastisches Bühnenbild, das Euripides als spectaculum einsetzte."
    ),

    // Question 19 — Aristophanes: Komödie — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "In welchem Stück des Aristophanes streiken die Frauen Athens und Spartas mit Liebesentzug für den Frieden?",
        answerA = "Die Wolken",
        answerB = "Die Frösche",
        answerC = "Lysistrate",
        answerD = "Die Vögel",
        correctAnswer = 2,
        explanation = "In Lysistrate (411 v. Chr.) organisiert die Athenerin Lysistrate einen Sexstreik der Frauen beider Seiten, um den Peloponnesischen Krieg zu beenden.",
        difficulty = 3,
        funFact = "Aristophanes schrieb das Stück mitten im Peloponnesischen Krieg — eine der mutigsten politischen Satiren der Antike."
    ),

    // Question 20 — Nibelungenlied — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Aus wie vielen Aventiuren (Kapiteln) besteht das Nibelungenlied?",
        answerA = "24",
        answerB = "32",
        answerC = "44",
        answerD = "39",
        correctAnswer = 3,
        explanation = "Das Nibelungenlied ist in 39 Aventiuren gegliedert — der Begriff stammt aus dem Altfranzösischen und bedeutet etwa 'Ereignis' oder 'Abenteuer'.",
        difficulty = 3,
        funFact = "Das Nibelungenlied entstand um 1200 in mittelhochdeutscher Sprache und wurde von einem unbekannten Verfasser niedergeschrieben."
    ),

    // Question 21 — Nibelungenlied — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "An welcher Körperstelle ist Siegfried verwundbar, weil dort beim Bad im Drachenblut ein Lindenblatt lag?",
        answerA = "Zwischen den Schulterblättern auf dem Rücken",
        answerB = "An der linken Ferse",
        answerC = "An der rechten Schläfe",
        answerD = "Auf der Brust über dem Herzen",
        correctAnswer = 0,
        explanation = "Als Siegfried im Blut des erschlagenen Drachen badete, bedeckte ein Lindenblatt eine Stelle zwischen den Schulterblättern — genau dort trifft ihn Hagens Speer.",
        difficulty = 3,
        funFact = "Kriemhild verrät Hagen Siegfrieds Schwachstelle, indem sie vermeintlich zu seinem Schutz ein Kreuz auf die verwundbare Stelle seiner Kleidung näht."
    ),

    // Question 22 — Nibelungenlied — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wo und bei welcher Gelegenheit wird Siegfried von Hagen ermordet?",
        answerA = "Im Zweikampf vor der Burg Worms",
        answerB = "Auf der Jagd im Odenwald, als er sich an einer Quelle bückt",
        answerC = "Im Schlaf in seiner Kammer",
        answerD = "Beim Wettschwimmen im Rhein",
        correctAnswer = 1,
        explanation = "Während einer Jagd im Odenwald bückt sich Siegfried, um aus einer Quelle zu trinken — in diesem Moment durchbohrt Hagen ihn von hinten mit dem Speer.",
        difficulty = 3,
        funFact = "Die Mordstelle wird im Volksglauben mit der Quelle bei Gras-Ellenbach im Odenwald gleichgesetzt — dort steht heute ein Siegfried-Denkmal."
    ),

    // Question 23 — Nibelungenlied — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wie heißt der Schatz der Nibelungen, den Hagen in den Rhein versenkt?",
        answerA = "Das Rheingold der Alben",
        answerB = "Das Hortgold der Burgundenkönige",
        answerC = "Der Nibelungenhort",
        answerD = "Das Drachengold von Fáfnir",
        correctAnswer = 2,
        explanation = "Der Nibelungenhort ist ein sagenhafter Schatz aus Gold und Edelsteinen, den Hagen in den Rhein versenkt, damit Kriemhild ihn nicht zur Finanzierung ihrer Rache benutzen kann.",
        difficulty = 3,
        funFact = "Hagen verrät den Versenkungsort des Hortes niemandem — er nimmt das Geheimnis mit ins Grab, was Kriemhild wütend macht und zu seiner Enthauptung führt."
    ),

    // Question 24 — Wolfram von Eschenbach: Parzival — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wie heißt die Gralsburg in Wolframs von Eschenbachs Parzival?",
        answerA = "Camelot",
        answerB = "Avalon",
        answerC = "Klingsor",
        answerD = "Munsalvaesche",
        correctAnswer = 3,
        explanation = "Munsalvaesche ('Wilder Berg') ist die Gralsburg, auf der der kranke Gralskönig Anfortas residiert und die nur von den Auserwählten gefunden werden kann.",
        difficulty = 3,
        funFact = "Wolfram von Eschenbachs Gral ist im Gegensatz zur französischen Vorlage kein Kelch, sondern ein magischer Stein, genannt 'lapsit exillis'."
    ),

    // Question 25 — Wolfram von Eschenbach: Parzival — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welche entscheidende Frage versäumt Parzival beim ersten Besuch auf der Gralsburg zu stellen?",
        answerA = "Was fehlt dir / Was quält dich, Onkel?",
        answerB = "Darf ich die Nacht auf der Burg verbringen?",
        answerC = "Wer trägt den Heiligen Gral?",
        answerD = "Wer ist dein Feind, König Anfortas?",
        correctAnswer = 0,
        explanation = "Parzival sieht den leidenden Anfortas, stellt aber aus falsch verstandenem höfischen Anstand die erlösende Mitleidsfrage nicht — was ihn jahrelang in Schande stürzt.",
        difficulty = 3,
        funFact = "Parzival muss diese Lektion erst lernen, dass wahres Rittertum nicht blind Regeln befolgt, sondern aus echtem Mitgefühl handelt."
    ),

    // Question 26 — Wolfram von Eschenbach: Parzival — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wer ist der Einsiedler-Onkel, der Parzival auf seinem zweiten Weg zur Gralsburg spirituell führt?",
        answerA = "Gurnemanz von Graharz",
        answerB = "Trevrizent",
        answerC = "Gawan",
        answerD = "Kyot der Provenzale",
        correctAnswer = 1,
        explanation = "Der Einsiedler Trevrizent, Bruder des Gralskönigs Anfortas, unterrichtet Parzival in Glauben, Reue und ritterlicher Ethik — er ist der eigentliche geistliche Führer des Helden.",
        difficulty = 3,
        funFact = "Trevrizent enthüllt Parzival auch, dass Anfortas sein Onkel ist — Parzival entstammt also selbst der Gralsfamilie."
    ),

    // Question 27 — Walther von der Vogelweide — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welches Lied Walthers von der Vogelweide beginnt mit den Worten 'Unter der Linden'?",
        answerA = "Der Leich",
        answerB = "Herzeliebez frouwelîn",
        answerC = "Under der linden an der heide",
        answerD = "Ir sult sprechen willekomen",
        correctAnswer = 2,
        explanation = "Das bekannteste Minnelied Walthers beginnt mit 'Under der linden an der heide' und schildert ein idyllisches Stelldichein zweier Liebender auf einer Wiese.",
        difficulty = 3,
        funFact = "Walther von der Vogelweide gilt als der bedeutendste deutschsprachige Lyriker des Mittelalters — seine genauen Lebensdaten (um 1170–1230) sind bis heute nicht gesichert."
    ),

    // Question 28 — Walther von der Vogelweide — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welche politische Dichtgattung pflegte Walther von der Vogelweide neben dem Minnesang?",
        answerA = "Das Märe",
        answerB = "Die Legende",
        answerC = "Das Tagelied",
        answerD = "Der Spruch (Spruchdichtung)",
        correctAnswer = 3,
        explanation = "Walther war neben dem Minnesang auch für seine politischen Sprüche bekannt, mit denen er Stellung zu Thronstreitigkeiten und dem Verhältnis von Kaiser und Papst nahm.",
        difficulty = 3,
        funFact = "Walther unterstützte in seinen Sprüchen verschiedene Seiten im Thronstreit zwischen Philipp von Schwaben und Otto IV. — je nachdem, wer ihn besser entlohnte."
    ),

    // Question 29 — Dante: Göttliche Komödie — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welcher antike Dichter führt Dante durch Hölle und Läuterungsberg in der Göttlichen Komödie?",
        answerA = "Vergil",
        answerB = "Homer",
        answerC = "Ovid",
        answerD = "Horaz",
        correctAnswer = 0,
        explanation = "Vergil, der Verfasser der Aeneis, führt Dante als Symbol der menschlichen Vernunft durch die Hölle (Inferno) und den Läuterungsberg (Purgatorio).",
        difficulty = 3,
        funFact = "Vergil kann Dante nicht ins Paradies begleiten, da er als Heide nie getauft wurde — die Führung übernimmt dort Dantes idealisierte Geliebte Beatrice."
    ),

    // Question 30 — Dante: Göttliche Komödie — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welche drei Sünder werden im tiefsten Kreis der Danteschen Hölle (Judecca) von Luzifer gekaut?",
        answerA = "Pilatus, Herodes und Kain",
        answerB = "Judas Iskariot, Brutus und Cassius",
        answerC = "Nero, Kaligula und Tiberius",
        answerD = "Faust, Simon Magus und Giuda",
        correctAnswer = 1,
        explanation = "Im tiefsten Kreis der Hölle kaut Luzifer mit seinen drei Mäulern an Judas (Verrat an Christus), Brutus und Cassius (Verrat an Cäsar) — für Dante die schlimmsten Verräter.",
        difficulty = 3,
        funFact = "Luzifers drei Gesichter parodieren die Heilige Dreifaltigkeit: Statt dreier göttlicher Tugenden verkörpern sie drei Arten der Verwerflichkeit."
    ),

    // Question 31 — Dante: Göttliche Komödie — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wie viele Gesänge hat der gesamte Inferno-Teil der Göttlichen Komödie?",
        answerA = "33",
        answerB = "30",
        answerC = "34",
        answerD = "36",
        correctAnswer = 2,
        explanation = "Der Inferno hat 34 Gesänge (33 plus einen einleitenden Prolog-Gesang), während Purgatorio und Paradiso je 33 Gesänge haben — zusammen 100 Gesänge.",
        difficulty = 3,
        funFact = "Die Zahl 100 ist für Dante symbolisch: das Quadrat von 10, selbst das Quadrat der vollkommenen Zahl 3 (Dreifaltigkeit) plus 1 (Gott)."
    ),

    // Question 32 — Dante: Göttliche Komödie — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "In welchem Kreis der Hölle bestraft Dante die Wollüstigen — und was ist ihre Strafe?",
        answerA = "Im siebten Kreis — sie treiben in siedendem Blut",
        answerB = "Im dritten Kreis — sie liegen im eiskalten Schlamm",
        answerC = "Im fünften Kreis — sie ertrinken in einem Sumpf",
        answerD = "Im zweiten Kreis — sie werden von einem ewigen Sturm umhergewirbelt",
        correctAnswer = 3,
        explanation = "Im zweiten Kreis der Hölle werden die Wollüstigen von einem nie endenden Sturm umhergetrieben — als Symbol ihrer Leidenschaft, die sie im Leben beherrschte.",
        difficulty = 3,
        funFact = "Hier trifft Dante auf Paolo und Francesca, das berühmteste Liebespaar der Komödie — Francesca erzählt, dass eine Lektüre von Lancelot und Guinevere sie verführt habe."
    ),

    // Question 33 — Boccaccio: Decameron — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie viele Novellen umfasst Boccaccios Decameron und über wie viele Tage verteilen sie sich?",
        answerA = "100 Novellen, verteilt auf 10 Tage mit je 10 Erzählern",
        answerB = "72 Novellen, verteilt auf 9 Tage",
        answerC = "50 Novellen, verteilt auf 5 Tage",
        answerD = "120 Novellen, verteilt auf 12 Tage",
        correctAnswer = 0,
        explanation = "Das Decameron besteht aus 100 Novellen, erzählt von 10 jungen Florentinern (7 Frauen, 3 Männer), die während der Pest von 1348 aufs Land geflohen sind — je 10 pro Tag.",
        difficulty = 3,
        funFact = "Der Titel 'Decameron' bedeutet auf Griechisch 'Zehn-Tage-Werk' — eine Anspielung auf Hexameron-Werke über die sechs Schöpfungstage."
    ),

    // Question 34 — Boccaccio: Decameron — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welches historische Ereignis bildet den Rahmen für die Flucht der Erzählrunde im Decameron?",
        answerA = "Der Hundertjährige Krieg",
        answerB = "Die Pest (Schwarzer Tod) in Florenz 1348",
        answerC = "Die Invasion der Türken in Sizilien",
        answerD = "Das Große Schisma der Kirche",
        correctAnswer = 1,
        explanation = "Boccaccio schildert im Prolog die verheerende Pest von 1348 in Florenz und lässt seine zehn Erzähler aufs Land fliehen, wo sie sich die Zeit mit Geschichten vertreiben.",
        difficulty = 3,
        funFact = "Boccaccio erlebte die Pest in Florenz selbst mit — sein Vater starb daran. Die eindrucksvolle Schilderung der Epidemie im Prolog gilt als eines der wichtigsten historischen Dokumente der Pest."
    ),

    // Question 35 — Petrarca: Canzoniere — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "An wen sind die 366 Gedichte von Petrarcas Canzoniere hauptsächlich gerichtet?",
        answerA = "An seine Mäzenin Giovanna von Neapel",
        answerB = "An die Jungfrau Maria",
        answerC = "An die unerreichbare Laura",
        answerD = "An seinen Freund Boccaccio",
        correctAnswer = 2,
        explanation = "Die Gedichte des Canzoniere sind an die idealisierte, unerreichbare Laura gerichtet — eine Frau, die Petrarca 1327 in Avignon erblickte und die 1348 an der Pest starb.",
        difficulty = 3,
        funFact = "Ob Laura eine historische Person oder eine literarische Fiktion war, ist bis heute nicht eindeutig geklärt — Petrarcas Gedichte machten sie unsterblich."
    ),

    // Question 36 — Petrarca — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welchen lateinischen Text schrieb Petrarca, in dem er eine imaginäre Unterhaltung mit Augustinus führt?",
        answerA = "De viris illustribus",
        answerB = "Africa",
        answerC = "De remediis utriusque fortunae",
        answerD = "Secretum meum",
        correctAnswer = 3,
        explanation = "Im 'Secretum meum' führt Petrarca ein fiktives dreitägiges Gespräch mit dem Kirchenvater Augustinus über seine innere Zerrissenheit zwischen irdischer Liebe und spirituellem Heil.",
        difficulty = 3,
        funFact = "Petrarca hielt das Secretum zurück und wollte es nicht veröffentlichen — es gilt als sein persönlichstes Werk und beeinflusste die spätere Renaissance-Literatur stark."
    ),

    // Question 37 — Cervantes: Don Quijote — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie heißt der treue Knappe, der Don Quijote auf all seinen Abenteuern begleitet?",
        answerA = "Sancho Pansa",
        answerB = "Rocinante",
        answerC = "Dulcinea",
        answerD = "Cardenio",
        correctAnswer = 0,
        explanation = "Sancho Pansa, ein einfacher Bauer, begleitet den idealistischen Don Quijote als sein Knappe — er reitet auf einem Esel und steht mit seinen bodenständigen Sprüchen im Kontrast zu Don Quijotes Träumereien.",
        difficulty = 3,
        funFact = "Rocinante ist der Name von Don Quijotes Pferd — ein ausgezehrter alter Klepper, den Don Quijote für ein edles Streitross hält."
    ),

    // Question 38 — Cervantes: Don Quijote — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wofür hält Don Quijote die Windmühlen, gegen die er kämpft?",
        answerA = "Für verzauberte Burgen seiner Feinde",
        answerB = "Für riesige Ungeheuer (Riesen)",
        answerC = "Für feindliche Ritter in Eisenrüstungen",
        answerD = "Für mechanische Dämonen",
        correctAnswer = 1,
        explanation = "Don Quijote greift Windmühlen an, weil er sie für Riesen hält — eine der bekanntesten Szenen der Weltliteratur, die zum Symbol für das Kämpfen gegen eingebildete Feinde wurde.",
        difficulty = 3,
        funFact = "Der Ausdruck 'gegen Windmühlen kämpfen' geht auf diese Szene zurück und bedeutet heute: gegen eingebildete oder unlösbare Probleme ankämpfen."
    ),

    // Question 39 — Cervantes: Don Quijote — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "In welcher spanischen Region lebt Don Quijote, aus der er zu seinen Abenteuern aufbricht?",
        answerA = "Andalusien",
        answerB = "Kastilien",
        answerC = "La Mancha",
        answerD = "Aragonien",
        correctAnswer = 2,
        explanation = "Don Quijote lebt in La Mancha — einer kargen, flachen Hochebene im Zentrum Spaniens. Der vollständige Titel des Werkes lautet 'El ingenioso hidalgo Don Quijote de la Mancha'.",
        difficulty = 3,
        funFact = "Cervantes lässt absichtlich offen, in welchem genauen Dorf in La Mancha Don Quijote lebt — 'dessen Name mir nicht einfallen will', schreibt er berühmt im ersten Satz."
    ),

    // Question 40 — Shakespeare: Hamlet — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wer ist der Geist, der Hamlet in der ersten Szene auf dem Schloss Elsinore erscheint?",
        answerA = "Der ermordete Feldherr Fortinbras",
        answerB = "Hamlets Großvater, König Halvar",
        answerC = "Der Hofnarr Yorick",
        answerD = "Der Geist von Hamlets ermordetem Vater, König Hamlet",
        correctAnswer = 3,
        explanation = "Der Geist von Hamlets Vater erscheint auf der Burgmauer und enthüllt, dass er von Claudius, seinem Bruder, mit Gift getötet wurde — er fordert Hamlet zur Rache auf.",
        difficulty = 3,
        funFact = "Shakespeare lässt die Frage offen, ob der Geist wirklich der tote König ist oder ein Dämon, der Hamlet manipuliert — Hamlet selbst zweifelt mehrfach daran."
    ),

    // Question 41 — Shakespeare: Hamlet — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie heißt Ophelias Vater, der Hofrat in Shakespeares Hamlet?",
        answerA = "Polonius",
        answerB = "Osric",
        answerC = "Horatio",
        answerD = "Bernardo",
        correctAnswer = 0,
        explanation = "Polonius ist der geschwätzige Hofrat, Vater von Ophelia und Laertes — Hamlet tötet ihn versehentlich, als Polonius hinter einem Wandteppich lauscht.",
        difficulty = 3,
        funFact = "Polonius' Tod ist der Auslöser für Ophelias Wahnsinn und Laertes' Racheschwur — damit setzt Hamlet eine Kettenreaktion in Gang, die alle vernichtet."
    ),

    // Question 42 — Shakespeare: Macbeth — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was prophezeien die drei Hexen Macbeth zu Beginn von Shakespeares gleichnamigem Stück?",
        answerA = "Er werde König werden, aber keinen Sohn haben",
        answerB = "Er werde Thane von Cawdor werden und dann König von Schottland",
        answerC = "Er werde seinen besten Freund Banquo töten",
        answerD = "Er werde von einem Mann mit rotem Haar ermordet",
        correctAnswer = 1,
        explanation = "Die Hexen prophezeien Macbeth, er werde Thane von Cawdor und danach König werden — Banquo dagegen wird kein König sein, aber seine Nachkommen werden regieren.",
        difficulty = 3,
        funFact = "Dem Theaterstück 'Macbeth' haftet seit Jahrhunderten ein Fluch an — Schauspieler nennen es deswegen nur 'The Scottish Play' und vermeiden den echten Namen backstage."
    ),

    // Question 43 — Shakespeare: Macbeth — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Durch welche sprachliche Doppeldeutigkeit der Hexenprophezeiung fühlt sich Macbeth unbesiegbar?",
        answerA = "Er werde siegen, solange er den Dolch des Königs trägt",
        answerB = "Er falle nicht, solange drei Könige ihn stützen",
        answerC = "Kein von einer Frau geborener Mann kann ihn töten",
        answerD = "Er sei sicher, solange Birnam Wood nicht nach Dunsinane kommt",
        correctAnswer = 2,
        explanation = "Die Prophezeiung, kein 'von einer Frau Geborener' könne ihn töten, gibt Macbeth falsche Sicherheit — bis Macduff enthüllt, er sei per Kaiserschnitt geboren worden.",
        difficulty = 3,
        funFact = "Auch die zweite Prophezeiung ('bis Birnam Wood nach Dunsinane kommt') erfüllt sich: Macduffs Armee tarnt sich mit abgehauenen Ästen des Birnam-Waldes."
    ),

    // Question 44 — Shakespeare: Othello — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welches Objekt benutzt Iago in Othello als 'Beweisstück', um Othellos Eifersucht auf Cassio zu entfachen?",
        answerA = "Ein Gemälde von Desdemona",
        answerB = "Ein Liebesbrief in Cassios Schrift",
        answerC = "Cassios Ring mit Desdemonas Wappen",
        answerD = "Ein Taschentuch, das Othello Desdemona geschenkt hatte",
        correctAnswer = 3,
        explanation = "Iago manipuliert Rodrigo, das Taschentuch, das Othello seiner Frau Desdemona als erstes Liebesgeschenk gab, an sich zu bringen und es bei Cassio zu deponieren.",
        difficulty = 3,
        funFact = "Das Taschentuch ist eines der berühmtesten Requisiten der Theatergeschichte — ein scheinbar harmloses Objekt, das zum Auslöser einer Tragödie wird."
    ),

    // Question 45 — Shakespeare: Othello — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie lautet der Hauptvorwurf, den Iagos Manipulation bei Othello gegen Desdemona hervorruft?",
        answerA = "Untreue (Ehebruch) mit Cassio",
        answerB = "Verrat an der Republik Venedig",
        answerC = "Zauberei bei ihrer Heirat mit Othello",
        answerD = "Verschwörung mit dem türkischen Feind",
        correctAnswer = 0,
        explanation = "Iago überredet Othello, Desdemona habe ihn mit seinem Leutnant Cassio betrogen — Othello glaubt den gefälschten Beweisen und erdrosselt die unschuldige Desdemona.",
        difficulty = 3,
        funFact = "Iago nennt im Stück kein wirklich überzeugendes Motiv für seinen Hass auf Othello — dieser 'motivlose Bösewicht' gilt als eine der faszinierendsten Schurken-Figuren der Literaturgeschichte."
    ),

    // Question 46 — Homer: Ilias (Tiefer) — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welche Gottheit unterstützt die Trojaner aktiv im Trojanischen Krieg auf der Seite Hektors?",
        answerA = "Athene",
        answerB = "Apollon",
        answerC = "Poseidon",
        answerD = "Hephaistos",
        correctAnswer = 1,
        explanation = "Apollon schützt Hektor und die Trojaner, während Athene und Hera auf Seiten der Griechen kämpfen — die Götter greifen aktiv in die Schlachten ein.",
        difficulty = 3,
        funFact = "Apollon ist es auch, der Paris' Hand beim tödlichen Pfeilschuss auf Achilles' Ferse lenkt — der Tod des Achilles geschieht kurz nach dem Abschluss der Ilias."
    ),

    // Question 47 — Euripides: Weitere Werke — correct: C (2)
    Question(
        categoryId = 10,
        questionText = "In welchem Stück des Euripides opfert Agamemnon seine Tochter für günstige Seewinde?",
        answerA = "Die Troerinnen",
        answerB = "Hekabe",
        answerC = "Iphigenie in Aulis",
        answerD = "Elektra",
        correctAnswer = 2,
        explanation = "In 'Iphigenie in Aulis' lässt Agamemnon seine Tochter Iphigenie opfern, um die Göttin Artemis zu besänftigen und den Schiffen der Griechen Fahrtwind nach Troja zu verschaffen.",
        difficulty = 3,
        funFact = "In Euripides' Version wird Iphigenie in letzter Sekunde durch Artemis gerettet und durch eine Hindin ersetzt — in anderen Versionen stirbt sie wirklich."
    ),

    // Question 48 — Vergil: Georgica — correct: D (3)
    Question(
        categoryId = 10,
        questionText = "In welchem Werk Vergils findet sich die berühmte Episode über Orpheus und Eurydike?",
        answerA = "In der Aeneis, Buch 6",
        answerB = "In den Eclogae (Hirtengedichte)",
        answerC = "Im Culex (Mücke)",
        answerD = "In den Georgica (Buch 4)",
        correctAnswer = 3,
        explanation = "Die Orpheus-Eurydike-Episode steht am Ende von Vergils Lehrgedicht über den Landbau, den Georgica — im vierten Buch, das sich mit der Bienenzucht befasst.",
        difficulty = 3,
        funFact = "Vergil verknüpft die Orpheus-Geschichte mit dem Imker Aristaeus, der durch sein Begehren nach Eurydike deren Tod mitverursacht und als Strafe seine Bienen verliert."
    ),

    // Question 49 — Ovid: Weitere Details — correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Mit welcher Verwandlung enden Ovids Metamorphosen — wessen Apotheose schließt das Werk ab?",
        answerA = "Die Vergöttlichung Caesars und ein Lob auf Augustus",
        answerB = "Die Verwandlung Ovids selbst in einen Lorbeerstrauch",
        answerC = "Die Verwandlung Roms in die ewige Stadt",
        answerD = "Die Apotheose des Herkules",
        correctAnswer = 0,
        explanation = "Das 15. Buch endet mit der Apotheose Caesars (Verwandlung in einen Stern) und einer Huldigung an Kaiser Augustus als Nachfolger — eine politische Geste Ovids an den Herrscher.",
        difficulty = 3,
        funFact = "Trotz dieser Huldigung wurde Ovid von Augustus im Jahr 8 n. Chr. nach Tomis am Schwarzen Meer verbannt — warum genau, ist bis heute nicht völlig geklärt."
    ),

    // Question 50 — Dante: Paradiso — correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wer führt Dante durch das Paradiso (das Paradies), nachdem Vergil ihn verlassen hat?",
        answerA = "Der Erzengel Gabriel",
        answerB = "Beatrice",
        answerC = "Der heilige Bernhard von Clairvaux",
        answerD = "Die Jungfrau Maria",
        correctAnswer = 1,
        explanation = "Beatrice, Dantes idealisierte Jugendliebe, die jung gestorben ist, übernimmt von Vergil die Führung durch die neun Himmelssphären des Paradieses.",
        difficulty = 3,
        funFact = "Am Ende des Paradisos übergibt Beatrice die Führung an den heiligen Bernhard von Clairvaux, der Dante zur Schau Gottes begleitet — dem höchsten Punkt der Reise."
    )

)
