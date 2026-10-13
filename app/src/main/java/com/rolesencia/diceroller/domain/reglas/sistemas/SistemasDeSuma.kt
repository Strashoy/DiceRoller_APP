package com.rolesencia.diceroller.domain.reglas.sistemas

import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.CampoParametro
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.NivelResultado
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.domain.reglas.SistemaReglas
import com.rolesencia.diceroller.domain.reglas.Veredicto
import com.rolesencia.diceroller.domain.reglas.pool

// Sistemas donde se SUMAN los dados (más un modificador) y el total se compara con algo.

internal fun conSigno(n: Int) = if (n >= 0) "+$n" else "$n"

/** Elige el dado que se conserva según ventaja/desventaja. */
private fun List<ResultadoDado>.conservar(modo: Conservar): ResultadoDado? = when (modo) {
    Conservar.MAYOR -> maxByOrNull { it.valor }
    Conservar.MENOR -> minByOrNull { it.valor }
    Conservar.TODOS -> firstOrNull()
}

/** Suma todo y listo. El clásico "3d6+2". */
object SumaLibre : SistemaReglas {
    override val id = "suma"
    override val nombre = "Suma libre"
    override val descripcion = "Suma todos los dados más el modificador. Sin juicio de éxito o fallo."
    override val usaModificador = true

    override fun tiradaSugerida() = pool { agregar(TipoDado.D6, 2) }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val suma = resultados.sumOf { it.valor } + parametros.modificador
        return Veredicto(
            nivel = NivelResultado.NEUTRO,
            titulo = "$suma",
            detalle = resultados.joinToString(" + ") { it.valor.toString() } +
                if (parametros.modificador != 0) " (${conSigno(parametros.modificador)})" else "",
            total = suma,
            dadosDestacados = resultados.map { it.dado.id }.toSet()
        )
    }
}

/**
 * D&D 5e / Pathfinder / d20 System: 1d20 + modificador contra una Clase de Dificultad.
 * Con 2d20 se aplica ventaja (MAYOR) o desventaja (MENOR). 20 natural = crítico, 1 natural = pifia.
 */
object D20ContraCd : SistemaReglas {
    override val id = "d20"
    override val nombre = "d20 contra CD (D&D 5e)"
    override val descripcion = "1d20 + modificador ≥ CD es éxito. 20 natural: crítico. 1 natural: pifia. " +
        "Con 2d20 elegí Mayor/Menor para ventaja o desventaja."
    override val usaModificador = true
    override val campoObjetivo = CampoParametro("CD", 1..30, 15)

    override fun tiradaSugerida() = pool { agregar(TipoDado.D20) }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val elegido = resultados.filter { it.tipo == TipoDado.D20 }.conservar(parametros.conservar)
            ?: return Veredicto(NivelResultado.NEUTRO, "Falta el d20")
        val natural = elegido.valor
        val total = natural + parametros.modificador
        val cd = parametros.objetivo
        val detalle = "$natural ${conSigno(parametros.modificador)} = $total" + (cd?.let { " vs CD $it" } ?: "")
        val nivel = when {
            natural == 20 -> NivelResultado.EXITO_CRITICO
            natural == 1 -> NivelResultado.FALLO_CRITICO
            cd == null -> NivelResultado.NEUTRO
            total >= cd -> NivelResultado.EXITO
            else -> NivelResultado.FALLO
        }
        val titulo = when (nivel) {
            NivelResultado.EXITO_CRITICO -> "¡20 natural!"
            NivelResultado.FALLO_CRITICO -> "¡1 natural!"
            NivelResultado.EXITO -> "Éxito"
            NivelResultado.FALLO -> "Fallo"
            else -> "$total"
        }
        return Veredicto(nivel, titulo, detalle, total, setOf(elegido.dado.id))
    }
}

/**
 * Powered by the Apocalypse (Apocalypse World, Dungeon World, Masks…):
 * 2d6 + modificador. 6 o menos fallo, 7–9 éxito parcial (con coste), 10+ éxito completo.
 */
object PoweredByTheApocalypse : SistemaReglas {
    override val id = "pbta"
    override val nombre = "Powered by the Apocalypse"
    override val descripcion = "2d6 + modificador. 6 o menos: fallo. 7–9: éxito parcial (con coste). 10+: éxito completo."
    override val usaModificador = true

