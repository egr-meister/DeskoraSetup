# Deskora Setup ProGuard / R8 rules

# Keep Kotlinx Serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable model classes and their companion serializers.
-keep,includedescriptorclasses class com.deskora.setup.model.** { *; }
-keepclassmembers class com.deskora.setup.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.deskora.setup.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlin metadata used by serialization reflection.
-keep class kotlin.Metadata { *; }

# DataStore uses Kotlin coroutines; keep coroutine internals used via reflection.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose keeps its own rules via the AndroidX consumer files; nothing extra required.
