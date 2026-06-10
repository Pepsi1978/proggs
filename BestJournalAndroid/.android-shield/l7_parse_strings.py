# -*- coding: utf-8 -*-
"""Robust strings.xml parser for L7. Extracts <string name=...> values.
Returns dict name->raw inner text (with entities decoded for inspection but %n$s kept literal)."""
import xml.etree.ElementTree as ET
import os, json, re

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
res = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')

PATHS = {
    'de':  os.path.join(res, 'values', 'strings.xml'),
    'nl':  os.path.join(res, 'values-nl', 'strings.xml'),
    'pl':  os.path.join(res, 'values-pl', 'strings.xml'),
    'uk':  os.path.join(res, 'values-uk', 'strings.xml'),
    'in':  os.path.join(res, 'values-in', 'strings.xml'),
}

def inner_xml(elem):
    """Serialize children + text so we keep raw content (e.g. nested tags)."""
    parts = []
    if elem.text:
        parts.append(elem.text)
    for child in elem:
        parts.append(ET.tostring(child, encoding='unicode'))
        if child.tail:
            parts.append(child.tail)
    return ''.join(parts)

def parse_strings(path):
    """Return dict name -> raw inner text for <string> only (translatable check ignored)."""
    tree = ET.parse(path)
    root = tree.getroot()
    out = {}
    for el in root:
        if el.tag == 'string' and 'name' in el.attrib:
            out[el.attrib['name']] = inner_xml(el)
    return out

def parse_plurals_arrays(path):
    tree = ET.parse(path)
    root = tree.getroot()
    plur = {}
    arr = {}
    for el in root:
        if el.tag == 'plurals' and 'name' in el.attrib:
            items = {}
            for it in el:
                q = it.attrib.get('quantity', '?')
                items[q] = inner_xml(it)
            plur[el.attrib['name']] = items
        elif el.tag == 'string-array' and 'name' in el.attrib:
            arr[el.attrib['name']] = [inner_xml(it) for it in el]
    return plur, arr

if __name__ == '__main__':
    data = {lang: parse_strings(p) for lang, p in PATHS.items()}
    # quick counts
    for lang in PATHS:
        print(lang, "strings:", len(data[lang]))
    # save full DE + 4 langs to file-as-memory (big — only path retained in context)
    with open(os.path.join(base, 'l7-parsed-strings.json'), 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False)
    print("saved l7-parsed-strings.json")
