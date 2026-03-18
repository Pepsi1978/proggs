package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsMedium5(): List<Question> = listOf(

    // ── NUTRITION SCIENCE ────────────────────────────────────────────────────

    // Question 1 – Nutrition Science: Glycemic Index
    // Correct answer: B → moved to position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der glykämische Index (GI) eines Lebensmittels?",
        answerA = "Den Gesamtzuckergehalt in Gramm pro 100 g Lebensmittel",
        answerB = "Den prozentualen Anteil von Fructose im Vergleich zu Glucose",
        answerC = "Die Geschwindigkeit und das Ausmaß, mit dem ein Lebensmittel den Blutzucker anhebt, verglichen mit reiner Glucose",
        answerD = "Die Anzahl der Kalorien, die aus Kohlenhydraten stammen",
        correctAnswer = 2, // C
        explanation = "Der glykämische Index (GI) misst, wie schnell und wie stark ein kohlenhydrathaltiges Lebensmittel den Blutzuckerspiegel im Vergleich zu reiner Glucose (GI = 100) anhebt. Lebensmittel mit hohem GI (>70) lassen den Blutzucker schnell steigen, während niedrig-GI-Lebensmittel (<55) einen langsameren Anstieg bewirken.",
        difficulty = 2,
        funFact = "Weißbrot hat einen höheren GI (ca. 75) als Haushaltszucker (ca. 65), da die Stärke im Brot sehr schnell verdaut wird."
    ),

    // Question 2 – Nutrition Science: Omega-3/6
    // Correct answer stays at position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Welches Verhältnis von Omega-6- zu Omega-3-Fettsäuren gilt in der modernen westlichen Ernährung als problematisch?",
        answerA = "1:1 (ausgeglichen)",
        answerB = "3:1 (leicht erhöhtes Omega-6)",
        answerC = "1:10 (stark erhöhtes Omega-3)",
        answerD = "15:1 bis 20:1 (stark erhöhtes Omega-6)",
        correctAnswer = 3, // D
        explanation = "In der modernen westlichen Ernährung liegt das Verhältnis von Omega-6 zu Omega-3 oft bei 15:1 bis 20:1 – das evolutionäre Optimum war etwa 4:1 oder niedriger. Ein zu hohes Omega-6-Verhältnis fördert entzündliche Prozesse, da Omega-6-Fettsäuren (insbesondere Arachidonsäure) Vorläufer proentzündlicher Eicosanoide sind.",
        difficulty = 2,
        funFact = null
    ),

    // Question 3 – Nutrition Science: Antioxidants
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Welchen chemischen Mechanismus nutzen Antioxidantien wie Vitamin C, um freie Radikale zu neutralisieren?",
        answerA = "Sie spenden ein Elektron an freie Radikale und stabilisieren diese, ohne selbst gefährlich reaktiv zu werden",
        answerB = "Sie binden freie Radikale enzymatisch und bauen sie in der Leber ab",
        answerC = "Sie umhüllen freie Radikale mit einer Lipidschicht und machen sie fettlöslich",
        answerD = "Sie katalysieren die Umwandlung von Sauerstoffradikalen in Wasserstoff",
        correctAnswer = 0, // A
        explanation = "Antioxidantien wie Vitamin C (Ascorbinsäure) neutralisieren freie Radikale durch Elektronenspende. Das entstehende Ascorbylradikal ist deutlich stabiler und weniger reaktiv als das ursprüngliche Radikal. Andere Antioxidantien wie Vitamin E können das Ascorbylradikal anschließend regenerieren.",
        difficulty = 2,
        funFact = "Vitamin C kann sich selbst regenerieren: Das entstandene Vitamin-C-Radikal wird durch Glutathion wieder in aktives Vitamin C umgewandelt."
    ),

    // Question 4 – Nutrition Science: Probiotics
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Wie wirken Probiotika im Darm, um die Gesundheit zu fördern?",
        answerA = "Sie verdauen unverdauliche Ballaststoffe direkt zu Glucose und erhöhen die Energieausbeute",
        answerB = "Sie stimulieren ausschließlich die Produktion von Magensäure für bessere Verdauung",
        answerC = "Sie synthetisieren fettlösliche Vitamine direkt in der Darmschleimhaut",
        answerD = "Sie konkurrieren mit pathogenen Keimen um Nährstoffe und Haftungsstellen, produzieren antimikrobielle Substanzen und stärken die Darmbarriere",
        correctAnswer = 3, // D
        explanation = "Probiotische Bakterien wie Lactobacillus und Bifidobacterium wirken durch mehrere Mechanismen: Kompetition mit Pathogenen um Bindungsstellen, Produktion von Milchsäure und Bacteriocinen (antimikrobielle Peptide), Stärkung der Darmbarriere durch Förderung von Tight Junctions und Modulation des Immunsystems.",
        difficulty = 2,
        funFact = null
    ),

    // Question 5 – Nutrition Science: Micronutrient Deficiency
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Welche Erkrankung entsteht durch chronischen Vitamin-D-Mangel bei Kindern?",
        answerA = "Rachitis – Störung der Knochenmineralisation mit Skelettverformungen",
        answerB = "Skorbut – Bindegewebsschwäche durch fehlende Kollagensynthese",
        answerC = "Pellagra – Hautausschlag und neurologische Störungen",
        answerD = "Beriberi – periphere Neuropathie durch gestörten Glukosestoffwechsel",
        correctAnswer = 0, // A
        explanation = "Rachitis entsteht durch Vitamin-D-Mangel, der die Kalzium- und Phosphataufnahme im Darm vermindert. Dadurch kann die Knochenmatrix (Osteoid) nicht ausreichend mineralisiert werden, was zu weichen, verformbaren Knochen führt. Typische Zeichen sind O-Beine, Rosenkranzbrust und verzögerter Zahnentwicklung.",
        difficulty = 2,
        funFact = "Rickets (Rachitis) war im industriellen England des 19. Jahrhunderts weit verbreitet, weil Kinder kaum Sonnenlicht bekamen und Vitamin D nicht synthetisieren konnten."
    ),

    // ── RENEWABLE ENERGY TECH ────────────────────────────────────────────────

    // Question 6 – Renewable Energy: Solar Cell Efficiency
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Was begrenzt die theoretische maximale Effizienz einer konventionellen Silizium-Solarzelle (Shockley-Queisser-Limit)?",
        answerA = "Die begrenzte Reflektivität des Siliziums für sichtbares Licht",
        answerB = "Die Leitfähigkeit des Siliziumkristalls, die bei höheren Temperaturen sinkt",
        answerC = "Die Tatsache, dass nur Photonen mit genau der Bandlückenenergie absorbiert werden können, und Überschussenergie als Wärme verloren geht",
        answerD = "Der maximale Füllfaktor, der durch parasitäre Widerstände auf 50 % begrenzt ist",
        correctAnswer = 2, // C
        explanation = "Das Shockley-Queisser-Limit (ca. 33 % für Silizium) ergibt sich aus zwei Hauptverlusten: Photonen mit weniger Energie als die Bandlücke (1,1 eV für Si) werden nicht absorbiert. Photonen mit mehr Energie werden absorbiert, aber die Überschussenergie wird als Wärme (Thermalisierungsverlust) abgegeben. Multijunction-Solarzellen mit mehreren Halbleiterschichten können dieses Limit überwinden.",
        difficulty = 2,
        funFact = "Die effizientesten Labor-Solarzellen (Multijunction mit Konzentratoren) haben Wirkungsgrade von über 47 % erreicht – weit über dem Si-Limit."
    ),

    // Question 7 – Renewable Energy: Wind Turbine Physics
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Was besagt das Betz'sche Gesetz in der Windenergie?",
        answerA = "Eine Windturbine kann maximal 100 % der kinetischen Windenergie in elektrische Energie umwandeln",
        answerB = "Die Leistung einer Windturbine ist direkt proportional zur Windgeschwindigkeit",
        answerC = "Der optimale Rotordurchmesser ist gleich der dreifachen Windgeschwindigkeit in Metern",
        answerD = "Eine Windturbine kann theoretisch maximal etwa 59,3 % der kinetischen Windenergie extrahieren",
        correctAnswer = 3, // D
        explanation = "Das Betz'sche Gesetz (1919) zeigt, dass der theoretische Maximalwirkungsgrad einer Windturbine bei 16/27 ≈ 59,3 % liegt. Würde die Turbine die gesamte kinetische Energie entziehen, käme der Wind zum Stillstand und kein neuer Wind könnte nachströmen. Der optimale Kompromiss liegt bei einer Windgeschwindigkeitsreduktion auf 1/3 des Ausgangswertes hinter der Turbine.",
        difficulty = 2,
        funFact = null
    ),

    // Question 8 – Renewable Energy: Battery Technology
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Was passiert beim Laden einer Lithium-Ionen-Batterie auf chemischer Ebene?",
        answerA = "Lithium-Ionen wandern von der Kathode zur Anode und werden in der Graphitstruktur der Anode eingelagert",
        answerB = "Lithium-Ionen wandern von der Anode zur Kathode und werden dort eingelagert",
        answerC = "Lithium wird durch Elektrolyse an der Kathode zu Lithiumhydroxid reduziert",
        answerD = "Elektronen fließen von der Kathode zur Anode und bilden eine Lithiumoxidschicht",
        correctAnswer = 0, // A
        explanation = "Beim Laden einer Li-Ionen-Batterie wandern Lithium-Ionen (Li⁺) durch den Elektrolyten von der Kathode (z. B. LiCoO₂) zur Anode (Graphit) und lagern sich zwischen den Graphitschichten ein (Interkalation). Gleichzeitig fließen Elektronen über den externen Stromkreis zur Anode. Beim Entladen kehrt der Prozess um.",
        difficulty = 2,
        funFact = "Der Begriff 'Rocking Chair Battery' beschreibt treffend, wie Lithium-Ionen beim Laden und Entladen hin und her 'schaukeln'."
    ),

    // Question 9 – Renewable Energy: Hydrogen Fuel Cells
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Welche chemische Reaktion findet in einer Polymer-Elektrolyt-Membran (PEM) Brennstoffzelle statt?",
        answerA = "Wasserstoff und Stickstoff reagieren zu Ammoniak, das dann Strom erzeugt",
        answerB = "Methanol verbrennt katalytisch an einer Platinelektrode und erzeugt Gleichstrom",
        answerC = "Wasserstoff wird an der Anode zu Protonen oxidiert, die durch die Membran zur Kathode wandern und dort mit Sauerstoff zu Wasser reagieren",
        answerD = "Kohlendioxid reagiert mit Wasserstoff zu Methan, wobei Elektronen freigesetzt werden",
        correctAnswer = 2, // C
        explanation = "In einer PEM-Brennstoffzelle wird an der Anode Wasserstoff zu Protonen (H⁺) und Elektronen (e⁻) oxidiert: H₂ → 2H⁺ + 2e⁻. Die Protonen wandern durch die Nafion-Membran zur Kathode, die Elektronen fließen als Strom durch den externen Kreislauf. An der Kathode reagieren Protonen, Elektronen und Sauerstoff zu Wasser: 2H⁺ + 2e⁻ + ½O₂ → H₂O.",
        difficulty = 2,
        funFact = null
    ),

    // Question 10 – Renewable Energy: Geothermal Details
    // Correct answer → position B (correctAnswer = 1)
    Question(
        categoryId = 2,
        questionText = "Woher stammt die Wärme im Erdinnern, die geothermische Energie speist?",
        answerA = "Ausschließlich aus der gespeicherten Restwärme der Erdentstehung vor 4,5 Milliarden Jahren",
        answerB = "Zu etwa gleichen Teilen aus der Restwärme der Erdentstehung und dem radioaktiven Zerfall von Isotopen wie Uran-238, Thorium-232 und Kalium-40",
        answerC = "Hauptsächlich aus Reibungswärme durch tektonische Plattenbewegungen",
        answerD = "Aus der solaren Strahlung, die tief in die Erdkruste eindringt",
        correctAnswer = 1, // B
        explanation = "Die Erdwärme hat zwei Hauptquellen: Ca. 40–50 % stammen noch aus der primordalen Wärme (Akkretion, Kollisionen, Kernbildung). Die restlichen 50–60 % werden kontinuierlich durch den radioaktiven Zerfall langlebiger Isotope produziert: ²³⁸U (Halbwertszeit 4,5 Mrd. J.), ²³²Th (14 Mrd. J.) und ⁴⁰K (1,3 Mrd. J.).",
        difficulty = 2,
        funFact = "Island deckt über 90 % seines Haushaltsheizbedarfs mit Geothermie, da es direkt auf dem Mittelatlantischen Rücken sitzt."
    ),

    // ── SENSORY BIOLOGY ───────────────────────────────────────────────────────

    // Question 11 – Sensory Biology: Color Blindness
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Warum ist Rot-Grün-Farbenblindheit bei Männern deutlich häufiger als bei Frauen?",
        answerA = "Das Testosteron bei Männern verändert die Empfindlichkeit der Zapfen im Auge",
        answerB = "Frauen haben mehr Zapfenzellen im Auge, was Defekte automatisch ausgleicht",
        answerC = "Das weibliche Immunsystem repariert Gendefekte in Netzhautzellen effizienter",
        answerD = "Die Gene für Rot- und Grünzapfen liegen auf dem X-Chromosom; Männer haben nur ein X-Chromosom und können den Defekt nicht durch ein zweites X kompensieren",
        correctAnswer = 3, // D
        explanation = "Die Gene für den Rot- (OPN1LW) und Grünzapfen (OPN1MW) liegen auf dem X-Chromosom (Xq28). Männer (XY) besitzen nur ein X-Chromosom: ein einziger Defekt führt zur Farbenblindheit. Frauen (XX) müssen beide X-Chromosomen betroffen sein – da das defekte X oft durch ein normales X kompensiert wird, sind nur ~0,5 % der Frauen, aber ~8 % der Männer betroffen.",
        difficulty = 2,
        funFact = "Einige Frauen besitzen durch einen X-chromosomalen Polymorphismus vier verschiedene Zapfentypen (Tetrachromaten) und können Farben unterscheiden, die für andere unsichtbar sind."
    ),

    // Question 12 – Sensory Biology: Tinnitus
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Was ist die wahrscheinlichste neurologische Ursache von chronischem Tinnitus?",
        answerA = "Eine gesteigerte spontane neuronale Aktivität im auditiven Kortex nach Verlust von Haarzellen im Innenohr",
        answerB = "Eine Überproduktion von Ohrenschmalz, die mechanischen Druck auf das Trommelfell erzeugt",
        answerC = "Eine Verengung der Blutgefäße im Innenohr, die zu einem pulsierenden Geräusch führt",
        answerD = "Eine Fehlfunktion des Gleichgewichtsorgans, die fehlerhafte Signale ans Gehirn sendet",
        correctAnswer = 0, // A
        explanation = "Nach Schädigung oder Verlust von Haarzellen (z. B. durch Lärm) verliert der auditive Kortex seinen normalen Eingangsreiz. Als Reaktion erhöht er seine Spontanaktivität (kortikale Reorganisation) – ähnlich wie Phantomschmerz nach Amputation. Das Gehirn interpretiert diese aberrante Aktivität als Ton. Dieser Mechanismus erklärt, warum Tinnitus oft nach Hörverlust auftritt.",
        difficulty = 2,
        funFact = null
    ),

    // Question 13 – Sensory Biology: Phantom Limb
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Wie erklärt das Konzept der kortikalen Reorganisation das Phänomen des Phantomschmerzes nach einer Amputation?",
        answerA = "Die abgetrennten Nervenenden im Stumpf senden weiterhin Schmerzsignale zum Gehirn",
        answerB = "Das Immunsystem greift verbliebene Nerven im Stumpf an und erzeugt dadurch Schmerzempfindungen",
        answerC = "Das Gehirn enthält eine Körperkarte (Homunkulus); benachbarte Körperareale übernehmen die verwaisten Kortexregionen und verursachen so Empfindungen im nicht mehr vorhandenen Glied",
        answerD = "Das Kleinhirn generiert Bewegungskommandos für die fehlende Extremität, die als Schmerz interpretiert werden",
        correctAnswer = 2, // C
        explanation = "Im somatosensorischen Kortex ist jeder Körperteil topographisch repräsentiert (Homunkulus nach Penfield). Nach Amputation erhält die entsprechende Kortexregion keinen Input mehr. Benachbarte Areale (z. B. Gesicht oder Schulter für Arm-Amputierte) expandieren in die verwaiste Region. Reize aus diesen Arealen werden fälschlicherweise als Empfindungen im amputierten Glied wahrgenommen.",
        difficulty = 2,
        funFact = "Der Neurologe V.S. Ramachandran entwickelte die 'Mirror Box Therapy': Durch Spiegelung des gesunden Armes kann das Gehirn überlistet werden, Phantomschmerzen zu lindern."
    ),

    // Question 14 – Sensory Biology: Synesthesia
    // Correct answer → position B (correctAnswer = 1)
    Question(
        categoryId = 2,
        questionText = "Was ist Synästhesie, und welche neurologische Erklärung wird am häufigsten genannt?",
        answerA = "Eine Störung, bei der alle Sinne ausfallen; erklärt durch Unteraktivierung des Thalamus",
        answerB = "Eine unwillkürliche Verschmelzung von Sinneswahrnehmungen (z. B. Buchstaben sehen Farben); erklärt durch verstärkte Querverbindungen zwischen benachbarten sensorischen Kortexarealen",
        answerC = "Eine Form von Halluzinationen durch dopaminerge Überaktivität im limbischen System",
        answerD = "Eine genetische Unfähigkeit, Sinneskanäle zu trennen; erklärt durch fehlende Myelinisierung der Sehbahn",
        correctAnswer = 1, // B
        explanation = "Bei der Synästhesie führt ein Sinnesreiz automatisch zu einer zusätzlichen Wahrnehmung in einem anderen Sinn (z. B. Graphem-Farb-Synästhesie: Buchstaben lösen Farberlebnisse aus). Die häufigste neurologische Erklärung ist eine erhöhte neuronale Konnektivität (Disinhibitions-Hypothese oder Cross-Activation-Hypothese) zwischen benachbarten Kortexarealen – z. B. zwischen dem Farb- und Buchstabenverarbeitungsbereich im fusiformen Gyrus.",
        difficulty = 2,
        funFact = null
    ),

    // Question 15 – Sensory Biology: Proprioception
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Welche Rezeptoren sind hauptsächlich für die Propriozeption (Tiefensensibilität) verantwortlich?",
        answerA = "Meissner-Körperchen und Merkel-Scheiben in der Hautoberfläche",
        answerB = "Nozizeptoren in Muskeln und Sehnen, die Dehnungsschmerzen registrieren",
        answerC = "Pacini-Körperchen in der Tiefe der Haut, die Vibration und Druck messen",
        answerD = "Muskelspindeln (Ia-Afferenzen), Golgi-Sehnenorgane und Gelenkrezeptoren",
        correctAnswer = 3, // D
        explanation = "Die Propriozeption – das Körperlageempfinden – beruht auf drei Rezeptortypen: Muskelspindeln (intrafusale Fasern mit Ia-Afferenzen) messen Muskellänge und -dehnungsrate. Golgi-Sehnenorgane (Ib-Afferenzen) registrieren Muskelspannung. Gelenkrezeptoren (Ruffini-Körperchen, Pacini-Körperchen) liefern Informationen über Gelenkstellung und -bewegung. Alle drei zusammen ermöglichen Bewegungskoordination ohne visuelle Kontrolle.",
        difficulty = 2,
        funFact = "Der Propriozeptionssinn fehlt beim Romberg-Test: Ohne visuelle und vestibuläre Kontrolle fallen Menschen mit propriozeptiven Störungen sofort um."
    ),

    // ── MEDICAL IMAGING ───────────────────────────────────────────────────────

    // Question 16 – Medical Imaging: X-ray vs CT
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Was ist der wesentliche Unterschied zwischen einem konventionellen Röntgenbild und einer Computertomographie (CT)?",
        answerA = "Ein Röntgenbild ist eine 2D-Summationsaufnahme aller Gewebeschichten; eine CT erstellt durch viele Winkelaufnahmen und Computerrekonstruktion dreidimensionale Schnittbilder",
        answerB = "Röntgen nutzt Gammastrahlen, CT verwendet Röntgenstrahlen",
        answerC = "CT ist strahlungsfrei, da nur Magnetfelder verwendet werden",
        answerD = "Röntgen erfasst nur Knochen, CT kann auch Weichteile darstellen, weil Kontrastmittel zwingend ist",
        correctAnswer = 0, // A
        explanation = "Ein konventionelles Röntgenbild projiziert alle Strukturen auf eine einzige Ebene (Summationsbild). Die CT (entwickelt von Hounsfield und Cormack, Nobelpreis 1979) rotiert Röntgenröhre und Detektor um den Patienten und nimmt aus hunderten von Winkeln Projektionen auf. Ein Algorithmus (gefilterte Rückprojektion oder iterative Rekonstruktion) berechnet daraus dreidimensionale Schnittbilder mit Hounsfield-Einheiten für jeden Voxel.",
        difficulty = 2,
        funFact = null
    ),

    // Question 17 – Medical Imaging: MRI Basics
    // Correct answer → position B (correctAnswer = 1)
    Question(
        categoryId = 2,
        questionText = "Welches physikalische Prinzip liegt der Magnetresonanztomographie (MRT) zugrunde?",
        answerA = "Röntgenstrahlen werden von Wasser unterschiedlich absorbiert",
        answerB = "Protonen im Körper werden durch ein starkes Magnetfeld ausgerichtet und durch Hochfrequenzpulse zur Resonanz gebracht; beim Relaxieren emittieren sie Radiosignale",
        answerC = "Radioaktive Tracer senden Positronen aus, die im Gewebe annihilieren",
        answerD = "Ultraschallwellen werden an Gewebegrenzen reflektiert und die Laufzeit gemessen",
        correctAnswer = 1, // B
        explanation = "Im MRT werden die Protonen (¹H) des Körperwassers durch ein starkes Magnetfeld (1,5–3 Tesla) ausgerichtet. Ein Hochfrequenzpuls kippt die Magnetisierung aus dem Gleichgewicht (Resonanz). Beim Zurückrelaxieren in die Gleichgewichtslage emittieren die Protonen ein Radiosignal (FID – Free Induction Decay), das empfangen und zu einem Bild rekonstruiert wird. T1- und T2-Relaxationszeiten variieren zwischen Geweben und erzeugen den Kontrast.",
        difficulty = 2,
        funFact = "Kein Röntgen, kein Radionuklid: Das MRT ist für den Patienten strahlungsfrei. Die Magnetfelder gelten bei korrekter Anwendung als biologisch unbedenklich."
    ),

    // Question 18 – Medical Imaging: Ultrasound Principles
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Welches Prinzip nutzt der medizinische Ultraschall zur Bildgebung?",
        answerA = "Infrarotstrahlung wird von Körperwärme absorbiert und in ein Temperaturbild umgewandelt",
        answerB = "Elektrische Impulse werden durch den Körper geleitet und die Impedanzunterschiede gemessen",
        answerC = "Hochfrequente Schallwellen (2–15 MHz) werden von einem piezoelektrischen Kristall ausgesandt, an Gewebegrenzen reflektiert und die Echos zeitlich erfasst",
        answerD = "Terahertz-Strahlung durchdringt Weichteile und wird von Knochen vollständig reflektiert",
        correctAnswer = 2, // C
        explanation = "Medizinischer Ultraschall nutzt den Piezoeffekt: Ein piezoelektrischer Kristall (z. B. PZT) sendet Schallpulse im MHz-Bereich aus, die Gewebe unterschiedlich reflektieren (Impedanzunterschiede). Die Echos kehren zum Transducer zurück, der sie wieder in elektrische Signale umwandelt. Aus der Laufzeit (Echzeit) und Schallgeschwindigkeit im Gewebe (~1540 m/s) wird die Tiefe berechnet (A/B/M-Mode, Doppler).",
        difficulty = 2,
        funFact = null
    ),

    // Question 19 – Medical Imaging: PET Basics
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Was wird bei einer Positronen-Emissions-Tomographie (PET) gemessen?",
        answerA = "Die Dichte von Knochen und Kalk durch hochenergetische Röntgenstrahlung",
        answerB = "Die elektrische Aktivität des Herzens durch Elektrodenfelder auf der Körperoberfläche",
        answerC = "Die Bewegung von Blut durch Magnetresonanz-Phasenverschiebung",
        answerD = "Die Verteilung eines radioaktiven Tracers (z. B. ¹⁸F-FDG) im Körper durch Detektion der Vernichtungsstrahlung von Positronen",
        correctAnswer = 3, // D
        explanation = "Bei der PET wird ein positronenemittierender Tracer (häufig ¹⁸F-Fluordesoxyglukose, FDG) injiziert. Die Positronen annihilieren sofort mit Elektronen und erzeugen zwei gegenläufige 511-keV-Gammastrahlen (Koinzidenzdetektion). Da Tumore und entzündliche Gewebe mehr Glucose aufnehmen, reichert FDG dort an – die PET zeigt also funktionelle Stoffwechselaktivität statt nur Anatomie.",
        difficulty = 2,
        funFact = "PET/CT-Kombisysteme vereinen Stoffwechselbild (PET) und Anatomie (CT) in einem Scan – revolutionär für die Krebsdiagnostik und Therapiekontrolle."
    ),

    // Question 20 – Medical Imaging: Endoscopy
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Wie wird das Bild bei modernen Videoendoskopen vom Körperinneren zum Arzt übertragen?",
        answerA = "Durch einen CCD- oder CMOS-Chip an der Endoskopspitze, der das Bild direkt digital erfasst und über einen Signalkabel überträgt",
        answerB = "Durch ein Bündel von Glasfasern, die Licht durch Totalreflexion transportieren (faseroptisch)",
        answerC = "Durch Infrarot-Triangulation, bei der drei Sensoren die Position berechnen",
        answerD = "Durch Miniaturspiegel, die das Bild durch den Endoskopkanal reflektieren",
        correctAnswer = 0, // A
        explanation = "Moderne Videoendoskope (seit den 1980ern) haben einen miniaturisierten CCD- oder CMOS-Bildsensor direkt an der Spitze. Das Bild wird digital kodiert und über Kabel zu einem Videoprozessor geleitet, der es auf dem Monitor darstellt. Ältere Faseroptikendoskope transportierten das Bild dagegen über kohärente Glasfaserbündel – dabei wird das Bild in die Pixelauflösung der einzelnen Fasern aufgeteilt.",
        difficulty = 2,
        funFact = null
    ),

    // ── AGRICULTURE SCIENCE ───────────────────────────────────────────────────

    // Question 21 – Agriculture: Crop Rotation
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Warum verbessert die Fruchtfolge mit Leguminosen die Bodenqualität für nachfolgende Kulturen?",
        answerA = "Leguminosen wurzeln besonders tief und lockern den Boden mechanisch auf",
        answerB = "Die Blätter der Leguminosen enthalten viel Kalium, das beim Verrotten freigesetzt wird",
        answerC = "Leguminosen leben in Symbiose mit Rhizobium-Bakterien, die atmosphärischen Stickstoff binden und dem Boden in pflanzenaufnehmbarer Form hinterlassen",
        answerD = "Leguminosen produzieren natürliche Fungizide, die schädliche Bodenpilze abtöten",
        correctAnswer = 2, // C
        explanation = "Leguminosen (Hülsenfrüchte wie Klee, Erbsen, Soja) leben in Wurzelsymbiose mit Rhizobium-Bakterien. Diese Bakterien besitzen das Enzym Nitrogenase, das N₂ aus der Luft zu Ammonium (NH₄⁺) fixiert – biologische Stickstofffixierung. Wenn Leguminosen absterben oder umgearbeitet werden, verbleibt gebundener Stickstoff im Boden und steht Folgekulturen als natürlicher Dünger zur Verfügung.",
        difficulty = 2,
        funFact = "Eine gut etablierte Rotklee-Kultur kann 200–300 kg Stickstoff pro Hektar und Jahr fixieren – das entspricht etwa 3 Säcken Harnstoffdünger."
    ),

    // Question 22 – Agriculture: Nitrogen Fixation
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Welches Enzym katalysiert die biologische Stickstofffixierung, und welche Bedingung ist dafür zwingend notwendig?",
        answerA = "Nitratreduktase; benötigt viel Licht für die Photolyse von N₂",
        answerB = "Glutaminase; benötigt hohe Temperaturen über 40 °C",
        answerC = "Urease; benötigt eine saure Umgebung mit pH unter 5",
        answerD = "Nitrogenase; benötigt strikt anaerobe Bedingungen, da das Enzym durch Sauerstoff irreversibel inaktiviert wird",
        correctAnswer = 3, // D
        explanation = "Das Enzymkomplex Nitrogenase (bestehend aus Dinitrogenase und Dinitrogenase-Reduktase) katalysiert: N₂ + 8H⁺ + 8e⁻ + 16 ATP → 2NH₃ + H₂ + 16 ADP + 16 Pᵢ. Nitrogenase ist extrem O₂-empfindlich. Rhizobien schützen das Enzym durch Leghämoglobin in den Wurzelknöllchen, das Sauerstoff bindet aber gleichzeitig geregelt zuführt.",
        difficulty = 2,
        funFact = null
    ),

    // Question 23 – Agriculture: GMOs
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Wie funktioniert das Bt-Toxin in genetisch veränderten Bt-Kulturpflanzen (z. B. Bt-Mais)?",
        answerA = "Die Pflanze produziert ein Protein aus Bacillus thuringiensis, das im alkalischen Darm bestimmter Insektenlarven zu einem Toxin aktiviert wird und deren Darmzellen zerstört",
        answerB = "Das Bt-Toxin ist ein systemisches Herbizid, das nur in Unkräutern wirkt, nicht in Nutzpflanzen",
        answerC = "Das Bt-Toxin ist ein Pheromon, das Schädlinge von der Pflanze fernhält",
        answerD = "Das Bt-Toxin verändert den Cholesterinstoffwechsel der Insekten und macht sie unfruchtbar",
        correctAnswer = 0, // A
        explanation = "Bt-Pflanzen tragen Gene für Cry-Proteine (Kristallproteine) aus dem Bodenbakterium Bacillus thuringiensis. Im alkalischen Darm von Schmetterlingsraupen oder Käferlarven werden die Cry-Protoxine proteolytisch zu aktiven Toxinen gespalten. Diese binden an spezifische Rezeptoren im Darmepithel, bilden Poren in der Membran und verursachen osmotischen Zelltod. Da menschliche Darmzellen diese Rezeptoren nicht besitzen und der Magen-pH zu sauer ist, sind Bt-Toxine für Menschen unbedenklich.",
        difficulty = 2,
        funFact = "Bt-Sprühmittel werden im konventionellen und biologischen Landbau schon seit den 1950ern eingesetzt – Bt-Mais macht dasselbe Toxin nur pflanzeneigen."
    ),

    // Question 24 – Agriculture: Pesticide Types
    // Correct answer → position B (correctAnswer = 1)
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet systemische Pestizide von Kontaktpestiziden?",
        answerA = "Systemische Pestizide töten Schädlinge beim direkten Kontakt, Kontaktpestizide müssen aufgenommen werden",
        answerB = "Systemische Pestizide werden von der Pflanze aufgenommen und im Leitungssystem verteilt, sodass auch saugende Insekten wie Blattläuse betroffen sind; Kontaktpestizide wirken nur bei direktem Kontakt",
        answerC = "Systemische Pestizide sind ausschließlich für den Boden, Kontaktpestizide nur für Blattanwendungen",
        answerD = "Systemische Pestizide sind wasserlöslich, Kontaktpestizide sind fettlöslich",
        correctAnswer = 1, // B
        explanation = "Systemische Pestizide (z. B. Neonicotinoide wie Imidacloprid) werden von der Pflanze über Wurzeln oder Blätter aufgenommen und durch das Phloem und Xylem in alle Pflanzenteile transportiert. Auch Insekten, die Pflanzensaft saugen oder Blätter von innen fressen, werden exponiert. Kontaktpestizide (z. B. Pyrethrine) wirken nur dort, wo sie direkten Kontakt mit dem Schädling haben.",
        difficulty = 2,
        funFact = null
    ),

    // Question 25 – Agriculture: Soil pH
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Warum ist der Boden-pH-Wert so wichtig für die Nährstoffverfügbarkeit von Pflanzen?",
        answerA = "Bei extremen pH-Werten lösen sich alle Nährstoffe besser auf und sind daher besser verfügbar",
        answerB = "Nur der Phosphatgehalt ist pH-abhängig; alle anderen Nährstoffe sind pH-unabhängig",
        answerC = "Der pH bestimmt die Löslichkeit und Ionenform der Nährstoffe; bei zu saurem Boden werden giftige Aluminium- und Mangan-Ionen freigesetzt, bei zu alkalischem Boden werden Eisen, Mangan und Bor unlöslich",
        answerD = "Der pH reguliert ausschließlich die Bodentemperatur, was die Nährstoffaufnahme beeinflusst",
        correctAnswer = 2, // C
        explanation = "Der optimale pH für die meisten Nutzpflanzen liegt bei 6,0–7,0. Bei pH < 5,5 werden Al³⁺ und Mn²⁺ toxisch löslich und hemmen Wurzelwachstum. Bei pH > 7,5 fällt Fe²⁺ als Fe(OH)₃ aus (Chlorose durch Eisenmangel), Phosphat wird als Ca₃(PO₄)₂ immobilisiert und Bor wird unlöslich. Bodenkalkung erhöht den pH saurer Böden, Schwefel oder organische Säuren senken ihn bei alkalischen Böden.",
        difficulty = 2,
        funFact = "Heidelbeeren benötigen einen sehr sauren Boden (pH 4–5), weil ihre Mykorrhiza-Pilze nur in diesem Bereich funktionieren."
    ),

    // ── WATER CHEMISTRY ───────────────────────────────────────────────────────

    // Question 26 – Water Chemistry: Water Hardness
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter Wasserhärte, und welche Ionen sind dafür verantwortlich?",
        answerA = "Den Chloridgehalt des Wassers, verursacht durch NaCl aus dem Boden",
        answerB = "Den Fluoridgehalt des Wassers aus natürlichen Mineralquellen",
        answerC = "Den Gehalt an gelöstem Sauerstoff, der die Korrosion von Rohrleitungen bestimmt",
        answerD = "Die Konzentration von gelöstem Kalzium (Ca²⁺) und Magnesium (Mg²⁺), die Kalkablagerungen in Rohren und Geräten verursachen",
        correctAnswer = 3, // D
        explanation = "Wasserhärte wird durch Kalzium- (Ca²⁺) und Magnesiumionen (Mg²⁺) verursacht, die aus kalkhaltigen Böden ins Grundwasser gelangen. Bei Erwärmung fällt das gelöste CaHCO₃ als CaCO₃ (Kalk/Kesselstein) aus: Ca²⁺ + 2HCO₃⁻ → CaCO₃↓ + H₂O + CO₂. Hartes Wasser (>14 °dH) bildet mehr Ablagerungen, ist aber mineralreicher.",
        difficulty = 2,
        funFact = null
    ),

    // Question 27 – Water Chemistry: Chlorination
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Wie tötet Chlor im Trinkwasser Mikroorganismen ab?",
        answerA = "Hypochlorige Säure (HOCl), die aus Chlor gebildet wird, diffundiert durch Zellmembranen und oxidiert lebenswichtige Enzyme und DNA-Bestandteile der Mikroorganismen",
        answerB = "Chlor erhöht den pH-Wert des Wassers, was die Bakterienzellwände alkalisch denaturiert",
        answerC = "Chlor bindet an das Chlorophyll der Algen und verhindert Photosynthese",
        answerD = "Chlorgas ist schwerer als Wasser und lagert sich am Boden ab, wo es Bodenkeime abtötet",
        correctAnswer = 0, // A
        explanation = "Chlor reagiert mit Wasser zu hypochloriger Säure: Cl₂ + H₂O → HOCl + HCl. HOCl (und in geringerem Maß OCl⁻) ist der eigentliche Wirkstoff: Als neutrale Säure diffundiert HOCl leicht durch Zellmembranen und oxidiert dort SH-Gruppen von Enzymen (z. B. in der Atmungskette), schädigt die DNA und verursacht oxidativen Stress, der zur Inaktivierung der Mikroorganismen führt.",
        difficulty = 2,
        funFact = "Trihalomethane (z. B. Chloroform) entstehen als Nebenprodukte, wenn Chlor mit natürlicher organischer Materie reagiert – weshalb Trinkwasser vor der Chlorierung gefiltert wird."
    ),

    // Question 28 – Water Chemistry: Fluoridation
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Wie schützt Fluorid im Trinkwasser vor Karies?",
        answerA = "Fluorid tötet direkt das Bakterium Streptococcus mutans im Mund ab",
        answerB = "Fluorid puffert die Säure im Mund und neutralisiert Bakteriensäuren direkt",
        answerC = "Fluorid wird in den Zahnschmelz eingebaut und bildet Fluorapatit, das säureresistenter ist als natürlicher Hydroxylapatit",
        answerD = "Fluorid verhindert die Zuckeraufnahme durch Mundschleimhautzellen und reduziert so die Nährstoffbasis der Kariesbakterien",
        correctAnswer = 2, // C
        explanation = "Fluorid (F⁻) substituiert Hydroxylgruppen im Hydroxylapatit [Ca₁₀(PO₄)₆(OH)₂] und bildet Fluorapatit [Ca₁₀(PO₄)₆F₂]. Fluorapatit ist säureresistenter (löst sich erst bei pH < 4,5, Hydroxylapatit schon bei pH < 5,5) und hemmt die Demineralisation durch Bakteriensäuren. Fluorid fördert auch die Remineralisation beschädigten Schmelzes.",
        difficulty = 2,
        funFact = null
    ),

    // Question 29 – Water Chemistry: Desalination
    // Correct answer → position B (correctAnswer = 1)
    Question(
        categoryId = 2,
        questionText = "Welches Verfahren ist das weltweit am häufigsten eingesetzte zur Meerwasserentsalzung?",
        answerA = "Mehrstufige Flashverdampfung (MSF – Multi-Stage Flash Distillation)",
        answerB = "Umkehrosmose (Reverse Osmosis), bei der Hochdruck Wasser durch eine semipermeable Membran treibt und Salz zurückhält",
        answerC = "Elektrodialyse, bei der elektrische Felder Salzionen durch Ionenaustauschermembranen ziehen",
        answerD = "Gefrierentsalzung, bei der das Salzwasser langsam eingefroren und das salzfreie Eis abgetrennt wird",
        correctAnswer = 1, // B
        explanation = "Die Umkehrosmose (RO) hat die thermischen Verfahren (MSF) als dominantes Entsalzungsverfahren abgelöst und macht heute ~65 % der weltweiten Kapazität aus. Meerwasser wird mit 55–70 bar durch Polyamid-Dünnfilmmembranen gepresst, die Ionen (Na⁺, Cl⁻, etc.) zurückhalten. Der Trinkwasser-Anteil (Permeatlösung) hat typischerweise <500 mg/l TDS. Energieeffiziente Druckrückgewinnungssysteme senken den Verbrauch auf ca. 3–4 kWh/m³.",
        difficulty = 2,
        funFact = "Saudi-Arabien betreibt die größten Meerwasserentsalzungsanlagen der Welt und deckt über 70 % seines Trinkwasserbedarfs durch Entsalzung."
    ),

    // Question 30 – Water Chemistry: Water pH
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Warum ist destilliertes Wasser nicht neutral (pH 7,0), wenn es an Luft steht?",
        answerA = "Destilliertes Wasser nimmt Natriumionen aus Luftpartikeln auf und wird dadurch basisch",
        answerB = "Sauerstoff aus der Luft oxidiert Wassermoleküle und erzeugt dabei Protonen",
        answerC = "Stickstoff aus der Luft reagiert mit Wasser zu Salpetersäure und senkt den pH drastisch",
        answerD = "CO₂ aus der Luft löst sich im Wasser und bildet Kohlensäure (H₂CO₃), was den pH auf etwa 5,6 senkt",
        correctAnswer = 3, // D
        explanation = "Atmosph. CO₂ (~0,042 Vol-%) löst sich in Wasser nach: CO₂ + H₂O ⇌ H₂CO₃ ⇌ H⁺ + HCO₃⁻. Das Gleichgewicht stellt sich bei etwa pH 5,6 ein – das ist der natürliche pH-Wert von sauberem Regen. Destilliertes oder deionisiertes Wasser nimmt CO₂ innerhalb von Minuten auf. Erst unter vollständigem CO₂-Ausschluss (z. B. unter N₂-Atmosphäre) bleibt der pH bei 7,0.",
        difficulty = 2,
        funFact = "Saurer Regen durch industrielle SO₂- und NOₓ-Emissionen hat pH-Werte von 4,0–4,5, was zehnmal saurer ist als normaler Regen."
    ),

    // ── SLEEP SCIENCE ────────────────────────────────────────────────────────

    // Question 31 – Sleep Science: REM Sleep
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Was ist charakteristisch für den REM-Schlaf (Rapid Eye Movement)?",
        answerA = "Hohe Gehirnaktivität (ähnlich dem Wachzustand), lebhafte Träume und aktive Muskelatonie (motorische Lähmung)",
        answerB = "Maximale Muskelentspannung, sehr niedrige Gehirnaktivität und kaum Träume",
        answerC = "Tiefste Schlafphase mit langsamen Deltawellen und Ausschüttung von Wachstumshormon",
        answerD = "Kurze Phase direkt nach dem Einschlafen mit schnellen Schlafspindeln",
        correctAnswer = 0, // A
        explanation = "Im REM-Schlaf zeigt das EEG desynchronisierte, hochfrequente Wellen ähnlich dem Wachzustand. Gleichzeitig treten schnelle Augenbewegungen auf und der Körper ist durch aktive Hemmung motorischer Neuronen (durch Glycin und GABA) nahezu gelähmt – vermutlich um Traumhandlungen zu unterdrücken. Der REM-Schlaf ist essentiell für emotionale Gedächtnisverarbeitung und kreatives Denken.",
        difficulty = 2,
        funFact = null
    ),

    // Question 32 – Sleep Science: Circadian Rhythm
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Wo befindet sich die 'innere Uhr' des Menschen im Gehirn, und welcher externe Taktgeber synchronisiert sie täglich?",
        answerA = "Im Hippocampus; synchronisiert durch Körpertemperatur",
        answerB = "In der Amygdala; synchronisiert durch Mahlzeiten und Nahrungsaufnahme",
        answerC = "Im Nucleus suprachiasmaticus (SCN) des Hypothalamus; synchronisiert hauptsächlich durch Licht über die Retina",
        answerD = "Im Kleinhirn; synchronisiert durch Bewegung und Sport",
        correctAnswer = 2, // C
        explanation = "Der Nucleus suprachiasmaticus (SCN) im vorderen Hypothalamus ist der zentrale Schrittmacher des circadianen Systems. Er enthält eine molekulare Uhr aus transkriptionalen Rückkopplungsschleifen (CLOCK/BMAL1, PER/CRY). Licht – besonders blaues Licht (480 nm) – wird von intrinsisch photosensitiven Retinalganglionzellen (ipRGCs) mit Melanopsin wahrgenommen und über den Retinohypothalamus-Trakt direkt zum SCN geleitet.",
        difficulty = 2,
        funFact = "Blaues Licht von Smartphone-Bildschirmen unterdrückt die Melatoninproduktion besonders effektiv und kann den Schlaf-Wach-Rhythmus um bis zu 2 Stunden verschieben."
    ),

    // Question 33 – Sleep Science: Melatonin
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Wie wird Melatonin im Körper gebildet, und was reguliert seine Ausschüttung?",
        answerA = "Melatonin wird in der Nebennierenrinde aus Kortisol synthetisiert; seine Ausschüttung steigt bei körperlichem Stress",
        answerB = "Melatonin wird im Pankreas aus Insulin gebildet; niedrige Blutglukose triggert die Ausschüttung",
        answerC = "Melatonin wird in der Leber aus Tryptophan synthetisiert; die Ausschüttung wird durch Körpertemperatur gesteuert",
        answerD = "Melatonin wird in der Zirbeldrüse aus Serotonin synthetisiert; Dunkelheit aktiviert die Synthese, Licht hemmt sie",
        correctAnswer = 3, // D
        explanation = "Die Zirbeldrüse (Epiphyse) synthetisiert Melatonin aus Serotonin durch zwei Enzyme: Arylalkylamin-N-Acetyltransferase (AANAT) und Hydroxyindol-O-methyltransferase (HIOMT). Der SCN hemmt über einen sympathischen Pfad die AANAT bei Licht – daher steigt Melatonin nur bei Dunkelheit. Der Anstieg beginnt ca. 2 h vor der gewohnten Schlafzeit und fördert Schläfrigkeit.",
        difficulty = 2,
        funFact = null
    ),

    // Question 34 – Sleep Science: Sleep Stages
    // Correct answer → position B (correctAnswer = 1)
    Question(
        categoryId = 2,
        questionText = "In welcher Schlafphase wird am meisten Wachstumshormon (Somatotropin) ausgeschüttet?",
        answerA = "In der REM-Phase, da der Körper in dieser Phase träumt und Energie benötigt",
        answerB = "In den Tiefschlafphasen (N3-Schlaf mit Slow-Wave-Sleep), kurz nach dem Einschlafen",
        answerC = "Direkt beim Einschlafen (Schlafstadium N1) während des Hypnagogen Zustands",
        answerD = "Gleichmäßig über alle Schlafphasen verteilt, ohne Unterschied",
        correctAnswer = 1, // B
        explanation = "Die stärkste Wachstumshormonpulsation erfolgt während des ersten Tiefschlafs (N3, Slow-Wave-Sleep) etwa 1–2 Stunden nach dem Einschlafen. Wachstumshormon fördert in dieser Phase Gewebereparatur, Muskelaufbau und Fettverstoffwechsel. Schlafentzug oder fragmentierter Schlaf reduziert diese Ausschüttung erheblich.",
        difficulty = 2,
        funFact = "Kinder und Jugendliche haben deutlich längere N3-Phasen als Erwachsene – ein Grund, warum ausreichend Schlaf für das Wachstum so wichtig ist."
    ),

    // Question 35 – Sleep Science: Sleep Deprivation Effects
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Welches Protein, das durch Schlaf aus dem Gehirn gespült wird, steht in Zusammenhang mit Alzheimer-Krankheit?",
        answerA = "Beta-Amyloid – während des Schlafs wird es durch das glymphatische System aus dem Gehirn gespült; Schlafmangel lässt es sich ansammeln",
        answerB = "Dopamin – der Neurotransmitter akkumuliert bei Schlafmangel und schädigt Synapsen",
        answerC = "Cortisol – erhöhte Stresshormone bei Schlafmangel greifen Neuronen direkt an",
        answerD = "Myelin – die Schutzschicht der Nervenfasern wird bei Schlafmangel abgebaut",
        correctAnswer = 0, // A
        explanation = "Das 2013 entdeckte glymphatische System (Maiken Nedergaard) ist ein Entsorgungskanal des Gehirns: Während des Tiefschlafs schrumpfen Gliazellen um ~60 %, der extrazelluläre Raum weitet sich aus, und Gehirn-Rückenmarks-Flüssigkeit (CSF) fließt entlang perivaskulärer Räume durch das Gewebe und spült Metabolite heraus – darunter Beta-Amyloid (Aβ) und Tau-Protein, die bei Alzheimer toxische Aggregate bilden.",
        difficulty = 2,
        funFact = "Studien zeigen, dass schon eine einzige Nacht Schlafentzug die Beta-Amyloid-Konzentration im Gehirn messbar erhöht."
    ),

    // ── GENETICS APPLICATIONS ────────────────────────────────────────────────

    // Question 36 – Genetics: Forensic DNA
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Welche DNA-Methode wird in der forensischen Spurenanalyse eingesetzt, und warum eignet sie sich besonders?",
        answerA = "Ganzgenomsequenzierung (WGS), weil sie das vollständige Erbgut eines Täters liefert",
        answerB = "FISH (Fluoreszenz-in-situ-Hybridisierung), weil sie Chromosomenaberrationen sichtbar macht",
        answerC = "STR-Analyse (Short Tandem Repeat Profiling), weil diese hochvariablen, nicht-kodierenden Wiederholungssequenzen individuelle Muster liefern und aus kleinen, degradierten Proben amplifiziert werden können",
        answerD = "Southernblot-Hybridisierung, weil sie große DNA-Fragmente besonders präzise aufrennt",
        correctAnswer = 2, // C
        explanation = "Die forensische DNA-Analyse nutzt STR-Profile (Short Tandem Repeats) – kurze, sich tandemförmig wiederholende DNA-Sequenzen (z. B. AGAT-AGAT-AGAT-...). Anzahl der Wiederholungen variiert zwischen Individuen stark. Nach PCR-Amplifikation werden Längen mehrerer STR-Loci per Kapillarelektrophorese bestimmt. Kombinationen von 15–20 STR-Loci ergeben eine nahezu einzigartige genetische Signatur. Aus winzigen Proben (1–2 ng DNA) kann ein vollständiges Profil erstellt werden.",
        difficulty = 2,
        funFact = null
    ),

    // Question 37 – Genetics: Paternity Testing
    // Correct answer → position B (correctAnswer = 1)
    Question(
        categoryId = 2,
        questionText = "Auf welchem genetischen Prinzip basiert ein Vaterschaftstest?",
        answerA = "Mitochondriale DNA, die nur von der Mutter vererbt wird, wird verglichen",
        answerB = "Jedes Kind erhält für jeden genetischen Locus je ein Allel von Vater und Mutter; stimmt kein väterliches Allel des Kindes mit dem potenziellen Vater überein, ist Vaterschaft ausgeschlossen",
        answerC = "Y-Chromosom-Sequenzen werden direkt zwischen Vater und Kind verglichen",
        answerD = "Blutgruppen-Antigene werden genutzt, da sie strikt nach Mendel vererbt werden",
        correctAnswer = 1, // B
        explanation = "Bei einem STR-basierten Vaterschaftstest werden die Allele von Kind, Mutter und potenziellem Vater an 15–20 Loci verglichen. Da das Kind je ein Allel von jedem Elternteil erhält, muss man das mütterliche Allel herausrechnen – das verbleibende Allel muss mit dem potenziellen Vater übereinstimmen. Ein einziger Ausschluss reicht für den biologischen Ausschluss. Übereinstimmungen an allen Loci ergeben eine Vaterschaftswahrscheinlichkeit von >99,999 %.",
        difficulty = 2,
        funFact = "Vaterschaft kann nie zu 100 % 'bewiesen' werden – nur ausgeschlossen. Statistisch gesehen kann die Wahrscheinlichkeit aber auf 99,9999 % steigen."
    ),

    // Question 38 – Genetics: Genetic Diseases
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet autosomal-dominante von autosomal-rezessiven Erbkrankheiten im Vererbungsmuster?",
        answerA = "Dominante Erkrankungen betreffen nur Männer, rezessive nur Frauen",
        answerB = "Dominante Erkrankungen werden nur über die mütterliche Linie vererbt",
        answerC = "Bei rezessiven Erkrankungen sind die Gene auf dem X-Chromosom lokalisiert",
        answerD = "Bei dominanten Erkrankungen reicht ein einziges verändertes Allel aus, um die Krankheit auszulösen; bei rezessiven müssen beide Allele betroffen sein",
        correctAnswer = 3, // D
        explanation = "Autosomal-dominant: Ein mutiertes Allel reicht aus (Heterozygote erkranken). Beispiel: Huntington-Krankheit (HTT-Gen). Statistisch: 50 % der Nachkommen eines betroffenen Elternteils erkranken. Autosomal-rezessiv: Beide Allele müssen mutiert sein (nur Homozygote erkranken, Heterozygote sind gesunde Träger). Beispiel: Mukoviszidose (CFTR-Gen). Statistisch: 25 % der Nachkommen zweier Träger erkranken.",
        difficulty = 2,
        funFact = null
    ),

    // Question 39 – Genetics: Gene Therapy Basics
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen somatischer Gentherapie und Keimbahngentherapie?",
        answerA = "Somatische Gentherapie verändert nur Körperzellen des Patienten (nicht vererbt); Keimbahntherapie verändert Ei-, Spermien- oder Embryonalzellen (wird vererbt)",
        answerB = "Somatische Gentherapie funktioniert nur bei Erwachsenen, Keimbahntherapie nur bei Kindern",
        answerC = "Somatische Gentherapie nutzt Viren, Keimbahntherapie nutzt ausschließlich chemische Methoden",
        answerD = "Somatische Gentherapie ersetzt defekte Gene vollständig, Keimbahntherapie repariert sie nur teilweise",
        correctAnswer = 0, // A
        explanation = "Somatische Gentherapie (z. B. CAR-T-Zelltherapie, AAV-Gentherapien wie Zolgensma bei SMA) behandelt erkrankte Körperzellen – die Veränderungen gehen mit dem Patienten 'verloren' und werden nicht vererbt. Keimbahninterventionen verändern Reproduktionszellen und werden an alle Nachkommen weitergegeben. Keimbahntherapien sind in fast allen Ländern verboten – mit Ausnahme des berühmten/berüchtigten Falls von He Jiankui (2018).",
        difficulty = 2,
        funFact = "Die erste zugelassene somatische Gentherapie weltweit war Glybera (2012) gegen Lipoproteinlipase-Defizienz – mit einem Preis von ca. 1 Million Euro pro Behandlung."
    ),

    // Question 40 – Genetics: Pharmacogenomics
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Was untersucht die Pharmakogenomik, und welches praktische Beispiel veranschaulicht ihre Bedeutung?",
        answerA = "Die Wirkung von Pflanzenextrakten auf genetische Mutationen; Beispiel: Kurkumin hemmt bestimmte Krebsgene",
        answerB = "Die Herstellung von Medikamenten aus genetisch veränderten Organismen; Beispiel: Insulin aus E. coli",
        answerC = "Den Einfluss genetischer Variationen auf die individuelle Reaktion auf Medikamente; Beispiel: CYP2D6-Polymorphismen bestimmen, wie schnell Codein zu Morphin metabolisiert wird",
        answerD = "Die Wirkung von Medikamenten auf die DNA-Methylierung; Beispiel: Aspirin verändert epigenetische Muster",
        correctAnswer = 2, // C
        explanation = "Pharmakogenomik untersucht, wie genetische Varianten (SNPs, Kopienzahlvarianten) die Pharmakokinetik und Pharmakodynamik beeinflussen. Das CYP2D6-Enzym metabolisiert >25 % aller Medikamente. 'Poor Metabolizer' (fehlende CYP2D6-Aktivität) können Codein nicht in Morphin umwandeln (kein Schmerzmitteleffekt). 'Ultrarapid Metabolizer' (mehrere Genkopien) produzieren zu viel Morphin – lebensgefährlich für Kinder und Stillende.",
        difficulty = 2,
        funFact = null
    ),

    // ── ENVIRONMENTAL SCIENCE ─────────────────────────────────────────────────

    // Question 41 – Environmental Science: Carbon Footprint
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet den direkten vom indirekten CO₂-Fußabdruck einer Person?",
        answerA = "Der direkte Fußabdruck umfasst nur Flugreisen, der indirekte nur Lebensmittel",
        answerB = "Der direkte Fußabdruck ist immer größer als der indirekte Fußabdruck",
        answerC = "Nur der direkte Fußabdruck wird in internationalen Klimaverträgen berücksichtigt",
        answerD = "Der direkte Fußabdruck umfasst persönliche Emissionen (Heizen, Auto, Flüge); der indirekte umfasst Emissionen, die durch die Produktion gekaufter Güter und Dienstleistungen entstehen",
        correctAnswer = 3, // D
        explanation = "Der direkte (Scope 1) CO₂-Fußabdruck umfasst Emissionen, die direkt durch eine Person verursacht werden: Autofahren, Heizöl, Erdgas. Der indirekte Fußabdruck (Scope 2+3) umfasst: Emissionen aus der Stromerzeugung (Scope 2) und alle Emissionen in der Wertschöpfungskette konsumierten Güter (Scope 3) – z. B. Produktion eines Smartphones (~70 kg CO₂) oder 1 kg Rindfleisch (~27 kg CO₂eq).",
        difficulty = 2,
        funFact = "Durchschnittlich 70 % des persönlichen CO₂-Fußabdrucks entfallen in Deutschland auf Konsum, Ernährung und Wohnen – nicht auf individuelle Mobilität."
    ),

    // Question 42 – Environmental Science: Greenhouse Effect Details
    // Correct answer → position B (correctAnswer = 1)
    Question(
        categoryId = 2,
        questionText = "Warum ist Methan (CH₄) ein deutlich stärkeres Treibhausgas als CO₂, obwohl seine atmosphärische Konzentration viel niedriger ist?",
        answerA = "Methan ist schwerer als CO₂ und lagert sich in Bodennähe ab, wo es Wärme direkt reflektiert",
        answerB = "Methan hat ein Global Warming Potential (GWP) von ca. 84 über 20 Jahre, weil es Infrarotstrahlung in anderen Wellenlängenbereichen absorbiert als CO₂ und eine kürzere atmosphärische Lebensdauer hat",
        answerC = "Methan reagiert mit Ozon und verstärkt dadurch den Ozonabbau, der seinerseits die Temperatur erhöht",
        answerD = "Methan reflektiert solare UV-Strahlung zurück und verhindert das Abkühlen der Stratosphäre",
        correctAnswer = 1, // B
        explanation = "Methan hat über 20 Jahre ein GWP100 von ~84, über 100 Jahre ca. 28 (im Vergleich zu CO₂ = 1). Die stärkere Wirkung pro Molekül liegt an der größeren IR-Absorptionsbandbreite und der Tatsache, dass die CO₂-Absorptionsbanden bereits weitgehend gesättigt sind, CH₄-Banden dagegen noch nicht. Die kürzere Verweilzeit in der Atmosphäre (~12 Jahre vs. Jahrhunderte für CO₂) erklärt das niedrigere 100-Jahres-GWP.",
        difficulty = 2,
        funFact = null
    ),

    // Question 43 – Environmental Science: Ocean Pollution
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Was sind Mikroplastik-Partikel, und wie gelangen sie in marine Nahrungsketten?",
        answerA = "Mikroplastik sind Plastikpartikel < 5 mm (primär aus Industrie; sekundär durch UV- und mechanischen Abbau größerer Teile); sie werden von Zooplankton und Filterfressern aufgenommen und akkumulieren in der Nahrungskette",
        answerB = "Mikroplastik sind Plastiksplitter > 5 mm, die direkt von Fischen als Nahrung aufgenommen werden",
        answerC = "Mikroplastik entsteht ausschließlich aus Kosmetikprodukten und gelangt über Abwasser ins Meer",
        answerD = "Mikroplastik sind biologisch abbaubare Polymere, die sich im Ozean innerhalb von Wochen zersetzen",
        correctAnswer = 0, // A
        explanation = "Mikroplastik (<5 mm) entsteht primär als industrielle Pellets, Fasern (Synthetik-Textilien), und Mikrokügelchen (Kosmetik) oder sekundär durch den Abbau größerer Plastikteile unter UV-Strahlung, Wellen und Temperaturwechseln. Im Ozean nehmen Zooplankton, Muscheln und Filterfische die Partikel auf – sie können nicht mehr ausgeschieden werden, akkumulieren und werden über die Nahrungskette weitergegeben (biologische Magnifikation).",
        difficulty = 2,
        funFact = "Mikroplastik wurde bereits in der Tiefsee (Marianengraben, >10 km), in Polareis, menschlichem Blut und Plazentagewebe nachgewiesen."
    ),

    // Question 44 – Environmental Science: Biodegradable Materials
    // Correct answer → position C (correctAnswer = 2)
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet 'biologisch abbaubar' (biodegradable) von 'kompostierbar' (compostable) bei Kunststoffen?",
        answerA = "Beide Begriffe sind synonym und beschreiben Materialien, die sich innerhalb eines Jahres vollständig zersetzen",
        answerB = "Kompostierbar bedeutet nur, dass das Material pflanzlichen Ursprungs ist; biologisch abbaubar bezieht sich auf Kunststoffe",
        answerC = "Biologisch abbaubar bedeutet, dass sich ein Material irgendwann durch Mikroorganismen zersetzt (kann Jahrzehnte dauern); kompostierbar bedeutet vollständige Zersetzung unter definierten Bedingungen (Temperatur, Feuchtigkeit) innerhalb von 12 Wochen ohne schädliche Rückstände",
        answerD = "Der Unterschied betrifft nur die Zertifizierung: biologisch abbaubar ist eine Industriekennzeichnung, kompostierbar ist ein staatliches Gütesiegel",
        correctAnswer = 2, // C
        explanation = "Der Begriff 'biologisch abbaubar' ist ungeschützt und sagt nichts über den Zeitrahmen aus – konventionelles Plastik ist auch biologisch abbaubar, aber nach Jahrhunderten. 'Industriell kompostierbar' (EN 13432) bedeutet: >90 % Zerfall innerhalb von 12 Wochen bei 58 °C, ohne toxische Rückstände. Jedoch zersetzen sich diese Materialien im Hausgarten oder in der Natur oft nicht, da die nötigen Temperaturen nicht erreicht werden.",
        difficulty = 2,
        funFact = null
    ),

    // ── ACOUSTICS ────────────────────────────────────────────────────────────

    // Question 45 – Acoustics: Decibel Scale
    // Correct answer → position D (correctAnswer = 3)
    Question(
        categoryId = 2,
        questionText = "Warum verdoppelt eine Zunahme von 3 dB den Schallpegel subjektiv kaum, obwohl sie physikalisch einer Verdopplung der Schallintensität entspricht?",
        answerA = "Das menschliche Ohr ist so empfindlich, dass 3 dB-Schritte problemlos wahrgenommen werden",
        answerB = "3 dB entsprechen einer linearen Intensitätszunahme, die das Ohr direkt als Halbierung wahrnimmt",
        answerC = "Das Dezibel ist eine lineare Skala; 3 dB bedeuten 3 % mehr Lautstärke",
        answerD = "Das menschliche Gehör folgt einer logarithmischen Wahrnehmungskurve; eine wahrnehmbare Lautstärkeverdopplung erfordert eine Zunahme von etwa 10 dB (dem 10-fachen der Schallintensität)",
        correctAnswer = 3, // D
        explanation = "Das Dezibel (dB) ist eine logarithmische Skala: L = 10 · log₁₀(I/I₀). +3 dB = doppelte Intensität (Watt/m²). +10 dB = zehnfache Intensität. Das menschliche Gehör (Sone-Skala nach Stevens) empfindet +10 dB als doppelte Lautstärke. Eine Zunahme von 3 dB ist zwar wahrnehmbar (minimale wahrnehmbare Änderung ca. 1 dB), klingt aber nicht 'doppelt so laut'.",
        difficulty = 2,
        funFact = "Ein Kettensägengeräusch (110 dB) ist physikalisch 10 Milliarden Mal intensiver als die Hörschwelle (0 dB) – dank der logarithmischen Skala bleibt das handhabbar."
    ),

    // Question 46 – Acoustics: Doppler in Medicine
    // Correct answer → position A (correctAnswer = 0)
    Question(
        categoryId = 2,
        questionText = "Wie nutzt die medizinische Doppler-Sonographie den Doppler-Effekt zur Messung von Blutfluss?",
        answerA = "Ultraschallwellen werden von bewegten Erythrozyten reflektiert; je nach Bewegungsrichtung und -geschwindigkeit verschiebt sich die Frequenz des Echos nach oben (Annäherung) oder unten (Entfernung) – die Frequenzverschiebung ist proportional zur Fließgeschwindigkeit",
        answerB = "Ultraschallwellen werden von stillstehenden Blutzellen absorbiert und von fließenden Zellen reflektiert",
        answerC = "Das Gerät misst den Druckunterschied zwischen systolischer und diastolischer Phase direkt",
        answerD = "Infrarotlicht wird von Hämoglobin absorbiert und die Absorptionsstärke zeigt die Fließgeschwindigkeit",
        correctAnswer = 0, // A
        explanation = "Im Doppler-Ultraschall (gepulst, kontinuierlich oder Farb-Doppler) werden Schallwellen (2–10 MHz) gesendet, die von bewegten Erythrozyten zurückreflektiert werden. Die Doppler-Gleichung: Δf = 2 · f₀ · v · cos(θ) / c (v = Blutgeschwindigkeit, θ = Winkel zwischen Schallstrahl und Gefäß, c = Schallgeschwindigkeit im Gewebe). Die Frequenzverschiebung (Δf) wird in Blutflussgeschwindigkeit umgerechnet. Farbdoppler codiert Flussrichtung in Rot/Blau.",
        difficulty = 2,
        funFact = null
    ),

    // Question 47 – Acoustics: Noise Pollution
    // Correct answer → position B (correctAnswer = 1)
    Question(
        categoryId = 2,
        questionText = "Ab welchem Dauerschallpegel (Leq) gilt Umgebungslärm nach WHO-Empfehlungen als gesundheitsgefährdend, und welche physiologischen Auswirkungen treten bei chronischer Exposition auf?",
        answerA = "Ab 80 dB(A): nur Hörschäden durch direkte mechanische Schädigung der Haarzellen",
        answerB = "Ab 53–55 dB(A) Außenlärm tagsüber: Schlafstörungen, chronischer Stress mit erhöhten Kortisol- und Adrenalinspiegeln, erhöhtes Herzerkrankungsrisiko",
        answerC = "Erst ab 90 dB(A): kurzzeitige Taubheit ohne langfristige Folgen",
        answerD = "Ab 70 dB(A): ausschließlich psychische Beeinträchtigungen ohne körperliche Folgen",
        correctAnswer = 1, // B
        explanation = "Die WHO Environmental Noise Guidelines (2018) empfehlen, Straßenlärm unter 53 dB(A) Leq (Tag) und 45 dB(A) Lnight zu halten. Chronischer Lärm aktiviert dauerhaft das sympathische Nervensystem (Stressreaktion), erhöht Kortisol und Adrenalin, stört den Schlaf (ab 40 dB nachts) und erhöht das Risiko für arterielle Hypertonie und koronare Herzerkrankungen. In Europa sterben laut EEA schätzungsweise 48.000 Menschen jährlich vorzeitig an lärmbedingten Herzerkrankungen.",
        difficulty = 2,
        funFact = "Lärm ist nach Luftverschmutzung der zweitgrößte umweltbedingte Gesundheitsrisikofaktor in Europa – noch vor Wasserverschmutzung und Chemikalienbelastung."
    ),

    // ── ADDITIONAL MEDIUM QUESTIONS ──────────────────────────────────────────

    // Question — Immunsystem: Antikörper  [correct: C → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Das Immunsystem bildet Antikoerper, um Krankheitserreger zu bekaempfen. Wie funktioniert die Schluessel-Schloss-Spezifitaet von Antikoerpern, und welche Zellen produzieren sie?",
        answerA = "Antikoerper werden von Killerzellen (zytotoxischen T-Zellen) produziert und zerstoeren Bakterien durch direkte Zelllyse; ihre Spezifitaet entsteht durch zufaellige Genrekombination erst nach dem Kontakt mit dem Erreger",
        answerB = "Antikoerper sind kleine RNA-Molekuele, die von Makrophagen ins Blut abgegeben werden; sie binden an Virusoberflaechen und loesen die RNA-Interferenz aus, wodurch die Virusreplikation gestoppt wird",
        answerC = "Antikoerper sind Y-foermige Proteine, die von Plasmazellen (ausdifferenzierte B-Lymphozyten) produziert werden; jeder Antikoerper hat eine hochspezifische Bindungsstelle (Paratop), die genau zu einem Antigen-Epitop passt wie ein Schluessel zum Schloss",
        answerD = "Antikoerper entstehen in der Leber und werden permanent ohne Erregerkontakt produziert; sie sind unspezifisch und bekaempfen alle Fremdstoffe gleichermassen durch Agglomeration",
        correctAnswer = 2, // C
        explanation = "Antikoerper (Immunglobuline) sind Y-foermige Glykoproteine mit zwei identischen schweren und zwei leichten Ketten. Die variablen Regionen an den Armen des Y (Fab-Fragment) bilden das Paratop — eine einzigartige Bindungsstruktur, die nur an ein spezifisches Antigen-Epitop bindet. Plasmazellen, die aus aktivierten B-Lymphozyten entstehen, produzieren bis zu 2000 Antikoerper pro Sekunde. Durch somatische Hypermutation und klonale Selektion wird die Affinitaet im Verlauf der Immunreaktion optimiert (Affinitaetsreifung). Der Fc-Teil des Antikoerpers aktiviert Komplement und signalisiert Fresszellen.",
        difficulty = 2,
        funFact = "Das Immunsystem kann theoretisch ueber 10 hoch 18 verschiedene Antikoerper-Varianten erzeugen — mehr als es Atome im sichtbaren Universum gibt. Dies ermoeglicht es, auf praktisch jeden denkbaren Krankheitserreger zu reagieren, auch auf Substanzen, die in der Natur noch nie vorgekommen sind."
    ),

    // Question — Chemie: Periodensystem-Gruppen  [correct: A → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Edelgase (Gruppe 18 im Periodensystem) sind unter normalen Bedingungen chemisch extrem reaktionstraege. Was ist die chemische Erklaerung fuer ihre Reaktionstraegheit?",
        answerA = "Edelgase haben eine vollstaendig gefuellte aeusserste Elektronenschale (Oktettregel erfuellt), was ihnen maximale Stabilitaet verleiht — sie muessen keine Bindungen eingehen, um einen guenstigen Elektronenzustand zu erreichen",
        answerB = "Edelgase sind reaktionstraege, weil sie keine Elektronen besitzen; sie sind reine Protonenkerne ohne Elektronen-Huelle",
        answerC = "Edelgase sind radioaktiv und zerfallen zu schnell, um stabile Verbindungen zu bilden; die Reaktionstraegheit ist eine Folge ihrer Instabilitaet",
        answerD = "Edelgase reagieren bei Raumtemperatur nicht, weil ihre Atome zu gross sind, um sich anderen Atomen naeher als 1 nm zu naehern",
        correctAnswer = 0, // A
        explanation = "Edelgase (Helium, Neon, Argon, Krypton, Xenon, Radon) haben vollstaendig gefuellte Valenzelektronenschalen: Helium 2 Elektronen (K-Schale voll), die anderen je 8 Valenzelektronen (Oktettregel). Da chemische Bindungen entstehen, um durch Elektronen-Teilen oder -Uebertragen eine stabile Edelgaskonfiguration zu erreichen, haben Edelgase keinen 'Antrieb' zur Bindungsbildung. Tatsaechlich koennen schwerere Edelgase unter extremen Bedingungen Verbindungen eingehen: Xenonhexafluoroplatinate (XePtF6) wurde 1962 von Neil Bartlett synthetisiert.",
        difficulty = 2,
        funFact = null
    ),

    // Question — Geologie: Halbwertszeit und Radiometrische Datierung  [correct: D → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Wissenschaftler nutzen den radioaktiven Zerfall bestimmter Isotope zur Altersbestimmung von Gesteinen und organischen Materialien. Welche Aussage zur radiometrischen Datierung ist korrekt?",
        answerA = "Die Kohlenstoff-14-Methode (C-14) kann Gesteine bis 100 Millionen Jahre zuverlaessig datieren, weil C-14 eine extrem lange Halbwertszeit hat",
        answerB = "Radiometrische Datierung funktioniert nur bei Fossilien, weil nur organisches Material radioaktive Isotope enthaelt",
        answerC = "Die Uran-Blei-Methode misst, wie viel Uran aus einem Gestein herausgewaschen wurde; je weniger Uran noch vorhanden ist, desto aelter ist das Gestein",
        answerD = "Bei der C-14-Methode vergleicht man das Verhaeltnis von C-14 zu stabilem C-12 in einer Probe; da C-14 mit einer Halbwertszeit von ca. 5730 Jahren zerraellt, ist die Methode fuer organisches Material bis ca. 50.000 Jahre geeignet",
        correctAnswer = 3, // D
        explanation = "Radiokarbondatierung (C-14): Lebende Organismen nehmen C-14 (entsteht durch kosmische Strahlung in der Atmosphaere) im gleichen Verhaeltnis wie C-12 auf. Nach dem Tod wird kein neues C-14 aufgenommen, und der vorhandene Anteil zerraellt mit der Halbwertszeit von 5730 Jahren. Durch Messung des C-14/C-12-Verhaeltnisses laesst sich das Sterbedatum bestimmen — aber nur bis ca. 50.000 Jahre (dann ist der C-14-Anteil zu gering). Fuer aeltere Gesteine verwendet man Uran-Blei (Halbwertszeit ~4,5 Mrd. Jahre), Kalium-Argon oder Rubidium-Strontium.",
        difficulty = 2,
        funFact = "Mit der Uran-Blei-Methode wurden Zirkon-Kristalle in Westaustralien auf 4,4 Milliarden Jahre datiert — sie sind die aeltesten bekannten Minerale auf der Erde und fast so alt wie die Erde selbst (4,54 Mrd. Jahre)."
    ),

)
