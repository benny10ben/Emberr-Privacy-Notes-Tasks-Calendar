# Emberr — Beta Release Readiness

**Date:** 2026-09-03 · *Q2 revised and Q5 added 2026-09-04* · *Q5 resolved (except Q5.6) 2026-09-05* · *Q6 added 2026-09-05* · *Q4.1 resolved 2026-09-06* · *Q4.3 confirmed resolved 2026-09-07* · *Q4.3 fully resolved 2026-09-08 (keystore backed up offline)* · *Q4.2 resolved 2026-09-08* · *Application ID renamed to `com.emberr` 2026-09-09* · *Q3 test suite and CI built 2026-09-09*
**Version at time of audit:** `versionCode = 1`, `versionName = "1.0"`
**Application ID:** `com.emberr` — renamed from `com.ben.emberr` on 2026-09-09, along with a full Kotlin package rename of `com.ben.emberr.*` → `com.emberr.*`. References throughout this document reflect the new ID, including in sections describing work done before the rename.
**Distribution target:** GitHub Releases — a signed Android APK plus a Linux `.rpm` desktop package (Play Store, Windows, macOS, and other Linux packaging formats all deferred)

This document is an audit of everything standing between Emberr and a beta release. It covers six areas:

| # | Area | Verdict |
|---|---|---|
| Q1 | Android permissions | ✅ **Resolved 2026-09-03** — the 4 unused media permissions are removed |
| Q2 | Code obfuscation | ✅ **Resolved 2026-09-03** — rules rewritten and verified on a release build |
| Q3 | Automated testing | ✅ **Built 2026-09-09** — ~310 pure-logic tests across both targets, plus a GitHub Actions workflow. Room/UI/R8-smoke coverage deliberately out of scope |
| Q4 | Everything else (Android) | ✅ **Fully resolved 2026-09-08** — data-loss bug, signing, plaintext-traffic scope, download size, and all cleanups closed. None of it has been compiled/run yet — see the verification checklist |
| Q5 | Desktop packaging | ✅ **Fully resolved 2026-09-05** — all six items closed. *A 2026-09-08 attempt to add ARM Linux support via a different engine (`de.kherud:llama`) was reverted the same day — see Q5.8* |
| Q6 | Build tooling (AGP / Gradle) | 🟢 **Deferred 2026-09-05** — AGP 9 requires a KMP module-split refactor; staying on AGP 8.13.2 / Gradle 9.4.1 for this beta |

### Severity legend

- 🔴 **Critical** — can destroy user data permanently
- 🟠 **Blocker** — cannot ship without it
- 🟡 **Important** — should fix before beta
- 🟢 **Cleanup** — safe to defer
- ✅ **Resolved** — verified fixed in the working tree

---

# Q1 — Are our Android permissions acceptable? ✅ Resolved

**Status: fixed on 2026-09-03.** The four unused media permissions and all their supporting code are gone. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` was deliberately kept — see the judgement call below. The remaining 10 permissions are all justified.

Every declared permission was traced to real call sites in the codebase. A permission with no code behind it is worse than useless: it scares users at the install prompt, and on Play it invites review scrutiny for no benefit.

## Justified — keep these

| Permission | Proof it is used |
|---|---|
| `VIBRATE` | `androidMain/.../domain/util/Platform.kt:15-19` — haptic feedback on block interactions |
| `POST_NOTIFICATIONS` | `AndroidReminderReceiver.kt:29-46` (reminders), `BackupNotifier.kt:11-49` (backups), `ModelDownloadWorker.kt:55-79` (AI model downloads) |
| `SCHEDULE_EXACT_ALARM` | `AndroidReminderScheduler.kt:33-37` — `setExactAndAllowWhileIdle` for precise reminders |
| `INTERNET` | Ktor + jsoup — AI providers, model downloads, WebDAV, LAN sync, link previews |
| `RECORD_AUDIO` | `AndroidAudioRecorder.kt:36-43` (voice notes), `NativeVoiceRecognizer.kt:73-78` (speech to text) |
| `CAMERA` | `MainActivity.kt:104-110` (photo capture), `SyncComponents.android.kt:97` (CameraX QR pairing) |
| `RECEIVE_BOOT_COMPLETED` | `AndroidReminderBootReceiver.kt` — reschedules reminders after reboot |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | `ModelDownloadWorker.kt:24,42,64-79` — multi-GB model downloads survive backgrounding |

### Two things already done correctly

**`USE_EXACT_ALARM` is not declared, and that is the right call.** Android offers two exact-alarm permissions. `USE_EXACT_ALARM` is auto-granted but reserved for alarm clocks and calendar apps; a notes app requesting it gets rejected. `SCHEDULE_EXACT_ALARM` is user-grantable and correct here. Critically, `AndroidReminderScheduler.kt:39-43` already **degrades gracefully** to `setAndAllowWhileIdle` when the user denies it, instead of breaking.

**Camera is declared optional.** `<uses-feature android:name="android.hardware.camera" android:required="false"/>` means devices without a camera can still install the app. Without `required="false"`, Android would silently exclude them.

## ✅ These four were removed — nothing used them

```
READ_EXTERNAL_STORAGE
READ_MEDIA_IMAGES
READ_MEDIA_VIDEO
READ_MEDIA_VISUAL_USER_SELECTED
```

### The problem (as diagnosed)

These permissions exist so an app can browse the user's photo library *directly*. Emberr never does that. There is no `MediaStore.query` call and no media file read by path anywhere in the codebase.

Instead, the app uses the **Storage Access Framework**:

- `MainActivity.kt:89-95` declares `ActivityResultContracts.GetContent()`, launched at `:316` for images and `:335` for documents.
- SAF opens the *system's own* picker. The user chooses a file, and Android hands your app a temporary read grant for **that one file**.
- Because the OS did the browsing and the user made the choice, no permission is required. This is the whole point of SAF.

Saving images out is also permissionless: `AndroidImageDownloader.kt:26-40` uses a `MediaStore` **insert**, which has needed no permission since Android 10.

Since `minSdk = 31` (Android 12), the pre-Android-13 `READ_EXTERNAL_STORAGE` fallback path is unreachable for this flow anyway.

### Why it matters beyond tidiness

Users see a media-access prompt during onboarding that grants the app nothing at all. Removing these makes the install prompt honest and shrinks the app's attack surface.

`READ_MEDIA_VISUAL_USER_SELECTED` deserves a special note: it exists to support Android 14's partial photo access, which only works with the `PickVisualMedia` photo picker. This app never uses that picker, so declaring the permission is a contradiction — and a well-known Play review flag.

### ✅ What was done

1. Deleted the four `<uses-permission>` lines from `AndroidManifest.xml`.
2. Removed `AppPermission.Media` from `commonMain/.../domain/util/AppPermissions.kt`.
3. Removed the supporting Android plumbing in `androidMain/.../domain/util/AppPermissions.kt`: `mediaPermissionNames()`, `hasMediaAccess()`, and the `Media` branches in both `when` expressions.
4. Removed the "Photos & Media" card from `OnboardingFinishStep.kt`, along with its now-unused `Res.drawable.images` import.

Both `when` expressions in the Android file are used as expressions, so Kotlin enforces exhaustiveness: deleting the enum entry and its branches together means the compiler proves no case was missed. The desktop `actual` never switched on the enum, so both targets still satisfy the `expect`/`actual` contract.

The `images` drawable itself was kept — `DesktopMainScreen.kt:560` still uses it.

A follow-up grep for `AppPermission.Media`, `mediaPermissionNames`, `hasMediaAccess`, `READ_MEDIA`, and `READ_EXTERNAL_STORAGE` across `app/src` returns zero hits.

Nothing else changed — the pickers keep working exactly as before, because they never depended on these permissions.

### Two corrections to the original audit text

1. The step above originally read "onboarding **and settings** permission screens." Settings never had a media card; `SettingsScreen.kt:153,160` only touches `AppPermission.UnrestrictedBackground`. Onboarding was the sole UI site.
2. `READ_EXTERNAL_STORAGE` was declared with `android:maxSdkVersion="32"`, so it *was* live on API 31–32 rather than fully unreachable as stated below. Still pointless given SAF, but the fallback was not dead code.

## 🟢 Judgement call — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

**Genuinely used.** `AppPermissions.kt:157-158` builds the settings intent, launched at `:80-83`, with state read via `PowerManager.isIgnoringBatteryOptimizations` at `:150-152`. The purpose is legitimate: Android's battery saver can delay alarms, so reminders arrive late.

**Keep it for the GitHub beta.** There is no policy gate outside the Play Store. Explicitly retained on 2026-09-03 when the media permissions were removed — this was a deliberate decision, not an oversight.

**Revisit before Play submission.** Google restricts this permission to a narrow allowlist and generally expects reminder apps to rely on `SCHEDULE_EXACT_ALARM` alone. It would need written justification, and might be rejected.

## Direct answer: would Play accept this today?

Nothing here is an automatic rejection. Of the two items that would have drawn scrutiny:

1. ✅ The four unused media permissions — especially `READ_MEDIA_VISUAL_USER_SELECTED` without the photo picker. **Removed, so this is resolved.**
2. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, as above — **still outstanding by choice**, and the only remaining permissions concern for a future Play submission.

Separately, Play requires a **Data safety declaration**. Emberr will need to disclose that note content, audio recordings, and camera images are collected, and that note text is transmitted to third-party AI providers (Anthropic, Google, OpenAI) when the user enables those features. That is paperwork rather than code, and is out of scope for this beta.

---

# Q2 — Obfuscation ✅ Resolved

**Resolved 2026-09-03.** `app/proguard-rules.pro` was rewritten and verified against a real release build on a device. Two of this audit's original findings turned out to be wrong and are corrected below.

## It is already switched on

`app/build.gradle.kts:210-219`:

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

`isMinifyEnabled = true` runs **R8**, which does three things: deletes unused code, shortens names (`NoteRepositoryImpl` becomes `a`), and optimizes. `isShrinkResources = true` drops unused resources.

So the question was never "should we obfuscate" — it was "are our rules correct?"

## The mental model

R8 renames things. Anything whose **name still matters while the app is running** breaks when renamed.

Keep rules are **additive across three sources**, and none of them can cancel another:

1. AGP's `proguard-android-optimize.txt` default file
2. This project's `proguard-rules.pro`
3. **Consumer rules** that libraries ship inside their own AAR/JAR and that AGP injects automatically

A hand-written rules file therefore *adds to* the defaults rather than replacing them. Point 3 is why several rules this audit originally recommended turned out to be redundant.

## Correction 1 — Risk 1 was wrong: serialization is not at risk

The original audit rated `@Serializable` classes 🔴 and called it "the worst item in the document." That was incorrect, and the reasoning behind it does not hold.

**kotlinx.serialization is a compiler plugin, not a reflection library.** At compile time it generates a `$serializer` class whose descriptor is built by passing each field name to `PluginGeneratedSerialDescriptor.addElement` as a **string constant** baked into the bytecode. R8 renames symbols; it never rewrites string literals. So even a fully renamed field keeps its original JSON key.

Verified two independent ways:

1. `kotlinx-serialization-core-jvm-1.6.3.jar` ships its own R8 rules under `META-INF/com.android.tools/`. The serializer machinery is **already kept by the library** — consumer rules, source 3 above.
2. Disassembling `SyncEnvelope$serializer.class` shows the field names present as `ldc // String entityId` constants, not as references to the renameable field.

