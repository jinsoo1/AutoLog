# ============================================================
# AutoLog R8 규칙
#
# 원칙: "확실히 리플렉션으로 로드되는 것"만 keep 한다.
# Hilt/Room/WorkManager/Compose/Glance/DataStore 등 androidx·구글 라이브러리는
# 각자 consumer proguard 규칙을 배포하므로 여기서 다시 keep 하지 않는다.
# (과거의 광범위한 keep 이 축소/난독화율 17%의 원인이었음 — Play Console 경고)
# ============================================================

############################################
# 크래시 리포트 가독성 (스택트레이스 라인 번호 유지)
############################################

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

############################################
# Apache POI (엑셀 내보내기)
#
# 앱은 XSSFWorkbook 을 직접 생성하므로 POI 본체는 R8 이 정적 참조로
# 추적 가능하다. 다만 XmlBeans 는 .xsb 메타데이터에 "클래스 이름 문자열"로
# 스키마 타입을 기록해두고 런타임 리플렉션으로 로드하므로,
# XmlBeans 와 생성된 스키마 패키지는 keep 이 반드시 필요하다.
# (릴리즈 엑셀 오류(1.0.5)의 원인이 이 부분이었음 — 이 블록은 줄이지 말 것)
############################################

-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.w3.x2000.x09.xmldsig.** { *; }
-keep class org.etsi.uri.x01903.** { *; }

# POI가 ServiceLoader/리플렉션으로 찾는 진입점(안전장치, 범위 최소)
-keepclassmembers class * extends org.apache.poi.ooxml.POIXMLFactory { public <init>(...); }

############################################
# Apache POI - 안드로이드에 없는 선택적 의존성 경고 무시
############################################

# XMLBeans optional 도구류
-dontwarn com.github.javaparser.**
-dontwarn org.apache.maven.**
-dontwarn org.apache.tools.ant.**
-dontwarn com.sun.org.apache.xml.internal.resolver.**

# Optional XML stream / XPath / Saxon
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**

# Optional SVG / PDF / Drawing / Desktop 렌더링
-dontwarn org.apache.batik.**
-dontwarn org.apache.pdfbox.**
-dontwarn de.rototor.pdfbox.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.traversal.**

# Optional XML 서명 / 암호화
-dontwarn javax.xml.crypto.**
-dontwarn javax.xml.crypto.dsig.**
-dontwarn javax.xml.crypto.dsig.dom.**
-dontwarn javax.xml.crypto.dsig.keyinfo.**
-dontwarn javax.xml.crypto.dsig.spec.**
-dontwarn org.apache.jcp.xml.dsig.internal.dom.**
-dontwarn org.apache.xml.security.**
-dontwarn org.ietf.jgss.**

# Android에 없는 Java desktop API
# (autoSizeColumn(), PDF/SVG/PPT 렌더링 기능은 사용하면 안 됨)
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.imageio.**
-dontwarn javax.print.**

# Optional 로깅 / 암호화
-dontwarn org.apache.logging.log4j.**
-dontwarn org.slf4j.**
-dontwarn org.bouncycastle.**

# Office 스키마 optional 참조
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.w3.x2000.x09.xmldsig.**
-dontwarn org.etsi.uri.x01903.**

############################################
# Apache Commons Compress optional 코덱
############################################

-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn org.objectweb.asm.**
-dontwarn org.tukaani.xz.**
