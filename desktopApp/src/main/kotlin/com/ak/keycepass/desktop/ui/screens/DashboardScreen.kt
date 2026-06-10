package com.ak.keycepass.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.keycepass.desktop.ui.theme.*
import com.ak.keycepass.desktop.ui.viewmodel.AdminViewModel
import com.ak.keycepass.desktop.ui.viewmodel.AttendanceRow
import com.ak.keycepass.shared.domain.model.StatutFinal
import com.ak.keycepass.shared.domain.model.StatutSeance
import androidx.compose.runtime.remember

@Composable
fun DashboardScreen(viewModel: AdminViewModel = remember { AdminViewModel() }) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        // Header row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Aperçu de la séance",
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

            // Séance status chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (state.seanceStatut) {
                    StatutSeance.PLANIFIE -> MaterialTheme.colorScheme.surfaceVariant
                    StatutSeance.EN_COURS -> GreenPresent.copy(alpha = 0.15f)
                    StatutSeance.CLOTURE_ENSEIGNANT -> PurpleAccent.copy(alpha = 0.15f)
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
                                    StatutSeance.PLANIFIE -> Color.Gray
                                    StatutSeance.EN_COURS -> GreenPresent
                                    StatutSeance.CLOTURE_ENSEIGNANT -> PurpleAccent
                                }
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (state.seanceStatut) {
                            StatutSeance.PLANIFIE -> "Planifiée"
                            StatutSeance.EN_COURS -> "En cours"
                            StatutSeance.CLOTURE_ENSEIGNANT -> "Clôturée"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = when (state.seanceStatut) {
                            StatutSeance.PLANIFIE -> MaterialTheme.colorScheme.onSurfaceVariant
                            StatutSeance.EN_COURS -> GreenPresent
                            StatutSeance.CLOTURE_ENSEIGNANT -> PurpleAccent
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== KPI CARDS ROW =====
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientKPICard(
                title = "Présents",
                value = state.presents,
                subtitle = "${if (state.total > 0) (state.presents * 100 / state.total) else 0}%",
                gradient = GradientGreen,
                icon = "✅",
                modifier = Modifier.weight(1f)
            )
            GradientKPICard(
                title = "Retards",
                value = state.lates,
                subtitle = "${if (state.total > 0) (state.lates * 100 / state.total) else 0}%",
                gradient = GradientOrange,
                icon = "⏳",
                modifier = Modifier.weight(1f)
            )
            GradientKPICard(
                title = "Absents",
                value = state.absents,
                subtitle = "${if (state.total > 0) (state.absents * 100 / state.total) else 0}%",
                gradient = GradientRed,
                icon = "❌",
                modifier = Modifier.weight(1f)
            )
            GradientKPICard(
                title = "Inscrits",
                value = state.total,
                subtitle = "Total",
                gradient = GradientPurple,
                icon = "👥",
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
            // Filter chips row
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

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { viewModel.rafraichir() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🔄", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Rafraîchir", fontSize = 13.sp)
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
                        Text("🔒", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Clôturer", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ===== STATS BAR (mini progress) =====
        if (state.total > 0) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val presentPct = state.presents.toFloat() / state.total
                val latePct = state.lates.toFloat() / state.total
                val absentPct = state.absents.toFloat() / state.total

                Box(
                    modifier = Modifier
                        .weight(presentPct.coerceAtLeast(0.01f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GreenPresent.copy(alpha = 0.3f))
                )
                Box(
                    modifier = Modifier
                        .weight(latePct.coerceAtLeast(0.01f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(YellowLate.copy(alpha = 0.3f))
                )
                Box(
                    modifier = Modifier
                        .weight(absentPct.coerceAtLeast(0.01f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(RedAbsent.copy(alpha = 0.3f))
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${state.presents} présents", fontSize = 11.sp, color = GreenPresent)
                Text("${state.lates} retards", fontSize = 11.sp, color = YellowLate)
                Text("${state.absents} absents", fontSize = 11.sp, color = RedAbsent)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ===== DATA TABLE =====
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Table header
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
                    Text("Étudiant", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp,
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

                // Table rows
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.id }) { row ->
                        AttendanceRowItem(row)
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

@Composable
fun GradientKPICard(
    title: String,
    value: Int,
    subtitle: String,
    gradient: List<Color>,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = gradient.map { it.copy(alpha = 0.9f) }
                    )
                )
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
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                    Text(icon, fontSize = 20.sp)
                }

                Column {
                    Text(
                        "$value",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 32.sp
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Decorative circle (glassmorphism)
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f)
            ) {}
        }
    }
}

@Composable
fun AttendanceRowItem(row: AttendanceRow) {
    val statutColor = when (row.statut) {
        StatutFinal.PRESENT -> GreenPresent
        StatutFinal.RETARD -> YellowLate
        StatutFinal.ABSENT -> RedAbsent
        StatutFinal.EN_ATTENTE -> Color.Gray
    }
    val statutBg = when (row.statut) {
        StatutFinal.PRESENT -> GreenPresentBg
        StatutFinal.RETARD -> YellowLateBg
        StatutFinal.ABSENT -> RedAbsentBg
        StatutFinal.EN_ATTENTE -> Color.Gray.copy(alpha = 0.1f)
    }
    val statutLabel = when (row.statut) {
        StatutFinal.PRESENT -> "Présent"
        StatutFinal.RETARD -> "Retard"
        StatutFinal.ABSENT -> "Absent"
        StatutFinal.EN_ATTENTE -> "En attente"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (row.id % 2 == 0) Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ID
        Text(
            "${row.id}",
            Modifier.width(36.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Matricule
        Text(
            row.matricule,
            Modifier.width(100.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Name
        Column(Modifier.weight(1f)) {
            Text(
                "${row.nom} ${row.prenom}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Status badge
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

        // Scan times
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
