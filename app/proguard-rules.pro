# Glintbox R8 rules
# https://developer.android.com/build/shrink-code

# Type-safe navigation routes are @Serializable objects/classes.
-keep @kotlinx.serialization.Serializable class com.glintbox.app.ui.navigation.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.glintbox.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager instantiates workers reflectively.
-keep class com.glintbox.app.work.** extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
