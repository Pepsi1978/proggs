import json,os,pathlib,subprocess,tempfile,time,hashlib,importlib.util,threading,sys
sys.dont_write_bytecode = True
skill = pathlib.Path(__file__).resolve().parents[1]
new = str(skill/'scripts/tmux_bridge.py')
def call(program,*args,ok=True):
    r=subprocess.run(['python3',program,*args],capture_output=True,text=True,timeout=15)
    assert (r.returncode==0)==ok,r.stdout+r.stderr
    return json.loads(r.stdout),len(r.stdout.encode())
with tempfile.TemporaryDirectory(prefix='bridge-opt-check-') as tmp:
    d=pathlib.Path(tmp)
    old = str(d/'baseline.py')
    pathlib.Path(old).write_bytes(subprocess.check_output([
        'git','-C',str(skill),'show',
        'd0b0352a8:OpenLauncher/Profiles/ClaudeCode/standard/skills/ins-macos-terminal-einfuegen/scripts/tmux_bridge.py']))
    sock=str(d/'socket');target=d/'receiver.py';out=d/'bytes'
    target.write_text('''import os,sys,tty
from pathlib import Path
tty.setraw(0)
def paint(draft):
    rows=['Testantwort '+str(i) for i in range(10)]+['─'*70,'❯ '+draft,'─'*70,'  📁 ~/test   💰 $0.00   ⏳ 2m10s   🏷️ v2.1.276','  '+('paste again to expand' if draft else '⏵⏵ bypass permissions on (shift+tab to cycle) · ← for agents')]
    os.write(1,('\\x1b[?2004h\\x1b[H\\x1b[2J'+'\\r\\n'.join(rows)+'\\x1b[12;'+str(3+len(draft))+'H').encode())
paint('')
data=b''
while True:
    b=os.read(0,4096);data+=b;Path(sys.argv[1]).write_bytes(data)
    if b'\\x1b[201~' in data:paint('[Pasted text #1]')
''')
    tmux=['/opt/homebrew/bin/tmux','-S',sock]
    subprocess.run(tmux+['-f','/dev/null','new-session','-d','-x','90','-y','24','-s','test','python3',str(target),str(out)],check=True)
    try:
        time.sleep(.15)
        info,_=call(new,'list','--socket',sock)
        def bind(program,name):
            state=str(d/name);call(program,'bind','--state',state,'--socket',sock,'--pane',info['pane'],'--agent-pid',info['pane_pid'],'--cwd',info['cwd']);return state
        a=bind(old,'old.json');b=bind(new,'new.json')
        oldbytes=newbytes=0
        for i in range(6):
            _,n=call(old,'read','--state',a);oldbytes+=n
            _,n=call(new,'read','--state',b);newbytes+=n
        start=time.monotonic();unchanged,n=call(new,'read','--state',b,'--wait','1');duration=time.monotonic()-start
        assert unchanged['event']=='unchanged' and duration>=1
        cancel=d/'cancel';timer=threading.Timer(.25,lambda:cancel.touch());timer.start()
        start=time.monotonic();call(new,'read','--state',b,'--wait','5','--cancel-file',str(cancel),ok=False);cancel_time=time.monotonic()-start;timer.join();assert cancel_time<1.5
        x,_=call(new,'read','--state',b,'--force-view')
        text='O-test: Grüße "Quote" $(false)\nZweite Zeile';f=d/'text';f.write_text(text)
        common=['--state',b,'--id','M1','--text-file',str(f),'--sha256',hashlib.sha256(text.encode()).hexdigest()]
        call(new,'submit',*common,'--observed','falsch',ok=False)
        assert 'M1' not in json.loads(pathlib.Path(b).read_text())['deliveries']
        start=time.monotonic();result,_=call(new,'submit',*common,'--observed',x['observed']);latency=time.monotonic()-start
        assert result.get('transport')=='enter_sent',result
        deadline=time.monotonic()+2
        while not out.read_bytes().endswith(b'\r'):
            assert time.monotonic()<deadline;time.sleep(.02)
        assert out.read_bytes()==b'\x1b[200~'+text.encode()+b'\x1b[201~\r',out.read_bytes()
        fresh,_=call(new,'read','--state',b)
        call(new,'submit',*common,'--observed',fresh['observed'],ok=False)
        foreign = ['--state',b,'--id','M2','--text-file',str(f),'--sha256',hashlib.sha256(text.encode()).hexdigest()]
        call(new,'submit',*foreign,'--observed',fresh['observed'],ok=False)
        assert 'M2' not in json.loads(pathlib.Path(b).read_text())['deliveries']
        bad=json.loads(pathlib.Path(b).read_text());bad['identity']['agent_process']='anderer Prozess';badfile=d/'bad.json';badfile.write_text(json.dumps(bad));call(new,'read','--state',str(badfile),ok=False)
        spec=importlib.util.spec_from_file_location('bridge',new);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        footer='Antwort ⏳ 2m10s\n'+'─'*30+'\n❯ \n'+'─'*30+'\n  📁 ~/test   💰 $0.00   ⏳ 2m10s   🏷️ v2.1.276\n'
        normalized=module.normalize_clock(footer)
        assert 'Antwort ⏳ 2m10s' in normalized and '⏳ <Laufzeit>' in normalized
        assert module.normalize_clock(footer.replace('$0.00','$1.00'))!=normalized
        measurement={'scenario':'6 gleiche Abfragen (erste Ansicht + 5 Wiederholungen), UTF-8-JSON-Bytes','old_bytes':oldbytes,'new_bytes':newbytes,'wait_1s_bytes':n,'wait_1s_elapsed':round(duration,3),'cancel_elapsed':round(cancel_time,3),'submit_elapsed':round(latency,3),'model_tool_calls_old_read_paste_read_enter':4,'model_tool_calls_new_read_submit':2}
        print(json.dumps(measurement))
        print('OK: eigenes isoliertes TUI-Testpane; bytegenaues Paste+Enter, Doppel-/Identitätsschutz, Cancel und nur eng verankerte Laufzeitnormalisierung.')
    finally:subprocess.run(tmux+['kill-server'],capture_output=True)
