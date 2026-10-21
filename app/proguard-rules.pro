# =======================================================================
# Locus Math Olympiad App - Release ProGuard / R8 Minification Rules
# =======================================================================

# Preserve line numbers and source files for clean crash traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions

# -----------------------------------------------------------------------
# 1. Protect Androidx @Keep Annotations
# -----------------------------------------------------------------------
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# -----------------------------------------------------------------------
# 2. KaTeX WebView & JavaScript Interface Bridges
# -----------------------------------------------------------------------
# Protect methods annotated with @JavascriptInterface from obfuscation or stripping
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Explicitly keep KaTeX view and JavaScript height calculation bridge
-keep class com.locus.olympiad.ui.math.KatexWebView { *; }
-keep class com.locus.olympiad.ui.math.MathResizeBridge { *; }
-keepclassmembers class com.locus.olympiad.ui.math.MathResizeBridge {
    public void setHeight(float);
    public <methods>;
}

# -----------------------------------------------------------------------
# 3. Gson Models & Serialization
# -----------------------------------------------------------------------
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# Keep all domain/data models so reflection-based serialization/deserialization never breaks
-keep class com.locus.olympiad.data.model.** { *; }
-keepclassmembers class com.locus.olympiad.data.model.** { *; }
-keep class com.locus.olympiad.network.** { *; }
-keepclassmembers class com.locus.olympiad.network.** { *; }
-keep class com.google.gson.** { *; }

# -----------------------------------------------------------------------
# 4. Room Database Entities, DAOs, and Converters
# -----------------------------------------------------------------------
-keep class androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase { *; }

-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

-keep @androidx.room.Dao interface * { *; }
-keepclassmembers interface * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Update <methods>;
    @androidx.room.Delete <methods>;
}

-keep class com.locus.olympiad.data.LocusDatabase { *; }
-keep class com.locus.olympiad.data.dao.** { *; }
-keepclassmembers class com.locus.olympiad.data.dao.** { *; }
-keep class com.locus.olympiad.data.converter.** { *; }

-dontwarn androidx.room.paging.**

# -----------------------------------------------------------------------
# 5. WorkManager & Background Sync Workers
# -----------------------------------------------------------------------
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class com.locus.olympiad.worker.** { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# -----------------------------------------------------------------------
# 6. Dagger Hilt Dependency Injection & ViewModels
# -----------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep class * extends androidx.fragment.app.Fragment { *; }
-keep class * extends androidx.appcompat.app.AppCompatActivity { *; }
-keep class * extends android.app.Application { *; }
-keep class com.locus.olympiad.di.** { *; }

# -----------------------------------------------------------------------
# 7. Retrofit & OkHttp
# -----------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**

# -----------------------------------------------------------------------
# 8. Cloud Firestore & Play Services
# -----------------------------------------------------------------------
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
