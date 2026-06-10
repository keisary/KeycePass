package com.ak.keycepass.desktop.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.keycepass.desktop.ui.theme.*
import com.ak.keycepass.desktop.ui.viewmodel.AdminViewModel
import com.ak.keycepass.desktop.ui.viewmodel.AttendanceRow
import com.ak.keycepass.shared.domain.model.StatutFinal
import com.ak.keycepass.shared.domain.model.StatutSeance
import kotlinx.coroutines.delay

// ── Toast notification ──

@Composable
private fun LiveToast(message: String, visible: Boolean, onDismiss: () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
        modifier = Modifier.offset { androidx.compose.ui.unit.IntOffset(0, 0) }
    ) {
        LaunchedEffect(Unit) {
            delay(3000)
            onDismiss()
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusPresent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(message, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Dashboard Principal ──

@Composable
fun DashboardScreen(viewModel: AdminViewModel = remember { AdminViewModel() }) {
    val state by viewModel.state.collectAsState()

    var toastMessage by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.liveEvents.collect { event ->
            toastMessage = event
            toastVisible = true
        }
    }

    var simEnabled by remember { mutableStateOf(true) }  // Auto-demarrage temps reel

    LaunchedEffect(simEnabled) {
        if (simEnabled) {
            while (true) {
                delay(5000 + (Math.random() * 10000).toLong())
                viewModel.simulateArrivee()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            "Apercu de la seance",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            state.enseignant.matiereCourante,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    if (simEnabled) {
                        Surface(
                            shape = CircleShape,
                            color = StatusPresent.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusPresent)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusPresent)
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (state.seanceStatut) {
                        StatutSeance.PLANIFIE -> MaterialTheme.colorScheme.surfaceVariant
                        StatutSeance.EN_COURS -> StatusPresent.copy(alpha = 0.1f)
                        StatutSeance.CLOTURE_ENSEIGNANT -> InfoBlue.copy(alpha = 0.1f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(
                                when (state.seanceStatut) {
                                    StatutSeance.PLANIFIE -> StatusPending
                                    StatutSeance.EN_COURS -> StatusPresent
                                    StatutSeance.CLOTURE_ENSEIGNANT -> InfoBlue
                                }
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (state.seanceStatut) {
                                StatutSeance.PLANIFIE -> "Planifiee"
                                StatutSeance.EN_COURS -> "En cours"
                                StatutSeance.CLOTURE_ENSEIGNANT -> "Cloturee"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = when (state.seanceStatut) {
                                StatutSeance.PLANIFIE -> MaterialTheme.colorScheme.onSurfaceVariant
                                StatutSeance.EN_COURS -> StatusPresent
                                StatutSeance.CLOTURE_ENSEIGNANT -> InfoBlue
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // KPIs — compacts + cliquables
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCardCompact(state.presents, if (state.total > 0) "${state.presents * 100 / state.total}%" else "0%",
                    Icons.Default.CheckCircle, StatusPresent, Modifier.weight(1f),
                    isActive = state.statutFilter == StatutFinal.PRESENT,
                    onClick = { viewModel.filterByStatut(if (state.statutFilter == StatutFinal.PRESENT) null else StatutFinal.PRESENT) })
                KpiCardCompact(state.lates, if (state.total > 0) "${state.lates * 100 / state.total}%" else "0%",
                    Icons.Default.Schedule, StatusLate, Modifier.weight(1f),
                    isActive = state.statutFilter == StatutFinal.RETARD,
                    onClick = { viewModel.filterByStatut(if (state.statutFilter == StatutFinal.RETARD) null else StatutFinal.RETARD) })
                KpiCardCompact(state.absents, if (state.total > 0) "${state.absents * 100 / state.total}%" else "0%",
                    Icons.Default.Cancel, StatusAbsent, Modifier.weight(1f),
                    isActive = state.statutFilter == StatutFinal.ABSENT,
                    onClick = { viewModel.filterByStatut(if (state.statutFilter == StatutFinal.ABSENT) null else StatutFinal.ABSENT) })
                KpiCardCompact(state.total, "Total",
                    Icons.Default.People, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f),
                    isActive = false,
                    onClick = { viewModel.filterByStatut(null) })
            }

            Spacer(Modifier.height(12.dp))

            // Filtres — scrollables
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    state.classes.forEach { c ->
                        FilterChip(
                            selected = state.selectedClasse == c,
                            onClick = { viewModel.filterByClasse(c) },
                            label = { Text(c, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true, selected = state.selectedClasse == c
                            )
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    state.semestres.forEach { s ->
                        FilterChip(
                            selected = state.selectedSemestre == s,
                            onClick = { viewModel.filterBySemestre(s) },
                            label = { Text(s, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                enabled = true, selected = state.selectedSemestre == s
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { simEnabled = !simEnabled },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (simEnabled) StatusPresent.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (simEnabled) "Stop" else "Live", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.rafraichir() },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rafr.", fontSize = 12.sp)
                    }

                    if (state.seanceStatut == StatutSeance.EN_COURS) {
                        Button(
                            onClick = { viewModel.cloturerSeance() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusAbsent, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clot.", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Table - remplit l'espace restant
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", Modifier.width(30.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Matricule", Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Etudiant", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Statut", Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Arrivee", Modifier.width(80.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Depart", Modifier.width(80.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.rows, key = { it.id }) { row ->
                            AttendanceRowItemCompact(row)
                        }
                        if (state.rows.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.SearchOff, contentDescription = null,
                                            modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Aucune donnee pour ce filtre",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Toast overlay
        Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
            LiveToast(message = toastMessage, visible = toastVisible, onDismiss = { toastVisible = false })
        }
    }
}

// ── KPI Card compact ──

@Composable
private fun KpiCardCompact(
    value: Int,
    subtitle: String,
    icon: ImageVector,
    valueColor: Color,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.height(80.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isActive) 3.dp else 1.dp,
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, valueColor.copy(alpha = 0.3f)) else null
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = valueColor.copy(alpha = if (isActive) 1f else 0.6f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                val animValue by animateIntAsState(
                    targetValue = value,
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    label = "kpi-$value"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$animValue", fontWeight = FontWeight.Bold, color = valueColor, fontSize = 22.sp)
                    if (isActive) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(valueColor))
                    }
                }
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Table Row compact ──

@Composable
private fun AttendanceRowItemCompact(row: AttendanceRow) {
    val statutColor = when (row.statut) {
        StatutFinal.PRESENT -> StatusPresent
        StatutFinal.RETARD -> StatusLate
        StatutFinal.ABSENT -> StatusAbsent
        StatutFinal.EN_ATTENTE -> StatusPending
    }
    val statutBg = when (row.statut) {
        StatutFinal.PRESENT -> StatusPresentBg
        StatutFinal.RETARD -> StatusLateBg
        StatutFinal.ABSENT -> StatusAbsentBg
        StatutFinal.EN_ATTENTE -> Color.DarkGray.copy(alpha = 0.1f)
    }
    val statutLabel = when (row.statut) {
        StatutFinal.PRESENT -> "Present"
        StatutFinal.RETARD -> "Retard"
        StatutFinal.ABSENT -> "Absent"
        StatutFinal.EN_ATTENTE -> "En attente"
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${row.id}", Modifier.width(30.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(row.matricule, Modifier.width(90.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Column(Modifier.weight(1f)) {
            Text("${row.nom} ${row.prenom}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }

        Surface(Modifier.width(90.dp), shape = RoundedCornerShape(6.dp), color = statutBg) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(statutColor))
                Spacer(Modifier.width(4.dp))
                Text(statutLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statutColor, textAlign = TextAlign.Center)
            }
        }

        Text(row.heureScanDebut, Modifier.width(80.dp), fontSize = 12.sp,
            color = if (row.heureScanDebut == "---") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface)
        Text(row.heureScanFin, Modifier.width(80.dp), fontSize = 12.sp,
            color = if (row.heureScanFin == "---") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), thickness = 0.5.dp)
}
