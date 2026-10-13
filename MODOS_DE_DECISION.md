# Cómo se decide el número de cada dado

Dice Roller tiene dos modos, cada uno en su pestaña de la app y en su propio paquete de código:

| | **Física real** | **Resultado predeterminado** |
|---|---|---|
| ¿Quién decide el número? | La simulación: es la cara que quedó arriba | Se decide antes (al azar o a mano) |
| ¿Cuándo empieza a moverse? | Enseguida | Después de pre-simular toda la tirada |
| ¿Lo que se ve es la física? | Sí, cuadro a cuadro | Sí: la misma trayectoria, con el dado "re-etiquetado" |
| Código | `tirada/fisicareal/` | `tirada/predeterminada/` |

Los dos comparten la misma física (`fisica/motor/MotorFisicoPropio.kt`) y las mismas
**garantías de resultado** (`tirada/SimulacionTirada.kt`).

---

## 1. La física (común a ambos modos)

`MotorFisicoPropio` es un motor de cuerpos rígidos pensado para dados. Unidades reales: cm, s.

1. **Mallas reales.** Cada dado es un poliedro convexo (`Poliedros.kt`): vértices y caras con su
   normal. El d10 es un trapezoedro pentagonal cuya altura se calcula para que cada cara sea plana.
   Las caras opuestas suman como en un dado de verdad (7 en el d6, 21 en el d20…).
2. **Sub-pasos fijos de 1/240 s.** En cada uno:
   * gravedad (981 cm/s²) sobre las velocidades;
   * **contactos**: vértices del dado por debajo del piso o fuera de las paredes, y vértices de un
     dado dentro de otro (se prueban contra los planos de sus caras);
   * **impulsos secuenciales** (el método de Box2D): impulso normal con rebote (restitución) y
     corrección de penetración, más fricción de Coulomb en dos direcciones. Cada contacto arranca
     con el impulso que necesitó en el paso anterior (*warm starting*), lo que permite apilar
     dados sin que tiemblen;
   * integración de posición y rotación (cuaterniones), amortiguación y freno de rodadura;
   * un dado quieto durante 0,2 s se **duerme**; si otro lo golpea, se despierta.
3. **Determinismo.** Con la misma semilla, la misma tirada da exactamente el mismo resultado
   (test `la_simulacion_es_determinista`). Es lo que permite grabar y reproducir en el modo 2.
4. **Equiprobabilidad.** El test `el_d6_fisico_no_esta_cargado` lanza 600 d6 físicos y aplica χ²
   (resultado típico: χ² ≈ 2–7, límite 20,5): la física no favorece ninguna cara.

### Garantías de resultado (`SimulacionTirada`)

Pase lo que pase, la tirada termina con todos los dados apoyados sobre una cara legible:

1. **Contención**: piso, paredes y techo; si un dado atravesara algo, vuelve al centro.
2. **Reposo**: se espera a que todos los dados duerman.
3. **Dados montados** (inclinados sobre otro dado o contra la pared, alineación < 0,95): se los
   empuja hacia arriba y lejos de sus vecinos, como haría un jugador, hasta 3 veces. Si persiste,
   se los apoya sobre la cara más cercana.
4. **Temblores**: si unos pocos dados siguen despiertos sin cambios durante 1,5 s (por ejemplo un
   d10 balanceándose sobre la arista de un d6), se los empuja.
5. **Límite de tiempo**: a los 8 s de simulación se apoya lo que siga moviéndose.
6. **Sin superposiciones**: los dados nunca quedan metidos uno dentro de otro.
   * El choque entre dados usa el **teorema del eje separador (SAT)**: prueba las normales de las
     caras de ambos dados y los productos cruz de sus aristas. Así detecta también el cruce arista
     contra arista, que la prueba de "vértice dentro del otro dado" no veía.
   * Después de cada sub-paso se separan directamente las posiciones de cualquier par que se
     superponga más de 0,4 mm (proyección, sin agregar velocidad).
   * Un dado nunca arranca dentro de otro (se lo sube hasta que quede libre). Al acomodar un dado
     en su lugar se lo saca del vecino por el camino más corto (milímetros), nunca se lo mueve de sitio.
   * Antes de terminar se verifica que no quede ningún par superpuesto.
   * Medido (test `SolapamientosTest`, 50 tiradas de 2 a 20 dados): superposición máxima 0 al
     inicio, 0,8 mm en el peor sub-paso y 0,4 mm al final (imperceptible).

