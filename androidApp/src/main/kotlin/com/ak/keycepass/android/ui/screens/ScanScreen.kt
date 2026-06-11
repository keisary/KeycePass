package com.ak.keycepass.android.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ak.keycepass.android.ui.viewmodel.ScanUiState
import com.ak.keycepass.android.ui.viewmodel.ScanViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    var handledError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(scanState) {
        val s = scanState
        if (s is ScanUiState.Erreur && s.message != handledError) {
            handledError = s.message
            Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
        }
        if (s !is ScanUiState.Erreur) handledError = null
    }

    val permissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            !permissions.allPermissionsGranted -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Permissions requises")
                        Text("Camera + Localisation")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissions.launchMultiplePermissionRequest() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Autoriser les permissions")
                        }
                    }
                }
            }
            scanState is ScanUiState.Pret -> {
                Button(
                    onClick = { viewModel.lancerScan(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Text("Scanner")
                }
            }
            else -> {
                val texte = when (scanState) {
                    is ScanUiState.Pret -> ""
                    is ScanUiState.Traitement -> "Traitement en cours..."
                    is ScanUiState.AttenteClotureEnseignant -> {
                        val a = scanState as ScanUiState.AttenteClotureEnseignant
                        "Présence enregistrée\nStatut : ${a.statutProvisoire}\nEn attente de la clôture par l'enseignant..."
                    }
                    is ScanUiState.StatutFinal -> {
                        val f = scanState as ScanUiState.StatutFinal
                        "Statut final\n${f.statut}"
                    }
                    is ScanUiState.SeanceCloturee -> "Séance clôturée"
                    is ScanUiState.Erreur -> {
                        val e = scanState as ScanUiState.Erreur
                        "Erreur\n${e.message}"
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = texte,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                if (scanState is ScanUiState.AttenteClotureEnseignant) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.lancerScan(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scanner le QR de fin de cours")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.reinitialiser() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Réinitialiser")
                }

                if (scanState is ScanUiState.SeanceCloturee) {
                    Button(
                        onClick = onBackToLogin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retour au login")
                    }
                }
            }
        }
    }
}



