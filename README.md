# Call Tracker (Android)

A personal Android app that logs **every call** on your own phone — incoming,
outgoing, missed and rejected — storing the **number, date/time and duration**,
grouped per number, with full history. It also **attempts automatic call
recording** and attaches the audio file to the matching call.

Built with Kotlin + Jetpack Compose + Room + WorkManager.

---

## ⚠️ Read this first — what actually works

**Call logging: works everywhere.** Reading all calls (number, direction,
date, duration) and keeping a per-number history is fully supported on every
Android version. This is the reliable core of the app.

**Automatic call recording: depends entirely on your phone.** Since Android 10
(2019), and tightened again in 2022, Google blocks any normal third-party app
from recording the *voice stream of a call* (both people talking). The app
tries three audio sources in order — `VOICE_CALL`, `VOICE_COMMUNICATION`,
`MIC` — and uses whichever the device allows:

| Your phone | Recording result |
|---|---|
| **Rooted** | Full both-sides recording works. |
| **Android 9 or older** | Usually works (both sides). |
| **Stock Android 10+ (Pixel, most Samsung, etc.)** | Call audio is blocked. You may capture only your own mic, silence, or nothing. |
| **Some OEM ROMs** (Tecno/Infinix/itel HiOS-XOS, Xiaomi MIUI, some Samsung regions) | Often already have a **built-in call recorder** in the stock dialer — frequently the easiest route, no app needed. |

If recording fails on your device, the app silently keeps logging calls — it
never crashes, and empty/broken audio files are discarded automatically.

**Legal note:** in many countries you must tell the other person that a call is
being recorded. Logging your own calls is fine; recording the other party may
require their consent depending on where you (and they) are. Check your local
rule before recording other people.

---

## Build & install

**Easiest — Android Studio:**
1. Install [Android Studio](https://developer.android.com/studio).
2. `File ▸ Open` → select this `CallTracker` folder.
3. Let it sync (it generates the Gradle wrapper and downloads dependencies).
4. Plug in your phone with **USB debugging** on (Settings ▸ Developer options),
   or create an emulator.
5. Press **Run ▸ Run 'app'**. The app installs and launches.

**Command line (if you already have Android SDK + JDK 17):**
```bash
cd CallTracker
gradle wrapper        # one-time, generates ./gradlew
./gradlew assembleDebug
# APK appears at app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug   # with a device/emulator connected
```

On first launch the app asks for Call Log, Phone State, Contacts, Microphone
and (Android 13+) Notification permissions. Grant them all for full function.

---

## How it works

- **`CallLogRepository`** reads the system call log via the `CallLog.Calls`
  content provider and stores new rows in a Room database (keyed on the system
  log id, so re-syncing never duplicates).
- **`CallLogSyncWorker`** re-syncs every 15 minutes in the background via
  WorkManager, so history stays current even if you never open the app.
- **`PhoneStateReceiver`** watches ring / answer / hang-up. When a call goes
  active it starts `CallRecordingService`; when it ends it stops it.
- **`CallRecordingService`** is a foreground service that tries to record and
  writes the file path back onto the matching call row.
- **`MainActivity`** (Compose) shows one card per number: contact name, call
  count, last call date and total talk time. Recordings live in
  `Android/data/com.example.calltracker/files/recordings/`.

## Project layout
```
CallTracker/
├─ settings.gradle.kts
├─ build.gradle.kts
└─ app/
   ├─ build.gradle.kts
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ res/values/ (strings, theme)
      └─ java/com/example/calltracker/
         ├─ MainActivity.kt
         ├─ data/        (Room DB, DAO, repository, sync worker)
         └─ recording/   (phone-state receiver + recording service)
```

## Ideas to extend
- Export the log to CSV / the Sheets you keep.
- Search and filter by number or date range.
- Play recordings in-app (tap a call row).
- Note that Google Play **rejects** apps that record calls; this is for your
  own sideloaded/personal build, not Play Store distribution.
