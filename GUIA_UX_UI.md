# Guía de UX/UI — Dice Roller

> Experiencias y pautas de interfaz que guían la pantalla principal: qué dicen la ergonomía, las
> normas de Android/Material y las apps que conviven con un gráfico dominante (cámaras, mapas,
> lanzadores de dados 3D, juegos de cartas), y **cómo se aplicó cada una en Dice Roller**.
>
> - Lo que pasó en cada cambio (problemas, soluciones): [BITACORA_DE_DESARROLLO.md](BITACORA_DE_DESARROLLO.md).
> - Apps de dados pagas y cómo se promocionan: [INVESTIGACION_COMPETENCIA.md](INVESTIGACION_COMPETENCIA.md).
>
> Base: informe "Cómo gestionan las apps con un elemento gráfico dominante el resto de controles"
> (octubre de 2026), hecho a partir de un video de la versión anterior de la pantalla, cuando la mesa
> estaba arriba, los controles debajo con scroll y los dados se agregaban desde una manija lateral.

## 1. Principios

| # | Principio | Por qué | En Dice Roller |
|---|---|---|---|
| 1 | **El gráfico ocupa la pantalla y los controles se superponen abajo** | Ninguna app de referencia pone el canvas arriba y los controles "debajo con scroll". Cámaras (Halide: "todos los controles al alcance del pulgar"), mapas (hoja inferior no modal) y dados 3D (Dice by PCalc, Dicenomicon) superponen o anclan abajo. | Mesa 3D a sangre; barra superior y franja inferior superpuestas sobre degradés. La cámara encuadra la mesa **en el área que no tapan las barras**. |
| 2 | **La acción principal, abajo y al centro** | Hoober (UXmatters): 49 % usa el teléfono con una mano; la precisión va de 7 mm en el centro a 12 mm en las esquinas. Fitts: el objetivo frecuente, grande y cerca del pulgar. | LANZAR de 64 dp al centro de la franja inferior; secundarios a sus lados; lo terciario (historial, ajustes) arriba. |
| 3 | **Los bordes laterales son del sistema** | Con navegación por gestos (Android 10+), deslizar desde un borde lateral es "Atrás". La exclusión (`setSystemGestureExclusionRects`) sólo se respeta en los **200 dp de abajo** de cada borde y es "una vía de escape, no un patrón" (Chris Banes). Google reconoció que sólo un 3–7 % abría cajones deslizando desde el borde. | **Sin manija ni gesto desde el borde.** La solapa de dados se abre con el botón "Dados". |
| 4 | **Todo gesto tiene un control visible** | Los gestos (deslizar, agitar, mantener) se descubren poco; Material pide siempre una alternativa visible. Las reseñas de Dice by PCalc se quejan de que agitar "necesitaba una sacudida fuerte". | Deslizar sobre la mesa y agitar son atajos; LANZAR siempre está. Mantener LANZAR (ventaja) lo anuncia la pista de las primeras tiradas. |
| 5 | **Componer y después confirmar** | Owlbear Rodeo 1.0 tiraba al tocar el ícono del dado y confundía; en 2.0 se arma la tirada y se confirma con "Roll". | Tocar un dado lo **suma**; tirar es siempre LANZAR. |
| 6 | **La selección de dados, en una fila al alcance del pulgar** | Dicenomicon y Dice by PCalc: barra inferior de tipos de dado; tocar tres veces "d6" = 3d6. | **Carrusel de dados** sobre los botones, con el **mismo dado 3D** de la solapa y de la mesa (lo que se elige es lo que cae): tocar suma uno, mantener quita uno (con "Deshacer"), contador dorado en cada tipo. |
| 7 | **El resultado, grande y sobre el gráfico** | El zoom de cámara ya pone el foco en los dados; la suma debe aparecer ahí, no en otra parte de la pantalla. | Marcador grande arriba con el color del veredicto; toma cenital de los dados. En el carrusel, **el número ocupa el lugar del dado**: una ficha grande por dado, en el color de su skin, agrupadas bajo el nombre del tipo cuando hay varios; borde dorado en las que deciden el resultado. |
| 8 | **Una física espectacular tiene que poder acortarse** | Usuarios de D&D Beyond piden que los dados no "reboten como gomitas 3 segundos". | **Tocar la mesa mientras ruedan los dados adelanta la tirada hasta el final**, sin sonido. El marcador lo avisa. |
| 9 | **Opciones de baja frecuencia, fuera de la vista principal** | Escenarios, modo de decisión, sonido: se cambian poco y no merecen espacio permanente. | Escenario en un menú de la barra superior; modo, superficie, sonido y pantalla en la hoja de Ajustes. |
| 10 | **Hojas inferiores para lo secundario** | Material 3 y NN/g: hojas cortas, que se cierran con un control visible y con "Atrás", sin apilarlas. | Sistema, Tirada, Historial y Ajustes son hojas inferiores; se cierran deslizando, tocando afuera o con "Atrás". |
| 11 | **Ergonomía para cada mano** | Dos tercios de los agarres a una mano son con la derecha, pero no todos. La comunidad de Balatro móvil pidió modo zurdo/diestro. | **Controles para zurdos** en Ajustes: "Tirada" y "Dados" cambian de lado; LANZAR queda al centro. |
| 12 | **Ayuda que se retira sola** | Un texto de ayuda permanente consume altura y sólo sirve la primera vez. | El marcador enseña los gestos en las tres primeras tiradas y después muestra la notación ("2d6 + 4"). |
| 13 | **Objetivos táctiles ≥ 48 dp** | Material: 48 × 48 dp con 8 dp de separación (Apple: 44 pt). | Botones inferiores de 64 dp, píldoras de 48 dp, fichas del carrusel de 58 × 56 dp con 8 dp entre sí. |
| 14 | **Un mismo gesto hace lo mismo en todas partes** | La coherencia hace que un gesto aprendido sirva en toda la app (heurística de consistencia de Nielsen). | Mantener apretado un dado lo quita, tanto en el carrusel como **sobre la mesa 3D**, y siempre se puede deshacer. |
| 15 | **Lo más importante nunca se recorta** | En una fila con poco espacio, el elemento que define el contexto (el sistema de juego) tiene que leerse entero. | El escenario usa un nombre corto y tiene un ancho máximo; el sistema de juego toma el resto. |
| 16 | **El resaltado pertenece al ambiente** | Un amarillo fijo encima de cada escena se siente pegado; el color de acento debe salir de la luz de la escena para que el resalte se lea sin romper la armonía. | El contorno de los dados que deciden la tirada, los bordes del carrusel y los contadores usan el **acento del ambiente**: ámbar de vela (taberna), lavanda de antorcha (castillo), turquesa (cuero), crema o ámbar suave (bandeja). |
| 17 | **Los dados contrastan con su escena y entre sí** | Si todos los dados comparten gama (azules, verdes, violetas) cuesta distinguirlos y se pierden contra fondos de esa gama. | La skin "Por tipo" usa una **paleta por ambiente**: ocho colores bien distintos que evitan los tonos dominantes de esa escena. Un test exige distancia mínima entre colores y números legibles en los cinco ambientes. |
| 18 | **Accesible sin mirar** | TalkBack debe poder leer el resultado y cada control. | Marcador como región "viva"; cada ficha del carrusel dice "d6, 2 en la mesa, salió 3, 4. Tocá para agregar, mantené para quitar"; voz opcional. |

