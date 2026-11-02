# AGENTS.md — Manual de trabajo para agentes en MiPatrimonio

> Este archivo va en la **raíz del repositorio**, junto a `MiPatrimonio.md`. OpenCode lo carga automáticamente como instrucciones del proyecto.
> `MiPatrimonio.md` es la **especificación**. Este archivo define **cómo se trabaja**: el bucle de comprobación y qué modelo hace qué.

---

## 1. Reglas que siempre se cumplen

1. Antes de cualquier tarea, lee `MiPatrimonio.md` **sección 0** y la **tarea indicada en la sección 18**, más las secciones que esa tarea necesite (ver el mapa de la sección 4 de este archivo). No leas el documento entero salvo que George lo pida.
2. Lee también `PROGRESS.md` (sección 7 de este archivo) para saber en qué estado está el proyecto.
3. Una sola tarea cada vez. No empieces otra sin cerrar la actual con el bucle completo (sección 3).
4. No inventes librerías, artefactos, versiones ni APIs. Si no estás seguro, **busca en la documentación oficial o pregunta a George**. Si una dependencia no se resuelve, no pongas otra por intuición: informa del error.
5. Dinero en céntimos (`Long`). Textos de usuario en `strings.xml`. Nunca datos de tarjeta. Los nombres de entidades y campos son los de `MiPatrimonio.md` sección 6.
6. No toques archivos fuera del alcance de la tarea. Si crees que hace falta, propónlo al final.
7. Nunca escribas claves, tokens ni contraseñas en el código ni en los commits. Las claves van en variables de entorno o en `local.properties` (que debe estar en `.gitignore`).
8. No pegues datos personales reales de George en prompts ni en tests: usa datos de ejemplo.
9. Responde en español. El código y los identificadores, en inglés.

---

## 2. Reparto de modelos

Los modelos son gratuitos, tienen límites de uso y contexto, y su calidad relativa todavía no está contrastada en este proyecto. El reparto es un punto de partida: **se revisa después de la tarea T1.5** (prueba comparativa, sección 6).

| Modelo | Rol | Úsalo para | No lo uses para |
|---|---|---|---|
| **GLM-5.3-Flash** | Implementador principal | Tareas con varios archivos: Room, ViewModels, navegación, Compose, workers, OCR, backups; arreglar errores de compilación | Redacción larga de documentación |
| **Big Pickle** | Revisor y segunda opinión | Revisión cruzada de lo que implementa otro modelo; errores que GLM no resuelve tras 2 intentos; seguridad y cifrado; cálculos de dinero | Tareas largas de implementación (puede cambiar o desaparecer sin aviso) |
| **Nemotron 3.5** | Tareas ligeras | `strings.xml`, tests sencillos, KDoc/documentación, refactors mecánicos, mover o renombrar archivos, datos de ejemplo para tests | Lógica de negocio, cifrado, migraciones de base de datos |
| **NVIDIA NIM (reserva)** | Respaldo | Cuando OpenCode alcance su límite o un modelo no responda. Elegir en el catálogo un modelo orientado a código con soporte de herramientas | Bucles largos de agente (límite aproximado de 40 peticiones por minuto: puede devolver errores 429) |

**Reglas del reparto:**
- Quien implementa **no** revisa su propio trabajo. La revisión la hace otro modelo.
- Las tareas de **seguridad, cifrado, dinero y cálculos** (marcadas con 🔒 en la tabla) siempre pasan revisión de Big Pickle.
- Si un modelo falla 3 veces seguidas con el mismo error, **se cambia de modelo** usando la plantilla de traspaso (sección 5).
- No cambies de modelo a mitad de una tarea sin traspaso por escrito.

---

## 3. Bucle de comprobación (se ejecuta en CADA tarea)

```
┌─ 0. PREPARAR ──► 1. LEER ──► 2. PLANIFICAR ──► 3. IMPLEMENTAR
│                                                      │
│   10. CERRAR ◄── 9. PROBAR EN EL MÓVIL ◄── 8. REVISAR (otro modelo)
│                                                      ▲
│                      7. CONFORMIDAD ◄── 6. TESTS ◄── 5. COMPILAR ◄──┘
└────────────── si algo falla: volver al paso 3 (máx. 3 intentos) ──────────────
```

### Paso 0 — Preparar
- `git status` limpio. Crear rama: `git checkout -b tX-Y-nombre` (por ejemplo, `t1-4-base-datos`).
- Confirmar con George el número de tarea.

### Paso 1 — Leer
- `MiPatrimonio.md` sección 0, la tarea en sección 18 y las secciones del mapa (sección 4 de este archivo).
- `PROGRESS.md`.

