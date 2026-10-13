package com.rolesencia.diceroller.fisica.motor

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.fisica.EscenarioFisico
import com.rolesencia.diceroller.fisica.EstadoCuerpo
import com.rolesencia.diceroller.fisica.EventoColision
import com.rolesencia.diceroller.fisica.ImpulsoLanzamiento
import com.rolesencia.diceroller.fisica.MotorFisicaDados
import com.rolesencia.diceroller.fisica.PiezaEscenario
import com.rolesencia.diceroller.fisica.Poliedro
import com.rolesencia.diceroller.fisica.Poliedros
import com.rolesencia.diceroller.fisica.Quat
import com.rolesencia.diceroller.fisica.Superficie
import com.rolesencia.diceroller.fisica.TipoEvento
import com.rolesencia.diceroller.fisica.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/** Parámetros físicos. Unidades: cm, s. Los valores se ajustaron a ojo para dados de resina sobre tapete. */
data class ConfigFisica(
    val gravedad: Float = 981f,
    val iteraciones: Int = 12,
    val restitucionMesa: Float = 0.40f,
    val friccionMesa: Float = 0.45f,
    val restitucionPared: Float = 0.45f,
    val friccionPared: Float = 0.25f,
    val restitucionDado: Float = 0.40f,
    val friccionDado: Float = 0.30f,
    /** Por debajo de esta velocidad de impacto no hay rebote (evita el "temblor" en reposo). */
    val umbralRebote: Float = 12f,
    val amortiguacionLineal: Float = 0.10f,
    val amortiguacionAngular: Float = 0.25f,
    /** Freno extra al girar apoyado en algo (resistencia a la rodadura del tapete o de otro dado). */
    val rodadura: Float = 0.9f,
    /** Freno extra (1/s) de un dado apoyado en otros: amortigua el balanceo en las pilas. */
    val rocePila: Float = 3f,
    val velocidadReposo: Float = 4f,
    val angularReposo: Float = 0.6f,
    val tiempoParaDormir: Float = 0.2f,
    val altoTecho: Float = 30f,
    /** Corrección de penetración (Baumgarte) y tolerancia. */
    val beta: Float = 0.15f,
    val holgura: Float = 0.01f,
    val maxCorreccion: Float = 15f
)

/** Un plano estático: `normal · p = d`. */
private class Plano(val normal: Vec3, val d: Float, val superficie: Superficie, val restitucion: Float, val friccion: Float, val indice: Int)

/** Pendiente máxima de un apoyo (componente vertical de su normal): 0.5 = 60°. */
const val APOYO_MINIMO = 0.5f

internal class Cuerpo(
    val dado: DadoEnMesa,
    val poli: Poliedro,
    var pos: Vec3,
    var rot: Quat,
    var vel: Vec3,
    var omega: Vec3,
    val estatico: Boolean,
    /** No nulo si es una pieza del escenario (rampa o pared de una torre). */
    val pieza: PiezaEscenario? = null
) {
    val invMasaBase = if (estatico) 0f else 1f
    // Los sólidos platónicos tienen tensor de inercia isotrópico: alcanza con un escalar.
    val invInerciaBase = if (estatico) 0f else 1f / (0.33f * poli.radio * poli.radio)
    var dormido = estatico
    var tiempoQuieto = 0f
    var tocaPiso = false
    /** Toca algo (piso, pared u otro dado): condición para poder dormirse. */
    var enContacto = false
    /** Apoyado o rozando otro dado (no el piso ni una pieza de la torre). */
    var tocaDado = false
    /** Velocidades promediadas: el reposo se decide con ellas, no con picos sueltos de temblor. */
    var promedioV = 0f
    var promedioW = 0f
    /** Normal (promediada por el peso que carga cada contacto) de lo que sostiene al dado. */
    var normalApoyo = Vec3.ARRIBA
    val verticesMundo = Array(poli.vertices.size) { Vec3.CERO }

    val invMasa get() = if (dormido) 0f else invMasaBase
    val invInercia get() = if (dormido) 0f else invInerciaBase

    fun actualizarVertices() {
        for (i in verticesMundo.indices) verticesMundo[i] = pos + rot.rotar(poli.vertices[i])
    }

    fun velocidadEn(r: Vec3): Vec3 = vel + (omega cross r)

    fun aplicarImpulso(p: Vec3, r: Vec3) {
        if (dormido) return
        vel = vel + p * invMasa
        omega = omega + (r cross p) * invInercia
    }
}

