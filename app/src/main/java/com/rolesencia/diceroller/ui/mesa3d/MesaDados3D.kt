package com.rolesencia.diceroller.ui.mesa3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.rolesencia.diceroller.fisica.Poliedros
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.tirada.torre.ModeloTorre
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ConfigMesa
import com.rolesencia.diceroller.tirada.DadoVisual
import com.rolesencia.diceroller.tirada.FrameMesa
import com.rolesencia.diceroller.tirada.Gesto
import com.rolesencia.diceroller.tirada.SesionTirada
import com.rolesencia.diceroller.ui.theme.Ivory
import kotlinx.coroutines.isActive
import kotlin.math.hypot

private const val ALTO_PARED = 3.5f

/**
 * Bandeja 3D. Es "tonta" a propósito: no sabe de modos ni de reglas.
 * Si hay [sesion], la avanza en cada cuadro con el tiempo real y dibuja lo que devuelve;
 * si no, dibuja [dadosQuietos]. Deslizar el dedo sobre la mesa lanza con esa dirección y fuerza.
 */
@Composable
fun MesaDados3D(
    mesa: ConfigMesa,
    material: MaterialMesa,
    sesion: SesionTirada?,
    dadosQuietos: List<DadoVisual>,
    destacados: Set<Int>,
    preparando: Boolean,
    puedeLanzar: Boolean,
    onCuadro: (FrameMesa, Long) -> Unit,
    onSesionTerminada: (SesionTirada) -> Unit,
    onGesto: (Gesto) -> Unit,
    onProporcion: (Float) -> Unit,
    modifier: Modifier = Modifier,
    /** Torre de dados; null = bandeja sola. */
    modelo: ModeloTorre? = null
) {
    var dibujo by remember { mutableStateOf(dadosQuietos) }
    val escena = remember(modelo) { modelo?.let { EscenaTorre(it) } }
    val cuadro by rememberUpdatedState(onCuadro)
    val terminada by rememberUpdatedState(onSesionTerminada)
    val contexto = LocalContext.current
    val pintor = remember { PintorDados(SimbolosLegendarios(contexto)) }

    LaunchedEffect(dadosQuietos, sesion) { if (sesion == null) dibujo = dadosQuietos }

    // Bucle de animación: un paso de sesión por cuadro de pantalla (vsync).
    LaunchedEffect(sesion) {
        val actual = sesion ?: return@LaunchedEffect
        var anterior = withFrameNanos { it }
        while (isActive && !actual.terminada) {
            withFrameNanos { ahora ->
                val frame = actual.avanzar((ahora - anterior) / 1e9f)
                anterior = ahora
                dibujo = frame.dados
                cuadro(frame, ahora)
            }
        }
        if (isActive) terminada(actual)
    }

    val lanzarConGesto by rememberUpdatedState(onGesto)
    val habilitado by rememberUpdatedState(puedeLanzar)

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { if (it.width > 0) onProporcion(it.height.toFloat() / it.width) }
                .pointerInput(Unit) {
                    // Se capturan los toques en la pasada inicial: la mesa se queda con el gesto
                    // y la página no se desplaza mientras se "agita" la mano sobre ella.
                    awaitEachGesture {
                        val inicio = awaitFirstDown(requireUnconsumed = false)
                        val rastreador = VelocityTracker()
                        var ultima = inicio.position
                        rastreador.addPosition(inicio.uptimeMillis, inicio.position)
                        inicio.consume()
                        while (true) {
                            val evento = awaitPointerEvent(PointerEventPass.Initial)
                            val cambio = evento.changes.firstOrNull { it.id == inicio.id } ?: break
                            rastreador.addPosition(cambio.uptimeMillis, cambio.position)
                            ultima = cambio.position
                            cambio.consume()
                            if (!cambio.pressed) break
                        }
                        val v = rastreador.calculateVelocity()
                        val rapidez = hypot(v.x, v.y)
                        val recorrido = (ultima - inicio.position).getDistance()
                        // Un toque o un roce corto no lanza: hace falta un deslizamiento decidido.
                        if (habilitado && rapidez > 800f && recorrido > 80.dp.toPx()) {
                            lanzarConGesto(Gesto(v.x, v.y, (rapidez / 4000f).coerceIn(0.15f, 1f)))
                        }
                    }
                }
        ) {
            val mx = mesa.ancho / 2f
            val mz = mesa.profundidad / 2f
            if (escena != null) {
                dibujarConTorre(escena, camara = Camara(Vec3(0f, 30f, mz + 26f), Vec3(0f, 8f, -mz * 0.3f)), mx, mz, dibujo, destacados, pintor)
                return@Canvas
            }
            val camara = Camara(Vec3(0f, 40f, mesa.profundidad * 0.6f), Vec3(0f, 0f, 1f))
            camara.ajustar(
                listOf(Vec3(-mx, ALTO_PARED, -mz), Vec3(mx, ALTO_PARED, -mz), Vec3(-mx, 0f, mz), Vec3(mx, 0f, mz),
                    Vec3(-mx, ALTO_PARED, mz), Vec3(mx, ALTO_PARED, mz)),
                size.width, size.height, margen = 6f
            )
            dibujarBandeja(camara, mx, mz, material)
            val ordenados = dibujo.sortedByDescending { camara.profundidad(it.posicion) }
            ordenados.forEach { pintor.dibujarSombra(this, camara, it) }
            ordenados.forEach { pintor.dibujarDado(this, camara, it, it.dado.id in destacados) }
        }
        if (preparando) {
            Text(
                "Pre-simulando la tirada…",
                color = Ivory,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            )
        }
    }
}