### Paso 2 — Planificar (antes de escribir código)
Escribe en 5–10 líneas:
- Archivos que vas a **crear** y **modificar**.
- Dependencias nuevas (nombre exacto del artefacto; verifica que existe).
- Cómo se cumplirá el **criterio de aceptación** de la tarea.
- Dudas para George (si las hay, pregunta ahora, no después).

### Paso 3 — Implementar
- Cambios mínimos para cumplir la tarea. Código completo, con imports, sin `TODO()`.
- Añadir `@Preview` en pantallas de Compose.
- Escribir los tests que indica la tarea.

### Paso 4 — (reservado) Autoevaluación rápida
Antes de compilar, repasa el código buscando: `Float`/`Double` en importes, textos literales en Composables, imports inexistentes, nombres distintos de los de la sección 6.

### Paso 5 — Compilar
```
./gradlew assembleDebug
```
- Si falla: lee el **primer** error (los demás suelen ser consecuencia), corrige, vuelve a compilar.
- **Máximo 3 intentos.** Si sigue fallando, aplica la regla de escalado (sección 5).

### Paso 6 — Tests
```
./gradlew testDebugUnitTest
```
- Todos los tests deben pasar. No borres ni desactives tests para que pasen.
- Los tests instrumentados (`connectedDebugAndroidTest`) los lanza George con el móvil conectado, cuando la tarea lo indique.

### Paso 7 — Lista de conformidad con `MiPatrimonio.md`
Responde **sí/no** a cada punto y corrige los "no":

- [ ] ¿Se cumple el criterio de aceptación de la tarea (sección 18)?
- [ ] ¿Los importes son `Long` en céntimos y se formatean con `Money`?
- [ ] ¿Los textos de usuario están en `strings.xml` en español?
- [ ] ¿Las entidades y campos coinciden con la sección 6?
- [ ] ¿Los colores, fuentes y formas salen del tema (sección 5), sin colores sueltos en las pantallas?
- [ ] ¿Los colores por tipo de movimiento son los definidos (metálico ámbar, tarjeta cian, suscripciones violeta, ingresos verde)?
- [ ] ¿No se guarda ningún número de tarjeta ni dato bancario, ni se imprime en logs?
- [ ] ¿Las dependencias nuevas están en `libs.versions.toml` y son estables?
- [ ] ¿No hay librerías o funciones que no estén en la especificación?
- [ ] ¿La lógica de negocio está en `domain/usecase` y no en Composables ni DAOs?
- [ ] ¿Hay `contentDescription` en iconos y zonas táctiles de al menos 44 dp?
- [ ] ¿Se ha respetado la reducción de movimiento si hay animaciones?

### Paso 8 — Revisión cruzada (otro modelo)
Abre una sesión nueva con el modelo revisor (ver tabla de tareas). Pásale **solo**: la tarea, el criterio de aceptación, el `git diff` y la lista del paso 7. Prompt:

```
Eres revisor de código Android (Kotlin, Compose, Room). Tarea: <tarea>.
Criterio de aceptación: <criterio>.
Revisa el diff siguiente y responde SOLO con:
1) Errores que impedirían compilar o ejecutar.
2) Incumplimientos de MiPatrimonio.md (usa la lista adjunta).
3) Riesgos de seguridad o de pérdida de datos.
4) Mejoras menores (máx. 3).
No reescribas el código entero; indica archivo y línea. Si algo es una suposición, dilo.
<git diff>
<lista del paso 7>
```
- Los puntos de las categorías 1–3 se corrigen y se vuelve al paso 5.
- Las mejoras menores se anotan en `PROGRESS.md` si no se aplican.

### Paso 9 — Prueba en el móvil (George)
El agente escribe una lista de **3–6 pasos concretos** para probar la tarea en el RedMagic (por ejemplo, "Abre Cartera → Añadir tarjeta → elige CaixaBank, variante Imagin → guarda → comprueba que aparece"). George instala (`./gradlew installDebug`) y confirma o describe el fallo.

### Paso 10 — Cerrar
- `git add -A && git commit -m "feat(tX.Y): descripción corta"`.
- Marcar `[x]` la tarea en `MiPatrimonio.md` sección 18.
- Actualizar `PROGRESS.md` (sección 7).
- Informe final de 5 líneas: qué se hizo, archivos tocados, tests, decisiones tomadas, pendientes.

---

## 4. Mapa de contexto: qué leer según la tarea

Pasa al modelo **solo** estas secciones de `MiPatrimonio.md` (más la sección 0 y la tarea en 18):

