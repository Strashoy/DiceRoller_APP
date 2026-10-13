# Skins de dados — referencia

> Qué es una skin, qué hay implementado, el catálogo de colores y las ideas para el creador.
> Código: `domain/apariencia/` (modelo, catálogo, ideas) y `ui/skins/` (selector y creador).

## 1. Qué hay hoy

* **Skins de color**: cada dado guarda su skin al agregarse (cuerpo, números/símbolos y aristas).
  No afecta la física ni las reglas: es sólo apariencia.
* **Selector** en la solapa de dados (donde antes decía "Dados" / "Especial"): un desplegable con
  las skins agrupadas por categoría, más:
  * **Crear skin…** → abre el creador.
  * **Pintar los dados de la mesa** → aplica la skin elegida a todos los dados ya agregados.
  * **Agregar como especial** → los próximos dados se agregan con rol especial (Hambre, Miedo,
    Estrés…). Se distinguen por sus **aristas rojas**, con cualquier skin.
  * En "Mis skins", ✎ edita y ✕ borra.
* **El Dado Legendario conserva su dorado** con los símbolos: es la identidad del dado.
* **Legibilidad garantizada**: el test `SkinsTest` exige contraste WCAG ≥ 4,5 entre cuerpo y
  números en todas las skins del catálogo y en todo lo que generan las ideas del creador.

## 2. Catálogo de colores

La skin **Por tipo** (la de siempre) pinta cada tipo de dado de un color distinto con números blancos:
d4 `#00796B`, d6 `#6A1B9A`, d8 `#1565C0`, d10 `#283593`, d% `#37474F`, d12 `#2E7D32`,
d20 `#4527A0`, dF `#5D4037`.

Las demás tienen un solo color. "Automático" en aristas = el cuerpo oscurecido un 45 %.
El contraste va de 1 (invisible) a 21 (negro sobre blanco); 4,5 o más se lee bien.

### Colores clásicos

| Skin | Cuerpo | Números / símbolos | Aristas | Contraste |
|---|---|---|---|---|
| Marfil | `#F3EEDF` | `#111111` | automático | 16.3 |
| Blanco | `#FAFAFA` | `#111111` | automático | 18.1 |
| Negro | `#1C1C1C` | `#FFFFFF` | automático | 17.0 |
| Gris | `#616161` | `#FFFFFF` | automático | 6.2 |
| Rojo | `#C62828` | `#FFFFFF` | automático | 5.6 |
| Granate | `#7B1E2B` | `#FFFFFF` | automático | 10.2 |
| Naranja | `#F57C00` | `#111111` | automático | 7.0 |
| Amarillo | `#F9D71C` | `#111111` | automático | 13.3 |
| Verde lima | `#9CCC65` | `#111111` | automático | 10.1 |
| Verde | `#2E7D32` | `#FFFFFF` | automático | 5.1 |
| Verde bosque | `#1B5E20` | `#FFFFFF` | automático | 7.9 |
| Turquesa | `#26A69A` | `#111111` | automático | 6.3 |
| Celeste | `#4FC3F7` | `#111111` | automático | 9.4 |
| Azul | `#1565C0` | `#FFFFFF` | automático | 5.7 |
| Azul marino | `#1A237E` | `#FFFFFF` | automático | 13.2 |
| Violeta | `#6A1B9A` | `#FFFFFF` | automático | 9.4 |
| Lavanda | `#B39DDB` | `#111111` | automático | 7.9 |
| Rosa | `#F48FB1` | `#111111` | automático | 8.5 |
| Fucsia | `#C2185B` | `#FFFFFF` | automático | 5.9 |
| Marrón | `#5D4037` | `#FFFFFF` | automático | 9.3 |
| Beige | `#D7C4A3` | `#111111` | automático | 11.1 |

### Gemas

| Skin | Cuerpo | Números / símbolos | Aristas | Contraste |
|---|---|---|---|---|
| Rubí | `#9B111E` | `#FFFFFF` | `#4A0008` | 8.4 |
| Esmeralda | `#046307` | `#FFFFFF` | `#012A03` | 7.5 |
| Zafiro | `#0F52BA` | `#FFFFFF` | `#06275A` | 7.2 |
| Amatista | `#7E57C2` | `#FFFFFF` | `#3E2370` | 5.2 |
| Topacio | `#FFC87C` | `#111111` | `#B07A2A` | 12.4 |
| Ónix | `#0B0B0B` | `#FFFFFF` | `#5A5A5A` | 19.7 |
| Perla | `#EDE7E0` | `#111111` | `#B8AFA4` | 15.4 |
| Jade | `#00A86B` | `#111111` | `#00573A` | 6.1 |
| Aguamarina | `#7FFFD4` | `#111111` | `#2E9C7F` | 15.4 |
| Citrino | `#E4D00A` | `#111111` | `#8A7C00` | 12.0 |

### Metales

| Skin | Cuerpo | Números / símbolos | Aristas | Contraste |
|---|---|---|---|---|
| Oro | `#D4AF37` | `#111111` | `#8C6D1F` | 9.0 |
| Plata | `#C0C0C0` | `#111111` | `#7A7A7A` | 10.4 |
| Bronce | `#CD7F32` | `#111111` | `#7A4A1A` | 6.0 |
| Cobre | `#B87333` | `#111111` | `#6E4019` | 5.0 |
| Oro rosa | `#B76E79` | `#111111` | `#6E3A42` | 5.0 |
| Hierro | `#434B4D` | `#FFFFFF` | `#1F2324` | 8.9 |

