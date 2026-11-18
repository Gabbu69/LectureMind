# LectureMind

A personal Android lecture recorder and AI study app. Record only with permission.

## On your phone

1. Open **LectureMind → Settings** and select **Gemini** or **OpenAI**.
2. Paste your own provider API key into the masked key field and tap **Save key**, then **Test connection**. A provider subscription/login is not an API key. Provider usage and account quotas apply.
3. Tap **Record a lecture**, enter a title and subject, confirm that you have permission, and grant microphone access.
4. Use **Mark important moment** for audio bookmarks. Pause/resume as needed. **Save** stops the microphone.
5. Open **Generate study materials**, review the provider/content disclosure, choose 10/20/30 questions, and tap **Send & generate**.
6. Read/edit Notes, study the Reviewer, and take a Quiz. Answers appear after submission. Audio/source chips return to the referenced recording section.
7. Share notes/reviewers as PDF or UTF-8 text using the share icon. Export important content before uninstalling.

Recordings and generated materials work offline. AI generation requires internet and a usable API key. The transcription requests explicitly cover Ilonggo/Hiligaynon, Bisaya/Cebuano, Tagalog/Filipino, English and code-switching, while preserving original speech. Settings offers these regional study-output choices; the earlier saved Filipino preference maps to Filipino / Tagalog. Provider recognition accuracy, local accents and translated study quality still require native-speaker acceptance. Check AI output against the original audio. OpenAI's plain transcript links to the start of each five-minute audio section rather than claiming precise sentence timestamps.

## Storage and privacy

- Mono 16 kHz, 16-bit WAV: approximately 115 MB/hour; five-minute independently recoverable segments.
- App-private recordings and Room database. Android cloud backup is disabled. Local content is protected by Android's app sandbox/device encryption, not a separate encrypted lecture vault.
- API keys are AES-GCM encrypted with a non-exportable Android Keystore key; no provider keys are embedded in the APK, logs, exports, or source.
- Settings uses Android's secure-window flag to prevent screenshots of key entry. Never paste your API key into a chat or issue.
- AI upload happens only after your explicit in-app confirmation. No analytics, ads, LectureMind accounts, or LectureMind server.
- Gemini Interactions and OpenAI Responses use `store=false`. Providers may still retain data under their own policies; this is not a promise of zero provider retention.
- Cancelling stops local processing/network calls where possible. Provider requests already received can still be charged.
- Lecture deletion removes its local audio, notes, quiz attempts and temporary job files. Exports you shared elsewhere are separate files. Uninstalling removes the local library and saved provider keys.

## Build on Windows

The source is in `G:\LectureMind`, and the external toolchain/delivery root defaults to `G:\LectureMind-build`. Override `LECTUREMIND_BUILD_ROOT` when needed. Tools, signing material, large fixtures and final deliverables stay on G:. The project-owned cache and compiled outputs are in `G:\LectureMind\.build-fast`. Both launchers set temporary directories on G:, including Java's `java.io.tmpdir`. The launcher stops if less than 1.1 GiB is free. The earlier 1.5 GiB temporary-cache cap applies only when explicitly using D:.

- JDK 17, Gradle 9.3.1, Android Gradle Plugin 9.1.1.
- Android SDK platform 36 and Build Tools 36.0.0, minimum API 26.
- Kotlin/Compose, Material 3, Room/KSP, Media3, WorkManager, OkHttp.
- `python tools/download_tools.py` downloads Gradle and JDK 17 from official distributors and verifies their published SHA-256 checksums. Install/copy Android SDK platform 36, Build Tools 36.0.0 and platform-tools into the build root's `sdk` directory.
- `python tools/create_signing_key.py` creates your private signing material when setting up a new build environment.
- `./build.ps1` runs debug assembly, unit tests and Android lint.
- `python tools/build.py assembleDebug testDebugUnitTest lintDebug` offers the same build using a lighter Windows command launcher.
- `./build.ps1 -Tasks assembleRelease` builds the personally signed installation APK.
- `./build.ps1 -Tasks assembleDebugAndroidTest` builds device tests.
- Signing key: external build root `private/lecturemind.jks`; password file: `private/signing-password.txt`. Preserve these together securely if you want to install future updates without uninstalling. Do not commit them.

For this 4 GB laptop, use separate processes and one build at a time:

```powershell
python tools/phone_tests_build.py kspDebugKotlin
python tools/phone_tests_build.py compileDebugKotlin
python tools/phone_tests_build.py compileDebugUnitTestKotlin
python tools/unit_tests.py
python tools/lint_build.py
python tools/phone_tests_build.py kspReleaseKotlin
python tools/phone_tests_build.py compileReleaseKotlin
python tools/assemble.py assembleRelease
python tools/verify_release.py
```

The smaller unit-test launcher assumes compilation is already cached. `tools/assemble.py` gives D8 packaging a 512 MiB heap with a lower metaspace cap; its compilation and lint inputs should already be cached. Without arguments it builds release, debug, and instrumentation APKs. Separating lint from Kotlin compilation also avoids an observed Kotlin analysis-plugin crash. No lint rules are disabled. Use `tools/build.py` instead of `tools/fast_build.py` to put all mutable build state under `G:\LectureMind-build`. The fast launcher requires its `.lecturemind-owned` marker. Build outputs are under the selected cache root's `outputs` directory.

On this laptop an interactive PowerShell process can fail under memory pressure while its Gradle child continues. Check the active processes and daemon log before retrying. The following launcher keeps one build phase independent of the interactive shell and records its exit code:

```powershell
Start-Process -WindowStyle Hidden -FilePath C:\Python314\python.exe -ArgumentList @('G:\LectureMind\tools\run_detached.py', 'release', 'assemble.py', 'assembleRelease', '--offline')
Get-Content G:\LectureMind\.build-fast\logs\job-release.json
```

Wait until the job reports `finished` before starting another phase. The launcher writes its log and status under `.build-fast/logs` on G:. Do not put API keys in build commands or build environment files.

Windows Application Control blocked Room's build-time SQLite JDBC native library during a later optional timestamp experiment on this laptop. The delivered APK/source use the previously completed, tested regional-language implementation; the unbuilt experiment is excluded. Do not disable Application Control to reproduce a build. Use an approved toolchain or build environment if that policy blocks SQLite's compiler component. This is a host build restriction, not an Android runtime dependency.

The release is personally signed, not a Google Play release. `tools/verify_release.py` checks the signature, manifest, ZIP integrity and common embedded-provider-key patterns, then copies the APK to `G:\LectureMind-build\releases`. `tools/package_source.py` creates a source-only archive there without caches, recordings, API keys or signing files.

Only the 24 additional Material icons used by the app are vendored, with their upstream drawing code and Apache-2.0 notices preserved in `third_party/material-icons`. The core icon library remains a normal dependency. Dependency versions are locked in `app/gradle.lockfile`.

## Verification

See `VERIFICATION.md` for actual results and pending acceptance checks. Local fixture/provider contract tests do not establish live AI quality. The one-hour microphone endurance test is explicitly opt-in and never uploads its captured audio.

For an endurance run that does not keep UiAutomator attached during capture, start a normal app recording titled `Lecture mind endurance check 2026-11-16`, subject `Verification`, and save it after at least 60 minutes. Only then invoke `tools/device_tests.py manual-check`. This opt-in test checks segment order, WAV headers, duration, file sizes and playback across a boundary, then removes only that specifically named verification lecture and its audio. The `screenLocked` argument records host-observed screen-lock evidence; it does not establish that the screen stayed off every second. `tools/monitor_manual_recording.py` observes foreground-service status, memory and power state without sending recording controls or uploading audio. It requires the recording start metadata in `verification/manual-endurance-monitor.json` and explicit ADB path/device arguments.

## Official implementation references

- [Android microphone foreground service](https://developer.android.com/develop/background-work/services/fgs/service-types#microphone)
- [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord)
- [Room](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Gemini audio](https://ai.google.dev/gemini-api/docs/audio) and [structured output](https://ai.google.dev/gemini-api/docs/structured-output)
- [OpenAI file transcription](https://developers.openai.com/api/docs/guides/speech-to-text) and [structured output](https://developers.openai.com/api/docs/guides/structured-outputs)
- [AGP compatibility](https://developer.android.com/build/releases/agp-9-1-0-release-notes)

The build uses Compose BOM `2026.03.01` (UI 1.10.6, Material 3 1.4.0) to retain the planned SDK 36 compilation target. The newer UI 1.12.1 artifact requires compile SDK 37; this was checked in the published Android archive metadata.
