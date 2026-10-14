package com.rolesencia.diceroller.tirada

import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.fisica.ImpulsoLanzamiento
import com.rolesencia.diceroller.fisica.LectorCara
import com.rolesencia.diceroller.fisica.Poliedros
import com.rolesencia.diceroller.fisica.Quat
import com.rolesencia.diceroller.fisica.Vec3
import com.rolesencia.diceroller.tirada.torre.ModeloTorre
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Arma las condiciones iniciales de cada dado: "la mano" que los suelta. */
object GeneradorLanzamiento {

    private const val SEPARACION = 3.0f

    /**
     * Los dados salen agrupados desde el borde cercano de la mesa, por encima del tapete, hacia
     * el fondo. Con [gesto] la dirección y la fuerza vienen del deslizamiento del dedo.
     */
    fun impulsos(dados: List<DadoEnMesa>, mesa: ConfigMesa, gesto: Gesto?, random: Random): List<ImpulsoLanzamiento> {
        val columnas = ceil(sqrt(dados.size.toFloat())).toInt().coerceIn(1, 5)
        val (dirX, dirZ) = if (gesto != null) {
            val n = sqrt(gesto.dirX * gesto.dirX + gesto.dirZ * gesto.dirZ).coerceAtLeast(1e-3f)
            gesto.dirX / n to gesto.dirZ / n
        } else {
            (random.nextFloat() - 0.5f) * 0.7f to -1f
        }
        val fuerza = gesto?.fuerza ?: (0.45f + random.nextFloat() * 0.4f)
        val rapidez = 60f + 140f * fuerza.coerceIn(0f, 1f)
        val baseZ = mesa.profundidad / 2f - 3f
        return dados.mapIndexed { i, _ ->
            val col = i % columnas
            val fila = i / columnas
            val posicion = Vec3(
                (col - (columnas - 1) / 2f) * SEPARACION + ruido(random, 0.3f),
                5f + (fila % 2) * SEPARACION + ruido(random, 0.3f),
                baseZ - (fila / 2) * SEPARACION
            )
            ImpulsoLanzamiento(
                posicion = posicion,
                orientacion = orientacionAleatoria(random),
                velocidadLineal = Vec3(
                    dirX * rapidez + ruido(random, 25f),
                    30f + random.nextFloat() * 50f,
                    dirZ * rapidez + ruido(random, 25f)
                ),
                velocidadAngular = Vec3(ruido(random, 30f), ruido(random, 30f), ruido(random, 30f))
            )
        }
    }

    /** Segundos entre un dado y el siguiente al verterlos en la torre. */
    const val INTERVALO_VERTIDO = 0.12f

    /**
     * Torre de dados: los dados se vierten **de a uno**, alternando dos puntos sobre el primer
     * escalón, cada uno [INTERVALO_VERTIDO] segundos después del anterior, como cuando se vuelcan
     * de la mano. Así no llegan todos juntos a la salida y no se forma un tapón.
     *
     * [alturaEntrada] es desde dónde se sueltan: la mesa 3D la calcula para que el dado arranque
     * **por encima del borde de la pantalla** y se lo vea entrar cayendo, no aparecer de golpe.
     * Nunca es menor que [ModeloTorre.alturaCaida] (la boca de la torre).
     */
    fun impulsosTorre(
        dados: List<DadoEnMesa>, modelo: ModeloTorre, random: Random, alturaEntrada: Float = modelo.alturaCaida
    ): List<ImpulsoLanzamiento> {
        val puntos = puntosDeCaida(modelo)
        val altura = alturaEntrada.coerceIn(modelo.alturaCaida, modelo.altoTecho - 3f)
        return dados.mapIndexed { i, _ ->
            val p = puntos[i % 2]
            ImpulsoLanzamiento(
                posicion = Vec3(p.x + ruido(random, 0.3f), altura + ruido(random, 0.3f), p.z + ruido(random, 0.3f)),
                orientacion = orientacionAleatoria(random),
                velocidadLineal = Vec3(ruido(random, 8f), -15f - random.nextFloat() * 15f, ruido(random, 5f)),
                velocidadAngular = Vec3(ruido(random, 20f), ruido(random, 20f), ruido(random, 20f)),
                retardo = i * INTERVALO_VERTIDO + random.nextFloat() * 0.03f
            )
        }
    }

    /** Los dos puntos (x, z) sobre la boca de la torre donde se alternan los dados al verterlos. */
    fun puntosDeCaida(modelo: ModeloTorre): List<Vec3> {
        val z = modelo.zonaCaida
        return listOf(Vec3(z[0], 0f, z[1]), Vec3(z[0] + 1.6f, 0f, z[3]))
    }

    /** Cómo se ven los dados antes de lanzar: en fila sobre el borde cercano, con el máximo arriba. */
    fun disposicionInicial(dados: List<DadoEnMesa>, mesa: ConfigMesa, zona: FloatArray? = null): List<DadoVisual> {
        if (zona != null) return disposicionEnZona(dados, zona)
        val porFila = ((mesa.ancho - 2f) / SEPARACION).toInt().coerceAtLeast(1)
        return dados.mapIndexed { i, dado ->
            val col = i % porFila
            val fila = i / porFila
            val enFila = minOf(porFila, dados.size - fila * porFila)
            val tipo = dado.tipo
            val indiceMax = tipo.valoresCaras.indexOf(tipo.maximo)
            val orientacion = LectorCara.orientacionParaCara(tipo, indiceMax)
            val altura = -Poliedros.de(tipo).vertices.minOf { orientacion.rotar(it).y }
            DadoVisual(
                dado,
                Vec3((col - (enFila - 1) / 2f) * SEPARACION, altura, mesa.profundidad / 2f - 3f - fila * SEPARACION),
                orientacion,
                enReposo = true
            )
        }
    }

    /** Con torre: los dados esperan en una grilla centrada en la zona de la bandeja (x0, z0, x1, z1). */
    private fun disposicionEnZona(dados: List<DadoEnMesa>, zona: FloatArray): List<DadoVisual> {
        val porFila = ((zona[2] - zona[0]) / SEPARACION).toInt().coerceAtLeast(1) + 1
        val filas = (dados.size + porFila - 1) / porFila
        val centroZ = (zona[1] + zona[3]) / 2f
        return dados.mapIndexed { i, dado ->
            val fila = i / porFila
            val enFila = minOf(porFila, dados.size - fila * porFila)
            val tipo = dado.tipo
            val orientacion = LectorCara.orientacionParaCara(tipo, tipo.valoresCaras.indexOf(tipo.maximo))
            val altura = -Poliedros.de(tipo).vertices.minOf { orientacion.rotar(it).y }
            DadoVisual(
                dado,
                Vec3(((i % porFila) - (enFila - 1) / 2f) * SEPARACION, altura, centroZ + (fila - (filas - 1) / 2f) * SEPARACION),
                orientacion, enReposo = true
            )
        }
    }

    /** Rotación uniforme al azar (método de Shoemake). */
    fun orientacionAleatoria(random: Random): Quat {
        val u1 = random.nextFloat()
        val u2 = random.nextFloat() * 2f * PI.toFloat()
        val u3 = random.nextFloat() * 2f * PI.toFloat()
        val a = sqrt(1f - u1)
        val b = sqrt(u1)
        return Quat(a * sin(u2), a * cos(u2), b * sin(u3), b * cos(u3))
    }

    private fun ruido(random: Random, escala: Float) = (random.nextFloat() - 0.5f) * 2f * escala
}
