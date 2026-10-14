package com.rolesencia.diceroller.ui.mesa3d

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.EstiloSkin
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.apariencia.Tipografia
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.sistemas.SimboloLegendario
import com.rolesencia.diceroller.fisica.Cara
import com.rolesencia.diceroller.fisica.Poliedros
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.tirada.DadoVisual
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Dibuja un dado en 3D con su skin: cara por cara, de las visibles (o también las de atrás, si es
 * una gema translúcida), con el estilo, la iluminación y la tipografía de la skin.
 */
class PintorDados(private val simbolos: SimbolosLegendarios? = null) {
    private val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = 70f }
    private val pinturaImagen = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matriz = Matrix()
    private val origen = floatArrayOf(-50f, -50f, 50f, -50f, 50f, 50f, -50f, 50f)
    private val destino = FloatArray(8)

    fun dibujarSombra(scope: DrawScope, camara: Camara, dado: DadoVisual) {
        val radio = Poliedros.de(dado.dado.tipo).radio
        val altura = dado.posicion.y
        val piso = Vec3(dado.posicion.x + altura * 0.25f, 0f, dado.posicion.z - altura * 0.2f)
        val centro = camara.proyectar(piso)
        // Vista cenital (resultado): la sombra se ve redonda y casi toda tapada por el dado.
        val cenital = camara.cenital
        val r = camara.tamanoEnPantalla(piso, radio * (1.05f - 0.25f * cenital + altura * 0.04f))
        val aplastado = 0.75f + 0.25f * cenital
        val alfa = (0.45f * (1f - altura / 25f)).coerceIn(0.05f, 0.45f) *
            if (dado.dado.skin.estilo == EstiloSkin.GEMA) 0.6f else 1f
        scope.drawOval(Color.Black.copy(alpha = alfa), topLeft = Offset(centro.x - r, centro.y - r * aplastado), size = Size(2 * r, 2f * aplastado * r))
    }

    fun dibujarDado(scope: DrawScope, camara: Camara, dado: DadoVisual, destacado: Boolean, acento: Color = Color(0xFFFFD700)) {
        val tipo = dado.dado.tipo
        val legendario = tipo == TipoDado.LEGENDARIO
        val skin = dado.dado.skin
        val estilo = if (legendario) EstiloSkin.SOLIDO else skin.estilo
        val poli = Poliedros.de(tipo)
        val q = dado.orientacion
        val mundo = poli.vertices.map { dado.posicion + q.rotar(it) }
        val pantalla = mundo.map { camara.proyectar(it) }
        val especial = dado.dado.rol == RolDado.ESPECIAL
        val borde = if (destacado) acento else colorBorde(dado.dado).copy(alpha = if (especial) 1f else 0.85f)
        val anchoBorde = if (destacado) 3f else if (especial) 2.4f else 1.3f

        fun camino(cara: Cara) = Path().apply {
            cara.vertices.forEachIndexed { k, v -> if (k == 0) moveTo(pantalla[v].x, pantalla[v].y) else lineTo(pantalla[v].x, pantalla[v].y) }
            close()
        }
        fun colorBase(i: Int, cara: Cara): Long =
            if (legendario) CatalogoSkins.colorPorTipo(TipoDado.LEGENDARIO) else skin.colorCara(tipo, i, cara.normal.y)

        // Gema: primero las caras de atrás, oscuras y translúcidas: se ven a través del dado.
        if (estilo == EstiloSkin.GEMA) {
            poli.caras.forEachIndexed { i, cara ->
                val normal = q.rotar(cara.normal)
                val centro = dado.posicion + q.rotar(cara.centro)
                if (((camara.ojo - centro) dot normal) > 0f) return@forEachIndexed
                val path = camino(cara)
                scope.drawPath(path, lerp(Color.Black, Color(colorBase(i, cara)), 0.4f).copy(alpha = 0.6f))
                scope.drawPath(path, borde.copy(alpha = 0.3f), style = Stroke(width = 1f))
            }
        }

        poli.caras.forEachIndexed { i, cara ->
            val normal = q.rotar(cara.normal)
            val centro = dado.posicion + q.rotar(cara.centro)
            if (((camara.ojo - centro) dot normal) <= 0f) return@forEachIndexed   // cara de espaldas
            dibujarCara(scope, camara, dado, poli.radio, poli.inradio, cara, i, normal, centro, mundo, pantalla,
                camino(cara), colorBase(i, cara), estilo, legendario, borde, anchoBorde, destacado)
        }
    }

    private fun dibujarCara(
        scope: DrawScope, camara: Camara, dado: DadoVisual, radioDado: Float, inradio: Float, cara: Cara, i: Int,
        normal: Vec3, centro: Vec3, mundo: List<Vec3>, pantalla: List<Offset>, path: Path, baseLong: Long,
        estilo: EstiloSkin, legendario: Boolean, borde: Color, anchoBorde: Float, destacado: Boolean
    ) {
        val tipo = dado.dado.tipo
        val skin = dado.dado.skin
        val vista = (camara.ojo - centro).normalizado()
        val medio = (LUZ + vista).normalizado()
        // En la toma cenital, las caras que miran arriba conservan el tono de la vista normal: se
        // apagan los reflejos (dependen del ángulo de la cámara, y mirando a plomo aclaraban la cara
        // hasta el gris). La luz difusa es fija al mundo y no cambia.
        val plana = camara.cenital * max(0f, normal.y).pow(4)
        val luz = Sombreado.luz(normal)
        val base = Color(baseLong)
        val arriba = direccionArriba(cara.vertices.map { mundo[it] }, centro)
        val derecha = (arriba cross normal).normalizado()
        val radioCara = cara.vertices.maxOf { (mundo[it] - centro).longitud }

        // 1. Relleno del cuerpo según el estilo.
        when (estilo) {
            EstiloSkin.DEGRADADO -> rellenarDegradado(scope, radioDado, cara, i, pantalla, luz, tipo, skin, path)
            EstiloSkin.IMAGEN -> rellenarImagen(scope, camara, skin.imagenes, i, path, centro, arriba, derecha, radioCara, base, luz)
            EstiloSkin.METALICO -> {
                // Metal: poca luz difusa y un reflejo especular fuerte que se desliza al rodar.
                scope.drawPath(path, Sombreado.metal(base, normal, vista, plana))
            }
            EstiloSkin.GEMA -> {
                scope.drawPath(path, lerp(Color.Black, base, luz).copy(alpha = 0.68f))
                val destello = Sombreado.destelloGema(normal, vista, plana)
                if (destello > 0.01f) scope.drawPath(path, Color.White.copy(alpha = destello))
            }
            EstiloSkin.BRILLO_NOCTURNO -> scope.drawPath(path, lerp(Color.Black, base, 0.6f + 0.4f * luz))
            else -> scope.drawPath(path, lerp(Color.Black, base, luz))
        }

        // 2. Efectos pegados a la cara: giran con el dado.
        val clave = "${skin.id}|${tipo.name}|$i"
        fun aPantalla(u: Float, v: Float) = camara.proyectar(centro + derecha * (u * radioCara) + arriba * (v * radioCara))
        if (estilo == EstiloSkin.MARMOL) {
            val veta = Color(skin.color2Para(tipo))
            scope.clipPath(path) {
                PatronesCara.vetas(clave).forEach { linea ->
                    val trazo = Path().apply {
                        linea.forEachIndexed { k, (u, v) -> aPantalla(u, v).let { if (k == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
                    }
                    drawPath(trazo, lerp(Color.Black, veta, luz).copy(alpha = 0.6f), style = Stroke(width = camara.tamanoEnPantalla(centro, 0.07f)))
                    drawPath(trazo, veta.copy(alpha = 0.35f), style = Stroke(width = camara.tamanoEnPantalla(centro, 0.025f)))
                }
            }
        }
        if (estilo == EstiloSkin.PURPURINA) {
            val brillo = Color(skin.color2Para(tipo))
            val giro = normal dot medio
            scope.clipPath(path) {
                PatronesCara.destellos(clave).forEach { (u, v, fase) ->
                    val intensidad = max(0f, sin(fase + giro * 10f)).pow(3)
                    drawCircle(
                        brillo.copy(alpha = 0.25f + 0.75f * intensidad),
                        radius = camara.tamanoEnPantalla(centro, 0.04f) * (0.6f + intensidad),
                        center = aPantalla(u, v)
                    )
                }
            }
        }

        // 3. Aristas (en el brillo nocturno, con resplandor).
        if (estilo == EstiloSkin.BRILLO_NOCTURNO && !destacado) {
            val resplandor = Color(skin.color2Para(tipo))
            scope.drawPath(path, resplandor.copy(alpha = 0.35f), style = Stroke(width = 5f))
            scope.drawPath(path, resplandor, style = Stroke(width = 1.4f))
        } else {
            scope.drawPath(path, borde, style = Stroke(width = anchoBorde))
        }

        // 4. Números o símbolos.
        val tintaBase = when {
            legendario -> Color(0xFF2B1D05)
            estilo == EstiloSkin.IMAGEN -> Color.White
            else -> Color(skin.tintaEfectiva(baseLong))
        }
        val tinta = if (estilo == EstiloSkin.BRILLO_NOCTURNO) tintaBase else lerp(lerp(Color.Black, tintaBase, 0.7f), tintaBase, luz)
        val brilloTinta = if (estilo == EstiloSkin.BRILLO_NOCTURNO) Color(skin.color2Para(tipo)) else null
        val contorno = when {
            estilo == EstiloSkin.IMAGEN -> Color.Black
            !legendario && skin.necesitaContorno(baseLong) ->
                if (tintaBase.luminance() > 0.5f) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
            else -> null
        }
        val tipografia = if (legendario) Tipografia.CLASICA else skin.tipografia
        if (tipo == TipoDado.D4) {
            // El d4 lleva un número junto a cada vértice: el de la cara opuesta a ese vértice.
            cara.vertices.forEach { v ->
                val haciaVertice = mundo[v] - centro
                dibujarTexto(scope, camara, etiquetaCara(tipo, v, tipografia), centro + haciaVertice * 0.5f, haciaVertice.normalizado(),
                    normal, inradio * 0.42f, tinta, tipografia, brilloTinta, contorno)
            }
            return
        }
        val medioArista = (mundo[cara.vertices[0]] + mundo[cara.vertices[1]]) * 0.5f
        val radioInterno = (medioArista - centro).longitud
        if (legendario && simbolos != null) {
            val simbolo = SimboloLegendario.de(tipo.valoresCaras[i])
            val tono = lerp(Color.Black, SimbolosLegendarios.color(simbolo), 0.35f + 0.65f * luz)
            dibujarSimbolo(scope, camara, simbolos.drawable(simbolo), centro, arriba, normal, radioInterno * 0.78f, tono)
            return
        }
        val texto = etiquetaCara(tipo, i, tipografia)
        if (texto.isEmpty()) return
        val escala = radioInterno * when (cara.vertices.size) { 3 -> 0.75f; 4 -> 0.62f; else -> 0.6f }
        dibujarTexto(scope, camara, texto, centro, arriba, normal, escala, tinta, tipografia, brilloTinta, contorno)
    }

    /** Degradado fijo al dado: del color de arriba (y local alta) al de abajo, iluminado. */
    private fun rellenarDegradado(
        scope: DrawScope, radio: Float, cara: Cara, i: Int, pantalla: List<Offset>, luz: Float, tipo: TipoDado, skin: SkinDado, path: Path
    ) {
        val poli = Poliedros.de(tipo)
        val alto = cara.vertices.maxBy { poli.vertices[it].y }
        val bajo = cara.vertices.minBy { poli.vertices[it].y }
        val cAlto = lerp(Color.Black, Color(skin.colorCara(tipo, i, poli.vertices[alto].y / radio)), luz)
        val cBajo = lerp(Color.Black, Color(skin.colorCara(tipo, i, poli.vertices[bajo].y / radio)), luz)
        if ((pantalla[alto] - pantalla[bajo]).getDistance() < 1f) {
            scope.drawPath(path, cAlto)
            return
        }
        scope.drawPath(path, Brush.linearGradient(listOf(cAlto, cBajo), start = pantalla[alto], end = pantalla[bajo]))
    }

    /** Imagen del usuario proyectada sobre la cara (recortada a su forma) y sombreada como el resto. */
    private fun rellenarImagen(
        scope: DrawScope, camara: Camara, imagenes: List<String>, i: Int, path: Path,
        centro: Vec3, arriba: Vec3, derecha: Vec3, radio: Float, base: Color, luz: Float
    ) {
        val bmp = imagenes.takeIf { it.isNotEmpty() }?.let { ImagenesSkins.bitmap(it[i % it.size]) }
        if (bmp == null) {
            scope.drawPath(path, lerp(Color.Black, base, luz))
            return
        }
        val esquinas = listOf(
            centro - derecha * radio + arriba * radio, centro + derecha * radio + arriba * radio,
            centro + derecha * radio - arriba * radio, centro - derecha * radio - arriba * radio
        )
        esquinas.forEachIndexed { k, p -> camara.proyectar(p).let { destino[2 * k] = it.x; destino[2 * k + 1] = it.y } }
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val matrizImagen = Matrix()
        if (!matrizImagen.setPolyToPoly(floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h), 0, destino, 0, 4)) return
        scope.drawIntoCanvas { canvas ->
            val nativo = canvas.nativeCanvas
            nativo.save()
            nativo.clipPath(path.asAndroidPath())
            nativo.drawBitmap(bmp, matrizImagen, pinturaImagen)
            nativo.restore()
        }
        scope.drawPath(path, Color.Black.copy(alpha = (1f - luz) * 0.75f))
    }

    /** Triángulos: hacia un vértice. Cometas (d10): hacia el polo. Cuadrados y pentágonos: hacia una arista. */
    private fun direccionArriba(vertices: List<Vec3>, centro: Vec3): Vec3 = when (vertices.size) {
        3 -> vertices[0] - centro
        4 -> {
            val lejano = vertices.maxBy { (it - centro).longitud }
            val distancias = vertices.map { (it - centro).longitud }.sorted()
            if (distancias.last() - distancias.first() > 0.05f) lejano - centro       // cometa
            else (vertices[0] + vertices[1]) * 0.5f - centro                           // cuadrado
        }
        else -> (vertices[0] + vertices[1]) * 0.5f - centro
    }.normalizado()

    /** Proyecta un cuadrado del plano de la cara y arma la matriz que lleva (-50..50)² a ese cuadrado. */
    private fun prepararMatriz(camara: Camara, centro: Vec3, arriba: Vec3, normal: Vec3, s: Float): Boolean {
        val derecha = (arriba cross normal).normalizado()
        val esquinas = listOf(
            centro - derecha * s + arriba * s, centro + derecha * s + arriba * s,
            centro + derecha * s - arriba * s, centro - derecha * s - arriba * s
        )
        esquinas.forEachIndexed { k, p ->
            val o = camara.proyectar(p)
            destino[2 * k] = o.x
            destino[2 * k + 1] = o.y
        }
        return matriz.setPolyToPoly(origen, 0, destino, 0, 4)
    }

    /** Texto deformado con la perspectiva de la cara; con resplandor o contorno si la skin lo pide. */
    private fun dibujarTexto(
        scope: DrawScope, camara: Camara, texto: String, centro: Vec3, arriba: Vec3, normal: Vec3, s: Float, color: Color,
        tipografia: Tipografia, brillo: Color?, contorno: Color?
    ) {
        if (!prepararMatriz(camara, centro, arriba, normal, s)) return
        pintura.typeface = Fuentes.de(tipografia)
        pintura.textSize = when {
            texto.length >= 4 -> 38f
            texto.length == 3 -> 50f
            else -> 70f
        }
        scope.drawIntoCanvas { canvas ->
            val nativo = canvas.nativeCanvas
            nativo.save()
            nativo.concat(matriz)
            if (contorno != null) {
                pintura.style = Paint.Style.STROKE
                pintura.strokeWidth = 9f
                pintura.color = contorno.toArgb()
                nativo.drawText(texto, 0f, 25f, pintura)
                pintura.style = Paint.Style.FILL
            }
            pintura.color = color.toArgb()
            if (brillo != null) pintura.setShadowLayer(16f, 0f, 0f, brillo.toArgb())
            nativo.drawText(texto, 0f, 25f, pintura)
            pintura.clearShadowLayer()
            nativo.restore()
        }
    }

    /** Símbolo vectorial deformado con la perspectiva de la cara, conservando su proporción. */
    private fun dibujarSimbolo(scope: DrawScope, camara: Camara, simbolo: Drawable, centro: Vec3, arriba: Vec3, normal: Vec3, s: Float, color: Color) {
        if (!prepararMatriz(camara, centro, arriba, normal, s)) return
        val ancho = simbolo.intrinsicWidth.toFloat().coerceAtLeast(1f)
        val alto = simbolo.intrinsicHeight.toFloat().coerceAtLeast(1f)
        val k = 80f / maxOf(ancho, alto)
        simbolo.setBounds((-ancho * k / 2).toInt(), (-alto * k / 2).toInt(), (ancho * k / 2).toInt(), (alto * k / 2).toInt())
        simbolo.setTint(color.toArgb())
        scope.drawIntoCanvas { canvas ->
            val nativo = canvas.nativeCanvas
            nativo.save()
            nativo.concat(matriz)
            simbolo.draw(nativo)
            nativo.restore()
        }
    }
}
