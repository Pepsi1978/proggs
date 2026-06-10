import xml.etree.ElementTree as ET
import json, os

p = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values/strings.xml')
result = {"xml_parse": None, "fix1": None, "fix2": None, "fix3": None, "fix3_placeholder_count": None, "errors": []}

# 1. ElementTree parse
try:
    tree = ET.parse(p)
    root = tree.getroot()
    result["xml_parse"] = "OK"
except ET.ParseError as e:
    result["xml_parse"] = "FAIL"
    result["errors"].append("ParseError: %s" % e)

with open(p, encoding='utf-8') as f:
    content = f.read()

# Helper to fetch a string by name from parsed tree
def get_str(name):
    if result["xml_parse"] != "OK":
        return None
    for el in root.iter('string'):
        if el.get('name') == name:
            return el.text or ""
    return None

# FIX 1
s1 = get_str('settings_report_ai_confirm_body')
result["fix1"] = bool(s1 and "Wir antworten in der Regel innerhalb von 24 Stunden an Werktagen." in s1)

# FIX 2
s2 = get_str('consent_card3_title')
result["fix2"] = (s2 == "Pseudonyme Statistiken")

# FIX 3
s3 = get_str('paywall_monthly_note')
# Note: ET decodes \n as literal backslash-n since it's escaped in XML; check raw text
result["fix3"] = bool(s3 and "Danach %1$s pro Monat, verl" in s3 and "Jederzeit k" in s3)
# Placeholder count: count %1$s occurrences in the raw monthly_note line
result["fix3_placeholder_count"] = (s3 or "").count("%1$s")
result["fix3_format_ok"] = (result["fix3_placeholder_count"] == 1)
# Verify \n literal present (backslash-n) in the string
result["fix3_newline_literal"] = bool(s3 and "\\n" in s3)

ok = (result["xml_parse"] == "OK" and result["fix1"] and result["fix2"]
      and result["fix3"] and result["fix3_format_ok"] and result["fix3_newline_literal"])
result["all_ok"] = ok

print(json.dumps(result, ensure_ascii=False, indent=2))
