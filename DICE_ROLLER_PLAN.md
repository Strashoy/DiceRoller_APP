# Dice Roller — Plan de migración desde *Lanzador Dado Legendario*

> La historia del desarrollo (problemas, soluciones y cómo surgió cada feature) está en
> [BITACORA_DE_DESARROLLO.md](BITACORA_DE_DESARROLLO.md).

> Documento vivo. Describe qué se toma del proyecto anterior (`../Lanzador-Dado-Legendario`),
> qué se reemplaza por librerías, qué es nuevo y **por qué** se eligió cada recurso.

## 1. Objetivo

Un lanzador de **múltiples dados** (d4, d6, d8, d10, d12, d20, d% y dF) simulados en **3D con física
real** (rotaciones, rebotes, choques entre dados y contra la bandeja), que además **interpreta la
tirada según el sistema de juego elegido** (PbtA, Blades, V5, D&D, La Llamada de Cthulhu, etc.).

## 2. Estado actual (segunda entrega)

La app tiene **mesa 3D con física real**, los **dos modos de decisión del número** en apartados
separados (ver [MODOS_DE_DECISION.md](MODOS_DE_DECISION.md)), lanzamiento por botón o deslizando
el dedo, y un sonido que aprovecha las 65 tomas grabadas.

```
app/src/main/java/com/rolesencia/diceroller/
├── MainActivity.kt                 Splash nativo + edge-to-edge (igual que el Lanzador)
├── domain/                         Kotlin puro: dados y reglas de juego
│   ├── dados/                      TipoDado, RolDado, FuenteResultados (sorteo)
│   └── reglas/                     SistemaReglas, CatalogoSistemas, 17 mecánicas (ver §6)
├── fisica/                         Geometría y motor, sin Android
│   ├── Matematica3D.kt             Vec3 / Quat (integración, alineación de pares)
│   ├── Poliedros.kt                Malla real de cada dado (vértices + caras); d10 = trapezoedro calculado
│   ├── GeometriaDados.kt           Normales de cara (derivadas de la malla)
│   ├── LectorCara.kt               Orientación final → número (+ detección de dado "montado")
│   ├── Simetrias.kt                Rotaciones que dejan el dado igual pero cambian el número (modo 2)
│   ├── MotorFisicaDados.kt         Contrato del motor + eventos de colisión
│   └── motor/                      MotorFisicoPropio (impulsos secuenciales) + EjeSeparador (SAT)
├── tirada/                         Los dos modos de decisión
│   ├── SesionTirada.kt             Contrato común: avanzar(dt) → cuadro + eventos
│   ├── SimulacionTirada.kt         Física con GARANTÍA de resultado (empujones, temblores, límite)
│   ├── GeneradorLanzamiento.kt     "La mano": posición, giro e impulso de cada dado
│   ├── fisicareal/                 MODO 1: SesionFisicaReal (simula en vivo y lee la cara)
│   ├── torre/                      TowerDice: ModeloTorre + ModelosTorre (madera, castillo, cuero)
│   └── predeterminada/             MODO 2: PreSimulador + SesionResultadoPredeterminado
├── domain/apariencia/           Skins: modelo, catálogo de colores, contraste e ideas del creador
├── sensorial/
│   ├── PlanificadorSonido.kt       Qué toma, volumen, tono y paneo (Kotlin puro, testeado)
│   ├── GestorSonido.kt             SoundPool: reproduce, limita voces y mide variedad
│   └── GestorHaptico.kt, FeedbackTactil.kt (portados)
├── data/GestorPreferencias.kt      DataStore: sistema, pool, modo, mesa y ajustes
└── ui/
    ├── mesa3d/                     Camara, PintorDados (caras, números, sombras), MesaDados3D, IconoDado3D
    │                               DibujoTorre: torre en baldosas ordenadas junto con los dados
    ├── components/SolapaDados.kt   Solapa lateral (deslizar desde el borde derecho) con los dados en 3D
    ├── skins/                      Selector de skins y creador (ver SKINS.md)
    ├── components/                 Hojas inferiores (sistema, tirada, ajustes), fila de fichas, apartados de cada modo
    └── screen/, viewmodel/, theme/

app/src/test/…                      38 tests: reglas, mallas y simetrías, ambos modos, determinismo,
                                    equiprobabilidad (χ²), estadísticas de la física y del sonido
```

