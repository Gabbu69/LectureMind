import collections,pathlib,re
root=pathlib.Path(__file__).resolve().parent.parent/'.build-fast/outputs/app'
report=root/'intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt'
text=report.read_text(errors='replace')
counts=collections.Counter()
for line in text.splitlines():
    match=re.match(r'^(.*?):(\d+): (Error|Warning): (.*?) \[(\w+)\]$',line)
    if match:
        file,line,severity,message,kind=match.groups();counts[(severity,kind)]+=1
        print(severity,kind,pathlib.Path(file).name+':'+line,message,flush=True)
print('Counts:',dict(counts))