    override fun tiradaSugerida() = pool { agregar(TipoDado.D6, 2) }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val total = resultados.sumOf { it.valor } + parametros.modificador
        val (nivel, titulo) = when {
            total >= 10 -> NivelResultado.EXITO to "Éxito completo"
            total >= 7 -> NivelResultado.EXITO_PARCIAL to "Éxito parcial"
            else -> NivelResultado.FALLO to "Fallo"
        }
        return Veredicto(
            nivel, titulo,
            detalle = resultados.joinToString(" + ") { "${it.valor}" } + " ${conSigno(parametros.modificador)} = $total",
            total = total,
            dadosDestacados = resultados.map { it.dado.id }.toSet()
        )
    }
}

/**
 * Cypher System (Numenera, The Strange): 1d20 contra nivel de dificultad × 3.
 * 1 = intrusión del DJ, 17–18 = daño extra, 19 = efecto menor, 20 = efecto mayor.
 */
object CypherSystem : SistemaReglas {
    override val id = "cypher"
    override val nombre = "Cypher System"
    override val descripcion = "1d20 ≥ dificultad × 3. 1 natural: intrusión del DJ. 17–18: +daño. " +
        "19: efecto menor. 20: efecto mayor."
    override val usaModificador = true
    override val campoObjetivo = CampoParametro("Dificultad", 0..10, 4)

    override fun tiradaSugerida() = pool { agregar(TipoDado.D20) }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val d20 = resultados.firstOrNull { it.tipo == TipoDado.D20 }
            ?: return Veredicto(NivelResultado.NEUTRO, "Falta el d20")
        val natural = d20.valor
        val objetivo = (parametros.objetivo ?: 4) * 3
        val exito = natural + parametros.modificador >= objetivo
        val extra = when (natural) {
            20 -> "Efecto mayor"
            19 -> "Efecto menor"
            17, 18 -> "+${natural - 16} de daño"
            else -> null
        }
        val (nivel, titulo) = when {
            natural == 1 -> NivelResultado.FALLO_CRITICO to "Intrusión del DJ"
            exito && natural >= 19 -> NivelResultado.EXITO_CRITICO to "Éxito · $extra"
            exito -> NivelResultado.EXITO to (if (extra != null) "Éxito · $extra" else "Éxito")
            else -> NivelResultado.FALLO to "Fallo"
        }
        return Veredicto(nivel, titulo, "$natural ${conSigno(parametros.modificador)} vs $objetivo", natural, setOf(d20.dado.id))
    }
}

/**
 * Fate Core / Fate Condensed: 4dF + habilidad contra oposición. La diferencia son los "shifts".
 * <0 fallo, 0 empate, 1–2 éxito, 3+ éxito con estilo.
 */
object FateCore : SistemaReglas {
    override val id = "fate"
    override val nombre = "Fate (4dF)"
    override val descripcion = "4 dados Fate (−, 0, +) + habilidad contra oposición. Empate: éxito con coste " +
        "menor. 3 o más de diferencia: éxito con estilo."
    override val usaModificador = true
    override val campoObjetivo = CampoParametro("Oposición", -2..8, 2)

    private val escalera = mapOf(
        8 to "Legendario", 7 to "Épico", 6 to "Fantástico", 5 to "Excelente", 4 to "Grande",
        3 to "Bueno", 2 to "Razonable", 1 to "Normal", 0 to "Mediocre", -1 to "Malo", -2 to "Terrible"
    )

    fun nombreEscalera(valor: Int): String = escalera[valor.coerceIn(-2, 8)] ?: "$valor"

    override fun tiradaSugerida() = pool { agregar(TipoDado.FATE, 4) }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val dados = resultados.filter { it.tipo == TipoDado.FATE }.sumOf { it.valor }
        val total = dados + parametros.modificador
        val oposicion = parametros.objetivo ?: 0
        val shifts = total - oposicion
        val (nivel, titulo) = when {
            shifts >= 3 -> NivelResultado.EXITO_CRITICO to "Éxito con estilo"
            shifts >= 1 -> NivelResultado.EXITO to "Éxito"
            shifts == 0 -> NivelResultado.EXITO_PARCIAL to "Empate"
            else -> NivelResultado.FALLO to "Fallo"
        }
        return Veredicto(
            nivel, titulo,
            detalle = "${conSigno(dados)} ${conSigno(parametros.modificador)} = ${nombreEscalera(total)} ($total) · " +
                "${conSigno(shifts)} shifts",
            total = total,
            dadosDestacados = resultados.filter { it.valor != 0 }.map { it.dado.id }.toSet()
        )
    }
}

