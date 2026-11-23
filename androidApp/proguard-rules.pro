# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation @kotlinx.serialization.Serializable class * {
    <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# GitLive Firebase Firestore / Auth reflection
-keepattributes Signature
-keepattributes *Annotation*
