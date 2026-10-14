package com.rolesencia.diceroller.domain.apariencia

import com.rolesencia.diceroller.domain.dados.TipoDado

/**
 * Colores que dependen de dónde se lanza (bandeja de tapete o de madera, o una de las torres):
 *
 * - **Paleta "Por tipo"**: un color bien distinto para cada tipo de dado, elegido para **contrastar
 *   con el ambiente**. Cada ambiente evita los tonos que dominan su escena: nada verde sobre el tapete,
 *   nada marrón ni naranja en la madera, nada violeta, gris ni turquesa en el castillo, nada ciruela ni
 *   turquesa en la caja de cuero.
 * - **Acento**: el color con que se resaltan los dados que deciden la tirada y los dados elegidos del
 *   carrusel. Sale de la luz de cada escena (vela, antorcha violeta, foco turquesa) para que el
 *   resaltado se sienta parte del ambiente y no un amarillo pegado encima.
 *
 * El Legendario conserva siempre su dorado: es su identidad.
 */
object PaletasAmbiente {

    const val BANDEJA_LONA = "bandeja-lona"
    const val BANDEJA_MADERA = "bandeja-madera"
    const val TORRE_MADERA = "madera"
    const val TORRE_CASTILLO = "castillo"
    const val TORRE_CUERO = "cuero"

    val claves = listOf(BANDEJA_LONA, BANDEJA_MADERA, TORRE_MADERA, TORRE_CASTILLO, TORRE_CUERO)

    /** Clave del ambiente: la torre si hay una, o la bandeja según su superficie. */
    fun clave(torre: String?, bandejaDeMadera: Boolean): String =
        torre ?: if (bandejaDeMadera) BANDEJA_MADERA else BANDEJA_LONA

    private const val DORADO_LEGENDARIO = 0xFFC9A227L

    // Orden: d4, d6, d8, d10, d12, d20, d%, dF.
    private val paletas: Map<String, List<Long>> = mapOf(
        // Tapete verde: rojo, marfil, azules, dorado, negro, naranja y violeta; ningún verde.
        BANDEJA_LONA to listOf(0xFFC62828L, 0xFFF1E6C8L, 0xFF1E5BD8L, 0xFFF2A900L, 0xFF1E1E22L, 0xFFE8641EL, 0xFF4FC3F7L, 0xFF8E5BD8L),
        // Madera clara: tonos fríos y joya; ningún marrón ni naranja.
        BANDEJA_MADERA to listOf(0xFF00A3A3L, 0xFFF3EAD3L, 0xFF2851C9L, 0xFFC0262DL, 0xFF1F1F24L, 0xFF9CCC3AL, 0xFF64B5F6L, 0xFFE0559BL),
        // Taberna (roble oscuro, luz de vela): joyas frías y claras que brillan contra lo cálido.
        TORRE_MADERA to listOf(0xFF00A6A6L, 0xFFF2E8D0L, 0xFF2B5BD6L, 0xFFC62832L, 0xFF2FA35AL, 0xFFB0B8C4L, 0xFF7FD3F5L, 0xFFD9479AL),
        // Castillo (piedra gris, antorchas violetas, niebla): cálidos y saturados; ningún violeta ni gris.
        TORRE_CASTILLO to listOf(0xFFC62828L, 0xFFF2B630L, 0xFF2E9E5BL, 0xFFE8701EL, 0xFFF2E8D0L, 0xFFA6D13AL, 0xFFFF7A6BL, 0xFF1E1E22L),
        // Caja de cuero (ciruela y turquesa): dorado, rojo, marfil, esmeralda, lima, azul, negro y rosa; ningún violeta ni turquesa.
        TORRE_CUERO to listOf(0xFFF2B630L, 0xFFC62832L, 0xFFF2E8D0L, 0xFF2E9E5BL, 0xFFA6D13AL, 0xFF3D7BE0L, 0xFF1E1E22L, 0xFFF48FB1L)
    )

    private val acentos: Map<String, Long> = mapOf(
        BANDEJA_LONA to 0xFFF6E7C1L,    // tiza crema sobre el tapete
        BANDEJA_MADERA to 0xFFFFC98AL,  // ámbar suave
        TORRE_MADERA to 0xFFFFB45AL,    // luz de vela
        TORRE_CASTILLO to 0xFFC9A2FFL,  // antorcha violeta
        TORRE_CUERO to 0xFF4FE8D0L      // foco turquesa
    )

    /** Color del tipo de dado en ese ambiente (skin "Por tipo"). */
    fun porTipo(clave: String, tipo: TipoDado): Long {
        if (tipo == TipoDado.LEGENDARIO) return DORADO_LEGENDARIO
        val paleta = paletas[clave] ?: paletas.getValue(BANDEJA_LONA)
        val indice = when (tipo) {
            TipoDado.D4 -> 0
            TipoDado.D6 -> 1
            TipoDado.D8 -> 2
            TipoDado.D10 -> 3
            TipoDado.D12 -> 4
            TipoDado.D20 -> 5
            TipoDado.D100_DECENAS -> 6
            else -> 7
        }
        return paleta[indice]
    }

    /** Color de resaltado del ambiente. */
    fun acento(clave: String): Long = acentos[clave] ?: acentos.getValue(BANDEJA_LONA)
}