private fun DrawScope.dibujarBandeja(camara: Camara, mx: Float, mz: Float, material: MaterialMesa) {
    fun quad(vararg p: Vec3) = Path().apply {
        p.forEachIndexed { i, v -> camara.proyectar(v).let { o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        close()
    }
    val piso = if (material == MaterialMesa.LONA) Color(0xFF1B4D3E) else Color(0xFF7A4E2D)
    val madera = Color(0xFF5D3A1A)
    drawPath(quad(Vec3(-mx, 0f, -mz), Vec3(mx, 0f, -mz), Vec3(mx, 0f, mz), Vec3(-mx, 0f, mz)), piso)
    if (material == MaterialMesa.MADERA) {
        // Vetas de la madera.
        for (k in 1 until 12) {
            val z = -mz + k * (2 * mz / 12f)
            drawLine(Color.Black.copy(alpha = 0.12f), camara.proyectar(Vec3(-mx, 0f, z)), camara.proyectar(Vec3(mx, 0f, z)), 2f)
        }
    }
    // Paredes interiores visibles (se descartan las que dan la espalda a la cámara).
    val paredes = listOf(
        Triple(Vec3(0f, 0f, 1f), Vec3(-mx, 0f, -mz), Vec3(mx, 0f, -mz)),
        Triple(Vec3(1f, 0f, 0f), Vec3(-mx, 0f, mz), Vec3(-mx, 0f, -mz)),
        Triple(Vec3(-1f, 0f, 0f), Vec3(mx, 0f, -mz), Vec3(mx, 0f, mz)),
        Triple(Vec3(0f, 0f, -1f), Vec3(mx, 0f, mz), Vec3(-mx, 0f, mz))
    )
    val alto = Vec3(0f, ALTO_PARED, 0f)
    paredes.forEach { (normal, a, b) ->
        if (((camara.ojo - a) dot normal) > 0f) {
            val luz = 0.6f + 0.4f * (normal dot Vec3(-0.35f, 0f, 0.45f).normalizado()).coerceAtLeast(0f)
            drawPath(quad(a, b, b + alto, a + alto), androidx.compose.ui.graphics.lerp(Color.Black, madera, luz))
        }
    }
    // Borde superior de la bandeja.
    drawPath(
        quad(Vec3(-mx, ALTO_PARED, -mz), Vec3(mx, ALTO_PARED, -mz), Vec3(mx, ALTO_PARED, mz), Vec3(-mx, ALTO_PARED, mz)),
        Color(0xFF8D6E4A), style = Stroke(width = 7f)
    )
}

/**
 * Vista de torre: cámara más baja y de frente para ver la torre de pie. Piso, sombras y después
 * baldosas de la torre y dados juntos, ordenados por profundidad (algoritmo del pintor).
 */
private fun DrawScope.dibujarConTorre(
    escena: EscenaTorre, camara: Camara, mx: Float, mz: Float,
    dados: List<DadoVisual>, destacados: Set<Int>, pintor: PintorDados
) {
    val piso = listOf(Vec3(-mx, 0f, -mz), Vec3(mx, 0f, -mz), Vec3(mx, 0f, mz), Vec3(-mx, 0f, mz))
    camara.ajustar(escena.vertices + piso, size.width, size.height, margen = 6f)
    val pts = piso.map { camara.proyectar(it) }
    drawPath(
        Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() },
        Color(escena.modelo.colorPiso)
    )
    dados.forEach { pintor.dibujarSombra(this, camara, it) }
    val lista = escena.dibujables(camara) + dados.map { d ->
        // Un poco hacia la cámara: un dado apoyado se dibuja después de la baldosa sobre la que está.
        Dibujable(camara.profundidad(d.posicion) - Poliedros.de(d.dado.tipo).radio * 0.6f) {
            pintor.dibujarDado(this, camara, d, d.dado.id in destacados)
        }
    }
    lista.sortedByDescending { it.profundidad }.forEach { it.dibujar(this) }
}
