<div align="center">

# MiPatrimonio

**Tus gastos, ingresos, ahorro y documentos, en un solo sitio. Sin cuentas, sin bancos y sin salir de tu móvil.**

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26%20(Android%208)-2ea44f?style=for-the-badge)

</div>

---

## ¿Qué es MiPatrimonio?

**MiPatrimonio** es una app Android para llevar el control de tus finanzas personales de forma sencilla y **privada**. No se conecta a tu banco: tú decides qué registras, y todo se guarda **en tu propio teléfono**.

Pensada para saber en qué se va el dinero, organizar nóminas y documentos, controlar suscripciones y presupuestos, y ahorrar para una meta concreta, con un diseño oscuro de estética neón.

## Descarga

- **APK (compartir entre amigos):** el archivo `MiPatrimonio.apk` se genera con firma de depuración (válido para instalar y compartir; no apto para Play Store). Puedes generarlo tú mismo con `./gradlew assembleDebug` (ver [Compilación](#compilación)) o descargarlo desde [Releases](../../releases) cuando esté publicado.
- Requiere **Android 8.0 (API 26)** o superior.

## Funcionalidades

| | Función | Descripción |
|---|---|---|
| 📊 | **Inicio / Resumen** | Saldo disponible del mes, ingresos y gastos, desglose por tipo (metálico, tarjeta, suscripción, ingreso), proyección, últimos movimientos y avisos. Dashboard personalizable (orden y visibilidad de bloques). |
| 💸 | **Movimientos** | Registro de gastos e ingresos (metálico, tarjeta, suscripción, nómina/otros), búsqueda, filtros por tipo y etiqueta, edición y borrado con deshacer. |
| 🧾 | **Escaneo de tickets (OCR)** | Lectura de importe, fecha y comercio desde imagen (ML Kit, en el dispositivo). Los datos se revisan antes de guardar; la imagen se cifra. |
| 📄 | **Documentos y nóminas** | Guarda PDFs cifrados (nóminas, facturas, garantías). Detección de nómina con importación de ingreso (confirmación obligatoria) y aviso de vencimiento próximo. |
| 📤 | **Compartir desde otras apps** | Recibe PDF o imágenes compartidas (p. ej. desde una app de escaneo) y clasifica si es ticket, nómina u otro documento. |
| 💳 | **Tarjetas (sin datos sensibles)** | Añade tarjetas solo con banco/alias: **nunca** números ni CVV. Imagen personalizada opcional, guardada en almacenamiento privado. |
| 🔁 | **Suscripciones y gastos fijos** | Recurrentes con periodicidad, aviso previo, coste anual y generación automática de movimientos. |
| 🎯 | **Planes de ahorro** | Objetivo, cuota mensual, aportaciones/retiradas, prioridad entre planes y estimación de cuándo llegarás a la meta (adelantado/en plazo/retrasado). Barra de progreso animada (respeta “reducir animaciones”). |
| 📉 | **Presupuestos** | Límite mensual por categoría con avisos al 80 % y 100 % (una vez al mes) y barras de estado. |
| 📈 | **Análisis** | Comparativa mes a mes, gastos por categoría y fijos vs. variables, con proyección a fin de mes. |
| 👥 | **Gastos compartidos** | Reparto igual o por importes, saldos netos y “quién debe a quién”, con liquidación que refleja el efecto en el disponible. |
| 💱 | **Multimoneda** | Moneda base EUR, conversión con tipo de cambio manual y totales calculados en base. |
| 📤 | **Exportación e informes** | Exporta movimientos a **CSV** o resumen a **PDF**, e informe mensual del mes anterior (generación manual y aviso). |
| 🔐 | **Copia de seguridad local** | Copia cifrada con contraseña (`.mpbak`) y restauración (sustituye los datos). *La copia en Google Drive (T4.3) está pospuesta por decisión del proyecto.* |
| 🧩 | **Widget y atajo** | Widget (Glance) con disponible/gastos/ingresos y acceso rápido a añadir gasto. |
| 🔔 | **Notificaciones** | Presupuestos, ahorro, suscripciones, vencimiento de documentos, informe mensual y recordatorio nocturno opcional de gastos en metálico (con explicación de permisos y ajustes de fiabilidad). |
| ⚙️ | **Configuración** | Notificaciones/recordatorio, tipos de cambio, exportación e informes, copias de seguridad y personalización del Inicio. Bloqueo de la app disponible (biometría/PIN/patrón del sistema).

## Privacidad y seguridad

- **Tus datos viven en tu móvil** (Room/almacenamiento local). Sin servidores propios ni cuentas obligatorias.
- **Sin conexión bancaria.** La app nunca accede a tu banco.
- **Sin datos de tarjeta sensibles.** No se guardan números, CVV ni IBAN.
- Documentos, tickets y copias de seguridad se **cifran** (AES-GCM; la copia local se cifra con contraseña).
- La copia de seguridad/exportación es **opcional** y la controlas tú. *Google Drive no está integrado (T4.3 pospuesta).*

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose (Material 3) |
| Navegación | Navigation Compose |
| Base de datos local | Room (SQLite) |
| Preferencias | DataStore |
| Inyección de dependencias | Hilt |
| OCR de tickets | ML Kit Text Recognition (en el dispositivo) |
| Tareas en segundo plano | WorkManager |
| Widget | Jetpack Glance |
| Copia de seguridad local | ZIP + cifrado (PBKDF2/AES-GCM) |
| Build | Android Gradle Plugin 9.5, Gradle 9.6, JDK 17 |

## Compilación

**Requisitos**

- Android Studio reciente (o Gradle desde línea de comandos)
- **JDK 17**
- Android SDK con plataforma instalada (el proyecto compila con `compileSdk` reciente; `minSdk 26`)

**Pasos**

```bash
git clone https://github.com/Vikksxz/MiPatrimonio.git
cd MiPatrimonio
```

1. Abre la carpeta en Android Studio y deja que Gradle sincronice.
2. Ejecuta en un dispositivo o emulador: `./gradlew installDebug` (o **Run**).
3. APK de depuración: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

**Release firmado (opcional):** la firma se configura desde `local.properties` (**no se sube al repositorio**) con `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. El archivo `.jks` de keystore y las contraseñas no deben commitearse.

## Estado del proyecto

- [x] Núcleo: movimientos, tarjetas, inicio/resumen, bloqueo
- [x] Captura: documentos cifrados, OCR de tickets, compartir desde otras apps, etiquetas/notas
- [x] Planificación: ahorro, presupuestos, suscripciones/recurrentes, notificaciones, análisis, recordatorio nocturno
- [x] Extras: exportación CSV/PDF e informe mensual, copia local cifrada, widget/atajo, multimoneda, gastos compartidos, dashboard y vencimiento de documentos
- [x] Pulido (T4.9): reorganización Cartera/Configuración, ajustes de UI, proguard y preparación de release
- [ ] **Pospuesto:** copia en Google Drive (T4.3)

## Contribuir

Las ideas y sugerencias son bienvenidas. Abre un [issue](../../issues) para proponer mejoras o reportar errores.

## Licencia

Pendiente de definir (no se ha añadido archivo `LICENSE` todavía).

## Autor

Hecho con cariño por **George**.
