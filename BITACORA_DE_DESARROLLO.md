# Bitácora de desarrollo — Dice Roller

> Cómo se construyó la app, feature por feature, contado en el orden en que pasó: qué se quería,
> qué se intentó, qué salió mal, cómo se arregló y qué idea nueva salió de cada paso.
> Está pensada para alguien que quiere **aprender** (física de juegos, gráficos 3D a mano en
> Compose, diseño de reglas de rol) y no sólo usar el código.
>
> **Este documento se actualiza con cada feature nueva.** Al final hay una plantilla.

## Cómo leer este documento

Cada capítulo sigue la misma forma:

- **El pedido** — qué se quería lograr y por qué.
- **La solución** — la idea central, sin entrar en cada línea.
- **Problemas y soluciones** — lo que no funcionó a la primera. Es la parte más útil para aprender.
- **Lo que aprendimos** — la lección que se puede llevar a otros proyectos.
- **De acá salió…** — la idea o el pedido siguiente que nació de este capítulo.

Los documentos de referencia (qué hay hoy, sin la historia) siguen existiendo:

| Documento | Para qué sirve |
|---|---|
| [DICE_ROLLER_PLAN.md](DICE_ROLLER_PLAN.md) | Estructura del código, qué se reutilizó del proyecto anterior, mecánicas de juego investigadas y fuentes. |
| [MODOS_DE_DECISION.md](MODOS_DE_DECISION.md) | Física, garantías de resultado, los dos modos, latencia, sonido y torres. |
| [SKINS.md](SKINS.md) | Catálogo de skins, contraste de los números, creador y estilos. |
| [GUIA_UX_UI.md](GUIA_UX_UI.md) | Pautas de interfaz (ergonomía, normas de Android y Material, patrones de otras apps) y cómo se aplica cada una. |
| [INVESTIGACION_COMPETENCIA.md](INVESTIGACION_COMPETENCIA.md) | Apps de dados pagas, cómo se promocionan, sus patrones de interfaz e ideas todavía no implementadas. |
| **Esta bitácora** | La historia: por qué las cosas son como son. |

---

## Capítulo 1 · Punto de partida: del *Lanzador Dado Legendario* a *Dice Roller*

**El pedido.** Tomar como base una app anterior (un lanzador de un único dado legendario) y
convertirla en un lanzador de **muchos dados en 3D** que entienda las **reglas de distintos juegos
de rol**: "sólo cuentan los 10 en el d10", "2d6: 6 o menos falla, 7–9 éxito parcial, 10+ éxito",
ventaja y desventaja, etc. Investigar en internet las mecánicas reales y documentar qué se
reutiliza, qué se reemplaza por librerías y qué es nuevo.

**La solución.**

- **Reglas enchufables** (`SistemaReglas`, patrón *Strategy*): cada juego es una clase que sugiere
  el pool inicial, declara qué parámetros necesita y traduce los números en un veredicto. Agregar
  un juego = una clase + una línea en el catálogo. Hoy hay 17 mecánicas (D&D, PbtA, Blades, V5,
  Year Zero, Daggerheart, Cthulhu, Fate, Shadowrun…).
- **Dados con rol** (`RolDado.ESPECIAL`): un solo mecanismo cubre Hambre (V5), Estrés (Alien),
  Miedo (Daggerheart) y el d12 Legendario.
- **Dados explosivos** (10-again, regla del seis): el sistema pide relanzar ciertos dados y la
  tirada se encadena en rondas.
- Del proyecto anterior se reutilizó lo que ya funcionaba (splash, háptica, preferencias) y se
  reescribió lo que estaba pensado para un solo dado.

**Lo que aprendimos.** Separar **dominio** (reglas, Kotlin puro y testeable) de **presentación**
desde el primer día hizo posible todo lo que vino después: las reglas no saben nada de 3D y la
mesa 3D no sabe nada de reglas.

**De acá salió…** la pregunta central de la app: *¿quién decide el número de cada dado, la física
o el sorteo?*

---

## Capítulo 2 · Dos formas de decidir el número

**El pedido.** Dos modos, en apartados separados y elegibles:

1. **Física real con garantía de resultado**: los dados ruedan de verdad y el número es la cara
   que queda arriba, pero la tirada *siempre* termina con un resultado legible.
2. **Resultado predeterminado**: el número se sortea antes y la animación 3D tiene que terminar
   mostrando exactamente ese número. Medir si eso introduce una demora perceptible.

Y aprovechar las **65 grabaciones** de sonido que había.

**La solución.**

- **Motor físico propio** (`MotorFisicoPropio`): cuerpos rígidos con impulsos secuenciales (el
  método de Box2D), *warm starting*, corrección de Baumgarte, fricción de Coulomb y "sueño" de los
  dados quietos. Las mallas son las reales de cada dado (el d10 es un trapezoedro calculado).
- **Lector de caras** (`LectorCara`): se rota la normal de cada cara con la orientación final del
  dado y gana la más alineada con "arriba". El d4 se lee por abajo.
- **Modo predeterminado sin trampas visibles**: se simula la tirada completa antes de mostrarla
  (*pre-simulación*), se ve qué cara cayó y se aplica una **rotación de simetría** del poliedro
  (`Simetrias`) que deja al dado idéntico en forma pero con el número pedido arriba. La película
  grabada se reproduce con esa rotación: el movimiento es 100 % físico.
- **Garantías** (`SimulacionTirada`): contención, espera a que todos duerman, tratamiento de dados
  montados, temblores, límite de tiempo.
- **Sonido**: cada choque del motor genera un evento (impacto, roce, asentamiento) con velocidad y
  posición; un planificador elige toma, volumen, tono y paneo (detalle en MODOS_DE_DECISION §5).

**Problemas y soluciones.**

| Problema | Solución |
|---|---|
| ¿La pre-simulación demora el lanzamiento? | Se midió: hasta ~10 dados es casi imperceptible (13 ms en JVM con 10 d6); con 20–30 se nota (60–300 ms). Se dejaron anotadas formas de esconderla (pre-simular al apoyar el dedo). |
| La misma toma de sonido repetida suena "a máquina" | Bolsa aleatoria por banco, capas suave/fuerte por probabilidad, superposición en golpes fuertes, tono según tamaño del dado. Una tirada de 9 dados usó 48 de las 65 tomas. |
| La app se cerraba al vibrar | Faltaba el permiso `VIBRATE` en el manifiesto. |

