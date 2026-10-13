package com.rolesencia.diceroller.tirada

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.fisica.EscenarioFisico
import com.rolesencia.diceroller.fisica.EstadoCuerpo
import com.rolesencia.diceroller.fisica.EventoColision
import com.rolesencia.diceroller.fisica.GeometriaDados
import com.rolesencia.diceroller.fisica.ImpulsoLanzamiento
import com.rolesencia.diceroller.fisica.LecturaCara
import com.rolesencia.diceroller.fisica.LectorCara
import com.rolesencia.diceroller.fisica.MotorFisicaDados
import com.rolesencia.diceroller.fisica.Quat
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.fisica.motor.MotorFisicoPropio
import kotlin.random.Random

/**
 * Una tirada física con **garantía de resultado**: pase lo que pase, termina con cada dado bien
 * apoyado sobre una cara y con un número legible. La usan los dos modos (en vivo y pre-simulado).
 *
 * Garantías:
 *  1. **Contención**: piso, 4 paredes y techo; si un dado atravesara algo, el motor lo devuelve al centro.
 *  2. **Reposo**: se espera a que TODOS los dados "duerman" (quietos un cuarto de segundo).
 *  3. **Dados montados**: si un dado quedó inclinado sobre otro o contra la pared
 *     ([LecturaCara.estaMontado]) se hace lo que manda la mesa: se lo levanta y se lo **vuelve a
 *     tirar en el lugar más libre de la bandeja** (hasta [MAX_EMPUJONES] veces). No cae otra vez
 *     sobre la misma pila. Si aun así persiste, se lo apoya sobre la cara más cercana.
 *  4. **Dados que tiemblan**: si unos pocos dados siguen despiertos sin cambios durante
 *     [ESPERA_TEMBLOR] s (p. ej. uno balanceándose sobre la arista de otro), se los empuja.
 *  5. **Límite de tiempo**: a los [limiteSegundos] de simulación se apoya todo lo que siga moviéndose.
 *  6. **Sin superposiciones**: el motor separa a cada paso los dados que se interpenetran (SAT);
 *     antes de dar la tirada por terminada se verifica que ningún par quede superpuesto.
 *  7. **Torre de dados**: los dados se vierten de a uno (cada uno con su [ImpulsoLanzamiento.retardo]).
 *     Si un dado se queda quieto dentro de la torre o en la boca, se le da un **golpecito a la torre**
 *     enseguida (como hace un jugador), sin esperar a que el resto termine. Si aun así sigue trabado
 *     al final, se empuja como uno montado y, en último caso, se lo saca a la bandeja.
 *
 * Las garantías 3 y 4 se cuentan en las métricas: en una mesa normal deberían ser casi siempre 0.
 */
