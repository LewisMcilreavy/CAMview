## What's new

- **Fix: insulin doses no longer show as glucose.** After a bolus, CamAPS can post a notification like "2.4 U", which Sugar Lock was reading as a glucose of 2.4. Sugar Lock now:
  - ignores CamAPS notifications about insulin, carbs or the pump
  - ignores numbers with units like U, g, %, h or min
  - rejects any value that jumps further than glucose physically can since the last reading

## Install

1. Tap **SugarLock.apk** below to download it.
2. Open it and tap **Install**. It updates over your current version.

New to Sugar Lock? Follow the [setup guide](https://github.com/LewisMcilreavy/Sugar-Lock#2-first-time-setup).

> Sugar Lock is a display only. CamAPS FX stays in charge of alarms and insulin. Always check CamAPS before acting on a number.
