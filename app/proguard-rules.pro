# Room
-keep class androidx.room.paging.** { *; }

# Gson (if used internally by libraries)
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# ThreeTenABP
-keep class org.threeten.bp.** { *; }
-keep class org.threeten.bp.zone.TzdbZoneRulesProvider { *; }

# AndroidX Core
-keep class androidx.core.app.CoreComponentFactory { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep our data entities and DAOs to avoid obfuscation issues with Room reflection
-keep class com.spendwise.ai.data.entity.** { *; }
-keep class com.spendwise.ai.data.dao.** { *; }
-keep class com.spendwise.ai.data.AppDatabase_Impl { *; }

# Keep widget provider
-keep class com.spendwise.ai.widget.** { *; }
