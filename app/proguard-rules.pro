# Advanced R8 Optimization for SMYS Food Count

# Optimization Flags
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses ''

# Data Models - CRITICAL: Keep all fields for Firestore reflection
-keep class com.satwik.oodapplication.data.model.** { *; }

# Firebase & Google Play Services
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn com.google.firebase.**

# Hilt & Dagger
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# Remove Logcat Debugging - Strip debug and info logs to reduce DEX size
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimize Kotlin Intrinsics
-keepclassmembernames class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
