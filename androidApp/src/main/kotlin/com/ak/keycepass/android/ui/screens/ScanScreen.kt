package com.ak.keycepass.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ak.keycepass.android.ui.viewmodel.ScanViewModel
import com.ak.keycepass.android.ui.viewmodel.ScanUiState

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onNavigateDelegate: () -> Unit,
    onNavigateTeacher: () -> Unit,
    onBackToLogin: () -> Unit,
    onStartSecondScan: (String) -> Unit = {}
) {
    val scanState by viewModel.scanState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Button(onClick = onBackToLogin) {
            Text("Déconnexion")
        }

        when (scanState) {
            is ScanUiState.Pret -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Prêt à scanner")
                        Button(onClick = {
                            viewModel.onQrCodeDetecte("keycepass://presence?semaineId=3&classeId=B2_IT&token=DEMO&seanceId=101")
                        }) {
                            Text("Simuler scan arrivée")
                        }
                    }
                }
            }
            is ScanUiState.Traitement -> {
                Text("Traitement en cours...")
            }
            is ScanUiState.AttenteClotureEnseignant -> {
                Text("En attente de clôture enseignant...")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNavigateTeacher) {
                        Text("Espace Enseignant")
                    }
                    Button(onClick = {
                        onStartSecondScan("keycepass://presence?semaineId=3&classeId=B2_IT&token=DEMO&seanceId=101")
                    }) {
                        Text("Simuler scan départ")
                    }
                }
            }
            is ScanUiState.StatutFinal -> {
                Text("Statut final: ${(scanState as ScanUiState.StatutFinal).statut}")
                Button(onClick = onBackToLogin) {
                    Text("Retour accueil")
                }
            }
            is ScanUiState.SeanceCloturee -> {
                Text("Séance clôturée par enseignant")
                Button(onClick = onNavigateDelegate) {
                    Text("Espace Délégué")
                }
            }
            is ScanUiState.Erreur -> {
                Text((scanState as ScanUiState.Erreur).message)
                Button(onClick = onBackToLogin) {
                    Text("Retour accueil")
                }
            }
        }
    }
}
