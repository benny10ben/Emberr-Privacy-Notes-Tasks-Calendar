# Emberr — Architecture & Developer Documentation

> **Version:** 1.0 · **Stack:** Kotlin, Jetpack Compose, Room + SQLCipher, Hilt, kotlinx.serialization

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Package Structure](#2-package-structure)
3. [Security Implementation](#3-security-implementation)
4. [Domain Models](#4-domain-models)
5. [Data Layer](#5-data-layer)
6. [Editor Engine](#6-editor-engine)
7. [Presentation Layer](#7-presentation-layer)
8. [Navigation](#8-navigation)
9. [Dependency Injection](#9-dependency-injection)
10. [Roadmap](#10-roadmap)

---

## 1. Project Overview

Emberr is a **privacy-first, encrypted, block-based note-taking app** for Android. All user data is encrypted at rest using a layered security model: the Room metadata database is encrypted via **SQLCipher**, and note content payloads are stored as individual **AES-256-GCM encrypted JSON files** on internal storage.

The editor follows a **block-based content model** inspired by Notion — a note is a flat, ordered list of typed blocks (paragraphs, headings, checkboxes, code blocks, etc.) that can be formatted, indented, reordered, and toggled. The architecture follows **Clean Architecture** with a domain layer, a data layer, and a presentation layer wired together via **Hilt**.

---

## 2. Package Structure

```
com.emberr
├── core.security
│   └── EncryptionManager
├── data.local
│   ├── file
│   │   └── FileStorageManager
│   └── room
│       ├── AppDatabase
│       ├── Daos.kt
│       └── Entities.kt
├── di
│   ├── AppModule
│   └── RepositoryModule
├── domain
│   ├── model
│   │   └── NoteModels.kt
│   └── repository
│       ├── NoteRepository
│       └── NoteRepositoryImpl
├── presentation
│   ├── daily
│   │   ├── CalendarStrip.kt
│   │   └── DailyScreen.kt
│   ├── editor
│   │   ├── BlockComponents.kt
│   │   ├── EditorScreen.kt
│   │   └── EditorViewModel
│   ├── navigation
│   │   └── Screen
│   ├── notes
│   │   └── HomeScreen.kt
│   └── EmberrApp.kt
├── ui.theme
│   ├── SystemBars.kt
│   ├── Theme.kt
│   └── Type.kt
├── EmberrApplication
└── MainActivity
```

---

## 3. Security Implementation

Emberr uses a **three-layer security stack** to ensure no user data is ever stored in cleartext on disk.

### 3.1 Android Keystore + MasterKey

Both the SQLCipher passphrase store and the encrypted file store derive their root key from the **Android Hardware Keystore** via `MasterKey.Builder`:

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
```

The hardware-backed keystore means the AES key material never leaves the secure enclave, even if the device is rooted.

### 3.2 SQLCipher — Room Database Encryption

The Room metadata database (`emberr_metadata.db`) is encrypted at the page level using **SQLCipher**. A 256-bit random passphrase is generated on first launch, encoded as Base64, stored in `EncryptedSharedPreferences`, and then passed into Room via SQLCipher's `SupportFactory`:

```kotlin
// AppModule.kt
val supportFactory = SupportFactory(passphrase)
Room.databaseBuilder(context, AppDatabase::class.java, "emberr_metadata.db")
    .openHelperFactory(supportFactory)
    .build()
```

The passphrase generation path:

```
First launch → SecureRandom.nextBytes(32) → Base64 → EncryptedSharedPreferences
Subsequent launches → EncryptedSharedPreferences.getString() → Base64.decode → SupportFactory
```

### 3.3 EncryptedSharedPreferences

The passphrase itself is protected by `EncryptedSharedPreferences` using:

- **Key encryption:** AES256-SIV (deterministic, so key lookup is possible without a nonce)
- **Value encryption:** AES256-GCM (non-deterministic, authenticated encryption for stored values)

### 3.4 EncryptedFile — Note Content Storage

Note content (the block list) is **not** stored in Room. Instead it is serialized to JSON and written to `context.filesDir` as an `EncryptedFile` using `AES256_GCM_HKDF_4KB`. A fresh IV is derived per-write; the file must be deleted before overwriting because `EncryptedFile` does not support in-place mutation.

```kotlin
// FileStorageManager.kt
if (file.exists()) file.delete()     // EncryptedFile cannot overwrite in place
encryptedFile.openFileOutput().use { it.write(jsonString.toByteArray()) }
```

File naming conventions:

| Note Type  | File Name Pattern           |
|------------|-----------------------------|
| Daily note | `daily_YYYY-MM-DD.json`     |
| Note       | `note_{uuid}.json`          |

### 3.5 Why Split Room + File?

Room indexes **metadata** (title, folder, timestamps, file path). Heavy content lives in files. This means:

- Room queries stay fast regardless of note size.
- Full-text search can operate on metadata without decrypting every file.
- Individual notes can be deleted precisely by removing one file and one DB row.

---

## 4. Domain Models

All models live in `com.emberr.domain.model` and are annotated with `@Serializable` for kotlinx.serialization.

### 4.1 NoteContent

The top-level serializable envelope for a note file:

```kotlin
data class NoteContent(
    val version: Int = 1,
    val blocks: List<NoteBlock>
)
```

The `version` field is reserved for future migration logic.

### 4.2 NoteBlock Sealed Class

`NoteBlock` is a `@Serializable sealed class`. Each subtype carries a `@SerialName` discriminator used by kotlinx.serialization's polymorphic JSON encoding.

**Shared abstract properties on every block:**

| Property           | Type      | Description                                 |
|--------------------|-----------|---------------------------------------------|
| `id`               | `String`  | UUID — stable identity across recompositions |
| `indentationLevel` | `Int`     | 0-based nesting depth (each level = +20 dp) |
| `isBold`           | `Boolean` | Bold formatting flag                        |
| `isItalic`         | `Boolean` | Italic formatting flag                      |
| `isStrikeThrough`  | `Boolean` | Strikethrough formatting flag               |
| `isUnderlined`     | `Boolean` | Underline formatting flag                   |

**Concrete block types:**

| Class               | SerialName  | Key Extra Fields                        |
|---------------------|-------------|-----------------------------------------|
| `TextBlock`         | `"text"`    | `text: String`                          |
| `HeadingBlock`      | `"heading"` | `text`, `level: Int` (1 or 2)           |
| `CheckboxBlock`     | `"checkbox"`| `text`, `isChecked: Boolean`            |
| `BulletedListBlock` | `"bullet"`  | `text`                                  |
| `NumberedListBlock` | `"number"`  | `text`, `number: Int`                   |
| `ToggleBlock`       | `"toggle"`  | `text`, `isExpanded: Boolean`           |
| `CodeBlock`         | `"code"`    | `code: String`, `language: String`      |

**Design note:** `CodeBlock` intentionally does not participate in indentation adjustments (`adjustBlockIndent` returns `b` unchanged) and does not support inline formatting toggles (`updateFormat` returns `b` unchanged). This keeps code blocks semantically pure.

---

## 5. Data Layer

### 5.1 Room Entities

**`NoteMetadataEntity`** (`notes_metadata` table):

| Column       | Type      | Notes                                  |
|--------------|-----------|----------------------------------------|
| `noteId`     | `String`  | UUID primary key                       |
| `title`      | `String`  | Display title                          |
| `folderId`   | `String?` | Null for daily notes or root notes     |
| `isDaily`    | `Boolean` | True if this belongs to the Daily tab  |
| `dateString` | `String?` | `"YYYY-MM-DD"` — only set if `isDaily` |
| `createdAt`  | `Long`    | Epoch ms                               |
| `updatedAt`  | `Long`    | Epoch ms, updated on every save        |
| `filePath`   | `String`  | Relative path within `filesDir`        |

**`FolderEntity`** (`folders` table):

| Column      | Type     | Notes           |
|-------------|----------|-----------------|
| `folderId`  | `String` | UUID primary key |
| `name`      | `String` | Display name    |
| `createdAt` | `Long`   | Epoch ms        |

### 5.2 DAOs

`NoteDao` exposes:
- `insertOrUpdateMetadata` — upsert via `OnConflictStrategy.REPLACE`
- `getAllNotes()` — `Flow<List<...>>` ordered by `updatedAt DESC`
- `getNotesInFolder(folderId)` — `Flow` filtered by folder
- `getDailyNoteMetadata(date)` — single `suspend` lookup by date string
- `deleteNoteMetadata(noteId)` — hard delete by ID

`FolderDao` exposes insert, reactive `getAllFolders()`, and delete.

### 5.3 NoteRepositoryImpl

Implements `NoteRepository`. All suspend functions run on `Dispatchers.IO`.

**Daily note save flow:**

```
saveDailyNote(dateString, content)
  ├── 1. fileStorageManager.saveNoteContent("daily_$dateString.json", content)
  └── 2. noteDao.insertOrUpdateMetadata(NoteMetadataEntity(...))
```

**Daily note load flow:**

```
getDailyNote(dateString)
  ├── noteDao.getDailyNoteMetadata(dateString)      → NoteMetadataEntity? (has filePath)
  └── fileStorageManager.readNoteContent(filePath)  → NoteContent?
```

If no metadata exists, `null` is returned and the ViewModel seeds an empty `TextBlock`.

---

## 6. Editor Engine

### 6.1 EditorViewModel — State

`EditorViewModel` owns all mutable editor state as `StateFlow`s:

| Flow                 | Type                     | Description                                         |
|----------------------|--------------------------|-----------------------------------------------------|
| `_blocks`            | `MutableStateFlow<List<NoteBlock>>` | Full flat block list (source of truth)    |
| `visibleBlocks`      | `StateFlow<List<NoteBlock>>` | Filtered for collapsed toggles (see §6.2)       |
| `requestedFocusId`   | `StateFlow<String?>`     | ID to imperatively focus after a structural change  |
| `selectedDate`       | `StateFlow<LocalDate>`   | Currently active date                               |
| `selectedBlockIds`   | `StateFlow<Set<String>>` | IDs of blocks in multi-select mode                  |

`currentlyFocusedBlockId` is held as a plain `var` (not a `StateFlow`) since it only drives toolbar actions and does not need to trigger recomposition.

### 6.2 Toggle Collapse — Visibility Filtering

`visibleBlocks` is derived from `_blocks` via a single-pass filter:

```kotlin
val visibleBlocks = _blocks.map { allBlocks ->
    val visible = mutableListOf<NoteBlock>()
    var skipUntilLevel: Int? = null

    for (block in allBlocks) {
        if (skipUntilLevel != null) {
            if (block.indentationLevel > skipUntilLevel) continue   // hidden child
            else skipUntilLevel = null                               // re-enter visible scope
        }
        visible.add(block)
        if (block is ToggleBlock && !block.isExpanded) {
            skipUntilLevel = block.indentationLevel                  // begin hiding children
        }
    }
    visible
}
```

**Invariant:** A block is a "child" of a `ToggleBlock` if and only if its `indentationLevel` is strictly greater than the toggle's level and it appears immediately after it in the flat list. The algorithm is O(n) and processes the list in a single forward pass.

### 6.3 Autosave

Every mutation that should persist calls `scheduleAutosave()`:

```kotlin
private fun scheduleAutosave() {
    autosaveJob?.cancel()
    autosaveJob = viewModelScope.launch {
        delay(1000L)
        saveCurrentDailyNote()
    }
}
```

This is a **debounced save**: rapid keystrokes cancel and reschedule the job, so disk writes only happen after 1 second of inactivity. On date change, any pending job is cancelled and a synchronous save runs immediately before switching context.

### 6.4 Structural Block Operations

**Enter key handling (`handleEnter`):**

When the user presses Enter, `BasicTextField` fires `onValueChange` with a newline in the string. `NoteBlockItem` splits at the newline and calls `onEnterPressed(blockId, remainderText)`. The ViewModel:

1. Finds the current block by ID.
2. Creates a new block of a contextually appropriate type (e.g. pressing Enter in a `CheckboxBlock` creates another `CheckboxBlock`; pressing Enter in a heading or toggle creates a plain `TextBlock`).
3. Inserts the new block at `idx + 1`.
4. Sets `requestedFocusId` to the new block's ID, triggering a `LaunchedEffect` in `EditorScreen` to call `focusRequester.requestFocus()` after a 50ms delay.

**Backspace on empty (`handleBackspaceOnEmpty`):**

1. If the block has `indentationLevel > 0`, it is outdented by one level instead of deleted.
2. Otherwise, the block is removed from the list and focus is moved to the previous block.
3. A single block at index 0 is never deleted (the note always has at least one block).

**Block type conversion (`changeFocusedBlockType`):**

The text content and indentation level are preserved; the block is replaced with a new instance of the target type. This is purely a ViewModel-level operation with no persistence side effect beyond the next autosave.

### 6.5 Multi-Select Mode

Selection mode activates automatically when `selectedBlockIds` is non-empty (triggered by a long press). In selection mode:

- The `TopAppBar` in `DailyScreen` switches to a contextual action bar showing Copy, Cut, and Delete actions.
- `BasicTextField` has `enabled = false`, preventing accidental edits.
- An overlay `Box` intercepts all taps on each block and routes them to `onToggleSelection`.
- The floating editor toolbar is hidden (`!isSelectionMode` guard in `AnimatedVisibility`).

---

## 7. Presentation Layer

### 7.1 CalendarStrip

A `LazyRow` of date cells spanning 31 days (±15 from today). The selected date cell uses an inverted `onSurface` background to achieve a high-contrast "selected" state that adapts automatically to light and dark themes without hardcoded colors.

`LaunchedEffect(selectedDate)` animates the list to keep the selected date in view. The initial scroll offset is seeded via `rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex - 3)` so today is not pinned to the far left edge.

### 7.2 EditorScreen

Structured as a `Column` with two children:

1. **`LazyColumn` (weight 1f):** Contains the calendar strip, the dynamic date header, and the block list. `contentPadding = PaddingValues(bottom = 120dp)` ensures the last block is never obscured by the floating toolbar or bottom navigation.

2. **`AnimatedVisibility` toolbar:** Slides in from the bottom (`slideInVertically`) when the keyboard is open and selection mode is inactive. Uses `WindowInsets.isImeVisible` to detect keyboard state.

The date header is content-aware: if the note contains any `CheckboxBlock`s, it renders a task count chip (icon + count). Otherwise it renders a `Spacer` to maintain consistent vertical rhythm.

### 7.3 NoteBlockItem

Each block renders inside a `Box` with two layers:

- **Content layer:** A `Row` containing a drag handle icon, an optional block prefix (bullet dot, number, checkbox, toggle chevron), and a `BasicTextField`.
- **Selection overlay:** When `inSelectionMode` is true, a full-size transparent `Box` sits on top, capturing all taps and routing them to `onToggleSelection`. This prevents accidental text editing during selection.

Typography is resolved per block type:

| Block Type         | Font Size | Weight       | Font Family  |
|--------------------|-----------|--------------|--------------|
| `HeadingBlock` L1  | 26 sp     | Bold         | BricolageFont |
| `HeadingBlock` L2  | 20 sp     | Bold         | BricolageFont |
| `CodeBlock`        | 14 sp     | Normal       | Monospace    |
| All others         | 16 sp     | Normal       | BricolageFont |

Inline format flags (`isBold`, `isItalic`, etc.) are composed on top of the base style. Indentation is applied as `padding(start = (20 + indentationLevel * 20).dp)`.

### 7.4 EditorToolbar

A horizontally scrollable `Row` of `IconButton`s divided into logical groups by `VerticalDivider` components. Groups: Add / Block Type / Indentation / Formatting / Headings & Code.

`LocalMinimumInteractiveComponentSize provides 36.dp` reduces the default 48dp touch target to keep the toolbar compact without sacrificing usability.

---

## 8. Navigation

Navigation uses Jetpack Navigation Compose with two top-level destinations defined as `sealed class Screen` objects.

The root `Scaffold` in `EmberrApp` sets `contentWindowInsets = WindowInsets(0)` to prevent the Scaffold from consuming IME insets itself. This allows `EditorScreen` to own `imePadding()` exclusively, avoiding double-application of keyboard insets.

Transitions are disabled (`EnterTransition.None`, `ExitTransition.None`) for instant tab switching — appropriate given the minimal two-tab structure.

**EmberrBottomBar** is a floating, pill-shaped `Surface` using `MaterialTheme.colorScheme.onSurface` as its background. Selected items are shown as a `surface`-colored circular cutout within the pill, achieving a natural inversion for both light and dark themes without theme-conditional color logic.

---

## 9. Dependency Injection

All Hilt bindings are in `SingletonComponent` scope.

**`AppModule` provides:**

| Binding                  | Type                    | Notes                                         |
|--------------------------|-------------------------|-----------------------------------------------|
| `FileStorageManager`     | Singleton               | Injected with `@ApplicationContext`            |
| `SharedPreferences`      | `EncryptedSharedPreferences` | Used exclusively for the DB passphrase   |
| `ByteArray` (passphrase) | Singleton               | Generated once, stored in EncryptedPrefs       |
| `AppDatabase`            | Singleton               | SQLCipher-backed Room database                 |
| `NoteDao`                | Singleton               | Delegated from `AppDatabase`                   |
| `FolderDao`              | Singleton               | Delegated from `AppDatabase`                   |

**`RepositoryModule` provides:**

| Binding          | Type              | Notes                                                      |
|------------------|-------------------|------------------------------------------------------------|
| `NoteRepository` | `NoteRepositoryImpl` | Constructor-injected with `NoteDao`, `FileStorageManager`, `Context` |

`EditorViewModel` is annotated `@HiltViewModel` and injected at the composable level via `hiltViewModel()`. It receives `NoteRepository` directly.

---

## 10. Roadmap

### Phase 1 — Notes Tab

The `HomeScreen` stub and all data-layer plumbing for notes are already in place (`saveNote`, `getAllNotes`, `FolderEntity`, `FolderDao`). What remains is purely UI work.

**Folder Structure**

Implement a directory composable backed by `FolderDao.getAllFolders()`. Each folder tap navigates to a filtered note list driven by `NoteDao.getNotesInFolder(folderId)`. Folder creation requires only `FolderDao.insertFolder(FolderEntity(...))`.

**Note Library UI**

A `LazyVerticalGrid` (2-column) or `LazyColumn` (list) of `NoteMetadataEntity` cards. Cards should display the title, `updatedAt` timestamp, and a truncated content preview. Content preview requires reading the encrypted file — consider caching a plaintext snippet in the `NoteMetadataEntity.title` field or adding a `preview: String` column.

**Global Search**

Because content lives in encrypted files, real-time full-text search requires an in-memory index or a decryption pass at query time. Recommended approach: on each save, extract a plaintext representation of all block texts and store it in a new `contentSnapshot: String` column in Room. Room's `LIKE` query or FTS4/FTS5 extension can then search across all notes without decrypting files at query time.

---

### Phase 2 — Advanced Block Types

**Image Blocks**

Add an `ImageBlock` to the `NoteBlock` sealed class with a `localFilePath: String` field. On insert, copy the image from the Android Photo Picker URI into `context.filesDir` (optionally encrypted with a per-file `EncryptedFile`). Render with Coil's `AsyncImage`. Deletion must clean up the image file as well as the block entry.

**Drag & Drop**

The `DragIndicator` icon is already rendered in `NoteBlockItem`. Wire it up using `Modifier.draggable` combined with a custom `LazyColumn` drag-reorder state (or the `reorderable` library). The ViewModel needs a `reorderBlocks(fromIndex: Int, toIndex: Int)` function that moves the block and triggers autosave.

**Table Blocks**

Add a `TableBlock` with a `rows: List<List<String>>` field. Render as a `LazyColumn` of `Row`s within a fixed-width `Box`. For editing, tapping a cell focuses a dedicated `BasicTextField` overlay. Tables should not participate in indentation.

---

### Phase 3 — Polish

**Settings Screen**

A new `SettingsScreen` destination accessible from the navigation. Initial settings: theme selection (System / Light / Dark), font size scale, and autosave interval. Store preferences in a dedicated `DataStore<Preferences>` instance (not in the encrypted SharedPreferences — theme preferences are not sensitive).

**Biometric Lock**

Wrap app launch in a `BiometricPrompt` authentication gate using `androidx.biometric`. On successful authentication, release the DB passphrase from the Keystore. Gate this behind a Settings toggle. Consider using a `UserAuthenticationRequired` key property on the `MasterKey` for tighter integration with the hardware-backed keystore.

**Undo / Redo**

Maintain a bounded `ArrayDeque<List<NoteBlock>>` history stack in the ViewModel. Push a snapshot before every structural mutation (Enter, Backspace, type change, deletion). `undo()` pops the stack and replaces `_blocks`; add an undo button to the editor toolbar.

---

*Generated from source review of Emberr v1.0 codebase — May 2026.*
