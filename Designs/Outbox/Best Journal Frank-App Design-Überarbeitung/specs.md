# Entropy Journal — extrahierte UI-Spezifikationen (aus BestJournalFrank Quellcode)

## Fonts (aktuell)
Exo 2 (Headings: headlineMedium 24 semibold, titleMedium 16 medium, titleSmall 14, displayLarge 32 bold), Source Sans 3 (body 16/24, 14/20; labelLarge 14), JetBrains Mono (labelMedium 12 ls .5, labelSmall 11), Caveat (Handschrift).

## Default-Theme "Profilfarbe" (Profil 0 = Zusammenfassung, accent #14B8A6 teal)
Dark: bg #121918, surface #18201F, surfaceVariant #212C2B, text #E6E1E5/#CAC4D0/#938F99, outlineVariant #2A2A2A.
Light: bg ~#EDF5F8, surface ~#FCFEFE, surfaceVariant #E9F2F1, text #1A1A2E/#5A5A70/#6E6E86.
primary=accent; primaryContainer dark=accent*0.45; secondaryContainer=accent dunkel.

## Farb-Konstanten
NeonEmerald #4CAF7D, NeonAmber #FFB300, NeonRed #FF5252, NeonCyan #4ECDC4, FeatureAccentOrange #FF8C00, WarmCopper #C25E00, SunMoonYellow #FFD54F, SunMoonGrau #666.
SummaryPalette: #5B8DEF/#6366F1/#14B8A6/#94A3B8. InsightPalette #A78BFA/#F472B6/#FBBF24/#C084FC. GoalPalette #10B981/#38BDF8/#F59E0B/#FB7185. CustomPalette #E8A838/#D4A574/#8FAE8B/#A09890.

