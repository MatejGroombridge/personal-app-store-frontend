# Keep kotlinx.serialization classes (they're accessed via reflection/generated code)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class dev.matejgroombridge.store.**$$serializer { *; }
-keepclassmembers class dev.matejgroombridge.store.** {
    *** Companion;
}
-keepclasseswithmembers class dev.matejgroombridge.store.** {
    kotlinx.serialization.KSerializer serializer(...);
}
