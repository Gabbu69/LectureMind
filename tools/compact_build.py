"""Retry cached compiler phases with a smaller native/JVM footprint."""
import os
import pathlib
import runpy
import sys

project=pathlib.Path(__file__).resolve().parent.parent
os.environ['JAVA_OPTS']='-Xms16m -Xmx48m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=16m -XX:ActiveProcessorCount=1'
tasks=sys.argv[1:]
assert tasks
sys.argv=[sys.argv[0],*tasks,
    '-Dorg.gradle.jvmargs=-Xms16m -Xmx320m -XX:MaxMetaspaceSize=256m -XX:CompressedClassSpaceSize=64m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=24m -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=1 -Xss512k -Dfile.encoding=UTF-8',
    '--offline','--write-locks']
runpy.run_path(str(project/'tools/fast_build.py'),run_name='__main__')