**Lo que aprendimos.** Para "elegir" un resultado sin que se note, no hay que tocar la física:
alcanza con una **simetría** del objeto. Y medir la latencia con números concretos evita discutir
sobre sensaciones.

**De acá salió…** que, con muchos dados en pantalla, hacía falta una forma cómoda de agregarlos.

---

## Capítulo 3 · La solapa lateral con dados 3D

**El pedido.** Que los dados aparezcan al deslizar desde el borde derecho, como una solapa, de
arriba hacia abajo en orden ascendente, y que cada botón muestre **la forma 3D del dado**.
Después: el dado legendario debajo del dF y un botón (−) para quitar de a uno.

**La solución.** `SolapaDados` con `IconoDado3D`, que reutiliza el mismo pintor de la mesa con una
cámara fija: el ícono es literalmente el dado de la mesa.

**Problemas y soluciones.**

| Problema | Solución |
|---|---|
| El deslizamiento lo "robaba" el `verticalScroll` de la pantalla | Detectar el gesto con `awaitEachGesture` en la pasada *Initial* y consumirlo; exigir 80 dp de recorrido y 800 px/s para que un roce no abra nada. |

**Lo que aprendimos.** En Compose, cuando dos gestos compiten, el orden de las pasadas
(*Initial / Main / Final*) decide quién gana.

**De acá salió…** que, con muchos dados chocando, se notaron dados **superpuestos** y el pedido de
poder elegir **colores**.

---

## Capítulo 4 · Nunca superpuestos, y los primeros colores (skins)

**El pedido.** Que los dados no se superpongan nunca (ni al empezar, ni en el recorrido, ni al
final; sí pueden taparse por perspectiva). Un desplegable para elegir colores, un catálogo de
referencia de skins con números que contrasten, un creador de skins y la posibilidad de tomar una
imagen como referencia.

**La solución.**

- **Choques exactos con SAT** (teorema del eje separador): se prueban las normales de las caras de
  ambos dados y los productos cruz de sus aristas. Así se detecta también el cruce **arista contra
  arista**, que la prueba anterior ("¿algún vértice quedó adentro del otro?") no veía.
- **Proyección de posiciones**: después de cada sub-paso se separan directamente los pares que se
  meten más de 0,4 mm, sin agregar velocidad.
- **Skins**: catálogo por categorías; la tinta de los números se elige por **contraste WCAG**; si un
  color medio no contrasta con blanco ni con negro, el número lleva contorno.
- **Creador**: elige colores a mano o los extrae de una imagen (AndroidX Palette, Photo Picker).

**Problemas y soluciones.**

| Problema | Solución |
|---|---|
| Dados que se atravesaban por las aristas | SAT completo (caras + aristas) en `EjeSeparador`. |
| Colores intermedios (p. ej. "Atardecer") ilegibles con tinta blanca o negra | Contorno automático (`necesitaContorno`). |
| El emulador congelaba la interfaz del sistema (ANR) | Se pasó a probar en el **teléfono real** conectado, mucho más fiel. |

**Lo que aprendimos.** "No superponer" no es una sola técnica: es detección exacta + corrección de
posición + verificación final. Y la legibilidad de los números se puede **calcular** (contraste),
no adivinar.

**De acá salió…** el deseo de una forma más teatral de tirar: las **torres de dados**.

---

## Capítulo 5 · TowerDice: tres torres de dados

**El pedido.** Un modo distinto a la bandeja, con tres torres a partir de fotos reales: madera,
castillo de piedra y caja de cuero. Probarlas en el teléfono.

**La solución.** Cada torre es una lista de **piezas** (cajas orientadas) que el motor trata como
escenario fijo: paredes, rampas internas alternadas y una rampa de salida. El dibujo parte cada
cara en **baldosas** que se ordenan por profundidad junto con los dados (algoritmo del pintor),
para que un dado pueda pasar por delante o por detrás de una pared.

**Lo que aprendimos.** Con un motor propio, el escenario es "más cuerpos": las torres no
necesitaron física nueva, sólo piezas estáticas.

**De acá salió…** el primer problema serio: con muchos dados, las torres se **trababan**.

---

## Capítulo 6 · Muchos dados: puertas que se achican y rampas trabadas

**El pedido.** (1) En el castillo, con más dados, la "puerta inferior" se achicaba hasta
desaparecer. (2) Con 11 dados o más, quedaban en la última rampa y la tirada tardaba mucho.
Buscar una solución "elegante y coherente con la realidad". Además, implementar la hoja de ruta de
skins: bicolor, degradado, gema translúcida, metálico, mármol, purpurina, brillo nocturno, imagen
por cara, tipografías y skins por sistema de juego.

**Problemas y soluciones.**

| Problema | Causa | Solución |
|---|---|---|
| La puerta del castillo se achicaba con más dados | La bandeja crece con la cantidad de dados; el aro de piedra crece con ella y sus tramos terminaban delante de la puerta. | El aro **se abre siempre frente a la torre**: se omite todo tramo que tape la puerta o el pasillo de salida. |
| Con ≥11 dados se formaba un tapón en la boca | Todos entraban juntos y se apilaban en la salida. | **Vertido escalonado** (un dado cada 0,12 s), como al volcar un cubilete. |
| Los que salían quedaban en la puerta y frenaban a los de atrás | La rampa terminaba justo en la boca. | **Lengua de salida**: la rampa sigue 4 cm dentro de la bandeja y lanza a los dados lejos. |
| Un dado quieto dentro de la torre frenaba todo | — | **Golpecito a la torre**: si un dado queda quieto adentro, se le da un impulso hacia la salida, como haría un jugador. |
| Dados que no "se dormían" por temblores mínimos | El reposo se decidía con la velocidad instantánea. | Velocidades **promediadas** (media móvil exponencial) y más roce entre dados apilados. |
| Dados montados en un montón | — | Se relanzaban en el lugar más libre de la bandeja *(esto después se cambió: ver capítulo 8)*. |

