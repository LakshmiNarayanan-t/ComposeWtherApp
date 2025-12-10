# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

############################################
# General attributes & annotations
############################################


-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontwarn dagger.**
-dontwarn javax.inject.**
-keep class dagger.hilt.internal.** { *; }
-keep class ** extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keep @androidx.room.Dao public class * { *; }
-keep @androidx.room.Database public class * { *; }
-keep @androidx.room.Entity public class * { *; }

-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.weatherapp.data.local.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep interface com.example.weatherapp.data.remote.OpenWeatherApi
-keepclassmembers class com.example.weatherapp.data.remote.dto.** { *; }
-keep class com.example.weatherapp.domain.** { *; }
-keep class com.example.weatherapp.ui.** { *; }
-dontwarn androidx.lifecycle.**
-dontwarn androidx.activity.**
-dontwarn androidx.compose.**
-dontwarn androidx.savedstate.**
