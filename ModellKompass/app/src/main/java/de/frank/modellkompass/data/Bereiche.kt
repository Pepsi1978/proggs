package de.frank.modellkompass.data

private const val LM = "LM Studio"
private const val COMFY = "ComfyUI, Forge oder SwarmUI"

/**
 * Die Bereiche, für die die App die besten lokalen Modelle sucht. [laufzeit] nennt das Programm, in dem
 * die Modelle dieses Bereichs laufen; [auftrag] beschreibt dem Recherche-Modell, worauf es ankommt.
 */
enum class Bereich(
    val id: String,
    val titel: String,
    val emoji: String,
    val beschreibung: String,
    val laufzeit: String,
    val auftrag: String,
) {
    BILDER(
        "bilder", "Bilder erstellen", "🎨",
        "Aus Text werden Bilder: Fotos, Illustrationen, Plakate mit lesbarer Schrift.", COMFY,
        "Text-zu-Bild-Modelle mit der besten Bildqualität und Prompt-Treue, auch bei Schrift im Bild und bei Menschen " +
            "(Hände, Gesichter). Nenne die konkret passende Variante bzw. Quantisierung für 32 GB VRAM (z. B. FP8, GGUF Q8, " +
            "Nunchaku) und die Datei, die man herunterlädt.",
    ),
    BILD_BEARBEITEN(
        "bild_bearbeiten", "Bilder bearbeiten", "🖌️",
        "Vorhandene Bilder per Anweisung ändern, Teile ersetzen, hochskalieren, Person und Stil beibehalten.", COMFY,
        "Modelle zum Bearbeiten vorhandener Bilder per Textanweisung (Instruction-Editing, Inpainting, Outpainting, " +
            "Motiv- und Gesichtstreue) sowie die besten Hochskalierer. Nenne die für 32 GB VRAM passende Variante.",
    ),
    VIDEO(
        "video", "Videos erstellen", "🎬",
        "Kurze Videos aus Text oder aus einem Startbild, mit flüssiger Bewegung.", COMFY,
        "Text-zu-Video- und Bild-zu-Video-Modelle mit der besten Bewegungs- und Bildqualität, die auf 32 GB VRAM in " +
            "vertretbarer Zeit (Minuten, nicht Stunden je Clip) laufen. Nenne Auflösung und Cliplänge, die realistisch sind, " +
            "und die passende Quantisierung bzw. beschleunigte Variante (z. B. GGUF, Lightning-/Distill-LoRA).",
    ),
    MEDIZIN(
        "medizin", "Medizin & Nahrungsergänzung", "🩺",
        "Fachfragen wie von Medizinern: Wirkstoffe, Wechselwirkungen, Studienlage zu Nahrungsergänzungsmitteln.", LM,
        "Sprachmodelle mit dem besten medizinischen und biochemischen Fachwissen: Pharmakologie, Wechselwirkungen, " +
            "Laborwerte, Studienlage zu Nahrungsergänzungsmitteln (Dosierung, Wirksamkeit, Sicherheit, Bioverfügbarkeit). " +
            "Vergleiche medizinisch feinabgestimmte Modelle mit starken Allzweckmodellen anhand medizinischer Benchmarks " +
            "(z. B. MedQA, MedXpertQA) und nimm die, die tatsächlich besser abschneiden. Antworten auf Deutsch müssen gut sein.",
    ),
    ALLGEMEIN(
        "allgemein", "Allgemeine Intelligenz", "🧠",
        "Der beste Allrounder für beliebige Fragen: Wissen, Erklären, Abwägen, Ratschläge.", LM,
        "Sprachmodelle mit der höchsten allgemeinen Intelligenz und dem breitesten Wissen für beliebige Alltags- und " +
            "Fachfragen, gemessen an aktuellen unabhängigen Bestenlisten (z. B. Artificial Analysis Intelligence Index, " +
            "LiveBench, LMArena für offene Modelle). Gutes Deutsch ist Pflicht.",
    ),
    PROGRAMMIEREN(
        "programmieren", "Programmieren", "💻",
        "Kotlin-Apps für Android, Windows-Programme, Webseiten, Swift-Apps für iPhone und Mac.", LM,
        "Programmiermodelle mit der besten Codequalität für: Kotlin und Jetpack Compose (Android), C#/.NET/WPF (Windows), " +
            "TypeScript/HTML/CSS/React (Web) und Swift/SwiftUI (iPhone, Mac). Wichtig sind agentisches Arbeiten in einem " +
            "CLI-Werkzeug (zuverlässige Werkzeugaufrufe, lange Aufgaben) und ein großes Kontextfenster, das in 32 GB noch " +
            "nutzbar ist. Stütze dich auf aktuelle Coding-Benchmarks (z. B. SWE-bench Verified, Aider Polyglot, LiveCodeBench).",
    ),
    TEXT(
        "text", "Texte & Office", "✍️",
        "Große Texte in sehr gutes Deutsch bringen, zusammenfassen, umformulieren; Hilfe für Word und Excel.", LM,
        "Sprachmodelle für Textarbeit in sehr gutem, fehlerfreiem Deutsch: lange Texte überarbeiten, kürzen, " +
            "zusammenfassen, gliedern, Briefe und Berichte schreiben, Werbe- und Lead-Texte; dazu Office-Aufgaben wie " +
            "Excel-Formeln, Tabellen auswerten und Word-Dokumente strukturieren. Entscheidend sind Deutsch-Qualität und " +
            "ein langes nutzbares Kontextfenster in 32 GB.",
    ),
    KREATIV(
        "kreativ", "Kreatives Schreiben", "📖",
        "Gedichte, Liedtexte, Geschichten und Dialoge mit Gefühl und eigenem Ton.", LM,
        "Sprachmodelle für kreatives Schreiben auf Deutsch: Gedichte mit sauberem Reim und Rhythmus, Liedtexte, " +
            "Kurzgeschichten, Dialoge, Rollenspiel. Berücksichtige neben den Basismodellen auch beliebte Feinabstimmungen " +
            "aus der Schreib-Community, wenn sie nachweislich besser schreiben.",
    ),
    DENKEN(
        "denken", "Tiefes Denken & Mathe", "🧮",
        "Knifflige Logik, Mathematik, Planung und mehrstufige Analysen.", LM,
        "Reasoning-Modelle (mit Denkphase), die bei Mathematik, Logik, Planung und mehrstufigen Analysen am besten " +
            "abschneiden. Nenne, wie man die Denkphase einstellt, und wie viel Kontext in 32 GB realistisch bleibt.",
    ),
    SEHEN(
        "sehen", "Bilder & Dokumente verstehen", "👁️",
        "Fotos, Screenshots, Tabellen und PDFs lesen, beschreiben und auswerten (auch Handschrift).", LM,
        "Vision-Sprachmodelle, die Bilder, Screenshots, Diagramme, Tabellen und gescannte Dokumente am besten verstehen, " +
            "sowie die besten OCR- bzw. Dokument-Modelle für deutsche Texte. Nenne, ob die mmproj-Datei bzw. der " +
            "Vision-Teil im genannten Repo enthalten ist.",
    ),
    SPRACHE_ERKENNEN(
        "sprache_erkennen", "Sprache erkennen", "🎙️",
        "Gesprochenes Deutsch in Text verwandeln: Diktate, Besprechungen, Videos.", "whisper.cpp, faster-whisper oder ein eigenes Werkzeug",
        "Spracherkennungsmodelle (Speech-to-Text) mit der niedrigsten Fehlerrate für Deutsch, auch bei Dialekt, " +
            "Fachwörtern und Hintergrundgeräuschen; dazu Sprechertrennung. Nenne Echtzeitfaktor auf der Grafikkarte.",
    ),
    SPRACHE_AUSGEBEN(
        "sprache_ausgeben", "Vorlesen & Stimmen", "🔊",
        "Text natürlich auf Deutsch vorlesen, eigene Stimme klonen, Gefühle in der Stimme.", "ein eigenes Werkzeug oder ComfyUI",
        "Sprachausgabe-Modelle (Text-to-Speech) mit der natürlichsten deutschen Stimme, gutem Stimmklonen aus kurzer " +
            "Probe und steuerbarer Betonung. Nenne ausdrücklich, wie gut Deutsch unterstützt wird, und die Lizenz.",
    ),
    MUSIK(
        "musik", "Musik & Klang", "🎵",
        "Lieder mit Gesang, Instrumentalstücke und Geräusche erzeugen.", "ComfyUI oder ein eigenes Werkzeug",
        "Modelle, die Musik mit Gesang (auch deutscher Text), Instrumentalmusik und Geräusche/Soundeffekte in der " +
            "besten Klangqualität erzeugen. Nenne maximale Stücklänge und Lizenz.",
    ),
    UEBERSETZEN(
        "uebersetzen", "Übersetzen", "🌍",
        "Texte zwischen Deutsch, Englisch und weiteren Sprachen treffsicher übertragen.", LM,
        "Modelle mit der besten Übersetzungsqualität von und nach Deutsch (vor allem Englisch, dazu weitere europäische " +
            "und asiatische Sprachen), auch für lange Dokumente und Fachtexte. Berücksichtige spezialisierte " +
            "Übersetzungsmodelle ebenso wie Allzweckmodelle.",
    ),
    AGENTEN(
        "agenten", "Agenten & Werkzeuge", "🤖",
        "Modelle, die selbstständig Werkzeuge bedienen, im Netz suchen und Aufgaben in vielen Schritten erledigen.", LM,
        "Sprachmodelle, die Werkzeugaufrufe (Function Calling, MCP) am zuverlässigsten beherrschen und lange " +
            "mehrstufige Aufgaben ohne Abdriften durchhalten. Nenne, ob die Chat-Vorlage in LM Studio Werkzeugaufrufe " +
            "sauber unterstützt, und das nutzbare Kontextfenster in 32 GB.",
    ),
    WISSEN(
        "wissen", "Dokumentensuche (RAG)", "📚",
        "Eigene Dokumente durchsuchbar machen: Einbettungs- und Sortiermodelle für deutsche Texte.", LM,
        "Embedding-Modelle und Reranker mit der besten Trefferqualität für deutsche und mehrsprachige Texte (MTEB, " +
            "besonders die deutschen bzw. mehrsprachigen Wertungen). Nenne Vektorgröße, maximale Textlänge und VRAM-Bedarf.",
    ),
    DREI_D(
        "drei_d", "3D-Modelle erstellen", "🧊",
        "Aus einem Bild oder Text ein 3D-Objekt mit Oberfläche erzeugen, etwa für Druck oder Spiele.", "ComfyUI oder ein eigenes Werkzeug",
        "Bild-zu-3D- und Text-zu-3D-Modelle mit der besten Geometrie- und Texturqualität, die auf 32 GB VRAM laufen. " +
            "Nenne das Ausgabeformat und ob Texturen mit erzeugt werden.",
    ),
    TEMPO(
        "tempo", "Schnell & sparsam", "⚡",
        "Kleine Modelle, die sofort antworten und VRAM für anderes frei lassen.", LM,
        "Kleine Sprachmodelle (grob bis 16 GB VRAM), die bei sehr hoher Geschwindigkeit die beste Qualität liefern – " +
            "für schnelle Antworten, Sprachassistenten und Hintergrundaufgaben, während daneben noch ein anderes Modell " +
            "oder Spiel läuft. Nenne Tokens pro Sekunde auf einer RTX 5090, soweit belegt.",
    ),
    ;

    /** Ob die Modelle dieses Bereichs in LM Studio geladen werden (dann ist der Suchtext für LM Studio gefragt). */
    val inLmStudio: Boolean get() = laufzeit == LM
}
