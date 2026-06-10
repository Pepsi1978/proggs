import os, re, json
import xml.etree.ElementTree as ET

root = os.path.expanduser('~/proggs/BestJournalAndroid')
xml_path = os.path.join(root, 'app/src/main/res/values/strings.xml')
cp_path = os.path.join(root, '.android-shield/worker-checkpoints/fa1.jsonl')

report = {"xmlValid": None, "umlautCheck": None, "formatCheck": None,
          "details": [], "fixIdsDone": [], "missing": []}

# ---- Checkpoint append (idempotent: rewrite full list) ----
done_ids = ["B1", "B2", "B3", "B4+C3", "A1", "A2", "A3a", "A3b", "A3c",
            "A4", "C1", "C2", "C4", "B5+F2", "B6", "B7", "B8",
            "A5", "A6", "D1-string", "D2-comment", "X13"]
os.makedirs(os.path.dirname(cp_path), exist_ok=True)
with open(cp_path, 'w', encoding='utf-8', newline='\n') as f:
    for fid in done_ids:
        f.write(json.dumps({"unit": fid, "done": True}) + "\n")
report["fixIdsDone"] = done_ids

# ---- Check 1: XML parse (whole file) ----
try:
    ET.parse(xml_path)
    report["xmlValid"] = True
except ET.ParseError as e:
    report["xmlValid"] = False
    report["details"].append("XML PARSE ERROR: %s" % e)

