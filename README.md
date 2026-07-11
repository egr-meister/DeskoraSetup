# Deskora Setup

Plan and document your workspace layout in one clear local desk map.

Deskora Setup is a native Android application (Kotlin + Jetpack Compose, Material 3) that lets you manually build and maintain one or more workspace setups as a simplified **top-down desk map**. Devices, accessories, cable routes, and desk zones are drawn as abstract Compose shapes. Everything is entered by you and stored only on your device.

---

## Main features

- **Top-down desk map** — a simplified desk viewed from above, built with Compose layouts and Canvas.
- **Desk setups** — create, edit, duplicate, archive, restore, delete, and switch between multiple setups.
- **Desk zones** — divide the desk into Main, Left, Center, Right, Rear, Front, Cable, Storage, Writing, or Custom regions using stable directional/size controls (no fragile drag required).
- **Setup items** — generic devices and accessories (monitor, laptop, keyboard, mouse, lamp, dock, charger, notebook, plant, cable hub, and more) represented as abstract shapes.
- **Item placement** — normalized coordinates, 90° rotation, resize, and zone assignment through predictable controls.
- **Unplaced-items tray** — hold items that are not yet placed on the desk.
- **Cable routes** — manual visual connections between two items, colored by type.
- **Cleaning checklist** — recurring desk cleaning tasks with local due-date calculation.
- **Needed-items list** — a personal list of items you may want, with optional conversion into desk items.
- **Templates** — built-in Work Desk, Study Desk, Gaming Desk, and Blank Desk, plus your own custom templates.
- **Local search** — search zones, items, cables, checklist tasks, and needed items in the active setup.
- **Fully offline** — no account, no cloud, no internet, no ads, no analytics.

---

## Disclaimers

**Manual setup disclaimer**
> Deskora Setup is a manual workspace organizer. Desk dimensions, zones, devices, accessories, cables, checklists, and notes are entered by the user. The app does not detect hardware, control devices, provide electrical guidance, recommend products, or guarantee ergonomic or safety outcomes.

**Hardware and safety disclaimer**
> Deskora Setup is for personal organization only. Follow manufacturer instructions and appropriate professional guidance for electrical connections, mounting, cable loads, power distribution, and workspace ergonomics.

- **No smart-device connection.** The app never connects to, detects, or controls any device.
- **No electrical guidance.** No wiring instructions, power-strip load calculations, or voltage advice are provided.
- **No ergonomic guarantee.** The app does not provide medical or professional ergonomic assessments.
- **No product recommendations.** No brands, product suggestions, or compatibility advice.
- **No shopping links.** No store integration, affiliate links, or product catalog.
- **No brands.** Item shapes are abstract and never imitate a specific commercial product. Any label you type is your own.

---

## Privacy

> Deskora Setup stores desk setups, zones, generic device labels, accessory labels, cable routes, cleaning tasks, notes, needed items, templates, and settings locally on this device. The app has no account, no cloud sync, no internet access, no ads, no analytics, no payments, no smart-device access, no Bluetooth access, no network scanning, and no product shopping.

### What the app does NOT use
No camera, no gallery, no augmented reality, no OCR, no barcode/QR scanning, no microphone, no voice control, no location, no maps, no contacts, no storage permissions, no calendar permissions, no notifications, no background tasks, no alarms, no sensors, no Bluetooth, no NFC, no Wi-Fi discovery, no USB device detection, no local network scanning, no accounts, no backend, no cloud sync, no Firebase.

### Runtime permissions
**None.** The app requests no runtime permissions and declares no dangerous permissions in the manifest (including no `INTERNET` permission).

---

## Architecture

Simple MVVM with a single local repository.

- **Kotlin**, **Jetpack Compose**, **Material 3**
- **Navigation Compose** for all screen routing
- **AndroidViewModel** + **StateFlow** for observable app state
- **Kotlin Coroutines / Flow**
- **DataStore Preferences** storing serialized JSON strings
- **Kotlinx Serialization** for models
- **Gradle Kotlin DSL**

No dependency-injection framework, no Room, no networking, no cloud SDKs.

