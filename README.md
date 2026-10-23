# WfcWatch — Wi-Fi Calling monitor for Android (Samsung-friendly)

Shows whether Wi-Fi Calling is working and takes you straight to its settings in one tap.

- **Home-screen widget** — green **ON** / red **OFF**, tap to open Wi-Fi Calling settings.
- **Quick Settings tile** — same thing in the swipe-down panel.
- **Background watcher** — checks every 30 s and whenever Wi-Fi changes; posts a loud
  alert the moment Wi-Fi Calling drops (and every 15 min while it stays off).
  Tapping the alert opens the settings.

## How it detects Wi-Fi Calling

Android doesn't let normal apps read the Wi-Fi Calling switch directly. Instead the app
reads which network your phone's **voice service** is registered on. When Wi-Fi Calling is
working, that's `IWLAN` (voice over Wi-Fi). In a zero-signal area, anything else means
incoming calls won't reach you — so the app reports what actually matters: *can I get calls
right now?* It also says when the cause is simply that Wi-Fi is disconnected.

The main screen shows the raw value ("Voice network: IWLAN …") so you can confirm it
matches what you see in Settings.

## Building the APK

### Option A — no software to install (GitHub Actions)
1. Create a free GitHub account and a new **private** repository.
2. Upload this whole folder's contents (drag-and-drop works on github.com — include the
   `.github` folder).
3. Open the **Actions** tab → "Build APK" runs automatically (or press *Run workflow*).
4. When it finishes (~5 min), open the run and download **WfcWatch-apk** under Artifacts.
5. Unzip, copy the `.apk` to your phone, open it, and allow "Install unknown apps" when asked.

### Option B — Android Studio
Open the folder in Android Studio (Ladybug or newer), let it sync, then
**Build → Build APK(s)**, or plug in the phone and press Run.

## First-time setup on the phone
1. Open **WfcWatch**, allow **Phone** and **Notifications** permissions.
2. Tap **"Stop Samsung from putting this app to sleep"** → Allow.
   Also recommended: Settings → Apps → WfcWatch → Battery → **Unrestricted**.
3. Add the widget: long-press home screen → Widgets → WfcWatch.
4. Add the tile: swipe down twice → ✎ edit → drag **Wi-Fi Calling** into the panel.

## Where the tap takes you
It tries, in order: Android's Wi-Fi Calling screen → Phone app **Call settings** (on Samsung,
Wi-Fi calling is listed there) → Connections settings. If it lands on Call settings, tap
**Wi-Fi calling** once more.