| Tipo de tarea | Secciones a leer |
|---|---|
| Proyecto base, Gradle, estructura (T1.1, T1.3) | 3, 4 |
| Tema y componentes visuales (T1.2, T3.2) | 5 |
| Base de datos y modelos (T1.4) | 4, 6 |
| `Money` y formatos (T1.5) | 7.10 |
| Tarjetas (T1.6) | 5.5, 6, 8.5 |
| Movimientos y añadir (T1.7, T1.8) | 6, 8.2, 8.3 |
| Inicio (T1.9) | 5, 7.1, 8.1 |
| Bloqueo y seguridad (T1.10) | 14 |
| Cifrado y documentos (T2.1, T2.2) | 11, 14 |
| OCR, parser, escaneo (T2.3, T2.4, T2.5) | 9 |
| Compartir desde CamScanner (T2.6) | 9.4, 10, 8.6 |
| Etiquetas (T2.7) | 6, 8.2 |
| Ahorro y presupuestos (T3.1, T3.3, T3.4) | 6, 7.2, 7.3, 7.4, 8.4 |
| Recurrentes y notificaciones (T3.5, T3.6, T3.8) | 7.6, 12 |
| Análisis (T3.7) | 7.5, 7.7, 8.2 |
| Exportación y copias (T4.1–T4.3) | 13, 15, 14 |
| Widget y extras (T4.4, T4.8) | 16 |
| Informe, multimoneda, compartidos (T4.5–T4.7) | 7.8, 7.9, 8.7, 8.8 |

---

## 5. Escalado y traspaso entre modelos

### Cuándo escalar
- **3 fallos de compilación o de tests** con el mismo error → cambiar de modelo.
- **Mismo error dos veces** tras "arreglarlo" → pedir al modelo que cite la documentación oficial antes de seguir.
- **Error 429 / límite de uso** → esperar unos minutos o pasar al proveedor de reserva (NVIDIA NIM). No reintentar en bucle.
- **El modelo propone algo que contradice `MiPatrimonio.md`** → parar, citar la sección y pedir que lo corrija.
- **El modelo inventa una librería o función** → pedir enlace a la documentación; si no lo da, descartar la propuesta.

### Plantilla de traspaso (pegar al nuevo modelo)
```
TRASPASO — Tarea <Tx.y>: <nombre>
Estado: <qué está hecho>
Archivos tocados: <lista>
Error actual (texto exacto, primeras 20 líneas): <...>
Lo que ya se intentó: <1., 2., 3.>
Restricciones: leer MiPatrimonio.md sección 0 y esta tarea; no cambiar nombres de la sección 6.
Qué necesito de ti: <arreglar el error / revisar / continuar desde X>
```

### Cuándo pedir ayuda a George
- Decisiones de producto no recogidas en `MiPatrimonio.md` (sección 21 tiene las abiertas).
- Configuración externa: Google Cloud/Drive (sección 13.3), firma del APK, permisos del móvil.
- Cuando algo funciona en el emulador pero no en el RedMagic (posible ahorro de batería del sistema, sección 12).

---

## 6. Reparto de tareas por modelo

Leyenda: **GLM** = GLM-5.3-Flash · **BP** = Big Pickle · **NEM** = Nemotron 3.5 · 🔒 = pasa siempre revisión de BP.

### Fase 1 — Núcleo
| Tarea | Implementa | Revisa | Notas |
|---|---|---|---|
| T1.1 Proyecto base | GLM | BP | Gradle y version catalog: cuidado con versiones y KSP |
| T1.2 Tema neón | GLM | NEM | Verificar colores contra sección 5.1 |
| T1.3 Navegación | GLM | NEM | |
| T1.4 Base de datos 🔒 | GLM | BP | Entidades exactas de la sección 6; `exportSchema = true` |
| **T1.5 `Money` — prueba comparativa** | **GLM, BP y NEM (por separado)** | George | Misma tarea a los tres; elige el que compile a la primera, escriba mejores tests y invente menos. **Ajusta esta tabla con el resultado** |
| T1.6 Tarjetas | GLM | NEM | Foto con `PickVisualMedia`, copiada a almacenamiento privado |
| T1.7 Añadir movimiento 🔒 | GLM | BP | Validación de importes |
| T1.8 Lista de movimientos | GLM | NEM | |
| T1.9 Inicio 🔒 | GLM | BP | Cálculo del saldo con tests |
| T1.10 Bloqueo 🔒 | GLM | BP | |

