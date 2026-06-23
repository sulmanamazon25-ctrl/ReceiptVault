# ReceiptVault

A privacy-first, **fully offline** receipt scanner for Android. Receipts are stored in an
encrypted local database and **never leave the device** — there is no `INTERNET` permission,
no analytics, no ads, and no cloud.

This repository is a working **first MVP**: capture and organize receipts, store them
encrypted, and lock the app behind biometrics — all offline.

---

## What the MVP does

- **Add a receipt** with title, merchant, amount, tax, date, category, notes, and an optional
  photo captured with the device camera (offline, via the system camera app).
- **Browse** all receipts on the home screen with a running total, and filter by folder.
- **View / edit / delete** a receipt, including its photo.
- **Folders** to organize receipts (create, delete, filter).
- **Search** receipts by title.
- **Settings**: biometric app lock, screenshot blocking (`FLAG_SECURE`), and dynamic color.
- **Encrypted storage**: Room on top of SQLCipher, keyed by a random passphrase that is sealed
  with an AES-256-GCM key held in the Android Keystore.
- **Optional biometric app lock** gate on launch.

## Architecture

Clean Architecture + MVVM.

```
presentation/   Compose UI, screens, view models, navigation, theme
domain/         Models + repository interfaces (no Android dependencies)
data/           Room (entities, DAOs), repository implementations, mappers,
                preferences, image storage
security/       Keystore-sealed DB passphrase, biometric auth, screenshot flag
di/             Hilt modules (dispatchers, database, repositories)
```

Empty packages (`ocr/`, `scanner/`, `pdf/`, `billing/`, `worker/`, `domain/usecase/`,
`data/local/`) are intentional placeholders for the roadmap below.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Hilt · Room + SQLCipher · Coroutines/Flow ·
Navigation Compose · WorkManager · AndroidX Biometric · minSdk 26.

---

## Build & run

1. Open the project root in **Android Studio** (a recent stable release).
2. On first sync, Android Studio downloads the Gradle distribution declared in
   `gradle/wrapper/gradle-wrapper.properties` and regenerates the Gradle wrapper JAR. *(The
   wrapper JAR is intentionally not committed; let the IDE create it, or run
   `gradle wrapper` if you have a local Gradle.)*
3. Let Gradle sync resolve dependencies, then **Run** the `app` configuration on a device or
   emulator (API 26+).

> **First-run note on encryption:** the SQLCipher native library is loaded at app start and
> the database passphrase is generated and sealed in the Keystore on first launch. Use a
> device/emulator image that supports the Keystore (standard Google images do).

---

## Version choices (deliberate)

The toolchain is pinned to a **proven, internally consistent** set rather than the
absolute-newest releases, because several mid-2026 "latest" versions break the conventional
setup. All versions live in `gradle/libs.versions.toml`.

| Area | Pinned | Why not newest |
|------|--------|----------------|
| Android Gradle Plugin | 8.7.3 | AGP 9.x switches to a new DSL with built-in Kotlin and changes how KSP/Hilt/the Compose plugin are applied; 8.7.x keeps the conventional, well-documented wiring. |
| Kotlin | 2.0.21 | Pairs with a matching KSP build and the Compose compiler plugin; 2.4.x was too fresh for the surrounding plugin ecosystem. |
| KSP | 2.0.21-1.0.28 | Must track the Kotlin version exactly. |
| Room | 2.7.1 | Room 3.0 (`androidx.room3`) drops the `SupportSQLite` open-helper API that SQLCipher's `SupportOpenHelperFactory` depends on. Staying on 2.x keeps SQLCipher integration straightforward. |
| SQLCipher | `net.zetetic:sqlcipher-android:4.9.0` | The current artifact; the older `android-database-sqlcipher` is deprecated and lacks Play's mandatory 16 KB page-size support. |
| compileSdk / targetSdk | 35 | Play-compliant and reliable for a first build. Bumping to 36 is a one-line change once your installed SDK/AGP support it (see below). |

### Upgrade paths (when you're ready)

- **compileSdk/targetSdk → 36:** change `compileSdk` and `targetSdk` in
  `gradle/libs.versions.toml` after installing the SDK 36 platform; no code changes expected.
- **AGP 9 / Kotlin 2.4 / Room 3:** these are larger migrations (new AGP DSL, Room's
  `androidx.room3` package rename, and re-validating the SQLCipher open-helper path). Tackle
  them one at a time, not together.

---

## Build verification

This project was authored without an Android SDK/Gradle daemon available, so it has **not been
compiled here**. The source is written to be compile-ready, but on first Gradle sync minor
adjustments can surface — most likely around the exact alignment of
**Room ↔ androidx.sqlite ↔ SQLCipher** versions for your installed tooling. If Gradle reports a
version mismatch there, align those three in `libs.versions.toml` and re-sync.

---

## Roadmap (post-MVP)

- On-device OCR (ML Kit) to auto-fill amount/merchant — `ocr/`, `scanner/`.
- PDF export of receipts and reports via `PdfDocument` — `pdf/`.
- Real Google Play Billing for Pro — `billing/`.
- Paging 3 wiring for very large libraries.
- WorkManager jobs (e.g., scheduled local maintenance) — `worker/`.
- Unit/UI test coverage and a security review.

## Privacy

No `INTERNET` permission is declared. Camera is used only to attach a photo to a receipt, and
photos are stored in app-private internal storage, shared with the camera app through a
scoped `FileProvider` URI. `allowBackup` is disabled so receipts are not copied off-device by
system backups.
