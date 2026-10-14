package com.rolesencia.diceroller.ui.viewmodel

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rolesencia.diceroller.data.GestorPreferencias
import com.rolesencia.diceroller.data.PreferenciasGuardadas
import com.rolesencia.diceroller.domain.apariencia.CatalogoSkins
import com.rolesencia.diceroller.domain.apariencia.SkinDado
import com.rolesencia.diceroller.domain.dados.DadoEnMesa
import com.rolesencia.diceroller.domain.dados.FuenteResultados
import com.rolesencia.diceroller.domain.dados.LanzadorAleatorio
import com.rolesencia.diceroller.domain.dados.ResultadoDado
import com.rolesencia.diceroller.domain.dados.RolDado
import com.rolesencia.diceroller.domain.dados.TipoDado
import com.rolesencia.diceroller.domain.historial.EntradaHistorial
import com.rolesencia.diceroller.domain.historial.MAX_HISTORIAL
import com.rolesencia.diceroller.domain.historial.Notacion
import com.rolesencia.diceroller.domain.historial.TiradaGuardada
import com.rolesencia.diceroller.domain.historial.Ventaja
import com.rolesencia.diceroller.domain.reglas.CatalogoSistemas
import com.rolesencia.diceroller.domain.reglas.Conservar
import com.rolesencia.diceroller.domain.reglas.ParametrosTirada
import com.rolesencia.diceroller.domain.reglas.SistemaReglas
import com.rolesencia.diceroller.fisica.Superficie
import com.rolesencia.diceroller.fisica.TipoEvento
import com.rolesencia.diceroller.sensorial.GestorHaptico
import com.rolesencia.diceroller.sensorial.GestorSonido
import com.rolesencia.diceroller.sensorial.MaterialMesa
import com.rolesencia.diceroller.tirada.ConfigMesa
import com.rolesencia.diceroller.tirada.DadoVisual
import com.rolesencia.diceroller.tirada.FrameMesa
import com.rolesencia.diceroller.tirada.GeneradorLanzamiento
import com.rolesencia.diceroller.tirada.Gesto
import com.rolesencia.diceroller.tirada.ModoDecision
import com.rolesencia.diceroller.tirada.SesionTirada
import com.rolesencia.diceroller.tirada.SimulacionTirada
import com.rolesencia.diceroller.tirada.fisicareal.SesionFisicaReal
import com.rolesencia.diceroller.tirada.torre.ModelosTorre
import com.rolesencia.diceroller.tirada.predeterminada.PreSimulador
import com.rolesencia.diceroller.tirada.predeterminada.SesionResultadoPredeterminado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

