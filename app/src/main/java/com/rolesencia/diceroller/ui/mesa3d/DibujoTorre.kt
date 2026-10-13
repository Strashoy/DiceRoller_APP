package com.rolesencia.diceroller.ui.mesa3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.os.Build
import com.rolesencia.diceroller.fisica.Poliedros
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.tirada.torre.Abertura
import com.rolesencia.diceroller.tirada.torre.Acabado
import com.rolesencia.diceroller.tirada.torre.Ambiente
import com.rolesencia.diceroller.tirada.torre.ModeloTorre
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Algo para dibujar en orden de profundidad (lo más lejano primero). */
class Dibujable(val profundidad: Float, val dibujar: DrawScope.() -> Unit)

private val LUZ_TORRE = Vec3(-0.35f, 1f, 0.45f).normalizado()

/** Una baldosa de una cara (las caras grandes se parten para ordenar bien contra los dados). */
private class Tesela(
    val esquinas: List<Vec3>,
    val centro: Vec3,
    val normal: Vec3,
    /** Color ya iluminado (la luz es fija: se calcula una sola vez). */
    val color: Color,
    val juntas: Color?,
    val alfa: Float,
    /** Huecos (ventanas, puerta) que atraviesan esta baldosa. */
    val huecos: List<List<Vec3>> = emptyList(),
    /** Veta de la madera: un trazo a lo largo del tablón. */
    val veta: Pair<Vec3, Vec3>? = null,
    /** Color iluminado en cada esquina: la luz se interpola suave entre baldosas (sin cuadros). */
    val colores: IntArray? = null,
    /**
     * Superficie interior de la torre (rampas, forro): se ordena como si estuviera un poco más
     * lejos, para que nunca se pinte encima de las paredes exteriores que la tapan.
     */
    val interior: Boolean = false
)

/** Contorno de una cara: borde oscuro, o costura punteada en el cuero. */
private class Contorno(val esquinas: List<Vec3>, val centro: Vec3, val normal: Vec3, val color: Color, val punteado: Boolean)

/**
 * Adorno pegado a una cara: marco de ventana, parteluz, ribete. Se ordena por su punto más cercano
 * a la cámara ([puntos]), así ninguna baldosa de su misma pared lo tapa.
 */
private class Adorno(val centro: Vec3, val normal: Vec3, val puntos: List<Vec3> = listOf(centro), val dibujar: DrawScope.(Camara) -> Unit)

/** Iluminación de la escena de una torre: ambiente + luz principal + luces puntuales + niebla. */
private class Iluminacion(private val amb: Ambiente) {
    private fun Long.rgb() = floatArrayOf(((this shr 16) and 0xFF) / 255f, ((this shr 8) and 0xFF) / 255f, (this and 0xFF) / 255f)
    private val ambiente = amb.ambiente.rgb()
    private val principal = amb.luzPrincipal.rgb()
    private val luces = amb.luces.map { Triple(it, it.color.rgb(), it.intensidad) }
    private val niebla = amb.niebla?.let { Color(it) }

    /** [interior]: dentro de la torre llega menos luz. */
    fun iluminar(base: Color, p: Vec3, n: Vec3, interior: Boolean = false): Color {
        val difusa = max(0f, n dot LUZ_TORRE)
        val c = floatArrayOf(base.red, base.green, base.blue)
        val salida = FloatArray(3) { k -> c[k] * (ambiente[k] + principal[k] * difusa) }
        for ((luz, color, intensidad) in luces) {
            val d = luz.posicion - p
            val dist = d.longitud.coerceAtLeast(0.3f)
            val atenuacion = intensidad / (1f + (dist / luz.alcance) * (dist / luz.alcance) * 4f)
            val lambert = 0.25f + 0.75f * max(0f, (n dot d) / dist)
            for (k in 0..2) salida[k] += (c[k] * 0.85f + 0.15f) * color[k] * atenuacion * lambert
        }
        val f = if (interior) 0.55f else 1f
        var resultado = Color((salida[0] * f).coerceIn(0f, 1f), (salida[1] * f).coerceIn(0f, 1f), (salida[2] * f).coerceIn(0f, 1f))
        if (niebla != null) resultado = lerp(resultado, niebla, ((p.y - 8f) / 40f).coerceIn(0f, 0.4f))
        return resultado
    }
}

