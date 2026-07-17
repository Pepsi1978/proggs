package de.frank.fisetinbegleiter.data

object SeedData {
    val ingredients = listOf(
        IngredientEntity(phase = IngredientPhase.DRINK, name = "Fisetin", amount = "2.000 mg", note = "Pulver oder Kapseln, laborgeprüft", sortOrder = 1),
        IngredientEntity(phase = IngredientPhase.DRINK, name = "Piperin", amount = "15–20 mg", note = "Zusammen mit Fisetin im Drink", sortOrder = 2),
        IngredientEntity(phase = IngredientPhase.DRINK, name = "MCT-Öl oder Olivenöl", amount = "10 ml", note = "Fettträger für die Aufnahme", sortOrder = 3),
        IngredientEntity(phase = IngredientPhase.DRINK, name = "Sonnenblumenlecithin", amount = "5–10 g (1–2 TL)", note = "Verbessert die Bioverfügbarkeit", sortOrder = 4),
        IngredientEntity(phase = IngredientPhase.DRINK, name = "Lauwarmes Wasser oder grüner Tee", amount = "200–250 ml", note = "Trägerflüssigkeit", sortOrder = 5),
        IngredientEntity(phase = IngredientPhase.SPERMIDIN, name = "Piperin", amount = "10–15 mg", note = "Als Kapsel", optional = true, sortOrder = 1),
        IngredientEntity(phase = IngredientPhase.SPERMIDIN, name = "Spermidin", amount = "6 mg", note = "Fester Bestandteil", sortOrder = 2),
        IngredientEntity(phase = IngredientPhase.SPERMIDIN, name = "Leichte Mahlzeit", amount = "1 Portion", note = "Zusammen einnehmen", sortOrder = 3),
    )

    val stackItems: List<StackItemEntity> = buildList {
        var order = 0
        fun addItem(name: String, dose: String, category: StackCategory, medication: Boolean = false, note: String = "") {
            add(StackItemEntity(name = name, usualDose = dose, category = category, isMedication = medication, note = note, sortOrder = order++))
        }
        addItem("Vitamin C (Kapseln)", "80 mg × 2", StackCategory.HARTE_SPERRE, note = "Direktes starkes Antioxidans")
        addItem("Eisen-Bisglycinat mit Vitamin C", "enthält 80 mg Vitamin C", StackCategory.HARTE_SPERRE, note = "Versteckte Falle: Eisen-Kapsel verschieben oder für die Kur ein Präparat ohne Vitamin C verwenden.")
        addItem("Vitamin E Komplex", "1 Portion", StackCategory.HARTE_SPERRE, note = "Fettlösliches Antioxidans")
        addItem("Curcumin Phytosome", "500 mg", StackCategory.HARTE_SPERRE, note = "Starkes Antioxidans/Entzündungshemmer")
        addItem("CoQ10 (Ubiquinol)", "200 mg", StackCategory.HARTE_SPERRE, note = "Antioxidans")
        addItem("Astaxanthin", "12 mg", StackCategory.HARTE_SPERRE, note = "Sehr potentes Antioxidans")
        addItem("Selen", "200 µg, alle 3 Tage", StackCategory.GRENZFALL, note = "Antioxidativer Kofaktor; im Zweifel verschieben.")
        addItem("Nicotinamid-Ribosid", "300 mg", StackCategory.GRENZFALL, note = "Im Zweifel hinter das 4-Stunden-Fenster verschieben.")
        addItem("Apigenin", "200 mg", StackCategory.GRENZFALL, note = "Im Zweifel hinter das 4-Stunden-Fenster verschieben.")
        addItem("R-Alpha-Liponsäure", "Sport-Block", StackCategory.GRENZFALL, note = "Falls im Fenster fällig, verschieben.")
        addItem("PQQ", "Sport-Block", StackCategory.GRENZFALL, note = "Falls im Fenster fällig, verschieben.")
        addItem("Fucoxanthin", "Sport-Block", StackCategory.GRENZFALL, note = "Falls im Fenster fällig, verschieben.")
        addItem("Venlafaxin", "100 mg Arbeitstag / 50 mg freier Tag", StackCategory.UNPROBLEMATISCH, medication = true, note = "Medikament – immer einnehmen, nie wegen der Kur weglassen.")
        listOf(
            "L-Theanin", "L-Tyrosin", "Vitamin-B-Komplex", "Hyaluronsäure", "Löwenmähne",
            "Uridin Monophosphat", "Citicolin", "EAAs + Kollagen", "Kreatin",
            "Acetyl-L-Carnitin", "Vitamin D3+K2", "DHEA", "Ashwagandha KSM-66",
            "Magnesium (beide Formen)", "Kupfer", "Mangan", "Phosphatidylserin",
            "Huperzin A", "Ginkgo", "TMG", "MSM", "Bor", "Kaffee",
        ).forEach { addItem(it, "wie im Morgen-Stack", StackCategory.UNPROBLEMATISCH) }
    }
}
