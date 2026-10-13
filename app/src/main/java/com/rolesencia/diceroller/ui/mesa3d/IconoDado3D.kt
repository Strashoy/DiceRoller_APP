package com.rolesencia.diceroller.ui.mesa3d

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.fisica.LectorCara
import com.rolesencia.diceroller.fisica.Poliedros
import com.rolesencia.diceroller.fisica.Quat
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.tirada.DadoVisual

/**
 * El dado en 3D, quieto y en tres cuartos, con su número máximo arriba.
 * Usa la misma malla y el mismo pintor que la mesa: lo que se elige es lo que cae.
 */
@Composable
fun IconoDado3D(
    tipo: TipoDado,
    rol: RolDado,
    modifier: Modifier = Modifier,
    skin: SkinDado = CatalogoSkins.POR_TIPO
) {
    val contexto = LocalContext.current
    val pintor = remember { PintorDados(SimbolosLegendarios(contexto)) }
    val dado = remember(tipo, rol, skin) {
        DadoVisual(DadoEnMesa(-1, tipo, rol, skin), Vec3.CERO, orientacionIcono(tipo), enReposo = true)
    }
    val vertices = remember(tipo) { Poliedros.de(tipo).vertices.map { dado.orientacion.rotar(it) } }
    Canvas(modifier) {
        val camara = Camara(Vec3(0f, 6f, 5f), Vec3.CERO)
        camara.ajustar(vertices, size.width, size.height, margen = size.minDimension * 0.06f)
        pintor.dibujarDado(this, camara, dado, destacado = false)
    }
}

/** Máximo arriba (en el d4, el máximo en el vértice superior) y un giro de 30° para que se vean los lados. */
private fun orientacionIcono(tipo: TipoDado): Quat {
    val indiceMax = tipo.valoresCaras.indexOf(tipo.maximo)
    return (Quat.desdeEjeAngulo(Vec3.ARRIBA, 0.52f) * LectorCara.orientacionParaCara(tipo, indiceMax)).normalizado()
}
