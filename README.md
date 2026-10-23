# AppVin

A personal wine cellar app for Android (built for a Pixel 6). Take a photo of a label: Gemini reads it, searches Google for the drinking window and current value, and adds the wine to the cellar. A sommelier chat knows every bottle in the cellar and recommends what to open.

Native Kotlin, Jetpack Compose, Material 3 (follows the phone's wallpaper colours). Data stays on the phone in a Room database (included in Android's automatic backup). Gemini runs on Google Cloud's Vertex AI / Agent Platform through Firebase AI Logic, so there is no server to run.

## Features (v0.1)

- **Scan a label**: camera or gallery → Gemini extracts producer, wine, vintage, colour, region, grapes, size, alcohol → Gemini + Google Search fills in drinking window, peak year, value per bottle (EUR), description, food pairings and serving tips, with source links
- **Cellar**: search, filter by ready-to-drink / colour / location, sort by name, vintage, drink-by date or value
- **Wine page**: drinking-window bar, value, bottles and where each one is stored, refresh info from the web, edit, delete
- **Bottles**: add more, move between locations, remove as Drunk (with 1–5 star rating), Gifted or Sold
- **Home**: bottle count, estimated cellar value, ready-to-drink shelf, recent activity
- **Sommelier chat**: text chat with Gemini, grounded in Google Search, aware of the whole cellar
- **Cellars & fridges**: named storage locations with bottle counts
- **Past bottles**: history with ratings and notes
- **Export**: the whole cellar as CSV

## One-time Google Cloud / Firebase setup

1. Open the [Firebase console](https://console.firebase.google.com/) → **Create a project** → pick your **existing Google Cloud project** (the one with the credits).
2. Make sure the project is on the **Blaze** (pay-as-you-go) plan, linked to the billing account that holds the credits.
3. In Firebase: **Build → AI Logic → Get started** → choose **Agent Platform Gemini API** (formerly Vertex AI Gemini API). This enables the required APIs.
4. **Project settings → Your apps → Add app → Android**, package name `com.appvin`. Download `google-services.json` and put it at `app/google-services.json` (it's git-ignored).
5. Optional but recommended: in Google Cloud → APIs & Services → Credentials, restrict the Android API key to the Firebase AI Logic API. You can also set a budget alert in Billing.

The model (`gemini-3.5-flash`) and currency (`EUR`) are set at the top of `app/src/main/java/com/appvin/ai/Sommelier.kt`.

## Build

Needs JDK 17 and the Android SDK (platform 36). On the ARM build server, x86 build tools run through qemu user emulation.

```bash
./gradlew assembleRelease
```

The APK is at `app/build/outputs/apk/release/app-release.apk`. It is signed with the build machine's debug key so later versions install over earlier ones; keep building on the same machine (`~/.android/debug.keystore`).

## Install on the Pixel

Copy the APK to the phone (Google Drive, email, USB), open it, and allow "Install unknown apps" for the app you opened it from.

```bash
./gradlew testReleaseUnitTest
```

runs the unit tests.