Resultado de entonces: 15 dados en 3,6–4,5 s sin forzar ningún dado.

**Skins.** Se implementaron los diez estilos con un pintor que entiende de estilos (`PintorDados`):
el metal tiene reflejo especular, la gema es translúcida, el mármol tiene vetas fijas a la cara,
etc. El color "Cobre pulido" se aclaró porque no pasaba el test de contraste.

**Lo que aprendimos.** Los atascos se resolvieron imitando lo que hace una persona (verter de a
poco, golpear la torre), no con trucos invisibles. Esa idea de "que sea coherente con la realidad"
guió todo lo que vino después.

**De acá salió…** el repositorio en GitHub (privado, `DiceRoller_APP`) y una crítica clave: el
relanzamiento "al lugar más libre" no era natural.

---

## Capítulo 7 · (Infraestructura) Git y GitHub

Se inicializó el repositorio, se hizo el primer commit con todo y se publicó en GitHub como
repositorio privado. A partir de acá cada bloque de trabajo termina en un commit con un mensaje
que explica el *porqué*.

---

## Capítulo 8 · Basta de teletransportes: leer la cara respecto del apoyo

**El pedido.** "No me gusta que el dado que no queda dormido de repente aparezca en otro lado del
tablero. Es antinatural." Propuesta del usuario: aceptar los dados apoyados en una rampa,
reconociendo la cara de arriba por estar **paralela a la rampa**, usando el lector de caras que ya
existía.

**La solución.** El lector no cambió: sólo se le pasó **otra vertical**.

- El motor calcula, para cada dado, su **normal de apoyo**: la normal de la superficie que lo
  sostiene (piso, rampa o cara de otro dado).
- `LectorCara.leer(tipo, orientación, arriba)` lee la cara respecto de esa normal. Un dado plano
  sobre una rampa es válido.
- Un dado realmente **montado** (sobre una arista o un vértice) recibe un **empujoncito en el
  lugar**; si persiste, se lo **acomoda donde está** (`asentarEnSitio`): se lo gira lo mínimo y se
  lo separa milímetros de lo que toca. Nunca se mueve de sitio.

**Problemas y soluciones.** Este capítulo es un buen ejemplo de **depurar con datos**: se
imprimió, para cada dado "mal apoyado", su posición, su normal de apoyo y su alineación.

| Problema | Lo que mostraron los datos | Solución |
|---|---|---|
| Con 20 dados: 142 empujones y 34 forzados en 10 tiradas | Dados **perfectamente planos** en el piso (alineación 0,9999) tenían una normal de apoyo inclinada. | La primera versión sumaba todos los contactos pesados por la fuerza; la **presión lateral** de los vecinos en un montón inclinaba la suma. Ahora se agrupan los contactos por "con quién" y gana la superficie contra la que el dado tiene **una cara más pareja**. |
| Aparecían como apoyo paredes o caras casi verticales | Normales con componente vertical 0,3–0,47. | Sólo cuentan superficies de **hasta 60°** de pendiente. |
| Un d4 trabado en una esquina no salía nunca | El empujón sólo se alejaba de los dados, no de las paredes; y un dado dormido no tiene contactos calculados. | El empujón busca vecinos **por cercanía** y también se aleja de las paredes. |
| Tiradas de 20 dados que llegaban al límite de tiempo | Dados planos que los vecinos seguían zarandeando y nunca se dormían. | Si en 1,5 s **ningún dado nuevo se durmió**, los que ya están planos se asientan ahí mismo y sólo los montados reciben empujón. |

Resultado: 15 dados en torre, 3,3–4 s de media sin acomodar ninguno; 20 dados, 5–5,7 s, sin llegar
nunca al límite. En el modo predeterminado la simetría sigue funcionando: la cara paralela al
apoyo es la pedida.

**Lo que aprendimos.** Antes de ajustar parámetros, **mirar los datos**: el error no estaba en los
empujones sino en la definición de "apoyo". Y una buena abstracción (el lector de caras) se
extiende cambiando una entrada, no reescribiéndola.

**De acá salió…** que, con dados apoyados en rampas y en montones, hacía falta **mirar el
resultado de cerca**.

---

## Capítulo 9 · La toma cenital del resultado

**El pedido.** Al terminar la tirada, que la cámara pase a vista **cenital** y haga zoom sobre los
dados; ya no importa ver toda la bandeja o la torre, sino leer bien las caras. Después: un toque
alterna entre la cenital y la vista general.

**La solución.** La cámara mezcla posición y objetivo entre la vista general y una casi cenital
(una pizca de inclinación conserva "arriba" = fondo de la mesa). El **encuadre** (escala y centro)
también se mezcla, así el zoom acompaña al movimiento sin saltos. Animación de 1,1 s al entrar y
0,5 s al salir.

**Problemas y soluciones.**

| Problema | Solución |
|---|---|
| Con dados muy desparramados a lo ancho se veía el vacío fuera de la mesa | El encuadre toma la **proporción del lienzo** y se mantiene **dentro del piso**. |
| Un solo dado ocupaba toda la pantalla | Tamaño mínimo del encuadre. |
| Las sombras se veían como un halo enorme desde arriba | Las sombras se vuelven redondas y más chicas a medida que la cámara se pone cenital. |
| Con dados explosivos el zoom empezaba y se cortaba | Sólo se acerca cuando no hay otra ronda en curso. |

**Lo que aprendimos.** Interpolar *parámetros de encuadre* además de la cámara da transiciones
mucho más naturales que interpolar sólo la posición.

**De acá salió…** una observación sobre el **color** de los dados en la vista cenital.

---

## Capítulo 10 · El color de la cara superior no debe cambiar con la cámara

**El pedido.** En la cenital, la cara superior "ganaba brillo" y un dado oscuro se veía gris.
Después apareció otro detalle: al elegir "Amatista" el dado se agregaba gris metálico. Y se pidió
**probar con distintas skins** si la diferencia era sólo de metálicos y gemas.

**Problemas y soluciones.**

