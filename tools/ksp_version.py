import urllib.request,xml.etree.ElementTree as ET
url='https://repo.maven.apache.org/maven2/com/google/devtools/ksp/symbol-processing-gradle-plugin/maven-metadata.xml'
root=ET.fromstring(urllib.request.urlopen(url,timeout=30).read())
print('Release:',root.findtext('versioning/release'));print('Latest stable versions:',[e.text for e in root.findall('versioning/versions/version') if e.text.startswith('2.3.')][-10:])
