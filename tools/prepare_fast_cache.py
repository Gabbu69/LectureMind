"""Prepare the project-owned cache on G:. The legacy D: cap applies on D: only."""
import json,pathlib,shutil,subprocess,time,uuid
project=pathlib.Path(__file__).resolve().parent.parent
target=project/'.build-fast'
assert target.resolve().parent==project.resolve()
target.mkdir(exist_ok=True)
marker=target/'.lecturemind-owned'
if not marker.exists():marker.write_text(str(uuid.uuid4()))
assert marker.read_text().strip()
source=pathlib.Path('G:/LectureMind-build/gradle/caches')
destination=target/'gradle/caches'
process=subprocess.Popen(['robocopy',str(source),str(destination),'/E','/MT:4','/R:0','/W:0','/XF','*.lock','/XD','generated-gradle-jars','kotlin-dsl','/NFL','/NDL','/NJH','/NJS','/NP'],stdout=subprocess.DEVNULL)
while process.poll() is None:
    if shutil.disk_usage(project).free < 1024**3:
        process.terminate();raise RuntimeError('Build-drive free-space guard stopped copying.')
    time.sleep(2)
if process.returncode>=8:raise RuntimeError('Cache copy failed with code '+str(process.returncode))
size=sum(p.stat().st_size for p in target.rglob('*') if p.is_file())
if project.drive.upper()=='D:':assert size<=1.5*1024**3
print('Temporary cache copied:',round(size/1024**2,1),'MB;',project.drive,'free:',round(shutil.disk_usage(project).free/1024**3,2),'GB',flush=True)
