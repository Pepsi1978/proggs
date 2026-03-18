package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsMaster3(): List<Question> = listOf(

    // Question 1 — First Defenestration of Prague 1419: date
    Question(
        categoryId = 3,
        questionText = "Am welchem genauen Datum fand der erste Prager Fenstersturz statt, bei dem hussitische Demonstranten Ratsherren aus dem Neustädter Rathaus warfen?",
        answerA = "30. Juli 1419",
        answerB = "23. Mai 1419",
        answerC = "6. Juli 1419",
        answerD = "14. September 1419",
        correctAnswer = 0, // A
        explanation = "Am 30. Juli 1419 stürmte eine hussitische Menge unter Jan Žižka das Neustädter Rathaus in Prag und warf sieben Ratsherren aus dem Fenster. Alle starben durch den Sturz oder wurden von der aufgebrachten Menge getötet. Dieses Ereignis löste die Hussitenkriege aus.",
        difficulty = 5,
        funFact = "König Wenzel IV. soll wenige Tage nach dem Fenstersturz an einem Schlaganfall gestorben sein — der Schock über das Ereignis gilt als möglicher Auslöser."
    ),

    // Question 2 — First Defenestration: Žižka's role
    Question(
        categoryId = 3,
        questionText = "Welcher hussitische Führer leitete den Marsch zum Neustädter Rathaus beim ersten Prager Fenstersturz 1419?",
        answerA = "Jan Hus",
        answerB = "Hieronymus von Prag",
        answerC = "Jan Žižka",
        answerD = "Prokop der Große",
        correctAnswer = 2, // C
        explanation = "Jan Žižka von Trocnov (ca. 1360–1424) führte die hussitische Prozession, die nach dem Angriff auf ihre Brüder in einer Kirche das Neustädter Rathaus stürmte. Er war damals bereits ein erfahrener Kriegsmann und wurde später zum gefürchtetsten Heerführer der Hussitenkriege.",
        difficulty = 5,
        funFact = "Žižka verlor im Laufe seines Lebens beide Augen durch Verletzungen, führte aber seine Armeen auch als vollständig Blinder zum Sieg."
    ),

    // Question 3 — Second Defenestration of Prague 1618: exact date
    Question(
        categoryId = 3,
        questionText = "Am welchem Datum fand der zweite Prager Fenstersturz statt, der als Auftakt des Dreißigjährigen Krieges gilt?",
        answerA = "23. Mai 1618",
        answerB = "17. März 1618",
        answerC = "4. August 1618",
        answerD = "12. Juni 1618",
        correctAnswer = 0, // A
        explanation = "Am 23. Mai 1618 warfen protestantische böhmische Adlige die kaiserlichen Statthalter Martinitz und Slavata sowie deren Sekretär Fabricius aus einem Fenster der Prager Burg (Hradschin). Alle drei überlebten den 17-Meter-Sturz.",
        difficulty = 5,
        funFact = "Katholische Quellen behaupteten, die drei seien durch ein Wunder der Jungfrau Maria gerettet worden; protestantische Pamphlete erklärten, sie hätten auf einem Misthaufen gelandet."
    ),

    // Question 4 — Second Defenestration: the three victims
    Question(
        categoryId = 3,
        questionText = "Wer waren die drei Männer, die beim zweiten Prager Fenstersturz 1618 aus dem Fenster geworfen wurden?",
        answerA = "Martinitz, Slavata und der Sekretär Fabricius",
        answerB = "Lobkowitz, Thurn und Trčka",
        answerC = "Wallenstein, Dietrichstein und Schwarzenberg",
        answerD = "Collalto, Lichtenstein und Questenberg",
        correctAnswer = 0, // A
        explanation = "Die Statthalter Jaroslav Bořita von Martinitz und Wilhelm Slavata sowie ihr Sekretär Philipp Fabricius wurden von Graf Thurn und seinen Verbündeten aus dem Fenster geworfen. Alle drei überlebten und flohen nach Wien. Fabricius wurde dafür später geadelt, sein Spottname 'von Hohenfall' blieb haften.",
        difficulty = 5,
        funFact = "Fabricius nannte sich danach stolz 'von Hohenfall' als Anspielung auf seinen Sturz, was ihm aber mehr Spott als Respekt einbrachte."
    ),

    // Question 5 — Hussite Wars: Battle of Vítkov Hill
    Question(
        categoryId = 3,
        questionText = "In welcher entscheidenden Schlacht 1420 besiegte Jan Žižka ein Kreuzfahrerheer unter Kaiser Sigismund bei Prag und sicherte die hussitische Hauptstadt?",
        answerA = "Schlacht bei Lipan",
        answerB = "Schlacht am Veitsberg (Vítkov Hill)",
        answerC = "Schlacht bei Deutsch Brod",
        answerD = "Schlacht von Aussig",
        correctAnswer = 1, // B
        explanation = "Am 14. Juli 1420 verteidigte Žižka mit einer kleinen hussitischen Truppe den Veitsberg (Vítkov Hill) gegen das Kreuzfahrerheer Sigismunds. Der kaiserliche Angriff scheiterte, und Sigismund musste sich zurückziehen. Der Hügel wurde danach in Žižkov umbenannt, heute ein Stadtteil Prags.",
        difficulty = 5,
        funFact = "Auf dem Žižkov-Hügel steht heute das größte Reiterstandbild der Welt — die kolossale Bronzestatue von Jan Žižka."
    ),

    // Question 6 — Hussite Wars: Four Articles of Prague
    Question(
        categoryId = 3,
        questionText = "Was war der erste der Vier Prager Artikel von 1420, das Kernprogramm der hussitischen Bewegung?",
        answerA = "Freiheit der hussitischen Predigt",
        answerB = "Laienkelch beim Abendmahl (Kommunion unter beiden Gestalten)",
        answerC = "Abschaffung des Papsttums",
        answerD = "Säkularisierung aller Kirchengüter",
        correctAnswer = 1, // B
        explanation = "Der erste und wichtigste der Vier Prager Artikel forderte die Kommunion unter beiden Gestalten (sub utraque specie) — also Brot UND Wein für Laien beim Abendmahl. Daher hießen die Gemäßigten auch 'Utraquisten' oder 'Kalixtiner'. Der Kelch wurde zum Symbol der hussitischen Bewegung.",
        difficulty = 5,
        funFact = "Das Konzil von Konstanz hatte 1415 den Laienkelch ausdrücklich verboten — die hussitische Forderung war also direkte Revolte gegen konziliare Autorität."
    ),

    // Question 7 — Hussite Wars: Battle of Lipan 1434
    Question(
        categoryId = 3,
        questionText = "Welche hussitische Fraktion wurde in der entscheidenden Schlacht bei Lipan 1434 von den gemäßigten Utraquisten und der böhmischen Adelspartei vernichtend geschlagen?",
        answerA = "Die Adamiten",
        answerB = "Die Taboriten und Waisen (Orphans)",
        answerC = "Die Orebiten",
        answerD = "Die Pikarden",
        correctAnswer = 1, // B
        explanation = "Bei Lipan am 30. Mai 1434 besiegten die gemäßigten Utraquisten in Verbindung mit dem Hochadel die radikalen Taboriten und die Waisen (auch 'Orphans', Anhänger des verstorbenen Prokop des Kleinen). Prokop der Große fiel in der Schlacht. Damit endete die radikale Phase der Hussitenkriege.",
        difficulty = 5,
        funFact = "Die Taboriten hatten ihren Namen von der Stadt Tábor, die sie 1420 als gottesstaatliche Gemeinschaft gegründet hatten — benannt nach dem biblischen Berg Tabor."
    ),

    // Question 8 — Hussite Wars: Wagenburg tactics
    Question(
        categoryId = 3,
        questionText = "Welche revolutionäre militärische Taktik entwickelten die Hussiten unter Žižka, die befestigte Wagenkreise als mobile Festungen einsetzte?",
        answerA = "Tercio-Formation",
        answerB = "Wagenburg",
        answerC = "Schiltron",
        answerD = "Pavesenschild-Reihe",
        correctAnswer = 1, // B
        explanation = "Die hussitische Wagenburg war ein taktisches System, bei dem Kampfwagen zu einer Verteidigungsstellung verketttet wurden. Hinter den Wagen schossen Armbrustschützen und frühe Feuerwaffen. Diese Taktik war so effektiv, dass sie fünf Kreuzzüge gegen Böhmen zurückschlug und in ganz Europa imitiert wurde.",
        difficulty = 5,
        funFact = "Die hussitensche Wagenburg-Taktik beeinflusste direkt die spätere Entwicklung des mobilen Artillerieeinsatzes und gilt als Vorläufer moderner Panzertaktiken."
    ),

    // Question 9 — Polish-Lithuanian Commonwealth: Union of Lublin
    Question(
        categoryId = 3,
        questionText = "Was war die wichtigste institutionelle Neuerung der Union von Lublin 1569, die Polen und Litauen zur Adelsrepublik verband?",
        answerA = "Einführung einer gemeinsamen Streitmacht unter einem Oberbefehlshaber",
        answerB = "Schaffung eines gemeinsamen Sejm mit einheitlicher Gesetzgebungshoheit",
        answerC = "Vereinigung beider Staatskanzleien unter dem polnischen Kanzler",
        answerD = "Übertragung der Kurland-Provinzen an die polnische Krone",
        correctAnswer = 1, // B
        explanation = "Die Union von Lublin (1. Juli 1569) schuf einen gemeinsamen polnisch-litauischen Sejm (Parlament) mit gemeinsamer Gesetzgebung und gemeinsamer Außenpolitik. Gleichzeitig wurden Volhynien, Podolien und Kiev vom litauischen Großfürstentum direkt der polnischen Krone unterstellt — ein enormer Machtverlust für Litauen.",
        difficulty = 5,
        funFact = "Litauische Magnaten lehnten die Union zunächst ab und verließen den Lublin-Sejm, kehrten aber nach polnischen Drohungen als schwächere Partei zurück."
    ),

    // Question 10 — Polish-Lithuanian Commonwealth: Liberum Veto
    Question(
        categoryId = 3,
        questionText = "Was bedeutete das 'Liberum Veto' im polnisch-litauischen Sejm, das erstmals 1652 von Władysław Siciński angewendet wurde?",
        answerA = "Das Recht des Königs, jeden Sejm-Beschluss zu vetieren",
        answerB = "Das Recht eines einzelnen Abgeordneten, durch seinen Einspruch den gesamten Sejm aufzulösen und alle Beschlüsse zu annullieren",
        answerC = "Das Recht des Adels, ausländische Kandidaten für den Königsthron abzulehnen",
        answerD = "Das Veto-Recht des Senats gegen Entscheidungen der Abgeordnetenkammer",
        correctAnswer = 1, // B
        explanation = "Das Liberum Veto ('Ich verbiete es frei') erlaubte jedem einzelnen Sejm-Abgeordneten, nicht nur einen Beschluss zu blockieren, sondern die gesamte Parlamentssitzung zu beenden und alle bereits gefassten Beschlüsse rückgängig zu machen. Władysław Siciński nutzte es 1652 erstmals; danach wurde es zur Lähmungswaffe fremder Mächte.",
        difficulty = 5,
        funFact = "Zwischen 1652 und 1764 wurden 53 von 55 Sejm-Sitzungen durch das Liberum Veto gesprengt — oft von ausländisch besoldeten Abgeordneten."
    ),

    // Question 11 — Polish-Lithuanian Commonwealth: Golden Freedom
    Question(
        categoryId = 3,
        questionText = "Wie bezeichnete der polnisch-litauische Adel (Szlachta) das System seiner weitreichenden politischen Privilegien, das Königswahl, Liberum Veto und Rechtsimmunität einschloss?",
        answerA = "Szlachta Republika",
        answerB = "Złota Wolność (Goldene Freiheit)",
        answerC = "Pacta Conventa",
        answerD = "Neminem Captivabimus",
        correctAnswer = 1, // B
        explanation = "Die 'Złota Wolność' (Goldene Freiheit) bezeichnete die Gesamtheit adliger Privilegien: Wahlkönigtum (keine Erbmonarchie), Liberum Veto, Recht auf Konföderation (Rokosz) gegen den König, und das Privileg 'Neminem captivabimus' (kein Adliger ohne Gerichtsurteil verhaftbar). Dieses System machte die Szlachta zum souveränen Stand.",
        difficulty = 5,
        funFact = "Die Szlachta umfasste ca. 10% der polnischen Bevölkerung — deutlich mehr als der westeuropäische Adel, was das System schwer kontrollierbar machte."
    ),

    // Question 12 — Swedish Empire: Stormaktstiden definition
    Question(
        categoryId = 3,
        questionText = "Was bezeichnet der schwedische Begriff 'Stormaktstiden' und welchen Zeitraum umfasst er?",
        answerA = "Die Wikinger-Ära, ca. 793–1066",
        answerB = "Die schwedische Großmachtzeit, ca. 1611–1721",
        answerC = "Die Union von Kalmar, 1397–1521",
        answerD = "Die Reformationszeit in Schweden, 1527–1600",
        correctAnswer = 1, // B
        explanation = "Stormaktstiden ('Großmachtzeit') bezeichnet Schwedens Aufstieg zur Hegemonialmacht im Ostseeraum, beginnend unter Gustav II. Adolf (reg. 1611–1632) und endend mit der Niederlage im Großen Nordischen Krieg beim Frieden von Nystad 1721. In dieser Zeit kontrollierte Schweden weite Teile der Ostseeküste.",
        difficulty = 5,
        funFact = "Auf dem Höhepunkt der Stormaktstiden war Schweden mit ca. 2,5 Millionen Einwohnern bevölkerungsmäßig kleiner als die damalige Stadt Paris."
    ),

    // Question 13 — Swedish Empire: Battle of Lützen 1632
    Question(
        categoryId = 3,
        questionText = "Bei welcher Schlacht 1632 fiel Gustav II. Adolf von Schweden, obwohl seine Truppen den Sieg davontrugen?",
        answerA = "Schlacht bei Breitenfeld",
        answerB = "Schlacht bei Lützen",
        answerC = "Schlacht am Weißen Berg",
        answerD = "Schlacht bei Nördlingen",
        correctAnswer = 1, // B
        explanation = "In der Schlacht bei Lützen am 16. November 1632 standen sich die schwedisch-protestantische Armee und Wallensteins kaiserliches Heer gegenüber. Gustav II. Adolf ritt zu nahe an feindliche Musketiere und wurde erschossen. Seine Truppen gewannen trotzdem die Schlacht, aber Schweden verlor seinen überragenden Feldherrn.",
        difficulty = 5,
        funFact = "Gustaves Tod wurde als 'Löwe von Mitternacht' in apokalyptischen Prophezeiungen des 17. Jahrhunderts vorhergesagt — eine Figur, die er selbst verkörpert haben soll."
    ),

    // Question 14 — Swedish Empire: dominium maris baltici
    Question(
        categoryId = 3,
        questionText = "Welchen lateinischen Begriff verwendeten schwedische Staatstheoretiker des 17. Jahrhunderts für Schwedens angestrebte Kontrolle über den gesamten Ostseehandel?",
        answerA = "Mare Nostrum",
        answerB = "Dominium Maris Baltici",
        answerC = "Imperium Septentrionale",
        answerD = "Pax Suecica",
        correctAnswer = 1, // B
        explanation = "Das 'Dominium Maris Baltici' ('Herrschaft über das Baltische Meer') war das schwedische Staatsziel, alle Ostseeküsten zu kontrollieren und damit Zölle auf den enormen Ostseehandel zu erheben. Durch Frieden von Roskilde (1658), Kardis (1661) und Oliva (1660) kam Schweden diesem Ziel nahe, erreichte es aber nie vollständig.",
        difficulty = 5,
        funFact = "Der Ostseehandel des 17. Jahrhunderts, vor allem Getreide und Holz, war so wertvoll, dass ihn Zeitgenossen als 'Mutterbrust der europäischen Wirtschaft' bezeichneten."
    ),

    // Question 15 — War of Spanish Succession: Partition Treaties
    Question(
        categoryId = 3,
        questionText = "Welcher der beiden Haager Teilungsverträge (1698/1700) sah vor, dass der habsburgische Erzherzog Karl das spanische Kernreich erhalten sollte, bevor Karl II. ein anderes Testament machte?",
        answerA = "Erster Teilungsvertrag (1698)",
        answerB = "Zweiter Teilungsvertrag (1700)",
        answerC = "Weder — beide Verträge sahen Bayern als Haupterben vor",
        answerD = "Keiner — beide Verträge teilten Spanien zwischen Frankreich und Habsburg",
        correctAnswer = 1, // B
        explanation = "Im zweiten Haager Teilungsvertrag (3. März 1700) einigten sich Ludwig XIV. und Wilhelm III. darauf, dass Erzherzog Karl (der spätere Kaiser Karl VI.) Spanien, Spanisch-Amerika und die Spanischen Niederlande erhalten sollte, während Frankreich Neapel, Sizilien und Mailand bekäme. Karl II. ignorierte dies und vermachte alles Philipp von Anjou.",
        difficulty = 5,
        funFact = "Der bayerische Erbprinz Joseph Ferdinand, der im ersten Teilungsvertrag das spanische Haupterbe bekommen sollte, starb 1699 mit sechs Jahren — was den zweiten Teilungsvertrag notwendig machte."
    ),

    // Question 16 — War of Spanish Succession: Battle of Blenheim
    Question(
        categoryId = 3,
        questionText = "Welcher strategische Effekt der Schlacht bei Höchstädt/Blenheim (1704) rettete das Heilige Römische Reich vor dem Zusammenbruch?",
        answerA = "Frankreich wurde zur Aufgabe Elsass-Lothringens gezwungen",
        answerB = "Der Vormarsch der bayerisch-französischen Armee auf Wien wurde gestoppt und Bayern aus dem Krieg gedrängt",
        answerC = "Die spanischen Niederlande wurden endgültig von Frankreich getrennt",
        answerD = "Portugal trat in die Große Allianz ein",
        correctAnswer = 1, // B
        explanation = "Die Verbündeten unter Marlborough und Prinz Eugen vernichteten bei Höchstädt/Blenheim am 13. August 1704 ein 56.000 Mann starkes bayerisch-französisches Heer. Wien war gerettet, Bayern wurde besetzt, und Kurfürst Maximilian Emanuel floh ins Exil. Es war die erste französische Niederlage unter Ludwig XIV. in einer großen Feldschlacht.",
        difficulty = 5,
        funFact = "Marlborough ließ die Siegesnachricht auf einer Taschenrechnung an seine Frau schreiben — dieses Dokument ist noch heute erhalten."
    ),

    // Question 17 — War of Spanish Succession: Peace of Utrecht details
    Question(
        categoryId = 3,
        questionText = "Welche Bestimmung des Friedens von Utrecht 1713 regelte die Handelsprivilegien Großbritanniens in Spanisch-Amerika und war wirtschaftlich besonders bedeutsam?",
        answerA = "Der Vertrag von Methuen",
        answerB = "Der Asiento-Vertrag",
        answerC = "Die Newfoundland-Klausel",
        answerD = "Der Gibraltar-Schutzvertrag",
        correctAnswer = 1, // B
        explanation = "Der Asiento de Negros, den Großbritannien im Utrechter Frieden erhielt, gewährte das Monopolrecht, für 30 Jahre jährlich 4.800 versklavte Afrikaner nach Spanisch-Amerika zu liefern. Zusätzlich durfte einmal jährlich ein Handelsschiff ('Navío de Permiso') mit Waren segeln. Der Asiento war riesig profitabel für die South Sea Company.",
        difficulty = 5,
        funFact = "Die Spekulation um den Asiento-Vertrag war ein Hauptgrund für die South Sea Bubble von 1720 — eine der ersten großen Börsenblasen der Geschichte."
    ),

    // Question 18 — War of Spanish Succession: Catalan resistance
    Question(
        categoryId = 3,
        questionText = "Was geschah mit Katalonien nach dem Fall von Barcelona am 11. September 1714, dem Ende des spanischen Erbfolgekriegs auf der iberischen Halbinsel?",
        answerA = "Katalonien erhielt einen Sonderstatus als autonomes Fürstentum",
        answerB = "Die katalanischen Institutionen (Generalitat, Corts) wurden durch die Decretos de Nueva Planta aufgelöst und zentralisiert",
        answerC = "Katalonien wurde als Entschädigung an Österreich abgetreten",
        answerD = "Die katalanische Sprache wurde verboten, das Catalanische Parlament blieb erhalten",
        correctAnswer = 1, // B
        explanation = "Nach dem Fall Barcelonas (11. September 1714 — heute katalanischer Nationalfeiertag) erließ Philipp V. die 'Decretos de Nueva Planta', die die katalanischen Selbstverwaltungsrechte abschafften, die Generalitat und die Corts auflösten, Kastilisch als Verwaltungssprache einführten und Kastilisches Recht einführten.",
        difficulty = 5,
        funFact = "Der 11. September als katalanischer Nationalfeiertag erinnert an die Kapitulation und den Widerstand — nicht an einen Sieg."
    ),

    // Question 19 — Great Northern War: Battle of Narva 1700
    Question(
        categoryId = 3,
        questionText = "Mit welcher taktischen Entscheidung gewann Karl XII. von Schweden die Schlacht bei Narva am 19. November 1700 gegen ein zahlenmäßig weit überlegenes russisches Heer?",
        answerA = "Nachtangriff bei dichtem Schneesturm von Westen, der das russische Heer blendete",
        answerB = "Umgehung der russischen Flanken durch aufgeteilte Kavallerie",
        answerC = "Vortäuschung eines Rückzugs und anschließende Umzingelung",
        answerD = "Einsatz schwerer Artillerie aus befestigter Stellung",
        correctAnswer = 0, // A
        explanation = "Karl XII. griff am 19. November 1700 bei einem heftigen Schneesturm an, der dem russischen Heer ins Gesicht blies. Die 8.000 Schweden überrannten die 40.000 Russen, die in Panik verfielen. Fast die gesamte russische Artillerie und Führung fiel in schwedische Hände. Peter I. floh bereits vor der Entscheidungsschlacht.",
        difficulty = 5,
        funFact = "Peter I. nutzte die erbeuteten schwedischen Kanonen als Symbol der Erneuerung: Er ließ sie einschmelzen und neue Geschütze gießen, die er in späteren Schlachten gegen Schweden einsetzte."
    ),

    // Question 20 — Great Northern War: Battle of Poltava 1709
    Question(
        categoryId = 3,
        questionText = "Was war die unmittelbare Ursache für Karls XII. katastrophale Niederlage bei Poltawa 1709, abgesehen von der numerischen Überlegenheit Russlands?",
        answerA = "Verrat durch hetman Mazepa",
        answerB = "Erschöpfte Munition nach der langen ukrainischen Winterkampagne und Verwundung Karls",
        answerC = "Fehlendes schwedisches Entsatzheer aus Pommern",
        answerD = "Überläufer verraten den schwedischen Angriffsplan",
        correctAnswer = 1, // B
        explanation = "Nach dem verheerenden russischen Winter 1708/09 hatte die schwedische Armee akuten Munitionsmangel. Karl XII. selbst war kurz vor Poltawa durch eine Fußverletzung kampfunfähig. Die geschwächte, erschöpfte Armee griff mit fast leeren Pulvermagazinen an. Hetman Mazepas Verrat an Peter hatte zusätzlich die Versorgungsbase vernichtet.",
        difficulty = 5,
        funFact = "Karl XII. entkam nach Poltawa in das Osmanische Reich und lebte fünf Jahre in Bender (heute Moldau), wo er osmanische Sultane zu neuem Krieg gegen Russland zu überreden versuchte."
    ),

    // Question 21 — Great Northern War: Peace of Nystad 1721
    Question(
        categoryId = 3,
        questionText = "Welche Provinzen verlor Schweden durch den Frieden von Nystad 1721 an Russland, womit die schwedische Großmachtstellung endgültig endete?",
        answerA = "Norwegen und Dänemark",
        answerB = "Livland, Estland, Ingermanland und Teile Kareliens",
        answerC = "Pommern, Rügen und Wismar",
        answerD = "Finnland und die Ålandinseln",
        correctAnswer = 1, // B
        explanation = "Im Frieden von Nystad (10. September 1721) trat Schweden Livland, Estland, Ingermanland, einen Teil Kareliens und einige Inseln an Russland ab. Damit verlor Schweden seinen gesamten östlichen Ostseebesitz. Russland bezahlte dafür zwei Millionen Reichstaler und gab Finnland zurück. Peter I. nahm daraufhin den Titel 'Kaiser aller Reußen' an.",
        difficulty = 5,
        funFact = "Der schwedische Reichsrat erhielt die Nachricht vom Frieden noch vor dem König — und ratifizierte ihn, bevor Karl XII.' Nachfolger überhaupt konsultiert werden konnte."
    ),

    // Question 22 — Partitions of Poland: First Partition 1772
    Question(
        categoryId = 3,
        questionText = "Welche Gebiete erhielt Preußen bei der ersten Teilung Polens 1772, und warum war dies strategisch besonders wichtig für Friedrich den Großen?",
        answerA = "Großpolen (Poznań) als Verbindung zu Schlesien",
        answerB = "Westpreußen (ohne Danzig und Thorn), was Ostpreußen mit dem Hauptkönigreich verband",
        answerC = "Ermland und Masuren, was den Zugang zur Ostsee sicherte",
        answerD = "Schlesien und das Breslauer Gebiet als Erweiterung des früheren Gewinns",
        correctAnswer = 1, // B
        explanation = "Preußen erhielt bei der ersten Teilung Westpreußen (ohne die Städte Danzig und Thorn, die erst 1793 fielen), was Ostpreußen erstmals mit Brandenburg-Preußen landverbunden verband. Friedrich der Große nannte dies wichtiger als Schlesien, da es die gefährliche Landbrücke ('Polnischer Korridor') schloss, die Ostpreußen isoliert hatte.",
        difficulty = 5,
        funFact = "Friedrich II. nannte Westpreußen im privaten Briefwechsel verächtlich 'Kaschubenland' — sah aber seinen strategischen Wert als unschätzbar an."
    ),

    // Question 23 — Partitions of Poland: Constitution of May 3, 1791
    Question(
        categoryId = 3,
        questionText = "Was war das wichtigste Merkmal der polnischen Verfassung vom 3. Mai 1791, das sie als erste moderne Verfassung Europas auszeichnet?",
        answerA = "Sie schaffte den Adel vollständig ab",
        answerB = "Sie ersetzte das lähmende Liberum Veto durch Mehrheitsbeschluss und stärkte die Exekutivgewalt",
        answerC = "Sie erklärte Polen zur Republik ohne König",
        answerD = "Sie gewährte allen Bauern volle politische Rechte",
        correctAnswer = 1, // B
        explanation = "Die Verfassung vom 3. Mai 1791 — nach der US-Verfassung (1787) und vor der französischen (September 1791) die zweitälteste kodifizierte Verfassung der Neuzeit — schaffte das Liberum Veto ab, führte Mehrheitsentscheidungen ein, stärkte die Exekutive durch eine erbliche Monarchie und gewährte Bürgern (Stadtbewohnern) politische Rechte.",
        difficulty = 5,
        funFact = "Russland und Preußen sahen die Verfassung als revolutionäre Bedrohung und nutzten die Konföderation von Targowica, die gegen die Verfassung gegründet wurde, als Vorwand für die zweite Teilung."
    ),

    // Question 24 — Partitions of Poland: Third Partition 1795
    Question(
        categoryId = 3,
        questionText = "Was unterzeichneten die drei teilenden Mächte im Geheimvertrag zur dritten Polenteilung 1797, das für 123 Jahre galt?",
        answerA = "Das Versprechen, Polen nach dem Ende der Napoleonischen Kriege wiederherzustellen",
        answerB = "Die Verpflichtung, den Namen 'Polen' und 'Königreich Polen' nie wieder offiziell zu verwenden",
        answerC = "Ein gegenseitiges Bündnis gegen jeden, der die Teilung anfechten würde",
        answerD = "Die Auslieferung aller polnischen Patrioten an das jeweils kontrollierende Land",
        correctAnswer = 1, // B
        explanation = "Im Geheimvertrag von 1797 zwischen Russland, Preußen und Österreich verpflichteten sich alle drei Mächte, den Namen 'Königreich Polen' aus allen offiziellen Dokumenten zu streichen und nie wieder zu verwenden. Polen sollte als Staatsbegriff ausgelöscht werden — ein juristisches Auslöschungsversuch, der bis 1918 weitgehend wirksam blieb.",
        difficulty = 5,
        funFact = "Trotz des Verbots lebte der Name 'Polen' in der Bezeichnung 'Herzogtum Warschau' (1807–1815) und 'Königreich Polen' (1815–1831) unter Napoleon bzw. dem russischen Zaren kurzzeitig wieder auf."
    ),

    // Question 25 — Partitions of Poland: Kosciuszko Uprising
    Question(
        categoryId = 3,
        questionText = "Was war der 'Krakauer Akt' vom 24. März 1794, mit dem Tadeusz Kościuszko den Aufstand gegen die Teilungsmächte eröffnete?",
        answerA = "Eine formelle Kriegserklärung an Russland und Preußen",
        answerB = "Kościuszkos Proklamation als Oberster Befehlshaber (Naczelnik) mit diktatorischen Vollmachten zur Landesverteidigung",
        answerC = "Die Abschaffung des Adels und Befreiung der Leibeigenen",
        answerD = "Ein Hilfegesuch an Frankreich und das Osmanische Reich",
        correctAnswer = 1, // B
        explanation = "Im Krakauer Akt proklamierte sich Kościuszko zum 'Naczelnik' (Obersten Befehlshaber) der nationalen Streitkräfte und erhielt diktatorische Vollmachten zur Kriegsführung. Er leistete einen Eid auf die Verfassung von 1791. Das Manifest mobilisierte auch Bauern ('Kosinerists' — Sensenträger), denen er im Połaniec-Manifest Freiheiten versprach.",
        difficulty = 5,
        funFact = "Kościuszko hatte zuvor im amerikanischen Unabhängigkeitskrieg als Ingenieur die Befestigungen bei Saratoga gebaut, die zum Wendepunkt des Krieges beitrugen."
    ),

    // Question 26 — Pugachev's Rebellion: causes
    Question(
        categoryId = 3,
        questionText = "Wer war Jemeljan Pugachow, und welche gesellschaftliche Stellung behauptete er von sich zu haben, um Anhänger zu gewinnen?",
        answerA = "Ein adliger Offizier, der sich als rechtmäßiger Zar Iwan VI. ausgab",
        answerB = "Ein donkosackischer Sotnik, der behauptete, der totgesagte Zar Peter III. zu sein",
        answerC = "Ein ehemaliger Leibeigener, der sich als Sohn Katharinas II. ausgab",
        answerD = "Ein Baschkirenfürst, der sich auf ein altes Herrschaftsrecht berief",
        correctAnswer = 1, // B
        explanation = "Jemeljan Pugachow (ca. 1742–1775) war ein donkosackischer Unteroffizier, der behauptete, der totgeglaubte Zar Peter III. (ermordet 1762) zu sein. Diese Behauptung — typisch für russische 'Prätendentenbewegungen' (Samozvanstwo) — gab seinem Aufstand legitimatorische Kraft und zog Kosaken, Leibeigene, Bashkiren und Ural-Arbeiter an.",
        difficulty = 5,
        funFact = "Pugachows Aufstand erschreckte Katharina die Große so sehr, dass sie die Verwaltungsreform von 1775 (Provinzreform) beschleunigte, um die Kontrolle über die Peripherie zu stärken."
    ),

    // Question 27 — Pugachev's Rebellion: scope
    Question(
        categoryId = 3,
        questionText = "Welche Stadt war das wichtigste Ziel und der Höhepunkt von Pugachows Feldzug 1773–1774, dessen Belagerung seine Armee auf ihrem Höchststand hatte?",
        answerA = "Moskau",
        answerB = "Kasan",
        answerC = "Orenburg",
        answerD = "Astrachan",
        correctAnswer = 2, // C
        explanation = "Orenburg war Pugachows strategisches Hauptziel: Die Festungsstadt kontrollierte die Verbindung nach Sibirien und Mittelasien. Die Belagerung Orenburgs dauerte von Oktober 1773 bis März 1774 und band einen Großteil von Pugachows Armee, die zeitweise 30.000 Mann zählte. Als reguläre Truppen entsandt wurden, hob Pugachow die Belagerung auf.",
        difficulty = 5,
        funFact = "Alexander Puschkin schrieb über den Pugachow-Aufstand sowohl eine historische Studie als auch die Novelle 'Die Hauptmannstochter' — er war historisch-wissenschaftlich fasziniert von der Figur."
    ),

    // Question 28 — Pugachev's Rebellion: defeat and execution
    Question(
        categoryId = 3,
        questionText = "Wie wurde Pugachows Aufstand 1774 letztlich beendet, und welche Strafe erhielt er?",
        answerA = "Er wurde in der offenen Feldschlacht besiegt und starb im Kampf",
        answerB = "Seine eigenen Offiziere lieferten ihn an die zaristischen Behörden aus; er wurde gevierteilt",
        answerC = "Er ergab sich freiwillig gegen ein Begnadigungsversprechen, das gebrochen wurde",
        answerD = "Er wurde an der Wolga von einer Kürassierbrigade überrascht und gefangen genommen",
        correctAnswer = 1, // B
        explanation = "Nach der Niederlage bei Zaritsyn flohen Pugachow und sein Restgefolge über die Wolga. Im August 1774 wurde Pugachow von seinen eigenen Anführern (darunter Iwan Tworohow) gefesselt und den Behörden übergeben — sie hofften auf Begnadigung. Pugachow wurde nach Moskau gebracht, zum Tode verurteilt und am 10. Januar 1775 auf dem Bolotnaya-Platz gevierteilt.",
        difficulty = 5,
        funFact = "Katharina II. ließ den Don Jaik (Ural-Fluss) in 'Ural' umbenennen, um die geographische Erinnerung an das kosackische Widerstandszentrum zu tilgen."
    ),

    // Question 29 — Vendée Uprising: geography and causes
    Question(
        categoryId = 3,
        questionText = "Was war die unmittelbare Ursache für den Ausbruch des Vendée-Aufstands im März 1793?",
        answerA = "Die Hinrichtung König Ludwigs XVI.",
        answerB = "Die Zwangsrekrutierung (levée en masse) von 300.000 Mann durch die Revolutionsregierung",
        answerC = "Das Verbot der katholischen Messe durch den Nationalkonvent",
        answerD = "Die Beschlagnahme von Kirchengütern in der Vendée",
        correctAnswer = 1, // B
        explanation = "Der unmittelbare Auslöser war das Dekret vom 23. Februar 1793, das 300.000 Männer für die Revolutionsarmee einzog. In der Vendée, wo der Widerstand gegen die Dechristianisierung und die Zerstörung traditioneller Gemeindestrukturen bereits brodelte, war dies der Funke. Am 11. März 1793 begann der offene Aufstand in Saint-Florent-le-Vieil.",
        difficulty = 5,
        funFact = "Die Vendée-Bauern nannten ihre royalistische Armee die 'Armée catholique et royale' — sie kämpften explizit sowohl für den König als auch für den alten Glauben."
    ),

    // Question 30 — Vendée Uprising: Colonnes Infernales
    Question(
        categoryId = 3,
        questionText = "Was waren die 'Colonnes Infernales' (Höllische Kolonnen) unter General Turreau, die 1794 in der Vendée operierten?",
        answerA = "Schwere Artilleriekolonnen zur Belagerung vendéischer Festungen",
        answerB = "Systematische Vernichtungskolonnen, die die Vendée mit Mord, Brandschatzung und Massenexekutionen verwüsteten",
        answerC = "Mobile Einheiten zur Verfolgung der geflohenen royalistischen Führung",
        answerD = "Republikanische Propagandaeinheiten zur ideologischen Umerziehung der Bevölkerung",
        correctAnswer = 1, // B
        explanation = "General Louis-Marie Turreau organisierte ab Januar 1794 zwölf 'Colonnes Infernales', die die Vendée in einem Raster abmarschieren sollten. Ihr Befehl: alles niederbrennen, alle Männer töten, Frauen und Kinder deportieren. Historiker schätzen die Opferzahl auf 170.000–250.000 Tote. Ob dies als Genozid qualifiziert werden soll, ist bis heute umstritten.",
        difficulty = 5,
        funFact = "Die Noyades von Nantes (Massenertränkungen unter Jean-Baptiste Carrier) parallelten die Colonnes Infernales: Carrier ließ Tausende in Booten auf der Loire ertränken."
    ),

    // Question 31 — Thermidorian Reaction: 9 Thermidor
    Question(
        categoryId = 3,
        questionText = "Was geschah am 9. Thermidor (27. Juli 1794) im Nationalkonvent, das zum Ende der Herrschaft des Wohlfahrtsausschusses unter Robespierre führte?",
        answerA = "Ein Volksaufstand der Pariser Sansculottes gegen den Konvent",
        answerB = "Eine Koalition von Konventsmitgliedern verhaftete Robespierre und seine Mitstreiter durch Abstimmung im Plenum",
        answerC = "Robespierres Sturz durch einen Militärputsch unter General Barras",
        answerD = "Der Thermidorianer-Aufstand begann in Lyon und griff auf Paris über",
        correctAnswer = 1, // B
        explanation = "Am 9. Thermidor II (27. Juli 1794) unterbrachen Mitglieder des Konvents Robespierres Rede und stimmten für seine Verhaftung. Robespierre, Saint-Just, Couthon und ihre Anhänger wurden verhaftet, am folgenden Tag guillotiniert. Der Sturz war parlamentarisch — keine Massenbewegung, sondern ein Coup von Konventsmitgliedern, die selbst Angst vor der Guillotine hatten.",
        difficulty = 5,
        funFact = "Als Robespierre im Saal verhaftet werden sollte, soll er gerufen haben: 'Die Republik stirbt!' — Tallien antwortete: 'Die Republik stirbt an euch!'"
    ),

    // Question 32 — Thermidorian Reaction: White Terror
    Question(
        categoryId = 3,
        questionText = "Was bezeichnete der 'Weiße Terror' (Terreur Blanche) in der thermidorianischen Periode 1794–1795?",
        answerA = "Die Verfolgung verbleibender Royalisten durch thermidorianische Sicherheitskräfte",
        answerB = "Gewaltwellen gegen frühere Terroristen und Jakobiner, besonders in Lyon, Marseille und dem Rhônetal",
        answerC = "Die royalistische Insurrektion in der Bretagne und Normandie",
        answerD = "Der Volksaufstand der Sansculottes gegen die thermidorianische Regierung",
        correctAnswer = 1, // B
        explanation = "Der Weiße Terror bezeichnet Rachewellen von Opfern des Jakobinischen Terrors (Muscadin-Jugend, Compagnies du Jésus und du Soleil) gegen frühere Terroristen. Besonders brutal in Lyon, Marseille und dem Rhônetal: Jakobiner und frühere Mitglieder von Überwachungsausschüssen wurden ermordet, oft aus Gefängnissen heraus.",
        difficulty = 5,
        funFact = "Die Opfer des Weißen Terrors wurden oft als 'Mitglieder der Compagnie du Jésus' bezeichnet — ein bitterer Witz, da der Jesuitenorden von der Revolution zuvor aufgelöst worden war."
    ),

    // Question 33 — Directory: Constitution of Year III
    Question(
        categoryId = 3,
        questionText = "Welche wichtige Neuerung enthielt die Verfassung des Jahres III (1795), die das Direktorium begründete, um Tyrannei wie unter Robespierre zu verhindern?",
        answerA = "Ein Zweikammerparlament mit Rat der Fünfhundert und Rat der Alten (500 und 250 Mitglieder)",
        answerB = "Ein direktes Volksreferendum über alle wichtigen Gesetze",
        answerC = "Die Abschaffung der Exekutive zugunsten eines rein legislativen Systems",
        answerD = "Eine stärkere Dezentralisierung aller Regierungskompetenzen auf die Départements",
        correctAnswer = 0, // A
        explanation = "Die Verfassung des Jahres III schuf ein Zweikammerparlament: den Rat der Fünfhundert (initiierte Gesetze) und den Rat der Alten (Conseil des Anciens, 250 Mitglieder ab 40 Jahren, verhinderte oder bestätigte Gesetze). Die Exekutive bildete ein fünfköpfiges Direktorium. Dieses System war bewusst auf Machtteilung ausgelegt, erwies sich aber als dysfunktional.",
        difficulty = 5,
        funFact = "Die Mitglieder des Rates der Alten mussten mindestens 40 Jahre alt, verheiratet oder verwitwet sein — Ledige galten als zu unbeständig für das Amt."
    ),

    // Question 34 — Directory: Coup of 18 Fructidor
    Question(
        categoryId = 3,
        questionText = "Was war der Staatsstreich des 18. Fructidor V (4. September 1797), und welche Direktoren initiierten ihn?",
        answerA = "Napoleon Bonaparte annullierte die Wahlergebnisse von 1797 mit Hilfe von General Augereau",
        answerB = "Die Direktoren Barras, Reubell und La Revellière-Lépeaux annullierten royalistische Wahlergebnisse mit Militärhilfe und deportierten Abgeordnete",
        answerC = "Neojacobinische Kräfte stürzten das konservative Direktorium",
        answerD = "Außenminister Talleyrand arrangierte eine parlamentarische Mehrheit gegen die Direktoren",
        correctAnswer = 1, // B
        explanation = "Am 18. Fructidor V annullierten die republikanischen Direktoren Barras, Reubell und La Revellière-Lépeaux mit Hilfe von General Augereau (auf Napoleons Befehl aus Italien) die Ergebnisse von 49 Départements, deportierten über 50 royalistisch-sympathisierende Abgeordnete nach Guyana und setzten zwei royalistisch gesinnter Direktoren ab.",
        difficulty = 5,
        funFact = "General Augereau wurde für seine Hilfe beim Staatsstreich mit dem Kommando über die Pariser Garnison belohnt — ein für Napoleon nützliches Muster."
    ),

    // Question 35 — Brumaire: 18 Brumaire 1799
    Question(
        categoryId = 3,
        questionText = "Warum verlief Napoleons Coup des 19. Brumaire VIII (10. November 1799) in Saint-Cloud fast katastrophal, und was rettete die Situation?",
        answerA = "Das Militär weigerte sich zunächst, den Konventsaal zu stürmen, bis Marschall Murat die Soldaten führte",
        answerB = "Napoleon wurde von Abgeordneten des Rats der Fünfhundert physisch angegriffen und fast lyncht; sein Bruder Lucien rettete ihn durch die Lüge, Dolchträger hätten Napoleon angegriffen",
        answerC = "Direktor Barras widerrief seine Abdankung, bis Talleyrand ihn bestach",
        answerD = "Eine jacobinische Kontrevolte in Paris musste durch Kavallerie niedergeschlagen werden",
        correctAnswer = 1, // B
        explanation = "Im Rat der Fünfhundert wurde Napoleon von wütenden Abgeordneten angegriffen, bewusstlos oder beinahe ohnmächtig abgeführt. Sein Bruder Lucien, Präsident des Rates, rettete die Situation, indem er den Soldaten erklärte, dolchtragende Terroristen hätten Napoleon bedroht. Die Grenadiere stürmten daraufhin den Saal und jagten die Abgeordneten auseinander.",
        difficulty = 5,
        funFact = "Napoleon soll nach dem Rat der Fünfhundert so bleich und verwirrt gewesen sein, dass seine Begleiter fürchteten, er hätte einen Nervenzusammenbruch erlitten."
    ),

    // Question 36 — Napoleonic satellite states: Kingdom of Westphalia
    Question(
        categoryId = 3,
        questionText = "Was war das politisch-verfassungsrechtliche Modell, das Napoleon für das Königreich Westphalen unter seinem Bruder Jérôme (gegr. 1807) vorsah?",
        answerA = "Eine absolutistische Monarchie nach dem Vorbild der Bourbon-Monarchien",
        answerB = "Ein konstitutioneller Modellstaat mit Code Napoléon, Zweikammerparlament und Abschaffung der Leibeigenschaft",
        answerC = "Eine republikanische Verfassung mit jährlichen Wahlen",
        answerD = "Ein Bundesstaat aus deutschen Fürstentümern mit lokaler Autonomie",
        correctAnswer = 1, // B
        explanation = "Napoleon bezeichnete Westphalen als Modellstaat, der den deutschen Untertanen und Fürsten zeigen sollte, was das Code Napoléon und die Ideen von 1789 bringen. Es gab eine Verfassung, zwei Kammern (Staatsrat und Adelskammer), Religionsfreiheit, Gleichheit vor dem Gesetz und das Ende der Feudalordnung — theoretisch fortschrittlicher als die deutschen Nachbarstaaten.",
        difficulty = 5,
        funFact = "Jérôme Bonaparte war für seinen verschwenderischen Hof in Kassel bekannt; Napoleon schrieb ihm mehrfach, er solle aufhören, so viel Geld auszugeben, und sich auf Reformen konzentrieren."
    ),

    // Question 37 — Napoleonic satellite states: Grand Duchy of Warsaw
    Question(
        categoryId = 3,
        questionText = "Welches Dokument bildete die Verfassungsgrundlage des Herzogtums Warschau (1807–1815), und wer hatte es in Napoleon zugeschrieben?",
        answerA = "Die Warschauer Konstitution, ausgearbeitet von Kościuszko",
        answerB = "Die Verfassung von 1807, von Napoleon in Dresden diktiert und dem sächsischen König als Herzogtum-Grund übertragen",
        answerC = "Die modifizierte polnische Verfassung vom 3. Mai 1791",
        answerD = "Der Tilsiter Vertrag als direkte Verfassungsurkunde",
        correctAnswer = 1, // B
        explanation = "Napoleon diktierte im Juli 1807 in Dresden eine Verfassung für das neue Herzogtum Warschau, das er dem sächsischen König Friedrich August I. als Lehen gab. Die Verfassung übernahm den Code Napoléon, schaffte die Leibeigenschaft ab (formal) und sah einen Sejm vor. Das Herzogtum war die Grundlage für spätere polnische Unabhängigkeitshoffnungen.",
        difficulty = 5,
        funFact = "Polnische Legionäre hatten für Napoleon in Italien und Haiti gekämpft — ihre Loyalität basierte auf der Hoffnung, er würde Polen wiederherstellen, was er nie fest versprach."
    ),

    // Question 38 — Napoleonic satellite states: Kingdom of Italy
    Question(
        categoryId = 3,
        questionText = "Welchen Titel trug Napoleon im Königreich Italien (gegr. 1805), und wer war Vizekönig?",
        answerA = "König von Italien; Vizekönig war sein Bruder Joseph",
        answerB = "König von Italien; Vizekönig war sein Stiefsohn Eugène de Beauharnais",
        answerC = "Protector; Präsident war Melzi d'Eril",
        answerD = "Kaiser des Westens; Vizekönig war Marschall Murat",
        correctAnswer = 1, // B
        explanation = "Napoleon krönte sich am 26. Mai 1805 in Mailand mit der Eisernen Krone der Langobarden zum König von Italien. Als Vizekönig setzte er seinen Stiefsohn Eugène de Beauharnais (Sohn der Kaiserin Joséphine aus erster Ehe) ein. Eugène erwies sich als fähiger Verwalter und Feldherr und hielt die Treue bis zum Ende 1814.",
        difficulty = 5,
        funFact = "Die Eiserne Krone soll einen Nagel vom Kreuz Christi enthalten — Napoleon soll bei der Krönung gesagt haben: 'Gott hat sie mir gegeben; wehe dem, der sie anrührt.'"
    ),

    // Question 39 — Napoleonic satellite states: Confederation of the Rhine
    Question(
        categoryId = 3,
        questionText = "Welche Konsequenz hatte die Gründung des Rheinbundes am 12. Juli 1806 für das Heilige Römische Reich?",
        answerA = "Das Reich wurde in einen Nord- und Südbund aufgeteilt",
        answerB = "Kaiser Franz II. legte die Kaiserkrone des Heiligen Römischen Reiches nieder und löste das Reich auf",
        answerC = "Preußen und Österreich wurden aus dem Reich ausgeschlossen",
        answerD = "Das Reich wurde zu einem Bundesstaat unter napoleonischem Protektorat umgewandelt",
        correctAnswer = 1, // B
        explanation = "Sechs Wochen nach Gründung des Rheinbundes, am 6. August 1806, legte Kaiser Franz II. die Reichskrone nieder und erklärte das Heilige Römische Reich Deutscher Nation für aufgelöst. Er behielt seinen 1804 angenommenen Titel als Kaiser von Österreich. Das Reich, das 844 Jahre bestanden hatte, endete damit formell.",
        difficulty = 5,
        funFact = "Napoleon wurde Protektor des Rheinbundes — ein Titel, der ihm mehr Einfluss über Mitteldeutschland gab als jeder Kaiser zuvor hatte."
    ),

    // Question 40 — Congress System: Congress of Vienna details
    Question(
        categoryId = 3,
        questionText = "Was war das Prinzip der 'Legitimität', das Talleyrand als Vertreter des besiegten Frankreich beim Wiener Kongress 1814/15 erfolgreich einbrachte?",
        answerA = "Das Recht der Sieger, territoriale Kompensationen zu verlangen",
        answerB = "Die Wiederherstellung der vor der Revolution rechtmäßig herrschenden Dynastien als Grundlage der neuen Ordnung",
        answerC = "Die Anerkennung der durch Napoleon geschaffenen Tatsachen als bindend",
        answerD = "Das Selbstbestimmungsrecht der Völker bei Grenzziehungen",
        correctAnswer = 1, // B
        explanation = "Talleyrand nutzte das Legitimitätsprinzip (rechtmäßige Dynastien sollen ihre Throne zurückerhalten) um Frankreich von einer Position der Stärke zu verhandeln: Wenn die Bourbons legitime Könige waren, war Frankreich kein Aggressorstaat, sondern selbst Opfer Napoleons. Dies erlaubte Frankreich, als gleichberechtigte Macht teilzunehmen und eine Spaltung der Alliierten zu bewirken.",
        difficulty = 5,
        funFact = "Talleyrand hatte nacheinander dem Ancien Régime, der Revolutionsregierung, Napoleon und Ludwig XVIII. gedient — er selbst sagte: 'Ich habe nie intrigiert, ich habe immer gelogen.'"
    ),

    // Question 41 — Congress System: Holy Alliance
    Question(
        categoryId = 3,
        questionText = "Wer initiierte die Heilige Allianz von 1815, und was war ihr praktischer politischer Inhalt im Gegensatz zu ihrer mystisch-religiösen Proklamation?",
        answerA = "Metternich initiierte sie als formales Interventionssystem gegen Revolutionen",
        answerB = "Zar Alexander I. initiierte sie als christlich-mystisches Dokument; Metternich nannte sie 'ein leeres, klingendes Wort'; praktisch diente sie der Unterdrückung liberaler und nationaler Bewegungen",
        answerC = "Großbritannien und Österreich gründeten sie als Garantiepakt für die neue Ordnung",
        answerD = "Preußen und Russland schufen sie als militärisches Bündnis gegen Frankreich",
        correctAnswer = 1, // B
        explanation = "Zar Alexander I. entwarf die Heilige Allianz als religiös-mystisches Dokument, das christliche Brüderlichkeit unter Monarchen proklamierte. Metternich bezeichnete sie privat als 'ein leeres, klingendes Wort' oder 'Nichts'. Praktisch wurde sie aber als ideologische Rechtfertigung benutzt, um liberale und nationale Aufstände zu unterdrücken (Troppau-Protokoll 1820).",
        difficulty = 5,
        funFact = "Großbritannien und der Papst traten der Heiligen Allianz nie bei — London wegen konstitutioneller Bedenken, der Papst weil er kein protestantischer und orthodoxer Bündnispartner sein wollte."
    ),

    // Question 42 — Congress System: Congress of Aix-la-Chapelle 1818
    Question(
        categoryId = 3,
        questionText = "Was war das wichtigste Ergebnis des Kongresses von Aachen 1818, dem ersten großen Kongress nach dem Wiener Kongress?",
        answerA = "Die Unterdrückung des deutschen Nationalismus durch die Karlsbader Beschlüsse",
        answerB = "Die Aufnahme Frankreichs in das Konzert der Großmächte und der Abzug der Besatzungstruppen",
        answerC = "Die Lösung der griechischen Frage durch Autonomiezusagen",
        answerD = "Die Aufteilung der deutschen Bundesakte unter den Großmächten",
        correctAnswer = 1, // B
        explanation = "Der Kongress von Aachen (September–November 1818) nahm Frankreich (unter Ludwig XVIII.) als gleichberechtigte fünfte Großmacht in das Konzert der Mächte auf, nachdem die Kriegsschuld vollständig bezahlt war. Die alliierten Besatzungstruppen verließen Frankreich, und aus der Vierfachen Allianz wurde eine Fünffache ('Pentarchie').",
        difficulty = 5,
        funFact = "Beim Kongress von Aachen wurde auch die Frage diskutiert, ob die Verbündeten Spanisch-Amerika zur Rückkehr unter spanische Herrschaft zwingen sollten — Großbritannien blockierte diesen Plan."
    ),

    // Question 43 — Congress System: Congress of Troppau 1820
    Question(
        categoryId = 3,
        questionText = "Was legte das Troppauer Protokoll (1820) fest, das beim Kongress von Troppau verabschiedet wurde?",
        answerA = "Das Recht der Großmächte, in revolutionäre Staaten einzugreifen, um die legitime Ordnung wiederherzustellen",
        answerB = "Den Mechanismus für kollektive Abrüstungsverhandlungen in Europa",
        answerC = "Die Definition nationaler Minderheitenrechte in den Habsburger und osmanischen Gebieten",
        answerD = "Das Verbot geheimer Gesellschaften in allen Mitgliedsstaaten der Heiligen Allianz",
        correctAnswer = 0, // A
        explanation = "Das Troppauer Protokoll (19. November 1820) erklärte, dass Staaten, die durch Revolution ihre Regierungsform geändert hatten, aus der europäischen Allianz ausgeschlossen seien und notfalls mit Gewalt zur alten Ordnung zurückgebracht werden könnten. Es war die formelle Grundlage für die österreichische Intervention gegen die neapolitanische Revolution 1821.",
        difficulty = 5,
        funFact = "Großbritannien und Frankreich unterzeichneten das Troppauer Protokoll nicht, schickten aber Beobachter — ein frühes Zeichen des wachsenden britischen Unbehagens mit dem Interventionismus."
    ),

    // Question 44 — Congress System: Congress of Laibach 1821
    Question(
        categoryId = 3,
        questionText = "Welche Entscheidung trafen die Großmächte beim Kongress von Laibach 1821, und welche Armee führte sie aus?",
        answerA = "Russland erhielt Mandat zur Unterdrückung des polnischen Aufstands",
        answerB = "Österreich erhielt Mandat zur militärischen Intervention in Neapel zur Niederschlagung der konstitutionellen Revolution",
        answerC = "Preußen durfte die deutschen Burschenschaften gewaltsam auflösen",
        answerD = "Frankreich erhielt Mandat zur Intervention in Spanien",
        correctAnswer = 1, // B
        explanation = "Beim Kongress von Laibach (Januar–Mai 1821) beschlossen die Großmächte (ohne Großbritannien und Frankreich als Vollunterzeichner), Österreich zu ermächtigen, die 1820 ausgebrochene neapolitanische Revolution niederzuschlagen. Österreichische Truppen marschierten im März 1821 in Neapel ein, besiegten das konstitutionelle Heer und stellten König Ferdinand I. auf dem Thron wieder her.",
        difficulty = 5,
        funFact = "Das Wort 'Laibach' ist der deutsch-österreichische Name für das heutige Ljubljana in Slowenien — damals Teil des Habsburgerreichs."
    ),

    // Question 45 — Congress System: Congress of Verona 1822
    Question(
        categoryId = 3,
        questionText = "Welche Entscheidung wurde beim Kongress von Verona 1822 über die spanische Revolution getroffen, und wie reagierte Großbritannien?",
        answerA = "Frankreich erhielt ein Mandat zur Intervention in Spanien; Großbritannien brach daraufhin mit dem Kongresssystem",
        answerB = "Österreich intervenierte in Spanien, Großbritannien stimmte zu",
        answerC = "Die Heilige Allianz beschloss eine kollektive militärische Intervention aller fünf Mächte",
        answerD = "Die Intervention wurde abgelehnt, Spanien erhielt eine Amnestie",
        correctAnswer = 0, // A
        explanation = "Beim Kongress von Verona (Oktober–Dezember 1822) ermächtigten Russland, Österreich und Preußen Frankreich, in Spanien einzugreifen, um die liberale Verfassung von 1812 und die Revolution von 1820 zu beenden. Großbritannien (vertreten durch Wellington) lehnte ab. Der britische Außenminister Canning erklärte offen, das Kongresssystem sei damit für Großbritannien beendet.",
        difficulty = 5,
        funFact = "Canning sagte zum spanischen Eingriff berühmt: 'I called the New World into existence to redress the balance of the Old' — gemeint war die britische Unterstützung für die Unabhängigkeit Spanisch-Amerikas als Gegengewicht."
    ),

    // Question 46 — Polish-Lithuanian Commonwealth: Election of kings
    Question(
        categoryId = 3,
        questionText = "Welches einzigartige Verfahren nutzten die polnisch-litauischen Adligen bei der Königswahl ('Elekcja'), das ein welthistorisches Novum darstellte?",
        answerA = "Geheime Abstimmung im Sejm durch bevollmächtigte Abgesandte",
        answerB = "Direkte Wahl auf dem Wahlfeld (Wola bei Warschau) durch alle persönlich anwesenden Adligen ('viritim')",
        answerC = "Wahl durch einen Wahlkolleg aus je zehn Vertretern jeder Provinz",
        answerD = "Automatische Nachfolge des ältesten männlichen Verwandten des verstorbenen Königs",
        correctAnswer = 1, // B
        explanation = "Die polnische Elekcja war eine Direktwahl 'viritim' — jeder Adlige hatte persönlich Stimmrecht. Auf dem Wahlfeld vor Warschau konnten Zehntausende erscheinen. Diese Form stellte das größte Wahlkolleg der damaligen Welt dar, aber auch ein riesiges Bestechungspotenzial für ausländische Mächte, die Kandidaten finanzieren konnten.",
        difficulty = 5,
        funFact = "Bei der Königswahl von 1573 (erste nach dem Aussterben der Jagiellonen) sollen über 50.000 Adlige auf dem Wahlfeld bei Warschau zusammengekommen sein."
    ),

    // Question 47 — War of Spanish Succession: Bourbon Dynasty establishment
    Question(
        categoryId = 3,
        questionText = "Welche Bedingung stellte der Frieden von Utrecht 1713 für Philipps V. Anerkennung als König von Spanien bezüglich der französischen Thronfolge?",
        answerA = "Philipp musste auf die spanische Kolonien in Amerika verzichten",
        answerB = "Philipp und seine Nachkommen mussten auf alle Ansprüche auf den französischen Thron verzichten",
        answerC = "Philipp musste die Infantin mit einem habsburgischen Prinzen verheiraten",
        answerD = "Philipp musste Katalonien Sonderrechte garantieren",
        correctAnswer = 1, // B
        explanation = "Im Vertrag von Utrecht (11. April 1713) erkannte Europa Philipp V. als König von Spanien an, aber er und seine Nachkommen mussten feierlich auf alle Erbrechte an der französischen Krone verzichten. Damit sollte eine Personalunion Frankreich-Spanien dauerhaft verhindert werden. Philipp behielt in Spanien jedoch die gesamte spanische Erbschaft.",
        difficulty = 5,
        funFact = "Philipp V. litt an schwerer Melancholie und Depression; der berühmte Kastrat Farinelli sang ihm jahrelang täglich die gleichen vier Lieder vor, um seinen Gemütszustand zu stabilisieren."
    ),

    // Question 48 — Great Northern War: Pruth Campaign 1711
    Question(
        categoryId = 3,
        questionText = "Was war das Ergebnis von Peters I. Pruth-Feldzug 1711 gegen das Osmanische Reich, den er auf Betreiben des dort residierenden Karl XII. unternahm?",
        answerA = "Russland besiegte die Osmanen und drängte die Türkei aus Moldau",
        answerB = "Peters Armee wurde am Fluss Pruth eingeschlossen; er musste Asow zurückgeben und den Zugang zum Schwarzen Meer aufgeben",
        answerC = "Der Feldzug endete unentschieden und Karl XII. wurde aus dem Osmanischen Reich ausgewiesen",
        answerD = "Peter siegte, aber Karl XII. entkam nach Schweden und setzte den Krieg fort",
        correctAnswer = 1, // B
        explanation = "Peters Armee wurde am Pruth von einer überlegenen osmanischen Streitmacht eingeschlossen. Im Frieden vom Pruth (Juli 1711) musste Russland Asow zurückgeben, alle Festungen am Asowschen Meer schleifen und Karl XII. freie Heimkehr garantieren. Es war Peters größte Demütigung — aber er entkam persönlich, was er vor allem Großwesir Baltadzhi Mehmed pascha und Zarin Katharina zuschrieb.",
        difficulty = 5,
        funFact = "Eine Legende besagt, Katharina I. habe ihren Schmuck genutzt, um den Großwesir zu bestechen und Peters Kapitulation abzumildern — ob wahr oder nicht, stärkte Katharinas Position am Hof erheblich."
    ),

    // Question 49 — Hussite Wars: Compacts of Basel
    Question(
        categoryId = 3,
        questionText = "Was regelten die Basler Kompaktaten (1433/36), die den Hussitenkriegen ein Ende setzten?",
        answerA = "Die vollständige Wiederaufnahme Böhmens in die Römische Kirche unter Anerkennung des Papstes",
        answerB = "Die Anerkennung der Kommunion unter beiden Gestalten (Kelch) für Laien in Böhmen als offiziell tolerierte Praxis",
        answerC = "Die Schaffung einer eigenständigen böhmischen Nationalkirche",
        answerD = "Das Recht Böhmens, seinen eigenen Erzbischof ohne päpstliche Bestätigung zu wählen",
        correctAnswer = 1, // B
        explanation = "Die Basler Kompaktaten (verhandelt beim Konzil von Basel, bestätigt 1436 in Iglau) genehmigten für Böhmen den Laienkelch beim Abendmahl, die freie Predigt des Gotteswortes, die Kirchenzucht nach Evangelienmaßstab und die Säkularisierung bereits eingezogener Kirchengüter. Sie waren ein einzigartiges Zugeständnis der Kirche an hussitische Forderungen.",
        difficulty = 5,
        funFact = "Papst Pius II. widerrief die Basler Kompaktaten 1462 — aber Böhmen ignorierte dies und praktizierte den Laienkelch weiter bis zur Rekatholisierung nach 1620."
    ),

    // Question 50 — Partitions of Poland: Targowica Confederation
    Question(
        categoryId = 3,
        questionText = "Was war die Konföderation von Targowica (1792), die als Rechtfertigung der zweiten Polenteilung diente?",
        answerA = "Eine legitime adlige Opposition gegen die Verfassung von 1791, die von russisch bezahlten polnischen Magnaten gegründet wurde, um russische Intervention zu legitimieren",
        answerB = "Eine preußisch-russische Militärallianz zur Besetzung Polens",
        answerC = "Eine litauische Separationsbewegung vom polnischen Commonwealth",
        answerD = "Eine bäuerliche Revolte gegen die adlige Herrschaft in der Ukraine",
        correctAnswer = 0, // A
        explanation = "Die Konföderation von Targowica (14. Mai 1792) wurde von polnischen Magnaten (Potocki, Rzewuski, Branicki) in Russland gegründet und proklamierte die Verfassung von 1791 als illegitim. Sofort nach der Proklamation marschierten russische Truppen ein. Die Konföderation diente Katharina II. als juristischer Vorwand für die Intervention und bereitete die zweite Teilung vor.",
        difficulty = 5,
        funFact = "König Stanisław August Poniatowski trat der Konföderation von Targowica bei, um das Blutvergießen zu beenden — ein Schritt, der ihm als Verrat galt und sein Ansehen dauerhaft zerstörte."
    ),
)
