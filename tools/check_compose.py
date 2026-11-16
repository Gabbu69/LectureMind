import sys,urllib.request,xml.etree.ElementTree as ET
ns={'m':'http://maven.apache.org/POM/4.0.0'}
version=sys.argv[1] if len(sys.argv)>1 else '2026.03.01'
url=f'https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/{version}/compose-bom-{version}.pom'
root=ET.fromstring(urllib.request.urlopen(url,timeout=30).read())
for dep in root.findall('.//m:dependency',ns):
    group=dep.findtext('m:groupId',namespaces=ns);name=dep.findtext('m:artifactId',namespaces=ns);version=dep.findtext('m:version',namespaces=ns)
    if (group,name) in [('androidx.compose.ui','ui'),('androidx.compose.material3','material3')]: print(group,name,version)
