# Design-Spezifikation — PerfectMoment
Alle Werte in dieser Datei sind **deterministisch aus den Projektquellen gemessen**, nicht geschätzt.
Sie sind für die Umsetzung verbindlich.
- Plattform: android
- Bildschirme im Design: 18
- Erscheinungen: DarkPmColors (Dunkel) (`darkpmcolors`, dark), LightPmColors (`lightpmcolors`, light)
- Quellgeometrie: 412×915 px
# VERBINDLICHE DESIGN-FAKTEN (android) — deterministisch aus den Quellen geparst, nicht geschätzt

## Farben (26)
colors.success = #6fa860  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
DarkPmColors.background = #181209  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.surface = #251c10  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.surface2 = #332717  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.gold = #d4a24c  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.goldHi = #f0c97a  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.goldDim = #9a7c40  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.amber = #e8873b  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.text1 = #f5eee2  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.text2 = #b3a68f  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.text3 = #786a57  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.warning = #c4634a  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
DarkPmColors.breath = rgba(212, 162, 76, 0.1294)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.background = #fbf6ec  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.surface = #f3ead9  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.surface2 = #ede1ca  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.gold = #a87a2a  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.goldHi = #7a5518  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.goldDim = #c7ae7e  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.amber = #c4661f  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.text1 = #241d12  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.text2 = #6b5d48  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.text3 = #a2947c  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.warning = #a33f28  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors.breath = rgba(168, 122, 42, 0.0706)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
success = #6fa860  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]

## Themes (2)
DarkPmColors „DarkPmColors (Dunkel)“: background=#181209, surface=#251c10, surface2=#332717, gold=#d4a24c, goldHi=#f0c97a, goldDim=#9a7c40, amber=#e8873b, text1=#f5eee2, text2=#b3a68f, text3=#786a57, warning=#c4634a, breath=rgba(212, 162, 76, 0.1294)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
LightPmColors „LightPmColors“: background=#fbf6ec, surface=#f3ead9, surface2=#ede1ca, gold=#a87a2a, goldHi=#7a5518, goldDim=#c7ae7e, amber=#c4661f, text1=#241d12, text2=#6b5d48, text3=#a2947c, warning=#a33f28, breath=rgba(168, 122, 42, 0.0706)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]

