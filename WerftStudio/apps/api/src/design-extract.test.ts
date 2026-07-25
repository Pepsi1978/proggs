import { describe, expect, it } from "vitest";
import { resolveDrawable, shapeDrawableToCss, vectorDrawableToSvg } from "./android-drawable.js";
import { extractDesignFacts, factCandidatePaths, orderedScreens } from "./design-extract.js";
import { dimensionToPx, normalizeHexColor, renderFactSheet, rgbaFromArgb } from "./design-facts.js";
import { elevationToShadow, swiftShadowToCss, wpfDropShadowToCss } from "./effect-catalog.js";
import { extractWindowsFacts, thicknessToCss } from "./extract-windows.js";
import { parseXml } from "./xml-lite.js";

const source = (path: string, text: string) => ({ path, text });

describe("Farb- und Maßumrechnung", () => {
  it("liest Android-ARGB in der richtigen Kanalreihenfolge", () => {
    // #FF6200EE ist volldeckendes Lila — ohne Kanaldrehung landet das Alpha im Rotkanal.
    expect(normalizeHexColor("#FF6200EE")).toBe("#6200ee");
    expect(rgbaFromArgb("806200EE")).toBe("rgba(98, 0, 238, 0.502)");
    expect(normalizeHexColor("#F00")).toBe("#ff0000");
  });

  it("behandelt dp, sp und pt als CSS-Pixel und rechnet nur physische Einheiten um", () => {
    expect(dimensionToPx("16dp")).toBe(16);
    expect(dimensionToPx("14sp")).toBe(14);
    expect(dimensionToPx("17pt")).toBe(17);
    expect(dimensionToPx("0.5in")).toBe(48);
    expect(dimensionToPx("wrap_content")).toBeUndefined();
  });
});

describe("Effektumrechnung", () => {
  it("bildet Material-Elevation auf die offiziellen Schattenstufen ab", () => {
    expect(elevationToShadow(0)).toBe("none");
    expect(elevationToShadow(1)).toContain("0px 1px 3px 1px");
    expect(elevationToShadow(6)).toContain("0px 4px 8px 3px");
    // Zwischenwerte werden interpoliert statt auf eine Stufe gerundet.
    const between = elevationToShadow(4);
    expect(between).not.toBe(elevationToShadow(3));
    expect(between).not.toBe(elevationToShadow(6));
  });

  it("dreht die WPF-Schattenrichtung korrekt in Bildschirmkoordinaten", () => {
    // Direction 270 zeigt in WPF nach unten; in CSS ist das ein positiver Y-Versatz.
    expect(wpfDropShadowToCss({ shadowDepth: 4, direction: 270, blurRadius: 10, opacity: 0.5 })).toBe("0px 4px 10px rgba(0, 0, 0, 0.5)");
    expect(wpfDropShadowToCss({ shadowDepth: 4, direction: 0, blurRadius: 8, opacity: 1 })).toBe("4px 0px 8px #000000");
  });

  it("verdoppelt den SwiftUI-Radius für dieselbe optische Weichheit in CSS", () => {
    expect(swiftShadowToCss({ radius: 8, x: 0, y: 4, color: "#000000", opacity: 0.2 })).toBe("0px 4px 16px rgba(0, 0, 0, 0.2)");
  });
});

describe("Android-Drawables", () => {
  it("wandelt VectorDrawables in einsetzbare SVGs statt Ersatzsymbole", () => {
    const svg = vectorDrawableToSvg(`<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
  <path android:fillColor="#FF6200EE" android:pathData="M12,2L2,22h20z"/>
  <path android:strokeColor="#FF000000" android:strokeWidth="2" android:strokeLineCap="round" android:pathData="M4,4h16"/>
</vector>`);
    expect(svg).toContain('viewBox="0 0 24 24"');
    expect(svg).toContain('d="M12,2L2,22h20z"');
    expect(svg).toContain('fill="#6200ee"');
    expect(svg).toContain('stroke-width="2"');
    expect(svg).toContain('stroke-linecap="round"');
  });

  it("übernimmt Gruppen-Rotation samt Pivot", () => {
    const svg = vectorDrawableToSvg(`<vector android:viewportWidth="24" android:viewportHeight="24"><group android:rotation="45" android:pivotX="12" android:pivotY="12"><path android:pathData="M0,0h4"/></group></vector>`);
    expect(svg).toContain("translate(12 12) rotate(45) translate(-12 -12)");
  });

  it("übersetzt Shape-Drawables in exakte Button-Optik", () => {
    const css = shapeDrawableToCss(`<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
  <solid android:color="#FF3157D5"/>
  <corners android:radius="12dp"/>
  <stroke android:width="1dp" android:color="#33000000"/>
</shape>`);
    expect(css).toContain("background-color: #3157d5");
    expect(css).toContain("border-radius: 12px");
    expect(css).toContain("border: 1px solid rgba(0, 0, 0, 0.2)");
  });

  it("dreht Android-Verlaufswinkel in die CSS-Zählrichtung", () => {
    // Android 0° = nach rechts (gegen den Uhrzeigersinn), CSS 90deg = nach rechts.
    expect(shapeDrawableToCss(`<shape><gradient android:startColor="#FFFFFFFF" android:endColor="#FF000000" android:angle="0"/></shape>`)).toContain("linear-gradient(90deg");
    expect(resolveDrawable(`<ripple xmlns:android="http://schemas.android.com/apk/res/android" android:color="#1F000000"/>`)?.css).toContain("--ripple-color");
  });
});

