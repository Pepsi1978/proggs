package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsMedium5(): List<Question> = listOf(

    // ── INSECTS, REPTILES & AMPHIBIANS — MEDIUM (difficulty = 2) — 50 questions ──

    Question(
        categoryId = 9,
        questionText = "Wie nennt man die vollstaendige Verwandlung bei Insekten mit vier Entwicklungsstadien?",
        answerA = "Hemimetabolie",
        answerB = "Ametabolie",
        answerC = "Holometabolie",
        answerD = "Prometabolie",
        correctAnswer = 2,
        explanation = "Die Holometabolie bezeichnet die vollstaendige Metamorphose mit den vier Stadien Ei, Larve, Puppe und Imago. Schmetterlinge, Kafer und Fliegen durchlaufen diesen Prozess.",
        difficulty = 2,
        funFact = "Waehrend der Verpuppung loest sich der Koerper der Raupe fast vollstaendig auf und wird zu einer Art Zellsuppe, aus der sich der Schmetterling neu aufbaut."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Organ nutzen Schlangen hauptsaechlich, um Geruchsstoffe aufzunehmen?",
        answerA = "Nasenlocher",
        answerB = "Jacobsonsches Organ",
        answerC = "Infrarotgruben",
        answerD = "Ohroffnungen",
        correctAnswer = 1,
        explanation = "Das Jacobsonsche Organ (Vomeronasalorgan) im Gaumen der Schlange analysiert Duftmolekule, die die Zunge aufnimmt und dorthin transportiert.",
        difficulty = 2,
        funFact = "Schlangen strecken ihre gespaltene Zunge aus, um Geruchspartikel aus der Luft einzusammeln — die beiden Zungenspitzen ermoglichen ihnen dabei sogar eine Richtungsbestimmung."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen einem Froschlurch und einem Schwanzlurch?",
        answerA = "Froschlurche haben immer einen Schwanz, Schwanzlurche nicht",
        answerB = "Froschlurche leben nur im Wasser",
        answerC = "Schwanzlurche behalten den Schwanz auch als adulte Tiere",
        answerD = "Schwanzlurche sind keine Amphibien",
        correctAnswer = 2,
        explanation = "Schwanzlurche wie Molche und Salamander behalten ihren Schwanz das gesamte Leben. Froschlurche hingegen verlieren den Schwanz waehrend der Metamorphose.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Druse produziert bei Bienen das Bienenwachs?",
        answerA = "Mandibeldrusen",
        answerB = "Wachsdrusen am Hinterleib",
        answerC = "Kopfspeicheldrusen",
        answerD = "Duftdrusen an den Beinen",
        correctAnswer = 1,
        explanation = "Arbeitsbienen haben an der Bauchseite des Hinterleibs spezielle Wachsdrusen, aus denen fluessiges Wachs ausgeschieden wird. Es erstarrt zu kleinen Plattchen, die die Biene zum Wabenbau verwendet.",
        difficulty = 2,
        funFact = "Eine Biene muss etwa acht Gramm Honig verbrennen, um ein Gramm Wachs zu produzieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Stadium, in dem Froschlarven kurz vor der vollstandigen Metamorphose noch Kieme und Beine gleichzeitig besitzen?",
        answerA = "Quappe",
        answerB = "Nymphe",
        answerC = "Kaulquappe mit Beinen",
        answerD = "Metamorphling",
        correctAnswer = 2,
        explanation = "In der Endphase der Froschentwicklung wachsen zuerst die Hinterbeine, dann die Vorderbeine, wahrend die Kaulquappe noch ihren Schwanz besitzt. Erst danach wird der Schwanz resorbiert.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Eigenschaft besitzt das Gift der Blaupfeilgiftfrosche in der Wildnis, die in Gefangenschaft fehlt?",
        answerA = "Es ist wasserloslich",
        answerB = "Es entsteht durch die Nahrung",
        answerC = "Es wird durch Sonnenlicht erzeugt",
        answerD = "Es ist nur nachts wirksam",
        correctAnswer = 1,
        explanation = "Giftfrosche synthetisieren ihr Hautgift nicht selbst, sondern nehmen Vorlaeufer-Alkaloide durch den Verzehr bestimmter Ameisen und Milben auf. In Gefangenschaft fehlt diese Nahrungsquelle, weshalb die Tiere ungiftig bleiben.",
        difficulty = 2,
        funFact = "Indigene Volker in Sudamerika nutzten das Hautsekret der Blattsteigerfrosche, um Blasrohpfeile zu vergiften — daher der Name Pfeilgiftfrosch."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die sogenannte Hautosmose bei Amphibien?",
        answerA = "Trinken durch die Haut",
        answerB = "Atmung ausschliesslich durch die Haut",
        answerC = "Farbwechsel durch Hautzellen",
        answerD = "Fortpflanzung uber Hautsekretion",
        correctAnswer = 0,
        explanation = "Amphibien nehmen Wasser nicht durch Trinken auf, sondern durch die permeable Haut. Besonders der Bauch und die Oberschenkel enthalten spezialisierte, wasserdurchlassige Hautbereiche.",
        difficulty = 2,
        funFact = "Krotenarten konnen nach langer Trockenheit innerhalb von Minuten erhebliche Wassermengen durch die Haut aufnehmen, wenn sie einen feuchten Untergrund beruehren."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie regulieren ektotherme Reptilien ihre Koerpertemperatur?",
        answerA = "Durch innere Waermeproduktion wie Saugetiere",
        answerB = "Durch Verhaltensanpassungen wie Sonnen und Schatten aufsuchen",
        answerC = "Durch beschleunigten Herzschlag",
        answerD = "Durch ein spezielles Hautorgan",
        correctAnswer = 1,
        explanation = "Reptilien sind ektotherm und erzeugen kaum eigene Korperwarme. Sie regulieren ihre Temperatur durch Verhalten: Sie sonnen sich zur Erwarmung oder suchen Schatten und kuhlere Platze zur Abkuhlung.",
        difficulty = 2,
        funFact = "Manche Reptilien konnen ihre Herzfrequenz gezielt verandern, um beim Sonnen schneller warm zu werden und beim Abkuhlen die Warme langer zu halten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus ermoglicht Geckos, an glatten Oberflachen zu laufen?",
        answerA = "Saugnapfe an den Zehen",
        answerB = "Van-der-Waals-Krafte durch Millionen von Nanohaarchen",
        answerC = "Klebsekretion aus Drosen",
        answerD = "Electrostatische Aufladung",
        correctAnswer = 1,
        explanation = "Die Zehenballen von Geckos sind mit Millionen winziger Haare (Setae) bedeckt, die wiederum in noch kleinere Spitzen (Spatulae) aufgeteilt sind. Diese erzeugen Van-der-Waals-Krafte, die das Haften ermoglichen.",
        difficulty = 2,
        funFact = "Ein Tokeh-Gecko (ca. 300 g) kann theoretisch sein eigenes Korpergewicht etwa 133-mal an einer Glasscheibe halten — die Haftkraft seiner Zehen ist enorm."
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter der Kastenteilung in einem Ameisenkolonie?",
        answerA = "Die geografische Trennung verschiedener Kolonien",
        answerB = "Die Aufteilung der Koloniemitglieder in spezialisierte Gruppen mit unterschiedlichen Aufgaben",
        answerC = "Das Teilen von Nahrungsquellen zwischen rivalisierenden Kolonien",
        answerD = "Die Verteilung der Koronin auf verschiedene Nester",
        correctAnswer = 1,
        explanation = "In Ameisenkolonien gibt es verschiedene Kasten: die Konigin (Fortpflanzung), Arbeiterinnen (Nahrungssuche, Brutpflege), und bei manchen Arten Soldaten (Verteidigung). Jede Kaste ist morphologisch und physiologisch spezialisiert.",
        difficulty = 2,
        funFact = "Bei manchen Ameisenarten gibt es Soldaten mit uberdimensional grossen Kopfen und Kiefern, die wie lebende Stropsel Nesteingnge verstopfen konnen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Reptiliengruppe ist die artenreichste auf der Erde?",
        answerA = "Krokodile",
        answerB = "Schildkroten",
        answerC = "Schuppenreptilien (Squamata)",
        answerD = "Bruckenechsen (Tuatara)",
        correctAnswer = 2,
        explanation = "Die Schuppenreptilien umfassen Schlangen und Eidechsen (einschliesslich Chamoleons, Geckos, Monitore) mit uber 10.000 Arten und bilden bei weitem die gro??te Reptiliengruppe.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Funktion der Tympanalmembran bei Heuschrecken?",
        answerA = "Zur Nahrungsaufnahme",
        answerB = "Als Gehorsorgan zur Schallwahrnehmung",
        answerC = "Zur Temperaturregulation",
        answerD = "Als Lautproduktionsorgan",
        correctAnswer = 1,
        explanation = "Das Tympanalorgan (Trommelfell) bei Heuschrecken sitzt am ersten Hinterleibssegment und dient der Schallwahrnehmung. Damit konnen sie die Gesange von Artgenossen orten.",
        difficulty = 2,
        funFact = "Feldgrillen konnen ihren Artgenossen uber mehrere hundert Meter horen — das entspricht dem Hunderttausandfachen ihrer eigenen Korpergrosse."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Phanomen, bei dem Schmetterlinge schadliche Arten in Farbgebung und Musterung nachahmen?",
        answerA = "Kryptik",
        answerB = "Somatolyse",
        answerC = "Batessche Mimikry",
        answerD = "Mullerscheinstimulation",
        correctAnswer = 2,
        explanation = "Bei der Batesschen Mimikry ahmen ungefahrliche Arten die Warntrachten giftiger oder gefahrlicher Arten nach. Fressfeinde lernen die Warnung und meiden so auch die harmlose Nachahmungsart.",
        difficulty = 2,
        funFact = "Das Monarch-Schmetterling-Prinzip: Der essbare Vizekonigsschmetterling imitiert den giftigen Monarchfalter so perfekt, dass Vogel auch ihn meiden."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Merkmal unterscheidet Krokodile von Alligatoren?",
        answerA = "Krokodile haben keinen Schwanz",
        answerB = "Bei Krokodilen ist der vierte Unterkieferzahn bei geschlossenem Maul sichtbar",
        answerC = "Alligatoren sind grosser",
        answerD = "Krokodile leben nur im Salzwasser",
        correctAnswer = 1,
        explanation = "Bei Krokodilen (Familie Crocodylidae) ist der grosse vierte Zahn des Unterkiefers auch bei geschlossenem Maul zu sehen. Bei Alligatoren passt dieser Zahn in eine Ausholung des Oberkiefers und ist versteckt.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter der Trophallaxis bei sozialen Insekten?",
        answerA = "Den Tanzcode der Bienen",
        answerB = "Die Weitergabe von Nahrung von Mund zu Mund zwischen Kolonimiegliedern",
        answerC = "Das Markieren von Nahrungsquellen mit Duftstoffen",
        answerD = "Die Aggressionsgeste bei Wespen",
        correctAnswer = 1,
        explanation = "Trophallaxis bezeichnet die direkte Ubertragung von Nahrungsflussigkeit von einem Tier zum anderen durch Mundkontakt. Sie dient der Nahrungsverteilung, dem Informationsaustausch und der sozialen Bindung.",
        difficulty = 2,
        funFact = "Uber Trophallaxis konnen Ameisen nicht nur Nahrung, sondern auch Pheromone und chemische Signale austauschen, die die Kastenentwicklung beeinflusse konnen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Anpassung besitzen Wustenschildkroten um in extremer Trockenheit zu uberleben?",
        answerA = "Sie produzieren kein Urin",
        answerB = "Sie speichern Wasser in der Harnblase und resorbieren es bei Bedarf",
        answerC = "Sie trinken durch die Haut wie Frosche",
        answerD = "Sie graben sich ein und atmen Feuchtigkeit aus dem Boden",
        correctAnswer = 1,
        explanation = "Wustenschildkroten wie die Agassiz-Wustenschildkrote speichern grosse Mengen stark verdunnten Urins in der Harnblase, bis zu 40% ihres Korpergewichts. In Trockenzeiten wird dieses Wasser wieder aufgenommen.",
        difficulty = 2,
        funFact = "Indigene Volker wussten von dieser Eigenschaft und konnten durch das Aufheben einer Wustenschildkrote in Notlagen Trinkwasser gewinnen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welchen Zweck hat das Stridulationsorgan bei Grillen?",
        answerA = "Nahrungsaufnahme",
        answerB = "Warnen vor Feinden durch Ultraschall",
        answerC = "Lauterzeugung durch Reiben der Flugel aneinander",
        answerD = "Ausscheidung von Duftstoffen",
        correctAnswer = 2,
        explanation = "Bei Grillen (und manchen Heuschreckenarten) wird Schall durch Stridulation erzeugt: Eine gezahnte Ader des einen Flugels reibt uber eine scharfe Ader des anderen Flugels (Pars stridens und Plektrum).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Neotenie bei Amphibien?",
        answerA = "Das Phanonem, dass adulte Tiere Larvenmerkmale dauerhaft behalten",
        answerB = "Besonders schnelles Wachstum der Jungtiere",
        answerC = "Die Fahigkeit zur Hautregeneration",
        answerD = "Geschlechtsumwandlung wahrend des Lebens",
        correctAnswer = 0,
        explanation = "Neotenie beschreibt das Phanomen, dass geschlechtsreife adulte Tiere Jugendmerkmale wie aussere Kiemen behalten. Der Axolotl ist das bekannteste Beispiel — er bleibt zeitlebens in der Larvenform, ist aber fortpflanzungsfahig.",
        difficulty = 2,
        funFact = "Der Axolotl ist ein berumhtes Beispiel fur Neotenie und zudem einer der am besten erforschten Organismen fur Regenerationsforschung — er kann ganze Glieder nachwachsen lassen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Pheromonkommunikationssystem bei Ameisen, das Wegmarkierungen erzeugt?",
        answerA = "Taktiler Kanal",
        answerB = "Spurpheromon",
        answerC = "Alarmferomon",
        answerD = "Kastenferomon",
        correctAnswer = 1,
        explanation = "Ameisen setzen Spurpheromone (chemische Markierungen) auf dem Weg zur Nahrungsquelle ab. Andere Ameisen folgen dieser Spur. Je mehr Ameisen eine Spur verwenden, desto starker und effektiver wird sie.",
        difficulty = 2,
        funFact = "Das Spurpheromon ist so effektiv, dass Ameisen selbst bei experimentell im Kreis gelegten Spuren endlos im Kreise laufen, bis sie erschopft zusammenbrechen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Reptiliengruppe ist am nachsten mit den Vogeln verwandt?",
        answerA = "Schildkroten",
        answerB = "Chamoleons",
        answerC = "Krokodile",
        answerD = "Komodowarane",
        correctAnswer = 2,
        explanation = "Krokodile und Vogel gehoren beide zu den Archosauria. Vogel sind aus Theropoden-Dinosauriern hervorgegangen, und Krokodile teilen mit Vogeln mehr gemeinsame Vorfahren als jede andere heute lebende Reptiliengruppe.",
        difficulty = 2,
        funFact = "Krokodile zeigen tatsachlich vogelartige Verhaltensweisen wie Brutpflege, Nestbau und Jungtierbetreuung — Merkmale, die bei Reptilien aussergewohnlich sind."
    ),

    Question(
        categoryId = 9,
        questionText = "Was produziert die Giftzentrale eines Skorpions und wo befindet sie sich?",
        answerA = "In den Scheren, Kontaktgift",
        answerB = "Im Stachel am Schwanzende, Neurotoxin oder Cytotoxin",
        answerC = "In der Mundregion, Verdauungsenzyme",
        answerD = "In den Beinen, Beruhigungsgift",
        correctAnswer = 1,
        explanation = "Der Giftstachel des Skorpions befindet sich am letzten Segment des Schwanzes (Telson). Die Giftkapseln enthalten je nach Art haupt sachlich Neurotoxine oder cytotoxische Substanzen.",
        difficulty = 2,
        funFact = "Von uber 2.500 Skorpionarten sind nur etwa 25 fur Menschen gefahrlich. Der gefahrlichste ist der Deathstalker (Leiurus quinquestriatus) aus Nordafrika."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Chorion bei Insekteneiern?",
        answerA = "Die Dottermasse",
        answerB = "Die harte, schutzen de Aussenschale des Eies",
        answerC = "Die Larve innerhalb des Eies",
        answerD = "Die Eileiterflussigkeit",
        correctAnswer = 1,
        explanation = "Das Chorion ist die harte, vom Mutterfolliker gebildete Aussenhule des Insekteneies. Es schutzt das Ei vor Austrocknung und mechanischen Schaden, enthalt aber Mikropyle-Offnungen fur die Befruchtung.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Strategie nutzen Chamoleons zur Jagd, die einzigartig unter Wirbeltieren ist?",
        answerA = "Sie verfolgen Beute mit Hochgeschwindigkeit",
        answerB = "Sie fangen Insekten mit der Zunge, die schneller als ein Auge blinzeln ist",
        answerC = "Sie graben Fallen fur Insekten",
        answerD = "Sie verwenden Toxine aus der Haut zum Lahmlegen",
        correctAnswer = 1,
        explanation = "Die Zunge eines Chamolon kann in 0,07 Sekunden ausgestreckt werden und dabei die Beute mit einem klebrigen Muskelpads packen. Die Beschleunigung ubertrifft die eines Sportwagens um ein Vielfaches.",
        difficulty = 2,
        funFact = "Ein Chamoleon kann mit seiner Zunge Beute fangen, die bis zu 1,5 Korperlagen entfernt ist — und das bei einer Zunge, die langer als der gesamte Korper sein kann."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Haemolymphe bei Insekten?",
        answerA = "Eine Art Blutzellenflussigkeit, die im offenen Kreislaufsystem zirkuliert",
        answerB = "Ein Verdauungssaft",
        answerC = "Das Flu ssigkeit in den Gelenken",
        answerD = "Eine Schutzsubstanz in der Cuticula",
        correctAnswer = 0,
        explanation = "Haemolymphe ist das Aquivalent von Blut bei Insekten. Sie zirkuliert in einem offenen System (keine geschlossenen Blutgefasse) und transportiert Nahrstoffe, Hormone und Abfallprodukte, aber kaum Sauerstoff.",
        difficulty = 2,
        funFact = "Da Sauerstofftransport nicht durch die Haemolymphe erfolgt, haben Insekten ein direktes Tracheensystem, das Sauerstoff uber feine Rohren direkt zu den Zellen leitet."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen ovipar und ovovivipar bei Reptilien?",
        answerA = "Ovipar = lebend gebarend, ovovivipar = eierlegend",
        answerB = "Ovipar = eierlegend, ovovivipar = Eier schlupfen im Mutterleib",
        answerC = "Ovipar = asexuelle Vermehrung, ovovivipar = Hermaphrodismus",
        answerD = "Kein Unterschied, beides bedeutet lebend gebarend",
        correctAnswer = 1,
        explanation = "Ovipare Reptilien legen Eier, aus denen Junge schlupfen. Ovovivipare Reptilien behalten die Eier im Korper, bis sie schlupfen — die Jungen werden also lebend geboren, aber aus Eiern, ohne Plazenta.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Besonderheit hat das Immunsystem des Komodowarans, das lange falsch verstanden wurde?",
        answerA = "Es produziert kein Antikorp er gegen eigenes Fleisch",
        answerB = "Sein Speichel enthalt nicht hauptsachlich Bakterien, sondern Gift aus Unterkieferdrusen",
        answerC = "Es ist komplett immun gegen alle bekannten Bakterien",
        answerD = "Das Immunsystem ist rudimentar und fast nicht vorhanden",
        correctAnswer = 1,
        explanation = "Lange glaubte man, Komodowarane toton durch Bakterien im Speichel. Neuere Forschung zeigte, dass sie tatsachlich Giftdrusen im Unterkiefer besitzen, die antikoagulante Substanzen produzieren und Beute schwachen.",
        difficulty = 2,
        funFact = "Komodowarane konnen Beute viel grosser als sie selbst reissen — sie haben eine enorm flexible Kieferstruktur und konnen bis zu 80% ihres Korpergewichts auf einmal fressen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie heisst das pheromonegesteuerte Massenphano men bei Heuschrecken, das Schwarmbildung ausloest?",
        answerA = "Aggregationspheromon-Response",
        answerB = "Gregarisierung durch Serotonin",
        answerC = "Alarmkommunikation",
        answerD = "Sexualferomon-Bindung",
        correctAnswer = 1,
        explanation = "Wenn Heuschrecken der Art Schistocerca gregaria haufig beruhrungsreize an den Hinterbeinen erfahren, wird Serotonin ausgeschuttet, das eine Phasenumwandlung von der solitaren zur gregaren (schwarmenden) Form ausloest.",
        difficulty = 2,
        funFact = "Ein einziger Heuschrecken schwarm kann Milliarden von Individuen enthalten und taglich genug Nahrung vernichten, um 35.000 Menschen zu ernahren."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Funktion der Infrarotgrubenorgane bei Grubenottern?",
        answerA = "Gehorsinn",
        answerB = "Tastsinn fur Bodenvibration",
        answerC = "Warmedetektion zur Jagd auf warmblutie Beute",
        answerD = "Elektrorezeption wie bei Haien",
        correctAnswer = 2,
        explanation = "Grubenottern (Crotalinae) besitzen zwischen Auge und Nase Grubenorgane mit einer warmesensitiven Membran. Diese erkennen Temperaturunterschiede von 0,003 Grad und ermoglichen das Aufspuren warmblutiger Beute in totaler Dunkelheit.",
        difficulty = 2,
        funFact = "Klapperschlangen konnen im Infrarot-Sinnesbild praktisch sehen — ihre Grubenorgane erzeugen ein Warme bild, das ihr visuelles Sehen erganzt."
    ),

    Question(
        categoryId = 9,
        questionText = "Warum sind viele Amphibienarten weltweit stark gefahrdet?",
        answerA = "Wegen der globalen Ausbreitung der Chytridiomykose-Pilzkrankheit",
        answerB = "Wegen invasiver Fischarten allein",
        answerC = "Wegen Ozonloch-bedingter UV-Strahlung",
        answerD = "Wegen fehlender Fortpflanzung in Gefangenschaft",
        correctAnswer = 0,
        explanation = "Der Chytridpilz Batrachochytrium dendrobatidis (Bd) hat weltweit uber 200 Amphibienarten dezimiert oder ausgerottet. Die Krankheit befalt die Haut und stort die Wasseraufnahme, was zum Herzversagen fuhrt.",
        difficulty = 2,
        funFact = "Chytridiomykose gilt als die destruktivste Infektionskrankheit eines Wirbeltiers in der gesamten Menschheitsgeschichte — mehr Artenaussterbeereignisse als jede andere Krankheit."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Komplexauge bei Insekten und wie unterscheidet es sich vom menschlichen Auge?",
        answerA = "Es ist ein einfaches Linsenauge mit hoher Brennweite",
        answerB = "Es besteht aus vielen einzelnen Augeneinheiten (Ommatidien), die ein Mosaikbild erzeugen",
        answerC = "Es nimmt nur Schwarz-Weiss wahr",
        answerD = "Es ist unter der Cuticula versteckt und nicht sichtbar",
        correctAnswer = 1,
        explanation = "Das Facettenauge (Komplexauge) besteht aus hunderten bis tausenden von Ommatidien, die jeweils ein kleines Bildausschnitterfassen. Das Gehirn kombiniert diese zu einem Mosaikbild mit sehr weitem Blickfeld.",
        difficulty = 2,
        funFact = "Libellenaugen bedecken fast den gesamten Kopf mit bis zu 30.000 Ommatidien und bieten fast 360-Grad-Sicht — ausserdem konnen sie UV-Licht und polarisiertes Licht wahrnehmen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist parthenogenetische Fortpflanzung und bei welcher Reptiliengruppe kommt sie vor?",
        answerA = "Zweigeschlechtliche Fortpflanzung mit mehreren Partnern",
        answerB = "Unbefruchtete Eier entwickeln sich zu Jungtieren; kommt bei manchen Echsen vor",
        answerC = "Asexuelle Knospung wie bei Korallen",
        answerD = "Befruchtung durch konservierte Spermien nach Jahren",
        correctAnswer = 1,
        explanation = "Bei der Parthenogenese entwickeln sich unbefruchtete Eier zu lebensfahigen Nachkommen. Bei Reptilien tritt dies zum Beispiel bei bestimmten Krustenechsen (Aspidoscelis), Komodowaranen und manchen Pythonarten auf.",
        difficulty = 2,
        funFact = "Weibliche Komodowarane konnen sich unter Isolation parthenogenetisch fortpflanzen — die Nachkommen sind immer mannlich, da Komodowarane ein ZW-Geschlechtsdeterminations-System haben."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Puppe bei holometabolen Insekten und welche Prozesse laufen in ihr ab?",
        answerA = "Ein Ruhestadium ohne innere Aktivitat",
        answerB = "Ein Stadium intensiver innerer Umstrukturierung vom Larvenkorper zum Adulttier",
        answerC = "Das Stadium in dem das Insekt frisst und wachst",
        answerD = "Ein Schutzmantel um das Ei",
        correctAnswer = 1,
        explanation = "Wahrend der Verpuppung wird der Larvenkorper durch Histolyse weitgehend aufgelost. Aus imaginalscheiben (Stammzellgruppen) bilden sich dann die adulten Strukturen neu — ein kompletter Korperbauplan-Umbau.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Schildkrotenart legt ihre Eier ausschliesslich an einem einzigen Strand ab, was sie besonders verletzlich macht?",
        answerA = "Lederschildkrote",
        answerB = "Unechte Karettschildkrote",
        answerC = "Kemp's Ridley-Meeresschildkrote",
        answerD = "Australische Flachschildkrote",
        correctAnswer = 2,
        explanation = "Die Kemps Ridley-Meeresschildkrote (Lepidochelys kempii) brutet fast ausschliesslich am Rancho Nuevo-Strand in Mexiko. Dieses Verhalten (Arribada = Massenlandung) macht sie extrem vulnerabel gegenuber lokalen Bedrohungen.",
        difficulty = 2,
        funFact = "In den 1940ern wurden an diesem Strand in einem Tag uber 40.000 Schildkroten gesichtet. Durch Bejagung sank die Zahl auf unter 300 Nistende in den 1980ern — heute erholt sich die Art langsam."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Druse produziert das Koeniginnenpheromon bei Honigbienen?",
        answerA = "Wachsdrusen",
        answerB = "Mandibeldrusen der Konigin",
        answerC = "Nasanov-Druse",
        answerD = "Dufour-Druse",
        correctAnswer = 1,
        explanation = "Die Mandibelkoniginsubstanz (9-ODA) wird in den Mandibeldrusen der Bienenkonigin produziert. Sie verhindert die Entwicklung der Ovarien der Arbeiterinnen, hemmt den Aufbau von Weiselzellen und zieht Drohnen zur Paarung an.",
        difficulty = 2,
        funFact = "Fallt die Konigin aus, bemerken Arbeiterinnen innerhalb von Minuten den Verlust des Pheromons und beginnen sofort mit dem Aufbau neuer Koniginzellen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen Hyla und Rana-Frischarten bezuglich ihrer Fortbewegung?",
        answerA = "Hyla sind ausschliesslich Wasserbewohner, Rana leben an Land",
        answerB = "Hyla (Laubfrosche) klettern durch Haftscheiben an Zehen, Rana (Echte Frosche) springen und schwimmen",
        answerC = "Rana konnen fliegen, Hyla nicht",
        answerD = "Hyla laufen auf allen Vieren, Rana ausschliesslich springen",
        correctAnswer = 1,
        explanation = "Laubfrosche (Hyla) haben spezialisierte Haftscheiben an den Zehenspitzen mit einer klebrigen Sekretion, die ihnen das Klettern an glatten Oberflachen ermoglicht. Echte Frosche (Rana) sind auf Springen und Schwimmen optimiert.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Thanatose bei Insekten und Reptilien?",
        answerA = "Aggressive Kampfhaltung zur Abschreckung",
        answerB = "Sich-Totstellen als Schutzstrategie",
        answerC = "Abwurf von Korperteilen bei Gefahr",
        answerD = "Flucht durch Tarnung",
        correctAnswer = 1,
        explanation = "Thanatose (Totstellreflex) ist eine Verteidigungsstrategie, bei der das Tier bewegungslos verbleibt und einen Todesfall simuliert. Viele Rauber reagieren nur auf Bewegung und lassen das vermeintlich tote Tier in Ruhe.",
        difficulty = 2,
        funFact = "Die Virginia-Opossum ist das beruhmteste Beispiel, aber auch Schlangen wie die Hog-Nosed Snake rollen sich auf den Rucken, offnen den Mund und strecken die Zunge heraus, um uberzeugend tot zu wirken."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher chemische Prozess ermoglicht Gluhhwurmchen das Leuchten?",
        answerA = "Phosphoreszenz durch chemische Pigmente",
        answerB = "Biolumineszenz durch Luciferase-katalysierte Luciferin-Oxidation",
        answerC = "Reflexion von Mondlicht durch spezielle Schichten",
        answerD = "Elektrische Entladung wie bei Zitteraalen",
        correctAnswer = 1,
        explanation = "Das Leuchten der Gluhhwurmchen entsteht durch die enzymatische Reaktion: Das Enzym Luciferase katalysiert die Oxidation des Substrats Luciferin unter ATP-Verbrauch. Dabei wird Energie als Licht abgegeben statt als Warme.",
        difficulty = 2,
        funFact = "Gluhhwurmchen-Biolumineszenz ist fast 100% effizient — nur ein winziger Bruchteil der Energie wird als Warme abgegeben. Gluhlampem haben dagegen nur etwa 5% Lichtausbeute."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man den Abwurf des Schwanzes bei Eidechsen als Fluchtmechanismus?",
        answerA = "Histolyse",
        answerB = "Autotomie",
        answerC = "Nekrotomie",
        answerD = "Apoptose",
        correctAnswer = 1,
        explanation = "Autotomie bezeichnet den willentlichen Abwurf eines Korperteils als Verteidigungsmechanismus. Der abgeworfene Schwanz der Eidechse zuckt weiter und lenkt den Rauber ab. Der Schwanz regeneriert sich spater, enthalt aber Knorpel statt Knochen.",
        difficulty = 2,
        funFact = "Beim Nachwachsen des Schwanzes bildet sich keine vollstandige Wiederholung des Originals: Der neue Schwanz enthalt Knorpelgewebe statt Wirbel und oft ein anderes Schuppenmuster."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Funktion der Ocelli bei Insekten?",
        answerA = "Farbwahrnehmung wie die Facettenaugen",
        answerB = "Einfache Punktaugen zur Lichtintensitatswahrnehmung und Horizonterkennung",
        answerC = "Infrarot-Wahrneh mung zur Warmelokalisation",
        answerD = "Ultraschallortung wie bei Fleder mausen",
        correctAnswer = 1,
        explanation = "Die Ocelli sind einfache Punktaugen auf dem Scheitel vieler Insekten. Sie konnen keine scharfen Bilder erzeugen, sind aber extrem empfindlich fur Lichtintensitaten und helfen bei der Flugstabilisierung sowie der Horizonterkennung.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Eigenschaft macht den Axolotl wissenschaftlich besonders wertvoll?",
        answerA = "Seine Biolumineszenz",
        answerB = "Seine aussergewohnliche Regenerationsfahigkeit ganzer Gliedmassen und Organe",
        answerC = "Seine Fahigkeit in extremer Kalte zu uberleben",
        answerD = "Sein hochentwickeltes soziales Verhalten",
        correctAnswer = 1,
        explanation = "Der Axolotl (Ambystoma mexicanum) kann nicht nur Gliedmassen, sondern auch Teile des Herzens, der Augen und sogar des Gehirns regenerieren. Wissenschaftler erforschen diese Fahigkeit fur medizinische Anwendungen beim Menschen.",
        difficulty = 2,
        funFact = "Der Axolotl ist in freier Wildbahn vom Aussterben bedroht — er kommt nur im Xochimilco-See-System bei Mexico-Stadt vor. In Forschungslabors und als Heimtier lebt er jedoch weltweit in Tausenden von Exemplaren."
    ),

    Question(
        categoryId = 9,
        questionText = "Was unterscheidet Neurotoxine von Haemotoxinen im Kontext von Schlangengiften?",
        answerA = "Neurotoxine sind immer starker als Haemotoxine",
        answerB = "Neurotoxine lah men das Nervensystem, Haemotoxine zerstoren Blut und Gewebe",
        answerC = "Haemotoxine kommen nur bei Korallensch langen vor",
        answerD = "Neurotoxine werden oral aufgenommen, Haemotoxine injiziert",
        correctAnswer = 1,
        explanation = "Neurotoxische Schlangengif te (z.B. Kobras, Mambas) blockieren Nervenubertragung und fuhren zu Lahmung und Atemstillstand. Haemotoxische Gifte (z.B. viele Vipern) storen die Blutgerinnung und zerstoren Gewebe.",
        difficulty = 2,
        funFact = "Manche Schlangenarten wie die Mojave-Klapperschlange besitzen ein komplexes Gift mit sowohl neuro- als auch haemotoxischen Komponenten — besonders gefahrlich fur Opfer."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie kommunizieren Termiten ihre Alarmsignale innerhalb des Nestes?",
        answerA = "Durch Ultraschall aus spez ialisierten Kopforganen",
        answerB = "Durch Pheromone und Kopfschlagen gegen den Tunnelboden",
        answerC = "Durch Farbveranderungen an der Cuticula",
        answerD = "Durch elektrische Impulse ubers Nestgeflecht",
        correctAnswer = 1,
        explanation = "Termitensoldaten schlagen bei Gefahr rhythmisch mit dem Kopf gegen den Tunnelboden, was Vibrationssignale erzeugt die sich schnell durch das Nest verbreiten. Gleichzeitig werden Alarmpheromone ausgeschuttet.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter Ecdysis bei Insekten und Reptilien?",
        answerA = "Den Winterschlaf",
        answerB = "Die Hautung, also das Abwerfen der alten Aussen haut",
        answerC = "Die Fortpflanzungsphase",
        answerD = "Das Absterben alter Korperzellen",
        correctAnswer = 1,
        explanation = "Ecdysis bezeichnet den Hautungsvorgang. Da Insekten und Reptilien ein unvermehrbares Aussenskelett (Cuticula) oder undehnbare Schuppen besitzen, mussen sie dieses regelmasig abwerfen um wachsen zu konnen.",
        difficulty = 2,
        funFact = "Kurz vor der Hautung bildet Schlangen eine trubes Aussehen uber den Augen durch Flussigkeit zwischen alter und neuer Hautschicht — in dieser Phase sind sie fast blind und besonders aggressiv."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Bedeutung der Temperatur bei der Geschlechtsbestimmung von Krokodilen?",
        answerA = "Die Temperatur beeinflusst die Geschlechtsbestimmung nicht",
        answerB = "Hohere Bebrustungstemperaturen (32-34 Grad C) ergeben Mannchen, niedrigere Weibchen",
        answerC = "Die Temperatur bestimmt nur die Hatchling-Grosse",
        answerD = "Alle Krokodile sind bei Geburt zunachst weiblich",
        correctAnswer = 1,
        explanation = "Bei Krokodilen (und vielen anderen Reptilien) gibt es keine genetische Geschlechtsdetermination. Das Geschlecht wird durch die Bruttemperatur bestimmt (TSD = Temperature-dependent Sex Determination). Bei Krokodilen werden Mannchen bei Mitteltemperaturen geboren.",
        difficulty = 2,
        funFact = "Der Klimawandel bedroht Arten mit TSD besonders stark: Veranderungen der Bodentemperaturen konnen zu stark verschobenen Geschlechterverhaltnissen fuhren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Anpassung hat die Wustenohrenkroete (Scaphiopus) entwickelt um extreme Trockenheit zu uberleben?",
        answerA = "Sie wandert jahrlich zu Wasserquellen",
        answerB = "Sie vergrabst sich und bildet einen Schleimkokon wahrend der Trockenzeit",
        answerC = "Sie kann Wasser aus der Luft kondensieren",
        answerD = "Sie schlaft tief und atmet kein Sauerstoff",
        correctAnswer = 1,
        explanation = "Spatenfusskroten graben sich in den Boden und bilden eine Zyste (Kokon) aus ausgeha rtetem Hautsekret, die Wasserverlust minimiert. Sie konnen so bis zu zwei Jahre in Sommerlethargie (Estivation) uberdauern.",
        difficulty = 2,
        funFact = "Nach dem ersten Regen kommen diese Kroten innerhalb von Stunden aus dem Boden, paaren sich und legen Eier, die in weniger als zwei Wochen zu kleinen Froskhen herangewachsen sind — extremste Anpassung an Wustenumgebung."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Tracheensystem bei Insekten und welchen Vorteil bietet es?",
        answerA = "Ein Nervensystem zur Koordination von Bewegungen",
        answerB = "Ein System von Luftrohren, das Sauerstoff direkt an alle Gewebe liefert ohne Blut",
        answerC = "Ein Verdauungssystem mit mehreren Magen",
        answerD = "Ein Wasserleitungssystem in der Cuticula",
        correctAnswer = 1,
        explanation = "Das Tracheensystem besteht aus verzweigten Luftrohren (Tracheen), die durch Atemoffnungen (Stigmen) verbunden sind. Sauerstoff diffundiert direkt in die Gewebe — kein Transportsystem wie Blut notwendig.",
        difficulty = 2,
        funFact = "Das Tracheensystem limitiert die maximale Grosse von Insekten: In der Karbonzeit mit 35% Sauerstoff in der Atmosphare gab es Libellen mit 70 cm Flugel spannweite — heute mit 21% Sauerstoff ist eine solche Grosse nicht mehr effizient versorgbar."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Funktion des Nagels (Knochenwachstum) bei der Tartaruga (Riesenschildkrote)?",
        answerA = "Keine besondere Funktion",
        answerB = "Erkennung des Alters durch Jahresringe am Panzer",
        answerC = "Wachstum des Panzers durch Scutes, die Wachstumsringe bilden",
        answerD = "Thermoregulation durch Wache",
        correctAnswer = 2,
        explanation = "Die Scutes (Hornplatten) des Schildkrotenpanzers wachsen in konzentrischen Ringen, ahnlich wie Baumringe. Diese Ringe spiegeln saisonales Wachstum wider und konnen zur groben Altersschatzung dienen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie funktioniert die Echolokation bei der kurzohrigen Schnarrschildkrote?",
        answerA = "Durch Ultraschallsignale wie Fledermause",
        answerB = "Schnarrschildkroten nutzen keine Echolokation",
        answerC = "Durch Infraschall unter der Horgrenze",
        answerD = "Durch Wasserschallwellen uber die Seitenlinie",
        correctAnswer = 1,
        explanation = "Schildkroten besitzen kein Echolokationssystem. Sie orientieren sich uber Gesichtssinn, Geruch und bei Meeresschildkroten uber Magnetfeldsinn. Echolokation ist ein Merkmal von Saugetieren (Fledermause, Wale) und einigen Vogeln.",
        difficulty = 2,
        funFact = "Meeresschildkroten navigieren mithilfe des Erdmagnetfelds uber tausende Kilometer zuruck zu dem Strand, an dem sie selbst geschlupft sind — ein noch nicht vollstandig verstandenes Phanomen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Mullerscheimikry und wie unterscheidet sie sich von Batesscher Mimikry?",
        answerA = "Muller: schadliche Arten ahmen unschadliche nach; Bates: umgekehrt",
        answerB = "Muller: mehrere gefahrliche Arten gleichen sich gegenseitig an; Bates: harmlose Art ahmt gefahrliche nach",
        answerC = "Muller: Anpassung an Umgebung; Bates: Anpassung an Artgenossen",
        answerD = "Kein Unterschied, beides ist dasselbe Phanomen",
        correctAnswer = 1,
        explanation = "Bei der Mullerschen Mimikry ahmen mehrere unabhangig gefahrliche oder giftige Arten einander an. Alle profitieren, weil Fressfeinde schneller lernen die gemeinsame Warntracht zu meiden. Bei Bates ahmt eine harmlose Art eine gefahrliche nach.",
        difficulty = 2,
        funFact = "Das klassische Mullerscheimikry-Beispiel sind heliconide Schmetterlinge in Sudamerika: Dutzende verschiedener giftiger Arten zeigen ahnliche orange-schwarze Muster."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie orientieren sich Zugvogel-Zugschmetterlinge wie der Monarchfalter auf ihrer Wanderung?",
        answerA = "Ausschliesslich nach Wind richtungen",
        answerB = "Durch eine Kombination aus Sonnenkompass und angeborenem Magnetsinnsystem",
        answerC = "Durch Pheromone fruher Generationen",
        answerD = "Durch Nachahmung anderer Individuen",
        correctAnswer = 1,
        explanation = "Monarchfalter nutzen einen Zeitkompensations-Sonnenkompass (Antennenuhr) und moglicherweise auch magnetische Sinnesorgane. Die Fahigkeit ist angeboren — eine Generation kehrt nie zur Geburtsstelle zuruck, aber findet die selben Winterquartiere.",
        difficulty = 2,
        funFact = "Monarchfalter wandern bis zu 4.500 km von Kanada nach Mexiko. Da keine einzelne Faltergeneration den gesamten Weg macht, muss die Navigationsinformation genetisch vererbt werden."
    ),

)