/**
 * Geometría visible de una torre, precalculada una vez (incluida la luz). Cada cuadro sólo se
 * proyecta, se descartan las caras de espaldas y se arma la lista de [Dibujable]s.
 */
class EscenaTorre(val modelo: ModeloTorre) {

    private val teselas = ArrayList<Tesela>()
    private val contornos = ArrayList<Contorno>()
    private val adornos = ArrayList<Adorno>()
    private val piso = ArrayList<Tesela>()
    private val luz = Iluminacion(modelo.ambiente)

    /** Vértices de todas las piezas visibles (para encuadrar la cámara). */
    val vertices = ArrayList<Vec3>()

    private fun interior(p: Vec3): Boolean {
        val z = modelo.zonaTorre
        return p.x > z[0] + 0.4f && p.x < z[2] - 0.4f && p.z > z[1] + 0.4f && p.z < z[3] - 0.4f
    }

    init {
        var indiceCara = 0
        for (pv in modelo.piezas) {
            if (pv.acabado == Acabado.INVISIBLE) continue
            val p = pv.pieza
            val poli = Poliedros.caja(p.mitad)
            val mundo = poli.vertices.map { p.centro + p.rotacion.rotar(it) }
            vertices += mundo
            val detalle = Color(pv.colorDetalle)
            for (cara in poli.caras) {
                indiceCara++
                val esq = cara.vertices.map { mundo[it] }
                val normal = p.rotacion.rotar(cara.normal)
                val centro = esq.reduce { a, b -> a + b } * 0.25f
                // Forro: las caras que miran al interior de la torre llevan otro color y van lisas.
                val forrada = pv.colorInterior != null && pv.puntoInterior != null &&
                    ((pv.puntoInterior - centro) dot normal) > 0f
                val base = Color(if (forrada) pv.colorInterior!! else pv.color)
                val acabado = if (forrada) Acabado.LISO else pv.acabado
                val calada = pv.aberturas.isNotEmpty() && (normal dot pv.haciaAfuera.normalizado()) > 0.7f && abs(normal.y) < 0.3f
                val marco = if (calada) MarcoCara(centro, normal) else null
                val huecos = marco?.let { m -> pv.aberturas.mapNotNull { m.contorno(it) } } ?: emptyList()
                // Baldosas de esta cara (caja en u/y y centro): los adornos se ordenan contra ellas.
                val celdas = ArrayList<Pair<FloatArray, Vec3>>()
                partir(esq, acabado, indiceCara) { esqT, i, j, total ->
                    val centroT = esqT.reduce { a, b -> a + b } * 0.25f
                    val color = variar(base, acabado, i, j, total, centroT, pv.colorMusgo)
                    val enCelda = if (marco == null) emptyList() else {
                        val (u0, y0, u1, y1) = marco.caja(esqT)
                        celdas += marco.caja(esqT) to centroT
                        huecos.filter { h -> h.u0 < u1 && h.u1 > u0 && h.y0 < y1 && h.y1 > y0 }.map { h -> h.puntos.map { (u, y) -> marco.aMundo(u, y) } }
                    }
                    val veta = if (acabado == Acabado.TABLONES) {
                        val k = ((i * 37 + j * 53 + indiceCara * 11) % 7) / 7f
                        val a = esqT[0] + (esqT[3] - esqT[0]) * (0.3f + 0.4f * k)
                        val b = a + (esqT[1] - esqT[0]) * (0.55f + 0.35f * k)
                        (a + (esqT[1] - esqT[0]) * 0.08f) to b
                    } else null
                    val adentro = interior(centroT)
                    teselas += Tesela(
                        esqT, centroT, normal, luz.iluminar(color, centroT, normal, adentro),
                        when (acabado) { Acabado.LADRILLO, Acabado.PIEDRA, Acabado.TABLONES -> detalle; else -> null },
                        if (acabado == Acabado.VIDRIO) 0.16f else 1f, enCelda, veta,
                        IntArray(4) { k -> luz.iluminar(color, esqT[k], normal, adentro).toArgb() },
                        interior = adentro
                    )
                }
                val iluminada = luz.iluminar(base, centro, normal, interior(centro))
                if (pv.acabado == Acabado.COSTURA && !forrada) {
                    // Costura: rectángulo un poco hacia adentro del borde de la cara.
                    val inset = esq.map { it + (centro - it).normalizado() * 0.45f }
                    contornos += Contorno(inset, centro, normal, luz.iluminar(detalle, centro, normal), punteado = true)
                }
                contornos += Contorno(esq, centro, normal, lerp(iluminada, Color.Black, 0.55f), punteado = false)
                if (marco != null) for ((ab, hueco) in pv.aberturas.zip(pv.aberturas.map { marco.contorno(it) })) {
                    if (hueco != null) agregarAdornos(ab, hueco, marco, iluminada, detalle, esq, celdas)
                }
            }
        }
        construirPiso()
    }

