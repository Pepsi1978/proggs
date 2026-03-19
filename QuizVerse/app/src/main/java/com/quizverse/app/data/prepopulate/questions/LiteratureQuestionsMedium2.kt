package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun literatureQuestionsMedium2(): List<Question> = listOf(

    // Question 1 — Dickens: Oliver Twist — Fagin
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie heißt der Hehler und Anführer der Kinderbande in Charles Dickens' 'Oliver Twist' (1838)?",
        answerA = "Fagin",
        answerB = "Bill Sikes",
        answerC = "Monks",
        answerD = "Mr. Brownlow",
        correctAnswer = 0,
        explanation = "Fagin ist der Hehler, der eine Bande von Straßenkindern als Taschendiebe ausbildet und Oliver Twist in die Londoner Unterwelt einführt.",
        difficulty = 2,
        funFact = "Dickens veröffentlichte 'Oliver Twist' ursprünglich 1837–1839 als Fortsetzungsroman in einer Zeitschrift — es war sein erstes sozialkritisches Werk."
    ),

    // Question 2 — Dickens: Oliver Twist — Erscheinungsjahr
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "In welchem Jahrzehnt erschien Charles Dickens' Roman 'Oliver Twist' als Buch?",
        answerA = "1820er Jahre",
        answerB = "1830er Jahre",
        answerC = "1850er Jahre",
        answerD = "1860er Jahre",
        correctAnswer = 1,
        explanation = "Oliver Twist erschien 1838 als dreibändiges Buch, nachdem er zuvor 1837–1839 als Fortsetzungsroman im Magazin 'Bentley's Miscellany' veröffentlicht wurde.",
        difficulty = 2,
        funFact = "Dickens war Herausgeber von 'Bentley's Miscellany' und schrieb 'Oliver Twist' parallel zu seinem ersten Roman 'The Pickwick Papers'."
    ),

    // Question 3 — Dickens: Oliver Twist — Nancy
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welche Figur in 'Oliver Twist' ist eine Kriminelle, die aber Mitleid mit Oliver empfindet und ihm zu helfen versucht?",
        answerA = "Rose Maylie",
        answerB = "Agnes Fleming",
        answerC = "Nancy",
        answerD = "Charlotte",
        correctAnswer = 2,
        explanation = "Nancy ist Bill Sikes' Geliebte und Teil von Fagins Bande, bewahrt aber genug Empathie, um Oliver zu helfen und ihn vor einem noch schlimmeren Schicksal zu bewahren.",
        difficulty = 2,
        funFact = "Nancy gilt als eine der komplexesten Figuren bei Dickens — eine Frau in der Kriminalität, die trotzdem moralisch handelt und dafür mit dem Leben bezahlt."
    ),

    // Question 4 — Tolstoi: Krieg und Frieden — Hauptfiguren
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welche drei Hauptfiguren stehen im Mittelpunkt von Leo Tolstois 'Krieg und Frieden' (1869)?",
        answerA = "Anna Karenina, Lewin, Oblonski",
        answerB = "Raskolnikow, Sonja, Porfirij",
        answerC = "Jean Valjean, Cosette, Javert",
        answerD = "Pierre Besuchow, Fürst Andrej Bolkonski, Natascha Rostowa",
        correctAnswer = 3,
        explanation = "Die drei Zentralfiguren sind Pierre Besuchow, Fürst Andrej Bolkonski und Natascha Rostowa, deren Schicksale sich während der Napoleonischen Kriege von 1805–1812 entfalten.",
        difficulty = 2,
        funFact = "'Krieg und Frieden' umfasst über 1600 Seiten und mehr als 500 Figuren — Tolstoi arbeitete sechs Jahre daran."
    ),

    // Question 5 — Tolstoi: Krieg und Frieden — Erscheinungsjahr
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "In welchem Zeitraum erschien Tolstois 'Krieg und Frieden' erstmals als vollständiges Werk?",
        answerA = "1868/1869",
        answerB = "1855/1856",
        answerC = "1880/1881",
        answerD = "1901/1902",
        correctAnswer = 0,
        explanation = "Der Roman erschien ab 1865 in Teilen in der Zeitschrift 'Russkij Wjestnik' und wurde 1868/1869 als vollständiges Werk veröffentlicht.",
        difficulty = 2,
        funFact = "Tolstoi begann mit einem Roman über die Dekabristen-Bewegung von 1825, blätterte immer weiter zurück in der Geschichte und landete schließlich bei 1805."
    ),

    // Question 6 — Tolstoi: Krieg und Frieden — Napoleon
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welches historische Ereignis bildet den zentralen Hintergrund von 'Krieg und Frieden'?",
        answerA = "Die Französische Revolution 1789",
        answerB = "Napoleons Invasion Russlands 1812",
        answerC = "Der Krimkrieg 1853–1856",
        answerD = "Die Befreiungskriege nach 1813",
        correctAnswer = 1,
        explanation = "Der Höhepunkt des Romans ist Napoleons Feldzug gegen Russland 1812, einschließlich der verheerenden Schlacht bei Borodino und dem Brand Moskaus.",
        difficulty = 2,
        funFact = "Tolstoi selbst kämpfte im Krimkrieg und nutzte seine Kriegserfahrungen, um die Schlachtszenen in 'Krieg und Frieden' realistisch zu gestalten."
    ),

    // Question 7 — Dostojewski: Schuld und Sühne — Mord
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wen ermordet Raskolnikow in Dostojewskis 'Schuld und Sühne' (1866) zuerst?",
        answerA = "Seinen Freund Rasumichin",
        answerB = "Den Polizeiinspektor Porfirij",
        answerC = "Eine alte Pfandleiherin namens Aljona Iwanowna",
        answerD = "Seinen Vermieter Marmeladow",
        correctAnswer = 2,
        explanation = "Raskolnikow ermordet die alte Pfandleiherin Aljona Iwanowna mit einer Axt und tötet im Anschluss auch deren zufällig anwesende Stiefschwester Lisaweta.",
        difficulty = 2,
        funFact = "'Schuld und Sühne' erschien 1866 und ist Dostojewskis erster großer Roman — er schrieb ihn, um einen Vorschussschulden bei einem Verleger zu begleichen."
    ),

    // Question 8 — Dostojewski: Schuld und Sühne — Sonja
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welche Figur hilft in 'Schuld und Sühne' Raskolnikow bei seiner moralischen Erneuerung und begleitet ihn nach Sibirien?",
        answerA = "Awdotja (Dunja), seine Schwester",
        answerB = "Natascha, die Wirtstochter",
        answerC = "Die Witwe Marmeladow",
        answerD = "Sofja (Sonja) Marmeladowa",
        correctAnswer = 3,
        explanation = "Sonja Marmeladowa, die sich als Prostituierte verdingt, um ihre Familie zu ernähren, wird Raskolnikows moralischer Anker. Sie begleitet ihn in die sibirische Verbannung.",
        difficulty = 2,
        funFact = "Sonja liest Raskolnikow die biblische Geschichte der Auferweckung des Lazarus vor — ein Symbol für die Möglichkeit der Erneuerung und Erlösung."
    ),

    // Question 9 — Victor Hugo: Les Misérables — Jean Valjean
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie viele Jahre verbrachte Jean Valjean insgesamt als Galeerensträfling in Victor Hugos 'Les Misérables' (1862)?",
        answerA = "19 Jahre",
        answerB = "5 Jahre",
        answerC = "12 Jahre",
        answerD = "25 Jahre",
        correctAnswer = 0,
        explanation = "Valjean wurde ursprünglich zu 5 Jahren verurteilt, weil er Brot stahl. Fluchtversuche verlängerten die Strafe auf insgesamt 19 Jahre Zwangsarbeit in Toulon.",
        difficulty = 2,
        funFact = "Jean Valjean trägt die Häftlingsnummer 24601 — diese Nummer wurde durch das Musical 'Les Misérables' weltberühmt."
    ),

    // Question 10 — Victor Hugo: Les Misérables — Javert
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wer ist der Polizeiinspektor, der Jean Valjean in 'Les Misérables' jahrelang unerbittlich verfolgt?",
        answerA = "Thénardier",
        answerB = "Javert",
        answerC = "Marius",
        answerD = "Gillenormand",
        correctAnswer = 1,
        explanation = "Inspektor Javert verkörpert das unerbittliche Gesetz. Er verfolgt Valjean über Jahrzehnte, bis er in einem moralischen Dilemma zwischen Gesetz und Gnade scheitert.",
        difficulty = 2,
        funFact = "Javert und Valjean sind heute Archetypen in der Weltliteratur: Valjean steht für Erlösung, Javert für blinde Gesetzeserfüllung ohne Mitgefühl."
    ),

    // Question 11 — Victor Hugo: Les Misérables — Cosette
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welches Mädchen nimmt Jean Valjean in 'Les Misérables' als seine Pflegetochter auf und zieht es als eigenes Kind groß?",
        answerA = "Éponine",
        answerB = "Fantine",
        answerC = "Cosette",
        answerD = "Azelma",
        correctAnswer = 2,
        explanation = "Cosette ist die Tochter von Fantine, die sie bei den skrupellosen Thénardiers lässt. Jean Valjean befreit sie und zieht sie als seine Tochter auf.",
        difficulty = 2,
        funFact = "'Les Misérables' von Victor Hugo ist mit über 1200 Seiten einer der längsten Romane der Weltliteratur und enthält ausgedehnte Exkurse über die Geschichte Frankreichs."
    ),

    // Question 12 — Jane Austen: Stolz und Vorurteil — Elizabeth
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welche Bennett-Tochter ist die Hauptfigur in Jane Austens 'Stolz und Vorurteil' (1813)?",
        answerA = "Jane Bennett",
        answerB = "Lydia Bennett",
        answerC = "Mary Bennett",
        answerD = "Elizabeth Bennett",
        correctAnswer = 3,
        explanation = "Elizabeth Bennett ist die zweite Tochter der Familie und die Hauptprotagonistin. Ihr Geist, ihr Witz und ihre Unabhängigkeit machen sie zu einer der beliebtesten Heldinnen der Weltliteratur.",
        difficulty = 2,
        funFact = "'Stolz und Vorurteil' erschien 1813 anonym mit dem Vermerk 'By a Lady' — der Name Jane Austen erschien erst nach ihrem Tod auf dem Titelblatt."
    ),

    // Question 13 — Jane Austen: Stolz und Vorurteil — Darcy
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welche Eigenschaft verkörpert Mr. Darcy in 'Stolz und Vorurteil' zu Beginn des Romans, die Elizabeth Bennett abstößt?",
        answerA = "Arroganz und Hochmut (der Stolz)",
        answerB = "Feigheit und Unentschlossenheit",
        answerC = "Geldgier und Habsucht",
        answerD = "Faulheit und Gleichgültigkeit",
        correctAnswer = 0,
        explanation = "Mr. Darcy verkörpert den 'Stolz' im Romantitel. Sein arrogantes Auftreten bei ihrer ersten Begegnung weckt Elizabeths 'Vorurteil' gegen ihn.",
        difficulty = 2,
        funFact = "Jane Austen hatte den Roman ursprünglich 'First Impressions' (Erste Eindrücke) genannt — 'Stolz und Vorurteil' wurde erst beim Verleger so benannt."
    ),

    // Question 14 — Jane Austen: Stolz und Vorurteil — Wickham
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was enthüllt sich in 'Stolz und Vorurteil' über Mr. Wickhams wahren Charakter?",
        answerA = "Er ist heimlich mit Jane Bennett verlobt",
        answerB = "Er ist ein Schuldenmacher und Verführer, der Lydia entführt",
        answerC = "Er ist ein Spion der Franzosen",
        answerD = "Er ist Darcys unehelicher Bruder",
        correctAnswer = 1,
        explanation = "Wickham erscheint charmant, entpuppt sich aber als unehrlicher Schuldenmacher. Er entführt die junge Lydia Bennett, um sie zur Flucht und Heirat zu überreden.",
        difficulty = 2,
        funFact = "Wickham hatte zuvor versucht, Darcys jüngere Schwester Georgiana zu entführen, um an ihr Vermögen zu kommen — was Darcy verhinderte."
    ),

    // Question 15 — Emily Brontë: Wuthering Heights — Heathcliff
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wie kommt Heathcliff in Emily Brontës 'Sturmhöhe' (1847) als Kind zu der Familie Earnshaw?",
        answerA = "Er ist der uneheliche Sohn von Hindley Earnshaw",
        answerB = "Er wird von der Familie adoptiert, weil seine Eltern starben",
        answerC = "Mr. Earnshaw bringt ihn als verwahrlostes Kind von den Straßen Liverpools mit",
        answerD = "Er läuft als Waise von einer Nachbarfarm weg",
        correctAnswer = 2,
        explanation = "Old Earnshaw findet den verwahrlosten Jungen auf den Straßen Liverpools und bringt ihn mit nach Hause. Heathcliffs Herkunft bleibt im Roman rätselhaft.",
        difficulty = 2,
        funFact = "'Sturmhöhe' war Emily Brontës einziger Roman. Er wurde 1847 unter dem Pseudonym Ellis Bell veröffentlicht und von der viktorianischen Gesellschaft zunächst abgelehnt."
    ),

    // Question 16 — Emily Brontë: Wuthering Heights — Catherine
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Warum heiratet Catherine in 'Sturmhöhe' nicht Heathcliff, obwohl sie ihn liebt?",
        answerA = "Weil Heathcliff sie ablehnt und nach Liverpool flieht",
        answerB = "Weil ihr Bruder Hindley die Heirat mit Waffengewalt verhindert",
        answerC = "Weil Heathcliff bereits mit einer anderen Frau verheiratet ist",
        answerD = "Weil Edgar Linton von höherem Stand und reichem Besitz ist",
        correctAnswer = 3,
        explanation = "Catherine wählt den wohlhabenden Edgar Linton, weil sie gesellschaftlichen Aufstieg und Sicherheit anstrebt — eine Entscheidung, die Heathcliff zu seinem lebenslangen Rachefeldzug treibt.",
        difficulty = 2,
        funFact = "Heathcliff und Catherine sind eines der tragischsten Liebespaare der Weltliteratur. Der Roman spielt über zwei Generationen auf den sturmgepeitschten Mooren Yorkshires."
    ),

    // Question 17 — Herman Melville: Moby Dick — Erzähler
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Mit welchem berühmten ersten Satz beginnt Herman Melvilles Roman 'Moby Dick' (1851)?",
        answerA = "'Nennt mich Ismael.' (Call me Ishmael.)",
        answerB = "'Es war der beste aller Zeiten, es war der schlechteste aller Zeiten.'",
        answerC = "'Alle glücklichen Familien gleichen einander, jede unglückliche ist auf ihre eigene Weise unglücklich.'",
        answerD = "'Es war einmal ein großer weißer Wal.'",
        correctAnswer = 0,
        explanation = "Der berühmte Eröffnungssatz 'Call me Ishmael' ist einer der bekanntesten der Weltliteratur. Ismael ist der Ich-Erzähler und einzige Überlebende des Romans.",
        difficulty = 2,
        funFact = "Der vollständige Titel des Romans lautet 'Moby-Dick; or, The Whale'. Melville widmete das Buch seinem Freund Nathaniel Hawthorne."
    ),

    // Question 18 — Herman Melville: Moby Dick — Ahabs Bein
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Woraus besteht Kapitän Ahabs Prothese in 'Moby Dick', die sein abgebissenes Bein ersetzt?",
        answerA = "Aus Teakholz",
        answerB = "Aus dem Knochen (Elfenbein) eines Pottwals",
        answerC = "Aus Eisen und Messing",
        answerD = "Aus dem Holz des Schiffes Pequod",
        correctAnswer = 1,
        explanation = "Ahab trägt ein Bein aus Walnochen (Elfenbein) als Prothese — ein Symbol seiner Obsession mit dem Wal, der ihn verstümmelt hat.",
        difficulty = 2,
        funFact = "Kapitän Ahab ist nach dem biblischen König Ahab benannt, der sich von Gott abwandte. Melville verlieh der Figur Züge von König Lear, Faust und Prometheus."
    ),

    // Question 19 — Herman Melville: Moby Dick — Ende
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was geschieht am Ende von 'Moby Dick' nach dem dreitägigen Kampf mit dem Wal?",
        answerA = "Ahab tötet Moby Dick und kehrt als Held heim",
        answerB = "Moby Dick entkommt und die Mannschaft kehrt unverletzt zurück",
        answerC = "Moby Dick versenkt das Schiff Pequod, nur Ismael überlebt",
        answerD = "Ahab versöhnt sich mit dem Wal und gibt die Jagd auf",
        correctAnswer = 2,
        explanation = "Nach drei Tagen des Kampfes rammt Moby Dick das Schiff Pequod und versenkt es. Die gesamte Mannschaft außer Ismael kommt ums Leben.",
        difficulty = 2,
        funFact = "Ismael überlebt, indem er sich an den schwimmenden Sarg seines Freundes Queequeg klammert — bis ihn ein anderes Schiff, die Rachel, aufsammelt."
    ),

    // Question 20 — Flaubert: Madame Bovary — Emma
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Warum ist Emma Bovary in Flauberts Roman 'Madame Bovary' (1857) so unglücklich in ihrer Ehe?",
        answerA = "Weil ihr Mann sie körperlich misshandelt",
        answerB = "Weil sie arm ist und keine Kleider kaufen kann",
        answerC = "Weil sie keine Kinder bekommen kann",
        answerD = "Weil die Realität des Provinzlebens ihren romantischen Träumen aus Romanen nicht entspricht",
        correctAnswer = 3,
        explanation = "Emma wurde durch romantische Romane geprägt und erwartet ein leidenschaftliches, aufregendes Leben. Die Realität als Landarztgattin in der Provinz enttäuscht sie zutiefst.",
        difficulty = 2,
        funFact = "Flaubert wurde nach der Veröffentlichung wegen Obszönität angeklagt, aber freigesprochen. Der Skandal machte 'Madame Bovary' zum Bestseller."
    ),

    // Question 21 — Flaubert: Madame Bovary — Erscheinungsjahr
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "In welcher Zeitschrift wurde 'Madame Bovary' 1856 erstmals als Fortsetzungsroman veröffentlicht?",
        answerA = "Revue de Paris",
        answerB = "Le Figaro",
        answerC = "La Gazette de France",
        answerD = "Revue des Deux Mondes",
        correctAnswer = 0,
        explanation = "'Madame Bovary' erschien von Oktober bis Dezember 1856 in der 'Revue de Paris' — und sorgte unmittelbar für einen Zensurprozess wegen angeblicher Unmoral.",
        difficulty = 2,
        funFact = "Flaubert soll gesagt haben: 'Madame Bovary, c'est moi' (Madame Bovary bin ich) — er identifizierte sich mit dem Innenleben seiner Heldin."
    ),

    // Question 22 — Flaubert: Madame Bovary — Tod
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wie endet Emma Bovarys Leben in Flauberts Roman?",
        answerA = "Sie flieht mit ihrem Liebhaber nach Paris",
        answerB = "Sie nimmt sich das Leben durch Arsen vergiften",
        answerC = "Sie stirbt bei der Geburt ihres zweiten Kindes",
        answerD = "Sie wird von ihrem betrogenen Ehemann erschossen",
        correctAnswer = 1,
        explanation = "Emma Bovary nimmt sich das Leben, indem sie Arsen schluckt, nachdem ihre Schulden sie ruiniert haben und all ihre Liebhaber sie verlassen haben.",
        difficulty = 2,
        funFact = "Die langsam sterbende Emma war eines der ersten realistischen Sterbeszenen der Literatur — Flaubert recherchierte medizinische Quellen über Arsenvergiftung."
    ),

    // Question 23 — Mark Twain: Tom Sawyer — Erscheinungsjahr
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "In welchem Jahr erschien Mark Twains 'Die Abenteuer des Tom Sawyer'?",
        answerA = "1860",
        answerB = "1870",
        answerC = "1876",
        answerD = "1884",
        correctAnswer = 2,
        explanation = "'Die Abenteuer des Tom Sawyer' erschien 1876. Der Schauplatz ist das fiktive Dorf St. Petersburg in Missouri am Mississippiflussufer.",
        difficulty = 2,
        funFact = "Mark Twain ist das Pseudonym von Samuel Langhorne Clemens. 'Mark Twain' ist ein Flussschiffer-Ausdruck und bedeutet 'zwei Faden tief' (ca. 3,6 Meter)."
    ),

    // Question 24 — Mark Twain: Huckleberry Finn — Jim
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wer ist Huck Finns wichtigster Reisegefährte auf dem Mississippi in 'Die Abenteuer des Huckleberry Finn' (1884)?",
        answerA = "Tom Sawyer, sein bester Freund",
        answerB = "Becky Thatcher, Toms Freundin",
        answerC = "Hucks Vater Pap Finn",
        answerD = "Jim, ein entlaufener Sklave",
        correctAnswer = 3,
        explanation = "Jim, ein entlaufener Sklave, ist Hucks wichtigster Weggefährte. Gemeinsam fliehen sie auf einem Floß den Mississippi hinunter — Huck schützt Jim vor den Sklavenjägern.",
        difficulty = 2,
        funFact = "Ernest Hemingway sagte: 'Alle moderne amerikanische Literatur kommt aus einem Buch von Mark Twain namens Huckleberry Finn.' — Das ist seine herausragende Stellung."
    ),

    // Question 25 — Mark Twain: Huckleberry Finn — Ort
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "An welchem großen amerikanischen Fluss spielt die Haupthandlung von 'Huckleberry Finn'?",
        answerA = "Am Mississippi",
        answerB = "Am Missouri",
        answerC = "Am Ohio",
        answerD = "Am Colorado",
        correctAnswer = 0,
        explanation = "Huck und Jim reisen auf einem Floß den Mississippi hinunter. Der Fluss ist der Freiheitsraum des Romans — auf dem Wasser sind sie frei, an Land lauern die Gefahren.",
        difficulty = 2,
        funFact = "Mark Twain arbeitete selbst als Flussschiffer auf dem Mississippi und nutzte seine tiefen Kenntnisse des Flusses für den Roman."
    ),

    // Question 26 — Edgar Allan Poe: Der Rabe — Refrain
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welches einzige Wort spricht der Rabe in Edgar Allan Poes Gedicht 'Der Rabe' (1845) immer wieder?",
        answerA = "Aldebaran",
        answerB = "Nevermore (Nimmermehr)",
        answerC = "Forever (Für immer)",
        answerD = "Darkness (Dunkelheit)",
        correctAnswer = 1,
        explanation = "Der geheimnisvolle Rabe antwortet auf jede Frage des Sprechers mit dem einzigen Wort 'Nevermore' (Nimmermehr), was den Sprecher in Wahnsinn treibt.",
        difficulty = 2,
        funFact = "Poe veröffentlichte 'The Raven' am 29. Januar 1845 und erklärte in seinem Essay 'The Philosophy of Composition', wie er das Gedicht mathematisch konstruiert habe."
    ),

    // Question 27 — Edgar Allan Poe: Werke — Inquisition
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welche Poe-Erzählung schildert die Folterungen eines Gefangenen der Spanischen Inquisition in Toledo?",
        answerA = "Der Untergang des Hauses Usher",
        answerB = "Der Goldkäfer",
        answerC = "Die Grube und das Pendel",
        answerD = "Berenice",
        correctAnswer = 2,
        explanation = "'Die Grube und das Pendel' (1842) schildert, wie ein Gefangener der Spanischen Inquisition zwischen einer tiefen Grube und einem absenkenden Pendelmesser gefangen ist.",
        difficulty = 2,
        funFact = "Edgar Allan Poe schrieb über 70 Kurzgeschichten und mehr als 50 Gedichte. Er gilt als Begründer des modernen Kriminalromans und der Horrorliteratur."
    ),

    // Question 28 — Edgar Allan Poe: Herkunft und Tod
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "In welcher amerikanischen Stadt wurde Edgar Allan Poe 1809 geboren?",
        answerA = "New York City",
        answerB = "Philadelphia",
        answerC = "Baltimore",
        answerD = "Boston",
        correctAnswer = 3,
        explanation = "Edgar Allan Poe wurde am 19. Januar 1809 in Boston, Massachusetts, geboren. Er starb 1849 unter ungeklärten Umständen in Baltimore.",
        difficulty = 2,
        funFact = "Poes Tod mit nur 40 Jahren ist bis heute ungeklärt. Theorien reichen von Tollwut über Alkohol bis zu Wahlbetrug als mögliche Todesursachen."
    ),

    // Question 29 — Émile Zola: Germinal — Étienne
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wer ist der Protagonist in Émile Zolas Roman 'Germinal' (1885)?",
        answerA = "Étienne Lantier, ein junger Eisenbahner der im Bergwerk Arbeit sucht",
        answerB = "Jean Macquart, ein Soldat",
        answerC = "Claude Lantier, ein Maler in Paris",
        answerD = "Nana Coupeau, eine Tänzerin",
        correctAnswer = 0,
        explanation = "Étienne Lantier ist ein arbeitsloser Eisenbahner, der im Kohlenbergwerk Le Voreux eine Stelle annimmt und dort zum Anführer eines Bergarbeiterstreiks wird.",
        difficulty = 2,
        funFact = "'Germinal' ist der 13. Roman in Zolas 20-bändiger Rougon-Macquart-Serie. Der Titel stammt vom siebten Monat des Französischen Revolutionskalenders."
    ),

    // Question 30 — Balzac: Vater Goriot — Thema
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Worum geht es in Honoré de Balzacs Roman 'Vater Goriot' (1835)?",
        answerA = "Um einen reichen Adligen, der in der Revolution all seinen Besitz verliert",
        answerB = "Um einen Vater, der sich für seine undankbaren Töchter ruiniert",
        answerC = "Um einen Bankier, der durch Betrug reich wird",
        answerD = "Um einen Pfarrer in der französischen Provinz",
        correctAnswer = 1,
        explanation = "Vater Goriot gibt sein gesamtes Vermögen aus, um seinen beiden Töchtern ein Leben in der Pariser Gesellschaft zu ermöglichen — sie lassen ihn daraufhin verarmt sterben.",
        difficulty = 2,
        funFact = "'Vater Goriot' gehört zu Balzacs 'La Comédie Humaine', einem Zyklus von über 90 Romanen und Novellen. Rastignac, der junge Protagonist, erscheint in mehreren Werken."
    ),

    // Question 31 — Balzac: La Comédie Humaine
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wie heißt der riesige Romanzyklus, zu dem Honoré de Balzacs 'Vater Goriot' gehört?",
        answerA = "Les Rougon-Macquart",
        answerB = "La Recherche du Temps Perdu",
        answerC = "La Comédie Humaine",
        answerD = "Le Rouge et le Noir",
        correctAnswer = 2,
        explanation = "'La Comédie Humaine' (Die menschliche Komödie) umfasst über 90 Werke Balzacs, in denen über 2000 Figuren auftreten und ein Panorama der französischen Gesellschaft entsteht.",
        difficulty = 2,
        funFact = "Balzac arbeitete mit extremer Disziplin: Er schrieb täglich 12–16 Stunden, trank massenhaft Kaffee und schuf so ein Gesamtwerk von beispiellosem Umfang."
    ),

    // Question 32 — Stendhal: Rot und Schwarz — Julien Sorel
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welchen gesellschaftlichen Aufstieg verfolgt Julien Sorel in Stendhals 'Rot und Schwarz' (1830)?",
        answerA = "Er will Offizier werden und zieht in den Krieg",
        answerB = "Er will als Kaufmann reich werden",
        answerC = "Er will Politiker und Abgeordneter werden",
        answerD = "Er will durch Bildung, Verführung und Heuchelei in die Aristokratie aufsteigen",
        correctAnswer = 3,
        explanation = "Julien Sorel, Sohn eines Sägemühlbesitzers, versucht durch Intelligenz, das Spiel der Gesellschaft und Liebschaften mit höhergestellten Frauen sozial aufzusteigen.",
        difficulty = 2,
        funFact = "Der Titel 'Rot und Schwarz' soll die Wahl zwischen militärischer (rot) und kirchlicher (schwarz) Karriere symbolisieren — die beiden Wege zum sozialen Aufstieg in der Restaurationszeit."
    ),

    // Question 33 — Stendhal: Rot und Schwarz — Erscheinungsjahr
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "In welchem Jahr erschien Stendhals Roman 'Rot und Schwarz'?",
        answerA = "1830",
        answerB = "1815",
        answerC = "1842",
        answerD = "1857",
        correctAnswer = 0,
        explanation = "'Rot und Schwarz' erschien 1830 in Paris. Der Untertitel lautet 'Chronique du XIXe siècle' (Chronik des 19. Jahrhunderts).",
        difficulty = 2,
        funFact = "Stendhal ist das Pseudonym von Marie-Henri Beyle. Er wählte den Ortsnamen 'Stendhal' (ein Ort in Preußen) als sein literarisches Alter Ego."
    ),

    // Question 34 — Nathaniel Hawthorne: Scharlachrotes A — Hester
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was bedeutet das scharlachrote 'A' auf Hester Prynnes Kleid in Hawthornes 'The Scarlet Letter' (1850)?",
        answerA = "America — sie soll als Fremde gebrandmarkt werden",
        answerB = "Adultery (Ehebruch) — ihre öffentliche Strafe für außereheliche Beziehung",
        answerC = "Atheism — ihre Strafe für Gottlosigkeit",
        answerD = "Able — eine späte Ehrung der Gemeinde für ihre Hilfsbereitschaft",
        correctAnswer = 1,
        explanation = "Hester Prynne wird von der puritanischen Gemeinde in Boston verurteilt, für immer ein rotes 'A' (für Adultery/Ehebruch) zu tragen, weil sie ein uneheliches Kind gebar.",
        difficulty = 2,
        funFact = "Im Verlauf des Romans wandelt sich die Bedeutung des 'A' im Bewusstsein der Gemeinde von 'Adultery' zu 'Able' — Hester wird für ihre guten Taten respektiert."
    ),

    // Question 35 — Nathaniel Hawthorne: Scarlet Letter — Dimmesdale
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wer ist in Hawthornes 'The Scarlet Letter' der Vater von Hesters Kind Pearl?",
        answerA = "Roger Chillingworth, Hesters Ehemann",
        answerB = "Governor Bellingham, der Stadthalter",
        answerC = "Arthur Dimmesdale, der geehrte Prediger",
        answerD = "Roger Williams, ein Kaufmann",
        correctAnswer = 2,
        explanation = "Arthur Dimmesdale, der hochangesehene Prediger, ist der Vater von Pearl. Sein Geheimnis zerfrisst ihn von innen, während Hester öffentlich bestraft wird.",
        difficulty = 2,
        funFact = "'The Scarlet Letter' erschien 1850 und gilt als erster großer amerikanischer Roman. Hawthorne widmete es seinem Freund Herman Melville, der wenig später 'Moby Dick' schrieb."
    ),

    // Question 36 — Dostojewski: Schuld und Sühne — Erscheinungsjahr
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "In welchem Jahr erschien Dostojewskis 'Schuld und Sühne'?",
        answerA = "1859",
        answerB = "1871",
        answerC = "1880",
        answerD = "1866",
        correctAnswer = 3,
        explanation = "'Schuld und Sühne' erschien 1866 als Fortsetzungsroman in der Zeitschrift 'Russkij Westnik'. Es ist Dostojewskis erster großer Roman nach seiner Verbannung nach Sibirien.",
        difficulty = 2,
        funFact = "Dostojewski schrieb 'Schuld und Sühne' unter extremem Zeitdruck — er hatte einen Verlagsvertrag unterschrieben, der ihn verpflichtete, bis Ende 1866 zu liefern, sonst hätte er die Rechte verloren."
    ),

    // Question 37 — Émile Zola: Nana
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welchem sozialen Milieu entstammt die Protagonistin Nana in Zolas gleichnamigem Roman (1880)?",
        answerA = "Sie ist die Tochter eines Arbeiters und wird zur Kurtisane und Theaterschauspielerin",
        answerB = "Sie ist die Tochter eines Bankiers, der bankrott geht",
        answerC = "Sie kommt aus der Provinz und wird Gouvernante",
        answerD = "Sie ist die uneheliche Tochter eines Grafen",
        correctAnswer = 0,
        explanation = "Nana (Anna Coupeau) ist die Tochter des Trinkers Coupeau aus dem Arbeitermilieu. Sie steigt zur gefeierten Theaterschauspielerin und Kurtisane auf, die die Männer der Pariser Oberschicht ruiniert.",
        difficulty = 2,
        funFact = "Nana ist der 9. Band von Zolas Rougon-Macquart-Zyklus. Sie erscheint bereits als Kind in 'L'Assommoir' — Zola zeigt, wie das soziale Milieu über Generationen wirkt."
    ),

    // Question 38 — Tolstoi: Anna Karenina — Thema
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Mit welchem berühmten Satz beginnt Leo Tolstois Roman 'Anna Karenina' (1878)?",
        answerA = "'Alle Menschen sind gleich, nur manche sind gleicher.'",
        answerB = "'Alle glücklichen Familien gleichen einander, jede unglückliche ist auf ihre eigene Weise unglücklich.'",
        answerC = "'Es war der schlechteste aller Zeiten, und es war der beste aller Zeiten.'",
        answerD = "'Viele Jahre später, vor dem Erschießungskommando, sollte sich Oberst Aureliano Buendía...'",
        correctAnswer = 1,
        explanation = "Dieser Eröffnungssatz von 'Anna Karenina' gilt als einer der bekanntesten der Weltliteratur und stellt das zentrale Thema des Romans vor.",
        difficulty = 2,
        funFact = "'Anna Karenina' erschien 1877/78 und erzählt von Anna, die ihren Ehemann für den Offizier Wronskij verlässt — und dafür von der Gesellschaft geächtet wird."
    ),

    // Question 39 — Victor Hugo: Erscheinungsjahr Les Misérables
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "In welchem Jahr erschien Victor Hugos 'Les Misérables'?",
        answerA = "1831",
        answerB = "1848",
        answerC = "1862",
        answerD = "1874",
        correctAnswer = 2,
        explanation = "'Les Misérables' erschien 1862 in fünf Bänden gleichzeitig in mehreren Ländern. Hugo arbeitete über 20 Jahre daran, unterbrochen durch sein politisches Exil.",
        difficulty = 2,
        funFact = "Als Victor Hugo seinen Verleger fragte, wie das Buch sich verkaufe, schrieb er nur '?' — der Verleger antwortete: '!'  Es war ein sofortiger Bestseller."
    ),

    // Question 40 — Dickens: David Copperfield
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welcher Dickens-Roman (1850) gilt als stark autobiografisch und beginnt mit den Worten 'Ob ich der Held meines eigenen Lebens sein werde'?",
        answerA = "Große Erwartungen",
        answerB = "Bleak House",
        answerC = "Dombey und Sohn",
        answerD = "David Copperfield",
        correctAnswer = 3,
        explanation = "'David Copperfield' (1850) ist Dickens' eigener Lieblingsroman und stark autobiografisch. Dickens arbeitete als Kind in einer Schuhcreme-Fabrik — wie David in der Flaschenetikettenfabrik.",
        difficulty = 2,
        funFact = "Sigmund Freud nannte 'David Copperfield' seinen Lieblingsroman. Das Buch beeinflusste unzählige Autoren, darunter Franz Kafka."
    ),

    // Question 41 — Bronte: Jane Eyre — Charlotte
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wer schrieb den Roman 'Jane Eyre' (1847), der die Geschichte einer Waise und Gouvernante erzählt?",
        answerA = "Charlotte Brontë",
        answerB = "Emily Brontë",
        answerC = "Anne Brontë",
        answerD = "Elizabeth Gaskell",
        correctAnswer = 0,
        explanation = "'Jane Eyre' wurde 1847 von Charlotte Brontë unter dem Pseudonym Currer Bell veröffentlicht — im gleichen Jahr wie Emilys 'Sturmhöhe' unter Ellis Bell.",
        difficulty = 2,
        funFact = "Alle drei Brontë-Schwestern veröffentlichten 1847 ihre ersten Romane: Charlotte ('Jane Eyre'), Emily ('Wuthering Heights') und Anne ('Agnes Grey')."
    ),

    // Question 42 — Dostojewski: Der Idiot — Myschkin
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wie heißt der epileptische Fürst, der in Dostojewskis Roman 'Der Idiot' (1869) als vollkommen guter Mensch dargestellt wird?",
        answerA = "Fürst Bolkonski",
        answerB = "Fürst Myschkin",
        answerC = "Fürst Rasumichin",
        answerD = "Fürst Porfirij",
        correctAnswer = 1,
        explanation = "Fürst Lew Nikolajewitsch Myschkin kehrt aus der Schweiz nach Russland zurück. Dostojewski versuchte, einen 'vollkommen schönen Menschen' zu gestalten — der an der Gesellschaft scheitert.",
        difficulty = 2,
        funFact = "Dostojewski litt selbst an Epilepsie — er nutzte seine persönlichen Erfahrungen mit der Krankheit für die Darstellung von Fürst Myschkin."
    ),

    // Question 43 — Zola: Rougon-Macquart
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wie viele Romane umfasst Émile Zolas Zyklus 'Les Rougon-Macquart', zu dem auch 'Germinal' und 'Nana' gehören?",
        answerA = "5 Romane",
        answerB = "12 Romane",
        answerC = "20 Romane",
        answerD = "35 Romane",
        correctAnswer = 2,
        explanation = "Zolas Rougon-Macquart-Zyklus umfasst genau 20 Romane, die zwischen 1871 und 1893 erschienen. Sie verfolgen eine Familie über mehrere Generationen im Zweiten Kaiserreich.",
        difficulty = 2,
        funFact = "Zola konzipierte den gesamten Zyklus mit 20 Bänden bereits 1868, bevor er das erste Buch schrieb — als Pendant zu Balzacs 'Comédie Humaine'."
    ),

    // Question 44 — Melville: Moby Dick — Schiff
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wie heißt das Walfangschiff, auf dem Kapitän Ahab in 'Moby Dick' seine Jagd unternimmt?",
        answerA = "The Rachel",
        answerB = "The Delight",
        answerC = "The Jeroboam",
        answerD = "The Pequod",
        correctAnswer = 3,
        explanation = "Das Schiff heißt 'Pequod' — benannt nach dem Pequot-Indianerstamm, der fast ausgerottet wurde. Es wird am Ende des Romans ebenfalls vernichtet.",
        difficulty = 2,
        funFact = "Die 'Rachel' ist das letzte Schiff, das die Pequod trifft — es sucht nach vermissten Besatzungsmitgliedern und rettet am Ende den einzigen Überlebenden Ismael."
    ),

    // Question 45 — Hugo: Notre-Dame de Paris
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie heißt der bucklige Glöckner in Victor Hugos Roman 'Notre-Dame de Paris' (1831)?",
        answerA = "Quasimodo",
        answerB = "Frollo",
        answerC = "Phoebus",
        answerD = "Gringoire",
        correctAnswer = 0,
        explanation = "Quasimodo ist der taubstumme, bucklige Glöckner von Notre-Dame. Er verehrt die Zigeunerin Esmeralda und opfert sich für sie.",
        difficulty = 2,
        funFact = "Victor Hugo schrieb 'Notre-Dame de Paris' auch, um auf den schlechten Zustand der mittelalterlichen Kathedrale aufmerksam zu machen — das Buch löste eine Restaurierungsbewegung aus."
    ),

    // Question 46 — Austen: Stolz und Vorurteil — Bennet-Schwestern Anzahl
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wie viele Töchter hat Mrs. Bennet in Jane Austens 'Stolz und Vorurteil'?",
        answerA = "Drei",
        answerB = "Fünf",
        answerC = "Vier",
        answerD = "Sechs",
        correctAnswer = 1,
        explanation = "Mrs. Bennet hat fünf Töchter: Jane, Elizabeth, Mary, Kitty (Catherine) und Lydia. Ihr größtes Ziel im Leben ist es, alle fünf gut zu verheiraten.",
        difficulty = 2,
        funFact = "Da die Bennet-Töchter das Familiengut nicht erben können (es fällt an den nächsten männlichen Verwandten Mr. Collins), sind gute Heiraten für die Familie überlebenswichtig."
    ),

    // Question 47 — Hawthorne: Erscheinungsjahr Scarlet Letter
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "In welchem historischen Jahrhundert spielt die Handlung von Hawthornes 'The Scarlet Letter', obwohl der Roman 1850 erschien?",
        answerA = "Im 18. Jahrhundert",
        answerB = "Im 16. Jahrhundert",
        answerC = "Im 17. Jahrhundert (Puritanisches Neuengland)",
        answerD = "Im frühen 19. Jahrhundert",
        correctAnswer = 2,
        explanation = "Die Handlung spielt im puritanischen Boston des 17. Jahrhunderts, ungefähr 1642–1649. Hawthorne griff auf die strengen puritanischen Moralvorstellungen seiner Vorfahren zurück.",
        difficulty = 2,
        funFact = "Hawthorne fügte seinem Namen ein 'w' hinzu, um sich von seinem Vorfahren John Hathorne zu distanzieren — einem Richter bei den Hexenprozessen von Salem 1692."
    ),

    // Question 48 — Dickens: Große Erwartungen — Pip
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "In Dickens' 'Große Erwartungen' (1861): Wer enthüllt sich am Ende als Pips geheimnisvoller Wohltäter?",
        answerA = "Miss Havisham, die exzentrische reiche Dame",
        answerB = "Der Anwalt Mr. Jaggers",
        answerC = "Estella, in die Pip verliebt ist",
        answerD = "Abel Magwitch, der entflohene Sträfling",
        correctAnswer = 3,
        explanation = "Pip glaubt lange, Miss Havisham sei sein Wohltäter. Doch es enthüllt sich, dass der Sträfling Magwitch, dem Pip als Kind half, sein gesamtes Vermögen für Pips Bildung einsetzte.",
        difficulty = 2,
        funFact = "Dickens schrieb ursprünglich ein unglückliches Ende für 'Große Erwartungen', änderte es aber auf Druck seines Freundes Bulwer-Lytton zu einem offeneren Schluss."
    ),

    // Question 49 — Poe: Untergang des Hauses Usher
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welches Motiv steht im Mittelpunkt von Edgar Allan Poes Erzählung 'Der Untergang des Hauses Usher' (1839)?",
        answerA = "Der physische und geistige Verfall eines adeligen Zwillingspaares und ihres Familienhauses",
        answerB = "Die Jagd auf einen entflohenen Mörder in einem Schloss",
        answerC = "Die Geschichte eines falschen Detektivs",
        answerD = "Ein Schatz, der in den Mauern eines alten Hauses versteckt ist",
        correctAnswer = 0,
        explanation = "Roderick und Madeline Usher verfallen körperlich und geistig — parallel verfällt ihr Haus. Als Madeline lebendig begraben wird und zurückkehrt, stürzt das Haus zusammen.",
        difficulty = 2,
        funFact = "Poe gilt als Meister der Schauerliteratur. 'Der Untergang des Hauses Usher' ist seine bekannteste Erzählung und beeinflusste H.P. Lovecraft und Stephen King."
    ),

    // Question 50 — Brontë: Sturmhöhe — Erscheinungsjahr und Pseudonym
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Unter welchem Pseudonym veröffentlichte Emily Brontë 'Sturmhöhe' (Wuthering Heights) im Jahr 1847?",
        answerA = "Acton Bell",
        answerB = "Ellis Bell",
        answerC = "Currer Bell",
        answerD = "George Eliot",
        correctAnswer = 1,
        explanation = "Emily Brontë veröffentlichte 'Sturmhöhe' unter dem männlichen Pseudonym Ellis Bell, um nicht als Frau abgestempelt zu werden. Ihre Schwestern wählten Currer (Charlotte) und Acton (Anne).",
        difficulty = 2,
        funFact = "'Sturmhöhe' war zur Zeit der Veröffentlichung skandalös und wurde von Kritikern abgelehnt. Heute gilt es als eines der größten Liebesromane der Weltliteratur."
    )

)
