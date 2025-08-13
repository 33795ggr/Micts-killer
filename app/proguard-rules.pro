# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep all classes in our package
-keep class com.lensshortcut.vivo.** { *; }

# Keep Google Lens related classes
-keep class com.google.android.googlequicksearchbox.** { *; }
-keep class com.google.ar.lens.** { *; }

# Keep Android system classes we use
-keep class android.app.** { *; }
-keep class android.content.** { *; }
-keep class android.provider.** { *; }

# Keep service and receiver classes
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep permission-related classes
-keep class android.permission.** { *; }

# WorkManager classes
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Material Design classes
-keep class com.google.android.material.** { *; }

# Keep ViewBinding classes
-keep class com.lensshortcut.vivo.databinding.** { *; }
