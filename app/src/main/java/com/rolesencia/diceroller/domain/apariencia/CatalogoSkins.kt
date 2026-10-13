package com.rolesencia.diceroller.domain.apariencia

import com.rolesencia.diceroller.domain.apariencia.CategoriaSkin.CLASICOS
import com.rolesencia.diceroller.domain.apariencia.CategoriaSkin.GEMAS
import com.rolesencia.diceroller.domain.apariencia.CategoriaSkin.METALES
import com.rolesencia.diceroller.domain.apariencia.CategoriaSkin.TEMATICOS
import com.rolesencia.diceroller.domain.apariencia.ColorArgb.BLANCO
import com.rolesencia.diceroller.domain.apariencia.ColorArgb.NEGRO
import com.rolesencia.diceroller.domain.dados.TipoDado

/**
 * Referencia de skins de color. Cada una indica el color del cuerpo y una tinta (números o
 * símbolos) que contraste: casi siempre negro o blanco. El test `CatalogoSkinsTest` exige un
 * contraste WCAG de 4,5 o más en todas, así que ninguna skin del catálogo es ilegible.
 *
 * Tipos de skin futuros (degradados, materiales, texturas, imágenes): ver SKINS.md.
 */
object CatalogoSkins {

    /** Colores de la skin "Por tipo": cada tipo de dado con su color, para distinguirlos de un vistazo. */
    fun colorPorTipo(tipo: TipoDado): Long = when (tipo) {
        TipoDado.D4 -> 0xFF00796BL
        TipoDado.D6 -> 0xFF6A1B9AL
        TipoDado.D8 -> 0xFF1565C0L
        TipoDado.D10 -> 0xFF283593L
        TipoDado.D100_DECENAS -> 0xFF37474FL
        TipoDado.D12 -> 0xFF2E7D32L
        TipoDado.D20 -> 0xFF4527A0L
        TipoDado.FATE -> 0xFF5D4037L
        TipoDado.LEGENDARIO -> 0xFFC9A227L
    }

    val POR_TIPO = SkinDado("por_tipo", "Por tipo", CategoriaSkin.POR_TIPO, cuerpo = null, tinta = BLANCO)

    private fun s(id: String, nombre: String, categoria: CategoriaSkin, cuerpo: Long, tinta: Long, borde: Long? = null) =
        SkinDado(id, nombre, categoria, cuerpo, tinta, borde)

    /** Los colores más comunes de dados, con tinta negra o blanca según cuál se lea mejor. */
    val clasicos: List<SkinDado> = listOf(
        s("marfil", "Marfil", CLASICOS, 0xFFF3EEDFL, NEGRO),
        s("blanco", "Blanco", CLASICOS, 0xFFFAFAFAL, NEGRO),
        s("negro", "Negro", CLASICOS, 0xFF1C1C1CL, BLANCO),
        s("gris", "Gris", CLASICOS, 0xFF616161L, BLANCO),
        s("rojo", "Rojo", CLASICOS, 0xFFC62828L, BLANCO),
        s("granate", "Granate", CLASICOS, 0xFF7B1E2BL, BLANCO),
        s("naranja", "Naranja", CLASICOS, 0xFFF57C00L, NEGRO),
        s("amarillo", "Amarillo", CLASICOS, 0xFFF9D71CL, NEGRO),
        s("lima", "Verde lima", CLASICOS, 0xFF9CCC65L, NEGRO),
        s("verde", "Verde", CLASICOS, 0xFF2E7D32L, BLANCO),
        s("bosque", "Verde bosque", CLASICOS, 0xFF1B5E20L, BLANCO),
        s("turquesa", "Turquesa", CLASICOS, 0xFF26A69AL, NEGRO),
        s("celeste", "Celeste", CLASICOS, 0xFF4FC3F7L, NEGRO),
        s("azul", "Azul", CLASICOS, 0xFF1565C0L, BLANCO),
        s("marino", "Azul marino", CLASICOS, 0xFF1A237EL, BLANCO),
        s("violeta", "Violeta", CLASICOS, 0xFF6A1B9AL, BLANCO),
        s("lavanda", "Lavanda", CLASICOS, 0xFFB39DDBL, NEGRO),
        s("rosa", "Rosa", CLASICOS, 0xFFF48FB1L, NEGRO),
        s("fucsia", "Fucsia", CLASICOS, 0xFFC2185BL, BLANCO),
        s("marron", "Marrón", CLASICOS, 0xFF5D4037L, BLANCO),
        s("beige", "Beige", CLASICOS, 0xFFD7C4A3L, NEGRO)
    )