/**
 * Daggerheart: dado de Esperanza (d12 normal) + dado de Miedo (d12 especial) + modificador vs dificultad.
 * El dado mayor colorea el resultado. Dobles = crítico (éxito automático).
 */
object Daggerheart : SistemaReglas {
    override val id = "daggerheart"
    override val nombre = "Daggerheart (Esperanza/Miedo)"
    override val descripcion = "2d12: Esperanza (normal) y Miedo (especial) + modificador ≥ dificultad. " +
        "Si sale mayor el de Miedo, el DJ gana Miedo. Dobles: ¡crítico!"
    override val usaModificador = true
    override val campoObjetivo = CampoParametro("Dificultad", 5..30, 12)

    override fun tiradaSugerida() = pool {
        agregar(TipoDado.D12)
        agregar(TipoDado.D12, rol = RolDado.ESPECIAL)
    }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val esperanza = resultados.firstOrNull { it.tipo == TipoDado.D12 && !it.esEspecial }
        val miedo = resultados.firstOrNull { it.tipo == TipoDado.D12 && it.esEspecial }
        if (esperanza == null || miedo == null) return Veredicto(NivelResultado.NEUTRO, "Faltan los d12 de Esperanza y Miedo")
        val total = esperanza.valor + miedo.valor + parametros.modificador
        val exito = total >= (parametros.objetivo ?: 12)
        val conEsperanza = esperanza.valor > miedo.valor
        val (nivel, titulo) = when {
            esperanza.valor == miedo.valor -> NivelResultado.EXITO_CRITICO to "¡Crítico!"
            exito && conEsperanza -> NivelResultado.EXITO to "Éxito con Esperanza"
            exito -> NivelResultado.EXITO_PARCIAL to "Éxito con Miedo"
            conEsperanza -> NivelResultado.FALLO to "Fallo con Esperanza"
            else -> NivelResultado.FALLO_CRITICO to "Fallo con Miedo"
        }
        val destacado = if (conEsperanza) esperanza else miedo
        return Veredicto(
            nivel, titulo, "${esperanza.valor} + ${miedo.valor} ${conSigno(parametros.modificador)} = $total",
            total, setOf(destacado.dado.id)
        )
    }
}

/**
 * Ironsworn / Starforged: dado de acción (d6 + modificador, tope 10) contra dos d10 de desafío.
 * Superar ambos = golpe fuerte, uno = golpe débil, ninguno = fallo. d10 iguales = "coincidencia".
 */
object Ironsworn : SistemaReglas {
    override val id = "ironsworn"
    override val nombre = "Ironsworn"
    override val descripcion = "d6 + modificador (máx. 10) contra 2d10. Superar ambos: golpe fuerte. " +
        "Uno: golpe débil. Ninguno: fallo. Si los d10 coinciden, algo inesperado ocurre."
    override val usaModificador = true

    override fun tiradaSugerida() = pool {
        agregar(TipoDado.D6)
        agregar(TipoDado.D10, 2)
    }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val accion = resultados.firstOrNull { it.tipo == TipoDado.D6 }
        val desafio = resultados.filter { it.tipo == TipoDado.D10 }.take(2)
        if (accion == null || desafio.size < 2) return Veredicto(NivelResultado.NEUTRO, "Necesita 1d6 y 2d10")
        val puntaje = (accion.valor + parametros.modificador).coerceAtMost(10)
        val superados = desafio.count { puntaje > it.valor }
        val coincidencia = desafio[0].valor == desafio[1].valor
        val (nivel, titulo) = when (superados) {
            2 -> (if (coincidencia) NivelResultado.EXITO_CRITICO else NivelResultado.EXITO) to "Golpe fuerte"
            1 -> NivelResultado.EXITO_PARCIAL to "Golpe débil"
            else -> (if (coincidencia) NivelResultado.FALLO_CRITICO else NivelResultado.FALLO) to "Fallo"
        }
        return Veredicto(
            nivel, titulo + if (coincidencia) " · ¡coincidencia!" else "",
            "$puntaje vs ${desafio[0].valor} y ${desafio[1].valor}",
            puntaje,
            setOf(accion.dado.id) + desafio.filter { puntaje > it.valor }.map { it.dado.id }
        )
    }
}
