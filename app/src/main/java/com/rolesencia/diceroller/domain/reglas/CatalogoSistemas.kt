package com.rolesencia.diceroller.domain.reglas

import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.reglas.sistemas.ConteoExitos
import com.rolesencia.diceroller.domain.reglas.sistemas.CypherSystem
import com.rolesencia.diceroller.domain.reglas.sistemas.D20ContraCd
import com.rolesencia.diceroller.domain.reglas.sistemas.Daggerheart
import com.rolesencia.diceroller.domain.reglas.sistemas.FateCore
import com.rolesencia.diceroller.domain.reglas.sistemas.ForgedInTheDark
import com.rolesencia.diceroller.domain.reglas.sistemas.Ironsworn
import com.rolesencia.diceroller.domain.reglas.sistemas.LlamadaDeCthulhu
import com.rolesencia.diceroller.domain.reglas.sistemas.PoweredByTheApocalypse
import com.rolesencia.diceroller.domain.reglas.sistemas.Rolesencia
import com.rolesencia.diceroller.domain.reglas.sistemas.Shadowrun
import com.rolesencia.diceroller.domain.reglas.sistemas.SumaLibre
import com.rolesencia.diceroller.domain.reglas.sistemas.VampiroV5
import com.rolesencia.diceroller.domain.reglas.sistemas.YearZero

/** Todos los sistemas que ofrece la app, en el orden en que aparecen en el selector. */
object CatalogoSistemas {

    val SOLO_DIECES = ConteoExitos(
        id = "solo_dieces", nombre = "Sólo dieces (d10)",
        tipo = TipoDado.D10, umbral = 10
    )
    val MUNDO_DE_TINIEBLAS = ConteoExitos(
        id = "wod", nombre = "Mundo de Tinieblas clásico",
        tipo = TipoDado.D10, umbral = 6, unosRestan = true
    )
    val CRONICAS_DE_LA_OSCURIDAD = ConteoExitos(
        id = "cofd", nombre = "Crónicas de la Oscuridad",
        tipo = TipoDado.D10, umbral = 8, explotaCon = 10, excepcional = 5
    )
    val EXALTED = ConteoExitos(
        id = "exalted", nombre = "Exalted 3ª ed.",
        tipo = TipoDado.D10, umbral = 7, dobleCon = 10, dadosIniciales = 7
    )

    val todos: List<SistemaReglas> = listOf(
        SumaLibre,
        Rolesencia,
        D20ContraCd,
        PoweredByTheApocalypse,
        ForgedInTheDark,
        SOLO_DIECES,
        MUNDO_DE_TINIEBLAS,
        CRONICAS_DE_LA_OSCURIDAD,
        EXALTED,
        VampiroV5,
        Shadowrun(),
        Shadowrun(reglaDelSeis = true),
        YearZero,
        LlamadaDeCthulhu,
        FateCore,
        CypherSystem,
        Daggerheart,
        Ironsworn
    )

    fun porId(id: String?): SistemaReglas = todos.firstOrNull { it.id == id } ?: SumaLibre
}