    val gemas: List<SkinDado> = listOf(
        s("rubi", "Rubí", GEMAS, 0xFF9B111EL, BLANCO, 0xFF4A0008L),
        s("esmeralda", "Esmeralda", GEMAS, 0xFF046307L, BLANCO, 0xFF012A03L),
        s("zafiro", "Zafiro", GEMAS, 0xFF0F52BAL, BLANCO, 0xFF06275AL),
        s("amatista", "Amatista", GEMAS, 0xFF7E57C2L, BLANCO, 0xFF3E2370L),
        s("topacio", "Topacio", GEMAS, 0xFFFFC87CL, NEGRO, 0xFFB07A2AL),
        s("onix", "Ónix", GEMAS, 0xFF0B0B0BL, BLANCO, 0xFF5A5A5AL),
        s("perla", "Perla", GEMAS, 0xFFEDE7E0L, NEGRO, 0xFFB8AFA4L),
        s("jade", "Jade", GEMAS, 0xFF00A86BL, NEGRO, 0xFF00573AL),
        s("aguamarina", "Aguamarina", GEMAS, 0xFF7FFFD4L, NEGRO, 0xFF2E9C7FL),
        s("citrino", "Citrino", GEMAS, 0xFFE4D00AL, NEGRO, 0xFF8A7C00L)
    )

    val metales: List<SkinDado> = listOf(
        s("oro", "Oro", METALES, 0xFFD4AF37L, NEGRO, 0xFF8C6D1FL),
        s("plata", "Plata", METALES, 0xFFC0C0C0L, NEGRO, 0xFF7A7A7AL),
        s("bronce", "Bronce", METALES, 0xFFCD7F32L, NEGRO, 0xFF7A4A1AL),
        s("cobre", "Cobre", METALES, 0xFFB87333L, NEGRO, 0xFF6E4019L),
        s("oro_rosa", "Oro rosa", METALES, 0xFFB76E79L, NEGRO, 0xFF6E3A42L),
        s("hierro", "Hierro", METALES, 0xFF434B4DL, BLANCO, 0xFF1F2324L)
    )

    /** Combinaciones con tinta de color: siguen cumpliendo el contraste mínimo. */
    val tematicos: List<SkinDado> = listOf(
        s("rolesencia", "Rolesencia", TEMATICOS, 0xFF0D1B2AL, 0xFFFFD700L, 0xFFFFA000L),
        s("vampiro", "Vampiro", TEMATICOS, 0xFF120A0AL, 0xFFFF5252L, 0xFF5C0000L),
        s("hielo", "Hielo", TEMATICOS, 0xFFE3F2FDL, 0xFF0D47A1L, 0xFF90CAF9L),
        s("fuego", "Fuego", TEMATICOS, 0xFFFF6F00L, 0xFF2B0A00L, 0xFFB71C1CL),
        s("elfico", "Bosque élfico", TEMATICOS, 0xFF1B5E20L, 0xFFE6C35CL, 0xFF0B2E0FL),
        s("neon", "Neón", TEMATICOS, 0xFF101018L, 0xFF00E5FFL, 0xFFFF00E5L),
        s("hueso", "Hueso y sangre", TEMATICOS, 0xFFE3DAC9L, 0xFF8B0000L, 0xFF9E9076L),
        s("pergamino", "Pergamino", TEMATICOS, 0xFFF2E8D5L, 0xFF3E2723L, 0xFFA1887FL),
        s("abismo", "Abismo", TEMATICOS, 0xFF0A1A2FL, 0xFF7FDBFFL, 0xFF14365CL),
        s("toxico", "Tóxico", TEMATICOS, 0xFF1B1B1BL, 0xFF76FF03L, 0xFF33691EL),
        s("real", "Realeza", TEMATICOS, 0xFF4A148CL, 0xFFFFD54FL, 0xFF240A45L)
    )

