package com.rolesencia.diceroller.domain.reglas.sistemas

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
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

// Sistemas de "pool": se lanzan varios dados y se cuentan éxitos o se mira el más alto.

/**
 * Forged in the Dark (Blades in the Dark, Scum and Villainy…): se mira el d6 más alto.
 * 1–3 fallo, 4–5 éxito parcial, 6 éxito, dos o más 6 = crítico.
 * Con 0 dados en la acción: se lanzan 2 y se conserva el MENOR (sin crítico posible).
 */
object ForgedInTheDark : SistemaReglas {
    override val id = "fitd"
    override val nombre = "Forged in the Dark (Blades)"
    override val descripcion = "Pool de d6, cuenta el más alto. 1–3: fallo. 4–5: éxito parcial. 6: éxito. " +
        "Dos 6: crítico. Con 0 dados: lanzar 2 y elegir Menor."

    override fun tiradaSugerida() = pool { agregar(TipoDado.D6, 2) }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val d6 = resultados.filter { it.tipo == TipoDado.D6 }
        if (d6.isEmpty()) return Veredicto(NivelResultado.NEUTRO, "Faltan d6")
        val sinDados = parametros.conservar == Conservar.MENOR
        val clave = if (sinDados) d6.minOf { it.valor } else d6.maxOf { it.valor }
        val seises = d6.count { it.valor == 6 }
        val (nivel, titulo) = when {
            !sinDados && seises >= 2 -> NivelResultado.EXITO_CRITICO to "¡Crítico!"
            clave == 6 -> NivelResultado.EXITO to "Éxito total"
            clave >= 4 -> NivelResultado.EXITO_PARCIAL to "Éxito parcial"
            else -> NivelResultado.FALLO to "Fallo"
        }
        val destacados = if (nivel == NivelResultado.EXITO_CRITICO) d6.filter { it.valor == 6 }
        else listOfNotNull(d6.firstOrNull { it.valor == clave })
        return Veredicto(nivel, titulo, "Dado clave: $clave", clave, destacados.map { it.dado.id }.toSet())
    }
}

/**
 * Conteo de éxitos genérico y configurable. Cubre el caso "sólo los 10 en d10 son éxitos"
 * y la familia Storyteller/Storytelling.
 *
 * @param umbral valor mínimo para que un dado sea éxito.
 * @param unosRestan cada 1 cancela un éxito (Mundo de Tinieblas clásico); 0 éxitos con algún 1 = pifia.
 * @param explotaCon los dados con este valor o más se relanzan y se suman (10-again).
 * @param dobleCon los dados con este valor o más cuentan 2 éxitos (Exalted).
 * @param excepcional con esta cantidad de éxitos o más es éxito excepcional.
 * [ParametrosTirada.objetivo] = éxitos necesarios.
 */
class ConteoExitos(
    override val id: String,
    override val nombre: String,
    private val tipo: TipoDado = TipoDado.D10,
    private val umbral: Int,
    private val unosRestan: Boolean = false,
    private val explotaCon: Int? = null,
    private val dobleCon: Int? = null,
    private val excepcional: Int? = null,
    private val dadosIniciales: Int = 5
) : SistemaReglas {

    override val descripcion: String = buildString {
        append("Pool de ${tipo.etiqueta}: cada dado con $umbral o más es un éxito.")
        if (dobleCon != null) append(" Los $dobleCon+ cuentan doble.")
        if (unosRestan) append(" Cada 1 resta un éxito; sin éxitos y con algún 1 es pifia.")
        if (explotaCon != null) append(" Los $explotaCon+ se vuelven a lanzar.")
        if (excepcional != null) append(" $excepcional+ éxitos: excepcional.")
    }
    override val campoObjetivo = CampoParametro("Éxitos necesarios", 1..10, 1)

    override fun tiradaSugerida() = pool { agregar(tipo, dadosIniciales) }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val dados = resultados.filter { it.tipo == tipo }
        val aciertos = dados.filter { it.valor >= umbral }
        val dobles = if (dobleCon == null) 0 else aciertos.count { it.valor >= dobleCon }
        val brutos = aciertos.size + dobles
        val unos = dados.count { it.valor == 1 }
        val exitos = if (unosRestan) (brutos - unos).coerceAtLeast(0) else brutos
        val necesarios = parametros.objetivo ?: 1
        val (nivel, titulo) = when {
            unosRestan && brutos == 0 && unos > 0 -> NivelResultado.FALLO_CRITICO to "¡Pifia!"
            excepcional != null && exitos >= excepcional -> NivelResultado.EXITO_CRITICO to "Éxito excepcional"
            exitos >= necesarios -> NivelResultado.EXITO to "Éxito"
            else -> NivelResultado.FALLO to "Fallo"
        }
        return Veredicto(nivel, titulo, "$exitos éxito(s) de $necesarios", exitos, aciertos.map { it.dado.id }.toSet())
    }

    override fun dadosExplosivos(ultimaRonda: List<ResultadoDado>, siguienteId: Int): List<DadoEnMesa> {
        val limite = explotaCon ?: return emptyList()
        var id = siguienteId
        return ultimaRonda.filter { it.tipo == tipo && it.valor >= limite }.map { DadoEnMesa(id++, tipo, it.dado.rol) }
    }
}

/**
 * Shadowrun (5e/6e): pool de d6, 5 y 6 son "hits". Más de la mitad de unos = glitch;
 * glitch sin hits = glitch crítico. [ParametrosTirada.objetivo] = umbral de hits.
 */
