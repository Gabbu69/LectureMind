"""Read a selected build JVM's stack when diagnosing a stalled task."""
import pathlib,subprocess,sys
root=pathlib.Path('G:/LectureMind-build')
text=subprocess.check_output([str(root/'jdk/bin/jcmd.exe'),'-J-Xmx32m',sys.argv[1],'Thread.print'],text=True,timeout=45)
for block in text.split('\n\n'):
    if block.startswith('"') and 'java.lang.Thread.State: RUNNABLE' in block and any(word in block for word in ['Socket','org.jetbrains','ksp','Execution','FileDispatcher','com.android.tools.r8']):
        print('\n'.join(block.splitlines()[:38]),flush=True)
