"""Verify a release APK before copying it to the external-drive delivery folder."""
import datetime
import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import sys
import zipfile

project = pathlib.Path(__file__).resolve().parent.parent
root = pathlib.Path(os.environ.get('LECTUREMIND_BUILD_ROOT', 'G:/LectureMind-build'))
os.environ.update(TEMP=str(root/'tmp'), TMP=str(root/'tmp'))
os.environ['JAVA_TOOL_OPTIONS']=(os.environ.get('JAVA_TOOL_OPTIONS','')+' -Djava.io.tmpdir="'+str(root/'tmp')+'"').strip()
apk = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else project / '.build-fast/outputs/app/outputs/apk/release/app-release.apk'
if not apk.is_file():
    raise SystemExit('Build the release APK first.')
destination = root / 'releases'
evidence = project / 'verification'
destination.mkdir(parents=True, exist_ok=True)
evidence.mkdir(exist_ok=True)
java = root / 'jdk/bin/java.exe'
signer = root / 'sdk/build-tools/36.0.0/lib/apksigner.jar'
aapt = root / 'sdk/build-tools/36.0.0/aapt2.exe'
commands = {
    'signature': [str(java), '-Xmx64m', '-XX:+UseSerialGC', '-jar', str(signer), 'verify', '--verbose', '--print-certs', str(apk)],
    'manifest': [str(aapt), 'dump', 'badging', str(apk)],
}
outputs = {}
for name, command in commands.items():
    result = subprocess.run(command, capture_output=True, text=True, errors='replace')
    outputs[name] = result.stdout + result.stderr
    (evidence / (name + '.txt')).write_text(outputs[name], encoding='utf-8')
    if result.returncode:
        raise SystemExit(name + ' verification failed; inspect verification/' + name + '.txt')
assert "package: name='com.lecturemind.app'" in outputs['manifest']
assert "versionName='1.0.0'" in outputs['manifest']
assert "minSdkVersion:'26'" in outputs['manifest'] or "sdkVersion:'26'" in outputs['manifest']
assert "targetSdkVersion:'36'" in outputs['manifest']
assert 'application-debuggable' not in outputs['manifest']
with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None, 'APK ZIP integrity failed'
    names = archive.namelist()
    assert 'AndroidManifest.xml' in names and 'classes.dex' in names
    assert not any(name.endswith(('.jks', '.keystore', '.pem')) for name in names)
    # No known provider API-key format may be embedded in application files.
    import re
    pattern = re.compile(rb'(AIza[0-9A-Za-z_-]{30,}|AQ\.[0-9A-Za-z_-]{30,}|sk-(?:proj-|svcacct-)?[0-9A-Za-z_-]{20,})')
    for name in names:
        if not name.startswith('META-INF/'):
            assert not pattern.search(archive.read(name)), 'Potential provider key found in APK entry: ' + name
target = destination / 'LectureMind-1.0.0.apk'
shutil.copy2(apk, target)
with apk.open('rb') as handle:
    digest = hashlib.file_digest(handle, 'sha256').hexdigest()
with target.open('rb') as handle:
    assert hashlib.file_digest(handle, 'sha256').hexdigest() == digest
report = {
    'verified_at': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'apk': str(target), 'size_bytes': target.stat().st_size, 'sha256': digest,
    'package': 'com.lecturemind.app', 'version': '1.0.0', 'minimum_sdk': 26, 'target_sdk': 36,
    'debuggable': False, 'signature_verified': True, 'zip_integrity': True,
    'embedded_provider_key_pattern_check': 'passed',
    'device_acceptance': 'See VERIFICATION.md and verification/device-short.log; APK inspection alone does not establish device acceptance',
}
(evidence / 'release.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
(destination / 'LectureMind-1.0.0.apk.sha256').write_text(digest + '  ' + target.name + '\n', encoding='ascii')
print(json.dumps(report, indent=2))