    private fun e(id: String, nombre: String, estilo: EstiloSkin, cuerpo: Long, tinta: Long, color2: Long? = null,
                  borde: Long? = null, tipografia: Tipografia = Tipografia.CLASICA, categoria: CategoriaSkin = CategoriaSkin.ESPECIALES) =
        SkinDado(id, nombre, categoria, cuerpo, tinta, borde, estilo, color2, tipografia)

    /** Un ejemplo (o más) de cada estilo de la hoja de ruta: punto de partida para el creador. */
    val especiales: List<SkinDado> = listOf(
        e("ajedrez", "Ajedrez", EstiloSkin.BICOLOR, 0xFFF3EEDFL, NEGRO, color2 = 0xFF1C1C1CL),
        e("arlequin", "Arlequín", EstiloSkin.BICOLOR, 0xFFC62828L, BLANCO, color2 = 0xFF1A237EL),
        e("atardecer", "Atardecer", EstiloSkin.DEGRADADO, 0xFFFFA726L, NEGRO, color2 = 0xFF6A1B9AL),
        e("oceano", "Océano", EstiloSkin.DEGRADADO, 0xFF4DD0E1L, NEGRO, color2 = 0xFF0D2A5CL),
        e("rubi_cristal", "Rubí translúcido", EstiloSkin.GEMA, 0xFFD50032L, BLANCO),
        e("zafiro_cristal", "Zafiro translúcido", EstiloSkin.GEMA, 0xFF1E63E9L, BLANCO),
        e("cristal", "Cristal de hielo", EstiloSkin.GEMA, 0xFFB3E5FCL, 0xFF0D2A5CL),
        e("oro_pulido", "Oro pulido", EstiloSkin.METALICO, 0xFFC9A227L, NEGRO, borde = 0xFF6E5410L),
        e("plata_pulida", "Plata pulida", EstiloSkin.METALICO, 0xFFA7ADB2L, NEGRO, borde = 0xFF5E6266L),
        e("cobre_pulido", "Cobre pulido", EstiloSkin.METALICO, 0xFFC27445L, NEGRO, borde = 0xFF5A2E14L),
        e("marmol_blanco", "Mármol blanco", EstiloSkin.MARMOL, 0xFFEDEBE6L, NEGRO, color2 = 0xFF8D8A85L),
        e("marmol_negro", "Mármol negro", EstiloSkin.MARMOL, 0xFF1E1E22L, 0xFFE6C35CL, color2 = 0xFFC9A227L),
        e("galaxia", "Galaxia", EstiloSkin.PURPURINA, 0xFF141A3CL, BLANCO, color2 = 0xFFFFFFFFL),
        e("purpurina_rosa", "Purpurina rosa", EstiloSkin.PURPURINA, 0xFFD81B60L, BLANCO, color2 = 0xFFFFE0F0L),
        e("fantasma", "Fantasma", EstiloSkin.BRILLO_NOCTURNO, 0xFF0B1A12L, 0xFFB9F6CAL, color2 = 0xFF69F0AEL),
        e("brasa", "Brasa", EstiloSkin.BRILLO_NOCTURNO, 0xFF1A0A06L, 0xFFFFCC80L, color2 = 0xFFFF6D00L),
        e("romano", "Senado romano", EstiloSkin.SOLIDO, 0xFFEDE3CEL, 0xFF5D1A1AL, tipografia = Tipografia.ROMANA),
        e("maquina", "Máquina de escribir", EstiloSkin.SOLIDO, 0xFFF3EEDFL, 0xFF212121L, tipografia = Tipografia.TECNICA),
        e("caligrafia", "Caligrafía", EstiloSkin.SOLIDO, 0xFF2E1F12L, 0xFFF3E5C8L, tipografia = Tipografia.MANUSCRITA)
    )

