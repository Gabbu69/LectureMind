# Build and package size checks

The target laptop has 4 GB RAM and limited free space on C: and D:. At the user's latest request, source, toolchain, caches, signing and deliverables now use G:. The earlier build used an approved temporary area on D:, capped at 1.5 GiB with at least 1 GiB free; those historical measurements are retained below. The D: size cap is enforced only on D:. On G: the launcher retains a free-space guard and explicitly places Java temporary files on G:.

| Measured issue | Change | Evidence |
| --- | --- | --- |
| 1 MiB in 128 small writes took 9.631 s on E: and 0.015 s on D: | Use the approved capped temporary D: area for Gradle work | `tools/io_probe.py`; source and final deliverables remain outside that temporary area |
| The full extended icon library produced large unused API/runtime/extraction files | Retain the core icon library and vendor only the 24 additional vectors actually used | 32 total icon references, 24 vendored vectors, 65,259 source bytes; drawing code is copied unchanged from the published Apache-2.0 sources |
| Unused extended-icon temporary data consumed space needed for packaging | Remove only that dependency's owned cache entries | 5 cache folders removed, 204.2 MiB reclaimed |
| The unit-test JVM failed to commit its initial G1 memory region | Use a small initial heap and Serial GC for tests; run cached tests with a smaller Gradle JVM | 12 tests passed, zero failures/errors/skips, including the two-hour fixture and resume checks |
| The main build JVM exhausted Windows commit space in a C2 compiler thread | Reduce heap/native overhead and use Serial GC with C1 compilation | Rebuild verification is recorded in `VERIFICATION.md` |
| D8 exhausted the 384 MiB compiler heap during the final external-DEX merge | Use a separate cached packaging process with a 512 MiB heap and 192 MiB metaspace cap | `tools/assemble.py`; final result recorded in `VERIFICATION.md` |

`tools/vendor_used_icons.py` verifies every selected vector's drawing code remains identical to its published source and preserves copyright/license notices in `third_party/material-icons`. Local and device checks remain the correctness gates; APK size and test results are recorded after the final build.
