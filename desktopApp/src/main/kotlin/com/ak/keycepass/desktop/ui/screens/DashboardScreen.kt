package com.ak.keycepass.desktop.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    var simEnabled by remember { mutableStateOf(false) }

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
                            state.matiere,
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

            Spacer(Modifier.height(24.dp))

            // KPIs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard("Presence", state.presents, if (state.total > 0) "${state.presents * 100 / state.total}%" else "0%",
                    Icons.Default.CheckCircle, StatusPresent, Modifier.weight(1f))
                KpiCard("Retard", state.lates, if (state.total > 0) "${state.lates * 100 / state.total}%" else "0%",
                    Icons.Default.Schedule, StatusLate, Modifier.weight(1f))
                KpiCard("Absence", state.absents, if (state.total > 0) "${state.absents * 100 / state.total}%" else "0%",
                    Icons.Default.Cancel, StatusAbsent, Modifier.weight(1f))
                KpiCard("Inscrits", state.total, "Total",
                    Icons.Default.People, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // Filtres + Actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Dropdown classe
                    state.classes.forEach { c ->
                        FilterChip(
                            selected = state.selectedClasse == c,
                            onClick = { viewModel.filterByClasse(c) },
                            label = { Text(c, fontSize = 13.sp) },
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
                    state.semestres.forEach { s ->
                        FilterChip(
                            selected = state.selectedSemestre == s,
                            onClick = { viewModel.filterBySemestre(s) },
                            label = { Text(s, fontSize = 13.sp) },
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { simEnabled = !simEnabled },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (simEnabled) StatusPresent.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (simEnabled) "Arreter simulation" else "Simulation live", fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.rafraichir() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Rafraichir", fontSize = 13.sp)
                    }

                    if (state.seanceStatut == StatutSeance.EN_COURS) {
                        Button(
                            onClick = { viewModel.cloturerSeance() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusAbsent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cloturer", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stats bar
            if (state.total > 0) {
                val presentPct = state.presents.toFloat() / state.total
                val latePct = state.lates.toFloat() / state.total
                val absentPct = state.absents.toFloat() / state.total

                val animP by animateFloatAsState(presentPct, tween(400), label = "bar-p")
                val animL by animateFloatAsState(latePct, tween(400), label = "bar-l")
                val animA by animateFloatAsState(absentPct, tween(400), label = "bar-a")

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(animP.coerceAtLeast(0.01f)).height(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(StatusPresent.copy(alpha = 0.3f)))
                    Box(Modifier.weight(animL.coerceAtLeast(0.01f)).height(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)))
                    Box(Modifier.weight(animA.coerceAtLeast(0.01f)).height(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(StatusAbsent.copy(alpha = 0.3f)))
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${state.presents} presents", fontSize = 11.sp, color = StatusPresent)
                    Text("${state.lates} retards", fontSize = 11.sp, color = StatusLate)
                    Text("${state.absents} absents", fontSize = 11.sp, color = StatusAbsent)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Table
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", Modifier.width(36.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Matricule", Modifier.width(100.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Etudiant", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Statut", Modifier.width(110.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1er Scan", Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("2nd Scan", Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.rows, key = { it.id }) { row ->
                            AttendanceRowItem(row)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), thickness = 0.5.dp)
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

// ── KPI Card ──

@Composable
private fun KpiCard(title: String, value: Int, subtitle: String, icon: ImageVector, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Icon(icon, contentDescription = title, tint = valueColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }

            val animValue by animateIntAsState(
                targetValue = value,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "kpi-$title"
            )
            Column {
                Text("$animValue", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                    color = valueColor, fontSize = 32.sp)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Table Row ──

@Composable
private fun AttendanceRowItem(row: AttendanceRow) {
    var hovered by remember { mutableStateOf(false) }
    val bgAlpha by animateFloatAsState(if (hovered) 0.06f else 0f, tween(200), label = "hover-bg")

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
            .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha))
            .clickable { hovered = !hovered }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${row.id}", Modifier.width(36.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(row.matricule, Modifier.width(100.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Column(Modifier.weight(1f)) {
            Text("${row.nom} ${row.prenom}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }

        Surface(Modifier.width(110.dp), shape = RoundedCornerShape(8.dp), color = statutBg) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(statutColor))
                Spacer(Modifier.width(6.dp))
                Text(statutLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statutColor, textAlign = TextAlign.Center)
            }
        }

        Text(row.heureScanDebut, Modifier.width(90.dp), fontSize = 13.sp,
            color = if (row.heureScanDebut == "---") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface)
        Text(row.heureScanFin, Modifier.width(90.dp), fontSize = 13.sp,
            color = if (row.heureScanFin == "---") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface)
    }
}