    /** Skins pensadas para los dados de cada sistema (Hambre de V5, Esperanza y Miedo de Daggerheart…). */
    val porSistema: List<SkinDado> = listOf(
        e("v5_hambre", "Hambre (Vampiro V5)", EstiloSkin.GEMA, 0xFF8E0000L, BLANCO, categoria = CategoriaSkin.SISTEMA),
        e("v5_normal", "Vampiro (V5)", EstiloSkin.SOLIDO, 0xFF141012L, 0xFFFF5252L, borde = 0xFF4A0000L, categoria = CategoriaSkin.SISTEMA),
        e("dh_esperanza", "Esperanza (Daggerheart)", EstiloSkin.METALICO, 0xFFD4AF37L, NEGRO, categoria = CategoriaSkin.SISTEMA),
        e("dh_miedo", "Miedo (Daggerheart)", EstiloSkin.BRILLO_NOCTURNO, 0xFF1A0A2EL, 0xFFE1BEE7L, color2 = 0xFFB388FFL, categoria = CategoriaSkin.SISTEMA),
        e("yze_base", "Base (Year Zero)", EstiloSkin.SOLIDO, 0xFF263238L, BLANCO, categoria = CategoriaSkin.SISTEMA),
        e("yze_estres", "Estrés (Alien)", EstiloSkin.SOLIDO, 0xFFFFD600L, NEGRO, categoria = CategoriaSkin.SISTEMA),
        e("sr_matriz", "Matriz (Shadowrun)", EstiloSkin.BRILLO_NOCTURNO, 0xFF07110FL, 0xFFA7FFEBL, color2 = 0xFF00E5FFL, tipografia = Tipografia.TECNICA, categoria = CategoriaSkin.SISTEMA),
        e("coc_mythos", "Mythos (Cthulhu)", EstiloSkin.MARMOL, 0xFF1B3A2FL, 0xFFE3DAC9L, color2 = 0xFF4DB6ACL, categoria = CategoriaSkin.SISTEMA),
        e("fate_pergamino", "Pergamino (Fate)", EstiloSkin.SOLIDO, 0xFFF2E8D5L, 0xFF3E2723L, tipografia = Tipografia.ANTIGUA, categoria = CategoriaSkin.SISTEMA),
        e("pbta_apocalipsis", "Apocalipsis (PbtA)", EstiloSkin.DEGRADADO, 0xFFFFB300L, NEGRO, color2 = 0xFF6D2E00L, categoria = CategoriaSkin.SISTEMA),
        e("fitd_humo", "Humo y acero (Blades)", EstiloSkin.METALICO, 0xFF5E6870L, BLANCO, categoria = CategoriaSkin.SISTEMA)
    )

    /**
     * Skin sugerida para un dado de un sistema según su rol, o null si el sistema no propone una.
     * Con la opción "Skins del sistema" activada, los dados se agregan con estas skins.
     */
    fun sugeridaPara(sistemaId: String, especial: Boolean): SkinDado? {
        val id = when (sistemaId) {
            "v5" -> if (especial) "v5_hambre" else "v5_normal"
            "daggerheart" -> if (especial) "dh_miedo" else "dh_esperanza"
            "yze" -> if (especial) "yze_estres" else "yze_base"
            "shadowrun", "shadowrun_edge" -> "sr_matriz"
            "coc" -> "coc_mythos"
            "fate" -> "fate_pergamino"
            "pbta" -> "pbta_apocalipsis"
            "fitd" -> "fitd_humo"
            else -> null
        }
        return id?.let { i -> porSistema.firstOrNull { it.id == i } }
    }

    val predefinidas: List<SkinDado> = listOf(POR_TIPO) + clasicos + especiales + gemas + metales + tematicos + porSistema

    /** Busca entre las predefinidas y las del usuario; si no existe, vuelve a "Por tipo". */
    fun porId(id: String?, personalizadas: List<SkinDado> = emptyList()): SkinDado =
        predefinidas.firstOrNull { it.id == id } ?: personalizadas.firstOrNull { it.id == id } ?: POR_TIPO
}