**Consequence:** the silent-data-loss scenario built on `ignoreUnknownKeys = true` — a renamed field being quietly dropped to `null` — cannot be caused by R8. The `ignoreUnknownKeys` observation remains accurate as a general fragility, and it is still worth knowing that a genuine schema mismatch fails silently rather than throwing. But obfuscation is not a way to trigger it.

One accurate part of the original Risk 1 survives: the `NoteBlock` hierarchy uses **explicit** `@SerialName` discriminators (`NoteModels.kt:77-540` declares `"text"`, `"heading"`, `"quote"`, `"checkbox"`, `"database"`, `"voice"`, …). Those are string literals in source, so the on-disk note format is stable regardless of obfuscation.

## Correction 2 — this file is Android-only

`proguard-rules.pro` reaches R8 through `proguardFiles` inside the `android { }` block. It has **no effect whatsoever on the desktop build**.

Desktop is a separate pipeline: `compose.desktop.application.buildTypes.release.proguard.configurationFiles`, backed by ProGuard rather than R8. Its defaults are `isEnabled = false` on the default build type, `isEnabled = true` on `release`, `obfuscate = false` always, `optimize = true`.

**Desktop obfuscation is deliberately left off.** It buys nothing — the app image is an unpacked directory on the user's disk either way — and it risks breaking Skiko's native loading and llamatik's resource-path lookups, both of which resolve by name at runtime. Q5 covers the desktop concerns that are real.

## 🔴 Risk 2 — the llamatik JNI bridge (the genuine critical item)

`domain/ai/LocalAiEngine.kt` imports `com.llamatik.library.platform.LlamaBridge` and `GenStream`. This is a **JNI** bridge — Kotlin calling compiled C++.

JNI matches Kotlin methods to native functions by **exact class and method name**, resolved at runtime. Rename either side and the lookup fails. No keep rule existed for this before the rewrite.

The failure mode is nasty: local AI generation crashes **on the release build only**. The debug build is unobfuscated, so this never appears during development.

`includedescriptorclasses` was added to the native-methods rule so protection extends to the parameter and return types in native signatures, not just the method names — `GenStream` is passed *into* native code as a callback, so its shape has to survive too.

## 🟢 Risk 3 — SQLCipher

`-keep class net.sqlcipher.** { *; }` was already present and correct. Also a native library, and it holds database encryption. Left alone.

## 🟡 Risk 4 — enums stored by name

These enums are written to preferences as strings and read back with `valueOf`:

`FontStylePreference`, `FontSizePreference`, `SubNoteOpenMode`, `CalendarViewMode`

Call sites: `MainActivity.kt:194,198`, `SettingsScreen.kt:324,334,605,665`, `CalendarViewModel.kt:39`, `NoteScreen.kt:211`, `DesktopMain.kt:153-261`

Every call site is wrapped in `runCatching { }.getOrDefault(...)`, so a rename would not crash — the preference would silently reset to default. Mild, but cheap to prevent, so it is covered.

## ✅ What was done

`app/proguard-rules.pro` in full (no comments, per project convention):

```proguard
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.llamatik.** { *; }

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }

-keepclassmembers enum com.emberr.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

-keepclassmembers class com.emberr.**$serializer {
    *;
}

-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
```

**Rules this audit originally recommended that were deliberately left out:**

| Dropped rule | Why |
|---|---|
| `-keep @kotlinx.serialization.Serializable class com.emberr.** { *; }` and the `-if` companion block | Redundant — the library's own consumer rules already cover it, and serial names are string constants (Correction 1) |
| `-keepattributes RuntimeVisibleAnnotations,AnnotationDefault` | Same reason; nothing in this codebase reads annotations reflectively at runtime |
| `-keep class com.emberr.domain.model.** { *; }` | Tested and proven not load-bearing |
| `-keep class com.emberr.presentation.**.*ViewModel` | The `**.*` pattern does not reliably match nested packages, and Koin resolves ViewModels through constructor references, not reflection |
| `-keepnames class androidx.navigation.compose.**` | Navigation ships its own consumer rules |

The one serialization rule that was kept — `-keepclassmembers class com.emberr.**$serializer { *; }` — is retained as belt-and-braces at near-zero size cost.

## Needs no action

- **Room** ships its own consumer rules.
- **ML Kit** likewise.
- **SQLDelight** generates ordinary Kotlin, so normal analysis applies.
- **No Java reflection anywhere** in the codebase, which removes an entire category of obfuscation risk.

## ✅ Verified on device against a release build

- `app/build/outputs/mapping/release/` exists — confirms R8 actually ran
- Local AI generation completed — proves the llamatik JNI keep rule
- A note using every block type round-tripped through force-close and reopen with nothing lost
- Font style and sub-note open mode persisted across an app restart — proves the enum keep rule

LAN sync pairing, backup export/re-import, and the 8 widgets have **not** yet been exercised on a release build. Those remain in the verification checklist.

---

# Q3 — Automated tests ✅ Built 2026-09-09

**Status: the test suite and CI now exist.** Roughly 4,600 lines of test code across 27 files, ~310 test methods, every one of them running on **both** the desktop and Android targets. No emulator, no device, no Robolectric.

## Two stale premises in the original plan, now corrected

1. **Wrong module.** This section said tests go in `app/src/commonTest/`. Since `fd79ba0` all code lives in `:shared`; `:app` is a plain `com.android.application` wrapper with zero Kotlin files. Tests are in `shared/src/commonTest/`.
2. **Already done.** "Delete the two existing test files" and "un-ignore `gradle-wrapper.jar`" were both already resolved — the example files are gone and the jar is tracked. The `.gitignore` `*.so` / `*.so.sha256` warning still stands, but is harmless today (llamatik natives resolve from Maven Central; the local `so16/` directory is untracked scratch and unreferenced by the build).

## What an automated test is, and why bother

A test is code that runs your other code and checks the answer. Run automatically, it catches a regression the moment you introduce it, instead of a user finding it weeks later.

For Emberr the value is concentrated in logic where **failure is invisible**:

- A reminder computed for the wrong day — the user just never gets notified.
- A note block dropped during serialization — the text is simply gone.
- A sync payload that will not decrypt — data present, unreadable.

None of these can be spotted by opening the app and looking around.

## Where the tests live

| Source set | Holds |
|---|---|
| `shared/src/commonTest/` | Every pure-logic test, plus the three abstract crypto contracts and the golden fixtures |
| `shared/src/desktopTest/` | Three thin subclasses binding the desktop crypto implementations to the contracts |
| `shared/src/androidHostTest/` | Three thin subclasses binding the Android crypto implementations to the same contracts |

`commonTest` is included in **both** target test compilations (the AGP KMP host test uses sourceSet tree `test`), so every common test runs twice — once per toolchain.

## Build changes

`gradle/libs.versions.toml` — added `kotlin-test` and `kotlinx-coroutines-test`. `kotlinx-coroutines-test` deliberately reuses the existing `kotlinxCoroutines` version ref so the two can never drift apart.

`shared/build.gradle.kts`:
- a `commonTest` source set with those two dependencies;
- `withHostTest { isReturnDefaultValues = true }` inside `androidLibrary`. The AGP KMP library plugin creates **no** test compilation unless asked, and names it `androidHostTest` (not the old `androidUnitTest`). `isReturnDefaultValues` is required because `SelfHostSyncLog` and friends call `android.util.Log`, which throws "not mocked" in a host test;
- a `tasks.withType<Test>` block pinning `user.timezone=UTC`, `user.language=en` and `user.country=US`. The test JVM heap is deliberately left at Gradle's 512 MB default: the largest allocations in the suite are the 4 MiB chunked-stream round-trips, and a standalone probe of that worst case (encrypt 4 MiB + 1, split every frame, rebuild the stream) peaks at ~146 MB and still completes under `-Xmx64m`, because the buffers are short-lived. Raising the ceiling would only make the GC lazier and the resident footprint larger.

The locale/timezone pinning is not cosmetic. `FormulaEngine.kt:36` calls `"%.2f".format(result)` with **no `Locale`**, and `NoteIndexer` uses `SimpleDateFormat` with `Locale.getDefault()`. Unpinned, those tests pass on one machine and fail on another. **The `FormulaEngine` bug itself has since been fixed** (see the production changes below), so the pinning now only guards against future locale drift and against `NoteIndexer`'s `SimpleDateFormat`.

### Three production changes made alongside the suite

Two were bugs the tests exposed, one is a seam:

