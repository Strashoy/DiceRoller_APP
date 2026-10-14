package com.rolesencia.diceroller.domain.historial

import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.NivelResultado
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada

/**
 * Lo que las apps de dados pagas resuelven igual (Dice by PCalc, D&D Beyond, Roll20, Dice Roller
 * PRO+): la tirada se describe en notación estándar, queda en un historial que se puede repetir y
 * las tiradas frecuentes se guardan con nombre. Todo esto es Kotlin puro y se testea sin Android.
 */

/** Una tirada ya resuelta, tal como se muestra en el historial. */
data class EntradaHistorial(
    /** Momento de la tirada (System.currentTimeMillis). */
    val instante: Long,
    val sistemaId: String,
    val sistemaNombre: String,
    /** "1d20 + 2d8 + 5" */
    val notacion: String,
    /** Valores obtenidos, en orden: "18 · 6 · 5". */
    val valores: String,
    val nivel: NivelResultado,
    val titulo: String,
    val detalle: String,
    /** Lo necesario para repetirla: el pool (codificado como en las preferencias) y los parámetros. */
    val pool: List<DadoEnMesa>,
    val parametros: ParametrosTirada
)

/** Tirada frecuente guardada con nombre ("Espada larga", "Bola de fuego"…). */
data class TiradaGuardada(
    val nombre: String,
    val sistemaId: String,
    val pool: List<DadoEnMesa>,
    val parametros: ParametrosTirada
)

object Notacion {

    /**
     * Notación de dados estándar: agrupa por tipo, de los más grandes a los más chicos (como se
     * escribe en rol), y suma el modificador. Los dados especiales (Hambre, Miedo, Estrés…) se
     * cuentan aparte. Ej.: [d8, d20, d8] con +5 → "1d20 + 2d8 + 5".
     */
    fun de(pool: List<DadoEnMesa>, modificador: Int = 0): String {
        if (pool.isEmpty()) return if (modificador != 0) conSigno(modificador) else "Sin dados"
        val partes = pool
            .groupBy { it.tipo to it.rol }
            .entries
            .sortedBy { -orden(it.key.first) * 10 + it.key.second.ordinal }
            .map { (clave, dados) ->
                val (tipo, rol) = clave
                "${dados.size}${tipo.etiqueta}" + if (rol == RolDado.ESPECIAL) " especial" else ""
            }
        val base = partes.joinToString(" + ")
        return when {
            modificador > 0 -> "$base + $modificador"
            modificador < 0 -> "$base − ${-modificador}"
            else -> base
        }
    }

    /** "18 · 6 · 5"; el dF se escribe con signo y el Legendario con su número de cara. */
    fun valores(resultados: List<ResultadoDado>): String = resultados.joinToString(" · ") { r ->
        when (r.tipo) {
            TipoDado.FATE -> when { r.valor > 0 -> "+"; r.valor < 0 -> "−"; else -> "0" }
            TipoDado.D100_DECENAS -> if (r.valor == 0) "00" else "${r.valor}"
            else -> "${r.valor}"
        }
    }

    // El orden "natural" de los dados en rol: d20 antes que d8, el percentil y el Fate al final.
    private fun orden(tipo: TipoDado): Int = when (tipo) {
        TipoDado.D20 -> 20
        TipoDado.D12 -> 12
        TipoDado.LEGENDARIO -> 11
        TipoDado.D10 -> 10
        TipoDado.D8 -> 8
        TipoDado.D6 -> 6
        TipoDado.D4 -> 4
        TipoDado.D100_DECENAS -> 2
        TipoDado.FATE -> 1
    }

    private fun conSigno(n: Int) = if (n >= 0) "+$n" else "−${-n}"
}

/**
 * Ventaja y desventaja "de un toque", como el botón de tirada de D&D Beyond: en el sistema d20
 * basta un d20 en la mesa; al pedir ventaja o desventaja se agrega el segundo, y al volver a una
 * tirada normal se quita. En los demás sistemas el pool no se toca.
 */
object Ventaja {
    const val SISTEMA_D20 = "d20"

    fun ajustarPool(
        sistemaId: String,
        pool: List<DadoEnMesa>,
        conservar: Conservar,
        skin: SkinDado
    ): List<DadoEnMesa> {
        if (sistemaId != SISTEMA_D20) return pool
        val d20 = pool.filter { it.tipo == TipoDado.D20 }
        return when {
            conservar != Conservar.TODOS && d20.size == 1 -> {
                val id = (pool.maxOfOrNull { it.id } ?: -1) + 1
                // El segundo d20 copia la skin del primero: se ve como "el mismo dado, dos veces".
                pool + DadoEnMesa(id, TipoDado.D20, RolDado.NORMAL, d20.first().skin)
            }
            conservar == Conservar.TODOS && d20.size == 2 -> pool - d20.last()
            d20.isEmpty() && conservar != Conservar.TODOS -> {
                val id = (pool.maxOfOrNull { it.id } ?: -1) + 1
                pool + DadoEnMesa(id, TipoDado.D20, RolDado.NORMAL, skin) + DadoEnMesa(id + 1, TipoDado.D20, RolDado.NORMAL, skin)
            }
            else -> pool
        }
    }
}

/** Límite del historial: alcanza para una sesión larga sin crecer sin fin en DataStore. */
const val MAX_HISTORIAL = 50