class DiceRollerViewModel(
    private val gestorSonido: GestorSonido,
    private val gestorHaptico: GestorHaptico,
    private val gestorPreferencias: GestorPreferencias,
    /** Sorteo de los números en el modo predeterminado (cuando no se eligen a mano). */
    private val sorteo: FuenteResultados = LanzadorAleatorio()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiceRollerUiState())
    val uiState: StateFlow<DiceRollerUiState> = _uiState.asStateFlow()

    // Medición de la tirada en curso
    private var instanteToque = 0L
    private var instantePrimerMovimiento: Long? = null
    private var instanteSesionLista: Long? = null
    private var rondas = 0
    private val acumulados = ArrayList<ResultadoDado>()
    private var ultimaVibracion = 0L
    private var proporcionMesa = 1.2f
    /** Altura (cm) desde la que se sueltan los dados en cada torre, informada por la mesa 3D. */
    private val alturasEntrada = HashMap<String, Float>()

    init {
        viewModelScope.launch {
            val prefs = gestorPreferencias.preferenciasFlow.first()
            val sistema = CatalogoSistemas.porId(prefs.sistemaId)
            _uiState.update {
                val pool = prefs.pool.ifEmpty { sistema.tiradaSugerida() }
                it.copy(
                    estaCargando = false,
                    sistema = sistema,
                    pool = pool,
                    modeloTorre = prefs.modeloTorre,
                    skinsDelSistema = prefs.skinsDelSistema,
                    mesa = mesaPara(pool.size, prefs.modeloTorre),
                    dadosEnMesa = disposicion(pool, mesaPara(pool.size, prefs.modeloTorre), prefs.modeloTorre),
                    parametros = ParametrosTirada(
                        modificador = prefs.modificador,
                        objetivo = prefs.objetivo ?: sistema.campoObjetivo?.porDefecto,
                        conservar = prefs.conservar
                    ),
                    modo = prefs.modo,
                    skinsPersonalizadas = prefs.skinsPersonalizadas,
                    skinActual = CatalogoSkins.porId(prefs.skinSeleccionada, prefs.skinsPersonalizadas),
                    materialMesa = prefs.materialMesa,
                    audioHabilitado = prefs.audioHabilitado,
                    vibracionHabilitada = prefs.vibracionHabilitada,
                    pantallaEncendida = prefs.pantallaEncendida,
                    pantallaCompleta = prefs.pantallaCompleta,
                    vozHabilitada = prefs.vozHabilitada,
                    agitarHabilitado = prefs.agitarHabilitado,
                    manoIzquierda = prefs.manoIzquierda,
                    historial = prefs.historial,
                    tiradasGuardadas = prefs.tiradasGuardadas
                )
            }
        }
    }

    private fun guardar() {
        val s = _uiState.value
        viewModelScope.launch {
            gestorPreferencias.guardar(
                PreferenciasGuardadas(
                    sistemaId = s.sistema.id,
                    pool = s.pool,
                    modificador = s.parametros.modificador,
                    objetivo = s.parametros.objetivo,
                    conservar = s.parametros.conservar,
                    modo = s.modo,
                    skinsPersonalizadas = s.skinsPersonalizadas,
                    skinSeleccionada = s.skinActual.id,
                    materialMesa = s.materialMesa,
                    modeloTorre = s.modeloTorre,
                    skinsDelSistema = s.skinsDelSistema,
                    audioHabilitado = s.audioHabilitado,
                    vibracionHabilitada = s.vibracionHabilitada,
                    pantallaEncendida = s.pantallaEncendida,
                    pantallaCompleta = s.pantallaCompleta,
                    vozHabilitada = s.vozHabilitada,
                    agitarHabilitado = s.agitarHabilitado,
                    manoIzquierda = s.manoIzquierda
                )
            )
        }
    }

    private val ocupado: Boolean get() = _uiState.value.let { it.estaRodando || it.preparando }

    /** Cambia la mesa, limpia resultados y vuelve a acomodar los dados; nada cambia mientras ruedan. */
    private fun editarMesa(cambio: (DiceRollerUiState) -> DiceRollerUiState) {
        if (ocupado) return
        _uiState.update { anterior ->
            val nuevo = cambio(anterior)
            val ids = nuevo.pool.map { it.id }.toSet()
            val mesa = mesaPara(nuevo.pool.size, nuevo.modeloTorre)
            nuevo.copy(
                mesa = mesa,
                resultados = emptyList(),
                veredicto = null,
                valoresElegidos = nuevo.valoresElegidos.filterKeys { it in ids },
                dadosEnMesa = disposicion(nuevo.pool, mesa, nuevo.modeloTorre)
            )
        }
        guardar()
    }

    // --- SISTEMA Y PARÁMETROS ---
    /** La skin con que se agrega un dado: la del sistema (si la propone y está activado) o la elegida. */
    private fun skinPara(estado: DiceRollerUiState, sistema: SistemaReglas, rol: RolDado): SkinDado =
        (if (estado.skinsDelSistema) CatalogoSkins.sugeridaPara(sistema.id, rol == RolDado.ESPECIAL) else null)
            ?: estado.skinActual

    fun seleccionarSistema(sistema: SistemaReglas) = editarMesa {
        it.copy(
            sistema = sistema,
            pool = sistema.tiradaSugerida().map { d -> d.copy(skin = skinPara(it, sistema, d.rol)) },
            parametros = ParametrosTirada(objetivo = sistema.campoObjetivo?.porDefecto)
        )
    }

    fun cambiarModificador(delta: Int) = editarMesa {
        it.copy(parametros = it.parametros.copy(modificador = (it.parametros.modificador + delta).coerceIn(-20, 20)))
    }

    fun cambiarObjetivo(delta: Int) = editarMesa { estado ->
        val campo = estado.sistema.campoObjetivo ?: return@editarMesa estado
        val actual = estado.parametros.objetivo ?: campo.porDefecto
        val nuevo = (actual + delta).coerceIn(campo.rango.first, campo.rango.last)
        estado.copy(parametros = estado.parametros.copy(objetivo = nuevo))
    }

    fun cambiarConservar(modo: Conservar) = editarMesa {
        it.copy(parametros = it.parametros.copy(conservar = modo))
    }

    // --- DADOS EN LA MESA ---
    /** Agrega un dado con la skin elegida y, si está marcado, como especial. */
    fun agregarDado(tipo: TipoDado) = editarMesa { estado ->
        if (estado.pool.size >= DiceRollerUiState.MAX_DADOS) return@editarMesa estado
        val id = (estado.pool.maxOfOrNull { it.id } ?: -1) + 1
        val rol = if (estado.especialSiguiente) RolDado.ESPECIAL else RolDado.NORMAL
        estado.copy(pool = estado.pool + DadoEnMesa(id, tipo, rol, skinPara(estado, estado.sistema, rol)))
    }

    fun quitarDado(id: Int) = editarMesa { estado -> estado.copy(pool = estado.pool.filterNot { it.id == id }) }

    /** Quita el último dado agregado de ese tipo, prefiriendo uno con el rol que se está agregando. */
    fun quitarUnoDeTipo(tipo: TipoDado) {
        val aQuitar = dadoAQuitar(_uiState.value, tipo) ?: return
        editarMesa { estado -> estado.copy(pool = estado.pool - aQuitar) }
    }

    /** Qué dado quitaría [quitarUnoDeTipo] (para ofrecer "Deshacer"). */
    fun dadoAQuitar(tipo: TipoDado): DadoEnMesa? = dadoAQuitar(_uiState.value, tipo)

    private fun dadoAQuitar(estado: DiceRollerUiState, tipo: TipoDado): DadoEnMesa? {
        val rol = if (estado.especialSiguiente) RolDado.ESPECIAL else RolDado.NORMAL
        val delTipo = estado.pool.filter { it.tipo == tipo }
        return delTipo.lastOrNull { it.rol == rol } ?: delTipo.lastOrNull()
    }

    /** "Deshacer" tras quitar un dado: vuelve tal cual estaba (tipo, rol y skin). */
    fun restaurarDado(dado: DadoEnMesa) = editarMesa { estado ->
        if (estado.pool.size >= DiceRollerUiState.MAX_DADOS) return@editarMesa estado
        val id = if (estado.pool.any { it.id == dado.id }) (estado.pool.maxOfOrNull { it.id } ?: -1) + 1 else dado.id
        estado.copy(pool = estado.pool + dado.copy(id = id))
    }

    fun restaurarTiradaSugerida() = editarMesa { estado ->
        estado.copy(pool = estado.sistema.tiradaSugerida().map { it.copy(skin = skinPara(estado, estado.sistema, it.rol)) })
    }

    // --- SKINS ---
    /** Elegir una skin a mano gana sobre las del sistema de juego (si no, el dado no saldría con ella). */
    fun seleccionarSkin(skin: SkinDado) {
        _uiState.update { it.copy(skinActual = skin, skinsDelSistema = false) }
        guardar()
    }

    fun toggleSkinsDelSistema() {
        _uiState.update { it.copy(skinsDelSistema = !it.skinsDelSistema) }
        guardar()
    }

    fun toggleEspecialSiguiente() {
        _uiState.update { it.copy(especialSiguiente = !it.especialSiguiente) }
    }

    /** Pinta todos los dados de la mesa con la skin actual (el Legendario conserva su dorado). */
    fun aplicarSkinATodos() = editarMesa { estado ->
        estado.copy(pool = estado.pool.map { it.copy(skin = estado.skinActual) })
    }

    /** Guarda (o reemplaza, si ya existe el id) una skin creada por el usuario y la selecciona. */
    fun guardarSkin(skin: SkinDado) {
        _uiState.update { estado ->
            val lista = estado.skinsPersonalizadas.filterNot { it.id == skin.id } + skin
            estado.copy(skinsPersonalizadas = lista, skinActual = skin)
        }
        guardar()
    }

    fun borrarSkin(skin: SkinDado) {
        _uiState.update { estado ->
            estado.copy(
                skinsPersonalizadas = estado.skinsPersonalizadas.filterNot { it.id == skin.id },
                skinActual = if (estado.skinActual.id == skin.id) CatalogoSkins.POR_TIPO else estado.skinActual
            )
        }
        guardar()
    }

    // --- MODO DE DECISIÓN ---
    fun seleccionarModo(modo: ModoDecision) = editarMesa { it.copy(modo = modo, metricas = null) }

    fun toggleElegirValores() = editarMesa { it.copy(elegirValores = !it.elegirValores) }

    /** Pasa al siguiente valor posible del dado (sólo modo predeterminado con valores elegidos). */
    fun ciclarValor(id: Int) {
        if (ocupado) return
        _uiState.update { estado ->
            val dado = estado.pool.firstOrNull { it.id == id } ?: return@update estado
            val valores = dado.tipo.valoresCaras.distinct().sorted()
            val actual = estado.valoresElegidos[id] ?: valores.last()
            val siguiente = valores[(valores.indexOf(actual) + 1) % valores.size]
            estado.copy(valoresElegidos = estado.valoresElegidos + (id to siguiente))
        }
    }

    // --- MESA ---

    /** La bandeja según la cantidad de dados; con torre, con fondo suficiente para la torre y la salida. */
    private fun mesaPara(cantidad: Int, torre: String?): ConfigMesa {
        val base = ConfigMesa.para(cantidad, proporcionMesa)
        return if (torre == null) base else base.copy(profundidad = maxOf(base.profundidad, ModelosTorre.PROFUNDIDAD_MINIMA))
    }

    /** Dados esperando el lanzamiento: en fila sobre la bandeja, o dentro de la bandeja de la torre. */
    private fun disposicion(pool: List<DadoEnMesa>, mesa: ConfigMesa, torre: String?): List<DadoVisual> =
        GeneradorLanzamiento.disposicionInicial(pool, mesa, torre?.let { ModelosTorre.construir(it, mesa).zonaBandeja })

    /** Bandeja (null) o una de las torres de dados. */
    fun seleccionarLanzamiento(torre: String?) = editarMesa { it.copy(modeloTorre = torre) }
    fun cambiarMaterialMesa(material: MaterialMesa) {
        _uiState.update { it.copy(materialMesa = material) }
        guardar()
    }

    /** La mesa 3D informa desde qué altura un dado queda fuera de la pantalla, sobre la torre. */
    fun actualizarAlturaEntrada(idTorre: String, altura: Float) {
        alturasEntrada[idTorre] = altura
    }

    /** La mesa 3D informa su proporción; la bandeja física se adapta a ella y a la cantidad de dados. */
    fun actualizarProporcionMesa(altoSobreAncho: Float) {
        if (abs(proporcionMesa - altoSobreAncho) < 0.01f) return
        proporcionMesa = altoSobreAncho
        if (ocupado) return
        _uiState.update {
            val mesa = mesaPara(it.pool.size, it.modeloTorre)
            it.copy(mesa = mesa, dadosEnMesa = disposicion(it.pool, mesa, it.modeloTorre))
        }
    }

    // --- AJUSTES ---
    fun toggleAudio() {
        _uiState.update { it.copy(audioHabilitado = !it.audioHabilitado) }
        guardar()
    }

    fun toggleVibracion() {
        _uiState.update { it.copy(vibracionHabilitada = !it.vibracionHabilitada) }
        guardar()
    }

    fun toggleVoz() {
        _uiState.update { it.copy(vozHabilitada = !it.vozHabilitada) }
        guardar()
    }

    fun toggleZurdo() {
        _uiState.update { it.copy(manoIzquierda = !it.manoIzquierda) }
        guardar()
    }

    fun toggleAgitar() {
        _uiState.update { it.copy(agitarHabilitado = !it.agitarHabilitado) }
        guardar()
    }

    fun togglePantallaCompleta() {
        _uiState.update { it.copy(pantallaCompleta = !it.pantallaCompleta) }
        guardar()
    }

    fun togglePantallaEncendida() {
        _uiState.update { it.copy(pantallaEncendida = !it.pantallaEncendida) }
        guardar()
    }

    // --- TIRADAS RÁPIDAS: ventaja, historial y tiradas guardadas ---

    /**
     * Pulsación larga en LANZAR: elegir ventaja, desventaja o normal y lanzar en el mismo gesto
     * (como el menú del botón de tirada de D&D Beyond). En el sistema d20 agrega o quita el segundo d20.
     */
    fun lanzarCon(conservar: Conservar) {
        if (ocupado) return
        editarMesa { estado ->
            estado.copy(
                pool = Ventaja.ajustarPool(estado.sistema.id, estado.pool, conservar, estado.skinActual),
                parametros = estado.parametros.copy(conservar = conservar)
            )
        }
        lanzar()
    }

    /** Deja la mesa como en una tirada anterior o guardada (sistema, dados y parámetros). */
    private fun cargar(sistemaId: String, pool: List<DadoEnMesa>, parametros: ParametrosTirada) = editarMesa {
        it.copy(
            sistema = CatalogoSistemas.porId(sistemaId),
            pool = pool.mapIndexed { i, d -> d.copy(id = i) },
            parametros = parametros
        )
    }

    /** "Otra vez": repite una tirada del historial tal cual (D&D Beyond, Roll20). */
    fun repetir(entrada: EntradaHistorial) {
        if (ocupado || entrada.pool.isEmpty()) return
        cargar(entrada.sistemaId, entrada.pool, entrada.parametros)
        lanzar()
    }

    /** Tocar una tirada guardada la carga y la lanza (presets de Dice by PCalc). */
    fun lanzarGuardada(tirada: TiradaGuardada) {
        if (ocupado || tirada.pool.isEmpty()) return
        cargar(tirada.sistemaId, tirada.pool, tirada.parametros)
        lanzar()
    }

    /** Guarda la mesa actual con un nombre; si el nombre ya existe, la reemplaza. */
    fun guardarTiradaActual(nombre: String) {
        val limpio = nombre.trim().take(40)
        if (limpio.isEmpty()) return
        _uiState.update { s ->
            val nueva = TiradaGuardada(limpio, s.sistema.id, s.pool, s.parametros)
            s.copy(tiradasGuardadas = s.tiradasGuardadas.filterNot { it.nombre.equals(limpio, ignoreCase = true) } + nueva)
        }
        val lista = _uiState.value.tiradasGuardadas
        viewModelScope.launch { gestorPreferencias.guardarTiradas(lista) }
    }

    fun borrarTiradaGuardada(tirada: TiradaGuardada) {
        _uiState.update { s -> s.copy(tiradasGuardadas = s.tiradasGuardadas - tirada) }
        val lista = _uiState.value.tiradasGuardadas
        viewModelScope.launch { gestorPreferencias.guardarTiradas(lista) }
    }

    fun borrarHistorial() {
        _uiState.update { it.copy(historial = emptyList()) }
        viewModelScope.launch { gestorPreferencias.guardarHistorial(emptyList()) }
    }

    // --- LANZAMIENTO ---

    /** Botón "Lanzar" (sin gesto) o deslizamiento sobre la mesa (con dirección y fuerza). */
    fun lanzar(gesto: Gesto? = null) {
        val s = _uiState.value
        if (ocupado || s.pool.isEmpty()) return
        instanteToque = System.nanoTime()
        instantePrimerMovimiento = null
        instanteSesionLista = null
        rondas = 0
        acumulados.clear()
        gestorSonido.reiniciarEstadisticas()
        _uiState.update { it.copy(estaRodando = true, resultados = emptyList(), veredicto = null, metricas = null) }
        iniciarRonda(s.pool, obstaculos = emptyList(), gesto = gesto, primeraRonda = true)
    }

    private fun iniciarRonda(dados: List<DadoEnMesa>, obstaculos: List<DadoVisual>, gesto: Gesto?, primeraRonda: Boolean) {
        val s = _uiState.value
        val semilla = Random.nextLong()
        // Con torre los dados se sueltan por arriba y la torre es parte del escenario físico.
        val torre = s.modeloTorre?.let { ModelosTorre.construir(it, s.mesa) }
        val impulsos = if (torre != null) GeneradorLanzamiento.impulsosTorre(
            dados, torre, Random(semilla), alturasEntrada[torre.id] ?: torre.alturaCaida
        )
        else GeneradorLanzamiento.impulsos(dados, s.mesa, gesto, Random(semilla))
        val simulacion = SimulacionTirada(dados, impulsos, obstaculos, s.mesa, semilla, escenario = torre?.escenarioFisico())
        when (s.modo) {
            // Modo 1: la sesión arranca ya; la mesa la avanza en vivo cuadro a cuadro.
            ModoDecision.FISICA_REAL -> {
                val sesion = SesionFisicaReal(simulacion)
                if (primeraRonda) instanteSesionLista = System.nanoTime()
                _uiState.update { it.copy(sesion = sesion) }
            }

            // Modo 2: primero se decide el número y se pre-simula todo (fuera del hilo de UI).
            ModoDecision.PREDETERMINADO -> {
                val deseados = if (primeraRonda && s.elegirValores) {
                    dados.associate { d -> d.id to (s.valoresElegidos[d.id] ?: d.tipo.maximo) }
                } else {
                    sorteo.lanzar(dados).associate { it.dado.id to it.valor }
                }
                _uiState.update { it.copy(preparando = true) }
                viewModelScope.launch {
                    val grabacion = withContext(Dispatchers.Default) { PreSimulador.grabar(simulacion) }
                    Log.d(TAG, "Pre-simulación: ${grabacion.milisegundosCalculo} ms para ${grabacion.duracion} s")
                    if (primeraRonda) instanteSesionLista = System.nanoTime()
                    _uiState.update {
                        it.copy(
                            preparando = false,
                            sesion = SesionResultadoPredeterminado(dados, grabacion, deseados, Random(semilla))
                        )
                    }
                }
            }
        }
    }

    /** La mesa 3D avisa cada cuadro dibujado: se mide la latencia y suenan/vibran los choques. */
    fun onCuadro(frame: FrameMesa, instanteCuadroNanos: Long) {
        if (instantePrimerMovimiento == null && frame.dados.any { !it.enReposo }) {
            instantePrimerMovimiento = instanteCuadroNanos
        }
        if (frame.eventos.isEmpty()) return
        val s = _uiState.value
        val material = s.modeloTorre?.let { ModelosTorre.materialPiso(it) } ?: s.materialMesa
        if (s.audioHabilitado) gestorSonido.reproducir(frame.eventos, material, s.mesa.ancho / 2f)
        if (s.vibracionHabilitada) {
            val golpe = frame.eventos
                .filter { it.tipo == TipoEvento.IMPACTO && (it.superficie == Superficie.PARED || it.velocidad > 150f) }
                .maxOfOrNull { it.velocidad }
            val ahora = SystemClock.uptimeMillis()
            if (golpe != null && ahora - ultimaVibracion > 50) {
                ultimaVibracion = ahora
                gestorHaptico.vibrarImpacto(golpe, velocidadMaxima = 250f)
            }
        }
    }

    /** La mesa avisa que la sesión terminó: o se relanzan explosiones o se evalúa. */
    fun onSesionTerminada(sesion: SesionTirada) {
        val s = _uiState.value
        if (sesion !== s.sesion) return
        val ronda = sesion.resultados()
        acumulados += ronda
        rondas++
        val estadoFinal = sesion.estadoFinal()
        val siguienteId = (acumulados.maxOfOrNull { it.dado.id } ?: -1) + 1
        val extra = if (rondas < 20) s.sistema.dadosExplosivos(ronda, siguienteId) else emptyList()

        if (extra.isNotEmpty()) {
            _uiState.update { it.copy(sesion = null, dadosEnMesa = estadoFinal) }
            iniciarRonda(extra, obstaculos = estadoFinal, gesto = null, primeraRonda = false)
            return
        }

        val fin = System.nanoTime()
        val metricas = MetricasTirada(
            sesion = sesion.metricas(),
            preparacionMs = instanteSesionLista?.let { (it - instanteToque) / 1_000_000 },
            latenciaPrimerMovimientoMs = instantePrimerMovimiento?.let { (it - instanteToque) / 1_000_000 },
            duracionTotalMs = (fin - instanteToque) / 1_000_000,
            rondas = rondas,
            sonido = gestorSonido.estadisticas()
        )
        Log.d(TAG, "Tirada ${metricas.sesion.modo}: primer movimiento ${metricas.latenciaPrimerMovimientoMs} ms")
        _uiState.update {
            val veredicto = it.sistema.evaluar(acumulados, it.parametros)
            val entrada = EntradaHistorial(
                instante = System.currentTimeMillis(),
                sistemaId = it.sistema.id,
                sistemaNombre = it.sistema.nombre,
                notacion = Notacion.de(it.pool, if (it.sistema.usaModificador) it.parametros.modificador else 0),
                valores = Notacion.valores(acumulados),
                nivel = veredicto.nivel,
                titulo = veredicto.titulo,
                detalle = veredicto.detalle,
                pool = it.pool,
                parametros = it.parametros
            )
            it.copy(
                sesion = null,
                estaRodando = false,
                dadosEnMesa = estadoFinal,
                resultados = acumulados.toList(),
                veredicto = veredicto,
                metricas = metricas,
                historial = (listOf(entrada) + it.historial).take(MAX_HISTORIAL)
            )
        }
        val historial = _uiState.value.historial
        viewModelScope.launch { gestorPreferencias.guardarHistorial(historial) }
    }

    override fun onCleared() {
        super.onCleared()
        gestorSonido.liberarMemoria()
    }

    private companion object {
        const val TAG = "DiceRoller"
    }
}

class DiceRollerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiceRollerViewModel::class.java)) {
            val app = context.applicationContext
            @Suppress("UNCHECKED_CAST")
            return DiceRollerViewModel(GestorSonido(app), GestorHaptico(app), GestorPreferencias(app)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
