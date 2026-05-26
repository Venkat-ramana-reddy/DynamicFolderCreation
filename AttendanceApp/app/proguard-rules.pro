# Keep kotlinx.serialization for Supabase DTOs
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities and DAOs
-keep class com.factoryattendance.data.local.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Supabase
-keep class io.github.jan.** { *; }
-dontwarn io.github.jan.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# DataStore
-keep class androidx.datastore.** { *; }
