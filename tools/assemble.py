"""Package cached compilation with enough heap for D8, without a compiler/test fork."""
import os
import pathlib
import runpy
import sys

project = pathlib.Path(__file__).resolve().parent.parent
os.environ['JAVA_OPTS']='-Xms16m -Xmx64m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=24m -XX:ActiveProcessorCount=1'
tasks = sys.argv[1:] or ['assembleRelease', 'assembleDebug', 'assembleDebugAndroidTest']
sys.argv = [sys.argv[0], *tasks,
    '-Dorg.gradle.jvmargs=-Xms16m -Xmx512m -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=64m -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8',
    '--write-locks']
runpy.run_path(str(project / 'tools/fast_build.py'), run_name='__main__')
