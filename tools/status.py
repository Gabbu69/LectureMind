from pathlib import Path
p=Path('G:/LectureMind-build')
print('Gradle ready:',(p/'tools/gradle-9.3.1/bin/gradle.bat').exists(),flush=True)
print('JDK ready:',(p/'jdk/bin/java.exe').exists(),flush=True)
print('JDK extraction executable:',(p/'tools/jdk-17.0.21.0.2+1/bin/java.exe').exists(),flush=True)
print('SDK build tools:',[x.name for x in (p/'sdk/build-tools').glob('*')],flush=True)
cache=p/'gradle/caches/modules-2/files-2.1'
files=list(cache.rglob('*.jar')) if cache.exists() else []
print('Downloaded dependency jars:',len(files),'MB:',round(sum(x.stat().st_size for x in files)/1024/1024,1),flush=True)
generated=p/'gradle/caches/9.3.1/generated-gradle-jars'
print('Generated Gradle API:',[(x.name,x.stat().st_size) for x in generated.glob('*')],flush=True)
temporary=sorted((p/'gradle/.tmp').glob('*'),key=lambda x:x.stat().st_mtime,reverse=True)[:5]
print('Active cache files:',[(x.name,x.stat().st_size) for x in temporary],flush=True)
