# ProGuard — premium plugin (per-repo). Same file ships in each standalone plugin repository.
#
# Do not use -dontpreverify: Kotlin coroutines need valid StackMapTable for JVM bytecode
# verification (otherwise java.lang.VerifyError at runtime).

-dontshrink
-dontoptimize
-ignorewarnings

-keepnames class * extends com.panomc.platform.api.PanoPlugin

-keep class * extends com.panomc.platform.api.PanoPlugin {
    public <init>();
}

-keep @org.pf4j.Extension class * {
    public <init>(...);
    *;
}

-keepnames @com.panomc.platform.annotation.Endpoint class *

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions,StackMapTable,BootstrapMethods,NestMembers,NestHost

-keepclassmembers class * {
    @org.springframework.context.annotation.** *;
    @org.springframework.beans.factory.annotation.** *;
    @org.springframework.scheduling.annotation.** *;
    @javax.inject.** *;
    @jakarta.inject.** *;
    @org.pf4j.** *;
}

-keep class org.springframework.** { *; }
-keep interface org.springframework.** { *; }
-keep class com.panomc.platform.api.event.** { *; }

-keep class * extends co.statu.rule.database.DBEntity { *; }
-keep class * extends co.statu.rule.database.Dao { *; }

-keep class com.panomc.platform.license.** { *; }
-keep class com.panomc.plugins.license.PluginLicenseClient { public *; }
-keep class com.panomc.plugins.license.LicenseGuard { *; }
-keep class com.panomc.plugins.license.PluginBuildConstants { *; }

-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

-dontwarn org.slf4j.**
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
-dontwarn jakarta.annotation.**
-dontwarn io.vertx.**
-dontwarn com.auth0.jwt.**
-dontwarn org.checkerframework.**
-dontwarn org.apache.logging.log4j.**
