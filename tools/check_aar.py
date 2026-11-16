import io,sys,urllib.request,zipfile
group,artifact,version=sys.argv[1:] if len(sys.argv)>1 else ['androidx.compose.ui','ui-android','1.12.1']
url=f'https://dl.google.com/dl/android/maven2/{group.replace(".","/")}/{artifact}/{version}/{artifact}-{version}.aar'
with urllib.request.urlopen(url,timeout=60) as response:data=response.read()
with zipfile.ZipFile(io.BytesIO(data)) as archive:print(archive.read('META-INF/com/android/build/gradle/aar-metadata.properties').decode())
