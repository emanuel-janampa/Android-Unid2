package pe.upeu.ecorecicla.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.core.context.GlobalContext
import pe.upeu.ecorecicla.domain.model.CrearPunto
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RegistrarPesaje
import pe.upeu.ecorecicla.domain.model.Sesion

private val EcoGreen = androidx.compose.ui.graphics.Color(0xFF176B4D)
private val EcoGreenLight = androidx.compose.ui.graphics.Color(0xFFE4F3E9)
private val EcoBackground = androidx.compose.ui.graphics.Color(0xFFF5F8F3)
private val EcoInk = androidx.compose.ui.graphics.Color(0xFF19342B)

@Composable
fun EcoRutasApp() {
    val loginViewModel = remember { GlobalContext.get().get<LoginViewModel>() }
    val auth by loginViewModel.uiState.collectAsState()
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = EcoGreen,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = EcoGreenLight,
            secondary = androidx.compose.ui.graphics.Color(0xFF50705D),
            background = EcoBackground,
            surface = androidx.compose.ui.graphics.Color.White,
            onSurface = EcoInk
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                auth.busy && auth.session == null -> LoadingScreen("Preparando sesión segura…")
                auth.session == null -> LoginScreen(
                    busy = auth.busy,
                    error = auth.error,
                    onLogin = loginViewModel::login
                )
                else -> MainScreen(
                    session = auth.session!!,
                    onLogout = loginViewModel::logout
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(busy: Boolean, error: String?, onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("operario1") }
    var password by remember { mutableStateOf("Eco2026*") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.width(76.dp).height(76.dp).background(EcoGreen, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("ER", color = androidx.compose.ui.graphics.Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("EcoRutas", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = EcoInk)
        Text("Recolección inteligente · EcoRecicla", color = MaterialTheme.colorScheme.secondary, fontSize = 15.sp)
        Spacer(Modifier.height(30.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) ErrorBanner(error)
                Button(
                    onClick = { onLogin(username, password) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                    else Text("Entrar a mi ruta")
                }
                Text("Demo: operario1 / Eco2026*", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Tu ruta, tus registros y la sincronización en un solo lugar.", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(session: Sesion, onLogout: () -> Unit) {
    val viewModel = remember { GlobalContext.get().get<DashboardViewModel>() }
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(0) }
    // Copy the delegated StateFlow value to a stable local before null checks.
    // Kotlin cannot smart-cast a delegated property such as state.route across accesses.
    val route = state.route
    val point = state.activePointId?.let { id ->
        route?.puntos?.firstOrNull { it.id == id } ?: state.points.firstOrNull { it.id == id }
    }
    val plannedPoint = point != null && route?.puntos?.any { it.id == point.id } == true
    val routeOpen = route?.let { !it.estado.equals("CERRADA", ignoreCase = true) } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("EcoRutas", fontWeight = FontWeight.Bold)
                        Text("Hola, ${session.nombre.substringBefore(' ')}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                actions = {
                    if (state.pendingCount > 0) {
                        Surface(color = androidx.compose.ui.graphics.Color(0xFFFFE8C4), shape = CircleShape, modifier = Modifier.padding(end = 6.dp)) {
                            Text("${state.pendingCount} pend.", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = EcoInk)
                        }
                    }
                    TextButton(onClick = viewModel::synchronize, enabled = !state.busy) { Text("Sync") }
                    TextButton(onClick = onLogout) { Text("Salir") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EcoBackground)
            )
        },
        bottomBar = {
            if (point == null) {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂") }, label = { Text("Ruta") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("▤") }, label = { Text("Puntos") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("●") }, label = { Text("Perfil") })
                }
            }
        },
        containerColor = EcoBackground
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.error != null) {
                ErrorBanner(state.error!!, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
            }
            if (state.message != null) {
                SuccessBanner(state.message!!, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
            }
            if (state.busy) LinearBusy()
            if (point != null) {
                PointDetailScreen(
                    point = point,
                    session = session,
                    state = state,
                    canRegister = plannedPoint && routeOpen && "PESAJE_CREAR" in session.privilegios,
                    canOmit = route?.let { currentRoute ->
                        currentRoute.puntos.any { it.id == point.id } &&
                            !currentRoute.estado.equals("CERRADA", ignoreCase = true) &&
                            !point.atendido && "RUTA_CERRAR" in session.privilegios
                    } == true,
                    onBack = viewModel::closePoint,
                    onRefresh = { viewModel.openPoint(point.id) },
                    onRegister = { input -> viewModel.registerWeighing(point.id, input) },
                    onCorrect = viewModel::correctWeighing,
                    onVoid = viewModel::voidWeighing,
                    onOmit = { reason -> route?.id?.let { routeId -> viewModel.omitPoint(routeId, point.id, reason) } }
                )
            } else {
                when (tab) {
                    0 -> RouteScreen(session, state, viewModel, onPoint = { viewModel.openPoint(it.id) })
                    1 -> PointsScreen(session, state, viewModel, onPoint = { viewModel.openPoint(it.id) })
                    else -> ProfileScreen(session, onLogout)
                }
            }
        }
    }
}

@Composable
private fun RouteScreen(
    session: Sesion,
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    onPoint: (PuntoAcopio) -> Unit
) {
    val route = state.route
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Ruta del día", "${route?.fecha ?: "Sin ruta descargada"} · ${route?.operario ?: session.usuario}")
        if (route == null && state.loading) {
            LoadingScreen("Cargando ruta guardada…")
        } else if (route == null) {
            EmptyCard("Todavía no hay una ruta en el almacenamiento local.", "Conéctate al backend y pulsa Actualizar.")
            Button(onClick = { viewModel.refreshAll() }, modifier = Modifier.fillMaxWidth()) { Text("Actualizar desde servidor") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(if (route.estado.equals("CERRADA", true)) "CERRADA" else "ABIERTA", route.estado.equals("CERRADA", true))
                StatusChip("${route.puntos.count { it.atendido }} / ${route.puntos.size} atendidos", false)
            }
            Text("Toca un punto para registrar o revisar sus pesajes.", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
            route.puntos.forEach { point -> PointCard(point, onClick = { onPoint(point) }) }
            if ("RUTA_CERRAR" in session.privilegios && !route.estado.equals("CERRADA", true)) {
                Button(onClick = { viewModel.closeRoute(route.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Cerrar ruta")
                }
            }
            Text("Última sincronización: ${route.ultimaSincronizacion ?: "todavía no sincronizada"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        }
        state.syncResult?.let { SyncSummary(it.enviados, it.pendientes, it.mensaje) }
    }
}

@Composable
private fun PointsScreen(
    session: Sesion,
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    onPoint: (PuntoAcopio) -> Unit
) {
    var createOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Puntos de acopio", "Consulta el padrón sincronizado con EcoAPI.")
        OutlinedTextField(
            value = state.zoneFilter,
            onValueChange = viewModel::setZoneFilter,
            label = { Text("Filtrar por zona") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::refreshZone, enabled = !state.busy) { Text("Consultar filtro") }
            OutlinedButton(onClick = { viewModel.setZoneFilter(""); viewModel.refreshAll() }) { Text("Todos") }
        }
        if ("PUNTO_GESTIONAR" in session.privilegios) {
            Button(onClick = { createOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Crear punto de acopio") }
        }
        val filtered = state.points.filter { state.zoneFilter.isBlank() || it.zona.contains(state.zoneFilter.trim(), ignoreCase = true) }
        if (filtered.isEmpty()) EmptyCard("No hay puntos locales para mostrar.", "Sincroniza cuando tengas conexión.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.id }) { point -> PointCard(point, onClick = { onPoint(point) }) }
        }
    }
    if (createOpen) CreatePointDialog(onDismiss = { createOpen = false }, onSave = { point -> createOpen = false; viewModel.createPoint(point) })
}

@Composable
private fun PointDetailScreen(
    point: PuntoAcopio,
    session: Sesion,
    state: DashboardUiState,
    canRegister: Boolean,
    canOmit: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRegister: (RegistrarPesaje) -> Unit,
    onCorrect: (Pesaje, RegistrarPesaje) -> Unit,
    onVoid: (Pesaje, String) -> Unit,
    onOmit: (String) -> Unit
) {
    var editorOpen by remember(point.id) { mutableStateOf(false) }
    var editing by remember(point.id) { mutableStateOf<Pesaje?>(null) }
    var voidTarget by remember(point.id) { mutableStateOf<Pesaje?>(null) }
    var voidReason by remember(point.id) { mutableStateOf("") }
    var omitDialogOpen by remember(point.id) { mutableStateOf(false) }
    var omitReason by remember(point.id) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("‹ Volver") }
        SectionHeader(point.razonSocial, "${point.zona} · ${point.materialHabitual}")
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(point.direccion, color = MaterialTheme.colorScheme.secondary)
                Text(if (point.atendido) "Punto atendido en la ruta" else "Pendiente de atención", fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canRegister) Button(onClick = { editing = null; editorOpen = true }, modifier = Modifier.weight(1f)) { Text("Registrar") }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("Actualizar") }
        }
        if (canOmit) {
            OutlinedButton(onClick = { omitReason = ""; omitDialogOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Registrar motivo de omisión")
            }
        }
        SectionHeader("Pesajes", "${state.weighings.size} registro(s) local(es)")
        if (state.weighings.isEmpty()) EmptyCard("Aún no hay pesajes locales para este punto.", "La lista se actualiza desde la base local; usa Actualizar cuando haya conexión.")
        state.weighings.forEach { weight ->
            WeighingCard(
                weight = weight,
                canEdit = canEditWeight(session, weight, state.route),
                canVoid = "PESAJE_ANULAR" in session.privilegios && weight.estado !is EstadoPesaje.Anulado,
                onEdit = { editing = weight; editorOpen = true },
                onVoid = { voidTarget = weight; voidReason = "" }
            )
        }
    }
    if (editorOpen) {
        WeighingEditorDialog(
            initial = editing,
            busy = state.busy,
            onDismiss = { editorOpen = false },
            onSave = { input ->
                val old = editing
                editorOpen = false
                if (old == null) onRegister(input) else onCorrect(old, input)
            }
        )
    }
    if (voidTarget != null) {
        AlertDialog(
            onDismissRequest = { voidTarget = null },
            title = { Text("Anular pesaje") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("El registro se conservará en el historial con estado anulado.")
                    OutlinedTextField(value = voidReason, onValueChange = { voidReason = it }, label = { Text("Motivo") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(enabled = voidReason.isNotBlank() && !state.busy, onClick = {
                    val target = voidTarget!!
                    voidTarget = null
                    onVoid(target, voidReason)
                }) { Text("Confirmar anulación") }
            },
            dismissButton = { TextButton(onClick = { voidTarget = null }) { Text("Cancelar") } }
        )
    }
    if (omitDialogOpen) {
        AlertDialog(
            onDismissRequest = { omitDialogOpen = false },
            title = { Text("Omitir punto de la ruta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Indica por qué este punto no se pudo atender. El motivo quedará en la ruta.")
                    OutlinedTextField(value = omitReason, onValueChange = { omitReason = it }, label = { Text("Motivo de omisión") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(enabled = omitReason.isNotBlank() && !state.busy, onClick = {
                    val reason = omitReason.trim()
                    omitDialogOpen = false
                    onOmit(reason)
                }) { Text("Guardar motivo") }
            },
            dismissButton = { TextButton(onClick = { omitDialogOpen = false }) { Text("Cancelar") } }
        )
    }
}

private fun canEditWeight(session: Sesion, weight: Pesaje, route: pe.upeu.ecorecicla.domain.model.RutaDelDia?): Boolean {
    if ("PESAJE_EDITAR" !in session.privilegios || weight.estado is EstadoPesaje.Anulado) return false
    if (route?.id == weight.rutaId && route.estado.equals("CERRADA", ignoreCase = true)) return false
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
    // In this case, the previous-day route is closed; do not offer edits for its old weighings.
    if (weight.fechaHora.take(10) != today) return false
    if ("RUTA_CERRAR" in session.privilegios) return true // Supervisor may correct any weighing on today's open route.
    return weight.operario.equals(session.usuario, ignoreCase = true)
}

@Composable
private fun WeighingEditorDialog(
    initial: Pesaje?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (RegistrarPesaje) -> Unit
) {
    var material by remember(initial?.id) { mutableStateOf(initial?.material ?: "Papel") }
    var gross by remember(initial?.id) { mutableStateOf(initial?.pesoBruto?.toString() ?: "25.5") }
    var tare by remember(initial?.id) { mutableStateOf(initial?.tara?.toString() ?: "2.5") }
    var observation by remember(initial?.id) { mutableStateOf(initial?.observacion.orEmpty()) }
    var validation by remember(initial?.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Registrar pesaje" else "Corregir pesaje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = material, onValueChange = { material = it }, label = { Text("Material") }, singleLine = true)
                OutlinedTextField(value = gross, onValueChange = { gross = it }, label = { Text("Peso bruto (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = tare, onValueChange = { tare = it }, label = { Text("Tara (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = observation, onValueChange = { observation = it }, label = { Text("Observación (opcional)") }, minLines = 2)
                val net = (gross.toDoubleOrNull() ?: 0.0) - (tare.toDoubleOrNull() ?: 0.0)
                Text("Peso neto estimado: ${formatWeight(net)} kg", color = MaterialTheme.colorScheme.secondary)
                validation?.let { ErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                val grossValue = gross.replace(',', '.').toDoubleOrNull()
                val tareValue = tare.replace(',', '.').toDoubleOrNull()
                validation = when {
                    material.isBlank() -> "El material es obligatorio."
                    grossValue == null || tareValue == null -> "Escribe pesos numéricos válidos."
                    tareValue < 0 -> "La tara no puede ser negativa."
                    grossValue <= tareValue -> "El peso bruto debe ser mayor que la tara."
                    else -> null
                }
                if (validation == null) onSave(RegistrarPesaje(material, grossValue!!, tareValue!!, observation.ifBlank { null }))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CreatePointDialog(onDismiss: () -> Unit, onSave: (CrearPunto) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("Norte") }
    var material by remember { mutableStateOf("Papel") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo punto de acopio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("Razón social") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Dirección") }, singleLine = true)
                OutlinedTextField(zone, { zone = it }, label = { Text("Zona") }, singleLine = true)
                OutlinedTextField(material, { material = it }, label = { Text("Material habitual") }, singleLine = true)
                error?.let { ErrorBanner(it) }
            }
        },
        confirmButton = { TextButton(onClick = {
            error = if (name.isBlank() || address.isBlank() || zone.isBlank() || material.isBlank()) "Completa todos los campos." else null
            if (error == null) onSave(CrearPunto(name.trim(), address.trim(), zone.trim(), material.trim()))
        }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ProfileScreen(session: Sesion, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Mi perfil", "Sesión protegida con JWT")
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(session.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("@${session.usuario}", color = MaterialTheme.colorScheme.secondary)
                StatusChip(session.rol, false)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Privilegios", fontWeight = FontWeight.SemiBold)
                session.privilegios.sorted().forEach { Text("✓  $it", color = EcoGreen, fontSize = 14.sp) }
                Text("Token válido durante ${session.expiraEnSegundos / 60} min al iniciar sesión.", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
            }
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión y limpiar el equipo") }
        Text("Los permisos se validan en el backend. Ocultar un botón en esta pantalla no sustituye la seguridad del servidor.", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
    }
}

@Composable
private fun PointCard(point: PuntoAcopio, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(44.dp).height(44.dp).background(EcoGreenLight, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(if (point.secuencia > 0) point.secuencia.toString() else "•", color = EcoGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(point.razonSocial, fontWeight = FontWeight.SemiBold, color = EcoInk)
                Text(point.direccion, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, maxLines = 2)
                Text("${point.zona} · ${point.materialHabitual}", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
            }
            StatusChip(if (point.atendido) "Listo" else "Pendiente", point.atendido)
        }
    }
}

@Composable
private fun WeighingCard(
    weight: Pesaje,
    canEdit: Boolean,
    canVoid: Boolean,
    onEdit: () -> Unit,
    onVoid: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Pesaje #${weight.id}", fontWeight = FontWeight.SemiBold)
                StatusChip(
                    when {
                        weight.pendiente -> "Pendiente sync"
                        weight.estado is EstadoPesaje.Anulado -> "Anulado"
                        else -> "Registrado"
                    },
                    weight.estado is EstadoPesaje.Registrado && !weight.pendiente
                )
            }
            Text("${weight.material} · bruto ${formatWeight(weight.pesoBruto)} kg · tara ${formatWeight(weight.tara)} kg")
            Text("Neto: ${formatWeight(weight.pesoNeto)} kg", color = EcoGreen, fontWeight = FontWeight.Bold)
            Text("${weight.operario} · ${weight.fechaHora}", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
            weight.observacion?.takeIf { it.isNotBlank() }?.let { Text("Nota: $it", fontSize = 12.sp) }
            if (weight.estado is EstadoPesaje.Anulado) Text("Motivo: ${(weight.estado as EstadoPesaje.Anulado).motivo}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            if ((canEdit && weight.estado !is EstadoPesaje.Anulado) || canVoid) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canEdit && weight.estado !is EstadoPesaje.Anulado) OutlinedButton(onClick = onEdit) { Text("Corregir") }
                    if (canVoid) TextButton(onClick = onVoid) { Text("Anular") }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = EcoInk)
        Text(subtitle, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
    }
}

@Composable
private fun StatusChip(text: String, positive: Boolean) {
    val bg = if (positive) EcoGreenLight else androidx.compose.ui.graphics.Color(0xFFFFEBCB)
    val fg = if (positive) EcoGreen else androidx.compose.ui.graphics.Color(0xFF855315)
    Surface(color = bg, shape = CircleShape) {
        Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
    }
}

@Composable
private fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = androidx.compose.ui.graphics.Color(0xFFFFE7E6), shape = RoundedCornerShape(12.dp)) {
        Text(message, modifier = Modifier.padding(12.dp), color = androidx.compose.ui.graphics.Color(0xFF9C2925), fontSize = 13.sp)
    }
}

@Composable
private fun SuccessBanner(message: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = EcoGreenLight, shape = RoundedCornerShape(12.dp)) {
        Text(message, modifier = Modifier.padding(12.dp), color = EcoGreen, fontSize = 13.sp)
    }
}

@Composable
private fun EmptyCard(title: String, description: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun LoadingScreen(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = EcoGreen)
            Text(text, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun LinearBusy() {
    androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EcoGreen)
}

@Composable
private fun SyncSummary(sent: Int, pending: Int, message: String) {
    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Resumen de sincronización", fontWeight = FontWeight.SemiBold)
            Text("Enviados: $sent · Pendientes: $pending", color = EcoGreen)
            Text(message, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        }
    }
}

private fun formatWeight(value: Double): String = (kotlin.math.round(value * 100.0) / 100.0).toString()
