#!/usr/bin/env python3
# Escapet literale % (nicht Teil eines Format-Specifiers) -> %% in den beiden
# mit Format-Argument aufgerufenen KI-Prompt-Strings. Verhindert String.format-Crash.
import glob, os, re, xml.etree.ElementTree as ET

keys = ["ai_prompt_custom_intro", "profile_style_custom"]
line_re = re.compile(r'(<string name="([^"]+)">)(.*)(</string>)')
# literales % = % das NICHT gefolgt ist von [Ziffern][$]?[sd] und nicht von %
lit_pct = re.compile(r'%(?![0-9]*\$?[sd%])')

fixed = {}
for f in sorted(glob.glob('app/src/main/res/values*/strings.xml')):
    lang = os.path.basename(os.path.dirname(f))
    with open(f, encoding='utf-8') as fh:
        lines = fh.readlines()
    changed = False
    for i, line in enumerate(lines):
        m = line_re.search(line)
        if not m or m.group(2) not in keys:
            continue
        val = m.group(3)
        new = lit_pct.sub('%%', val)
        if new != val:
            lines[i] = m.group(1) + new + m.group(4) + "\n"
            fixed.setdefault(lang, []).append(f"{m.group(2)}({val.count('%')-new.count('%')*0}->esc)")
            changed = True
    if changed:
        tmp = f + '.tmp'
        with open(tmp, 'w', encoding='utf-8', newline='\n') as fh:
            fh.writelines(lines)
        ET.parse(tmp)
        os.replace(tmp, f)

if not fixed:
    print("Keine literalen % zum Escapen gefunden.")
else:
    for lang, ks in fixed.items():
        print(f"{lang}: escaped in {', '.join(set(k.split('(')[0] for k in ks))}")
