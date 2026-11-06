# AGENTS.md — Manual de trabajo para agentes en MiPatrimonio

> Va en la **raíz del repositorio**, junto a `MiPatrimonio.md` y `opencode.json`. OpenCode lo carga como instrucciones del proyecto para todos los agentes.
> `MiPatrimonio.md` es la **especificación**. Este archivo define **cómo se trabaja**: el bucle de comprobación y **qué agente (y modelo) hace cada cosa**, según los agentes definidos en `opencode.json`.

---

## 1. Reglas que siempre se cumplen

1. Antes de cualquier tarea, lee `MiPatrimonio.md` **sección 0** y la **tarea indicada en la sección 18**, más las secciones que esa tarea necesite (mapa en la sección 4 de este archivo). No leas el documento entero salvo que George lo pida.
2. Lee también `PROGRESS.md` (sección 8) para conocer el estado del proyecto.
3. Una sola tarea cada vez. No empieces otra sin cerrar la actual con el bucle completo (sección 3).
4. No inventes librerías, artefactos, versiones ni APIs. Si no estás seguro, **busca en la documentación oficial o pregunta a George**. Si una dependencia no se resuelve, no pongas otra por intuición: informa del error.
5. Dinero en céntimos (`Long`). Textos de usuario en `strings.xml`. Nunca datos de tarjeta. Nombres de entidades y campos exactamente como en `MiPatrimonio.md` sección 6.
6. No toques archivos fuera del alcance de la tarea. Si crees que hace falta, propónlo al final.
7. Nunca escribas claves, tokens ni contraseñas en código ni commits. Van en variables de entorno o `local.properties` (en `.gitignore`).
8. No pegues datos personales reales de George en prompts ni en tests: usa datos de ejemplo.
9. Responde en español. Código e identificadores, en inglés.
10. **Los agentes de solo lectura no modifican nada** (ver sección 2.2): proponen cambios, y los aplica un agente con permiso de edición.

---

## 2. Agentes y modelos

### 2.1 Quién es quién

| Agente (`opencode.json`) | Modelo | Tipo | Especialidad en este proyecto |
|---|---|---|---|
| `developer` | glm-5.3-flash-free | principal, edita | Implementador por defecto: Room, ViewModels, Compose, navegación, workers, OCR |
| `deepseek-developer` | deepseek-v4.1-flash-free | principal, edita | Tareas grandes con muchos archivos y refactors extensos |
| `architect` | mimo-v2.6-flash-free | principal, solo lectura | Planificar cada tarea compleja: archivos, dependencias, riesgos |
| `debugger` | mimo-v2.6-flash-free | subagente, solo lectura | Causa raíz de errores que persisten tras 2 intentos |
| `code-reviewer` | deepseek-v4.1-flash-free | subagente, solo lectura | Revisión cruzada de cada tarea (paso 8) |
| `financial-auditor` | mimo-v2.6-flash-free | subagente, solo lectura | Cálculos de dinero: saldos, ahorro, presupuestos, redondeos, duplicados, casos límite |
| `reasoning-expert` | big-pickle | subagente, solo lectura | Seguridad, cifrado, decisiones de diseño difíciles y segunda opinión |
| `ui-reviewer` | nemotron-3.5-super-vl-free | subagente, solo lectura | Comparar capturas de la app con el diseño (sección 5 de `MiPatrimonio.md`) y el mockup |
| `visual-analyst` | jev-1.13-free | subagente, solo lectura | Respaldo visual si `ui-reviewer` no está disponible (ver nota 2.3) |
| `fast-worker` | nemotron-3.5-lightning-free | subagente, edita | `strings.xml`, búsquedas en el código, cambios pequeños y repetitivos |
| `utility-developer` | flint-40b-free | subagente, edita | Utilidades, scripts, tests aislados, datos de ejemplo, exportador CSV |
| `general-assistant` | beam-free | subagente, edita | Respaldo generalista; candidato a segundo implementador (ver nota 2.3) |

### 2.2 Cómo se usan
- **Agentes principales:** se cambia de uno a otro en la interfaz de OpenCode (tecla Tab). Trabajan en la sesión de George.
- **Subagentes:** se invocan con `@nombre` en el mensaje, o los llama `developer` mediante la herramienta de tareas.
- **Un subagente no ve la conversación.** Cada vez hay que pasarle: la tarea, el criterio de aceptación, el `git diff` o los archivos relevantes, y la lista de conformidad del paso 7.
- **Agentes de solo lectura** (`architect`, `debugger`, `code-reviewer`, `financial-auditor`, `reasoning-expert`, `ui-reviewer`, `visual-analyst`): no pueden editar archivos. Tres de ellos (`architect`, `debugger`, `reasoning-expert`) tienen acceso a la terminal, así que además se les prohíbe expresamente ejecutar comandos que escriban o modifiquen archivos (`sed -i`, redirecciones `>`/`>>`, `git commit`, `git checkout`, `rm`, `mv`, etc.). Solo pueden usar comandos de lectura (`git diff`, `git log`, `git status`, `grep`, `ls`, `cat`) y `./gradlew` para compilar y probar.

