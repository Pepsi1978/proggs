package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsExpert4(): List<Question> = listOf(

    // ── EXPERT (difficulty = 4) — Meeresbiologie & Ichthyologie ─────────────

    // Q1
    Question(
        categoryId = 9,
        questionText = "Welches Enzym katalysiert bei Chemosynthese-Bakterien hydrothermaler Quellen (z.B. Thiomicrospira crunogena) den ersten Schritt, bei dem H2S oxidiert wird?",
        answerA = "Rubisco (Ribulose-1,5-bisphosphat-Carboxylase)",
        answerB = "Sulfid-Chinon-Oxidoreduktase (SQR)",
        answerC = "ATP-Synthase",
        answerD = "Nitrit-Oxidoreduktase",
        correctAnswer = 1,
        explanation = "Die Sulfid-Chinon-Oxidoreduktase (SQR) katalysiert die Oxidation von H2S zu elementarem Schwefel und koppelt dabei Elektronen an den Chinon-Pool der Atmungskette. Dieser erste Schritt liefert die Elektronen fuer die chemolithotrophe Energiegewinnung.",
        difficulty = 4,
        funFact = null
    ),

    // Q2
    Question(
        categoryId = 9,
        questionText = "Auf welcher ungefaehren Frequenz senden Blauwal-Maennchen (Balaenoptera musculus) ihre typischen Langstrecken-D-Call-Gesaenge aus?",
        answerA = "20 Hz",
        answerB = "52 Hz",
        answerC = "180 Hz",
        answerD = "440 Hz",
        correctAnswer = 0,
        explanation = "Blauwal-Maennchen kommunizieren vorrangig im Infraschallbereich bei etwa 15-20 Hz. Dieser 20-Hz-Ruf ist ihr charakteristischster Langstreckenruf und kann im SOFAR-Kanal tausende Kilometer weit uebertragen werden.",
        difficulty = 4,
        funFact = "Der raeselhafte 52-Hz-Wal singt seit 1989 auf einer einzigartigen Frequenz, auf die kein anderer Wal antwortet. Er gilt als genetisch isoliertes Individuum oder als Hybride."
    ),

    // Q3
    Question(
        categoryId = 9,
        questionText = "Welches ist der primaere Neurotransmitter, der in Tintenfischen (Sepia officinalis) die radialen Muskelfasern der Chromatophoren zur Pigment-Expansion kontrahiert?",
        answerA = "Serotonin",
        answerB = "Dopamin",
        answerC = "Acetylcholin",
        answerD = "Glutamat",
        correctAnswer = 2,
        explanation = "Acetylcholin ist der Haupttransmitter an den neuromuskulaeren Synapsen der radialen Chromatophoren-Muskeln. Seine Ausschuettung fuehrt zur Kontraktion dieser Muskeln, was die Pigmentbeutel expandiert und die Farbe sichtbar macht.",
        difficulty = 4,
        funFact = "Ein Tintenfisch kann seine Haut in weniger als 200 Millisekunden komplett veraendern — schneller als jede bekannte Muskelreaktion bei Wirbeltieren."
    ),

    // Q4
    Question(
        categoryId = 9,
        questionText = "Welches Hexactinellid-Glasschwamm-Genus bildet die lebenden Riffe der Hecate Strait (Kanada) in 165-240 m Tiefe?",
        answerA = "Euplectella",
        answerB = "Aphrocallistes",
        answerC = "Farrea",
        answerD = "Rossella",
        correctAnswer = 1,
        explanation = "Aphrocallistes vastus bildet die Hauptmasse der lebenden Glasschwamm-Riffe der Hecate Strait. Diese bis zu 9.000 Jahre alten, bis zu 19 m hohen Riffe sind die einzigen bekannten lebenden Riffe dieser Art weltweit.",
        difficulty = 4,
        funFact = "Die Glasschwamm-Riffe der Hecate Strait stehen unter strengem Meeresschutz und sind ein einzigartiges Oekosystem, das vergleichbare Biodiversitaetsfunktion wie Korallenriffe hat."
    ),

    // Q5
    Question(
        categoryId = 9,
        questionText = "Welcher Ionenkanal in den Ampullen der Lorenzini von Haien ist primaer fuer die Detektion von DC-Feldern unter 1 Mikrovolt pro Zentimeter verantwortlich?",
        answerA = "Spannungsabhaengige Ca2+-Kanaele (Cav1.3)",
        answerB = "TRPV1-Kanaele",
        answerC = "KCNQ-Kanaele",
        answerD = "HCN-Kanaele (Hyperpolarization-activated cyclic nucleotide-gated)",
        correctAnswer = 3,
        explanation = "HCN-Kanaele (If-Kanaele) in den Ampullarzellen der Lorenzini reagieren auf minimale Spannungsaenderungen und erzeugen graduierte Rezeptorpotenziale. Ihre aussergewoehnliche Spannungsempfindlichkeit erklaert die extreme Elektrosensitivitaet der Haie.",
        difficulty = 4,
        funFact = "Der Grosse Weisse Hai besitzt etwa 1.500 Ampullen der Lorenzini und kann damit das elektrische Feld eines schlagenden Herzens auf 50 cm Distanz detektieren."
    ),

    // Q6
    Question(
        categoryId = 9,
        questionText = "Welche physiologische Kombination erklaert den Tauchweltrekord des Cuviers Schnabelwals (Ziphius cavirostris) auf 2.992 m Tiefe?",
        answerA = "Hyperventilation mit CO2-Retention",
        answerB = "Bradykardie, periphere Vasokonstriktion und Milzkontraktion als Erythrozyten-Reservoir",
        answerC = "Haemoglobin-Umschaltung auf fetales Haemoglobin",
        answerD = "Schwimmblasen-Kompression mit direktem Gasaustausch",
        correctAnswer = 1,
        explanation = "Das Tauchreflexsyndrom kombiniert: Diving Bradykardie auf 4-5 Schlaege/min, massive periphere Vasokonstriktion fuer Sauerstoffreservierung fuer Gehirn und Herz, Milzkontraktion als sauerstoffreiches Erythrozyten-Reservoir und Lungenkollaps zur Vermeidung von Stickstoff-Narkose.",
        difficulty = 4,
        funFact = "Cuviers Schnabelwal tauchte 2014 fuer 2 Stunden und 17 Minuten — absoluter Rekord unter allen Saeugetieren. Sein Myoglobin-Gehalt ist 10x hoeher als beim Menschen."
    ),

    // Q7
    Question(
        categoryId = 9,
        questionText = "Welche chemische Reaktionskette beschreibt die Ozeanversauerung durch anthropogenes CO2 und ihre Wirkung auf die Aragonit-Saettigung?",
        answerA = "CO2 + H2O bildet H2CO3, das in HCO3- und H+ dissoziiert; steigende H+-Konzentration senkt pH und verringert CO32--Konzentration",
        answerB = "CO2 + CaCO3 bildet direkt Ca2+ und 2HCO3-",
        answerC = "CO2 + OH- bildet HCO3- ohne pH-Veraenderung",
        answerD = "CO2 + H2O + CaO bildet CaCO3 und H2O",
        correctAnswer = 0,
        explanation = "CO2 loest sich in Meerwasser, bildet Kohlensaeure (H2CO3), die in HCO3- und H+ dissoziiert. Steigende H+-Konzentration senkt den pH und verschiebt das Karbonat-Gleichgewicht weg von CO32-, was die Aragonit-Saettigung senkt und Korallenorganismen beim Kalkschalenaufbau schaedigt.",
        difficulty = 4,
        funFact = "Seit Beginn der Industrialisierung ist der Ozean-pH von 8,2 auf 8,1 gesunken — das entspricht einer 30%igen Erhoehung der Protonenkonzentration."
    ),

    // Q8
    Question(
        categoryId = 9,
        questionText = "Welches Pigment ermoeglicht dem Tiefseefisch Malacosteus niger die Produktion von Rotlicht (700+ nm) fuer 'geheime' Kommunikation?",
        answerA = "Bacteriochlorophyll c als Fluorophor",
        answerB = "Chlorophyll-a-Derivate als Antennenpigment in Photophoren",
        answerC = "Phytocyanobilin als Cofaktor",
        answerD = "Kynurenin-basierte Oxidationsprodukte",
        correctAnswer = 1,
        explanation = "Malacosteus niger nutzt Chlorophyll-a-Derivate aus der Nahrung als Antennenpigmente in seinen Photophoren. Diese Molekule re-emittieren im Rotbereich (~700 nm) — eine einzigartige Adaptation fuer einen Kommunikationskanal, den andere Tiefseefische nicht wahrnehmen koennen.",
        difficulty = 4,
        funFact = null
    ),

    // Q9
    Question(
        categoryId = 9,
        questionText = "Welche Tiefe wird als Lysokline in tropischen Ozeanen definiert, unterhalb derer Kalkschalen bevorzugt aufgeloest werden?",
        answerA = "500-800 m",
        answerB = "3.500-4.500 m",
        answerC = "1.000-2.000 m",
        answerD = "6.000-7.000 m",
        correctAnswer = 1,
        explanation = "Die Lysokline liegt im Atlantik bei ca. 4.000-4.500 m und im Pazifik bei 3.500-4.000 m. Darueber liegt die Carbonate Compensation Depth (CCD), unterhalb derer sich kein Kalk mehr am Meeresgrund ablagert.",
        difficulty = 4,
        funFact = null
    ),

    // Q10
    Question(
        categoryId = 9,
        questionText = "Welches Phytoplankton-Genus produziert Domoinsaeure und ist fuer amnesische Schalentiervergiftung (ASP) verantwortlich?",
        answerA = "Karenia brevis",
        answerB = "Alexandrium tamarense",
        answerC = "Gymnodinium catenatum",
        answerD = "Pseudo-nitzschia",
        correctAnswer = 3,
        explanation = "Diatomeen der Gattung Pseudo-nitzschia (insbesondere P. australis und P. multiseries) produzieren Domoinsaeure, eine Aminosaeure die als Glutamat-Agonist an AMPA- und Kainat-Rezeptoren bindet und hippokampale Schaeden verursacht.",
        difficulty = 4,
        funFact = "Der Begriff amnesische Schalentiervergiftung entstand 1987, als 107 Menschen in Kanada nach dem Verzehr von Blaumuscheln erkrankten und mehrere dauerhafte Gedaechtnisschaeden erlitten."
    ),

    // Q11
    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus erklaert die O2-Anreicherung in der Wundernetz-Schwimmblase (Rete mirabile) pelagischer Fische?",
        answerA = "Bohr-Effekt verschiebt O2-Abgabe durch erhoehte CO2-Spannung allein",
        answerB = "Root-Effekt und Lactat-induzierte pH-Senkung konzentrieren O2 trotz Saettigung",
        answerC = "Myoglobin-Reservoir gibt O2 graduell ab",
        answerD = "Haemoglobin-Polymerisation bei niedrigem pH",
        correctAnswer = 1,
        explanation = "Der Root-Effekt (pH-bedingte maximale O2-Kapazitaetsreduktion des Haemoglobins) kombiniert mit Milchsaeure-Sekretion schafft einen O2-Gradienten, der Sauerstoff aus dem Blut in die Schwimmblase treibt — entgegen dem Konzentrationsgefaelle. Dies ermoeglicht O2-Partialdrucke von bis zu 200 atm.",
        difficulty = 4,
        funFact = null
    ),

    // Q12
    Question(
        categoryId = 9,
        questionText = "Welche Rezeptorzellen bilden die funktionelle Einheit des Seitenlinien-Organs bei Fischen?",
        answerA = "Haarzellen mit Kinocilium und Stereozilien-Buendel in Neuromasten, ueberdeckt von einer Gallert-Cupula",
        answerB = "Ampullaere Organe mit Typ-I und Typ-II Vestibularzellen",
        answerC = "Macula sacculi mit Otolithen-Kanaelen",
        answerD = "Semicircular canals mit Statocyst-Strukturen",
        correctAnswer = 0,
        explanation = "Das Seitenlinien-Organ besteht aus Neuromasten, in denen Haarzellen mit Kinocilium und Stereozilien-Buendel von einer Gallertmasse (Cupula) ueberdeckt werden. Wasserstroeme biegen die Cupula und depolarisieren oder hyperpolarisieren die Haarzellen.",
        difficulty = 4,
        funFact = "Blinde Hoehlenfische (Astyanax mexicanus) entwickelten vergroesserte Seitenlinien-Neuromasten als Kompensation fuer den Sehverlust."
    ),

    // Q13
    Question(
        categoryId = 9,
        questionText = "Welches Coccolithophoriden-Genus spielt die entscheidende Rolle in der biologischen Kohlenstoffpumpe durch Calcifizierung und ist global am haeufigsten?",
        answerA = "Trichodesmium",
        answerB = "Emiliania huxleyi",
        answerC = "Noctiluca",
        answerD = "Ceratium tripos",
        correctAnswer = 1,
        explanation = "Emiliania huxleyi (Haptophyta) ist die weltweit haeufigste Coccolithophoride und bildet Kalkplaettchen (Coccolithen) aus Calcit. Beim Absterben sinken diese als Marine Snow in die Tiefsee und binden CO2 langfristig (Carbonat-Pumpe).",
        difficulty = 4,
        funFact = "Die weissen Kreidefelsen von Dover entstanden zu einem grossen Teil aus Coccolithen-Ansammlungen, die sich ueber Millionen von Jahren am Meeresgrund abgelagert haben."
    ),

    // Q14
    Question(
        categoryId = 9,
        questionText = "Durch welchen primaeren Umwelttrigger wird das synchronisierte Broadcast Spawning (Massenlaichen) von Korallenriffen ausgeloest?",
        answerA = "Tiefenveraenderungen; Cortisol-Puls",
        answerB = "Salzgehalt-Schwankungen; Oxytocin-aehnliches Peptid",
        answerC = "Licht-Dunkelphasen; Melatonin allein",
        answerD = "Vollmond nach dem waermsten Monat; GnRH-aehnliche Neuropeptide",
        correctAnswer = 3,
        explanation = "Korallenlaichen wird durch den Vollmond nach dem waermsten Monat getriggert. Lunarperiodische Lichtreize aktivieren cryptochrome Photorezeptoren, die eine neuroendokrine Kaskade ausloesen. GnRH-aehnliche Neuropeptide koordinieren die synchrone Gameten-Freisetzung aller Kolonien.",
        difficulty = 4,
        funFact = "Das groesste Massenlaichen der Erde findet am Great Barrier Reef statt — in einer einzigen Nacht werden Milliarden von Gameten ins Meer entlassen."
    ),

    // Q15
    Question(
        categoryId = 9,
        questionText = "Welches Protein in der lichtempfindlichen Membran von Rimicaris exoculata (Tiefsee-Garnele) ermoeglicht die Detektion des schwachen Infrarotleuchtens hydrothermaler Quellen?",
        answerA = "Melanopsin (OPN4)",
        answerB = "Peropsin (RGR-Opsin)",
        answerC = "Parapinopsin",
        answerD = "Go-Opsin (Rq-Opsin) in einem dorsalen Infrarot-Organ",
        correctAnswer = 3,
        explanation = "Rimicaris exoculata hat keine Facettenaugen, besitzt aber ein stark vergroessertes dorsales Organ mit einer lichtempfindlichen Membran. Go-Opsin (Rq-Opsin) ist das dominante Sehpigment und reagiert auf die schwache Waermestrahlung (~600-750 nm) der hydrothermalen Quellen.",
        difficulty = 4,
        funFact = "Rimicaris exoculata lebt in Kolonien von bis zu 2.000 Tieren pro Quadratmeter um schwarze Raucher herum und ernaehrt sich von den chemosynthetischen Bakterien der Quellen."
    ),

    // Q16
    Question(
        categoryId = 9,
        questionText = "Welche einzigartige Anpassung erklaert den O2-Transport beim Antarktischen Eisfisch (Channichthyidae) ohne Haemoglobin und Erythrozyten?",
        answerA = "Haemocyanin ersetzt Haemoglobin",
        answerB = "Monomeres Haemoglobin mit extremer O2-Affinitaet",
        answerC = "Haemerythrin als alternatives O2-Bindeprotein",
        answerD = "Physikalische O2-Loesung im Plasma durch 4-5x erhohtes Herzminutenvolumen und vergroesserte Gefaesse",
        correctAnswer = 3,
        explanation = "Channichthyidae sind die einzigen Wirbeltiere ohne Haemoglobin und Erythrozyten. O2 wird ausschliesslich physikalisch im Blutplasma geloest. Kompensation: 4-5x hoehere Herzauswurfleistung, 2x groesseres Blutvolumen, vergroessertes Herz, weitlumige Gefaesse und niedrige Metabolismusrate bei -1,9 Grad Celsius.",
        difficulty = 4,
        funFact = "Das Blut des Eisfisches ist weiss — ein surreales Bild, das Wissenschaftler bei der Entdeckung dieser Fische 1928 voellig verbluefte."
    ),

    // Q17
    Question(
        categoryId = 9,
        questionText = "In welchem Tiefenbereich liegt die mesopelagische Zone und durch welchen charakteristischen Prozess ist sie biologisch bedeutsam?",
        answerA = "200-1.000 m; Diel Vertical Migration (DVM) und biologische Kohlenstoffpumpe",
        answerB = "1.000-4.000 m; Chemosynthese an Hydrothermalquellen",
        answerC = "50-200 m; photosynthetisch aktiver Bereich",
        answerD = "4.000-6.000 m; Abyssalzone mit stabilen Bedingungen",
        correctAnswer = 0,
        explanation = "Die mesopelagische Zone (200-1.000 m) beherbergt die groesste Biomasse tageszeitlich wandernder Organismen (Diel Vertical Migration). Organismen fressen nachts an der Oberflaeche und tauchen tags in die Tiefe — dabei transportieren sie Kohlenstoff aktiv nach unten (biologische Kohlenstoffpumpe).",
        difficulty = 4,
        funFact = null
    ),

    // Q18
    Question(
        categoryId = 9,
        questionText = "Welches spezifische Protein in den Elektroplatten des Zitteraals (Electrophorus electricus) erzeugt die elektrischen Entladungen von bis zu 860 Volt?",
        answerA = "K+-Kanaele (Kv2.1) in serotonergen Neuronenclustern",
        answerB = "Ca2+-ATPasen in modifizierten Herzmuskelzellen",
        answerC = "Spannungsabhaengige Na+-Kanaele (Nav1.4a) in einseitig innervierten Elektrox-Zellen",
        answerD = "Connexin-43 in Gap-Junction-Kanaelen",
        correctAnswer = 2,
        explanation = "Elektrische Organe bestehen aus Elektrox-Zellen (modifizierte Muskelzellen), deren innervierte Seite massenhaft Nav1.4a-Kanaele exprimiert. Aktionspotenziale oeffnen alle Kanaele synchron, und die serielle Verschaltung tausender Zellen summiert die Spannungen auf bis zu 860 V.",
        difficulty = 4,
        funFact = "Der Zitteraal ist kein echter Aal, sondern ein Messerfisch (Gymnotiformes). Er hat drei separate elektrische Organe fuer Navigation, Kommunikation sowie Jagd und Verteidigung."
    ),

    // Q19
    Question(
        categoryId = 9,
        questionText = "Welches Genus der Tiefsee-Anglerfische betreibt permanenten sexuellen Parasitismus, bei dem das Maennchen physiologisch mit dem Weibchen verwachst?",
        answerA = "Melanocetus",
        answerB = "Cryptopsaras",
        answerC = "Ceratias",
        answerD = "Linophryne",
        correctAnswer = 2,
        explanation = "Ceratias holboelli ist das klassische Beispiel fuer obligaten sexuellen Parasitismus. Das winzige Maennchen beisst sich in das Weibchen, Haut verwachst und Blutkreislauf verbindet sich. Das Maennchen verliert Augen, Kiemen und innere Organe — nur die Hoden bleiben funktional.",
        difficulty = 4,
        funFact = "Als Wissenschaftler die ersten weiblichen Tiefsee-Anglerfische untersuchten, hielten sie die angelwachsenen Maennchen fuer Parasiten — bis man erkannte, dass diese Spermien produzierten."
    ),

    // Q20
    Question(
        categoryId = 9,
        questionText = "Wie beeintraechtigt El Nino die Nahrungsketten pelagischer Fischpopulationen im Ostzentralpazifik?",
        answerA = "Thermokline steigt auf, erhoehte Naehrstoffzufuhr fuehrt zur Eutrophierung",
        answerB = "Salzgehalt erhoehte sich, was osmotischen Stress verursacht",
        answerC = "Thermokline wird zerstoert, Tiefenwasser mischt sich mit Oberflachenwasser",
        answerD = "Erwaermung vertieft die Thermokline, reduziert Auftrieb und Naehrstoffzufuhr, Phytoplankton kollabiert",
        correctAnswer = 3,
        explanation = "Beim El Nino vertieft sich die Thermokline vor der suedamerikanischen Kueste von ca. 50 m auf 150+ m. Der Auftriebseffekt wird stark reduziert, naehrstoffreiches Tiefenwasser erreicht die photische Zone nicht mehr. Phytoplankton-Biomasse kollabiert um 50-80%, was Sardellen, Seevoegel und Meeressaeuger massiv schaedigt.",
        difficulty = 4,
        funFact = "Das El Nino 1997-1998 reduzierte die peruanische Anchovy-Fangmenge von 12 Millionen auf 2,5 Millionen Tonnen — ein wirtschaftlicher Schaden von mehreren Milliarden US-Dollar."
    ),

    // Q21
    Question(
        categoryId = 9,
        questionText = "Welches Neurotoxin produziert der Blauring-Oktopus (Hapalochlaena maculosa) und in welcher Struktur wird es von Symbionten erzeugt?",
        answerA = "Tetrodotoxin, produziert von Pseudoalteromonas-Bakterien in den hinteren Speicheldrüsen",
        answerB = "Ciguatoxin, synthetisiert durch Dinoflagellaten in der Tinte",
        answerC = "Saxitoxin, aus der Nahrung im Muskelfleisch akkumuliert",
        answerD = "Palytoxin, von Zooxanthellen in der Haut erzeugt",
        correctAnswer = 0,
        explanation = "Hapalochlaena-Arten beherbergen TTX-produzierende Bakterien (v.a. Pseudoalteromonas tetraodonis) in ihren hinteren Speicheldrüsen. TTX blockiert voltage-gated Natriumkanaele irreversibel. Eine einzige Injektion enthaelt genug TTX um 26 erwachsene Menschen zu toeten; kein Gegengift existiert.",
        difficulty = 4,
        funFact = null
    ),

    // Q22
    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus der Lateral Inhibition in den Ommatidien von Limulus polyphemus wurde als grundlegend fuer das Verstaendnis visueller Kontrastverstaerkung erkannt?",
        answerA = "Exzitatorische Verbindungen zwischen Ganglienzellen verstaerken gleichmaessige Flaechen",
        answerB = "Hemmende Seitenverbindungen zwischen benachbarten Eccentric Cells verstaerken Kontraste an Helligkeitsgrenzen",
        answerC = "Inhibitorische Rueckkopplung vom Gehirn unterdrueckt periphere Signale",
        answerD = "Temporal inhibition verhindert Bewegungsdetektion bei langsamen Objekten",
        correctAnswer = 1,
        explanation = "Hartline und Ratliff (Nobel 1967) zeigten, dass benachbarte Ommatidien sich gegenseitig hemmen. Je staerker ein Ommatidium stimuliert wird, desto mehr hemmt es seine Nachbarn via laterale inhibitorische Verbindungen (Eccentric Cell zu Eccentric Cell). Dies verstaerkt Kontraste an Helligkeitsgrenzen (Mach-Banden-Effekt).",
        difficulty = 4,
        funFact = "Haldan Keffer Hartline erhielt 1967 den Nobelpreis fuer Physiologie/Medizin fuer seine Entdeckungen an Limulus-Photorezeptoren — ein Meerestier, das unser Verstaendnis des menschlichen Sehens revolutionierte."
    ),

    // Q23
    Question(
        categoryId = 9,
        questionText = "Welches ist der intrazellulaere Chemosynthese-Symbiont des Tiefsee-Roehrenrwurms Riftia pachyptila und wie transportiert dieser seinen H2S-Substrat?",
        answerA = "Arcobacter sulfidicus durch osmotische Diffusion ueber die Haut",
        answerB = "Thiovulum majus wird direkt mit der Nahrung aufgenommen",
        answerC = "Candidatus Endoriftia persephone; spezielles Haemoglobin (Hb-1/Hb-2) bindet H2S und O2 ohne gegenseitige Hemmung",
        answerD = "Thiobacillus thiooxidans extrahiert SO4 anaerob aus Sedimenten",
        correctAnswer = 2,
        explanation = "Candidatus Endoriftia persephone lebt im Trophosom-Organ von Riftia pachyptila. Das Haemoglobin von Riftia ist einzigartig: Hb-2 transportiert H2S, Hb-1 transportiert O2 und CO2 — beide werden simultan gebunden ohne gegenseitige Hemmung.",
        difficulty = 4,
        funFact = null
    ),

    // Q24
    Question(
        categoryId = 9,
        questionText = "Wie unterscheiden sich Superficial Neuromasts und Canal Neuromasts im Seitenlinien-System von Fischen in Bezug auf Frequenzfilterung?",
        answerA = "Oberflaechenneuromasten reagieren auf DC und niedrige Frequenzen (<50 Hz); Kanalneuromasten reagieren als Hochpassfilter auf Frequenzen >50 Hz durch Massentraegheit der Kanalflussigkeit",
        answerB = "Kanalneuromasten sind ausschliesslich elektroreceptiv",
        answerC = "Beide Typen reagieren identisch und unterscheiden sich nur in der Position",
        answerD = "Oberflaechenneuromasten sind nur bei juvenilen Fischen funktional",
        correctAnswer = 0,
        explanation = "Superficial Neuromasts (SNs) reagieren auf stationaere und langsame Stroemungen (DC, <50 Hz). Canal Neuromasts (CNs) in geschlossenen Kanaelen fungieren als Hochpassfilter — die Wassertraegheit im Kanal daempft niedrige Frequenzen und verstaerkt die Reaktion auf Stroemungsgradienten (>50 Hz).",
        difficulty = 4,
        funFact = "Haie haben bis zu 5.000 Kanalneuromasten und koennen damit die Druckwellen eines schwimmenden Fisches auf ueber 100 Meter Entfernung detektieren."
    ),

    // Q25
    Question(
        categoryId = 9,
        questionText = "Durch welchen Evolutionsmechanismus entstanden die 500+ Cichliden-Arten des Viktoriasees in weniger als 15.000 Jahren?",
        answerA = "Adaptive Radiation durch sexuelle Selektion auf Farbmuster kombiniert mit Nahrungsspezialisierung in post-glacialer Neubesiedlung",
        answerB = "Allopatrische Artbildung durch tektonische Becken-Fragmentierung",
        answerC = "Polyploidisierungs-Ereignis gefolgt von Chromosomen-Umstrukturierung",
        answerD = "Horizontaler Gentransfer durch Retroviren zwischen Populationen",
        correctAnswer = 0,
        explanation = "Die Viktoriasee-Cichliden entstanden durch explosive adaptive Radiation nach der Neuflutung des ausgetrockneten Sees vor ca. 14.700 Jahren. Treibende Kraefte waren sexuelle Selektion auf LWS-Opsin-Farbmuster in verschiedenen Lichttiefen sowie Nahrungsoekologische Spezialisierung.",
        difficulty = 4,
        funFact = "Der Nil-Barsch, 1954 im Viktoriasee eingesetzt, hat seitdem ueber 200 endemische Cichliden-Arten ausgerottet — eine der groessten vom Menschen verursachten Massenausrottungen der Neuzeit."
    ),

    // Q26
    Question(
        categoryId = 9,
        questionText = "Welches Prinzip der Stickstoff-Fixierung nutzt das Cyanobacterium Trichodesmium erythraeum, obwohl Nitrogenase O2-empfindlich ist?",
        answerA = "Glutamin-Synthetase im Periplasma der Heterocysten",
        answerB = "Nitrogenase in spezialisierten Diazocyten; zeitliche Trennung von Photosynthese und N2-Fixierung",
        answerC = "Nitrat-Reductase unter photosynthetischen Bedingungen",
        answerD = "Urease-Komplex bei Harnstoff-Recycling",
        correctAnswer = 1,
        explanation = "Trichodesmium fixiert N2 in spezialisierten Diazocyten mittels Nitrogenase. Das O2-Problem wird durch zeitliche Trennung geloest: Photosynthese mittags, N2-Fixierung fruehmorgens und spaetabends, kombiniert mit hoher Respiratory-Scavenging-Rate fuer O2.",
        difficulty = 4,
        funFact = null
    ),

    // Q27
    Question(
        categoryId = 9,
        questionText = "Welcher primäre molekulare Mechanismus ermoeglicht dem Seidenhai (Carcharhinus falciformis) die Navigation ueber Fernstrecken mithilfe des Erdmagnetfelds?",
        answerA = "Biogener Magnetit (Fe3O4) in der olfaktorischen Region als Magnetorezeptor",
        answerB = "Cryptochromen in der Netzhaut erzeugen Singulett-Triplett-Elektronenpaar-Reaktionen",
        answerC = "Statolithen aus CaCO3 reagieren auf magnetische Feldlinien",
        answerD = "Kupfer-Proteine im Blutplasma orientieren sich am Magnetfeld",
        correctAnswer = 0,
        explanation = "Biogener Magnetit (Einzeldomain-Magnetit, ~40-100 nm) wurde in der olfaktorischen Region und Haut von Elasmobranchen nachgewiesen. Magnetit-Kristalle koennen sich im Erdmagnetfeld ausrichten und ueber mechanosensitive Kanaele Signale erzeugen — der exakte Transduktionsmechanismus ist noch Forschungsgegenstand.",
        difficulty = 4,
        funFact = null
    ),

    // Q28
    Question(
        categoryId = 9,
        questionText = "Was beschreibt die Thermal Mass von Carcharodon carcharias und welchen funktionellen Vorteil bietet sie bei der Jagd im Kaltwasser?",
        answerA = "Grosses Volumen-Oberflaeche-Verhaeltnis erhaelt Koerperwaerme laenger; Regional Endothermy der roten Muskulatur erhaelt 14-20 Grad Celsius mehr als das Umgebungswasser",
        answerB = "Grosse Leber als Waermepuffer durch Fettsaeure-Oxidation",
        answerC = "Zahnschmelz-Dichte korreliert mit Koerpertemperatur",
        answerD = "Hohe Koerpermasse erhoehte hydrostatischen Druck auf Beutetiere",
        correctAnswer = 0,
        explanation = "Carcharodon carcharias ist ein regionaler Endotherm. Bei grossen Haien reduziert das guenstige Volumen-Oberflaeche-Verhaeltnis Waermeverluste, und ein Rete mirabile haelt die rote Muskulatur 14-20 Grad Celsius waermer als das Kalt-Kapwasser. Dies verbessert Muskelleistung und Reaktionszeit.",
        difficulty = 4,
        funFact = "Weisshaie jaegen im Kapgewaesser bevorzugt fruehmorgens und spaetnachmittags, wenn das Licht von oben kommt und sie Robbensilhouetten von unten erkennen koennen."
    ),

    // Q29
    Question(
        categoryId = 9,
        questionText = "Welches spezifische Merkmal der Chitonzaehne (Polyplacophora) macht sie zum haertesten biogenen Biomaterial der Erde?",
        answerA = "Calciumcarbosyl-Transferase synthetisiert Eisen-Aragonit-Composite",
        answerB = "Ferredoxin-Cluster erzeugen magnetische Momente in Protein-Netzwerken",
        answerC = "Magnetit (Fe3O4)-Kristalle in einer Chitin-alpha-Protein-Matrix, nukleiert durch Ferritin-aehnliche Proteine",
        answerD = "Haptoglobin bindet Fe3+ und polymerisiert zu Eisenoxid-Fasern",
        correctAnswer = 2,
        explanation = "Chiton-Zaehne bestehen aus Magnetit (Fe3O4) eingebettet in eine Chitin-Protein-Matrix. Ferritin-aehnliche Proteine akkumulieren Fe3+ und nukleieren Ferrihydrit-Nanopartikel, die zu hochkristallinem Magnetit umgewandelt werden. Haerte: ~12 GPa, haerter als menschlichen Zahnemail.",
        difficulty = 4,
        funFact = "Erst 2015 entdeckten Forscher, dass Chiton-Zaehne das haerteste biologische Material der Erde sind. Ihr Design inspiriert neue Kompositmaterialien fuer die Ingenieurswissenschaften."
    ),

    // Q30
    Question(
        categoryId = 9,
        questionText = "In welchem Mineral-Vorkommen der Tiefsee ist der hoechste globale Bestand an Mangangknollen konzentriert und wie schnell wachsen diese?",
        answerA = "Nazca Ridge im Pazifik; ~1 cm pro 1.000 Jahre durch vulkanische Exhalation",
        answerB = "Angola Basin; ~10 mm pro 10.000 Jahre durch bakterielle Biomineralisierung",
        answerC = "Clarion-Clipperton-Zone (CCZ) im Nordpazifik; ~1-6 mm pro Million Jahre durch diagenetische und hydrothermale Ausfaellung",
        answerD = "Ryukyu Trench; ~1 mm pro 100.000 Jahre durch submarine Verwitterung",
        correctAnswer = 2,
        explanation = "Die Clarion-Clipperton-Zone (CCZ) beherbergt ca. 21 Milliarden Tonnen Manganknollen. Knollen entstehen durch diagenetische und hydrothermale Ausfaellung von Mangan, Eisen, Nickel, Kobalt und Kupfer um einen Nukleus. Wachstumsrate: 1-6 mm pro Million Jahre.",
        difficulty = 4,
        funFact = "Die Manganknollen der CCZ enthalten mehr Nickel und Kobalt als alle Landvorkommen zusammen — was intensive Diskussionen ueber Tiefseebergbau und seine oekologischen Folgen ausloest."
    ),

    // Q31
    Question(
        categoryId = 9,
        questionText = "Wie funktioniert aktive Elektrortung bei Gnathonemus petersii (Elefantenruesselfisch)?",
        answerA = "Passive Detektion fremder bioelektrischer Felder via Ampullen der Lorenzini",
        answerB = "Infrarot-Detektion durch thermorezeptive Hautorgane",
        answerC = "Elektrostatische Induktion durch Oberflachen-Feldverzerrung im Schnauzen-Organ",
        answerD = "Schwache elektrische Entladungen (EOD) erzeugen ein Feld; Objekte stoeren dieses; tubulaere und ampullaere Elektrorezeptoren messen die Verzerrungen",
        correctAnswer = 3,
        explanation = "Gnathonemus petersii produziert schwache elektrische Entladungen (ca. 1 mV/cm) durch ein modifiziertes Rueckenmark-Organ. Objekte mit anderem elektrischen Widerstand als Wasser stoeren dieses Feld. Tubulaere Elektrorezeptoren messen hochfrequente Feldverzerrungen fuer Objekt-Ortung, ampullaere Elektrorezeptoren kommunizieren via EODs.",
        difficulty = 4,
        funFact = null
    ),

    // Q32
    Question(
        categoryId = 9,
        questionText = "Welchen Life History Shift zeigt Kabeljau (Gadus morhua) unter jahrzehntelangem selektivem Fischereidruck auf grosse Individuen?",
        answerA = "Keine evolutionaere Reaktion, nur Populationsabnahme",
        answerB = "Monogame Paarbindung und Polygamie-Shift",
        answerC = "Inversions-Hermaphroditismus unter Praedationsdruck",
        answerD = "Frueherer Eintritt der Geschlechtsreife bei kleinerer Koerpergroesse (2-3 Jahre frueher, 30-40% geringere Laenge)",
        correctAnswer = 3,
        explanation = "Jahrzehntelanger selektiver Fischereidruck auf grosse, reife Kabeljau-Individuen hat evolutionaere Veraenderungen induziert. Kabeljau bestimmter Populationen werden heute 2-3 Jahre frueher geschlechtsreif bei 30-40% geringerer Koerperlaenge. Dieser Life History Shift ist nur sehr langsam reversibel.",
        difficulty = 4,
        funFact = "Der Zusammenbruch des Neufundland-Kabeljaubestandes 1992 gilt als eine der groessten oekologischen Katastrophen der modernen Fischerei — 40.000 Menschen verloren ihre Erwerbsgrundlage."
    ),

    // Q33
    Question(
        categoryId = 9,
        questionText = "Welches bioakustische Phaenomen erklaert die nicht-linearen Vokal-Elemente in Buckelwal-Gesaengen (Megaptera novaeangliae)?",
        answerA = "Doppler-Verschiebung durch Schwimmbewegung veraendert Ruffrequenz",
        answerB = "Infrarot-Signal-Verstaerkung durch Resonanzhoehlen in der Nasenpassage",
        answerC = "Echo-Lokation analog zu Delfinen bei 40-80 kHz",
        answerD = "Bifurcation Points (Chaos-Rufe) durch Vocal Fry-Mechanismus in U-Falten-Schleimhautvibrationen erzeugen breite Spektralbaender",
        correctAnswer = 3,
        explanation = "Buckelwale besitzen U-foermige Vocal Folds und erzeugen nichtlineare Geraeusche (Chaos-Bifurkationen, Vocal Fry, Subharmonics), die entstehen wenn die Schleimhaut-Schwingungsfrequenz in ein deterministisches Chaos-Regime eintritt. Diese Nonlinearities tragen zur individuellen Erkennbarkeit bei.",
        difficulty = 4,
        funFact = "Buckelwal-Gesaenge zeigen kulturelle Evolution: Neue Melodie-Elemente breiten sich innerhalb eines Ozeans von West nach Ost aus — wie ein Musikstil, der viral geht."
    ),

    // Q34
    Question(
        categoryId = 9,
        questionText = "Welche Reaktion der intrinsischen Luziferase erklaert die autogene Biolumineszenz (ohne Bakterien) von Laternenfischen (Myctophidae)?",
        answerA = "ATP-Oxidation durch Firefly-Luciferase (analog Lampyris noctiluca)",
        answerB = "Riboflavin-FMNH2-Oxidation via bakterielle LuxAB-Luziferase",
        answerC = "Coelenterazin + O2 wird durch intrinsische Luziferase zu Coelenteramid + CO2 + Photon (475 nm) oxidiert",
        answerD = "Bakterieller Luxl/LuxR-Quorum-Sensing-Kreis aktiviert lux-Operon",
        correctAnswer = 2,
        explanation = "Laternenfische produzieren Biolumineszenz intrinsisch (autogen). Die Reaktion: Coelenterazin + O2 wird durch intrinsische Luziferase zu angeregtem Coelenteramid oxidiert. Das Chromophor emittiert beim Zerfall ein Photon bei ~475 nm. Kein ATP wird benoetigt — im Gegensatz zur Gluehwuermchen-Chemie.",
        difficulty = 4,
        funFact = null
    ),

    // Q35
    Question(
        categoryId = 9,
        questionText = "Durch welchen primären Prozess entstehen hypoxische Dead Zones (z.B. Golf von Mexiko) und welcher Stickstoff-Kreislauf-Schritt ist der groesste O2-Verbraucher?",
        answerA = "Denitrifikation (NO3- -> N2) erzeugt O2-freie Bedingungen direkt",
        answerB = "Methanogenese verbraucht geloesten O2 in Sedimenten",
        answerC = "Sulfatreduktion im Tiefenwasser der Sedimentsaeule",
        answerD = "Heterotrophe Remineralisierung von eutrophiertem Phytoplankton und Nitrifikation (NH4+ oxidation) verbrauchen O2 schneller als Reaeration",
        correctAnswer = 3,
        explanation = "Eutrophierung durch Stickstoff- und Phosphat-Eintraege foerdert massives Phytoplankton-Wachstum. Abgestorbenes Phytoplankton ernaehrt mikrobielle Benthosgemeinschaften. Heterotrophe Respiration und Nitrifikation verbrauchen O2 schneller als Reaeration, was O2-Konzentrationen unter 2 mg/L erzeugt.",
        difficulty = 4,
        funFact = "Die Dead Zone im Golf von Mexiko erreichte 2017 eine Rekordflaeche von 22.720 km2 — groesser als New Jersey. Die Ursache liegt zu 70% in Duengemitteln aus dem Midwest der USA."
    ),

    // Q36
    Question(
        categoryId = 9,
        questionText = "Welches einzigartige Gehirnareal bei Zahnwalen (Odontoceti) verarbeitet Ultraschall-Echos fuer die Echolocation?",
        answerA = "Hypertropher hippokampaler CA3-Bereich fuer raumliche Kartierung",
        answerB = "Erweiterter Vomeronasal-Kortex fuer Chemosignal-Verarbeitung",
        answerC = "Paralobaler Kortex (PLC) und hypertropher inferiorer Kolliculus fuer biaurale Zeitdifferenz-Analyse von Ultraschall-Echos",
        answerD = "Zusaetzlicher spinaler Cortex fuer Flossenkoordination",
        correctAnswer = 2,
        explanation = "Odontoceti haben einen massiv erweiterten auditorischen Cortex mit dem einzigartigen paralobalen Kortex (PLC) — ausschliesslich bei Delfinen vorhanden. Er verarbeitet biaurale Zeitdifferenzen im Mikrosekunden-Bereich fuer 3D-Echolocation. Der inferiore Kolliculus ist proportional 3-5x groesser als bei Fledermaeausen.",
        difficulty = 4,
        funFact = null
    ),

    // Q37
    Question(
        categoryId = 9,
        questionText = "Welches Prinzip erklaert die ozeanische Upwelling-Entstehung an Ostkuesten der Kontinente durch Ekman-Transport?",
        answerA = "Thermohaline Zirkulation durch Dichtegradient-Pumpwirkung",
        answerB = "Langmuir-Zirkulation durch oberflaechennahe Helixwirbel",
        answerC = "Wind parallel zur Kueste treibt durch Coriolis-Kraft Oberflaechenwasser 90 Grad davon weg, naehrstoffreiches Tiefenwasser steigt nach",
        answerD = "Kelvin-Wellen-induzierter Auftrieb in Aequatorialnaehe",
        correctAnswer = 2,
        explanation = "Coastal Upwelling entsteht wenn kuestenparalleler Wind den Ekman-Transport aktiviert: Oberflaechenwasser wird 90 Grad zur Windrichtung versetzt. Das abwandernde Oberflaechenwasser wird durch aufsteigendes, kaltes, naehrstoffreiches Tiefenwasser (600-1000 m) ersetzt.",
        difficulty = 4,
        funFact = null
    ),

    // Q38
    Question(
        categoryId = 9,
        questionText = "Welcher primäre chemische Stoffstrom kennzeichnet die Symbiose zwischen Riesenmuscheln (Tridacna gigas) und ihren Zooxanthellen (Symbiodiniaceae)?",
        answerA = "Zooxanthellen produzieren Sauerstoff, den die Muschel fuer Fermentation nutzt",
        answerB = "Muschel verdaut Zooxanthellen als supplementaere Proteinquelle",
        answerC = "Zooxanthellen exportieren Photosynthese-Produkte (Glycerin, Glucose); Muschel liefert CO2, NH4+ und Phosphat",
        answerD = "Zooxanthellen produzieren Chitin fuer die Schalenstruktur der Muschel",
        correctAnswer = 2,
        explanation = "Die Mutualsymbiose funktioniert als bidirektionaler Naehrstoffstrom: Zooxanthellen exportieren bis zu 90% ihres Assimilats (Glycerin, Glucose, Aminosauren) an die Muschel. Die Muschel liefert metabolische Abfallprodukte (CO2, NH4+, Phosphat) direkt an die Zooxanthellen.",
        difficulty = 4,
        funFact = "Tridacna gigas kann ueber 200 kg wiegen und ueber 100 Jahre alt werden. Ein ausgewachsenes Tier enthaelt bis zu einer Milliarde Zooxanthellen-Zellen im Mantelgewebe."
    ),

    // Q39
    Question(
        categoryId = 9,
        questionText = "Welche spezifische Anpassung des Pottwals (Physeter macrocephalus) ermoeglicht Tauchtiefen bis 2.250 m ohne Barotrauma?",
        answerA = "Spezielles elastisches Kollagen Typ IV in der Pleurahoehlenwand",
        answerB = "Haemoglobin-Polymer mit hoher O2-Affinitaet (HbS-Typ)",
        answerC = "Carboanhydrase-Hyperaktivitaet verhindert CO2-Narkose",
        answerD = "Extrem hohe Myoglobin-Konzentration (10-20 mg/g Muskelgewebe) als O2-Reservoir; vollstaendiger Lungen-Kollaps ohne Gewebeschaeden durch speziellen Surfactant",
        correctAnswer = 3,
        explanation = "Pottwale haben Myoglobin in extrem hoher Konzentration als primaeres O2-Reservoir (Muskeln sind dunkelbraun bis schwarz). Beim Tauchgang kollabieren die Lungen vollstaendig (100+ atm), und die Alveolen falten sich ohne Verletzung dank spezieller Surfactant-Zusammensetzung. N2 wird herausgepresst, was Dekompressionskrankheit verhindert.",
        difficulty = 4,
        funFact = "Pottwale haben die groesste Gehirn-Masse aller je gelebten Tiere (ca. 8 kg). Verschiedene Clan-Gruppen haben verschiedene Dialekte — ein Hinweis auf kulturelle Transmission."
    ),

    // Q40
    Question(
        categoryId = 9,
        questionText = "Welche Thermoregulations-Anpassung haelt beim Blauflossen-Thunfisch (Thunnus thynnus) Gehirn und Augen waermer als das Umgebungswasser?",
        answerA = "Mitochondrien-Entkopplung (UCP1) in speziellen braunen Fettzellengewerben",
        answerB = "Gegenstrom-Waermeaustauscher (Rete mirabile) in der Orbita und Suprahepatisches Rete erhalten Gehirn/Augen-Temperatur 10-15 Grad ueber Umgebungstemperatur",
        answerC = "Shivering-Thermogenese durch Extraokular-Muskulatur",
        answerD = "Vasodilatation in Hautgefaessen erhoehte thermale Schichtung",
        correctAnswer = 1,
        explanation = "Thunnidae besitzen Gegenstrom-Waermeaustauscher (Rete mirabile). Arterielles Blut gibt Waerme an zurueckkehrendes kuehles venoses Blut ab, bevor es Kaltbereiche erreicht. Das craniale Rete erhaelt Augen und Gehirn bis zu 10-15 Grad ueber Umgebungstemperatur, was Sehschaerfe und Reaktionszeit verbessert.",
        difficulty = 4,
        funFact = "Beim Tauchgang in kaltes Tiefenwasser haelt der Blauflossen-Thunfisch sein Gehirn bei 28 Grad — obwohl das umgebende Wasser nur 13 Grad hat."
    ),

    // Q41
    Question(
        categoryId = 9,
        questionText = "Welches Elektrorezeptionssystem hat das Schnabeltier (Ornithorhynchus anatinus) und wo sind seine Elektrorezeptoren lokalisiert?",
        answerA = "In der Schwanzflosse: modifizierte Haarfollikel mit piezoelektrischen Eigenschaften",
        answerB = "Im Kloaken-Bereich: ampullaere Organe analog zu Haifischen",
        answerC = "Im Schnabel: ca. 40.000 mechanosensitive Push-Rod-Rezeptoren und 60.000 Mukosal-Elektrorezeptoren",
        answerD = "Im Magen: pH-sensitive Elektrorezeptoren zur Beute-Detektion",
        correctAnswer = 2,
        explanation = "Das Schnabeltier hat ~40.000 mechanosensitive Push-Rod-Mechanorezeptoren und ~60.000 Elektrorezeptoren (Mucosal gland-type) in der Schleimhaut des Schnabels. Die Elektrorezeptoren detektieren DC-Felder von 0,5-1 mV/cm — empfindlich genug fuer Muskelaktionspotenziale eines schwimmenden Krebses.",
        difficulty = 4,
        funFact = "Das Schnabeltier jaegt ausschliesslich mit geschlossenen Augen, Ohren und Nase unter Wasser — es orientiert sich rein elektrisch und mechanisch via Schnabelsensoren."
    ),

    // Q42
    Question(
        categoryId = 9,
        questionText = "Welchen adaptiven Mechanismus verwendet der Beilfisch (Argyropelecus hemigymnus) als Tarnstrategie durch Counter-Illumination?",
        answerA = "Ventrale Photophoren emittieren blaues Licht (470-490 nm), das dem Restlicht von oben entspricht und den Schatten des Fisches fuer Raeuber von unten eliminiert",
        answerB = "Dorsale Pigmentzellen absorbieren Licht von oben, um die Silhouette zu minimieren",
        answerC = "Reflektierende Guanin-Plaettchen zerstreuen Licht gleichmaessig nach unten",
        answerD = "Chromatophoren-Biolumineszenz passt Farbton an die Tiefenzone an",
        correctAnswer = 0,
        explanation = "Counter-Illumination: Beilfische haben ventrale Photophoren, die blaues Licht (470-490 nm) emittieren, das dem Spektrum und der Intensitaet des schwachen Restlichts von oben entspricht. Raeuber, die von unten aufschauen, sehen kein dunkles Schattensignal — der Fisch verschwindet vor dem Hintergrund.",
        difficulty = 4,
        funFact = null
    ),

    // Q43
    Question(
        categoryId = 9,
        questionText = "Welches Radiolarien-Merkmal unterscheidet sie von anderen marinen Protozoen und macht sie zu wichtigen Proxy-Indikatoren in Palaeozeanographie?",
        answerA = "Cellulose-Zellwand aus Phytoplankton-Pigmenten",
        answerB = "Silikatische Skelette (Opalglas) mit artenspezifischen Strukturen; stratigraphischer Leitfossil-Charakter",
        answerC = "Calcium-Phosphat-Skelette mit isotopischen Signaturen",
        answerD = "Calcitische Tests wie bei Foraminiferen",
        correctAnswer = 1,
        explanation = "Radiolarien bilden elegante Skelette aus amorphem Silikat (Opal, SiO2 nH2O) mit artenspezifischen Formen. Da ihre Artenzusammensetzung von Wassertemperatur, -tiefe und Produktivitaet abhaengt, werden Radiolarien-Vergesellschaftungen in Tiefseesedimenten als Palaeothermometer und Palaeoproduktivitaetsindikatoren genutzt.",
        difficulty = 4,
        funFact = null
    ),

    // Q44
    Question(
        categoryId = 9,
        questionText = "Wie erklaert das Prinzip der Bioturbation die erhoehte Sauerstoff-Penetrationstiefe in Tiefsee-Sedimenten?",
        answerA = "Bioturbation versiegelt Sedimentporen und reduziert O2-Diffusion",
        answerB = "Bioturbation hat keine messbaren Auswirkungen unterhalb 3.000 m Tiefe",
        answerC = "Bioturbation durch Makrofauna erhoehte effektive Diffusionskoeffizienten fuer O2 um das 10-100-fache und vertieft die O2-Penetrationstiefe von 1-5 mm auf 1-5 cm",
        answerD = "Bioturbation erzeugt Anaerobie durch Methan-Freisetzung",
        correctAnswer = 2,
        explanation = "Bioturbation durch Polychaeten, Amphipoden und andere Makrofauna erhoehte den effektiven Diffusionskoeffizienten fuer geloeste Stoffe um 10-100x ueber molekulare Diffusion. Die Sauerstoffpenetrationstiefe vertieft sich von 1-5 mm auf 1-5 cm, was organisches Material effizienter remineralisiert.",
        difficulty = 4,
        funFact = null
    ),

    // Q45
    Question(
        categoryId = 9,
        questionText = "Welches K-Pg-Ereignis katalysierte die dominante Adaptive Radiation der Acanthomorpha (Stachelflosser) und wann fand ihre Hauptdiversifizierung statt?",
        answerA = "K-Pg-Massenaussterben vor 66 Mio. Jahren schuf oekologische Vakua; Acanthomorpha diversifizierten im Palaeozaen/Eozaen (65-48 Mio. Jahre) in diese Nischen",
        answerB = "Ordovizische Explosion vor 488 Mio. Jahren diversifizierte fruehe Gnathostomata",
        answerC = "Silurische Bodenbesiedelung vor 430 Mio. Jahren erzeugte terrestrische Formen",
        answerD = "Miozaene Klimaabkuehlung vor 15 Mio. Jahren katalysierte polare Eisfisch-Diversifikation",
        correctAnswer = 0,
        explanation = "Das K-Pg-Grenzaussterben (vor 66 Mio. Jahren) loeschte wichtige marine Vertebraten aus und schuf oekologische Vakua. Acanthomorpha explodierten im Palaeozaen/Eozaen in einer adaptiven Radiation. Heute sind sie mit ~16.000 Arten (>50% aller Wirbeltiere) die artenreichste Wirbeltiergruppe.",
        difficulty = 4,
        funFact = null
    ),

    // Q46
    Question(
        categoryId = 9,
        questionText = "Durch welche einzigartige Haemoglobin-Eigenschaft wird bei Tiefsee-Anglerfischen (Ceratioidei) eine effiziente O2-Aufnahme trotz niedrigster pO2-Werte im Tiefenwasser gewaehrleistet?",
        answerA = "Hohe O2-Affinitaet (P50 <5 mmHg) durch modifizierter 2,3-BPG-Bindestelle und verminderter Bohr-Effekt",
        answerB = "Niedrige O2-Affinitaet (P50 >40 mmHg) durch extra Histidin in der Globin-Sequenz",
        answerC = "Haemocyanin statt Haemoglobin fuer verbesserte O2-Loeslichkeit bei Hochdruck",
        answerD = "Fetales Haemoglobin (HbF) bleibt lebenslang aktiv fuer hohe Affinitaet",
        correctAnswer = 0,
        explanation = "Tiefsee-Ceratioidei-Haemoglobin zeigt extreme O2-Affinitaet (P50 <5 mmHg vs. 26 mmHg beim Menschen). Modifizierte 2,3-BPG-Bindestelle reduziert den allosterischen Regulationseffekt. Kombiniert mit vermindertem Bohr-Effekt sichert dies O2-Bindung bei pO2-Werten von 0,5-2 ml O2/l im Tiefenwasser.",
        difficulty = 4,
        funFact = null
    ),

    // Q47
    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus erklaert das Knochen- und Organ-Schrumpfen bei Saccopharynx ampullaceus (Schwarzer Schlund-Aal) unter extremem Nahrungsmangel?",
        answerA = "Zellulare Seneszenz des Kiemenepithels",
        answerB = "Reversibler katabolischer Abbau von Wirbelsaeulen-Knorpel, Organen und Muskulatur in Hungerphasen, der das Tier physisch kleiner werden laesst",
        answerC = "Periostaler Knochenabbau durch Parathormon-Ueberproduktion",
        answerD = "Autophagische Degeneration der Schwimmblase bei Tiefenwechsel",
        correctAnswer = 1,
        explanation = "Saccopharynx und verwandte Gulper-Eels koennen in Hungerphasen Wirbelsaeulenknorpel, Organgewebe und Muskulatur reversibel resorbieren. Das Tier wird physisch kuerzer und leichter. Dieser extreme Katabolismus eroeffnet Energiereserven, die weit ueber normale Fettreserven hinausgehen — entscheidend in der Tiefsee.",
        difficulty = 4,
        funFact = null
    ),

    // Q48
    Question(
        categoryId = 9,
        questionText = "Welches Phytoplankton produziert durch Bloom-Bildung weltweit sichtbare Truebungen im Ozean und welche spezifische Struktur macht dies moeglich?",
        answerA = "Emiliania huxleyi durch Milliarden von Coccolithen-Platten aus Calcit, die Licht stark reflektieren",
        answerB = "Trichodesmium durch Gasvakuolen, die Auftrieb und Lichtreflexion erzeugen",
        answerC = "Noctiluca scintillans durch biolumineszente Vakuolen",
        answerD = "Karenia brevis durch Pigment-Aggregationen an der Wasseroberflaeche",
        correctAnswer = 0,
        explanation = "Emiliania huxleyi bildet Bluetenteppiche, die aus dem Weltraum sichtbar sind. Die Ursache sind Millionen winziger Coccolithen (Calcit-Platten, 2-4 Mikrometern), die Sonnenlicht stark zurueckwerfen und dem Wasser eine milchig-tuerkise Faerbung geben.",
        difficulty = 4,
        funFact = "Ein einzelner E. huxleyi-Bloom kann eine Flaeche von mehreren hunderttausend Quadratkilometern bedecken — groesser als Deutschland — und ist auf Satellitenbildern klar erkennbar."
    ),

    // Q49
    Question(
        categoryId = 9,
        questionText = "Welches Massensterbe-Ereignis wird als ausloesend fuer die Entstehung heutiger tiefer Ozean-Anoxia im Schwarzen Meer betrachtet und welcher Mechanismus haelt die Anoxia aufrecht?",
        answerA = "Halocline blockiert Durchmischung: Salzreiches Tiefenwasser (>22 psu) trennt sich von Suesswasser-Zufluss-dominiertem Oberflaechenwasser; mikrobielle Sulfatreduktion verbraucht alle Restoxigenierung",
        answerB = "Thermohaline Inversion durch Klimaerwaermung loest regelmaessige Durchmischung aus",
        answerC = "Submarine vulkanische Aktivitaet pumpt anaerobes CO2-reiches Wasser in die Tiefe",
        answerD = "Saisonale Eisbildung verhindert Gaseintrag im Winter",
        correctAnswer = 0,
        explanation = "Das Schwarze Meer hat eine permanente Halocline: Salzhaltiges Mittelmeerwasser (>22 psu) liegt schwer unter dem Suesswasser-dominierten Oberflaechenwasser. Diese stabile Schichtung verhindert O2-Eintrag in die Tiefe. Unterhalb von ~150-200 m herrscht permanente Anoxia mit H2S-Produktion durch sulfatreduzierende Bakterien.",
        difficulty = 4,
        funFact = null
    ),

    // Q50
    Question(
        categoryId = 9,
        questionText = "Welches Tauchen-Paradox beschreibt das 'Midnight Zone Paradox' und welche biologischen Strategien eroeffnen abyssalen Gemeinschaften das Ueberleben trotz extremer Nahrungsarmut?",
        answerA = "Tiefseefische haben hoehere Stoffwechselraten als Flachwasserfische trotz geringerer Nahrungsverfuegbarkeit",
        answerB = "Hydrothermalquellen produzieren mehr Biomasse als das gesamte Phytoplankton",
        answerC = "Tiefseeorganismen koennen ohne jede Nahrungsaufnahme ueber 100 Jahre ueberleben",
        answerD = "Nur 1-5% der Oberflaechenproduktion erreicht die Tiefsee; Ueberleben durch hocheffiziente Nahrungsverwertung, extreme Metabolismusreduktion und Nahrungspulse (Whale Falls, Jelly Falls)",
        correctAnswer = 3,
        explanation = "Das Paradox: Die Tiefsee als groesstes Oekosystem der Erde erhaelt nur 1-5% der photischen Produktion. Ueberlebensstrategien: Hocheffiziente Nahrungsverwertung, sehr langsame Metabolisierungsraten, Nutzung sporadischer Nahrungspulse (Whale Falls, Phytoplankton-Sinkraten nach Blueten), und in bestimmten Gebieten chemolithotrophe Supplementierung.",
        difficulty = 4,
        funFact = "Wenn ein Wal stirbt und auf den Meeresgrund sinkt (Whale Fall), unterstuetzt sein Kadaver fuer Jahrzehnte ein vollstaendiges Sukzessionsoekosystem mit ueber 200 spezialisierten Arten."
    ),

)
