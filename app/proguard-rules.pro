# Regras do Proguard para manter modelos, controladores nano-spring e reflexão
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class io.github.matheuscruzsouza.ticketmaster.data.model.** { *; }
-keep class io.github.matheuscruzsouza.ticketmaster.web.** { *; }
-keep @com.github.matheuscruzsouza.nanospring.annotation.* class * { *; }
-keepclassmembers class * {
    @com.github.matheuscruzsouza.nanospring.annotation.* <methods>;
    @com.github.matheuscruzsouza.nanospring.annotation.* <fields>;
}
