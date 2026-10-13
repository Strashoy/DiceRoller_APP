package com.rolesencia.diceroller.fisica

import com.rolesencia.diceroller.domain.dados.TipoDado
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Una cara del poliedro, en espacio LOCAL del dado.
 * Plano de la cara: `normal · p = distancia`. [vertices] en orden antihorario visto desde afuera.
 */
class Cara(
    val vertices: IntArray,
    val normal: Vec3,
    val distancia: Float,
    val centro: Vec3
)

/**
 * Malla convexa de un dado: la usan el motor físico (colisiones), el render (dibujo) y la lectura
 * de caras. El índice de cada cara coincide con `TipoDado.valoresCaras`.
 */
/** [tipo] es null para las piezas del escenario (rampas, paredes de una torre). */
class Poliedro(val tipo: TipoDado?, val vertices: List<Vec3>, val caras: List<Cara>) {
    /** Radio de la esfera que envuelve al dado (para descartar choques lejanos). */
    val radio: Float = vertices.maxOf { it.longitud }
    /** Radio de la esfera inscrita (respaldo para choques arista-arista). */
    val inradio: Float = caras.minOf { it.distancia }

    /** Normales de cara sin repetir las paralelas (las caras opuestas dan el mismo eje). */
    val ejesCaras: List<Vec3> = caras.map { it.normal }.fold(mutableListOf<Vec3>()) { acc, n ->
        if (acc.none { kotlin.math.abs(it dot n) > 0.999f }) acc += n
        acc
    }

    /** Direcciones de arista sin repetir (para los ejes arista×arista del teorema del eje separador). */
    val direccionesAristas: List<Vec3> = caras.flatMap { cara ->
        cara.vertices.indices.map { k ->
            (vertices[cara.vertices[(k + 1) % cara.vertices.size]] - vertices[cara.vertices[k]]).normalizado()
        }
    }.fold(mutableListOf<Vec3>()) { acc, e ->
        if (acc.none { kotlin.math.abs(it dot e) > 0.999f }) acc += e
        acc
    }

    /** Caras vecinas (comparten una arista) de cada cara. */
    val vecinas: List<List<Int>> = caras.indices.map { i ->
        caras.indices.filter { j -> j != i && caras[i].vertices.count { it in caras[j].vertices } >= 2 }
    }
}

object Poliedros {

    private val PHI = ((1.0 + sqrt(5.0)) / 2.0).toFloat()
    private val INV_PHI = 1f / PHI

    /** Tamaño visual de cada dado (radio envolvente en cm). Un d6 de 16 mm tiene radio ≈ 1,39 cm. */
    private val radioCm = mapOf(
        TipoDado.D4 to 1.45f, TipoDado.D6 to 1.30f, TipoDado.D8 to 1.30f, TipoDado.D10 to 1.30f,
        TipoDado.D100_DECENAS to 1.30f, TipoDado.D12 to 1.40f, TipoDado.LEGENDARIO to 1.45f, TipoDado.D20 to 1.45f, TipoDado.FATE to 1.30f
    )

    private val cache = HashMap<TipoDado, Poliedro>()

    fun de(tipo: TipoDado): Poliedro = synchronized(cache) { cache.getOrPut(tipo) { construir(tipo) } }

    private fun v(x: Float, y: Float, z: Float) = Vec3(x, y, z)

    /** Caja (prisma rectangular) centrada en el origen: la pieza básica de rampas y paredes. */
    fun caja(mitad: Vec3): Poliedro {
        val vs = listOf(-1f, 1f).flatMap { x -> listOf(-1f, 1f).flatMap { y -> listOf(-1f, 1f).map { z -> v(x * mitad.x, y * mitad.y, z * mitad.z) } } }
        val normales = listOf(v(1f, 0f, 0f), v(-1f, 0f, 0f), v(0f, 1f, 0f), v(0f, -1f, 0f), v(0f, 0f, 1f), v(0f, 0f, -1f))
        val caras = normales.map { n ->
            val maximo = vs.maxOf { it dot n }
            crearCara(vs, vs.indices.filter { (vs[it] dot n) > maximo - 1e-4f }, n)
        }
        return Poliedro(null, vs, caras)
    }