### Offline-only architecture & local storage
All application data is persisted with **DataStore Preferences**. Each collection is stored as a serialized JSON string under a dedicated key:

`desk_setups_json`, `desk_zones_json`, `setup_items_json`, `cable_routes_json`, `cleaning_tasks_json`, `needed_items_json`, `setup_templates_json`, `settings_json`.

The repository (`DeskoraRepository`):
- exposes app data as a `Flow<AppData>`,
- deserializes safely (lenient JSON, unknown keys ignored, per-element recovery),
- merges missing settings with defaults,
- always merges the four built-in templates on read (so they are never duplicated after relaunch and only user templates are persisted),
- guards every operation so missing setups, zones, items, or cable endpoints never crash.

### Data model
- **DeskSetup** — id, name, `setupType`, `deskShape`, optional `widthCm`/`depthCm`, description, `templateSourceId`, archived, timestamps.
  - **Desk shapes:** Rectangle, L-Shape, Corner, Compact, Custom.
- **DeskZone** — normalized geometry (x, y, width, height in 0.0–1.0), `zoneType`, `colorKey`, `sortOrder`, note.
- **SetupItem** — normalized geometry, `itemType`, `customTypeName`, `rotationDegrees` (0/90/180/270), `colorKey`, `status`.
- **CableRoute** — `cableType`, `startItemId`/`endItemId` (nullable), optional intermediate points, hidden flag, label, note.
- **CleaningTask** — `category`, `frequencyType`, `intervalValue`, `selectedDays`, `nextDueDate`, `lastCompletedDate`, enabled, note.
- **NeededItem** — `category`, `quantityLabel`, `priority`, acquired, `plannedZoneId`, note.
- **SetupTemplate** — default zones and generic default items.

### Normalized geometry
Item and zone positions/sizes are stored as normalized floats in `[0.0, 1.0]`. The map scales to the available width and preserves the desk aspect ratio. Geometry utilities clamp values, prevent zero-size and negative dimensions, and guard against `NaN`/`Infinity`, so the Canvas never crashes — even at zero width/height.

### Generic item shapes
Items are drawn as abstract shapes (wide rectangle + stand line for monitors, hinge line for laptops, key lines for keyboards, ovals for mice, curved outline for headphones, lined paper for notebooks, potted circle for plants, etc.). No product photographs, no branded silhouettes.

### Unplaced-items tray
Items with status `Unplaced` appear in a horizontal tray below the desk map, with an integrated **Add Item** action.

### Cable routes & missing endpoints
Cables render **behind** items and connect two placed items by their centers. If a connected item is deleted, the cable is **kept** and marked **“Missing endpoint”** in the list; it can be reassigned or deleted. The app never claims the visual route matches real cable length, compatibility, or electrical safety.

### Cleaning checklist & recurrence
Recurrence is computed locally with `java.time.LocalDate` (Manual, Daily, Selected Days, Weekly, Every N Days, Monthly). Due-state is evaluated when the app or checklist screen becomes active — there are no background workers, alarms, or notifications. Status values are neutral: Due, Upcoming, Complete, Manual, Disabled.

### Needed-items list & conversion
Keep a personal list of items you may need (title, category, quantity label, priority, planned zone, note). Acquired items can be converted into a `SetupItem` via **Add to Desk** — choose a generic item type and a zone (or the unplaced tray), then keep or remove the needed item.

### Templates
Built-in **Work Desk**, **Study Desk**, **Gaming Desk**, and **Blank Desk** use only generic labels. Applying a template creates a new setup and copies its zones and generic placeholder items. Built-in templates are read-only; you can also save any setup as a **custom template** and duplicate/edit/delete your own.

### Search and filters
Fully local search across setup/zone/item/cable/checklist/needed fields, with category and priority filters on the Needed Items screen and grouped filtering on the Item List.

---

## Visual concept & layout uniqueness

**Blueprint Desk Surface** — precise, calm, technical, non-commercial. The Home screen is a **true spatial workspace layout**, not the generic "mascot → title → stat card → button stack" pattern:

