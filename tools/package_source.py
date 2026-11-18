"""Create a source-only delivery archive, excluding caches and signing material."""
import hashlib
import json
import os
import pathlib
import re
import zipfile

project = pathlib.Path(__file__).resolve().parent.parent
root = pathlib.Path(os.environ.get('LECTUREMIND_BUILD_ROOT', 'G:/LectureMind-build'))
release = root / 'releases'
release.mkdir(parents=True, exist_ok=True)
excluded_dirs = {'.git', '.gradle', '.kotlin', '.build-fast', 'build', '__pycache__', 'private'}
excluded_names = {'local.properties', 'signing-password.txt'}
excluded_suffixes = {'.jks', '.keystore', '.pem', '.env', '.hprof', '.apk', '.aab', '.pyc'}
secret = re.compile(rb'(AIza[0-9A-Za-z_-]{30,}|AQ\.[0-9A-Za-z_-]{30,}|sk-(?:proj-|svcacct-)?[0-9A-Za-z_-]{20,})')
files = []
candidates = []
for directory, dirs, names in os.walk(project):
    dirs[:] = [name for name in dirs if name not in excluded_dirs]
    candidates.extend(pathlib.Path(directory) / name for name in names)
for path in sorted(candidates):
    relative = path.relative_to(project)
    if not path.is_file() or any(p in excluded_dirs for p in relative.parts):
        continue
    if path.name in excluded_names or path.suffix in excluded_suffixes or path.name.startswith(('.env', 'hs_err_', 'replay_')):
        continue
    data = path.read_bytes()
    if secret.search(data):
        raise SystemExit('Potential provider key detected in ' + str(relative))
    files.append((relative.as_posix(), path, hashlib.sha256(data).hexdigest()))
assert any(name == 'app/src/main/AndroidManifest.xml' for name, _, _ in files)
assert any(name == 'VERIFICATION.md' for name, _, _ in files)
target = release / 'LectureMind-1.0.0-source.zip'
with zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for name, path, _ in files:
        archive.write(path, 'LectureMind/' + name)
with zipfile.ZipFile(target) as archive:
    assert archive.testzip() is None
    assert len(archive.namelist()) == len(files)
    for name, _, digest in files:
        assert hashlib.sha256(archive.read('LectureMind/' + name)).hexdigest() == digest
manifest = {'archive': str(target), 'files': len(files), 'size_bytes': target.stat().st_size,
            'sha256': hashlib.sha256(target.read_bytes()).hexdigest(),
            'contents': {name: digest for name, _, digest in files}}
(release / 'source-manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
print('Source archive:', target)
print('Verified files:', len(files), 'Archive bytes:', target.stat().st_size)
print('SHA-256:', manifest['sha256'])
