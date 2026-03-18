package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsMaster2(): List<Question> = listOf(

    // --- Albigensian Crusade ---

    Question(
        categoryId = 3,
        questionText = "Welcher päpstliche Legat wurde im Jahr 1208 ermordet und löste damit den Albigenserkreuzzug aus?",
        answerA = "Innozenz III.",
        answerB = "Pierre de Castelnau",
        answerC = "Arnaud Amaury",
        answerD = "Dominikus von Guzmán",
        correctAnswer = 1,
        explanation = "Die Ermordung des päpstlichen Legaten Pierre de Castelnau am 14. Januar 1208, vermutlich durch einen Gefolgsmann Graf Raimunds VI. von Toulouse, veranlasste Papst Innozenz III. zur Ausrufung des Albigenserkreuzzuges gegen die Katharer in Südfrankreich.",
        difficulty = 5,
        funFact = "Nach dem Fall von Béziers (1209) soll Arnaud Amaury auf die Frage, wie man Katharer von Katholiken unterscheiden könne, geantwortet haben: 'Tötet sie alle, Gott wird die Seinen erkennen' – ob dieser Satz wirklich fiel, ist historisch umstritten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche militärische Führungsfigur des Albigenserkreuzzuges starb 1218 bei der Belagerung von Toulouse?",
        answerA = "Ludwig VIII. von Frankreich",
        answerB = "Raimund VI. von Toulouse",
        answerC = "Simon IV. de Montfort",
        answerD = "Philipp II. von Frankreich",
        correctAnswer = 2,
        explanation = "Simon IV. de Montfort, der wichtigste Heerführer des Albigenserkreuzzuges, starb am 25. Juni 1218 während der zweiten Belagerung von Toulouse durch einen Steinwurf aus einer von Frauen bedienten Wurfmaschine.",
        difficulty = 5,
        funFact = "Simons Sohn Simon V. setzte den Kreuzzug fort, gab die Führung aber schließlich an die französische Krone ab. Sein Enkel Simon de Montfort the Younger wurde in England als Begründer des Parlamentarismus berühmt."
    ),

    Question(
        categoryId = 3,
        questionText = "Mit welchem Vertrag von 1229 wurde der Albigenserkreuzzug formell beendet und Südfrankreich der Krone Frankreichs unterworfen?",
        answerA = "Frieden von Meaux-Paris",
        answerB = "Vertrag von Carcassonne",
        answerC = "Frieden von Pamiers",
        answerD = "Vertrag von Corbeil",
        correctAnswer = 0,
        explanation = "Der Frieden von Meaux-Paris (1229) beendete den Albigenserkreuzzug. Raimund VII. von Toulouse musste schwere Bedingungen akzeptieren: Abtretung von Land, Schleifung von Burgen, seine Tochter Johanna sollte einen Capetinger heiraten, womit die Grafschaft langfristig an Frankreich fiel.",
        difficulty = 5,
        funFact = "Der Vertrag legte auch die Grundlage für die Universität Toulouse (gegründet 1229), die als theologisches Gegengewicht zur Ketzerei im Süden dienen sollte."
    ),

    // --- Cathar Beliefs ---

    Question(
        categoryId = 3,
        questionText = "Wie bezeichneten die Katharer ihre vollständig initiierten Mitglieder, die das 'Consolamentum' empfangen hatten?",
        answerA = "Parfaits (Perfecti)",
        answerB = "Credentes",
        answerC = "Bonhommes",
        answerD = "Diaconi",
        correctAnswer = 0,
        explanation = "Die 'Parfaits' oder 'Perfecti' (auf Okzitanisch 'Bons Hommes') waren die vollständig initiierten Katharer, die durch das Consolamentum (Geisttaufe) aufgenommen wurden. Sie lebten in strenger Askese, durften kein Fleisch essen und keine sexuellen Beziehungen haben.",
        difficulty = 5,
        funFact = "Nur ein Bruchteil der Katharer waren Parfaits – die meisten waren 'Credentes' (Gläubige), die normal leben durften und erst auf dem Sterbebett das Consolamentum empfingen, um im Stand der Reinheit zu sterben."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches theologische Kernprinzip unterschied den Katharismus grundlegend vom orthodoxen Christentum?",
        answerA = "Die Ablehnung der Eucharistie als Symbol",
        answerB = "Der Dualismus: Ein böser Schöpfergott schuf die materielle Welt, ein guter Gott die spirituelle",
        answerC = "Die Ablehnung der Trinität zugunsten eines strikten Monotheismus",
        answerD = "Die Überzeugung, dass Christus ein reiner Engel ohne menschliche Natur war",
        correctAnswer = 1,
        explanation = "Der Katharismus war fundamental dualistisch: Die materielle Welt wurde von einem bösen Demiurgen geschaffen (identifiziert mit dem Gott des Alten Testaments), während der gute, transzendente Gott nur die Geisterwelt erschuf. Seelen sind göttlichen Ursprungs und in materiellen Körpern gefangen.",
        difficulty = 5,
        funFact = "Der Katharismus hatte Verbindungen zur bogomilischen Bewegung auf dem Balkan und weiter zurück zu gnostischen Strömungen der Spätantike. Die Katharer verwarfen Kreuz, Kirchen und Sakramente als Werkzeuge des bösen Schöpfers."
    ),

    // --- Children's Crusade ---

    Question(
        categoryId = 3,
        questionText = "Welche zwei Anführer stehen im Mittelpunkt der historischen Quellen zum Kinderkreuzzug von 1212?",
        answerA = "Raimund von Toulouse und Konrad von Marburg",
        answerB = "Stephan von Cloyes und Nikolaus von Köln",
        answerC = "Fulk von Neuilly und Jacob von Vitry",
        answerD = "Peter der Einsiedler und Walter ohne Habe",
        correctAnswer = 1,
        explanation = "Die Quellen berichten von zwei parallelen Bewegungen: Stephan von Cloyes führte eine Gruppe in Frankreich an, die nach Marseille zog, und Nikolaus von Köln führte eine deutsche Gruppe über die Alpen nach Genua. Die historische Realität dieser Ereignisse ist jedoch stark umstritten.",
        difficulty = 5,
        funFact = "Neuere Forschungen bezweifeln, dass es sich wirklich um Kinder handelte – der mittelalterliche Begriff 'puer' konnte auch 'Dienstbote' oder 'Abhängiger' bedeuten. Möglicherweise waren es Arme und Landlose verschiedenen Alters."
    ),

    // --- Waldensians ---

    Question(
        categoryId = 3,
        questionText = "Wer gründete die Waldenserische Bewegung im 12. Jahrhundert und in welcher Stadt?",
        answerA = "Arnold von Brescia in Rom",
        answerB = "Petrus Waldo in Lyon",
        answerC = "Berthold von Regensburg in Regensburg",
        answerD = "Heinrich von Lausanne in Lausanne",
        correctAnswer = 1,
        explanation = "Petrus Waldo (Pierre Valdès), ein reicher Kaufmann in Lyon, verschenkte um 1173 seinen Besitz, ließ die Bibel ins Okzitanische übersetzen und gründete die Gemeinschaft der 'Armen von Lyon'. Beim Dritten Laterankonzil 1179 wurde die Gruppe zunächst toleriert, aber 1184 exkommuniziert.",
        difficulty = 5,
        funFact = "Die Waldenser überlebten als einzige der mittelalterlichen Ketzerbewegungen bis heute. Sie schlossen sich im 16. Jahrhundert der Reformation an und existieren noch immer als Waldensisch-Evangelische Kirche in Italien."
    ),

    Question(
        categoryId = 3,
        questionText = "Auf welchem Konzil wurden die Waldenser 1184 offiziell als Häretiker verurteilt und exkommuniziert?",
        answerA = "Konzil von Verona",
        answerB = "Viertes Laterankonzil",
        answerC = "Konzil von Toulouse",
        answerD = "Drittes Laterankonzil",
        correctAnswer = 0,
        explanation = "Auf dem Konzil von Verona (1184) unter Papst Lucius III. wurden die Waldenser gemeinsam mit anderen Gruppen (Humiliaten, Katharer) durch die Bulle 'Ad abolendam' offiziell als Häretiker verurteilt, weil sie ohne kirchliche Erlaubnis predigten.",
        difficulty = 5,
        funFact = "Ironisch ist, dass die Waldenser ursprünglich päpstliche Zustimmung zur Armutspredigt gesucht hatten. Erst als Bischöfe ihnen die Predigt verboten und Waldo trotzdem predigte, eskalierte der Konflikt zur Exkommunikation."
    ),

    // --- Flagellant Movements ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr erließ Papst Clemens VI. die Bulle 'Inter sollicitudines', die die Flagellantenbewegung verurteilte?",
        answerA = "1347",
        answerB = "1349",
        answerC = "1351",
        answerD = "1353",
        correctAnswer = 1,
        explanation = "Papst Clemens VI. verurteilte die Flagellanten im Oktober 1349 durch die Bulle 'Inter sollicitudines'. Die Bewegung hatte sich im Kontext der Pest (Schwarzer Tod 1347–1351) explosionsartig ausgebreitet; die Verurteilung begründete sich teils mit theologischen Irrtümern, teils mit den von Flagellanten angezettelten Judenpogromen.",
        difficulty = 5,
        funFact = "Die Flagellanten zogen in Gruppen von Stadt zu Stadt, führten öffentliche Geißelungsrituale durch und glaubten, durch 33,5 Tage Buße (für jedes Lebensjahr Christi) die Sünden der Welt zu sühnen. Sie stellten damit die Sakramente der Kirche in Frage."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche theologisch gefährliche Lehre vertraten radikale Flagellanten, die eine besondere Verurteilung durch die Kirche provozierte?",
        answerA = "Die Behauptung, Christus habe die Menschheit noch nicht vollständig erlöst",
        answerB = "Die Ablehnung der Transsubstantiation",
        answerC = "Die Lehre, dass das eigene Blut vergießen die Beichte überflüssig mache",
        answerD = "Die Behauptung, der Papst sei der Antichrist",
        correctAnswer = 2,
        explanation = "Radikale Flagellanten lehrten, dass die eigene Selbstgeißelung die Beichte beim Priester ersetze und direkte Sündenvergebung bewirke. Dies untergrub die sakramentale Autorität der Kirche fundamental und war der theologische Kerngrund für die päpstliche Verurteilung.",
        difficulty = 5,
        funFact = "Einige Flagellanten-Gruppen entwickelten eine regelrechte Sekte mit eigenen Ritualtexten, Liedern (Geißlerlieder) und der Überzeugung, einen neuen heiligen Geist empfangen zu haben. Diese 'sektiererischen' Flagellanten wurden noch bis ins 15. Jahrhundert von Inquisitoren verfolgt."
    ),

    // --- Prester John Legend ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr tauchte der erste bekannte Brief des 'Priesterkönig Johannes' auf, der an den byzantinischen Kaiser Manuel I. Komnenos adressiert war?",
        answerA = "1122",
        answerB = "1145",
        answerC = "1165",
        answerD = "1187",
        correctAnswer = 2,
        explanation = "Der berühmte 'Priester-Johannes-Brief' erschien um 1165 und war angeblich an Kaiser Manuel I. Komnenos, Papst Alexander III. und andere Herrscher gerichtet. Er beschrieb ein gewaltiges christliches Reich jenseits Persiens und Armeniens, reich an Wundern und Tugenden.",
        difficulty = 5,
        funFact = "Der Brief wurde in über 100 Handschriften kopiert und in zahlreiche Sprachen übersetzt – er war ein Bestseller des Mittelalters. Spätere Versionen verlegten das Reich des Priesterkönigs nach Äthiopien, was reale diplomatische Kontakte mit dem christlichen Abessinien beeinflusste."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches historische Ereignis des frühen 13. Jahrhunderts verstärkte den europäischen Glauben an einen christlichen Königreich im Osten und wurde anfänglich mit dem Priesterkönig Johannes identifiziert?",
        answerA = "Die mongolische Invasion Polens 1241",
        answerB = "Der Sieg des Kara-Khitai-Reiches über den Seldschuken-Sultan Sanjar 1141",
        answerC = "Die Gründung des Lateinischen Kaiserreichs in Konstantinopel 1204",
        answerD = "Der Kreuzzug König Andreas' II. von Ungarn 1217",
        correctAnswer = 1,
        explanation = "Der Sieg von Ye-lü Ta-shih (Gur Khan des Kara-Khitai-Reiches) über den Seldschuken-Sultan Ahmad Sanjar bei Qatwan 1141 erreichte Europa in verzerrter Form und wurde als Bestätigung eines mächtigen östlichen christlichen Königs gedeutet. Das Kara-Khitai-Reich war tatsächlich buddhistisch.",
        difficulty = 5,
        funFact = "Als die Mongolen nach Europa vordrangen, hofften viele zunächst, es handle sich um die Streitkräfte des Priesterkönigs Johannes, der zur Befreiung des Heiligen Landes komme. Diese Hoffnung zerplatzte beim mongolischen Einfall von 1241."
    ),

    // --- Avignon Papacy ---

    Question(
        categoryId = 3,
        questionText = "Welcher Papst verlegte als erster offiziell die Residenz nach Avignon im Jahr 1309?",
        answerA = "Clemens V.",
        answerB = "Johannes XXII.",
        answerC = "Benedikt XII.",
        answerD = "Clemens VI.",
        correctAnswer = 0,
        explanation = "Clemens V. (Bertrand de Got), gewählt 1305, verlegte 1309 die päpstliche Residenz nach Avignon, damals ein Territorium der Grafschaft Venaissin (Papsttum). Er ließ sich nie in Rom nieder und begründete damit das sogenannte 'Avignonesische Exil' oder 'Babylonische Gefangenschaft der Kirche'.",
        difficulty = 5,
        funFact = "Clemens V. ist auch für seine Aufhebung des Templerordens (1312) bekannt. Er ließ sich stark vom französischen König Philipp IV. beeinflussen, der hinter der Vernichtung der Templer stand."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Avignon-Papst führte einen jahrzehntelangen theologischen Streit über die 'Visio Beatifica' – die Frage, ob Seelen Gott unmittelbar nach dem Tod schauen könnten?",
        answerA = "Clemens V.",
        answerB = "Johannes XXII.",
        answerC = "Benedikt XII.",
        answerD = "Innozenz VI.",
        correctAnswer = 1,
        explanation = "Johannes XXII. (1316–1334) lehrte kontrovers, dass Seelen die vollständige Gottesschau (Visio Beatifica) erst nach dem Jüngsten Gericht erlangen. Diese Position wurde von Theologen wie Meister Eckhart und Wilhelm von Ockham angegriffen. Sein Nachfolger Benedikt XII. definierte 1336 in 'Benedictus Deus' die unmittelbare Gottesschau als Dogma.",
        difficulty = 5,
        funFact = "Johannes XXII. betrieb in Avignon auch eine intensive Kräutermedizin und verfasste alchimistische Traktate – ob er wirklich an die Transmutation von Metallen glaubte oder nur die Theorie diskutierte, ist bis heute Gegenstand der Forschung."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Avignon-Papst erwarb 1348 die Stadt Avignon durch Kauf von Königin Johanna I. von Neapel und machte sie damit zum dauerhaften Papstsitz?",
        answerA = "Benedikt XII.",
        answerB = "Clemens VI.",
        answerC = "Innozenz VI.",
        answerD = "Urban V.",
        correctAnswer = 1,
        explanation = "Clemens VI. kaufte 1348 die Stadt Avignon von Königin Johanna I. von Neapel für 80.000 Gulden. Damit wurde Avignon selbst päpstliches Territorium – zuvor residierte der Papst nur dort, während die Stadt der Grafschaft Venaissin (bereits päpstlich) benachbart war.",
        difficulty = 5,
        funFact = "Johanna I. hatte Clemens VI. den Kauf zu günstigen Konditionen angeboten, weil sie seine Absolution für den möglichen Mord an ihrem ersten Mann Andreas von Ungarn benötigte. Der Papst erklärte sie offiziell für unschuldig."
    ),

    // --- Western Schism ---

    Question(
        categoryId = 3,
        questionText = "Welches Konzil von 1409 wählte einen dritten Papst und verschlimmerte damit das Abendländische Schisma zunächst zu einem Dreifach-Schisma?",
        answerA = "Konzil von Konstanz",
        answerB = "Konzil von Pavia",
        answerC = "Konzil von Pisa",
        answerD = "Konzil von Basel",
        correctAnswer = 2,
        explanation = "Das Konzil von Pisa (1409) wählte Alexander V. als dritten Papst, um das Schisma zu beenden. Da aber weder der römische Papst Gregor XII. noch der avignonesische Gegenpapst Benedikt XIII. abdankten, gab es nun drei konkurrierende Päpste statt zweien.",
        difficulty = 5,
        funFact = "Alexander V. starb bereits 1410, woraufhin der dubiose Baldassare Cossa als Johannes XXIII. gewählt wurde. Er sollte später vom Konzil von Konstanz abgesetzt werden – weswegen der 'gute' Johannes XXIII. (Angelo Roncalli, 1958–1963) die Nummerierung neu verwendete."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie lautet der Name der konziliaristischen Theorie, die beim Konzil von Konstanz die Absetzung aller drei Päpste ermöglichte?",
        answerA = "Suprema Potestas",
        answerB = "Haec Sancta Synodus",
        answerC = "Frequens",
        answerD = "Sacrosancta",
        correctAnswer = 1,
        explanation = "Das Dekret 'Haec Sancta' (1415) erklärte, dass ein allgemeines Konzil seine Autorität direkt von Christus empfange und der Papst in Glaubensfragen, Schisma und Reform an das Konzil gebunden sei. Dies war die theologische Grundlage für die Absetzung der drei Päpste.",
        difficulty = 5,
        funFact = "Das Dekret 'Frequens' (1417) verpflichtete zur regelmäßigen Einberufung von Konzilien alle 5, dann 7, dann alle 10 Jahre. Diese konziliaristische Beschränkung päpstlicher Macht wurde von späteren Päpsten nie anerkannt."
    ),

    // --- Teutonic Knights in Prussia ---

    Question(
        categoryId = 3,
        questionText = "Durch welches päpstliche Dokument von 1234 wurden die preußischen Eroberungen des Deutschen Ordens formal unter päpstlichen Schutz gestellt und dem Orden als feudales Lehen übertragen?",
        answerA = "Bulle 'Quia maior'",
        answerB = "Bulle 'Pietati proximum'",
        answerC = "Goldene Bulle von Rimini",
        answerD = "Bulle 'Cum hora undecima'",
        correctAnswer = 1,
        explanation = "Papst Gregor IX. stellte durch die Bulle 'Pietati proximum' (1234) die preußischen Gebiete des Deutschen Ordens unter päpstlichen Schutz und übertrug sie dem Orden als Lehen des Heiligen Stuhls. Dies sicherte die ordenspolitische Unabhängigkeit gegenüber weltlichen Ansprüchen.",
        difficulty = 5,
        funFact = "Die Goldene Bulle von Rimini (1226), durch Kaiser Friedrich II. ausgestellt, hatte dem Orden zuvor Preußen als Reichslehen gegeben. Die kombinierte kaiserlich-päpstliche Legitimation machte den Deutschen Orden zur nahezu souveränen Macht im Baltikum."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Katastrophe des Deutschen Ordens bei Tannenberg (Grunwald) 1410 und ihr Friedensvertrag markieren den Beginn des ordenspolitischen Niedergangs?",
        answerA = "Erster Frieden von Thorn",
        answerB = "Zweiter Frieden von Thorn",
        answerC = "Frieden von Melno",
        answerD = "Frieden von Kalisch",
        correctAnswer = 0,
        explanation = "Nach der verheerenden Niederlage bei Tannenberg (15. Juli 1410) gegen das polnisch-litauische Heer unter Władysław II. Jagiełło und Vytautas schloss der Deutsche Orden den Ersten Frieden von Thorn (1411). Trotz militärischer Katastrophe musste er nur relativ geringe Gebiete und hohe Reparationen zahlen.",
        difficulty = 5,
        funFact = "Der Deutsche Orden überlebte Tannenberg, aber die psychologischen und finanziellen Folgen waren verheerend. Der Zweite Frieden von Thorn (1466) nach dem Dreizehnjährigen Krieg war weit schlimmer: Westpreußen ging an Polen, der Hochmeister wurde polnischer Vasall."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Hochmeister des Deutschen Ordens säkularisierte den Ordensstaat 1525 und wandelte ihn in das Herzogtum Preußen um?",
        answerA = "Hermann von Salza",
        answerB = "Albrecht von Brandenburg-Ansbach",
        answerC = "Heinrich von Plauen",
        answerD = "Winrich von Kniprode",
        correctAnswer = 1,
        explanation = "Hochmeister Albrecht von Brandenburg-Ansbach konvertierte 1525 zum Luthertum, löste den Ordensstaat auf und wurde erster Herzog von Preußen als polnisches Lehen. Dieser Akt gilt als einer der bedeutendsten Staatsgründungsakte der Reformationszeit.",
        difficulty = 5,
        funFact = "Albrecht wurde von Martin Luther persönlich beraten und ermutigt, den Ordensstaat zu säkularisieren. Das Herzogtum Preußen wurde 1618 durch Erbfolge mit Brandenburg vereinigt und bildete den Kern des späteren Preußischen Königreichs."
    ),

    // --- Livonian Crusade ---

    Question(
        categoryId = 3,
        questionText = "Wer gründete 1202 den Schwertbrüderorden (Fratres Militiae Christi), den ersten in Livland gegründeten Ritterorden?",
        answerA = "Albert von Buxthoeven",
        answerB = "Dietrich von Thoreida",
        answerC = "Vincke von Gerwer",
        answerD = "Bernhard von Lippe",
        correctAnswer = 0,
        explanation = "Bischof Albert von Buxthoeven, der Gründer Rigas (1201) und erste Bischof von Riga, gründete 1202 den Schwertbrüderorden als militärischen Arm zur Unterstützung der Livlandmission. Der Orden erhielt sein Statut vom Zisterzienserorden.",
        difficulty = 5,
        funFact = "Der Schwertbrüderorden wurde 1237 nach seiner vernichtenden Niederlage gegen die Litauer und Semgaller bei Saule (1236) in den Deutschen Orden als 'Livländischer Orden' inkorporiert, blieb aber faktisch weitgehend eigenständig."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher entscheidenden Schlacht 1236 wurde der Schwertbrüderorden von Litauern und Semgallern vernichtend geschlagen, was seine Inkorporierung in den Deutschen Orden erzwang?",
        answerA = "Schlacht auf dem Eis (Peipussee)",
        answerB = "Schlacht bei Saule (Šiauliai)",
        answerC = "Schlacht bei Durben",
        answerD = "Schlacht bei Rakvere",
        correctAnswer = 1,
        explanation = "Die Schlacht bei Saule (22. September 1236) war eine katastrophale Niederlage: Der Heermeister Volquin und etwa 50 Ritter wurden getötet. Das Massaker zwang den geschwächten Orden 1237 zur Fusion mit dem Deutschen Orden, wobei die Ordensniederlassung in Livland als 'Livländischer Orden' weiterbestand.",
        difficulty = 5,
        funFact = "Litauen gilt als das letzte heidnische Land Europas, das christianisiert wurde – erst 1387 nach der polnisch-litauischen Personalunion. Die Livländischen Kreuzzüge waren ein wesentlicher Faktor, der Litauen zum Widerstand einte und seine Staatlichkeit formte."
    ),

    // --- Byzantine Iconoclasm ---

    Question(
        categoryId = 3,
        questionText = "Welcher byzantinische Kaiser löste 726/730 den ersten byzantinischen Bilderstreit aus, indem er das Christusbild am Chalkegate entfernen ließ?",
        answerA = "Konstantin V.",
        answerB = "Leo III.",
        answerC = "Nikephoros I.",
        answerD = "Michael II.",
        correctAnswer = 1,
        explanation = "Kaiser Leo III. (717–741) initiierte den ersten Ikonoklasmus, indem er ca. 726 das Christusbild am Chalke-Tor des Kaiserpalastes entfernen ließ. 730 erließ er ein formelles Edikt gegen Bilderverehrung. Papst Gregor III. verurteilte dies auf einer Synode in Rom (731).",
        difficulty = 5,
        funFact = "Leo III. entzog dem Papst als Reaktion auf die römische Verurteilung die kirchlichen Einkünfte aus Kalabrien und Illyrien und unterstellte diese Gebiete dem Patriarchen von Konstantinopel – ein schwerwiegender Schritt im Ost-West-Kirchenkonflikt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Konzil von 787 stellte die Bilderverehrung wieder her und definierte die theologische Unterscheidung zwischen 'Latria' und 'Proskynesis'?",
        answerA = "Fünftes Ökumenisches Konzil (553)",
        answerB = "Sechstes Ökumenisches Konzil (681)",
        answerC = "Siebtes Ökumenisches Konzil (Nikaia II.)",
        answerD = "Konzil von Frankfurt (794)",
        correctAnswer = 2,
        explanation = "Das Zweite Konzil von Nikaia (787), das Siebte Ökumenische Konzil, stellte die Bilderverehrung wieder her. Es unterschied zwischen 'Latria' (Anbetung, nur Gott gebührend) und 'Proskynesis/Timisis' (Verehrung, den Bildern erlaubt) als theologische Grundlage.",
        difficulty = 5,
        funFact = "Das Konzil wurde maßgeblich von Kaiserin Eirene einberufen, die als Regentin für ihren Sohn Konstantin VI. herrschte. Sie ist die einzige Frau, die in der byzantinischen Geschichte als alleinige Kaiserin (nicht Mitregentin) herrschte, nachdem sie ihren eigenen Sohn blenden ließ."
    ),

    Question(
        categoryId = 3,
        questionText = "Mit welchem theologischen Begriff bezeichneten die Ikonoklasten die ihrer Ansicht nach illegitime Bilderverehrung der Orthodoxen?",
        answerA = "Anthropomorphismus",
        answerB = "Eidololatria (Idolatrie)",
        answerC = "Hypostasenverwischung",
        answerD = "Pneumatomachie",
        correctAnswer = 1,
        explanation = "Die Ikonoklasten bezeichneten die Bilderverehrung als 'Eidololatria' (Götzendienst), da Bilder nach ihrer Ansicht materielle Objekte seien, denen keine göttliche Präsenz innewohnen könne. Sie beriefen sich auf das alttestamentliche Bilderverbot.",
        difficulty = 5,
        funFact = "Der zweite Ikonoklasmus (815–843) endete am 11. März 843 mit dem 'Triumph der Orthodoxie', der bis heute im byzantinischen Ritus am ersten Fastensonntag gefeiert wird. Theodora, die Regentin für Kaiser Michael III., stellte die Bilder wieder her."
    ),

    // --- Nika Riots ---

    Question(
        categoryId = 3,
        questionText = "Welche zwei Zirkusparteien schlossen sich im Januar 532 beim Nika-Aufstand in Konstantinopel zusammen und bedrohten damit Kaiser Justinian I.?",
        answerA = "Blaue und Grüne",
        answerB = "Rote und Weiße",
        answerC = "Blaue und Rote",
        answerD = "Grüne und Weiße",
        correctAnswer = 0,
        explanation = "Der Nika-Aufstand (13.–18. Januar 532) entstand, als sich die sonst verfeindeten Blauen und Grünen Zirkusparteien gegen Justinian vereinigten. Der Name 'Nika' (Siege!) war ihr Kampfruf. Justinian wollte zunächst fliehen, wurde aber von seiner Frau Theodora zum Bleiben überredet.",
        difficulty = 5,
        funFact = "Fast die halbe Stadtmitte Konstantinopels wurde niedergebrannt, darunter die ursprüngliche Hagia Sophia. General Belisarios sperrte mit seinen Truppen etwa 30.000–40.000 Aufständische im Hippodrom ein und ließ sie massakrieren. Die neue Hagia Sophia entstand als direkte Folge dieses Aufstands."
    ),

    Question(
        categoryId = 3,
        questionText = "Welchen gegnerischen Kaiser hatten die Aufständischen beim Nika-Aufstand proklamiert, bevor die kaiserlichen Truppen eingriffen?",
        answerA = "Germanus",
        answerB = "Hypatius",
        answerC = "Pompeius",
        answerD = "Vitalianus",
        correctAnswer = 1,
        explanation = "Die Aufständischen proklamierten Hypatius, den Neffen des früheren Kaisers Anastasios I., zum Gegenkaiser. Hypatius selbst soll unwillig gewesen sein, aber von der Menge gezwungen worden, die Kaiserwürde anzunehmen. Nach der Niederschlagung des Aufstands wurde er hingerichtet.",
        difficulty = 5,
        funFact = "Justinians Reaktion auf den Nika-Aufstand war bezeichnend für seinen Regierungsstil: öffentliche Reue, Entlassung unpopulärer Beamter als Geste, dann rücksichtslose militärische Niederschlagung. Anschließend ließ er mit dem Wiederaufbau der Hagia Sophia eine der größten Kirchen der Welt errichten."
    ),

    // --- Justinian's Corpus Juris Civilis ---

    Question(
        categoryId = 3,
        questionText = "Welcher Jurist leitete unter Justinian I. die Kommission zur Erstellung des Corpus Juris Civilis und gilt als sein geistiger Vater?",
        answerA = "Papinian",
        answerB = "Tribonian",
        answerC = "Ulpian",
        answerD = "Gaius",
        correctAnswer = 1,
        explanation = "Tribonian (gestorben ca. 545), Justinians Quaestor sacri palatii, leitete die Rechtskommissionen zur Erstellung aller drei Hauptteile des Corpus Juris: den Codex (529/534), die Digesten (533) und die Institutionen (533). Er war beim Nika-Aufstand kurzzeitig abgesetzt worden, da er als korrupt galt.",
        difficulty = 5,
        funFact = "Die Digesten (Pandekten) kompilieren Auszüge aus Schriften von 39 klassischen Juristen aus drei Jahrhunderten. Dieses Werk von 50 Büchern wurde in nur drei Jahren fertiggestellt – eine unglaubliche intellektuelle Leistung, die das europäische Recht bis heute prägt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Teil des Corpus Juris Civilis war als Lehrbuch für Jurastudenten konzipiert und basierte auf dem Werk des klassischen Juristen Gaius?",
        answerA = "Digesten",
        answerB = "Novellen",
        answerC = "Institutionen",
        answerD = "Codex Justinianus",
        correctAnswer = 2,
        explanation = "Die Institutionen (533) waren ein offizielles Rechtslehrbuch für Studenten, strukturiert in vier Bücher. Sie basierten auf den 'Institutiones' des Gaius aus dem 2. Jahrhundert und hatten Gesetzeskraft. Die Institutionen gliedern das Recht in Personenrecht, Sachenrecht und Aktionenrecht.",
        difficulty = 5,
        funFact = "Die Institutionen des Gaius wurden erst 1816 wiederentdeckt, als Niebuhr einen Palimpsest in Verona entzifferte, unter dem der Text verborgen war. Zuvor kannte man Gaius' Werk hauptsächlich durch die justinianischen Institutionen."
    ),

    // --- Theme System ---

    Question(
        categoryId = 3,
        questionText = "In welches Thema wurde Anatolien als erste byzantinische Militärverwaltungseinheit unter Kaiser Herakleios umstrukturiert, ca. 636–640?",
        answerA = "Thema Armeniakon",
        answerB = "Thema Opsikion",
        answerC = "Thema Anatolikon",
        answerD = "Thema Thrakesion",
        correctAnswer = 2,
        explanation = "Das Thema Anatolikon (von griech. 'anatole', Osten) war das erste und wichtigste Thema, entstanden aus dem Heer der orientalischen Feldzüge. Es umfasste Zentral- und Südanatolien und hatte seinen Sitz in Amorion. Der Strategos (Militärgouverneur) vereinte zivile und militärische Macht.",
        difficulty = 5,
        funFact = "Das Themensystem ersetzte die spätantike Diözesen-Präfektur-Verwaltung, die Militär und Zivilverwaltung trennte. Die Konzentration beider Gewalten beim Strategos war eine direkte Antwort auf die arabischen Invasionen, die schnelle Entscheidungen erforderten."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß die Steuereinheit und Militärdienstverpflichtung im byzantinischen Themensystem, bei der ein Landgut den Unterhalt eines Soldaten zu leisten hatte?",
        answerA = "Paroikie",
        answerB = "Stratiōtikē Ktēma",
        answerC = "Pronoia",
        answerD = "Chrysoteleia",
        correctAnswer = 1,
        explanation = "Das 'Stratiōtikē Ktēma' (Soldatengut) war die Institution, bei der ein Landgut im Thema militärpflichtig war: der Besitzer musste selbst dienen oder einen Ersatzmann stellen und ausrüsten. Diese Bindung von Militärdienst an Landbesitz schuf eine lokale Miliz und war fundamental für das Überleben Byzanz gegen arabische Angriffe.",
        difficulty = 5,
        funFact = "Theodoros Studites und andere Reformer kritisierten die schrittweise Enteignung der kleinen Soldatengüter durch den Landadel (Dynatoi) im 10. Jahrhundert scharf – Kaiser Romanos I. Lekapenos und Basilios II. erließen Gesetze zum Schutz dieser Bauernmilizen, letztlich ohne Dauererfolg."
    ),

    // --- Varangian Guard ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die Warägergarde als kaiserliche Elitetruppe in Byzanz gegründet, und durch welches Ereignis wurden ihr plötzlich viele Rekruten aus Kiew zugeführt?",
        answerA = "976, durch die Thronbesteigung Basileios II.",
        answerB = "988, durch den Pakt zwischen Basileios II. und Wladimir I. von Kiew",
        answerC = "1000, durch den Friedensvertrag von Basileios II. mit Bulgarien",
        answerD = "956, durch Prinzessin Olgas Besuch in Konstantinopel",
        correctAnswer = 1,
        explanation = "Die Warägergarde entstand faktisch 988, als Basileios II. 6.000 Waräger (nordische Krieger aus dem Kiewer Rus) von Wladimir I. als Hilfstruppe im Gegenleistung für seine Missionstätigkeit erhielt. Diese wurden als kaiserliche Leibwache institutionalisiert und direkt dem Kaiser unterstellt.",
        difficulty = 5,
        funFact = "Harald Hardråde, später König von Norwegen und Anwärter auf den englischen Thron (1066), diente als junger Mann in der Warägergarde und nahm an Feldzügen in Kleinasien, Georgien und Sizilien teil. Sein Schildknappe Hálfdanarson ritzte Runen in das Geländer der Hagia Sophia."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche besondere Gewohnheit der Warägergarde in Bezug auf das kaiserliche Erbe war als 'Palastlese' bekannt?",
        answerA = "Sie wählten den nächsten Kaiser per Akklamation",
        answerB = "Sie hatten das Recht, die Gemächer des verstorbenen Kaisers zu plündern",
        answerC = "Sie eskortierten den Leichnam des Kaisers zum Begräbnisort",
        answerD = "Sie bewachten die Schatzkammer und hatten Anteil an Steuereinkünften",
        correctAnswer = 1,
        explanation = "Die 'Palastlese' (griech. Polis-Plünderung beim Kaisertod) war ein verbrieftes Recht der Warägergarde: Beim Tod eines Kaisers durften sie in die kaiserlichen Gemächer eindringen und Gold, Silber und Wertsachen mitnehmen. Dieses Privileg sicherte ihre Loyalität gegenüber dem Kaiser, nicht dem Palastadel.",
        difficulty = 5,
        funFact = "Die Warägergarde bestand bis in die Kreuzfahrerzeit hinein. Ironischerweise wurden 1204 beim Vierten Kreuzzug die Waräger, die den Kaiser verteidigen sollten, durch die Kreuzfahrer besiegt, die ebenfalls zum großen Teil nordischer Herkunft waren."
    ),

    // --- Fourth Crusade / Sack of Constantinople ---

    Question(
        categoryId = 3,
        questionText = "Welcher blinde Doge Venedigs spielte eine Schlüsselrolle bei der Umlenkung des Vierten Kreuzzugs auf Konstantinopel?",
        answerA = "Pietro Orseolo II.",
        answerB = "Domenico Michiele",
        answerC = "Enrico Dandolo",
        answerD = "Sebastiano Ziani",
        correctAnswer = 2,
        explanation = "Enrico Dandolo (ca. 1107–1205), Doge von Venedig und trotz seines hohen Alters (über 90 Jahre) und Blindheit einer der entschlossensten Akteure des Kreuzzugs, leitete die Umlenkung auf Zara (1202) und dann auf Konstantinopel (1203/04). Er soll persönlich an der Landung von 1203 teilgenommen haben.",
        difficulty = 5,
        funFact = "Dandolo wurde nach der Einnahme Konstantinopels 1204 angeblich angeboten, selbst Kaiser des Lateinischen Reichs zu werden. Er lehnte ab und blieb als 'Herr eines Viertels und eines halben Viertels des Römischen Reiches' in Konstantinopel, wo er 1205 starb und im Hagia-Sophia-Komplex begraben wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches byzantinische Kunstwerk wurde 1204 aus Konstantinopel geraubt und steht heute vor dem Markusdom in Venedig?",
        answerA = "Die Porphyr-Tetrarchengruppe",
        answerB = "Die Quadriga der Vier Pferde (Pferde des Heiligen Markus)",
        answerC = "Der Obelisk des Theodosius",
        answerD = "Die Schlangensäule des Platäischen Weihgeschenks",
        correctAnswer = 1,
        explanation = "Die berühmten vier Bronzepferde (Quadriga) wurden 1204 vom Hippodrom in Konstantinopel geraubt und nach Venedig gebracht, wo sie auf der Galerie des Markusdoms aufgestellt wurden. Sie stammen ursprünglich wahrscheinlich aus dem 4. Jahrhundert v. Chr. oder der Römerzeit.",
        difficulty = 5,
        funFact = "Napoleon ließ die Pferde 1797 nach Paris bringen, wo sie als Kriegsbeute auf dem Arc du Carrousel standen. Nach Napoleons Niederlage wurden sie 1815 nach Venedig zurückgebracht. Die heutigen Pferde am Dom sind Kopien – die Originale stehen im Museum des Markusdoms."
    ),

    Question(
        categoryId = 3,
        questionText = "Welchen Anteil an den erbeuteten Gütern Konstantinopels hatte Venedig laut dem Teilungsvertrag (Partitio Romaniae) von 1204 erhalten?",
        answerA = "Ein Viertel",
        answerB = "Drei Achtel",
        answerC = "Die Hälfte",
        answerD = "Ein Drittel",
        correctAnswer = 1,
        explanation = "Laut der 'Partitio Terrarum Imperii Romaniae' erhielt Venedig drei Achtel (3/8) des Byzantinischen Reichs, während die restlichen fünf Achtel zwischen dem neuen Lateinischen Kaiser (3/8) und den anderen Kreuzfahrern (2/8) aufgeteilt wurden.",
        difficulty = 5,
        funFact = "Venedig wählte seine Gebiete strategisch aus: die wichtigsten Handelshäfen (Korfu, Modon, Coron, Kreta, Durazzo) – kein geschlossenes Territorium, sondern ein Archipel von Handelsstützpunkten. Dies begründete Venedigs Seeherrschaft im östlichen Mittelmeer für Jahrhunderte."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der erste Lateinische Kaiser von Konstantinopel, der nach dem Vierten Kreuzzug 1204 eingesetzt wurde?",
        answerA = "Bonifatius von Montferrat",
        answerB = "Gottfried von Villehardouin",
        answerC = "Balduin I. von Flandern",
        answerD = "Heinrich von Flandern",
        correctAnswer = 2,
        explanation = "Balduin I. von Flandern und Hennegau wurde am 9. Mai 1204 als erster Lateinischer Kaiser in der Hagia Sophia gekrönt. Er regierte nur kurz: 1205 wurde er in der Schlacht von Adrianopel gegen die Bulgaren gefangen genommen und starb in bulgarischer Gefangenschaft.",
        difficulty = 5,
        funFact = "Bonifatius von Montferrat, der nominelle Anführer des Kreuzzugs, wurde nicht Kaiser, weil Venedig ihn ablehnte. Er wurde König von Thessaloniki und gründete das Königreich Thessaloniki. Venedig bevorzugte Balduin, da man ihn für leichter kontrollierbar hielt."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr endete das Lateinische Kaiserreich von Konstantinopel, und wer stellte das Byzantinische Reich wieder her?",
        answerA = "1258, durch Johannes III. Dukas Vatatzes",
        answerB = "1261, durch Michael VIII. Palaiologos",
        answerC = "1272, durch Andronikos II. Palaiologos",
        answerD = "1265, durch Wilhelm II. von Villehardouin",
        correctAnswer = 1,
        explanation = "Michael VIII. Palaiologos, Kaiser des Kaiserreichs von Nikaia, ließ am 25. Juli 1261 durch seinen General Alexios Strategopoulos Konstantinopel zurückerobern, als die lateinische Besatzung geschwächt war. Er zog am 15. August 1261 in die Stadt ein und restaurierte das Byzantinische Reich.",
        difficulty = 5,
        funFact = "Michael VIII. zahlte einen hohen Preis für die Restauration: Er ließ seinen siebenjährigen Mitkaiser Johannes IV. Laskaris blenden und ins Kloster verbannen. Dafür wurde er vom Patriarchen Arsenios exkommuniziert, was das 'Arsenitische Schisma' auslöste, das die Kirche jahrzehntelang spaltete."
    ),

    // --- Additional Medieval Obscurities ---

    Question(
        categoryId = 3,
        questionText = "Wie hieß die häretische Sekte in Norditalien des 12./13. Jahrhunderts, die wie die Katharer dualistische Ansichten vertrat und durch Gerhard Segarelli in Parma gegründet wurde?",
        answerA = "Apostelbrüder (Apostolici)",
        answerB = "Fraticelli",
        answerC = "Spiritualen",
        answerD = "Humiliaten",
        correctAnswer = 0,
        explanation = "Die 'Apostelbrüder' (Apostolici), gegründet von Gerhard Segarelli ca. 1260 in Parma, predigten apostolische Armut und verwarfen die Kirche. Nach Segaellis Hinrichtung 1300 führte Fra Dolcino die Bewegung an, bis er 1307 nach einem bewaffneten Aufstand im Piemont gefangen und hingerichtet wurde.",
        difficulty = 5,
        funFact = "Fra Dolcino und seine Anhänger hielten sich zwei Winter lang im Piemont gegen päpstliche Kreuzzugsheere. Dante erwähnt Dolcino in der Hölle (Inferno XXVIII) und lässt Mohammed ihm eine Warnung zukommen lassen – ein bemerkenswertes Nebeneinander zweier 'Häretiker' in Dantes Weltbild."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches mittelalterliche Konzil von 1215 erließ Canon 21, der die jährliche Beichte und Kommunionpflicht einführte und auch den Begriff 'Transsubstantiation' offiziell kodifizierte?",
        answerA = "Erstes Laterankonzil (1123)",
        answerB = "Zweites Laterankonzil (1139)",
        answerC = "Drittes Laterankonzil (1179)",
        answerD = "Viertes Laterankonzil (1215)",
        correctAnswer = 3,
        explanation = "Das Vierte Laterankonzil (1215) unter Innozenz III. war eines der folgenreichsten Konzilien des Mittelalters. Canon 21 ('Omnis utriusque sexus') verpflichtete alle Gläubigen zur jährlichen Beichte und Osterkommunion. Das Konzil definierte auch Transsubstantiation als offizielle Eucharistielehre und erließ antijüdische Kanones.",
        difficulty = 5,
        funFact = "Das Vierte Laterankonzil verpflichtete Juden und Muslime zum Tragen von Erkennungszeichen (Judenhut, gelber Fleck) – eine Diskriminierungsmaßnahme, die über Jahrhunderte Bestand hatte und im nationalsozialistischen Judenstern eine schreckliche Fortsetzung fand."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches spezifische Verfahren führte die Inquisition ein, um Geständnisse zu erlangen, und wurde offiziell durch Papst Innozenz IV. in der Bulle 'Ad extirpanda' 1252 autorisiert?",
        answerA = "Wasserprobe (Ordal)",
        answerB = "Tortur",
        answerC = "Gottesurteil durch Feuer",
        answerD = "Zweikampf (Duellum)",
        correctAnswer = 1,
        explanation = "Papst Innozenz IV. autorisierte durch die Bulle 'Ad extirpanda' (1252) die Anwendung von Folter durch weltliche Behörden im Dienst der Inquisition, um Geständnisse und die Namen von Komplizen zu erhalten. Dies war ein entscheidender Schritt in der Institutionalisierung der Inquisitionsverfahren.",
        difficulty = 5,
        funFact = "Die Inquisition durfte selbst keine Folter anwenden – dies musste durch weltliche Behörden geschehen. Geständnisse unter Folter mussten am nächsten Tag ohne Folter wiederholt werden, um Gültigkeit zu haben. In der Praxis wurde diese Schutzklausel häufig umgangen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher byzantinische Kaiser ließ 1082 durch die 'Chrysobull' Venedig weitreichende Handelsprivilegien gewähren, die zum venezianischen Handelsimperium im Osten führten?",
        answerA = "Alexios I. Komnenos",
        answerB = "Johannes II. Komnenos",
        answerC = "Manuel I. Komnenos",
        answerD = "Isaak II. Angelos",
        correctAnswer = 0,
        explanation = "Alexios I. Komnenos gewährte 1082 Venedig durch eine Chrysobull weitreichende Handelsprivilegien: Zollfreiheit im gesamten Byzantinischen Reich, eigene Handelsniederlassungen in Konstantinopel und anderen Städten. Dies war der Preis für venezianische Flottenhilfe gegen die Normannen Robert Guiskards.",
        difficulty = 5,
        funFact = "Diese Chrysobull gab venezianischen Händlern mehr Rechte als byzantinischen Staatsbürgern selbst. Der daraus entstehende Neid führte 1171 zu einem Massaker an venezianischen Kaufleuten unter Manuel I. und vergiftete die Beziehungen nachhaltig bis zum Vierten Kreuzzug."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß das theologische Konzept, das im Streit zwischen Papst Bonifatius VIII. und König Philipp IV. von Frankreich aufkam, wonach der Papst über alle weltlichen Herrscher Gewalt habe?",
        answerA = "Translatio Imperii",
        answerB = "Plenitudo Potestatis",
        answerC = "Caesaropapismus",
        answerD = "Superiorem non Recognoscens",
        correctAnswer = 1,
        explanation = "Der Begriff 'Plenitudo Potestatis' (Fülle der Gewalt) bezeichnete den päpstlichen Anspruch auf vollständige kirchliche und indirekt auch weltliche Macht. Bonifatius VIII. artikulierte dies in der Bulle 'Unam Sanctam' (1302) auf dem Höhepunkt des Konflikts mit Philipp IV.",
        difficulty = 5,
        funFact = "Als Antwort auf 'Unam Sanctam' ließ Philipp IV. Bonifatius VIII. beim 'Attentat von Anagni' (1303) durch Guillaume de Nogaret körperlich angreifen. Der Schock soll den bereits kranken Papst innerhalb eines Monats getötet haben. Sein Nachfolger wurde der erste Avignon-Papst."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches böhmische Konzilsdekret von 1420 umriss die religiösen Forderungen der Hussiten in vier Artikeln, einschließlich des Laienkelchs?",
        answerA = "Vier Prager Artikel",
        answerB = "Kompaktaten von Basel",
        answerC = "Kutná-Hora-Dekret",
        answerD = "Confessio Bohemica",
        correctAnswer = 0,
        explanation = "Die Vier Prager Artikel (1420) fanden die religiösen Mindestforderungen der Hussiten: 1. Laienkelch (Kommunion unter beiden Gestalten), 2. Freie Predigt, 3. Kirchlicher Armutsbesitz nur nach geistlichem Recht, 4. Bestrafung öffentlicher Sünden durch weltliche Gerichte. Diese Artikel bildeten die Grundlage der hussitischen Verhandlungen.",
        difficulty = 5,
        funFact = "Die Hussiten spalteten sich in gemäßigte Utraquisten (Kelch) und radikale Taboriten. Die Taboriten entwickelten unter Jan Žižka die revolutionäre Wagenburg-Taktik und besiegten fünf päpstliche Kreuzzüge. Jan Žižka verlor im Kampf beide Augen und führte sein Heer trotzdem weiter."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Hochmeister des Deutschen Ordens schloss 1466 den Zweiten Frieden von Thorn ab, der die Ordensherrschaft in Preußen faktisch beendete?",
        answerA = "Ludwig von Erlichshausen",
        answerB = "Heinrich Reffle von Richtenberg",
        answerC = "Martin Truchsess von Wetzhausen",
        answerD = "Johann von Tiefen",
        correctAnswer = 0,
        explanation = "Hochmeister Ludwig von Erlichshausen unterzeichnete 1466 den Zweiten Frieden von Thorn, der den Dreizehnjährigen Krieg beendete. Der Orden verlor Westpreußen (Pommerellen, Kulmerland, Marienburg) an Polen und musste für den Restbesitz Ostpreußen die polnische Lehnshoheit anerkennen.",
        difficulty = 5,
        funFact = "Der Verlust Marienburgs war besonders symbolisch – die Ordensburg war seit 1309 der Sitz des Hochmeisters. Der Hochmeister musste in Königsberg residieren. Die Stadt Marienburg wurde zu 'Malbork' (polnisch) und ist heute als UNESCO-Welterbe bekannt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Konzil beschloss 1311/12 die Auflösung des Templerordens und übergab dessen Güter dem Johanniterorden?",
        answerA = "Konzil von Tours",
        answerB = "Konzil von Vienne",
        answerC = "Konzil von Sens",
        answerD = "Konzil von Bourges",
        correctAnswer = 1,
        explanation = "Das Konzil von Vienne (1311–1312) beschloss auf Druck König Philipps IV. von Frankreich die Aufhebung des Templerordens. Papst Clemens V. hob den Orden durch die Bulle 'Vox in excelso' (22. März 1312) auf. Die Güter sollten dem Johanniterorden zufallen, in Frankreich aber an die Krone.",
        difficulty = 5,
        funFact = "Großmeister Jacques de Molay und Präzeptor Gottfried de Charney wurden am 18. März 1314 auf einer Insel in der Seine als rückfällige Häretiker verbrannt. De Molay soll Philipp IV. und Clemens V. verflucht haben – beide starben tatsächlich noch im selben Jahr."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche mittellateinische Legendentradition beschreibt einen christlichen Priesterkönig in Äthiopien und wurde im 15. Jahrhundert durch portugiesische Entdeckungsreisen ernsthaft gesucht?",
        answerA = "Der Heilige Gral im Heiligen Land",
        answerB = "Die Legende von Presbyter Johannes",
        answerC = "Das Reich der Amazonen in Asien",
        answerD = "Das verlorene Paradies am Euphrat",
        correctAnswer = 1,
        explanation = "Die Legende des Priesterkönigs Johannes wurde im 15. Jahrhundert in Äthiopien verortet. Die Portugiesen entsandten Gesandte (Pero da Covilhã 1487) zur Suche nach diesem christlichen Bundesgenossen. Der äthiopische Kaiser Lebna Dengel wurde als möglicher 'Johannes' angesehen, was erste europäisch-äthiopische diplomatische Kontakte auslöste.",
        difficulty = 5,
        funFact = "Pero da Covilhã erreichte tatsächlich Äthiopien und lebte dort bis zu seinem Tod – aber er durfte das Land nicht verlassen. Die Portugiesen etablierten schließlich diplomatische Beziehungen mit Äthiopien und halfen dem Kaiser 1541 gegen osmanisch-unterstützte muslimische Invasoren (Ahmad Grañ)."
    ),

    Question(
        categoryId = 3,
        questionText = "Welchen Titel trug der Anführer der livländischen Kreuzfahrerkirche, der als weltlicher Herrscher über die nicht zum Deutschen Orden gehörigen Teile Livlands herrschte?",
        answerA = "Bischof von Riga",
        answerB = "Erzbischof von Riga",
        answerC = "Patriarch von Livland",
        answerD = "Fürstbischof von Dorpat",
        correctAnswer = 1,
        explanation = "Der Erzbischof von Riga war der höchste kirchliche und zeitweise auch weltliche Herr über Teile Livlands, der nicht zum Deutschen Orden gehörten. Es gab einen ständigen Konflikt zwischen dem Erzbischof von Riga, dem Livländischen Orden und den Bürgern Rigas um Vorherrschaft in der Region.",
        difficulty = 5,
        funFact = "Riga selbst wechselte mehrfach zwischen Ordensherrschaft und erzbischöflicher Kontrolle. Die Stadt schloss sich 1282 der Hanse an und versuchte, zwischen den rivalisierenden Mächten zu balancieren – ein kleiner Bürger-Stadtstaat eingeklemmt zwischen Ordensburg und Erzbischofssitz."
    ),

    // --- Byzantine Theme System / Additional Details ---

    Question(
        categoryId = 3,
        questionText = "Welches byzantinische Amt bezeichnete den Gouverneur eines Themas, der sowohl militärische als auch zivile Autorität in seiner Provinz vereinte?",
        answerA = "Logothetes",
        answerB = "Strategos",
        answerC = "Domestikos",
        answerD = "Katepano",
        correctAnswer = 1,
        explanation = "Der Strategos ('Feldherr') war der Gouverneur eines byzantinischen Themas und vereinte militärische und zivile Verwaltung in seiner Person. Dieses Amt ersetzte die spätantike Trennung von Zivil- und Militärgewalt und war entscheidend für die Verteidigung gegen arabische und bulgarische Angriffe im 7.–10. Jahrhundert.",
        difficulty = 5,
        funFact = "Der Strategos des Themas Anatolikon galt als einer der mächtigsten byzantinischen Beamten. Mehrere byzantinische Kaiser – darunter Nikephoros II. Phokas und Johannes I. Tzimiskes – stiegen als erfolgreiche Strategen zur Kaiserwürde auf und zeigten damit die gefährliche Machtfülle dieses Amtes."
    ),

    // --- Cathar Consolamentum Detail ---

    Question(
        categoryId = 3,
        questionText = "Wie bezeichneten die Katharer das einzige Sakrament, das sie anerkannten und das sowohl die Taufe als auch die Ordination der Perfecti darstellte?",
        answerA = "Endura",
        answerB = "Consolamentum",
        answerC = "Melioramentum",
        answerD = "Apparellamentum",
        correctAnswer = 1,
        explanation = "Das Consolamentum (lateinisch: 'Trost, Stärkung') war das einzige Sakrament der Katharer – eine Handauflegung und Geisttaufe, die den Empfänger von einem 'Credente' (Gläubigen) zum 'Perfectus' (Vollendeten) erhob oder einem Sterbenden die Absolution gewährte. Alle kirchlichen Sakramente der Katholiken lehnten die Katharer als Werkzeuge des bösen Schöpfers ab.",
        difficulty = 5,
        funFact = "Wer das Consolamentum auf dem Sterbebett empfangen hatte, war danach zur strengsten Askese verpflichtet. Um dem Rückfall in die Sünde vorzubeugen, praktizierten manche Katharer die 'Endura' – freiwilliges Fasten bis zum Tod. Die Inquisition betrachtete dies als verkappten Selbstmord und damit als weiteres Verbrechen."
    ),

)
