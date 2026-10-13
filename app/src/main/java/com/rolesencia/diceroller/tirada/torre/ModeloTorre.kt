package com.rolesencia.diceroller.tirada.torre

import com.rolesencia.diceroller.fisica.EscenarioFisico
import com.rolesencia.diceroller.fisica.PiezaEscenario
import com.rolesencia.diceroller.fisica.Quat
import com.rolesencia.diceroller.fisica.Superficie
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.sensorial.MaterialMesa
import kotlin.math.abs

/** Cómo se dibuja una pieza: el patrón que se le pinta encima a cada cara. */
enum class Acabado {
    LISO,
    /** Ladrillos en hilera (madera grabada, como la torre de madera). */
    LADRILLO,
    /** Piedras irregulares con variación de tono (castillo). */
    PIEDRA,
    /** Cuero con costura de color en el borde de cada cara. */
    COSTURA,
    /** Pared frontal en corte: casi transparente para ver los dados caer adentro. */
    VIDRIO,
    /** Sólo choca: no se dibuja (la forma visible es otra pieza). */
    INVISIBLE
}

/** Pieza de una torre con su aspecto. Si [colisiona] es false es sólo decorado (almenas, forros). */
data class PiezaVisible(
    val pieza: PiezaEscenario,
    val color: Long,
    val acabado: Acabado,
    val colisiona: Boolean = true,
    /** Color de la costura o de las juntas. */
    val colorDetalle: Long = 0xFF000000L,
    /** Si no es nulo, las caras que miran hacia [puntoInterior] se pintan de este color (forro). */
    val colorInterior: Long? = null,
    val puntoInterior: Vec3? = null
)

/**
 * Una torre de dados lista para simular y dibujar.
 * Los dados se sueltan en [zonaCaida] (x0, z0, x1, z1) a partir de la altura [alturaCaida],
 * bajan por las rampas y salen por la abertura frontal a la bandeja.
 */
class ModeloTorre(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val piezas: List<PiezaVisible>,
    val colorPiso: Long,
    /** Material del piso de la bandeja para el sonido. */
    val materialPiso: MaterialMesa,
    val altoTecho: Float,
    /** Rectángulo (x0, z0, x1, z1) que ocupa la torre. */
    val zonaTorre: FloatArray,
    val zonaCaida: FloatArray,
    val alturaCaida: Float,
    val puntoRescate: Vec3,
    /** Rectángulo (x0, z0, x1, z1) justo frente a la salida (donde se forma la cola de dados). */
    val zonaBoca: FloatArray,
    /** Rectángulo (x0, z0, x1, z1) de la bandeja donde se muestran los dados antes de lanzar. */
    val zonaBandeja: FloatArray
) {
    fun escenarioFisico() = EscenarioFisico(
        piezas = piezas.filter { it.colisiona }.map { it.pieza },
        altoTecho = altoTecho,
        zonaTorre = zonaTorre,
        puntoRescate = puntoRescate,
        zonaBoca = zonaBoca,
        zonaBandeja = zonaBandeja
    )
}

/** Ayudas geométricas para armar torres con cajas. Unidades: cm. */
internal object Piezas {

    /** Caja alineada a los ejes entre dos esquinas. */
    fun caja(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, sup: Superficie = Superficie.PARED,
             rest: Float = 0.35f, fric: Float = 0.25f) = PiezaEscenario(
        centro = Vec3((x0 + x1) / 2f, (y0 + y1) / 2f, (z0 + z1) / 2f),
        mitad = Vec3(abs(x1 - x0) / 2f, abs(y1 - y0) / 2f, abs(z1 - z0) / 2f),
        superficie = sup, restitucion = rest, friccion = fric
    )

    /**
     * Rampa plana cuya cara superior va del punto [alto] al punto [bajo] (sus centros de borde),
     * con [ancho] en la dirección horizontal [ejeAncho] y [grosor] hacia abajo.
     */
    fun rampa(alto: Vec3, bajo: Vec3, ancho: Float, ejeAncho: Vec3, grosor: Float = 0.6f,
              sup: Superficie = Superficie.PARED, rest: Float = 0.3f, fric: Float = 0.22f): PiezaEscenario {
        val d = bajo - alto
        val largo = d.longitud
        var eje = ejeAncho.normalizado()
        var rot = Quat.entrePares(Vec3(1f, 0f, 0f), Vec3(0f, 0f, 1f), d.normalizado(), eje)
        if (rot.rotar(Vec3.ARRIBA).y < 0f) {
            eje = -eje
            rot = Quat.entrePares(Vec3(1f, 0f, 0f), Vec3(0f, 0f, 1f), d.normalizado(), eje)
        }
        val arriba = rot.rotar(Vec3.ARRIBA)
        return PiezaEscenario(
            centro = (alto + bajo) * 0.5f - arriba * (grosor / 2f),
            mitad = Vec3(largo / 2f, grosor / 2f, ancho / 2f),
            rotacion = rot, superficie = sup, restitucion = rest, friccion = fric
        )
    }

    /** Segmento de pared vertical girado alrededor del eje Y (para torres redondas y bandejas curvas). */
    fun segmento(centro: Vec3, largo: Float, alto: Float, grosor: Float, anguloY: Float, sup: Superficie = Superficie.PARED) =
        PiezaEscenario(
            centro = centro,
            mitad = Vec3(largo / 2f, alto / 2f, grosor / 2f),
            rotacion = Quat.desdeEjeAngulo(Vec3.ARRIBA, anguloY),
            superficie = sup, restitucion = 0.35f, friccion = 0.25f
        )
}
