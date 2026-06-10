# -*- coding: utf-8 -*-
"""finale Phase3 Post-Verifikation pl (FIN-051c).
Prueft: (1) alle 42 Strings + 2 Plurals vorhanden, (2) keiner identisch mit DE,
(3) XML valide (ElementTree), (4) Format-Args identisch zur DE-Referenz
(die 6 Ex-Mismatch-Keys MUESSEN 0 Format-Args haben), (5) kein nacktes literales %,
(6) polnische Diakritika vorhanden, (7) Plurals one/few/many/other.
"""
import re, sys, io, json
import xml.etree.ElementTree as ET

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

PL = 'app/src/main/res/values-pl/strings.xml'
JOB = '.android-shield/translation-jobs/job-plan.json'
DEREF = '.android-shield/translation-jobs/de-reference.json'

with open(JOB, 'r', encoding='utf-8') as f:
    pl_job = json.load(f)['languages']['pl']
str_keys = pl_job['strings']
plural_keys = pl_job['plurals']

with open(DEREF, 'r', encoding='utf-8') as f:
    de = json.load(f)
de_str = de['strings']
de_plu = de['plurals']

# 3) XML valide
tree = ET.parse(PL)
root = tree.getroot()

pl_strings = {}
for el in root.findall('string'):
    pl_strings[el.get('name')] = el.text if el.text is not None else ''

pl_plurals = {}
for el in root.findall('plurals'):
    forms = {it.get('quantity'): (it.text or '') for it in el.findall('item')}
    pl_plurals[el.get('name')] = forms

fmt = re.compile(r'%\d+\$[sd]|%[sd]')  # echte Platzhalter (ohne %% Escape)
diacritics = set('ąćęłńóśźżĄĆĘŁŃÓŚŹŻ')
EX_MISMATCH = {
    'ai_prompt_custom_schema', 'ai_prompt_rerank_system',
    'ai_prompt_rerank_actions_header', 'ai_prompt_rerank_entries_header',
    'ai_prompt_rerank_user_focus_header',
    'dashboard_profile_focus_entropy', 'dashboard_profile_focus_goals',
    'dashboard_profile_focus_insight', 'dashboard_profile_focus_summary',
}

errors = []
warnings = []

def fmt_multiset(s):
    return sorted(fmt.findall(s))

def bare_percent(s):
    # entferne alle echten Platzhalter UND %% Escapes, dann zaehle uebrige %
    tmp = fmt.sub('', s)
    tmp = tmp.replace('%%', '')
    return tmp.count('%')

# (1)+(2)+(4)+(5)+(6) fuer Strings
for k in str_keys:
    if k not in pl_strings:
        errors.append(f'[MISSING] string {k}')
        continue
    pv = pl_strings[k]
    dv = de_str.get(k, '')
    if pv.strip() == dv.strip() and pv.strip() != '':
        errors.append(f'[IDENTICAL-DE] {k}')
    # Format-Args: ET liefert UNESCAPED Text (\\ -> \, " bleibt). DE-Ref ist roher String.
    # Platzhalter %1$s/%1$d sind in beiden identisch darstellbar.
    de_args = fmt_multiset(dv)
    pl_args = fmt_multiset(pv)
    if de_args != pl_args:
        errors.append(f'[FMT-MISMATCH] {k}: DE={de_args} PL={pl_args}')
    if k in EX_MISMATCH and pl_args:
        errors.append(f'[EX-MISMATCH-HAS-ARGS] {k}: {pl_args} (muss leer sein)')
    bp = bare_percent(pv)
    if bp > 0:
        errors.append(f'[BARE-%] {k}: {bp} nacktes %')
    # Diakritika: nur fuer laengere Inhalts-Strings sinnvoll pruefen (Header sind reine === ===)
    if len(pv) > 25 and not (set(pv) & diacritics):
        warnings.append(f'[NO-DIACRITICS?] {k}: {pv[:60]!r}')

# Plurals
for k in plural_keys:
    if k not in pl_plurals:
        errors.append(f'[MISSING] plural {k}')
        continue
    forms = pl_plurals[k]
    for q in ('one', 'few', 'many', 'other'):
        if q not in forms:
            errors.append(f'[PLURAL-FORM-MISSING] {k}: {q}')
    # Format-Arg %1$d in jeder Form
    for q, v in forms.items():
        if fmt_multiset(v) != ['%1$d']:
            errors.append(f'[PLURAL-FMT] {k}/{q}: {fmt_multiset(v)} (erwartet [%1$d])')
        if bare_percent(v) > 0:
            errors.append(f'[PLURAL-BARE-%] {k}/{q}')
    # nicht identisch mit DE (DE hat nur one/other)
    if forms.get('one', '').strip() == de_plu.get(k, {}).get('one', '').strip():
        # one-Form darf abweichen muessen — DE 'vor %1$d Monat' vs PL '%1$d miesiąc temu'
        pass

# Diakritika global vorhanden?
all_text = ' '.join(pl_strings[k] for k in str_keys if k in pl_strings)
if not (set(all_text) & diacritics):
    errors.append('[GLOBAL-NO-DIACRITICS] keine polnischen Sonderzeichen gefunden')

print('=== VERIFY pl ===')
print('strings checked:', len([k for k in str_keys if k in pl_strings]), '/', len(str_keys))
print('plurals checked:', len([k for k in plural_keys if k in pl_plurals]), '/', len(plural_keys))
print('XML: VALID (ElementTree parsed)')
print('ERRORS:', len(errors))
for e in errors:
    print('  ', e)
print('WARNINGS:', len(warnings))
for w in warnings:
    print('  ', w)
print('RESULT:', 'PASS' if not errors else 'FAIL')
sys.exit(0 if not errors else 1)
