# PROGRESS

## Estado
- Fase actual: 1
- Última tarea cerrada: T1.6
- Tarea en curso: T1.7 Añadir movimiento manual (sin empezar)

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
- (T1.4, RESUELTO 2026-11-06) `TransactionDao.deleteCardAndNullify` borraba los movimientos en vez de poner `cardId = NULL`. Corregido con `nullifyCardId(cardId)` + `@Transaction`; `WalletViewModel.deleteCard` lo ejecuta dentro de `database.withTransaction`. Test instrumentado `transactionDaoDeleteCardAndNullify` pasa (14/14 instrumentados en NX779J).
- (T1.6) No hay `dataExtractionRules`/`fullBackupContent` en `res/xml` (lo pide §14); `allowBackup="false"` está puesto en el manifest. Se hará en T1.10.

## Mejoras menores pendientes (de revisiones)
- (T1.6) `CardPreview` decodifica la foto con muestreo y en `Dispatchers.IO`, pero sin caché en memoria entre recomposiciones; si se nota jank en la lista, añadir caché.
- (T1.6) El estado del formulario de tarjeta vive en `remember` (no `rememberSaveable`): al rotar se cierra la hoja. Aceptable por ahora; revisar en T4.9.
- (T1.6) El banco se guarda como texto visible (p. ej. "CaixaBank") porque §6 lo define así; si se traduce el nombre, las tarjetas antiguas dejan de reconocerlo.

## Notas para el próximo modelo
- (2026-11-02) T1.1 cerrada: pantalla vacía oscura compilando.
- (2026-11-02) T1.2 cerrada: tema neón (Color.kt, Type.kt, Theme.kt, Shape.kt) + componentes NeonCard, NeonButton, NeonChip, ScreenTitle y ShowcaseScreen con @Preview. Fuentes variables Orbitron y Space Grotesk aplicadas con `FontVariation`. Revisión cruzada con @code-reviewer aplicada (tipografía completa, ColorScheme completo, accesibilidad de chips).
- IMPORTANTE: los imports correctos de fuentes en Compose son `androidx.compose.ui.text.font.FontFamily` y `...text.font.Font` (no `androidx.compose.ui.text.FontFamily`).
- (2026-11-02) T1.3 cerrada: barra inferior neón (`NeonTabBar`, 4 pestañas + botón 56 dp), `AppNavHost`, `AppRoot` con Scaffold y `ModalBottomSheet` vacía, 4 pantallas en `feature/`. Selección de pestaña por `destination.hierarchy` para soportar subrutas futuras. Token `navBar = #0B1020` tomado de la sección 8 (no estaba en 5.1). Dependencia añadida: `material-icons-core` (BOM, sin versión explícita). Revisión cruzada con @code-reviewer aplicada.
- (2026-11-03) T1.4 cerrada: Base de datos Room completa (18 entidades, 8 DAOs, 7 TypeConverters, AppDatabase con exportSchema=true). Seed de 12 categorías (10 gastos + 2 ingresos) en strings.xml. Tests unitarios (Converters, DefaultCategories) e instrumentados (DAOs con Room in-memory) pasan. Schema exportado en `app/schemas/` (14 tablas). Hilt module provee DB y DAOs. Sin `fallbackToDestructiveMigration`. Importes en céntimos (Long), fechas epochDay/epochMillis.
- (2026-11-04) T1.5 cerrada: `Money.kt` en `core/money/` con formato `1.234,56 €` (Locale ES, miles `.`, decimal `,`, símbolo € con espacio), prefijo `−` (U+2212) para gastos y `+` para ingresos, parseo flexible (`23,45`, `23.45`, `1.234,56`, con/sin símbolo, con/sin signo). Tests unitarios: 34 tests cubriendo formato, signo, parseo, edge cases y round-trip. Sin `Float`/`Double` en persistencia.
- (2026-11-06) Publicado en `main`: T1.4, T1.5, AGENTS.md nuevo y `opencode.json` con agentes/permisos. `main` en `1e44540`.
- (2026-11-06) T1.6 implementada (rama `t1-6-tarjetas`): `WalletScreen` con lista (`CardPreview` 180 dp), hoja `CardFormSheet` (banco, variante, tipo, nombre, 6 diseños, vista previa en vivo), botón "Añadir tarjeta" con borde discontinuo, borrado con confirmación y `WalletViewModel` (save/delete + limpieza de la foto). Foto propia con `ActivityResultContracts.PickVisualMedia`, copiada a `noBackupFilesDir/card_photos`, reducida a 1280 px, orientación EXIF aplicada y borrado limitado a esa carpeta. Colores de tarjeta según tabla §5.5. 8 tests unitarios nuevos de diseños y bancos. Revisado por `@reasoning-expert` (mismo modelo no revisa): aplicados borrado seguro, validación de rutas, IO fuera del hilo principal y semántica `selectable` en los diseños. Pendiente: prueba en móvil y `@ui-reviewer` con capturas.
- (2026-11-06) T1.6 CERRADA: probada en RedMagic NX779J (Android 15) con datos limpios: alta (banco BBVA, crédito, nombre, diseño), persistencia tras `force-stop`, edición precargada, foto propia desde el selector del sistema (copia en `no_backup/card_photos`, "Quitar foto" borra el archivo), borrado con confirmación (Cancelar mantiene la hoja) y empty state. `assembleDebug`, 42 tests unitarios y 14 instrumentados en verde. Revisión visual con `@ui-reviewer`: aplicados `FlowRow` en los chips de banco (ya no se corta "Otro"), borde de 1 dp en las muestras de diseño, brillo de tarjeta 24 dp/alfa 0.55 y sombra de texto reforzada. Se descartaron dos observaciones que contradecían §5.1/§5.5 (banco duplicado a la derecha sin variante; texto oscuro en chips cuando la tabla fija tinte + texto neón).
- (2026-11-06) Aprendizaje de APIs en este proyecto: no existe `rememberPickVisualMedia` (usar `rememberLauncherForActivityResult` + `ActivityResultContracts.PickVisualMedia`); `Modifier.weight` NO se importa (es extensión de `RowScope`/`ColumnScope`); `shadow` es `androidx.compose.ui.draw.shadow`; `material-icons-core` no trae `Icons.Filled.CreditCard`; en material3 1.4 usar `OutlinedTextFieldDefaults.colors(...)` y `rememberModalBottomSheetState`.
- No tocar archivos fuera del alcance de la tarea. local.properties ya está en .gitignore.
