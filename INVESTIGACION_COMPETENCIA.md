# Investigación de la competencia — apps de dados para rol

> Qué hacen las apps de dados que la gente paga, cómo se promocionan y qué patrones de interfaz
> comparten. Es material de referencia: **no describe la app**, sino el mercado en el que se
> inspiró. Lo que se llegó a implementar está contado en
> [BITACORA_DE_DESARROLLO.md](BITACORA_DE_DESARROLLO.md) (capítulos 12 y 13).
>
> Investigación hecha en octubre de 2026. Precios, calificaciones y catálogos cambian con el tiempo.

## 1. Apps pagas o que venden dados digitales

| App | Plataforma | Modelo de negocio | Qué la distingue |
|---|---|---|---|
| **Dice by PCalc** | iOS, Mac, Apple Watch | Pago (US$1,99) + compras dentro de la app | Simulación con física de dados poliédricos para rol y gráficos 3D casi fotorrealistas. 4,9 estrellas con 322 reseñas. Historial de tiradas, tiradas guardadas (*presets*) y lectura del resultado en voz alta. |
| **D&D Beyond** (sets de dados 3D) | Web, iOS, Android | App gratis; los sets de dados se venden en su Marketplace | El caso más grande de gente que paga por dados digitales. Cada set tiene diseño, colores y efectos de sonido propios. También los usa como incentivo: quien reservaba el libro *Icewind Dale* recibía un set exclusivo. Registro de tiradas y menú de ventaja/desventaja en el botón de tirada. |
| **dddice** | Web, integraciones | Temas de la comunidad | Lanzador 3D con más de 2000 temas de dados creados por la comunidad y un editor para personalizarlos. Se integra con D&D Beyond. |
| **DiceShaker 3D** | iOS | Freemium: app gratis, "Power Pack" opcional y sets premium a US$0,99 | Agitar el teléfono para lanzar; estadísticas de equidad de los dados. |
| **Roll20** | Web | Plataforma de mesa virtual | Notación de dados estándar (`1d20 + 5`) y repetir tiradas desde el registro. |
| **Dice Roller PRO+** | Android | Pago | Historial y tiradas guardadas. |

## 2. Cómo se promocionan (tipos de video)

| Tipo de video | Ejemplo | Qué muestra |
|---|---|---|
| **Tráiler animado con humor** | D&D Beyond (2018): canción al estilo de los dibujos animados de los 80, con la voz de Sam Riegel (Critical Role). | Marca y tono, más que funciones. |
| **"Dev Update"** | D&D Beyond anuncia funciones nuevas, como los dados compartidos en tiempo real. | Novedades para quienes ya usan la app. |
| **Vista previa de la tienda** (App Store / Play Store) | Pip RPG Dice, D&D Beyond. | 15–30 s de pantalla: dados rebotando, totales y personalización. |
| **Clips cortos de una sola función** | Dice by PCalc tirando un d20 y un d8 en el Apple Watch (publicado por su desarrollador). | Una función vistosa: reloj, realidad aumentada, cientos de dados a la vez. No es un tráiler producido. |
| **Tutoriales e integraciones** | dddice conectado con D&D Beyond. | Lo fácil que es empezar. |
| **Concursos** | D&D Beyond regala dados digitales a los ganadores. | Comunidad y coleccionismo. |

Para verlos, buscar en YouTube: "Dice by PCalc", "D&D Beyond digital dice", "D&D Beyond Dev Update
dice", "dddice D&D Beyond", "D&D Beyond trailer 2018". (La búsqueda original no pudo obtener
enlaces directos a los videos.)

**Conclusión.** Casi nadie vende la utilidad de tirar dados. Lo que se vende es la **física
realista**, la **estética de los sets** y el **placer de coleccionarlos**.

## 3. Patrones de interfaz que se repiten

Se tomaron como referencia sólo los patrones que aparecen en **varias** apps, no los de una sola:

| Patrón | Quién lo usa |
|---|---|
| Los dados son el protagonista y ocupan la pantalla | Dice by PCalc, D&D Beyond, DiceShaker |
| Pocos controles, abajo, que se apartan mientras ruedan los dados | Dice by PCalc, D&D Beyond, DiceShaker |
| Resultado grande y con color, como un marcador de juego | Dice by PCalc, D&D Beyond |
| Historial de tiradas con resultado | Dice by PCalc, D&D Beyond, Dice Roller PRO+ |
| Repetir una tirada ("otra vez") | D&D Beyond, Roll20 |
| Tiradas guardadas con nombre | Dice by PCalc, Dice Roller PRO+ |
| Notación de dados (`1d20 + 2d8 + 5`) | Roll20, D&D Beyond, dddice |
| Ventaja/desventaja desde el botón de tirada | D&D Beyond, Dice by PCalc |
| Agitar el teléfono para lanzar | DiceShaker y la mayoría de los lanzadores móviles |
| Leer el resultado en voz alta | Dice by PCalc |

Cuáles de estos se implementaron y cómo: bitácora, capítulos 12 y 13.

## 4. Ideas que salieron de esta investigación y todavía no están implementadas

| Idea | Inspirada en |
|---|---|
| Packs de skins / sets de dados: núcleo gratis y sets pagos | DiceShaker 3D, D&D Beyond |
| "Modo vitrina" sin interfaz y con cámara lenta, para grabar el video promocional | Los clips cortos de una sola función |
| Dados con caras personalizadas (texto, emoji, imagen) | Dice by PCalc |
| Estadísticas de equidad de los propios dados | DiceShaker 3D |
| Tiradas guardadas agrupadas por personaje | Dice by PCalc |
| Compartir una tirada del historial con el grupo | D&D Beyond (dados compartidos) |

## 5. Fuentes

- App Store – Dice by PCalc
- AppFollow – Dice by PCalc
- Mac4Ever – Dice by PCalc en Apple Watch
- D&D Beyond – Dados compartidos
- Prima Games – Dados de Icewind Dale
- Black Sci-Fi – Tráiler de D&D Beyond
- Online Dice Rollers – dddice
- App Store – DiceShaker 3D
- ArkTimes – Colección de dados en D&D Beyond