1. **`FormulaEngine` no longer depends on the machine's locale.** `"%.2f".format(result)` was replaced with a small arithmetic formatter. `Locale.ROOT` would have been the one-line fix, but `java.util.Locale` is a JVM-only import and `CLAUDE.md` forbids those in `commonMain`; the arithmetic version needs nothing outside `kotlin.math` and cannot regress. Rounding, `Infinity`, `-Infinity` and `NaN` output are all unchanged.
2. **`LocalNetworkHostValidator` no longer accepts malformed IPv6.** `expandIpv6Groups` used `.filter { it.isNotEmpty() }`, which silently swallowed empty groups — so `":::1"` parsed as the loopback address and would have been treated as a local host, permitting plain `http://` to it. The new `splitIntoGroups` rejects any empty group instead. `":::1"`, `"::1:"`, `":1:2:3:4:5:6:7:8"`, `"fe80:::1"` and `"fe80:"` are now all refused; every previously-accepted address still passes.
3. **Time is now injectable, via default parameters, so no call site changed.**
   - `RecurrenceEngine.retargetTimestampToDate(...)` takes a trailing `timeZone: TimeZone = TimeZone.currentSystemDefault()`. Kotlin evaluates default arguments per call, so behaviour is identical.
   - `HeuristicTaskExtractor(clock: Clock = Clock.System, currentTimeZone: () -> TimeZone = { TimeZone.currentSystemDefault() })`. The timezone is a **lambda, not a value**, deliberately: the extractor is a Koin `single`, so a captured `TimeZone` would be fixed at app start and go stale if the device zone changed. A lambda is re-invoked on every `extractTasks` call.

   The payoff is that natural-language date parsing is now tested against a **fixed clock** (Monday 2026-01-05 08:00 UTC) with absolute expected timestamps, instead of relative offsets — including the "time already passed today, roll to tomorrow" rule — and `retargetTimestampToDate` is tested across a real daylight-saving boundary in `America/New_York`.

## What is covered

| Area | File | Notable ground covered |
|---|---|---|
| Recurrence | `RecurrenceEngineTest` | daily/weekly/monthly/yearly × interval × until; **Jan 31 → Feb 28/29 clamping**; leap years; ISO week-start with a Sunday anchor; the 3,660-day forward-scan cap; timestamp retargeting preserving wall-clock time **across a daylight-saving boundary** |
| Block schema | `NoteBlockSerializationTest` | all 18 block types round-trip; **every `@SerialName` asserted literally** (they *are* the on-disk schema); all 8 `CellData` discriminators; forward compatibility with unknown fields; and a cross-check that the repository's `Json` settings and the merge helper's differing settings stay lossless |
| Block copying | `NoteBlockDeepCopyTest` | `DatabaseBlock` column/row/view id remapping, cell-key remapping, orphaned cells kept, stale sorts/filters dropped, group *names* preserved |
| Block helpers | `NoteBlockHelpersTest` | pins the asymmetry that `CodeBlock` carries an alignment but never inline spans |
| Cell rendering | `CellDataDisplayTextTest` | all 9 branches of `displayText()` |
| Export | `ExportEngineTest` | plain-text and markdown for every block type, tab vs two-space indentation, `<details>` toggles, database and table markdown, italic→bold→strike nesting |
| Formulas | `FormulaEngineTest` | `prop()` substitution, case-insensitive column match, missing column → 0, precedence, unary signs, `Error` on unparseable input, `Infinity` on divide-by-zero |
| Voice reminders | `HeuristicTaskExtractorTest` | filler and greeting stripping, multi-task splitting, spoken intervals, 12/24-hour clocks, noon/midnight, "quarter past three", weekday resolution, and the roll-to-tomorrow rule. Run against a **fixed clock** so every expected timestamp is absolute |
| Network safety | `LocalNetworkHostValidatorTest` | private IPv4 ranges incl. the 172.16–31 boundary, IPv6 loopback/link-local/unique-local, and malformed input such as `":::1"` that used to slip through |
| WebDAV | `WebDavServerUrlValidatorTest`, `WebDavSyncPathsTest` | plain `http://` allowed on a private address and **refused on a public one** (the Q4.2 regression test); remote path layout |
| Sync merge | `SelfHostNoteMergeHelperTest`, `LanNoteContentMergeTest` | last-write-wins with the deliberate local-wins tie-break, tombstone `>=`, foreign-note blocks dropped, undecodable JSON falling back to a decodable deleted block, `DatabaseBlock` field-level column/row/cell merging |
| Sync wire format | `NotePayloadRoundTripTest` | all 18 block types survive compile → JSON → parse; `filePath` never crosses; tombstone dedupe by newest; a block claimed both live and deleted is refused |
| Crypto | `SyncEncryptionManagerContract` + 2 platform subclasses | payload/bytes/stream round-trips at 0 B, 1 B, 1 KB, and either side of the 4 MiB chunk boundary; wrong key; tamper; **chunk reordering and truncation detection via the per-chunk AAD**; wire-layout byte counts; golden-fixture interop |
| Signing | `SyncHmacSignerContract` + 2 subclasses | a hard-coded expected signature, determinism, sensitivity to path/timestamp/secret |
| Key derivation | `KeyDerivationManagerContract` + 2 subclasses | same passphrase + salt always yields the same key; 32-byte output; length guards; generated passphrases avoid easily-misread characters |
| Calendar storage | `CalendarTaskRecurrenceMappingTest` | `toRecurrenceRule()` ↔ `toEntityColumns()` round-trip, bad weekday tokens skipped |
| Home sorting | `HomeItemsTest` | all 5 sort types × both directions, `MANUAL`'s `sortOrder == 0` sentinel, folders-before-notes, drag reordering |
| AI budgeting | `PromptBudgetTest` | token estimation, output reservation clamped to half the window, chunks skipped vs history breaking |
| Sync schema | `LanSyncSchemaTest` | supported-version boundaries |

## The crypto contracts — why they matter most

`AesGcmEncryptionManager`, `HmacSha256Signer` and `Pbkdf2KeyDerivationManager` each exist **twice**, once in `androidMain` and once in `desktopMain`. A `diff` of the two `AesGcmEncryptionManager` files today shows differences in **comments only** — the logic is byte-identical. That is precisely the risk: nothing stops the two from drifting, and if they do, **LAN sync and WebDAV sync between an Android device and the desktop app break with a decryption error** — the "data present, unreadable" failure this document opens with.

Two mechanisms close that gap:

1. **Contract tests.** The assertions are written once as an `abstract class` in `commonTest`; each platform contributes a three-line subclass supplying its own implementation. Both copies are held to exactly the same behaviour.
2. **Golden fixtures.** `CryptoGoldenFixtures` holds a hard-coded key, ciphertext and expected HMAC. Because AES-GCM uses a random IV, two ciphertexts of the same plaintext are never byte-comparable — so the test decrypts a *shared* fixture instead, which is what actually proves cross-platform interoperability. The fixtures were generated with a standalone JDK program replicating the documented algorithm, independent of the project's own code, so they are a genuine oracle rather than a snapshot of current behaviour.

## GitHub Actions

**New file: `.github/workflows/ci.yml`** — one `ubuntu-latest` job on push to `main`, on pull requests targeting `main`, and on manual `workflow_dispatch`, with `concurrency` cancelling superseded runs. The manual trigger exists so the workflow can be exercised from the Actions tab without merging anything.

`ubuntu-latest` is correct even though development and the `.rpm` build happen on Fedora: GitHub-hosted runners only come in Ubuntu, Windows and macOS flavours, and **this job never builds the RPM**. It runs tests and an Android release build, neither of which cares about the host distro. RPM packaging stays a local Fedora step — `jpackage --type rpm` needs a working `rpmbuild` (the Q5.2 digest failure was a missing `rpm-build` package), and the jlink runtime it bundles is built from the host's glibc, so building it on the distro it is tested on removes a whole class of packaging surprise. If it ever needs to move into CI, the answer is a `container: fedora:latest` job, not a different runner.

| Step | Purpose |
|---|---|
| `actions/setup-java@v4` (temurin 21) | matches `libs.versions.javaToolchain` |
| `android-actions/setup-android@v3` | **required** — `compileSdk = 37` is newer than the runner image ships |
| `gradle/actions/setup-gradle@v4` | caches `~/.gradle`, which also covers auto-provisioned toolchains |
| `./gradlew :shared:allTests` | runs **every** test compilation on **both** targets |
| `./gradlew :shared:compileKotlinDesktop` | stops the desktop target silently rotting |
| `./gradlew :app:assembleRelease` | **validates R8.** Missing-keep-rule and shrinker errors surface here on a clean machine. Produces an unsigned APK, which is correct |
| `actions/upload-artifact@v4`, `if: failure()` | uploads the test reports so a red run is diagnosable without reproducing locally |

`allTests` is Kotlin's own aggregate task ("Runs the tests for all targets and create aggregated report"), and AGP registers the Android host test into it via `KotlinTestsRegistry`. It is used in preference to a hand-written task name because it cannot go stale across AGP versions.

Three configuration details worth remembering:

- **Memory: do not override it.** Run 1 of the workflow failed because a job-level `GRADLE_OPTS` shrank the Kotlin daemon to 1500m, and both `compileAndroidMain` and `compileKotlinDesktop` died with `OOMErrorException`. Two lessons: a GitHub-hosted `ubuntu-24.04` runner has **16 GB**, not the 7 GB the override assumed, so `gradle.properties`' 4 GB + 2 GB fits comfortably; and `-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=768m` is silently truncated at the first space unless quoted, so only `-Xmx3g` ever applied. The override is gone — CI now uses the same tuned memory settings as a local build.
- **Daemon JVM vendor.** `gradle/gradle-daemon-jvm.properties` pins `toolchainVendor=JETBRAINS`, which `setup-java` with temurin does not satisfy — Gradle downloads a JetBrains Runtime 21 from foojay. `setup-gradle`'s cache of `~/.gradle` covers `~/.gradle/jdks`, so this happens once. Expect a slow first run.
- **No signing, ever.** No keystore, no GitHub secrets. Releases are signed locally, exactly as Q4.3 describes.

Deliberately left out of CI: `./gradlew lint` (would surface a backlog needing triage or a baseline first), the instrumented R8 smoke test (needs a device), and `.rpm` packaging (needs `rpm-build` and `jpackage`; belongs in a separate release workflow).

## Still not covered — read this before trusting the suite

The suite is **pure logic only**. It is a real safety net for the failure classes above, and it is silent about everything below.