    // ---------- Aparejo: cómo se parte cada cara y con qué tono ----------

    /** Parte la cara en baldosas; piedras, ladrillos y tablones van trabados (hilera por medio). */
    private fun partir(esq: List<Vec3>, acabado: Acabado, semilla: Int, hacer: (List<Vec3>, Int, Int, Int) -> Unit) {
        var eu = esq[1] - esq[0]
        var ev = esq[3] - esq[0]
        val origen = esq[0]
        // Las hileras corren por el borde más horizontal.
        if (abs(ev.normalizado().y) < abs(eu.normalizado().y)) { val t = eu; eu = ev; ev = t }
        val (tamU, tamV, trabado) = when (acabado) {
            Acabado.LADRILLO -> Triple(2.2f, 1.1f, true)
            Acabado.PIEDRA -> Triple(2.1f, 1.5f, true)
            Acabado.TABLONES -> Triple(5.5f, 1.25f, true)
            else -> Triple(2.5f, 2.5f, false)
        }
        val nu = max(1, ceil(eu.longitud / tamU).toInt())
        val nv = max(1, ceil(ev.longitud / tamV).toInt())
        fun punto(a: Float, b: Float) = origen + eu * a + ev * b
        for (j in 0 until nv) {
            val b0 = j / nv.toFloat()
            val b1 = (j + 1) / nv.toFloat()
            val corrido = trabado && (j + semilla) % 2 == 1 && nu > 1
            val cortes = if (corrido) (0..nu + 1).map { ((it - 0.5f) / nu).coerceIn(0f, 1f) }.distinct()
            else (0..nu).map { it / nu.toFloat() }
            for (i in 0 until cortes.size - 1) {
                val a0 = cortes[i]
                val a1 = cortes[i + 1]
                if (a1 - a0 < 1e-3f) continue
                hacer(listOf(punto(a0, b0), punto(a1, b0), punto(a1, b1), punto(a0, b1)), i, j, nu)
            }
        }
    }

    /** Variación de tono por baldosa: piedras y tablones no son todos iguales; algunas piedras con musgo. */
    private fun variar(base: Color, acabado: Acabado, i: Int, j: Int, total: Int, centro: Vec3, musgo: Long?): Color {
        val variacion = when (acabado) {
            Acabado.PIEDRA -> ((i * 73 + j * 151 + total * 7) % 9) / 9f * 0.26f - 0.13f
            Acabado.LADRILLO -> ((i * 31 + j * 17) % 5) / 5f * 0.08f - 0.04f
            Acabado.TABLONES -> ((i * 47 + j * 29) % 6) / 6f * 0.16f - 0.08f
            else -> 0f
        }
        var color = if (variacion >= 0f) lerp(base, Color.White, variacion) else lerp(base, Color.Black, -variacion)
        if (musgo != null && acabado == Acabado.PIEDRA) {
            val azar = (i * 92821 + j * 68917 + total * 31) % 100
            val probabilidad = if (centro.y < 5f) 34 else if (centro.y > 26f) 22 else 10
            if (azar < probabilidad) color = lerp(color, Color(musgo), 0.3f + (azar % 10) / 28f)
        }
        return color
    }

    // ---------- Aberturas ----------

    private class Hueco(val puntos: List<Pair<Float, Float>>) {
        val u0 = puntos.minOf { it.first }
        val u1 = puntos.maxOf { it.first }
        val y0 = puntos.minOf { it.second }
        val y1 = puntos.maxOf { it.second }
    }

    /** Sistema de coordenadas de una cara vertical: `u` horizontal desde su centro, `y` la altura. */
    private class MarcoCara(val centro: Vec3, val normal: Vec3) {
        val h = (Vec3.ARRIBA cross normal).normalizado()

        fun aMundo(u: Float, y: Float) = centro + h * u + Vec3(0f, y - centro.y, 0f) + normal * 0.01f

        fun caja(esq: List<Vec3>): FloatArray {
            val us = esq.map { (it - centro) dot h }
            return floatArrayOf(us.min(), esq.minOf { it.y }, us.max(), esq.maxOf { it.y })
        }

        fun contorno(ab: Abertura): Hueco? = when (ab) {
            is Abertura.Ventana -> Hueco(ventana(ab.u0, ab.u1, ab.y0, ab.y1, ab.arco, ab.apuntado))
            is Abertura.Puerta -> if (abs(h.x) < 0.2f) null else {
                // La puerta se mide en X del mundo: se pasa a la `u` de esta cara.
                ventana(ab.x0, ab.x1, -1f, ab.yArranque, ab.arco, apuntado = false)
                    .map { (x, y) -> (x - centro.x) / h.x to y }
                    .let { Hueco(it) }
            }
        }

        fun ventana(u0: Float, u1: Float, y0: Float, y1: Float, arco: Float, apuntado: Boolean): List<Pair<Float, Float>> {
            val medio = (u0 + u1) / 2f
            val semi = (u1 - u0) / 2f
            val puntos = mutableListOf(u0 to y0, u1 to y0, u1 to y1)
            if (apuntado) {
                // Arco gótico: dos arcos de radio igual a la luz, centrados en los arranques opuestos.
                val escala = arco / (sqrt(3f) * semi)
                for (k in 1..8) {
                    val t = (PI / 3 * k / 8).toFloat()
                    puntos += (u0 + 2 * semi * cos(t)) to (y1 + 2 * semi * sin(t) * escala)
                }
                for (k in 1..8) {
                    val t = (2 * PI / 3 + PI / 3 * k / 8).toFloat()
                    puntos += (u1 + 2 * semi * cos(t)) to (y1 + 2 * semi * sin(t) * escala)
                }
            } else if (arco > 0f) {
                for (k in 1..15) {
                    val t = (PI * k / 16).toFloat()
                    puntos += (medio + semi * cos(t)) to (y1 + arco * sin(t))
                }
            }
            puntos += u0 to y1
            return puntos
        }
    }

