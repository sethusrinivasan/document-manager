# Travel Document Manager ProGuard Rules

# Keep BouncyCastle provider
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep Room entities and database classes
-keep class com.app.traveldocs.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }

# Keep Hilt generated components
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Jetpack Compose - essential for proper functioning
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Lifecycle components
-keep class androidx.lifecycle.** { *; }
-keepclasseswithmembers class * { @androidx.lifecycle.* <methods>; }

# Navigation components
-keep class androidx.navigation.** { *; }

# ML Kit (on-device OCR)
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Google Play Services & API Client
-keep class com.google.android.gms.** { *; }
-keep class com.google.api.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.api.**

# Apache HttpClient (for Google Drive API)
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**

# Retrofit/OkHttp (for Drive/S3 backend)
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# AndroidX Core
-keep class androidx.core.** { *; }
-keep class androidx.biometric.** { *; }
-keep class androidx.security.** { *; }

# Keep native libraries
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep annotations used by Hilt and other libraries
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# Keep generated code for Hilt
-keep class dagger.hilt.internal.** { *; }

# Keep auto-generated code for Navigation
-keep class com.app.traveldocs.di.** { *; }
