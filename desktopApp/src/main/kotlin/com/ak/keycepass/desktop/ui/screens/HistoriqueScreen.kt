package com.ak.keycepass.desktop.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.keycepass.desktop.ui.theme.*

// Donnees d'exemple pour les statistiques
private data class SeanceData(
    val date: String,
    val label: String,
    val presents: Int,
    val retards: Int,
    val absents: Int,
    val total: Int
)

private val mockHistorique = listOf(
    SeanceData("03/06", "Sem 1 - Lun", 9, 1, 2, 12),
    SeanceData("04/06", "Sem 1 - Mar", 7, 3, 2, 12),
    SeanceData("05/06", "Sem 1 - Mer", 10, 1, 1, 12),
    SeanceData("06/06", "Sem 1 - Jeu", 8, 2, 2, 12),
    SeanceData("07/06", "Sem 1 - Ven", 6, 2, 4, 12),
    SeanceData("10/06", "Sem 2 - Lun", 8, 2, 2, 12),
)

@Composable
fun HistoriqueScreen() {
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
            val totalP = mockHistorique.sumOf { it.presents }
            val totalR = mockHistorique.sumOf { it.retards }
            val totalA = mockHistorique.sumOf { it.absents }
            val totalT = mockHistorique.sumOf { it.total }

            StatGlobale(
                title = "Presence moyenne",
                value = "${if (totalT > 0) (totalP * 100 / totalT) else 0}%",
                description = "${totalP}/${totalT} emargements",
                color = GreenPresent,
                modifier = Modifier.weight(1f)
            )
            StatGlobale(
                title = "Taux de retard",
                value = "${if (totalT > 0) (totalR * 100 / totalT) else 0}%",
                description = "${totalR} retards",
                color = YellowLate,
                modifier = Modifier.weight(1f)
            )
            StatGlobale(
                title = "Taux d'absence",
                value = "${if (totalT > 0) (totalA * 100 / totalT) else 0}%",
                description = "${totalA} absences",
                color = RedAbsent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Graphique
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Evolution des presences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))

                // Barres horizontales animees
                mockHistorique.forEach { seance ->
                    BarChartRow(seance)
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Legende
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(GreenPresent, "Presence")
                    Spacer(Modifier.width(24.dp))
                    LegendDot(YellowLate, "Retard")
                    Spacer(Modifier.width(24.dp))
                    LegendDot(RedAbsent, "Absence")
                }
            }
        }
    }
}

@Composable
private fun StatGlobale(
    title: String,
    value: String,
    description: String,
    color: Color,
    modifier: Modifier = Modifier
) {
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
private fun BarChartRow(seance: SeanceData) {
    val pctP = seance.presents.toFloat() / seance.total
    val pctR = seance.retards.toFloat() / seance.total
    val pctA = seance.absents.toFloat() / seance.total

    val animP by animateFloatAsState(pctP, tween(600), label = "bar-p-${seance.date}")
    val animR by animateFloatAsState(pctR, tween(600), label = "bar-r-${seance.date}")
    val animA by animateFloatAsState(pctA, tween(600), label = "bar-a-${seance.date}")

    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(seance.label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(seance.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth().height(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barre presence
            Surface(
                modifier = Modifier
                    .weight(animP.coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                shape = RoundedCornerShape(4.dp),
                color = GreenPresent.copy(alpha = 0.7f)
            ) {}
            // Barre retard
            Surface(
                modifier = Modifier
                    .weight(animR.coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                shape = RoundedCornerShape(4.dp),
                color = YellowLate.copy(alpha = 0.7f)
            ) {}
            // Barre absence
            Surface(
                modifier = Modifier
                    .weight(animA.coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                shape = RoundedCornerShape(4.dp),
                color = RedAbsent.copy(alpha = 0.7f)
            ) {}
        }
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${seance.presents}p", fontSize = 10.sp, color = GreenPresent)
            Text("${seance.retards}r", fontSize = 10.sp, color = YellowLate)
            Text("${seance.absents}a", fontSize = 10.sp, color = RedAbsent)
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
