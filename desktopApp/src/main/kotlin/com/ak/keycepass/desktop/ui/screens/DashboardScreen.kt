package com.ak.keycepass.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.keycepass.desktop.ui.theme.*
import com.ak.keycepass.desktop.ui.viewmodel.AdminViewModel
import com.ak.keycepass.desktop.ui.viewmodel.AttendanceRow
import com.ak.keycepass.shared.domain.model.StatutFinal
import com.ak.keycepass.shared.domain.model.StatutSeance

// ── KPI Cards avec compteurs animés ──

@Composable
fun AnimatedKpiCard(
    title: String,
    value: Int,
    subtitle: String,
    icon: ImageVector,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "kpi-${title}"
    )

    Surface(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Icon(
                    icon,
                    contentDescription = title,
                    tint = valueColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    "${animatedValue}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    fontSize = 32.sp
                )
                // Subtitle fade when value changes
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(600))
                ) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Statistiques barre animée ──

@Composable
fun AnimatedStatBar(
    presentPct: Float,
    latePct: Float,
    absentPct: Float
) {
    val animPresent by animateFloatAsState(
        targetValue = presentPct, animationSpec = tween(600), label = "bar-present"
    )
    val animLate by animateFloatAsState(
        targetValue = latePct, animationSpec = tween(600), label = "bar-late"
    )
    val animAbsent by animateFloatAsState(
        targetValue = absentPct, animationSpec = tween(600), label = "bar-absent"
    )

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(animPresent.coerceAtLeast(0.01f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(GreenPresent.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .weight(animLate.coerceAtLeast(0.01f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(YellowLate.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .weight(animAbsent.coerceAtLeast(0.01f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RedAbsent.copy(alpha = 0.25f))
        )
    }
}

// ── Toast Notification ──

@Composable
fun LiveToast(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = androidx.compose.animation.fadeOut() +
                androidx.compose.animation.slideOutVertically { -it },
        modifier = Modifier.offset { IntOffset(0, 0) }
    ) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            onDismiss()
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GreenPresent,
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

    // Toast state
    var toastMessage by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }

    // Écouter les événements live du ViewModel
    LaunchedEffect(Unit) {
        viewModel.liveEvents.collect { event ->
            toastMessage = event
            toastVisible = true
        }
    }

    // Simulation live automatique
    var simEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(simEnabled) {
        if (simEnabled) {
            while (true) {
                kotlinx.coroutines.delay(5000 + (Math.random() * 10000).toLong())
                viewModel.simulateArrivee()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Header row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Apercu de la seance",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(12.dp))
                    // Live indicator
                    if (simEnabled) {
                        Surface(
                            shape = CircleShape,
                            color = GreenPresent.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GreenPresent)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenPresent)
                            }
                        }
                    }
                }
                Text(
                    state.matiere,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Toggle simulation
                if (!simEnabled) {
                    OutlinedButton(
                        onClick = { simEnabled = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Simulation live", fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                } else {
                    TextButton(
                        onClick = { simEnabled = false },
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Stop", fontSize = 11.sp, color = RedAbsent)
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // Séance status chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (state.seanceStatut) {
                        StatutSeance.PLANIFIE -> MaterialTheme.colorScheme.surfaceVariant
                        StatutSeance.EN_COURS -> GreenPresent.copy(alpha = 0.12f)
                        StatutSeance.CLOTURE_ENSEIGNANT -> BlueInfo.copy(alpha = 0.12f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (state.seanceStatut) {
                                        StatutSeance.PLANIFIE -> StatusPending
                                        StatutSeance.EN_COURS -> GreenPresent
                                        StatutSeance.CLOTURE_ENSEIGNANT -> BlueInfo
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
                                StatutSeance.EN_COURS -> GreenPresent
                                StatutSeance.CLOTURE_ENSEIGNANT -> BlueInfo
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Toast notification
        Box(Modifier.fillMaxWidth()) {
            LiveToast(
                message = toastMessage,
                visible = toastVisible,
                onDismiss = { toastVisible = false }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ===== KPI CARDS ROW (animés) =====
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedKpiCard(
                title = "Presence",
                value = state.presents,
                subtitle = "${if (state.total > 0) (state.presents * 100 / state.total) else 0}%",
                icon = Icons.Default.CheckCircle,
                valueColor = GreenPresent,
                modifier = Modifier.weight(1f)
            )
            AnimatedKpiCard(
                title = "Retards",
                value = state.lates,
                subtitle = "${if (state.total > 0) (state.lates * 100 / state.total) else 0}%",
                icon = Icons.Default.Schedule,
                valueColor = YellowLate,
                modifier = Modifier.weight(1f)
            )
            AnimatedKpiCard(
                title = "Absences",
                value = state.absents,
                subtitle = "${if (state.total > 0) (state.absents * 100 / state.total) else 0}%",
                icon = Icons.Default.Cancel,
                valueColor = RedAbsent,
                modifier = Modifier.weight(1f)
            )
            AnimatedKpiCard(
                title = "Inscrits",
                value = state.total,
                subtitle = "Total",
                icon = Icons.Default.People,
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // ===== Filters + Actions =====
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = state.selectedClasse == "B2_IT",
                    onClick = { viewModel.filterByClasse("B2_IT") },
                    label = { Text("B2_IT", fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        enabled = true,
                        selected = state.selectedClasse == "B2_IT"
                    )
                )
                FilterChip(
                    selected = state.selectedSemestre == "S2_2026",
                    onClick = { viewModel.filterBySemestre("S2_2026") },
                    label = { Text("Semestre S2", fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        enabled = true,
                        selected = state.selectedSemestre == "S2_2026"
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            containerColor = RedAbsent,
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

        // ===== STATS BAR (animée) =====
        if (state.total > 0) {
            AnimatedStatBar(
                presentPct = state.presents.toFloat() / state.total,
                latePct = state.lates.toFloat() / state.total,
                absentPct = state.absents.toFloat() / state.total
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${state.presents} presences", fontSize = 11.sp, color = GreenPresent)
                Text("${state.lates} retards", fontSize = 11.sp, color = YellowLate)
                Text("${state.absents} absences", fontSize = 11.sp, color = RedAbsent)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ===== DATA TABLE =====
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.id }) { row ->
                        RowItem(row)
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

// ── Row avec hover animation ──

@Composable
fun RowItem(row: AttendanceRow) {
    var hovered by remember { mutableStateOf(false) }

    val statutColor = when (row.statut) {
        StatutFinal.PRESENT -> GreenPresent
        StatutFinal.RETARD -> YellowLate
        StatutFinal.ABSENT -> RedAbsent
        StatutFinal.EN_ATTENTE -> StatusPending
    }
    val statutBg = when (row.statut) {
        StatutFinal.PRESENT -> GreenPresentBg
        StatutFinal.RETARD -> YellowLateBg
        StatutFinal.ABSENT -> RedAbsentBg
        StatutFinal.EN_ATTENTE -> Color.Gray.copy(alpha = 0.1f)
    }
    val statutLabel = when (row.statut) {
        StatutFinal.PRESENT -> "Present"
        StatutFinal.RETARD -> "Retard"
        StatutFinal.ABSENT -> "Absent"
        StatutFinal.EN_ATTENTE -> "En attente"
    }

    val bgAlpha by animateFloatAsState(
        targetValue = if (hovered) 0.08f else 0f,
        animationSpec = tween(200), label = "hover-bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (row.id % 2 == 0) MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = bgAlpha.coerceAtLeast(0.2f))
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${row.id}",
            Modifier.width(36.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            row.matricule,
            Modifier.width(100.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(Modifier.weight(1f)) {
            Text(
                "${row.nom} ${row.prenom}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Surface(
            modifier = Modifier.width(110.dp),
            shape = RoundedCornerShape(8.dp),
            color = statutBg
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statutColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    statutLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statutColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            row.heureScanDebut,
            Modifier.width(90.dp),
            fontSize = 13.sp,
            color = if (row.heureScanDebut == "---") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface
        )
        Text(
            row.heureScanFin,
            Modifier.width(90.dp),
            fontSize = 13.sp,
            color = if (row.heureScanFin == "---") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
