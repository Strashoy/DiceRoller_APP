package com.rolesencia.diceroller.domain.reglas

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.TipoDado

/**
 * Nivel semántico del resultado. La UI lo traduce a color/brillo
 * (heredero directo de `TipoResultado` del Lanzador Legendario).
 */
enum class NivelResultado {
    FALLO_CRITICO,
    FALLO,
    EXITO_PARCIAL,
    EXITO,
    EXITO_CRITICO,
    /** Tiradas sin juicio (suma libre, tirada enfrentada sin rival…). */
    NEUTRO
}

/** Qué dados se conservan cuando se tiran de más (ventaja/desventaja, Blades con 0 dados). */
enum class Conservar { TODOS, MAYOR, MENOR }

/**
 * Parámetros que el jugador ajusta en cada tirada. Cada sistema documenta
 * qué significa [objetivo] (CD, dificultad, habilidad, umbral de éxitos…).
 */
data class ParametrosTirada(
    val modificador: Int = 0,
    val objetivo: Int? = null,
    val conservar: Conservar = Conservar.TODOS
)

/** Cómo se interpreta la tirada. */
data class Veredicto(
    val nivel: NivelResultado,
    val titulo: String,
    val detalle: String = "",
    /** Suma, cantidad de éxitos o valor clave, según el sistema. */
    val total: Int? = null,
    /** ids de [DadoEnMesa] que "cuentan" (para resaltarlos en la mesa 3D). */
    val dadosDestacados: Set<Int> = emptySet()
)

/** Un parámetro que la UI debe mostrar para este sistema (con su rango). */
data class CampoParametro(
    val etiqueta: String,
    val rango: IntRange,
    val porDefecto: Int,
    /** Para los lugares angostos, como el botón "Tirada" ("Dif. 12" en vez de "Dificultad 12"). */
    val etiquetaCorta: String = etiqueta,
    /**
     * Se puede tirar sin fijarlo: el sistema muestra sólo el total. Es el caso de la CD de D&D, que
     * muchas veces conoce sólo el máster; juzgar contra un número que nadie dijo confunde.
     */
    val opcional: Boolean = false
) {
    /** Con qué valor arranca la mesa al elegir el sistema: nada, si es opcional. */
    val inicial: Int? get() = if (opcional) null else porDefecto
}

/**
 * El total, sólo si el título del veredicto no lo dice ya: "Éxito" con total 19 → 19; "54" o
 * "3 éxitos" → null. Lo usan el marcador (número grande aparte del veredicto) y el mensaje de Discord.
 */
fun totalFueraDelTitulo(titulo: String, total: Int?): Int? {
    total ?: return null
    val buscado = kotlin.math.abs(total).toString()
    return if (Regex("""\d+""").findAll(titulo).any { it.value == buscado }) null else total
}

interface SistemaReglas {
    val id: String
    val nombre: String
    /** Para la barra superior, donde el nombre completo no entra en teléfonos angostos. */
    val nombreCorto: String get() = nombre
    /** Explicación corta que se muestra en el diálogo de info. */
    val descripcion: String

    /** Pool con el que arranca la mesa al elegir este sistema. */
    fun tiradaSugerida(): List<DadoEnMesa>

    /** null = el sistema no usa ese parámetro y la UI lo oculta. */
    val campoObjetivo: CampoParametro? get() = null
    val usaModificador: Boolean get() = false

    fun evaluar(resultados: List<ResultadoDado>, parametros: ParametrosTirada): Veredicto

    /**
     * Dados que "explotan" en [ultimaRonda] y deben relanzarse y sumarse (10-again de Crónicas
     * de la Oscuridad, Regla del Seis de Shadowrun…). Los ids nuevos arrancan en [siguienteId].
     * La mesa los vuelve a tirar (también en 3D) hasta que no explote ninguno, y llama
     * a [evaluar] con todos los resultados acumulados.
     */
    fun dadosExplosivos(ultimaRonda: List<ResultadoDado>, siguienteId: Int): List<DadoEnMesa> = emptyList()
}

/** Ayuda para construir pools: `pool { agregar(TipoDado.D6, 3) }`. */
class ConstructorPool {
    private var siguienteId = 0
    internal val dados = mutableListOf<DadoEnMesa>()

    fun agregar(tipo: TipoDado, cantidad: Int = 1, rol: RolDado = RolDado.NORMAL) {
        repeat(cantidad) { dados += DadoEnMesa(siguienteId++, tipo, rol) }
    }
}

fun pool(bloque: ConstructorPool.() -> Unit): List<DadoEnMesa> = ConstructorPool().apply(bloque).dados.toList()
