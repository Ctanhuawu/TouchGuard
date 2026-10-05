# Compose and Miuix rules
-keepattributes *Annotation*,InnerClasses,Signature
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-keep class com.ccwait.touchguard.model.** { *; }
-keep class com.ccwait.touchguard.strategy.** { *; }
-keep class com.ccwait.touchguard.service.** { *; }
-keep class com.ccwait.touchguard.receiver.** { *; }
-keep class com.ccwait.touchguard.notification.** { *; }
-keep class com.ccwait.touchguard.AppPreferences { *; }
-keep class top.yukonga.miuix.** { *; }

-dontwarn java.lang.invoke.**
-dontwarn top.yukonga.miuix.**
