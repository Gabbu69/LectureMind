"""Create a private personal update-signing key outside the source tree."""
import os,pathlib,secrets,subprocess
root=pathlib.Path(os.environ.get('LECTUREMIND_BUILD_ROOT','G:/LectureMind-build'))
private=root/'private';private.mkdir(parents=True,exist_ok=True)
password=private/'signing-password.txt'
if not password.exists():password.write_text(secrets.token_urlsafe(32))
os.environ['LECTUREMIND_STORE_PASSWORD']=password.read_text().strip()
key=private/'lecturemind.jks'
if not key.exists():
    subprocess.run([str(root/'jdk/bin/keytool.exe'),'-genkeypair','-keystore',str(key),'-storepass:env','LECTUREMIND_STORE_PASSWORD','-keypass:env','LECTUREMIND_STORE_PASSWORD','-alias','lecturemind','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=LectureMind Personal, O=LectureMind, C=PH'],check=True)
print('Personal signing key is ready outside the source tree. Keep its password file private.')
