package com.rolesencia.diceroller.tirada.torre

import com.rolesencia.diceroller.fisica.PiezaEscenario
import com.rolesencia.diceroller.fisica.Superficie
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ConfigMesa
import com.rolesencia.diceroller.tirada.torre.Acabado.COSTURA
import com.rolesencia.diceroller.tirada.torre.Acabado.INVISIBLE
import com.rolesencia.diceroller.tirada.torre.Acabado.LADRILLO
import com.rolesencia.diceroller.tirada.torre.Acabado.LISO
import com.rolesencia.diceroller.tirada.torre.Acabado.PIEDRA
import com.rolesencia.diceroller.tirada.torre.Acabado.VIDRIO
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Las tres torres de dados, inspiradas en tres modelos reales. Todas comparten la idea:
 * la torre ocupa el fondo de la bandeja; los dados entran por arriba, rebotan en rampas
 * internas alternadas y salen por una abertura al pie del frente hacia la bandeja.
 * La pared frontal se dibuja casi transparente (corte) para ver los dados caer.
 */
object ModelosTorre {

    val ids = listOf("madera", "castillo", "cuero")

    fun nombre(id: String): String = when (id) {
        "madera" -> "Torre de madera"
        "castillo" -> "Castillo de piedra"
        else -> "Caja de cuero"
    }

    /** Material del piso de cada torre, para el sonido (sin construir la geometría). */
    fun materialPiso(id: String): MaterialMesa = if (id == "cuero") MaterialMesa.LONA else MaterialMesa.MADERA

    /** Profundidad mínima de la bandeja para que entren la torre y la zona de salida. */
    const val PROFUNDIDAD_MINIMA = 24f

    fun construir(id: String, mesa: ConfigMesa): ModeloTorre = when (id) {
        "castillo" -> castillo(mesa)
        "cuero" -> cuero(mesa)
        else -> madera(mesa)
    }

    // Geometría común: interior de 10 × 7 cm pegado al fondo de la bandeja.
    private class Base(mesa: ConfigMesa) {
        val w = mesa.ancho / 2f
        val zf = -mesa.profundidad / 2f
        val zb = zf + 0.6f
        val zFront = zb + 7f
        val zc = (zb + zFront) / 2f
        val zSalida = zFront + 0.6f
        val zFrente = mesa.profundidad / 2f
        val zonaTorre = floatArrayOf(-5.6f, zf, 5.6f, zSalida)
        /** Centros posibles de los dados al soltarlos: 3 columnas × 2 filas por capa. */
        val zonaCaida = floatArrayOf(-3.2f, zc - 1.5f, 3.2f, zc + 1.5f)
        val rescate = Vec3(0f, 0f, (zSalida + zFrente) / 2f)
        /** Donde termina la lengua de salida, ya dentro de la bandeja. */
        val zLengua = zSalida + LENGUA
        val zonaBoca = floatArrayOf(-5.6f, zSalida, 5.6f, zLengua + 3f)
        val zonaBandeja = floatArrayOf(-w + 1.8f, zLengua + 1.5f, w - 1.8f, zFrente - 1.8f)
    }

    /**
     * La rampa de salida no termina en la puerta: sigue unos centímetros dentro de la bandeja
     * (como la "lengua" de las torres reales). Los dados salen lanzados y lejos de la boca,
     * en vez de apilarse en la puerta y tapar a los que vienen detrás.
     */
    private const val LENGUA = 4f

    private val X = Vec3(1f, 0f, 0f)
    private val Z = Vec3(0f, 0f, 1f)
    private const val CUARTO = (PI / 2).toFloat()

