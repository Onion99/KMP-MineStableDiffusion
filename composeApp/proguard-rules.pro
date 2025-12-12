# ================= Aggressive Optimizations =================
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses


# ================= Keep Rules for Dependencies =================

# --- OkHttp & Okio ---
# Ignore warnings for optional platform-specific classes that OkHttp probes for at runtime. [11]
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep all essential classes for OkHttp and its dependency Okio. [3, 4]
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *; }

# --- Other libraries from your original file ---
-keep class com.sun.jna.** { *; }
-keep class org.freedesktop.dbus.** { *; }
-keep class io.github.alexzhirkevich.** { *; }


# ================= Kotlin & Serialization Rules =================

# Keep annotations, which are critical for many libraries including serialization. [5]
-keepattributes Signature, *Annotation*

# Keep classes and their serializers for kotlinx.serialization.
# This fixes the "but not the descriptor class" notes.
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <methods>;
}
-keep class *$$serializer { *; }

# Keep kotlinx-datetime, as it's referenced by other kept libraries (like okio.fakefilesystem).
-keep class kotlinx.datetime.** { *; }


# ================= Suppress other specific warnings if they persist =================

# Ktor might use SLF4J for logging.
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Your log shows warnings for android.* classes. This happens because you are building
# a multiplatform module, but ProGuard runs in a context where Android SDK classes may not be available.
# These are safe to ignore as they will be present on the actual Android device.
-dontwarn android.security.**
-dontwarn android.util.**
-dontwarn android.os.**
-dontwarn android.net.**



-keep class com.sun.jna.** { *; }
-keep class org.freedesktop.dbus.** { *; }
-keep class io.github.alexzhirkevich.** { *; }
-keep class okio.fakefilesystem.** { *; }
-keep class okio.** { *; }
-keep class okhttp3.** { *; }