### 2.3 Reglas del reparto
- **Quien implementa no revisa su propio trabajo, y tampoco otro agente con el mismo modelo.** Comparten modelo: `architect`, `debugger` y `financial-auditor` (mimo); `deepseek-developer` y `code-reviewer` (deepseek). Si implementa `deepseek-developer`, revisa `reasoning-expert` en lugar de `code-reviewer`.
- Las tareas con 🔒 (dinero, seguridad, cifrado, datos) pasan siempre por `financial-auditor` (si hay cálculos de dinero) y/o `reasoning-expert` (si hay seguridad), además de `code-reviewer`.
- Las tareas con 🖼 (interfaz) pasan por `ui-reviewer` con **capturas de pantalla** del móvil o del emulador, comparadas con la sección 5 de `MiPatrimonio.md`.
- **Nota sobre `beam-free`:** según fuentes de terceros, "Beam" es un modelo de programación agéntica de gran tamaño con muy buenos resultados en pruebas de ingeniería de software y disponible gratis. Está definido como "general-assistant", pero podría rendir mejor que el resto como implementador. Se comprueba en la tarea T1.5 (sección 7) y, si gana, se promueve a `developer` o a segundo implementador.
- **Nota sobre `jev-1.13-free`:** una fuente de terceros describe los modelos "JEV" como clasificadores que emiten pocos tokens, no como analistas de imágenes. Antes de depender de `visual-analyst`, George debe comprobar que acepta imágenes; si no, se elimina del `opencode.json` y se usa solo `ui-reviewer`.
- **Disponibilidad:** los modelos gratuitos rotan y pueden desaparecer sin aviso. Antes de empezar y de vez en cuando, ejecutar `opencode models` y comprobar que los identificadores de `opencode.json` siguen existiendo.
- **Orden de sustitución** si un modelo falla o alcanza su límite: `developer` → `general-assistant` → `deepseek-developer` → proveedor de reserva (NVIDIA NIM, elegir un modelo de código con soporte de herramientas), manteniendo la regla de que quien implementa no revisa.

---

## 3. Bucle de comprobación (se ejecuta en CADA tarea)

```
0 PREPARAR → 1 LEER → 2 PLANIFICAR → 3 IMPLEMENTAR → 4 AUTOEVALUAR
                                                         │
10 CERRAR ← 9 PROBAR EN MÓVIL ← 8 REVISAR ← 7 CONFORMIDAD ← 6 TESTS ← 5 COMPILAR
                                    │
              (si algo falla, volver al paso 3: máx. 3 intentos, luego escalar)
```

| Paso | Qué se hace | Agente |
|---|---|---|
| 0. Preparar | `git status` limpio; rama `tX-Y-nombre`; confirmar tarea con George | George / `developer` |
| 1. Leer | Sección 0, tarea (sección 18), secciones del mapa (sección 4) y `PROGRESS.md` | implementador |
| 2. Planificar | Lista de archivos a crear/modificar, dependencias (nombres verificados), cómo se cumple el criterio de aceptación, dudas para George. **En las tareas marcadas "plan: architect" lo hace `architect`**; en las demás, el propio implementador | `architect` o implementador |
| 3. Implementar | Cambios mínimos, código completo con imports, sin `TODO()`, `@Preview` en pantallas, tests de la tarea | implementador |
| 4. Autoevaluar | Buscar `Float`/`Double` en importes, textos literales en Composables, imports inexistentes, nombres distintos a la sección 6 | implementador |
| 5. Compilar | `./gradlew assembleDebug`. Leer el **primer** error. Máx. 3 intentos; al 3.º fallo se invoca `debugger` y se aplica la regla de escalado (sección 6) | implementador / `debugger` |
| 6. Tests | `./gradlew testDebugUnitTest`. No borrar ni desactivar tests. Los instrumentados los lanza George | implementador |
| 7. Conformidad | Lista de 12 puntos (abajo) | implementador |
| 8. Revisar | Revisión cruzada + especialistas según la tabla de tareas (sección 7) | `code-reviewer`, `financial-auditor`, `reasoning-expert`, `ui-reviewer` |
| 9. Probar en móvil | El agente redacta 3–6 pasos concretos; George instala (`./gradlew installDebug`) y confirma | George |
| 10. Cerrar | Commit `feat(tX.Y): ...`, marcar `[x]` en la sección 18 de `MiPatrimonio.md`, actualizar `PROGRESS.md`, informe de 5 líneas | `developer` / implementador |

