"""Observe the native app's recorder without attaching Android instrumentation."""
import datetime
import json
import pathlib
import re
import subprocess
import sys
import time

project=pathlib.Path(__file__).resolve().parent.parent
report=project/'verification/manual-endurance-monitor.json'
data=json.loads(report.read_text(encoding='utf-8'))
started=datetime.datetime.fromisoformat(data['startedAt'].replace('Z','+00:00')).timestamp()
adb=sys.argv[1]
serial=sys.argv[2]
def query(*arguments):
    result=subprocess.run([adb,'-s',serial,*arguments],capture_output=True,text=True,timeout=20,creationflags=subprocess.CREATE_NO_WINDOW)
    if result.returncode:raise RuntimeError('Device observation failed; no recording command was sent.')
    return result.stdout
while True:
    elapsed=round((time.time()-started)*1000)
    try:
        services=query('shell','dumpsys','activity','services','com.lecturemind.app')
        active='.recording.RecordingService' in services and 'isForeground=true' in services
        memory=query('shell','dumpsys','meminfo','-s','com.lecturemind.app')
        match=re.search(r'TOTAL PSS:\s*([\d,]+)',memory)
        power=query('shell','dumpsys','power')
        state=re.search(r'mWakefulness=(\w+)',power)
        sample={'observedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'hostElapsedMs':elapsed,
                'recorderForeground':active,'pssKb':int(match[1].replace(',','')) if match else None,'powerState':state[1] if state else 'unknown'}
    except (RuntimeError,subprocess.TimeoutExpired,OSError):
        sample={'observedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'hostElapsedMs':elapsed,'observationError':True}
        active=None
    data['samples'].append(sample)
    data['lastObservedAt']=sample['observedAt']
    data['monitorState']='recorder-not-active' if active is False else 'observing'
    if elapsed>=3_606_000:data['monitorState']='observation-complete-awaiting-save'
    try:report.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    except OSError:pass  # Keep observations in RAM and retry G: on the next observation.
    if active is False or elapsed>=3_606_000:break
    time.sleep(30)
