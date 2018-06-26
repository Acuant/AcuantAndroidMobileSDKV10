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

-optimizationpasses 5
-dontpreverify
-ignorewarnings
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keep class * {
    native <methods>;
}
-keep class org.ejbca.** { *; }
-keepclassmembers class org.ejbca.** { *; }
-keep class net.sf.scuba.** { *; }
-keepclassmembers class net.sf.scuba.** { *; }
-keep class org.jmrtd.** { *; }
-keepclassmembers class org.jmrtd.** { *; }
-keep class com.acuant.sampleapp.** { *; }
-keepclassmembers class com.acuant.sampleapp.** { *; }

