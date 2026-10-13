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
import com.rolesencia.diceroller.fisica.motor.APOYO_MINIMO
import com.rolesencia.diceroller.fisica.motor.MotorFisicoPropio

/**
 * Una tirada física con **garantía de resultado**: pase lo que pase, termina con cada dado bien
 * apoyado sobre una cara y con un número legible. La usan los dos modos (en vivo y pre-simulado).
 *
 * Garantías:
 *  1. **Contención**: piso, 4 paredes y techo; si un dado atravesara algo, el motor lo devuelve al centro.
 *  2. **Reposo**: se espera a que TODOS los dados "duerman" (quietos un cuarto de segundo).
 *  3. **Lectura respecto del apoyo**: la cara de arriba se lee respecto de la superficie que
 *     sostiene al dado ([EstadoCuerpo.apoyo]), no de la vertical. Un dado apoyado plano sobre una
 *     rampa o sobre la cara de otro dado es válido: muestra la cara paralela a esa superficie.
 *  4. **Dados montados** (apoyados sobre una arista o un vértice, [LecturaCara.estaMontado]):
 *     se les da un **empujoncito en el lugar** (hasta
 *     [MAX_EMPUJONES] veces). Si aun así persisten, se los **acomoda donde están** sobre la cara
 *     más cercana ([MotorFisicaDados.asentarEnSitio]). Nunca aparecen en otro lado del tablero.
 *     Si unos pocos dados siguen despiertos sin cambios durante [ESPERA_TEMBLOR] s
 *     (p. ej. uno balanceándose sobre la arista de otro), se los trata igual.
 *  5. **Límite de tiempo**: a los [limiteSegundos] de simulación se apoya todo lo que siga moviéndose.
 *  6. **Sin superposiciones**: el motor separa a cada paso los dados que se interpenetran (SAT);
 *     antes de dar la tirada por terminada se verifica que ningún par quede superpuesto.
 *  7. **Torre de dados**: los dados se vierten de a uno (cada uno con su [ImpulsoLanzamiento.retardo]).
 *     Si un dado se queda quieto dentro de la torre o en la boca, se le da un **golpecito a la torre**
 *     enseguida (como hace un jugador), sin esperar a que el resto termine. Si aun así sigue trabado
 *     tras [MAX_GOLPECITOS] golpecitos, vale lo mismo que en la bandeja: si está plano sobre su
 *     rampa se lee así; si no, se lo acomoda en el lugar.
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
        /** Con más dados que esto moviéndose, la tirada todavía no se "trabó": se espera. */
        const val MAX_TEMBLANDO = 5
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

    private var menosDespiertos = Int.MAX_VALUE
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
        .associate { it.dado.id to leer(it) }

    /** Lee la cara de arriba respecto de lo que sostiene al dado (si es razonablemente horizontal). */
    private fun leer(estado: EstadoCuerpo): LecturaCara =
        LectorCara.leer(estado.dado.tipo, estado.orientacion, apoyoDe(estado))

    private fun apoyoDe(estado: EstadoCuerpo): Vec3 =
        if (estado.apoyo.y >= APOYO_MINIMO) estado.apoyo else Vec3.ARRIBA

    /** Mal apoyado: sobre una arista o un vértice en vez de una cara. */
    private fun malApoyado(estado: EstadoCuerpo): Boolean = leer(estado).estaMontado(ALINEACION_MINIMA)

    /** Empujoncito en el lugar; si ya se probó varias veces, se lo acomoda donde está. */
    private fun corregir(estado: EstadoCuerpo) {
        val intentos = empujonesPorDado.merge(estado.dado.id, 1, Int::plus)!!
        if (intentos <= MAX_EMPUJONES) {
            motor.empujar(estado.dado.id)
            empujones++
        } else {
            motor.asentarEnSitio(estado.dado.id, apoyada(estado))
            forzados++
        }
    }

    private fun verificarLecturas() {
        if (motor.solapamientoMaximo() > SOLAPAMIENTO_TOLERADO) {
            separados += motor.resolverSolapamientos()
            return
        }
        val propios = motor.estados().filter { it.dado.id in idsPropios }
        // Dentro de la torre primero se golpea la torre (lo que haría un jugador), y recién después
        // se aceptan los que quedaron planos sobre su rampa.
        if (escenario != null && golpecitos < MAX_GOLPECITOS && propios.any { escenario.dentroDeTorre(it.posicion) }) return
        val mal = propios.filter { malApoyado(it) }
        mal.forEach { corregir(it) }
        if (mal.isEmpty()) terminada = true
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

    /**
     * Si pasan [ESPERA_TEMBLOR] s sin que ningún dado nuevo se duerma (unos pocos se zarandean entre
     * sí en un montón), los que ya están planos sobre su apoyo se asientan ahí mismo (es un ajuste
     * imperceptible) y los montados reciben su empujoncito.
     */
    private fun vigilarTemblores() {
        val despiertos = motor.estados().filter { !it.enReposo && it.dado.id in idsPropios }
        if (despiertos.size < menosDespiertos) {
            menosDespiertos = despiertos.size
            desdeCambioDespiertos = 0f
            return
        }
        desdeCambioDespiertos += SUBPASO
        if (desdeCambioDespiertos < ESPERA_TEMBLOR || despiertos.size > MAX_TEMBLANDO) return
        desdeCambioDespiertos = 0f
        menosDespiertos = Int.MAX_VALUE
        for (estado in despiertos) {
            if (malApoyado(estado)) corregir(estado) else motor.asentarEnSitio(estado.dado.id, apoyada(estado))
        }
    }

    private fun apoyarTodo() {
        for (estado in motor.estados()) {
            if (estado.dado.id !in idsPropios) continue
            if (!estado.enReposo || malApoyado(estado)) {
                motor.asentarEnSitio(estado.dado.id, apoyada(estado))
                forzados++
            }
        }
        var vueltas = 0
        while (motor.solapamientoMaximo() > SOLAPAMIENTO_TOLERADO && vueltas++ < 5) separados += motor.resolverSolapamientos()
        terminada = true
    }

    /** Gira el dado lo mínimo para que la cara más cercana quede paralela a su apoyo. */
    private fun apoyada(estado: EstadoCuerpo): Quat {
        val tipo = estado.dado.tipo
        val arriba = apoyoDe(estado)
        val lectura = LectorCara.leer(tipo, estado.orientacion, arriba)
        val normalMundo = estado.orientacion.rotar(GeometriaDados.normales(tipo)[lectura.indice])
        val destino = if (GeometriaDados.seLeeHaciaAbajo(tipo)) -arriba else arriba
        return (Quat.entre(normalMundo, destino) * estado.orientacion).normalizado()
    }
}