    /** Completa una lista de normales con sus opuestas: índice i ↔ índice (n-1-i). */
    private fun conOpuestas(mitad: List<Vec3>): List<Vec3> = mitad + mitad.reversed().map { -it }

    private fun construir(tipo: TipoDado): Poliedro {
        val r = radioCm.getValue(tipo)
        return when (tipo) {
            TipoDado.D6, TipoDado.FATE -> desdeNormales(
                tipo, r,
                vertices = listOf(-1f, 1f).flatMap { x -> listOf(-1f, 1f).flatMap { y -> listOf(-1f, 1f).map { z -> v(x, y, z) } } },
                normales = listOf(v(0f, 1f, 0f), v(1f, 0f, 0f), v(0f, 0f, 1f), v(0f, 0f, -1f), v(-1f, 0f, 0f), v(0f, -1f, 0f))
            )
            TipoDado.D8 -> desdeNormales(
                tipo, r,
                vertices = listOf(v(1f, 0f, 0f), v(-1f, 0f, 0f), v(0f, 1f, 0f), v(0f, -1f, 0f), v(0f, 0f, 1f), v(0f, 0f, -1f)),
                normales = conOpuestas(listOf(v(1f, 1f, 1f), v(-1f, 1f, 1f), v(-1f, 1f, -1f), v(1f, 1f, -1f)))
            )
            // El Legendario es un d12 físico: misma malla, otras caras impresas.
            TipoDado.D12, TipoDado.LEGENDARIO -> desdeNormales(tipo, r, verticesDodecaedro(), normalesD12())
            TipoDado.D20 -> desdeNormales(tipo, r, verticesIcosaedro(), normalesD20())
            TipoDado.D4 -> {
                // Normales de cara = direcciones de vértice del tetraedro "opuesto". El vértice k
                // queda enfrentado a la cara k, así el número de la cara apoyada se ve arriba.
                val normales = listOf(v(1f, 1f, 1f), v(1f, -1f, -1f), v(-1f, 1f, -1f), v(-1f, -1f, 1f))
                desdeNormales(tipo, r, normales.map { -it }, normales)
            }
            TipoDado.D10, TipoDado.D100_DECENAS -> trapezoedro(tipo, r)
        }
    }

    private fun normalesD12() = conOpuestas(
        listOf(v(0f, 1f, PHI), v(0f, 1f, -PHI), v(1f, PHI, 0f), v(-1f, PHI, 0f), v(PHI, 0f, 1f), v(PHI, 0f, -1f))
    )

    private fun normalesD20() = conOpuestas(
        listOf(
            v(1f, 1f, 1f), v(1f, 1f, -1f), v(1f, -1f, 1f), v(-1f, 1f, 1f),
            v(0f, INV_PHI, PHI), v(0f, INV_PHI, -PHI), v(INV_PHI, PHI, 0f), v(-INV_PHI, PHI, 0f),
            v(PHI, 0f, INV_PHI), v(PHI, 0f, -INV_PHI)
        )
    )

    private fun verticesIcosaedro(): List<Vec3> = buildList {
        for (a in listOf(-1f, 1f)) for (b in listOf(-PHI, PHI)) {
            add(v(0f, b, a)); add(v(b, a, 0f)); add(v(a, 0f, b))
        }
    }

    private fun verticesDodecaedro(): List<Vec3> = buildList {
        for (x in listOf(-1f, 1f)) for (y in listOf(-1f, 1f)) for (z in listOf(-1f, 1f)) add(v(x, y, z))
        for (a in listOf(-INV_PHI, INV_PHI)) for (b in listOf(-PHI, PHI)) {
            add(v(0f, b, a)); add(v(b, a, 0f)); add(v(a, 0f, b))
        }
    }