Cuánto actúan (test `EstadisticasFisicaTest`, 20 tiradas por fila, tiempos simulados):

| Dados | Duración media | Máxima | Empujones por tirada | Asentados a la fuerza por tirada |
|---|---|---|---|---|
| 2 | 0,71 s | 0,92 s | 0 | 0 |
| 5 | 0,95 s | 1,67 s | 0,1 | 0 |
| 10 | 1,07 s | 1,92 s | 0,2 | 0 |
| 20 | 2,66 s | 8,00 s | 2,3 | 0,75 |
| 30 | 6,35 s | 8,00 s | 6,0 | 2,85 |

Hasta 10 dados nunca hace falta forzar nada. Con 20–30 dados se forman pilas (igual que en una
bandeja real) y las garantías trabajan; la bandeja crece automáticamente de 18 a 24 y 30 cm.
Cada tirada muestra en su apartado cuántas garantías se aplicaron.

---

## 2. Modo 1 · Física real

**El número lo decide la simulación.** (`SesionFisicaReal`)

1. Al lanzar se crean los cuerpos con su impulso inicial (gesto o al azar). Nada más.
2. En cada cuadro de pantalla (~16 ms) la mesa llama a `avanzar(dt)` con el tiempo real
   transcurrido; la sesión ejecuta los sub-pasos de 1/240 s que correspondan (acumulador de
   tiempo). Si el teléfono se traba, no intenta "alcanzar" más de 0,1 s de golpe.
3. Se dibuja exactamente la orientación que calculó el motor.
4. Cuando `SimulacionTirada` garantiza que todo está apoyado, `LectorCara` **lee** la cara de
   arriba de cada dado (rota las normales de cara y elige la más vertical; el d4 se lee por abajo).

---

## 3. Modo 2 · Resultado predeterminado

**El número se decide antes y la física se "ajusta" sin trampa visible.**
(`PreSimulador` + `SesionResultadoPredeterminado`)

1. Se decide el número de cada dado: sorteado, o elegido a mano en el apartado
   (tocando cada ficha se cambia su valor).
2. **Pre-simulación**: se corre la tirada entera sin dibujarla, lo más rápido posible, y se graba
   una "película" a 60 cuadros por segundo con los eventos sonoros y su instante.
3. Para cada dado se compara la cara que dejó la física (L) con la deseada (D) y se busca una
   **rotación de simetría** S del poliedro que lleve D a la posición de L (`Simetrias.kt`):
   un giro que deja la malla idéntica (mismos vértices, mismas caras) pero cambia qué número está
   en cada lugar. Ejemplos reales de la app: d6 de 2 a 6 → 90°, d4 → 120°, d10 → 180°.
4. Se reproduce la película dibujando cada dado con orientación `q(t) · S` **desde el primer
   cuadro**. Como S no cambia la forma, los rebotes, choques y sonidos son exactamente los que
   calculó la física; lo único distinto es el número impreso arriba al final.

Los tests `el_modo_predeterminado_muestra_exactamente_el_valor_pedido` (20 tiradas con los 8
tipos de dado) y `hay_una_simetria_entre_cualquier_par_de_caras` lo verifican.

---

## 4. La demora entre apretar y ver moverse los dados

Cada tirada muestra en su apartado: **preparación** (del toque a la sesión lista), **del toque al
primer cuadro en movimiento** y, en el modo 2, **pre-simulación**. También se escribe en Logcat
con la etiqueta `DiceRoller`.

Medido en el emulador (Small Phone, GPU por software, más lento que un teléfono real):

| Escenario | Pre-simulación | Del toque al primer movimiento |
|---|---|---|
| Física real, 2 dados | — | 112 ms |
| Física real, 9 dados | — | 122–130 ms |
| Predeterminado, 2 dados | 29–50 ms | 127–187 ms |
| Predeterminado, 9 dados | 74–284 ms | 163–329 ms |
| Primera tirada tras abrir la app (carga de clases/JIT) | — | ~450 ms |

