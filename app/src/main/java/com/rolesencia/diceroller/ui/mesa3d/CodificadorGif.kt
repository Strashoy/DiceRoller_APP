package com.rolesencia.diceroller.ui.mesa3d

import java.io.ByteArrayOutputStream

/**
 * Paleta de hasta 255 colores para un GIF (el índice 255 queda para "transparente").
 *
 * Se arma por **corte de mediana**: los colores de unos cuadros de muestra, reducidos a 15 bits, se
 * reparten en cajas que se van partiendo por su canal más extendido, a la altura de la mediana. Cada
 * caja termina siendo un color de la paleta (su promedio).
 */
class PaletaGif private constructor(val colores: IntArray) {
    // Para cada color de 15 bits, el índice del color más parecido de la paleta (se calcula al pedirlo).
    private val cercano = ShortArray(32768) { -1 }

    fun indice(argb: Int): Int {
        val clave = ((argb shr 9) and 0x7C00) or ((argb shr 6) and 0x3E0) or ((argb shr 3) and 0x1F)
        val guardado = cercano[clave].toInt()
        if (guardado >= 0) return guardado
        // Se busca con el color representativo del grupo de 15 bits, no con el primero que llegó: así
        // el resultado no depende del orden de los píxeles ni de un empate en el borde del grupo.
        fun ocho(cinco: Int) = (cinco shl 3) or (cinco shr 2)
        val r = ocho((clave shr 10) and 0x1F)
        val g = ocho((clave shr 5) and 0x1F)
        val b = ocho(clave and 0x1F)
        var mejor = 0
        var distancia = Int.MAX_VALUE
        for (i in colores.indices) {
            val c = colores[i]
            val dr = r - ((c shr 16) and 0xFF)
            val dg = g - ((c shr 8) and 0xFF)
            val db = b - (c and 0xFF)
            val d = dr * dr + dg * dg + db * db
            if (d < distancia) { distancia = d; mejor = i }
        }
        cercano[clave] = mejor.toShort()
        return mejor
    }

    companion object {
        const val MAXIMO = 255

        fun de(muestras: List<IntArray>, maximo: Int = MAXIMO): PaletaGif {
            val cuenta = IntArray(32768)
            for (m in muestras) for (p in m) {
                cuenta[((p shr 9) and 0x7C00) or ((p shr 6) and 0x3E0) or ((p shr 3) and 0x1F)]++
            }
            val presentes = (0 until 32768).filter { cuenta[it] > 0 }
            if (presentes.isEmpty()) return PaletaGif(intArrayOf(0))
            var cajas = listOf(presentes)
            while (cajas.size < maximo) {
                // Se parte la caja con el canal más extendido (entre las que tienen más de un color).
                var elegida = -1
                var canal = 0
                var mayor = 0
                cajas.forEachIndexed { i, caja ->
                    if (caja.size < 2) return@forEachIndexed
                    for (k in 0..2) {
                        val desplazamiento = 10 - 5 * k
                        var minimo = 31
                        var maximoCanal = 0
                        for (c in caja) {
                            val v = (c shr desplazamiento) and 0x1F
                            if (v < minimo) minimo = v
                            if (v > maximoCanal) maximoCanal = v
                        }
                        // A igual extensión, la caja con más píxeles: ahí el error se nota más.
                        val peso = (maximoCanal - minimo) * 4096 + minOf(caja.size, 4095)
                        if (maximoCanal > minimo && peso > mayor) { mayor = peso; elegida = i; canal = desplazamiento }
                    }
                }
                if (elegida < 0) break
                val ordenada = cajas[elegida].sortedBy { (it shr canal) and 0x1F }
                val total = ordenada.sumOf { cuenta[it].toLong() }
                var acumulado = 0L
                var corte = 1
                for (i in ordenada.indices) {
                    acumulado += cuenta[ordenada[i]]
                    if (acumulado * 2 >= total) { corte = (i + 1).coerceIn(1, ordenada.size - 1); break }
                }
                cajas = cajas.take(elegida) + listOf(ordenada.take(corte), ordenada.drop(corte)) + cajas.drop(elegida + 1)
            }
            val colores = IntArray(cajas.size) { i ->
                var r = 0L; var g = 0L; var b = 0L; var n = 0L
                for (c in cajas[i]) {
                    val peso = cuenta[c].toLong()
                    r += ((c shr 10) and 0x1F) * peso; g += ((c shr 5) and 0x1F) * peso; b += (c and 0x1F) * peso; n += peso
                }
                fun ocho(v: Long): Int { val cinco = (v / n).toInt(); return (cinco shl 3) or (cinco shr 2) }
                (ocho(r) shl 16) or (ocho(g) shl 8) or ocho(b)
            }
            return PaletaGif(colores)
        }
    }
}

/**
 * Escribe un GIF animado, cuadro por cuadro, sin bibliotecas (Android no trae un codificador de GIF).
 *
 * Cada cuadro guarda sólo el rectángulo que cambió respecto del anterior, y dentro de él los píxeles
 * iguales van como "transparentes": con la cámara quieta y los dados moviéndose, un cuadro pesa una
 * fracción de la imagen completa. La compresión es LZW, la que define el formato.
 */
class CodificadorGif(private val ancho: Int, private val alto: Int, private val paleta: PaletaGif) {
    private val salida = ByteArrayOutputStream(256 * 1024)
    private var anterior: ByteArray? = null

