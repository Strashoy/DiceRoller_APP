package com.rolesencia.diceroller.domain.dados

import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.SkinDado

/**
 * Tipos de dado físicos que la mesa sabe simular.
 *
 * [valoresCaras] es el valor impreso en cada cara, en el MISMO orden que las normales
 * de [com.rolesencia.diceroller.fisica.GeometriaDados]. Así el lector de caras 3D sólo
 * devuelve un índice y el valor sale de acá.
 */
enum class TipoDado(val etiqueta: String, val valoresCaras: List<Int>) {
    D4("d4", (1..4).toList()),
    D6("d6", (1..6).toList()),
    D8("d8", (1..8).toList()),
    D10("d10", (0..9).map { if (it == 0) 10 else it }),   // la cara "0" vale 10
    D12("d12", (1..12).toList()),
    D20("d20", (1..20).toList()),

    /** Dado de decenas para tiradas de percentil (00, 10 … 90). Se combina con un D10. */
    D100_DECENAS("d%", (0..9).map { it * 10 }),

    /** Dado Fate/Fudge: dos caras "−", dos en blanco y dos "+". */
    FATE("dF", listOf(-1, -1, 0, 0, 1, 1)),

    /**
     * Dado Legendario de Rolesencia: un d12 con símbolos en vez de números.
     * 1–7 Heptagrama (7 caras), 8–9 Ying-Yang (2 caras), 10–12 Mega (3 caras).
     */
    LEGENDARIO("dL", (1..12).toList());

    val caras: Int get() = valoresCaras.size
    val minimo: Int get() = valoresCaras.min()
    val maximo: Int get() = valoresCaras.max()
}

/**
 * Papel que cumple un dado dentro de la tirada. Cada sistema de reglas decide qué significa
 * [ESPECIAL]: Hambre en Vampiro V5, Estrés en Alien (Year Zero), Miedo en Daggerheart,
 * el Dado Legendario en Rolesencia…  La UI lo pinta de otro color.
 */
enum class RolDado { NORMAL, ESPECIAL }

/** Un dado puesto sobre la mesa, antes de lanzarse. */
data class DadoEnMesa(
    val id: Int,
    val tipo: TipoDado,
    val rol: RolDado = RolDado.NORMAL,
    /** Apariencia elegida al agregarlo (no afecta reglas ni física). */
    val skin: SkinDado = CatalogoSkins.POR_TIPO
)

/** El valor leído de un dado una vez quieto. */
data class ResultadoDado(
    val dado: DadoEnMesa,
    val valor: Int
) {
    val tipo: TipoDado get() = dado.tipo
    val esEspecial: Boolean get() = dado.rol == RolDado.ESPECIAL
}