describe("Android-Faktenextraktion", () => {
  const files = [
    source("app/src/main/res/values/colors.xml", `<resources><color name="brand">#FF3157D5</color><color name="scrim">#66000000</color></resources>`),
    source("app/src/main/res/values-night/colors.xml", `<resources><color name="brand">#FF9DB4FF</color></resources>`),
    source("app/src/main/res/values/dimens.xml", `<resources><dimen name="card_padding">18dp</dimen><dimen name="title_size">22sp</dimen></resources>`),
    source("app/src/main/res/values/themes.xml", `<resources><style name="Theme.Acme" parent="Theme.Material3.DayNight"><item name="colorPrimary">@color/brand</item><item name="android:elevation">6dp</item></style></resources>`),
    source("app/src/main/res/values/strings.xml", `<resources><string name="app_name">Acme</string></resources>`),
    source("app/src/main/res/layout/activity_main.xml", `<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent"><TextView android:id="@+id/title" android:layout_width="wrap_content" android:layout_height="wrap_content" android:paddingTop="@dimen/card_padding" android:textSize="22sp" android:textColor="@color/brand" android:elevation="3dp"/><Button android:id="@+id/go" android:layout_width="120dp" android:layout_height="48dp" android:background="@color/brand" android:alpha="0.8"/></LinearLayout>`),
    source("app/src/main/AndroidManifest.xml", `<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application><activity android:name=".MainActivity"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity></application></manifest>`),
    source("app/src/main/java/acme/ui/theme/Color.kt", `val Purple40 = Color(0xFF6650a4)\nval Surface = Color(0xFFFFFBFE)`),
    source("app/src/main/java/acme/ui/theme/Theme.kt", `private val LightColors = lightColorScheme(\n  primary = Purple40,\n  surface = Surface\n)`),
    source("app/src/main/java/acme/ui/theme/Type.kt", `val titleLarge = TextStyle(\n  fontSize = 22.sp,\n  lineHeight = 28.sp,\n  fontWeight = FontWeight.SemiBold\n)`),
    source("app/src/main/java/acme/ui/HomeScreen.kt", `@Composable\nfun HomeScreen(modifier: Modifier = Modifier) {\n  Column(modifier.padding(16.dp).shadow(4.dp).background(Purple40)) {\n    Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3157D5))) { }\n  }\n}`),
    source("app/src/main/java/acme/ui/Nav.kt", `NavHost(navController, startDestination = "home") {\n  composable("home") { HomeScreen() }\n  composable("details") { DetailsScreen() }\n}`)
  ];
  const facts = extractDesignFacts("android", files);

  it("liest Farben inklusive Nacht-Variante und Alpha exakt", () => {
    expect(facts.colors.find((color) => color.name.startsWith("brand"))?.css).toBe("#3157d5");
    expect(facts.colors.find((color) => color.name.startsWith("brand@night"))?.css).toBe("#9db4ff");
    expect(facts.colors.find((color) => color.name.startsWith("scrim"))?.css).toBe("rgba(0, 0, 0, 0.4)");
    expect(facts.colors.find((color) => color.name.startsWith("Purple40"))?.css).toBe("#6650a4");
  });

  it("misst Maße aus Ressourcen, Layouts und Compose-Modifiern", () => {
    const values = facts.dimensions.map((dimension) => dimension.px);
    expect(values).toContain(18);
    expect(values).toContain(120);
    expect(values).toContain(48);
    expect(values).toContain(16);
  });

  it("löst Theme-Token, Typografie und Elevation auf", () => {
    expect(facts.themes.some((theme) => theme.tokens.colorPrimary === "#3157d5")).toBe(true);
    expect(facts.themes.some((theme) => theme.tokens.primary === "#6650a4")).toBe(true);
    expect(facts.typography.some((type) => type.sizePx === 22 && type.weight === 600 && type.lineHeightPx === 28)).toBe(true);
    expect(facts.effects.some((effect) => effect.kind === "shadow" && effect.css === elevationToShadow(6))).toBe(true);
    expect(facts.effects.some((effect) => effect.kind === "shadow" && effect.css === elevationToShadow(3))).toBe(true);
    expect(facts.effects.some((effect) => effect.kind === "opacity" && effect.css === "opacity: 0.8")).toBe(true);
  });

  it("erkennt Bildschirme, Startbildschirm und Navigationsziele", () => {
    expect(facts.screens.find((screen) => screen.id === "compose:HomeScreen")?.route).toBe("home");
    expect(facts.screens.find((screen) => screen.id === "compose:HomeScreen")?.isStart).toBe(true);
    expect(facts.screens.some((screen) => screen.id === "compose:DetailsScreen")).toBe(true);
    expect(facts.screens.find((screen) => screen.id === "activity:MainActivity")?.isStart).toBe(true);
    expect(facts.screens.find((screen) => screen.id === "layout:activity_main")?.files).toEqual(["app/src/main/res/layout/activity_main.xml"]);
  });

  it("führt Route und Composable zu EINEM Bildschirm zusammen statt ihn doppelt aufzubauen", () => {
    // `composable("home") { HomeScreen() }` beschreibt denselben Bildschirm wie die Funktion
    // HomeScreen — zwei Einträge hieße zwei teure KI-Aufbauläufe für dasselbe Ergebnis.
    expect(facts.screens.filter((screen) => screen.id === "route:home")).toHaveLength(0);
    expect(facts.screens.filter((screen) => screen.id === "route:details")).toHaveLength(0);
    expect(new Set(facts.screens.map((screen) => screen.id)).size).toBe(facts.screens.length);
    // Die Quelldatei des Composables bleibt erhalten, damit der Aufbau den Originalcode sieht.
    expect(facts.screens.find((screen) => screen.id === "compose:HomeScreen")?.files).toContain("app/src/main/java/acme/ui/HomeScreen.kt");
  });

  it("baut keine leere Activity-Hülle als zusätzlichen Bildschirm auf", () => {
    // Bei Compose-/Layout-Apps ist die Activity nur der Container; ihr einziger eigener Beitrag
    // ist die Information, welcher Bildschirm beim Start sichtbar ist.
    const ordered = orderedScreens(facts);
    expect(ordered.some((screen) => screen.id.startsWith("activity:"))).toBe(false);
    expect(ordered.some((screen) => screen.isStart)).toBe(true);
    expect(ordered[0]?.isStart).toBe(true);
  });

  it("markiert am Element gesetzte Farben als verwendet, bloße Definitionen nicht", () => {
    // Im Layout per @color/brand und in Compose per background()/containerColor gesetzte Farben.
    expect(facts.colors.filter((color) => color.used && color.css === "#3157d5").length).toBeGreaterThan(0);
    expect(facts.colors.some((color) => color.used && color.css === "#6650a4")).toBe(true);
    // Eine reine colors.xml-Definition ohne Verwendung darf nie nachgemessen werden.
    expect(facts.colors.find((color) => color.name.startsWith("scrim"))?.used).toBeFalsy();
  });

  it("schreibt jeden Wert mit Quelle in das verbindliche Faktenblatt", () => {
    const sheet = renderFactSheet(facts);
    expect(sheet).toContain("VERBINDLICHE DESIGN-FAKTEN (android)");
    expect(sheet).toContain("#3157d5");
    expect(sheet).toContain("app/src/main/res/values/colors.xml");
    expect(sheet).toContain("STARTSCREEN");
  });

  it("wählt nur designtragende Dateien als Messquellen aus", () => {
    const candidates = factCandidatePaths("android", ["app/src/main/res/values/colors.xml", "app/src/main/java/acme/Repository.kt", "app/build/reports/index.html", "README.md"]);
    expect(candidates).toContain("app/src/main/res/values/colors.xml");
    expect(candidates).not.toContain("README.md");
  });
});

