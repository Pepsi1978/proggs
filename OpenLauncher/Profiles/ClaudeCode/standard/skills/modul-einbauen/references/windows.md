# Einbau auf Windows — C# / .NET WPF (M2.x)

## Wohin die Dateien kommen

```
<App>/Module/<Kurzname>/
├── <Modul>.cs / .xaml / .xaml.cs   ← byte-identische Kopien
└── Anbindung.cs                     ← app-eigen
```

## Zuerst prüfen: braucht die .csproj einen Eintrag?

SDK-Projekte kompilieren alles unterhalb des Projektordners automatisch mit —
dann ist nichts zu tun. Benutzt die `.csproj` aber ausdrückliche
`<Compile Include=…>`- oder `<Page Include=…>`-Einträge, müssen die neuen
Dateien dort ergänzt werden, sonst fehlen sie stumm im Build.

```
grep -n "Compile Include\|Page Include\|EnableDefaultCompileItems" <App>/*.csproj
```

XAML-Dateien brauchen zusätzlich `<Page>` mit `Generator: MSBuild:Compile`,
wenn das Projekt nicht auf den Standard-Globs läuft.

## Mindestversion prüfen (Phase 1)

```
grep -n "TargetFramework\|UseWPF\|LangVersion" <App>/*.csproj
```

Gegen `Mindestens:` aus `MODUL.md` halten.

## Aussehen aus der Ziel-App holen

In die Anbindung, nie ins Modul:

| Was | Woher |
|---|---|
| Farben und Pinsel | `{DynamicResource …}` aus dem `ResourceDictionary` der App |
| Schrift | die `FontFamily`/`FontSize` der App |
| Texte | die Ressourcen der App |
| Dienste | Konstruktorparameter, die die App füllt |

Im Modul selbst darf kein `{StaticResource}` auf einen Schlüssel der App
zeigen — den gibt es in der nächsten App nicht. Findest du so einen Zugriff,
ist das der Abbruchfall aus Phase 1, Punkt 4.

## Häufiger Stolperstein

`x:Class` in der kopierten `.xaml` muss zum Namensraum der `.xaml.cs` passen.
Stimmt das nicht, meldet der Build eine fehlende partielle Klasse — die
Ursache steht dann in einer ganz anderen Datei, als die Meldung nahelegt.

## Abnahme

`diff -r` zwischen Bibliotheksordner und App-Kopie (die Anbindung ausnehmen)
muss leer sein. Danach die App im Debug-Profil über die `dotnet`-CLI
durchbauen, dann Regel 9: `<Version>` in der `.csproj` bumpen, committen,
pushen, deployen.

> **TVO und CVO:** Bauen und Deployen laufen dort ausschließlich über
> `~/proggs/rebuild-overlay.ps1` (Regel 17) — nie von Hand bauen, nie den
> laufenden Prozess beenden.
