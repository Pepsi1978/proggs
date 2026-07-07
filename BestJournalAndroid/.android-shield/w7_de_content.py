import os, re, html
base = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/assets/legal/de')

def load_plain(fn):
    raw = open(os.path.join(base, fn), encoding='utf-8').read()
    plain = re.sub(r'<[^>]+>', ' ', raw)
    plain = html.unescape(re.sub(r'\s+', ' ', plain))
    return raw, plain

def has(plain, *terms):
    return any(t.lower() in plain.lower() for t in terms)

# DATENSCHUTZ.html
raw, ds = load_plain('DATENSCHUTZ.html')
print("=== DATENSCHUTZ.html (%d bytes) ===" % len(raw))
checks = {
  "verantwortlicher": has(ds, 'Verantwortlich', 'Verantwortliche Stelle', 'Data Controller'),
  "anschrift": has(ds, 'Barwandt') and (bool(re.search(r'\b\d{5}\b', ds)) or has(ds,'Straße','Str.','Postanschrift','Anschrift')),
  "zweck": has(ds, 'Zweck'),
  "rechtsgrundlagen_art6": has(ds, 'Art. 6', 'Artikel 6', 'Rechtsgrundlage'),
  "empfaenger": has(ds, 'Empfänger', 'Auftragsverarbeiter', 'weitergeg'),
  "drittland": has(ds, 'Drittland', 'USA', 'Vereinigte Staaten', 'Standardvertragsklausel', 'SCC'),
  "google_mentioned": has(ds, 'Google'),
  "gemini_mentioned": has(ds, 'Gemini'),
  "groq_mentioned": has(ds, 'Groq'),
  "firebase_mentioned": has(ds, 'Firebase'),
  "speicherdauer": has(ds, 'Speicherdauer', 'Speicherfrist', 'Aufbewahrung', 'gelöscht', 'Löschung'),
  "auskunft": has(ds, 'Auskunft', 'Art. 15'),
  "loeschung_recht": has(ds, 'Löschung', 'Art. 17', 'Recht auf Vergessen'),
  "widerspruch": has(ds, 'Widerspruch', 'Art. 21'),
  "beschwerde": has(ds, 'Beschwerde', 'Aufsichtsbehörde', 'Art. 77'),
  "datenuebertragbarkeit": has(ds, 'Datenübertragbarkeit', 'Art. 20'),
  "kontakt": has(ds, 'dev.app.support@gmail.com', 'Kontakt'),
}
for k, v in checks.items():
    print("  %-22s %s" % (k, "OK" if v else "MISSING"))

# IMPRESSUM.html
raw, im = load_plain('IMPRESSUM.html')
print("\n=== IMPRESSUM.html (%d bytes) ===" % len(raw))
imc = {
  "name": has(im, 'Barwandt'),
  "anschrift": bool(re.search(r'\b\d{5}\b', im)) or has(im, 'Straße','Str.','Postanschrift'),
  "email": has(im, 'dev.app.support@gmail.com', '@'),
  "ddg_or_tmg": has(im, 'DDG', '§ 5', 'TMG', 'Verantwortlich'),
}
for k, v in imc.items():
    print("  %-22s %s" % (k, "OK" if v else "MISSING"))
print("  head:", im[:200])

# NUTZUNGSBEDINGUNGEN.html
raw, nb = load_plain('NUTZUNGSBEDINGUNGEN.html')
print("\n=== NUTZUNGSBEDINGUNGEN.html (%d bytes) ===" % len(raw))
nbc = {
  "widerrufsbelehrung": has(nb, 'Widerruf', 'Widerrufsbelehrung'),
  "14_tage": has(nb, '14 Tage', 'vierzehn Tagen', 'vierzehn Tage'),
  "muster_widerrufsformular": has(nb, 'Muster-Widerruf', 'Widerrufsformular', 'Muster für die Widerruf'),
  "para_312g_bgb": has(nb, '312g', 'BGB', 'Fernabsatz'),
  "digitale_inhalte": has(nb, 'digitale Inhalte', 'digitaler Inhalt', 'vorzeitig'),
}
for k, v in nbc.items():
    print("  %-22s %s" % (k, "OK" if v else "MISSING"))
