#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Kuerzt onboarding_free_trial in values-pl/strings.xml"""
import os, re, tempfile, xml.etree.ElementTree as ET

STRINGS_PATH = r"C:\Users\barwa\proggs\BestJournalAndroid\app\src\main\res\values-pl\strings.xml"

FIX = {
    "onboarding_free_trial": "Bezpłatnie, anuluj kiedy chcesz",
}

def patch(path, fixes):
    with open(path, "r", encoding="utf-8") as fh:
        content = fh.read()
    for key, value in fixes.items():
        pattern = r'(<string\s+name="{}"[^>]*>)(.*?)(</string>)'.format(re.escape(key))
        def replacer(m, v=value):
            return m.group(1) + v + m.group(3)
        content = re.sub(pattern, replacer, content, flags=re.DOTALL)
    dir_name = os.path.dirname(path)
    with tempfile.NamedTemporaryFile("w", dir=dir_name, suffix=".tmp",
                                     delete=False, encoding="utf-8") as tmp:
        tmp.write(content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)
    ET.parse(path)
    print("OK — fix applied, XML valide")

patch(STRINGS_PATH, FIX)
