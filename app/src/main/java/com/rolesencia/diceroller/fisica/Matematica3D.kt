package com.rolesencia.diceroller.fisica

import kotlin.math.sqrt

// Matemática mínima, sin dependencias, para que la lectura de caras sea testeable en la JVM.
// Convención de mundo: Y hacia arriba (la misma que usan Filament/SceneView y Jolt).

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    operator fun unaryMinus() = Vec3(-x, -y, -z)

    infix fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)

    val longitud: Float get() = sqrt(this dot this)
    fun normalizado(): Vec3 = longitud.let { if (it == 0f) this else this * (1f / it) }

    companion object {
        val CERO = Vec3(0f, 0f, 0f)
        val ARRIBA = Vec3(0f, 1f, 0f)
        val ABAJO = Vec3(0f, -1f, 0f)
    }
}

/** Cuaternión unitario (x, y, z, w). Es lo que devuelve el motor físico para cada cuerpo. */
data class Quat(val x: Float, val y: Float, val z: Float, val w: Float) {

    /** Rota un vector local al espacio de mundo. */
    fun rotar(v: Vec3): Vec3 {
        val u = Vec3(x, y, z)
        val t = (u cross v) * 2f
        return v + t * w + (u cross t)
    }

    fun conjugado() = Quat(-x, -y, -z, w)

    fun normalizado(): Quat {
        val n = sqrt(x * x + y * y + z * z + w * w)
        return if (n == 0f) IDENTIDAD else Quat(x / n, y / n, z / n, w / n)
    }

    /** Integra una velocidad angular [omega] (rad/s, espacio mundo) durante [dt] segundos. */
    fun integrar(omega: Vec3, dt: Float): Quat {
        val dq = Quat(omega.x, omega.y, omega.z, 0f) * this
        val h = 0.5f * dt
        return Quat(x + dq.x * h, y + dq.y * h, z + dq.z * h, w + dq.w * h).normalizado()
    }

    operator fun times(o: Quat) = Quat(
        w * o.x + x * o.w + y * o.z - z * o.y,
        w * o.y - x * o.z + y * o.w + z * o.x,
        w * o.z + x * o.y - y * o.x + z * o.w,
        w * o.w - x * o.x - y * o.y - z * o.z
    )

    companion object {
        val IDENTIDAD = Quat(0f, 0f, 0f, 1f)

        fun desdeEjeAngulo(eje: Vec3, radianes: Float): Quat {
            val e = eje.normalizado()
            val s = kotlin.math.sin(radianes / 2f)
            return Quat(e.x * s, e.y * s, e.z * s, kotlin.math.cos(radianes / 2f))
        }

        /** La rotación más corta que lleva [desde] a [hasta] (ambos normalizados). */
        fun entre(desde: Vec3, hasta: Vec3): Quat {
            val a = desde.normalizado()
            val b = hasta.normalizado()
            val d = a dot b
            if (d < -0.9999f) {
                val eje = (Vec3(1f, 0f, 0f) cross a).let { if (it.longitud < 1e-4f) Vec3(0f, 0f, 1f) cross a else it }
                return desdeEjeAngulo(eje, Math.PI.toFloat())
            }
            val c = a cross b
            return Quat(c.x, c.y, c.z, 1f + d).normalizado()
        }

        /**
         * Rotación que lleva el par de direcciones (a1, a2) al par (b1, b2): primero alinea a1→b1 y
         * luego gira alrededor de b1 para que a2 coincida con b2. Requiere ángulo(a1,a2) = ángulo(b1,b2).
         */
        fun entrePares(a1: Vec3, a2: Vec3, b1: Vec3, b2: Vec3): Quat {
            val q1 = entre(a1, b1)
            val a2r = q1.rotar(a2)
            val eje = b1.normalizado()
            val pa = (a2r - eje * (a2r dot eje)).normalizado()
            val pb = (b2 - eje * (b2 dot eje)).normalizado()
            val angulo = kotlin.math.atan2((pa cross pb) dot eje, pa dot pb)
            return desdeEjeAngulo(eje, angulo) * q1
        }
    }
}
