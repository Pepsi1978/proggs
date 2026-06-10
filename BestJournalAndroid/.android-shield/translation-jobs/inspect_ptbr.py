import xml.etree.ElementTree as ET
f = '/c/Users/barwa/proggs/BestJournalAndroid/app/src/main/res/values-pt-rBR/strings.xml'
import os
f = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-pt-rBR/strings.xml')
# raw scan for rerank to see raw newlines
with open(f,'r',encoding='utf-8') as fh:
    raw = fh.read()
import re
# find the rerank string block raw
m = re.search(r'<string name="ai_prompt_rerank_system">(.*?)</string>', raw, re.DOTALL)
if m:
    val = m.group(1)
    print('rerank len:', len(val), 'contains raw newline?:', '\n' in val)
    print('rerank RAW first 200:', repr(val[:200]))
print()
# existing legal phrasings (for consistency) — parse via ET
tree = ET.parse(f)
root = tree.getroot()
d = {}
for s in root.findall('string'):
    d[s.get('name')] = s.text or ''
for k in ['settings_delete_account_confirm_body','settings_delete_account_subtitle','consent_toggle_do_not_sell_body','settings_revoke_subtitle','settings_revoke_confirm_title','paywall_from_per_day','onboarding_feature_secure','privacy_gate_groq_body']:
    print('==', k, '==')
    print(repr(d.get(k,'<MISSING>'))[:280])
    print()
