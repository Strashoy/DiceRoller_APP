package com.rolesencia.diceroller.ui.mesa3d

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * El GIF se escribe a mano, así que se comprueba leyéndolo con un decodificador independiente (el de
 * abajo, que sigue la especificación del formato): lo que se decodifica tiene que ser, píxel por
 * píxel, lo que se pidió codificar.
 */
class CodificadorGifTest {

    /** Colores que sobreviven intactos a la reducción a 15 bits de la paleta. */
    private fun color(r5: Int, g5: Int, b5: Int): Int {
        fun ocho(v: Int) = (v shl 3) or (v shr 2)
        return (0xFF shl 24) or (ocho(r5) shl 16) or (ocho(g5) shl 8) or ocho(b5)
    }

    private fun codificar(ancho: Int, alto: Int, cuadros: List<IntArray>, centesimas: List<Int>): ByteArray {
        val codificador = CodificadorGif(ancho, alto, PaletaGif.de(cuadros))
        cuadros.forEachIndexed { i, c -> codificador.agregar(c, centesimas[i]) }
        return codificador.terminar()
    }

    @Test fun lo_que_se_codifica_es_lo_que_se_lee() {
        val ancho = 16
        val alto = 12
        val colores = List(20) { color(it, 31 - it, (it * 7) % 32) }
        val a = IntArray(ancho * alto) { colores[(it * 3 + it / ancho) % colores.size] }
        // Segundo cuadro: cambia sólo un rectángulo chico. Tercero: igual al segundo.
        val b = a.copyOf().also { for (y in 4..6) for (x in 5..9) it[y * ancho + x] = colores[(x + y) % 5] }
        val gif = codificar(ancho, alto, listOf(a, b, b), listOf(8, 8, 250))
        val leido = GifDePrueba.leer(gif)
        assertEquals(ancho to alto, leido.ancho to leido.alto)
        assertTrue("se repite para siempre", leido.repite)
        assertEquals(listOf(8, 8, 250), leido.centesimas)
        assertArrayEquals(a, leido.cuadros[0])
        assertArrayEquals(b, leido.cuadros[1])
        assertArrayEquals(b, leido.cuadros[2])
    }

    @Test fun un_cuadro_ruidoso_llena_la_tabla_y_sigue_bien() {
        // Ruido con muchos colores: obliga a pasar por códigos de 9 a 12 bits y a vaciar la tabla.
        val ancho = 160
        val alto = 120
        val azar = Random(7)
        val colores = List(200) { color(azar.nextInt(32), azar.nextInt(32), azar.nextInt(32)) }.distinct()
        val ruido = IntArray(ancho * alto) { colores[azar.nextInt(colores.size)] }
        val otro = IntArray(ancho * alto) { colores[azar.nextInt(colores.size)] }
        val leido = GifDePrueba.leer(codificar(ancho, alto, listOf(ruido, otro), listOf(10, 10)))
        assertArrayEquals(ruido, leido.cuadros[0])
        assertArrayEquals(otro, leido.cuadros[1])
    }

    @Test fun con_la_camara_quieta_un_cuadro_casi_no_pesa() {
        val ancho = 200
        val alto = 150
        val fondo = IntArray(ancho * alto) { color((it % ancho) / 8, (it / ancho) / 6, 12) }
        val conDado = fondo.copyOf().also { for (y in 60..75) for (x in 90..105) it[y * ancho + x] = color(31, 2, 2) }
        val uno = codificar(ancho, alto, listOf(fondo), listOf(8)).size
        val dos = codificar(ancho, alto, listOf(fondo, conDado), listOf(8, 8)).size
        assertTrue("el segundo cuadro pesó ${dos - uno} bytes de $uno", dos - uno < uno / 8)
    }

    @Test fun la_paleta_no_pasa_de_255_colores_y_acerca_bien() {
        val azar = Random(3)
        val muchos = IntArray(40_000) { (0xFF shl 24) or azar.nextInt(0x1000000) }
        val paleta = PaletaGif.de(listOf(muchos))
        assertTrue("colores en la paleta: ${paleta.colores.size}", paleta.colores.size in 200..255)
        // Un degradé suave se representa con error chico en cada canal.
        val gris = IntArray(256) { (0xFF shl 24) or (it shl 16) or (it shl 8) or it }
        val paletaGris = PaletaGif.de(listOf(gris))
        gris.forEach { c ->
            val aproximado = paletaGris.colores[paletaGris.indice(c)]
            assertTrue("gris ${c and 0xFF} → ${aproximado and 0xFF}", kotlin.math.abs((c and 0xFF) - (aproximado and 0xFF)) <= 8)
        }
    }
}

