# -*- coding: utf-8 -*-
"""POST-VERIFICATION zh-rCN (FIN-051c)."""
import re, json, sys
import xml.etree.ElementTree as ET

TARGET = "app/src/main/res/values-zh-rCN/strings.xml"
DEREF = ".android-shield/translation-jobs/de-reference.json"

deref = json.load(open(DEREF, encoding='utf-8'))
de_strings = deref["strings"]
de_plurals = deref["plurals"]

# keys we must have (34 strings + 2 plurals)
str_keys = ["ai_prompt_custom_intro","ai_prompt_custom_rules","ai_prompt_entropy_intro","ai_prompt_goals_rules","ai_prompt_insight_attitude","ai_prompt_no_dates_rule","ai_prompt_rerank_system","churn_offer_feature_perspectives","consent_card3_title","consent_toggle_analytics_body","consent_toggle_analytics_title","consent_toggle_do_not_sell_body","onboarding_feature_secure","onboarding_premium_feature_noads","paywall_feature_noads","paywall_feature_profiles","paywall_from_per_day","paywall_headline_clarity_sub","paywall_monthly_note","privacy_gate_groq_body","privacy_gate_tts_body","privacy_gate_tts_title","profile_entropy_long","retro_benefit_weekly","settings_analytics_title","settings_delete_account_confirm_body","settings_delete_account_subtitle","settings_feedback_dialog_desc","settings_premium_feature_5_perspectives","settings_premium_feature_profiles","settings_report_ai_confirm_body","settings_revoke_confirm_body","settings_revoke_confirm_title","settings_revoke_subtitle"]
plu_keys = ["datetime_months_relative","datetime_years_relative"]

errors = []

# 1) XML valid
try:
    tree = ET.parse(TARGET)
    root = tree.getroot()
except Exception as e:
    print("FATAL: XML invalid:", e); sys.exit(1)

strings = {}
plurals = {}
for el in root:
    if el.tag == "string":
        strings[el.get("name")] = el.text or ""
    elif el.tag == "plurals":
        d = {}
        for it in el:
            d[it.get("quantity")] = it.text or ""
        plurals[el.get("name")] = d

# 2) all keys present
for k in str_keys:
    if k not in strings:
        errors.append("MISSING string: " + k)
for k in plu_keys:
    if k not in plurals:
        errors.append("MISSING plural: " + k)

fmt = re.compile(r'%\d+\$[sd]|%[sd]|%%')

def args_of(s):
    return sorted(re.findall(r'%\d+\$[sd]', s))

# raw text from file for de-comparison (ElementTree decodes entities; de-reference has literal \n)
raw = open(TARGET, encoding='utf-8').read()

for k in str_keys:
    if k not in strings:
        continue
    val = strings[k]
    # raw value (with literal \n) for arg / identical-de check
    m = re.search(r'<string name="%s"[^>]*>(.*?)</string>' % re.escape(k), raw, re.DOTALL)
    rawval = m.group(1) if m else val
    de = de_strings.get(k, "")
    # 3) not identical to DE
    if rawval == de:
        errors.append("IDENTICAL to DE: " + k)
    # 4) format args identical to DE
    if args_of(rawval) != args_of(de):
        errors.append("ARG mismatch %s: de=%s zh=%s" % (k, args_of(de), args_of(rawval)))
    # 5) no naked %
    unguarded = fmt.sub('', rawval).count('%')
    if unguarded != 0:
        errors.append("NAKED %% in %s" % k)

# plurals args + simplified
for k in plu_keys:
    if k not in plurals:
        continue
    for q, t in plurals[k].items():
        m = re.search(r'<item quantity="%s">(.*?)</item>' % q, raw)
        rawt = m.group(1) if m else t
        de_form = de_plurals[k].get(q) or de_plurals[k].get("other")
        if args_of(rawt) != args_of(de_plurals[k].get("other","")):
            # months/years use %1$d
            if "%1$d" not in rawt:
                errors.append("PLURAL arg missing %s/%s" % (k,q))
        if fmt.sub('', rawt).count('%') != 0:
            errors.append("NAKED %% in plural %s/%s" % (k,q))

# 6) Traditional-character leakage check (common simplified-vs-traditional pairs)
# A set of frequently-leaked traditional chars that should NOT appear in zh-rCN
trad_chars = set("軟體應該說讀寫無隻關於對開閉錯誤檢證顯頁鐘訊轉換圖聰雲檔傳輸觀頭裡層爾盡範圍鬆緊復製刪錄們區")
# Only flag if it appears in OUR translated values
trad_found = {}
for k in str_keys + plu_keys:
    if k in strings:
        v = strings[k]
    elif k in plurals:
        v = " ".join(plurals[k].values())
    else:
        continue
    hits = [c for c in v if c in trad_chars]
    if hits:
        trad_found[k] = sorted(set(hits))
if trad_found:
    errors.append("TRADITIONAL leakage: " + json.dumps(trad_found, ensure_ascii=False))

print("ERRORS:", len(errors))
for e in errors:
    print("  -", e)
print("string keys present:", sum(1 for k in str_keys if k in strings), "/", len(str_keys))
print("plural keys present:", sum(1 for k in plu_keys if k in plurals), "/", len(plu_keys))
