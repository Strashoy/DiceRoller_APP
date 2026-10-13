package com.rolesencia.diceroller.fisica.motor

import com.rolesencia.diceroller.fisica.Poliedro
import com.rolesencia.diceroller.fisica.Quat
import com.rolesencia.diceroller.fisica.Vec3

/** Penetración entre dos poliedros: [normal] apunta de B hacia A (empujar A por ahí los separa). */
class Penetracion(val normal: Vec3, val profundidad: Float)

/**
 * Teorema del eje separador (SAT) para dos poliedros convexos.
 *
 * Dos convexos NO se tocan si existe un eje donde sus proyecciones no se superponen. Para
 * poliedros alcanza con probar: las normales de las caras de A, las de B y los productos
 * cruz de cada dirección de arista de A con cada una de B (el caso arista contra arista,
 * que la prueba "vértice dentro del otro dado" no ve). Si ningún eje los separa, el de menor
 * superposición da la dirección y la profundidad mínimas para separarlos.
 */
object EjeSeparador {

    fun penetracion(
        polA: Poliedro, posA: Vec3, rotA: Quat, verticesA: Array<Vec3>,
        polB: Poliedro, posB: Vec3, rotB: Quat, verticesB: Array<Vec3>
    ): Penetracion? {
        var mejorEje = Vec3.CERO
        var mejor = Float.MAX_VALUE

        fun probar(ejeBruto: Vec3, sesgo: Float): Boolean {
            val l = ejeBruto.longitud
            if (l < 1e-4f) return true
            val eje = ejeBruto * (1f / l)
            var minA = Float.MAX_VALUE; var maxA = -Float.MAX_VALUE
            for (v in verticesA) { val d = v dot eje; if (d < minA) minA = d; if (d > maxA) maxA = d }
            var minB = Float.MAX_VALUE; var maxB = -Float.MAX_VALUE
            for (v in verticesB) { val d = v dot eje; if (d < minB) minB = d; if (d > maxB) maxB = d }
            val superposicion = minOf(maxA - minB, maxB - minA)
            if (superposicion <= 0f) return false            // eje separador: no hay contacto
            // Los ejes arista×arista sólo ganan si son claramente mejores (evita saltos de normal).
            if (superposicion * sesgo < mejor) {
                mejor = superposicion
                mejorEje = if (((posA - posB) dot eje) < 0f) -eje else eje
            }
            return true
        }

        for (n in polA.ejesCaras) if (!probar(rotA.rotar(n), 1f)) return null
        for (n in polB.ejesCaras) if (!probar(rotB.rotar(n), 1f)) return null
        val aristasA = polA.direccionesAristas.map { rotA.rotar(it) }
        val aristasB = polB.direccionesAristas.map { rotB.rotar(it) }
        for (ea in aristasA) for (eb in aristasB) if (!probar(ea cross eb, 1.05f)) return null
        return Penetracion(mejorEje, mejor)
    }

    /** Vértice de [vertices] más extremo en la dirección [d]. */
    fun soporte(vertices: Array<Vec3>, d: Vec3): Vec3 {
        var mejor = vertices[0]
        var max = mejor dot d
        for (v in vertices) { val p = v dot d; if (p > max) { max = p; mejor = v } }
        return mejor
    }
}
