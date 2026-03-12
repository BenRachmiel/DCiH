# Don't Call it Hodoku

A Kotlin Multiplatform + Compose Multiplatform Sudoku app targeting Android and Desktop (JVM). Inspired by the original [HoDoKu](https://hodoku.sourceforge.net/) solver.

## Disclaimer

**This code should be assumed to be entirely AI-generated**, albeit with human guidance and direction. It should not be assumed to be good, safe, or even functional at any point in time — although I'll try to make it as good, safe, and functional as I can.

Pull requests would be sick! It would be really fun to work with others on something like this.

## Why this exists

- **Personal use** — I wanted an app I'd actually want to use to learn Sudoku, even without cell reception. Other people using it is nice but secondary.
- **Learning Android** — understanding how Android apps really work, the deploy cycle, and how to make AI-assisted development more efficient.
- **Learning the lifecycle** — GitHub Actions, commit signing, tags, releases, hashes, and other software lifecycle components.
- **Having fun** — developing something I like.

## Features

- Step-by-step solver with 30+ recognized techniques (naked/hidden singles through coloring and wings) - Work in Progress! Still some AI generated slop examples.
- Progressive hint system (vague → concrete → visual highlights → auto-execute) - I loved this in HoDoKu
- Learn mode with interactive examples for each technique - WIP, but the ones that can regenerate seem quite good
- Difficulty grading (Easy through Extreme) - Tried to lift the same logic from HoDoKu
- Pencil marks, candidate filtering, peer highlighting
- Puzzle export (JSON to clipboard)
- Responsive portrait and landscape layouts

## Building

### Prerequisites

| Tool                  | Version | Notes                                                         |
| --------------------- | ------- | ------------------------------------------------------------- |
| JDK                   | 17+     | JDK 17 is the minimum; builds are tested with JDK 25          |
| Gradle                | 9.3.1   | Included via wrapper (`./gradlew`) — no manual install needed |
| Android SDK           | API 36  | `compileSdk 36`, `buildToolsVersion 36.1.0`                   |
| Kotlin                | 2.2.20  | Managed by Gradle version catalog                             |
| Android Gradle Plugin | 9.0.1   | Managed by Gradle version catalog                             |
| Rust                  | 1.94+   | Stable toolchain; needed to build the native engine           |

You need the Android SDK installed with API level 36 and build tools 36.1.0. The easiest way to get this is through [Android Studio](https://developer.android.com/studio), but you can also install the [command-line tools](https://developer.android.com/studio#command-line-tools-only) standalone. Set `ANDROID_HOME` to point at your SDK root.

### Rust engine

All solver, generator, and board logic lives in a Rust crate (`rust/sudoku-core`). The compiled native library is gitignored — you must build it from source before running the app.

```bash
cd rust/sudoku-core
cargo build --release --features uniffi
```

Then copy the library to where the Gradle build expects it:

```bash
# Desktop (Linux)
cp target/release/libsudoku_core.so ../../app/src/desktopMain/resources/native/

# Desktop (macOS)
cp target/release/libsudoku_core.dylib ../../app/src/desktopMain/resources/native/
```

For Android, use `cargo-ndk`:

```bash
cargo ndk --target aarch64-linux-android --target x86_64-linux-android \
    --platform 24 build --release --features uniffi
```

Or use the convenience scripts: `rust/build-desktop.sh` and `rust/build-android.sh`.

The UniFFI-generated Kotlin bindings (`app/src/jvmMain/kotlin/sudoku/engine/uniffi/sudoku_core.kt`) are checked into the repo. Regenerate only when the FFI surface (`ffi/uniffi_api.rs`) changes:

```bash
cargo run --features "uniffi/cli,uniffi" --bin uniffi-bindgen -- \
  generate --library target/release/libsudoku_core.so \
  --language kotlin --out-dir ../../app/src/jvmMain/kotlin/
```

### Desktop (JVM)

```bash
./gradlew :app:run
```

### Android

Build and install to a connected device or emulator:

```bash
./gradlew :app:installDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk` if you want to sideload it manually (e.g. via `adb install`).

#### Getting it onto your phone

1. **Via USB** — enable [USB debugging](https://developer.android.com/studio/debug/dev-options) on your phone, connect it, and run `./gradlew :app:installDebug`. Gradle will install directly.
2. **Via APK transfer** — build with `./gradlew :app:assembleDebug`, then copy `app/build/outputs/apk/debug/app-debug.apk` to your phone (email, cloud drive, USB file transfer) and open it. You'll need to allow installation from unknown sources.

### Running tests

```bash
cd rust/sudoku-core && cargo test --all-targets   # Rust engine tests
./gradlew :app:desktopTest :app:testDebugUnitTest # App tests (JVM + Android)
```

## Benchmarking

The `benchmark` binary tests the solver against external puzzle datasets for correctness, difficulty correlation, and technique coverage.

```bash
cd rust/sudoku-core
cargo run --release --bin benchmark -- <csv-path> [--limit N] [--sample N]
```

We use the [3 Million Sudoku Puzzles with Ratings](https://www.kaggle.com/datasets/radcliffe/3-million-sudoku-puzzles-with-ratings) dataset from Kaggle. Download the zip, extract `sudoku-3m.csv` into the repo root (gitignored). CSV format: `id,puzzle,solution,clues,difficulty`.

The benchmark reports correctness (solver output vs known solutions), solve rate, technique usage counts, difficulty correlation (Kaggle rating vs our scoring), and throughput. Use `--limit N` to cap puzzles or `--sample N` to solve every Nth.

## Project structure

```
android-sudoku/
├── rust/sudoku-core/  Rust engine — board model, solver, generator, UniFFI bridge
├── app/               Compose Multiplatform UI — screens, components, view model
├── Hodoku/            Reference: original HoDoKu solver source
└── obsidian/          Living project documentation (Obsidian vault)
```

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE) (GPLv3). This is a copyleft license — if you use or modify this code, your derivative work must also be released under GPLv3.

All dependencies (Kotlin, kotlinx-coroutines, Compose Multiplatform, AndroidX) are Apache 2.0 licensed, which is [one-way compatible with GPLv3](https://www.apache.org/licenses/GPL-compatibility.html). The original [HoDoKu](https://hodoku.sourceforge.net/) that inspired this project and from which I lifted logic for certain functions is also GPLv3.

If I've made a mistake in license choice or compatibility, please open an issue and I'll do my best to fix it or take down the project.
