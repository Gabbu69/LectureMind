import pathlib,urllib.request,zipfile
root=pathlib.Path('G:/LectureMind-build/downloads')
root.mkdir(parents=True,exist_ok=True)
for artifact in ['material-icons-extended','material-icons-core']:
    url=f'https://dl.google.com/dl/android/maven2/androidx/compose/material/{artifact}/1.7.8/{artifact}-1.7.8-sources.jar'
    path=root/(artifact+'-sources.jar')
    if not path.exists():
        with urllib.request.urlopen(url,timeout=90) as response,path.open('wb') as output:
            import shutil;shutil.copyfileobj(response,output,1024*1024)
    print(artifact,round(path.stat().st_size/1024**2,2),'MB',flush=True)
    with zipfile.ZipFile(path) as archive:
        suffix='/outlined/School.kt' if artifact.endswith('extended') else '/Icons.kt'
        names=[name for name in archive.namelist() if name.endswith(suffix)]
        if names:print(names[0],archive.read(names[0]).decode()[:8500],flush=True)
