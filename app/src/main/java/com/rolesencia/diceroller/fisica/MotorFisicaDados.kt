package com.rolesencia.diceroller.fisica

import com.rolesencia.diceroller.domain.dados.DadoEnMesa

/** Contra qué chocó un dado. Se traduce a las familias de sonido grabadas (lona, madera, dado). */
enum class Superficie { MESA, PARED, DADO }

enum class TipoEvento {
    /** Golpe: velocidad normal de acercamiento. */
    IMPACTO,
    /** Dos dados que se rozan/deslizan uno contra otro. */
    ROCE,
    /** El dado acaba de quedar quieto. */
    ASENTAMIENTO
}

data class EventoColision(
    val dado: DadoEnMesa,
    val superficie: Superficie,
    /** cm/s: velocidad normal del impacto (o tangencial, en un ROCE). */
    val velocidad: Float,
    val tipo: TipoEvento = TipoEvento.IMPACTO,
    /** Punto del contacto (para el paneo estéreo). */
    val posicion: Vec3 = Vec3.CERO,
    /** El otro dado, si chocaron dos dados. */
    val otro: DadoEnMesa? = null
)

/**
 * Pieza fija del escenario (rampa, pared, borde de bandeja): una caja orientada.
 * [superficie] decide el sonido al chocarla (MESA = tapete/fieltro, PARED = madera/piedra).
 */
data class PiezaEscenario(
    val centro: Vec3,
    val mitad: Vec3,
    val rotacion: Quat = Quat.IDENTIDAD,
    val superficie: Superficie = Superficie.PARED,
    val restitucion: Float = 0.35f,
    val friccion: Float = 0.25f
)

/**
 * Escenario extra sobre la bandeja básica (por ejemplo una torre de dados).
 * @param zonaTorre rectángulo (x0, z0, x1, z1) que ocupa la torre: un dado nunca se rescata ahí.
 * @param puntoRescate dónde se apoya un dado que hubo que sacar de un atasco.
 */
data class EscenarioFisico(
    val piezas: List<PiezaEscenario>,
    val altoTecho: Float,
    val zonaTorre: FloatArray,
    val puntoRescate: Vec3,
    /** Rectángulo (x0, z0, x1, z1) justo frente a la boca de salida. */
    val zonaBoca: FloatArray = floatArrayOf(0f, 0f, 0f, 0f),
    /** Hacia dónde sale un dado de la torre (para el "golpecito"). */
    val direccionSalida: Vec3 = Vec3(0f, 0f, 1f),
    /** Rectángulo (x0, z0, x1, z1) de bandeja libre donde se pueden relanzar dados montados. */
    val zonaBandeja: FloatArray? = null
) {
    fun enLaBoca(p: Vec3): Boolean =
        p.x > zonaBoca[0] && p.x < zonaBoca[2] && p.z > zonaBoca[1] && p.z < zonaBoca[3]

    fun dentroDeTorre(p: Vec3): Boolean =
        p.x > zonaTorre[0] && p.x < zonaTorre[2] && p.z > zonaTorre[1] && p.z < zonaTorre[3]
}

/** Snapshot de un cuerpo para dibujarlo. */
data class EstadoCuerpo(
    val dado: DadoEnMesa,
    val posicion: Vec3,
    val orientacion: Quat,
    val enReposo: Boolean
)

/** Condiciones iniciales de un dado al soltarlo (cm, cm/s, rad/s). */
data class ImpulsoLanzamiento(
    val posicion: Vec3,
    val orientacion: Quat,
    val velocidadLineal: Vec3,
    val velocidadAngular: Vec3,
    /** Segundos desde el inicio de la tirada en que el dado entra en juego (verter de a poco). */
    val retardo: Float = 0f
)

/**
 * Contrato del motor de física de cuerpos rígidos. Hoy lo implementa
 * [com.rolesencia.diceroller.fisica.motor.MotorFisicoPropio] (Kotlin puro); Jolt o Bullet podrían
 * enchufarse detrás de la misma interfaz.
 *
 * Unidades: centímetros, segundos, radianes. Y hacia arriba; el piso es el plano y = 0.
 */
interface MotorFisicaDados {
    /** Crea piso, 4 paredes y techo con las medidas internas de la bandeja. */
    fun prepararMesa(ancho: Float, profundidad: Float)

    fun agregarDado(dado: DadoEnMesa, impulso: ImpulsoLanzamiento)

    /** Dado ya leído que queda fijo: los nuevos chocan contra él pero no lo mueven. */
    fun agregarObstaculo(dado: DadoEnMesa, posicion: Vec3, orientacion: Quat)

    /** Avanza un sub-paso fijo de [dt] segundos. */
    fun paso(dt: Float)

    fun estados(): List<EstadoCuerpo>

    fun todosEnReposo(): Boolean

    /** Se invoca durante [paso] por cada contacto relevante (sonido + háptica). */
    var alColisionar: ((EventoColision) -> Unit)?

    /** Pequeño impulso para "desmontar" un dado apoyado en otro o contra la pared. */
    fun empujar(idDado: Int)

    /** Último recurso: apoya el dado con la orientación dada y lo duerme. */
    fun forzarReposo(idDado: Int, orientacion: Quat)

    /** Agrega piezas fijas (torre de dados). Se llama después de [prepararMesa]. */
    fun agregarEscenario(escenario: EscenarioFisico) {}

    /** Mayor superposición entre dos dados, en cm (0 = ninguno se mete dentro de otro). */
    fun solapamientoMaximo(): Float = 0f

    /** Separa los dados superpuestos; devuelve cuántos movió. */
    fun resolverSolapamientos(): Int = 0

    /** Despierta el dado y le suma esa velocidad (cm/s): el "golpecito" a la torre. */
    fun impulsar(idDado: Int, velocidad: Vec3) {}

    /** Levanta el dado y lo vuelve a tirar con estas condiciones (relanzar un dado montado). */
    fun relanzar(idDado: Int, impulso: ImpulsoLanzamiento) {}
}