/** Decodificador mínimo de GIF89a para las pruebas: tabla global, control gráfico, LZW y transparencia. */
private class GifDePrueba(val ancho: Int, val alto: Int, val repite: Boolean, val centesimas: List<Int>, val cuadros: List<IntArray>) {
    companion object {
        fun leer(b: ByteArray): GifDePrueba {
            fun u(i: Int) = b[i].toInt() and 0xFF
            fun corto(i: Int) = u(i) or (u(i + 1) shl 8)
            check(String(b, 0, 6, Charsets.US_ASCII) == "GIF89a")
            val ancho = corto(6)
            val alto = corto(8)
            check(u(10) == 0xF7) { "se espera una tabla global de 256 colores" }
            val paleta = IntArray(256) { (0xFF shl 24) or (u(13 + it * 3) shl 16) or (u(14 + it * 3) shl 8) or u(15 + it * 3) }
            var p = 13 + 768
            var repite = false
            var transparente = -1
            var demora = 0
            val lienzo = IntArray(ancho * alto)
            val demoras = ArrayList<Int>()
            val cuadros = ArrayList<IntArray>()
            while (true) {
                when (u(p)) {
                    0x3B -> return GifDePrueba(ancho, alto, repite, demoras, cuadros)
                    0x21 -> {
                        if (u(p + 1) == 0xF9) {
                            transparente = if (u(p + 3) and 1 == 1) u(p + 6) else -1
                            demora = corto(p + 4)
                        }
                        if (u(p + 1) == 0xFF && String(b, p + 3, 11, Charsets.US_ASCII) == "NETSCAPE2.0") repite = true
                        p += 2
                        while (u(p) != 0) p += u(p) + 1
                        p++
                    }
                    0x2C -> {
                        val x0 = corto(p + 1); val y0 = corto(p + 3); val w = corto(p + 5); val h = corto(p + 7)
                        check(u(p + 9) == 0) { "sin tabla local ni entrelazado" }
                        val minimo = u(p + 10)
                        p += 11
                        val datos = java.io.ByteArrayOutputStream()
                        while (u(p) != 0) { datos.write(b, p + 1, u(p)); p += u(p) + 1 }
                        p++
                        val indices = descomprimir(datos.toByteArray(), minimo, w * h)
                        for (y in 0 until h) for (x in 0 until w) {
                            val i = indices[y * w + x]
                            if (i != transparente) lienzo[(y0 + y) * ancho + x0 + x] = paleta[i]
                        }
                        demoras += demora
                        cuadros += lienzo.copyOf()
                    }
                    else -> error("bloque desconocido ${u(p)} en $p")
                }
            }
        }

        private fun descomprimir(datos: ByteArray, minimo: Int, cantidad: Int): IntArray {
            val limpiar = 1 shl minimo
            val fin = limpiar + 1
            val prefijo = IntArray(4096)
            val sufijo = IntArray(4096)
            val salida = IntArray(cantidad)
            var escritos = 0
            var bits = minimo + 1
            var libre = fin + 1
            var previo = -1
            var acumulador = 0
            var disponibles = 0
            var p = 0
            val pila = IntArray(4097)
            while (true) {
                while (disponibles < bits) { acumulador = acumulador or ((datos[p++].toInt() and 0xFF) shl disponibles); disponibles += 8 }
                val codigo = acumulador and ((1 shl bits) - 1)
                acumulador = acumulador ushr bits
                disponibles -= bits
                if (codigo == limpiar) { bits = minimo + 1; libre = fin + 1; previo = -1; continue }
                if (codigo == fin) break
                var n = 0
                var c = codigo
                if (previo >= 0 && codigo >= libre) { c = previo }       // caso "cScSc": se completa abajo
                while (c > limpiar) { pila[n++] = sufijo[c]; c = prefijo[c] }
                val primero = c
                pila[n++] = primero
                while (n > 0) salida[escritos++] = pila[--n]
                if (previo >= 0 && codigo >= libre) salida[escritos++] = primero
                if (previo >= 0 && libre < 4096) {
                    prefijo[libre] = previo; sufijo[libre] = primero; libre++
                    if (libre == (1 shl bits) && bits < 12) bits++
                }
                previo = codigo
            }
            check(escritos == cantidad) { "se esperaban $cantidad píxeles y salieron $escritos" }
            return salida
        }
    }
}
