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

# proguard-rules.pro (최소 시작점)

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

############################################
# Hilt / Dagger
############################################
# Hilt가 생성한 컴포넌트/엔트리포인트 관련(대부분 기본으로 되지만, 릴리즈에서 리플렉션 이슈 시 안전장치)
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }

# 애노테이션 유지(필요 시)
-keepattributes *Annotation*

############################################
# Room
############################################
# Room은 compile-time 생성이라 보통 keep 불필요.
# 다만, 룸 메타/애노테이션 기반 이슈나 리플렉션 기반 유틸이 섞인 경우 대비(안전장치)
-keep class androidx.room.** { *; }
-keep class **_Impl { *; }   # Room이 생성하는 *_Impl(DAO/DB 구현체)
-keep class **_Dao { *; }    # 혹시 생성물 네이밍 케이스 대비(없어도 무방)

############################################
# WorkManager
############################################
# Worker는 리플렉션으로 인스턴스 생성될 수 있어 릴리즈에서 종종 깨짐
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# (선택) HiltWorker를 쓰면 이쪽도 도움이 됩니다
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class androidx.hilt.work.** { *; }

############################################
# Glance AppWidget
############################################
# Glance 위젯 리시버/서비스/프로바이더가 매니페스트로 로딩되므로 보통 괜찮지만,
# 릴리즈에서 위젯이 안 뜨는 경우가 있어 안전장치로 유지
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

############################################
# Compose / Navigation
############################################
# Compose/Navigation은 보통 추가 keep 불필요.
# 하지만 "release에서만 화면 전환/리플렉션 관련 크래시"가 나면 아래를 고려
-keep class androidx.navigation.** { *; }

############################################
# DataStore
############################################
# Preferences DataStore는 보통 문제 없음. (Proto DataStore를 쓸 땐 protobuf 규칙 필요)
-keep class androidx.datastore.** { *; }

# Apache POI - Android release build R8 warnings
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.print.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**