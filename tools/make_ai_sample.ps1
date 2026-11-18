$ErrorActionPreference = 'Stop'
$buildRoot='G:\LectureMind-build'
$env:TEMP=Join-Path $buildRoot 'tmp'
$env:TMP=$env:TEMP
Add-Type -AssemblyName System.Speech
$text=@'
Today's lecture is about photosynthesis. Photosynthesis is the process plants use to convert light energy into chemical energy stored in glucose. Chlorophyll is a green pigment that absorbs light. Photosynthesis takes place in chloroplasts. Plants get carbon dioxide from the air. They get water from the soil through their roots.
The light dependent reactions happen in the thylakoid membranes. These reactions use sunlight and water to produce oxygen, A T P, and N A D P H. Oxygen is released as a byproduct of splitting water. A T P and N A D P H provide energy and reducing power for the Calvin cycle. The Calvin cycle takes place in the stroma. During carbon fixation, carbon dioxide is added to organic molecules. These products help build glucose. Glucose can be used in cellular respiration or stored as starch.
Sa madaling salita, kailangan ng halaman ang liwanag, tubig, at carbon dioxide upang gumawa ng glucose. Ang chlorophyll ang pigment na sumisipsip ng liwanag. Ang oxygen ay byproduct, hindi ito ang pagkain ng halaman.
Three factors affect the rate of photosynthesis: light intensity, carbon dioxide concentration, and temperature. Increasing one factor may raise the rate until another factor becomes limiting. Excessive heat can reduce enzyme activity and lower the rate. Remember the locations: light dependent reactions in the thylakoid membranes, and the Calvin cycle in the stroma.
'@
$directory=Join-Path $buildRoot 'fixtures'
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$text | Set-Content -LiteralPath 'G:\LectureMind\verification\ai-sample-source.txt' -Encoding UTF8
$wav=Join-Path $directory 'synthetic-speech.wav'
$synth=New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
    $synth.Rate=0
    $synth.Volume=80
    $format=New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(16000,[System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen,[System.Speech.AudioFormat.AudioChannel]::Mono)
    $synth.SetOutputToWaveFile($wav,$format)
    $synth.Speak($text)
} finally { $synth.Dispose() }
Get-Item -LiteralPath $wav | Select-Object FullName,Length
