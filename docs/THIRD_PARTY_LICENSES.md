# Third-party licenses

Paperstow source is Apache License 2.0. See [LICENSE](../LICENSE) and [NOTICE](../NOTICE).

Nothing below copylefts the app. Apache 2.0 and MIT are compatible. Google ML Kit / Play services are proprietary SDKs used at runtime; they stay under Google’s terms. JUnit’s EPL-2.0 applies only to tests.

## Production (in the APK / AAB)

| Component | Version pin | License | Notes |
|-----------|-------------|---------|--------|
| Paperstow | 1.1.0 | Apache-2.0 | This repository |
| Android Gradle Plugin | 8.13.2 | Apache-2.0 | Build |
| Kotlin / Compose compiler | 2.2.21 | Apache-2.0 | |
| KSP | 2.2.21-2.0.5 | Apache-2.0 | Build |
| AndroidX Core KTX | 1.18.0 | Apache-2.0 | Last line on AGP 8 / compileSdk 36 |
| AndroidX Activity | 1.13.0 | Apache-2.0 | |
| AndroidX Lifecycle | 2.10.0 | Apache-2.0 | Last line on AGP 8 / compileSdk 36 |
| AndroidX Navigation | 2.9.8 | Apache-2.0 | Last line on AGP 8 / compileSdk 36 |
| AndroidX Hilt Navigation | 1.3.0 | Apache-2.0 | Last line on AGP 8 / compileSdk 36 |
| Compose BOM | 2026.06.01 | Apache-2.0 | Compose UI 1.11.x; 2026.08+ needs AGP 9 |
| AndroidX Room | 2.8.5 | Apache-2.0 | |
| AndroidX Biometric | 1.1.0 | Apache-2.0 | |
| AndroidX DocumentFile | 1.1.0 | Apache-2.0 | |
| AndroidX Security Crypto | 1.1.0 | Apache-2.0 | |
| Dagger Hilt | 2.58 | Apache-2.0 | |
| kotlinx.coroutines | 1.11.0 | Apache-2.0 | |
| Zip4j | 2.11.6 | Apache-2.0 | Optional AES ZIP archive |
| Gson | 2.14.0 | Apache-2.0 | Forced over Tink’s 2.8.9 |
| Tink Android | 1.8.0 (transitive) | Apache-2.0 | Via security-crypto |
| JSpecify | 1.0.0 (transitive) | Apache-2.0 | |
| Bouncy Castle `bcprov-jdk18on` | 1.86 | [Bouncy Castle Licence](https://www.bouncycastle.org/licence.html) (MIT-style) | Argon2id / HKDF |
| ML Kit Text Recognition | 16.0.1 | [ML Kit Terms](https://developers.google.com/ml-kit/terms) | On-device OCR |
| Play services ML Kit Document Scanner | 16.0.0 | [ML Kit Terms](https://developers.google.com/ml-kit/terms) | On-device scan UI |
| Google Play services (base, basement, tasks, …) | transitive | [Android SDK License](https://developer.android.com/studio/terms) | Pulled by ML Kit |

Packaging excludes duplicate `META-INF/NOTICE`, `AL2.0`, and `LGPL2.1` files from AARs so the APK merges. That does **not** mean Paperstow is LGPL. Attribution for Apache components is this file and [NOTICE](../NOTICE).

## Test / CI only (not shipped)

| Component | Version pin | License |
|-----------|-------------|---------|
| JUnit BOM / Jupiter / Platform | 5.14.4 | EPL-2.0 |
| Kotest | 6.2.5 | Apache-2.0 |
| MockK | 1.14.11 | Apache-2.0 |
| AndroidX Test JUnit / Espresso | 1.3.0 / 3.7.0 | Apache-2.0 |
| Compose UI Test | via BOM | Apache-2.0 |

## Why the project license stays Apache 2.0

- Direct open-source libraries are Apache-2.0 or MIT-style.
- MIT (Bouncy Castle) is compatible with Apache-2.0; the copyright and permission notice are reproduced in [NOTICE](../NOTICE).
- ML Kit and Play services are binary SDKs with Google terms. They do not relicense Paperstow source.
- EPL-2.0 JUnit is test-only and is not linked into release builds.

## Bouncy Castle Licence (verbatim)

Copyright (c) 2000-2025 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