En la JVM de escritorio (test `cuanto_tarda_la_pre_simulacion`), la pre-simulación de d6 tarda:
1 dado ≈ 0 ms · 5 ≈ 3 ms · 10 ≈ 13 ms · 20 ≈ 60 ms · 30 ≈ 300 ms.

Conclusiones:

* En física real casi toda la demora es el propio dibujo de la pantalla (esperar el siguiente
  cuadro): es la mínima posible.
* En el modo predeterminado se suma la pre-simulación, que crece con la cantidad de dados y,
  sobre todo, con **lo que dura la tirada** (una tirada que rebota más es más larga de calcular).
  Hasta ~10 dados se percibe casi inmediato; con 20–30 dados empieza a notarse.
* Formas de esconderla, si hiciera falta: empezar a pre-simular al **apoyar el dedo** (antes de
  soltar), reducir asignaciones de memoria en el motor (hoy usa vectores inmutables por
  claridad), o grabar con menos sub-pasos.

Para medir en tu teléfono: instalá el APK debug, abrí la pestaña de cada modo y lanzá.

---

## 5. Sonido: cómo se aprovechan las 65 tomas

Grabaciones: 13 bancos × 5 tomas (lona y madera × ligero/pesado × suave/fuerte, dado contra dado
ligero/pesado, roce y asentamiento). Cada choque del motor pasa por `PlanificadorSonido`:

| Técnica | Qué aporta |
|---|---|
| **Eventos físicos reales** | Cada impacto, roce y asentamiento sale del motor con su velocidad y su punto: ya no hay un "sonido de caída" fijo. |
| **Bolsa aleatoria por banco** | Se reparten las 5 tomas en orden al azar; nunca suena la misma dos veces seguidas. |
| **Capa suave/fuerte por probabilidad** | Entre intensidad 0,25 y 0,55 la elección es gradual, no un corte: impactos parecidos suenan distinto. |
| **Superposición en golpes fuertes** | Por encima de 0,75 se suma una toma suave como "cuerpo": 25 combinaciones por banco. |
| **Tono por tamaño** | d4 +7 %, d6 +3 %, d8 0, d10 −2 %, d12 −4 %, d20 −6 %, ± 2,5 % al azar y un leve brillo con la fuerza. |
| **Paneo estéreo** | Según dónde ocurre el choque en la bandeja (potencia constante). |
| **Material de mesa elegible** | Tapete (lona) o madera: el piso usa sus 20 tomas; las paredes siempre suenan a madera. |
| **Dado contra dado** | Ligero/ligero, pesado/pesado o mezcla; los rozones usan el banco de roce (más grave para dados pesados). |
| **Asentamiento** | Suena cuando cada dado se duerme. |
| **Control de densidad** | La misma fuente no se repite antes de 35 ms; máximo 6 sonidos nuevos por cuadro, priorizando los más fuertes; 16 voces. |

Resultado medido en la app: una tirada de 9 dados produjo 115 choques → 79 reproducciones con
**48 de las 65 tomas distintas**. El test `una_tirada_tipica_usa_muchas_tomas_distintas` comprueba
que una secuencia de 400 choques variados usa las 65.

---

## 6. Bandeja (DiceTray) o torre (TowerDice)

Debajo de la mesa se elige dónde se lanzan los dados: **Bandeja** o una de tres torres,
inspiradas en modelos reales:

| Torre | Interior | Bandeja | Sonido del piso |
|---|---|---|---|
| **Torre de madera** | 3 escalones que se cruzan (izquierda, derecha, salida) | Rectangular con almenas, ladrillo grabado | Madera |
| **Castillo de piedra** | Torre octogonal con 4 tramos de escalera | Base redonda de piedra | Madera (piedra) |
| **Caja de cuero** | Un deflector y rampa de salida de fieltro azul | Laterales curvos de cuero con costura azul | Tapete (fieltro) |

Cómo funciona:

* Cada torre es un conjunto de **piezas físicas fijas** (cajas orientadas: paredes, rampas, bordes)
  que el motor trata como cuerpos estáticos con la misma detección SAT que los dados. Cada pieza
  tiene su material (rebote, fricción y sonido).
* Al lanzar, los dados se **sueltan por la boca de la torre** en capas (3 × 2 por capa, cada capa
  más alta), bajan rebotando por las rampas y salen por la abertura del frente a la bandeja.
  Con torre, el deslizamiento del dedo sólo dispara el lanzamiento.
* Cada torre tiene su **ambiente** (luz de vela en la taberna, antorchas violetas y niebla en el
  castillo, luces violeta y turquesa en el cuero) y el frente **cerrado con aberturas** por donde se
  ve caer a los dados en parte: ventanas con parteluz (madera), puerta en arco, ventanas góticas y
  troneras (castillo) y una ventana de medio punto con ribete de cuero y doble pespunte dorado (cuero). La física no cambia.
* Funciona con **los dos modos** de decisión: en el predeterminado la caída completa por la torre
  se pre-simula y se reproduce con las rotaciones de simetría.
* **Garantía extra**: si un dado queda quieto dentro de la torre, la torre recibe golpecitos. Si aun
  así queda plano sobre una rampa, vale como en la mesa real: se lee respecto de la rampa.
* **Muchos dados**: se vierten **de a uno** (cada 0,12 s), la rampa de salida sigue 4 cm dentro de
  la bandeja (lengua de salida). Si un dado se queda quieto dentro de la torre, la torre recibe un
  **golpecito** enseguida. Con 15 dados una tirada dura 3,3–4 s de media (máx. 5,6 s) sin acomodar
  nada; con 20, unos 5–5,7 s (incluye 2,4 s de vertido), sin llegar nunca al límite de tiempo.

## Lectura respecto del apoyo (sin teletransportes)

Ningún dado aparece de golpe en otro lado del tablero. La cara de arriba se lee **respecto de la
superficie que sostiene al dado**, no sólo de la vertical:

* El motor calcula, para cada dado, su **normal de apoyo** (`EstadoCuerpo.apoyo`): agrupa sus
  contactos por "con quién" (piso, cada rampa, cada dado vecino), pesa cada grupo por la carga que
  soporta, descarta lo que tenga más de 60° de pendiente (paredes, caras empinadas) y se queda con la
  superficie contra la que el dado tiene una cara más pareja. Así la presión lateral de un montón no
  inclina la lectura.
* `LectorCara.leer(tipo, orientacion, arriba)` es el mismo lector de siempre con otra "vertical": un
  dado apoyado plano sobre una rampa o sobre la cara de otro dado muestra la cara paralela a esa
  superficie, y es válido.
* Un dado **montado** (sobre una arista o un vértice) recibe un **empujoncito en el lugar** (hacia
  arriba y lejos de los dados y paredes en que se recuesta), hasta 3 veces. Si persiste, se lo
  **acomoda donde está** (`asentarEnSitio`): se lo gira lo mínimo para que la cara más cercana quede
  paralela a su apoyo y se lo separa milímetros de lo que toque.
* Si pasan 1,5 s sin que ningún dado nuevo se duerma (unos pocos se zarandean en un montón), los
  que ya están planos se asientan ahí mismo y sólo los montados reciben empujoncito.
* En el modo predeterminado la rotación de simetría conserva esta lectura: la cara que queda
  paralela al apoyo es la pedida.
* El aro del castillo se abre siempre frente a la puerta, aunque crezca la bandeja.
* Separación mínima de 3 cm entre rampas para que ningún dado (el d20 mide ~2,9 cm) quede encajado.

Medido (test `TorreTest`, 10 tiradas por fila; tiempos simulados):

| Torre | 1 dado | 3 dados | 6 dados | 10 dados |
|---|---|---|---|---|
| Madera | 1,1 s | 1,2 s | 1,3 s | 1,5 s (0 forzados) |
| Castillo | 1,3 s | 1,4 s | 1,7 s | 2,9 s (0,3 forzados por tirada) |
| Cuero | 1,0 s | 1,0 s | 1,1 s | 1,3 s (0 forzados) |

En todos los casos los dados terminan en la bandeja, sin superponerse. En el celular (modo
predeterminado, 8 dados, caja de cuero) el primer movimiento llegó a los 278 ms.