## 2. Diagnóstico de la versión anterior y estado actual

| Problema (informe) | Gravedad | Estado |
|---|---|---|
| Manija amarilla en el borde derecho para agregar dados | Alta | **Resuelto**: sin manija; botón "Dados" y carrusel. Además estaba centrada en la altura, fuera de los 200 dp que Android deja excluir, así que podía disparar "Atrás". |
| Lanzar, suma, selección, historial y modificadores fuera de la vista (scroll) | Alta | **Resuelto** (capítulo 12): todo en la franja inferior y la barra superior, sin scroll. |
| Chips de escenario cortados abajo | Media | **Resuelto**: el escenario se elige en un menú de la barra superior. |
| Pestañas "Física real / Resultado predeterminado" arriba | Media | **Resuelto**: es un modo, está en Ajustes (botones segmentados). |
| Texto de ayuda permanente | Baja | **Resuelto**: pistas sólo en las primeras tiradas. |
| Resultado separado del gráfico | Media | **Resuelto**: marcador superpuesto, toma cenital y valores en el carrusel. |
| Tocar una ficha de dado no hacía nada | Media | **Resuelto**: tocar suma, mantener quita. |
| No se podía saltar la animación | Media | **Resuelto**: tocar la mesa adelanta la tirada. |
| Con una torre elegida, el nombre del sistema de juego quedaba reducido a ".." (detectado al probar) | Media | **Resuelto**: el escenario muestra un nombre corto ("Castillo") con tope de ancho, sin flechas; TalkBack lee los nombres completos. |

## 3. Patrones de referencia (resumen)

