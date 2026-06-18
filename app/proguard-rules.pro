# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

############################################
# Debugging stack traces
############################################

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Annotation / Kotlin / Reflection metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

############################################
# Hilt / Dagger
############################################

# Hilt/Dagger는 대부분 기본 규칙으로 처리되지만,
# 릴리즈에서 리플렉션/생성 코드 관련 이슈가 있을 때를 대비한 안전장치
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }

############################################
# Room
############################################

# Room은 compile-time 생성이라 보통 keep 불필요하지만,
# 릴리즈에서 생성 구현체 관련 이슈를 피하기 위한 안전장치
-keep class androidx.room.** { *; }
-keep class **_Impl { *; }
-keep class **_Dao { *; }

############################################
# WorkManager
############################################

# Worker는 리플렉션으로 인스턴스 생성될 수 있어 릴리즈에서 깨질 수 있음
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# HiltWorker 사용 대비
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class androidx.hilt.work.** { *; }

############################################
# Glance AppWidget
############################################

-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

############################################
# Compose / Navigation
############################################

# Compose/Navigation은 보통 추가 keep 불필요하지만 안전장치
-keep class androidx.navigation.** { *; }

############################################
# DataStore
############################################

-keep class androidx.datastore.** { *; }

############################################
# Apache POI / XMLBeans / OOXML - Release R8
############################################

# Apache POI core
-keep class org.apache.poi.** { *; }
-keepnames class org.apache.poi.**

# XMLBeans
-keep class org.apache.xmlbeans.** { *; }
-keepnames class org.apache.xmlbeans.**

# OOXML generated schemas
-keep class org.openxmlformats.schemas.** { *; }
-keepnames class org.openxmlformats.schemas.**

-keep class com.microsoft.schemas.** { *; }
-keepnames class com.microsoft.schemas.**

-keep class schemaorg_apache_xmlbeans.** { *; }
-keepnames class schemaorg_apache_xmlbeans.**

# XML signature / drawing schemas sometimes used by POI internals
-keep class org.w3.x2000.x09.xmldsig.** { *; }
-keepnames class org.w3.x2000.x09.xmldsig.**

-keep class org.etsi.uri.x01903.** { *; }
-keepnames class org.etsi.uri.x01903.**

############################################
# Apache POI dependencies
############################################

# Commons libraries used by POI
-keep class org.apache.commons.** { *; }
-keepnames class org.apache.commons.**

# Logging libraries that POI may access indirectly/reflection-style
-keep class org.apache.logging.** { *; }
-keepnames class org.apache.logging.**

-keep class org.slf4j.** { *; }
-keepnames class org.slf4j.**

############################################
# Keep constructors for reflection
############################################

-keepclassmembers class org.apache.poi.** {
    public <init>(...);
}

-keepclassmembers class org.apache.xmlbeans.** {
    public <init>(...);
}

-keepclassmembers class org.openxmlformats.schemas.** {
    public <init>(...);
}

-keepclassmembers class com.microsoft.schemas.** {
    public <init>(...);
}

-keepclassmembers class schemaorg_apache_xmlbeans.** {
    public <init>(...);
}

-keepclassmembers class org.apache.commons.** {
    public <init>(...);
}

-keepclassmembers class org.apache.logging.** {
    public <init>(...);
}

-keepclassmembers class org.slf4j.** {
    public <init>(...);
}

############################################
# Apache POI optional dependencies - dontwarn
############################################

# XMLBeans optional JavaParser config parser
-dontwarn com.github.javaparser.**

# XMLBeans optional Maven / Ant tooling
-dontwarn org.apache.maven.**
-dontwarn org.apache.tools.ant.**
-dontwarn com.sun.org.apache.xml.internal.resolver.**

# Optional XML stream / XPath / Saxon
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**

# Optional SVG / PDF / Drawing / Desktop rendering
-dontwarn org.apache.batik.**
-dontwarn org.apache.pdfbox.**
-dontwarn de.rototor.pdfbox.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.traversal.**

# Optional XML digital signature / crypto
-dontwarn javax.xml.crypto.**
-dontwarn javax.xml.crypto.dsig.**
-dontwarn javax.xml.crypto.dsig.dom.**
-dontwarn javax.xml.crypto.dsig.keyinfo.**
-dontwarn javax.xml.crypto.dsig.spec.**
-dontwarn org.apache.jcp.xml.dsig.internal.dom.**
-dontwarn org.apache.xml.security.**
-dontwarn org.ietf.jgss.**

# Android에 없는 Java desktop APIs
# autoSizeColumn(), PDF/SVG/PPT 렌더링 같은 기능은 사용하면 안 됨
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.imageio.**
-dontwarn javax.print.**

# Optional logging / crypto dependencies
-dontwarn org.apache.logging.log4j.**
-dontwarn org.slf4j.**
-dontwarn org.bouncycastle.**

# Office schema optional references
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.w3.x2000.x09.xmldsig.**
-dontwarn org.etsi.uri.x01903.**

############################################
# Apache Commons Compress optional codecs
############################################

-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn org.objectweb.asm.**
-dontwarn org.tukaani.xz.**