## 3. Qué se reutiliza del Lanzador Legendario

| Elemento original | Estado en Dice Roller | Comentario |
|---|---|---|
| `GestorSonido.kt` + 65 `.ogg` (lona/madera/dado × ligero/pesado × suave/fuerte/asentamiento) | **Archivos copiados tal cual; lógica reescrita** en `PlanificadorSonido` + `GestorSonido` | El foley es el mayor activo del proyecto. Antes un evento elegía al azar entre 5 tomas de un banco; ahora cada choque físico decide banco, toma (sin repetir), capa, tono, paneo y volumen, y una tirada típica de 9 dados usa ~48 de las 65 tomas. SoundPool sigue siendo lo correcto para muchos sonidos cortos y simultáneos. |
| `GestorHaptico.kt`, `FeedbackTactil.kt` | **Copiados** | Vibración proporcional al impacto; ahora se dispara con los choques contra las paredes. |
| `ReglasJuego.calcularJuicio` + `DadoLogica.kt` | **Portados** como el sistema `Rolesencia` | Misma lógica (Mega/Ying-Yang/Heptagrama, éxito por capacidad, dificultad 3 = enfrentada), ahora como un sistema más del catálogo y con tests que la fijan. |
| `TipoResultado` (etiqueta + color) | **Generalizado** a `NivelResultado` + `Veredicto` | 6 niveles semánticos que sirven para cualquier juego; el color sale de `colorDe(nivel)`. |
| `GestorPreferencias` (DataStore Preferences) | **Misma receta**, datos nuevos | Guarda sistema, pool de N dados, modificador, objetivo y ajustes. |
| Paleta *Dark Fantasy* (`Color.kt`, `Theme.kt`) | **Portada** | Mismo look & feel de marca Rolesencia. |
| Patrón ViewModel + `StateFlow<UiState>` + Factory manual | **Mismo patrón** | `DiceRollerViewModel` replica init-desde-DataStore, guardado tras cada acción y bloqueo mientras ruedan los dados. |
| Splash nativo atado a la carga de DataStore | **Igual** | `setKeepOnScreenCondition { estaCargando }`. |
| "Mantener pantalla encendida", toggles de audio/vibración | **Igual** | |
| Secuencia de lanzamiento "0 spoilers" (limpiar → rodar → revelar) | **Se conserva la idea** | Con física real el resultado ya no se "inyecta": se lee al detenerse. |

### Lo que NO se reutiliza (y por qué)

| Elemento | Motivo |
|---|---|
| `ArenaFisica.kt` | Motor 2D casero de **círculos**, con colisión fija entre `particulas[0]` y `particulas[1]`: no escala a N dados ni a 3D. |
| `ArenaDados.kt` / `ArenaRender.kt` | Dibujan PNG que giran en el plano y "levitan" para mostrar el resultado. En 3D la cara visible **es** el resultado. |
| `d4.png … d10.png`, `dl.png` | Se sustituyen por modelos 3D (glTF). Los vectoriales de símbolos (`heptagrama`, `yingyang`, `mega`) sí servirán como textura de las caras del d12 Legendario. |
| `SelectorDificultad`, `PanelControlDados`, `BotonLanzador` | Pensados para 2 dados fijos; la nueva UI maneja pools arbitrarios y parámetros que dependen del sistema. Pueden servir de referencia visual. |
| `AutoSizeText.kt` | Compose ya trae autoajuste de texto nativo (`BasicText(autoSize = TextAutoSize.StepBased())`, Compose ≥ 1.8). |
| `.txt` de contexto y `.bat` dentro de `src/` | Herramientas de trabajo, no código de la app. |

## 4. Qué se cambia por librerías

| Necesidad | Antes (casero) | Librería elegida | Por qué es la mejor opción |
|---|---|---|---|
| **Física de cuerpos rígidos 3D** | `ArenaFisica.kt` (círculos 2D) | **Implementado: motor propio** (`MotorFisicoPropio`). **Alternativa preparada: Jolt Physics** vía `com.github.stephengold:jolt-jni-Android` (v6.1.1, MIT) | Motor moderno de nivel AAA (creado para *Horizon Forbidden West*), estable con muchos cuerpos chicos, colisiones convexas exactas (`ConvexHullShape` = poliedro real del dado), "sleeping" para saber cuándo un dado se detuvo y callbacks de contacto para disparar sonido/háptica. Tiene **artefacto Android oficial** en Maven Central y mantenimiento activo en 2026. **Por qué se escribió uno propio primero**: el objetivo era *entender* la física con garantía de resultado; un motor en Kotlin puro se lee de punta a punta, corre en los tests de JVM (miles de tiradas en segundos, determinismo verificable) y no suma código nativo al APK. Con dados convexos y pocas decenas de cuerpos alcanza. Si hiciera falta más precisión (choques arista-arista, cientos de dados), Jolt entra detrás de la misma interfaz `MotorFisicaDados`. |
| (alternativa) | — | `Libbulletjme-Android` (Bullet, mismo autor) | Plan B si Jolt diera problemas: Bullet es el estándar histórico, igual de completo pero más antiguo. **Descartados**: jBullet (port Java sin mantenimiento), JBox2D/PhysicsBox (sólo 2D), escribir un motor propio (meses de trabajo para igualar estabilidad). |
| **Render 3D** | Compose `Image` + `graphicsLayer` | **Implementado: Canvas de Compose con proyección propia** (`ui/mesa3d`). **Evolución prevista: SceneView** `io.github.sceneview:sceneview` (v4.55.0) sobre **Filament** | Es la librería 3D *nativa de Jetpack Compose* (`Scene { ModelNode(…) }`), así la mesa 3D convive con el resto de la UI sin vistas legacy. Filament (Google) da PBR, sombras suaves e iluminación por entorno con muy buen rendimiento en gama media. Carga glTF/GLB directo. **Descartados**: Filament "crudo" (mucho código de bajo nivel para lo mismo), OpenGL ES a mano, Sceneform (deprecado), Rajawali (abandonado), Unity/Godot embebido (APK enorme y rompe la arquitectura Compose). **Por qué Canvas por ahora**: los dados son poliedros de 4 a 20 caras; dibujarlos con caras sombreadas, números en perspectiva (`Matrix.setPolyToPoly`) y sombras es poco código, sin modelos 3D ni texturas que preparar, y deja ver claramente qué hace cada modo. SceneView aporta materiales PBR y sombras reales cuando haya modelos glTF. |
| **Modelos de los dados** | PNG | **glTF 2.0 / GLB** (Blender → export) | Formato estándar que Filament carga sin conversión; permite materiales PBR (resina, metal, madera), números como textura o relieve y variantes de color para dados ESPECIALES. |
| **Autoajuste de texto** | `AutoSizeText.kt` | `BasicText(autoSize = …)` de Compose | Ya viene en la plataforma. |
| Inyección de dependencias | Factory manual | (Opcional) **Hilt** o **Koin** | Recién vale la pena cuando haya motor físico, repositorio de historial y presets; hoy la Factory alcanza. |
| Historial / presets de tiradas | — | (Fase 4) **Room** + kotlinx.serialization | DataStore Preferences no está pensado para listas grandes ni consultas. |

Las dos dependencias 3D ya están declaradas en `gradle/libs.versions.toml` y **comentadas** en
`app/build.gradle.kts` para no inflar el APK hasta que se implementen.

## 5. Elementos nuevos y por qué

1. **Motor de reglas enchufable (`SistemaReglas`)** — patrón *Strategy*: cada juego es una clase
   que (a) sugiere el pool inicial, (b) declara qué parámetros necesita (modificador, CD,
   dificultad, habilidad…) y (c) traduce los números en un `Veredicto`. Agregar un juego nuevo es
   agregar una clase y una línea al `CatalogoSistemas`, sin tocar la UI.
2. **Dados con rol (`RolDado.ESPECIAL`)** — un mismo mecanismo cubre Hambre (V5), Estrés (Alien),
   Miedo (Daggerheart) y el d12 Legendario. La UI/3D sólo cambia el color del dado.
3. **Dados explosivos (`dadosExplosivos`)** — el sistema pide relanzar ciertos dados (10-again,
   Regla del Seis) y `resolverTirada` los encadena. En 3D se verán caer dados nuevos sobre la mesa.
4. **Ventaja/desventaja genérica (`Conservar.MAYOR/MENOR`)** — sirve para D&D, dados de
   bonificación/penalización de Cthulhu y la regla de "0 dados" de Blades.
5. **`LectorCara` + `GeometriaDados`** — convierten la orientación final (cuaternión que devuelve
   Jolt) en el número: se rota cada normal de cara y gana la más alineada con la vertical, o con la
   normal de la superficie que lo sostiene (rampa, otro dado); el d4 se lee por abajo. También
   detecta dados **"montados"** (sobre una arista o vértice) para empujarlos en el lugar.
   Es Kotlin puro y está testeado para todos los tipos de dado.
6. **`MotorFisicaDados` (interfaz) + `MotorFisicoPropio`** — aísla a la app del motor concreto.
   Los eventos de contacto (`EventoColision`: impacto, roce, asentamiento) alimentan el sonido.
7. **`GeneradorLanzamiento` + gesto** — el lanzamiento nace del deslizamiento (dirección y fuerza)
   o de un impulso al azar, con giro y ruido para que dos tiradas nunca sean iguales.
8. **Dos modos de decisión del número**, en apartados separados de la UI y en paquetes separados
   del código. Proceso, garantías y mediciones en [MODOS_DE_DECISION.md](MODOS_DE_DECISION.md).
9. **Sonido con variedad** — `PlanificadorSonido` combina bolsa aleatoria por banco, capas
   suave/fuerte por probabilidad, superposición en golpes fuertes, tono por tamaño de dado, paneo
   estéreo y material de mesa elegible (detalle en el mismo documento).
10. **Lanzamiento por agitado (pendiente)** — acelerómetro (`SensorManager`) para "agitar el cubilete".

## 6. Mecánicas de juego investigadas e implementadas

| Sistema (clase) | Dados | Cómo se lee | Ejemplos de juegos |
|---|---|---|---|
| Suma libre (`SumaLibre`) | cualquiera | Suma + modificador, sin juicio | Daño, tablas aleatorias |
| Rolesencia (`Rolesencia`) | d12 Legendario (especial) + d4–d10 de Atributo | 10–12 éxito legendario, 8–9 parcial, 1–7 decide la capacidad; atributo ≥ dificultad; dif. 3 = enfrentada | Lanzador Dado Legendario |
| d20 contra CD (`D20ContraCd`) | 1d20 (2d20 con ventaja/desventaja) | d20 + mod ≥ CD; 20 natural crítico, 1 natural pifia | D&D 5e, Pathfinder |
| Powered by the Apocalypse (`PoweredByTheApocalypse`) | 2d6 | ≤6 fallo, 7–9 éxito parcial, 10+ éxito completo | Apocalypse World, Dungeon World, Masks |
| Forged in the Dark (`ForgedInTheDark`) | pool d6 | Más alto: 1–3 fallo, 4–5 parcial, 6 éxito; dos 6 crítico; 0 dados = 2d6 el menor | Blades in the Dark, Scum and Villainy |
| Sólo dieces (`CatalogoSistemas.SOLO_DIECES`) | pool d10 | Sólo los 10 son éxitos | Caso pedido como ejemplo |
| Mundo de Tinieblas clásico (`…MUNDO_DE_TINIEBLAS`) | pool d10 | 6+ éxito, cada 1 resta, sin éxitos con 1 = pifia | Vampiro/Hombre Lobo clásicos |
| Crónicas de la Oscuridad (`…CRONICAS_DE_LA_OSCURIDAD`) | pool d10 | 8+ éxito, los 10 explotan, 5+ éxitos excepcional | Chronicles of Darkness |
| Exalted 3ª (`…EXALTED`) | pool d10 | 7+ éxito, 10 cuenta doble | Exalted |
| Vampiro V5 (`VampiroV5`) | pool d10 + dados de Hambre | 6+ éxito, cada par de 10 = 4 éxitos; 10 de Hambre en crítico = caótico; 1 de Hambre en fallo = bestial | Vampiro: la Mascarada 5ª |
| Shadowrun (`Shadowrun`) | pool d6 | 5–6 son hits; más de la mitad de 1 = glitch; glitch sin hits = crítico; con Edge los 6 explotan | Shadowrun 5e/6e |
| Year Zero Engine (`YearZero`) | pool d6 (+ dados de Estrés) | Cada 6 es éxito; 1 en Estrés = Pánico | Mutant: Year Zero, Alien, Vaesen, Forbidden Lands |
| La Llamada de Cthulhu (`LlamadaDeCthulhu`) | d% + d10 | ≤ habilidad éxito, ≤½ difícil, ≤⅕ extremo, 01 crítico, 100 (o 96+ con hab. < 50) pifia; dados de bonificación/penalización | CoC 7ª, BRP |
| Fate (`FateCore`) | 4dF | dados + habilidad vs oposición; 0 empate, 3+ con estilo; escalera de adjetivos | Fate Core, Fate Condensed |
| Cypher System (`CypherSystem`) | 1d20 | ≥ dificultad × 3; 1 intrusión, 17–18 daño extra, 19–20 efectos | Numenera, The Strange |
| Daggerheart (`Daggerheart`) | d12 Esperanza + d12 Miedo | Suma ≥ dificultad; el mayor define "con Esperanza / con Miedo"; dobles = crítico | Daggerheart |
| Ironsworn (`Ironsworn`) | d6 de acción + 2d10 de desafío | Superar ambos golpe fuerte, uno débil, ninguno fallo; d10 iguales = coincidencia | Ironsworn, Starforged |

**Pendientes / candidatos para próximas iteraciones**

* **Genesys / Star Wars FFG**: dados narrativos con símbolos (éxito, ventaja, triunfo, amenaza…).
  Requiere un `TipoDado` con caras de símbolos múltiples; la arquitectura lo admite agregando un
  tipo de cara no numérica.
* **Savage Worlds**: dado de rasgo + dado salvaje, ambos explotan ("ases"), se toma el mayor;
  4+ éxito y cada +4 un "aumento". Necesita explosión que *suma* sobre el mismo dado.
* **Dungeon Crawl Classics**: dados raros (d3, d5, d7, d14, d16, d24, d30) → nuevos modelos 3D.
* Sistema **personalizado** por el usuario (umbral, explosión, unos restan…) reutilizando
  `ConteoExitos`, editable desde la UI y guardado en Room.