describe("Windows-Faktenextraktion", () => {
  it("dreht Thickness in die CSS-Reihenfolge", () => {
    // XAML: links,oben,rechts,unten — CSS: oben,rechts,unten,links.
    expect(thicknessToCss("4,8,12,16")).toBe("8px 12px 16px 4px");
    expect(thicknessToCss("6,10")).toBe("10px 6px");
    expect(thicknessToCss("5")).toBe("5px");
  });

  it("liest Ressourcen, Fenstergeometrie, Typografie und Schatten", () => {
    const facts = extractWindowsFacts([
      source("App/Themes/Generic.xaml", `<ResourceDictionary><SolidColorBrush x:Key="AccentBrush" Color="#FF3157D5"/><SolidColorBrush x:Key="CardBrush" Color="#FFFFFFFF"/></ResourceDictionary>`),
      source("App/MainWindow.xaml", `<Window x:Class="App.MainWindow" Title="Acme" Width="1100" Height="720"><Border CornerRadius="12" Padding="16,20,16,20"><Border.Effect><DropShadowEffect BlurRadius="16" ShadowDepth="4" Direction="270" Opacity="0.25" Color="#FF000000"/></Border.Effect><TextBlock FontSize="22" FontWeight="SemiBold" FontFamily="Segoe UI"/></Border></Window>`)
    ]);
    expect(facts.colors?.find((color) => color.name === "AccentBrush")?.css).toBe("#3157d5");
    expect(facts.viewport).toMatchObject({ width: 1100, height: 720 });
    expect(facts.shapes?.some((shape) => shape.radiusCss === "12px")).toBe(true);
    expect(facts.typography?.some((type) => type.sizePx === 22 && type.weight === 600 && type.family === "Segoe UI")).toBe(true);
    expect(facts.effects?.some((effect) => effect.css === "0px 4px 16px rgba(0, 0, 0, 0.25)")).toBe(true);
    expect(facts.effects?.some((effect) => effect.css === "padding: 20px 16px 20px 16px")).toBe(true);
    expect(facts.screens?.some((screen) => screen.id === "xaml:MainWindow" && screen.isStart)).toBe(true);
  });
});

