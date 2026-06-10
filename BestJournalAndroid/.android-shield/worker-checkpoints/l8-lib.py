# -*- coding: utf-8 -*-
"""Shared parsing lib for L8. Parses Android strings.xml without loading into LLM context.
Handles <string>, <plurals>, <string-array>. Returns dicts."""
import re, html, os, json

def _unescape(s):
    # Android-level unescape for comparison purposes (keep it simple/lossless-ish)
    if s is None:
        return s
    s = s.replace('\\\'', "'").replace('\\"', '"').replace('\\n', '\n').replace('\\t', '\t')
    return s

def parse_strings(path):
    """Return (strings: dict[name->raw_inner], plurals: dict[name->{qty->text}], arrays: dict[name->[items]])."""
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    strings = {}
    # <string name="x" ...>...</string>  (non-greedy, DOTALL)
    for m in re.finditer(r'<string\s+name="([^"]+)"([^>]*)>(.*?)</string>', content, re.DOTALL):
        name = m.group(1)
        attrs = m.group(2)
        inner = m.group(3)
        # skip translatable="false"? keep but mark
        strings[name] = inner
    # self-closing/empty <string name="x"/>
    for m in re.finditer(r'<string\s+name="([^"]+)"\s*/>', content):
        strings.setdefault(m.group(1), '')
    plurals = {}
    for m in re.finditer(r'<plurals\s+name="([^"]+)"\s*>(.*?)</plurals>', content, re.DOTALL):
        name = m.group(1)
        body = m.group(2)
        items = {}
        for im in re.finditer(r'<item\s+quantity="([^"]+)"\s*>(.*?)</item>', body, re.DOTALL):
            items[im.group(1)] = im.group(2)
        plurals[name] = items
    arrays = {}
    for m in re.finditer(r'<string-array\s+name="([^"]+)"\s*>(.*?)</string-array>', content, re.DOTALL):
        name = m.group(1)
        body = m.group(2)
        items = [im.group(1) for im in re.finditer(r'<item\s*>(.*?)</item>', body, re.DOTALL)]
        arrays[name] = items
    return strings, plurals, arrays

if __name__ == '__main__':
    root = os.path.expanduser('~/proggs/BestJournalAndroid/app/src/main/res')
    de_s, de_p, de_a = parse_strings(os.path.join(root, 'values/strings.xml'))
    br_s, br_p, br_a = parse_strings(os.path.join(root, 'values-pt-rBR/strings.xml'))
    pt_s, pt_p, pt_a = parse_strings(os.path.join(root, 'values-pt-rPT/strings.xml'))
    print("DE strings:", len(de_s), "plurals:", len(de_p), "arrays:", len(de_a))
    print("BR strings:", len(br_s), "plurals:", len(br_p), "arrays:", len(br_a))
    print("PT strings:", len(pt_s), "plurals:", len(pt_p), "arrays:", len(pt_a))
