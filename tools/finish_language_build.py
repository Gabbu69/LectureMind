"""Complete the language update in separate, sequential low-memory build phases."""
import datetime
import json
import pathlib
import subprocess
import sys

project=pathlib.Path(__file__).resolve().parent.parent
status=project/'.build-fast/logs/language-pipeline.json'
stages=[
    ('release-compile','phone_tests_build.py',['compileReleaseKotlin']),
    ('unit-compile','compact_build.py',['compileDebugUnitTestKotlin']),
    ('unit-tests','unit_tests.py',['--offline']),
    ('device-test-compile','compact_build.py',['compileDebugAndroidTestKotlin']),
    ('lint','lint_build.py',['lintDebug','lintVitalRelease']),
    ('apks','assemble.py',['assembleRelease','assembleDebugAndroidTest','--offline']),
]
if len(sys.argv)>1:
    start=sys.argv[1]
    position=next(i for i,stage in enumerate(stages) if stage[0]==start)
    stages=stages[position:]
report={'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'state':'running','stages':[]}
for name,script,arguments in stages:
    entry={'name':name,'state':'running'}
    report['stages'].append(entry)
    status.write_text(json.dumps(report,indent=2),encoding='utf-8')
    result=subprocess.run([sys.executable,str(project/'tools'/script),*arguments],cwd=project,creationflags=subprocess.CREATE_NO_WINDOW)
    entry.update(state='finished',exitCode=result.returncode)
    if result.returncode:
        report['state']='failed'
        status.write_text(json.dumps(report,indent=2),encoding='utf-8')
        sys.exit(result.returncode)
report.update(state='finished',finishedAt=datetime.datetime.now(datetime.timezone.utc).isoformat())
status.write_text(json.dumps(report,indent=2),encoding='utf-8')
