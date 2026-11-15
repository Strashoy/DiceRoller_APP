package com.rolesencia.diceroller.domain.historial

/**
 * Textos de los avisos breves de la pantalla. En las pruebas con gente, el consejo para quitar un dado
 * se cortaba ("…o tocá su –") y tapaba la bandeja, y al guardar nadie veía si se había guardado.
 * Acá viven los textos y su límite de largo, para poder probar que entran en una línea de un teléfono
 * angosto sin tener que abrir Android.
 */
object Avisos {
    /** Caracteres que entran en una línea en 320 dp (14 sp ≈ 7 dp por letra): sin acción, y con "Deshacer" al lado. */
    const val MAX_SIN_ACCION = 40
    const val MAX_CON_ACCION = 24

    const val CONSEJO_QUITAR = "Mantené apretado un dado para quitarlo"

    fun quitado(etiquetaDado: String): String = "Quitaste un $etiquetaDado"

    /** "Guardada: Espada larga"; si el nombre es largo se corta con "…" para que quepa en una línea. */
    fun guardada(nombre: String, max: Int = MAX_NOMBRE_CONFIRMADO): String {
        val limpio = nombre.trim()
        val corto = if (limpio.length > max) limpio.take(max - 1).trimEnd() + "…" else limpio
        return "Guardada: $corto"
    }

    /** Posición de la guardada que se acaba de crear (el nombre se compara sin distinguir mayúsculas). */
    fun posicionDe(guardadas: List<TiradaGuardada>, nombre: String): Int =
        guardadas.indexOfFirst { it.nombre.equals(nombre.trim(), ignoreCase = true) }

    private const val MAX_NOMBRE_CONFIRMADO = 24
}
