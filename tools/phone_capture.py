"""Capture the foreground LectureMind screen without text/binary shell conversion."""
import pathlib
import subprocess
import sys

root=pathlib.Path('G:/LectureMind-build')
adb=str(root/'sdk/platform-tools/adb.exe')
command=[adb,'-s','13030374AN000103']
state=subprocess.check_output(command+['shell','dumpsys','activity','activities'],text=True,errors='replace')
resumed=[line for line in state.splitlines() if 'topResumedActivity=' in line]
if not any('com.lecturemind.app/.MainActivity' in line for line in resumed):
    raise SystemExit('LectureMind is not foreground; no screenshot captured.')
name=sys.argv[1] if len(sys.argv)>1 else 'library'
assert name.replace('-','').replace('_','').isalnum()
target=root/'releases/screenshots'/f'{name}.png'
target.parent.mkdir(parents=True,exist_ok=True)
data=subprocess.check_output(command+['exec-out','screencap','-p'])
assert data.startswith(b'\x89PNG\r\n\x1a\n')
target.write_bytes(data)
print(target)