    /** Almenas: bloques decorativos sobre un borde, alternados cada [paso] cm. */
    private fun almenas(x0: Float, x1: Float, z0: Float, z1: Float, y: Float, color: Long, acabado: Acabado, detalle: Long, paso: Float = 2.4f): List<PiezaVisible> {
        val largoX = x1 - x0
        val largoZ = z1 - z0
        val largo = maxOf(largoX, largoZ)
        val n = (largo / paso).toInt()
        return (0 until n step 2).map { k ->
            val t0 = k * paso
            val t1 = minOf(t0 + paso, largo)
            val p = if (largoX >= largoZ) Piezas.caja(x0 + t0, y, z0, x0 + t1, y + 1.2f, z1)
            else Piezas.caja(x0, y, z0 + t0, x1, y + 1.2f, z0 + t1)
            PiezaVisible(p, color, acabado, colisiona = false, colorDetalle = detalle)
        }
    }

    /** Torre de madera grabada con tres escalones que se cruzan y bandeja almenada. */
    private fun madera(mesa: ConfigMesa): ModeloTorre {
        val b = Base(mesa)
        val h = 24f
        val madera = 0xFFD9B98AL
        val oscura = 0xFFB08A5AL
        val juntas = 0xFF8C6A42L
        fun pared(p: PiezaEscenario) = PiezaVisible(p, madera, LADRILLO, colorDetalle = juntas)
        val piezas = mutableListOf(
            pared(Piezas.caja(-5.6f, 0f, b.zf, 5.6f, h, b.zb)),
            pared(Piezas.caja(-5.6f, 0f, b.zb, -5f, h, b.zSalida)),
            pared(Piezas.caja(5f, 0f, b.zb, 5.6f, h, b.zSalida)),
            PiezaVisible(Piezas.caja(-5f, 5.5f, b.zFront, 5f, h, b.zSalida), madera, VIDRIO, colorDetalle = juntas),
            PiezaVisible(Piezas.rampa(Vec3(-5f, 21f, b.zc), Vec3(1.5f, 17f, b.zc), 7f, Z), oscura, LISO),
            PiezaVisible(Piezas.rampa(Vec3(5f, 14f, b.zc), Vec3(-1.5f, 10f, b.zc), 7f, Z), oscura, LISO),
            PiezaVisible(Piezas.rampa(Vec3(0f, 6f, b.zb), Vec3(0f, 0.25f, b.zLengua), 10f, X, rest = 0.3f, fric = 0.12f), oscura, LISO),
            // Bandeja: paredes bajas de ladrillo; el frente más bajo para ver adentro.
            pared(Piezas.caja(-b.w, 0f, b.zf, -5.6f, 4f, b.zf + 0.6f)),
            pared(Piezas.caja(5.6f, 0f, b.zf, b.w, 4f, b.zf + 0.6f)),
            pared(Piezas.caja(-b.w, 0f, b.zf, -b.w + 0.6f, 4f, b.zFrente)),
            pared(Piezas.caja(b.w - 0.6f, 0f, b.zf, b.w, 4f, b.zFrente)),
            pared(Piezas.caja(-b.w, 0f, b.zFrente - 0.6f, b.w, 3f, b.zFrente))
        )
        piezas += almenas(-5.6f, 5.6f, b.zf, b.zb, h, madera, LADRILLO, juntas)
        piezas += almenas(-b.w, -b.w + 0.6f, b.zSalida, b.zFrente, 4f, madera, LADRILLO, juntas)
        piezas += almenas(b.w - 0.6f, b.w, b.zSalida, b.zFrente, 4f, madera, LADRILLO, juntas)
        return ModeloTorre(
            "madera", nombre("madera"), "Madera grabada, tres escalones que se cruzan y bandeja almenada.",
            piezas, colorPiso = 0xFFCDB089L, materialPiso = MaterialMesa.MADERA, altoTecho = 70f,
            zonaTorre = b.zonaTorre, zonaCaida = b.zonaCaida, alturaCaida = 23.5f, puntoRescate = b.rescate,
            zonaBoca = b.zonaBoca, zonaBandeja = b.zonaBandeja
        )
    }

