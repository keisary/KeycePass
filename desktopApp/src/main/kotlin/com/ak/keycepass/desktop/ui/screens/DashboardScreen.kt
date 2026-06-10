package com.ak.keycepass.desktop.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.keycepass.desktop.ui.theme.*
import com.ak.keycepass.desktop.ui.viewmodel.AdminViewModel
import com.ak.keycepass.desktop.ui.viewmodel.AttendanceRow
import com.ak.keycepass.shared.domain.model.StatutFinal
import com.ak.keycepass.shared.domain.model.StatutSeance
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ── Dashboard ──

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

    var simEnabled by remember { mutableStateOf(true) }

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

            // ── Header compact ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        state.enseignant.matiereCourante,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Live dot
                if (simEnabled) {
                    Surface(
                        shape = CircleShape,
                        color = StatusPresent.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(StatusPresent))
                            Spacer(Modifier.width(6.dp))
                            Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusPresent)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // Statut seance badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (state.seanceStatut) {
                        StatutSeance.PLANIFIE -> MaterialTheme.colorScheme.surfaceVariant
                        StatutSeance.EN_COURS -> StatusPresent.copy(alpha = 0.1f)
                        StatutSeance.CLOTURE_ENSEIGNANT -> InfoBlue.copy(alpha = 0.1f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(
                            when (state.seanceStatut) {
                                StatutSeance.PLANIFIE -> StatusPending
                                StatutSeance.EN_COURS -> StatusPresent
                                StatutSeance.CLOTURE_ENSEIGNANT -> InfoBlue
                            }
                        ))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when (state.seanceStatut) {
                                StatutSeance.PLANIFIE -> "Planifiee"
                                StatutSeance.EN_COURS -> "En cours"
                                StatutSeance.CLOTURE_ENSEIGNANT -> "Cloturee"
                            },
                            fontSize = 11.sp,
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

            Spacer(Modifier.height(12.dp))

            // ── KPIs compacts + Filtres sur UNE ligne ──
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // KPIs
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KpiMini(state.presents, Icons.Default.CheckCircle, StatusPresent,
                        isActive = state.statutFilter == StatutFinal.PRESENT,
                        onClick = { viewModel.filterByStatut(if (state.statutFilter == StatutFinal.PRESENT) null else StatutFinal.PRESENT) })
                    KpiMini(state.lates, Icons.Default.Schedule, StatusLate,
                        isActive = state.statutFilter == StatutFinal.RETARD,
                        onClick = { viewModel.filterByStatut(if (state.statutFilter == StatutFinal.RETARD) null else StatutFinal.RETARD) })
                    KpiMini(state.absents, Icons.Default.Cancel, StatusAbsent,
                        isActive = state.statutFilter == StatutFinal.ABSENT,
                        onClick = { viewModel.filterByStatut(if (state.statutFilter == StatutFinal.ABSENT) null else StatutFinal.ABSENT) })
                    KpiMini(state.total, Icons.Default.People, MaterialTheme.colorScheme.onSurface,
                        isActive = false, onClick = { viewModel.filterByStatut(null) })
                }

                Spacer(Modifier.width(8.dp))

                // Actions : classe / semestre / refresh / cloture
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        state.classes.forEach { c ->
                            FilterChip(
                                selected = state.selectedClasse == c,
                                onClick = { viewModel.filterByClasse(c) },
                                label = { Text(c, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    enabled = true, selected = state.selectedClasse == c
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        // Separator
                        Box(Modifier.width(1.dp).height(18.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))

                        state.semestres.forEach { s ->
                            FilterChip(
                                selected = state.selectedSemestre == s,
                                onClick = { viewModel.filterBySemestre(s) },
                                label = { Text(s, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    enabled = true, selected = state.selectedSemestre == s
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        Box(Modifier.width(1.dp).height(18.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))

                        IconButton(onClick = { simEnabled = !simEnabled }, modifier = Modifier.size(26.dp)) {
                            Icon(
                                if (simEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (simEnabled) StatusPresent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.rafraichir() }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (state.seanceStatut == StatutSeance.EN_COURS) {
                            IconButton(onClick = { viewModel.cloturerSeance() }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp),
                                    tint = StatusAbsent)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Tableau des presences ──
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Header compact
                    Row(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", Modifier.width(28.dp), fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Etudiant", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Statut", Modifier.width(72.dp), fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Arrivee", Modifier.width(52.dp), fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Depart", Modifier.width(52.dp), fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), thickness = 1.dp)

                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.rows, key = { it.id }) { row ->
                            StudentRow(row)
                        }
                        if (state.rows.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.SearchOff, contentDescription = null,
                                            modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                        Spacer(Modifier.height(6.dp))
                                        Text("Aucune donnee", fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Toast
        if (toastVisible) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                LiveToast(message = toastMessage, onDismiss = { toastVisible = false })
            }
        }
    }
}

// ── Toast ──

@Composable
private fun LiveToast(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { delay(3000); onDismiss() }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 6.dp
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusPresent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(message, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("OK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ── KPI Mini ──

@Composable
private fun KpiMini(
    value: Int, icon: ImageVector, valueColor: Color,
    isActive: Boolean = false, onClick: () -> Unit = {}
) {
    val animValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(350),
        label = "kpi-$value"
    )
    Surface(
        modifier = Modifier.height(48.dp).width(72.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) valueColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, valueColor.copy(alpha = 0.2f)) else null
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null,
                tint = valueColor.copy(alpha = if (isActive) 1f else 0.5f),
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("$animValue", fontWeight = FontWeight.Bold, color = valueColor, fontSize = 16.sp)
        }
    }
}

// ── Ligne etudiant ──

@Composable
private fun StudentRow(row: AttendanceRow) {
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
        StatutFinal.EN_ATTENTE -> "Attente"
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${row.id}", Modifier.width(28.dp), fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))

        Column(Modifier.weight(1f)) {
            Text("${row.nom} ${row.prenom}", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(row.matricule, fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
        Spacer(Modifier.width(4.dp))

        // Badge statut compact
        Surface(Modifier.width(72.dp), shape = RoundedCornerShape(4.dp), color = statutBg) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(statutColor))
                Spacer(Modifier.width(3.dp))
                Text(statutLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = statutColor, textAlign = TextAlign.Center)
            }
        }

        Text(row.heureScanDebut, Modifier.width(52.dp), fontSize = 11.sp,
            color = if (row.heureScanDebut == "---") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.onSurface)
        Text(row.heureScanFin, Modifier.width(52.dp), fontSize = 11.sp,
            color = if (row.heureScanFin == "---") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f), thickness = 0.5.dp)
}