1. Compact setup selector at the top with a small Edit Map action.
2. A large top-down desk surface occupying the main area.
3. Zones as subtle outlined regions; devices/accessories as plates inside the map.
4. Cable routes drawn behind item labels.
5. A narrow unplaced-items tray with an integrated Add Item action.
6. A compact cleaning strip and a Needed-Items drawer preview.
7. Bottom navigation for Desk, Items, Checklist, Needed, and Settings.

### Desk-map rendering approach
Rendering order: desk surface → grid → zones → cables → items → selection outlines → labels. Items are clipped to desk bounds; selected outlines draw above normal content.

### Accessibility list fallback
The desk map exposes a full textual `contentDescription`, and every screen provides a list-view alternative (Item List, Zone Management, Cable List) so content is reachable without the spatial map. Item and cable types are always communicated with text, not color alone.

### App icon concept
A custom adaptive icon: blueprint-navy background with a subtle grid, a simplified top-down desk rectangle, one monitor shape, one keyboard shape, two cable lines, and a small warm-wood accent. No logos, no text, no shopping/Wi-Fi/Bluetooth symbols. Legacy PNG launcher icons are provided for API 24–25; an adaptive icon (with monochrome layer) is used on API 26+.

### Splash screen concept
A static splash (AndroidX SplashScreen library) on a light blueprint background with the centered top-down desk icon. No animation-heavy effects.

---

## Requirements & how to build

### Prerequisites
- **JDK 17**
- **Android Studio** (Koala or newer recommended)
- **Android SDK Platform 35** and **Build Tools 35.0.0**

### Open in Android Studio
1. `File → Open…` and select the `DeskoraSetup` folder.
2. Let Gradle sync. The project targets **compileSdk = 35**, **targetSdk = 35**, **minSdk = 24**, JDK 17, portrait-only.
3. Run the `app` configuration on a device/emulator (Android 7.0+).

### Generate the Gradle wrapper (first time)
This repository ships the wrapper scripts but not the `gradle-wrapper.jar` binary. Generate it once with a local Gradle 8.9 install:

```bash
gradle wrapper --gradle-version 8.9
```

Android Studio also regenerates the wrapper automatically on first sync. CI generates it as part of the workflow.

### 16 KB page-size compatibility
The app uses only Kotlin, Compose, DataStore, and Kotlinx Serialization — no third-party native binaries — so it is compatible with Android 15+ 16 KB memory page sizes. Verify the final release bundle on a 16 KB target as part of release testing.

### Debug build
```bash
./gradlew :app:assembleDebug
```

### Non-minified release build (do this first)
The release build type is configured for staged R8. For the **first** release validation, temporarily set both flags to `false` in `app/build.gradle.kts`:

```kotlin
getByName("release") {
    isMinifyEnabled = false
    isShrinkResources = false
    ...
}
```

Build, install, launch, and run the functional checklist. **Only after** the non-minified release passes, restore the final configuration:

```kotlin
getByName("release") {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
    ...
}
```

Then rebuild and re-test Kotlinx Serialization, DataStore, Navigation Compose, Canvas rendering, geometry handling, templates, and checklist scheduling.

---

## Signing (release APK & AAB)

Release APK and AAB must be signed with a real **PKCS12** keystore. The build **never** falls back to the Android debug key for release artifacts — if credentials are missing, the release build fails clearly.

### Generate a keystore
```bash
keytool -genkeypair -v -storetype PKCS12 \
  -keystore deskora-setup-release-key.p12 \
  -alias deskora_setup_key -keyalg RSA -keysize 2048 -validity 10000
```

Keep the same password for the store and key unless you configure separate values.

### Local signing setup
Create a git-ignored `keystore.properties` in the project root (never commit it):

```properties
storeFile=/absolute/path/to/deskora-setup-release-key.p12
storePassword=YOUR_STORE_PASSWORD
keyAlias=deskora_setup_key
keyPassword=YOUR_KEY_PASSWORD
```