    /** Castillo de piedra redondo: cuatro tramos de escalera interna y base circular. */
    private fun castillo(mesa: ConfigMesa): ModeloTorre {
        val b = Base(mesa)
        val h = 28f
        val piedra = 0xFF7D7F84L
        val escalon = 0xFF6B6D72L
        val juntas = 0xFF3F4044L
        val piezas = mutableListOf(
            // Interior físico cuadrado (invisible); la forma visible es el octógono de piedra.
            PiezaVisible(Piezas.caja(-5.6f, 0f, b.zf, 5.6f, h, b.zb), piedra, INVISIBLE),
            PiezaVisible(Piezas.caja(-5.6f, 0f, b.zb, -5f, h, b.zSalida), piedra, INVISIBLE),
            PiezaVisible(Piezas.caja(5f, 0f, b.zb, 5.6f, h, b.zSalida), piedra, INVISIBLE),
            PiezaVisible(Piezas.caja(-5f, 5.5f, b.zFront, 5f, h, b.zSalida), piedra, INVISIBLE),
            PiezaVisible(Piezas.rampa(Vec3(-5f, 24.5f, b.zc), Vec3(1.5f, 21f, b.zc), 7f, Z, fric = 0.2f), escalon, PIEDRA, colorDetalle = juntas),
            PiezaVisible(Piezas.rampa(Vec3(5f, 18f, b.zc), Vec3(-1.5f, 14.5f, b.zc), 7f, Z, fric = 0.2f), escalon, PIEDRA, colorDetalle = juntas),
            PiezaVisible(Piezas.rampa(Vec3(-5f, 12.5f, b.zc), Vec3(1.5f, 9f, b.zc), 7f, Z, fric = 0.2f), escalon, PIEDRA, colorDetalle = juntas),
            PiezaVisible(Piezas.rampa(Vec3(0f, 5.3f, b.zb), Vec3(0f, 0.25f, b.zLengua), 10f, X, rest = 0.3f, fric = 0.12f), escalon, PIEDRA, colorDetalle = juntas)
        )
        // Torre octogonal visible: el frente en corte para ver los tramos.
        val n = 8
        val r = 6.3f
        val largo = 2f * r * tan(PI / n).toFloat() + 0.3f
        for (k in 0 until n) {
            val fi = (2.0 * PI * k / n + PI / n).toFloat()
            val centro = Vec3(r * cos(fi), h / 2f, b.zc + r * sin(fi))
            val frente = sin(fi) > 0.3f
            val giro = -(fi + CUARTO)
            piezas += PiezaVisible(Piezas.segmento(centro, largo, h, 0.8f, giro), piedra, if (frente) VIDRIO else PIEDRA, colisiona = false, colorDetalle = juntas)
            if (!frente) piezas += PiezaVisible(
                Piezas.segmento(Vec3(centro.x, h + 0.7f, centro.z), largo * 0.45f, 1.4f, 0.9f, giro),
                piedra, PIEDRA, colisiona = false, colorDetalle = juntas
            )
        }
        // Base circular de la bandeja (abierta hacia la torre).
        val zt = (b.zSalida + b.zFrente) / 2f
        val rr = minOf(b.w - 0.6f, (b.zFrente - b.zSalida) / 2f + 1.5f)
        val m = 16
        val lr = 2f * rr * tan(PI / m).toFloat() + 0.25f
        for (k in 0 until m) {
            val fi = (2.0 * PI * k / m).toFloat()
            val c = Vec3(rr * cos(fi), 1.6f, zt + rr * sin(fi))
            // El aro se abre frente a la torre: ningún tramo puede tapar la puerta ni el pasillo de
            // salida, sea cual sea el tamaño de la bandeja (crece con la cantidad de dados).
            val tangente = Vec3(-sin(fi), 0f, cos(fi)) * (lr / 2f)
            val tapaLaSalida = listOf(c - tangente, c, c + tangente).any { p -> kotlin.math.abs(p.x) < 6.6f && p.z < b.zLengua + 1f }
            if (tapaLaSalida) continue
            piezas += PiezaVisible(Piezas.segmento(c, lr, 3.2f, 0.9f, -(fi + CUARTO)), piedra, PIEDRA, colorDetalle = juntas)
        }
        return ModeloTorre(
            "castillo", nombre("castillo"), "Torre redonda de piedra con escalera interna y base circular.",
            piezas, colorPiso = 0xFF4E5055L, materialPiso = MaterialMesa.MADERA, altoTecho = 70f,
            zonaTorre = b.zonaTorre, zonaCaida = b.zonaCaida, alturaCaida = 26f, puntoRescate = Vec3(0f, 0f, zt),
            zonaBoca = b.zonaBoca,
            // Antes de lanzar, los dados esperan dentro del aro.
            zonaBandeja = floatArrayOf(-rr * 0.68f, zt - rr * 0.4f, rr * 0.68f, zt + rr * 0.68f)
        )
    }

