# Travel Document Manager ProGuard Rules

# Keep BouncyCastle provider
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep SQLCipher native libraries

# Keep Room entities
-keep class com.app.traveldocs.data.local.** { *; }

# Keep Hilt generated components
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**