class SimulacionTirada(
    val dados: List<DadoEnMesa>,
    impulsos: List<ImpulsoLanzamiento>,
    obstaculos: List<DadoVisual>,
    mesa: ConfigMesa,
    semilla: Long,
    private val limiteSegundos: Float = 8f,
    private val motor: MotorFisicaDados = MotorFisicoPropio(semilla = semilla),
    /** Piezas fijas extra (torre de dados); null = bandeja sola. */
    private val escenario: EscenarioFisico? = null
) {
    companion object {
        /** 240 sub-pasos por segundo: suficiente para que un dado rápido no atraviese a otro. */
        const val SUBPASO = 1f / 240f
        const val MAX_EMPUJONES = 3
        const val ALINEACION_MINIMA = 0.95f
        const val ESPERA_TEMBLOR = 1.5f
        /** Cuánto espera un dado quieto dentro de la torre antes del golpecito. */
        const val ESPERA_GOLPECITO = 0.3f
        const val MAX_GOLPECITOS = 15
        /** Superposición final tolerada (cm): medio milímetro, invisible en pantalla. */
        const val SOLAPAMIENTO_TOLERADO = 0.05f
    }

    private val idsPropios = dados.map { it.id }.toSet()
    private val buffer = ArrayList<EventoColision>()
    private val empujonesPorDado = HashMap<Int, Int>()

    var tiempo = 0f
        private set
    var pasos = 0
        private set
    var empujones = 0
        private set
    var forzados = 0
        private set
    /** Dados que hubo que reubicar porque quedaban superpuestos con otro. */
    var separados = 0
        private set
    /** Golpecitos dados a la torre para destrabar dados (sólo con torre). */
    var golpecitos = 0
        private set
    var terminada = false
        private set

    private var despiertosAnteriores: Set<Int> = emptySet()
    private var desdeCambioDespiertos = 0f

    /** Dados que todavía no entraron (se vierten de a uno). */
    private val pendientes = ArrayDeque(dados.zip(impulsos).sortedBy { it.second.retardo })
    private var ultimaEntrada = 0f
    private val quietoEnTorreDesde = HashMap<Int, Float>()

    init {
        motor.prepararMesa(mesa.ancho, mesa.profundidad)
        escenario?.let { motor.agregarEscenario(it) }
        obstaculos.forEach { motor.agregarObstaculo(it.dado, it.posicion, it.orientacion) }
        motor.alColisionar = { buffer += it }
        hacerEntrar()
    }

    private fun hacerEntrar() {
        while (pendientes.isNotEmpty() && pendientes.first().second.retardo <= tiempo) {
            val (dado, impulso) = pendientes.removeFirst()
            motor.agregarDado(dado, impulso)
            ultimaEntrada = tiempo
        }
    }

    /** Avanza un sub-paso fijo y devuelve los eventos sonoros que produjo. */
    fun paso(): List<EventoColision> {
        if (terminada) return emptyList()
        hacerEntrar()
        motor.paso(SUBPASO)
        tiempo += SUBPASO
        pasos++
        if (escenario != null && pasos % 12 == 0) vigilarTorre()
        when {
            pendientes.isNotEmpty() -> Unit                          // todavía se están vertiendo
            motor.todosEnReposo() -> verificarLecturas()
            tiempo - ultimaEntrada > limiteSegundos -> apoyarTodo()
            else -> vigilarTemblores()
        }
        if (buffer.isEmpty()) return emptyList()
        val salida = ArrayList(buffer)
        buffer.clear()
        return salida
    }

    fun estados(): List<EstadoCuerpo> = motor.estados()

    /** Sólo de los dados de esta tirada (no de los obstáculos de rondas anteriores). */
    fun lecturas(): Map<Int, LecturaCara> = motor.estados()
        .filter { it.dado.id in idsPropios }
        .associate { it.dado.id to LectorCara.leer(it.dado.tipo, it.orientacion) }

    private fun verificarLecturas() {
        if (motor.solapamientoMaximo() > SOLAPAMIENTO_TOLERADO) {
            separados += motor.resolverSolapamientos()
            return
        }
        var hayMontados = false
        for (estado in motor.estados()) {
            if (estado.dado.id !in idsPropios) continue
            // Un dado quieto dentro de la torre (trabado en una rampa) cuenta como mal apoyado.
            val trabado = escenario?.dentroDeTorre(estado.posicion) == true
            if (!trabado && !LectorCara.leer(estado.dado.tipo, estado.orientacion).estaMontado(ALINEACION_MINIMA)) continue
            hayMontados = true
            val intentos = empujonesPorDado.merge(estado.dado.id, 1, Int::plus)!!
            if (intentos <= MAX_EMPUJONES) {
                relanzarEnLugarLibre(estado.dado.id)
                empujones++
            } else {
                motor.forzarReposo(estado.dado.id, apoyada(estado))
                forzados++
            }
        }
        if (!hayMontados) terminada = true
    }

    /** Bandeja libre donde se relanzan los dados montados (la de la torre, o la mesa con margen). */
    private val zonaLibre: FloatArray = escenario?.zonaBandeja
        ?: floatArrayOf(-mesa.ancho / 2f + 2f, -mesa.profundidad / 2f + 2f, mesa.ancho / 2f - 2f, mesa.profundidad / 2f - 2f)
    private val azar = Random(semilla xor 0x5EEDL)

    /**
     * Relanza un dado montado como en la mesa real: se lo levanta y se lo deja caer, girando, sobre
     * el punto de la bandeja más alejado de los demás dados. Así no vuelve a caer sobre la pila.
     */
    private fun relanzarEnLugarLibre(id: Int) {
        val otros = motor.estados().filter { it.dado.id != id }.map { it.posicion }
        var mejor = Vec3((zonaLibre[0] + zonaLibre[2]) / 2f, 0f, (zonaLibre[1] + zonaLibre[3]) / 2f)
        var mejorDistancia = -1f
        var x = zonaLibre[0]
        while (x <= zonaLibre[2]) {
            var z = zonaLibre[1]
            while (z <= zonaLibre[3]) {
                val distancia = otros.minOfOrNull { (it.x - x) * (it.x - x) + (it.z - z) * (it.z - z) } ?: Float.MAX_VALUE
                val conAzar = distancia + azar.nextFloat() * 0.5f
                if (conAzar > mejorDistancia) {
                    mejorDistancia = conAzar
                    mejor = Vec3(x, 0f, z)
                }
                z += 1.5f
            }
            x += 1.5f
        }
        fun ruido(e: Float) = (azar.nextFloat() - 0.5f) * 2f * e
        motor.relanzar(
            id,
            ImpulsoLanzamiento(
                posicion = Vec3(mejor.x, 5f, mejor.z),
                orientacion = GeneradorLanzamiento.orientacionAleatoria(azar),
                velocidadLineal = Vec3(ruido(12f), -20f, ruido(12f)),
                velocidadAngular = Vec3(ruido(25f), ruido(25f), ruido(25f))
            )
        )
    }

    /**
     * Golpecito a la torre: si algún dado lleva un rato quieto dentro de la torre o en la boca
     * (la cola de dados que no deja salir a los de atrás), se golpea la torre: los dados quietos de
     * esa zona reciben un pequeño impulso hacia la salida. Es lo que hace un jugador en la mesa.
     */
    private fun vigilarTorre() {
        val e = escenario ?: return
        val estados = motor.estados().filter { it.dado.id in idsPropios }
        val trabados = estados.filter { it.enReposo && e.dentroDeTorre(it.posicion) }
        quietoEnTorreDesde.keys.retainAll(trabados.map { it.dado.id }.toSet())
        trabados.forEach { quietoEnTorreDesde.putIfAbsent(it.dado.id, tiempo) }
        if (golpecitos >= MAX_GOLPECITOS || quietoEnTorreDesde.values.none { tiempo - it >= ESPERA_GOLPECITO }) return
        golpecitos++
        quietoEnTorreDesde.clear()
        val salida = e.direccionSalida.normalizado()
        for (estado in estados) {
            if (!estado.enReposo) continue
            val adentro = e.dentroDeTorre(estado.posicion)
            if (!adentro && !e.enLaBoca(estado.posicion)) continue
            // Los de adentro reciben el golpe completo; los de la boca, un empujón de los que salen.
            val fuerza = if (adentro) 38f else 26f
            motor.impulsar(estado.dado.id, salida * fuerza + Vec3(0f, 10f, 0f))
        }
    }

    private fun vigilarTemblores() {
        val despiertos = motor.estados().filter { !it.enReposo && it.dado.id in idsPropios }.map { it.dado.id }.toSet()
        if (despiertos != despiertosAnteriores) {
            despiertosAnteriores = despiertos
            desdeCambioDespiertos = 0f
            return
        }
        desdeCambioDespiertos += SUBPASO
        if (desdeCambioDespiertos < ESPERA_TEMBLOR || despiertos.size > 3) return
        desdeCambioDespiertos = 0f
        for (id in despiertos) {
            val intentos = empujonesPorDado.merge(id, 1, Int::plus)!!
            if (intentos <= MAX_EMPUJONES) {
                relanzarEnLugarLibre(id)
                empujones++
            } else {
                val estado = motor.estados().first { it.dado.id == id }
                motor.forzarReposo(id, apoyada(estado))
                forzados++
            }
        }
    }

    private fun apoyarTodo() {
        for (estado in motor.estados()) {
            if (estado.dado.id !in idsPropios) continue
            val lectura = LectorCara.leer(estado.dado.tipo, estado.orientacion)
            if (!estado.enReposo || lectura.estaMontado(ALINEACION_MINIMA) || escenario?.dentroDeTorre(estado.posicion) == true) {
                motor.forzarReposo(estado.dado.id, apoyada(estado))
                forzados++
            }
        }
        if (motor.solapamientoMaximo() > SOLAPAMIENTO_TOLERADO) separados += motor.resolverSolapamientos()
        terminada = true
    }

    /** Gira el dado lo mínimo para que la cara más cercana quede perfectamente horizontal. */
    private fun apoyada(estado: EstadoCuerpo): Quat {
        val tipo = estado.dado.tipo
        val lectura = LectorCara.leer(tipo, estado.orientacion)
        val normalMundo = estado.orientacion.rotar(GeometriaDados.normales(tipo)[lectura.indice])
        val destino = if (GeometriaDados.seLeeHaciaAbajo(tipo)) Vec3.ABAJO else Vec3.ARRIBA
        return (Quat.entre(normalMundo, destino) * estado.orientacion).normalizado()
    }
}