- **No Room, DAO or repository tests.** `NoteRepositoryImpl` (1,291 lines) and its two in-memory caches are untested — including the `Daos.kt:17-22` requirement that `insertOrUpdateMetadata` be `@Upsert` and not `@Insert(REPLACE)`, where the wrong annotation cascades a delete that wipes `note_blocks`. Desktop/JVM with `BundledSQLiteDriver` and the existing `getDatabaseBuilder(dbFilePath)` overload is the natural home for these.
- **No UI tests.** `BaseEditorViewModel` (2,315 lines) is untested. Its most test-worthy function, `shiftSpansForEdit` (`:956`), is `private` and would need promoting to `internal` first.
- **No R8 smoke test.** CI's `assembleRelease` proves R8 *runs*, not that the minified app *works*. The keep rules are still only proven by the on-device checklist below.
- **Dispatchers are still not injected.** `NoteRepositoryImpl` hardcodes `withContext(Dispatchers.IO)` and the ViewModels hardcode `viewModelScope.launch(Dispatchers.IO)`, so a `TestDispatcher` cannot control them. Time *is* now injectable — see below.

## Verification — Gradle is run by hand, per project convention

1. `./gradlew :shared:tasks --group verification` — see the real task names.
2. `./gradlew :shared:allTests` — should finish in well under a minute. The slowest parts are the 4 MiB stream round-trips and PBKDF2 at 600,000 iterations.
3. Open `shared/build/reports/tests/` and confirm the test **count**, not just a green exit code. Expect the common tests to appear twice, once per target.
4. **Deliberately break something and watch a test go red** — change `@SerialName("checkbox")` to `"check_box"`, or flip `>=` to `>` in the selfhost `NoteMergeHelper` tombstone comparison. A suite that cannot fail is worse than none.
5. `./gradlew :app:assembleRelease` locally before expecting it to pass in CI.
6. Push a branch and confirm the Actions run is green — this is the Distribution checklist item below.

---


# Q4 — What else is missing before beta

## ✅ Problem 1: Backup restores an app the user cannot open — Resolved 2026-09-06

### How Emberr protects notes today

Notes live in a database encrypted with SQLCipher. Opening it requires a passphrase. `core/security/EncryptionManager.kt:19-33` generates that passphrase **once**, as 32 random bytes from `SecureRandom`, and stores it in an encrypted preferences file named `emberr_secure_prefs`.

Think of a **locked safe** (the database) and exactly **one key** (the passphrase). No key, no notes. There is no master password, no recovery code, and no reset — that is the point of encryption.

### What Android auto-backup does

The manifest sets `android:allowBackup="true"`, so Android periodically copies app data to the user's Google Drive and copies it back when they reinstall or set up a new phone. This happens automatically, without the user asking.

### The bug

`data_extraction_rules.xml` excludes four preference files from backup, including `emberr_secure_prefs` — the file holding the key. That instinct is reasonable; you do not want an encryption key sitting in Google Drive.

**But nothing excludes the database.** So Android backs up **the safe and not the key.**

What a real user experiences:

| Step | What happens |
|---|---|
| 1 | Writes 200 notes over several months |
| 2 | Gets a new phone, restores from Google backup |
| 3 | Android restores the encrypted database ✅ |
| 4 | Android does **not** restore the passphrase, because you excluded it ❌ |
| 5 | App starts, finds no passphrase, and **generates a brand-new random one** |
| 6 | Tries to open the restored database with the wrong key → fails |

Their notes are physically present on the device and **permanently unreadable**. Not corrupted, not deleted — locked, with the only key destroyed. Silent, unrecoverable, and it hits hardest the users who trusted the app with the most data.

### The solution

The current half-and-half state is the one configuration that cannot work. Either back up **both** pieces or **neither**.

**Recommended: neither.** Set `android:allowBackup="false"`.

Reasoning:
- Emberr already ships its own backup feature via `BackupWorker` — a path you control, can test, and can explain to users.
- Placing a database encryption key in Google Drive would undermine the privacy promise of an offline-first encrypted notes app.
- It is one attribute change, with no way to get it subtly wrong.

The alternative — extending the exclusion list to cover the database and note-content files — also works, but every future storage location becomes a new chance to reintroduce the bug.

### Also: dead configuration

The manifest sets `android:fullBackupContent="@xml/backup_rules"`. That attribute applies only to Android 10 and below. `minSdk = 31` means Android 12 or newer, so **it can never take effect**. The file is still the unmodified Android Studio template containing only sample comments.

Remove the attribute and delete `app/src/androidMain/res/xml/backup_rules.xml`.

### ✅ What was done

`android:allowBackup` set to `"false"`, the now-dead `android:fullBackupContent="@xml/backup_rules"` attribute removed, and `backup_rules.xml` deleted. Android's OS-level Auto Backup is now fully disabled for Emberr — nothing about the app is ever silently copied to or restored from a user's Google account.

`android:dataExtractionRules="@xml/data_extraction_rules"` was deliberately left in place. It becomes inert once `allowBackup="false"` (the OS ignores it entirely, since there is no backup to extract rules for), but the file has real content worth keeping around in case backup is ever deliberately re-enabled later — removing it wasn't part of this fix and isn't needed for it to work.

**Not to be confused with Emberr's own in-app "Automatic Backups" feature** (`BackupWorker`/`BackupScheduler`/`BackupSnapshotExporter`, `domain/backup/automatic`) — that is a separate, opt-in, user-controlled mechanism that writes `.emberr` zips to a folder the user picks, and was already unaffected by this bug since it never relied on the OS backup transport. This fix is entirely about the manifest-level `allowBackup` flag governing Android's *own* silent Google-account backup, which required no code from Emberr at all to be dangerous.

## ✅ Problem 2: Plaintext network traffic is unrestricted — Resolved 2026-09-08

### Background

`https://` is encrypted; `http://` is not. Over plain `http://`, anyone sharing the network — a café, an airport, a hotel — can read the traffic in transit.

The manifest sets `android:usesCleartextTraffic="true"`, which tells Android: *let this app send unencrypted traffic to any address on the internet.*

### Why it was needed

Legitimately, for LAN sync. `domain/sync/SyncClient.kt:80-84` builds an address like `http://192.168.1.5:8080` to reach the desktop app on the same home network. You cannot obtain a TLS certificate for a private home IP, so `https` is impractical here.

### The problem

The declared permission is far broader than the actual need:

- **Needed:** plaintext to my own home network.
- **Declared:** plaintext to anywhere on Earth.

That gap matters as a blast-radius issue rather than a "notes leak in plaintext" issue: the sync payload itself (`SyncEnvelope.metadataJson`/`contentJson`, and all media) is already AES-GCM encrypted at the application layer by `SyncEncryptionManager` before it ever reaches the HTTP layer, using a key exchanged during QR pairing — confirmed by reading `SyncRepositoryImpl.kt` and `SyncClient.kt` directly. So a public-Wi-Fi eavesdropper sniffing this traffic today would see ciphertext, not readable notes. What was genuinely missing was a code-level guardrail: with the blanket manifest flag and no validation in `SyncClient`, nothing stopped that same (encrypted) traffic — plus the metadata around it, like target IP, timestamps, and entity IDs, which are *not* inside the encrypted blob — from being sent to a non-LAN address if `SyncClient` were ever pointed at the wrong IP. Cloud AI traffic (to Anthropic/OpenAI/Google) is unaffected either way since it already goes over `https://` exclusively.

### Where the fix does *not* work

The obvious fix is a `network_security_config.xml` denying cleartext except on private networks. **Android cannot express that.** Its `<domain>` entries accept only exact hostnames or IP literals — no CIDR ranges, no wildcards. Because the desktop's LAN IP is whatever DHCP assigned, there is no way to whitelist "private networks." Setting the base config to `false` and listing `localhost` would simply break LAN sync.

### The solution: enforce it in code

You already wrote the right check. `WebDavServerUrlValidator.isLocalNetworkHost()` (`:7-22`) correctly recognises the private ranges:

- `localhost` and `::1`
- `127.x.x.x` (loopback)
- `10.x.x.x`
- `192.168.x.x`
- `172.16.x.x` through `172.31.x.x`

And `validate()` (`:24-41`) rejects any `http://` URL whose host fails that test.

**`SyncClient` performed no such validation** — `serverUrl` built `http://$ip:$port` from stored settings and used it directly, with no check at all.

### ✅ What was done

1. `isLocalNetworkHost` was promoted out of `WebDavServerUrlValidator` into a new shared `LocalNetworkHostValidator` (`domain/util/LocalNetworkHostValidator.kt`). `WebDavServerUrlValidator.isLocalNetworkHost()` now just delegates to it, so both sync paths share one implementation instead of two copies that could drift apart.
2. `SyncClient.serverUrl` (`domain/sync/SyncClient.kt`) now validates the stored IP through `LocalNetworkHostValidator` before building the URL, and throws a new `LanSyncConfigurationException` if it isn't a private-network address. Because every network call in `SyncClient` reads `serverUrl` through this one property getter, the guard applies everywhere — push, fetch, unpair, and all the media routes — without needing to be repeated at each call site.
3. `usesCleartextTraffic="true"` was left in the manifest, since the OS-level config still cannot express "private networks only" (see above).
4. The `WebDavServerUrlValidator`/`LocalNetworkHostValidator` tests from Q3 remain a to-do — they still need writing so this guarantee has regression coverage.

The exception surfaces cleanly through existing error handling: `SyncViewModel`'s manual and silent sync paths already wrap `pushChanges`/`fetchChanges` in a generic `catch (e: Exception)` and show `e.message`, and the media methods in `SyncClient` already catch broadly and log/return a failure result. No new error-handling plumbing was needed.

**Not yet done:** the Q3 test coverage for `LocalNetworkHostValidator`/`WebDavServerUrlValidator`, and a manual check that a public `http://` URL is actually refused by `SyncClient` at runtime (see the verification checklist).

Net effect: plaintext HTTP is now impossible for `SyncClient` to point anywhere except the user's own local network — enforced by code, on top of the payload encryption that was already there.

## ✅ Problem 3: Release signing — fully resolved 2026-09-08 (existing workflow, not a code gap)

### How Android signing works

