# VoiceKey ProGuard/R8 rules.
# Bisher keine besonderen Keep-Regeln noetig (kein Reflection/Serialization-Code in der Mini-App).
#
# TODO bei Vosk/JNI-Integration (Phase 2) — siehe bugs/android-build/r8.md §E2:
#   -keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
#   -keep class org.vosk.** { *; }
#   -keep class org.kaldi.** { *; }
