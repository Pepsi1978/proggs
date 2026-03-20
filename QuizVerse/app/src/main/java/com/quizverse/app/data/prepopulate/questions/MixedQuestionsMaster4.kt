package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsMaster4(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) — Kunst-Expertenwissen ──

    // Frage 1 — Mona Lisa / Diebstahl
    Question(
        categoryId = 11,
        questionText = "Im Jahr 1911 wurde die Mona Lisa aus dem Louvre gestohlen. Der Tater Vincenzo Peruggia wurde erst 1913 gefasst. Wo versteckte er das Gemälde die ganze Zeit?",
        answerA = "In einem Doppelboden seines Koffers im Hotel",
        answerB = "In einer falschen Wandverkleidung seiner Pariser Wohnung",
        answerC = "In einem mit Holz ausgekleideten Koffer unter seinem Bett in Florenz",
        answerD = "Eingemauert im Keller der Galerie Uffizi",
        correctAnswer = 2,
        explanation = "Vincenzo Peruggia, ein ehemaliger Louvre-Mitarbeiter, hatte das Gemälde in einem speziell angefertigten, mit rotem Samt ausgekleideten Holzkoffer unter seinem Bett in seiner Florentiner Unterkunft versteckt. Er versuchte es einem Kunsthaendler namens Alfredo Geri zu verkaufen, der die Polizei alarmierte. Peruggia wurde verhaftet und zu einer milden Haftstrafe verurteilt, da viele Italiener ihn als Patrioten betrachteten, der ein 'gestohlenes' italienisches Erbe zurueckgeholt habe.",
        difficulty = 5,
        funFact = "Waehrend die Mona Lisa gestohlen war, pilgerten mehr Menschen in den Louvre, um den leeren Fleck an der Wand zu sehen, als sich das Gemaelde jemals zuvor besucher angezogen hatte."
    ),

    // Frage 2 — Vermeer / Jan van Eyck Technik
    Question(
        categoryId = 11,
        questionText = "Johannes Vermeer verwendete nachweislich eine optische Hilfstechnologie fuer seine Gemaelde. Welches Instrument wird von Kunsthistorikern wie David Hockney als Hilfsmittel fuer Vermeers aussergewoehnliche Detailtreue diskutiert?",
        answerA = "Camera Lucida mit Spiegeloptik",
        answerB = "Camera Obscura als Projektionshilfe",
        answerC = "Teleskop mit Spiegellinsen nach Galilei",
        answerD = "Pantograph zur proportionalen Vergroesserung",
        correctAnswer = 1,
        explanation = "Die 'Camera Obscura Hypothese' wurde besonders durch den Kuenstler David Hockney und den Physiker Charles Falco popularisiert (Hockney-Falco-These, 2001). Eine Camera Obscura projiziert das Bild der Aussenwelt durch eine kleine Oeffnung oder Linse auf eine Flaeche, wo es als Vorlage dienen kann. Vermeers aussergewoehnliche perspektivische Praezision, die Lichtbehandlung und der charakteristische Schaerfeverlauf deuten laut dieser Theorie auf ein optisches Hilfsmittel hin. Die Hypothese ist unter Kunsthistorikern jedoch nach wie vor umstritten.",
        difficulty = 5,
        funFact = "Vermeers Gemaelde 'Das Maedchen mit dem Perlenohrring' zeigt nach Analysen keine echte Perle — das Ohrstück reflektiert kein Licht wie eine Perle, sondern eher wie Glas oder emailliertes Metall."
    ),

    // Frage 3 — Beltracchi Kunstfaelschung
    Question(
        categoryId = 11,
        questionText = "Wolfgang Beltracchi flog 2010 als Kunstfaelscher auf, weil er in einem seiner gefaelschten Gemaelde (im Stil von Heinrich Campendonk) ein falsches Pigment verwendete. Welches Pigment verriets ihn?",
        answerA = "Preussischblau, das erst nach Campendonks Tod synthetisiert wurde",
        answerB = "Titanweiss, das zur Zeit von Campendonks noch nicht im Handel erhaeltlich war",
        answerC = "Phthaloblau, das erst in den 1950er-Jahren entwickelt wurde",
        answerD = "Zinkweiss, das laut Laborbefund aus modernem Industrieprozess stammte",
        correctAnswer = 1,
        explanation = "Wolfgang Beltracchi flog auf, weil eine Laboranalyse in seinem gefaelschten 'Rotes Bild mit Pferden' (angeblich von Heinrich Campendonk) das Pigment Titanweiss (Titandioxid) nachwies. Dieses Weiss wurde zwar um 1916 entwickelt, war aber zu Campendonks aktiver Schaffenszeit noch nicht als Kuenstlerfarbe im Handel erhaeltlich. Beltracchi und seine Frau Helene wurden 2011 wegen Betrugs verurteilt. Beltracchi erhielt sechs Jahre Haft, Helene fuenf Jahre auf Bewaehrung. Der Schaden wird auf 20-50 Millionen Euro geschaetzt.",
        difficulty = 5,
        funFact = "Beltracchi faelschte keine echten Originale — er erfand Werke, die nie existiert hatten, im Stil bekannter Meister. Er fabrizierte sogar gefaelschte Familienfotos, die seine Frau mit angeblichen Erbstuecken zeigten, um die Provenienz zu beglaubigen."
    ),

    // Frage 4 — Han van Meegeren
    Question(
        categoryId = 11,
        questionText = "Der hollaendische Faelscher Han van Meegeren verkaufte 1943 ein gefaelschtes Vermeer-Gemaelde an Hermann Goering. Wie konnte er nach dem Krieg beweisen, dass es eine Faelschung war und er kein Kollaborateur?",
        answerA = "Er legte das Originalrezept seiner selbst gemischten Farben vor",
        answerB = "Er malte vor Zeugen in seiner Gefaengniszelle ein weiteres Vermeer-Faelsemälde",
        answerC = "Er benannte seinen Auftraggeber als NS-Offizier, der das Gemaelde in Auftrag gab",
        answerD = "Sein Notar bestaetigte, dass das Bild niemals eine offizielle Provenienz hatte",
        correctAnswer = 1,
        explanation = "Nach dem Krieg wurde van Meegeren verhaftet, weil man glaubte, er habe ein echtes nationales Kulturgut (den Vermeer) an die Nazis verkauft. Um zu beweisen, dass es eine Faelschung war, malte er tatsaechlich vor Zeugen und Behoerden ein weiteres Gemälde im Vermeer-Stil: 'Jesus unter den Schriftgelehrten'. Dies ueberzeugte die Gerichte. Van Meegeren wurde schliesslich nur wegen Faelschung und Betrugs verurteilt und galt in der oeffentlichen Meinung als Held, der Goering zum Narren gehalten hatte. Er starb 1947 noch vor Antritt der Haftstrafe.",
        difficulty = 5,
        funFact = "Goering hatte fuer das gefaelschte Vermeer-Bild 137 andere Gemaelde aus seiner Sammlung eingetauscht — ein Handel, der van Meegeren nachtraeglich grosse Befriedigung bereitete."
    ),

    // Frage 5 — Botticelli Primavera
    Question(
        categoryId = 11,
        questionText = "In Botticellis 'Primavera' (ca. 1482) sind im Hintergrund mehr als 500 verschiedene Pflanzenarten dargestellt. Welche Familie wird durch den Orangenhain im Zentrum des Bildes symbolisiert?",
        answerA = "Die Familie Pazzi, Rivalen der Medici",
        answerB = "Die Familie Medici, da die Orange ihr Wappensymbol war",
        answerC = "Die Familie Strozzi, grosse Bankiersdynastie in Florenz",
        answerD = "Die Familie Sforza aus Mailand, Auftraggeber des Bildes",
        correctAnswer = 1,
        explanation = "Der Orangenbaum (mala medica = Medizinalapfel) war ein Wortspiel auf den Namen 'Medici' und deren Familiensymbol. Botticelli malte die Primavera im Auftrag oder Umfeld der Medici-Familie und platzierte Venus bewusst in einem Orangenhain als Verweis auf den Auftraggeber. Das Bild hing urspruenglich in der Villa von Lorenzo di Pierfrancesco de Medici. Botaniker haben in dem Gemaelde ueber 500 verschiedene Pflanzenarten identifiziert, darunter 190 Blumenarten — ein einzigartiges Naturdokument der Florentiner Renaissance.",
        difficulty = 5,
        funFact = "Botticelli wurde von neoplatonischen Philosophen wie Marsilio Ficino beeinflusst. Die drei Grazien im Bild stehen fuer Keuschheit, Schoenheit und Freude — und sollen sich in einem ewigen Kreistanz befinden, der das Geben und Empfangen von Gaben symbolisiert."
    ),

    // Frage 6 — Salvator Mundi Rekordpreis
    Question(
        categoryId = 11,
        questionText = "Leonardos 'Salvator Mundi' wurde 2017 fuer 450,3 Millionen Dollar versteigert — Weltrekord. Jahrzehntelang galt es als Kopie. Was liess die Fachwelt das Bild schliesslich Leonardo selbst zuschreiben?",
        answerA = "Ein Fingerabdruck unter dem Firnis, der mit anderen Leonardo-Werken uebereinstimmte",
        answerB = "Die Restaurierung enthullte Sfumato-Schichten und pentimenti (Korrekturen), die fuer Leonardo typisch sind",
        answerC = "Ein Roentgenbild zeigte den charakteristischen Linkshaender-Pinselstrich Leonardos",
        answerD = "Ein Testament eines Medici-Mitglieds nannte das Bild als Leonardo-Auftrag",
        correctAnswer = 1,
        explanation = "Bei der Restaurierung ab 2005 durch Dianne Dwyer Modestini wurden durch technische Analysen (Infrarotreflektographie, Roentgenuntersuchung) typische pentimenti (Reuestriche/Korrekturen) sichtbar, die zeigen, wie Leonardo waehrend des Malens die Position des Daumens der segnenden Hand veraenderte. Ausserdem wurde die charakteristische Sfumato-Technik und der Aufbau der Farbschichten als leonardesk eingestuft. Das Bild wurde 2011 in einer grossen Leonardo-Ausstellung in der National Gallery London als Originalwerk prasentiert. Allerdings bleibt die Zuschreibung unter einigen Kunsthistorikern weiterhin umstritten.",
        difficulty = 5,
        funFact = "Das 'Salvator Mundi' verschwand nach dem Auktionsrekord von der Oeffentlichkeit. Es sollte 2019 im Louvre Abu Dhabi ausgestellt werden, die Ausstellung wurde ohne Begruendung abgesagt. Sein aktueller Aufenthaltsort ist ungeklaert."
    ),

    // Frage 7 — Caravaggio Technik
    Question(
        categoryId = 11,
        questionText = "Caravaggio revolutionierte die Malerei mit dem Chiaroscuro-Prinzip, das er zur extremen Form des 'Tenebrismo' steigerte. Was ist das definierte Merkmal des Tenebrismo, das ihn von normalem Chiaroscuro unterscheidet?",
        answerA = "Der Maler arbeitet ausschliesslich mit schwarzer und weisser Farbe ohne Mitteltone",
        answerB = "Figuren tauchen aus einem nahezu vollstaendig schwarzen Hintergrund hervor, ohne Uebergangszonen",
        answerC = "Das Licht kommt immer von links unten, wie bei einer am Boden stehenden Kerze",
        answerD = "Alle Schatten werden als tiefblaue Lasurschichten ausgefuehrt, nicht als Schwarz",
        correctAnswer = 1,
        explanation = "Beim Tenebrismo (von ital. tenebroso = finster, dunkel) entstammt die Figur scheinbar einem absoluten, tiefen Dunkel ohne atmosphaerische Uebergaenge. Waehrend das klassische Chiaroscuro sanfte Uebergaenge zwischen Licht und Schatten verwendet, setzt Tenebrismo auf harte, fast theatralische Kontraste: Die beleuchteten Partien wirken wie mit einem Scheinwerfer aus der Dunkelheit herausgegriffen. Caravaggio verwendete diese Technik, um dramatische psychologische Spannung zu erzeugen. Nachwirkungen finden sich bei Rembrandt und den Caravaggisten wie Artemisia Gentileschi.",
        difficulty = 5,
        funFact = "Caravaggio war notorisch gewaltbereit — er toeete 1606 einen Mann bei einem Streit und floh aus Rom. Er starb 1610 unter unklaren Umstaenden mit 38 Jahren, moeglicherweise an einer Bleivergiftung durch seine Farben."
    ),

    // Frage 8 — Velazquez Las Meninas
    Question(
        categoryId = 11,
        questionText = "Diego Velazquez' 'Las Meninas' (1656) gilt als eines der raetselhaftesten Gemaelde der Welt. Was macht die Perspektivkonstruktion des Bildes so ungewoehnlich?",
        answerA = "Der Fluchtpunkt liegt ausserhalb des Bildrahmens, was zum Zeitpunkt technisch unmoeglich war",
        answerB = "Velazquez malte sich selbst malend, und der Spiegel zeigt das koenigliche Paar — also schaut der Betrachter vom Standpunkt des Koenigs aus",
        answerC = "Die Infanta Margarita ist tatsaechlich zweimal im Bild — einmal real und einmal als Spiegelung",
        answerD = "Der Raum ist absichtlich falsch konstruiert, um eine unmoeglich grosse Tiefe vorzutaeuschen",
        correctAnswer = 1,
        explanation = "Das Faszinierende an Las Meninas ist die Doppeldeutigkeit des Betrachterstandpunkts: Im Hintergrund haengt ein Spiegel, der das Koenigspaar Philipp IV. und Mariana von Oesterreich zeigt. Damit befindet sich der Betrachter des Bildes genau dort, wo das Koenigspaar steht — er blickt also vom selben Punkt aus wie die Koenige, die von Velazquez portraitiert werden. Gleichzeitig blickt Velazquez auf der Leinwand direkt aus dem Bild heraus. Diese Verschachtelung von Malenden, Betrachteten und Betrachtern inspirierte Jahrhunderte spaeter Michel Foucaults Analyse in 'Die Ordnung der Dinge' (1966).",
        difficulty = 5,
        funFact = "Velazquez integrierte in Las Meninas eine versteckte Standespolitik: Er malte sich selbst mit dem Kreuz des Ordens von Santiago auf der Brust — ein Orden, den er erst nach der Fertigstellung des Bildes erhielt. Koenig Philipp IV. soll das Kreuz persoenlich nachtraeglich aufgemalt haben."
    ),

    // Frage 9 — Rembrandt Selbstportrait
    Question(
        categoryId = 11,
        questionText = "Rembrandt van Rijn schuf mehr Selbstportrats als jeder andere Kuenstler seiner Zeit — rund 80 Gemaelde, Radierungen und Zeichnungen. Was war der Hauptgrund dafuer, laut kunsthistorischer Forschung?",
        answerA = "Er war zu arm, um Modelle zu bezahlen, und nutzte sich selbst als kostenlose Alternative",
        answerB = "Er wollte eine spirituelle Autobiographie schaffen, die sein Altern und inneres Leben dokumentiert",
        answerC = "Selbstportrats galten als Probieruebungen fuer neue Techniken ohne Qualitaetsanspruch",
        answerD = "Er hatte einen Pakt mit einem Sammler geschlossen, der ausschliesslich Selbstportrats sammelte",
        correctAnswer = 0,
        explanation = "Obwohl spirituelle und autobiographische Motive eine Rolle spielen moegen, ist die gaengigste kunsthistorische Erklaerung pragmatisch: Als junger, aufstrebender Kuenstler ohne Auftraege und Geld fuer Modelle nutzte Rembrandt sich selbst als guenstiges Uebungsmodell fuer Gesichtsausdruecke und Lichteffekte. Spaeter wurden Selbstportrats zu einem Mittel der Selbstvermarktung und -reflexion. Die Serie dokumentiert aussergewoehnlich den koerperlichen Verfall und die psychologische Reifung eines Malers ueber 40 Jahre hinweg.",
        difficulty = 5,
        funFact = "Rembrandts spaete Selbstportrats, gemalt nach dem Bankrott und dem Tod seiner geliebten Frau Saskia, gelten als einige der psychologisch eindringlichsten Bilder der Kunstgeschichte — ehrliche Dokumente des Alterns ohne Schmeichelei."
    ),

    // Frage 10 — Gustave Courbet Realismus
    Question(
        categoryId = 11,
        questionText = "Gustave Courbets Gemaelde 'Un enterrement a Ornans' (Begräbnis in Ornans, 1849-50) loeste einen Skandal aus. Was war der kunstpolitische Sprengstoff des Werkes?",
        answerA = "Es zeigte nackte Leichen auf dem Friedhof, was als Blasphemie galt",
        answerB = "Es stellte gewoehnliche Dorfbewohner in Lebensgroesse dar — ein Format, das ausschliesslich Helden und Adeligen vorbehalten war",
        answerC = "Es verspottete den Klerus durch karikaturhafte Darstellung der Priester",
        answerD = "Es wurde als politisches Manifest der Pariser Commune gedeutet und deshalb beschlagnahmt",
        correctAnswer = 1,
        explanation = "Das Skandalon des 'Begräbnisses in Ornans' war, dass Courbet gewoehnliche Bauern und Buerger aus seiner Heimatstadt im Masstab von Historienbildern (bis zu 3,10 x 6,60 Meter) malte. Lebensgrosse Darstellungen auf grossen Formaten waren der hohen Historienmalerei, mythologischen oder religiosen Szenen vorbehalten. Indem Courbet Dorfbewohner in diesem Format zeigte, demokratisierte er die Malerei und stellte das Wertesystem der Akademie fundamental in Frage. Das Werk gilt als Gruendungsdokument des Realismus.",
        difficulty = 5,
        funFact = "Courbet war so selbstbewusst, dass er 1855, als seine Werke von der Weltausstellung abgelehnt wurden, kurzerhand ein eigenes 'Pavillon du Realisme' gegenueber dem offiziellen Ausstellungsgebaude errichtete — eine der ersten Kuenstler-Gegengallerien der Geschichte."
    ),

    // Frage 11 — Edvard Munch Der Schrei
    Question(
        categoryId = 11,
        questionText = "Edvard Munch schuf 'Der Schrei' in vier Versionen. Er selbst beschrieb in seinem Tagebuch ein konkretes Erlebnis, das ihn zu dem Bild inspirierte. Was erlebte er an jenem Abend?",
        answerA = "Er sah in einem Albtraum, wie seine verstorbene Schwester ihn anlickte",
        answerB = "Er wanderte mit Freunden, blieb allein zurueck und sah den Himmel blutigrot leuchten — er fuehlte 'einen unendlichen Schrei durch die Natur'",
        answerC = "Er ueberstand knapp einen Sturm auf See und erlebte dabei eine Panikattacke",
        answerD = "Er wurde Zeuge einer psychiatrischen Einweisung und erschrak ueber den Schrei des Patienten",
        correctAnswer = 1,
        explanation = "In seinem Tagebucheintrag von 1892 beschrieb Munch: 'Ich ging mit Freunden die Strasse entlang — die Sonne sank — ploetzlich wurde der Himmel blutrot... ich blieb stehen, todmuede, und lehnte an einem Zaun — ueber dem blauschwarzen Fjord und der Stadt lag Blut und Feuer — meine Freunde gingen weiter, ich stand zitternd vor Angst — und ich fuehlte einen unendlichen Schrei durch die Natur.' Wissenschaftler haben versucht, das blutrote Wetterleuchten als Nachwirkung des Krakatau-Ausbruchs von 1883 zu erklaeren, der weltweit atmosphaerische Farbeffekte erzeugte.",
        difficulty = 5,
        funFact = "Eine der vier Versionen von 'Der Schrei' (die Pastellversion von 1895) wurde 2012 bei Sotheby's fuer 119,9 Millionen Dollar versteigert und ist damit eines der teuersten je versteigerten Kunstwerke."
    ),

    // Frage 12 — Jan van Eyck Arnolfini-Hochzeit
    Question(
        categoryId = 11,
        questionText = "Jan van Eycks 'Arnolfini-Hochzeit' (1434) enthält im Hintergrund einen konvexen Spiegel, der den Raum zeigt. Was macht dieser Spiegel kunsthistorisch so bedeutend?",
        answerA = "Er ist das erste Mal in der Kunstgeschichte, dass ein Kuenstler sein Spiegelbild malte, und zeigt van Eyck selbst",
        answerB = "Im Spiegel sind zwei weitere Personen zu sehen, und ueber dem Spiegel steht die Inschrift 'Johannes de eyck fuit hic' (Jan van Eyck war hier), was als frueheste Kuenstlersignatur mit Zeugencharakter gilt",
        answerC = "Der Spiegel ist mit zehn Medaillons umrahmt, die zehn Werke der Barmherzigkeit zeigen — ein verstecktes religioes-moralisches Programm",
        answerD = "Van Eyck verwendete fuer den Spiegel als Erster in der Malgeschichte die optisch korrekte Verzerrung eines konvexen Spiegels",
        correctAnswer = 1,
        explanation = "Jan van Eyck signierte das Bild auf ungewoehnliche Weise: Ueber dem Spiegel steht auf Latein 'Johannes de Eyck fuit hic 1434' — 'Jan van Eyck war hier'. Im konvexen Spiegel selbst sind zwei weitere Personen zu sehen, die in den Raum eintreten — eine davon wird als van Eyck selbst interpretiert. Damit fungiert die Signatur wie eine notarielle Bezeugung: Der Kuenstler war Zeuge der Szene. Gleichzeitig ist es eines der ersten bekannten Selbstportrats in der nordeuropaischen Malerei. Der konvex verzerrte Raumausblick im Spiegel ist malerisch aussergewoehnlich praezise.",
        difficulty = 5,
        funFact = "Die Frau im Bild, frueher als schwanger gedeutet, ist es nach heutigen Erkenntnissen nicht — die ausladende Silhouette entspricht der damaligen Modeschau mit hochgeraefftem Kleid. Der ausgestreckte Bauch war ein Schoenheitsideal der Spaetgotik."
    ),

    // Frage 13 — Impressionismus Entstehung
    Question(
        categoryId = 11,
        questionText = "Der Begriff 'Impressionismus' entstand als Spottname, abgeleitet von einem Gemaelde. Wie lautete der vollstaendige originale Titel dieses Werkes, und von wem stammt es?",
        answerA = "Impression, Sonnenuntergang — von Camille Pissarro",
        answerB = "Impression, soleil levant (Eindruck, aufgehende Sonne) — von Claude Monet",
        answerC = "Impression du soir (Abendstimmung) — von Alfred Sisley",
        answerD = "Impression de lumiere (Lichtimpression) — von Pierre-Auguste Renoir",
        correctAnswer = 1,
        explanation = "Claude Monets Gemaelde 'Impression, soleil levant' (1872) zeigte den Hafen von Le Havre im Morgenlicht. Als es 1874 in der ersten unabhaengigen Ausstellung der Gruppe gezeigt wurde, verspottete der Kritiker Louis Leroy in der Zeitschrift Le Charivari die Maler als 'Impressionisten' — eine Bezeichnung, die er von Monets Bildtitel ableitete. Die Kuenstler uebernahmen den Spitznamen daraufhin selbst. Das Bild befindet sich heute im Musee Marmottan Monet in Paris.",
        difficulty = 5,
        funFact = "Das originale 'Impression, soleil levant' wurde 1985 aus dem Musee Marmottan gestohlen und tauchte erst 1990 wieder auf. Es ist seither eines der meistbewachten Kunstwerke Frankreichs."
    ),

    // Frage 14 — Pablo Picasso Guernica
    Question(
        categoryId = 11,
        questionText = "Picassos 'Guernica' (1937) hing jahrzehntelang nicht in Spanien, sondern im Museum of Modern Art in New York. Wann kam es nach Spanien, und welche Bedingung hatte Picasso dafuer gestellt?",
        answerA = "1975 nach dem Tod Francos — Picasso hatte bestimmt, das Bild duorfe erst in ein republikanisches, demokratisches Spanien",
        answerB = "1981, erst nach dem Tod Picassos 1973 — er hatte testamentarisch festgelegt, das Bild erbe Spanien nach seiner demokratischen Wiedergeburt",
        answerC = "1955 als Leihgabe zur Weltausstellung, danach kehrte es nach New York zurueck",
        answerD = "1939 nach dem Ende des Bueergerkrieges, da Picasso das Bild als politisches Mahnmal konzipiert hatte",
        correctAnswer = 1,
        explanation = "Picasso stellte die Bedingung, dass 'Guernica' erst nach Wiederherstellung der demokratischen Freiheiten und der Republik in Spanien dorthin zurueckkehren durfte. Franco konnte das Bild nicht beanspruchen. Nach Picassos Tod 1973 und dem Ende der Franco-Diktatur (1975) wurde die spanische Demokratie etabliert. Das Bild kam schliesslich 1981 nach Spanien, wo es heute im Museo Reina Sofia in Madrid zu sehen ist. Die Spanische Verfassung von 1978 bereitete den Weg.",
        difficulty = 5,
        funFact = "Waehrend der Nazi-Besatzung in Paris soll ein Gestapo-Offizier Picasso gefragt haben, ob er Guernica gemacht habe. Picasso soll geantwortet haben: 'Nein, Sie.'"
    ),

    // Frage 15 — Surrealismus Dali
    Question(
        categoryId = 11,
        questionText = "Salvador Dalis 'Die Bestaendigkeit der Erinnerung' (1931) zeigt schmelzende Uhren. Dali beschrieb, woher er die Idee hatte. Welche banaele Alltagssituation inspirierte ihn zu dem surrealen Motiv?",
        answerA = "Er traumte von schmelzendem Gold und malte das Bild noch im Halbschlaf",
        answerB = "Er beobachtete einen schmelzenden Camembert-Kaese und dachte ueber die 'Weichheit von Zeit' nach",
        answerC = "Er sah eine Taschenuhren-Sammlung seines Vaters und fragte sich, ob Zeit sich verbiegen koenne",
        answerD = "Er las Einstein und wollte die Relativitaetstheorie als Bild darstellen",
        correctAnswer = 1,
        explanation = "Dali beschrieb die Entstehung des Bildes als spontane Eingebung: Er war allein in seinem Atelier, ass Camembert-Kaese und betrachtete den schmelzenden Kaese. Dieser Anblick liess ihn ueber die 'Weichheit' und Relativitaet der Zeit nachdenken. Er begann das kleine Bild (24 x 33 cm) sofort zu malen, und als seine Frau Gala zurueckkehrte, war es fertig. Das Bild ist heute eines der bekanntesten Werke des 20. Jahrhunderts und befindet sich im MoMA in New York.",
        difficulty = 5,
        funFact = "Dali nannte seinen Stil 'kritischen Paranoia-Methode' — er versuchte, Halluzinationen willentlich herbeizufuehren und sie praezise zu malen. Er sagte: 'Der einzige Unterschied zwischen mir und einem Verrueckten ist, dass ich nicht verrueckt bin.'"
    ),

    // Frage 16 — Georges Seurat Pointillismus
    Question(
        categoryId = 11,
        questionText = "Georges Seurat entwickelte den Pointillismus auf wissenschaftlicher Basis. Welche optische Theorie bildete die wissenschaftliche Grundlage, auf die er sich explizit berief?",
        answerA = "Newtons Farbenkreis-Theorie aus 'Opticks' (1704)",
        answerB = "Die Farbentheorie von Michel Chevreul ueber den Simultankontrast und optische Farbmischung",
        answerC = "Goethes Farbenlehre und die psychologische Wirkung von Komplementaerfarben",
        answerD = "Helmholtz' physiologische Theorie der drei Zapfentypen im Auge",
        correctAnswer = 1,
        explanation = "Georges Seurat stuetzte sich hauptsaechlich auf Michel-Eugene Chevreuls 'De la loi du contraste simultane des couleurs' (1839) und Ogden Roods 'Modern Chromatics' (1879). Chevreul hatte als Direktor der Gobelin-Tapisseriemanufaktur entdeckt, dass nebeneinander liegende Farben sich optisch beeinflussen (Simultankontrast). Seurat wandte dies an, indem er reine Farbpunkte nebeneinander setzte, die sich im Auge des Betrachters optisch mischen sollten — eine 'optische Mischung' statt einer Pigmentmischung auf der Palette.",
        difficulty = 5,
        funFact = "Seurat nannte seine Technik selbst 'Chromoluminarismus' oder 'Divisionismus', nicht 'Pointillismus'. Letzterer Begriff wurde von Kritikern eingebracht und von Seurat als abwertend empfunden."
    ),

    // Frage 17 — Michelangelo Sixtinische Kapelle
    Question(
        categoryId = 11,
        questionText = "Michelangelos Deckengemaelde der Sixtinischen Kapelle (1508–1512) zeigt die 'Erschaffung Adams'. In welchem Detail haben Wissenschaftler eine versteckte anatomische Abbildung des Gehirns entdeckt?",
        answerA = "In der wellenfoermigen Gewandung Gottes, die den Querschnitt des Kleinhirns abbildet",
        answerB = "In der Form des roten Mantels und des umgebenden Stoffs, der Gott umgibt — er hat die exakte Form eines menschlichen Gehirns im Querschnitt",
        answerC = "In der Wolkenformation links von Adam, die dem Limbischen System ahnelt",
        answerD = "In der Armhaltung Adams, die einen Nervenstrang darstellt",
        correctAnswer = 1,
        explanation = "1990 veroffentlichte der Neurochirurg Frank Meshberger in JAMA (Journal of the American Medical Association) die These, dass der rote Mantel und die Figurenanordnung hinter Gott in der 'Erschaffung Adams' exakt der Form eines menschlichen Gehirns im Sagittalschnitt entsprechen — inklusive Hirnstamm, Frontallappen und Sulcus. Da Michelangelo nachweislich heimlich Leichen sezierte und ein tiefes anatomisches Wissen hatte, gilt die These als plausibel. Er koennte damit die Idee verschluesselt haben, dass Gott dem Menschen Verstand/Intellekt schenkt.",
        difficulty = 5,
        funFact = "Michelangelo hasste das Auftragswerk zutiefst. Er war Bildhauer, kein Freskomaler. Er schrieb spater ein Spottgedicht ueber den Schmerz: 'Mein Bauch ist unter meinem Kinn gerutscht... Mein Gesicht macht eine gute Flaeche fuer Tauben.'"
    ),

    // Frage 18 — Vermeer Mädchen mit Perlenohrring
    Question(
        categoryId = 11,
        questionText = "Vermeers 'Das Maedchen mit dem Perlenohrring' (ca. 1665) wird oft als 'nordisches Mona Lisa' bezeichnet. Kunstwissenschaftlich gesehen gehoert es jedoch zu einer besonderen Bildgattung, die sich von normalen Portraets unterscheidet. Wie heisst diese Gattung?",
        answerA = "Troniekopf — eine Studie eines Gesichtsausdrucks oder Charaktertyps ohne Portraetanspruch",
        answerB = "Paragone — ein Wettbewerbsbild zum Vergleich zweier Techniken",
        answerC = "Historia — eine historisch-allegorische Figurendarstellung",
        answerD = "Conterfei — ein niedrierrangiges Kopienportrait eines hoechstrangigen Originals",
        correctAnswer = 0,
        explanation = "Das 'Maedchen mit dem Perlenohrring' ist kein Portraet einer bestimmten Person, sondern ein 'Tronie' — ein niederlaendischer Begriff fuer eine Studie eines Gesichtsausdrucks, Charaktertyps oder einer Fantasiefigur. Tronies wurden in den Niederlanden des 17. Jahrhunderts als eigenstaendige Kunstform geschaetzt, ohne Portraetanspruch. Das Modell ist unbekannt und war vermutlich nicht Vermeers Tochter (obwohl das die bekannteste populaere These ist). Der eigentliche Perlenohrring ist laut Materialanalyse moeglicherweise kein echter Perlenohrring, sondern Glas oder Emaille.",
        difficulty = 5,
        funFact = "Das Bild hatte jahrhundertelang einen anderen Titel: Es hiess 'Mädchen mit einem Turban'. Der heutige Titel 'Mädchen mit dem Perlenohrring' wurde erst im 20. Jahrhundert etabliert."
    ),

    // Frage 19 — Expressionismus Bruecke
    Question(
        categoryId = 11,
        questionText = "Die Kuenstlergruppe 'Die Bruecke' wurde 1905 in Dresden gegruendet. Einer der Gruender verfasste ein Manifest, das auf einer Holzplatte gedruckt wurde. Wer war dieser Gruender, und was forderte das Manifest?",
        answerA = "Ernst Ludwig Kirchner — er forderte eine Bruecke zwischen der deutschen und der franzoesischen Kunst",
        answerB = "Ernst Ludwig Kirchner — er rief alle Jugendlichen auf, sich frei von alten Kraeften zu machen und ein neues, ehrliches Leben und Schaffen zu erkampfen",
        answerC = "Karl Schmidt-Rottluff — er forderte die Rueckkehr zur reinen gotischen Formsprache als nationalem Erbe",
        answerD = "Erich Heckel — er verlangte die voellige Abkehr von Ausstellungen und kommerzieller Kunst",
        correctAnswer = 1,
        explanation = "Ernst Ludwig Kirchner schrieb das Gruendungsmanifest der 'Bruecke', das 1906 auf einer Holzplatte gedruckt wurde. Es lautet in Auszuegen: 'Mit dem Glauben an Entwicklung, an eine neue Generation der Schaffenden wie der Geniessenden rufen wir alle Jugend zusammen, und als Jugend, die die Zukunft traegt, wollen wir uns Arm- und Lebensfreiheit verschaffen gegenueber den wohlgesessenen aelteren Kraeften.' Die Formulierung 'Bruecke' sollte die Verbindung zwischen Vergangenheit und Zukunft symbolisieren, nicht zu einer bestimmten Kunstrichtung oder Nation.",
        difficulty = 5,
        funFact = "Die Bruecke-Kuenstler lebten und arbeiteten zeitweise in bewusster Gegenkultur: gemeinsame Ateliers, Aktmalerei mit Laiinnen (Vivi), gemeinsame Ausstellungen. Kirchner versah spaeter Bilder seiner Mitgruender mit falschen frueheren Datierungen, um seinen Vorrang zu sichern — was zu einem Eklat fuehrte."
    ),

    // Frage 20 — Abstrakte Kunst Kandinsky
    Question(
        categoryId = 11,
        questionText = "Wassily Kandinsky gilt als Vater der abstrakten Kunst. In welchem Jahr und unter welchen Umstaenden malte er angeblich sein erstes rein abstraktes Bild?",
        answerA = "1910 — er erkannte sein eigenes Gemaelde nicht, als es kopfueber auf einer Staffelei stand, und war von den Farb- und Formwirkungen fasziniert",
        answerB = "1908 — er entschied sich nach einem Besuch bei den franzoesischen Fauvisten, alle Gegenstande aus seinen Bildern zu verbannen",
        answerC = "1913 — nach einer Synasthesie-Erfahrung beim Hoeren einer Wagner-Oper begann er, Musik in Formen zu uebertragen",
        answerD = "1905 — er sah erstmals in Paris Monets 'Heuhaufen' und beschloss, Gegenstandlichkeit aufzugeben",
        correctAnswer = 0,
        explanation = "Kandinsky beschrieb selbst, wie er 1910 ein eigenes Gemaelde zunaechst nicht erkannte, weil es seitlich auf der Staffelei lehnte. Er sah nur Farben und Formen — und war begeistert, wie diese allein ohne erkennbaren Gegenstand wirkten. Diese Erfahrung gilt als Schluessel-Erlebnis fuer seine Hinwendung zur reinen Abstraktion. Das 'Erste abstrakte Aquarell' von 1910 (dessen genaue Datierung allerdings bis heute diskutiert wird) gilt als eines der Schluesseldokumente der Moderne. Kandinsky verband Abstraktion mit spirituellen und synasthetischen Ideen — er wollte Musik sichtbar machen.",
        difficulty = 5,
        funFact = "Kandinsky war Synasthetiker: Er hörte Farben und sah Musik. Gelb klang fuer ihn wie eine Trompete, Blau wie ein Cello. Diese Erfahrung pragte sein gesamtes Werk."
    ),

    // Frage 21 — Gustav Klimt und die Wiener Secession
    Question(
        categoryId = 11,
        questionText = "Gustav Klimts 'Judith I' (1901) galt lange als Portraet von Adele Bloch-Bauer. Welche kunsthistorisch bedeutsame Fehlidentifikation haengt mit diesem Bild zusammen?",
        answerA = "Das Bild wurde jahrzehntelang als 'Salome' katalogisiert, weil man die Bibelfigur verwechselte",
        answerB = "Klimt wurde lange vorgeworfen, er habe Adele Bloch-Bauer als juedische Verraeterin (Judith) dargestellt, obwohl er sie moeglicherweise als heldenhafte Befreierin meinte",
        answerC = "Das Bild wurde als Portraet von Klimts Schwester Sophie fehlidentifiziert und deshalb aus einer Retrospektive entfernt",
        answerD = "Man hielt es fuer ein Selbstportrait Klimts in Frauenkleidern, was einen Skandal ausloeste",
        correctAnswer = 0,
        explanation = "Klimts 'Judith I' wurde jahrzehntelang als 'Salome' katalogisiert und ausgestellt, obwohl Klimt selbst den Titel 'Judith' auf dem Rahmen angab. Salome liess Johannes den Taeufer koepfen, Judith toetete den assyrischen Feldherrn Holofernes. Die Verwechslung entstand, weil man annahm, eine femme fatale mit abgeschlagenem Kopf muesse Salome sein. Erst spaeter wurde die korrekte Identifikation als Judith allgemein anerkannt. Das Original befindet sich im Oesterreichischen Belvedere in Wien.",
        difficulty = 5,
        funFact = "Klimts 'Portraet der Adele Bloch-Bauer I' (das sogenannte 'Goldene Adele') wurde 2006 nach einem langen Restitutionsstreit an die Erbin Maria Altmann zurueckgegeben und kurz darauf fuer 135 Millionen Dollar an Ronald Lauder verkauft — damals der hoechste Preis fuer ein Gemaelde."
    ),

    // Frage 22 — Japanischer Einfluss Ukiyo-e
    Question(
        categoryId = 11,
        questionText = "Der japanische Farbholzschnitt (Ukiyo-e) beeinflusste massgeblich den europaeischen Impressionismus und Post-Impressionismus. Welchen Begriff praegte man fuer diese Begeisterung europaeischer Kuenstler fuer japanische Kunst?",
        answerA = "Chinoiserie — die Begeisterung fuer alles Ostasiatische ab dem 18. Jahrhundert",
        answerB = "Japonismus (Japonisme) — der spezifische Einfluss japanischer Kunst auf europaeische Kuenstler ab den 1860ern",
        answerC = "Orientalisme — die Sehnsucht nach dem fernen, exotischen Osten",
        answerD = "Exotisme — der allgemeine Trend zu aussereuropaeischen Motiven in der Malerei",
        correctAnswer = 1,
        explanation = "Der Begriff 'Japonisme' (Japonismus) wurde vom franzoesischen Kritiker Philippe Burty 1872 gepraegte. Er beschreibt den tiefgreifenden Einfluss japanischer Ukiyo-e-Holzschnitte (vor allem von Hiroshige und Hokusai) auf europaische Kuenstler ab den 1860er Jahren, nachdem Japan sich 1854 dem Welthandel oeffnete. Van Gogh kopierte Hiroshige-Holzschnitte direkt in Oel. Monet sammelte ueber 200 japanische Holzschnitte, die noch heute in seinem Haus in Giverny haengen. Toulouse-Lautrec uebernahm die Flaechen-Komposition und schwarze Konturen.",
        difficulty = 5,
        funFact = "Die Geschichte, dass Japanische Holzschnitte als Verpackungsmaterial fuer Porzellan nach Europa kamen und so entdeckt wurden, ist zwar schoen, aber historisch nicht belegt. Die Holzschnitte wurden gezielt gehandelt und gesammelt."
    ),

    // Frage 23 — Banksy Identitaet
    Question(
        categoryId = 11,
        questionText = "2018 wurde Banksys 'Girl with Balloon' sofort nach der Versteigerung bei Sotheby's fuer 1,04 Millionen Pfund automatisch durch einen im Rahmen versteckten Reisswolf halbwegs zerkleinert. Wie heisst das Werk seitdem?",
        answerA = "Shredded Illusion",
        answerB = "Love is in the Bin (Liebe ist im Muell)",
        answerC = "Self-Destructed Desire",
        answerD = "The End of Art",
        correctAnswer = 1,
        explanation = "Nachdem das Bild 'Girl with Balloon' in der Auktion versteigert wurde und der Schredder-Mechanismus im Rahmen teilweise ausloeste (das Bild wurde nicht vollstaendig zerkleinert, sondern nur zur Haelfte durchgeschreddert), benannte Banksy das entstandene Werk in 'Love is in the Bin' um. Das teilweise zerkleinerte Werk wurde 2021 erneut bei Sotheby's versteigert — diesmal fuer 18,58 Millionen Pfund, mehr als das Achtzehnfache des urspruenglichen Preises. Banksy hatte den Schredder heimlich bei Sotheby's eingebaut.",
        difficulty = 5,
        funFact = "Sotheby's-Mitarbeiterin Lydia Fenet beschrieb, wie sie nach dem Schredder-Vorfall den Hammer niederlegte und rief: 'I think we just made history.' Die Kaeuferinn entschied sich, das Werk trotzdem zu behalten."
    ),

    // Frage 24 — Mark Rothko Farbe und Emotion
    Question(
        categoryId = 11,
        questionText = "Mark Rothko erhielt 1958 einen grossen Auftrag fuer das neue Four Seasons Restaurant in New York. Er arbeitete monatelang daran, lehnte die Arbeit aber schliesslich ab und gab das Honorar zurueck. Was war sein Begruendung?",
        answerA = "Er wollte nicht, dass seine Bilder neben Speisekarten und Weinglaesern haengen — er wollte Bilder schaffen, die Menschen zum Weinen bringen, nicht zum Essen",
        answerB = "Das Restaurant hatte die vereinbarten Dimensionen der Wandflaechen veraendert, was seine Kompositionen unmoeglich machte",
        answerC = "Er erfuhr, dass das Restaurant einem Investorengruppe gehoerte, die er politisch ablehnte",
        answerD = "Ein Besuch im fertigen Restaurant zeigte ihm, dass die Beleuchtung seine Farbwirkungen zerstoerte",
        correctAnswer = 0,
        explanation = "Rothko besuchte das Four Seasons Restaurant selbst zum Essen und beschloss danach, den Auftrag abzubrechen. Er sagte spaeter, sein Ziel sei es, 'die Leute, die in diesem Raum essen, zu ruinieren'. Er wollte, dass seine Bilder bedrohlich und einschliessend wirken, dass sie den Betrachter in sich einsperren. Als er realisierte, dass die wohlhabenden Gaeste einfach neben seinen Bildern ihre Speisen geniessen wuerden, gab er das Geld zurueck. Die Seagram-Bilder gingen spaeter an die Tate Modern in London.",
        difficulty = 5,
        funFact = "Rothko soll gesagt haben: 'Ich male grosse Bilder, weil ich intim sein will. Ein kleines Bild staelst du nicht ab, ein grosses tritt in deinen Raum und ueberwaltigt dich.'"
    ),

    // Frage 25 — Leonardo Sfumato
    Question(
        categoryId = 11,
        questionText = "Leonardo da Vincis 'Sfumato'-Technik erzeugt weiche Uebergaenge ohne scharfe Konturen. Welche besondere technische Methode verwendete Leonardo dabei, die erst durch moderne Infrarotreflektografie nachgewiesen wurde?",
        answerA = "Er verwendete eine Mischung aus Bleiweiss und Oel, die er mit den Fingern statt mit Pinseln auftrug",
        answerB = "Er malte mit dem Finger direkt auf die Leinwand, ohne Pinsel, was durch Fingerabdruckanalyse bestaetigt wurde",
        answerC = "Er benutzte Schleier aus gespannter Gaze als Trennschichten zwischen Farblagen",
        answerD = "Er mischte Glasstaub in seine Farben, um lichtbrechende Mikrotexturen zu erzeugen",
        correctAnswer = 1,
        explanation = "Wissenschaftliche Analysen, insbesondere durch das Centre de recherche et de restauration des musees de France, haben Fingerabdruecke in den feinen Lasurschichten der Mona Lisa und anderer Leonardo-Werke gefunden. Leonardo trug die duennsten Farbschichten (teilweise wenige Mikrometer dick) mit den Fingern auf, um die feinsten Uebergaenge zu erzielen. Diese Methode ist einzigartig und erklaert die besondere Tiefenwirkung des Sfumato. Die einzelnen Farbschichten sind so duenn, dass sie mit normalen Untersuchungsmethoden nicht sichtbar waren.",
        difficulty = 5,
        funFact = "Die Mona Lisa besteht aus bis zu 40 transparenten Lasurschichten, von denen einige nur 2 Mikrometer duenn sind — duenner als ein menschliches Haar. Das Auftragen dauerte vermutlich mehrere Jahre."
    ),

    // Frage 26 — Auktionsrekord Frauenkuenstlerin
    Question(
        categoryId = 11,
        questionText = "Welche Kuenstlerin haelt den Auktionsrekord fuer das teuerste je versteigerte Werk einer Frau, und welches Werk ist es?",
        answerA = "Frida Kahlo mit 'Dos desnudos en el bosque' (2016, 8 Mio. Dollar)",
        answerB = "Georgia O'Keeffe mit 'Jimson Weed/White Flower No. 1' (2014, 44,4 Mio. Dollar)",
        answerC = "Lee Krasner mit 'The Eye is the First Circle' (2019, 11 Mio. Dollar)",
        answerD = "Berthe Morisot mit 'Apres le dejeuner' (2013, 10,9 Mio. Dollar)",
        correctAnswer = 1,
        explanation = "Georgia O'Keeffes 'Jimson Weed/White Flower No. 1' (1932) wurde im November 2014 bei Sotheby's fuer 44,4 Millionen Dollar versteigert — das bis dahin teuerste jemals versteigerte Werk einer Kuenstlerin und ein Rekord, der den bisherigen Hoechstpreis fuer ein Werk einer Frau um das Vierfache uebertraf. Das Gemaelde zeigt eine extrem vergrosserte weisse Bluete der Stechapfelpflanze (Jimsonweed/Datura), typisch fuer O'Keeffes serielle Blumenstudien, die oft als Vulva-Symbolik gedeutet werden — was O'Keeffe selbst zeitlebens ablehnte.",
        difficulty = 5,
        funFact = "Georgia O'Keeffe lebte die letzten Jahrzehnte ihres Lebens in der Wueste von New Mexico in Ghost Ranch und starb 1986 im Alter von 98 Jahren. Sie war eine der wenigen Kuenstlerinnen ihrer Generation, die zu Lebzeiten weltweite Anerkennung erlangte."
    ),

    // Frage 27 — Duchamps Readymade
    Question(
        categoryId = 11,
        questionText = "Marcel Duchamps 'Fountain' (1917) — ein umgedrehtes Urinal mit der Signatur 'R. Mutt' — gilt als wichtigstes Kunstwerk des 20. Jahrhunderts laut einer 2004er Umfrage unter 500 Kunstexperten. Was geschah mit dem Original?",
        answerA = "Es wurde von der Ausstellungskommission sofort vernichtet, bevor es gezeigt werden konnte",
        answerB = "Es verschwand kurz nach der Einreichung — vermutlich weggeworfen oder zerstoert; alle heutigen Exemplare sind autorisierte Repliken",
        answerC = "Es befindet sich heute im Philadelphia Museum of Art, wo auch Duchamps Hauptsammlung liegt",
        answerD = "Es wurde 1918 von der New Yorker Gesundheitsbehoerde als Hygienerisiko beschlagnahmt",
        correctAnswer = 1,
        explanation = "Das Original von 'Fountain' ist verloren. Duchamp reichte das Urinal 1917 bei der Society of Independent Artists ein, die es (trotz der Regel, alle eingereichten Werke auszustellen) ablehnte. Es wurde fotografiert (von Alfred Stieglitz), danach verschwand es spurlos — vermutlich weggeworfen. Alle heute in Museen ausgestellten Versionen (u.a. in der Tate Modern, im Pompidou und im Philadelphia Museum) sind autorisierte Repliken, die Duchamp ab 1950 anfertigen liess. Das Kunstwerk existiert daher vor allem als Fotografie und Konzept.",
        difficulty = 5,
        funFact = "Die Frage, wer 'Fountain' wirklich einsendete, ist bis heute ungeklaert. Manche Kunsthistoriker, darunter Irene Gammel, vermuten, dass die Dadaistin Elsa von Freytag-Loringhoven die eigentliche Urheberin war und Duchamp die Idee uebernahm."
    ),

    // Frage 28 — Frida Kahlo Schmerz und Werk
    Question(
        categoryId = 11,
        questionText = "Frida Kahlo malte die meisten ihrer Werke in einer besonderen Situation, die durch einen schweren Unfall 1925 bedingt war. Welche technische Besonderheit ihrer Maltechnik entstand direkt aus ihrer koerperlichen Einschraenkung?",
        answerA = "Sie malte ausschliesslich mit der linken Hand, nachdem ihr rechter Arm teilweise gelahmt war",
        answerB = "Ihr Bett hatte einen speziellen Aufsatz mit Spiegel und einer befestigten Leinwand, sodass sie liegend malen konnte — was die vielen Selbstportrats erklaert",
        answerC = "Sie malte fast ausschliesslich im Kleinformat, weil sie nicht stehen konnte und grosse Formate unmoeglich waren",
        answerD = "Sie benutzte eine Mundhalterung fuer den Pinsel, weil ihre Haende durch die Operationen zu stark geschaedigt waren",
        correctAnswer = 1,
        explanation = "Nach dem Busunfall 1925 verbrachte Kahlo monatelang immobil im Bett. Ihre Mutter liess eigens ein speziell angefertigtes Bett mit einem Baldachin-Aufsatz bauen, an dem eine Leinwand befestigt werden konnte, und liess einen Spiegel in den Himmel des Baldachins einbauen. So konnte Kahlo liegend malen und sich selbst im Spiegel sehen — das erklaert die aussergewoehnliche Haeufung von Selbstportrats in ihrem Werk. Kahlo selbst sagte: 'Ich male mich selbst, weil ich so viel Zeit allein verbringe und weil ich das Subjekt bin, das ich am besten kenne.'",
        difficulty = 5,
        funFact = "Kahlo unterzog sich in ihrem Leben mehr als 35 Operationen als Folge des Unfalls. Noch in ihrem Todesjahr 1954 stellte sie trotz ihrer Erkrankung aus — und erschien in einer Trage zu ihrer letzten Ausstellung."
    ),

    // Frage 29 — Hieronymus Bosch
    Question(
        categoryId = 11,
        questionText = "Hieronymus Boschs Triptychon 'Der Garten der lueste' (ca. 1490-1510) enthält auf dem Innendeckel des Musiktisches eine Notenschrift. Was ist das Aussergewoehnliche daran?",
        answerA = "Es ist die erste bekannte Musiknotation auf einem Gemaelde ueberhaupt",
        answerB = "Musiker haben die Noten transkribiert und tatsaechlich gespielt — die Melodie ist hoerbar und wurde im 21. Jahrhundert aufgenommen",
        answerC = "Die Noten sind rueckwaerts geschrieben und koennen nur in einem Spiegel gelesen werden",
        answerD = "Es handelt sich um eine versteckte Geheimschrift der Bruderschaft vom Freien Geist, der Bosch angehoerete",
        correctAnswer = 1,
        explanation = "Die auf dem Hintern einer Figur im Gemälde eingravierten Noten wurden 2014 von der Oklahomaer Studentin Amelia Hamrick transkribiert und als Choralgesang aufgenommen. Das Stueck, das sie 'Butt Music' nannte, klingt wie ein mittelalterlicher Choral. Das Video verbreitete sich viral. Obwohl die Interpretation diskutiert wird (die Lesart als Noten ist nicht unumstritten), ist das Ergebnis ein hoerbares Musikstueck aus einem Bild des 15./16. Jahrhunderts. Das Triptychon befindet sich im Museo del Prado in Madrid.",
        difficulty = 5,
        funFact = "Boschs Identitaet und Biographie sind weitgehend unbekannt. Sein echter Name war Jheronimus van Aken — 'Bosch' war sein Kuenstlername nach seiner Heimatstadt 's-Hertogenbosch. Fast nichts ist ueber sein Leben dokumentiert."
    ),

    // Frage 30 — Rembrandt Nachtwache
    Question(
        categoryId = 11,
        questionText = "Rembrandts 'Nachtwache' (1642) wurde im Laufe der Geschichte mehrfach beschaedigt. Welche irreversible Veraenderung wurde vorgenommen, die das Originalbild dauerhaft verkleinerte?",
        answerA = "Das Bild wurde nach einem Brand 1723 beschnitten, um beschaedigte Raender zu entfernen",
        answerB = "Um das Bild durch eine zu kleine Tuer ins Amsterdamer Rathaus zu bringen, wurden rund 60 cm vom linken Rand abgeschnitten",
        answerC = "Ein Restaurator entfernte 1889 eine grosse ueberstrichene Flaeche, die er irrtuemlicherweise als spaetere Ergaenzung identifizierte",
        answerD = "Das Bild wurde 1945 fuer den Transport in einen Bunker gerollt, wobei die Raender abgerissen wurden",
        correctAnswer = 1,
        explanation = "Um 1715 wurde das Bild abgeschnitten, damit es durch eine zu kleine Tuer im Amsterdamer Rathaus passte. Man kuerzte den linken Rand um etwa 60 cm und den oberen Rand leicht. Dabei gingen zwei vollstaendige Figuren links vom Hauptmotiv verloren. Eine Kopie des 17. Jahrhunderts (von Gerrit Lundens) zeigt die urspruengliche Version und gibt Aufschluss ueber das, was fehlt. Heute befindet sich das Bild im Rijksmuseum Amsterdam. Eine digitale Rekonstruktion des vollstaendigen Bildes wurde 2021 der Oeffentlichkeit praesentiert.",
        difficulty = 5,
        funFact = "Die 'Nachtwache' wurde dreimal Ziel von Vandalismusattacken: 1911 mit einem Messer, 1975 erneut mit einem Messer (12 Schnitte), und 1990 mit Saeureattacke. Das Bild gilt als eines der meistangegriffenen Kunstwerke der Welt."
    ),

    // Frage 31 — Impressionismus Ausstellung
    Question(
        categoryId = 11,
        questionText = "Die erste Impressionisten-Ausstellung 1874 wurde von den Teilnehmern bewusst als Alternative zum offiziellen 'Salon de Paris' organisiert. In welchen Raeumen fand sie statt?",
        answerA = "In einem leer stehenden Herrenhaus im Marais-Viertel, das der Maler Renoir anmietete",
        answerB = "Im Atelier des Fotografen Nadar im Boulevard des Capucines",
        answerC = "Im Jardin des Tuileries in einem provisorischen Holzpavillon",
        answerD = "Im Grand Palais in den Nebenraeumen, die fuer abgelehnte Einreichungen reserviert waren",
        correctAnswer = 1,
        explanation = "Die erste Ausstellung der 'Societe anonyme cooperative des artistes peintres, sculpteurs, graveurs' (so ihr offizieller Name, erst spaeter Impressionisten) fand vom 15. April bis 15. Mai 1874 im Atelier des Fotografen Gaspard-Felix Tournachon, genannt Nadar, im Boulevard des Capucines 35 statt. Nadar stellte seine Raeume kostenlos zur Verfuegung. 30 Kuenstler nahmen teil, darunter Monet, Renoir, Degas, Pissarro, Sisley und Berthe Morisot. Die Ausstellung wurde von der Kritik verspottet, jedoch von ca. 3.500 Besuchern gesehen.",
        difficulty = 5,
        funFact = "Edgar Degas hasste den Begriff 'Impressionisten' und wollte, dass die Gruppe sich 'Independants' (Unabhaengige) nannte. Er bestand darauf, dass Degas kein Impressionist sei — obwohl sein Name untrennbar mit der Bewegung verbunden ist."
    ),

    // Frage 32 — Uffizien Angriff
    Question(
        categoryId = 11,
        questionText = "Die Uffizien in Florenz wurden 1993 durch einen Bombenanschlag schwer beschaedigt. Wer steckte dahinter, und welches Gemaelde des Museums wurde dabei unwiederbringlich zerstoert?",
        answerA = "Die Rote Brigaden zerstoerten einen kleinen Rubens als Warnsignal, das Original blieb erhalten",
        answerB = "Die Cosa Nostra (Mafia) zerstörte unter anderem Bartholomäus Manfredis 'Konzert', das vollständig verloren ging",
        answerC = "Ein Einzeltaeter aus politischen Motiven sprengte den Eingang, wobei Grafiken aus dem 16. Jahrhundert verbrannten",
        answerD = "Die Bombenattacke war ein Anschlag auf den damaligen Kulturminister, der Bilder als 'Geiseln' nutzte",
        correctAnswer = 1,
        explanation = "Am 27. Mai 1993 detonierte eine Autobombe der Sicilianischen Mafia (Cosa Nostra) neben dem Westkorridor der Uffizien. Ziel war laut Ermittlungen die Destabilisierung des Staates. Fuenf Menschen kamen ums Leben. Mehrere Gemaelde wurden zerstoert, darunter Werke von Bartolommeo Manfredi. Grosse Meisterwerke wie Botticellis Gemaelde blieben weitgehend unverletzt, aber Dutzende kleinerer Werke wurden beschaedigt oder vernichtet. Der Anschlag war Teil einer Mafia-Anschlagsserie, die auch Mailand und Rom traf.",
        difficulty = 5,
        funFact = "Die Mafia waehlte Kulturstaetten bewusst als Ziele, um maximale Aufmerksamkeit zu erzeugen und den Staat unter Druck zu setzen — eine Strategie, die als 'Strategie der Spannung' bezeichnet wird."
    ),

    // Frage 33 — Jackson Pollock Drip Painting
    Question(
        categoryId = 11,
        questionText = "Jackson Pollocks Drip-Paintings wurden wissenschaftlich auf fraktale Muster untersucht. Zu welchem Ergebnis kam der Physiker Richard Taylor bei seiner Analyse?",
        answerA = "Pollocks Bilder enthalten zufaellige Muster, die keine Struktur aufweisen — was ihre hypnotische Wirkung erklaert",
        answerB = "Die Muster entsprechen Fraktaldimensionen, die in der Natur vorkommen, und aehnel physischen Erscheinungen wie Wolken oder Kuesten",
        answerC = "Die Farbverteilung folgt einem Fibonacci-Muster, das Pollock unbewusst in seine Technik integrierte",
        answerD = "Die Analysen zeigten, dass jedes Bild exakt den gleichen Entropie-Wert hat, was auf eine versteckte Systematik hinweist",
        correctAnswer = 1,
        explanation = "Der Physiker Richard Taylor (University of Oregon) publizierte 1999 in 'Nature', dass Pollocks Drip-Paintings fraktale Muster enthalten, deren Dimension (D-Wert) mit der Zeit der Entstehung zunahm: von D=1,45 (1943) bis D=1,7 (1952). Diese Fraktaldimensionen entsprechen jenen, die in natuerlichen Systemen wie Kuesten, Wolken und Baumkronen vorkommen. Taylor argumentierte, Pollock habe intuitiv Muster erzeugt, die der Natur entsprechen. Die Studie ist einflussreich, aber auch kontrovers — andere Wissenschaftler konnten die Ergebnisse nicht immer replizieren.",
        difficulty = 5,
        funFact = "Taylor entwickelte auf Basis seiner Forschung auch einen 'Pollock-Detektor', der echte Pollocks von Faelschungen unterscheiden soll — mit allerdings umstrittener Zuverlaessigkeit."
    ),

    // Frage 34 — Hieronymus Bosch Auftraggeber
    Question(
        categoryId = 11,
        questionText = "Wer war der Auftraggeber von Boschs 'Garten der Lüste', und was weiss man ueber den urspruenglichen Verwendungszweck des Triptychons?",
        answerA = "Philipp II. von Spanien kaufte es 1591 als erstes und liess es in seinem Schlafzimmer im Escorial aufhaengen",
        answerB = "Enguerrand von Nassauzu liess es fuer seinen Palast in Bruessel anfertigen; es war moeglicherweise ein Hochzeitsgeschenk oder Wohnzimmerdekor, nicht fuer die Kirche",
        answerC = "Die Bruderschaft vom Freien Geist gab es in Auftrag als geheimes Andachtsbild fuer ihre Treffen",
        answerD = "Es war ein Auftrag der Medici fuer die Villa Medici in Florenz, wurde aber nie geliefert",
        correctAnswer = 1,
        explanation = "Der Garten der Lueaste befand sich urspruenglich im Besitz von Enguerrand (Heinrich III.) von Nassau, einem hochrangigen niederlaendischen Adligen. Er hing in dessen Palais in Bruessel. Das Triptychon wurde vermutlich um 1505-1515 fertiggestellt. Da es im Haus eines weltlichen Herren und nicht in einer Kirche hing, nehmen Historiker an, dass es als Gesprächsstueck oder zur Unterhaltung (und vielleicht Warnung) diente, nicht als Andachtsbild. Philipp II. von Spanien erwarb es spater, und es kam in den Escorial, wo es heute noch im Prado-Depot liegt.",
        difficulty = 5,
        funFact = "Die genaue Bedeutung des 'Gartens der Lueaste' ist bis heute ungeklaert. Kunsthistoriker streiten, ob es eine moralische Warnung vor Suende, ein alchemistisches Werk oder die Vision eines utopischen Paradieses ist."
    ),

    // Frage 35 — Mondrian De Stijl
    Question(
        categoryId = 11,
        questionText = "Piet Mondrians charakteristische Bilder zeigen horizontale und vertikale Linien mit roten, gelben, blauen Feldern und Weiss/Schwarz. Wie nannte Mondrian seine eigene Kunsttheorie, und was bedeutete sie ihm philosophisch?",
        answerA = "Konstruktivismus — die reine Konstruktion der Welt aus Grundformen",
        answerB = "Neoplastizismus (De Stijl) — die Reduktion auf das Universale und Reine, das hinter den sichtbaren Erscheinungen liegt",
        answerC = "Suprematismus — die Befreiung der Kunst von jedem Bezug zur sichtbaren Welt",
        answerD = "Purismus — die Suche nach der unveraenderten reinen Form ohne Emotion",
        correctAnswer = 1,
        explanation = "Mondrian nannte seine Theorie 'Neoplastizismus' (niederlaendisch: Nieuwe Beelding). Er glaubte, dass hinter der sichtbaren Welt eine universale Harmonie liegt, die sich in den Grundgegensaetzen des Horizontalen (das Weibliche, Ruhende) und Vertikalen (das Maennliche, Dynamische) manifestiert. Die Primarfarben Rot, Gelb, Blau plus Weiss, Schwarz und Grau sollten die reinsten, universellsten Ausdrucksmittel sein. Mondrian war beeinflusst von der Theosophie und strebte nach einer spirituellen Reinheit der Form. Er glaubte, Neoplastizismus werde die Basis einer neuen universalen Aesthtik der Gesellschaft.",
        difficulty = 5,
        funFact = "Mondrian liebte Jazz und Boogie-Woogie leidenschaftlich — sein letztes vollendetes Werk heisst 'Broadway Boogie-Woogie' (1942/43). Er tanzte regelmaessig und umgab sich mit dem lebendigen New Yorker Nachtleben, waehrend er geometrische Strenge malte."
    ),

    // Frage 36 — Wandmalerei Pompeji
    Question(
        categoryId = 11,
        questionText = "Die Wandgemaelde von Pompeji werden nach vier Stilen klassifiziert. Welcher Stil imitierte Marmorverkleidungen und galt als der frueheste?",
        answerA = "Zweiter Stil (Architekturstil) — illusionistische Architekturmalerei mit scheinbaren Raumtiefe",
        answerB = "Erster Stil (Inkrustationsstil) — Nachahmung von Marmorvortafelungen durch Stuck und Farbe",
        answerC = "Dritter Stil (Kandelaberostil) — ornamentale Dekorationen mit zarten Rahmungen",
        answerD = "Vierter Stil (Phantasiestil) — Kombination aller frueher Elemente mit mythologischen Szenen",
        correctAnswer = 1,
        explanation = "Der Erste Stil der pompejanischen Wandmalerei, auch 'Mauerwerk-' oder 'Inkrustationsstil' genannt, entstand etwa von 200 v. Chr. bis 80 v. Chr. Er imitierte durch Stuckauflage und Farbe edle Marmorverkleidungen und farbige Steinplatten (Inkrustationen), wie sie in hellenistischen Palaesten ueblich waren. Da echter Marmor unbezahlbar teuer war, ahmte die Mittelschicht diesen Luxus malerisch nach. Der zweite, dritte und vierte Stil wurden vom deutschen Archaeologen August Mau im 19. Jahrhundert als Klassifizierungssystem eingefuehrt.",
        difficulty = 5,
        funFact = "Einige Raume in Pompeji zeigen den Vierten Stil noch in erstaunlich gutem Erhaltungszustand, weil der Vesuv-Ausbruch die Bilder unter einer Asche- und Bimsstein-Schicht versiegelte — sie hatten seit 79 n. Chr. kein Sonnenlicht gesehen, bis Ausgrabungen begannen."
    ),

    // Frage 37 — Goya Schwarze Gemaelde
    Question(
        categoryId = 11,
        questionText = "Francisco Goyas 'Schwarze Gemaelde' entstanden ca. 1819–1823 direkt auf den Waenden seiner Landhausvilla 'La Quinta del Sordo'. Warum ist ihre Ueberlieferung so problematisch?",
        answerA = "Goya malte sie nur auf Gips, der bald zu blaetern begann — die Bilder sind deshalb nur in Kopien erhalten",
        answerB = "Goya hinterliess keine Titel oder Erklaerungen; sie wurden nicht fuer die Oeffentlichkeit geschaffen, und Sinn und Bedeutung sind bis heute weitgehend ungeklaert",
        answerC = "Nach Goyas Tod zerstoerte seine Familie die meisten Bilder aus Scham ueber ihren schauerigen Inhalt",
        answerD = "Die Bilder wurden 1874 von der Wand geloest, auf Leinwand uebertragen, dabei aber stark beschaedigt und teilweise veraendert",
        correctAnswer = 1,
        explanation = "Beide Aussagen B und D enthalten wahre Elemente, aber die Kernaussage ist B: Goya hinterliess keine Titel, Erklaerungen oder schriftlichen Hinweise auf die Schwarzen Gemaelde. Sie wurden nicht in Auftrag gegeben, nicht fuer die Oeffentlichkeit gemacht — sie waren rein persoenliche Ausdrucksgeste eines tauben, alten, verbitterten Mannes. Gleichzeitig ist richtig (D), dass sie 1874 vom deutschen Bankier Baron Emile d'Erlanger auf Leinwand uebertragen wurden, dabei durch die Restauratoren Salvador Martinez Cubells stark veraendert wurden. Die Originale auf den Hauswanden existieren nicht mehr.",
        difficulty = 5,
        funFact = "Das bekannteste Schwarze Gemaelde 'Saturno devorando a su hijo' (Saturn verschlingt seinen Sohn) zeigt ein riesiges Monster beim Verschlingen eines Menschen. Es haengt heute im Prado in Madrid und gilt als eines der beunruhigendsten Bilder der Kunstgeschichte."
    ),

    // Frage 38 — Kuenstlernamen und Pseudonyme
    Question(
        categoryId = 11,
        questionText = "El Greco ist eines der bekanntesten Kuenstler-Pseudonyme der Kunstgeschichte. Wie lautete sein eigentlicher buergerlicher Name?",
        answerA = "Jorge Theotokopulos aus Sevilla",
        answerB = "Domenikos Theotokopoulos, geboren auf Kreta",
        answerC = "Domenico Greco aus Venedig, spaeter eingebuergerter Spanier",
        answerD = "Konstantinos Hellenikos aus Konstantinopel",
        correctAnswer = 1,
        explanation = "El Grecos buergerlicher Name war Domenikos Theotokopoulos (griechisch: Δομήνικος Θεοτοκόπουλος). Er wurde um 1541 auf Kreta geboren (damals venezianisches Territorium), kam nach Venedig und Rom, bevor er sich in Toledo/Spanien niederliess. 'El Greco' bedeutet auf Spanisch schlicht 'Der Grieche' — ein Spitzname, der auf seine Herkunft verwies. Er selbst unterschrieb seine Bilder immer auf Griechisch mit seinem vollen Nachnamen. Seine Maltechnik verband byzantinische Ikonenmalerei, venezianischen Manierismus und spanische Mystik.",
        difficulty = 5,
        funFact = "El Greco wurde jahrhundertelang vergessen und galt als Kuriositaet. Erst um 1900 'wiederentdeckten' ihn Kuenstler wie Paul Cézanne und Pablo Picasso — seine gelaengten Figuren und expressiven Farben beeinflussten Picassos fruehe Periode und den Expressionismus."
    ),

    // Frage 39 — Expressionismus Nolde
    Question(
        categoryId = 11,
        questionText = "Emil Nolde war NSDAP-Mitglied und ueberzeugter Nationalsozialist. Trotzdem erklaerten die Nationalsozialisten seine Kunst als 'entartet' und verboten ihm zu malen. Wie umging er das Malverbot?",
        answerA = "Er malte heimlich Aquarelle auf kleinen, leicht versteckbaren Blaettern, die er 'Ungemalte Bilder' nannte",
        answerB = "Er malte weiter offen in seinem Haus und bestach einen Gauleiter, der ihn deckte",
        answerC = "Er floh nach Daenemark und malte dort unter falschem Namen weiter",
        answerD = "Er arbeitete als Restaurator in staatlichen Sammlungen und malte dabei eigene Werke auf die Rueckseiten von Verpackungspappe",
        correctAnswer = 0,
        explanation = "Emil Nolde malte nach dem offiziellen Malverbot 1941 heimlich mehr als 1.300 kleine Aquarelle auf Papier, die er als 'Ungemalte Bilder' bezeichnete, weil er sie niemandem zeigen durfte. Er versteckte sie in seinem Haus in Seebull. Erst nach dem Krieg wurden sie der Oeffentlichkeit bekannt. Noldes Fall ist eines der bekanntesten Beispiele fuer die paradoxe Situation vieler Kuenstler im Dritten Reich: Politisch loyal, aber kuenstlerisch verfolgt. Heute ist Noldes NS-Mitgliedschaft ein wichtiger Teil der kritischen Diskussion um sein Werk.",
        difficulty = 5,
        funFact = "Nolde beschwerte sich persoenlich beim NS-Kulturkammer-Chef Goebbels und schrieb Briefe, in denen er betonte, er sei ein treuer Nationalsozialist — aber seine Kunst blieb verboten. Das Naziregime trennte strikt zwischen politischer Loyalitaet und kuenstlerischer 'Rassenreinheit'."
    ),

    // Frage 40 — Tizian Lebenswerk
    Question(
        categoryId = 11,
        questionText = "Tizian (Tiziano Vecellio) gilt als einer der laengsten lebenden Kuenstler der Kunstgeschichte. In welchem Alter starb er ungefaehr, und welches Bild arbeitete er noch kurz vor seinem Tod?",
        answerA = "Mit ca. 88 Jahren; sein letztes Werk war 'Die Pieta', die er fuer sein eigenes Grabmal begann",
        answerB = "Mit ca. 99 Jahren; sein letztes Werk war ein Selbstportrat, das er nie vollendete",
        answerC = "Mit ca. 94 Jahren; er arbeitete bis zu seinem Tod an 'Marsyas wird geschunden'",
        answerD = "Mit ca. 76 Jahren; er starb an der Pest, nachdem er das letzte Abendmahl vollendet hatte",
        correctAnswer = 0,
        explanation = "Tizians genaues Geburtsjahr ist unbekannt (geschaetzt zwischen 1488-1490), er starb 1576 — damit lebte er etwa 86-88 Jahre. In seinen letzten Jahren arbeitete er intensiv an der 'Pieta' (heute in der Accademia in Venedig), die er fuer sein eigenes Grabdenkmal in der Frari-Kirche bestimmte. Das Bild zeigt die tote Christusfigur in einer Nische, mit einer Bildinnschrift, die als Tizians Bitte um Fuerbitte gedeutet wird. Er starb an der Pest, bevor das Bild vollendet war — es wurde von seinem Schueler Palma il Giovane fertiggestellt.",
        difficulty = 5,
        funFact = "Tizian ueberlebte fast alle seine Zeitgenossen und Konkurrenten. Er malte noch im Alter von ueber 80 Jahren mit aussergewoehnlicher Energie. Seine spaete Maltechnik, bei der er mit den Fingern malte und die Farbe grob und unfertig wirken liess, gilt als Vorwegnahme des Impressionismus um 300 Jahre."
    ),

    // Frage 41 — Camille Claudel
    Question(
        categoryId = 11,
        questionText = "Camille Claudel, Bildhauerin und enge Mitarbeiterin und Geliebte Auguste Rodins, verbrachte die letzten 30 Jahre ihres Lebens in einer psychiatrischen Anstalt. Was loeste ihre Einweisung 1913 aus, und was geschah mit ihrem Atelier?",
        answerA = "Sie erlitt nach dem Tod ihres Vaters einen psychischen Zusammenbruch; ihr Bruder, der Dichter Paul Claudel, liess sie einweisen und ihr Atelier raeumen und zerstoeren lassen",
        answerB = "Rodin liess sie einweisen, nachdem sie oeffentlich behauptete, er habe ihre Ideen gestohlen, und dann ihr Atelier verkauft",
        answerC = "Sie versuchte, Rodin zu erstechen; nach dem Vorfall wurde ihr Atelier auf Gerichtsbeschluss versiegelt und ihr Werk beschlagnahmt",
        answerD = "Nach einer Fehlgeburt zerstoerte sie selbst ihr gesamtes Atelier und wurde dann auf eigenen Wunsch eingewiesen",
        correctAnswer = 0,
        explanation = "Nach dem Tod ihres Vaters 1913 (der sie stets unterstuetzt hatte) liess ihr Bruder Paul Claudel, ein kath. Dichter und Diplomat, sie gegen ihren Willen in eine Nervenheilanstalt einweisen. Ihr Atelier wurde von der Familie geleert und ein grosser Teil ihrer Werke zerstoert. Obwohl Aerzte mehrfach bescheinigten, sie sei nicht gefaehrlich und koenne entlassen werden, blieb sie bis zu ihrem Tod 1943 eingesperrt. Paul Claudel besuchte sie in 30 Jahren nur wenige Male. Ihr Werk wurde erst nach dem Zweiten Weltkrieg systematisch wiederentdeckt.",
        difficulty = 5,
        funFact = "Das Musee Rodin in Paris besitzt heute bedeutende Werke von Camille Claudel — ironischerweise lagern Teile ihres Lebenswerks im Museum ihres Geliebten und Mentors, der von vielen als Mitschuldiger an ihrem Schicksal gesehen wird."
    ),

    // Frage 42 — Pop Art Warhol Technik
    Question(
        categoryId = 11,
        questionText = "Andy Warhol verwendete fuer seine beruehm ten Werke (Marilyn, Campbell's Soup) hauptsaechlich eine bestimmte Drucktechnik. Warum wechselte er von der Handmalerei zur Siebdrucktechnik?",
        answerA = "Er wollte den Mythos des einzigartigen Kunstwerks zerstoeren und stattdessen serielle Massenproduktion als Kunststrategie einsetzen",
        answerB = "Ein Arbeitsunfall hatte seine rechte Hand beschaedigt, sodass er nicht mehr differenziert malen konnte",
        answerC = "Sein Galerist Leo Castelli zwang ihn zum Siebdruck, weil Handgemaelde zu teuer in der Produktion waren",
        answerD = "Er verwendete den Siebdruck, weil er die exakte Reproduzierbarkeit fuer Museen sicherstellen wollte",
        correctAnswer = 0,
        explanation = "Warhol wechselte bewusst zum Siebdruck, um den romantischen Mythos des handgefertigten Originals zu dekonstruieren. Er gruendete sein Atelier 'The Factory' und liess Mitarbeiter Bilder produzieren, oft ohne selbst Hand anzulegen. Er sagte explizit, er wolle eine Maschine sein und wie eine Maschine produzieren. Die serielle Wiederholung (32 Versionen der Marilyn, Dutzende Campbell-Dosen) stellte Fragen nach Einzigartigkeit, Reproduzierbarkeit und Wert — Kernanliegen der Pop Art. Die 'Fabrik' war auch Aussage: Kunst als industrielle Produktion.",
        difficulty = 5,
        funFact = "Warhol verlangte manchmal von Sammlern, ein Foto einzusenden, das er dann als Siebaruckvorlage verwendete — ein fruehes Beispiel fuer 'personalized art as service'. Sein Haus war vollgestopft mit Kuchen, Kaese und Koffer — er konnte sich bis zu seinem Tod nicht trennen von Hamstervorraeten."
    ),

    // Frage 43 — Kunstmarkt China
    Question(
        categoryId = 11,
        questionText = "China ist seit etwa 2010 einer der groessten Kunstmarkte der Welt. Welches spezifische Problem haett massgeblich zur Entwicklung des chinesischen Kunstmarkts beigetragen und gilt als strukturelles Charakteristikum?",
        answerA = "Kunstkaeufe gelten in China als sichere Steuervermeidung, da keine Mehrwertsteuer auf Kunsthandel erhoben wird",
        answerB = "Ein hoher Anteil der Auktionsergebnisse in China wurde als 'Phantom Sales' (Scheinverkaeufe) identifiziert, bei denen Kaeufer nicht zahlen oder Transaktionen fiktiv sind",
        answerC = "China foerdert Kunstexporte durch staatliche Subventionen, was den Markt kuenstlich aufblaehte",
        answerD = "Wegen strenger Kapitalverkehrskontrollen konnten chinesische Milliardaere nur durch Kunstkaeufe Kapital ins Ausland transferieren",
        correctAnswer = 1,
        explanation = "Mehrere unabhaengige Studien und Berichte (u.a. von ArtTactic und Tefaf) haben dokumentiert, dass ein erheblicher Anteil der gemeldeten Auktionskaeufe in China nicht bezahlt wird — die Kaeufer tauchen nicht auf oder zahlen nie. Diese 'Phantom Sales' verzerren Statistiken massiv. Schatzungen zufolge wurden zeitweise 30-60% aller gemeldeten Auktionsumsaetze in China nie tatsaechlich abgewickelt. Dies macht Vergleiche mit westlichen Maerkten schwierig und wirft grundlegende Fragen ueber die Zuverlaessigkeit chinesischer Kunstmarktdaten auf.",
        difficulty = 5,
        funFact = "Gleichzeitig ist Antwort D auch ein reales Phnaomen: Kunstkaeufe wurden tatsaechlich als Mittel zur Kapitalflucht genutzt, bis die chinesische Zentralbank gegensteuerte."
    ),

    // Frage 44 — Neurowissenschaft und Kunst
    Question(
        categoryId = 11,
        questionText = "Die Neurowissenschaft des aesthetischen Erlebens (Neuroaesthetik) wurde massgeblich von einem Kunsthistoriker und einem Neurologen gemeinsam begruendet. Wer sind die Gruender dieser Disziplin?",
        answerA = "Ernst Gombrich und Oliver Sacks, die gemeinsam 1990 die erste Studie zu Hirnaktivitaet beim Betrachten von Kunst publizierten",
        answerB = "Semir Zeki (Neurowissenschaftler) und Ramachandran, der den Begriff 'Neuroaesthetik' praegte und mit Erkenntnissen aus der Kunst verknuepfte",
        answerC = "Semir Zeki (Neurowissenschaftler) und Kunstkritiker John Ruskin (posthum gewuerdigter Beitrag)",
        answerD = "Antonio Damasio und Hans Belting, die gemeinsam 'Bild-Anthropologie' und Neuroaesthetik verbanden",
        correctAnswer = 1,
        explanation = "Der Begriff 'Neuroaesthetik' wurde massgeblich vom Neurowissenschaftler Semir Zeki (University College London) gepraegte und populaer gemacht, der ihn in seinem Buch 'Inner Vision' (1999) verwendete. V.S. Ramachandran trug ebenfalls entscheidend zur Disziplin bei, insbesondere mit seinen 'neun Gesetzen der Aesthetik' (2001). Beide forschten unabhaengig und wurden dann zu den zentralen Figuren dieser neuen Disziplin. Zeki untersuchte speziell, wie das Gehirn Schoenheit verarbeitet und identifizierte den medialen orbitofrontalen Kortex als zentrale Region der Schoenheitserfahrung.",
        difficulty = 5,
        funFact = "Zekis Forschungen ergaben, dass das Betrachten eines als schoen empfundenen Kunstwerks dieselben Hirnareale aktiviert wie romantische Liebe — insbesondere das Belohnungssystem mit Dopaminausschuettung."
    ),

    // Frage 45 — Caravaggio Erste Auftragsablehnung
    Question(
        categoryId = 11,
        questionText = "Caravaggios erste Version der 'Berufung des Heiligen Matthaeus' fuer die Contarelli-Kapelle in Rom wurde vom Auftraggeber abgelehnt. Was war der Grund fuer die Ablehnung?",
        answerA = "Caravaggio malte Matthaeus mit schmutzigen, nackten Fuessen, was als wuerdeloses Bild eines Heiligen galt",
        answerB = "Die erste Version zeigte Matthaeus als analphabetischen Bauern, dem ein Engel die Hand fuehren musste — die Kleriker lehnten es ab, einen Apostel so ungelehrt darzustellen",
        answerC = "Caravaggio hatte die Figur Christi zu wenig beleuchtet und dafuer den Teufel prominent dargestellt",
        answerD = "Die erste Version zeigte Matthaeus von hinten — was kirchenrechtlich verboten war fuer heilige Darstellungen",
        correctAnswer = 1,
        explanation = "Caravaggios erste Version des 'Heiligen Matthaeus mit dem Engel' (heute zerstoert, nur aus Fotos bekannt) zeigte Matthaeus als rustikalen, schlichter Bauer mit nackten, schmutzigen Fuessen, der von einem Engel gefuehrt wird, als koennte er nicht selbst schreiben. Die Kleriker der Contarelli-Kapelle lehnten das Bild ab — es sei nicht wuerdevoll genug fuer einen Apostel und Evangelisten. Caravaggio schuf eine zweite Version, in der Matthaeus als intelligenter Mann mit Buch dargestellt ist, waehrend ein Engel ihm zuschaut, nicht seine Hand fuehrt.",
        difficulty = 5,
        funFact = "Die erste abgelehnte Version des Matthaeus wurde vom Kardinal Francesco Maria Del Monte aufgekauft und hing in seiner Privatsammlung in Rom. Es wurde im Zweiten Weltkrieg in Berlin ausgelagert und 1945 beim Brand des Flakturms Friedrichshain zerstoert — ein unersetzlicher Verlust."
    ),

    // Frage 46 — Damien Hirst Hai
    Question(
        categoryId = 11,
        questionText = "Damien Hirsts beruehm tes Werk 'The Physical Impossibility of Death in the Mind of Someone Living' (1991) zeigt einen echten Tigerhai in Formaldehyd. Welches strukturelle Problem zwang zur aufwendigen Restaurierung?",
        answerA = "Das Formaldehyd verdunstete ueber die Jahre, sodass der Hai zunehmend freilag",
        answerB = "Der Hai begann zu verwesen, da die Formaldehyd-Konservierung nicht ausreichend gewesen war, und musste 2006 vollstaendig durch einen neuen Hai ersetzt werden",
        answerC = "Die Glasscheiben des Tanks brachen durch den Druck, und der Hai musste entnommen und neu positioniert werden",
        answerD = "Das Formaldehyd faerbte den Hai ungleichmaessig gelb, was den Kuenstler dazu veranlasste, das Tier auszutauschen",
        correctAnswer = 1,
        explanation = "Der originale Hai in Hirsts Werk wurde tatsaechlich nicht ausreichend konserviert und begann zu verwesen. Die Haut wellte sich, der Hai bekam eine traube, schlaffe Textur. 2006 liess Hirst den Shark vollstaendig durch einen neu gefangenen Tigerhai ersetzen. Dies loeste eine Kontroverse aus: Ist das Werk noch dasselbe, wenn sein zentraler Inhalt ausgetauscht wurde? Der amerikanische Sammler Steven Cohen, der das Werk 2004 fuer angeblich 8 Millionen Dollar gekauft hatte, stimmte dem Austausch zu. Die Frage nach Identitaet und Originalitaet bei konzeptueller Kunst wurde dadurch zum Thema.",
        difficulty = 5,
        funFact = "Den Hai kaufte Hirst urspruenglich fuer 6.000 australische Dollar von einem Fischhändler in Australien, nachdem er Saatchi gefragt hatte, 'ob Geld kein Problem sei'. Das Werk wurde spaeter fuer ca. 12 Millionen Dollar gehandelt."
    ),

    // Frage 47 — Kunstdiebstahl Isabella Stewart Gardner
    Question(
        categoryId = 11,
        questionText = "Der Kunstraub im Isabella Stewart Gardner Museum in Boston (1990) gilt als groesster Kunstdiebstahl in der Geschichte. Wie viele Werke wurden gestohlen, und welches ist der bekannteste Verlust?",
        answerA = "6 Werke, darunter Vermeers 'Die Konzert' und Rembrandts 'Christus im Sturm'",
        answerB = "13 Werke, darunter Rembrandts 'Christus im Sturm' und Vermeers 'Das Konzert' — seither nie wiedergefunden",
        answerC = "22 Werke, darunter ein unvollendetes Vermeer-Gemaelde und drei Rembrandt-Drucke",
        answerD = "9 Werke, darunter Degas' 'Im Cafe' und ein Manet-Portraet",
        correctAnswer = 1,
        explanation = "In der Nacht vom 18. auf den 19. Maerz 1990 stahlen zwei als Polizisten verkleidete Taeter 13 Kunstwerke aus dem Gardner Museum — darunter Vermeers 'Das Konzert' (das einzige Vermeer in amerikanischem Privatbesitz), Rembrandts 'Christus im Sturm auf dem See Genezareth' (sein einziges bekanntes Seestueck), eine Rembrandtselbstportrat-Radierung und fuenf Degas-Skizzen. Der Gesamtschaetzwert betraegt ueber 500 Millionen Dollar. Bis heute sind die Werke verschwunden — das FBI vermutet, sie seien im Untergrund der organisierten Kriminalitaet.",
        difficulty = 5,
        funFact = "Im Gardner Museum hangen bis heute die leeren Rahmen der gestohlenen Bilder — Isabella Stewart Gardner hatte testamentarisch festgelegt, dass nichts im Museum veraendert werden darf. Die Leerrahmen sind zu Mahnmalen geworden."
    ),

    // Frage 48 — Albrecht Duerer und Proportionen
    Question(
        categoryId = 11,
        questionText = "Albrecht Duerer veroeffentlichte 1525 'Underweysung der Messung' (Anleitung zur Messung). Was war das Revolutionaere an dieser Publikation in der Geschichte der Kunst?",
        answerA = "Es war das erste deutschsprachige Buch, das die italiaenische Perspektivlehre nordeuropaeischen Kuenstlern vermittelte, mit praktischen geometrischen Konstruktionsanleitungen",
        answerB = "Es war die erste Publikation, die Kunsthandwerk und Freie Kunst gleichstellte und damit den Status des Kuenstlers veraenderte",
        answerC = "Es war das erste Buch, das mathematische Beweise fuer aesthetische Schoenheitsregeln lieferte",
        answerD = "Es war die erste Anleitung zur Oelmalerei in Deutschland, die Leinwand statt Holz empfahl",
        correctAnswer = 0,
        explanation = "Duerers 'Underweysung der Messung' von 1525 war das erste deutschsprachige Werk, das die Grundlagen der Perspektive und Geometrie fuer Kuenstler systematisch erklaerte. Es vermittelte die italiaenische Zentralperspektivlehre (von Alberti bis Leonardo) erstmals einem nordeuropaeischen Publikum auf Deutsch, mit praktischen Konstruktionszeichnungen und Anleitungen. Es behandelte Punkt, Linie, Flaeche und Koerper aus mathematischer Perspektive und stellte Instrumente vor. Das Werk wurde ins Lateinische, Franzoesische und Niederlerlandische uebersetzt und pragte das nordeuropaeische Kunsthandwerk massgeblich.",
        difficulty = 5,
        funFact = "Duerer bereiste zweimal Italien (1494 und 1505) und war tief beeindruckt von der dort herrschenden Wertschaetzung fuer Kuenstler. Er klagte, dass in Deutschland Maler kaum mehr als Handwerker galten — obwohl er selbst als Genie gefeiert wurde."
    ),

    // Frage 49 — Joseph Beuys Performance
    Question(
        categoryId = 11,
        questionText = "Joseph Beuys schuf 1974 die Performance 'Ich liebe Amerika und Amerika liebt mich', bei der er drei Tage mit einem lebenden Kojoten in einer New Yorker Galerie eingesperrt war. Welche spezifische rituelle Handlung wiederholte sich in dieser Performance taeglich?",
        answerA = "Beuys wickelte sich jede Stunde vollstaendig in Filz ein und lag reglos, bevor er wieder auftauchte",
        answerB = "Er liess jeden Morgen die 'Wall Street Journal' Zeitung in die Galerie liefern, wickelte sich in Filz, bearbeitete den Kojoten mit seinem Hirtenstab und haute die Zeitung",
        answerC = "Er sprach jeden Morgen einen deutschen Text vor, waehrend der Kojote ihn beobachtete",
        answerD = "Beuys und der Kojote schliefen Seite an Seite auf einem grossen Filztuch, das er taeglich neu auslegte",
        correctAnswer = 1,
        explanation = "In der Performance 'I like America and America likes Me' (Rene Block Gallery, New York, 1974) wickelte sich Beuys taeglich in Filz (sein Leitmaterial), stand auf Stroh, kommunizierte mit dem Kojoten ueber seinen Hirtenstab und liess taeglich eine Ausgabe des Wall Street Journal in die Galerie bringen — die er aufrollte und mit dem Stab in den Raum legte. Der Kojote zerfetzte und beruhrte die Zeitung. Die Performance thematisierte das Verhaltnis von Ureinwohnern (der Kojote als heiliges Tier der Native Americans) und dem kapitalistischen Amerika (Wall Street Journal). Beuys betrat und verliess Amerika ausschliesslich durch die Galerie, ohne den amerikanischen Boden ausserhalb zu beruehren.",
        difficulty = 5,
        funFact = "Beuys liess sich bei seiner Ankunft in Amerika am Flughafen in Filz einwickeln und mit einer Krankentrage direkt in die Galerie transportieren — er wollte keinen anderen amerikanischen Boden beruehren als den Galerieraum, als symbolischen Akt gegen die Vietnam-Kriegspolitik."
    ),

    // Frage 50 — Kunsttheorie Iconography
    Question(
        categoryId = 11,
        questionText = "Erwin Panofsky begruendete die Ikonographie und Ikonologie als kunstwissenschaftliche Methoden. Was ist der wesentliche Unterschied zwischen Ikonographie und Ikonologie?",
        answerA = "Ikonographie beschreibt nur religiose Motive; Ikonologie beschreibt weltliche Symbole",
        answerB = "Ikonographie ist die Beschreibung und Klassifikation von Bildmotiven und Symbolen; Ikonologie geht einen Schritt weiter und fragt nach der kulturellen, philosophischen und weltanschaulichen Bedeutung hinter diesen Motiven",
        answerC = "Ikonographie analysiert zweidimensionale Bilder; Ikonologie bezieht auch Skulptur und Architektur ein",
        answerD = "Ikonographie ist die historische Methode; Ikonologie die zeitgemaesse, psychoanalytische Methode nach Freud",
        correctAnswer = 1,
        explanation = "In Panofskys Drei-Stufen-Modell (aus 'Studies in Iconology', 1939) unterscheidet er: 1) Praeikonographische Beschreibung (Was sehe ich? — Formen, Farben), 2) Ikonographische Analyse (Was bedeuten die Motive? — Symbole, Attribute, Szenen identifizieren), 3) Ikonologische Interpretation (Warum dieses Bild in dieser Zeit? — kulturelle, philosophische, weltanschauliche Tiefenbedeutung). Die Ikonologie fragt nach dem 'symbolischen Wert' eines Bildes in seinem historischen und kulturellen Kontext — sie geht weit ueber das bloss e Identifizieren von Symbolen hinaus.",
        difficulty = 5,
        funFact = "Panofsky floh 1933 vor den Nazis aus Hamburg in die USA, wo er am Institute for Advanced Study in Princeton arbeitete. Seine Methoden prägten die gesamte angloamerikanische Kunstgeschichte — ohne seine Emigration waere die amerikanische Kunstwissenschaft voellig anders."
    )
)
