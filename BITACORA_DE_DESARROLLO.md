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

## Patrones que se repitieron

1. **Imitar lo que haría una persona.** Verter de a poco, golpear la torre, empujar un dado montado,
   enderezarlo donde está: cada vez que la solución fue "natural", también fue robusta.
2. **Medir antes de suponer.** Latencia, empujones, forzados, tiempos de tirada, píxeles de color:
   los números guiaron cada decisión y quedaron como tests.
3. **Probar en el dispositivo real.** El emulador escondía problemas de rendimiento y de gestos, y
   generaba otros que no existían.
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
```

## Ideas pendientes (de dónde salieron)

| Idea | Surgió de |
|---|---|
| Pre-simular al apoyar el dedo, para esconder la latencia con 20–30 dados | Capítulo 2 (medición de latencia). |
| Lanzar agitando el teléfono (acelerómetro) | Plan original. |
| Girar los números en la cenital para que se lean derechos | Capítulo 9 (algunos números quedan de cabeza). |
| Atenuar la torre del castillo en la cenital (queda muy cerca de la cámara) | Capítulo 11. |
| Otras aberturas para el cuero (ojos de buey con aro de latón, calado, escudo) | Capítulo 11 (opciones no elegidas). |

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