private class Contacto(
    val a: Cuerpo,
    val b: Cuerpo?,
    val punto: Vec3,
    val normal: Vec3,          // empuja a "a" hacia afuera de "b"/del plano
    val profundidad: Float,
    val restitucion: Float,
    val friccion: Float,
    val superficie: Superficie,
    /** Identifica al par (para sonido). */
    val clave: Long,
    /** Identifica al punto de contacto entre pasos (para el warm starting). */
    val id: Long
) {
    val rA = punto - a.pos
    val rB = if (b != null) punto - b.pos else Vec3.CERO
    var masaN = 0f
    var t1 = Vec3.CERO
    var t2 = Vec3.CERO
    var masaT1 = 0f
    var masaT2 = 0f
    var objetivo = 0f
    var vnInicial = 0f
    var vtInicial = 0f
    var accN = 0f
    var accT1 = 0f
    var accT2 = 0f

    fun velocidadRelativa(): Vec3 = a.velocidadEn(rA) - (b?.velocidadEn(rB) ?: Vec3.CERO)

    fun masaEfectiva(dir: Vec3): Float {
        val ca = rA cross dir
        var k = a.invMasa + a.invInercia * (ca dot ca)
        if (b != null) {
            val cb = rB cross dir
            k += b.invMasa + b.invInercia * (cb dot cb)
        }
        return if (k > 0f) 1f / k else 0f
    }

    fun aplicar(p: Vec3) {
        a.aplicarImpulso(p, rA)
        b?.aplicarImpulso(-p, rB)
    }
}

/**
 * Motor de cuerpos rígidos escrito a medida para dados (≈ 300 líneas, sin código nativo).
 *
 * Cada sub-paso:
 *  1. Gravedad sobre las velocidades.
 *  2. Detección de contactos: vértices del dado bajo el piso/paredes, y vértices de un dado dentro
 *     del otro (prueba contra los planos de sus caras). Respaldo esférico para choques arista-arista.
 *  3. Resolución por **impulsos secuenciales** (Erin Catto / Box2D) con "warm starting": impulso normal con rebote
 *     (restitución) + corrección de penetración, y fricción de Coulomb en 2 tangentes.
 *  4. Integración semi-implícita (velocidad → posición, ω → cuaternión) con amortiguación.
 *  5. "Dormir" los dados quietos; un dado dormido se despierta si otro lo golpea.
 *
 * Es determinista: con la misma semilla, el mismo lanzamiento da exactamente la misma trayectoria.
 */
