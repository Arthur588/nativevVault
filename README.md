# Vault Offline Android App

This repository contains the source code for a fully offline, native Android
application for securely storing and viewing personal photos and videos.  The
application is designed to operate without any network access and places a
strong emphasis on security, performance and user experience.  Key features
include:

* **Offline operation** – no internet access required; all data and logic run
  locally on the device.
* **Secure storage** – imported media files are copied into the app’s
  internal storage and encrypted with AES‑GCM.  A SQLCipher‐backed Room
  database stores metadata such as file names, MIME types and the randomised
  playback order.
* **Password protection** – on first launch a password is set; a derived
  encryption key is used to open the encrypted database.  Subsequent logins
  verify the password without storing it in plain text.
* **Controlled viewing** – each day at most seven items become available.
  Once viewed, items cannot be revisited within the same day and
  navigation is strictly one‑way.  The order is randomised and persists
  across app restarts.
* **Hilt/Compose architecture** – the app is built with Kotlin, Jetpack
  Compose, Room and Hilt.  A clean MVVM pattern separates the UI from
  business logic.

## Building the App

The project uses Gradle and can be built on any machine with the Android
SDK installed.  To build the release APK locally:

```bash
cd vault-app
./gradlew assembleRelease
```

The continuous integration pipeline is defined in `.github/workflows/android-ci.yml` and
builds the release APK on every push using GitHub Actions.

## Running

1. Clone the repository and open the `vault-app` module in Android Studio.
2. Run the `app` configuration on an emulator or physical device.
3. On first launch you will be prompted to set a password.  Remember this
   password: it is required to unlock the vault.  If you forget it you
   cannot access your files.
4. Use the **Import Files** button on the home screen to pick photos or
   videos from your device.  These will be copied and encrypted inside the
   vault.  After importing you can choose to delete the originals.
5. Tap **View Today** to see up to seven items selected for the current day.
   Once you start viewing, the navigation is one‑way and you cannot revisit
   previous items.  At 07:00 local time a new set of items becomes
   available (unless you have not yet viewed the current set, in which case
   they carry over).

## Security Considerations

* **Encryption key** – the key is derived from the user’s password with
  PBKDF2 and a per‑device salt.  The salt and a hash of the key are
  stored using EncryptedSharedPreferences.  Do not share your password; it
  cannot be recovered.
* **Database encryption** – SQLCipher is used to encrypt the Room database.
  The same key that encrypts the media files encrypts the database.
* **One‑way navigation** – back navigation is disabled in the viewer to
  prevent revisiting earlier items.  This is enforced in both UI and
  repository logic.

For more detailed design rationale and security analysis see the attached
design document.