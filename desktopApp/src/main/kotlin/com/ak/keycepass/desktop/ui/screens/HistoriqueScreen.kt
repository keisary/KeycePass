package com.ak.keycepass.desktop.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.ak.keycepass.desktop.ui.theme.*
import com.ak.keycepass.desktop.ui.viewmodel.AdminViewModel
import com.ak.keycepass.desktop.ui.viewmodel.HistoriqueEntry

@Composable
fun HistoriqueScreen(viewModel: AdminViewModel = remember { AdminViewModel() }) {
    val entries = remember { viewModel.getHistorique() }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Historique et Statistiques",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Assiduite des etudiants sur les 6 dernieres seances",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Stats globales
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val totalP = entries.sumOf { it.presents }
            val totalR = entries.sumOf { it.retards }
            val totalA = entries.sumOf { it.absents }
            val totalT = entries.sumOf { it.total }

            StatGlobale("Presence moyenne", "${if (totalT > 0) (totalP * 100 / totalT) else 0}%",
                "${totalP}/${totalT} emargements", StatusPresent, Modifier.weight(1f))
            StatGlobale("Taux de retard", "${if (totalT > 0) (totalR * 100 / totalT) else 0}%",
                "${totalR} retards", StatusLate, Modifier.weight(1f))
            StatGlobale("Taux d'absence", "${if (totalT > 0) (totalA * 100 / totalT) else 0}%",
                "${totalA} absences", StatusAbsent, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // Graphique
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Evolution des presences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))

                entries.forEach { seance ->
                    BarChartRow(seance)
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Legende monochrome
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(StatusPresent, "Presence")
                    Spacer(Modifier.width(24.dp))
                    LegendDot(Color.Gray, "Retard")
                    Spacer(Modifier.width(24.dp))
                    LegendDot(StatusAbsent, "Absence")
                }
            }
        }
    }
}

@Composable
private fun StatGlobale(title: String, value: String, description: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(4.dp))
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BarChartRow(seance: HistoriqueEntry) {
    val pctP = seance.presents.toFloat() / seance.total
    val pctR = seance.retards.toFloat() / seance.total
    val pctA = seance.absents.toFloat() / seance.total

    val animP by animateFloatAsState(pctP, tween(600), label = "bar-p-${seance.date}")
    val animR by animateFloatAsState(pctR, tween(600), label = "bar-r-${seance.date}")
    val animA by animateFloatAsState(pctA, tween(600), label = "bar-a-${seance.date}")

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(seance.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(seance.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.weight(animP.coerceAtLeast(0.01f)).fillMaxHeight().padding(end = 2.dp),
                shape = RoundedCornerShape(4.dp), color = StatusPresent.copy(alpha = 0.8f)
            ) {}
            Surface(
                modifier = Modifier.weight(animR.coerceAtLeast(0.01f)).fillMaxHeight().padding(end = 2.dp),
                shape = RoundedCornerShape(4.dp), color = Color.Gray.copy(alpha = 0.5f)
            ) {}
            Surface(
                modifier = Modifier.weight(animA.coerceAtLeast(0.01f)).fillMaxHeight().padding(end = 2.dp),
                shape = RoundedCornerShape(4.dp), color = StatusAbsent.copy(alpha = 0.8f)
            ) {}
        }
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${seance.presents}p", fontSize = 10.sp, color = StatusPresent)
            Text("${seance.retards}r", fontSize = 10.sp, color = StatusLate)
            Text("${seance.absents}a", fontSize = 10.sp, color = StatusAbsent)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
