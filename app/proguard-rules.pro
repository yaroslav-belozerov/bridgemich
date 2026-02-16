# Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, InnerClasses, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class tech.tarakoshka.bridgemich.data.dtos.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.debug.AgentLoader { *; }
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# Coil
-keep class coil.** { *; }