describe("Apple-Faktenextraktion", () => {
  const facts = extractDesignFacts("macos", [
    source("Sources/Theme.swift", `let accent = Color(red: 0.19, green: 0.34, blue: 0.84)\nlet card = Color(hex: "#FFFFFF")`),
    source("Sources/ContentView.swift", `struct ContentView: View {\n  var body: some View {\n    VStack(spacing: 12) {\n      Text("Titel").font(.largeTitle)\n      Text("Detail").font(.system(size: 15, weight: .medium))\n    }\n    .padding(20)\n    .frame(width: 980, height: 640)\n    .cornerRadius(14)\n    .shadow(color: .black.opacity(0.18), radius: 10, x: 0, y: 6)\n  }\n}`),
    source("Assets.xcassets/Brand.colorset/Contents.json", `{"colors":[{"color":{"components":{"red":"0.192","green":"0.341","blue":"0.835","alpha":"1.000"}}}]}`)
  ]);

  it("misst SwiftUI-Farben, Typografie, Radien und Schatten exakt", () => {
    expect(facts.colors.some((color) => color.css === "#3157d5")).toBe(true);
    expect(facts.colors.some((color) => color.name.startsWith("Brand"))).toBe(true);
    expect(facts.typography.some((type) => type.sizePx === 34)).toBe(true);
    expect(facts.typography.some((type) => type.sizePx === 15 && type.weight === 500)).toBe(true);
    expect(facts.shapes.some((shape) => shape.radiusCss === "14px")).toBe(true);
    expect(facts.effects.some((effect) => effect.css === "0px 6px 20px rgba(0, 0, 0, 0.18)")).toBe(true);
    expect(facts.dimensions.some((dimension) => dimension.px === 980)).toBe(true);
    expect(facts.screens.some((screen) => screen.id === "apple:ContentView" && screen.isStart)).toBe(true);
  });
});

describe("XML-Leser", () => {
  it("erhält Verschachtelung, Attribute und Text auch ohne Namensraum-Auflösung", () => {
    const root = parseXml(`<root a="1"><child b="2">Text</child><self-closing c="3"/></root>`);
    expect(root?.tag).toBe("root");
    expect(root?.attributes.a).toBe("1");
    expect(root?.children).toHaveLength(2);
    expect(root?.children[0]?.text).toBe("Text");
    expect(root?.children[1]?.tag).toBe("self-closing");
  });

  it("überspringt Kommentare, Deklarationen und CDATA-Inhalte korrekt", () => {
    const root = parseXml(`<?xml version="1.0"?><!-- Kommentar --><resources><string name="a"><![CDATA[Roh & Text]]></string></resources>`);
    expect(root?.tag).toBe("resources");
    expect(root?.children[0]?.text).toBe("Roh & Text");
  });
});