    /**
     * Arma las caras eligiendo, para cada normal, los vértices más alejados en esa dirección.
     * Funciona para los sólidos platónicos porque las normales dadas son exactas.
     */
    private fun desdeNormales(tipo: TipoDado, radio: Float, vertices: List<Vec3>, normales: List<Vec3>): Poliedro {
        val escala = radio / vertices.maxOf { it.longitud }
        val vs = vertices.map { it * escala }
        val caras = normales.map { bruta ->
            val n = bruta.normalizado()
            val maximo = vs.maxOf { it dot n }
            val indices = vs.indices.filter { (vs[it] dot n) > maximo - 1e-3f * radio }
            check(indices.size >= 3) { "${tipo.etiqueta}: la normal $n no corresponde a una cara" }
            crearCara(vs, indices, n)
        }
        return Poliedro(tipo, vs, caras)
    }

    /** Ordena los vértices de la cara en sentido antihorario visto desde afuera. */
    private fun crearCara(vs: List<Vec3>, indices: List<Int>, normal: Vec3): Cara {
        val centro = indices.map { vs[it] }.reduce { a, b -> a + b } * (1f / indices.size)
        val u = (vs[indices[0]] - centro).normalizado()
        val w = normal cross u
        val ordenados = indices.sortedBy { i ->
            val d = vs[i] - centro
            atan2(d dot w, d dot u)
        }
        return Cara(ordenados.toIntArray(), normal, normal dot centro, centro)
    }

    /**
     * Trapezoedro pentagonal (d10 / d%): dos polos y un anillo de 10 vértices en zigzag.
     * La altura de los polos se calcula para que cada "cometa" sea plana:
     * H = c · (1 + cos 36°) / (1 − cos 36°), con c = media altura del zigzag.
     * Caras de arriba (dígitos 0, 2, 4, 6, 8) y sus opuestas abajo (9, 7, 5, 3, 1).
     */
    private fun trapezoedro(tipo: TipoDado, radio: Float): Poliedro {
        val c = 0.1056f
        val cos36 = cos(Math.toRadians(36.0)).toFloat()
        val h = c * (1f + cos36) / (1f - cos36)
        val anillo = (0 until 10).map { j ->
            val az = Math.toRadians(36.0 * j)
            v(cos(az).toFloat(), if (j % 2 == 0) c else -c, sin(az).toFloat())
        }
        val polos = listOf(v(0f, h, 0f), v(0f, -h, 0f))
        val brutos = anillo + polos                 // índices 10 = polo superior, 11 = inferior
        val escala = radio / brutos.maxOf { it.longitud }
        val vs = brutos.map { it * escala }

        val porDigito = arrayOfNulls<Cara>(10)
        for (k in 0 until 5) {
            val j = 2 * k + 1                         // vértice bajo del anillo: centro de una cara de arriba
            val arriba = listOf(10, (j + 9) % 10, j, (j + 1) % 10)
            val jOp = (j + 5) % 10                    // vértice alto opuesto: centro de una cara de abajo
            val abajo = listOf(11, (jOp + 9) % 10, jOp, (jOp + 1) % 10)
            val digito = 2 * k
            porDigito[digito] = caraDesdeVertices(vs, arriba)
            porDigito[9 - digito] = caraDesdeVertices(vs, abajo)
        }
        return Poliedro(tipo, vs, porDigito.map { it!! })
    }

    private fun caraDesdeVertices(vs: List<Vec3>, indices: List<Int>): Cara {
        val centro = indices.map { vs[it] }.reduce { a, b -> a + b } * (1f / indices.size)
        var n = ((vs[indices[1]] - vs[indices[0]]) cross (vs[indices[2]] - vs[indices[0]])).normalizado()
        if ((n dot centro) < 0f) n = -n
        return crearCara(vs, indices, n)
    }
}
