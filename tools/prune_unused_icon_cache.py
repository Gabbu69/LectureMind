"""Remove only the unused all-icons cache entries owned by this project's temporary build."""
import os,pathlib,shutil
project=pathlib.Path(__file__).resolve().parent.parent
root=(project/'.build-fast').resolve()
assert root.parent==project.resolve() and (root/'.lecturemind-owned').read_text().strip()
transforms=root/'gradle/caches/9.3.1/transforms'
targets=[]
for child in transforms.iterdir():
    if child.is_dir() and any('material-icons-extended' in p.name for p in child.rglob('*') if p.is_file()):targets.append(child)
store=root/'gradle/caches/modules-2/files-2.1/androidx.compose.material'
for name in ['material-icons-extended','material-icons-extended-android']:
    if (store/name).exists():targets.append(store/name)
total=0
for target in targets:
    resolved=target.resolve();assert resolved.is_relative_to(root) and len(resolved.relative_to(root).parts)>=5
    for directory,dirs,files in os.walk(resolved):
        for name in dirs:
            candidate=pathlib.Path(directory)/name
            assert not candidate.is_symlink() and not candidate.is_junction()
        total+=sum((pathlib.Path(directory)/name).stat().st_size for name in files)
    shutil.rmtree(resolved)
print('Removed unused icon caches:',len(targets),'folders;',round(total/1024**2,1),'MiB reclaimed.',flush=True)
