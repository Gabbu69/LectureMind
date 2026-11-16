import ctypes,pathlib,subprocess
root=pathlib.Path('G:/LectureMind-build')
subprocess.run(['tasklist','/FI','IMAGENAME eq java.exe','/FO','CSV','/NH'])
fast=pathlib.Path('G:/LectureMind/.build-fast')
logs=sorted([*(root/'logs').glob('build-*.log'),*(fast/'logs').glob('build-*.log')],key=lambda p:p.stat().st_mtime)
if logs:
    print('Latest log:',logs[-1].name,flush=True)
    with logs[-1].open('rb') as source:
        source.seek(max(0,logs[-1].stat().st_size-7000));data=source.read().decode(errors='replace')
    print('\n'.join(line for line in data.splitlines()[-40:] if not any(x in line.upper() for x in ['PASSWORD','TOKEN=','KEY='])),flush=True)
print('APKs:',[str(p) for base in [root,fast] for p in (base/'outputs/app/outputs/apk').rglob('*.apk')],flush=True)
print('Generated Room sources:',[str(p.relative_to(fast)) for p in (fast/'outputs/app/generated/ksp').rglob('*.java')],flush=True)
print('Project build cache usage MB:',round(sum(p.stat().st_size for p in fast.rglob('*') if p.is_file())/1024**2,1),flush=True)
temporary=sorted((fast/'gradle/.tmp').glob('*'),key=lambda p:p.stat().st_mtime,reverse=True)[:4]
print('Recent downloads:',[(p.name,p.stat().st_size) for p in temporary],flush=True)