| Problema | Causa | Solución |
|---|---|---|
| La cara de arriba se aclaraba en la cenital | El **reflejo especular** depende del ángulo de la cámara; mirando a plomo la cara superior quedaba justo en el reflejo. La luz difusa, en cambio, es fija al mundo. | En la cenital se apagan sólo los reflejos de las caras que miran arriba. Un primer intento llevó la cara al color base puro, y eso tampoco coincidía con la vista normal: lo correcto era conservar la luz difusa. |
| "Amatista" se agregaba como gris metálico | Con "skins del sistema de juego" activado, el dado nuevo recibía la skin sugerida para Blades aunque se hubiera elegido otra. | Elegir una skin a mano **desactiva** las del sistema. |
| ¿Afecta a otras skins? | — | Se extrajo la iluminación a una función pura (`Sombreado`) y un test recorre **las 79 skins** en 9 posiciones: sólo 5 metálicas y 4 gemas cambiaban (13–90 de 255); las otras 70, cero. |

**Lo que aprendimos.** Separar lo que depende del **mundo** (luz difusa) de lo que depende del
**observador** (reflejos) explica casi todos los "cambios de color raros" en 3D. Y sacar una
fórmula a una función pura permite probarla con todo el catálogo en milisegundos.

**De acá salió…** el pedido de darle **ambiente** a las torres.

---

## Capítulo 11 · Torres medievales: ambiente, luz y aberturas

**El pedido.** A partir de tres imágenes de referencia (una taberna iluminada con velas, un salón
con luces violeta y turquesa y una muralla de castillo en la niebla), dar un enfoque medieval: la
taberna inspira la torre de madera; las luces violetas, la iluminación del castillo y la caja de
cuero (y los colores del cuero); la muralla, un castillo muy temático. **Ventanas sólo en el
frente**, por donde se vea caer a los dados en parte, en vez de un frente simplemente abierto.

**La solución.**

- **Ambiente por torre**: fondo con degradado y resplandores, luz ambiente y principal de color,
  luces puntuales (velas, antorchas, focos desde abajo) y niebla que aclara lo alto. Como el
  escenario no se mueve, la luz se calcula **una sola vez** y no cuesta nada por cuadro.
- **Aparejo**: piedras, ladrillos y tablones trabados (hilera por medio), veta en la madera y musgo
  en la piedra, más cerca del suelo.
- **Aberturas** definidas en coordenadas de la cara: ventanas con arco (redondo o gótico) y
  parteluz, puerta en arco medida en X del mundo (abarca varias caras de la torre octogonal) y
  troneras. El hueco se recorta con operaciones de *path* (`Difference`), así se ve el interior.
- **La física no cambió**: la pared visible es decoración; la que choca es la misma de antes.

**Problemas y soluciones.** Casi todos fueron del **algoritmo del pintor**: ordenar por profundidad
funciona para piezas chicas, pero falla con piezas grandes o con adornos que sobresalen.

| Problema | Causa | Solución |
|---|---|---|
| La luz de velas y focos se veía en **cuadros** | La luz se calculaba en el centro de cada baldosa. | **Colores por vértice** (`Canvas.drawVertices`): la luz se interpola entre las esquinas. Necesita Android 10+; antes se usa un color por baldosa. |
| El marco de la puerta se estiraba en líneas negras enormes | La puerta abarca varias caras y cada una dibujaba el marco completo, proyectado en su propio plano. | Cada cara dibuja el marco **recortado a sí misma** (`clipPath`). |
| Un triángulo turquesa sobre el frente del cuero | Una cara **interior** de la torre se ordenaba delante de la pared exterior. | Las superficies interiores se ordenan como si estuvieran un poco más lejos. |
| Costuras que faltaban a medias | Una arista larga tiene una sola profundidad y las baldosas más cercanas la tapaban. | Las aristas se parten en **tramos cortos**, cada uno con su profundidad. |
| El ribete de la ventana se cortaba arriba | El adorno sobresale del hueco y la baldosa de arriba, más cercana, lo tapaba. | Cada adorno se ordena **detrás de todas las baldosas que pisa**. |
| Los cordones cruzados del frente de cuero "parecían un zapato" | Decisión de diseño. | Se propusieron siete alternativas; se eligió una **ventana de medio punto con ribete de cuero y doble pespunte dorado**. El hilo brilla algo más que el cuero para que se note con la luz tenue. |

**Lo que aprendimos.** El algoritmo del pintor es simple y rápido, pero cada vez que algo se
dibuja "de más" o "de menos" la pregunta es la misma: *¿con qué profundidad se está ordenando este
elemento, y qué otro elemento queda más cerca?* Partir en piezas chicas o mover la profundidad de
referencia resuelve casi todo. Y en diseño visual, **ofrecer opciones en texto antes de programar**
ahorra iteraciones.

---

## Capítulo 12 · La mesa a pantalla completa

**El pedido.** Primero se pidió investigar las apps de dados para rol que la gente paga y sus videos
promocionales. Con esa investigación, el pedido fue: "Fijate si podés incorporar una solución en base
a la investigación que hiciste". Antes, la mesa era un recuadro de 400 dp dentro de una página con
scroll, con todos los controles apilados debajo.

**De la investigación se tomó** una conclusión: en esas apps los dados son el protagonista y ocupan
la pantalla, los controles son pocos y están abajo, y el resultado se anuncia grande. El detalle
(apps, precios, tipos de video y patrones) está en
[INVESTIGACION_COMPETENCIA.md](INVESTIGACION_COMPETENCIA.md).

**La solución.**

- **Mesa a sangre** (`MesaDados3D` con `fillMaxSize`) y la interfaz **superpuesta** encima, sobre
  degradés oscuros que garantizan la lectura sin tapar la mesa con paneles opacos.
- **Área útil**: la cámara (`Camara.ajustar(…, x0, y0)`) encuadra la bandeja, la torre y la toma
  cenital del resultado **dentro del rectángulo que no tapan las barras**, medido con
  `onSizeChanged`. La profundidad de la bandeja sigue la proporción de esa área. El fondo (halo
  cálido para la bandeja, ambiente para las torres) se dibuja igual en toda la pantalla.
- **Arriba**: píldoras translúcidas con el **sistema de juego** y **dónde se lanza** (bandeja o torre,
  en un menú), y el engranaje de **ajustes**. Debajo, el **marcador**: el veredicto en 30 sp con el
  color de su nivel; antes de tirar, una pista de uso ("Deslizá el dedo sobre la mesa…").
