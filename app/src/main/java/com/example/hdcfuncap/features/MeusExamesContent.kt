package com.example.hdcfuncap.features

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hdcfuncap.components.HdcPullToRefresh
import com.example.hdcfuncap.network.*
import com.example.hdcfuncap.storage.UserPreferences
import com.example.hdcfuncap.ui.theme.*

private enum class FiltroExames(val titulo: String) { TODOS("Todos"), AGENDADOS("Agendados"), PUBLICADOS("Publicados") }

@Composable
internal fun MeusExamesContent(innerPadding: PaddingValues, onBack: () -> Unit) = HdcFuncapTheme(darkTheme = false) {
    val context = LocalContext.current.applicationContext
    val api = remember(context) { RetrofitClient.getAuthApi(context) }
    val model: ExamesViewModel = viewModel(factory = ExamesViewModelFactory(api, context))
    val state by model.state.collectAsState()
    val preferences = remember(context) { UserPreferences(context) }
    val patientId by preferences.pacienteId.collectAsState(initial = null)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    var filter by rememberSaveable { mutableStateOf(FiltroExames.TODOS) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedPatient by rememberSaveable { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    var pendingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingPatient by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        pendingUri = uri?.toString()
        if (uri == null) pendingId = null
    }

    fun requestDownload(exame: ExameResponse) {
        pendingId = exame.id
        pendingPatient = patientId
        try { createDocument.launch(exame.nomePdf()) } catch (_: RuntimeException) {
            pendingId = null
            model.informarMensagem("Não foi possível abrir o seletor de arquivos do aparelho.")
        }
    }

    BackHandler(enabled = state.pdf != null || selectedId != null) {
        if (state.pdf != null) model.fecharPdf() else selectedId = null
    }
    val selectedExam = state.exames.firstOrNull { it.id == selectedId && it.pacienteId == patientId }
    LaunchedEffect(patientId, selectedId, state.exames, state.carregado, state.carregando, state.erro) {
        if (selectedId != null && patientId != null) {
            if (selectedPatient != patientId) {
                selectedId = null
            } else if (state.carregado && !state.carregando && state.erro == null && selectedExam == null) {
                selectedId = null
                model.informarMensagem("Este exame não está mais disponível. A lista foi atualizada.")
            }
        }
    }

    LaunchedEffect(patientId) { model.carregar(patientId) }
    DisposableEffect(lifecycle, patientId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.carregar(patientId)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    DisposableEffect(model) { onDispose { model.sair() } }
    LaunchedEffect(state.mensagem) {
        state.mensagem?.let {
            snackbar.showSnackbar(it)
            model.consumirMensagem()
        }
    }
    LaunchedEffect(pendingUri, patientId, state.exames, state.carregado) {
        val uri = pendingUri ?: return@LaunchedEffect
        val id = pendingId ?: return@LaunchedEffect
        val patient = patientId ?: return@LaunchedEffect
        if (pendingPatient != patient || (state.carregado && state.exames.none { it.id == id && it.situacao() == SituacaoExame.PUBLICADO })) {
            pendingUri = null
            pendingId = null
            snackbar.showSnackbar("Este exame não está disponível. Atualize a lista.")
        } else if (state.carregado) {
            pendingUri = null
            pendingId = null
            model.baixar(id, Uri.parse(uri))
        }
    }
    val filtered = state.exames.filter {
        when (filter) {
            FiltroExames.TODOS -> true
            FiltroExames.AGENDADOS -> it.situacao() == SituacaoExame.AGENDADO
            FiltroExames.PUBLICADOS -> it.situacao() == SituacaoExame.PUBLICADO
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize().padding(innerPadding), containerColor = HdcBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        HdcPullToRefresh(state.carregando, { model.carregar(patientId, atualizar = true) },
            Modifier.fillMaxSize().padding(padding)) {
            if (selectedId != null) {
                ExameDetalhesContent(
                    exame = selectedExam,
                    loading = state.arquivoEmAndamento == selectedId,
                    enabled = state.arquivoEmAndamento == null && pendingId == null,
                    error = state.erro,
                    onBack = { selectedId = null },
                    onRefresh = { model.carregar(patientId, atualizar = true) },
                    onView = { selectedExam?.id?.let(model::abrir) },
                    onDownload = { selectedExam?.let(::requestDownload) }
                )
            } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp),
                state = listState,
                contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = HdcText) }
                        Text("Meus exames", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = HdcText)
                    }
                    Text("Acompanhe seus laudos em PDF.", color = HdcTextSecondary, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FiltroExames.entries.forEach { option ->
                            FilterChip(selected = filter == option, onClick = { filter = option },
                                label = { Text(option.titulo) }, shape = RoundedCornerShape(50), border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = HdcBlueLight, labelColor = HdcBlueDark,
                                    selectedContainerColor = HdcBlue, selectedLabelColor = Color.White))
                        }
                    }
                }
                state.erro?.let { message ->
                    item {
                        Text(message, color = HdcRed, fontSize = 14.sp)
                        TextButton(onClick = { model.carregar(patientId, atualizar = true) }, enabled = !state.carregando) {
                            Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tentar novamente")
                        }
                    }
                }
                if (state.carregando && state.exames.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                } else if (filtered.isEmpty() && state.erro == null) {
                    item {
                        Text(when (filter) {
                            FiltroExames.TODOS -> "Nenhum exame disponível no momento."
                            FiltroExames.AGENDADOS -> "Nenhum exame agendado disponível."
                            FiltroExames.PUBLICADOS -> "Nenhum exame publicado disponível."
                        }, color = HdcTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 24.dp))
                    }
                }
                items(filtered, key = { it.id!! }) { exame ->
                    ExameCard(exame, loading = state.arquivoEmAndamento == exame.id,
                        enabled = state.arquivoEmAndamento == null && pendingId == null,
                        onOpen = {
                            selectedPatient = patientId
                            selectedId = exame.id
                        },
                        onView = { exame.id?.let(model::abrir) },
                        onDownload = { requestDownload(exame) })
                }
            }
            }
        }
    }
    state.pdf?.let { ExamePdfDialog(it, onClose = model::fecharPdf) }
}

