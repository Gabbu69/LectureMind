"""Run cached unit-test compilation with a smaller Gradle JVM on this 4 GB laptop."""
import os,pathlib,runpy,sys
project=pathlib.Path(__file__).resolve().parent.parent
os.environ['JAVA_OPTS']='-Xms16m -Xmx64m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=24m -XX:ActiveProcessorCount=1'
sys.argv=[sys.argv[0],'testDebugUnitTest','-Dorg.gradle.jvmargs=-Xmx256m -XX:MaxMetaspaceSize=256m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8','--write-locks',*sys.argv[1:]]
runpy.run_path(str(project/'tools/fast_build.py'),run_name='__main__')
