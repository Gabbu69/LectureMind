import collections,os,pathlib,shutil
root=pathlib.Path(__file__).resolve().parent.parent/'.build-fast'
totals=collections.Counter();largest=[]
for directory,dirs,files in os.walk(root):
    for name in files:
        path=pathlib.Path(directory)/name
        try:size=path.stat().st_size
        except FileNotFoundError:continue
        relative=path.relative_to(root)
        category='/'.join(relative.parts[:4])
        totals[category]+=size
        largest.append((size,str(relative)))
print(root.drive+' free GiB:',round(shutil.disk_usage(root).free/1024**3,2))
print('Temporary total MiB:',round(sum(totals.values())/1024**2,1))
for name,size in totals.most_common(12):print(round(size/1024**2,1),name)
print('Largest files:')
for size,name in sorted(largest,reverse=True)[:12]:print(round(size/1024**2,1),name)
