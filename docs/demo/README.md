# Demo video

[`paperstow-emulator-demo.mp4`](paperstow-emulator-demo.mp4) is a screen recording from the `Paperstow_API36` emulator after loading the sample trip. It walks home, a tag folder, preview, all documents, import, search, checklist, backup, and About.

This file is tracked in the repo and linked from the README.

Re-record:

```bash
# after the app is installed and the sample trip is loaded
adb shell screenrecord --time-limit 180 /sdcard/paperstow-demo.mp4
# walk the app, then:
adb shell kill -2 "$(adb shell pidof screenrecord)"
adb pull /sdcard/paperstow-demo.mp4 docs/demo/paperstow-emulator-demo.mp4
```