## Bestehende 9 Themes (Anzeigenamen im Themes Manager)
Profilfarbe, Neutral (WarmCopper primär dark / Teal #00796B light), Sonnenwende (Solarized: dark bg #002B36 sf #073642 pri #268BD2; light bg #FDF6E3 sf #EEE8D5), Mitternacht (Dracula: #282A36/#343746 pri #BD93F9 sek #FF79C6; Light Alucard #FFFBEB pri #644AC9), Atelier (One Dark #282C34/#21252B pri #61AFEF; Light #FAFAFA pri #4078F2), Polarnacht (Nord #2E3440/#3B4252 pri #88C0D0; Light #ECEFF4 pri #5E81AC), Bernstein (Gruvbox #282828/#32302F pri #D79921; Light #FBF1C7 pri #B57614), Cosmos (#0A0E1A radial→#0F1729, Glas-Cards #1D212C, pri Cyan #22D3EE sek #A78BFA; Light #F5F7FB→#E8EDFA), Aurora (Light Diagonal-Gradient #D8EDDF→#E2D6F0→#F2DDE8, Cards weiß, pri #1F8E5A sek #7A6BB8 ter #E8B547; Dark #1A2B2F→#2A1F3B→#3B1F2E, Cards #2C2435 pri #7DD3A4).

## GlassCard
radius 20, padding 16, bg = surface→surfaceContainer, dezenter Glow (primary, 10-20%), Border-Schimmer outlineVariant.

## BottomNavBar (Material3)
surface-Farbe, 4 Items: auto_awesome "Rückblick", analytics "Dashboard", book "Tagebuch", settings "Einstellungen". Aktiv: primary + Pill secondaryContainer; inaktiv: outline. Swipe wechselt Tabs.

## Tagebuch-Screen
Header: "Tagebuch" headlineMedium + SunMoonToggle (light_mode/dark_mode, aktiv #FFD54F 26px, inaktiv #666 18px, 1px Divider). Rechts: Cloud-Icon (cloud_done emerald / Pfeile cyan / off rot) + "Suche"-Pill (surfaceVariant 50%, Border outline 30%, r20, search 18 + Label mono). Darunter: "N Einträge" labelSmall mono + Streak-Pill (local_fire_department + "N Tage", amber>7 sonst grau, bg 10%). Prompt-Banner (Tagesimpuls). Sektionen: Heute/Gestern/Letzte Woche… titleSmall primary + 1px Linie. Timeline: Rail 52px, 2px Linie outlineVariant, Icon-Badge 36 (Kreis primary 15%, Icon primary 20). Karte: Titel titleMedium bold underline primary (KI-Überschrift 3-4 Wörter), Datum "24. Juli 2026, 14:32 · vor 2 Std." labelMedium mono muted, Text bodyLarge max 5 Zeilen, Tag-Chips surfaceVariant r4 labelMedium. Unten Mitte: FAB edit 64 surfaceVariant + FAB mic 64 (Aufnahme: rot + rotierender Sweep-Ring cyan→violet→magenta).

## Aufnahme-Overlay
Karte r20 surface 95%, Dauer displayLarge primary (z.B. "0:42"), Waveform-Balken, roter Punkt 8 + "Lokales Whisper-Modell" labelMedium, "Erneut tippen zum Stoppen" bodyMedium.

## Dashboard
Dark: ParticleBackground + TwinklingStars hinter allem. Header "Dashboard" + Toggle; rechts info, undo (amber, optional), refresh (primary). "Zuletzt aktualisiert…" labelSmall. Profil-Karte surfaceVariant r12: "Aktives Profil: Zusammenfassung" titleSmall primary bold + Fokus bodySmall max3. Top-Aktionen-Block. GlassCard "Überblick" (Titel zentriert #5B8DEF bold, Glow blau) + Analyse bodyLarge + Zeile volume_up (#FF8C00) / share. LazyRow Kategorie-Karten 110×100: Kreis 36 (catColor 15%) + Icon 20 catColor, Name 10sp max3, Entropie-Halbbogen 36×18 (Track catColor 20%, Wert emerald/amber/rot). NeonDivider. "Alle Beobachtungen" zentriert #6366F1 + Relevanz-Legende (Punkte rot/orange/cyan = hoch/mittel/niedrig). Beobachtungs-Karten: Prioritätspunkt + Kategorie-Label + Text.

## Rückblick
Header "Rückblick" + Toggle, "Aktualisiert" labelSmall. Hero r24 Vertikal-Gradient (primary 35%→22%→10%→surface): auto_awesome 48 primary zentriert, "Dein persönlicher Rückblick" headlineSmall bold, Fließtext 80%, kursiv "Schau zurück und entdecke, was dich bewegt hat." primary. 3 aufklappbare CategoryButtons (Gradient primary 22%→surface, Icon-Kreis, expand_more): Wochenrückblick/"Die letzten 7 Tage im Überblick" calendar_today; Monatsrückblick/"Dein vergangener Monat auf einen Blick" date_range; Jahresrückblick calendar_month. Aufgeklappt: Timeline-Sektionen mit SummaryEntryCards, MonthDivider.

## Einstellungen (Reihenfolge fix)
Header "Einstellungen". GlassCard-Sektionen, Titelzeile zentriert (Icon 20 primary + titleMedium primary): 1. Konto (person; Google-Logo 40, Name+Email, "Abmelden" outlined rot; "Fotos sichern"/"Videos sichern" Switches m. Icons photo_camera/videocam; Button "Tagebucheinträge sichern" primary; "Letzte Synchronisation: …" labelMedium outline). 2. Erscheinungsbild (Zeilen icon+Titel/Untertitel+Switch: Dunkelmodus/Aktiv, System folgen/Automatisch, Sonnenauf-/untergang/"Dunkel bei Nacht, hell bei Tag"; Divider; palette 16 + "Themes Manager" bodyMedium; Dropdown-TextField surfaceVariant r12 Wert "Profilfarbe — Farbe des Dashboard-Profils" + keyboard_arrow_down). 3. Sicherheit (Fingerabdruck/PIN). 4. API-Schlüssel. 5. Gemini-Modell. 6. Aufnahme. 7. Feedback. 8. Über die App ("Entropy Journal v0.x.x", © Frank Barwandt).

## Eintrag-Detail
TopAppBar transparent, arrow_back. "Zusammenfassung"-Karte (KI-Stichpunkte). Tabs "Verbessert"|"Original" (TabRow, Indicator primary). GlassCard Glow amber: book 18 amber + "Tagebucheintrag" titleSmall amber, delete rot rechts; Datum labelSmall outline; Text bodyLarge editierbar. "Fotos und Videos"-Sektion + "Fotos/Videos hinzufügen". "Nachtrag Eins/Zwei…"-Karten (gelbes Label, delete). Share-Icon (#FF8C00), "Aufnahmedauer: 1:23" label. Unten: Schreiben (edit) + Einsprechen (mic) Buttons.

## Splash (aktuell "Gilded Sanctum")
bg #131313, Copper #FFB689/#DF741E, Gold #ECC165, OnSurface #E5E2E1, 200 Kupfer-Staub-Partikel, Hero-Bild splash_hero_book.png (Ken-Burns Zoom), Titel, Trennlinie gold, atmender Start-Button (Copper), Subtitle zuletzt. Login-Variante (Screenshot): schwarz, cyanes "E", "Entropy Journal" weiß, "Dein persönliches KI-Tagebuch für Klarheit und Veränderung" grau, weißer Pill-Button "Mit Google anmelden".

## Sonstiges
Version "Entropy Journal v0.x.x". Nav-Toggle gelb #FFD54F. Dark-Mode-Alt-Palette (SESSION-RULES): #2C3930/#3C3D37/#3F4F44/#A27B5C/#D36B00/#DCD7C9.