    private fun agregarAdornos(
        ab: Abertura, hueco: Hueco, marco: MarcoCara, pared: Color, detalle: Color, cara: List<Vec3>,
        celdas: List<Pair<FloatArray, Vec3>>
    ) {
        val mundo = hueco.puntos.map { (u, y) -> marco.aMundo(u, y) }
        val centro = marco.aMundo((hueco.u0 + hueco.u1) / 2f, (hueco.y0 + hueco.y1) / 2f)
        val brillo = Color(modelo.ambiente.brilloAberturas)
        val borde = lerp(pared, Color.Black, 0.6f)
        when (ab) {
            is Abertura.Ventana -> {
                // El marco (y el ribete con su pespunte) sobresale del hueco: se ordena detrás de
                // todas las baldosas que pisa, para que ninguna de ellas lo tape.
                val margen = if (ab.ribete != null) 1.1f else 0.4f
                val pisadas = celdas.filter { (c, _) ->
                    c[0] < hueco.u1 + margen && c[2] > hueco.u0 - margen && c[1] < hueco.y1 + margen && c[3] > hueco.y0 - margen
                }.map { it.second }
                adornos += Adorno(centro, marco.normal, mundo + pisadas) { camara ->
                val path = camino(mundo.map { camara.proyectar(it) })
                drawPath(path, brillo)
                if (ab.ribete != null) dibujarRibete(this, camara, ab, marco, centro)
                else drawPath(path, borde, style = Stroke(width = camara.tamanoEnPantalla(centro, 0.22f)))
                if (ab.parteluz) {
                    // Cruz de madera de las ventanas de taberna.
                    val medio = (ab.u0 + ab.u1) / 2f
                    val ancho = camara.tamanoEnPantalla(centro, 0.3f)
                    val madera = lerp(pared, Color.Black, 0.25f)
                    val alto = ab.y1 + ab.arco
                    drawLine(madera, camara.proyectar(marco.aMundo(medio, ab.y0)), camara.proyectar(marco.aMundo(medio, alto)), ancho)
                    val ym = (ab.y0 + ab.y1) / 2f
                    drawLine(madera, camara.proyectar(marco.aMundo(ab.u0, ym)), camara.proyectar(marco.aMundo(ab.u1, ym)), ancho)
                }
            }
            }
            is Abertura.Puerta -> {
                // La puerta cruza varias caras: cada una dibuja sólo el tramo de marco que le toca.
                val u0 = cara.minOf { (it - marco.centro) dot marco.h }
                val u1 = cara.maxOf { (it - marco.centro) dot marco.h }
                val enCara = marco.aMundo((maxOf(hueco.u0, u0) + minOf(hueco.u1, u1)) / 2f, ab.yArranque)
                adornos += Adorno(enCara, marco.normal) { camara ->
                    clipPath(camino(cara.map { camara.proyectar(it) })) {
                        drawPath(camino(mundo.map { camara.proyectar(it) }), borde, style = Stroke(width = camara.tamanoEnPantalla(enCara, 0.3f)))
                    }
                }
            }
        }
    }

    /**
     * Ribete de cuero: una tira oscura alrededor del hueco con doble pespunte dorado (uno sobre la
     * tira y otro por fuera, sobre el cuero de la caja) y una sombra fina en el canto del corte.
     */
    private fun dibujarRibete(scope: DrawScope, camara: Camara, ab: Abertura.Ventana, marco: MarcoCara, centro: Vec3) = with(scope) {
        fun contorno(d: Float) = camino(
            marco.ventana(ab.u0 - d, ab.u1 + d, ab.y0 - d, ab.y1, ab.arco + d, ab.apuntado).map { (u, y) -> camara.proyectar(marco.aMundo(u, y)) }
        )
        val ancho = 0.6f
        val tira = luz.iluminar(Color(ab.ribete!!), centro, marco.normal)
        // El hilo dorado brilla algo más que el cuero: se nota aun con la luz tenue de la caja.
        val dorado = Color(ab.costura ?: 0xFFD8B46AL)
        val hilo = lerp(luz.iluminar(dorado, centro, marco.normal), dorado, 0.55f)
        val puntada = PathEffect.dashPathEffect(floatArrayOf(camara.tamanoEnPantalla(centro, 0.32f), camara.tamanoEnPantalla(centro, 0.2f)))
        drawPath(contorno(ancho / 2f), tira, style = Stroke(width = camara.tamanoEnPantalla(centro, ancho)))
        drawPath(contorno(ancho + 0.04f), lerp(tira, Color.Black, 0.5f), style = Stroke(width = camara.tamanoEnPantalla(centro, 0.06f)))
        drawPath(contorno(0.02f), Color.Black.copy(alpha = 0.6f), style = Stroke(width = camara.tamanoEnPantalla(centro, 0.1f)))
        for (d in listOf(ancho * 0.5f, ancho + 0.3f)) {
            drawPath(contorno(d), hilo, style = Stroke(width = camara.tamanoEnPantalla(centro, 0.07f), pathEffect = puntada))
        }
    }

    // ---------- Piso de la bandeja ----------

