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
    val entries by viewModel.historiqueBackend.collectAsState()

    // Charger les donnees au demarrage
    LaunchedEffect(Unit) {
        viewModel.chargerHistorique()
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Historique et Statistiques",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Assiduite sur les dernieres seances",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // Stats globales
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val totalP = entries.sumOf { it.presents }
            val totalR = entries.sumOf { it.retards }
            val totalA = entries.sumOf { it.absents }
            val totalT = entries.sumOf { it.total }

            StatGlobale("Presence", "${if (totalT > 0) (totalP * 100 / totalT) else 0}%",
                "${totalP}/${totalT}", StatusPresent, Modifier.weight(1f))
            StatGlobale("Retard", "${if (totalT > 0) (totalR * 100 / totalT) else 0}%",
                "${totalR} retards", StatusLate, Modifier.weight(1f))
            StatGlobale("Absence", "${if (totalT > 0) (totalA * 100 / totalT) else 0}%",
                "${totalA} absences", StatusAbsent, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Graphique
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(20.dp).fillMaxSize()) {
                Text("Evolution des presences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                if (entries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aucune donnee disponible",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Spacer(Modifier.height(4.dp))
                            Text("Importez des etudiants et creez des seances",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                } else {
                    entries.forEach { seance ->
                        BarChartRow(seance)
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(StatusPresent, "Presence")
                        Spacer(Modifier.width(20.dp))
                        LegendDot(Color.Gray, "Retard")
                        Spacer(Modifier.width(20.dp))
                        LegendDot(StatusAbsent, "Absence")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatGlobale(title: String, value: String, description: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BarChartRow(seance: HistoriqueEntry) {
    val pctP = if (seance.total > 0) seance.presents.toFloat() / seance.total else 0f
    val pctR = if (seance.total > 0) seance.retards.toFloat() / seance.total else 0f
    val pctA = if (seance.total > 0) seance.absents.toFloat() / seance.total else 0f

    val animP by animateFloatAsState(pctP, tween(500), label = "bar-p-${seance.date}")
    val animR by animateFloatAsState(pctR, tween(500), label = "bar-r-${seance.date}")
    val animA by animateFloatAsState(pctA, tween(500), label = "bar-a-${seance.date}")

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(seance.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(seance.date, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.weight(animP.coerceAtLeast(0.01f)).fillMaxHeight().padding(end = 2.dp),
                shape = RoundedCornerShape(3.dp), color = StatusPresent.copy(alpha = 0.8f)) {}
            Surface(modifier = Modifier.weight(animR.coerceAtLeast(0.01f)).fillMaxHeight().padding(end = 2.dp),
                shape = RoundedCornerShape(3.dp), color = Color.Gray.copy(alpha = 0.5f)) {}
            Surface(modifier = Modifier.weight(animA.coerceAtLeast(0.01f)).fillMaxHeight().padding(end = 2.dp),
                shape = RoundedCornerShape(3.dp), color = StatusAbsent.copy(alpha = 0.8f)) {}
        }
        Spacer(Modifier.height(1.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${seance.presents}p", fontSize = 9.sp, color = StatusPresent)
            Text("${seance.retards}r", fontSize = 9.sp, color = StatusLate)
            Text("${seance.absents}a", fontSize = 9.sp, color = StatusAbsent)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
