# How to get a ready-to-install APK without Android Studio

The project includes a GitHub build recipe (`.github/workflows/build-apk.yml`).
GitHub will compile the APK for you in the cloud — free, nothing to install on
your computer or phone except a browser.

## Steps (about 10 minutes, mostly waiting)

1. **Make a free GitHub account** at https://github.com if you don't have one.

2. **Create a new repository**: click **+** (top right) → **New repository**.
   Name it `CallTracker`, keep it **Private** if you like, click
   **Create repository**.

3. **Upload the project files**: on the new empty repo page click
   **uploading an existing file**. Unzip `CallTracker.zip` on your computer
   first, then drag **everything inside** the `CallTracker` folder (the
   `app` folder, `build.gradle.kts`, the `.github` folder, etc.) into the
   browser. Click **Commit changes**.
   - Make sure the hidden `.github` folder gets uploaded — it holds the build
     recipe. If drag-and-drop skips it, that's the one folder to double-check.

4. **Wait for the build**: go to the **Actions** tab. A job called
   "Build APK" starts automatically. Green tick = done (takes ~3–5 min the
   first time).

5. **Download the APK**: click the finished run → scroll to **Artifacts** →
   download **CallTracker-debug-apk**. Unzip it to get `app-debug.apk`.

6. **Install on your phone**: copy `app-debug.apk` to your phone (or download
   it there directly), tap it, and allow **"Install from unknown sources"**
   when prompted. Open the app and grant the permissions.

## If the Actions tab says nothing ran
Open the **Actions** tab → if it asks, click **"I understand my workflows,
enable them"** → then **Build APK** on the left → **Run workflow**.

---

### Note on the APK type
This produces a **debug** APK — perfect for installing on your own phone. It is
not signed for the Play Store (and Google doesn't allow call-recording apps
there anyway), so this personal sideloaded build is exactly the right form.