    init {
        salida.write("GIF89a".toByteArray(Charsets.US_ASCII))
        corto(ancho); corto(alto)
        salida.write(0xF7)          // tabla global de 256 colores
        salida.write(0); salida.write(0)
        for (i in 0 until 256) {
            val c = if (i < paleta.colores.size) paleta.colores[i] else 0
            salida.write((c shr 16) and 0xFF); salida.write((c shr 8) and 0xFF); salida.write(c and 0xFF)
        }
        // Extensión de Netscape: repetir para siempre.
        salida.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B))
        salida.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        salida.write(byteArrayOf(0x03, 0x01, 0x00, 0x00, 0x00))
    }

    /** Agrega un cuadro ([pixeles] en ARGB, de [ancho] × [alto]) que dura [centesimas] de segundo. */
    fun agregar(pixeles: IntArray, centesimas: Int) {
        require(pixeles.size == ancho * alto)
        val indices = ByteArray(pixeles.size) { paleta.indice(pixeles[it]).toByte() }
        val previo = anterior
        var x0 = 0; var y0 = 0; var x1 = ancho - 1; var y1 = alto - 1
        var datos = indices
        if (previo != null) {
            // Rectángulo que cambió; si no cambió nada, un píxel transparente (sólo cuenta la demora).
            x0 = ancho; y0 = alto; x1 = -1; y1 = -1
            for (y in 0 until alto) for (x in 0 until ancho) {
                if (indices[y * ancho + x] != previo[y * ancho + x]) {
                    if (x < x0) x0 = x
                    if (x > x1) x1 = x
                    if (y < y0) y0 = y
                    if (y > y1) y1 = y
                }
            }
            if (x1 < 0) { x0 = 0; y0 = 0; x1 = 0; y1 = 0 }
            val w = x1 - x0 + 1
            datos = ByteArray(w * (y1 - y0 + 1))
            var k = 0
            for (y in y0..y1) for (x in x0..x1) {
                val i = y * ancho + x
                datos[k++] = if (indices[i] == previo[i]) TRANSPARENTE.toByte() else indices[i]
            }
        }
        anterior = indices
        // Control gráfico: no borrar el cuadro anterior; con transparencia desde el segundo.
        salida.write(byteArrayOf(0x21, 0xF9.toByte(), 0x04, (if (previo != null) 0x05 else 0x04).toByte()))
        corto(centesimas.coerceIn(2, 65535))
        salida.write(TRANSPARENTE); salida.write(0)
        salida.write(0x2C)
        corto(x0); corto(y0); corto(x1 - x0 + 1); corto(y1 - y0 + 1)
        salida.write(0)
        salida.write(8)             // tamaño mínimo de código LZW
        comprimir(datos)
        salida.write(0)
    }

    fun terminar(): ByteArray {
        salida.write(0x3B)
        return salida.toByteArray()
    }

    private fun corto(v: Int) { salida.write(v and 0xFF); salida.write((v shr 8) and 0xFF) }

    // --- LZW de GIF: códigos de 9 a 12 bits, bits menos significativos primero, en bloques de 255 ---

    private val claves = IntArray(TABLA)
    private val codigos = IntArray(TABLA)
    private val bloque = ByteArray(255)
    private var enBloque = 0
    private var acumulador = 0
    private var bitsAcumulados = 0

    private fun comprimir(datos: ByteArray) {
        var bits = 9
        var libre = FIN + 1
        claves.fill(-1)
        acumulador = 0; bitsAcumulados = 0; enBloque = 0
        emitir(LIMPIAR, bits)
        var prefijo = datos[0].toInt() and 0xFF
        for (i in 1 until datos.size) {
            val c = datos[i].toInt() and 0xFF
            val clave = (prefijo shl 8) or c
            var h = ((c shl 4) xor prefijo) % TABLA
            var encontrado = -1
            while (claves[h] >= 0) {
                if (claves[h] == clave) { encontrado = codigos[h]; break }
                h = if (h == 0) TABLA - 1 else h - 1
            }
            if (encontrado >= 0) { prefijo = encontrado; continue }
            emitir(prefijo, bits)
            // El código crece cuando el próximo a asignar ya no entra en los bits actuales.
            if (libre > (1 shl bits) - 1 && bits < 12) bits++
            if (libre < 4096) {
                claves[h] = clave; codigos[h] = libre++
            } else {
                emitir(LIMPIAR, bits)
                claves.fill(-1)
                libre = FIN + 1
                bits = 9
            }
            prefijo = c
        }
        emitir(prefijo, bits)
        if (libre > (1 shl bits) - 1 && bits < 12) bits++
        emitir(FIN, bits)
        if (bitsAcumulados > 0) byteDeDatos(acumulador and 0xFF)
        if (enBloque > 0) { salida.write(enBloque); salida.write(bloque, 0, enBloque); enBloque = 0 }
    }

    private fun emitir(codigo: Int, bits: Int) {
        acumulador = acumulador or (codigo shl bitsAcumulados)
        bitsAcumulados += bits
        while (bitsAcumulados >= 8) {
            byteDeDatos(acumulador and 0xFF)
            acumulador = acumulador ushr 8
            bitsAcumulados -= 8
        }
    }

    private fun byteDeDatos(b: Int) {
        bloque[enBloque++] = b.toByte()
        if (enBloque == 255) { salida.write(255); salida.write(bloque, 0, 255); enBloque = 0 }
    }

    private companion object {
        const val TRANSPARENTE = 255
        const val LIMPIAR = 256
        const val FIN = 257
        const val TABLA = 5003
    }
}