- **Lienzo a sangre + controles en el tercio inferior (HUD)**: cámaras (Halide, iOS Camera, Pixel Camera). Exige degradés y que la cámara 3D encuadre la acción por encima de los controles.
- **Gestos directos sobre el objeto** (deslizar, lanzar con un dedo, agitar): cero distancia entre acción y resultado, poca descubribilidad → siempre con botón.
- **Hoja inferior con alturas (peek / media / completa)**: Google y Apple Maps. Muchos controles cerca del pulgar sin sacar el gráfico.
- **FAB / barra flotante**: D&D Beyond abre la bandeja de dados con un FAB; Material 3 Expressive propone la *floating toolbar*.
- **Carrusel horizontal tipo "dial"**: el modo de la cámara, la barra de dados de Dicenomicon. La ficha cortada en el borde comunica "hay más", sólo si está al alcance.
- **Lienzo que se reencuadra según el estado**: Owlbear Rodeo (bandeja chica/grande), Dice by PCalc (cámara cinematográfica en críticos). Las transiciones deben respetar la zona que tapan los controles.
- **Cajones desde el borde**: patrón a evitar como único acceso desde que el borde es "Atrás".
- **Juegos de cartas en vertical**: lo que pide la comunidad (mods de Balatro) son botones grandes, fijos, sin scroll y modo zurdo/diestro.

## 4. Cómo validar en el teléfono

- [ ] Prueba de pulgar a **una mano**, derecha e izquierda (con y sin "Controles para zurdos"), en un teléfono de 6,5" o más: contar cuántas veces hay que cambiar el agarre por tirada.
- [ ] Medir "abrir la app → tirar 2d6 + 3".
- [ ] Con **navegación por gestos** activada (el Samsung A33 de las pruebas usa los tres botones, así que esto queda pendiente): deslizar desde cada borde lateral a distintas alturas y comprobar que siempre es "Atrás" y que ningún control vive en esa franja.
- [ ] Con TalkBack: recorrer el carrusel, lanzar y escuchar el veredicto.
- [ ] Teléfono con recorte de cámara: nada importante queda debajo del recorte.
- [ ] Tocar la mesa durante una tirada de 20 dados en una torre: el resultado aparece casi al instante.

## 5. Ideas de UX todavía no implementadas

| Idea | De dónde |
|---|---|
| Hoja inferior con "peek" de **tiradas recientes** visible sobre la franja inferior (alternar ataque y daño de un toque) | Owlbear Rodeo 2.0: "Recientes" es el atajo que más fricción ahorra |
| Chip de "último resultado" junto a LANZAR al volver a la vista general; tocarlo abre el historial | Informe, propuesta de layout |
| Elegir escenario con miniaturas grandes en una hoja "Mesa", o deslizando dentro de la mesa | Informe, propuesta de layout |
| Relanzar un dado concreto deslizándolo con el dedo | Dice by PCalc (flick) |
| Respetar "quitar animaciones" del sistema en las transiciones de cámara | Accesibilidad de Android |

## 6. Fuentes

- Hoober, S. "How Do Users Really Hold Mobile Devices?" (UXmatters, 2013) y "Design for Fingers, Touch and People, Part 3" (UXmatters, 2017).
- Android Developers Blog: "Gesture Navigation: A Backstory" (2019); Chris Banes, "Gesture Navigation: handling gesture conflicts"; documentación de `View.setSystemGestureExclusionRects`.
- Material Design 3: Bottom sheets, FAB menu, Accessibility (tamaños táctiles); Material 3 Expressive.
- Apple Human Interface Guidelines: Sheets; notas de iOS 16.4 (`presentationBackgroundInteraction`).
- NN/g: "Bottom Sheets: Definition and UX Guidelines"; "Accidental Dismissal of Overlays".
- Owlbear Rodeo: "Owlbear Rodeo 2.0 Dice Deep Dive"; Owlbear Rodeo legacy (bandeja chica/grande).
- Dice by PCalc: pcalc.com/dice, App Store, TidBITS ("Take a Chance with Dice by PCalc").
- The Dicenomicon: reseña en *Riding to Hell in a Handbasket* (2014).
- D&D Beyond: foros de soporte y de sugerencias (bandeja de dados).
- Halide (Lux): "An Introduction to Halide"; ayuda de Pixel Camera y de iPhone Camera.
- Balatro: Engadget, TouchArcade; mods "Balatro Portrait Mobile" y "Dual Screen" (comunitarios).

Limitaciones del informe: no se pudieron ver las galerías de capturas (Mobbin, Page Flows,
Dribbble); los datos de Hoober son de 2013 y de teléfonos más chicos; los mods de Balatro son de la
comunidad, no del estudio.
