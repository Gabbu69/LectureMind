import pathlib,re,time
root=pathlib.Path('G:/LectureMind/.build-fast/gradle')
files=sorted((root/'.tmp').glob('gradle_download*'),key=lambda p:p.stat().st_mtime,reverse=True)[:4]
for file in files:
    print(file.name,round(file.stat().st_size/1024**2,2),'MB',flush=True)
    if file.stat().st_size>1024**2:
        with file.open('rb') as source:data=source.read(65536)
        entries=[]
        for match in re.finditer(b'PK\x03\x04',data):
            pos=match.start();n=int.from_bytes(data[pos+26:pos+28],'little');name=data[pos+30:pos+30+n]
            if 0<n<180 and re.fullmatch(rb'[\w/.$-]+',name):entries.append(name.decode())
        print('Archive entries:',entries[:6],flush=True)
