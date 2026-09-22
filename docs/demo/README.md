# Local demo capture

`paperstow-emulator-demo.mp4` is a screen recording from the `Paperstow_API36` emulator after loading the sample trip. Walks home, a tag folder, preview, all documents, import, search, checklist, backup, and About.

Recorded so you can upload it (YouTube, Play listing, etc.). It is not published from this repo.

Re-record:

```bash
# after the app is installed and the sample trip is loaded
adb shell screenrecord --time-limit 180 /sdcard/paperstow-demo.mp4
# walk the app, then:
adb shell kill -2 "$(adb shell pidof screenrecord)"
adb pull /sdcard/paperstow-demo.mp4 docs/demo/paperstow-emulator-demo.mp4
```