    private fun construirPiso() {
        val (x0, z0, x1, z1) = modelo.piso
        val esq = listOf(Vec3(x0, 0f, z1), Vec3(x1, 0f, z1), Vec3(x1, 0f, z0), Vec3(x0, 0f, z0))
        val base = Color(modelo.colorPiso)
        val juntas = Color(modelo.detallePiso)
        val acabado = modelo.acabadoPiso
        val (tamU, tamV) = when (acabado) {
            Acabado.TABLONES -> 7f to 2.4f
            Acabado.PIEDRA -> 3.4f to 2.8f
            else -> 1.6f to 1.6f
        }
        val eu = esq[1] - esq[0]
        val ev = esq[3] - esq[0]
        val nu = max(1, ceil(eu.longitud / tamU).toInt())
        val nv = max(1, ceil(ev.longitud / tamV).toInt())
        val trabado = acabado != Acabado.LISO
        for (j in 0 until nv) {
            val b0 = j / nv.toFloat()
            val b1 = (j + 1) / nv.toFloat()
            val cortes = if (trabado && j % 2 == 1) (0..nu + 1).map { ((it - 0.5f) / nu).coerceIn(0f, 1f) }.distinct()
            else (0..nu).map { it / nu.toFloat() }
            for (i in 0 until cortes.size - 1) {
                val e = listOf(cortes[i] to b0, cortes[i + 1] to b0, cortes[i + 1] to b1, cortes[i] to b1).map { (a, b) -> esq[0] + eu * a + ev * b }
                val centro = e.reduce { a, b -> a + b } * 0.25f
                val color = variar(base, acabado, i, j, nu, centro, null)
                val veta = if (acabado == Acabado.TABLONES) {
                    val k = ((i * 37 + j * 53) % 7) / 7f
                    val a = e[0] + (e[3] - e[0]) * (0.3f + 0.4f * k) + (e[1] - e[0]) * 0.06f
                    a to (a + (e[1] - e[0]) * (0.5f + 0.4f * k))
                } else null
                piso += Tesela(e, centro, Vec3.ARRIBA, luz.iluminar(color, centro, Vec3.ARRIBA),
                    if (trabado) juntas else null, 1f, veta = veta,
                    colores = IntArray(4) { k -> luz.iluminar(color, e[k], Vec3.ARRIBA).toArgb() })
            }
        }
    }

    /** Fondo con su degradado y resplandores, y el piso iluminado (va debajo de todo). */
    fun dibujarFondoYPiso(scope: DrawScope, camara: Camara) = with(scope) {
        val amb = modelo.ambiente
        drawRect(Brush.verticalGradient(listOf(Color(amb.fondoArriba), Color(amb.fondoAbajo))))
        for (r in amb.resplandores) {
            val c = Offset(size.width * r.x, size.height * r.y)
            val radio = r.radio * max(size.width, size.height)
            drawCircle(Brush.radialGradient(listOf(Color(r.color), Color.Transparent), c, radio), radio, c)
        }
        for (t in piso) dibujarTesela(t, camara.let { cam -> t.esquinas.map { cam.proyectar(it) } }, camara)
    }

