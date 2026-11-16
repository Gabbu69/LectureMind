import subprocess
adb='G:/LectureMind-build/sdk/platform-tools/adb.exe'
text=subprocess.check_output([adb,'shell','dumpsys','window','policy'],text=True,errors='replace')
for line in text.splitlines():
    if any(word in line.lower() for word in ['keyguardshowing','keyguardoccluded','showing=','inputrestricted=','keyguarddrawcomplete','mkeyguardshowing']):print(line.strip())