## Maße (48)
PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt:padding(10dp) ×9 = 10px (10.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:width(12dp) ×9 = 12px (12.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt:padding(8dp) ×7 = 8px (8.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt:padding(16dp) ×6 = 16px (16.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt:padding(20dp) ×5 = 20px (20.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:size(24dp) ×5 = 24px (24.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt:padding(48dp) ×4 = 48px (48.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:size(26dp) ×4 = 26px (26.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:size(18dp) ×4 = 18px (18.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:padding(14dp) ×4 = 14px (14.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt:padding(28dp) ×3 = 28px (28.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt:padding(40dp) ×3 = 40px (40.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:size(52dp) ×3 = 52px (52.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:size(32dp) ×3 = 32px (32.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:padding(2dp) ×3 = 2px (2.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:height(1dp) ×3 = 1px (1.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt:size(96dp) ×2 = 96px (96.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt:size(42dp) ×2 = 42px (42.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:height(56dp) ×2 = 56px (56.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:padding(3dp) ×2 = 3px (3.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:height(44dp) ×2 = 44px (44.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:padding(6dp) ×2 = 6px (6.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt:padding(36dp) ×2 = 36px (36.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:padding(38dp) ×2 = 38px (38.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:padding(72dp) ×2 = 72px (72.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:padding(22dp) ×2 = 22px (22.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:padding(4dp) ×2 = 4px (4.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:size(50dp) = 50px (50.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt:height(58dp) = 58px (58.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:padding(108dp) = 108px (108.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:height(188dp) = 188px (188.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:size(168dp) = 168px (168.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:size(27dp) = 27px (27.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:size(64dp) = 64px (64.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:padding(13dp) = 13px (13.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:width(220dp) = 220px (220.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:size(88dp) = 88px (88.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:size(30dp) = 30px (30.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:width(126dp) = 126px (126.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:size(23dp) = 23px (23.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:height(140dp) = 140px (140.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:padding(5dp) = 5px (5.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:height(180dp) = 180px (180.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:height(68dp) = 68px (68.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:padding(7dp) = 7px (7.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:width(240dp) = 240px (240.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:height(1.5dp) = 1.5px (1.5.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
itemStep = 78px (78.dp)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]

## Typografie (7)
Theme.PerfectMoment: family=sans  [PerfectMoment/app/src/main/res/values/themes.xml]
textStyle: family=if  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
screenTitle: family=Inter, size=26px, weight=600, lineHeight=32.5px  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
section: family=Inter, size=13px, weight=500, lineHeight=17px, letterSpacing=0.8px  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
body: family=Inter, size=15px, weight=400, lineHeight=23.25px  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
question: family=Newsreader, size=20px, weight=300, lineHeight=31px  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]
mono: family=JetBrainsMono, size=13px, weight=400, lineHeight=20.8px  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt]

## Formen/Radien (3)
shape = border-radius: 20px  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
controlShape = border-radius: 14px  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
topShape = border-radius: 24px  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]

## Effekte — exakt so in CSS übernehmen (5)
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt:Dauerschleife (animation) = animation: werft-loop 1767ms linear infinite alternate  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Components.kt (infiniteRepeatable, rememberInfiniteTransition)]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt:Ein-/Ausblenden (animation) = transition: opacity 300ms cubic-bezier(0.4, 0, 0.2, 1), transform 300ms cubic-bezier(0.4, 0, 0.2, 1)  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt (AnimatedContent, AnimatedVisibility)]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:Dauerschleife (animation) = animation: werft-loop 2200ms linear infinite  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt (infiniteRepeatable, rememberInfiniteTransition)]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt:Zustandsübergang (animation) = transition: all 2200ms linear  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt (animateColorAsState, animateFloatAsState, animateTo)]
PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt:Dauerschleife (animation) = animation: werft-loop 300ms cubic-bezier(0.4, 0, 0.2, 1) infinite alternate  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/theme/PerfectMomentTheme.kt (infiniteRepeatable, rememberInfiniteTransition)]

## Screens (19)
activity:MainActivity „MainActivity“ (activity) → keine erkannte Navigation  [PerfectMoment/app/src/main/AndroidManifest.xml]
compose:AppLockedScreen „AppLockedScreen“ (composable) → compose:SessionScreen  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/MainActivity.kt]
compose:AppBottomSheet „AppBottomSheet“ (composable) → compose:DurationSheet, compose:OptionSheet, compose:IntroAnswerSheet  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt]
compose:DurationSheet „DurationSheet“ (composable) → keine erkannte Navigation  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt]
compose:OptionSheet „OptionSheet“ (composable) → keine erkannte Navigation  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt]
compose:IntroAnswerSheet „IntroAnswerSheet“ (composable) → keine erkannte Navigation  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/PerfectMomentApp.kt]
compose:StartScreen „StartScreen“ (composable, STARTSCREEN) → compose:HistoryScreen, compose:SettingsScreen, compose:ChatGptScreen, compose:DurationSheet  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:SessionScreen „SessionScreen“ (composable) → keine erkannte Navigation  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:SessionStopDialog „SessionStopDialog“ (composable) → keine erkannte Navigation  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:HistoryScreen „HistoryScreen“ (composable) → compose:HistoryDetailScreen  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:HistoryDetailScreen „HistoryDetailScreen“ (composable) → compose:DurationSheet  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:SettingsScreen „SettingsScreen“ (composable) → compose:HooksScreen, compose:SkillsScreen, compose:VoiceScreen, compose:ChatGptScreen, compose:DurationSheet, compose:OptionSheet  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:HooksScreen „HooksScreen“ (composable) → compose:HookEditorScreen  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:HookEditorScreen „HookEditorScreen“ (composable) → keine erkannte Navigation  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:SkillsScreen „SkillsScreen“ (composable) → compose:SkillEditorScreen  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:SkillEditorScreen „SkillEditorScreen“ (composable) → keine erkannte Navigation  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:VoiceScreen „VoiceScreen“ (composable) → compose:SettingsScreen  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:ChatGptScreen „ChatGptScreen“ (composable) → keine erkannte Navigation  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]
compose:RawDataScreen „RawDataScreen“ (composable) → keine erkannte Navigation  [PerfectMoment/app/src/main/java/de/frank/perfectmoment/ui/Screens.kt]

## Assets (2)
ic_app_icon (vector) → PerfectMoment/app/src/main/res/drawable/ic_app_icon.xml [SVG inline verfügbar]
ic_notification (vector) → PerfectMoment/app/src/main/res/drawable/ic_notification.xml [SVG inline verfügbar]

## Texte (2)
app_name = "Perfect Moment"
notification_channel_session = "Laufende Sitzung"

## Hinweise
Compose-Navigation ohne NavHost: „StartScreen“ ist der Startbildschirm (Aufzählungs-Navigation). Er muss beim Öffnen sichtbar sein.
Start-Activity laut Manifest: .MainActivity. Der erste sichtbare Zustand der Rekonstruktion muss dieser Activity entsprechen.
## Bildschirme und ihre Dateien
| Nr. | Bildschirm | Start | Dateien je Erscheinung |
|-----|------------|-------|------------------------|
| 1 | StartScreen (`compose:StartScreen`) | ja | `bildschirme/darkpmcolors/01-startscreen.html`<br>`bildschirme/lightpmcolors/01-startscreen.html` |
| 2 | HistoryScreen (`compose:HistoryScreen`) | — | `bildschirme/darkpmcolors/02-historyscreen.html`<br>`bildschirme/lightpmcolors/02-historyscreen.html` |
| 3 | SettingsScreen (`compose:SettingsScreen`) | — | `bildschirme/darkpmcolors/03-settingsscreen.html`<br>`bildschirme/lightpmcolors/03-settingsscreen.html` |
| 4 | ChatGptScreen (`compose:ChatGptScreen`) | — | `bildschirme/darkpmcolors/04-chatgptscreen.html`<br>`bildschirme/lightpmcolors/04-chatgptscreen.html` |
| 5 | DurationSheet (`compose:DurationSheet`) | — | `bildschirme/darkpmcolors/05-durationsheet.html`<br>`bildschirme/lightpmcolors/05-durationsheet.html` |
| 6 | HistoryDetailScreen (`compose:HistoryDetailScreen`) | — | `bildschirme/darkpmcolors/06-historydetailscreen.html`<br>`bildschirme/lightpmcolors/06-historydetailscreen.html` |
| 7 | HooksScreen (`compose:HooksScreen`) | — | `bildschirme/darkpmcolors/07-hooksscreen.html`<br>`bildschirme/lightpmcolors/07-hooksscreen.html` |
| 8 | SkillsScreen (`compose:SkillsScreen`) | — | `bildschirme/darkpmcolors/08-skillsscreen.html`<br>`bildschirme/lightpmcolors/08-skillsscreen.html` |
| 9 | VoiceScreen (`compose:VoiceScreen`) | — | `bildschirme/darkpmcolors/09-voicescreen.html`<br>`bildschirme/lightpmcolors/09-voicescreen.html` |
| 10 | OptionSheet (`compose:OptionSheet`) | — | `bildschirme/darkpmcolors/10-optionsheet.html`<br>`bildschirme/lightpmcolors/10-optionsheet.html` |
| 11 | HookEditorScreen (`compose:HookEditorScreen`) | — | `bildschirme/darkpmcolors/11-hookeditorscreen.html`<br>`bildschirme/lightpmcolors/11-hookeditorscreen.html` |
| 12 | SkillEditorScreen (`compose:SkillEditorScreen`) | — | `bildschirme/darkpmcolors/12-skilleditorscreen.html`<br>`bildschirme/lightpmcolors/12-skilleditorscreen.html` |
| 13 | AppBottomSheet (`compose:AppBottomSheet`) | — | `bildschirme/darkpmcolors/13-appbottomsheet.html`<br>`bildschirme/lightpmcolors/13-appbottomsheet.html` |
| 14 | AppLockedScreen (`compose:AppLockedScreen`) | — | `bildschirme/darkpmcolors/14-applockedscreen.html`<br>`bildschirme/lightpmcolors/14-applockedscreen.html` |
| 15 | IntroAnswerSheet (`compose:IntroAnswerSheet`) | — | `bildschirme/darkpmcolors/15-introanswersheet.html`<br>`bildschirme/lightpmcolors/15-introanswersheet.html` |
| 16 | RawDataScreen (`compose:RawDataScreen`) | — | `bildschirme/darkpmcolors/16-rawdatascreen.html`<br>`bildschirme/lightpmcolors/16-rawdatascreen.html` |
| 17 | SessionScreen (`compose:SessionScreen`) | — | `bildschirme/darkpmcolors/17-sessionscreen.html`<br>`bildschirme/lightpmcolors/17-sessionscreen.html` |
| 18 | SessionStopDialog (`compose:SessionStopDialog`) | — | `bildschirme/darkpmcolors/18-sessionstopdialog.html`<br>`bildschirme/lightpmcolors/18-sessionstopdialog.html` |