    /** Caja de cuero negro con fieltro azul: un deflector y bandeja de bordes curvos. */
    private fun cuero(mesa: ConfigMesa): ModeloTorre {
        val b = Base(mesa)
        val h = 18f
        val cuero = 0xFF1E1F24L
        val costura = 0xFF29B6F6L
        val fieltro = 0xFF1565C0L
        // Cuero por fuera, fieltro azul en las caras que dan al interior de la torre.
        val interior = Vec3(0f, h / 2f, b.zc)
        fun pared(p: PiezaEscenario) = PiezaVisible(p, cuero, COSTURA, colorDetalle = costura)
        fun paredForrada(p: PiezaEscenario) =
            PiezaVisible(p, cuero, COSTURA, colorDetalle = costura, colorInterior = fieltro, puntoInterior = interior)
        val piezas = mutableListOf(
            paredForrada(Piezas.caja(-5.6f, 0f, b.zf, 5.6f, h, b.zb)),
            paredForrada(Piezas.caja(-5.6f, 0f, b.zb, -5f, h, b.zSalida)),
            paredForrada(Piezas.caja(5f, 0f, b.zb, 5.6f, h, b.zSalida)),
            PiezaVisible(Piezas.caja(-5f, 6f, b.zFront, 5f, h, b.zSalida), cuero, VIDRIO, colorDetalle = costura),
            PiezaVisible(Piezas.rampa(Vec3(-5f, 14f, b.zc), Vec3(1.5f, 10f, b.zc), 7f, Z, sup = Superficie.MESA, rest = 0.2f, fric = 0.3f), fieltro, LISO),
            // Rampa de salida algo más resbaladiza: los dados salen con envión a la bandeja.
            PiezaVisible(Piezas.rampa(Vec3(0f, 6f, b.zb), Vec3(0f, 0.25f, b.zLengua), 10f, X, sup = Superficie.MESA, rest = 0.25f, fric = 0.12f), fieltro, LISO)
        )
        // Bandeja con laterales curvos: alturas que bajan hacia el frente, como la ola del modelo.
        val alturas = listOf(7f, 5.5f, 4f, 3f, 2.6f, 3.2f)
        val tramo = (b.zFrente - b.zf) / alturas.size
        alturas.forEachIndexed { k, alto ->
            val z0 = b.zf + k * tramo
            piezas += pared(Piezas.caja(-b.w, 0f, z0, -b.w + 0.7f, alto, z0 + tramo + 0.01f))
            piezas += pared(Piezas.caja(b.w - 0.7f, 0f, z0, b.w, alto, z0 + tramo + 0.01f))
        }
        piezas += pared(Piezas.caja(-b.w, 0f, b.zFrente - 0.7f, b.w, 2.6f, b.zFrente))
        piezas += pared(Piezas.caja(-b.w, 0f, b.zf, -5.6f, 7f, b.zf + 0.7f))
        piezas += pared(Piezas.caja(5.6f, 0f, b.zf, b.w, 7f, b.zf + 0.7f))
        return ModeloTorre(
            "cuero", nombre("cuero"), "Cuero negro con costura azul, fieltro por dentro y bandeja curva.",
            piezas, colorPiso = fieltro, materialPiso = MaterialMesa.LONA, altoTecho = 70f,
            zonaTorre = b.zonaTorre, zonaCaida = b.zonaCaida, alturaCaida = 16.5f, puntoRescate = b.rescate,
            zonaBoca = b.zonaBoca, zonaBandeja = b.zonaBandeja
        )
    }
}
