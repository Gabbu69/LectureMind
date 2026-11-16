"""Bounded ranged downloads; never writes to the Windows system drive."""
import concurrent.futures, hashlib, os, pathlib, shutil, urllib.request, zipfile
ROOT=pathlib.Path(os.environ.get('LECTUREMIND_BUILD_ROOT','G:/LectureMind-build'))
ROOT.joinpath('downloads').mkdir(parents=True,exist_ok=True)
def digest(file):
    sha=hashlib.sha256()
    with file.open('rb') as source:
        for block in iter(lambda:source.read(1024*1024),b''):sha.update(block)
    return sha.hexdigest()
def ranged(url, name, checksum=None):
    target=ROOT/'downloads'/name
    request=urllib.request.Request(url,headers={'Range':'bytes=0-0'})
    with urllib.request.urlopen(request,timeout=90) as r:
        size=int(r.headers['Content-Range'].split('/')[-1])
    def chunk(i):
        start=i*size//8; end=(i+1)*size//8-1
        part=target.with_suffix('.part'+str(i))
        if part.exists() and part.stat().st_size==end-start+1:return part
        with urllib.request.urlopen(urllib.request.Request(url,headers={'Range':f'bytes={start}-{end}'}),timeout=120) as r,part.open('wb') as w:
            if r.status!=206:raise RuntimeError('Range request refused')
            shutil.copyfileobj(r,w,256*1024)
        if part.stat().st_size!=end-start+1:raise RuntimeError('Incomplete download')
        print(name,'part',i+1,'ready',flush=True);return part
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool: parts=list(pool.map(chunk,range(8)))
    with target.open('wb') as w:
        for part in parts:
            with part.open('rb') as r:shutil.copyfileobj(r,w)
    if checksum and digest(target)!=checksum:raise RuntimeError('Checksum mismatch')
    with zipfile.ZipFile(target) as z:z.testzip(); z.extractall(ROOT/'tools')
    print(name,'extracted',flush=True)
def gradle():
    if (ROOT/'tools/gradle-9.3.1/bin/gradle.bat').exists():return
    url='https://services.gradle.org/distributions/gradle-9.3.1-bin.zip'
    with urllib.request.urlopen(url+'.sha256') as r: sha=r.read().decode().strip()
    ranged(url,'gradle-ranged.zip',sha)
def jdk():
    if (ROOT/'jdk/bin/java.exe').exists():return
    url='https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip'
    with urllib.request.urlopen(url+'.sha256sum.txt',timeout=90) as r: sha=r.read().decode().split()[0]
    ranged(url,'microsoft-jdk17.zip',sha)
    folder=next(p for p in (ROOT/'tools').glob('jdk-17*') if p.is_dir())
    if not (ROOT/'jdk').exists():shutil.move(str(folder),str(ROOT/'jdk'))
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    for job in [pool.submit(gradle),pool.submit(jdk)]:job.result()
