# Verification — LectureMind 1.0.0

Release review: **16 November 2026** · Package: `com.lecturemind.app` · Android 8.0+ (API 26)

This is an **early-access release**. It includes a signed installable APK and recorded automated checks. Live provider quality, long recording endurance and broad device coverage remain unverified.

## What has been checked

| Check | Observed result | Evidence |
| --- | --- | --- |
| Release, debug and instrumentation APK assembly | Successful; 123 Gradle tasks | [APK build log](verification/apk-build.log) |
| JVM unit tests | Recorded successful run: 12 tests, zero failures/errors/skips | [JUnit XML](verification/unit-tests.xml), [build log](verification/unit-build.log) |
| Android lint | Zero errors, 21 warnings; release vital lint passed | [Lint report](verification/lint-results-debug.html), [build log](verification/lint-build.log) |
| Physical-device instrumentation | Six tests passed in 37.293 seconds | [Device test log](verification/device-short.log) |
| Release signature | Rechecked for publication: v2 signature, RSA 3072-bit, one signer | [Signature report](verification/signature.txt) |
| Release manifest | Rechecked: correct package/version, min API 26, target API 36, not debuggable | [Manifest report](verification/manifest.txt) |
| APK integrity and common embedded provider-key patterns | Rechecked: ZIP valid, no matching provider-key patterns found | [Release report](verification/release.json) |
| Locked Maven advisory scan | Rechecked: 199 coordinates, zero reported advisories | [Audit result](dependency-audit.json) |

The six device tests exercise Keystore credential encryption/removal, database persistence and deletion, short microphone capture with pause/resume and background continuation, sample audio playback, editing sample notes, quiz scoring, and PDF/UTF-8 export/cleanup. Study screens use a controlled local fixture. No live provider key or paid AI request was used. Passing these checks does not establish full acceptance on every phone.

The JVM suite includes structured-content validation, source references, invalid quiz answers, repeated questions, insufficient content, malformed JSON, scoring, Taglish timestamp round-trips, WAV repair, seeking and retry behavior. A synthetic two-hour fixture uses 24 five-minute WAV parts and a fake transcription function; it checks file handling and resume behavior, not two hours of microphone or live API use.

The earlier successful compilation, unit/lint and assembly checks preceded moving the project to G:. Application source inputs were checked after relocation and a Gradle configuration check passed. A publication-time full unit rerun on this 4 GB Windows laptop failed because the JVM/Gradle daemon exited under memory pressure; that attempt is not counted as a passing run. No application source code was changed for publication.

Lint's 21 warnings concern available dependency updates and optional Android/Kotlin API improvements. No rules are disabled and no lint baseline hides errors. Dependency advisory scans only identify known reported vulnerabilities; they are not a security guarantee.

## Download identity

File: **LectureMind-1.0.0.apk** · 11,090,711 bytes · personally signed, direct GitHub distribution

SHA-256:

```text
07c31e6a7e7a024ef5dc9a432185f39b4a465afa87a4e5867199d5aa9f512073
```

Signing material stays outside the repository and release assets. Preserving the same signing identity is necessary for future in-place updates.

## Still awaiting acceptance

- Live Gemini and OpenAI audio/transcript generation, including English, Filipino and Taglish accuracy, provider quota failures and output quality against the original recording.
- A 60-minute real microphone recording with the screen locked; duration, segment and memory checks.
- Force-stop/reopen recovery through the full interruption/recovery sequence.
- Permission denial, microphone contention, low storage, network interruption and provider error paths on a phone.
- A signed release-to-release update preserving saved data.
- Small-screen/landscape/large-text/light-theme/accessibility coverage and a broader device matrix.
- Human visual inspection of exported multi-page PDFs beyond the existing automated rendering assertions.

OpenAI source links refer to the start of each five-minute audio section. A Settings connection check establishes model access only. AI output must be checked against the recording, and provider requests already received may be billed even when cancelled.

## Reproduce

Follow [the build guide](docs/BUILD.md) for JDK 17, Gradle 9.3.1, SDK 36 and signing setup. Use separate compiler, test, lint and APK assembly processes on low-memory machines. Run `python tools/verify_release.py` to inspect the APK and `python tools/audit_dependencies.py` for the locked Maven advisory scan. No native package-manager advisory audit is available for this Gradle project; OSV is used for resolved locked coordinates.

Publication follow-up: a second, lower-memory unit-test retry reached Kotlin compilation but reported JVM metaspace exhaustion and made no further useful progress. Its identified Gradle client/daemon were stopped; it is not a passing run. See [retry output](verification/publication-test-retry.log). The public APK was downloaded without authentication after publication: 11,090,711 bytes, with SHA-256 exactly matching the value above. The public README and all four presentation assets were also reachable.