class Shadowrun(private val reglaDelSeis: Boolean = false) : SistemaReglas {
    override val id = if (reglaDelSeis) "shadowrun_edge" else "shadowrun"
    override val nombre = if (reglaDelSeis) "Shadowrun (con Edge)" else "Shadowrun"
    override val descripcion = "Pool de d6: cada 5 o 6 es un hit. Si más de la mitad son 1: glitch. " +
        "Glitch sin hits: glitch crítico." + if (reglaDelSeis) " Regla del Seis: los 6 se vuelven a lanzar." else ""
    override val campoObjetivo = CampoParametro("Umbral", 1..10, 2)

    override fun tiradaSugerida() = pool { agregar(TipoDado.D6, 8) }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val d6 = resultados.filter { it.tipo == TipoDado.D6 }
        val hits = d6.filter { it.valor >= 5 }
        val glitch = d6.count { it.valor == 1 } * 2 > d6.size
        val umbral = parametros.objetivo ?: 1
        val logrado = hits.size >= umbral
        val (nivel, titulo) = when {
            glitch && hits.isEmpty() -> NivelResultado.FALLO_CRITICO to "¡Glitch crítico!"
            logrado && glitch -> NivelResultado.EXITO_PARCIAL to "Éxito con glitch"
            logrado -> NivelResultado.EXITO to "Éxito"
            glitch -> NivelResultado.FALLO to "Fallo con glitch"
            else -> NivelResultado.FALLO to "Fallo"
        }
        return Veredicto(nivel, titulo, "${hits.size} hit(s) de $umbral", hits.size, hits.map { it.dado.id }.toSet())
    }

    override fun dadosExplosivos(ultimaRonda: List<ResultadoDado>, siguienteId: Int): List<DadoEnMesa> {
        if (!reglaDelSeis) return emptyList()
        var id = siguienteId
        return ultimaRonda.filter { it.tipo == TipoDado.D6 && it.valor == 6 }.map { DadoEnMesa(id++, TipoDado.D6) }
    }
}

/**
 * Vampiro: la Mascarada 5ª ed.: pool de d10, 6+ es éxito; cada PAR de 10 suma 4 éxitos (crítico).
 * Dados ESPECIALES = Hambre: un 10 de Hambre en un crítico lo vuelve "caótico"; un 1 de Hambre
 * en una tirada fallida es un "fallo bestial". [ParametrosTirada.objetivo] = dificultad.
 */
object VampiroV5 : SistemaReglas {
    override val id = "v5"
    override val nombre = "Vampiro V5 (Hambre)"
    override val descripcion = "Pool de d10: 6+ es éxito. Cada par de 10 vale 4 éxitos (crítico). " +
        "Los dados especiales son de Hambre: crítico con un 10 de Hambre = caótico; " +
        "fallo con un 1 de Hambre = bestial."
    override val campoObjetivo = CampoParametro("Dificultad", 1..10, 3)

    override fun tiradaSugerida() = pool {
        agregar(TipoDado.D10, 4)
        agregar(TipoDado.D10, 2, RolDado.ESPECIAL)
    }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val d10 = resultados.filter { it.tipo == TipoDado.D10 }
        val aciertos = d10.filter { it.valor >= 6 }
        val dieces = d10.filter { it.valor == 10 }
        val pares = dieces.size / 2
        val exitos = aciertos.size + pares * 2
        val dificultad = parametros.objetivo ?: 1
        val logrado = exitos >= dificultad
        val (nivel, titulo) = when {
            logrado && pares > 0 && dieces.any { it.esEspecial } -> NivelResultado.EXITO_CRITICO to "Crítico caótico"
            logrado && pares > 0 -> NivelResultado.EXITO_CRITICO to "¡Crítico!"
            logrado -> NivelResultado.EXITO to "Éxito"
            d10.any { it.esEspecial && it.valor == 1 } -> NivelResultado.FALLO_CRITICO to "Fallo bestial"
            exitos == 0 -> NivelResultado.FALLO to "Fallo total"
            else -> NivelResultado.FALLO to "Fallo"
        }
        return Veredicto(nivel, titulo, "$exitos éxito(s) de $dificultad", exitos, aciertos.map { it.dado.id }.toSet())
    }
}

/**
 * Year Zero Engine (Mutant, Alien, Vaesen, Forbidden Lands…): pool de d6, cada 6 es un éxito.
 * Dados ESPECIALES = Estrés (Alien): cualquier 1 en ellos dispara una tirada de Pánico.
 * Los 1 en dados normales importan si se "fuerza" la tirada.
 */
object YearZero : SistemaReglas {
    override val id = "yze"
    override val nombre = "Year Zero Engine"
    override val descripcion = "Pool de d6: cada 6 es un éxito; los 6 extra compran efectos. " +
        "Dados especiales = Estrés (Alien): un 1 en ellos provoca Pánico."

    override fun tiradaSugerida() = pool { agregar(TipoDado.D6, 5) }

    override fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto {
        val d6 = resultados.filter { it.tipo == TipoDado.D6 }
        val seises = d6.filter { it.valor == 6 }
        val panico = d6.any { it.esEspecial && it.valor == 1 }
        val unosBase = d6.count { !it.esEspecial && it.valor == 1 }
        val (nivel, titulo) = when {
            seises.size >= 2 -> NivelResultado.EXITO_CRITICO to "Éxito +${seises.size - 1} efecto(s)"
            seises.size == 1 -> NivelResultado.EXITO to "Éxito"
            else -> NivelResultado.FALLO to "Fallo"
        }
        val detalle = buildList {
            add("${seises.size} seis(es)")
            if (unosBase > 0) add("$unosBase uno(s) al forzar")
            if (panico) add("¡PÁNICO!")
        }.joinToString(" · ")
        return Veredicto(
            if (panico && nivel == NivelResultado.FALLO) NivelResultado.FALLO_CRITICO else nivel,
            titulo, detalle, seises.size, seises.map { it.dado.id }.toSet()
        )
    }
}