### Temáticos

| Skin | Cuerpo | Números / símbolos | Aristas | Contraste |
|---|---|---|---|---|
| Rolesencia | `#0D1B2A` | `#FFD700` | `#FFA000` | 12.4 |
| Vampiro | `#120A0A` | `#FF5252` | `#5C0000` | 6.1 |
| Hielo | `#E3F2FD` | `#0D47A1` | `#90CAF9` | 7.6 |
| Fuego | `#FF6F00` | `#2B0A00` | `#B71C1C` | 6.6 |
| Bosque élfico | `#1B5E20` | `#E6C35C` | `#0B2E0F` | 4.6 |
| Neón | `#101018` | `#00E5FF` | `#FF00E5` | 12.3 |
| Hueso y sangre | `#E3DAC9` | `#8B0000` | `#9E9076` | 7.2 |
| Pergamino | `#F2E8D5` | `#3E2723` | `#A1887F` | 11.4 |
| Abismo | `#0A1A2F` | `#7FDBFF` | `#14365C` | 11.2 |
| Tóxico | `#1B1B1B` | `#76FF03` | `#33691E` | 13.2 |
| Realeza | `#4A148C` | `#FFD54F` | `#240A45` | 8.4 |

## 3. El creador de skins

Pensado para que cualquiera pueda diseñar, desde lo más simple hasta lo más artístico:

1. **Editor de color** para *cuerpo*, *números* y *aristas*: tono, saturación y luz con pistas de
   color, código hex (para copiar exactamente un color de otra app) y muestras rápidas.
2. **Automáticos**: números negro/blanco según cuál se lea mejor; aristas = cuerpo oscurecido.
3. **Ideas a partir del cuerpo** (`IdeaSkin`): Complementario, Análogo, Tríada, Pastel, Neón,
   Monocromo, Metálico y Sorpresa. Cada una aplica una regla de armonía de color y después ajusta
   la luz de los números hasta que se lean.
4. **Colores desde una imagen**: el selector de fotos del sistema (sin permisos) carga una
   imagen, AndroidX Palette extrae hasta 12 colores (dominante, vivos, apagados, claros, oscuros)
   y se pueden tocar para aplicarlos, o usar **la paleta completa** (cuerpo = dominante,
   números = el color de la imagen que mejor contrasta, aristas = el más oscuro).
5. **Vista previa 3D** con la misma malla y el mismo pintor que la mesa (d6, d20, d10, d12) y el
   contraste de los números en vivo.

Las skins del usuario se guardan en DataStore ("Mis skins") y viajan con cada dado del pool.

## 4. Tipos de skin (implementados)

| Estilo | Cómo se dibuja | Segundo color |
|---|---|---|
| **Sólido** | Un color, iluminado por cara | — |
| **Bicolor** | Caras alternadas de dos colores (las opuestas quedan distintas) | Segundo color |
| **Degradado** | Del cuerpo al segundo color de arriba hacia abajo del dado; gira con él | Color final |
| **Gema translúcida** | Primero las caras de atrás, oscuras y translúcidas; encima las de adelante con destello | — |
| **Metálico** | Poca luz difusa y reflejo especular fuerte que se desliza al rodar | — |
| **Mármol** | Tres vetas procedurales por cara, fijas al dado, recortadas a la cara | Vetas |
| **Purpurina** | 16 destellos por cara que titilan según hacia dónde mira la cara | Brillos |
| **Brillo nocturno** | Números sin sombra con resplandor y aristas luminosas | Resplandor |
| **Imagen por cara** | Una imagen para todas las caras o una por cara (hasta 20), con números contorneados | — |

**Tipografías**: Clásica, Moderna, Técnica, Manuscrita, Antigua, Condensada y Romana
(números romanos; el 0 se escribe "N").

**Por sistema de juego**: con la opción "Skins del sistema" activada, los dados se agregan con
la skin del sistema: V5 (Vampiro y Hambre), Daggerheart (Esperanza y Miedo), Year Zero (Base y
Estrés), Shadowrun (Matriz), Cthulhu (Mythos), Fate (Pergamino), PbtA (Apocalipsis) y Blades
(Humo y acero).

**Legibilidad**: en cada cara se usa la tinta de la skin si contrasta ≥ 4,5; si no, negro o blanco;
y en tonos medios (el centro de un degradado), los números llevan contorno. `SkinsTest` lo
comprueba cara por cara en todo el catálogo.

## 5. Más ideas para el creador

* **Tomar el color de un dado real con la cámara** (misma extracción de paleta que con una imagen).
* **Código para compartir**: exportar una skin como texto corto (`DR1|nombre|#cuerpo|#tinta|#aristas`)
  para pasarla por chat y pegarla en otro teléfono.
* **Sets coordinados**: una skin que asigna un tono distinto a cada tipo de dado manteniendo
  la armonía (como "Por tipo", pero creada por el usuario).
* **Modo daltonismo**: avisar si dos skins de la mesa se confunden para protanopía/deuteranopía.
* **Aleatoria por tirada**: cada lanzamiento pinta los dados con una idea "Sorpresa".
* **Galería comunitaria** de skins (requiere backend; fuera del alcance actual).
