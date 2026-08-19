# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for readable stack traces in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# AndroidX / CameraX / Media3 / ML Kit / Compose all ship their own
# consumer-proguard-rules bundled in their AARs, so no library-specific
# keep rules should be needed here under normal R8 shrinking.
