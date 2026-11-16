"""Low-memory Windows build launcher with all mutable build state on the external drive."""
import datetime,os,pathlib,shutil,subprocess,sys,threading,time
project=pathlib.Path(__file__).resolve().parent.parent
root=pathlib.Path(os.environ.get('LECTUREMIND_BUILD_ROOT','G:/LectureMind-build'))
os.environ.update(LECTUREMIND_BUILD_ROOT=str(root),GRADLE_USER_HOME=str(root/'gradle'),JAVA_HOME=str(root/'jdk'),ANDROID_HOME=str(root/'sdk'),ANDROID_SDK_ROOT=str(root/'sdk'),TEMP=str(root/'tmp'),TMP=str(root/'tmp'))
fast=os.environ.get('LECTUREMIND_FAST_ROOT')
cache_root=pathlib.Path(fast) if fast else root
if fast:
    os.environ.update(GRADLE_USER_HOME=str(cache_root/'gradle'),TEMP=str(cache_root/'tmp'),TMP=str(cache_root/'tmp'),LECTUREMIND_OUTPUT_ROOT=str(cache_root/'outputs'))
    (cache_root/'tmp').mkdir(parents=True,exist_ok=True)
password=root/'private/signing-password.txt'
if password.exists():os.environ['LECTUREMIND_STORE_PASSWORD']=password.read_text().strip()
os.environ['JAVA_TOOL_OPTIONS']=(os.environ.get('JAVA_TOOL_OPTIONS','')+' -Djava.io.tmpdir="'+os.environ['TEMP']+'"').strip()

tasks=sys.argv[1:] or ['assembleDebug','testDebugUnitTest','lintDebug']
command=[str(root/'tools/gradle-9.3.1/bin/gradle.bat'),'--project-dir',str(project),'--project-cache-dir',str(cache_root/'project-cache'),'--console=plain',*tasks]
logs=cache_root/'logs';logs.mkdir(parents=True,exist_ok=True)
log=logs/('build-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'.log')
print('Build log:',log,flush=True)
with log.open('w',encoding='utf-8') as output:
    process=subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,errors='replace')
    def guard():
        while fast and process.poll() is None:
            size=0
            entries=cache_root.rglob('*') if cache_root.drive.upper()=='D:' else []
            for path in entries:
                try:
                    if path.is_file():size+=path.stat().st_size
                except FileNotFoundError:pass
            if (cache_root.drive.upper()=='D:' and size>1.4*1024**3) or shutil.disk_usage(cache_root).free<1.1*1024**3:
                print('Build storage guard reached; stopping this build.',flush=True)
                subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],stdout=subprocess.DEVNULL)
                return
            time.sleep(3)
    if fast:threading.Thread(target=guard,daemon=True).start()
    for line in process.stdout:
        output.write(line);output.flush()
        if '--info' not in tasks or any(x in line for x in ['FAILURE:','BUILD ','> Task', 'What went wrong:','e: ','error:','Could not','Exception']):print(line,end='',flush=True)
    result=process.wait()
    output.write('\nPROCESS_EXIT_CODE='+str(result)+'\n');output.flush()
sys.exit(result)
