package com.ak.keycepass.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PairedDevice(
    val id: Int,
    val deviceName: String,
    val deviceId: String,
    val etudiant: String,
    val matricule: String,
    val pairedAt: String,
    val isActive: Boolean
)

@Composable
fun GestionEnrolementScreen() {
    var devices by remember {
        mutableStateOf(
            listOf(
                PairedDevice(1, "Samsung Galaxy A54", "abc-def-123", "Diallo Amadou", "B2-001", "01/06/2026", true),
                PairedDevice(2, "Redmi Note 13", "ghi-jkl-456", "Koné Fatoumata", "B2-002", "01/06/2026", true),
                PairedDevice(3, "iPhone 14", "mno-pqr-789", "Camara Seydou", "B2-004", "02/06/2026", false),
                PairedDevice(4, "Tecno Spark 10", "stu-vwx-012", "Sissoko Ibrahim", "B2-006", "03/06/2026", true),
            )
        )
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Gestion des Pairages",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "Appareils couplés aux comptes étudiants",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Stats rapides
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Appareils actifs", style = MaterialTheme.typography.labelMedium)
                    Text("${devices.count { it.isActive }}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Inactifs / Dissociés", style = MaterialTheme.typography.labelMedium)
                    Text("${devices.count { !it.isActive }}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total appareils", style = MaterialTheme.typography.labelMedium)
                    Text("${devices.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Liste des appareils
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onReset = {
                            devices = devices.map {
                                if (it.id == device.id) it.copy(isActive = !it.isActive) else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: PairedDevice, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(device.deviceName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (device.isActive)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            if (device.isActive) "✅ Actif" else "❌ Inactif",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("${device.etudiant} • ${device.matricule}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("ID: ${device.deviceId} | Couplé le ${device.pairedAt}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
                onClick = onReset,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (device.isActive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (device.isActive) "Dissocier" else "Recoupler")
            }
        }
    }
}
