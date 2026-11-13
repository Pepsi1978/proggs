using System.Windows;
using System.Windows.Controls;
using static JarvisWindows.Bau;

namespace JarvisWindows;

/// <summary>Einstellungen → Zentrale &amp; Bereiche: Karten der Zentrale (zeigen, Reihenfolge), Listen-Abgleich, Biomarker und Solarbatterie.
/// Jede Einstellung steht auch im Katalog von windows_einstellungen, damit Frank sie per Sprache abfragen und umstellen kann.</summary>
internal sealed class BereicheEinstellungen : UserControl
{
    public static readonly (string Id, string Titel)[] ZentraleKarten =
    {
        ("pc", "Am Laptop"), ("auswertung", "Tagesauswertung"), ("biomarker", "Biomarker"), ("solar", "Solarbatterie"), ("aufgaben", "Aufgaben heute"), ("termine", "Termine"), ("wetter", "Wetter"),
    };

    private readonly Einstellungen einst = Einstellungen.Shared;

    public BereicheEinstellungen() => Bauen();

    private void Bauen()
    {
        var ausgeblendet = Ausgeblendet();
        var karten = V(8, Titel("sparkles", "Zentrale"),
            T("Karten lassen sich in der Zentrale per Drag & Drop verschieben. Hier blendest du sie ein oder aus.", 12, null, Farbe.Text2));
        foreach (var (id, titel) in ZentraleKarten)
        {
            var k = id;
            VAnfuegen(karten, EinstellungenAnsicht.Zeile(titel, "", new Schalter(() => !Ausgeblendet().Contains(k), an =>
            {
                var l = Ausgeblendet();
                if (an) l.Remove(k); else if (!l.Contains(k)) l.Add(k);
                einst.ZentraleAusgeblendet = string.Join(",", l);
            })), 2);
        }
        VAnfuegen(karten, EinstellungenAnsicht.Zeile("Reihenfolge zurücksetzen", "Stellt die Werks-Reihenfolge der Zentrale wieder her.",
            new KapselKnopf("arrow.uturn.backward", "Zurücksetzen", () => { einst.ZentraleReihenfolge = ""; Log("einstellung", "Zentrale-Reihenfolge zurückgesetzt", wo: "Einstellungen"); })), 6);

        var listen = Karte(V(8, Titel("checklist", "Listen", Farbe.Gruen),
            EinstellungenAnsicht.Zeile("Mit Handy und Tablet abgleichen", "Der Reiter Listen zeigt dieselben Listen wie auf Handy und Tablet. Aus: Der Laptop zeigt und ändert nur seine Kopie, nichts geht hoch, bis du es wieder einschaltest.",
                new Schalter(() => einst.ListenAbgleichAn, an => { einst.ListenAbgleichAn = an; ListenSpeicher.Shared.JetztAbgleichen(); })),
            EinstellungenAnsicht.Zeile("Jetzt abgleichen", "Holt sofort den Stand vom Server und schickt Offenes los.", new KapselKnopf("arrow.clockwise", "Abgleichen", ListenSpeicher.Shared.JetztAbgleichen))));

        var bio = Karte(V(8, Titel("bolt.heart.fill", "Biomarker", Farbe.Rot),
            T("Der Laptop ist ein Spiegel des Handys: Abgerufen wird nur am Handy (Whoop verträgt einen Abrufer). Die fachlichen Biomarker-Einstellungen (Quellen, Anmeldungen, Abrufzeiten) stehen am Handy.", 12, null, Farbe.Text2),
            EinstellungenAnsicht.Zeile("Zeitraum", "Vergleich und Verlauf im Biomarker-Bereich.",
                Segment(new[] { ("7", "7 Tage"), ("30", "30 Tage"), ("90", "90 Tage") }, () => einst.BiomarkerTage.ToString(), v => { einst.BiomarkerTage = int.Parse(v); Ui.Starte(() => BioDaten.Shared.Laden(tage: einst.BiomarkerTage)); })),
            EinstellungenAnsicht.Zeile("Beim Öffnen der Zentrale lesen", "Liest Auswertung und Verläufe vom Handy, höchstens alle fünf Minuten.",
                new Schalter(() => einst.BiomarkerBeimStart, an => einst.BiomarkerBeimStart = an))));

        var solar = Karte(V(8, Titel("sun.max.fill", "Solarbatterie", Farbe.Gold),
            EinstellungenAnsicht.Zeile("Beim Start lesen", "Einmal je App-Start von selbst, sobald die Zentrale aufgeht (wie am Handy).",
                new Schalter(() => einst.SolarBeimStart, an => einst.SolarBeimStart = an)),
            EinstellungenAnsicht.Zeile("Regelmäßig lesen", "Zusätzlich im Takt, solange Jarvis läuft; nie während eines Sprachgesprächs.",
                Auswahl(new (int, string)[] { (0, "nur Start und Knopf"), (5, "alle 5 Minuten"), (15, "alle 15 Minuten"), (30, "alle 30 Minuten"), (60, "jede Stunde") },
                    () => einst.SolarTaktMinuten, v => einst.SolarTaktMinuten = v, 220))));

        Content = V(18, Karte(karten), listen, bio, solar);
    }

    private List<string> Ausgeblendet() => einst.ZentraleAusgeblendet.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries).ToList();
}
