param([string[]]$Tasks = @('assembleDebug','testDebugUnitTest','lintDebug'))
$ErrorActionPreference = 'Stop'
$buildRoot = if($env:LECTUREMIND_BUILD_ROOT){$env:LECTUREMIND_BUILD_ROOT}else{'G:\LectureMind-build'}
$env:LECTUREMIND_BUILD_ROOT = $buildRoot
$env:GRADLE_USER_HOME = Join-Path $buildRoot 'gradle'
$env:JAVA_HOME = Join-Path $buildRoot 'jdk'
$env:ANDROID_HOME = Join-Path $buildRoot 'sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:TEMP = Join-Path $buildRoot 'tmp'
$env:TMP = $env:TEMP
$env:JAVA_TOOL_OPTIONS = ($env:JAVA_TOOL_OPTIONS + ' -Djava.io.tmpdir="' + $env:TEMP + '"').Trim()
New-Item -ItemType Directory -Force -Path $env:TEMP | Out-Null
if(Test-Path -LiteralPath (Join-Path $buildRoot 'private\signing-password.txt')) { $env:LECTUREMIND_STORE_PASSWORD = (Get-Content -Raw -LiteralPath (Join-Path $buildRoot 'private\signing-password.txt')).Trim() }
& (Join-Path $buildRoot 'tools\gradle-9.3.1\bin\gradle.bat') --project-dir $PSScriptRoot --project-cache-dir (Join-Path $buildRoot 'project-cache') @Tasks
exit $LASTEXITCODE
