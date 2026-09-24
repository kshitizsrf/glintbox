# Glintbox R8 rules
# Most libraries (Compose, Coil, Media3, DataStore, Navigation) ship their own consumer rules.
# Only project-specific and known-gap rules live here.

# ---------------------------------------------------------------- crash reports
# Keep file/line info so Play Console stack traces can be de-obfuscated with mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------ persisted enums
# Enum names are written to DataStore (theme, palette, sources, etc.). If R8 renamed them,
# settings would reset on every update.
-keepclassmembers enum com.glintbox.app.data.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# -------------------------------------------------- type-safe navigation routes
# Navigation builds route strings from the serializer's class name.
-keepattributes *Annotation*, InnerClasses, Signature
-keep @kotlinx.serialization.Serializable class com.glintbox.app.ui.navigation.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.glintbox.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.glintbox.app.**$$serializer { *; }

# ------------------------------------------------------ WorkManager + Room
# WorkManager's Room database is created reflectively through its no-arg constructor.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# Workers are instantiated reflectively by class name.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --------------------------------------------------------------------- misc
# Silence warnings for optional annotations some libraries reference.
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
