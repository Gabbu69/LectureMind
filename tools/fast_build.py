"""Launch the project-local build cache; tools and signing remain on G:."""
import os,pathlib,runpy
project=pathlib.Path(__file__).resolve().parent.parent
fast=project/'.build-fast'
if not (fast/'.lecturemind-owned').is_file():raise SystemExit('Run prepare_fast_cache.py to create the project-owned cache.')
os.environ['LECTUREMIND_FAST_ROOT']=str(fast)
runpy.run_path(str(project/'tools/build.py'),run_name='__main__')