### Fase 2 — Captura
| Tarea | Implementa | Revisa | Notas |
|---|---|---|---|
| T2.1 `CryptoManager` 🔒 | GLM | BP | No usar `security-crypto` (deprecada) |
| T2.2 Documentos 🔒 | GLM | BP | Archivos temporales descifrados: borrarlos al salir |
| T2.3 OCR base | GLM | BP | |
| T2.4 `ReceiptParser` 🔒 | GLM (parser) + NEM (textos de ejemplo) | BP | ≥ 8 casos de test; los textos de ejemplo no deben contener datos reales |
| T2.5 Escaneo de ticket | GLM | BP | |
| T2.6 Compartir desde CamScanner 🔒 | GLM | BP | Copiar el archivo antes de mostrar nada |
| T2.7 Etiquetas y notas | NEM | GLM | |

### Fase 3 — Planificación
| Tarea | Implementa | Revisa | Notas |
|---|---|---|---|
| T3.1 Planes de ahorro 🔒 | GLM | BP | Objetivo de solo lectura en la tarjeta; se edita en "Editar plan" |
| T3.2 `ProgressNeonBar` | GLM | NEM | Respetar reducción de movimiento |
| T3.3 Varios planes y prioridad | NEM | GLM | |
| T3.4 Presupuestos 🔒 | GLM | BP | Umbrales una vez al mes |
| T3.5 Recurrentes y suscripciones 🔒 | GLM | BP | Probar huecos de varios días |
| T3.6 Notificaciones | GLM | NEM | Permiso en Android 13+ |
| T3.7 Análisis 🔒 | GLM | BP | |
| T3.8 Recordatorio nocturno | NEM | GLM | |

### Fase 4 — Extras
| Tarea | Implementa | Revisa | Notas |
|---|---|---|---|
| T4.1 Exportación CSV y PDF | NEM (CSV) + GLM (PDF) | BP | CSV con BOM y `;` |
| T4.2 Copia local cifrada 🔒 | GLM | BP | Clave derivada de contraseña, no de Keystore |
| T4.3 Copia en Drive 🔒 | GLM | BP | Requiere configuración de Google Cloud por George |
| T4.4 Widget Glance | GLM | NEM | |
| T4.5 Informe mensual | GLM | NEM | |
| T4.6 Multimoneda 🔒 | NEM | BP | Redondeo en céntimos |
| T4.7 Gastos compartidos 🔒 | GLM | BP | Algoritmo de saldos con tests |
| T4.8 Dashboard personalizable y vencimientos | GLM | NEM | |
| T4.9 Pulido y revisión global | BP | GLM | Accesibilidad, rendimiento, pruebas en Android 8/12/15 |

**Si un modelo está no disponible o con límite:** sustituirlo por el siguiente de la lista GLM → BP → NEM → NVIDIA NIM, manteniendo la regla de que quien implementa no revisa.

---

## 7. `PROGRESS.md` (memoria del proyecto entre sesiones)

Los modelos no recuerdan sesiones anteriores. Crea y mantén este archivo en la raíz del repositorio. Plantilla:

```markdown
# PROGRESS

## Estado
- Fase actual: 1
- Última tarea cerrada: T1.4
- Tarea en curso: T1.5

## Decisiones tomadas
- (fecha) Paquete: com.mipatrimonio.app
- (fecha) Modelo principal tras T1.5: <modelo> porque <motivo>

## Dependencias (nombres exactos, versiones en libs.versions.toml)
- Room, Hilt, Navigation Compose, DataStore, ...

## Problemas conocidos
- (tarea) descripción y cómo se resolvió o se esquivó

## Mejoras menores pendientes (de revisiones)
- ...

## Notas para el próximo modelo
- ...
```

Cada vez que se cierra una tarea (paso 10), el agente actualiza este archivo.

---

## 8. Comandos útiles

```
./gradlew assembleDebug              # compilar
./gradlew testDebugUnitTest          # tests unitarios
./gradlew connectedDebugAndroidTest  # tests instrumentados (móvil conectado)
./gradlew installDebug               # instalar en el móvil
./gradlew lintDebug                  # análisis estático
git diff --stat                      # resumen de cambios
adb logcat -s MiPatrimonio           # logs de la app (sin datos personales)
```

---

## 9. Lo que NUNCA debe hacer el agente

- Guardar números de tarjeta, CVV, IBAN o credenciales bancarias en ningún sitio.
- Usar `Float` o `Double` para importes.
- Añadir analíticas, anuncios o librerías de rastreo.
- Borrar o desactivar tests para que pase la compilación.
- Usar `fallbackToDestructiveMigration` fuera de desarrollo.
- Subir claves, tokens o `local.properties` al repositorio.
- Cambiar nombres de entidades, campos, colores o rutas definidos en `MiPatrimonio.md` sin permiso.
- Empezar una tarea nueva sin haber cerrado la anterior con el bucle completo.
- Dar por buena una dependencia o API que no ha verificado.
