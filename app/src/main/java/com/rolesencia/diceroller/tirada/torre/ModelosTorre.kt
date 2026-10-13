package com.rolesencia.diceroller.tirada.torre

import com.rolesencia.diceroller.fisica.PiezaEscenario
import com.rolesencia.diceroller.fisica.Superficie
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ConfigMesa
import com.rolesencia.diceroller.tirada.torre.Acabado.COSTURA
import com.rolesencia.diceroller.tirada.torre.Acabado.INVISIBLE
import com.rolesencia.diceroller.tirada.torre.Acabado.LISO
import com.rolesencia.diceroller.tirada.torre.Acabado.PIEDRA
import com.rolesencia.diceroller.tirada.torre.Acabado.TABLONES
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Las tres torres de dados, inspiradas en tres modelos reales. Todas comparten la idea:
 * la torre ocupa el fondo de la bandeja; los dados entran por arriba, rebotan en rampas
 * internas alternadas y salen por una abertura al pie del frente hacia la bandeja.
 * Cada una tiene su ambiente (luces, fondo, niebla) y aberturas sólo en el frente (ventanas,
 * troneras, cordones) por donde se ve caer a los dados en parte.
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

    /** Piso de la bandeja: toda la mesa. */
    private fun piso(mesa: ConfigMesa) = floatArrayOf(-mesa.ancho / 2f, -mesa.profundidad / 2f, mesa.ancho / 2f, mesa.profundidad / 2f)

    /**
     * Torre de taberna: tablones de roble oscuro con flejes de hierro, dos ventanas con parteluz en
     * el frente (se ve caer a los dados) y velas encendidas sobre la bandeja. Luz cálida de vela.
     */
    private fun madera(mesa: ConfigMesa): ModeloTorre {
        val b = Base(mesa)
        val h = 24f
        val madera = 0xFF8A5530L
        val oscura = 0xFF4E2E16L
        val juntas = 0xFF2A170AL
        val hierro = 0xFF2B2624L
        fun pared(p: PiezaEscenario) = PiezaVisible(p, madera, TABLONES, colorDetalle = juntas)
        val piezas = mutableListOf(
            pared(Piezas.caja(-5.6f, 0f, b.zf, 5.6f, h, b.zb)),
            pared(Piezas.caja(-5.6f, 0f, b.zb, -5f, h, b.zSalida)),
            pared(Piezas.caja(5f, 0f, b.zb, 5.6f, h, b.zSalida)),
            // Frente de tablones con dos ventanas de taberna (marco y cruz de madera).
            PiezaVisible(
                Piezas.caja(-5f, 5.5f, b.zFront, 5f, h, b.zSalida), madera, TABLONES, colorDetalle = juntas,
                aberturas = listOf(
                    Abertura.Ventana(-3f, 3f, 15.4f, 19.6f, arco = 1.3f, parteluz = true),
                    Abertura.Ventana(-3f, 3f, 8.0f, 11.8f, arco = 1.3f, parteluz = true)
                )
            ),
            PiezaVisible(Piezas.rampa(Vec3(-5f, 21f, b.zc), Vec3(1.5f, 17f, b.zc), 7f, Z), oscura, TABLONES, colorDetalle = juntas),
            PiezaVisible(Piezas.rampa(Vec3(5f, 14f, b.zc), Vec3(-1.5f, 10f, b.zc), 7f, Z), oscura, TABLONES, colorDetalle = juntas),
            PiezaVisible(Piezas.rampa(Vec3(0f, 6f, b.zb), Vec3(0f, 0.25f, b.zLengua), 10f, X, rest = 0.3f, fric = 0.12f), oscura, TABLONES, colorDetalle = juntas),
            // Bandeja: paredes bajas de tablones; el frente más bajo para ver adentro.
            pared(Piezas.caja(-b.w, 0f, b.zf, -5.6f, 4f, b.zf + 0.6f)),
            pared(Piezas.caja(5.6f, 0f, b.zf, b.w, 4f, b.zf + 0.6f)),
            pared(Piezas.caja(-b.w, 0f, b.zf, -b.w + 0.6f, 4f, b.zFrente)),
            pared(Piezas.caja(b.w - 0.6f, 0f, b.zf, b.w, 4f, b.zFrente)),
            pared(Piezas.caja(-b.w, 0f, b.zFrente - 0.6f, b.w, 3f, b.zFrente))
        )
        // Flejes de hierro que abrazan la torre (frente y costados).
        for (y in listOf(5.9f, 22.6f)) {
            piezas += PiezaVisible(Piezas.caja(-5.75f, y, b.zSalida, 5.75f, y + 0.55f, b.zSalida + 0.14f), hierro, LISO, colisiona = false)
            piezas += PiezaVisible(Piezas.caja(-5.75f, y, b.zf + 0.2f, -5.6f, y + 0.55f, b.zSalida + 0.14f), hierro, LISO, colisiona = false)
            piezas += PiezaVisible(Piezas.caja(5.6f, y, b.zf + 0.2f, 5.75f, y + 0.55f, b.zSalida + 0.14f), hierro, LISO, colisiona = false)
        }
        piezas += almenas(-5.6f, 5.6f, b.zf, b.zb, h, madera, TABLONES, juntas)
        piezas += almenas(-b.w, -b.w + 0.6f, b.zSalida, b.zFrente, 4f, madera, TABLONES, juntas)
        piezas += almenas(b.w - 0.6f, b.w, b.zSalida, b.zFrente, 4f, madera, TABLONES, juntas)
        // Velas sobre las paredes del fondo de la bandeja.
        val velas = listOf(-(b.w + 5.6f) / 2f, (b.w + 5.6f) / 2f).map { x ->
            piezas += PiezaVisible(Piezas.caja(x - 0.35f, 4f, b.zf - 0.05f, x + 0.35f, 6.1f, b.zf + 0.65f), 0xFFEFE2C0L, LISO, colisiona = false)
            LuzPuntual(Vec3(x, 6.6f, b.zf + 0.3f), 0xFFFFA347L, alcance = 9f, intensidad = 1.15f, visible = true)
        }
        val ambiente = Ambiente(
            fondoArriba = 0xFF2E1C0FL, fondoAbajo = 0xFF0C0604L,
            ambiente = 0xFF5C4434L, luzPrincipal = 0xFFB98A5AL,
            luces = velas + LuzPuntual(Vec3(0f, 16f, b.zFrente + 4f), 0xFFFFB463L, alcance = 22f, intensidad = 0.45f),
            resplandores = listOf(
                Resplandor(0.14f, 0.10f, 0.07f, 0x90FFB347L), Resplandor(0.36f, 0.04f, 0.05f, 0x70FFC46BL),
                Resplandor(0.66f, 0.08f, 0.06f, 0x80FFB347L), Resplandor(0.88f, 0.16f, 0.08f, 0x70FFA040L),
                Resplandor(0.5f, 0.5f, 0.7f, 0x30FF9A3CL)
            ),
            brilloAberturas = 0x26FFB45AL
        )
        return ModeloTorre(
            "madera", nombre("madera"), "Roble de taberna con flejes de hierro, ventanas con parteluz y velas.",
            piezas, colorPiso = 0xFF5E3A1EL, materialPiso = MaterialMesa.MADERA, altoTecho = 70f,
            zonaTorre = b.zonaTorre, zonaCaida = b.zonaCaida, alturaCaida = 23.5f, puntoRescate = b.rescate,
            zonaBoca = b.zonaBoca, zonaBandeja = b.zonaBandeja,
            ambiente = ambiente, piso = piso(mesa), acabadoPiso = TABLONES, detallePiso = 0xFF24130AL
        )
    }

    /**
     * Castillo de muralla: piedra gastada con musgo, almenas alrededor, puerta en arco, ventanas
     * góticas y troneras sólo en el frente, antorchas de fuego violeta y niebla en lo alto.
     */
    private fun castillo(mesa: ConfigMesa): ModeloTorre {
        val b = Base(mesa)
        val h = 28f
        val piedra = 0xFF8C8D8FL
        val escalon = 0xFF6F7073L
        val juntas = 0xFF2C2D30L
        val musgo = 0xFF56663AL
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
        // Torre octogonal de muralla. Sólo el frente tiene aberturas: la puerta (abarca las caras
        // centrales), dos ventanas góticas sobre la puerta y troneras en las caras de los costados.
        val n = 8
        val r = 6.3f
        val largo = 2f * r * tan(PI / n).toFloat() + 0.3f
        val puerta = Abertura.Puerta(-5f, 5f, yArranque = 5.5f, arco = 2.2f)
        val luces = mutableListOf<LuzPuntual>()
        for (k in 0 until n) {
            val fi = (2.0 * PI * k / n + PI / n).toFloat()
            val afuera = Vec3(cos(fi), 0f, sin(fi))
            val centro = Vec3(r * cos(fi), h / 2f, b.zc + r * sin(fi))
            val frente = sin(fi) > 0.3f
            val central = sin(fi) > 0.8f
            val giro = -(fi + CUARTO)
            val aberturas = when {
                central -> listOf(
                    puerta,
                    Abertura.Ventana(-0.85f, 0.85f, 19.2f, 22.4f, arco = 1.5f, apuntado = true),
                    Abertura.Ventana(-0.85f, 0.85f, 11.6f, 14.6f, arco = 1.5f, apuntado = true)
                )
                frente -> listOf(
                    puerta,
                    Abertura.Ventana(-0.32f, 0.32f, 15.2f, 18.6f, arco = 0.35f, apuntado = true),
                    Abertura.Ventana(-0.32f, 0.32f, 7.4f, 10.6f, arco = 0.35f, apuntado = true)
                )
                else -> emptyList()
            }
            piezas += PiezaVisible(
                Piezas.segmento(centro, largo, h, 0.8f, giro), piedra, PIEDRA, colisiona = false, colorDetalle = juntas,
                aberturas = aberturas, haciaAfuera = afuera, colorMusgo = musgo
            )
            piezas += PiezaVisible(
                Piezas.segmento(Vec3(centro.x, h + 0.7f, centro.z), largo * 0.45f, 1.4f, 0.9f, giro),
                piedra, PIEDRA, colisiona = false, colorDetalle = juntas, colorMusgo = musgo
            )
            // Antorchas de fuego violeta a los lados de la puerta, en las caras de los costados.
            if (frente && !central) {
                val tangente = Vec3(-sin(fi), 0f, cos(fi))
                val lado = if (cos(fi) > 0f) 1f else -1f
                val u = (lado * 6.1f - centro.x) / tangente.x
                val base = Vec3(centro.x + tangente.x * u, 0f, centro.z + tangente.z * u) + afuera * 0.55f
                piezas += PiezaVisible(
                    Piezas.caja(base.x - 0.18f, 3.3f, base.z - 0.18f, base.x + 0.18f, 4.5f, base.z + 0.18f),
                    0xFF2A2420L, LISO, colisiona = false
                )
                luces += LuzPuntual(Vec3(base.x, 5.0f, base.z), if (lado < 0f) 0xFFC77DFFL else 0xFF9D6BFFL, alcance = 8f, intensidad = 1.2f, visible = true)
            }
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
            piezas += PiezaVisible(Piezas.segmento(c, lr, 3.2f, 0.9f, -(fi + CUARTO)), piedra, PIEDRA, colorDetalle = juntas, colorMusgo = musgo)
        }
        val ambiente = Ambiente(
            fondoArriba = 0xFFA4A6B4L, fondoAbajo = 0xFF2E2840L,
            ambiente = 0xFF66647AL, luzPrincipal = 0xFF9C9CAEL,
            luces = luces + listOf(
                LuzPuntual(Vec3(-7.5f, 0.6f, b.zSalida + 3f), 0xFFB050FFL, alcance = 13f, intensidad = 1.1f),
                LuzPuntual(Vec3(7.5f, 0.6f, b.zSalida + 3f), 0xFF34D8C4L, alcance = 13f, intensidad = 0.9f)
            ),
            resplandores = listOf(
                Resplandor(0.18f, 0.12f, 0.4f, 0x50FFFFFFL), Resplandor(0.85f, 0.22f, 0.35f, 0x40FFFFFFL),
                Resplandor(0.5f, 1.05f, 0.65f, 0x608A3CC8L)
            ),
            niebla = 0xFFB2B4C2L,
            brilloAberturas = 0x229A5CFFL
        )
        return ModeloTorre(
            "castillo", nombre("castillo"), "Torre de muralla con musgo, puerta en arco, ventanas góticas y antorchas.",
            piezas, colorPiso = 0xFF55565CL, materialPiso = MaterialMesa.MADERA, altoTecho = 70f,
            zonaTorre = b.zonaTorre, zonaCaida = b.zonaCaida, alturaCaida = 26f, puntoRescate = Vec3(0f, 0f, zt),
            zonaBoca = b.zonaBoca,
            // Antes de lanzar, los dados esperan dentro del aro.
            zonaBandeja = floatArrayOf(-rr * 0.68f, zt - rr * 0.4f, rr * 0.68f, zt + rr * 0.68f),
            ambiente = ambiente, piso = piso(mesa), acabadoPiso = PIEDRA, detallePiso = 0xFF2A2B2FL
        )
    }

    /**
     * Caja de cuero ciruela con costura dorada, fieltro turquesa por dentro y una ventana de medio
     * punto ribeteada y cosida al frente (por ella se ve caer a los dados). Luces violeta y turquesa.
     */
    private fun cuero(mesa: ConfigMesa): ModeloTorre {
        val b = Base(mesa)
        val h = 18f
        val cuero = 0xFF4A2463L
        val costura = 0xFFD8B46AL
        val fieltro = 0xFF13625DL
        // Cuero por fuera, fieltro en las caras que dan al interior de la torre.
        val interior = Vec3(0f, h / 2f, b.zc)
        fun pared(p: PiezaEscenario) = PiezaVisible(p, cuero, COSTURA, colorDetalle = costura)
        fun paredForrada(p: PiezaEscenario) =
            PiezaVisible(p, cuero, COSTURA, colorDetalle = costura, colorInterior = fieltro, puntoInterior = interior)
        val piezas = mutableListOf(
            paredForrada(Piezas.caja(-5.6f, 0f, b.zf, 5.6f, h, b.zb)),
            paredForrada(Piezas.caja(-5.6f, 0f, b.zb, -5f, h, b.zSalida)),
            paredForrada(Piezas.caja(5f, 0f, b.zb, 5.6f, h, b.zSalida)),
            PiezaVisible(
                Piezas.caja(-5f, 6f, b.zFront, 5f, h, b.zSalida), cuero, COSTURA, colorDetalle = costura,
                // Ventana de medio punto con ribete de cuero oscuro y doble pespunte dorado.
                aberturas = listOf(Abertura.Ventana(-2.3f, 2.3f, 7.4f, 13.6f, arco = 2.3f, ribete = 0xFF2A1238L, costura = costura))
            ),
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
        val ambiente = Ambiente(
            fondoArriba = 0xFF2C1342L, fondoAbajo = 0xFF0A0512L,
            ambiente = 0xFF5A4670L, luzPrincipal = 0xFF9C86B4L,
            luces = listOf(
                LuzPuntual(Vec3(-6.5f, 0.5f, b.zSalida + 3f), 0xFF2EE6C8L, alcance = 14f, intensidad = 1.2f),
                LuzPuntual(Vec3(6.5f, 0.5f, b.zSalida + 3f), 0xFFB060FFL, alcance = 14f, intensidad = 1.2f),
                LuzPuntual(Vec3(0f, 20f, b.zf - 2f), 0xFFE070FFL, alcance = 16f, intensidad = 0.5f)
            ),
            resplandores = listOf(
                Resplandor(0.12f, 0.95f, 0.45f, 0x6030E0C0L), Resplandor(0.88f, 0.92f, 0.45f, 0x60A050FFL),
                Resplandor(0.5f, 0.0f, 0.6f, 0x40C8A0FFL)
            ),
            brilloAberturas = 0x1E2EE6C8L
        )
        return ModeloTorre(
            "cuero", nombre("cuero"), "Cuero ciruela con costura dorada, ventana ribeteada y fieltro turquesa.",
            piezas, colorPiso = 0xFF2E1542L, materialPiso = MaterialMesa.LONA, altoTecho = 70f,
            zonaTorre = b.zonaTorre, zonaCaida = b.zonaCaida, alturaCaida = 16.5f, puntoRescate = b.rescate,
            zonaBoca = b.zonaBoca, zonaBandeja = b.zonaBandeja,
            ambiente = ambiente, piso = piso(mesa), acabadoPiso = LISO, detallePiso = 0xFF1A0B26L
        )
    }
}
