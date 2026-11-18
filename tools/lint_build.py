"""Give lint enough heap while keeping its other JVM allocations bounded."""
import os
import pathlib
import runpy
import sys

project=pathlib.Path(__file__).resolve().parent.parent
os.environ['JAVA_OPTS']='-Xms16m -Xmx48m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=16m -XX:ActiveProcessorCount=1'
tasks=sys.argv[1:] or ['lintDebug','lintVitalRelease']
sys.argv=[sys.argv[0],*tasks,
    '-Dorg.gradle.jvmargs=-Xms16m -Xmx512m -XX:MaxMetaspaceSize=224m -XX:CompressedClassSpaceSize=64m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=24m -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=1 -Xss512k -Dfile.encoding=UTF-8',
    '--offline','--write-locks']
runpy.run_path(str(project/'tools/fast_build.py'),run_name='__main__')
