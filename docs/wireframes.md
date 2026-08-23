# Document Manager — UI Wireframes

## Screen Map (Navigation Flow)

```
┌─────────────────────────────────────────────────────┐
│                   APP LAUNCH                         │
└──────────────────────┬──────────────────────────────┘
                       │
            ┌──────────▼──────────┐
            │  First Launch?       │
            └──────────┬──────────┘
                  YES  │  NO
        ┌──────────────┼──────────────┐
        ▼                             ▼
┌───────────────┐              ┌───────────────┐
│ CONSENT &     │              │  PIN ENTRY    │
│ ONBOARDING    │              │  SCREEN       │
└───────┬───────┘              └───────┬───────┘
        │                              │
        ▼                              ▼
┌─────────────────────────────────────────────────────┐
│                  MAIN SCREEN                         │
│  ┌─────────┐  ┌──────────┐  ┌──────────┐           │
│  │ IMPORT  │  │   DOCS   │  │  SEARCH  │           │
│  └────┬────┘  └────┬─────┘  └────┬─────┘           │
│       │             │             │                  │
│       ▼             ▼             ▼                  │
│  Import Flow   Doc List    Search Results            │
│       │             │             │                  │
│       │             ▼             │                  │
│       │        Doc Viewer ◄───────┘                  │
└──────────────────────────────────────────────────────┘
                       │
              ┌────────┼────────┐
              ▼        ▼        ▼
        ┌─────────┐┌──────┐┌──────────┐
        │SETTINGS ││ TAGS ││DIAGNOSTICS│
        └─────────┘└──────┘└──────────┘
```

---

## Screen 1: Consent & Onboarding (First Launch Only)

```
┌─────────────────────────────────┐
│                                 │
│     🌍 Welcome                  │
│     Document Manager     │
│                                 │
│  ┌─────────────────────────┐    │
│  │ Your documents will be   │    │
│  │ encrypted with AES-256   │    │
│  │ and protected by PIN.    │    │
│  │ All data stays on device.│    │
│  └─────────────────────────┘    │
│                                 │
│  Create your PIN                │
│  ┌─────────────────────────┐    │
│  │ PIN (4-8 digits)     ●●●●│   │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Confirm PIN          ●●●●│   │
│  └─────────────────────────┘    │
│                                 │
│  Recovery contacts              │
│  ┌─────────────────────────┐    │
│  │ Phone: +1234567890       │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Email: you@example.com   │    │
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ ⚠️ WARNING               │    │
│  │ PIN cannot be recovered. │    │
│  │ Write it down securely.  │    │
│  └─────────────────────────┘    │
│                                 │
│  ☐ I understand PIN is          │
│    irrecoverable                │
│  ☐ I agree to encrypted storage │
│                                 │
│  ┌─────────────────────────┐    │
│  │     Set Up & Continue    │    │
│  └─────────────────────────┘    │
│                                 │
└─────────────────────────────────┘
```

---

## Screen 2: PIN Entry (Every Launch)

```
┌─────────────────────────────────┐
│                                 │
│                                 │
│           🔒                    │
│                                 │
│        Enter PIN                │
│     Unlock your documents       │
│                                 │
│                                 │
│     ┌───────────────────┐       │
│     │ ● ● ● ●           │       │
│     └───────────────────┘       │
│                                 │
│     ┌───────────────────┐       │
│     │      Unlock        │       │
│     └───────────────────┘       │
│                                 │
│                                 │
│  [If locked: "Account locked    │
│   Try again in 4:32"]           │
│                                 │
│  [If error: "Incorrect PIN      │
│   2 attempts remaining"]        │
│                                 │
└─────────────────────────────────┘
```

---

## Screen 3: Main Screen (Primary — Document Focused)

> **Pull-to-refresh**: Swipe down shows LinearProgressIndicator. Documents reload automatically.

```
┌─────────────────────────────────┐
│  Document Manager    ⚙️  │
├─────────────────────────────────┤
│                                 │
│  ┌─Import─┐ ┌─Docs──┐ ┌Search┐ │
│  │  + Add  │ │📁 List│ │🔍 Find│ │
│  └─────────┘ └───────┘ └──────┘ │
│                                 │
│  ─── Recent Documents ────────  │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🌐 passport_scan.jpg    │    │
│  │    PASSPORT | 95%        │    │
│  │    passport, singapore   │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ ✈️ flight_ticket.pdf     │    │
│  │    TICKET | 87%          │    │
│  │    ticket, 2025          │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🏨 hotel_booking.png     │    │
│  │    HOTEL_BOOKING | 92%   │    │
│  │    accommodation, tokyo  │    │
│  └─────────────────────────┘    │
│                                 │
│  ─── Quick Stats ─────────────  │
│  📄 3 documents managed         │
│                                 │
│                           🐛    │
└─────────────────────────────────┘
```

