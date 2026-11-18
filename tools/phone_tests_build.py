"""Run the normal Gradle graph in separate low-memory compiler/test phases."""
import os
import pathlib
import runpy
import sys

project=pathlib.Path(__file__).resolve().parent.parent
os.environ['JAVA_OPTS']='-Xms16m -Xmx64m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=24m -XX:ActiveProcessorCount=1'
tasks=sys.argv[1:] or ['compileDebugAndroidTestKotlin']
sys.argv=[sys.argv[0],*tasks,
    '-Dorg.gradle.jvmargs=-Xms16m -Xmx384m -XX:MaxMetaspaceSize=320m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=32m -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=1 -Dfile.encoding=UTF-8',
    '--offline','--write-locks']
runpy.run_path(str(project/'tools/fast_build.py'),run_name='__main__')
