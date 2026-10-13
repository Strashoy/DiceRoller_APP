package com.rolesencia.diceroller.fisica

import com.rolesencia.diceroller.domain.dados.TipoDado

/**
 * Rotaciones de simetría de cada dado: giros que dejan la malla EXACTAMENTE igual (mismos vértices,
 * mismas caras) pero cambian qué número queda en cada posición.
 *
 * Es la clave del modo "resultado predeterminado": como la forma no cambia, la física de la
 * tirada es idéntica; sólo cambia el número que se ve arriba.
 */
object Simetrias {

    private val cache = HashMap<Triple<TipoDado, Int, Int>, Quat>()

    /**
     * Rotación local S tal que `S · normal(desde) = normal(hacia)` y S es una simetría del poliedro.
     * Si el dado físico terminó con la cara [hacia] arriba, dibujarlo con orientación `q · S`
     * muestra la cara [desde] arriba.
     */
    fun rotacionEntreCaras(tipo: TipoDado, desde: Int, hacia: Int): Quat = synchronized(cache) {
        cache.getOrPut(Triple(tipo, desde, hacia)) { buscar(Poliedros.de(tipo), desde, hacia) }
    }

    private fun buscar(p: Poliedro, desde: Int, hacia: Int): Quat {
        if (desde == hacia) return Quat.IDENTIDAD
        val nDesde = p.caras[desde].normal
        val nHacia = p.caras[hacia].normal
        val vecinaDesde = p.vecinas[desde].first()
        for (vecinaHacia in p.vecinas[hacia]) {
            val s = Quat.entrePares(nDesde, p.caras[vecinaDesde].normal, nHacia, p.caras[vecinaHacia].normal)
            if (esSimetria(p, s)) return s
        }
        error("${p.tipo?.etiqueta}: no hay simetría que lleve la cara $desde a la $hacia")
    }

    /** Una rotación es simetría si manda cada vértice sobre otro vértice. */
    fun esSimetria(p: Poliedro, s: Quat): Boolean {
        val tolerancia = 1e-3f * p.radio
        return p.vertices.all { v ->
            val r = s.rotar(v)
            p.vertices.any { (it - r).longitud < tolerancia }
        }
    }
}
