package com.ak.keycepass.android.metier.vue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ak.keycepass.android.metier.vue.EtatScan

@Composable
fun EcranScan(
    vueModele: VueScan = viewModel()
) {
    val etat by vueModele.etatScan.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            etat.enChargement -> {
                Text(text = "Recherche du cours en cours...", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            etat.erreur != null -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Erreur", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = etat.erreur!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            etat.localisationRefusee -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "📍 Présence refusée", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vous devez être dans la salle de cours pour enregistrer votre présence.\nDistance maximale autorisée : 200 mètres.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            etat.etape is EtapeScan.SeanceTrouvee -> {
                val info = etat.etape as EtapeScan.SeanceTrouvee
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Cours : ${info.nomMatiere}", style = MaterialTheme.typography.titleLarge)
                        Text(text = "Horaire : ${info.heureDebut} → ${info.heureFin}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { vueModele.confirmerScanArrivee() }) {
                                Text("Confirmer arrivée")
                            }
                            Button(onClick = { /* TODO: action clôture enseignant */ }) {
                                Text("Clôturer séance")
                            }
                        }
                    }
                }
            }
            else -> {
                Text(text = "Prêt à scanner", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { /* TODO: lancer scan */ }) {
                    Text("Scanner QR de présence")
                }
            }
        }
    }
}
