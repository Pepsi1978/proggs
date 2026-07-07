#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""POST-VERIFIKATION uk (FIN-051c):
(a) alle Keys vorhanden, (b) keiner identisch DE, (c) XML valide,
(d) Format-Args identisch DE<->UK, (e) keine nackten %, (f) 0 russische Zeichen [ыэъё]."""
import json, os, re
import xml.etree.ElementTree as ET

base = os.path.dirname(os.path.abspath(__file__))
de = json.load(open(os.path.join(base, 'de-reference.json'), encoding='utf-8'))
uk_data = json.load(open(os.path.join(base, 'uk-data.json'), encoding='utf-8'))
target = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res/values-uk/strings.xml')

problems = []
RU = re.compile(r'[ыэъё]')
FMT = re.compile(r'%\d+\$[sd]|%[sd]|%%')
ARG = re.compile(r'%\d+\$[sd]|%[sd]')

# 1) XML valide
try:
    tree = ET.parse(target)
    root = tree.getroot()
    xml_ok = True
except Exception as e:
    xml_ok = False
    problems.append(f"XML PARSE FAIL: {e}")

# Build maps from parsed XML
str_map = {}
plur_map = {}
if xml_ok:
    for el in root.findall('string'):
        str_map[el.get('name')] = ''.join(el.itertext())
    for pl in root.findall('plurals'):
        plur_map[pl.get('name')] = {it.get('quantity'): ''.join(it.itertext()) for it in pl.findall('item')}

de_strings = de['strings']
de_plurals = de['plurals']

# 2) all string keys present + not identical to DE + russian + format args
checked = 0
for key, de_val in de_strings.items():
    if key not in str_map:
        problems.append(f"MISSING key: {key}")
        continue
    uk_val = str_map[key]
    checked += 1
    # identical to DE? (compare against DE raw, both have literal \n)
    de_norm = de_val.replace('\\n', '\n').replace('\\"', '"').replace('\\\\', '\\')
    if uk_val.strip() == de_norm.strip():
        problems.append(f"IDENTICAL TO DE: {key}")
    # russian chars
    if RU.search(uk_val):
        problems.append(f"RUSSIAN CHAR in {key}: {RU.findall(uk_val)}")
    # format args identical set/count
    de_args = sorted(ARG.findall(de_norm))
    uk_args = sorted(ARG.findall(uk_val))
    if de_args != uk_args:
        problems.append(f"FORMAT ARG MISMATCH {key}: de={de_args} uk={uk_args}")
    # naked %
    naked = FMT.sub('', uk_val).count('%')
    if naked != 0:
        problems.append(f"NAKED %% in {key}: {naked}")

# 3) plurals: present, four forms, not identical to DE, russian, format args
for pname, de_forms in de_plurals.items():
    if pname not in plur_map:
        problems.append(f"MISSING plural: {pname}")
        continue
    forms = plur_map[pname]
    for q in ('one', 'few', 'many', 'other'):
        if q not in forms:
            problems.append(f"plural {pname}: missing form {q}")
    # not identical to DE one/other
    for q, dv in de_forms.items():
        if q in forms and forms[q].strip() == dv.strip():
            problems.append(f"PLURAL IDENTICAL TO DE {pname}/{q}: {forms[q]}")
    for q, uv in forms.items():
        if RU.search(uv):
            problems.append(f"RUSSIAN CHAR plural {pname}/{q}: {uv}")
        # each plural form must contain %1$d
        if '%1$d' not in uv:
            problems.append(f"plural {pname}/{q} missing %1$d: {uv}")
        naked = FMT.sub('', uv).count('%')
        if naked != 0:
            problems.append(f"NAKED %% plural {pname}/{q}")

# global russian scan in newly written values (de-reference keys only — whole-file scan as bonus)
print("=== VERIFY uk ===")
print("XML valid:", xml_ok)
print("string keys checked:", checked, "of", len(de_strings))
print("plurals checked:", list(plur_map.keys()))
print("problems:", len(problems))
for p in problems:
    print("  !", p)
print("RESULT:", "PASS" if (xml_ok and not problems) else "FAIL")
