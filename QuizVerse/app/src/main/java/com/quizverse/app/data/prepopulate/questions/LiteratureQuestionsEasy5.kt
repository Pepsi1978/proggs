package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun literatureQuestionsEasy5(): List<Question> = listOf(

    // ── EASY (difficulty = 1) ── Literary Basics, Genres & Famous Quotes ──────

    Question(
        categoryId = 10,
        questionText = "Was ist ein Roman?",
        answerA = "Ein kurzes Gedicht mit Reimen",
        answerB = "Ein langes Bühnenstück in Versen",
        answerC = "Ein längeres erzählendes Werk in Prosa",
        answerD = "Eine kurze Geschichte ohne Hauptfigur",
        correctAnswer = 2,
        explanation = "Ein Roman ist ein längeres erzählendes Prosawerk, das eine Geschichte mit Figuren, Handlung und Schauplätzen erzählt.",
        difficulty = 1,
        funFact = "Das Wort 'Roman' kommt vom mittelhochdeutschen 'roman', was ursprünglich einfach 'in romanischer Sprache' bedeutete."
    ),

    Question(
        categoryId = 10,
        questionText = "Aus welchem Werk stammt das berühmte Zitat 'Sein oder Nichtsein, das ist hier die Frage'?",
        answerA = "Faust von Goethe",
        answerB = "Hamlet von Shakespeare",
        answerC = "Don Quijote von Cervantes",
        answerD = "Die Leiden des jungen Werthers von Goethe",
        correctAnswer = 1,
        explanation = "Dieses Zitat stammt aus Shakespeares Tragödie 'Hamlet' (ca. 1600). Es ist eines der bekanntesten Zitate der Weltliteratur.",
        difficulty = 1,
        funFact = "Im englischen Original lautet das Zitat: 'To be, or not to be, that is the question.'"
    ),

    Question(
        categoryId = 10,
        questionText = "Was versteht man unter einer Fabel?",
        answerA = "Ein langes Heldenepos über Götter und Krieger",
        answerB = "Eine kurze Lehrgeschichte, in der meist Tiere menschliche Eigenschaften zeigen",
        answerC = "Ein Liebeslied in mehreren Strophen",
        answerD = "Ein historischer Bericht über wahre Ereignisse",
        correctAnswer = 1,
        explanation = "Eine Fabel ist eine kurze Lehrerzählung, in der oft Tiere als Träger menschlicher Eigenschaften auftreten und eine Moral vermitteln.",
        difficulty = 1,
        funFact = "Äsop, ein griechischer Sklave aus dem 6. Jahrhundert v. Chr., gilt als Erfinder der Fabel."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist der Protagonist einer Geschichte?",
        answerA = "Der Autor des Buches",
        answerB = "Die Hauptfigur oder der Held der Handlung",
        answerC = "Der Bösewicht in der Geschichte",
        answerD = "Der Erzähler, der die Geschichte berichtet",
        correctAnswer = 1,
        explanation = "Der Protagonist ist die Hauptfigur einer Geschichte, um die sich die Handlung dreht. Das Wort stammt aus dem Griechischen: 'protos' (erster) + 'agonistes' (Kämpfer).",
        difficulty = 1,
        funFact = "In manchen Geschichten kann es auch mehrere Protagonisten geben, zum Beispiel in Romanen mit mehreren Handlungssträngen."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist eine Metapher?",
        answerA = "Ein Satzzeichen am Ende eines Ausrufs",
        answerB = "Eine wörtliche Beschreibung eines Gegenstands",
        answerC = "Ein sprachliches Bild, das einen Begriff durch einen anderen ersetzt",
        answerD = "Eine Wiederholung desselben Wortes am Satzanfang",
        correctAnswer = 2,
        explanation = "Eine Metapher ist ein sprachliches Bild, bei dem ein Wort in übertragener Bedeutung verwendet wird, z.B. 'Das Leben ist eine Reise'.",
        difficulty = 1,
        funFact = "Metaphern sind so alltäglich, dass wir sie oft gar nicht bemerken – 'Zeit ist Geld' ist zum Beispiel eine sehr verbreitete Metapher."
    ),

    Question(
        categoryId = 10,
        questionText = "Welches Zitat stammt aus Goethes 'Faust'?",
        answerA = "'Alle Tiere sind gleich, aber manche sind gleicher.'",
        answerB = "'Ich bin der Geist, der stets verneint.'",
        answerC = "'Es war einmal ein König, der hatte drei Söhne.'",
        answerD = "'Zwei Seelen leben, ach, in meiner Brust.' – Nein, das ist die falsche Antwort",
        correctAnswer = 1,
        explanation = "'Ich bin der Geist, der stets verneint' sagt der Teufel Mephistopheles in Goethes 'Faust I'. Es beschreibt seinen nihilistischen Charakter.",
        difficulty = 1,
        funFact = "Das erste der vier Optionen stammt aus George Orwells 'Farm der Tiere' (1945)."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Drama?",
        answerA = "Eine Geschichte, die ausschließlich in Briefen erzählt wird",
        answerB = "Ein Werk, das für die Aufführung auf einer Bühne geschrieben wurde",
        answerC = "Ein Gedichtband mit mehr als 100 Seiten",
        answerD = "Eine Sammlung von Kurzgeschichten eines Autors",
        correctAnswer = 1,
        explanation = "Ein Drama ist ein literarisches Werk, das für die Aufführung im Theater geschrieben wurde und meist aus Dialogen besteht.",
        difficulty = 1,
        funFact = "Das Wort 'Drama' kommt aus dem Griechischen und bedeutet 'Handlung' oder 'Tat'."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist Prosa?",
        answerA = "Sprachliche Gestaltung ohne festes Versmaß oder Reimschema",
        answerB = "Ein Gedicht mit genau 14 Zeilen",
        answerC = "Eine Erzählung, die ausschließlich aus Dialogen besteht",
        answerD = "Ein Theaterstück in Versform",
        correctAnswer = 0,
        explanation = "Prosa bezeichnet die normale, nicht an Versmaß oder Reim gebundene Sprachform. Romane, Novellen und Kurzgeschichten sind Prosaformen.",
        difficulty = 1,
        funFact = "Im Gegensatz zur Lyrik hat Prosa keinen festen Rhythmus. Trotzdem kann auch Prosa sehr musikalisch und rhythmisch klingen."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Antagonist in einer Geschichte?",
        answerA = "Der zweite Erzähler in einem Roman mit doppelter Perspektive",
        answerB = "Die Figur, die dem Protagonisten entgegenwirkt oder ihn bekämpft",
        answerC = "Ein Nebendarsteller ohne wichtige Funktion",
        answerD = "Der Autor, der in seiner eigenen Geschichte auftaucht",
        correctAnswer = 1,
        explanation = "Der Antagonist ist der Gegenspieler des Protagonisten. Er steht seinen Zielen im Weg oder bekämpft ihn aktiv.",
        difficulty = 1,
        funFact = "Nicht jeder Antagonist muss böse sein – manchmal ist der Antagonist einfach jemand, dessen Ziele den Zielen des Protagonisten widersprechen."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Sonett?",
        answerA = "Ein Theaterstück mit genau fünf Akten",
        answerB = "Eine Kurzgeschichte mit überraschendem Ende",
        answerC = "Ein Gedicht mit 14 Zeilen, meist in zwei Quartette und zwei Terzette unterteilt",
        answerD = "Ein episches Langgedicht über Heldentaten",
        correctAnswer = 2,
        explanation = "Ein Sonett ist eine Gedichtform mit genau 14 Zeilen, die sich in vier Strophen gliedern: zwei Vierteiler (Quartette) und zwei Dreizeiler (Terzette).",
        difficulty = 1,
        funFact = "William Shakespeare schrieb 154 Sonette. Die Sonettform stammt ursprünglich aus Italien und wurde im 13. Jahrhundert entwickelt."
    ),

    Question(
        categoryId = 10,
        questionText = "Womit beginnen die meisten Märchen?",
        answerA = "'In einer dunklen und stürmischen Nacht...'",
        answerB = "'Es war einmal...'",
        answerC = "'Lange vor unserer Zeit...'",
        answerD = "'Man erzählt sich, dass...'",
        correctAnswer = 1,
        explanation = "'Es war einmal...' ist die klassische Einleitung von Märchen. Sie versetzt den Leser sofort in eine unbestimmte, weit zurückliegende Zeit.",
        difficulty = 1,
        funFact = "Im Englischen heißt es 'Once upon a time', im Französischen 'Il était une fois' – die Formel existiert in fast allen Sprachen der Welt."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist Lyrik?",
        answerA = "Sammelbegriff für alle erzählenden Texte in Prosaform",
        answerB = "Die Gattung der literarischen Werke für das Theater",
        answerC = "Die Gattung der Dichtung, zu der Gedichte gehören",
        answerD = "Eine besondere Form des wissenschaftlichen Schreibens",
        correctAnswer = 2,
        explanation = "Lyrik ist neben Epik und Dramatik eine der drei großen literarischen Gattungen. Sie umfasst alle Gedichtformen.",
        difficulty = 1,
        funFact = "Das Wort 'Lyrik' kommt von der griechischen Lyra – einem Musikinstrument. Gedichte wurden ursprünglich zur Musik gesungen."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist eine Novelle?",
        answerA = "Ein sehr kurzer Roman mit weniger als 100 Seiten",
        answerB = "Eine mittellange Prosaerzählung, die sich auf ein zentrales Ereignis konzentriert",
        answerC = "Ein Theaterstück in drei Akten",
        answerD = "Ein Gedicht, das eine Geschichte erzählt",
        correctAnswer = 1,
        explanation = "Eine Novelle ist kürzer als ein Roman, aber länger als eine Kurzgeschichte. Sie kreist meist um ein einzelnes unerhörtes Ereignis.",
        difficulty = 1,
        funFact = "Goethe definierte die Novelle als 'eine sich ereignete unerhörte Begebenheit'. Berühmte Beispiele: Storms 'Der Schimmelreiter', Kleists 'Michael Kohlhaas'."
    ),

    Question(
        categoryId = 10,
        questionText = "Was versteht man unter einem Reim?",
        answerA = "Eine Wiederholung desselben Buchstaben am Wortanfang",
        answerB = "Der Gleichklang von Wortenden in einem Gedicht",
        answerC = "Eine besonders lange und komplizierte Metapher",
        answerD = "Ein Abschnitt in einem Gedicht",
        correctAnswer = 1,
        explanation = "Ein Reim ist der Gleichklang von Wörtern ab dem letzten betonten Vokal, z.B. 'Haus' und 'Maus' oder 'Licht' und 'Gedicht'.",
        difficulty = 1,
        funFact = "Nicht alle Gedichte reimen sich – viele moderne Gedichte sind Reimgedichte, aber auch reimlose (sogenannte 'freie Verse') sind sehr verbreitet."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist eine Strophe in einem Gedicht?",
        answerA = "Ein einzelner Buchstabe in einem Akrostichon",
        answerB = "Die Zusammenfassung am Ende eines Gedichts",
        answerC = "Eine Gruppe von Versen, die einen Abschnitt des Gedichts bildet",
        answerD = "Das Reimschema des gesamten Gedichts",
        correctAnswer = 2,
        explanation = "Eine Strophe ist ein Abschnitt eines Gedichts, der mehrere Verse (Zeilen) umfasst. Sie ist mit einer Strophe in einem Lied vergleichbar.",
        difficulty = 1,
        funFact = "Das Wort 'Strophe' kommt aus dem Griechischen und bedeutet 'Wendung' oder 'Drehung' – eine Anspielung auf die Bewegungen des antiken Chores."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Vers in einem Gedicht?",
        answerA = "Das gesamte Gedicht in seiner fertigen Form",
        answerB = "Eine einzelne Zeile in einem Gedicht",
        answerC = "Die Moral oder Lehre am Ende",
        answerD = "Ein Gedicht, das keine Reime hat",
        correctAnswer = 1,
        explanation = "Ein Vers ist eine einzelne Zeile in einem Gedicht. Mehrere Verse bilden zusammen eine Strophe.",
        difficulty = 1,
        funFact = "Das lateinische Wort 'versus' bedeutet 'Umkehrung' oder 'Wendung' – weil man beim Schreiben am Ende einer Zeile umkehrt."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist Epik?",
        answerA = "Die Gattung erzählender Literatur, also Romane, Novellen, Kurzgeschichten",
        answerB = "Die Gattung der Bühnenstücke und Theatertexte",
        answerC = "Ein Begriff für besonders große und wichtige Gedichte",
        answerD = "Die Wissenschaft von der Literaturgeschichte",
        correctAnswer = 0,
        explanation = "Epik ist neben Lyrik und Dramatik eine der drei Grundgattungen der Literatur. Sie umfasst alle erzählenden Texte: Romane, Novellen, Kurzgeschichten, Märchen.",
        difficulty = 1,
        funFact = "Der Begriff kommt vom griechischen 'epos' (Wort, Erzählung). Das älteste bekannte epische Werk ist das Gilgamesch-Epos aus dem 3. Jahrtausend v. Chr."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist Dramatik?",
        answerA = "Besonders spannend geschriebene Romane",
        answerB = "Die Gattung der für die Bühne bestimmten Texte",
        answerC = "Gedichte, die Geschichten erzählen",
        answerD = "Erzählungen, die ausschließlich dramatische Ereignisse schildern",
        correctAnswer = 1,
        explanation = "Dramatik ist die dritte Grundgattung der Literatur neben Lyrik und Epik. Sie umfasst alle für die Bühnenaufführung bestimmten Werke: Tragödie, Komödie, Theaterstück.",
        difficulty = 1,
        funFact = "Das antike griechische Theater unterschied bereits zwischen Tragödie (ernstes Drama) und Komödie (heiteres Drama)."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Monolog?",
        answerA = "Ein Gespräch zwischen zwei Figuren auf der Bühne",
        answerB = "Eine längere Rede einer einzelnen Person, ohne dass andere antworten",
        answerC = "Ein Gedicht, das aus einer einzigen Strophe besteht",
        answerD = "Die abschließende Zusammenfassung eines Romans",
        correctAnswer = 1,
        explanation = "Ein Monolog ist eine längere Rede, die eine Figur allein hält – ohne Unterbrechung durch andere. Das Gegenteil ist der Dialog.",
        difficulty = 1,
        funFact = "Der bekannteste Monolog der Weltliteratur ist wohl Hamlets 'Sein oder Nichtsein'-Rede aus Shakespeares gleichnamigem Stück."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Dialog?",
        answerA = "Ein Gespräch zwischen zwei oder mehr Personen",
        answerB = "Eine Rede, die eine Person allein hält",
        answerC = "Ein Gedicht in Gesprächsform",
        answerD = "Die direkte Rede in einem Roman ohne Anführungszeichen",
        correctAnswer = 0,
        explanation = "Ein Dialog ist ein Gespräch zwischen zwei oder mehr Personen. In der Literatur ist er das Gegenstück zum Monolog.",
        difficulty = 1,
        funFact = "Das griechische Wort 'dialogos' bedeutet 'Gespräch'. Platon schrieb seine Philosophie in Dialogform – mit Sokrates als Hauptgesprächspartner."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist eine Ballade?",
        answerA = "Ein sehr kurzes Haiku mit drei Zeilen",
        answerB = "Eine wissenschaftliche Abhandlung über Musik",
        answerC = "Ein erzählendes Gedicht, das oft dramatische oder fantastische Ereignisse schildert",
        answerD = "Ein Theaterstück mit musikalischer Begleitung",
        correctAnswer = 2,
        explanation = "Eine Ballade ist eine Gedichtform, die eine Geschichte erzählt – oft mit dramatischen, mysteriösen oder übernatürlichen Elementen.",
        difficulty = 1,
        funFact = "Berühmte deutsche Balladen sind Goethes 'Erlkönig' und Schillers 'Die Bürgschaft'. Beide wurden später auch vertont."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist eine Biografie?",
        answerA = "Eine erfundene Geschichte über einen fiktiven Menschen",
        answerB = "Die Beschreibung des Lebens einer realen Person",
        answerC = "Ein wissenschaftlicher Text über Lebewesen",
        answerD = "Ein Lexikon mit Kurzeinträgen über bekannte Personen",
        correctAnswer = 1,
        explanation = "Eine Biografie ist die schriftliche Darstellung des Lebens einer realen Person, verfasst von jemand anderem. Schreibt man über das eigene Leben, heißt es Autobiografie.",
        difficulty = 1,
        funFact = "Das Wort 'Biografie' kommt aus dem Griechischen: 'bios' (Leben) und 'graphein' (schreiben). Eine Biografie über sich selbst heißt Autobiografie."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Epos?",
        answerA = "Ein kurzes lyrisches Gedicht über die Natur",
        answerB = "Ein langes erzählendes Gedicht über Heldentaten oder historische Ereignisse",
        answerC = "Ein moderner Begriff für E-Books",
        answerD = "Eine Sammlung von Kurzgedichten verschiedener Autoren",
        correctAnswer = 1,
        explanation = "Ein Epos ist ein langes erzählendes Gedicht, das Heldentaten, mythische Ereignisse oder historische Begebenheiten schildert. Die bekanntesten sind Homers 'Ilias' und 'Odyssee'.",
        difficulty = 1,
        funFact = "Das deutsche Nibelungenlied aus dem Mittelalter ist ein berühmtes deutschsprachiges Heldenepos – es handelt von Siegfried und den Nibelungen."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist eine Satire?",
        answerA = "Ein ernstes Drama über historische Ereignisse",
        answerB = "Ein literarisches Werk, das Missstände durch Spott und Ironie kritisiert",
        answerC = "Ein Gedicht, das ausschließlich Naturbilder verwendet",
        answerD = "Eine Heldensage aus dem Mittelalter",
        correctAnswer = 1,
        explanation = "Satire ist eine Kunstform, die gesellschaftliche Missstände, Personen oder Ereignisse mit Spott, Ironie oder Übertreibung kritisiert.",
        difficulty = 1,
        funFact = "Einer der bekanntesten Satiriker der deutschen Literatur ist Heinrich Heine. Im modernen Fernsehen sind Shows wie 'heute-show' oder 'Die Anstalt' typische Satire."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Taschenbuch?",
        answerA = "Ein sehr kleines Notizbuch für die Hosentasche",
        answerB = "Ein günstiges Buch im kleinen Format mit weichem Einband",
        answerC = "Ein digitales Buch zum Lesen auf dem Tablet",
        answerD = "Eine Anthologie von Kurzgeschichten",
        correctAnswer = 1,
        explanation = "Ein Taschenbuch (Paperback) ist eine günstige Buchausgabe mit weichem Einband und kleinerem Format. Es ist billiger als ein Hardcover.",
        difficulty = 1,
        funFact = "Das moderne Taschenbuch wurde 1935 von dem britischen Verleger Allen Lane erfunden. Die erste Taschenbuchreihe hieß 'Penguin Books'."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Hardcover-Buch?",
        answerA = "Ein Buch mit einem festen, stabilen Einband",
        answerB = "Ein Buch, das nur auf Bestellung erhältlich ist",
        answerC = "Ein besonders dickes Buch mit mehr als 500 Seiten",
        answerD = "Eine erste Druckauflage eines Buches",
        correctAnswer = 0,
        explanation = "Ein Hardcover-Buch (auch Gebundene Ausgabe) hat einen festen, steifen Einband. Es ist langlebiger und hochwertiger als ein Taschenbuch, aber auch teurer.",
        difficulty = 1,
        funFact = "Neue Bücher erscheinen oft zuerst als Hardcover, dann später als günstigeres Taschenbuch – manchmal mit 1–2 Jahren Abstand."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein E-Book?",
        answerA = "Ein sehr teures seltenes Buch (von 'Exklusiv-Buch')",
        answerB = "Ein digitales Buch, das auf elektronischen Geräten gelesen wird",
        answerC = "Ein Buch über Computer und Technologie",
        answerD = "Ein Buch mit eingebetteten Videos und Animationen",
        correctAnswer = 1,
        explanation = "Ein E-Book (Electronic Book) ist eine digitale Version eines Buches, die auf E-Readern, Tablets, Smartphones oder Computern gelesen werden kann.",
        difficulty = 1,
        funFact = "Das erste E-Book wurde 1971 erstellt, als Michael Hart die Unabhängigkeitserklärung der USA digitalisierte. Heute verkaufen sich E-Books in manchen Ländern besser als gedruckte Bücher."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Hörbuch?",
        answerA = "Ein Buch mit großen Buchstaben für Menschen mit Sehschwäche",
        answerB = "Ein Buch, das vorgelesen oder von Schauspielern eingesprochen wurde",
        answerC = "Ein Buch über die Geschichte des Hörens und der Musik",
        answerD = "Ein interaktives Buch mit Soundeffekten für Kinder",
        correctAnswer = 1,
        explanation = "Ein Hörbuch ist die eingesprochene Version eines Buches, die man anhören kann – ideal fürs Autofahren, Sport oder für Menschen, die nicht gut lesen können.",
        difficulty = 1,
        funFact = "Die ersten Hörbücher wurden in den 1930er Jahren für blinde Menschen produziert. Heute ist es eines der am schnellsten wachsenden Buch-Formate."
    ),

    Question(
        categoryId = 10,
        questionText = "Was macht ein Lektor in einem Verlag?",
        answerA = "Er druckt und bindet die Bücher",
        answerB = "Er gestaltet die Buchcover und das Layout",
        answerC = "Er liest Manuskripte, gibt Feedback und verbessert Texte",
        answerD = "Er verkauft die Bücher in Buchhandlungen",
        correctAnswer = 2,
        explanation = "Ein Lektor liest eingesandte Manuskripte, entscheidet über ihre Veröffentlichung und arbeitet mit dem Autor zusammen, um den Text zu verbessern.",
        difficulty = 1,
        funFact = "Viele berühmte Bücher wären ohne gute Lektoren nie so erfolgreich geworden. Max Perkins lektorierte u.a. Hemingway und Fitzgerald."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Verlag?",
        answerA = "Ein Ort, wo Bücher ausgeliehen werden können",
        answerB = "Ein Unternehmen, das Bücher herstellt und vertreibt",
        answerC = "Eine staatliche Behörde, die Bücher genehmigt",
        answerD = "Eine Autorenvereinigung für gegenseitigen Austausch",
        correctAnswer = 1,
        explanation = "Ein Verlag ist ein Unternehmen, das Bücher (oder andere Medien) veröffentlicht, druckt und vermarktet. Bekannte Verlage sind z.B. Suhrkamp, dtv oder Random House.",
        difficulty = 1,
        funFact = "Der älteste noch existierende Verlag der Welt ist Cambridge University Press, gegründet im Jahr 1534 in England."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist die Romantik als literarische Epoche?",
        answerA = "Eine Epoche, in der ausschließlich Liebesromane geschrieben wurden",
        answerB = "Eine Literaturepoche um 1800, die Gefühl, Natur und das Übernatürliche betonte",
        answerC = "Die moderne Literatur des 20. Jahrhunderts",
        answerD = "Eine Epoche, die ausschließlich realistische Alltagsschilderungen bevorzugte",
        correctAnswer = 1,
        explanation = "Die Romantik (ca. 1795–1848) war eine Literaturepoche, die Vernunft ablehnte und stattdessen Gefühle, Träume, die Natur und das Geheimnisvolle in den Vordergrund stellte.",
        difficulty = 1,
        funFact = "Berühmte Romantiker waren E.T.A. Hoffmann, Novalis und die Gebrüder Grimm. Die Romantik entstand als Reaktion auf die sachliche Vernunft der Aufklärung."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist die Klassik als literarische Epoche?",
        answerA = "Ein Begriff für alle antiken griechischen und römischen Texte",
        answerB = "Eine Epoche um 1800, die Harmonie, Humanität und klassische Formen betonte",
        answerC = "Die Epoche, in der die ersten deutschen Bücher gedruckt wurden",
        answerD = "Eine moderne Bezeichnung für qualitativ hochwertige Literatur",
        correctAnswer = 1,
        explanation = "Die Weimarer Klassik (ca. 1786–1832) um Goethe und Schiller betonte Ideale wie Harmonie, Menschlichkeit und klare Formen, beeinflusst von der griechischen Antike.",
        difficulty = 1,
        funFact = "Goethe und Schiller waren die Zentralfiguren der deutschen Klassik. Beide lebten in Weimar – deshalb spricht man von der 'Weimarer Klassik'."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist die Aufklärung als literarische Epoche?",
        answerA = "Eine Epoche, in der Bücher durch Buchdruck erstmals massenhaft verbreitet wurden",
        answerB = "Ein Begriff für moderne, verständlich geschriebene Sachbücher",
        answerC = "Eine Epoche im 18. Jahrhundert, die Vernunft und kritisches Denken in den Mittelpunkt stellte",
        answerD = "Die Zeit, in der die Bibel erstmals ins Deutsche übersetzt wurde",
        correctAnswer = 2,
        explanation = "Die Aufklärung (ca. 1720–1790) war eine geistige Bewegung, die Vernunft, Bildung und kritisches Denken gegen Aberglauben und Autoritätshörigkeit stellte.",
        difficulty = 1,
        funFact = "Immanuel Kant fasste die Aufklärung so zusammen: 'Habe Mut, dich deines eigenen Verstandes zu bedienen!' Das wurde zum Leitspruch der Epoche."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist der erste Satz von Franz Kafkas 'Die Verwandlung' (1915)?",
        answerA = "'Es war ein ungewöhnlicher Morgen, als Gregor Samsa aufwachte.'",
        answerB = "'Als Gregor Samsa eines Morgens aus unruhigen Träumen erwachte, fand er sich in seinem Bett zu einem ungeheueren Ungeziefer verwandelt.'",
        answerC = "'Der Käfer war da, noch bevor Gregor die Augen öffnete.'",
        answerD = "'Gregor Samsa war ein Mann, der sein Leben dem Wohl seiner Familie geopfert hatte.'",
        correctAnswer = 1,
        explanation = "Dies ist einer der berühmtesten Romananfänge der Weltliteratur. Kafka beginnt sofort mit dem Ungeheuerlichen, ohne Erklärung – das ist typisch für seinen Stil.",
        difficulty = 1,
        funFact = "Kafka wollte, dass auf dem Cover niemals ein Insekt oder Käfer abgebildet wird. Er sagte, das 'Ungeziefer' solle undarstellbar bleiben."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist der Unterschied zwischen Autor und Erzähler?",
        answerA = "Kein Unterschied – Autor und Erzähler sind immer dieselbe Person",
        answerB = "Der Autor schreibt das Buch, der Erzähler ist die fiktive Stimme, die die Geschichte erzählt",
        answerC = "Der Autor erfindet die Geschichte, der Erzähler schreibt sie auf",
        answerD = "Der Autor ist für Romane zuständig, der Erzähler nur für mündliche Geschichten",
        correctAnswer = 1,
        explanation = "Der Autor ist der reale Mensch, der ein Werk schreibt. Der Erzähler ist die fiktive Instanz im Text, die die Geschichte erzählt. Sie müssen nicht identisch sein.",
        difficulty = 1,
        funFact = "In manchen Romanen erzählt eine Figur die Geschichte in der Ich-Form – das nennt man einen 'Ich-Erzähler'. Er ist trotzdem eine Erfindung des Autors."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist das Reimschema ABAB?",
        answerA = "Reimschema, bei dem sich Zeile 1 und 2 reimen, Zeile 3 und 4 reimen",
        answerB = "Reimschema, bei dem sich Zeile 1 und 3 reimen, Zeile 2 und 4 reimen",
        answerC = "Reimschema, bei dem alle vier Zeilen dasselbe Reimwort haben",
        answerD = "Reimschema, bei dem gar keine Zeilen sich reimen",
        correctAnswer = 1,
        explanation = "Im Reimschema ABAB reimen sich die erste und dritte Zeile (A) sowie die zweite und vierte Zeile (B). Man nennt es auch 'Kreuzreim'.",
        difficulty = 1,
        funFact = "Das ABAB-Muster ist der häufigste Kreuzreim in der Lyrik. Das AABB-Muster, bei dem je zwei aufeinanderfolgende Zeilen reimen, nennt man 'Paarreim'."
    ),

    Question(
        categoryId = 10,
        questionText = "Wer ist der Autor des Romans 'Die Leiden des jungen Werthers' (1774)?",
        answerA = "Friedrich Schiller",
        answerB = "Heinrich von Kleist",
        answerC = "Johann Wolfgang von Goethe",
        answerD = "Gotthold Ephraim Lessing",
        correctAnswer = 2,
        explanation = "'Die Leiden des jungen Werthers' ist ein Briefroman von Johann Wolfgang von Goethe, der 1774 erschien und ihn schlagartig berühmt machte.",
        difficulty = 1,
        funFact = "Der Roman löste in Europa eine Welle der 'Werther-Begeisterung' aus – junge Männer kleideten sich wie Werther. Es gibt sogar Berichte über Selbstmorde nach Werthers Vorbild."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist eine Kurzgeschichte?",
        answerA = "Ein Roman, der in weniger als einer Woche geschrieben wurde",
        answerB = "Eine kurze Prosaerzählung, die sich auf einen einzigen Moment oder ein Ereignis konzentriert",
        answerC = "Ein Gedicht, das eine Geschichte in wenigen Strophen erzählt",
        answerD = "Eine Fabel, die weniger als eine Seite lang ist",
        correctAnswer = 1,
        explanation = "Eine Kurzgeschichte ist eine kompakte Prosaerzählung, die sich meist auf eine Schlüsselsituation konzentriert. Sie ist kürzer als eine Novelle und hat oft ein offenes Ende.",
        difficulty = 1,
        funFact = "Die Kurzgeschichte als Gattung wurde stark von amerikanischen Autoren wie Edgar Allan Poe und O. Henry geprägt. Auf Deutsch wurde sie erst nach 1945 sehr populär."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist eine Tragödie?",
        answerA = "Ein Drama mit glücklichem Ende",
        answerB = "Ein humoristisches Theaterstück, das zum Lachen bringt",
        answerC = "Ein Drama, in dem der Held scheitert oder stirbt",
        answerD = "Ein Theaterstück, das ausschließlich für Kinder geschrieben wurde",
        correctAnswer = 2,
        explanation = "Eine Tragödie ist ein ernstes Drama, in dem der Protagonist typischerweise scheitert, leidet oder stirbt. Das Gegenteil ist die Komödie.",
        difficulty = 1,
        funFact = "Aristoteles beschrieb in seiner 'Poetik' die Tragödie als Handlung, die beim Zuschauer 'Jammer und Schauder' auslöst und so zur 'Reinigung' (Katharsis) führt."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist eine Komödie?",
        answerA = "Ein Theaterstück mit tragischem, traurigem Ende",
        answerB = "Ein heiteres Theaterstück, das meist mit einem glücklichen Ende endet",
        answerC = "Ein Fernsehformat, das kurze Sketche zeigt",
        answerD = "Ein moderner Begriff für humoristische Romane",
        correctAnswer = 1,
        explanation = "Eine Komödie ist ein heiteres Drama, das beim Publikum Lachen erzeugt und typischerweise mit einem glücklichen Ende schließt. Das Gegenteil ist die Tragödie.",
        difficulty = 1,
        funFact = "Shakespeare schrieb sowohl Tragödien ('Hamlet', 'Macbeth') als auch Komödien ('Ein Sommernachtstraum', 'Wie es euch gefällt')."
    ),

    Question(
        categoryId = 10,
        questionText = "Was bedeutet 'Alliteration' in der Literatur?",
        answerA = "Eine Liste aller verwendeten Wörter in einem Text",
        answerB = "Die Wiederholung desselben Anfangsbuchstabens oder -lauts in aufeinanderfolgenden Wörtern",
        answerC = "Ein Zitat am Anfang eines Kapitels",
        answerD = "Die Nummerierung von Versen in einem Gedicht",
        correctAnswer = 1,
        explanation = "Eine Alliteration ist die Wiederholung des gleichen Anfangslauts, z.B. 'Mit Mann und Maus' oder 'Milch macht müde Männer munter'.",
        difficulty = 1,
        funFact = "Alliterationen wurden schon in der althochdeutschen Dichtung verwendet, lange bevor Endreime üblich wurden. Viele Redewendungen nutzen sie heute noch."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Märchen?",
        answerA = "Eine wahre Geschichte, die mündlich überliefert wurde",
        answerB = "Eine Kurzbiografie über historische Persönlichkeiten",
        answerC = "Eine fantastische Erzählung mit übernatürlichen Elementen, gutem Ende und klarer Moral",
        answerD = "Ein mittelalterliches Heldenepos über Ritter",
        correctAnswer = 2,
        explanation = "Ein Märchen ist eine fantasievolle Erzählung mit Wunderelementen, die meist mit dem Triumph des Guten über das Böse endet. Klassisch sind die Grimm'schen Märchen.",
        difficulty = 1,
        funFact = "Die Gebrüder Grimm sammelten ihre Märchen nicht selbst im Wald, sondern ließen sie sich von gebildeten Bürgerfrauen erzählen – zum Teil Märchen aus Frankreich."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Pseudonym?",
        answerA = "Ein Buchtitel, der vom Verlag vergeben wird",
        answerB = "Ein Künstlername, den ein Autor anstelle seines echten Namens verwendet",
        answerC = "Eine falsche Jahreszahl in einem Buch",
        answerD = "Ein anderes Wort für das Vorwort eines Buches",
        correctAnswer = 1,
        explanation = "Ein Pseudonym ist ein Künstler- oder Deckname, den ein Autor anstatt seines echten Namens benutzt. Bekannte Beispiele: George Orwell (Eric Blair) oder Mark Twain (Samuel Clemens).",
        difficulty = 1,
        funFact = "Viele Autorinnen im 19. Jahrhundert schrieben unter männlichen Pseudonymen, weil ihre Werke sonst nicht ernst genommen wurden – z.B. George Eliot (Mary Ann Evans)."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Epilog in einem Buch oder Theaterstück?",
        answerA = "Das Vorwort, das den Inhalt erklärt",
        answerB = "Das inhaltsverzeichnis am Anfang",
        answerC = "Ein abschließender Text nach dem eigentlichen Ende des Werkes",
        answerD = "Ein langes Nachwort des Verlegers",
        correctAnswer = 2,
        explanation = "Ein Epilog ist ein Schlussabschnitt, der nach dem eigentlichen Ende des Werkes steht und oft einen Ausblick oder eine Zusammenfassung bietet.",
        difficulty = 1,
        funFact = "Das Gegenteil des Epilogs ist der Prolog – ein einleitender Text vor dem Hauptwerk. Im antiken Theater gab es immer einen Prolog und einen Epilog."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Prolog?",
        answerA = "Eine Zusammenfassung am Ende eines Werkes",
        answerB = "Ein einleitender Text vor dem eigentlichen Hauptwerk",
        answerC = "Die Nummerierung der Kapitel in einem Roman",
        answerD = "Ein lyrisches Zwischenspiel in einem Drama",
        correctAnswer = 1,
        explanation = "Ein Prolog ist eine einleitende Passage vor dem eigentlichen Werk, die den Leser oder Zuschauer auf das Folgende vorbereitet.",
        difficulty = 1,
        funFact = "In Goethes 'Faust' gibt es gleich drei Vorreden: 'Zueignung', 'Vorspiel auf dem Theater' und 'Prolog im Himmel'."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Kapitel in einem Roman?",
        answerA = "Ein einzelner Satz, der ein Thema zusammenfasst",
        answerB = "Ein Abschnitt des Romans, der einen eigenen Titel oder eine Nummer hat",
        answerC = "Die Zusammenfassung am Ende jedes Romans",
        answerD = "Ein Zitat eines anderen Autors am Beginn des Romans",
        correctAnswer = 1,
        explanation = "Ein Kapitel ist ein Abschnitt innerhalb eines Romans, der durch Überschrift oder Nummer gekennzeichnet ist und eine Einheit der Handlung darstellt.",
        difficulty = 1,
        funFact = "Das Wort 'Kapitel' kommt vom lateinischen 'caput' (Kopf). Es bezog sich ursprünglich auf die Hauptabschnitte der Bibel."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Haiku?",
        answerA = "Ein japanisches Kurzgedicht aus drei Zeilen mit 5-7-5 Silben",
        answerB = "Ein chinesisches Lehrgedicht aus fünf Versen",
        answerC = "Ein koreanisches Märchen ohne Reime",
        answerD = "Ein indisches Epos über Götter und Helden",
        correctAnswer = 0,
        explanation = "Ein Haiku ist eine traditionelle japanische Gedichtform mit drei Zeilen: Die erste hat 5 Silben, die zweite 7 Silben, die dritte wieder 5 Silben.",
        difficulty = 1,
        funFact = "Haikus schildern oft einen kurzen Moment in der Natur. Das berühmteste deutsche Haiku: 'Über den Teich / springt ein Frosch – das leise / Plätschern des Wassers' (nach Matsuo Bashō)."
    ),

    Question(
        categoryId = 10,
        questionText = "Was bedeutet 'Ironie' in der Literatur?",
        answerA = "Das Weglassen von Satzzeichen zur besonderen Betonung",
        answerB = "Eine Aussage, die das Gegenteil des Gemeinten ausdrückt",
        answerC = "Die Wiederholung eines Themas an verschiedenen Stellen",
        answerD = "Eine literarische Technik, bei der Ereignisse rückblickend erzählt werden",
        correctAnswer = 1,
        explanation = "Ironie bedeutet, etwas so zu sagen, dass das Gegenteil gemeint ist. Z.B. wenn jemand bei strömenden Regen sagt: 'Was für ein wunderschöner Tag!'",
        difficulty = 1,
        funFact = "Das Wort 'Ironie' kommt vom griechischen 'eironeia', was 'Verstellung' bedeutet. Sokrates war berühmt für seine ironischen Fragen."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Wörterbuch?",
        answerA = "Eine Liste aller Bücher in einer Bibliothek",
        answerB = "Ein Nachschlagewerk, das Wörter erklärt oder in andere Sprachen übersetzt",
        answerC = "Ein Verzeichnis aller bekannten Autoren eines Landes",
        answerD = "Ein Buch, das ausschließlich Zitate enthält",
        correctAnswer = 1,
        explanation = "Ein Wörterbuch ist ein Nachschlagewerk, das Wörter alphabetisch ordnet und ihre Bedeutung, Herkunft oder Übersetzung erläutert.",
        difficulty = 1,
        funFact = "Das berühmteste deutsche Wörterbuch ist der 'Duden', benannt nach dem Gymnasialdirektor Konrad Duden, der es 1880 erstmals herausgab."
    ),

    Question(
        categoryId = 10,
        questionText = "Was ist ein Cliffhanger in einem Roman oder einer Serie?",
        answerA = "Die Auflösung aller Rätsel am Ende eines Buches",
        answerB = "Ein offenes Ende, das den Leser in Spannung lässt und Neugier auf die Fortsetzung weckt",
        answerC = "Ein spannender Wendepunkt genau in der Mitte eines Romans",
        answerD = "Eine Szene, in der eine Figur buchstäblich von einer Klippe hängt",
        correctAnswer = 1,
        explanation = "Ein Cliffhanger ist ein spannungserzeugender Schluss – ein Kapitel oder eine Episode endet auf einem Höhepunkt ohne Auflösung, um den Leser oder Zuschauer zu fesseln.",
        difficulty = 1,
        funFact = "Der Begriff 'Cliffhanger' stammt aus Fortsetzungsromanen des 19. Jahrhunderts, in denen Figuren tatsächlich an Klippen hingen – bis zur nächsten Ausgabe."
    )

)
