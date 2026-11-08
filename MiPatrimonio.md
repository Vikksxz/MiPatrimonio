# MiPatrimonio — Guía de desarrollo

> Documento maestro del proyecto. Está escrito para que un agente de IA (por ejemplo, un modelo gratuito dentro de OpenCode) y el desarrollador (George) construyan la app juntos, fase a fase.
> Idioma de la app y de los textos de usuario: **español (España)**. Identificadores de código, nombres de clases y de paquetes: **inglés**.

---

## 0. Instrucciones para el agente (LEER PRIMERO)

1. **Trabaja por tareas pequeñas.** Usa la sección 18 (Plan de tareas). Haz UNA tarea cada vez, y no avances a la siguiente sin que compile y el criterio de aceptación se cumpla.
2. **Compila siempre tras cada tarea** (`./gradlew assembleDebug`). Si falla, arregla el error antes de seguir. Si hay tests, ejecútalos (`./gradlew testDebugUnitTest`).
3. **No inventes APIs ni nombres de librerías.** Si no estás seguro de un artefacto de Maven, de su versión o de una función, búscalo en la documentación oficial (developer.android.com, kotlinlang.org, documentación de la librería) o pregunta a George. Es preferible preguntar a escribir código que no compila.
4. **Versiones:** usa las últimas versiones **estables** de cada librería. Declara todas en `gradle/libs.versions.toml` (version catalog). No uses versiones alpha o beta salvo que sea imprescindible. Este documento NO fija versiones a propósito, porque cambian.
5. **Respeta la arquitectura de la sección 4 y el modelo de datos de la sección 6.** No cambies nombres de entidades ni campos sin avisar.
6. **Dinero siempre en céntimos (`Long`).** Nunca `Float` ni `Double` para importes.
7. **Textos de usuario siempre en `res/values/strings.xml`** (español). Nada de cadenas literales en los Composables, salvo ejemplos de preview.
8. **No guardes nunca números de tarjeta, CVV ni datos bancarios.** Ni en la base de datos, ni en logs, ni en copias de seguridad. La app NO se conecta a bancos.
9. **No añadas funcionalidades que no estén en este documento** sin preguntar. Si crees que falta algo, propónlo al final de tu respuesta.
10. **Código que escribas:** completo y compilable, con imports. No dejes `TODO()` en el código entregado, salvo que la tarea lo indique.
11. **Explica brevemente** qué archivos has creado o modificado al terminar cada tarea.
12. **Pantalla de referencia visual:** hay un mockup interactivo (https://claude.ai/artifact/7YLccDjarYRYspzsoLHbxF). Si no puedes abrirlo, usa las especificaciones de diseño de la sección 5 y las pantallas de la sección 8, que lo describen por completo.

---

## 1. Resumen del proyecto

**MiPatrimonio** es una app Android de finanzas personales para centralizar gastos e ingresos, **sin conexión bancaria**. Todo se registra a mano o escaneando tickets. Los datos viven **solo en el móvil** (con copia de seguridad cifrada opcional en Google Drive).

**Usuario objetivo:** George (uso personal) y, quizás, algunos amigos. No es una app comercial ni se publicará de forma masiva.

**Propuesta de valor:**
- Tarjetas **sin datos sensibles**: solo banco, variante, tipo y nombre. La tarjeta se muestra con un diseño visual personalizable.
- Registro de gastos por **escaneo OCR de tickets** o manual; gastos en metálico, con tarjeta y suscripciones; ingresos (nómina y otros).
- **Dashboard** dinámico con colores neón distintivos por tipo.
- **Almacén de documentos** (nóminas, facturas, garantías) en PDF, con recepción directa desde CamScanner mediante "Compartir".
- **Planes de ahorro** con importe objetivo editable, cuota mensual, estimación de fecha de cumplimiento y alertas.
- **Presupuestos** por categoría con alertas.
- Estética **futurista, oscura y neón**.

---

## 2. Alcance por fases

### Fase 1 — Núcleo
- [ ] Proyecto base, tema neón, navegación inferior (Inicio, Movimientos, Ahorro, Cartera + botón central +).
- [ ] Base de datos Room con todas las entidades de la sección 6.
- [ ] Categorías por defecto.
- [ ] Tarjetas: alta, edición, eliminación, personalización (diseños neón y foto propia).
- [ ] Movimientos: alta manual (metálico, tarjeta, suscripción, ingreso), lista con filtros, edición y borrado.
- [ ] Dashboard Inicio: saldo disponible, ingresos, gastos, barra por tipo, leyenda, últimos movimientos.
- [ ] Bloqueo con huella/PIN.

### Fase 2 — Captura
- [ ] Escaneo de ticket con OCR (importe, fecha, comercio) y pantalla de revisión.
- [ ] Recepción de PDF/imagen compartidos desde CamScanner (u otras apps), con pregunta "¿Ticket o nómina?" y clasificación automática sugerida.
- [ ] Almacén de documentos cifrado (nóminas, facturas, garantías, otros) con visor de PDF.
- [ ] Adjuntar el ticket al gasto.
- [ ] Etiquetas y notas en movimientos.

### Fase 3 — Planificación
- [ ] Planes de ahorro (varios, con prioridad), cuota mensual, estimación y alertas.
- [ ] Presupuestos por categoría con alertas al 80 % y al 100 %.
- [ ] Gastos recurrentes y suscripciones (generación automática, aviso previo de renovación, coste anual).
- [ ] Categorías: gráficos por categoría y comparativa mes a mes.
- [ ] Proyección de gasto a fin de mes.
- [ ] Gastos fijos vs. variables y "libre al mes".
- [ ] Recordatorio nocturno para apuntar el metálico.

### Fase 4 — Extras
- [ ] Widget de pantalla de inicio (Jetpack Glance).
- [ ] Copia de seguridad cifrada en Google Drive (manual y semanal) y restauración.
- [ ] Exportación a CSV y PDF.
- [ ] Informe mensual automático.
- [ ] Multimoneda (tipos de cambio manuales).
- [ ] Gastos compartidos (reparto y saldos).
- [ ] Dashboard personalizable (orden y visibilidad de bloques).
- [ ] Documentos con fecha de vencimiento y aviso.

**Descartado explícitamente:** iOS (por ahora), modo autónomo/IVA/Hacienda, conexión con bancos.

---

## 3. Stack técnico

| Área | Elección |
|---|---|
| Lenguaje | Kotlin (última estable) |
| UI | Jetpack Compose + Material 3 (con tema propio neón) |
| Arquitectura | MVVM + casos de uso (`domain/usecase`) + repositorios |
| Asincronía | Kotlin Coroutines + Flow |
| Inyección | Hilt (con KSP, no kapt) |
| Navegación | Navigation Compose |
| Base de datos | Room (SQLite), con KSP |
| Preferencias | DataStore (Preferences) |
| Tareas en segundo plano | WorkManager (+ integración con Hilt) |
| OCR | ML Kit Text Recognition (modelo latino, en el dispositivo) |
| Escáner de documentos | ML Kit Document Scanner (opcional, alternativa a CamScanner) |
| Imágenes | Coil (Compose) |
| PDF (visualizar) | `android.graphics.pdf.PdfRenderer` |
| PDF (generar) | `android.graphics.pdf.PdfDocument` |
| Biometría | `androidx.biometric` |
| Widget | Jetpack Glance (fase 4) |
| Serialización | kotlinx.serialization (copias de seguridad, exportación) |
| Copia en Drive | `AuthorizationClient` (Google Play Services) + Drive REST API v3, carpeta `appDataFolder` |
| Cifrado | AES-GCM con clave en Android Keystore; para backups, clave derivada de contraseña (PBKDF2) |
| Fechas | `java.time` (soportado de forma nativa con minSdk 26) |
| Tests | JUnit, MockK, Turbine (Flow), Room testing, Compose UI test |

**Parámetros de la app:**
- `minSdk = 26`, `targetSdk` y `compileSdk` = la última estable de Android.
- Paquete base: `com.mipatrimonio.app` (George puede cambiarlo al inicio; hacerlo una sola vez).
- Dispositivo de pruebas principal: RedMagic con Android 15 (REDMAGIC OS 10). Resolución 2480×1116. Probar también en emulador con Android 8 y 12.
- **Evitar:** `androidx.security:security-crypto` (`EncryptedFile` y `EncryptedSharedPreferences`), porque está deprecada. Implementar un pequeño `CryptoManager` propio con `Cipher` AES/GCM y `AndroidKeyStore`, o usar Tink si lo prefieres.
- **Evitar:** Google Sign-In legacy (`GoogleSignInClient`), deprecado. Para Drive se usa `AuthorizationClient`.
- **Evitar:** kapt (usar KSP), LiveData, RxJava, XML layouts.

---

## 4. Estructura del proyecto

Un único módulo `app`, organizado por funcionalidades:

```
app/src/main/java/com/mipatrimonio/app/
├── MiPatrimonioApp.kt              # @HiltAndroidApp, canales de notificación
├── MainActivity.kt                 # setContent, NavHost, bloqueo biométrico
├── core/
│   ├── database/                   # AppDatabase, DAOs, entidades, converters, seeds
│   ├── model/                      # enums y clases de dominio compartidas
│   ├── money/                      # Money.kt (formato y parseo de céntimos)
│   ├── security/                   # CryptoManager, AppLockManager
│   ├── datastore/                  # SettingsRepository (DataStore)
│   ├── work/                       # Workers (recurrentes, alertas, recordatorios, backup)
│   ├── notifications/              # NotificationHelper + canales
│   └── ui/
│       ├── theme/                  # Color.kt, Type.kt, Theme.kt, Shape.kt
│       ├── components/             # NeonCard, NeonButton, NeonChip, ProgressNeonBar, TabBar...
│       └── navigation/             # Routes.kt, AppNavHost.kt
├── feature/
│   ├── home/                       # HomeScreen, HomeViewModel
│   ├── transactions/               # TransactionsScreen, filtros, detalle/edición
│   ├── add/                        # AddMenuSheet, ReviewSheet, ScanFlow, ShareReceiverActivity
│   ├── ocr/                        # TextRecognizer wrapper, ReceiptParser, DocumentClassifier
│   ├── savings/                    # SavingsScreen, PlanFormSheet, BudgetsSection
│   ├── wallet/                     # WalletScreen, CardFormSheet, Subscriptions, DocumentsScreen
│   ├── settings/                   # SettingsScreen, seguridad, backup, exportación
│   ├── reports/                    # Informe mensual, gráficos por categoría
│   ├── shared/                     # Gastos compartidos (fase 4)
│   └── widget/                     # Glance widget (fase 4)
├── data/
│   ├── repository/                 # Implementaciones de repositorios
│   ├── backup/                     # BackupManager, DriveClient
│   └── export/                     # CsvExporter, PdfExporter
└── domain/
    ├── repository/                 # Interfaces
    └── usecase/                    # Cálculos de negocio (sección 7)
```

Reglas:
- `ViewModel` expone `StateFlow<UiState>`. Los Composables reciben estado y callbacks, no ViewModels (salvo la pantalla raíz de cada feature).
- Los cálculos de negocio (saldo, estimación de ahorro, alertas, proyección) viven en `domain/usecase` y son **funciones puras testeables**.
- Un único `AppDatabase`. Los DAOs devuelven `Flow` para listas observables.

---

## 5. Diseño visual (tema neón oscuro)

La app **siempre es oscura** por defecto (no hay tema claro en la primera versión). Futurista, con brillos neón sutiles.

### 5.1 Colores

| Token | Hex | Uso |
|---|---|---|
| `bg` | `#070A12` | Fondo de pantalla |
| `panel` | `#0E1424` | Tarjetas y paneles |
| `panelAlt` | `#0A1226` | Subpaneles, botones circulares, chips sin seleccionar |
| `line` | `#1F2A4A` | Bordes de paneles |
| `lineInput` | `#26325A` | Bordes de inputs y chips |
| `track` | `#161F3A` | Fondo de barras de progreso |
| `ink` | `#E6ECFF` | Texto principal |
| `muted` | `#8FA0C4` | Texto secundario |
| `chipText` | `#C7D2F0` | Texto de chips sin seleccionar |
| `neonCyan` | `#22D3EE` | Acento principal, tarjeta, pestaña activa |
| `neonViolet` | `#B26BFF` | Acento secundario |
| `neonMagenta` | `#FF4FD8` | Acento terciario (degradados) |
| `neonGreen` | `#39FF88` | Ingresos, estado correcto |
| `neonAmber` | `#FFB020` | Metálico, avisos |
| `expenseRed` | `#FF7A93` | Texto de gastos y errores |

**Colores por tipo de movimiento (obligatorio, consistente en toda la app):**

| Tipo | Color neón | Fondo tintado (chips) | Texto sobre tinte |
|---|---|---|---|
| Metálico (`CASH`) | `#FFB020` | `#2A1F08` | `#FFB020` |
| Tarjeta (`CARD`) | `#22D3EE` | `#07252B` | `#22D3EE` |
| Suscripciones (`SUBSCRIPTION`) | `#B26BFF` | `#241236` | `#C79BFF` |
| Ingresos (`INCOME`) | `#39FF88` | `#0B2A19` | `#39FF88` |

Fondos de alerta: ámbar `#2A1F08` con borde `#6B4A0E` y texto `#FFD27A`; correcto `#0B2A19` con texto `#7CFFB2`; error `#330A14` con texto `#FF8FA3`.

Contraste: todo el texto sobre `panel` debe cumplir al menos 4.5:1. No uses color como única señal (acompáñalo de texto o signo +/−).

### 5.2 Tipografía
- **Orbitron** (semibold/bold): títulos de pantalla en MAYÚSCULAS y cifras grandes (saldo, importes destacados). Letter-spacing ~0.04em.
- **Space Grotesk** (regular/medium/semibold/bold): resto del texto.
- Ambas son de licencia libre (OFL). Descarga los `.ttf` y colócalos en `res/font/` (en minúsculas y con guion bajo: `orbitron_semibold.ttf`, etc.). Define `FontFamily` en `Type.kt`. No dependas de descargas en tiempo de ejecución.
- Tamaños de referencia: título de pantalla 22 sp, saldo 30 sp, texto 14–15 sp, secundario 12–13 sp. Mínimo 12 sp.

### 5.3 Formas y brillo
- Paneles: radio 16–20 dp, borde 1 dp `line`. Pantalla de saldo y tarjetas destacadas: borde de acento más brillo.
- Brillo neón: sombra de color del acento con alfa ~0.15–0.3 y radio 24–28 dp (en Compose, `Modifier.shadow` con `ambientColor`/`spotColor`, o `drawBehind` con un `Paint` y `setShadowLayer`).
- Botón primario: fondo degradado horizontal `neonCyan → neonViolet`, texto `#070A12`, negrita, alto mínimo 48–52 dp, radio 14 dp.
- Botón secundario: borde del color de acento, fondo transparente, texto del acento.
- Zonas táctiles mínimas: 44 dp.

### 5.4 Barra de progreso neón animada (componente `ProgressNeonBar`)
Se usa en **Inicio** (plan de ahorro activo) y en **Ahorro**. Especificación:
- Pista (`track`): alto 14 dp (12 dp en Ahorro), radio 8 dp, borde 1 dp `lineInput`.
- Relleno: degradado horizontal `neonCyan → neonViolet → neonMagenta → neonCyan` que **se desplaza en bucle** (4 s, lineal, infinito) mediante un offset animado del `Brush`.
- Brillo exterior cian suave.
- **Destello**: una franja blanca translúcida (alfa 0.3, ancho ~30 % de la pista) que recorre la pista de izquierda a derecha cada 2,4 s.
- **Cabeza pulsante**: punto blanco de 10 dp al final del relleno con halo que alterna entre cian y magenta (1,6 s).
- Cambios de progreso: animación de ancho de 800 ms (`animateFloatAsState`).
- **Accesibilidad:** si la animación del sistema está desactivada (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`), mostrar la barra sin animar. Añadir `semantics` con el porcentaje.

### 5.5 Tarjetas (visual)
Seis diseños por defecto. Cada uno: degradado diagonal 135°, borde 1 dp del color de acento y brillo.

| Diseño | Degradado (inicio → fin) | Acento |
|---|---|---|
| Cian | `#0B1B3A → #1D0B3A` | `#22D3EE` |
| Magenta | `#2A0B2E → #0B1B3A` | `#FF4FD8` |
| Verde | `#06241A → #0B1B3A` | `#39FF88` |
| Ámbar | `#2A1A06 → #1A0B2E` | `#FFB020` |
| Violeta | `#1D0B3A → #0B0B24` | `#B26BFF` |
| Grafito | `#1A2030 → #0A0D14` | `#8FA0C4` |

Contenido de la tarjeta: arriba a la izquierda el nombre de la variante (o del banco si no hay variante) en Orbitron y color de acento; arriba a la derecha el banco; abajo "Tarjeta de débito/crédito/prepago" y el nombre de la tarjeta. **Sin números.** Altura 180 dp en la lista, 120 dp en la vista previa del formulario.

Foto propia: la imagen se guarda en el almacenamiento privado de la app y se muestra como fondo con una capa oscura (alfa 0.45) y sombra de texto para legibilidad.

### 5.6 Animación y movimiento general
Transiciones breves (150–300 ms). Evitar animaciones continuas salvo la barra de progreso. Respetar la reducción de movimiento.

---

## 6. Modelo de datos (Room)

Todas las fechas con `java.time` y `TypeConverter` (guardar `LocalDate` como `Long` epochDay o `String` ISO; `Instant` como `Long` epochMillis). Importes en **céntimos** (`Long`). Ids autogenerados (`Long`, `autoGenerate = true`).

```kotlin
enum class TransactionType { CASH, CARD, SUBSCRIPTION, INCOME }
enum class IncomeKind { PAYROLL, OTHER }
enum class CardKind { DEBIT, CREDIT, PREPAID }
enum class DocumentKind { PAYSLIP, INVOICE, WARRANTY, INSURANCE, CONTRACT, OTHER }
enum class RecurrencePeriod { WEEKLY, MONTHLY, QUARTERLY, YEARLY }

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bank: String,                 // "CaixaBank", "BBVA", "Santander", "ING", "Revolut", "Otro"
    val variant: String = "",         // "Imagin", opcional
    val kind: CardKind,
    val alias: String,
    val theme: String,                // "cyan","magenta","green","amber","violet","graphite"
    val photoPath: String? = null,    // ruta privada de la imagen propia
    val createdAt: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val icon: String,                 // nombre de icono de Material Icons
    val isIncome: Boolean = false,
    val isFixed: Boolean = false,     // gasto fijo vs variable (por defecto)
    val isDefault: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("type"), Index("categoryId"), Index("cardId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val incomeKind: IncomeKind? = null,   // solo si type == INCOME
    val amountCents: Long,                // siempre positivo; el signo lo da el tipo
    val currency: String = "EUR",
    val amountBaseCents: Long,            // importe convertido a la moneda base (EUR)
    val date: Long,                       // epochDay
    val merchant: String,
    val note: String = "",
    val categoryId: Long? = null,
    val cardId: Long? = null,             // solo si type == CARD
    val receiptPath: String? = null,      // ticket adjunto (cifrado)
    val recurringId: Long? = null,        // si lo generó una regla recurrente
    val sharedExpenseId: Long? = null,
    val createdAt: Long
)

@Entity(tableName = "tags")
data class TagEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(tableName = "transaction_tags", primaryKeys = ["transactionId", "tagId"])
data class TransactionTagCrossRef(val transactionId: Long, val tagId: Long)

@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                     // "Spotify", "Alquiler"
    val type: TransactionType,            // SUBSCRIPTION o CARD/CASH para fijos; INCOME para nómina fija
    val amountCents: Long,
    val period: RecurrencePeriod,
    val nextDate: Long,                   // epochDay del próximo cobro
    val cardId: Long? = null,
    val categoryId: Long? = null,
    val isSubscription: Boolean,
    val remindDaysBefore: Int = 2,
    val active: Boolean = true
)

@Entity(tableName = "savings_plans")
data class SavingsPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetCents: Long,                // editable en cualquier momento (sube o baja)
    val monthlyQuotaCents: Long,
    val priority: Int,                    // 1 = más alta
    val isActive: Boolean,                // el plan "activo" se muestra en Inicio
    val initialEstimatedMonths: Int,      // estimación al crear/editar (para detectar adelantos/retrasos)
    val lastNotifiedState: String = "NONE", // NONE, ON_TRACK, AHEAD, BEHIND
    val lastNotifiedMonths: Int = -1,
    val createdAt: Long
)

@Entity(tableName = "plan_contributions", indices = [Index("planId")])
data class PlanContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val amountCents: Long,                // puede ser negativo (retirada)
    val date: Long
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val limitCents: Long,                 // límite mensual
    val notified80Month: Int = -1,        // YYYYMM del último aviso al 80 %
    val notified100Month: Int = -1
)

@Entity(tableName = "documents", indices = [Index("kind")])
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: DocumentKind,
    val title: String,
    val filePath: String,                 // archivo cifrado en almacenamiento privado
    val mimeType: String,                 // application/pdf o image/*
    val date: Long,
    val expiresAt: Long? = null,          // garantías, seguros
    val createdAt: Long
)

// Fase 4
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(@PrimaryKey val currency: String, val rateToBase: Double, val updatedAt: Long)

@Entity(tableName = "people")
data class PersonEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val isMe: Boolean = false)

@Entity(tableName = "shared_expenses")
data class SharedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val totalCents: Long, val paidByPersonId: Long, val date: Long
)

@Entity(tableName = "shared_shares", primaryKeys = ["sharedExpenseId", "personId"])
data class SharedShareEntity(val sharedExpenseId: Long, val personId: Long, val shareCents: Long, val settled: Boolean = false)
```

**Categorías por defecto (seed en la primera ejecución):**
Gastos: Alimentación (fija: no), Restauración, Transporte y coche, Hogar (fija: sí), Ocio, Salud, Compras, Suscripciones (fija: sí), Viajes, Otros gastos.
Ingresos: Nómina, Otros ingresos (autónomo/freelance, Bizum recibido, devoluciones).
Cada una con color y un icono de Material Icons. Las categorías de gasto usan colores neón del tema; no uses los cuatro colores de tipo (reservados para tipos de movimiento) como único identificador.

**Reglas de integridad:**
- Si `type == CARD`, `cardId` es opcional pero recomendado. Si se elimina una tarjeta, sus movimientos NO se borran (`cardId` pasa a `null`).
- `amountBaseCents == amountCents` cuando `currency == "EUR"`.
- Borrar un plan borra sus aportaciones. Borrar una categoría reasigna sus movimientos a "Otros gastos".
- Migraciones: exportar el esquema de Room (`exportSchema = true`) y escribir migraciones explícitas, nunca `fallbackToDestructiveMigration` salvo en desarrollo.

---

## 7. Reglas de negocio

Todas implementadas como funciones puras en `domain/usecase` y cubiertas con tests.

### 7.1 Resumen mensual (pantalla Inicio)
Para el mes natural actual:
- `ingresos = Σ amountBaseCents` de `INCOME`.
- `gastos = Σ amountBaseCents` de `CASH + CARD + SUBSCRIPTION`.
- `aportadoAhorro = Σ aportaciones del mes` a planes.
- `disponible = ingresos − gastos − aportadoAhorro`.
- Barra por tipo: segmentos proporcionales al gasto de `CASH`, `CARD` y `SUBSCRIPTION` (mínimo visible aunque sea 0).
- Leyenda: los cuatro tipos con importe (el ingreso en verde).

### 7.2 Estimación de ahorro
Para un plan:
- `restante = max(0, targetCents − Σ aportaciones)`.
- `mesesEstimados = ceil(restante / monthlyQuotaCents)`; si `restante == 0` → meta alcanzada.
- `fechaFin = YearMonth.now().plusMonths(mesesEstimados)`; se muestra como "mar 2027".
- `libreMensual`: media de `(ingresos − gastos)` de los últimos 3 meses **completos**; si aún no hay 3, usar los meses disponibles; si no hay ninguno, usar la proyección del mes actual (7.5).
- **Estado:**
  - `BEHIND` si `monthlyQuotaCents > libreMensual` (no puede cumplir el plan con los gastos actuales).
  - `AHEAD` si `mesesEstimados < initialEstimatedMonths` (llegará antes de lo previsto).
  - `ON_TRACK` en cualquier otro caso.
- Cambiar el **importe objetivo** o la **cuota** recalcula todo y actualiza `initialEstimatedMonths`.
- **Edición del objetivo solo desde el formulario "Editar plan"** (nombre + importe). En la tarjeta del plan el importe objetivo es de solo lectura. La cuota mensual sí se ajusta con botones − / + (pasos de 25 €) en la tarjeta.

### 7.3 Notificaciones de ahorro
Un worker diario evalúa los planes activos. Notifica solo si cambia el estado (`lastNotifiedState`) o cambia `mesesEstimados` en ≥ 1 mes respecto de `lastNotifiedMonths`. Mensajes (ejemplos):
- `AHEAD`: "Vas por delante: alcanzarás «Viaje de verano» en 4 meses (feb 2027)."
- `BEHIND`: "Con tus gastos actuales no llegas a la cuota de «Viaje de verano». Baja la cuota o reduce gasto."
- Tras notificar, actualizar `lastNotifiedState` y `lastNotifiedMonths`.

### 7.4 Presupuestos
- `gastadoCategoría` = suma del mes actual de gastos de esa categoría.
- Aviso al alcanzar el **80 %** y al **100 %**, **una sola vez por mes y umbral** (usar `notified80Month` y `notified100Month` con valor `YYYYMM`).
- Colores de barra: ≤ 79 % cian, 80–99 % ámbar, ≥ 100 % rojo `#FF7A93`.

### 7.5 Proyección a fin de mes
`proyección = gastosHastaHoy / díaDelMes × díasDelMes` (solo si el día ≥ 3; antes, no mostrar). Mostrar junto a "Disponible" como "Proyección de gasto: X €".

### 7.6 Gastos recurrentes y suscripciones
- Un worker diario recorre `recurring` activas con `nextDate <= hoy` y genera un `TransactionEntity` por cada vencimiento pendiente (poner al día si el móvil estuvo apagado varios días), avanzando `nextDate` según `period`.
- Si la regla es de suscripción, el movimiento es `SUBSCRIPTION`.
- Aviso previo: notificación `remindDaysBefore` días antes del cobro de una suscripción.
- **Coste anual de una suscripción:** mensual×12, trimestral×4, anual×1, semanal×52. El total anual se muestra en la sección Suscripciones de Cartera.

### 7.7 Gastos fijos vs. variables
`fijos = Σ gastos de categorías con isFixed + movimientos recurrentes`; `variables = gastos − fijos`; `libreAlMes = ingresos − fijos − aportadoAhorro − variables`. Se muestra como un pequeño panel en Movimientos o Informe mensual.

### 7.8 Moneda
Moneda base: EUR. Para otra moneda, el usuario introduce un tipo de cambio manual (se guarda en `exchange_rates`). `amountBaseCents = round(amountCents × rate)`. Sin llamadas de red.

### 7.9 Gastos compartidos (fase 4)
Reparto igual o por importes. Saldo neto por persona = pagado − su parte. Mostrar "quién debe a quién" con un algoritmo simple: ordenar deudores y acreedores y compensar el mayor con el mayor.

### 7.10 Formato
- Dinero: `1.234,56 €` (separador de miles `.`, decimal `,`, símbolo al final con espacio). Gastos con prefijo `−`, ingresos con `+`.
- Fechas: `2 nov` en listas, `02/11/2026` en formularios. Usar `Locale("es", "ES")`.
- Parseo de importes escritos por el usuario: aceptar `23,45`, `23.45` y `1.234,56`.

---

## 8. Pantallas y flujos

Navegación inferior de 5 elementos: **Inicio · Movimientos · [+] · Ahorro · Cartera**. El botón + central es circular (56 dp) con degradado cian→violeta y brillo. La pestaña activa se resalta en cian con un leve brillo de texto. Fondo de la barra `#0B1020` con borde superior `line`.

### 8.1 Inicio
Orden vertical (scroll):
1. Cabecera: mes y año en `muted`, título "RESUMEN" (Orbitron).
2. **Tarjeta de saldo**: fondo degradado oscuro azul-violeta con borde `#2B3A6E` y brillo cian. "Disponible este mes" y la cifra grande en Orbitron color cian. Debajo dos mini-paneles: Ingresos (verde) y Gastos (rojo claro).
3. **Plan de ahorro activo** (panel): etiqueta "PLAN DE AHORRO ACTIVO", porcentaje en Orbitron cian, nombre del plan, `ProgressNeonBar` animada, debajo "X € / Y €" a la izquierda y "N meses · mes año" a la derecha. Si no hay plan activo: botón "Crear plan de ahorro". Al tocar, abre Ahorro.
4. **Dónde se va el dinero**: barra segmentada por tipo con brillo y leyenda de 4 filas.
5. Avisos activos (presupuesto cercano al límite, suscripciones próximas a renovar): banners ámbar/verde.
6. Proyección a fin de mes.
7. **Últimos movimientos** (4): fila con barra vertical de color del tipo (6 dp, con brillo), comercio, "fecha · tipo", importe con color del tipo.

### 8.2 Movimientos
- Título "MOVIMIENTOS". Buscador por comercio/nota.
- Chips de filtro: Todos, Metálico, Tarjeta, Suscripciones, Ingresos (+ filtros por tarjeta, categoría, etiqueta y rango de fechas en un panel).
- Lista agrupada por día. Tocar un movimiento abre detalle/edición (con ticket adjunto si lo hay, categoría, etiquetas, nota). Deslizar para borrar (con deshacer).
- Comparativa mes a mes y gráfico por categoría (donut/barras con `Canvas`) accesibles desde un botón "Análisis".

### 8.3 Añadir (botón +)
Hoja inferior con tres opciones: **Escanear ticket (OCR)**, **Gasto manual o en metálico**, **Ingreso (nómina u otros)**.

**Pantalla de revisión (común a las tres):**
- Chips de tipo: Metálico, Tarjeta, Suscripciones, Ingresos (seleccionado: relleno neón con texto oscuro).
- Si el tipo es Tarjeta: selector de tarjeta del usuario.
- Campos: Importe (€), Comercio o concepto, Fecha (selector de fecha), Categoría, Etiquetas, Nota, Moneda (fase 4).
- Si viene de OCR: texto "Leído del ticket: revisa los datos" y la imagen del ticket adjunta (miniatura).
- Si el tipo es Ingreso: elegir "Nómina" u "Otros ingresos".
- Botón "Guardar". Validación: el importe debe ser > 0. Tras guardar, navegar a Movimientos con el nuevo movimiento arriba.

### 8.4 Ahorro
- Título "AHORRO".
- **Panel del plan activo:** nombre, `ProgressNeonBar`, "ahorrado / objetivo", **Importe objetivo (solo lectura)** y botón "Editar plan" (abre la hoja de edición), cuota mensual con − / + (25 €), texto "Lo alcanzarás en N meses (mes año)", y banner de estado (verde "Vas bien" / rojo "No llegas…").
- Botón "Aportar" para registrar una aportación al plan.
- **Lista de planes** con prioridad (reordenable) y botón "Crear plan de ahorro" (degradado). Solo uno es el "activo" para la barra de Inicio (se elige con un interruptor).
- **Hoja de crear/editar plan:** Nombre y Importe objetivo (€). Texto de ayuda: "Podrás subir o bajar el importe cuando quieras y la estimación se recalcula." Botón "Guardar plan". Al editar, también "Eliminar plan".
- **Presupuestos por categoría:** lista con barra (colores según 7.4), importe gastado/límite, añadir/editar presupuesto.

### 8.5 Cartera
- Título "CARTERA".
- **Tarjetas:** lista de tarjetas con su diseño. Texto de ayuda: "Toca una tarjeta para editarla o eliminarla. No se guardan números ni datos de la tarjeta." Botón "Añadir tarjeta" (borde discontinuo).
  - **Hoja de tarjeta (crear/editar):** vista previa en vivo; banco (chips: CaixaBank, BBVA, Santander, ING, Revolut, Otro); variante opcional (texto); tipo (Débito, Crédito, Prepago); nombre; diseño (seis muestras con el nombre del diseño); botón "Usar mi propia foto" (selector de fotos del sistema, `PickVisualMedia`); "Quitar foto y volver al diseño"; "Guardar tarjeta"; en edición, "Eliminar tarjeta" (con confirmación).
- **Suscripciones:** lista con importe, próxima renovación y coste anual total. Añadir/editar (nombre, importe, periodicidad, próxima fecha, tarjeta, categoría).
- **Recurrentes** (gastos fijos): misma estructura.
- **Documentos:** lista por tipo (Nómina, Factura, Garantía, Seguro, Otros) con icono PDF verde, búsqueda, fecha de vencimiento si aplica. Botón "Añadir documento": abre selector de archivos del sistema (`OpenDocument`) y/o escáner. Visor de PDF integrado.
- **Ajustes** (acceso a la pantalla de ajustes): bloqueo con huella/PIN, copia en Drive (último backup), exportación, recordatorio nocturno, moneda, informe mensual.

### 8.6 Recepción desde "Compartir" (CamScanner u otras apps)
`ShareReceiverActivity` (tema oscuro, hoja inferior):
1. Título "DESDE CAMSCANNER" (o el nombre de la app origen si se conoce).
2. Muestra el nombre del archivo y la pregunta "¿Qué es?".
3. **Clasificación automática sugerida** (sección 9.4): preselecciona el botón más probable, pero el usuario confirma.
4. Botones: "Ticket de compra" (→ OCR → pantalla de revisión) y "Nómina" (→ formulario de documento: tipo Nómina, título editable por defecto "Nómina <mes año>", fecha; y, opcionalmente, registrar el ingreso con el importe líquido leído si el OCR lo detecta). Opciones adicionales: "Factura", "Garantía", "Otro documento".
5. Siempre copiar el archivo a almacenamiento privado inmediatamente (el permiso de lectura del URI es temporal).

### 8.7 Informe mensual (fase 4)
Resumen del mes anterior: ingresos, gastos por tipo y categoría, comparativa con el mes anterior, planes de ahorro, top 5 comercios, 1–3 consejos generados con reglas simples (por ejemplo, "Ocio subió un 30 % respecto al mes anterior"). Notificación el día 1 de cada mes.

### 8.8 Gastos compartidos (fase 4)
Crear grupo (personas), añadir gasto compartido (quién pagó, reparto igual o por importes), ver saldos y "liquidar". Un gasto compartido que paga el usuario genera su movimiento normal por su parte.

---

## 9. OCR y análisis de tickets

### 9.1 Flujo
1. Obtener imagen: (a) ML Kit Document Scanner (cámara con recorte automático), (b) selector de fotos, o (c) archivo recibido por "Compartir" (imagen o PDF).
2. Si es PDF: renderizar la primera página con `PdfRenderer` a un `Bitmap` (ancho ~1600 px) y pasarlo al reconocedor.
3. `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)` (latino, en el dispositivo). Obtener el texto por líneas con sus posiciones.
4. `ReceiptParser.parse(lines: List<String>): ParsedReceipt` devuelve `amountCents?`, `date?`, `merchant?` y una confianza por campo.
5. Mostrar siempre la pantalla de revisión con los datos rellenados. **Nunca guardar sin confirmación.**

### 9.2 Heurísticas del importe
- Normalizar líneas (mayúsculas, quitar espacios dobles, `O`→`0` solo dentro de números).
- Regex de importe: `(\d{1,3}(?:[.\s]\d{3})*|\d+)[.,]\d{2}` (admitir `€` o `EUR` junto al número).
- **Prioridad:** buscar el importe en líneas que contengan `TOTAL`, `TOTAL A PAGAR`, `IMPORTE`, `A PAGAR`, `TOTAL (EUR)`, `TOTAL EUROS`, `TOTAL COMPRA`. Si hay varios, quedarse con el último.
- **Excluir** líneas con `IVA`, `BASE`, `CUOTA`, `CAMBIO`, `ENTREGADO`, `EFECTIVO`, `DESCUENTO`, `AHORRO`, `PUNTOS` al decidir el total.
- Si no hay línea con palabra clave: el importe **mayor** que aparezca en el tercio inferior del ticket. Baja confianza.

### 9.3 Heurísticas de fecha y comercio
- Fecha: regex `\b(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})\b` (dd/MM/yyyy, dd-MM-yy, dd.MM.yyyy); validar rango razonable (no futura más de 1 día, no anterior a 5 años). Si no hay, usar hoy.
- Comercio: primeras 6 líneas con letras; descartar las que contengan `CIF`, `NIF`, `TEL`, `C/`, `CALLE`, `AVDA`, `WWW`, `@`, números largos. Coger la primera que quede. Si coincide con una lista de comercios conocidos (MERCADONA, CARREFOUR, LIDL, DIA, ALCAMPO, EROSKI, AHORRAMAS, ALDI, LEROY MERLIN, IKEA, REPSOL, CEPSA, BP, SHELL…), usar el nombre normalizado.
- Sugerir categoría por comercio (tabla simple de palabras clave → categoría) y recordar la elección del usuario (aprendizaje simple: `merchant → categoryId`).

### 9.4 Clasificación del documento recibido (ticket vs nómina)
Tras el OCR, puntuar por palabras clave (sin acentos, mayúsculas):
- **Nómina:** `NOMINA`, `RECIBO DE SALARIOS`, `DEVENGOS`, `LIQUIDO A PERCIBIR`, `DEDUCCIONES`, `SEGURIDAD SOCIAL`, `IRPF`, `BASE DE COTIZACION`, `TOTAL A DEDUCIR`.
- **Ticket:** `TICKET`, `FACTURA SIMPLIFICADA`, `IVA`, `CAMBIO`, `TOTAL`, `GRACIAS POR SU VISITA`, `TARJETA`.
- Si la puntuación de uno supera claramente a la del otro, preseleccionarlo; en caso contrario, no preseleccionar. El usuario siempre confirma.
- Para nóminas, intentar leer `LIQUIDO A PERCIBIR` (importe) como sugerencia de ingreso.

### 9.5 Tests del parser
Crear `ReceiptParserTest` con al menos 8 textos de ejemplo realistas (supermercado, gasolinera, restaurante, ticket con IVA desglosado, ticket con cambio en efectivo, ticket con fecha en formato dd-MM-yy, ticket sin palabra "TOTAL") y verificar importe, fecha y comercio.

---

## 10. Recibir archivos desde CamScanner (Compartir)

En `AndroidManifest.xml`:

```xml
<activity
    android:name=".feature.add.ShareReceiverActivity"
    android:exported="true"
    android:theme="@style/Theme.MiPatrimonio.Sheet">
    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="application/pdf" />
        <data android:mimeType="image/*" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.SEND_MULTIPLE" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="application/pdf" />
        <data android:mimeType="image/*" />
    </intent-filter>
</activity>
```

Implementación:
- Leer `Intent.EXTRA_STREAM` (`Uri`) con la API correcta para Android 13+ (`getParcelableExtra(name, Uri::class.java)`) y la antigua para versiones anteriores.
- Copiar el contenido a almacenamiento privado (`filesDir/inbox/`) con `contentResolver.openInputStream(uri)` en un hilo de IO, **antes** de mostrar nada.
- El bloqueo de la app (huella/PIN) debe pedirse también antes de mostrar o guardar contenido recibido si la app está bloqueada.
- Limitación conocida: Android no permite "abrir CamScanner desde la app y recoger el resultado automáticamente". El flujo soportado es **compartir desde CamScanner hacia MiPatrimonio** y, alternativamente, elegir un archivo con el selector del sistema o usar el escáner integrado de ML Kit.

---

## 11. Documentos y almacenamiento cifrado

- Ruta: `filesDir/documents/<uuid>.bin` (contenido cifrado). El nombre original y el tipo MIME se guardan en `DocumentEntity`.
- **Cifrado:** AES-256-GCM con clave generada en Android Keystore (`KeyGenParameterSpec`, sin exigir autenticación de usuario para poder usar workers). Formato del archivo: `[12 bytes de IV][datos cifrados]`.
- `CryptoManager` expone `encrypt(input: InputStream, output: OutputStream)` y `decrypt(input: InputStream): InputStream` (o descifrado a un archivo temporal en `cacheDir` para el visor de PDF, borrándolo al cerrar).
- Visor de PDF: descifrar a `cacheDir/view_<uuid>.pdf`, abrir con `PdfRenderer` y dibujar páginas en una `LazyColumn` de `Bitmap`. Borrar el temporal al salir.
- Imágenes de tickets: mismo cifrado; miniaturas generadas y cacheadas en memoria.
- Compartir o exportar un documento: descifrar a un archivo temporal y compartir con `FileProvider` (nunca exponer la ruta privada).
- Las claves de Keystore **no son transferibles** a otro móvil: por eso las copias de seguridad usan una clave derivada de una contraseña (sección 13).

---

## 12. Notificaciones y tareas en segundo plano

**Canales de notificación** (crear en `MiPatrimonioApp`):
- `budgets` — Presupuestos
- `savings` — Ahorro
- `subscriptions` — Suscripciones y recurrentes
- `reminders` — Recordatorios (apuntar gastos en metálico)
- `reports` — Informe mensual y documentos por vencer

**Permiso:** `POST_NOTIFICATIONS` en Android 13+ (pedirlo con una explicación previa, no al arrancar).

**Workers (WorkManager, con `HiltWorker`):**
| Worker | Periodicidad | Función |
|---|---|---|
| `RecurringWorker` | diaria | generar movimientos recurrentes pendientes y avisar de renovaciones |
| `AlertsWorker` | diaria | evaluar presupuestos (7.4) y planes de ahorro (7.3) |
| `CashReminderWorker` | diaria (~21:00, flexible) | "¿Has apuntado hoy tus gastos en metálico?" (configurable y desactivable) |
| `MonthlyReportWorker` | mensual (día 1) | generar informe y notificar |
| `DocumentExpiryWorker` | diaria | avisar de documentos que vencen en 30/7 días |
| `BackupWorker` | semanal (opcional) | copia a Drive (fase 4) |

- Usar `PeriodicWorkRequest` con `ExistingPeriodicWorkPolicy.KEEP` y restricciones mínimas. **No usar alarmas exactas.**
- Tras ejecutar `RecurringWorker`, lanzar las comprobaciones de alertas.
- Además de los workers, recalcular alertas de presupuesto justo después de guardar un gasto (en el caso de uso), para que el aviso sea inmediato.

**Fabricantes (REDMAGIC OS y similares):** estos sistemas pueden cerrar procesos en segundo plano. En Ajustes, incluir una sección "Fiabilidad de avisos" que explique cómo desactivar la optimización de batería para la app y un botón que abra `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`. No pedir `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` directamente salvo que George lo decida.

---

## 13. Copia de seguridad en Google Drive (fase 4)

### 13.1 Qué se guarda
Un único archivo `mipatrimonio-AAAAMMDD-HHmm.mpbak` (ZIP interno) con:
- `data.json` (kotlinx.serialization): todas las tablas, **excepto** archivos binarios.
- `files/`: documentos y tickets **descifrados** y vueltos a **cifrar con la clave del backup**.
- `meta.json`: versión del esquema, fecha, versión de la app.

### 13.2 Cifrado del backup
- El usuario define una **contraseña de copia** (se la pide al activar la función; la app **no puede recuperarla**, hay que avisar).
- Derivar la clave con PBKDF2WithHmacSHA256 (sal aleatoria de 16 bytes, iteraciones altas) y cifrar con AES-256-GCM. Guardar sal e IV en la cabecera del archivo.
- Al restaurar: pedir contraseña, descifrar, importar datos y **volver a cifrar los archivos con la clave de Keystore del nuevo dispositivo**.

### 13.3 Drive
- Autorización con **`AuthorizationClient`** pidiendo el alcance `https://www.googleapis.com/auth/drive.appdata` (carpeta oculta de la app en el Drive del usuario). No hace falta iniciar sesión en la app.
- Llamadas a la API REST de Drive v3 (`appDataFolder`): listar, subir (multipart) y descargar archivos. Conservar las últimas 5 copias y borrar las anteriores.
- **Configuración en Google Cloud (manual, George):** crear un proyecto, activar la API de Drive, configurar la pantalla de consentimiento OAuth en modo de prueba, añadir como usuarios de prueba las cuentas de Google de los amigos, y crear un cliente OAuth de tipo Android con el nombre de paquete y la huella SHA-1 del certificado de firma (de depuración y de release). Documentar los pasos en `docs/drive-setup.md`.
- La subida semanal la hace `BackupWorker` con la restricción de red no medida (Wi-Fi) por defecto, configurable.
- Pantalla de Ajustes → Copia de seguridad: estado, fecha de la última copia, "Hacer copia ahora", "Restaurar", interruptor de copia semanal, cambiar contraseña.

### 13.4 Copia local (alternativa sin Google)
Exportar/importar `.mpbak` con el selector de archivos del sistema (`CreateDocument` / `OpenDocument`) para quien no quiera usar Drive.

---

## 14. Seguridad y privacidad

- **Sin red por defecto.** La app solo usa internet para la copia en Drive (si se activa). No incluir analíticas, anuncios ni librerías de rastreo.
- **No almacenar** números de tarjeta, CVV, IBAN, ni credenciales bancarias. Ni en la base, ni en backups ni en logs.
- **Bloqueo de la app:** `BiometricPrompt` con huella o credencial del dispositivo (PIN/patrón). Se pide al abrir la app y tras X minutos en segundo plano (configurable: inmediato, 1 min, 5 min). Pantalla de bloqueo con estética de la app.
- **`FLAG_SECURE`** en las ventanas con documentos o saldos si el usuario activa "Ocultar contenido en recientes y capturas" (activado por defecto).
- **`android:allowBackup="false"`** y reglas de extracción de datos que excluyan todo (la copia propia sustituye a la automática de Android).
- Logs: nunca imprimir importes, comercios ni rutas de documentos en versiones release.
- Permisos mínimos: `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, `CAMERA` (solo si se usa captura propia; el escáner de ML Kit gestiona la cámara por sí mismo), y red para Drive. Nada de almacenamiento amplio: usar el selector del sistema.
- `FileProvider` solo para compartir archivos temporales y exportaciones.

---

## 15. Exportación (fase 4)

- **CSV:** UTF-8 con BOM, separador `;`, decimales con coma, fechas `dd/MM/yyyy`. Columnas: `fecha;tipo;importe;moneda;comercio;categoria;tarjeta;etiquetas;nota`. Filtro por rango de fechas.
- **PDF:** informe del periodo con `PdfDocument` (resumen, tabla de movimientos, totales por categoría), con fondo claro para impresión.
- Guardar con `CreateDocument` (el usuario elige dónde) o compartir con la hoja de compartir del sistema.

---

## 16. Widget, dashboard personalizable y otros extras

- **Widget (Glance):** muestra "Disponible este mes", la barra del plan activo (sin animación) y un botón "+ Gasto" que abre `mipatrimonio://add` (deep link a la hoja de añadir). Diseño oscuro con acentos neón, tamaños 2×2 y 4×2. Se actualiza tras cada cambio y con un worker periódico.
- **Acceso directo (shortcut)** de la app: "Añadir gasto".
- **Dashboard personalizable:** en Ajustes → Inicio, lista reordenable con interruptores de visibilidad para cada bloque (saldo, plan activo, barra por tipo, avisos, proyección, últimos movimientos). Se guarda en DataStore.
- **Multimoneda:** selector de moneda en el formulario; pantalla de tipos de cambio manuales en Ajustes.

---

## 17. Estrategia de tests

- **Unit tests (obligatorios):** `Money` (formato y parseo), cálculos de saldo, estimación de ahorro y estados (`AHEAD/BEHIND/ON_TRACK`), proyección, coste anual de suscripciones, generación de recurrentes con huecos de días, presupuestos (umbrales una vez al mes), `ReceiptParser`, `DocumentClassifier`, saldos de gastos compartidos.
- **Room:** tests instrumentados de DAOs con base en memoria (inserciones, filtros, borrado en cascada, migraciones).
- **ViewModel:** tests con Turbine para `StateFlow`.
- **UI:** al menos tests de Compose para el formulario de revisión y el formulario de plan de ahorro.
- Cada tarea del plan indica qué tests escribir.

---

## 18. Plan de tareas (en orden)

Cada tarea termina con la app **compilando**. Marca `[x]` al completar.

### Fase 1
- [x] **T1.1 Proyecto base.** Crear proyecto Android (Kotlin, Compose, Material 3, `minSdk 26`), version catalog, KSP, Hilt, Room, DataStore, Navigation Compose. Clase `MiPatrimonioApp`. *Aceptación:* la app arranca y muestra una pantalla vacía oscura.
- [x] **T1.2 Tema neón.** `Color.kt`, `Type.kt` (fuentes Orbitron y Space Grotesk en `res/font`), `Theme.kt` oscuro, `Shape.kt`. Componentes: `NeonCard`, `NeonButton` (primario/secundario), `NeonChip`, `ScreenTitle`. *Aceptación:* pantalla de muestra con todos los componentes.
- [x] **T1.3 Navegación.** Barra inferior con 4 pestañas + botón central +, `AppNavHost` con 4 pantallas vacías. *Aceptación:* se navega entre pestañas; el + abre una hoja vacía.
- [x] **T1.4 Base de datos.** Entidades, enums, `TypeConverters`, DAOs (`TransactionDao`, `CardDao`, `CategoryDao`, `RecurringDao`, `SavingsDao`, `BudgetDao`, `DocumentDao`, `TagDao`), `AppDatabase` con `exportSchema = true`, seed de categorías. Módulo Hilt. *Aceptación:* tests de DAO pasan.
- [x] **T1.5 `Money`.** Formato `1.234,56 €`, parseo flexible, tests. *Aceptación:* tests pasan.
- [x] **T1.6 Tarjetas.** `WalletScreen` (lista de tarjetas) y `CardFormSheet` (banco, variante, tipo, nombre, 6 diseños, foto propia con `PickVisualMedia` copiada a almacenamiento privado, vista previa en vivo, editar/eliminar con confirmación). *Aceptación:* alta, edición, borrado y foto funcionan y persisten.
- [x] **T1.7 Añadir movimiento manual.** `AddMenuSheet` y `ReviewSheet` (tipo, importe, comercio, fecha, categoría, tarjeta si aplica, nota). Casos de uso `AddTransaction`. *Aceptación:* un movimiento guardado aparece en la lista.
- [x] **T1.8 Lista de movimientos.** Filtros por tipo, buscador, agrupado por día, edición, borrado con deshacer. *Aceptación:* filtros correctos; tests de ViewModel.
- [x] **T1.9 Inicio.** Tarjeta de saldo, barra por tipo, leyenda, últimos movimientos (con los colores por tipo y su brillo). Caso de uso `GetMonthlySummary` con tests. *Aceptación:* cifras coinciden con los movimientos.
- [x] **T1.10 Bloqueo.** `BiometricPrompt` + ajustes de bloqueo en DataStore, `FLAG_SECURE`. *Aceptación:* al volver a la app se pide autenticación.

### Fase 2
- [x] **T2.1 `CryptoManager`.** AES-GCM con Keystore, cifrar/descifrar streams, tests instrumentados. *Aceptación:* round-trip correcto.
- [x] **T2.2 Documentos.** Lista, alta desde selector del sistema, cifrado, visor PDF (`PdfRenderer`), borrado. *Aceptación:* se guarda y se abre una nómina PDF.
- [x] **T2.3 OCR base.** Wrapper de ML Kit Text Recognition, renderizado de PDF a bitmap. *Aceptación:* devuelve el texto de una imagen de prueba.
- [x] **T2.4 `ReceiptParser`.** Reglas de la sección 9 con tests (≥ 8 casos). *Aceptación:* tests pasan.
- [x] **T2.5 Escaneo de ticket.** Integrar ML Kit Document Scanner (con alternativa de galería) → OCR → `ReviewSheet` rellenada, con miniatura adjunta. *Aceptación:* escanear un ticket real prellena importe, fecha y comercio editables.
- [x] **T2.6 Compartir.** `ShareReceiverActivity`, copia inmediata, `DocumentClassifier`, flujo ticket/nómina. *Aceptación:* compartir un PDF desde CamScanner abre la hoja y guarda correctamente.
- [x] **T2.7 Etiquetas y notas.** Tabla de etiquetas, selector en el formulario, filtro por etiqueta. *Aceptación:* etiquetar y filtrar funciona.

### Fase 3
- [x] **T3.1 Planes de ahorro.** CRUD de planes, hoja de crear/editar (nombre + importe objetivo), aportaciones, **importe objetivo de solo lectura en la tarjeta**, cuota con −/+ (25 €). Caso de uso `EstimateSavings` con tests. *Aceptación:* estimación se actualiza al cambiar objetivo o cuota.
- [x] **T3.2 `ProgressNeonBar`.** Componente animado (5.4) usado en Inicio y Ahorro, con soporte de reducción de movimiento. *Aceptación:* animación fluida; sin animación si el sistema la desactiva.
- [x] **T3.3 Varios planes y prioridad.** Reordenar, plan activo. *Aceptación:* Inicio muestra el plan activo.
- [x] **T3.4 Presupuestos.** CRUD, barra por colores, avisos 80/100 % una vez al mes. *Aceptación:* tests de umbrales.
- [ ] **T3.5 Recurrentes y suscripciones.** CRUD, `RecurringWorker`, coste anual, aviso previo. *Aceptación:* tests de generación con huecos de días.
- [ ] **T3.6 Notificaciones.** Canales, permiso, `AlertsWorker`, avisos de ahorro (7.3). *Aceptación:* se reciben avisos de prueba.
- [ ] **T3.7 Análisis.** Gráfico por categoría, comparativa mes a mes, proyección a fin de mes, fijos vs. variables. *Aceptación:* cifras verificadas con tests.
- [ ] **T3.8 Recordatorio nocturno** configurable.

### Fase 4
- [ ] **T4.1 Exportación** CSV y PDF.
- [ ] **T4.2 Copia local** `.mpbak` (cifrada con contraseña) y restauración.
- [ ] **T4.3 Copia en Drive** (`AuthorizationClient` + REST `appDataFolder`), `BackupWorker`.
- [ ] **T4.4 Widget Glance** y shortcut.
- [ ] **T4.5 Informe mensual.**
- [ ] **T4.6 Multimoneda.**
- [ ] **T4.7 Gastos compartidos.**
- [ ] **T4.8 Dashboard personalizable** y vencimiento de documentos.
- [ ] **T4.9 Pulido:** accesibilidad (TalkBack), rendimiento, pruebas en Android 8/12/15, APK firmado para amigos.

---

## 19. Convenciones de código

- Kotlin idiomático, `ktlint`/estilo oficial de Kotlin. Funciones cortas, nombres descriptivos.
- Composables: `PascalCase`, parámetros `modifier: Modifier = Modifier` en último lugar, `@Preview` con datos de ejemplo para cada pantalla.
- Estado inmutable (`data class` + `copy`). Un `UiState` y un `UiEvent` por pantalla.
- Sin lógica de negocio en Composables ni en DAOs.
- Commits pequeños con mensaje en español o inglés coherente (`feat:`, `fix:`, `test:`, `docs:`).
- Estructura de ramas: `main` estable; una rama por tarea (`t1-6-tarjetas`).
- Manejo de errores: nunca tragarse excepciones en silencio; mostrar mensaje al usuario cuando proceda.
- Accesibilidad: `contentDescription` en iconos, zonas táctiles de 44 dp, no usar solo el color como señal.

---

## 20. Textos clave de la app (español)

- Pestañas: Inicio, Movimientos, Ahorro, Cartera. Botón: "Añadir".
- Tipos: Metálico, Tarjeta, Suscripciones, Ingresos.
- Plan de ahorro: "Crear plan de ahorro", "Editar plan", "Importe objetivo", "Cuota mensual de ahorro", "Lo alcanzarás en N meses (mes año)", "Vas bien: con tus ingresos y gastos actuales puedes cumplirlo.", "Aviso: con tus gastos actuales no llegas a esta cuota. Baja la cuota o reduce gasto.", "Podrás subir o bajar el importe cuando quieras y la estimación se recalcula."
- Tarjetas: "Añadir tarjeta", "Toca una tarjeta para editarla o eliminarla. No se guardan números ni datos de la tarjeta.", "Usar mi propia foto", "Quitar foto y volver al diseño", "Eliminar tarjeta".
- Compartir: "¿Qué es?", "Ticket de compra", "Nómina".
- Revisión: "Revisa los datos", "Leído del ticket: revisa los datos".

---

## 21. Preguntas abiertas para George (resolver cuando toque)

1. Nombre definitivo del paquete (`com.mipatrimonio.app` por defecto).
2. Logos oficiales de bancos en las tarjetas (por ahora, solo texto con tipografía propia).
3. Si las aportaciones a planes de ahorro se registran solo a mano o también con aportación automática mensual.
4. Si el recordatorio nocturno debe estar activado por defecto.
5. Si los amigos usarán APK directo o prueba cerrada en Google Play.

---

## 22. Cómo trabajar con OpenCode

- Coloca este archivo en la raíz del repositorio como `MiPatrimonio.md` y crea un `AGENTS.md` breve que diga: "Lee `MiPatrimonio.md` antes de cualquier tarea. Sigue la sección 0."
- Sesión típica: "Lee MiPatrimonio.md. Haz la tarea T1.4 (sección 18). Al terminar, compila y dime qué archivos has tocado."
- Los modelos gratuitos tienen contexto limitado: pasa al modelo **solo las secciones necesarias** para la tarea (0, la tarea en la sección 18 y las secciones que menciona). No pegues el documento entero en cada petición.
- Si el modelo propone una librería o API que no reconoces, pídele que cite la documentación oficial antes de usarla.
- Después de cada tarea: compilar, ejecutar tests, probar en el móvil y hacer commit.
