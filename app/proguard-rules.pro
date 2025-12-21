# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep SMS related classes
-keep class com.example.smsreply.** { *; }

# Keep notification and service classes
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# Keep JSON serialization for MessageTemplate
-keepclassmembers class com.example.smsreply.MessageTemplate {
    <fields>;
    <init>(...);
}

# Keep telephony manager classes
-keep class android.telephony.** { *; }

# Keep SMS manager classes  
-keep class android.telephony.SmsManager { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}