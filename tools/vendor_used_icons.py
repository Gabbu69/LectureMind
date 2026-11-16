"""Vendor only used Apache-2.0 Material vectors; preserve their exact drawing code."""
import hashlib,json,pathlib,re,urllib.request,zipfile
project=pathlib.Path(__file__).resolve().parent.parent
downloads=pathlib.Path('G:/LectureMind-build/downloads')
archives={kind:zipfile.ZipFile(downloads/f'material-icons-{kind}-sources.jar') for kind in ['extended','core']}
main=project/'app/src/main/java'
used=set()
for source in main.rglob('*.kt'):
    if 'icons' in source.relative_to(main).parts:continue
    used.update(re.findall(r'Icons\.(AutoMirrored\.)?Outlined\.(\w+)',source.read_text(encoding='utf-8')))
destination=main/'com/lecturemind/app/ui/icons';destination.mkdir(parents=True,exist_ok=True)
records=[]
for mirrored,name in sorted(used):
    relative='androidx/compose/material/icons/'+('automirrored/' if mirrored else '')+'outlined/'+name+'.kt'
    matches=[entry for entry in archives['extended'].namelist() if entry.endswith('/'+relative)]
    if not matches:
        assert any(entry.endswith('/'+relative) for entry in archives['core'].namelist()),'Unknown icon: '+relative
        continue
    assert len(matches)==1
    original=archives['extended'].read(matches[0]).decode()
    updated=re.sub(r'^package androidx\.compose\.material\.icons\.[\w.]+$', 'package com.lecturemind.app.ui.icons',original,flags=re.MULTILINE)
    assert original[original.index('public val'):]==updated[updated.index('public val'):]
    target=destination/(('Auto' if mirrored else '')+name+'.kt')
    if target.exists() and target.read_text(encoding='utf-8')!=updated:raise RuntimeError('Review modified icon before replacing: '+str(target))
    target.write_text(updated,encoding='utf-8')
    records.append({'name':('AutoMirrored.' if mirrored else '')+name,'source':matches[0],'source_sha256':hashlib.sha256(original.encode()).hexdigest(),'file':str(target.relative_to(project)).replace('\\','/')})
third=project/'third_party/material-icons';third.mkdir(parents=True,exist_ok=True)
license=third/'LICENSE.txt'
if not license.exists():license.write_bytes(urllib.request.urlopen('https://www.apache.org/licenses/LICENSE-2.0.txt',timeout=30).read())
(third/'NOTICE.txt').write_text('Material icon vectors derived from AndroidX Compose Material Icons 1.7.8.\nCopyright The Android Open Source Project. Apache License 2.0.\nOriginal copyright headers and drawing code are retained; only package declarations change.\nSource: https://dl.google.com/dl/android/maven2/androidx/compose/material/material-icons-extended/1.7.8/material-icons-extended-1.7.8-sources.jar\n')
(third/'sources.json').write_text(json.dumps(records,indent=2))
print('Used icons:',len(used),'Small vendored vectors:',len(records),'Total source bytes:',sum((project/r['file']).stat().st_size for r in records),flush=True)
