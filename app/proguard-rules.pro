# Reglas ProGuard/R8 de MiPatrimonio (T4.9).
# La app usa minificación opcional en release; estas reglas conservan APIs accedidas
# por reflexión/generación (Room, Hilt, WorkManager/Workers, Glance) y evitan roturas.

# Room: entidades, DAOs y base de datos (esquemas/constructores usados por reflexión).
-keep class com.mipatrimonio.app.core.database.** { *; }

# WorkManager: workers instanciados por nombre/reflexión (incl. HiltWorkerFactory).
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker
-keep class com.mipatrimonio.app.feature.reminders.**Worker { *; }
-keep class com.mipatrimonio.app.feature.recurrings.**Worker { *; }
-keep class com.mipatrimonio.app.feature.documents.**Worker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Glance widget (providers referenciados desde manifest/recursos del sistema).
-keep class com.mipatrimonio.app.feature.widget.** { *; }

# Hilt/Dagger: conservar metadatos de inyección generada.
-keepclasseswithmembers class * { @dagger.hilt.* <methods>; }
-keepclasseswithmembers class * { @javax.inject.* <methods>; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Modelos/DTOs serializados manualmente (JSON backup) - mantener nombres de campos usados.
-keepclassmembers class com.mipatrimonio.app.core.database.entity.** { *; }

# ML Kit / OCR: mantener clases usadas por la librería.
-dontwarn com.google.mlkit.**

# Atributos útiles para stack traces legibles en release.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,Exceptions,InnerClasses,EnclosingMethod

