# PROGRESS

## Estado
- Fase actual: 1
- Última tarea cerrada: T1.4
- Tarea en curso: T1.5 Money (por iniciar)

## Decisiones tomadas
- (2026-11-02) Paquete: com.mipatrimonio.app
- (2026-11-02) AGP 9.5.0 + Gradle 9.6.0 + JDK 17; Kotlin integrado (built-in Kotlin), sin plugin kotlin-android; KGP embebido 2.2.10
- (2026-11-02) compileSdk 37 (requerido por Compose BOM 2026.09.00 / Compose 1.12), targetSdk 36, minSdk 26
- (2026-11-02) Plataforma android-37.2 instalada en el SDK local (no venía por defecto; solo android-36)
- (2026-11-02) Modelo de implementación: MiMo-V2.6-Flash (asume rol de implementador; revisión cruzada pendiente con otro agente)

## Dependencias (nombres exactos, versiones en libs.versions.toml)
- AGP 9.5.0, Gradle 9.6.0, Kotlin 2.2.10 (embebido en AGP), KSP 2.3.13, Hilt 2.60.1, Room 2.8.5, DataStore 1.3.0, Navigation Compose 2.10.2, Compose BOM 2026.09.00, material-icons-core (BOM), core-ktx 1.19.1, activity-compose 1.13.0, lifecycle 2.11.0, coroutines 1.11.0, serialization 1.12.0, biometric 1.1.0

## Problemas conocidos
- KGP 2.4.21 declarado por buildscript falló (NoClassDefFoundError BaseVariant, API eliminada en AGP 9): se revirtió al KGP embebido 2.2.10
- compileSdk 36 insuficiente: Compose 1.12 exige 37; se subió a 37 y se instaló la plataforma android-37.2

## Mejoras menores pendientes (de revisiones)
- (ninguna)

## Notas para el próximo modelo
- (2026-11-02) T1.1 cerrada: pantalla vacía oscura compilando.
- (2026-11-02) T1.2 cerrada: tema neón (Color.kt, Type.kt, Theme.kt, Shape.kt) + componentes NeonCard, NeonButton, NeonChip, ScreenTitle y ShowcaseScreen con @Preview. Fuentes variables Orbitron y Space Grotesk aplicadas con `FontVariation`. Revisión cruzada con @code-reviewer aplicada (tipografía completa, ColorScheme completo, accesibilidad de chips).
- IMPORTANTE: los imports correctos de fuentes en Compose son `androidx.compose.ui.text.font.FontFamily` y `...text.font.Font` (no `androidx.compose.ui.text.FontFamily`).
- (2026-11-02) T1.3 cerrada: barra inferior neón (`NeonTabBar`, 4 pestañas + botón 56 dp), `AppNavHost`, `AppRoot` con Scaffold y `ModalBottomSheet` vacía, 4 pantallas en `feature/`. Selección de pestaña por `destination.hierarchy` para soportar subrutas futuras. Token `navBar = #0B1020` tomado de la sección 8 (no estaba en 5.1). Dependencia añadida: `material-icons-core` (BOM, sin versión explícita). Revisión cruzada con @code-reviewer aplicada.
- (2026-11-03) T1.4 cerrada: Base de datos Room completa (18 entidades, 8 DAOs, 7 TypeConverters, AppDatabase con exportSchema=true). Seed de 12 categorías (10 gastos + 2 ingresos) en strings.xml. Tests unitarios (Converters, DefaultCategories) e instrumentados (DAOs con Room in-memory) pasan. Schema exportado en `app/schemas/` (14 tablas). Hilt module provee DB y DAOs. Sin `fallbackToDestructiveMigration`. Importes en céntimos (Long), fechas epochDay/epochMillis.
- No tocar archivos fuera del alcance de la tarea. local.properties ya está en .gitignore.