# Load all string entries (name -> raw inner text) for targeted checks
with open(xml_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Map of keys we newly wrote/edited (verify presence of expected new content)
expected_substrings = {
    "retro_benefit_weekly": "im Rahmen deines KI-Tageskontingents",
    "settings_premium_feature_profiles": "ohne Anzahllimit",
    "paywall_feature_profiles": "ohne Anzahllimit",
    "settings_premium_feature_5_perspectives": "so viele eigene wie du brauchst",
    "churn_offer_feature_perspectives": "so viele eigene wie du brauchst",
    "onboarding_feature_secure": "bleiben auf dem Gerät",
    "profile_entropy_long": "Aufräum-Schritte",
    "ai_prompt_entropy_intro": "Ordnungs- und Muster-Analyst",
    "ai_prompt_custom_intro": "verbindlichen Ratschläge",
    "ai_prompt_custom_rules": "FACHBERATUNGS-REGEL",
    "ai_prompt_rerank_system": "verweise bei solchen Themen wertneutral auf Fachpersonen",
    "ai_prompt_goals_rules": "Diät-, Medikamenten- oder Therapieempfehlungen",
    "ai_prompt_insight_attitude": "KEINE psychologischen Diagnosen",
    "ai_prompt_no_dates_rule": "Postfach läuft seit Tagen über",
    "consent_toggle_analytics_title": "Pseudonyme Nutzungsanalyse",
    "consent_toggle_analytics_body": "pseudonyme Nutzungsstatistiken",
    "settings_analytics_title": "Pseudonyme Nutzungsanalyse",
    "privacy_gate_tts_body": "ausschließlich zur Sprachsynthese",
    "onboarding_premium_feature_noads": "Ungestört schreiben und reflektieren",
    "paywall_feature_noads": "Ungestört schreiben und reflektieren",
    "paywall_headline_clarity_sub": "wiederkehrende Muster in deinen Einträgen sichtbar",
    "settings_feedback_dialog_desc": "so schnell wie möglich",
    "paywall_monthly_note": "Verlängert sich automatisch monatlich",
}
for k, sub in expected_substrings.items():
    pat = re.compile(r'<string name="%s"[^>]*>(.*?)</string>' % re.escape(k), re.S)
    m = pat.search(content)
    if not m:
        report["missing"].append("KEY NOT FOUND: %s" % k)
    elif sub not in m.group(1):
        report["missing"].append("EXPECTED TEXT MISSING in %s: '%s'" % (k, sub))

# translatable=false check for dev_seed
for k in ["dev_seed_dialog_title", "dev_seed_dialog_message", "dev_seed_add_action",
          "dev_seed_delete_all_action", "dev_seed_cancel_action"]:
    pat = re.compile(r'<string name="%s"[^>]*translatable="false"' % re.escape(k))
    if not pat.search(content):
        report["missing"].append("translatable=false MISSING on %s" % k)

# ---- Check 2: Umlaut substitution check in NEW texts only ----
# Look for ae/oe/ue/ss as umlaut-replacement in the edited string values.
umlaut_offenders = []
ascii_umlaut = re.compile(r'\b\w*(ae|oe|ue)\w*\b')
# Whitelist: legitimate German words containing ae/oe/ue/ss sequences that are NOT umlaut replacements
whitelist = {"neue", "neuer", "neuen", "neues", "treue", "Reue", "Steuer", "steuer",
             "genauer", "Dauer", "dauer", "Mauer", "Aue", "blaue", "graue", "Statue",
             "aktuelle", "aktuell", "individuelle", "individuell", "manuelle", "visuell",
             "Quelle", "quelle", "Quellen", "quellen", "queue", "sequence",
             "neuer", "feuer", "Feuer", "teuer", "scheuer", "ungeheuer", "abenteuer",
             "Abenteuer", "erneuern", "bedauern", "auswerten", "auer"}
for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', content, re.S):
    key = m.group(1)
    if key not in expected_substrings:
        continue  # only check strings WE edited
    val = m.group(2)
    for w in re.finditer(r'\b[A-Za-zÄÖÜäöüß]+\b', val):
        word = w.group(0)
        low = word.lower()
        if low in {x.lower() for x in whitelist}:
            continue
        # flag if contains ae/oe/ue between consonants (typical umlaut substitution)
        if re.search(r'(ae|oe|ue)', low) and not re.search(r'[äöü]', word):
            # extra heuristic: skip if it's clearly an English/technical token
            umlaut_offenders.append("%s: %s" % (key, word))

# Filter out known-good German words that naturally contain these digraphs
filtered = []
known_good = ("neue", "quel", "aktuel", "individuel", "manuel", "feuer", "teuer",
              "steuer", "dauer", "genauer", "auswert", "abenteuer", "erneuer",
              "bedauer", "produkt", "reduz", "neutr", "freue", "treue",
              # JSON field names (technical keys, present in original, not German prose):
              "erklaerung", "herleitung", "zusammenfassung",
              # normal German words with ue/ae/oe as plain letter sequences:
              "zuerst", "zueinander", "abenteuer")
for off in umlaut_offenders:
    word = off.split(": ", 1)[1].lower()
    if any(g in word for g in known_good):
        continue
    filtered.append(off)
if filtered:
    report["umlautCheck"] = "FAILED"
    report["details"].append("Possible ae/oe/ue substitutions: %s" % filtered)
else:
    report["umlautCheck"] = "passed"

# ---- Check 3: Format-string integrity in edited strings (no bare %) ----
fmt_offenders = []
for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', content, re.S):
    key = m.group(1)
    if key not in expected_substrings:
        continue
    val = m.group(2)
    # find % not part of %1$s style and not %%
    for pm in re.finditer(r'%', val):
        i = pm.start()
        rest = val[i:i+4]
        if rest.startswith('%%'):
            continue
        if re.match(r'%\d+\$[sd]', rest):
            continue
        if re.match(r'%[sd]', rest):
            continue
        fmt_offenders.append("%s: ...%s..." % (key, val[max(0,i-5):i+5]))
if fmt_offenders:
    report["formatCheck"] = "FAILED"
    report["details"].append("Bare %% in edited strings: %s" % fmt_offenders)
else:
    report["formatCheck"] = "passed"

print(json.dumps(report, ensure_ascii=False, indent=2))