class MotorFisicoPropio(
    private val config: ConfigFisica = ConfigFisica(),
    semilla: Long = 0L
) : MotorFisicaDados {

    private companion object {
        /** Fracción del impulso del paso anterior con la que arranca cada contacto. */
        const val CALENTAMIENTO = 0.85f
        /** Ids internos de las piezas del escenario (nunca coinciden con dados). */
        const val ID_PIEZAS = 5000
        /** Superposición tolerada entre dados (0,4 mm): por debajo es imperceptible. */
        const val HOLGURA_SOLAPAMIENTO = 0.04f
    }

    private val random = Random(semilla)
    private val cuerpos = ArrayList<Cuerpo>()
    private val planos = ArrayList<Plano>()
    private val contactos = ArrayList<Contacto>()
    private val ultimoImpacto = HashMap<Long, Float>()
    private val ultimoRoce = HashMap<Long, Float>()
    private val impulsosPrevios = HashMap<Long, Vec3>()
    private val paresEnContacto = ArrayList<Pair<Cuerpo, Cuerpo>>()
    private var escenario: EscenarioFisico? = null
    private var tiempo = 0f
    private var mitadAncho = 15f
    private var mitadProfundidad = 20f

    override var alColisionar: ((EventoColision) -> Unit)? = null

    override fun prepararMesa(ancho: Float, profundidad: Float) {
        mitadAncho = ancho / 2f
        mitadProfundidad = profundidad / 2f
        val c = config
        planos.clear()
        planos += Plano(Vec3(0f, 1f, 0f), 0f, Superficie.MESA, c.restitucionMesa, c.friccionMesa, 0)
        planos += Plano(Vec3(1f, 0f, 0f), -mitadAncho, Superficie.PARED, c.restitucionPared, c.friccionPared, 1)
        planos += Plano(Vec3(-1f, 0f, 0f), -mitadAncho, Superficie.PARED, c.restitucionPared, c.friccionPared, 2)
        planos += Plano(Vec3(0f, 0f, 1f), -mitadProfundidad, Superficie.PARED, c.restitucionPared, c.friccionPared, 3)
        planos += Plano(Vec3(0f, 0f, -1f), -mitadProfundidad, Superficie.PARED, c.restitucionPared, c.friccionPared, 4)
        planos += Plano(Vec3(0f, -1f, 0f), -c.altoTecho, Superficie.PARED, c.restitucionPared, c.friccionPared, 5)
    }

    override fun agregarDado(dado: DadoEnMesa, impulso: ImpulsoLanzamiento) {
        val c = Cuerpo(
            dado, Poliedros.de(dado.tipo), impulso.posicion, impulso.orientacion,
            impulso.velocidadLineal, impulso.velocidadAngular, estatico = false
        ).also { it.actualizarVertices() }
        // Nunca arrancar metido en otro dado: se lo sube hasta que quede libre.
        var intentos = 0
        while (intentos++ < 20 && cuerpos.any { penetracion(c, it) != null }) {
            c.pos = c.pos + Vec3(0f, 1f, 0f)
            c.actualizarVertices()
        }
        cuerpos += c
    }

    override fun agregarObstaculo(dado: DadoEnMesa, posicion: Vec3, orientacion: Quat) {
        cuerpos += Cuerpo(dado, Poliedros.de(dado.tipo), posicion, orientacion, Vec3.CERO, Vec3.CERO, estatico = true)
            .also { it.actualizarVertices() }
    }

    override fun estados(): List<EstadoCuerpo> =
        cuerpos.filter { it.pieza == null }.map { EstadoCuerpo(it.dado, it.pos, it.rot, it.dormido, it.normalApoyo) }

    override fun agregarEscenario(escenario: EscenarioFisico) {
        this.escenario = escenario
        planos.removeAll { it.indice == 5 }
        planos += Plano(Vec3(0f, -1f, 0f), -escenario.altoTecho, Superficie.PARED, config.restitucionPared, config.friccionPared, 5)
        escenario.piezas.forEachIndexed { k, pieza ->
            cuerpos += Cuerpo(
                DadoEnMesa(ID_PIEZAS + k, TipoDado.D6), Poliedros.caja(pieza.mitad), pieza.centro, pieza.rotacion,
                Vec3.CERO, Vec3.CERO, estatico = true, pieza = pieza
            ).also { it.actualizarVertices() }
        }
    }

    /** Material de un contacto: el de la pieza del escenario si participa una, si no el de dado contra dado. */
    private fun material(a: Cuerpo, b: Cuerpo): Triple<Float, Float, Superficie> =
        (a.pieza ?: b.pieza)?.let { Triple(it.restitucion, it.friccion, it.superficie) }
            ?: Triple(config.restitucionDado, config.friccionDado, Superficie.DADO)

    /** Si el dado quedó dentro de la torre, lo lleva a la bandeja (para rescates). */
    private fun sacarDeLaTorre(c: Cuerpo) {
        val e = escenario ?: return
        if (e.dentroDeTorre(c.pos)) c.pos = Vec3(e.puntoRescate.x + ruido(2f), c.pos.y, e.puntoRescate.z + ruido(1f))
    }

    override fun todosEnReposo(): Boolean = cuerpos.all { it.dormido }

    /** Para depurar: velocidad y contactos de cada dado despierto. */
    internal fun diagnostico(): List<String> = cuerpos.filter { !it.dormido }.map { c ->
        val n = contactos.count { it.a === c || it.b === c }
        "${c.dado.tipo.etiqueta}#${c.dado.id} v=${"%.2f".format(c.vel.longitud)} w=${"%.2f".format(c.omega.longitud)} " +
            "contactos=$n piso=${c.tocaPiso} quieto=${"%.2f".format(c.tiempoQuieto)}"
    }

    override fun empujar(idDado: Int) {
        val c = cuerpos.firstOrNull { it.dado.id == idDado && !it.estatico } ?: return
        c.dormido = false
        c.tiempoQuieto = 0f
        // Se lo empuja hacia arriba y lejos de los dados y paredes en que está recostado, como haría
        // un jugador. Se buscan por cercanía: un dado dormido no tiene contactos calculados.
        val vecinos = cuerpos.filter { o ->
            o !== c && o.pieza == null && (o.pos - c.pos).longitud < o.poli.radio + c.poli.radio + 0.1f
        }
        var lejos = vecinos.fold(Vec3.CERO) { acc, v -> acc + (c.pos - v.pos).normalizado() }
        for (pl in planos) {
            if (pl.normal.y != 0f) continue
            if ((pl.normal dot c.pos) - pl.d < c.poli.radio + 0.1f) lejos = lejos + pl.normal
        }
        lejos = Vec3(lejos.x, 0f, lejos.z)
        if (lejos.longitud < 0.1f) lejos = Vec3(ruido(1f), 0f, ruido(1f))
        c.vel = c.vel + lejos.normalizado() * 30f + Vec3(ruido(8f), 45f, ruido(8f))
        c.omega = c.omega + Vec3(ruido(10f), ruido(10f), ruido(10f))
        for (v in vecinos) if (!v.estatico) { v.dormido = false; v.tiempoQuieto = 0f }
    }

    override fun relanzar(idDado: Int, impulso: ImpulsoLanzamiento) {
        val c = cuerpos.firstOrNull { it.dado.id == idDado && !it.estatico } ?: return
        c.pos = impulso.posicion
        c.rot = impulso.orientacion
        c.vel = impulso.velocidadLineal
        c.omega = impulso.velocidadAngular
        c.dormido = false
        c.tiempoQuieto = 0f
        c.promedioV = 0f
        c.promedioW = 0f
        c.actualizarVertices()
    }

    override fun asentarEnSitio(idDado: Int, orientacion: Quat) {
        val c = cuerpos.firstOrNull { it.dado.id == idDado && !it.estatico } ?: return
        c.rot = orientacion
        c.actualizarVertices()
        // Al girarlo en el lugar puede meterse un poco en su apoyo o en un vecino: se lo saca por el
        // camino más corto (la normal de cada contacto), milímetros, nunca a otro lado del tablero.
        var vueltas = 0
        var movido = true
        while (movido && vueltas++ < 40) {
            movido = false
            for (pl in planos) {
                val hundido = c.verticesMundo.minOf { (pl.normal dot it) - pl.d }
                if (hundido < 0f) {
                    c.pos = c.pos + pl.normal * (-hundido + 0.005f)
                    c.actualizarVertices()
                    movido = true
                }
            }
            for (o in cuerpos) {
                if (o === c || (o.pos - c.pos).longitud > o.poli.radio + c.poli.radio) continue
                val pen = penetracion(c, o) ?: continue
                c.pos = c.pos + pen.normal * (pen.profundidad + 0.005f)
                c.actualizarVertices()
                movido = true
            }
        }
        c.vel = Vec3.CERO
        c.omega = Vec3.CERO
        c.dormido = true
    }

    override fun impulsar(idDado: Int, velocidad: Vec3) {
        val c = cuerpos.firstOrNull { it.dado.id == idDado && !it.estatico } ?: return
        c.dormido = false
        c.tiempoQuieto = 0f
        c.vel = c.vel + velocidad
        c.omega = c.omega + Vec3(ruido(6f), ruido(6f), ruido(6f))
    }

    override fun forzarReposo(idDado: Int, orientacion: Quat) {
        val c = cuerpos.firstOrNull { it.dado.id == idDado && !it.estatico } ?: return
        c.rot = orientacion
        sacarDeLaTorre(c)
        c.actualizarVertices()
        val minY = c.verticesMundo.minOf { it.y }
        c.pos = c.pos + Vec3(0f, -minY, 0f)
        ubicarSinSolapar(c)
        c.vel = Vec3.CERO
        c.omega = Vec3.CERO
        c.dormido = true
    }

    private fun ruido(escala: Float) = (random.nextFloat() - 0.5f) * 2f * escala

    override fun paso(dt: Float) {
        tiempo += dt
        val g = Vec3(0f, -config.gravedad * dt, 0f)
        for (c in cuerpos) if (!c.dormido) {
            c.vel = c.vel + g
            c.actualizarVertices()
        }
        generarContactos()
        resolver(dt)
        integrar(dt)
        proyectarSolapamientos()
        emitirEventos()
        revisarReposo(dt)
    }

    // ---------- 2. DETECCIÓN DE CONTACTOS ----------

    private fun generarContactos() {
        contactos.clear()
        paresEnContacto.clear()
        for (c in cuerpos) {
            if (c.dormido) continue
            c.tocaPiso = false
            c.enContacto = false
            c.tocaDado = false
            for (plano in planos) {
                for ((k, p) in c.verticesMundo.withIndex()) {
                    val s = (plano.normal dot p) - plano.d
                    if (s < 0f) {
                        if (plano.indice == 0) c.tocaPiso = true
                        c.enContacto = true
                        contactos += Contacto(
                            c, null, p, plano.normal, -s, plano.restitucion, plano.friccion,
                            plano.superficie, (c.dado.id.toLong() shl 20) or plano.indice.toLong(),
                            idContacto(c, k, plano.indice)
                        )
                    }
                }
            }
        }
        for (i in cuerpos.indices) for (j in i + 1 until cuerpos.size) {
            val a = cuerpos[i]
            val b = cuerpos[j]
            if (a.dormido && b.dormido) continue
            val d = b.pos - a.pos
            val alcance = a.poli.radio + b.poli.radio
            if ((d dot d) > alcance * alcance) continue
            // Prueba exacta (SAT): si hay un eje que los separa, no se tocan.
            val pen = penetracion(a, b) ?: continue
            despertarSiGolpea(a, b)
            val clave = (min(a.dado.id, b.dado.id).toLong() shl 20) or (max(a.dado.id, b.dado.id) + 100).toLong()
            val antes = contactos.size
            verticesDentro(a, b, clave)
            verticesDentro(b, a, clave)
            // Arista contra arista: ningún vértice quedó adentro, pero SAT detectó el cruce.
            if (contactos.size == antes) contactoArista(a, b, pen, clave)
            a.enContacto = true
            b.enContacto = true
            if (a.pieza == null && b.pieza == null) {
                a.tocaDado = true
                b.tocaDado = true
            }
            paresEnContacto += a to b
        }
    }

    /** Un dado dormido vuelve a la simulación si otro lo golpea con suficiente velocidad. */
    private fun despertarSiGolpea(a: Cuerpo, b: Cuerpo) {
        val (quieto, movil) = when {
            a.dormido && !b.dormido -> a to b
            b.dormido && !a.dormido -> b to a
            else -> return
        }
        if (quieto.estatico) return
        if (movil.vel.longitud + movil.omega.longitud * movil.poli.radio > 25f) {
            quieto.dormido = false
            quieto.tiempoQuieto = 0f
        }
    }

    /** Vértices de [a] que quedaron dentro de [b]: se empuja [a] por la cara de [b] menos penetrada. */
    private fun verticesDentro(a: Cuerpo, b: Cuerpo, clave: Long) {
        val inversa = b.rot.conjugado()
        val radioB = b.poli.radio
        for ((k, p) in a.verticesMundo.withIndex()) {
            val local = inversa.rotar(p - b.pos)
            if ((local dot local) > radioB * radioB) continue
            var mayor = -Float.MAX_VALUE
            var mejor = -1
            var dentro = true
            for ((i, cara) in b.poli.caras.withIndex()) {
                val s = (cara.normal dot local) - cara.distancia
                if (s > 0f) { dentro = false; break }
                if (s > mayor) { mayor = s; mejor = i }
            }
            if (!dentro) continue
            val normal = b.rot.rotar(b.poli.caras[mejor].normal)
            val (rest, fric, sup) = material(a, b)
            contactos += Contacto(
                a, b, p, normal, -mayor, rest, fric, sup, clave,
                idContacto(a, k, 16 + b.dado.id)
            )
        }
    }

    private fun penetracion(a: Cuerpo, b: Cuerpo): Penetracion? = EjeSeparador.penetracion(
        a.poli, a.pos, a.rot, a.verticesMundo, b.poli, b.pos, b.rot, b.verticesMundo
    )

    /** Contacto en el punto medio entre los vértices más hundidos de cada dado según la normal SAT. */
    private fun contactoArista(a: Cuerpo, b: Cuerpo, pen: Penetracion, clave: Long) {
        val punto = (EjeSeparador.soporte(a.verticesMundo, -pen.normal) + EjeSeparador.soporte(b.verticesMundo, pen.normal)) * 0.5f
        val (rest, fric, sup) = material(a, b)
        contactos += Contacto(
            a, b, punto, pen.normal, pen.profundidad, rest, fric, sup, clave,
            idContacto(a, 255, 16 + b.dado.id)
        )
    }

    // ---------- PROYECCIÓN: ningún par de dados queda superpuesto ----------

    /**
     * Después de integrar, se recalcula SAT en los pares que se tocaban y se separan las posiciones
     * directamente (sin agregar velocidad): así un choque fuerte nunca deja dados metidos uno en otro.
     */
    private fun proyectarSolapamientos() {
        // Después de moverse puede haber pares nuevos (un dado rápido que se metió en otro):
        // se revisan todos los pares cercanos, no sólo los que ya se tocaban.
        for (c in cuerpos) if (!c.dormido) c.actualizarVertices()
        val cercanos = ArrayList<Pair<Cuerpo, Cuerpo>>()
        for (i in cuerpos.indices) for (j in i + 1 until cuerpos.size) {
            val a = cuerpos[i]
            val b = cuerpos[j]
            if (a.dormido && b.dormido) continue
            val d = b.pos - a.pos
            val alcance = a.poli.radio + b.poli.radio
            if ((d dot d) <= alcance * alcance) cercanos += a to b
        }
        if (cercanos.isEmpty()) return
        repeat(4) {
            for ((a, b) in cercanos) {
                val pen = penetracion(a, b) ?: continue
                val exceso = pen.profundidad - HOLGURA_SOLAPAMIENTO
                if (exceso <= 0f) continue
                val wa = if (a.dormido || a.estatico) 0f else 1f
                val wb = if (b.dormido || b.estatico) 0f else 1f
                if (wa + wb == 0f) continue
                a.pos = a.pos + pen.normal * (exceso * wa / (wa + wb))
                b.pos = b.pos - pen.normal * (exceso * wb / (wa + wb))
                a.actualizarVertices()
                b.actualizarVertices()
            }
        }
    }

    /** Mueve [c] por la mesa hasta que no se superponga con ningún otro dado (lo usan los rescates; no lo saca de la torre). */
    private fun ubicarSinSolapar(c: Cuerpo) {
        repeat(40) {
            c.actualizarVertices()
            var movido = false
            for (o in cuerpos) {
                if (o === c) continue
                val pen = penetracion(c, o) ?: continue
                var dir = Vec3(pen.normal.x, 0f, pen.normal.z)
                if (dir.longitud < 0.2f) dir = Vec3(c.pos.x - o.pos.x + 0.01f, 0f, c.pos.z - o.pos.z)
                c.pos = c.pos + dir.normalizado() * (pen.profundidad + 0.05f)
                val margen = c.poli.radio
                c.pos = Vec3(
                    c.pos.x.coerceIn(-mitadAncho + margen, mitadAncho - margen),
                    c.pos.y,
                    c.pos.z.coerceIn(-mitadProfundidad + margen, mitadProfundidad - margen)
                )
                movido = true
            }
            if (!movido) return
        }
        // Mesa demasiado llena: se lo apoya encima de los demás.
        c.pos = Vec3(c.pos.x, c.pos.y + 2f * c.poli.radio, c.pos.z)
        c.actualizarVertices()
    }

    /** Mayor profundidad de superposición entre dos dados cualesquiera (cm). */
    override fun solapamientoMaximo(): Float {
        var max = 0f
        for (i in cuerpos.indices) for (j in i + 1 until cuerpos.size) {
            val a = cuerpos[i]
            val b = cuerpos[j]
            if (a.estatico && b.estatico) continue          // piezas de la torre: se cruzan a propósito
            val d = b.pos - a.pos
            val alcance = a.poli.radio + b.poli.radio
            if ((d dot d) > alcance * alcance) continue
            penetracion(a, b)?.let { if (it.profundidad > max) max = it.profundidad }
        }
        return max
    }

    /** Separa todo dado superpuesto con otro; devuelve cuántos movió. */
    override fun resolverSolapamientos(): Int {
        var movidos = 0
        for (c in cuerpos) {
            if (c.estatico) continue
            val solapado = cuerpos.any { o -> o !== c && (penetracion(c, o)?.profundidad ?: 0f) > HOLGURA_SOLAPAMIENTO }
            if (solapado) {
                ubicarSinSolapar(c)
                c.dormido = false
                c.tiempoQuieto = 0f
                movidos++
            }
        }
        return movidos
    }


    // ---------- 3. IMPULSOS SECUENCIALES ----------

    private fun perpendicular(n: Vec3): Vec3 =
        (if (abs(n.x) < 0.9f) n cross Vec3(1f, 0f, 0f) else n cross Vec3(0f, 1f, 0f)).normalizado()

    private fun resolver(dt: Float) {
        for (c in contactos) {
            val vrel = c.velocidadRelativa()
            val vn = vrel dot c.normal
            val vt = vrel - c.normal * vn
            c.vnInicial = vn
            c.vtInicial = vt.longitud
            c.t1 = if (c.vtInicial > 1e-4f) vt * (1f / c.vtInicial) else perpendicular(c.normal)
            c.t2 = c.normal cross c.t1
            c.masaN = c.masaEfectiva(c.normal)
            c.masaT1 = c.masaEfectiva(c.t1)
            c.masaT2 = c.masaEfectiva(c.t2)
            val rebote = if (-vn > config.umbralRebote) -c.restitucion * vn else 0f
            val correccion = min(config.beta / dt * max(c.profundidad - config.holgura, 0f), config.maxCorreccion)
            c.objetivo = max(rebote, correccion)
            // Warm starting: se arranca con el impulso que este mismo contacto necesitó el paso anterior.
            impulsosPrevios[c.id]?.let { p ->
                c.accN = max(p dot c.normal, 0f) * CALENTAMIENTO
                val limite = c.friccion * c.accN
                c.accT1 = ((p dot c.t1) * CALENTAMIENTO).coerceIn(-limite, limite)
                c.accT2 = ((p dot c.t2) * CALENTAMIENTO).coerceIn(-limite, limite)
                c.aplicar(c.normal * c.accN + c.t1 * c.accT1 + c.t2 * c.accT2)
            }
        }
        repeat(config.iteraciones) {
            for (c in contactos) {
                // Normal: nunca "tira" (impulso acumulado >= 0).
                val vn = c.velocidadRelativa() dot c.normal
                val nuevoN = max(c.accN + c.masaN * (c.objetivo - vn), 0f)
                c.aplicar(c.normal * (nuevoN - c.accN))
                c.accN = nuevoN
                // Fricción de Coulomb: |impulso tangencial| <= mu * impulso normal.
                val limite = c.friccion * c.accN
                val nuevoT1 = (c.accT1 - (c.velocidadRelativa() dot c.t1) * c.masaT1).coerceIn(-limite, limite)
                c.aplicar(c.t1 * (nuevoT1 - c.accT1))
                c.accT1 = nuevoT1
                val nuevoT2 = (c.accT2 - (c.velocidadRelativa() dot c.t2) * c.masaT2).coerceIn(-limite, limite)
                c.aplicar(c.t2 * (nuevoT2 - c.accT2))
                c.accT2 = nuevoT2
            }
        }
        impulsosPrevios.clear()
        for (c in contactos) impulsosPrevios[c.id] = c.normal * c.accN + c.t1 * c.accT1 + c.t2 * c.accT2
        actualizarApoyos()
    }

    /**
     * Para cada dado despierto, la superficie sobre la que está apoyado: el piso, una rampa o la cara
     * de otro dado. Se agrupan los contactos por "con quién" (cada grupo da una normal pesada por la
     * carga) y, entre los que lo sostienen desde abajo, gana aquel contra el que el dado tiene una
     * cara más pareja. Así la presión lateral de los vecinos no inclina la lectura.
     */
    private fun actualizarApoyos() {
        val grupos = HashMap<Cuerpo, HashMap<Any, Vec3>>()
        for (c in contactos) {
            if (c.accN <= 0f) continue
            grupos.getOrPut(c.a) { HashMap() }.merge(c.b ?: c.normal, c.normal * c.accN, Vec3::plus)
            c.b?.let { b -> grupos.getOrPut(b) { HashMap() }.merge(c.a, -c.normal * c.accN, Vec3::plus) }
        }
        for ((cuerpo, porOtro) in grupos) {
            if (cuerpo.dormido || cuerpo.estatico) continue
            var mejor: Vec3? = null
            var mejorAjuste = -2f
            for (suma in porOtro.values) {
                if (suma.longitud < 1e-4f) continue
                val n = suma.normalizado()
                if (n.y < APOYO_MINIMO) continue               // pared o cara empinada: no lo sostiene
                val ajuste = cuerpo.poli.caras.maxOf { cara -> -(cuerpo.rot.rotar(cara.normal) dot n) }
                if (ajuste > mejorAjuste) { mejorAjuste = ajuste; mejor = n }
            }
            mejor?.let { cuerpo.normalApoyo = it }
        }
    }

    private fun idContacto(a: Cuerpo, vertice: Int, otro: Int): Long =
        (a.dado.id.toLong() shl 32) or (vertice.toLong() shl 16) or otro.toLong()

    // ---------- 4. INTEGRACIÓN ----------

    private fun integrar(dt: Float) {
        for (c in cuerpos) {
            if (c.dormido) continue
            c.pos = c.pos + c.vel * dt
            c.rot = c.rot.integrar(c.omega, dt)
            // Apilado sobre otros dados: el roce entre dados frena el balanceo, como en la realidad.
            val pila = if (c.tocaDado) config.rocePila else 0f
            c.vel = c.vel * (1f / (1f + dt * (config.amortiguacionLineal + pila * 0.5f)))
            val freno = config.amortiguacionAngular + (if (c.enContacto) config.rodadura else 0f) + pila
            c.omega = c.omega * (1f / (1f + dt * freno))
            // Red de seguridad: un dado que atravesó algo vuelve al centro de la mesa.
            if (c.pos.y < -5f || abs(c.pos.x) > mitadAncho + 5f || abs(c.pos.z) > mitadProfundidad + 5f) {
                c.pos = escenario?.puntoRescate?.let { Vec3(it.x, 8f, it.z) } ?: Vec3(0f, 8f, 0f)
                c.vel = Vec3.CERO
                ubicarSinSolapar(c)
            }
        }
    }

    // ---------- EVENTOS PARA SONIDO Y HÁPTICA ----------

    private fun emitirEventos() {
        val oyente = alColisionar ?: return
        val porClave = HashMap<Long, Contacto>()
        for (c in contactos) {
            val previo = porClave[c.clave]
            if (previo == null || c.vnInicial < previo.vnInicial) porClave[c.clave] = c
        }
        for ((clave, c) in porClave) {
            val impacto = -c.vnInicial
            // Con una pieza del escenario, el "dado" del evento es siempre el dado real.
            val dado = if (c.a.pieza != null) c.b!!.dado else c.a.dado
            val otro = c.b?.takeIf { it.pieza == null && c.a.pieza == null }?.dado
            if (impacto > 4f && tiempo - (ultimoImpacto[clave] ?: -1f) > 0.06f) {
                ultimoImpacto[clave] = tiempo
                oyente(EventoColision(dado, c.superficie, impacto, TipoEvento.IMPACTO, c.punto, otro))
            } else if (c.superficie == Superficie.DADO && c.vtInicial > 8f && tiempo - (ultimoRoce[clave] ?: -1f) > 0.12f) {
                ultimoRoce[clave] = tiempo
                oyente(EventoColision(dado, c.superficie, c.vtInicial, TipoEvento.ROCE, c.punto, otro))
            }
        }
    }

    // ---------- 5. REPOSO ----------

    private fun revisarReposo(dt: Float) {
        for (c in cuerpos) {
            if (c.dormido) continue
            c.promedioV += (c.vel.longitud - c.promedioV) * 0.08f
            c.promedioW += (c.omega.longitud - c.promedioW) * 0.08f
            val quieto = c.promedioV < config.velocidadReposo && c.promedioW < config.angularReposo
            if (quieto && c.enContacto) {
                c.tiempoQuieto += dt
                if (c.tiempoQuieto >= config.tiempoParaDormir) {
                    c.dormido = true
                    c.vel = Vec3.CERO
                    c.omega = Vec3.CERO
                    c.actualizarVertices()
                    alColisionar?.invoke(EventoColision(c.dado, Superficie.MESA, 10f, TipoEvento.ASENTAMIENTO, c.pos))
                }
            } else {
                c.tiempoQuieto = 0f
            }
        }
    }
}
