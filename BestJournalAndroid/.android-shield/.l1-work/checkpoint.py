# -*- coding: utf-8 -*-
import json, os, sys, time

base = os.path.expanduser('~/proggs/BestJournalAndroid/.android-shield')
cp = os.path.join(base, 'worker-checkpoints', 'l1-ar-ur-tr.jsonl')

def log(lang, checkpoint, status, detail=''):
    rec = {'ts': time.strftime('%Y-%m-%dT%H:%M:%S'), 'worker': 'l1',
           'lang': lang, 'checkpoint': checkpoint, 'status': status, 'detail': detail}
    with open(cp, 'a', encoding='utf-8') as f:
        f.write(json.dumps(rec, ensure_ascii=False) + '\n')

if __name__ == '__main__':
    # args: lang checkpoint status detail
    a = sys.argv[1:]
    log(a[0], a[1], a[2], a[3] if len(a) > 3 else '')
    print('checkpoint logged:', a)