---

## Screen 4: Import Document

```
┌─────────────────────────────────┐
│  ← Import Document              │
├─────────────────────────────────┤
│                                 │
│           📤                    │
│                                 │
│     Import Document             │
│     Select a PDF, JPG, PNG      │
│     or video file               │
│                                 │
│     ┌───────────────────┐       │
│     │    Choose File     │       │
│     └───────────────────┘       │
│                                 │
│     Back to Dashboard           │
│                                 │
├─────── After Import ────────────┤
│                                 │
│           ✅                    │
│     Document Imported!          │
│                                 │
│  ┌─────────────────────────┐    │
│  │ Type:       PASSPORT     │    │
│  │ Format:     JPG          │    │
│  │ File:       scan.jpg     │    │
│  │ Confidence: 95%          │    │
│  │ Tags:       passport,    │    │
│  │             singapore    │    │
│  │ ─── Extracted ────────── │    │
│  │ HOLDER_NAME: John Smith  │    │
│  │ ID_NUMBER:   A1234567    │    │
│  │ EXPIRY_DATE: 15/06/2030  │    │
│  └─────────────────────────┘    │
│                                 │
│     ┌───────────────────┐       │
│     │       Done         │       │
│     └───────────────────┘       │
│                                 │
└─────────────────────────────────┘
```

---


---

## Screen 4b: Subfolder Scan Dialog (Modal — After Folder Selection)

```
┌─────────────────────────────────┐
│                                 │
│     ┌───────────────────┐       │
│     │  📁 Scan Subfolders?│      │
│     │                   │       │
│     │ Should we scan     │       │
│     │ subfolders         │       │
│     │ recursively?       │       │
│     │                   │       │
│     │ If yes, the first- │       │
│     │ level subfolder    │       │
│     │ name will be added │       │
│     │ as a tag to each   │       │
│     │ imported document. │       │
│     │                   │       │
│     │ [No, root only]   │       │
│     │ [Yes, include      │       │
│     │  subfolders]       │       │
│     └───────────────────┘       │
│                                 │
└─────────────────────────────────┘
```

**Behavior:**
- Shown after user picks a folder via system folder picker
- "Yes, include subfolders" → recursive scan, each subfolder level becomes a separate tag
- "No, root only" → only files at root level imported (no subfolders scanned)
- Example: root selected = `Personal/`, file at `Personal/Travel/2025/passport.pdf`
  → tags "Travel" AND "2025" applied to passport.pdf
- Files at root level (directly inside selected folder) get no folder-derived tags

## Screen 5: Document List

```
┌─────────────────────────────────┐
│  ← My Documents         📁 🗑️  │
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🌐 passport_scan.jpg  🗑│    │
│  │    PASSPORT              │    │
│  │    passport, singapore   │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ ✈️ ticket.pdf          🗑│    │
│  │    TICKET                │    │
│  │    ticket, tokyo         │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🏨 hotel.png           🗑│    │
│  │    HOTEL_BOOKING         │    │
│  │    accommodation         │    │
│  └─────────────────────────┘    │
│                                 │
│  [Tap item → Doc Viewer]        │
│  [Tap 🗑️ → Recycle dialog]     │
│  [Tap toolbar 🗑️→ Recycle Bin]  │
│                                 │
└─────────────────────────────────┘
```

---

## Screen 6: Document Viewer