### Lista de conformidad (paso 7)
- [ ] ¿Se cumple el criterio de aceptación de la tarea (sección 18)?
- [ ] ¿Los importes son `Long` en céntimos y se formatean con `Money`?
- [ ] ¿Los textos de usuario están en `strings.xml` en español?
- [ ] ¿Las entidades y campos coinciden con la sección 6?
- [ ] ¿Los colores, fuentes y formas salen del tema (sección 5), sin colores sueltos en pantallas?
- [ ] ¿Los colores por tipo son los definidos (metálico ámbar, tarjeta cian, suscripciones violeta, ingresos verde)?
- [ ] ¿No se guarda ningún número de tarjeta ni dato bancario, ni se imprime en logs?
- [ ] ¿Las dependencias nuevas están en `libs.versions.toml` y son estables?
- [ ] ¿No hay librerías o funciones que no estén en la especificación?
- [ ] ¿La lógica de negocio está en `domain/usecase` y no en Composables ni DAOs?
- [ ] ¿Hay `contentDescription` en iconos y zonas táctiles de al menos 44 dp?
- [ ] ¿Se respeta la reducción de movimiento si hay animaciones?

---

## 4. Mapa de contexto: qué leer según la tarea

Pasa al agente **solo** estas secciones de `MiPatrimonio.md` (más la sección 0 y la tarea en 18):

| Tipo de tarea | Secciones |
|---|---|
| Proyecto base, Gradle, estructura (T1.1, T1.3) | 3, 4 |
| Tema y componentes visuales (T1.2, T3.2) | 5 |
| Base de datos (T1.4) | 4, 6 |
| `Money` y formatos (T1.5) | 7.10 |
| Tarjetas (T1.6) | 5.5, 6, 8.5 |
| Movimientos y añadir (T1.7, T1.8) | 6, 8.2, 8.3 |
| Inicio (T1.9) | 5, 7.1, 8.1 |
| Bloqueo y seguridad (T1.10) | 14 |
| Cifrado y documentos (T2.1, T2.2) | 11, 14 |
| OCR, parser, escaneo (T2.3–T2.5) | 9 |
| Compartir desde CamScanner (T2.6) | 9.4, 10, 8.6 |
| Etiquetas (T2.7) | 6, 8.2 |
| Ahorro y presupuestos (T3.1, T3.3, T3.4) | 6, 7.2, 7.3, 7.4, 8.4 |
| Recurrentes y notificaciones (T3.5, T3.6, T3.8) | 7.6, 12 |
| Análisis (T3.7) | 7.5, 7.7, 8.2 |
| Exportación y copias (T4.1–T4.3) | 13, 14, 15 |
| Widget y extras (T4.4, T4.8) | 16 |
| Informe, multimoneda, compartidos (T4.5–T4.7) | 7.8, 7.9, 8.7, 8.8 |

---

## 5. Prompts base por agente

**`architect` (paso 2):**
```
Eres arquitecto Android (Kotlin, Compose, Room). Tarea: <tarea>. Criterio: <criterio>.
Lee MiPatrimonio.md (sección 0, la tarea y las secciones: <lista>) y PROGRESS.md.
NO modifiques archivos ni ejecutes comandos que escriban. Responde con:
1) Archivos a crear y a modificar. 2) Dependencias nuevas (nombre exacto de artefacto; indica si no estás seguro de que exista).
3) Riesgos y casos límite. 4) Orden de implementación en pasos pequeños. 5) Dudas para George.
```

**`code-reviewer` (paso 8):**
```
Eres revisor de código Android. Tarea: <tarea>. Criterio: <criterio>.
Revisa el diff y responde SOLO con:
1) Errores que impedirían compilar o ejecutar. 2) Incumplimientos de MiPatrimonio.md (usa la lista adjunta).
3) Riesgos de seguridad o pérdida de datos. 4) Mejoras menores (máx. 3).
Indica archivo y línea. Si algo es suposición, dilo. No reescribas el código entero.
<git diff> <lista de conformidad>
```

