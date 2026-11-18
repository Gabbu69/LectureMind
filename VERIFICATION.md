# Verification — LectureMind 1.0.0

Review: **18 November 2026** · Package: **com.lecturemind.app** · Minimum API 26, target API 36

This is an early-access personal Android app. Local recording checks and live AI quality are separate. The APK below is published as the regional-language update, retaining the original release separately.

## Build checks

| Check | Observed result | Evidence |
| --- | --- | --- |
| Final debug and signed release assembly | Passed; 106 tasks, 2 minutes 39 seconds | [Assembly log](verification/apk-build.log), [job exit status](verification/final-apks-job.json) |
| JVM unit suite on updated production source | 14 tests, zero failures/errors/skips; 7.341 seconds | [JUnit XML](verification/unit-tests.xml), [build log](verification/unit-build.log) |
| Android lint and release vital lint | Passed on the final production/test source; zero errors and 11 warnings | [Lint report](verification/lint-results-debug.html), [build log](verification/lint-build.log) |
| Release signature | Verified; v2 signature, RSA 3072-bit, one signer | [Signature report](verification/signature.txt) |
| Manifest | Correct package/version; minimum API 26, target API 36; not debuggable | [Manifest report](verification/manifest.txt) |
| ZIP integrity and embedded provider-key patterns | Passed; checks include Google AIza/AQ. and OpenAI key formats | [Release report](verification/release.json) |
| Locked Maven advisory scan | Earlier scan: 199 coordinates, zero reported advisories; dependency versions unchanged in this update | [Audit result](dependency-audit.json) |

The unit suite checks source references, malformed JSON, invalid quiz answers, repeated questions, insufficient content, scoring, Taglish transcript round-trips, WAV headers/repair, boundary seeking and checkpoint reuse. Its two-hour fixture creates 24 five-minute WAV parts, simulates a failure on transcription call thirteen, then reuses the first twelve completed sections on retry. It checks file handling and retry behavior, not two hours of microphone capture or live AI.

The 4 GB laptop encountered native memory failures and an interactive shell crash. The latest regional-language unit suite completed successfully with its full Gradle log and JUnit XML retained. Final APK assembly completed through a detached launcher with an explicit zero exit code. Full JVM crash dumps remain private because they can contain process environment data.

The 11 lint warnings concern dependency updates and optional API/Kotlin improvements. No lint baseline or disabled rules hide errors. The advisory scan is not a security guarantee.

## Physical phone checks

Device: **TECNO KL7, Android 14 / API 34**, owner user 0. Results apply to this phone, not a broad device matrix.

| Check | Observed result | Evidence |
| --- | --- | --- |
| Installed regional-language release instrumentation | Six tests passed in 37.33 seconds | [Device log](verification/device-short.log) |
| Real recording interruption/recovery | Started real microphone capture, externally force-stopped without Save, then reopened; recovery check passed | [Recovery log](verification/device-recovery.log) |
| Short recorder controls | Capture, pause/resume timing, bookmark command, Home/background continuation and finalized WAV files passed | [Device log](verification/device-short.log) |
| Playback and study screens | Controlled local fixture: playlist playback, notes editing, quiz submission and scoring passed | [Device log](verification/device-short.log) |
| PDF/text export | Multi-page rendering assertions, UTF-8 sharing and owned-file cleanup passed; first/last pages visually inspected | [Device log](verification/device-short.log) |
| Baseline visual checks | Portrait, landscape, font scale 1.3, light and dark themes inspected; font/rotation/appearance restored | Delivery screenshots |
| Full-hour recording | UI recording timer reached **1:01:04**, with independently observed audio growth and background recording. The saved fixture is no longer on the phone, so its final segment/header check could not be run. | [Host observations](verification/manual-endurance-monitor.json), [audio growth](verification/manual-audio-growth.json) |
| Final release install and retention | Installed over USB; a known synthetic lecture survived a same-signed in-place reinstall and reopen. Encrypted key remained saved; temporary fixtures/test companion were then removed. | Final results to be added |

The first attempt stopped at 26:29.6 because Android silenced the microphone. LectureMind finalized six audio parts and saved the explicit interruption message. The attached runner then encountered a UiAutomation disconnect/DeadObjectException after its host drive connection failed. Metadata was checked on 18 November and only this owned verification recording was deleted. This interrupted attempt is excluded from a full-hour pass. [Target-process failure](verification/endurance-interrupted-target.log), [progress](verification/endurance-interrupted-progress.json).

The replacement run starts recording in the ordinary app, without instrumentation attached during capture. Its observer samples only service state, memory and power state; no audio is transferred. Screen wakeups are recorded rather than claiming the display stayed off every second. Charging wakefulness was temporarily disabled and has been restored to its recorded original value (15). [Restoration record](verification/device-test-settings.json).