```
┌─────────────────────────────────┐
│  ← passport_scan.jpg    📤 ✏️  │
├─────────────────────────────────┤
│  ┌─────────────────────────┐    │
│  │                         │    │
│  │                         │    │
│  │   [DOCUMENT PREVIEW]    │    │
│  │   (maximized area)      │    │
│  │                         │    │
│  │   IMAGE: pinch-to-zoom  │    │
│  │   (async decode, large  │    │
│  │    images subsampled)   │    │
│  │                         │    │
│  │   PDF: LazyColumn of    │    │
│  │   pages (rendered on    │    │
│  │   demand, not all at    │    │
│  │   once)                 │    │
│  │                         │    │
│  │   VIDEO: icon + "Play"  │    │
│  │   (opens external)      │    │
│  │                         │    │
│  └─────────────────────────┘    │
│                                 │
│  ┌─Open External─┐ ┌─Chrome──┐ │
│  │  (all types)   │ │(PDF only)│ │
│  └────────────────┘ └─────────┘ │
│                                 │
│  ┌─────────────────────────┐    │
│  │  ▼ Show Properties       │    │
│  └─────────────────────────┘    │
│                                 │
│  (collapsed by default, tap     │
│   to expand:)                   │
│  ┌─────────────────────────┐    │
│  │  ▲ Hide Properties       │    │
│  ├─────────────────────────┤    │
│  │ Tags  [+]               │    │
│  │ ┌────────┐ ┌──────────┐ │    │
│  │ │passport✕│ │singapore✕│ │    │
│  │ └────────┘ └──────────┘ │    │
│  │                         │    │
│  │ Properties              │    │
│  │ Type:       PASSPORT    │    │
│  │ Format:     JPG         │    │
│  │ Confidence: 95%         │    │
│  │ Doc ID:     abc123...   │    │
│  │ Location:   10.01,77.48 │    │
│  │                         │    │
│  │ Extracted Fields        │    │
│  │ HOLDER NAME: John Smith │    │
│  │ ID NUMBER:   A1234567   │    │
│  │ EXPIRY DATE: 15/06/2030 │    │
│  └─────────────────────────┘    │
│                                 │
└─────────────────────────────────┘
```

**Key UX decisions:**
- **📤 Share button** in top bar uses Android ACTION_SEND share sheet
- **Content maximized**: Preview area uses `weight(1f)` to fill all available space
- **Properties collapsed by default**: Only visible when user taps "Show Properties"
- **Lazy rendering**: Large images decoded off-thread with subsampling (>4096px). PDF pages rendered on-demand via LazyColumn. App stays responsive even for 50+ page PDFs.
- **GPS import location**: Shown in Properties when available

---

## Screen 7: Search

```
┌─────────────────────────────────┐
│  ← Search Documents             │
├─────────────────────────────────┤
│                                 │
│  ┌─🔍───────────────────────┐   │
│  │ passport                  │   │
│  └───────────────────────────┘   │
│                                 │
│  ── Results ──────────────────  │
│                                 │
│  ┌─────────────────────────┐    │
│  │ passport_scan.jpg    →   │    │
│  │ PASSPORT | JPG           │    │
│  │ passport, singapore      │    │
│  └─────────────────────────┘    │
│                                 │
│  [Tap result → Doc Viewer]      │
│                                 │
├─── Travel Checklist Query ──────┤
│                                 │
│  "what documents for Singapore?"│
│                                 │
│  Travel Checklist:              │
│  ┌─────────────────────────┐    │
│  │ PASSPORT x4              │    │
│  │ Valid passport for each  │    │
│  ├─────────────────────────┤    │
│  │ VISA x4                  │    │
│  │ Visa for Singapore       │    │
│  ├─────────────────────────┤    │
│  │ TICKET x4                │    │
│  │ Flight ticket for each   │    │
│  └─────────────────────────┘    │
│                                 │
└─────────────────────────────────┘
```

---

## Screen 8: Settings (Secondary Page)

```
┌─────────────────────────────────┐
│  ← Settings                     │
├─────────────────────────────────┤
│                                 │
│  Security Alerts                │
│  ┌─────────────────────────┐    │
│  │ Recovery Phone           │    │
│  │ ┌─────────────────────┐ │    │
│  │ │ +1234567890          │ │    │
│  │ └─────────────────────┘ │    │
│  │                         │    │
│  │ Recovery Email           │    │
│  │ ┌─────────────────────┐ │    │
│  │ │ you@example.com     │ │    │
│  │ └─────────────────────┘ │    │
│  │                         │    │
│  │ SMS alerts on lockout   │    │
│  │                   [ON]  │    │
│  │                         │    │
│  │ ┌─────────────────────┐ │    │
│  │ │       Save           │ │    │
│  │ └─────────────────────┘ │    │
│  │                         │    │
│  │ Last alert: 10 Aug 14:30│    │
│  └─────────────────────────┘    │
│                                 │
│  About                          │
│  Document Manager v1.0   │
│  AES-256-GCM encryption         │
│  Argon2id PIN hashing           │
│                                 │
└─────────────────────────────────┘
```

---

## Screen 9: Tag Management (Secondary Page)

```
┌─────────────────────────────────┐
│  ← Manage Tags          🔤 [+] │
├─────────────────────────────────┤
│  3 tags (sorted alphabetically) │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🏷️ accommodation    [2] │    │
│  │    Auto-generated  ✏️ 🗑│    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🏷️ passport         [1] │    │
│  │    Auto-generated  ✏️ 🗑│    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🏷️ singapore        [2] │    │
│  │    Auto-generated  ✏️ 🗑│    │
│  └─────────────────────────┘    │
│                                 │
│  [✏️ = Rename dialog]           │
│  [🗑 = Safe delete with         │
│   "Used by N docs" warning]     │
│  [[+] = Create new tag dialog]  │
│  [🔤 = Toggle sort mode]        │
│                                 │
└─────────────────────────────────┘
```

