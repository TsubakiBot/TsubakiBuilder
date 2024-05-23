# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class ani.dantotsu.others.webview.* {
   public *;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

-keep class com.google.android.material.R$drawable { *; }

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,allowoptimization class ani.dantotsu.**
-keep,allowoptimization class bit.himitsu.**
-keep,allowoptimization class eu.kanade.**
-keep,allowoptimization class tachiyomi.**

##---------------Begin: proguard configuration for aniyomi  ----------
# Keep common dependencies used in extensions
-keep,allowoptimization class androidx.preference.** { public protected *; }
-keep,allowoptimization class android.content.** { *; }
-keep,allowoptimization class uy.kohesive.injekt.** { public protected *; }
-keep,allowoptimization class android.test.base.** { *; }
-keep,allowoptimization class kotlin.** { public protected *; }
-keep,allowoptimization class kotlinx.coroutines.** { public protected *; }
-keep,allowoptimization class kotlinx.serialization.** { public protected *; }
-keep,allowoptimization class kotlin.time.** { public protected *; }
-keep,allowoptimization class okhttp3.** { public protected *; }
-keep,allowoptimization class okio.** { public protected *; }
-keep,allowoptimization class org.jsoup.** { public protected *; }
-keep,allowoptimization class rx.** { public protected *; }
-keep,allowoptimization class app.cash.quickjs.** { public protected *; }
-keep,allowoptimization class uy.kohesive.injekt.** { public protected *; }
-keep,allowoptimization class is.xyz.mpv.** { public protected *; }
-keep,allowoptimization class com.arthenica.** { public protected *; }

# From extensions-lib
-keep,allowoptimization class eu.kanade.tachiyomi.network.interceptor.RateLimitInterceptorKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.interceptor.SpecificHostRateLimitInterceptorKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.NetworkHelper { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.OkHttpExtensionsKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.network.RequestsKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.AppInfo { public protected *; }
##---------------End: proguard configuration for aniyomi  ----------

-keep,allowoptimization class ani.dantotsu.parsers.** { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.animesource.** { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.source.** { public protected *; }

##---------------Begin: proguard configuration for RxJava 1.x  ----------
-dontwarn sun.misc.**

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

-dontnote rx.internal.util.PlatformDependent
##---------------End: proguard configuration for RxJava 1.x  ----------

##---------------Begin: proguard configuration for kotlinx.serialization  ----------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.** # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class eu.kanade.**$$serializer { *; }
-keepclassmembers class eu.kanade.** {
    *** Companion;
}
-keepclasseswithmembers class eu.kanade.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class ani.dantotsu.**$$serializer { *; }
-keepclassmembers class ani.dantotsu.** {
    *** Companion;
}
-keepclasseswithmembers class ani.dantotsu.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class bit.himitsu.**$$serializer { *; }
-keepclassmembers class bit.himitsu.** {
    *** Companion;
}
-keepclasseswithmembers class bit.himitsu.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class tachiyomi.**$$serializer { *; }
-keepclassmembers class tachiyomi.** {
    *** Companion;
}
-keepclasseswithmembers class tachiyomi.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.** {
    <methods>;
}
##---------------End: proguard configuration for kotlinx.serialization  ----------

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class eu.kanade.** { <fields>; }
-keep class ani.dantotsu.** { <fields>; }
-keep class bit.himitsu.** { <fields>; }
-keep class tachiyomi.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

##---------------End: proguard configuration for Gson  ----------

-keepattributes EnclosingMethod
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.nodes.Document { *; }

-dontwarn org.jspecify.annotations.NullMarked
-dontwarn com.oracle.svm.core.annotate.Delete
-dontwarn com.oracle.svm.core.annotate.Substitute
-dontwarn com.oracle.svm.core.annotate.TargetClass
-dontwarn java.lang.Module
-dontwarn org.graalvm.nativeimage.hosted.Feature$BeforeAnalysisAccess
-dontwarn org.graalvm.nativeimage.hosted.Feature
-dontwarn org.graalvm.nativeimage.hosted.RuntimeResourceAccess

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault