"""Install and test only LectureMind for the phone's verified owner user (0)."""
import datetime,pathlib,subprocess,sys
project=pathlib.Path(__file__).resolve().parent.parent
root=pathlib.Path('G:/LectureMind-build')
fast=project/'.build-fast'
adb='G:/LectureMind-build/sdk/platform-tools/adb.exe'
mode=sys.argv[1] if len(sys.argv)>1 else 'short'
def run(args):subprocess.run([adb,*args],check=True)
if mode=='install':
    for relative in ['debug/app-debug.apk','androidTest/debug/app-debug-androidTest.apk']:
        apk=fast/'outputs/app/outputs/apk'/relative
        if not apk.exists():raise SystemExit('Build the app and test APK before installation: '+str(apk))
        run(['install','--user','0','-r','-g','-t',str(apk)])
    run(['shell','am','start','--user','0','-n','com.lecturemind.app/.MainActivity'])
else:
    classes={'long':'com.lecturemind.app.LongRecordingTest','manual-check':'com.lecturemind.app.ManualEnduranceCheckTest','endurance-cleanup':'com.lecturemind.app.InterruptedEnduranceCleanupTest','recovery-seed':'com.lecturemind.app.RecoverySeedTest','recovery-check':'com.lecturemind.app.RecoveryCheckTest','ai-diagnostic':'com.lecturemind.app.AiTranscriptDiagnosticTest','ai-snapshot':'com.lecturemind.app.AiFixtureSnapshotTest'}.get(mode,'com.lecturemind.app.DeviceTests,com.lecturemind.app.UiAcceptanceTest,com.lecturemind.app.ExportTest')
    args=[adb,'shell','am','instrument','--user','0','-w','-r','-e','class',classes]
    if mode=='long':args+=['-e','longRecording','true']
    if mode=='manual-check':args+=['-e','screenLocked','true']
    args+=['com.lecturemind.app.test/androidx.test.runner.AndroidJUnitRunner']
    logs=fast/'logs';logs.mkdir(parents=True,exist_ok=True)
    path=logs/('device-'+mode+'-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'.log')
    print('Device report:',path,flush=True)
    with path.open('w',encoding='utf-8') as output:
        process=subprocess.Popen(args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,errors='replace')
        failed=False
        for line in process.stdout:
            output.write(line);output.flush();print(line,end='',flush=True)
            if 'FAILURES!!!' in line or 'INSTRUMENTATION_FAILED' in line or 'INSTRUMENTATION_STATUS_CODE: -2' in line:failed=True
        code=process.wait()
    sys.exit(code or int(failed))