---

## Screen 10: Diagnostics (Secondary Page)

```
┌─────────────────────────────────┐
│  ← Diagnostics                  │
├─────────────────────────────────┤
│                                 │
│  System Status                  │
│  ┌─────────────────────────┐    │
│  │ 🔋 Battery: 45%          │    │
│  │    Charging              │    │
│  ├─────────────────────────┤    │
│  │ 📍 GPS: 10.01, 77.48    │    │
│  │    Accuracy: 15m         │    │
│  ├─────────────────────────┤    │
│  │ 📶 WiFi | Connected      │    │
│  │    37Mbps down           │    │
│  └─────────────────────────┘    │
│                                 │
│  GPS Tracking                   │
│  ┌─────────────────────────┐    │
│  │ Background tracking [ON] │    │
│  │                         │    │
│  │ Interval:               │    │
│  │ [30s][1m][5m][15m][30m] │    │
│  └─────────────────────────┘    │
│                                 │
│  Debug Logs                     │
│  ┌─────────────────────────┐    │
│  │    View Debug Logs 🐛   │    │
│  └─────────────────────────┘    │
│                                 │
└─────────────────────────────────┘
```

---

## Screen 11: Debug Log Viewer (from Diagnostics)

```
┌─────────────────────────────────┐
│  Debug Logs          ⬇️ 🗑️ ✕   │
│  500 entries | /data/.../log    │
├─────────────────────────────────┤
│ 14:30:01.123 I [App] Started   │
│ 14:30:01.456 I [Session] start │
│ 14:30:02.001 D [Telemetry]     │
│   Memory: 6239MB/7571MB        │
│ 14:30:02.100 I [Telemetry]     │
│   Battery: 45% (charging)      │
│ 14:30:02.200 I [Telemetry]     │
│   GPS: lat=10.01, lng=77.48    │
│ 14:30:05.000 I [Import]        │
│   Pipeline started: PNG 253KB  │
│ 14:30:05.500 I [OCR]           │
│   Extract: type=PASSPORT 95%   │
│ 14:30:06.000 I [Import]        │
│   Pipeline COMPLETE in 1200ms  │
│ 14:30:10.000 E [CRASH]         │
│   NullPointerException at...   │
│                                 │
│  [Dark background, monospace]   │
│  [Color coded: D=blue I=green  │
│   W=yellow E=red]              │
│  [Auto-scrolls to bottom]      │
└─────────────────────────────────┘
```

---

## Screen 12: Duplicate Detection Dialog (Modal)

```
┌─────────────────────────────────┐
│                                 │
│     ┌───────────────────┐       │
│     │  📋 Duplicate      │       │
│     │     Document       │       │
│     │                   │       │
│     │ "passport_scan.jpg"│       │
│     │                   │       │
│     │ Type: PASSPORT     │       │
│     │ Imported: 10 Aug   │       │
│     │                   │       │
│     │ Replace existing   │       │
│     │ or cancel?         │       │
│     │                   │       │
│     │ [Cancel] [Replace] │       │
│     └───────────────────┘       │
│                                 │
└─────────────────────────────────┘
```

---

## Screen 13: Recycle Bin

```
┌─────────────────────────────────┐
│  ← Recycle Bin          🗑️All  │
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🌐 old_passport.jpg     │    │
│  │    PASSPORT         ♻️ 🗑│    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ ✈️ expired_ticket.pdf    │    │
│  │    TICKET           ♻️ 🗑│    │
│  └─────────────────────────┘    │
│                                 │
│  [♻️ = Restore to docs]         │
│  [🗑 = Delete forever]          │
│  [🗑️All = Empty entire bin]     │
│                                 │
└─────────────────────────────────┘
```

---

## Color Palette

| Element | Color | Hex |
|---------|-------|-----|
| Primary (headers, buttons) | Blue | #1565C0 |
| Success (imported, connected) | Green | #4CAF50 |
| Warning (lockout, battery low) | Orange | #FFC107 |
| Error (failed, delete) | Red | #F44336 |
| Background | Light Gray | #F5F5F5 |
| Cards | White | #FFFFFF |
| Text primary | Dark | #212121 |
| Text secondary | Gray | #757575 |
| Tag chips | Light Blue | #E3F2FD |

## Typography

