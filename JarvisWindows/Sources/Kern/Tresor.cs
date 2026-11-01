using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace JarvisWindows;

/// <summary>Der Tresor: Schlüssel und Anmeldedaten liegen nur verschlüsselt auf der Platte (DPAPI, an den Windows-Benutzer dieses Rechners gebunden,
/// mit eigener Entropie). Eine kopierte Datei ist woanders wertlos. Entspricht dem Secure-Enclave-Tresor der Mac-Fassung; das schützt den Speicher,
/// nicht vor Programmen unter Franks Benutzer.</summary>
public static class Tresor
{
    private static readonly byte[] Entropie = Encoding.UTF8.GetBytes("JarvisWindows-Tresor-1");
    private static readonly string Ordner = Path.Combine(Pfade.Unterstuetzung, "Tresor");
    private static readonly object Schloss = new();
    private static readonly HashSet<string> Unlesbar = new();

    private static string Datei(string name) { Directory.CreateDirectory(Ordner); return Path.Combine(Ordner, name + ".enc"); }

    /// <summary>Für die Oberfläche: Text, wenn etwas im Tresor nicht lesbar war.</summary>
    public static string? Stoerung
    {
        get
        {
            lock (Schloss)
                return Unlesbar.Count == 0 ? null : $"Tresor gerade nicht lesbar ({string.Join(", ", Unlesbar.OrderBy(x => x))}). Es wird nichts überschrieben; bei Windows anmelden und Jarvis neu starten.";
        }
    }

    public static byte[] Versiegeln(byte[] klar) => ProtectedData.Protect(klar, Entropie, DataProtectionScope.CurrentUser);
    public static byte[] Oeffnen(byte[] d) => ProtectedData.Unprotect(d, Entropie, DataProtectionScope.CurrentUser);

    public static T? Lade<T>(string name) where T : class
    {
        var pfad = Datei(name);
        if (!File.Exists(pfad)) return null;
        try
        {
            return JsonSerializer.Deserialize<T>(Oeffnen(File.ReadAllBytes(pfad)));
        }
        catch (Exception ex)
        {
            // Nie überschreiben: Sonst wären die Schlüssel darin endgültig weg.
            lock (Schloss) Unlesbar.Add(name);
            Log("fehler", $"Tresor: {name} nicht lesbar, Speichern gesperrt", wo: "Tresor", fehler: ex.Message);
            return null;
        }
    }

    public static void Sichere<T>(T wert, string name)
    {
        lock (Schloss) if (Unlesbar.Contains(name)) return;
        try
        {
            var pfad = Datei(name);
            var teil = pfad + ".teil";
            File.WriteAllBytes(teil, Versiegeln(JsonSerializer.SerializeToUtf8Bytes(wert)));
            File.Move(teil, pfad, true);
        }
        catch (Exception ex)
        {
            Log("fehler", $"Tresor: {name} nicht gespeichert", wo: "Tresor", fehler: ex.Message);
        }
    }

    public static void Loesche(string name)
    {
        try { File.Delete(Datei(name)); } catch { }
        lock (Schloss) Unlesbar.Remove(name);
    }

    /// <summary>Bremse gegen untergeschobene Anweisungen: Jarvis' Werkzeuge greifen nicht auf Zugangsdaten zu. Kein vollständiger Schutz; die Schwärzung ist die zweite Stufe.</summary>
    private static readonly Regex GesperrterWeg = new(
        @"(~|\$home|\$\{home\}|%userprofile%|\$env:userprofile|c:[\\/]+users[\\/]+[^\\/\s]+)[\\/]+sk(?=[\\/\s'""]|$)|jarviswindows[\\/]+tresor|cmdkey\b|credentialmanager|vaultcmd",
        RegexOptions.Compiled);

    public static WerkzeugErgebnis? Sperre(params string?[] teile)
    {
        foreach (var teil in teile)
        {
            if (teil == null || !GesperrterWeg.IsMatch(teil.ToLowerInvariant())) continue;
            Log("tresor", "Werkzeug-Zugriff auf Zugangsdaten abgewiesen", wo: "Tresor", eingabe: teil);
            return new WerkzeugErgebnis("Abgelehnt: Zugangsdaten sind für Jarvis' Werkzeuge gesperrt. Frank sieht sie unter Einstellungen → Konten nach Windows Hello.", Fehler: true);
        }
        return null;
    }
}

/// <summary>Die aktuell bekannten Schlüsselwerte. Alles, was ins Logbuch oder ans Modell geht, läuft durch Schwaerzen.</summary>
public static class Geheimnisse
{
    private static readonly object Schloss = new();
    private static readonly Dictionary<string, List<string>> Werte = new();

    public static void Setzen(string quelle, IEnumerable<string> neu)
    {
        lock (Schloss)
            Werte[quelle] = neu.Where(x => x != null && x.Length >= 12).SelectMany(x => new[] { x, Convert.ToBase64String(Encoding.UTF8.GetBytes(x)) }).ToList();
    }

    public static string Schwaerzen(string text)
    {
        List<string> alle;
        lock (Schloss) alle = Werte.Values.SelectMany(x => x).OrderByDescending(x => x.Length).ToList();
        foreach (var w in alle) if (text.Contains(w)) text = text.Replace(w, "••••");
        return text;
    }
}

/// <summary>Ein Werkzeug fürs Modell (WerkzeugDefinition in McpKlient.swift).</summary>
public sealed record WerkzeugDefinition(string Name, string Beschreibung, JSONObjekt Schema);

/// <summary>Ergebnis eines Werkzeugaufrufs. BildPNG landet beim Modell als Bild (z. B. Bildschirmfoto). Mit `with` oder `Text`-Setzer änderbar.</summary>
public sealed record WerkzeugErgebnis(string Text, bool Fehler = false, byte[]? BildPNG = null);

public sealed record LokalesWerkzeug(WerkzeugDefinition Definition, Func<JSONObjekt, Task<WerkzeugErgebnis>> Ausfuehren);