    /** Viñeta: oscurece apenas los bordes (sólo en la vista general). */
    fun dibujarVineta(scope: DrawScope, intensidad: Float) = with(scope) {
        if (intensidad <= 0f) return@with
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f * intensidad)),
                center = Offset(size.width / 2f, size.height * 0.55f), radius = max(size.width, size.height) * 0.75f
            )
        )
    }

    private fun DrawScope.dibujarTesela(t: Tesela, pts: List<Offset>, camara: Camara) {
        var path = camino(pts)
        for (h in t.huecos) {
            val resta = Path()
            resta.op(path, camino(h.map { camara.proyectar(it) }), PathOperation.Difference)
            path = resta
        }
        val suave = t.colores != null && t.huecos.isEmpty() && t.alfa >= 1f && Build.VERSION.SDK_INT >= 29
        if (suave) {
            // Colores por vértice: la luz de velas y antorchas se degrada sin escalones.
            val v = FloatArray(8) { k -> if (k % 2 == 0) pts[k / 2].x else pts[k / 2].y }
            drawContext.canvas.nativeCanvas.drawVertices(
                android.graphics.Canvas.VertexMode.TRIANGLE_FAN, 8, v, 0, null, 0, t.colores, 0, null, 0, 0, pinturaVertices
            )
        } else drawPath(path, t.color.copy(alpha = t.alfa))
        if (t.veta != null) drawLine(
            lerp(t.color, Color.Black, 0.35f).copy(alpha = 0.6f),
            camara.proyectar(t.veta.first), camara.proyectar(t.veta.second), 1.2f
        )
        if (t.juntas != null) drawPath(path, t.juntas.copy(alpha = 0.6f * t.alfa), style = Stroke(width = 1.1f))
        // Superficies lisas: un trazo del mismo color tapa la junta entre baldosas vecinas.
        else if (t.alfa >= 1f && !suave) drawPath(path, t.color, style = Stroke(width = 1.5f))
    }

    fun dibujables(camara: Camara): List<Dibujable> {
        val lista = ArrayList<Dibujable>(teselas.size + contornos.size + adornos.size + 4)
        for (t in teselas) {
            if (((camara.ojo - t.centro) dot t.normal) <= 0f) continue
            val pts = t.esquinas.map { camara.proyectar(it) }
            lista += Dibujable(camara.profundidad(t.centro) + if (t.interior) 1.5f else 0f) { dibujarTesela(t, pts, camara) }
        }
        for (c in contornos) {
            if (((camara.ojo - c.centro) dot c.normal) <= 0f) continue
            val pts = c.esquinas.map { camara.proyectar(it) }
            // Cada arista en tramos cortos, un poco por delante de su cara: así no la tapan las
            // baldosas de la misma cara que quedan más cerca de la cámara.
            val efecto = if (c.punteado) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null
            for (k in c.esquinas.indices) {
                val a = c.esquinas[k]
                val b = c.esquinas[(k + 1) % c.esquinas.size]
                val tramos = max(1, ceil((b - a).longitud / 1.5f).toInt())
                for (m in 0 until tramos) {
                    val p0 = a + (b - a) * (m / tramos.toFloat())
                    val p1 = a + (b - a) * ((m + 1) / tramos.toFloat())
                    val o0 = camara.proyectar(p0)
                    val o1 = camara.proyectar(p1)
                    lista += Dibujable(camara.profundidad((p0 + p1) * 0.5f) - 0.05f) {
                        drawLine(c.color, o0, o1, strokeWidth = if (c.punteado) 1.6f else 1.2f, pathEffect = efecto)
                    }
                }
            }
        }
        for (a in adornos) {
            if (((camara.ojo - a.centro) dot a.normal) <= 0f) continue
            lista += Dibujable(a.puntos.minOf { camara.profundidad(it) } - 0.08f) { a.dibujar(this, camara) }
        }
        for (l in modelo.ambiente.luces) {
            if (!l.visible) continue
            lista += Dibujable(camara.profundidad(l.posicion) - 0.1f) { dibujarLlama(camara, l.posicion, Color(l.color)) }
        }
        return lista
    }

    /** Llama de vela o antorcha: halo de luz y una lágrima de fuego con el centro claro. */
    private fun DrawScope.dibujarLlama(camara: Camara, p: Vec3, color: Color) {
        val c = camara.proyectar(p)
        val halo = camara.tamanoEnPantalla(p, 2.6f)
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), c, halo), halo, c)
        val alto = camara.tamanoEnPantalla(p, 0.75f)
        val ancho = camara.tamanoEnPantalla(p, 0.28f)
        fun lagrima(escala: Float) = Path().apply {
            moveTo(c.x, c.y - alto * escala)
            quadraticTo(c.x + ancho * 1.4f * escala, c.y, c.x, c.y + alto * 0.45f * escala)
            quadraticTo(c.x - ancho * 1.4f * escala, c.y, c.x, c.y - alto * escala)
            close()
        }
        drawPath(lagrima(1f), color)
        drawPath(lagrima(0.55f), lerp(color, Color.White, 0.75f))
    }
}

private val pinturaVertices = android.graphics.Paint()

private fun camino(pts: List<Offset>) = Path().apply {
    pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
    close()
}