| Element | Size | Weight |
|---------|------|--------|
| Screen title | 22sp | Bold |
| Section header | 16sp | SemiBold |
| Card title | 14sp | Medium |
| Body text | 13sp | Normal |
| Caption/meta | 11-12sp | Normal |
| Button text | 14sp | Medium |

## Key Interactions

1. **Main → Import**: Tap "Import" button → file picker → processing → result (GPS captured)
2. **Main → Docs**: Tap "Docs" → document list → tap item → viewer
3. **Main → Search**: Tap "Search" → type query → results → tap result → viewer
4. **Viewer → Share**: Tap 📤 in top bar → Android share sheet (ACTION_SEND)
5. **Viewer → External**: Tap "Open External" → system chooser (ACTION_VIEW)
6. **Viewer → Properties**: Tap "Show Properties" at bottom → expands tags + metadata
7. **Viewer → Tags**: In expanded properties, tap "+" → dialog → add tag chip
8. **Doc List → Delete**: Tap 🗑️ → confirmation → moves to recycle bin
9. **Main → ⚙️**: Tap gear icon → Settings/Tags/Diagnostics pages
10. **Main → Pull-to-refresh**: Swipe down → progress indicator → documents reload
11. **Settings → GPS Tracking**: Toggle in experimental features gates background GPS

---

## Screen 14: Disclaimer (First Launch — Before Everything)

```
┌─────────────────────────────────┐
│                                 │
│     Terms of Use                │
│                                 │
│  ┌─────────────────────────┐    │
│  │ ⚠️ No Warranty           │    │
│  │ App is "as-is". No      │    │
│  │ warranties or support.   │    │
│  │ Verify before relying.   │    │
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🔒 Your Data             │    │
│  │ Sharing/backup can move  │    │
│  │ docs off your phone.     │    │
│  │ Use caution.             │    │
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🛡️ Privacy               │    │
│  │ No intent to collect     │    │
│  │ your content. All local. │    │
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 📊 Telemetry (Optional)  │    │
│  │ Anonymous usage data to  │    │
│  │ improve the app. No      │    │
│  │ content collected.       │    │
│  └─────────────────────────┘    │
│                                 │
│  ☐ I consent to anonymous       │
│    usage telemetry              │
│    (change anytime in Settings) │
│                                 │
│  ┌─────────────────────────┐    │
│  │  I Understand & Continue │    │
│  └─────────────────────────┘    │
│                                 │
└─────────────────────────────────┘
```

**Navigation**: Disclaimer → Consent/Onboarding → Main Screen

---

---

## Screen 15: Settings (Updated with Experimental Toggles)

```
┌─────────────────────────────────┐
│  ← Settings                     │
├─────────────────────────────────┤
│                                 │
│  Security Alerts                │
│  ┌─────────────────────────┐    │
│  │ Recovery Phone           │    │
│  │ [+91 ▼] [9876543210  ]  │    │
│  │                         │    │
│  │ Recovery Email           │    │
│  │ [you@example.com     ]  │    │
│  │                         │    │
│  │ SMS on lockout    [ON]  │    │
│  │ [     Save     ]        │    │
│  └─────────────────────────┘    │
│                                 │
│  Experimental Features          │
│  ┌─────────────────────────┐    │
│  │ Enable Experimental [OFF]│    │
│  │                         │    │
│  │ (when ON, shows:)       │    │
│  │ Google Drive      [OFF] │    │
│  │ S3 Storage        [OFF] │    │
│  │ Backup & Restore  [OFF] │    │
│  │ GPS Tracking      [OFF] │    │
│  └─────────────────────────┘    │
│                                 │
│  About                          │
│  Document Manager v1.0   │
│  AES-256-GCM (Android KeyStore) │
│  ML Kit Text Recognition        │
│  License: Apache 2.0            │
│                                 │
└─────────────────────────────────┘
```

---

## Screen 16: Backup & Restore (Feature-Flag Gated)

```
┌─────────────────────────────────┐
│  ← Backup & Restore            │
├─────────────────────────────────┤
│                                 │
│  Backup                         │
│  Encrypted files → ZIP. No      │
│  plaintext leaves device.       │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 📁 Local Folder          │    │
│  │    Save to phone storage │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ ☁️ Google Drive           │    │
│  │    (if flag enabled)     │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🗄️ S3 Compatible         │    │
│  │    (if flag enabled)     │    │
│  └─────────────────────────┘    │
│                                 │
│  ─── Restore ───────────────    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ ♻️ Restore from Backup    │    │
│  │    Pick a backup ZIP     │    │
│  └─────────────────────────┘    │
│                                 │
└─────────────────────────────────┘
```

---
