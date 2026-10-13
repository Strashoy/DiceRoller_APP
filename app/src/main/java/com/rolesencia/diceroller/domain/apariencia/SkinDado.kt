package com.rolesencia.diceroller.domain.apariencia

import com.rolesencia.diceroller.domain.dados.TipoDado

enum class CategoriaSkin(val titulo: String) {
    POR_TIPO("Por tipo"),
    CLASICOS("Colores clásicos"),
    ESPECIALES("Efectos"),
    GEMAS("Gemas"),
    METALES("Metales"),
    TEMATICOS("Temáticos"),
    SISTEMA("Por sistema de juego"),
    PERSONALIZADAS("Mis skins")
}

/**
 * Cómo se pinta el cuerpo del dado. Los que usan [SkinDado.color2] lo indican con [etiquetaColor2].
 */
enum class EstiloSkin(val titulo: String, val etiquetaColor2: String?) {
    SOLIDO("Sólido", null),
    /** Caras alternadas de dos colores (las opuestas quedan de distinto color). */
    BICOLOR("Bicolor", "Segundo color"),
    /** Del cuerpo al segundo color de arriba hacia abajo del dado (fijo al dado, gira con él). */
    DEGRADADO("Degradado", "Color final"),
    /** Translúcido: se ven las caras de atrás a través de las de adelante, con brillo. */
    GEMA("Gema translúcida", null),
    /** Reflejo especular fuerte que se mueve al rodar. */
    METALICO("Metálico", null),
    /** Vetas que atraviesan cada cara. */
    MARMOL("Mármol", "Vetas"),
    /** Puntos que destellan según hacia dónde mira cada cara. */
    PURPURINA("Purpurina", "Brillos"),
    /** Números y aristas que brillan como fosforescentes. */
    BRILLO_NOCTURNO("Brillo nocturno", "Resplandor"),
    /** Una imagen del usuario en cada cara (una para todas o una por cara). */
    IMAGEN("Imagen por cara", null)
}

/** Tipografía de los números. ROMANA además los escribe en números romanos. */
enum class Tipografia(val titulo: String) {
    CLASICA("Clásica"),
    MODERNA("Moderna"),
    TECNICA("Técnica"),
    MANUSCRITA("Manuscrita"),
    ANTIGUA("Antigua"),
    CONDENSADA("Condensada"),
    ROMANA("Romana")
}

/**
 * Apariencia de un dado: colores, estilo del cuerpo, tipografía y, opcionalmente, imágenes.
 *
 * @param cuerpo color del dado; `null` = un color distinto según el tipo (skin "Por tipo").
 * @param tinta color de los números o símbolos. Si en alguna cara no contrasta lo suficiente,
 *   el dibujo usa negro o blanco en esa cara (ver [tintaEfectiva]).
 * @param borde color de las aristas; `null` = el cuerpo oscurecido.
 * @param color2 segundo color del estilo (bicolor, final del degradado, vetas, brillos, resplandor).
 * @param imagenes rutas de imágenes guardadas por la app (estilo IMAGEN).
 */
data class SkinDado(
    val id: String,
    val nombre: String,
    val categoria: CategoriaSkin,
    val cuerpo: Long?,
    val tinta: Long,
    val borde: Long? = null,
    val estilo: EstiloSkin = EstiloSkin.SOLIDO,
    val color2: Long? = null,
    val tipografia: Tipografia = Tipografia.CLASICA,
    val imagenes: List<String> = emptyList()
) {
    fun cuerpoPara(tipo: TipoDado): Long = cuerpo ?: CatalogoSkins.colorPorTipo(tipo)

    fun bordePara(tipo: TipoDado): Long = borde ?: ColorArgb.mezclar(cuerpoPara(tipo), 0xFF000000L, 0.45f)

    /** El segundo color, con un valor razonable si la skin no lo trae. */
    fun color2Para(tipo: TipoDado): Long = color2 ?: when (estilo) {
        EstiloSkin.MARMOL -> ColorArgb.mezclar(cuerpoPara(tipo), 0xFF808080L, 0.6f)
        EstiloSkin.PURPURINA, EstiloSkin.BRILLO_NOCTURNO -> 0xFFFFFFFFL
        else -> ColorArgb.mezclar(cuerpoPara(tipo), 0xFF000000L, 0.5f)
    }

    /**
     * Color base de la cara [indice] (antes de la iluminación). Sólo cambia con BICOLOR y DEGRADADO;
     * en el degradado [alturaLocal] va de -1 (abajo del dado) a 1 (arriba).
     */
    fun colorCara(tipo: TipoDado, indice: Int, alturaLocal: Float): Long = when (estilo) {
        EstiloSkin.BICOLOR -> if (indice % 2 == 0) cuerpoPara(tipo) else color2Para(tipo)
        EstiloSkin.DEGRADADO -> ColorArgb.mezclar(cuerpoPara(tipo), color2Para(tipo), ((1f - alturaLocal) / 2f).coerceIn(0f, 1f))
        else -> cuerpoPara(tipo)
    }

    /** La tinta de la skin si se lee sobre [fondo]; si no, negro o blanco. Nunca un número ilegible. */
    fun tintaEfectiva(fondo: Long): Long =
        if (ColorArgb.contraste(tinta, fondo) >= 4.5) tinta else ColorArgb.tintaLegible(fondo)

    /**
     * En tonos medios (el centro de un degradado, por ejemplo) ni el negro ni el blanco llegan a 4,5:
     * en esas caras los números se dibujan con un contorno del color opuesto.
     */
    fun necesitaContorno(fondo: Long): Boolean = ColorArgb.contraste(tintaEfectiva(fondo), fondo) < 4.5

    val esPersonalizada: Boolean get() = categoria == CategoriaSkin.PERSONALIZADAS
}

/** Números romanos para la tipografía ROMANA (0 se escribe "N", de *nulla*). */
fun aRomano(n: Int): String {
    if (n == 0) return "N"
    if (n < 0) return "−" + aRomano(-n)
    val valores = listOf(100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
    var resto = n
    return buildString {
        for ((v, s) in valores) while (resto >= v) { append(s); resto -= v }
    }
}
