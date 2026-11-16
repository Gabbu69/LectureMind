import os,pathlib,time,uuid
for root in [pathlib.Path('G:/LectureMind-build/tmp'),pathlib.Path('G:/LectureMind')]:
    path=root/('lecturemind-io-probe-'+uuid.uuid4().hex+'.tmp')
    started=time.monotonic()
    try:
        with path.open('xb',buffering=0) as output:
            for _ in range(128):output.write(bytes(8192))
            os.fsync(output.fileno())
        print(root.drive,'1 MB / 128 writes:',round(time.monotonic()-started,3),'seconds',flush=True)
    finally:
        if path.exists():path.unlink()
