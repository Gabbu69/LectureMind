"""Run one build phase independently of an interactive command-shell lifetime."""
import datetime
import json
import pathlib
import re
import subprocess
import sys

project=pathlib.Path(__file__).resolve().parent.parent
name,script,*arguments=sys.argv[1:]
assert re.fullmatch(r'[a-z0-9-]+',name)
target=(project/'tools'/script).resolve()
assert target.parent==project/'tools' and target.suffix=='.py' and target.is_file()
directory=project/'.build-fast/logs'
directory.mkdir(parents=True,exist_ok=True)
status=directory/f'job-{name}.json'
log=directory/f'job-{name}.log'
record={'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'script':script,'arguments':arguments,'state':'running'}
status.write_text(json.dumps(record,indent=2),encoding='utf-8')
with log.open('w',encoding='utf-8') as output:
    child=subprocess.Popen([sys.executable,str(target),*arguments],cwd=project,stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    record['childPid']=child.pid
    status.write_text(json.dumps(record,indent=2),encoding='utf-8')
    code=child.wait()
record.update(state='finished',exitCode=code,finishedAt=datetime.datetime.now(datetime.timezone.utc).isoformat())
status.write_text(json.dumps(record,indent=2),encoding='utf-8')
sys.exit(code)
