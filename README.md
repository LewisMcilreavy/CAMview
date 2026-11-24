# Sugar Lock

Shows your CamAPS FX glucose on the Android lock screen and in the status bar.

## How it works
CamAPS FX has no public API, but it shows your glucose in its own notification.
Sugar Lock reads that notification (like xDrip+ Companion mode does) and posts a
silent, always-on notification that's visible on the lock screen. It only reads;
it never touches CamAPS, your pump or your sensor. Nothing leaves your phone.

## Build and install
1. Install Android Studio and open this folder (File > Open).
2. Let it sync (it downloads the Android SDK and Gradle the first time).
3. Turn on Developer options and USB debugging on your phone, plug it in, press Run ▶.
   Or use Build > Build App Bundle(s)/APK(s) > Build APK(s) and copy the APK to your phone.

## First run
1. Tap **Allow notification access** and turn on Sugar Lock.
2. Tap **Allow this app to post notifications**.
3. In your phone's lock-screen settings, set notifications to **show all content**.
4. On some phones (Samsung, Xiaomi, etc.) set Sugar Lock's battery use to **Unrestricted**
   so Android doesn't stop it.

The reading appears the next time CamAPS updates (every ~5 min). After 15 minutes
with no new reading it shows "OLD READING" and a ? in the status bar.

## If the number is wrong or missing
CamAPS's notification wording isn't documented. The app shows the last raw
CamAPS notification text at the bottom of its screen. If the value isn't picked
up, adjust the patterns in `GlucoseParser.kt`.

## Safety
Display only. CamAPS FX remains the source of truth for readings, alarms and insulin.
