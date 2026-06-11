# VoiceKey ProGuard/R8 rules.
# Vosk + JNA: native Bruecke ruft Java-Klassen per JNI/Reflection beim Namen —
# Klassen erhalten, sonst crasht NUR der Release-Build (bugs/android-build/r8.md §E2).
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
-keep class org.vosk.** { *; }
-keep class org.kaldi.** { *; }
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure { public *; }
-dontwarn java.awt.**