The post-recording verifier is implemented for segment order, headers, lengths, accumulated duration, file size and boundary playback. It was not run against the independent hour fixture because that lecture is absent on reconnection. The observed one-hour timer and storage growth are partial evidence. Only specifically owned verification fixtures were removed by the verification cleanup.

## Regional-language update

The shared language policy explicitly requests Ilonggo/Hiligaynon, Bisaya/Cebuano, Tagalog/Filipino, English and mixed-language transcription without translation. Settings provides regional-language notes, reviewers and quizzes. Legacy Filipino preferences map to Filipino / Tagalog. New unit checks cover preference migration and regional transcript text/source-reference preservation. These checks do not establish native-speaker speech-recognition accuracy; live accents and dialects remain unverified.

## Gemini key and live AI

The latest supplied Gemini key was entered into the masked Settings field, AES-GCM encrypted using Android Keystore, and retained after force-stop/reopen. Checked preferences did not contain its plaintext. No key is included in this report, source, APK, exports or test logs. [Non-secret storage evidence](verification/gemini-phone-key.json).

Authenticated phone metadata requests for Gemini 3.8 Flash returned **HTTP 200** both with and without the documented revision header. The first took about 199 seconds and the next about 255 milliseconds; the latency difference was not explained. This confirms credential/model access, not generation quality or a paid quota tier. [Phone result](verification/network-diagnostics.json).

Live generation is **not yet accepted**:

- Gemini 3.8 Flash returned HTTP 503/high demand or timed out. [Result](verification/gemini-connection.json).
- The app uploaded an original 103.555-second synthetic English/Taglish photosynthesis lesson. Gemini 3.6 Flash returned a transcript that failed timing validation. The app retained the audio and rejected the transcript. [Saved job result](verification/ai-sample-result.json), [sample text](verification/ai-sample-source.txt).
- A further synthetic-response diagnostic timed out. This is a failed diagnostic, not a passing provider test. [Diagnostic log](verification/device-ai-diagnostic.log).
- No completed live notes, reviewer or quiz has been accepted against the sample. OpenAI has no configured key and remains unverified.

Only synthetic verification audio was uploaded by the assistant. No assistant-initiated AI upload of the user's existing lecture occurred. API billing and quota belong to the provider account/project; a subscription claim does not establish that this workflow can finish. [Google API billing documentation](https://aistudio.google.com/docs/billing).

The 18 November release also passed the on-phone connection test using the saved key. Gemini 3.8 still returned a temporary service error for the synthetic sample; 3.6 again returned invalid timing; Gemini 4 Hydrogen accepted model access but its audio request failed to reach the provider. Thus no live transcript/notes/reviewer/quiz quality pass is claimed. The model setting was restored to Gemini 3.8 Flash.

A proposed timing fallback was preserved separately, but Windows Application Control blocked Room’s build-time SQLite DLL during that follow-up build. It was not delivered. The source and APK retain the fully tested 14-test implementation and strict timing validation. No security policy was disabled.

## Current local APK

**LectureMind-1.0.0.apk** · **11,090,711 bytes** · Personally signed and non-debuggable

SHA-256:

    27c2dc24979e4a007816e8d429a6d620e8699180ce9ba06c64b1d9c8d50e28a0

Location: **G:/LectureMind-build/releases/LectureMind-1.0.0.apk**.

The earlier public APK has the same Android version/file name and a different SHA-256: 07c31e6a7e7a024ef5dc9a432185f39b4a465afa87a4e5867199d5aa9f512073. Its previously verified identity is retained [separately](verification/public-release-identity.json). The current APK is distributed through [the regional-language release](https://github.com/Gabbu69/LectureMind/releases/tag/v1.0.0-regional-update). The Android version remains 1.0.0; the release tag distinguishes this source and APK from the original publication.

Signing material stays outside source control under G:/LectureMind-build/private. Keep it securely for future in-place updates.

## Storage and remaining boundaries

Source: **G:/LectureMind**. Downloaded tools/final artifacts: **G:/LectureMind-build**. Caches and outputs: **G:/LectureMind/.build-fast**. Relocation verified 20,815 files / 3,058,607,915 bytes before removing verified originals. D: retains only a location note; no active C: project copy was found. A preinstalled C: Android SDK ADB executable was reused for independent observation after the drive interruption; project/build data was not moved back to C:. [Relocation evidence](verification/relocation-cleanup.json).

Remaining acceptance boundaries include live Gemini/OpenAI output quality and English/Filipino/Taglish recognition, controlled quota/network failures, actual low-storage/microphone-contention scenarios, TalkBack and broader device/API coverage. Baseline screenshots do not cover every screen at every font size. Rendering assertions and selected-page inspection do not constitute human review of every PDF page.

Follow [the build guide](docs/BUILD.md) to reproduce checks. Local fixtures, static scans, physical-device behavior and live provider acceptance remain separate evidence categories.
