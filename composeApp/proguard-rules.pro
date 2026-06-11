# ATHENA release R8 rules.
#
# Keep this file intentionally small. Most Android/Firebase/Play/Ktor/Compose
# dependencies ship their own consumer ProGuard rules; project-specific rules
# should only be added here when a release build or manual validation proves
# that ATHENA code is reached reflectively or by string name outside manifest
# processing.

# Preserve metadata used by Kotlin/JVM libraries that inspect generic type
# signatures or runtime annotations. This does not keep application classes from
# being shrunk/obfuscated, but avoids stripping schema/annotation information
# needed by serialization and selected SDK internals.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