**`financial-auditor` (tareas con dinero):**
```
Eres auditor de lógica financiera. Revisa el diff. Comprueba: importes en céntimos (Long), redondeos,
signo (gasto/ingreso), saldo = ingresos − gastos − aportaciones, divisiones por cero, meses sin datos,
duplicados, fechas límite (fin de mes, 29/30/31, año bisiesto), umbrales (80 % y 100 % una vez al mes),
estimación de ahorro (ceil, restante = 0, cuota = 0). Propón al menos 5 casos de prueba concretos con resultado esperado.
No modifiques archivos.
```

**`reasoning-expert` (seguridad y diseño):**
```
Revisa desde el punto de vista de seguridad y robustez: cifrado (AES-GCM, IV único por archivo, claves en Keystore),
archivos temporales descifrados, permisos, intents recibidos, logs con datos sensibles, FLAG_SECURE,
pérdida de datos en migraciones o restauraciones. Indica riesgo, impacto y corrección propuesta. No modifiques archivos.
```

**`ui-reviewer` (tareas 🖼):**
```
Compara la captura adjunta con MiPatrimonio.md sección 5 y 8 (colores, tipografía, jerarquía, brillo neón, zonas táctiles,
contraste, textos). Lista diferencias concretas (qué elemento, qué se esperaba, qué se ve). No modifiques archivos.
```

**`debugger` (errores persistentes):**
```
Analiza la causa raíz del siguiente error de compilación o ejecución. Lee los archivos implicados.
Responde con: causa probable, evidencia (archivo y línea), solución mínima propuesta y cómo verificarla.
No modifiques archivos ni ejecutes comandos que escriban. Error (texto exacto): <...> Lo ya intentado: <...>
```

---

## 6. Escalado y traspaso

### Cuándo escalar
- **3 fallos de compilación o tests** con el mismo error → invocar `debugger`; si aun así no se resuelve, cambiar de implementador.
- **Mismo error dos veces** tras "arreglarlo" → el agente debe citar la documentación oficial antes de continuar.
- **Error 429 o límite de uso** → esperar unos minutos o pasar al siguiente de la lista de sustitución (sección 2.3). No reintentar en bucle.
- **El agente contradice `MiPatrimonio.md`** → parar, citar la sección y pedir corrección.
- **El agente inventa una librería o función** → pedir enlace a la documentación; si no lo da, descartar.

### Plantilla de traspaso (al cambiar de agente o modelo)
```
TRASPASO — Tarea <Tx.y>: <nombre>
Estado: <qué está hecho>
Archivos tocados: <lista>
Error actual (texto exacto, primeras 20 líneas): <...>
Lo ya intentado: <1., 2., 3.>
Restricciones: leer MiPatrimonio.md sección 0 y esta tarea; no cambiar nombres de la sección 6.
Qué necesito de ti: <arreglar / revisar / continuar desde X>
```

### Cuándo pedir ayuda a George
- Decisiones de producto no recogidas (sección 21 de `MiPatrimonio.md`).
- Configuración externa: Google Cloud/Drive, firma del APK, permisos del móvil.
- Algo funciona en el emulador pero no en el RedMagic (posible ahorro de batería del sistema).

---

## 7. Reparto de tareas

Leyenda: **ARQ** architect · **DEV** developer · **DSK** deepseek-developer · **REV** code-reviewer · **FIN** financial-auditor · **RSN** reasoning-expert · **UI** ui-reviewer · **DBG** debugger · **FAST** fast-worker · **UTIL** utility-developer · **GEN** general-assistant.
"Plan" indica quién planifica (paso 2). Si pone "—", planifica el propio implementador.
La asignación es inicial y se **ajusta tras la tarea T1.5**.

### Fase 1 — Núcleo
| Tarea | Plan | Implementa | Revisan |
|---|---|---|---|
| T1.1 Proyecto base | ARQ | DEV | REV (si falla Gradle: DBG) |
| T1.2 Tema neón 🖼 | — | DEV | REV, UI |
| T1.3 Navegación 🖼 | — | DEV | REV, UI |
| T1.4 Base de datos 🔒 | ARQ | DSK | RSN (no REV: mismo modelo que DSK) |
| **T1.5 `Money` (prueba comparativa)** 🔒 | — | **DEV, DSK y GEN, cada uno por separado** | FIN audita las tres; George elige |
| T1.6 Tarjetas 🖼 | — | DEV | REV, UI |
| T1.7 Añadir movimiento 🔒 | — | DEV | REV, FIN |
| T1.8 Lista de movimientos | — | DEV (strings: FAST) | REV |
| T1.9 Inicio 🔒🖼 | — | DEV | REV, FIN, UI |
| T1.10 Bloqueo 🔒 | — | DEV | RSN, REV |

