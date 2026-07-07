# Cowork: Commit + Push direkt aus der Sandbox aktivieren

Damit Claude **in der Cowork-Sandbox** committen UND pushen kann, sind zwei Huerden
zu loesen. Huerde 1 macht das lokale Committen moeglich, Huerde 2 den Push.

## Huerde 1 — Mount auf „read-write" (lokales Committen)

Der verbundene `proggs`-Ordner steht in Cowork auf **„read-write, no delete"**. Git
braucht zum Committen aber das Loeschen (Lockfiles, Ref-Updates).

1. Claude-Cowork-Fenster nach vorne holen.
2. Auf den verbundenen Ordner **„proggs"** klicken (Ordner-Chip an der Eingabeleiste).
3. Zugriffsstufe von **„Read & write, no delete"** auf **„Read & write"** umstellen.

Gilt **ab der naechsten Session**.

## Huerde 2 — GitHub-Token fuer den Push (einmalig)

Die Sandbox hat keinen GitHub-Login. Loesung: ein fein-granularer Token in einer
Datei **innerhalb von `.git/`** (wird von Git nie mitcommittet, liegt nicht im
Arbeitsbaum, bleibt zwischen Sessions erhalten).

### Schritt A — Token erstellen (du)

1. GitHub → Settings → Developer settings → **Fine-grained personal access tokens**
   → „Generate new token".
2. **Resource owner:** Pepsi1978. **Repository access:** „Only select repositories"
   → **nur `Pepsi1978/proggs`**.
3. **Permissions → Repository permissions → Contents: Read and write**
   (alles andere auf „No access" lassen).
4. **Expiration:** z. B. 90 Tage (danach erneuern).
5. Token generieren und **einmal kopieren** (wird nur einmal angezeigt).

### Schritt B — Token hinterlegen (du, am Host-Terminal)

Ersetze `DEIN_TOKEN` durch den kopierten Token:

```bash
printf 'https://Pepsi1978:DEIN_TOKEN@github.com\n' > ~/proggs/.git/.cowork-credentials
chmod 600 ~/proggs/.git/.cowork-credentials
```

Die Git-Config ist bereits vorbereitet (von Claude gesetzt):
`credential.helper = store --file=.git/.cowork-credentials`.

### Fertig

Ab der naechsten Cowork-Session kann Claude mit **einem** Befehl abschliessen:

```bash
bash scripts/cowork-commit.sh "kurze Beschreibung"
```

Das raeumt Lockfiles, committet mit fortlaufender `#NNN`-Nummer, rebase't auf
`origin/main` und pusht.

## Sicherheitshinweise

- Der Token liegt **im Klartext** in `.git/.cowork-credentials`. Er ist auf genau ein
  Repo + nur „Contents" beschraenkt und laeuft ab → minimaler Schaden bei Leak.
- Die Datei liegt in `.git/` und wird daher **nie** ins Repo committet/gepusht.
- Token regelmaessig rotieren (neuen erstellen, Datei neu schreiben, alten auf GitHub
  widerrufen). Bei Verdacht sofort auf GitHub unter „revoke" loeschen.
- `git remote -v` zeigt den Token **nicht** (er steht in der Credential-Datei, nicht
  in der Remote-URL).
