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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.keycepass.desktop.ui.theme.GreenPresent
import com.ak.keycepass.desktop.ui.theme.RedAbsent
import com.ak.keycepass.desktop.ui.theme.YellowLate
import com.ak.keycepass.desktop.ui.viewmodel.AdminViewModel
import com.ak.keycepass.desktop.ui.viewmodel.AttendanceRow
import com.ak.keycepass.shared.domain.model.StatutFinal
import com.ak.keycepass.shared.domain.model.StatutSeance
import androidx.compose.runtime.remember

@Composable
fun DashboardScreen(viewModel: AdminViewModel = remember { AdminViewModel() }) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        // Ligne d'en-tête avec le statut séance
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Dashboard • ${state.matiere}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (state.seanceStatut) {
                    StatutSeance.PLANIFIE -> Color.Gray.copy(alpha = 0.2f)
                    StatutSeance.EN_COURS -> GreenPresent.copy(alpha = 0.2f)
                    StatutSeance.CLOTURE_ENSEIGNANT -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                }
            ) {
                Text(
                    when (state.seanceStatut) {
                        StatutSeance.PLANIFIE -> "📋 PLANIFIÉE"
                        StatutSeance.EN_COURS -> "🟢 EN COURS"
                        StatutSeance.CLOTURE_ENSEIGNANT -> "🔒 CLÔTURÉE"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // KPIs
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KPICard("Présents", state.presents, "${if (state.total > 0) (state.presents * 100 / state.total) else 0}%", GreenPresent)
            KPICard("Retards", state.lates, "${if (state.total > 0) (state.lates * 100 / state.total) else 0}%", YellowLate)
            KPICard("Absents", state.absents, "${if (state.total > 0) (state.absents * 100 / state.total) else 0}%", RedAbsent)
            if (state.total > 0) {
                KPICard("Total", state.total, "Inscrits", MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Filtres + Actions
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { viewModel.filterByClasse("B2_IT") },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (state.selectedClasse == "B2_IT")
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) { Text("B2_IT") }

                FilledTonalButton(
                    onClick = { viewModel.filterBySemestre("S2_2026") },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (state.selectedSemestre == "S2_2026")
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) { Text("Semestre S2") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { viewModel.rafraichir() }) {
                    Text("🔄 Rafraîchir")
                }
                if (state.seanceStatut == StatutSeance.EN_COURS) {
                    Button(
                        onClick = { viewModel.cloturerSeance() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("🔒 Clôturer la séance")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tableau des présences
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // En-tête du tableau
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("N°", Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Matricule", Modifier.width(100.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Nom", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Statut", Modifier.width(120.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("1er Scan", Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("2nd Scan", Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                HorizontalDivider(thickness = 2.dp)

                // Lignes
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.id }) { row ->
                        AttendanceRowItem(row)
                        HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
fun KPICard(title: String, value: Int, subtitle: String, color: Color) {
    Card(
        modifier = Modifier.size(width = 200.dp, height = 110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = color)
            Text(
                "$value",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
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

    Row(
        Modifier
            .fillMaxWidth()
            .background(statutColor.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${row.id}", Modifier.width(40.dp), fontSize = 13.sp)
        Text(row.matricule, Modifier.width(100.dp), fontSize = 13.sp)
        Text("${row.nom} ${row.prenom}", Modifier.weight(1f), fontSize = 13.sp)

        Row(Modifier.width(120.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(statutColor))
            Spacer(Modifier.width(6.dp))
            Text(
                row.statut.name,
                color = statutColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        Text(row.heureScanDebut, Modifier.width(90.dp), fontSize = 13.sp, color = if (row.heureScanDebut == "---") Color.Gray else Color.Unspecified)
        Text(row.heureScanFin, Modifier.width(90.dp), fontSize = 13.sp, color = if (row.heureScanFin == "---") Color.Gray else Color.Unspecified)
    }
}