## 7. Hoja de ruta

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Estructura, dominio de reglas (17 mecánicas), lectura de caras, sonido/háptica portados, persistencia, tests | ✅ |
| 2 | Mallas reales de los 8 dados, motor físico propio, mesa 3D en Canvas, lectura de caras real, foley por contacto, **modo física real** y **modo resultado predeterminado**, lanzamiento por gesto, resaltado de dados que cuentan, explosiones en 3D | ✅ esta entrega |
| 3 | Lanzar agitando el teléfono; pre-simular al apoyar el dedo para ocultar la latencia del modo 2; optimizar asignaciones del motor | ⏳ |
| 4 | Historial de tiradas (Room), presets por personaje, sistema personalizado, accesibilidad (anuncio del veredicto con TalkBack) | ⏳ |
| 4b | **Interfaz a pantalla completa**: mesa a sangre, barras superpuestas con área útil para la cámara, hojas inferiores, modo inmersivo (bitácora, cap. 12) | ✅ |
| 4c | **Atajos de las apps pagas**: historial (50) con "otra vez", tiradas guardadas, notación, ventaja con pulsación larga, agitar para lanzar, voz, deshacer (bitácora, cap. 13) | ✅ |
| 5 | Pulido: modelos glTF + SceneView (PBR), materiales de dados, optimización en gama baja, publicación | ⏳ |

## 8. Fuentes consultadas

* Dice pool (conteo de éxitos, roll-and-keep) — [Wikipedia: Dice pool](https://www.wikipedia.com/wiki/Dice_pool), [The Game Mechanic: D is for Dice Pool](https://d20radio.com/main/?p=22996)
* PbtA (2d6, 6-/7–9/10+) — [Let the apocalypse power your first TTRPG](https://pittnews.com/?p=172182), [PbtA-style checks for D&D](https://taylor-lane.itch.io/powered-by-the-apocalypse-style-checks-for-dd-osr-and-dd-like-games)
* Blades in the Dark — [AnyDice: Blades in the Dark](https://anydice.catlikecoding.com/articles/blades-in-the-dark), [Probabilities in Blades in the Dark](https://statmodeling.stat.columbia.edu/2020/07/15/probabilities-for-action-and-resistance-in-blades-in-the-dark/)
* Vampiro V5 — [Strange Assembly: mecánicas V5](https://www.strangeassembly.com/?p=9381), [Roll20 forum](https://app.roll20.net/forum/permalink/6657527/)
* Year Zero Engine — [RPGGeek: Year Zero Engine](https://rpggeek.com/rpgsystem/43636/year-zero-engine)
* Shadowrun — [World Anvil: dice rolls and skillchecks](https://www.worldanvil.com/w/shadowfixers-syndicate-twisty/a/dice-rolls-and-skillchecks-article)
* La Llamada de Cthulhu — [Chaosium CoC wiki: The Game System](https://callofcthulhuwiki.chaosium.com/rules/game-system.html), [Roll20: Skill Rolls and Difficulty Levels](https://roll20.net/compendium/coc/Rules:Skill%20Rolls%20and%20Difficulty%20Levels)
* Fate — [Fate SRD: The Basics](https://fate-srd.com/downloads/fate-3/material-genericized-from-dresden-files-rpg/basics), [Evil Hat: The Core of Fate Core](https://evilhat.com/the-core-of-fate-core/)
* Cypher System — [Cypher SRD: How to Play](https://cypher-srd.statichost.eu/01-how-to-play)
* Genesys — [Wikipedia: Genesys (RPG)](https://en.wikipedia.org/wiki/Genesys_(RPG))
* Jolt JNI para Android — [Maven Central: jolt-jni-Android](https://central.sonatype.com/artifact/com.github.stephengold/jolt-jni-Android), [Libbulletjme](https://mvnrepository.com/artifact/com.github.stephengold/Libbulletjme/14.0.0)
* SceneView — [Maven Central: sceneview](https://central.sonatype.com/artifact/io.github.sceneview/sceneview), [SceneView docs](https://docsearch.algolia.com/mcp/docs/repo/sceneview/sceneview)
* PhysicsBox (descartado, 2D) — [ProAndroidDev: PhysicsBox](https://proandroiddev.com/physicsbox-adding-physics-to-compose-driven-projects-bc7926fe3b23)

Daggerheart e Ironsworn se implementaron a partir de sus reglas públicas básicas (SRD); conviene
contrastar los detalles finos (p. ej. efectos de Esperanza/Miedo) con los manuales oficiales.
