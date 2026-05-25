-keep class com.factoryattendance.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Kotlin serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
# Supabase / Ktor
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**
