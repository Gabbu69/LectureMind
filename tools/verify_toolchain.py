import hashlib,pathlib,urllib.request,zipfile
root=pathlib.Path('G:/LectureMind-build')
for filename,url in [('gradle-ranged.zip','https://services.gradle.org/distributions/gradle-9.3.1-bin.zip.sha256'),('microsoft-jdk17.zip','https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip.sha256sum.txt')]:
    with urllib.request.urlopen(url,timeout=60) as response:expected=response.read().decode().split()[0]
    sha=hashlib.sha256()
    with (root/'downloads'/filename).open('rb') as source:
        for block in iter(lambda:source.read(1024*1024),b''):sha.update(block)
    assert sha.hexdigest()==expected,'Toolchain checksum mismatch'
    print(filename,'SHA-256 verified',flush=True)
with zipfile.ZipFile(root/'sdk/platforms/android-36/android.jar') as jar:assert 'android/app/Activity.class' in jar.namelist()
print('Android platform archive readable.',flush=True)
