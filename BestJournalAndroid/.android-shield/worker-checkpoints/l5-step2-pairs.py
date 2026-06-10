# -*- coding: utf-8 -*-
import json, os, re, xml.etree.ElementTree as ET

ROOT = os.path.expanduser("~/proggs/BestJournalAndroid")
SHIELD = os.path.join(ROOT, ".android-shield")
RES = os.path.join(ROOT, "app/src/main/res")
LANGS = ["ja", "ko", "zh-rCN", "zh-rTW"]

def parse_strings(path):
    d = {}
    if not os.path.exists(path):
        return d
    tree = ET.parse(path)
    for el in tree.getroot():
        if el.tag == "string":
            d[el.get("name")] = "".join(el.itertext())
    return d

de = parse_strings(os.path.join(RES, "values/strings.xml"))
L = {l: parse_strings(os.path.join(RES, f"values-{l}/strings.xml")) for l in LANGS}

with open(os.path.join(SHIELD, "worker-checkpoints", "l5-artifacts.json"), encoding="utf-8") as f:
    art = json.load(f)
present = art["presentLegalKeys"]

# ---- AUTOMATED CHECK A: ai_limits_dialog_body numbers ----
NUMS = ["150", "600", "30", "101", "151", "50", "5"]
def num_check(txt):
    found = {}
    for n in NUMS:
        # count as standalone token (avoid 150 matching inside 1500 etc.)
        found[n] = len(re.findall(r'(?<!\d)' + n + r'(?!\d)', txt or ""))
    return found
print("=== ai_limits_dialog_body NUMBER CHECK ===")
print("DE:", num_check(de.get("ai_limits_dialog_body","")))
for l in LANGS:
    print(f"{l}:", num_check(L[l].get("ai_limits_dialog_body","")))

# ---- AUTOMATED CHECK B: zh-rCN vs zh-rTW identical across present legal keys ----
cn, tw = L["zh-rCN"], L["zh-rTW"]
identical = [k for k in present if k in cn and k in tw and cn[k].strip() == tw[k].strip()]
print("\n=== zh-rCN vs zh-rTW (legal keys) ===")
print(f"compared:{len([k for k in present if k in cn and k in tw])} identical:{len(identical)}")
print("identical keys:", identical)

# Also across ALL shared keys (broad signal)
all_shared = [k for k in cn if k in tw]
all_identical = [k for k in all_shared if cn[k].strip() == tw[k].strip()]
print(f"ALL shared:{len(all_shared)} identical:{len(all_identical)} ({100*len(all_identical)//max(1,len(all_shared))}%)")

# ---- AUTOMATED CHECK C: key legal-term presence ----
TERMS = {
    "ja": {"auto_renew": ["自動更新", "自動的に更新", "自動的に継続"], "cancel": ["解約", "キャンセル", "解除"], "trial14": ["14"], "anytime": ["いつでも"]},
    "ko": {"auto_renew": ["자동 갱신", "자동갱신", "자동으로 갱신", "자동 결제"], "cancel": ["해지", "취소", "해제"], "trial14": ["14"], "anytime": ["언제든지", "언제든"]},
    "zh-rCN": {"auto_renew": ["自动续订", "自动续费", "自动续期", "自动更新"], "cancel": ["取消", "退订", "解除"], "trial14": ["14"], "anytime": ["随时"]},
    "zh-rTW": {"auto_renew": ["自動續訂", "自動續費", "自動續期", "自動更新"], "cancel": ["取消", "退訂", "解除"], "trial14": ["14"], "anytime": ["隨時"]},
}
# Keys most likely to carry auto-renewal/cancel/trial wording
RENEW_KEYS = ["paywall_yearly_note", "paywall_legal_hint", "churn_auto_renew_note", "churn_offer_auto_renew",
              "onboarding_free_after_trial", "onboarding_free_trial", "paywall_free_note", "settings_premium_cancelled_until",
              "settings_premium_cancelled_desc", "churn_cancel_sub", "churn_google_play_redirect"]
print("\n=== KEY-TERM PRESENCE (renewal/cancel-related legal keys) ===")
for l in LANGS:
    t = TERMS[l]
    rep = {}
    for k in RENEW_KEYS:
        if k not in L[l]:
            continue
        v = L[l][k]
        hits = []
        for cat, words in t.items():
            if any(w in v for w in words):
                hits.append(cat)
        rep[k] = hits
    print(f"\n--{l}--")
    for k, h in rep.items():
        print(f"  {k}: {h}")

# ---- AUTOMATED CHECK D: \n in words (CJK has no spaces, so \n mid-text needs eyeball) ----
print("\n=== NEWLINE COUNTS in legal keys (per lang) ===")
for l in LANGS:
    cnt = sum(1 for k in present if k in L[l] and "\n" in L[l][k])
    print(f"{l}: {cnt} legal keys contain \\n")

# ---- AUTOMATED CHECK E: politeness register heuristic ----
print("\n=== JA politeness (です/ます vs plain) sample over legal keys ===")
ja = L["ja"]
desu_masu = sum(1 for k in present if k in ja and re.search(r'(です|ます|ません|でしょう|ください)', ja[k]))
print(f"ja legal keys with です/ます markers: {desu_masu}/{len([k for k in present if k in ja])}")
ko = L["ko"]
hapnida = sum(1 for k in present if k in ko and re.search(r'(습니다|합니다|입니다|십시오|세요)', ko[k]))
haeyo = sum(1 for k in present if k in ko and re.search(r'(해요|예요|어요|아요|이에요)', ko[k]))
print(f"ko legal keys: 합니다체~{hapnida}, 해요체~{haeyo} (of {len([k for k in present if k in ko])})")

# Save automated results
auto = {
    "numCheck": {"de": num_check(de.get("ai_limits_dialog_body","")), **{l: num_check(L[l].get("ai_limits_dialog_body","")) for l in LANGS}},
    "cnTwLegalIdentical": identical,
    "cnTwLegalCompared": len([k for k in present if k in cn and k in tw]),
    "cnTwAllShared": len(all_shared),
    "cnTwAllIdentical": len(all_identical),
}
with open(os.path.join(SHIELD, "worker-checkpoints", "l5-auto.json"), "w", encoding="utf-8") as f:
    json.dump(auto, f, ensure_ascii=False, indent=1)
print("\nauto results saved.")
