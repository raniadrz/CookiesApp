# kotlinx.serialization — keep generated serializers for seed DTOs and navigation routes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.crumbcodex.crk.**$$serializer { *; }
-keepclassmembers class com.crumbcodex.crk.** { *** Companion; }
-keepclasseswithmembers class com.crumbcodex.crk.** { kotlinx.serialization.KSerializer serializer(...); }