Every APK must be cryptographically signed. The signature proves that update #2 came from the same author as update #1 — it is what prevents someone from publishing a malicious "Emberr update" that Android would accept as yours.

### The original finding, and why it turned out not to apply

`app/build.gradle.kts` contains **no `signingConfigs` block at all**, which is true — so `./gradlew assembleRelease` on its own produces an **unsigned** APK. The original audit treated that as a blocker, on the assumption that `assembleRelease` was the whole release pipeline.

**It isn't, for this project.** The keystore already exists (kept in the home folder, deliberately outside the repo) and every release is produced through Android Studio's **Build → Generate Signed Bundle / APK** GUI wizard. That wizard builds the `release` variant and signs it itself — it does the same job `signingConfigs` would automate, just as a manual click-through step instead of a Gradle-driven one. Any "remember these values" option in the wizard saves at the IDE level, never into `build.gradle.kts` or anything git-tracked. The end result each time is a validly signed, installable APK.

**Consequence:** there is no missing capability here. A `signingConfigs` block would only matter if the release process needed to run headlessly — e.g. from a GitHub Actions job with no human to click through a wizard. Q3 already establishes that CI never signs anything (signing stays local, on purpose, so the key and passwords never touch GitHub secrets), so that scenario doesn't apply either. Adding `signingConfigs` remains a valid future option if the workflow ever needs to be scripted, but it is optional tooling, not a fix for a real gap.

### ✅ The remaining piece — resolved 2026-09-08: keystore backed up offline

This risk was true regardless of GUI wizard vs. Gradle vs. command-line `apksigner` — it lived in the key file itself, not in how it gets invoked.

**If the keystore file were lost or its password forgotten, Emberr could never be updated again.** Not inconvenient — impossible. There is no recovery process, no support channel, no reset. Every existing user would have had to **uninstall** Emberr, destroying their local notes, and install a newly-signed version as an unrelated app.

Play Store developers have a safety net called Play App Signing, where Google retains a backup key. Distributing on GitHub means there is no such safety net — so this had to be handled manually.

**Confirmed 2026-09-08: the `.jks` file and its passwords are now safely stored offline**, outside the repo. No further action needed on this item.

## ✅ Problem 4: The download is much larger than it needs to be — `abiFilters` done 2026-09-08, `noCompress` decided 2026-09-08: leave as-is

Emberr can run AI models on-device. The **model weights are downloaded at runtime**, not bundled — `ModelDownloadManager.kt:81-83` fetches them from huggingface.co on demand. That part is already efficient.

But the **engine** that runs those models is bundled: llama.cpp, compiled to native binaries inside the `com.llamatik:library:1.7.0` AAR.

Compiled binaries only run on the exact processor architecture they were built for, so the library ships one copy per architecture — and the APK currently carries all four:

| Architecture | Approx. size | Who actually needs it |
|---|---|---|
| `arm64-v8a` | ~34 MB | ✅ Essentially every modern phone |
| `armeabi-v7a` | ~31 MB | ✅ Older and budget 32-bit phones |
| `x86` | ~36 MB | ❌ Emulators only |
| `x86_64` | ~35 MB | ❌ Emulators only |

**~131 MB uncompressed in total**, of which roughly half serves emulators. `libllama_jni.so` alone is ~24 MB per architecture. No production phone uses an Intel processor, so every user downloads ~71 MB that can never execute on their device.

### ✅ What was done — `abiFilters`, 2026-09-08

Restricted architectures on the **release** build only, so emulator development keeps working:

```kotlin
release {
    ndk {
        abiFilters += listOf("arm64-v8a", "armeabi-v7a")
    }
}
```

Roughly halves the download with no real-world loss. Dropping `armeabi-v7a` too would save another ~31 MB, but excludes older 32-bit devices; `minSdk 31` makes those uncommon, so it is a reasonable follow-up decision rather than an obvious win.

**Not yet verified:** this is a config-only change — confirm with `apkanalyzer apk file-size` before/after (see the verification checklist) once a release build is produced, since Gradle is run manually.

### ✅ Decided 2026-09-08: keep `noCompress` including `"so"` — not worth changing

`androidResources.noCompress` at `build.gradle.kts:238` includes `"so"`, which stores these binaries **uncompressed** in the APK. The alternative (removing `"so"`, letting native libraries compress like everything else) was weighed and rejected:

