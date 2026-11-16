"""Audit resolved Maven coordinates against the public OSV advisory database."""
import json,pathlib,urllib.request
project=pathlib.Path(__file__).resolve().parent.parent
lock=project/'app/gradle.lockfile'
if not lock.exists():raise SystemExit('Resolve and lock dependencies before auditing.')
coordinates=[]
for line in lock.read_text().splitlines():
    if line.startswith('#') or '=' not in line:continue
    value=line.split('=')[0].split(':')
    if len(value)==3:coordinates.append(value)
queries=[{'package':{'ecosystem':'Maven','name':group+':'+artifact},'version':version} for group,artifact,version in coordinates]
payload=json.dumps({'queries':queries}).encode()
req=urllib.request.Request('https://api.osv.dev/v1/querybatch',data=payload,headers={'Content-Type':'application/json'})
with urllib.request.urlopen(req,timeout=90) as response:results=json.load(response)['results']
findings=[]
for coordinate,result in zip(coordinates,results):
    for vuln in result.get('vulns',[]):
        with urllib.request.urlopen('https://api.osv.dev/v1/vulns/'+vuln['id'],timeout=60) as response:detail=json.load(response)
        findings.append({'coordinate':':'.join(coordinate),'id':vuln['id'],'summary':detail.get('summary'),'severity':detail.get('severity',[]),'references':detail.get('references',[]),'affected':detail.get('affected',[])})
report={'packages_checked':len(coordinates),'findings':findings}
(project/'dependency-audit.json').write_text(json.dumps(report,indent=2))
print('Resolved packages checked:',len(coordinates),'Advisories:',len(findings))
for finding in findings:print(finding['coordinate'],finding['id'],finding['summary'])