@Composable
private fun ExameCard(
    exame: ExameResponse,
    loading: Boolean,
    enabled: Boolean,
    onView: () -> Unit,
    onDownload: () -> Unit,
    onOpen: (() -> Unit)? = null,
    details: Boolean = false
) {
    val published = exame.situacao() == SituacaoExame.PUBLICADO
    val accent = if (published) Color(0xFF16894F) else Color(0xFFB85E00)
    val availableAt = formatarDisponibilidadeExame(if (published) exame.publicadoEm ?: exame.disponibilizacaoEm else exame.disponibilizacaoEm)
    val modifier = if (onOpen != null) Modifier.fillMaxWidth().clickable(
        role = Role.Button, onClickLabel = "Ver detalhes do exame", onClick = onOpen
    ) else Modifier.fillMaxWidth()
    Card(modifier, shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = HdcSurface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(if (details) 20.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(if (details) 20.dp else 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(if (details) 54.dp else 42.dp).background(HdcBlueLight, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PictureAsPdf, null, tint = HdcBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(exame.titulo, Modifier.weight(1f), fontSize = if (details) 18.sp else 16.sp, fontWeight = FontWeight.SemiBold,
                            color = HdcText, maxLines = if (details) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                        Text(if (published) "Publicado" else "Agendado", fontSize = 11.sp, color = accent,
                            modifier = Modifier.background(if (published) Color(0xFFE8F7ED) else Color(0xFFFFF3E0), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                    ExameInfo(Icons.Outlined.CalendarToday, "Realizado em ${formatarColetaExame(exame.dataColeta)}", HdcTextSecondary,
                        fontSize = if (details) 14.sp else 12.sp)
                    ExameInfo(Icons.Outlined.Schedule, availableAt?.let { if (published) "Disponível desde $it" else "Disponível em $it" }
                        ?: if (published) "PDF disponível" else "Aguardando liberação", accent,
                        fontSize = if (details) 14.sp else 12.sp)
                    exame.laboratorio?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, color = HdcTextSecondary) }
                    exame.observacao?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 13.sp, color = HdcTextSecondary) }
                }
            }
            if (details) {
                HorizontalDivider(color = Color(0xFFEAF1FF))
                Text(if (published) "Laudo em PDF disponível para visualização e download."
                    else "O laudo em PDF estará disponível após a liberação.",
                    color = HdcTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (published) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onView, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        border = BorderStroke(1.dp, if (enabled) HdcBlue else MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HdcBlue),
                        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
                        Icon(Icons.Outlined.Visibility, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ver PDF", fontSize = 13.sp)
                    }
                    Button(onClick = onDownload, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
                        Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Baixar", fontSize = 13.sp)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF1F3F6), RoundedCornerShape(8.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp), tint = HdcTextSecondary)
                    Spacer(Modifier.width(8.dp))
                    Text(availableAt?.let { "PDF disponível em $it" } ?: "PDF aguardando liberação", fontSize = 12.sp, color = HdcTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ExameInfo(icon: ImageVector, text: String, color: Color, fontSize: TextUnit = 12.sp) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(14.dp).padding(top = 1.dp), tint = HdcTextSecondary)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = fontSize, lineHeight = fontSize * 1.4f, color = color)
    }
}

@Composable
private fun ExameDetalhesContent(
    exame: ExameResponse?,
    loading: Boolean,
    enabled: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onView: () -> Unit,
    onDownload: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar para meus exames", tint = HdcText) }
                Text("Detalhes do exame", Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = HdcText)
            }
        }
        error?.let {
            item {
                Text(it, color = HdcRed, fontSize = 14.sp)
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tentar novamente")
                }
            }
        }
        if (exame == null && error == null) {
            item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        exame?.let {
            item {
                ExameCard(it, loading, enabled, onView, onDownload, details = true)
            }
            item {
                Row(Modifier.fillMaxWidth().background(HdcBlueLight, RoundedCornerShape(8.dp)).padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, null, tint = HdcBlue, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Sobre os exames", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = HdcText)
                        Text("Nesta primeira versão, você tem acesso apenas ao laudo do exame em PDF.",
                            fontSize = 14.sp, lineHeight = 20.sp, color = HdcTextSecondary)
                    }
                }
            }
        }
    }
}