- **Why it's tempting:** compressed `.so` files would shrink the GitHub-distributed APK by roughly 20–30 MB (DEFLATE typically saves ~30–40% on compiled binaries).
- **Why it's not worth it:** compressed native libraries can't be executed directly via `mmap` from the APK (Android's fast path since API 23, and this app's `minSdk 31` always qualifies) — Android would instead extract them to disk at install time, meaning **two on-disk copies** (the compressed one inside the installed APK, plus a decompressed one Android writes out), a slower install, and an unverified interaction with llamatik's own native-library loader (the same category of packaging risk that already caused real bugs in Q5.1/Q5.7).
- **Why the trade doesn't pay off here specifically:** the ~20–30 MB saved is small next to the multi-gigabyte model weights this app already downloads separately at runtime, and it's a one-time cost per install rather than a recurring one (this isn't an auto-updating Play Store app).

**Decision: leave `noCompress` as-is.** Revisit only if GitHub download size becomes an actual reported problem for users on limited bandwidth.

### ✅ Verified non-issue: 16 KB page size

Android 15 introduced 16 KB memory pages, and native libraries not aligned for them fail to load. The arm64 libraries were inspected directly and all report `LOAD align = 0x4000` (16 KB). **Already compliant — no action required.**

## ✅ Problem 5: Smaller cleanups — all resolved or clarified 2026-09-08

### ✅ Resolved 2026-09-08: Two permission systems consolidated onto `AppPermissions`

`domain/util/AppPermissions.kt` was the full coordinator; `domain/util/PermissionHelper.kt` was an older microphone-only helper used in exactly one place, `Application.kt`'s mic-permission trigger for voice tasks.

**What was done:** `Application.kt` now uses `rememberAppPermissionCoordinator()` and `AppPermission.Microphone` instead. Since `AppPermissionCoordinator.request()` is fire-and-forget (it updates `isGranted()` reactively rather than taking a result callback, unlike `PermissionHelper`'s callback-based API), the call site was restructured: a `isMicPermissionPending` flag is set when the permission is requested, and a `LaunchedEffect` watching `isGranted(AppPermission.Microphone)` starts voice-task listening once it flips true while a request is pending. `PermissionHelper.kt` (all three `expect`/`androidMain`/`desktopMain` files) was then deleted — confirmed via grep that nothing else referenced it.

### ✅ Resolved 2026-09-08: debug application ID

Debug and release builds previously shared the application ID (`com.emberr`), and since they're also signed with different keys (debug's auto-managed keystore vs. the real release keystore from Q4.3), installing one over the other didn't silently replace it — it **failed outright** on signature mismatch, forcing a manual uninstall (and data wipe) to switch between testing a release build and continuing regular debug development.

**What was done:** added to the `debug` build type in `app/build.gradle.kts`:

```kotlin
debug {
    applicationIdSuffix = ".debug"
    versionNameSuffix = "-debug"
}
```

**Checked for the classic gotcha before applying this:** `applicationIdSuffix` breaks anything that hardcodes the real package name instead of resolving it dynamically. Verified the `FileProvider` authorities (manifest `${applicationId}` placeholder, and `MainActivity.kt`/`ImageClipboard.android.kt` both use `context.packageName` at runtime) and the internal widget broadcast action name strings (self-consistent, not tied to the real package name).

**That check missed one real instance, caught by an actual build + run:** `ModelPathResolver.android.kt:3` hardcoded `"/data/data/com.emberr/files/$fileName"` directly as a string literal — invisible to a grep for `FileProvider` or broadcast actions since it's neither. Once the debug build became `com.emberr.debug`, that path pointed at a directory that doesn't exist for it, and the embedding model download failed with `FileNotFoundException` on `ModelDownloadSizeCache.android.kt`'s attempt to write `<model>.gguf.size` there. The failure was initially invisible because `ModelDownloadManager.kt`'s catch blocks swallow the real exception into a generic "check your internet connection" message — temporarily added exception logging to surface the real `FileNotFoundException` via Logcat, then removed it again once the root cause was found, so the fix below is the only lasting change.

**Fixed:** `resolveModelPath` now resolves `context.filesDir` via Koin's global accessor (`KoinPlatform.getKoin().get<Context>()`, matching the exact pattern already used in `Platform.kt`/`ImageClipboard.android.kt` for this same "no Context available in a plain top-level function" situation) instead of hardcoding the package name. This self-corrects for whatever the actual installed application ID is, on any build type, permanently — not just a patch for `.debug`. Verified every caller (`ModelDownloadSizeCache.android.kt`, `ModelFileDeletion.android.kt`, `LocalAiEngine.kt`, etc.) treats the return value as an opaque path, so nothing depended on the old literal string shape.

**Verified 2026-09-08:** confirmed working — the embedding model now downloads successfully on the debug build. Still worth confirming both debug and release variants install and run side-by-side.

### ✅ Already resolved — correction to this audit

**`android.enableJetifier`** was checked in `gradle.properties` and is already `false`, not `true` as this document claimed — it was disabled in an earlier commit (`e79c1ea`, "Tune Gradle build performance flags and disable the now-unneeded Jetifier"), predating this pass. This document's original text was stale. No action needed; nothing to remove.

### ✅ Resolved 2026-09-08, corrected same day after a real build failure: SQLCipher migrated to the maintained artifact

`net.zetetic:android-database-sqlcipher:4.5.4` (retired) replaced with `net.zetetic:sqlcipher-android` (the vendor's own long-term-replacement library), per the official migration guide at zetetic.net. This was **not** a version-string change — the package, class name, and native-library-loading step all changed:

| | Old | New |
|---|---|---|
| Package | `net.sqlcipher.database` | `net.zetetic.database.sqlcipher` |
| Room factory class | `SupportFactory` | `SupportOpenHelperFactory` |
| Native library loading | (implicit) | Explicit `System.loadLibrary("sqlcipher")` before first use |

Updated: `gradle/libs.versions.toml` (version + artifact name), `database/DatabaseDriverFactory.kt` and `di/AndroidModule.kt` (import, class name, added the explicit `loadLibrary` call), and `proguard-rules.pro`'s Q2 keep rule (`net.sqlcipher.**` → `net.zetetic.database.**`, otherwise R8 could strip/rename the new library's classes in a release build and reintroduce a Q2-style native-loading crash).

**Version correction:** initially pinned to `4.18.0` (the latest release), but a real `./gradlew assembleDebug` run failed — `4.18.0` requires `compileSdk 37`, and this project is on `compileSdk 36` (AGP 8.13.2's own recommended max, and bumping either was explicitly ruled out by Q6 to avoid the AGP 9 KMP module-split refactor). Checked every release's notes directly on GitHub: **only `4.18.0` (published 2026-08-18) requires API 37** — the prior release, **`4.17.0`** (2026-07-08), does not. Pinned to `4.17.0` instead; same migrated library, same API, just one release earlier.

**Deliberately not preserved:** existing installs' encrypted databases will not open after this change (different package/class, and no confirmed on-disk format compatibility was found in the vendor's migration guide). Accepted explicitly — the app is still in development and no production user data exists yet.

**Verification status:** `./gradlew assembleDebug` failed once on the `4.18.0` compileSdk mismatch (confirming the rest of the dependency graph resolves correctly — sqlcipher-android, its `androidx.sqlite:*:2.7.0` dependencies, and Gradle sync all worked). **Not yet re-verified** with `4.17.0` — re-run the build before trusting this.

### ✅ Resolved 2026-09-08: `jitpack.io` removed from the repository list

This document originally guessed llamatik needed JitPack — **that guess was wrong.** llamatik publishes to Maven Central under `com.llamatik:*`. The actual (and only) `com.github.*` dependency in the project is `com.github.javakeyring:java-keyring:1.0.4` (`desktopMain`) — the library `SecureSyncKeyStorage` and `SecureAiKeyStorage` use to store sync encryption keys and AI provider API keys in the OS keyring, which makes its supply-chain integrity genuinely security-relevant, not just a style concern.

Investigating this turned up a concrete instance of the exact risk this item warned about: the canonical upstream repo, `github.com/javakeyring/java-keyring`, was checked directly via GitHub's API and has exactly 4 tags — the newest is `java-keyring-1.0.3`. **There is no tag matching the `1.0.4` this project depends on.** Its provenance couldn't be confirmed against any inspectable source tag.

It still works because the maintainer separately published the identical coordinates directly to **Maven Central** (confirmed via Sonatype's registry) — and `settings.gradle.kts` already listed `mavenCentral()` (repository resolution order matters: Gradle uses the first repository in the list that has a match) before `jitpack.io`, so the dependency was almost certainly already resolving from Central, not JitPack, before this change.

**What was done:** removed `maven { url = uri("https://jitpack.io") }` from `settings.gradle.kts`. Since it was the only dependency ever needing JitPack, and it's confirmed available on Central under the same coordinates, this should have no resolution impact — just removes an entire class of supply-chain risk (unverifiable, mutable git-tag-based builds) from the project. **Not yet verified:** hasn't been built; confirm the desktop target still resolves `java-keyring` and compiles cleanly.

---

# Q5 — Desktop packaging

Everything above concerns the Android APK. This section covers the Compose Desktop build, which is a **completely separate pipeline**: `jpackage` + `jlink` + the host system's `rpmbuild` / `dpkg`. Nothing in Q2 applies here (see Q2, Correction 2).

**Scope decision:** Windows and macOS are out of scope for this beta. `TargetFormat` is hard-bound to the host OS — `Deb`/`Rpm` require Linux, `Exe`/`Msi` require Windows, `Dmg`/`Pkg` require macOS — so **there is no cross-compilation**. A Windows installer needs a Windows machine or a Windows CI runner. **Debian was dropped from scope on 2026-09-05** — the project now packages `.rpm` only, and will pick up other distros/formats as a deliberate follow-up rather than carrying untested packaging surface. Current target:

```kotlin
targetFormats(TargetFormat.Rpm)
```

## Status summary

| # | Problem | Severity |
|---|---|---|
| Q5.1 | The bundled Java runtime is missing 8 modules the app needs | ✅ Resolved |
| Q5.2 | `packageRpm` produced an `.rpm` that RPM 6.0.2 refused to install | ✅ Resolved |
| Q5.3 | `TargetFormat.AppImage` is not the AppImage format | ✅ Closed — not applicable |
| Q5.4 | The desktop database is stored unencrypted | ✅ Decision made — accepted for now |
| Q5.5 | The desktop app has never been tested as an installed package | ✅ Resolved |
| Q5.6 | LAN sync failure is not surfaced in the desktop UI | ✅ Resolved |

Q5.7 and Q5.8 were already closed before this pass and are recorded at the end of this section. Q5.8 was revisited on 2026-09-08 — a library swap was attempted and then reverted the same day; see that entry for detail.

## ✅ Q5.1 — The bundled runtime is missing 8 modules — Resolved

### Background

A Compose Desktop package does not require the user to have Java installed. `jlink` builds a **trimmed** Java runtime containing only the modules you declare, and `jpackage` bundles it. The Compose plugin's default is deliberately minimal:

```
DEFAULT_RUNTIME_MODULES = ["java.base", "java.desktop", "java.logging", "jdk.crypto.ec"]
includeAllModules = false
```

### The problem

`app/build.gradle.kts` currently declares **no `modules(...)` block at all**, so the packaged runtime ships only those four. The app needs more:

| Module | Needed by |
|---|---|
| `java.sql` | `DatabaseDriverFactory.desktop.kt` — `JdbcSqliteDriver`. Every note read and write. |
| `java.prefs` | `SecureAiKeyStorage`, `SecureSyncKeyStorage`, `DesktopSettingsManager` — API keys, sync keys, all settings |
| `java.net.http` | Every network call — cloud AI providers, WebDAV self-host sync, link previews |
| `java.naming` | Transitively required by the HTTP and TLS stack |
| `java.management` | JVM/runtime introspection pulled in by the coroutines and Netty stacks |
| `java.instrument` | Found by Gradle's own dependency scan |
| `jdk.unsupported` | `sun.misc.Unsafe` — used by Netty, which backs the LAN sync server |
| `jdk.crypto.cryptoki` | PKCS#11 crypto provider, reachable from the TLS stack |

**This is why the app must not be shipped as-is.** It installs and launches — `java.base` and `java.desktop` are present, so a window appears — and then fails the moment it opens the database.

### Two traps found while investigating

1. **`suggestRuntimeModules` misses `java.net.http`.** The Gradle task scans bytecode for direct references; the HTTP client is reached reflectively through Ktor's engine selection, so the scan does not see it. Do not treat that task's output as complete.
2. **`java.datatransfer` and `java.xml` must *not* be listed.** `java --describe-module java.desktop` shows both as `requires transitive`, so `java.desktop` already pulls them in. Listing them is harmless but noise.

### The solution

Add to `nativeDistributions`:

```kotlin
modules(
    "java.sql",
    "java.prefs",
    "java.net.http",
    "java.naming",
    "java.management",
    "java.instrument",
    "jdk.unsupported",
    "jdk.crypto.cryptoki"
)
```

`includeAllModules = true` would also work and is a valid choice, at the cost of roughly 40 MB of runtime the app never touches.

**Verified** by installing the `.rpm` package and exercising all four features below — not `./gradlew run`, which uses the full host JDK and therefore cannot reproduce a missing-module failure at all. See Q5.5.

## ✅ Q5.2 — `packageRpm` output was rejected by RPM 6.0.2 — Resolved

### The problem, as originally observed

`./gradlew packageRpm` exited 0 and produced a file, but installing it on Fedora 44 failed:

```
Failed to run transaction: rpm transaction failed with code 4
```

`rpm -Kv` showed Header SHA256, Payload SHA256 and Payload SHA256 ALT **all** storing the SHA256 of empty input, which looked like `rpmbuild` was finalizing digests over no data while still reporting success.

### The real root cause — not a toolchain bug

Two ordinary, avoidable problems, not a jpackage/RPM 6.0.2 bug as originally suspected:

1. **`rpm-build` (the package providing the `rpmbuild` binary) was not installed on the machine.** Without it, `jpackage` fails fast with `Invalid or unsupported type: [rpm]` before `rpmbuild` ever runs. `sudo dnf install rpm-build` fixed this outright.
2. **The `.rpm` was being read/installed while `rpmbuild` was still writing it.** `rpmbuild -bb` writes its payload incrementally, and Gradle's progress bar sitting at "96% EXECUTING" for a while looks finished when it is not. Installing at that point installs a truncated, partially-written file — which is exactly what produces digests that don't match. Confirmed live via `ps aux` showing the `rpmbuild -bb ... emberr.spec` process still running at that point. Waiting for `BUILD SUCCESSFUL` before installing resolved it completely.

### Verified

- `rpm -Kv` on the completed file reports `Header SHA256 digest: OK` and `Payload SHA256 digest: OK`.
- `sudo rpm -Uvh` installs cleanly, `rpm -q emberr` confirms registration, and the app opens with every feature working.

### Also done in this pass

**Debian and the tarball were removed from scope entirely**, per a deliberate decision to carry one packaging target well rather than several untested ones. `app/build.gradle.kts` now declares `targetFormats(TargetFormat.Rpm)` only, and the `debMaintainer` field was removed from the `linux { }` block. Other distros/formats are a future, separate decision — not silently reintroduced here.

## ✅ Q5.3 — `TargetFormat.AppImage` is not AppImage — Closed, not applicable

`TargetFormat.AppImage` maps to jpackage's id `"app-image"`, which is the **unpacked application directory** — the same thing `createDistributable` produces. It is not the single-file `.AppImage` format users expect, and a real one would require running `appimagetool` as a separate CI post-processing step.

**Moot as of the RPM-only decision above** — `AppImage` is not referenced anywhere in `build.gradle.kts`. If a real `.AppImage` is wanted later, it is new scope, not a fix to something currently broken.

## ✅ Q5.4 — The desktop database is unencrypted — Decision made

`DatabaseDriverFactory.desktop.kt` opens:

```kotlin
JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
```

against a plain, unencrypted `~/.emberr/emberr.db`. Android uses SQLCipher for the same data.

### Why this needs a decision rather than a patch

Emberr's stated pitch is offline-first and private. Right now desktop notes sit in plaintext on disk while Android's are encrypted at rest — a discrepancy worth resolving deliberately rather than discovering later. Options considered: adopting a JDBC-based SQLCipher-compatible driver for Room's desktop backend (real effort — Room's desktop driver is native/JNI-based, not JDBC, so this means writing an adapter layer, and adds another native library to the packaging surface right after Q5.1/Q5.2/Q5.7 were all native-packaging headaches), or a lighter file-level envelope encryption (decrypt to a temp file on launch, re-encrypt on clean shutdown, key from the Linux Secret Service/libsecret) as a cheaper follow-up.

**Decided 2026-09-05: not worth it yet.** Desktop database encryption is explicitly deprioritized for this beta rather than left as an open question — documented here so it isn't rediscovered as a surprise. Revisit if desktop ever needs a stronger privacy story than "trust the OS-level disk," or before any use case where the desktop device itself is less trusted.

## ✅ Q5.5 — The installed package has never been tested — Resolved

Every desktop check so far has used `./gradlew run`, which runs against the **full host JDK** and the Gradle classpath. That configuration cannot detect:

- a missing `jlink` module (Q5.1) — the host JDK has every module
- anything about packaging, installation, desktop-entry registration, or icon placement
- native library extraction from the packaged jar layout rather than the Gradle cache

### Verified

The `.rpm` was installed via `sudo rpm -Uvh` on Fedora 44 and the app was exercised as an installed package rather than via `./gradlew run`: it opened, and every feature was confirmed working. The only issue surfaced this way was a UI bug (desktop dark mode not following the system theme, unrelated to Q5.1's missing-module concern), which has since been fixed by wiring up Linux system-theme auto-detection and a manual Theme setting.

## ✅ Q5.6 — LAN sync failure is invisible in the desktop UI — Resolved

### The problem

`startSyncServer` called `embeddedServer(...).start()` with no error handling. If the port was already in use — a stale Emberr process, or any other program on 8080 — Netty threw `java.net.BindException: Address already in use`, the server died, and **`startBroadcasting` still advertised the machine as available for sync**. Other devices would discover a peer that could never accept a connection.

Encountered for real: a stale process (PID 282106, running 40:47) held port 8080.

### ✅ What was done

`SyncServer.kt` now returns `Boolean`, wraps `start()` in `runCatching`, stops the half-started server on failure, and reports through a new `SyncServerAvailability` holder:

```kotlin
sealed interface SyncServerStatus {
    data object Starting : SyncServerStatus
    data class Running(val port: Int) : SyncServerStatus
    data class Unavailable(val port: Int, val reason: String) : SyncServerStatus
}
```

`SyncServerAvailability` walks the exception cause chain looking for `BindException` and turns it into a message a user can act on ("Port 8080 is already in use by another program. Pick a different sync port in Settings…"). `DesktopMain.kt` now only calls `startBroadcasting` when the server actually started, and `DesktopModule.kt` registers the holder as a Koin `single`.

### ✅ The remaining piece — done 2026-09-05

**`SyncServerStatus` is now consumed by the UI.** Since `SyncServerAvailability` lives in `desktopMain` (it needs `java.net.BindException`) but `SyncViewModel` is `commonMain` code shared with Android, the plain `SyncServerStatus` sealed interface was split out into its own `commonMain` file with no JVM-specific types in it, so `SyncViewModel` can hold an optional `StateFlow<SyncServerStatus>?` without pulling a desktop-only class into shared code. `SyncViewModel` gained a 6th, defaulted-to-`null` constructor parameter; only `DesktopModule.kt`'s Koin registration supplies the real `SyncServerAvailability().status` flow, so Android's registration (unchanged, 5 args) simply never has server-status data, which is correct since Android never hosts the LAN sync server.

`SettingsScreen.kt`'s "LAN Sync" group now shows a red banner with the exact actionable reason (e.g. "Port 8080 is already in use by another program. Pick a different sync port in Settings…") whenever the status is `Unavailable`, on desktop only. That message is doubly useful now because a real "Sync Port" field exists to act on it (see the Q5.6-adjacent fix below).

### Also fixed alongside this

Two related desktop LAN-sync bugs surfaced and were fixed while testing this:

- **A manual sync port field.** The port was previously a single fixed value only ever set implicitly via QR pairing, with no way to change it if it conflicted with another program — even though the error message above already told users to "pick a different sync port in Settings," that setting didn't exist. `SettingsScreen.kt` now has a numeric "Sync Port" field (validated to 1024–65535) in the desktop, not-yet-paired view, with a note that a restart is needed for it to take effect.
- **Pairing was marked "paired" the instant the QR code was shown, not when the phone actually scanned it.** `SyncViewModel.generatePairingData()` used to call `pairingState.markPaired()` immediately on generating the QR, so the desktop's Settings screen would flip to "Paired" and require manually tapping "Disconnect" even if nobody had scanned anything yet. Fixed by moving the real confirmation to the first successfully authenticated `/sync/fetch` or `/sync/push` request the desktop's embedded server receives — the earliest point a real device is provably on the other end. A new persisted `isSyncPairingConfirmed` flag (`SettingsManager`) replaced the old proxy of "does an auth token exist," which had the identical bug in a sneakier form across app restarts.

## ✅ Q5.7 — Closed: llamatik shipped foreign platform natives

The `com.llamatik:library-jvm` jar bundles all three platforms' natives — `native/linux/libllama_jni.so` (30 MB), `native/macos/libllama_jni.dylib` (28 MB) and `native/windows/llama_jni.dll` (25 MB). Since Windows and macOS are out of scope, ~53 MB was dead weight in every Linux package.

**Fixed** with a `llamatikWithLinuxNativesOnly` repack task in `app/build.gradle.kts` that strips `native/macos/**` and `native/windows/**`, substituted into `desktopRuntimeClasspath`. App image went from ~306 MB to **253 MB**, and local AI generation was confirmed still working end-to-end — which proves the repack preserved the classes, `native-libs.txt`, and JNI binding including the `GenStream` callbacks.

Two things learned that are worth keeping:

- In KMP, **declarable** configurations are named after the source set (`desktopMainRuntimeOnly`), while **resolvable** ones are named after the target (`desktopRuntimeClasspath`). `desktopRuntimeOnly` does not exist.
- The Compose plugin collects runtime jars via `compilations.getByName("main").runtimeDependencyFiles`, so excluding the original module from `desktopRuntimeClasspath` is what actually removes it from the package.

## ✅ Q5.8 — Closed via a capability check. A library swap to add real ARM support was attempted and reverted 2026-09-08

The bundled Linux native is x86-64 only, and llamatik's loader branches on `os.name` alone with no `os.arch` check — so on ARM Linux it extracted an x86-64 ELF and `System.load` threw `UnsatisfiedLinkError`. Worse, `NoteIndexer.indexNote` embeds on **every note save**, so the crash hit while typing, not while using AI.

**Fixed** with an `expect`/`actual` `detectLocalAiSupport()` capability check (`domain/ai/LocalAiSupport.kt`). On unsupported hardware the engine throws a typed `LocalAiUnsupportedException` before touching `LlamaBridge`, note indexing returns early, model downloads and uploads are blocked, the local model options are hidden from the UI, and the chat explains why. Cloud AI providers keep working.

**Accepted consequence:** embeddings are local-only with no cloud path, so on ARM Linux semantic note search is gone and the assistant falls back to world-only knowledge. Documented in `README.md` under System Requirements.

### Considered and reverted 2026-09-08: swapping the desktop engine to `de.kherud:llama` (`java-llama.cpp`)

To actually close the ARM-Linux gap rather than just detect it, the desktop target's local-AI engine was swapped from `com.llamatik:library` to `de.kherud:llama` — a different JNI wrapper over the same llama.cpp engine that ships a genuine prebuilt `linux-aarch64` native. This required a shared `LocalLlamaBridge` abstraction (`commonMain` interface, `androidMain` wrapping llamatik unchanged, `desktopMain` wrapping kherud's `LlamaModel`), plus a Q5.7-style repack task to strip kherud's bundled macOS/Windows natives.

**Reverted before ever being compiled**, once a closer look at both libraries' maintenance history changed the risk calculus:

| | llamatik | `de.kherud:llama` |
|---|---|---|
| Latest release | v1.10.1, 2026-08-24 | v4.1.0 (stable), 2025-03-18 — ~18 months old |
| Recent activity | 9 releases since May 2026, including a llama.cpp core upgrade and embedding fixes | Nothing since a June 2025 prerelease |
| Linux ARM64 desktop native | ❌ Confirmed absent — checked every changelog from 1.7.0 to 1.10.1, none mention new platform support | ✅ Ships it |

The concern: `llama.cpp` upstream moves fast (new model architectures, quantization formats), and a binding that hasn't updated its vendored core in ~18 months risks silently failing to load newer GGUF models that llamatik (updated as recently as this year) would handle fine. Weighed against that, ARM Linux desktop support serves a niche audience for this beta, and Windows/macOS/ARM were already deliberately deferred (see the scope decision at the top of Q5). **Decision: keep desktop on llamatik, drop ARM Linux support for now** — matches the original Q5.8 fix above, which remains the current state.

**What was reverted:** `domain/ai/LocalLlamaBridge.kt` (and its `.android.kt`/`.desktop.kt` actuals) deleted; `LocalAiEngine.kt` restored to calling `LlamaBridge`/`GenStream` directly; `LocalAiSupport.desktop.kt`'s processor check restored to x86-64 only; `build.gradle.kts` restored to `com.llamatik:library` in `commonMain` with the original `llamatikWithLinuxNativesOnly` repack/exclusion, and the `de.kherud:llama` dependency and its repack task removed entirely. Verified via `git diff` that no residue was left behind in any of these files.

**Worth revisiting if:** kherud ships a new stable release showing renewed activity, or llamatik itself ever adds Linux ARM support (it hasn't as of 1.10.1), or ARM Linux desktop support becomes an actual user request rather than a hypothetical.

---

# Q6 — Should we upgrade AGP and Gradle? ✅ Resolved

**Resolved 2026-09-09.** The migration this section deferred has been carried out in full: the module split, the `androidLibrary { }` rewrite, and the whole bump chain. The build compiles and the app runs on both targets.

## What the deferral was blocked on, and where each item landed

| Blocker recorded 2026-09-05 | Required for AGP 9 | Now |
|---|---|---|
| KMP + `com.android.application` in one module | must split into an app module and a KMP library module | `:app` applies `com.android.application` only; `:shared` applies `com.android.kotlin.multiplatform.library` (`settings.gradle.kts:27-28`) |
| `android { }` in the shared module | must become `kotlin.androidLibrary { }` | `androidLibrary { }` at `shared/build.gradle.kts:54`; no `android { }` block remains there |
| AGP 8.13.2 | 9.x | `9.4.0` |
| Gradle 9.4.1 | 9.1.0+ | `9.7.1` |
| Kotlin Gradle Plugin 2.1.21 | 2.2.10+ | `2.4.0` |
| KSP 2.1.21-2.0.2 | 2.2.10-2.0.2+ | `2.3.11` |
| Compose Multiplatform 1.8.2 | 1.9.3+ | `1.12.0` |

Also raised in the same pass: `compileSdk` 37 and Java 21 toolchains throughout. The Gradle wrapper was taken to `9.7.1` on 2026-09-09, which is the target this section originally named.

## The coupling concern held up

The original finding — that Gradle and AGP cannot be bumped independently — was correct, and it is why this was done as a single coordinated change rather than incrementally. The IDE-support worry that motivated the deferral did not materialise as a blocker.

## Library upgrades landed alongside it

With the toolchain no longer pinning them, the dependency set was brought to latest stable in the same pass: Ktor `3.5.2`, Coil `3.6.2`, SQLDelight `2.3.2`, Koin `4.2.2`, coroutines `1.11.0`, serialization `1.11.0`, kotlinx-datetime `0.8.0`, llamatik `1.10.1`, lifecycle `2.11.0`, sqlcipher `4.19.0`, haze `1.7.3`, compose-bom `2026.08.00`.

Two items previously recorded elsewhere as holds are now closed rather than deferred:

- **`security-crypto`** — a stable `1.1.0` had shipped, superseding the "no stable release exists" note. Rather than bump onto a deprecated API, `EncryptedSharedPreferences` was replaced with Tink (`tink-android 1.23.0`) and the dependency was removed from the project entirely. Secrets now live in `AndroidSecretCipher` / `TinkSecretStore`, with the database passphrase in a Tink-encrypted file.
- **Room** — no `androidx.room3` artifact is published; `androidx.room` tops out at `2.8.4`, which is what is in use. There is no group-rename migration available to take, so this is not an outstanding decision.

## Remaining, deliberately not taken

| Dependency | Held at | Why |
|---|---|---|
| haze | 1.7.3 | 2.0.0 is still `beta03` |
| glance | 1.2.0 | 1.3.0 is still `alpha02` |
| navigation-compose | 2.9.2 | already the newest stable; 2.10.0 is `alpha02` |

One pairing is worth keeping in mind: navigation-compose 2.9.2 was built against lifecycle 2.9.6, and it now runs against 2.11.0. Chosen because 2.11.0 targets Compose runtime 1.11.0 against 2.9.6's 1.9.0, so it sits far closer to Compose Multiplatform 1.12.0. Navigation was exercised after the upgrade without issue; the fallback if anything surfaces is pinning those two declarations in `shared/build.gradle.kts` back to `2.9.6`.

---

# Recommended order of work

## Phase 1 — Must fix before anyone installs a build

1. ✅ ~~**Backup data-loss bug** — set `allowBackup="false"`, remove `fullBackupContent`, delete `backup_rules.xml`~~ *(Q4.1 — done 2026-09-06)*
2. ✅ ~~**Release signing** — confirmed a non-issue: releases are already produced and signed via Android Studio's Generate Signed Bundle/APK GUI wizard using a keystore kept outside the repo; no `signingConfigs` block is needed for this workflow~~ *(Q4.3 — confirmed resolved 2026-09-07)*
3. ✅ ~~**Back up the keystore offline** — not code, but irreversible if skipped~~ *(Q4.3 — done 2026-09-08)*
4. ✅ ~~**ProGuard rules** — llamatik JNI and enum names~~ *(Q2 — done 2026-09-03)*
5. ✅ ~~**Desktop `jlink` modules**~~ *(Q5.1 — done 2026-09-05)*

## Phase 2 — Before publishing the beta

6. ✅ ~~Remove the four unused media permissions and their plumbing~~ *(Q1 — done 2026-09-03)*
7. ✅ ~~Install and exercise the desktop `.rpm`~~ *(Q5.5 — done 2026-09-05)*
8. ✅ ~~Enforce private-address-only plaintext in `SyncClient`~~ *(Q4.2 — done 2026-09-08)*
9. ✅ ~~Surface `SyncServerStatus` in the desktop sync settings screen~~ *(Q5.6 — done 2026-09-05)*
10. 🟠 Un-ignore and commit `gradle-wrapper.jar` *(Q3)*
11. 🟡 Add the `commonTest` source set and the priority tests *(Q3)*
12. 🟡 Add `.github/workflows/ci.yml` *(Q3)*
13. ✅ ~~Release-only `abiFilters`~~ *(Q4.4 — done 2026-09-08, not yet verified with a build)*
14. ✅ ~~**Decide on desktop database encryption**~~ *(Q5.4 — decided 2026-09-05: not worth it yet)*

## Phase 3 — Quality of life

15. 🟢 `releaseTest` build type and the R8 smoke test *(Q3)*
16. 🟢 Debug `applicationIdSuffix` *(Q4.5)*
17. 🟢 Establish `versionCode` discipline *(Q4.5)*
18. 🟢 Consolidate the permission helpers *(Q4.5)*
19. 🟢 Remove `enableJetifier`; README install instructions and checksums *(Q4.5)*
20. ✅ ~~Chase the `.rpm` digest bug down~~ *(Q5.2 — done 2026-09-05: missing `rpm-build` + installing mid-build, not a toolchain bug)*
21. ✅ ~~Produce a real `.AppImage`~~ *(Q5.3 — closed, not applicable: RPM-only scope)*

---

# Verification checklist

Per project convention, Gradle is run manually rather than by the assistant.

### Build level

- [ ] `./gradlew :shared:allTests` — the `commonTest`, `desktopTest` and `androidHostTest` suites pass on both targets
- [ ] `./gradlew assembleRelease` — R8 completes; confirm `app/build/outputs/mapping/release/` exists and skim `usage.txt` for anything unexpectedly removed
- [ ] `./gradlew lint` — no new errors
- [ ] Confirm the produced APK is **signed**: `apksigner verify --print-certs app-release.apk`

### On a real device, using the release build

Obfuscation bugs appear **only** here, never in a debug build.

- [ ] Create a note using every block type → force-close → reopen → confirm nothing is lost
- [ ] Run a local AI generation *(proves the llamatik JNI keep rule)*
- [ ] Pair via QR and complete a LAN sync *(proves the sync `@Serializable` keeps)*
- [ ] Export a backup, reinstall, re-import *(proves the backup `@Serializable` keeps)*
- [ ] Add and remove all 8 widgets *(proves the Glance state keeps)*
- [ ] Change font style and sub-note open mode, restart the app, confirm the settings persisted *(proves the enum keeps)*
- [ ] Set a reminder ~2 minutes out and confirm it fires
- [ ] Reboot the device and confirm pending reminders still fire
- [ ] Confirm all five media paths still work **with the four media permissions removed** *(Q1 — the code change is done; this device check is what remains)*: image picker, document picker, camera capture, "save image to gallery", and importing an image via another app's share sheet

### Desktop, using the installed `.rpm` — not `./gradlew run`

`./gradlew run` uses the full host JDK and cannot detect a missing `jlink` module. These checks only mean something against an installed package.

- [x] `sudo rpm -Uvh <path-to-rpm>` installs cleanly *(Q5.2 — verified 2026-09-05)*
- [x] `rpm -Kv` on the `.rpm` reports `Header SHA256 digest: OK` and `Payload SHA256 digest: OK` *(Q5.2 — verified 2026-09-05)*
- [ ] Emberr appears in the desktop application menu with the correct icon *(Q5)*
- [x] Open and edit a note, then reopen the app *(proves `java.sql`; Q5.1/Q5.5 — verified 2026-09-05)*
- [x] Save an AI API key, restart, confirm it persisted *(proves `java.prefs`; Q5.1/Q5.5 — verified 2026-09-05)*
- [x] Fetch a bookmark/link preview *(proves `java.net.http`; Q5.1/Q5.5 — verified 2026-09-05)*
- [x] Start LAN sync and pair with the Android build *(proves `jdk.unsupported` / Netty; Q5.1/Q5.5 — verified 2026-09-05)*
- [x] Run a local AI generation *(proves native extraction from the packaged jar layout; Q5.1/Q5.5 — verified 2026-09-05)*
- [ ] Occupy port 8080 with another process, launch Emberr, confirm Settings shows the conflict banner instead of silently advertising itself *(Q5.6 — UI now wired 2026-09-05, not yet manually re-tested)*

### Security and data safety

- [ ] LAN sync still connects over `http://` to a private address
- [ ] A public `http://` URL is now **refused** by `SyncClient` *(Q4.2 — code change done 2026-09-08, not yet manually re-tested)*
- [x] `android:allowBackup="false"` confirmed in the built manifest — Android's OS-level Auto Backup no longer touches Emberr *(Q4.1 — fixed 2026-09-06)*
- [ ] Back up → reinstall → restore leaves the app coherent, never holding an undecryptable database

### Distribution

- [ ] Push a branch and confirm the GitHub Actions run is green
- [ ] `apkanalyzer apk file-size app-release.apk` before and after `abiFilters` to confirm the size reduction
- [ ] `versionCode` was incremented
- [x] Keystore is backed up offline *(Q4.3 — confirmed 2026-09-08)*
