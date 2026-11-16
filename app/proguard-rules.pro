# R8 / ProGuard rules for Teb Al-Hajayzah (طب الحجايزة)
# Keeps Room entities/DAOs, Coroutines, DataStore, and serialization-safe structures intact.

-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database, Entities, and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

# App models & configuration classes
-keep class com.example.config.** { *; }
-keep class com.example.data.local.** { *; }
-keep class com.example.data.medications.** { *; }
-keep class com.example.auth.** { *; }

# Keep BuildConfig fields
-keep class com.example.BuildConfig { *; }

# Suppress optional missing platform warnings when shrinking
-dontwarn org.slf4j.**
-dontwarn sun.misc.Unsafe
-dontwarn javax.annotation.**