- **Abajo, en la zona del pulgar**: una fila de fichas que se desplaza de costado (no se envuelve) y
  tres botones de 64 dp: **Tirada** (resume "+2 · CD 15 · Mayor" y abre los parámetros),
  **LANZAR** al centro y **Dados** (abre la solapa lateral).
- **Hojas inferiores** (`ModalBottomSheet`) en vez de diálogos y listas apiladas: *Sistema*,
  *Tirada* y *Ajustes* (modo de decisión con botones segmentados y su explicación, superficie,
  sonido, vibración y pantalla). Se cierran deslizando o con el "atrás" predictivo.
- **Modo inmersivo** de Android (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`), activable en Ajustes y
  guardado en DataStore: las barras del sistema se ocultan y vuelven un instante deslizando el borde.
- **Durante la tirada** los controles bajan a 25 % de opacidad y el marcador dice "Rodando…".
- **Accesibilidad**: el marcador es una región "viva" (TalkBack lee cada veredicto nuevo), todos los
  botones miden 48 dp o más y los íconos son vectores propios (Material Icons, Apache 2.0), sin sumar
  la librería de íconos.

**Pautas que se aplicaron.**

| Pauta | De dónde | Cómo quedó |
|---|---|---|
| Edge-to-edge y respetar *insets* (barras, recorte de cámara) | Android: *Display content edge-to-edge* | Mesa detrás de todo; barras con `WindowInsets.safeDrawing` |
| Modo inmersivo con barras transitorias para juegos | Android: *Hide system bars for immersive mode* | `ModoInmersivo`, opcional |
| Acciones frecuentes al alcance del pulgar | Material 3: *bottom app bar*, zona de alcance | LANZAR, Tirada y Dados abajo |
| Opciones secundarias en hojas inferiores | Material 3: *bottom sheets* | Sistema, Tirada, Ajustes |
| Todo gesto necesita un control visible | Material 3: *gestures* | Botón "Dados" además del deslizamiento desde el borde |
| Objetivos táctiles ≥ 48 dp | Material 3: *accessibility* | Píldoras 48 dp, botones inferiores 64 dp |
| Resultado protagonista, interfaz que se aparta | Investigación de competencia | Marcador grande; controles atenuados al rodar |

**Problemas y soluciones.**

| Problema | Causa | Solución |
|---|---|---|
| Con la mesa a pantalla completa, los dados del borde quedaban detrás de los botones | La cámara encuadraba el lienzo entero | Encuadre dentro del área útil medida (`areaUtil`) |
| La mesa "saltaba" al aparecer el veredicto | Un panel que crece cambia el área útil | Marcador de altura fija y fichas en una sola línea con scroll horizontal |
| Fuera de la bandeja se veía un color plano | Antes la mesa era un recuadro | Fondo con halo cálido en todo el lienzo |
| El deslizamiento desde el borde era la única forma de agregar dados | Gesto sin control visible | `SolapaDados(solicitudApertura)` abierto desde el botón "Dados" |

**Lo que aprendimos.** En una app donde el gráfico es el producto, la interfaz no se "pone debajo":
se superpone y **le avisa al gráfico qué parte de la pantalla tapa**. Medir las barras y pasarle ese
rectángulo a la cámara es poco código y evita toda la familia de "el dado quedó detrás del botón".

**Cómo se trabajó.** Este capítulo y el siguiente se hicieron desde claude.ai, en un entorno sin SDK
de Android y sin permiso de escritura en GitHub: no se pudo compilar, y los cambios se entregaron
como un parche (`git am`) que se aplicó a mano. Las capturas que acompañaron la propuesta eran
simulaciones en HTML, no capturas reales. Después de aplicar el parche, el proyecto **compiló y
pasaron los 60 tests**. Queda por probar en el teléfono: recorte de cámara, navegación por gestos y
la toma cenital con las barras visibles.

**De acá salió…** el pedido de sumar los atajos de interacción de esas mismas apps (capítulo 13).

---

## Capítulo 13 · Lo que hacen bien las otras apps de dados

**El pedido.** "Incorporá las mejores prácticas de UI e interacción del resto de las apps de dados."

**La solución.** Se tomaron los patrones que se repiten en varias apps pagas (cuáles y en qué apps:
[INVESTIGACION_COMPETENCIA.md](INVESTIGACION_COMPETENCIA.md) §3) y que encajan con una mesa a
pantalla completa:

| Atajo | Cómo quedó en Dice Roller |
|---|---|
| **Historial** de tiradas | Hoja "Historial": notación, valores, veredicto con su color y hace cuánto. Guarda las últimas 50 |
| **"Otra vez"** | Botón en cada entrada: restaura sistema, dados y parámetros, y lanza |
| **Tiradas guardadas** con nombre | Pestaña "Guardadas": "Espada larga" se guarda una vez y se lanza con un toque |
| **Notación de dados** (1d20 + 2d8 + 5) | En el marcador (antes de tirar), el historial y las guardadas |
| **Ventaja / desventaja de un gesto** | Mantener LANZAR: ventaja, desventaja o normal, y lanza. En D&D agrega o quita el segundo d20 solo. El botón avisa "con ventaja" |
| **Agitar para lanzar** | Acelerómetro: dos picos fuertes seguidos lanzan; 1,5 s de espera; desactivable |
| **Leer el resultado en voz alta** | TextToSpeech en español (rioplatense si el motor lo tiene), desactivado por defecto |
| **Deshacer** (pauta de Material: snackbar con acción) | Quitar un dado con pulsación larga muestra "Quitaste un d8 · Deshacer" |
| **Pistas que se retiran solas** | Las tres primeras tiradas el marcador enseña los gestos; después muestra la notación |

El historial se guarda en DataStore: con 50 entradas alcanza.

**Problemas y soluciones.**

| Problema | Causa | Solución |
|---|---|---|
| Al abrir la app, la voz leía la última tirada guardada | El historial persistido "aparecía" como tirada nueva | Sólo se leen tiradas posteriores a la apertura |
| Ventaja en D&D exigía agregar el segundo d20 a mano | El sistema evalúa "el mayor de los d20" | `Ventaja.ajustarPool` agrega/quita el segundo d20 (testeado) |
| Agitar lanzaba al caminar o al apoyar el teléfono | Un solo pico de aceleración alcanza | Umbral de 14 m/s² sobre la gravedad y dos picos en menos de 0,6 s |
| Guardar el historial reescribía todas las preferencias | Un solo `guardar()` para todo | `guardarHistorial` y `guardarTiradas` escriben sólo su clave |

**Lo que aprendimos.** Las apps que la gente paga no se diferencian en tirar dados (eso lo hace
cualquiera) sino en **todo lo que ahorra pasos entre tirada y tirada**: repetir, guardar, elegir
ventaja sin abrir un menú. Copiar sus pantallas no sirve; copiar sus atajos, sí.

**Verificación.** Igual que el capítulo 12, se escribió sin compilar; después de aplicar el parche
compiló y pasaron los 60 tests (incluidos los nuevos de notación, ventaja e historial). Falta
probarlo en el teléfono: agitar, voz y deshacer.

**De acá salió…** ideas que todavía no están implementadas (tiradas agrupadas por personaje,
compartir una tirada con el grupo); están en
[INVESTIGACION_COMPETENCIA.md](INVESTIGACION_COMPETENCIA.md) §4.

---

## Capítulo 14 · Los dados entran cayendo desde fuera de la pantalla

**El pedido.** "Necesito ahora que los dados caigan desde la parte más arriba del celular. Que
realmente parezcan que caen por encima de lo que se ve y no que se 'popeen' de la nada. Actualmente
inician su caída desde dentro de las torres."

**Por qué pasaba.** Cada dado entraba a la simulación en su turno de vertido, ya ubicado dentro de
la torre (a la altura de la boca, entre 16,5 y 26 cm). Antes de ese instante no existía, así que
aparecía de golpe.

**La solución.** La altura desde la que se suelta cada dado ya no es fija: la calcula la mesa 3D
con **la misma cámara con la que dibuja**.

- `alturaFueraDeCuadro` sube de a medio centímetro desde la boca de la torre hasta encontrar la
  altura en la que el punto más bajo del dado queda **por encima del borde superior del lienzo**,
  en los dos puntos de vertido.
- La mesa avisa esa altura (`onAlturaEntrada`), el ViewModel la guarda por torre y
  `GeneradorLanzamiento.impulsosTorre(…, alturaEntrada)` suelta los dados desde ahí. Nunca es menor
  que la boca, nunca llega al techo físico.
- Como la caída es física (no una animación aparte), el modo predeterminado la pre-simula igual
  que el resto de la tirada.

En un Samsung A33 (1080 × 2400), el castillo necesita soltar los dados a 40 cm, 14 cm sobre la boca.
Grabando la pantalla se ve el dado asomar por el borde, pasar detrás de la barra superior, caer
sobre las almenas, verse por las ventanas y salir por la puerta.

**Problemas y soluciones.**

| Problema | Causa | Solución |
|---|---|---|
| ¿Caer desde más alto traba la torre? | Los dados llegan más rápido a la primera rampa. | Se midió con `TorreTest` soltándolos 18 cm sobre la boca: ningún dado forzado ni trabado en las tres torres. Con 20 dados las tiradas fueron incluso más cortas (madera: 4,1 s de media contra 5,7 s antes), porque los dados llegan más separados entre sí. |
| La altura depende de cada pantalla | Proporción del teléfono, barras de la interfaz y tamaño de la bandeja cambian el encuadre. | Se recalcula cada vez que cambian el tamaño, el área útil, la torre o la mesa. |
| Las capturas sueltas no alcanzaban a mostrar la caída | Una captura por adb tarda medio segundo. | Se grabó la pantalla (`screenrecord`) y se extrajeron cuadros cada 0,1 s con ffmpeg. |

**Lo que aprendimos.** Para que algo "entre en cuadro" de forma natural, la física no tiene que
saber de pantallas: alcanza con que la vista le diga **desde dónde ya no se ve**, usando la misma
cámara con la que dibuja.

**De acá salió…** que en la bandeja los dados todavía aparecen de golpe junto al borde cercano (no
se cambió en este capítulo).

---

## Capítulo 15 · La franja del pulgar: dados a un toque y sin bordes del sistema

**El pedido.** "Ahora necesito que mejores la UX/UI en base a las mejores prácticas actuales que
incluso solucionen problemas que puedan aparecer actualmente." Junto con un informe sobre cómo
gestionan los controles las apps con un gráfico dominante (cámaras, mapas, dados 3D, juegos de
cartas), hecho a partir de un video de la versión anterior de la pantalla. Las pautas y sus fuentes
quedaron en [GUIA_UX_UI.md](GUIA_UX_UI.md).

**Qué seguía siendo un problema.** Varios puntos del informe ya estaban resueltos desde el
capítulo 12 (mesa a pantalla completa, controles abajo, modo en Ajustes, pistas sólo al principio).
Comparando el informe con la pantalla actual quedaban cuatro:

1. La **manija amarilla del borde derecho**. Además de chocar con el "Atrás" de Android, estaba
   centrada en la altura de la pantalla, y Android sólo respeta la exclusión del gesto en los 200 dp
   de abajo de cada borde: deslizar ahí podía sacar al usuario de la app.
2. **Tocar una ficha de dado no hacía nada**; para agregar un dado había que abrir la solapa.
3. **No se podía saltar la animación** de la tirada.
4. **No había modo para zurdos.**

**La solución.**

- **Sin manija ni gesto desde el borde.** La solapa (Legendario, especiales, skins) se abre con el
  botón "Dados" y se cierra deslizando, tocando afuera o con "Atrás".
- **Carrusel de dados** (`CarruselDados`) en lugar de la fila de fichas: todos los tipos en una fila
  que se desplaza de costado. **Tocar suma uno, mantener quita uno** con "Deshacer", contador dorado
  por tipo, los tipos que no están en la mesa se ven apagados y, después de la tirada, cada ficha
  muestra sus valores (borde dorado si alguno decide el resultado). TalkBack lee, por ejemplo,
  "d6, 2 en la mesa, salió 3, 4. Tocá para agregar, mantené para quitar".
- **Tocar la mesa mientras ruedan los dados** adelanta la tirada hasta el final, sin sonido. El
  marcador lo avisa: "Tocá la mesa para ver el resultado ya".
- **Controles para zurdos** en Ajustes: "Tirada" y "Dados" cambian de lado; LANZAR queda al centro.
  Se guarda en las preferencias.
- Las pistas del marcador ya no mencionan el borde: enseñan a tocar los dados.

**Problemas y soluciones.**

| Problema | Causa | Solución |
|---|---|---|
| Al probar, el nombre del sistema de juego quedaba reducido a ".." con una torre elegida | El nombre del escenario ("Torre de madera") ocupaba todo el ancho que necesitaba | Nombre corto del escenario ("Madera", "Castillo"), ancho máximo de 170 dp y sin las flechas "▾"; TalkBack lee los nombres completos |
| Tocar la mesa durante una tirada no la saltaba | El gesto de la mesa se registra una sola vez y quedaba atado al estado de "saltar" de la primera tirada | Un único estado para toda la vida de la mesa, que se reinicia al empezar cada tirada |
| Al saltar, la pantalla se congelaba unos 400 ms | Se calculaban 3 s de física en un solo cuadro | Medio segundo de tirada por cuadro: se ve un avance rápido (unas 30 veces la velocidad normal) y termina sin trabarse |
| Los botones laterales no compilaban dentro de una lambda | `Modifier.weight` sólo existe dentro de una fila | Las lambdas de los botones son extensiones de `RowScope` |
| Los resultados debajo del nombre del dado no se leían | Letra de 10 sp, apretada en una línea | El valor reemplaza al dado, en 22 sp, con una ficha por dado |
| ¿Cómo distinguir una pulsación larga de un toque o un deslizamiento en la mesa? | El gesto de la mesa decidía todo al soltar el dedo | Se espera hasta 500 ms: si suelta es un toque, si se mueve es un deslizamiento, y si sigue quieto es una pulsación larga |

**Segunda vuelta, con lo visto en el teléfono.** El primer carrusel mostraba cuadros con el nombre
del dado y los resultados en letra chica debajo, que no se apreciaban. El pedido: "El carrusel
debería tener las imágenes en 3D que ya están en el sidebar", los resultados deberían "ocupar el
espacio que tenía el nombre del dado" y verse bien cuando hay varios dados del mismo tipo, y
"debería poder eliminarse un dado manteniéndolo presionado en el dice tray también".

- Antes de tirar, cada tipo se ve con su **dado 3D** (`IconoDado3D`, el mismo de la solapa), con el
  nombre debajo y el contador dorado.
- Después de tirar, **el número ocupa el lugar del dado**: una ficha por dado con el valor en 22 sp,
  sobre el color de su skin (el Legendario muestra su símbolo). Varios dados del mismo tipo forman
  un grupo de fichas bajo un solo nombre ("d6: 5 6"); la que decide el resultado lleva borde dorado.
- **Mantener apretado un dado sobre la mesa 3D lo quita**, con vibración y "Deshacer", igual que en
  el carrusel. La mesa guarda la última cámara con la que dibujó y busca el dado quieto cuyo
  contorno en pantalla contiene el dedo. La pulsación larga se reconoce a los 500 ms sin moverse;
  si el dedo se mueve, sigue siendo un deslizamiento para lanzar, y un toque corto sigue alternando
  la vista (o saltando la tirada).

**Verificación.** Compila y pasan los 60 tests. En un Samsung A33 se comprobó: el carrusel (tocar
suma, mantener quita con "Deshacer"; dados 3D antes de tirar; fichas grandes después, con dos d6
agrupados), quitar un dado manteniéndolo apretado en la mesa, que no queda nada en el borde derecho, el salto de la tirada
(registrado por log) y la barra superior sin recortes. Quedan por probar en el teléfono el modo
zurdo y el comportamiento con navegación por gestos (el A33 usa los tres botones).

**Lo que aprendimos.** Un informe hecho sobre una versión anterior sirve igual, pero hay que
contrastarlo con la pantalla actual antes de tocar código: la mitad ya estaba resuelta, y probar en
el teléfono encontró un problema que el informe no mencionaba (el nombre recortado).

**De acá salió…** las ideas de UX que quedan pendientes en [GUIA_UX_UI.md](GUIA_UX_UI.md) §5
(tiradas recientes a la vista, chip de último resultado, elegir escenario con miniaturas).

---

## Capítulo 16 · Colores que pertenecen al ambiente

**El pedido.** "Lo que me está molestando hace rato es el delineado amarillo que está en los dados
relevantes a la tirada, a la vez que bordeando los dados actuales en el carrusel. ¿Podrías hacer
que el color del delineado y borde tenga coherencia y armonía con el ambiente del lanzador que esté
seleccionado?" Y que los colores de los dados "sean diferentes entre sí, pero tengan algún contraste
interesante con los colores del ambiente seleccionado. Actualmente son todos azules, verdes, violetas".

**La solución.** Un objeto de dominio, `PaletasAmbiente`, con dos cosas por ambiente (bandeja de
tapete, bandeja de madera, taberna, castillo, cuero):

- **Acento**: sale de la luz de cada escena. Ámbar de vela en la taberna, lavanda de antorcha en el
  castillo, turquesa en el cuero, crema sobre el tapete y ámbar suave sobre la madera. Lo usan el
  contorno de los dados que deciden la tirada (en la mesa y en las fichas de resultado), los bordes
  del carrusel y los contadores de la fila y de la solapa.
- **Paleta "Por tipo"**: ocho colores bien distintos que evitan los tonos dominantes de la escena
  (nada verde sobre el tapete, nada marrón ni naranja en la madera, nada violeta, gris ni turquesa
  en el castillo, nada ciruela ni turquesa en el cuero). El Legendario conserva su dorado y las
  skins elegidas a mano no cambian.

La pantalla fija el ambiente activo según la torre o la superficie de la bandeja y lo reparte con
`CompositionLocal` (`LocalAmbiente`, `LocalAcento`); los íconos 3D, el carrusel y la mesa lo leen
para volver a pintarse cuando cambia.

**Problemas y soluciones.**

| Problema | Causa | Solución |
|---|---|---|
| El test de skins fallaba con las paletas nuevas | Exigía que la tinta declarada de "Por tipo" (blanca) se leyera sobre cada color, y ahora hay colores claros | Para "Por tipo" vale la tinta efectiva (pasa a negro sobre colores claros), comprobada en los cinco ambientes por `PaletasAmbienteTest` |
| Sobre el tapete, el rojo del d4 y el magenta del dF se parecían | Los elegí "a ojo" | El test de distancia mínima lo detectó; el dF pasó a violeta |
| En el cuero, el dorado del d4 y el naranja del d10 se parecían | Igual | El d10 pasó a esmeralda |
| En el carrusel, los dados claros (marfil, dorado) mostraban números blancos ilegibles | Las fichas usaban la tinta declarada de la skin ("Por tipo" la declara blanca), no la tinta efectiva que ya usaba la mesa 3D | `colorTinta` usa la tinta efectiva: negro cuando el blanco no se lee sobre el color del dado |
| El resaltado crema no se veía sobre una ficha marfil | Acento y cuerpo casi del mismo tono | Un anillo oscuro fino por dentro del borde de acento en las fichas resaltadas |
| Al cambiar de escenario después de una tirada, la cámara seguía en la toma cenital | La vista del resultado sólo se cerraba si cambiaban los dados de la mesa, y cambiar de escenario los reacomoda sin cambiarlos | Cambiar de escenario o de superficie vuelve a la vista general |

**Verificación.** Compila y pasan los 63 tests. En el Samsung A33 se vieron los cuatro ambientes
(taberna, castillo, cuero y bandeja de tapete) con sus paletas y acentos, una tirada en la taberna
con el Legendario resaltado en ámbar, y el cambio de escenario volviendo a la vista general.

**Lo que aprendimos.** Un color de resaltado fijo funciona en una sola escena; cuando la escena
cambia, el acento tiene que salir de ella. Y elegir colores "a ojo" engaña: un test con una
distancia mínima entre colores encontró dos pares parecidos que en la pantalla no se notaban.

**De acá salió…** que las skins elegidas a mano no se adaptan al ambiente (a propósito); una idea
posible es sugerir, para cada escenario, skins que contrasten con él.

---

## Patrones que se repitieron

1. **Imitar lo que haría una persona.** Verter de a poco, golpear la torre, empujar un dado montado,
   enderezarlo donde está: cada vez que la solución fue "natural", también fue robusta.
2. **Medir antes de suponer.** Latencia, empujones, forzados, tiempos de tirada, píxeles de color:
   los números guiaron cada decisión y quedaron como tests.
3. **Probar en el dispositivo real.** El emulador escondía problemas de rendimiento y de gestos, y
   generaba otros que no existían. Y un cambio hecho sin poder compilar (capítulos 12 y 13) no está
   terminado hasta compilarlo y correr los tests.
4. **Funciones puras para lo que se quiere probar.** Reglas, lector de caras, planificador de
   sonido y sombreado se testean sin Android.
5. **El algoritmo del pintor tiene un costo.** Ver capítulo 11.

## Árbol de ideas: cómo una feature trajo la siguiente

```
Lanzador de un dado ──► muchos dados + reglas de rol
        │
        └─► ¿quién decide el número? ──► dos modos (física / predeterminado) + sonido
                                │
                                └─► muchos dados en pantalla ──► solapa lateral con íconos 3D
                                                │
                                                ├─► dados superpuestos ──► SAT + proyección
                                                └─► elegir colores ──► skins, contraste, creador
                                                                │
                                                                └─► estilos (metal, gema, mármol…)
        ┌───────────────────────────────────────────────────────┘
        └─► una forma más teatral de tirar ──► TowerDice (3 torres)
                    │
                    └─► atascos con muchos dados ──► vertido, lengua, golpecito, reposo promediado
                                │
                                └─► "relanzar lejos" es antinatural ──► lectura respecto del apoyo
                                                │
                                                └─► dados en rampas y montones ──► toma cenital
                                                                │
                                                                └─► color distinto en la cenital ──► reflejos vs. luz difusa
                    ┌───────────────────────────────────────────┘
                    └─► torres más lindas ──► ambiente, luz, aberturas medievales
                                │
                                └─► dados que aparecen de golpe ──► entrada desde fuera de la pantalla

Informe de UX sobre gráficos dominantes ──► sin manija lateral, carrusel de dados, saltar la tirada, modo zurdo
        └─► carrusel con dados 3D y resultados grandes ──► mantener apretado en la mesa también quita
                └─► amarillo fijo y dados de la misma gama ──► acento y paleta "Por tipo" por ambiente

Investigación de apps pagas ──► mesa a pantalla completa (interfaz superpuesta, hojas, inmersivo)
        └─► sus atajos ──► historial, "otra vez", guardadas, ventaja con pulsación larga, agitar, voz
```

## Ideas pendientes (de dónde salieron)

| Idea | Surgió de |
|---|---|
| Pre-simular al apoyar el dedo, para esconder la latencia con 20–30 dados | Capítulo 2 (medición de latencia). |
| Girar los números en la cenital para que se lean derechos | Capítulo 9 (algunos números quedan de cabeza). |
| Atenuar la torre del castillo en la cenital (queda muy cerca de la cámara) | Capítulo 11. |
| Otras aberturas para el cuero (ojos de buey con aro de latón, calado, escudo) | Capítulo 11 (opciones no elegidas). |
| Que en la bandeja los dados también entren desde fuera de la pantalla | Capítulo 14. |
| Sugerir skins que contrasten con cada escenario | Capítulo 16. |

Las ideas que salieron de la investigación de la competencia están en
[INVESTIGACION_COMPETENCIA.md](INVESTIGACION_COMPETENCIA.md) §4.

---

## Plantilla para el próximo capítulo

```markdown
## Capítulo N · Título

**El pedido.** Qué se quería y por qué (con las palabras de quien lo pidió, si ayudan).

**La solución.** La idea central.

**Problemas y soluciones.**

| Problema | Causa | Solución |
|---|---|---|

**Lo que aprendimos.** La lección que sirve fuera de este proyecto.

**De acá salió…** La idea o el pedido siguiente.
```