### Fase 2 — Captura
| Tarea | Plan | Implementa | Revisan |
|---|---|---|---|
| T2.1 `CryptoManager` 🔒 | RSN | DEV | RSN, REV |
| T2.2 Documentos 🔒 | — | DEV | RSN, REV |
| T2.3 OCR base | — | DEV | REV |
| T2.4 `ReceiptParser` 🔒 | — | DEV (parser) + UTIL (textos de ejemplo y tests) | REV, FIN |
| T2.5 Escaneo de ticket 🖼 | — | DEV | REV, UI |
| T2.6 Compartir desde CamScanner 🔒 | ARQ | DEV | RSN, REV |
| T2.7 Etiquetas y notas | — | FAST | REV |

### Fase 3 — Planificación
| Tarea | Plan | Implementa | Revisan |
|---|---|---|---|
| T3.1 Planes de ahorro 🔒🖼 | ARQ | DEV | FIN, REV, UI |
| T3.2 `ProgressNeonBar` 🖼 | — | DEV | UI, REV |
| T3.3 Varios planes y prioridad | — | FAST | REV |
| T3.4 Presupuestos 🔒 | — | DEV | FIN, REV |
| T3.5 Recurrentes y suscripciones 🔒 | ARQ | DEV (fechas: DBG si hay bugs) | FIN, REV |
| T3.6 Notificaciones | — | DEV (textos: FAST) | REV |
| T3.7 Análisis 🔒🖼 | — | DEV | FIN, UI, REV |
| T3.8 Recordatorio nocturno | — | FAST | REV |

### Fase 4 — Extras
| Tarea | Plan | Implementa | Revisan |
|---|---|---|---|
| T4.1 Exportación CSV y PDF | — | UTIL (CSV) + DEV (PDF) | REV |
| T4.2 Copia local cifrada 🔒 | RSN | DEV | RSN, REV |
| T4.3 Copia en Drive 🔒 | ARQ | DEV | RSN, REV |
| T4.4 Widget Glance 🖼 | — | DEV | UI, REV |
| T4.5 Informe mensual 🔒 | — | DEV | FIN, REV |
| T4.6 Multimoneda 🔒 | — | DEV | FIN, REV |
| T4.7 Gastos compartidos 🔒 | ARQ | DEV | FIN, REV |
| T4.8 Dashboard personalizable 🖼 | — | DSK (refactor grande) | RSN, UI |
| T4.9 Pulido y revisión global | ARQ | DEV (cambios) | RSN (revisión global), UI, REV |

---

## 8. `PROGRESS.md` (memoria del proyecto)

Los modelos no recuerdan sesiones anteriores. Crea y mantén este archivo en la raíz:

```markdown
# PROGRESS

## Estado
- Fase actual: 1
- Última tarea cerrada: T1.4
- Tarea en curso: T1.5

## Decisiones tomadas
- (fecha) Paquete: com.mipatrimonio.app
- (fecha) Implementador principal tras T1.5: <agente/modelo> porque <motivo>

## Modelos disponibles (resultado de `opencode models`)
- (fecha) <lista>

## Dependencias (nombres exactos; versiones en libs.versions.toml)
- Room, Hilt, Navigation Compose, DataStore, ...

## Problemas conocidos
- (tarea) descripción y cómo se resolvió o esquivó

## Mejoras menores pendientes (de revisiones)
- ...

## Notas para el próximo agente
- ...
```

El agente actualiza este archivo en el paso 10 de cada tarea.

---

## 9. Comandos útiles

```
opencode models                      # modelos disponibles ahora mismo
./gradlew assembleDebug              # compilar
./gradlew testDebugUnitTest          # tests unitarios
./gradlew connectedDebugAndroidTest  # tests instrumentados (móvil conectado)
./gradlew installDebug               # instalar en el móvil
./gradlew lintDebug                  # análisis estático
git diff --stat                      # resumen de cambios
adb logcat -s MiPatrimonio           # logs de la app (sin datos personales)
```

---

## 10. Lo que NUNCA debe hacer ningún agente

- Guardar números de tarjeta, CVV, IBAN o credenciales bancarias.
- Usar `Float` o `Double` para importes.
- Añadir analíticas, anuncios o librerías de rastreo.
- Borrar o desactivar tests para que pase la compilación.
- Usar `fallbackToDestructiveMigration` fuera de desarrollo.
- Subir claves, tokens o `local.properties` al repositorio.
- Cambiar nombres de entidades, campos, colores o rutas de `MiPatrimonio.md` sin permiso.
- Empezar una tarea sin haber cerrado la anterior con el bucle completo.
- Dar por buena una dependencia o API sin verificarla.
- (Agentes de solo lectura) Escribir o modificar archivos, ni siquiera a través de la terminal.