Alternatively, provide these environment variables (used by CI):
`ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

Both the release APK and AAB use `signingConfigs.release`.

### Required GitHub Secrets
- `ANDROID_KEYSTORE_BASE64` — base64 of the `.p12` file (`base64 -w0 deskora-setup-release-key.p12`)
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

**Never commit** the PKCS12 file, decoded keystore, passwords, or signing properties. These are already in `.gitignore`.

### GitHub Actions
`.github/workflows/android-build.yml` runs on push to `main` and via `workflow_dispatch`. It:
1. checks out the repo,
2. sets up JDK 17,
3. installs Android SDK Platform 35 and Build Tools 35.0.0,
4. sets up Gradle 8.9 with caching and generates the wrapper,
5. decodes `ANDROID_KEYSTORE_BASE64` into a temporary PKCS12 file,
6. exposes signing secrets only as environment variables,
7. builds the signed release **APK** and **AAB**,
8. locates the APK and runs `apksigner verify --print-certs`,
9. **fails** if verification fails or the certificate contains `CN=Android Debug`,
10. uploads the signed APK (test artifact) and the signed AAB (Google Play artifact).

CI covers compilation, signing, certificate verification, and artifact generation. CI is **not** proof that the app launches — always run the local functional checklist.

---

## Local release verification

```bash
# Build & verify
./gradlew :app:assembleRelease :app:bundleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk

# Install & inspect
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat
```

The signing certificate must **not** contain `CN=Android Debug`.

- Use the **APK** for local installation and verification.
- Use the **AAB** for Google Play. Only the `.aab` file should be uploaded to Google Play.

### Local functional test checklist
- First launch with empty storage; onboarding; skip onboarding.
- Create Blank/Work/Study/Gaming/custom setups; switch, edit, duplicate, archive, restore, delete.
- Add/edit/move/resize/reorder zones; delete empty zone; attempt to delete a zone with items (reassigns items to the tray first).
- Add monitor/laptop/keyboard/mouse/accessory/custom item; move within desk; move between zones; resize; rotate; duplicate; send to unplaced tray; mark Planned; archive; restore; delete.
- Add cable; select endpoints; edit type; hide/show; delete a connected item and verify the **Missing endpoint** state; reassign.
- Open the list-view fallback.
- Add cleaning tasks (daily/weekly/selected-day/every-N-days/monthly); mark complete; verify next due date; undo; disable; delete.
- Add needed item; mark acquired; convert to a setup item.
- Create/apply/delete a custom template.
- Search by item/zone/cable/checklist/needed.
- Reset all local data; relaunch; launch in airplane mode and confirm full functionality.
- Confirm no `INTERNET` permission, no runtime permission dialogs, no Bluetooth/device scanning, no product links/prices/brand suggestions.

Check `adb logcat` for: `ClassNotFoundException`, `NoSuchMethodError`, serialization/DataStore parse crashes, navigation-argument crashes, Canvas drawing crashes, invalid geometry / `NaN` / `Infinity`, missing setup/zone/item/cable crashes, duplicate built-in templates, checklist-date crashes, R8-related crashes, and signing misconfiguration.

---

## Data reset behavior
- **Clear completed checklist history** — clears last-completed dates for the active setup's tasks.
- **Clear acquired needed items** — removes acquired needed items for the active setup.
- **Delete archived setups** — permanently removes all archived setups and their data.
- **Delete active setup** — removes the active setup and all of its zones, items, cables, tasks, needed items, and notes (with confirmation).
- **Reset all local data** — permanently removes every setup, zone, item, cable, task, needed item, note, custom template, and setting.

## Manual-entry limitations
All information is entered by you. Desk dimensions are optional, entered in centimeters, and are not converted or validated against real products. The app does not detect hardware, measure dimensions automatically, verify cable connections or power safety, or sync between devices.

---

## Technology stack (dependencies)
`androidx.core:core-ktx`, `androidx.lifecycle:lifecycle-runtime-ktx`, `androidx.lifecycle:lifecycle-viewmodel-ktx`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.lifecycle:lifecycle-runtime-compose`, `androidx.activity:activity-compose`, Compose UI/graphics/tooling-preview, `androidx.compose.material3:material3`, `androidx.compose.material:material-icons-extended`, `androidx.navigation:navigation-compose`, `androidx.datastore:datastore-preferences`, `kotlinx-coroutines-android`, `kotlinx-serialization-json`, `androidx.core:core-splashscreen`, and core-library desugaring for `java.time`.
```
